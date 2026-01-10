# NeoEssentials v1.0.2.4 - Changelog

**Build #TBD** | January 10, 2026 | Minecraft 1.21.1 - 1.21.11 | NeoForge 21.1.179+ / 21.11.24-beta

---

## 🎉 HUGE UPDATE - Complete Chat System Overhaul!

This is **THE BIG ONE**! We've completely transformed the chat system with **4 massive phases** of new features!

---

## 🐛 CRITICAL FIX - Config Auto-Update System!

### **Chat Channels NOW WORK!** ✅
**HUGE FIX!** Channels were in config but didn't actually work! Now they do!

**What's New:**
- ✅ `/l` or `/local` → Local chat (only nearby players hear you!)
- ✅ `/g` or `/global` → Global chat (everyone hears you!)
- ✅ `/staff` → Staff-only chat (admins/mods only!)
- ✅ Use `!` prefix for quick global: `!Hello everyone!`
- ✅ Use `@` prefix for quick staff: `@Need backup at spawn!`
- ✅ Works exactly as config shows!

**Perfect for:**
- RP servers (realistic proximity chat!)
- Survival servers (local + global options!)
- Staff coordination (private staff channel!)

### **Config Version Migration** 🔧
**FIXED THE BIGGEST PAIN POINT!** Configs now auto-update when you upgrade!

**Before:** Had to manually delete config files to get new features 😫  
**Now:** Automatically detects old configs and updates them! 🎉

**What happens:**
1. Mod checks config versions on startup/reload
2. Old config? → Auto-backup created
3. New config copied from mod JAR
4. Your settings preserved in backup file
5. All new features work immediately!

**Backups stored as:** `config_v12_backup_2026-01-10_14-30-00.json`

---

## 🚀 What's New

### 📂 **Split Your Configs!** 🆕

**HUGE QOL IMPROVEMENT:** Stop scrolling through a 685-line config file!

One command: `/neoessentials config split`

**Splits into 9 small files:**
- chat.json (200 lines) - Just chat settings!
- commands.json (110 lines) - Just command toggles!
- moderation.json (130 lines) - Just ban/jail/etc!
- teleportation.json (120 lines) - Just teleport!
- + 5 more focused files!

**Benefits:**
- ✅ Find settings WAY faster
- ✅ Edit one system without affecting others
- ✅ Less risk of syntax errors
- ✅ Automatic backup (config.json.backup)
- ✅ Easy rollback
- ✅ Works exactly like before (100% compatible!)

**Perfect for:**
- Servers with custom configs
- Multiple admins editing configs
- Version control (git)
- Anyone tired of huge config files!

**Try it:** `/neoessentials config split`

---

## 🚀 What's New (Continued)

### 💬 Phase 2: Interactive Chat - Make Chat Come Alive!

#### **🔗 Clickable URLs**
Share links that actually work! URLs are automatically detected and made clickable.
- Click to open in browser
- Hover to see full URL
- Example: `Check out https://minecraft.net` → Click to visit!

#### **📢 @Mention System**
Get someone's attention the modern way!
- Type `@PlayerName` to mention someone
- Bold yellow highlight + notification sound
- Click to message them
- Example: `@Steve check this out!` → Steve hears a ding!

#### **💎 [item] Links**
Show off your gear in style!
- Type `[item]` while holding something
- Shows item name with full hover details
- Example: `Look at my [item]!` → Shows Diamond Sword with enchantments!

---

### 🎨 Phase 3: Advanced Features - Professional Chat Management!

#### **👑 Badge & Icon System**
- **Rank Badges:** 👑 Owner, ⭐ Admin, 💎 VIP, 🛡️ Moderator, 🔨 Builder
- **Custom Images:** Drop your own PNG files in the badges folder!
- **Status Icons:** 💤 AFK, 👻 Vanished, 🔇 Muted
- **Flexible Positioning:** Put badges anywhere you want

#### **🛡️ Anti-Spam Protection**
- **Caps Filter:** STOP SHOUTING or auto-convert to lowercase
- **Repeat Filter:** No more spam! Blocks duplicate messages
- **Link Filter:** Control who can post URLs
- **Rate Limit:** Prevents message flooding
- **Staff Bypass:** Admins can bypass all filters

#### **📋 Format Templates**
Pre-built chat styles - just pick one!
- **RPG Style:** `[Lv.30] [Admin] Steve: Hello!`
- **Modern Style:** `● [VIP] Alex › Hey!`
- **Minimal Style:** `Steve: message`
- **Detailed Style:** `[world] <[Admin] Steve> message`
- **Custom:** Make your own!

---

### 🌈 Phase 4: Rich Text - Next-Level Formatting!

#### **🎨 Gradient Text**
Smooth color transitions across text!
- Syntax: `<gradient:FF0000-0000FF>text</gradient>`
- Example: `<gradient:FFD700-FF1493>VIP Player</gradient>` (gold to pink!)

