# NeoEssentials Tablist System (Enhanced)

> **Note:** This feature has been significantly enhanced in v1.0.1. For detailed documentation, see [TABLIST_DOCUMENTATION.md](TABLIST_DOCUMENTATION.md) and [TABLIST_CONFIGURATION_GUIDE.md](TABLIST_CONFIGURATION_GUIDE.md).

The NeoEssentials tablist feature provides an animated, customizable tablist with support for headers, footers, player sorting, ranks, and placeholders similar to popular plugins like BungeeTablistPlus and StatsScoreBoard.

## New in v1.0.1

- **Multiple Animation Types**: Now supports rotation, scrolling, fade, rainbow, typewriter, and blinking animations
- **Enhanced Placeholder System**: More dynamic content options and better performance
- **Improved Group Management**: Better player sorting and ranking capabilities
- **Group-specific Templates**: Different headers/footers for different permission groups
- **Color and Formatting Improvements**: Full support for all Minecraft formatting codes
- **Performance Optimizations**: More efficient animation processing

## Features

- **Animated Headers and Footers**: Multiple animation types to choose from
- **Player Sorting**: Sort players by name, rank, or playtime
- **LuckPerms Integration**: Display player ranks, prefixes, and suffixes
- **Economy Integration**: Show player balances in the tablist
- **Player-specific Content**: Headers and footers can contain player-specific placeholders
- **Colorcode Support**: Full support for Minecraft color codes using the `&` symbol

## Configuration

The tablist configuration is now stored in `config/neoessentials/tablist.toml` and contains the following main settings:

- `updateInterval`: How often to update the tablist (in milliseconds)
- `timeFormat`: Date/time format for the `%time%` placeholder
- `enableSorting`: Whether to enable player sorting in the tablist
- `sortType`: How to sort players (`name`, `rank`, or `playtime`)
- `enableAnimations`: Whether to enable animations in headers/footers
- `animationSpeed`: How fast animations should run
- `headerAnimationType`/`footerAnimationType`: Which animation style to use
- `scrollWidth`: How many characters to show in scroll animation
- `showEconomyInTablist`: Whether to show economy info in the tablist
- `enablePlayerSpecificHeaders`/`enablePlayerSpecificFooters`: Whether to use player-specific content

## Animation Types Available

- **None**: Static display (first line only)
- **Rotation**: Cycles through each line in sequence
- **Scroll**: Scrolls text horizontally
- **Fade**: Smooth transition between different lines
- **Rainbow**: Applies rainbow color effect to text
- **Typewriter**: Types out text character by character
- **Blink**: Text appears and disappears

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
