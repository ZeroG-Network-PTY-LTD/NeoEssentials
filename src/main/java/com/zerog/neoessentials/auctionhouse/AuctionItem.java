package com.zerog.neoessentials.auctionhouse;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a single listing in the Auction House (active or expired).
 *
 * <p>The {@code nbt} field stores a JSON-serialised {@link DataComponentMap} so that
 * enchantments, names and other item data survive the round-trip to SQLite.
 * The {@code itemKey} field is the registry key, e.g. {@code "minecraft:diamond_sword"}.
 * Both are used together in {@link #getItemStack()} to reconstruct the full {@link ItemStack}.
 */
public class AuctionItem {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionItem.class);

    private final int    id;
    private final String uuid;    // seller UUID string
    private final String owner;   // seller display name
    private final String nbt;     // JSON DataComponentMap, may be null/empty
    private final String itemKey; // registry key e.g. "minecraft:diamond"
    private final int    count;
    private final double price;
    private long         secondsLeft;

    /** Lazily rebuilt on demand; invalidated when the DB is updated. */
    private transient ItemStack cachedStack;

    public AuctionItem(int id, String uuid, String owner, String nbt,
                       String itemKey, int count, double price, long secondsLeft) {
        this.id          = id;
        this.uuid        = uuid;
        this.owner       = owner;
        this.nbt         = nbt;
        this.itemKey     = itemKey;
        this.count       = count;
        this.price       = price;
        this.secondsLeft = secondsLeft;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int    getId()         { return id; }
    public String getUuid()       { return uuid; }
    public String getOwner()      { return owner; }
    public String getNbt()        { return nbt; }
    public String getItemKey()    { return itemKey; }
    public int    getCount()      { return count; }
    public double getPrice()      { return price; }
    public long   getSecondsLeft(){ return secondsLeft; }

    public void setSecondsLeft(long seconds) {
        this.secondsLeft = seconds;
    }

    // ── Display helpers ──────────────────────────────────────────────────────

    /** Human-readable "time left" string shown in the GUI. */
    public String getTimeLeft() {
        if (secondsLeft <= 0) return "Expired";
        long days    = secondsLeft / 86400;
        long hours   = (secondsLeft % 86400) / 3600;
        long minutes = (secondsLeft % 3600) / 60;
        long secs    = secondsLeft % 60;
        if (days    > 0) return days    + "d " + hours   + "h";
        if (hours   > 0) return hours   + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + secs    + "s";
        return secs + "s";
    }

    // ── ItemStack reconstruction ─────────────────────────────────────────────

    /**
     * Builds (or returns the cached) {@link ItemStack} for this listing.
     * Returns {@link ItemStack#EMPTY} if reconstruction fails.
     */
    public ItemStack getItemStack() {
        if (cachedStack != null) return cachedStack;
        try {
            Identifier rl   = Identifier.parse(itemKey);
            var              item = BuiltInRegistries.ITEM.getValue(rl);
            cachedStack = new ItemStack(item, count);

            if (nbt != null && !nbt.isBlank()) {
                try {
                    JsonElement    json       = JsonParser.parseString(nbt);
                    DataComponentMap components = AuctionComponentSerializer.deserialize(json);
                    cachedStack.applyComponents(components);
                } catch (Exception e) {
                    LOGGER.warn("[AuctionHouse] Could not apply components for '{}': {}", itemKey, e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("[AuctionHouse] Failed to build ItemStack for '{}': {}", itemKey, e.getMessage());
            cachedStack = ItemStack.EMPTY;
        }
        return cachedStack;
    }

    /** Discard the cached stack so it is rebuilt on the next {@link #getItemStack()} call. */
    public void invalidateCache() {
        cachedStack = null;
    }
}

