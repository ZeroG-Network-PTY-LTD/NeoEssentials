# NeoEssentials - CurseForge Description

*The ultimate server-side essentials mod for NeoForge servers*

![NeoEssentials Logo](../images/Logo.png)

## 🎯 What is NeoEssentials?

NeoEssentials is a comprehensive server management and essentials mod designed specifically for **NeoForge 1.21.1+** servers. Inspired by the legendary EssentialsX for Bukkit/Spigot, NeoEssentials brings all the essential features you need to run a professional Minecraft server - **without requiring any client-side installation**.

### ⚡ **100% Server-Side Implementation**

NeoEssentials is engineered to be completely server-side:
- ✅ **No client installation required** - Players join with vanilla or any modded client
- ✅ **Zero client disconnects** - Works seamlessly in modded environments  
- ✅ **Vanilla compatibility** - Uses only vanilla-compatible command arguments
- ✅ **Universal support** - Works with both vanilla and modded clients

---

## 🚀 **Revolutionary Features (v1.0.1.89+)**

### 🎨 **Ultra-Smooth Tablist System**
Experience the most advanced tablist system available for NeoForge:
- **25ms Animation Updates** - Ultra-smooth 40 FPS animations for professional quality
- **Multiple Simultaneous Animations** - Use unlimited `<anim:name>` placeholders in headers/footers  
- **Three-Tier Update System** - Optimized performance: 25ms animations, 250ms placeholders, 3000ms templates
- **Group-Specific Templates** - Different displays for admins, VIPs, and regular players
- **Hex Color Support** - Full RGB colors with `&#RRGGBB` format
- **Boss Bar Integration** - Animated boss bars with dynamic content

```yaml
# Example: Multiple animations working together
headers:
  - "<anim:welcome> &a%player% <anim:pulse>"
  - "Online: <anim:counter> &7| Time: <anim:clock>"
  - "<anim:rainbow> MyServer &aOnline <anim:rainbow>"
```

### 🏠 **Advanced Teleportation System**
Complete teleportation management for your server:
- **Multiple Homes** - Players can set unlimited homes with `/home`, `/sethome`, `/delhome`
- **Server Warps** - Create public teleportation points with `/warp`, `/setwarp`, `/delwarp`
- **TPA System** - Send and accept teleport requests with `/tpa`, `/tpaccept`, `/tpdeny`
- **Back Command** - Return to previous location after teleporting with `/back`
- **Cooldowns & Costs** - Configurable delays and economy costs
- **Permission Control** - Fine-grained access control per warp/home

### 💰 **Comprehensive Economy System**
Full-featured economy management:
- **Balance Management** - Check and manage player balances with `/balance` and `/eco`
- **Player Payments** - Transfer money between players with `/pay`
- **Admin Controls** - Give, take, set, and reset balances with `/eco` commands
- **Shop Integration** - Ready integration with shop systems and plugins
- **Transaction Logging** - Complete audit trail of all economic activities
- **Multiple Storage Backends** - JSON, SQLite, or MySQL support

### 🛠️ **Professional Administrative Tools**
Everything you need to manage your server:
- **Moderation Suite** - Ban, kick, mute, jail players with comprehensive feedback
- **Performance Monitoring** - Track server TPS, memory usage, and entity counts
- **Maintenance Mode** - Toggle server maintenance with whitelist bypass
- **Vanish System** - Become invisible to regular players
- **Staff Tools** - Admin panel, mod tools, and staff utilities

### 📦 **Player Utility Features**
Enhance your players' experience:
- **Kit System** - Create and distribute item kits with cooldowns and permissions
- **Mail System** - Send offline messages to players that persist across sessions
- **AFK Detection** - Automatically detect and mark idle players
- **Chat Management** - Format, color, and manage chat messages
- **PowerTools** - Bind commands to items for quick execution
- **Time & Weather Control** - Manage game environment with simple commands

---

## ⚙️ **Advanced Configuration**

### **Hybrid Configuration System**
NeoEssentials uses an intelligent configuration approach:

**TOML Files** (`config/neoessentials/`)
- `general.toml` - Core mod settings and performance options
- `tablist.toml` - Tablist system configuration and intervals
- `economy.toml` - Economy settings and storage backend
- `permissions.toml` - Permission system configuration

**YAML/JSON Templates** (`neoessentials/`)
- `tablist.yml` - Tablist headers, footers, and group templates
- `animations.yml` - Custom animation definitions and effects
- `templates.yml` - Additional template configurations

