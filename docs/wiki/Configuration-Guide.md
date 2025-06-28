# Configuration Guide

Complete guide to configuring NeoEssentials v1.0.1.89+ with the latest features including ultra-smooth animations and advanced tablist customization.

## 📁 Configuration Structure

NeoEssentials uses a modern hybrid configuration system:

### TOML Configs (`config/neoessentials/`)
Basic settings and system toggles:
- `general.toml` - Core mod settings
- `tablist.toml` - Tablist system configuration
- `economy.toml` - Economy system settings
- `permissions.toml` - Permission system configuration

### YAML Templates (`neoessentials/`)  
Advanced template and animation configurations:
- `tablist.yml` - Headers, footers, and group templates
- `animations.yml` - Custom animation definitions
- `templates.yml` - Additional template configurations

## ⚙️ Essential Configuration Files

### `general.toml` - Core Settings
```toml
[general]
debug_mode = false
enable_metrics = true
locale = "en_US"

[performance]  
max_concurrent_operations = 100
cache_cleanup_interval = 300
```

### `tablist.toml` - Tablist Performance
```toml
[tablist]
# Three-tier update system (NEW in v1.0.1.89)
update_interval = 3000              # Template switching (3 seconds)
placeholder_update_interval = 250   # Dynamic data (250ms)  
animation_frame_interval = 25       # Ultra-smooth animations (25ms)

enable_animations = true
enable_headers = true
enable_footers = true
enable_group_specific = true
```

### `tablist.yml` - Templates & Animations
```yaml
settings:
  update_interval: 3000
  placeholder_update_interval: 250
  animation_frame_interval: 25
  enable_animations: true

templates:
  headers:
    - "&6&l✦ &b&lNeoEssentials Server &6&l✦"
    - "<anim:welcome>"
    - "&a%player%&e! <anim:server_name>"
  footers:
    - "&eBalance: &a%balance% coins"
    - "<anim:example> &7| <anim:rainbow>"
```

## 🚀 Advanced Configuration

### Three-Tier Update System  
**NEW in v1.0.1.89** - Revolutionary performance architecture:

| Update Type | Interval | Purpose | Performance Impact |
|-------------|----------|---------|-------------------|
| **Template Switching** | 3000ms | Cycle through different templates | Minimal |
| **Placeholder Updates** | 250ms | Dynamic data (player count, stats) | Low |
| **Animation Frames** | 25ms | Ultra-smooth animation rendering | Optimized |

### Multiple Animation Placeholders
Configure unlimited simultaneous animations:
```yaml
animations:
  welcome:
    interval: 100
    frames: ["&aW", "&aWe", "&aWel", "&aWelcome!"]
  
  rainbow:
    interval: 50  
    frames: ["&#FF0000Text", "&#FF7F00Text", "&#FFFF00Text"]
    
  pulse:
    interval: 25
    frames: ["&f●", "&7●", "&8●", "&7●"]
```

## 🔄 Configuration Management

### Reloading Configuration
Reload without server restart:
```
/neoessentials reload          # Reload everything
/tablist reload               # Reload tablist only  
/neoessentials reload tablist # Reload specific system
```

### File Format Support
- **YAML** (`.yml`) - Recommended for readability
- **JSON** (`.json`) - Alternative format  
- **TOML** (`.toml`) - Core configuration files

Priority: YAML > JSON > TOML (for templates)

### Automatic Backups
NeoEssentials automatically creates backups:
- **Before updates**: `config_backup_YYYY-MM-DD/`
- **On reload**: `.bak` files created
- **On migration**: Previous format preserved

- Backups are stored in `config/neoessentials/backups/`
- Files are timestamped for easy identification
- Use `/neoessentials config restore [filename]` to restore a backup

## Advanced Configuration

For advanced configuration topics, see:

- [Custom Templates](Custom-Templates)
- [Placeholder Guide](Placeholders)
- [Integration Settings](Mod-Compatibility)
- [Performance Tuning](Performance-Optimization)

## Need Help?

If you need assistance with configuration:

- Visit our [Discord server](https://discord.gg/dUGAQF2Mga)
- Check the [Troubleshooting Guide](Troubleshooting)
- Open an [issue on GitHub](https://github.com/ZeroG-Network/NeoEssentials/issues)
