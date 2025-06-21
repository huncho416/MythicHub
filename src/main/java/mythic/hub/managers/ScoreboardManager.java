package mythic.hub.managers;

import mythic.hub.MythicHubServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Sidebar;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {
    private final ConcurrentHashMap<Player, Sidebar> playerScoreboards = new ConcurrentHashMap<>();
    private static final TextColor LIGHT_PINK = TextColor.color(255, 182, 193);
    private static final TextColor WHITE = NamedTextColor.WHITE;
    private static final TextColor GRAY = NamedTextColor.GRAY;
    private static final TextColor LIGHT_BLUE = TextColor.color(173, 216, 230);
    private static final TextColor DARK_BLUE = NamedTextColor.DARK_BLUE;
    private static final TextColor LIGHT_RED = TextColor.color(255, 182, 193);
    private static final TextColor DARK_RED = NamedTextColor.DARK_RED;
    private static final TextColor LIGHT_GREEN = NamedTextColor.GREEN;
    private static final TextColor DARK_GREEN = NamedTextColor.DARK_GREEN;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    public void createScoreboard(Player player) {
        // Create title: (LightPink)Mythic(White)PvP
        Component title = Component.text("Mythic")
                .color(LIGHT_PINK)
                .append(Component.text("PvP").color(WHITE));

        Sidebar sidebar = new Sidebar(title);

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

        // Get server player counts (mock data for now - you'll need to implement actual server communication)
        int prisonPlayers = getServerPlayerCount("Prison");
        int gensPlayers = getServerPlayerCount("Gens");
        int skyblockPlayers = getServerPlayerCount("Skyblock");

        // Get current date
        String currentDate = LocalDate.now().format(DATE_FORMATTER);

        // Build scoreboard content - LOWER line numbers appear HIGHER on the scoreboard
        int lineNumber = 1;

        // Server IP (bottom line) - light pink: play.mythicpvp.net
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "server_ip",
                Component.text("play.mythicpvp.net").color(LIGHT_PINK),
                lineNumber++
        ));

        // Today's date - white (second to last line)
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "date",
                Component.text(currentDate).color(WHITE),
                lineNumber++
        ));

        // Empty line
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "empty1",
                Component.empty(),
                lineNumber++
        ));

        // Skyblock server: (space)(white)▌ (Light green)Skyblock (dark green) [players]
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "skyblock",
                Component.text(" ▌ ").color(WHITE)
                        .append(Component.text("Skyblock ").color(LIGHT_GREEN))
                        .append(Component.text("[" + skyblockPlayers + "]").color(DARK_GREEN)),
                lineNumber++
        ));

        // Gens server: (space)(white)▌ (light red)Gens (dark red) [players]  
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "gens",
                Component.text(" ▌ ").color(WHITE)
                        .append(Component.text("Gens ").color(NamedTextColor.RED))
                        .append(Component.text("[" + gensPlayers + "]").color(DARK_RED)),
                lineNumber++
        ));

        // Prison server: (space)(white)▌ (Light Blue)Prison (darkblue) [players]
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "prison",
                Component.text(" ▌ ").color(WHITE)
                        .append(Component.text("Prison ").color(LIGHT_BLUE))
                        .append(Component.text("[" + prisonPlayers + "]").color(DARK_BLUE)),
                lineNumber++
        ));

        // Servers header: (space)(white)▌ (lightpink)Servers
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "servers_header",
                Component.text(" ▌ ").color(WHITE)
                        .append(Component.text("Servers").color(LIGHT_PINK)),
                lineNumber++
        ));

        // Empty line
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "empty2",
                Component.empty(),
                lineNumber++
        ));

        // Current server info: (space)(white)▌ (grey)Server: (current server in white)
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "current_server",
                Component.text(" ▌ ").color(WHITE)
                        .append(Component.text("Server: ").color(GRAY))
                        .append(Component.text(currentServer).color(WHITE)),
                lineNumber++
        ));

        // Player name: (space)(white)▌ (grey)Player: (player name in white)
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "player_name",
                Component.text(" ▌ ").color(WHITE)
                        .append(Component.text("Player: ").color(GRAY))
                        .append(Component.text(player.getUsername()).color(WHITE)),
                lineNumber++
        ));

        // Information header: (space)(white)▌ (lightpink)Information
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "info_header",
                Component.text(" ▌ ").color(WHITE)
                        .append(Component.text("Information").color(LIGHT_PINK)),
                lineNumber++
        ));

        // Empty line
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "empty3",
                Component.empty(),
                lineNumber++
        ));

        // Lobby Server (right under title, perfectly centered) - grey
        sidebar.createLine(new Sidebar.ScoreboardLine(
                "lobby_server",
                Component.text("      Lobby Server      ").color(GRAY),
                lineNumber++
        ));
    }

    private int getServerPlayerCount(String serverName) {
        // Mock implementation - replace with actual server communication
        // You'll need to implement Redis communication or BungeeCord plugin messaging
        // to get real player counts from other servers
        switch (serverName.toLowerCase()) {
            case "prison":
                return 42; // Mock data
            case "gens":
                return 28; // Mock data
            case "skyblock":
                return 35; // Mock data
            default:
                return 0;
        }
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
            "server_ip", "date", "empty1", "skyblock", "gens", "prison", 
            "servers_header", "empty2", "current_server", "player_name", 
            "info_header", "empty3", "lobby_server"
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