# Velocity Integration for MythicHub

## Overview
MythicHub has been integrated with Velocity proxy support to work with your Radium backend proxy s4. **"Invalid Version, please use 1.21" error** ⚠️ **CRITICAL ISSUE**
   
   **Error Analysis**: Based on the error message format, this is coming from the **Velocity proxy layer**, not MythicHub.
   
   **Immediate Test**:
   ```
   1. Connect directly to localhost:25566 with 1.21 client
   2. If direct connection works → Issue is in Velocity/Radium proxy
   3. If direct connection fails → Issue is in MythicHub (rare)
   ```

5. **"A proxy plugin caused an illegal protocol state" error** ⚠️ **PROXY CONFIGURATION ISSUE**
   
   **Root Cause**: This error occurs when there's a protocol mismatch between Velocity proxy and MythicHub server.
   
   **Common Causes**:
   - Connecting directly to MythicHub (port 25566) when it's configured for proxy mode
   - Velocity forwarding secret mismatch
   - Incorrect forwarding mode configuration
   
   **Resolution Steps**:
   
   a) **Always connect through proxy**: Use `localhost:25565` (NOT 25566)
   
   b) **Verify forwarding secrets match**:
   ```bash
   # Check Velocity secret file:
   cat "C:\Users\xxxga\Desktop\Development\Radium\run\forwarding.secret"
   # Should contain: radium-velocity-secret
   ```
   
   c) **Verify Velocity configuration**:
   ```toml
   player-info-forwarding-mode = "modern"
   forwarding-secret-file = "forwarding.secret"
   # NOT: forwarding-secret = "..."
   ```
   
   d) **If you need to bypass proxy temporarily**:
   - Disable Velocity forwarding in MythicHub
   - Comment out the VelocityProxy.enable() line
   - Restart MythicHub and connect directly to port 25566s setup allows the hub to communicate with the proxy running on port 25565 while the hub itself runs on port 25566.

## Features Added

### 1. Velocity Simple Proxy (No Forwarding)
- Velocity acts as a simple proxy/router
- **No player data forwarding** (UUID, IP, etc.)
- **Authentication handled by Velocity** (online-mode = true)
- **Maximum compatibility** with all Minecraft versions

### 2. Proxy Communication via Redis
- **Server Registration**: Hub automatically registers with Radium proxy
- **Player Count Updates**: Sends real-time player count every 30 seconds
- **Player Transfers**: Can send players to other servers through proxy
- **Global Messaging**: Support for network-wide messages

### 3. Server Commands
- `/server` - Show available servers
- `/server <name>` - Join a specific server (pvp, survival, creative, skyblock)
- `/join <name>` - Alias for server command
- `/play <name>` - Alias for server command

## Configuration

### Velocity Config (velocity.toml)
Your Velocity proxy is now configured with these settings:

```toml
# Basic configuration
config-version = "2.7"
bind = "0.0.0.0:25565"
motd = "&3A Velocity Server"
show-max-players = 500
online-mode = true
prevent-client-proxy-connections = false

# Version support - CRITICAL for 1.21 compatibility
force-key-authentication = false
announce-forge = false

# No forwarding configuration ✅ CONFIGURED (Simplest)
player-info-forwarding-mode = "none"
# forwarding-secret-file = "forwarding.secret" (not needed)

# Ping passthrough settings
ping-passthrough = "mods"

# Server configurations
[servers]
lobby = "127.0.0.1:25566"
pvp = "127.0.0.1:25567"
survival = "127.0.0.1:25568"
creative = "127.0.0.1:25569"
skyblock = "127.0.0.1:25570"

# Try order for connecting players
try = ["lobby"]
```

**Note**: This configuration uses simple proxy routing without player data forwarding for maximum compatibility.

### Redis Channels Used
- `radium:proxy` - Main proxy communication
- `radium:server:register` - Server registration
- `radium:player:count` - Player count updates

