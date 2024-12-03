package KilimanJARo.P2P.client;

import KilimanJARo.P2P.client.request.EstablishConnectionRequest;
import KilimanJARo.P2P.client.response.CreateTunnelResponse;
import KilimanJARo.P2P.client.response.EstablishConnectionResponse;
import KilimanJARo.P2P.client.tunneling.ReadableTunnel;
import KilimanJARo.P2P.client.tunneling.Tunnel;
import KilimanJARo.P2P.client.tunneling.TunnelInterface;
import KilimanJARo.P2P.client.tunneling.UnreadableTunnel;
import KilimanJARo.P2P.server.requests.AuthRequest;
import KilimanJARo.P2P.server.requests.LogoutRequest;
import KilimanJARo.P2P.server.requests.RegisterRequest;
import KilimanJARo.P2P.server.responses.AuthResponse;
import KilimanJARo.P2P.server.responses.LogoutResponse;
import KilimanJARo.P2P.server.responses.RegisterResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@RestController
@RequestMapping("/api")
public class ClientServerController {
    private final RestTemplate restTemplate;
    private final SmartProperties central_properties;
    private final SmartProperties properties;
    private String username = "";

    /**
     * This field is for test purposes only and will be removed later and moved to front.
     */
    @Deprecated
    private String password;

    private final Map<String, String> localToPublicIDTubeMap = new HashMap<>();
    private final Map<String, TunnelInterface> publicIDToLocalTunnels = new HashMap<>();

    @Autowired
    public ClientServerController(@Qualifier("ClientServerRestTemplate") RestTemplate restTemplate, @Qualifier("centralServerProperties") PropertiesFactoryBean centralServerProperties, @Qualifier("serverProperties") PropertiesFactoryBean serverProperties) throws IOException {
        this.restTemplate = restTemplate;
        this.central_properties = new SmartProperties(centralServerProperties.getObject());
        this.properties = new SmartProperties(serverProperties.getObject());
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your name: ");
        username = scanner.nextLine();
        registerWithMainServer();
    }

    @GetMapping("/registerWithMainServer")
    public ResponseEntity<RegisterResponse> registerWithMainServer() {
        RegisterRequest request = new RegisterRequest(username);
        HttpHeaders headers = new HttpHeaders();
        // headers.setBasicAuth("username", "password");
        // headers.set("Content-Type", "application/json");
        RequestEntity<RegisterRequest> requestEntity =
            RequestEntity.post(URI.create(central_properties.getProperty("server.api.register.url")))
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
        AuthRequest request = new AuthRequest(username, password);
        HttpHeaders headers = new HttpHeaders();
        // headers.setBasicAuth("username", "password");
        // headers.set("Content-Type", "application/json");

        RequestEntity<AuthRequest> requestEntity = RequestEntity
                .post(URI.create(central_properties.getProperty("server.api.login.url")))
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

    @Deprecated
    @GetMapping("/authWithMainServerAuto")
    public ResponseEntity<AuthResponse> authWithMainServerAuto() {
        AuthRequest request = new AuthRequest(username, password);
        HttpHeaders headers = new HttpHeaders();
        // headers.setBasicAuth("username", "password");
        // headers.set("Content-Type", "application/json");

        RequestEntity<AuthRequest> requestEntity = RequestEntity
                .post(URI.create(central_properties.getProperty("server.api.login.url")))
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
        LogoutRequest request = new LogoutRequest(username);
        HttpHeaders headers = new HttpHeaders();
        // headers.setBasicAuth("username", "password");
        // headers.set("Content-Type", "application/json");

        RequestEntity<LogoutRequest> requestEntity = RequestEntity
                .post(URI.create(central_properties.getProperty("server.api.logout.url")))
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
        boolean readable = false;
        if (from == null && to == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CreateTunnelResponse(false));
        }
        if (from == null) {
            from = properties.getProperty("front.port");
            if (from == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CreateTunnelResponse(false));
            }
            readable = true;
        }
        if (to == null) {
            to = properties.getProperty("front.port");
            if (to == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CreateTunnelResponse(false));
            }
            readable = true;
        }
        TunnelInterface tunnel;
        String local_id = generateLocalId();
        if (readable) {
            tunnel = new ReadableTunnel(from, to, username, local_id);
        } else {
            tunnel = new UnreadableTunnel(from, to, username, local_id);
        }
        if (!tunnel.initialize()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CreateTunnelResponse(false));
        }

        localToPublicIDTubeMap.put(local_id, tunnel_id);
        publicIDToLocalTunnels.put(tunnel_id, tunnel);
        return ResponseEntity.ok(new CreateTunnelResponse(true));
    }

    @PostMapping("/closeTube")
    public void closeTube(@RequestParam String tunnel_id) {
        localToPublicIDTubeMap.remove(publicIDToLocalTunnels.get(tunnel_id).getLocalId());
        publicIDToLocalTunnels.get(tunnel_id).close();
        publicIDToLocalTunnels.remove(tunnel_id);
    }


    @PostMapping("/requestConnection")
    public ResponseEntity<EstablishConnectionResponse> requestConnection(@RequestParam String request_user) {
        EstablishConnectionRequest request = new EstablishConnectionRequest(username, request_user);
        HttpHeaders headers = new HttpHeaders();
        // headers.setBasicAuth("username", "password");
        // headers.set("Content-Type", "application/json");
        RequestEntity<EstablishConnectionRequest> requestEntity =
                RequestEntity.post(URI.create(central_properties.getProperty("server.api.connectionRequested.url")))
                        .headers(headers)
                        .body(request);
        ResponseEntity<EstablishConnectionResponse> response = restTemplate.exchange(requestEntity, EstablishConnectionResponse.class);
        if (response.getBody() != null && response.getBody().isAllowed()) {
            String local_id = generateLocalId();
            localToPublicIDTubeMap.put(local_id, response.getBody().tunnelId());
            return ResponseEntity.ok(response.getBody());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new EstablishConnectionResponse(false, "failure", null));
    }

    /*@PostMapping("/establishConnection")
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
    }*/

    @PostMapping("/requestCloseTube")
    public void requestCloseTube(@RequestParam String local_id) {
        String tunnel_id = localToPublicIDTubeMap.get(local_id);
        if (tunnel_id != null) {
            String url = properties.getProperty("server.api.requestCloseTube.url") + "&tunnel_id=" + tunnel_id;
            // String url = "http://server/api/requestCloseTube?local_id=" + local_id + "&tunnel_id=" + tunnel_id;
            restTemplate.postForEntity(url, null, Void.class);
            closeTube(tunnel_id);
        }
    }

    private String generateLocalId() {
        String local_id;
        do {
            local_id = RandomStringGenerator.generateRandomString(10);
        } while (localToPublicIDTubeMap.containsKey(local_id));
        return local_id;
    }

    private class RandomStringGenerator {
        private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        private static final SecureRandom RANDOM = new SecureRandom();

        public static String generateRandomString(int length) {
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                int index = RANDOM.nextInt(CHARACTERS.length());
                sb.append(CHARACTERS.charAt(index));
            }
            return sb.toString();
        }
    }
}
