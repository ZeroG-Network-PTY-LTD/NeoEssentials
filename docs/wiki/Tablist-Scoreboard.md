# Tablist & Scoreboard System

NeoEssentials provides a theme-based tablist and scoreboard system that allows customization of player list displays and scoreboards with multiple themes and real-time updates.

## 🎯 Overview

The tablist and scoreboard system allows administrators to:
- Apply different tablist and scoreboard themes to players
- Display dynamic content with real-time server information
- Use predefined themes with consistent styling
- Show player-specific information and server statistics
- Support color formatting and basic placeholders

## 🎨 Tablist Features

### Theme System
The system includes predefined themes that provide different visual styles for the tablist:

#### Available Themes
- **default** - Standard theme with basic formatting
- **modern** - Contemporary styling with enhanced colors
- **classic** - Traditional appearance with simple design
- **minimalist** - Clean, minimal styling

### Tablist Display
- **Custom Headers**: Configurable top display with server information
- **Custom Footers**: Bottom display with helpful information
- **Player Name Formatting**: Theme-based player name styling
- **Real-time Updates**: Live server statistics and player information

### Dynamic Content
The tablist supports dynamic placeholders that update automatically:
- Server player count
- Server TPS (Ticks Per Second)
- Server memory usage
- Player-specific information
- Current time and server uptime

## 🏆 Scoreboard Features

### Scoreboard Themes
Multiple scoreboard layouts for different purposes:

#### Available Themes
- **serverinfo** - General server information display
- **playerstats** - Player-specific statistics
- **economy** - Economy-related information (if economy is enabled)

### Scoreboard Display
- **Dynamic Titles**: Themed scoreboard titles
- **Multiple Lines**: Up to 15 lines of configurable content
- **Real-time Updates**: Live data refresh
- **Player-specific Content**: Personalized information for each player

### Scoreboard Content
The scoreboard can display:
- Player health, food, and level
- Server statistics (TPS, memory, player count)
- Player position and world information
- Economy balance (if economy system is enabled)
- Session time and statistics

## 🎮 Commands

### Theme Commands

The main command for managing themes is `/theme`:

#### `/theme tablist <theme> [player]`
Apply a tablist theme to yourself or another player.

**Examples**:
```bash
# Apply modern theme to yourself
/theme tablist modern

# Apply classic theme to specific player
/theme tablist classic Steve
```

**Permission**: `neoessentials.moderation.basic`

---

#### `/theme scoreboard <theme> [player]`
Apply a scoreboard theme to yourself or another player.

**Examples**:
```bash
# Apply server info theme to yourself
/theme scoreboard serverinfo

# Apply player stats theme to specific player
/theme scoreboard playerstats Steve
```

**Permission**: `neoessentials.moderation.basic`

---

#### `/theme list [type]`
List available themes and templates.

**Examples**:
```bash
# List all available themes
/theme list

# List only tablist themes
/theme list tablist

# List only scoreboard themes
/theme list scoreboard
```

**Permission**: `neoessentials.moderation.basic`

---

#### `/theme reload`
Reload the theme system configuration.

**Example**:
```bash
/theme reload
```

**Permission**: `neoessentials.admin.basic`

### Bossbar Commands

The theme system also includes bossbar management:

#### `/theme bossbar show <template> [duration] [player]`
Show a bossbar with the specified template.

**Examples**:
```bash
# Show default bossbar (10 seconds)
/theme bossbar show welcome

# Show bossbar for 30 seconds
/theme bossbar show announcement 30

# Show bossbar for specific player
/theme bossbar show info 15 Steve
```

#### `/theme bossbar hide [player]`
Hide the bossbar for yourself or another player.

**Examples**:
```bash
# Hide your bossbar
/theme bossbar hide

# Hide bossbar for specific player
/theme bossbar hide Steve
```

## ⚙️ Placeholders

### Supported Placeholders

The system supports various placeholders for dynamic content:

#### Player Information
- `{player}` - Player display name
- `{player_name}` - Player username
- `{health}` - Player health
- `{max_health}` - Player maximum health
- `{food}` - Player food level
- `{level}` - Player level
- `{exp}` - Player experience percentage
- `{ping}` - Player ping in milliseconds
- `{world}` - Player current world
- `{player_x}` - Player X coordinate
- `{player_y}` - Player Y coordinate
- `{player_z}` - Player Z coordinate