## Server Information Registered
When the hub starts, it registers these details with the proxy:
```json
{
  "name": "hub",
  "type": "hub", 
  "host": "localhost",
  "port": 25566,
  "motd": "MythicHub - Lobby Server",
  "maxPlayers": 1000,
  "status": "online",
  "timestamp": 1234567890
}
```

## Usage

### Starting the Hub
```bash
./gradlew run
```

The hub will:
1. Start on port 25566
2. Connect to Redis (localhost:6379)
3. Register with Radium proxy
4. Begin sending player count updates

### Player Commands
Players can use these commands in the hub:
- `/server` - Lists all available servers
- `/server pvp` - Connects to PvP server
- `/server survival` - Connects to Survival server
- `/server creative` - Connects to Creative server
- `/server skyblock` - Connects to Skyblock server

### Proxy Communication Examples

**Send Player to Another Server:**
```java
ProxyManager proxy = MythicHubServer.getInstance().getProxyManager();
proxy.sendPlayerToServer(player, "pvp");
```

**Send Global Message:**
```java
proxy.sendGlobalMessage("Server maintenance in 5 minutes!");
```

**Request Server List:**
```java
proxy.requestServerList();
```

## Architecture Flow

1. **Player Connects** → Velocity Proxy (port 25565) 
2. **Proxy Routes** → MythicHub (port 25566) [No data forwarding]
3. **Player Uses Command** → `/server pvp`
4. **Hub Communicates** → Redis → Radium Proxy
5. **Proxy Transfers** → Player to PvP Server

**Note**: With `player-info-forwarding-mode = "none"`, all players appear to MythicHub as coming from the proxy IP (127.0.0.1).

## Troubleshooting

### Common Issues

1. **"Proxy manager not available"**
   - Check Redis connection
   - Verify proxy manager initialization

2. **"Unknown server" error**
   - Make sure the server name matches the valid servers list
   - Valid servers: pvp, survival, creative, skyblock

3. **Player transfer fails**
   - Check Redis pub/sub is working
   - Verify target server is registered and online
   - Check Velocity proxy logs

