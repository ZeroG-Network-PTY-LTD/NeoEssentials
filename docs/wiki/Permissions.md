# Permissions System

NeoEssentials includes a comprehensive built-in permission system with group-based management, inheritance, wildcards, and persistent storage. The system provides fine-grained control over player access to commands and features.

## 🎯 System Overview

### Key Features
- **Group-based Permissions** - Organize players into hierarchical permission groups
- **Permission Inheritance** - Groups can inherit permissions from parent groups  
- **Individual Player Overrides** - Per-player permission customization
- **Persistent Storage** - All permissions saved to JSON configuration files
- **Permission Caching** - High-performance caching system for fast permission checks
- **Prefix/Suffix Support** - Chat formatting with group prefixes and suffixes
- **Wildcard Support** - Use `*` for broad permission grants
- **Permission Validation** - Built-in validation for permission node format

### Storage System
Permissions are stored in JSON format in:
- `config/neoessentials/permissions.json` - Main permissions configuration
- Cached data stored in `neoessentials_data/` for performance

## 👥 Permission Groups

NeoEssentials comes with four default permission groups configured with proper inheritance:

### Default Group
**Name**: `Default`  
**Priority**: 0 (lowest)  
**Prefix**: `§7[Player]`  
**Suffix**: ` §7`  
**Inheritance**: None  
**Permissions**: Basic player functionality

Core permissions for all players:
- `neoessentials.home.*` - Home management (set, delete, teleport, list)
- `neoessentials.spawn` - Access spawn teleportation
- `neoessentials.back` - Return to previous location  
- `neoessentials.warp` - Use public warps
- `neoessentials.tpa.*` - TPA system (send, accept, deny, cancel)
- `neoessentials.balance` - View economy balance
- `neoessentials.pay` - Pay other players
- `neoessentials.msg.*` - Private messaging system
- `neoessentials.mail.*` - Mail system access
- `neoessentials.kit.use` - Use available kits

### VIP Group  
**Name**: `VIP`  
**Priority**: 10  
**Prefix**: `§6[VIP]`  
**Suffix**: ` §6♦`  
**Inheritance**: `Default`  
**Additional Permissions**: Enhanced features for VIP players

Additional VIP permissions:
- `neoessentials.kit.vip` - Access to VIP-only kits
- `neoessentials.warp.vip` - Access to VIP warps
- `neoessentials.home.multiple` - Set multiple homes
- `neoessentials.back.ondeath` - Automatic back point on death
- Enhanced limits for various commands

### Moderator Group
**Name**: `Moderator`  
**Priority**: 50  
**Prefix**: `§b[Mod]`  
**Suffix**: ` §b⚡`  
**Inheritance**: `VIP`  
**Additional Permissions**: Moderation and management tools

Additional Moderator permissions:
- `neoessentials.moderation.*` - Full moderation access
- `neoessentials.teleport.*` - Advanced teleportation commands  
- `neoessentials.warp.admin` - Warp management
- `neoessentials.kit.admin` - Kit management
- `neoessentials.economy.admin` - Economy management
- `neoessentials.player.info` - Player information commands
- `neoessentials.placeholder.*` - Placeholder system access

### Admin Group
**Name**: `Admin`  
**Priority**: 100 (highest)  
**Prefix**: `§c[Admin]`  
**Suffix**: ` §c★`  
**Inheritance**: `Moderator`  
**Additional Permissions**: Full system access

Additional Admin permissions:
- `neoessentials.*` - Full NeoEssentials access (wildcard)
- All moderation, configuration, and system management features

## 🔑 Permission Nodes

NeoEssentials uses a structured permission node system organized by feature category:

### Core Commands
| Permission Node | Description | Default Group |
|----------------|-------------|---------------|
| `neoessentials.spawn` | Access to spawn command | Default |
| `neoessentials.back` | Access to back command | Default |
| `neoessentials.suicide` | Access to suicide command | Default |

### Teleportation System
| Permission Node | Description | Default Group |
|----------------|-------------|---------------|
| `neoessentials.home.*` | All home commands | Default |
| `neoessentials.tpa.*` | All TPA commands | Default |
| `neoessentials.warp` | Use warps | Default |
| `neoessentials.warp.admin` | Manage warps | Moderator |
| `neoessentials.teleport.*` | Advanced teleport commands | Moderator |

### Economy System
| Permission Node | Description | Default Group |
|----------------|-------------|---------------|
| `neoessentials.balance` | View balance | Default |
| `neoessentials.pay` | Pay other players | Default |
| `neoessentials.economy.admin` | Economy management | Moderator |

