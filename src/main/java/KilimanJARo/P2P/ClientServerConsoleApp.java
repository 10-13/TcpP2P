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
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

import java.net.URI;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public class ClientServerConsoleApp {
	private static final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
	private static final Logger logger = LoggerFactory.getLogger(ClientServerConsoleApp.class);
	private static final Object LOCK = new Object();
	private static volatile boolean isHttpRequestProcessing = false;
	private static SmartProperties properties;
	private static final RestTemplate restTemplate = new RestTemplate();
	private static final Scanner scanner = new Scanner(System.in);
	private static int httpPort;
	private static int tcpPort;
	private static ServerSocket tcpServer;
	private static ExecutorService executorService;
	private static HttpServer httpServer;

	private static class ConsoleReader implements Runnable {
		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					String input = scanner.nextLine();
					inputQueue.put(input);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
	}

	private static String getUserInput() throws InterruptedException {
		String input = inputQueue.poll(5, TimeUnit.MINUTES);
		if (input == null) {
			throw new InterruptedException("Input timeout");
		}
		return input;
	}

	private static void startHttpServer() {
		try {
			httpPort = Integer.parseInt(properties.getProperty("front.port.http"));
			httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);

			httpServer.createContext("/api/handleConnectionRequest", new ConnectionHandler());

			ExecutorService executor = Executors.newFixedThreadPool(10);
			httpServer.setExecutor(executor);

			httpServer.start();
			logger.info("HTTP Server started on port {}", httpPort);
		} catch (IOException e) {
			logger.error("Failed to start HTTP server", e);
		}
	}

	private static class ConnectionHandler extends JsonHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			synchronized (LOCK) {
				isHttpRequestProcessing = true;
			}
			if ("POST".equals(exchange.getRequestMethod())) {
				try {
					String requestBody = readRequestBody(exchange);
					EstablishConnectionRequest request = parseJson(requestBody, EstablishConnectionRequest.class);

					EstablishConnectionResponse response;
					while (true) {
						System.out.println("\nUser " + request.from() + " wants to connect with you. Accept? y/n \n");
						String ans = inputQueue.take();

						if (ans.equals("n")) {
							response = new EstablishConnectionResponse(false, "Don't want to");
						} else if (ans.equals("y")) {
							response = new EstablishConnectionResponse(true, "Yeah, you're cool");
						} else {
							continue;
						}
						break;
					}

					String jsonResponse = convertToJson(response);
					sendJsonResponse(exchange, 200, jsonResponse);
				} catch (Exception e) {
					sendJsonResponse(exchange, 400, "{\"error\": \"Invalid request\"}");
				}
			} else {
				exchange.sendResponseHeaders(405, -1);
			}
			synchronized (LOCK) {
				isHttpRequestProcessing = false;
				LOCK.notify();
			}
		}
	}

	private abstract static class JsonHandler implements HttpHandler {
		protected void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			byte[] responseBytes = jsonResponse.getBytes();
			exchange.sendResponseHeaders(statusCode, responseBytes.length);

			try (OutputStream os = exchange.getResponseBody()) {
				os.write(responseBytes);
			}
		}

		protected String readRequestBody(HttpExchange exchange) throws IOException {
			return new String(exchange.getRequestBody().readAllBytes());
		}
	}

	private static <T> T parseJson(String json, Class<T> clazz) {
		return new Gson().fromJson(json, clazz);
	}

	private static String convertToJson(Object obj) {
		return new Gson().toJson(obj);
	}


	private static void stopHttpServer() {
		if (httpServer != null) {
			httpServer.stop(0);
			logger.info("HTTP Server stopped");
		}
	}

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
		startHttpServer();
		Thread consoleReaderThread = new Thread(new ConsoleReader());
		consoleReaderThread.setDaemon(true);
		consoleReaderThread.start();

		while (true) {
			synchronized (LOCK) {
				while (isHttpRequestProcessing) {
					try {
						LOCK.wait();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}
			}
			displayMenu();

			try {
				String input = inputQueue.poll(5, TimeUnit.SECONDS);

				int choice;
				try {
					choice = Integer.parseInt(input);
				} catch (NumberFormatException e) {
					System.out.println("Invalid input. Please enter a number.");
					continue;
				}

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
						stopHttpServer();
						System.out.println("Exiting...");
						System.exit(0);
						return;
					case 5:
						makeConnectionOut();
						break;
					default:
						logger.warn("Invalid menu choice: {}", choice);
						System.out.println("Invalid choice. Try again.");
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	private static void makeConnectionOut() {
		try {
			logger.info("Starting making connection out process");

			System.out.print("Enter server name: ");
			String name = getUserInput();
			logger.debug("My name request for server: {}", name);

			System.out.print("Enter target server name: ");
			String nameTo = getUserInput();
			logger.debug("Target name request for server: {}", nameTo);

			EstablishConnectionRequest estRequest = new EstablishConnectionRequest(name, nameTo);

			ResponseEntity<EstablishConnectionResponse> response = restTemplate.postForEntity(
					properties.getProperty("client_server.api.connectionOut.url"),
					estRequest,
					EstablishConnectionResponse.class
			);

			logger.info("MakeConnectionOut response received: {}", response.getBody());
		} catch (Exception e) {
			logger.info("Error during connection out " + e.getMessage());
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
			String name = getUserInput();
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
			String name = getUserInput();
			System.out.print("Enter server password: ");
			String password = getUserInput();

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
			return;
		}
	}

	private static void logoutFromCentralServer() {
		try {
			logger.info("Starting server logout process");

			System.out.print("Enter server name: ");
			String name = getUserInput();

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
			return;
		}
	}

	public static String getProperty(String key) {
		logger.debug("Retrieving property: {}", key);
		return properties.getProperty(key);
	}
}