package KilimanJARo.P2P.client.tunneling;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractTunnel implements TunnelInterface {
    protected String localId;
    protected String from;
    protected String to;
    protected String owner;
    protected ServerSocket serverSocket;
    protected Socket clientSocket;
    protected ExecutorService executorService;
    protected static final int TIMEOUT = 10000;
    protected static final int toRetryCount = 3;

    public AbstractTunnel(String from, String to, String owner, String localId) {
        this.from = from;
        this.to = to;
        this.owner = owner;
        this.localId = localId;
        this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    public boolean initialize() {
        try {
            String[] fromParts = from.split(":");
            clientSocket = new Socket(fromParts[0], Integer.parseInt(fromParts[1]));
            clientSocket.setSoTimeout(TIMEOUT);


            serverSocket = new ServerSocket(0);
            serverSocket.setSoTimeout(TIMEOUT);
            executorService.submit(this::waitForConnection);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    protected void waitForConnection() {
        for (int i = 0; i < toRetryCount; i++) {
            try {
                Socket incomingSocket = serverSocket.accept();
                if (incomingSocket.getInetAddress().getHostAddress().equals(to.split(":")[0])) {
                    clientSocket = incomingSocket;
                    executorService.submit(() -> forwardData(clientSocket, incomingSocket));
                    executorService.submit(() -> forwardData(incomingSocket, clientSocket));
                    return;
                } else {
                    incomingSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.err.println("Failed to establish connection after multiple attempts.");
    }

    protected void forwardData(Socket inputSocket, Socket outputSocket) {
        try (var inputStream = inputSocket.getInputStream();
             var outputStream = outputSocket.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            if (clientSocket != null) {
                clientSocket.close();
            }
            if (serverSocket != null) {
                serverSocket.close();
            }
            executorService.shutdownNow();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getPort() {
        return serverSocket != null ? serverSocket.getLocalPort() : -1;
    }

    @Override
    public boolean confirmOwner(String username) {
        return owner.equals(username);
    }

    @Override
    public String getLocalId() {
        return localId;
    }
}