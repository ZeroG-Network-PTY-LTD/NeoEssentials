# Leaderboard System

> **Config file:** `config/neoessentials/leaderboard.json`  
> **Reload in-game:** `/leaderboard reload`  
> **Commands:** `/leaderboard` (alias `/lb`) · **Admin permission:** `neoessentials.leaderboard.admin`  
> **Introduced:** v1.0.6

---

## Overview

A generalized, config-driven ranked-stat system. `/baltop` still works exactly as before and
is untouched — the leaderboard system is a superset built alongside it, not a replacement.

| Feature | Details |
|---|---|
| **Config-driven boards** | Register any vanilla-tracked per-player stat as a board just by adding a `leaderboard.json` entry — no code |
| **Economy boards** | Ranks by balance (same data `/baltop` uses) |
| **Vanilla stat boards** | Blocks mined by type, mobs killed by type, distance traveled, deaths, playtime, ... — anything vanilla's own `/scoreboard objectives add` criteria grammar can reference |
| **Custom boards** | Point totals nothing in Minecraft tracks — admin-settable, dashboard-settable, or fed by another mod |
| **`LeaderboardAPI`** | One-line board registration for other mods, mirroring `PlaceholderAPI` |
| **Placeholders** | `{leaderboard_<board>:<rank>:name\|value}` — usable in chat, tablist, the sidebar scoreboard, or holograms |
| **Hologram generator** | `/leaderboard hologram create <board> <id> [lines]` |
| **Dashboard endpoint** | `/api/leaderboard` |

---

## Why Vanilla Stats Need No New Tracking

Minecraft already tracks dozens of per-player statistics on its own — kills, deaths,
playtime, blocks mined (per block type), mobs killed (per entity type), distance traveled by
every movement type, damage dealt/taken, items crafted/used/broken, and more — persisted to
`<world>/stats/<uuid>.json` whether the player is online or not. The `vanilla_stat` board type
reads this directly:

- **Online players** — the live in-memory value via `ServerPlayer.getStats()`, no disk I/O.
- **Offline players** — read straight from their stats JSON file.

Zero new event listeners, zero new storage. Any stat vanilla already knows about is a config
entry away from being a leaderboard.

---

## Configuration

### Full `leaderboard.json` structure

```jsonc
{
  "_configVersion": 1,
  "leaderboard": {
    "boards": [
      { "id": "money", "type": "economy", "displayName": "Balance", "format": "currency",
        "higherIsBetter": true, "exemptPermission": "neoessentials.economy.baltop.exempt",
        "enabled": true },

      { "id": "kills", "type": "vanilla_stat", "stat": "minecraft.custom:minecraft.player_kills",
        "displayName": "Player Kills", "format": "integer", "higherIsBetter": true,
        "exemptPermission": "neoessentials.leaderboard.kills.exempt", "enabled": true },

      { "id": "mob_kills", "type": "vanilla_stat", "stat": "minecraft.custom:minecraft.mob_kills",
        "displayName": "Mob Kills", "format": "integer", "higherIsBetter": true, "enabled": true },

      { "id": "playtime", "type": "vanilla_stat", "stat": "minecraft.custom:minecraft.play_time",
        "displayName": "Playtime", "format": "time", "higherIsBetter": true, "enabled": true },

      { "id": "diamonds_mined", "type": "vanilla_stat", "stat": "minecraft.mined:minecraft.diamond_ore",
        "displayName": "Diamond Ore Mined", "format": "integer", "higherIsBetter": true, "enabled": false },

      { "id": "event_points", "type": "custom", "displayName": "Event Points", "format": "integer",
        "higherIsBetter": true, "enabled": false }
    ]
  }
}
```

### Board Fields

