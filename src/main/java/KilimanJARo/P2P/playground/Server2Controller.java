/*
package KilimanJARo.P2P.playground;

import KilimanJARo.P2P.networking.requests.AuthRequest;
import KilimanJARo.P2P.networking.requests.RegisterRequest;
import KilimanJARo.P2P.networking.responses.RegisterResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

@RestController
@Profile("server2")
@CrossOrigin
public class Server2Controller {
    private final RestTemplate restTemplate;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String mainServerUrl;
    private String password;
    // TODO: интеграционные тесты
    // TODO: макиты

    @Autowired
    public Server2Controller(@Qualifier("Server2RestTemplate") RestTemplate restTemplate, PropertiesFactoryBean mainServerProperties) throws IOException {
        this.restTemplate = restTemplate;
        Properties properties = mainServerProperties.getObject();
        if (properties != null) {
            this.mainServerUrl = properties.getProperty("server.url");
        } else {
            throw new IOException("Failed to load properties.");
        }
    }


    @GetMapping("/server2")
    public String server2Endpoint() {
        return "Hello from Server 2";
    }

    @GetMapping("/callServer1")
    public String callServer1() {
        String response = restTemplate.getForObject("http://localhost:8081/server1", String.class);
        return "Server 2 received: " + response;
    }

    // @Scheduled(fixedRate = 10000)
    // @GetMapping("/callServer1Number")
    // public Integer callServer1Number() {
    //     String url = "http://localhost:8081/random";
    //     try {
    //         int randomNumber = restTemplate.getForObject(url, Integer.class);
    //         String logMessage = String.format("%s : %s : %d%n", LocalDateTime.now().format(formatter), this.getClass().getSimpleName(), randomNumber);
    //         try (FileWriter writer = new FileWriter("servers.log", true)) {
    //             writer.write(logMessage);
    //         }
    //         return randomNumber;
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     } catch (Exception e) {
    //         System.err.println("Failed to tconnect to Server 1: " + e.getMessage());
    //     }
    //     return 0;
    // }

    @GetMapping("/registerWithMainServer")
    public ResponseEntity<RegisterResponse> registerWithMainServer() {
        RegisterRequest request = new RegisterRequest("Server2");
        ResponseEntity<RegisterResponse> response = restTemplate.postForEntity(mainServerUrl + "/api/register", request, RegisterResponse.class);
        password = response.getBody().getPassword();

        if (response.getBody() != null && response.getBody().isSuccess()) {
            return ResponseEntity.ok(new RegisterResponse(true, "Server registered successfully", response.getBody().getPassword()));
        } else {
            return ResponseEntity.status(500).body(new RegisterResponse(false, "Server registration failed", null));
        }
    }

    @GetMapping("/authWithMainServer")
    public ResponseEntity<AuthResponse> authWithMainServer(@RequestParam String password) {
        AuthRequest request = new AuthRequest("Server2", password);
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(mainServerUrl + "/api/auth", request, AuthResponse.class);
        password = response.getBody().getNextPassword();

        if (response.getBody() != null && response.getBody().isSuccess()) {
            return ResponseEntity.ok(new AuthResponse(true, "Server authenticated successfully", response.getBody().getNextPassword()));
        } else {
            return ResponseEntity.status(500).body(new AuthResponse(false, "Server authentication failed", null));
        }
    }

    @GetMapping("/authWithMainServerAuto")
    public ResponseEntity<AuthResponse> authWithMainServerAuto() {
        AuthRequest request = new AuthRequest("Server2", password);
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(mainServerUrl + "/api/auth", request, AuthResponse.class);
        password = response.getBody().getNextPassword();

        if (response.getBody() != null && response.getBody().isSuccess()) {
            return ResponseEntity.ok(new AuthResponse(true, "Server authenticated successfully", response.getBody().getNextPassword()));
        } else {
            return ResponseEntity.status(500).body(new AuthResponse(false, "Server authentication failed", null));
        }
    }

    @GetMapping("/logoutFromMainServer")
    public ResponseEntity<LogoutResponse> logoutFromMainServer() {
        LogoutRequest request = new LogoutRequest("Server2");
        ResponseEntity<LogoutResponse> response = restTemplate.postForEntity(mainServerUrl + "/api/logout", request, LogoutResponse.class);

        if (response.getBody() != null && response.getBody().isSuccess()) {
            return ResponseEntity.ok(new LogoutResponse(true, "Logged out successfully"));
        } else {
            return ResponseEntity.status(500).body(new LogoutResponse(false, "Logout failed"));
        }
    }
}*/
