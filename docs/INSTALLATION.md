# NeoEssentials Installation Guide

## System Requirements
- NeoForge 1.21.1 or later
- Java 21 or later
- Minecraft Server 1.21.1

## Installation Steps

### Server Installation
1. Download the latest `neoessentials-X.X.X.jar` from the [Releases page](https://github.com/ZeroG-Network-Org/NeoEssentials/releases)
2. Place the JAR file in your server's `mods` folder
3. Start your server
4. The mod will generate default configuration files in `config/neoessentials/`
5. Stop the server and configure as needed
6. Restart the server

### Configuration
Configuration files are located in `config/neoessentials/`:
- `main.json` - Main mod settings
- `economy.json` - Economy system settings  
- `homes.json` - Home system settings
- `warps.json` - Warp system settings
- `commands/` - Command-specific configurations

## First Setup
1. Set up permissions using your permission plugin (LuckPerms recommended)
2. Configure economy settings if using the economy system
3. Set spawn location using `/setspawn`
4. Create initial warps for players

## Updating
1. Stop the server
2. Replace the old JAR with the new version
3. Start the server
4. Check configuration files for new options

## Troubleshooting
- Check server logs for any error messages
- Ensure all dependencies are installed
- Verify permission configurations
- Join our Discord for support
