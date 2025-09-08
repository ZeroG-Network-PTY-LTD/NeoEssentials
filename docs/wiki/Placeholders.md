# Placeholder System

NeoEssentials includes a comprehensive placeholder system that provides dynamic content replacement for messages, GUIs, commands, and tablist. The system supports multiple placeholder formats, built-in placeholders, custom placeholders, animated placeholders, and conditional logic.

## 🎯 System Overview

### Key Features
- **Dynamic Content Replacement** - Real-time values for player, server, and world information
- **Multiple Formats** - Support for `%placeholder%`, `{placeholder}`, and `${placeholder}` formats
- **Custom Placeholders** - JSON-based configuration for custom static, animated, and conditional placeholders
- **FTB Integration** - Seamless integration with FTB Teams, Ranks, and Chunks
- **Conditional Logic** - Complex conditional statements for dynamic content
- **Animated Placeholders** - Cycling animations for visual effects
- **High Performance** - Optimized for real-time processing with minimal performance impact

### Architecture
The placeholder system is built around:
- **PlaceholderManager** - Central management and processing engine
- **PlaceholderContext** - Context container for player and server state
- **Custom Configuration** - JSON-based custom placeholder definitions
- **Pattern Matching** - Efficient regex-based placeholder detection and replacement

## 📝 Placeholder Formats

### Supported Formats
The placeholder system supports three different formats:

```
%placeholder_name%      # Percentage format (traditional)
{placeholder_name}      # Curly brace format (modern)
${placeholder_name}     # Dollar-brace format (custom config)
```

All formats are processed equally and can be mixed within the same text.

### Format Usage
- **%format%** - Standard format, compatible with most plugins
- **{format}** - Modern format, cleaner appearance in configurations
- **${format}** - Used in custom placeholder JSON configurations for nested processing

## 👤 Player Placeholders

### Basic Information
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%player_name%` | Player's username | `Steve` |
| `%player_displayname%` | Player's display name | `Steve` |
| `%player_level%` | Experience level | `30` |
| `%player%` | Alias for player_name | `Steve` |

### Health & Status
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%player_health%` | Current health (1 decimal) | `18.5` |
| `%player_max_health%` | Maximum health (1 decimal) | `20.0` |
| `%player_food%` | Hunger level | `18` |
| `%player_ping%` | Connection latency | `45` |

### Location & Position
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%player_x%` | X coordinate (integer) | `123` |
| `%player_y%` | Y coordinate (integer) | `64` |
| `%player_z%` | Z coordinate (integer) | `-456` |
| `%player_world%` | Current world name | `overworld` |

### Permission System
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%prefix%` | Player's permission prefix | `§6[VIP]` |
| `%suffix%` | Player's permission suffix | ` §6♦` |

## 🖥️ Server Placeholders

### Basic Server Information
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%server_name%` | Server name | `NeoEssentials Server` |
| `%server_version%` | Minecraft version | `1.21.1` |
| `%server_players%` | Current player count | `15` |
| `%server_max_players%` | Maximum player slots | `20` |

### Performance Metrics
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%server_tps%` | Ticks per second | `19.8` |
| `%server_tps_colored%` | TPS with color coding | `§a19.8` |
| `%server_performance%` | TPS performance rating | `§a§lEXCELLENT` |
| `%server_mspt%` | Milliseconds per tick | `1.0` |
| `%server_memory_used%` | Used memory (MB) | `1024` |
| `%server_memory_total%` | Total memory (MB) | `4096` |
| `%server_memory_percent%` | Memory usage percentage | `25.0` |

## 🌍 World Placeholders

### Time & Environment
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%world_time%` | World time in ticks | `6000` |
| `%world_day%` | Current day number | `1234` |
| `%world_weather%` | Current weather | `clear`, `rain`, `thunder` |

## ⏰ Time & Date Placeholders

### Current Time
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%time%` | Current time (HH:mm:ss) | `14:30:45` |
| `%date%` | Current date | `2025-09-07` |
| `%datetime%` | Date and time | `2025-09-07 14:30:45` |

