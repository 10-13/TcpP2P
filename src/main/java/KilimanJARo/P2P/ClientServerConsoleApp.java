package KilimanJARo.P2P;

import KilimanJARo.P2P.networking.requests.AuthRequest;
import KilimanJARo.P2P.networking.requests.LogoutRequest;
import KilimanJARo.P2P.networking.requests.RegisterRequest;
import KilimanJARo.P2P.networking.responses.AuthResponse;
import KilimanJARo.P2P.networking.responses.LogoutResponse;
import KilimanJARo.P2P.networking.responses.RegisterResponse;
import KilimanJARo.P2P.utils.SmartProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.Scanner;

public class ClientServerConsoleApp {
	private static SmartProperties properties;
	private static final RestTemplate restTemplate = new RestTemplate();
	private static final Scanner scanner = new Scanner(System.in);

	static {
		try {
			Resource resource_public = new ClassPathResource("public.properties");
			Resource resource_private = new ClassPathResource("front_private.properties");

			properties = new SmartProperties(PropertiesLoaderUtils.loadProperties(resource_public), PropertiesLoaderUtils.loadProperties(resource_private));
		} catch (IOException e) {
			System.err.println("Error loading properties: " + e.getMessage());
		}
	}

	public static void main(String[] args) {
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
					System.out.println("Exiting...");
					return;
				default:
					System.out.println("Invalid choice. Try again.");
			}
		}
	}

	private static void displayMenu() {
		System.out.println("\n--- Client Server Management ---");
		System.out.println("1. Register with Central Server");
		System.out.println("2. Authenticate");
		System.out.println("3. Logout");
		System.out.println("4. Exit");
		System.out.print("Enter your choice (1-4): ");
	}

	private static void registerWithCentralServer() {
		try {
			System.out.print("Enter server name: ");
			String name = scanner.nextLine();

			RegisterRequest regRequest = new RegisterRequest(name);

			// Отправляем запрос
			ResponseEntity<RegisterResponse> response = restTemplate.postForEntity(
					properties.getProperty("client_server.api.register.url"),
					regRequest,
					RegisterResponse.class
			);

			System.out.println("Response:");
			System.out.println(response.getBody());
		} catch (Exception e) {
			System.out.println("Error during registration: " + e.getMessage());
		}
	}

	private static void authenticateWithCentralServer() {
		try {
			System.out.print("Enter server name: ");
			String name = scanner.nextLine();

			System.out.print("Enter server password: ");
			String password = scanner.nextLine();

			AuthRequest authRequest = new AuthRequest(name, password, "", -1);

			// Отправляем запрос
			ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
					properties.getProperty("client_server.api.login.url"),
					authRequest,
					AuthResponse.class
			);

			System.out.println("Response:");
			System.out.println(response.getBody());
		} catch (Exception e) {
			System.out.println("Error during registration: " + e.getMessage());
		}

	}

	private static void logoutFromCentralServer() {
		try {
			System.out.print("Enter server name: ");
			String name = scanner.nextLine();

			LogoutRequest logoutRequest = new LogoutRequest(name);

			// Отправляем запрос
			ResponseEntity<LogoutResponse> response = restTemplate.postForEntity(
					properties.getProperty("client_server.api.logout.url"),
					logoutRequest,
					LogoutResponse.class
			);

			System.out.println("Response:");
			System.out.println(response.getBody());
		} catch (Exception e) {
			System.out.println("Error during registration: " + e.getMessage());
		}
	}

	public static String getProperty(String key) {
		return properties.getProperty(key);
	}
}