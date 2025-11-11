# Kit Management System

## Overview
The Kit Management System in NeoEssentials allows players and staff to create, manage, and claim item kits with configurable cooldowns, permissions, and preview options. All features are strictly governed by configuration and permissions.

---

## Manager
- **KitManager**: Handles kit creation, deletion, claiming, preview, cooldowns, and usage tracking.

---

## Configuration (`config.json` > `kits`)
- `skipUsedOneTimeKitsFromKitList`: Remove one-time kits from `/kit list` after use
- `kitAutoEquip`: Auto-equip armor from kits if slots are empty
- `pastebinCreatekit`: If true, `/createkit` gives a Pastebin link; if false, adds to `kits.json`
- `maxKitsPerPlayer`: Max active kit cooldowns per player
- `allowKitOverride`: Allow permission-based bypass of kit restrictions
- `enableKitPreview`: Allow previewing kit contents
- `logKitUsage`: Log kit usage to server logs
- `commandCosts`: Cost per kit command (economy integration)
- `newPlayerKit`: Kit given to new players on first join (optional)

---

## Kit Data Structure (`kits.json`)
Each kit is defined with:
- `name`: Kit name
- `cooldown`: Cooldown in seconds
- `items`: List of items (item ID, count, NBT)
- `commands`: List of commands executed on claim

**Example:**
```
{
  "kits": [
    {
      "name": "starter",
      "cooldown": 86400,
      "items": [
        {"item": "minecraft:stone_sword", "count": 1, "nbt": "{}"},
        {"item": "minecraft:bread", "count": 16, "nbt": "{}"}
      ],
      "commands": ["say Welcome to the server, {player}!"]
    }
  ]
}
```

---

## Commands
- `/kit`: Claim a kit
- `/createkit`: Create a new kit from inventory
- `/delkit`: Delete a kit
- `/listkits`: List available kits
- Kit preview and management via commands

---

## Permissions
- `neoessentials.kit.*`: General kit permissions
- `neoessentials.kit.<kitname>`: Specific kit access
- Permission-based override for cooldowns and restrictions

---

## Logging
- All kit usage and management actions are logged if enabled in config

---

## Safety & Restrictions
- Cooldown enforcement per kit and per player
- Usage tracking and limits
- Optional auto-equip for armor
- One-time kits can be hidden after use

---

## Notes
- All features and limits are strictly controlled by config and permissions
- For advanced usage, refer to the comments in `config.json` and `kits.json` for each setting
# Kit Management System

## Overview
The Kit Management system allows server administrators to create, manage, and distribute custom kits to players. Kits are predefined sets of items and resources that can be claimed by players, often with cooldowns and usage limits.

## Features
- Create custom kits from inventory
- Cooldown system for kit claims
- Usage tracking per player
- Admin commands for kit management
- Configurable via JSON files

## Commands
- `/createkit <name> [cooldown]` — Create a new kit from your inventory
- `/kit [name]` — Claim or view available kits
- `/delkit <name>` — Delete a kit
- `/listkits` — List all kits

## Configuration Files
The following .json file in `src/main/resources/data/config` is relevant to the Kit Management system:

- `kits.json` — Defines all available kits, their contents, cooldowns, and permissions

### Example: kits.json
```json
{
  "kits": {
    "starter": {
      "items": [
        { "id": "minecraft:stone_sword", "count": 1 },
        { "id": "minecraft:bread", "count": 16 }
      ],
      "cooldown": 3600,
      "permission": "neoessentials.kit.starter"
    },
    "vip": {
      "items": [
        { "id": "minecraft:diamond_sword", "count": 1 },
        { "id": "minecraft:golden_apple", "count": 5 }
      ],
      "cooldown": 86400,
      "permission": "neoessentials.kit.vip"
    }
  }
}
```

## Usage
1. Define kits in `kits.json` with desired items, cooldowns, and permissions.
2. Use `/createkit` to add new kits from your inventory.
3. Players use `/kit <name>` to claim kits, subject to cooldowns and permissions.
4. Admins manage kits with `/delkit` and `/listkits`.

## Integration
- Works with the Permission System for kit access control.
- Cooldown and usage tracking are persistent across server restarts.

## Troubleshooting
- Ensure item IDs in `kits.json` are valid Minecraft item IDs.
- Check permissions for kit access if players cannot claim kits.
- Review cooldown settings to avoid excessive waiting times.

---

For more details, see the main documentation or ask in the Discord support server.