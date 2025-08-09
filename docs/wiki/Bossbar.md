# Enhanced Bossbar System

The NeoEssentials Enhanced Bossbar System provides dynamic, multi-bossbar management with theme support, animations, and comprehensive customization options.

## 🎯 Overview

The enhanced bossbar system allows administrators to:
- Display multiple bossbars simultaneously per player
- Use theme-based styling for consistent visual design
- Show animated content with 20-frame animation cycles
- Display real-time server statistics and player data
- Create custom progress indicators with advanced placeholders
- Send server-wide announcements with visual effects
- Use pre-defined templates or create custom themed bossbars

## 🎨 Enhanced Bossbar Features

### Multi-Bossbar Support
- **Multiple Simultaneous Bossbars**: Players can have multiple bossbars displayed at once
- **Layered Display**: Bossbars stack visually without interfering with each other
- **Individual Management**: Each bossbar can be updated, styled, and removed independently

### Theme System
The bossbar system includes 4 built-in themes for consistent styling:

#### Default Theme
- **Title Format**: "§f§l{title}"
- **Subtitle Format**: "§7{subtitle}"
- **Color**: WHITE
- **Style**: PROGRESS

#### Modern Theme
- **Title Format**: "§b§l► {title} §b§l◄"
- **Subtitle Format**: "§f{subtitle}"
- **Color**: BLUE
- **Style**: NOTCHED_10

#### Classic Theme
- **Title Format**: "§6§l═══ {title} ═══"
- **Subtitle Format**: "§e{subtitle}"
- **Color**: YELLOW
- **Style**: NOTCHED_6

#### Minimalist Theme
- **Title Format**: "§f{title}"
- **Subtitle Format**: "§8{subtitle}"
- **Color**: WHITE
- **Style**: PROGRESS

**Note**: Theme support is implemented in the manager but not exposed through commands in the current version.

### Pre-defined Templates

#### Welcome Template
**Purpose**: Welcome new players to the server
- **Text**: "§6§lWelcome to the Server! - §7Enjoy your stay and have fun!"
- **Color**: YELLOW
- **Style**: PROGRESS
- **Progress**: 100%

#### Server Info Template
**Purpose**: Display real-time server information
- **Text**: "§b§lServer Information - §fOnline: §a{online}§f/§a{max} §7| §fTPS: §a{tps}"
- **Color**: BLUE
- **Style**: NOTCHED_10
- **Progress**: 80%

#### Event Template
**Purpose**: Announce server events
- **Text**: "§d§lEvent Announcement - §fCheck out the latest server events!"
- **Color**: PURPLE
- **Style**: NOTCHED_6
- **Progress**: 100%

#### Warning Template
**Purpose**: Display warning messages
- **Text**: "§c§lWarning - §fPlease read the server rules!"
- **Color**: RED
- **Style**: PROGRESS
- **Progress**: 100%
- **Effects**: Darkens screen

#### Progress Template
**Purpose**: Show progress indicators
- **Text**: "§e§lProgress - §fTask in progress..."
- **Color**: YELLOW
- **Style**: NOTCHED_20
- **Progress**: 50%
- **Effects**: Plays boss music

#### Health Template
**Purpose**: Real-time health monitoring
- **Text**: "§c§lHealth Status - §fHealth: §a{health}§f/§a{maxhealth}"
- **Color**: RED
- **Style**: PROGRESS
- **Progress**: 100%

#### Animated Welcome Template
**Purpose**: Animated welcome experience for new players
- **Text**: "§6§l{animated_title} - §7{animated_subtitle}"
- **Color**: YELLOW
- **Style**: PROGRESS
- **Progress**: 100%

## 🎮 Commands

### Display Commands

#### `/bossbar show <template> [player] [duration]`
Display a bossbar using a predefined template.

**Examples**:
```bash
# Show welcome bossbar to yourself
/bossbar show welcome

# Show server info to specific player for 30 seconds
/bossbar show serverinfo Steve 30

# Show event bossbar with custom duration
/bossbar show event 45
```

**Permissions**: `neoessentials.bossbar.show`, `neoessentials.bossbar.show.others`

---

#### `/bossbar broadcast <template> <duration>`
Broadcast a bossbar to all online players.

**Examples**:
```bash
# Broadcast event announcement for 60 seconds
/bossbar broadcast event 60

# Broadcast warning message for 10 seconds
/bossbar broadcast warning 10
```

**Permission**: `neoessentials.bossbar.broadcast`

---

