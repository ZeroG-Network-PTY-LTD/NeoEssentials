package com.zerog.neoessentials.shop.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persistent data for one NPC shop entity.
 *
 * <p>Stored in {@code neoessentials/npc_shops.json}.
 * The entity in-world stores only the {@link #shopId} in its NBT; all shop
 * data lives here and is looked up when the entity is right-clicked.
 */
public class ShopEntityData {

    /** Unique identifier for this shop (also stored on the entity via NBT). */
    public UUID shopId;

    /** UUID of the Minecraft entity representing this shop in-world. */
    public UUID entityUUID;

    /** Human-readable display name shown in the GUI title. */
    public String shopName;

    /** UUID of the player who created the shop (for admin tracking). */
    public UUID ownerUUID;

    /** Dimension of the entity (e.g. {@code "minecraft:overworld"}). */
    public String dimension;

    /** Approximate spawn coordinates (informational / for /npcshop list). */
    public double spawnX, spawnY, spawnZ;

    /** Ordered list of item listings available in this NPC shop. Max 54. */
    public List<ShopListing> listings = new ArrayList<>();

    /** Whether this shop accepts money from the built-in economy (always true for now). */
    public boolean economyEnabled = true;

    // ── Helpers ───────────────────────────────────────────────────────────────

    public void addListing(ShopListing listing) {
        if (listings.size() < 54) listings.add(listing);
    }

    public boolean removeListing(int index) {
        if (index < 0 || index >= listings.size()) return false;
        listings.remove(index);
        return true;
    }

    public String toKey() {
        return shopId != null ? shopId.toString() : "unknown";
    }
}

