package com.zerog.neoessentials.shops;

import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import java.util.*;

/**
 * Event hooks for shop creation and transactions
 */
public class ShopEventHandler {
    // Example: handle sign change to create shop
    @SubscribeEvent
    public void onSignChanged(BlockEvent.EntityPlaceEvent event) {
        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();
        // Check if block is a sign
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity signEntity)) return;
        List<String> lines = new ArrayList<>();
        for (var msg : signEntity.getFrontText().getMessages(false)) {
            lines.add(msg.getString().trim());
        }
        if (lines.size() < 3) return; // Require at least header, amount, price
        String header = lines.get(0).toLowerCase(Locale.ROOT);
        ShopType type = switch (header) {
            case "[buy]" -> ShopType.BUY;
            case "[sell]" -> ShopType.SELL;
            case "[admin buy]" -> ShopType.ADMIN_BUY;
            case "[admin sell]" -> ShopType.ADMIN_SELL;
            default -> null;
        };
        if (type == null) return;
        int amount;
        double price;
        try {
            amount = Integer.parseInt(lines.get(1));
            price = Double.parseDouble(lines.get(2));
        } catch (Exception e) {
            return;
        }
        // Find chest behind/adjacent
        BlockPos chestPos = findLinkedChest(level, pos);
        if (chestPos == null) return;
        // Infer item from chest (stub)
        net.minecraft.world.item.ItemStack itemSpec = inferFirstItemFromChest(level, chestPos);
        if (itemSpec == null) return;
        // Create and register shop
        Shop shop = new Shop(pos, chestPos, event.getEntity().getUUID(), type, itemSpec, amount, price);
        ShopRegistry.register(shop);
        // Render validated sign using updateText
        net.minecraft.network.chat.Component[] newLines = new net.minecraft.network.chat.Component[4];
        newLines[0] = net.minecraft.network.chat.Component.literal(shop.type().toString());
        newLines[1] = net.minecraft.network.chat.Component.literal(shop.amount() + "x " + shop.itemSpec().getHoverName().getString());
        newLines[2] = net.minecraft.network.chat.Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.shop.price_format", String.format("%.2f", shop.price())));
        newLines[3] = net.minecraft.network.chat.Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.shop.chest_location", chestPos.getX(), chestPos.getY(), chestPos.getZ()));
        signEntity.updateText(frontText -> frontText
            .setMessage(0, newLines[0])
            .setMessage(1, newLines[1])
            .setMessage(2, newLines[2])
            .setMessage(3, newLines[3]), true);
        signEntity.setChanged();
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
        // Send feedback to player
        if (event.getEntity() instanceof ServerPlayer player) {
            player.sendSystemMessage(MessageUtil.translatable(player, "neoessentials.shop.created", shop.type(), shop.amount(), shop.itemSpec().getHoverName().getString(), shop.price()));
        }
    }

    private BlockPos findLinkedChest(Level level, BlockPos signPos) {
        // Check adjacent blocks for chest (stub)
        for (var dir : net.minecraft.core.Direction.values()) {
            BlockPos adj = signPos.relative(dir);
            var block = level.getBlockState(adj).getBlock();
            if (block instanceof net.minecraft.world.level.block.ChestBlock) {
                return adj;
            }
        }
        return null;
    }

    private net.minecraft.world.item.ItemStack inferFirstItemFromChest(Level level, BlockPos chestPos) {
        var be = level.getBlockEntity(chestPos);
        if (be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
            for (int i = 0; i < chest.getContainerSize(); i++) {
                var stack = chest.getItem(i);
                if (!stack.isEmpty()) return stack.copy();
            }
        }
        return null;
    // ...existing code...
    }

    // Example: handle right-click on sign to transact
    @SubscribeEvent
    public void onRightClickSign(PlayerInteractEvent.RightClickBlock event) {
        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();
        ShopRegistry.getBySign(pos).ifPresent(shop -> {
            event.setCanceled(true);
            transact((ServerPlayer)event.getEntity(), level, shop);
        });
    }

    private void transact(ServerPlayer actor, Level level, Shop shop) {
        synchronized (shop.mutex()) {
            if (shop.type().isBuy()) {
                buyFlow(actor, level, shop);
            } else {
                sellFlow(actor, level, shop);
            }
        }
    }

    private void buyFlow(ServerPlayer buyer, Level level, Shop shop) {
        // EconomyService integration (stub)
    // double cost = shop.price(); // Will use when economy is integrated
        // Economy check
        var econ = com.zerog.neoessentials.managers.EconomyManager.getInstance();
        boolean hasFunds = econ.hasBalance(buyer.getUUID(), shop.price());
        if (!hasFunds) {
            buyer.sendSystemMessage(MessageUtil.translatable(buyer, "neoessentials.shop.insufficient_funds"));
            return;
        }
        boolean withdrawn = econ.withdrawBalance(buyer.getUUID(), shop.price(), "Shop purchase");
        if (!withdrawn) {
            buyer.sendSystemMessage(MessageUtil.translatable(buyer, "neoessentials.shop.withdraw_failed"));
            return;
        }
        // Chest check
        var chest = level.getBlockEntity(shop.chestPos());
        if (!(chest instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chestEntity)) return;
        int found = 0;
        for (int i = 0; i < chestEntity.getContainerSize(); i++) {
            var stack = chestEntity.getItem(i);
            if (!stack.isEmpty() && stack.is(shop.itemSpec().getItem())) {
                found += stack.getCount();
            }
        }
        if ((shop.type() == ShopType.ADMIN_BUY || shop.type() == ShopType.ADMIN_SELL) || found >= shop.amount()) {
            // Admin shop: infinite stock, skip chest check
        } else {
            // Not enough stock
            econ.depositBalance(buyer.getUUID(), shop.price(), "Shop refund");
            buyer.sendSystemMessage(MessageUtil.translatable(buyer, "neoessentials.shop.out_of_stock"));
            return;
        }
        // Remove items from chest unless admin shop
        if (shop.type() != ShopType.ADMIN_BUY && shop.type() != ShopType.ADMIN_SELL) {
            int toRemove = shop.amount();
            for (int i = 0; i < chestEntity.getContainerSize() && toRemove > 0; i++) {
                var stack = chestEntity.getItem(i);
                if (!stack.isEmpty() && stack.is(shop.itemSpec().getItem())) {
                    int remove = Math.min(stack.getCount(), toRemove);
                    stack.shrink(remove);
                    toRemove -= remove;
                }
            }
        }
        // Give items to buyer
        buyer.getInventory().add(shop.itemSpec().copyWithCount(shop.amount()));
    // Pay owner and handle tax (stub)
    // Pay owner and handle tax
    double tax = shop.price() * 0.05;
    double payout = shop.price() - tax;
    econ.depositBalance(shop.owner(), payout, "Shop sale");
    // For tax, use a fixed UUID (server account)
    UUID taxAccount = new UUID(0, 0); // Replace with actual tax account if needed
    econ.depositBalance(taxAccount, tax, "Shop tax");
    buyer.sendSystemMessage(MessageUtil.translatable(buyer, "neoessentials.shop.purchase_successful"));
    }
    private void sellFlow(ServerPlayer seller, Level level, Shop shop) {
        // EconomyService integration (stub)
    // double payout = shop.price(); // Will use when economy is integrated
        // Economy deposit
        var econ = com.zerog.neoessentials.managers.EconomyManager.getInstance();
        boolean deposited = econ.depositBalance(seller.getUUID(), shop.price(), "Shop sale");
        if (!deposited) {
            seller.sendSystemMessage(MessageUtil.translatable(seller, "neoessentials.shop.deposit_failed"));
            return;
        }
        // Remove items from seller
        // Check if seller has enough items
        int sellerCount = 0;
        for (int i = 0; i < seller.getInventory().getContainerSize(); i++) {
            var stack = seller.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(shop.itemSpec().getItem())) {
                sellerCount += stack.getCount();
            }
        }
        if (sellerCount < shop.amount()) return;
        // Remove items from seller
        int toRemove = shop.amount();
        for (int i = 0; i < seller.getInventory().getContainerSize() && toRemove > 0; i++) {
            var stack = seller.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(shop.itemSpec().getItem())) {
                int remove = Math.min(stack.getCount(), toRemove);
                stack.shrink(remove);
                toRemove -= remove;
            }
        }
        // Add items to chest unless admin shop
        if (shop.type() != ShopType.ADMIN_BUY && shop.type() != ShopType.ADMIN_SELL) {
            var chest = level.getBlockEntity(shop.chestPos());
            if (!(chest instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chestEntity)) return;
            // Find first empty slot in chest
            ItemStack toAdd = shop.itemSpec().copyWithCount(shop.amount());
            for (int i = 0; i < chestEntity.getContainerSize(); i++) {
                var stack = chestEntity.getItem(i);
                if (stack.isEmpty()) {
                    chestEntity.setItem(i, toAdd);
                    toAdd = null;
                    break;
                }
            }
            // If no empty slot, try to merge with existing stacks
            if (toAdd != null) {
                for (int i = 0; i < chestEntity.getContainerSize(); i++) {
                    var stack = chestEntity.getItem(i);
                    if (!stack.isEmpty() && stack.is(shop.itemSpec().getItem())) {
                        int space = stack.getMaxStackSize() - stack.getCount();
                        int add = Math.min(space, toAdd.getCount());
                        if (add > 0) {
                            stack.grow(add);
                            toAdd.shrink(add);
                            if (toAdd.getCount() <= 0) break;
                        }
                    }
                }
            }
        }
    // Handle tax
    double tax = shop.price() * 0.05;
    UUID taxAccount = new UUID(0, 0); // Replace with actual tax account if needed
    econ.depositBalance(taxAccount, tax, "Shop tax");
    // Admin shop logic: infinite stock, no chest required (already handled above)
        // Notify seller
        String itemName = shop.itemSpec().getHoverName().getString();
        seller.sendSystemMessage(MessageUtil.translatable(seller, "neoessentials.shop.sale_successful", String.valueOf(shop.amount()), itemName, String.valueOf(shop.price())));
    }
    }
}
