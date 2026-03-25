package sd2526.trab.server.java; // Ajusta ao teu pacote

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import sd2526.trab.api.Message;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;
import sd2526.trab.api.java.Users;
import sd2526.trab.clients.UsersClientFactory;
import sd2526.trab.clients.rest.RestUsersClient;
import sd2526.trab.api.server.persistence.Hibernate;

public class JavaMessages implements Messages {

    private static Logger Log = Logger.getLogger(JavaMessages.class.getName());

    private final Hibernate hibernate;
    private final String domain;

    public JavaMessages(String domain) {
        this.hibernate = Hibernate.getInstance();
        this.domain = domain;
    }

    @Override
    public Result<String> postMessage(String pwd, Message msg){

        if (pwd == null || msg == null || msg.getSender() == null || msg.getDestination() == null || msg.getDestination().isEmpty()) {
            return Result.error(ErrorCode.BAD_REQUEST); // Antes era throw new WebApplicationException...
        }

        String senderName = msg.getSender();
        if (senderName.contains("@")) {
            senderName = senderName.split("@")[0];
        }

        // Validação com o serviço de Users
        Result<User> userResult = UsersClientFactory.get(domain).getUser(senderName, pwd);
        if (!userResult.isOK()) {
            return Result.error(ErrorCode.FORBIDDEN); // Devolvemos erro em vez de exceção
        }

        User senderUser = userResult.value();

        try {
            if (msg.getId() == null || msg.getId().isEmpty()) {
                msg.setId(UUID.randomUUID().toString());
            }

            String formattedSender = String.format("%s <%s@%s>", senderUser.getDisplayName(), senderUser.getName(), senderUser.getDomain());
            msg.setSender(formattedSender);

            if (msg.getInboxUsers() == null || msg.getInboxUsers().isEmpty()) {
                msg.setInboxUsers(msg.getDestination());
            }

            hibernate.persist(msg);

            return Result.ok(msg.getId()); // Devolvemos sucesso com o ID

        } catch (Exception e) {
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd)  {
        if (name == null || mid == null || pwd == null) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        Result<User> userResult = UsersClientFactory.get(domain).getUser(name, pwd);
        if (!userResult.isOK()) {
            return Result.error(ErrorCode.FORBIDDEN);
        }

        try {
            Message msg = hibernate.get(Message.class, mid);

            if (msg == null || !msg.getInboxUsers().contains(name)) {
                return Result.error(ErrorCode.NOT_FOUND);
            }
            return Result.ok(msg);
        } catch (Exception x) {
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String name, String pwd){
        if (name == null || pwd == null) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        Result<User> userResult = UsersClientFactory.get(domain).getUser(name, pwd);
        if (!userResult.isOK()) {
            return Result.error(ErrorCode.FORBIDDEN);
        }

        try {
            String jpql = String.format("SELECT m.id FROM Message m WHERE '%s' MEMBER OF m.inboxUsers", name);
            List<String> mids = hibernate.jpql(jpql, String.class);
            return Result.ok(mids);
        } catch (Exception x) {
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<List<String>> searchInbox(String name, String pwd, String query)  {
        if (name == null || pwd == null || query == null) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        Result<User> userResult = UsersClientFactory.get(domain).getUser(name, pwd);
        if (!userResult.isOK()) {
            return Result.error(ErrorCode.FORBIDDEN);
        }

        try {
            String safeQuery = query.replace("'", "''");
            String jpql = String.format(
                    "SELECT m.id FROM Message m WHERE '%s' MEMBER OF m.inboxUsers AND " +
                            "(lower(m.subject) LIKE lower('%%%s%%') OR lower(m.contents) LIKE lower('%%%s%%'))",
                    name, safeQuery, safeQuery
            );
            List<String> mids = hibernate.jpql(jpql, String.class);
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

        Result<User> userResult = UsersClientFactory.get(domain).getUser(name, pwd);
        if (!userResult.isOK()) {
            return Result.error(ErrorCode.FORBIDDEN);
        }

        try {
            Message msg = hibernate.get(Message.class, mid);

            if (msg == null || !msg.getInboxUsers().contains(name)) {
                return Result.error(ErrorCode.NOT_FOUND);
            }

            msg.getInboxUsers().remove(name);
            hibernate.update(msg);

            return Result.ok();
        } catch (Exception x) {
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd)  {
        if (name == null || mid == null || pwd == null) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        Result<User> userResult = UsersClientFactory.get(domain).getUser(name, pwd);
        if (!userResult.isOK()) {
            return Result.error(ErrorCode.FORBIDDEN);
        }

        try {
            Message msg = hibernate.get(Message.class, mid);

            if (msg == null) {
                return Result.ok();
            }

            String rawSender = msg.getSender();
            String senderName = rawSender;
            if (rawSender.contains("@")) {
                senderName = rawSender.substring(rawSender.indexOf('<') + 1, rawSender.indexOf('@'));
            }

            if (!senderName.equals(name)) {
                return Result.error(ErrorCode.FORBIDDEN);
            }

            long ageInMillis = System.currentTimeMillis() - msg.getCreationTime();
            if (ageInMillis <= 30000) {
                hibernate.delete(msg);
            }

            return Result.ok();
        } catch (Exception x) {
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
    }
}