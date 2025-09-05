# NeoEssentials Scoreboard Admin Customization

This document provides examples for server administrators to customize scoreboards using the enhanced NeoEssentials scoreboard system.

## Quick Admin Commands

### Basic Scoreboard Management
```
/scoreboard reload          - Reload scoreboard configuration
/scoreboard status          - Show system status
/scoreboard toggle [player] - Toggle scoreboard on/off
/scoreboard update [player] - Force update scoreboard
/scoreboard updateall       - Update all player scoreboards
/scoreboard test <player> <layout> - Test layout (development)
```

### Permission Requirements
- `neoessentials.admin.scoreboard` - Basic admin access
- `neoessentials.admin.scoreboard.reload` - Reload configuration
- `neoessentials.admin.scoreboard.toggle` - Toggle player scoreboards
- `neoessentials.admin.scoreboard.update` - Force updates
- `neoessentials.admin.scoreboard.test` - Test layouts

## Configuration Examples

### Basic Server Information Layout
```json
{
  "name": "server_info",
  "title": "&6&lMyServer Network",
  "priority": 10,
  "conditionType": "default",
  "condition": "",
  "lines": [
    "&7Welcome &a{player_name}",
    "",
    "&7Players Online: &a{server_players}/{server_max_players}",
    "&7Your Level: &e{player_level}",
    "&7Health: &c{player_health}/20",
    "",
    "&7Location:",
    "&7X: &f{player_x} &7Y: &f{player_y} &7Z: &f{player_z}",
    "&7World: &a{player_world}",
    "",
    "&6play.myserver.com"
  ]
}
```

### VIP Player Layout (Higher Priority)
```json
{
  "name": "vip_layout",
  "title": "&6&l&nVIP &r&6&lMyServer",
  "priority": 50,
  "conditionType": "permission",
  "condition": "vip",
  "lines": [
    "&6&lVIP &7Player &a{player_name}",
    "",
    "&7Server: &aOnline &7(&e{server_players}&7)",
    "&7Rank: &6VIP Member",
    "&7Balance: &e$1,000,000",
    "",
    "&7Location: &f{player_x}, {player_y}, {player_z}",
    "&7World: &a{player_world}",
    "",
    "&7&oThanks for supporting us!",
    "&6play.myserver.com"
  ]
}
```

### Admin Layout (Highest Priority)
```json
{
  "name": "admin_layout", 
  "title": "&c&l&nADMIN &r&c&lPanel",
  "priority": 100,
  "conditionType": "permission",
  "condition": "admin",
  "lines": [
    "&c&lADMIN &7- &a{player_name}",
    "",
    "&7Players: &a{server_players}&7/&a{server_max_players}",
    "&7Health: &c{player_health}&7/&c20",
    "&7Level: &e{player_level}",
    "",
    "&7Admin Location:",
    "&7X: &f{player_x}",
    "&7Y: &f{player_y}",
    "&7Z: &f{player_z}",
    "",
    "&c&lSTAFF PANEL ACTIVE"
  ]
}
```

### World-Specific Layout
```json
{
  "name": "nether_layout",
  "title": "&4&lNether Realm",
  "priority": 30,
  "conditionType": "world",
  "condition": "the_nether",
  "lines": [
    "&4&lNether Explorer",
    "&7Player: &a{player_name}",
    "",
    "&7Dangerous realm!",
    "&7Health: &c{player_health}/20",
    "&7Location: &f{player_x}, {player_y}, {player_z}",
    "",
    "&4&lBe careful!",
    "&7Return to overworld with &a/spawn"
  ]
}
```

## Available Placeholders

### Player Information
- `{player_name}` - Player's username
- `{player_health}` - Current health (rounded)
- `{player_level}` - Experience level
- `{player_x}` - X coordinate (rounded)
- `{player_y}` - Y coordinate (rounded) 
- `{player_z}` - Z coordinate (rounded)
- `{player_world}` - Current world name

### Server Information
- `{server_players}` - Current online players
- `{server_max_players}` - Maximum server capacity

## Condition Types

### Permission-Based Conditions
Use `"conditionType": "permission"` with these condition values:
- `"owner"` - Server owners (permission level 4)
- `"admin"` - Administrators (permission level 3)
- `"moderator"` - Moderators (permission level 2)
- `"vip"` - VIP players (permission level 1)

### World-Based Conditions
Use `"conditionType": "world"` with world names:
- `"overworld"` - Main world
- `"the_nether"` - Nether dimension
- `"the_end"` - End dimension
- `"custom_world"` - Any custom world name

### Default Condition
Use `"conditionType": "default"` for fallback layout (lowest priority recommended)

## Color Codes

### Basic Colors
- `&0` - Black
- `&1` - Dark Blue  
- `&2` - Dark Green
- `&3` - Dark Aqua
- `&4` - Dark Red
- `&5` - Dark Purple
- `&6` - Gold
- `&7` - Gray
- `&8` - Dark Gray
- `&9` - Blue
- `&a` - Green
- `&b` - Aqua
- `&c` - Red
- `&d` - Light Purple
- `&e` - Yellow
- `&f` - White

### Text Formatting
- `&l` - Bold
- `&m` - Strikethrough
- `&n` - Underline
- `&o` - Italic
- `&r` - Reset formatting

## Tips for Administrators

1. **Priority System**: Higher priority layouts override lower ones
2. **Line Limits**: Keep layouts under 15 lines for best display
3. **Unique Lines**: Minecraft requires unique scoreboard entries
4. **Testing**: Use `/scoreboard test <player> <layout>` to test designs
5. **Reload**: Use `/scoreboard reload` after configuration changes
6. **Monitoring**: Check `/scoreboard status` for system health

## Troubleshooting

### Layout Not Showing
1. Check priority values (higher = more important)
2. Verify condition logic matches player status
3. Use `/scoreboard status` to see available layouts
4. Test with `/scoreboard test` command

### Performance Issues
1. Reduce update frequency in main config
2. Simplify placeholder usage
3. Monitor with `/scoreboard status`
4. Consider disabling for specific players

### Configuration Errors
1. Validate JSON syntax
2. Check color code formatting
3. Verify placeholder names
4. Use `/scoreboard reload` to test changes

## Advanced Examples

### Economic Layout (requires economy system)
```json
{
  "name": "economy_focus",
  "title": "&e&l$$ Economy $$",
  "priority": 25,
  "conditionType": "default",
  "condition": "",
  "lines": [
    "&e&l$$ &7Player &a{player_name} &e&l$$",
    "",
    "&7Balance: &e$10,000",
    "&7Bank: &e$50,000", 
    "&7Total Worth: &e$60,000",
    "",
    "&7Players Online: &a{server_players}",
    "&7Location: &f{player_x}, {player_y}, {player_z}",
    "",
    "&e&lEconomy Server"
  ]
}
```

This enhanced scoreboard system provides powerful customization while maintaining clean, maintainable code and excellent performance for server administrators.
