package com.zerog.neoessentials.items.handlers;

/**
 * Item event handler for config-aware item behavior.
 * 
 * NOTE: NeoForge 1.21+ API Limitation
 * ===================================
 * [REMOVED] The "drop-items-if-full" config option and related logic have been commented out.
 * Original config:
 *   "drop-items-if-full": false,
 *   "drop-items-if-full_comment": "[NeoForge API Limitation] Cannot currently be enforced - ItemEntityPickupEvent.Pre doesn't support cancellation. Config option exists for future implementation. Original intent: Allow items to be picked up when inventory is full (excess dropped). If false, items remain on ground until space is available",
 * This feature is documented but not enforceable until the API changes.
 * 
 * Working Features:
 * - Oversized stack sizes (via ItemStackHelper)
 * - Default stack size override (via ItemStackHelper)
 * - Permission-based item spawning (via ItemSpawnHelper)
 */
public class ItemEventHandler {
    // This class is registered for future expansion when NeoForge provides
    // appropriate event hooks for item pickup cancellation.
}
