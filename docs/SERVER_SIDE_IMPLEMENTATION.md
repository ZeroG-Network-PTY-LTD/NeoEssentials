# Server-Only Implementation Guide for Modded Environments

## Overview

NeoEssentials has been optimized to work as a true server-side mod in a modded NeoForge environment. This means clients with other mods can connect to the server without needing to install NeoEssentials, even though the server has it installed.

## Implementation Details

We've made significant changes to make NeoEssentials function as a true server-side mod:

<<<<<<< HEAD
### 1. Eliminated Custom Command Argument Types

- Completely removed all custom command argument type registration
- Created `VanillaBooleanParser` that uses standard `StringArgumentType` with post-processing
- Added suggestion providers for better user experience
- Command parsing now happens after execution using vanilla types
- No registry entries are created or synchronized with clients
=======
### 1. Modified Registration System

- Command argument types are now only registered on the server side
- The mod explicitly checks for the dedicated server environment before registering critical components
- Server-side registrations are isolated to prevent client synchronization issues
>>>>>>> 7e60483 (feat: Optimize NeoEssentials for server-only functionality and update documentation)

### 2. Updated `mods.toml` Configuration

- Set `displayTest` to `IGNORE_SERVER_VERSION` to indicate clients don't need the mod
- Changed dependency sides from `BOTH` to `SERVER` to specify server-only operation
- Updated documentation to reflect the server-only approach

### 3. Enhanced Network Handling

- Created a dedicated NetworkHandler to isolate server functionality
- Implemented server-side initialization that doesn't require client components
- Added robust error handling for modded environment compatibility

<<<<<<< HEAD
### 4. Environment-Specific Code

- Added explicit environment checks with `FMLEnvironment.dist.isDedicatedServer()`
- Only loads server-specific components when on dedicated server
- Prevents unnecessary operations on integrated server or client

## Technical Implementation

### VanillaBooleanParser Approach

Instead of using custom command argument types that require registration, we now:

1. Use vanilla `StringArgumentType.word()` for command arguments
2. Add suggestion provider with `SharedSuggestionProvider.suggest(BOOL_OPTIONS, builder)`
3. Parse the string value after command execution with `VanillaBooleanParser.getBoolean()`
4. Convert string values "on"/"true" to Boolean.TRUE and "off"/"false" to Boolean.FALSE

Key advantages:
- No custom registry entries are created
- No client-side synchronization needed
- Works with vanilla clients and modded clients
- Maintains the same user experience (suggestions and validation)

### Command Registration Example

Before:
```java
Commands.argument("enabled", StringToBooleanArgumentType.stringToBoolean())
    .executes(context -> command(context, StringToBooleanArgumentType.getBoolean(context, "enabled")))
```

After:
```java
Commands.argument("enabled", VanillaBooleanParser.argument())
    .suggests(VanillaBooleanParser.booleanSuggestions())
    .executes(context -> command(context, VanillaBooleanParser.getBoolean(context, "enabled")))
```
=======
### 4. Command Argument Registration

- Modified how command arguments are registered to avoid client sync issues
- Implemented server-side validation to ensure commands work in a modded environment
- Removed dependencies on client-side registrations
>>>>>>> 7e60483 (feat: Optimize NeoEssentials for server-only functionality and update documentation)

## Deployment Instructions

### Server Setup

1. Install NeoEssentials on your server
2. Ensure the server is running NeoForge
3. Start the server and verify NeoEssentials loads correctly
4. No special configuration is needed - it works out of the box

### Client Requirements

- Clients do **not** need to install NeoEssentials
<<<<<<< HEAD
- Clients can be vanilla or modded with any combination of mods
- No special client configuration is needed

## Solving the Registry Synchronization Issue

The main issue causing client disconnects was:

```
Connection lost
The server send registeries with unknown keys:
ResourceKey[minecraft:command_argument_type / neoessentials:String_to_boolean]
```

Our solution:
- Completely eliminate custom command argument types
- Use only vanilla command argument types that exist in both modded and vanilla clients
- Handle specialized parsing server-side after command execution
- Ensure zero custom registry entries need synchronization
=======
- Clients should be running NeoForge with their preferred mods
- No special client configuration is needed

## How It Works in a Modded Environment

In a NeoForge modded environment, the server registers all command types and managers, but doesn't require clients to have matching registrations. This is different from vanilla Minecraft, where registry synchronization would cause disconnection issues.

The key components that make this work:

1. Server-side registration that respects modded environments
2. Proper `mods.toml` configuration to indicate server-only operation
3. Network handlers optimized for one-way server-to-client communication
4. Careful management of command argument types
>>>>>>> 7e60483 (feat: Optimize NeoEssentials for server-only functionality and update documentation)

## Testing Your Setup

To ensure the server-only setup works correctly:

1. Install NeoEssentials on your server
2. Have clients connect with different mod loadouts (none of which include NeoEssentials)
<<<<<<< HEAD
3. Test boolean commands like `/fly on`, `/god off`, `/powertool -e true`
4. Verify tab completion works for boolean arguments
5. Monitor server logs for any registry synchronization errors

See the `SERVER_SIDE_TEST_PLAN.md` document for a comprehensive testing protocol.

## Best Practices for Server-Side Command Arguments

When developing server-side command functionality:

1. Use vanilla command argument types whenever possible
2. If custom behavior is needed, use vanilla types and post-process the values
3. Add suggestion providers to enhance user experience
4. Avoid creating custom registry entries that require synchronization
5. Use proper error handling for validation

This approach ensures maximum compatibility across vanilla and modded environments.
=======
3. Verify that all NeoEssentials commands function properly from the server
4. Confirm clients can join and play without disconnection issues

If you encounter any issues, please check the server logs for registration messages and errors.
>>>>>>> 7e60483 (feat: Optimize NeoEssentials for server-only functionality and update documentation)
