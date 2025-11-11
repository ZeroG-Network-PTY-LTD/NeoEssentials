## Item Management System

### Features
- **Permission-based Item Spawning**: Controlled by `permission-based-item-spawn` in config. Requires specific permissions for item spawning, with support for general, specific, and wildcard permissions.
- **Item Spawn Blacklist**: Items listed in `item-spawn-blacklist` cannot be spawned, even with permissions.
- **Oversized Stack Sizes**: Configurable via `oversized-stacksize` and `default-stack-size`. Allows stacks larger than vanilla, or overrides vanilla stack sizes.
- **Unsafe Enchantments**: Controlled by `unsafe-enchantments` and `max-unsafe-enchantment-level`. Allows enchantments beyond vanilla limits.
- **Powertool System**: Assign commands to items, executed on right-click. Permission required: `neoessentials.item.powertool`.
- **Dispose System**: Allows players to dispose of items via a chest interface.
- **Clear Inventory**: Command to clear player inventory.
- **Repair System**: Command to repair items.
- **Enchantment System**: Commands to enchant items, including unsafe levels if enabled.

### Key Config Options (`config.json`)
- `permission-based-item-spawn`: Enable permission checks for item spawning.
- `item-spawn-blacklist`: List of item IDs that cannot be spawned.
- `oversized-stacksize`: Max stack size for oversized stacks.
- `default-stack-size`: Default stack size override (-1 for vanilla).
- `unsafe-enchantments`: Allow unsafe enchantments.
- `max-unsafe-enchantment-level`: Max level for unsafe enchantments.

### Key Classes
- `ItemStackHelper`: Handles stack size logic, config-aware.
- `ItemSpawnHelper`: Manages item spawning, permission checks, blacklist enforcement.
- `ItemInteractionHandler`: Handles powertool item interactions.
- `ItemEventHandler`: Placeholder for future item event logic (API limitations).

### Commands
- `/repair`
- `/dispose`
- `/powertool`, `/powertooltoggle`
- `/clearinventory`
- `/enchant`, `/enchanthand`

### Permissions
- `neoessentials.item.spawn`
- `neoessentials.item.spawn.<namespace>.<item>`
- `neoessentials.item.spawn.<namespace>.*`
- `neoessentials.item.powertool`

### Limitations
- "drop-items-if-full" is documented but not enforceable due to NeoForge API limitations.
