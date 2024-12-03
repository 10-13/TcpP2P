package KilimanJARo.P2P.client.tunneling;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ReadableTunnel extends AbstractTunnel {
    public ReadableTunnel(String from, String to, String owner, String localId) {
        super(from, to, owner, localId);
    }

    public String readMessage() {
        if (clientSocket == null) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            return reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}