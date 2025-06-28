# Installation Guide

This guide will help you install NeoEssentials on your Minecraft NeoForge server and get started with the latest features.

## 📋 Requirements

- **Minecraft**: 1.21.1+
- **NeoForge**: 52.1.1+
- **Java**: 17 or higher
- **Server RAM**: Minimum 2GB (4GB+ recommended for animations)

## 📦 Installation Steps

### Step 1: Download NeoEssentials

Download the latest version (v1.0.1.89+) from:
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/neoessentials) - Recommended
- [Modrinth](https://modrinth.com/mod/neoessentials) - Alternative
- [GitHub Releases](https://github.com/ZeroG-Network/NeoEssentials/releases) - Development builds

### Step 2: Server Installation

1. **Stop your server** completely
2. **Place the JAR file** in your server's `mods/` folder
3. **Remove old versions** if upgrading from a previous version
4. **Start your server**

### Step 3: Automatic Configuration

NeoEssentials will automatically create:

📁 **`config/neoessentials/`** - Main configuration files  
📁 **`neoessentials/`** - Templates, animations, and data files  
📁 **`neoessentials/templates/`** - Tablist template configurations  
📁 **`neoessentials/animations/`** - Animation frame definitions  

### Step 4: Verify Installation

Check that NeoEssentials is working:

1. **Join your server**
2. **Run**: `/neoessentials version`
3. **Expected output**: `NeoEssentials v1.0.1.89` (or newer)
4. **Check tablist**: You should see the default animated tablist

## 🚀 Quick Start Features

### Instant Tablist Animations
NeoEssentials comes with pre-configured animations:
- **Welcome messages** with typewriter effects
- **Rainbow server names** with smooth color transitions  
- **Real-time player counts** and server stats
- **Multiple simultaneous animations** running independently

### Default Configuration
The mod includes optimized default settings:
- **Ultra-smooth 25ms** animation frame updates
- **250ms placeholder updates** for dynamic data
- **3000ms template switching** for variety
- **Zero server impact** performance optimization

## 🔧 Next Steps

### Essential Configuration
- [**Tablist System**](Tablist-System) - Customize headers, footers, and animations
- [**Animation System**](Animation-System) - Create custom smooth animations
- [**Configuration Guide**](Configuration-Guide) - Basic server setup
- [**Permissions Guide**](Permissions-Guide) - Set up player permissions

### Advanced Features  
- [Economy System](Economy-System) - Server economy and transactions
- [Home & Warp System](Home-System) - Player teleportation management
- [Kit System](Kit-System) - Custom item packages
- [Performance Optimization](Performance-Optimization) - Server tuning

## ⚠️ Important Notes

### Performance Recommendations
- **Monitor TPS** when using many animations simultaneously
- **Adjust animation intervals** based on server capacity  
- **Use `/tablist debug`** to check performance impact

### Common Issues
- **Animations not showing**: Check `enable_animations: true` in config
- **Poor performance**: Increase animation intervals
- **Permission errors**: Install LuckPerms or use vanilla permissions

### Getting Help
- [Troubleshooting Guide](Troubleshooting)
- [FAQ](Frequently-Asked-Questions)  
- [Discord Support](https://discord.gg/neoessentials)

---

*Ready to get started? Continue with the [Configuration Guide](Configuration-Guide) to customize your server!*

## Troubleshooting

If you encounter any issues during installation:

- Check the server logs for error messages
- Ensure you're using compatible versions of Minecraft and NeoForge
- Verify you don't have conflicting mods installed
- Visit our [Discord server](https://discord.gg/dUGAQF2Mga) for support

For detailed troubleshooting steps, see the [Troubleshooting](Troubleshooting) guide.
