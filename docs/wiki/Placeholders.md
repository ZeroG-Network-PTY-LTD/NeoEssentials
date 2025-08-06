# Placeholder System

The NeoEssentials Placeholder System provides dynamic content replacement with 50+ built-in placeholders and support for custom placeholders. This system integrates throughout the mod to provide real-time, contextual information.

## 🎯 Overview

The placeholder system allows you to:
- Display dynamic content in messages, GUIs, and bossbars
- Access real-time player and server information
- Create custom placeholders for your specific needs
- Format data with parameters and options

## 📝 Placeholder Formats

### Standard Format
The default placeholder format uses curly braces:
```
{placeholder_name}
```

### Alternative Format
For compatibility with other systems, percentage signs are also supported:
```
%placeholder_name%
```

### With Parameters
Many placeholders support parameters for formatting:
```
{player_health:1}     # Shows health with 1 decimal place
{server_tps:2}        # Shows TPS with 2 decimal places
{random_1_100}        # Random number between 1 and 100
```

## 👤 Player Placeholders

### Basic Information
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `{player_name}` | Player's username | `Steve` |
| `{player_display_name}` | Player's display name (with formatting) | `§6Steve` |
| `{player_uuid}` | Player's UUID | `550e8400-e29b-41d4-a716-446655440000` |
| `{player_ping}` | Player's ping in milliseconds | `42` |

### Health & Status
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `{player_health}` | Current health | `20.0` |
| `{player_health:0}` | Health rounded to integer | `20` |
| `{player_max_health}` | Maximum health | `20.0` |
| `{player_health_percent}` | Health as percentage | `100` |
| `{player_food}` | Hunger level | `20` |
| `{player_saturation}` | Saturation level | `20.0` |
| `{player_air}` | Air level | `300` |

### Enhanced Display Placeholders
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `{animated_title}` | Cycling animated title | `Welcome to Server!` |
| `{animated_subtitle}` | Cycling animated subtitle | `Enjoy your stay!` |
| `{tablist_theme}` | Current tablist theme | `modern` |
| `{scoreboard_theme}` | Current scoreboard theme | `stats` |
| `{player_ping_color}` | Color-coded ping | `§a42ms` |

### Experience & Levels
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `{player_level}` | Experience level | `30` |
| `{player_exp}` | Total experience points | `1395` |
| `{player_exp_to_next}` | Experience needed for next level | `112` |
| `{player_exp_percent}` | Progress to next level (%) | `73` |

### Location
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `{player_x}` | X coordinate | `123.45` |
| `{player_x:0}` | X coordinate (integer) | `123` |
| `{player_y}` | Y coordinate | `64.00` |
| `{player_z}` | Z coordinate | `-456.78` |
| `{player_world}` | Current world name | `overworld` |
| `{player_biome}` | Current biome | `plains` |

### Game State
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `{player_gamemode}` | Current game mode | `SURVIVAL` |
| `{player_flying}` | Whether player is flying | `true` |
| `{player_sneaking}` | Whether player is sneaking | `false` |
| `{player_sprinting}` | Whether player is sprinting | `true` |

## 🖥️ Server Placeholders

### Basic Information
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `{server_name}` | Server name from config | `My Awesome Server` |
| `{server_version}` | Server version | `1.21.3` |
| `{server_players}` | Current player count | `15` |
| `{server_max_players}` | Maximum player slots | `20` |
| `{server_uptime}` | Server uptime | `2h 34m` |

### Performance
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `{server_tps}` | Ticks per second | `19.8` |
| `{server_tps:1}` | TPS with 1 decimal | `19.8` |
| `{server_mspt}` | Milliseconds per tick | `12.5` |
| `{server_memory_used}` | Used memory (MB) | `1024` |
| `{server_memory_max}` | Maximum memory (MB) | `4096` |
| `{server_memory_percent}` | Memory usage percentage | `25` |
| `{server_cpu_usage}` | CPU usage percentage | `35.7` |

