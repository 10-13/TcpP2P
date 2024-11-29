package KilimanJARo.P2P.server.monitors;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/*
    * This class is responsible for monitoring user activity and choosing a user to connect to based on recent activity.
 */
public class UserActivityMonitor {
    private static final long RECENT_TIME_THRESHOLD = 48 * 60 * 60 * 1000; // 48 hours
    private Map<String, Long> userActivityMap = new HashMap<>();
    private static UserActivityMonitor instance;

    private UserActivityMonitor(){}

    public static synchronized UserActivityMonitor getInstance() {
        if (instance == null) {
            instance = new UserActivityMonitor();
        }
        return instance;
    }

    public void updateUserActivity(String userName) {
        userActivityMap.put(userName, System.currentTimeMillis());
    }

    public String chooseUser() {
        // TODO: Implement a better algorithm to choose a user
        long currentTime = System.currentTimeMillis();
        Map<String, Long> recentUsers = new HashMap<>();

        for (Map.Entry<String, Long> entry : userActivityMap.entrySet()) {
            if (currentTime - entry.getValue() <= RECENT_TIME_THRESHOLD) {
                recentUsers.put(entry.getKey(), entry.getValue());
            }
        }

        if (recentUsers.isEmpty()) {
            return null;
        }

        int index = new Random().nextInt(recentUsers.size());
        return (String) recentUsers.keySet().toArray()[index];
    }

    public void addUser(String userName) {
        userActivityMap.put(userName, System.currentTimeMillis());
    }

    public void deleteUser(String userName) {
        userActivityMap.remove(userName);
    }

    public boolean isUserActive(String userName) {
        Long lastActivity = userActivityMap.get(userName);
        if (lastActivity == null) {
            return false;
        }
        return System.currentTimeMillis() - lastActivity <= RECENT_TIME_THRESHOLD;
    }

    public int getActiveUserCount() {
        // TODO: Implement a better algorithm to find active users
        long currentTime = System.currentTimeMillis();
        int count = 0;
        for (Long lastActivity : userActivityMap.values()) {
            if (currentTime - lastActivity <= RECENT_TIME_THRESHOLD) {
                count++;
            }
        }
        return count;
    }

    public void clear() {
        userActivityMap.clear();
    }

    public int size() {
        return userActivityMap.size();
    }

    public boolean isEmpty() {
        return userActivityMap.isEmpty();
    }

    public boolean containsUser(String userName) {
        return userActivityMap.containsKey(userName);
    }

    public long getLastActivityTime(String userName) {
        return userActivityMap.get(userName);
    }
}