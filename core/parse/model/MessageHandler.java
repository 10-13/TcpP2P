package core.parse.model;

import java.util.stream.Stream;

public interface MessageHandler {
    void handle(Stream<Byte> data, ConnectionData connection);
}