### **Performance Optimization**
Built for servers of all sizes:
- **Three-Tier Update System** - Separate intervals for optimal performance
- **Efficient Caching** - Smart memory management and data caching
- **Async Operations** - Non-blocking operations for better TPS
- **Scalable Architecture** - Tested with 100+ concurrent players

---

## 📊 **Flexible Storage Options**

Choose the storage backend that fits your needs:

1. **JSON (Default)** - Simple file-based storage for small servers
2. **SQLite** - Local database for better performance and data integrity  
3. **MySQL** - External database for multi-server networks and large communities

All storage backends support automatic migration and data backup.

---

## 🔐 **Comprehensive Permission System**

Works seamlessly with popular permission mods:
- **LuckPerms** (Recommended) - Full integration with advanced features
- **FTB Ranks** - Complete compatibility with FTB modpacks
- **Built-in Fallback** - Vanilla permission system when no mod is present
- **Fine-Grained Control** - Over 100 permission nodes for precise access control

---

## 📋 **50+ Essential Commands**

NeoEssentials includes comprehensive commands across all categories:

**Administrative Commands**
- `/neoessentials` - Main mod information and management
- `/adminpanel` - Intuitive admin interface
- `/broadcast` - Server-wide announcements
- `/maintenance` - Toggle maintenance mode

**Player Commands**  
- `/home`, `/sethome`, `/delhome` - Home management
- `/warp`, `/warps` - Warp system
- `/balance`, `/pay` - Economy commands
- `/kit`, `/kits` - Kit system
- `/mail` - Offline messaging

**Moderation Tools**
- `/ban`, `/unban`, `/tempban` - Player banning
- `/kick`, `/mute`, `/unmute` - Basic moderation
- `/jail`, `/unjail` - Jail system
- `/vanish` - Staff invisibility

**Utility Commands**
- `/afk` - AFK status management
- `/ping` - Server latency check
- `/time`, `/weather` - World management
- `/fly`, `/gamemode` - Player utilities

---

## 🔧 **Technical Requirements**

**Server Requirements:**
- **Minecraft:** 1.21.1+
- **NeoForge:** 52.1.1+  
- **Java:** 17 or higher
- **RAM:** Minimum 4GB (8GB+ recommended for animations)

**Client Compatibility:**
- ✅ **Vanilla Clients** - Full compatibility
- ✅ **Modded Clients** - Works with any mod pack
- ✅ **Cross-Platform** - No client restrictions

---

## 🌟 **Why Choose NeoEssentials?**

### **Performance First**
- **Zero TPS Impact** - Optimized for large servers
- **Ultra-Smooth Animations** - 40 FPS quality without lag
- **Efficient Memory Usage** - Smart caching and cleanup
- **Scalable Design** - Grows with your server

### **Feature Rich**
- **Complete Solution** - Everything you need in one mod
- **Professional Quality** - Enterprise-grade features and reliability
- **Constantly Updated** - Regular updates with new features
- **Community Driven** - Built based on server owner feedback

### **Easy to Use**
- **Intuitive Commands** - Easy to learn and remember
- **Comprehensive Documentation** - Detailed wiki and guides
- **Great Support** - Active community and developer support
- **Quick Setup** - Working in minutes, not hours

---

## 🔗 **Links & Resources**

- **📚 [Complete Documentation](https://github.com/ZeroG-Network/NeoEssentials/wiki)** - Comprehensive guides and tutorials
- **🐛 [Issue Tracker](https://github.com/ZeroG-Network/NeoEssentials/issues)** - Bug reports and feature requests  
- **💬 [Discord Community](https://discord.gg/neoessentials)** - Get help and connect with other users
- **📖 [Command Reference](https://github.com/ZeroG-Network/NeoEssentials/wiki/Commands-Reference)** - Complete command list
- **⚙️ [Configuration Guide](https://github.com/ZeroG-Network/NeoEssentials/wiki/Configuration-Guide)** - Setup and customization
- **🎨 [Animation Examples](https://github.com/ZeroG-Network/NeoEssentials/wiki/Animation-System)** - Custom animation tutorials

---

## 📈 **Perfect For**

✅ **Small Private Servers** - Easy setup with sensible defaults  
✅ **Large Public Networks** - Scalable architecture and performance  
✅ **Modded Server Networks** - Full compatibility with popular modpacks  
✅ **Creative Servers** - Rich customization and player tools  
✅ **Survival Servers** - Complete economy and teleportation systems  
✅ **RP Servers** - Advanced chat and formatting features  

---

**Ready to enhance your NeoForge server?** Download NeoEssentials today and experience the most comprehensive server management solution available!

*NeoEssentials - Professional server management made simple.*
