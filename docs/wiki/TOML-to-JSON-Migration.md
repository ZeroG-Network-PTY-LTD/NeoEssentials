# TOML to JSON Migration

NeoEssentials has updated its tablist template and animation system to use JSON files instead of TOML. This page explains how to migrate your configurations to the new format.

## Background

In the latest version of NeoEssentials, we've moved the tablist templates and animations from TOML format to JSON format, and relocated them from the `config/neoessentials/` directory to the main `neoessentials/` directory in your server root.

## Why the Change?

1. **Better structure support**: JSON handles complex nested structures better
2. **Enhanced flexibility**: More options for animations and templates
3. **Improved organization**: Separating templates from basic settings
4. **Multi-line support**: Better handling of multi-line text
5. **Format options**: Support for both JSON and YML formats

## Automatic Migration

When you first start your server with the new version, NeoEssentials will:

1. Detect your existing TOML configurations
2. Create backup copies with `.bak` extensions
3. Generate new JSON files in the `neoessentials/` directory
4. Copy all your existing templates and settings to the new format

This process happens automatically and requires no manual intervention.

## File Location Changes

| Old Location (TOML) | New Location (JSON/YML) |
|---------------------|-------------------------|
| `config/neoessentials/tablist.toml` | `config/neoessentials/tablist.toml` (basic settings only) |
| `config/neoessentials/templates/` | `neoessentials/templates.json` or `templates.yml` |
| `config/neoessentials/animations/` | `neoessentials/animations.json` or `animations.yml` |

## Format Changes

### Headers and Footers

**Old TOML format**:
```toml
[tablist.templates.headers]
templates = [
    "&6&l✦ &b&lNeoEssentials Server &6&l✦",
    "&eWelcome, &a%player%&e!"
]
```

**New JSON format**:
```json
{
  "templates": {
    "headers": [
      "&6&l✦ &b&lNeoEssentials Server &6&l✦",
      "&eWelcome, &a%player%&e!"
    ]
  }
}
```

### Group Templates

**Old TOML format**:
```toml
[tablist.templates.groups.admin.headers]
templates = [
    "&4&l★ &c&lAdmin Panel &4&l★"
]
```

**New JSON format**:
```json
{
  "groups": {
    "admin": {
      "headers": [
        "&4&l★ &c&lAdmin Panel &4&l★"
      ]
    }
  }
}
```

### Boss Bars

**Old TOML format**:
```toml
[tablist.bossbars]
enabled = true
templates = [
    "{color:red}{style:progress}{progress:1.0}Server TPS: %tps%"
]
```

**New JSON format**:
```json
{
  "bossbars": {
    "global": [
      "{color:red}{style:progress}{progress:1.0}Server TPS: %tps%"
    ]
  }
}
```

## Manual Migration

If you need to manually migrate your settings:

1. Create a new JSON file at `neoessentials/templates.json`
2. Follow the structure shown in the examples above
3. Transfer your templates from the old TOML format to the new JSON format
4. Do the same for animations in `neoessentials/animations.json`

## What to Keep in TOML

Some basic settings remain in the `tablist.toml` file:

```toml
[tablist]
# How often to update the tablist (in milliseconds)
updateInterval = 2000

# Enable sorting of players in the tablist
enableSorting = true

# Sort type: "name", "rank", or "playtime"
sortType = "name"

# Enable tablist animations
enableAnimations = true

# Animation speed multiplier (1-10)
animationSpeed = 1

[bossbars]
# Enable the boss bar feature
enabled = true

# Maximum boss bars per player
bossBarLimitPerPlayer = 3
```

## YML Support

As an alternative to JSON, you can use YAML format:

```yaml
templates:
  headers:
    - '&6&l✦ &b&lNeoEssentials Server &6&l✦'
    - '&eWelcome, &a%player%&e!'
  footers:
    - '&eBalance: &a%balance% coins'
```

Save this as `neoessentials/templates.yml` if you prefer YAML over JSON.

## Testing Your Migration

After migration:

1. Start your server and check the tablist
2. Verify that all your templates appear correctly
3. Check that boss bars are displayed properly
4. Test any group-specific templates

## Troubleshooting

If you encounter issues:

1. Check server logs for JSON parsing errors
2. Validate your JSON using [JSONLint](https://jsonlint.com/)
3. Ensure all strings are in double quotes (`"`)
4. Check for missing commas or mismatched brackets
5. Try reloading with `/neoessentials tablist reload`

## Need Help?

If you need assistance with migration:

- Visit our [Discord server](https://discord.gg/dUGAQF2Mga)
- Open an issue on [GitHub](https://github.com/ZeroG-Network/NeoEssentials/issues)
- Check our [JSON Templates](https://github.com/ZeroG-Network/NeoEssentials/wiki/JSON-Templates) page for more examples

For more detailed information, see our [JSON Templates Guide](https://github.com/ZeroG-Network/NeoEssentials/wiki/JSON-Templates-Guide).