### Statistics
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `{server_total_entities}` | Total entities | `1543` |
| `{server_loaded_chunks}` | Loaded chunks | `2876` |
| `{server_total_worlds}` | Number of worlds | `3` |

## 🌍 World Placeholders

### Time & Weather
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `{world_time}` | World time (ticks) | `6000` |
| `{world_time_formatted}` | Formatted world time | `12:00 PM` |
| `{world_day}` | Current day number | `1234` |
| `{world_weather}` | Current weather | `CLEAR` |
| `{world_thundering}` | Whether thundering | `false` |
| `{world_raining}` | Whether raining | `true` |

### Environment
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `{world_name}` | World name | `world` |
| `{world_seed}` | World seed | `-123456789` |
| `{world_spawn_x}` | Spawn X coordinate | `0` |
| `{world_spawn_y}` | Spawn Y coordinate | `64` |
| `{world_spawn_z}` | Spawn Z coordinate | `0` |

## ⏰ Time & Date Placeholders

### Current Time
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `{time}` | Current time (HH:mm) | `14:30` |
| `{time_seconds}` | Time with seconds | `14:30:45` |
| `{time_12h}` | 12-hour format | `2:30 PM` |
| `{date}` | Current date | `08/03/2025` |
| `{datetime}` | Date and time | `08/03/2025 14:30` |

### Formatted Dates
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `{date_year}` | Current year | `2025` |
| `{date_month}` | Current month | `8` |
| `{date_day}` | Current day | `3` |
| `{date_month_name}` | Month name | `August` |
| `{date_day_name}` | Day name | `Sunday` |

## 🎨 Color & Formatting Placeholders

### Colors
| Placeholder | Description | Color |
|-------------|-------------|-------|
| `{color_black}` | Black color code | `§0` |
| `{color_dark_blue}` | Dark blue | `§1` |
| `{color_dark_green}` | Dark green | `§2` |
| `{color_dark_aqua}` | Dark aqua | `§3` |
| `{color_dark_red}` | Dark red | `§4` |
| `{color_dark_purple}` | Dark purple | `§5` |
| `{color_gold}` | Gold | `§6` |
| `{color_gray}` | Gray | `§7` |
| `{color_dark_gray}` | Dark gray | `§8` |
| `{color_blue}` | Blue | `§9` |
| `{color_green}` | Green | `§a` |
| `{color_aqua}` | Aqua | `§b` |
| `{color_red}` | Red | `§c` |
| `{color_light_purple}` | Light purple | `§d` |
| `{color_yellow}` | Yellow | `§e` |
| `{color_white}` | White | `§f` |

### Formatting
| Placeholder | Description | Code |
|-------------|-------------|------|
| `{bold}` | Bold formatting | `§l` |
| `{italic}` | Italic formatting | `§o` |
| `{underline}` | Underline formatting | `§n` |
| `{strikethrough}` | Strikethrough formatting | `§m` |
| `{obfuscated}` | Obfuscated formatting | `§k` |
| `{reset}` | Reset formatting | `§r` |

## 🎲 Utility Placeholders

### Random Numbers
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `{random_1_10}` | Random 1-10 | `7` |
| `{random_1_100}` | Random 1-100 | `42` |
| `{random_0_1}` | Random 0-1 | `0` |
| `{random_min_max}` | Custom range | `{random_5_15}` |

### System Information
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `{neoessentials_version}` | Mod version | Current version |
| `{java_version}` | Java version | `21.0.1` |

## 🎮 Commands

### Testing Placeholders

#### `/placeholder test <text>`
Test placeholder replacement with your current context.

**Examples**:
```bash
# Test basic placeholders
/placeholder test "Hello {player_name}! Health: {player_health}"

# Test server placeholders
/placeholder test "Server: {server_players}/{server_max_players} players"

# Test formatting
/placeholder test "{color_green}Welcome!{reset}"
```

#### `/placeholder test <text> <player>`
Test placeholders for a specific player context.

