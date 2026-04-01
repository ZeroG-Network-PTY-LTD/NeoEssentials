# Utility Systems

> **Version:** 1.0.2.6

---

## Overview

Miscellaneous quality-of-life commands covering player info, server admin tools, world/environment manipulation, fun commands, and player state management.

---

## Player Info Commands

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/seen` | `/seen <player>` | `neoessentials.seen` | Show online/offline status with location or last-seen time |
| `/near` | `/near [radius]` | `neoessentials.near` | List nearby players and their distance (default 200 blocks) |
| `/ping` | `/ping [player]` | `neoessentials.ping` | Show connection latency (colour-coded) |
| `/playtime` | `/playtime [player]` | `neoessentials.playtime` | Show total play time in h/m/s |
| `/whois` | `/whois <player>` | `neoessentials.whois` | Show UUID, dimension, coords, gamemode, ping, health, food |
| `/realname` | `/realname <nick>` | `neoessentials.realname` | Look up real username from nickname |
| `/list` | `/list` | `neoessentials.list` | List online players with count |
| `/who` | alias | same | Alias |
| `/motd` | `/motd` | `neoessentials.motd` | Display the active server MOTD |

---

## MOTD System

The MOTD (Message of the Day) system supports **multiple named profiles**, **auto-rotation**, and **web-dashboard management**.

### Commands

| Command | Permission | Description |
|---|---|---|
| `/motd` | `neoessentials.motd` | Show the active MOTD |
| `/motd set <message>` | `neoessentials.motd.set` | Set the active profile's MOTD text |
| `/motd clear` | `neoessentials.motd.set` | Clear the active profile's MOTD |
| `/motd reload` | `neoessentials.motd.reload` | Reload all profiles from disk |
| `/motd broadcast` | `neoessentials.motd.broadcast` | Broadcast active MOTD to all online players |
| `/motd profile list` | `neoessentials.motd.profile` | List all profiles and the active one |
| `/motd profile create <name> <message>` | `neoessentials.motd.profile` | Create or overwrite a named profile |
| `/motd profile delete <name>` | `neoessentials.motd.profile` | Delete a profile (at least one must remain) |
| `/motd profile switch <name>` | `neoessentials.motd.profile` | Switch the active profile |
| `/motd profile info [name]` | `neoessentials.motd.profile` | Show details for a profile (defaults to active) |
| `/motd rotation enable <minutes>` | `neoessentials.motd.rotation` | Enable auto-rotation every N minutes |
| `/motd rotation disable` | `neoessentials.motd.rotation` | Disable auto-rotation |
| `/motd rotation next` | `neoessentials.motd.rotation` | Rotate to the next profile immediately |

### Color codes

Use `&` color codes in MOTD messages (e.g. `&aGreen text &cRed text`). They are converted to `§` automatically.

### Profiles

Profiles are stored in `config/neoessentials/motd_data.json`. Example:

```json
{
  "activeProfile": "default",
  "rotation": { "enabled": false, "intervalMinutes": 60, "currentIndex": 0 },
  "profiles": {
    "default": { "motd": "§aWelcome to the server!", "author": "Admin", "timestamp": "01/01/2026 12:00" },
    "event":   { "motd": "§6Special event is running!", "author": "Admin", "timestamp": "01/01/2026 12:00" }
  }
}
```

### Dashboard API

The dashboard exposes `/api/motd` for full profile management:

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/motd` | Get all profiles + rotation settings |
| `GET` | `/api/motd/active` | Get the active profile only |
| `POST` | `/api/motd/profiles` | Create/update a profile `{name, motd, author?}` |
| `DELETE` | `/api/motd/profiles/{name}` | Delete a profile |
| `PUT` | `/api/motd/active` | Switch active profile `{name}` |
| `PUT` | `/api/motd/rotation` | Update rotation `{enabled, intervalMinutes}` |
| `POST` | `/api/motd/rotation/next` | Rotate to next profile immediately |
| `POST` | `/api/motd/broadcast` | Broadcast active MOTD to online players |
| `/rules` | `/rules` | `neoessentials.rules` | Display server rules |
| `/helpop` | `/helpop <message>` | `neoessentials.helpop` | Send message to online staff |
| `/suicide` | `/suicide` | `neoessentials.suicide` | Kill yourself |

---

## Nicknames

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/nick` | `/nick <name\|off> [player]` | `neoessentials.nick` | Set a nickname |
| `/nickname` | alias | same | Alias |

Colour codes in nicks require `neoessentials.nick.color`. Setting others' nicks requires `neoessentials.nick.others`.

---

## Player State Commands

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/fly` | `/fly [on\|off] [player]` | `neoessentials.fly` | Toggle flight |
| `/god` | `/god [on\|off] [player]` | `neoessentials.god` | Toggle god mode |
| `/heal` | `/heal [player]` | `neoessentials.heal` | Full health, hunger, and saturation |
| `/feed` | `/feed [player]` | `neoessentials.feed` | Full hunger and saturation |
| `/speed` | `/speed [walk\|fly] <0-10> [player]` | `neoessentials.speed` | Set walk or fly speed |
| `/ext` | `/ext [player]` | `neoessentials.ext` | Extinguish fire |
| `/extinguish` | alias | same | Alias |
| `/burn` | `/burn <player> [seconds]` | `neoessentials.burn` | Set fire ticks on a player |
| `/give` | `/give <player> <item> [amount]` | `neoessentials.give` | Give items to a player |
| `/more` | `/more [amount]` | `neoessentials.more` | Fill held stack to max |
| `/hat` | `/hat` | `neoessentials.hat` | Wear held item as helmet |
| `/exp` | `/exp [show\|set\|give] [amount] [player]` | `neoessentials.exp` | Show, set, or give XP |
| `/gamemode` | `/gamemode <mode\|0-3> [player]` | `neoessentials.gamemode` | Change gamemode |
| `/gms`, `/gmc`, `/gma`, `/gmsp` | shortcut | same | Gamemode shortcuts |

