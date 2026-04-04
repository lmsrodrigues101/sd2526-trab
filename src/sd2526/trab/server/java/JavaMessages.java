package sd2526.trab.server.java;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import sd2526.trab.api.Message;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;
import sd2526.trab.server.persistence.Hibernate;
import sd2526.trab.server.ServiceFactory; // Import da nova Factory
import sd2526.trab.clients.UsersClient;
import sd2526.trab.clients.MessagesClient;
import sd2526.trab.server.persistence.TxContext;

public class JavaMessages implements Messages {

    private static Logger Log = Logger.getLogger(JavaMessages.class.getName());

    private final Hibernate hibernate;
    private final String domain;

    public JavaMessages(String domain) {
        this.hibernate = Hibernate.getInstance();
        this.domain = domain;
    }

    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        Log.info(String.format("postMessage: pwd = %s, msg = %s", pwd, msg));

        if (pwd == null || msg == null || msg.getSender() == null
                || msg.getDestination() == null || msg.getDestination().isEmpty()
                || msg.getSubject() == null || msg.getContents() == null) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        String senderStr = msg.getSender();

        // ====================================================================
        // 1. MENSAGEM RECEBIDA DE OUTRO DOMÍNIO (Onde ocorre o Truque)
        // ====================================================================
        if (senderStr.contains("<")) {
            try (TxContext tx = hibernate.beginTx()) {
                Message existing = hibernate.getTx(tx, Message.class, msg.getId());
                if (existing != null) return Result.ok(msg.getId());

                hibernate.persistTx(tx, msg);

                // TRAVÃO: Evita ciclos infinitos. Se já é um Bounce, não valida destinos!
                if (msg.getSubject() != null && msg.getSubject().startsWith("FAILED TO SEND")) {
                    tx.commit();
                    return Result.ok(msg.getId());
                }

                UsersClient usersClient = ServiceFactory.getInstance().getUsersClient(this.domain);
                if (usersClient != null) {
                    for (String dest : msg.getDestination()) {
                        String cleanDest = dest.contains("<") ? dest.substring(dest.indexOf("<") + 1, dest.indexOf(">")) : dest;
                        String[] parts = cleanDest.split("@");
                        if (parts.length != 2) continue;

                        String destName = parts[0];
                        String destDomain = parts[1];

                        if (destDomain.equals(this.domain)) {
                            // 🎯 TRUQUE DE MESTRE: Forçar um CONFLICT para saber se existe (não requer auth!)
                            sd2526.trab.api.User dummyUser = new sd2526.trab.api.User();
                            dummyUser.setName(destName);
                            dummyUser.setPwd("temp_dummy_pwd");
                            dummyUser.setDomain(destDomain);
                            dummyUser.setDisplayName("Dummy");

                            Result<String> createRes = usersClient.postUser(dummyUser);

                            if (createRes.isOK()) {
                                // O utilizador NÃO EXISTIA! (Acabámos de o criar). Apagamos já:
                                usersClient.deleteUser(destName, "temp_dummy_pwd");

                                // Gerar o Bounce para enviar de volta!
                                Message failMsg = createFailedNotificationObj(msg, dest, "UNKNOWN USER");
                                String originalSenderEmail = failMsg.getDestination().iterator().next();
                                String originalSenderDomain = originalSenderEmail.split("@")[1];

                                // Enviar de volta ao servidor de origem
                                java.util.concurrent.CompletableFuture.runAsync(() -> {
                                    try {
                                        MessagesClient remoteClient = ServiceFactory.getInstance().getMessagesClient(originalSenderDomain);
                                        if (remoteClient != null) {
                                            // Envia sem password (é servidor-a-servidor, vai ter < >)
                                            remoteClient.postMessage("", failMsg);
                                        }
                                    } catch (Exception ignored) {}
                                });
                            }
                            // Se deu CONFLICT (ou seja, não é isOK), o utilizador já lá estava! Tudo certo.
                        }
                    }
                }

                tx.commit();
                return Result.ok(msg.getId());
            } catch (Exception e) {
                try (TxContext tx2 = hibernate.beginTx()) {
                    if (hibernate.getTx(tx2, Message.class, msg.getId()) != null) return Result.ok(msg.getId());
                } catch (Exception ignored) {}
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
        }

        // ====================================================================
        // 2. MENSAGEM NOVA CRIADA NESTE DOMÍNIO (O teu código original e perfeito)
        // ====================================================================
        String senderName = senderStr.contains("@") ? senderStr.split("@")[0] : senderStr;

        UsersClient usersClient = ServiceFactory.getInstance().getUsersClient(this.domain);
        if (usersClient == null) return Result.error(ErrorCode.INTERNAL_ERROR);

        Result<User> userRes = usersClient.getUser(senderName, pwd);
        if (!userRes.isOK()) return Result.error(ErrorCode.FORBIDDEN);

        User sender = userRes.value();
        msg.setSender(String.format("%s <%s@%s>", sender.getDisplayName(), sender.getName(), sender.getDomain()));

        if (msg.getId() == null || msg.getId().isEmpty()) {
            String uniqueContent = msg.getSender() + msg.getSubject() + msg.getContents() + msg.getCreationTime();
            msg.setId(java.util.UUID.nameUUIDFromBytes(uniqueContent.getBytes()).toString());
        }
        if (msg.getCreationTime() == 0) {
            msg.setCreationTime(System.currentTimeMillis());
        }

        try (TxContext tx = hibernate.beginTx()) {
            Message existing = hibernate.getTx(tx, Message.class, msg.getId());
            if (existing != null) return Result.ok(msg.getId());

            hibernate.persistTx(tx, msg);

            java.util.Map<String, java.util.List<String>> remoteDomains = new java.util.HashMap<>();

            for (String dest : msg.getDestination()) {
                String cleanDest = dest.contains("<") ? dest.substring(dest.indexOf("<") + 1, dest.indexOf(">")) : dest;
                String[] parts = cleanDest.split("@");
                if (parts.length != 2) continue;

                String destName = parts[0];
                String destDomain = parts[1];

                if (destDomain.equals(this.domain)) {
                    // Validar Utilizador Local (porque aqui temos password do sender!)
                    boolean destExists = false;
                    var searchRes = usersClient.searchUsers(senderName, pwd, destName);
                    if (searchRes.isOK() && searchRes.value() != null) {
                        for (User u : searchRes.value()) {
                            if (u.getName().equals(destName)) {
                                destExists = true;
                                break;
                            }
                        }
                    }
                    if (!destExists) {
                        Message failMsg = createFailedNotificationObj(msg, dest, "UNKNOWN USER");
                        hibernate.persistTx(tx, failMsg);
                    }
                } else {
                    remoteDomains.computeIfAbsent(destDomain, k -> new java.util.ArrayList<>()).add(dest);
                }
            }

            tx.commit();

            for (java.util.Map.Entry<String, java.util.List<String>> entry : remoteDomains.entrySet()) {
                forwardToRemoteDomain(entry.getKey(), msg, pwd);
            }

            return Result.ok(msg.getId());

        } catch (Exception e) {
            Log.severe("Erro na persistência do postMessage: " + e.getMessage());
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
    }

    // ====================================================================
    // MÉTODOS AUXILIARES
    // ====================================================================

    private Message createFailedNotificationObj(Message originalMsg, String failedUser, String reason) {
        String rawSender = originalMsg.getSender();
        String senderEmail = rawSender.contains("<") ? rawSender.substring(rawSender.indexOf("<") + 1, rawSender.indexOf(">")) : rawSender;

        Message failMsg = new Message();
        failMsg.setId(originalMsg.getId() + "." + failedUser);
        failMsg.setSender(originalMsg.getSender()); // Mantém o formato "Nome <email>"
        failMsg.setDestination(java.util.Set.of(senderEmail));
        failMsg.setSubject(String.format("FAILED TO SEND %s TO %s: %s", originalMsg.getId(), failedUser, reason));
        failMsg.setContents(originalMsg.getContents());
        failMsg.setCreationTime(System.currentTimeMillis());

        return failMsg;
    }

    private void forwardToRemoteDomain(String remoteDomain, Message msg, String pwd) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                MessagesClient remoteClient = ServiceFactory.getInstance().getMessagesClient(remoteDomain);
                if (remoteClient != null) {
                    remoteClient.postMessage(pwd, msg);
                }
            } catch (Exception ignored) {}
        });
    }

    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd) {
        Log.info("getInboxMessage: " + name + " / " + mid);
        if (name == null || mid == null || pwd == null) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }
        UsersClient uc = ServiceFactory.getInstance().getUsersClient(this.domain);
        if (uc == null) return Result.error(ErrorCode.INTERNAL_ERROR);

        Result<User> userRes = uc.getUser(name, pwd);
        if (!userRes.isOK()) {
            return Result.error(ErrorCode.FORBIDDEN);
        }
        try (TxContext tx = hibernate.beginTx()) {
            Message msg = hibernate.getTx(tx, Message.class, mid);
            if (msg == null || msg.getDestination().stream().noneMatch(d -> d.split("@")[0].equals(name))) {
                return Result.error(ErrorCode.NOT_FOUND);
            }Message cleanMsg = new Message(
                    msg.getId(),
                    msg.getSender(),
                    new java.util.HashSet<>(msg.getDestination()),
                    msg.getSubject(),
                    msg.getContents()
            );
            cleanMsg.setCreationTime(msg.getCreationTime());

            tx.commit();
            return Result.ok(cleanMsg);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
    }


    @Override
    public Result<List<String>> getAllInboxMessages(String name, String pwd){
        if (name == null || pwd == null) return Result.error(ErrorCode.BAD_REQUEST);

        UsersClient usersClient = ServiceFactory.getInstance().getUsersClient(domain);
        if (usersClient == null) return Result.error(ErrorCode.INTERNAL_ERROR);

        Result<User> userResult = usersClient.getUser(name, pwd);
        if (!userResult.isOK()) return Result.error(ErrorCode.FORBIDDEN);

        try (TxContext tx = hibernate.beginTx()) {
            String jpql = "SELECT m.id FROM Message m JOIN m.destination d WHERE d LIKE :userPattern";
            var query = tx.session().createQuery(jpql, String.class);
            query.setParameter("userPattern", name + "@%");
            List<String> mids = query.list();
            tx.commit();
            return Result.ok(mids);

        } catch (Exception x) {
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<List<String>> searchInbox(String name, String pwd, String query)  {
        if (name == null || pwd == null || query == null) return Result.error(ErrorCode.BAD_REQUEST);

        UsersClient usersClient = ServiceFactory.getInstance().getUsersClient(domain);
        if (usersClient == null) return Result.error(ErrorCode.INTERNAL_ERROR);

        Result<User> userResult = usersClient.getUser(name, pwd);
        if (!userResult.isOK()) return Result.error(ErrorCode.FORBIDDEN);

        try (TxContext tx = hibernate.beginTx()){
            String jpql = "SELECT m.id FROM Message m JOIN m.destination d " +
                    "WHERE d LIKE :userPattern AND " +
                    "(lower(m.subject) LIKE lower(:q) OR lower(m.contents) LIKE lower(:q))";

            var hqlQuery = tx.session().createQuery(jpql, String.class);
            hqlQuery.setParameter("userPattern", name + "@%");
            hqlQuery.setParameter("q", "%" + query.toLowerCase() + "%");
            List<String> mids = hqlQuery.list();
            tx.commit();
            return Result.ok(mids);
        } catch (Exception x) {
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
        if (name == null || mid == null || pwd == null) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        UsersClient uc = ServiceFactory.getInstance().getUsersClient(this.domain);
        if (uc == null) return Result.error(ErrorCode.INTERNAL_ERROR);

        Result<User> uRes = uc.getUser(name, pwd);
        if (!uRes.isOK()) return Result.error(ErrorCode.FORBIDDEN);

        try (TxContext tx = hibernate.beginTx()) {
            Message msg = hibernate.getTx(tx, Message.class, mid);
            if (msg == null ) {
                return Result.error(ErrorCode.NOT_FOUND);
            }
            String toRemove = null;
            for (String dest : msg.getDestination()) {
                String cleanDest = dest.contains("<") ? dest.substring(dest.indexOf("<") + 1, dest.indexOf(">")) : dest;
                if (cleanDest.split("@")[0].equals(name)) {
                    toRemove = dest;
                    break;
                }
            }if (toRemove == null) {
                return Result.error(ErrorCode.NOT_FOUND);
            }Set<String> newDestination = new java.util.HashSet<>(msg.getDestination());
            newDestination.remove(toRemove);
            msg.setDestination(newDestination); // Acorda o "Dirty Checking"
            hibernate.updateTx(tx, msg);
            tx.commit();
            return Result.ok();
        } catch (Exception e) {
            Log.severe("Erro ao remover mensagem " + mid + " da inbox de " + name + ": " + e.getMessage());
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd) {
        Log.info(String.format("deleteMessage: name=%s, mid=%s", name, mid));
        if (name == null || mid == null || pwd == null) return Result.error(ErrorCode.BAD_REQUEST);

        try (TxContext tx = hibernate.beginTx()) {
            Message msg = hibernate.getTx(tx, Message.class, mid);
            if (msg == null) return Result.ok();

            String rawSender = msg.getSender();
            String senderEmail = rawSender;
            if (rawSender.contains("<") && rawSender.contains(">")) {
                senderEmail = rawSender.substring(rawSender.indexOf("<") + 1, rawSender.indexOf(">"));
            }

            String[] senderParts = senderEmail.split("@");
            if (senderParts.length != 2) return Result.error(ErrorCode.BAD_REQUEST);
            String sName = senderParts[0];
            String sDomain = senderParts[1];

            if (sDomain.equals(this.domain)) {
                UsersClient usersClient = ServiceFactory.getInstance().getUsersClient(this.domain);
                if (usersClient == null) return Result.error(ErrorCode.INTERNAL_ERROR);

                var userRes = usersClient.getUser(name, pwd);
                if (!userRes.isOK()) return Result.error(ErrorCode.FORBIDDEN);
                if (!sName.equals(name)) return Result.error(ErrorCode.FORBIDDEN);

                for (String dest : msg.getDestination()) {
                    String cleanDest = dest.contains("<") ? dest.substring(dest.indexOf("<") + 1, dest.indexOf(">")) : dest;
                    String[] destParts = cleanDest.split("@");
                    if (destParts.length != 2) continue;
                    String dDomain = destParts[1];

                    if (!dDomain.equals(this.domain)) {
                        java.util.concurrent.CompletableFuture.runAsync(() -> {
                            try {
                                MessagesClient remoteClient = ServiceFactory.getInstance().getMessagesClient(dDomain);
                                if (remoteClient != null) remoteClient.deleteMessage(name, mid, pwd);
                            } catch (Exception ignored) {}
                        });
                    }
                }
            } else {
                if (!sName.equals(name)) return Result.error(ErrorCode.FORBIDDEN);
            }

            hibernate.deleteTx(tx, msg);
            tx.commit();
            return Result.ok();

        } catch (Exception e) {
            Log.severe("Erro ao apagar mensagem " + mid + ": " + e.getMessage());
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
    }
}