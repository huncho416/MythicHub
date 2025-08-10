package mythic.hub.database;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.RedisPubSubListener;
import mythic.hub.config.DatabaseConfig;
import mythic.hub.data.PlayerProfile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class RedisManager {
    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> syncCommands;
    private final StatefulRedisPubSubConnection<String, String> pubSubConnection;

    public RedisManager(DatabaseConfig config) {
        RedisURI redisUri = RedisURI.Builder
                .redis(config.getRedisHost(), config.getRedisPort())
                .withAuthentication(config.getRedisUsername(), config.getRedisPassword())
                .build();

        this.redisClient = RedisClient.create(redisUri);
        this.connection = redisClient.connect();
        this.syncCommands = connection.sync();
        this.pubSubConnection = redisClient.connectPubSub();
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
    
    /**
     * Subscribe to a Redis channel with a message handler
     * @param channel The channel to subscribe to
     * @param messageHandler Handler for received messages (channel, message)
     */
    public void subscribe(String channel, BiConsumer<String, String> messageHandler) {
        pubSubConnection.addListener(new RedisPubSubListener<String, String>() {
            @Override
            public void message(String channel, String message) {
                messageHandler.accept(channel, message);
            }

            @Override
            public void message(String pattern, String channel, String message) {
                // Pattern messages handled here if needed
            }

            @Override
            public void subscribed(String channel, long count) {
                System.out.println("[RedisManager] Subscribed to channel: " + channel);
            }

            @Override
            public void psubscribed(String pattern, long count) {
                // Pattern subscription
            }

            @Override
            public void unsubscribed(String channel, long count) {
                System.out.println("[RedisManager] Unsubscribed from channel: " + channel);
            }

            @Override
            public void punsubscribed(String pattern, long count) {
                // Pattern unsubscription
            }
        });
        
        pubSubConnection.async().subscribe(channel);
    }
    
    /**
     * Unsubscribe from a Redis channel
     * @param channel The channel to unsubscribe from
     */
    public void unsubscribe(String channel) {
        pubSubConnection.async().unsubscribe(channel);
    }
    
    // Get keys matching a pattern
    public Set<String> getKeys(String pattern) {
        List<String> keysList = syncCommands.keys(pattern);
        return Set.copyOf(keysList);
    }
    
    /**
     * Shutdown and close all Redis connections
     */
    public void shutdown() {
        try {
            if (pubSubConnection != null) {
                pubSubConnection.close();
            }
            if (connection != null) {
                connection.close();
            }
            if (redisClient != null) {
                redisClient.shutdown();
            }
        } catch (Exception e) {
            System.err.println("Error shutting down RedisManager: " + e.getMessage());
        }
    }
}