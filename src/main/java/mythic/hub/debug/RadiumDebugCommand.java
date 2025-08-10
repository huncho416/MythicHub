package mythic.hub.debug;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mythic.hub.MythicHubServer;
import mythic.hub.integrations.radium.RadiumClient;
import mythic.hub.integrations.radium.RadiumProfile;
import mythic.hub.integrations.radium.RadiumRank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

import java.util.Set;
import java.util.UUID;

/**
 * Debug command to inspect Radium data in Redis
 */
public class RadiumDebugCommand extends Command {
    
    public RadiumDebugCommand() {
        super("radiumdebug", "rd");
        
        ArgumentString subcommand = ArgumentType.String("subcommand");
        ArgumentString parameter = ArgumentType.String("parameter");
        
        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Only players can use this command").color(NamedTextColor.RED));
                return;
            }
            
            String sub = context.get(subcommand);
            RadiumClient radiumClient = MythicHubServer.getInstance().getRadiumClient();
            
            switch (sub.toLowerCase()) {
                case "profile" -> {
                    UUID uuid = player.getUuid();
                    if (context.has(parameter)) {
                        try {
                            uuid = UUID.fromString(context.get(parameter));
                        } catch (IllegalArgumentException e) {
                            player.sendMessage(Component.text("Invalid UUID format").color(NamedTextColor.RED));
                            return;
                        }
                    }
                    
                    radiumClient.getPlayerProfile(uuid).thenAccept(profile -> {
                        player.sendMessage(Component.text("=== Profile Debug ===").color(NamedTextColor.YELLOW));
                        player.sendMessage(Component.text("UUID: " + profile.getUuid()).color(NamedTextColor.WHITE));
                        player.sendMessage(Component.text("Username: " + profile.getUsername()).color(NamedTextColor.WHITE));
                        player.sendMessage(Component.text("Ranks: " + profile.getRanks()).color(NamedTextColor.WHITE));
                        player.sendMessage(Component.text("Permissions: " + profile.getPermissions().size()).color(NamedTextColor.WHITE));
                        
                        RadiumRank highest = profile.getHighestRank(radiumClient);
                        player.sendMessage(Component.text("Highest Rank: " + highest.getName()).color(NamedTextColor.GREEN));
                        player.sendMessage(Component.text("Prefix: '" + highest.getPrefix() + "'").color(NamedTextColor.GREEN));
                        player.sendMessage(Component.text("Color: '" + highest.getColor() + "'").color(NamedTextColor.GREEN));
                        player.sendMessage(Component.text("Weight: " + highest.getWeight()).color(NamedTextColor.GREEN));
                    });
                }
                
                case "rank" -> {
                    String rankName = context.has(parameter) ? context.get(parameter) : "Member";
                    
                    RadiumRank rank = radiumClient.getRank(rankName);
                    player.sendMessage(Component.text("=== Rank Debug ===").color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("Name: " + rank.getName()).color(NamedTextColor.WHITE));
                    player.sendMessage(Component.text("Prefix: '" + rank.getPrefix() + "'").color(NamedTextColor.WHITE));
                    player.sendMessage(Component.text("Color: '" + rank.getColor() + "'").color(NamedTextColor.WHITE));
                    player.sendMessage(Component.text("Weight: " + rank.getWeight()).color(NamedTextColor.WHITE));
                    player.sendMessage(Component.text("Permissions: " + rank.getPermissions().size()).color(NamedTextColor.WHITE));
                }
                
                case "keys" -> {
                    try {
                        Set<String> profileKeys = MythicHubServer.getInstance().getPlayerDataManager().getRedisManager().getKeys("radium:profile:*");
                        Set<String> rankKeys = MythicHubServer.getInstance().getPlayerDataManager().getRedisManager().getKeys("radium:rank:*");
                        
                        player.sendMessage(Component.text("=== Redis Keys Debug ===").color(NamedTextColor.YELLOW));
                        player.sendMessage(Component.text("Profile keys found: " + profileKeys.size()).color(NamedTextColor.WHITE));
                        player.sendMessage(Component.text("Rank keys found: " + rankKeys.size()).color(NamedTextColor.WHITE));
                        
                        if (rankKeys.size() <= 20) {
                            player.sendMessage(Component.text("Rank keys: " + rankKeys).color(NamedTextColor.GRAY));
                        }
                        
                        if (profileKeys.size() <= 10) {
                            player.sendMessage(Component.text("Profile keys: " + profileKeys).color(NamedTextColor.GRAY));
                        }
                    } catch (Exception e) {
                        player.sendMessage(Component.text("Error fetching keys: " + e.getMessage()).color(NamedTextColor.RED));
                    }
                }
                
                case "raw" -> {
                    if (!context.has(parameter)) {
                        player.sendMessage(Component.text("Usage: /radiumdebug raw <redis_key>").color(NamedTextColor.RED));
                        return;
                    }
                    
                    String key = context.get(parameter);
                    try {
                        String data = MythicHubServer.getInstance().getPlayerDataManager().getRedisManager().get(key);
                        player.sendMessage(Component.text("=== Raw Redis Data ===").color(NamedTextColor.YELLOW));
                        player.sendMessage(Component.text("Key: " + key).color(NamedTextColor.WHITE));
                        if (data != null) {
                            player.sendMessage(Component.text("Data: " + data).color(NamedTextColor.WHITE));
                            
                            // Try to parse as JSON for better formatting
                            try {
                                JsonObject json = JsonParser.parseString(data).getAsJsonObject();
                                player.sendMessage(Component.text("JSON parsed successfully").color(NamedTextColor.GREEN));
                            } catch (Exception e) {
                                player.sendMessage(Component.text("Not valid JSON: " + e.getMessage()).color(NamedTextColor.YELLOW));
                            }
                        } else {
                            player.sendMessage(Component.text("No data found for key").color(NamedTextColor.RED));
                        }
                    } catch (Exception e) {
                        player.sendMessage(Component.text("Error fetching data: " + e.getMessage()).color(NamedTextColor.RED));
                    }
                }
                
                case "clearcache" -> {
                    // Clear all caches in RadiumClient
                    radiumClient.clearPlayerCache(player.getUuid());
                    radiumClient.refreshRankCache();
                    
                    player.sendMessage(Component.text("=== Cache Cleared ===").color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("Cleared profile and rank caches").color(NamedTextColor.GREEN));
                }
                
                case "testchat" -> {
                    // Test chat formatting for current player
                    String testMessage = context.has(parameter) ? context.get(parameter) : "Test message";
                    
                    radiumClient.formatChatMessage(player.getUuid(), player.getUsername(), testMessage)
                        .thenAccept(formattedMessage -> {
                            player.sendMessage(Component.text("=== Chat Format Test ===").color(NamedTextColor.YELLOW));
                            player.sendMessage(Component.text("Formatted result:").color(NamedTextColor.WHITE));
                            player.sendMessage(formattedMessage);
                        })
                        .exceptionally(throwable -> {
                            player.sendMessage(Component.text("Error formatting message: " + throwable.getMessage()).color(NamedTextColor.RED));
                            return null;
                        });
                }
                
                default -> {
                    player.sendMessage(Component.text("=== Radium Debug Commands ===").color(NamedTextColor.YELLOW));
                    player.sendMessage(Component.text("/radiumdebug profile [uuid] - Show profile data").color(NamedTextColor.WHITE));
                    player.sendMessage(Component.text("/radiumdebug rank [name] - Show rank data").color(NamedTextColor.WHITE));
                    player.sendMessage(Component.text("/radiumdebug keys - Show Redis keys").color(NamedTextColor.WHITE));
                    player.sendMessage(Component.text("/radiumdebug raw <key> - Show raw Redis data").color(NamedTextColor.WHITE));
                    player.sendMessage(Component.text("/radiumdebug clearCache - Clear all caches").color(NamedTextColor.WHITE));
                    player.sendMessage(Component.text("/radiumdebug testChat [message] - Test chat formatting").color(NamedTextColor.WHITE));
                    player.sendMessage(Component.text("/radiumdebug clearCache - Clear profile and rank caches").color(NamedTextColor.WHITE));
                    player.sendMessage(Component.text("/radiumdebug testChat [message] - Test chat formatting").color(NamedTextColor.WHITE));
                }
            }
        }, subcommand);
        
        addSyntax((sender, context) -> {
            sender.sendMessage(Component.text("=== Radium Debug Commands ===").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/radiumdebug profile [uuid] - Show profile data").color(NamedTextColor.WHITE));
            sender.sendMessage(Component.text("/radiumdebug rank [name] - Show rank data").color(NamedTextColor.WHITE));
            sender.sendMessage(Component.text("/radiumdebug keys - Show Redis keys").color(NamedTextColor.WHITE));
            sender.sendMessage(Component.text("/radiumdebug raw <key> - Show raw Redis data").color(NamedTextColor.WHITE));
            sender.sendMessage(Component.text("/radiumdebug clearCache - Clear profile and rank caches").color(NamedTextColor.WHITE));
            sender.sendMessage(Component.text("/radiumdebug testChat [message] - Test chat formatting").color(NamedTextColor.WHITE));
        }, subcommand, parameter);
    }
}
