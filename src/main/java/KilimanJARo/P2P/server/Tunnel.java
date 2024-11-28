package KilimanJARo.P2P.server;

import java.util.List;
import KilimanJARo.P2P.user.User;

public class Tunnel {
    private final int id;
    private final int localId;
    private final User begpoint;
    private final User endpoint;
    private final List<User> nodes;

    public Tunnel(int id, int localId, User begpoint, User endpoint, List<User> nodes) {
        this.id = id;
        this.localId = localId;
        this.begpoint = begpoint;
        this.endpoint = endpoint;
        this.nodes = nodes;
    }

    public int getId() {
        return id;
    }

    public int getLocalId() {
        return localId;
    }

    public User getBegpoint() {
        return begpoint;
    }фф

    public User getEndpoint() {
        return endpoint;
    }

    public List<User> getNodes() {
        return nodes;
    }

    public static Tunnel createTunnel(int id, int localId, User begpoint, User endpoint, List<User> nodes) {
        return new Tunnel(id, localId, begpoint, endpoint, nodes);
    }

    public boolean deleteTunnel() {
        // TODO: Implement this method
    }
}