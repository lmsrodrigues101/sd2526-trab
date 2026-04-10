package sd2526.trab.server.rest;

import java.util.List;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

import sd2526.trab.api.Message;
import sd2526.trab.api.rest.RestMessages;
import sd2526.trab.clients.GatewayRestServer;
import sd2526.trab.server.ServiceFactory;
import sd2526.trab.clients.MessagesClient;

public class RestGatewayMessagesResource extends RestResource implements RestMessages {

    private MessagesClient getClient() {
        MessagesClient client = ServiceFactory.getInstance().getMessagesClient(GatewayRestServer.DOMAIN);
        if (client == null) {
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
        return client;
    }

    @Override
    public String postMessage(String pwd, Message msg) {
        return unwrapResultOrThrow(getClient().postMessage(pwd, msg));
    }

    @Override
    public Message getMessage(String name, String mid, String pwd) {
        return unwrapResultOrThrow(getClient().getInboxMessage(name, mid, pwd));
    }

    @Override
    public List<String> getMessages(String name, String pwd, String query) {
        if (query == null || query.isEmpty()) {
            return unwrapResultOrThrow(getClient().getAllInboxMessages(name, pwd));
        } else {
            return unwrapResultOrThrow(getClient().searchInbox(name, pwd, query));
        }
    }

    @Override
    public void removeFromUserInbox(String name, String mid, String pwd) {
        unwrapResultOrThrow(getClient().removeInboxMessage(name, mid, pwd));
    }

    @Override
    public void deleteMessage(String name, String mid, String pwd) {
        unwrapResultOrThrow(getClient().deleteMessage(name, mid, pwd));
    }
}