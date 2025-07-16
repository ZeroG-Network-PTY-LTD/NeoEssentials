<<<<<<< HEAD
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
=======
# NeoEssentials v1.0.2

![NeoEssentials Logo](https://raw.githubusercontent.com/ZeroG-Network/NeoEssentials/main/Logo.png)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-52.1.1+-blue.svg)](https://neoforged.net/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](https://opensource.org/licenses/MIT)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()
[![Economy System](https://img.shields.io/badge/Economy-Complete-success.svg)]()

## 🌟 Overview

NeoEssentials is a comprehensive server management and quality-of-life mod for Minecraft NeoForge servers. **Version 1.0.2** introduces a revolutionary **Advanced Economy System** with complete banking, multi-currency support, loans, shops, and economic analytics.

## ✨ Key Features

### 🏦 **Advanced Economy System (v1.0.2)** ✅ **FULLY IMPLEMENTED**
- **Multi-Currency Support**: Standard coins, resource-backed currencies (gold, diamonds), and event tokens ✅
- **Complete Banking System**: Checking, savings, business, and investment accounts with interest ✅
- **Sophisticated Loan System**: Personal, business, and mortgage loans with credit scoring ✅
- **Shop Management**: Player and admin shops with dynamic pricing systems ✅
- **Auction House**: Complete bidding system with buyout options ✅
- **Economic Analytics**: Real-time monitoring, inflation tracking, and wealth distribution analysis ✅
- **Async Persistence**: High-performance SQLite database operations with JSON backup ✅

### 🎮 **Core Server Features**
- **Enhanced Tablist System**: Customizable headers, footers, and boss bars with animations and native hex color support
- **Home & Warp System**: Player homes and server teleportation points
- **Kit System**: Configurable item kits for players with cooldowns
- **Moderation Tools**: Advanced tools for server moderation and administration
- **Permission Integration**: Works with popular permission systems (LuckPerms, FTB Ranks)
- **Performance Optimized**: Designed for minimal server impact
- **Extensive Configuration**: YAML and JSON-based configuration for maximum flexibility

## 📋 Requirements

- Minecraft 1.21.1+
- NeoForge 52.1.1+

## 🚀 Installation

1. Download the latest version from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/neoessentials) or [Modrinth](https://modrinth.com/mod/neoessentials)
2. Place the JAR file in your server's `mods` folder
3. Start or restart your server
4. Configure the mod in `config/neoessentials/` and `neoessentials/` directories

## 📚 Documentation

### **Economy System Documentation**
- 📋 [Economy System Complete](docs/ECONOMY_SYSTEM_COMPLETE.md) - Implementation overview
- 🚀 [Production Deployment Guide](docs/PRODUCTION_DEPLOYMENT_GUIDE.md) - Deploy to production
- 💰 [Loan System Guide](docs/LOAN_SYSTEM_COMPLETE.md) - Complete loan system documentation
- 📊 [Economy Plan](docs/v1.0.2_ECONOMY_PLAN.md) - Full feature specifications
- 📈 [Final Implementation Report](docs/FINAL_IMPLEMENTATION_REPORT.md) - Technical completion status

### **General Documentation**
- [Wiki](https://github.com/ZeroG-Network/NeoEssentials/wiki) - Complete usage guides
- [Commands](https://github.com/ZeroG-Network/NeoEssentials/wiki/Commands) - Command reference
- [Permissions](https://github.com/ZeroG-Network/NeoEssentials/wiki/Permissions) - Permission list
- [Configuration](https://github.com/ZeroG-Network/NeoEssentials/wiki/Configuration) - Config guide
- [Tablist Hex Colors](docs/HEX_COLOR_SUPPORT.md) - Native hex color support guide
- [JSON Templates](https://github.com/ZeroG-Network/NeoEssentials/wiki/JSON-Templates) - Template system guide

## 🛠️ Configuration

NeoEssentials uses a comprehensive configuration system:

### **Economy Configuration** (v1.0.2)
```yaml
# config/neoessentials/economy.yml
economy:
  enabled: true
  starting_balance: 1000.0
  default_currency: "coins"
  
  banking:
    enabled: true
    interest_rate: 0.05
    max_accounts_per_player: 5
    
  loans:
    enabled: true
    max_loan_amount: 50000.0
    credit_scoring: true
    
  shops:
    enabled: true
    dynamic_pricing: true
    max_shops_per_player: 3
```

### **Legacy Configuration**
- **TOML Configs**: Located in `config/neoessentials/` directory for basic mod settings
- **YAML Configs**: Located in `neoessentials/` directory (server root) for tablist configuration

## 📢 Support & Community

- [Discord](https://discord.gg/dUGAQF2Mga) - Get help and chat with other users
- [GitHub Issues](https://github.com/ZeroG-Network/NeoEssentials/issues) - Report bugs and suggest features
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/neoessentials) - Leave reviews and comments

## 🔄 Compatibility

NeoEssentials works with:

- Most permission mods (LuckPerms, FTB Ranks)
- Popular placeholder APIs
- Other utility and management mods

## 📜 License

NeoEssentials is licensed under the MIT License. See the LICENSE file for details.
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
