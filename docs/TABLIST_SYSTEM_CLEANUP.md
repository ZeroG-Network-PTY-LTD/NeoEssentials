# TabList System Cleanup Documentation

## Problem Description

The tablist system was experiencing conflicts between multiple competing systems that were trying to manage the same tablist display, causing flickering between hardcoded defaults and config-based layouts when animated placeholders were used.

## Root Causes Identified

### 1. Multiple Tablist Systems Running Simultaneously
- **TabListManager**: Main permission-based system with config layouts
- **HeaderFooterManager**: Separate animated header/footer system
- **TabUpdateOrchestrator**: Additional orchestration layer
- **Hardcoded Fallbacks**: Default headers/footers in TabListManager

### 2. Hardcoded Fallback Conflicts
```java
// PROBLEMATIC CODE (FIXED):
private String defaultHeaderText = "&6&l╔═══════════════..."; // Hardcoded fallback
private String defaultFooterText = "&6&l╔═══════════════..."; // Hardcoded fallback

// In updatePlayerHeaderFooter():
String header = defaultHeaderText; // Always started with hardcoded
String footer = defaultFooterText; // Always started with hardcoded
```

### 3. Legacy Method Interference
- `setHeaderText()` and `setFooterText()` were overriding config layouts
- No coordination between systems
- Animated placeholders triggered multiple update paths

## Solutions Implemented

### 1. Eliminated Hardcoded Fallbacks
```java
// FIXED CODE:
private String defaultHeaderText = ""; // Now empty - no hardcoded fallback
private String defaultFooterText = ""; // Now empty - no hardcoded fallback
```

### 2. Config-Only Layout Selection
```java
// NEW APPROACH:
String header = "";  // Start empty - no hardcoded fallback
String footer = "";  // Start empty - no hardcoded fallback

// ONLY use permission-based layout selection
if (selectedLayout != null) {
    // Use config layout
} else {
    // Clear tablist to prevent conflicts instead of using hardcoded fallback
    player.connection.send(new ClientboundTabListPacket(Component.empty(), Component.empty()));
    return;
}
```

### 3. System Coordination Safety Checks
Added conflict prevention between competing systems:

**HeaderFooterManager:**
```java
public void tick(...) {
    // SAFETY CHECK: Don't interfere if TabListManager is handling via config
    if (isTabListManagerActive()) {
        return; // Step back gracefully
    }
    // ... rest of logic
}
```

**TabUpdateOrchestrator:**
```java
public void refreshTablistForAll() {
    // SAFETY CHECK: Don't interfere if TabListManager is handling via config
    if (isTabListManagerActive()) {
        return; // Step back gracefully  
    }
    // ... rest of logic
}
```

### 4. Deprecated Legacy Methods
```java
@Deprecated
public void setHeaderText(String headerText) {
    DebugUtil.warnLog("Use config-based layouts to prevent animated placeholder conflicts");
    // No longer modifies internal state
}
```

## Configuration Structure

### Proper TabList Configuration
The tablist system now relies **exclusively** on config-based layouts in `tablist.json`:

```json
{
  "tablist": {
    "enabled": true,
    "updateInterval": 20,
    "layouts": {
      "default_layout": {
        "priority": 0,
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

## How Animated Placeholders Work Now

### 1. Clean Processing Flow
1. **TabListManager** determines player's permission-based layout
2. **PlaceholderManager** processes all placeholders (including animated ones)  
3. **Only config-based content** is sent to client
4. **No hardcoded fallbacks** to cause conflicts

### 2. Conflict Prevention
- **HeaderFooterManager** checks if TabListManager has active config layouts
- **TabUpdateOrchestrator** steps back when config layouts are present
- **No competing systems** try to manage the same tablist

### 3. Animated Placeholder Support
- Animated placeholders like `{server_status_animation}` work properly
- They're processed within the config layout system
- No switching between hardcoded and config content

## Best Practices Going Forward

### 1. Use Config-Based Layouts Only
```json
// ✅ CORRECT: Define layouts in config
"layouts": {
  "my_layout": {
    "header": ["&eMy animated header with {animated_placeholder}"],
    "footer": ["&7Footer with {time}"]
  }
}
```

```java
// ❌ AVOID: Hardcoded strings in Java code
public void setHeaderText("&eHardcoded header"); // Can cause conflicts
```

### 2. Permission-Based Layout Selection
```json
// ✅ CORRECT: Use permission sets to assign layouts
"permissionSets": {
  "vip": {
    "priority": 600,
    "permission": "neoessentials.tablist.vip", 
    "layoutId": "vip_layout"
  }
}
```

### 3. Test Animated Placeholders
When adding animated placeholders:
1. Add them to config layouts (not Java code)
2. Test with debug logging enabled: `-Dneoessentials.debug.tablist=true`
3. Verify no conflicts in logs

## Debug Commands

### Enable Debug Logging
```bash
# Add to JVM args:
-Dneoessentials.debug.tablist=true
```

### Check Active Systems
```java
// In-game command: /neoessentials debug tablist
// Shows which systems are active and their status
```

## Migration Notes

### If You Have Custom Tablist Code
1. **Remove hardcoded headers/footers** from Java code
2. **Move content to config layouts** in `tablist.json`
3. **Use permission sets** for player-specific layouts
4. **Test animated placeholders** in config

### Legacy Method Usage
- `setHeaderText()` and `setFooterText()` are now deprecated
- Use config-based layouts instead
- Will log warnings if used

## Troubleshooting

### Tablist Not Showing
1. Check if `tablist.enabled = true` in config
2. Verify player has matching permission set
3. Check layout exists and has content
4. Enable debug logging to see selection process

### Animated Placeholders Not Working
1. Ensure placeholder is registered in PlaceholderManager
2. Check it's used within config layout (not hardcoded)
3. Verify no competing systems are interfering
4. Check placeholder update intervals

### Flickering Between Different Tablists
1. This should now be **fixed** with the cleanup
2. If still occurring, check for conflicting mods
3. Enable debug logging to identify the source

## Summary

The tablist system is now **unified** under the config-based TabListManager with proper conflict prevention. This eliminates the animated placeholder conflicts and provides a clean, predictable tablist experience based solely on your configuration files.
