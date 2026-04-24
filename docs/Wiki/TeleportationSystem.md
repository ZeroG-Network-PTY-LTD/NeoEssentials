# Teleportation System

> **Version:** 1.0.2.6+build.38 · **Config:** `config.json` → `teleportation` / `generalSettings` sections

---

## Overview

Full teleportation suite — homes, warps, player warps, spawn, TPA requests, random teleport, direct TP commands, and utility teleports. All with safe-location detection, delay/warmup, and `/back` support.

---

## Safe Location Detection

All teleport destinations are checked for safety:
- **Feet block** must have a solid collision shape (correctly handles slabs, stairs, glass, trapdoors)
- **Head block** must be passable (air, non-solid)
- **Dangerous blocks** are rejected: lava, fire, soul fire, magma, cactus, sweet berry bush, wither rose, nether portal, campfire, soul campfire, powder snow
- A top-down column scan finds the surface first, then an expanding XZ radius search as fallback
- Safety can be disabled per feature in config

---

## Homes

### Commands

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/home` | `/home [name]` | `neoessentials.home` | Teleport to a home |
| `/sethome` | `/sethome [name]` | `neoessentials.home.set` | Set home at current location |
| `/delhome` | `/delhome [name]` | `neoessentials.home.delete` | Delete a home |
| `/deletehome` | alias | same | Alias |
| `/homes` | `/homes` | `neoessentials.home.list` | List all homes |
| `/renamehome` | `/renamehome <old> <new>` | `neoessentials.renamehome` | Rename a home |

### Config (`teleportation.homeSettings`)

| Key | Default | Description |
|---|---|---|
| `maxHomes` | `5` | Max homes per player |
| `allowCrossDimensionHomes` | `true` | Allow homes in other dimensions |
| `enableHomeTeleportSafety` | `true` | Check safe location on home TP |
| `homeTeleportCooldown` | `0` | Seconds a player must wait between `/home` uses (0 = disabled) |
| `cancelOnMovement` | `true` | Cancel if player moves during warm-up delay |

> `teleportDelay` (warmup before the teleport fires) is a **global** setting in `generalSettings.teleportDelay`, not per-feature.

---

## Warmup & Cooldown Behaviour

| Setting | Config key | Section | Description |
|---|---|---|---|
| Warmup delay | `generalSettings.teleportDelay` | `config.json` | Seconds to wait before any teleport completes. `0` = instant. |
| Warmup countdown | `generalSettings.enableTeleportWarmup` | `config.json` | Show a countdown message to the player during the warmup. |
| Cancel on move | `teleportation.homeSettings.cancelOnMovement` | same for warp/spawn | Cancel teleport if the player moves during warmup. |
| Home cooldown | `teleportation.homeSettings.homeTeleportCooldown` | `config.json` | Seconds between successive `/home` uses. `0` = no cooldown. |
| Warp cooldown | `teleportation.warpSettings.warpCooldown` | `config.json` | Seconds between successive `/warp` uses. `0` = no cooldown. |
| Spawn cooldown | `teleportation.spawnSettings.spawnCooldown` | `config.json` | Seconds between successive `/spawn` uses. `0` = no cooldown. |

---

## Warps

### Commands

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/warp` | `/warp [name] [player]` | `neoessentials.teleport.warp` | Teleport to a warp (or warp another player) |
| `/warp` | `/warp` or `/warp <page>` | same | List all warps (paginated) |
| `/setwarp` | `/setwarp <name>` | `neoessentials.teleport.warp.create` | Create a warp |
| `/delwarp` | `/delwarp <name>` | `neoessentials.teleport.warp.delete` | Delete a warp |
| `/warps` | `/warps [page]` | `neoessentials.teleport.warp.list` | List warps (20 per page) |
| `/warpinfo` | `/warpinfo <name>` | `neoessentials.warpinfo` | Show warp coordinates and world |
| `/pwarp` | `/pwarp [name]` | `neoessentials.teleport.pwarp` | Teleport to your player warp |
| `/setpwarp` | `/setpwarp <name>` | `neoessentials.teleport.pwarp.create` | Create a player warp |
| `/delpwarp` | `/delpwarp <name>` | `neoessentials.teleport.pwarp.delete` | Delete a player warp |
| `/pwarps` | `/pwarps` | `neoessentials.teleport.pwarp.list` | List your player warps |

