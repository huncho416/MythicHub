package mythic.hub.integrations.radium;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mythic.hub.database.RedisManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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
    
    public RadiumClient(RedisManager redisManager) {
        this.redisManager = redisManager;
        this.gson = new Gson();
        this.miniMessage = MiniMessage.miniMessage();
        this.legacySerializer = LegacyComponentSerializer.legacyAmpersand();
        
        // Initialize rank cache
        loadRanksFromRedis();
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
                return new RadiumProfile(playerUuid, "Member", new ArrayList<>(), new HashMap<>());
                
            } catch (Exception e) {
                System.err.println("Error fetching player profile from Radium: " + e.getMessage());
                return new RadiumProfile(playerUuid, "Member", new ArrayList<>(), new HashMap<>());
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
     */
    public CompletableFuture<Component> formatChatMessage(UUID playerUuid, String playerName, String message) {
        return getPlayerHighestRank(playerUuid).thenApply(rank -> {
            String prefix = rank.getPrefix();
            String nameColor = rank.getColor();
            
            // Create the formatted message using Radium's format
            // Format: [Prefix] PlayerName: Message
            Component prefixComponent = legacySerializer.deserialize(prefix);
            Component nameComponent = legacySerializer.deserialize(nameColor + playerName);
            Component messageComponent = Component.text(": " + message);
            
            return Component.empty()
                    .append(prefixComponent)
                    .append(nameComponent)
                    .append(messageComponent);
        });
    }
    
    /**
     * Get tab list display name for a player
     */
    public CompletableFuture<Component> getTabListDisplayName(UUID playerUuid, String playerName) {
        return getPlayerHighestRank(playerUuid).thenApply(rank -> {
            String prefix = rank.getPrefix();
            String nameColor = rank.getColor();
            
            // Format: [Prefix] PlayerName
            Component prefixComponent = legacySerializer.deserialize(prefix);
            Component nameComponent = legacySerializer.deserialize(nameColor + playerName);
            
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
            // Check direct permissions
            if (profile.getPermissions().containsKey(permission) && 
                profile.getPermissions().get(permission)) {
                return true;
            }
            
            // Check rank permissions
            for (String rankName : profile.getRanks()) {
                RadiumRank rank = getRank(rankName);
                if (rank != null && rank.hasPermission(permission)) {
                    return true;
                }
            }
            
            return false;
        });
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
     * Parse profile from JSON
     */
    private RadiumProfile parseProfile(JsonObject json) {
        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        String primaryRank = json.has("primaryRank") ? json.get("primaryRank").getAsString() : "Member";
        
        List<String> ranks = new ArrayList<>();
        if (json.has("ranks") && json.get("ranks").isJsonArray()) {
            json.getAsJsonArray("ranks").forEach(element -> ranks.add(element.getAsString()));
        }
        if (ranks.isEmpty()) {
            ranks.add(primaryRank);
        }
        
        Map<String, Boolean> permissions = new HashMap<>();
        if (json.has("permissions") && json.get("permissions").isJsonObject()) {
            JsonObject perms = json.getAsJsonObject("permissions");
            perms.entrySet().forEach(entry -> 
                permissions.put(entry.getKey(), entry.getValue().getAsBoolean()));
        }
        
        return new RadiumProfile(uuid, primaryRank, ranks, permissions);
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
}
