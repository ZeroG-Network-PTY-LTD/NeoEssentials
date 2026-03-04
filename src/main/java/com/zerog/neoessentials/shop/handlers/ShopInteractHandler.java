package com.zerog.neoessentials.shop.handlers;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.shop.ShopManager;
import com.zerog.neoessentials.shop.ShopTransaction;
import com.zerog.neoessentials.shop.ShopTransaction.TransactionResult;
import com.zerog.neoessentials.shop.model.ShopData;
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

    // ── Right-click = BUY ─────────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        ServerLevel level = player.serverLevel();
        BlockPos pos = event.getPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SignBlockEntity)) return;

        String dimension = level.dimension().location().toString();
        ShopData shop = ShopManager.getInstance().getShopBySign(dimension, pos);
        if (shop == null) return;

        event.setCanceled(true);

        // Owner right-clicks their own sign → show info instead of buying
        if (shop.ownerUUID != null && shop.ownerUUID.equals(player.getUUID())) {
            sendShopInfo(player, shop);
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

        TransactionResult result = ShopTransaction.executeBuy(player, shop, level);
        sendTransactionResult(player, result, shop, true);
    }

    // ── Left-click = SELL ─────────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel level = player.serverLevel();
        BlockPos pos = event.getPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SignBlockEntity)) return;

        String dimension = level.dimension().location().toString();
        ShopData shop = ShopManager.getInstance().getShopBySign(dimension, pos);
        if (shop == null) return;

        // Owner left-clicks → show info only
        if (shop.ownerUUID != null && shop.ownerUUID.equals(player.getUUID())) {
            event.setCanceled(true);
            sendShopInfo(player, shop);
            return;
        }

        if (!PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.use")) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§cYou don't have permission to use shops."));
            return;
        }

        if (!shop.canSell()) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§cThis shop does not buy items."));
            return;
        }

        event.setCanceled(true);
        TransactionResult result = ShopTransaction.executeSell(player, shop, level);
        sendTransactionResult(player, result, shop, false);
    }

    // ── Block break → remove shop ─────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        String dimension = level.dimension().location().toString();

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
            player.sendSystemMessage(Component.literal("§cYou cannot break someone else's shop."));
            return;
        }

        ShopManager.getInstance().removeShop(dimension, shop.getSignPos());
        player.sendSystemMessage(Component.literal("§aShop removed."));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void sendTransactionResult(ServerPlayer player, TransactionResult result,
                                              ShopData shop, boolean buying) {
        String currency = EconomyManager.getInstance().getCurrencySymbol();
        String itemDisplay = shop.itemId.replace("minecraft:", "");
        switch (result.type) {
            case SUCCESS -> {
                if (buying) {
                    player.sendSystemMessage(Component.literal(String.format(
                        "§aYou bought §f%dx %s §afor §f%s%s§a from §f%s§a.",
                        result.quantity, itemDisplay,
                        currency, result.price.toPlainString(),
                        shop.ownerName)));
                } else {
                    player.sendSystemMessage(Component.literal(String.format(
                        "§aYou sold §f%dx %s §afor §f%s%s§a.",
                        result.quantity, itemDisplay,
                        currency, result.price.toPlainString())));
                }
            }
            case NOT_ENOUGH_MONEY ->
                player.sendSystemMessage(Component.literal(buying
                    ? "§cYou don't have enough money to buy that."
                    : "§cThe shop owner can't afford to buy that."));
            case NOT_ENOUGH_STOCK ->
                player.sendSystemMessage(Component.literal(buying
                    ? "§cThis shop is out of stock."
                    : "§cYou don't have enough of that item."));
            case NO_SPACE ->
                player.sendSystemMessage(Component.literal(buying
                    ? "§cYour inventory is full."
                    : "§cThe shop's chest is full."));
            case NO_CHEST ->
                player.sendSystemMessage(Component.literal("§cShop has no linked chest."));
            case SHOP_DISABLED ->
                player.sendSystemMessage(Component.literal(buying
                    ? "§cThis shop doesn't sell items."
                    : "§cThis shop doesn't buy items."));
            default ->
                player.sendSystemMessage(Component.literal("§cTransaction failed (internal error)."));
        }
    }

    private static void sendShopInfo(ServerPlayer player, ShopData shop) {
        String currency = EconomyManager.getInstance().getCurrencySymbol();
        String itemDisplay = shop.itemId != null ? shop.itemId.replace("minecraft:", "") : "?";
        player.sendSystemMessage(Component.literal("§6§l--- Shop Info ---"));
        player.sendSystemMessage(Component.literal("§eOwner: §f" + shop.ownerName));
        player.sendSystemMessage(Component.literal("§eItem:  §f" + shop.quantity + "x " + itemDisplay));
        if (shop.buyPrice  != null) player.sendSystemMessage(Component.literal(
            "§eBuy:   §f" + currency + shop.buyPrice.toPlainString()));
        if (shop.sellPrice != null) player.sendSystemMessage(Component.literal(
            "§eSell:  §f" + currency + shop.sellPrice.toPlainString()));
        if (!shop.isAdminShop() && shop.hasChest) {
            // Show stock count
            player.sendSystemMessage(Component.literal(
                "§eChest: §f" + shop.chestX + ", " + shop.chestY + ", " + shop.chestZ));
        }
    }
}

