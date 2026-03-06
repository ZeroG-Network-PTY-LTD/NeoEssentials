# NeoEssentials — Permission System

> **Last updated:** 2026-03-06 · **Version:** 1.0.2.6  
> **Source of truth:** `PermissionRegistry.registerAllPermissions()` in the mod source.  
> All nodes listed here are **actively registered** and recognised by the permission engine.  
> Nodes marked `✅ default` are granted to every player automatically (including non-OP).  
> Nodes marked `🔒 op-only` require explicit grant or OP level 2+ unless overridden.

---

## Table of Contents
1. [Configuration](#configuration)
2. [How Permissions Work](#how-permissions-work)
3. [Wildcards & Inheritance](#wildcards--inheritance)
4. [Dynamic Nodes](#dynamic-nodes)
5. [Permission Nodes — Full Reference](#permission-nodes--full-reference)
   - [Core](#core)
   - [Economy](#economy)
   - [Teleportation](#teleportation)
   - [Kits](#kits)
   - [Items](#items)
   - [Chat & Messaging](#chat--messaging)
   - [Moderation](#moderation)
   - [Miscellaneous Utilities](#miscellaneous-utilities)
   - [Admin & Config](#admin--config)
   - [Permission System Commands](#permission-system-commands)
   - [Web Dashboard](#web-dashboard)
6. [Example groups.json](#example-groupsjson)
7. [External Permission Mods](#external-permission-mods)

---

## Configuration

**`config.json` → `permissions` section:**

| Key | Default | Description |
|---|---|---|
| `useExternalPermissions` | `false` | Use LuckPerms / FTB Ranks instead of built-in engine |
| `defaultGroup` | `"default"` | Group assigned to new players |
| `opsBypassPermissions` | `true` | OPs (level 2+) bypass all permission checks |
| `cachePermissions` | `true` | Cache permission lookups for performance |
| `permissionCacheExpiryMinutes` | `5` | How long cached results are valid |

**Permission data file:** `neoessentials/permissions.json`

---

## How Permissions Work

1. When a player runs a command, `PermissionValidator.validatePermission()` is called.
2. It checks `PermissionAPI.hasPermission(uuid, node)`.
3. `PermissionAPI` checks (in order):
   - Player's explicit node grants/denials
   - Player's group (and inherited groups, highest priority first)
   - Wildcard nodes (`neoessentials.*`, `neoessentials.teleport.*`, etc.)
   - `opsBypassPermissions` — OPs skip the check entirely if enabled
4. If denied, the player sees: `§cYou don't have permission to use this command. §7Required: §f<node>`

---

## Wildcards & Inheritance

| Wildcard | Grants access to |
|---|---|
| `neoessentials.*` | Every permission in the mod |
| `neoessentials.economy.*` | All economy nodes |
| `neoessentials.teleport.*` | All teleport nodes |
| `neoessentials.teleport.admin.*` | All admin-teleport nodes |
| `neoessentials.teleport.home.*` | All home nodes |
| `neoessentials.teleport.request.*` | All TPA request nodes |
| `neoessentials.teleport.spawn.*` | All spawn nodes |
| `neoessentials.teleport.warp.*` | All warp nodes |
| `neoessentials.kits.*` | All kit nodes |
| `neoessentials.item.*` | All item management nodes |
| `neoessentials.chat.*` | All chat nodes |
| `neoessentials.moderation.*` | All moderation nodes |
| `neoessentials.permissions.*` | All permissions-command nodes |

> **Negative permissions** — prefix a node with `-` to explicitly deny it even if a wildcard grants it.  
> Example: give `neoessentials.*` then add `-neoessentials.item.enchant.unsafe` to deny unsafe enchanting.

---

## Dynamic Nodes

These are **not pre-registered** but are checked at runtime:

### Home limit
Pattern: `neoessentials.home.<number>` (1–100)  
The **highest number** the player has is used as their home limit.  
Example: `neoessentials.home.5` → player can set 5 homes.  
If no home-limit node is found, the config default is used.

### Warp limit
Pattern: `neoessentials.warp.limit.<number>` (1–100)  
Example: `neoessentials.warp.limit.10` → player can create 10 player-warps.  
Special: `neoessentials.warp.limit.unlimited` → no limit.

### Per-kit nodes
Pattern: `neoessentials.kits.<kitname>` — grants access to that specific kit.  
Pattern: `neoessentials.kits.<kitname>.nocooldown` — bypasses the cooldown for that kit.  
These are **registered automatically** when a kit is created via `/createkit`.

---

## Permission Nodes — Full Reference

### Core

| Node | Default | Description |
|---|---|---|
| `neoessentials.use` | ✅ default | Basic mod usage — required for all commands |
| `neoessentials.info` | ✅ default | View mod information (`/neoe`) |

---

### Economy

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.economy.balance` | ✅ default | Check own balance | `/balance` |
| `neoessentials.economy.balance.others` | 🔒 op-only | Check another player's balance | `/balance <player>` |
| `neoessentials.economy.pay` | ✅ default | Send money to online players | `/pay` |
| `neoessentials.economy.pay.offline` | 🔒 op-only | Send money to offline players | `/pay` |
| `neoessentials.economy.pay.toggle` | ✅ default | Toggle receiving payments | `/paytoggle` |
| `neoessentials.economy.baltop` | ✅ default | View balance leaderboard | `/baltop [page]` |
| `neoessentials.economy.baltop.exempt` | 🔒 op-only | Exclude self from baltop ranking | |
| `neoessentials.economy.eco` | 🔒 op-only | Run eco admin commands | `/eco` |
| `neoessentials.economy.admin` | 🔒 op-only | Economy administration (parent node) | `/eco` |
| `neoessentials.economy.admin.give` | 🔒 op-only | Give money to a player | `/eco give` |
| `neoessentials.economy.admin.take` | 🔒 op-only | Take money from a player | `/eco take` |
| `neoessentials.economy.admin.set` | 🔒 op-only | Set a player's balance | `/eco set` |
| `neoessentials.economy.admin.reset` | 🔒 op-only | Reset a player's balance to starting balance | `/eco reset` |
| `neoessentials.worth` | ✅ default | Check sell value of item | `/worth [item] [amount]` |
| `neoessentials.sell` | ✅ default | Use the sell command | `/sell` |
| `neoessentials.sell.hand` | ✅ default | Sell item in hand | `/sell hand [amount]` |
| `neoessentials.sell.bulk` | ✅ default | Sell entire inventory | `/sell inventory\|all` |
| `neoessentials.setworth` | 🔒 op-only | Set item sell prices | `/setworth <item\|hand> <price\|remove>` |

---

### Teleportation

#### Admin Teleport
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.teleport.admin` | 🔒 op-only | Admin teleport (parent node) | |
| `neoessentials.teleport.admin.tp` | 🔒 op-only | Teleport a player to another | `/tp <player> <target>` |
| `neoessentials.teleport.tp` | 🔒 op-only | Teleport self (alias) | `/tp <player>` |
| `neoessentials.teleport.admin.tphere` | 🔒 op-only | Bring a player to you | `/tphere` |
| `neoessentials.teleport.tphere` | 🔒 op-only | Bring a player to you (alias) | `/tphere` |
| `neoessentials.teleport.admin.tpall` | 🔒 op-only | Teleport all players to a target | `/tpall` |
| `neoessentials.teleport.admin.tppos` | 🔒 op-only | Teleport to coordinates | `/tppos` |
| `neoessentials.teleport.tppos` | 🔒 op-only | Teleport to coordinates (alias) | `/tppos` |
| `neoessentials.teleport.admin.tpo` | 🔒 op-only | Teleport to offline player's last location | `/tpo` |

#### Teleport Requests (TPA)
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.teleport.request.tpa` | ✅ default | Send a teleport request | `/tpa <player>` |
| `neoessentials.teleport.request.tpahere` | ✅ default | Request a player teleport to you | `/tpahere <player>` |
| `neoessentials.teleport.request.accept` | ✅ default | Accept a teleport request | `/tpaccept` |
| `neoessentials.teleport.request.deny` | ✅ default | Deny a teleport request | `/tpdeny` |
| `neoessentials.teleport.request.cancel` | ✅ default | Cancel a sent request | `/tpcancel` |

#### Home System
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.teleport.home` | ✅ default | Use the home system | `/home` |
| `neoessentials.teleport.home.set` | ✅ default | Set a home location | `/sethome` |
| `neoessentials.teleport.home.delete` | ✅ default | Delete a home | `/delhome` |
| `neoessentials.teleport.home.list` | ✅ default | List homes | `/homes` |
| `neoessentials.teleport.home.others` | 🔒 op-only | Access other players' homes | `/home <player>:<name>` |
| `neoessentials.home.<number>` | — | **Dynamic** — sets home limit (see above) | |

#### Warp System
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.teleport.warp` | ✅ default | Use warps | `/warp <name>` |
| `neoessentials.teleport.warp.list` | ✅ default | List available warps | `/warps [page]`, `/warp` |
| `neoessentials.teleport.warp.others` | 🔒 op-only | Warp another player to a warp | `/warp <name> <player>` |
| `neoessentials.teleport.warp.create` | 🔒 op-only | Create a warp | `/setwarp` |
| `neoessentials.teleport.warp.delete` | 🔒 op-only | Delete a warp | `/delwarp` |
| `neoessentials.warps.<name>` | — | **Per-warp** — access to specific warp (when `perWarpPermission: true` in config) | |
| `neoessentials.warps.*` | 🔒 op-only | Access ALL warps regardless of per-warp permissions | |
| `neoessentials.teleport.pwarp` | ✅ default | Use player warps | `/pwarp` |
| `neoessentials.teleport.pwarp.create` | ✅ default | Create a player warp | `/pwarp create` |
| `neoessentials.teleport.pwarp.delete` | ✅ default | Delete a player warp | `/pwarp delete` |
| `neoessentials.teleport.pwarp.list` | ✅ default | List player warps | `/pwarp list` |
| `neoessentials.warp.limit.<number>` | — | **Dynamic** — sets player-warp limit | |
| `neoessentials.warp.limit.unlimited` | 🔒 op-only | Unlimited player warps | |

#### Spawn System
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.teleport.spawn` | ✅ default | Teleport to spawn | `/spawn` |
| `neoessentials.teleport.spawn.set` | 🔒 op-only | Set the server spawn | `/setspawn` |
| `neoessentials.teleport.spawn.info` | 🔒 op-only | View spawn info | `/spawninfo` |
| `neoessentials.teleport.spawn.clear` | 🔒 op-only | Clear spawn location | `/clearspawn` |

#### Misc Teleport
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.teleport.back` | ✅ default | Return to previous location | `/back` |
| `neoessentials.teleport.death` | ✅ default | Teleport to death location | `/back` (on death) |
| `neoessentials.teleport.top` | ✅ default | Teleport to highest block | `/top` |
| `neoessentials.teleport.jump` | ✅ default | Teleport through walls | `/jump` |
| `neoessentials.teleport.jumpto` | ✅ default | Teleport to block you're looking at | `/jumpto` |
| `neoessentials.teleport.tpr` | ✅ default | Random teleport | `/tpr` |
| `neoessentials.teleport.settpr` | 🔒 op-only | Set random teleport centre | `/settpr` |

---

### Kits

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.kits.use` | ✅ default | Use the kit system | `/kit` |
| `neoessentials.kits.list` | ✅ default | List available kits | `/kit`, `/listkits` |
| `neoessentials.kits.nocooldown` | 🔒 op-only | Bypass all kit cooldowns | |
| `neoessentials.kit.others` | 🔒 op-only | Give a kit to another player | `/kit <name> <player>` |
| `neoessentials.kitreset` | 🔒 op-only | Reset own kit cooldown | `/kitreset <kit>` |
| `neoessentials.kitreset.others` | 🔒 op-only | Reset another player's kit cooldown | `/kitreset <kit> <player>` |
| `neoessentials.kits.create` | 🔒 op-only | Create a kit from inventory | `/createkit` |
| `neoessentials.kits.delete` | 🔒 op-only | Delete a kit | `/delkit` |
| `neoessentials.kits.override` | 🔒 op-only | Override all kit restrictions | |
| `neoessentials.kits.admin` | 🔒 op-only | Kit administration (parent) | `/kit admin` |
| `neoessentials.kits.admin.create` | 🔒 op-only | Admin kit creation | |
| `neoessentials.kits.admin.delete` | 🔒 op-only | Admin kit deletion | |
| `neoessentials.kits.admin.list` | 🔒 op-only | List all kits (admin) | |
| `neoessentials.kits.<kitname>` | — | **Dynamic** — access to specific kit | |
| `neoessentials.kits.<kitname>.nocooldown` | — | **Dynamic** — bypass cooldown for specific kit | |

---

### Player State & Admin Tools

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.fly` | 🔒 op-only | Toggle flight mode | `/fly [on\|off]` |
| `neoessentials.fly.others` | 🔒 op-only | Toggle flight for another player | `/fly <player> [on\|off]` |
| `neoessentials.god` | 🔒 op-only | Toggle god mode (invincibility) | `/god [on\|off]` |
| `neoessentials.god.others` | 🔒 op-only | Toggle god mode for another player | `/god <player> [on\|off]` |
| `neoessentials.heal` | 🔒 op-only | Restore own health and hunger | `/heal` |
| `neoessentials.heal.others` | 🔒 op-only | Restore another player's health | `/heal <player>` |
| `neoessentials.feed` | 🔒 op-only | Restore own hunger | `/feed` |
| `neoessentials.feed.others` | 🔒 op-only | Restore another player's hunger | `/feed <player>` |
| `neoessentials.speed` | 🔒 op-only | Set own walk or fly speed (0–10) | `/speed [walk\|fly] <0-10>` |
| `neoessentials.speed.others` | 🔒 op-only | Set another player's speed | `/speed [walk\|fly] <0-10> <player>` |
| `neoessentials.ext` | ✅ default | Extinguish own fire | `/ext` |
| `neoessentials.ext.others` | 🔒 op-only | Extinguish another player | `/ext <player>` |
| `neoessentials.burn` | 🔒 op-only | Set a player on fire | `/burn <player> [seconds]` |
| `neoessentials.give` | 🔒 op-only | Give items to players | `/give <player> <item> [amount]` |
| `neoessentials.more` | 🔒 op-only | Fill held stack to max | `/more [amount]` |
| `neoessentials.hat` | 🔒 op-only | Wear held item as helmet | `/hat` |
| `neoessentials.exp` | ✅ default | View own XP info | `/exp [show]` |
| `neoessentials.exp.set` | 🔒 op-only | Set own XP | `/exp set <amount>` |
| `neoessentials.exp.set.others` | 🔒 op-only | Set another player's XP | `/exp set <amount> <player>` |
| `neoessentials.exp.give` | 🔒 op-only | Give XP to self | `/exp give <amount>` |
| `neoessentials.exp.give.others` | 🔒 op-only | Give XP to another player | `/exp give <amount> <player>` |
| `neoessentials.sudo` | 🔒 op-only | Run a command as another player | `/sudo <player> <command>` |
| `neoessentials.sudo.exempt` | 🔒 op-only | Cannot be sudo'd by non-console | |
| `neoessentials.playtime` | ✅ default | View own playtime | `/playtime` |
| `neoessentials.playtime.others` | 🔒 op-only | View another player's playtime | `/playtime <player>` |

---

### Items

### Server Admin Commands

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.broadcast` | 🔒 op-only | Broadcast a message to all players | `/broadcast <msg>`, `/bc`, `/announce` |
| `neoessentials.time` | 🔒 op-only | View current world time | `/time` |
| `neoessentials.time.set` | 🔒 op-only | Set or add world time | `/time set\|add <value>`, `/day`, `/night` |
| `neoessentials.weather` | 🔒 op-only | Set world weather | `/weather <sun\|storm\|thunder> [dur]`, `/sun`, `/storm`, `/thunder` |
| `neoessentials.kill` | 🔒 op-only | Kill a player | `/kill <player>` |
| `neoessentials.kill.exempt` | 🔒 op-only | Exempt from being killed by /kill | |
| `neoessentials.kill.force` | 🔒 op-only | Force kill even exempt players | |
| `neoessentials.gamemode` | 🔒 op-only | Change own gamemode | `/gamemode <mode>` |
| `neoessentials.gamemode.others` | 🔒 op-only | Change another player's gamemode | `/gamemode <mode> <player>` |
| `neoessentials.teleport.tpo` | 🔒 op-only | Teleport to player (bypass tptoggle) | `/tpo <player>` |
| `neoessentials.teleport.tpohere` | 🔒 op-only | Bring player to you (bypass tptoggle) | `/tpohere <player>` |
| `neoessentials.teleport.tpoffline` | 🔒 op-only | Teleport to offline player's last position | `/tpoffline <player>` |

---

### Utility Commands

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.ptime` | 🔒 op-only | Set own per-player time override | `/ptime [reset\|day\|night\|<ticks>]` |
| `neoessentials.ptime.others` | 🔒 op-only | Set another player's time override | `/ptime <value> <player>` |
| `neoessentials.pweather` | 🔒 op-only | Set own per-player weather override | `/pweather [reset\|sun\|storm]` |
| `neoessentials.pweather.others` | 🔒 op-only | Set another player's weather override | `/pweather <type> <player>` |
| `neoessentials.effect` | 🔒 op-only | Apply potion effects to players | `/effect <player> <effect\|clear> [dur] [amp]` |
| `neoessentials.spawnmob` | 🔒 op-only | Spawn entities at own location | `/spawnmob <mob> [amount]`, `/mob` |
| `neoessentials.spawnmob.others` | 🔒 op-only | Spawn entities at another player | `/spawnmob <mob> [amount] <player>` |
| `neoessentials.unlimited` | 🔒 op-only | Toggle unlimited item use | `/unlimited [list\|clear\|<item>]` |
| `neoessentials.unlimited.others` | 🔒 op-only | Toggle unlimited items for another player | `/unlimited <item> <player>` |
| `neoessentials.condense` | 🔒 op-only | Condense items to storage blocks | `/condense [item]` |

---

### Item Customisation & Miscellaneous

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.me` | ✅ default | Broadcast action messages | `/me <action>` |
| `neoessentials.tptoggle` | ✅ default | Toggle own teleport request acceptance | `/tptoggle [on\|off]` |
| `neoessentials.tptoggle.others` | 🔒 op-only | Toggle tptoggle for another player | `/tptoggle <player> [on\|off]` |
| `neoessentials.gc` | 🔒 op-only | View server TPS, memory, uptime, chunk info | `/gc`, `/mem` |
| `neoessentials.lightning` | 🔒 op-only | Strike lightning at look target | `/lightning`, `/smite` |
| `neoessentials.lightning.others` | 🔒 op-only | Strike lightning at a named player | `/lightning <player>` |
| `neoessentials.skull` | 🔒 op-only | Get a player head item | `/skull [player]` |
| `neoessentials.itemname` | 🔒 op-only | Rename the held item | `/itemname [name\|-]`, `/rename` |
| `neoessentials.itemlore` | 🔒 op-only | Edit held item lore lines | `/itemlore add\|set\|remove\|clear` |
| `neoessentials.remove` | 🔒 op-only | Remove entities in radius | `/remove <type> [radius]` |
| `neoessentials.loom` | 🔒 op-only | Open portable loom | `/loom` |
| `neoessentials.cartography` | 🔒 op-only | Open portable cartography table | `/cartography`, `/cartographytable` |

---

### Home & Warp Enhancements

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.renamehome` | ✅ default | Rename own home | `/renamehome <old> <new>` |
| `neoessentials.renamehome.others` | 🔒 op-only | Rename another player's home | `/renamehome <player:old> <new>` |
| `neoessentials.warpinfo` | ✅ default | Show warp coordinates and world | `/warpinfo <name>` |
| `neoessentials.world` | 🔒 op-only | Teleport to a world/dimension | `/world [name]` |
| `neoessentials.world.others` | 🔒 op-only | Teleport another player to a world | `/world <name> <player>` |
| `neoessentials.spawner` | 🔒 op-only | Change a mob spawner type | `/spawner <mob>` |
| `neoessentials.spawner.*` | 🔒 op-only | Change spawner to any mob | wildcard — grants all mob types |
| `neoessentials.spawner.<mob>` | 🔒 op-only | Change spawner to a specific mob | e.g. `neoessentials.spawner.zombie` |
| `neoessentials.recipe` | ✅ default | Show/unlock crafting recipe for an item | `/recipe [item]` |
| `neoessentials.tpauto` | ✅ default | Auto-accept all incoming teleport requests | `/tpauto [on\|off]` |
| `neoessentials.tpauto.others` | 🔒 op-only | Toggle tpauto for another player | `/tpauto <player> [on\|off]` |

---

### World Interaction & Fun Commands

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.fireball` | 🔒 op-only | Shoot a projectile | `/fireball [type] [speed]` |
| `neoessentials.fireball.*` | 🔒 op-only | Shoot any projectile type | wildcard |
| `neoessentials.fireball.<type>` | 🔒 op-only | Shoot specific type (fireball/small/large/arrow/skull/egg/snowball/expbottle/dragon/trident/windcharge) | e.g. `neoessentials.fireball.arrow` |
| `neoessentials.fireball.ride` | 🔒 op-only | Ride the shot projectile | `/fireball <type> <speed> ride` |
| `neoessentials.tree` | 🔒 op-only | Grow a tree at look target | `/tree <type>`, `/bigtree` |
| `neoessentials.break` | 🔒 op-only | Instantly break the looked-at block (no drops) | `/break` |
| `neoessentials.break.bedrock` | 🔒 op-only | Break bedrock blocks | permission bypass |
| `neoessentials.ice` | 🔒 op-only | Freeze self solid | `/ice` |
| `neoessentials.ice.others` | 🔒 op-only | Freeze another player | `/ice <player>` |
| `neoessentials.bottom` | 🔒 op-only | Teleport to world bottom at current XZ | `/bottom` |
| `neoessentials.tpaall` | 🔒 op-only | Send tpa-here to all online players | `/tpaall [player]` |
| `neoessentials.tpaall.others` | 🔒 op-only | Send tpaall on behalf of another player | `/tpaall <player>` |
| `neoessentials.broadcastworld` | 🔒 op-only | Broadcast to players in your current world | `/broadcastworld`, `/bcastworld` |

---

### Player Info & Admin Tools

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.seen` | ✅ default | View when a player was last online | `/seen <player>` |
| `neoessentials.near` | ✅ default | List players within a radius | `/near [radius]` |
| `neoessentials.ping` | ✅ default | View your own ping | `/ping` |
| `neoessentials.ping.others` | ✅ default | View another player's ping | `/ping <player>` |
| `neoessentials.playtime` | ✅ default | View your total play time | `/playtime` |
| `neoessentials.playtime.others` | ✅ default | View another player's play time | `/playtime <player>` |
| `neoessentials.whois` | 🔒 op-only | View detailed player info (UUID, pos, gamemode, health) | `/whois <player>` |
| `neoessentials.realname` | ✅ default | Look up real name from nickname | `/realname <nickname>` |
| `neoessentials.sudo` | 🔒 op-only | Force a player to run a command | `/sudo <player> <command>` |
| `neoessentials.sudo.exempt` | 🔒 op-only | Be immune to /sudo | permission node |
| `neoessentials.suicide` | ✅ default | Kill yourself | `/suicide` |
| `neoessentials.msgtoggle` | ✅ default | Toggle your incoming private messages on/off | `/msgtoggle [on\|off]` |
| `neoessentials.msgtoggle.others` | 🔒 op-only | Toggle another player's messages | `/msgtoggle <player> [on\|off]` |
| `neoessentials.rtoggle` | ✅ default | Toggle reply-to-last-sender for `/r` | `/rtoggle [on\|off]` |
| `neoessentials.rtoggle.others` | 🔒 op-only | Toggle rtoggle for another player | `/rtoggle <player> [on\|off]` |
| `neoessentials.motd` | ✅ default | View the message of the day | `/motd` |
| `neoessentials.rules` | ✅ default | View server rules | `/rules` |

---

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.item.repair` | 🔒 op-only | Repair held item | `/repair` |
| `neoessentials.item.enchant` | 🔒 op-only | Enchant held item | `/enchant` |
| `neoessentials.item.enchant.unsafe` | 🔒 op-only | Apply enchants beyond vanilla limits | `/enchant` |
| `neoessentials.item.enchant.others` | 🔒 op-only | Enchant another player's item | `/enchant <player>` |
| `neoessentials.item.enchant.any` | 🔒 op-only | Enchant any item (ignore type restrictions) | `/enchant` |
| `neoessentials.item.powertool` | 🔒 op-only | Use the powertool system | `/powertool` |
| `neoessentials.item.powertool.toggle` | 🔒 op-only | Toggle powertool on/off | `/pttoggle` |
| `neoessentials.item.dispose` | ✅ default | Use the item disposal chest | `/dispose` |
| `neoessentials.item.clearinventory` | 🔒 op-only | Clear own inventory | `/clearinv` |
| `neoessentials.item.clearinventory.others` | 🔒 op-only | Clear another player's inventory | `/clearinv <player>` |
| `neoessentials.item.spawn` | 🔒 op-only | Spawn items | `/spawnitem` |
| `neoessentials.invsee` | 🔒 op-only | View another player's inventory | `/invsee` |
| `neoessentials.invsee.edit` | 🔒 op-only | Edit another player's inventory | `/invsee` |
| `neoessentials.enderchest` | 🔒 op-only | View another player's ender chest | `/ec <player>` |
| `neoessentials.enderchest.edit` | 🔒 op-only | Edit another player's ender chest | `/ec <player>` |

---

### Chat & Messaging

#### Private Messaging
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.chat.msg` | ✅ default | Send private messages | `/msg` |
| `neoessentials.chat.reply` | ✅ default | Reply to messages | `/reply` |
| `neoessentials.chat.ignore` | ✅ default | Ignore a player | `/ignore` |
| `neoessentials.chat.unignore` | ✅ default | Unignore a player | `/unignore` |
| `neoessentials.chat.msgtoggle` | ✅ default | Toggle receiving messages | `/msgtoggle` |
| `neoessentials.chat.socialspy` | 🔒 op-only | See all private messages | `/socialspy` |
| `neoessentials.chat.socialspy.exempt` | 🔒 op-only | Private messages not visible to socialspy | |
| `neoessentials.chat.msgtoggle.bypass` | 🔒 op-only | Message players who have toggled off | |

#### Mail
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.mail` | ✅ default | Use the mail system | `/mail` |
| `neoessentials.mail.send` | ✅ default | Send mail to a player | `/mail send` |
| `neoessentials.mail.clear` | ✅ default | Clear own mailbox | `/mail clear` |

#### Moderation Chat
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.chat.mute` | 🔒 op-only | Mute a player | `/mute` |
| `neoessentials.chat.unmute` | 🔒 op-only | Unmute a player | `/unmute` |
| `neoessentials.chat.mutelist` | 🔒 op-only | View muted players | `/mutelist` |
| `neoessentials.chat.exempt` | 🔒 op-only | Exempt from being muted | |

#### Formatting & Colours
| Node | Default | Description |
|---|---|---|
| `neoessentials.chat.color` | 🔒 op-only | Use `&0-9`, `&a-f` colour codes in chat |
| `neoessentials.chat.color.hex` | 🔒 op-only | Use `&#RRGGBB` hex colours in chat |
| `neoessentials.chat.format` | 🔒 op-only | Use `&k-o`, `&r` formatting codes in chat |
| `neoessentials.chat.richtext` | 🔒 op-only | Use gradient/rainbow rich text effects |
| `neoessentials.chat.gradient` | 🔒 op-only | Use gradient text effects |
| `neoessentials.chat.rainbow` | 🔒 op-only | Use rainbow text effects |

#### Chat Channels
| Node | Default | Description |
|---|---|---|
| `neoessentials.chat.channel.local` | ✅ default | Use local chat channel |
| `neoessentials.chat.channel.global` | ✅ default | Use global chat channel |
| `neoessentials.chat.staff` | 🔒 op-only | Access staff chat channel |
| `neoessentials.chat.mention` | ✅ default | Mention players with `@name` |
| `neoessentials.chat.mention.all` | 🔒 op-only | Mention everyone with `@everyone` |
| `neoessentials.chat.itemlink` | ✅ default | Show held item in chat with `[item]` |

#### Anti-Spam Bypasses
| Node | Default | Description |
|---|---|---|
| `neoessentials.chat.caps.bypass` | 🔒 op-only | Bypass caps filter |
| `neoessentials.chat.repeat.bypass` | 🔒 op-only | Bypass repeat-message filter |
| `neoessentials.chat.links.bypass` | 🔒 op-only | Bypass link filter |
| `neoessentials.chat.spam.bypass` | 🔒 op-only | Bypass spam rate limit |

---

### Moderation

| Node | Default | Description | Command |
|---|---|---|---|
| **Banning** | | | |
| `neoessentials.moderation.ban` | 🔒 op-only | Ban a player | `/ban` |
| `neoessentials.moderation.banip` | 🔒 op-only | Ban an IP address | `/banip` |
| `neoessentials.moderation.banlist` | 🔒 op-only | View the ban list | `/banlist` |
| `neoessentials.moderation.tempban` | 🔒 op-only | Temporarily ban a player | `/tempban` |
| `neoessentials.moderation.unban` | 🔒 op-only | Unban a player | `/unban` |
| `neoessentials.moderation.unbanip` | 🔒 op-only | Unban an IP address | `/unbanip` |
| **Kicking** | | | |
| `neoessentials.moderation.kick` | 🔒 op-only | Kick a player | `/kick` |
| `neoessentials.moderation.kickall` | 🔒 op-only | Kick all players | `/kickall` |
| **Freezing** | | | |
| `neoessentials.moderation.freeze` | 🔒 op-only | Freeze a player | `/freeze` |
| `neoessentials.moderation.unfreeze` | 🔒 op-only | Unfreeze a player | `/unfreeze` |
| `neoessentials.moderation.freezeall` | 🔒 op-only | Freeze all players | `/freezeall` |
| `neoessentials.moderation.unfreezeall` | 🔒 op-only | Unfreeze all players | `/unfreezeall` |
| `neoessentials.moderation.freezelist` | 🔒 op-only | List frozen players | `/freezelist` |
| **Jailing** | | | |
| `neoessentials.moderation.jail` | 🔒 op-only | Jail a player | `/jail` |
| `neoessentials.moderation.unjail` | 🔒 op-only | Unjail a player | `/unjail` |
| `neoessentials.moderation.setjail` | 🔒 op-only | Create a jail location | `/setjail` |
| `neoessentials.moderation.jaillist` | 🔒 op-only | List jailed players | `/jaillist` |
| `neoessentials.moderation.jailinfo` | 🔒 op-only | View jail info | `/jailinfo` |
| **Vanish** | | | |
| `neoessentials.moderation.vanish` | 🔒 op-only | Vanish yourself | `/vanish` |
| `neoessentials.moderation.vanish.others` | 🔒 op-only | Vanish another player | `/vanish <player>` |
| `neoessentials.moderation.seevanished` | 🔒 op-only | See vanished players | |
| `neoessentials.vanish.see` | 🔒 op-only | See vanished players (alias) | |
| `neoessentials.moderation.vanishlist` | 🔒 op-only | List vanished players | `/vanishlist` |
| **Notifications** | | | |
| `neoessentials.moderation.notify` | 🔒 op-only | Receive moderation action notifications | |
| `neoessentials.moderation.notifications` | 🔒 op-only | Receive moderation event broadcasts | |

---

### Miscellaneous Utilities

#### Player Info
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.list` | ✅ default | View online player list | `/list`, `/who` |
| `neoessentials.near` | ✅ default | View nearby players | `/near` |
| `neoessentials.seen` | ✅ default | Check when a player was last seen | `/seen` |
| `neoessentials.whois` | ✅ default | View player info | `/whois` |
| `neoessentials.whois.detailed` | 🔒 op-only | View detailed player info | `/whois` |
| `neoessentials.ping` | ✅ default | Check own ping | `/ping` |
| `neoessentials.ping.others` | 🔒 op-only | Check another player's ping | `/ping <player>` |
| `neoessentials.realname` | ✅ default | Look up a player's real name from nickname | `/realname` |
| `neoessentials.depth` | ✅ default | View depth/Y-level info | `/depth` |
| `neoessentials.depth.others` | 🔒 op-only | View another player's depth info | `/depth <player>` |
| `neoessentials.compass` | ✅ default | View compass/direction info | `/compass` |
| `neoessentials.compass.others` | 🔒 op-only | View compass info for another player | `/compass <player>` |
| `neoessentials.getpos` | ✅ default | View own position | `/getpos` |
| `neoessentials.getpos.others` | 🔒 op-only | View another player's position | `/getpos <player>` |

#### Nicknames
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.nick` | ✅ default | Change own nickname | `/nick` |
| `neoessentials.nick.color` | 🔒 op-only | Use colour codes in nickname | `/nick` |
| `neoessentials.nick.others` | 🔒 op-only | Change another player's nickname | `/setnick` |

#### Server Info
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.motd` | ✅ default | View the message of the day | `/motd` |
| `neoessentials.rules` | ✅ default | View server rules | `/rules` |
| `neoessentials.helpop` | ✅ default | Send a help request to staff | `/helpop` |
| `neoessentials.helpop.receive` | 🔒 op-only | Receive help-op requests | |
| `neoessentials.staff` | 🔒 op-only | Access staff chat and features | |

#### Portable Workstations
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.anvil` | ✅ default | Open portable anvil | `/anvil` |
| `neoessentials.crafting` | ✅ default | Open portable crafting table | `/craft` |
| `neoessentials.grindstone` | ✅ default | Open portable grindstone | `/grindstone` |
| `neoessentials.smithing` | ✅ default | Open portable smithing table | `/smithing` |
| `neoessentials.stonecutting` | ✅ default | Open portable stonecutter | `/stonecutter` |

#### Book & Sign
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.book` | ✅ default | Give yourself a writable book | `/book` |
| `neoessentials.book.unlock` | 🔒 op-only | Unlock a written book for editing | `/book unlock` |
| `neoessentials.book.title` | 🔒 op-only | Set a book's title | `/book title` |
| `neoessentials.book.author` | 🔒 op-only | Set a book's author | `/book author` |
| `neoessentials.sign` | ✅ default | Edit sign text | `/sign` |
| `neoessentials.sign.colors` | 🔒 op-only | Use colours on signs | `/sign` |

#### AFK, Gamemode & Other
| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.afk` | ✅ default | Use the AFK system | `/afk` |
| `neoessentials.afk.exempt` | 🔒 op-only | Exempt from AFK kick | |
| `neoessentials.suicide` | ✅ default | Use the suicide command | `/suicide` |
| `neoessentials.gamemode` | 🔒 op-only | Change own gamemode | `/gm`, `/gmc`, `/gms` |
| `neoessentials.gamemode.others` | 🔒 op-only | Change another player's gamemode | `/gm <player>` |

---

### Admin & Config

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.admin` | 🔒 op-only | General admin access | |
| `neoessentials.reload` | 🔒 op-only | Reload the mod configuration | `/neoe reload` |
| `neoessentials.debug` | 🔒 op-only | Enable debug logging | |
| `neoessentials.rules.admin` | 🔒 op-only | Create/edit/delete server rules | `/rules add` etc. |
| `neoessentials.motd.set` | 🔒 op-only | Set the message of the day | `/motd set` |
| `neoessentials.motd.broadcast` | 🔒 op-only | Broadcast the MOTD to all players | `/motd broadcast` |
| `neoessentials.motd.reload` | 🔒 op-only | Reload MOTD from file | `/motd reload` |

---

### Permission System Commands

| Node | Default | Description | Command |
|---|---|---|---|
| `neoessentials.permissions.admin` | 🔒 op-only | Full permissions system access | `/permissions` |
| `neoessentials.permissions.reload` | 🔒 op-only | Reload the permissions system | `/permissions reload` |
| `neoessentials.permissions.list` | 🔒 op-only | List registered permission nodes | `/permissions list` |
| `neoessentials.permissions.check` | 🔒 op-only | Check a player's effective permissions | `/permissions check` |
| `neoessentials.permissions.search` | 🔒 op-only | Search permission nodes | `/permissions search` |
| `neoessentials.permissions.user` | 🔒 op-only | User management (parent) | `/permissions user` |
| `neoessentials.permissions.user.permissions` | 🔒 op-only | Add/remove user permission nodes | |
| `neoessentials.permissions.user.groups` | 🔒 op-only | Add/remove user from groups | |
| `neoessentials.permissions.user.clear` | 🔒 op-only | Clear all user permissions | |
| `neoessentials.permissions.list.users` | 🔒 op-only | List all permission users | |
| `neoessentials.permissions.info.user` | 🔒 op-only | View a user's permission info | |
| `neoessentials.permissions.group` | 🔒 op-only | Group management (parent) | `/permissions group` |
| `neoessentials.permissions.group.create` | 🔒 op-only | Create a new group | |
| `neoessentials.permissions.group.delete` | 🔒 op-only | Delete a group | |
| `neoessentials.permissions.group.rename` | 🔒 op-only | Rename a group | |
| `neoessentials.permissions.group.clone` | 🔒 op-only | Clone a group | |
| `neoessentials.permissions.group.inherit` | 🔒 op-only | Set group inheritance | |
| `neoessentials.permissions.group.permissions` | 🔒 op-only | Manage group permission nodes | |
| `neoessentials.permissions.group.modify` | 🔒 op-only | Modify group settings (prefix/suffix) | |
| `neoessentials.permissions.group.clear` | 🔒 op-only | Clear all group permissions | |
| `neoessentials.permissions.list.groups` | 🔒 op-only | List all groups | |
| `neoessentials.permissions.info.group` | 🔒 op-only | View a group's info | |

---

### Web Dashboard

| Node | Default | Description |
|---|---|---|
| `neoessentials.admin.dashboard` | 🔒 op-only | Access the admin dashboard command |
| `neoessentials.dashboard.access` | 🔒 op-only | Register an account and log in to the dashboard |
| `neoessentials.dashboard.view` | 🔒 op-only | View-only dashboard access |
| `neoessentials.dashboard.manage` | 🔒 op-only | Manage dashboard settings |
| `neoessentials.dashboard.moderator` | 🔒 op-only | Moderator-level dashboard access |
| `neoessentials.dashboard.admin` | 🔒 op-only | Full admin dashboard access |

---

## Example groups.json

```json
{
  "defaultGroup": "default",
  "groups": [
    {
      "name": "default",
      "prefix": "§7",
      "suffix": "",
      "permissions": [
        "neoessentials.use",
        "neoessentials.economy.balance",
        "neoessentials.economy.pay",
        "neoessentials.economy.pay.toggle",
        "neoessentials.economy.baltop",
        "neoessentials.teleport.request.tpa",
        "neoessentials.teleport.request.tpahere",
        "neoessentials.teleport.request.accept",
        "neoessentials.teleport.request.deny",
        "neoessentials.teleport.request.cancel",
        "neoessentials.teleport.home",
        "neoessentials.teleport.home.set",
        "neoessentials.teleport.home.delete",
        "neoessentials.teleport.home.list",
        "neoessentials.home.3",
        "neoessentials.teleport.warp",
        "neoessentials.teleport.warp.list",
        "neoessentials.teleport.spawn",
        "neoessentials.teleport.back",
        "neoessentials.teleport.death",
        "neoessentials.teleport.tpr",
        "neoessentials.kits.use",
        "neoessentials.kits.list",
        "neoessentials.item.dispose",
        "neoessentials.chat.msg",
        "neoessentials.chat.reply",
        "neoessentials.chat.ignore",
        "neoessentials.chat.unignore",
        "neoessentials.chat.msgtoggle",
        "neoessentials.chat.channel.local",
        "neoessentials.chat.channel.global",
        "neoessentials.chat.mention",
        "neoessentials.chat.itemlink",
        "neoessentials.mail",
        "neoessentials.mail.send",
        "neoessentials.mail.clear",
        "neoessentials.list",
        "neoessentials.near",
        "neoessentials.seen",
        "neoessentials.whois",
        "neoessentials.ping",
        "neoessentials.realname",
        "neoessentials.motd",
        "neoessentials.rules",
        "neoessentials.helpop",
        "neoessentials.afk",
        "neoessentials.anvil",
        "neoessentials.crafting",
        "neoessentials.grindstone",
        "neoessentials.smithing",
        "neoessentials.stonecutting",
        "neoessentials.book",
        "neoessentials.sign",
        "neoessentials.nick",
        "neoessentials.suicide",
        "neoessentials.depth",
        "neoessentials.compass",
        "neoessentials.getpos",
        "neoessentials.info"
      ],
      "inherits": []
    },
    {
      "name": "vip",
      "prefix": "§6[VIP] §f",
      "suffix": "",
      "permissions": [
        "neoessentials.home.10",
        "neoessentials.teleport.top",
        "neoessentials.teleport.jump",
        "neoessentials.teleport.jumpto",
        "neoessentials.nick.color",
        "neoessentials.chat.color",
        "neoessentials.chat.format",
        "neoessentials.chat.richtext",
        "neoessentials.teleport.warp.create",
        "neoessentials.warp.limit.5",
        "neoessentials.item.repair",
        "neoessentials.sign.colors"
      ],
      "inherits": ["default"]
    },
    {
      "name": "moderator",
      "prefix": "§2[Mod] §f",
      "suffix": "",
      "permissions": [
        "neoessentials.moderation.ban",
        "neoessentials.moderation.banip",
        "neoessentials.moderation.banlist",
        "neoessentials.moderation.tempban",
        "neoessentials.moderation.unban",
        "neoessentials.moderation.unbanip",
        "neoessentials.moderation.kick",
        "neoessentials.moderation.kickall",
        "neoessentials.moderation.freeze",
        "neoessentials.moderation.unfreeze",
        "neoessentials.moderation.freezeall",
        "neoessentials.moderation.unfreezeall",
        "neoessentials.moderation.freezelist",
        "neoessentials.moderation.jail",
        "neoessentials.moderation.unjail",
        "neoessentials.moderation.setjail",
        "neoessentials.moderation.jaillist",
        "neoessentials.moderation.jailinfo",
        "neoessentials.moderation.vanish",
        "neoessentials.moderation.seevanished",
        "neoessentials.moderation.vanishlist",
        "neoessentials.moderation.notify",
        "neoessentials.chat.mute",
        "neoessentials.chat.unmute",
        "neoessentials.chat.mutelist",
        "neoessentials.chat.socialspy",
        "neoessentials.chat.staff",
        "neoessentials.staff",
        "neoessentials.helpop.receive",
        "neoessentials.whois.detailed",
        "neoessentials.nick.others",
        "neoessentials.teleport.admin.tp",
        "neoessentials.teleport.admin.tphere",
        "neoessentials.teleport.admin.tpo",
        "neoessentials.home.20",
        "neoessentials.warp.limit.unlimited",
        "neoessentials.dashboard.moderator"
      ],
      "inherits": ["vip"]
    },
    {
      "name": "admin",
      "prefix": "§c[Admin] §f",
      "suffix": "",
      "permissions": [
        "neoessentials.*"
      ],
      "inherits": ["moderator"]
    }
  ]
}
```

---

## External Permission Mods

NeoEssentials supports the following external permission systems when `useExternalPermissions: true`:

| Mod | Notes |
|---|---|
| **FTB Ranks** | Full support — ranks map to groups, all nodes respected |
| **LuckPerms** | Full support via the LuckPerms API adapter |
| **YAWP** | Basic support |

When an external system is active, the built-in `permissions.json` groups are **not used** for permission checks, but the registry still provides node metadata (descriptions, defaults) for export via `/permissions export`.

> **Tip:** Run `/permissions export luckperms` or `/permissions export ftbranks` to generate a ready-to-import config file for your preferred permission mod.
