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
        // Only run on server
        if (event.getLevel().isClientSide()) return;
        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();
        // Check if block is a sign
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity signEntity)) return;
        // Parse and validate sign
        ParsedSignShop parsed = parseSignShop(signEntity);
        if (parsed == null) return;
        // Prevent negative/zero values
        if (parsed.amount <= 0 || parsed.price <= 0) return;
        // Find chest unless admin shop
        BlockPos chestPos = null;
        if (!parsed.type.isAdmin()) {
            chestPos = findLinkedChest(level, pos);
            if (chestPos == null) return;
            // Prevent duplicate chest linkage
            if (ShopRegistry.getByChest(chestPos).isPresent()) return;
        }
        // Infer item from chest (stub)
        net.minecraft.world.item.ItemStack itemSpec = null;
        if (!parsed.type.isAdmin()) {
            itemSpec = inferFirstItemFromChest(level, chestPos);
            if (itemSpec == null || itemSpec.isEmpty()) return;
        } else {
            // For admin shop, require item name in line 3 (or fallback to chest if present)
            itemSpec = inferFirstItemFromChest(level, chestPos);
            if (itemSpec == null || itemSpec.isEmpty()) return;
        }
        // Create and register shop
        Shop shop = new Shop(pos, chestPos, event.getEntity().getUUID(), parsed.type, itemSpec, parsed.amount, parsed.price);
        ShopRegistry.register(shop);
        // Render validated sign using updateText
        net.minecraft.network.chat.Component[] newLines = new net.minecraft.network.chat.Component[4];
        newLines[0] = net.minecraft.network.chat.Component.literal(shop.type().toString());
        newLines[1] = net.minecraft.network.chat.Component.literal(shop.amount() + "x " + shop.itemSpec().getHoverName().getString());
        newLines[2] = net.minecraft.network.chat.Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.shop.price_format", String.format("%.2f", shop.price())));
        newLines[3] = net.minecraft.network.chat.Component.literal(
            shop.chestPos() != null ?
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.shop.chest_location", shop.chestPos().getX(), shop.chestPos().getY(), shop.chestPos().getZ()) :
                "ADMIN SHOP"
        );
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

    // Helper for parsing sign shop data
    private static class ParsedSignShop {
        public final ShopType type;
        public final int amount;
        public final double price;
        public ParsedSignShop(ShopType type, int amount, double price) {
            this.type = type;
            this.amount = amount;
            this.price = price;
        }
    }
    private static ParsedSignShop parseSignShop(SignBlockEntity signEntity) {
        List<String> lines = new ArrayList<>();
        for (var msg : signEntity.getFrontText().getMessages(false)) {
            lines.add(msg.getString().trim());
        }
        if (lines.size() < 3) return null;
        String header = lines.get(0).toLowerCase(Locale.ROOT);
        ShopType type = switch (header) {
            case "[buy]" -> ShopType.BUY;
            case "[sell]" -> ShopType.SELL;
            case "[admin buy]" -> ShopType.ADMIN_BUY;
            case "[admin sell]" -> ShopType.ADMIN_SELL;
            default -> null;
        };
        if (type == null) return null;
        int amount;
        double price;
        try {
            amount = Integer.parseInt(lines.get(1));
            price = Double.parseDouble(lines.get(2));
        } catch (Exception e) {
            return null;
        }
        return new ParsedSignShop(type, amount, price);
    }

    private static BlockPos findLinkedChest(Level level, BlockPos signPos) {
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

    private static net.minecraft.world.item.ItemStack inferFirstItemFromChest(Level level, BlockPos chestPos) {
        var be = level.getBlockEntity(chestPos);
        if (be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
            for (int i = 0; i < chest.getContainerSize(); i++) {
                var stack = chest.getItem(i);
                if (!stack.isEmpty()) return stack.copy();
            }
        }
        return null;
    }

    // Example: handle right-click on sign to transact
    @SubscribeEvent
    public void onRightClickSign(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();
        ShopRegistry.getBySign(pos).ifPresent(shop -> {
            event.setCanceled(true);
            transact((ServerPlayer)event.getEntity(), level, shop);
        });
    }

    private void transact(ServerPlayer actor, Level level, Shop shop) {
        synchronized (shop.mutex()) {
            // Double-check shop is still valid
            if (!ShopRegistry.getBySign(shop.signPos()).isPresent()) return;
            if (shop.type().isBuy()) {
                buyFlow(actor, level, shop);
            } else {
                sellFlow(actor, level, shop);
            }
        }
    }

    private void buyFlow(ServerPlayer buyer, Level level, Shop shop) {
        var econ = com.zerog.neoessentials.managers.EconomyManager.getInstance();
        boolean hasFunds = econ.hasBalance(buyer.getUUID(), shop.price());
        if (!hasFunds) {
            buyer.sendSystemMessage(MessageUtil.translatable(buyer, "neoessentials.shop.insufficient_funds"));
            return;
        }
        // Chest check (skip for admin shop)
        int found = Integer.MAX_VALUE;
        net.minecraft.world.level.block.entity.ChestBlockEntity chestEntity = null;
        if (!shop.type().isAdmin()) {
            var chest = level.getBlockEntity(shop.chestPos());
            if (!(chest instanceof net.minecraft.world.level.block.entity.ChestBlockEntity c)) {
                buyer.sendSystemMessage(MessageUtil.translatable(buyer, "neoessentials.shop.chest_missing"));
                return;
            }
            chestEntity = c;
            found = 0;
            for (int i = 0; i < chestEntity.getContainerSize(); i++) {
                var stack = chestEntity.getItem(i);
                if (!stack.isEmpty() && stack.is(shop.itemSpec().getItem())) {
                    found += stack.getCount();
                }
            }
            if (found < shop.amount()) {
                buyer.sendSystemMessage(MessageUtil.translatable(buyer, "neoessentials.shop.out_of_stock"));
                return;
            }
        }
        // Withdraw funds
        boolean withdrawn = econ.withdrawBalance(buyer.getUUID(), shop.price(), "Shop purchase");
        if (!withdrawn) {
            buyer.sendSystemMessage(MessageUtil.translatable(buyer, "neoessentials.shop.withdraw_failed"));
            return;
        }
        // Remove items from chest unless admin shop
        if (!shop.type().isAdmin()) {
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
        // Give items to buyer, check for inventory full
        var toGive = shop.itemSpec().copyWithCount(shop.amount());
        boolean added = buyer.getInventory().add(toGive);
        if (!added) {
            // Refund if inventory full
            econ.depositBalance(buyer.getUUID(), shop.price(), "Shop refund");
            buyer.sendSystemMessage(MessageUtil.translatable(buyer, "neoessentials.shop.inventory_full"));
            // If not admin, return items to chest
            if (!shop.type().isAdmin() && chestEntity != null) {
                for (int i = 0; i < chestEntity.getContainerSize(); i++) {
                    var stack = chestEntity.getItem(i);
                    if (stack.isEmpty()) {
                        chestEntity.setItem(i, toGive);
                        break;
                    }
                }
            }
            return;
        }
        // Pay owner and handle tax
        double tax = shop.price() * 0.05;
        double payout = shop.price() - tax;
        econ.depositBalance(shop.owner(), payout, "Shop sale");
        UUID taxAccount = new UUID(0, 0); // Replace with actual tax account if needed
        econ.depositBalance(taxAccount, tax, "Shop tax");
        String itemName = shop.itemSpec().getHoverName().getString();
        buyer.sendSystemMessage(MessageUtil.translatable(buyer, "neoessentials.shop.purchase_successful", String.valueOf(shop.amount()), itemName, String.format("%.2f", shop.price())));
    }

    private void sellFlow(ServerPlayer seller, Level level, Shop shop) {
        var econ = com.zerog.neoessentials.managers.EconomyManager.getInstance();
        // Check if seller has enough items
        int sellerCount = 0;
        for (int i = 0; i < seller.getInventory().getContainerSize(); i++) {
            var stack = seller.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(shop.itemSpec().getItem())) {
                sellerCount += stack.getCount();
            }
        }
        String itemName = shop.itemSpec().getHoverName().getString();
        if (sellerCount < shop.amount()) {
            seller.sendSystemMessage(MessageUtil.translatable(seller, "neoessentials.shop.not_enough_items", itemName));
            return;
        }
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
        if (!shop.type().isAdmin()) {
            var chest = level.getBlockEntity(shop.chestPos());
            if (!(chest instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chestEntity)) {
                // Refund items to seller
                seller.getInventory().add(shop.itemSpec().copyWithCount(shop.amount()));
                seller.sendSystemMessage(MessageUtil.translatable(seller, "neoessentials.shop.chest_missing"));
                return;
            }
            // Find first empty slot in chest
            ItemStack toAdd = shop.itemSpec().copyWithCount(shop.amount());
            boolean added = false;
            for (int i = 0; i < chestEntity.getContainerSize(); i++) {
                var stack = chestEntity.getItem(i);
                if (stack.isEmpty()) {
                    chestEntity.setItem(i, toAdd);
                    toAdd = null;
                    added = true;
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
                            added = true;
                            if (toAdd.getCount() <= 0) {
                                toAdd = null;
                                break;
                            }
                        }
                    }
                }
            }
            if (toAdd != null && toAdd.getCount() > 0) {
                // Chest is full, refund items to seller
                seller.getInventory().add(toAdd);
                seller.sendSystemMessage(MessageUtil.translatable(seller, "neoessentials.shop.chest_full"));
                return;
            }
        }
        // Economy deposit
        boolean deposited = econ.depositBalance(seller.getUUID(), shop.price(), "Shop sale");
        if (!deposited) {
            seller.sendSystemMessage(MessageUtil.translatable(seller, "neoessentials.shop.deposit_failed"));
            // Refund items to seller
            seller.getInventory().add(shop.itemSpec().copyWithCount(shop.amount()));
            return;
        }
        // Handle tax
        double tax = shop.price() * 0.05;
        UUID taxAccount = new UUID(0, 0); // Replace with actual tax account if needed
        econ.depositBalance(taxAccount, tax, "Shop tax");
        seller.sendSystemMessage(MessageUtil.translatable(seller, "neoessentials.shop.sale_successful", String.valueOf(shop.amount()), itemName, String.format("%.2f", shop.price())));
    }

    // Scans all loaded chunks for sign shops and registers any missing ones
    public static void scanWorldForSignShops(Level level) {
        // Not supported: No public API for iterating loaded chunks/block entities in this environment
        System.err.println("scanWorldForSignShops: Unable to scan for sign shops - no public chunk/block entity iterable available in this environment.");
    }
}