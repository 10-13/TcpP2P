package KilimanJARo.P2P.playground;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
@EnableScheduling
public class Server2Application {

    public static void main(String[] args) {
        SpringApplication.run(Server2Application.class, "--spring.config.name=Server2Application", "--spring.profiles.active=server2");
    }

    @Bean(name="Server2RestTemplate")
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@RestController
@Profile("server2")
class Server2Controller {
    private final RestTemplate restTemplate;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public Server2Controller(@Qualifier("Server2RestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
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

    @Scheduled(fixedRate = 10000)
    @GetMapping("/callServer1Number")
    public Integer callServer1Number() {
        String url = "http://localhost:8081/random";
        try {
            int randomNumber = restTemplate.getForObject(url, Integer.class);
            String logMessage = String.format("%s : %s : %d%n", LocalDateTime.now().format(formatter), this.getClass().getSimpleName(), randomNumber);
            try (FileWriter writer = new FileWriter("servers.log", true)) {
                writer.write(logMessage);
            }
            return randomNumber;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Failed to connect to Server 1: " + e.getMessage());
        }
        return 0;
    }
}