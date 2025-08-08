package mythic.hub.commands;

import mythic.hub.MythicHubServer;
import mythic.hub.integrations.radium.RadiumClient;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;

/**
 * Test command to verify Radium integration
 */
public class RadiumTestCommand extends Command {
    
    public RadiumTestCommand() {
        super("radiumtest", "rtest");
        
        setDefaultExecutor(this::testRadium);
    }
    
    private void testRadium(CommandSender sender, CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
            return;
        }
        
        RadiumClient radiumClient = MythicHubServer.getInstance().getRadiumClient();
        
        player.sendMessage(Component.text("=== Radium Integration Test ===").color(NamedTextColor.GOLD));
        
        // Test permission check
        radiumClient.hasPermission(player.getUuid(), "mythic.staff").thenAccept(hasStaff -> {
            player.sendMessage(Component.text("Staff Permission: " + (hasStaff ? "✓" : "✗"))
                    .color(hasStaff ? NamedTextColor.GREEN : NamedTextColor.RED));
        });
        
        // Test profile retrieval
        radiumClient.getPlayerProfile(player.getUuid()).thenAccept(profile -> {
            player.sendMessage(Component.text("Profile: " + profile.getPrimaryRank())
                    .color(NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Ranks: " + String.join(", ", profile.getRanks()))
                    .color(NamedTextColor.YELLOW));
        });
        
        // Show available forwarded commands
        player.sendMessage(Component.text("Available Staff Commands:").color(NamedTextColor.AQUA));
        player.sendMessage(Component.text("• /rank <args> - Rank management").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("• /grant <player> <rank> - Grant rank").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("• /permission <args> - Permission management").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("• /vanish - Toggle vanish").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("• /staffchat <message> - Staff chat").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("• /gmc, /gms, /gamemode - Gamemode").color(NamedTextColor.GRAY));
        
        player.sendMessage(Component.text("Commands are forwarded to Radium proxy for execution!")
                .color(NamedTextColor.GREEN));
    }
}
