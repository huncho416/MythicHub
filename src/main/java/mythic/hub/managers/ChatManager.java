package mythic.hub.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerChatEvent;
import mythic.hub.MythicHubServer;
import mythic.hub.config.RankConfig;
import mythic.hub.data.PlayerProfile;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatManager {
    private final AtomicBoolean chatLocked = new AtomicBoolean(false);
    private final ConcurrentHashMap<Player, String> playerTags = new ConcurrentHashMap<>();
    
    /**
     * Locks the chat so only staff can type
     * @param lockedBy The staff member who locked the chat
     */
    public void lockChat(Player lockedBy) {
        chatLocked.set(true);
        
        // Broadcast to all players
        Component lockMessage = Component.text("Chat has been locked by ")
            .color(NamedTextColor.RED)
            .append(Component.text(lockedBy.getUsername()).color(NamedTextColor.YELLOW))
            .append(Component.text("! Only staff can type.").color(NamedTextColor.RED));
        
        broadcastToAll(lockMessage);
    }
    
    /**
     * Unlocks the chat
     * @param unlockedBy The staff member who unlocked the chat
     */
    public void unlockChat(Player unlockedBy) {
        chatLocked.set(false);
        
        // Broadcast to all players
        Component unlockMessage = Component.text("Chat has been unlocked by ")
            .color(NamedTextColor.GREEN)
            .append(Component.text(unlockedBy.getUsername()).color(NamedTextColor.YELLOW))
            .append(Component.text("!").color(NamedTextColor.GREEN));
        
        broadcastToAll(unlockMessage);
    }
    
    /**
     * Sets a custom tag for a player
     * @param player The player to set the tag for
     * @param tag The tag to set
     */
    public void setPlayerTag(Player player, String tag) {
        playerTags.put(player, tag);
        System.out.println("Set tag '" + tag + "' for player " + player.getUsername());
    }
    
    /**
     * Removes a player's custom tag
     * @param player The player to remove the tag from
     */
    public void removePlayerTag(Player player) {
        String removedTag = playerTags.remove(player);
        if (removedTag != null) {
            System.out.println("Removed tag '" + removedTag + "' from player " + player.getUsername());
        }
    }
    
    /**
     * Gets a player's custom tag
     * @param player The player to get the tag for
     * @return The player's tag, or null if they don't have one
     */
    public String getPlayerTag(Player player) {
        return playerTags.get(player);
    }
    
    /**
     * Handles player chat events
     * @param event The chat event
     */
    public void handlePlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // Check if chat is locked and player can chat
        if (!canPlayerChat(player)) {
            event.setCancelled(true);
            
            // Send message to player that chat is locked
            player.sendMessage(Component.text("Chat is currently locked! Only staff members can type.")
                .color(NamedTextColor.RED));
            return;
        }
        
        // Format the chat message with tags and ranks
        Component formattedMessage = formatChatMessage(player, message);
        if (formattedMessage != null) {
            // Cancel the event and send the formatted message manually
            event.setCancelled(true);
            broadcastToAll(formattedMessage);
        }
    }
    
    /**
     * Formats a chat message with player tags and ranks
     * @param player The player sending the message
     * @param message The message content
     * @return The formatted message component
     */
    private Component formatChatMessage(Player player, String message) {
        Component formattedMessage = Component.empty();
        
        // Get player's highest rank
        String playerRank = getPlayerHighestRank(player);
        RankConfig.RankInfo rankInfo = RankConfig.getRankInfo(playerRank);
        
        // Add rank prefix if not default
        if (!"DEFAULT".equals(playerRank.toUpperCase()) && !"MEMBER".equals(playerRank.toUpperCase())) {
            formattedMessage = formattedMessage.append(
                Component.text("[" + playerRank + "] ")
                    .color(rankInfo.getColor())
            );
        }
        
        // Add custom tag if player has one
        String tag = getPlayerTag(player);
        if (tag != null && !tag.isEmpty()) {
            formattedMessage = formattedMessage.append(Component.text("[" + tag + "] ").color(NamedTextColor.LIGHT_PURPLE));
        }
        
        // Add player name with rank color
        formattedMessage = formattedMessage.append(Component.text(player.getUsername()).color(rankInfo.getColor()));
        
        // Add separator and message
        formattedMessage = formattedMessage.append(Component.text(": ").color(NamedTextColor.GRAY));
        formattedMessage = formattedMessage.append(Component.text(message).color(NamedTextColor.WHITE));
        
        return formattedMessage;
    }
    
    /**
     * Gets the player's highest rank
     * @param player The player to get the rank for
     * @return The player's highest rank name
     */
    private String getPlayerHighestRank(Player player) {
        try {
            PlayerProfile profile = getPlayerProfile(player);
            if (profile != null) {
                // Get the highest priority rank from the player's active ranks
                return profile.getActiveRanks().stream()
                    .map(rank -> rank.getName().toUpperCase())
                    .max((rank1, rank2) -> Integer.compare(
                        RankConfig.getRankInfo(rank1).getPriority(),
                        RankConfig.getRankInfo(rank2).getPriority()
                    ))
                    .orElse("DEFAULT");
            }
        } catch (Exception e) {
            System.err.println("Error getting rank for " + player.getUsername() + ": " + e.getMessage());
        }
        
        // Default fallback
        return "DEFAULT";
    }
    
    /**
     * Checks if a player can send a message
     * @param player The player trying to send a message
     * @return true if the player can send messages, false otherwise
     */
    public boolean canPlayerChat(Player player) {
        if (!chatLocked.get()) {
            return true; // Chat is not locked, everyone can chat
        }
        
        // Chat is locked, check if player is staff
        return isPlayerStaff(player);
    }
    
    /**
     * Checks if the chat is currently locked
     * @return true if chat is locked, false otherwise
     */
    public boolean isChatLocked() {
        return chatLocked.get();
    }
    
    /**
     * Checks if a player has staff permissions
     * @param player The player to check
     * @return true if player is staff, false otherwise
     */
    private boolean isPlayerStaff(Player player) {
        try {
            // Check if player has staff permission or is the hardcoded operator
            if ("Expenses".equals(player.getUsername())) {
                return true; // Hardcoded operator
            }
            
            // Check if player has staff rank or permission
            String playerRank = getPlayerHighestRank(player);
            RankConfig.RankInfo rankInfo = RankConfig.getRankInfo(playerRank);
            
            // Staff ranks (priority 600+)
            if (rankInfo.getPriority() >= 600) {
                return true;
            }
            
            // Check for staff permission
            PlayerProfile profile = getPlayerProfile(player);
            return profile != null && profile.hasPermission("mythic.staff");
            
        } catch (Exception e) {
            System.err.println("Error checking staff status for " + player.getUsername() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Broadcasts a message to all online players
     * @param message The message to broadcast
     */
    private void broadcastToAll(Component message) {
        for (Player player : getAllOnlinePlayers()) {
            player.sendMessage(message);
        }
    }
    
    /**
     * Cleans up data when a player disconnects
     * @param player The player who disconnected
     */
    public void onPlayerDisconnect(Player player) {
        playerTags.remove(player);
    }
    
    // Helper methods
    private PlayerProfile getPlayerProfile(Player player) {
        try {
            // Use getPlayer() method which exists in PlayerDataManager
            return MythicHubServer.getInstance().getPlayerDataManager().getPlayer(player.getUuid());
        } catch (Exception e) {
            System.err.println("Error getting player profile for " + player.getUsername() + ": " + e.getMessage());
            return null;
        }
    }
    
    private Iterable<Player> getAllOnlinePlayers() {
        // Get all online players from Minestom
        return MinecraftServer.getConnectionManager().getOnlinePlayers();
    }
}