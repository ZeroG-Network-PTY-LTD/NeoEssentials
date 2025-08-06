# Server Administration

NeoEssentials provides comprehensive server administration tools for managing and maintaining your Minecraft server. This documentation covers all administrative commands, server control features, and advanced management capabilities.

## 🖥️ Server Control Commands

### World Management

#### Time Control
```bash
/time set <time>                # Set world time
/time add <time>                # Add time to current
/day                           # Set time to day (1000)
/night                         # Set time to night (13000)
/sunrise                       # Set time to sunrise (6000)
/sunset                        # Set time to sunset (18000)
```

**Time Values:**
- `day` or `1000` - Daytime
- `night` or `13000` - Nighttime  
- `noon` or `6000` - Noon
- `midnight` or `18000` - Midnight
- Custom values: `0-24000`

#### Weather Control
```bash
/weather <type> [duration]      # Set weather
/sun [duration]                # Clear weather
/rain [duration]               # Set rain
/thunder [duration]            # Set thunderstorm
```

**Examples:**
```bash
/weather clear 1000            # Clear weather for 1000 ticks
/rain 600                      # Rain for 600 seconds
/thunder                       # Toggle thunderstorm
```

#### World Properties
```bash
/gamerule <rule> <value>       # Set game rules
/difficulty <level>            # Set difficulty
/worldborder <command>         # Manage world border
/seed                          # Display world seed
```

**Common Game Rules:**
```bash
/gamerule keepInventory true   # Keep items on death
/gamerule doDaylightCycle false # Stop day/night cycle
/gamerule doMobSpawning false  # Disable mob spawning
/gamerule announceAdvancements false # Disable advancement announcements
```

### Server Control

#### Server Management
```bash
/restart [delay] [reason]      # Restart server with countdown
/stop [reason]                 # Stop server gracefully
/reload                        # Reload server plugins/configs
/save-all                      # Force save all worlds
/save-off                      # Disable auto-saving
/save-on                       # Enable auto-saving
```

#### Player Management
```bash
/list                          # List online players
/whitelist <add/remove> <player> # Manage whitelist
/op <player>                   # Give operator permissions
/deop <player>                 # Remove operator permissions
/pardon <player>               # Remove player from ban list
```

#### Memory & Performance
```bash
/gc                            # Force garbage collection
/lag                           # Check server performance
/tps                           # View server TPS (ticks per second)
/memory                        # View memory usage
/entities                      # Count entities per world
```

## 🎛️ Admin GUI System

### Admin Control Panel
```bash
/admin                         # Open main admin panel
```

**Panel Features:**
- **Server Status** - TPS, memory usage, uptime
- **Player Management** - Online players, quick actions
- **World Control** - Time, weather, game rules
- **Server Tools** - Restart, reload, maintenance mode
- **Configuration** - Quick config access

### Server Management GUI
```bash
/servermanager                 # Open server management interface
```

**Management Sections:**
- **Performance Monitoring** - Real-time server metrics
- **World Management** - Multi-world administration
- **Plugin Control** - Enable/disable features
- **Maintenance Tools** - Backup, cleanup, optimization
- **Scheduling** - Automated server tasks

## 📊 Monitoring & Analytics

### Performance Monitoring
```bash
/performance                   # Open performance dashboard
/tps                          # Current TPS information
/lag analyze                  # Analyze lag sources
/memory detailed              # Detailed memory breakdown
/cpu usage                    # CPU usage statistics
```

**Performance Metrics:**
- **TPS (Ticks Per Second)** - Server performance indicator
- **Memory Usage** - RAM consumption and allocation
- **CPU Usage** - Processor utilization
- **Entity Count** - Living entities per world
- **Chunk Loading** - Active chunks and loading times

### Server Statistics
```bash
/stats server                 # Server-wide statistics
/uptime                       # Server uptime information
/playercount                  # Player count over time
/activity                     # Server activity metrics
```

**Statistics Include:**
- **Uptime Statistics** - Current session and historical
- **Player Activity** - Join/leave patterns, peak times
- **Command Usage** - Most used commands and features
- **Resource Usage** - Historical performance data
- **Error Tracking** - System errors and warnings

