package sd2526.trab.server.rest;

import java.util.List;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

import sd2526.trab.api.User;
import sd2526.trab.api.rest.RestUsers;
import sd2526.trab.clients.GatewayRestServer;
import sd2526.trab.server.ServiceFactory;
import sd2526.trab.clients.UsersClient;

public class RestGatewayUsersResource extends RestResource implements RestUsers {

    // Método auxiliar para ir buscar o cliente (REST ou gRPC) do servidor de Users real deste domínio
    private UsersClient getClient() {
        UsersClient client = ServiceFactory.getInstance().getUsersClient(GatewayRestServer.DOMAIN);
        if (client == null) {
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
        return client;
    }

    @Override
    public String postUser(User user) {
        return unwrapResultOrThrow(getClient().postUser(user));
    }

    @Override
    public User getUser(String name, String pwd) {
        return unwrapResultOrThrow(getClient().getUser(name, pwd));
    }

    @Override
    public User updateUser(String name, String pwd, User info) {
        return unwrapResultOrThrow(getClient().updateUser(name, pwd, info));
    }

    @Override
    public User deleteUser(String name, String pwd) {
        return unwrapResultOrThrow(getClient().deleteUser(name, pwd));
    }

    @Override
    public List<User> searchUsers(String name, String pwd, String pattern) {
        return unwrapResultOrThrow(getClient().searchUsers(name, pwd, pattern));
    }
}