package mythic.hub.commands;

import mythic.hub.MythicHubServer;
import mythic.hub.integrations.radium.RadiumClient;
import mythic.hub.integrations.radium.RadiumProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.entity.Player;

import java.util.UUID;

public class RankCommand extends Command {

    public RankCommand() {
        super("rank");

        var playerArg = ArgumentType.Entity("player").singleEntity(true).onlyPlayers(true);
        var rankArg = ArgumentType.Word("rank");
        var actionArg = ArgumentType.Word("action").from("set", "add", "remove", "list", "info");

        // /rank set <player> <rank>
        addSyntax((sender, context) -> {
            if (!(sender instanceof Player senderPlayer)) {
                sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
                return;
            }

            if (!hasPermission(senderPlayer, "mythic.rank.set")) {
                senderPlayer.sendMessage(Component.text("You don't have permission to set ranks!").color(NamedTextColor.RED));
                return;
            }

            Player target = (Player) context.get(playerArg).findFirstPlayer(sender);
            String rank = context.get(rankArg);

            if (target == null) {
                senderPlayer.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
                return;
            }

            setPlayerRank(senderPlayer, target, rank);

        }, actionArg, playerArg, rankArg);

        // /rank add <player> <rank>
        addSyntax((sender, context) -> {
            if (!(sender instanceof Player senderPlayer)) {
                sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
                return;
            }

            if (!hasPermission(senderPlayer, "mythic.rank.add")) {
                senderPlayer.sendMessage(Component.text("You don't have permission to add ranks!").color(NamedTextColor.RED));
                return;
            }

            Player target = (Player) context.get(playerArg).findFirstPlayer(sender);
            String rank = context.get(rankArg);

            if (target == null) {
                senderPlayer.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
                return;
            }

            addPlayerRank(senderPlayer, target, rank);

        }, ArgumentType.Literal("add"), playerArg, rankArg);

        // /rank remove <player> <rank>
        addSyntax((sender, context) -> {
            if (!(sender instanceof Player senderPlayer)) {
                sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
                return;
            }

            if (!hasPermission(senderPlayer, "mythic.rank.remove")) {
                senderPlayer.sendMessage(Component.text("You don't have permission to remove ranks!").color(NamedTextColor.RED));
                return;
            }

            Player target = (Player) context.get(playerArg).findFirstPlayer(sender);
            String rank = context.get(rankArg);

            if (target == null) {
                senderPlayer.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
                return;
            }

            removePlayerRank(senderPlayer, target, rank);

        }, ArgumentType.Literal("remove"), playerArg, rankArg);

        // /rank list <player>
        addSyntax((sender, context) -> {
            if (!(sender instanceof Player senderPlayer)) {
                sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
                return;
            }

            if (!hasPermission(senderPlayer, "mythic.rank.list")) {
                senderPlayer.sendMessage(Component.text("You don't have permission to list ranks!").color(NamedTextColor.RED));
                return;
            }

            Player target = (Player) context.get(playerArg).findFirstPlayer(sender);

            if (target == null) {
                senderPlayer.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
                return;
            }

            listPlayerRanks(senderPlayer, target);

        }, ArgumentType.Literal("list"), playerArg);

        // /rank info <rank>
        addSyntax((sender, context) -> {
            if (!(sender instanceof Player senderPlayer)) {
                sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
                return;
            }

            if (!hasPermission(senderPlayer, "mythic.rank.info")) {
                senderPlayer.sendMessage(Component.text("You don't have permission to view rank info!").color(NamedTextColor.RED));
                return;
            }

            String rank = context.get(rankArg);
            showRankInfo(senderPlayer, rank);

        }, ArgumentType.Literal("info"), rankArg);
    }

    private void setPlayerRank(Player sender, Player target, String rankName) {
        try {
            RadiumClient radiumClient = MythicHubServer.getInstance().getRadiumClient();
            
            // First check if the rank exists
            if (radiumClient.getRank(rankName) == null) {
                sender.sendMessage(Component.text("Rank '" + rankName + "' does not exist!").color(NamedTextColor.RED));
                return;
            }

            // Update Radium via Redis (this would need to be implemented based on Radium's API)
            // For now, we'll use a placeholder method
            updatePlayerRankInRadium(target.getUuid(), rankName, "set");
            
            sender.sendMessage(Component.text("Set " + target.getUsername() + "'s rank to " + rankName).color(NamedTextColor.GREEN));
            target.sendMessage(Component.text("Your rank has been set to " + rankName).color(NamedTextColor.GREEN));
            
            // Update tablist for all players
            MythicHubServer.getInstance().getTabListManager().updateAllTabLists();
            
        } catch (Exception e) {
            sender.sendMessage(Component.text("Error setting rank: " + e.getMessage()).color(NamedTextColor.RED));
            System.err.println("Error setting rank for " + target.getUsername() + ": " + e.getMessage());
        }
    }

