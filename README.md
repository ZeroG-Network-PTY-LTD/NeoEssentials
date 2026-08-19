# NeoEssentials

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://www.minecraft.net/) [![NeoForge](https://img.shields.io/badge/NeoForge-orange.svg)](https://neoforged.net/) [![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](https://opensource.org/licenses/MIT) [![Version](https://img.shields.io/github/v/release/ZeroG-Network-PTY-LTD/NeoEssentials?label=Version)](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/releases) [![Discord](https://img.shields.io/discord/709351422088708196?color=7289da&label=Discord&logo=discord&logoColor=white)](https://discord.gg/dUGAQF2Mga)
[![Ko-fi](https://img.shields.io/badge/Ko--fi-Support%20Me-F16061?logo=ko-fi&logoColor=white)](https://ko-fi.com/mrwhiteflamesyt)

> NeoEssentials is a comprehensive, config-driven essentials mod for Minecraft NeoForge 1.21.1 - 1.21.11 servers. It provides 50+ commands, GUI tools, advanced administration, and a real-time web dashboard—all managed by modular JSON config files and standardized documentation.

## 📦 This Branch

`Released` holds documentation, changelogs, and release notes only — there is
no buildable source code here. Development happens on two separate branches,
split by target Minecraft version:

| Branch | Target | Purpose |
|---|---|---|
| [`Dev-Builds`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/tree/Dev-Builds) | Minecraft 1.21.1 – 1.21.11 · NeoForge 21.1.179+ | Primary development branch |
| [`mc-26.1-port`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/tree/mc-26.1-port) | Minecraft 26.2 · NeoForge 26.2.0.63+ | 26.x port branch — rolls forward with each new 26.x release |

### Download

Grab the built jar for your Minecraft version from:
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/neoessentials)
- [Modrinth](https://modrinth.com/mod/neoessentials)
- [GitHub Releases](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/releases)

## 🌟 Overview

NeoEssentials brings essential server management, player utilities, and advanced admin features to NeoForge servers. All features are strictly documented and driven by config files for reliability and transparency.

**Server-Side Only**: No client install required—works with vanilla clients.
**50+ Commands**: Covers all major server functions, utilities, and moderation.
**Modern UI**: GUI commands, color code support, and web dashboard.

## ✨ Core Systems & Features

- **Economy System**: Player balances, payments, kits, and shop support.
- **Chat & Messaging**: Private messages, mail, ignore/socialspy, AFK system.
- **Moderation**: Ban, kick, mute, jail, vanish, freeze, sudo, player data.
- **Teleportation**: Homes, warps, spawn, teleport requests, back system.
- **Kit Management**: Configurable item kits with cooldowns and preview.
- **Web Dashboard**: Real-time server monitoring, config editing, API endpoints.
- **Permission System**: LuckPerms, FTB Ranks, and built-in support.
- **Item Management**: Item spawning, repair, enchant, clearinventory, powertool.
- **Utility Systems**: Nicknames, MOTD, near, ping, depth, helpop, rules, suicide, etc.
- **API & Placeholder System**: PlaceholderAPI integration, custom placeholders, REST API endpoints.

## 📖 Documentation

Start at [Home](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki/Home) for a complete, config-driven documentation hub. All major systems are documented and standardized to match the codebase and config files:
  - [EconomySystem](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki/EconomySystem)
  - [ChatSystem](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki/ChatSystem)
  - [ModerationSystem](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki/ModerationSystem)
  - [TeleportationSystem](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki/TeleportationSystem)
  - [KitManagement](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki/KitManagement)
  - [WebDashboard](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki/WebDashboard)
  - [PermissionSystem](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki/PermissionSystem)
  - [ItemManagement](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki/ItemManagement)
  - [UtilitySystems](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki/UtilitySystems)
  - [APISystem](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki/APISystem)
  - [AFKSystem](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki/AFKSystem)

See [APISystem](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki/APISystem) for API & Placeholder System details, including:
  - PlaceholderAPI integration for dynamic text
  - Custom and expansion placeholders
  - Web Dashboard REST API endpoints for server status, player info, logs, config, events, and statistics
  - Permissions and config options for API features

## 🚀 Quick Start

### Installation
1. Download the latest release for your Minecraft version (see [Download](#-download) above)
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

See [Home](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki/Home) and individual system docs above for full command lists and config options.

## 🔧 Configuration Examples

All features are managed by modular JSON config files. See [Home](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki/Home) and system docs above for details.

## 🔗 API Integration for Modders

See [APISystem](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki/APISystem) for full API and PlaceholderAPI documentation, including:
- Registering custom placeholders
- Using REST API endpoints for server data
- Economy API for mod integration

## 🔗 Integration & Compatibility

- **LuckPerms** and **FTB Ranks** supported
- **Server-Side Only** (no client mods required)
- **Vanilla Client Support**
- **Performance Optimized**

## 🛠️ Development

This branch has no source code — see [This Branch](#-this-branch) above for
where to find it. `Dev-Builds` and `mc-26.1-port` each have their own
IDE/Gradle setup and build instructions.

## 🤝 Support & Community

- **Discord**: [Join our Discord server](https://discord.gg/dUGAQF2Mga) for support and community discussion
- **Bug Reports**: Report issues and bugs through GitHub or Discord
- **Feature Requests**: Suggest new features and improvements
- **Documentation**: See [Home](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki/Home) and system docs above

## 📄 License

NeoEssentials is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

**🌟 Ready to enhance your server? Download NeoEssentials and give your players the essential tools they need!**

*Made with ❤️ for the Minecraft community*
