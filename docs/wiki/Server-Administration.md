# Server Administration

NeoEssentials provides essential server administration tools for managing your Minecraft server. This documentation covers the administrative commands and server control features that are currently implemented in the mod.

## 🖥️ Server Control Commands

### World Management

#### Time Control

Control world time with comprehensive time management commands:

```bash
/time set <preset|value>       # Set world time to preset or specific value
/time add <value>              # Add time to current world time
/time query daytime            # Query current day time
/time query gametime           # Query total game time
```

**Time Presets:**
- `day` or `1000` - Daytime (morning)
- `noon` or `6000` - Noon (midday)  
- `night` or `13000` - Nighttime (evening)
- `midnight` or `18000` - Midnight
- Custom values: `0-24000` (full day cycle)

**Examples:**
```bash
/time set day                  # Set time to day (1000 ticks)
/time set 12000               # Set time to specific value
/time add 6000                # Add 6000 ticks to current time
/time query daytime           # Check current day time
```

#### Weather Control

Manage world weather conditions:

```bash
/weather clear [duration]      # Clear weather
/weather rain [duration]       # Start rain
/weather thunder [duration]    # Start thunderstorm
```

**Duration Options:**
- No duration: Default duration (600 seconds)
- Custom duration: Specify time in seconds
- Maximum duration: 1,000,000 seconds

**Examples:**
```bash
/weather clear                # Clear weather (default duration)
/weather rain 300             # Rain for 300 seconds
/weather thunder 600          # Thunderstorm for 600 seconds
```

### Player Management

Basic player administration commands:

#### Game Mode Management
```bash
/gamemode <mode> [player]     # Change game mode
/gm <mode> [player]           # Alias for gamemode
/gmc [player]                 # Creative mode shortcut
/gms [player]                 # Survival mode shortcut  
/gma [player]                 # Adventure mode shortcut
/gmsp [player]                # Spectator mode shortcut
```

**Game Mode Options:**
- `survival` or `0` - Survival mode
- `creative` or `1` - Creative mode
- `adventure` or `2` - Adventure mode
- `spectator` or `3` - Spectator mode

#### Player Control
```bash
/kick <player> [reason]       # Kick player from server
/ban <player> [reason]        # Ban player from server
/heal <player>                # Heal player to full health
/feed <player>                # Feed player to full hunger
```

### Server Information

#### System Information
```bash
/info                         # Display comprehensive server information
```

**Information Displayed:**
- Server version and MOTD
- Player count (online/max)
- Memory usage and percentage
- Current world day
- Game rule status (keepInventory, mobGriefing, etc.)
- Difficulty level
- Server tick count
- NeoEssentials version

#### Configuration Management
```bash
/config                       # Basic configuration command
```

**Note**: Configuration features are limited in the current implementation.

## 📊 Performance Monitoring

### Performance Commands

Monitor and manage server performance:

```bash
/performance                  # Show comprehensive performance statistics
/performance stats            # Display performance metrics
/performance memory           # Show detailed memory information
/performance cache            # Show cache statistics
/performance clear            # Clear performance cache
/performance gc               # Force garbage collection
/performance monitoring <on|off> # Enable/disable monitoring
```

#### Performance Statistics

**System Metrics:**
- **Memory Usage**: Current heap usage, free memory, and usage percentage
- **Cache Performance**: Cache size and hit rates
- **Command Statistics**: Execution times and frequency
- **Thread Information**: Executor statistics and thread usage

**Example Output:**
```
=== Performance Statistics ===
Memory Usage: 65.2%
Cache Size: 147
Average Command Time: 2.3ms
Total Commands: 1,524
Cache Hit Rate: 87.4%

Top Commands:
1. /heal - 234 executions
2. /feed - 187 executions
3. /home - 156 executions
```

#### Memory Management

**Memory Information:**
```bash
/performance memory           # Detailed memory breakdown
```

**Memory Status Indicators:**
- **✅ Healthy**: < 70% memory usage
- **⚠️ Moderate**: 70-85% memory usage
- **❌ High**: > 85% memory usage (optimization recommended)

**Garbage Collection:**
```bash
/performance gc               # Force garbage collection
```

**Features:**
- Memory usage before and after GC
- Amount of memory freed
- Performance impact analysis

### Cache Management

