package mythic.hub.data;

import mythic.hub.config.RankConfig;

import java.util.*;

public class PlayerProfile {
    private final UUID uuid;
    private final String username;
    private final List<Permission> permissions;
    private final List<Rank> ranks;
    private final Map<String, Object> additionalData;

    // Cache for performance
    private volatile List<Permission> cachedActivePermissions;
    private volatile List<Rank> cachedActiveRanks;
    private volatile Rank cachedHighestRank;
    private volatile long lastCacheUpdate = -1;
    private static final long CACHE_DURATION = 30000; // 30 seconds

    public PlayerProfile(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
        this.permissions = new ArrayList<>();
        this.ranks = new ArrayList<>();
        this.additionalData = new HashMap<>();
    }

    public void addPermission(Permission permission) {
        permissions.add(permission);
        invalidateCache();
    }

    public void addRank(Rank rank) {
        ranks.add(rank);
        invalidateCache();
    }

    public List<Permission> getActivePermissions() {
        if (isCacheValid() && cachedActivePermissions != null) {
            return cachedActivePermissions;
        }

        cachedActivePermissions = permissions.stream()
                .filter(Permission::isActive)
                .toList(); // More efficient than collect(Collectors.toList())
        updateCacheTimestamp();
        return cachedActivePermissions;
    }

    public List<Rank> getActiveRanks() {
        if (isCacheValid() && cachedActiveRanks != null) {
            return cachedActiveRanks;
        }

        cachedActiveRanks = ranks.stream()
                .filter(Rank::isActive)
                .toList();
        updateCacheTimestamp();
        return cachedActiveRanks;
    }

    public boolean hasPermission(String node) {
        return getActivePermissions().stream()
                .anyMatch(perm -> perm.getNode().equals(node));
    }

    public boolean hasRank(String rankName) {
        return getActiveRanks().stream()
                .anyMatch(rank -> rank.getName().equalsIgnoreCase(rankName));
    }

    public Rank getHighestRank() {
        if (isCacheValid() && cachedHighestRank != null) {
            return cachedHighestRank;
        }

        cachedHighestRank = getActiveRanks().stream()
                .max(Comparator.comparingInt(r ->
                        RankConfig.getRankInfo(r.getName()).getPriority()))
                .orElse(null);
        updateCacheTimestamp();
        return cachedHighestRank;
    }

    private boolean isCacheValid() {
        return System.currentTimeMillis() - lastCacheUpdate < CACHE_DURATION;
    }

    private void updateCacheTimestamp() {
        lastCacheUpdate = System.currentTimeMillis();
    }

    private void invalidateCache() {
        cachedActivePermissions = null;
        cachedActiveRanks = null;
        cachedHighestRank = null;
        lastCacheUpdate = -1;
    }

    // Getters
    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public List<Permission> getPermissions() { return new ArrayList<>(permissions); } // Defensive copy
    public List<Rank> getRanks() { return new ArrayList<>(ranks); } // Defensive copy
    public Map<String, Object> getAdditionalData() { return new HashMap<>(additionalData); } // Defensive copy
}