package KilimanJARo.P2P;

import KilimanJARo.P2P.client.ClientServerApplication;
import KilimanJARo.P2P.client.ClientServerController;
import KilimanJARo.P2P.server.CentralServerApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class Example {
	public static void main(String[] args) throws InterruptedException {
		new Thread(() -> {
			CentralServerApplication.main(null);
		}).start();

		Thread.sleep(10000);

		ConfigurableApplicationContext clientContext = SpringApplication.run(ClientServerApplication.class, "--spring.config.name=client_server_private");
		ClientServerController clientController = clientContext.getBean(ClientServerController.class);
		clientController.registerWithCentralServerAuto();
		clientController.authWithCentralServerAuto();
		Thread.sleep(10000);
		clientController.logoutFromCentralServerAuto();
	}
}