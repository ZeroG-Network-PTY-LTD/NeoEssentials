# Chat System

> **Version:** 1.0.2.7 · **Config:** `config.json` → `chat` section

---

## Overview

Full-featured chat system with format templates, rich text (gradients/rainbow/hover/click), per-group and per-player formatting, channel routing, Discord relay, mute/ignore, social spy, and per-player time/weather. All chat is logged to the server console.

---

## Config (`config.json` → `chat`)

| Key | Default | Description |
|---|---|---|
| `enabled` | `true` | Enable NeoEssentials chat handling |
| `enable-chat-formatting` | `true` | Apply format templates to messages |
| `chat-format` | `"<{prefix}{name}{suffix}> {message}"` | Default format. Supports placeholders, color codes, and rich text tags |
| `logChatToConsole` | `true` | Print formatted messages to server console |
| `localChatRadius` | `0` | Block radius for local chat (0 = global) |
| `joinMessage` | `"§e{player} joined the server"` | Join broadcast (blank = disabled) |
| `quitMessage` | `"§e{player} left the server"` | Quit broadcast |
| `richText.enabled` | `true` | Enable gradient/rainbow/hover/click MiniMessage tags |
| `richText.allowedRoles` | `[]` | Groups allowed to use rich text in messages (empty = all) |

---

## Chat Format Placeholders

| Placeholder | Value |
|---|---|
| `{prefix}` | Player's permission group prefix |
| `{suffix}` | Player's permission group suffix |
| `{name}` | Player's real username |
| `{displayname}` | Player's nickname or real name |
| `{message}` | The chat message content |
| `{world}` | Current world/dimension name |
| `{neoessentials_prefix}` | Alias for `{prefix}` |
| `{neoessentials_suffix}` | Alias for `{suffix}` |
| `{neoessentials_username}` | Alias for `{name}` |
| `{neoessentials_displayname}` | Alias for `{displayname}` |
| `{MESSAGE}` | Alias for `{message}` (case-insensitive) |

---

## Format Priority

When a chat message is sent, NeoEssentials selects the format using this priority order (highest to lowest):

```
1. Per-player override    (/chatformat set <player> <format>)
2. Per-group + per-world  (key: "group:admin:world:overworld")
3. Per-group              (key: "group:admin")
4. Per-world              (key: "world:overworld")
5. Default format         (key: "default")
```

---

## Color Codes in `chat-format`

Color codes go in the **value** (the format string), **not** in the key.
The key must remain a plain group identifier such as `group:fondateur`.

### Legacy `&` codes

| Code | Color/Effect |
|---|---|
| `&0`–`&9`, `&a`–`&f` | Standard Minecraft colors |
| `&l` `&m` `&n` `&o` `&k` | Bold / Strikethrough / Underline / Italic / Obfuscated |
| `&r` | Reset all formatting |

### Hex colors

```
&#RRGGBB
```

Example: `&#FF5500` for orange, `&#00FFCC` for mint.

### Per-group and per-world format examples

```json
"chat-format": {
  "default":                          "&f[&7Member&f] &f{neoessentials_username}&7: &f{MESSAGE}",
  "group:vip":                        "&f[&#FFD700VIP&f] &f{neoessentials_username}&7: &f{MESSAGE}",
  "group:moderateur":                 "&f[&cModérateur&f] &f{neoessentials_username}&7: &f{MESSAGE}",
  "group:fondateur":                  "&f[&4Fondateur&f] &f{neoessentials_username}&7: &f{MESSAGE}",
  "group:fondateur:world:overworld":  "&f[&4Fondateur&f|&aOverworld&f] &f{neoessentials_username}&7: &f{MESSAGE}",
  "world:the_nether":                 "&f[&6Nether&f] &f{neoessentials_username}&7: &f{MESSAGE}"
}
```

> **Note** – Before v1.0.2.7 there was a bug where `&` color codes in format strings were
> silently stripped when `enableChatEnhancements` was `true` (the default), causing all chat
> text to appear white. This is fixed in v1.0.2.7.

