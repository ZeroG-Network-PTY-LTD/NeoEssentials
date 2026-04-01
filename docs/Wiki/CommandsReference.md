# NeoEssentials — Complete Commands Reference

> **Last Updated:** 2026-03-06 · **Version:** 1.0.2.6  
> All commands are prefixed with `/`. Permission nodes follow `neoessentials.<node>` pattern.  
> `🔒` = op-only by default · `✅` = available to all players by default  
> Square brackets `[x]` = optional · Angle brackets `<x>` = required · `|` = or

---

## 📋 Table of Contents

1. [Economy](#economy)
2. [Teleportation](#teleportation)
3. [Homes](#homes)
4. [Warps](#warps)
5. [Spawn](#spawn)
6. [Player State & Admin Tools](#player-state--admin-tools)
7. [Server Admin](#server-admin)
8. [Moderation](#moderation)
9. [Chat & Messaging](#chat--messaging)
10. [Kits](#kits)
11. [Items](#items)
12. [Worth & Sell](#worth--sell)
13. [Utility](#utility)
14. [AFK](#afk)
15. [Web Dashboard](#web-dashboard)
16. [Permissions Management](#permissions-management)
17. [Mod Root Commands](#mod-root-commands)
18. [Miscellaneous](#miscellaneous)

---

## Economy

| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/balance` | `/balance [player]` | `neoessentials.economy.balance` / `.balance.others` | ✅ | Check own or another player's balance |
| `/bal` | alias for `/balance` | same | ✅ | Alias |
| `/pay` | `/pay <player> <amount>` | `neoessentials.economy.pay` | ✅ | Send money to an online player |
| `/paytoggle` | `/paytoggle` | `neoessentials.economy.pay.toggle` | ✅ | Toggle receiving payments |
| `/pt` | alias for `/paytoggle` | same | ✅ | Alias |
| `/baltop` | `/baltop [page]` | `neoessentials.economy.baltop` | ✅ | View top player balances |
| `/balancetop` | alias for `/baltop` | same | ✅ | Alias |
| `/eco` | `/eco give\|take\|set\|reset <player> <amount>` | `neoessentials.economy.eco` | 🔒 | Admin economy management |

---

## Teleportation

### Player Teleport
| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/tp` | `/tp <player>` or `/tp <x> <y> <z>` | `neoessentials.teleport.admin.tp` | 🔒 | Teleport to a player or coordinates |
| `/tphere` | `/tphere <player>` | `neoessentials.teleport.admin.tphere` | 🔒 | Teleport a player to you |
| `/tpall` | `/tpall` | `neoessentials.teleport.admin.tpall` | 🔒 | Teleport all players to you |
| `/tppos` | `/tppos <x> <y> <z>` | `neoessentials.teleport.admin.tppos` | 🔒 | Teleport to exact coordinates |
| `/tpo` | `/tpo <player>` | `neoessentials.teleport.tpo` | 🔒 | Teleport to player, bypassing their tptoggle |
| `/tpohere` | `/tpohere <player>` | `neoessentials.teleport.tpohere` | 🔒 | Bring player here, bypassing tptoggle |
| `/tpoffline` | `/tpoffline <player>` | `neoessentials.teleport.tpoffline` | 🔒 | Teleport to an offline player's last position |
| `/back` | `/back` | `neoessentials.teleport.back` | ✅ | Return to previous location |
| `/top` | `/top` | `neoessentials.teleport.top` | 🔒 | Teleport to the highest block above you |
| `/jump` | `/jump` | `neoessentials.teleport.jump` | 🔒 | Teleport to the block you are looking at |
| `/jumpto` | alias for `/jump` | same | 🔒 | Alias |

### Teleport Requests
| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/tpa` | `/tpa <player>` | `neoessentials.teleport.tpa` | ✅ | Request to teleport to a player |
| `/tpahere` | `/tpahere <player>` | `neoessentials.teleport.tpahere` | ✅ | Request a player teleport to you |
| `/tpaccept` | `/tpaccept` | `neoessentials.teleport.tpaccept` | ✅ | Accept a pending teleport request |
| `/tpdeny` | `/tpdeny` | `neoessentials.teleport.tpdeny` | ✅ | Deny a pending teleport request |
| `/tpacancel` | `/tpacancel` | `neoessentials.teleport.tpacancel` | ✅ | Cancel your outgoing teleport request |

### Random Teleport
| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/tpr` | `/tpr [location]` | `neoessentials.teleport.tpr` | ✅ | Teleport to a random location |
| `/rtp` | alias for `/tpr` | same | ✅ | Alias |
| `/randomtp` | alias for `/tpr` | same | ✅ | Alias |
| `/randomteleport` | alias for `/tpr` | same | ✅ | Alias |
| `/settpr` | `/settpr <name>` | `neoessentials.teleport.settpr` | 🔒 | Set a named RTP centre location |

---

## Homes

| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/home` | `/home [name]` | `neoessentials.home` | ✅ | Teleport to your home (or named home) |
| `/sethome` | `/sethome [name]` | `neoessentials.home.set` | ✅ | Set your home at current location |
| `/delhome` | `/delhome [name]` | `neoessentials.home.delete` | ✅ | Delete a home |
| `/deletehome` | alias for `/delhome` | same | ✅ | Alias |
| `/homes` | `/homes` | `neoessentials.home.list` | ✅ | List all your homes |

---

## Warps

### Server Warps
| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/warp` | `/warp <name>` | `neoessentials.warp` | ✅ | Teleport to a named warp |
| `/setwarp` | `/setwarp <name>` | `neoessentials.warp.set` | 🔒 | Create a warp at current location |
| `/delwarp` | `/delwarp <name>` | `neoessentials.warp.delete` | 🔒 | Delete a warp |
| `/warps` | `/warps [page]` | `neoessentials.warp.list` | ✅ | List all available warps |

### Player Warps
| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/pwarp` | `/pwarp <name>` | `neoessentials.pwarp` | ✅ | Teleport to a player warp |
| `/setpwarp` | `/setpwarp <name>` | `neoessentials.pwarp.set` | ✅ | Create your own player warp |
| `/delpwarp` | `/delpwarp <name>` | `neoessentials.pwarp.delete` | ✅ | Delete one of your player warps |
| `/pwarps` | `/pwarps` | `neoessentials.pwarp.list` | ✅ | List your player warps |

---

## Spawn

| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/spawn` | `/spawn` | `neoessentials.spawn` | ✅ | Teleport to server spawn |
| `/setspawn` | `/setspawn` | `neoessentials.spawn.set` | 🔒 | Set the server spawn at your location |

---

## Player State & Admin Tools

| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/fly` | `/fly [on\|off]` or `/fly <player> [on\|off]` | `neoessentials.fly` / `.fly.others` | 🔒 | Toggle flight mode |
| `/god` | `/god [on\|off]` or `/god <player> [on\|off]` | `neoessentials.god` / `.god.others` | 🔒 | Toggle invincibility (god mode) |
| `/heal` | `/heal [player]` | `neoessentials.heal` / `.heal.others` | 🔒 | Restore full health, hunger, saturation, clear effects |
| `/feed` | `/feed [player]` | `neoessentials.feed` / `.feed.others` | 🔒 | Restore full hunger and saturation |
| `/speed` | `/speed [walk\|fly] <0-10> [player]` | `neoessentials.speed` / `.speed.others` | 🔒 | Set walk or fly speed (0–10 scale) |
| `/ext` | `/ext [player]` | `neoessentials.ext` / `.ext.others` | ✅ (self) 🔒 (others) | Extinguish fire on a player |
| `/extinguish` | alias for `/ext` | same | ✅ | Alias |
| `/burn` | `/burn <player> [seconds]` | `neoessentials.burn` | 🔒 | Set a player on fire (default 10s) |
| `/give` | `/give <player> <item> [amount]` | `neoessentials.give` | 🔒 | Give items to a player |
| `/more` | `/more [amount]` | `neoessentials.more` | 🔒 | Fill held item stack to max (or set amount) |
| `/hat` | `/hat` | `neoessentials.hat` | 🔒 | Wear held item as helmet |
| `/exp` | `/exp [show\|set\|give] [amount] [player]` | `neoessentials.exp` + sub-nodes | ✅ (show) 🔒 (set/give) | Manage player experience |
| `/xp` | alias for `/exp` | same | ✅ | Alias |
| `/sudo` | `/sudo <player> <command>` | `neoessentials.sudo` | 🔒 | Execute a command as another player |
| `/playtime` | `/playtime [player]` | `neoessentials.playtime` / `.playtime.others` | ✅ | View how long a player has played |

---

## Server Admin

| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/broadcast` | `/broadcast <message>` | `neoessentials.broadcast` | 🔒 | Broadcast a coloured message to all players |
| `/bc` | alias for `/broadcast` | same | 🔒 | Alias |
| `/announce` | alias for `/broadcast` | same | 🔒 | Alias |
| `/time` | `/time [set\|add] <value>` | `neoessentials.time` / `.time.set` | 🔒 | Get or set world time (names: day/noon/night/midnight etc.) |
| `/day` | `/day` | `neoessentials.time.set` | 🔒 | Set time to day (1000 ticks) |
| `/night` | `/night` | `neoessentials.time.set` | 🔒 | Set time to night (13000 ticks) |
| `/weather` | `/weather <sun\|storm\|thunder> [seconds]` | `neoessentials.weather` | 🔒 | Set world weather |
| `/sun` | `/sun` | `neoessentials.weather` | 🔒 | Set weather to clear |
| `/storm` | `/storm` | `neoessentials.weather` | 🔒 | Set weather to rain/storm |
| `/thunder` | `/thunder` | `neoessentials.weather` | 🔒 | Set weather to thunderstorm |
| `/kill` | `/kill <player>` | `neoessentials.kill` | 🔒 | Kill a player (respects kill.exempt) |
| `/gamemode` | `/gamemode <survival\|creative\|adventure\|spectator\|0-3> [player]` | `neoessentials.gamemode` / `.gamemode.others` | 🔒 | Change player gamemode |
| `/gms` | `/gms [player]` | `neoessentials.gamemode` | 🔒 | Switch to Survival mode |
| `/gmc` | `/gmc [player]` | `neoessentials.gamemode` | 🔒 | Switch to Creative mode |
| `/gma` | `/gma [player]` | `neoessentials.gamemode` | 🔒 | Switch to Adventure mode |
| `/gmsp` | `/gmsp [player]` | `neoessentials.gamemode` | 🔒 | Switch to Spectator mode |

---

## Moderation

### Banning
| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/ban` | `/ban <player> [reason]` | `neoessentials.moderation.ban` | 🔒 | Permanently ban a player |
| `/tempban` | `/tempban <player> <duration> [reason]` | `neoessentials.moderation.tempban` | 🔒 | Temporarily ban a player (e.g. `1h`, `7d`) |
| `/unban` | `/unban <player>` | `neoessentials.moderation.unban` | 🔒 | Unban a player |
| `/banip` | `/banip <player\|ip>` | `neoessentials.moderation.banip` | 🔒 | Ban a player's IP address |
| `/unbanip` | `/unbanip <ip>` | `neoessentials.moderation.unbanip` | 🔒 | Unban an IP address |
| `/banlist` | `/banlist [page]` | `neoessentials.moderation.banlist` | 🔒 | View all banned players |

### Kicking & Muting
| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/kick` | `/kick <player> [reason]` | `neoessentials.moderation.kick` | 🔒 | Kick a player from the server |
| `/kickall` | `/kickall [reason]` | `neoessentials.moderation.kickall` | 🔒 | Kick all online players |
| `/mute` | `/mute <player> [duration] [reason]` | `neoessentials.moderation.mute` | 🔒 | Mute a player |
| `/unmute` | `/unmute <player>` | `neoessentials.moderation.unmute` | 🔒 | Unmute a player |
| `/mutelist` | `/mutelist` | `neoessentials.moderation.mutelist` | 🔒 | List all muted players |

### Jail
| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/jail` | `/jail <player> [jail] [reason]` | `neoessentials.moderation.jail` | 🔒 | Jail a player indefinitely |
| `/jailfor` | `/jailfor <player> <duration> [jail] [reason]` | `neoessentials.moderation.jail` | 🔒 | Jail a player for a duration |
| `/unjail` | `/unjail <player>` | `neoessentials.moderation.unjail` | 🔒 | Release a player from jail |
| `/setjail` | `/setjail <name>` | `neoessentials.moderation.setjail` | 🔒 | Create a jail at current location |
| `/deljail` | `/deljail <name>` | `neoessentials.moderation.deljail` | 🔒 | Delete a jail location |
| `/jaillist` | `/jaillist` | `neoessentials.moderation.jaillist` | 🔒 | List all jail locations and jailed players |

### Freeze & Vanish
| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/freeze` | `/freeze <player>` | `neoessentials.moderation.freeze` | 🔒 | Freeze a player in place |
| `/unfreeze` | `/unfreeze <player>` | `neoessentials.moderation.unfreeze` | 🔒 | Unfreeze a player |
| `/freezeall` | `/freezeall` | `neoessentials.moderation.freeze` | 🔒 | Freeze all online players |
| `/unfreezeall` | `/unfreezeall` | `neoessentials.moderation.unfreeze` | 🔒 | Unfreeze all players |
| `/freezelist` | `/freezelist` | `neoessentials.moderation.freezelist` | 🔒 | List all frozen players |
| `/vanish` | `/vanish [on\|off]` | `neoessentials.moderation.vanish` | 🔒 | Toggle vanish mode (invisible to other players) |
| `/v` | alias for `/vanish` | same | 🔒 | Alias |
| `/unvanish` | `/unvanish` | `neoessentials.moderation.vanish` | 🔒 | Disable vanish mode |
| `/vanishlist` | `/vanishlist` | `neoessentials.moderation.vanish` | 🔒 | List all vanished players |

---

## Chat & Messaging

| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/msg` | `/msg <player> <message>` | `neoessentials.chat.msg` | ✅ | Send a private message to a player |
| `/tell` | alias for `/msg` | same | ✅ | Alias |
| `/whisper` | alias for `/msg` | same | ✅ | Alias |
| `/message` | alias for `/msg` | same | ✅ | Alias |
| `/w` | alias for `/msg` | same | ✅ | Alias |
| `/reply` | `/reply <message>` | `neoessentials.chat.reply` | ✅ | Reply to the last private message received |
| `/r` | alias for `/reply` | same | ✅ | Alias |
| `/msgtoggle` | `/msgtoggle` | `neoessentials.chat.msgtoggle` | ✅ | Toggle receiving private messages |
| `/socialspy` | `/socialspy [on\|off]` | `neoessentials.chat.socialspy` | 🔒 | See all private messages between players |
| `/ignore` | `/ignore <player>` | `neoessentials.chat.ignore` | ✅ | Ignore a player's messages |
| `/unignore` | `/unignore <player>` | `neoessentials.chat.unignore` | ✅ | Stop ignoring a player |
| `/mail` | `/mail send\|read\|clear\|sendall [args]` | `neoessentials.mail` | ✅ | In-game mail system |
| `/helpop` | `/helpop <message>` | `neoessentials.helpop` | ✅ | Send a help request to all online staff |
| `/ac` | alias for `/helpop` | same | ✅ | Alias |
| `/amsg` | alias for `/helpop` | same | ✅ | Alias |

---

## Kits

| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/kit` | `/kit [name] [player]` | `neoessentials.kit` | ✅ | Claim a kit (respects cooldown) |
| `/kits` | `/kits [page]` | `neoessentials.kit.list` | ✅ | List all available kits |
| `/listkits` | alias for `/kits` | same | ✅ | Alias |
| `/createkit` | `/createkit <name> [cooldown]` | `neoessentials.kit.create` | 🔒 | Create a kit from current inventory |
| `/delkit` | `/delkit <name>` | `neoessentials.kit.delete` | 🔒 | Delete a kit |
| `/kitreset` | `/kitreset <kit> [player]` | `neoessentials.kit.reset` | 🔒 | Reset a player's kit cooldown |

---

## Items

| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/repair` | `/repair [all]` | `neoessentials.item.repair` | 🔒 | Repair held item (or all items with `all`) |
| `/fix` | alias for `/repair` | same | 🔒 | Alias |
| `/enchant` | `/enchant <enchantment> [level]` | `neoessentials.item.enchant` | 🔒 | Enchant held item |
| `/dispose` | `/dispose` | `neoessentials.item.dispose` | ✅ | Open an item disposal chest |
| `/trash` | alias for `/dispose` | same | ✅ | Alias |
| `/clearinventory` | `/clearinventory [player]` | `neoessentials.item.clearinventory` | 🔒 | Clear a player's inventory |
| `/ci` | alias for `/clearinventory` | same | 🔒 | Alias |
| `/clear` | alias for `/clearinventory` | same | 🔒 | Alias |
| `/powertool` | `/powertool <command>` or `/powertool clear` | `neoessentials.item.powertool` | 🔒 | Bind a command to held item |
| `/pt` | alias for `/powertool` | same | 🔒 | Alias (also alias for paytoggle — use with care) |

> **⚠️ Powertool command filter** — by default, commands containing relative coordinates (`~`), `@` selectors, `{...}` NBT, or shell-like characters are blocked.  
> Set `allowUnsafeCommands: true` in `security.json` (or `config.json → security`) and run `/neoe reload` to unlock all patterns.  
> See [Security Configuration](SplitConfigs#security-configuration-securityjson) for the full list of blocked patterns and examples.

| `/invsee` | `/invsee <player>` | `neoessentials.item.invsee` | 🔒 | View another player's inventory (read-only) |
| `/inv` | alias for `/invsee` | same | 🔒 | Alias |
| `/invseeedit` | `/invseeedit <player>` | `neoessentials.item.invsee.edit` | 🔒 | View and edit another player's inventory |
| `/enderchest` | `/enderchest <player>` | `neoessentials.item.enderchest` | 🔒 | View another player's ender chest |
| `/ec` | alias for `/enderchest` | same | 🔒 | Alias |
| `/enderchestedit` | `/enderchestedit <player>` | `neoessentials.item.enderchest.edit` | 🔒 | View and edit another player's ender chest |
| `/ecedit` | alias for `/enderchestedit` | same | 🔒 | Alias |

### Portable Workstations
| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/anvil` | `/anvil` | `neoessentials.item.anvil` | 🔒 | Open portable anvil |
| `/workbench` | `/workbench` | `neoessentials.item.workbench` | 🔒 | Open portable crafting table |
| `/crafting` | alias for `/workbench` | same | 🔒 | Alias |
| `/craft` | alias for `/workbench` | same | 🔒 | Alias |
| `/grindstone` | `/grindstone` | `neoessentials.item.grindstone` | 🔒 | Open portable grindstone |
| `/smithing` | `/smithing` | `neoessentials.item.smithing` | 🔒 | Open portable smithing table |
| `/loom` | `/loom` | `neoessentials.loom` | 🔒 | Open portable loom |
| `/cartography` | `/cartography` | `neoessentials.cartography` | 🔒 | Open portable cartography table |
| `/cartographytable` | alias for `/cartography` | same | 🔒 | Alias |

---

## Worth & Sell

| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/worth` | `/worth [item\|hand] [amount]` | `neoessentials.worth` | ✅ | Check the sell value of an item |
| `/sell` | `/sell hand\|inventory\|all\|<item> [amount]` | `neoessentials.sell` | ✅ | Sell items for money |
| `/setworth` | `/setworth <item\|hand> <price\|remove>` | `neoessentials.setworth` | 🔒 | Set or remove an item's sell price |

---

## Utility

### Per-Player Time & Weather
| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/ptime` | `/ptime [reset\|day\|noon\|night\|midnight\|<ticks>] [player]` | `neoessentials.ptime` / `.ptime.others` | 🔒 | Set a client-side time override for a player |
| `/pweather` | `/pweather [reset\|sun\|clear\|storm\|rain] [player]` | `neoessentials.pweather` / `.pweather.others` | 🔒 | Set a client-side weather override for a player |

### Effects & Entities
| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/effect` | `/effect <player> <effect\|clear> [duration] [amplifier]` | `neoessentials.effect` | 🔒 | Apply or clear potion effects on a player |
| `/spawnmob` | `/spawnmob <mob> [amount] [player]` | `neoessentials.spawnmob` / `.spawnmob.others` | 🔒 | Spawn entities at a player's location |
| `/mob` | alias for `/spawnmob` | same | 🔒 | Alias |

### Item Utilities
| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/unlimited` | `/unlimited [list\|clear\|<item\|hand>] [player]` | `neoessentials.unlimited` / `.unlimited.others` | 🔒 | Toggle infinite item use for a player |
| `/condense` | `/condense [item]` | `neoessentials.condense` | 🔒 | Compress loose items into storage blocks |

---

## Item Customisation & Miscellaneous Commands

| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/me` | `/me <action>` | `neoessentials.me` | ✅ | Broadcast an action message to all players |
| `/tptoggle` | `/tptoggle [on\|off] [player]` | `neoessentials.tptoggle` / `.tptoggle.others` | ✅ | Toggle teleport request acceptance |
| `/gc` | `/gc` | `neoessentials.gc` | 🔒 | Show server TPS, memory, uptime, loaded chunks |
| `/mem` | alias for `/gc` | same | 🔒 | Alias |
| `/lightning` | `/lightning [player]` | `neoessentials.lightning` / `.lightning.others` | 🔒 | Strike lightning at look target or player |
| `/smite` | alias for `/lightning` | same | 🔒 | Alias |
| `/skull` | `/skull [player]` | `neoessentials.skull` | 🔒 | Get a player head item |
| `/itemname` | `/itemname [name\|-]` | `neoessentials.itemname` | 🔒 | Rename held item (omit or use `-` to clear) |
| `/rename` | alias for `/itemname` | same | 🔒 | Alias |
| `/itemlore` | `/itemlore add\|set\|remove\|clear [args]` | `neoessentials.itemlore` | 🔒 | Add/set/remove/clear held item lore lines |
| `/remove` | `/remove <type> [radius]` | `neoessentials.remove` | 🔒 | Remove entities in radius (types: all, items, mobs, animals, monsters, arrows, xp, boats, minecarts, tnt, paintings) |

---

## Player Info & Admin Tools

| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/seen` | `/seen <player>` | `neoessentials.seen` | ✅ | Show if a player is online with location/ping, or offline |
| `/near` | `/near [radius]` | `neoessentials.near` | ✅ | List players within radius (default 200 blocks) with distance |
| `/ping` | `/ping [player]` | `neoessentials.ping` / `.ping.others` | ✅ | Show network latency in ms. Green <80ms, yellow <200ms, red otherwise |
| `/playtime` | `/playtime [player]` | `neoessentials.playtime` / `.playtime.others` | ✅ | Show total server play time (hours/minutes/seconds) from vanilla stats |
| `/whois` | `/whois <player>` | `neoessentials.whois` | 🔒 | Show UUID, world, coordinates, gamemode, ping, health and food level |
| `/realname` | `/realname <nickname>` | `neoessentials.realname` | ✅ | Find the real username of a player by their display name/nickname |
| `/sudo` | `/sudo <player> <command>` | `neoessentials.sudo` | 🔒 | Force a player to run a command. Prefix `c:` to send chat. Respects `neoessentials.sudo.exempt` |
| `/suicide` | `/suicide` | `neoessentials.suicide` | ✅ | Kill yourself. Broadcasts death message to all online players |
| `/msgtoggle` | `/msgtoggle [on\|off] [player]` | `neoessentials.msgtoggle` / `.msgtoggle.others` | ✅ | Block or allow incoming private messages. Synced with `MsgToggleManager` |
| `/rtoggle` | `/rtoggle [on\|off] [player]` | `neoessentials.rtoggle` / `.rtoggle.others` | ✅ | Toggle whether `/r` replies to the last sender (default on) |
| `/motd` | `/motd` | `neoessentials.motd` | ✅ | Show the active message of the day |
| `/motd set` | `/motd set <message>` | `neoessentials.motd.set` | 🔒 | Set the active profile's MOTD text |
| `/motd clear` | `/motd clear` | `neoessentials.motd.set` | 🔒 | Clear the active profile's MOTD |
| `/motd reload` | `/motd reload` | `neoessentials.motd.reload` | 🔒 | Reload all profiles from disk |
| `/motd broadcast` | `/motd broadcast` | `neoessentials.motd.broadcast` | 🔒 | Broadcast active MOTD to all online players |
| `/motd profile list` | `/motd profile list` | `neoessentials.motd.profile` | 🔒 | List all profiles |
| `/motd profile create` | `/motd profile create <name> <message>` | `neoessentials.motd.profile` | 🔒 | Create or overwrite a profile |
| `/motd profile delete` | `/motd profile delete <name>` | `neoessentials.motd.profile` | 🔒 | Delete a profile |
| `/motd profile switch` | `/motd profile switch <name>` | `neoessentials.motd.profile` | 🔒 | Switch the active profile |
| `/motd profile info` | `/motd profile info [name]` | `neoessentials.motd.profile` | 🔒 | Show profile details |
| `/motd rotation enable` | `/motd rotation enable <minutes>` | `neoessentials.motd.rotation` | 🔒 | Enable auto-rotation |
| `/motd rotation disable` | `/motd rotation disable` | `neoessentials.motd.rotation` | 🔒 | Disable auto-rotation |
| `/motd rotation next` | `/motd rotation next` | `neoessentials.motd.rotation` | 🔒 | Rotate to next profile immediately |
| `/rules` | `/rules` | `neoessentials.rules` | ✅ | Show server rules (configured in `config.json` → `general.rules`) |

---

## World Interaction & Fun Commands

| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/fireball` | `/fireball [type] [speed] [ride]` | `neoessentials.fireball.<type>` | 🔒 | Shoot a projectile. Types: fireball, small, large, arrow, skull, egg, snowball, expbottle, dragon, trident, windcharge |
| `/tree` | `/tree <type>` | `neoessentials.tree` | 🔒 | Grow a tree at look target. Types: oak, birch, spruce, jungle, acacia, darkoak, mangrove, cherry, azalea, bigoak, mega_spruce, mega_jungle |
| `/bigtree` | `/bigtree` | `neoessentials.tree` | 🔒 | Grow a large oak tree (alias for `/tree bigoak`) |
| `/break` | `/break` | `neoessentials.break` | 🔒 | Instantly break the looked-at block (no drops). Bedrock requires `neoessentials.break.bedrock` |
| `/ice` | `/ice [player]` | `neoessentials.ice` / `.ice.others` | 🔒 | Freeze a player solid using powder snow freeze ticks |
| `/bottom` | `/bottom` | `neoessentials.bottom` | 🔒 | Teleport to the lowest safe position at your current XZ coordinates |
| `/tpaall` | `/tpaall [player]` | `neoessentials.tpaall` / `.tpaall.others` | 🔒 | Send a tpa-here request to every online player (respects tptoggle) |
| `/broadcastworld` | `/broadcastworld <message>` | `neoessentials.broadcastworld` | 🔒 | Broadcast a coloured message to all players in the sender's current world |
| `/bcastworld` | alias for `/broadcastworld` | same | 🔒 | Alias |

---

## AFK

| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/afk` | `/afk [message]` | `neoessentials.afk` | ✅ | Toggle AFK status with optional message |
| `/away` | alias for `/afk` | same | ✅ | Alias |

---

## Web Dashboard

| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/dashboard` | `/dashboard start\|stop\|restart\|status\|info` | `neoessentials.dashboard` | 🔒 | Manage the web dashboard server |
| `/dashboardregister` | `/dashboardregister [username] [password]` | `neoessentials.dashboard.register` | ✅ (if permitted) | Register a web dashboard account in-game |

---

## Permissions Management

| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/permissions` | `/permissions <user\|group> <action> [args]` | `neoessentials.permissions` | 🔒 | Manage user and group permissions |
| `/pex` | alias for `/permissions` | same | 🔒 | Alias |
| `/permissions debug` | `/permissions debug <player>` | `neoessentials.permissions.debug` | 🔒 | Full permission resolution trace for a player (see below) |
| `/permissions group … setpriority` | `/permissions group <name> setpriority <value>` | `neoessentials.permissions.group.modify` | 🔒 | Set group priority (−999 to 999; higher = checked first in inheritance) |
| `/permissions group … getpriority` | `/permissions group <name> getpriority` | `neoessentials.permissions.info.group` | 🔒 | Read the current priority of a group |

> **`/permissions debug <player>`**  
> Prints a complete in-game diagnostic for the named player without requiring debug logging:
> - System mode (Internal · External adapter · EMERGENCY)
> - Adapter health, detected version, consecutive failures
> - `opsBypassPermissions` and `vanillaOpFallback` on/off
> - OP status (works for offline players via profile cache)
> - Assigned group, direct user permission nodes
> - Group inheritance chain (recursive, indented)
> - Numbered resolution summary — shows which step would **GRANT** or **continue** for this player based on current config
>
> Useful when players report "I have the group but still can't use the command" — diagnose everything from in-game chat.

---

## Miscellaneous

### Information
| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/whois` | `/whois <player>` | `neoessentials.whois` | 🔒 | View detailed info about a player |
| `/info` | alias for `/whois` | same | 🔒 | Alias |
| `/seen` | `/seen <player>` | `neoessentials.seen` | ✅ | Check when a player was last online |
| `/list` | `/list` | `neoessentials.list` | ✅ | List all online players |
| `/who` | alias for `/list` | same | ✅ | Alias |
| `/online` | alias for `/list` | same | ✅ | Alias |
| `/near` | `/near [radius]` | `neoessentials.near` | ✅ | Show nearby players |
| `/nearby` | alias for `/near` | same | ✅ | Alias |
| `/ping` | `/ping [player]` | `neoessentials.ping` | ✅ | Check your ping (or another player's) |
| `/pong` | alias for `/ping` | same | ✅ | Alias |
| `/playtime` | `/playtime [player]` | `neoessentials.playtime` | ✅ | Check a player's total play time |
| `/getpos` | `/getpos [player]` | `neoessentials.getpos` | ✅ | Show your current coordinates |
| `/coords` | alias for `/getpos` | same | ✅ | Alias |
| `/whereami` | alias for `/getpos` | same | ✅ | Alias |
| `/compass` | `/compass` | `neoessentials.compass` | ✅ | Show your current facing direction |
| `/direction` | alias for `/compass` | same | ✅ | Alias |
| `/depth` | `/depth` | `neoessentials.depth` | ✅ | Show your current depth (Y level relative to sea level) |
| `/motd` | `/motd [set\|clear\|reload\|broadcast\|profile\|rotation]` | `neoessentials.motd` | ✅ | View / manage the server message of the day (see full table above) |
| `/rules` | `/rules` | `neoessentials.rules` | ✅ | View the server rules |

### Player Actions
| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/nick` | `/nick <nickname\|off>` | `neoessentials.nick` | 🔒 | Set your display nickname |
| `/nickname` | alias for `/nick` | same | 🔒 | Alias |
| `/realname` | `/realname <nickname>` | `neoessentials.realname` | ✅ | Find a player's real name from their nickname |
| `/suicide` | `/suicide` | `neoessentials.suicide` | ✅ | Kill yourself |
| `/killme` | alias for `/suicide` | same | ✅ | Alias |
| `/sign` | `/sign <line> <text>` | `neoessentials.sign` | 🔒 | Edit sign text |
| `/book` | `/book` | `neoessentials.book` | 🔒 | Edit or unsign a written book |
| `/language` | `/language [code]` | `neoessentials.language` | 🔒 | View or switch the server language |
| `/world` | `/world [name] [player]` | `neoessentials.world` / `.world.others` | 🔒 | Teleport to a world/dimension (lists worlds if no arg) |
| `/spawner` | `/spawner <mob>` | `neoessentials.spawner` | 🔒 | Change the looked-at mob spawner type |
| `/recipe` | `/recipe [item]` | `neoessentials.recipe` | ✅ | Unlock and show crafting recipe for held or named item |
| `/tpauto` | `/tpauto [on\|off] [player]` | `neoessentials.tpauto` / `.tpauto.others` | ✅ | Auto-accept all incoming teleport requests |

---

## 📊 Command Count Summary

| System | Commands (incl. aliases) |
|---|---|
| Economy | 8 |
| Teleportation | 17 |
| Homes | 5 |
| Warps | 8 |
| Spawn | 2 |
| Player State & Admin Tools | 15 |
| Server Admin | 16 |
| Moderation | 23 |
| Chat & Messaging | 13 |
| Kits | 6 |
| Items (incl. workstations) | 21 |
| Worth & Sell | 3 |
| Utility | 7 |
| AFK | 2 |
| Web Dashboard | 2 |
| Permissions Management | 2 |
| Miscellaneous | 22 |
| **Total** | **~172** |

---

## ⚙️ Configuration

All commands can be individually enabled or disabled in `config.json` under the `commands` section:

```json
{
  "commands": {
    "fly": true,
    "god": true,
    "heal": true,
    "sell": true
  }
}
```

Economy-related settings (currency symbol, sell multiplier, etc.) are under the `economy` section.  
Teleportation settings (delays, safe teleport, random teleport) are under `teleportation`.  
Web dashboard settings are under `webDashboard`.

---

## Mod Root Commands

Commands for managing NeoEssentials itself — reload, split config management, and routing.

| Command | Syntax | Permission | Default | Description |
|---|---|---|---|---|
| `/neoe` | `/neoe` | `neoessentials.use` | ✅ | Show list of available NeoEssentials commands |
| `/neoe reload` | `/neoe reload` | `neoessentials.admin.reload` | 🔒 | Reload all configs and live systems (chat, kits, homes, tablist, etc.) |
| `/neoe config split` | `/neoe config split` | `neoessentials.admin.reload` | 🔒 | Migrate monolithic config.json to split files |
| `/neoe config status` | `/neoe config status` | `neoessentials.admin.reload` | 🔒 | Show ✔/✘ status of every split config file |
| `/neoe config validate` | `/neoe config validate` | `neoessentials.admin.reload` | 🔒 | Check all split config files for problems |
| `/neoe config repair` | `/neoe config repair` | `neoessentials.admin.reload` | 🔒 | Auto-regenerate missing/incomplete split config files |
| `/neoessentials` | alias | same | ✅ | Alias for `/neoe` |

> See [Split Config System](SplitConfigs.md) for the full split config documentation.

---

*See [PermissionSystem.md](PermissionSystem.md) for the full permissions reference.*  
*See [EconomySystem.md](EconomySystem.md), [TeleportationSystem.md](TeleportationSystem.md), etc. for system-specific documentation.*

