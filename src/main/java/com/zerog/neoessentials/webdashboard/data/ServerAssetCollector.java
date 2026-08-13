package com.zerog.neoessentials.webdashboard.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Server Asset Collector
 * Collects all loaded assets (items, blocks, etc.) from the server's registries
 * Provides asset information for the web dashboard to use with texture APIs
 *
 * This allows the dashboard to know what items exist without bundling assets
 */
public class ServerAssetCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerAssetCollector.class);
    private final MinecraftServer server;

    // Cache of asset data
    private JsonObject cachedAssets = null;
    private long lastCacheTime = 0;
    private static final long CACHE_DURATION = 300000; // 5 minutes

    public ServerAssetCollector(MinecraftServer server) {
        this.server = server;
    }

    /**
     * Get all loaded assets (items)
     * Endpoint: GET /api/server/assets
     */
    public JsonObject getAllAssets() {
        // Return cached data if still valid
        long currentTime = System.currentTimeMillis();
        if (cachedAssets != null && (currentTime - lastCacheTime) < CACHE_DURATION) {
            return cachedAssets;
        }

        LOGGER.info("Collecting server assets...");
        JsonObject assets = new JsonObject();

        // Collect all registered items
        JsonArray items = new JsonArray();
        JsonObject itemsByNamespace = new JsonObject();
        Map<String, Integer> namespaceCount = new HashMap<>();

        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            Identifier itemId = entry.getKey().identifier();

            JsonObject itemData = new JsonObject();
            itemData.addProperty("id", itemId.toString());
            itemData.addProperty("namespace", itemId.getNamespace());
            itemData.addProperty("path", itemId.getPath());
            itemData.addProperty("modded", !itemId.getNamespace().equals("minecraft"));

            // Determine if this namespace is from a mod
            if (!itemId.getNamespace().equals("minecraft")) {
                // Try to get mod name
                try {
                    var modContainer = net.neoforged.fml.ModList.get().getModContainerById(itemId.getNamespace());
                    if (modContainer.isPresent()) {
                        itemData.addProperty("modName", modContainer.get().getModInfo().getDisplayName());
                    }
                } catch (Exception e) {
                    NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Could not resolve mod name for namespace: {}", itemId.getNamespace(), e);
                }
            }

            items.add(itemData);

            // Count by namespace
            String namespace = itemId.getNamespace();
            namespaceCount.put(namespace, namespaceCount.getOrDefault(namespace, 0) + 1);

            // Group by namespace
            if (!itemsByNamespace.has(namespace)) {
                itemsByNamespace.add(namespace, new JsonArray());
            }
            itemsByNamespace.getAsJsonArray(namespace).add(itemData);
        }

        assets.add("items", items);
        assets.add("itemsByNamespace", itemsByNamespace);
        assets.addProperty("totalItems", items.size());

        // Add namespace statistics
        JsonObject stats = new JsonObject();
        for (Map.Entry<String, Integer> entry : namespaceCount.entrySet()) {
            stats.addProperty(entry.getKey(), entry.getValue());
        }
        assets.add("namespaceStats", stats);
        assets.addProperty("moddedNamespaces", namespaceCount.size() - 1); // -1 for minecraft

        // Add texture API recommendations
        JsonObject textureApis = new JsonObject();
        textureApis.addProperty("vanilla", "https://mc-heads.net/minecraft/item/{item_path}");
        textureApis.addProperty("fallback", "placeholder");
        textureApis.addProperty("note", "For modded items, configure custom texture server or use placeholders");
        assets.add("textureApis", textureApis);

        // Cache the result
        cachedAssets = assets;
        lastCacheTime = currentTime;

        LOGGER.info("Collected {} items from {} namespaces ({} modded)",
            items.size(), namespaceCount.size(), namespaceCount.size() - 1);

        return assets;
    }

    /**
     * Get assets for a specific namespace (mod)
     * Endpoint: GET /api/server/assets/{namespace}
     */
    public JsonObject getNamespaceAssets(String namespace) {
        JsonObject allAssets = getAllAssets();

        if (!allAssets.has("itemsByNamespace")) {
            return new JsonObject();
        }

        JsonObject itemsByNamespace = allAssets.getAsJsonObject("itemsByNamespace");

        if (!itemsByNamespace.has(namespace)) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Namespace not found: " + namespace);
            error.addProperty("namespace", namespace);
            return error;
        }

        JsonObject result = new JsonObject();
        result.addProperty("namespace", namespace);
        result.add("items", itemsByNamespace.get(namespace));
        result.addProperty("count", itemsByNamespace.getAsJsonArray(namespace).size());

        // Try to get mod info
        if (!namespace.equals("minecraft")) {
            try {
                var modContainer = net.neoforged.fml.ModList.get().getModContainerById(namespace);
                if (modContainer.isPresent()) {
                    result.addProperty("modName", modContainer.get().getModInfo().getDisplayName());
                    result.addProperty("modVersion", modContainer.get().getModInfo().getVersion().toString());
                }
            } catch (Exception e) {
                LOGGER.debug("Could not get mod info for namespace: {}", namespace);
            }
        }

        return result;
    }

    /**
     * Clear asset cache (forces reload on next request)
     */
    public void clearCache() {
        cachedAssets = null;
        lastCacheTime = 0;
        LOGGER.info("Server asset cache cleared");
    }
}

