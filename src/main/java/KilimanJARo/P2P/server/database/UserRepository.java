package KilimanJARo.P2P.server.database;

import KilimanJARo.P2P.user.User;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
	User save(User user);

	default User get(String username) {
		return findById(username).orElse(null);
	}

	default boolean exists(String username) {
		return findById(username).isPresent();
	}

	default void delete(String username) {
		deleteById(username);
	}

	default void update(User user) {
		save(user);
	}

	default List<User> getAll() {
		return findAll();
	}

}