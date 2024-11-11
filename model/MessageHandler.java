package model;

public interface MessageHandler {
    void handle(String msg, ActionType act, String token);
}
