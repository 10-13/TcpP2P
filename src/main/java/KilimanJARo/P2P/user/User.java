package KilimanJARo.P2P.user;

public class User {
    private final String username;
    private String passwordHash;

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

    @Override
    public String toString() {
        return "User{" +
                ", username='" + username + '\'' +
                ", passwordHash='" + passwordHash + '\'' +
                '}';
    }
}