### Common mistakes to avoid

| ❌ Wrong | ✅ Correct |
|---|---|
| Color code in the **key**: `"group:&cFondateur"` | Keep the key as `"group:fondateur"` |
| Unicode escapes in the key: `"\u0026cgroup:fondateur"` | Color codes belong in the value string |
| Missing reset after colored text | Add `&f` (white) or `&r` (reset) after the group name |

---

## Per-Player Format Overrides

Admins can assign a completely custom chat format to any individual player. The per-player format takes the **highest priority** and overrides all group and world formats for that player.

### Commands

| Command | Permission | Description |
|---|---|---|
| `/chatformat set <player> <format>` | `neoessentials.chat.format.set` | Assign a custom format to a player |
| `/chatformat clear <player>` | `neoessentials.chat.format.set` | Remove the custom format (reverts to group/default) |
| `/chatformat check <player>` | `neoessentials.chat.format.set` | Show the active per-player override |
| `/chatformat list` | `neoessentials.chat.format.set` | List all currently active per-player overrides |
| `/chatformat reload` | `neoessentials.chat.format.set` | Reload per-player formats from disk |

### Examples

```
/chatformat set Notch &f[&#FFD700Owner&f] &6{neoessentials_username}&7: &f{MESSAGE}
/chatformat set Steve <gradient:ff0000-0000ff>{neoessentials_username}</gradient>&7: &f{MESSAGE}
/chatformat clear Notch
```

### Persistence

Per-player formats are saved to `config/neoessentials/player_chat_formats.json` and survive server restarts.

---

## Rich Text

When `richText.enabled` is `true`, the format string (and messages by players with the appropriate permission) can use advanced text effects.

### Gradients

```
<gradient:RRGGBB-RRGGBB>text</gradient>
```

| Example | Effect |
|---|---|
| `<gradient:ff0000-0000ff>text</gradient>` | Red → blue gradient |
| `<gradient:FFD700-FF8C00>VIP</gradient>` | Gold → dark-orange gradient |
| `<gradient:00c6ff-0072ff>text</gradient>` | Sky-blue gradient |

Gradients work in both format templates and in player messages (if the player has the `neoessentials.chat.richtext` permission).

**Format template example with gradient prefix:**
```json
"group:vip": "<gradient:FFD700-FF8C00>[VIP]</gradient> &f{neoessentials_username}&7: &f{MESSAGE}"
```

### Rainbow

```
<rainbow>text</rainbow>
```

Applies a cycling rainbow colour to each character.

### Hex colour span

```
<color:#RRGGBB>text</color>
```

Example: `<color:#00FFCC>Hello world</color>`

### Named colours

```
<red>text</red>    <gold>text</gold>    <aqua>text</aqua>    <white>text</white>
<dark_red>text</dark_red>   <dark_blue>text</dark_blue>   <yellow>text</yellow>
```

### Format tags

```
<bold>text</bold>               <italic>text</italic>
<underline>text</underline>     <strikethrough>text</strikethrough>
<obfuscated>text</obfuscated>
```

### Hover events

Show a tooltip when a player hovers their cursor over part of the message:

```
<hover:HOVER_TEXT>VISIBLE_TEXT</hover>
```

**Examples:**
```
<hover:Click to join our Discord!>[Discord]</hover>
<hover:This player is a donator!><gradient:FFD700-FF8C00>[VIP]</gradient></hover>
```

> **Tip** – Hover text is plain text only (no color codes inside the hover tooltip value).

### Click events

Make text clickable in chat:

```
<click:ACTION:VALUE>VISIBLE_TEXT</click>
```

| Action | Effect |
|---|---|
| `suggest_command` | Populates the player's chat bar with a command (does not run it) |
| `run_command` | Executes a command when clicked |
| `open_url` | Opens a URL in the player's browser |
| `copy_to_clipboard` | Copies the value to clipboard |

