# Chat & Messaging System

## Overview
NeoEssentials provides a powerful chat and messaging system with advanced formatting, AFK management, mute/ignore features, and staff tools.

## Core Managers
- **ChatManager**: Handles chat config, formatting, permissions, mute/ignore logic, join/quit messages, AFK, death messages, and more.
- **AfkManager**: Manages AFK status, activity tracking, auto-save, tablist indicators, AFK kick, and broadcasts.

## Configuration Options
- `chat-format`: Global, per-group, per-world chat formats
- `channels`: Local, global, staff (configurable commands, permissions, radius, etc.)
- `muteCommands`: List of muted commands
- `playerChatPermissions`: Permissions required for chat features
- AFK settings: timeout, kick, broadcasts, tablist indicator, activity tracking, excluded commands
- Join/quit/death/custom messages
- `sleepIgnoresAfkPlayers`, `sleepIgnoresVanishedPlayers`: Sleep logic integration
- `vanishingItemsPolicy`, `bindingItemsPolicy`: Item policies
- `hideJoinQuitMessagesAbove`: Hide join/quit messages above player count

## Commands
- `/msg`, `/message`, `/tell`, `/whisper`, `/w` — Private messaging
- `/reply`, `/r` — Reply to last message
- `/ignore`, `/unignore`, `/block` — Ignore/unignore players
- `/mute`, `/unmute`, `/mutelist` — Mute/unmute/list muted players
- `/socialspy` — Spy on private messages
- `/msgtoggle` — Toggle receiving private messages
- `/afk`, `/away` — Toggle AFK status

## Features
- Advanced chat formatting (color codes, per-group/world formats)
- AFK detection and management (auto/manual, kick, broadcasts)
- Mute/ignore system
- SocialSpy for staff
- Custom join/quit/death messages
- Sleep logic integration
- Configurable via JSON and in-game commands

## Example Config
```json
{
  "chat-format": {
    "default": "<{neoessentials_prefix} {neoessentials_username} {neoessentials_suffix}> {MESSAGE}",
    "group:admin": "&c[Admin] {neoessentials_username}: {MESSAGE}",
    "group:mod": "&2[Mod] {neoessentials_username}: {MESSAGE}",
    "world:creative": "&b[C] {neoessentials_username}: {MESSAGE}"
  },
  "channels": {
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
  },
  "muteCommands": ["msg", "tell"],
  "playerChatPermissions": ["neoessentials.chat.use"],
  "sleepIgnoresAfkPlayers": true,
  "sleepIgnoresVanishedPlayers": true,
  "afkListName": "none",
  "broadcastAfkMessage": true,
  "deathMessages": true,
  "vanishingItemsPolicy": "keep",
  "bindingItemsPolicy": "keep",
  "sendInfoAfterDeath": true,
  "allowSilentJoinQuit": false,
  "customJoinMessage": "none",
  "customQuitMessage": "none",
  "customNewUsernameMessage": "none",
  "useCustomServerFullMessage": false,
  "hideJoinQuitMessagesAbove": -1
}
```

---
For more details, see the main documentation or ask in the Discord support server.