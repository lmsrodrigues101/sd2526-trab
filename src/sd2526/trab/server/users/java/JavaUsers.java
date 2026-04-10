package sd2526.trab.server.users.java;

import sd2526.trab.api.User;
import java.util.List;
import java.util.logging.Logger;
import jakarta.persistence.LockModeType;

import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Users;
import sd2526.trab.api.java.Result.ErrorCode;
import sd2526.trab.server.persistence.Hibernate;
import sd2526.trab.server.persistence.TxContext;
import sd2526.trab.server.users.rest.UsersRestServer;

public class JavaUsers implements Users {

    private static Logger Log = Logger.getLogger(UsersRestServer.class.getName());
    private final Hibernate hibernate;
    private final String domain;

    private static final int MAX_DB_RETRIES = 50;

    public JavaUsers(String domain) {
        this.hibernate = Hibernate.getInstance();
        this.domain = domain;
    }

    @Override
    public Result<String> postUser(User user) {
        if (user.getName() == null || user.getName().isBlank() || user.getPwd() == null
                || user.getPwd().isBlank() || user.getDisplayName() == null || user.getDisplayName().isBlank()
                || user.getDomain() == null || user.getDomain().isBlank()){
            return Result.error(ErrorCode.BAD_REQUEST);
        }
        if (!user.getDomain().equals(this.domain)) {
            return Result.error(ErrorCode.FORBIDDEN);
        }

        for (int attempt = 0; attempt < MAX_DB_RETRIES; attempt++) {
            try (TxContext tx = hibernate.beginTx()) {
                User existingUser = tx.session().find(User.class, user.getName(), LockModeType.PESSIMISTIC_WRITE);
                if (existingUser != null) {
                    if (existingUser.getPwd().equals(user.getPwd()) &&
                            existingUser.getDisplayName().equals(user.getDisplayName())) {
                        tx.commit();
                        return Result.ok(user.getName() + "@" + user.getDomain());
                    } else {
                        tx.commit();
                        return Result.error(ErrorCode.CONFLICT);
                    }
                }
                hibernate.persistTx(tx, user);
                tx.commit();
                return Result.ok(user.getName() + "@" + user.getDomain());
            } catch (Exception e) {
                if (attempt == MAX_DB_RETRIES - 1) {
                    return Result.error(ErrorCode.INTERNAL_ERROR);
                }
                try { Thread.sleep(10 + (long)(Math.random() * 50)); } catch (Exception ignored) {}
            }
        }
        return Result.error(ErrorCode.INTERNAL_ERROR);
    }

