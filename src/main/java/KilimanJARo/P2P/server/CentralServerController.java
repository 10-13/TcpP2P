package KilimanJARo.P2P.server;

import KilimanJARo.P2P.client.SmartProperties;
import KilimanJARo.P2P.client.tunneling.Tunnel;
import KilimanJARo.P2P.networking.requests.AuthRequest;
import KilimanJARo.P2P.networking.requests.RegisterRequest;
import KilimanJARo.P2P.networking.responses.AuthResponse;
import KilimanJARo.P2P.networking.responses.RegisterResponse;
import KilimanJARo.P2P.networking.requests.LogoutRequest;
import KilimanJARo.P2P.networking.responses.LogoutResponse;
import KilimanJARo.P2P.server.monitors.UserConnectionMonitor;
import KilimanJARo.P2P.user.User;
import com.github.edouardswiac.zerotier.ZTService;
import com.github.edouardswiac.zerotier.ZTServiceImpl;
import com.github.edouardswiac.zerotier.api.ZTNetworkMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class CentralServerController {
	private final Map<Integer, Tunnel> tunnels = new HashMap<>();
	private final Map<String, User> users = new HashMap<>();
	private final Map<String, ZTNetworkMember> userToZT = new HashMap<>();
	private final Random random = new Random();
	private final UserConnectionMonitor userConnectionMonitor = new UserConnectionMonitor();
	private static final Logger logger = Logger.getLogger(CentralServerController.class.getName());
	private final RestTemplate restTemplate;
	private final SmartProperties properties;
	private String zerotierAddress;
	private final String token;
	private final String networkId;
	private ZTService ztService;

	@Autowired
	public CentralServerController(
			@Qualifier("CentralServerRestTemplate") RestTemplate restTemplate,
			@Qualifier("publicProperties") PropertiesFactoryBean publicProperties,
			@Qualifier("centralServerProperties") PropertiesFactoryBean serverProperties)
			throws IOException, InterruptedException {
		try {
			FileHandler fileHandler = new FileHandler("central_server.log", true);
			fileHandler.setFormatter(new SimpleFormatter());
			logger.addHandler(fileHandler);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.restTemplate = restTemplate;
		this.properties =
				new SmartProperties(publicProperties.getObject(), serverProperties.getObject());
		networkId = properties.getProperty("network_id");
		token = System.getenv("ZEROTIER_TOKEN");
		getZeroTierAddress();
		ztService = new ZTServiceImpl(token);
		ZTNetworkMember ztNetworkMember = new ZTNetworkMember(networkId, zerotierAddress);
		ztService.createNetworkMember(ztNetworkMember);
		ztNetworkMember.getConfig().setAuthorized(true);
		ztService.updateNetworkMember(ztNetworkMember);
	}

	private void getZeroTierAddress() throws IOException {
		try {
			ProcessBuilder processBuilder = new ProcessBuilder("zerotier-cli", "info");
			Process process = processBuilder.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = reader.readLine();

			if (line != null) {
				String[] outputParts = line.split("\\s+");
				if (outputParts.length >= 3) {
					zerotierAddress = outputParts[2];
				}
			} else {
				throw new RuntimeException("Failed to get ZeroTier address");
			}

			process.waitFor();

		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@PostMapping("/register")
	public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
		if (users.values().stream().anyMatch(user -> user.getUsername().equals(request.name()))) {
			logger.info("Registration failed: User already exists - " + request.name());
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(new RegisterResponse(false, "User already exists", null));
		}
		String password = generateRandomPassword();
		String passwordHash = hashPassword(password);
		User newUser = new User(request.name(), passwordHash);
		users.put(newUser.getUsername(), newUser);
		ZTNetworkMember ztNetworkMember = new ZTNetworkMember(networkId, request.zerotierAddress());
		ztService.createNetworkMember(ztNetworkMember);
		userToZT.put(newUser.getUsername(), ztNetworkMember);
		logger.info("User registered successfully: " + request.name() + " " + password + "\n");
		return ResponseEntity.ok(new RegisterResponse(true, "User registered successfully", password));
	}


	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
		User user = users.get(request.name());
		if (!checkCredentials(request.name(), request.password())) {
			logger.info("Authentication failed: Invalid credentials - " + request.name());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(false, "Authentication failed", null));
		}
		String newPassword = generateRandomPassword();
		user.setPass(newPassword);
		user.setCurrentPort(request.port());
		userConnectionMonitor.userConnected(request.name());
		ZTNetworkMember ztNetworkMember = userToZT.get(request.name());
		ztNetworkMember.getConfig().setAuthorized(true);
		ztService.updateNetworkMember(ztNetworkMember);
		logger.info("User authenticated successfully: " + request.name() + " " + newPassword + "\n");
		return ResponseEntity.ok(new AuthResponse(true, "User authenticated", newPassword));

	}

	@PostMapping("/logout")
	public ResponseEntity<LogoutResponse> logout(@RequestBody LogoutRequest request) {
		String username = request.username();
		if (!checkIfOnline(username)) {
			logger.info("Logout failed: User not found - " + username);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new LogoutResponse(false, "User not found"));
		}
		userConnectionMonitor.userDisconnected(username);
		ZTNetworkMember ztNetworkMember = userToZT.get(username);
		ztNetworkMember.getConfig().setAuthorized(false);
		ztService.updateNetworkMember(ztNetworkMember);
		logger.info("User logged out successfully: " + username + "\n");
		return ResponseEntity.ok(new LogoutResponse(true, "Logged out successfully"));
	}
