package KilimanJARo.P2P.client;

import KilimanJARo.P2P.client.response.CreateTunnelResponse;
import KilimanJARo.P2P.client.response.EstablishConnectionResponse;
import KilimanJARo.P2P.client.tunneling.Tunnel;
import KilimanJARo.P2P.server.requests.AuthRequest;
import KilimanJARo.P2P.server.requests.LogoutRequest;
import KilimanJARo.P2P.server.requests.RegisterRequest;
import KilimanJARo.P2P.server.responses.AuthResponse;
import KilimanJARo.P2P.server.responses.LogoutResponse;
import KilimanJARo.P2P.server.responses.RegisterResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@RestController
@RequestMapping("/api")
public class ClientServerController {
    private final RestTemplate restTemplate;
    private final Properties central_properties;
    private final Properties properties;
    private final PropertiesFactoryBean mainServerProperties;

    /**
     * This field is for test purposes only and will be removed later and moved to front.
     */
    @Deprecated
    private String password;

    private final Map<String, String> localToPublicIDTubeMap = new HashMap<>();
    private final Map<String, Tunnel> publicIDToLocalTunnels = new HashMap<>();

    @Autowired
    public ClientServerController(@Qualifier("ClientServerRestTemplate") RestTemplate restTemplate, PropertiesFactoryBean centralServerProperties, PropertiesFactoryBean serverProperties, PropertiesFactoryBean mainServerProperties) throws IOException {
        this.restTemplate = restTemplate;
        this.central_properties = centralServerProperties.getObject();
        if (central_properties == null) {
            throw new IOException("Failed to load central properties.");
        }

        this.properties = serverProperties.getObject();
        if (properties == null) {
            throw new IOException("Failed to load properties.");
        }
        this.mainServerProperties = mainServerProperties;
    }

    @GetMapping("/registerWithMainServer")
    public ResponseEntity<RegisterResponse> registerWithMainServer() {
        RegisterRequest request = new RegisterRequest("ClientServer");
        HttpHeaders headers = new HttpHeaders();
        // headers.setBasicAuth("username", "password");
        // headers.set("Content-Type", "application/json");
        RequestEntity<RegisterRequest> requestEntity =
            RequestEntity.post(URI.create(central_properties.getProperty("server.url.apu.register")))
            .headers(headers)
            .body(request);

        ResponseEntity<RegisterResponse> response = restTemplate.exchange(requestEntity, RegisterResponse.class);
        password = response.getBody().getPassword();

        if (response.getBody() != null && response.getBody().isSuccess()) {
            return ResponseEntity.ok(new RegisterResponse(true, "Server registered successfully", response.getBody().getPassword()));
        } else {
            return ResponseEntity.status(500).body(new RegisterResponse(false, "Server registration failed", null));
        }
    }

    @GetMapping("/authWithMainServer")
    public ResponseEntity<AuthResponse> authWithMainServer(@RequestParam String password) {
        AuthRequest request = new AuthRequest("ClientServer", password);
        HttpHeaders headers = new HttpHeaders();
        // headers.setBasicAuth("username", "password");
        // headers.set("Content-Type", "application/json");

        RequestEntity<AuthRequest> requestEntity = RequestEntity
                .post(URI.create(central_properties.getProperty("server.url.api.login")))
                .headers(headers)
                .body(request);

        ResponseEntity<AuthResponse> response = restTemplate.exchange(requestEntity, AuthResponse.class);
        password = response.getBody().getNextPassword();

        if (response.getBody() != null && response.getBody().isSuccess()) {
            return ResponseEntity.ok(new AuthResponse(true, "Server authenticated successfully", response.getBody().getNextPassword()));
        } else {
            return ResponseEntity.status(500).body(new AuthResponse(false, "Server authentication failed", null));
        }
    }