## 🔧 Configuration Management

### Live Configuration
```bash
/config reload                # Reload all configurations
/config reload <section>      # Reload specific section
/config set <key> <value>     # Set configuration value
/config get <key>             # Get configuration value
/config validate              # Validate all configurations
```

**Examples:**
```bash
/config reload security       # Reload security settings
/config set features.teleportation true
/config get server.maxPlayers
```

### Feature Management
```bash
/features list                # List all features and status
/features enable <feature>    # Enable specific feature
/features disable <feature>   # Disable specific feature
/features reload <feature>    # Reload specific feature
```

**Available Features:**
- `essentialCommands` - Core utility commands
- `teleportation` - Home/warp system
- `security` - Security monitoring
- `bossbar` - Bossbar system
- `gui` - GUI interfaces
- `notifications` - Notification system

## 💾 Backup & Maintenance

### Backup System
```bash
/backup create [name]         # Create manual backup
/backup list                  # List available backups
/backup restore <name>        # Restore from backup
/backup schedule              # View backup schedule
/backup cleanup               # Clean old backups
```

**Automated Backups:**
- **Scheduled Backups** - Daily, weekly, monthly schedules
- **Pre-restart Backups** - Automatic backup before restarts
- **Player-triggered** - Backups on significant events
- **Incremental Backups** - Save only changed files

### Maintenance Mode
```bash
/maintenance enable [reason]  # Enable maintenance mode
/maintenance disable          # Disable maintenance mode
/maintenance status           # Check maintenance status
/maintenance message <text>   # Set maintenance message
```

**Maintenance Features:**
- **Player Restriction** - Prevent non-admin logins
- **Custom Messages** - Informative maintenance messages
- **Scheduled Maintenance** - Automatic maintenance windows
- **Grace Period** - Allow current players to finish

### Database Management
```bash
/database backup              # Backup player data
/database cleanup             # Clean old/orphaned data
/database optimize            # Optimize database tables
/database migrate             # Migrate data format
/database verify              # Verify data integrity
```

## 🚨 Security Administration

### Security Dashboard
```bash
/security dashboard           # Open security overview
/security threats             # View current threats
/security logs                # View security logs
/security settings            # Modify security settings
```

### IP Management
```bash
/ipmanager                    # Open IP management GUI
/ipban <ip> [reason]          # Ban IP address
/ipunban <ip>                 # Unban IP address
/ipwhitelist <ip>             # Whitelist IP address
/ipinfo <ip>                  # Get IP information
```

### Security Monitoring
```bash
/security monitor <player>    # Monitor specific player
/security profile <player>    # View player security profile
/security alerts              # Configure security alerts
/security response <action>   # Configure automatic responses
```

## 📡 Network & Communication

### Announcement System
```bash
/broadcast <message>          # Broadcast to all players
/announce <title> <message>   # Send title announcement
/motd set <message>           # Set message of the day
/welcome set <message>        # Set welcome message
```

**Announcement Types:**
- **Chat Broadcasts** - Traditional chat messages
- **Title Announcements** - Full-screen title/subtitle
- **Bossbar Announcements** - Persistent bossbar messages
- **Action Bar** - Messages above hotbar

### Communication Tools
```bash
/mail send <player> <message> # Send mail to offline player
/mail read [player]           # Read mail messages
/mail clear [player]          # Clear mail messages
/helpop <message>             # Send help request to staff
```

## 🎯 Advanced Administration

### Multi-World Management
```bash
/world list                   # List all worlds
/world create <name> <type>   # Create new world
/world delete <name>          # Delete world
/world load <name>            # Load world
/world unload <name>          # Unload world
/world tp <world>             # Teleport to world spawn
```

### Plugin Integration
```bash
/plugins                      # List loaded plugins
/plugin enable <name>         # Enable plugin
/plugin disable <name>        # Disable plugin
/plugin reload <name>         # Reload plugin
/version [plugin]             # Show version information
```

