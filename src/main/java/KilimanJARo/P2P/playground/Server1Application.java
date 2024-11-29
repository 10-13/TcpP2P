package KilimanJARo.P2P.playground;
import java.util.Random;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class Server1Application {
    public static void main(String[] args) {
        SpringApplication.run(Server1Application.class, "--spring.config.name=Server1Application", "--spring.profiles.active=server1");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().permitAll()
                )
                .httpBasic().disable();
        return http.build();
    }
}

@RestController
@Profile("server1")
class Server1Controller {
    @GetMapping("/server1")
    public String server1Endpoint() {
        return "Hello from Server 1";
    }

    @GetMapping("/random")
    public int getRandomNumber() {
        Random random = new Random();
        return random.nextInt(100);
    }
}