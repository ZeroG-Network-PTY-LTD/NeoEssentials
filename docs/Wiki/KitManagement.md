# Kit Management

> **Version:** 1.0.2.6 · **Config:** `kits.json`, `config.json` → `kits` section

---

## Overview

Create item kits with cooldowns, permission gates, and command execution on claim. Players can preview kits before claiming. Staff can give kits to other players and reset cooldowns.

---

## Commands

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/kit` | `/kit` | `neoessentials.kits.use` | List kits available to you, with cooldown status |
| `/kit` | `/kit <name> [player]` | per-kit permission (see below) + `neoessentials.kit.others` to target another player | Claim a kit (or give to another player) |
| `/kits`, `/listkits` | `/kits [page]` | `neoessentials.kits.list` | **Admin overview**: paginated list of *all* kits with item counts, cooldowns, permissions, and descriptions — not an alias of `/kit` |
| `/showkit` | `/showkit <name>[,<name2>,...]` | `neoessentials.showkit` | Preview one or more kits' contents without claiming |
| `/createkit` | `/createkit <name> [displayname] [cooldownSeconds] [description]` | `neoessentials.kits.create` | Create/update a kit from your current inventory (main inventory only; armor/offhand excluded) |
| `/makekit`, `/addkit` | alias | same | Aliases |
| `/delkit` | `/delkit <name>` then `/delkit <name> confirm` | `neoessentials.kits.delete` | Delete a kit (two-step confirmation) |
| `/deletekit`, `/removekit`, `/rkit` | alias | same | Aliases |
| `/kitreset` | `/kitreset <kit> [player]` | `neoessentials.kitreset` / `neoessentials.kitreset.others` | Reset a kit cooldown |

Note: `/kit`, `/kits`/`/listkits`, and `/showkit` are three distinct commands with different permissions and purposes — `/kit` is for claiming, `/kits`/`/listkits` is an admin-facing overview of every kit, and `/showkit` previews a specific kit's contents.

There is no built-in support for running server commands on kit claim — kits only grant items.

---

## Permissions

| Node | Default | Description |
|---|---|---|
| `neoessentials.kits.use` | ✅ | Use `/kit` to list/claim kits |
| `neoessentials.kits.list` | ✅ | Use `/kits`/`/listkits` (admin overview) |
| `neoessentials.kit.others` | 🔒 | Give a kit to another player via `/kit <name> <player>` |
| `neoessentials.kits.create` | 🔒 | Create kits with `/createkit` |
| `neoessentials.kits.delete` | 🔒 | Delete kits with `/delkit` |
| `neoessentials.kits.override` | 🔒 | Bypass all kit cooldowns/restrictions (requires `allowKitOverride` also enabled) |
| `neoessentials.kits.nocooldown` | 🔒 | Bypass cooldowns only (kept even if `allowKitOverride` is off) |
| `neoessentials.showkit` | ✅ | Preview kit contents with `/showkit` |
| `neoessentials.kitreset` | 🔒 | Reset your own kit cooldown |
| `neoessentials.kitreset.others` | 🔒 | Reset another player's cooldown |
| `neoessentials.kits.<name>` | auto-registered per kit | Default per-kit permission, required to see/claim that kit |
| `neoessentials.kits.<name>.nocooldown` | 🔒 | Bypass cooldown for one specific kit |

---

## Kit Data Format (`kits.json`)

```json
{
  "kits": [
    {
      "name": "starter",
      "displayName": "Starter Kit",
      "description": "Given to new players",
      "cooldownMillis": 86400000,
      "permission": "neoessentials.kits.starter",
      "maxUses": -1,
      "enabled": true,
      "items": [
        { "item": "minecraft:stone_sword", "count": 1 },
        { "item": "minecraft:bread", "count": 16 }
      ]
    }
  ]
}
```

| Field | Description |
|---|---|
| `name` | Kit name (used in `/kit <name>`) |
| `displayName` | Human-readable name shown in messages/previews (defaults to `name`) |
| `description` | Shown by `/showkit` and `/kits` (defaults to empty) |
| `cooldownMillis` | Milliseconds between claims (`0` = no cooldown). A legacy `cooldown` field (in **seconds**) is still read for backward compatibility if `cooldownMillis` is absent |
| `permission` | Permission required to see/claim the kit; defaults to `neoessentials.kits.<name>` if omitted |
| `maxUses` | Max claims per player (`-1` = unlimited); `/listkits`'s `skipUsedOneTimeKitsFromKitList` treats `maxUses == 1` (or a negative legacy value) as "one-time" |
| `enabled` | Whether the kit is currently claimable (defaults to `true`) |
| `items` | List of items — `item` (registry ID), `count`, optional `nbt` |

There is no `commands` field — kits only grant items, they do not run server commands on claim.

---

## Config (`config.json` → `kits`)

| Key | Default | Description |
|---|---|---|
| `skipUsedOneTimeKitsFromKitList` | `false` | Hide one-time kits from `/listkits` once claimed |
| `kitAutoEquip` | `false` | Auto-equip armour from kits into empty armour slots |
| `maxKitsPerPlayer` | `-1` | Max simultaneous active cooldowns (`-1`/any non-positive = unlimited) |
| `allowKitOverride` | `false` | Allow `neoessentials.kits.override` bypass |
| `newPlayerKit.kitName` | *(none)* | Kit name to auto-give on first join (nested under `kits.newPlayerKit`, not a flat key) |
| `logKitUsage` | `true` | Log kit claims to console |
| `pastebinCreatekit` | `false` | If enabled, `/createkit` uploads the kit JSON instead of saving it locally |
| `commandCosts.<command>` | `0` | Economy cost to run a given kit command (e.g. `createkit`, `kit`, `delkit`, `listkits`) |

`/showkit` has no dedicated config toggle — it's always available to anyone with `neoessentials.showkit`.

---

## How Cooldowns Work

- Cooldown starts the moment a kit is successfully claimed
- Staff with `neoessentials.kits.override` bypass everything (requires `allowKitOverride: true`); `neoessentials.kits.nocooldown` (or the per-kit `neoessentials.kits.<name>.nocooldown`) bypasses just the cooldown
- `/kitreset <kit>` clears a specific cooldown
- `/kitreset <kit> <player>` requires `neoessentials.kitreset.others`
- A kit becomes permanently unclaimable for a player once `maxUses` is reached (there's no `-1` "cooldown" sentinel — use `maxUses: 1` for one-time kits)

---

*Back to [Wiki Home](Home)*