#### Server Information
- `{server_players}` - Current online player count
- `{server_max_players}` - Maximum server player capacity
- `{server_tps}` - Server TPS (Ticks Per Second)
- `{server_memory_percent}` - Memory usage percentage
- `{server_name}` - Server name
- `{time}` - Current time
- `{uptime}` - Server uptime

#### Economy Information (if enabled)
- `{balance}` - Player balance
- `{player_rank}` - Player rank (if available)

#### Session Information
- `{session_time}` - Current session playtime

## 🎨 Theme Configuration

### Theme Structure

Tablist and scoreboard themes are defined programmatically with the following structure:

#### Tablist Theme Properties
- **Theme Name**: Unique identifier for the theme
- **Headers**: List of header texts that can cycle
- **Footers**: List of footer texts that can cycle
- **Name Format**: How player names appear in the tablist

#### Scoreboard Theme Properties
- **Theme Name**: Unique identifier for the theme
- **Title**: Scoreboard title text
- **Lines**: List of content lines (up to 15 lines)

### Default Themes

The system comes with several predefined themes:

#### Tablist Themes
1. **default** - Basic white and gray formatting
2. **modern** - Blue and cyan color scheme
3. **classic** - Gold and yellow traditional styling
4. **minimalist** - Simple white and gray design

#### Scoreboard Themes
1. **serverinfo** - General server information display
2. **playerstats** - Detailed player statistics
3. **economy** - Economy-focused information

## 🔧 Configuration

### Animation Settings

The system includes animation support for dynamic content:
- **Update Interval**: How often content refreshes (configurable)
- **Animation Frames**: Cycling through different header/footer texts
- **Real-time Data**: Live updates of server statistics

### Performance Settings

- **Update Timer**: Manages the refresh rate for dynamic content
- **Cache Management**: Efficient handling of player data
- **Memory Usage**: Optimized for server performance

## 📊 Usage Examples

### Basic Theme Application
```bash
# Set up server information display
/theme tablist modern
/theme scoreboard serverinfo

# Apply player stats for detailed information
/theme scoreboard playerstats
```

### Administrative Usage
```bash
# Apply specific themes to players
/theme tablist classic Steve
/theme scoreboard economy VIP_Player

# List available options
/theme list tablist
/theme list scoreboard
```

### Event Management
```bash
# Show announcement bossbar to all online players
/theme bossbar show announcement 60 @a

# Hide all bossbars
/theme bossbar hideall @a
```

## 🛠️ Technical Details

### Implementation

The tablist and scoreboard system is implemented through:

#### TablistScoreboardManager
- **Singleton Pattern**: Single instance manages all functionality
- **Event Handling**: Responds to player join/leave events
- **Theme Management**: Stores and applies different themes
- **Update System**: Regular updates for dynamic content

#### Theme Storage
- **ConcurrentHashMap**: Thread-safe storage for themes
- **Player Preferences**: Per-player theme assignments
- **Default Themes**: Built-in themes loaded at startup

#### Update Mechanism
- **Timer-based Updates**: Regular refresh of dynamic content
- **Player-specific Updates**: Individual player display management
- **Server Statistics**: Real-time server data integration

### Performance Considerations

- **Efficient Updates**: Only updates when necessary
- **Memory Management**: Proper cleanup of player data
- **Thread Safety**: Concurrent access handling
- **Animation Management**: Controlled animation frame cycling

## ⚠️ Limitations

### Current Limitations

#### Theme System
- **Predefined Themes**: Limited to built-in themes (no custom theme creation)
- **Static Configuration**: Themes are hardcoded, not configurable via files
- **Limited Customization**: Cannot modify theme properties without code changes

#### Display Constraints
- **Minecraft Limits**: Scoreboard limited to 15 lines maximum
- **Color Support**: Limited to Minecraft color codes (no RGB support)
- **Animation Complexity**: Simple text cycling only

#### Commands
- **Basic Functionality**: Limited to theme switching and listing
- **No Advanced Features**: No custom header/footer commands
- **Permission Requirements**: Requires moderation permissions for basic usage

### Recommendations

#### For Server Administrators
- **Use Built-in Themes**: Stick to the provided themes for consistency
- **Regular Updates**: Use the timer system for real-time information
- **Permission Management**: Ensure proper permissions for theme commands

