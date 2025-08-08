package mythic.hub.handlers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerChatEvent;
import mythic.hub.MythicHubServer;
import mythic.hub.integrations.radium.RadiumClient;

public class ChatHandler {

    public static void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Check if chat is locked and player can chat
        if (!MythicHubServer.getInstance().getChatManager().canPlayerChat(player)) {
            event.setCancelled(true);

            // Send message to player that chat is locked
            player.sendMessage(Component.text("Chat is currently locked! Only staff members can type.")
                    .color(NamedTextColor.RED));
            return;
        }

        // Cancel the original event and handle with Radium formatting
        event.setCancelled(true);
        
        // Get Radium client and format the message
        RadiumClient radiumClient = MythicHubServer.getInstance().getRadiumClient();
        
        radiumClient.formatChatMessage(player.getUuid(), player.getUsername(), message)
            .thenAccept(formattedMessage -> {
                // Broadcast the formatted message to all players
                MythicHubServer.getInstance().getChatManager().broadcastToAll(formattedMessage);
            })
            .exceptionally(throwable -> {
                // Fallback if Radium formatting fails
                Component fallbackMessage = Component.text(player.getUsername() + ": " + message)
                        .color(NamedTextColor.WHITE);
                MythicHubServer.getInstance().getChatManager().broadcastToAll(fallbackMessage);
                
                System.err.println("Error formatting chat message with Radium: " + throwable.getMessage());
                return null;
            });
    }
}