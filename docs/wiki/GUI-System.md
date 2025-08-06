# GUI System Documentation

Welcome to the comprehensive GUI System documentation for NeoEssentials. Our advanced config-based GUI system provides highly customizable user interfaces for all mod features.

## 🎨 Overview

The NeoEssentials GUI system is a revolutionary approach to Minecraft mod interfaces, featuring:

- **Complete JSON-based customization** - Every GUI element configurable through files
- **Theme system** - Multiple visual themes with player preferences
- **Live data integration** - Real-time server and player data in GUIs
- **Advanced action system** - Complex interactions and workflows
- **Permission-based access** - Role-specific interface customization
- **Hot-reloading** - Configuration changes without server restart

## 📁 GUI Configuration Structure

### Configuration Directory
```
config/gui/
├── main_config.json          # Global GUI settings and themes
├── shop_gui.json             # Shop system interface
├── stats_gui.json            # Player statistics display
├── economy_gui.json          # Economy management panel
├── kits_gui.json             # Kit selection and management
├── warps_gui.json            # Warp destinations interface
├── admin_gui.json            # Administrative control panel
├── teleport_gui.json         # Teleportation hub
├── themes/                   # Custom theme definitions
│   ├── dark_theme.json
│   ├── ocean_theme.json
│   └── custom_theme.json
├── languages/                # GUI text translations
│   ├── en_US.json
│   ├── es_ES.json
│   └── fr_FR.json
└── player_preferences.json   # Player theme and settings
```

## 🎮 Available GUI Types

### 1. Shop GUI (`shop_gui.json`)
**Purpose**: Complete shop interface with buy/sell functionality
**Features**:
- Category-based item organization
- Dynamic pricing display
- Purchase confirmation dialogs
- Sell interface with quantity selection
- Search and filtering capabilities

**Access Command**: `/shop` or `/gui shop`

### 2. Player Statistics (`stats_gui.json`)
**Purpose**: Comprehensive player data and statistics
**Features**:
- Player achievements and progress
- Server rankings and leaderboards
- Social features and friend lists
- Performance metrics
- Activity history

**Access Command**: `/stats` or `/gui stats`

### 3. Economy Management (`economy_gui.json`)
**Purpose**: Administrative economy control panel
**Features**:
- Real-time economic data
- Transaction monitoring
- Player balance management
- Security and audit tools
- Emergency controls

**Access Command**: `/economy` or `/gui economy` (Admin only)

### 4. Kit System (`kits_gui.json`)
**Purpose**: Kit selection and management interface
**Features**:
- Multiple kit categories (Starter, Survival, Premium, Elite)
- Cooldown tracking and display
- Kit creation tools for admins
- Favorite kits system
- Permission-based access

**Access Command**: `/kits` or `/gui kits`

### 5. Warp Destinations (`warps_gui.json`)
**Purpose**: Comprehensive warp management system
**Features**:
- Category-based warp organization
- Distance and coordinate display
- Quick access favorites
- Warp creation interface
- Recent warps tracking

**Access Command**: `/warps` or `/gui warps`

### 6. Admin Panel (`admin_gui.json`)
**Purpose**: Complete administrative control interface
**Features**:
- Player management tools
- Server monitoring and performance
- Bulk operations
- Security management
- Emergency controls

**Access Command**: `/admin` or `/gui admin` (Admin only)

### 7. Teleportation Hub (`teleport_gui.json`)
**Purpose**: Central teleportation management
**Features**:
- Player teleport requests (TPA)
- Coordinate-based teleportation
- Home and spawn quick access
- Teleport history
- Cross-dimensional travel

**Access Command**: `/tp` or `/gui teleport`

## 🎨 Theme System

### Built-in Themes

#### Default Theme
```json
{
  "name": "default",
  "display_name": "§f⚪ Default",
  "description": "Clean and professional default theme",
  "colors": {
    "primary": "§6",
    "secondary": "§f",
    "accent": "§e",
    "success": "§a",
    "warning": "§c",
    "info": "§b"
  },
  "borders": {
    "main": "minecraft:gray_stained_glass_pane",
    "accent": "minecraft:white_stained_glass_pane"
  }
}
```

#### Dark Theme
```json
{
  "name": "dark",
  "display_name": "§8⚫ Dark",
  "description": "Sleek dark theme for low-light environments",
  "colors": {
    "primary": "§7",
    "secondary": "§8",
    "accent": "§f",
    "success": "§a",
    "warning": "§c",
    "info": "§b"
  },
  "borders": {
    "main": "minecraft:black_stained_glass_pane",
    "accent": "minecraft:gray_stained_glass_pane"
  }
}
```

