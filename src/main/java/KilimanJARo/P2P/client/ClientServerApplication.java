package KilimanJARo.P2P.client;

import KilimanJARo.P2P.client.response.CreateTunnelResponse;
import KilimanJARo.P2P.client.response.EstablishConnectionResponse;
import KilimanJARo.P2P.client.tunneling.Tunnel;
import org.springframework.boot.SpringApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ClientServerApplication {
    private final Map<String, String> localToPublicIDTubeMap = new HashMap<>();
    private final Map<String, Tunnel> publicIDToLocalTunnels = new HashMap<>();
    private final RestTemplate restTemplate = new RestTemplate();

    public final int FrontPort = 8080;

    public static void main(String[] args) {
        SpringApplication.run(ClientServerApplication.class, args);
    }

    @PostMapping("/makeTube")
    public CreateTunnelResponse makeTube(@RequestParam(required = false) String from,
                             @RequestParam(required = false) String to,
                             @RequestParam String tunnel_id) {
        if (from == null)
            from = "local_port";
        if (to == null)
            to = "local_port";

        var tunnel = Tunnel.Create(from, to);
        if (tunnel == null)
            return new CreateTunnelResponse(false);

        publicIDToLocalTunnels.put(tunnel_id, tunnel);
        return new CreateTunnelResponse(true);
    }

    @PostMapping("/closeTube")
    public void closeTube(@RequestParam String tunnel_id) {
        publicIDToLocalTunnels.get(tunnel_id).Close();
    }

    @PostMapping("/requestConnection")
    public EstablishConnectionResponse requestConnection(@RequestParam String request_user,
                                                         @RequestParam String tunnel_id) {
        String url = "http://localhost:" + FrontPort + "/requestConnection?" +
                "request_user=" + request_user +
                "&tunnel_id=" + tunnel_id;

        EstablishConnectionResponse response = restTemplate.postForObject(url, null, EstablishConnectionResponse.class);
        if (response != null)
            return response;

        return new EstablishConnectionResponse(false, "failure", tunnel_id);
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

        restTemplate.postForObject(url, null, Void.class);
    }

    @PostMapping("/requestTube")
    public EstablishConnectionResponse requestTube(@RequestParam String endpoint_name,
                                @RequestParam String local_id) {
        String url = "http://server/api/requestTube?endpoint_name=" + endpoint_name + "&local_id=" + local_id;
        EstablishConnectionResponse response = restTemplate.postForObject(url, null, EstablishConnectionResponse.class);

        if (response == null)
            return new EstablishConnectionResponse(false, "failure", local_id);

        if (response.isAllowed()) {
            localToPublicIDTubeMap.put(local_id, response.tunnelId());
        }

        return new EstablishConnectionResponse(response.isAllowed(), response.reason(), local_id);
    }

    @PostMapping("/requestCloseTube")
    public void requestCloseTube(@RequestParam String local_id) {
        String tunnel_id = localToPublicIDTubeMap.get(local_id);
        if (tunnel_id != null) {
            String url = "http://server/api/requestCloseTube?local_id=" + local_id + "&tunnel_id=" + tunnel_id;
            restTemplate.postForObject(url, null, Void.class);
            localToPublicIDTubeMap.remove(local_id);
        }
    }
}
