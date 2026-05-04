package com.zerog.neoessentials.hologram.integration;
import com.zerog.neoessentials.hologram.*;
import com.zerog.neoessentials.shop.events.ShopTransactionEvent;
import com.zerog.neoessentials.shop.model.ShopData;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Locale;
/**
 * Auto-creates holographic displays above sign shops so that long item names
 * and pricing info are visible even when they don't fit on the sign itself.
 *
 * <p>Call {@link #createShopHologram(ShopData, String)} when a shop sign is placed,
 * and {@link #deleteShopHologram(String, String)} when the sign is broken.
 *
 * <p>Also listens to {@link ShopTransactionEvent} to refresh the hologram text
 * after each buy/sell.
 */
@EventBusSubscriber(modid = "neoessentials")
public class ShopHologramManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShopHologramManager.class);
    /** Prefix for all shop-auto-holograms so they can be identified / cleaned up. */
    public static final String SHOP_HOLOGRAM_PREFIX = "shop_";
    // ── Public API ────────────────────────────────────────────────────────────
    /**
     * Create (or update) a hologram above a shop sign.
     *
     * @param shop         the shop to display
     * @param dimensionKey e.g. {@code "minecraft:overworld"}
     */
    public static void createShopHologram(ShopData shop, String dimensionKey) {
        try {
            String id = shopHologramId(shop);
            HologramData data = new HologramData();
            data.id = id;
            data.world = dimensionKey;
            // Position: centred on sign block, ~1.8 blocks above it
            data.x = shop.signX + 0.5;
            data.y = shop.signY + 1.8;
            data.z = shop.signZ + 0.5;
            data.refreshInterval = 10;
            data.visible = true;
            if (data.entityUUIDs == null) data.entityUUIDs = new ArrayList<>();
            data.lines = buildShopLines(shop);
            HologramManager.getInstance().registerHologram(data);
            spawnInLevel(data);
            LOGGER.debug("[ShopHologram] Created hologram '{}' for shop at {}", id, shop.toKey());
        } catch (Exception e) {
            LOGGER.error("[ShopHologram] Failed to create shop hologram: {}", e.getMessage(), e);
        }
    }
    /** Remove the hologram that was auto-created for the given shop sign position. */
    public static void deleteShopHologram(String dimensionKey, String shopKey) {
        try {
            // shopKey is in format "dim@x,y,z" - derive id from it
            // try to find the hologram by searching shop prefix holograms
            for (HologramData d : HologramManager.getInstance().getAllHolograms()) {
                if (d.id.startsWith(SHOP_HOLOGRAM_PREFIX) && d.world.equals(dimensionKey)) {
                    // Check if position matches
                    String key = dimensionKey + "@" + (int)Math.floor(d.x - 0.5) + "," + (int)Math.floor(d.y - 1.8) + "," + (int)Math.floor(d.z - 0.5);
                    if (key.equals(shopKey)) {
                        ServerLevel level = findLevel(dimensionKey);
                        if (level != null) HologramRenderer.despawn(d, level);
                        HologramManager.getInstance().removeHologram(d.id);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("[ShopHologram] Error deleting shop hologram: {}", e.getMessage());
        }
    }
    /** Convenience overload using ShopData directly. */
    public static void deleteShopHologram(ShopData shop) {
        try {
            String id = shopHologramId(shop);
            HologramData data = HologramManager.getInstance().getHologram(id);
            if (data != null) {
                ServerLevel level = findLevel(data.world);
                if (level != null) HologramRenderer.despawn(data, level);
                HologramManager.getInstance().removeHologram(id);
            }
        } catch (Exception e) {
            LOGGER.debug("[ShopHologram] Error deleting shop hologram: {}", e.getMessage());
        }
    }
    /**
     * Refresh the hologram text for a shop (e.g. after a transaction updates stock).
     */
    public static void refreshShopHologram(ShopData shop) {
        try {
            String id = shopHologramId(shop);
            HologramData data = HologramManager.getInstance().getHologram(id);
            if (data == null) return;
            data.lines = buildShopLines(shop);
            HologramManager.getInstance().registerHologram(data);
            ServerLevel level = findLevel(data.world);
            if (level != null) HologramRenderer.spawn(data, level);
        } catch (Exception e) {
            LOGGER.debug("[ShopHologram] Error refreshing shop hologram: {}", e.getMessage());
        }
    }
    // ── Event listener ────────────────────────────────────────────────────────
    @SubscribeEvent
    public static void onShopTransaction(ShopTransactionEvent event) {
        try {
            refreshShopHologram(event.getShop());
        } catch (Exception ignored) {}
    }
    // ── Helpers ───────────────────────────────────────────────────────────────
    private static String shopHologramId(ShopData shop) {
        return (SHOP_HOLOGRAM_PREFIX + shop.signDimension + "_" + shop.signX + "_" + shop.signY + "_" + shop.signZ)
            .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }
    private static java.util.List<HologramLine> buildShopLines(ShopData shop) {
        java.util.List<HologramLine> lines = new ArrayList<>();
        // Line 1: Item name (solves the "too long for sign" problem)
        String itemName = shop.itemId != null ? shop.itemId.replace("minecraft:", "") : "?";
        String ownerDisplay = shop.isAdminShop() ? "&6[Admin Shop]" : ("&e" + shop.ownerName);
        lines.add(new HologramLine(ownerDisplay + " &7| &f" + itemName + " &8x" + shop.quantity));
        // Line 2: Buy / Sell prices
        String buyStr  = shop.canBuy()  ? "&aBuy: &f$" + shop.buyPrice.toPlainString()   : "";
        String sellStr = shop.canSell() ? "&cSell: &f$" + shop.sellPrice.toPlainString() : "";
        String priceLine;
        if (!buyStr.isEmpty() && !sellStr.isEmpty()) priceLine = buyStr + " &7| " + sellStr;
        else if (!buyStr.isEmpty()) priceLine = buyStr;
        else if (!sellStr.isEmpty()) priceLine = sellStr;
        else priceLine = "&7(no transactions)";
        lines.add(new HologramLine(priceLine));
        return lines;
    }
    private static void spawnInLevel(HologramData data) {
        try {
            ServerLevel level = findLevel(data.world);
            if (level != null) HologramRenderer.spawn(data, level);
        } catch (Exception e) {
            LOGGER.debug("[ShopHologram] Could not spawn in level: {}", e.getMessage());
        }
    }
    private static ServerLevel findLevel(String dimensionKey) {
        try {
            net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return null;
            for (ServerLevel level : server.getAllLevels()) {
                if (HologramRenderer.dimensionKey(level).equals(dimensionKey)) return level;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
