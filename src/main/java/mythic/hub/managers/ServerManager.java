
package mythic.hub.managers;

import mythic.hub.database.RedisManager;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerManager {
    private final RedisManager redisManager;
    private final String serverName;
    private final ScheduledExecutorService heartbeatExecutor;
    private static final String SERVER_KEY_PREFIX = "server:";
    private static final String SERVERS_SET_KEY = "servers:online";
    private static final int HEARTBEAT_INTERVAL = 30; // seconds
    private static final int SERVER_TIMEOUT = 90; // seconds

    public ServerManager(RedisManager redisManager, String serverName) {
        this.redisManager = redisManager;
        this.serverName = serverName;
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();

        // Register this server and start heartbeat
        registerServer();
        startHeartbeat();
    }

    /**
     * Register this server in Redis
     */
    private void registerServer() {
        try {
            String serverKey = SERVER_KEY_PREFIX + serverName;
            long currentTime = System.currentTimeMillis();

            // Add server to the online servers set
            redisManager.sadd(SERVERS_SET_KEY, serverName);

            // Set server info with heartbeat timestamp
            redisManager.hset(serverKey, "name", serverName);
            redisManager.hset(serverKey, "lastHeartbeat", String.valueOf(currentTime));
            redisManager.hset(serverKey, "startTime", String.valueOf(currentTime));
            redisManager.hset(serverKey, "type", getServerType());

            // Set expiration for the server key
            redisManager.expire(serverKey, SERVER_TIMEOUT);

            System.out.println("Server '" + serverName + "' registered in Redis network");
        } catch (Exception e) {
            System.err.println("Failed to register server: " + e.getMessage());
        }
    }

    /**
     * Start the heartbeat mechanism to keep server alive in Redis
     */
    private void startHeartbeat() {
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                String serverKey = SERVER_KEY_PREFIX + serverName;
                long currentTime = System.currentTimeMillis();

                // Update heartbeat timestamp
                redisManager.hset(serverKey, "lastHeartbeat", String.valueOf(currentTime));

                // Refresh expiration
                redisManager.expire(serverKey, SERVER_TIMEOUT);

                // Clean up expired servers
                cleanupExpiredServers();

            } catch (Exception e) {
                System.err.println("Heartbeat failed: " + e.getMessage());
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Clean up servers that haven't sent heartbeat recently
     */
    private void cleanupExpiredServers() {
        try {
            Set<String> onlineServers = redisManager.smembers(SERVERS_SET_KEY);
            long currentTime = System.currentTimeMillis();

            for (String server : onlineServers) {
                String serverKey = SERVER_KEY_PREFIX + server;
                String lastHeartbeatStr = redisManager.hget(serverKey, "lastHeartbeat");

                if (lastHeartbeatStr != null) {
                    long lastHeartbeat = Long.parseLong(lastHeartbeatStr);
                    if (currentTime - lastHeartbeat > (SERVER_TIMEOUT * 1000)) {
                        // Server is expired, remove it
                        redisManager.srem(SERVERS_SET_KEY, server);
                        redisManager.del(serverKey);
                        System.out.println("Removed expired server: " + server);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to cleanup expired servers: " + e.getMessage());
        }
    }

    /**
     * Get all currently online servers
     */
    public Set<String> getOnlineServers() {
        try {
            return redisManager.smembers(SERVERS_SET_KEY);
        } catch (Exception e) {
            System.err.println("Failed to get online servers: " + e.getMessage());
            return Set.of();
        }
    }

    /**
     * Get server information
     */
    public ServerInfo getServerInfo(String serverName) {
        try {
            String serverKey = SERVER_KEY_PREFIX + serverName;
            String name = redisManager.hget(serverKey, "name");
            String lastHeartbeatStr = redisManager.hget(serverKey, "lastHeartbeat");
            String startTimeStr = redisManager.hget(serverKey, "startTime");
            String type = redisManager.hget(serverKey, "type");

            if (name != null && lastHeartbeatStr != null) {
                return new ServerInfo(
                        name,
                        Long.parseLong(lastHeartbeatStr),
                        startTimeStr != null ? Long.parseLong(startTimeStr) : 0,
                        type != null ? type : "Unknown"
                );
            }
        } catch (Exception e) {
            System.err.println("Failed to get server info for " + serverName + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Get the current server name
     */
    public String getCurrentServerName() {
        return serverName;
    }

    /**
     * Get the server type based on server name
     */
    private String getServerType() {
        String lowerName = serverName.toLowerCase();
        if (lowerName.contains("hub") || lowerName.contains("lobby")) {
            return "Hub";
        } else if (lowerName.contains("kitpvp")) {
            return "KitPvP";
        } else if (lowerName.contains("survival")) {
            return "Survival";
        } else if (lowerName.contains("creative")) {
            return "Creative";
        } else if (lowerName.contains("skyblock")) {
            return "Skyblock";
        }
        return "Game";
    }

    /**
     * Shutdown the server manager
     */
    public void shutdown() {
        try {
            // Remove server from online list
            redisManager.srem(SERVERS_SET_KEY, serverName);
            redisManager.del(SERVER_KEY_PREFIX + serverName);

            // Shutdown heartbeat executor
            heartbeatExecutor.shutdown();

            System.out.println("Server '" + serverName + "' unregistered from Redis network");
        } catch (Exception e) {
            System.err.println("Failed to unregister server: " + e.getMessage());
        }
    }

    /**
     * Server information class
     */
    public static class ServerInfo {
        private final String name;
        private final long lastHeartbeat;
        private final long startTime;
        private final String type;

        public ServerInfo(String name, long lastHeartbeat, long startTime, String type) {
            this.name = name;
            this.lastHeartbeat = lastHeartbeat;
            this.startTime = startTime;
            this.type = type;
        }

        public String getName() { return name; }
        public long getLastHeartbeat() { return lastHeartbeat; }
        public long getStartTime() { return startTime; }
        public String getType() { return type; }

        public boolean isOnline() {
            return System.currentTimeMillis() - lastHeartbeat < 90000; // 90 seconds
        }
    }
}