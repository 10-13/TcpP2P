// AuthRequest.java
package KilimanJARo.P2P.networking.requests;

public record AuthRequest (String name, String password, int port) {}