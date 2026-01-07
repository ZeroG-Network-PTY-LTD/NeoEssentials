# NeoEssentials v1.0.2.4 - Changelog

**Build #TBD** | January 7, 2026 | Minecraft 1.21.1 - 1.21.11 | NeoForge 21.1.179+ / 21.11.24-beta

---

## 🚀 What's New

### 💬 **Phase 2: Interactive Chat Enhancements** - NEW! ✨

Transform your chat with modern, interactive features that bring your server communication to life!

#### **Clickable URLs** 🔗
- **Auto-Detection:** URLs starting with `http://` or `https://` are automatically detected
- **Click Action:** Opens in browser
- **Visual:** Blue and underlined
- **Hover:** Shows "Click to open in browser"
- **Example:** `Check out https://minecraft.net!` → Clickable blue link

#### **@Mention System** 📢
- **Syntax:** Type `@PlayerName` to mention someone
- **Highlighting:** Bold + yellow (configurable color)
- **Sound Notification:** Plays experience orb sound to mentioned player
- **Click Action:** Suggests `/msg PlayerName` command
- **Smart:** Only works for online players, no sound when mentioning yourself
- **Example:** `@PlayerB come to spawn` → Bold yellow, plays sound to PlayerB

#### **[item] Links** 💎
- **Syntax:** Type `[item]` to show your held item
- **Display:** Item name in aqua/underlined
- **Hover:** Full item details (enchantments, durability, etc.)
- **Empty Hand:** Shows "[Empty Hand]" if no item
- **Example:** `Check out my [item]!` → `[Diamond Sword]` with full hover details

#### **New Permissions**
```
neoessentials.chat.color         - Use basic color codes (&0-9, &a-f)
neoessentials.chat.color.hex     - Use hex colors (&#RRGGBB)
neoessentials.chat.format        - Use formatting codes (&l, &o, &k, etc.)
neoessentials.chat.staff         - Access staff chat channel
neoessentials.chat.mention       - Mention other players with @name
neoessentials.chat.mention.all   - Mention everyone (reserved for future)
neoessentials.chat.itemlink      - Show held item in chat with [item]
```

#### **New Configuration Options**
```json
{
  "chat": {
    "enableChatEnhancements": true,
    "autoLinkUrls": true,
    "allowItemLinks": true,
    "mentions": {
      "enabled": true,
      "highlightColor": "&e",
      "playSound": true,
      "soundName": "entity.experience_orb.pickup",
      "soundVolume": 1.0
    }
  }
}
```

#### **Files Added**
- `ChatEnhancer.java` - Interactive chat processing engine

#### **Files Modified**
- `ChatFormatter.java` - Integrated ChatEnhancer
- `PermissionRegistry.java` - Added 7 new permissions
- `config.json` - Added Phase 2 configuration section

---

## Previous Releases

See the [Releases](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/releases) page for previous version changelogs.

