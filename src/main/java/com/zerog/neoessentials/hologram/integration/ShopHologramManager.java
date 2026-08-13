package com.zerog.neoessentials.hologram.integration;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.hologram.*;
import com.zerog.neoessentials.shop.ShopManager;
import com.zerog.neoessentials.shop.ShopTransaction;
import com.zerog.neoessentials.shop.ShopTransaction.TransactionResult;
import com.zerog.neoessentials.shop.ShopParser;
import com.zerog.neoessentials.shop.model.ShopData;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
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
     * Remove all shop holograms from {@link HologramManager} that no longer have a
     * corresponding shop in {@link ShopManager}.
     *
     * <p>Call this after {@code ShopManager.reload()} or during server startup (once both
     * managers are initialized) to prevent orphaned holograms from re-spawning on
     * level load when shops are manually deleted from {@code shops.json}.
     *
     * <p>Despawning the in-world entities is attempted but may silently fail if the
     * level is not yet loaded (e.g. during early server start-up).  In that case the
     * stale-entity sweep that {@link HologramRenderer#spawnAllForWorld} performs on each
     * level load will discard any leftover tagged entities automatically.
     */
    public static void cleanOrphanedShopHolograms() {
        try {
            // Collect IDs of all holograms that SHOULD exist for currently loaded shops
            Set<String> validIds = ShopManager.getInstance().getAllShops().stream()
                .filter(s -> s.hologramEnabled)
                .map(ShopHologramManager::shopHologramId)
                .collect(Collectors.toSet());

            // Find holograms with the shop_ prefix that are NOT in the valid set
            List<HologramData> orphans = HologramManager.getInstance().getAllHolograms().stream()
                .filter(h -> h.id.startsWith(SHOP_HOLOGRAM_PREFIX))
                .filter(h -> !validIds.contains(h.id))
                .collect(Collectors.toList());

            for (HologramData orphan : orphans) {
                // Best-effort in-world despawn (level may not be available yet)
                ServerLevel level = findLevel(orphan.world);
                if (level != null) HologramRenderer.despawn(orphan, level);
                HologramManager.getInstance().removeHologram(orphan.id);
                LOGGER.info("[ShopHologram] Removed orphaned shop hologram '{}' (no matching shop found).", orphan.id);
            }
            if (!orphans.isEmpty()) {
                LOGGER.info("[ShopHologram] Cleaned {} orphaned shop hologram(s).", orphans.size());
            }
        } catch (Exception e) {
            LOGGER.warn("[ShopHologram] cleanOrphanedShopHolograms failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Refresh the hologram text for a shop (e.g. after a transaction updates stock).
     */    public static void refreshShopHologram(ShopData shop) {
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
     * Minimum time between processed shop-hologram interactions, per player.
     *
     * <p>Unlike right-clicking a block (fires once per click), {@link AttackEntityEvent}
     * is tied to the player's attack-swing mechanic — holding the mouse button down on an
     * entity fires it repeatedly (as fast as the client's attack-speed cooldown allows,
     * which is a damage-scaling mechanic, not a click-rate limiter), so without a cooldown
     * here each individual swing was processed as a separate full buy/sell transaction —
     * "holding click to sell" would sell repeatedly instead of once.</p>
     */
    private static final long INTERACTION_COOLDOWN_MS = 400L;
    private static final java.util.Map<java.util.UUID, Long> lastInteractionMs = new java.util.concurrent.ConcurrentHashMap<>();

    /** Returns {@code true} (and records the attempt) if this player is outside the cooldown window. */
    private static boolean tryConsumeInteractionCooldown(ServerPlayer player) {
        long now = System.currentTimeMillis();
        Long last = lastInteractionMs.get(player.getUUID());
        if (last != null && now - last < INTERACTION_COOLDOWN_MS) return false;
        lastInteractionMs.put(player.getUUID(), now);
        return true;
    }

    /**
     * Right-clicking a shop hologram entity is identical to right-clicking the sign → BUY.
     * Skipped if the hologram's {@code interactive} flag is {@code false}.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        // Display entities never override Entity#isPickable() (defaults to false), so they
        // can never actually be the target of an interact/attack raycast — the invisible
        // Interaction entity spawned alongside the hologram (see HologramRenderer) is what
        // receives clicks.
        if (!(event.getTarget() instanceof net.minecraft.world.entity.Interaction hitbox)) return;

        ShopData shop = shopFromHologramEntity(hitbox, com.zerog.neoessentials.util.LevelCompat.of(player));
        if (shop == null) return;

        // Guard: only process if the hologram is marked interactive
        HologramData holo = getHologramForShop(shop);
        if (holo == null || !holo.interactive) return;

        event.setCanceled(true);
        if (!tryConsumeInteractionCooldown(player)) return;

        boolean isOwner = shop.ownerUUID != null && shop.ownerUUID.equals(player.getUUID());

        // Item autofill: owner of a pending "?" shop right-clicks with item in hand.
        // Must run BEFORE the owner-info check below — previously the owner-info branch
        // returned unconditionally first, so a player-owned pending shop's own owner could
        // never actually complete assignment via the hologram at all (only admin shops,
        // whose ownerUUID is null, ever reached this far).
        if (shop.itemPending) {
            net.minecraft.world.item.ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
            boolean canAssign = shop.isAdminShop()
                    ? PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.create.admin")
                    : isOwner;
            if (canAssign && !held.isEmpty()) {
                shop.itemId      = com.zerog.neoessentials.economy.worth.WorthManager.getItemId(held);
                shop.itemNbt     = ShopParser.captureComponents(held);
                shop.itemPending = false;
                ShopManager.getInstance().registerShop(shop);
                player.sendSystemMessage(MessageUtil.component(
                    "commands.neoessentials.shop.item_set", ShopParser.buildFullItemDisplayName(shop)));
            } else if (canAssign) {
                player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.hold_item_hint_hologram"));
            } else {
                player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.not_ready"));
            }
            return;
        }

        // Owner sneak+right-click with an item in hand → (re)assign the item on an
        // already-configured shop, same as the sign's shift+right-click gesture — the
        // itemPending flow above only ever fires once, at shop creation.
        if (isOwner && player.isShiftKeyDown()) {
            net.minecraft.world.item.ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (held.isEmpty()) {
                player.sendSystemMessage(MessageUtil.component(
                    "commands.neoessentials.shop.hold_item_hint", "shift+right-click", "hologram"));
            } else {
                shop.itemId  = com.zerog.neoessentials.economy.worth.WorthManager.getItemId(held);
                shop.itemNbt = ShopParser.captureComponents(held);
                ShopManager.getInstance().registerShop(shop);
                player.sendSystemMessage(MessageUtil.component(
                    "commands.neoessentials.shop.item_updated", ShopParser.buildFullItemDisplayName(shop)));
            }
            return;
        }

        // Owner right-clicks → show info (same behaviour as sign)
        if (isOwner) {
            sendShopInfo(player, shop);
            return;
        }

        if (!PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.use")) {
            player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.no_permission_use"));
            return;
        }
        if (!shop.canBuy()) {
            player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.no_sell_price"));
            return;
        }
        TransactionResult result = ShopTransaction.executeBuy(player, shop, com.zerog.neoessentials.util.LevelCompat.of(player));
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
        if (!(event.getTarget() instanceof net.minecraft.world.entity.Interaction hitbox)) return;

        ShopData shop = shopFromHologramEntity(hitbox, com.zerog.neoessentials.util.LevelCompat.of(player));
        if (shop == null) return;

        // Guard: only process if the hologram is marked interactive
        HologramData holo = getHologramForShop(shop);
        if (holo == null || !holo.interactive) return;

        event.setCanceled(true);
        if (!tryConsumeInteractionCooldown(player)) return;

        // Owner left-clicks → show info
        if (shop.ownerUUID != null && shop.ownerUUID.equals(player.getUUID())) {
            sendShopInfo(player, shop);
            return;
        }

        if (!PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.use")) {
            player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.no_permission_use"));
            return;
        }
        if (!shop.canSell()) {
            player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.no_buy_price"));
            return;
        }
        TransactionResult result = ShopTransaction.executeSell(player, shop, com.zerog.neoessentials.util.LevelCompat.of(player));
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
    private static ShopData shopFromHologramEntity(net.minecraft.world.entity.Entity entity, ServerLevel level) {
        try {
            net.minecraft.nbt.CompoundTag nbt = entity.getPersistentData();
            if (!nbt.contains("neoessentials_hologram")) return null;
            String holoId = com.zerog.neoessentials.util.CompoundTagCompat.getString(nbt, "neoessentials_hologram_id");
            if (!holoId.startsWith(SHOP_HOLOGRAM_PREFIX)) return null;

            // ── Primary path: shop key stored directly in entity NBT ─────────
            if (nbt.contains(NBT_SHOP_KEY)) {
                String shopKey = com.zerog.neoessentials.util.CompoundTagCompat.getString(nbt, NBT_SHOP_KEY);
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
        String itemDisplay = ShopParser.buildFullItemDisplayName(shop);
        player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.info_header"));
        player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.info_owner", shop.ownerName));
        player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.info_item", shop.quantity + "x " + itemDisplay));
        if (shop.buyPrice  != null) player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.info_buy", currency, shop.buyPrice.toPlainString()));
        if (shop.sellPrice != null) player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.info_sell", currency, shop.sellPrice.toPlainString()));
    }

    private static void sendTransactionResult(ServerPlayer player, TransactionResult result,
                                              ShopData shop, boolean buying) {
        String currency = EconomyManager.getInstance().getCurrencySymbol();
        String itemDisplay = ShopParser.buildFullItemDisplayName(shop);
        switch (result.type) {
            case SUCCESS -> {
                if (buying) {
                    player.sendSystemMessage(MessageUtil.component(
                        "commands.neoessentials.shop.buy_success",
                        result.quantity, itemDisplay, currency, result.price.toPlainString(), shop.ownerName));
                } else {
                    player.sendSystemMessage(MessageUtil.component(
                        "commands.neoessentials.shop.sell_success_simple",
                        result.quantity, itemDisplay, currency, result.price.toPlainString()));
                }
            }
            case NOT_ENOUGH_MONEY -> player.sendSystemMessage(MessageUtil.component(buying
                ? "commands.neoessentials.shop.buy_fail_no_money"
                : "commands.neoessentials.shop.sell_fail_funds"));
            case NOT_ENOUGH_STOCK -> player.sendSystemMessage(MessageUtil.component(buying
                ? "commands.neoessentials.shop.buy_fail_stock"
                : "commands.neoessentials.shop.sell_fail_items"));
            case NO_SPACE -> player.sendSystemMessage(MessageUtil.component(buying
                ? "commands.neoessentials.shop.inventory_full"
                : "commands.neoessentials.shop.sell_fail_space"));
            case NO_CHEST -> player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.no_linked_chest"));
            case SHOP_DISABLED -> player.sendSystemMessage(MessageUtil.component(buying
                ? "commands.neoessentials.shop.no_sell_price"
                : "commands.neoessentials.shop.no_buy_price"));
            default -> player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.transaction_failed"));
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
    /**
     * Re-applies the {@code NBT_SHOP_KEY} tag to every shop hologram's entities.
     *
     * <p>{@link #createShopHologram} re-tags entities itself whenever it respawns a
     * hologram (e.g. after a transaction), but the generic startup path —
     * {@link com.zerog.neoessentials.hologram.HologramRenderer#spawnAllForWorld} —
     * has no concept of shops and doesn't. Since that path always creates brand-new
     * entities, every shop hologram loses its {@code NBT_SHOP_KEY} tag on every server
     * restart, leaving {@link #shopFromHologramEntity} to fall back to a reverse
     * position guess using hardcoded default offsets that don't account for a shop's
     * actual configured {@code hologramOffsetX/Y/Z} — silently breaking clicks on any
     * shop whose hologram isn't at the default offset. Call this once, shortly after
     * {@code spawnAllForWorld} runs at server start, to restore the tags immediately
     * instead of waiting for the fallback's opportunistic (and unreliable) caching.
     */
    public static void retagAllShopHolograms() {
        try {
            int tagged = 0;
            for (ShopData shop : ShopManager.getInstance().getAllShops()) {
                if (!shop.hologramEnabled) continue;
                HologramData holo = getHologramForShop(shop);
                if (holo == null) continue;
                tagEntitiesWithShopKey(holo, shop);
                tagged++;
            }
            if (tagged > 0) {
                LOGGER.debug("[ShopHologram] Re-tagged {} shop hologram(s) after startup respawn.", tagged);
            }
        } catch (Exception e) {
            LOGGER.error("[ShopHologram] Failed to re-tag shop holograms: {}", e.getMessage(), e);
        }
    }

    private static void tagEntitiesWithShopKey(HologramData data, ShopData shop) {
        ServerLevel level = findLevel(data.world);
        if (level == null) return;
        String shopKey = shop.toKey();
        if (data.entityUUIDs != null) {
            for (java.util.UUID uuid : data.entityUUIDs) {
                try {
                    net.minecraft.world.entity.Entity entity = level.getEntity(uuid);
                    if (entity != null) {
                        entity.getPersistentData().putString(NBT_SHOP_KEY, shopKey);
                    }
                } catch (Exception e) {
                    NeoLog.debug(LOGGER, LogCategory.GENERAL, "Failed to tag entity {} with shop key '{}'", uuid, shopKey, e);
                }
            }
        }
        // The clickable hitbox is the entity that actually receives interact/attack
        // events (see HologramData#interactionEntityUUID) — it needs the shop key too.
        if (data.interactionEntityUUID != null) {
            try {
                net.minecraft.world.entity.Entity entity = level.getEntity(data.interactionEntityUUID);
                if (entity != null) {
                    entity.getPersistentData().putString(NBT_SHOP_KEY, shopKey);
                }
            } catch (Exception e) {
                NeoLog.debug(LOGGER, LogCategory.GENERAL, "Failed to tag interaction entity with shop key '{}'", shopKey, e);
            }
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
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Failed to resolve level for dimension '{}'", dimensionKey, e);
        }
        return null;
    }
}
