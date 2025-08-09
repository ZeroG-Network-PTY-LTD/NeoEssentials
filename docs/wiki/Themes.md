# Theme System

NeoEssentials provides a basic theme system that allows customization of tablist, scoreboard, and bossbar displays through predefined themes.

## 🎯 Overview

The theme system currently supports:
- **Tablist Themes** - Different visual styles for player lists
- **Scoreboard Themes** - Various layouts for scoreboard information
- **Bossbar Themes** - Styled bossbar templates with different colors and overlays

The theme system uses predefined themes that are built into the mod and cannot be customized through configuration files.

## 🎨 Available Themes

### Tablist Themes

The tablist system includes several predefined themes:

#### Available Themes:
- **default** - Standard theme with basic formatting
- **modern** - Contemporary styling with enhanced colors  
- **classic** - Traditional appearance with simple design
- **minimalist** - Clean, minimal styling

Each theme provides different header/footer formatting and color schemes for the player list display.

### Scoreboard Themes

Multiple scoreboard layouts for different information displays:

#### Available Themes:
- **serverinfo** - General server information (TPS, memory, player count)
- **playerstats** - Detailed player statistics (health, level, position)
- **economy** - Economy-related information (balance, ranks)

Each theme displays different types of information with themed formatting and colors.

### Bossbar Themes

Predefined bossbar templates with different visual styles:

#### Available Themes:
- **default** - Standard white progress bar
- **modern** - Blue with notched overlay and arrow formatting
- **classic** - Gold/yellow with decorative formatting
- **minimalist** - Simple white design

Each theme includes different colors, overlays, and text formatting for bossbar displays.

## 🎮 Commands

The theme system is managed through the `/theme` command:

### `/theme tablist <theme> [player]`
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

### `/theme scoreboard <theme> [player]`
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

### `/theme bossbar show <template> [duration] [player]`
Show a bossbar with the specified theme template.

**Examples**:
```bash
# Show default bossbar for 10 seconds
/theme bossbar show default

# Show modern bossbar for 30 seconds
/theme bossbar show modern 30

# Show bossbar for specific player
/theme bossbar show classic 15 Steve
```

**Permission**: `neoessentials.moderation.basic`

---

### `/theme bossbar hide [player]`
Hide the bossbar for yourself or another player.

**Examples**:
```bash
# Hide your bossbar
/theme bossbar hide

# Hide bossbar for specific player
/theme bossbar hide Steve
```

**Permission**: `neoessentials.moderation.basic`

---

### `/theme list [type]`
List available themes for different components.

**Examples**:
```bash
# List all available themes
/theme list

# List only tablist themes
/theme list tablist

# List only scoreboard themes
/theme list scoreboard

# List only bossbar templates
/theme list bossbar
```

**Permission**: `neoessentials.moderation.basic`

---

### `/theme reload`
Reload the theme system (admin command).

**Example**:
```bash
/theme reload
```

**Permission**: `neoessentials.admin.basic`

## ⚙️ Theme Implementation

### Tablist Themes

Tablist themes are implemented in the `TablistScoreboardManager` class and include:

#### Theme Properties:
- **Headers**: List of header texts that can cycle through animations
- **Footers**: List of footer texts for the bottom of the tablist
- **Name Formatting**: How player names appear in the list
- **Color Schemes**: Predefined color combinations

#### Features:
- Real-time placeholder replacement
- Animation support through text cycling
- Player-specific theme assignments
- Automatic updates at configured intervals

### Scoreboard Themes

Scoreboard themes provide different information layouts:

#### Theme Structure:
- **Title**: Scoreboard title with theme-specific formatting
- **Lines**: Up to 15 lines of content with placeholders
- **Colors**: Theme-specific color coding
- **Update Intervals**: Automatic refresh of dynamic content

#### Content Types:
- Server statistics (TPS, memory usage, player count)
- Player information (health, level, position, balance)
- Session data (playtime, experience)

### Bossbar Themes

Bossbar themes control the appearance of temporary message displays:

#### Theme Components:
- **Title Format**: Main bossbar text formatting
- **Subtitle Format**: Secondary text formatting (if supported)
- **Bar Color**: Progress bar color (WHITE, BLUE, YELLOW, etc.)
- **Bar Overlay**: Progress bar style (PROGRESS, NOTCHED_6, NOTCHED_10, etc.)

