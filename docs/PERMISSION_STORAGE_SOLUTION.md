# NeoEssentials Persistent Permission System

## ✅ **PROBLEM SOLVED: Permissions Now Persist Through Server Restarts**

The permission system has been completely overhauled with **persistent storage** to prevent permissions from being reset to defaults.

### 🔧 **What Was Fixed**

**Before (Problem):**
- ❌ Group assignments lost on server restart
- ❌ Individual player permissions wiped
- ❌ No permanent storage system
- ❌ Always reverted to defaults

**After (Solution):**
- ✅ **Groups persist permanently** in `neoessentials/permissions/groups.json`
- ✅ **Player assignments persist** in `neoessentials/permissions/players/[UUID].json`
- ✅ **Individual permissions persist** with backup system
- ✅ **Automatic storage** on every permission change

### 📁 **Storage Structure**

```
neoessentials/
└── permissions/
    ├── groups.json              # All permission groups with their permissions
    ├── players/                 # Individual player data
    │   ├── [player-uuid].json   # Each player's group and custom permissions
    │   └── [player-uuid].json   # ...
    └── backups/                 # Automatic backups
        ├── groups_[timestamp].json
        └── [player-uuid]_[timestamp].json
```

### 🎯 **Group Management Commands (Now Persistent)**

```bash
# Create persistent group
/permissions group create vip "VIP Member" 10

# Add permission to group (saved immediately)
/permissions group permission add vip neoessentials.fly

# Set group inheritance (saved immediately)
/permissions group inheritance vip default

# Modify group properties (saved immediately)
/permissions group prefix vip "&6[VIP] "
/permissions group suffix vip " &6♦"
/permissions group priority vip 15
```

### 👤 **Player Management Commands (Now Persistent)**

```bash
# Set player group (saved immediately)
/permissions user Steve group set vip

# Add individual permission (saved immediately)
/permissions user Steve permission add neoessentials.god

# Remove individual permission (saved immediately)
/permissions user Steve permission remove neoessentials.god

# All changes are automatically saved to storage!
```

### 💾 **Storage Features**

1. **Automatic Saving**: Every permission change is immediately saved
2. **Backup System**: Automatic backups before modifications
3. **Cache System**: Fast access with persistent fallback
4. **Error Recovery**: Graceful handling of corrupted data
5. **Migration Safe**: Existing permissions are preserved

### 🔄 **How It Works**

1. **On Server Start**:
   - Loads groups from `groups.json`
   - If no saved groups exist, creates defaults and saves them
   - Ready to load player data on demand

2. **When Player Joins**:
   - Loads player data from `players/[UUID].json`
   - If no file exists, assigns default group and creates file
   - Caches data for fast access

3. **When Permissions Change**:
   - Updates in-memory cache
   - Immediately saves to persistent storage
   - Creates backup of previous state

4. **On Server Restart**:
   - All permissions are restored exactly as they were
   - No data loss, no reset to defaults

### 🛡️ **Backup & Safety**

- **Automatic Backups**: Created before any modifications
- **Timestamp System**: All backups have timestamps
- **Error Handling**: Falls back to defaults only if data is completely corrupted
- **Migration Support**: Existing configurations are preserved

### 📊 **Example Storage Files**

**groups.json:**
```json
{
  "vip": {
    "name": "vip",
    "prefix": "&6[VIP] ",
    "suffix": " &6♦",
    "priority": 10,
    "inheritance": "default",
    "permissions": [
      "neoessentials.fly",
      "neoessentials.heal",
      "neoessentials.feed"
    ],
    "lastUpdated": 1723142400000
  }
}
```

**players/[uuid].json:**
```json
{
  "playerUUID": "550e8400-e29b-41d4-a716-446655440000",
  "groupName": "vip",
  "permissions": {
    "neoessentials.god": true,
    "neoessentials.gamemode": false
  },
  "lastUpdated": 1723142400000
}
```

### 🚀 **Immediate Benefits**

1. **No More Permission Loss**: Server restarts won't reset permissions
2. **Reliable Group System**: Group assignments persist permanently
3. **Individual Permission Storage**: Custom player permissions are saved
4. **Administrative Confidence**: Changes are permanent and backed up
5. **Easy Management**: All existing commands work the same way

### ⚡ **Usage Instructions**

**For Server Administrators:**
1. Use any permission commands as before
2. Changes are automatically saved - no manual saving needed
3. Server restarts will preserve all permissions
4. Check `neoessentials/permissions/` directory for stored data

**For Developers:**
- New `PermissionStorageManager` handles all persistence
- Integrated with existing `CustomPermissionsManager`
- Backward compatible with all existing code

---

**🎉 Result: Your permission system now has bulletproof persistence! No more lost permissions on server restarts.**