**Examples:**
```
<click:open_url:https://discord.gg/myserver>[Discord]</click>
<click:suggest_command:/help>[Help]</click>
<click:run_command:/spawn>[Spawn]</click>
```

### Combining hover + click

```
<hover:Visit our website!><click:open_url:https://example.com>[Website]</click></hover>
```

### Full format template examples

**VIP group with gradient prefix and hover tooltip:**
```json
"group:vip": "<hover:VIP Member><gradient:FFD700-FF8C00>[VIP]</gradient></hover> &f{neoessentials_username}&7: &f{MESSAGE}"
```

**Admin group with clickable rank badge:**
```json
"group:admin": "<hover:Server Administrator><click:suggest_command:/list>[&cAdmin&r]</click></hover> &f{neoessentials_username}&7: &f{MESSAGE}"
```

**Founder with rainbow name:**
```json
"group:fondateur": "[&4Fondateur&r] <rainbow>{neoessentials_username}</rainbow>&7: &f{MESSAGE}"
```

**Hex per-player format (set via /chatformat set):**
```
[&#FF5500Custom&r] &#FFD700{neoessentials_username}&7: &f{MESSAGE}
```

---

## Player Message Colour Permissions

Players must have the appropriate permissions to use color/formatting in their own chat *messages*:

| Permission | Effect |
|---|---|
| `neoessentials.chat.color` | Allow `&`-color codes in messages |
| `neoessentials.chat.color.hex` | Allow `&#RRGGBB` hex colors in messages |
| `neoessentials.chat.format` | Allow `&`-format codes (bold, italic, etc.) in messages |
| `neoessentials.chat.richtext` | Allow rich text tags (gradient, rainbow, hover, click) in messages |

> Format strings set by admins (in `config.json` or via `/chatformat set`) are **not** subject to these restrictions — they always render fully.

---

## Commands

### Private Messaging

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/msg` | `/msg <player> <message>` | `neoessentials.chat.msg` | Send a private message |
| `/tell`, `/whisper`, `/w`, `/m` | aliases | same | Aliases |
| `/reply` | `/reply <message>` | `neoessentials.chat.reply` | Reply to last private message |
| `/r` | alias | same | Alias |
| `/msgtoggle` | `/msgtoggle [on\|off]` | `neoessentials.msgtoggle` | Toggle receiving private messages |
| `/rtoggle` | `/rtoggle [on\|off]` | `neoessentials.rtoggle` | Toggle receiving replies |
| `/socialspy` | `/socialspy [on\|off]` | `neoessentials.chat.socialspy` | Spy on all private messages |

### Ignore System

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/ignore` | `/ignore <player>` | `neoessentials.chat.ignore` | Ignore a player's messages |
| `/unignore` | `/unignore <player>` | `neoessentials.chat.ignore` | Unignore a player |
| `/ignorelist` | `/ignorelist` | `neoessentials.chat.ignore` | List ignored players |

---

## Discord Integration (Simple Discord Link)

When **Simple Discord Link** is installed, NeoEssentials automatically:
- Relays Minecraft chat → Discord channel (configurable `channelId`)
- Relays Discord messages → Minecraft chat
- Formats messages using the configured Discord chat format

Config (`config.json` → `discord` section per channel):

| Key | Description |
|---|---|
| `channelId` | Discord channel ID to relay to/from |
| `relayToDiscord` | Send MC chat to Discord |
| `relayFromDiscord` | Send Discord messages to MC |
| `format` | Message format for Discord → MC relay |

Works standalone (no relay) if Simple Discord Link is not installed.

---

## Data Files

| File | Contents |
|---|---|
| `neoessentials/ignore_data.json` | Per-player ignore lists |
| `neoessentials/muted_players.json` | Active mutes |
| `config/neoessentials/player_chat_formats.json` | Per-player format overrides |

---

*Back to [Wiki Home](Home)*
