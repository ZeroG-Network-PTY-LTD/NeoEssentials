# NeoEssentials v1.0.2.4

## 🚨 CRITICAL UPDATE - Module Conflict Resolution + Teleportation Fix

**Build #750** | January 13, 2026 | MC 1.21.1 - 1.21.11 | NeoForge 21.1.179+ / 21.11.24-beta

This release fixes critical compatibility issues AND teleportation safety bugs!

---

## 🐛 Critical Fixes

### Unsafe Teleportation System - FIXED ✅
**The Problem:**
- Safety config options (`enableHomeTeleportSafety: false`) didn't work
- Mod blocked teleportation to "unsafe" locations even when safety was disabled
- Couldn't teleport to Nether ceiling, lava bases, intentional unsafe builds
- All teleportation types affected (homes, warps, spawn, TPA)

**The Solution:**
- ✅ Fixed safety check logic in HomeManager, WarpManager, SpawnManager
- ✅ When safety `true`: Finds safe alternatives or blocks if none exist
- ✅ When safety `false`: Allows teleportation ANYWHERE (even dangerous spots)
- ✅ Updated all config comments to clearly explain behavior
- ✅ Tested with Nether ceiling, void platforms, lava builds

**The Result:**
- 🎉 Full control over teleportation safety!
- 🎉 Advanced players can disable safety for legitimate use cases!
- 🎉 Nether ceiling farms work when safety is disabled!
- 🎉 Default behavior unchanged (safe by default for casual players)!

**Affected Config Options:**
```
"enableHomeTeleportSafety": false
"enableWarpSafety": false
"enableSpawnSafety": false
"enableTeleportSafety": false
```
*These options now work correctly! Set to `false` to allow teleportation anywhere.*

### Module Export Conflicts - RESOLVED ✅
**The Problem:**
- Server crashes with "Module neoessentials exports package..." errors
- Conflicts with: JadeAddons, SmoothChunk, SDLink, LetMeDespawn, and others
- Incompatible with Arclight/hybrid server platforms
- "Module neoforge reads more than one module named com.google.gson"

**The Solution:**
- ✅ Migrated from Shadow plugin to NeoForge's native JarJar system
- ✅ Removed Gson dependency (NeoForge already provides it)
- ✅ Properly isolated Java-WebSocket library
- ✅ Eliminated module-info.class conflicts
- ✅ No longer exports Minecraft/NeoForge packages

**The Result:**
- 🎉 Works with ALL tested mod combinations!
- 🎉 Compatible with Arclight & hybrid platforms!
- 🎉 Clean server startup every time!
- 🎉 No more module resolution errors!

**Technical Changes:**
- Build system: Shadow → JarJar
- Dependencies: Gson removed, Java-WebSocket isolated
- JAR structure: Optimized for NeoForge module system
- Module metadata: Cleaned up, no package exports

---

## 🎉 Previous Updates (v1.0.2.4)

### Chat Channels System
**FIXED:** Channels were configured but commands didn't exist!
- `/l`, `/g`, `/staff` commands now work
- Prefix support: `!message` (global), `@message` (staff)
- Local = proximity chat, Global = server-wide, Staff = permission-based
- Fully functional and documented

### Config Auto-Update System
**FIXED:** Configs now automatically update when you upgrade the mod!
- Detects old `_configVersion` values
- Creates timestamped backups before updating
- Replaces old configs with new versions
- No more manual config deletion!
- All new features work immediately after update

---

### Config File Splitting (IMPROVED!)
**SMART AUTO-DETECTION** - New servers get optimized configs automatically!

**How it works:**
- **Fresh installs:** Auto-creates 9 split config files (no manual steps!)
- **Existing servers:** In-game notification to OPs/admins
- **Deleted configs:** Auto-recovery with split structure

**In-game notification shown to admins:**
- OPs (level 4)
- Players with `*` or `neoessentials.*` permission
- Only shown once per server start
- Beautiful formatted message with benefits

**Manual migration:**
- Command: `/neoessentials config split`
- Automatic backup creation
- Splits config.json into 9 focused files
- Easy rollback support
- 100% backward compatible

**Files created:**
main.json, commands.json, chat.json, teleportation.json, moderation.json, webdashboard.json, items.json, afk.json, security.json

**Benefits:**
- Easier to find settings
- Less syntax errors
- Better organization
- Per-file version control

