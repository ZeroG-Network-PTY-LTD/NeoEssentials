# Server-Only Implementation Guide for Modded Environments

## Overview

NeoEssentials has been optimized to work as a true server-side mod in a modded NeoForge environment. This means clients with other mods can connect to the server without needing to install NeoEssentials, even though the server has it installed.

## Implementation Details

We've made significant changes to make NeoEssentials function as a true server-side mod:

### 1. Modified Registration System

- Command argument types are now only registered on the server side
- The mod explicitly checks for the dedicated server environment before registering critical components
- Server-side registrations are isolated to prevent client synchronization issues

### 2. Updated `mods.toml` Configuration

- Set `displayTest` to `IGNORE_SERVER_VERSION` to indicate clients don't need the mod
- Changed dependency sides from `BOTH` to `SERVER` to specify server-only operation
- Updated documentation to reflect the server-only approach

### 3. Enhanced Network Handling

- Created a dedicated NetworkHandler to isolate server functionality
- Implemented server-side initialization that doesn't require client components
- Added robust error handling for modded environment compatibility

### 4. Command Argument Registration

- Modified how command arguments are registered to avoid client sync issues
- Implemented server-side validation to ensure commands work in a modded environment
- Removed dependencies on client-side registrations

## Deployment Instructions

### Server Setup

1. Install NeoEssentials on your server
2. Ensure the server is running NeoForge
3. Start the server and verify NeoEssentials loads correctly
4. No special configuration is needed - it works out of the box

### Client Requirements

- Clients do **not** need to install NeoEssentials
- Clients should be running NeoForge with their preferred mods
- No special client configuration is needed

## How It Works in a Modded Environment

In a NeoForge modded environment, the server registers all command types and managers, but doesn't require clients to have matching registrations. This is different from vanilla Minecraft, where registry synchronization would cause disconnection issues.

The key components that make this work:

1. Server-side registration that respects modded environments
2. Proper `mods.toml` configuration to indicate server-only operation
3. Network handlers optimized for one-way server-to-client communication
4. Careful management of command argument types

## Testing Your Setup

To ensure the server-only setup works correctly:

1. Install NeoEssentials on your server
2. Have clients connect with different mod loadouts (none of which include NeoEssentials)
3. Verify that all NeoEssentials commands function properly from the server
4. Confirm clients can join and play without disconnection issues

If you encounter any issues, please check the server logs for registration messages and errors.
