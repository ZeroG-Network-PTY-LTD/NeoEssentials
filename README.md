# NeoEssentials

![NeoEssentials Logo](docs/images/Logo.png)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.179+-blue.svg)](https://neoforged.net/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](https://opensource.org/licenses/MIT)
[![Version](https://img.shields.io/badge/Version-1.0.2.2--HOTFIX-brightgreen.svg)]()
[![Discord](https://img.shields.io/discord/placeholder?color=7289da&label=Discord&logo=discord&logoColor=white)](https://discord.gg/dUGAQF2Mga)

> A comprehensive essentials mod for Minecraft NeoForge 1.21.1 servers featuring 50+ commands, GUI tools, and advanced administration features

## 🌟 Overview

NeoEssentials is a feature-rich server-side mod that brings essential server management and player utility commands to Minecraft NeoForge servers. Built for performance and ease of use, it provides administrators and players with the tools they need for a smooth server experience.

**🎯 Server-Side Only**: Players don't need to install anything - works with vanilla clients!
**⚡ 50+ Commands**: Comprehensive command suite covering all essential server functions
**🎨 Modern UI**: Enhanced GUI commands with proper color code support

## ✨ Core Features

### ⚡ **Essential Commands** (50+ Commands)
- **Player Utilities**: `/heal`, `/feed`, `/god`, `/vanish`, `/fly`, `/speed`, `/gamemode`
- **Item Management**: `/give`, `/item`, `/repair`, `/enchant`, `/clear`
- **Player Info**: `/list`, `/whois`, `/seen`, `/ping`, `/afk`
- **Inventory Tools**: `/invsee`, `/enderchest`, nickname system with `/nick`

### 🎮 **GUI Commands** (Working & Enhanced)
- **Workbench**: `/wb` or `/workbench` - Portable crafting table
- **Anvil**: `/anvil` - Portable anvil interface  
- **Smithing Table**: `/smithing` - Portable smithing interface
- **Stonecutter**: `/stonecutter` - Portable stonecutter interface
- **Crafting**: `/craft` or `/crafting` - Alternative crafting interface
- All GUI commands properly support color codes and stay open

### � **Teleportation System**
- **Homes**: `/home`, `/sethome`, `/delhome`, `/homes` - Player home system
- **Warps**: `/warp`, `/setwarp`, `/delwarp`, `/warps` - Server warp points
- **Teleport Requests**: `/tpa`, `/tpaccept`, `/tpdeny` - Player-to-player teleporting
- **Back System**: `/back` - Return to previous locations
- **Spawn Management**: `/spawn`, `/setspawn` - Server spawn system

### � **Economy System**
- **Balance Management**: `/balance`, `/bal`, `/eco` - Check and manage economy
- **Payment System**: `/pay` - Transfer money between players
- **Kit System**: `/kit`, `/kits` - Item kit rewards with cooldowns
- **Shop System**: Sign-based shops for buying/selling items

### � **Communication & Social**
- **Messaging**: `/msg`, `/reply`, `/r` - Private messaging system
- **Mail System**: `/mail` - Offline messaging between players
- **Social Features**: Ignore system, social spy for staff
- **AFK System**: Automatic AFK detection and management

### 🛡️ **Moderation Tools**
- **Player Management**: `/ban`, `/unban`, `/kick`, `/mute`, `/unmute`
- **Administrative**: `/sudo`, `/powertool`, `/time`, `/weather`
- **Player Data**: Complete player data management and storage
- **Last Seen**: Track player login/logout times

### � **Customization & Display**
- **Language System**: Multi-language support with hot-swapping
- **Color Support**: Full hex color code support with `&#RRGGBB` format
- **Placeholder System**: Extensive placeholder support for dynamic text
- **Animation Commands**: Basic animation system for displays

### ⚙️ **Administration Features**
- **Web Dashboard**: Real-time server monitoring and management interface
- **Permission Integration**: LuckPerms and FTB Ranks support
- **Configuration System**: JSON-based modular configuration
- **Performance Monitoring**: Server performance tracking and optimization

## 📋 Requirements

- **Minecraft**: 1.21.1
- **NeoForge**: 21.1.179 or higher
- **Java**: 21 or higher
- **Server Type**: Dedicated server (client installation not required)

## 🚀 Quick Start

### Installation
1. Download the latest release (neoessentials-1.0.2.2_HOTFIX.jar)
2. Place the JAR file in your server's `mods` folder
3. Start your server to generate configuration files in `config/neoessentials/`
4. Configure permissions and features as needed
5. Restart the server to apply changes

### Essential Configuration Files
```
config/neoessentials/
├── config.json           # Main configuration settings
├── permissions.json      # Permission system setup
├── language/            # Language files directory
├── shops.json           # Shop system configuration
└── settings.json        # Additional mod settings
```

### Quick Permission Setup
For LuckPerms users:
```
/lp group admin permission set neoessentials.admin true
/lp group moderator permission set neoessentials.moderator true
/lp group default permission set neoessentials.player true
```

## 🎮 Command Reference

### � **GUI Commands**
```
/workbench (/wb)         # Open portable crafting table
/anvil                   # Open portable anvil
/smithing                # Open portable smithing table  
/stonecutter             # Open portable stonecutter
/craft (/crafting)       # Open crafting interface
```

### � **Teleportation Commands**
```
/home [name]             # Teleport to home
/sethome [name]          # Set home at current location
/delhome <name>          # Delete a home
/homes                   # List your homes
/warp <name>             # Teleport to warp
/warps                   # List available warps
/spawn                   # Teleport to spawn
/tpa <player>            # Request teleport to player
/tpaccept                # Accept teleport request
/tpdeny                  # Deny teleport request
/back                    # Return to previous location
```

### ⚡ **Essential Utilities**
```
/heal [player]           # Heal yourself or target player
/feed [player]           # Feed yourself or target player
/god [player]            # Toggle god mode
/vanish                  # Toggle invisibility
/fly [player]            # Toggle flight mode
/speed <speed> [player]  # Set movement speed
/gamemode <mode> [player] # Change gamemode
/repair [all]            # Repair held item or all items
/give <player> <item>    # Give items to players
/item <item>             # Give yourself items
```

### 👥 **Player Management**
```
/list                    # Show online players
/whois <player>          # Show player information
/seen <player>           # Check when player was last online
/nick <nickname>         # Set your nickname
/invsee <player>         # View player's inventory
/afk                     # Toggle AFK status
/ping [player]           # Check connection ping
```

### 💰 **Economy Commands**
```
/balance (/bal)          # Check your balance
/pay <player> <amount>   # Pay another player
/eco <subcommand>        # Economy administration
/kit <name>              # Claim a kit
/kits                    # List available kits
```

### 💬 **Communication**
```
/msg <player> <message>  # Send private message
/reply <message> (/r)    # Reply to last message
/mail send <player> <msg> # Send offline mail
/mail read               # Read your mail
/ignore <player>         # Ignore a player
```

### 🛡️ **Moderation Commands**
```
/ban <player> [reason]   # Ban a player
/unban <player>          # Unban a player
/kick <player> [reason]  # Kick a player
/mute <player> [time]    # Mute a player
/unmute <player>         # Unmute a player
/sudo <player> <command> # Force player to run command
/time <set|add> <value>  # Control server time
/weather <clear|rain>    # Control weather
```

### ⚙️ **Administration**
```
/setwarp <name>          # Create a warp point
/delwarp <name>          # Delete a warp point
/setspawn                # Set server spawn point
/language <set|reload>   # Language management
/config reload           # Reload configuration
/permissions <subcommand> # Permission management
```

## 🔧 Configuration Examples

### Basic Configuration Structure
The mod uses JSON-based configuration files for flexibility:

```json
// config/neoessentials/config.json
{
  "general": {
    "debug": false,
    "language": "en"
  },
  "features": {
    "economy": true,
    "homes": true,
    "warps": true,
    "kits": true,
    "shops": true
  },
  "limits": {
    "max_homes": 5,
    "max_warps": 10
  }
}
```

### Permission Integration
```json
// config/neoessentials/permissions.json
{
  "system": "luckperms",
  "groups": {
    "admin": "neoessentials.admin",
    "moderator": "neoessentials.moderator", 
    "player": "neoessentials.player"
  }
}
```

### Shop System Setup
```json
// config/neoessentials/shops.json
{
  "enabled": true,
  "sign_shops": true,
  "max_shops_per_player": 3,
  "creation_cost": 100.0
}
```

## 🔗 Integration & Compatibility

### ✅ Supported Permission Systems
- **LuckPerms** - Full integration with permission nodes and contexts
- **FTB Ranks** - Complete compatibility with FTB server systems
- **Built-in System** - Fallback permission system when no plugin is available

### ✅ Server Compatibility
- **Server-Side Only** - No client modifications required
- **Vanilla Client Support** - Works with unmodded Minecraft clients
- **Mod Compatibility** - Compatible with most other NeoForge server mods
- **Performance Optimized** - Designed for minimal server impact

## 🚀 Key Highlights

### What Makes NeoEssentials Special
- **🎯 50+ Essential Commands** - Complete command suite for server administration and player utilities
- **🎮 Working GUI Commands** - Portable crafting, anvil, smithing, and stonecutter interfaces that actually work
- **🎨 Modern Color Support** - Full hex color code support with `&#RRGGBB` format
- **🔧 Easy Configuration** - JSON-based config files for maximum flexibility
- **⚡ Performance Focused** - Optimized for minimal server impact
- **🌐 Multi-Language** - Built-in language system with easy translation support
- **🛡️ Permission Ready** - Seamless integration with popular permission plugins

## 🤝 Support & Community

### Getting Help
-  **Discord**: Join our [Discord server](https://discord.gg/dUGAQF2Mga) for support and community discussion
- 🐛 **Bug Reports**: Report issues and bugs through GitHub or Discord
- 💡 **Feature Requests**: Suggest new features and improvements
- 📖 **Documentation**: Check the `docs/` folder for detailed guides and examples

### Contributing
We welcome contributions from the community! Whether you're fixing bugs, adding features, or improving documentation, your help is appreciated.

## 🎯 Perfect For

- **� Survival Servers** - Essential commands and utilities for survival gameplay
- **🎮 Creative Servers** - Creative tools and administrative features
- **👥 Community Servers** - Social features, messaging, and player management
- **🏪 Economy Servers** - Built-in economy system with shop support
- **⚙️ Admin-Focused** - Comprehensive administration and moderation tools

## 📊 Statistics

- **50+ Commands** across all essential categories
- **5 GUI Interfaces** (Workbench, Anvil, Smithing, Stonecutter, Crafting)
- **Multi-Language Support** with easy translation system
- **Permission Integration** for LuckPerms and FTB Ranks
- **JSON Configuration** for maximum flexibility and ease of use

## 📄 License

NeoEssentials is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

**🌟 Ready to enhance your server? Download NeoEssentials and give your players the essential tools they need!**

*Made with ❤️ for the Minecraft community*
