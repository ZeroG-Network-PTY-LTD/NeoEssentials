# Leaderboard System

> **Config file:** `config/neoessentials/leaderboard.json`  
> **Reload in-game:** `/leaderboard reload`  
> **Commands:** `/leaderboard` (alias `/lb`) · **Admin permission:** `neoessentials.leaderboard.admin`  
> **Introduced:** v1.0.6 (shop boards, styling & GUI added in a later v1.0.6 update)

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
| **Shop-sales board** | Ranks shops (sign/chest **and** NPC) by total revenue, shown by shop/owner name — the first board that isn't ranking players at all, see [Non-Player Boards](#non-player-boards-shop_sales) |
| **Per-board styling** | Custom chat/GUI line templates, per-rank medal/color, and a per-board GUI icon — see [Styling](#styling-templates-medals--the-gui-viewer) |
| **GUI viewer** | `/leaderboard <board> gui` — a paginated chest-GUI browser, real player heads for player entries |
| **`LeaderboardAPI`** | One-line board registration for other mods, mirroring `PlaceholderAPI` |
| **Placeholders** | `{leaderboard_<board>:<rank>:name\|value\|medal\|rankcolor}` — usable in chat, tablist, the sidebar scoreboard, or holograms |
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
  "_configVersion": 3,
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
        "displayName": "Playtime", "format": "time", "higherIsBetter": true,
        "refreshInterval": 300, "enabled": true },

      { "id": "diamonds_mined", "type": "vanilla_stat", "stat": "minecraft.mined:minecraft.diamond_ore",
        "displayName": "Diamond Ore Mined", "format": "integer", "higherIsBetter": true, "enabled": false },

      { "id": "event_points", "type": "custom", "displayName": "Event Points", "format": "integer",
        "higherIsBetter": true, "enabled": false },

      { "id": "shop_sales", "type": "shop_sales", "displayName": "Top Shops", "format": "currency",
        "higherIsBetter": true, "refreshInterval": 120, "icon": "minecraft:emerald",
        "entryFormat": "{rankColor}{medal} #{rank} &f{name} &8- &a{value}", "enabled": true }
    ]
  }
}
```

### Board Fields

| Field | Type | Description |
|---|---|---|
| `id` | string | Used in `/leaderboard <id>`, `/lb <id>`, and placeholders |
| `type` | string | `"economy"` \| `"vanilla_stat"` \| `"custom"` \| `"shop_sales"` |
| `displayName` | string | Shown in the board's header |
| `format` | string | `"integer"` \| `"time"` \| `"currency"` |
| `higherIsBetter` | boolean | `true` for "top" boards; `false` ranks lowest-first |
| `exemptPermission` | string | Players with this permission are excluded from the ranking (player-keyed boards only — ignored on `shop_sales`, which has no player to check) |
| `refreshInterval` | integer | Seconds a cached ranking is served before rebuilding on next access. Default 60 if omitted; cheap boards can go lower, expensive ones (offline-stats scans, the shop board) may want it higher |
| `entryFormat` | string | Optional custom line template for `/leaderboard`'s chat output **and** the GUI's item names — see [Styling](#styling-templates-medals--the-gui-viewer) |
| `headerFormat` | string | Optional custom header-line template — same section |
| `icon` | string | Item id (e.g. `"minecraft:emerald"`) used as the GUI icon for entries with no player (e.g. a shop). Omit for `minecraft:paper`. Player entries always show their real head regardless |
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

## Non-Player Boards (`shop_sales`)

Every board type above ranks **players** — the entry's identity is a UUID, and its display
name comes from a Mojang profile lookup on that UUID. `shop_sales` is the first board that
doesn't: it ranks **shops** (sign/chest shops and NPC shops) by total revenue, shown by the
shop's own name, not a player's.

- Sign/chest shops are keyed by their `shopId` (or a position-derived key if a shop predates
  that field); NPC shops by their own `shopId`. Neither goes through a Mojang lookup.
- Revenue is tracked per-shop (`totalRevenueCents` on both shop data types), incremented on
  every buy/sell — this data didn't exist before this board was added.
- Shops with zero sales are omitted from the ranking rather than showing a $0.00 entry.
- No `exemptPermission` applies (there's no player to check a permission against).

This is built on a general extension point — `NamedStatProvider` — so any future non-player
ranking (factions, guilds, anything with a stable id and its own name) can be added the same
way without touching the core ranking/caching engine. See
[`LeaderboardAPI`](#leaderboardapi--for-mod-developers) below for how a mod would use it.

---

## Styling: Templates, Medals & the GUI Viewer

Three independent ways to customize how a board looks, all opt-in — every existing board
keeps its original plain look until you add these fields.

### Per-rank medals and colors (works everywhere placeholders do)

Two new placeholder fields, available on **every** board with no config needed:

```
{leaderboard_<board>:<rank>:medal}      → 🥇 / 🥈 / 🥉 on ranks 1-3, empty string otherwise
{leaderboard_<board>:<rank>:rankcolor}  → a gold/silver/bronze color tag on ranks 1-3, empty otherwise
```

Use these in a hologram line, a sidebar scoreboard line, or a tablist line exactly like
`:name`/`:value` — e.g. `{leaderboard_money:1:rankcolor}{leaderboard_money:1:medal} {leaderboard_money:1:name}`.

### Per-board line templates (`entryFormat` / `headerFormat`)

Set `entryFormat` (and optionally `headerFormat`) on a board in `leaderboard.json` to replace
its default plain line — this affects **both** `/leaderboard`'s chat output and the GUI's item
names. Tokens:

| Token | Where | Meaning |
|---|---|---|
| `{rank}` | entry | The rank number |
| `{name}` | entry | Entry's display name |
| `{value}` | entry | Formatted value (currency/time/integer per `format`) |
| `{medal}` | entry | Same as the `:medal` placeholder above |
| `{rankColor}` | entry | Same as the `:rankcolor` placeholder above |
| `{displayName}` | header | The board's `displayName` |
| `{page}` / `{totalPages}` | header | Current/total page number |
| `{age}` | header | Seconds since the ranking was last refreshed |

Templates support the same `&`-codes, hex, and gradient tags as tablist/scoreboard/hologram
lines — no new syntax to learn. A board with no `entryFormat` set keeps rendering through the
original lang-file line exactly as before. Example (from the shipped `shop_sales` board):

```jsonc
"entryFormat": "{rankColor}{medal} #{rank} &f{name} &8- &a{value}"
```

### GUI viewer

```
/leaderboard <board> gui
```

Opens a paginated chest GUI for that board — 45 entries per page, prev/close/next buttons.
Player entries show the player's real head (resolved from their UUID, same technique as the
`/skull` command); non-player entries (shops) show the board's configured `icon` item, or
`minecraft:paper` if none is set. View-only — editing still goes through
`/leaderboard admin set|add|reset` for custom boards.

---

## Commands

| Command | Permission | Description |
|---|---|---|
| `/leaderboard` / `/lb` | `neoessentials.leaderboard.view` | List registered boards |
| `/leaderboard <board> [page]` | view | Show a page of rankings |
| `/leaderboard <board> gui` | view | Open the paginated chest-GUI viewer (see [Styling](#styling-templates-medals--the-gui-viewer)) |
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

`{leaderboard_<board>:<rank>:name|value|medal|rankcolor}`, e.g.:

```
{leaderboard_money:1:name}       → richest player's name
{leaderboard_money:1:value}      → their formatted balance
{leaderboard_kills:3:name}       → 3rd-place player kills
{leaderboard_shop_sales:1:name}  → best-selling shop's name
{leaderboard_money:1:medal}      → 🥇 (empty string past rank 3)
{leaderboard_money:1:rankcolor}  → a gold color tag (empty string past rank 3)
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