#### Ocean Theme
```json
{
  "name": "ocean",
  "display_name": "§9🌊 Ocean",
  "description": "Calming ocean-inspired theme",
  "colors": {
    "primary": "§3",
    "secondary": "§9",
    "accent": "§b",
    "success": "§a",
    "warning": "§6",
    "info": "§f"
  },
  "borders": {
    "main": "minecraft:cyan_stained_glass_pane",
    "accent": "minecraft:light_blue_stained_glass_pane"
  }
}
```

### Player Theme Preferences

Players can change their theme using:
```
/gui theme <theme_name>
```

Available themes: `default`, `dark`, `ocean`, `custom`

## 🔧 Configuration Guide

### Basic GUI Configuration

Each GUI configuration file follows this structure:

```json
{
  "gui_type": {
    "enabled": true,
    "title": "§6§lGUI Title",
    "size": 54,
    "theme": "default",
    "auto_refresh": true,
    "require_permission": "neoessentials.gui.type"
  },
  "layout": {
    "title": "§6§lActual Display Title",
    "size": 54,
    "slots": {
      "slot_definition": {
        "type": "slot_type",
        "item": "minecraft:item_name",
        "name": "§fDisplay Name",
        "lore": ["§7Lore line 1", "§7Lore line 2"],
        "action": "action_name"
      }
    }
  }
}
```

### Slot Range Definitions

Slots can be defined as:
- **Single slot**: `"9": { ... }`
- **Range**: `"0-8": { ... }` (slots 0 through 8)
- **Multiple ranges**: `"18-26": { ... }`, `"36-44": { ... }`

### Slot Types

#### Border Slots
```json
"0-8": {
  "type": "border",
  "item": "minecraft:gray_stained_glass_pane",
  "name": " ",
  "action": "none"
}
```

#### Functional Slots
```json
"9": {
  "type": "function",
  "item": "minecraft:emerald",
  "name": "§2💰 Economy",
  "lore": [
    "§7Manage server economy",
    "§7View transactions",
    "",
    "§eClick to open!"
  ],
  "action": "open_economy"
}
```

#### Category Slots
```json
"10": {
  "type": "category",
  "item": "minecraft:chest",
  "name": "§6📦 Shop Categories",
  "category": "tools",
  "action": "open_shop_category"
}
```

#### Dynamic Slots
```json
"27-35": {
  "type": "dynamic",
  "max_items": 9,
  "source": "online_players",
  "template": {
    "item": "minecraft:player_head",
    "name": "§f{player_name}",
    "lore": ["§7Click to teleport!"],
    "action": "teleport_to_player"
  }
}
```

## 🎯 Action System

### Built-in Actions

#### Navigation Actions
- `close_gui` - Close current GUI
- `refresh_gui` - Refresh GUI content
- `open_category` - Open specific category
- `go_back` - Return to previous GUI

#### Shop Actions
- `purchase_item` - Buy specific item
- `sell_item` - Sell item from inventory
- `open_shop_category` - Open shop category
- `show_item_details` - Display item information

#### Teleportation Actions
- `warp_to:warp_name` - Teleport to specific warp
- `teleport_spawn` - Teleport to spawn
- `teleport_home` - Teleport to player home
- `open_tpa_menu` - Open TPA interface

#### Admin Actions
- `open_player_management` - Player admin tools
- `open_economy_management` - Economy admin panel
- `quick_ban` - Quick ban interface
- `emergency_stop` - Emergency server controls

### Custom Actions

You can create custom actions by extending the action system:

```json
"custom_action": {
  "type": "custom",
  "command": "/custom command {player}",
  "permission": "custom.permission",
  "confirmation": true,
  "sound": "minecraft:block.note_block.pling"
}
```

## 📊 Live Data Integration

### Available Placeholders

#### Player Data
- `{player}` - Player name
- `{player_balance}` - Player's money
- `{player_level}` - Player level
- `{player_health}` - Current health
- `{player_world}` - Current world

#### Server Data
- `{server_players}` - Online player count
- `{server_max_players}` - Maximum players
- `{server_tps}` - Server TPS
- `{server_uptime}` - Server uptime
- `{server_memory}` - Memory usage

