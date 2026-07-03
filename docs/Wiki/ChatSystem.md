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
| `customJoinMessage` | `"none"` | Custom join broadcast (placeholders supported via PlaceholderAPI). `"none"` = use vanilla join message |
| `customQuitMessage` | `"none"` | Custom quit broadcast. `"none"` = use vanilla quit message |
| `channels` | *(object)* | Per-channel definitions — see [Chat Channels](#chat-channels) below |
| `richText.enabled` | `true` | Enable gradient/rainbow/hover/click tags |

> There is no global `localChatRadius`/`joinMessage`/`quitMessage` key — proximity-based chat is
> configured per-channel (see below), and join/quit broadcasts are controlled by
> `customJoinMessage` / `customQuitMessage`.

---

## Chat Channels

Channels are defined under `config.json` → `chat.channels` and are resolved (in order) by
explicit prefix, per-player channel state (set via channel-switch commands), then the channel
flagged `"default": true`, falling back to `"global"` if none matches.

```json
"channels": {
  "enabled": true,
  "global": {
    "enabled": true,
    "default": true
  },
  "local": {
    "enabled": true,
    "prefix": "!",
    "radius": 100
  },
  "staff": {
    "enabled": true,
    "prefix": "@",
    "permission": "neoessentials.chat.staff",
    "discord": {
      "enabled": true,
      "channelId": "123456789012345678"
    }
  }
}
```

| Key (per channel) | Description |
|---|---|
| `enabled` | Enable this channel |
| `prefix` | Message prefix that switches to this channel for a single message (e.g. `!hello`) |
| `default` | Marks the channel used when the player has no explicit channel and typed no prefix |
| `radius` | If set, makes the channel proximity-based (blocks); only players within `radius` blocks in the same dimension receive the message |
| `permission` | If set, only players holding this permission receive the message (and it gates Discord relay for the channel) |
| `discord.enabled` / `discord.channelId` | Relay this channel's messages to a specific Discord channel (see [Discord Integration](#discord-integration-simple-discord-link)) |

A channel with neither `radius` nor `permission` behaves as global chat. `chat.channels.enabled: false` disables the whole channel system (falls back to plain global chat).

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

Gradients work in both format templates and in player messages (if the player has the `neoessentials.chat.namedcolors` permission — see [Player Message Colour Permissions](#player-message-colour-permissions)).

**Format template example with gradient prefix:**
```json
"group:vip": "<gradient:FFD700-FF8C00>[VIP]</gradient> &f{neoessentials_username}&7: &f{MESSAGE}"
```

> **Always close your `<gradient:...>` tags.** An unclosed `<gradient:...>` (no matching
> `</gradient>`) is treated as "gradient the rest of the line" — everything after the tag,
> including any `&`-color codes you meant to reset back to normal color (e.g. `&r`, `&8`), gets
> swallowed into the gradient region. Single legacy `&`-codes inside that region are passed
> through atomically rather than being shredded character-by-character, so this degrades
> gracefully, but closing the tag explicitly is still the clearest way to control exactly where
> a gradient starts and stops:
> ```
> <gradient:00FFC8-0080FF>&lGradiented Text</gradient>&r &8| &enormal text again
> ```

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
| `neoessentials.chat.namedcolors` | Allow `<tag>`-style rich text (named colors, gradient, rainbow, hover, click) in messages |

> Format strings set by admins (in `config.json` or via `/chatformat set`) are **not** subject to these restrictions — they always render fully.
>
> `neoessentials.chat.richtext`, `neoessentials.chat.gradient`, and `neoessentials.chat.rainbow` are
> registered permission nodes (visible in `/permissions search`) but are not currently consulted
> anywhere in the chat pipeline — `neoessentials.chat.namedcolors` is the node that actually gates
> `<gradient>`/`<rainbow>`/`<hover>`/`<click>`/named-color tags in a player's own chat message.

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

> A player holding `neoessentials.chat.ignore.exempt` cannot be ignored. A player holding
> `neoessentials.chat.mute.exempt` cannot be muted with `/mute`.

---

## Discord Integration (Simple Discord Link)

When **Simple Discord Link** is installed, NeoEssentials relays chat to/from Discord **per
channel**, using the `discord` object nested inside that channel's entry under
`chat.channels` (see [Chat Channels](#chat-channels)):

```json
"channels": {
  "global": {
    "enabled": true,
    "default": true,
    "discord": {
      "enabled": true,
      "channelId": "123456789012345678"
    }
  }
}
```

| Key | Description |
|---|---|
| `discord.enabled` | Relay this channel's Minecraft chat to the given Discord channel |
| `discord.channelId` | Discord channel ID to relay to |

There is no separate top-level `discord` config section — relay settings live under each
channel. If a channel has a `permission` requirement, players without it are excluded from the
Discord relay as well as in-game delivery.

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
