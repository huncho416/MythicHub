package mythic.hub.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import mythic.hub.MythicHubServer;

public class ChatCommands extends Command {
    
    public ChatCommands() {
        super("chatlock", "lockchat");
        
        setDefaultExecutor(this::usage);
        
        addSubcommand(new Command("lock") {{
            setDefaultExecutor(ChatCommands.this::lockChat);
        }});
        
        addSubcommand(new Command("unlock") {{
            setDefaultExecutor(ChatCommands.this::unlockChat);
        }});
        
        addSubcommand(new Command("status") {{
            setDefaultExecutor(ChatCommands.this::chatStatus);
        }});
    }
    
    private void usage(CommandSender sender, CommandContext context) {
        sender.sendMessage(Component.text("Usage:").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/chatlock lock - Lock the chat").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/chatlock unlock - Unlock the chat").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/chatlock status - Check chat status").color(NamedTextColor.GRAY));
    }
    
    private void lockChat(CommandSender sender, CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
            return;
        }
        
        if (!isPlayerStaff(player)) {
            player.sendMessage(Component.text("You don't have permission to use this command!").color(NamedTextColor.RED));
            return;
        }
        
        if (MythicHubServer.getInstance().getChatManager().isChatLocked()) {
            player.sendMessage(Component.text("Chat is already locked!").color(NamedTextColor.YELLOW));
            return;
        }
        
        MythicHubServer.getInstance().getChatManager().lockChat(player);
    }
    
    private void unlockChat(CommandSender sender, CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
            return;
        }
        
        if (!isPlayerStaff(player)) {
            player.sendMessage(Component.text("You don't have permission to use this command!").color(NamedTextColor.RED));
            return;
        }
        
        if (!MythicHubServer.getInstance().getChatManager().isChatLocked()) {
            player.sendMessage(Component.text("Chat is not locked!").color(NamedTextColor.YELLOW));
            return;
        }
        
        MythicHubServer.getInstance().getChatManager().unlockChat(player);
    }
    
    private void chatStatus(CommandSender sender, CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
            return;
        }
        
        if (!isPlayerStaff(player)) {
            player.sendMessage(Component.text("You don't have permission to use this command!").color(NamedTextColor.RED));
            return;
        }
        
        boolean locked = MythicHubServer.getInstance().getChatManager().isChatLocked();
        Component status = Component.text("Chat Status: ")
            .color(NamedTextColor.YELLOW)
            .append(Component.text(locked ? "LOCKED" : "UNLOCKED")
                .color(locked ? NamedTextColor.RED : NamedTextColor.GREEN));
        
        player.sendMessage(status);
    }
    
    private boolean isPlayerStaff(Player player) {
        // Check if player is staff - implement based on your permission system
        return "Expenses".equals(player.getUsername()); // Placeholder
    }
}