# NeoEssentials Configuration

NeoEssentials now stores templates and animations in JSON files in the main server directory:

## Configuration Structure

- `./neoessentials/templates.json` - Contains all header/footer templates and bossbars
- `./neoessentials/animations.json` - Contains all animation configurations

These files are automatically created when the mod is first run, or when missing.

## Why JSON Instead of TOML?

JSON provides better support for complex nested configurations and arrays, which are heavily used in the tablist template system. TOML has limitations when it comes to complex array structures and can cause serialization issues.

## Migration

If you previously customized templates or animations in the TOML config files, they will be automatically migrated to the new JSON format. The original TOML files will be backed up with a `.bak` extension.

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
