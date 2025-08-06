# Notification System

NeoEssentials features a comprehensive notification system that supports multiple delivery channels, customizable templates, and intelligent message routing. This system ensures important information reaches players through their preferred communication methods.

## 📢 Notification Channels

### Chat Notifications
Traditional chat-based notifications with advanced formatting:

```bash
/notify chat <player> <message>    # Send chat notification
/broadcast <message>               # Broadcast to all players
/announce <title> <message>        # Send formatted announcement
```

**Features:**
- **Rich Formatting** - Colors, styles, hover text, click actions
- **Sound Integration** - Optional sound effects with notifications
- **Prefix Support** - Customizable notification prefixes
- **Channel Filtering** - Players can filter notification types

### Action Bar Notifications
Temporary messages displayed above the player's hotbar:

```bash
/actionbar <player> <message>      # Send action bar message
/actionbar all <message>           # Send to all players
/actionbar timed <player> <time> <message> # Timed message
```

**Use Cases:**
- **Status Updates** - Health, hunger, coordinates
- **Progress Indicators** - Task completion, cooldowns
- **Quick Alerts** - Warning messages, confirmations
- **Real-time Data** - Server TPS, player count

### Bossbar Notifications
Persistent notifications using boss health bars:

```bash
/bossbar notify <player> <message> # Send bossbar notification
/bossbar broadcast <message>       # Broadcast bossbar message
/bossbar template <name> <player>  # Use notification template
```

**Advantages:**
- **High Visibility** - Prominent display at top of screen
- **Persistent Display** - Stays visible for set duration
- **Color Coding** - Different colors for different message types
- **Progress Bars** - Visual progress indicators

### Title/Subtitle Notifications
Full-screen notifications for important messages:

```bash
/title <player> <title> [subtitle] # Send title notification
/subtitle <player> <subtitle>      # Send subtitle only
/title all <title> [subtitle]      # Send to all players
```

**Best For:**
- **Welcome Messages** - Player join notifications
- **Important Alerts** - Server events, warnings
- **Achievement Celebrations** - Special accomplishments
- **System Messages** - Maintenance, restarts

## 🎨 Message Templates

### Pre-configured Templates
NeoEssentials includes ready-to-use notification templates:

#### Welcome Templates
```json
{
  "welcome_new": {
    "type": "title",
    "title": "§6Welcome to {server_name}!",
    "subtitle": "§eEnjoy your stay, {player_name}!",
    "sound": "entity.player.levelup",
    "duration": 60
  },
  "welcome_returning": {
    "type": "actionbar",
    "message": "§aWelcome back, {player_name}! Last seen: {last_seen}",
    "sound": "block.note_block.chime"
  }
}
```

#### System Templates
```json
{
  "server_restart": {
    "type": "bossbar",
    "message": "§cServer restart in {time}! Please find a safe location.",
    "color": "RED",
    "style": "PROGRESS",
    "sound": "block.anvil.land"
  },
  "maintenance_mode": {
    "type": "title",
    "title": "§6Maintenance Mode",
    "subtitle": "§eServer undergoing maintenance",
    "fadeIn": 20,
    "stay": 100,
    "fadeOut": 20
  }
}
```

### Custom Template Creation
Create custom templates in `config/neoessentials/templates/notifications/`:

```json
{
  "achievement_unlock": {
    "type": "combo",
    "notifications": [
      {
        "type": "title",
        "title": "§6§lAchievement Unlocked!",
        "subtitle": "§e{achievement_name}"
      },
      {
        "type": "chat",
        "message": "§7{player_name} §eunlocked §6{achievement_name}§e!"
      },
      {
        "type": "sound",
        "sound": "ui.toast.challenge_complete"
      }
    ]
  }
}
```

## 🔧 Advanced Notification Features

### Smart Routing
The notification system intelligently routes messages based on player preferences:

```bash
/notify preferences             # Open notification preferences GUI
/notify toggle <type>           # Toggle notification type on/off
/notify priority <level>        # Set notification priority threshold
```

**Routing Options:**
- **Player Status** - Different notifications for AFK, busy, etc.
- **Permission Level** - Role-appropriate notifications
- **Channel Preferences** - Player's preferred notification method
- **Time-based** - Different notifications based on time of day

### Notification Queue
Advanced queuing system prevents notification spam:

```toml
[notifications.queue]
# Maximum notifications per player
maxPerPlayer = 5

# Time between notifications (seconds)
timeBetween = 2

# Priority levels (higher = more important)
priorities = ["low", "normal", "high", "urgent"]

# Auto-consolidate similar notifications
consolidate = true
```

### Conditional Notifications
Send notifications based on specific conditions:

```bash
/notify condition <condition> <message>  # Conditional notification
/notify trigger <event> <template>       # Event-triggered notification
/notify schedule <time> <message>        # Scheduled notification
```

**Condition Examples:**
- `health < 50%` - Low health warning
- `balance > 10000` - Wealth milestone
- `playtime = 1h` - Hourly reminders
- `death = true` - Death notifications

## 📱 External Integrations

### Discord Integration
Send notifications to Discord channels:

```bash
/discord notify <channel> <message>     # Send to Discord channel
/discord alert <level> <message>        # Send alert to Discord
/discord webhook <url> <message>        # Send to webhook URL
```

**Configuration:**
```toml
[integrations.discord]
enabled = true
webhook_url = "your_discord_webhook_url"
bot_token = "your_bot_token"

[integrations.discord.channels]
general = "general_channel_id"
admin = "admin_channel_id"
alerts = "alerts_channel_id"
```

### Email Notifications
Send important alerts via email:

```bash
/email notify <address> <subject> <message>  # Send email
/email alert <level> <message>               # Send alert email
/email template <name> <recipient>           # Use email template
```

