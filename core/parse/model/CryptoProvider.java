package core.parse.model;

import java.util.stream.Stream;

public interface CryptoProvider {
    Stream<Byte> Encrypt(Stream<Byte> data, ConnectionData connection);
    Stream<Byte> Decrypt(Stream<Byte> data, ConnectionData connection);
}
