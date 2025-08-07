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

#### Health & Wellness
```
essentials.heal                    # /heal command - heal yourself
essentials.heal.others             # Heal other players
essentials.heal.*                  # All heal permissions

essentials.feed                    # /feed command - feed yourself
essentials.feed.others             # Feed other players
essentials.feed.*                  # All feed permissions

essentials.god                     # /god command - toggle god mode for yourself
essentials.god.others              # Toggle god mode for others
essentials.god.*                   # All god mode permissions
```

#### Movement & Visibility
```
essentials.fly                     # /fly command - toggle flight for yourself
essentials.fly.others              # Toggle flight for others
essentials.fly.*                   # All fly permissions

essentials.speed.walk              # /speed walk command
essentials.speed.fly               # /speed fly command
essentials.speed.others            # Change speed for others
essentials.speed.*                 # All speed permissions

essentials.vanish                  # /vanish command - toggle vanish for yourself
essentials.vanish.others           # Toggle vanish for others
essentials.vanish.see              # See vanished players
essentials.vanish.*                # All vanish permissions
```

#### Item & Environment Management
```
essentials.repair                  # /repair command - repair item in hand
essentials.repair.all              # Repair all items
essentials.repair.others           # Repair items for others

essentials.give                    # /give command - give items
essentials.give.unlimited          # Give items without limits
essentials.give.*                  # All give permissions

essentials.time.set                # /time set command
essentials.time.add                # /time add command
essentials.time.query              # /time query command
essentials.time.*                  # All time permissions

essentials.weather.set             # /weather command base
essentials.weather.clear           # Set clear weather
essentials.weather.rain            # Set rain
essentials.weather.thunder         # Set thunderstorm
essentials.weather.*               # All weather permissions

essentials.workbench               # /workbench command - open crafting table
essentials.anvil                   # /anvil command - open anvil
essentials.enderchest              # /enderchest command - open your enderchest
essentials.enderchest.others       # View others' enderchests
```

### Teleportation System

#### Basic Teleportation
```
essentials.tp                      # /tp command - teleport to players
essentials.tp.others               # Teleport other players
essentials.tp.coords               # Teleport to coordinates
essentials.tphere                  # /tphere command - teleport players to you
essentials.tp.*                    # All teleport permissions
```

#### Home System
```
essentials.home                    # /home command - teleport to homes
essentials.sethome                 # /sethome command - set home locations
essentials.delhome                 # /delhome command - delete homes
essentials.homes                   # /homes command - list homes
essentials.home.others             # Access others' homes
essentials.home.multiple           # Set multiple homes
essentials.home.*                  # All home permissions
```

#### Warp System
```
essentials.warp                    # /warp command - teleport to warps
essentials.setwarp                 # /setwarp command - create warps (admin)
essentials.delwarp                 # /delwarp command - delete warps (admin)
essentials.warps                   # /warps command - list warps
essentials.warp.*                  # All warp permissions
```

#### TPA (Teleport Request) System
```
essentials.tpa                     # /tpa command - send teleport requests
essentials.tpahere                 # /tpahere command - request player to teleport to you
essentials.tpaccept                # /tpaccept command - accept teleport requests
essentials.tpdeny                  # /tpdeny command - deny teleport requests
essentials.tpcancel                # /tpcancel command - cancel pending requests
essentials.tpa.*                   # All TPA permissions
```

#### Spawn System
```
essentials.spawn                   # /spawn command - teleport to spawn
essentials.setspawn                # /setspawn command - set spawn location (admin)
essentials.spawn.others            # Teleport others to spawn
essentials.spawn.*                 # All spawn permissions
```

#### Back System
```
essentials.back                    # /back command - return to previous location
essentials.back.ondeath           # Use /back after death
essentials.back.onteleport        # Use /back after teleportation
```

### Moderation Commands

#### Ban System
```
essentials.ban                     # /ban command - ban players
essentials.tempban                 # /tempban command - temporary bans
essentials.banip                   # /banip command - IP bans
essentials.unban                   # /unban command - unban players
essentials.ban.exempt             # Exempt from being banned
essentials.ban.*                   # All ban permissions
```

