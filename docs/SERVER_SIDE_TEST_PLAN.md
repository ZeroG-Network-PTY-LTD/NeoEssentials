# Server-Side Test Plan for NeoEssentials

## Overview

This document outlines the testing procedures to verify that NeoEssentials functions correctly as a true server-side mod in a NeoForge modded environment, without requiring client installation.

## Test Environment Setup

### Server Configuration
1. Clean NeoForge 1.21.1 server installation
2. NeoEssentials mod installed in the server's mods folder
3. Server configured with standard settings
4. Optional: Additional mods for compatibility testing

### Client Configurations
To thoroughly test server-side compatibility, prepare clients with the following configurations:
1. Vanilla Minecraft client (no mods)
2. NeoForge client with NO mods installed
3. NeoForge client with various mods, but NOT NeoEssentials
4. NeoForge client with various mods INCLUDING NeoEssentials

## Test Cases

### Connection Tests
| Test ID | Description | Expected Outcome | Status |
|---------|-------------|-----------------|--------|
| CONN-01 | Connect with vanilla client | Successful connection | ⬜ |
| CONN-02 | Connect with NeoForge client (no mods) | Successful connection | ⬜ |
| CONN-03 | Connect with NeoForge client (other mods) | Successful connection | ⬜ |
| CONN-04 | Connect with NeoForge client (with NeoEssentials) | Successful connection | ⬜ |

### Command Tests
| Test ID | Description | Expected Outcome | Status |
|---------|-------------|-----------------|--------|
| CMD-01 | Execute `/heal` command | Player is healed | ⬜ |
| CMD-02 | Execute `/feed` command | Player hunger is filled | ⬜ |
| CMD-03 | Execute `/fly on` command | Player can fly | ⬜ |
| CMD-04 | Execute `/fly off` command | Player cannot fly | ⬜ |
| CMD-05 | Execute `/god on` command | Player takes no damage | ⬜ |
| CMD-06 | Execute `/god off` command | Player takes normal damage | ⬜ |
| CMD-07 | Execute `/powertool -e true` command | PowerTools enabled | ⬜ |
| CMD-08 | Execute `/powertool -e false` command | PowerTools disabled | ⬜ |
| CMD-09 | Tab completion for boolean arguments | Shows on/off/true/false | ⬜ |

### Home/Warp System Tests
| Test ID | Description | Expected Outcome | Status |
|---------|-------------|-----------------|--------|
| HOME-01 | Set a home with `/sethome` | Home is set | ⬜ |
| HOME-02 | Teleport to home with `/home` | Player teleports | ⬜ |
| HOME-03 | Delete a home with `/delhome` | Home is removed | ⬜ |
| WARP-01 | Create warp with admin permissions | Warp is created | ⬜ |
| WARP-02 | Teleport to warp | Player teleports | ⬜ |
| WARP-03 | Delete warp with admin permissions | Warp is removed | ⬜ |

### Configuration Tests
| Test ID | Description | Expected Outcome | Status |
|---------|-------------|-----------------|--------|
| CONF-01 | Modify general.toml and reload | Config changes take effect | ⬜ |
| CONF-02 | Modify teleport.toml and reload | Config changes take effect | ⬜ |
| CONF-03 | Check config defaults work when values missing | Default values applied | ⬜ |

### Server-Only Feature Tests
| Test ID | Description | Expected Outcome | Status |
|---------|-------------|-----------------|--------|
| FEAT-01 | Check tablist formatting | Tablist updates per config | ⬜ |
| FEAT-02 | Test server performance under load | No performance issues | ⬜ |
| FEAT-03 | Test server restart with mod installed | Mod loads correctly | ⬜ |
| FEAT-04 | Check server logs for registry errors | No registry sync errors | ⬜ |

## Registry Synchronization Tests

These tests specifically target the command argument fixes:

| Test ID | Description | Expected Outcome | Status |
|---------|-------------|-----------------|--------|
| REG-01 | Check server logs during client connection | No "unknown keys" errors | ⬜ |
| REG-02 | Review registry synchronization packets | No custom registry entries | ⬜ |
| REG-03 | Connect multiple clients simultaneously | No disconnection issues | ⬜ |

## Bug Verification

Verify that the following previously reported bugs have been fixed:

1. Client disconnection with error: "The server send registeries with unknown keys: ResourceKey[minecraft:command_argument_type / neoessentials:String_to_boolean]"
2. Config access before initialization causing server crash
3. Command argument type registration issues

## Test Results

| Category | Tests Passed | Total Tests | Status |
|----------|--------------|------------|--------|
| Connection Tests | 0 | 4 | Not Started |
| Command Tests | 0 | 9 | Not Started |
| Home/Warp Tests | 0 | 6 | Not Started |
| Configuration Tests | 0 | 3 | Not Started |
| Server-Only Tests | 0 | 4 | Not Started |
| Registry Tests | 0 | 3 | Not Started |

## Conclusion

When completed, this test plan will verify that NeoEssentials functions correctly as a true server-side mod in a NeoForge environment, with no client-side installation required. The testing focuses on ensuring that all functionality works correctly and that there are no issues with registry synchronization or client disconnections.
