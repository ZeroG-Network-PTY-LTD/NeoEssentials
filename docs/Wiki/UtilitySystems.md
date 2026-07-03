# Utility Systems

> **Version:** 1.0.2.6+build.26

---

## Table of Contents

1. [Player Info Commands](#player-info-commands)
2. [Nicknames](#nicknames)
3. [MOTD System](#motd-system)
4. [Rules System](#rules-system)
5. [Helpop](#helpop)
6. [Suicide](#suicide)
7. [Depth](#depth)
8. [Player State Commands](#player-state-commands)
9. [Per-Player Time & Weather](#per-player-time--weather)
10. [Server Admin Commands](#server-admin-commands)
11. [World Interaction](#world-interaction)
12. [Fun Commands](#fun-commands)
13. [Mail](#mail)

---

## Overview

Miscellaneous quality-of-life commands covering player info, server admin tools, world/environment manipulation, fun commands, and player state management.

---

## Player Info Commands

| Command | Aliases | Permission | Description |
|---|---|---|---|
| `/seen <player>` | — | `neoessentials.seen` | Show online/offline status with location or last-seen time |
| `/near [radius]` | `/nearby` | `neoessentials.near` | List nearby players and their distance (default 100 blocks, max 500) |
| `/ping [player]` | `/pong` | `neoessentials.ping` | Show own latency (colour-coded). Checking others requires `neoessentials.ping.others` |
| `/playtime [player]` | — | `neoessentials.playtime` | Show total play time in h/m/s. Others requires `neoessentials.playtime.others` |
| `/whois <player>` | — | `neoessentials.whois` | Show UUID, dimension, coords, gamemode, ping, health, food. Detailed view requires `neoessentials.whois.detailed` |
| `/realname <nick>` | — | `neoessentials.realname` | Look up real username from nickname |
| `/list` | `/who`, `/online` | `neoessentials.list` | List online players with count |
| `/motd` | — | `neoessentials.motd` | Display the active server MOTD |

> **Permissions note:** `neoessentials.whois` is an **admin-only** node (`default: false`). Use `neoessentials.whois.detailed` for detailed diagnostics (IPs, internal flags). `neoessentials.ping.others` is granted to all players by default.

---

## Nicknames

### Commands

| Command | Permission | Description |
|---|---|---|
| `/nick` | `neoessentials.nick` | Show your current nickname |
| `/nick <name>` | `neoessentials.nick` | Set your nickname |
| `/nick reset` | `neoessentials.nick` | Remove your nickname |
| `/nick off` | `neoessentials.nick` | Alias for `/nick reset` |
| `/nickname …` | `neoessentials.nick` | Full alias — redirects to `/nick` |
| `/setnick <player> <name>` | `neoessentials.nick.others` | Set another player's nickname (admin) |
| `/setnick <player> reset` | `neoessentials.nick.others` | Remove another player's nickname (admin) |

### Permission nodes

| Node | Default | Description |
|---|---|---|
| `neoessentials.nick` | `true` | Set / clear own nickname |
| `neoessentials.nick.color` | `false` | Use `&`-colour codes in nickname |
| `neoessentials.nick.others` | `false` | Set / clear another player's nickname |

### Nickname rules

- **Length:** 3–16 characters (after stripping colour codes).
- **Characters:** `a-z A-Z 0-9 _ & # §` only.
- **Uniqueness:** case-insensitive; two players cannot share the same plain-text nick.
- **Colour codes:** `&0–9`, `&a–f`, `&k–r`, and hex `&#RRGGBB` are all supported when the player has `neoessentials.nick.color`.

### Storage

Nicknames are persisted in `config/neoessentials/nickname_data.json` (UUID → raw nick string with `&` codes). They are loaded on server start and re-applied to all online players.

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

---

## Rules System

The Rules system stores server rules in `config/neoessentials/rules_data.json`.  
Rules are **auto-generated with sensible defaults** on first startup if no file exists.

### Commands

| Command | Permission | Description |
|---|---|---|
| `/rules` | `neoessentials.rules` | Display rules (page 1) |
| `/rules <page>` | `neoessentials.rules` | Show a specific page |
| `/rules add <text>` | `neoessentials.rules.admin` | Append a new rule |
| `/rules remove <n>` | `neoessentials.rules.admin` | Remove rule number *n* |
| `/rules edit <n> <text>` | `neoessentials.rules.admin` | Replace rule number *n* |
| `/rules insert <n> <text>` | `neoessentials.rules.admin` | Insert rule before position *n* |
| `/rules clear` | `neoessentials.rules.admin` | Remove all rules |
| `/rules reload` | `neoessentials.rules.admin` | Reload rules from disk without a server restart |

### Color codes

Use `&` color codes in rule text (e.g. `&cNo griefing`). They are converted to `§` automatically when displayed.

### Data file

Rules are stored in `config/neoessentials/rules_data.json`:

```json
{
  "rules": [
    "&6Be respectful to all players and staff members",
    "&cNo griefing, stealing, or destroying other players' builds",
    "&eNo spamming in chat or using excessive caps"
  ]
}
```

**Location:** `config/neoessentials/rules_data.json`  
**Generated automatically:** Yes — 10 default rules are written on first start if the file is absent.  
**Edit in-game:** Use `/rules add`, `/rules edit`, `/rules remove`.  
**Edit on disk:** Modify `rules_data.json` directly, then run `/rules reload` in-game.

> **Legacy migration:** Servers upgrading from NeoEssentials <1.0.2.6 may have `rules.json`.  
> NeoEssentials automatically detects and migrates it to `rules_data.json` on first load — no manual action needed.

### Console feedback

NeoEssentials logs detailed guidance to the console when rules fail to load:

```
╔══════════════════════════════════════════════════════╗
║  RULES LOAD ERROR: rules_data.json is corrupt JSON  ║
║  File: config/neoessentials/rules_data.json         ║
║  Error: Unexpected character ...                    ║
║  Fix:  Delete the file and run /rules reload to     ║
║        regenerate defaults, then re-add your rules. ║
╚══════════════════════════════════════════════════════╝
```

If the file is simply missing, NeoEssentials creates it automatically and logs its absolute path along with quick-start edit instructions.

### Dashboard API

The dashboard exposes `/api/rules` for full rule management:

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/rules` | List all rules with 1-based index |
| `POST` | `/api/rules` | Replace all rules `{"rules": ["...", ...]}` |
| `POST` | `/api/rules/add` | Append a rule `{"rule": "..."}` |
| `PUT` | `/api/rules/{n}` | Edit rule at position *n* `{"rule": "..."}` |
| `DELETE` | `/api/rules/{n}` | Delete rule at position *n* |
| `POST` | `/api/rules/reload` | Reload rules from disk |

All `/api/rules` endpoints require Bearer-token authentication (same as every other dashboard endpoint).

---

## Helpop

The `/helpop` command lets players send requests to online staff. Staff receive them if they have the `neoessentials.helpop.receive` permission.

### Commands

| Command | Aliases | Permission | Description |
|---|---|---|---|
| `/helpop <message>` | `/ac`, `/amsg` | `neoessentials.helpop` | Send a help request to all online staff |

### Permission nodes

| Node | Default | Description |
|---|---|---|
| `neoessentials.helpop` | `true` | Send a help request |
| `neoessentials.helpop.receive` | `false` | Receive help requests (staff) |

### Behaviour

- Messages are broadcast to all online players who have `neoessentials.helpop.receive`.
- The sender's name is prefixed in the staff notification (e.g. `[HelpOp] PlayerName: message`).
- Console operators always receive helpop messages.

---

## Suicide

### Commands

| Command | Aliases | Permission | Description |
|---|---|---|---|
| `/suicide` | `/killme` | `neoessentials.suicide` | Kill yourself (requires confirmation) |
| `/suicide confirm` | `/killme confirm` | `neoessentials.suicide` | Confirm the suicide within 10 seconds |

### Behaviour

1. `/suicide` displays a confirmation prompt. Players have **10 seconds** to type `/suicide confirm`.
2. If the confirmation window expires, the prompt is silently cancelled.
3. After a successful suicide, a **30-second cooldown** prevents spamming.
4. `/suicide` is blocked in **Creative** and **Spectator** modes.
5. Death uses `DamageTypes.GENERIC_KILL` (does not count as PvP; no attacker credited).

> **Note:** `/kill` is the vanilla admin command (can target entities). NeoEssentials' `/suicide` is strictly self-only and is intentionally not aliased to `/kill`.

---

## Depth

Shows Y-coordinate information including depth below sea level and height above bedrock.

### Commands

| Command | Permission | Description |
|---|---|---|
| `/depth` | `neoessentials.depth` | Show own depth/elevation info |
| `/depth <player>` | `neoessentials.depth.others` | Show another player's depth info |

### Output

- Current Y level
- Depth below / height above sea level (Y=63)
- Height above bedrock (Y=−64)
- Layer description (e.g. *Deepslate Region*, *Nether*, *Diamond Level*)

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
| `/xp` | alias | same | Alias for `/exp` |
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
