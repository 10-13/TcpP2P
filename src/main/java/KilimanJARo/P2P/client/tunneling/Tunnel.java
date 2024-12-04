package KilimanJARo.P2P.client.tunneling;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Tunnel {
    private String id;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ExecutorService executorService;
    private static final int TIMEOUT = 10000;
    private static final int toRetryCount = 3;

    private Tunnel(String id) {
        this.executorService = Executors.newCachedThreadPool();
        this.id = id;
    }
    /**
     * Use some timeout to return null if it failed to connect in some short period of time
     * @param from Contains IP and PORT where u need to connect via TCP
     * @param to Find free port and run async thread to wait for connection on them.
     *           Check for connected user has IP same as 'to'.
     *           After connected, need to redirect data on the same thread until Close is called or connection closed.
     */
    public static Tunnel Create(String from, String to, String localId) {
        Tunnel tunnel = new Tunnel(localId);
        if (!tunnel.initialize(tunnel, from, to)) {
            return null;
        }
        return tunnel;
    }

    /**
     * Initializes the tunnel by either starting a server socket to listen for incoming connections
     * or connecting to a specified address.
     *
     * @return true if initialization is successful, otherwise false.
     */
    private boolean initialize(Tunnel tunnel, String from, String to) {
        try {
            if (from != null) {
                String[] fromParts = from.split(":");
                tunnel.clientSocket = new Socket(fromParts[0], Integer.parseInt(fromParts[1]));
                tunnel.clientSocket.setSoTimeout(TIMEOUT);
            }
            if (to != null) {
                tunnel.serverSocket = new ServerSocket(0); // If port is 0 then it will find free port
                tunnel.serverSocket.setSoTimeout(TIMEOUT);

                tunnel.executorService.submit(() -> tunnel.waitForConnection(to));
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Waits for an incoming connection on the server socket. Once a connection is established,
     * it checks if the incoming connection's IP matches the expected IP. If it matches, it starts
     * forwarding data between the sockets.
     */
    private void waitForConnection(String to) {
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

    /**
     * Forwards data between the input and output sockets. Reads data from the input socket
     * and writes it to the output socket.
     *
     * @param inputSocket The socket to read data from.
     * @param outputSocket The socket to write data to.
     */
    private void forwardData(Socket inputSocket, Socket outputSocket) {
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

    /**
     * Close all connections
     */
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
            throw new RuntimeException("Failed to close tunnel.", e);
        }
    }

    /**
     * @return Returns port for connection to "from"
     */
    public int PortToFrom() {
        return clientSocket != null ? clientSocket.getLocalPort() : -1;
    }

    /**
     * @return Returns port for connection to "to"
     */
    public int PortToTo() {
        return serverSocket != null ? serverSocket.getLocalPort() : -1;
}

    public String getId() {
        return id;
    }
}