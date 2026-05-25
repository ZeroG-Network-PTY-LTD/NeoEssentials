package com.zerog.neoessentials.hologram.integration;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.hologram.*;
import com.zerog.neoessentials.shop.ShopManager;
import com.zerog.neoessentials.shop.ShopTransaction;
import com.zerog.neoessentials.shop.ShopTransaction.TransactionResult;
import com.zerog.neoessentials.shop.ShopParser;
import com.zerog.neoessentials.shop.model.ShopData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
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
 * <p>Hologram text is refreshed via {@link #createShopHologram} which is triggered
 * automatically by {@link com.zerog.neoessentials.shop.ShopManager#registerShop}
 * after each buy/sell transaction.
 */
@EventBusSubscriber(modid = "neoessentials")
public class ShopHologramManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShopHologramManager.class);
    /** Prefix for all shop-auto-holograms so they can be identified / cleaned up. */
    public static final String SHOP_HOLOGRAM_PREFIX = "shop_";
    /** Maximum offset (in blocks) in any axis from the sign that the hologram may be moved. */
    public static final double MAX_OFFSET = 4.5;

    /**
     * NBT key placed on each TextDisplay entity so we can recover the sign position
     * even after the hologram has been moved far from its default offset.
     * Value format: {@code "<dimension>@<x>,<y>,<z>"} (same as {@link ShopData#toKey()}).
     */
    private static final String NBT_SHOP_KEY = "neoessentials_shop_key";

    // ── Public API ────────────────────────────────────────────────────────────
    /**
     * Create (or update) a hologram above a shop sign.
     * Only creates the hologram if {@link ShopData#hologramEnabled} is {@code true}.
     *
     * @param shop         the shop to display
     * @param dimensionKey e.g. {@code "minecraft:overworld"}
     */
    public static void createShopHologram(ShopData shop, String dimensionKey) {
        if (!shop.hologramEnabled) return;   // opt-in required
        try {
            String id = shopHologramId(shop);

            // ── BUG FIX: reuse existing HologramData so despawn() can locate its entities ──
            // If we created a brand-new HologramData every time, its entityUUIDs would be empty
            // and HologramRenderer.spawn() → despawn() would find nothing to remove.  The old
            // TextDisplay entities would remain in the world as orphans while new ones get
            // spawned on top — causing entities to accumulate on every registerShop() call
            // (which happens on every transaction, price-change, etc.).
            HologramData existing = HologramManager.getInstance().getHologram(id);
            if (existing != null) {
                // Update lines so the text reflects the current shop state.
                existing.lines = buildShopLines(shop);
                HologramManager.getInstance().registerHologram(existing);
                ServerLevel level = findLevel(existing.world);
                if (level != null) {
                    // spawn() calls despawn() first — uses existing.entityUUIDs, so old
                    // entities ARE removed before new ones are created.
                    HologramRenderer.spawn(existing, level);
                    tagEntitiesWithShopKey(existing, shop);
                }
                LOGGER.debug("[ShopHologram] Refreshed hologram '{}' for shop at {}", id, shop.toKey());
                return;
            }

            // ── First-time creation ──────────────────────────────────────────────────────
            HologramData data = new HologramData();
            data.id = id;
            data.world = dimensionKey;
            // Position uses owner-defined offsets (clamped to 9×9×9 around the sign)
            data.x = shop.signX + clampOffset(shop.hologramOffsetX);
            data.y = shop.signY + clampOffset(shop.hologramOffsetY);
            data.z = shop.signZ + clampOffset(shop.hologramOffsetZ);
            data.refreshInterval = 10;
            data.visible = true;
            data.interactive = true;   // shop holograms are always interactive
            data.entityUUIDs = new ArrayList<>();
            data.lines = buildShopLines(shop);
            HologramManager.getInstance().registerHologram(data);
            spawnInLevel(data);
            // Tag all spawned entities with the shop key so interaction lookup
            // works correctly even if the hologram is later moved.
            tagEntitiesWithShopKey(data, shop);
            LOGGER.debug("[ShopHologram] Created hologram '{}' for shop at {}", id, shop.toKey());
        } catch (Exception e) {
            LOGGER.error("[ShopHologram] Failed to create shop hologram: {}", e.getMessage(), e);
        }
    }

    /**
     * Enable the hologram for a shop and immediately spawn it.
     * Sets {@link ShopData#hologramEnabled} to {@code true} and persists the shop.
     */
    public static void enableShopHologram(ShopData shop) {
        shop.hologramEnabled = true;
        com.zerog.neoessentials.shop.ShopManager.getInstance().registerShop(shop);
        createShopHologram(shop, shop.signDimension);
    }

    /**
     * Disable the hologram for a shop and despawn it.
     * Sets {@link ShopData#hologramEnabled} to {@code false} and persists the shop.
     */
    public static void disableShopHologram(ShopData shop) {
        shop.hologramEnabled = false;
        com.zerog.neoessentials.shop.ShopManager.getInstance().registerShop(shop);
        deleteShopHologram(shop);
    }

    /**
     * Move the hologram to a new position relative to the sign block.
     * Offsets are clamped to [{@code -MAX_OFFSET}, {@code MAX_OFFSET}] in each axis,
     * keeping the hologram inside a 9×9×9 cube centred on the sign.
     *
     * @param shop    the shop whose hologram should be moved
     * @param offsetX new X offset relative to sign block
     * @param offsetY new Y offset relative to sign block
     * @param offsetZ new Z offset relative to sign block
     * @return {@code true} if the hologram existed and was moved, {@code false} otherwise
     */
    public static boolean moveShopHologram(ShopData shop, double offsetX, double offsetY, double offsetZ) {
        if (!shop.hologramEnabled) return false;
        // Clamp each axis to MAX_OFFSET
        shop.hologramOffsetX = clampOffset(offsetX);
        shop.hologramOffsetY = clampOffset(offsetY);
        shop.hologramOffsetZ = clampOffset(offsetZ);
        com.zerog.neoessentials.shop.ShopManager.getInstance().registerShop(shop);

        String id = shopHologramId(shop);
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) {
            // Not yet spawned — create it now
            createShopHologram(shop, shop.signDimension);
            return true;
        }
        // Despawn old entities and re-spawn at new position
        ServerLevel level = findLevel(data.world);
        if (level != null) HologramRenderer.despawn(data, level);
        data.x = shop.signX + shop.hologramOffsetX;
        data.y = shop.signY + shop.hologramOffsetY;
        data.z = shop.signZ + shop.hologramOffsetZ;
        HologramManager.getInstance().registerHologram(data);
        if (level != null) {
            HologramRenderer.spawn(data, level);
            // Re-tag the freshly spawned entities with the shop key so
            // interaction lookup still works after the move.
            tagEntitiesWithShopKey(data, shop);
        }
        return true;
    }
    /** Remove the hologram that was auto-created for the given shop sign position. */
    public static void deleteShopHologram(String dimensionKey, String shopKey) {
        try {
            // shopKey format: "dim@x,y,z"  — the hologram ID is derived from the same components,
            // so reconstruct it directly instead of reverse-engineering from hologram x/y/z
            // (which would break for moved holograms since those offsets are no longer 0.5/1.8/0.5).
            //
            // Derive an ID that matches shopHologramId(): shop_ + dim_x_y_z (sanitised to a-z0-9_).
            String rawId = SHOP_HOLOGRAM_PREFIX + shopKey.replace("@", "_").replace(",", "_");
            String id = rawId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");

            HologramData data = HologramManager.getInstance().getHologram(id);
            if (data != null) {
                ServerLevel level = findLevel(dimensionKey);
                if (level != null) HologramRenderer.despawn(data, level);
                HologramManager.getInstance().removeHologram(id);
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
            if (level != null) {
                HologramRenderer.spawn(data, level);
                // Re-tag the freshly spawned entities with the shop key.
                tagEntitiesWithShopKey(data, shop);
            }
        } catch (Exception e) {
            LOGGER.debug("[ShopHologram] Error refreshing shop hologram: {}", e.getMessage());
        }
    }
    // ── Event listeners ───────────────────────────────────────────────────────

    // NOTE: ShopTransactionEvent refresh is intentionally NOT handled here.
    // ShopTransaction always calls ShopManager.registerShop() after a transaction to persist
    // stats, and registerShop() calls createShopHologram() which already updates the hologram
    // in-place.  Adding a second refresh here would cause two full despawn+respawn cycles per
    // transaction, doubling entity churn for no benefit.

    /**
     * Right-clicking a shop hologram entity is identical to right-clicking the sign → BUY.
     * Skipped if the hologram's {@code interactive} flag is {@code false}.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getTarget() instanceof Display.TextDisplay display)) return;

        ShopData shop = shopFromHologramEntity(display, player.serverLevel());
        if (shop == null) return;

        // Guard: only process if the hologram is marked interactive
        HologramData holo = getHologramForShop(shop);
        if (holo == null || !holo.interactive) return;

        event.setCanceled(true);

        // Owner right-clicks → show info (same behaviour as sign)
        if (shop.ownerUUID != null && shop.ownerUUID.equals(player.getUUID())) {
            sendShopInfo(player, shop);
            return;
        }

        // Item autofill: owner of a pending "?" shop right-clicks with item in hand
        if (shop.itemPending) {
            net.minecraft.world.item.ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
            boolean canAssign = shop.isAdminShop()
                    ? PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.create.admin")
                    : (shop.ownerUUID != null && shop.ownerUUID.equals(player.getUUID()));
            if (canAssign && !held.isEmpty()) {
                shop.itemId      = com.zerog.neoessentials.economy.worth.WorthManager.getItemId(held);
                shop.itemPending = false;
                ShopManager.getInstance().registerShop(shop);
                player.sendSystemMessage(Component.literal(
                    "§aItem set to §f" + ShopParser.buildItemDisplayName(shop.itemId) + "§a!"));
            } else {
                player.sendSystemMessage(Component.literal("§eHold the item you want to trade, then right-click the hologram."));
            }
            return;
        }

        if (!PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.use")) {
            player.sendSystemMessage(Component.literal("§cYou don't have permission to use shops."));
            return;
        }
        if (!shop.canBuy()) {
            player.sendSystemMessage(Component.literal("§cThis shop does not sell items."));
            return;
        }
        TransactionResult result = ShopTransaction.executeBuy(player, shop, player.serverLevel());
        sendTransactionResult(player, result, shop, true);
    }

    /**
     * Left-clicking (attacking) a shop hologram entity → SELL.
     * Display entities are invulnerable so no damage is dealt.
     * Skipped if the hologram's {@code interactive} flag is {@code false}.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof Display.TextDisplay display)) return;

        ShopData shop = shopFromHologramEntity(display, player.serverLevel());
        if (shop == null) return;

        // Guard: only process if the hologram is marked interactive
        HologramData holo = getHologramForShop(shop);
        if (holo == null || !holo.interactive) return;

        event.setCanceled(true);

        // Owner left-clicks → show info
        if (shop.ownerUUID != null && shop.ownerUUID.equals(player.getUUID())) {
            sendShopInfo(player, shop);
            return;
        }

        if (!PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.use")) {
            player.sendSystemMessage(Component.literal("§cYou don't have permission to use shops."));
            return;
        }
        if (!shop.canSell()) {
            player.sendSystemMessage(Component.literal("§cThis shop does not buy items."));
            return;
        }
        TransactionResult result = ShopTransaction.executeSell(player, shop, player.serverLevel());
        sendTransactionResult(player, result, shop, false);
    }
    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Given a TextDisplay entity, return the ShopData it belongs to, or {@code null}
     * if this entity is not a shop hologram.
     *
     * <p>The preferred path reads the shop's sign-position key directly from the
     * entity's persistent NBT ({@value #NBT_SHOP_KEY}).  This is set whenever a
     * shop hologram is created, moved, or refreshed, so it remains correct even
     * when the hologram has been repositioned away from its default offset.
     *
     * <p>Entities spawned by an older build of the mod may not have the tag yet.
     * For those we fall back to the original offset-based reverse lookup using the
     * default placement offsets (0.5, 1.8, 0.5) — this works as long as the
     * hologram has never been moved.
     */
    private static ShopData shopFromHologramEntity(Display.TextDisplay entity, ServerLevel level) {
        try {
            net.minecraft.nbt.CompoundTag nbt = entity.getPersistentData();
            if (!nbt.contains("neoessentials_hologram")) return null;
            String holoId = nbt.getString("neoessentials_hologram_id");
            if (!holoId.startsWith(SHOP_HOLOGRAM_PREFIX)) return null;

            // ── Primary path: shop key stored directly in entity NBT ─────────
            if (nbt.contains(NBT_SHOP_KEY)) {
                String shopKey = nbt.getString(NBT_SHOP_KEY);
                return ShopManager.getInstance().getShopByKey(shopKey);
            }

            // ── Fallback: reverse-compute from default offset ─────────────────
            // Only accurate if the hologram was never moved from its spawn position.
            HologramData holo = HologramManager.getInstance().getHologram(holoId);
            if (holo == null) return null;

            int signX = (int) Math.floor(holo.x - 0.5);
            int signY = (int) Math.floor(holo.y - 1.8);
            int signZ = (int) Math.floor(holo.z - 0.5);
            String dimension = HologramRenderer.dimensionKey(level);
            ShopData shop = ShopManager.getInstance().getShopBySign(dimension, new BlockPos(signX, signY, signZ));

            // If found, opportunistically tag the entity for future lookups.
            if (shop != null) {
                entity.getPersistentData().putString(NBT_SHOP_KEY, shop.toKey());
            }
            return shop;
        } catch (Exception e) {
            LOGGER.debug("[ShopHologram] shopFromHologramEntity error: {}", e.getMessage());
            return null;
        }
    }

    private static void sendShopInfo(ServerPlayer player, ShopData shop) {
        String currency = EconomyManager.getInstance().getCurrencySymbol();
        String itemDisplay = ShopParser.buildItemDisplayName(shop.itemId);
        player.sendSystemMessage(Component.literal("§6§l--- Shop Info ---"));
        player.sendSystemMessage(Component.literal("§eOwner: §f" + shop.ownerName));
        player.sendSystemMessage(Component.literal("§eItem:  §f" + shop.quantity + "x " + itemDisplay));
        if (shop.buyPrice  != null) player.sendSystemMessage(Component.literal("§eBuy:   §f" + currency + shop.buyPrice.toPlainString()));
        if (shop.sellPrice != null) player.sendSystemMessage(Component.literal("§eSell:  §f" + currency + shop.sellPrice.toPlainString()));
    }

    private static void sendTransactionResult(ServerPlayer player, TransactionResult result,
                                              ShopData shop, boolean buying) {
        String currency = EconomyManager.getInstance().getCurrencySymbol();
        String itemDisplay = ShopParser.buildItemDisplayName(shop.itemId);
        switch (result.type) {
            case SUCCESS -> {
                if (buying) {
                    player.sendSystemMessage(Component.literal(String.format(
                        "§aYou bought §f%dx %s §afor §f%s%s§a from §f%s§a.",
                        result.quantity, itemDisplay, currency, result.price.toPlainString(), shop.ownerName)));
                } else {
                    player.sendSystemMessage(Component.literal(String.format(
                        "§aYou sold §f%dx %s §afor §f%s%s§a.",
                        result.quantity, itemDisplay, currency, result.price.toPlainString())));
                }
            }
            case NOT_ENOUGH_MONEY -> player.sendSystemMessage(Component.literal(buying
                ? "§cYou don't have enough money to buy that."
                : "§cThe shop owner can't afford to buy that."));
            case NOT_ENOUGH_STOCK -> player.sendSystemMessage(Component.literal(buying
                ? "§cThis shop is out of stock."
                : "§cYou don't have enough of that item."));
            case NO_SPACE -> player.sendSystemMessage(Component.literal(buying
                ? "§cYour inventory is full."
                : "§cThe shop's chest is full."));
            case NO_CHEST -> player.sendSystemMessage(Component.literal("§cShop has no linked chest."));
            case SHOP_DISABLED -> player.sendSystemMessage(Component.literal(buying
                ? "§cThis shop doesn't sell items."
                : "§cThis shop doesn't buy items."));
            default -> player.sendSystemMessage(Component.literal("§cTransaction failed (internal error)."));
        }
    }

    private static String shopHologramId(ShopData shop) {
        return (SHOP_HOLOGRAM_PREFIX + shop.signDimension + "_" + shop.signX + "_" + shop.signY + "_" + shop.signZ)
            .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }

    /** Returns the live {@link HologramData} for a shop, or {@code null} if not registered. */
    private static HologramData getHologramForShop(ShopData shop) {
        return HologramManager.getInstance().getHologram(shopHologramId(shop));
    }

    /** Clamp an offset value to the ±{@value #MAX_OFFSET}-block range. */
    private static double clampOffset(double offset) {
        return Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, offset));
    }

    /**
     * Tag every live TextDisplay entity that belongs to {@code data} with the shop's
     * sign-position key ({@value #NBT_SHOP_KEY}).  This persists through server
     * restarts and lets {@link #shopFromHologramEntity} recover the correct shop
     * even after the hologram has been repositioned.
     */
    private static void tagEntitiesWithShopKey(HologramData data, ShopData shop) {
        if (data.entityUUIDs == null || data.entityUUIDs.isEmpty()) return;
        ServerLevel level = findLevel(data.world);
        if (level == null) return;
        String shopKey = shop.toKey();
        for (java.util.UUID uuid : data.entityUUIDs) {
            try {
                net.minecraft.world.entity.Entity entity = level.getEntity(uuid);
                if (entity != null) {
                    entity.getPersistentData().putString(NBT_SHOP_KEY, shopKey);
                }
            } catch (Exception ignored) {}
        }
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
