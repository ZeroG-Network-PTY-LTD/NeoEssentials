# Tablist Display System

NeoEssentials provides a sophisticated permission-based tablist system that automatically displays customized headers, footers, and player formatting based on player permissions and groups. The system is fully configuration-driven and requires no manual commands.

## 🎯 Overview

The tablist system automatically provides:
- **Permission-based layouts**: Different display layouts based on player permissions
- **Automatic header/footer**: Dynamic content that updates based on placeholders
- **Team-based formatting**: Player name prefixes and suffixes from permission groups
- **FTB Integration**: Support for FTB placeholder system
- **Real-time updates**: Live updates when permissions or data changes

## 🔧 How It Works

### Automatic Permission-Based Display

The TabListManager automatically:
1. **Detects player permissions** when they join or permissions change
2. **Selects appropriate layout** based on highest priority permission they have
3. **Applies header/footer** from the selected layout configuration
4. **Sets prefix/suffix** based on their permission group
5. **Updates in real-time** when permissions change

### Configuration-Driven System

All tablist customization is done through `config/neoessentials/tablist.json`:

#### Layout Structure
```json
{
  "tablist": {
    "enabled": true,
    "updateInterval": 20,
    "layouts": {
      "default_layout": {
        "priority": 0,
        "conditionType": "default",
        "condition": "",
        "header": ["&7Welcome to NeoEssentials Server!", "&fOnline: &a{server_players}"],
        "footer": ["&7TPS: &a{server_tps}", "&7Thanks for playing!"]
      },
      "vip_layout": {
        "priority": 600,
        "conditionType": "permission", 
        "condition": "neoessentials.tablist.vip",
        "header": ["&d&lVIP Area", "&fWelcome back, VIP!"],
        "footer": ["&dVIP perks active", "&7Enjoy your stay!"]
      }
    },
    "permissionSets": {
      "default": {
        "priority": 0,
        "conditionType": "default",
        "layoutId": "default_layout",
        "prefix": "&7",
        "suffix": ""
      },
      "vip": {
        "priority": 600,
        "conditionType": "permission",
        "permission": "neoessentials.tablist.vip", 
        "layoutId": "vip_layout",
        "prefix": "&d[VIP] ",
        "suffix": " &d★"
      }
    }
  }
}
```

## 🛠️ Available Commands

### Debug Commands (Admin Only)

#### `/tablisttest` - Tablist Testing Command
Debug and test tablist functionality:

```bash
/tablisttest permissions <player>    # View player permissions and group info
/tablisttest group <player> <group>  # Set player group for testing
/tablisttest groups                  # List all available permission groups
/tablisttest reload                  # Reload tablist system
```

**Permission Required**: Operator level 2+

#### `/tablistdebug` - Tablist Debug Command  
Advanced debugging for tablist issues:

```bash
/tablistdebug status    # Show TabListManager status
/tablistdebug refresh   # Force refresh tablist
/tablistdebug layout    # Show player layout information
```

**Permission Required**: Operator level 2+

## 🎨 Layout Configuration

### Layout Priority System

Layouts are selected by priority - higher priority layouts override lower ones:

1. **Priority 800+**: Staff/Admin layouts
2. **Priority 600-799**: VIP/Premium layouts  
3. **Priority 400-599**: Helper/Supporter layouts
4. **Priority 200-399**: Member layouts
5. **Priority 0-199**: Default layouts

### Condition Types

#### Default Condition
```json
{
  "conditionType": "default",
  "condition": ""
}
```
Everyone qualifies - used as fallback.

#### Permission Condition
```json
{
  "conditionType": "permission", 
  "condition": "neoessentials.tablist.vip"
}
```
Player must have the specified permission.

### Header and Footer Content

Headers and footers support:
- **Multiple lines**: Array of strings, each becomes a new line
- **Placeholder integration**: All NeoEssentials placeholders work
- **Color codes**: Full Minecraft color code support
- **Dynamic updates**: Content updates automatically with placeholder changes

