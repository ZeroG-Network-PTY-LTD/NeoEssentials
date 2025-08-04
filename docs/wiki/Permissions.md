# Permissions System

NeoEssentials includes a comprehensive permission system with group-based management, inheritance, wildcards, and temporary permissions. This system can work standalone or integrate with external permission plugins.

## 🎯 Overview

The permission system provides:
- **Group-based permissions** with inheritance
- **Wildcard permission support** (`essentials.*`)
- **Permission negation** (`-permission.node`)
- **Temporary permissions** with automatic expiration
- **High-performance caching** for fast permission checks
- **Integration with external systems** (LuckPerms, etc.)

## 👥 Permission Groups

### Default Groups

NeoEssentials creates four default permission groups:

#### Default Group
**Description**: Basic permissions for all players
**Priority**: 0
**Permissions**:
```
essentials.home
essentials.sethome
essentials.delhome
essentials.homes
essentials.warp
essentials.warps
essentials.spawn
essentials.back
neoessentials.placeholder.test
```

#### VIP Group
**Description**: Enhanced permissions for VIP players
**Priority**: 10
**Inherits**: Default
**Additional Permissions**:
```
essentials.fly
essentials.heal
essentials.feed
essentials.workbench
essentials.anvil
essentials.enderchest
essentials.repair
neoessentials.bossbar.show
```

#### Moderator Group
**Description**: Moderation permissions for staff
**Priority**: 50
**Inherits**: VIP
**Additional Permissions**:
```
essentials.kick
essentials.mute
essentials.unmute
essentials.jail
essentials.unjail
essentials.vanish
essentials.god
essentials.invsee
neoessentials.bossbar.broadcast
neoessentials.security.view
```

#### Admin Group
**Description**: Full administrative permissions
**Priority**: 100
**Inherits**: Moderator
**Additional Permissions**:
```
essentials.*
neoessentials.*
```

## 🎮 Commands

### User Information

#### `/permissions info [player]`
Display permission information for a player.

**Examples**:
```bash
# View your own permissions
/permissions info

# View another player's permissions
/permissions info Steve
```

**Output**:
```
=== Permission Info for Steve ===
Group: VIP (Priority: 10)
Permissions: 25 total
- essentials.fly ✓
- essentials.heal ✓
- essentials.kick ✗
Temporary Permissions: 2 active
```

#### `/permissions check <player> <permission>`
Test if a player has a specific permission.

**Examples**:
```bash
# Check if player has permission
/permissions check Steve essentials.fly

# Check negative permission
/permissions check Alex essentials.kick
```

### Group Management

#### `/permissions group list`
List all available permission groups.

**Example Output**:
```
=== Permission Groups ===
1. Default (Priority: 0) - 12 permissions
2. VIP (Priority: 10) - 8 additional permissions
3. Moderator (Priority: 50) - 15 additional permissions
4. Admin (Priority: 100) - All permissions
```

#### `/permissions group info <group>`
Show detailed information about a permission group.

**Example**:
```bash
/permissions group info VIP
```

**Output**:
```
=== Group: VIP ===
Priority: 10
Inherits: Default
Prefix: "&6[VIP] "
Suffix: ""
Permissions: 8 direct, 20 total
Direct Permissions:
- essentials.fly
- essentials.heal
- essentials.feed
[...]
```

#### `/permissions group create <name> <prefix> [priority]`
Create a new permission group.

**Examples**:
```bash
# Create basic group
/permissions group create Builder "&e[Builder] " 25

# Create group with priority
/permissions group create Helper "&b[Helper] " 30
```

#### `/permissions group delete <group>`
Delete a permission group.

**Example**:
```bash
/permissions group delete Builder
```

**Note**: Cannot delete groups that have members or are inherited by other groups.

### Group Permissions

#### `/permissions group permission add <group> <permission>`
Add a permission to a group.