```bash
/performance cache            # Show cache statistics
/performance clear            # Clear all performance caches
```

**Cache Information:**
- Cache size and capacity
- Hit rates and performance impact
- Memory usage by cache system
- Cache cleanup and optimization

## ⚙️ Configuration

### Basic Settings

NeoEssentials integrates with standard server configuration through TOML files:

```
config/
├── neoessentials-common.toml     # Common server settings
├── neoessentials-general.toml    # General configuration
└── neoessentials/               # Detailed configurations
    ├── main.json                # Core settings
    ├── permissions.json         # Permission configuration
    └── other config files...
```

### Permission Requirements

Administrative commands require appropriate permissions:

```yaml
# World management
neoessentials.time               # Time control commands
neoessentials.weather            # Weather control commands

# Player management  
neoessentials.gamemode           # Change own gamemode
neoessentials.gamemode.others    # Change other players' gamemode
neoessentials.kick               # Kick players
neoessentials.ban                # Ban players
neoessentials.heal.others        # Heal other players
neoessentials.feed.others        # Feed other players

# Performance monitoring
neoessentials.performance        # Basic performance commands
neoessentials.admin.performance  # Advanced performance management

# System information
neoessentials.info               # Server information command
neoessentials.config             # Configuration access
```

## 🛠️ Administrative Workflow

### Daily Administration Tasks

1. **Monitor Server Health**:
   ```bash
   /info                         # Check overall server status
   /performance stats            # Review performance metrics
   ```

2. **Manage World Settings**:
   ```bash
   /time set day                 # Reset time if needed
   /weather clear               # Clear bad weather
   ```

3. **Handle Player Issues**:
   ```bash
   /heal PlayerName             # Help players with health issues
   /gamemode survival PlayerName # Fix gamemode problems
   ```

4. **Performance Maintenance**:
   ```bash
   /performance memory          # Check memory usage
   /performance gc              # Clean up memory if needed
   /performance clear           # Clear caches periodically
   ```

### Performance Optimization

#### Memory Management
- Run `/performance memory` regularly to monitor usage
- Use `/performance gc` when memory usage exceeds 80%
- Clear caches with `/performance clear` during low-activity periods

#### Command Monitoring
- Review command statistics with `/performance stats`
- Identify frequently used commands for optimization
- Monitor command execution times for performance issues

### Troubleshooting

#### Common Issues

**Performance Problems:**
1. Check memory usage: `/performance memory`
2. Review command performance: `/performance stats`
3. Force garbage collection: `/performance gc`
4. Clear caches: `/performance clear`

**World Issues:**
1. Check server info: `/info`
2. Verify game rules and settings
3. Reset time/weather if needed

**Player Problems:**
1. Use `/heal` and `/feed` for player health issues
2. Correct gamemode with `/gamemode` commands
3. Use moderation commands (`/kick`, `/ban`) for problem players

#### Debug Information

**System Status:**
- Server version and configuration
- Memory usage and available resources
- Active game rules and world settings
- Player count and connection status

**Performance Analysis:**
- Command execution statistics
- Memory usage patterns
- Cache performance metrics
- Thread and executor status

## 🔧 Limitations & Recommendations

### Current Limitations

**Missing Features:**
- **Server restart/stop commands**: Use your server control panel or console
- **Advanced TPS monitoring**: Consider dedicated performance plugins
- **Automated scheduling**: Use external cron jobs or server management tools
- **Advanced player analytics**: Limited to basic information

**Workarounds:**
- **Server Control**: Use your hosting panel or direct console access
- **Advanced Monitoring**: Combine with external monitoring tools
- **Automation**: Use server management scripts outside of NeoEssentials
- **Backup Management**: Use dedicated backup plugins or scripts

### Recommended Setup

**For Basic Administration:**
- Use NeoEssentials for world and player management
- Monitor performance with built-in tools
- Handle configuration through TOML files

**For Advanced Administration:**
- Combine NeoEssentials with dedicated server management tools
- Use external monitoring for comprehensive analytics
- Implement automated scripts for advanced scheduling
- Consider additional plugins for specialized features

---

**Related Documentation**: [Essential Commands](Essential-Commands.md) | [Permissions](Permissions.md) | [Performance](Performance.md) | [Configuration](Configuration.md)

*Last Updated: August 9, 2025*
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
