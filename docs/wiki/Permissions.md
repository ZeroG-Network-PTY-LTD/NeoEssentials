# Permissions

NeoEssentials includes a comprehensive permission system that provides fine-grained control over player access to commands and features. The system supports group-based permissions with inheritance, individual player permissions, temporary permissions, and wildcard support.

## 📋 Permission System Overview

### Features
- **Group-based Permissions** - Organize players into permission groups
- **Permission Inheritance** - Groups can inherit permissions from parent groups
- **Individual Player Permissions** - Override group permissions for specific players
- **Temporary Permissions** - Grant permissions for a limited time
- **Wildcard Support** - Use `*` for broader permission grants
- **Permission Caching** - Optimized performance through permission caching
- **Prefix/Suffix Support** - Visual indicators for player groups

### Default Groups
NeoEssentials comes with four default permission groups:

- **default** - Basic player permissions
- **vip** - Enhanced player permissions with some bypass abilities
- **moderator** - Moderation permissions with user management capabilities
- **admin** - Full administrative access to all features

## 🔧 Permission Commands

### Group Management
Manage permission groups and their settings:

```bash
# List all groups
/permissions group list

# View group information
/permissions group info <group>

# Create a new group
/permissions group create <name> <prefix>

# Delete a group
/permissions group delete <group>

# Add permission to group
/permissions group permission add <group> <permission>

# Remove permission from group
/permissions group permission remove <group> <permission>

# Set group inheritance
/permissions group inheritance <group> <parent>

# Set group prefix/suffix
/permissions group prefix <group> <prefix>
/permissions group suffix <group> <suffix>
```

### User Management
Manage individual player permissions:

```bash
# View player permission info
/permissions user <player> info

# Set player's group
/permissions user <player> group set <group>

# Add permission to player
/permissions user <player> permission add <permission>

# Remove permission from player
/permissions user <player> permission remove <permission>

# Add temporary permission (in seconds)
/permissions user <player> permission temp <permission> <duration>

# Clear player's custom permissions
/permissions user <player> clear
```

### System Commands
System-level permission management:

```bash
# Check player's permission
/permissions check <player> <permission>

# View permission statistics
/permissions stats

# List common permission nodes
/permissions nodes

# Reload permission system
/permissions reload
```

## 🎯 Permission Nodes

### Essential Commands
Core utility commands for players:

```
neoessentials.heal              # Heal self
neoessentials.heal.others       # Heal other players
neoessentials.feed              # Feed self
neoessentials.feed.others       # Feed other players
neoessentials.god               # God mode for self
neoessentials.god.others        # God mode for others
neoessentials.fly               # Flight for self
neoessentials.fly.others        # Flight for others
neoessentials.vanish            # Vanish mode
neoessentials.workbench         # Portable workbench
neoessentials.anvil             # Portable anvil
neoessentials.enderchest        # Portable ender chest
```

### Teleportation System
Teleportation and location-based commands:

```
neoessentials.home              # Use homes
neoessentials.sethome           # Set homes
neoessentials.delhome           # Delete homes
neoessentials.homes             # List homes
neoessentials.spawn             # Use spawn
neoessentials.spawn.set         # Set spawn point
neoessentials.warp              # Use warps
neoessentials.warp.set          # Create warps
neoessentials.warp.delete       # Delete warps
neoessentials.tp                # Basic teleportation
neoessentials.tp.others         # Teleport others
neoessentials.tpa               # TPA requests
neoessentials.back              # Return to previous location
```

### Moderation Commands
Player management and moderation tools:

```
neoessentials.kick              # Kick players
neoessentials.ban               # Ban players
neoessentials.tempban           # Temporary bans
neoessentials.mute              # Mute players
neoessentials.jail              # Jail players
neoessentials.vanish.see        # See vanished players
```

### Economy System
Economic features and shop management:

```
neoessentials.balance           # View balance
neoessentials.pay               # Pay other players
neoessentials.eco.give          # Give money to players
neoessentials.eco.take          # Take money from players
neoessentials.shop.use          # Use shops
neoessentials.shop.create       # Create shops
neoessentials.shop.admin        # Shop administration
```

### Administration
Server administration and configuration:

```
neoessentials.config.reload     # Reload configurations
neoessentials.permissions.*     # Permission management
neoessentials.performance.admin # Performance monitoring
neoessentials.security.admin    # Security administration
```

### Wildcard Permissions
Broad permission grants:

```
neoessentials.*                 # All NeoEssentials permissions
neoessentials.teleport.*        # All teleportation permissions
neoessentials.moderation.*      # All moderation permissions
neoessentials.economy.*         # All economy permissions
*                              # All permissions (use with caution)
```

## 🔐 Permission Groups

### Default Group Configuration

#### Default Group
Basic permissions for all players:
```
neoessentials.home
neoessentials.sethome
neoessentials.spawn
neoessentials.balance
neoessentials.pay
neoessentials.msg
neoessentials.reply
```

#### VIP Group
Enhanced permissions with bypass abilities:
```
Inherits: default
Additional permissions:
neoessentials.fly
neoessentials.heal
neoessentials.feed
neoessentials.bypass.cooldown.teleport
neoessentials.bossbar.show
```

#### Moderator Group
Moderation capabilities:
```
Inherits: vip
Additional permissions:
neoessentials.kick
neoessentials.mute
neoessentials.jail
neoessentials.vanish
neoessentials.vanish.see
neoessentials.permissions.check
```

#### Admin Group
Full administrative access:
```
Inherits: moderator
Additional permissions:
neoessentials.*
neoessentials.config.*
neoessentials.permissions.*
neoessentials.performance.admin
```

## ⚙️ Configuration

### Permission Integration
The permission system integrates with:
- **Minecraft's built-in OP system** - OPs automatically have all permissions
- **Fallback permissions** - Basic permissions granted when permission system fails
- **External permission plugins** - Compatibility with other permission systems

### Performance Optimization
- **Permission Caching** - Reduces database lookups
- **Lazy Loading** - Permissions loaded only when needed
- **Batch Operations** - Efficient bulk permission changes

### Storage
- Permissions are stored persistently and survive server restarts
- Player group assignments are maintained across sessions
- Temporary permissions automatically expire

## 📝 Usage Examples

### Setting Up Groups
```bash
# Create a builder group
/permissions group create builder "&e[Builder] "

# Give building permissions
/permissions group permission add builder neoessentials.fly
/permissions group permission add builder neoessentials.god
/permissions group permission add builder neoessentials.give

# Set inheritance from VIP
/permissions group inheritance builder vip
```

### Managing Players
```bash
# Promote a player to moderator
/permissions user Steve group set moderator

# Give temporary admin access for 1 hour
/permissions user Steve permission temp neoessentials.* 3600

# Check what permissions a player has
/permissions user Steve info
/permissions check Steve neoessentials.fly
```

### Debugging Permissions
```bash
# Check if player has specific permission
/permissions check Alex neoessentials.tp

# View detailed permission breakdown
/permissions user Alex info

# Check group permissions
/permissions group info moderator
```

## 🛡️ Security Best Practices

### Permission Hierarchy
- Use inheritance to maintain clean permission structures
- Avoid granting wildcard permissions (`*`) unnecessarily
- Regularly audit group permissions

### Temporary Permissions
- Use temporary permissions for short-term access
- Monitor temporary permission usage through logs
- Set reasonable duration limits

### Regular Maintenance
- Review and update group permissions regularly
- Remove unused groups and permissions
- Monitor permission usage statistics

---

*For detailed permission node documentation, use `/permissions nodes` in-game or consult the [API documentation](API.md).*