#### Economy Data
- `{total_money}` - Total money in circulation
- `{avg_balance}` - Average player balance
- `{richest_player}` - Wealthiest player
- `{daily_transactions}` - Today's transactions

#### Time and Date
- `{current_time}` - Current server time
- `{current_date}` - Current date
- `{world_time}` - In-game time
- `{uptime}` - Server uptime

### Dynamic Content Updates

Live data is automatically updated based on configuration:

```json
"live_data": {
  "enabled": true,
  "update_interval": 5,
  "placeholders": [
    "server_players",
    "server_tps",
    "player_balance"
  ]
}
```

## 🔐 Permission System

### GUI Access Permissions

#### Basic Access
- `neoessentials.gui.shop` - Access shop GUI
- `neoessentials.gui.stats` - Access statistics GUI
- `neoessentials.gui.kits` - Access kits GUI
- `neoessentials.gui.warps` - Access warps GUI
- `neoessentials.gui.teleport` - Access teleport GUI

#### Administrative Access
- `neoessentials.gui.admin` - Access admin panel
- `neoessentials.gui.economy` - Access economy management
- `neoessentials.gui.create` - Create custom GUIs

#### Advanced Permissions
- `neoessentials.gui.theme.change` - Change GUI themes
- `neoessentials.gui.reload` - Reload GUI configurations
- `neoessentials.gui.bypass` - Bypass GUI restrictions

### Permission-Based Customization

GUIs can be customized based on player permissions:

```json
"conditional_slots": {
  "vip_slot": {
    "permission": "server.vip",
    "item": "minecraft:diamond",
    "name": "§b💎 VIP Features",
    "action": "open_vip_menu"
  },
  "admin_slot": {
    "permission": "server.admin",
    "item": "minecraft:command_block",
    "name": "§4⚡ Admin Tools",
    "action": "open_admin_tools"
  }
}
```

## 🎵 Sound System Integration

### Sound Configuration

```json
"sounds": {
  "enabled": true,
  "volume": 1.0,
  "actions": {
    "gui_open": "minecraft:block.note_block.pling",
    "gui_close": "minecraft:block.note_block.bass",
    "button_click": "minecraft:ui.button.click",
    "purchase_success": "minecraft:entity.experience_orb.pickup",
    "purchase_fail": "minecraft:block.note_block.didgeridoo",
    "teleport": "minecraft:entity.enderman.teleport",
    "error": "minecraft:block.note_block.didgeridoo"
  }
}
```

### Per-Action Sounds

Individual actions can have custom sounds:

```json
"action_with_sound": {
  "type": "function",
  "action": "purchase_item",
  "sound": {
    "success": "minecraft:entity.villager.yes",
    "failure": "minecraft:entity.villager.no",
    "volume": 0.8
  }
}
```

## 🌍 Multi-Language Support

### Language Configuration

```json
"language": {
  "enabled": true,
  "default": "en_US",
  "auto_detect": true,
  "available": ["en_US", "es_ES", "fr_FR", "de_DE"]
}
```

### Translation Files

**`languages/en_US.json`**:
```json
{
  "gui": {
    "shop": {
      "title": "§6§lServer Shop",
      "categories": {
        "tools": "§6🔧 Tools",
        "blocks": "§3🧱 Blocks",
        "food": "§c🍎 Food"
      },
      "actions": {
        "purchase": "§aClick to purchase!",
        "insufficient_funds": "§cNot enough money!"
      }
    }
  }
}
```

**`languages/es_ES.json`**:
```json
{
  "gui": {
    "shop": {
      "title": "§6§lTienda del Servidor",
      "categories": {
        "tools": "§6🔧 Herramientas",
        "blocks": "§3🧱 Bloques",
        "food": "§c🍎 Comida"
      },
      "actions": {
        "purchase": "§a¡Clic para comprar!",
        "insufficient_funds": "§c¡No tienes suficiente dinero!"
      }
    }
  }
}
```

## 🔄 Hot-Reloading System

### Configuration Reloading

Reload all GUI configurations without server restart:

```bash
/neoessentials gui reload
```

### Specific GUI Reloading

Reload individual GUI configurations:

```bash
/neoessentials gui reload shop
/neoessentials gui reload admin
```

### Player Preference Reloading

Reload player preferences and themes:

```bash
/neoessentials gui reload preferences
```

## 🛠️ Creating Custom GUIs

### Step 1: Create Configuration File

Create a new JSON file in `config/gui/`:

