# NeoEssentials Home and Warp System

This document explains how the Home and Warp systems work in NeoEssentials.

## Home System

The home system allows players to set and teleport to personal locations on the server.

### Home Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/home [name]` | `neoessentials.command.home` | Teleport to a home. If no name is specified, teleports to the default home. |
| `/sethome [name]` | `neoessentials.command.sethome` | Sets a home at your current location. If no name is specified, sets the default home. |
| `/delhome <name>` | `neoessentials.command.delhome` | Deletes a home. |
| `/homes` | `neoessentials.command.homes` | Lists all your homes with interactive teleport options. |
| `/homehelp` | `neoessentials.command.homehelp` | Shows help for all home commands. |

### Features

1. **Interactive Home List**
   - Shows all homes with locations
   - Clickable home names for easy teleportation
   - Hover information showing dimension and coordinates

2. **Multiple Homes**
   - Players can have multiple named homes
   - Default "home" location when no name is specified
   - Homes are stored per-player and persist across sessions

3. **Cross-dimensional Support**
   - Homes work across all dimensions
   - Dimension information is displayed in hover text

## Warp System

The warp system allows server administrators to create server-wide teleport locations that any player can use (with appropriate permissions).

### Warp Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/warp <name>` | `neoessentials.command.warp` | Teleport to a warp location. |
| `/warps` | `neoessentials.command.warp.list` | Lists all available warps with interactive teleport options. |
| `/setwarp <name>` | `neoessentials.command.warp.set` | Sets a warp at your current location. |
| `/delwarp <name>` | `neoessentials.command.warp.delete` | Deletes a warp location. |
| `/warpplayer <player> <warp>` | `neoessentials.command.warp.player` | Teleports another player to a warp location. |
| `/warphelp` | `neoessentials.command.warp.help` | Shows help for all warp commands. |

### Features

1. **Interactive Warp List**
   - Shows all available warps
   - Clickable warp names for easy teleportation
   - Hover information showing dimension and coordinates

2. **Permission System**
   - Server-wide warps managed by administrators
   - Individual warp permissions can be set
   - Only players with appropriate permissions can create/delete warps

3. **Cross-dimensional Support**
   - Warps work across all dimensions
   - Dimension information is displayed in hover text

## Implementation Details

### Storage

Both home and warp data are stored using the NeoEssentials storage system:

- **Home data**: Stored per player, with UUID as the key
- **Warp data**: Stored server-wide in a single collection

### Data Structure

- **HomeData**: Contains position (x, y, z), rotation (pitch, yaw), and dimension information
- **WarpData**: Contains name, position (x, y, z), rotation (pitch, yaw), dimension, and optional permission information

### User Interface

Both systems use interactive chat components with:
- Color-coding for better readability
- Hover text showing additional information
- Click events for executing commands
- Help commands with examples
