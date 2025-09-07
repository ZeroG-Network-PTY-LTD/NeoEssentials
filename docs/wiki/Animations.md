
# NeoEssentials Animation System

## Overview
Create dynamic, animated placeholders for tablist displays using a streamlined custom placeholder system. The animation system has been simplified to focus on tablist functionality with better performance and stability.

## Features
- Animated text cycling with customizable intervals
- Conditional placeholders based on player/server state
- Works in tablist headers, footers, and player formats
- Fully configurable via `config/neoessentials/customPlaceholders.json`
- Integrated with the main placeholder system for consistency

## Configuration
**File:** `config/neoessentials/customPlaceholders.json` (auto-generated on first run)

Edit this file to define animated placeholders, then use `/neoanimations reload` to apply changes.

### Example Animation Configuration
```json
{
  "customPlaceholders": {
    "server_status_animation": {
      "type": "animated",
      "frames": [
        "&a● &fOnline",
        "&e● &fOnline", 
        "&6● &fOnline",
        "&c● &fOnline"
      ],
      "interval": 1.0
    },
    "welcome_animation": {
      "type": "animated",
      "frames": [
        "&cWelcome &f${player_name}",
        "&eWelcome &f${player_name}",
        "&aWelcome &f${player_name}",
        "&bWelcome &f${player_name}",
        "&dWelcome &f${player_name}"
      ],
      "interval": 0.3
    },
    "tps_indicator": {
      "type": "conditional",
      "condition": "${server_tps} >= 18.0",
      "trueValue": "&a⚡ &f${server_tps} TPS",
      "falseValue": "&c⚡ &f${server_tps} TPS",
      "interval": 0.0
    }
  }
}
```

### Animation Types
- **animated**: Cycles through multiple frames with configurable intervals
- **conditional**: Shows different content based on placeholder conditions  
- **static**: Simple text replacement (interval 0.0)

## Usage in Tablist
Reference animated placeholders in your tablist configuration using the `${placeholder_name}` format:

```json
{
  "tablist": {
    "layouts": [
      {
        "priority": 1,
        "conditionType": "default",
        "header": [
          "&6=== &e${welcome_animation} &6===",
          "&7Status: ${server_status_animation}",
          "&7TPS: ${tps_indicator}"
        ],
        "footer": [
          "&7Performance: ${performance_bar}",
          "&7Players: &a{server_players}&7/&a{server_max_players}",
          "&7Time: &f{time}"
        ]
      }
    ]
  }
}
```

## Built-in Placeholders
Standard NeoEssentials placeholders can be used within animation frames:
- `{player_name}` `{player_health}` `{player_max_health}` `{player_food}` `{player_level}` `{player_ping}`
- `{server_players}` `{server_max_players}` `{server_tps}` `{time}` `{date}` `{server_name}`
- `{player_world}` `{player_x}` `{player_y}` `{player_z}`

## System Status
The animation system has been streamlined for better performance:
- ✅ **Tablist Animations**: Fully supported and active
- ❌ **Scoreboard Animations**: Removed for performance optimization  
- ❌ **Bossbar Animations**: Removed for performance optimization

## Commands
Animation management is simplified with basic control commands:

| Command | Description | Permission |
|---------|-------------|------------|
| `/neoanimations reload` | Reload animation system (tablist only) | `neoessentials.admin` |
| `/neoanimations stats` | Show animation system status | `neoessentials.admin` |
| `/neoanimations help` | Show available commands | `neoessentials.admin` |

Server operators (level 3+) have access by default.

**Note**: Scoreboard and bossbar animation commands have been removed.

## Troubleshooting
**Animations not showing:**
- Check that placeholders are defined in `customPlaceholders.json`
- Verify JSON syntax is valid
- Use `/neoanimations reload` after making changes
- Ensure placeholders use the `${placeholder_name}` format in tablist configs

**Performance issues:**
- Reduce animation intervals (increase interval values)
- Limit the number of frames in animated placeholders
- Consider using conditional placeholders instead of animations

**Config errors:**
- Validate JSON syntax in `customPlaceholders.json`
- Check console logs for specific error messages
- Ensure interval values are positive numbers (0.0 for static content)

## Current Limitations
- Animations only work in tablist headers and footers
- Scoreboard animations are no longer supported
- Bossbar animations are no longer supported
- Animation commands are limited to reload and status checking

## Advanced Usage & Integration
The animation system integrates directly with the placeholder system for seamless functionality.

**Java Example (Plugin Integration):**
```java
// Access the placeholder manager
PlaceholderManager placeholderManager = PlaceholderManager.getInstance();

// Process text containing animated placeholders
String processedText = placeholderManager.processPlaceholders(
    "${welcome_animation} - Server: ${server_status_animation}", 
    new PlaceholderManager.PlaceholderContext(serverPlayer)
);

// Get animation interval for timing
double interval = placeholderManager.getAnimationInterval("welcome_animation");
```

**Custom Placeholder Integration:**
- Animated placeholders can reference other placeholders in their frames
- Use `${placeholder_name}` syntax within animation frames
- Conditional placeholders can use comparison operators (`>=`, `==`, etc.)

## Configuration Examples
See the auto-generated `config/neoessentials/customPlaceholders.json` for comprehensive examples of:
- Animated text cycling
- Conditional status indicators  
- Server performance displays
- Player-specific animations

---
The NeoEssentials animation system provides focused tablist animation support with streamlined configuration and optimal performance.
