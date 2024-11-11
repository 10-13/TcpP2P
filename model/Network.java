package model;

import java.util.List;

public interface Network {
    record NetData(String selfToken, List<String> connections) {}

    void broadcast(String msg);
    void setHandler(MessageHandler handler);

    NetData getNet();
}
