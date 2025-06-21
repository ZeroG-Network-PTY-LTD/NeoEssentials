# NeoEssentials Tablist System

The NeoEssentials tablist feature provides an animated, customizable tablist with support for headers, footers, player sorting, ranks, and placeholders similar to popular plugins like BungeeTablistPlus and StatsScoreBoard.

## Features

- **Animated Headers and Footers**: Rotating messages that change at configurable intervals
- **Player Sorting**: Sort players by name, rank, or playtime
- **LuckPerms Integration**: Display player ranks, prefixes, and suffixes
- **Economy Integration**: Show player balances in the tablist
- **Player-specific Content**: Headers and footers can contain player-specific placeholders
- **Colorcode Support**: Full support for Minecraft color codes using the `&` symbol

## Configuration

The tablist configuration is stored in `neoessentials/tablist.json` and contains the following settings:

- `updateInterval`: How often to update the tablist (in milliseconds)
- `serverName`: The server name to use in placeholders
- `timeFormat`: Date/time format for the `%time%` placeholder
- `headers`: List of header templates that rotate
- `footers`: List of footer templates that rotate
- `enableSorting`: Whether to enable player sorting in the tablist
- `sortType`: How to sort players (`name`, `rank`, or `playtime`)
- `showEconomyInTablist`: Whether to show economy info in the tablist
- `enablePlayerSpecificHeaders`: Whether to replace player-specific placeholders in headers
- `enablePlayerSpecificFooters`: Whether to replace player-specific placeholders in footers

## Available Placeholders

### Server Placeholders
- `%server_name%`: Server name from config
- `%online_players%`: Current online player count
- `%max_players%`: Maximum player slots
- `%server_tps%`: Current server TPS (ticks per second)
- `%mc_version%`: Minecraft version
- `%mod_version%`: NeoEssentials version
- `%time%`: Current time formatted according to config
- `%economy_total%`: Total amount of currency in the economy
- `%economy_accounts%`: Number of economy accounts

### Player Placeholders
- `%player_name%`: Player's name
- `%player_displayname%`: Player's display name
- `%player_uuid%`: Player's UUID
- `%ping%`: Player's ping in milliseconds
- `%health%`: Player's current health
- `%max_health%`: Player's maximum health
- `%x%`: Player's X coordinate
- `%y%`: Player's Y coordinate
- `%z%`: Player's Z coordinate
- `%dimension%`: Player's current dimension
- `%balance%`: Player's economy balance

## Examples

### Header Examples
```
&6&lWelcome to &e&l%server_name%
&e&lPlayers Online: &a%online_players%&e/&a%max_players%
&b&lServer TPS: &a%server_tps%
&d&l%server_name% &f- &6The Best Minecraft Server
&6&l━━━━━━━━━━━━━━━━━━━━━━━
&e&l%server_name% &7- &fTime: &a%time%
```

### Footer Examples
```
&6&l━━━━━━━━━━━━━━━━━━━━━━━
&7&lWebsite: &b&nwww.example.com
&7&lDiscord: &b&ndiscord.gg/example
&7&lCurrent Time: &a%time% &7| &7&lOnline: &a%online_players% &7players
&e&lStore: &b&nstore.example.com &7- &6Support the server!
&d&lTPS: &a%server_tps% &7| &c&lPing: &a%ping%ms
```

## LuckPerms Integration

The tablist will automatically detect and use LuckPerms for:

1. **Prefix/Suffix Display**: Shows prefix and suffix from LuckPerms
2. **Player Sorting**: When using `rank` sorting type, sorts players by group weight
3. **Rank-based Formatting**: Applies different formatting based on player ranks

If LuckPerms is not available, it will fall back to using permissions level for basic admin/player differentiation.
