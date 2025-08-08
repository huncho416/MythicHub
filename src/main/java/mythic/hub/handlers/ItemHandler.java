package mythic.hub.handlers;

import mythic.hub.MythicHubServer;
import mythic.hub.data.PlayerProfile;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ItemHandler {
    private static final TextColor LIGHT_PINK = TextColor.color(255, 182, 193);
    private static final TextColor WHITE = NamedTextColor.WHITE;
    private static final TextColor GRAY = NamedTextColor.GRAY;
    private static final TextColor GREEN = NamedTextColor.GREEN;
    private static final TextColor RED = NamedTextColor.RED;

    // Use ConcurrentHashMap for thread safety
    private static final ConcurrentHashMap<Player, Boolean> playerVisibility = new ConcurrentHashMap<>();

    // Cache sound for performance
    private static final Sound CLICK_SOUND = Sound.sound(SoundEvent.UI_BUTTON_CLICK, Sound.Source.PLAYER, 0.5f, 1.0f);

    // Pre-create common components
    private static final Component EMPTY_COMPONENT = Component.empty();
    private static final Component CLICK_TO_JOIN = Component.text("Click to join!").color(GREEN).decoration(TextDecoration.ITALIC, false);
    private static final Component CLOSE_TEXT = Component.text("Close").color(RED).decoration(TextDecoration.ITALIC, false);

    public static void onPlayerUseItem(PlayerUseItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemStack();

        if (item == null || item.material() == Material.AIR) {
            return;
        }

        // Check by slot position since we know where each item is placed
        int slot = getSlotOfItem(player, item);
        if (slot == -1) return;

        // Play click sound
        player.playSound(CLICK_SOUND);

        switch (slot) {
            case 0 -> handleServerSelector(player);
            case 1 -> handlePlayerSettings(player);
            case 4 -> handleCosmetics(player);
            case 7 -> handlePlayerVisibility(player);
            case 8 -> handleProfile(player);
        }
    }

    private static int getSlotOfItem(Player player, ItemStack item) {
        Material itemMaterial = item.material();
        for (int i = 0; i < 9; i++) {  // Fixed: added 'i' before the '<' operator
            ItemStack slotItem = player.getInventory().getItemStack(i);
            if (slotItem != null && slotItem.material() == itemMaterial) {
                return i;
            }
        }
        return -1;
    }

    private static void handleServerSelector(Player player) {

        Inventory serverMenu = new Inventory(InventoryType.CHEST_3_ROW,
                Component.text("Server Selector")
                        .color(LIGHT_PINK)
                        .decoration(TextDecoration.BOLD, true));


        TextColor LIGHT_BLUE = TextColor.color(173, 216, 230);
        TextColor DARK_BLUE = NamedTextColor.DARK_BLUE;
        TextColor LIGHT_RED = NamedTextColor.RED; // Changed to use &c (NamedTextColor.RED)
        TextColor DARK_RED = NamedTextColor.DARK_RED; // Dark red for Gens player count
        TextColor LIGHT_GREEN = NamedTextColor.GREEN;
        TextColor DARK_GREEN = NamedTextColor.DARK_GREEN;


        int prisonPlayers = getServerPlayerCount("Prison");
        int gensPlayers = getServerPlayerCount("Gens");
        int skyblockPlayers = getServerPlayerCount("Skyblock");


        Component playersOnlineText = Component.text("Players Online: ").color(WHITE).decoration(TextDecoration.ITALIC, false);


        ItemStack prison = ItemStack.builder(Material.IRON_BARS)
                .customName(Component.text("Prison").color(LIGHT_BLUE).decoration(TextDecoration.ITALIC, false))
                .lore(List.of(
                        Component.text("Mine, rankup, and dominate!").color(GRAY).decoration(TextDecoration.ITALIC, false),
                        EMPTY_COMPONENT,
                        playersOnlineText.append(Component.text(String.valueOf(prisonPlayers)).color(DARK_BLUE)),
                        EMPTY_COMPONENT,
                        CLICK_TO_JOIN
                ))
                .build();


        ItemStack gens = ItemStack.builder(Material.SPAWNER)
                .customName(Component.text("Gens").color(LIGHT_RED).decoration(TextDecoration.ITALIC, false))
                .lore(List.of(
                        Component.text("Generate resources and build!").color(GRAY).decoration(TextDecoration.ITALIC, false),
                        EMPTY_COMPONENT,
                        playersOnlineText.append(Component.text(String.valueOf(gensPlayers)).color(DARK_RED)),
                        EMPTY_COMPONENT,
                        CLICK_TO_JOIN
                ))
                .build();


        ItemStack skyblock = ItemStack.builder(Material.GRASS_BLOCK)
                .customName(Component.text("Skyblock").color(LIGHT_GREEN).decoration(TextDecoration.ITALIC, false))
                .lore(List.of(
                        Component.text("Start from nothing, build everything!").color(GRAY).decoration(TextDecoration.ITALIC, false),
                        EMPTY_COMPONENT,
                        playersOnlineText.append(Component.text(String.valueOf(skyblockPlayers)).color(DARK_GREEN)),
                        EMPTY_COMPONENT,
                        CLICK_TO_JOIN
                ))
                .build();


        serverMenu.setItemStack(10, prison);   // Middle row, second position
        serverMenu.setItemStack(13, gens);     // Middle row, fourth position  
        serverMenu.setItemStack(16, skyblock); // Middle row, sixth position

        player.openInventory(serverMenu);
    }

    private static int getServerPlayerCount(String serverName) {
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

    private static void handlePlayerSettings(Player player) {
        Inventory settingsMenu = new Inventory(InventoryType.CHEST_3_ROW,
                Component.text("Player Settings")
                        .color(LIGHT_PINK)
                        .decoration(TextDecoration.BOLD, true));

        ItemStack chatSettings = ItemStack.builder(Material.WRITABLE_BOOK)
                .customName(Component.text("Chat Settings").color(WHITE).decoration(TextDecoration.ITALIC, false))
                .lore(List.of(Component.text("Configure chat preferences").color(GRAY).decoration(TextDecoration.ITALIC, false)))
                .build();

        ItemStack friendRequests = ItemStack.builder(Material.PLAYER_HEAD)
                .customName(Component.text("Friend Requests").color(GREEN).decoration(TextDecoration.ITALIC, false))
                .lore(List.of(Component.text("Manage friend requests").color(GRAY).decoration(TextDecoration.ITALIC, false)))
                .build();

        ItemStack soundSettings = ItemStack.builder(Material.NOTE_BLOCK)
                .customName(Component.text("Sound Settings").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                .lore(List.of(Component.text("Configure sound preferences").color(GRAY).decoration(TextDecoration.ITALIC, false)))
                .build();

        ItemStack back = ItemStack.builder(Material.BARRIER)
                .customName(CLOSE_TEXT)
                .build();

        settingsMenu.setItemStack(11, chatSettings);
        settingsMenu.setItemStack(13, friendRequests);
        settingsMenu.setItemStack(15, soundSettings);
        settingsMenu.setItemStack(22, back);

        player.openInventory(settingsMenu);
    }

    private static void handleCosmetics(Player player) {
        Inventory cosmeticsMenu = new Inventory(InventoryType.CHEST_6_ROW,
                Component.text("Cosmetics")
                        .color(LIGHT_PINK)
                        .decoration(TextDecoration.BOLD, true));

        ItemStack particles = ItemStack.builder(Material.FIREWORK_STAR)
                .customName(Component.text("Particle Effects").color(LIGHT_PINK).decoration(TextDecoration.ITALIC, false))
                .lore(List.of(Component.text("Customize your particle trail").color(GRAY).decoration(TextDecoration.ITALIC, false)))
                .build();

        ItemStack hats = ItemStack.builder(Material.DIAMOND_HELMET)
                .customName(Component.text("Hats").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
                .lore(List.of(Component.text("Show off with stylish hats").color(GRAY).decoration(TextDecoration.ITALIC, false)))
                .build();

        ItemStack pets = ItemStack.builder(Material.BONE)
                .customName(Component.text("Pets").color(GREEN).decoration(TextDecoration.ITALIC, false))
                .lore(List.of(Component.text("Adopt a loyal companion").color(GRAY).decoration(TextDecoration.ITALIC, false)))
                .build();

        ItemStack gadgets = ItemStack.builder(Material.BLAZE_ROD)
                .customName(Component.text("Gadgets").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                .lore(List.of(Component.text("Fun gadgets and toys").color(GRAY).decoration(TextDecoration.ITALIC, false)))
                .build();

        ItemStack back = ItemStack.builder(Material.BARRIER)
                .customName(CLOSE_TEXT)
                .build();

        cosmeticsMenu.setItemStack(20, particles);
        cosmeticsMenu.setItemStack(22, hats);
        cosmeticsMenu.setItemStack(24, pets);
        cosmeticsMenu.setItemStack(26, gadgets);
        cosmeticsMenu.setItemStack(49, back);

        player.openInventory(cosmeticsMenu);
    }

    private static void handleProfile(Player player) {
        Inventory profileMenu = new Inventory(InventoryType.CHEST_6_ROW,
                Component.text(player.getUsername() + "'s Profile")
                        .color(LIGHT_PINK)
                        .decoration(TextDecoration.BOLD, true));

        // Get player data
        PlayerProfile profile = MythicHubServer.getInstance().getPlayerDataManager().getPlayer(player);

        ItemStack playerInfo = ItemStack.builder(Material.PLAYER_HEAD)
                .customName(Component.text(player.getUsername()).color(WHITE).decoration(TextDecoration.ITALIC, false))
                .lore(List.of(
                        Component.text("UUID: " + player.getUuid()).color(GRAY).decoration(TextDecoration.ITALIC, false),
                        Component.text("Join Date: Coming Soon").color(GRAY).decoration(TextDecoration.ITALIC, false)
                ))
                .build();

        String rankName = "Member"; // Default rank
        try {
            // Get rank from Radium
            mythic.hub.integrations.radium.RadiumProfile radiumProfile = 
                mythic.hub.MythicHubServer.getInstance().getRadiumClient()
                    .getPlayerProfile(player.getUuid()).get();
            
            if (radiumProfile != null && !radiumProfile.getRanks().isEmpty()) {
                // Get the highest priority rank
                mythic.hub.integrations.radium.RadiumClient radiumClient = 
                    mythic.hub.MythicHubServer.getInstance().getRadiumClient();
                
                rankName = radiumProfile.getRanks().stream()
                    .map(rName -> {
                        mythic.hub.integrations.radium.RadiumRank rank = radiumClient.getRank(rName);
                        return new Object[] { rName, rank != null ? rank.getWeight() : 0 };
                    })
                    .max((r1, r2) -> Integer.compare((Integer) r1[1], (Integer) r2[1]))
                    .map(r -> (String) r[0])
                    .orElse("Member");
            }
        } catch (Exception e) {
            System.err.println("Error getting rank from Radium for profile GUI: " + e.getMessage());
        }

        ItemStack rankInfo = ItemStack.builder(Material.GOLDEN_APPLE)
                .customName(Component.text("Rank: " + rankName).color(LIGHT_PINK).decoration(TextDecoration.ITALIC, false))
                .lore(List.of(Component.text("Your current rank").color(GRAY).decoration(TextDecoration.ITALIC, false)))
                .build();

        ItemStack stats = ItemStack.builder(Material.BOOK)
                .customName(Component.text("Statistics").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                .lore(List.of(
                        Component.text("View your game statistics").color(GRAY).decoration(TextDecoration.ITALIC, false),
                        EMPTY_COMPONENT,
                        Component.text("Total Playtime: Coming Soon").color(GRAY).decoration(TextDecoration.ITALIC, false),
                        Component.text("Games Played: Coming Soon").color(GRAY).decoration(TextDecoration.ITALIC, false)
                ))
                .build();

        ItemStack achievements = ItemStack.builder(Material.DIAMOND)
                .customName(Component.text("Achievements").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
                .lore(List.of(Component.text("View your achievements").color(GRAY).decoration(TextDecoration.ITALIC, false)))
                .build();

        ItemStack back = ItemStack.builder(Material.BARRIER)
                .customName(CLOSE_TEXT)
                .build();

        profileMenu.setItemStack(13, playerInfo);
        profileMenu.setItemStack(21, rankInfo);
        profileMenu.setItemStack(23, stats);
        profileMenu.setItemStack(25, achievements);
        profileMenu.setItemStack(49, back);

        player.openInventory(profileMenu);
    }

    private static void handlePlayerVisibility(Player player) {
        boolean currentlyVisible = playerVisibility.getOrDefault(player, true);
        boolean newVisibility = !currentlyVisible;
        playerVisibility.put(player, newVisibility);

        ItemStack visibilityItem;
        Component message;

        if (newVisibility) {
            visibilityItem = ItemStack.builder(Material.LIME_DYE)
                    .customName(Component.text("Player Visibility: ON").color(GREEN).decoration(TextDecoration.ITALIC, false))
                    .lore(List.of(Component.text("Right-click to toggle player visibility").color(GRAY).decoration(TextDecoration.ITALIC, false)))
                    .build();
            message = Component.text("Players are now ").color(GRAY).append(Component.text("visible").color(GREEN));
        } else {
            visibilityItem = ItemStack.builder(Material.GRAY_DYE)
                    .customName(Component.text("Player Visibility: OFF").color(RED).decoration(TextDecoration.ITALIC, false))
                    .lore(List.of(Component.text("Right-click to toggle player visibility").color(GRAY).decoration(TextDecoration.ITALIC, false)))
                    .build();
            message = Component.text("Players are now ").color(GRAY).append(Component.text("hidden").color(RED));
        }

        player.getInventory().setItemStack(7, visibilityItem);
        player.sendMessage(message);
    }

    public static void resetPlayerVisibility(Player player) {
        playerVisibility.remove(player);
    }
// Add this to your onPlayerUseItem method in the switch statement or if-else chain
// Add this method to handle friends functionality
private static void handleFriends(Player player) {
    player.playSound(CLICK_SOUND);
    
    Inventory friendsInventory = new Inventory(InventoryType.CHEST_6_ROW, Component.text("Friends")
            .color(LIGHT_PINK)
            .decoration(TextDecoration.ITALIC, false));

    // You'll need to implement FriendManager or use your existing friend system
    // Example structure for the friends GUI:
    
    // Friends list section
    // TODO: Integrate with your FriendManager to get actual friends
    // List<Friend> friends = FriendManager.getFriends(player.getUuid());
    
    // Friend requests section
    // TODO: Integrate with your FriendManager to get pending requests
    // List<FriendRequest> requests = FriendManager.getPendingRequests(player.getUuid());
    
    // Add friend button
    ItemStack addFriend = ItemStack.builder(Material.GREEN_WOOL)
            .customName(Component.text("Add Friend")
                    .color(GREEN)
                    .decoration(TextDecoration.ITALIC, false))
            .lore(List.of(Component.text("Click to add a new friend")
                    .color(GRAY)
                    .decoration(TextDecoration.ITALIC, false)))
            .build();
    
    // Settings button
    ItemStack friendSettings = ItemStack.builder(Material.GRAY_WOOL)
            .customName(Component.text("Friend Settings")
                    .color(WHITE)
                    .decoration(TextDecoration.ITALIC, false))
            .lore(List.of(Component.text("Manage friend preferences")
                    .color(GRAY)
                    .decoration(TextDecoration.ITALIC, false)))
            .build();
    
    // Close button
    ItemStack close = ItemStack.builder(Material.BARRIER)
            .customName(CLOSE_TEXT)
            .build();
    
    // Set items in inventory
    friendsInventory.setItemStack(45, addFriend);
    friendsInventory.setItemStack(49, friendSettings);
    friendsInventory.setItemStack(53, close);
    
    player.openInventory(friendsInventory);
}
}