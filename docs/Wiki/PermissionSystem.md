# Permission System

## Overview
The Permission System in NeoEssentials provides group-based and node-based permission management, supporting both internal and external systems. All features are strictly governed by configuration and permissions.

---

## Manager
- **PermissionSystem**: Central initialization and management
- **PermissionManager**: Handles groups, nodes, and inheritance
- **PermissionAPI**: Permission checks and external adapter support

---

## Configuration (`config.json` > `permissions`)
- `useExternalPermissions`: Enable/disable external permission system (LuckPerms, FTB Ranks)
- `defaultGroup`: Default group for new players
- `opsBypassPermissions`: Allow server operators to bypass permission checks
- `cachePermissions`: Enable permission caching
- `permissionCacheExpiryMinutes`: Cache expiry time in minutes

---

## Group Structure (`permissions.json`)
- Groups: `default`, `moderator`, `admin`
- Each group has a prefix, suffix, permissions, and inheritance

**Example:**
```
{
  "groups": [
    {
      "name": "default",
      "prefix": "&7",
      "permissions": ["neoessentials.economy.balance", "neoessentials.chat.msg"]
    },
    {
      "name": "moderator",
      "prefix": "&2[Mod] ",
      "permissions": ["neoessentials.economy.*", "neoessentials.moderation.kick"],
      "inherits": ["default"]
    },
    {
      "name": "admin",
      "prefix": "&c[Admin] ",
      "permissions": ["neoessentials.*"],
      "inherits": ["moderator"]
    }
  ]
}
```

---

## Permission Nodes (`permissions_nodes.txt`)
- Comprehensive list of all permission nodes and descriptions
- Economy, item management, chat, kits, teleportation, moderation, and more

---

## Commands
- `/permissions reload`: Hot-reload permission system
- `/permissions group list`: List all permission groups
- `/permissions group create`: Create new permission groups

---

## Logging
- Permission system initialization, group loading, and config settings are logged

---

## Notes
- All features and limits are strictly controlled by config and permissions
- For advanced usage, refer to the comments in `config.json`, `permissions.json`, and `permissions_nodes.txt` for each setting
# Permission System

## Config File: permissions.json

### Options
- `defaultGroup`: Default permission group for new players.
- `groups`: List of permission groups, each with:
  - `name`: Group name
  - `prefix`: Chat prefix
  - `suffix`: Chat suffix
  - `permissions`: List of permission nodes
  - `inherits`: Groups this group inherits from

### Example
```json
{
  "defaultGroup": "default",
  "groups": [
    {
      "name": "default",
      "prefix": "&7",
      "suffix": "",
      "permissions": ["neoessentials.economy.balance", "neoessentials.chat.msg"],
      "inherits": []
    },
    {
      "name": "moderator",
      "prefix": "&2[Mod] ",
      "suffix": "",
      "permissions": ["neoessentials.economy.*", "neoessentials.chat.*"],
      "inherits": ["default"]
    },
    {
      "name": "admin",
      "prefix": "&c[Admin] ",
      "suffix": "",
      "permissions": ["neoessentials.*"],
      "inherits": ["moderator"]
    }
  ]
}
```

---

For more details, see the main documentation or ask in the Discord support server.