package mythic.hub.integrations.radium;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mythic.hub.database.RedisManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Integration client for communicating with Radium backend system
 * Handles player profiles, ranks, permissions, and chat formatting
 */
public class RadiumClient {
    
    private final RedisManager redisManager;
    private final Gson gson;
    private final MiniMessage miniMessage;
    private final LegacyComponentSerializer legacySerializer;
    
    // Cache for player profiles and ranks
    private final ConcurrentHashMap<UUID, RadiumProfile> profileCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RadiumRank> rankCache = new ConcurrentHashMap<>();
    
    // Cache expiration times (5 minutes)
    private static final long CACHE_DURATION = 5 * 60 * 1000;
    private final ConcurrentHashMap<UUID, Long> profileCacheTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> rankCacheTime = new ConcurrentHashMap<>();
    
    // Profile update subscription
    private boolean subscriptionInitialized = false;
    
    public RadiumClient(RedisManager redisManager) {
        this.redisManager = redisManager;
        this.gson = new Gson();
        this.miniMessage = MiniMessage.miniMessage();
        this.legacySerializer = LegacyComponentSerializer.legacyAmpersand();
        
        // Initialize rank cache
        loadRanksFromRedis();
        
        // Initialize profile update listener
        initializeProfileUpdateListener();
    }
    
