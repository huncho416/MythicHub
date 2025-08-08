package mythic.hub.commands;

import mythic.hub.MythicHubServer;
import mythic.hub.integrations.radium.RadiumClient;
import mythic.hub.integrations.radium.RadiumProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

import java.util.UUID;

public class PermissionCommand extends Command {

    public PermissionCommand() {
        super("perm", "permission");

        var playerArg = ArgumentType.Entity("player").singleEntity(true).onlyPlayers(true);
        var permissionArg = ArgumentType.StringArray("permission");
        var actionArg = ArgumentType.Word("action").from("grant", "revoke", "check", "list");

        // /perm grant <player> <permission>
        addSyntax((sender, context) -> {
            if (!(sender instanceof Player senderPlayer)) {
                sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
                return;
            }

            if (!hasPermission(senderPlayer, "mythic.permission.grant")) {
                senderPlayer.sendMessage(Component.text("You don't have permission to grant permissions!").color(NamedTextColor.RED));
                return;
            }

            Player target = (Player) context.get(playerArg).findFirstPlayer(sender);
            String[] permissionParts = context.get(permissionArg);
            String permission = String.join(" ", permissionParts);

            if (target == null) {
                senderPlayer.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
                return;
            }

            grantPermission(senderPlayer, target, permission);

        }, actionArg, playerArg, permissionArg);

        // /perm revoke <player> <permission>
        addSyntax((sender, context) -> {
            if (!(sender instanceof Player senderPlayer)) {
                sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
                return;
            }

            if (!hasPermission(senderPlayer, "mythic.permission.revoke")) {
                senderPlayer.sendMessage(Component.text("You don't have permission to revoke permissions!").color(NamedTextColor.RED));
                return;
            }

            Player target = (Player) context.get(playerArg).findFirstPlayer(sender);
            String[] permissionParts = context.get(permissionArg);
            String permission = String.join(" ", permissionParts);

            if (target == null) {
                senderPlayer.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
                return;
            }

            revokePermission(senderPlayer, target, permission);

        }, ArgumentType.Literal("revoke"), playerArg, permissionArg);

        // /perm check <player> <permission>
        addSyntax((sender, context) -> {
            if (!(sender instanceof Player senderPlayer)) {
                sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
                return;
            }

            if (!hasPermission(senderPlayer, "mythic.permission.check")) {
                senderPlayer.sendMessage(Component.text("You don't have permission to check permissions!").color(NamedTextColor.RED));
                return;
            }

            Player target = (Player) context.get(playerArg).findFirstPlayer(sender);
            String[] permissionParts = context.get(permissionArg);
            String permission = String.join(" ", permissionParts);

            if (target == null) {
                senderPlayer.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
                return;
            }

            checkPermission(senderPlayer, target, permission);

        }, ArgumentType.Literal("check"), playerArg, permissionArg);

        // /perm list <player>
        addSyntax((sender, context) -> {
            if (!(sender instanceof Player senderPlayer)) {
                sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
                return;
            }

            if (!hasPermission(senderPlayer, "mythic.permission.list")) {
                senderPlayer.sendMessage(Component.text("You don't have permission to list permissions!").color(NamedTextColor.RED));
                return;
            }

            Player target = (Player) context.get(playerArg).findFirstPlayer(sender);

            if (target == null) {
                senderPlayer.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
                return;
            }

            listPermissions(senderPlayer, target);

        }, ArgumentType.Literal("list"), playerArg);
    }

    private void grantPermission(Player sender, Player target, String permission) {
        try {
            // Update permission in Radium (placeholder)
            updatePlayerPermissionInRadium(target.getUuid(), permission, true);
            
            sender.sendMessage(Component.text("Granted permission '" + permission + "' to " + target.getUsername()).color(NamedTextColor.GREEN));
            target.sendMessage(Component.text("You have been granted the permission: " + permission).color(NamedTextColor.GREEN));
            
        } catch (Exception e) {
            sender.sendMessage(Component.text("Error granting permission: " + e.getMessage()).color(NamedTextColor.RED));
            System.err.println("Error granting permission to " + target.getUsername() + ": " + e.getMessage());
        }
    }

