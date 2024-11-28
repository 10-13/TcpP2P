package KilimanJARo.P2P.user;

public class User {
    private int id;
    private String username;
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
}