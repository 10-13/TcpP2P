package KilimanJARo.P2P;

import KilimanJARo.P2P.networking.requests.AuthRequest;
import KilimanJARo.P2P.networking.requests.EstablishConnectionRequest;
import KilimanJARo.P2P.networking.requests.LogoutRequest;
import KilimanJARo.P2P.networking.requests.RegisterRequest;
import KilimanJARo.P2P.networking.responses.AuthResponse;
import KilimanJARo.P2P.networking.responses.EstablishConnectionResponse;
import KilimanJARo.P2P.networking.responses.LogoutResponse;
import KilimanJARo.P2P.networking.responses.RegisterResponse;
import KilimanJARo.P2P.utils.SmartProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.Scanner;

public class ClientServerConsoleApp {
	private static final Logger logger = LoggerFactory.getLogger(ClientServerConsoleApp.class);

	private static SmartProperties properties;
	private static final RestTemplate restTemplate = new RestTemplate();
	private static final Scanner scanner = new Scanner(System.in);
	private static int httpPort;
	private static int tcpPort;
	private static ServerSocket tcpServer;
	private static ExecutorService executorService;

	static {
		try {
			logger.info("Initializing application properties");
			Resource resource_public = new ClassPathResource("public.properties");
			Resource resource_private = new ClassPathResource("front_private.properties");

			properties = new SmartProperties(
					PropertiesLoaderUtils.loadProperties(resource_public),
					PropertiesLoaderUtils.loadProperties(resource_private)
			);

			httpPort = Integer.parseInt(properties.getProperty("front.port.http"));
			tcpPort = Integer.parseInt(properties.getProperty("front.port.tcp"));

			logger.info("Configured HTTP port: {}, TCP port: {}", httpPort, tcpPort);
		} catch (IOException e) {
			logger.error("Error loading properties", e);
		}
	}

	public static void main(String[] args) {
		logger.info("Starting ClientServerConsoleApp");
		startTcpServer();

		while (true) {
			displayMenu();
			int choice = scanner.nextInt();
			scanner.nextLine();

			switch (choice) {
				case 1:
					registerWithCentralServer();
					break;
				case 2:
					authenticateWithCentralServer();
					break;
				case 3:
					logoutFromCentralServer();
					break;
				case 4:
					logger.info("Exiting application");
					stopTcpServer();
					System.out.println("Exiting...");
					break;
				case 5:
					makeConnectionOut();
					return;
				default:
					logger.warn("Invalid menu choice: {}", choice);
					System.out.println("Invalid choice. Try again.");
			}
		}
	}

	private static void makeConnectionOut() {
		try {
			logger.info("Starting making connection out process");

			System.out.print("Enter server name: ");
			String name = scanner.nextLine();
			logger.debug("My name request for server: {}", name);

			System.out.print("Enter target server name: ");
			String nameTo = scanner.nextLine();
			logger.debug("Target name request for server: {}", nameTo);

			EstablishConnectionRequest estRequest = new EstablishConnectionRequest(name, nameTo);

			ResponseEntity<EstablishConnectionResponse> response = restTemplate.postForEntity(
					properties.getProperty("client_server.api.requestConnectionOut.url"),
					estRequest,
					EstablishConnectionResponse.class
			);

			logger.info("MakeConnectionOut response received: {}", response.getBody());
		} catch (Exception e) {
			logger.error("Error during server registration", e);
		}
	}

	private static void startTcpServer() {
		logger.info("Initializing TCP server on port {}", tcpPort);
		executorService = Executors.newCachedThreadPool();

		try {
			tcpServer = new ServerSocket(tcpPort);
			logger.info("TCP Server started successfully on port {}", tcpPort);

			executorService.submit(() -> {
				while (!tcpServer.isClosed()) {
					try {
						Socket clientSocket = tcpServer.accept();
						logger.info("New TCP connection accepted from {}", clientSocket.getInetAddress());
						handleTcpConnection(clientSocket);
					} catch (IOException e) {
						if (!tcpServer.isClosed()) {
							logger.error("Error accepting TCP connection", e);
						}
					}
				}
			});
		} catch (IOException e) {
			logger.error("Could not start TCP server on port {}", tcpPort, e);
		}
	}

	private static void handleTcpConnection(Socket clientSocket) {
		executorService.submit(() -> {
			try (
					BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
			) {
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					logger.info("Received TCP message: {}", inputLine);
					out.println("Server received: " + inputLine);
				}
			} catch (IOException e) {
				logger.error("Error handling TCP connection", e);
			} finally {
				try {
					clientSocket.close();
					logger.info("TCP connection closed");
				} catch (IOException e) {
					logger.error("Error closing client socket", e);
				}
			}
		});
	}

	private static void stopTcpServer() {
		logger.info("Stopping TCP server");
		if (tcpServer != null) {
			try {
				tcpServer.close();
				logger.info("TCP server closed successfully");
			} catch (IOException e) {
				logger.error("Error closing TCP server", e);
			}
		}

		if (executorService != null) {
			executorService.shutdown();
			logger.info("Executor service shutdown");
		}
	}

	private static void displayMenu() {
		logger.debug("Displaying main menu");
		System.out.println("\n--- Client Server Management ---");
		System.out.println("1. Register with Central Server");
		System.out.println("2. Authenticate");
		System.out.println("3. Logout");
		System.out.println("4. Exit");
		System.out.println("5. Make connection out");
		System.out.print("Enter your choice (1-4): ");
	}

	private static void registerWithCentralServer() {
		try {
			logger.info("Starting server registration process");

			System.out.print("Enter server name: ");
			String name = scanner.nextLine();
			logger.debug("Registration request for server: {}", name);

			RegisterRequest regRequest = new RegisterRequest(name);

			ResponseEntity<RegisterResponse> response = restTemplate.postForEntity(
					properties.getProperty("client_server.api.register.url"),
					regRequest,
					RegisterResponse.class
			);

			logger.info("Registration response received: {}", response.getBody());
		} catch (Exception e) {
			logger.error("Error during server registration", e);
		}
	}

	private static void authenticateWithCentralServer() {
		try {
			logger.info("Starting server authentication process");

			System.out.print("Enter server name: ");
			String name = scanner.nextLine();
			System.out.print("Enter server password: ");
			String password = scanner.nextLine();

			logger.debug("Authentication request for server: {}", name);

			AuthRequest authRequest = new AuthRequest(name, password, "", -1);

			ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
					properties.getProperty("client_server.api.login.url"),
					authRequest,
					AuthResponse.class
			);

			logger.info("Authentication response received: {}", response.getBody());
		} catch (Exception e) {
			logger.error("Error during server authentication", e);
		}
	}

	private static void logoutFromCentralServer() {
		try {
			logger.info("Starting server logout process");

			System.out.print("Enter server name: ");
			String name = scanner.nextLine();

			logger.debug("Logout request for server: {}", name);

			LogoutRequest logoutRequest = new LogoutRequest(name);

			ResponseEntity<LogoutResponse> response = restTemplate.postForEntity(
					properties.getProperty("client_server.api.logout.url"),
					logoutRequest,
					LogoutResponse.class
			);

			logger.info("Logout response received: {}", response.getBody());
		} catch (Exception e) {
			logger.error("Error during server logout", e);
		}
	}

	public static String getProperty(String key) {
		logger.debug("Retrieving property: {}", key);
		return properties.getProperty(key);
	}
}