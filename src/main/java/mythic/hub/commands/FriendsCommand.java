package mythic.hub.commands;

import mythic.hub.MythicHubServer;
import mythic.hub.managers.PlayerDataManager;
import mythic.hub.data.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.ArgumentWord;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import net.minestom.server.MinecraftServer;

import java.util.List;
import java.util.UUID;

public class FriendsCommand extends Command {
    private static final TextColor LIGHT_PINK = TextColor.color(255, 182, 193);
    private static final TextColor GREEN = NamedTextColor.GREEN;
    private static final TextColor RED = NamedTextColor.RED;
    private static final TextColor GRAY = NamedTextColor.GRAY;
    private static final TextColor YELLOW = NamedTextColor.YELLOW;

    public FriendsCommand() {
        super("friend", "friends");

        setDefaultExecutor(this::handleFriendsHelp);

        // /friend list
        addSubcommand(new Command("list") {{
            setDefaultExecutor(FriendsCommand.this::handleFriendsList);
        }});

        // /friend <username>
        ArgumentWord usernameArgument = ArgumentType.Word("username");
        addSyntax(this::handleFriendAdd, usernameArgument);
    }

    private void handleFriendsList(CommandSender sender, CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(RED));
            return;
        }

        PlayerDataManager dataManager = MythicHubServer.getInstance().getPlayerDataManager();
        PlayerProfile profile = dataManager.getPlayer(player);

        if (profile == null) {
            player.sendMessage(Component.text("Could not load your profile. Please try again.")
                    .color(RED));
            return;
        }

        // Get friends list from profile
        List<UUID> friends = profile.getFriends();
        
        if (friends.isEmpty()) {
            player.sendMessage(Component.text("You don't have any friends yet!")
                    .color(YELLOW));
            player.sendMessage(Component.text("Use ")
                    .color(GRAY)
                    .append(Component.text("/friend <username>")
                            .color(LIGHT_PINK))
                    .append(Component.text(" to add a friend!")
                            .color(GRAY)));
            return;
        }

        player.sendMessage(Component.text("═══════════════════")
                .color(LIGHT_PINK));
        player.sendMessage(Component.text("Your Friends (" + friends.size() + ")")
                .color(LIGHT_PINK)
                .decoration(TextDecoration.BOLD, true));
        player.sendMessage(Component.text("═══════════════════")
                .color(LIGHT_PINK));

        // Display each friend with online status
        for (UUID friendUuid : friends) {
            // Try to get friend's current name and online status
            Player onlineFriend = getPlayerByUuid(friendUuid);
            
            if (onlineFriend != null) {
                // Friend is online
                player.sendMessage(Component.text("● ")
                        .color(GREEN)
                        .append(Component.text(onlineFriend.getUsername())
                                .color(NamedTextColor.WHITE))
                        .append(Component.text(" (Online)")
                                .color(GREEN)));
            } else {
                // Friend is offline - get their stored username
                dataManager.loadPlayer(friendUuid, "Unknown").thenAccept(friendProfile -> {
                    if (friendProfile != null) {
                        player.sendMessage(Component.text("● ")
                                .color(GRAY)
                                .append(Component.text(friendProfile.getUsername())
                                        .color(NamedTextColor.WHITE))
                                .append(Component.text(" (Offline)")
                                        .color(GRAY)));
                    }
                });
            }
        }
        
        player.sendMessage(Component.text("═══════════════════")
                .color(LIGHT_PINK));
    }

    private void handleFriendAdd(CommandSender sender, CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(RED));
            return;
        }

        String targetUsername = context.get("username");
        
        if (targetUsername.equalsIgnoreCase(player.getUsername())) {
            player.sendMessage(Component.text("You cannot add yourself as a friend!")
                    .color(RED));
            return;
        }

        PlayerDataManager dataManager = MythicHubServer.getInstance().getPlayerDataManager();
        PlayerProfile senderProfile = dataManager.getPlayer(player);

        if (senderProfile == null) {
            player.sendMessage(Component.text("Could not load your profile. Please try again.")
                    .color(RED));
            return;
        }

        // Check if player is online
        Player targetPlayer = getPlayerByUsername(targetUsername);
        
        if (targetPlayer != null) {
            // Player is online
            handleOnlineFriendAdd(player, targetPlayer, senderProfile, dataManager);
        } else {
            // Player is offline - try to load their profile
            player.sendMessage(Component.text("Looking up player...")
                    .color(YELLOW));
            
            // This is a simplified approach - you might want to implement a UUID lookup service
            player.sendMessage(Component.text("Player '" + targetUsername + "' is not online. They must be online to receive friend requests.")
                    .color(RED));
        }
    }

    private void handleOnlineFriendAdd(Player sender, Player target, PlayerProfile senderProfile, PlayerDataManager dataManager) {
        PlayerProfile targetProfile = dataManager.getPlayer(target);
        
        if (targetProfile == null) {
            sender.sendMessage(Component.text("Could not load target player's profile.")
                    .color(RED));
            return;
        }

        // Check if already friends
        if (senderProfile.getFriends().contains(target.getUuid())) {
            sender.sendMessage(Component.text("You are already friends with " + target.getUsername() + "!")
                    .color(YELLOW));
            return;
        }

        // Check if already sent a request
        if (targetProfile.hasPendingFriendRequest(sender.getUuid())) {
            sender.sendMessage(Component.text("You have already sent a friend request to " + target.getUsername() + "!")
                    .color(YELLOW));
            return;
        }

        // Check if target has sent a request to sender (mutual request)
        if (senderProfile.hasPendingFriendRequest(target.getUuid())) {
            // Accept the mutual request
            senderProfile.addFriend(target.getUuid());
            targetProfile.addFriend(sender.getUuid());
            senderProfile.removeFriendRequest(target.getUuid());
            
            // Save profiles
            dataManager.getRedisManager().savePlayerProfile(senderProfile);
            dataManager.getRedisManager().savePlayerProfile(targetProfile);
            
            // Notify both players
            sender.sendMessage(Component.text("You are now friends with ")
                    .color(GREEN)
                    .append(Component.text(target.getUsername())
                            .color(LIGHT_PINK))
                    .append(Component.text("!")
                            .color(GREEN)));
            
            target.sendMessage(Component.text("You are now friends with ")
                    .color(GREEN)
                    .append(Component.text(sender.getUsername())
                            .color(LIGHT_PINK))
                    .append(Component.text("!")
                            .color(GREEN)));
            return;
        }

        // Send friend request
        targetProfile.addFriendRequest(sender.getUuid());
        dataManager.getRedisManager().savePlayerProfile(targetProfile);

        // Notify sender
        sender.sendMessage(Component.text("Friend request sent to ")
                .color(GREEN)
                .append(Component.text(target.getUsername())
                        .color(LIGHT_PINK))
                .append(Component.text("!")
                        .color(GREEN)));

        // Notify target
        target.sendMessage(Component.text("═══════════════════")
                .color(LIGHT_PINK));
        target.sendMessage(Component.text("Friend Request")
                .color(LIGHT_PINK)
                .decoration(TextDecoration.BOLD, true));
        target.sendMessage(Component.text(sender.getUsername() + " wants to be your friend!")
                .color(NamedTextColor.WHITE));
        target.sendMessage(Component.text("Use ")
                .color(GRAY)
                .append(Component.text("/friend " + sender.getUsername())
                        .color(LIGHT_PINK))
                .append(Component.text(" to accept!")
                        .color(GRAY)));
        target.sendMessage(Component.text("═══════════════════")
                .color(LIGHT_PINK));
    }

    private void handleFriendsHelp(CommandSender sender, CommandContext context) {
        sender.sendMessage(Component.text("═══════════════════")
                .color(LIGHT_PINK));
        sender.sendMessage(Component.text("Friends Commands")
                .color(LIGHT_PINK)
                .decoration(TextDecoration.BOLD, true));
        sender.sendMessage(Component.text("═══════════════════")
                .color(LIGHT_PINK));
        sender.sendMessage(Component.text("/friend <username>")
                .color(LIGHT_PINK)
                .append(Component.text(" - Send a friend request")
                        .color(GRAY)));
        sender.sendMessage(Component.text("/friend list")
                .color(LIGHT_PINK)
                .append(Component.text(" - View your friends list")
                        .color(GRAY)));
        sender.sendMessage(Component.text("═══════════════════")
                .color(LIGHT_PINK));
    }

    // Helper method to find a player by UUID
    private Player getPlayerByUuid(UUID uuid) {
        for (Player onlinePlayer : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (onlinePlayer.getUuid().equals(uuid)) {
                return onlinePlayer;
            }
        }
        return null;
    }

    // Helper method to find a player by username
    private Player getPlayerByUsername(String username) {
        for (Player onlinePlayer : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (onlinePlayer.getUsername().equalsIgnoreCase(username)) {
                return onlinePlayer;
            }
        }
        return null;
    }
}