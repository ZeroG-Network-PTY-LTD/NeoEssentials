# NeoEssentials Enhanced Quick Reference Guide

Quick reference for NeoEssentials commands, features, and enhanced customization options.

## 🎨 **Enhanced Display Features**

### **Tablist & Scoreboard Commands**
```bash
# Apply themes to displays
/tablist theme <modern|classic|minimalist|default> [player]
/scoreboard theme <info|stats|economy> [player]
/display theme <theme> [player]                    # Apply to both

# Customize content
/tablist header "§6§lWelcome to {server_name}!"
/tablist footer "§7TPS: §a{server_tps} §7| Players: §a{server_players}"

# Management commands  
/tablist refresh [player]                          # Force refresh
/scoreboard update [player]                        # Update content
/display reset [player]                            # Reset to default
```

### **Enhanced Bossbar Commands**
```bash
# Theme-based bossbars
/bossbar themed <template> <theme> [player] [duration]
/bossbar show health modern Steve 30

# Multiple bossbars per player
/bossbar multi welcome serverinfo health [player]

# Standard enhanced commands
/bossbar show <template> [theme] [player] [duration]
/bossbar broadcast <template> <duration>
/bossbar update <text> <progress> [player]
```

## 🎭 **Available Themes**

### **Tablist/Scoreboard Themes**
- **default** - Clean white and gray styling: `§f§l{title}` 
- **modern** - Blue with symbols: `§b§l► {title} ◄`
- **classic** - Gold traditional: `§6§l═══ {title} ═══`
- **minimalist** - Simple styling: `§f{title}`

### **Bossbar Themes**  
- **default** - Standard white progress bar
- **modern** - Blue with arrow decorations
- **classic** - Gold with equals decorations  
- **minimalist** - Simple white styling

## 🎮 **Essential Player Commands**

### **Player Utilities**
```bash
/heal [player]          - Restore health and hunger
/feed [player]          - Restore hunger and saturation  
/god [player]           - Toggle invincibility
/vanish [player]        - Toggle invisibility
/fly [player]           - Toggle flight mode
/speed <type> <1-10>    - Set movement speed
/repair [hand/all]      - Repair items
/workbench              - Open crafting table
/back                   - Return to previous location
```

### **Teleportation**
```bash
/home [name]            - Teleport to home
/sethome [name]         - Set home location
/delhome <name>         - Delete home
/homes                  - List all homes

/warp <name>            - Teleport to warp
/warps                  - List all warps

/spawn                  - Go to spawn
/tpa <player>          - Request teleport to player
/tpaccept              - Accept teleport request
```

### **Economy System**
```bash
/balance               - Check your balance
/pay <player> <amount> - Send money to player
/baltop                - View balance leaderboard

# Admin Commands:
/eco give <player> <amount>  - Give money
/eco set <player> <amount>   - Set balance
```

## 🏷️ **Enhanced Placeholders**

### **Animation Placeholders**
```
{animated_title}        - Cycling welcome titles
{animated_subtitle}     - Cycling welcome subtitles
{color_cycle}           - Rotating color codes
{rainbow_text}          - Rainbow text effects
```

### **Display Placeholders**
```
{tablist_theme}         - Current tablist theme name
{scoreboard_theme}      - Current scoreboard theme
{player_ping_color}     - Color-coded ping display
{player_health_percent} - Health as percentage
```

### **Enhanced Health & Status**
```
{player_health}         - Current health (20.0)
{player_max_health}     - Maximum health (20.0)
{player_health_percent} - Health percentage (100)
{player_food}           - Hunger level (20)
```

### **Server & World Data**
```
{server_name}           - Server name
{server_players}        - Current player count
{server_max_players}    - Maximum players
{server_tps}            - Ticks per second
{server_memory_percent} - Memory usage percentage
{world_time}            - Minecraft world time
{time}                  - Current real time (HH:mm)
```

## 📋 **Template & Theme Examples**

### **Multi-Bossbar Setup**
```bash
# Welcome sequence with multiple bossbars
/bossbar multi animated_welcome serverinfo health Steve

# Health monitoring with modern theme
/bossbar themed health modern @a 60

# Server info with classic theme
/bossbar themed serverinfo classic @a 30
```

### **Themed Tablist Setup**
```bash
# Modern server info display
/tablist theme modern @a
/tablist header "§b§l❖ {server_name} ❖" @a
/tablist footer "§fTPS: §a{server_tps} §7| §fHealth: §c{player_health_percent}%" @a

# Classic style setup  
/tablist theme classic @a
/tablist header "§6§l「 Welcome Players 」" @a
```

### **Animated Scoreboard**
```bash
# Stats display with animations
/scoreboard theme stats @a    # Shows animated content automatically

# Economy scoreboard
/scoreboard theme economy @a  # Displays balance and economy info
```

## 🎯 **Common Use Cases**

