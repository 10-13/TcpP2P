// AuthResponse.java
package KilimanJARo.P2P.networking.responses;

public record AuthResponse (boolean success, String message, String nextPassword) {}