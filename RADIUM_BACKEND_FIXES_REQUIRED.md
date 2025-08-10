# Radium Backend Fixes Required for MythicHub Chat Integration

## Current Status
MythicHub has been successfully updated with:
- ✅ **Temporary Fix**: Hardcoded Owner rank formatting for "Expenses" user
- ✅ **Performance Fix**: Reduced console spam with negative caching and reduced logging
- ✅ **Integration**: Complete friend system integration with Radium
- ✅ **Fallback**: Proper error handling when Radium data is missing

## Issues in Radium Backend

Based on the debug logs from MythicHub, the following issues exist in your Radium backend:

### 1. **Missing Player Profiles in Redis** ❌
**Problem**: Player profiles are not being saved to or loaded from Redis correctly.

**Evidence**: 
```
[RadiumClient] No profile data found in Redis for key: radium:profile:429ec85b-317e-44ce-94b4-74bccf266a12
[RadiumClient] Available profile keys in Redis: 2 keys found
[RadiumClient] Profile keys: [radium:profile:ff897faf-7cbe-4c3c-bd10-d4e4f1cb762c, radium:profile:89edf95f-1650-45ca-acbe-4ca74f861fa0]
```

**What's needed in Radium**:
- Ensure the ProfileManager is properly saving profiles to Redis under `radium:profile:<uuid>`
- Verify that profiles are created and saved when players join
- Check that the Redis connection is working and saving data persistently

### 2. **Missing Rank Definitions in Redis** ❌
**Problem**: No rank definitions exist in Redis at all.

**Evidence**:
```
[RadiumClient] Available rank keys in Redis: []
```

**What's needed in Radium**:
- Create default ranks (Member, Admin, Owner) and save them to Redis under `radium:rank:<rankname>`
- Ensure RankManager is properly initialized and saving rank data
- Verify ranks are loaded on server startup

### 3. **User "Expenses" Missing Admin Rank** ❌
**Problem**: The user "Expenses" doesn't have the admin rank assigned in the database.

**Commands to run in Radium**:
```bash
# Create the default ranks
/rank create Member "&a[Member] " 10 "&f"
/rank create Admin "&c[Admin] " 100 "&c" 
/rank create Owner "&4[Owner] " 1000 "&4"

# Grant Owner rank to Expenses
/grant Expenses Owner

# Verify the setup
/rank list
/userinfo Expenses
```

## Required Radium Code Changes

### 1. **Fix Profile Persistence**
Ensure in your `ProfileManager.java`:
```java
// When a player joins, save their profile to Redis
public void saveProfile(Profile profile) {
    String json = gson.toJson(profile);
    redisManager.set("radium:profile:" + profile.getUuid().toString(), json);
    
    // Also publish update notification for real-time sync
    redisManager.publish("radium:profile_update", profile.getUuid().toString());
}
```

### 2. **Fix Rank Persistence**
Ensure in your `RankManager.java`:
```java
// When ranks are created or loaded, save them to Redis
public void saveRank(Rank rank) {
    String json = gson.toJson(rank);
    redisManager.set("radium:rank:" + rank.getName().toLowerCase(), json);
}

// Load all ranks on startup and save to Redis
public void loadRanks() {
    // Load from MongoDB/database
    List<Rank> ranks = rankRepository.findAll();
    
    // Save each to Redis cache
    for (Rank rank : ranks) {
        saveRank(rank);
    }
}
```

### 3. **Ensure Data Initialization**
In your main server startup:
```java
public void onEnable() {
    // ... existing code ...
    
    // Ensure default ranks exist
    createDefaultRanks();
    
    // Load all ranks to Redis cache
    rankManager.loadRanks();
    
    // Load all online player profiles to Redis cache
    refreshPlayerProfiles();
}

private void createDefaultRanks() {
    if (rankManager.getRank("Member") == null) {
        rankManager.createRank("Member", "&a[Member] ", 10, "&f", new HashSet<>(), new ArrayList<>());
    }
    if (rankManager.getRank("Admin") == null) {
        Set<String> adminPerms = Set.of("mythic.staff", "mythic.admin");
        rankManager.createRank("Admin", "&c[Admin] ", 100, "&c", adminPerms, new ArrayList<>());
    }
    if (rankManager.getRank("Owner") == null) {
        Set<String> ownerPerms = Set.of("*");
        rankManager.createRank("Owner", "&4[Owner] ", 1000, "&4", ownerPerms, new ArrayList<>());
    }
}
```

## Testing Steps

After making these changes in Radium:

1. **Restart Radium server**
2. **Run the setup commands** listed above
3. **Verify data in Redis**: 
   - Check `radium:profile:*` keys exist
   - Check `radium:rank:*` keys exist
4. **Test in MythicHub**: The hardcoded formatting should no longer be needed

## Expected Data Format

### Profile in Redis (`radium:profile:<uuid>`):
```json
{
    "uuid": "429ec85b-317e-44ce-94b4-74bccf266a12",
    "username": "Expenses",
    "ranks": ["Owner|console|1723267200000|0"],
    "permissions": ["mythic.admin|console|1723267200000|0"],
    "lastSeen": 1723267200000
}
```

### Rank in Redis (`radium:rank:<rankname>`):
```json
{
    "name": "Owner",
    "prefix": "&4[Owner] ",
    "weight": 1000,
    "color": "&4",
    "permissions": ["*"],
    "inherits": []
}
```

## Current Workaround

Until these fixes are implemented in Radium, MythicHub has a **temporary hardcoded override** that gives "Expenses" the Owner rank formatting (`&4[Owner] &4Expenses`) in both chat and tab list.

This override can be removed once Radium is properly saving and loading profile/rank data.
