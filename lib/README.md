# NeoEssentials Library Directory

This directory contains libraries and resources for various integrations with NeoEssentials.

## Current Integrations

- [ForgePerms](./forgeperms/README.md) - Integration with the ForgePerms permission system

## Integration Strategy

NeoEssentials uses a flexible integration strategy that allows it to work with various permission and utility mods without requiring them as hard dependencies. This is achieved through:

1. **Reflection-based detection** - Checking if specific classes are available on the classpath
2. **Graceful fallbacks** - Using built-in systems when external mods are not available
3. **Priority-based handling** - Trying multiple systems in sequence based on preference

This approach ensures that NeoEssentials works well in a variety of modpack configurations while still taking advantage of popular utility mods when available.
