# YML Configuration Guide

Complete YAML configuration guide for NeoEssentials v1.0.1.89+ including the three-tier update system and multiple animation placeholders.

## 🎯 Overview

NeoEssentials supports **YAML (YML)** as the recommended format for templates and animations. YAML is human-friendly, easier to read than JSON, and perfect for complex animation configurations.

### Why YAML?
- ✅ **More Readable** - Clean syntax without brackets and commas
- ✅ **Less Error-Prone** - Indentation-based structure  
- ✅ **Better for Animations** - Easier to manage complex frame sequences
- ✅ **Comments Supported** - Add documentation directly in config files

## 📁 File Structure

YAML configuration files are located in the server root `neoessentials/` directory:

| File | Purpose | Priority |
|------|---------|----------|
| `tablist.yml` | **Main tablist configuration** | **Primary** |
| `animations.yml` | **Animation definitions** | **Primary** |
| `templates.yml` | Additional template configurations | Secondary |

### Loading Priority
1. **YAML files** (`.yml`) - **Recommended and preferred**
2. JSON files (`.json`) - Legacy support
3. TOML files (`.toml`) - Basic settings only

## 🚀 Core Configuration: `tablist.yml`

### Basic Structure
```yaml
# NeoEssentials Tablist Configuration v1.0.1.89+
settings:
  # Three-tier update system for optimal performance
  update_interval: 3000              # Template switching (3 seconds)
  placeholder_update_interval: 250   # Dynamic data updates (250ms)
  animation_frame_interval: 25       # Ultra-smooth animations (25ms)
  
  # Feature toggles
  enable_animations: true
  enable_headers: true
  enable_footers: true
  enable_group_specific: true

# Default templates for all players
templates:
  headers:
    - "&6&l✦ &b&lNeoEssentials Server &6&l✦"
    - "<anim:welcome>"
    - "&a%player%&e! <anim:server_name>"
    - "&eOnline: &a%online%&e/&a%max% <anim:pulse>"
  
  footers:
    - "&eBalance: &a%balance% coins"
    - "<anim:example> &7| <anim:rainbow>"
    - "&eServer TPS: &a%tps% <anim:clock>"
### Group-Specific Templates
```yaml
# Group-specific configurations  
groups:
  admin:
    headers:
      - "&4&l★ &c&lAdmin Panel &4&l★"
      - "&a%player% &7| &cAdmin Mode"
      - "<anim:admin_status> &7| &eOnline: &a%online%"
    footers:
      - "&cAdmin Commands: &f/neoessentials help"
      - "&cServer uptime: &f%uptime%"
  
  vip:
    headers:
      - "&6&l⚜ &e&lVIP Member &6&l⚜"
      - "&eWelcome back, &6%player%&e!"
      - "<anim:vip_benefits> &7| <anim:pulse>"
    footers:
      - "&6VIP Balance: &e%balance% coins"
      - "&6Use &e/vip help &6for perks list"
```

## 🎨 Animation Configuration: `animations.yml`

### Ultra-Smooth Animation Examples
```yaml
# Animation definitions optimized for v1.0.1.89+
animations:
  # Typewriter welcome effect
  welcome:
    interval: 100
    frames:
      - "&eW"
      - "&eWe"
      - "&eWel"
      - "&eWelc"
      - "&eWelco"
      - "&eWelcom"
      - "&eWelcome"
      - "&eWelcome!"

  # Ultra-smooth pulse (25ms for 40 FPS effect)
  pulse:
    interval: 25
    frames:
      - "&f●"
      - "&7●"
      - "&8●"
      - "&7●"

  # Rainbow server name
  server_name:
    interval: 80
    frames:
      - "&b&lM&f&lyServer"
      - "&f&lM&b&ly&f&lServer"
      - "&f&lMy&b&lS&f&lerver"
      - "&f&lMyS&b&le&f&lrver"
      - "&f&lMySe&b&lr&f&lver"
      - "&f&lMyServer"

  # Hex gradient rainbow
  rainbow:
    interval: 50
    frames:
      - "&#FF0000R&#FF3300a&#FF6600i&#FF9900n&#FFCC00b&#FFFF00o&#CCFF00w"
      - "&#FF3300R&#FF6600a&#FF9900i&#FFCC00n&#FFFF00b&#CCFF00o&#FF0000w"
      - "&#FF6600R&#FF9900a&#FFCC00i&#FFFF00n&#CCFF00b&#FF0000o&#FF3300w"

  # Clock animation
  clock:
    interval: 120
    frames: ["🕐", "🕑", "🕒", "🕓", "🕔", "🕕", "🕖", "🕗", "🕘", "🕙", "🕚", "🕛"]
