<<<<<<< HEAD
=======
# NeoEssentials

![NeoEssentials Logo](https://i.imgur.com/placeholder_for_logo.png)

> A comprehensive server-side essentials mod for Minecraft NeoForge servers, inspired by EssentialsX for Bukkit/Spigot.

[![Minecraft Versions](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)](https://minecraft.net)
[![NeoForge](https://img.shields.io/badge/Loader-NeoForge-blue)](https://neoforged.net)
[![GitHub Release](https://img.shields.io/github/v/release/zerog/neoessentials?include_prereleases&label=GitHub)](https://github.com/zerog/neoessentials/releases)
[![CurseForge](https://img.shields.io/badge/CurseForge-Download-orange)](https://curseforge.com)
[![Modrinth](https://img.shields.io/badge/Modrinth-Download-green)](https://modrinth.com)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](https://choosealicense.com/licenses/mit/)

## Overview

NeoEssentials is a powerful server-side utility mod designed to provide all essential commands and features needed to run a Minecraft server. As a purely server-side mod, players can join your server using either vanilla or modded clients without needing to install NeoEssentials themselves.

## Features

### 🏠 Teleportation
- **Homes**: Set multiple homes and teleport between them with `/home`, `/sethome`, and `/delhome`
- **Warps**: Create public teleportation points with `/warp`, `/setwarp`, and `/delwarp`
- **Teleport Requests**: Send and accept teleport requests with `/tpa`, `/tpaccept`, and `/tpdeny`
- **Back Command**: Return to your previous location after teleporting with `/back`

### 💰 Economy
- **Balance Management**: Check and manage player balances with `/balance` and `/eco`
- **Payments**: Transfer money between players with `/pay`
- **Server Shop Integration**: Ready integration with shop systems

### 🎮 User Interface
- **Enhanced Tablist**: Fully customizable tablist with headers, footers, and player sorting
- **Animated Displays**: Multiple animation types including rotation, scroll, fade, rainbow, typewriter, blink, and wave
- **Placeholder Support**: Extensive placeholder system for dynamic content including %uptime%, %time%, %player%
- **Permission-Based Groups**: Display different headers/footers based on player groups
- **Easy Configuration**: Simple TOML configuration with detailed examples

### 🔧 Administration
- **Admin Panel**: Intuitive admin interface with `/adminpanel`
- **Moderation Tools**: Ban, kick, mute players with professional feedback
- **Performance Monitoring**: Track server TPS, memory usage, and entities
- **Maintenance Mode**: Toggle server maintenance status
- **Jail System**: Restrict problematic players to designated areas
- **Vanish**: Become invisible to regular players
- **PowerTools**: Bind commands to items for quick execution
- **Chat Management**: Format, color, and manage chat messages
- **Time & Weather Control**: Manage game environment

### 📦 Player Utilities
- **Kits**: Create and distribute item kits with cooldowns
- **Mail System**: Send offline messages to players
- **AFK System**: Detect and mark idle players

## Installation

1. Download NeoEssentials from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/neoessentials) or [Modrinth](https://modrinth.com/mod/neoessentials)
2. Place the JAR file in your server's `mods` folder
3. Start or restart your server
4. Configure the mod using generated config files (see Configuration section)

## Configuration

NeoEssentials creates the following configuration files:
```
config/neoessentials/general.toml   # General mod settings
config/neoessentials/economy.toml   # Economy system settings
config/neoessentials/homes.toml     # Home teleportation settings
config/neoessentials/warps.toml     # Server warp settings
config/neoessentials/kits.toml      # Item kit configuration
config/neoessentials/tablist.toml   # Tablist display and animations
config/neoessentials/database.toml  # Storage backend configuration
```

The mod automatically generates all necessary config files with detailed comments explaining each option. Each file includes working examples and detailed explanations.

### Tablist Animations

NeoEssentials features a powerful tablist animation system with multiple animation types:
- **Rotation** - Cycles through multiple lines of text
- **Scroll** - Horizontally scrolls long text
- **Fade** - Cycles through colors for text
- **Rainbow** - Applies rainbow colors to each character
- **Typewriter** - Types out text character by character
- **Blink** - Makes text appear and disappear
- **Wave** - Creates a flowing wave effect with colors

For detailed configuration examples, see the [Tablist Animation Guide](docs/TABLIST_ANIMATIONS.md).

### Storage System

NeoEssentials supports three storage backends:

1. **JSON** - Default storage method. Data is stored in JSON files.
2. **SQLite** - Data is stored in a SQLite database file.
3. **MySQL** - Data is stored in a MySQL database server.

For detailed storage configuration, see our [wiki](https://github.com/zerog/neoessentials/wiki/Storage).

## Commands

Below is a summary of the main command categories. For a complete list with permissions, see our [Commands Wiki Page](https://github.com/zerog/neoessentials/wiki/Commands).

- **Admin Commands**: `/adminpanel`, `/maintenance`, `/server`
- **Moderation**: `/ban`, `/kick`, `/mute`, `/jail`
- **Teleportation**: `/home`, `/warp`, `/tp`, `/back`
- **Economy**: `/balance`, `/pay`, `/eco`
- **Player Utilities**: `/kit`, `/mail`, `/powertool`
- **World Management**: `/time`, `/weather`

## Key Commands

NeoEssentials includes dozens of useful commands. Here's a summary of the most important ones:

### Teleportation Commands
- `/home` - Teleport to your home
- `/sethome [name]` - Set your home at your current position
- `/delhome [name]` - Delete a home
- `/warp [name]` - Teleport to a server warp
- `/tpa <player>` - Send a teleport request
- `/tpaccept` - Accept a teleport request
- `/back` - Return to your previous location

### Economy Commands
- `/balance` - Check your account balance
- `/pay <player> <amount>` - Send money to another player
- `/baltop` - View richest players on the server

### Kit Commands
- `/kit <name>` - Receive an item kit
- `/kits` - List available kits

### Administrative Commands
- `/adminpanel` - Open admin control panel
- `/heal [player]` - Heal a player
- `/feed [player]` - Feed a player
- `/gm <mode> [player]` - Change gamemode

### UI Commands
- `/workbench` - Open a virtual crafting table
- `/anvil` - Open a virtual anvil
- `/tablist reload` - Reload the tablist configuration
- `/tablist reset` - Reset tablist config to defaults
- `/tablist debug` - Toggle tablist debug mode

For a complete list of commands and permissions, see [Commands Documentation](docs/COMMANDS_FULL.md).

## Permissions

NeoEssentials works with popular permission plugins:

<<<<<<< HEAD
Community Documentation: https://docs.neoforged.net/ | NeoForged Discord: https://discord.neoforged.net/
>>>>>>> d0645b4 (feat: Add KitManager class for managing player kits and cooldowns; implement JSON data handling)
=======
- **LuckPerms** - Primary supported permission system
- **FTB Ranks** - Alternative permission provider
- **Default Permissions** - Built-in fallback system

See our [Permissions Wiki Page](https://github.com/zerog/neoessentials/wiki/Permissions) for a complete permissions list.

## Server-Side Compatibility

NeoEssentials is designed as a **true server-side mod** with:
- **No client requirements** - Works with vanilla clients
- **No custom packets** - Uses only vanilla-compatible networking
- **No custom command argument types** - Compatible with all clients
- **Standard serialization** - No desync with vanilla or modded clients

This means NeoEssentials works seamlessly with vanilla clients, modded clients, and other server mods.

## Support & Issues

If you encounter any issues or have suggestions:
- Check the [Wiki](https://github.com/zerog/neoessentials/wiki) for documentation
- Report bugs on our [Issue Tracker](https://github.com/zerog/neoessentials/issues)
- Join our [Discord Server](https://discord.gg/placeholder) for community support

## License

NeoEssentials is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
>>>>>>> 9085b7b (Documentation improvements for release - Enhanced README, command documentation, and JavaDoc comments)
