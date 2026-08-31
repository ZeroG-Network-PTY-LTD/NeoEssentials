# Teleportation System

> **Version:** 1.0.5+build.54 · **Config:** `config.json` → `teleportation` / `generalSettings` sections

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

## Chunk Loading & Safety Interaction

Before any teleport fires, NeoEssentials **force-loads the destination chunks** to ensure the target location is fully loaded in memory. This happens regardless of whether safety checks are enabled.

### How it works

1. A **3×3 grid of chunks** centred on the destination is loaded via `ServerLevel.getChunkSource().addRegionTicket()` using the `FORCED` ticket type.
2. Safety validation (if enabled) runs **after** chunk loading, so the safety scanner always has valid block data to work with.
3. Once the teleport completes the forced-load tickets are released; normal chunk unloading resumes.

### Disabling safety checks

Setting `enableHomeTeleportSafety`, `enableWarpSafety`, or `enableSpawnSafety` to `false` in `config.json` **completely bypasses** the block-level validation step. The chunks are still preloaded, but the player is teleported directly to the stored coordinates without any safe-location search.

> **Warning:** Disabling safety can land players inside blocks or above the void if the destination has changed since the location was saved.

### When teleport fails due to unloaded chunks

If chunk loading itself fails (e.g., the target dimension is unavailable or the world is being unloaded), the teleport is cancelled with a descriptive error message that includes the **world name** and **coordinates** so the player knows exactly where the failed destination was.

### Configuration quick-reference

| Config key | Section | Effect on chunk loading |
|---|---|---|
| `enableHomeTeleportSafety` | `homeSettings` | Disables safety scan; chunks still preloaded |
| `enableWarpSafety` | `warpSettings` | Disables safety scan; chunks still preloaded |
| `enableSpawnSafety` | `spawnSettings` | Disables safety scan; chunks still preloaded |
| `teleportDelay` (`generalSettings`) | `generalSettings` | Chunks are preloaded at warmup start, not at fire time |

---

## Homes

