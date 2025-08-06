# Quick Start Guide

Get NeoEssentials up and running on your server in minutes! This guide covers the essential steps to install, configure, and start using NeoEssentials with your Minecraft server.

## 🚀 Prerequisites

Before installing NeoEssentials, ensure you have:

### Server Requirements
- **Java Version**: Java 21 or higher
- **Server RAM**: Minimum 2GB (4GB+ recommended)
- **Operating System**: Windows, Linux, or macOS

### Check Your Setup

1. **Verify Java Version**:
   ```bash
   java -version
   ```
   Should show Java 21 or higher.

2. **Confirm NeoForge Installation**:
   Look for `neoforge-universal-*.jar` in your server directory.

3. **Check Server Performance**:
   Ensure your server runs smoothly without the mod first.

## ⬇️ Installation

### Step 1: Download NeoEssentials

1. Download the latest NeoEssentials JAR file from:
   - GitHub Releases: `neoessentials-1.0.2.jar`
   - CurseForge (when available)
   - Modrinth (when available)

### Step 2: Install the Mod

1. **Stop your server** if it's currently running
2. **Copy the JAR file** to your server's `mods/` folder:
   ```
   your-server/
   ├── mods/
   │   └── neoessentials-1.0.2.jar  ← Place here
   ├── world/
   ├── config/
   └── ...
   ```
3. **Start your server**

### Step 3: Verify Installation

1. **Check server logs** for successful loading:
   ```
   [INFO] Loading NeoEssentials v1.0.2
   [INFO] NeoEssentials: Essential commands system initialized
   [INFO] NeoEssentials: Configuration loaded successfully
   ```

2. **Test basic command**:
   ```bash
   /heal
   ```
   Should heal you to full health.

## ⚙️ Basic Configuration

### Initial Setup

When NeoEssentials first starts, it creates default configuration files:

```
config/
├── neoessentials-common.toml     # Server-wide settings
├── neoessentials-general.toml    # General configuration
├── gui/                         # GUI System configurations
│   ├── main_config.json         # Main GUI settings
│   ├── shop_gui.json           # Shop interface
│   ├── kits_gui.json           # Kit selection menu
│   ├── stats_gui.json          # Player statistics
│   ├── economy_gui.json        # Economy management
│   ├── warps_gui.json          # Warp destinations
│   ├── admin_gui.json          # Admin control panel
│   └── teleport_gui.json       # Teleport hub
└── neoessentials/               # Detailed configurations
    ├── commands.toml            # Command-specific settings
    ├── permissions.toml         # Permission system
    ├── teleportation.toml       # Teleport settings
    └── security.toml            # Security features
```

### Essential Settings

Edit `config/neoessentials-general.toml` for basic settings:

```toml
# Server display name
serverName = "My Minecraft Server"

# Enable essential commands
[commands]
enableEssentialCommands = true
enableTeleportation = true
enableModeration = false  # Set to true for moderation features

[features]
# Enable the new GUI system
guiSystem = true

# Basic permissions
[permissions]
useBuiltinPermissions = true
defaultGroup = "default"
```

## 👥 Setting Up Permissions

### Quick Permission Setup

1. **View current permissions**:
   ```bash
   /permissions info
   ```

2. **Set yourself as admin**:
   ```bash
   /permissions user YourUsername group set admin
   ```

3. **Give yourself all permissions**:
   ```bash
   /permissions user YourUsername permission add neoessentials.*
   /permissions user YourUsername permission add essentials.*
   ```

### Basic Groups

The default permission groups are:

- **default**: Basic player permissions
- **vip**: Enhanced player permissions  
- **moderator**: Staff moderation permissions
- **admin**: Full administrative access

**Promote a player to VIP**:
```bash
/permissions user PlayerName group set vip
```

## 🎮 Essential Commands

### Player Commands

Try these basic commands to get started:

#### Health & Sustenance
```bash
/heal          # Restore full health
/feed          # Restore full hunger
```

#### Movement & Travel
```bash
/spawn         # Teleport to spawn
/back          # Return to previous location
/home          # Teleport to your home (after setting one)
/sethome       # Set your home location
```

#### GUI System Commands 🎮
```bash
/shop          # Open the server shop interface
/kits          # Open kit selection menu
/stats         # View your player statistics
/warps         # Open warp destinations menu
/gui teleport  # Open teleportation hub
/gui theme dark    # Change to dark theme
/gui theme ocean   # Change to ocean theme
/gui theme default # Return to default theme
```

#### Utility
```bash
/repair        # Repair item in hand
/speed 2       # Set movement speed (1-10)
/fly           # Toggle flight mode
```

### Admin Commands

For server administrators:

#### Player Management
```bash
/heal PlayerName        # Heal another player
/feed PlayerName        # Feed another player
/god PlayerName         # Toggle god mode for player
```

