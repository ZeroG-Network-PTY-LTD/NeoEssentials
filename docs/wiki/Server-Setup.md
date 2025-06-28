# Server Setup Guide

This guide will help you set up a NeoForge Minecraft server with NeoEssentials.

## Prerequisites

- Java 17 or newer
- At least 4GB of RAM allocated to the server
- NeoForge 1.21.1 or newer

## Step-by-Step Installation

### 1. Download and Install NeoForge

1. Download the latest NeoForge installer for your Minecraft version from [NeoForged](https://neoforged.net/)
2. Run the installer and follow the instructions to create a server
3. Start the server once to generate all necessary files and folders
4. Stop the server by typing `stop` in the console

### 2. Install NeoEssentials

1. Download the latest version of NeoEssentials from [GitHub Releases](https://github.com/ZeroG-Network/NeoEssentials/releases)
2. Place the NeoEssentials JAR file in the `mods` folder of your server
3. Restart the server

### 3. Initial Configuration

After starting the server with NeoEssentials installed:

1. The mod will generate default configuration files in the `config/neoessentials` directory
2. Stop the server again by typing `stop` in the console
3. Edit the configuration files according to your needs (see the [Configuration Guide](Configuration-Guide) for details)

### 4. Additional Setup

#### Memory Allocation

For optimal performance, allocate enough memory to your server:

```bash
java -Xmx4G -Xms2G -jar forge-server.jar nogui
```

Adjust the `-Xmx` and `-Xms` values according to your available RAM.

#### Using a Startup Script

Create a startup script to simplify server management:

**Windows (start.bat):**
```batch
@echo off
java -Xmx4G -Xms2G -jar forge-server.jar nogui
pause
```

**Linux/macOS (start.sh):**
```bash
#!/bin/bash
java -Xmx4G -Xms2G -jar forge-server.jar nogui
```

Make the script executable on Linux/macOS: `chmod +x start.sh`

## Server Properties Configuration

Edit the `server.properties` file to configure basic server settings:

- `server-port=25565` - The port your server will run on
- `max-players=20` - Maximum number of concurrent players
- `difficulty=normal` - Game difficulty
- `pvp=true` - Enable or disable player versus player combat
- `view-distance=10` - Render distance in chunks
- `online-mode=true` - Verify player identities with Minecraft account servers
- `motd=A Minecraft Server with NeoEssentials` - Server description in the server list

## Setting Up Permissions

NeoEssentials works best with a permissions system:

1. Install [LuckPerms](https://luckperms.net/) in your `mods` folder
2. Configure LuckPerms according to their documentation
3. Set up permission groups and assign permissions for NeoEssentials commands
4. See our [LuckPerms Integration](LuckPerms-Integration) guide for details

## Next Steps

Once your server is set up:

1. Configure [economy](Economy-System), [homes](Home-System), and other systems
2. Set up [custom tablist templates](Tablist-System) for a professional server appearance
3. Create [warps](Warp-System) and [kits](Kit-System) for your players

## Troubleshooting

If you encounter issues during setup:

- Check the server logs for errors
- Verify that you're using a compatible version of NeoForge and NeoEssentials
- Visit our [Troubleshooting](Troubleshooting) page for common issues and solutions
- Join our [Discord server](https://discord.gg/dUGAQF2Mga) for support

## Performance Tips

For server performance recommendations, see our [Performance Optimization](Performance-Optimization) guide.
