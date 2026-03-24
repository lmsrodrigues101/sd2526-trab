package sd2526.trab.server.rest;

import sd2526.trab.api.User;
import sd2526.trab.api.java.Users;
import sd2526.trab.server.java.JavaUsers;
import sd2526.trab.api.rest.RestUsers;

import java.util.List;

public class RestUsersResource extends RestResource implements RestUsers {

    final Users impl;

    public RestUsersResource(String domain) {

        this.impl = new JavaUsers(domain);
    }

    @Override
    public String postUser(User user) {
        return unwrapResultOrThrow(impl.postUser(user));
    }

    @Override
    public User getUser(String name, String pwd) {
        return unwrapResultOrThrow(impl.getUser(name, pwd));
    }

    @Override
    public User updateUser(String name, String pwd, User user) {
        return unwrapResultOrThrow(impl.updateUser(name, pwd, user));
    }

    @Override
    public User deleteUser(String name, String pwd) {
        return unwrapResultOrThrow(impl.deleteUser(name, pwd));
    }

    @Override
    public List<User> searchUsers(String name, String pwd, String query) {
        return unwrapResultOrThrow(impl.searchUsers(name, pwd, query));
    }
}
