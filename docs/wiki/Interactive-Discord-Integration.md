# Interactive Discord Integration

## Overview

The Interactive Discord Integration brings advanced features similar to the popular "InteractiveChat DiscordSRV Addon" to NeoEssentials. This system allows players to share items, inventories, and other game content directly in Discord with rich visual previews and interactive elements.

## Features

### 🎒 Inventory Sharing
- **[inventory]** - Share your inventory
- **[inventory:PlayerName]** - Share another player's inventory (with permissions)
- Rich Discord embeds with item previews
- Permission-based access control

### 📦 Item Display
- **[item]** - Share your held item
- Detailed item information including:
  - Item name and count
  - Enchantments and their levels
  - Durability information
  - Custom NBT data
  - Item rarity and descriptions

### 🔮 Ender Chest Preview
- **[enderchest]** - Share your ender chest contents
- **[enderchest:PlayerName]** - Share another player's ender chest (with permissions)
- Visual representation of stored items

### 🔄 Automatic Chat Processing
- Real-time chat message scanning
- Automatic replacement of tags with Discord embeds
- Seamless integration with existing Discord webhooks

## Commands

### Player Commands

#### `/ditem` - Share Current Item
```
/ditem
```
**Description:** Shares your currently held item to Discord with detailed information.

**Permissions:** `neoessentials.discord.item` (Default: All players)

**Usage Examples:**
- Hold a diamond sword and use `/ditem` to share its stats
- Share enchanted items with their enchantment details
- Display custom items with their descriptions

#### `/dinv [player]` - Share Inventory
```
/dinv                    # Share your inventory
/dinv PlayerName         # Share another player's inventory
```
**Description:** Creates a Discord embed showing inventory contents with item previews.

**Permissions:** 
- `neoessentials.discord.inventory.self` - Share own inventory
- `neoessentials.discord.inventory.others` - View other players' inventories

**Features:**
- Grid-based visual representation
- Item count and enchantment indicators
- Empty slot visualization
- Hotbar highlighting

#### `/dender [player]` - Share Ender Chest
```
/dender                  # Share your ender chest
/dender PlayerName       # Share another player's ender chest
```
**Description:** Displays ender chest contents in Discord with visual preview.

**Permissions:**
- `neoessentials.discord.enderchest.self` - Share own ender chest
- `neoessentials.discord.enderchest.others` - View other players' ender chests

## Chat Integration

### Interactive Tags

Use these tags in regular chat messages to automatically generate Discord previews:

#### Item Tags
```
[item]                   # Your held item
Check out my [item]!     # Embedded in normal chat
```

#### Inventory Tags
```
[inventory]              # Your inventory
[inventory:Steve]        # Steve's inventory (with permission)
Look at my gear: [inventory]
```

#### Ender Chest Tags
```
[enderchest]             # Your ender chest
[enderchest:Alex]        # Alex's ender chest (with permission)
My storage: [enderchest]
```

### Message Processing

The system automatically:
1. Scans chat messages for interactive tags
2. Validates permissions for cross-player access
3. Generates rich Discord embeds
4. Replaces tags with preview text
5. Sends enhanced messages to Discord

## Permissions

### Basic Permissions
- `neoessentials.discord.interactive` - Access to interactive features
- `neoessentials.discord.item` - Share items to Discord
- `neoessentials.discord.inventory.self` - Share own inventory
- `neoessentials.discord.enderchest.self` - Share own ender chest

### Advanced Permissions
- `neoessentials.discord.inventory.others` - View other players' inventories
- `neoessentials.discord.enderchest.others` - View other players' ender chests
- `neoessentials.discord.admin` - Admin access to all interactive features

### Permission Examples

**For Regular Players:**
```yaml
groups:
  default:
    permissions:
      - neoessentials.discord.interactive
      - neoessentials.discord.item
      - neoessentials.discord.inventory.self
      - neoessentials.discord.enderchest.self
```

**For Moderators:**
```yaml
groups:
  moderator:
    permissions:
      - neoessentials.discord.*
```

## Configuration

### Discord Webhook Setup

The Interactive Discord Integration uses your existing Discord webhook configuration. Ensure you have:

```json
{
  "discord": {
    "webhook_url": "your_webhook_url_here",
    "enabled": true,
    "interactive_features": {
      "enabled": true,
      "embed_color": "#00ff00",
      "max_items_display": 27,
      "show_empty_slots": true,
      "show_enchantments": true,
      "show_durability": true
    }
  }
}
```

### Feature Configuration

