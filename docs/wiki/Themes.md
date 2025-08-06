# Themes and Styling

NeoEssentials offers comprehensive theming and styling capabilities, allowing server administrators to create unique visual experiences that match their server's brand and aesthetic preferences.

## 🎨 Theme System Overview

### What are Themes?
Themes in NeoEssentials control the visual appearance of:
- **GUI Interfaces** - Colors, borders, backgrounds
- **Chat Messages** - Prefixes, colors, formatting
- **Notifications** - Styling for different notification types
- **Bossbar Displays** - Colors, styles, and animations
- **Tablist & Scoreboard** - Headers, formatting, colors

### Theme Architecture
The theme system uses a hierarchical structure:
1. **Base Theme** - Core color palette and styling rules
2. **Component Themes** - Specific styling for GUI components
3. **User Overrides** - Player-specific customizations
4. **Dynamic Themes** - Themes that change based on conditions

## 🖌️ Built-in Themes

### Default Theme
Clean, professional appearance suitable for most servers:

```json
{
  "name": "default",
  "displayName": "§fDefault Theme",
  "description": "Clean and professional appearance",
  "colors": {
    "primary": "§6",      // Gold
    "secondary": "§7",    // Gray
    "accent": "§b",       // Aqua
    "success": "§a",      // Green
    "warning": "§e",      // Yellow
    "error": "§c",        // Red
    "info": "§9"          // Blue
  },
  "gui": {
    "background": "minecraft:black_stained_glass_pane",
    "border": "minecraft:gray_stained_glass_pane",
    "button": "minecraft:stone_button",
    "decoration": "minecraft:gold_ingot"
  }
}
```

### Dark Theme
Sleek dark appearance for low-light environments:

```json
{
  "name": "dark",
  "displayName": "§8Dark Theme",
  "description": "Sleek dark mode for night owls",
  "colors": {
    "primary": "§8",      // Dark Gray
    "secondary": "§7",    // Gray
    "accent": "§b",       // Aqua
    "success": "§2",      // Dark Green
    "warning": "§6",      // Gold
    "error": "§4",        // Dark Red
    "info": "§1"          // Dark Blue
  },
  "gui": {
    "background": "minecraft:black_stained_glass_pane",
    "border": "minecraft:gray_stained_glass_pane",
    "button": "minecraft:blackstone_button",
    "decoration": "minecraft:iron_ingot"
  }
}
```

### Ocean Theme
Calming blue ocean-inspired theme:

```json
{
  "name": "ocean",
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
