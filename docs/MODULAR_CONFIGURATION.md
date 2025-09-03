# NeoEssentials Modular Configuration System

## Overview
NeoEssentials now uses a **modular configuration approach** where each major system has its own dedicated configuration file. This provides better organization, easier maintenance, and cleaner separation of concerns.

## Configuration File Structure

```
config/neoessentials/
├── tablist.json          # Tablist display configuration
├── scoreboard.json       # Scoreboard display configuration  
├── bossbar.json         # Bossbar display configuration
├── discord.json         # Discord integration configuration
├── animations.json      # Animation sequences
├── permissions.json     # Permission system configuration
├── placeholders.json    # Custom placeholders
├── settings.json        # General mod settings
└── commands.json        # Command configuration
```

## File Purposes

### 📋 `tablist.json`
**Purpose**: Configure tablist headers, footers, and player list formatting
- Player ordering rules
- Conditional layouts based on permissions/placeholders
- Header and footer animations
- FTB Teams integration display

### 📊 `scoreboard.json` 
**Purpose**: Configure side scoreboard displays
- Multiple conditional layouts
- Title animations
- Player statistics display
- Team and rank information

### 📢 `bossbar.json`
**Purpose**: Configure top-screen bossbar displays
- Multiple simultaneous bossbars
- Progress animations
- Special event notifications
- Conditional visibility

### 💬 `discord.json`
**Purpose**: Discord integration and role synchronization
- **Discord Role ID mappings** (not role names!)
- Chat synchronization
- Event notifications
- Webhook configuration

### 🎬 `animations.json`
**Purpose**: Animation sequences used across all systems
- Text animations
- Color cycling
- Progress bar animations

### 🔐 `permissions.json`
**Purpose**: Permission system configuration
- Permission groups
- Inheritance rules
- Default permissions

## Benefits of Modular Configuration

### ✅ **Better Organization**
- Each system has its own dedicated file
- Easier to find and edit specific configurations
- Reduced file complexity

### ✅ **Easier Maintenance**
- Changes to one system don't affect others
- Smaller, more manageable files
- Clear separation of concerns

### ✅ **Improved Performance**
- Only load configurations for enabled systems
- Faster parsing of smaller files
- Better memory usage

### ✅ **Version Control Friendly**
- Smaller diffs when making changes
- Easier to track changes to specific systems
- Better collaboration

## Migration from Single File

If you previously used a single configuration file:

1. **Backup your existing configuration**
2. **Use the new modular files** - they contain all the same functionality
3. **Update any custom configurations** to match the new structure
4. **Test each system individually** to ensure proper functionality

## Configuration Loading Order

The mod loads configurations in this order:
1. `settings.json` - Core mod settings
2. `permissions.json` - Permission system
3. `placeholders.json` - Custom placeholders
4. `animations.json` - Animation sequences
5. `tablist.json` - Tablist system
6. `scoreboard.json` - Scoreboard system
7. `bossbar.json` - Bossbar system
8. `discord.json` - Discord integration

## Cross-System References

Some systems can reference configurations from other files:

### Animations
Tablist, scoreboard, and bossbar configurations can reference animation sequences from `animations.json`:
```json
"titleAnimation": "server_status"  // References animation ID from animations.json
```

### Placeholders
All systems can use placeholders defined in `placeholders.json` and the core placeholder system.

### Permissions
Discord role mappings in `discord.json` link to permissions that can be defined in `permissions.json`.

## Best Practices

### 📁 **File Organization**
- Keep related configurations in their respective files
- Use descriptive IDs and names
- Add comments for complex configurations

### 🔄 **Updates**
- Update one system at a time
- Test changes before deploying to production
- Keep backups of working configurations

### 🛠️ **Debugging**
- Check logs for configuration loading errors
- Verify JSON syntax in each file
- Test individual systems separately

### 📝 **Documentation**
- Comment complex conditional logic
- Document custom placeholders and animations
- Keep track of Discord role IDs

## Common Issues

### ❌ **File Not Found**
- Ensure all required configuration files exist
- Check file names and paths
- Verify proper file permissions

### ❌ **JSON Syntax Errors**
- Validate JSON syntax in each file
- Check for missing commas or brackets
- Use a JSON validator tool

### ❌ **Cross-References Not Working**
- Verify referenced IDs exist in their respective files
- Check animation and placeholder references
- Ensure proper loading order dependencies

## Configuration Examples

### Simple Tablist Layout
```json
{
  "tablist": {
    "enabled": true,
    "layouts": [
      {
        "priority": 100,
        "conditionType": "default",
        "header": ["&6Welcome to the server!"],
        "footer": ["&7Online: {server_players}"]
      }
    ]
  }
}
```

### Discord Role Mapping
```json
{
  "discord": {
    "roleSync": {
      "enabled": true,
      "roleMappings": {
        "987654321012345678": {
          "_roleName": "Admin",
          "minecraftPermission": "neoessentials.admin",
          "priority": 800
        }
      }
    }
  }
}
```

### Conditional Bossbar
```json
{
  "bossbar": {
    "layouts": {
      "admin_bar": {
        "conditionType": "permission",
        "condition": "neoessentials.admin",
        "bars": [
          {
            "text": "&c&lAdmin Panel Active",
            "color": "RED",
            "progress": 1.0
          }
        ]
      }
    }
  }
}
```

---

## Quick Reference

| System | File | Purpose |
|--------|------|---------|
| Tablist | `tablist.json` | Player list headers/footers |
| Scoreboard | `scoreboard.json` | Side display panels |
| Bossbar | `bossbar.json` | Top screen notifications |
| Discord | `discord.json` | Discord role sync & chat |
| Animations | `animations.json` | Reusable animations |
| Permissions | `permissions.json` | Permission groups |
| Placeholders | `placeholders.json` | Custom variables |
| Settings | `settings.json` | Core mod settings |

This modular approach makes NeoEssentials much more maintainable and easier to configure! 🎉