**Email Configuration:**
```toml
[integrations.email]
enabled = false
smtp_server = "smtp.gmail.com"
smtp_port = 587
username = "your_email@gmail.com"
password = "your_app_password"
from_address = "noreply@yourserver.com"
```

### Webhook Integration
Send notifications to external services:

```bash
/webhook send <url> <payload>           # Send webhook
/webhook template <name> <data>         # Use webhook template
```

## 🎯 Event-Based Notifications

### Player Events
Automatically notify on player events:

```toml
[notifications.events.player]
# Player join notifications
join.enabled = true
join.template = "welcome_new"
join.broadcast = false

# Player leave notifications  
leave.enabled = true
leave.template = "player_leave"
leave.broadcast = true

# Death notifications
death.enabled = true
death.template = "player_death"
death.broadcast = true

# Achievement notifications
achievement.enabled = true
achievement.template = "achievement_unlock"
achievement.broadcast = true
```

### Server Events
Notify on server-related events:

```toml
[notifications.events.server]
# Server start/stop
startup.enabled = true
startup.template = "server_online"

shutdown.enabled = true  
shutdown.template = "server_offline"

# Performance alerts
lag.enabled = true
lag.threshold = 15.0
lag.template = "lag_warning"

# Resource alerts
memory.enabled = true
memory.threshold = 80
memory.template = "memory_warning"
```

### Security Events
Security-related notifications:

```toml
[notifications.events.security]
# Threat detection
threat.enabled = true
threat.template = "security_threat"
threat.level = "admin"

# IP bans
ban.enabled = true
ban.template = "player_banned"
ban.broadcast = false

# Login attempts
login.enabled = true
login.template = "failed_login"
login.threshold = 3
```

## 📊 Notification Analytics

### Delivery Tracking
Track notification delivery and engagement:

```bash
/notify stats                   # View notification statistics
/notify analytics <timeframe>   # Detailed analytics
/notify delivery <id>           # Check delivery status
```

**Metrics Tracked:**
- **Delivery Rate** - Successfully delivered notifications
- **Read Rate** - Notifications actually seen by players
- **Response Rate** - Player actions following notifications
- **Channel Effectiveness** - Most effective notification channels

### Performance Monitoring
Monitor notification system performance:

```bash
/notify performance             # Performance metrics
/notify queue status            # Queue status and backlog
/notify errors                  # Recent notification errors
```

## 🎨 Customization Options

### Visual Customization
Customize notification appearance:

```json
{
  "visual": {
    "colors": {
      "info": "§b",
      "warning": "§e", 
      "error": "§c",
      "success": "§a"
    },
    "prefixes": {
      "info": "§8[§bINFO§8]§r",
      "warning": "§8[§eWARN§8]§r",
      "error": "§8[§cERROR§8]§r"
    },
    "sounds": {
      "info": "block.note_block.chime",
      "warning": "block.note_block.bass",
      "error": "entity.villager.no"
    }
  }
}
```

### Behavioral Customization
Configure notification behavior:

```toml
[notifications.behavior]
# Default notification duration (seconds)
defaultDuration = 5

# Fade in/out times for titles
fadeIn = 10
fadeOut = 10

# Sound volume levels
soundVolume = 1.0

# Enable notification history
enableHistory = true

# Maximum history length
maxHistory = 100
```

## 📱 Player Notification Management

### Player Preferences
Allow players to manage their notification preferences:

```bash
/notify settings                # Open settings GUI
/notify mute <type>             # Mute specific type
/notify unmute <type>           # Unmute specific type
/notify volume <level>          # Set sound volume
/notify dnd                     # Do not disturb mode
```

**Preference Options:**
- **Notification Types** - Enable/disable specific types
- **Delivery Channels** - Preferred notification methods
- **Sound Settings** - Volume levels and sound choices
- **Timing** - When to receive notifications
- **Priority Filtering** - Minimum priority level

### Notification History
Track and review past notifications:

```bash
/notify history                 # View notification history
/notify history clear           # Clear history
/notify search <keyword>        # Search notification history
```

## 🛠️ Administration Tools

### Notification Management
Administrative tools for managing notifications:

```bash
/notify admin                   # Open admin panel
/notify broadcast <message>     # Admin broadcast
/notify emergency <message>     # Emergency notification
/notify test <player> <type>    # Test notifications
```

### Bulk Operations
Send notifications to multiple players:

```bash
/notify group <group> <message>     # Notify permission group
/notify world <world> <message>     # Notify players in world
/notify radius <radius> <message>   # Notify players in radius
/notify online <message>            # Notify all online players
```

### System Notifications
Built-in system notifications:

```bash
/notify system restart <time>       # Server restart warning
/notify system maintenance         # Maintenance notification
/notify system update             # Update notification
/notify system welcome <player>    # Welcome notification
```

## 🔧 Troubleshooting

### Common Issues

#### Notifications Not Appearing
- Check player notification preferences
- Verify notification system is enabled
- Test with different notification types
- Check for client-side issues

#### Sound Not Playing
- Verify sound is enabled in settings
- Check player's sound volume settings
- Test with different sounds
- Verify resource pack compatibility

#### External Integrations Failing
- Check network connectivity
- Verify webhook URLs and tokens
- Test authentication credentials
- Review integration logs

### Debug Commands
```bash
/notify debug <player>          # Debug player notifications
/notify test delivery           # Test notification delivery
/notify validate templates      # Validate notification templates
/notify reload                  # Reload notification system
```

---

## 📚 Related Documentation

- **[Configuration](Configuration.md)** - Notification system configuration
- **[Placeholders](Placeholders.md)** - Available placeholders for notifications
- **[Events System](Events.md)** - Event-triggered notifications
- **[Security Features](Security.md)** - Security notification integration

*Last Updated: August 6, 2025*