#### GUI Management 🎛️
```bash
/admin                 # Open admin control panel
/economy               # Economy management interface (if enabled)
/neoessentials gui reload     # Reload GUI configurations
/neoessentials gui reload shop # Reload specific GUI config
```

#### Server Management
```bash
/time set day          # Set time to day
/weather clear         # Clear weather
/gamemode creative PlayerName  # Change player's gamemode
```

## 🏠 Setting Up Homes & Warps

### Player Homes

1. **Set your first home**:
   ```bash
   /sethome
   ```

2. **Set a named home**:
   ```bash
   /sethome base
   /sethome farm
   ```

3. **Teleport to home**:
   ```bash
   /home          # Default home
   /home base     # Named home
   ```

4. **List your homes**:
   ```bash
   /homes
   ```

### Server Warps (Admin)

1. **Create spawn warp**:
   ```bash
   /setwarp spawn
   ```

2. **Create public warps**:
   ```bash
   /setwarp mall
   /setwarp pvp
   /setwarp mining
   ```

3. **List all warps**:
   ```bash
   /warps
   ```

Players can then use `/warp mall` to teleport to warps.

## 🎮 GUI System Quick Setup

### Opening GUI Menus

The new GUI system provides intuitive interfaces for common server functions:

1. **Shop Interface**:
   ```bash
   /shop
   ```
   Opens a categorized shop where players can buy and sell items with visual categories and pricing.

2. **Kit Selection**:
   ```bash
   /kits
   ```
   Browse available kits with descriptions, cooldowns, and requirements.

3. **Player Statistics**:
   ```bash
   /stats
   ```
   View comprehensive player stats including playtime, deaths, kills, and more.

4. **Warp Destinations**:
   ```bash
   /warps
   ```
   Visual warp browser with descriptions and quick teleportation.

### Personalizing Your Experience

Players can customize their GUI experience with themes:

```bash
/gui theme list        # Show available themes
/gui theme dark        # Switch to dark theme  
/gui theme ocean       # Switch to ocean theme
/gui theme default     # Return to default theme
```

### Quick Shop Configuration (Admin)

1. **Add items to shop** by editing `config/gui/shop_gui.json`:
   ```json
   "diamond_sword": {
     "item": "minecraft:diamond_sword",
     "name": "§b💎 Diamond Sword",
     "price": 500,
     "category": "weapons",
     "lore": [
       "§7A powerful diamond sword",
       "§6Price: §f500 coins"
     ]
   }
   ```

2. **Reload shop configuration**:
   ```bash
   /neoessentials gui reload shop
   ```

### GUI Permissions

Grant GUI access permissions:

```yaml
# Basic GUI permissions
neoessentials.gui.shop      # Access to shop
neoessentials.gui.kits      # Access to kits
neoessentials.gui.stats     # View statistics
neoessentials.gui.warps     # Access warps GUI

# Advanced permissions  
neoessentials.gui.theme.change  # Change themes
neoessentials.gui.admin         # Admin panel access
neoessentials.gui.economy       # Economy management
```

## 📊 Bossbar System

### Quick Bossbar Setup

1. **Test the bossbar system**:
   ```bash
   /bossbar show welcome "Welcome to {server_name}!" BLUE SOLID
   ```

2. **Create a permanent server info bar**:
   ```bash
   /bossbar broadcast server_info "Players: {player_count}/{max_players} | TPS: {tps}" GREEN SOLID 300
   ```

3. **Hide a bossbar**:
   ```bash
   /bossbar hide welcome
   ```

### Bossbar Templates

Create reusable bossbar templates in `config/neoessentials/bossbars/`:

**welcome.json**:
```json
{
  "text": "Welcome to {server_name}, {player_name}!",
  "color": "BLUE",
  "style": "SOLID",
  "duration": 10
}
```

Use with: `/bossbar template welcome PlayerName`

## 🔒 Basic Security

### Enable Security Features

Edit `config/neoessentials/security.toml`:

```toml
[security.detection]
enabled = true

[security.detection.commands]
maxCommandsPerSecond = 5

[security.rateLimiting]
enabled = true

[security.logging]
enabled = true
```

### Monitor Security

1. **Check security status**:
   ```bash
   /security status
   ```

2. **View security stats**:
   ```bash
   /security stats
   ```

3. **Monitor specific player**:
   ```bash
   /security status PlayerName
   ```

## 🌐 Testing Your Setup

### Verification Checklist

**✅ Commands Working**:
- [ ] `/heal` restores health
- [ ] `/feed` restores hunger  
- [ ] `/spawn` teleports to spawn
- [ ] `/fly` toggles flight

**✅ GUI System Working**:
- [ ] `/shop` opens shop interface
- [ ] `/kits` displays available kits  
- [ ] `/stats` shows player statistics
- [ ] `/warps` opens warp browser
- [ ] `/gui theme dark` changes to dark theme

