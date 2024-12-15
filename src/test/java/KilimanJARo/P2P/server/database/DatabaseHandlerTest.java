package KilimanJARo.P2P.server.database;

import KilimanJARo.P2P.user.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseHandlerTest {

    private SessionFactory sessionFactory;
    private Session session;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        sessionFactory = mock(SessionFactory.class);
        session = mock(Session.class);
        transaction = mock(Transaction.class);

        when(sessionFactory.openSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);

        HibernateUtil.setSessionFactory(sessionFactory);
    }

    @Test
    void testSaveUser() {
        User user = new User("testUser", "passwordHash");

        DatabaseHandler.save(user);

        verify(session).save(user);
        verify(transaction).commit();

        assertEquals("testUser", user.getUsername());
        assertTrue(user.isCorrectPassword("passwordHash"));
    }

    @Test
    void testUpdateUser() {
        User user = new User("testUser", "passwordHash");

        DatabaseHandler.update(user);

        verify(session).update(user);
        verify(transaction).commit();

        assertEquals("testUser", user.getUsername());
        assertTrue(user.isCorrectPassword("passwordHash"));
    }

    @Test
    void testDeleteUser() {
        User user = new User("testUser", "passwordHash");

        DatabaseHandler.delete(user);

        verify(session).delete(user);
        verify(transaction).commit();

        assertEquals("testUser", user.getUsername());
        assertTrue(user.isCorrectPassword("passwordHash"));
    }

    @Test
    void testGetUser() {
        User user = new User("testUser", "passwordHash");
        when(session.get(User.class, "testUser")).thenReturn(user);

        User result = DatabaseHandler.get("testUser");

        assertEquals(user, result);
        assertEquals("testUser", result.getUsername());
        assertTrue(result.isCorrectPassword("passwordHash"));
    }

    @Test
    void testGetAllUsers() {
        User user1 = new User("user1", "passwordHash1");
        User user2 = new User("user2", "passwordHash2");
        List<User> users = Arrays.asList(user1, user2);

        Query<User> query = mock(Query.class);
        when(session.createQuery("from User", User.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(users);

        List<User> result = DatabaseHandler.getAll();

        assertEquals(users, result);
        assertEquals("user1", result.get(0).getUsername());
        assertTrue(result.get(0).isCorrectPassword("passwordHash1"));
        assertEquals("user2", result.get(1).getUsername());
        assertTrue(result.get(1).isCorrectPassword("passwordHash2"));
    }
}