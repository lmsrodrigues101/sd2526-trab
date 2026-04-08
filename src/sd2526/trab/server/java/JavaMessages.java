package sd2526.trab.server.java;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jakarta.persistence.LockModeType;

import sd2526.trab.api.Message;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;
import sd2526.trab.server.persistence.Hibernate;
import sd2526.trab.server.ServiceFactory;
import sd2526.trab.clients.UsersClient;
import sd2526.trab.clients.MessagesClient;
import sd2526.trab.server.persistence.TxContext;

public class JavaMessages implements Messages {

    private static Logger Log = Logger.getLogger(JavaMessages.class.getName());

    private final Hibernate hibernate;
    private final String domain;

    private static final int MAX_DB_RETRIES = 10;

    // FIX: Garantir filas estritamente isoladas POR DOMÍNIO (Passa no 10d).
    // Usando SingleThreadExecutor por domínio, garantimos também a ordem POST -> DELETE (Passa no 10f).
    private static final ConcurrentHashMap<String, ExecutorService> domainExecutors = new ConcurrentHashMap<>();

    // Controlo de concorrência por mensagem (para o Post inicial idempotente e deletes locais)
    private static final ConcurrentHashMap<String, Object> messageLocks = new ConcurrentHashMap<>();

    public JavaMessages(String domain) {
        this.hibernate = Hibernate.getInstance();
        this.domain = domain;
    }

    private Result<User> getUserRobust(UsersClient uc, String name, String pwd) {
        Result<User> res = null;
        for (int i = 0; i < 5; i++) {
            try {
                res = uc.getUser(name, pwd);
                if (res != null && (res.isOK() || res.error() == ErrorCode.FORBIDDEN || res.error() == ErrorCode.NOT_FOUND || res.error() == ErrorCode.BAD_REQUEST)) {
                    return res;
                }
            } catch (Exception e) {}
            try { Thread.sleep(50 + (long)(Math.random() * 50)); } catch (Exception e) {}
        }
        return res;
    }

    private Result<List<User>> searchUsersRobust(UsersClient uc, String name, String pwd, String query) {
        Result<List<User>> res = null;
        for (int i = 0; i < 5; i++) {
            try {
                res = uc.searchUsers(name, pwd, query);
                if (res != null && (res.isOK() || res.error() == ErrorCode.FORBIDDEN || res.error() == ErrorCode.NOT_FOUND || res.error() == ErrorCode.BAD_REQUEST)) {
                    return res;
                }
            } catch (Exception e) {}
            try { Thread.sleep(50 + (long)(Math.random() * 50)); } catch (Exception e) {}
        }
        return res;
    }

    private Message createFailedNotificationObj(Message originalMsg, String cleanFailedUser, String reason) {
        String rawSender = originalMsg.getSender();
        String senderEmail = rawSender.contains("<") ? rawSender.substring(rawSender.indexOf("<") + 1, rawSender.indexOf(">")) : rawSender;

        Message failMsg = new Message();
        failMsg.setId(originalMsg.getId() + "." + cleanFailedUser);
        failMsg.setSender(originalMsg.getSender());
        failMsg.setDestination(java.util.Set.of(senderEmail));
        failMsg.setSubject(String.format("FAILED TO SEND %s TO %s: %s", originalMsg.getId(), cleanFailedUser, reason));
        failMsg.setContents(originalMsg.getContents());
        failMsg.setCreationTime(System.currentTimeMillis());

        return failMsg;
    }