**✅ Permissions Working**:
- [ ] Default players can use basic commands
- [ ] Admins can use all commands
- [ ] VIP players have enhanced permissions
- [ ] GUI access permissions work correctly

**✅ Teleportation Working**:
- [ ] `/sethome` and `/home` work
- [ ] `/back` returns to previous location
- [ ] Warps can be created and used
- [ ] GUI warp browser functions

**✅ Features Working**:
- [ ] Bossbar displays correctly
- [ ] Security monitoring active
- [ ] Configuration loads without errors
- [ ] GUI configurations load properly

### Common Test Commands

Run these to verify everything works:

```bash
# Test basic functionality
/heal
/feed
/speed 2
/fly

# Test GUI system
/shop              # Open shop interface
/kits              # Open kits menu
/stats             # View player stats  
/warps             # Open warp browser
/gui theme dark    # Test theme changing
/gui theme default # Return to default

# Test teleportation
/sethome test
/spawn
/home test
/back

# Test bossbar
/bossbar show test "Testing: {player_name}" RED SOLID 5

# Test GUI admin functions (as admin)
/admin             # Open admin panel
/neoessentials gui reload  # Reload GUI configs

# Test permissions (as admin)
/permissions info
/permissions user TestPlayer group set vip

# Test placeholders
/placeholder test {player_name}
/placeholder test {server_tps}
```

## 🛠️ Configuration Tips

### Performance Optimization

For better performance on larger servers:

```toml
# In neoessentials-general.toml
[performance]
enableAsyncProcessing = true
cacheSize = 1000
tickInterval = 20

# In security.toml
[security.performance]
maxCpuUsage = 10  # Limit security system CPU usage
detectionInterval = 2000  # Check less frequently
```

### Resource Management

```toml
# In commands.toml
[commands.cooldowns]
globalCooldown = 1  # 1 second between commands
heal = 30          # 30 second cooldown for /heal
feed = 30          # 30 second cooldown for /feed
```

### Chat & Messaging

```toml
# In general.toml
[messaging]
prefix = "&8[&6NeoEssentials&8]&r"
useActionBar = true
enableSounds = true
```

## 🚨 Troubleshooting

### Common Issues

#### Commands Not Working
**Problem**: Commands not recognized  
**Solution**: 
1. Check mod is installed correctly
2. Verify Java 21+ is being used
3. Check server logs for errors
4. Try restarting server

#### Permission Errors
**Problem**: "You don't have permission"  
**Solution**:
1. Check your permission group: `/permissions info`
2. Set yourself as admin: `/permissions user YourName group set admin`
3. Give yourself permissions: `/permissions user YourName permission add essentials.*`

#### Configuration Issues
**Problem**: Settings not applying  
**Solution**:
1. Check TOML syntax is correct
2. Restart server after changes
3. Use `/neoessentials reload` to reload config
4. Check logs for configuration errors

#### Performance Problems
**Problem**: Server lag with NeoEssentials  
**Solution**:
1. Reduce security monitoring frequency
2. Lower cache sizes in config
3. Disable unused features
4. Monitor with `/security stats`

### Getting Help

If you need additional help:

1. **Check the logs**: Look in `logs/latest.log` for error messages
2. **Read the documentation**: See other wiki pages for detailed information
3. **Test in isolation**: Try commands one by one to identify issues
4. **Check permissions**: Most issues are permission-related

### Debug Commands

For troubleshooting:

```bash
# Check mod status
/neoessentials info

# Reload configuration
/neoessentials reload

# Test specific features
/placeholder test {player_name}
/security debug
/permissions debug YourUsername essentials.heal
```

## 🎯 Next Steps

### Explore Advanced Features

Once you have the basics working:

1. **[GUI System](GUI-System)**: Complete GUI customization and configuration
2. **[Essential Commands](Essential-Commands)**: Learn all available commands
3. **[Configuration](Configuration)**: Detailed configuration options
4. **[Permissions](Permissions)**: Advanced permission management
5. **[Bossbar System](Bossbar)**: Create advanced bossbar displays
6. **[Security Features](Security)**: Set up comprehensive server protection
7. **[Teleportation](Teleportation)**: Advanced teleportation features

### Customize for Your Server

- **Set up custom warps** for your server's unique locations
- **Create custom bossbar templates** for announcements
- **Configure permission groups** that match your server's hierarchy
- **Adjust security settings** based on your server's needs
- **Set up economy integration** if using economy plugins

### Community & Support

- **Share your configurations** with other server administrators
- **Report bugs or request features** through GitHub issues
- **Contribute to the project** by submitting improvements
- **Help other users** by sharing your experience

---

**Congratulations!** 🎉 You now have NeoEssentials running on your server. Your players can enjoy essential commands, teleportation, and enhanced server features while you benefit from comprehensive administration tools and security features.

**Related Documentation**: [Installation](Installation) | [Essential Commands](Essential-Commands) | [Configuration](Configuration)

*Last Updated: August 6, 2025*
