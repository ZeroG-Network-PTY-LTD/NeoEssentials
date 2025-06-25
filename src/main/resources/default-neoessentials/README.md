# NeoEssentials Configuration

NeoEssentials stores templates and animations in JSON or YML files in the main server directory:

## Configuration Structure

- `./neoessentials/templates.json` or `./neoessentials/templates.yml` - Contains all header/footer templates and bossbars
- `./neoessentials/animations.json` or `./neoessentials/animations.yml` - Contains all animation configurations

These files are automatically created when the mod is first run, or when missing. You can use either JSON or YML format based on your preference.

## Why JSON Instead of TOML?

JSON provides better support for complex nested configurations and arrays, which are heavily used in the tablist template system. TOML has limitations when it comes to complex array structures and can cause serialization issues.

## Migration

If you previously customized templates or animations in the TOML config files, they will be automatically migrated to the new JSON format. The original TOML files will be backed up with a `.bak` extension.

## Format Options

You can choose between two formats:

1. **JSON** - Standard format (templates.json, animations.json)
2. **YML** - Alternative YAML format (templates.yml, animations.yml)

If both formats exist for the same configuration, JSON takes precedence.

## Templates Format

The `templates.json` file contains:

- Global headers and footers
- Group-specific headers and footers 
- Global boss bars
- Group-specific boss bars

## Animations Format

The `animations.json` file contains:

- Custom hex color animations with timing settings
- Gradient animations with color stops
- Pulse animations

## Examples

See the default files for examples of how to configure templates and animations.

## Documentation

For detailed documentation on templates and animations:

- Configuration Guide: https://github.com/ZeroG-Network/NeoEssentials/wiki/Configuration
- JSON Templates: https://github.com/ZeroG-Network/NeoEssentials/wiki/JSON-Templates
- YML Configuration: https://github.com/ZeroG-Network/NeoEssentials/wiki/YML-Configuration
- Format Guide: https://github.com/ZeroG-Network/NeoEssentials/wiki/Colors-and-Formatting

## Support

If you need help with your configuration:
- Discord: https://discord.gg/dUGAQF2Mga
- GitHub: https://github.com/ZeroG-Network/NeoEssentials/issues
