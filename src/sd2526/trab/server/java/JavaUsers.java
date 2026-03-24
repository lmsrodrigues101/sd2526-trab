package sd2526.trab.server.java;

import jakarta.validation.ConstraintViolationException;
import sd2526.trab.api.User;
import java.util.List;

import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Users;
import sd2526.trab.api.java.Result.ErrorCode;

import sd2526.trab.api.server.persistence.Hibernate;



public class JavaUsers implements Users {

    private final Hibernate hibernate;
    private final String domain;

    public JavaUsers(String domain) {
        this.hibernate = Hibernate.getInstance();
        this.domain = domain;
    }

    @Override
    public Result<String> postUser(User user) {
        if (user.getName() == null || user.getPwd() == null || user.getDisplayName() == null || user.getDomain() == null) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }
        if (!user.getDomain().equals(this.domain)) {
            return Result.error(ErrorCode.FORBIDDEN);
        }
        try {
            hibernate.persist(user);
        } catch (ConstraintViolationException e) {
            return Result.error(ErrorCode.CONFLICT);
        }catch (Exception e) {
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok(user.getName()+"@"+user.getDomain());
    }


    @Override
    public Result<User> getUser(String name, String pwd) {
        if (name == null || pwd == null) return Result.error(ErrorCode.BAD_REQUEST);
        try {
            User user = hibernate.get(User.class, name);
            // user does not exist or password is incorrect
            if (user == null || !user.getPwd().equals(pwd)) {
                return Result.error(ErrorCode.FORBIDDEN);
            }
            return Result.ok(user);
        } catch (Exception e) {
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<User> updateUser(String name, String pwd, User info) {
        if (name == null || pwd == null || info == null) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }
        try {
            User user = hibernate.get(User.class, name);

            if (user == null || !user.getPwd().equals(pwd)) {
                return Result.error(ErrorCode.FORBIDDEN);
            }

            if (info.getPwd() != null) user.setPwd(info.getPwd());
            if (info.getDisplayName() != null) user.setDisplayName(info.getDisplayName());
            if (info.getDomain() != null) user.setDomain(info.getDomain());
            
            hibernate.update(user);
            return Result.ok(user);

        } catch (Exception e) {
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<User> deleteUser(String name, String pwd) {
        if (name == null || pwd == null) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        try {
            User user = hibernate.get(User.class, name);

            if (user == null || !user.getPwd().equals(pwd)) {
                return Result.error(ErrorCode.FORBIDDEN);
            }
            hibernate.delete(user);
            return Result.ok(user);

        } catch (Exception e) {
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<List<User>> searchUsers(String name, String pwd, String query) {
        if (name == null || pwd == null || query == null){
            return Result.error(ErrorCode.BAD_REQUEST);
        }
        try {
            User user = hibernate.get(User.class, name);
            if (user == null || !user.getPwd().equals(pwd)) {
                return Result.error(ErrorCode.FORBIDDEN);
            }
            String jpql = "SELECT u FROM User u WHERE " +
                    "lower(u.name) LIKE lower(:query) OR " +
                    "lower(u.displayName) LIKE lower(:query)";
            List<User> users = hibernate.jpql(jpql.replace(":query", "'%" + query + "%'"), User.class);

           for (User u : users) {
                u.setPwd("");
            }
           return Result.ok(users);
        } catch (Exception e) {
            return Result.error(ErrorCode.INTERNAL_ERROR);
        }
    }
}
