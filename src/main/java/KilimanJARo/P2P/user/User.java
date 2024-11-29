package KilimanJARo.P2P.user;

public class User {
    private final int id;
    private final String username;
    private String passwordHash;

    public User(int id, String username, String passwordHash) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public int getId() {
        return id;
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
                "id=" + id +
                ", username='" + username + '\'' +
                ", passwordHash='" + passwordHash + '\'' +
                '}';
    }
}