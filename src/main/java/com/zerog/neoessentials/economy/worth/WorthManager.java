package com.zerog.neoessentials.economy.worth;

import com.google.gson.*;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;
import com.zerog.neoessentials.util.ResourceUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages item sell prices — equivalent to EssentialsX's Worth.java.
 *
 * Prices are persisted via the active {@link DataStore} under the "item_worth" collection
 * (id = item registry id, field "price" as a BigDecimal string). Legacy installs that still
 * have config/neoessentials/worth.json are imported once on first run — see
 * {@link #migrateLegacyFileIfNeeded()}.
 *
 * Port from EssentialsX Worth.java:
 *  - getPrice(itemStack) — returns price or null if not set
 *  - setPrice(itemStack, price) — saves price
 *  - getSellMultiplier() — config multiplier applied to all sells
 */
public class WorthManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorthManager.class);
    private static final WorthManager INSTANCE = new WorthManager();
    private static final String COLLECTION = "item_worth";
    // Legacy file — kept only so migrateLegacyFileIfNeeded() can import pre-DataStore data.
    private static final String LEGACY_WORTH_FILE = "worth.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final DataStore store = StorageManager.getInstance().getStore();

    // itemId (minecraft:diamond) → price
    private final Map<String, BigDecimal> worthMap = new ConcurrentHashMap<>();
    private volatile boolean loaded = false;

    private WorthManager() {}

    public static WorthManager getInstance() {
        return INSTANCE;
    }

    public synchronized void initialize() {
        if (loaded) return;
        migrateLegacyFileIfNeeded();
        load();
        loaded = true;
    }

    // ── Load / Save ───────────────────────────────────────────────────────────

    private void load() {
        worthMap.clear();
        for (Map.Entry<String, JsonObject> e : store.getAll(COLLECTION).entrySet()) {
            try {
                JsonObject obj = e.getValue();
                if (obj.has("price")) {
                    worthMap.put(normalizeId(e.getKey()), new BigDecimal(obj.get("price").getAsString()));
                }
            } catch (Exception ex) {
                LOGGER.warn("Invalid worth entry '{}': {}", e.getKey(), ex.getMessage());
            }
        }
        LOGGER.info("Loaded {} item prices", worthMap.size());
    }

    private void persistPrice(String id, BigDecimal price) {
        JsonObject obj = new JsonObject();
        obj.addProperty("price", price.toPlainString());
        store.put(COLLECTION, id, obj);
    }

    public void reload() {
        worthMap.clear();
        loaded = false;
        initialize();
    }

    // ── API ───────────────────────────────────────────────────────────────────

    /**
     * Get the sell price of one unit of the item.
     * Returns null if item has no price set (Essentials: getPrice returns null).
     */
    public BigDecimal getPrice(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String id = getItemId(stack);
        BigDecimal price = worthMap.get(id);
        if (price != null) return price;
        // Try short name (diamond instead of minecraft:diamond)
        String shortName = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        return worthMap.get(shortName);
    }

    /**
     * Get the sell price of one unit by item ID string.
     */
    public BigDecimal getPrice(String itemId) {
        return worthMap.get(normalizeId(itemId));
    }

    /**
     * Set the price of an item. price <= 0 removes it.
     * Essentials: Worth.setPrice(ess, itemStack, price)
     */
    public void setPrice(ItemStack stack, double price) {
        String id = getItemId(stack);
        if (price <= 0) {
            worthMap.remove(id);
            store.delete(COLLECTION, id);
            NeoLog.debug(LOGGER, LogCategory.ECONOMY, "setPrice: removed price for item={} (price<=0)", id);
        } else {
            BigDecimal value = BigDecimal.valueOf(price);
            worthMap.put(id, value);
            persistPrice(id, value);
            NeoLog.debug(LOGGER, LogCategory.ECONOMY, "setPrice: item={} price={}", id, value);
        }
    }

    /**
     * Set the price of an item by ID string.
     */
    public void setPrice(String itemId, double price) {
        String id = normalizeId(itemId);
        if (price <= 0) {
            worthMap.remove(id);
            store.delete(COLLECTION, id);
            NeoLog.debug(LOGGER, LogCategory.ECONOMY, "setPrice: removed price for item={} (price<=0)", id);
        } else {
            BigDecimal value = BigDecimal.valueOf(price);
            worthMap.put(id, value);
            persistPrice(id, value);
            NeoLog.debug(LOGGER, LogCategory.ECONOMY, "setPrice: item={} price={}", id, value);
        }
    }

    /**
     * Remove the price of an item.
     */
    public boolean removePrice(ItemStack stack) {
        String id = getItemId(stack);
        boolean removed = worthMap.remove(id) != null;
        if (removed) {
            store.delete(COLLECTION, id);
            NeoLog.debug(LOGGER, LogCategory.ECONOMY, "removePrice: removed price for item={}", id);
        }
        return removed;
    }

    /**
     * Get all configured item IDs and prices (sorted).
     */
    public Map<String, BigDecimal> getAllPrices() {
        return new TreeMap<>(worthMap);
    }

    /**
     * Get the configured sell multiplier (default 1.0).
     * Essentials: getSettings().getMultiplier(user)
     */
    public BigDecimal getSellMultiplier() {
        try {
            com.google.gson.JsonObject cfg = com.zerog.neoessentials.config.ConfigManager
                .getInstance().getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
            if (cfg.has("economy")) {
                com.google.gson.JsonObject eco = cfg.getAsJsonObject("economy");
                if (eco.has("sellMultiplier")) {
                    return BigDecimal.valueOf(eco.get("sellMultiplier").getAsDouble());
                }
            }
        } catch (Exception ignored) {
            NeoLog.debug(LOGGER, LogCategory.ECONOMY, "getSellMultiplier: failed to read config, falling back to 1.0", ignored);
        }
        return BigDecimal.ONE;
    }

    /**
     * Whether selling named items (with custom display name) is allowed.
     * Essentials: getSettings().isAllowSellNamedItems()
     */
    public boolean isAllowSellNamedItems() {
        try {
            com.google.gson.JsonObject cfg = com.zerog.neoessentials.config.ConfigManager
                .getInstance().getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
            if (cfg.has("economy")) {
                com.google.gson.JsonObject eco = cfg.getAsJsonObject("economy");
                if (eco.has("allowSellNamedItems")) {
                    return eco.get("allowSellNamedItems").getAsBoolean();
                }
            }
        } catch (Exception ignored) {
            NeoLog.debug(LOGGER, LogCategory.ECONOMY, "isAllowSellNamedItems: failed to read config, falling back to false", ignored);
        }
        return false;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public static String getItemId(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key.toString(); // minecraft:diamond
    }

    public static String getItemDisplayName(ItemStack stack) {
        return stack.getDisplayName().getString();
    }

    /**
     * Resolve an item name or ID to an ItemStack.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Exact full ID with namespace   — {@code thermal:copper_ingot}</li>
     *   <li>Exact full ID, assume minecraft — {@code diamond} → {@code minecraft:diamond}</li>
     *   <li>Fuzzy path-only match          — {@code copper_ingot} matches the FIRST registry
     *       entry whose path equals {@code copper_ingot} (any namespace, alphabetical priority
     *       so {@code minecraft:} sorts before most mods)</li>
     *   <li>Fuzzy contains match           — {@code copper} matches the first entry whose
     *       path <em>contains</em> {@code copper}</li>
     * </ol>
     *
     * Returns {@code null} if nothing matches.
     */
    public static ItemStack resolveItem(String name) {
        if (name == null || name.isBlank()) return null;
        String trimmed = name.trim().toLowerCase();

        // 1. Try as-is if it has a namespace (modded or vanilla full id)
        if (trimmed.contains(":")) {
            ResourceLocation loc = ResourceLocation.tryParse(trimmed);
            if (loc != null) {
                Item item = BuiltInRegistries.ITEM.get(loc);
                if (item != net.minecraft.world.item.Items.AIR) return new ItemStack(item);
            }
            // Namespace given but not found — don't fall through to vanilla assumption
            return null;
        }

        // 2. Try minecraft: prefix for unqualified vanilla names
        ResourceLocation vanillaLoc = ResourceLocation.tryParse("minecraft:" + trimmed);
        if (vanillaLoc != null) {
            Item item = BuiltInRegistries.ITEM.get(vanillaLoc);
            if (item != net.minecraft.world.item.Items.AIR) return new ItemStack(item);
        }

        // 3. Fuzzy: exact path match across ALL namespaces (catches modded items by short name)
        for (ResourceLocation key : BuiltInRegistries.ITEM.keySet()) {
            if (key.getPath().equals(trimmed)) {
                Item item = BuiltInRegistries.ITEM.get(key);
                if (item != net.minecraft.world.item.Items.AIR) return new ItemStack(item);
            }
        }

        // 4. Fuzzy: path contains the search string (last resort)
        for (ResourceLocation key : BuiltInRegistries.ITEM.keySet()) {
            if (key.getPath().contains(trimmed)) {
                Item item = BuiltInRegistries.ITEM.get(key);
                if (item != net.minecraft.world.item.Items.AIR) return new ItemStack(item);
            }
        }

        return null;
    }

    private static String normalizeId(String id) {
        if (id == null) return "";
        id = id.trim().toLowerCase();
        // Ensure namespace
        return id.contains(":") ? id : "minecraft:" + id;
    }

    /**
     * One-time import of the legacy config/neoessentials/worth.json file into the active
     * DataStore, if it's still empty and storage.autoMigrate is enabled.
     */
    private void migrateLegacyFileIfNeeded() {
        if (store.hasAnyData(COLLECTION)) return;
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;

        File f = ResourceUtil.getConfigFile(LEGACY_WORTH_FILE);
        if (!f.exists()) return;

        int migrated = 0;
        try (Reader r = new FileReader(f)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null) return;
            JsonObject worth = root.has("worth") ? root.getAsJsonObject("worth") : root;
            for (Map.Entry<String, JsonElement> e : worth.entrySet()) {
                try {
                    String id = normalizeId(e.getKey());
                    BigDecimal price = new BigDecimal(e.getValue().getAsString());
                    JsonObject obj = new JsonObject();
                    obj.addProperty("price", price.toPlainString());
                    store.put(COLLECTION, id, obj);
                    migrated++;
                } catch (Exception ex) {
                    LOGGER.warn("Invalid legacy worth entry '{}': {}", e.getKey(), ex.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to migrate legacy worth.json: {}", e.getMessage(), e);
        }

        if (migrated > 0) {
            LOGGER.info("WorthManager: migrated {} item price record(s) from legacy files into the '{}' storage backend.",
                migrated, StorageManager.getInstance().getActiveType());
        }
    }
}
