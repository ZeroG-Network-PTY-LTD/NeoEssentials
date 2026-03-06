# Kit Management

> **Version:** 1.0.2.6 · **Config:** `kits.json`, `config.json` → `kits` section

---

## Overview

Create item kits with cooldowns, permission gates, and command execution on claim. Players can preview kits before claiming. Staff can give kits to other players and reset cooldowns.

---

## Commands

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/kit` | `/kit` | `neoessentials.kits` | List available kits with cooldown status |
| `/kit` | `/kit <name> [player]` | `neoessentials.kits` / `neoessentials.kits.others` | Claim a kit (or give to another player) |
| `/kits` | alias | same | Alias |
| `/listkits` | alias | same | Alias |
| `/showkit` | `/showkit <name>` | `neoessentials.kits` | Preview kit contents without claiming |
| `/createkit` | `/createkit <name> [cooldown]` | `neoessentials.kits.create` | Create kit from current inventory |
| `/delkit` | `/delkit <name>` | `neoessentials.kits.delete` | Delete a kit |
| `/kitreset` | `/kitreset <kit> [player]` | `neoessentials.kitreset` / `neoessentials.kitreset.others` | Reset a kit cooldown |

---

## Permissions

| Node | Default | Description |
|---|---|---|
| `neoessentials.kits` | ✅ | List and claim kits |
| `neoessentials.kits.others` | 🔒 | Give a kit to another player |
| `neoessentials.kits.create` | 🔒 | Create kits with `/createkit` |
| `neoessentials.kits.delete` | 🔒 | Delete kits with `/delkit` |
| `neoessentials.kits.override` | 🔒 | Bypass kit cooldowns |
| `neoessentials.kitreset` | 🔒 | Reset your own kit cooldown |
| `neoessentials.kitreset.others` | 🔒 | Reset another player's cooldown |
| `neoessentials.kit.<name>` | — | Restrict a specific kit to players with this node |

---

## Kit Data Format (`kits.json`)

```json
{
  "kits": [
    {
      "name": "starter",
      "cooldown": 86400,
      "items": [
        { "item": "minecraft:stone_sword", "count": 1 },
        { "item": "minecraft:bread", "count": 16 }
      ],
      "commands": [
        "say Welcome {player}!"
      ]
    }
  ]
}
```

| Field | Description |
|---|---|
| `name` | Kit name (used in `/kit <name>`) |
| `cooldown` | Seconds between claims (`0` = no cooldown, `-1` = one-time) |
| `items` | List of items — `item` (registry ID), `count`, optional `nbt` |
| `commands` | Server commands run on claim; `{player}` replaced with claimer name |

---

## Config (`config.json` → `kits`)

| Key | Default | Description |
|---|---|---|
| `skipUsedOneTimeKitsFromKitList` | `true` | Hide one-time kits after claimed |
| `kitAutoEquip` | `true` | Auto-equip armour from kits into empty armour slots |
| `maxKitsPerPlayer` | `0` | Max simultaneous active cooldowns (0 = unlimited) |
| `allowKitOverride` | `true` | Allow `neoessentials.kits.override` bypass |
| `enableKitPreview` | `true` | Enable `/showkit` preview |
| `newPlayerKit` | `""` | Kit name to auto-give on first join (blank = disabled) |
| `logKitUsage` | `true` | Log kit claims to console |

---

## How Cooldowns Work

- Cooldown starts the moment a kit is successfully claimed
- Staff with `neoessentials.kits.override` bypass cooldowns entirely
- `/kitreset <kit>` clears a specific cooldown
- `/kitreset <kit> <player>` requires `neoessentials.kitreset.others`
- One-time kits (`cooldown: -1`) can never be re-claimed

---

*Back to [Wiki Home](Home)*
