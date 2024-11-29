
package KilimanJARo.P2P.server.responses;

public class RegisterResponse {
    private boolean success;
    private String message;
    private String password;

    public RegisterResponse() {
    }

    public RegisterResponse(boolean success, String message, String password) {
        this.success = success;
        this.message = message;
        this.password = password;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPassword() {
        return password;
    }
}