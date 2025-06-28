# NeoEssentials - Essential Features for NeoForge Servers

**A comprehensive server-side essentials mod for Minecraft NeoForge servers, inspired by EssentialsX for Bukkit/Spigot.**

NeoEssentials brings all the essential features you need to run and manage a professional Minecraft server. From advanced tablist systems to complete economy management, this mod provides a complete solution for server administrators.

---

## 🌟 Key Features

### Enhanced Tablist System ✨
- **Custom Headers & Footers**: Design stunning tablists with rich text formatting and RGB colors
- **Advanced Animation System**: Smooth, configurable animations with multiple placeholders support
- **Three-Tier Update System**: Independent update intervals for templates (slow), placeholders (medium), and animations (ultra-smooth 25ms)
- **JSON Templates**: Flexible template system with easy-to-manage JSON configuration
- **Group-Specific Display**: Show different content based on player groups and permissions
- **Dynamic Bossbars**: Create informative and interactive boss bars

### Economy System 💰
- **Complete Currency Management**: Full-featured economy with balances, transactions, and history
- **Shop Integration**: Buy and sell items through a robust, extensible shop system
- **Admin Controls**: Comprehensive economy management tools for server administrators
- **API Support**: Extensible API for seamless integration with other mods
- **Payment System**: Secure player-to-player money transfers with transaction logging

### Home & Warp System 🏠
- **Multiple Homes**: Players can set and manage multiple home locations
- **Server Warps**: Admins can create public teleportation points for all players
- **Permission Control**: Fine-grained access control for warps and homes
- **Cooldowns & Costs**: Configurable costs and cooldowns for teleportation commands
- **TPA System**: Teleport request system with accept/deny functionality

### Kit System 🎁
- **Custom Kits**: Create kits with any items, enchantments, and special properties
- **Smart Cooldown System**: Set usage limits with configurable per-kit cooldowns
- **Economy Integration**: Optional costs for kit redemption with balance checking
- **Permission-Based Access**: Control kit availability with granular permissions
- **One-Time Kits**: Support for kits that can only be claimed once per player

### Advanced Permission System 🔐
- **Flexible Group Management**: Create and manage permission groups with inheritance
- **Fine-Grained Control**: Control access to specific features, commands, and areas
- **Multiple Backend Support**: Works with LuckPerms, FTB Ranks, or built-in system
- **User-Friendly Interface**: Simple command interface for permission management
- **Real-Time Updates**: Permission changes take effect immediately

### Comprehensive Command System 🛠️
- **50+ Essential Commands**: Complete set of administrative and utility commands
- **Custom Aliases**: Create shortcuts and aliases for frequently used commands
- **Intelligent Help System**: Detailed command documentation available in-game
- **Permission Integration**: All commands respect the permission system
- **Tab Completion**: Smart tab completion for all commands and arguments

---

## 🚀 True Server-Side Implementation

**100% Server-Side Operation**: This mod is completely server-side! Players can join using vanilla or modded clients without needing to install NeoEssentials themselves.

- ✅ **No Client Installation Required**: Players join with vanilla or any modded client
- ✅ **Cross-Platform Compatibility**: Works with vanilla, Fabric, Forge, and NeoForge clients
- ✅ **No Disconnections**: Carefully engineered to prevent client disconnects
- ✅ **Vanilla Protocol**: Uses only vanilla-compatible command arguments and packets

---

## ⚙️ Advanced Configuration

### Hybrid Configuration System
- **TOML Files**: Main settings in `config/neoessentials/` for easy editing
- **JSON Templates**: Complex data like tablist templates in `neoessentials/` directory
- **Live Reloading**: Most settings can be reloaded without server restart
- **Extensive Documentation**: Every config option includes helpful comments

### Flexible Storage Options
Choose from three powerful storage backends:
1. **JSON** (Default) - Simple file-based storage, perfect for small servers
2. **SQLite** - Local database file for better performance and data integrity
3. **MySQL** - External database support for multi-server setups and large networks

---

## 📋 Essential Commands Overview

| Command | Description | Permission |
|---------|-------------|------------|
| `/neoessentials help` | Show comprehensive help information | `neoessentials.command.help` |
| `/home [name]` | Teleport to a saved home location | `neoessentials.command.home` |
| `/sethome [name]` | Set a new home location | `neoessentials.command.sethome` |
| `/warp [name]` | Teleport to a server warp point | `neoessentials.command.warp` |
| `/balance` | Check your current balance | `neoessentials.command.balance` |
| `/pay <player> <amount>` | Transfer money to another player | `neoessentials.command.pay` |
| `/kit [name]` | Claim an available kit | `neoessentials.command.kit` |
| `/tpa <player>` | Send a teleport request | `neoessentials.command.tpa` |
| `/adminpanel` | Open the admin management interface | `neoessentials.admin.panel` |

**And 40+ more commands for complete server management!**

---

## 🔧 Installation & Setup

### Requirements
- **Minecraft**: 1.21.1
- **Platform**: NeoForge
- **Server Type**: Dedicated server (no client installation needed)

### Quick Installation
1. Install [NeoForge](https://neoforged.net/) for Minecraft 1.21.1
2. Download NeoEssentials from this page
3. Place the mod JAR file in your server's `mods` folder
4. Start your server - default configs will be automatically generated
5. Configure the mod through files in `config/neoessentials/` and `neoessentials/`

### Initial Configuration
- Main settings: `config/neoessentials/general.toml`
- Tablist settings: `config/neoessentials/tablist.toml`
- Templates & animations: `neoessentials/templates.json` and `animations.json`
- Permissions: Configure through your preferred permission mod or built-in system

---

## 🎯 Perfect For

- **Small Private Servers**: Easy setup with sensible defaults
- **Large Public Networks**: Scalable with MySQL support and advanced features
- **Roleplay Servers**: Rich tablist customization and group-based features
- **Economy Servers**: Complete shop and currency management
- **Creative Servers**: Extensive kit system and admin tools
- **Survival Servers**: Home/warp system and player utilities

---

## 📚 Documentation & Support

- 📖 **[Complete Wiki](https://github.com/ZeroG-Network/NeoEssentials/wiki)** - Comprehensive documentation
- 🐛 **[Issue Tracker](https://github.com/ZeroG-Network/NeoEssentials/issues)** - Report bugs and request features
- 💬 **[Discord Community](https://discord.gg/dUGAQF2Mga)** - Get help and share experiences
- 📦 **[GitHub Repository](https://github.com/ZeroG-Network/NeoEssentials)** - Source code and development

---

## 🎨 Screenshots & Examples

### Advanced Tablist System
![Tablist Example](https://cdn.modrinth.com/data/neoessentials/images/tablist-showcase.png)
*Custom headers, footers, and animations with RGB color support*

### Permission Management
![Permissions Example](https://cdn.modrinth.com/data/neoessentials/images/permissions-showcase.png)
*Easy-to-use permission system with group management*

### Economy & Shop System
![Economy Example](https://cdn.modrinth.com/data/neoessentials/images/economy-showcase.png)
*Complete economy management with shop integration*

---

**Transform your Minecraft server with NeoEssentials - The complete server management solution for NeoForge!**

*Licensed under MIT - Free and open source*