#### `/bossbar announce <template> <duration>`
Alias for broadcast - announce a bossbar to all online players.

**Examples**:
```bash
# Announce event for 60 seconds
/bossbar announce event 60

# Announce warning for 10 seconds
/bossbar announce warning 10
```

**Permission**: `neoessentials.bossbar.broadcast`

---

#### `/bossbar create <text> [player] [duration]`
Create a custom bossbar with specified text.

**Examples**:
```bash
# Create custom bossbar for yourself
/bossbar create "Server restart in 5 minutes!"

# Create custom bossbar for specific player
/bossbar create "Welcome back!" Steve 15
```

**Permission**: `neoessentials.bossbar.create`

---

### Management Commands

#### `/bossbar update <text> <progress> [player]`
Update an existing bossbar's text and progress.

**Examples**:
```bash
# Update your bossbar
/bossbar update "New message" 75

# Update specific player's bossbar
/bossbar update "Updated text" 50 Steve
```

**Permission**: `neoessentials.bossbar.update`

---

#### `/bossbar hide [player]`
Hide a player's active bossbar.

**Examples**:
```bash
# Hide your own bossbar
/bossbar hide

# Hide specific player's bossbar
/bossbar hide Steve
```

**Permission**: `neoessentials.bossbar.hide`

---

#### `/bossbar templates`
List all available bossbar templates.

**Example**:
```bash
/bossbar templates
```

**Permission**: `neoessentials.bossbar.templates`

## 🎨 Customization

### Colors
Available bossbar colors:
- `PINK` - Pink bossbar
- `BLUE` - Blue bossbar
- `RED` - Red bossbar
- `GREEN` - Green bossbar
- `YELLOW` - Yellow bossbar
- `PURPLE` - Purple bossbar
- `WHITE` - White bossbar

### Styles
Available bossbar styles:
- `SOLID` - Solid progress bar
- `SEGMENTED_6` - 6 segments
- `SEGMENTED_10` - 10 segments
- `SEGMENTED_12` - 12 segments
- `SEGMENTED_20` - 20 segments

### Custom Template Configuration

**Note**: Template customization is currently managed programmatically. JSON-based template configuration is planned for future versions.

Current templates can be modified by extending the CustomBossbarManager class or through the API.

## 🔧 Placeholder Support

### Player Placeholders
- `{player_name}` - Player display name
- `{player_uuid}` - Player UUID
- `{player_health}` - Current health points
- `{player_max_health}` - Maximum health points
- `{player_level}` - Player experience level
- `{player_world}` - Current world name

### Server Placeholders
- `{online}` - Current player count
- `{max}` - Maximum player slots
- `{tps}` - Server TPS (ticks per second)

### Animation Placeholders
- `{animated_title}` - Animated title text
- `{animated_subtitle}` - Animated subtitle text

**Animation Support**: The bossbar system integrates with the NeoEssentials animation system for cycling text content.

### Custom Placeholders

Custom placeholders can be registered through the NeoEssentials API for advanced use cases.

## ⚙️ Configuration

### Global Bossbar Settings

The bossbar system is currently configured through code and templates are managed programmatically. Configuration files are handled through the NeoEssentials JSON configuration system in `config/neoessentials/`.

### Template Management

Templates are currently defined in the CustomBossbarManager class and include:
- **welcome** - Welcome message for new players
- **serverinfo** - Server information display
- **event** - Event announcements
- **warning** - Warning messages
- **progress** - Progress indicators
- **health** - Health status display
- **animated_welcome** - Animated welcome message

### Default Settings

- **Default Duration**: 10 seconds
- **Multi-Bossbar Support**: Yes (multiple bossbars per player)
- **Animation Support**: Yes (through AnimationManager integration)
- **Placeholder Support**: Yes (player, server, and custom placeholders)
- **Theme Support**: Yes (4 built-in themes)

### Future Configuration

Template and theme customization through JSON configuration files is planned for future versions.

## 🎭 Event Integration

### Automatic Events

The bossbar system automatically integrates with player events:

#### Player Join Event
- Automatically shows the "welcome" template when players join
- Duration: 10 seconds
- Template: "§6§lWelcome to the Server! - §7Enjoy your stay and have fun!"

#### Player Leave Event
- Automatically cleans up all active bossbars for the player
- Clears animation data to prevent memory leaks

### Animation Integration

