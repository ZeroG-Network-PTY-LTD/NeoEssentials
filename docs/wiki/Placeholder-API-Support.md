# Placeholder API Support

NeoEssentials provides comprehensive integration with Placeholder API, allowing you to use a wide range of dynamic placeholders from other mods in your NeoEssentials messages, templates, and configurations.

## Overview

Placeholder API is a platform for mods to provide placeholders that can be used in various contexts. NeoEssentials both consumes placeholders from other mods and provides its own placeholders for use by other mods.

## Requirements

To use Placeholder API placeholders in NeoEssentials:

1. Install [Placeholder API for NeoForge](https://placeholderapi.com/downloads/neoforge) on your server
2. Install mods that provide placeholders via Placeholder API
3. NeoEssentials will automatically detect and integrate with Placeholder API

## Using External Placeholders in NeoEssentials

You can use Placeholder API placeholders in NeoEssentials by enclosing them in curly braces with the `papi:` prefix:

```
{papi:placeholder_name}
```

For example:
```
{papi:server_tps}
{papi:player_name}
{papi:essentials_nickname}
```

### Usage in Templates

You can use Placeholder API placeholders in any NeoEssentials template:

```json
{
  "templates": {
    "welcome": {
      "lines": [
        "&6Welcome, &e{papi:player_name}&6!",
        "&7Your balance is &a{papi:economy_balance}"
      ]
    }
  }
}
```

### Usage in Configuration Files

Use Placeholder API placeholders in any NeoEssentials configuration:

```toml
[messages]
welcome = "&6Welcome to the server, &e{papi:player_displayname}&6!"
balance = "&aYour balance is &e{papi:economy_balance}"
```

### Usage in Commands

You can use Placeholder API placeholders in command messages:

```
/broadcast "&aServer uptime: &e{papi:server_uptime}"
```

## Using NeoEssentials Placeholders in Other Mods

NeoEssentials registers its own placeholders with Placeholder API, making them available to other mods that support Placeholder API.

### Available NeoEssentials Placeholders

These placeholders are exposed to other mods through Placeholder API:

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%neoessentials_balance%` | Player's balance | 1500 |
| `%neoessentials_balance_formatted%` | Formatted balance | $1,500.00 |
| `%neoessentials_homes_count%` | Number of player's homes | 5 |
| `%neoessentials_homes_max%` | Max homes allowed | 10 |
| `%neoessentials_warps_count%` | Number of available warps | 15 |
| `%neoessentials_kits_available%` | Number of available kits | 3 |
| `%neoessentials_currency_name%` | Currency name | Coins |
| `%neoessentials_currency_symbol%` | Currency symbol | $ |
| `%neoessentials_playtime%` | Player's total playtime | 10h 30m |
| `%neoessentials_playtime_seconds%` | Playtime in seconds | 37800 |
| `%neoessentials_first_join%` | First join date | 2025-06-01 |
| `%neoessentials_last_join%` | Last join date | 2025-06-25 |

## Advanced Placeholder Usage

### Nested Placeholders

You can nest placeholders within other placeholders:

```
{if:papi:economy_balance>1000}You're rich!{else}Keep saving!{/if}
```

### Placeholder Processing Order

When multiple placeholder systems are used, they are processed in this order:

1. NeoEssentials internal variables
2. NeoEssentials global placeholders
3. Placeholder API placeholders
4. Conditional processing

### Placeholder Formatting

Apply formatting to Placeholder API values:

```
{papi:economy_balance:format:%.2f} → 1500.00
```

## Popular Placeholder API Expansions

Here are some popular Placeholder API expansions and their placeholders that work well with NeoEssentials:

### Essential Placeholders

```
%player_name% - Player's name
%player_displayname% - Player's display name
%player_uuid% - Player's UUID
%player_health% - Player's health
%player_food_level% - Player's food level
%player_gamemode% - Player's game mode
%player_world% - Player's world
%player_x% - Player's X coordinate
%player_y% - Player's Y coordinate
%player_z% - Player's Z coordinate
%server_online% - Online players count
%server_max_players% - Maximum players
%server_ram_used% - Used RAM
%server_ram_total% - Total RAM
%server_tps% - Server TPS
```

### LuckPerms Placeholders

```
%luckperms_prefix% - Player's prefix
%luckperms_suffix% - Player's suffix
%luckperms_primary_group% - Player's primary group
%luckperms_primary_group_prefix% - Primary group's prefix
%luckperms_meta_<key>% - Meta value
%luckperms_has_permission_<permission>% - Permission check
%luckperms_in_group_<group>% - Group membership check
```

### Economy Placeholders

```
%economy_balance% - Player's balance
%economy_formatted% - Formatted balance
%economy_currency_name% - Currency name
%economy_currency_symbol% - Currency symbol
%economy_top_balance_<position>% - Top balance at position
%economy_top_player_<position>% - Player name at position
```

## Setting Up Custom Placeholders

You can create custom placeholders that combine multiple placeholders:

### In NeoEssentials Configuration

```toml
[placeholders.custom]
player_info = "&7Name: &f{player} &7Balance: &f{balance} &7Homes: &f{homes_count}/{homes_max}"
server_stats = "&7Online: &f{online_players}/{max_players} &7TPS: &f{tps}"
```

### Using Custom Placeholders

Once defined, you can use them anywhere:

```
{player_info}
{server_stats}
```

## Performance Considerations

- Use placeholders efficiently to avoid unnecessary processing
- Cache complex placeholder results when possible
- Consider performance impact when using many placeholders in frequently updated text
- Enable placeholder caching in `config/neoessentials/config.toml`:

```toml
[performance]
cachePlaceholders = true
placeholderCacheTime = 30  # Seconds
```

## Troubleshooting

### Common Issues

- **Placeholder Not Working**: Verify the placeholder name and format
- **Missing Expansion**: Ensure the required expansion is installed
- **Incorrect Prefix**: Check if you're using `{papi:placeholder}` format correctly
- **Cache Issues**: Try reloading placeholders with `/neoessentials:reload placeholders`

### Debug Mode

Enable placeholder debugging:

```
/neoessentials:debug placeholders on
```

This will log placeholder processing to the console.

### Reloading Placeholders

If you install new placeholder expansions or update configurations:

```
/neoessentials:reload placeholders
```

## Example Use Cases

### Dynamic Welcome Message

```json
{
  "templates": {
    "welcome": {
      "lines": [
        "&6&l=== {papi:server_name} ===",
        "&eWelcome, &b{player}&e!",
        "",
        "&7» &eCurrent Balance: &a{papi:economy_balance_formatted}",
        "&7» &eRank: &a{papi:luckperms_primary_group}",
        "&7» &eOnline Players: &a{papi:server_online}&7/&a{papi:server_max_players}",
        "",
        "&7» &eServer TPS: &a{papi:server_tps}",
        "&7» &eServer Uptime: &a{papi:server_uptime}",
        "",
        "&6Enjoy your stay!"
      ]
    }
  }
}
```

### Dynamic Tablist Header/Footer

```json
{
  "templates": {
    "tablist": {
      "header": {
        "lines": [
          "&6&l=== {papi:server_name} ===",
          "&eWelcome, &b{player}&e!",
          "&7Today is &f{date:dd/MM/yyyy}&7, &f{time:HH:mm}&7 server time",
          ""
        ]
      },
      "footer": {
        "lines": [
          "&7» &ePlayers: &a{papi:server_online}&7/&a{papi:server_max_players}",
          "&7» &eTPS: &a{papi:server_tps} &7Uptime: &a{papi:server_uptime}",
          "&7» &eBalance: &a{papi:economy_balance_formatted} &7Rank: &a{papi:luckperms_primary_group}",
          "",
          "&6&nwww.yourserver.com"
        ]
      }
    }
  }
}
```

### Status Board

```json
{
  "templates": {
    "status_board": {
      "lines": [
        "&6&l=== Player Stats ===",
        "",
        "&7» &eBalance: &a{papi:economy_balance_formatted}",
        "&7» &eRank: &a{papi:luckperms_primary_group}",
        "&7» &eHomes: &a{papi:neoessentials_homes_count}/{papi:neoessentials_homes_max}",
        "&7» &ePlaytime: &a{papi:neoessentials_playtime}",
        "",
        "&6&l=== Server Stats ===",
        "",
        "&7» &ePlayers: &a{papi:server_online}&7/&a{papi:server_max_players}",
        "&7» &eTPS: &a{papi:server_tps}",
        "&7» &eMemory: &a{papi:server_ram_used}&7/&a{papi:server_ram_total} MB",
        "&7» &eUptime: &a{papi:server_uptime}"
      ]
    }
  }
}
```

## Additional Resources

- [Placeholder API Documentation](https://placeholderapi.com/docs)
- [NeoEssentials Placeholders](Placeholders)
- [Custom Templates](Custom-Templates)
- [NeoEssentials Discord](https://discord.gg/dUGAQF2Mga) for placeholder support
