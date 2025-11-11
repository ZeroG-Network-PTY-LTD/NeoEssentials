# Moderation System

## Overview
NeoEssentials provides a comprehensive moderation system for managing player behavior, staff actions, and server integrity. It includes vanish, freeze, jail, ban, and mute features with persistent tracking and permission integration.

## Core Managers
- **VanishManager**: Handles staff invisibility, tracks vanished players, manages viewer priorities, integrates with permission/group system.
- **FreezeManager**: Manages player immobilization, tracks frozen players, reasons, positions, and provides persistent storage.
- **JailManager**: Manages player jailing, tracks jailed players, reasons, jail locations, and provides persistent storage.

## Configuration & Data
- Persistent storage in JSON files (e.g., `vanished_players.json`, `frozen_players.json`, `jailed_players.json`, `jail_locations.json`).
- Integration with permission/group system for priority and exemptions.
- Customizable messages, reasons, and tracking for moderation actions.

## Commands
- `/ban`, `/unban`, `/banip`, `/unbanip`, `/banlist` — Ban management
- `/kick`, `/kickall` — Kick management
- `/mute`, `/unmute`, `/mutelist` — Mute management
- `/jail`, `/unjail`, `/setjail`, `/jaillist` — Jail management
- `/freeze`, `/unfreeze`, `/freezeall`, `/unfreezeall`, `/freezelist` — Freeze management
- `/vanish`, `/unvanish`, `/vanishlist`, `/v` — Vanish management

## Features
- Staff vanish/invisibility with priority system
- Player freeze/immobilization with reason and position tracking
- Player jailing with location, reason, and dimension tracking
- Ban and mute management
- Persistent moderation data
- Permission/group integration for exemptions and priority

---
For more details, see the main documentation or ask in the Discord support server.