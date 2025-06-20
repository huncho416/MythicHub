package mythic.hub.config;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.HashMap;
import java.util.Map;

public class RankConfig {
    private static final Map<String, RankInfo> RANK_INFO = new HashMap<>();

    static {
        // Define rank priorities and colors (higher priority = higher rank)
        RANK_INFO.put("OWNER", new RankInfo(1000, TextColor.color(255, 85, 85), "§c"));
        RANK_INFO.put("ADMIN", new RankInfo(900, TextColor.color(255, 85, 85), "§c"));
        RANK_INFO.put("MANAGER", new RankInfo(800, TextColor.color(255, 170, 85), "§6"));
        RANK_INFO.put("DEVELOPER", new RankInfo(750, TextColor.color(85, 255, 255), "§b"));
        RANK_INFO.put("MODERATOR", new RankInfo(700, TextColor.color(85, 255, 85), "§a"));
        RANK_INFO.put("HELPER", new RankInfo(600, TextColor.color(85, 255, 85), "§a"));
        RANK_INFO.put("YOUTUBE", new RankInfo(550, TextColor.color(255, 85, 85), "§c"));
        RANK_INFO.put("TWITCH", new RankInfo(540, TextColor.color(170, 85, 255), "§5"));
        RANK_INFO.put("FAMOUS", new RankInfo(500, TextColor.color(255, 255, 85), "§e"));
        RANK_INFO.put("MYTHIC", new RankInfo(450, TextColor.color(255, 182, 193), "§d"));
        RANK_INFO.put("LEGEND", new RankInfo(400, TextColor.color(255, 170, 85), "§6"));
        RANK_INFO.put("HERO", new RankInfo(350, TextColor.color(85, 255, 255), "§b"));
        RANK_INFO.put("VIP+", new RankInfo(300, TextColor.color(85, 255, 85), "§a"));
        RANK_INFO.put("VIP", new RankInfo(200, TextColor.color(85, 255, 85), "§a"));
        RANK_INFO.put("MEMBER", new RankInfo(100, NamedTextColor.GRAY, "§7"));
        RANK_INFO.put("DEFAULT", new RankInfo(0, NamedTextColor.GRAY, "§7"));
    }

    public static RankInfo getRankInfo(String rankName) {
        return RANK_INFO.getOrDefault(rankName.toUpperCase(), RANK_INFO.get("DEFAULT"));
    }

    public static class RankInfo {
        private final int priority;
        private final TextColor color;
        private final String legacyColor;

        public RankInfo(int priority, TextColor color, String legacyColor) {
            this.priority = priority;
            this.color = color;
            this.legacyColor = legacyColor;
        }

        public int getPriority() { return priority; }
        public TextColor getColor() { return color; }
        public String getLegacyColor() { return legacyColor; }
    }
}