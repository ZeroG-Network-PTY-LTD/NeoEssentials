package com.zerog.neoessentials.shop.handlers;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.shop.ShopManager;
import com.zerog.neoessentials.shop.ShopParser;
import com.zerog.neoessentials.shop.ShopTransaction;
import com.zerog.neoessentials.shop.ShopTransaction.TransactionResult;
import com.zerog.neoessentials.shop.model.ShopData;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;


/**
 * Handles player interactions with ChestShop signs.
 * <ul>
 *   <li>Right-click sign → BUY</li>
 *   <li>Left-click sign  → SELL</li>
 *   <li>Break sign/chest → remove shop</li>
 * </ul>
 */
@EventBusSubscriber(modid = "neoessentials")
public class ShopInteractHandler {

    /**
     * Minimum time between processed shop-sign interactions, per player.
     *
     * <p>Both {@link PlayerInteractEvent.RightClickBlock} and {@link PlayerInteractEvent.LeftClickBlock}
     * are tied to the player's click/swing input — holding the mouse button down on a block
     * fires them repeatedly (as fast as the client's attack-speed cooldown allows, which is a
     * damage-scaling mechanic, not a click-rate limiter), so without a cooldown here each
     * individual swing was processed as a separate full buy/sell transaction — "holding click
     * to sell" would sell repeatedly instead of once. Same fix as
     * {@link com.zerog.neoessentials.hologram.integration.ShopHologramManager}'s hologram
     * click handlers, kept as a separate cooldown map since these are unrelated interaction
     * surfaces (sign vs. hologram).
     */
    private static final long INTERACTION_COOLDOWN_MS = 400L;
    private static final java.util.Map<java.util.UUID, Long> lastInteractionMs = new java.util.concurrent.ConcurrentHashMap<>();

    private static boolean tryConsumeInteractionCooldown(ServerPlayer player) {
        long now = System.currentTimeMillis();
        Long last = lastInteractionMs.get(player.getUUID());
        if (last != null && now - last < INTERACTION_COOLDOWN_MS) return false;
        lastInteractionMs.put(player.getUUID(), now);
        return true;
    }

