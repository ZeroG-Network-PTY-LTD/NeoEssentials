# PlaceholderAPI - Developer Documentation

NeoEssentials provides a comprehensive **PlaceholderAPI** system that allows mods to register and resolve custom placeholders in text strings. This system is used internally for chat formatting, join/quit messages, and other text templating needs.

## Overview

The PlaceholderAPI provides:
- **Thread-safe placeholder registration and resolution**
- **Extensible system for other mods to add custom placeholders**  
- **Built-in NeoEssentials placeholders** (player info, economy, permissions, etc.)
- **Automatic PREFIX/SUFFIX integration** with the permission system
- **Support for placeholder expansions** (grouped placeholders)

## Basic Usage

### For Chat Messages and Text Formatting

```java
import com.zerog.neoessentials.api.PlaceholderAPI;
import net.minecraft.server.level.ServerPlayer;

// Replace placeholders in text for a specific player
ServerPlayer player = /* get player */;
String template = "Welcome {prefix}{displayname}! You have {balance} coins.";
String result = PlaceholderAPI.setPlaceholders(player, template);
// Result: "Welcome [VIP] PlayerName! You have 1500.0 coins."
```

### Registering Simple Placeholders

```java
import com.zerog.neoessentials.api.PlaceholderAPI;

// Register a simple placeholder
PlaceholderAPI.registerPlaceholder("mymod_time", (player, params) -> {
    return java.time.LocalTime.now().toString();
});

// Now {mymod_time} will be replaced with current time
String text = PlaceholderAPI.setPlaceholders(player, "Current time: {mymod_time}");
```

### Registering Placeholder Expansions

For mods that provide many related placeholders, use expansions:

```java
import com.zerog.neoessentials.api.PlaceholderExpansion;
import java.util.Set;
import java.util.HashSet;

public class MyModPlaceholderExpansion extends PlaceholderExpansion {
    
    @Override
    public String getIdentifier() {
        return "mymod"; // Prefix for all placeholders: mymod_*
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getAuthor() {
        return "YourName";
    }
    
    @Override
    public Set<String> getPlaceholders() {
        Set<String> placeholders = new HashSet<>();
        placeholders.add("level");      // {mymod_level}
        placeholders.add("experience"); // {mymod_experience}  
        placeholders.add("rank");       // {mymod_rank}
        return placeholders;
    }
    
    @Override
    public String onPlaceholderRequest(ServerPlayer player, String identifier, String params) {
        if (player == null) return null;
        
        return switch (identifier) {
            case "level" -> String.valueOf(getPlayerLevel(player));
            case "experience" -> String.valueOf(getPlayerXP(player));
            case "rank" -> getPlayerRank(player);
            default -> null;
        };
    }
}

// Register the expansion
PlaceholderAPI.registerExpansion(new MyModPlaceholderExpansion());
```

## Built-in NeoEssentials Placeholders

### Player Identity
- `{neoessentials_displayname}` - Player's display name
- `{neoessentials_username}` - Player's raw username  
- `{neoessentials_name}` - Alias for username

### Permission System
- `{neoessentials_prefix}` - Player's permission prefix
- `{neoessentials_suffix}` - Player's permission suffix
- `{neoessentials_group}` - Player's primary permission group

### Location & World
- `{neoessentials_world}` - Current world name
- `{neoessentials_x}` - X coordinate (integer)
- `{neoessentials_y}` - Y coordinate (integer) 
- `{neoessentials_z}` - Z coordinate (integer)
- `{neoessentials_biome}` - Current biome name

### Player Status
- `{neoessentials_health}` - Current health (formatted)
- `{neoessentials_max_health}` - Maximum health (formatted)
- `{neoessentials_food}` - Food level (0-20)
- `{neoessentials_level}` - Experience level
- `{neoessentials_exp}` - Experience progress (percentage)
- `{neoessentials_gamemode}` - Current gamemode

### Economy
- `{neoessentials_balance}` - Raw balance amount
- `{neoessentials_balance_formatted}` - Formatted balance with decimals