#### Usage:
- Announcements and notifications
- Event notifications
- Server status messages
- Player-specific information

## 🔧 Technical Details

### Theme Storage

Themes are stored in memory as predefined objects:

#### TablistTheme Class:
- Theme name and display properties
- Header and footer text lists
- Animation frame management
- Player assignment tracking

#### ScoreboardTheme Class:
- Theme identification
- Title and line content
- Placeholder processing
- Real-time data integration

#### BossbarTheme Class:
- Template name and formatting
- Color and overlay properties
- Duration and display settings

### Theme Application

#### Player Assignment:
- Themes are assigned per-player using UUID mapping
- Default themes applied to new players
- Theme preferences persist during session
- Automatic cleanup on player disconnect

#### Update System:
- Timer-based updates for dynamic content
- Placeholder replacement for real-time data
- Animation frame cycling for visual effects
- Performance optimization through caching

### Placeholder Support

The theme system supports various placeholders:

#### Player Information:
- `{player}` - Player display name
- `{health}` - Player health
- `{level}` - Player level
- `{balance}` - Player balance (if economy enabled)
- `{world}` - Current world

#### Server Information:
- `{server_players}` - Online player count
- `{server_tps}` - Server TPS
- `{server_memory_percent}` - Memory usage
- `{time}` - Current time
- `{uptime}` - Server uptime

## ⚠️ Limitations

### Current Constraints

#### Theme Customization:
- **Predefined Only**: Themes are hardcoded and cannot be modified
- **No Custom Themes**: Cannot create new themes through configuration
- **Limited Options**: Only the built-in themes are available
- **No File-based Configuration**: Themes cannot be edited in external files

#### Design Restrictions:
- **Basic Formatting**: Limited to Minecraft color codes
- **Static Structure**: Theme layouts cannot be modified
- **No Advanced Features**: No gradients, animations, or complex styling
- **Memory Storage**: Themes exist only in memory, not persistent

#### Functionality Limits:
- **Tablist/Scoreboard/Bossbar Only**: Limited to these three components
- **No GUI Theming**: Cannot style inventory GUIs or menus
- **No Chat Theming**: Chat messages use separate formatting
- **No Dynamic Switching**: No automatic theme changes based on conditions

### Recommendations

#### For Server Administrators:
- **Use Built-in Themes**: Work with the provided theme options
- **Test Combinations**: Try different theme combinations for different players
- **Permission Management**: Control theme access through permissions
- **Regular Updates**: Use the reload command when making system changes

#### For Future Development:
- **File-based Configuration**: Consider adding configurable themes
- **GUI Integration**: Expand theming to inventory interfaces
- **Custom Color Support**: Add hex color or RGB support
- **Dynamic Features**: Implement conditional theme switching
- **Theme Editor**: Create in-game theme customization tools

---

**Related Documentation**: [Tablist-Scoreboard](Tablist-Scoreboard.md) | [Bossbar](Bossbar.md) | [Configuration](Configuration.md)

