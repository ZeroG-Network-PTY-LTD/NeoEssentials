# AFK System

> **Version:** 1.0.2.6 · **Config:** `config.json` → `afk` section

---

## Overview

The AFK system automatically marks players as AFK after a configurable period of inactivity, broadcasts status changes, shows an indicator in the tablist, and optionally kicks long-term AFK players.

---

## Config (`config.json` → `afk`)

| Key | Default | Description |
|---|---|---|
| `enabled` | `true` | Enable/disable the AFK system |
| `timeout` | `300` | Seconds of inactivity before a player is marked AFK |
| `kickTimeout` | `0` | Seconds after going AFK before being kicked (0 = disabled). Setting this to any value `> 0` automatically enables AFK kicking. |
| `kickAfkPlayers` | *(auto)* | Optional explicit override. Omit this key (recommended) and let `kickTimeout > 0` enable kicking. Set to `false` to force-disable kicking even if `kickTimeout > 0`. |
| `afkkickMessage` | `"Kicked for being AFK too long"` | Message shown on AFK kick |
| `enableafkBroadcasts` | `true` | Broadcast AFK status changes to all players |
| `broadcastOnAfk` | `true` | Broadcast when a player goes AFK |
| `broadcastOnReturn` | `true` | Broadcast when a player returns from AFK |
| `afkMessage` | `"{player} is now AFK"` | Broadcast when going AFK (`{player}` placeholder) |
| `returnMessage` | `"{player} is no longer AFK"` | Broadcast on return |
| `enableTablistIndicator` | `true` | Show AFK indicator in tablist |
| `tablistAfkPrefix` | `"[AFK] "` | Prefix added to tablist name for AFK players |
| `tablistAfkSuffix` | `""` | Suffix added to tablist name for AFK players |
| `ignoreAfkInSleep` | `true` | AFK players do not count for sleep percentage |
| `enableActivityTracking` | `true` | Track player activity to detect inactivity |
| `trackMovement` | `true` | Player movement resets AFK timer |
| `trackChat` | `true` | Chat messages reset AFK timer |
| `trackCommands` | `true` | Commands reset AFK timer |
| `trackInteractions` | `true` | Block/entity interactions reset AFK timer |
| `movementThreshold` | `0.1` | Minimum movement distance to count as activity |
| `rotationThreshold` | `5.0` | Minimum look-rotation change to count as activity |
| `excludedCommands` | `["afk","list","who","tps","ping","help","?"]` | Commands that do NOT reset the AFK timer |
| `invulnerableWhenAfk` | `false` | Make AFK players immune to damage while AFK |
| `autoSave` | `true` | Periodically save AFK state to disk |
| `saveInterval` | `60` | Auto-save interval in seconds |

`enableActivityTracking` is a master switch — set it to `false` and none of `trackMovement`/
`trackChat`/`trackCommands`/`trackInteractions` reset the AFK timer regardless of their own value.
Each of the four sub-toggles independently gates its own activity source (movement ticks, chat
messages, command execution, block/item interactions). A genuine, non-muted, non-frozen chat
message now resets the AFK timer when `trackChat` is enabled (previously chat never reset the
timer at all, regardless of this setting — fixed alongside the rest of this audit).

---

## Commands

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/afk` | `/afk [message]` | `neoessentials.afk` | Toggle your AFK status manually, optionally with a custom reason shown in the broadcast |
| `/away` | alias | same | Alias |

---

## Permissions

| Node | Default | Description |
|---|---|---|
| `neoessentials.afk` | ✅ | Use `/afk` to manually toggle AFK |
| `neoessentials.afk.exempt` | 🔒 | Exempt from AFK kick timer |

---

## How It Works

1. **Activity detection** — `AfkActivityHandler` listens for movement, chat, commands, and interactions. Each event that passes the threshold resets the player's inactivity timer.
2. **AFK trigger** — After `timeout` seconds of no qualifying activity, `AfkManager` marks the player as AFK, broadcasts the message, and updates the tablist.
3. **Return** — Any qualifying activity while AFK removes the AFK flag and broadcasts the return message.
4. **Kick** — If `kickTimeout > 0`, a player who remains AFK longer than that value is kicked with `afkkickMessage`.
5. **Sleep** — With `ignoreAfkInSleep: true`, AFK players are excluded from the sleep count so the night can be skipped without them.
6. **Invulnerability** — With `invulnerableWhenAfk: true`, players take no damage while AFK.

---

## Anti-Spam Filter

The activity tracker (`AfkActivityHandler`, covering block/item right-click, left-click-attack, and item-toss events) has a built-in repetitive-action filter. If the same action type occurs more than 30 times within a rolling 60-second window, the suspicion score increases by 10 per occurrence over that threshold; once the score exceeds 300 that action stops resetting the AFK timer, preventing AFK farms via automated clicking. The score decays by 5 the next time that action type recurs after a 5-minute gap (it does not decay purely from the 60-second window elapsing).

---

*Back to [Wiki Home](Home)*