**Examples**:
```bash
# Add specific permission
/permissions group permission add VIP essentials.speed

# Add wildcard permission
/permissions group permission add Admin essentials.*

# Add negative permission (deny)
/permissions group permission add Default -essentials.give
```

#### `/permissions group permission remove <group> <permission>`
Remove a permission from a group.

**Example**:
```bash
/permissions group permission remove VIP essentials.speed
```

### Group Inheritance

#### `/permissions group inheritance <group> <parent>`
Set inheritance for a permission group.

**Examples**:
```bash
# Set VIP to inherit from Default
/permissions group inheritance VIP Default

# Remove inheritance (set to none)
/permissions group inheritance Builder none
```

### User Management

#### `/permissions user <player> info`
Show detailed permission information for a user.

**Example**:
```bash
/permissions user Steve info
```

#### `/permissions user <player> group set <group>`
Set a player's primary permission group.

**Examples**:
```bash
# Promote player to VIP
/permissions user Steve group set VIP

# Demote player to Default
/permissions user Alex group set Default
```

#### `/permissions user <player> permission add <permission>`
Add a direct permission to a player.

**Examples**:
```bash
# Give specific permission
/permissions user Steve permission add essentials.speed

# Give negative permission
/permissions user Griefer permission add -essentials.build
```

#### `/permissions user <player> permission remove <permission>`
Remove a direct permission from a player.

**Example**:
```bash
/permissions user Steve permission remove essentials.speed
```

#### `/permissions user <player> permission temp <permission> <duration>`
Give a player temporary permission.

**Examples**:
```bash
# 1 hour temporary fly permission
/permissions user Steve permission temp essentials.fly 1h

# 30 minutes temporary god mode
/permissions user Alex permission temp essentials.god 30m

# 7 days temporary VIP access
/permissions user NewPlayer permission temp essentials.fly 7d
```

**Duration Formats**:
- `s` - seconds
- `m` - minutes
- `h` - hours
- `d` - days
- `w` - weeks

#### `/permissions user <player> clear`
Remove all custom permissions from a player (keeps group permissions).

**Example**:
```bash
/permissions user Steve clear
```

### System Commands

#### `/permissions reload`
Reload the permission system configuration.

**Example**:
```bash
/permissions reload
```

#### `/permissions stats`
Show permission system statistics.

**Example Output**:
```
=== Permission Statistics ===
Total Users: 157
Total Groups: 4
Cached Permissions: 892
Temporary Permissions: 12
Cache Hit Rate: 94.2%
Last Cleanup: 2 minutes ago
```

## 🔧 Permission Syntax

### Basic Permissions
```
essentials.heal          # Allow /heal command
essentials.fly           # Allow /fly command
neoessentials.bossbar.*  # All bossbar permissions
```

### Wildcard Permissions
```
essentials.*             # All essentials permissions
neoessentials.*          # All NeoEssentials permissions
*.admin                  # All admin permissions across plugins
*                        # ALL permissions (dangerous!)
```

### Negative Permissions
```
-essentials.give         # Deny /give command
-essentials.gamemode.*   # Deny all gamemode commands
-neoessentials.admin     # Deny admin access
```

### Permission Hierarchy
When checking permissions, the system follows this order:
1. **Direct user permissions** (positive and negative)
2. **Group permissions** (by priority, highest first)
3. **Inherited group permissions**
4. **Default permissions**

## 📋 Permission Nodes

### Essential Commands
```
essentials.heal                    # /heal command
essentials.heal.others             # Heal other players
essentials.feed                    # /feed command
essentials.feed.others             # Feed other players
essentials.god                     # /god command
essentials.god.others              # Toggle god mode for others
essentials.fly                     # /fly command
essentials.fly.others              # Toggle flight for others
essentials.vanish                  # /vanish command
essentials.vanish.others           # Toggle vanish for others
essentials.speed                   # /speed command
essentials.speed.others            # Change speed for others
essentials.repair                  # /repair command
essentials.repair.all              # Repair all items
essentials.give                    # /give command
essentials.gamemode                # /gamemode command
essentials.gamemode.others         # Change gamemode for others
essentials.time                    # /time command
essentials.weather                 # /weather command
essentials.workbench               # /workbench command
essentials.anvil                   # /anvil command
essentials.enderchest              # /enderchest command
essentials.enderchest.others       # View others' enderchests
essentials.invsee                  # /invsee command
```

