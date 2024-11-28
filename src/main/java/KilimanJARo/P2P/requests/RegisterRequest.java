// RegisterRequest.java
package KilimanJARo.P2P.requests;

public class RegisterRequest {
    private String name;
    private String password;

    public RegisterRequest() {
    }

    public RegisterRequest(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}