**Examples**:
```bash
# Test with specific player
/placeholder test "Player {player_name} is in {player_world}" Steve

# Test health display
/placeholder test "Health: {player_health}/{player_max_health}" Alex
```

### Information Commands

#### `/placeholder list`
Display all available placeholders organized by category.

**Example Output**:
```
=== Available Placeholders ===
Player: player_name, player_health, player_level...
Server: server_name, server_players, server_tps...
World: world_time, world_weather, world_name...
Time: time, date, datetime...
Colors: color_red, color_green, color_blue...
```

#### `/placeholder info <placeholder>`
Get detailed information about a specific placeholder.

**Example**:
```bash
/placeholder info player_health
# Output: player_health - Player's current health (supports decimal formatting)
```

#### `/placeholder reload`
Reload the placeholder system configuration.

```bash
/placeholder reload
```

## 🛠️ Custom Placeholders

### Creating Custom Placeholders

You can register custom placeholders programmatically:

```java
// Simple static placeholder
PlaceholderManager.registerPlaceholder("custom_message", (player, context) -> {
    return "Hello from custom placeholder!";
});

// Dynamic placeholder with player context
PlaceholderManager.registerPlaceholder("player_custom_data", (player, context) -> {
    if (player == null) return "N/A";
    // Return some custom data for the player
    return getCustomPlayerData(player);
});

// Placeholder with parameters
PlaceholderManager.registerPlaceholder("custom_number", (player, context) -> {
    String param = context.getParameter();
    if (param != null) {
        try {
            int decimals = Integer.parseInt(param);
            return String.format("%." + decimals + "f", someNumberValue);
        } catch (NumberFormatException e) {
            // Handle invalid parameter
        }
    }
    return String.valueOf(someNumberValue);
});
```

### Configuration-Based Custom Placeholders

Add custom placeholders in your configuration:

```toml
[placeholders.custom]
# Simple static placeholders
server_website = "https://myserver.com"
server_discord = "discord.gg/myserver"
server_rules = "Be respectful and have fun!"

# Dynamic placeholders (requires mod support)
[placeholders.custom.dynamic]
online_staff = "staff_count"
server_economy_total = "economy_total_balance"
```

### Using External Data Sources

```java
// Example: Database-backed placeholder
PlaceholderManager.registerPlaceholder("player_balance", (player, context) -> {
    if (player == null) return "0";
    
    // Get balance from economy system
    EconomyManager economy = EconomyManager.getInstance();
    double balance = economy.getBalance(player.getUUID());
    
    // Format with parameter support
    String param = context.getParameter();
    if (param != null) {
        try {
            int decimals = Integer.parseInt(param);
            return String.format("%." + decimals + "f", balance);
        } catch (NumberFormatException e) {
            return String.format("%.2f", balance);
        }
    }
    
    return String.valueOf(balance);
});
```

## ⚙️ Configuration

### Placeholder System Settings

```toml
[placeholders]
# Enable placeholder system
enabled = true

# Default placeholder format ({} or %%)
format = "{}"

# Enable alternative format support
alternativeFormat = "%%"

# Enable custom placeholder registration
customPlaceholders = true

# Cache placeholder results (seconds)
cacheTime = 30

# Enable parameter support
enableParameters = true
```

### Performance Settings

```toml
[placeholders.performance]
# Update interval for dynamic placeholders (seconds)
updateInterval = 5

# Enable caching for expensive operations
enableCaching = true

# Maximum cache size
maxCacheSize = 1000

# Enable async placeholder resolution
asyncResolution = true
```

### Category Settings

```toml
[placeholders.categories]
# Enable specific placeholder categories
player = true
server = true
world = true
time = true
colors = true
utility = true
performance = true

# Custom category configuration
[placeholders.categories.custom]
enabled = true
prefix = "custom_"
```

## 🔧 Integration Examples

### In Enhanced Bossbar Templates

