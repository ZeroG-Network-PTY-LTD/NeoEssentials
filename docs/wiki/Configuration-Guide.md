# Configuration Guide

Complete guide to configuring NeoEssentials v1.0.2+ with the latest features including the comprehensive economy system, ultra-smooth animations, and advanced tablist customization.

## 📁 Configuration Structure

NeoEssentials uses a modern hybrid configuration system:

### TOML Configs (`config/neoessentials/`)
Basic settings and system toggles:
- `general.toml` - Core mod settings
- `homes.toml` - Home system configuration
- `warps.toml` - Warp system settings
- `kits.toml` - Kit system configuration
- `database.toml` - Database connection settings

### YAML Configs (`config/neoessentials/`)
Advanced system configurations:
- **`economy.yml`** - **NEW!** Complete economy system configuration

### YAML Templates (`neoessentials/`)  
Advanced template and animation configurations:
- `tablist.yml` - Headers, footers, and group templates
- `animations.yml` - Custom animation definitions

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

### `economy.yml` - Economy System (NEW!)
```yaml
economy:
  enabled: true
  starting_balance: 100.0
  max_balance: 1000000.0
  allow_negative_balances: false

currencies:
  coins:
    display_name: "Coin"
    symbol: "$"
    is_default: true
    exchange_rate: 1.0
  gold_ingots:
    display_name: "Gold Ingot"
    symbol: "⚆"
    is_default: false
    exchange_rate: 10.0
    type: "RESOURCE"
  diamonds:
    display_name: "Diamond"
    symbol: "♦"
    is_default: false
    exchange_rate: 50.0
    type: "RESOURCE"

banking:
  enabled: true
  account_creation:
    creation_fee: 100.0
    auto_create_checking: true
  account_types:
    checking:
      base_interest_rate: 0.01
      withdrawal_limit: -1  # No limit
      minimum_balance: 0.0
    savings:
      base_interest_rate: 0.05
      withdrawal_limit: 5000.0
      minimum_balance: 100.0
    business:
      base_interest_rate: 0.02
      withdrawal_limit: -1
      minimum_balance: 500.0
    investment:
      base_interest_rate: 0.08
      withdrawal_limit: 0.0  # Cannot withdraw during term
      minimum_balance: 10000.0

loans:
  enabled: true
  types:
    personal:
      min_amount: 500.0
      max_amount: 50000.0
      max_term_months: 60
      base_interest_rate: 0.08
      requires_collateral: true
    business:
      min_amount: 1000.0
      max_amount: 500000.0
      max_term_months: 120
      base_interest_rate: 0.06
      requires_collateral: true
    mortgage:
      min_amount: 10000.0
      max_amount: 1000000.0
      max_term_months: 360
      base_interest_rate: 0.05
      requires_collateral: false

shops:
  enabled: true
  creation:
    creation_fee: 500.0
    max_shops_per_player: 5
    rental_cost_per_day: 50.0
  taxation:
    sales_tax_rate: 0.05
    income_tax_rate: 0.10

auctions:
  enabled: true
  settings:
    min_duration: 1      # hours
    max_duration: 168    # hours (7 days)
    listing_fee_rate: 0.02
    success_fee_rate: 0.05
    min_bid_increment: 0.05

analytics:
  enabled: true
  track_inflation: true
  wealth_distribution_analysis: true
  economic_health_monitoring: true
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
