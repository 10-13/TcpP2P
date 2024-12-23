package KilimanJARo.P2P.server;

import KilimanJARo.P2P.client.SmartProperties;
import KilimanJARo.P2P.client.tunneling.Tunnel;
import KilimanJARo.P2P.networking.requests.*;
import KilimanJARo.P2P.networking.responses.*;
import KilimanJARo.P2P.server.database.UserRepository;
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
import java.net.URI;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.reverse;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class CentralServerController {
	private static final int NUMBER_OF_THIRD_PARTY_CANDIDATES = 1;
	private static final int LENGTH_OF_TUNNEL_ID = 10;
	private final BidirectionalMap<String, TunnelKey> tunnels = new BidirectionalMap<>(); // id to TunnelKey
	private final Map<String, List<String>> personToTunnel = new HashMap<>(); // name to List<id>
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
	private UserRepository databaseHandler;

	@Autowired
	public CentralServerController(UserRepository userRepository, @Qualifier("CentralServerRestTemplate") RestTemplate restTemplate, @Qualifier("publicProperties") PropertiesFactoryBean publicProperties, @Qualifier("centralServerProperties") PropertiesFactoryBean serverProperties) throws IOException, InterruptedException {
		try {
			FileHandler fileHandler = new FileHandler("central_server.log", true);
			fileHandler.setFormatter(new SimpleFormatter());
			logger.addHandler(fileHandler);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.databaseHandler = userRepository;
		this.restTemplate = restTemplate;
		this.properties = new SmartProperties(publicProperties.getObject(), serverProperties.getObject());

		List<User> userList = databaseHandler.getAll();
		for (User user : userList) {
			users.put(user.getUsername(), user);
		}

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
		if (users.containsKey(request.name())) {
			logger.info("Registration failed: User already exists - " + request.name());
			return ResponseEntity.status(HttpStatus.CONFLICT).body(new RegisterResponse(false, "User already exists", null));
		}
		String password = generateRandomPassword();
		String passwordHash = hashPassword(password);
		User newUser = new User(request.name(), passwordHash);
		users.put(newUser.getUsername(), newUser);
		databaseHandler.save(newUser);
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
		user.setPasswordHash(hashPassword(newPassword));
		user.setCurrentPort(request.port());
		user.setZerotierAddress(request.zerotierAddress());

		userConnectionMonitor.userConnected(request.name());

		ZTNetworkMember ztNetworkMember = new ZTNetworkMember(networkId, request.zerotierAddress());
		ztNetworkMember.getConfig().setAuthorized(true);
		ztService.createNetworkMember(ztNetworkMember);
		userToZT.put(user.getUsername(), ztNetworkMember);

		databaseHandler.update(user);

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

	@PostMapping("/makeConnection")
	public ResponseEntity<EstablishConnectionResponse> makeConnection(@RequestBody EstablishConnectionRequest request) {
		if (!checkIfOnline(request.from())) {
			logger.info("Connection failed: User not found - " + request.from());
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new EstablishConnectionResponse(false, "User 'from' not found"));
		}

		if (!checkIfOnline(request.to())) {
			logger.info("Connection failed: User not found - " + request.to());
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new EstablishConnectionResponse(false, "User 'to' not found"));
		}

		EstablishConnectionRequest requestToRecipient = new EstablishConnectionRequest(request.from(), request.to());
		HttpHeaders headers = new HttpHeaders();
		// headers.setBasicAuth("username", "password");
		// headers.set("Content-Type", "application/json");

		RequestEntity<EstablishConnectionRequest> requestEntity = RequestEntity.post(URI.create(properties.getProperty("client.api.requestConnectionIn.url", Map.of("client.url", getCentralClientConnection(request.to()))))).headers(headers).body(requestToRecipient);

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

		RequestEntity<EstablishConnectionRequest> requestEntity = RequestEntity.post(URI.create(properties.getProperty("client.api.connectionEstablished.url", Map.of("client.url", getCentralClientConnection(to))))).headers(headers).body(requestToRecipient);

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
		Stream<String> thirdPartyCandidates = userConnectionMonitor.getRandomizedSet(NUMBER_OF_THIRD_PARTY_CANDIDATES);
		boolean success = true;
		personToTunnel.put(from, new ArrayList<>());
		personToTunnel.put(to, new ArrayList<>());
		if (thirdPartyCandidates.findAny().isEmpty()) {
			List<Object> x1 = makeTube(null, from, to);
			List<Object> x2 = makeTube(from, to, null);
			success = (boolean) x1.getFirst() && (boolean) x2.getFirst();
			if (!success) {
				closeTube(null, from, to);
				closeTube(from, to, null);
			}
			personToTunnel.get(from).add((String) x1.get(1));
			personToTunnel.get(to).add((String) x2.get(1));
			return success;
		}

		List<String[]> triplets = buildTriplets(from, to, thirdPartyCandidates);
		for (String[] triplet : triplets) {
			List<Object> x = makeTube(triplet[0], triplet[1], triplet[2]);
			success = (boolean) x.getFirst();
			if (!success) {
				for (String[] triplet1 : triplets) {
					closeTube(triplet1[0], triplet1[1], triplet1[2]);
				}
				break;
			}
			personToTunnel.get(from).add((String) x.get(1));
			personToTunnel.get(to).add((String) x.get(1));
		}
		if (success) {
			reverse(personToTunnel.get(to));
		}
		return success;
	}

	private List<Object> makeTube(String from, String to, String thirdParty) {
		CreateTunnelRequest requestToRecipient = new CreateTunnelRequest(from, thirdParty, to);
		HttpHeaders headers = new HttpHeaders();
		// headers.setBasicAuth("username", "password");
		// headers.set("Content-Type", "application/json");

		RequestEntity<CreateTunnelRequest> requestEntity = RequestEntity.post(URI.create(properties.getProperty("client.api.makeTube.url", Map.of("client.url", getCentralClientConnection(thirdParty))))).headers(headers).body(requestToRecipient);

		ResponseEntity<CreateTunnelResponse> response = restTemplate.exchange(requestEntity, CreateTunnelResponse.class);

		if (response.getBody() != null && response.getBody().isSuccess()) {
			String id = new Random().ints(48, 122)
					.filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
					.mapToObj(i -> String.valueOf((char) i))
					.limit(LENGTH_OF_TUNNEL_ID)
					.collect(Collectors.joining());
			tunnels.put(id, new TunnelKey(from, thirdParty, to));
			logger.info("Created tunnel: " + from + " -> " + thirdParty + " -> " + to);
			return Arrays.asList(true, id);
		} else {
			logger.info("Failed to create tunnel: " + from + " -> " + thirdParty + " -> " + to);
			return Arrays.asList(true, null);
		}
	}

	private void closeTube(String from, String to, String thirdParty) {
		CloseTunnelRequest requestToRecipient = new CloseTunnelRequest();
		HttpHeaders headers = new HttpHeaders();
		// headers.setBasicAuth("username", "password");
		// headers.set("Content-Type", "application/json");

		RequestEntity<CloseTunnelRequest> requestEntity = RequestEntity.post(URI.create(properties.getProperty("client.api.closeTube.url", Map.of("client.url", getCentralClientConnection(thirdParty))))).headers(headers).body(requestToRecipient);

		ResponseEntity<CloseTunnelResponse> response = restTemplate.exchange(requestEntity, CloseTunnelResponse.class);

		if (response.getBody() != null && response.getBody().isSuccess()) {
			tunnels.removeByValue(new TunnelKey(from, thirdParty, to));
			logger.info("Closed tunnel: " + from + " -> " + thirdParty + " -> " + to);
		} else {
			logger.info("Failed to close tunnel: " + from + " -> " + thirdParty + " -> " + to);
		}
	}

	public List<String[]> buildTriplets(String from, String to, Stream<String> thirdParty) {
		List<String[]> triplets = new ArrayList<>();
		String previous = from;

		for (String thirdPartyCandidate : thirdParty.toArray(String[]::new)) {
			triplets.add(new String[]{Objects.equals(previous, from) ? null : previous, previous, thirdPartyCandidate});
			previous = thirdPartyCandidate;
		}

		triplets.add(new String[]{previous, to, null});
		return triplets;
	}


	/**
	 * This method is for test purposes only and will be removed later.
	 */
	@Deprecated
	@GetMapping("/users")
	public String getUsers() {
		List<User> usersList = databaseHandler.getAll();
		StringBuilder usersString = new StringBuilder();
		for (User user : usersList) {
			usersString.append(user.toString()).append("\n");
		}
		return usersString.toString();
	}


	/**
	 * This method is for test purposes only and will be removed later.
	 */

	@Deprecated
	@GetMapping("/online_users")
	public String getOnlineUsers() {
		StringBuilder onlineUsersList = new StringBuilder();
		userConnectionMonitor.getUsers().forEach(username -> {
			onlineUsersList.append(username).append("\n");
		});
		return onlineUsersList.toString();
	}

	@Deprecated
	@GetMapping("/all_users")
	public ArrayList<User> getUsersList() {
		return new ArrayList<>(users.values());
	}
/*


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
		return user != null && user.isCorrectPassword(hashPassword(password));
	}

	private String generateRandomPassword() {
		return Integer.toString(random.nextInt(10000));
	}

	private String hashPassword(String password) {
		return Integer.toString(password.hashCode());
	}

	private String getCentralClientConnection(String username) {
		ZTNetworkMember member = ztService.getNetworkMember(networkId, userToZT.get(username).getNodeId());
		if (member == null || member.getConfig() == null || member.getConfig().getAddress() == null) {
			throw new RuntimeException("Failed to get ZeroTier member information for user: " + username);
		}
		String localIp = member.getConfig().getAddress();
		User user = users.get(username);
		if (user == null) {
			throw new RuntimeException("User not found: " + username);
		}
		int port = user.getCurrentPort();
		return localIp + ":" + port;
	}
}