```yaml
multi_bossbar_welcome:
  text: "{color_green}{animated_title}!{reset} Health: {player_health}/{player_max_health}"
  theme: "modern"
  duration: 10

health_monitor:
  text: "Health: {player_health_percent}% | Theme: {tablist_theme}"
  updateInterval: 1

animated_server_info:
  text: "{animated_subtitle} | TPS: {server_tps:1} | Players: {server_players}"
  updateInterval: 3
```

### In Enhanced Tablist & Scoreboard

```yaml
tablist_header: "{color_aqua}{animated_title}{reset}"
tablist_footer: "Theme: {tablist_theme} | Health: {player_health_percent}%"

scoreboard_lines:
  - "§fPlayer: §a{player_name}"
  - "§fTheme: §e{scoreboard_theme}"
  - "§fHealth: §c{player_health}§f/§c{player_max_health}"
  - "§fPing: {player_ping_color}"
  - "§f{animated_subtitle}"
```

### In Language Files

```yaml
messages:
  player_join: "{color_yellow}{player_name} joined the server! ({server_players}/{server_max_players})"
  server_status: "{color_aqua}Server Status: {server_tps:1} TPS, {server_memory_used}MB used"
  welcome_message: "{color_green}Welcome to {server_name}, {player_name}! Current time: {time}"
```

### In Commands

```java
// Using placeholders in custom commands
String message = PlaceholderManager.replacePlaceholders(
    "Hello {player_name}! You are at {player_x:0}, {player_y:0}, {player_z:0}",
    player
);
player.sendSystemMessage(Component.literal(message));
```

### In Configuration Messages

```toml
[messages]
motd = "{color_gold}Welcome to {server_name}!{reset}\n{color_aqua}Players online: {server_players}/{server_max_players}"
join_message = "{color_yellow}+ {player_name} joined"
leave_message = "{color_yellow}- {player_name} left"
```

## 🛡️ Permissions

- `neoessentials.placeholder.*` - All placeholder permissions
- `neoessentials.placeholder.test` - Test placeholder replacement
- `neoessentials.placeholder.list` - List available placeholders
- `neoessentials.placeholder.info` - View placeholder information
- `neoessentials.placeholder.reload` - Reload placeholder system
- `neoessentials.placeholder.admin` - Administrative functions

## 🔍 Troubleshooting

### Common Issues

#### Placeholder Not Replacing
1. Check placeholder name spelling
2. Verify placeholder is registered
3. Ensure proper format (`{}` or `%%`)
4. Check if placeholder requires player context

#### Performance Issues
1. Reduce cache time for frequently changing data
2. Increase cache time for static data
3. Enable async resolution for expensive operations
4. Monitor placeholder resolution times

#### Custom Placeholders Not Working
1. Verify registration code is executed
2. Check for null pointer exceptions
3. Ensure proper parameter handling
4. Test with `/placeholder test` command

### Debug Commands

```bash
# Test specific placeholder
/placeholder test "{placeholder_name}"

# Check placeholder registration
/placeholder debug list

# Monitor placeholder performance
/placeholder debug performance

# Validate placeholder syntax
/placeholder validate "{your_text_here}"
```

## 📊 Best Practices

### Performance Optimization
1. **Cache expensive operations** - Don't recalculate complex data on every replacement
2. **Use appropriate update intervals** - Balance freshness with performance
3. **Avoid heavy operations in frequently used placeholders**
4. **Use parameters for formatting** instead of multiple similar placeholders

### Code Organization
1. **Group related placeholders** - Use consistent naming schemes
2. **Document custom placeholders** - Include descriptions and examples
3. **Handle edge cases** - Null checks and error handling
4. **Use meaningful names** - Clear, descriptive placeholder names

### User Experience
1. **Provide fallback values** - Handle cases where data isn't available
2. **Use consistent formatting** - Similar data should look similar
3. **Include helpful examples** - Document usage patterns
4. **Test thoroughly** - Verify placeholders work in all contexts

---

**Related Documentation**: [Bossbar System](Bossbar.md) | [Language System](Language.md) | [Configuration](Configuration.md)

*Last Updated: August 6, 2025*
