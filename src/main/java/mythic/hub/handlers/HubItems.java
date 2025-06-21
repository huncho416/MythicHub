package mythic.hub.handlers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.HeadProfile;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class HubItems {
    private static final TextColor LIGHT_PINK = TextColor.color(255, 182, 193);
    private static final Component SETTINGS_LORE = Component.text("Right-click to open settings")
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false);
    private static final Component COSMETICS_LORE = Component.text("Right-click to open cosmetics")
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false);
    private static final Component PROFILE_LORE = Component.text("Right-click to view your profile")
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false);
    private static final Component VISIBILITY_LORE = Component.text("Right-click to toggle player visibility")
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false);
    private static final Component SERVER_SELECTOR_LORE = Component.text("Right-click to open server menu")
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false);
    private static final Component FRIENDS_LORE = Component.text("Right-click to manage friends")
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false);

    // Cache for player skins to avoid repeated API calls
    private static final ConcurrentHashMap<String, PlayerSkin> skinCache = new ConcurrentHashMap<>();

    public static void giveHubItems(Player player) {
        player.getInventory().clear();

        // Pre-build items for better performance
        ItemStack serverSelector = createServerSelector();
        ItemStack playerSettings = createPlayerSettings();
        ItemStack friends = createFriends();
        ItemStack cosmetics = createCosmetics();
        ItemStack playerHider = createPlayerHider();
        ItemStack profile = createProfile(player);

        // Set items in inventory
        player.getInventory().setItemStack(0, serverSelector);
        player.getInventory().setItemStack(1, playerSettings);
        player.getInventory().setItemStack(2, friends);
        player.getInventory().setItemStack(4, cosmetics);
        player.getInventory().setItemStack(7, playerHider);
        player.getInventory().setItemStack(8, profile);
    }

    private static ItemStack createServerSelector() {
        return ItemStack.builder(Material.COMPASS)
                .customName(Component.text("Server Selector")
                        .color(LIGHT_PINK)
                        .decoration(TextDecoration.ITALIC, false))
                .lore(List.of(SERVER_SELECTOR_LORE))
                .build();
    }

    private static ItemStack createPlayerSettings() {
        return ItemStack.builder(Material.REDSTONE)
                .customName(Component.text("Player Settings")
                        .color(LIGHT_PINK)
                        .decoration(TextDecoration.ITALIC, false))
                .lore(List.of(SETTINGS_LORE))
                .build();
    }

    private static ItemStack createFriends() {
        return ItemStack.builder(Material.EMERALD)
                .customName(Component.text("Friends")
                        .color(LIGHT_PINK)
                        .decoration(TextDecoration.ITALIC, false))
                .lore(List.of(FRIENDS_LORE))
                .build();
    }

    private static ItemStack createCosmetics() {
        return ItemStack.builder(Material.ENDER_PEARL)
                .customName(Component.text("Cosmetics")
                        .color(LIGHT_PINK)
                        .decoration(TextDecoration.ITALIC, false))
                .lore(List.of(COSMETICS_LORE))
                .build();
    }

    private static ItemStack createPlayerHider() {
        return ItemStack.builder(Material.LIME_DYE)
                .customName(Component.text("Player Visibility: ON")
                        .color(NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false))
                .lore(List.of(VISIBILITY_LORE))
                .build();
    }

    private static ItemStack createProfile(Player player) {
        ItemStack.Builder profileBuilder = ItemStack.builder(Material.PLAYER_HEAD)
                .customName(Component.text("Profile")
                        .color(LIGHT_PINK)
                        .decoration(TextDecoration.ITALIC, false))
                .lore(List.of(PROFILE_LORE));

        // Use cached skin if available
        String uuid = player.getUuid().toString();
        PlayerSkin cachedSkin = skinCache.get(uuid);

        if (cachedSkin != null) {
            profileBuilder.set(ItemComponent.PROFILE, new HeadProfile(cachedSkin));
        } else {
            // Try to get the player's skin and cache it
            try {
                PlayerSkin playerSkin = PlayerSkin.fromUuid(uuid);
                if (playerSkin != null) {
                    skinCache.put(uuid, playerSkin);
                    profileBuilder.set(ItemComponent.PROFILE, new HeadProfile(playerSkin));
                }
            } catch (Exception e) {
                System.out.println("Could not load skin for player: " + player.getUsername() + " - " + e.getMessage());
            }
        }

        return profileBuilder.build();
    }

    public static ItemStack createPlayerHead(Player targetPlayer, Component displayName, List<Component> lore) {
        ItemStack.Builder headBuilder = ItemStack.builder(Material.PLAYER_HEAD)
                .customName(displayName)
                .lore(lore);

        String uuid = targetPlayer.getUuid().toString();
        PlayerSkin cachedSkin = skinCache.get(uuid);

        if (cachedSkin != null) {
            headBuilder.set(ItemComponent.PROFILE, new HeadProfile(cachedSkin));
        } else {
            try {
                PlayerSkin playerSkin = PlayerSkin.fromUuid(uuid);
                if (playerSkin != null) {
                    skinCache.put(uuid, playerSkin);
                    headBuilder.set(ItemComponent.PROFILE, new HeadProfile(playerSkin));
                }
            } catch (Exception e) {
                System.out.println("Could not load skin for player: " + targetPlayer.getUsername() + " - " + e.getMessage());
            }
        }

        return headBuilder.build();
    }

    public static ItemStack createPlayerHeadFromUuid(java.util.UUID playerUuid, String playerName, Component displayName, List<Component> lore) {
        ItemStack.Builder headBuilder = ItemStack.builder(Material.PLAYER_HEAD)
                .customName(displayName)
                .lore(lore);

        String uuid = playerUuid.toString();
        PlayerSkin cachedSkin = skinCache.get(uuid);

        if (cachedSkin != null) {
            headBuilder.set(ItemComponent.PROFILE, new HeadProfile(cachedSkin));
        } else {
            try {
                PlayerSkin playerSkin = PlayerSkin.fromUuid(uuid);
                if (playerSkin != null) {
                    skinCache.put(uuid, playerSkin);
                    headBuilder.set(ItemComponent.PROFILE, new HeadProfile(playerSkin));
                }
            } catch (Exception e) {
                System.out.println("Could not load skin for UUID: " + playerUuid + " (Name: " + playerName + ") - " + e.getMessage());
            }
        }

        return headBuilder.build();
    }

    // Method to clear cached skins when needed
    public static void clearSkinCache() {
        skinCache.clear();
    }

    // Method to remove specific player from cache
    public static void removeSkinFromCache(String uuid) {
        skinCache.remove(uuid);
    }
}