#### Kick & Mute System
```
essentials.kick                    # /kick command - kick players
essentials.kick.exempt             # Exempt from being kicked

essentials.mute                    # /mute command - mute players
essentials.unmute                  # /unmute command - unmute players
essentials.mute.exempt             # Exempt from being muted
essentials.mute.*                  # All mute permissions
```

#### Jail System
```
essentials.jail                    # /jail command - jail players
essentials.unjail                  # /unjail command - unjail players
essentials.setjail                 # /setjail command - create jail locations
essentials.deljail                 # /deljail command - delete jails
essentials.jail.exempt             # Exempt from being jailed
essentials.jail.*                  # All jail permissions
```

### Economy System

#### Basic Economy
```
essentials.balance                 # /balance command - view your balance
essentials.balance.others          # View others' balances
essentials.pay                     # /pay command - send money to players
essentials.balancetop              # /balancetop command - view richest players
```

#### Economy Administration
```
essentials.eco.give                # /eco give command - give money
essentials.eco.take                # /eco take command - take money
essentials.eco.set                 # /eco set command - set balance
essentials.eco.reset               # /eco reset command - reset balances
essentials.eco.*                   # All economy admin permissions
```

#### Advanced Economy Features
```
neoessentials.economy.analytics    # View economy analytics
neoessentials.economy.transactions # View transaction history
neoessentials.economy.history      # View detailed economic history
```

### Messaging System

#### Private Messages
```
essentials.msg                     # /msg command - send private messages
essentials.reply                   # /reply command - reply to messages
essentials.msgtoggle               # /msgtoggle command - toggle message reception
essentials.socialspy               # /socialspy command - spy on private messages
```

#### Mail System
```
essentials.mail.send               # /mail send command - send mail
essentials.mail.read               # /mail read command - read mail
essentials.mail.clear              # /mail clear command - clear mailbox
essentials.mail.*                  # All mail permissions
```

#### Broadcasting
```
essentials.broadcast               # /broadcast command - server-wide messages
essentials.broadcast.world         # /broadcast command - world-specific messages
```

### Player Information Commands

#### Player Lists & Information
```
essentials.list                    # /list command - view online players
essentials.list.hidden             # See hidden/vanished players in list

essentials.whois                   # /whois command - detailed player info
essentials.seen                    # /seen command - when player was last online
essentials.realname                # /realname command - find player by nickname
```

#### Nickname System
```
essentials.nick                    # /nick command - set your nickname
essentials.nick.others             # Set nicknames for others
essentials.nick.color              # Use color codes in nicknames
essentials.nick.magic              # Use magic/obfuscated formatting
```

### Kit System

#### Kit Usage
```
essentials.kit                     # /kit command - use kits
essentials.kit.list                # /kit list command - list available kits
essentials.kit.preview             # /kit preview command - preview kit contents
```

#### Kit Administration
```
essentials.kit.create              # /kit create command - create new kits
essentials.kit.delete              # /kit delete command - delete kits
essentials.kit.edit                # /kit edit command - modify kits
essentials.kit.give                # /kit give command - give kits to players
essentials.kit.*                   # All kit permissions
```

### NeoEssentials Features

#### Bossbar System
```
neoessentials.bossbar.show         # Show bossbars to yourself
neoessentials.bossbar.show.others  # Show bossbars to other players
neoessentials.bossbar.hide         # Hide bossbars
neoessentials.bossbar.broadcast    # Broadcast bossbars to all players
neoessentials.bossbar.create       # Create custom bossbars
neoessentials.bossbar.update       # Update existing bossbars
neoessentials.bossbar.delete       # Delete bossbars
neoessentials.bossbar.templates    # Access bossbar templates
neoessentials.bossbar.*            # All bossbar permissions
```

#### Placeholder System
```
neoessentials.placeholder.test     # Test placeholder values
neoessentials.placeholder.list     # List available placeholders
neoessentials.placeholder.info     # View placeholder information
neoessentials.placeholder.reload   # Reload placeholder system
neoessentials.placeholder.*        # All placeholder permissions
```

#### GUI System
```
neoessentials.gui.open             # Open GUI menus
neoessentials.gui.admin            # Access admin GUI features
neoessentials.gui.themes           # Change GUI themes
neoessentials.gui.*                # All GUI permissions
```

#### Security System
```
neoessentials.security.view        # View security events and logs
neoessentials.security.admin       # Security system administration
neoessentials.security.alerts     # Receive security alerts
neoessentials.security.*           # All security permissions
```

