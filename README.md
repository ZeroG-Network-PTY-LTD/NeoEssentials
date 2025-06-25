# NeoEssentials

![NeoEssentials Logo](https://raw.githubusercontent.com/ZeroG-Network/NeoEssentials/main/Logo.png)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-52.1.1+-blue.svg)](https://neoforged.net/)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](https://opensource.org/licenses/MIT)

## 🌟 Overview

NeoEssentials is a comprehensive server management and quality-of-life mod for Minecraft NeoForge servers. It provides essential commands, utilities, and features to enhance the multiplayer experience for both players and administrators.

## ✨ Key Features

- **Enhanced Tablist System**: Customizable headers, footers, and boss bars with animations
- **Home & Warp System**: Player homes and server teleportation points
- **Economy System**: Complete player economy with transactions and shop integration
- **Kit System**: Configurable item kits for players with cooldowns
- **Moderation Tools**: Advanced tools for server moderation and administration
- **Permission Integration**: Works with popular permission systems (LuckPerms, FTB Ranks)
- **Performance Optimized**: Designed for minimal server impact
- **Extensive Configuration**: JSON-based configuration for maximum flexibility

## 📋 Requirements

- Minecraft 1.21.1+
- NeoForge 52.1.1+

## 🚀 Installation

1. Download the latest version from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/neoessentials) or [Modrinth](https://modrinth.com/mod/neoessentials)
2. Place the JAR file in your server's `mods` folder
3. Start or restart your server
4. Configure the mod in `config/neoessentials/` and `neoessentials/` directories

## 📚 Documentation

Comprehensive documentation is available:

- [Wiki](https://github.com/ZeroG-Network/NeoEssentials/wiki) - Complete usage guides
- [Commands](https://github.com/ZeroG-Network/NeoEssentials/wiki/Commands) - Command reference
- [Permissions](https://github.com/ZeroG-Network/NeoEssentials/wiki/Permissions) - Permission list
- [Configuration](https://github.com/ZeroG-Network/NeoEssentials/wiki/Configuration) - Config guide
- [JSON Templates](https://github.com/ZeroG-Network/NeoEssentials/wiki/JSON-Templates) - Template system guide
- [TOML to JSON Migration](https://github.com/ZeroG-Network/NeoEssentials/wiki/TOML-to-JSON-Migration) - Migration guide

## 🛠️ Configuration

NeoEssentials uses a hybrid configuration system:

- **TOML Configs**: Located in `config/neoessentials/` directory for basic settings
- **JSON/YML Templates**: Located in `neoessentials/` directory (server root) for complex data

Files are organized as follows:
- Basic settings: `config/neoessentials/tablist.yml`
- Templates: `neoessentials/templates.json` or `templates.yml`
- Animations: `neoessentials/animations.json` or `animations.yml`

The mod will automatically generate default configurations and migrate legacy TOML templates to JSON on first startup.

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
