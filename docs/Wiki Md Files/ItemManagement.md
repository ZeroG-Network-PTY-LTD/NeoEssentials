# Item Management System

## Config File: config.json (items section)

This system manages item spawning, stack sizes, enchantments, and item blacklists. It does not provide any shop, trading, or buy/sell functionality.

### Options
- `permission-based-item-spawn`: Require permission to spawn items
- `oversized-stacksize`: Maximum stack size for oversized stacks
- `default-stack-size`: Default stack size for all items (-1 = vanilla)
- `unsafe-enchantments`: Allow enchantments beyond vanilla limits
- `max-unsafe-enchantment-level`: Maximum level for unsafe enchantments
- `item-spawn-blacklist`: List of items that cannot be spawned via commands

### Example
```json
{
  "permission-based-item-spawn": false,
  "oversized-stacksize": 64,
  "default-stack-size": -1,
  "unsafe-enchantments": true,
  "max-unsafe-enchantment-level": 32767,
  "item-spawn-blacklist": [
    "minecraft:bedrock",
    "minecraft:barrier",
    "minecraft:command_block",
    "minecraft:chain_command_block",
    "minecraft:repeating_command_block",
    "minecraft:structure_block",
    "minecraft:jigsaw",
    "minecraft:debug_stick",
    "minecraft:knowledge_book"
  ]
}
```

---

This system is for item management only. There is no shop, trading, or buy/sell feature in NeoEssentials.

For more details, see the main documentation or ask in the Discord support server.