### Permission Management

#### Permission Commands
```
neoessentials.permissions.info     # /permissions info command
neoessentials.permissions.check    # /permissions check command
neoessentials.permissions.user     # User permission management
neoessentials.permissions.group    # Group permission management
neoessentials.permissions.reload   # Reload permission system
neoessentials.permissions.stats    # View permission statistics
neoessentials.permissions.*        # All permission management commands
```

### Administration & Configuration

#### Configuration Management
```
neoessentials.config.reload        # Reload configuration files
neoessentials.config.save          # Save current configuration
neoessentials.config.reset         # Reset configuration to defaults
neoessentials.config.*             # All configuration permissions
```

#### Language System
```
neoessentials.language.set         # Set language preferences
neoessentials.language.list        # List available languages
neoessentials.language.reload      # Reload language files
neoessentials.language.*           # All language permissions
```

#### Performance Monitoring
```
neoessentials.performance.view     # View performance metrics
neoessentials.performance.admin    # Performance system administration
neoessentials.performance.*        # All performance permissions
```

#### Status Monitoring
```
neoessentials.status.view          # View system status
neoessentials.status.admin         # Status system administration
neoessentials.status.*             # All status permissions
```

#### Analytics
```
neoessentials.analytics.view       # View analytics data
neoessentials.analytics.admin      # Analytics system administration
neoessentials.analytics.*          # All analytics permissions
```

### Player Features

#### Playtime Tracking
```
neoessentials.playtime.view        # View your playtime
neoessentials.playtime.others      # View others' playtime
neoessentials.playtime.top         # View playtime leaderboards
neoessentials.playtime.*           # All playtime permissions
```

#### Achievement System
```
neoessentials.achievements.view    # View your achievements
neoessentials.achievements.others  # View others' achievements
neoessentials.achievements.admin   # Achievement system administration
neoessentials.achievements.*       # All achievement permissions
```

#### Player Preferences
```
neoessentials.preferences.set      # Set your preferences
neoessentials.preferences.view     # View preference settings
neoessentials.preferences.*        # All preference permissions
```

### Animation System

#### Animation Commands
```
neoessentials.animation.play       # Play animations
neoessentials.animation.stop       # Stop animations
neoessentials.animation.list       # List available animations
neoessentials.animation.create     # Create custom animations
neoessentials.animation.delete     # Delete animations
neoessentials.animation.*          # All animation permissions
```

### Web Dashboard

#### Web Dashboard Access
```
neoessentials.webdash.access       # Access web dashboard
neoessentials.webdash.admin        # Web dashboard administration
neoessentials.webdash.*            # All web dashboard permissions
```

### Bypass Permissions

#### Cooldown Bypasses
```
essentials.bypass.cooldown         # Bypass all cooldowns
essentials.bypass.cooldown.teleport # Bypass teleportation cooldowns
essentials.bypass.cooldown.command  # Bypass command cooldowns
```

#### Limit Bypasses
```
essentials.bypass.limit.home       # Bypass home limits
essentials.bypass.limit.warp       # Bypass warp limits
```

#### Cost Bypasses
```
essentials.bypass.cost             # Bypass all costs
essentials.bypass.cost.teleport    # Bypass teleportation costs
essentials.bypass.cost.command     # Bypass command costs
```

### Administrative Wildcard Permissions

#### Category Wildcards
```
essentials.*                       # All Essentials permissions
neoessentials.*                    # All NeoEssentials permissions
essentials.teleport.*              # All teleportation permissions
essentials.moderation.*            # All moderation permissions
essentials.economy.*               # All economy permissions
essentials.messaging.*             # All messaging permissions
*.admin                            # All admin permissions across all plugins
```

#### Ultimate Permission
```
*                                  # ALL permissions (use with extreme caution!)
```

### Permission Node Validation

All permission nodes follow these rules:
- Only contain letters, numbers, dots, underscores, and hyphens
- Maximum length of 100 characters
- Use consistent naming conventions
- Support wildcard matching with `*`
- Support negation with `-` prefix

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

**Related Documentation**: [Essential Commands](Essential-Commands) | [Configuration](Configuration) | [Security Features](Security)

*Last Updated: August 6, 2025*
