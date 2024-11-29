// AuthResponse.java
package KilimanJARo.P2P.server.responses;

public class AuthResponse {
    private boolean success;
    private String nextPassword;

    public AuthResponse(boolean success, String nextPassword) {
        this.success = success;
        this.nextPassword = nextPassword;
    }

    public String getNextPassword() {
        return nextPassword;
    }

    public void setNextPassword(String nextPassword) {
        this.nextPassword = nextPassword;
    }
}