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
