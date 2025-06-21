# Client-Server Registry Synchronization in NeoEssentials

## Important Update for Server Administrators

NeoEssentials now requires installation on **both client and server** for full compatibility. While the mod still primarily provides server-side functionality, it needs to be present on clients to ensure proper registry synchronization.

## Changes Made

We've updated NeoEssentials to be a "both-sides" mod instead of a "server-only" mod to address connection issues. The following changes were made:

1. Updated the mod's metadata (`mods.toml`) to:
   - Set `displayTest` to `"MATCH_VERSION"` instead of `"IGNORE_SERVER_VERSION"`
   - Change side designation from `"SERVER"` to `"BOTH"` for NeoForge dependency
   - Update the display name to remove "(Server-Side)" suffix

2. Enhanced initialization to support both environments:
   - Server-side: Full functionality as before
   - Client-side: Minimal functionality, just enough for registry compatibility

3. Improved command argument type registration for client compatibility

## Why This Change Was Necessary

Minecraft's registry synchronization system requires clients to understand all registry entries that a server uses, even if the client doesn't actively use that functionality. This includes:

- Command argument types
- Network message types
- Entity data serializers
- And other synchronized registries

When the server uses a custom command argument type like `neoessentials:string_to_boolean`, clients must have this type registered on their side to understand server communication.

## Error Being Fixed

This change addresses the following disconnection error:
```
Client disconnected with reason: The server send registries with unknown keys: ResourceKey[minecraft:command_argument_type / neoessentials:string_to_boolean]
```

## Installation Instructions

### For Server Administrators
- Continue installing NeoEssentials on your server as before
- Inform your players that they need to install the same version of NeoEssentials on their clients

### For Players
- Install NeoEssentials (same version as the server)
- The mod will not add any noticeable client-side features
- It simply enables compatibility with servers running NeoEssentials

## Technical Details

This is a common pattern in Minecraft modding. Many "server-focused" mods still need to be installed on clients for proper registry synchronization, even if they don't add any visible client-side features.

The mod now follows NeoForge's recommended practice of loading everywhere it finds itself, while conditionally enabling features based on environment.
