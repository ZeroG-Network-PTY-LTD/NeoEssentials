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
| `discord.channelId` | Discord channel ID to relay to (blank = the Discord bridge mod's own default/fallback chat channel). When set, the message is sent as a plain `PlayerName: message` line directly to that channel — it does **not** get the sender-avatar/embed styling a blank `channelId` gets, since posting to an arbitrary channel by ID and posting via the bridge mod's own styled "chat message" type are two different capabilities the bridge mod's API exposes. See [Discord Interoperability](#discord-interoperability-avoiding-duplicate--leaked-messages) below before relying on this for a channel meant to be private. |

There is no `type` or `format` key, and no `discord.relayFromDiscord` — the current implementation only relays Minecraft chat **to** Discord, not the reverse.

Switching channels with `/<command>` persists until changed again (`ChatHandler.setPlayerChannel`); running `/<command> <message>` switches and sends that one message immediately.

Use `{channel}` / `{neoessentials_channel}` in a `chat-format` template to show which channel a
message was sent in — see [Chat Format Placeholders](ChatSystem#chat-format-placeholders).

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

## Discord Interoperability (Avoiding Duplicate / Leaked Messages)

If you're seeing chat messages posted to Discord **twice**, or a channel you configured as
private (e.g. `staff`) still showing up in your server's default/public Discord channel, this is
almost always caused by running NeoEssentials' own per-channel Discord relay **at the same time
as** the Discord bridge mod's (Simple Discord Link, Mc2Discord, DCIntegration) own independent,
built-in "relay every chat message" feature — two completely separate systems both deciding to
post the same message.

**For Simple Discord Link specifically:** its own `config/simple-discord-link/simple-discord-link.toml`
has a `[chat] playerMessages` setting (`true` by default) that relays **every** Minecraft chat
message to its own single configured `chatChannelID`, completely independent of and unaware of
NeoEssentials' `chat.channels.*.discord` settings. With both active:
- Any channel using `discord.channelId: ""` (the default/fallback case — this is `global`'s and
  `staff`'s shipped default) gets posted to Discord twice: once by NeoEssentials' own relay, once
  by SDLink's native relay.
- A channel you intend to keep private, like `staff`, still gets relayed **in full** by SDLink's
  native relay regardless of whatever NeoEssentials' own settings say — SDLink's blanket relay has
  no concept of NeoEssentials' channels at all, so there is nothing NeoEssentials can configure to
  stop it from its side.

**Fix:** set `playerMessages = false` under `[chat]` in SDLink's own config file and restart, so
NeoEssentials' own per-channel relay is the *only* one active. NeoEssentials logs a startup
warning if it detects `playerMessages = true` while SDLink is loaded, specifically to catch this.

If you configure a real `discord.channelId` for a channel (e.g. a dedicated private staff
channel), NeoEssentials will route that specific message there correctly — this was a genuine bug
prior to the fix that shipped alongside this documentation, where a configured `channelId` was
silently ignored and every relayed message always went to SDLink's own default channel regardless
of what `channelId` said. That part is now fixed; the "SDLink's own native relay still leaks it
elsewhere" part above is a separate, unavoidable interoperability issue that only the
`playerMessages = false` fix resolves.

**Mc2Discord** has the same class of channel-routing fix applied (a configured `discord.channelId`
is now honored instead of silently ignored). Mc2Discord's own core purpose is also "relay chat to
Discord automatically," so the same double-post risk for a channel using a blank `channelId`
almost certainly applies — check Mc2Discord's own config for a way to disable its automatic relay
for channels NeoEssentials already handles explicitly, the same way `playerMessages = false` does
for SDLink. NeoEssentials does not currently ship a startup diagnostic for Mc2Discord's config the
way it does for SDLink's.

**DCIntegration** works differently: it relays chat entirely through its own vanilla-level mixins,
with no supported hook for NeoEssentials to intercept or suppress. Because of that,
`onPlayerChat` only ever acts for a DCIntegration setup when a channel has a specific
`discord.channelId` configured — a channel left blank gets **no** explicit relay from
NeoEssentials at all (trusting DCIntegration's own native relay to handle it, avoiding a
duplicate). This means a channel with a real `channelId` set (e.g. a private staff channel) is
correctly posted there by NeoEssentials, but DCIntegration's own native relay may *also* still
post that same message to its own configured channel (`general.botChannel` /
`advanced.chatOutputChannelID`) regardless, since it has no concept of NeoEssentials channels
either — check DCIntegration's own config if you see a private channel still leaking elsewhere.

> Mc2Discord's and DCIntegration's channel-routing fixes are based on reading their compiled
> public API directly, not live-tested against a running instance of either mod (unlike SDLink,
> which was verified live) — if you hit anything unexpected with either, please report it.

---

## Permissions

| Node | Description |
|---|---|
| `neoessentials.chat.staff` | Used by the shipped default config to gate the `staff` channel (channel permissions are arbitrary strings you set per-channel in config — this is just the convention the default config uses) |
| `neoessentials.chat.channel.local`, `neoessentials.chat.channel.global` | Registered permission-registry entries for the built-in `local`/`global` channels (informational/tab-completion; the `local` and `global` channels have no `permission` key by default, so these aren't actually enforced out of the box) |

There is no generic `neoessentials.chat.bypass` permission — access to a channel is controlled solely by whether that channel's config defines a `permission` key.

---

*Back to [Wiki Home](Home)*
