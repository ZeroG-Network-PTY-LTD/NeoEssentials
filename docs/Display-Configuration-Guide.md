# Comprehensive Display Configuration Guide

## Overview
All display elements (Tablist, Scoreboard, Bossbar) are now configured in a unified, multiline-compatible system that supports advanced customization and FTB integration.

## Configuration Files

### 1. Main Tablist Configuration (`tablist.json`)
This is the primary configuration file containing all display elements:
- **Tablist**: Player list headers, footers, and formatting
- **Scoreboard**: Sidebar display with multiline support
- **Bossbar**: Top-screen bars with progress and styling
- **Animations**: Animated sequences for dynamic content

### 2. Dedicated Scoreboard Configuration (`scoreboard.json`)
Advanced scoreboard-specific configuration with:
- Extended multiline support (up to 15+ lines)
- Conditional logic and animations
- Role-specific layouts with detailed information
- Performance-optimized update intervals

## Configuration Structure

### Tablist Section
```json
"tablist": {
  "enabled": true,
  "updateInterval": 20,
  "format": "{ftb_combined_prefix}[{team_name}] {player_name}{ftb_combined_suffix}",
  "layouts": [
    {
      "priority": 300,
      "conditionType": "permission|placeholder|default",
      "condition": "permission.node or placeholder:value",
      "header": [
        "Line 1 with full multiline support",
        "Line 2 with {placeholders}",
        "Line 3 with &ccolor codes"
      ],
      "footer": [
        "Footer line 1",
        "Footer line 2"
      ]
    }
  ],
  "playerOrder": [
    { "placeholder": "ftb_rank_weight", "direction": "desc", "asNumber": true },
    { "placeholder": "player_name", "direction": "asc" }
  ]
}
```

### Scoreboard Section
```json
"scoreboard": {
  "enabled": true,
  "updateInterval": 20,
  "title": "&6&lNeoEssentials",
  "layouts": [
    {
      "priority": 200,
      "conditionType": "permission",
      "condition": "neoessentials.admin",
      "lines": [
        "&c&m─────────────────",
        "&c&lADMIN PANEL",
        "&c&m─────────────────",
        "&f● &7Player: &e{player_name}",
        "&f● &7Team: &b{ftb_team_display_name}",
        "... up to 15+ lines supported"
      ]
    }
  ]
}
```

### Bossbar Section
```json
"bossbar": {
  "enabled": true,
  "updateInterval": 20,
  "layouts": [
    {
      "priority": 200,
      "conditionType": "permission",
      "condition": "neoessentials.admin",
      "bars": [
        {
          "id": "admin_bar",
          "text": "&c&lADMIN &7| &f{player_name} &7| &aTPS: {server_tps}",
          "color": "RED|BLUE|GREEN|YELLOW|PURPLE|PINK|WHITE",
          "style": "PROGRESS|SEGMENTED_6|SEGMENTED_10|SEGMENTED_12|SEGMENTED_20",
          "progress": 1.0
        }
      ]
    }
  ]
}
```

## Priority System
Layouts are processed by priority (highest first):
- **400+**: Owner/Super Admin panels
- **300-399**: Admin panels
- **200-299**: Staff/Moderator panels
- **100-199**: Team-based layouts
- **50-99**: Rank-based layouts
- **1-49**: Default/fallback layouts

## Condition Types

### Permission-Based
```json
{
  "conditionType": "permission",
  "condition": "neoessentials.admin"
}
```

### Placeholder-Based
```json
{
  "conditionType": "placeholder",
  "condition": "ftb_team_role:Owner"
}
```

### Default (Fallback)
```json
{
  "conditionType": "default"
}
```

## Multiline Support Features

### Advanced Text Formatting
- **Color Codes**: `&c`, `&a`, `&6`, etc.
- **Formatting**: `&l` (bold), `&o` (italic), `&n` (underline)
- **Decorative Elements**: `&m` (strikethrough for lines)
- **Reset**: `&r` (reset formatting)

### Special Characters
- **Bullets**: `●`, `◆`, `★`, `♔`, `♦`, `◈`, `◇`, `○`
- **Lines**: `─`, `═`, `╔`, `╗`, `╚`, `╝`, `╠`, `╣`
- **Arrows**: `▶`, `◀`, `▲`, `▼`

### Layout Design Patterns
```json
[
  "&c&l╔═══════════════════════════════════╗",
  "&c&l║           &f&lTITLE SECTION       &c&l║",
  "&c&l╠═══════════════════════════════════╣",
  "&c&l║ &f● &7Info Line 1                 &c&l║",
  "&c&l║ &f● &7Info Line 2                 &c&l║",
  "&c&l╚═══════════════════════════════════╝"
]
```

## FTB Integration Placeholders

### Team Placeholders
- `{ftb_team_name}` - Team internal name
- `{ftb_team_display_name}` - Team display name
- `{ftb_team_role}` - Player's role (Owner/Moderator/Member)
- `{ftb_team_members}` - Number of team members
- `{ftb_team_prefix}` - Team prefix
- `{ftb_team_suffix}` - Team suffix
- `{ftb_team_color}` - Team color code

