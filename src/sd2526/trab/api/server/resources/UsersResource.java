package sd2526.trab.api.server.resources;

import java.util.List;
import java.util.logging.Logger;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

import sd2526.trab.api.User;
import sd2526.trab.api.rest.RestUsers;
import sd2526.trab.api.server.persistence.Hibernate;

@Singleton
public class UsersResource implements RestUsers {

    private final Hibernate hibernate;

    private static Logger Log = Logger.getLogger(UsersResource.class.getName());

    private final String domain; // O domínio ao qual este servidor pertence

    public UsersResource(String domain) {
        this.hibernate = Hibernate.getInstance();
        this.domain = domain;
    }

    @Override
    public String postUser(User user) {
        Log.info("postUser : " + user);

        if (user == null || user.getName() == null || user.getDisplayName() == null || user.getPwd() == null || user.getDomain() == null) {
            Log.info("User object invalid.");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        if (!user.getDomain().equals(this.domain)) {
            Log.info("Domain mismatch. Server domain is " + this.domain);
            throw new WebApplicationException(Status.FORBIDDEN);
        }

        try {
            User existingUser = hibernate.get(User.class, user.getName());
            if (existingUser != null) {
                // password and diplayname are the same
                if (existingUser.getPwd().equals(user.getPwd()) &&
                        existingUser.getDisplayName().equals(user.getDisplayName())) {
                    return user.getName() + "@" + user.getDomain();
                } else {
                    // diferent password or displayname(or both)
                    Log.info("User already exists with different data.");
                    throw new WebApplicationException(Status.CONFLICT);
                }
            }
            hibernate.persist(user);
            return user.getName() + "@" + user.getDomain();
        } catch (Exception x) {
            x.printStackTrace();
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public User getUser(String name, String pwd) {
        Log.info("getUser : name = " + name);

        if (name == null || pwd == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        try {
            User user = hibernate.get(User.class, name);
            // user does not exist or password is incorrect
            if (user == null || !user.getPwd().equals(pwd)) {
                Log.info("User does not exist or password is incorrect.");
                throw new WebApplicationException(Status.FORBIDDEN);
            }
            return user;

        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception x) {
            x.printStackTrace();
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public User updateUser(String name, String pwd, User info) {
        Log.info("updateUser : name = " + name);

        if (info == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        User user = getUser(name, pwd);

        // Any field of the provided info containing null should be left unchanged
        if (info.getPwd() != null) user.setPwd(info.getPwd());
        if (info.getDomain() != null) user.setDomain(info.getDomain());
        if (info.getDisplayName() != null) user.setDisplayName(info.getDisplayName());

        try {
            hibernate.update(user);
            return user;
        } catch (Exception x) {
            x.printStackTrace();
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public User deleteUser(String name, String pwd) {
        Log.info("deleteUser : name = " + name);

        User user = getUser(name, pwd);

        try {
            hibernate.delete(user);
            return user;
        } catch (Exception x) {
            x.printStackTrace();
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<User> searchUsers(String name, String pwd, String pattern) {
        Log.info("searchUsers : pattern = " + pattern);

        if (pattern == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        // Only existing users can perform a search.
        getUser(name, pwd);

        try {

            String safePattern = pattern.replace("'", "''");
            String query = "SELECT u FROM User u WHERE lower(u.name) LIKE lower('%" + safePattern + "%')";

            List<User> list = hibernate.jpql(query, User.class);
            list.forEach(u -> u.setPwd(""));

            return list;
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }
}