### Commands

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/home` | `/home [name]` | `neoessentials.teleport.home` | Teleport to a home |
| `/sethome` | `/sethome [name]` | `neoessentials.teleport.home.set` | Set home at current location |
| `/delhome` | `/delhome [name]` | `neoessentials.teleport.home.delete` | Delete a home |
| `/deletehome` | alias | same | Alias |
| `/homes` | `/homes` | `neoessentials.teleport.home.list` | List all homes |
| `/renamehome` | `/renamehome <old> <new>` | `neoessentials.renamehome` | Rename a home |

> Aliases: `/h` for `/home` · `/createhome` for `/sethome` · `/deletehome`, `/removehome`, `/rhome` for `/delhome` · `/homelist`/`/listhomes` for `/homes`. `/h`, `/createhome`, `/removehome`, `/rhome` are skipped automatically if a conflicting mod (FTB Essentials, Essentials) is detected.
>
> Overwriting an existing home with `/sethome <name>` requires confirmation, same as `/delhome` — you'll be prompted with clickable `/sethome confirm`/`/sethome deny` buttons.

### Config (`teleportation.homeSettings`)

| Key | Default | Description |
|---|---|---|
| `maxHomes` | `5` | Max homes per player |
| `allowCrossDimensionHomes` | `true` | Allow homes in other dimensions |
| `homeSetCooldown` | `30` | Seconds between `/sethome` uses (0 = disabled) |
| `homeTeleportCooldown` | `5` | Seconds a player must wait between `/home` uses (0 = disabled) |
| `homeDeleteCooldown` | `10` | Seconds between `/delhome` uses (0 = disabled) |
| `requireConfirmationForDelete` | `true` | Require a confirmation dialog before deleting a home |
| `enableHomeTeleportSafety` | `true` | Check safe location on home TP |
| `logHomeActions` | `true` | Log home set/delete/teleport actions |

> `teleportDelay` (warmup before the teleport fires) and `cancelOnMovement` (cancel if the player moves during warm-up) are **global** settings under `generalSettings`, not per-feature — see the table below.

---

## Warmup & Cooldown Behaviour

| Setting | Config key | Section | Description |
|---|---|---|---|
| Warmup delay | `generalSettings.teleportDelay` | `config.json` | Seconds to wait before any teleport completes. `0` = instant. |
| Warmup countdown | `generalSettings.enableTeleportWarmup` | `config.json` | Show a countdown message to the player during the warmup. |
| Cancel on move | `teleportation.generalSettings.cancelOnMovement` | `config.json` | Cancel teleport if the player moves during warmup (applies to all teleport types). |
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

> Aliases: `/createwarp`/`/addwarp` for `/setwarp` · `/deletewarp`, `/removewarp`, `/rwarp` for `/delwarp` · `/warplist`/`/listwarps` for `/warps`.

Set `perWarpPermission: true` in config to require `neoessentials.warps.<name>` per warp.

> **Warp cooldown** is configured via `teleportation.warpSettings.warpCooldown` in `config.json` (seconds; `0` = no cooldown).
>
> **Server-wide warp limit** defaults to 50 total warps and, unlike the per-feature settings
> above, is **not** currently read from `config.json` at runtime — it's tracked in the warp
> data store's own internal record. The dashboard's Teleportation settings page shows/edits a
> `warpSettings.maxWarps` value in `config.json`, but changing it there has no effect on the
> actual enforced limit.

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
| `/tpa` | `/tpa <player>` | `neoessentials.teleport.request.tpa` | Request to TP to a player |
| `/tpahere` | `/tpahere <player>` | `neoessentials.teleport.request.tpahere` | Request player TP to you |
| `/tpaccept` | `/tpaccept` | `neoessentials.teleport.request.accept` | Accept incoming request |
| `/tpdeny` | `/tpdeny` | `neoessentials.teleport.request.deny` | Deny incoming request |
| `/tpcancel` | `/tpcancel` | `neoessentials.teleport.request.cancel` | Cancel your outgoing request |
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
| `/randomteleport` | alias | same | Alias |
| `/settpr` | `/settpr <name>` | `neoessentials.teleport.settpr` | Set a named RTP centre point |

### Config (`teleportation.randomTeleportSettings`)

| Key | Default | Description |
|---|---|---|
| `defaultMinRange` | `0` | Minimum distance from centre (global default) |
| `defaultMaxRange` | `5000` | Maximum distance from centre (-1 = half the world border). See [Performance](#performance-why-rtp-could-lag-or-crash-a-server) below before raising this much on a server without pre-generated terrain |
| `findAttempts` | `6` | Attempts to find a safe spot before giving up |
| `cooldown` | `60` | Seconds between uses per player |
| `cacheThreshold` | `5` | Pre-computed location cache size to maintain per named location |
| `prewarmBatchSize` | `2` | Max NEW cache slots one `/tpr` use may force-generate in the background right after teleporting — see [Performance](#performance-why-rtp-could-lag-or-crash-a-server) |
| `excludedBiomes` | `[]` | Biomes excluded globally (empty by default) |
| `mode` | `"command"` | `"command"` = `/tpr` teleports instantly (unchanged default behavior). `"gui"` = bare `/tpr` (no location argument) opens a chest-GUI biome picker instead — see below |
| `biomeSearchRadius` | `6400` | GUI-only — search radius in blocks for a biome-targeted teleport, same engine `/locate biome` uses |
| `biomeSearchStep` | `32` | GUI-only — horizontal search step; larger = faster but coarser |
| `biomeMenuItems` | `[]` | GUI-only — pin specific biomes to fixed slots and/or override their icon, see below |

> The built-in `"default"` named location under `randomTeleportSettings.locations` additionally excludes all ocean variants and `minecraft:the_void` — configure per-location via `/settpr <name>`.

### Biome-Select GUI (`mode: "gui"`)

When enabled, bare `/tpr`/`/rtp` opens a paginated chest GUI instead of teleporting instantly:

- **One slot per biome** the current dimension can actually generate — this is read live from
  the dimension's own generator (`BiomeSource.possibleBiomes()`), so **modded biomes appear
  automatically** with no configuration needed. 45 biomes per page, with prev/close/next nav
  buttons.
- **A dedicated "Random — Any Biome" button** (bottom-right) behaves exactly like plain
  `/tpr` — no biome constraint, uses the normal cached random-teleport path.
- Clicking a specific biome closes the GUI and searches for that biome using the same engine
  `/locate biome` uses (`ServerLevel.findClosestBiome3d`) from a jittered center within the
  configured range, then finds a safe Y the same way plain RTP does. If no match turns up
  within `biomeSearchRadius` blocks **or** the match falls outside the world border, the
  player gets a clear "could not find that biome within the server border" error instead of
  being teleported somewhere unexpected.
- `/tpr <locationName>` (an explicit location argument) always bypasses the GUI, even in
  `"gui"` mode — the GUI only intercepts the bare, no-argument form.
- Same `neoessentials.teleport.tpr` permission gates both the plain command and the GUI —
  no separate permission node.

#### Biome icons

Every biome gets a default icon — a wooded biome shows its own sapling/propagule/fungus (the
"growable" item, not the log), and biomes with no tree of their own show a block unique to
that biome (sand for desert, water bucket for ocean, netherrack for nether wastes, etc.).
Anything not in that built-in table (every biome from a mod that isn't specifically supported)
falls back to a generic `minecraft:grass_block` icon.

#### `biomeMenuItems` — pinning and icon overrides

```jsonc
"biomeMenuItems": [
  // Pin to a fixed slot (0 = top-left box, 1 = next box, ... 44 = last box on page 1),
  // keep the default icon (spruce_sapling, from the built-in table):
  { "slot": 0, "biome": "minecraft:taiga" },

  // Pin to a slot AND override the icon:
  { "slot": 1, "biome": "minecraft:desert", "item": "minecraft:cactus" },

  // Icon-only override, no slot — stays auto-positioned like any other biome,
  // just with a custom icon (this is also how to support a biome mod's own
  // saplings, since this mod can't auto-detect a modded biome's tree species):
  { "biome": "biomesoplenty:redwood_forest", "item": "biomesoplenty:redwood_sapling" }
]
```

Both `slot` and `item` are independently optional. A pin only reserves its slot **on page 1**;
pinned biomes are removed from the auto-listed pool (shown once, at their pinned position) —
every other biome, including unrecognized/modded ones, still fills the remaining slots
automatically in alphabetical order, continuing onto later pages exactly as if no pins existed.
A pin referencing a biome that doesn't exist in the current dimension, or a `slot` outside
0-44, is silently skipped — it can't break the GUI.

### Cancelling an in-progress `/tpr`

`/tpr` runs through the same warmup as `/home`/`/warp`/`/tpa` (`teleportation.generalSettings.
teleportDelay`, 3 seconds by default) — **just move and it cancels**, same as any other
teleport (`generalSettings.cancelOnMovement`, on by default). `/tpr` now announces this
("Teleporting in 3 second(s) — move to cancel.") when the warmup starts, so it's actually
visible instead of a silent delay with no indication it could be interrupted. There's no
separate `/tpr cancel` command — moving during the warmup already is the cancel.

### Performance — why RTP could lag or crash a server

Every teleport into terrain that hasn't been visited/generated before forces a **real, main-
thread-blocking** chunk generation — there's no way around this for a genuinely random
destination, but a naive implementation can force far more of these per `/tpr` than actually
necessary, which is exactly what was happening before this was fixed:

- The safety-preload step used to force-generate a 3×3 grid (9 chunks) around **every**
  teleport destination unconditionally, even for RTP, which already verifies its own landing
  spot and never reads the 8 neighbour chunks at all. Fixed — RTP now only force-loads the one
  chunk it actually lands in.
- Refilling the background location cache after a successful teleport used to fire the entire
  gap up to `cacheThreshold` (up to 10 forced generations) as one burst immediately after the
  teleport — a second lag spike stacked right on top of the teleport's own cost. Fixed via
  `prewarmBatchSize` — now only a couple of new slots are force-generated per use, and the
  cache tops back up gradually across several `/tpr` calls instead of all at once.
- `defaultMaxRange`/`findAttempts`/`cacheThreshold` defaults were lowered (10000→5000,
  10→6, 10→5) — smaller/fewer attempts by default means less chance of repeatedly pushing into
  brand-new, ungenerated territory on a server that hasn't pre-generated terrain around itself.

**If `/tpr` is still causing lag/crashes after updating:** pre-generate terrain out to your
players' expected roaming radius (a chunk pre-generator mod, or just having players explore
normally, both work — subsequent `/tpr` uses landing in already-generated terrain are cheap).
Lowering `defaultMaxRange` further, or `findAttempts`, reduces worst-case cost at the expense
of a higher "no safe location found" retry rate.

---

## Admin Teleport Commands

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/tp` | `/tp <player>` or `/tp <x y z>` | `neoessentials.teleport.tp` | Teleport to player or coords |
| `/tphere` | `/tphere <player>` | `neoessentials.teleport.tphere` | Bring player to you |
| `/tpall` | `/tpall` | `neoessentials.teleport.admin.tpall` | Bring all players to you |
| `/tppos` | `/tppos <x> <y> <z>` | `neoessentials.teleport.tppos` | Teleport to exact coordinates |
| `/tpo` | `/tpo <player>` | `neoessentials.teleport.admin.tpo` | TP to player, bypasses tptoggle |
| `/tpohere` | `/tpohere <player>` | `neoessentials.teleport.tpohere` | Bring player, bypasses tptoggle |
| `/tpoffline` | `/tpoffline <player>` | `neoessentials.teleport.tpoffline` | TP to offline player's last position |
| `/world` | `/world [dimension] [player]` | `neoessentials.world` | Teleport to a world/dimension |

