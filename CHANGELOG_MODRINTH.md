# NeoEssentials v1.0.2.4 - Changelog

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