### Rank Placeholders
- `{ftb_rank_name}` - Rank internal name
- `{ftb_rank_display_name}` - Rank display name
- `{ftb_rank_prefix}` - Rank prefix
- `{ftb_rank_suffix}` - Rank suffix
- `{ftb_rank_weight}` - Rank weight/priority
- `{ftb_rank_permissions}` - Number of permissions

### Combined Placeholders
- `{ftb_combined_prefix}` - Best prefix (rank + team)
- `{ftb_combined_suffix}` - Best suffix (rank + team)

### Status Placeholders
- `{ftb_has_team}` - "true"/"false"
- `{ftb_has_rank}` - "true"/"false"

### Legacy Aliases
- `{team_name}` - Same as `{ftb_team_name}`
- `{rank_name}` - Same as `{ftb_rank_name}`
- `{team_role}` - Same as `{ftb_team_role}`

## Standard Placeholders

### Player Information
- `{player_name}` - Player's name
- `{player_displayname}` - Player's display name
- `{player_level}` - Experience level
- `{player_health}` - Current health
- `{player_max_health}` - Maximum health
- `{player_food}` - Food level
- `{player_ping}` - Ping in milliseconds
- `{player_x}`, `{player_y}`, `{player_z}` - Coordinates
- `{player_world}` - Current world

### Server Information
- `{server_name}` - Server name
- `{server_players}` - Online player count
- `{server_max_players}` - Maximum players
- `{server_tps}` - Server TPS
- `{server_memory_used}` - Used memory (MB)
- `{server_memory_total}` - Total memory (MB)
- `{server_memory_percent}` - Memory usage percentage

### Time & Date
- `{time}` - Current time (HH:mm:ss)
- `{date}` - Current date (yyyy-MM-dd)
- `{datetime}` - Date and time

## Animation System

### Basic Animation
```json
"animations": {
  "enabled": true,
  "updateInterval": 5,
  "sequences": [
    {
      "id": "loading_dots",
      "frames": [
        "&7.",
        "&7..",
        "&7...",
        "&7"
      ],
      "duration": 0.3
    }
  ]
}
```

### Using Animations in Text
```json
"text": "Loading {loading_dots} Please wait..."
```

## Performance Optimization

### Update Intervals
- **Tablist**: 20 ticks (1 second) - Good balance
- **Scoreboard**: 20 ticks (1 second) - Detailed info
- **Bossbar**: 20 ticks (1 second) - Simple bars
- **Animations**: 5-10 ticks (0.25-0.5 seconds) - Smooth animations

### Best Practices
1. **Avoid excessive updates** - Use appropriate intervals
2. **Limit complex calculations** - Cache heavy operations
3. **Use conditions wisely** - Don't check unnecessary permissions
4. **Optimize placeholders** - Use FTB combined placeholders when possible

## Example Configurations

### Minimal Setup
```json
{
  "tablist": {
    "enabled": true,
    "format": "{player_name}",
    "layouts": [
      {
        "priority": 1,
        "conditionType": "default",
        "header": ["&eWelcome {player_name}"],
        "footer": ["&7Online: {server_players}"]
      }
    ]
  }
}
```

### Advanced Team-Based Setup
```json
{
  "tablist": {
    "enabled": true,
    "format": "{ftb_combined_prefix}[{team_name}] {player_name}{ftb_combined_suffix}",
    "layouts": [
      {
        "priority": 200,
        "conditionType": "placeholder",
        "condition": "ftb_team_role:Owner",
        "header": [
          "&e&l╔═══════════════════════════════════╗",
          "&e&l║         &f&lTEAM OWNER           &e&l║",
          "&e&l║ &f{player_name} &7| &bTeam: {ftb_team_display_name}   &e&l║",
          "&e&l╚═══════════════════════════════════╝"
        ],
        "footer": [
          "&e&l╔═══════════════════════════════════╗",
          "&e&l║ &7Members: &e{ftb_team_members} &7| &aRank: {ftb_rank_display_name} &e&l║",
          "&e&l╚═══════════════════════════════════╝"
        ]
      }
    ],
    "playerOrder": [
      { "placeholder": "ftb_rank_weight", "direction": "desc", "asNumber": true },
      { "placeholder": "ftb_team_role", "direction": "desc" }
    ]
  }
}
```

## Migration from Old Configuration

### Old Format
```json
{
  "enableTablist": true,
  "tablistFormat": "[{group}] {player_name}",
  "header": ["&6Staff | &e{player_name}"],
  "footer": ["&7Online: {online_count}"]
}
```

### New Format
```json
{
  "tablist": {
    "enabled": true,
    "format": "[{group}] {player_name}",
    "layouts": [
      {
        "priority": 1,
        "conditionType": "default",
        "header": ["&6Staff | &e{player_name}"],
        "footer": ["&7Online: {server_players}"]
      }
    ]
  }
}
```

The new configuration system provides complete backward compatibility while offering significantly more customization options and better organization.
