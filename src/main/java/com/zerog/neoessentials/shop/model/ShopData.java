package com.zerog.neoessentials.shop.model;

import net.minecraft.core.BlockPos;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents one ChestShop sign entry.
 * <p>
 * Sign layout:
 * <pre>
 *  Line 0 — owner name   (or "Admin Shop")
 *  Line 1 — quantity     (e.g. "5")
 *  Line 2 — price line   (e.g. "B 10", "S 5", "B 10:S 5")
 *  Line 3 — item id      (e.g. "diamond", "minecraft:oak_log")
 * </pre>
 */
public class ShopData {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Line indices */
    public static final int NAME_LINE     = 0;
    public static final int QUANTITY_LINE = 1;
    public static final int PRICE_LINE    = 2;
    public static final int ITEM_LINE     = 3;

    /** Owner name used for admin shops (matches ChestShop default). */
    public static final String ADMIN_SHOP_NAME = "Admin Shop";

    // ── Fields ────────────────────────────────────────────────────────────────

    /** UUID of the shop owner; {@code null} for admin shops. */
    public UUID ownerUUID;

    /** Display name shown on the sign line 0. */
    public String ownerName;

    /** Quantity per transaction. */
    public int quantity;

    /** Buy price per transaction; {@code null} = buy disabled. */
    public BigDecimal buyPrice;

    /** Sell price per transaction; {@code null} = sell disabled. */
    public BigDecimal sellPrice;

    /** Resolved item registry id, e.g. {@code "minecraft:diamond"}. */
    public String itemId;

    /** Position of the shop sign (map key). */
    public String signDimension;
    public int signX, signY, signZ;

    /** Position of the linked chest; ignored for admin shops. */
    public String chestDimension;
    public int chestX, chestY, chestZ;
    public boolean hasChest;

    /**
     * When true, line 3 was "?" — the item has not yet been assigned.
     * The shop is stored but non-functional until the owner right-clicks with an item.
     */
    public boolean itemPending = false;

    // ── Helpers ───────────────────────────────────────────────────────────────

    public BlockPos getSignPos()  { return new BlockPos(signX, signY, signZ); }
    public BlockPos getChestPos() { return hasChest ? new BlockPos(chestX, chestY, chestZ) : null; }

    public boolean isAdminShop() {
        return ownerUUID == null ||
               ADMIN_SHOP_NAME.equalsIgnoreCase(ownerName != null ? ownerName.trim() : "");
    }

    public boolean canBuy()  { return buyPrice  != null; }
    public boolean canSell() { return sellPrice != null; }

    /** Human-readable shop key for logging. */
    public String toKey() {
        return signDimension + "@" + signX + "," + signY + "," + signZ;
    }

    @Override
    public String toString() {
        return String.format("ShopData{owner=%s, qty=%d, buy=%s, sell=%s, item=%s, pos=%s}",
            ownerName, quantity,
            buyPrice  != null ? buyPrice.toPlainString()  : "—",
            sellPrice != null ? sellPrice.toPlainString() : "—",
            itemId, toKey());
    }
}