| Field | Type | Description |
|---|---|---|
| `id` | string | Used in `/leaderboard <id>`, `/lb <id>`, and placeholders |
| `type` | string | `"economy"` \| `"vanilla_stat"` \| `"custom"` |
| `displayName` | string | Shown in the board's header |
| `format` | string | `"integer"` \| `"time"` \| `"currency"` |
| `higherIsBetter` | boolean | `true` for "top" boards; `false` ranks lowest-first |
| `exemptPermission` | string | Players with this permission are excluded from the ranking |
| `enabled` | boolean | `false` hides the board without deleting its definition |
| `stat` | string | **`vanilla_stat` only** — see below |

### `vanilla_stat` Criteria Strings

Uses vanilla's own scoreboard-objective criteria grammar — the exact same string
`/scoreboard objectives add <name> <criteria>` accepts in-game:

```
<stat type>.<type path>:<value namespace>.<value path>
```

| Category | Example | Meaning |
|---|---|---|
| `minecraft.custom` | `minecraft.custom:minecraft.player_kills` | Kills, deaths, playtime, distance, jumps, etc. |
| `minecraft.mined` | `minecraft.mined:minecraft.diamond_ore` | Blocks mined, by block id |
| `minecraft.killed` | `minecraft.killed:minecraft.zombie` | Mobs killed, by entity type id |
| `minecraft.crafted` / `minecraft.used` / `minecraft.broken` / `minecraft.picked_up` / `minecraft.dropped` | `minecraft.crafted:minecraft.diamond_pickaxe` | Item events, by item id |

Look up exact value ids the same way you would for a vanilla `/scoreboard` objective — this
mod doesn't invent its own stat names. A `stat` string that doesn't resolve to a real
per-player stat logs a clear warning and that one board is skipped — it can't break server
startup.

### Custom Boards

Point totals nothing in Minecraft tracks. Persisted through a single shared `DataStore`
collection (`leaderboard_custom`, records keyed `"<boardId>:<uuid>"`) — same
single-collection, write-immediately pattern as `PayToggleManager`.

---

## Commands

