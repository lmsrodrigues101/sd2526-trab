package sd2526.trab.clients.rest; // Ajusta ao teu package

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Users;
import sd2526.trab.api.rest.RestUsers;

public class RestUsersClient extends RestClient implements Users {



    public RestUsersClient(URI serverURI) {
        super(serverURI);
       target = target.path(RestUsers.PATH);
    }


    @Override
    public Result<String> postUser(User user) {
        return super.reTry(() -> doPostUser(user));
    }

    private Result<String> doPostUser(User user) {
        Response r = target.request()
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(user, MediaType.APPLICATION_JSON));

        return super.processResponse(r, String.class);
    }

    @Override
    public Result<User> getUser(String name, String pwd) {
        return super.reTry(() -> clt_getUser(name, pwd));
    }

    private Result<User> clt_getUser(String name, String pwd) {
        // Assume que o GET é feito em: /users/{name}?pwd={pwd}
        Response r = target.path(name)
                .queryParam("pwd", pwd)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        return super.processResponse(r, User.class);
    }

    @Override
    public Result<User> updateUser(String name, String pwd, User info) {
        return super.reTry(() -> doUpdateUser(name, pwd, info));
    }

    private Result<User> doUpdateUser(String name, String pwd, User info) {
         Response r = target.path(name)
                .queryParam("pwd", pwd)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .put(Entity.entity(info, MediaType.APPLICATION_JSON));

        return super.processResponse(r, User.class);
    }

    @Override
    public Result<User> deleteUser(String name, String pwd) {
        return super.reTry(() -> doDeleteUser(name, pwd));
    }

    private Result<User> doDeleteUser(String name, String pwd) {
         Response r = target.path(name)
                .queryParam("pwd", pwd)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .delete();

        return super.processResponse(r, User.class);
    }


    @Override
    public Result<List<User>> searchUsers(String name, String pwd, String query) {
        return super.reTry(() -> doSearchUsers(name, pwd, query));
    }

    private Result<List<User>> doSearchUsers(String name, String pwd, String query) {
       Response r = target
                .queryParam("name", name) // Ver na tua interface RestUsers se é "name" ou se vai no Path
                .queryParam("pwd", pwd)
                .queryParam("query", query)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        return super.processResponse(r, new GenericType<>() {});
    }
}