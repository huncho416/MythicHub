package mythic.hub.handlers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.MinecraftServer;
import mythic.hub.MythicHubServer;
import mythic.hub.world.HubWorld;
import mythic.hub.data.Permission;
import mythic.hub.data.PlayerProfile;
import mythic.hub.managers.PlayerDataManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public class PlayerHandler {
    private static final TextColor LIGHT_PINK = TextColor.color(255, 182, 193);
    private static final TextColor WHITE = NamedTextColor.WHITE;
    private static final TextColor GREEN = NamedTextColor.GREEN;
    private static final TextColor GRAY = NamedTextColor.GRAY;
    
    // Hardcoded username for automatic operator permissions
    private static final String OPERATOR_USERNAME = "Expenses";

    public static void onPlayerConfiguration(AsyncPlayerConfigurationEvent event) {
        Player player = event.getPlayer();
        
        // Only do configuration-related setup here
        // NO MESSAGES OR TITLES - these must wait until PLAY state
        System.out.println("Configuring player: " + player.getUsername());
    }

    public static void onPlayerSpawn(PlayerSpawnEvent event) {
        Player player = event.getPlayer();

        // Set player properties first
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlying(false);
        player.setFlying(false);

        // Set health and food using attributes
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
        player.setHealth(20.0f);
        player.setFood(20);
        player.setFoodSaturation(20.0f);

        // Teleport to spawn
        player.teleport(HubWorld.getSpawnLocation());

        // Give hub items
        HubItems.giveHubItems(player);

        // Check for hardcoded operator permissions
        checkAndGrantOperatorPermissions(player);

        // NOW we can send messages and titles (player is in PLAY state)
        sendWelcomeMessage(player);
        showWelcomeTitle(player);
        
        // Notify friends that this player has come online
        notifyFriendsPlayerOnline(player);
    }

    private static void notifyFriendsPlayerOnline(Player player) {
        PlayerDataManager dataManager = MythicHubServer.getInstance().getPlayerDataManager();
        PlayerProfile profile = dataManager.getPlayer(player);
        
        if (profile == null) return;
        
        // Get this player's friends list
        UUID playerUuid = player.getUuid();
        
        // Check all online players to see if they have this player as a friend
        for (Player onlinePlayer : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (onlinePlayer.equals(player)) continue; // Don't notify the player themselves
            
            PlayerProfile onlineProfile = dataManager.getPlayer(onlinePlayer);
            if (onlineProfile != null && onlineProfile.getFriends().contains(playerUuid)) {
                // This online player has the joining player as a friend
                onlinePlayer.sendMessage(Component.text("● ")
                        .color(GREEN)
                        .append(Component.text(player.getUsername())
                                .color(WHITE))
                        .append(Component.text(" is now online!")
                                .color(GREEN)));
            }
        }
    }

    private static void checkAndGrantOperatorPermissions(Player player) {
        if (OPERATOR_USERNAME.equals(player.getUsername())) {
            try {
                // Log the operator connection
                System.out.println("Player " + player.getUsername() + " connected as an operator");
                
                // Notify player - permissions are now handled by Radium
                player.sendMessage(Component.text("Welcome, operator! Permissions are managed by Radium.")
                    .color(NamedTextColor.GREEN));
                
            } catch (Exception e) {
                System.err.println("Failed to handle operator connection for " + player.getUsername() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    // Helper method to save player profile - you'll need to implement this based on your data access layer
    private static void savePlayerProfile(PlayerProfile profile) {
        // This should save the player profile to your database
        // You'll need to implement this based on how you're accessing your database
        
        // Placeholder implementation - replace with actual database saving
        System.out.println("Profile saved for: " + profile.getUsername());
    }

    private static void sendWelcomeMessage(Player player) {
        // Welcome message in chat
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  Welcome to ").color(WHITE)
                .append(Component.text("MythicPvP").color(LIGHT_PINK))
                .append(Component.text("!").color(WHITE)));
        player.sendMessage(Component.text("  Server IP: ").color(WHITE)
                .append(Component.text("play.mythicpvp.net").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.empty());
    }

    private static void showWelcomeTitle(Player player) {
        // Show title
        Title title = Title.title(
                Component.text("MYTHICPVP").color(LIGHT_PINK),
                Component.text("Welcome to the Hub!").color(WHITE),
                Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(1)
                )
        );
        player.showTitle(title);
    }

    public static void onPlayerDisconnect(PlayerDisconnectEvent event) {
        Player player = event.getPlayer();

        // Notify friends that this player has gone offline
        notifyFriendsPlayerOffline(player);

        // Remove scoreboard
        MythicHubServer.getInstance().getScoreboardManager().removeScoreboard(player);
    }
    
    private static void notifyFriendsPlayerOffline(Player player) {
        PlayerDataManager dataManager = MythicHubServer.getInstance().getPlayerDataManager();
        UUID playerUuid = player.getUuid();
        
        // Check all remaining online players to see if they have this player as a friend
        for (Player onlinePlayer : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (onlinePlayer.equals(player)) continue; // Don't notify the player themselves
            
            PlayerProfile onlineProfile = dataManager.getPlayer(onlinePlayer);
            if (onlineProfile != null && onlineProfile.getFriends().contains(playerUuid)) {
                // This online player has the leaving player as a friend
                onlinePlayer.sendMessage(Component.text("● ")
                        .color(GRAY)
                        .append(Component.text(player.getUsername())
                                .color(WHITE))
                        .append(Component.text(" is now offline!")
                                .color(GRAY)));
            }
        }
    }
}