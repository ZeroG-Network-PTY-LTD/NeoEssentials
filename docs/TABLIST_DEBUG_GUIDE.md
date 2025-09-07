# TabList Debug Test Guide

## Problem: TabList headers/footers not loading

The tablist system was cleaned up to prevent conflicts with animated placeholders, but now it's not showing headers/footers at all.

## Debug Steps

### 1. Enable Debug Logging
Add this to your server startup arguments:
```bash
-Dneoessentials.debug.tablist=true
```

### 2. Check Configuration File
Verify your `run/config/neoessentials/tablist.json` file exists and has content like:
```json
{
  "tablist": {
    "enabled": true,
    "layouts": {
      "default_layout": {
        "header": [
          "&6&l╔═══════════════════════════════════╗",
          "&6&l║         &f&lNeoEssentials         &6&l║",
          "&6&l║ &7Welcome &e{player_name}           &6&l║",
          "&6&l╚═══════════════════════════════════╝"
        ],
        "footer": [
          "&6&l╔═══════════════════════════════════╗",
          "&6&l║ &7Online: &e{server_players}&7/&e{server_max_players} &6&l║",
          "&6&l║ &7Time: &f{time}                   &6&l║",
          "&6&l╚═══════════════════════════════════╝"
        ]
      }
    },
    "permissionSets": {
      "default": {
        "priority": 0,
        "layoutId": "default_layout"
      }
    }
  }
}
```

### 3. Use Debug Commands
In-game admin commands:
```
/tablistdebug status   - Shows system status and available layouts
/tablistdebug refresh  - Reloads the configuration
/tablistdebug layout   - Shows your current layout assignment
```

### 4. Check Server Logs
Look for these log messages:
- `[TabListManager] Config loaded with X layouts`
- `[TabListManager] Using layout 'default_layout' for player X`
- `[TabListManager] No config layouts available for player X`

### 5. Expected Debug Output
With debug enabled, you should see:
```
[TabListManager] Professional TabList Manager initialized
[TabListManager] Config loaded with 2 layouts - using ONLY permission-based selection
[TabListManager] Config available with 2 layouts for player Steve
[TabListManager] Using layout 'default_layout' with priority 0 for player Steve
[TabListManager] Updated tablist for player Steve - Header length: 120, Footer length: 89
```

## Troubleshooting

### If No Layouts Found:
- Check if config file exists at `run/config/neoessentials/tablist.json`
- If missing, the system should create default layouts automatically
- Look for `[TabListManager] Default config created with X layouts` in logs

### If Config File Missing:
The system should automatically create default layouts. If you see:
```
[TabListManager] Config file not found, using default configuration
[TabListManager] Default config created with 2 layouts:
[TabListManager]   - default_layout
[TabListManager]   - vip_layout
```

### If Still No Headers/Footers:
1. Check if `tablist.enabled = true` in config
2. Verify player has matching permission set (default should work for everyone)
3. Look for error messages in logs
4. Try manually reloading with `/tablistdebug refresh`

### Common Fixes:
1. **Missing Config**: System should auto-create, but you can manually create the file
2. **Permission Issues**: Ensure player has access to at least the "default" permission set
3. **Layout Mismatch**: Verify permission sets point to existing layout IDs
4. **Service Conflicts**: Check if other tablist plugins/mods are interfering

## Testing Animated Placeholders
Once basic tablist is working, test animated placeholders:
```json
{
  "tablist": {
    "layouts": {
      "default_layout": {
        "header": [
          "&6&lServer Status: {server_status_animation}",
          "&7Welcome {player_name}!"
        ]
      }
    }
  }
}
```

The animated placeholder should now work without causing conflicts or flickering.

## Recovery Steps
If you need to completely reset:
1. Stop server
2. Delete `run/config/neoessentials/tablist.json`  
3. Start server (will recreate with defaults)
4. Test basic functionality
5. Add your custom layouts back gradually
