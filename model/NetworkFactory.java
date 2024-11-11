package model;

public interface NetworkFactory {
    Network connect(String connection);
    Network connect(Network.NetData net);
    Network initNew();
}
