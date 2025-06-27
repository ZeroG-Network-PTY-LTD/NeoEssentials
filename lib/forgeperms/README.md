# ForgePerms Integration for NeoEssentials

This directory is for ForgePerms library files. NeoEssentials uses reflection to access ForgePerms functionality without requiring a direct dependency.

## ForgePerms Integration

NeoEssentials now supports ForgePerms for permission management. This is implemented through the `ForgePermsPermissionHandler` class that uses reflection to check permissions via ForgePerms.

### How it Works

The integration follows these steps:

1. Detects if ForgePerms is available on the classpath
2. If available, sets up reflection-based access to the ForgePerms API
3. Checks permissions using the `canAccess` method from ForgePerms

### Required Classes

- `com.sperion.forgeperms.ForgePerms` - Used to get the permission handler
- `com.sperion.forgeperms.PermissionsBase` - The base class for permission managers in ForgePerms

### Permission Hierarchy

When checking permissions, NeoEssentials will now try:

1. LuckPerms (if available)
2. ForgePerms (if available)
3. NeoEssentials built-in permission system

This allows for flexibility in permission management while maintaining compatibility with popular permission systems.

## Installing ForgePerms

To use ForgePerms with NeoEssentials:

1. Install ForgePerms in your Minecraft server
2. No additional configuration is needed in NeoEssentials - it will automatically detect and use ForgePerms

## References

For more information on ForgePerms, visit the ForgePerms documentation.
