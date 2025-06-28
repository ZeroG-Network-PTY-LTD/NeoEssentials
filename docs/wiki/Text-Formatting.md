# Text Formatting Guide

Complete text formatting guide for NeoEssentials v1.0.1.89+ including animation-optimized colors and gradient effects.

## 🎨 Color Systems

NeoEssentials supports multiple color formats for maximum flexibility:

### Traditional Color Codes (`&`)
| Code | Color | Animation Use |
|------|-------|---------------|
| `&a` | Green | ✅ Excellent for success states |
| `&c` | Red | ✅ Great for error/warning animations |
| `&e` | Yellow | ✅ Perfect for attention-grabbing effects |
| `&b` | Aqua | ✅ Ideal for cool/calm animations |
| `&6` | Gold | ✅ Premium/VIP animation effects |
| `&f` | White | ✅ Clean, professional animations |

### Hex Colors (`&#RRGGBB`) - **Recommended for Animations**
```yaml
# Perfect for smooth animations
&#FF0000  # Pure Red
&#00FF00  # Pure Green  
&#0066CC  # Professional Blue
&#FF6B35  # Vibrant Orange
&#7B2CBF  # Rich Purple
```

### Gradient Colors - **Ultra-Smooth Animation Effects**
Create seamless color transitions:
```yaml
animations:
  smooth_gradient:
    interval: 30
    frames:
      - "&#FF0000G&#FF3300r&#FF6600a&#FF9900d&#FFCC00i&#FFFF00e&#CCFF00n&#99FF00t"
      - "&#FF3300G&#FF6600r&#FF9900a&#FFCC00d&#FFFF00i&#CCFF00e&#99FF00n&#66FF00t"
      - "&#FF6600G&#FF9900r&#FFCC00a&#FFFF00d&#CCFF00i&#99FF00e&#66FF00n&#33FF00t"
```

## ✨ Formatting Codes

| Code | Effect | Animation Use |
|------|--------|---------------|
| `&l` | **Bold** | ✅ Great for emphasis in animations |
| `&o` | *Italic* | ✅ Subtle style variation |
| `&n` | Underlined | ⚠️ Use sparingly in animations |
| `&m` | ~~Strikethrough~~ | ⚠️ Can be hard to read in motion |
| `&k` | Obfuscated | ❌ Not recommended for animations |
| `&r` | Reset | ✅ Essential for animation frame resets |
|------|--------|---------|
| &l | **Bold** | **Bold text** |
| &m | ~~Strikethrough~~ | ~~Strikethrough text~~ |
| &n | <u>Underline</u> | <u>Underlined text</u> |
| &o | *Italic* | *Italic text* |
| &k | Obfuscated | ⋯⋯⋯⋯⋯⋯⋯ |
| &r | Reset | Resets all formatting |

## Advanced Formatting

### Hex Color Codes

NeoEssentials supports hex color codes for precise color control:

```
&#RRGGBB
```

Examples:
- `&#ff0000` - Pure red
- `&#00ff00` - Pure green
- `&#0000ff` - Pure blue
- `&#ff00ff` - Magenta
- `&#123456` - Custom shade

### Gradient Text

Create smooth color transitions:

```
{gradient:#start_color:#end_color}Your Text Here{/gradient}
```

Example:
```
{gradient:#ff0000:#0000ff}Welcome to the server!{/gradient}
```

This creates a smooth transition from red to blue across the text.

### Rainbow Text

Create animated rainbow text:

```
{rainbow:speed}Your Text Here{/rainbow}
```

The speed parameter is optional (1-10, default is 5).

Example:
```
{rainbow:3}Welcome to the server!{/rainbow}
```

### Animated Text

Create text that changes color over time:

```
{animate:#color1:#color2:#color3}Your Text Here{/animate}
```

Example:
```
{animate:#ff0000:#00ff00:#0000ff}Welcome to the server!{/animate}
```

## Format Templates

NeoEssentials allows you to create reusable format templates in `config/neoessentials/formats.toml`:

```toml
[formats]
warning = "&c&l[WARNING] &r&c"
info = "&b&l[INFO] &r&b"
success = "&a&l[SUCCESS] &r&a"
error = "&4&l[ERROR] &r&c"
staff = "&5&l[STAFF] &r&d"
```

These can then be used in any message by using:

```
{format:warning}This is a warning message!
```

## Using Formats in Configuration

You can use all formatting options in any NeoEssentials configuration file:

