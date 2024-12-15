package KilimanJARo.P2P.user;

import jakarta.persistence.*;

@Entity
@Table(name = "db_table_users")
public class User {
    @Column(name = "username", nullable = false, unique = true)
    private final String username;

    @Column(name = "passwordHash", nullable = false)
    private String passwordHash;
    private int currentPort;

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getUsername() {
        return username;
    }

    public boolean isCorrectPassword(String hash) {
        return passwordHash.equals(hash);
    }

    public void setPass(String newPassword) {
        passwordHash = newPassword;
    }

    public int getCurrentPort() {
        return currentPort;
    }

    public void setCurrentPort(int port) {
        currentPort = port;
    }

    @Override
    public String toString() {
        return "User{" +
                ", username='" + username + '\'' +
                ", passwordHash='" + passwordHash + '\'' +
                '}';
    }
}