#### For Development
- **Theme Expansion**: Consider adding more predefined themes
- **Configuration Files**: Future enhancement could include file-based theme configuration
- **Advanced Placeholders**: Additional placeholder support could be beneficial

---

**Related Documentation**: [Essential Commands](Essential-Commands.md) | [Bossbar](Bossbar.md) | [Themes](Themes.md)

*Last Updated: August 9, 2025*
    - ""
    - "§fYour Stats:"
    - "§fHealth: §c{player_health}"
    - "§fLevel: §e{player_level}"
    - ""
    - "§7{server_name}"
```

#### Stats Scoreboard Theme
```yaml
stats:
  title: "§e§lPlayer Stats"
  lines:
    - "§fPlayer: §a{player_name}"
    - "§fWorld: §a{player_world}"
    - "§fPosition:"
    - "  §7X: §f{player_x}"
    - "  §7Y: §f{player_y}" 
    - "  §7Z: §f{player_z}"
    - ""
    - "§fHealth: §c{player_health}§f/§c{player_max_health}"
    - "§fHunger: §6{player_food}"
    - ""
    - "§7Updated: §f{time}"
```

#### Economy Scoreboard Theme
```yaml
economy:
  title: "§6§lEconomy"
  lines:
    - "§fBalance: §a${player_balance}"
    - "§fRank: §e{player_rank}"
    - ""
    - "§fServer Economy:"
    - "§fTotal: §a${server_total_money}"
    - "§fRichest: §e{richest_player}"
    - ""
    - "§fDaily Bonus:"
    - "§f{daily_bonus_status}"
    - ""
    - "§7{server_name}"
```

### Scoreboard Features
- **Dynamic Titles**: Animated and themed scoreboard titles
- **Customizable Lines**: Up to 15 lines of configurable content
- **Real-time Updates**: Live data refresh with configurable intervals
- **Theme Consistency**: Coordinated styling with tablist themes
- **Conditional Display**: Show/hide based on player permissions or status

## 🎮 Commands

### Tablist Commands

#### `/tablist theme <theme> [player]`
Apply a specific theme to a player's tablist display.

**Examples**:
```bash
# Apply modern theme to yourself
/tablist theme modern

# Apply classic theme to specific player
/tablist theme classic Steve

# Apply minimalist theme to all online players
/tablist theme minimalist @a
```

**Permissions**: `neoessentials.tablist.theme`, `neoessentials.tablist.theme.others`

---

#### `/tablist header <text> [player]`
Set custom header text for tablist display.

**Examples**:
```bash
# Set header for yourself
/tablist header "§6§lWelcome to Our Server!"

# Set header for specific player
/tablist header "§b§lVIP Area" Steve

# Set header with placeholders
/tablist header "§a{server_players} players online"
```

**Permission**: `neoessentials.tablist.header`

---

#### `/tablist footer <text> [player]`
Set custom footer text for tablist display.

**Examples**:
```bash
# Set footer with server info
/tablist footer "§7Website: §fmyserver.com"

# Set footer with dynamic content
/tablist footer "§7TPS: §a{server_tps} §7| Memory: §a{server_memory_percent}%"
```

**Permission**: `neoessentials.tablist.footer`

---

#### `/tablist refresh [player]`
Force refresh tablist display for better synchronization.

**Examples**:
```bash
# Refresh your own tablist
/tablist refresh

# Refresh specific player's tablist
/tablist refresh Steve

# Refresh all players' tablists
/tablist refresh @a
```

**Permission**: `neoessentials.tablist.refresh`

---

### Scoreboard Commands

#### `/scoreboard theme <theme> [player]`
Apply a specific scoreboard theme to a player.

**Examples**:
```bash
# Show info scoreboard
/scoreboard theme info

# Show stats scoreboard to specific player
/scoreboard theme stats Steve

# Show economy scoreboard
/scoreboard theme economy
```

**Permissions**: `neoessentials.scoreboard.theme`, `neoessentials.scoreboard.theme.others`

---

#### `/scoreboard show [theme] [player]`
Display scoreboard with optional theme to a player.

**Examples**:
```bash
# Show default scoreboard
/scoreboard show

# Show specific theme
/scoreboard show stats

# Show to specific player
/scoreboard show info Steve
```

**Permission**: `neoessentials.scoreboard.show`

---

#### `/scoreboard hide [player]`
Hide scoreboard display from a player.

**Examples**:
```bash
# Hide your scoreboard
/scoreboard hide

