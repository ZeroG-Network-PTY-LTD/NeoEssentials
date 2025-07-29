# NeoEssentials - Server-Side Installation Guide

## 🖥️ Server-Side Only Mod

**NeoEssentials is designed as a server-side only utility mod.** This means:

- ✅ **Install ONLY on the server**
- ❌ **Do NOT install on client**
- ✅ **Players can join without having the mod installed**
- ✅ **No client-server network compatibility issues**

## 📥 Installation

### For Server Administrators:

1. **Download** the NeoEssentials JAR file
2. **Place** the JAR in your server's `mods/` folder
3. **Start** your server
4. **Players join normally** - no client mod required!

### For Players:

**Nothing required!** Players can join the server with vanilla Minecraft or any other mod setup. NeoEssentials works entirely server-side.

## 🔧 How It Works

NeoEssentials operates using:
- **Server Commands**: All functionality through `/home`, `/spawn`, etc.
- **Server-Side Storage**: Player data stored on server only
- **Standard Minecraft Packets**: Uses vanilla teleportation and chat systems
- **No Custom Networking**: Avoids client-server communication that would require client installation

## ⚡ Features Available

Since this is server-side only, all features work through:
- **Chat Commands**: Type commands in game chat
- **Server Permissions**: Managed through server permission systems
- **Server Configuration**: Config files on server only
- **Server Storage**: Player homes, warps, etc. stored server-side

## 🛡️ Network Safety

The mod is designed to be "network safe":
- No custom packets sent to clients
- No requirement for client-side mod
- No version mismatches or connection issues
- Compatible with any client setup (vanilla, modded, etc.)

## 📋 Available Commands

All commands work from in-game chat:
- `/home [name]` - Teleport to home
- `/sethome [name]` - Set a home location  
- `/delhome <name>` - Delete a home
- `/homes` - List your homes
- More commands coming in future updates!

## 🎯 Perfect For

- **Public Servers**: No need to force clients to install mods
- **Multi-Version Support**: Works with any client version (within MC version range)
- **Plugin Alternatives**: Provides essential features without Bukkit/Spigot
- **Modded Servers**: Adds utilities without client mod requirements

---

*This server-side approach ensures maximum compatibility and ease of use for both server administrators and players.*
