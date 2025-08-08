package mythic.hub.integrations.radium;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a player profile from Radium backend
 */
public class RadiumProfile {
    
    private final UUID uuid;
    private final String primaryRank;
    private final List<String> ranks;
    private final Map<String, Boolean> permissions;
    
    public RadiumProfile(UUID uuid, String primaryRank, List<String> ranks, Map<String, Boolean> permissions) {
        this.uuid = uuid;
        this.primaryRank = primaryRank;
        this.ranks = ranks;
        this.permissions = permissions;
    }
    
    public UUID getUuid() {
        return uuid;
    }
    
    public String getPrimaryRank() {
        return primaryRank;
    }
    
    public List<String> getRanks() {
        return ranks;
    }
    
    public Map<String, Boolean> getPermissions() {
        return permissions;
    }
    
    public boolean hasRank(String rankName) {
        return ranks.contains(rankName);
    }
    
    public boolean hasPermission(String permission) {
        return permissions.getOrDefault(permission, false);
    }
    
    @Override
    public String toString() {
        return "RadiumProfile{" +
                "uuid=" + uuid +
                ", primaryRank='" + primaryRank + '\'' +
                ", ranks=" + ranks +
                ", permissions=" + permissions.size() + " permissions" +
                '}';
    }
}
