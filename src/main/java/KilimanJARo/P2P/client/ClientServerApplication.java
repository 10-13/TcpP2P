package KilimanJARo.P2P.client;

import org.springframework.boot.SpringApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ClientServerApplication {
    private final Map<String, String> tunnelMap = new HashMap<>(); // Maps local_id to tunnel_id
    private final RestTemplate restTemplate = new RestTemplate();

    public static void main(String[] args) {
        SpringApplication.run(ClientServerApplication.class, args);
    }

    @PostMapping("/makeTube")
    public RequestResponse makeTube(@RequestParam(required = false) String from,
                             @RequestParam(required = false) String to,
                             @RequestParam String tunnel_id) {
        if (from == null) {
            from = "local_port";
        }
        if (to == null) {
            to = "local_port";
        }
        String url = "http://server/api/makeTube?from=" + from + "&to=" + to + "&tunnel_id=" + tunnel_id;
        RequestResponse response = restTemplate.postForObject(url, null, RequestResponse.class);
        return response != null ? response : new RequestResponse(false,"failure", tunnel_id);
    }

    @PostMapping("/closeTube")
    public void closeTube(@RequestParam String tunnel_id) {
        String url = "http://server/api/closeTube?tunnel_id=" + tunnel_id;
        restTemplate.postForObject(url, null, Void.class);
    }

    @PostMapping("/requestConnection")
    public RequestResponse requestConnection(@RequestParam String request_user,
                                                @RequestParam String tunnel_id) {
        String url = "http://server/api/requestConnection?request_user=" + request_user + "&tunnel_id=" + tunnel_id;
        RequestResponse response = restTemplate.postForObject(url, null, RequestResponse.class);
        return response != null ? response : new RequestResponse(false, "failure", tunnel_id);
    }

    @PostMapping("/establishConnection")
    public void establishConnection(@RequestParam String tunnel_id,
                                    @RequestParam String local_id,
                                    @RequestParam String endpoint_user) {
        String url = "http://server/api/establishConnection?tunnel_id=" + tunnel_id + "&local_id=" + local_id + "&endpoint_user=" + endpoint_user;
        restTemplate.postForObject(url, null, Void.class);
    }

    @PostMapping("/requestTube")
    public RequestResponse requestTube(@RequestParam String endpoint_name,
                                @RequestParam String local_id) {
        String url = "http://server/api/requestTube?endpoint_name=" + endpoint_name + "&local_id=" + local_id;
        RequestResponse response = restTemplate.postForObject(url, null, RequestResponse.class);
        if (response != null && response.isAllowed()) {
            tunnelMap.put(local_id, response.getTunnel_id());
        }
        return response != null ? response : new RequestResponse(false, "failure", null);
    }

    @PostMapping("/requestCloseTube")
    public void requestCloseTube(@RequestParam String local_id) {
        String tunnel_id = tunnelMap.get(local_id);
        if (tunnel_id != null) {
            String url = "http://server/api/requestCloseTube?local_id=" + local_id + "&tunnel_id=" + tunnel_id;
            restTemplate.postForObject(url, null, Void.class);
            tunnelMap.remove(local_id);
        }
    }
}