---

## Per-Player Time & Weather

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/ptime` | `/ptime [reset\|day\|noon\|night\|<ticks>] [player]` | `neoessentials.ptime` | Set client-side time (server time unaffected) |
| `/pweather` | `/pweather [reset\|sun\|storm\|clear\|rain] [player]` | `neoessentials.pweather` | Set client-side weather |

---

## Server Admin Commands

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/broadcast` | `/broadcast <message>` | `neoessentials.broadcast` | Server-wide announcement |
| `/bc`, `/announce` | aliases | same | Aliases |
| `/broadcastworld` | `/broadcastworld <message>` | `neoessentials.broadcastworld` | Broadcast to current world only |
| `/bcastworld` | alias | same | Alias |
| `/time` | `/time [set\|add] <value\|day\|night…>` | `neoessentials.time` | Get/set server time |
| `/day`, `/night` | shortcuts | same | Shortcuts |
| `/weather` | `/weather <sun\|storm\|thunder> [dur]` | `neoessentials.weather` | Set server weather |
| `/sun`, `/storm`, `/thunder` | shortcuts | same | Shortcuts |
| `/sudo` | `/sudo <player> <command>` | `neoessentials.sudo` | Run a command as another player |
| `/gc` | `/gc` | `neoessentials.gc` | Show TPS, memory, uptime, chunks |
| `/mem` | alias | same | Alias |
| `/backup` | `/backup` | `neoessentials.backup` | Trigger a server backup |
| `/kill` | `/kill <player>` | `neoessentials.kill` | Kill a player |
| `/spawner` | `/spawner <mob>` | `neoessentials.spawner` | Set spawner type at looked block |
| `/spawnmob` | `/spawnmob <mob> [amount] [player]` | `neoessentials.spawnmob` | Spawn entities at a player |
| `/mob` | alias | same | Alias |
| `/effect` | `/effect <player> <effect\|clear> [dur] [amp]` | `neoessentials.effect` | Apply/clear potion effects |
| `/unlimited` | `/unlimited [list\|clear\|<item\|hand>] [player]` | `neoessentials.unlimited` | Unlimited item mode (never depleted) |
| `/recipe` | `/recipe [item]` | `neoessentials.recipe` | Unlock crafting recipes for an item |

---

## World Interaction

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/fireball` | `/fireball [type] [speed] [ride]` | `neoessentials.fireball.<type>` | Shoot a projectile (11 types) |
| `/tree` | `/tree <type>` | `neoessentials.tree` | Grow a tree at your feet |
| `/bigtree` | `/bigtree` | `neoessentials.tree` | Grow a big tree |
| `/break` | `/break` | `neoessentials.break` | Instantly break looked-at block |
| `/ice` | `/ice [player]` | `neoessentials.ice` | Fully freeze a player (powder snow mechanic) |
| `/lightning` | `/lightning [player]` | `neoessentials.lightning` | Strike lightning |
| `/smite` | alias | same | Alias |
| `/remove` | `/remove <type> [radius]` | `neoessentials.remove` | Remove entities by type in radius |
| `/nuke` | `/nuke` | `neoessentials.nuke` | Remove all nearby entities |
| `/tptoggle` | `/tptoggle [on\|off]` | `neoessentials.tptoggle` | Toggle receiving TP requests |
| `/msgtoggle` | `/msgtoggle [on\|off]` | `neoessentials.msgtoggle` | Toggle receiving private messages |
| `/rtoggle` | `/rtoggle [on\|off]` | `neoessentials.rtoggle` | Toggle receiving `/reply` messages |

---

## Fun Commands

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/me` | `/me <action>` | `neoessentials.me` | Broadcast a third-person action message |
| `/firework` | `/firework` | `neoessentials.firework` | Launch a firework |
| `/antioch` | `/antioch` | `neoessentials.antioch` | Launch a Holy Hand Grenade 🐇 |
| `/kittycannon` | `/kittycannon` | `neoessentials.kittycannon` | Launch a kitten |
| `/beezooka` | `/beezooka` | `neoessentials.beezooka` | Launch bees |
| `/rest` | `/rest` | `neoessentials.rest` | Skip the night (vote) |
| `/info` | `/info` | `neoessentials.info` | Show server/mod info |
| `/itemdb` | `/itemdb [item]` | `neoessentials.itemdb` | Show registry info for held/named item |

---

## Mail

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/mail read` | `/mail read [page]` | `neoessentials.mail` | Read your mail |
| `/mail send` | `/mail send <player> <message>` | `neoessentials.mail.send` | Send mail |
| `/mail sendtemp` | `/mail sendtemp <player> <duration> <message>` | `neoessentials.mail.sendtemp` | Send expiring mail |
| `/mail sendall` | `/mail sendall <message>` | `neoessentials.mail.sendall` | Broadcast mail to all players |
| `/mail clear` | `/mail clear [index]` | `neoessentials.mail` | Clear your mailbox or specific message |

---

*Back to [Wiki Home](Home)*