    private void revokePermission(Player sender, Player target, String permission) {
        try {
            // Update permission in Radium (placeholder)
            updatePlayerPermissionInRadium(target.getUuid(), permission, false);
            
            sender.sendMessage(Component.text("Revoked permission '" + permission + "' from " + target.getUsername()).color(NamedTextColor.GREEN));
            target.sendMessage(Component.text("The permission '" + permission + "' has been revoked").color(NamedTextColor.YELLOW));
            
        } catch (Exception e) {
            sender.sendMessage(Component.text("Error revoking permission: " + e.getMessage()).color(NamedTextColor.RED));
            System.err.println("Error revoking permission from " + target.getUsername() + ": " + e.getMessage());
        }
    }

    private void checkPermission(Player sender, Player target, String permission) {
        try {
            RadiumClient radiumClient = MythicHubServer.getInstance().getRadiumClient();
            boolean hasPermission = radiumClient.hasPermission(target.getUuid(), permission).get();
            
            Component message = Component.text(target.getUsername() + " ")
                    .append(Component.text(hasPermission ? "HAS" : "DOES NOT HAVE").color(hasPermission ? NamedTextColor.GREEN : NamedTextColor.RED))
                    .append(Component.text(" permission: " + permission).color(NamedTextColor.WHITE));
            
            sender.sendMessage(message);
            
        } catch (Exception e) {
            sender.sendMessage(Component.text("Error checking permission: " + e.getMessage()).color(NamedTextColor.RED));
            System.err.println("Error checking permission for " + target.getUsername() + ": " + e.getMessage());
        }
    }

    private void listPermissions(Player sender, Player target) {
        try {
            RadiumClient radiumClient = MythicHubServer.getInstance().getRadiumClient();
            RadiumProfile profile = radiumClient.getPlayerProfile(target.getUuid()).get();
            
            sender.sendMessage(Component.text("Permissions for " + target.getUsername() + ":").color(NamedTextColor.YELLOW));
            
            if (profile.getPermissions().isEmpty()) {
                sender.sendMessage(Component.text("  No direct permissions assigned").color(NamedTextColor.GRAY));
            } else {
                profile.getPermissions().forEach((perm, value) -> {
                    Component permComponent = Component.text("  " + (value ? "+" : "-") + perm)
                            .color(value ? NamedTextColor.GREEN : NamedTextColor.RED);
                    sender.sendMessage(permComponent);
                });
            }
            
            // Also show rank permissions
            sender.sendMessage(Component.text("Rank permissions:").color(NamedTextColor.YELLOW));
            for (String rankName : profile.getRanks()) {
                var rank = radiumClient.getRank(rankName);
                if (rank != null && !rank.getPermissions().isEmpty()) {
                    sender.sendMessage(Component.text("  From " + rankName + ":").color(NamedTextColor.AQUA));
                    for (String perm : rank.getPermissions()) {
                        sender.sendMessage(Component.text("    +" + perm).color(NamedTextColor.GREEN));
                    }
                }
            }
            
        } catch (Exception e) {
            sender.sendMessage(Component.text("Error listing permissions: " + e.getMessage()).color(NamedTextColor.RED));
            System.err.println("Error listing permissions for " + target.getUsername() + ": " + e.getMessage());
        }
    }

    private boolean hasPermission(Player player, String permission) {
        try {
            return MythicHubServer.getInstance().getRadiumClient()
                    .hasPermission(player.getUuid(), permission)
                    .get();
        } catch (Exception e) {
            System.err.println("Error checking permission " + permission + " for " + player.getUsername() + ": " + e.getMessage());
            return false;
        }
    }

    // Placeholder method - this would need to interface with Radium's actual API
    private void updatePlayerPermissionInRadium(UUID playerUuid, String permission, boolean grant) {
        // This is a placeholder - you would need to implement the actual Radium API calls
        System.out.println("TODO: Implement Radium permission update - " + (grant ? "grant" : "revoke") + " permission " + permission + " for player " + playerUuid);
        
        // For now, we'll just invalidate the cache to force a refresh
        MythicHubServer.getInstance().getRadiumClient().clearPlayerCache(playerUuid);
    }
}
