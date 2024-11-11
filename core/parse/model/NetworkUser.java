package core.parse.model;

import java.util.stream.Stream;

public abstract class NetworkUser {
    public String Token;

    public abstract void send(byte[] data);
}
