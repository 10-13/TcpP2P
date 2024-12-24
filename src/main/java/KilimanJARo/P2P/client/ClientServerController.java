package KilimanJARo.P2P.client;

import KilimanJARo.P2P.client.tunneling.Tunnel;
import KilimanJARo.P2P.networking.requests.*;
import KilimanJARo.P2P.networking.responses.*;
import KilimanJARo.P2P.utils.RandomStringGenerator;
import KilimanJARo.P2P.utils.SmartProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ClientServerController {
	private static final String AUTO_USERNAME = "ServerVova4";
	private final RestTemplate restTemplate;
	private final SmartProperties properties;
	private String username = "";
	private String zerotierAddress;

	/**
	 * This field is for test purposes only and will be removed later and moved to front.
	 */
	@Deprecated
	private String password;

	// TODO: здесь надо хранить TunnelKey: Tunnel
	private final Map<String, Tunnel> tunnels = new HashMap<>();

	@Autowired
	public ClientServerController(@Qualifier("ClientServerRestTemplate") RestTemplate restTemplate, @Qualifier("publicProperties") PropertiesFactoryBean publicProperties, @Qualifier("clientServerProperties") PropertiesFactoryBean serverProperties) throws IOException {
		this.restTemplate = restTemplate;
		this.properties = new SmartProperties(publicProperties.getObject(), serverProperties.getObject());
		getZeroTierAddress();
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
			System.out.println("My zerotier address " + zerotierAddress);

		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}


	/*
	 * Registers client server with central server. Gets a request from front.
	 * Sends a request to central server.
	 * Returns a response to front.
	 */
	@PostMapping("/registerWithCentralServer")
	public ResponseEntity<RegisterResponse> registerWithCentralServer(@RequestBody RegisterRequest request) {
		username = request.name();
		RegisterRequest requestToCentral = new RegisterRequest(username);
		HttpHeaders headers = new HttpHeaders();
		// headers.setBasicAuth("username", "password");
		// headers.set("Content-Type", "application/json");
		RequestEntity<RegisterRequest> requestEntity =
				RequestEntity.post(URI.create(properties.getProperty("central_server.api.register.url")))
						.headers(headers)
						.body(requestToCentral);
		ResponseEntity<RegisterResponse> response = restTemplate.exchange(requestEntity, RegisterResponse.class);

		if (!response.getStatusCode().isError() && response.getBody() != null && response.getBody().isSuccess()) {
			return ResponseEntity.ok(new RegisterResponse(true, "Server registered successfully", response.getBody().password()));
		} else {
			return ResponseEntity.status(500).body(new RegisterResponse(false, "Server registration failed", null));
		}
	}

	@Deprecated
	public ResponseEntity<RegisterResponse> registerWithCentralServerAuto() {
		username = AUTO_USERNAME;

		RegisterRequest requestToCentral = new RegisterRequest(username);
		HttpHeaders headers = new HttpHeaders();
		// headers.setBasicAuth("username", "password");
		// headers.set("Content-Type", "application/json");
		RequestEntity<RegisterRequest> requestEntity =
				RequestEntity.post(URI.create(properties.getProperty("central_server.api.register.url")))
						.headers(headers)
						.body(requestToCentral);
		ResponseEntity<RegisterResponse> response = restTemplate.exchange(requestEntity, RegisterResponse.class);
		password = response.getBody().password();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter("client_logs.txt", true))) {
			writer.write("Server registered successfully " + username + " " + password);
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try (BufferedWriter writer = new BufferedWriter(new FileWriter("client_pass.txt", true))) {
			writer.write(password);
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (response.getBody() != null && response.getBody().isSuccess()) {
			return ResponseEntity.ok(new RegisterResponse(true, "Server registered successfully", response.getBody().password()));
		} else {
			return ResponseEntity.status(500).body(new RegisterResponse(false, "Server registration failed", null));
		}
	}

	/*
	 * Authenticates client server with central server. Gets a request from front. Send response to front.
	 */
	@PostMapping("/authWithCentralServer")
	public ResponseEntity<AuthResponse> authWithCentralServer(@RequestBody AuthRequest request) {
		AuthRequest requestToCentral = new AuthRequest(request.name(), request.password(), zerotierAddress, Integer.parseInt(properties.getProperty("private.server.port")));
		HttpHeaders headers = new HttpHeaders();
		// headers.setBasicAuth("username", "password");
		// headers.set("Content-Type", "application/json");

		RequestEntity<AuthRequest> requestEntity = RequestEntity
				.post(URI.create(properties.getProperty("central_server.api.login.url")))
				.headers(headers)
				.body(requestToCentral);

		ResponseEntity<AuthResponse> response = restTemplate.exchange(requestEntity, AuthResponse.class);
		password = response.getBody().nextPassword();

		if (response.getBody() != null && response.getBody().success()) {
			return ResponseEntity.ok(new AuthResponse(true, "Server authenticated successfully", response.getBody().nextPassword()));
		} else {
			return ResponseEntity.status(500).body(new AuthResponse(false, "Server authentication failed", null));
		}
	}

	@Deprecated
	public ResponseEntity<AuthResponse> authWithCentralServerAuto() {
		username = AUTO_USERNAME;
		String lastLine = "";
		try (BufferedReader reader = new BufferedReader(new FileReader("client_pass.txt"))) {
			String currentLine;
			while ((currentLine = reader.readLine()) != null) {
				lastLine = currentLine;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Last line: " + lastLine);
		password = lastLine;
		AuthRequest request = new AuthRequest(username, password, zerotierAddress, Integer.parseInt(properties.getProperty("private.server.port")));
		HttpHeaders headers = new HttpHeaders();
		// headers.setBasicAuth("username", "password");
		// headers.set("Content-Type", "application/json");

		RequestEntity<AuthRequest> requestEntity = RequestEntity
				.post(URI.create(properties.getProperty("central_server.api.login.url")))
				.headers(headers)
				.body(request);

		ResponseEntity<AuthResponse> response = restTemplate.exchange(requestEntity, AuthResponse.class);
		password = response.getBody().nextPassword();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter("client_logs.txt", true))) {
			writer.write("Server logged in successfully " + username + " " + password);
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (BufferedWriter writer = new BufferedWriter(new FileWriter("client_pass.txt", true))) {
			writer.write(password);
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (response.getBody() != null && response.getBody().success()) {
			return ResponseEntity.ok(new AuthResponse(true, "Server authenticated successfully", response.getBody().nextPassword()));
		} else {
			return ResponseEntity.status(500).body(new AuthResponse(false, "Server authentication failed", null));
		}
	}

	/*
	 * Logs out client server from central server. Gets a request from front. Sends a request to central server. Returns a response to front.
	 */
	@PostMapping("/logoutFromCentralServer")
	public ResponseEntity<LogoutResponse> logoutFromCentralServer(@RequestBody LogoutRequest request) {
		LogoutRequest requestToCentral = new LogoutRequest(request.username());
		HttpHeaders headers = new HttpHeaders();
		// headers.setBasicAuth("username", "password");
		// headers.set("Content-Type", "application/json");

		RequestEntity<LogoutRequest> requestEntity = RequestEntity
				.post(URI.create(properties.getProperty("central_server.api.logout.url")))
				.headers(headers)
				.body(requestToCentral);

		ResponseEntity<LogoutResponse> response = restTemplate.exchange(requestEntity, LogoutResponse.class);

		if (response.getBody() != null && response.getBody().isSuccess()) {
			return ResponseEntity.ok(new LogoutResponse(true, "Logged out successfully"));
		} else {
			return ResponseEntity.status(500).body(new LogoutResponse(false, "Logout failed"));
		}
	}

	@Deprecated
	public ResponseEntity<LogoutResponse> logoutFromCentralServerAuto() {
		username = AUTO_USERNAME;
		LogoutRequest requestToCentral = new LogoutRequest(username);
		HttpHeaders headers = new HttpHeaders();
		// headers.setBasicAuth("username", "password");
		// headers.set("Content-Type", "application/json");

		RequestEntity<LogoutRequest> requestEntity = RequestEntity
				.post(URI.create(properties.getProperty("central_server.api.logout.url")))
				.headers(headers)
				.body(requestToCentral);

		ResponseEntity<LogoutResponse> response = restTemplate.exchange(requestEntity, LogoutResponse.class);

		if (response.getBody() != null && response.getBody().isSuccess()) {
			return ResponseEntity.ok(new LogoutResponse(true, "Logged out successfully"));
		} else {
			return ResponseEntity.status(500).body(new LogoutResponse(false, "Logout failed"));
		}
	}

	@PostMapping("/makeTube")
	public ResponseEntity<CreateTunnelResponse> makeTube(@RequestBody CreateTunnelRequest request) {
		String from = request.from();
		String to = request.to();
		String tunnel_id = request.tunnelId();
		if (from == null && to == null) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CreateTunnelResponse(false));
		}
		if (from == null) {
			from = properties.getProperty("front.port.tcp");
			if (from == null) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CreateTunnelResponse(false));
			}
		}
		if (to == null) {
			to = properties.getProperty("front.port.tcp");
			if (to == null) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CreateTunnelResponse(false));
			}
		}
		Tunnel tunnel = Tunnel.Create(from, to, tunnel_id);
		if (tunnel == null) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CreateTunnelResponse(false));
		}

		tunnels.put(tunnel_id, tunnel);
		return ResponseEntity.ok(new CreateTunnelResponse(true));
	}

	@PostMapping("/closeTube")
	public ResponseEntity<CloseTunnelResponse> closeTube(@RequestBody CloseTunnelRequest request) {
		try {
			String tunnel_id = request.tunnelId();
			tunnels.get(tunnel_id).close();
			tunnels.remove(tunnel_id);
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CloseTunnelResponse(false, e.getMessage()));
		}
		return ResponseEntity.ok(new CloseTunnelResponse(true, "Tunnel closed successfully"));
	}

	@PostMapping("/requestConnectionIn")
	public ResponseEntity<EstablishConnectionResponse> requestConnectionIn(@RequestBody EstablishConnectionRequest request) {
		ResponseEntity<EstablishConnectionResponse> response = restTemplate.postForEntity(properties.getProperty("front.api.handleConnectionRequest.url"), request, EstablishConnectionResponse.class);

		if (response.getBody() != null && response.getBody().isAllowed()) {
			return ResponseEntity.ok(new EstablishConnectionResponse(true, "Connection allowed"));
		} else {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new EstablishConnectionResponse(false, "Connection denied"));
		}
	}

	@PostMapping("/establishConnection")
	public ResponseEntity<Void> establishedConnection(@RequestBody ConnectionEstablishedRequest request) {
		String url = properties.getProperty("front.api.connectionEstablished.url");
		restTemplate.postForEntity(url, request, Void.class);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/requestConnectionOut")
	public ResponseEntity<EstablishConnectionResponse> requestConnectionOut(@RequestBody EstablishConnectionRequest request) {
		EstablishConnectionRequest requestToCentral = new EstablishConnectionRequest(username, request.to());
		HttpHeaders headers = new HttpHeaders();
		// headers.setBasicAuth("username", "password");
		// headers.set("Content-Type", "application/json");
		RequestEntity<EstablishConnectionRequest> requestEntity =
				RequestEntity.post(URI.create(properties.getProperty("central_server.api.requestConnection.url")))
						.headers(headers)
						.body(requestToCentral);
		ResponseEntity<EstablishConnectionResponse> response = restTemplate.exchange(requestEntity, EstablishConnectionResponse.class);
		if (response.getBody() != null && response.getBody().isAllowed()) {
			return ResponseEntity.ok(response.getBody());
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new EstablishConnectionResponse(false, "failure"));
	}

	@PostMapping("/requestCloseTube")
	public ResponseEntity<CloseConnectionResponse> requestCloseTube(@RequestBody CloseConnectionRequest request) {
		String usernameIn = request.username();
		CloseConnectionRequest requestToCentral = new CloseConnectionRequest(usernameIn);
		HttpHeaders headers = new HttpHeaders();
		// headers.setBasicAuth("username", "password");
		// headers.set("Content-Type", "application/json");

		RequestEntity<CloseConnectionRequest> requestEntity = RequestEntity
				.post(URI.create(properties.getProperty("central_server.api.closeConnection.url")))
				.headers(headers)
				.body(requestToCentral);

		ResponseEntity<CloseConnectionResponse> response = restTemplate.exchange(requestEntity, CloseConnectionResponse.class);

		if (response.getBody() != null && response.getBody().isSuccess()) {
			return ResponseEntity.ok(new CloseConnectionResponse(true));
		} else {
			return ResponseEntity.status(500).body(new CloseConnectionResponse(false));
		}
	}

	@PostMapping("/connectionClosed")
	public ResponseEntity<Void> connectionClosed(@RequestBody CloseConnectionRequest request) {
		String url = properties.getProperty("front.api.connectionClosed.url");
		restTemplate.postForEntity(url, request, Void.class);
		return ResponseEntity.ok().build();
	}

	private String generateLocalId() {
		String local_id;
		do {
			local_id = RandomStringGenerator.generateRandomString(10);
		} while (tunnels.containsKey(local_id));
		return local_id;
	}
}
