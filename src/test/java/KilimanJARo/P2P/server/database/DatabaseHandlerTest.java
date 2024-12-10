package KilimanJARo.P2P.server.database;

import KilimanJARo.P2P.user.User;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Arrays;
import java.util.Collection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DatabaseHandlerTest {

    @Mock
    private Session mockSession;

    @Mock
    private Transaction mockTransaction;

    @Mock
    private SessionFactory mockSessionFactory;

    @InjectMocks
    private DatabaseHandler databaseHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSaveUsers() {
        User user1 = new User("user1", "password1");
        User user2 = new User("user2", "password2");
        Collection<User> users = Arrays.asList(user1, user2);

        // Mock the behavior of session and transaction
        when(mockSession.beginTransaction()).thenReturn(mockTransaction);
        doNothing().when(mockSession).save(any(User.class));
        when(mockSession.getSessionFactory()).thenReturn(mockSessionFactory);

        // You might want to call the instance method of DatabaseHandler instead of the static one
        databaseHandler.save(users);  // Assume 'save' is refactored to an instance method

        verify(mockSession, times(2)).save(any(User.class));
        verify(mockTransaction).commit();
    }

    @Test
    void testUpdateUsers() {
        User user1 = new User("user1", "password1");
        User user2 = new User("user2", "password2");
        Collection<User> users = Arrays.asList(user1, user2);

        when(mockSession.beginTransaction()).thenReturn(mockTransaction);
        doNothing().when(mockSession).update(any(User.class));
        when(mockSession.getSessionFactory()).thenReturn(mockSessionFactory);

        databaseHandler.update(users);

        verify(mockSession, times(2)).update(any(User.class));
        verify(mockTransaction).commit();
    }

    @Test
    void testDeleteUsers() {
        User user1 = new User("user1", "password1");
        User user2 = new User("user2", "password2");
        Collection<User> users = Arrays.asList(user1, user2);

        when(mockSession.beginTransaction()).thenReturn(mockTransaction);
        doNothing().when(mockSession).delete(any(User.class));
        when(mockSession.getSessionFactory()).thenReturn(mockSessionFactory);

        databaseHandler.delete(users);

        verify(mockSession, times(2)).delete(any(User.class));
        verify(mockTransaction).commit();
    }

    @Test
    void testSaveUsers_WithException_ShouldRollback() {
        User user1 = new User("user1", "password1");
        User user2 = new User("user2", "password2");
        Collection<User> users = Arrays.asList(user1, user2);

        when(mockSession.beginTransaction()).thenReturn(mockTransaction);
        doThrow(new HibernateException("Mock Exception")).when(mockSession).save(any(User.class));
        when(mockSession.getSessionFactory()).thenReturn(mockSessionFactory);

        try {
            databaseHandler.save(users);
        } catch (RuntimeException e) {
            verify(mockTransaction).rollback();
        }
    }
}