    /**
     * Get a player's profile from Radium
     */
    public CompletableFuture<RadiumProfile> getPlayerProfile(UUID playerUuid) {
        // Check cache first
        RadiumProfile cached = profileCache.get(playerUuid);
        Long cacheTime = profileCacheTime.get(playerUuid);
        
        if (cached != null && cacheTime != null && 
            (System.currentTimeMillis() - cacheTime) < CACHE_DURATION) {
            return CompletableFuture.completedFuture(cached);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String profileData = redisManager.get("radium:profile:" + playerUuid.toString());
                if (profileData != null) {
                    JsonObject json = JsonParser.parseString(profileData).getAsJsonObject();
                    RadiumProfile profile = parseProfile(json);
                    
                    // Cache the profile
                    profileCache.put(playerUuid, profile);
                    profileCacheTime.put(playerUuid, System.currentTimeMillis());
                    
                    return profile;
                }
                
                // Return default profile if not found
                return new RadiumProfile(playerUuid, "Unknown", List.of("Member"), Map.of(), System.currentTimeMillis());
                
            } catch (Exception e) {
                System.err.println("Error fetching player profile from Radium: " + e.getMessage());
                return new RadiumProfile(playerUuid, "Unknown", List.of("Member"), Map.of(), System.currentTimeMillis());
            }
        });
    }
    
    /**
     * Get a rank definition from Radium
     */
    public RadiumRank getRank(String rankName) {
        RadiumRank cached = rankCache.get(rankName.toLowerCase());
        Long cacheTime = rankCacheTime.get(rankName.toLowerCase());
        
        if (cached != null && cacheTime != null && 
            (System.currentTimeMillis() - cacheTime) < CACHE_DURATION) {
            return cached;
        }
        
        // Load from Redis
        try {
            String rankData = redisManager.get("radium:rank:" + rankName.toLowerCase());
            if (rankData != null) {
                JsonObject json = JsonParser.parseString(rankData).getAsJsonObject();
                RadiumRank rank = parseRank(json);
                
                // Cache the rank
                rankCache.put(rankName.toLowerCase(), rank);
                rankCacheTime.put(rankName.toLowerCase(), System.currentTimeMillis());
                
                return rank;
            }
        } catch (Exception e) {
            System.err.println("Error fetching rank from Radium: " + e.getMessage());
        }
        
        // Return default rank if not found
        return new RadiumRank("Member", "&a[Member] ", 10, "&f", new HashSet<>(), new ArrayList<>());
    }
    
    /**
     * Get the highest priority rank for a player
     */
    public CompletableFuture<RadiumRank> getPlayerHighestRank(UUID playerUuid) {
        return getPlayerProfile(playerUuid).thenApply(profile -> {
            if (profile.getRanks().isEmpty()) {
                return getRank("Member");
            }
            
            RadiumRank highestRank = null;
            int highestWeight = -1;
            
            for (String rankName : profile.getRanks()) {
                RadiumRank rank = getRank(rankName);
                if (rank != null && rank.getWeight() > highestWeight) {
                    highestWeight = rank.getWeight();
                    highestRank = rank;
                }
            }
            
            return highestRank != null ? highestRank : getRank("Member");
        });
    }
    
    /**
     * Format a chat message using Radium's chat formatting
     * This matches Radium's ChatManager format: chat.main_format with prefix, player, chatColor, message
     */
    public CompletableFuture<Component> formatChatMessage(UUID playerUuid, String playerName, String message) {
        return getPlayerProfile(playerUuid).thenApply(profile -> {
            try {
                RadiumRank rank = profile.getHighestRank(this);
                String prefix = rank.getPrefix();
                String chatColor = rank.getColor();
                
                System.out.println("[RadiumClient] Formatting chat for " + playerName + " with rank " + rank.getName() + 
                                   " (prefix: '" + prefix + "', color: '" + chatColor + "')");
                
                // Use Radium's exact chat format pattern: {prefix}{chatColor}{player}&f: {message}
                // This matches the format used in Radium's ChatManager
                Component prefixComponent = legacySerializer.deserialize(prefix);
                Component nameComponent = legacySerializer.deserialize(chatColor + playerName);
                Component messageComponent = legacySerializer.deserialize("&f: " + message);
                
                Component finalMessage = Component.empty()
                        .append(prefixComponent)
                        .append(nameComponent)
                        .append(messageComponent);
                
                System.out.println("[RadiumClient] Successfully formatted chat message for " + playerName);
                return finalMessage;
                
            } catch (Exception e) {
                System.err.println("[RadiumClient] Error formatting chat message for " + playerName + ": " + e.getMessage());
                // Return fallback formatting
                return Component.text("[Member] " + playerName + ": " + message);
            }
        }).exceptionally(throwable -> {
            System.err.println("[RadiumClient] Failed to get profile for " + playerName + ": " + throwable.getMessage());
            // Return fallback formatting
            return Component.text("[Member] " + playerName + ": " + message);
        });
    }
    
    /**
     * Get tab list display name for a player using Radium's format
     * This matches Radium's TabListManager format: tab.player_format with prefix, player, color
     */
    public CompletableFuture<Component> getTabListDisplayName(UUID playerUuid, String playerName) {
        return getPlayerHighestRank(playerUuid).thenApply(rank -> {
            String prefix = rank.getPrefix();
            String color = rank.getColor();
            
            // Use Radium's tab list format: {prefix}{color}{player}
            Component prefixComponent = legacySerializer.deserialize(prefix);
            Component nameComponent = legacySerializer.deserialize(color + playerName);
            
            return Component.empty()
                    .append(prefixComponent)
                    .append(nameComponent);
        });
    }
    
    /**
     * Check if a player has a specific permission
     */
    public CompletableFuture<Boolean> hasPermission(UUID playerUuid, String permission) {
        return getPlayerProfile(playerUuid).thenApply(profile -> {
            // Hardcoded check for owner "Expenses" - always has all permissions
            if (profile != null && "Expenses".equalsIgnoreCase(getPlayerNameFromProfile(profile))) {
                return true;
            }
            
            // Check direct permissions
            if (profile != null && profile.getPermissions().containsKey(permission) && 
                profile.getPermissions().get(permission)) {
                return true;
            }
            
            // Check rank permissions
            if (profile != null) {
                for (String rankName : profile.getRanks()) {
                    RadiumRank rank = getRank(rankName);
                    if (rank != null && rank.hasPermission(permission)) {
                        return true;
                    }
                }
            }
            
            return false;
        });
    }
    
    /**
     * Check if a player has a specific permission by username (more reliable for hardcoded users)
     */
    public CompletableFuture<Boolean> hasPermission(String playerName, String permission) {
        // Hardcoded check for owner "Expenses" - always has all permissions
        if ("Expenses".equalsIgnoreCase(playerName)) {
            return CompletableFuture.completedFuture(true);
        }
        
        // For other players, try to get their UUID and use the UUID-based method
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Search for player by username in Redis
                Set<String> profileKeys = redisManager.getKeys("radium:profile:*");
                for (String key : profileKeys) {
                    String profileJson = redisManager.get(key);
                    if (profileJson != null) {
                        JsonObject profile = JsonParser.parseString(profileJson).getAsJsonObject();
                        if (profile.has("username") && 
                            playerName.equalsIgnoreCase(profile.get("username").getAsString())) {
                            // Found the player, extract UUID and use UUID-based method
                            String uuidStr = profile.get("uuid").getAsString();
                            UUID playerUuid = UUID.fromString(uuidStr);
                            return hasPermission(playerUuid, permission).join();
                        }
                    }
                }
                return false; // Player not found
            } catch (Exception e) {
                System.err.println("Error checking permission by username: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Helper method to get player name from profile
     */
    private String getPlayerNameFromProfile(RadiumProfile profile) {
        try {
            // Try to get username from the profile's UUID by looking up in Redis
            String key = "radium:profile:" + profile.getUuid().toString();
            String profileJson = redisManager.get(key);
            if (profileJson != null) {
                JsonObject json = JsonParser.parseString(profileJson).getAsJsonObject();
                if (json.has("username")) {
                    return json.get("username").getAsString();
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error getting player name from profile: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Forward a command to Radium proxy for execution
     */
    public CompletableFuture<Boolean> forwardCommandToProxy(String playerName, String command) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String requestId = UUID.randomUUID().toString();
                
                JsonObject request = new JsonObject();
                request.addProperty("player", playerName);
                request.addProperty("command", command);
                request.addProperty("requestId", requestId);
                
                // Publish to Redis channel
                redisManager.publish("radium:command:execute", request.toString());
                
                System.out.println("Forwarded command to Radium: /" + command + " for player: " + playerName);
                return true;
                
            } catch (Exception e) {
                System.err.println("Error forwarding command to Radium: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Check if a command should be forwarded to Radium proxy
     */
    public static boolean shouldForwardCommand(String command) {
        String cmd = command.toLowerCase();
        return cmd.startsWith("/rank") || 
               cmd.startsWith("/grant") || 
               cmd.startsWith("/permission") || 
               cmd.startsWith("/perm") ||
               cmd.startsWith("/vanish") ||
               cmd.startsWith("/staffchat") ||
               cmd.startsWith("/gmc") ||
               cmd.startsWith("/gms") ||
               cmd.startsWith("/gamemode");
    }

    /**
     * Load ranks from Redis on startup
     */
    private void loadRanksFromRedis() {
        try {
            // Get all rank keys
            Set<String> rankKeys = redisManager.getKeys("radium:rank:*");
            
            for (String key : rankKeys) {
                String rankData = redisManager.get(key);
                if (rankData != null) {
                    JsonObject json = JsonParser.parseString(rankData).getAsJsonObject();
                    RadiumRank rank = parseRank(json);
                    String rankName = key.substring("radium:rank:".length());
                    
                    rankCache.put(rankName.toLowerCase(), rank);
                    rankCacheTime.put(rankName.toLowerCase(), System.currentTimeMillis());
                }
            }
            
            System.out.println("Loaded " + rankCache.size() + " ranks from Radium");
            
        } catch (Exception e) {
            System.err.println("Error loading ranks from Radium: " + e.getMessage());
        }
    }
    
    /**
     * Parse profile from JSON (updated to match Radium's Profile structure)
     */
    private RadiumProfile parseProfile(JsonObject json) {
        try {
            // Extract UUID - it could be stored as "uuid" or "_id"
            String uuidString = json.has("uuid") ? json.get("uuid").getAsString() : 
                               json.has("_id") ? json.get("_id").getAsString() : null;
            if (uuidString == null) {
                System.err.println("Profile JSON missing UUID field");
                return null;
            }
            UUID uuid = UUID.fromString(uuidString);
            
            // Extract username
            String username = json.has("username") ? json.get("username").getAsString() : "Unknown";
            
            // Extract lastSeen
            long lastSeen = json.has("lastSeen") ? json.get("lastSeen").getAsLong() : System.currentTimeMillis();
            
            // Extract ranks - Radium stores this as a Set<String> but in different format
            List<String> ranks = new ArrayList<>();
            if (json.has("ranks")) {
                var ranksElement = json.get("ranks");
                if (ranksElement.isJsonArray()) {
                    for (var rankElement : ranksElement.getAsJsonArray()) {
                        String rankString = rankElement.getAsString();
                        // Radium stores ranks as "rank|granter|timestamp|expiry", we just want the rank name
                        String rankName = rankString.split("\\|")[0];
                        if (!ranks.contains(rankName)) {
                            ranks.add(rankName);
                        }
                    }
                }
            }
            
            // If no ranks found, add default Member rank
            if (ranks.isEmpty()) {
                ranks.add("Member");
            }
            
            // Extract permissions
            Map<String, Boolean> permissions = new HashMap<>();
            if (json.has("permissions")) {
                var permsElement = json.get("permissions");
                if (permsElement.isJsonArray()) {
                    for (var permElement : permsElement.getAsJsonArray()) {
                        String permString = permElement.getAsString();
                        // Radium stores permissions as "permission|granter|timestamp|expiry", we just want the permission
                        String permission = permString.split("\\|")[0];
                        permissions.put(permission, true);
                    }
                }
            }
            
            return new RadiumProfile(uuid, username, ranks, permissions, lastSeen);
            
        } catch (Exception e) {
            System.err.println("Error parsing profile JSON: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Parse rank from JSON
     */
    private RadiumRank parseRank(JsonObject json) {
        String name = json.get("name").getAsString();
        String prefix = json.get("prefix").getAsString();
        int weight = json.get("weight").getAsInt();
        String color = json.has("color") ? json.get("color").getAsString() : "&f";
        
        Set<String> permissions = new HashSet<>();
        if (json.has("permissions") && json.get("permissions").isJsonArray()) {
            json.getAsJsonArray("permissions").forEach(element -> permissions.add(element.getAsString()));
        }
        
        List<String> inherits = new ArrayList<>();
        if (json.has("inherits") && json.get("inherits").isJsonArray()) {
            json.getAsJsonArray("inherits").forEach(element -> inherits.add(element.getAsString()));
        }
        
        return new RadiumRank(name, prefix, weight, color, permissions, inherits);
    }
    
    /**
     * Clear cache for a specific player (call when player leaves)
     */
    public void clearPlayerCache(UUID playerUuid) {
        profileCache.remove(playerUuid);
        profileCacheTime.remove(playerUuid);
    }
    
    /**
     * Refresh rank cache (call periodically)
     */
    public void refreshRankCache() {
        rankCache.clear();
        rankCacheTime.clear();
        loadRanksFromRedis();
    }
    
    // Friend System Methods
    
    /**
     * Gets a player's friends list from Radium
     * @param playerUuid The player's UUID
     * @return Set of friend UUIDs, empty set if none or on error
     */
    public CompletableFuture<Set<UUID>> getFriends(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = "radium:profile:" + playerUuid.toString();
                String profileJson = redisManager.get(key);
                if (profileJson != null) {
                    JsonObject profile = JsonParser.parseString(profileJson).getAsJsonObject();
                    if (profile.has("friends")) {
                        return profile.getAsJsonArray("friends").asList().stream()
                            .map(element -> UUID.fromString(element.getAsString()))
                            .collect(Collectors.toSet());
                    }
                }
                return Collections.emptySet();
            } catch (Exception e) {
                System.err.println("Failed to get friends for " + playerUuid + ": " + e.getMessage());
                return Collections.emptySet();
            }
        });
    }
    
    /**
     * Gets a player's incoming friend requests from Radium
     * @param playerUuid The player's UUID
     * @return Set of incoming request UUIDs, empty set if none or on error
     */
    public CompletableFuture<Set<UUID>> getIncomingFriendRequests(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = "radium:profile:" + playerUuid.toString();
                String profileJson = redisManager.get(key);
                if (profileJson != null) {
                    JsonObject profile = JsonParser.parseString(profileJson).getAsJsonObject();
                    if (profile.has("incomingRequests")) {
                        return profile.getAsJsonArray("incomingRequests").asList().stream()
                            .map(element -> UUID.fromString(element.getAsString()))
                            .collect(Collectors.toSet());
                    }
                }
                return Collections.emptySet();
            } catch (Exception e) {
                System.err.println("Failed to get incoming requests for " + playerUuid + ": " + e.getMessage());
                return Collections.emptySet();
            }
        });
    }
    
    /**
     * Gets a player's outgoing friend requests from Radium
     * @param playerUuid The player's UUID
     * @return Set of outgoing request UUIDs, empty set if none or on error
     */
    public CompletableFuture<Set<UUID>> getOutgoingFriendRequests(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = "radium:profile:" + playerUuid.toString();
                String profileJson = redisManager.get(key);
                if (profileJson != null) {
                    JsonObject profile = JsonParser.parseString(profileJson).getAsJsonObject();
                    if (profile.has("outgoingRequests")) {
                        return profile.getAsJsonArray("outgoingRequests").asList().stream()
                            .map(element -> UUID.fromString(element.getAsString()))
                            .collect(Collectors.toSet());
                    }
                }
                return Collections.emptySet();
            } catch (Exception e) {
                System.err.println("Failed to get outgoing requests for " + playerUuid + ": " + e.getMessage());
                return Collections.emptySet();
            }
        });
    }
    
    /**
     * Checks if two players are friends in Radium
     * @param playerUuid The first player's UUID
     * @param friendUuid The second player's UUID
     * @return true if they are friends, false otherwise
     */
    public CompletableFuture<Boolean> areFriends(UUID playerUuid, UUID friendUuid) {
        return getFriends(playerUuid).thenApply(friends -> friends.contains(friendUuid));
    }
    
    /**
     * Gets a friend's last seen timestamp from Radium
     * @param playerUuid The player's UUID
     * @param friendUuid The friend's UUID
     * @return Last seen timestamp, or null if not available
     */
    public CompletableFuture<Instant> getFriendLastSeen(UUID playerUuid, UUID friendUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = "friend_lastseen:" + playerUuid.toString() + ":" + friendUuid.toString();
                String timestamp = redisManager.get(key);
                if (timestamp != null) {
                    return Instant.ofEpochMilli(Long.parseLong(timestamp));
                }
                return null;
            } catch (Exception e) {
                System.err.println("Failed to get friend last seen: " + e.getMessage());
                return null;
            }
        });
    }
    
    /**
     * Sends a friend request through Radium's command system
     * @param playerName The sender's name
     * @param targetName The target player's name
     */
    public CompletableFuture<Boolean> sendFriendRequest(String playerName, String targetName) {
        String command = "friend add " + targetName;
        return forwardCommandToProxy(playerName, command);
    }
    
    /**
     * Removes a friend through Radium's command system
     * @param playerName The sender's name
     * @param targetName The target player's name
     */
    public CompletableFuture<Boolean> removeFriend(String playerName, String targetName) {
        String command = "friend remove " + targetName;
        return forwardCommandToProxy(playerName, command);
    }
    
    /**
     * Denies a friend request through Radium's command system
     * @param playerName The sender's name
     * @param targetName The target player's name
     */
    public CompletableFuture<Boolean> denyFriendRequest(String playerName, String targetName) {
        String command = "friend deny " + targetName;
        return forwardCommandToProxy(playerName, command);
    }
    
    /**
     * Gets the profile name for a given UUID from Radium
     * @param playerUuid The player's UUID
     * @return The player's username, or null if not found
     */
    public CompletableFuture<String> getPlayerName(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = "radium:profile:" + playerUuid.toString();
                String profileJson = redisManager.get(key);
                if (profileJson != null) {
                    JsonObject profile = JsonParser.parseString(profileJson).getAsJsonObject();
                    if (profile.has("username")) {
                        return profile.get("username").getAsString();
                    }
                }
                return null;
            } catch (Exception e) {
                System.err.println("Failed to get player name for " + playerUuid + ": " + e.getMessage());
                return null;
            }
        });
    }
    
    /**
     * Initializes Redis subscription for profile updates
     */
    private void initializeProfileUpdateListener() {
        if (subscriptionInitialized) {
            return;
        }
        
        try {
            // Subscribe to profile updates
            redisManager.subscribe("radium:profile:updated", (channel, message) -> {
                if ("radium:profile:updated".equals(channel)) {
                    String playerUuid = message;
                    System.out.println("[RadiumClient] Received profile update notification for: " + playerUuid);
                    clearPlayerCacheByUuid(playerUuid);
                }
            });
            
            // Subscribe to rank updates
            redisManager.subscribe("radium:rank:updated", (channel, message) -> {
                if ("radium:rank:updated".equals(channel)) {
                    String rankName = message;
                    System.out.println("[RadiumClient] Received rank update notification for: " + rankName);
                    clearRankCache(rankName);
                    clearAllPlayerCacheForRankUpdate();
                }
            });
            
            subscriptionInitialized = true;
            System.out.println("[RadiumClient] Profile and rank update listeners initialized successfully");
        } catch (Exception e) {
            System.err.println("[RadiumClient] Error initializing update subscriptions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Clear cached profile by UUID
     */
    public void clearPlayerCacheByUuid(String playerUuid) {
        try {
            UUID uuid = UUID.fromString(playerUuid);
            RadiumProfile removed = profileCache.remove(uuid);
            profileCacheTime.remove(uuid);
            
            if (removed != null) {
                System.out.println("[RadiumClient] Cleared cache for player UUID: " + playerUuid);
            } else {
                System.out.println("[RadiumClient] No cached profile found for UUID: " + playerUuid);
            }
        } catch (Exception e) {
            System.err.println("[RadiumClient] Error clearing cache for UUID " + playerUuid + ": " + e.getMessage());
        }
    }

    /**
     * Clear cached rank by name
     */
    public void clearRankCache(String rankName) {
        try {
            RadiumRank removed = rankCache.remove(rankName.toLowerCase());
            rankCacheTime.remove(rankName.toLowerCase());
            
            if (removed != null) {
                System.out.println("[RadiumClient] Cleared rank cache for: " + rankName);
            }
        } catch (Exception e) {
            System.err.println("[RadiumClient] Error clearing rank cache for " + rankName + ": " + e.getMessage());
        }
    }

    /**
     * Clear all player profiles when a rank is updated (since player display might change)
     */
    public void clearAllPlayerCacheForRankUpdate() {
        try {
            int profileCount = profileCache.size();
            profileCache.clear();
            profileCacheTime.clear();
            System.out.println("[RadiumClient] Cleared " + profileCount + " player profiles due to rank update");
        } catch (Exception e) {
            System.err.println("[RadiumClient] Error clearing all player cache: " + e.getMessage());
        }
    }

    /**
     * Clear all cached profiles
     */
    public void clearAllCache() {
        profileCache.clear();
        profileCacheTime.clear();
        rankCache.clear();
        rankCacheTime.clear();
        System.out.println("[RadiumClient] Cleared all profile and rank cache");
    }

    /**
     * Clear cached profile by username (for debugging)
     */
    public void clearPlayerCache(String username) {
        // Find profile by username and remove it
        profileCache.entrySet().removeIf(entry -> {
            // Since we don't have direct username access in RadiumProfile,
            // we'll remove all cache and let it refresh
            return true;
        });
        profileCacheTime.clear();
        System.out.println("[RadiumClient] Cache clear requested for player: " + username + " (cleared all cache)");
    }

    /**
     * Debug method to check current cache status
     */
    public void debugCacheStatus() {
        System.out.println("[RadiumClient] Cache Status:");
        System.out.println("  - Profiles cached: " + profileCache.size());
        System.out.println("  - Ranks cached: " + rankCache.size());
        System.out.println("  - Subscription initialized: " + subscriptionInitialized);
    }
    
    /**
     * Force refresh a player's profile from Redis
     */
    public CompletableFuture<RadiumProfile> forceRefreshProfile(UUID playerUuid) {
        // Clear cache first
        profileCache.remove(playerUuid);
        profileCacheTime.remove(playerUuid);
        
        // Fetch fresh profile
        return getPlayerProfile(playerUuid);
    }

    /**
     * Shutdown the profile update listener and cleanup resources
     */
    public void shutdown() {
        try {
            if (subscriptionInitialized) {
                redisManager.unsubscribe("radium:profile:updated");
                redisManager.unsubscribe("radium:rank:updated");
            }
            clearAllCache();
            System.out.println("[RadiumClient] Shutdown completed");
        } catch (Exception e) {
            System.err.println("[RadiumClient] Error during shutdown: " + e.getMessage());
        }
    }
}
