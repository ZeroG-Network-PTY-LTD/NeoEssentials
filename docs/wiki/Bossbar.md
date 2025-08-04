# Bossbar System

The NeoEssentials Bossbar System provides dynamic, template-based bossbar management with real-time updates and comprehensive customization options.

## 🎯 Overview

The bossbar system allows administrators to:
- Display informational messages to players
- Show server statistics and real-time data
- Create custom progress indicators
- Send server-wide announcements
- Use pre-defined templates or create custom ones

## 🎨 Bossbar Templates

### Pre-defined Templates

#### Welcome Template
**Purpose**: Welcome new players to the server
```yaml
welcome:
  text: "Welcome to {server_name}, {player_name}!"
  color: "GREEN"
  style: "SOLID"
  duration: 5
  progress: 100
```

#### Server Info Template
**Purpose**: Display real-time server information
```yaml
serverinfo:
  text: "Players: {server_players}/{server_max_players} | TPS: {server_tps} | Memory: {server_memory_used}%"
  color: "BLUE"
  style: "SEGMENTED_10"
  duration: 15
  progress: "{server_memory_used}"
  updateInterval: 2
```

#### Event Template
**Purpose**: Announce server events
```yaml
event:
  text: "🎉 Server Event: {event_name} - {event_description}"
  color: "YELLOW"
  style: "SOLID"
  duration: 30
  progress: 100
```

#### Warning Template
**Purpose**: Display warning messages
```yaml
warning:
  text: "⚠️ Warning: {warning_message}"
  color: "RED"
  style: "SOLID"
  duration: 8
  progress: 100
```

#### Progress Template
**Purpose**: Show progress indicators
```yaml
progress:
  text: "Progress: {progress_text} - {progress_percent}%"
  color: "PURPLE"
  style: "SEGMENTED_20"
  duration: 60
  progress: "{progress_percent}"
```

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

Create custom templates in `config/neoessentials/templates/bossbar.yml`:

```yaml
custom_template:
  text: "Your custom message here with {placeholders}"
  color: "BLUE"
  style: "SEGMENTED_10"
  duration: 20
  progress: 100
  updateInterval: 5
  autoRemove: true
  playSound: true
  soundType: "BLOCK_NOTE_BLOCK_PLING"
```

### Template Properties

| Property | Type | Description | Default |
|----------|------|-------------|---------|
| `text` | String | Bossbar text (supports placeholders) | Required |
| `color` | String | Bossbar color | "BLUE" |
| `style` | String | Bossbar style | "SOLID" |
| `duration` | Integer | Display duration in seconds | 10 |
| `progress` | Integer/String | Progress percentage (0-100) or placeholder | 100 |
| `updateInterval` | Integer | Update interval in seconds | 0 (no updates) |
| `autoRemove` | Boolean | Auto-remove when duration expires | true |
| `playSound` | Boolean | Play sound when displayed | false |
| `soundType` | String | Sound to play | "BLOCK_NOTE_BLOCK_PLING" |

## 🔧 Placeholder Support

### Built-in Placeholders

#### Player Placeholders
- `{player_name}` - Player's name
- `{player_health}` - Player's current health
- `{player_max_health}` - Player's maximum health
- `{player_food}` - Player's hunger level
- `{player_level}` - Player's experience level
- `{player_world}` - Player's current world

#### Server Placeholders
- `{server_name}` - Server name
- `{server_players}` - Current player count
- `{server_max_players}` - Maximum player count
- `{server_tps}` - Server TPS (ticks per second)
- `{server_memory_used}` - Memory usage percentage
- `{server_memory_max}` - Maximum memory (MB)

#### Time Placeholders
- `{time}` - Current time (HH:mm)
- `{date}` - Current date (MM/dd/yyyy)
- `{datetime}` - Current date and time
- `{world_time}` - Minecraft world time

#### Utility Placeholders
- `{random_1_10}` - Random number 1-10
- `{random_1_100}` - Random number 1-100
- `{neoessentials_version}` - Mod version

### Custom Placeholders

Register custom placeholders for use in bossbars:

```java
// Example custom placeholder registration
PlaceholderManager.registerPlaceholder("custom_data", (player, context) -> {
    return "Your custom value";
});
```

## ⚙️ Configuration

### Global Bossbar Settings

```toml
[bossbar]
# Enable bossbar system
enabled = true

# Default duration for bossbars (seconds)
defaultDuration = 10

# Maximum bossbars per player
maxPerPlayer = 3

# Enable template system
enableTemplates = true

# Auto-remove expired bossbars
autoRemove = true

# Default update interval (seconds)
defaultUpdateInterval = 0
```

### Template-Specific Settings

```toml
[bossbar.templates.welcome]
enabled = true
autoShow = true  # Automatically show to new players
showDelay = 2    # Delay before showing (seconds)

[bossbar.templates.serverinfo]
enabled = true
updateInterval = 5
showToAdmins = true  # Only show to admins

[bossbar.templates.event]
enabled = true
broadcastByDefault = true  # Broadcast to all players
requirePermission = "neoessentials.events.view"
```

## 🎭 Event Integration

### Automatic Events

The bossbar system can automatically display bossbars for certain events:

#### Player Join Event
```yaml
player_join:
  template: "welcome"
  delay: 2
  enabled: true
```

#### Server Events
```yaml
server_restart:
  template: "warning"
  text: "Server restart in {restart_time} minutes!"
  duration: 30
  broadcast: true
```

### Custom Event Triggers

Create custom event triggers in your configuration:

```toml
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

### Template-Specific Permissions
- `neoessentials.bossbar.template.welcome` - Use welcome template
- `neoessentials.bossbar.template.serverinfo` - Use server info template
- `neoessentials.bossbar.template.event` - Use event template
- `neoessentials.bossbar.template.warning` - Use warning template
- `neoessentials.bossbar.template.progress` - Use progress template

### Administrative Permissions
- `neoessentials.bossbar.admin` - All administrative functions
- `neoessentials.bossbar.reload` - Reload bossbar configuration
- `neoessentials.bossbar.debug` - Debug bossbar system

## 🔍 Troubleshooting

### Common Issues

#### Bossbar Not Displaying
1. Check if bossbar system is enabled in configuration
2. Verify player has appropriate permissions
3. Ensure template exists and is properly formatted

#### Placeholders Not Working
1. Verify placeholder syntax is correct
2. Check if placeholder is registered
3. Ensure player context is available

#### Performance Issues
1. Reduce update intervals for frequently updating bossbars
2. Limit maximum bossbars per player
3. Optimize placeholder calculations

### Debug Commands

```bash
# Check bossbar system status
/neoessentials debug bossbar

# Test placeholder resolution
/placeholder test "{server_players} players online"

# Validate bossbar templates
/bossbar validate templates
```

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

*Last Updated: August 3, 2025*