    // ── Right-click = BUY ─────────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        ServerLevel level = com.zerog.neoessentials.util.LevelCompat.of(player);
        BlockPos pos = event.getPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SignBlockEntity)) return;

        String dimension = level.dimension().identifier().toString();
        ShopData shop = ShopManager.getInstance().getShopBySign(dimension, pos);
        if (shop == null) return;

        event.setCanceled(true);
        if (!tryConsumeInteractionCooldown(player)) return;

        try {
            // ── Item autofill: owner right-clicks a pending "?" shop with item in hand ──
            if (shop.itemPending) {
                // Admin shops have ownerUUID == null; any player with the admin-shop create
                // permission can assign the item.  Player shops require UUID ownership.
                boolean canAssign = shop.isAdminShop()
                        ? PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.create.admin")
                        : (shop.ownerUUID != null && shop.ownerUUID.equals(player.getUUID()));

                if (canAssign) {
                    net.minecraft.world.item.ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
                    if (held.isEmpty()) {
                        player.sendSystemMessage(MessageUtil.component(
                            "commands.neoessentials.shop.hold_item_hint", "right-click", "sign"));
                    } else {
                        // Assign the held item — capture its DataComponents (custom name, NBT-backed
                        // modded data, enchantments, etc.) too, not just the bare registry id, so the
                        // shop trades the actual item the owner is holding rather than a data-less
                        // lookalike reconstructed from just "modid:item_name".
                        shop.itemId      = com.zerog.neoessentials.economy.worth.WorthManager.getItemId(held);
                        shop.itemNbt     = com.zerog.neoessentials.shop.ShopParser.captureComponents(held);
                        shop.itemPending = false;
                        ShopManager.getInstance().registerShop(shop); // re-save with updated data
                        ShopSignHandler.writeSignLines(level, pos, com.zerog.neoessentials.shop.ShopParser.formatSignLines(shop));
                        String currency = EconomyManager.getInstance().getCurrencySymbol();
                        player.sendSystemMessage(MessageUtil.component(
                            "commands.neoessentials.shop.item_set", com.zerog.neoessentials.shop.ShopParser.buildFullItemDisplayName(shop)));
                        if (shop.buyPrice  != null) player.sendSystemMessage(MessageUtil.component(
                            "commands.neoessentials.shop.buy_price_announce", currency, shop.buyPrice.toPlainString()));
                        if (shop.sellPrice != null) player.sendSystemMessage(MessageUtil.component(
                            "commands.neoessentials.shop.sell_price_announce", currency, shop.sellPrice.toPlainString()));
                        player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.now_active"));
                    }
                } else {
                    player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.not_ready"));
                }
                return;
            }

            // ── Owner sneak+right-click with an item in hand → (re)assign the item ──
            // The itemPending "?" flow above is the ONLY other place that captures item
            // components — a shop created by typing an item name directly on the sign
            // (rather than "?") never goes through it, so there was previously no way to
            // attach/update NBT-bearing item data on an already-configured shop at all.
            boolean isOwner = shop.ownerUUID != null && shop.ownerUUID.equals(player.getUUID());
            if (isOwner && player.isShiftKeyDown()) {
                net.minecraft.world.item.ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
                if (held.isEmpty()) {
                    player.sendSystemMessage(MessageUtil.component(
                        "commands.neoessentials.shop.hold_item_hint", "shift+right-click", "sign"));
                } else {
                    shop.itemId  = com.zerog.neoessentials.economy.worth.WorthManager.getItemId(held);
                    shop.itemNbt = com.zerog.neoessentials.shop.ShopParser.captureComponents(held);
                    ShopManager.getInstance().registerShop(shop);
                    ShopSignHandler.writeSignLines(level, pos, com.zerog.neoessentials.shop.ShopParser.formatSignLines(shop));
                    player.sendSystemMessage(MessageUtil.component(
                        "commands.neoessentials.shop.item_updated", com.zerog.neoessentials.shop.ShopParser.buildFullItemDisplayName(shop)));
                }
                return;
            }

            // ── Normal right-click = BUY ──────────────────────────────────────────
            // Owner right-clicks their own active sign → show info instead of buying
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

            TransactionResult result = ShopTransaction.executeBuy(player, shop, level);
            sendTransactionResult(player, result, shop, true);

        } catch (Exception e) {
            player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.error", e.getMessage()));
            org.slf4j.LoggerFactory.getLogger(ShopInteractHandler.class)
                    .error("[ChestShop] Unhandled exception in onRightClick for shop {}: {}",
                            shop.toKey(), e.getMessage(), e);
        }
    }

    // ── Left-click = SELL ─────────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel level = com.zerog.neoessentials.util.LevelCompat.of(player);
        BlockPos pos = event.getPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SignBlockEntity)) return;

        String dimension = level.dimension().identifier().toString();
        ShopData shop = ShopManager.getInstance().getShopBySign(dimension, pos);
        if (shop == null) return;

        // Sneak+left-click bypasses the sell/info interception entirely, letting the swing
        // fall through as a normal (uncanceled) attack/break attempt — otherwise every
        // left-click on a shop sign is unconditionally canceled below (turned into a sell or
        // an info message), so a BreakBlockEvent could never fire and the sign could
        // never actually be broken/removed via left-click, sneaking or not.
        if (player.isShiftKeyDown()) return;

        event.setCanceled(true);
        if (!tryConsumeInteractionCooldown(player)) return;

        try {
            // Owner left-clicks → show info only
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

            TransactionResult result = ShopTransaction.executeSell(player, shop, level);
            sendTransactionResult(player, result, shop, false);

        } catch (Exception e) {
            player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.error", e.getMessage()));
            org.slf4j.LoggerFactory.getLogger(ShopInteractHandler.class)
                    .error("[ChestShop] Unhandled exception in onLeftClick for shop {}: {}",
                            shop.toKey(), e.getMessage(), e);
        }
    }

    // ── Block break → remove shop ─────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockBreak(BreakBlockEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        String dimension = level.dimension().identifier().toString();

        // Check if it's a shop sign
        ShopData shop = ShopManager.getInstance().getShopBySign(dimension, pos);
        if (shop == null) {
            // Maybe it's the linked chest
            shop = ShopManager.getInstance().getShopByChest(dimension, pos);
        }
        if (shop == null) return;

        boolean isOwner = shop.ownerUUID != null && shop.ownerUUID.equals(player.getUUID());
        boolean isAdmin = PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.admin.remove");

        if (!isOwner && !isAdmin) {
            event.setCanceled(true);
            player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.cannot_break_other"));
            return;
        }

        ShopManager.getInstance().removeShop(dimension, shop.getSignPos());
        player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.removed"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void sendTransactionResult(ServerPlayer player, TransactionResult result,
                                              ShopData shop, boolean buying) {
        String currency = EconomyManager.getInstance().getCurrencySymbol();
        // Full (untruncated) display name for chat — the 16-char truncation in
        // buildItemDisplayName() exists only for the physical sign line, not chat messages.
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
            case NOT_ENOUGH_MONEY ->
                player.sendSystemMessage(MessageUtil.component(buying
                    ? "commands.neoessentials.shop.buy_fail_no_money"
                    : "commands.neoessentials.shop.sell_fail_funds"));
            case NOT_ENOUGH_STOCK ->
                player.sendSystemMessage(MessageUtil.component(buying
                    ? "commands.neoessentials.shop.buy_fail_stock"
                    : "commands.neoessentials.shop.sell_fail_items"));
            case NO_SPACE ->
                player.sendSystemMessage(MessageUtil.component(buying
                    ? "commands.neoessentials.shop.inventory_full"
                    : "commands.neoessentials.shop.sell_fail_space"));
            case NO_CHEST ->
                player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.no_linked_chest"));
            case SHOP_DISABLED ->
                player.sendSystemMessage(MessageUtil.component(buying
                    ? "commands.neoessentials.shop.no_sell_price"
                    : "commands.neoessentials.shop.no_buy_price"));
            default ->
                player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.transaction_failed"));
        }
    }

    private static void sendShopInfo(ServerPlayer player, ShopData shop) {
        String currency = EconomyManager.getInstance().getCurrencySymbol();
        String itemDisplay = ShopParser.buildFullItemDisplayName(shop);
        player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.info_header"));
        player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.info_owner", shop.ownerName));
        player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.info_item", shop.quantity + "x " + itemDisplay));
        if (shop.buyPrice  != null) player.sendSystemMessage(MessageUtil.component(
            "commands.neoessentials.shop.info_buy", currency, shop.buyPrice.toPlainString()));
        if (shop.sellPrice != null) player.sendSystemMessage(MessageUtil.component(
            "commands.neoessentials.shop.info_sell", currency, shop.sellPrice.toPlainString()));
        if (!shop.isAdminShop() && shop.hasChest) {
            // Show stock count
            player.sendSystemMessage(MessageUtil.component(
                "commands.neoessentials.shop.info_chest", shop.chestX, shop.chestY, shop.chestZ));
        }
    }
}

