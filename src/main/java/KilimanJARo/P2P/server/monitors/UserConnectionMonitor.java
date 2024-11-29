// src/main/java/KilimanJARo/P2P/monitors/UserConnectionMonitor.java
package KilimanJARo.P2P.server.monitors;

import KilimanJARo.P2P.user.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * This class is responsible for monitoring the connection of the users.
 * It will help to determine the best third-part member of the tunnel to run the connection through.
 */
public class UserConnectionMonitor {
    private final Map<String, Instant> userConnections = new ConcurrentHashMap<>();
    private final ArrayList<String> onlineUsers = new ArrayList<>();
    private int userCount = 0;
    private int onlineUserCount = 0;
    private static UserConnectionMonitor instance;

    public UserConnectionMonitor() {}

    public static synchronized UserConnectionMonitor getInstance() {
        if (instance == null) {
            instance = new UserConnectionMonitor();
        }
        return instance;
    }

    public void userConnected(String username) {
        onlineUserCount++;
        userConnections.put(username, Instant.now());
        onlineUsers.add(username);
    }

    public void userDisconnected(String username) {
        onlineUserCount--;
        onlineUsers.remove(username);
    }

    public ArrayList<String> getOnlineUsers() {
        return onlineUsers;
    }

    public Instant getLastOnlineTime(String username) {
        return userConnections.get(username);
    }

    public Map<String, Instant> getAllUserConnections() {
        return userConnections;
    }

    public boolean isUserConnected(String username) {
        return userConnections.containsKey(username);
    }

    public boolean isUserActive(String username) {
        Instant lastOnlineTime = userConnections.get(username);
        if (lastOnlineTime == null) {
            return false;
        }
        return Instant.now().getEpochSecond() - lastOnlineTime.getEpochSecond() <= 48 * 60 * 60;
    }

    public void clearInactiveUsers() {
        userConnections.entrySet().removeIf(entry -> Instant.now().getEpochSecond() - entry.getValue().getEpochSecond() > 48 * 60 * 60);
    }

    public void clearAllUsers() {
        userConnections.clear();
    }

    public int getUserCount() {
        return userCount;
    }

    public int getOnlineUserCount() {
        return onlineUserCount;
    }
}