```json
{
  "custom_gui": {
    "enabled": true,
    "title": "§d§lCustom Interface",
    "size": 27,
    "theme": "default",
    "require_permission": "custom.gui.access"
  },
  "layout": {
    "title": "§d§lMy Custom GUI",
    "size": 27,
    "slots": {
      "0-8": {
        "type": "border",
        "item": "minecraft:purple_stained_glass_pane",
        "name": " ",
        "action": "none"
      },
      "13": {
        "type": "function",
        "item": "minecraft:nether_star",
        "name": "§5✨ Custom Action",
        "lore": [
          "§7This is a custom button",
          "§7It performs a custom action",
          "",
          "§eClick to activate!"
        ],
        "action": "custom_action"
      }
    }
  }
}
```

### Step 2: Register Custom Actions

Extend the action system to handle custom actions:

```java
// This would be done in your custom code
public void handleCustomAction(Player player) {
    player.sendSystemMessage(Component.literal("§5Custom action executed!"));
    // Your custom logic here
}
```

### Step 3: Add Access Command

Register a command to open your custom GUI:

```bash
/customgui
```

## 🐛 Troubleshooting

### Common Issues

#### GUI Not Loading
1. **Check JSON syntax** - Use a JSON validator
2. **Verify file permissions** - Ensure files are readable
3. **Check server logs** - Look for parsing errors
4. **Validate slot ranges** - Ensure slots don't exceed GUI size

#### Actions Not Working
1. **Verify action names** - Check for typos in action definitions
2. **Check permissions** - Ensure player has required permissions
3. **Test with admin account** - Rule out permission issues
4. **Check console logs** - Look for action execution errors

#### Performance Issues
1. **Reduce update intervals** - Lower refresh rates for complex GUIs
2. **Limit dynamic slots** - Too many dynamic elements can cause lag
3. **Optimize placeholders** - Use caching for expensive data
4. **Check server resources** - Ensure adequate memory and CPU

### Debug Mode

Enable debug mode for detailed logging:

```json
"debug": {
  "enabled": true,
  "log_level": "DEBUG",
  "log_actions": true,
  "log_placeholders": true,
  "log_permissions": true
}
```

### Validation Tools

Use built-in validation commands:

```bash
/neoessentials gui validate <gui_name>
/neoessentials gui test <gui_name> <player>
```

## 📈 Performance Optimization

### Best Practices

#### Configuration Optimization
1. **Use appropriate GUI sizes** - Larger GUIs use more resources
2. **Limit dynamic content** - Too many live updates can cause lag
3. **Cache expensive operations** - Use caching for database queries
4. **Optimize update intervals** - Balance freshness with performance

#### Server Resource Management
```json
"performance": {
  "max_concurrent_guis": 50,
  "gui_timeout": 300,
  "cache_size": 1000,
  "cleanup_interval": 60
}
```

#### Memory Management
- **Automatic cleanup** of inactive GUIs
- **Smart caching** of frequently accessed data
- **Garbage collection optimization** for GUI objects

## 🔧 Advanced Features

### Conditional Slot Display

Show/hide slots based on conditions:

```json
"conditional_slot": {
  "type": "conditional",
  "conditions": [
    {
      "type": "permission",
      "value": "vip.access"
    },
    {
      "type": "world",
      "value": "survival"
    },
    {
      "type": "time",
      "value": "day"
    }
  ],
  "item": "minecraft:diamond",
  "name": "§bVIP Feature",
  "action": "vip_action"
}
```

### Progressive Disclosure

Reveal content based on player progress:

```json
"progressive_slot": {
  "type": "progressive",
  "unlock_conditions": [
    {
      "type": "level",
      "value": 10
    },
    {
      "type": "achievement",
      "value": "minecraft:acquire_iron"
    }
  ],
  "locked": {
    "item": "minecraft:barrier",
    "name": "§c🔒 Locked",
    "lore": ["§7Reach level 10 to unlock"]
  },
  "unlocked": {
    "item": "minecraft:emerald",
    "name": "§a✅ Unlocked Feature",
    "action": "special_feature"
  }
}
```

### Animation System

Add visual effects to GUI elements:

```json
"animated_slot": {
  "type": "animated",
  "animation": {
    "type": "cycle",
    "speed": 20,
    "items": [
      "minecraft:red_wool",
      "minecraft:orange_wool",
      "minecraft:yellow_wool",
      "minecraft:lime_wool",
      "minecraft:cyan_wool",
      "minecraft:purple_wool"
    ]
  },
  "name": "§c🎨 Animated Slot",
  "action": "animation_demo"
}
```

