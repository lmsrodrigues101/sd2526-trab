package sd2526.trab.clients.users.rest;

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
import sd2526.trab.api.User;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.rest.RestUsers;
import sd2526.trab.clients.users.java.UsersClient;
import sd2526.trab.api.java.Result.ErrorCode;

public class RestUsersClient extends UsersClient {

    private static final java.util.concurrent.ConcurrentHashMap<String, Client> clients = new java.util.concurrent.ConcurrentHashMap<>();

    final URI serverURI;
    final Client client;
    final ClientConfig config;

    final WebTarget target;


    public RestUsersClient(URI serverURI) {
        this.serverURI = serverURI;

        this.config = new ClientConfig();

        config.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
        config.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);

        this.client = clients.computeIfAbsent(serverURI.toString(), k -> ClientBuilder.newClient(config));

        target = client.target(serverURI).path(RestUsers.PATH);
    }



    public Result<String> postUser(User user) {
        Response r = executeOperationPost(target.request().accept(MediaType.APPLICATION_JSON),
                Entity.entity(user, MediaType.APPLICATION_JSON));

        if (r == null)
            return Result.error(ErrorCode.TIMEOUT);

        int status = r.getStatus();
        if (status != Response.Status.OK.getStatusCode())
            return Result.error(getErrorCodeFrom(status));
        else
            return Result.ok(r.readEntity(String.class));
    }


    public Result<User> getUser(String name, String pwd) {
        Response r = executeOperationGet(target.path(name)
                .queryParam("pwd", pwd)
                .request()
                .accept(MediaType.APPLICATION_JSON));

        if (r == null)
            return Result.error(ErrorCode.TIMEOUT);

        int status = r.getStatus();
        if (status != Response.Status.OK.getStatusCode())
            return Result.error(getErrorCodeFrom(status));
        else
            return Result.ok(r.readEntity(User.class));
    }


    public Result<User> updateUser(String name, String pwd, User info) {
        Response r = executeOperationPut(target.path(name)
                        .queryParam("pwd", pwd).request()
                        .accept(MediaType.APPLICATION_JSON),
                Entity.entity(info, MediaType.APPLICATION_JSON));

        if (r == null)
            return Result.error(ErrorCode.TIMEOUT);

        int status = r.getStatus();
        if (status != Response.Status.OK.getStatusCode())
            return Result.error(getErrorCodeFrom(status));
        else
            return Result.ok(r.readEntity(User.class));
    }
    public Result<User> deleteUser(String name, String pwd) {

        Response r = executeOperationDelete(target.path(name)
                .queryParam("pwd", pwd)
                .request()
                .accept(MediaType.APPLICATION_JSON));

        int status = r.getStatus();
        if (status != Response.Status.OK.getStatusCode())
            return Result.error(getErrorCodeFrom(status));
        else
            return Result.ok(r.readEntity(User.class));

    }
    public Result<List<User>> searchUsers(String name, String pwd, String query) {
        Response r = executeOperationGet(target
                .queryParam("name", name)
                .queryParam("pwd", pwd)
                .queryParam("query", query)
                .request()
                .accept(MediaType.APPLICATION_JSON));

        if (r == null)
            return Result.error(ErrorCode.TIMEOUT);

        int status = r.getStatus();
        if (status != Response.Status.OK.getStatusCode())
            return Result.error(getErrorCodeFrom(status));
        else
            return Result.ok(r.readEntity(new GenericType<>() {}));
    }

}