| Command | Permission | Description |
|---|---|---|
| `/leaderboard` / `/lb` | `neoessentials.leaderboard.view` | List registered boards |
| `/leaderboard <board> [page]` | view | Show a page of rankings |
| `/leaderboard reload` | `neoessentials.leaderboard.admin` | Reload `leaderboard.json` |
| `/leaderboard admin set <board> <player> <value>` | admin | Set a **custom** board's value |
| `/leaderboard admin add <board> <player> <delta>` | admin | Add to a **custom** board's value |
| `/leaderboard admin reset <board> <player>` | admin | Reset a **custom** board's value to 0 |
| `/leaderboard admin create <id> <displayName>` | admin | Define a new **custom** board, persisted to `leaderboard.json` |
| `/leaderboard admin delete <board>` | admin | Delete a **custom** board |
| `/leaderboard hologram create <board> <id> [lines]` | admin | Generate a ranked leaderboard hologram (see [Holograms](#holograms) below) |

`set`/`add`/`reset`/`delete` only work on `type: "custom"` boards — `economy`/`vanilla_stat`
boards are config-file-only (an admin should hand-verify a stat key in the file, not
free-type one into a chat argument).

`/baltop`, `/balancetop`, `/btop` remain registered and fully independent — they don't route
through this system at all, so nothing about existing setups changes.

---

## Placeholders

`{leaderboard_<board>:<rank>:name}` / `{leaderboard_<board>:<rank>:value}`, e.g.:

```
{leaderboard_money:1:name}    → richest player's name
{leaderboard_money:1:value}   → their formatted balance
{leaderboard_kills:3:name}    → 3rd-place player kills
```

Works anywhere the placeholder system is resolved: chat, [Tablist](TablistSystem),
[Scoreboard](ScoreboardSystem), and holograms. An out-of-range rank resolves to an empty
string rather than an error.

---

## Holograms

`/leaderboard hologram create <board> <id> [lines]` (default `lines` = 10, max 15) spawns a
hologram at your position with a title line plus N ranked lines using
`{leaderboard_<board>:<rank>:name}` / `:value}`. This is a pure convenience generator — no new
rendering mechanism — the [Hologram System](HologramSystem#placeholders) already resolves
`{placeholder}` tokens live on its own refresh timer, so the generated lines keep updating on
their own exactly like any other hologram placeholder.

```
/leaderboard hologram create money top_balances 5
/hologram setrefresh top_balances 30
```

**One caveat:** a hologram is a single shared entity, not per-player packets — its
placeholders resolve using whichever player happens to be nearest at refresh time (harmless
for leaderboard placeholders specifically, since ranks are global, not per-viewer). If nobody
is anywhere near the hologram's dimension at a refresh tick, the token can't resolve that
tick and shows literally until someone's nearby again.

---

## `LeaderboardAPI` — For Mod Developers

Same one-line integration story as [`PlaceholderAPI`](APISystem#custom-placeholders-java-api).

```java
import com.zerog.neoessentials.leaderboard.LeaderboardAPI;
import com.zerog.neoessentials.leaderboard.LeaderboardDefinition;
import com.zerog.neoessentials.leaderboard.StatProvider;

// Call during your mod's own init/server-starting hook:
LeaderboardAPI.registerBoard(
    new LeaderboardDefinition("mymod_wins", "Arena Wins", "mymod.leaderboard.exempt", true),
    (server) -> myWinsMap()   // StatProvider: Map<UUID, Number> getAllValues(MinecraftServer)
);
```

`StatProvider` is a two-method functional-shaped interface:

```java
public interface StatProvider {
    Map<UUID, Number> getAllValues(MinecraftServer server);
    String formatValue(Number value);
}
```

```java
LeaderboardAPI.unregisterBoard("mymod_wins");
LeaderboardCache cache = LeaderboardAPI.getBoard("mymod_wins"); // read current standings
```

**Requirements & caveats:**
- Requires NeoEssentials as a compile/runtime dependency, same as any cross-mod NeoForge API.
- Boards registered this way are **in-memory only** — not written to `leaderboard.json`, and
  won't survive a `/leaderboard reload` unless you re-register on your own startup hook (same
  contract `PlaceholderExpansion` already has).
- Once registered, the board immediately works everywhere: `/leaderboard`/`/lb`, the
  `{leaderboard_...}` placeholder, and `/leaderboard hologram create`.

---

## Web Dashboard API

`/api/leaderboard` (auth required for reads, `auth-admin` for writes):

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/leaderboard` | List registered board ids + display names |
| `GET` | `/api/leaderboard/{board}?page=N` | Paginated entries for one board |
| `POST` | `/api/leaderboard/boards` | Create a custom board — `{ id, displayName }` |
| `PUT` | `/api/leaderboard/boards/{id}/value` | Set a custom board's value — `{ player, value }` |
| `DELETE` | `/api/leaderboard/boards/{id}` | Delete a custom board |

Write endpoints only accept `type: "custom"` boards — same restriction as the in-game
`admin` subcommands.

---

## Architecture Notes (for maintainers)

- **`LeaderboardManager`** — the board registry (`com.zerog.neoessentials.leaderboard`). Tracks
  which board ids came from `leaderboard.json` (`configManaged`) so a `/leaderboard reload`
  never wipes out boards another mod registered via `LeaderboardAPI`.
- **`LeaderboardCache`** — per-board async cache: 60s staleness window, build-in-flight/
  rebuild-queued handling for invalidations mid-build — the generalized lift of `BaltopCommand`'s
  original static-field caching into one instance per board.
- **`LeaderboardConfigLoader`** — reads `leaderboard.json`, builds the right `StatProvider` per
  entry, and tracks which board ids are actually `type: "custom"` (distinct from merely
  config-managed — an `economy`/`vanilla_stat` board is config-managed too, but isn't
  custom-editable).

---

*Back to [Wiki Home](Home)*
