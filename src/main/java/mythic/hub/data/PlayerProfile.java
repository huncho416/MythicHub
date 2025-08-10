package mythic.hub.data;

import java.util.*;

public class PlayerProfile {
    private final UUID uuid;
    private final String username;
    private final Map<String, Object> additionalData;

    public PlayerProfile(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
        this.additionalData = new HashMap<>();
    }

    // Basic getters
    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    // Additional data methods
    public void setData(String key, Object value) {
        additionalData.put(key, value);
    }

    public Object getData(String key) {
        return additionalData.get(key);
    }

    public <T> T getData(String key, Class<T> type) {
        Object value = additionalData.get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    @Override
    public String toString() {
        return "PlayerProfile{" +
                "uuid=" + uuid +
                ", username='" + username + '\'' +
                '}';
    }
}