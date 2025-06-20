package mythic.hub.managers;

import net.minestom.server.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();

    public void addPlayer(Player player) {
        playerDataMap.put(player.getUuid(), new PlayerData(player));
    }

    public void removePlayer(Player player) {
        playerDataMap.remove(player.getUuid());
    }

    public PlayerData getPlayerData(Player player) {
        return playerDataMap.get(player.getUuid());
    }

    public static class PlayerData {
        private final Player player;
        private boolean playersVisible = true;
        private long coins = 0;
        private String rank = "Player";

        public PlayerData(Player player) {
            this.player = player;
        }

        public Player getPlayer() {
            return player;
        }

        public boolean arePlayersVisible() {
            return playersVisible;
        }

        public void setPlayersVisible(boolean playersVisible) {
            this.playersVisible = playersVisible;
        }

        public long getCoins() {
            return coins;
        }

        public void setCoins(long coins) {
            this.coins = coins;
        }

        public String getRank() {
            return rank;
        }

        public void setRank(String rank) {
            this.rank = rank;
        }
    }
}