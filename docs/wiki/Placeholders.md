# Placeholders

NeoEssentials includes a comprehensive placeholder system that allows you to include dynamic content in messages, templates, and animations. This guide explains all available placeholders and how to use them effectively.

## Basic Usage

Placeholders are inserted into text using curly braces:

```
Hello, {player}! Your balance is {balance}.
```

## Core Placeholders

### Player Information

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `{player}` | Player's name | Steve |
| `{displayname}` | Player's display name (with prefix/suffix) | [VIP] Steve |
| `{uuid}` | Player's UUID | 550e8400-e29b-41d4-a716-446655440000 |
| `{health}` | Player's current health | 20/20 |
| `{health_scale}` | Player's health as numeric value | 20 |
| `{food}` | Player's food level | 20/20 |
| `{food_scale}` | Player's food level as numeric value | 20 |
| `{xp}` | Player's XP level | 30 |
| `{xp_progress}` | Player's progress to next XP level | 0.75 |
| `{gamemode}` | Player's game mode | Survival |
| `{ping}` | Player's ping in milliseconds | 45ms |
| `{address}` | Player's IP address | 192.168.1.1 |
| `{locale}` | Player's selected language | en_US |
| `{first_join}` | Player's first join date | 2025-06-01 |
| `{last_join}` | Player's last join date | 2025-06-25 |
| `{playtime}` | Player's total playtime | 10h 30m |
| `{playtime_seconds}` | Player's total playtime in seconds | 37800 |

### Location Information

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `{x}` | Player's X coordinate | 123 |
| `{y}` | Player's Y coordinate | 64 |
| `{z}` | Player's Z coordinate | -456 |
| `{world}` | Player's current world | world |
| `{world_display}` | World's display name | Overworld |
| `{biome}` | Player's current biome | Plains |
| `{block}` | Block at player's position | Stone |
| `{dimension}` | Player's current dimension | minecraft:overworld |
| `{yaw}` | Player's yaw (rotation) | 180.0 |
| `{pitch}` | Player's pitch (looking up/down) | 0.0 |
| `{direction}` | Cardinal direction (N, E, S, W) | N |
| `{direction_full}` | Full direction name | North |

### Server Information

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `{server}` | Server name | MyServer |
| `{motd}` | Server MOTD | Welcome to MyServer! |
| `{version}` | Server version | NeoForge 1.21.1 |
| `{online_players}` | Number of online players | 42 |
| `{max_players}` | Maximum player slots | 100 |
| `{unique_joins}` | Total unique player joins | 1250 |
| `{uptime}` | Server uptime | 3d 4h 12m |
| `{tps}` | Server TPS (ticks per second) | 20.0 |
| `{tps_1m}` | Average TPS over 1 minute | 19.8 |
| `{tps_5m}` | Average TPS over 5 minutes | 19.9 |
| `{tps_15m}` | Average TPS over 15 minutes | 19.95 |
| `{memory_used}` | Used memory in MB | 2048MB |
| `{memory_max}` | Maximum memory in MB | 4096MB |
| `{memory_free}` | Free memory in MB | 2048MB |
| `{memory_used_pct}` | Used memory percentage | 50% |

### Date and Time

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `{time}` | Current server time | 15:30:45 |
| `{date}` | Current server date | 2025-06-25 |
| `{datetime}` | Current server date and time | 2025-06-25 15:30:45 |
| `{time:format}` | Custom formatted time | 3:30 PM |
| `{date:format}` | Custom formatted date | 06/25/2025 |
| `{datetime:format}` | Custom formatted date and time | 06/25/2025 3:30 PM |
| `{day}` | Current day of month | 25 |
| `{month}` | Current month | June |
| `{month_num}` | Current month as number | 6 |
| `{year}` | Current year | 2025 |
| `{weekday}` | Current day of week | Wednesday |

#### Custom Date Format Patterns

For `{time:format}`, `{date:format}`, and `{datetime:format}`, you can use Java's standard date format patterns:

