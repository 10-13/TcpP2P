package KilimanJARo.P2P.client;

import KilimanJARo.P2P.client.response.CreateTunnelResponse;
import KilimanJARo.P2P.client.response.EstablishConnectionResponse;
import KilimanJARo.P2P.client.tunneling.Tunnel;
import KilimanJARo.P2P.server.responses.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
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
    private final Map<String, String> localToPublicIDTubeMap = new HashMap<>();
    private final Map<String, Tunnel> publicIDToLocalTunnels = new HashMap<>();

    @Autowired
    public ClientServerController(@Qualifier("ClientServerRestTemplate") RestTemplate restTemplate, PropertiesFactoryBean centralServerProperties, PropertiesFactoryBean serverProperties) throws IOException {
        this.restTemplate = restTemplate;
        this.central_properties = centralServerProperties.getObject();
        if (central_properties == null) {
            throw new IOException("Failed to load central properties.");
        }

        this.properties = serverProperties.getObject();
        if (properties == null) {
            throw new IOException("Failed to load properties.");
        }
    }

    @PostMapping("/makeTube")
    public ResponseEntity<CreateTunnelResponse> makeTube(@RequestParam(required = false) String from,
                                                         @RequestParam(required = false) String to,
                                                         @RequestParam String tunnel_id) {
        // TODO: Now for null 'from' needs to send request to front for creating local port.
        //       If front returns incorrect port|failed to establish connection return errors.
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
        String url = "http://localhost:" + FrontPort + "/requestConnection?" +
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
        String url = "http://localhost:" + FrontPort + "/requestConnection?" +
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
