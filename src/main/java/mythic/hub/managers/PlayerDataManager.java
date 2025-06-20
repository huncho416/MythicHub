package mythic.hub.managers;

import mythic.hub.config.DatabaseConfig;
import mythic.hub.data.PlayerProfile;
import mythic.hub.database.RedisManager;
import net.minestom.server.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class PlayerDataManager {
    private final RedisManager redisManager;
    private final ConcurrentHashMap<UUID, PlayerProfile> playerCache;

    public PlayerDataManager(DatabaseConfig config) {
        this.redisManager = new RedisManager(config);
        this.playerCache = new ConcurrentHashMap<>();
    }

    public CompletableFuture<PlayerProfile> loadPlayer(Player player) {
        return loadPlayer(player.getUuid(), player.getUsername());
    }

    public CompletableFuture<PlayerProfile> loadPlayer(UUID uuid, String username) {
        // Check cache first
        PlayerProfile cached = playerCache.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        // Load from Redis
        return redisManager.loadPlayerProfile(uuid, username)
                .thenApply(profile -> {
                    if (profile != null) {
                        playerCache.put(uuid, profile);
                        System.out.println("Loaded player profile for " + username + " with " +
                                profile.getActivePermissions().size() + " permissions and " +
                                profile.getActiveRanks().size() + " ranks");
                    } else {
                        // Create default profile if not found
                        profile = new PlayerProfile(uuid, username);
                        playerCache.put(uuid, profile);
                        System.out.println("Created new profile for " + username);
                    }
                    return profile;
                });
    }

    public PlayerProfile getPlayer(UUID uuid) {
        return playerCache.get(uuid);
    }

    public PlayerProfile getPlayer(Player player) {
        return getPlayer(player.getUuid());
    }

    public void unloadPlayer(UUID uuid) {
        PlayerProfile profile = playerCache.remove(uuid);
        if (profile != null) {
            // Save to Redis before unloading
            redisManager.savePlayerProfile(profile);
        }
    }

    public void unloadPlayer(Player player) {
        unloadPlayer(player.getUuid());
    }

    public boolean hasPermission(Player player, String permission) {
        PlayerProfile profile = getPlayer(player);
        return profile != null && profile.hasPermission(permission);
    }

    public boolean hasRank(Player player, String rank) {
        PlayerProfile profile = getPlayer(player);
        return profile != null && profile.hasRank(rank);
    }

    // Add this getter method for RedisManager
    public RedisManager getRedisManager() {
        return redisManager;
    }

    public void shutdown() {
        // Save all cached profiles
        playerCache.values().forEach(redisManager::savePlayerProfile);
        playerCache.clear();

        // Close Redis connection
        redisManager.close();
    }
}