4. **"A proxy plugin caused an illegal protocol state" error** ⚠️ **CRITICAL ISSUE**
   
   **Error Analysis**: This error occurs when there's a mismatch between Velocity's forwarding configuration and MythicHub's forwarding settings.
   
   **Root Causes**:
   - Velocity is set to `player-info-forwarding-mode = "legacy"` but MythicHub expects modern forwarding
   - Connecting directly to MythicHub (port 25566) when Velocity forwarding is enabled
   - Forwarding secret mismatch between Velocity and MythicHub
   
   **Solution**:
   1. **✅ COMPLETED - Velocity Configuration** (REQUIRED for Minestom):
      ```toml
      # In velocity.toml - ALREADY CONFIGURED
      player-info-forwarding-mode = "modern"
      forwarding-secret-file = "forwarding.secret"
      ```
   
   2. **✅ COMPLETED - Forwarding Secret Matches**:
      ```bash
      # In forwarding.secret file - ALREADY SET
      radium-velocity-secret
      ```
   
   3. **Connection Mode** (Follow this):
      - **For proxy connection**: Keep Velocity forwarding enabled (default), connect to port 25565 ✅
      - **For direct connection**: Disable Velocity forwarding with `-Dvelocity.enabled=false`, connect to port 25566
   
   **Test Steps**:
   ```bash
   # 1. Start Velocity proxy first
   cd "C:\Users\xxxga\Desktop\Development\Radium\run"
   java -jar velocity-*.jar
   
   # 2. Start MythicHub second (in new terminal)
   cd "C:\Users\xxxga\Desktop\Development\MythicHub" 
   .\gradlew run
   
   # 3. Connect via proxy (localhost:25565) NOT direct (localhost:25566)
   ```
   2. If direct connection works → Issue is in Velocity/Radium proxy
   3. If direct connection fails → Issue is in MythicHub (rare)
   ```
   
   **Step-by-Step Resolution**:
   
   a) **Velocity Configuration Check**:
   ```toml
   # In velocity.toml - MUST have these exact settings:
   force-key-authentication = false
   online-mode = true
   announce-forge = false
   config-version = "2.7"
   # Do NOT add any version restrictions
   ```
   
   b) **Radium Plugin Configuration**:
   - **Location**: `velocity/plugins/radium/` directory  
   - **Look for**: `config.yml`, `settings.yml`, or similar files
   - **Remove**: Any `version-check`, `protocol-version`, or `minecraft-version` restrictions
   - **Example problematic settings to remove**:
   ```yaml
   version-check: true
   allowed-versions: ["1.21"]
   protocol-version: 767
   ```
   
   c) **Velocity Version Requirements**:
   ```bash
   # Check your Velocity version
   # Minimum required: Velocity 3.3.0+ for Minecraft 1.21 support
   # Update if running older version
   ```
   
   d) **Debug Connection Flow**:
   ```
   Client (1.21) → Velocity (25565) → [VERSION CHECK HAPPENS HERE] → MythicHub (25566)
                                        ↑
                                   ERROR OCCURS HERE
   ```
   
   e) **Log Analysis**:
   - **Velocity logs**: Look for protocol version messages
   - **Radium logs**: Check for version enforcement messages
   - **Expected**: "Player connected with protocol version 767" (1.21)
   - **Problem**: "Rejecting player due to version" or similar

6. **Emergency Workaround**:
   If you need immediate access, players can connect directly to:
   ```
   Server Address: localhost:25566
   ```
   This bypasses the proxy and connects directly to MythicHub.

7. **"Server address too long" and packet corruption errors** ⚠️ **FORWARDING MODE MISMATCH**
   
   **Error Symptoms**:
   ```
   IllegalArgumentException: Server address too long: 1466
   String is too long (length: 69, readable: 23)
   WARNING: Packet (HANDSHAKE) 0x0 not fully read
   ```
   
   **Root Cause**: Velocity is sending legacy forwarding packets but MythicHub expects modern forwarding packets.
   
   **Solution**:
   1. **✅ FIXED - Ensure Velocity uses modern forwarding**:
      ```toml
      # In velocity.toml - CORRECTED
      player-info-forwarding-mode = "modern"
      forwarding-secret-file = "forwarding.secret"
      ```
   
   2. **Restart both services** (CRITICAL):
      ```bash
      # 1. Stop both Velocity and MythicHub completely
      # 2. Start Velocity first
      cd "C:\Users\xxxga\Desktop\Development\Radium\run"
      java -jar velocity-*.jar
      
      # 3. Start MythicHub second (new terminal)
      cd "C:\Users\xxxga\Desktop\Development\MythicHub"
      .\gradlew run
      ```
   
   **Verification**: Look for "Using modern forwarding" in Velocity logs and "Velocity forwarding enabled" in MythicHub logs.

### Debug Commands
```bash
# Check Redis connectivity
docker exec radium-redis redis-cli -a "password" ping

# Monitor Redis channels
docker exec radium-redis redis-cli -a "password" monitor

# Check running containers
docker ps
```

## Security Notes

- The forwarding secret (`radium-velocity-secret`) should match between Velocity and MythicHub
- Modern forwarding prevents IP spoofing and UUID forgery
- Only trusted servers should be able to communicate via Redis

## Version Support

### Minecraft 1.21 Support ✅
MythicHub supports Minecraft 1.21 and maintains backward compatibility with:
- 1.20.x (full support)  
- 1.21 (full support)

The server automatically handles version compatibility and will accept clients from any supported version through the Velocity proxy.

### Version Configuration
- **Target Version**: 1.21
- **Protocol Compatibility**: Enabled for all 1.20+ clients  
- **Version Check**: Disabled (accepts all compatible versions)
- **Brand Name**: `MythicHub 1.21`
- **Proxy Forwarding**: Handles version translation automatically
