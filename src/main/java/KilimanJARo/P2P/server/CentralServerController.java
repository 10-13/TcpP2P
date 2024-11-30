package KilimanJARo.P2P.server;

import KilimanJARo.P2P.server.monitors.UserConnectionMonitor;
import KilimanJARo.P2P.server.requests.AuthRequest;
import KilimanJARo.P2P.server.requests.LogoutRequest;
import KilimanJARo.P2P.server.requests.RegisterRequest;
import KilimanJARo.P2P.server.responses.AuthResponse;
import KilimanJARo.P2P.server.responses.LogoutResponse;
import KilimanJARo.P2P.server.responses.RegisterResponse;
import KilimanJARo.P2P.user.User;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@RestController
@RequestMapping("/api")
@Profile("central_server")
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
            e.printStackTrace();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        if (users.values().stream().anyMatch(user -> user.getUsername().equals(request.getName()))) {
            logger.info("Registration failed: User already exists - " + request.getName());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new RegisterResponse(false, "User already exists", null));
        }
        int userId;
        do {
            userId = random.nextInt(10000);
        } while (users.containsKey(userId));

        String password = generateRandomPassword();
        String passwordHash = hashPassword(password);
        User newUser = new User(userId, request.getName(), passwordHash);
        users.put(newUser.getUsername(), newUser);
        logger.info("User registered successfully: " + request.getName() + " " + password + "\n");
        return ResponseEntity.ok(new RegisterResponse(true, "User registered successfully", password));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        User user = users.get(request.getName());
        if (user != null && user.isCorrectPassword(hashPassword(request.getPassword()))) {
            String newPassword = generateRandomPassword();
            user.setPass(newPassword);
            userConnectionMonitor.userConnected(request.getName());
            logger.info("User authenticated successfully: " + request.getName() + " " + newPassword + "\n");
            return ResponseEntity.ok(new AuthResponse(true, "User authenticated", newPassword));
        }
        logger.info("Authentication failed: Invalid credentials - " + request.getName());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(false, "Authentication failed", null));
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestBody LogoutRequest request) {
        String username = request.getUsername();
        if (userConnectionMonitor.isUserConnected(request.getUsername())) {
            userConnectionMonitor.userDisconnected(username);
            logger.info("User logged out successfully: " + username + "\n");
            return ResponseEntity.ok(new LogoutResponse(true, "Logged out successfully"));
        } else {
            logger.info("Logout failed: User not found - " + username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new LogoutResponse(false, "User not found"));
        }
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
        for (String username : userConnectionMonitor.getOnlineUsers()) {
            onlineUsersList.append(username).append("\n");
        }
        return onlineUsersList.toString();
    }

    private String generateRandomPassword() {
        return Integer.toString(random.nextInt(10000));
    }

    private String hashPassword(String password) {
        return Integer.toString(password.hashCode());
    }
}