package KilimanJARo.P2P;

import KilimanJARo.P2P.client.ClientServerApplication;
import KilimanJARo.P2P.client.ClientServerController;
import KilimanJARo.P2P.server.CentralServerApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class ExampleCentral {
	public static void main(String[] args) throws InterruptedException {
		new Thread(() -> {
			CentralServerApplication.main(null);
		}).start();

		Thread.sleep(10000);
	}
}