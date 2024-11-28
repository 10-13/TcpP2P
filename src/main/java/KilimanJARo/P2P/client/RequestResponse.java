package KilimanJARo.P2P.client;

public class RequestResponse {
    private boolean isAllowed;
    private String reason;
    private String tunnel_id;

    public RequestResponse(boolean isAllowed, String reason, String tunnel_id) {
        this.isAllowed = isAllowed;
        this.reason = reason;
        this.tunnel_id = tunnel_id;
    }

    public boolean isAllowed() {
        return isAllowed;
    }

    public void setAllowed(boolean allowed) {
        isAllowed = allowed;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getTunnel_id() {
        return tunnel_id;
    }

    public void setTunnel_id(String tunnel_id) {
        this.tunnel_id = tunnel_id;
    }
}