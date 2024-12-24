package KilimanJARo.P2P.user;

import jakarta.persistence.*;

@Entity
@Table(name = "db_table_users")
public class User {

    @Id
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Transient
    private String zerotierAddress;

    @Transient
    private int currentPort;

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.zerotierAddress = "";
        this.currentPort = -1;
    }

    public User() {
        this.username = "";
        this.passwordHash = "";
        this.zerotierAddress = "";
        this.currentPort = -1;
    }

    // Геттеры
    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getZerotierAddress() {
        return zerotierAddress;
    }

    public int getCurrentPort() {
        return currentPort;
    }

    // Сеттеры
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setZerotierAddress(String zerotierAddress) {
        this.zerotierAddress = zerotierAddress;
    }

    public void setCurrentPort(int currentPort) {
        this.currentPort = currentPort;
    }

    // Дополнительный метод для проверки пароля
    public boolean isCorrectPassword(String password) {
        return this.passwordHash.equals(password);
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", zerotierAddress='" + zerotierAddress + '\'' +
                ", currentPort=" + currentPort +
                '}';
    }
}