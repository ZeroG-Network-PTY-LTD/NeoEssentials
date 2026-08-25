# Moderation System

> **Version:** 1.0.5+build.54 · **Config:** `config.json` → `moderation` / `storage` sections

---

## Overview

Comprehensive player moderation — ban, temp-ban, IP ban, kick, mute (with IP mutes), jail (timed), freeze, vanish, warnings, staff notes, and player reports — all with persistent storage, full history/audit trails, permission integration, and event enforcement.

Bans, mutes, kicks, warnings, notes, and reports are backed by a single canonical store per feature (see [Storage Backend](#storage-backend) below) — there is exactly one code path that enforces each punishment, and the [Web Dashboard](WebDashboard)'s moderation API reads and writes through the same managers, so in-game commands and dashboard actions always agree.

---

## Bans

### Commands

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/ban` | `/ban <player> [reason]` | `neoessentials.moderation.ban` | Permanently ban a player |
| `/tempban` | `/tempban <player> <duration> [reason]` | `neoessentials.moderation.tempban` | Temporarily ban (e.g. `30m`, `2h`, `1d`) |
| `/unban` | `/unban <player>` | `neoessentials.moderation.unban` | Unban a player |
| `/banip` | `/banip <ip> [reason]` | `neoessentials.moderation.banip` | Ban an IP address (takes the IP directly, not a player name) |
| `/tempbanip` | `/tempbanip <ip> <duration> [reason]` | `neoessentials.moderation.tempbanip` | Temporarily ban an IP |
| `/unbanip` | `/unbanip <ip>` | `neoessentials.moderation.unbanip` | Unban an IP |
| `/banlist` | `/banlist [players\|ips]` | `neoessentials.moderation.banlist` | View active bans (defaults to `players`) |

**Duration format:** `30s` · `5m` · `2h` · `1d` · `1w`

### History & Audit Trail

Every ban (player and IP) is stored with a unique ID, an `active` flag, optional `evidence`, and — once lifted — who unbanned it and when (`unbannedBy` / `unbannedAt`). Expired temp-bans are archived the same way (auto-marked inactive, no "unbanned by" staff member). Unbanning and re-banning a player never destroys their prior ban records; they remain queryable as history via the dashboard's `/api/moderation/bans/{uuid}` and `/api/moderation/ipbans` routes (see [Web Dashboard](WebDashboard)).

### Moderation History

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/modhistory` | `/modhistory <player>` | `neoessentials.moderation.history` | View a player's full moderation history — bans, mutes, kicks, and warns — in one summary |
| `/history` | alias for `/modhistory` | same | Alias |

---

## Kicks

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/kick` | `/kick <player> [reason]` | `neoessentials.moderation.kick` | Kick a player |
| `/kickall` | `/kickall [reason]` | `neoessentials.moderation.kickall` | Kick all players |

Every kick (including dashboard-initiated kicks) is recorded with a timestamp, reason, and issuing staff member, and is queryable as history via the dashboard's `/api/moderation/kicks` / `/api/moderation/kicks/{name}` routes — there is no in-game command to view kick history.

---

## Mutes

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/mute` | `/mute <player> [reason]` | `neoessentials.chat.mute` | Mute a player (indefinite) |
| `/silence` | alias for `/mute` | same | Alias |
| `/unmute` | `/unmute <player>` | `neoessentials.chat.mute` | Unmute a player |
| `/mutelist` | `/mutelist` | `neoessentials.chat.mute` | List muted players |

Muted players cannot chat, send private messages, or send mail. The mute system is implemented in the **chat** module (`com.zerog.neoessentials.chat`), not the moderation module — all three commands share the single `neoessentials.chat.mute` permission (there's no separate exempt/list/unmute node), plus `neoessentials.chat.mute.exempt` to make a player un-mutable.

> ⚠️ Unlike every other moderation command, `/mute`/`/unmute`/`/mutelist` have **no root
> permission gate at registration** — `neoessentials.chat.mute` is only checked deep inside the
> command body, not at the point Brigadier registers the command. This means all three are
> visible to every player in `/help` and can be *attempted* by anyone, though the body-level
> check should still reject an unauthorized caller — worth verifying directly if you rely on
> this restriction, rather than assuming registration-time enforcement like most other commands.

Like bans, every mute tracks reason, issuing staff member, an `active` flag, and — once lifted — `unmutedBy` / `unmutedAt`, with full history preserved. **IP mutes** are also supported (mute an IP directly rather than a player name) — currently exposed via the dashboard's `/api/moderation/ipmute` / `/api/moderation/ipmutes` routes rather than an in-game command.

---

## Jail

Jail teleports the player to a set jail location and blocks movement, interaction, combat, and teleport until released.

### Commands

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/jail` | `/jail <player> <jail> [reason]` | `neoessentials.moderation.jail` | Jail a player indefinitely |
| `/jailfor` | `/jailfor <player> <jail> <duration> [reason]` | `neoessentials.moderation.jail` | Jail for a set duration (same permission as `/jail`) |
| `/unjail` | `/unjail <player>` | `neoessentials.moderation.unjail` | Release a player from jail |
| `/setjail` | `/setjail <name>` (auto-detect shape) · `/setjail <name> sphere <radius>` · `/setjail <name> cuboid` | `neoessentials.moderation.setjail` | Create a jail location — see [Jail Region Shapes](#jail-region-shapes) below |
| `/deljail` | `/deljail <name>` | `neoessentials.moderation.setjail` | Delete a jail location (same permission as `/setjail`) |
| `/jailwand` | `/jailwand` | `neoessentials.jail.wand` | Give yourself the jail-region selection wand |
| `/jaillist` | `/jaillist` | `neoessentials.moderation.jaillist` | List all jail locations |
| `/jailinfo` | `/jailinfo [name]` | `neoessentials.moderation.jailinfo` | Show info for one jail, or all jails if no name given |
| `/jails` | alias for `/jaillist` | same | Alias |
| `/togglejail` | `/togglejail <player>` | `neoessentials.moderation.jail` | Toggle jail on/off for a player |

### Jail Region Shapes

A jail location can be either a **sphere** (radius around a point) or a **cuboid** (two-corner box):

- `/setjail <name>` — auto-detects: uses a wand or WorldEdit cuboid selection if one is active, otherwise falls back to a sphere at your current position using `moderation.jailSettings.defaultSphereRadius`.
- `/setjail <name> sphere <radius>` — explicit sphere at your current position.
- `/setjail <name> cuboid` — cuboid from the current wand/WorldEdit selection.
- `/jailwand` gives an item (configurable via `moderation.jailSettings.wandItem`) — right-click sets corner 1, left-click sets corner 2, then run `/setjail <name> cuboid`.

### Jail Enforcement

While jailed, the following are blocked:
- Movement outside jail radius
- Teleport commands (redirected back to jail on respawn too)
- Breaking/placing blocks (unless `neoessentials.jail.allow-break` / `allow-place`)
- Interactions (unless `neoessentials.jail.allow-interact`)
- Attacking entities (unless `neoessentials.jail.allow-attack`)

Timed jails auto-release when the duration expires (checked every second and on login).

---

## Freeze

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/freeze` | `/freeze <player> [reason]` | `neoessentials.moderation.freeze` | Freeze a player in place |
| `/unfreeze` | `/unfreeze <player>` | `neoessentials.moderation.unfreeze` | Unfreeze a player |
| `/freezeall` | `/freezeall` | `neoessentials.moderation.freezeall` | Freeze all online players |
| `/unfreezeall` | `/unfreezeall` | `neoessentials.moderation.unfreezeall` | Unfreeze all players |
| `/freezelist` | `/freezelist` | `neoessentials.moderation.freezelist` | List frozen players |

---

## Vanish

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/vanish` | `/vanish [player]` | `neoessentials.moderation.vanish` (self) + `neoessentials.moderation.vanish.others` (targeting another player) | Toggle vanish for yourself or another |
| `/v` | alias | same | Alias |
| `/unvanish` | `/unvanish [player]` | same as above | Force-disable vanish |
| `/vanishlist` | `/vanishlist` | `neoessentials.moderation.vanishlist` | List vanished players |

Broadcasting vanish toggles to staff/everyone and console logging are all config-driven (`moderation.vanishSettings.broadcastToStaffVanish`, etc. — see below).

---

## Warnings

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/warn` | `/warn <player> [reason]` | `neoessentials.moderation.warn` | Issue a warning to a player |
| `/warnings` | `/warnings <player>` | `neoessentials.moderation.warnings` | View a player's warnings and total count |
| `/clearwarnings` | `/clearwarnings <player>` | `neoessentials.moderation.warn` | Clear all warnings for a player |
| `/removewarn` | `/removewarn <player> <warnId>` | `neoessentials.moderation.warn` | Remove a single warning by its ID (accepts the short 8-char ID prefix) |

Warnings persist for offline players and are shown with a truncated ID, timestamp, reason, and issuing staff member.

---

## Staff Notes

Freeform notes staff can leave on a player's record — for context that isn't a punishment (e.g. "watch this player, borderline behavior last week").

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/note` | `/note <player> <text>` | `neoessentials.moderation.note` | Add a note to a player's record |
| `/notes` | `/notes <player>` | `neoessentials.moderation.notes` | View a player's notes (paginated, 5 per page) |
| `/removenote` | `/removenote <player> <noteId>` | `neoessentials.moderation.note` | Remove a note by ID (accepts the short 8-char prefix) |

Notes persist for offline players and record author, timestamp, and text. Also available via the dashboard's `/api/moderation/notes/{name}` and `/api/moderation/note` routes.

---

## Player Reports

Lets players flag misbehavior even while no staff are online — reports persist and show up in the review queue whenever staff next check.

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/report` | `/report <player> <reason>` | `neoessentials.moderation.report` | Report a player (default: **granted to everyone**) |
| `/reports` | `/reports` | `neoessentials.moderation.reports` | Staff: view the pending review queue (8 per page) |
| `/reviewreport` | `/reviewreport <id> <accept\|dismiss> [notes]` | `neoessentials.moderation.reports` | Staff: resolve a report, optionally with review notes |

Submitting a report also immediately notifies any online staff with `neoessentials.moderation.reports`, in addition to persisting for later review. Reports track reporter, target, reason, timestamp, and status (`PENDING` / `REVIEWED` / `DISMISSED`) plus who reviewed it and when. Also available via the dashboard's `/api/moderation/reports`, `/api/moderation/reports/all`, `/api/moderation/reports/{id}`, and `/api/moderation/reports/{id}/review` routes.

---

## Data Files & Storage Backend

Bans, mutes (including IP mutes), kicks, warnings, notes, and reports are persisted through a pluggable **DataStore** abstraction rather than bespoke JSON files — configured under `config.json` → `storage`:

```json
"storage": {
  "type": "json",
  "autoMigrate": true,
  "sqlite": { "file": "data.db" },
  "mysql": { "host": "localhost", "port": 3306, "database": "neoessentials", "username": "neoessentials", "password": "", "useSSL": false, "poolSize": 10 }
}
```

| Key | Default | Description |
|---|---|---|
| `type` | `"json"` | Backend to use: `json`, `yaml`, `sqlite`, or `mysql` |
| `autoMigrate` | `true` | On first boot with an empty store, import each manager's existing legacy JSON data automatically and losslessly |
| `sqlite.file` | `"data.db"` | SQLite database filename (under `neoessentials/store/`) |
| `mysql.*` | — | Connection details for a shared MySQL database — point every server in a network at the same database to share bans/mutes/etc. in real time |

Each punishment type is one **collection** (a schema-less table of JSON documents keyed by ID):

| Collection | Contents |
|---|---|
| `player_bans` | Player bans — active and historical, with unban audit trail |
| `ip_bans` | IP bans — active and historical, with unban audit trail |
| `mutes` | Player mutes — active and historical, with unmute audit trail |
| `ip_mutes` | IP mutes — active and historical |
| `kicks` | Kick history |
| `warns` | Warnings |
| `notes` | Staff notes |
| `reports` | Player reports |
| `jails` / `jail_locations` | Active jail state / named jail locations |
| `freezes` / `vanishes` | Frozen player state / vanish state |

With the default `json` backend, collections live at `neoessentials/store/<collection>.json`. With `yaml`, `neoessentials/store/<collection>.yml`. With `sqlite`, all collections live as tables in `neoessentials/store/data.db`. With `mysql`, all collections live as tables in the configured database. If MySQL is unreachable at boot, the mod automatically falls back to the JSON backend rather than failing to start.

> As of this release, `storage.type` covers the **entire mod**, not just moderation — economy, homes/warps/spawn, kits, chat (AFK/ignore/chat-format), holograms, shops, permissions (including group inheritance), the dashboard's own accounts, and the Auction House are all backed by the same DataStore system now. See [Storage Backend](Storage) for the complete collection list and per-system breakdown.

---

## Config (`config.json` → `moderation`)

Settings are nested under per-feature sub-objects, not flat keys directly under `moderation`.

### `moderation.banSettings`

| Key | Default | Description |
|---|---|---|
| `broadcastBans` | `false` | Announce bans to all players |
| `logBanActions` | — | Log ban details to console |
| `defaultBanReason` | *(built-in default)* | Reason used when none is given |
| `maxBanReason` | — | Max reason length |
| `enableIPBans` | — | Enable/disable IP bans |
| `enablePermanentBans` | — | Enable/disable permanent (non-timed) bans |
| `enableTempBans` | — | Enable/disable temp bans |
| `autoExpireTempBans` | — | Automatically lift temp bans when they expire |
| `checkExpiredBansInterval` | — | How often (seconds) to check for expired temp bans |
| `banMessageFormat` / `tempBanMessageFormat` / `ipBanMessageFormat` | — | Disconnect screen message templates |

### `moderation.kickSettings`

| Key | Default | Description |
|---|---|---|
| `enableKickSystem` | `true` | Enable/disable the kick system |
| `broadcastKicks` | `false` | Announce kicks to all players |
| `logKickActions` | `true` | Log kick details to console |
| `notifyStaffOnKick` | `true` | Notify staff (players with `neoessentials.moderation.notifications`) on kick, independent of `broadcastKicks` |
| `kickMessage` | `"You have been kicked from the server.\nReason: {reason}\nKicked by: {kicker}"` | Default kick screen message |
| `kickAllMessage` | `"Server maintenance in progress. Please reconnect in a few minutes."` | `/kickall` screen message |
| `defaultKickReason` | `"Kicked by an operator"` | Reason used when none is given |
| `maxKickReason` | `500` | Max reason length |

### `moderation.freezeSettings`

| Key | Default | Description |
|---|---|---|
| `enableFreezeSystem` | `true` | Enable/disable the freeze system |
| `defaultFreezeReason` | `"Frozen by an operator"` | Reason used when none is given |
| `maxFreezeReason` | `500` | Max reason length |
| `freezeMessage` / `unfreezeMessage` | — | Message templates sent to the frozen/unfrozen player (`{reason}`, `{freezer}` / `{unfreezer}`) |
| `freezeReminder` | — | Periodic reminder message shown while frozen |
| `freezeReminderInterval` | `30` | Seconds between freeze reminders |
| `logFreezeActions` | `true` | Log freeze/unfreeze to console |
| `freezeOnLogin` | `true` | Re-apply freeze if a frozen player logs back in |
| `preventCommands` | `true` | Block command use while frozen |
| `allowedCommands` | `[]` | Commands still allowed while frozen |

### `moderation.vanishSettings`

| Key | Default | Description |
|---|---|---|
| `enableVanishSystem` | — | Enable/disable the vanish system |
| `preventInteraction` | `true` | Block interactions while vanished |
| `broadcastToStaffVanish` | `false` | Announce vanish toggles to staff |
| `BroadcastToAllVanish` | — | Announce vanish toggles to all players (note the config's mixed-case key) |
| `hideFromTabList` | — | Hide vanished players from the tab list |
| `vanishOnJoin` | — | Re-apply vanish if a vanished player logs back in |
| `logVanishActions` | — | Log vanish/unvanish to console |

### `moderation.jailSettings`

| Key | Default | Description |
|---|---|---|
| `defaultJailReason` | — | Reason used for `/jail` when none is given |
| `logJailActions` | — | Log jail/unjail to console |
| `preventJailEscape` | — | Block movement out of the jail region |
| `jailMessageFormat` | — | Message template shown to the jailed player |
| `wandItem` | — | Item ID used as the jail-region selection wand (given by `/jailwand`) |
| `defaultSphereRadius` | — | Radius used by `/setjail <name>` auto-detect and `/setjail <name> sphere` when no explicit radius is given |
| `maxJailsBeforeTempBan` | — | Number of times a player can be jailed before an automatic temp-ban escalation |

---

## Notes & Reports Permissions

| Node | Default | Description |
|---|---|---|
| `neoessentials.moderation.note` | 🔒 | Add/remove staff notes (`/note`, `/removenote`) |
| `neoessentials.moderation.notes` | 🔒 | View a player's staff notes (`/notes`) |
| `neoessentials.moderation.report` | ✅ granted | Submit a player report (`/report`) |
| `neoessentials.moderation.reports` | 🔒 | View and review the report queue (`/reports`, `/reviewreport`) |

---

*Back to [Wiki Home](Home)*
