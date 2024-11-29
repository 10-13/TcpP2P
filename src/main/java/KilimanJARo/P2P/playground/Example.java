package KilimanJARo.P2P.playground;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class Example {
    public static void main(String[] args) throws InterruptedException {
        new Thread(() -> {
            KilimanJARo.P2P.server.CentralServerApplication.main(null);
        }).start();

        Thread.sleep(10000);

        new Thread(() -> {
            ConfigurableApplicationContext context = SpringApplication.run(KilimanJARo.P2P.playground.Server2Application.class, "--spring.config.name=Server2Application", "--spring.profiles.active=server2");
            Server2Controller server2Controller = context.getBean(Server2Controller.class);
            server2Controller.registerWithMainServer();
            server2Controller.authWithMainServerAuto();
            server2Controller.logoutFromMainServer();
        }).start();
    }
}
