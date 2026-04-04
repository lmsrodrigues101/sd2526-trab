package sd2526.trab.server.persistence;

import java.io.File;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

public class Hibernate {
    private static final String HIBERNATE_CFG_FILE = "hibernate.cfg.xml";
    private SessionFactory sessionFactory;
    private static Hibernate instance;

    private Hibernate() {
        try {
            sessionFactory = new Configuration()
                    .configure(new File(HIBERNATE_CFG_FILE))
                    .buildSessionFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    synchronized public static Hibernate getInstance() {
        if (instance == null)
            instance = new Hibernate();
        return instance;
    }

    // --- MÉTODOS COM TX CONTEXT EXPLÍCITO ---

    public TxContext beginTx() {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        return new TxContext(session, tx);
    }

    public void persistTx(TxContext txContext, Object... objects) {
        for (var o : objects) {
            txContext.session().persist(o);
        }
    }

    public void updateTx(TxContext txContext, Object... objects) {
        for (var o : objects) {
            txContext.session().merge(o);
        }
    }

    public void deleteTx(TxContext txContext, Object... objects) {
        for (var o : objects) {
            txContext.session().remove(o);
        }
    }

    public <T> List<T> jpqlTx(TxContext txContext, String jpqlStatement, Class<T> clazz) {
        var query = txContext.session().createQuery(jpqlStatement, clazz);
        return query.list();
    }

    public <T> T getTx(TxContext txContext, Class<T> clazz, Object identifier) {
        return txContext.session().find(clazz, identifier);
    }

    public void commitTx(TxContext txContext) {
        txContext.commit();
    }

    public void rollbackTx(TxContext txContext) {
        txContext.rollback();
    }

    // --- MÉTODOS AUTO-COMMIT (MANTIDOS PARA COMPATIBILIDADE) ---

    public void persist(Object... objects) {
        Transaction tx = null;
        try (var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            for (var o : objects) session.persist(o);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public <T> T get(Class<T> clazz, Object identifier) {
        Transaction tx = null;
        T element = null;
        try (var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            element = session.find(clazz, identifier);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
        return element;
    }

    public void update(Object... objects) {
        Transaction tx = null;
        try (var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            for (var o : objects) session.merge(o);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void delete(Object... objects) {
        Transaction tx = null;
        try (var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            for (var o : objects) session.remove(o);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public <T> List<T> jpql(String jpqlStatement, Class<T> clazz) {
        try (var session = sessionFactory.openSession()) {
            var query = session.createQuery(jpqlStatement, clazz);
            return query.list();
        }
    }
}