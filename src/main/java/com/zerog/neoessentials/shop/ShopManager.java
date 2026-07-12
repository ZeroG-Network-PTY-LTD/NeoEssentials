package com.zerog.neoessentials.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.shop.model.ShopData;
import com.zerog.neoessentials.shop.model.ShopType;
import com.zerog.neoessentials.util.ResourceUtil;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton manager that holds all active {@link ShopData} entries in memory
 * and persists them via the active {@link com.zerog.neoessentials.storage.DataStore}.
 * <p>
 * Key = {@code "<dimension>@<x>,<y>,<z>"} (the sign position).
 */
public class ShopManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShopManager.class);
    // Only used to parse the legacy shops.json file during one-time migration.
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String COLLECTION = "chest_shops";

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static ShopManager instance;
    public static ShopManager getInstance() {
        if (instance == null) instance = new ShopManager();
        return instance;
    }
    private ShopManager() {}

    private final com.zerog.neoessentials.storage.DataStore store =
        com.zerog.neoessentials.storage.StorageManager.getInstance().getStore();

    // ── State ─────────────────────────────────────────────────────────────────

    /** sign position key → shop */
    private final ConcurrentHashMap<String, ShopData> shopsBySign  = new ConcurrentHashMap<>();
    /** chest position key → shop (for chest-break detection) */
    private final ConcurrentHashMap<String, ShopData> shopsByChest = new ConcurrentHashMap<>();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void initialize() {
        try {
            migrateLegacyFilesIfNeeded();
            loadShops();
            LOGGER.info("[ChestShop] Loaded {} shop(s) from storage", shopsBySign.size());
        } catch (Exception e) {
            LOGGER.error("[ChestShop] Failed to load shops from storage — shops disabled until reload", e);
        }
    }

    public void shutdown() {
        // Every mutation is already persisted immediately via the DataStore — nothing to flush.
        LOGGER.info("[ChestShop] Shutdown — {} shop(s) persisted.", shopsBySign.size());
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    /** Register (or replace) a shop. Also indexes by chest position. */
    public void registerShop(ShopData shop) {
        shopsBySign.put(shop.toKey(), shop);
        if (shop.hasChest) {
            String chestKey = shop.chestDimension + "@"
                + shop.chestX + "," + shop.chestY + "," + shop.chestZ;
            shopsByChest.put(chestKey, shop);
        }
        store.put(COLLECTION, shop.toKey(), toJson(shop));
        // Auto-create / update hologram above this shop sign
        try {
            com.zerog.neoessentials.hologram.integration.ShopHologramManager
                .createShopHologram(shop, shop.signDimension);
        } catch (Exception ignored) {}
    }

    /** Remove by sign position. Returns the removed shop or null. */
    public ShopData removeShop(String dimension, BlockPos signPos) {
        String key = dimension + "@" + signPos.getX() + "," + signPos.getY() + "," + signPos.getZ();
        ShopData removed = shopsBySign.remove(key);
        if (removed != null && removed.hasChest) {
            String ck = removed.chestDimension + "@"
                + removed.chestX + "," + removed.chestY + "," + removed.chestZ;
            shopsByChest.remove(ck);
        }
        if (removed != null) {
            store.delete(COLLECTION, key);
            // Remove associated hologram
            try {
                com.zerog.neoessentials.hologram.integration.ShopHologramManager
                    .deleteShopHologram(removed);
            } catch (Exception ignored) {}
        }
        return removed;
    }

    /** Remove all shops owned by the given UUID. */
    //noinspection unused
    @SuppressWarnings("unused")
    public int removeShopsByOwner(UUID ownerUUID) {
        List<ShopData> toRemove = shopsBySign.values().stream()
            .filter(s -> ownerUUID.equals(s.ownerUUID))
            .toList();
        toRemove.forEach(s -> removeShop(s.signDimension, s.getSignPos()));
        return toRemove.size();
    }

    /** Look up a shop by the sign block position. */
    public ShopData getShopBySign(String dimension, BlockPos pos) {
        return shopsBySign.get(dimension + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ());
    }

    /** Look up a shop by its linked chest position. */
    public ShopData getShopByChest(String dimension, BlockPos pos) {
        return shopsByChest.get(dimension + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ());
    }

    /**
     * Look up a shop directly by its sign-position key (format: {@code "dim@x,y,z"}).
     * Useful when the key has been stored in NBT and needs to be resolved back to a shop.
     */
    public ShopData getShopByKey(String key) {
        return shopsBySign.get(key);
    }

    /** All shops owned by a given UUID. */
    public List<ShopData> getShopsByOwner(UUID ownerUUID) {
        return shopsBySign.values().stream()
            .filter(s -> ownerUUID.equals(s.ownerUUID))
            .toList();
    }

    /** Snapshot of all shops (defensive copy). */
    public Collection<ShopData> getAllShops() {
        return Collections.unmodifiableCollection(shopsBySign.values());
    }

    public int getShopCount() { return shopsBySign.size(); }

    // ── Persistence ───────────────────────────────────────────────────────────

    /** Load all shops from the active DataStore and rebuild both in-memory indices. */
    private void loadShops() {
        for (JsonObject obj : store.getAll(COLLECTION).values()) {
            ShopData s = fromJson(obj);
            shopsBySign.put(s.toKey(), s);
            if (s.hasChest) {
                String ck = s.chestDimension + "@" + s.chestX + "," + s.chestY + "," + s.chestZ;
                shopsByChest.put(ck, s);
            }
        }
    }

    /**
     * One-time import of the legacy {@code shops.json} file into the active DataStore, if
     * it's still empty and storage.autoMigrate is enabled.
     */
    private void migrateLegacyFilesIfNeeded() {
        if (store.hasAnyData(COLLECTION)) return;
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;

        Path file = ResourceUtil.getDataPath("shops.json");
        if (!Files.exists(file)) return;

        int migrated = 0;
        try (Reader r = Files.newBufferedReader(file)) {
            Type listType = new TypeToken<List<ShopData>>(){}.getType();
            List<ShopData> shops = GSON.fromJson(r, listType);
            if (shops != null) {
                for (ShopData s : shops) {
                    store.put(COLLECTION, s.toKey(), toJson(s));
                    migrated++;
                }
            }
        } catch (IOException e) {
            LOGGER.error("[ChestShop] Failed to migrate legacy shops.json: {}", e.getMessage());
        }

        if (migrated > 0) {
            LOGGER.info("[ChestShop] migrated {} shop record(s) from legacy shops.json into the '{}' storage backend.",
                migrated, com.zerog.neoessentials.storage.StorageManager.getInstance().getActiveType());
        }
    }

    private JsonObject toJson(ShopData s) {
        JsonObject o = new JsonObject();
        o.addProperty("ownerUUID", s.ownerUUID != null ? s.ownerUUID.toString() : null);
        o.addProperty("ownerName", s.ownerName);
        o.addProperty("quantity", s.quantity);
        o.addProperty("buyPrice", s.buyPrice != null ? s.buyPrice.toPlainString() : null);
        o.addProperty("sellPrice", s.sellPrice != null ? s.sellPrice.toPlainString() : null);
        o.addProperty("itemId", s.itemId);
        o.addProperty("itemNbt", s.itemNbt);
        o.addProperty("signDimension", s.signDimension);
        o.addProperty("signX", s.signX);
        o.addProperty("signY", s.signY);
        o.addProperty("signZ", s.signZ);
        o.addProperty("chestDimension", s.chestDimension);
        o.addProperty("chestX", s.chestX);
        o.addProperty("chestY", s.chestY);
        o.addProperty("chestZ", s.chestZ);
        o.addProperty("hasChest", s.hasChest);
        o.addProperty("itemPending", s.itemPending);
        o.addProperty("shopType", s.shopType != null ? s.shopType.name() : null);
        o.addProperty("shopId", s.shopId != null ? s.shopId.toString() : null);
        o.addProperty("totalSalesCount", s.totalSalesCount);
        o.addProperty("lastSaleTimestamp", s.lastSaleTimestamp);
        o.addProperty("stockLowThreshold", s.stockLowThreshold);
        o.addProperty("hologramEnabled", s.hologramEnabled);
        o.addProperty("hologramOffsetX", s.hologramOffsetX);
        o.addProperty("hologramOffsetY", s.hologramOffsetY);
        o.addProperty("hologramOffsetZ", s.hologramOffsetZ);
        return o;
    }

    private ShopData fromJson(JsonObject o) {
        ShopData s = new ShopData();
        s.ownerUUID = has(o, "ownerUUID") ? UUID.fromString(o.get("ownerUUID").getAsString()) : null;
        s.ownerName = str(o, "ownerName");
        s.quantity = o.has("quantity") ? o.get("quantity").getAsInt() : 0;
        s.buyPrice = has(o, "buyPrice") ? new BigDecimal(o.get("buyPrice").getAsString()) : null;
        s.sellPrice = has(o, "sellPrice") ? new BigDecimal(o.get("sellPrice").getAsString()) : null;
        s.itemId = str(o, "itemId");
        s.itemNbt = str(o, "itemNbt");
        s.signDimension = str(o, "signDimension");
        s.signX = o.has("signX") ? o.get("signX").getAsInt() : 0;
        s.signY = o.has("signY") ? o.get("signY").getAsInt() : 0;
        s.signZ = o.has("signZ") ? o.get("signZ").getAsInt() : 0;
        s.chestDimension = str(o, "chestDimension");
        s.chestX = o.has("chestX") ? o.get("chestX").getAsInt() : 0;
        s.chestY = o.has("chestY") ? o.get("chestY").getAsInt() : 0;
        s.chestZ = o.has("chestZ") ? o.get("chestZ").getAsInt() : 0;
        s.hasChest = o.has("hasChest") && o.get("hasChest").getAsBoolean();
        s.itemPending = o.has("itemPending") && o.get("itemPending").getAsBoolean();
        s.shopType = has(o, "shopType") ? ShopType.valueOf(o.get("shopType").getAsString()) : null;
        s.shopId = has(o, "shopId") ? UUID.fromString(o.get("shopId").getAsString()) : null;
        s.totalSalesCount = o.has("totalSalesCount") ? o.get("totalSalesCount").getAsLong() : 0L;
        s.lastSaleTimestamp = o.has("lastSaleTimestamp") ? o.get("lastSaleTimestamp").getAsLong() : 0L;
        s.stockLowThreshold = o.has("stockLowThreshold") ? o.get("stockLowThreshold").getAsInt() : 0;
        s.hologramEnabled = o.has("hologramEnabled") && o.get("hologramEnabled").getAsBoolean();
        s.hologramOffsetX = o.has("hologramOffsetX") ? o.get("hologramOffsetX").getAsDouble() : 0.5;
        s.hologramOffsetY = o.has("hologramOffsetY") ? o.get("hologramOffsetY").getAsDouble() : 1.8;
        s.hologramOffsetZ = o.has("hologramOffsetZ") ? o.get("hologramOffsetZ").getAsDouble() : 0.5;
        return s;
    }

    private static boolean has(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull();
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && !e.isJsonNull() ? e.getAsString() : null;
    }

    /** Reload from storage — call on /chestshop reload. */
    public void reload() {
        shopsBySign.clear();
        shopsByChest.clear();
        try {
            loadShops();
            LOGGER.info("[ChestShop] Reloaded {} shop(s).", shopsBySign.size());
        } catch (Exception e) {
            LOGGER.error("[ChestShop] Reload failed", e);
        }
        // Remove any shop holograms whose shop no longer exists (e.g. manually deleted
        // from storage, or removed by other means without hologram cleanup).
        try {
            com.zerog.neoessentials.hologram.integration.ShopHologramManager.cleanOrphanedShopHolograms();
        } catch (Exception e) {
            LOGGER.warn("[ChestShop] Orphan hologram cleanup failed during reload: {}", e.getMessage());
        }
    }
}
