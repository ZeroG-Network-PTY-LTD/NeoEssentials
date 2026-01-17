﻿# NeoEssentials v1.0.2.4 - Changelog

**Build #769** | January 14, 2026 | Minecraft 1.21.1 - 1.21.11 | NeoForge 21.1.179+ / 21.11.24-beta

---

## 🌍 **NEW: Custom Language System**

**Complete localization support - create custom language files for any language!**

### ✅ **Hardcoded Strings Removed**
Fixed **42 hardcoded English strings** that couldn't be translated:
- ✅ WhoisCommand (12 strings) - Player info, status, health, etc.
- ✅ ListCommand (8 strings) - Online players list, headers, separators
- ✅ HelpopCommand (5 strings) - Help request notifications
- ✅ DashboardCommand (3 strings) - Dashboard status display
- ✅ AdminEndpoint (2 strings) - Server restart/shutdown messages
- ✅ Config Splitter (12 strings) - Configuration migration notices

**All user-facing messages now support translation!**

See full documentation in `docs/CUSTOM_LANGUAGES.md`

**Commands:** `/language list`, `/language template <code>`, `/language reload`

**Quick Start:**
1. `/language template es_es` - Generate template
2. Edit `neoessentials/languages/templates/es_es_template.json`
3. Save as `neoessentials/languages/custom/es_es.json`
4. `/language reload` - Load it!

Supports: en_us, es_es, fr_fr, de_de, it_it, pt_br, ru_ru, ja_jp, ko_kr, zh_cn, ar_sa (RTL), he_il (RTL), and more!

---

## 🎯 **Combat System Improvements**

### **Fixed False-Positive Combat Detection** âœ…

Players were experiencing teleportation blocks even when not in combat. This has been completely fixed!

#### **The Problem**
- âŒ Combat timeout was too long (10 seconds â†’ players waited forever)
- âŒ Environmental damage (fall, fire, hunger) triggered combat status
- âŒ Movement threshold too strict (1.0 blocks â†’ caught network lag)
- âŒ No feedback on remaining combat time
- âŒ No debug logging to identify issues

#### **The Solution**

**Combat Timeout Reduced: 10s â†’ 5s**
- Better user experience
- Still prevents combat logging
- Industry standard timing

**Environmental Damage Excluded**
- Only PvP and mob combat triggers combat status
- Fall damage â†’ NOT combat âŒ
- Fire/Lava â†’ NOT combat âŒ
- Drowning â†’ NOT combat âŒ
- Hunger/Poison â†’ NOT combat âŒ
- Mining/Building â†’ NOT combat âŒ
- Attacked by player â†’ IS combat âœ…
- Attacked by mob â†’ IS combat âœ…
- Attacking player â†’ IS combat âœ…
- Attacking mob â†’ IS combat âœ…

**Movement Threshold Improved: 1.0 â†’ 1.5 blocks**
- Prevents false cancellations from network lag
- Tolerates small position shifts from server ticks
- Still catches intentional movement

**Better Feedback Messages**
- **Before:** "Teleportation is disabled while in combat!"
- **After:** "You cannot teleport while in combat! Please wait 3 second(s)."
- Shows exact remaining combat time
- Clear, actionable feedback

**Debug Logging Added**
- Tracks what triggers combat status
- Logs teleport cancellations with distance moved
- Auto-cleanup of expired combat entries
- Better troubleshooting for server admins

#### **New CombatTracker Methods**
```java
markInCombat(player)           // Mark player in combat (5s)
isInCombat(player)             // Check combat status (auto-cleanup)
getRemainingCombatTime(player) // Get seconds remaining
clearCombat(player)            // Clear status (logout)
clearAll()                     // Clear all data (shutdown)
```

#### **Configuration**
```json
"teleportation": {
  "generalSettings": {
    "allowTeleportInCombat": false,  // Block teleports during combat
    "cancelOnMovement": true,        // Cancel warmup if moved >1.5 blocks
    "cancelOnDamage": false,         // Cancel warmup if damaged
    "teleportDelay": 3               // Warmup time in seconds
  }
}
```

**Recommendations:**
- **PvP Servers:** Keep `allowTeleportInCombat: false` (default)
- **PvE Servers:** Can set `allowTeleportInCombat: true` if desired
- **Strict Servers:** Enable `cancelOnDamage: true` for extra security
- **Lenient Servers:** Disable `cancelOnMovement: false` for no restrictions

#### **User Impact**

**Before Fix:**
- ðŸ˜¤ Can't teleport after mining/building
- ðŸ˜¤ Can't teleport after falling
- ðŸ˜¤ Can't teleport after walking through fire
- ðŸ˜¤ Generic error messages
- ðŸ˜¤ 10-second wait times
- ðŸ˜¤ Network lag causes random failures

**After Fix:**
- ðŸ˜Š Normal gameplay doesn't block teleports
- ðŸ˜Š Only actual combat matters
- ðŸ˜Š Clear feedback with countdown timer
- ðŸ˜Š 5-second combat timeout
- ðŸ˜Š Network lag tolerance
- ðŸ˜Š No more false positives

---

## ðŸ“Š **Testing Results**

âœ… **All test cases passed:**
- Fall damage does NOT trigger combat
- Fire/lava damage does NOT trigger combat
- Drowning does NOT trigger combat
- Hunger/poison does NOT trigger combat
- Attacking mob DOES trigger combat (5s timeout)
- Being attacked by mob DOES trigger combat (5s timeout)
- PvP DOES trigger combat (5s timeout)
- Combat clears after 5 seconds
- Remaining time shown in error message
- Movement <1.5 blocks doesn't cancel teleport
- Movement >1.5 blocks DOES cancel teleport
- Expired combat entries auto-cleanup
- Debug logs show combat triggers correctly

---

## ðŸ”§ **Technical Details**

### **Files Modified**
1. `CombatTracker.java` - Improved tracking with auto-cleanup and remaining time
2. `CombatEventHandler.java` - Environmental damage filtering
3. `TeleportUtil.java` - Better feedback messages and movement threshold

### **Lines Changed:** ~150 lines
### **New Methods:** 3 (`getRemainingCombatTime`, `clearAll`, improved logging)
### **Breaking Changes:** None (fully backwards compatible)

---

## ðŸš€ **Installation & Upgrade**

1. Download `neoessentials-1.0.2.4+build.765.jar`
2. Stop your server
3. Replace old NeoEssentials jar in `/mods` folder
4. Start server
5. Review combat config in `config/neoessentials/config.json` (optional)
6. Test teleportation functionality

**No config migration needed** - all existing settings remain valid.

---

## ðŸ“ **Known Issues**

None! This build is stable and ready for production use.

---

## ðŸ’¬ **Support**

Having issues? Report them on:
- **GitHub Issues:** [NeoEssentials Issues](https://github.com/yourusername/NeoEssentials/issues)
- **Discord:** [Join our Discord](#)
- **CurseForge:** [NeoEssentials Page](#)

---

**Full Changelog:** See `COMBAT_SYSTEM_FIX.md` for complete technical documentation.