### Server Information
- `{neoessentials_server_name}` - Server name/MOTD
- `{neoessentials_online_players}` - Current online player count
- `{neoessentials_max_players}` - Maximum player slots

### Date & Time
- `{neoessentials_time}` - Current time (12-hour format)
- `{neoessentials_time_24}` - Current time (24-hour format)
- `{neoessentials_date}` - Current date (YYYY-MM-DD)

## Advanced Features

### Placeholder Parameters

Placeholders can accept parameters using the colon syntax:

```java
// Register a placeholder that accepts parameters
PlaceholderAPI.registerPlaceholder("mymod_player_stat", (player, params) -> {
    if (params == null) return "No stat specified";
    
    return switch (params) {
        case "kills" -> String.valueOf(getPlayerKills(player));
        case "deaths" -> String.valueOf(getPlayerDeaths(player));
        default -> "Unknown stat: " + params;
    };
});

// Usage: {mymod_player_stat:kills} or {mymod_player_stat:deaths}
```

### Checking Placeholder Registration

```java
// Check if a placeholder exists
boolean exists = PlaceholderAPI.isPlaceholderRegistered("mymod_level");

// Get all registered placeholders
Set<String> allPlaceholders = PlaceholderAPI.getRegisteredPlaceholders();

// Get single placeholder value
String value = PlaceholderAPI.getPlaceholderValue(player, "neoessentials_balance", null);
```

### Thread Safety

All PlaceholderAPI methods are thread-safe and can be called from any thread. The internal placeholder resolution is designed to handle concurrent access safely.

## Integration Examples

### Chat Formatting

```java
// Custom chat format with placeholders
String chatFormat = "&7[{neoessentials_world}] {neoessentials_prefix}{neoessentials_displayname}&f: {MESSAGE}";
String result = PlaceholderAPI.setPlaceholders(player, chatFormat.replace("{MESSAGE}", message));
```

### Join Messages

```java
// Custom join message
String joinTemplate = "&a➤ {neoessentials_prefix}{neoessentials_displayname} &ejoined from {neoessentials_world}!";
String joinMessage = PlaceholderAPI.setPlaceholders(player, joinTemplate);
```

### Scoreboard/HUD Integration  

```java
// Update player scoreboard with live data
String scoreboardTemplate = """
    &6&lPlayer Info
    &7Name: &f{neoessentials_displayname}
    &7Balance: &a${neoessentials_balance_formatted}  
    &7World: &b{neoessentials_world}
    &7Health: &c{neoessentials_health}&7/&c{neoessentials_max_health}
    """;
    
String scoreboardText = PlaceholderAPI.setPlaceholders(player, scoreboardTemplate);
```

## Best Practices

1. **Use descriptive placeholder names** with your mod prefix: `mymod_feature_name`
2. **Handle null players gracefully** - some placeholders work without player context
3. **Cache expensive calculations** in your placeholder providers
4. **Use expansions for related placeholders** rather than individual registrations
5. **Document your placeholders** for other developers
6. **Test placeholder performance** with many concurrent players

## API Compatibility

The PlaceholderAPI is designed to be:
- **Backward compatible** - existing code won't break with updates
- **Forward compatible** - new features won't break existing placeholders  
- **Mod-friendly** - easy integration with other NeoForge mods
- **Performance-focused** - minimal overhead for placeholder resolution

## Performance Considerations

- Placeholder resolution is cached where possible
- Complex calculations should be cached in your providers
- Avoid file I/O or network calls in placeholder providers
- Use `@Nullable` annotations for proper null handling

## Error Handling

```java
try {
    String result = PlaceholderAPI.setPlaceholders(player, template);
} catch (Exception e) {
    // PlaceholderAPI handles most errors internally
    // Your placeholder providers should handle their own exceptions
    LOGGER.error("Placeholder error: {}", e.getMessage());
}
```

The PlaceholderAPI system provides robust error handling and will:
- Return the original placeholder if resolution fails
- Log errors for debugging
- Never crash the server due to placeholder issues
- Gracefully handle null or invalid inputs