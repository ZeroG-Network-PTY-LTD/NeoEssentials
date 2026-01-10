# Chat Channels System - Documentation

**Version:** NeoEssentials v1.0.2.4  
**Date:** January 10, 2026

---

## Overview

The Chat Channels system allows players to communicate in different chat channels with different ranges and audiences. Channels can be configured for proximity-based (local), server-wide (global), or permission-based (staff) communication.

---

## Features

- **Local Channel:** Proximity-based chat (default 100 blocks radius)
- **Global Channel:** Server-wide chat for all players
- **Staff Channel:** Permission-restricted chat for staff members
- **Customizable:** Create your own channels via config
- **Prefix Support:** Use prefixes (!, @, etc.) to send messages to specific channels
- **Command Switching:** Use commands to switch your active channel
- **Flexible Configuration:** Enable/disable channels, customize commands, aliases, and settings

---

## Configuration

### Location
`config/neoessentials/config.json` → `chat.channels`

### Master Switch
```json
"channels": {
  "enabled": true
}
```

Set to `false` to disable the entire channel system and use standard global chat.

### Channel Configuration

Each channel has the following settings:

| Setting | Type | Description |
|---------|------|-------------|
| `enabled` | boolean | Enable/disable this channel |
| `command` | string | Main command to switch to this channel (e.g., "l", "g", "staff") |
| `aliases` | array | Alternative commands for this channel |
| `prefix` | string | Optional prefix to send message to this channel without switching |
| `default` | boolean | Set as default channel for new players |
| `permission` | string | (Optional) Required permission to use this channel |
| `radius` | number | (Local only) Chat range in blocks |

### Example Configuration

```json
"channels": {
  "enabled": true,
  "local": {
    "enabled": true,
    "radius": 100,
    "command": "l",
    "aliases": ["local", "lc"],
    "prefix": "",
    "default": true
  },
  "global": {
    "enabled": true,
    "command": "g",
    "aliases": ["global", "gc"],
    "prefix": "!",
    "default": false
  },
  "staff": {
    "enabled": true,
    "command": "staff",
    "aliases": ["mod", "admin", "s"],
    "prefix": "@",
    "permission": "neoessentials.chat.staff",
    "default": false
  }
}
```

---

## Commands

### Switching Channels

| Command | Description | Permission |
|---------|-------------|------------|
| `/l` | Switch to local channel | `neoessentials.chat.channel.local` |
| `/local` | Switch to local channel (alias) | `neoessentials.chat.channel.local` |
| `/g` | Switch to global channel | `neoessentials.chat.channel.global` |
| `/global` | Switch to global channel (alias) | `neoessentials.chat.channel.global` |
| `/staff` | Switch to staff channel | `neoessentials.chat.staff` |
| `/mod` | Switch to staff channel (alias) | `neoessentials.chat.staff` |
| `/admin` | Switch to staff channel (alias) | `neoessentials.chat.staff` |

**Note:** Commands are dynamically generated from config. You can create custom channels with custom commands!

### Sending Messages

#### Option 1: Switch Channel First
```
/g
Now chatting in global channel
Hello everyone!
```

#### Option 2: Use Prefix
```
!Hello everyone!
```
This sends "Hello everyone!" to global channel without switching your active channel.

#### Option 3: Command with Message
```
/g Hello everyone!
```
This switches to global channel AND sends the message in one command.

---

## Channel Types

### Local Channel (Proximity Chat)

**Range:** Configurable radius (default: 100 blocks)  
**Audience:** Players within radius in the same dimension  
**Use Case:** Role-play servers, realistic communication, proximity-based gameplay

**Example:**
```
Player is in local channel (radius: 100 blocks)
"Hey, anyone around?"
→ Only players within 100 blocks will see this message
```

**Configuration:**
```json
"local": {
  "enabled": true,
  "radius": 100,
  "command": "l",
  "aliases": ["local", "lc"],
  "prefix": "",
  "default": true
}
```

### Global Channel (Server-Wide)

**Range:** All players on the server  
**Audience:** Everyone online  
**Use Case:** Announcements, trading, general communication

**Example:**
```
Player switches to global: /g
"Selling diamonds at spawn!"
→ All online players see this message
```

**Configuration:**
```json
"global": {
  "enabled": true,
  "command": "g",
  "aliases": ["global", "gc"],
  "prefix": "!",
  "default": false
}
```

### Staff Channel (Permission-Based)

**Range:** All players with permission  
**Audience:** Staff members only  
**Use Case:** Staff coordination, moderation discussions

**Example:**
```
Staff member uses: @Need help at spawn
→ Only players with neoessentials.chat.staff see this
```

**Configuration:**
```json
"staff": {
  "enabled": true,
  "command": "staff",
  "aliases": ["mod", "admin", "s"],
  "prefix": "@",
  "permission": "neoessentials.chat.staff",
  "default": false
}
```

---

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `neoessentials.chat.channel.local` | Use local chat channel | true |
| `neoessentials.chat.channel.global` | Use global chat channel | true |
| `neoessentials.chat.staff` | Use staff chat channel | false (ops only) |

**Note:** Custom channels can have custom permissions defined in config.

---

## Use Cases & Server Setups

### 1. Proximity-Only Server (Realistic RP)
```json
"channels": {
  "enabled": true,
  "local": {
    "enabled": true,
    "radius": 50,
    "default": true
  },
  "global": {
    "enabled": false
  }
}
```
Players can only chat with nearby players. Perfect for role-play!

