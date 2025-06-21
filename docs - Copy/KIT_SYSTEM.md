# NeoEssentials Kit System

This document describes the kit system in NeoEssentials, including all commands and features.

## Overview

The kit system allows server administrators to create pre-defined sets of items (kits) that players can claim. Features include:

- Kit cooldowns to prevent abuse
- Permission-based access control
- Economy integration with kit pricing
- Interactive preview of kit contents
- Hoverable kit list with kit information

## Commands

### Player Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/kit <name>` | Claim a kit | `neoessentials.command.kit` + `neoessentials.command.kit.<kitname>` |
| `/kits` | View list of available kits | `neoessentials.command.kit.list` |
| `/previewkit <name>` | Preview a kit's contents | `neoessentials.command.kit.preview` |
| `/kithelp` | Display help for kit commands | `neoessentials.command.kit` |

### Admin Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/createkit <name> [cooldown] [price]` | Create a new kit | `neoessentials.command.kit.create` |
| `/deletekit <name>` | Delete a kit | `neoessentials.command.kit.delete` |
| `/givekit <player> <kit>` | Give a kit to a player | `neoessentials.command.kit.give` |

## Kit Features

### Economy Integration

Kits can have a price that players need to pay when claiming them. When a player purchases a kit:

1. The player's balance is checked against the kit price
2. The amount is deducted from the player's account
3. An economy transaction is recorded with the reason "Purchased kit: [name]"
4. The kit items are given to the player

### Interactive Kit List

The `/kits` command shows an enhanced list with:

- Color coding to indicate availability:
  - Green: Available for immediate use
  - Yellow: Available but not enough money to purchase
  - Red: On cooldown
  - Gray: No permission

- Hoverable names that display:
  - Kit permission requirements
  - Cooldown information
  - Price and affordability
  - Remaining cooldown time

- Clickable kit names that automatically run the `/kit <name>` command

### Kit Preview

The `/previewkit <name>` command shows:

1. Kit name and details (cooldown, price)
2. Total item count
3. Most notable items in the kit
4. Kit availability status
5. Clickable button to claim the kit directly

## Implementation Details

### Kit Storage

Kits are stored in `neoessentials/kits.json` with the following structure:

```json
{
  "kits": {
    "kitname": {
      "cooldown": 3600,
      "permission": "neoessentials.command.kit.kitname",
      "price": 100.0,
      "items": [
        {
          "id": "minecraft:diamond_sword",
          "count": 1
        },
        ...
      ]
    },
    ...
  },
  "cooldowns": {
    "player-uuid": {
      "kitname": 1623456789000
    }
  }
}
```

### Kit Class Structure

The `Kit` class contains:

- Name, cooldown, permission, and price properties
- List of `ItemDefinition` objects representing the kit contents
- Methods for manipulating and accessing kit data

The kit system integrates with the economy system for kit pricing and records transactions when kits are purchased.

## Permission Notes

- `neoessentials.kit.admin` permission allows bypassing all kit restrictions
- Each kit has an auto-generated permission of `neoessentials.command.kit.<kitname>`
- Kit creators can specify custom permissions if needed
