package mythic.hub.data;

import java.util.*;

public class PlayerProfile {
    private final UUID uuid;
    private final String username;
    private final Map<String, Object> additionalData;

    // Friends functionality
    private List<UUID> friends = new ArrayList<>();
    private List<UUID> friendRequests = new ArrayList<>();

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

    // Friends functionality
    public List<UUID> getFriends() {
        return friends;
    }

    public void setFriends(List<UUID> friends) {
        this.friends = friends;
    }

    public List<UUID> getFriendRequests() {
        return friendRequests;
    }

    public void setFriendRequests(List<UUID> friendRequests) {
        this.friendRequests = friendRequests;
    }

    public void addFriend(UUID friendUuid) {
        if (!friends.contains(friendUuid)) {
            friends.add(friendUuid);
        }
    }

    public void removeFriend(UUID friendUuid) {
        friends.remove(friendUuid);
    }

    public void addFriendRequest(UUID requesterUuid) {
        if (!friendRequests.contains(requesterUuid)) {
            friendRequests.add(requesterUuid);
        }
    }

    public void removeFriendRequest(UUID requesterUuid) {
        friendRequests.remove(requesterUuid);
    }

    public boolean isFriend(UUID otherUuid) {
        return friends.contains(otherUuid);
    }

    public boolean hasFriendRequest(UUID requesterUuid) {
        return friendRequests.contains(requesterUuid);
    }

    public boolean hasPendingFriendRequest(UUID requesterUuid) {
        return friendRequests.contains(requesterUuid);
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
                ", friends=" + friends.size() +
                ", friendRequests=" + friendRequests.size() +
                '}';
    }
}