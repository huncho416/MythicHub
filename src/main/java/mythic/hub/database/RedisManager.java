package mythic.hub.database;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import mythic.hub.config.DatabaseConfig;
import mythic.hub.data.PlayerProfile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RedisManager {
    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> syncCommands;

    public RedisManager(DatabaseConfig config) {
        RedisURI redisUri = RedisURI.Builder
                .redis(config.getRedisHost(), config.getRedisPort())
                .withAuthentication(config.getRedisUsername(), config.getRedisPassword())
                .build();

        this.redisClient = RedisClient.create(redisUri);
        this.connection = redisClient.connect();
        this.syncCommands = connection.sync();
    }

    public CompletableFuture<PlayerProfile> loadPlayerProfile(UUID uuid, String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String playerKey = "player:" + uuid.toString();
                Map<String, String> playerData = syncCommands.hgetall(playerKey);

                if (playerData.isEmpty()) {
                    return null; // Player not found in Redis
                }

                PlayerProfile profile = new PlayerProfile(uuid, username);

                // Load additional data
                for (Map.Entry<String, String> entry : playerData.entrySet()) {
                    if (!entry.getKey().equals("username") && !entry.getKey().equals("lastSeen")) {
                        profile.getAdditionalData().put(entry.getKey(), entry.getValue());
                    }
                }

                return profile;
            } catch (Exception e) {
                System.err.println("Error loading player profile for " + uuid + ": " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }

    public CompletableFuture<Void> savePlayerProfile(PlayerProfile profile) {
        return CompletableFuture.runAsync(() -> {
            try {
                String playerKey = "player:" + profile.getUuid().toString();

                // Save basic player data
                syncCommands.hset(playerKey, "username", profile.getUsername());
                syncCommands.hset(playerKey, "lastSeen", String.valueOf(System.currentTimeMillis()));

                // Save additional data
                for (Map.Entry<String, Object> entry : profile.getAdditionalData().entrySet()) {
                    syncCommands.hset(playerKey, entry.getKey(), entry.getValue().toString());
                }

                // Note: Permissions and ranks are typically managed by the main network system
                // This is mainly for caching and reading purposes

            } catch (Exception e) {
                System.err.println("Error saving player profile for " + profile.getUuid() + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void close() {
        if (connection != null) {
            connection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
    }

    // Utility methods for direct Redis operations
    public String get(String key) {
        return syncCommands.get(key);
    }

    public void set(String key, String value) {
        syncCommands.set(key, value);
    }

    public Map<String, String> hgetall(String key) {
        return syncCommands.hgetall(key);
    }

    // Additional methods for server management
    public void hset(String key, String field, String value) {
        syncCommands.hset(key, field, value);
    }

    public String hget(String key, String field) {
        return syncCommands.hget(key, field);
    }

    public void sadd(String key, String... members) {
        syncCommands.sadd(key, members);
    }

    public void srem(String key, String... members) {
        syncCommands.srem(key, members);
    }

    public Set<String> smembers(String key) {
        return syncCommands.smembers(key);
    }

    public void del(String... keys) {
        syncCommands.del(keys);
    }

    public void expire(String key, long seconds) {
        syncCommands.expire(key, seconds);
    }

    // Pub/Sub methods for proxy communication
    public void publish(String channel, String message) {
        syncCommands.publish(channel, message);
    }
    
    // Get keys matching a pattern
    public Set<String> getKeys(String pattern) {
        List<String> keysList = syncCommands.keys(pattern);
        return Set.copyOf(keysList);
    }
}