*Last Updated: August 9, 2025*
  "displayName": "§b§lOcean Theme",
  "description": "Calming ocean-inspired colors",
  "colors": {
    "primary": "§3",      // Dark Aqua
    "secondary": "§b",    // Aqua
    "accent": "§9",       // Blue
    "success": "§a",      // Green
    "warning": "§e",      // Yellow
    "error": "§c",        // Red
    "info": "§b"          // Aqua
  },
  "gui": {
    "background": "minecraft:cyan_stained_glass_pane",
    "border": "minecraft:light_blue_stained_glass_pane",
    "button": "minecraft:prismarine",
    "decoration": "minecraft:heart_of_the_sea"
  }
}
```

### Nether Theme
Fiery red theme inspired by the Nether:

```json
{
  "name": "nether",
  "displayName": "§4§lNether Theme",
  "description": "Fiery theme inspired by the Nether",
  "colors": {
    "primary": "§4",      // Dark Red
    "secondary": "§c",    // Red
    "accent": "§6",       // Gold
    "success": "§a",      // Green
    "warning": "§e",      // Yellow
    "error": "§4",        // Dark Red
    "info": "§c"          // Red
  },
  "gui": {
    "background": "minecraft:red_stained_glass_pane",
    "border": "minecraft:magma_block",
    "button": "minecraft:nether_brick",
    "decoration": "minecraft:blaze_rod"
  }
}
```

## 🎯 Theme Management Commands

### Player Theme Commands
```bash
/theme                          # Show current theme
/theme list                     # List available themes
/theme set <name>               # Set your theme
/theme preview <name>           # Preview theme without applying
/theme reset                    # Reset to server default
/theme info <name>              # Show theme information
```

### Admin Theme Commands
```bash
/theme admin                    # Open theme admin panel
/theme create <name>            # Create new theme
/theme edit <name>              # Edit existing theme
/theme delete <name>            # Delete custom theme
/theme reload                   # Reload all themes
/theme export <name>            # Export theme file
/theme import <file>            # Import theme file
```

## 🛠️ Creating Custom Themes

### Theme File Structure
Themes are defined in JSON files located in `config/neoessentials/themes/`:

```json
{
  "name": "custom_theme",
  "displayName": "§d§lCustom Theme",
  "description": "My awesome custom theme",
  "author": "ServerAdmin",
  "version": "1.0.0",
  
  "colors": {
    "primary": "§5",
    "secondary": "§d",
    "accent": "§f",
    "success": "§a",
    "warning": "§e",
    "error": "§c",
    "info": "§b",
    "background": "§8",
    "text": "§f",
    "disabled": "§7"
  },
  
  "gui": {
    "background": "minecraft:purple_stained_glass_pane",
    "border": "minecraft:magenta_stained_glass_pane",
    "button": "minecraft:purpur_block",
    "decoration": "minecraft:amethyst_shard",
    "navigation": "minecraft:ender_pearl",
    "separator": "minecraft:end_rod"
  },
  
  "sounds": {
    "click": "ui.button.click",
    "success": "entity.experience_orb.pickup",
    "error": "entity.villager.no",
    "navigation": "block.note_block.chime"
  },
  
  "animations": {
    "enabled": true,
    "fadeIn": 10,
    "fadeOut": 10,
    "slideSpeed": 5
  }
}
```

### Advanced Theme Features

#### Conditional Styling
Apply different styles based on conditions:

```json
{
  "conditional": {
    "time_based": {
      "day": {
        "colors": {
          "primary": "§e",
          "background": "§f"
        }
      },
      "night": {
        "colors": {
          "primary": "§1",
          "background": "§8"
        }
      }
    },
    "permission_based": {
      "vip": {
        "gui": {
          "decoration": "minecraft:diamond"
        }
      },
      "admin": {
        "gui": {
          "decoration": "minecraft:nether_star"
        }
      }
    }
  }
}
```

#### Animated Elements
Add animations to theme elements:

```json
{
  "animations": {
    "enabled": true,
    "titleAnimation": {
      "type": "typewriter",
      "speed": 50,
      "repeat": false
    },
    "backgroundAnimation": {
      "type": "pulse",
      "speed": 2000,
      "colors": ["§8", "§7", "§8"]
    },
    "borderAnimation": {
      "type": "rotate",
      "items": [
        "minecraft:red_stained_glass_pane",
        "minecraft:orange_stained_glass_pane",
        "minecraft:yellow_stained_glass_pane"
      ],
      "interval": 1000
    }
  }
}
```

## 🎨 GUI Component Styling

### Button Styles
Customize button appearance and behavior:

```json
{
  "buttons": {
    "primary": {
      "item": "minecraft:diamond",
      "name": "{theme.colors.primary}Primary Button",
      "lore": [
        "{theme.colors.secondary}Click to perform primary action"
      ],
      "sound": "{theme.sounds.click}",
      "effects": {
        "hover": {
          "name": "{theme.colors.accent}Primary Button §l(Hover)"
        },
        "click": {
          "particle": "villager_happy"
        }
      }
    },
    "secondary": {
      "item": "minecraft:iron_ingot",
      "name": "{theme.colors.secondary}Secondary Button",
      "effects": {
        "disabled": {
          "item": "minecraft:gray_dye",
          "name": "{theme.colors.disabled}Disabled Button"
        }
      }
    }
  }
}
```

### Menu Layouts
Define consistent menu layouts:

```json
{
  "layouts": {
    "main_menu": {
      "size": 54,
      "title": "{theme.colors.primary}Main Menu",
      "pattern": [
        "BBBBBBBBB",
        "B-------B",
        "B-------B",
        "B-------B",
        "B-------B",
        "BBBBBBBBB"
      ],
      "items": {
        "B": "border",
        "-": "background"
      }
    },
    "shop_category": {
      "size": 45,
      "title": "{theme.colors.primary}Shop - {category}",
      "navigation": {
        "back": {"slot": 36, "style": "back_button"},
        "next": {"slot": 44, "style": "next_button"},
        "close": {"slot": 40, "style": "close_button"}
      }
    }
  }
}
```

## 📱 Chat and Message Styling

### Message Formatting
Customize message appearance:

```json
{
  "messages": {
    "prefix": "{theme.colors.primary}[{server_name}]{theme.colors.text}",
    "success": "{theme.colors.success}✓ {message}",
    "warning": "{theme.colors.warning}⚠ {message}",
    "error": "{theme.colors.error}✗ {message}",
    "info": "{theme.colors.info}ℹ {message}",
    
    "formats": {
      "command_success": [
        "{theme.colors.success}════════════════════════",
        "{theme.colors.success}✓ {title}",
        "{theme.colors.text}{description}",
        "{theme.colors.success}════════════════════════"
      ],
      "player_join": "{theme.colors.accent}» {theme.colors.text}{player} {theme.colors.secondary}joined the server",
      "player_leave": "{theme.colors.accent}« {theme.colors.text}{player} {theme.colors.secondary}left the server"
    }
  }
}
```

### Notification Styling
Style different types of notifications:

```json
{
  "notifications": {
    "actionbar": {
      "format": "{theme.colors.primary}[{type}] {theme.colors.text}{message}",
      "colors": {
        "info": "{theme.colors.info}",
        "warning": "{theme.colors.warning}",
        "error": "{theme.colors.error}"
      }
    },
    "title": {
      "format": {
        "title": "{theme.colors.primary}{title}",
        "subtitle": "{theme.colors.secondary}{subtitle}"
      },
      "timing": {
        "fadeIn": 20,
        "stay": 60,
        "fadeOut": 20
      }
    },
    "bossbar": {
      "format": "{theme.colors.primary}[{server_name}] {theme.colors.text}{message}",
      "colors": {
        "info": "BLUE",
        "warning": "YELLOW",
        "error": "RED",
        "success": "GREEN"
      }
    }
  }
}
```

## 🎭 Dynamic Theming

### Time-based Themes
Automatically change themes based on time:

```json
{
  "dynamic": {
    "time_based": {
      "enabled": true,
      "schedules": [
        {
          "time": "06:00-18:00",
          "theme": "day_theme",
          "transition": "smooth"
        },
        {
          "time": "18:00-06:00", 
          "theme": "night_theme",
          "transition": "smooth"
        }
      ]
    }
  }
}
```

### Event-based Themes
Change themes during special events:

```json
{
  "dynamic": {
    "event_based": {
      "enabled": true,
      "events": {
        "halloween": {
          "start": "10-01",
          "end": "11-01", 
          "theme": "halloween_theme"
        },
        "christmas": {
          "start": "12-15",
          "end": "01-05",
          "theme": "christmas_theme"
        },
        "server_birthday": {
          "start": "06-01",
          "end": "06-07",
          "theme": "celebration_theme"
        }
      }
    }
  }
}
```

### Mood-based Themes
Change themes based on server mood/activity:

```json
{
  "dynamic": {
    "mood_based": {
      "enabled": true,
      "triggers": {
        "high_activity": {
          "condition": "player_count > 50",
          "theme": "energetic_theme"
        },
        "peaceful": {
          "condition": "player_count < 10 && time_night",
          "theme": "calm_theme"
        },
        "pvp_active": {
          "condition": "pvp_deaths > 5_per_hour",
          "theme": "combat_theme"
        }
      }
    }
  }
}
```

## 🎨 Theme Configuration

### Global Theme Settings
Configure theme system behavior:

```toml
[themes]
# Enable theme system
enabled = true

