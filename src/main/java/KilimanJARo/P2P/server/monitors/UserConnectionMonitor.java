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
    private final Random rand = new Random();

    private static UserConnectionMonitor instance;
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
        class RandomizeNext {
            int randomized = 0;
            int position = -1;
            int pos = 0;

            public void next() {
                position += rand.nextInt(position + 1, getUserCount() - count + randomized++);
            }

            public boolean filter() {
                if (pos++ == position) {
                    next();
                    return true;
                }
                return false;
            }

            public boolean filter(UserEntry userEntry) {
                return filter();
            }
        }
        final RandomizeNext rnd_gen = new RandomizeNext();
        return entrySet.stream().filter(rnd_gen::filter).map(a->a.name);
    }

    public int getUserCount() {
        return entrySet.size();
    }
}