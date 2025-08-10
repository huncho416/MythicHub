package mythic.hub.commands;

import mythic.hub.MythicHubServer;
import mythic.hub.integrations.radium.RadiumClient;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
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

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FriendsCommand extends Command {
    private static final TextColor LIGHT_PINK = TextColor.color(255, 182, 193);
    private static final TextColor GREEN = NamedTextColor.GREEN;
    private static final TextColor RED = NamedTextColor.RED;
    private static final TextColor GRAY = NamedTextColor.GRAY;
    private static final TextColor YELLOW = NamedTextColor.YELLOW;

    private final RadiumClient radiumClient;

    public FriendsCommand() {
        super("friend", "friends");
        this.radiumClient = MythicHubServer.getInstance().getRadiumClient();

        setDefaultExecutor(this::handleFriendsHelp);

        // /friend list
        addSubcommand(new Command("list") {{
            setDefaultExecutor(FriendsCommand.this::handleFriendsList);
        }});

        // /friend requests
        addSubcommand(new Command("requests") {{
            setDefaultExecutor(FriendsCommand.this::handleFriendRequests);
        }});

        // /friend add <username>
        ArgumentWord addUsernameArgument = ArgumentType.Word("username");
        addSubcommand(new Command("add") {{
            addSyntax(FriendsCommand.this::handleFriendAdd, addUsernameArgument);
        }});

        // /friend remove <username>
        ArgumentWord removeUsernameArgument = ArgumentType.Word("username");
        addSubcommand(new Command("remove") {{
            addSyntax(FriendsCommand.this::handleFriendRemove, removeUsernameArgument);
        }});

        // /friend deny <username>
        ArgumentWord denyUsernameArgument = ArgumentType.Word("username");
        addSubcommand(new Command("deny") {{
            addSyntax(FriendsCommand.this::handleFriendDeny, denyUsernameArgument);
        }});

        // /friend <username> (shortcut for add)
        ArgumentWord usernameArgument = ArgumentType.Word("username");
        addSyntax(this::handleFriendAdd, usernameArgument);
    }

    private void handleFriendsList(CommandSender sender, CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(RED));
            return;
        }

        player.sendMessage(Component.text("Loading your friends list...").color(YELLOW));

        radiumClient.getFriends(player.getUuid()).thenAccept(friends -> {
            if (friends.isEmpty()) {
                player.sendMessage(Component.text("You don't have any friends yet!")
                        .color(YELLOW));
                player.sendMessage(Component.text("Use ")
                        .color(GRAY)
                        .append(Component.text("/friend add <username>")
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
                    // Friend is offline - get their stored username and last seen
                    radiumClient.getPlayerName(friendUuid).thenAccept(friendName -> {
                        if (friendName != null) {
                            radiumClient.getFriendLastSeen(player.getUuid(), friendUuid).thenAccept(lastSeen -> {
                                String lastSeenText = "";
                                if (lastSeen != null) {
                                    lastSeenText = " (Last seen: " + formatTimeSince(lastSeen) + ")";
                                } else {
                                    lastSeenText = " (Offline)";
                                }
                                
                                player.sendMessage(Component.text("● ")
                                        .color(GRAY)
                                        .append(Component.text(friendName)
                                                .color(NamedTextColor.WHITE))
                                        .append(Component.text(lastSeenText)
                                                .color(GRAY)));
                            });
                        }
                    });
                }
            }
            
            player.sendMessage(Component.text("═══════════════════")
                    .color(LIGHT_PINK));
        }).exceptionally(throwable -> {
            player.sendMessage(Component.text("Failed to load friends list. Please try again.")
                    .color(RED));
            return null;
        });
    }

    private void handleFriendRequests(CommandSender sender, CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(RED));
            return;
        }

        player.sendMessage(Component.text("Loading your friend requests...").color(YELLOW));

        CompletableFuture<Set<UUID>> incomingFuture = radiumClient.getIncomingFriendRequests(player.getUuid());
        CompletableFuture<Set<UUID>> outgoingFuture = radiumClient.getOutgoingFriendRequests(player.getUuid());

        CompletableFuture.allOf(incomingFuture, outgoingFuture).thenRun(() -> {
            Set<UUID> incomingRequests = incomingFuture.join();
            Set<UUID> outgoingRequests = outgoingFuture.join();

            player.sendMessage(Component.text("═══════════════════")
                    .color(LIGHT_PINK));
            player.sendMessage(Component.text("Friend Requests")
                    .color(LIGHT_PINK)
                    .decoration(TextDecoration.BOLD, true));
            player.sendMessage(Component.text("═══════════════════")
                    .color(LIGHT_PINK));

            if (incomingRequests.isEmpty() && outgoingRequests.isEmpty()) {
                player.sendMessage(Component.text("You have no pending friend requests.")
                        .color(YELLOW));
            } else {
                if (!incomingRequests.isEmpty()) {
                    player.sendMessage(Component.text("Incoming Requests:")
                            .color(GREEN)
                            .decoration(TextDecoration.BOLD, true));
                    
                    for (UUID requesterUuid : incomingRequests) {
                        radiumClient.getPlayerName(requesterUuid).thenAccept(requesterName -> {
                            if (requesterName != null) {
                                Component acceptButton = Component.text("[Accept]")
                                        .color(GREEN)
                                        .clickEvent(ClickEvent.runCommand("/friend add " + requesterName))
                                        .hoverEvent(HoverEvent.showText(Component.text("Click to accept")));
                                
                                Component denyButton = Component.text("[Deny]")
                                        .color(RED)
                                        .clickEvent(ClickEvent.runCommand("/friend deny " + requesterName))
                                        .hoverEvent(HoverEvent.showText(Component.text("Click to deny")));

                                player.sendMessage(Component.text("• ")
                                        .color(YELLOW)
                                        .append(Component.text(requesterName)
                                                .color(NamedTextColor.WHITE))
                                        .append(Component.text(" "))
                                        .append(acceptButton)
                                        .append(Component.text(" "))
                                        .append(denyButton));
                            }
                        });
                    }
                }

                if (!outgoingRequests.isEmpty()) {
                    player.sendMessage(Component.text("Outgoing Requests:")
                            .color(YELLOW)
                            .decoration(TextDecoration.BOLD, true));
                    
                    for (UUID targetUuid : outgoingRequests) {
                        radiumClient.getPlayerName(targetUuid).thenAccept(targetName -> {
                            if (targetName != null) {
                                Component cancelButton = Component.text("[Cancel]")
                                        .color(RED)
                                        .clickEvent(ClickEvent.runCommand("/friend remove " + targetName))
                                        .hoverEvent(HoverEvent.showText(Component.text("Click to cancel")));

                                player.sendMessage(Component.text("• ")
                                        .color(GRAY)
                                        .append(Component.text(targetName)
                                                .color(NamedTextColor.WHITE))
                                        .append(Component.text(" "))
                                        .append(cancelButton));
                            }
                        });
                    }
                }
            }
            
            player.sendMessage(Component.text("═══════════════════")
                    .color(LIGHT_PINK));
        }).exceptionally(throwable -> {
            player.sendMessage(Component.text("Failed to load friend requests. Please try again.")
                    .color(RED));
            return null;
        });
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

        player.sendMessage(Component.text("Sending friend request to " + targetUsername + "...")
                .color(YELLOW));

        radiumClient.sendFriendRequest(player.getUsername(), targetUsername).thenAccept(success -> {
            if (success) {
                player.sendMessage(Component.text("Friend request sent to ")
                        .color(GREEN)
                        .append(Component.text(targetUsername)
                                .color(LIGHT_PINK))
                        .append(Component.text("!")
                                .color(GREEN)));
            } else {
                player.sendMessage(Component.text("Failed to send friend request. Please check the username and try again.")
                        .color(RED));
            }
        }).exceptionally(throwable -> {
            player.sendMessage(Component.text("An error occurred while sending the friend request.")
                    .color(RED));
            return null;
        });
    }

    private void handleFriendRemove(CommandSender sender, CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(RED));
            return;
        }

        String targetUsername = context.get("username");

        player.sendMessage(Component.text("Removing friend " + targetUsername + "...")
                .color(YELLOW));

        radiumClient.removeFriend(player.getUsername(), targetUsername).thenAccept(success -> {
            if (success) {
                player.sendMessage(Component.text("Successfully removed ")
                        .color(GREEN)
                        .append(Component.text(targetUsername)
                                .color(LIGHT_PINK))
                        .append(Component.text(" from your friends list.")
                                .color(GREEN)));
            } else {
                player.sendMessage(Component.text("Failed to remove friend. Please check the username and try again.")
                        .color(RED));
            }
        }).exceptionally(throwable -> {
            player.sendMessage(Component.text("An error occurred while removing the friend.")
                    .color(RED));
            return null;
        });
    }

    private void handleFriendDeny(CommandSender sender, CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(RED));
            return;
        }

        String targetUsername = context.get("username");

        player.sendMessage(Component.text("Denying friend request from " + targetUsername + "...")
                .color(YELLOW));

        radiumClient.denyFriendRequest(player.getUsername(), targetUsername).thenAccept(success -> {
            if (success) {
                player.sendMessage(Component.text("Successfully denied friend request from ")
                        .color(GREEN)
                        .append(Component.text(targetUsername)
                                .color(LIGHT_PINK))
                        .append(Component.text(".")
                                .color(GREEN)));
            } else {
                player.sendMessage(Component.text("Failed to deny friend request. Please check the username and try again.")
                        .color(RED));
            }
        }).exceptionally(throwable -> {
            player.sendMessage(Component.text("An error occurred while denying the friend request.")
                    .color(RED));
            return null;
        });
    }

    private void handleFriendsHelp(CommandSender sender, CommandContext context) {
        sender.sendMessage(Component.text("═══════════════════")
                .color(LIGHT_PINK));
        sender.sendMessage(Component.text("Friends Commands")
                .color(LIGHT_PINK)
                .decoration(TextDecoration.BOLD, true));
        sender.sendMessage(Component.text("═══════════════════")
                .color(LIGHT_PINK));
        sender.sendMessage(Component.text("/friend add <username>")
                .color(LIGHT_PINK)
                .append(Component.text(" - Send a friend request")
                        .color(GRAY)));
        sender.sendMessage(Component.text("/friend remove <username>")
                .color(LIGHT_PINK)
                .append(Component.text(" - Remove a friend")
                        .color(GRAY)));
        sender.sendMessage(Component.text("/friend deny <username>")
                .color(LIGHT_PINK)
                .append(Component.text(" - Deny a friend request")
                        .color(GRAY)));
        sender.sendMessage(Component.text("/friend list")
                .color(LIGHT_PINK)
                .append(Component.text(" - View your friends list")
                        .color(GRAY)));
        sender.sendMessage(Component.text("/friend requests")
                .color(LIGHT_PINK)
                .append(Component.text(" - View pending requests")
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

    // Helper method to format time since last seen
    private String formatTimeSince(Instant lastSeen) {
        Duration duration = Duration.between(lastSeen, Instant.now());
        
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        
        if (days > 0) {
            return days + "d " + hours + "h ago";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m ago";
        } else if (minutes > 0) {
            return minutes + "m ago";
        } else {
            return "Just now";
        }
    }
}