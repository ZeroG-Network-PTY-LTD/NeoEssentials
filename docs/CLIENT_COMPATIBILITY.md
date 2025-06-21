# NeoEssentials Client Compatibility Guide

## Overview

NeoEssentials is primarily a server-side mod, but due to Minecraft's registry synchronization system, there are some considerations for client compatibility. This document explains the options for server owners and players.

## Option 1: Clients Install the Mod (Recommended)

The most seamless experience is when clients also install NeoEssentials. This is because Minecraft requires registry synchronization between client and server for certain elements like command argument types.

**For Server Owners:**
- Install NeoEssentials on your server
- Ask your players to install the same version on their clients
- All features will work perfectly

**For Players:**
- Install NeoEssentials (same version as the server)
- The mod will only enable compatibility - it won't add any client-side features
- Enjoy an error-free connection to servers running NeoEssentials

## Option 2: Server-Only Installation (Advanced)

For server owners who cannot ask all clients to install the mod, we've implemented a robust fallback mechanism for command argument types.

**How it works:**
- The mod now registers command argument types in multiple ways to maximize compatibility
- It registers early in the lifecycle to ensure registry information is available
- It uses both deferred registration and direct registration methods

**Limitations:**
- Some advanced command features might not work for clients without the mod
- In rare cases, clients without the mod might still get disconnected

## Technical Details

The error that occurs without proper synchronization looks like this:
```
Client disconnected with reason: The server send registries with unknown keys: ResourceKey[minecraft:command_argument_type / neoessentials:string_to_boolean]
```

To mitigate this issue, we've implemented multiple layers of registration:
1. Standard DeferredRegister for the argument type registry
2. Early direct registration using ArgumentTypeInfos.registerByClass()
3. Additional registration in the CommonSetup event

This triple registration approach maximizes compatibility across different connection scenarios.

## Recommended Setup

For the best experience, we recommend:

1. Server owners install NeoEssentials
2. Players install the same version of NeoEssentials
3. Configure server to make the mod optional but recommended

## Advanced Configuration

If you need to modify how command argument types are registered, see `ModArgumentTypes.java` for details on the implementation. Do not modify this unless you know what you're doing, as it could break client compatibility.