Set `perWarpPermission: true` in config to require `neoessentials.warps.<name>` per warp.

> **Warp cooldown** is configured via `teleportation.warpSettings.warpCooldown` in `config.json` (seconds; `0` = no cooldown).

---

## Spawn

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/spawn` | `/spawn [player]` | `neoessentials.teleport.spawn` | Teleport to server spawn |
| `/setspawn` | `/setspawn` | `neoessentials.teleport.spawn.set` | Set spawn at current location |

> **Spawn cooldown** is configured via `teleportation.spawnSettings.spawnCooldown` in `config.json` (seconds; `0` = no cooldown).
> **Warmup** is controlled globally by `generalSettings.teleportDelay` + `generalSettings.enableTeleportWarmup`.

---

## Teleport Requests (TPA)

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/tpa` | `/tpa <player>` | `neoessentials.teleport.tpa` | Request to TP to a player |
| `/tpahere` | `/tpahere <player>` | `neoessentials.teleport.tpahere` | Request player TP to you |
| `/tpaccept` | `/tpaccept` | `neoessentials.teleport.tpaccept` | Accept incoming request |
| `/tpdeny` | `/tpdeny` | `neoessentials.teleport.tpdeny` | Deny incoming request |
| `/tpacancel` | `/tpacancel` | `neoessentials.teleport.tpacancel` | Cancel your outgoing request |
| `/tptoggle` | `/tptoggle [on\|off]` | `neoessentials.tptoggle` | Toggle accepting TP requests |
| `/tpauto` | `/tpauto [on\|off]` | `neoessentials.tpauto` | Auto-accept all incoming TPA requests |
| `/tpaall` | `/tpaall [player]` | `neoessentials.tpaall` | Send TPA-here to all online players |

---

## Random Teleport

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/tpr` | `/tpr [location]` | `neoessentials.teleport.tpr` | Teleport to a random location |
| `/rtp` | alias | same | Alias |
| `/randomtp` | alias | same | Alias |
| `/settpr` | `/settpr <name>` | `neoessentials.teleport.settpr` | Set a named RTP centre point |

### Config (`teleportation.randomTeleportSettings`)

| Key | Default | Description |
|---|---|---|
| `defaultMinRange` | `500` | Minimum distance from centre |
| `defaultMaxRange` | `10000` | Maximum distance from centre |
| `findAttempts` | `10` | Attempts to find a safe spot |
| `cooldown` | `300` | Seconds between uses per player |
| `cacheThreshold` | `10` | Pre-computed location cache size |
| `excludedBiomes` | `[ocean, deep_ocean, void]` | Biomes to avoid |

---

## Admin Teleport Commands

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/tp` | `/tp <player>` or `/tp <x y z>` | `neoessentials.teleport.admin.tp` | Teleport to player or coords |
| `/tphere` | `/tphere <player>` | `neoessentials.teleport.admin.tphere` | Bring player to you |
| `/tpall` | `/tpall` | `neoessentials.teleport.admin.tpall` | Bring all players to you |
| `/tppos` | `/tppos <x> <y> <z>` | `neoessentials.teleport.admin.tppos` | Teleport to exact coordinates |
| `/tpo` | `/tpo <player>` | `neoessentials.teleport.tpo` | TP to player, bypasses tptoggle |
| `/tpohere` | `/tpohere <player>` | `neoessentials.teleport.tpohere` | Bring player, bypasses tptoggle |
| `/tpoffline` | `/tpoffline <player>` | `neoessentials.teleport.tpoffline` | TP to offline player's last position |
| `/world` | `/world [dimension] [player]` | `neoessentials.world` | Teleport to a world/dimension |

---

## Utility Teleports

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/back` | `/back` | `neoessentials.teleport.back` | Return to previous location |
| `/top` | `/top` | `neoessentials.teleport.top` | Teleport to highest block above you |
| `/jump` | `/jump` | `neoessentials.teleport.jump` | Teleport to block you're looking at |
| `/bottom` | `/bottom` | `neoessentials.bottom` | Teleport to bottom of world at your X/Z |

---

## Data Files

| File | Contents |
|---|---|
| `neoessentials/homes.json` | Player UUID → named home locations |
| `neoessentials/warps.json` | Server warp locations |
| `neoessentials/player_warps.json` | Player-created warp locations |
| `neoessentials/spawn.json` | Spawn location (coordinates & world) |

---

*Back to [Wiki Home](Home)*