# Hide specific player's scoreboard
/scoreboard hide Steve

# Hide all players' scoreboards
/scoreboard hide @a
```

**Permission**: `neoessentials.scoreboard.hide`

---

#### `/scoreboard update [player]`
Force update scoreboard content for real-time data refresh.

**Examples**:
```bash
# Update your scoreboard
/scoreboard update

# Update specific player's scoreboard
/scoreboard update Steve
```

**Permission**: `neoessentials.scoreboard.update`

---

### Combined Commands

#### `/display theme <theme> [player]`
Apply the same theme to both tablist and scoreboard simultaneously.

**Examples**:
```bash
# Apply modern theme to both displays
/display theme modern

# Apply classic theme to specific player
/display theme classic Steve
```

**Permission**: `neoessentials.display.theme`

---

#### `/display reset [player]`
Reset both tablist and scoreboard to default settings.

**Examples**:
```bash
# Reset your displays
/display reset

# Reset specific player's displays
/display reset Steve
```

**Permission**: `neoessentials.display.reset`

## 🎨 Advanced Customization

### Theme Configuration

Create custom themes in `config/neoessentials/themes/tablist.yml`:

```yaml
custom_theme:
  name: "Custom Theme"
  description: "My custom tablist theme"
  
  tablist:
    header_format: "§d§l✦ {title} ✦"
    footer_format: "§f{subtitle}"
    name_format: "§d{player}"
    colors: ["§d", "§5", "§f"]
    update_interval: 5
  
  scoreboard:
    title: "§d§lCustom Board"
    lines:
      - "§fCustom Line 1"
      - "§fPlayer: §d{player_name}"
      - "§fTime: §d{time}"
      - ""
      - "§7Custom Footer"
    update_interval: 3
```

### Animation Configuration

Configure animated elements in `config/neoessentials/animations.yml`:

```yaml
animations:
  header_cycle:
    enabled: true
    frames:
      - "§6§lWelcome to Our Server!"
      - "§e§lEnjoy Your Stay!"
      - "§a§lHave Fun Gaming!"
    interval: 3  # seconds between frames
  
  color_cycle:
    enabled: true
    colors: ["§a", "§b", "§c", "§d", "§e", "§f"]
    interval: 1
```

### Advanced Placeholders

The system supports all standard placeholders plus enhanced ones:

#### Tablist-Specific Placeholders
- `{tablist_theme}` - Current tablist theme name
- `{player_ping_color}` - Color-coded ping display
- `{player_rank_prefix}` - Player rank prefix
- `{player_rank_suffix}` - Player rank suffix

#### Scoreboard-Specific Placeholders
- `{scoreboard_theme}` - Current scoreboard theme
- `{scoreboard_line_count}` - Number of lines displayed
- `{player_score}` - Player's objective score
- `{player_team}` - Player's team name

#### Animation Placeholders
- `{animated_header}` - Cycling header text
- `{animated_footer}` - Cycling footer text
- `{color_cycle}` - Cycling color codes
- `{rainbow_text}` - Rainbow-colored text effect

## ⚙️ Configuration

### Global Settings

```toml
[tablist]
# Enable tablist system
enabled = true

# Default theme for new players
defaultTheme = "default"

# Update interval for dynamic content (seconds)
updateInterval = 5

# Enable multi-theme support
multiTheme = true

# Maximum themes per player
maxThemesPerPlayer = 3

[scoreboard]
# Enable scoreboard system
enabled = true

# Default scoreboard theme
defaultTheme = "info"

# Update interval (seconds)
updateInterval = 3

# Enable animations
enableAnimations = true

# Maximum scoreboard lines
maxLines = 15
```

### Theme-Specific Settings

```toml
[tablist.themes.modern]
enabled = true
permission = "neoessentials.theme.modern"
defaultHeader = "§b§lModern Server"
defaultFooter = "§fEnjoy your stay!"

[scoreboard.themes.stats]
enabled = true
permission = "neoessentials.theme.stats"
autoShow = true  # Show automatically to players with permission
updateInterval = 2
```

### Performance Settings

```toml
[display.performance]
# Cache theme data (seconds)
themeCacheTime = 300

# Async updates for better performance
asyncUpdates = true

# Batch updates for multiple players
batchUpdates = true

