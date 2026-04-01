# Chat System

> **Version:** 1.0.2.6 · **Config:** `config.json` → `chat` section

---

## Overview

Full-featured chat system with format templates, rich text (gradients/rainbow), channel routing, Discord relay, mute/ignore, social spy, and per-player time/weather. All chat is logged to the server console.

---

## Config (`config.json` → `chat`)

| Key | Default | Description |
|---|---|---|
| `enabled` | `true` | Enable NeoEssentials chat handling |
| `enable-chat-formatting` | `true` | Apply format templates to messages |
| `chat-format` | `"<{prefix}{name}{suffix}> {message}"` | Default format. Supports placeholders |
| `logChatToConsole` | `true` | Print formatted messages to server console |
| `localChatRadius` | `0` | Block radius for local chat (0 = global) |
| `joinMessage` | `"§e{player} joined the server"` | Join broadcast (blank = disabled) |
| `quitMessage` | `"§e{player} left the server"` | Quit broadcast |
| `richText.enabled` | `true` | Enable gradient/rainbow MiniMessage tags |
| `richText.allowedRoles` | `[]` | Groups allowed to use rich text (empty = all) |

### Chat Format Placeholders

| Placeholder | Value |
|---|---|
| `{prefix}` | Player's permission group prefix |
| `{suffix}` | Player's permission group suffix |
| `{name}` | Player's real username |
| `{displayname}` | Player's nickname or real name |
| `{message}` | The chat message content |
| `{world}` | Current world/dimension name |

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

## Color Codes in `chat-format`

Color codes go in the **value** (the format string), **not** in the key.
The key must remain a plain group identifier such as `group:fondateur`.

### Legacy `&` codes

| Code | Color/Effect |
|---|---|
| `&0` – `&9`, `&a` – `&f` | Standard Minecraft colors |
| `&l` `&m` `&n` `&o` `&k` | Bold / Strike / Underline / Italic / Obfuscated |
| `&r` | Reset all formatting |

### Hex colors

```
&#RRGGBB
```

Example: `&#FF5500` for orange.

### Per-group format example

```json
"chat-format": {
  "default":           "&f[&eDresseur&f]   &f{neoessentials_username}&7: &f{MESSAGE}",
  "group:streameur":   "&f[&bStreameur&f]  &f{neoessentials_username}&7: &f{MESSAGE}",
  "group:moderateur":  "&f[&cModérateur&f] &f{neoessentials_username}&7: &f{MESSAGE}",
  "group:fondateur":   "&f[&4Fondateur&f]  &f{neoessentials_username}&7: &f{MESSAGE}"
}
```

> **Note** – Before v1.0.2.7 there was a bug where `&` color codes in format strings were
> silently stripped when `enableChatEnhancements` was `true` (the default), causing all chat
> text to appear white.  This is fixed in v1.0.2.7.

### Common mistakes to avoid

| ❌ Wrong | ✅ Correct |
|---|---|
| Color code in the **key**: `"group:&cFondateur"` | Keep the key as `"group:fondateur"` |
| Unicode escapes in the key: `"\u0026cgroup:fondateur"` | Color codes belong in the value string |
| Missing reset after colored text | Add `&f` (white) or `&r` (reset) after the group name |

---

## Rich Text

When `richText.enabled` is `true`, players (or players in `allowedRoles`) can use MiniMessage tags in chat:

| Tag | Effect |
|---|---|
| `<gradient:#ff0000:#0000ff>text</gradient>` | Red-to-blue gradient |
| `<rainbow>text</rainbow>` | Rainbow cycling colours |
| `<bold>text</bold>` | Bold |
| `<italic>text</italic>` | Italic |
| `<color:#hexcode>text</color>` | Hex colour |
| `<aqua>text</aqua>` | Named colour |

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

---

*Back to [Wiki Home](Home)*
