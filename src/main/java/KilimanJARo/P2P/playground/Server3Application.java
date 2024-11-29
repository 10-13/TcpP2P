/*
package KilimanJARo.P2P.playground;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
@EnableScheduling
public class Server3Application {

    public static void main(String[] args) {
        SpringApplication.run(Server3Application.class, "--spring.config.name=Server3Application", "--spring.profiles.active=server3");
    }

    @Bean(name="Server3RestTemplate")
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@RestController
@Profile("server3")
class Server3Controller {
    private final RestTemplate restTemplate;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public Server3Controller(@Qualifier("Server3RestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    @GetMapping("/server3")
    public String server3Endpoint() {
        return "Hello from Server 3";
    }

    @GetMapping("/callServer2")
    public String callServer2() {
        String response = restTemplate.getForObject("http://localhost:8088/server2", String.class);
        return "Server 3 received: " + response;
    }

    @Scheduled(fixedRate = 10000)
    @GetMapping("/callServer1Number")
    public void callServer1Number() {
        String url = "http://localhost:8082/callServer1Number";
        try {
            int randomNumber = restTemplate.getForObject(url, Integer.class);
            String logMessage = String.format("%s : %s : %d%n", LocalDateTime.now().format(formatter), this.getClass().getSimpleName(), randomNumber);
            try (FileWriter writer = new FileWriter("servers.log", true)) {
                writer.write(logMessage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Failed to connect to Server 1: " + e.getMessage());
        }
    }
}*/