### 2. Global-Only Server (Standard Minecraft)
```json
"channels": {
  "enabled": false
}
```
OR
```json
"channels": {
  "enabled": true,
  "local": {
    "enabled": false
  },
  "global": {
    "enabled": true,
    "default": true,
    "prefix": ""
  }
}
```
Standard server-wide chat like vanilla Minecraft.

### 3. Hybrid Server (Both Local and Global)
```json
"channels": {
  "enabled": true,
  "local": {
    "enabled": true,
    "radius": 100,
    "default": true
  },
  "global": {
    "enabled": true,
    "prefix": "!"
  }
}
```
Default is local, but players can use `!` prefix or `/g` for global.

### 4. Role-Play Server with Multiple Channels
```json
"channels": {
  "enabled": true,
  "whisper": {
    "enabled": true,
    "radius": 10,
    "command": "w",
    "prefix": "*",
    "default": false
  },
  "local": {
    "enabled": true,
    "radius": 50,
    "command": "l",
    "default": true
  },
  "shout": {
    "enabled": true,
    "radius": 200,
    "command": "shout",
    "prefix": "!",
    "default": false
  },
  "ooc": {
    "enabled": true,
    "command": "ooc",
    "prefix": "//",
    "permission": "rp.ooc",
    "default": false
  }
}
```

---

## How It Works

### Message Routing

1. **Player sends a chat message**
2. **System checks for prefix:**
   - `!message` → Global channel
   - `@message` → Staff channel
   - No prefix → Continue to step 3
3. **System checks player's active channel:**
   - Player used `/g` earlier → Global channel
   - Player used `/staff` earlier → Staff channel
   - Player never switched → Default channel
4. **Message is sent to channel:**
   - **Local:** Players within radius in same dimension
   - **Global:** All online players
   - **Staff:** Players with permission

### Default Channel

- When a player joins, they are placed in the channel with `"default": true`
- If multiple channels have `default: true`, the first one found is used
- If no channels have `default: true`, global is used as fallback

### Channel Persistence

- Channel selection persists across chat messages
- Channel selection does NOT persist across server restarts (resets to default)
- Future enhancement: Could save to player data for persistence

---

## Troubleshooting

### "Channel commands not working"
- Check `"channels": { "enabled": true }`
- Check individual channel `"enabled": true`
- Run `/neoessentials reload` after config changes

### "Can't use staff channel"
- Verify you have `neoessentials.chat.staff` permission
- Check permission with `/permissions check neoessentials.chat.staff`

### "Messages not reaching players"
- **Local:** Check if players are within radius and same dimension
- **Staff:** Verify all intended recipients have the permission
- **Global:** Check if chat formatting is enabled

### "Custom channel not registering"
- Ensure channel name doesn't conflict with existing command
- Check JSON syntax (trailing commas, quotes, etc.)
- Run `/neoessentials reload` after adding channel

---

## Advanced: Creating Custom Channels

### Example: Trade Channel

```json
"trade": {
  "enabled": true,
  "command": "trade",
  "aliases": ["tc", "market"],
  "prefix": "$",
  "default": false
}
```

**Usage:**
- `/trade` → Switch to trade channel
- `/tc` → Switch to trade channel (alias)
- `$Selling diamonds!` → Send to trade channel without switching

### Example: World-Specific Channel

You can combine channels with world-specific chat formats:

```json
"chat-format": {
  "default": "<{neoessentials_prefix}{neoessentials_username}> {MESSAGE}",
  "world:nether": "&4[Nether] &f{neoessentials_username}: {MESSAGE}",
  "world:end": "&5[End] &f{neoessentials_username}: {MESSAGE}"
}
```

---

## Integration with Other Features

### With LuckPerms
```
/lp group admin permission set neoessentials.chat.staff true
/lp group moderator permission set neoessentials.chat.staff true
```

### With Vanish
- Vanished players still receive all channels
- Vanished players can send messages (will be visible)
- Future: Could add config to prevent vanished players from being seen in any channel

### With Mute
- Muted players cannot send messages in ANY channel
- Mute is checked before channel routing

---

## Future Enhancements

Possible improvements for future versions:

1. **Persistent Channels:** Save player's channel across restarts
2. **Channel Colors:** Different colors for different channels
3. **Channel Spy:** Permission to see all channels (`/channelspy`)
4. **Cooldowns:** Prevent channel hopping spam
5. **Cross-Server:** BungeeCord/Velocity support for multi-server channels
6. **Channel Formatting:** Different chat format per channel
7. **Join/Leave Notifications:** "Player joined local channel"
8. **Channel List:** `/channels` to list all available channels
9. **Channel Info:** `/channel info <channel>` for details
10. **Channel Mute:** Mute specific channels without disabling them

---

## Changelog

**v1.0.2.4 - January 10, 2026**
- ✅ Implemented chat channels system
- ✅ Added dynamic channel command registration from config
- ✅ Added local (proximity), global (server-wide), staff (permission) channels
- ✅ Added prefix support for quick channel access
- ✅ Added command+message combination (`/g Hello!`)
- ✅ Added per-player channel state tracking
- ✅ Fully configurable via config.json

---

**Status:** ✅ Fully Implemented and Tested  
**Compatibility:** NeoForge 21.1.179+ | Minecraft 1.21.1 - 1.21.10

