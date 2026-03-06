# Chat Channels

> **Version:** 1.0.2.6 · **Config:** `config.json` → `chat.channels` section

---

## Overview

Chat channels allow messages to be scoped to a specific audience — local proximity, a permission group, global, or staff only. Each channel is configured independently with its own command, aliases, radius, format, and Discord relay settings.

---

## Built-in Channel Types

| Type | Description |
|---|---|
| `global` | All players on the server |
| `local` | Players within a configurable block radius |
| `staff` | Players with a specific permission node |
| `permission` | Players who hold a specified permission |

---

## Config (`config.json` → `chat.channels`)

Each channel entry supports:

| Key | Description |
|---|---|
| `enabled` | Enable/disable this channel |
| `command` | Primary command to switch to or speak in this channel |
| `aliases` | List of aliases for the command |
| `type` | Channel type: `global`, `local`, `staff`, `permission` |
| `radius` | Block radius for `local` type (ignored for others) |
| `permission` | Required permission to use this channel |
| `prefix` | Prefix prepended to messages in this channel |
| `format` | Message format (supports `{player}`, `{prefix}`, `{message}`) |
| `default` | If `true`, all normal chat goes through this channel |
| `discord.enabled` | Relay this channel to Discord |
| `discord.channelId` | Discord channel ID to relay to |
| `discord.relayFromDiscord` | Pull Discord messages into this channel |

---

## Example Config

```json
"channels": {
  "global": {
    "enabled": true,
    "command": "g",
    "aliases": ["global"],
    "type": "global",
    "prefix": "§8[§7G§8] ",
    "default": true,
    "discord": {
      "enabled": true,
      "channelId": "123456789",
      "relayFromDiscord": true
    }
  },
  "local": {
    "enabled": true,
    "command": "l",
    "aliases": ["local", "lc"],
    "type": "local",
    "radius": 100,
    "prefix": "§8[§aL§8] "
  },
  "staff": {
    "enabled": true,
    "command": "sc",
    "aliases": ["staffchat", "s"],
    "type": "staff",
    "permission": "neoessentials.chat.staff",
    "prefix": "§8[§cSTAFF§8] "
  }
}
```

---

## Permissions

| Node | Description |
|---|---|
| `neoessentials.chat.channel.<name>` | Access a specific channel |
| `neoessentials.chat.staff` | Access the staff channel |
| `neoessentials.chat.bypass` | Bypass channel restrictions |

---

*Back to [Wiki Home](Home)*
