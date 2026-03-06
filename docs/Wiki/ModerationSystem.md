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
| `/banip` | `/banip <player\|ip> [reason]` | `neoessentials.moderation.banip` | Ban a player's IP |
| `/tempbanip` | `/tempbanip <ip> <duration> [reason]` | `neoessentials.moderation.tempban` | Temporarily ban an IP |
| `/unbanip` | `/unbanip <ip>` | `neoessentials.moderation.unbanip` | Unban an IP |
| `/banlist` | `/banlist [page]` | `neoessentials.moderation.banlist` | View active bans |

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
| `/mute` | `/mute <player> [reason]` | `neoessentials.moderation.mute` | Mute a player (indefinite) |
| `/unmute` | `/unmute <player>` | `neoessentials.moderation.unmute` | Unmute a player |
| `/mutelist` | `/mutelist` | `neoessentials.moderation.mutelist` | List muted players |

Muted players cannot chat, send private messages, or send mail.

---

## Jail

Jail teleports the player to a set jail location and blocks movement, interaction, combat, and teleport until released.

### Commands

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/jail` | `/jail <player> <jail> [reason]` | `neoessentials.moderation.jail` | Jail a player indefinitely |
| `/jailfor` | `/jailfor <player> <jail> <duration> [reason]` | `neoessentials.moderation.jail.timed` | Jail for a set duration |
| `/unjail` | `/unjail <player>` | `neoessentials.moderation.unjail` | Release a player from jail |
| `/setjail` | `/setjail <name>` | `neoessentials.moderation.setjail` | Set a jail location at your position |
| `/deljail` | `/deljail <name>` | `neoessentials.moderation.deljail` | Delete a jail location |
| `/jaillist` | `/jaillist` | `neoessentials.moderation.jaillist` | List all jail locations |
| `/jailinfo` | `/jailinfo <name>` | `neoessentials.moderation.jailinfo` | Show jail location info |
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
| `/vanish` | `/vanish [player]` | `neoessentials.moderation.vanish` | Toggle vanish for yourself or another |
| `/v` | alias | same | Alias |
| `/unvanish` | `/unvanish [player]` | `neoessentials.moderation.vanish` | Force-disable vanish |
| `/vanishlist` | `/vanishlist` | `neoessentials.moderation.vanishlist` | List vanished players |

Players with `neoessentials.moderation.seevanished` can see vanished staff in the player list and world.

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

| Key | Default | Description |
|---|---|---|
| `broadcastBans` | `true` | Announce bans to all players |
| `broadcastKicks` | `true` | Announce kicks to all players |
| `logKickActions` | `true` | Log kick details to console |
| `notifyStaffOnKick` | `true` | Notify staff with `neoessentials.moderation.notify` on kick |
| `kickMessage` | `"You have been kicked..."` | Default kick screen message |
| `kickAllMessage` | `"Server maintenance..."` | `/kickall` screen message |

---

*Back to [Wiki Home](Home)*