The bossbar system integrates with the NeoEssentials animation system:
- Supports animated titles and subtitles
- 20-frame animation cycles
- Automatic frame updates
- Animation cleanup on player disconnect
[bossbar.events]
# Show bossbar when player reaches certain level
[bossbar.events.level_up]
trigger = "player_level_up"
template = "progress"
condition = "level >= 10"
text = "Congratulations on reaching level {player_level}!"
```

## 🎯 Advanced Features

### Progress Tracking

Use bossbars to track various progress indicators:

#### Experience Progress
```yaml
exp_progress:
  text: "Experience: Level {player_level} ({player_exp_percent}%)"
  progress: "{player_exp_percent}"
  updateInterval: 1
```

#### Task Progress
```yaml
task_progress:
  text: "Quest: {quest_name} - {quest_progress}/{quest_total}"
  progress: "{quest_percent}"
  updateInterval: 5
```

### Animation Support

Create animated bossbars with changing text:

```yaml
animated_welcome:
  frames:
    - text: "Welcome to {server_name}!"
      duration: 2
    - text: "Enjoy your stay, {player_name}!"
      duration: 2
    - text: "Type /help for assistance"
      duration: 2
  loop: false
```

### Conditional Display

Show bossbars based on conditions:

```yaml
conditional_bossbar:
  text: "VIP Area - Welcome {player_name}!"
  condition: "player_has_permission('server.vip')"
  duration: 5
```

## 🛡️ Permissions

### Basic Permissions
- `neoessentials.bossbar.*` - All bossbar permissions
- `neoessentials.bossbar.show` - Show bossbars to self
- `neoessentials.bossbar.show.others` - Show bossbars to other players
- `neoessentials.bossbar.broadcast` - Broadcast bossbars to all players
- `neoessentials.bossbar.create` - Create custom bossbars
- `neoessentials.bossbar.update` - Update existing bossbars
- `neoessentials.bossbar.hide` - Hide bossbars
- `neoessentials.bossbar.templates` - List available templates

### Administrative Permissions
- `neoessentials.moderation.basic` - Basic moderation permissions (required for bossbar commands)

**Note**: The bossbar system currently uses the general moderation permission for access control.

## 🔍 Troubleshooting

### Common Issues

#### Bossbar Not Showing
1. Check player permissions: `neoessentials.bossbar.show`
2. Verify template exists: `/bossbar templates`
3. Check if player already has multiple bossbars (system supports multiple per player)
4. Ensure the template name is spelled correctly

#### Template Not Found
1. Use `/bossbar templates` to list available templates
2. Verify template name spelling (case-sensitive)
3. Available templates: welcome, serverinfo, event, warning, progress, health, animated_welcome

#### Placeholders Not Working
1. Ensure placeholder syntax is correct: `{placeholder_name}`
2. Check if placeholder is registered in the system
3. Some placeholders require specific server conditions

#### Animation Issues
1. Check if AnimationManager is properly initialized
2. Verify animation configuration files in `config/neoessentials/`
3. Animation frames update automatically every few seconds

#### Performance Issues
1. Multiple bossbars per player are supported but may impact performance
2. Limit frequent updates to avoid client lag
3. Use appropriate durations to prevent bossbar spam

## 📊 Usage Examples

### Server Announcements
```bash
# Announce server maintenance
/bossbar broadcast warning 300
# Then update with countdown
/bossbar update "Maintenance in 4 minutes" 80
```

### Player Onboarding
```bash
# Welcome new players with progression
/bossbar show welcome
# Follow up with server info
/bossbar show serverinfo 20
```

### Event Management
```bash
# Start event announcement
/bossbar broadcast event 60
# Update with participation count
/bossbar update "Event: PvP Tournament - {event_participants} participants" 100
```

## 🎨 Best Practices

### Design Guidelines
1. **Keep text concise** - Bossbars have limited space
2. **Use appropriate colors** - Match the message type (red for warnings, green for positive messages)
3. **Meaningful progress** - Ensure progress bars represent actual progress
4. **Reasonable duration** - Don't overwhelm players with persistent bossbars

### Performance Optimization
1. **Limit update frequency** - Use appropriate update intervals
2. **Cache placeholder values** - Avoid expensive calculations in frequently updating placeholders
3. **Use conditions wisely** - Prevent unnecessary bossbar displays

### User Experience
1. **Clear messaging** - Make sure the purpose is obvious
2. **Consistent styling** - Use similar colors/styles for related messages
3. **Avoid spam** - Don't show too many bossbars simultaneously

---

**Related Documentation**: [Placeholders](Placeholders.md) | [Configuration](Configuration.md) | [Essential Commands](Essential-Commands.md)

*Last Updated: August 6, 2025*
