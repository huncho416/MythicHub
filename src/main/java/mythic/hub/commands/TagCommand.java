package mythic.hub.commands;

import mythic.hub.MythicHubServer;
import mythic.hub.managers.ChatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.entity.Player;

public class TagCommand extends Command {

    public TagCommand() {
        super("tag");

        var playerArg = ArgumentType.Entity("player").singleEntity(true).onlyPlayers(true);
        var tagArg = ArgumentType.String("tag");
        var actionArg = ArgumentType.Word("action").from("set", "remove", "clear");

        // /tag set <player> <tag>
        addSyntax((sender, context) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
                return;
            }

            Player senderPlayer = (Player) sender;
            Player target = (Player) context.get(playerArg).findFirstPlayer(sender);
            String tag = context.get(tagArg);

            if (target == null) {
                senderPlayer.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
                return;
            }

            // Check permission (you can implement your own permission system)
            if (!hasTagPermission(senderPlayer)) {
                senderPlayer.sendMessage(Component.text("You don't have permission to manage tags!").color(NamedTextColor.RED));
                return;
            }

            ChatManager chatManager = MythicHubServer.getInstance().getChatManager();
            chatManager.setPlayerTag(target, tag);

            senderPlayer.sendMessage(Component.text("Set tag for " + target.getUsername() + " to: " + tag).color(NamedTextColor.GREEN));
            target.sendMessage(Component.text("Your tag has been set to: " + tag).color(NamedTextColor.GREEN));

        }, actionArg, playerArg, tagArg);

        // /tag remove <player>
        addSyntax((sender, context) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
                return;
            }

            Player senderPlayer = (Player) sender;
            Player target = (Player) context.get(playerArg).findFirstPlayer(sender);

            if (target == null) {
                senderPlayer.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
                return;
            }

            if (!hasTagPermission(senderPlayer)) {
                senderPlayer.sendMessage(Component.text("You don't have permission to manage tags!").color(NamedTextColor.RED));
                return;
            }

            ChatManager chatManager = MythicHubServer.getInstance().getChatManager();
            chatManager.removePlayerTag(target);

            senderPlayer.sendMessage(Component.text("Removed tag from " + target.getUsername()).color(NamedTextColor.GREEN));
            target.sendMessage(Component.text("Your tag has been removed.").color(NamedTextColor.YELLOW));

        }, ArgumentType.Literal("remove"), playerArg);
    }

    private boolean hasTagPermission(Player player) {
        // Check if player has permission to manage tags
        // You can integrate this with your permission system
        return MythicHubServer.getInstance().getPlayerDataManager().hasPermission(player, "mythic.tag.manage") ||
                player.hasPermission("mythic.tag.manage");
    }
}