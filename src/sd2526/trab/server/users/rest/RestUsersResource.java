package sd2526.trab.server.users.rest;

import sd2526.trab.api.User;
import sd2526.trab.api.java.Users;
import sd2526.trab.api.rest.RestUsers;
import sd2526.trab.server.util.RestResource;
import sd2526.trab.server.users.java.JavaUsers;

import java.util.List;


public class RestUsersResource extends RestResource implements RestUsers {

    final Users impl;

    public RestUsersResource() {
        this.impl = new JavaUsers(UsersRestServer.DOMAIN);
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
    public User updateUser(String name, String pwd, User info) {
        return unwrapResultOrThrow(impl.updateUser(name, pwd, info));
    }

    @Override
    public User deleteUser(String name, String pwd) {
        return unwrapResultOrThrow(impl.deleteUser(name, pwd));
    }

    @Override
    public List<User> searchUsers(String name, String pwd, String pattern) {
        return unwrapResultOrThrow(impl.searchUsers(name, pwd, pattern));
    }
}