    @GetMapping("/logoutFromMainServer")
    public ResponseEntity<LogoutResponse> logoutFromMainServer() {
        LogoutRequest request = new LogoutRequest("ClientServer");
        HttpHeaders headers = new HttpHeaders();
        // headers.setBasicAuth("username", "password");
        // headers.set("Content-Type", "application/json");

        RequestEntity<LogoutRequest> requestEntity = RequestEntity
                .post(URI.create(central_properties.getProperty("server.url.api.logout")))
                .headers(headers)
                .body(request);

        ResponseEntity<LogoutResponse> response = restTemplate.exchange(requestEntity, LogoutResponse.class);

        if (response.getBody() != null && response.getBody().isSuccess()) {
            return ResponseEntity.ok(new LogoutResponse(true, "Logged out successfully"));
        } else {
            return ResponseEntity.status(500).body(new LogoutResponse(false, "Logout failed"));
        }
    }

    @PostMapping("/makeTube")
    public ResponseEntity<CreateTunnelResponse> makeTube(@RequestParam(required = false) String from,
                                                         @RequestParam(required = false) String to,
                                                         @RequestParam String tunnel_id) {
        if (from == null) {
            from = properties.getProperty("front.port");
            if (from == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CreateTunnelResponse(false));
            }
        }
        if (to == null) {
            to = properties.getProperty("front.port");
            if (to == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CreateTunnelResponse(false));
            }
        }
        var tunnel = Tunnel.Create(from, to);
        if (tunnel == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CreateTunnelResponse(false));
        publicIDToLocalTunnels.put(tunnel_id, tunnel);
        return ResponseEntity.ok(new CreateTunnelResponse(true));
    }

    @PostMapping("/closeTube")
    public void closeTube(@RequestParam String tunnel_id) {
        publicIDToLocalTunnels.get(tunnel_id).Close();
    }


    @PostMapping("/requestConnection")
    public ResponseEntity<EstablishConnectionResponse> requestConnection(@RequestParam String request_user,
                                                         @RequestParam String tunnel_id) {
        String url = "http://localhost:" + properties.getProperty("front.port") + "/requestConnection?" +
                "request_user=" + request_user +
                "&tunnel_id=" + tunnel_id;
        ResponseEntity<EstablishConnectionResponse> response = restTemplate.postForEntity(url, null, EstablishConnectionResponse.class);
        if (response.getBody().isAllowed())
            return ResponseEntity.ok(response.getBody());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new EstablishConnectionResponse(false, "failure", tunnel_id));
    }

    @PostMapping("/establishConnection")
    public void establishConnection(@RequestParam String tunnel_id,
                                    @RequestParam String local_id,
                                    @RequestParam String endpoint_user) {
        String url = "http://localhost:" + properties.getProperty("front.port") + "/requestConnection?" +
                "request_user=" + endpoint_user +
                "&tunnel_id=" + tunnel_id +
                "&local_id=" + local_id +
                "&port=" + publicIDToLocalTunnels.get(tunnel_id).Port();
        restTemplate.postForEntity(url, null, Void.class);
    }


    @PostMapping("/requestTube")
    public ResponseEntity<EstablishConnectionResponse> requestTube(@RequestParam String endpoint_name,
                                @RequestParam String local_id) {
        String url = "http://server/api/requestTube?endpoint_name=" + endpoint_name + "&local_id=" + local_id;
        ResponseEntity<EstablishConnectionResponse> response = restTemplate.postForEntity(url, null, EstablishConnectionResponse.class);
        if (response.getBody().isAllowed()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new EstablishConnectionResponse(false, "failure", local_id));
        }
        if (response.getBody().isAllowed()) {
            localToPublicIDTubeMap.put(local_id, response.getBody().tunnelId());
        }
        return ResponseEntity.ok(new EstablishConnectionResponse(response.getBody().isAllowed(), response.getBody().reason(), local_id));
    }

    @PostMapping("/requestCloseTube")
    public void requestCloseTube(@RequestParam String local_id) {
        String tunnel_id = localToPublicIDTubeMap.get(local_id);
        if (tunnel_id != null) {
            String url = "http://server/api/requestCloseTube?local_id=" + local_id + "&tunnel_id=" + tunnel_id;
            restTemplate.postForEntity(url, null, Void.class);
            localToPublicIDTubeMap.remove(local_id);
        }
    }
}
