package KilimanJARo.P2P.client.tunneling;

public interface TunnelInterface {
    boolean initialize();
    void close();
    int getPort();
    boolean confirmOwner(String username);
    String getLocalId();
}