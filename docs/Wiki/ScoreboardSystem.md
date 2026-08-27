# Sidebar Scoreboard System

> **Config file:** `config/neoessentials/scoreboard.json`  
> **Reload in-game:** `/scoreboard reload`  
> **Admin permission:** `neoessentials.scoreboard.admin` · **Toggle permission:** `neoessentials.scoreboard.toggle`  
> **Introduced:** v1.0.6

---

## Overview

The sidebar scoreboard system renders the classic right-hand-side Minecraft scoreboard —
title + up to 15 lines — fully server-side, config-driven, with animation, conditional
boards, and per-group/per-player overrides.

| Feature | Details |
|---|---|
| **Named boards** | Multiple boards, each with its own conditions; the first matching one wins |
| **Conditions** | `perm:<node>`, `world:<name>`, `{placeholder} == value` / `!= value` |
| **Animated title/lines** | Frame arrays, cycled at `refreshInterval` |
| **Per-group / per-player overrides** | Title and individual line overrides, same priority order as [Tablist](TablistSystem) |
| **Persisted per-player toggle** | `/scoreboard toggle` remembers each player's preference across sessions |
| **Join delay** | Configurable grace period before a player's board first appears |
| **True per-viewer rendering** | Unlike a vanilla `/scoreboard` objective (server-wide, one value for everyone), different players can see different boards or different placeholder values *simultaneously* — see [Technical Notes](#technical-notes) |
| **Dashboard endpoint** | `/api/scoreboard` — read/create/update boards, toggle enabled |

---

## Why This Isn't Just a Vanilla `/scoreboard` Objective

Vanilla's scoreboard `Objective`/`Team`/`Score` state lives on the server's single shared
`ServerScoreboard` and is broadcast identically to every connected player. That's fine for a
tablist row (a player's own nametag looks the same to every viewer) but wrong for a sidebar:
two players might be shown different boards (one in the Nether, one in the Overworld) or the
same board with different values (each player's own balance).

NeoEssentials solves this by building the four scoreboard packets
(`ClientboundSetObjectivePacket`, `ClientboundSetPlayerTeamPacket`, `ClientboundSetScorePacket`,
`ClientboundResetScorePacket`) against a private, never-registered `Scoreboard` instance
purely to serialize them, then sending them **directly to one player's connection** — the
same technique already used for the tablist header/footer packet. No two players' boards can
interfere with each other, and nothing is ever visible to a player other than the one the
packet was sent to.

---

## Configuration

### Full `scoreboard.json` structure

```jsonc
{
  "_configVersion": 1,
  "scoreboard": {
    "enabled": true,
    "refreshInterval": 20,      // ticks between updates (20 = 1s)
    "joinDelayTicks": 40,       // wait this long after join before showing a board
    "toggleCommandEnabled": true,

    "boards": [
      {
        "name": "default",
        "priority": 0,
        "conditions": [],       // empty = always matches — MUST be last (fallback)
        "title": "<gradient:FFD700-FF8C00>&l{server_name}",
        "titleFrames": [],      // optional: animated title, same shape as tablist header
        "lines": [
          { "text": "&7&m                    " },
          { "text": "&eOnline: &f{online}&7/&f{max}" },
          { "text": "&eBalance: &f${balance}" },
          { "text": "&eGroup: &f{group}" },
          { "text": "&6Richest: &f{leaderboard_money:1:name}" }
        ]
      },
      {
        "name": "nether_board",
        "priority": 10,
        "conditions": [ "world:the_nether" ],
        "title": "&c&lNETHER",
        "lines": [ { "text": "&7Danger zone" } ]
      }
    ],

    "groups":  { },
    "players": { }
  }
}
```

### Core Fields

| Field | Type | Default | Description |
|---|---|---|---|
| `enabled` | boolean | `true` | Enable/disable the whole system |
| `refreshInterval` | int | `20` | Update interval in ticks |
| `joinDelayTicks` | int | `40` | Ticks after join before the board first appears |
| `boards` | array | — | Named board definitions (see below) |
| `groups` | object | — | Per-group `titleOverride`/`lineOverrides` |
| `players` | object | — | Per-player UUID `titleOverride`/`lineOverrides` |

### Board Fields

| Field | Type | Description |
|---|---|---|
| `name` | string | Board identifier |
| `priority` | int | Boards are evaluated highest-priority first; first whose `conditions` all pass wins |
| `conditions` | array | List of condition strings, implicit AND. Empty = always matches |
| `title` | string or array | Static string, or array for animated frames |
| `titleFrames` | array | Alternate way to specify animated title frames |
| `lines` | array | Up to 15 `{ "text": ..., "condition": ... }` objects |

A line's own `condition` (optional, single string) controls whether that *specific line* is
shown — hidden lines are skipped entirely (remaining visible lines compact upward into
sequential slots), not left as a blank gap.

### Conditions Grammar

| Form | Matches when |
|---|---|
| `perm:<node>` | The player has that permission |
| `world:<name>` | The player's current dimension path equals `<name>` (e.g. `overworld`, `the_nether`, `the_end`) |
| `{placeholder} == value` | The resolved placeholder equals `value` (case-insensitive) |
| `{placeholder} != value` | The resolved placeholder does not equal `value` |
| anything else | Resolved as a placeholder and treated as boolean (`"false"`/empty = false) |

This is deliberately a small comparison grammar, not a scripting engine — it covers
permission gates, world checks, and placeholder-threshold checks, which covers the vast
majority of real use cases without the security/maintenance surface of an embedded
expression language.

A board's `conditions` list is implicit AND. There's no OR — approximate it with two boards
at adjacent priorities if you need it.

---

## Overrides

Same priority order as [Tablist](TablistSystem#priority): **per-player > per-group > board default.**

```jsonc
"groups": {
  "vip": {
    "titleOverride": "&6&lVIP SERVER",
    "lineOverrides": { "2": "&6Balance: &f${balance} &6[VIP]" }
  }
},
"players": {
  "<uuid>": {
    "titleOverride": "&dWelcome back!"
  }
}
```

`lineOverrides` keys are 0-based indexes into the resolved board's `lines` array. An override
replaces the line's text but doesn't change its `condition` — a conditionally-hidden line
stays hidden even with an override text set.

---

## Placeholders

Shorthand tokens (matching tablist's convention):

| Placeholder | Description |
|---|---|
| `{player}` `{displayname}` | Viewing player's name |
| `{online}` `{max}` | Online count / max slots |
| `{ping}` | Viewing player's ping |
| `{world}` | Current dimension path |
| `{balance}` | Economy balance |
| `{prefix}` `{suffix}` `{group}` | Permission group data |
| `{x}` `{y}` `{z}` | Block coordinates |
| `{server_name}` | Server MOTD |
| `{time}` | Server time (`HH:mm`) |

Plus `{animation:NAME}` (from `animations.json`, shared with the [Tablist System](TablistSystem)),
the full `{neoessentials_*}` registered placeholder set, and
`{leaderboard_<board>:<rank>:name|value}` — see [Leaderboard System](LeaderboardSystem).

---

## Commands

| Command | Permission | Description |
|---|---|---|
| `/scoreboard` | — | Show help |
| `/scoreboard toggle` | `neoessentials.scoreboard.toggle` | Show/hide your own sidebar (persisted) |
| `/scoreboard reload` | `neoessentials.scoreboard.admin` | Reload `scoreboard.json` and push to all players |
| `/scoreboard enable` / `disable` | admin | Enable/disable the whole system |
| `/scoreboard info` | admin | Status summary |
| `/scoreboard preview` | admin | Re-send your own board now |
| `/scoreboard board list` | admin | List configured boards |
| `/scoreboard set title <board> <text>` | admin | Replace a board's title (single static frame) |
| `/scoreboard set line <board> <index> <text>` | admin | Replace one line's text (0-based index) |
| `/scoreboard player <player> title\|line\|reset` | admin | Per-player overrides |
| `/scoreboard group <group> title\|line\|reset` | admin | Per-group overrides |

> Runtime overrides via `/scoreboard player`/`/scoreboard group` are **not persisted** — add
> them to `scoreboard.json` manually to survive `/scoreboard reload`.

---

## Toggle Persistence

`/scoreboard toggle` is backed by its own `DataStore` collection (`scoreboard_toggles`, one
record per player, written immediately on change) — the same single-collection,
write-immediately pattern used by `PayToggleManager` for `/paytoggle`. A player's preference
defaults to *on* and survives server restarts.

---

## Disabling Completely

Three independent layers, from most to least aggressive:

1. **`modules.scoreboardEnabled: false` in `config.json`** (restart required) — the event
   handlers driving tick/join/quit never fire at all, so no scoreboard packet is ever sent.
   Same restart-required module-toggle pattern as every other subsystem.
2. **`/scoreboard disable`** (live, no restart) — immediately tears down the sidebar for
   every currently-online player: clears the display slot, resets every visible line's score,
   removes the per-line teams, and removes the objective itself (not just hides it).
3. **`scoreboard.json`'s `"enabled": false` + `/scoreboard reload`** — same effect as #2.

---

## Technical Notes

- **Fake score-holder entries.** Each of the 15 possible line slots uses a fixed invisible
  string (`§0`–`§e`) as its score-holder name — these aren't real players, just unique
  client-side row identifiers. The actual line text lives entirely in that slot's team prefix.
- **Anti-flicker.** Packets are only sent when the player's resolved title/lines actually
  changed since the last send — same dirty-check philosophy as the Tablist System's team
  prefix caching.
- **Score ordering.** Vanilla's client sorts sidebar rows by descending score value; each
  slot is given a fixed score (`15 − slotIndex`) so line order stays stable regardless of
  content, with the number itself hidden via a blank `NumberFormat`.

---

## Web Dashboard API

`/api/scoreboard` (auth required for reads, `auth-admin` for writes):

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/scoreboard` | Full config overview |
| `GET` | `/api/scoreboard/boards/{name}` | Single board detail |
| `PUT` | `/api/scoreboard/enabled` | `{ enabled }` |
| `POST` | `/api/scoreboard/boards` | Create/update a board |
| `DELETE` | `/api/scoreboard/boards/{name}` | Delete a board |
| `PUT` | `/api/scoreboard/boards/{name}/line/{index}` | Quick single-line edit |

---

*Back to [Wiki Home](Home)*
