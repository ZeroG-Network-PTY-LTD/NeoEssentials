# Enhanced Tablist, Scoreboard & Bossbar Customization System

## 🎨 Overview

The Enhanced Theme System provides **massive customization** for tablist, scoreboard, and bossbar displays with:

- **Multiple Tablist Themes** (Default, VIP, Admin, Event) with animated headers/footers
- **Dynamic Scoreboard Themes** (Server Info, Player Stats, PvP) with real-time updates  
- **Multi-Bossbar System** with simultaneous bossbars, animations, and automatic triggers
- **Permission-Based Access** with role-specific themes and content
- **Advanced Animations** (rainbow, pulse, typewriter, emergency flash)

## 🎭 Tablist Themes

### Available Themes

#### **Default Theme**
- Clean server information display
- Rotating headers with welcome message, player count, TPS
- Professional footer with website, time, ping information
- **Animation**: 5-second rotation cycle

#### **VIP Theme** 
- Exclusive purple/pink styling for VIP players
- Premium benefits display in footer
- Faster 3-second animation cycle
- **Triggers**: Requires `neoessentials.theme.vip` permission

#### **Admin Theme**
- Red administrative styling
- Server control information (TPS, RAM, system status)
- Admin tool reminders
- **Triggers**: Requires `neoessentials.theme.admin` permission

#### **Event Theme**
- Green event-focused styling
- Dynamic event information (participants, rewards, time left)
- Fast 1-second updates for live events
- **Triggers**: Requires `neoessentials.theme.event` permission

### Commands
```bash
# Change your tablist theme
/theme tablist <theme>

# Change another player's theme (admin)
/theme tablist <theme> <player>

# List available themes
/theme list tablist
```

## 📊 Scoreboard Themes

### Available Themes

#### **Server Info Theme**
- Server statistics (online players, TPS, uptime)
- Player connection info (ping, world)
- Clean separator lines

#### **Player Stats Theme**  
- Detailed player information (health, food, level, XP)
- Economy balance display
- Session statistics (time played, deaths)

#### **PvP Theme**
- Combat statistics (kills, deaths, K/D ratio)
- Ranking information
- Current match status and timer

### Commands
```bash
# Change your scoreboard theme
/theme scoreboard <theme>

# Change another player's theme (admin)
/theme scoreboard <theme> <player>

# List available themes
/theme list scoreboard
```

## 🎯 Multi-Bossbar System

### Available Templates

#### **Welcome Bossbar**
- Greeting message for new players
- Yellow styling with player name
- **Trigger**: Automatic on join

#### **Health Monitor**
- Red health bar showing current/max health
- **Trigger**: Automatic when health < 30%
- **Animation**: Emergency flash when critical

#### **XP Progress**
- Green experience bar with level and percentage
- Segmented overlay showing progress chunks
- **Trigger**: Automatic on XP gain

#### **Announcements**
- Purple server announcement display
- Supports custom messages
- **Trigger**: Manual/scheduled

#### **Event Countdown**
- Yellow countdown timer for events
- 20-segment progress bar
- **Trigger**: Scheduled events

#### **Economy Notifications**
- Green money gain/loss notifications
- Shows transaction amount and new balance
- **Trigger**: Automatic on money change

#### **PvP Combat**
- Red combat mode indicator
- Shows remaining combat time
- **Trigger**: Automatic on combat entry
- **Animation**: Emergency flash

#### **Admin Tools**
- Red admin status bar
- Server monitoring (TPS, player count)
- **Trigger**: Permission-based

### Commands
```bash
# Show bossbar (10 second default)
/theme bossbar show <template>

# Show with animation
/theme bossbar show <template> <animation>

# Show with custom duration
/theme bossbar show <template> <animation> <seconds>

# Show for another player
/theme bossbar show <template> <animation> <seconds> <player>

# Hide specific bossbar
/theme bossbar hide <template>

# Hide all bossbars
/theme bossbar hideall

# List templates and animations
/theme list bossbar
/theme list animations
```

## 🎨 Advanced Animations

### Color Animations
- **Rainbow**: Cycles through all colors (20 ticks per color)
- **Emergency**: Fast red/white flashing (5 ticks per flash)