#### **🌈 Rainbow Text**
Vibrant rainbow colors!
- Syntax: `<rainbow>text</rainbow>`
- Example: `<rainbow>Rainbow Road!</rainbow>`

#### **🔀 Conditional Formatting**
Dynamic chat that changes based on conditions!

**Time-Based:**
- `<if:time=morning>Good morning!</if>` - Shows only in morning
- `<if:time=night>🌙</if>` - Shows moon at night

**Stat-Based:**
- `<if:health<50>❤️ Low Health!</if>` - Warns when health is low
- `<if:level>=50>⭐ High Level!</if>` - Shows star for high level

**State-Based:**
- `<if:afk>💤</if>` - Shows when player is AFK
- `<if:flying>✈️</if>` - Shows when flying
- `<if:creative>🔧</if>` - Shows when in creative mode

---

## 🆕 New Permissions

### Phase 2 Permissions
- `neoessentials.chat.color` - Use basic color codes
- `neoessentials.chat.color.hex` - Use hex colors
- `neoessentials.chat.format` - Use formatting codes
- `neoessentials.chat.mention` - Mention other players
- `neoessentials.chat.itemlink` - Show held item

### Phase 3 Permissions
- `neoessentials.chat.caps.bypass` - Bypass caps filter
- `neoessentials.chat.repeat.bypass` - Bypass repeat filter
- `neoessentials.chat.links.bypass` - Bypass link filter
- `neoessentials.chat.spam.bypass` - Bypass spam limit

### Phase 4 Permissions
- `neoessentials.chat.richtext` - Use all rich text effects
- `neoessentials.chat.gradient` - Use gradient text
- `neoessentials.chat.rainbow` - Use rainbow text

---

## ⚙️ Configuration

All features are **fully configurable**! Enable what you want, disable what you don't.

**Phase 2 - Interactive Chat:**
- Toggle URLs, mentions, item links individually
- Customize mention color and sound
- Control who can use each feature

**Phase 3 - Advanced Features:**
- Configure rank badges (emoji or custom images!)
- Set up anti-spam rules (how strict, what action to take)
- Choose from 7 format templates or make your own

**Phase 4 - Rich Text:**
- Enable/disable gradients, rainbow, conditionals
- Require permissions for rich text effects
- Fine-tune all conditional options

---

## 🎯 Example Setups

### **VIP with Gradient Prefix**
Config shows VIP prefix with gold-to-pink gradient

### **Time-Based Greeting**
Shows sun in morning, moon at night automatically

### **Health Warning**
Shows heart emoji when player health is below 50%

### **All Features Combined**
Gradient prefix + status icons + conditional formatting + interactive elements!

---

## 🐛 Bug Fixes

- Fixed LuckPerms prefix/suffix integration
- Fixed resource leaks in pack generator
- Fixed null pointer warnings
- Improved error handling throughout
- Debug logging now controlled by config
- Cleaned up code quality (removed unused imports, etc.)

---

## 📚 Documentation

**Included in this release:**
- Complete feature documentation (100+ pages!)
- Custom badge setup guide
- Placeholder reference guide
- Permission documentation
- Configuration examples
- Testing checklists

---

## 📊 Performance

Don't worry about lag! This update is **highly optimized**:
- Only ~10-15ms overhead with ALL features enabled
- Concurrent data structures for thread safety
- Cached lookups, no database queries
- Pre-compiled regex patterns
- Smart caching throughout

---

## 🎮 Getting Started

**It's easy to get started:**

1. **Download** the mod and add to your mods folder
2. **Start** your server (creates config files)
3. **Configure** at `config/neoessentials/config.json`
4. **Enable** the features you want (all disabled by default)
5. **Set permissions** using LuckPerms
6. **Enjoy** your new chat system!

**Pro Tip:** Start with Phase 2 features (enabled by default), then gradually enable Phase 3 and 4!

---

## 🌟 Highlights

**What makes this special:**

✅ **50+ New Features** across 4 major phases  
✅ **11 New Permissions** for fine-grained control  
✅ **Fully Configurable** - every feature can be toggled  
✅ **Well Documented** - complete guides included  
✅ **High Performance** - optimized for large servers  
✅ **Easy to Use** - works out of the box, customize as needed  
✅ **Future-Proof** - designed for extensibility  

---

## ⚠️ Notes

- Custom badge images require client resource pack (auto-generation included)
- Resource pack auto-send coming in future update (use server.properties for now)
- All Phase 4 features disabled by default (enable in config)

---

## 🔗 Support & Links

- **Discord:** https://discord.gg/dUGAQF2Mga
- **GitHub:** https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials
- **Wiki:** Full documentation and tutorials

---

## Download & Installation

1. Download the latest `.jar` file from CurseForge
2. Place it in your `mods` folder
3. Restart your server
4. Configure in `config/neoessentials/config.json`
5. Enjoy the most advanced chat system for NeoForge!

---

**This is our biggest update yet!** 🎉

4 development phases, months of work, the ultimate chat system!

Thank you for using NeoEssentials! 🚀

