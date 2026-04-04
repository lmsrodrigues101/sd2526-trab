package sd2526.trab.server.persistence;

import org.hibernate.Session;
import org.hibernate.Transaction;

public class TxContext implements AutoCloseable {
    private final Session session;
    private final Transaction tx;

    public TxContext(Session session, Transaction tx) {
        this.session = session;
        this.tx = tx;
    }

    public Session session() {
        return session;
    }

    public void commit() {
        if (tx.isActive()) {
            tx.commit();
        }
    }

    public void rollback() {
        if (tx.isActive()) {
            tx.rollback();
        }
    }

    @Override
    public void close() {
        // Se a transação ainda estiver ativa quando fecha (ex: ocorreu um erro e não houve commit), faz rollback
        if (tx != null && tx.isActive()) {
            tx.rollback();
        }
        if (session != null && session.isOpen()) {
            session.close();
        }
    }
}