```json
{
  "interactive_discord": {
    "enabled": true,
    "auto_process_chat": true,
    "permissions_required": true,
    "embed_settings": {
      "color": "#3498db",
      "show_thumbnails": true,
      "max_field_length": 1024,
      "include_player_avatar": true
    },
    "inventory_settings": {
      "show_hotbar_separately": true,
      "highlight_valuable_items": true,
      "group_similar_items": false
    }
  }
}
```

## Discord Embed Features

### Item Embeds
- **Title:** Item name with enchantment indicators
- **Description:** Item lore and custom descriptions
- **Fields:** 
  - Enchantments with levels
  - Durability status
  - Item count
  - Special properties
- **Thumbnail:** Item texture (if available)
- **Color:** Based on item rarity

### Inventory Embeds
- **Title:** Player's Inventory
- **Description:** Inventory summary with valuable items
- **Fields:**
  - Hotbar (slots 0-8)
  - Main inventory (slots 9-35)
  - Armor slots
  - Offhand slot
- **Footer:** Timestamp and server information

### Ender Chest Embeds
- **Title:** Player's Ender Chest
- **Description:** Storage summary
- **Fields:** All 27 ender chest slots
- **Visual:** Grid representation with item icons

## Technical Details

### Integration Points
- **Chat Events:** Intercepts player chat for tag processing
- **Discord Manager:** Uses existing webhook infrastructure
- **Permission System:** Integrates with NeoEssentials permission framework
- **Command Framework:** Brigadier-based command registration

### Performance Features
- **Caching:** Item and inventory data caching for performance
- **Rate Limiting:** Prevents Discord API spam
- **Async Processing:** Non-blocking Discord message sending
- **Memory Management:** Efficient inventory processing

## Troubleshooting

### Common Issues

**Tags not working in chat:**
- Verify Discord integration is enabled
- Check player permissions
- Ensure webhook URL is configured

**Permission denied errors:**
- Verify player has required permissions
- Check if trying to access another player's data
- Confirm admin permissions for cross-player access

**Discord embeds not appearing:**
- Verify webhook URL is valid
- Check Discord server permissions
- Ensure webhook channel permissions

**Commands not registered:**
- Check server logs for registration errors
- Verify NeoEssentials is properly loaded
- Restart server if necessary

### Debug Commands

```
/neoessentials debug discord    # Show Discord integration status
/neoessentials reload discord   # Reload Discord configuration
```

## Examples

### Sharing a Powerful Item
**Chat:** `Check out my new [item]! It's amazing!`

**Discord Result:**
```
Steve shared: Check out my new Netherite Sword! It's amazing!

[Discord Embed]
🗡️ Netherite Sword
✨ Sharpness V, Unbreaking III, Mending
⚡ 2031/2031 Durability
💎 Epic Quality
```

### Showing Off Gear
**Command:** `/dinv`

**Discord Result:**
```
[Discord Embed]
🎒 Steve's Inventory

Hotbar:
🗡️ Netherite Sword  🏹 Bow  🍞 Bread x64  💎 Diamond x32

Armor:
👑 Netherite Helmet  🛡️ Netherite Chestplate
👖 Netherite Leggings  👢 Netherite Boots

Storage: 28/36 slots used
Total Value: ~2,847 diamonds
```

## Advanced Usage

### Moderator Tools
- Use `/dinv PlayerName` to inspect player inventories
- Monitor `/dender PlayerName` for ender chest contents
- Cross-reference with player reports and investigations

### Community Features
- Players can showcase rare items and achievements
- Share inventory setups for different activities
- Demonstrate storage organization techniques

### Server Events
- Item showcases and competitions
- Inventory inspections for events
- Treasure hunt result sharing

## API Integration

### For Developers

The Interactive Discord Integration provides hooks for custom integrations:

```java
// Send custom item to Discord
InteractiveChatDiscordIntegration.sendItemToDiscord(player, itemStack);

// Send custom inventory preview
InteractiveChatDiscordIntegration.sendInventoryToDiscord(viewer, target);

// Process custom chat message
String processed = InteractiveChatDiscordIntegration.processChatMessage(player, message);
```

## Related Features

- [Discord Integration](Discord-Integration.md) - Basic Discord webhook setup
- [Permission System](Permission-System.md) - Permission configuration
- [Command System](Command-System.md) - Command framework overview
- [Player Management](Player-Management.md) - Player data and utilities

---

*This feature brings Minecraft and Discord closer together, creating seamless sharing experiences for your server community.*