---

## ✨ New Features (Continued)
## ✨ New Features


### 💬 Phase 2: Interactive Chat
- **Clickable URLs** - Auto-detect and link http/https URLs
- **@Mentions** - Highlight players with sound notifications
- **[item] Links** - Show held items with hover details

### 🎨 Phase 3: Advanced Features
- **Badge System** - Rank badges (👑⭐💎) + status icons (💤👻🔇)
- **Custom Images** - Drop PNG files in folder for custom badges
- **Anti-Spam** - Caps, repeat, link, and rate limiting filters
- **Templates** - 7 pre-built chat formats (RPG, Modern, Minimal, etc.)

### 🌈 Phase 4: Rich Text
- **Gradients** - `<gradient:FF0000-0000FF>text</gradient>`
- **Rainbow** - `<rainbow>text</rainbow>`
- **Conditionals** - Time/stat/state-based formatting
  - `<if:time=morning>Good morning!</if>`
  - `<if:health<50>❤️</if>`
  - `<if:afk>💤</if>`

---

## 🆕 What's Included

### **Interactive Elements**
- URLs auto-convert to clickable links (blue, underlined)
- @PlayerName mentions with sound notifications
- [item] shows held item with enchantments on hover

### **Badge & Icon System**
- Emoji badges per rank: 👑 Owner, ⭐ Admin, 💎 VIP, 🛡️ Moderator
- Custom badge images: Place PNG files in `config/neoessentials/badges/`
- Status icons: 💤 AFK, 👻 Vanished, 🔇 Muted
- Flexible positioning options

### **Anti-Spam Protection**
- **Caps Filter** - Converts SHOUTING to lowercase or blocks
- **Repeat Filter** - Blocks duplicate messages (5s cooldown)
- **Link Filter** - Whitelist/blacklist URLs
- **Rate Limit** - Prevents flooding (5 msgs per 10s)
- Bypass permissions for staff

### **Format Templates**
Choose from pre-built styles:
- **RPG:** `[Lv.30] [Admin] Steve: Hello!`
- **Modern:** `● [VIP] Alex › Hey!`
- **Minimal:** `Steve: message`
- **Detailed:** `[world] <[Admin] Steve> message`
- **Custom:** Your own format

### **Rich Text Effects**
- **Gradient Text:** Smooth color transitions
  - Example: `<gradient:FFD700-FF1493>VIP</gradient>` (gold→pink)
- **Rainbow Text:** 7-color spectrum
  - Example: `<rainbow>Rainbow Road!</rainbow>`

### **Conditional Formatting**
Dynamic chat based on:
- **Time:** morning, afternoon, evening, night, weekday, weekend
- **Stats:** health, level, food, armor, xp
- **State:** afk, flying, creative, survival, vanished, op
- **Dimension:** nether, end, overworld
- **Actions:** sneaking, sprinting, swimming, onfire, wet, underground

---

## 🆕 New Permissions

```
# Phase 2
neoessentials.chat.color
neoessentials.chat.color.hex
neoessentials.chat.format
neoessentials.chat.mention
neoessentials.chat.itemlink

# Phase 3
neoessentials.chat.caps.bypass
neoessentials.chat.repeat.bypass
neoessentials.chat.links.bypass
neoessentials.chat.spam.bypass

# Phase 4
neoessentials.chat.richtext
neoessentials.chat.gradient
neoessentials.chat.rainbow
```

---

## ⚙️ Configuration

All features are **highly configurable**:

```json
{
  "chat": {
    "enableChatEnhancements": true,
    "autoLinkUrls": true,
    "allowItemLinks": true,
    "mentions": { "enabled": true, "playSound": true },
    "badges": {
      "enabled": true,
      "useCustomImages": false,
      "rankBadges": { "admin": "⭐", "vip": "💎" }
    },
    "antiSpam": {
      "capsFilter": { "enabled": true },
      "repeatFilter": { "enabled": true },
      "spamFilter": { "enabled": true }
    },
    "formatTemplates": { "enabled": false },
    "richText": { "enabled": false },
    "conditionalFormatting": { "enabled": false }
  }
}
```

---

## 🎯 Example Usage

### **Gradient VIP Prefix**
```json
{
  "group:vip": "<gradient:FFD700-FF1493>{prefix}</gradient> {name}: {MESSAGE}"
}
```

