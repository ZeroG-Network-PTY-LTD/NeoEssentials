package com.zerog.neoessentials.economy.worth;

import com.google.gson.*;
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
 * Prices are stored in neoessentials/worth.json as:
 * { "worth": { "minecraft:diamond": 50.0, "minecraft:iron_ingot": 5.0 } }
 *
 * Port from EssentialsX Worth.java:
 *  - getPrice(itemStack) — returns price or null if not set
 *  - setPrice(itemStack, price) — saves price
 *  - getSellMultiplier() — config multiplier applied to all sells
 */
public class WorthManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorthManager.class);
    private static final WorthManager INSTANCE = new WorthManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String WORTH_FILE = "worth.json";

    // itemId (minecraft:diamond) → price
    private final Map<String, BigDecimal> worthMap = new ConcurrentHashMap<>();
    private volatile boolean loaded = false;

    private WorthManager() {}

    public static WorthManager getInstance() {
        return INSTANCE;
    }

    public synchronized void initialize() {
        if (loaded) return;
        load();
        loaded = true;
    }

    // ── Load / Save ───────────────────────────────────────────────────────────

    private void load() {
        try {
            File f = ResourceUtil.getConfigFile(WORTH_FILE);
            if (!f.exists()) {
                LOGGER.info("No worth.json found — starting with empty price list");
                save();
                return;
            }
            try (Reader r = new FileReader(f)) {
                JsonObject root = GSON.fromJson(r, JsonObject.class);
                if (root == null) return;
                JsonObject worth = root.has("worth") ? root.getAsJsonObject("worth") : root;
                worthMap.clear();
                for (Map.Entry<String, JsonElement> e : worth.entrySet()) {
                    try {
                        worthMap.put(normalizeId(e.getKey()),
                            new BigDecimal(e.getValue().getAsString()));
                    } catch (Exception ex) {
                        LOGGER.warn("Invalid worth entry '{}': {}", e.getKey(), ex.getMessage());
                    }
                }
                LOGGER.info("Loaded {} item prices from worth.json", worthMap.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load worth.json: {}", e.getMessage(), e);
        }
    }

    private void save() {
        try {
            File f = ResourceUtil.getConfigFile(WORTH_FILE);
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            JsonObject root = new JsonObject();
            root.addProperty("_comment", "Item sell prices. Keys are item registry IDs (e.g. minecraft:diamond).");
            JsonObject worth = new JsonObject();
            // Sort for readability
            new TreeMap<>(worthMap).forEach((k, v) -> worth.addProperty(k, v.toPlainString()));
            root.add("worth", worth);

            try (Writer w = new FileWriter(f)) {
                GSON.toJson(root, w);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save worth.json: {}", e.getMessage(), e);
        }
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
        } else {
            worthMap.put(id, BigDecimal.valueOf(price));
        }
        save();
    }

    /**
     * Set the price of an item by ID string.
     */
    public void setPrice(String itemId, double price) {
        String id = normalizeId(itemId);
        if (price <= 0) {
            worthMap.remove(id);
        } else {
            worthMap.put(id, BigDecimal.valueOf(price));
        }
        save();
    }

    /**
     * Remove the price of an item.
     */
    public boolean removePrice(ItemStack stack) {
        String id = getItemId(stack);
        boolean removed = worthMap.remove(id) != null;
        if (removed) save();
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
        } catch (Exception ignored) {}
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
        } catch (Exception ignored) {}
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
}

