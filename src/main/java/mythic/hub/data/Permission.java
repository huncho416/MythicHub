package mythic.hub.data;

import java.time.LocalDateTime;

public class Permission {
    private final String node;
    private final String grantedBy;
    private final LocalDateTime grantedDate;
    private final LocalDateTime expireDate;
    private final boolean revoked;
    private final String revokedBy;
    private final LocalDateTime revokedDate;

    // Cache the active status to avoid recalculating
    private volatile Boolean cachedIsActive;
    private volatile long lastActiveCheck = -1;
    private static final long ACTIVE_CHECK_CACHE_DURATION = 10000; // 10 seconds

    public Permission(String node, String grantedBy, LocalDateTime grantedDate,
                      LocalDateTime expireDate, boolean revoked, String revokedBy,
                      LocalDateTime revokedDate) {
        this.node = node;
        this.grantedBy = grantedBy;
        this.grantedDate = grantedDate;
        this.expireDate = expireDate;
        this.revoked = revoked;
        this.revokedBy = revokedBy;
        this.revokedDate = revokedDate;
    }

    // Parse from Redis string format - optimized with StringBuilder
    public static Permission fromString(String data) {
        String[] parts = data.split("\\|", -1); // -1 to include empty strings

        String node = parts[0];
        String grantedBy = parts[1];
        LocalDateTime grantedDate = parseDateTime(parts[2]);
        LocalDateTime expireDate = "null".equals(parts[3]) ? null : parseDateTime(parts[3]);

        boolean revoked = false;
        String revokedBy = null;
        LocalDateTime revokedDate = null;

        if (parts.length > 4) {
            revoked = Boolean.parseBoolean(parts[4]);
            revokedBy = parts.length > 5 && !"null".equals(parts[5]) ? parts[5] : null;
            revokedDate = parts.length > 6 && !"null".equals(parts[6]) ? parseDateTime(parts[6]) : null;
        }

        return new Permission(node, grantedBy, grantedDate, expireDate, revoked, revokedBy, revokedDate);
    }

    private static LocalDateTime parseDateTime(String dateStr) {
        return (dateStr == null || "null".equals(dateStr)) ? null : LocalDateTime.parse(dateStr);
    }

    public boolean isActive() {
        // Use cached result if valid
        if (cachedIsActive != null &&
                System.currentTimeMillis() - lastActiveCheck < ACTIVE_CHECK_CACHE_DURATION) {
            return cachedIsActive;
        }

        boolean active = !revoked &&
                (expireDate == null || LocalDateTime.now().isBefore(expireDate));

        // Cache the result
        cachedIsActive = active;
        lastActiveCheck = System.currentTimeMillis();

        return active;
    }

    // Getters
    public String getNode() { return node; }
    public String getGrantedBy() { return grantedBy; }
    public LocalDateTime getGrantedDate() { return grantedDate; }
    public LocalDateTime getExpireDate() { return expireDate; }
    public boolean isRevoked() { return revoked; }
    public String getRevokedBy() { return revokedBy; }
    public LocalDateTime getRevokedDate() { return revokedDate; }
}