### Moderation Commands
| Permission Node | Description | Default Group |
|----------------|-------------|---------------|
| `neoessentials.moderation.basic` | Basic moderation | Moderator |
| `neoessentials.moderation.ban` | Ban system | Moderator |
| `neoessentials.moderation.kick` | Kick players | Moderator |
| `neoessentials.moderation.mute` | Mute system | Moderator |
| `neoessentials.moderation.jail` | Jail system | Moderator |

### Communication System
| Permission Node | Description | Default Group |
|----------------|-------------|---------------|
| `neoessentials.msg.*` | Private messaging | Default |
| `neoessentials.mail.*` | Mail system | Default |
| `neoessentials.broadcast` | Send broadcasts | Moderator |

### Player Information
| Permission Node | Description | Default Group |
|----------------|-------------|---------------|
| `neoessentials.player.info` | View player info | Moderator |
| `neoessentials.player.list` | Enhanced player list | Moderator |

### Kit System
| Permission Node | Description | Default Group |
|----------------|-------------|---------------|
| `neoessentials.kit.use` | Use kits | Default |
| `neoessentials.kit.vip` | VIP kits | VIP |
| `neoessentials.kit.admin` | Kit management | Moderator |

### Administrative
| Permission Node | Description | Default Group |
|----------------|-------------|---------------|
| `neoessentials.admin.reload` | Reload configuration | Admin |
| `neoessentials.admin.debug` | Debug commands | Admin |
| `neoessentials.placeholder.*` | Placeholder system | Moderator |

### Bypass Permissions
| Permission Node | Description | Default Group |
|----------------|-------------|---------------|
| `neoessentials.bypass.cooldown` | Bypass command cooldowns | VIP |
| `neoessentials.bypass.cost` | Bypass command costs | VIP |
| `neoessentials.bypass.limit` | Bypass command limits | VIP |

## 🛠️ Permission Management Commands

### Core Commands

#### `/permission` or `/perm`
Main permission management command.

**Subcommands:**
- `/permission group <group> <player>` - Set player's group
- `/permission add <permission> <player>` - Add permission to player
- `/permission remove <permission> <player>` - Remove permission from player
- `/permission check <permission> <player>` - Check player permission
- `/permission list <player>` - List player permissions
- `/permission groups` - List all groups
- `/permission reload` - Reload permission system

### Group Management

#### `/group`
Group management commands.

**Subcommands:**
- `/group info <group>` - Show group information
- `/group create <group>` - Create new group
- `/group delete <group>` - Delete group
- `/group set prefix <group> <prefix>` - Set group prefix
- `/group set suffix <group> <suffix>` - Set group suffix
- `/group set inheritance <group> <parent>` - Set group inheritance

### Permission Assignment

**Setting Player Groups:**
```bash
/permission group VIP Steve
/permission group Moderator Alex
/permission group Admin Bob
```

**Individual Permission Management:**
```bash
/permission add neoessentials.warp.vip Steve
/permission remove neoessentials.home.unlimited Steve
/permission check neoessentials.teleport.tp Steve
```

**Group Information:**
```bash
/group info Default
/group info VIP
/permission groups
```

## 📁 Configuration Files

### Permission Configuration Format

**Example `permissions.json`:**
```json
{
  "groups": {
    "Default": {
      "priority": 0,
      "prefix": "§7[Player]",
      "suffix": " §7",
      "inheritance": [],
      "permissions": [
        "neoessentials.spawn",
        "neoessentials.back",
        "neoessentials.home.*",
        "neoessentials.tpa.*",
        "neoessentials.warp",
        "neoessentials.balance",
        "neoessentials.pay",
        "neoessentials.msg.*",
        "neoessentials.mail.*",
        "neoessentials.kit.use"
      ]
    },
    "VIP": {
      "priority": 10,
      "prefix": "§6[VIP]",
      "suffix": " §6♦",
      "inheritance": ["Default"],
      "permissions": [
        "neoessentials.kit.vip",
        "neoessentials.warp.vip",
        "neoessentials.home.multiple",
        "neoessentials.bypass.cooldown"
      ]
    }
  },
  "players": {
    "uuid-here": {
      "group": "VIP",
      "permissions": [
        "neoessentials.custom.permission"
      ]
    }
  }
}
```

### Default Configuration
NeoEssentials automatically creates a default permission configuration with the four standard groups when first launched.

## 🔄 Permission Inheritance

