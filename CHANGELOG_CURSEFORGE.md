# NeoEssentials v1.0.2.4 - Changelog

## 🌍 NEW: Custom Language System

**Create and use custom language files for full translation support!**

### ✅ **42 Hardcoded Strings Fixed!**
All user-facing messages now support translation:
- WhoisCommand, ListCommand, HelpopCommand
- DashboardCommand, AdminEndpoint
- Config Splitter notifications

**100% translatable - no more English-only messages!**

### Features
- ✅ **Custom language files** - Add any language you want
- ✅ **Template generation** - Auto-generate translation templates
- ✅ **Missing key tracking** - Find untranslated messages
- ✅ **Live reloading** - Update translations without restart
- ✅ **RTL support** - Right-to-left languages (Arabic, Hebrew, etc.)
- ✅ **Metadata tracking** - Author, version, language info

### Commands
- `/language list` - List all custom languages
- `/language reload` - Reload language files
- `/language stats` - Show statistics
- `/language template <code>` - Generate template (e.g., `/language template es_es`)
- `/language exportmissing` - Export missing translation keys
- `/language clearmissing` - Clear missing keys tracker
- `/language info` - Show help and information

### How to Create a Language
1. Run `/language template es_es` (or your language code)
2. Edit the template file in `neoessentials/languages/templates/`
3. Translate the text (keep `{0}` placeholders intact!)
4. Save as `es_es.json` in `neoessentials/languages/custom/`
5. Run `/language reload`

### Supported Languages
en_us, es_es, fr_fr, de_de, it_it, pt_br, ru_ru, ja_jp, ko_kr, zh_cn, nl_nl, and many more!

📖 **Full documentation:** See `docs/CUSTOM_LANGUAGES.md`

---

## 🎯 Combat System Fixes

**Fixed teleportation issues when moving/after combat!**

- ✅ Combat timeout reduced: 10s → 5s
- ✅ Environmental damage (fall, fire, hunger) no longer triggers combat
- ✅ Movement threshold improved: 1.0 → 1.5 blocks (less false cancellations)
- ✅ Better messages: Shows remaining combat time
- ✅ Auto-cleanup of expired combat entries
- ✅ Debug logging to track issues

**What triggers combat:**
- Attacking or being attacked by players/mobs ✅
- Fall damage, fire, drowning, hunger ❌ (NOT combat!)

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