### Teleportation
```
essentials.home                    # /home command
essentials.sethome                 # /sethome command
essentials.delhome                 # /delhome command
essentials.homes                   # /homes command
essentials.warp                    # /warp command
essentials.setwarp                 # /setwarp command (admin)
essentials.delwarp                 # /delwarp command (admin)
essentials.warps                   # /warps command
essentials.spawn                   # /spawn command
essentials.setspawn                # /setspawn command (admin)
essentials.back                    # /back command
essentials.tp                      # /tp command
essentials.tpa                     # /tpa command
essentials.tpaccept                # /tpaccept command
essentials.tpdeny                  # /tpdeny command
```

### Moderation
```
essentials.kick                    # /kick command
essentials.ban                     # /ban command
essentials.unban                   # /unban command
essentials.tempban                 # /tempban command
essentials.mute                    # /mute command
essentials.unmute                  # /unmute command
essentials.jail                    # /jail command
essentials.unjail                  # /unjail command
```

### NeoEssentials Features
```
neoessentials.bossbar.show         # Show bossbars
neoessentials.bossbar.show.others  # Show bossbars to others
neoessentials.bossbar.broadcast    # Broadcast bossbars
neoessentials.bossbar.create       # Create custom bossbars
neoessentials.bossbar.update       # Update bossbars
neoessentials.bossbar.hide         # Hide bossbars
neoessentials.bossbar.templates    # List templates

neoessentials.placeholder.test     # Test placeholders
neoessentials.placeholder.list     # List placeholders
neoessentials.placeholder.info     # View placeholder info
neoessentials.placeholder.reload   # Reload placeholders

neoessentials.security.view        # View security events
neoessentials.security.admin       # Security administration

neoessentials.reload               # Reload configuration
neoessentials.info                 # View mod information
neoessentials.debug                # Debug commands
```

### Administrative
```
neoessentials.admin                # All admin permissions
neoessentials.permissions.*        # All permission commands
essentials.admin                   # All essentials admin permissions
```

## ⚙️ Configuration

### Permission System Settings

```toml
[permissions]
# Use built-in permission system
useBuiltinPermissions = true

# Enable group inheritance
enableInheritance = true

# Enable wildcard permissions
enableWildcards = true

# Enable negative permissions
enableNegativePermissions = true

# Enable temporary permissions
enableTemporaryPermissions = true

# Permission cache settings
[permissions.cache]
# Cache duration in seconds
cacheTime = 300

# Maximum cached permissions
maxCacheSize = 10000

# Enable cache statistics
enableStatistics = true

# Cleanup settings
[permissions.cleanup]
# Cleanup interval in minutes
cleanupInterval = 5

# Remove expired temporary permissions
removeExpired = true

# Log cleanup operations
logCleanup = false
```

### Default Group Configuration

```toml
[permissions.groups.default]
name = "Default"
priority = 0
prefix = ""
suffix = ""
permissions = [
    "essentials.home",
    "essentials.sethome",
    "essentials.spawn",
    "essentials.back"
]

[permissions.groups.vip]
name = "VIP"
priority = 10
inherits = "Default"
prefix = "&6[VIP] "
suffix = ""
permissions = [
    "essentials.fly",
    "essentials.heal",
    "essentials.feed"
]
```

### Integration Settings

```toml
[permissions.integration]
# External permission plugin integration
[permissions.integration.luckperms]
enabled = false
syncGroups = true
syncPermissions = true

[permissions.integration.other]
# Other permission plugin compatibility
fallbackToVanilla = true
respectOpStatus = true
```