## 🎨 Color & Formatting Placeholders

### Color Codes
| Placeholder | Description | Color Code |
|-------------|-------------|------------|
| `%color_black%` | Black | `§0` |
| `%color_blue%` | Blue | `§9` |
| `%color_green%` | Green | `§a` |
| `%color_red%` | Red | `§c` |
| `%color_yellow%` | Yellow | `§e` |
| `%color_gold%` | Gold | `§6` |
| `%color_purple%` | Purple | `§5` |
| `%color_gray%` | Gray | `§7` |
| `%color_white%` | White | `§f` |

### Text Formatting
| Placeholder | Description | Format Code |
|-------------|-------------|-------------|
| `%bold%` | Bold formatting | `§l` |
| `%italic%` | Italic formatting | `§o` |
| `%underline%` | Underline formatting | `§n` |
| `%reset%` | Reset formatting | `§r` |

## 🎲 Utility Placeholders

### Random Values
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%random_1_10%` | Random number 1-10 | `7` |
| `%random_1_100%` | Random number 1-100 | `42` |

### NeoEssentials Information
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%neoessentials_version%` | Mod version | `2.1.0` |
| `%neoessentials_features%` | Feature count | `12` |
| `%neoessentials_commands%` | Command count | `50+` |

### Player Features
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%essentials_afk%` | AFK status | `false` |

## 🔗 FTB Integration Placeholders

NeoEssentials includes comprehensive FTB (Feed The Beast) integration with support for Teams, Ranks, and Chunks:

### FTB Teams
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%ftb_team_name%` | Team internal name | `myteam` |
| `%ftb_team_display_name%` | Team display name | `My Awesome Team` |
| `%ftb_team_role%` | Player's team role | `Owner`, `Moderator`, `Member` |
| `%ftb_team_members%` | Team member count | `5` |
| `%ftb_team_prefix%` | Team prefix | `§b[Team]` |
| `%ftb_team_suffix%` | Team suffix | ` §b♦` |
| `%team_name%` | Legacy alias for ftb_team_name | `myteam` |
| `%team_role%` | Legacy alias for ftb_team_role | `Owner` |

### FTB Ranks
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%ftb_rank_name%` | Rank internal name | `vip` |
| `%ftb_rank_display_name%` | Rank display name | `VIP Player` |
| `%ftb_rank_prefix%` | Rank prefix | `§6[VIP]` |
| `%ftb_rank_suffix%` | Rank suffix | ` §6★` |
| `%ftb_rank_color%` | Rank color code | `§6` |
| `%ftb_rank_permissions%` | Permission count | `25` |
| `%rank_name%` | Legacy alias for ftb_rank_name | `vip` |

### FTB Chunks
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%ftb_chunks_claimed%` | Claimed chunks count | `12` |
| `%ftb_chunks_loaded%` | Loaded chunks count | `8` |