- `HH:mm:ss` - 24-hour time with seconds (15:30:45)
- `hh:mm a` - 12-hour time with AM/PM (3:30 PM)
- `dd/MM/yyyy` - Day/month/year (25/06/2025)
- `MMMM d, yyyy` - Month day, year (June 25, 2025)
- `yyyy-MM-dd HH:mm:ss` - ISO date and time (2025-06-25 15:30:45)
- `EEEE` - Full day of week name (Wednesday)

### Economy

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `{balance}` | Player's current balance | 1500 |
| `{balance_formatted}` | Formatted balance with currency symbol | $1,500.00 |
| `{currency_symbol}` | Currency symbol | $ |
| `{currency_name}` | Currency name | Coins |
| `{currency_name_plural}` | Plural currency name | Coins |

### Homes and Warps

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `{homes_count}` | Number of player's homes | 5 |
| `{homes_max}` | Maximum homes player can set | 10 |
| `{homes_list}` | Comma-separated list of homes | home, mine, farm |
| `{warps_count}` | Number of available warps | 15 |
| `{warps_list}` | Comma-separated list of warps | spawn, shop, pvp |

### Kits

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `{kits_available}` | Number of available kits | 3 |
| `{kits_list}` | Comma-separated list of kits | starter, warrior, miner |
| `{kit_cooldown:name}` | Cooldown for specific kit | 2h 15m |

### Permissions and Ranks

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `{prefix}` | Player's prefix | [VIP] |
| `{suffix}` | Player's suffix | [Builder] |
| `{group}` | Player's primary group | VIP |
| `{groups}` | All player's groups | VIP, Builder, Donor |
| `{has_permission:perm}` | Checks if player has permission | true |
| `{has_group:group}` | Checks if player is in group | false |

## Advanced Placeholders

### Conditional Placeholders

Conditional placeholders change based on conditions:

```
{if:condition}True text{else}False text{/if}
```

Available conditions:
- `{if:permission:neoessentials.vip}VIP Content{else}Regular Content{/if}`
- `{if:group:admin}Admin Content{else}Regular Content{/if}`
- `{if:balance>1000}Rich!{else}Keep saving!{/if}`
- `{if:online:playername}Online{else}Offline{/if}`

### Mathematical Expressions

Perform calculations:

```
Your next rank costs {calc:1000-{balance}} more coins.
```

### Other Player Placeholders

Access other player's data:

```
{player:username:balance} - Shows username's balance
{player:username:online} - Checks if username is online
```

### Top Lists

Display server statistics:

```
Top player: {top:balance:1:name}
Top balance: {top:balance:1:value}
```

### Time Formatting

Format time durations:

```
{time_format:3600} → 1 hour
{time_format:90} → 1 minute 30 seconds
```

### Progress Bars

Create visual progress bars:

```
Health: {bar:health:10:❤:♡}
Balance: {bar:balance:1000:20:█:░}
```

## LuckPerms Integration

When LuckPerms is installed, these additional placeholders are available:

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `{luckperms_prefix}` | Player's LuckPerms prefix | [VIP] |
| `{luckperms_suffix}` | Player's LuckPerms suffix | [Builder] |
| `{luckperms_primary_group}` | Player's primary group | VIP |
| `{luckperms_groups}` | All player's groups | VIP, Builder, Donor |
| `{luckperms_meta:key}` | Player's meta value | 5 |
| `{luckperms_prefix_weight}` | Weight of player's prefix | 100 |

## Placeholder API Support

NeoEssentials integrates with Placeholder API, making all Placeholder API placeholders available in NeoEssentials:

```
{papi:placeholder_name}
```

For example:
```
{papi:essentials_nickname}
{papi:towny_town_name}
```

## Custom Placeholders

Server administrators can define custom placeholders in `config/neoessentials/placeholders.toml`:

```toml
[custom_placeholders]
website = "https://example.com"
discord = "https://discord.gg/dUGAQF2Mga"
store = "https://store.example.com"
ranks = [ "Warrior", "Knight", "Paladin", "Hero" ]
```