## 🤝 Integration with Other Systems

### Economy System Integration

```json
"shop_integration": {
  "enabled": true,
  "currency": "vault",
  "tax_rate": 0.05,
  "price_display": "§6{price} coins",
  "balance_display": "§2Balance: {balance}"
}
```

### Permission System Integration

```json
"permission_integration": {
  "system": "luckperms",
  "group_display": true,
  "prefix_display": true,
  "suffix_display": true
}
```

### Chat System Integration

```json
"chat_integration": {
  "enabled": true,
  "commands_in_chat": true,
  "gui_notifications": true,
  "click_actions": true
}
```

## 📚 Example Configurations

### Simple Shop GUI

```json
{
  "shop_gui": {
    "enabled": true,
    "title": "§6§lServer Shop",
    "size": 45,
    "theme": "default"
  },
  "layout": {
    "title": "§6§lServer Shop",
    "size": 45,
    "slots": {
      "0-8": {
        "type": "border",
        "item": "minecraft:yellow_stained_glass_pane",
        "name": " ",
        "action": "none"
      },
      "10": {
        "type": "category",
        "item": "minecraft:diamond_pickaxe",
        "name": "§3⛏ Tools",
        "category": "tools",
        "action": "open_shop_category"
      },
      "12": {
        "type": "category",
        "item": "minecraft:apple",
        "name": "§c🍎 Food",
        "category": "food",
        "action": "open_shop_category"
      },
      "14": {
        "type": "category",
        "item": "minecraft:stone",
        "name": "§8🧱 Blocks",
        "category": "blocks",
        "action": "open_shop_category"
      },
      "40": {
        "type": "close",
        "item": "minecraft:barrier",
        "name": "§c✖ Close",
        "action": "close_gui"
      }
    }
  }
}
```

### Player Stats GUI

```json
{
  "stats_gui": {
    "enabled": true,
    "title": "§b§lPlayer Statistics",
    "size": 54,
    "auto_refresh": true
  },
  "layout": {
    "title": "§b§lYour Statistics",
    "size": 54,
    "slots": {
      "0-8": {
        "type": "border",
        "item": "minecraft:light_blue_stained_glass_pane",
        "name": " ",
        "action": "none"
      },
      "13": {
        "type": "stat",
        "item": "minecraft:experience_bottle",
        "name": "§aLevel: {player_level}",
        "lore": [
          "§7Your current level",
          "§7XP: {player_xp}/{player_xp_next}",
          "§7Progress: {player_xp_progress}%"
        ],
        "action": "show_xp_details"
      },
      "22": {
        "type": "stat",
        "item": "minecraft:emerald",
        "name": "§2Balance: {player_balance}",
        "lore": [
          "§7Your current money",
          "§7Rank: #{player_wealth_rank}",
          "§7Total earned: {player_total_earned}"
        ],
        "action": "show_economy_details"
      }
    }
  }
}
```

## 🎯 GUI System Commands

### Player Commands

```bash
# Open specific GUIs
/gui shop          # Open shop interface
/gui stats         # Open statistics
/gui kits          # Open kit selection
/gui warps         # Open warp destinations
/gui teleport      # Open teleport hub

# Theme management
/gui theme <name>  # Change GUI theme
/gui theme list    # List available themes
/gui theme reset   # Reset to default theme

# Preferences
/gui settings      # Open GUI settings
/gui sounds <on/off>  # Toggle GUI sounds
```

### Admin Commands

```bash
# GUI management
/neoessentials gui reload [type]    # Reload configurations
/neoessentials gui validate <type>  # Validate configuration
/neoessentials gui test <type> <player>  # Test GUI for player
/neoessentials gui create <name>    # Create new GUI template

# Debug and monitoring
/neoessentials gui debug <on/off>   # Toggle debug mode
/neoessentials gui stats            # Show GUI usage statistics
/neoessentials gui performance      # Show performance metrics

# Player management
/neoessentials gui player <player> theme <theme>  # Set player theme
/neoessentials gui player <player> reset          # Reset player preferences
```

---

**Related Documentation**: 
- [Configuration Guide](Configuration.md) - Main configuration setup
- [Essential Commands](Essential-Commands.md) - Core command documentation  
- [Permissions](Permissions.md) - Permission system setup
- [Quick Start](Quick-Start.md) - Getting started guide

*Last Updated: August 6, 2025*