### How Inheritance Works
Groups can inherit permissions from parent groups, creating a hierarchical permission structure:

```
Default (Base permissions)
    ↓
   VIP (Default + VIP permissions)
    ↓
Moderator (Default + VIP + Moderator permissions)
    ↓
  Admin (Default + VIP + Moderator + Admin permissions)
```

### Inheritance Benefits
- **Simplified Management** - Changes to parent groups automatically apply to child groups
- **Consistent Permissions** - Ensures all higher groups have base permissions
- **Easy Upgrades** - Moving players between groups maintains expected permissions

### Multiple Inheritance
Groups can inherit from multiple parents:
```json
{
  "inheritance": ["Default", "Builder", "Helper"]
}
```

## 🎨 Chat Integration

### Prefix and Suffix Display
The permission system integrates with chat formatting to display group prefixes and suffixes:

**Example Chat Output:**
- Default: `§7[Player] Steve§7: Hello world!`
- VIP: `§6[VIP] Steve §6♦§f: Hello world!`
- Moderator: `§b[Mod] Alex §b⚡§f: Server will restart in 5 minutes`
- Admin: `§c[Admin] Bob §c★§f: Welcome everyone!`

### Placeholder Integration
Permission prefixes and suffixes are available as placeholders:
- `%prefix%` - Player's group prefix
- `%suffix%` - Player's group suffix

## 🚀 Performance Features

### Permission Caching
- **Memory Caching** - Permissions cached in memory for fast access
- **Smart Updates** - Cache automatically updated when permissions change
- **Minimal Database Queries** - Reduced file I/O through intelligent caching

### Optimized Checking
- **Hierarchy Awareness** - Permission checks respect group inheritance
- **Wildcard Optimization** - Efficient wildcard permission processing
- **Fast Lookups** - O(1) permission checking for most operations

## 🛡️ Security Features

### Permission Validation
- **Node Format Validation** - Ensures permission nodes follow correct format
- **Circular Inheritance Protection** - Prevents circular group inheritance
- **Safe Defaults** - Conservative default permissions for security

### Administrative Controls
- **Admin-Only Commands** - Sensitive permission commands restricted to admins
- **Audit Logging** - Permission changes logged for security tracking
- **Backup System** - Automatic backup of permission configurations

## 🔧 Developer API

### Checking Permissions in Code

```java
// Check if player has permission
boolean hasPermission = PermissionUtil.hasPermission(player, "neoessentials.home.set");

// Check permission or OP status
boolean hasPermissionOrOp = PermissionUtil.hasPermissionOrOp(player, "neoessentials.admin.reload");

// Get player's group
String group = CustomPermissionsManager.getInstance().getPlayerGroup(player.getUUID());

// Get player's prefix
String prefix = CustomPermissionsManager.getInstance().getPlayerPrefix(player.getUUID());

// Get player's suffix  
String suffix = CustomPermissionsManager.getInstance().getPlayerSuffix(player.getUUID());
```

### Managing Groups Programmatically

```java
CustomPermissionsManager manager = CustomPermissionsManager.getInstance();

// Set player's group
manager.setPlayerGroup(playerUUID, "VIP");

// Add permission to player
manager.addPlayerPermission(playerUUID, "neoessentials.custom.permission");

// Remove permission from player
manager.removePlayerPermission(playerUUID, "neoessentials.custom.permission");

// Check if player has permission
boolean hasPermission = manager.hasPermission(playerUUID, "neoessentials.home.set");
```

## 🔍 Troubleshooting

### Common Issues

**Permission Not Working:**
1. Check permission node spelling (case-sensitive)
2. Verify group inheritance is correct
3. Ensure player is in the expected group
4. Check for negative permissions overriding grants

**Group Assignment Issues:**
1. Verify group exists in configuration
2. Check group name spelling (case-sensitive)
3. Ensure inheritance chains are not circular
4. Restart server if permissions aren't applying

**Performance Problems:**
1. Check for excessive permission inheritance chains
2. Avoid overly complex wildcard patterns
3. Monitor permission cache hit rates
4. Consider simplifying permission structure

### Debug Commands

**Check Player Permissions:**
```bash
/permission list <player>
/permission check <permission> <player>
```

**Verify Group Configuration:**
```bash
/permission groups
/group info <group>
```

**Reload System:**
```bash
/permission reload
```

---

**Related Documentation**: [Commands](Commands.md) | [Configuration](Configuration.md) | [API Documentation](API_DOCUMENTATION.md)

*Last Updated: January 2025 - NeoEssentials 2.1.0*