# Default server theme
defaultTheme = "default"

# Allow players to change themes
allowPlayerThemes = true

# Theme cache settings
cacheThemes = true
cacheSize = 50
cacheTTL = 3600

# Theme update settings
autoReload = true
reloadOnChange = true

[themes.permissions]
# Permission-based theme access
requirePermission = false
premiumThemes = ["nether", "ocean", "custom"]
vipThemes = ["dark", "ocean"]
```

### Player Theme Preferences
Players can save theme preferences:

```json
{
  "player_preferences": {
    "uuid": "player-uuid-here",
    "current_theme": "dark",
    "favorite_themes": ["dark", "ocean", "nether"],
    "auto_switch": {
      "enabled": true,
      "day_theme": "default",
      "night_theme": "dark"
    },
    "customizations": {
      "colors": {
        "primary": "§d"
      },
      "gui": {
        "decoration": "minecraft:emerald"
      }
    }
  }
}
```

## 🛠️ Theme Development Tools

### Theme Editor
Built-in theme editor for easy customization:

```bash
/theme editor                   # Open theme editor GUI
/theme editor <name>            # Edit specific theme
/theme preview live             # Live preview while editing
/theme validate <name>          # Validate theme syntax
```

### Theme Testing
Test themes thoroughly before deployment:

```bash
/theme test <name>              # Test theme functionality
/theme test colors              # Test color combinations
/theme test gui                 # Test GUI components
/theme test animations          # Test animations
```

### Theme Sharing
Share themes with the community:

```bash
/theme export <name> <format>   # Export theme (json, zip)
/theme import <file>            # Import theme file
/theme publish <name>           # Publish to theme repository
/theme download <id>            # Download from repository
```

## 🎨 Advanced Styling Features

### Hex Color Support
Use hex colors for precise color control:

```json
{
  "colors": {
    "primary": "#FF6B35",     // Orange
    "secondary": "#004E89",   // Blue
    "accent": "#1A936F",      // Green
    "gradient_start": "#FF6B35",
    "gradient_end": "#F29E38"
  }
}
```

### CSS-like Styling
Advanced styling with CSS-like properties:

```json
{
  "styles": {
    "button": {
      "background": "linear-gradient(#FF6B35, #F29E38)",
      "border": "2px solid #004E89",
      "border-radius": "4px",
      "padding": "8px 16px",
      "font-weight": "bold",
      "text-shadow": "1px 1px 2px #000000"
    },
    "container": {
      "background": "rgba(0, 0, 0, 0.8)",
      "backdrop-filter": "blur(5px)",
      "box-shadow": "0 4px 8px rgba(0, 0, 0, 0.3)"
    }
  }
}
```

### Responsive Design
Themes that adapt to different screen sizes:

```json
{
  "responsive": {
    "enabled": true,
    "breakpoints": {
      "small": {
        "condition": "gui_scale < 2",
        "modifications": {
          "font_size": "small",
          "padding": "reduced"
        }
      },
      "large": {
        "condition": "gui_scale > 3",
        "modifications": {
          "font_size": "large",
          "spacing": "increased"
        }
      }
    }
  }
}
```

## 🔧 Troubleshooting Themes

### Common Theme Issues

#### Theme Not Loading
- Check theme file syntax (valid JSON)
- Verify theme is in correct directory
- Check console for error messages
- Validate theme with `/theme validate`

#### Colors Not Displaying
- Ensure proper color format (§ codes or hex)
- Check client compatibility
- Verify resource pack conflicts
- Test with different Minecraft versions

#### GUI Elements Missing
- Check item references are valid
- Verify slot assignments are correct
- Test with different GUI sizes
- Check for mod conflicts

### Debug Commands
```bash
/theme debug <name>             # Debug specific theme
/theme validate all             # Validate all themes
/theme reload force             # Force reload themes
/theme cache clear              # Clear theme cache
```

---

## 📚 Related Documentation

- **[GUI System](GUI-System.md)** - GUI customization and configuration
- **[Configuration](Configuration.md)** - Theme system configuration
- **[Hex Colors](Hex-Colors.md)** - Complete hex color reference
- **[Placeholders](Placeholders.md)** - Using placeholders in themes

*Last Updated: August 6, 2025*
