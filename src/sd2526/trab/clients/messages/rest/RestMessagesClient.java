package sd2526.trab.clients.messages.rest;

import java.net.URI;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import sd2526.trab.api.Message;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.rest.RestMessages;
import sd2526.trab.clients.messages.java.MessagesClient;
import sd2526.trab.api.java.Result.ErrorCode;

public class RestMessagesClient extends MessagesClient {

    private static final java.util.concurrent.ConcurrentHashMap<String, Client> clients = new java.util.concurrent.ConcurrentHashMap<>();

    final URI serverURI;
    final Client client;
    final ClientConfig config;
    final WebTarget target;

    public RestMessagesClient(URI serverURI) {
        this.serverURI = serverURI;
        this.config = new ClientConfig();

        config.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
        config.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);

        this.client = clients.computeIfAbsent(serverURI.toString(), k -> ClientBuilder.newClient(config));
        this.target = client.target(serverURI).path(RestMessages.PATH);
    }

    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        Response r = executeOperationPost(
                target.queryParam(RestMessages.PWD, pwd)
                        .request()
                        .accept(MediaType.APPLICATION_JSON),
                Entity.entity(msg, MediaType.APPLICATION_JSON)
        );

        if (r == null) return Result.error(ErrorCode.TIMEOUT);

        int status = r.getStatus();
        if (status != Response.Status.OK.getStatusCode())
            return Result.error(getErrorCodeFrom(status));

        return Result.ok(r.readEntity(String.class));
    }

    @Override
    public Result<Message> getInboxMessage(String name, String mid, String pwd) {
        Response r = executeOperationGet(
                target.path("mbox").path(name).path(mid)
                        .queryParam(RestMessages.PWD, pwd)
                        .request()
                        .accept(MediaType.APPLICATION_JSON)
        );

        if (r == null) return Result.error(ErrorCode.TIMEOUT);

        int status = r.getStatus();
        if (status != Response.Status.OK.getStatusCode())
            return Result.error(getErrorCodeFrom(status));

        return Result.ok(r.readEntity(Message.class));
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String name, String pwd) {
        return searchInbox(name, pwd, "");
    }

    @Override
    public Result<List<String>> searchInbox(String name, String pwd, String query) {
        Response r = executeOperationGet(
                target.path("mbox").path(name)
                        .queryParam(RestMessages.PWD, pwd)
                        .queryParam(RestMessages.QUERY, query)
                        .request()
                        .accept(MediaType.APPLICATION_JSON)
        );

        if (r == null) return Result.error(ErrorCode.TIMEOUT);

        int status = r.getStatus();
        if (status != Response.Status.OK.getStatusCode())
            return Result.error(getErrorCodeFrom(status));

        return Result.ok(r.readEntity(new GenericType<>() {}));
    }

    @Override
    public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
        Response r = executeOperationDelete(
                target.path("mbox").path(name).path(mid)
                        .queryParam(RestMessages.PWD, pwd)
                        .request()
        );

        if (r == null) return Result.error(ErrorCode.TIMEOUT);

        int status = r.getStatus();
        // Operações de remoção costumam retornar NO_CONTENT (204)
        if (status != Response.Status.NO_CONTENT.getStatusCode() && status != Response.Status.OK.getStatusCode())
            return Result.error(getErrorCodeFrom(status));

        return Result.ok();
    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd) {
        Response r = executeOperationDelete(
                target.path(name).path(mid)
                        .queryParam(RestMessages.PWD, pwd)
                        .request()
        );

        if (r == null) return Result.error(ErrorCode.TIMEOUT);

        int status = r.getStatus();
        if (status != Response.Status.NO_CONTENT.getStatusCode() && status != Response.Status.OK.getStatusCode())
            return Result.error(getErrorCodeFrom(status));

        return Result.ok();
    }
}