### FTB Combined & Status
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%ftb_combined_prefix%` | Effective prefix (rank + team) | `§6[VIP] §b[Team]` |
| `%ftb_combined_suffix%` | Effective suffix (rank + team) | ` §6★ §b♦` |
| `%FTB_Active%` | FTB system status | `true`/`false` |
| `%ftb_has_team%` | Player has team | `true`/`false` |
| `%ftb_has_rank%` | Player has rank | `true`/`false` |

## 🎬 Built-in Animated Placeholders

NeoEssentials includes several animated placeholders for dynamic visual effects:

### Status Animations
| Placeholder | Description | Animation |
|-------------|-------------|-----------|
| `%server_status%` | Server status indicator | `§a●` → `§e●` → `§6●` → `§c●` |
| `%loading_dots%` | Loading animation | `§7.` → `§7..` → `§7...` → `§7` |
| `%rainbow_star%` | Rainbow star effect | `§c★` → `§6★` → `§e★` → `§a★` → `§b★` → `§9★` → `§5★` |

## 🔀 Conditional Logic

NeoEssentials supports sophisticated conditional placeholders for dynamic content based on conditions:

### Basic Conditional Syntax
```json
{condition: CONDITION, value: 'TRUE_VALUE', else: 'FALSE_VALUE'}
```

### Supported Conditions

**Status Conditions:**
```json
{condition: is FTB_Active, value: 'FTB Enabled', else: 'FTB Disabled'}
{condition: ftb_has_team, value: 'Has Team', else: 'No Team'}
{condition: ftb_has_rank, value: 'Has Rank', else: 'No Rank'}
```

**Comparison Conditions:**
```json
{condition: server_tps >= 18.0, value: '§aGood TPS', else: '§cPoor TPS'}
{condition: player_health > 15, value: '§aHealthy', else: '§cInjured'}
{condition: server_players > 10, value: 'Busy Server', else: 'Quiet Server'}
```

**Equality Conditions:**
```json
{condition: player_world == 'overworld', value: '🌍 Overworld', else: '🌐 Other'}
{condition: world_weather == 'clear', value: '☀ Clear', else: '🌧 Rain'}
```

### Real-World Examples

**Dynamic Player Status:**
```json
"player_status": "{condition: player_health > 15, value: '§a❤ Healthy', else: '§c❤ Critical'}"
```

**Server Performance Indicator:**
```json
"performance": "{condition: server_tps >= 18.0, value: '§a⚡ Excellent', else: '§c⚡ Poor'}"
```

**FTB Integration Display:**
```json
"team_info": "{condition: ftb_has_team, value: 'Team: {ftb_team_display_name}', else: 'No Team'}"
```

## 🎮 Commands

The placeholder system provides comprehensive command support for testing and management:

### Core Commands

#### `/placeholder` or `/placeholders`
Main command for placeholder management.

**Subcommands:**
- `/placeholder help` - Show command help
- `/placeholder list` - List all available placeholders
- `/placeholder test <text>` - Test placeholder processing
- `/placeholder test <text> <player>` - Test placeholders for specific player
- `/placeholder info <placeholder>` - Get information about a placeholder
- `/placeholder reload` - Reload custom placeholders from config

#### Alias Commands
- `/papi` - Alias for `/placeholder` (PlaceholderAPI compatibility)

### Command Examples

**Testing Placeholders:**
```bash
/placeholder test "Hello %player_name%, you have %player_health%/20 health!"
/placeholder test "Server: %server_players%/%server_max_players% online, TPS: %server_tps%"
/placeholder test "%ftb_combined_prefix%%player_name%: Ready to play!" Steve
```

**Listing Placeholders:**
```bash
/placeholder list
# Shows categorized list of all available placeholders
```

**Getting Placeholder Information:**
```bash
/placeholder info player_health
/placeholder info server_tps
/placeholder info ftb_team_name
```

**Reloading Custom Placeholders:**
```bash
/placeholder reload
# Reloads custom placeholders from customPlaceholders.json
```

### Permissions
| Permission | Command Access | Default |
|------------|----------------|---------|
| `neoessentials.moderation.basic` | All placeholder commands | Moderator+ |
| `neoessentials.placeholder.test` | Test placeholder functionality | Moderator+ |
| `neoessentials.placeholder.list` | List available placeholders | Moderator+ |
| `neoessentials.placeholder.info` | View placeholder information | Moderator+ |
| `neoessentials.placeholder.reload` | Reload placeholder system | Moderator+ |

## ⚙️ Custom Placeholder Configuration

### Configuration File Location
Custom placeholders are configured in: `config/neoessentials/customPlaceholders.json`

### Configuration Structure
```json
{
  "customPlaceholders": {
    "placeholder_name": {
      "type": "static|animated|conditional",
      "value": "static_value",
      "frames": ["frame1", "frame2"],
      "condition": "condition_expression",
      "trueValue": "true_result",
      "falseValue": "false_result",
      "interval": 1.0
    }
  }
}
```

### Placeholder Types

#### Static Placeholders
Simple text replacement with support for nested placeholders:
```json
"welcome_message": {
  "type": "static",
  "value": "&6Welcome to &bNeoEssentials &6Server, ${player_name}!"
}
```

#### Animated Placeholders
Cycling through multiple frames with configurable timing:
```json
"server_status_animation": {
  "type": "animated", 
  "frames": [
    "&a● &fOnline",
    "&e● &fOnline",
    "&6● &fOnline", 
    "&c● &fOnline"
  ],
  "interval": 1.0
}
```

#### Conditional Placeholders
Dynamic content based on conditions:
```json
"tps_indicator": {
  "type": "conditional",
  "condition": "${server_tps} >= 18.0",
  "trueValue": "&a⚡ &f${server_tps} TPS",
  "falseValue": "&c⚡ &f${server_tps} TPS"
}
```

### Advanced Examples

**Complex Conditional with Nested Placeholders:**
```json
"player_status_complex": {
  "type": "conditional", 
  "condition": "${ftb_has_team} == true",
  "trueValue": "${ftb_rank_prefix}${player_name} &7[${ftb_team_display_name}]",
  "falseValue": "${prefix}${player_name}"
}
```

**Performance Bar Animation:**
```json
"performance_bar": {
  "type": "animated",
  "frames": [
    "&a▓▓▓▓▓▓▓▓▓▓ &f100%",
    "&a▓▓▓▓▓▓▓▓▓&7▒ &f90%", 
    "&e▓▓▓▓▓▓▓▓&7▒▒ &f80%",
    "&6▓▓▓▓▓▓▓&7▒▒▒ &f70%",
    "&c▓▓▓▓▓▓&7▒▒▒▒ &f60%"
  ],
  "interval": 2.0
}
```

**Multi-Condition Logic:**
```json
"health_indicator": {
  "type": "conditional",
  "condition": "${player_health} > 15",
  "trueValue": "&a❤ Healthy (${player_health}/20)",
  "falseValue": "${player_health} > 10 ? '&e❤ Hurt (' + ${player_health} + '/20)' : '&c❤ Critical (' + ${player_health} + '/20)'"
}
```

## 🔧 Integration & Usage

### Tablist Integration
Placeholders work seamlessly with the NeoEssentials tablist system:
```json
{
  "header": [
    "&6&lNeoEssentials Server",
    "&7TPS: %server_tps_colored% &7| Players: &e%server_players%/%server_max_players%",
    "%server_status_animation%"
  ],
  "playerFormat": "%ftb_combined_prefix%%player_name%"
}
```

### Chat Integration
Use placeholders in chat formatting and messages:
```yaml
chat_format: "%ftb_combined_prefix%%player_name%&7: &f%message%"
welcome_message: "Welcome %player_name% to %server_name%! You're player #%server_players%"
```

### Command Integration
Placeholders process automatically in command messages:
```yaml
motd_command: |
  &6=== %server_name% ===
  &7Server TPS: %server_tps_colored%
  &7Online: &e%server_players%/%server_max_players%
  &7Your status: %player_health% HP, Level %player_level%
