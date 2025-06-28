# Custom Placeholders in NeoEssentials

This guide explains how to create, manage, and use custom placeholders in NeoEssentials to enhance your server's dynamic content.

![Placeholders Icon](../images/icons/placeholders.png)

## What Are Custom Placeholders?

Custom placeholders are user-defined text variables that are dynamically replaced with content at runtime. They allow you to:

- Create server-specific dynamic text
- Display conditional content based on players or server state
- Extend the built-in placeholder system
- Create complex data representations

## Creating Custom Placeholders

### Configuration-Based Placeholders

The simplest way to create custom placeholders is through the `placeholders.json` configuration file:

```json
{
  "custom_placeholders": {
    "server_name": "ZeroG Survival Network",
    "server_difficulty": "%server_difficulty%",
    "welcome_message": "Welcome to our server, %player_name%!",
    "staff_count": "%placeholder:custom_staff_count%",
    "random_tip": "%random:server_tips%"
  },
  "placeholder_lists": {
    "server_tips": [
      "Type /help for a list of commands!",
      "Join our Discord server for updates!",
      "Remember to vote daily for rewards!",
      "Check out our website for more information."
    ]
  }
}
```

### API-Based Placeholders

For more advanced placeholders, you can use the Java API:

```java
import com.zerog.neoessentials.api.placeholder.PlaceholderAPI;
import com.zerog.neoessentials.api.placeholder.PlaceholderContext;

// Static placeholder
PlaceholderAPI.register("server_version", ctx -> {
    return "1.21.1";
});

// Player-specific placeholder
PlaceholderAPI.register("player_balance_formatted", ctx -> {
    if (ctx.getPlayer() != null) {
        double balance = EconomyAPI.getBalance(ctx.getPlayer());
        return String.format("$%.2f", balance);
    }
    return "$0.00";
});

// Context-aware placeholder
PlaceholderAPI.register("distance_to_spawn", ctx -> {
    if (ctx.getPlayer() != null) {
        BlockPos spawnPos = ctx.getPlayer().level().getSharedSpawnPos();
        BlockPos playerPos = ctx.getPlayer().blockPosition();
        double distance = Math.sqrt(
            Math.pow(spawnPos.getX() - playerPos.getX(), 2) +
            Math.pow(spawnPos.getZ() - playerPos.getZ(), 2)
        );
        return String.format("%.1f blocks", distance);
    }
    return "N/A";
});
```

## Placeholder Categories

NeoEssentials organizes placeholders into several categories:

| Category | Prefix | Description | Example |
|----------|--------|-------------|---------|
| **Player** | `player_` | Player-specific data | `%player_name%`, `%player_health%` |
| **Server** | `server_` | Server information | `%server_tps%`, `%server_uptime%` |
| **World** | `world_` | World-related data | `%world_time%`, `%world_weather%` |
| **Economy** | `economy_` | Economy information | `%economy_balance%`, `%economy_top_1%` |
| **Custom** | `custom_` | User-defined placeholders | `%custom_welcome%`, `%custom_rules%` |
| **Math** | `math_` | Mathematical calculations | `%math:add(1,2)%`, `%math:max(player_health,10)%` |
| **Random** | `random:` | Random selection | `%random:list_name%` |
| **Conditional** | `condition:` | Conditional text | `%condition:player_op?Admin:Player%` |

## Advanced Placeholder Features

### Nested Placeholders

Placeholders can contain other placeholders:

```
%custom_welcome_message% → "Welcome to %server_name%, %player_name%!"
```

### Placeholder Functions

Apply functions to placeholder results:

```
%player_name:uppercase% → "PLAYERNAME"
%player_health:format(%.1f)% → "19.5"
%player_balance:suffix(coins)% → "1250 coins"
```

### Conditional Placeholders

Display different content based on conditions:

```
%condition:player_op?You are an admin:You are a player%
%condition:economy_balance>1000?You are rich:You need more money%
```

### Math Expressions

Perform calculations within placeholders:

```
%math:multiply(player_level,5)%
%math:max(player_health,10)%
%math:round(economy_balance/100)%
```

## Placeholder Context

When creating custom placeholders with the API, you have access to the `PlaceholderContext` which provides:

- The player (if applicable)
- The server instance
- The world/level
- Additional parameters passed to the placeholder

```java
PlaceholderAPI.register("custom_placeholder", ctx -> {
    Player player = ctx.getPlayer();  // Can be null for server-wide placeholders
    MinecraftServer server = ctx.getServer();
    Level level = ctx.getLevel();  // Player's current level/world
    String param = ctx.getParameter("param1");  // %custom_placeholder:param1%
    
    // Your placeholder logic here
    return result;
});
```

## Using Placeholders

Placeholders can be used in various NeoEssentials systems:

### Tablist Templates

```json
{
  "header": "Welcome to %server_name%",
  "footer": "Players online: %server_online% | TPS: %server_tps%"
}
```

### Command Messages

```json
{
  "commands": {
    "balance": {
      "messages": {
        "self": "Your balance: %economy_balance_formatted%",
        "other": "%player_name%'s balance: %economy_player_balance_formatted%"
      }
    }
  }
}
```

### Custom Text Files

```
# motd.txt
Welcome to %server_name%!
There are currently %server_online% players online.
Your balance is %economy_balance_formatted%.
```

## Best Practices

- Create descriptive placeholder names
- Document your custom placeholders for server administrators
- Consider performance implications for complex placeholders
- Use conditional placeholders for different player groups
- Test placeholders with different player contexts

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Placeholder not working | Check proper syntax with % symbols |
| Empty result | Verify the placeholder exists and has a valid value |
| Performance issues | Optimize placeholder code, especially in tablist or frequently used areas |
| Circular references | Avoid placeholders that reference each other in loops |

## Related Documentation

- [Placeholders](Placeholders) - Built-in placeholders reference
- [API Documentation](API-Documentation) - Advanced placeholder API usage
- [Text Formatting](Text-Formatting) - Combining placeholders with text formatting
- [Tablist System](Tablist-System) - Using placeholders in the tablist

---

*For more information on custom placeholders, join our [Discord server](https://discord.gg/dUGAQF2Mga) or check the [GitHub repository](https://github.com/ZeroG-Network/NeoEssentials).*
