package sd2526.trab.clients.rest;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

import io.grpc.Status;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.rest.RestMessages;

public class RestMessagesClient extends RestClient implements Messages {

    private static Logger Log = Logger.getLogger(RestMessagesClient.class.getName());

    public RestMessagesClient(URI serverURI) {
        super(serverURI);
        target = super.target.path(RestMessages.PATH);
    }

    @Override
    public Result<String> postMessage(String pwd, Message msg)  {
        return super.reTry(() -> doPostMessage(pwd, msg));
    }

    public Result<String> doPostMessage(String pwd, Message msg) {
        Response r = target.queryParam(RestMessages.PWD, pwd)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(msg, MediaType.APPLICATION_JSON));
        return super.processResponse(r, String.class);
    }

    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd)  {
        return super.reTry(() -> doGetInboxMessage(name, mid, pwd));
    }

    public Result<Message> doGetInboxMessage(String name, String mid, String pwd) {
        Response r = target.path(RestMessages.MBOX).path(name).path(mid)
                .queryParam(RestMessages.PWD, pwd)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        return super.processResponse(r, Message.class);
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String name, String pwd) {
        return searchInbox(name, pwd, "");
    }

    @Override
    public Result<List<String>> searchInbox(String name, String pwd, String query)  {
        return super.reTry(() -> doSearchInbox(name, pwd, query));
    }

    public Result<List<String>> doSearchInbox(String name, String pwd, String query) {
        Response r = target.path(RestMessages.MBOX).path(name)
                .queryParam(RestMessages.PWD, pwd)
                .queryParam(RestMessages.QUERY, query)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        return super.processResponse(r, new GenericType<>() {});
    }

    @Override
    public Result<Void> removeInboxMessage(String name, String mid, String pwd)  {
        return super.reTry(() -> doRemoveInboxMessage(name, mid, pwd));
    }

    public Result<Void> doRemoveInboxMessage(String name, String mid, String pwd) {
        Response r = target.path(RestMessages.MBOX).path(name).path(mid)
                .queryParam(RestMessages.PWD, pwd)
                .request()
                .delete();
        return super.processResponse(r, Void.class);
    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd) {
        return super.reTry(() -> doDeleteMessage(name, mid, pwd));
    }

    public Result<Void> doDeleteMessage(String name, String mid, String pwd) {
        Response r = target.path(name).path(mid)
                .queryParam(RestMessages.PWD, pwd)
                .request()
                .delete();
        return super.processResponse(r, Void.class);
    }
}