```

### GUI Integration  
Dynamic content in GUI menus and tooltips:
```yaml
gui_title: "%server_name% - %time%"
player_info_tooltip: |
  &e%player_name%
  &7Health: %player_health%/20
  &7Level: %player_level%
  &7World: %player_world%
```

## 🚀 Performance & Optimization

### Performance Features
- **Efficient Pattern Matching** - Optimized regex processing
- **Lazy Evaluation** - Placeholders only computed when needed
- **Context Caching** - Player context reused across multiple placeholders
- **Minimal Memory Footprint** - Lightweight data structures
- **Thread Safety** - Safe for concurrent access

### Performance Metrics
- **Processing Time** - Sub-millisecond processing for typical text
- **Memory Usage** - Minimal memory allocation during processing
- **Scalability** - Handles hundreds of placeholders efficiently

### Best Practices
1. **Use Appropriate Intervals** - Set reasonable animation intervals to balance visual appeal and performance
2. **Avoid Complex Conditions** - Simple conditions process faster than complex nested logic
3. **Cache Static Content** - Use static placeholders for content that doesn't change frequently
4. **Monitor Performance** - Use `/placeholder list` to check total placeholder count

## 🔍 Troubleshooting

### Common Issues

**Placeholder Not Replacing:**
1. Check placeholder name spelling (case-sensitive)
2. Verify placeholder is registered: `/placeholder list`
3. Test with: `/placeholder test "%placeholder_name%"`
4. Check for typos in custom configuration

**Custom Placeholder Not Working:**
1. Validate JSON syntax in `customPlaceholders.json`
2. Check condition syntax for conditional placeholders
3. Reload with: `/placeholder reload`
4. Check server logs for parsing errors

**Performance Issues:**
1. Check animation intervals (too fast = performance impact)
2. Reduce complex conditional logic
3. Monitor with: `/placeholder info <placeholder>`
4. Consider static alternatives for frequently-used dynamic content

**FTB Integration Issues:**
1. Verify FTB mods are installed and loaded
2. Check `%FTB_Active%` placeholder status
3. Ensure player has team/rank assigned
4. Test with: `/placeholder test "%ftb_has_team%"`

### Debug Commands

**Test Specific Placeholder:**
```bash
/placeholder test "%server_tps%"
/placeholder test "{player_health} > 15"
/placeholder test "${ftb_team_display_name}"
```

**Check Placeholder Information:**
```bash
/placeholder info server_tps
/placeholder info ftb_team_name
/placeholder info custom_placeholder_name
```

**List All Placeholders:**
```bash
/placeholder list
# Shows organized list by category (Player, Server, World, Time, Other)
```

**Reload Configuration:**
```bash
/placeholder reload
# Reloads custom placeholders and updates tablist for all players
```

## 💻 Developer API

### Registering Custom Placeholders
Developers can register placeholders programmatically:

```java
// Simple placeholder
PlaceholderManager.getInstance().registerPlaceholder("my_placeholder", 
    ctx -> "Custom Value"
);

