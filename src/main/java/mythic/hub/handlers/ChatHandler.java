package mythic.hub.handlers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerChatEvent;
import mythic.hub.MythicHubServer;

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

        // Chat is not locked or player is staff - allow the message
        // You can add additional chat formatting here if needed
    }
}