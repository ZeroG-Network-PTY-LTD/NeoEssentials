# JSON Templates Guide

This guide explains how to use JSON templates for configuring the tablist system in NeoEssentials.

## Overview

NeoEssentials uses JSON files for tablist templates and animations to provide maximum flexibility and support for complex structures. This guide covers how to create and modify these templates.

## File Locations

The JSON template files are located in the main server directory:

- `neoessentials/templates.json` - Contains header/footer templates and boss bar configs
- `neoessentials/animations.json` - Contains animation definitions

Alternatively, you can use YAML format with the same structure:

- `neoessentials/templates.yml`
- `neoessentials/animations.yml`

## Templates.json Structure

The templates.json file follows this basic structure:

```json
{
  "templates": {
    "headers": [
      "First header line",
      "Second header line"
    ],
    "footers": [
      "First footer line",
      "Second footer line"
    ]
  },
  "groups": {
    "admin": {
      "headers": [
        "Admin header line 1",
        "Admin header line 2"
      ],
      "footers": [
        "Admin footer line 1",
        "Admin footer line 2"
      ]
    },
    "vip": {
      "headers": [
        "VIP header line 1"
      ],
      "footers": [
        "VIP footer line 1"
      ]
    }
  },
  "bossbars": {
    "global": [
      "{color:red}{style:progress}{progress:1.0}Server TPS: %tps%"
    ],
    "groups": {
      "admin": [
        "{color:purple}{style:progress}{progress:1.0}Admin Mode"
      ]
    }
  }
}
```

## Animations.json Structure

The animations.json file follows this structure:

```json
{
  "metadata": {
    "version": "1.0.0",
    "description": "NeoEssentials Tablist Animations"
  },
  "animations": {
    "rainbow": {
      "change-interval": 30,
      "texts": [
        "&#FF0000R&#FF7F00a&#FFFF00i&#00FF00n&#0000FFb&#4B0082o&#9400D3w",
        "&#FF7F00R&#FFFF00a&#00FF00i&#0000FFn&#4B0082b&#9400D3o&#FF0000w"
      ]
    },
    "welcome": {
      "change-interval": 50,
      "texts": [
        "&eW&fe&el&fc&eo&fm&ee",
        "&fW&ee&fl&ec&fo&em&fe"
      ]
    }
  },
  "gradients": {
    "sunset": {
      "colors": [
        "#FF5E62",
        "#FF9966"
      ],
      "steps": 10
    }
  }
}
```

## Text Formatting

NeoEssentials supports multiple formatting methods:

### Minecraft Color Codes

```
&4Dark Red
&cRed
&6Gold
&eYellow
&2Dark Green
&aGreen
&bAqua
&3Dark Aqua
&1Dark Blue
&9Blue
&dLight Purple
&5Dark Purple
&fWhite
&7Light Gray
&8Dark Gray
&0Black
```

### Formatting Codes

```
&lBold
&mStrikethrough
&nUnderline
&oItalic
&kObfuscated
&r Reset formatting
```

### Hex Colors

```
&#FF0000This is red text
&#00FF00This is green text
&#0000FFThis is blue text
```

### Gradients

Gradients can be defined using hex color codes:

```
&#FF0000G&#FF5A00r&#FFB400a&#FFff00d&#A5ff00i&#4Bff00e&#00ff00n&#00ff5At
```

## Placeholders

NeoEssentials supports various placeholders in templates:

### Player Placeholders
- `%player%` - Player name
- `%displayname%` - Player display name
- `%health%` - Player health
- `%max_health%` - Player max health
- `%food%` - Player food level
- `%gamemode%` - Player game mode
- `%world%` - Player's world name
- `%x%`, `%y%`, `%z%` - Player coordinates

### Server Placeholders
- `%server_name%` - Server name
- `%motd%` - Server MOTD
- `%online%` - Online players
- `%max%` - Maximum players
- `%tps%` - Server TPS
- `%memory_used%` - Used memory
- `%memory_max%` - Maximum memory
- `%memory_percent%` - Memory usage percentage
- `%uptime%` - Server uptime

### Economy Placeholders
- `%balance%` - Player's balance
- `%balance_formatted%` - Formatted balance
- `%currency_symbol%` - Currency symbol
- `%currency_name%` - Currency name

### Permission Placeholders
- `%rank%` - Player's primary rank
- `%prefix%` - Player's prefix
- `%suffix%` - Player's suffix

## Boss Bar Configuration

Boss bars use a specific format:

```
{color:COLOR}{style:STYLE}{progress:PROGRESS}Message text here
```

Where:
- `COLOR` can be: `red`, `green`, `blue`, `yellow`, `purple`, `white`
- `STYLE` can be: `solid`, `segmented_6`, `segmented_10`, `segmented_12`, `segmented_20`
- `PROGRESS` is a decimal number between 0.0 and 1.0

Example:
```
{color:red}{style:segmented_10}{progress:0.75}Server TPS: %tps%
```

## Multi-Line Support

Both JSON and YML formats support multi-line text:

### JSON Multi-line
```json
"headers": [
  "Line 1\nLine 2\nLine 3"
]
```

### YML Multi-line
```yaml
headers:
  - |
    Line 1
    Line 2
    Line 3
```

## Animation Integration

To use animations in templates:

```
<anim:rainbow>This text will be animated
```

Where `rainbow` is the name of an animation defined in animations.json.

## Rotation and Cycles

Templates in headers and footers rotate automatically based on the cycle interval defined in tablist.toml:

```toml
[tablist]
cycleInterval = 3000  # milliseconds between each template cycle
```

## Need Help?

For more detailed examples and assistance:

- See our [YML Configuration Guide](YML-Configuration) for YAML syntax
- Visit our [Discord server](https://discord.gg/dUGAQF2Mga)
- Check the [TOML to JSON Migration Guide](TOML-to-JSON-Migration) for migration from older versions