# Maximum concurrent updates
maxConcurrentUpdates = 10
```

## 🛡️ Permissions

### Tablist Permissions
- `neoessentials.tablist.*` - All tablist permissions
- `neoessentials.tablist.theme` - Change own tablist theme
- `neoessentials.tablist.theme.others` - Change others' tablist themes
- `neoessentials.tablist.header` - Modify tablist header
- `neoessentials.tablist.footer` - Modify tablist footer
- `neoessentials.tablist.refresh` - Refresh tablist display

### Scoreboard Permissions
- `neoessentials.scoreboard.*` - All scoreboard permissions
- `neoessentials.scoreboard.theme` - Change own scoreboard theme
- `neoessentials.scoreboard.theme.others` - Change others' scoreboard themes
- `neoessentials.scoreboard.show` - Show scoreboard
- `neoessentials.scoreboard.hide` - Hide scoreboard
- `neoessentials.scoreboard.update` - Update scoreboard content

### Theme-Specific Permissions
- `neoessentials.theme.default` - Use default theme
- `neoessentials.theme.modern` - Use modern theme
- `neoessentials.theme.classic` - Use classic theme
- `neoessentials.theme.minimalist` - Use minimalist theme
- `neoessentials.theme.custom.*` - Use custom themes

### Display Management Permissions
- `neoessentials.display.*` - All display permissions
- `neoessentials.display.theme` - Change display themes
- `neoessentials.display.reset` - Reset displays
- `neoessentials.display.admin` - Administrative functions

## 🔍 Troubleshooting

### Common Issues

#### Tablist Not Updating
1. Check if tablist system is enabled in configuration
2. Verify player has appropriate permissions for theme
3. Ensure update interval is not too high
4. Use `/tablist refresh` to force update

#### Scoreboard Display Issues
1. Verify scoreboard is enabled in config
2. Check if player has scoreboard theme permissions
3. Ensure scoreboard lines don't exceed maximum (15)
4. Test with `/scoreboard update` command

#### Theme Not Applying
1. Confirm theme exists and is enabled
2. Check player has permission for specific theme
3. Verify theme configuration syntax
4. Test with different theme to isolate issue

#### Performance Problems
1. Increase update intervals for better performance
2. Enable async updates in configuration
3. Reduce number of active themes per player
4. Monitor server TPS during peak usage

### Debug Commands

```bash
# Check tablist system status
/neoessentials debug tablist

# Test scoreboard system
/neoessentials debug scoreboard

# Validate theme configuration
/display debug themes

# Monitor performance
/display debug performance
```

## 📊 Usage Examples

### Server Information Display
```bash
# Set up server info tablist
/tablist theme info
/tablist header "§6§lMyServer §7- §a{server_players} players"
/tablist footer "§7TPS: §a{server_tps} §7| Website: §bmyserver.com"

# Add matching scoreboard
/scoreboard theme info
```

### VIP Player Experience
```bash
# Apply special theme for VIP players
/tablist theme modern Steve
/scoreboard theme economy Steve

# Custom VIP header
/tablist header "§6§l★ VIP ★ §7{player_name}" Steve
```

### Event Announcement
```bash
# Apply event theme to all players
/display theme classic @a

# Set event header
/tablist header "§c§lPvP Tournament §7- §eJoin now!" @a

# Show event scoreboard
/scoreboard theme event @a
```

## 🎨 Best Practices

### Design Guidelines
1. **Consistent Theming** - Use coordinated colors across tablist and scoreboard
2. **Readable Text** - Ensure good contrast and appropriate font weights
3. **Appropriate Information** - Show relevant data without overwhelming players
4. **Performance Aware** - Balance update frequency with server performance

### Content Strategy
1. **Informative Headers** - Use headers for important server information
2. **Helpful Footers** - Include useful links or tips in footers
3. **Relevant Scoreboards** - Show data that players actually need
4. **Consistent Updates** - Keep information fresh and accurate

### Technical Optimization
1. **Efficient Updates** - Use appropriate intervals for different content types
2. **Caching Strategy** - Cache static data and refresh dynamic data
3. **Permission Management** - Use specific permissions for theme access
4. **Monitoring** - Regular performance monitoring during peak times

---

**Related Documentation**: [Bossbar System](Bossbar.md) | [Placeholders](Placeholders.md) | [Configuration](Configuration.md)

*Last Updated: August 5, 2025*
