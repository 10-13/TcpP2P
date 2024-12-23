package KilimanJARo.P2P;

import KilimanJARo.P2P.client.ClientServerApplication;
import KilimanJARo.P2P.client.ClientServerController;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ExampleClient {
    public static void main(String[] args) throws InterruptedException {
        System.setProperty("java.awt.headless", "false");
        ConfigurableApplicationContext clientContext = SpringApplication.run(ClientServerApplication.class, "--spring.config.name=client_server_private");
        ClientServerController clientController = clientContext.getBean(ClientServerController.class);
        clientController.registerWithCentralServerAuto();
        clientController.authWithCentralServerAuto();
        Front f = new Front(clientController);
        f.Show();
        clientController.logoutFromCentralServerAuto();
        clientContext.close();
    }
}

class Front {
    JFrame window;
    JTextField messageInput;
    JTextArea displayArea;

    public ClientServerController controller;

    ServerSocket socket;
    Socket send;
    HttpServer server;

    Thread frontServer;

    boolean terminate;

    void StartHttpTcp() {
        try {
            socket = new ServerSocket(8011);
        }
        catch (IOException ex) {
            ReceiveMsg("Failed to open socket.");
            return;
        }
        frontServer = new Thread(() -> {
            try {
                var in = send.getInputStream();
                while (!terminate) {
                    int sz = in.read();
                    String str = new String(in.readNBytes(sz));
                    ReceiveMsg(str);
                }
            } catch (IOException ex) {
                ReceiveMsg("Failed to receive. Terminate.");
            }
        });
        try {
            server = HttpServer.create(new InetSocketAddress(8010), 0);
            server.createContext("/api/handleConnectionRequest", new HttpHandler() {
                @Override
                public void handle(HttpExchange ex) throws IOException {
                    String resp = "{\"isAllowed\": true, \"reason\": \"\"}";
                    ex.sendResponseHeaders(200, resp.getBytes().length);
                    var out = ex.getResponseBody();
                    out.write(resp.getBytes());
                    out.close();
                    ReceiveMsg("Connection allowed.");
                }
            });
            server.createContext("/api/establishConnection", (HttpExchange ex)-> {
                send = socket.accept();
                ex.sendResponseHeaders(200, 0);
                var out = ex.getResponseBody();
                out.close();
                ReceiveMsg("Connection allowed.");
            });
            server.createContext("/test", (HttpExchange ex) -> {
                String resp = "{\"isAllowed\": true, \"reason\": \"\"}";
                ex.sendResponseHeaders(200, resp.getBytes().length);
                var out = ex.getResponseBody();
                out.write(resp.getBytes());
                out.close();
                ReceiveMsg("Connection test allowed.");
            });
            server.setExecutor(null);
            server.start();
        } catch (IOException ex) {
            ReceiveMsg("Failed to create listener.");
        }
    }

    void ReceiveMsg(String message) {
        if (!message.isEmpty()) {
            displayArea.append("Rcv: " + message + "\n");
        }
    }

    void SendMsg(String message) {
        if (!message.isEmpty()) {
            displayArea.append("You: " + message + "\n");
            messageInput.setText("");
            try {
                var stream = send.getOutputStream();
                stream.write(message.getBytes().length);
                stream.write(message.getBytes());
            } catch (IOException _) {
                displayArea.append("Failed to send.");
            }
        }
    }

    public Front(ClientServerController controller) {
        window = new JFrame("Chat Interface");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setSize(400, 600);

        JPanel container = new JPanel(new BorderLayout());

        displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setLineWrap(true);
        displayArea.setWrapStyleWord(true);
        JScrollPane displayScroll = new JScrollPane(displayArea);
        container.add(displayScroll, BorderLayout.CENTER);

        JPanel inputContainer = new JPanel(new BorderLayout());
        messageInput = new JTextField();
        JButton sendBtn = new JButton("Send");

        inputContainer.add(messageInput, BorderLayout.CENTER);
        inputContainer.add(sendBtn, BorderLayout.EAST);

        container.add(inputContainer, BorderLayout.SOUTH);

        sendBtn.addActionListener((ActionEvent e) -> {
            String message = messageInput.getText().trim();
            this.SendMsg(message);
        });

        messageInput.addActionListener((ActionEvent e) -> sendBtn.doClick());

        window.add(container);
        StartHttpTcp();
        this.controller = controller;
    }

    void Show() {
        window.setVisible(true);
    }
}