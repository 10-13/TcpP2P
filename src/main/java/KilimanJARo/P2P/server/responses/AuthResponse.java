// AuthResponse.java
package KilimanJARo.P2P.server.responses;

public class AuthResponse {
    private boolean success;
    private String message;
    private String nextPassword;

    public AuthResponse(boolean success, String message, String nextPassword) {
        this.success = success;
        this.message = message;
        this.nextPassword = nextPassword;
    }

    public String getNextPassword() {
        return nextPassword;
    }

    public void setNextPassword(String nextPassword) {
        this.nextPassword = nextPassword;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}