### Progress Animations  
- **Pulse**: Smooth progress bar pulsing (10 ticks per step)

### Text Animations
- **Typewriter**: Progressive text reveal effect (15 ticks per character)

### Usage
```bash
# Apply rainbow animation to welcome bossbar
/theme bossbar show welcome rainbow 30

# Emergency flash for health warning
/theme bossbar show health emergency 10

# Pulsing progress bar
/theme bossbar show xp_progress pulse 15
```

## 🛡️ Permission System

### Tablist Theme Permissions
- `neoessentials.theme.admin` - Access admin tablist theme
- `neoessentials.theme.vip` - Access VIP tablist theme  
- `neoessentials.theme.event` - Access event tablist theme

### Bossbar Template Permissions
- `neoessentials.bossbar.welcome` - Welcome messages
- `neoessentials.bossbar.health` - Health monitoring
- `neoessentials.bossbar.xp` - XP progress bars
- `neoessentials.bossbar.announcements` - Server announcements
- `neoessentials.bossbar.events` - Event notifications
- `neoessentials.bossbar.economy` - Money notifications
- `neoessentials.bossbar.pvp` - Combat status
- `neoessentials.bossbar.admin` - Admin monitoring

### Command Permissions
- `neoessentials.theme.change` (level 2) - Change own themes
- `neoessentials.theme.others` (level 3) - Change others' themes
- `neoessentials.theme.admin` (level 3) - Admin theme commands

## 🔄 Automatic Triggers

The system automatically shows relevant bossbars based on player actions:

- **Join Server** → Welcome bossbar
- **Health < 30%** → Health monitor with emergency animation
- **Gain XP** → XP progress bar with pulse animation  
- **Money Change** → Economy notification
- **Enter Combat** → PvP status with emergency flash
- **Admin Login** → Admin tools bossbar

## 📱 Placeholders

### Universal Placeholders
- `{player}` - Player display name
- `{online}` - Online player count
- `{max}` - Maximum players
- `{tps}` - Server TPS
- `{time}` - Current time
- `{ping}` - Player ping

### Player-Specific
- `{health}` / `{max_health}` - Health values
- `{food}` - Food level
- `{level}` - Experience level
- `{exp}` - Experience percentage
- `{world}` - Current world
- `{balance}` - Economy balance

### Server-Specific  
- `{uptime}` - Server uptime
- `{ram_used}` / `{ram_max}` - Memory usage
- `{status}` - Server status

## 🎯 Usage Examples

### Basic Theme Switching
```bash
# Switch to VIP tablist theme
/theme tablist vip

# Switch to PvP scoreboard 
/theme scoreboard pvp

# Show welcome bossbar with rainbow animation
/theme bossbar show welcome rainbow 30
```

### Admin Management
```bash
# Set all VIP players to VIP theme
/theme tablist vip @a[team=vip]

# Show announcement to all players
/theme bossbar show announcement auto 60 @a

# Emergency health warning
/theme bossbar show health emergency 10 Steve
```

### Event Management
```bash
# Switch event participants to event theme
/theme tablist event @a[tag=event_participant]

# Show countdown to all players
/theme bossbar show countdown pulse 300 @a

# PvP tournament setup
/theme scoreboard pvp @a[world=pvp_arena]
```

## 🔧 Technical Features

- **Multi-Bossbar Support**: Players can have multiple bossbars simultaneously
- **Real-Time Updates**: All displays update every 2 seconds (tablist) or 50ms (bossbars)
- **Memory Efficient**: Automatic cleanup of expired bossbars
- **Permission Integration**: Automatic theme assignment based on permissions
- **Animation System**: 20 TPS smooth animations with multiple types
- **Placeholder Processing**: Real-time data injection with extensive placeholder support

## 🎨 Customization Tips

1. **Theme Layering**: Combine tablist themes with matching scoreboard themes
2. **Animation Matching**: Use emergency animations for warnings, pulse for progress
3. **Duration Tuning**: Short durations (3-5s) for notifications, longer (30s+) for persistent info
4. **Permission Tiers**: Set up automatic theme switching based on player ranks
5. **Event Coordination**: Use event themes during special server events

This enhanced system provides **massive customization** far beyond basic tablist management - it's a complete player experience enhancement platform! 🚀
