package KilimanJARo.P2P.server;

import KilimanJARo.P2P.monitors.UserConnectionMonitor;
import KilimanJARo.P2P.requests.AuthRequest;
import KilimanJARo.P2P.requests.RegisterRequest;
import KilimanJARo.P2P.responses.AuthResponse;
import KilimanJARo.P2P.responses.RegisterResponse;
import KilimanJARo.P2P.user.User;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@SpringBootApplication
public class CentralServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(CentralServerApplication.class, "--spring.config.name=main_server", "--spring.profiles.active=main_server");
    }
}

@RestController
@RequestMapping("/api")
@Profile("main_server")
class CentralServerController {
    private final Map<Integer, Tunnel> tunnels = new HashMap<>();
    private final Map<Integer, User> users = new HashMap<>();
    private final Random random = new Random();
    private final UserConnectionMonitor userConnectionMonitor = new UserConnectionMonitor();

    @PostMapping("/register")
    public RegisterResponse register(@RequestBody RegisterRequest request) {
        if (users.values().stream().anyMatch(user -> user.getUsername().equals(request.getName()))) {
            return new RegisterResponse(false,  "User already exists");
        }
        int userId;
        do {
            userId = random.nextInt(10000);
        } while (users.containsKey(userId));

        String passwordHash = hashPassword(request.getPassword());
        User newUser = new User(userId, request.getName(), passwordHash);
        users.put(userId, newUser);
        return new RegisterResponse(true, "User registered successfully");
    }

    @PostMapping("/auth")
    public AuthResponse auth(@RequestBody AuthRequest request) {
        User user = users.get(request.getName());
        if (user != null && user.isCorrectPassword(request.getPassword())) {
            String newPassword = generateRandomPassword();
            user.setPass(newPassword);
            userConnectionMonitor.userConnected(request.getName());
            return new AuthResponse(true, newPassword);
        }
        return null;
    }

    private String generateRandomPassword() {
        return Integer.toString(random.nextInt(10000));
    }

    private String hashPassword(String password) {
        return Integer.toString(password.hashCode());
    }
}