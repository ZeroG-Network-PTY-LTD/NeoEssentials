# Placeholder System

NeoEssentials includes a basic placeholder system that provides dynamic content replacement for messages, GUIs, and commands. The system supports both `%placeholder%` and `{placeholder}` formats and includes built-in placeholders for common server and player information.

## 🎯 Overview

The placeholder system allows you to:
- Display dynamic content in messages and text
- Access real-time player and server information
- Use formatted text with color codes
- Register custom placeholders programmatically

## 📝 Placeholder Formats

### Standard Formats
The placeholder system supports two formats:

```
%placeholder_name%     # Percentage format
{placeholder_name}     # Curly brace format
```

### With Parameters
Some placeholders support parameters for formatting:
```
%player_health:1%      # Shows health with 1 decimal place (planned feature)
```

## 👤 Player Placeholders

### Basic Information
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%player_name%` | Player's username | `Steve` |
| `%player_displayname%` | Player's display name | `Steve` |
| `%player_level%` | Experience level | `30` |

### Health & Status
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%player_health%` | Current health | `20.0` |
| `%player_max_health%` | Maximum health | `20.0` |
| `%player_food%` | Hunger level | `20` |

### Location
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%player_x%` | X coordinate (integer) | `123` |
| `%player_y%` | Y coordinate (integer) | `64` |
| `%player_z%` | Z coordinate (integer) | `-456` |
| `%player_world%` | Current world name | `overworld` |

## 🖥️ Server Placeholders

### Basic Information
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%server_name%` | Server name | `NeoEssentials Server` |
| `%server_version%` | Server version | `1.21.1` |
| `%server_players%` | Current player count | `15` |
| `%server_max_players%` | Maximum player slots | `20` |

### Performance
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%server_tps%` | Ticks per second | `20.0` |
| `%server_memory_used%` | Used memory (MB) | `1024` |
| `%server_memory_total%` | Total memory (MB) | `4096` |
| `%server_memory_percent%` | Memory usage percentage | `25.0` |

## 🌍 World Placeholders

### Time & Weather
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%world_time%` | World time (ticks) | `6000` |
| `%world_day%` | Current day number | `1234` |
| `%world_weather%` | Current weather | `clear`, `rain`, `thunder` |

## ⏰ Time & Date Placeholders

### Current Time
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%time%` | Current time (HH:mm:ss) | `14:30:45` |
| `%date%` | Current date | `2025-08-09` |
| `%datetime%` | Date and time | `2025-08-09 14:30:45` |

## 🎨 Color & Formatting Placeholders

### Colors
| Placeholder | Description | Color |
|-------------|-------------|-------|
| `%color_black%` | Black color code | `§0` |
| `%color_blue%` | Blue | `§9` |
| `%color_green%` | Green | `§a` |
| `%color_red%` | Red | `§c` |
| `%color_yellow%` | Yellow | `§e` |
| `%color_gold%` | Gold | `§6` |
| `%color_purple%` | Purple | `§5` |
| `%color_gray%` | Gray | `§7` |
| `%color_white%` | White | `§f` |

### Formatting
| Placeholder | Description | Code |
|-------------|-------------|------|
| `%bold%` | Bold formatting | `§l` |
| `%italic%` | Italic formatting | `§o` |
| `%underline%` | Underline formatting | `§n` |
| `%reset%` | Reset formatting | `§r` |

## 🎲 Utility Placeholders

### Random Numbers
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%random_1_10%` | Random 1-10 | `7` |
| `%random_1_100%` | Random 1-100 | `42` |

### System Information
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%neoessentials_version%` | Mod version | `2.1.0` |
| `%neoessentials_features%` | Feature count | `12` |
| `%neoessentials_commands%` | Command count | `50+` |

## 🔧 Commands

The placeholder system provides several commands for testing and managing placeholders:

### Test Placeholders
Test specific placeholders to see their output:
```
/placeholder test <placeholder>
```

**Examples:**
```
/placeholder test %player_name%
/placeholder test {server_players}
/placeholder test %time%
```

### List Placeholders
View all available placeholders:
```
/placeholder list
```

### Placeholder Information
Get information about the placeholder system:
```
/placeholder info
```

### Reload System
Reload the placeholder system (for custom placeholders):
```
/placeholder reload
```

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
PlaceholderManager.registerPlaceholder("my_placeholder", player -> "Custom Value");

// Register with parameters support
PlaceholderManager.registerPlaceholder("my_param_placeholder", (player, params) -> {
    if (params != null && params.length > 0) {
        return "Value: " + params[0];
    }
    return "Default Value";
});
```

### Placeholder Interface

Custom placeholders implement the placeholder interface for consistent behavior and parameter support.

## 🛡️ Permissions

- `neoessentials.placeholder.*` - All placeholder permissions
- `neoessentials.placeholder.test` - Test placeholder replacement
- `neoessentials.placeholder.list` - List available placeholders
- `neoessentials.placeholder.info` - View placeholder information
- `neoessentials.placeholder.reload` - Reload placeholder system

---

*NeoEssentials Placeholder System - Simple, efficient, and reliable dynamic content replacement.*
