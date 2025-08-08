package mythic.hub.commands;

import mythic.hub.MythicHubServer;
import mythic.hub.integrations.radium.RadiumClient;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;

/**
 * Handles forwarding of staff commands to Radium proxy
 */
public class StaffCommandForwarder extends Command {
    
    public StaffCommandForwarder(String name, String... aliases) {
        super(name, aliases);
        
        // Accept any arguments after the command
        var args = ArgumentType.StringArray("args");
        args.setDefaultValue(new String[0]);
        
        addSyntax(this::forwardCommand, args);
        setDefaultExecutor(this::forwardCommand);
    }
    
    private void forwardCommand(CommandSender sender, CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
            return;
        }
        
        // Check if player has permission to use staff commands
        RadiumClient radiumClient = MythicHubServer.getInstance().getRadiumClient();
        
        try {
            boolean hasPermission = radiumClient.hasPermission(player.getUuid(), "mythic.staff").get();
            if (!hasPermission) {
                player.sendMessage(Component.text("You don't have permission to use this command!").color(NamedTextColor.RED));
                return;
            }
        } catch (Exception e) {
            player.sendMessage(Component.text("Error checking permissions. Please try again.").color(NamedTextColor.RED));
            System.err.println("Error checking staff permission: " + e.getMessage());
            return;
        }
        
        // Build the full command string
        String commandName = context.getCommandName();
        String[] args = context.getOrDefault("args", new String[0]);
        
        String fullCommand = commandName;
        if (args.length > 0) {
            fullCommand += " " + String.join(" ", args);
        }
        
        // Forward the command to Radium
        radiumClient.forwardCommandToProxy(player.getUsername(), fullCommand)
                .thenAccept(success -> {
                    if (success) {
                        player.sendMessage(Component.text("Command forwarded to Radium proxy...")
                                .color(NamedTextColor.GRAY));
                    } else {
                        player.sendMessage(Component.text("Failed to forward command to Radium!")
                                .color(NamedTextColor.RED));
                    }
                });
    }
}
