package KilimanJARo.P2P.server.monitors;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

public class UserConnectionMonitor {
    private record UserEntry(String name, Instant time) {}

    private final SortedSet<UserEntry> entrySet = new TreeSet<>(Comparator.comparing(a->a.time));
    private final HashMap<String, UserEntry> mappedEntry = new HashMap<>();
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
        mappedEntry.put(username, entry);
    }
    public synchronized void userDisconnected(String username) {
        entrySet.remove(mappedEntry.get(username));
        mappedEntry.remove(username);
    }
    public synchronized void clearUsers() {
        entrySet.clear();
        mappedEntry.clear();
    }

    public synchronized boolean isUserConnected(String username) {
        return mappedEntry.containsKey(username);
    }

    public int getUserCount() {
        return entrySet.size();
    }

    public Stream<String> getUsers() {
        return entrySet.stream().map(a->a.name);
    }
    public Stream<String> getRandomizedSet(int count) {
        class RandomizeNext {
            int randomized = 0;
            int position = -1;
            int pos = 0;

            public boolean filter(UserEntry userEntry) {
                if (pos++ == position) {
                    position += rand.nextInt(position + 1, getUserCount() - count + randomized++);
                    return true;
                }
                return false;
            }
        }

        final RandomizeNext rndGen = new RandomizeNext();
        return entrySet.stream().filter(rndGen::filter).map(a->a.name);
    }
}