### Economy Administration
```bash
/economy status               # Economy system status
/economy set <player> <amount> # Set player balance
/economy give <player> <amount> # Give money to player
/economy take <player> <amount> # Take money from player
/economy reset <player>       # Reset player balance
/economy top                  # Richest players leaderboard
```

## 📈 Reporting & Analytics

### Generate Reports
```bash
/report generate daily        # Daily activity report
/report generate weekly       # Weekly summary report
/report generate monthly      # Monthly analytics
/report export <format>       # Export data (JSON, CSV)
/report schedule              # Schedule automatic reports
```

**Report Contents:**
- **Player Activity** - Joins, leaves, playtime
- **Command Usage** - Most used commands and features
- **Performance Metrics** - TPS, memory, CPU over time
- **Security Events** - Threats, violations, responses
- **Economy Activity** - Transactions, balance changes

### Data Export
```bash
/export players               # Export player data
/export configurations        # Export current configs
/export logs                  # Export log files
/export statistics            # Export server statistics
```

## ⚙️ Configuration Options

### Server Administration Settings
Configure in `config/neoessentials/server-administration.toml`:

```toml
[administration]
# Enable admin features
enabled = true

# Allow remote administration
allowRemote = false

# Require confirmation for destructive actions
requireConfirmation = true

[performance]
# Monitor server performance
monitoring = true

# Performance alert thresholds
tpsWarning = 15.0
memoryWarning = 80

[backup]
# Enable automatic backups
enabled = true

# Backup frequency
frequency = "daily"

# Maximum backup count
maxBackups = 7

[security]
# Enable security monitoring
enabled = true

# Log admin actions
logAdminActions = true

# Require secure authentication
requireSecureAuth = false
```

### GUI Administration Settings
Configure admin interfaces in `config/gui/admin_gui.json`:

```json
{
  "admin_panel": {
    "title": "§c§lServer Administration",
    "sections": {
      "server_control": {
        "name": "§6Server Control",
        "commands": [
          "restart", "stop", "reload"
        ]
      },
      "world_management": {
        "name": "§aWorld Management", 
        "commands": [
          "time", "weather", "gamerule"
        ]
      }
    }
  }
}
```

## 🛠️ Automation & Scheduling

### Scheduled Tasks
```bash
/schedule list                # List scheduled tasks
/schedule create <task>       # Create new scheduled task
/schedule delete <id>         # Delete scheduled task
/schedule enable <id>         # Enable scheduled task
/schedule disable <id>        # Disable scheduled task
```

**Common Scheduled Tasks:**
- **Daily Restart** - Automatic server restart
- **World Cleanup** - Remove unused chunks/entities
- **Player Cleanup** - Clean inactive player data
- **Backup Creation** - Automated backup generation
- **Performance Reports** - Regular performance analysis

### Auto-moderation
```bash
/automod enable               # Enable auto-moderation
/automod configure            # Configure auto-mod rules
/automod status               # Check auto-mod status
/automod logs                 # View auto-mod actions
```

## 🔧 Troubleshooting

### Common Administrative Issues

#### Performance Problems
- **High Memory Usage** - Use `/gc` and `/memory` to diagnose
- **Low TPS** - Check `/lag analyze` for lag sources
- **Entity Lag** - Use `/entities` to find problem areas

#### Configuration Issues
- **Invalid Settings** - Use `/config validate` to check
- **Feature Not Working** - Verify feature is enabled
- **Permission Problems** - Check admin permissions

#### Database Problems
- **Data Corruption** - Use `/database verify` to check
- **Slow Queries** - Use `/database optimize` to improve
- **Connection Issues** - Check database configuration

### Debug Tools
```bash
/debug server                 # Server debug information
/debug performance            # Performance debugging
/debug config                 # Configuration debugging
/debug database               # Database debugging
```

## 📚 Related Documentation

- **[Player Management](Player-Management.md)** - Player administration tools
- **[Security Features](Security.md)** - Security system management
- **[Configuration](Configuration.md)** - Detailed configuration options
- **[Performance](Performance.md)** - Performance optimization guide

---

*Last Updated: August 6, 2025*