    @Override
    public Result<User> getUser(String name, String pwd) {
        if (name == null || pwd == null) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }
        for (int attempt = 0; attempt < MAX_DB_RETRIES; attempt++) {
            try (TxContext tx = hibernate.beginTx()) {
                User user = hibernate.getTx(tx, User.class, name);

                if (user == null || !user.getPwd().equals(pwd)) {
                    tx.commit();
                    return Result.error(ErrorCode.FORBIDDEN);
                }
                tx.commit();
                return Result.ok(user);
            } catch (Exception e) {
                if (attempt == MAX_DB_RETRIES - 1) {
                    return Result.error(ErrorCode.INTERNAL_ERROR);
                }
                try { Thread.sleep(10 + (long)(Math.random() * 50)); } catch (Exception ignored) {}
            }
        }
        return Result.error(ErrorCode.INTERNAL_ERROR);
    }

    @Override
    public Result<User> updateUser(String name, String pwd, User info) {
        if (name == null || name.isBlank() || pwd == null || pwd.isBlank() || info == null
                || (info.getName() != null && !info.getName().equals(name))
                || (info.getDomain() != null && !info.getDomain().equals(this.domain))) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        for (int attempt = 0; attempt < MAX_DB_RETRIES; attempt++) {
            try (TxContext tx = hibernate.beginTx()) {
                User existingUser = tx.session().find(User.class, name, LockModeType.PESSIMISTIC_WRITE);

                if (existingUser == null || !existingUser.getPwd().equals(pwd)) {
                    tx.commit();
                    return Result.error(ErrorCode.FORBIDDEN);
                }

                boolean changed = false;
                if (info.getPwd() != null && !info.getPwd().isBlank()) {
                    existingUser.setPwd(info.getPwd());
                    changed = true;
                }
                if (info.getDisplayName() != null && !info.getDisplayName().isBlank()) {
                    existingUser.setDisplayName(info.getDisplayName());
                    changed = true;
                }

                if (changed) {
                    hibernate.updateTx(tx, existingUser);
                }
                tx.commit();
                return Result.ok(existingUser);
            } catch (Exception e) {
                if (attempt == MAX_DB_RETRIES - 1) {
                    Log.severe("Erro no updateUser: " + e.getMessage());
                    return Result.error(ErrorCode.INTERNAL_ERROR);
                }
                try { Thread.sleep(10 + (long)(Math.random() * 50)); } catch (Exception ignored) {}
            }
        }
        return Result.error(ErrorCode.INTERNAL_ERROR);
    }

    @Override
    public Result<User> deleteUser(String name, String pwd) {
        if (name == null || pwd == null) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }
        for (int attempt = 0; attempt < MAX_DB_RETRIES; attempt++) {
            try (TxContext tx = hibernate.beginTx()) {
                User user = tx.session().find(User.class, name, LockModeType.PESSIMISTIC_WRITE);

                if (user == null || !user.getPwd().equals(pwd)) {
                    tx.commit();
                    return Result.error(ErrorCode.FORBIDDEN);
                }

                hibernate.deleteTx(tx, user);
                tx.commit();
                return Result.ok(user);
            } catch (Exception e) {
                if (attempt == MAX_DB_RETRIES - 1) return Result.error(ErrorCode.INTERNAL_ERROR);
                try { Thread.sleep(10 + (long)(Math.random() * 50)); } catch (Exception ignored) {}
            }
        }
        return Result.error(ErrorCode.INTERNAL_ERROR);
    }

    @Override
    public Result<List<User>> searchUsers(String name, String pwd, String query) {
        if (name == null || pwd == null || query == null) return Result.error(ErrorCode.BAD_REQUEST);

        for (int attempt = 0; attempt < MAX_DB_RETRIES; attempt++) {
            try (TxContext tx = hibernate.beginTx()) {

                if (!pwd.equals("")) {
                    User user = hibernate.getTx(tx, User.class, name);
                    if (user == null || !user.getPwd().equals(pwd)) {
                        tx.commit();
                        return Result.error(ErrorCode.FORBIDDEN);
                    }
                }

                String jpql;
                org.hibernate.query.Query<User> hqlQuery;

                if (pwd.equals("")) {
                    jpql = "SELECT u FROM User u WHERE u.name = :q";
                    hqlQuery = tx.session().createQuery(jpql, User.class);
                    hqlQuery.setParameter("q", query);
                } else {
                    jpql = "SELECT u FROM User u WHERE lower(u.name) LIKE :q";
                    hqlQuery = tx.session().createQuery(jpql, User.class);
                    hqlQuery.setParameter("q", "%" + query.toLowerCase() + "%");
                }

                List<User> rawUsers = hqlQuery.list();
                List<User> safeUsers = new java.util.ArrayList<>();
                for (User u : rawUsers) {
                    safeUsers.add(new User(u.getName(), "", u.getDisplayName(), u.getDomain()));
                }

                tx.commit();
                return Result.ok(safeUsers);

            } catch (Exception e) {
                if (attempt == MAX_DB_RETRIES - 1) return Result.error(ErrorCode.INTERNAL_ERROR);
                try { Thread.sleep(10 + (long)(Math.random() * 50)); } catch (InterruptedException ignored) {}
            }
        }
        return Result.error(ErrorCode.INTERNAL_ERROR);
    }
}