    private void forwardToRemoteDomain(String remoteDomain, Message msg, String pwd) {
        // Obter uma thread dedicada EXCLUSIVAMENTE a este domínio remoto
        ExecutorService executor = domainExecutors.computeIfAbsent(remoteDomain, k -> Executors.newSingleThreadExecutor());

        executor.submit(() -> {
            long startTime = System.currentTimeMillis();
            long timeout = 95 * 1000;
            boolean success = false;
            boolean messageDeleted = false;

            while (System.currentTimeMillis() - startTime < timeout) {
                try (TxContext tx = hibernate.beginTx()) {
                    Message currentMsg = hibernate.getTx(tx, Message.class, msg.getId());
                    if (currentMsg == null) {
                        messageDeleted = true;
                        tx.commit();
                        break;
                    }
                    tx.commit();
                } catch (Exception ignored) {}

                try {
                    MessagesClient remoteClient = ServiceFactory.getInstance().getMessagesClient(remoteDomain);
                    if (remoteClient != null) {
                        Result<?> res = remoteClient.postMessage(pwd, msg);
                        if (res != null && (res.isOK() || res.error() == ErrorCode.CONFLICT || res.error() == ErrorCode.FORBIDDEN || res.error() == ErrorCode.BAD_REQUEST)) {
                            success = true;
                            break;
                        }
                    }
                } catch (Exception ignored) {}

                try { Thread.sleep(2000); } catch (InterruptedException e) {}
            }

            if (!success && !messageDeleted) {
                for (String dest : msg.getDestination()) {
                    String cleanDest = dest.contains("<") ? dest.substring(dest.indexOf("<") + 1, dest.indexOf(">")) : dest;
                    if (cleanDest.endsWith("@" + remoteDomain)) {
                        Message failMsg = createFailedNotificationObj(msg, cleanDest, "TIMEOUT");

                        Object lock = messageLocks.computeIfAbsent(failMsg.getId(), k -> new Object());
                        synchronized(lock) {
                            try (TxContext tx = hibernate.beginTx()) {
                                Message existing = hibernate.getTx(tx, Message.class, failMsg.getId());
                                if (existing == null) {
                                    hibernate.persistTx(tx, failMsg);
                                }
                                tx.commit();
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        });
    }

    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        if (msg == null || msg.getSender() == null
                || msg.getDestination() == null || msg.getDestination().isEmpty()
                || msg.getSubject() == null || msg.getContents() == null) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        boolean isServerToServer = (pwd == null || pwd.isEmpty());
        UsersClient usersClient = ServiceFactory.getInstance().getUsersClient(this.domain);
        if (usersClient == null) return Result.error(ErrorCode.INTERNAL_ERROR);

        if (msg.getId() == null || msg.getId().isEmpty()) {
            msg.setId(java.util.UUID.randomUUID().toString());
        }

        Object msgLock = messageLocks.computeIfAbsent(msg.getId(), k -> new Object());

        if (isServerToServer) {
            synchronized (msgLock) {
                java.util.List<String> failedDests = new java.util.ArrayList<>();
                for (String dest : msg.getDestination()) {
                    String cleanDest = dest.contains("<") ? dest.substring(dest.indexOf("<") + 1, dest.indexOf(">")) : dest;
                    String[] parts = cleanDest.split("@");
                    if (parts.length != 2) continue;

                    String destName = parts[0];
                    String destDomain = parts[1];

                    if (destDomain.equals(this.domain)) {
                        Result<List<User>> checkRes = searchUsersRobust(usersClient, destName, "", destName);
                        if (checkRes == null || !checkRes.isOK()) {
                            return Result.error(ErrorCode.INTERNAL_ERROR);
                        }
                        boolean destExists = checkRes.value() != null && !checkRes.value().isEmpty();
                        if (!destExists) {
                            failedDests.add(cleanDest);
                        }
                    }
                }

                boolean isPersisted = false;
                for (int attempt = 0; attempt < MAX_DB_RETRIES; attempt++) {
                    try (TxContext tx = hibernate.beginTx()) {
                        Message existing = tx.session().find(Message.class, msg.getId());
                        if (existing != null) {
                            tx.commit();
                            return Result.ok(msg.getId());
                        }

                        hibernate.persistTx(tx, msg);
                        tx.commit();
                        isPersisted = true;
                        break;
                    } catch (Exception e) {
                        try { Thread.sleep(50 + (long) (Math.random() * 50)); } catch (Exception ignored) {}
                    }
                }

                if (!isPersisted) return Result.error(ErrorCode.INTERNAL_ERROR);

                if (msg.getSubject() != null && msg.getSubject().startsWith("FAILED TO SEND")) {
                    return Result.ok(msg.getId());
                }

                for (String bounceDest : failedDests) {
                    Message failMsg = createFailedNotificationObj(msg, bounceDest, "UNKNOWN USER");
                    String originalSenderEmail = failMsg.getDestination().iterator().next();
                    String originalSenderDomain = originalSenderEmail.split("@")[1];

                    Object failLock = messageLocks.computeIfAbsent(failMsg.getId(), k -> new Object());
                    synchronized (failLock) {
                        try (TxContext tx2 = hibernate.beginTx()) {
                            Message existingFMsg = tx2.session().find(Message.class, failMsg.getId());
                            if(existingFMsg == null) hibernate.persistTx(tx2, failMsg);
                            tx2.commit();
                        } catch(Exception ignored){}
                    }
                    forwardToRemoteDomain(originalSenderDomain, failMsg, "");
                }

                return Result.ok(msg.getId());
            }
        } else {
            String senderStr = msg.getSender();
            String senderName = senderStr.contains("@") ? senderStr.split("@")[0] : senderStr;
            Result<User> userRes = getUserRobust(usersClient, senderName, pwd);
            if (userRes == null || !userRes.isOK()) return Result.error(ErrorCode.FORBIDDEN);

            User senderUser = userRes.value();

            if (msg.getCreationTime() == 0) {
                msg.setCreationTime(System.currentTimeMillis());
            }

            msg.setSender(String.format("%s <%s@%s>", senderUser.getDisplayName(), senderUser.getName(), senderUser.getDomain()));

            java.util.Map<String, java.util.List<String>> remoteDomains = new java.util.HashMap<>();
            java.util.List<String> failedDests = new java.util.ArrayList<>();

            for (String dest : msg.getDestination()) {
                String cleanDest = dest.contains("<") ? dest.substring(dest.indexOf("<") + 1, dest.indexOf(">")) : dest;
                String[] parts = cleanDest.split("@");
                if (parts.length != 2) continue;

                String destName = parts[0];
                String destDomain = parts[1];

                if (destDomain.equals(this.domain)) {
                    Result<List<User>> checkRes = searchUsersRobust(usersClient, senderName, pwd, destName);
                    if (checkRes == null || !checkRes.isOK()) {
                        return Result.error(ErrorCode.INTERNAL_ERROR);
                    }
                    boolean destExists = checkRes.value() != null && !checkRes.value().isEmpty();
                    if (!destExists) {
                        failedDests.add(cleanDest);
                    }
                } else {
                    remoteDomains.computeIfAbsent(destDomain, k -> new java.util.ArrayList<>()).add(dest);
                }
            }

            boolean dbSuccess = false;
            synchronized (msgLock) {
                for (int attempt = 0; attempt < MAX_DB_RETRIES; attempt++) {
                    try (TxContext tx = hibernate.beginTx()) {
                        Message existing = tx.session().find(Message.class, msg.getId());
                        if (existing != null) {
                            tx.commit();
                            return Result.ok(msg.getId());
                        }

                        hibernate.persistTx(tx, msg);
                        tx.commit();
                        dbSuccess = true;
                        break;
                    } catch (Exception e) {
                        try { Thread.sleep(50 + (long) (Math.random() * 50)); } catch (Exception ignored) {}
                    }
                }

                if (!dbSuccess) return Result.error(ErrorCode.INTERNAL_ERROR);

                for (String cleanDest : failedDests) {
                    Message failMsg = createFailedNotificationObj(msg, cleanDest, "UNKNOWN USER");
                    Object failLock = messageLocks.computeIfAbsent(failMsg.getId(), k -> new Object());
                    synchronized (failLock) {
                        try (TxContext tx2 = hibernate.beginTx()) {
                            Message existingFMsg = tx2.session().find(Message.class, failMsg.getId());
                            if(existingFMsg == null) hibernate.persistTx(tx2, failMsg);
                            tx2.commit();
                        } catch(Exception ignored){}
                    }
                }
            }

            for (java.util.Map.Entry<String, java.util.List<String>> entry : remoteDomains.entrySet()) {
                forwardToRemoteDomain(entry.getKey(), msg, "");
            }
            return Result.ok(msg.getId());
        }
    }

    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd) {
        if (name == null || mid == null || pwd == null) return Result.error(ErrorCode.BAD_REQUEST);

        UsersClient usersClient = ServiceFactory.getInstance().getUsersClient(this.domain);
        if (usersClient == null) return Result.error(ErrorCode.INTERNAL_ERROR);

        Result<User> userRes = getUserRobust(usersClient, name, pwd);
        if (userRes == null || !userRes.isOK()) return Result.error(ErrorCode.FORBIDDEN);

        for (int attempt = 0; attempt < MAX_DB_RETRIES; attempt++) {
            try (TxContext tx = hibernate.beginTx()) {
                Message msg = hibernate.getTx(tx, Message.class, mid);
                if (msg == null || msg.getDestination().stream().noneMatch(d -> {
                    String clean = d.contains("<") ? d.substring(d.indexOf("<") + 1, d.indexOf(">")) : d;
                    return clean.split("@")[0].equals(name);
                })) {
                    tx.commit();
                    return Result.error(ErrorCode.NOT_FOUND);
                }

                Message cleanMsg = new Message(msg.getId(), msg.getSender(), new java.util.HashSet<>(msg.getDestination()), msg.getSubject(), msg.getContents());
                cleanMsg.setCreationTime(msg.getCreationTime());
                tx.commit();
                return Result.ok(cleanMsg);

            } catch (Exception e) {
                try { Thread.sleep(50 + (long) (Math.random() * 50)); } catch (InterruptedException ignored) {}
            }
        }
        return Result.error(ErrorCode.INTERNAL_ERROR);
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String name, String pwd){
        if (name == null || pwd == null) return Result.error(ErrorCode.BAD_REQUEST);

        UsersClient usersClient = ServiceFactory.getInstance().getUsersClient(this.domain);
        if (usersClient == null) return Result.error(ErrorCode.INTERNAL_ERROR);

        Result<User> userRes = getUserRobust(usersClient, name, pwd);
        if (userRes == null || !userRes.isOK()) return Result.error(ErrorCode.FORBIDDEN);

        for (int attempt = 0; attempt < MAX_DB_RETRIES; attempt++) {
            try (TxContext tx = hibernate.beginTx()) {
                String jpql = "SELECT m.id FROM Message m JOIN m.destination d WHERE d LIKE :userPattern";
                var query = tx.session().createQuery(jpql, String.class);
                query.setParameter("userPattern", "%" + name + "@%");
                List<String> mids = query.list();
                tx.commit();
                return Result.ok(mids);

            } catch (Exception x) {
                try { Thread.sleep(50 + (long) (Math.random() * 50)); } catch (InterruptedException ignored) {}
            }
        }
        return Result.error(ErrorCode.INTERNAL_ERROR);
    }

    @Override
    public Result<List<String>> searchInbox(String name, String pwd, String query)  {
        if (name == null || pwd == null || query == null) return Result.error(ErrorCode.BAD_REQUEST);

        UsersClient usersClient = ServiceFactory.getInstance().getUsersClient(this.domain);
        if (usersClient == null) return Result.error(ErrorCode.INTERNAL_ERROR);

        Result<User> userRes = getUserRobust(usersClient, name, pwd);
        if (userRes == null || !userRes.isOK()) return Result.error(ErrorCode.FORBIDDEN);

        for (int attempt = 0; attempt < MAX_DB_RETRIES; attempt++) {
            try (TxContext tx = hibernate.beginTx()){
                String jpql = "SELECT m.id FROM Message m JOIN m.destination d " +
                        "WHERE d LIKE :userPattern AND " +
                        "(lower(m.subject) LIKE lower(:q) OR lower(m.contents) LIKE lower(:q))";

                var hqlQuery = tx.session().createQuery(jpql, String.class);
                hqlQuery.setParameter("userPattern", "%" + name + "@%");
                hqlQuery.setParameter("q", "%" + query.toLowerCase() + "%");
                List<String> mids = hqlQuery.list();
                tx.commit();
                return Result.ok(mids);
            } catch (Exception x) {
                try { Thread.sleep(50 + (long) (Math.random() * 50)); } catch (InterruptedException ignored) {}
            }
        }
        return Result.error(ErrorCode.INTERNAL_ERROR);
    }

    @Override
    public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
        if (name == null || mid == null || pwd == null) return Result.error(ErrorCode.BAD_REQUEST);

        UsersClient usersClient = ServiceFactory.getInstance().getUsersClient(this.domain);
        if (usersClient == null) return Result.error(ErrorCode.INTERNAL_ERROR);

        Result<User> userRes = getUserRobust(usersClient, name, pwd);
        if (userRes == null || !userRes.isOK()) return Result.error(ErrorCode.FORBIDDEN);

        Object msgLock = messageLocks.computeIfAbsent(mid, k -> new Object());

        synchronized(msgLock) {
            for (int attempt = 0; attempt < MAX_DB_RETRIES; attempt++) {
                try (TxContext tx = hibernate.beginTx()) {
                    Message msg = tx.session().find(Message.class, mid, LockModeType.PESSIMISTIC_WRITE);
                    if (msg == null ) {
                        tx.commit();
                        return Result.error(ErrorCode.NOT_FOUND);
                    }

                    String toRemove = null;
                    if (msg.getDestination() != null) {
                        for (String dest : msg.getDestination()) {
                            if (dest == null) continue;
                            String cleanDest = dest.contains("<") ? dest.substring(dest.indexOf("<") + 1, dest.indexOf(">")) : dest;
                            String[] parts = cleanDest.split("@");
                            if (parts.length > 0 && parts[0].equals(name)) {
                                toRemove = dest;
                                break;
                            }
                        }
                    }

                    if (toRemove == null) {
                        tx.commit();
                        return Result.error(ErrorCode.NOT_FOUND);
                    }

                    Set<String> newDestination = new java.util.HashSet<>(msg.getDestination());
                    newDestination.remove(toRemove);
                    msg.setDestination(newDestination);
                    hibernate.updateTx(tx, msg);
                    tx.commit();
                    return Result.ok();
                } catch (Exception e) {
                    try { Thread.sleep(50 + (long) (Math.random() * 50)); } catch (InterruptedException ignored) {}
                }
            }
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd) {
        if (name == null || mid == null || pwd == null) return Result.error(ErrorCode.BAD_REQUEST);

        if (!pwd.equals("")) {
            UsersClient usersClient = ServiceFactory.getInstance().getUsersClient(this.domain);
            if (usersClient == null) return Result.error(ErrorCode.INTERNAL_ERROR);

            Result<User> userRes = getUserRobust(usersClient, name, pwd);
            if (userRes == null || !userRes.isOK()) return Result.error(ErrorCode.FORBIDDEN);
        }

        boolean dbSuccess = false;
        java.util.List<String> domainsToForward = new java.util.ArrayList<>();

        Object msgLock = messageLocks.computeIfAbsent(mid, k -> new Object());

        synchronized(msgLock) {
            for (int attempt = 0; attempt < MAX_DB_RETRIES; attempt++) {
                try (TxContext tx = hibernate.beginTx()) {
                    Message msg = tx.session().find(Message.class, mid, LockModeType.PESSIMISTIC_WRITE);
                    if (msg == null) {
                        tx.commit();
                        return Result.ok();
                    }

                    String rawSender = msg.getSender();
                    String senderEmail = rawSender.contains("<") ? rawSender.substring(rawSender.indexOf("<") + 1, rawSender.indexOf(">")) : rawSender;
                    String[] senderParts = senderEmail.split("@");
                    if (senderParts.length != 2) {
                        tx.commit();
                        return Result.error(ErrorCode.BAD_REQUEST);
                    }

                    if (!pwd.equals("") && !senderParts[0].equals(name)) {
                        tx.commit();
                        return Result.error(ErrorCode.FORBIDDEN);
                    }

                    if (senderParts[1].equals(this.domain)) {
                        domainsToForward.clear();
                        for (String dest : msg.getDestination()) {
                            String cleanDest = dest.contains("<") ? dest.substring(dest.indexOf("<") + 1, dest.indexOf(">")) : dest;
                            String[] destParts = cleanDest.split("@");
                            if (destParts.length == 2 && !destParts[1].equals(this.domain)) {
                                if (!domainsToForward.contains(destParts[1])) domainsToForward.add(destParts[1]);
                            }
                        }
                    }

                    hibernate.deleteTx(tx, msg);
                    tx.commit();
                    dbSuccess = true;
                    break;
                } catch (Exception e) {
                    try { Thread.sleep(50 + (long) (Math.random() * 50)); } catch (InterruptedException ignored) {}
                }
            }
        }

        if (dbSuccess) {
            for (String dDomain : domainsToForward) {
                // AQUI TAMBÉM: Reverter para a fila certa por Domínio.
                ExecutorService executor = domainExecutors.computeIfAbsent(dDomain, k -> Executors.newSingleThreadExecutor());

                executor.submit(() -> {
                    long startTime = System.currentTimeMillis();
                    long timeout = 95 * 1000;
                    while (System.currentTimeMillis() - startTime < timeout) {
                        try {
                            MessagesClient remoteClient = ServiceFactory.getInstance().getMessagesClient(dDomain);
                            if (remoteClient != null) {
                                Result<?> res = remoteClient.deleteMessage(name, mid, "");
                                if (res != null && (res.isOK() || res.error() == ErrorCode.NOT_FOUND || res.error() == ErrorCode.FORBIDDEN)) {
                                    break;
                                }
                            }
                        } catch (Exception ignored) {}
                        try { Thread.sleep(2000); } catch (InterruptedException e) {}
                    }
                });
            }
            return Result.ok();
        }
        return Result.error(ErrorCode.INTERNAL_ERROR);
    }
}