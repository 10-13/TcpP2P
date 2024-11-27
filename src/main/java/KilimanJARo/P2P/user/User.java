package KilimanJARo.P2P.user;

public class User {
    private int id;
    private String username;
    private Coordinates coordinates;

    public User(int id, String username, Coordinates coordinates) {
        this.id = id;
        this.username = username;
        this.coordinates = coordinates;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }
}