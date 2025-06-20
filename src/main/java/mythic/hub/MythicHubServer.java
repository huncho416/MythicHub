package mythic.hub;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.Material;
import mythic.hub.commands.ChatCommands;
import mythic.hub.commands.TagCommand;
import mythic.hub.config.DatabaseConfig;
import mythic.hub.handlers.ItemHandler;
import mythic.hub.handlers.PlayerHandler;
import mythic.hub.managers.ChatManager;
import mythic.hub.managers.PlayerDataManager;
import mythic.hub.managers.ScoreboardManager;
import mythic.hub.managers.ServerManager;
import mythic.hub.managers.TabListManager;
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
    private static ScheduledExecutorService scheduler;
    private static InstanceContainer hubInstance;

    public static void main(String[] args) {
        // Initialize the server
        var server = MinecraftServer.init();

        // Create instance using same logic pattern
        instance = new MythicHubServer();

        // Initialize managers
        instance.initializeManagers();

        // Setup authentication BEFORE instance setup for proper skin loading
        MojangAuth.init();

        // Setup the default instance
        setupDefaultInstance();

        // Register commands
        registerCommands();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down MythicHub Server...");
            if (playerDataManager != null) {
                playerDataManager.shutdown();
            }
            if (serverManager != null) {
                serverManager.shutdown();
            }
        }));

        // Start the server
        server.start("0.0.0.0", 25565);
        System.out.println("MythicPvP Hub Server started successfully!");
    }

    // Add getInstance method using same logic pattern
    public static MythicHubServer getInstance() {
        return instance;
    }

    // Initialize managers method
    private void initializeManagers() {
        System.out.println("Initializing managers...");

        // Initialize database configuration
        DatabaseConfig databaseConfig = new DatabaseConfig();

        // Initialize managers
        scoreboardManager = new ScoreboardManager();
        tabListManager = new TabListManager();
        playerDataManager = new PlayerDataManager(databaseConfig);
        chatManager = new ChatManager();

        // Initialize server manager with server name
        String serverName = System.getProperty("server.name", "Hub-1"); // Can be set via JVM args
        serverManager = new ServerManager(playerDataManager.getRedisManager(), serverName);

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
                System.out.println("Loaded profile for " + player.getUsername() +
                        " - Permissions: " + profile.getActivePermissions().size() +
                        ", Ranks: " + profile.getActiveRanks().size());
            });

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

        // Handle inventory pre-clicks (this can be cancelled)
        globalEventHandler.addListener(InventoryPreClickEvent.class, event -> {
            Player player = event.getPlayer();
            ItemStack clickedItem = event.getClickedItem();

            // Prevent moving items in custom inventories (check if it's NOT the player's inventory)
            if (!event.getInventory().equals(player.getInventory())) {
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
        // In your initialization method, change the scheduler interval
        // Instead of updating every second (1000ms), update every 5 seconds (5000ms)
        scheduler.scheduleAtFixedRate(() -> {
            if (scoreboardManager != null) {
                scoreboardManager.updateAllScoreboards();
            }
        }, 1, 5, TimeUnit.SECONDS); // Changed from 1 second to 5 seconds

        // Tab list updater (less frequent)
        scheduler.scheduleAtFixedRate(() -> {
            if (tabListManager != null) {
                tabListManager.updateAllTabLists();
            }
        }, 5, 5, TimeUnit.SECONDS);

        System.out.println("All updaters started!");
    }
}