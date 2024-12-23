package KilimanJARo.P2P;

import KilimanJARo.P2P.client.ClientServerApplication;
import KilimanJARo.P2P.client.ClientServerController;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ExampleClient {
    public static void main(String[] args) throws InterruptedException {
//        ConfigurableApplicationContext clientContext = SpringApplication.run(ClientServerApplication.class, "--spring.config.name=client_server_private");
//        ClientServerController clientController = clientContext.getBean(ClientServerController.class);
//        clientController.registerWithCentralServerAuto();
//        clientController.authWithCentralServerAuto();
//        Thread.sleep(10000);
//        clientController.logoutFromCentralServerAuto();
//        clientContext.close();
        Front f = new Front();
    }


}

class Front {
    JFrame window;

    void StartHTTPTCP() {

    }

    void ReceiveMsg(String msg) {

    }

    void SendMsg(String msg) {

    }

    public Front() {
        window = new JFrame("Chat Interface");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setSize(400, 600);

        JPanel container = new JPanel(new BorderLayout());

        JTextArea displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setLineWrap(true);
        displayArea.setWrapStyleWord(true);
        JScrollPane displayScroll = new JScrollPane(displayArea);
        container.add(displayScroll, BorderLayout.CENTER);

        JPanel inputContainer = new JPanel(new BorderLayout());
        JTextField messageInput = new JTextField();
        JButton sendBtn = new JButton("Send");

        inputContainer.add(messageInput, BorderLayout.CENTER);
        inputContainer.add(sendBtn, BorderLayout.EAST);

        container.add(inputContainer, BorderLayout.SOUTH);

        sendBtn.addActionListener((ActionEvent e) -> {
            String message = messageInput.getText().trim();
            if (!message.isEmpty()) {
                displayArea.append("You: " + message + "\n");
                messageInput.setText("");
            }
        });

        messageInput.addActionListener((ActionEvent e) -> sendBtn.doClick());

        window.add(container);
        StartHTTPTCP();
        window.setVisible(true);
    }
}