---

## Utility Teleports

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/back` | `/back` | `neoessentials.teleport.back` | Return to previous location |
| `/top` | `/top` | `neoessentials.teleport.top` | Teleport to highest block above you |
| `/jump` | `/jump` | `neoessentials.teleport.jump` | Jump through walls (short-range teleport in the direction you're facing) |
| `/jumpto` | `/jumpto` | `neoessentials.teleport.jumpto` | Teleport to the block you're looking at |
| `/bottom` | `/bottom` | `neoessentials.bottom` | Teleport to bottom of world at your X/Z |

---

## Data Files

All teleportation data is persisted through the pluggable **DataStore** backend (JSON by
default — see [Storage Backend](Storage)), not dedicated bespoke files:

| Collection | Contents |
|---|---|
| `playerdata_homes` | Per-player named home locations (one record per player, id = UUID) |
| `playerdata_back_locations` | Per-player `/back` return location |
| `warps` | Server warp locations |
| `player_warps` | Player-created warp locations |
| `spawn` | Spawn location (coordinates & world) |

> **Legacy files:** `neoessentials/playerdata/homes/<uuid>.json`, `neoessentials/homes.json`,
> `neoessentials/warps.json`, `playerwarps.json`, and `neoessentials/spawn.json` are the
> pre-DataStore on-disk formats. They are only read once, automatically, to migrate their
> contents into the DataStore collections above — they are never written to again afterward.

---

*Back to [Wiki Home](Home)*
