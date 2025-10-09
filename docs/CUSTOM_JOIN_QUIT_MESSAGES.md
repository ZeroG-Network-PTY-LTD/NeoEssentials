# Custom Join/Quit Messages

NeoEssentials supports custom join and quit messages that can be configured per server. These messages support all the same placeholders as chat formatting, allowing for rich, personalized welcome and farewell messages.

## Configuration

Custom messages are configured in the `chat` section of your `config.json` file:

```json
{
  "chat": {
    "customJoinMessage": "&a➤ &f{neoessentials_prefix}{neoessentials_displayname} &ejoined the server!",
    "customQuitMessage": "&c➤ &f{neoessentials_prefix}{neoessentials_displayname} &eleft the server!",
    "customNewUsernameMessage": "&6➤ &f{neoessentials_prefix}{neoessentials_displayname} &echanged their name!"
  }
}
```

## Available Placeholders

All join/quit messages support the following placeholders:

- `{neoessentials_displayname}` - Player's display name (with any formatting)
- `{neoessentials_username}` - Player's raw username (no formatting)
- `{neoessentials_prefix}` - Player's permission group prefix (if configured)
- `{neoessentials_suffix}` - Player's permission group suffix (if configured)
- `{neoessentials_world}` - The world the player is joining/leaving

**Note**: All placeholders now use the PlaceholderAPI system. You can also use custom placeholders registered by other mods.

## Color Codes

Messages support Minecraft color codes using the `&` symbol:

- `&a` - Light Green
- `&c` - Red
- `&e` - Yellow
- `&f` - White
- `&6` - Gold
- `&9` - Blue
- And all other standard Minecraft color codes

## Examples

### Simple Messages
```json
"customJoinMessage": "Welcome {DISPLAYNAME}!",
"customQuitMessage": "Goodbye {DISPLAYNAME}!"
```

### Colored Messages with Prefixes
```json
"customJoinMessage": "&a[+] &f{PREFIX}{DISPLAYNAME} &ehas joined the server",
"customQuitMessage": "&c[-] &f{PREFIX}{DISPLAYNAME} &ehas left the server"
```

### Disable Custom Messages
To use default Minecraft join/quit messages, set the value to `"none"`:

```json
"customJoinMessage": "none",
"customQuitMessage": "none"
```

## Integration with Discord

Custom join/quit messages will also be sent to Discord if you have any of the supported Discord integration mods installed:

- DiscordSRV
- DCIntegration (Discord Integration)
- SDLink (Simple Discord Link)

## Notes

- Messages are broadcasted to all players on the server
- If a custom message is not configured or set to "none", the default Minecraft join/quit messages will be used
- Custom messages respect the `hideJoinQuitMessagesAbove` setting if configured
- The system is compatible with permission-based prefixes and suffixes