```

## 📝 YAML Syntax Guide

### Key Rules
1. **Indentation**: Use 2 spaces (no tabs!)
2. **Lists**: Start with `- ` (hyphen + space)
3. **Strings**: Use quotes for special characters
4. **Comments**: Start lines with `#`

### Common Patterns
```yaml
# Simple list
headers:
  - "Header 1"
  - "Header 2"

# Nested structure
groups:
  admin:
    headers:
      - "Admin header"
  
# Animation with multiple frames
animations:
  my_anim:
    interval: 50
    frames:
      - "Frame 1"
      - "Frame 2"
```

## 🔧 Advanced Configuration

### Boss Bars
```yaml
bossbars:
  global:
    - "{color:red}{style:progress}{progress:1.0}Server TPS: %tps%"
    - "{color:green}{style:notched_6}{progress:0.8}Welcome!"
  
  groups:
    admin:
      - "{color:purple}{style:progress}{progress:1.0}Admin Mode"
```

### Performance Tuning
```yaml
settings:
  # Ultra-smooth animations (high-end servers)
  animation_frame_interval: 25
  
  # Balanced performance (most servers)
  animation_frame_interval: 50
  
  # Conservative performance (lower-end servers)
  animation_frame_interval: 100
```

## ⚠️ Common YAML Mistakes

| Issue | Problem | Solution |
|-------|---------|----------|
| **Indentation Error** | Mixed tabs/spaces | Use only 2 spaces for indentation |
| **Missing Colon** | `interval 50` | Use `interval: 50` |
| **List Format** | `headers "text"` | Use `headers:` then `- "text"` |
| **Special Characters** | Unquoted `&` symbols | Wrap in quotes: `"&aText"` |

## 🔄 File Migration

### From JSON to YAML
Use online converters or manual conversion:

**JSON:**
```json
{
  "templates": {
    "headers": ["&aHello"]
  }
}
```

**YAML:**
```yaml
templates:
  headers:
    - "&aHello"
```

## 🛠️ Validation & Testing

### Debug Commands
```bash
/tablist reload    # Reload YAML configuration
/tablist debug     # Show parsing status
/neoessentials debug  # Enable detailed logging
```

