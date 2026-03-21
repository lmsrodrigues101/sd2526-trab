package sd2526.trab.api.server.resources;

import java.net.URI;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.List;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

import sd2526.trab.api.Message;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.rest.RestMessages;
import sd2526.trab.api.server.clients.rest.RestUsersClient;
import sd2526.trab.api.server.persistence.Hibernate;

@Singleton
public class MessagesResource implements RestMessages {

    private static Logger Log = Logger.getLogger(MessagesResource.class.getName());

    private final Hibernate hibernate;
    private final String domain;
    private final RestUsersClient usersClient; // Para podermos validar o remetente

    public MessagesResource(String domain, String usersServerUri) {
        this.hibernate = Hibernate.getInstance();
        this.domain = domain;
        this.usersClient = new RestUsersClient(URI.create(usersServerUri));
    }

    @Override
    public String postMessage(String pwd, Message msg) {
        Log.info("postMessage : " + msg.getSubject());

        if(pwd== null || msg == null || msg.getSender() == null || msg.getDestination() == null || msg.getDestination().isEmpty()) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        // sender can be in the form  "name" or "name@domain"
        String senderName = msg.getSender();
        if( senderName.contains("@")){
            // if contains @, split to be just sara instead of sara@...
            senderName = senderName.split("@")[0];
        }

        Result<User> userResult = usersClient.getUser(senderName,pwd);
        if (!userResult.isOK()) {
            Log.info("User authentication failed.");
            throw new WebApplicationException(Status.FORBIDDEN);
        }

        User senderUser = userResult.value();

        try{
            // give a random id if the message does not have one
            // assigning it an ID
            if(msg.getId() == null || msg.getId().isEmpty()){
                msg.setId(UUID.randomUUID().toString());
            }
            // changing the sender to be in the format "display name <name@domain>"
            String formattedSender = String.format("%s <%s@%s>",senderUser.getDisplayName(), senderUser.getName(), senderUser.getDomain());
            msg.setSender(formattedSender);

            //destination is the users we send the message
            //inboxUsers is the users that has the message inbox
            if (msg.getInboxUsers() == null || msg.getInboxUsers().isEmpty()) {
                msg.setInboxUsers(msg.getDestination());
            }
            hibernate.persist(msg);
            return msg.getId();

        } catch( Exception e){
            e.printStackTrace();
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Message getMessage(String name, String mid, String pwd) {
        Log.info("getMessage : name = " + name + " ; mid = " + mid);

        if (name == null || mid == null || pwd == null) {
            Log.info("Missing parameters.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        Result<User> userResult = usersClient.getUser(name, pwd);
        if (!userResult.isOK()) {
            Log.info("User authentication failed.");
            throw new WebApplicationException(Status.FORBIDDEN);
        }
        try {
            Message msg = hibernate.get(Message.class, mid);

            if (msg == null || !msg.getInboxUsers().contains(name)) {
                Log.info("Message does not exist or is not in this user's inbox.");
                throw new WebApplicationException(Status.NOT_FOUND);
            }
            return msg;
        } catch (Exception x) {
            x.printStackTrace();
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<String> getMessages(String name, String pwd, String query) {
        Log.info("getMessages : name = " + name + " ; query = " + query);

        if (name == null || pwd == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        Result<User> userResult = usersClient.getUser(name, pwd);
        if (!userResult.isOK()) {
            Log.info("User authentication failed.");
            throw new WebApplicationException(Status.FORBIDDEN);
        }

        try {
            String jpql;

            // If the query is empty, returns all message ids in the user's inbox.
            if (query == null || query.isEmpty()) {
                jpql = String.format("SELECT m.id FROM Message m WHERE '%s' MEMBER OF m.inboxUsers", name);
            } else {
                //Otherwise, a message matches when the query is a substring of the subject or contents, case-insensitive
                String safeQuery = query.replace("'", "''");

                jpql = String.format(
                        "SELECT m.id FROM Message m WHERE '%s' MEMBER OF m.inboxUsers AND " +
                                "(lower(m.subject) LIKE lower('%%%s%%') OR lower(m.contents) LIKE lower('%%%s%%'))",
                        name, safeQuery, safeQuery
                );
            }

            return hibernate.jpql(jpql, String.class);

        } catch (Exception x) {
            x.printStackTrace();
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void removeFromUserInbox(String name, String mid, String pwd) {
        Log.info("removeFromUserInbox : name = " + name + " ; mid = " + mid);

        if (name == null || mid == null || pwd == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }


        Result<User> userResult = usersClient.getUser(name, pwd);
        if (!userResult.isOK()) {
            Log.info("User authentication failed.");
            throw new WebApplicationException(Status.FORBIDDEN);
        }

        try {
            Message msg = hibernate.get(Message.class, mid);

            if (msg == null || !msg.getInboxUsers().contains(name)) {
                Log.info("User authentication failed.");
                throw new WebApplicationException(Status.NOT_FOUND);
            }

            msg.getInboxUsers().remove(name);

            hibernate.update(msg);

        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception x) {
            x.printStackTrace();
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void deleteMessage(String name, String mid, String pwd) {
        Log.info("deleteMessage : name = " + name + " ; mid = " + mid);

        if (name == null || mid == null || pwd == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        Result<User> userResult = usersClient.getUser(name, pwd);
        if (!userResult.isOK()) {
            Log.info("User authentication failed.");
            throw new WebApplicationException(Status.FORBIDDEN);
        }

        try {
            Message msg = hibernate.get(Message.class, mid);

            if (msg == null) {
                return;
            }
            String rawSender = msg.getSender();
            String senderName = rawSender;
            if (rawSender.contains("@")) {
                senderName = rawSender.substring(rawSender.indexOf('<') + 1, rawSender.indexOf('@'));
            }

            if (!senderName.equals(name)) {
                Log.info("User is not the sender of this message.");
                throw new WebApplicationException(Status.FORBIDDEN);
            }

            long ageInMillis = System.currentTimeMillis() - msg.getCreationTime();
            if (ageInMillis <= 30000) {
                hibernate.delete(msg);
            } else {
                Log.info("Message is older than 30 seconds. Cannot delete globally.");
                }

        } catch (Exception x) {
            x.printStackTrace();
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }

}