// Placeholder with player context
PlaceholderManager.getInstance().registerPlaceholder("my_player_placeholder",
    ctx -> ctx.getPlayer() != null ? ctx.getPlayer().getName().getString() : "Unknown"
);

// Conditional placeholder
PlaceholderManager.getInstance().registerPlaceholder("my_conditional",
    ctx -> {
        if (ctx.getPlayer() != null && ctx.getPlayer().experienceLevel > 10) {
            return "§aExperienced Player";
        }
        return "§7New Player";
    }
);
```

### PlaceholderContext API
The context object provides access to:
- `getPlayer()` - Current ServerPlayer instance
- `getServer()` - MinecraftServer instance 
- Custom data through extension

### Management Methods
```java
// Check if placeholder exists
boolean exists = PlaceholderManager.getInstance().isPlaceholderRegistered("my_placeholder");

// Get placeholder count
int count = PlaceholderManager.getInstance().getPlaceholderCount();

// Unregister placeholder
PlaceholderManager.getInstance().unregisterPlaceholder("my_placeholder");

// Process placeholders in text
String processed = PlaceholderManager.getInstance().processPlaceholders(text, player);
```

### Integration Points
- **TablistManager** - Automatic placeholder processing in tablist
- **LanguageManager** - Placeholder support in localized messages
- **Command System** - Automatic placeholder processing in command messages
- **Chat System** - Integration with chat formatting

---

**Related Documentation**: [Configuration](Configuration) | [Language System](Language) | [API Documentation](API_DOCUMENTATION)

*Last Updated: September 7, 2025 - NeoEssentials 2.1.0*

## ⚙️ Configuration

The placeholder system works out-of-the-box with no configuration required. All built-in placeholders are automatically registered when the mod loads.

### Usage Examples

**In Messages:**
```yaml
welcome_message: "Welcome %player_name% to %server_name%!"
```

**In Commands:**
```yaml
broadcast: "%color_yellow%[%time%] %color_white%Server has %server_players% players online"
```

**In Text Components:**
```yaml
status: "Health: %player_health% | Level: %player_level% | Time: %time%"
```

## 🔀 Conditional Placeholders

NeoEssentials supports conditional placeholders that allow you to display different content based on conditions. This is particularly useful for integrating with optional plugins like FTB Teams/Ranks.

### Syntax
```
{condition: CONDITION, value: 'TRUE_VALUE', else: 'FALSE_VALUE'}
```

### Basic Conditions

**is Condition:**
Check if a placeholder has a value or if a feature is active:
```json
{condition: is FTB_Active, value: 'FTB is enabled!', else: 'FTB not available'}
```

**Comparison Conditions:**
```json
{condition: {server_players} > 10, value: 'Busy server!', else: 'Quiet server'}
{condition: {player_health} == 20, value: 'Full health!', else: 'Injured'}
```

### Real-World Examples

**Tablist Integration:**
```json
"header": [
  "Welcome {player_name}!",
  "Rank: {condition: is FTB_Active, value: '{ftb_rank_display_name} | Team: {ftb_team_display_name}', else: '{neoessentials_rank}'}",
  "Health: {player_health}/20"
]
```

**Chat Formats:**
```json
"{condition: is FTB_Active, value: '{ftb_combined_prefix}', else: '[{neoessentials_rank}]'} {player_name}: {message}"
```

**Status Display:**
```json
"{condition: {player_health} > 15, value: '💚 Healthy', else: {player_health} > 10 ? '💛 Hurt' : '❤️ Critical'}"
```

### Available Status Placeholders

| Placeholder | Description | Values |
|-------------|-------------|---------|
| `FTB_Active` | FTB Teams/Ranks status | `true`/`false` |
| `ftb_has_team` | Player has FTB team | `true`/`false` |
| `ftb_has_rank` | Player has FTB rank | `true`/`false` |

### Nesting Support

Conditionals can contain regular placeholders, and the selected value will be processed for additional placeholders:

```json
{condition: is FTB_Active, value: 'Team {ftb_team_display_name} has {ftb_team_members} members', else: 'No team data available'}
```

## 🔍 Troubleshooting

### Common Issues

**Placeholder not working?**
- Verify the placeholder name is correct (case-sensitive)
- Use `/placeholder test <placeholder>` to test it directly
- Check that both `%placeholder%` and `{placeholder}` formats are supported

**Performance concerns?**
- The placeholder system is lightweight and processes placeholders efficiently
- Built-in placeholders have minimal performance impact
- Server performance placeholders update in real-time

### Debug Information

Use the info command to check system status:
```
/placeholder info
```

This displays:
- Number of registered placeholders
- System performance metrics
- Available placeholder formats

## � For Developers

### Adding Custom Placeholders

Developers can register custom placeholders programmatically:

```java
// Register a simple placeholder
PlaceholderManager.getInstance().registerPlaceholder("my_placeholder", 
    ctx -> "Custom Value"
);