### Validation Tools
- [YAMLLint](http://www.yamllint.com/) - Online syntax checker
- [JSON to YAML](https://www.json2yaml.com/) - Format converter

---

*YAML configuration provides the most user-friendly way to manage complex tablist templates and animations in NeoEssentials!*

### templates.yml Example

```yaml
templates:
  headers:
    - '&6&l✦ &b&lNeoEssentials Server &6&l✦'
    - '&eWelcome, &a%player%&e!'
    - '&eOnline players: &a%online%/%max%'
  footers:
    - '&eBalance: &a%balance% coins'
    - '&eWebsite: &awww.example.com'
    - '&eThanks for playing!'

groups:
  admin:
    headers:
      - '&4&l★ &c&lAdmin Panel &4&l★'
      - '&cServer TPS: &f%tps% &7| &cMemory: &f%memory_percent%'
    footers:
      - '&cAdmin Command Help: &f/neoessentials help'
      - '&cServer uptime: &f%uptime%'
  vip:
    headers:
      - '&6&l⚜ &e&lVIP Perks Active &6&l⚜'
      - '&eWelcome back, &6%player%&e!'
    footers:
      - '&6VIP Balance: &e%balance% coins'
      - '&6Use &e/vip help &6for a list of perks'

bossbars:
  global:
    - '{color:red}{style:progress}{progress:1.0}Server TPS: %tps%'
    - '{color:green}{style:notched_6}{progress:0.8}Welcome to the server!'
  groups:
    admin:
      - '{color:purple}{style:progress}{progress:1.0}Admin Mode Active'
    vip:
      - '{color:gold}{style:progress}{progress:1.0}VIP Status Active'

metadata:
  schemaVersion: '1.0'
  description: 'NeoEssentials Tablist Templates'
  generateTime: '2025-06-25'
```

### animations.yml Example

```yaml
metadata:
  version: '1.0.0'
  description: 'NeoEssentials Tablist Animations'

animations:
  default:
    change-interval: 50
    texts:
      - '&#54C5EAE&#54DAF4x&#54C5EAa&#54B1DFm&#549CD5p&#5487CBl&#5473C0e'
      - '&#54B1DFE&#54C5EAx&#54DAF4a&#54C5EAm&#54B1DFp&#549CD5l&#5487CBe'
  rainbow:
    change-interval: 30
    texts:
      - '&#FF0000R&#FF7F00a&#FFFF00i&#00FF00n&#0000FFb&#4B0082o&#9400D3w'
      - '&#FF7F00R&#FFFF00a&#00FF00i&#0000FFn&#4B0082b&#9400D3o&#FF0000w'
      - '&#FFFF00R&#00FF00a&#0000FFi&#4B0082n&#9400D3b&#FF0000o&#FF7F00w'

gradients:
  evening_sky:
    colors:
      - '#1E2A5E'
      - '#493267'
      - '#FB5858'
      - '#D68060'
    steps: 15
  water:
    colors:
      - '#015C92'
      - '#2D82B5'
      - '#88CDF6'
      - '#72EFDD'
    steps: 12
```

## YML Syntax Tips

1. **Indentation**: YAML uses indentation (spaces, not tabs) to denote structure
2. **Lists**: Use hyphens (`-`) followed by a space for list items
3. **Strings**: Can be unquoted, but best practice is to use single quotes (`'`) for strings containing special characters
4. **Multi-line**: Use `>` for folded multi-line text or `|` for literal multi-line text

```yaml
# Multi-line example
headers:
  - >
    &6&l✦ &b&lNeoEssentials Server &6&l✦
    &eWelcome, &a%player%&e!
```

## Advantages of YML

1. **Readability**: Cleaner format with less punctuation
2. **Comments**: YAML supports comments using `#`
3. **Less Error-Prone**: No need for commas between items
4. **Multi-line Support**: Better handling of multi-line strings

## Validation

If your YAML is invalid:

1. Check the server logs for parsing errors
2. Validate your YAML using an online tool like [YAMLLint](http://www.yamllint.com/)
3. Watch for common errors like incorrect indentation or missing colons
4. Use quotes for strings containing special characters

## Converting Between Formats

To convert from JSON to YML or vice versa:

1. Use an online converter tool like [JSON to YAML](https://www.json2yaml.com/)
2. Copy your existing JSON content
3. Paste into the converter
4. Save the output as a YML file in the `neoessentials/` directory

## Implementation Details

NeoEssentials includes dedicated YAML parsing support in both the `TemplateManager` and `AnimationManager` classes:

1. **Load Priority**: When both JSON and YML files exist, the system checks for files in the following order:
   - `neoessentials/templates.json` or `neoessentials/animations.json` (first choice)
   - `neoessentials/templates.yml` or `neoessentials/animations.yml` (second choice)
   - Legacy TOML files (last resort)

2. **YAML Parser**: A custom lightweight YAML parser handles the most common YAML structures used in templates and animations, including:
   - Nested maps
   - Lists
   - String values (with or without quotes)
   - Numeric values
   - Boolean values
   - Multi-line strings

3. **Error Handling**: If YAML parsing fails, the system falls back to default templates and logs the error.

## Need Help?

If you need assistance with YML configuration:

- Visit our [Discord server](https://discord.gg/dUGAQF2Mga)
- Open an issue on [GitHub](https://github.com/ZeroG-Network/NeoEssentials/issues)
- Check our [JSON Templates](https://github.com/ZeroG-Network/NeoEssentials/wiki/JSON-Templates) page for structure reference

For more detailed information, see our [JSON Templates Guide](https://github.com/ZeroG-Network/NeoEssentials/wiki/JSON-Templates-Guide) (the structure is the same for YML).
