package mythic.hub.managers;

import mythic.hub.MythicHubServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Sidebar;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {
    private final ConcurrentHashMap<Player, Sidebar> playerScoreboards = new ConcurrentHashMap<>();
    private static final TextColor LIGHT_PINK = TextColor.color(255, 182, 193);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yy");

    public void createScoreboard(Player player) {
        Sidebar sidebar = new Sidebar(Component.text("MYTHICPVP")
                .color(LIGHT_PINK)
                .decorate(TextDecoration.BOLD));

        updateScoreboard(sidebar, player);
        sidebar.addViewer(player);
        playerScoreboards.put(player, sidebar);
    }

    public void updateScoreboard(Sidebar sidebar, Player player) {
        // Get current server name
        String currentServer = "Hub"; // Default
        ServerManager serverManager = MythicHubServer.getInstance().getServerManager();
        if (serverManager != null) {
            currentServer = serverManager.getCurrentServerName();
        }

        // Get player count
        int onlinePlayers = MinecraftServer.getConnectionManager().getOnlinePlayers().size();

        // Get current date
        String currentDate = LocalDateTime.now().format(TIME_FORMATTER);

        // Build scoreboard content with proper line management
        int lineNumber = 15;

        // Date - right under title, centered and in grey
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "date",
                Component.text("   " + currentDate + "   ").color(NamedTextColor.GRAY),
                lineNumber--
        ));

        // Empty line
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "empty1",
                Component.empty(),
                lineNumber--
        ));

        // Rank
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "rank",
                Component.text("Rank: ").color(NamedTextColor.GRAY)
                        .append(Component.text("Member").color(NamedTextColor.WHITE)),
                lineNumber--
        ));

        // Empty line
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "empty2",
                Component.empty(),
                lineNumber--
        ));

        // Server
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "server",
                Component.text("Server: ").color(NamedTextColor.GRAY)
                        .append(Component.text(currentServer).color(LIGHT_PINK)),
                lineNumber--
        ));

        // Players
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "players",
                Component.text("Players: ").color(NamedTextColor.GRAY)
                        .append(Component.text(String.valueOf(onlinePlayers)).color(NamedTextColor.WHITE)),
                lineNumber--
        ));

        // Empty line
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "empty3",
                Component.empty(),
                lineNumber--
        ));

        // Show other online servers if available
        if (serverManager != null) {
            var onlineServers = serverManager.getOnlineServers();
            if (onlineServers.size() > 1) {
                sidebar.createLine(new Sidebar.ScoreboardLine(
                        "network",
                        Component.text("Network: ").color(NamedTextColor.GRAY)
                                .append(Component.text(onlineServers.size() + " servers").color(NamedTextColor.GREEN)),
                        lineNumber--
                ));

                sidebar.createLine(new Sidebar.ScoreboardLine(
                        "empty4",
                        Component.empty(),
                        lineNumber--
                ));
            }
        }

        // Empty line before website
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "empty5",
                Component.empty(),
                lineNumber--
        ));

        // Website
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "website",
                Component.text("play.mythicpvp.net").color(LIGHT_PINK),
                lineNumber--
        ));
    }

    public void updateAllScoreboards() {
        playerScoreboards.forEach((player, sidebar) -> {
            // Remove all existing lines first
            removeAllLines(sidebar);
            // Then update with new content
            updateScoreboard(sidebar, player);
        });
    }

    private void removeAllLines(Sidebar sidebar) {
        // Remove lines by their IDs
        String[] lineIds = {
            "date", "empty1", "rank", "empty2", "server", "players", 
            "empty3", "network", "empty4", "empty5", "website"
        };
        
        for (String lineId : lineIds) {
            try {
                sidebar.removeLine(lineId);
            } catch (Exception e) {
                // Ignore if line doesn't exist
            }
        }
    }

    public void removeScoreboard(Player player) {
        Sidebar sidebar = playerScoreboards.remove(player);
        if (sidebar != null) {
            sidebar.removeViewer(player);
        }
    }
}