// Register placeholder with player context
PlaceholderManager.getInstance().registerPlaceholder("my_player_data", ctx -> {
    if (ctx.getPlayer() != null) {
        return "Player: " + ctx.getPlayer().getName().getString();
    }
    return "No Player Context";
});

// Register animated placeholder
PlaceholderManager.getInstance().registerPlaceholder("my_animation", 
    new AnimatedPlaceholder(
        List.of("Frame 1", "Frame 2", "Frame 3"), 
        1.0 // seconds between frames
    )
);
```

### Management Methods

```java
// Check if placeholder exists
boolean exists = PlaceholderManager.getInstance().isPlaceholderRegistered("placeholder_name");

// Get total placeholder count  
int count = PlaceholderManager.getInstance().getPlaceholderCount();

// Remove custom placeholder
PlaceholderManager.getInstance().unregisterPlaceholder("my_placeholder");

// Process text with placeholders
String result = PlaceholderManager.getInstance().processPlaceholders("Hello %player_name%!", player);
```

## 🛡️ Permissions

- `neoessentials.placeholder.*` - All placeholder permissions
- `neoessentials.placeholder.test` - Test placeholder replacement
- `neoessentials.placeholder.list` - List available placeholders
- `neoessentials.placeholder.info` - View placeholder information
- `neoessentials.placeholder.reload` - Reload placeholder system

---

*NeoEssentials Placeholder System - Simple, efficient, and reliable dynamic content replacement.*
