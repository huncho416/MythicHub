package mythic.hub.data;

import java.time.LocalDateTime;

public class Rank {
    private final String name;
    private final String grantedBy;
    private final LocalDateTime grantedDate;
    private final LocalDateTime expireDate;
    private final String reason;
    private final boolean revoked;
    private final String revokedBy;
    private final LocalDateTime revokedDate;
    private final String revokedReason;

    // Cache the active status
    private volatile Boolean cachedIsActive;
    private volatile long lastActiveCheck = -1;
    private static final long ACTIVE_CHECK_CACHE_DURATION = 10000; // 10 seconds

    public Rank(String name, String grantedBy, LocalDateTime grantedDate,
                LocalDateTime expireDate, String reason, boolean revoked,
                String revokedBy, LocalDateTime revokedDate, String revokedReason) {
        this.name = name;
        this.grantedBy = grantedBy;
        this.grantedDate = grantedDate;
        this.expireDate = expireDate;
        this.reason = reason;
        this.revoked = revoked;
        this.revokedBy = revokedBy;
        this.revokedDate = revokedDate;
        this.revokedReason = revokedReason;
    }

    // Parse from Redis string format - optimized
    public static Rank fromString(String data) {
        String[] parts = data.split("\\|", -1);

        String name = parts[0];
        String grantedBy = parts[1];
        LocalDateTime grantedDate = parseDateTime(parts[2]);
        LocalDateTime expireDate = "null".equals(parts[3]) ? null : parseDateTime(parts[3]);
        String reason = parts.length > 4 && !"null".equals(parts[4]) ? parts[4] : null;

        boolean revoked = false;
        String revokedBy = null;
        LocalDateTime revokedDate = null;
        String revokedReason = null;

        if (parts.length > 5) {
            revoked = Boolean.parseBoolean(parts[5]);
            revokedBy = parts.length > 6 && !"null".equals(parts[6]) ? parts[6] : null;
            revokedDate = parts.length > 7 && !"null".equals(parts[7]) ? parseDateTime(parts[7]) : null;
            revokedReason = parts.length > 8 && !"null".equals(parts[8]) ? parts[8] : null;
        }

        return new Rank(name, grantedBy, grantedDate, expireDate, reason, revoked, revokedBy, revokedDate, revokedReason);
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
    public String getName() { return name; }
    public String getGrantedBy() { return grantedBy; }
    public LocalDateTime getGrantedDate() { return grantedDate; }
    public LocalDateTime getExpireDate() { return expireDate; }
    public String getReason() { return reason; }
    public boolean isRevoked() { return revoked; }
    public String getRevokedBy() { return revokedBy; }
    public LocalDateTime getRevokedDate() { return revokedDate; }
    public String getRevokedReason() { return revokedReason; }
}