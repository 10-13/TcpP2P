package KilimanJARo.P2P.client.tunneling;

public class Tunnel {
    /**
     * Use some timeout to return null if it failed to connect in some short period of time
     * @param from Wait for connection from this address
     * @param to Request connection to this address
     */
    public static Tunnel Create(String from, String to) {
        throw new RuntimeException("Not implemented.");
    }

    /**
     * Close all connections
     */
    public void Close() {
        throw new RuntimeException("Not implemented.");
    }

    /**
     * @return If from|to where null, returns port where they're redirecting
     */
    public int Port() {
        throw new RuntimeException("Not implemented.");
    }
}
