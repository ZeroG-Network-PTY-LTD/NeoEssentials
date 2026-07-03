# Moderation System

> **Version:** 1.0.2.6 · **Config:** `config.json` → `moderation` section

---

## Overview

Comprehensive player moderation — ban, temp-ban, IP ban, kick, mute, jail (timed), freeze, and vanish — all with persistent storage, permission integration, and event enforcement.

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

---

## Kicks

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/kick` | `/kick <player> [reason]` | `neoessentials.moderation.kick` | Kick a player |
| `/kickall` | `/kickall [reason]` | `neoessentials.moderation.kickall` | Kick all players |

---

## Mutes

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/mute` | `/mute <player> [reason]` | `neoessentials.chat.mute` | Mute a player (indefinite) |
| `/silence` | alias for `/mute` | same | Alias |
| `/unmute` | `/unmute <player>` | `neoessentials.chat.mute` | Unmute a player |
| `/mutelist` | `/mutelist` | `neoessentials.chat.mute` | List muted players |

Muted players cannot chat, send private messages, or send mail. The mute system is implemented in the **chat** module (`com.zerog.neoessentials.chat`), not the moderation module — all three commands share the single `neoessentials.chat.mute` permission (there's no separate exempt/list/unmute node), plus `neoessentials.chat.mute.exempt` to make a player un-mutable.

---

## Jail

Jail teleports the player to a set jail location and blocks movement, interaction, combat, and teleport until released.

### Commands

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/jail` | `/jail <player> <jail> [reason]` | `neoessentials.moderation.jail` | Jail a player indefinitely |
| `/jailfor` | `/jailfor <player> <jail> <duration> [reason]` | `neoessentials.moderation.jail` | Jail for a set duration (same permission as `/jail`) |
| `/unjail` | `/unjail <player>` | `neoessentials.moderation.unjail` | Release a player from jail |
| `/setjail` | `/setjail <name>` | `neoessentials.moderation.setjail` | Set a jail location at your position |
| `/deljail` | `/deljail <name>` | `neoessentials.moderation.setjail` | Delete a jail location (same permission as `/setjail`) |
| `/jaillist` | `/jaillist` | `neoessentials.moderation.jaillist` | List all jail locations |
| `/jailinfo` | `/jailinfo [name]` | `neoessentials.moderation.jailinfo` | Show info for one jail, or all jails if no name given |
| `/jails` | alias for `/jaillist` | same | Alias |
| `/togglejail` | `/togglejail <player>` | `neoessentials.moderation.jail` | Toggle jail on/off for a player |

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

## Data Files

| File | Contents |
|---|---|
| `neoessentials/bans.json` | Active bans and IP bans |
| `neoessentials/muted_players.json` | Active mutes |
| `neoessentials/jailed_players.json` | Active jail entries (with expiry for timed jails) |
| `neoessentials/jail_locations.json` | Named jail spawn points |
| `neoessentials/frozen_players.json` | Frozen player state |
| `neoessentials/vanished_players.json` | Persistent vanish state |

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
| `preventInteraction` | `true` | Block interactions while vanished |
| `broadcastToStaffVanish` | `false` | Announce vanish toggles to staff |
| (also: broadcast-to-all and log-vanish-actions toggles) | | |

### `moderation.jailSettings`

| Key | Description |
|---|---|
| `defaultJailReason` | Reason used for `/jail` when none is given |

---

*Back to [Wiki Home](Home)*
