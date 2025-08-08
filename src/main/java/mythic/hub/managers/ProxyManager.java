package mythic.hub.managers;

import com.google.gson.Gson;
import mythic.hub.config.VelocityConfig;
import mythic.hub.database.RedisManager;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ProxyManager {
    private final RedisManager redisManager;
    private final VelocityConfig velocityConfig;
    private final Gson gson;
    private final ScheduledExecutorService scheduler;
    
    private static final String PROXY_CHANNEL = "radium:proxy";
    private static final String SERVER_REGISTER_CHANNEL = "radium:server:register";
    private static final String PLAYER_COUNT_CHANNEL = "radium:player:count";
    
    public ProxyManager(RedisManager redisManager, VelocityConfig velocityConfig) {
        this.redisManager = redisManager;
        this.velocityConfig = velocityConfig;
        this.gson = new Gson();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        
        if (velocityConfig.shouldRegisterWithProxy()) {
            registerWithProxy();
            startPlayerCountUpdates();
        }
    }

    /**
     * Register this server with the Radium proxy
     */
    private void registerWithProxy() {
        try {
            Map<String, Object> serverInfo = new HashMap<>();
            serverInfo.put("name", velocityConfig.getServerName());
            serverInfo.put("type", "hub");
            serverInfo.put("host", "localhost");
            serverInfo.put("port", 25566);
            serverInfo.put("motd", "MythicHub - Lobby Server");
            serverInfo.put("maxPlayers", 1000);
            serverInfo.put("status", "online");
            serverInfo.put("timestamp", System.currentTimeMillis());
            
            String jsonData = gson.toJson(serverInfo);
            redisManager.publish(SERVER_REGISTER_CHANNEL, jsonData);
            
            System.out.println("Registered hub server with Radium proxy");
        } catch (Exception e) {
            System.err.println("Failed to register with proxy: " + e.getMessage());
        }
    }

    /**
     * Start periodic player count updates to the proxy
     */
    private void startPlayerCountUpdates() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updatePlayerCount();
            } catch (Exception e) {
                System.err.println("Failed to update player count: " + e.getMessage());
            }
        }, 10, 30, TimeUnit.SECONDS);
    }

    /**
     * Send current player count to proxy
     */
    private void updatePlayerCount() {
        int playerCount = MinecraftServer.getConnectionManager().getOnlinePlayers().size();
        
        Map<String, Object> countData = new HashMap<>();
        countData.put("server", velocityConfig.getServerName());
        countData.put("playerCount", playerCount);
        countData.put("timestamp", System.currentTimeMillis());
        
        String jsonData = gson.toJson(countData);
        redisManager.publish(PLAYER_COUNT_CHANNEL, jsonData);
    }

    /**
     * Send a player to another server through the proxy
     */
    public void sendPlayerToServer(Player player, String serverName) {
        try {
            Map<String, Object> transferData = new HashMap<>();
            transferData.put("type", "player_transfer");
            transferData.put("player", player.getUuid().toString());
            transferData.put("playerName", player.getUsername());
            transferData.put("fromServer", velocityConfig.getServerName());
            transferData.put("toServer", serverName);
            transferData.put("timestamp", System.currentTimeMillis());
            
            String jsonData = gson.toJson(transferData);
            redisManager.publish(PROXY_CHANNEL, jsonData);
            
            // Kick player with transfer message
            player.kick(net.kyori.adventure.text.Component.text("Transferring to " + serverName + "..."));
            
        } catch (Exception e) {
            System.err.println("Failed to transfer player " + player.getUsername() + " to " + serverName + ": " + e.getMessage());
        }
    }

    /**
     * Send a global message through the proxy to all servers
     */
    public void sendGlobalMessage(String message) {
        try {
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("type", "global_message");
            messageData.put("message", message);
            messageData.put("fromServer", velocityConfig.getServerName());
            messageData.put("timestamp", System.currentTimeMillis());
            
            String jsonData = gson.toJson(messageData);
            redisManager.publish(PROXY_CHANNEL, jsonData);
            
        } catch (Exception e) {
            System.err.println("Failed to send global message: " + e.getMessage());
        }
    }

    /**
     * Get list of online servers from proxy
     */
    public void requestServerList() {
        try {
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("type", "server_list_request");
            requestData.put("fromServer", velocityConfig.getServerName());
            requestData.put("timestamp", System.currentTimeMillis());
            
            String jsonData = gson.toJson(requestData);
            redisManager.publish(PROXY_CHANNEL, jsonData);
            
        } catch (Exception e) {
            System.err.println("Failed to request server list: " + e.getMessage());
        }
    }

    /**
     * Notify proxy that this server is shutting down
     */
    public void unregisterFromProxy() {
        try {
            Map<String, Object> unregisterData = new HashMap<>();
            unregisterData.put("type", "server_unregister");
            unregisterData.put("server", velocityConfig.getServerName());
            unregisterData.put("timestamp", System.currentTimeMillis());
            
            String jsonData = gson.toJson(unregisterData);
            redisManager.publish(PROXY_CHANNEL, jsonData);
            
            System.out.println("Unregistered from Radium proxy");
        } catch (Exception e) {
            System.err.println("Failed to unregister from proxy: " + e.getMessage());
        }
    }

    public void shutdown() {
        unregisterFromProxy();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
