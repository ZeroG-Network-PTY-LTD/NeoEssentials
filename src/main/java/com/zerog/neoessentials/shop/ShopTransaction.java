package com.zerog.neoessentials.shop;

import com.zerog.neoessentials.shop.api.ShopEconomyAdapter;
import com.zerog.neoessentials.shop.api.ShopEconomyRegistry;
import com.zerog.neoessentials.shop.events.ShopTransactionEvent;
import com.zerog.neoessentials.shop.model.ShopData;
import com.zerog.neoessentials.shop.pricing.PricingEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Executes buy/sell transactions for ChestShop.
 *
 * <p>Economy operations are routed through {@link ShopEconomyRegistry}.
 * Dynamic pricing is applied (if enabled) via {@link PricingEngine}.
 * A {@link ShopTransactionEvent} is fired on the NeoForge game bus after every
 * successful transaction.
 */
public final class ShopTransaction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShopTransaction.class);

    private ShopTransaction() {}

    // ── Result ────────────────────────────────────────────────────────────────

    public enum ResultType {
        SUCCESS, NOT_ENOUGH_MONEY, NOT_ENOUGH_STOCK, NO_SPACE,
        NO_CHEST, NO_ECONOMY_ACCOUNT, SHOP_DISABLED, ERROR
    }

    public static class TransactionResult {
        public final ResultType type;
        public final String message;
        public final BigDecimal price;
        public final int quantity;

        TransactionResult(ResultType type, String message, BigDecimal price, int quantity) {
            this.type = type; this.message = message;
            this.price = price; this.quantity = quantity;
        }
        public boolean isSuccess() { return type == ResultType.SUCCESS; }
    }

    private static TransactionResult ok(BigDecimal price, int qty) {
        return new TransactionResult(ResultType.SUCCESS, null, price, qty);
    }
    private static TransactionResult fail(ResultType type) {
        return new TransactionResult(type, type.name(), BigDecimal.ZERO, 0);
    }

    // ── BUY ───────────────────────────────────────────────────────────────────

    public static TransactionResult executeBuy(ServerPlayer buyer, ShopData shop, ServerLevel level) {
        if (!shop.canBuy()) return fail(ResultType.SHOP_DISABLED);

        ShopEconomyAdapter eco = ShopEconomyRegistry.getInstance().getAdapter();

        BigDecimal price = PricingEngine.getInstance()
                .computeBuyPrice(shop, buyer.getUUID(), shop.quantity, level);
        if (price == null) return fail(ResultType.ERROR);
        price = price.setScale(2, RoundingMode.HALF_UP);

        ItemStack template = resolveItem(shop.itemId);
        if (template.isEmpty()) return fail(ResultType.ERROR);
        ItemStack item = template.copyWithCount(shop.quantity);

        if (!eco.hasBalance(buyer.getUUID(), price)) return fail(ResultType.NOT_ENOUGH_MONEY);

        if (!shop.isAdminShop()) {
            ChestBlockEntity chest = getChest(shop, level);
            if (chest == null) return fail(ResultType.NO_CHEST);
            if (countItems(chest, template) < shop.quantity) return fail(ResultType.NOT_ENOUGH_STOCK);
        }

        if (!hasSpace(buyer.getInventory(), item)) return fail(ResultType.NO_SPACE);

        if (!eco.debit(buyer.getUUID(), price)) return fail(ResultType.NOT_ENOUGH_MONEY);

        if (!shop.isAdminShop()) {
            ChestBlockEntity chest = getChest(shop, level);
            if (chest == null) { eco.credit(buyer.getUUID(), price); return fail(ResultType.NO_CHEST); }
            if (!removeItems(chest, template, shop.quantity)) {
                eco.credit(buyer.getUUID(), price);
                return fail(ResultType.NOT_ENOUGH_STOCK);
            }
        }

        giveItems(buyer, item);

        if (!shop.isAdminShop() && shop.ownerUUID != null) {
            eco.credit(shop.ownerUUID, price);
        }

        shop.totalSalesCount++;
        shop.lastSaleTimestamp = System.currentTimeMillis();
        ShopManager.getInstance().registerShop(shop);

        NeoForge.EVENT_BUS.post(new ShopTransactionEvent(
                shop, buyer.getUUID(), ShopTransactionEvent.Type.BUY, price, shop.quantity));

        if (!shop.isAdminShop()) checkAndNotifyLowStock(shop, level, template);

        LOGGER.debug("[ChestShop] BUY: {} bought {}x {} for {} from {}",
                buyer.getName().getString(), shop.quantity, shop.itemId, price, shop.ownerName);
        return ok(price, shop.quantity);
    }

    // ── SELL ──────────────────────────────────────────────────────────────────

    public static TransactionResult executeSell(ServerPlayer seller, ShopData shop, ServerLevel level) {
        if (!shop.canSell()) return fail(ResultType.SHOP_DISABLED);

        ShopEconomyAdapter eco = ShopEconomyRegistry.getInstance().getAdapter();

        BigDecimal price = PricingEngine.getInstance()
                .computeSellPrice(shop, seller.getUUID(), shop.quantity, level);
        if (price == null) return fail(ResultType.ERROR);
        price = price.setScale(2, RoundingMode.HALF_UP);

        ItemStack template = resolveItem(shop.itemId);
        if (template.isEmpty()) return fail(ResultType.ERROR);
        ItemStack item = template.copyWithCount(shop.quantity);

        if (countItems(seller.getInventory(), template) < shop.quantity) return fail(ResultType.NOT_ENOUGH_STOCK);

        if (!shop.isAdminShop() && shop.ownerUUID != null) {
            if (!eco.hasBalance(shop.ownerUUID, price)) return fail(ResultType.NOT_ENOUGH_MONEY);
        }

        if (!shop.isAdminShop()) {
            ChestBlockEntity chest = getChest(shop, level);
            if (chest == null) return fail(ResultType.NO_CHEST);
            if (!hasSpaceInContainer(chest, template, shop.quantity)) return fail(ResultType.NO_SPACE);
        }

        if (!removeItemsFromPlayer(seller, template, shop.quantity)) return fail(ResultType.NOT_ENOUGH_STOCK);

        if (!shop.isAdminShop() && shop.ownerUUID != null) {
            if (!eco.debit(shop.ownerUUID, price)) {
                giveItems(seller, item);
                return fail(ResultType.NOT_ENOUGH_MONEY);
            }
        }

        if (!shop.isAdminShop()) {
            ChestBlockEntity chest = getChest(shop, level);
            if (chest != null) addItems(chest, template, shop.quantity);
        }

        eco.credit(seller.getUUID(), price);

        shop.totalSalesCount++;
        shop.lastSaleTimestamp = System.currentTimeMillis();
        ShopManager.getInstance().registerShop(shop);

        NeoForge.EVENT_BUS.post(new ShopTransactionEvent(
                shop, seller.getUUID(), ShopTransactionEvent.Type.SELL, price, shop.quantity));

        LOGGER.debug("[ChestShop] SELL: {} sold {}x {} for {} to {}",
                seller.getName().getString(), shop.quantity, shop.itemId, price, shop.ownerName);
        return ok(price, shop.quantity);
    }

    // ── Low-stock notification ─────────────────────────────────────────────────

    private static void checkAndNotifyLowStock(ShopData shop, ServerLevel level, ItemStack template) {
        try {
            ChestBlockEntity chest = getChest(shop, level);
            if (chest == null) return;
            int remaining = countItems(chest, template);
            int threshold = shop.stockLowThreshold > 0 ? shop.stockLowThreshold : getDefaultThreshold();
            if (remaining <= threshold && shop.ownerUUID != null) {
                var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server == null) return;
                var owner = server.getPlayerList().getPlayer(shop.ownerUUID);
                if (owner != null) {
                    owner.sendSystemMessage(Component.literal(
                            "§e[Shop] §fYour §e" + shop.itemId.replace("minecraft:", "") +
                            " §fshop at §7(" + shop.signX + ", " + shop.signY + ", " + shop.signZ +
                            ")§f is running low! §c" + remaining + " §fitem(s) left."));
                }
            }
        } catch (Exception ignored) {}
    }

    private static int getDefaultThreshold() {
        try {
            var cfg = com.zerog.neoessentials.config.ConfigManager.getInstance()
                    .getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
            if (cfg != null && cfg.has("shop")) {
                var shopCfg = cfg.getAsJsonObject("shop");
                if (shopCfg.has("stockLowThreshold")) return shopCfg.get("stockLowThreshold").getAsInt();
            }
        } catch (Exception ignored) {}
        return 5;
    }

    // ── Inventory helpers ──────────────────────────────────────────────────────

    public static ItemStack resolveItem(String itemId) {
        try {
            ItemStack result = com.zerog.neoessentials.economy.worth.WorthManager.resolveItem(itemId);
            if (result != null && !result.isEmpty()) return result;
        } catch (Exception ignored) {}
        return ItemStack.EMPTY;
    }

    private static ChestBlockEntity getChest(ShopData shop, ServerLevel level) {
        if (!shop.hasChest) return null;
        BlockPos pos = shop.getChestPos();
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof ChestBlockEntity c ? c : null;
    }

    static int countItems(Container container, ItemStack target) {
        int count = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slot = container.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, target)) count += slot.getCount();
        }
        return count;
    }

    static boolean removeItems(Container container, ItemStack target, int amount) {
        int toRemove = amount;
        for (int i = 0; i < container.getContainerSize() && toRemove > 0; i++) {
            ItemStack slot = container.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, target)) {
                int take = Math.min(slot.getCount(), toRemove);
                slot.shrink(take);
                toRemove -= take;
                container.setItem(i, slot.isEmpty() ? ItemStack.EMPTY : slot);
            }
        }
        if (container instanceof BlockEntity be) be.setChanged();
        return toRemove == 0;
    }

    private static boolean removeItemsFromPlayer(ServerPlayer player, ItemStack target, int amount) {
        return removeItems(player.getInventory(), target, amount);
    }

    public static void giveItems(ServerPlayer player, ItemStack item) {
        ItemStack copy = item.copy();
        if (!player.getInventory().add(copy)) player.drop(copy, false);
    }

    static void addItems(Container container, ItemStack target, int amount) {
        int toAdd = amount;
        for (int i = 0; i < container.getContainerSize() && toAdd > 0; i++) {
            ItemStack slot = container.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, target)) {
                int space = slot.getMaxStackSize() - slot.getCount();
                int add = Math.min(space, toAdd);
                slot.grow(add); toAdd -= add;
                container.setItem(i, slot);
            }
        }
        for (int i = 0; i < container.getContainerSize() && toAdd > 0; i++) {
            if (container.getItem(i).isEmpty()) {
                int stackAmt = Math.min(toAdd, target.getMaxStackSize());
                container.setItem(i, target.copyWithCount(stackAmt));
                toAdd -= stackAmt;
            }
        }
        if (container instanceof BlockEntity be) be.setChanged();
    }

    public static boolean hasSpaceInContainer(Container container, ItemStack target, int amount) {
        int canFit = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty()) canFit += target.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(slot, target)) canFit += slot.getMaxStackSize() - slot.getCount();
            if (canFit >= amount) return true;
        }
        return false;
    }

    private static boolean hasSpace(net.minecraft.world.entity.player.Inventory inv, ItemStack item) {
        return hasSpaceInContainer(inv, item, item.getCount());
    }
}