### **Time-Based Greeting**
```json
{
  "default": "<if:time=morning>☀️ </if>{prefix}{name}: {MESSAGE}"
}
```

### **Health Warning**
```json
{
  "default": "{prefix}{name} <if:health<50>❤️ </if>: {MESSAGE}"
}
```

### **Complex Multi-Feature**
```json
{
  "group:admin": "<gradient:FF0000-00FF00>{prefix}</gradient> <if:flying>✈️ </if>{name}: {MESSAGE}"
}
```

---

## 🐛 Bug Fixes

- Fixed LuckPerms prefix/suffix integration
- Fixed Stream resource leaks
- Fixed null pointer warnings
- Improved error handling and logging
- Removed unused imports and redundant code
- Fixed color code restriction logic
- Debug logging now config-controlled

---

## 📚 Documentation

Complete documentation included:
- Phase 2-4 implementation guides
- Custom badge setup guide
- Placeholder reference
- Permission documentation
- Configuration examples
- Testing checklists

---

## 📊 Performance

- Minimal overhead (~10-15ms with all features)
- Optimized concurrent data structures
- Cached badge lookups
- Pre-compiled regex patterns
- No database queries for chat

---

## 🎮 Getting Started

1. **Update the mod** to v1.0.2.4
2. **Check config** at `config/neoessentials/config.json`
3. **Enable features** you want (all disabled by default)
4. **Set permissions** for your ranks
5. **Customize** badges, templates, and formats
6. **Test** in-game and enjoy!

---

## ⚠️ Notes

- Custom badge images require client resource pack (auto-generation included)
- Resource pack auto-send pending NeoForge API update (use server.properties)
- All Phase 4 features disabled by default (opt-in)

---

## 🔗 Links

- **Discord:** https://discord.gg/dUGAQF2Mga
- **GitHub:** https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials
- **Wiki:** https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki

---

**This is the most comprehensive chat update ever!** 🎉

4 phases, 50+ new features, 11 new permissions, complete documentation!

Enjoy! 🚀 - Changelog

**Build #TBD** | January 7, 2026 | Minecraft 1.21.1 - 1.21.11 | NeoForge 21.1.179+ / 21.11.24-beta

---

## 🎯 What's New

### 💬 **Interactive Chat Revolution!** ✨

Modern chat features that make communication fun and intuitive!

#### **Features:**

**🔗 Clickable URLs**
- Auto-detects and links URLs
- Click to open in browser
- Blue and underlined

**📢 @Mentions**
- Type `@PlayerName` to ping someone
- Bold yellow highlight
- Plays notification sound
- Click to message

**💎 Item Links**
- Use `[item]` to show held item
- Hover for full details
- Clickable and stylish

**🎨 Full Color Support**
- Basic colors (`&c` for red)
- Hex colors (`&#FF5500`)
- Format codes (bold, italic, etc.)

#### **Config:**
```json
{
  "enableChatEnhancements": true,
  "autoLinkUrls": true,
  "allowItemLinks": true,
  "mentions": {
    "enabled": true,
    "playSound": true,
    "highlightColor": "&e"
  }
}
```

#### **Permissions:**
```
neoessentials.chat.color       - Colors
neoessentials.chat.color.hex   - Hex colors
neoessentials.chat.format      - Formatting
neoessentials.chat.mention     - @mentions
neoessentials.chat.itemlink    - [item] links
```

#### **Example:**
```
Input:  @Steve check my [item] at https://example.com
Output: @Steve check my [Diamond Sword] at https://example.com
        ^^^^^^          ^^^^^^^^^^^^^^^^    ^^^^^^^^^^^^^^^^^^^
        mention         item link            URL link
        (yellow,        (aqua, hover         (blue,
         sound)         shows stats)         clickable)
```

---

## Installation

1. Download the `.jar` file
2. Place in `mods` folder
3. Restart server
4. Enjoy!

---

## Support

- 🐛 Found a bug? Report it on our [issue tracker](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/issues)
- 💬 Need help? Join our [Discord](https://discord.gg/dUGAQF2Mga)
- 📖 Read the [wiki](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki) for detailed documentation

---

## Links

- [GitHub](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials)
- [Discord](https://discord.gg/dUGAQF2Mga)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/neoessentials)

---

## Previous Versions

Check Modrinth version history for older changelogs.

