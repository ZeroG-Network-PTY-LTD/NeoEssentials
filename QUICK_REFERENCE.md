# NeoEssentials Quick Reference Guide

## 🎮 **Essential Commands**

### **Player Utilities**
```
/heal [player]          - Restore health and hunger
/feed [player]          - Restore hunger and saturation  
/god [player]           - Toggle invincibility
/vanish [player]        - Toggle invisibility
/fly [player]           - Toggle flight mode
/speed <type> <1-10>    - Set movement speed
/repair [hand/all]      - Repair items
/workbench              - Open crafting table
/anvil                  - Open anvil interface
/back                   - Return to previous location
```

### **Teleportation**
```
/home [name]            - Teleport to home
/sethome [name]         - Set home location
/delhome <name>         - Delete home
/homes                  - List all homes

/warp <name>            - Teleport to warp
/setwarp <name>         - Create warp (admin)
/delwarp <name>         - Delete warp (admin)
/warps                  - List all warps

/spawn                  - Go to spawn
/setspawn              - Set spawn (admin)

/tpa <player>          - Request teleport to player
/tpahere <player>      - Request player teleport to you
/tpaccept              - Accept teleport request
/tpdeny                - Deny teleport request
```

### **Economy System**
```
/balance               - Check your balance
/pay <player> <amount> - Send money to player
/baltop                - View balance leaderboard

Admin Commands:
/eco give <player> <amount>  - Give money
/eco take <player> <amount>  - Take money  
/eco set <player> <amount>   - Set balance
/eco reset <player>          - Reset balance
```

### **Communication**
```
/msg <player> <message>     - Private message
/reply <message>            - Reply to last message
/nick [nickname]            - Set nickname
/nick off                   - Remove nickname

/mail send <player> <msg>   - Send offline mail
/mail read                  - Read your mail
/mail clear                 - Clear all mail
/mail delete <id>           - Delete specific mail

/motd                       - View message of the day
/rules                      - View server rules
```

### **GUI Interfaces**
```
/gui <type>            - Open GUI interface
/shop [category]       - Browse server shop
/menu                  - Main server menu  
/stats [player]        - Player statistics
/kits                  - Kit selector
/warps                 - Warp selector GUI
/economy               - Economy management
/servergui             - Server information
/tpmenu                - Teleportation options
```

### **Kit System**
```
/kit [name]            - Claim kit or list available kits
```

### **Moderation (Admin)**
```
/kick <player> [reason]     - Kick player
/ban <player> [reason]      - Ban player permanently
/unban <player>             - Unban player
/tempban <player> <time>    - Temporary ban
/mute <player> [time]       - Mute player
/unmute <player>            - Unmute player
/mutelist                   - List muted players
```

### **Server Management (Admin)**
```
/time <set/add> <value>     - Control time
/weather <type>             - Control weather
/gamemode <mode> [player]   - Change game mode
/spawner <mob>              - Change spawner type

/give <player> <item> [amount]  - Give items
/tp <player/coords>             - Teleport commands
/tphere <player>                - Teleport player to you
```

### **Information Commands**
```
/help [category]        - Command help system
/info                   - Server information  
/list                   - Online players
/whois <player>         - Player information
/seen <player>          - Check if player was online
/playtime [player]      - View playtime statistics
/achievements [player]  - View achievements
```

---

## 🎛️ **Admin Commands**

### **Configuration Management**
```
/config reload          - Reload all configurations
/config save            - Save current configurations
/config status          - View configuration status
/config validate        - Validate configurations
/config get <config>    - Get config information
```

### **Language System**
```
/language               - Show current language info
/language set <lang>    - Change your language
/language list          - List available languages
/language reload        - Reload language files (admin)
/language stats         - Language system stats (admin)
/language test <key>    - Test language key (admin)
```

### **System Monitoring**
```
/neostatus              - System health overview
/neostatus config       - Configuration status
/neostatus lang         - Language system status
/neostatus memory       - Memory usage statistics
/neostatus performance  - Performance metrics
```

### **Testing Framework**
```
/testgui shop           - Test shop GUI
/testgui stats          - Test stats GUI
/testgui kits           - Test kit GUI
/testgui all            - Test all GUIs
```

### **Discord Integration**
```
/discord status         - Integration status
/discord test           - Test webhook connection
/discord enable <bool>  - Enable/disable integration
/discord webhook <url>  - Set webhook URL
/discord notify <msg>   - Send custom notification
```

---

## 🎨 **GUI Categories**

### **Shop GUI Categories**
- **Weapons** - Swords, bows, crossbows
- **Armor** - All armor types and materials
- **Food** - Food items and consumables
- **Blocks** - Building and decoration blocks
- **Redstone** - Redstone components and mechanisms

### **Player Stats Categories**
- **General** - Basic player information
- **Economy** - Balance and transaction history
- **Playtime** - Session and total playtime
- **Achievements** - Progress and completed achievements
- **Teleportation** - Home and warp usage statistics
- **Social** - Communication and interaction stats

---

## ⚙️ **Configuration Files**

### **Main Configuration Files**
- `main.json` - Core mod settings
- `economy.json` - Economy system configuration
- `homes.json` - Home system settings
- `kits.json` - Kit definitions and cooldowns
- `warps.json` - Warp configurations
- `moderation.json` - Moderation system settings
- `messaging.json` - Communication settings
- `discord.json` - Discord integration setup
- `tablist.json` - Tab list customization
- `spawn.json` - Spawn system configuration

### **Language Files**
Located in `config/neoessentials/languages/`
- `en_us.json` - English (Default)
- `es_es.json` - Spanish
- `fr_fr.json` - French
- `de_de.json` - German
- And 14+ more languages...

---

## 🔧 **Permissions**

### **Basic Player Permissions**
```
neoessentials.player.*          - All basic player commands
neoessentials.home.*            - Home system access
neoessentials.warp.use          - Use warps
neoessentials.economy.use       - Economy system access  
neoessentials.messaging.*       - Communication commands
neoessentials.kit.use           - Use kits
neoessentials.gui.*             - Access to GUI interfaces
```

### **VIP/Donator Permissions**
```
neoessentials.kit.vip           - Access to VIP kits
neoessentials.home.multiple     - Multiple homes
neoessentials.economy.bonus     - Economy bonuses
neoessentials.teleport.instant  - Instant teleportation
```

### **Moderator Permissions**
```
neoessentials.moderation.*      - All moderation commands
neoessentials.admin.see         - Admin oversight commands
neoessentials.bypass.*          - Bypass restrictions
```

### **Admin Permissions**
```
neoessentials.admin.*           - All admin commands
neoessentials.config.*          - Configuration management
neoessentials.system.*          - System commands
neoessentials.*                 - Full access
```

---

## 📱 **Quick Tips**

### **For Players**
1. Use `/help` to explore available commands
2. Set up homes with `/sethome` for quick travel
3. Check your stats with `/stats` for progress tracking
4. Use the GUI interfaces for easier navigation
5. Set your preferred language with `/language set`

### **For Admins**
1. Regularly check `/neostatus` for system health
2. Use `/config status` to monitor configurations
3. Test GUIs with `/testgui` commands
4. Monitor language system with `/language stats`
5. Use Discord integration for server notifications

### **For Developers**
1. Check API documentation for extension points
2. Use the testing framework for quality assurance
3. Monitor logs for performance insights
4. Utilize configuration templates for customization
5. Leverage the language system for localization

---

**🎉 NeoEssentials - Your Complete Server Management Solution!**
