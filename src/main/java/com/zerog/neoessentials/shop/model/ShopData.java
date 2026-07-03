package com.zerog.neoessentials.shop.model;

import net.minecraft.core.BlockPos;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents one ChestShop sign entry or the data anchor for an NPC shop.
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

    /**
     * JSON-serialized {@code DataComponentMap} (custom name/lore, enchantments, modded data,
     * NBT-backed capabilities, etc.) captured from the exact item the owner was holding when
     * they assigned it to this shop — {@code null}/blank for a plain vanilla-default item.
     * Applied on top of a fresh {@link net.minecraft.world.item.ItemStack} of {@link #itemId}
     * in {@link com.zerog.neoessentials.shop.ShopTransaction#resolveItem(ShopData)} so shops
     * trade the real item (with its data), not a data-less lookalike. See
     * {@link com.zerog.neoessentials.auctionhouse.AuctionComponentSerializer} for the codec —
     * shared with the Auction House, which solves the exact same problem.
     */
    public String itemNbt;

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

    // ── New fields (Economy Integration v1) ───────────────────────────────────

    /** Shop type — defaults to legacy SIGN_PLAYER/SIGN_ADMIN via {@link #isAdminShop()}. */
    public ShopType shopType = null; // null = derived from ownerUUID for backward compat

    /** Unique shop identifier (used by NPC shops; optional for sign shops). */
    public UUID shopId = null;

    /** Total number of successful transactions on this shop. */
    public long totalSalesCount = 0L;

    /** Epoch-millis of the last sale, or 0 if never sold. */
    public long lastSaleTimestamp = 0L;

    /**
     * If stock drops to or below this threshold after a buy transaction, the owner
     * receives a notification. 0 = use server default from config.
     */
    public int stockLowThreshold = 0;

    // ── Hologram integration ──────────────────────────────────────────────────

    /**
     * Whether the shop owner has opted-in to showing a hologram above this sign shop.
     * Defaults to {@code false} — the owner must explicitly enable it via command or
     * the clickable chat prompt shown on shop creation.
     */
    public boolean hologramEnabled = false;

    /**
     * Hologram position offset relative to the sign block, in blocks.
     * Defaults to (0.5, 1.8, 0.5) — centred above the sign.
     * Limited to ±4.5 in X/Z and ±4.5 in Y from the sign position so the hologram
     * stays within a 9×9×9 cube around the sign/chest.
     */
    public double hologramOffsetX = 0.5;
    public double hologramOffsetY = 1.8;
    public double hologramOffsetZ = 0.5;

    // ── Helpers ───────────────────────────────────────────────────────────────

    public BlockPos getSignPos()  { return new BlockPos(signX, signY, signZ); }
    public BlockPos getChestPos() { return hasChest ? new BlockPos(chestX, chestY, chestZ) : null; }

    public boolean isAdminShop() {
        if (shopType == ShopType.SIGN_ADMIN) return true;
        if (shopType == ShopType.SIGN_PLAYER || shopType == ShopType.NPC) return false;
        // Legacy: derive from ownerUUID / ownerName
        return ownerUUID == null ||
               ADMIN_SHOP_NAME.equalsIgnoreCase(ownerName != null ? ownerName.trim() : "");
    }

    /** Resolved shop type, never null. */
    public ShopType resolvedShopType() {
        if (shopType != null) return shopType;
        return isAdminShop() ? ShopType.SIGN_ADMIN : ShopType.SIGN_PLAYER;
    }

    public boolean canBuy()  { return buyPrice  != null; }
    public boolean canSell() { return sellPrice != null; }

    /** Human-readable shop key for logging. */
    public String toKey() {
        return signDimension + "@" + signX + "," + signY + "," + signZ;
    }

    @Override
    public String toString() {
        return String.format("ShopData{owner=%s, qty=%d, buy=%s, sell=%s, item=%s, pos=%s, sales=%d}",
            ownerName, quantity,
            buyPrice  != null ? buyPrice.toPlainString()  : "—",
            sellPrice != null ? sellPrice.toPlainString() : "—",
            itemId, toKey(), totalSalesCount);
    }
}

