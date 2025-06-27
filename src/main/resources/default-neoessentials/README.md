# NeoEssentials Configuration

NeoEssentials stores tablist templates and animations in YAML files in the main server directory:

## Configuration Structure

- `./neoessentials/tablist.yml` - Contains all header/footer templates, groups, and tablist settings
- `./neoessentials/animations.yml` - Contains all animation configurations

These files are automatically created when the mod is first run, or when missing. Configuration is done exclusively in YAML format for better readability and structure.

## Why YAML Only?

YAML provides better readability and structure for complex nested configurations compared to JSON or TOML. It's easier to edit by hand and supports comments, making it ideal for server configuration files.

## Migration

If you have old JSON or TOML files (templates.json, animations.json, tablist.toml), they should be migrated to the new YAML format. The new system loads configuration exclusively from YAML files.

## Format

Configuration uses YAML format:

1. **tablist.yml** - Main tablist configuration including headers, footers, groups, and display settings
2. **animations.yml** - Animation definitions for headers, footers, and other dynamic content

## Tablist Configuration Format

The `tablist.yml` file contains:

- Global headers and footers
- Group-specific headers and footers
- Tablist display settings
- Player sorting options
- Update intervals

## Animations Format

The `animations.yml` file contains:

- Custom hex color animations with timing settings
- Gradient animations with color stops
- Pulse animations
- Text scroll animations

## Examples

See the default YAML files for examples of how to configure tablist settings and animations.

## Documentation

For detailed documentation on YAML configuration:

- Configuration Guide: https://github.com/ZeroG-Network/NeoEssentials/wiki/Configuration
- YAML Templates: https://github.com/ZeroG-Network/NeoEssentials/wiki/YAML-Templates
- Tablist Configuration: https://github.com/ZeroG-Network/NeoEssentials/wiki/Tablist-Configuration
- Format Guide: https://github.com/ZeroG-Network/NeoEssentials/wiki/Colors-and-Formatting

## Support

If you need help with your configuration:
- Discord: https://discord.gg/dUGAQF2Mga
- GitHub: https://github.com/ZeroG-Network/NeoEssentials/issues