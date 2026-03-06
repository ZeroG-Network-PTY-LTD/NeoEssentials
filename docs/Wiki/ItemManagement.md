# Item Management

> **Version:** 1.0.2.6

---

## Overview

Item management commands for repair, disposal, enchanting, inventory management, powertool bindings, and item utilities.

---

## Commands

### Repair & Disposal

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/repair` | `/repair [hand\|all]` | `neoessentials.item.repair` | Repair held item or all items |
| `/fix` | alias | same | Alias |
| `/dispose` | `/dispose` | `neoessentials.item.dispose` | Open a disposal chest (items placed are destroyed) |
| `/trash` | alias | same | Alias |

### Inventory

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/clearinventory` | `/clearinventory [player]` | `neoessentials.item.clearinventory` | Clear inventory |
| `/ci` | alias | same | Alias |
| `/ciconfirmtoggle` | `/ciconfirmtoggle` | same | Toggle confirmation prompt for `/ci` |
| `/invsee` | `/invsee <player>` | `neoessentials.invsee` | View a player's inventory |
| `/inv` | alias | same | Alias |
| `/enderchest` | `/enderchest [player]` | `neoessentials.enderchest` | View a player's ender chest |
| `/ec` | alias | same | Alias |
| `/condense` | `/condense [item]` | `neoessentials.condense` | Compact loose items to storage blocks |

### Item Customisation

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/itemname` | `/itemname [name\|-]` | `neoessentials.itemname` | Rename held item (omit or `-` to clear) |
| `/rename` | alias | same | Alias |
| `/itemlore` | `/itemlore add\|set <n>\|remove <n>\|clear <text>` | `neoessentials.itemlore` | Edit held item lore lines |
| `/more` | `/more [amount]` | `neoessentials.more` | Fill held stack to max |
| `/hat` | `/hat` | `neoessentials.hat` | Wear held item as helmet |
| `/item` | `/item <id> [amount]` | `neoessentials.item.give` | Give yourself an item by registry ID |
| `/i` | alias | same | Alias |
| `/skull` | `/skull [player]` | `neoessentials.skull` | Get a player head item |

### Enchanting

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/enchant` | `/enchant <enchantment> [level]` | `neoessentials.item.enchant` | Enchant held item |
| `/potion` | `/potion <add\|remove\|clear> <effect> [duration] [amp]` | `neoessentials.potion` | Edit potion effects on held potion |

### Powertool

| Command | Syntax | Permission | Description |
|---|---|---|---|
| `/powertool` | `/powertool <command>` | `neoessentials.item.powertool` | Bind a command to held item type |
| `/powertool remove` | `/powertool remove` | same | Remove powertool from held item |
| `/powertool list` | `/powertool list` | same | List all your powertool bindings |
| `/ptool` | alias | same | Alias |
| `/powertooltoggle` | `/powertooltoggle` | `neoessentials.item.powertool` | Toggle all powertools on/off |
| `/ptt` | alias | same | Alias |
| `/powertoollist` | `/powertoollist` | `neoessentials.item.powertool` | List active powertool bindings |
| `/ptlist` | alias | same | Alias |

**How powertools work:**
- Bindings are stored by **item type** (not slot) — the command travels with the item regardless of which inventory slot it's in
- Right-clicking with the item (on a block, mob, or in air) executes the bound command as you
- `/powertooltoggle` globally enables/disables all your powertools without removing bindings
- Use `@p <command>` syntax to execute the command on all other online players

### Portable Workstations

| Command | Permission | Description |
|---|---|---|
| `/anvil` | `neoessentials.anvil` | Open portable anvil |
| `/workbench` / `/crafting` / `/craft` | `neoessentials.workbench` | Open portable crafting table |
| `/grindstone` | `neoessentials.grindstone` | Open portable grindstone |
| `/smithing` | `neoessentials.smithing` | Open portable smithing table |
| `/stonecutting` / `/stonecutter` | `neoessentials.stonecutting` | Open portable stonecutter |
| `/loom` | `neoessentials.loom` | Open portable loom |
| `/cartography` | `neoessentials.cartography` | Open portable cartography table |

---

*Back to [Wiki Home](Home)*
