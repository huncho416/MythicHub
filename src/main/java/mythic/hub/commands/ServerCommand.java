package mythic.hub.commands;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.ArgumentWord;
import net.minestom.server.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import mythic.hub.MythicHubServer;
import mythic.hub.managers.ProxyManager;

public class ServerCommand extends Command {

    public ServerCommand() {
        super("server", "join", "play");

        ArgumentWord serverArgument = ArgumentType.Word("server");

        // Usage without arguments - show server list
        setDefaultExecutor((sender, context) -> {
            if (sender instanceof Player player) {
                showServerList(player);
            }
        });

        // Usage with server argument
        addSyntax((sender, context) -> {
            if (sender instanceof Player player) {
                String serverName = context.get(serverArgument);
                joinServer(player, serverName);
            }
        }, serverArgument);
    }

    private void showServerList(Player player) {
        player.sendMessage(Component.text("=== Available Servers ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("• ").color(NamedTextColor.GRAY)
                .append(Component.text("pvp").color(NamedTextColor.RED))
                .append(Component.text(" - PvP Arena Server").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("• ").color(NamedTextColor.GRAY)
                .append(Component.text("survival").color(NamedTextColor.GREEN))
                .append(Component.text(" - Survival World").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("• ").color(NamedTextColor.GRAY)
                .append(Component.text("creative").color(NamedTextColor.AQUA))
                .append(Component.text(" - Creative Building").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("• ").color(NamedTextColor.GRAY)
                .append(Component.text("skyblock").color(NamedTextColor.BLUE))
                .append(Component.text(" - Skyblock Islands").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("Usage: ").color(NamedTextColor.GRAY)
                .append(Component.text("/server <name>").color(NamedTextColor.YELLOW)));
    }

    private void joinServer(Player player, String serverName) {
        ProxyManager proxyManager = MythicHubServer.getInstance().getProxyManager();
        
        if (proxyManager == null) {
            player.sendMessage(Component.text("Proxy manager not available!").color(NamedTextColor.RED));
            return;
        }

        // Validate server name
        String normalizedName = serverName.toLowerCase();
        if (!isValidServer(normalizedName)) {
            player.sendMessage(Component.text("Unknown server: ").color(NamedTextColor.RED)
                    .append(Component.text(serverName).color(NamedTextColor.YELLOW)));
            player.sendMessage(Component.text("Use ").color(NamedTextColor.GRAY)
                    .append(Component.text("/server").color(NamedTextColor.YELLOW))
                    .append(Component.text(" to see available servers.").color(NamedTextColor.GRAY)));
            return;
        }

        // Send player to the requested server
        player.sendMessage(Component.text("Connecting to ").color(NamedTextColor.GREEN)
                .append(Component.text(normalizedName).color(NamedTextColor.YELLOW))
                .append(Component.text("...").color(NamedTextColor.GREEN)));
        
        proxyManager.sendPlayerToServer(player, normalizedName);
    }

    private boolean isValidServer(String serverName) {
        return switch (serverName) {
            case "pvp", "survival", "creative", "skyblock" -> true;
            default -> false;
        };
    }
}