## 📊 Placeholder Support

### Built-in Placeholders
The tablist system supports all NeoEssentials placeholders:

#### Server Information
- `{server_players}` - Current online players
- `{server_max_players}` - Maximum server slots
- `{server_tps}` - Current server TPS
- `{server_memory_percent}` - Memory usage percentage

#### Player Information  
- `{player_name}` - Player's display name
- `{player_ping}` - Player's connection ping
- `{player_world}` - Player's current world
- `{player_health}` - Player's health points

#### FTB Integration
- `{ftb_team_name}` - FTB team name (if available)
- `{ftb_team_color}` - FTB team color (if available)
- All other FTB placeholders from FTB Teams mod

## 🔄 Automatic Updates

### Permission Change Detection
The system automatically updates when:
- Player permissions are modified via `/permissions` commands
- Player groups are changed
- Permission inheritance is updated
- Permission nodes are added/removed

### Real-time Refresh
Updates happen automatically for:
- **Header/footer content**: Updates every 20 ticks (configurable)
- **Placeholder values**: Updates with data changes
- **Permission-based layouts**: Instant update when permissions change
- **Group prefixes/suffixes**: Instant update when group changes

## ⚙️ Configuration Options

### Global Settings
```json
{
  "tablist": {
    "enabled": true,           // Enable/disable tablist system
    "updateInterval": 20,      // Update interval in ticks (20 = 1 second)
    "debugMode": false         // Enable debug logging
  }
}
```

### Bossbar Integration
The tablist configuration also includes bossbar layouts:

```json
{
  "bossbar": {
    "enabled": true,
    "updateInterval": 20,
    "layouts": {
      "default_bossbar": {
        "priority": 0,
        "conditionType": "default",
        "message": "&eWelcome to NeoEssentials! | Online: &a{server_players}",
        "color": "YELLOW",
        "style": "SOLID",
        "progress": 1.0
      }
    }
  }
}
```

## 🔍 Troubleshooting

### Common Issues

#### Layout Not Applying
**Symptoms**: Player seeing default layout instead of expected layout
**Solutions**:
1. Check player has required permission: `/tablisttest permissions <player>`
2. Verify layout priority is higher than competing layouts
3. Check condition syntax in configuration
4. Use `/tablistdebug layout` to see active layout selection

#### Placeholders Not Working
**Symptoms**: Placeholders showing as literal text like `{server_players}`
**Solutions**:
1. Verify placeholder name is correct (check PlaceholderManager)
2. Ensure placeholder system is loaded
3. Check for typos in placeholder names
4. Test with simple placeholders first

#### Prefixes/Suffixes Not Showing
**Symptoms**: Player names not showing group prefixes or suffixes
**Solutions**:
1. Verify player is in correct permission group: `/tablisttest group <player> <group>`
2. Check permissionSets configuration matches permission groups
3. Ensure permission set has proper priority
4. Use `/tablisttest groups` to list available groups

### Debug Mode

Enable debug logging by setting system property:
```bash
-Dneoessentials.debug.tablist=true
```

This provides detailed logging of:
- Layout selection process
- Permission checks
- Header/footer determination
- Update events

## 🎯 Best Practices

### Layout Design
- **Use clear priorities**: Space priorities apart (e.g., 0, 100, 200) for easy insertion
- **Test thoroughly**: Use debug commands to verify layouts work correctly
- **Keep content concise**: Tablist space is limited, avoid overly long content
- **Use placeholders wisely**: Too many placeholders can impact performance

### Permission Setup
- **Follow hierarchy**: Higher ranks should have higher priority permissions
- **Use inheritance**: Set up permission group inheritance properly
- **Test permission changes**: Use `/tablisttest` to verify permission-based layouts
- **Document layouts**: Keep notes on which permissions trigger which layouts

---

**Related Documentation**: [Permissions](Permissions.md) | [Placeholders](Placeholders.md) | [Configuration](Configuration.md)

*Last Updated: September 7, 2025 - NeoEssentials 2.1.0*
