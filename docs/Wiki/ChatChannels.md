# Chat Channels

> **Version:** 1.0.2.6 · **Config:** `config.json` → `chat.channels` section

---

## Overview

Chat channels allow messages to be scoped to a specific audience — local proximity, a permission-gated group, or global. Each channel is a freeform entry under `chat.channels` with its own command, aliases, and behavior; there is **no explicit "type" field** — a channel's behavior is inferred purely from which optional keys it defines:

- Has a `radius` key → proximity-based (only players within that many blocks, same dimension, receive the message)
- Has a `permission` key (and no `radius`) → permission-gated (only players holding that permission receive the message, and it can also gate a Discord relay)
- Neither key → treated as a plain global broadcast channel

---

## Config (`config.json` → `chat.channels`)

`chat.channels.enabled` is the master on/off switch for the whole channel system (defaults to `true`; if `false`, no channel commands are registered and chat falls back to plain vanilla formatting).

Each channel entry (keyed by an arbitrary name, e.g. `"local"`, `"global"`, `"staff"`) supports:

| Key | Description |
|---|---|
| `enabled` | Enable/disable this channel |
| `command` | Primary command to switch to (and optionally speak directly in) this channel |
| `aliases` | List of additional command aliases |
| `radius` | If present, makes this a proximity/local channel (block radius, same dimension) |
| `permission` | If present, only players with this permission receive messages sent to the channel; also gates the Discord relay for this channel |
| `prefix` | If a chat message starts with this literal prefix, it's routed to this channel for that one message (e.g. typing `!hello` in the default config routes to `global`) |
| `default` | If `true`, this channel is used when a player hasn't explicitly switched channels and their message has no matching prefix |
| `discord.enabled` | Relay messages sent in this channel to Discord |
| `discord.channelId` | Discord channel ID to relay to (blank = default/fallback Discord channel) |

There is no `type` or `format` key, and no `discord.relayFromDiscord` — the current implementation only relays Minecraft chat **to** Discord, not the reverse.

Switching channels with `/<command>` persists until changed again (`ChatHandler.setPlayerChannel`); running `/<command> <message>` switches and sends that one message immediately.

---

## Example Config

This is (trimmed from) the actual shipped default in `config.json`:

```json
"channels": {
  "enabled": true,
  "local": {
    "enabled": true,
    "radius": 100,
    "command": "l",
    "aliases": ["local", "lc"],
    "prefix": "",
    "default": true,
    "discord": { "enabled": false, "channelId": "" }
  },
  "global": {
    "enabled": true,
    "command": "g",
    "aliases": ["global", "gc"],
    "prefix": "!",
    "default": false,
    "discord": { "enabled": true, "channelId": "" }
  },
  "staff": {
    "enabled": true,
    "command": "staff",
    "aliases": ["mod", "admin", "s"],
    "prefix": "@",
    "permission": "neoessentials.chat.staff",
    "default": false,
    "discord": { "enabled": true, "channelId": "" }
  }
}
```

With this config, typing `@hi` sends `hi` to the staff channel for that message only, `!hi` sends it to global, and plain unprefixed chat goes to `local` (the `default: true` channel) unless the player has run `/staff` or `/g` to switch persistently.

---

## Permissions

| Node | Description |
|---|---|
| `neoessentials.chat.staff` | Used by the shipped default config to gate the `staff` channel (channel permissions are arbitrary strings you set per-channel in config — this is just the convention the default config uses) |
| `neoessentials.chat.channel.local`, `neoessentials.chat.channel.global` | Registered permission-registry entries for the built-in `local`/`global` channels (informational/tab-completion; the `local` and `global` channels have no `permission` key by default, so these aren't actually enforced out of the box) |

There is no generic `neoessentials.chat.bypass` permission — access to a channel is controlled solely by whether that channel's config defines a `permission` key.

---

*Back to [Wiki Home](Home)*
