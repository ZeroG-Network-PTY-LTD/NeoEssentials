# NeoEssentials v1.0.2.4 - Changelog

**Build #TBD** | January 7, 2026 | Minecraft 1.21.1 - 1.21.11 | NeoForge 21.1.179+ / 21.11.24-beta

---

## 🎯 What's New

### 💬 **Interactive Chat Features** - The Future of Minecraft Chat! ✨

Your chat just got a MASSIVE upgrade! Say goodbye to boring text and hello to modern, interactive communication!

#### **🔗 Clickable URLs**
Share links that actually work! URLs are automatically detected and made clickable.
- **Click:** Opens in your browser
- **Hover:** See the full URL
- **Example:** `Check out https://minecraft.net` → Click to visit!

#### **📢 @Mention System**
Get someone's attention the modern way!
- **How:** Type `@PlayerName` in chat
- **Result:** Bold yellow highlight + notification sound
- **Click:** Suggest message to that player
- **Example:** `@Steve check this out!` → Steve gets notified!

#### **💎 [item] Links**
Show off your gear in style!
- **How:** Type `[item]` while holding something
- **Result:** Clickable item name
- **Hover:** Full item details (enchantments, durability, etc.)
- **Example:** `Look at my [item]!` → Shows your Diamond Sword!

#### **🎨 Full Color System**
Make your chat colorful with complete permission control!
- Basic colors: `&0-9, &a-f` (16 colors)
- Hex colors: `&#FF5500` (16.7 million colors!)
- Formatting: Bold, italic, underline, strikethrough, etc.

### **⚙️ Configuration**
Everything is configurable! Enable/disable features individually:
```json
{
  "enableChatEnhancements": true,    // Master toggle
  "autoLinkUrls": true,              // Clickable URLs
  "allowItemLinks": true,            // [item] placeholder
  "mentions": {
    "enabled": true,                 // @mentions
    "playSound": true,               // Sound on mention
    "highlightColor": "&e"           // Yellow highlight
  }
}
```

### **🔐 New Permissions**
```
neoessentials.chat.color       - Color codes
neoessentials.chat.color.hex   - Hex colors
neoessentials.chat.format      - Formatting
neoessentials.chat.mention     - @mentions
neoessentials.chat.itemlink    - [item] links
```

### **✨ Example Chat**
```
Before: <Player> Check out my sword at example.com
After:  <Player> Check out my [Diamond Sword] at example.com
                              ^^^^^^^^^^^^^^^^    ^^^^^^^^^^^
                              Clickable item      Clickable URL
```

---

## Download & Installation

1. Download the latest `.jar` file from CurseForge
2. Place it in your `mods` folder
3. Restart your server/client
4. Configure in `config/neoessentials/config.json`

---

## Previous Versions

For older changelogs, check the version history on CurseForge.

