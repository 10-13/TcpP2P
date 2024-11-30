package KilimanJARo.P2P.client.tunneling;

public class Tunnel {
    /**
     * Use some timeout to return null if it failed to connect in some short period of time
     * @param from Contains IP and PORT where u need to connect via TCP
     * @param to Find free port and run async thread to wait for connection on them.
     *           Check for connected user has IP same as 'to'.
     *           After connected, need to redirect data on the same thread until Close is called or connection closed.
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
     * @return Returns port for output connection
     */
    public int Port() {
        throw new RuntimeException("Not implemented.");
    }
}