## 🚀 Advanced Features

### Group Templates

Create group templates for easy setup:

```bash
# Create staff template
/permissions template create staff "Staff Template" \
  essentials.kick essentials.mute essentials.vanish

# Apply template to group
/permissions group apply Moderator staff
```

### Batch Operations

```bash
# Give multiple permissions at once
/permissions user Steve permission batch add \
  essentials.fly essentials.heal essentials.feed

# Remove multiple permissions
/permissions group VIP permission batch remove \
  essentials.give essentials.gamemode
```

### Permission Queries

```bash
# List all players with specific permission
/permissions query has essentials.fly

# List all players in group
/permissions query group VIP

# List all permissions containing keyword
/permissions query permission heal
```

### Scheduled Operations

```bash
# Schedule temporary promotion
/permissions schedule user Steve group set VIP 7d

# Schedule permission removal
/permissions schedule user Probation permission remove essentials.build 24h
```

## 🔄 Integration Examples

### With External Plugins

#### LuckPerms Integration
```toml
[permissions.integration.luckperms]
enabled = true
syncGroups = true
useNeoEssentialsGroups = false
respectLuckPermsPrefix = true
```

#### Vault Integration
```java
// Example vault integration
if (Vault.isAvailable()) {
    Permission vaultPerms = Vault.getPermission();
    // Sync with Vault API
}
```

### With Custom Systems

```java
// Custom permission check
public boolean hasCustomPermission(Player player, String permission) {
    CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
    return manager.hasPermission(player.getUUID(), permission);
}

// Event-based permission updates
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    // Apply automatic permissions based on criteria
    if (isFirstTimePlayer(player)) {
        permissionManager.addTemporaryPermission(
            player.getUUID(), 
            "essentials.newbie", 
            Duration.ofHours(24)
        );
    }
}
```

## 🛡️ Security Considerations

### Best Practices
1. **Principle of least privilege** - Give minimal necessary permissions
2. **Regular audit** - Review permissions periodically
3. **Temporary over permanent** - Use temporary permissions when possible
4. **Group over individual** - Prefer group-based permissions
5. **Negative permissions** - Use to restrict access granularly

### Security Features
- **Permission validation** - Prevents invalid permission assignments
- **Audit logging** - Tracks all permission changes
- **Automatic cleanup** - Removes expired temporary permissions
- **Cache security** - Prevents permission cache poisoning

## 🔍 Troubleshooting

### Common Issues

#### Permission Not Working
1. Check permission node spelling
2. Verify group inheritance chain
3. Check for negative permissions
4. Clear permission cache

#### Group Inheritance Problems
1. Verify parent group exists
2. Check for circular inheritance
3. Review group priorities
4. Test with `/permissions debug inheritance`

#### Performance Issues
1. Reduce cache time for dynamic permissions
2. Increase cache size for large servers
3. Monitor cleanup frequency
4. Use `/permissions stats` to check performance

### Debug Commands

```bash
# Debug permission calculation
/permissions debug player Steve essentials.fly

# Show inheritance chain
/permissions debug inheritance VIP

# Cache statistics
/permissions debug cache

# Performance metrics
/permissions debug performance
```

## 📊 Monitoring & Analytics

### Permission Usage Statistics

```bash
# Most used permissions
/permissions stats usage

# Group distribution
/permissions stats groups

# Temporary permission summary
/permissions stats temporary
```

### Audit Logs

Permission changes are logged for security:
```
[2025-08-03 14:30:15] [PERMISSION] Admin gave essentials.fly to Steve (permanent)
[2025-08-03 14:31:22] [PERMISSION] Steve promoted to VIP group by Admin
[2025-08-03 14:32:05] [PERMISSION] Temporary permission essentials.god expired for Alex
```

---

**Related Documentation**: [Essential Commands](Essential-Commands.md) | [Configuration](Configuration.md) | [Security Features](Security.md)

*Last Updated: August 3, 2025*