### **New Player Welcome Experience**
```bash
# Complete welcome setup
/bossbar themed animated_welcome modern {player} 15
/tablist theme modern {player}
/scoreboard theme info {player}
```

### **Server Event Announcement**
```bash
# Event theme for all players
/display theme classic @a
/bossbar broadcast event 60
/tablist header "§6§lPvP Tournament - Join Now!" @a
```

### **VIP Player Experience**
```bash
# Special VIP styling
/display theme modern {vip_player}
/tablist header "§6§l★ VIP ★ Welcome {player_name}!" {vip_player}
/bossbar themed health modern {vip_player} 300
```

### **Health Monitoring Setup**
```bash
# Real-time health display for all players
/bossbar themed health minimalist @a 600
/scoreboard theme stats @a
```

## 🛡️ **Enhanced Permissions**

### **Theme Permissions**
```
# Tablist themes
neoessentials.theme.modern
neoessentials.theme.classic  
neoessentials.theme.minimalist

# Scoreboard themes
neoessentials.theme.info
neoessentials.theme.stats
neoessentials.theme.economy

# Display management
neoessentials.display.theme
neoessentials.display.reset
```

### **Enhanced Bossbar Permissions**
```
neoessentials.bossbar.themed    - Use themed bossbars
neoessentials.bossbar.multi     - Multiple bossbars
neoessentials.bossbar.show      - Show bossbars
neoessentials.bossbar.broadcast - Broadcast to all
```

### **Basic Player Permissions**
```
neoessentials.player.*          - All basic player commands
neoessentials.home.*            - Home system access
neoessentials.economy.use       - Economy system access
neoessentials.tablist.*         - Tablist management
neoessentials.scoreboard.*      - Scoreboard access
```

## ⚙️ **Quick Configuration Setup**

### **Enable Enhanced Features**
```toml
[tablist]
enabled = true
defaultTheme = "modern"
multiTheme = true
updateInterval = 5

[scoreboard]
enabled = true
defaultTheme = "info"
enableAnimations = true
updateInterval = 3

[bossbar]
enabled = true
maxPerPlayer = 3
enableTemplates = true
defaultUpdateInterval = 2
```

### **Performance Optimization**
```toml
# For servers with 50+ players
[performance]
tablist.updateInterval = 8
scoreboard.updateInterval = 6
bossbar.defaultUpdateInterval = 5
maxThemesPerPlayer = 2
asyncUpdates = true

[cache]
themeCacheTime = 300
enableCaching = true
```

## 🔧 **Troubleshooting Quick Fixes**

### **Display Not Updating**
```bash
/tablist refresh @a             # Refresh all tablists
/scoreboard update @a           # Update all scoreboards
/bossbar update "Refreshed" 100 @a  # Update all bossbars
```

### **Reset Everything**
```bash
/display reset @a               # Reset displays to default
/bossbar hide @a               # Hide all bossbars
```

### **Test Enhanced Features**
```bash
# Test placeholders
/placeholder test "{animated_title} - {player_health_percent}%"

# Test themes
/tablist theme modern
/scoreboard theme stats  
/bossbar themed health modern
```

## 🚀 **Quick Setup Guide**

### **Basic Enhanced Setup**
```bash
1. Enable enhanced features in config
2. /tablist theme modern @a
3. /scoreboard theme info @a  
4. /bossbar themed welcome modern @a 10
5. Test with /display reset @a
```

### **Advanced Multi-Display Setup**
```bash
1. Configure themes and animations
2. /display theme modern @a
3. /bossbar multi welcome serverinfo health @a
4. Set custom headers/footers
5. Monitor with /neostatus performance
```

## 📊 **Available Bossbar Templates**

### **Enhanced Templates**
```
welcome              - Animated welcome experience
animated_welcome     - Multi-frame welcome sequence
serverinfo          - Real-time server statistics  
health              - Player health monitoring
progress            - Progress indicators
event               - Event announcements
warning             - Warning messages
```

### **Usage Examples**
```bash
/bossbar show welcome modern Steve 10
/bossbar themed health classic @a 60
/bossbar multi serverinfo health progress Steve
```

---

## 📱 **Pro Tips**

### **For Players**
1. Use `/help` to explore available commands
2. Try different themes: `/tablist theme modern`
3. Monitor your health: `/bossbar show health`
4. Check server stats: `/scoreboard theme info`

### **For Admins**  
1. Test all themes before applying to players
2. Use performance monitoring: `/neostatus`
3. Configure appropriate update intervals
4. Monitor memory usage with enhanced features

### **Performance Tips**
1. Use higher update intervals for large servers
2. Enable caching for better performance
3. Limit themes per player for stability
4. Monitor server TPS when using animations

---

**🎉 NeoEssentials Enhanced - Complete Server Customization!**

*Quick Reference for Enhanced Features - Updated August 5, 2025*
