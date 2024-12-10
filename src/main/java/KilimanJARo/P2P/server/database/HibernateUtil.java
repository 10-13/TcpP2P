package KilimanJARo.P2P.server.database;

import KilimanJARo.P2P.user.User;
import org.hibernate.HibernateError;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
    private static final SessionFactory sessionFactory;

    /**
     * Initialize PostgreSQL database using .properties file
     */
    static {
        try {
            sessionFactory = new Configuration()
                    .addAnnotatedClass(User.class)
                    .buildSessionFactory();
        } catch (HibernateException e) {
            throw new ExceptionInInitializerError("Initial SessionFactory creation failed: " +  e);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}

