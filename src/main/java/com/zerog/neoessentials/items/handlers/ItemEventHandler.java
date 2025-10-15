package com.zerog.neoessentials.items.handlers;

/**
 * Item event handler for config-aware item behavior.
 * 
 * NOTE: NeoForge 1.21+ API Limitation
 * ===================================
 * The "drop-items-if-full" config option cannot currently be enforced because
 * ItemEntityPickupEvent.Pre doesn't support event cancellation in current NeoForge.
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
