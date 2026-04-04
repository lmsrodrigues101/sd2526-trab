package sd2526.trab.clients;

import java.util.List;

import sd2526.trab.api.User;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Users;

public abstract class UsersClient extends ClientOperations implements Users {

    abstract public Result<String> postUser(User user);

    abstract public Result<User> getUser(String name, String pwd);

    abstract public Result<User> updateUser(String name, String pwd, User info);

    abstract public Result<User> deleteUser(String name, String pwd);

    abstract public Result<List<User>> searchUsers(String name, String pwd, String query);

}