/*
  @PostMapping("/makeConnection")
  public ResponseEntity<EstablishConnectionResponse> makeConnection(@RequestBody EstablishConnectionRequest request) {
      if (!checkIfOnline(request.from())) {
          logger.info("Connection failed: User not found - " + request.from());
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new EstablishConnectionResponse(false, "User not found", null));
      }

      if (!checkIfOnline(request.to())) {
          logger.info("Connection failed: User not found - " + request.to());
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new EstablishConnectionResponse(false, "User not found", null));
      }

      EstablishConnectionRequest requestToRecipient = new EstablishConnectionRequest(request.from(), request.to());
      HttpHeaders headers = new HttpHeaders();
      // headers.setBasicAuth("username", "password");
      // headers.set("Content-Type", "application/json");

      RequestEntity<EstablishConnectionRequest> requestEntity = RequestEntity
              .post(URI.create(properties.getProperty("client.api.requestConnectionIn.url", Map.of("client.url", users.get(to).getCurrentPort()))))
              .headers(headers)
              .body(requestToRecipient);

      ResponseEntity<EstablishConnectionResponse> response = restTemplate.exchange(requestEntity, EstablishConnectionResponse.class);

      if (response.getBody() != null && response.getBody().isAllowed()) {
          logger.info("Connection established: " + request.from() + " -> " + request.to());
          if (build(request.from(), request.to())) {
              return connectionEstablished(request.from(), request.to());
          } else {
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new EstablishConnectionResponse(false, "Connection failed"));
          }
      } else {
          logger.info("Connection not allowed: " + request.from() + " -> " + request.to());
          return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new EstablishConnectionResponse(false, "Connection not allowed"));
      }
  }

  private ResponseEntity<EstablishConnectionResponse> connectionEstablished(String from, String to) {
      EstablishConnectionRequest requestToRecipient = new EstablishConnectionRequest(from, to);
      HttpHeaders headers = new HttpHeaders();
      // headers.setBasicAuth("username", "password");
      // headers.set("Content-Type", "application/json");

      RequestEntity<EstablishConnectionRequest> requestEntity = RequestEntity
              .post(URI.create(properties.getProperty("client.api.connectionEstablished.url")))
              .headers(headers)
              .body(requestToRecipient);

      ResponseEntity<EstablishConnectionResponse> response = restTemplate.exchange(requestEntity, EstablishConnectionResponse.class);

      if (response.getBody() != null && response.getBody().isAllowed()) {
          logger.info("Connection established: " + from + " -> " + to);
          return ResponseEntity.ok(new EstablishConnectionResponse(true, "Connection established with " + to));
      } else {
          logger.info("Failed to establish connection: " + from + " -> " + to);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new EstablishConnectionResponse(false, "Failed to establish connection with " + to));
      }
  }

  private boolean build(String from, String to) {
      User[] thirdPartyCandidates = userConnectionMonitor.getCandidates(from, to, 2);
      boolean success = true;
      if (thirdPartyCandidates == null) {
          success &= makeTube(null, from, to);
          success &= makeTube(from, to, null);
          if (!success) {
              closeTube(null, from, to);
              closeTube(from, to, null);
          }
          return success;
      }

      List<String[]> triplets = buildTriplets(from, to, thirdPartyCandidates);
      for (String[] triplet : triplets) {
          success = makeTube(triplet[0], triplet[1], triplet[2]);
          if (!success) {
              for (String[] triplet1 : triplets) {
                  closeTube(triplet1[0], triplet1[1], triplet1[2]);
              }
              return success;
          }
      }
      return success;
  }

  public List<String[]> buildTriplets(String from, String to, User[] thirdParty) {
      List<String[]> triplets = new ArrayList<>();
      String previous = from;

      for (User user : thirdParty) {
          String current = user.getUsername();
          triplets.add(new String[]{Objects.equals(previous, from) ? null : previous, previous, current});
          previous = current;
      }

      triplets.add(new String[]{previous, to, null});
      return triplets;
  }

  */
	/** This method is for test purposes only and will be removed later. */
  /*
  @Deprecated
  @GetMapping("/users")
  public String getUsers() {
      StringBuilder usersList = new StringBuilder();
      for (User user : users.values()) {
          usersList.append(user.toString()).append("\n");
      }
      return usersList.toString();
  }

  */

	/**
	 * This method is for test purposes only and will be removed later.
	 */
  /*
  @Deprecated
  @GetMapping("/online_users")
  public String getOnlineUsers() {
      StringBuilder onlineUsersList = new StringBuilder();
      for (String username : userConnectionMonitor.getOnlineUsers()) {
          onlineUsersList.append(username).append("\n");
      }
      return onlineUsersList.toString();
  }

  @Deprecated
  public ArrayList<User> getUsersList() {
      return new ArrayList<>(users.values());
  }

  @Deprecated
  public ArrayList<String> getOnlineUsersList() {
      return userConnectionMonitor.getOnlineUsers();
  }
  */
	private boolean checkIfOnline(String username) {
		return userConnectionMonitor.isUserConnected(username);
	}

	private boolean checkCredentials(String username, String password) {
		User user = users.get(username);
		return user != null
				&& user.isCorrectPassword(hashPassword(password));
	}

	private String generateRandomPassword() {
		return Integer.toString(random.nextInt(10000));
	}

	private String hashPassword(String password) {
		return Integer.toString(password.hashCode());
	}
}
