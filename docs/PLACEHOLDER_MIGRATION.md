# PlaceholderAPI Migration Guide

## For Server Administrators

If you're updating NeoEssentials and your placeholders aren't working, you need to update your configuration files to use the new PlaceholderAPI format.

### Old Format vs New Format

| Old Format | New Format |
|------------|------------|
| `{DISPLAYNAME}` | `{neoessentials_displayname}` |
| `{USERNAME}` | `{neoessentials_username}` |
| `{PREFIX}` | `{neoessentials_prefix}` |
| `{SUFFIX}` | `{neoessentials_suffix}` |
| `{WORLD}` | `{neoessentials_world}` |
| `{X}`, `{Y}`, `{Z}` | `{neoessentials_x}`, `{neoessentials_y}`, `{neoessentials_z}` |
| `{HEALTH}` | `{neoessentials_health}` |
| `{LEVEL}` | `{neoessentials_level}` |

### Quick Fix

1. **Stop your server**
2. **Edit `config/neoessentials/config.json`**
3. **Replace old placeholders with new ones:**

```json
{
  "chat": {
    "chat-format": "<{neoessentials_prefix} {neoessentials_username} {neoessentials_suffix}> {MESSAGE}",
    "customJoinMessage": "&a{neoessentials_prefix}{neoessentials_displayname} &ejoined the server!",
    "customQuitMessage": "&c{neoessentials_prefix}{neoessentials_displayname} &eleft the server!"
  }
}
```

4. **Start your server**

### Benefits of New System

- ✅ **Fixed PREFIX/SUFFIX** - Now properly resolves from permission system
- ✅ **Other Mod Support** - Other mods can register custom placeholders
- ✅ **More Placeholders** - Economy, health, location, and more built-in
- ✅ **Consistent Behavior** - Same placeholders work in chat, messages, and join/quit

### Available Placeholders

See the complete list in [PLACEHOLDER_API.md](PLACEHOLDER_API.md#available-placeholders)

### Troubleshooting

If placeholders show as literal text like `{neoessentials_prefix}`:
1. Check that you're using the correct placeholder names
2. Ensure the PlaceholderAPI is initialized (check server logs)
3. Verify your permission plugin is providing PREFIX/SUFFIX data