    private void addPlayerRank(Player sender, Player target, String rankName) {
        try {
            RadiumClient radiumClient = MythicHubServer.getInstance().getRadiumClient();
            
            // Check if rank exists
            if (radiumClient.getRank(rankName) == null) {
                sender.sendMessage(Component.text("Rank '" + rankName + "' does not exist!").color(NamedTextColor.RED));
                return;
            }

            updatePlayerRankInRadium(target.getUuid(), rankName, "add");
            
            sender.sendMessage(Component.text("Added rank " + rankName + " to " + target.getUsername()).color(NamedTextColor.GREEN));
            target.sendMessage(Component.text("You have been granted the " + rankName + " rank").color(NamedTextColor.GREEN));
            
            MythicHubServer.getInstance().getTabListManager().updateAllTabLists();
            
        } catch (Exception e) {
            sender.sendMessage(Component.text("Error adding rank: " + e.getMessage()).color(NamedTextColor.RED));
            System.err.println("Error adding rank for " + target.getUsername() + ": " + e.getMessage());
        }
    }

    private void removePlayerRank(Player sender, Player target, String rankName) {
        try {
            updatePlayerRankInRadium(target.getUuid(), rankName, "remove");
            
            sender.sendMessage(Component.text("Removed rank " + rankName + " from " + target.getUsername()).color(NamedTextColor.GREEN));
            target.sendMessage(Component.text("The " + rankName + " rank has been removed").color(NamedTextColor.YELLOW));
            
            MythicHubServer.getInstance().getTabListManager().updateAllTabLists();
            
        } catch (Exception e) {
            sender.sendMessage(Component.text("Error removing rank: " + e.getMessage()).color(NamedTextColor.RED));
            System.err.println("Error removing rank for " + target.getUsername() + ": " + e.getMessage());
        }
    }

    private void listPlayerRanks(Player sender, Player target) {
        try {
            RadiumClient radiumClient = MythicHubServer.getInstance().getRadiumClient();
            RadiumProfile profile = radiumClient.getPlayerProfile(target.getUuid()).get();
            
            sender.sendMessage(Component.text("Ranks for " + target.getUsername() + ":").color(NamedTextColor.YELLOW));
            
            if (profile.getRanks().isEmpty()) {
                sender.sendMessage(Component.text("  No ranks assigned").color(NamedTextColor.GRAY));
            } else {
                for (String rank : profile.getRanks()) {
                    sender.sendMessage(Component.text("  - " + rank).color(NamedTextColor.WHITE));
                }
            }
            
        } catch (Exception e) {
            sender.sendMessage(Component.text("Error listing ranks: " + e.getMessage()).color(NamedTextColor.RED));
            System.err.println("Error listing ranks for " + target.getUsername() + ": " + e.getMessage());
        }
    }

    private void showRankInfo(Player sender, String rankName) {
        try {
            RadiumClient radiumClient = MythicHubServer.getInstance().getRadiumClient();
            var rank = radiumClient.getRank(rankName);
            
            if (rank == null) {
                sender.sendMessage(Component.text("Rank '" + rankName + "' does not exist!").color(NamedTextColor.RED));
                return;
            }
            
            sender.sendMessage(Component.text("Rank Information for " + rank.getName() + ":").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("  Prefix: " + rank.getPrefix()).color(NamedTextColor.WHITE));
            sender.sendMessage(Component.text("  Color: " + rank.getColor()).color(NamedTextColor.WHITE));
            sender.sendMessage(Component.text("  Weight: " + rank.getWeight()).color(NamedTextColor.WHITE));
            sender.sendMessage(Component.text("  Permissions: " + rank.getPermissions().size()).color(NamedTextColor.WHITE));
            
        } catch (Exception e) {
            sender.sendMessage(Component.text("Error showing rank info: " + e.getMessage()).color(NamedTextColor.RED));
            System.err.println("Error showing rank info for " + rankName + ": " + e.getMessage());
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
    private void updatePlayerRankInRadium(UUID playerUuid, String rankName, String action) {
        // This is a placeholder - you would need to implement the actual Radium API calls
        // For example, this might involve:
        // 1. Making HTTP requests to Radium's REST API
        // 2. Writing to Redis in the format Radium expects
        // 3. Calling Radium's internal methods if running in the same JVM
        
        System.out.println("TODO: Implement Radium rank update - " + action + " rank " + rankName + " for player " + playerUuid);
        
        // For now, we'll just invalidate the cache to force a refresh
        MythicHubServer.getInstance().getRadiumClient().clearPlayerCache(playerUuid);
    }
}
