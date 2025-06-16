# NeoEssentials

<<<<<<< HEAD
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

### 🔧 Administration
- **Admin Panel**: Intuitive admin interface with `/adminpanel`
- **Moderation Tools**: Ban, kick, mute players with professional feedback
- **Performance Monitoring**: Track server TPS, memory usage, and entities
- **Maintenance Mode**: Toggle server maintenance status

### 📦 Player Utilities
- **Kits**: Create and distribute item kits with cooldowns
- **Mail System**: Send offline messages to players
- **Jail System**: Restrict problematic players to designated areas
- **Vanish**: Become invisible to regular players
- **PowerTools**: Bind commands to items for quick execution

### ⚙️ Other Features
- **AFK System**: Detect and mark idle players
- **Chat Management**: Format, color, and manage chat messages
- **Time & Weather Control**: Manage game environment

## Installation

1. Download NeoEssentials from [CurseForge](https://curseforge.com) or [Modrinth](https://modrinth.com)
2. Place the JAR file in your server's `mods` folder
3. Start or restart your server
4. Configure the mod using generated config files (see Configuration section)

## Configuration

NeoEssentials creates the following configuration files:
```
config/neoessentials-general.toml   # General mod settings
config/neoessentials-database.toml  # Storage backend configuration
```

The mod automatically generates all necessary config files with detailed comments explaining each option.

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

## Permissions

NeoEssentials works with popular permission plugins:

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
=======
A comprehensive server-side essentials mod for Minecraft NeoForge servers, inspired by EssentialsX for Bukkit/Spigot.

## Features

- Economy system with balance tracking and transactions
- Home teleportation with multiple homes per player
- Warp system for server-wide teleportation points
- Teleport requests and last location tracking
- Player and admin commands for server management
- Kits system for distributing items
- Chat formatting and management
- Integration with permission mods (LuckPerms, FTB Ranks)

## Important: Server-Side Only

**This mod is designed to be installed only on the server side.** It does not need to be installed on clients and provides no client-side functionality.

## Installation

Once you have your clone, simply open the repository in the IDE of your choice. The usual recommendation for an IDE is either IntelliJ IDEA or Eclipse.

If at any point you are missing libraries in your IDE, or you've run into problems you can
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
{this does not affect your code} and then start the process again.

Mapping Names:
============
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

## Configuration

NeoEssentials is highly configurable. The main configuration file is created at:
```
config/neoessentials/config.json
```

Player data is stored in:
```
config/neoessentials/data/
```

## Commands

Here are some of the key commands included:

- `/home` - Teleport to your home
- `/sethome` - Set your home location
- `/warp` - Teleport to a server warp
- `/tpa` - Request to teleport to a player
- `/tpaccept` - Accept a teleport request
- `/back` - Return to your previous location
- `/balance` - Check your economy balance

## Permission Integration

NeoEssentials works with LuckPerms and FTB Ranks for permission control.

## Credits & Resources

Created by ZeroG_Essentialsx
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
