package mythic.hub;

import mythic.hub.commands.FriendsCommand;
import mythic.hub.commands.ServerCommand;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.Material;
import mythic.hub.commands.ChatCommands;
import mythic.hub.commands.TagCommand;
import mythic.hub.config.DatabaseConfig;
import mythic.hub.config.VelocityConfig;
import mythic.hub.config.VersionConfig;
import mythic.hub.config.ProxyConfig;
import mythic.hub.handlers.ItemHandler;
import mythic.hub.handlers.PlayerHandler;
import mythic.hub.managers.ChatManager;
import mythic.hub.managers.PlayerDataManager;
import mythic.hub.managers.ProxyManager;
import mythic.hub.managers.ScoreboardManager;
import mythic.hub.managers.ServerManager;
import mythic.hub.managers.TabListManager;
import mythic.hub.integrations.radium.RadiumClient;
import mythic.hub.world.HubWorld;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MythicHubServer {

    // Instance field for singleton pattern
    private static MythicHubServer instance;

    // Manager field declarations
    private static ScoreboardManager scoreboardManager;
    private static TabListManager tabListManager;
    private static PlayerDataManager playerDataManager;
    private static ServerManager serverManager;
    private static ChatManager chatManager;
    private static ProxyManager proxyManager;
    private static RadiumClient radiumClient;
    private static ScheduledExecutorService scheduler;
    private static InstanceContainer hubInstance;

    public static void main(String[] args) {
        // Initialize the server
        var server = MinecraftServer.init();

        // Create instance using same logic pattern
        instance = new MythicHubServer();

        // Initialize managers
        instance.initializeManagers();

        // Enable modern forwarding for Velocity proxy support (conditional)
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.initialize();
        
        if (proxyConfig.isVelocityForwardingEnabled()) {
            // Enable Velocity forwarding - requires connecting through proxy (port 25565)
            net.minestom.server.extras.velocity.VelocityProxy.enable(proxyConfig.getVelocitySecret());
            System.out.println("Velocity forwarding enabled - connect via proxy on port 25565");
        } else {
            // Direct connection mode - connect directly to MythicHub (port 25566)
            System.out.println("Direct connection mode - connect directly to port 25566");
        }

        // Setup the default instance
        setupDefaultInstance();

        // Register commands
        MinecraftServer.getCommandManager().register(new FriendsCommand());
        MinecraftServer.getCommandManager().register(new ServerCommand());
        MinecraftServer.getCommandManager().register(new mythic.hub.commands.RadiumTestCommand());
        
        // Register staff command forwarders for Radium integration
        registerStaffCommands();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down MythicHub Server...");
            if (radiumClient != null) {
                radiumClient.shutdown();
            }
            if (playerDataManager != null) {
                playerDataManager.shutdown();
            }
            if (serverManager != null) {
                serverManager.shutdown();
            }
            if (proxyManager != null) {
                proxyManager.shutdown();
            }
        }));

        // Start the server
        server.start("0.0.0.0", 25566);
        System.out.println("MythicPvP Hub Server started successfully on port 25566!");
    }

    // Add getInstance method using same logic pattern
    public static MythicHubServer getInstance() {
        return instance;
    }

    // Initialize managers method
    private void initializeManagers() {
        System.out.println("Initializing managers...");

        // Initialize version configuration first
        VersionConfig versionConfig = new VersionConfig();
        versionConfig.initialize();

        // Initialize database configuration
        DatabaseConfig databaseConfig = new DatabaseConfig();
        VelocityConfig velocityConfig = new VelocityConfig();

        // Initialize managers
        scoreboardManager = new ScoreboardManager();
        tabListManager = new TabListManager();
        playerDataManager = new PlayerDataManager(databaseConfig);
        chatManager = new ChatManager();

        // Initialize server manager with server name
        String serverName = System.getProperty("server.name", "Hub-1"); // Can be set via JVM args
        serverManager = new ServerManager(playerDataManager.getRedisManager(), serverName);

        // Initialize proxy manager for Velocity integration
        proxyManager = new ProxyManager(playerDataManager.getRedisManager(), velocityConfig);

        // Initialize Radium client for integration with Radium backend
        radiumClient = new RadiumClient(playerDataManager.getRedisManager());

        scheduler = Executors.newScheduledThreadPool(3);

        System.out.println("All managers initialized successfully!");
    }

    // Register all commands
    private static void registerCommands() {
        var commandManager = MinecraftServer.getCommandManager();
        
        // Register chat commands
        commandManager.register(new ChatCommands());
        
        // Register tag command
        commandManager.register(new TagCommand());
        
        System.out.println("Commands registered successfully!");
    }

    // Register staff command forwarders for Radium integration
    private static void registerStaffCommands() {
        var commandManager = MinecraftServer.getCommandManager();
        
        // Register staff command forwarders that will forward commands to Radium proxy
        commandManager.register(new mythic.hub.commands.StaffCommandForwarder("rank"));
        commandManager.register(new mythic.hub.commands.StaffCommandForwarder("grant"));
        commandManager.register(new mythic.hub.commands.StaffCommandForwarder("permission", "perm"));
        commandManager.register(new mythic.hub.commands.StaffCommandForwarder("vanish", "v"));
        commandManager.register(new mythic.hub.commands.StaffCommandForwarder("staffchat", "sc"));
        commandManager.register(new mythic.hub.commands.StaffCommandForwarder("gmc"));
        commandManager.register(new mythic.hub.commands.StaffCommandForwarder("gms"));
        commandManager.register(new mythic.hub.commands.StaffCommandForwarder("gamemode", "gm"));
        
        System.out.println("Staff command forwarders registered for Radium integration!");
    }

    // Getter methods for managers
    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public TabListManager getTabListManager() {
        return tabListManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public ServerManager getServerManager() {
        return serverManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public ProxyManager getProxyManager() {
        return proxyManager;
    }

    public RadiumClient getRadiumClient() {
        return radiumClient;
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public InstanceContainer getHubInstance() {
        return hubInstance;
    }

    private static void setupDefaultInstance() {
        // Create the instance manager
        var instanceManager = MinecraftServer.getInstanceManager();

        // Create a new instance container
        hubInstance = instanceManager.createInstanceContainer();

        // Set the chunk generator (flat world for hub)
        hubInstance.setGenerator(unit -> {
            // Generate basic terrain
            unit.modifier().fillHeight(0, 40, Block.STONE);
            unit.modifier().fillHeight(40, 43, Block.DIRT);
            unit.modifier().fillHeight(43, 44, Block.GRASS_BLOCK);
        });

        // Enable automatic lighting updates
        hubInstance.setChunkSupplier(LightingChunk::new);

        // Set time properties
        hubInstance.setTimeRate(0);
        hubInstance.setTime(6000); // Noon

        // Generate hub world
        HubWorld.generateHub(hubInstance);

        // Set up event handlers
        setupEventHandlers(hubInstance);

        // Start updaters
        startUpdaters();

        System.out.println("Default instance created and configured with proper hub mechanics.");
    }

    private static void setupEventHandlers(InstanceContainer instanceContainer) {
        var globalEventHandler = MinecraftServer.getGlobalEventHandler();

        // Handle player configuration and spawning with proper skin loading
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();
            System.out.println("Player configuring: " + player.getUsername());
            event.setSpawningInstance(instanceContainer);
            player.setRespawnPoint(HubWorld.getSpawnLocation());

            // Load player data asynchronously
            playerDataManager.loadPlayer(player).thenAccept(profile -> {
                System.out.println("Loaded profile for " + player.getUsername());
            });

            PlayerHandler.onPlayerConfiguration(event);
        });

        // Add version compatibility handler
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            Player player = event.getPlayer();
            System.out.println("Player " + player.getUsername() + " connecting with protocol version: " + player.getPlayerConnection().getProtocolVersion());
            
            // Call the existing handler
            PlayerHandler.onPlayerConfiguration(event);
        });

        // Handle player spawn - this is where the welcome message is triggered
        globalEventHandler.addListener(PlayerSpawnEvent.class, event -> {
            final Player player = event.getPlayer();
            System.out.println("Player spawned: " + player.getUsername());

            // Initialize player in scoreboard manager
            if (scoreboardManager != null) {
                scoreboardManager.createScoreboard(player);
            }

            // Initialize player in tab list manager
            if (tabListManager != null) {
                tabListManager.updateTabList(player);
                tabListManager.updateAllTabLists(); // Update for all players
            }

            // Call player handler
            PlayerHandler.onPlayerSpawn(event);
        });

        // Handle player disconnect
        globalEventHandler.addListener(PlayerDisconnectEvent.class, event -> {
            final Player player = event.getPlayer();
            System.out.println("Player disconnected: " + player.getUsername());

            // Remove scoreboard
            if (scoreboardManager != null) {
                scoreboardManager.removeScoreboard(player);
            }

            // Remove from tab list
            if (tabListManager != null) {
                tabListManager.removePlayer(player);
                tabListManager.updateAllTabLists(); // Update for remaining players
            }

            // Reset player visibility
            ItemHandler.resetPlayerVisibility(player);

            // Unload player data
            if (playerDataManager != null) {
                playerDataManager.unloadPlayer(player);
            }

            // Clean up chat manager data
            if (chatManager != null) {
                chatManager.onPlayerDisconnect(player);
            }

            PlayerHandler.onPlayerDisconnect(event);
        });

        // Handle player chat
        globalEventHandler.addListener(PlayerChatEvent.class, event -> {
            if (chatManager != null) {
                chatManager.handlePlayerChat(event);
            }
        });

        // Handle item usage (right-click)
        globalEventHandler.addListener(PlayerUseItemEvent.class, event -> {
            ItemHandler.onPlayerUseItem(event);
        });

        // Handle item drop prevention
        globalEventHandler.addListener(net.minestom.server.event.item.ItemDropEvent.class, event -> {
            // Get the entity that dropped the item (should be a Player)
            if (event.getEntity() instanceof Player player) {
                ItemStack droppedItem = event.getItemStack();
                
                // Check if trying to drop a hub item
                if (droppedItem != null && droppedItem.material() != Material.AIR) {
                    var customName = droppedItem.get(ItemComponent.CUSTOM_NAME);
                    if (customName != null) {
                        String itemName = ((net.kyori.adventure.text.TextComponent) customName).content();
                        if (isHubItem(itemName)) {
                            event.setCancelled(true);
                            // Refresh hub items to ensure they're in the correct slots
                            mythic.hub.handlers.HubItems.giveHubItems(player);
                        }
                    }
                }
            }
        });

        // Handle inventory pre-clicks (this can be cancelled)
        globalEventHandler.addListener(InventoryPreClickEvent.class, event -> {
            Player player = event.getPlayer();
            ItemStack clickedItem = event.getClickedItem();
            
            // Check if this is a player's inventory interaction
            if (event.getInventory() == null || event.getInventory().equals(player.getInventory())) {
                // This is the player's own inventory - check if they're trying to move hub items
                if (clickedItem != null && clickedItem.material() != Material.AIR) {
                    var customName = clickedItem.get(ItemComponent.CUSTOM_NAME);
                    if (customName != null) {
                        String itemName = ((net.kyori.adventure.text.TextComponent) customName).content();
                        
                        // Check if this is a hub item that shouldn't be moved
                        if (isHubItem(itemName)) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
                
                // ADDITIONAL CHECK: Handle number key swaps (1-9 keys)
                // Check if they're trying to swap with a hub item slot
                if (event.getSlot() >= 9) { // Only check inventory slots (not hotbar)
                    int hotbarSlot = -1;
                    
                    // This is tricky - we need to check if they pressed a number key
                    // For now, let's check all hotbar slots for hub items and cancel if any exist
                    for (int i = 0; i <= 8; i++) {
                        ItemStack hotbarItem = player.getInventory().getItemStack(i);
                        if (hotbarItem != null && hotbarItem.material() != Material.AIR) {
                            var customName = hotbarItem.get(ItemComponent.CUSTOM_NAME);
                            if (customName != null) {
                                String itemName = ((net.kyori.adventure.text.TextComponent) customName).content();
                                if (isHubItem(itemName)) {
                                    // Found a hub item in hotbar - cancel any inventory click that could swap with it
                                    event.setCancelled(true);
                                    return;
                                }
                            }
                        }
                    }
                }
            } else {
                // This is a custom inventory (GUI) - prevent all interactions
                event.setCancelled(true);

                if (clickedItem != null && clickedItem.material() != Material.AIR) {
                    String itemName = "";

                    // Get the custom name using the component system
                    var customName = clickedItem.get(ItemComponent.CUSTOM_NAME);
                    if (customName != null) {
                        itemName = ((net.kyori.adventure.text.TextComponent) customName).content();
                    }

                    // Handle menu item clicks
                    if (itemName.equals("Close")) {
                        player.closeInventory();
                    } else if (itemName.equals("Friend Requests")) {
                        player.closeInventory();
                        ItemHandler.handleFriends(player);
                    } else if (itemName.equals("KitPvP") || itemName.equals("Survival") || itemName.equals("Creative")) {
                        player.closeInventory();
                        player.sendMessage(net.kyori.adventure.text.Component.text("Connecting to " + itemName + "...")
                                .color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
                        // Here you would implement server switching logic
                    }
                    // Add more menu item handlers as needed
                }
            }
        });

        System.out.println("Event handlers registered successfully!");
    }

    private static void startUpdaters() {
        // Scoreboard updater
        scheduler.scheduleAtFixedRate(() -> {
            if (scoreboardManager != null) {
                scoreboardManager.updateAllScoreboards();
            }
        }, 1, 5, TimeUnit.SECONDS);

        // Tab list updater (less frequent)
        scheduler.scheduleAtFixedRate(() -> {
            if (tabListManager != null) {
                tabListManager.updateAllTabLists();
            }
        }, 5, 5, TimeUnit.SECONDS);
        
        // Start hub item monitor
        startHubItemMonitor();

        System.out.println("All updaters started!");
    }

    // Helper method to check if an item is a hub item that shouldn't be moved
    private static boolean isHubItem(String itemName) {
        return itemName.equals("Server Selector") ||
               itemName.equals("Player Settings") ||
               itemName.equals("Cosmetics") ||
               itemName.startsWith("Player Visibility:") ||
               itemName.equals("Profile");
    }
    private static void startHubItemMonitor() {
        // Monitor and fix hub item positions every 500ms
        scheduler.scheduleAtFixedRate(() -> {
            for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                ensureHubItemsInCorrectSlots(player);
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
    }
    
    private static void ensureHubItemsInCorrectSlots(Player player) {
        // Expected hub item positions
        int[] hubSlots = {0, 1, 4, 7, 8};
        String[] expectedItems = {"Server Selector", "Player Settings", "Cosmetics", "Player Visibility:", "Profile"};
        
        boolean needsRefresh = false;
        
        for (int i = 0; i < hubSlots.length; i++) {
            int slot = hubSlots[i];
            String expectedItem = expectedItems[i];
            
            ItemStack currentItem = player.getInventory().getItemStack(slot);
            
            // Check if the correct hub item is in the correct slot
            if (currentItem == null || currentItem.material() == Material.AIR) {
                needsRefresh = true;
                break;
            }
            
            var customName = currentItem.get(ItemComponent.CUSTOM_NAME);
            if (customName == null) {
                needsRefresh = true;
                break;
            }
            
            String itemName = ((net.kyori.adventure.text.TextComponent) customName).content();
            if (!itemName.equals(expectedItem) && !itemName.startsWith(expectedItem)) {
                needsRefresh = true;
                break;
            }
        }
        
        // Also check if hub items are in wrong slots (moved to inventory)
        for (int slot = 9; slot < 36; slot++) { // Check main inventory (not hotbar)
            ItemStack item = player.getInventory().getItemStack(slot);
            if (item != null && item.material() != Material.AIR) {
                var customName = item.get(ItemComponent.CUSTOM_NAME);
                if (customName != null) {
                    String itemName = ((net.kyori.adventure.text.TextComponent) customName).content();
                    if (isHubItem(itemName)) {
                        needsRefresh = true;
                        break;
                    }
                }
            }
        }
        
        if (needsRefresh) {
            mythic.hub.handlers.HubItems.giveHubItems(player);
        }
    }
}