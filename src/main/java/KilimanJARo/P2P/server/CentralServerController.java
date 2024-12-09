package KilimanJARo.P2P.server;

import KilimanJARo.P2P.client.tunneling.Tunnel;
import KilimanJARo.P2P.server.monitors.UserConnectionMonitor;
import KilimanJARo.P2P.networking.requests.AuthRequest;
import KilimanJARo.P2P.networking.requests.LogoutRequest;
import KilimanJARo.P2P.networking.requests.RegisterRequest;
import KilimanJARo.P2P.networking.responses.AuthResponse;
import KilimanJARo.P2P.networking.responses.LogoutResponse;
import KilimanJARo.P2P.networking.responses.RegisterResponse;
import KilimanJARo.P2P.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class CentralServerController {
    private final Map<Integer, Tunnel> tunnels = new HashMap<>();
    private final Map<String, User> users = new HashMap<>();
    private final Random random = new Random();
    private final UserConnectionMonitor userConnectionMonitor = new UserConnectionMonitor();
    private static final Logger logger = Logger.getLogger(CentralServerController.class.getName());

    public CentralServerController() {
        try {
            FileHandler fileHandler = new FileHandler("central_server.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        if (users.values().stream().anyMatch(user -> user.getUsername().equals(request.name()))) {
            logger.info("Registration failed: User already exists - " + request.name());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new RegisterResponse(false, "User already exists", null));
        }

        String password = generateRandomPassword();
        String passwordHash = hashPassword(password);
        User newUser = new User(request.name(), passwordHash);
        users.put(newUser.getUsername(), newUser);
        logger.info("User registered successfully: " + request.name() + " " + password + "\n");
        return ResponseEntity.ok(new RegisterResponse(true, "User registered successfully", password));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        User user = users.get(request.name());
        if (user != null && user.isCorrectPassword(hashPassword(request.password()))) {
            String newPassword = generateRandomPassword();
            user.setPass(newPassword);
            userConnectionMonitor.userConnected(request.name());
            logger.info("User authenticated successfully: " + request.name() + " " + newPassword + "\n");
            return ResponseEntity.ok(new AuthResponse(true, "User authenticated", newPassword));
        }
        logger.info("Authentication failed: Invalid credentials - " + request.name());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(false, "Authentication failed", null));
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestBody LogoutRequest request) {
        String username = request.username();
        userConnectionMonitor.userDisconnected(username);
        logger.info("User logged out successfully: " + username + "\n");
        return ResponseEntity.ok(new LogoutResponse(true, "Logged out successfully"));
    }

    /**
     * This method is for test purposes only and will be removed later.
     */
    @Deprecated
    @GetMapping("/users")
    public String getUsers() {
        StringBuilder usersList = new StringBuilder();
        for (User user : users.values()) {
            usersList.append(user.toString()).append("\n");
        }
        return usersList.toString();
    }

    /**
     * This method is for test purposes only and will be removed later.
     */
    @Deprecated
    @GetMapping("/online_users")
    public String getOnlineUsers() {
        StringBuilder onlineUsersList = new StringBuilder();
        userConnectionMonitor.getUsers().forEach(username->onlineUsersList.append(username).append("\n"));
        return onlineUsersList.toString();
    }

    @Deprecated
    public ArrayList<User> getUsersList() {
        return new ArrayList<>(users.values());
    }

    @Deprecated
    public ArrayList<String> getOnlineUsersList() {
        return new ArrayList<>(userConnectionMonitor.getUsers().toList());
    }

    private String generateRandomPassword() {
        return Integer.toString(random.nextInt(10000));
    }

    private String hashPassword(String password) {
        return Integer.toString(password.hashCode());
    }
}