// AuthResponse.java
package KilimanJARo.P2P.server.responses;

public record AuthResponse (boolean success, String message, String nextPassword) {}