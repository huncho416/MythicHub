package mythic.hub.managers;

import mythic.hub.MythicHubServer;
import mythic.hub.config.RankConfig;
import mythic.hub.data.PlayerProfile;
import mythic.hub.data.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import net.minestom.server.network.packet.server.play.PlayerListHeaderAndFooterPacket;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TabListManager {
    private final TextColor LIGHT_PINK = TextColor.color(255, 182, 193);
    private final TextColor WHITE = NamedTextColor.WHITE;
    private final TextColor GRAY = NamedTextColor.GRAY;
    private final TextColor DARK_GRAY = NamedTextColor.DARK_GRAY;
    
    private final ConcurrentHashMap<Player, Component> playerDisplayNames = new ConcurrentHashMap<>();

    public void updateTabList(Player player) {
        // Update header and footer
        updateHeaderFooter(player);
        
        // Update player display name
        updatePlayerDisplayName(player);
        
        // Update all other players' display names for this player
        updateAllPlayerDisplayNames(player);
    }

    public void updateAllTabLists() {
        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(this::updateTabList);
    }

    private void updateHeaderFooter(Player player) {
        // Get server name from system properties or default
        String serverName = System.getProperty("server.name", "Hub-1");
        
        // Calculate values for footer
        int currentServerPlayers = MinecraftServer.getConnectionManager().getOnlinePlayers().size();
        int globalPlayers = getGlobalPlayerCount(); // You'll need to implement this
        int hubPlayers = getHubPlayerCount(); // You'll need to implement this
        int playerPing = player.getLatency();
        double serverTps = getServerTps(); // You'll need to implement this
        
        // Header - final positioning adjustments
        Component header = Component.text()
                .append(Component.text("     ").append(Component.text("MYTHIC").color(LIGHT_PINK).decorate(TextDecoration.BOLD))
                        .append(Component.text("PVP").color(WHITE).decorate(TextDecoration.BOLD)))
                .append(Component.newline())
                .append(Component.newline()) // Blank line
                .append(Component.text("     ").append(Component.text("Connected to: ").color(WHITE))
                        .append(Component.text(serverName).color(LIGHT_PINK)))
                .append(Component.newline())
                .append(Component.text("      ").append(Component.text("play.mythicpvp.net").color(GRAY)))
                .build();

        // Footer - updated hub player text
        Component footer = Component.text()
                .append(Component.text("Ping: ").color(LIGHT_PINK)
                        .append(Component.text(playerPing + "ms").color(WHITE))
                        .append(Component.text(" * ").color(DARK_GRAY))
                        .append(Component.text("TPS: ").color(LIGHT_PINK))
                        .append(Component.text(String.format("%.1f", serverTps)).color(WHITE))
                        .append(Component.text(" * ").color(DARK_GRAY))
                        .append(Component.text("store.mythicpvp.net").color(LIGHT_PINK)))
                .append(Component.newline())
                .append(Component.text(String.valueOf(globalPlayers)).color(LIGHT_PINK)
                        .append(Component.text(" Global").color(WHITE))
                        .append(Component.text(" (" + hubPlayers + " on all hubs)").color(GRAY)))
                .build();

        player.sendPacket(new PlayerListHeaderAndFooterPacket(header, footer));
    }

    private void updatePlayerDisplayName(Player player) {
        Component displayName = getPlayerRankAndName(player);
        playerDisplayNames.put(player, displayName);
        player.setDisplayName(displayName);
        
        // Create player info update packet to update display name for all players
        PlayerInfoUpdatePacket.Entry entry = new PlayerInfoUpdatePacket.Entry(
                player.getUuid(),
                player.getUsername(),
                List.of(), // properties
                player.isOnline(), // listed
                player.getLatency(), // latency
                player.getGameMode(), // game mode
                displayName, // display name
                null // chat session
        );
        
        PlayerInfoUpdatePacket packet = new PlayerInfoUpdatePacket(
                EnumSet.of(PlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                List.of(entry)
        );
        
        // Send to all online players
        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(onlinePlayer -> {
            onlinePlayer.sendPacket(packet);
        });
    }

    private void updateAllPlayerDisplayNames(Player viewer) {
        // Create a batch update for all players' display names
        List<PlayerInfoUpdatePacket.Entry> entries = MinecraftServer.getConnectionManager()
                .getOnlinePlayers()
                .stream()
                .filter(p -> !p.equals(viewer))
                .map(p -> {
                    Component displayName = playerDisplayNames.get(p);
                    if (displayName == null) {
                        displayName = getPlayerRankAndName(p);
                        playerDisplayNames.put(p, displayName);
                    }
                    
                    return new PlayerInfoUpdatePacket.Entry(
                            p.getUuid(),
                            p.getUsername(),
                            List.of(), // properties
                            p.isOnline(), // listed
                            p.getLatency(), // latency
                            p.getGameMode(), // game mode
                            displayName, // display name
                            null // chat session
                    );
                })
                .toList();
        
        if (!entries.isEmpty()) {
            PlayerInfoUpdatePacket packet = new PlayerInfoUpdatePacket(
                    EnumSet.of(PlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                    entries
            );
            
            viewer.sendPacket(packet);
        }
    }

    private Component getPlayerRankAndName(Player player) {
        PlayerDataManager dataManager = MythicHubServer.getInstance().getPlayerDataManager();
        PlayerProfile profile = dataManager.getPlayer(player);
        
        if (profile != null) {
            Rank highestRank = getHighestPriorityRank(profile);
            if (highestRank != null && !highestRank.getName().equalsIgnoreCase("DEFAULT") && !highestRank.getName().equalsIgnoreCase("MEMBER")) {
                RankConfig.RankInfo rankInfo = RankConfig.getRankInfo(highestRank.getName());
                return Component.text("[" + highestRank.getName() + "] ")
                        .color(rankInfo.getColor())
                        .append(Component.text(player.getUsername()).color(rankInfo.getColor()));
            }
        }
        
        // Default display (no prefix for default/member ranks)
        return Component.text(player.getUsername()).color(GRAY);
    }

    private Rank getHighestPriorityRank(PlayerProfile profile) {
        return profile.getActiveRanks().stream()
                .max((r1, r2) -> Integer.compare(
                        RankConfig.getRankInfo(r1.getName()).getPriority(),
                        RankConfig.getRankInfo(r2.getName()).getPriority()
                ))
                .orElse(null);
    }

    public void removePlayer(Player player) {
        playerDisplayNames.remove(player);
    }

    public void updateSinglePlayerDisplay(Player player) {
        Component displayName = getPlayerRankAndName(player);
        playerDisplayNames.put(player, displayName);
        player.setDisplayName(displayName);
        
        // Create player info update packet for this specific player
        PlayerInfoUpdatePacket.Entry entry = new PlayerInfoUpdatePacket.Entry(
                player.getUuid(),
                player.getUsername(),
                List.of(), // properties
                player.isOnline(), // listed
                player.getLatency(), // latency
                player.getGameMode(), // game mode
                displayName, // display name
                null // chat session
        );
        
        PlayerInfoUpdatePacket packet = new PlayerInfoUpdatePacket(
                EnumSet.of(PlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                List.of(entry)
        );
        
        // Send to all online players
        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(onlinePlayer -> {
            onlinePlayer.sendPacket(packet);
        });
    }

    // Helper methods - you'll need to implement these based on your server architecture
    private int getGlobalPlayerCount() {
        // TODO: Implement Redis-based global player count
        // For now, return current server count as placeholder
        return MinecraftServer.getConnectionManager().getOnlinePlayers().size();
    }

    private int getHubPlayerCount() {
        // TODO: Implement Redis-based hub player count (all hub servers combined)
        // For now, return current server count as placeholder
        return MinecraftServer.getConnectionManager().getOnlinePlayers().size();
    }

    private double getServerTps() {
        // TODO: Implement TPS calculation
        // For now, return 20.0 as placeholder
        return 20.0;
    }
}