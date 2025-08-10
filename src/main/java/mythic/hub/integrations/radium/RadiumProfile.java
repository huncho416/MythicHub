package mythic.hub.integrations.radium;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a player profile from Radium backend
 * This matches the structure of Radium's Profile class
 */
public class RadiumProfile {
    
    private final UUID uuid;
    private final String username;
    private final List<String> ranks;
    private final Map<String, Boolean> permissions;
    private final long lastSeen;
    
    public RadiumProfile(UUID uuid, String username, List<String> ranks, Map<String, Boolean> permissions, long lastSeen) {
        this.uuid = uuid;
        this.username = username;
        this.ranks = ranks;
        this.permissions = permissions;
        this.lastSeen = lastSeen;
    }
    
    // Legacy constructor for backwards compatibility
    public RadiumProfile(UUID uuid, String primaryRank, List<String> ranks, Map<String, Boolean> permissions) {
        this.uuid = uuid;
        this.username = "Unknown";
        this.ranks = ranks;
        this.permissions = permissions;
        this.lastSeen = System.currentTimeMillis();
    }
    
    public UUID getUuid() {
        return uuid;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getPrimaryRank() {
        return getHighestRankName();
    }
    
    public List<String> getRanks() {
        return ranks;
    }
    
    public Map<String, Boolean> getPermissions() {
        return permissions;
    }
    
    public long getLastSeen() {
        return lastSeen;
    }
    
    public boolean hasRank(String rankName) {
        return ranks.contains(rankName);
    }
    
    public boolean hasPermission(String permission) {
        return permissions.getOrDefault(permission, false);
    }
    
    /**
     * Gets the highest priority rank name for this profile
     */
    public String getHighestRankName() {
        if (ranks.isEmpty()) {
            return "Member";
        }
        
        // For now, return the first rank. This will be enhanced when we have access to RadiumClient
        return ranks.get(0);
    }
    
    /**
     * Gets the highest priority rank for this profile using a RadiumClient
     */
    public RadiumRank getHighestRank(RadiumClient radiumClient) {
        if (ranks.isEmpty()) {
            return radiumClient.getRank("Member");
        }
        
        RadiumRank highestRank = null;
        int highestWeight = -1;
        
        for (String rankName : ranks) {
            RadiumRank rank = radiumClient.getRank(rankName);
            if (rank != null && rank.getWeight() > highestWeight) {
                highestWeight = rank.getWeight();
                highestRank = rank;
            }
        }
        
        return highestRank != null ? highestRank : radiumClient.getRank("Member");
    }
    
    @Override
    public String toString() {
        return "RadiumProfile{" +
                "uuid=" + uuid +
                ", username='" + username + '\'' +
                ", ranks=" + ranks +
                ", permissions=" + permissions.size() + " permissions" +
                ", lastSeen=" + lastSeen +
                '}';
    }
}
