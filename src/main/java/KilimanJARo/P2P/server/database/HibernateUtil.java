package KilimanJARo.P2P.server.database;

import KilimanJARo.P2P.annotations.VisibleForTesting;
import KilimanJARo.P2P.user.User;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
    private static SessionFactory sessionFactory_;

    /**
     * Initialize PostgreSQL database using .properties file
     */
    public static void initializeSessionFactory() {
        try {
            sessionFactory_ = new Configuration()
                    .addAnnotatedClass(User.class)
                    .buildSessionFactory();
        } catch (HibernateException e) {
            throw new ExceptionInInitializerError("Initial SessionFactory creation failed: " +  e);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory_;
    }

    /**
     * For test usage only, otherwise use method initializeSessionFactory()
     * and getSessionFactory()
     */
    @VisibleForTesting
    public static void setSessionFactory(SessionFactory sessionFactory) {
        sessionFactory_ = sessionFactory;
    }
}