`StatProvider` is a two-method functional-shaped interface for **player**-keyed boards:

```java
public interface StatProvider {
    Map<UUID, Number> getAllValues(MinecraftServer server);
    String formatValue(Number value);
}
```

For a board that isn't ranking players (the same problem `shop_sales` solves — see
[Non-Player Boards](#non-player-boards-shop_sales)), implement `NamedStatProvider` instead:

```java
public interface NamedStatProvider extends StatProvider {
    record NamedEntry(String displayName, Number value) {}
    Map<String, NamedEntry> getAllNamedValues(MinecraftServer server); // keyed by YOUR own stable id
}
```

`LeaderboardDefinition` also has a `refreshIntervalSeconds` field (default 60) and optional
`entryFormat`/`headerFormat`/`icon` styling fields — pass them via the full constructor, or
omit for defaults:

```java
LeaderboardAPI.registerBoard(
    new LeaderboardDefinition("mymod_wins", "Arena Wins", "mymod.leaderboard.exempt", true, 300),
    (server) -> myWinsMap());
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
- Once registered, the board immediately works everywhere: `/leaderboard`/`/lb`, `/leaderboard
  <board> gui`, the `{leaderboard_...}` placeholder, and `/leaderboard hologram create`.

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
- **`NamedStatProvider`** — sibling interface to `StatProvider` for non-player boards; `LeaderboardCache`
  checks for it before falling back to the UUID/profile-lookup path. `ShopSalesStatProvider`
  (`leaderboard/adapters/`) is the only current implementation.
- **`LeaderboardStyle`** — the single place rank→medal/color logic lives, shared by the
  placeholder expansion, `/leaderboard`'s template rendering, and the GUI.
- **A config-version note:** `leaderboard.json`'s array-merge (adding a new default board like
  `shop_sales` to an already-upgraded install) only adds boards **missing by id** — it can't
  retroactively add new fields (`entryFormat`/`icon`/etc.) to a board an install already has on
  disk. New installs and newly-added board ids get every field; existing boards on upgraded
  installs keep their on-disk shape until an admin edits the file by hand.

---

*Back to [Wiki Home](Home)*
