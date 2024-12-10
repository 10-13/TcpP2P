package KilimanJARo.P2P.server.database;

import KilimanJARo.P2P.user.User;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DatabaseHandler {
    static final int BATCH_SIZE = 50;

    /**
     * A universal method that processes a collection of User
     * entities using a specified operation.
     * ! WORKS WITH NOT OPTIMAL SPEED, because of opening session every paste,
     * but hope we'll prefer single methods ;)
     * @param users a collection of User entities to be processed
     * @param action a Consumer that represents the operation to perform on each User
     */
    private static void process(Collection<User> users, Consumer<User> action) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            int count = 0;
            for (User user : users) {
                action.accept(user);
                if (++count % BATCH_SIZE == 0) {
                    session.flush();
                    session.clear();
                }
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw new RuntimeException("Failed to process users", e);
        }
    }

    /**
     * Save a user to the database.
     * @param user the user to be saved
     */
    public static void save(User user) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(user);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }

    /**
     * Save a collection of User entities to the database.
     * @param users a collection of User entities to be saved
     */
    public static void save(Collection<User> users) {
        process(users, user -> {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                session.save(user);
            }
        });
    }

    /**
     * Update a user in the database.
     * @param user the user to be updated
     */
    public static void update(User user) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.update(user);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }

    /**
     * Update a collection of User entities in the database.
     * @param users a collection of User entities to be updated
     */
    public static void update(Collection<User> users) {
        process(users, user -> {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                session.update(user);
            }
        });
    }

    /**
     * Delete a user from the database.
     * @param user the user to be deleted
     */
    public static void delete(User user) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.delete(user);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }

    /**
     * Delete a collection of User entities from the database.
     * @param users a collection of User entities to be deleted
     */
    public static void delete(Collection<User> users) {
        process(users, user -> {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                session.delete(user);
            }
        });
    }

    /**
     * Retrieve a User by its primary key (username).
     * @param username the primary key
     * @return the User entity, or null if not found
     */
    public static User get(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(User.class, username);
        } catch (HibernateException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Retrieve all User entities from the database.
     * @return a list of all users
     */
    public static List<User> getAll() {
        List<User> users;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // User is not a table name in HQL query
            String hql = "from User";
            users = session.createQuery(hql, User.class).getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
        return users;
    }
}
