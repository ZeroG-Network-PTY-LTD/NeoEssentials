# NeoEssentials Commands

This document provides a comprehensive list of all commands available in NeoEssentials, organized by category. Each command includes its syntax, available aliases, required permission node, and a description of its function.

## Table of Contents

- [Administrative Commands](#administrative-commands)
- [Moderation Commands](#moderation-commands)
- [Teleportation Commands](#teleportation-commands)
- [Economy Commands](#economy-commands)
- [Home Commands](#home-commands)
- [Warp Commands](#warp-commands)
- [Player Commands](#player-commands)
- [Jail Commands](#jail-commands)
- [Utility Commands](#utility-commands)
- [Item Commands](#item-commands)
- [Chat & Messaging Commands](#chat--messaging-commands)
- [Time & Weather Commands](#time--weather-commands)

## Administrative Commands

### /adminpanel
**Aliases**: `/admin`, `/ap`  
**Permission**: `neoessentials.admin.panel`  
**Description**: Opens the main admin panel interface for server management.

**Subcommands**:
- `/adminpanel section <section>` - Navigate to a specific section of the admin panel
- `/adminpanel reload` - Reload NeoEssentials configuration
- `/adminpanel manage <section>` - Access management controls
- `/adminpanel toggle <feature> <enabled>` - Toggle mod features on/off
- `/adminpanel performance <section>` - View server performance metrics
- `/adminpanel server <action>` - Control server settings
- `/adminpanel optimize` - Run server optimization tasks

### /maintenance
**Permission**: `neoessentials.admin.maintenance`  
**Description**: Toggle server maintenance mode.

**Syntax**: `/maintenance <on|off>`

### /reload
**Permission**: `neoessentials.admin.reload`  
**Description**: Reload the NeoEssentials configuration.

## Moderation Commands

### /ban
**Permission**: `neoessentials.mod.ban`  
**Description**: Ban a player from the server.

**Syntax**: 
- `/ban <player> [reason]` - Permanently ban a player
- `/tempban <player> <time> [reason]` - Temporarily ban a player for a specified duration

### /unban
**Aliases**: `/pardon`  
**Permission**: `neoessentials.mod.unban`  
**Description**: Unban a player from the server.

**Syntax**: `/unban <player>`

### /banip
**Permission**: `neoessentials.mod.banip`  
**Description**: Ban an IP address from the server.

**Syntax**: `/banip <address> [reason]`

### /unbanip
**Aliases**: `/pardonip`  
**Permission**: `neoessentials.mod.unbanip`  
**Description**: Unban an IP address from the server.

**Syntax**: `/unbanip <address>`

### /kick
**Permission**: `neoessentials.mod.kick`  
**Description**: Kick a player from the server.

**Syntax**: `/kick <player> [reason]`

### /mute
**Permission**: `neoessentials.mod.mute`  
**Description**: Prevent a player from chatting.

**Syntax**: 
- `/mute <player> [reason]` - Permanently mute a player
- `/tempmute <player> <time> [reason]` - Temporarily mute a player for a specified duration

### /unmute
**Permission**: `neoessentials.mod.unmute`  
**Description**: Unmute a player.

**Syntax**: `/unmute <player>`

### /vanish
**Aliases**: `/v`  
**Permission**: `neoessentials.mod.vanish`  
**Description**: Make yourself invisible to other players.

**Syntax**: `/vanish [on|off]`

## Teleportation Commands

### /tp
**Aliases**: `/teleport`  
**Permission**: `neoessentials.teleport`  
**Description**: Teleport yourself or others.

**Syntax**:
- `/tp <player>` - Teleport to a player
- `/tp <player1> <player2>` - Teleport player1 to player2
- `/tp <x> <y> <z>` - Teleport to coordinates

### /tpa
**Permission**: `neoessentials.tpa`  
**Description**: Request to teleport to another player.

**Syntax**: `/tpa <player>`

### /tpaccept
**Aliases**: `/tpyes`  
**Permission**: `neoessentials.tpaccept`  
**Description**: Accept a teleport request.

**Syntax**: `/tpaccept [player]`

### /tpdeny
**Aliases**: `/tpno`  
**Permission**: `neoessentials.tpdeny`  
**Description**: Deny a teleport request.

**Syntax**: `/tpdeny [player]`

### /back
**Permission**: `neoessentials.back`  
**Description**: Teleport to your previous location.

**Syntax**: `/back`

### /tphere
**Aliases**: `/tph`  
**Permission**: `neoessentials.tphere`  
**Description**: Teleport another player to you.

**Syntax**: `/tphere <player>`

### /tppos
**Permission**: `neoessentials.tppos`  
**Description**: Teleport to specific coordinates.

**Syntax**: `/tppos <x> <y> <z> [dimension]`

## Economy Commands

### /balance
**Aliases**: `/bal`, `/money`  
**Permission**: `neoessentials.economy.balance`  
**Description**: Check your balance or another player's balance.

**Syntax**:
- `/balance` - Check your balance
- `/balance <player>` - Check another player's balance

### /pay
**Permission**: `neoessentials.economy.pay`  
**Description**: Pay another player from your balance.

**Syntax**: `/pay <player> <amount> [memo]`

### /eco
**Permission**: `neoessentials.economy.admin`  
**Description**: Admin economy commands.

**Syntax**:
- `/eco give <player> <amount>` - Give money to a player
- `/eco take <player> <amount>` - Take money from a player
- `/eco set <player> <amount>` - Set a player's balance
- `/eco reset <player>` - Reset a player's balance
- `/eco top` - View top balances

## Home Commands

### /home
**Permission**: `neoessentials.home`  
**Description**: Teleport to your home or list your homes.

**Syntax**:
- `/home` - Teleport to your default home
- `/home <name>` - Teleport to a specific home
- `/home list` - List all your homes

### /sethome
**Permission**: `neoessentials.sethome`  
**Description**: Set a home at your current location.

**Syntax**:
- `/sethome` - Set your default home
- `/sethome <name>` - Set a named home

### /delhome
**Aliases**: `/rmhome`  
**Permission**: `neoessentials.delhome`  
**Description**: Delete one of your homes.

**Syntax**: `/delhome <name>`

### /homelist
**Permission**: `neoessentials.homelist`  
**Description**: List all your homes.

**Syntax**: `/homelist`

## Warp Commands

### /warp
**Permission**: `neoessentials.warp`  
**Description**: Teleport to a server warp point.

**Syntax**:
- `/warp <name>` - Teleport to a warp
- `/warp list` - List all available warps

### /setwarp
**Permission**: `neoessentials.setwarp`  
**Description**: Create a new warp at your current location.

**Syntax**: `/setwarp <name>`

### /delwarp
**Aliases**: `/rmwarp`  
**Permission**: `neoessentials.delwarp`  
**Description**: Delete a warp point.

**Syntax**: `/delwarp <name>`

### /warplist
**Permission**: `neoessentials.warplist`  
**Description**: List all available warps.

**Syntax**: `/warplist`

## Player Commands

### /afk
**Permission**: `neoessentials.afk`  
**Description**: Mark yourself as AFK (Away From Keyboard).

**Syntax**: `/afk [message]`

### /heal
**Permission**: `neoessentials.heal`  
**Description**: Heal yourself or another player.

**Syntax**:
- `/heal` - Heal yourself
- `/heal <player>` - Heal another player

### /feed
**Aliases**: `/eat`  
**Permission**: `neoessentials.feed`  
**Description**: Fill your hunger bar or another player's.

**Syntax**:
- `/feed` - Feed yourself
- `/feed <player>` - Feed another player

### /god
**Permission**: `neoessentials.god`  
**Description**: Toggle invulnerability for yourself or another player.

**Syntax**:
- `/god` - Toggle god mode for yourself
- `/god <player>` - Toggle god mode for another player

### /fly
**Permission**: `neoessentials.fly`  
**Description**: Toggle flight for yourself or another player.

**Syntax**:
- `/fly` - Toggle flight for yourself
- `/fly <player>` - Toggle flight for another player

### /speed
**Permission**: `neoessentials.speed`  
**Description**: Adjust your walking or flying speed.

**Syntax**:
- `/speed <value>` - Set your current movement speed
- `/speed <walk|fly> <value>` - Set specific movement speed
- `/speed <walk|fly> <value> <player>` - Set another player's movement speed

### /nick
**Permission**: `neoessentials.nick`  
**Description**: Change your display name.

**Syntax**: `/nick <nickname|off>`

## Jail Commands

### /jail
**Permission**: `neoessentials.jail`  
**Description**: Send a player to jail.

**Syntax**:
- `/jail <player> <jail>` - Jail a player indefinitely
- `/jail <player> <jail> <time>` - Jail a player for a specific duration

### /unjail
**Permission**: `neoessentials.unjail`  
**Description**: Release a player from jail.

**Syntax**: `/unjail <player>`

### /setjail
**Aliases**: `/createjail`  
**Permission**: `neoessentials.setjail`  
**Description**: Create a new jail at your current location.

**Syntax**: `/setjail <name>`

### /deljail
**Aliases**: `/rmjail`, `/remjail`  
**Permission**: `neoessentials.deljail`  
**Description**: Remove an existing jail.

**Syntax**: `/deljail <name>`

### /jails
**Permission**: `neoessentials.jails`  
**Description**: List all available jails.

**Syntax**: `/jails`

## Utility Commands

### /kit
**Permission**: `neoessentials.kit`  
**Description**: Receive a predefined kit of items.

**Syntax**:
- `/kit <name>` - Receive a kit
- `/kit list` - List available kits

### /powertool
**Aliases**: `/pt`  
**Permission**: `neoessentials.powertool`  
**Description**: Bind a command to the currently held item.

**Syntax**:
- `/powertool <command>` - Bind a command to held item
- `/powertool -c` - Clear the current powertool
- `/powertool -a` - List all your powertools
- `/powertool -r` - Clear all your powertool bindings
- `/powertool -e <true|false>` - Enable/disable powertools
- `/powertool -t` - Toggle powertools on/off

### /jump
**Aliases**: `/j`  
**Permission**: `neoessentials.jump`  
**Description**: Jump to the block you're looking at.

**Syntax**: `/jump`

### /top
**Permission**: `neoessentials.top`  
**Description**: Teleport to the highest block above your position.

**Syntax**: `/top`

### /hat
**Permission**: `neoessentials.hat`  
**Description**: Put the item in your hand on your head.

**Syntax**: `/hat`

## Item Commands

### /give
**Permission**: `neoessentials.give`  
**Description**: Give items to yourself or another player.

**Syntax**:
- `/give <item> [amount]` - Give yourself an item
- `/give <player> <item> [amount]` - Give another player an item

### /repair
**Permission**: `neoessentials.repair`  
**Description**: Repair the item in your hand or equipped items.

**Syntax**:
- `/repair` - Repair the item in your hand
- `/repair all` - Repair all equipped items

### /clearinventory
**Aliases**: `/ci`, `/clear`  
**Permission**: `neoessentials.clearinventory`  
**Description**: Clear your inventory or another player's inventory.

**Syntax**:
- `/clearinventory` - Clear your inventory
- `/clearinventory <player>` - Clear another player's inventory

### /enderchest
**Aliases**: `/echest`, `/ec`  
**Permission**: `neoessentials.enderchest`  
**Description**: Open your enderchest or another player's enderchest.

**Syntax**:
- `/enderchest` - Open your enderchest
- `/enderchest <player>` - Open another player's enderchest

### /skull
**Permission**: `neoessentials.skull`  
**Description**: Get a player head.

**Syntax**: `/skull <player>`

## Chat & Messaging Commands

### /msg
**Aliases**: `/tell`, `/w`, `/whisper`  
**Permission**: `neoessentials.msg`  
**Description**: Send a private message to another player.

**Syntax**: `/msg <player> <message>`

### /reply
**Aliases**: `/r`  
**Permission**: `neoessentials.reply`  
**Description**: Reply to the last player who messaged you.

**Syntax**: `/reply <message>`

### /mail
**Permission**: `neoessentials.mail`  
**Description**: Send and receive offline messages.

**Syntax**:
- `/mail` - Show mail summary
- `/mail read` - Read all your messages
- `/mail read <index>` - Read a specific message
- `/mail send <player> <message>` - Send mail to a player
- `/mail clear` - Delete all messages
- `/mail delete <index>` - Delete a specific message

### /broadcast
**Aliases**: `/bc`  
**Permission**: `neoessentials.broadcast`  
**Description**: Send a message to all online players.

**Syntax**: `/broadcast <message>`

## Time & Weather Commands

### /time
**Permission**: `neoessentials.time`  
**Description**: Check or set the time in the world.

**Syntax**:
- `/time` - Check the current time
- `/time <day|night|noon|midnight|sunrise|sunset>` - Set time to preset
- `/time <ticks>` - Set time to specified ticks

### /weather
**Permission**: `neoessentials.weather`  
**Description**: Check or set the weather in the world.

**Syntax**:
- `/weather` - Check the current weather
- `/weather <clear|rain|thunder> [duration]` - Set the weather

### /sun
**Permission**: `neoessentials.sun`  
**Description**: Set the weather to clear.

**Syntax**: `/sun [duration]`

### /rain
**Permission**: `neoessentials.rain`  
**Description**: Set the weather to rain.

**Syntax**: `/rain [duration]`

### /storm
**Permission**: `neoessentials.storm`  
**Description**: Set the weather to thunder.

**Syntax**: `/storm [duration]`
