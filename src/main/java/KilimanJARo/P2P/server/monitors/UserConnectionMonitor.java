// src/main/java/KilimanJARo/P2P/monitors/UserConnectionMonitor.java
package KilimanJARo.P2P.server.monitors;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/*
 * This class is responsible for monitoring the connection of the users.
 * It will help to determine the best third-part member of the tunnel to run the connection through.
 */
public class UserConnectionMonitor {
    private record UserEntry(String name, Instant time) {}

    private final SortedSet<UserEntry> entrySet = new TreeSet<>(Comparator.comparing(a->a.time));
    private final HashMap<String, UserEntry> mapedEntry = new HashMap<>();

    private static UserConnectionMonitor instance;

    public UserConnectionMonitor() {}

    public static synchronized UserConnectionMonitor getInstance() {
        if (instance == null) {
            instance = new UserConnectionMonitor();
        }
        return instance;
    }

    public synchronized void userConnected(String username) {
        var entry = new UserEntry(username, Instant.now());
        entrySet.add(entry);
        mapedEntry.put(username, entry);
    }
    public synchronized void userDisconnected(String username) {
        entrySet.remove(mapedEntry.get(username));
        mapedEntry.remove(username);
    }
    public synchronized void clearAllUsers() {
        entrySet.clear();
        mapedEntry.clear();
    }

    public Stream<String> getOnlineUsers() {
        return entrySet.stream().map(a->a.name);
    }
    public Stream<String> getRandomizedSet(int count) {
        // TODO: Implement receiving set of random users.
        throw new RuntimeException("Not implemented.");
    }

    public int getUserCount() {
        return entrySet.size();
    }
}