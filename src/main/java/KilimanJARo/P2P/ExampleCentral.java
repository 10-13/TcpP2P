package KilimanJARo.P2P;

import KilimanJARo.P2P.server.CentralServerApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExampleCentral {
	public static void main(String[] args) throws InterruptedException {
		new Thread(() -> {
			CentralServerApplication.main(null);
		}).start();

		Thread.sleep(10000);
	}
}