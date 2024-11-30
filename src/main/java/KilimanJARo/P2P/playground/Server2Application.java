package KilimanJARo.P2P.playground;

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
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

@SpringBootApplication
@EnableScheduling
public class Server2Application {

    public static void main(String[] args) {
        SpringApplication.run(Server2Application.class, "--spring.config.name=Server2Application", "--spring.profiles.active=server2");
    }

    @Bean(name="Server2RestTemplate")
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate;
    }

    @Value("${main_server_properties}")
    private String mainServerPropertiesPath;

    @Bean(name = "mainServerProperties")
    public PropertiesFactoryBean mainServerProperties() {
        PropertiesFactoryBean bean = new PropertiesFactoryBean();
        bean.setLocation(new ClassPathResource(mainServerPropertiesPath));
        return bean;
    }
}

@RestController
@Profile("server2")
@CrossOrigin
class Server2Controller {
    private final RestTemplate restTemplate;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String mainServerUrl;
    private String password;
    // TODO: интеграционные тесты
    // TODO: макиты

    @Autowired
    public Server2Controller(RestTemplate restTemplate, PropertiesFactoryBean mainServerProperties) throws IOException {
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
}