### In Messages

```toml
[messages]
welcome = "{gradient:#00aaff:#ff00aa}Welcome to the server, {player}!{/gradient}"
goodbye = "&cGoodbye, &e{player}&c! We hope to see you again soon!"
```

### In Tablist Headers/Footers

```toml
[tablist]
header = """
&6&l===== {gradient:#ffaa00:#ff5500}YOUR SERVER NAME{/gradient} &6&l=====
&eWelcome, &b{player}&e!
"""
footer = """
&7Players online: &a{online_players}&7/&a{max_players}
&7Website: &b&nwww.yourserver.com
"""
```

## Special Characters

NeoEssentials provides special character codes:

| Code | Character | Description |
|------|-----------|-------------|
| &h | ♥ | Heart symbol |
| &s | ★ | Star symbol |
| &p | ✓ | Check mark |
| &x | ✗ | X mark |
| &c | © | Copyright symbol |
| &r | ® | Registered trademark |
| &a | → | Right arrow |
| &< | « | Left double arrow |
| &> | » | Right double arrow |

## Text Alignment

Control text alignment in multi-line messages:

```
{center}This text will be centered{/center}
{right}This text will be right-aligned{/right}
{left}This text will be left-aligned{/left}
```

## Conditional Formatting

Display different text based on conditions:

```
{if:condition}Text if true{else}Text if false{/if}
```

Available conditions:
- `{if:permission:neoessentials.vip}VIP Content{else}Regular Content{/if}`
- `{if:group:admin}Admin Content{else}Regular Content{/if}`
- `{if:online:playername}Online{else}Offline{/if}`

## Hover and Click Events

Create interactive text:

```
{hover:text:Hover text here}Text to hover over{/hover}
{click:command:/say Hello}Click to run command{/click}
{click:suggest:/msg }Click to suggest command{/click}
{click:url:https://example.com}Click to open URL{/click}
```

You can combine hover and click events:

```
{hover:text:Click to visit our website}{click:url:https://example.com}Visit our website{/click}{/hover}
```

## Dynamic Content

Insert dynamic content with placeholders:

```
You have {balance} coins.
Server time: {server_time:HH:mm:ss}
Your coordinates: {x}, {y}, {z}
```

See the [Placeholders](Placeholders) guide for all available placeholders.

## Best Practices

1. **Readability**: Don't overuse formatting; keep text readable
2. **Consistency**: Maintain consistent formatting across your server
3. **Color Meaning**: Use colors consistently for specific types of messages
   - Green for success/confirmation
   - Red for errors/warnings
   - Blue/Aqua for information
   - Gold/Yellow for important notes
4. **Accessibility**: Ensure text remains readable for all players
5. **Performance**: Avoid excessive animation in frequently updated text

## Examples

### Welcome Message

```
{gradient:#00aaff:#ff00aa}&l=== Welcome to the Server ==={/gradient}
&eHello, &b{player}&e! We're glad to have you here!

&7» &aPlayers online: &f{online_players}/{max_players}
&7» &aServer TPS: &f{tps}

{center}&6Type &f/help &6for a list of commands{/center}
```

### Staff Announcement

```
{format:staff}&l=== ANNOUNCEMENT ===
{hover:text:Sent by {staff_member}}{animate:#ff0000:#ff7700:#ffff00}{message}{/animate}{/hover}
&7» Click to acknowledge: {click:command:/acknowledge}[✓]{/click}
```

### Rules Sign

```
&l&9Server Rules
&r&c1. No griefing
&c2. Be respectful
&c3. No cheating
&c4. Have fun!
```

## Troubleshooting

### Common Issues

- **Formatting Not Working**: Ensure you're using the correct syntax and valid color codes
- **Missing Placeholders**: Check that placeholders are spelled correctly and available
- **Animations Not Visible**: Confirm client supports animated text (some mods may interfere)
- **Hex Colors Not Working**: Verify you're using the correct syntax (&#RRGGBB)

### Console Commands

For server admins, NeoEssentials provides commands to test formatting:

```
/neoessentials:format test "Your formatted text here"
/neoessentials:format reload
/neoessentials:format list
```

## Additional Resources

- [NeoEssentials Placeholders](Placeholders)
- [Custom Templates](Custom-Templates)
- [Animation System](Animation-System)
- [NeoEssentials Discord](https://discord.gg/dUGAQF2Mga) for formatting support