These can then be used as `{website}`, `{discord}`, `{store}`, and `{ranks}`.

## Placeholder Formatting

### Text Formatting

You can format placeholder output:

```
{balance:format:%.2f} → 1500.00
{displayname:uppercase} → STEVE
{world:lowercase} → world
{server:titlecase} → Myserver
```

### String Manipulation

Manipulate placeholder text:

```
{player:substring:0:3} → Ste (first 3 letters)
{player:replace:eve:ooo} → Stooo (replace "eve" with "ooo")
{motd:trim} → Removes whitespace
{motd:length} → Character count
```

## Global Variables

Set and use global variables:

```
{set:greeting}Hello there!{/set}
{get:greeting} → Hello there!
```

Variables persist throughout a player session.

## Performance Considerations

- Use placeholders efficiently to avoid excessive calculations
- Cache complex placeholder results when possible
- Avoid nesting too many conditional placeholders
- Use the performance settings in `config/neoessentials/config.toml`:

```toml
[performance]
cachePlaceholders = true
placeholderCacheTime = 30
disableComplexPlaceholders = false
```

## Troubleshooting

### Common Issues

- **Placeholder Not Working**: Verify the placeholder name and format
- **Unexpected Results**: Check for proper syntax and conditions
- **Permission-Based Issues**: Ensure proper permissions are set
- **Formatting Problems**: Check syntax for formatting modifiers

### Debug Mode

Enable placeholder debugging:

```
/neoessentials:debug placeholders on
```

This will log all placeholder processing to the console.

## Adding Custom Placeholders (For Developers)

Developers can register custom placeholders through the API:

```java
NeoEssentials.getPlaceholderManager().register("custom", player -> {
    return "Custom value for " + player.getName();
});
```

## Examples

### Welcome Message

```
{border}
{center}&e&lWelcome, &b{displayname}&e&l!{/center}

&7» &eBalance: &a{balance_formatted}
&7» &eRank: &a{group}
&7» &eOnline Players: &a{online_players}&7/&a{max_players}
&7» &eServer Time: &a{time:hh:mm a}
&7» &ePlaytime: &a{playtime}

{if:permission:neoessentials.vip}&a&lThank you for supporting our server!{/if}

{border}
```

### Tablist Header

```
{gradient:#ff0000:#ff7700:#ffff00}&l=== YOUR SERVER NAME ==={/gradient}
&eWelcome, &b{player}&e!
&7Today is &f{date:dd/MM/yyyy}&7, it's &f{time:HH:mm}&7 server time
&7Players online: &f{online_players}/{max_players} &7TPS: &f{tps}
```

### Economy Information

```
{border}
{center}&e&lECONOMY INFORMATION{/center}

&7» &eYour Balance: &a{balance_formatted}
&7» &eRichest Player: &a{top:balance:1:name} &7(&a{top:balance:1:value}&7)
&7» &eServer Economy: &a{top:balance:sum} {currency_name_plural}
&7» &eYour Rank: &a#{top:balance:index:{player}}

{if:balance>1000}&a&lYou are doing well financially!
{else}&c&lYou need to earn more {currency_name_plural}!{/if}

{border}
```

### Player Stats Card

```
{border}
{center}&b&l{player}'s Stats{/center}

&7» &eJoined: &f{first_join:dd/MM/yyyy}
&7» &ePlaytime: &f{playtime}
&7» &eBalance: &f{balance_formatted}

&7» &eHealth: &f{bar:health:10:❤:♡} &7(&f{health}&7)
&7» &eFood: &f{bar:food:10:🍗:⚪} &7(&f{food}&7)
&7» &eXP: &f{bar:xp_progress:10:⬛:⬜} &7Level &f{xp}&7)

{border}
```

## Additional Resources

- [Text Formatting Guide](Text-Formatting)
- [Custom Templates](Custom-Templates)
- [JSON Templates Guide](JSON-Templates-Guide)
- [Animation System](Animation-System)
- [NeoEssentials Discord](https://discord.gg/dUGAQF2Mga) for placeholder support
