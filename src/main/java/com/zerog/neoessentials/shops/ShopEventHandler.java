package com.zerog.neoessentials.shops;

import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.chat.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;

/**
 * Event hooks for shop creation and transactions
 */
public class ShopEventHandler {
    // Temporary linking map (player UUID -> last chest clicked)
    private static final Map<UUID, BlockPos> linkingChest = new ConcurrentHashMap<>();

    // --- Handle sign shop creation when a sign is placed ---
    @SubscribeEvent
    public void onSignPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;
        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity signEntity)) return;
        // Get all lines as strings
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            lines.add(signEntity.getFrontText().getMessage(i, false).getString().trim());
        }
        // Early exit if not a shop header
        String header = lines.get(0).toLowerCase(Locale.ROOT);
        if (!(header.equals("buy") || header.equals("[buy]") ||
              header.equals("sell") || header.equals("[sell]") ||
              header.equals("admin buy") || header.equals("[admin buy]") ||
              header.equals("admin sell") || header.equals("[admin sell]") ||
              header.equals("shop") || header.equals("[shop]") ||
              header.equals("admin shop") || header.equals("[admin shop]"))) {
            return;
        }
        // Multi-line tag support: if line 2 starts with # and line 3 is not a price, concatenate
        String line2 = lines.size() > 1 ? lines.get(1) : "";
        String line3 = lines.size() > 2 ? lines.get(2) : "";
        String line4 = lines.size() > 3 ? lines.get(3) : "";
        if (line2.startsWith("#")) {
            if (!line3.matches("(?i)^(B:|S:|\\d+|\\s*)") && !line3.isEmpty()) {
                lines.set(1, line2 + line3);
                lines.set(2, line4); // shift up
                lines.set(3, "");
            }
        }
        ParsedSignShop parsed = parseSignShopLines(lines, event.getEntity().getName().getString());
        if (parsed == null) {
            if (event.getEntity() instanceof ServerPlayer player) {
                player.sendSystemMessage(MessageUtil.translatable(player, "neoessentials.shop.invalid_sign_format"));
            }
            return;
        }
        if (parsed.amount <= 0) {
            if (event.getEntity() instanceof ServerPlayer player) {
                player.sendSystemMessage(MessageUtil.translatable(player, "neoessentials.shop.invalid_amount"));
            }
            return;
        }
        BlockPos storagePos = null;
        if (!parsed.type.isAdmin()) {
            storagePos = findLinkedStorage(level, pos);
            if (storagePos == null) {
                if (event.getEntity() instanceof ServerPlayer player) {
                    player.sendSystemMessage(MessageUtil.translatable(player, "neoessentials.shop.no_chest_found"));
                }
                return;
            }
            if (ShopRegistry.getByChest(storagePos).isPresent()) {
                if (event.getEntity() instanceof ServerPlayer player) {
                    player.sendSystemMessage(MessageUtil.translatable(player, "neoessentials.shop.chest_already_linked"));
                }
                return;
            }
        }
        net.minecraft.world.item.ItemStack itemSpec = null;
        boolean autodetected = false;
        if (parsed.type.isAdmin() || parsed.sellPrice != null) {
            itemSpec = inferFirstItemFromStorage(level, storagePos);
            if (itemSpec == null || itemSpec.isEmpty()) {
                if (event.getEntity() instanceof ServerPlayer player) {
                    player.sendSystemMessage(MessageUtil.translatable(player, "neoessentials.shop.no_item_for_admin_shop"));
                }
                return;
            }
            // Autodetect quantity if item line is empty
            if (parsed.itemOrTag == null || parsed.itemOrTag.isEmpty()) {
                parsed = new ParsedSignShop(parsed.type, itemSpec.getCount(), parsed.itemOrTag, parsed.isTag, parsed.buyPrice, parsed.sellPrice, parsed.sellerName);
                autodetected = true;
            }
        } else {
            if (parsed.itemOrTag != null) {
                itemSpec = ShopItemUtil.itemStackFromName(parsed.itemOrTag, parsed.amount);
                if (itemSpec == null || itemSpec.isEmpty()) {
                    if (event.getEntity() instanceof ServerPlayer player) {
                        player.sendSystemMessage(MessageUtil.translatable(player, "neoessentials.shop.invalid_item", parsed.itemOrTag));
                    }
                    return;
                }
            } else if (!parsed.type.isAdmin()) {
                itemSpec = inferFirstItemFromStorage(level, storagePos);
                if (itemSpec == null || itemSpec.isEmpty()) {
                    if (event.getEntity() instanceof ServerPlayer player) {
                        player.sendSystemMessage(MessageUtil.translatable(player, "neoessentials.shop.no_item_in_chest"));
                    }
                    return;
                }
                // Autodetect quantity if item line is empty
                parsed = new ParsedSignShop(parsed.type, itemSpec.getCount(), parsed.itemOrTag, parsed.isTag, parsed.buyPrice, parsed.sellPrice, parsed.sellerName);
                autodetected = true;
            }
        }
        // If autodetected, update the sign with the item name and quantity
        if (autodetected && signEntity != null) {
            String displayName = itemSpec.getHoverName().getString();
            String newItemLine = displayName + " x" + parsed.amount;
            signEntity.updateText((frontText) -> frontText.setMessage(1, Component.literal(newItemLine)), true);
            signEntity.setChanged();
            level.sendBlockUpdated(pos, signEntity.getBlockState(), signEntity.getBlockState(), 3);
        }
        ResourceLocation tagId = null;
        if (parsed.isTag && parsed.itemOrTag != null && parsed.itemOrTag.startsWith("#")) {
            try {
                String tagRaw = parsed.itemOrTag.substring(1);
                String[] tagParts = tagRaw.split(":", 2);
                if (tagParts.length == 2) {
                    tagId = ResourceLocation.tryParse(tagParts[0] + ":" + tagParts[1]);
                    if (tagId == null) throw new IllegalArgumentException("Invalid tag format: " + parsed.itemOrTag);
                } else {
                    throw new IllegalArgumentException("Invalid tag format: " + parsed.itemOrTag);
                }
            } catch (Exception e) {
                if (event.getEntity() instanceof ServerPlayer player) {
                    player.sendSystemMessage(MessageUtil.translatable(player, "neoessentials.shop.invalid_tag", parsed.itemOrTag));
                }
                return;
            }
        }
        Shop shop = new Shop(pos, storagePos, event.getEntity().getUUID(), parsed.type, itemSpec, parsed.amount, parsed.buyPrice != null ? parsed.buyPrice : parsed.sellPrice);
        ShopRegistry.register(shop);
        net.minecraft.network.chat.Component[] newLines = new net.minecraft.network.chat.Component[4];
        newLines[0] = net.minecraft.network.chat.Component.literal(shop.type().toString());
        newLines[1] = net.minecraft.network.chat.Component.literal(shop.amount() + "x " + shop.itemSpec().getHoverName().getString());
        newLines[2] = net.minecraft.network.chat.Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.shop.price_format", String.format("%.2f", shop.price())));
        newLines[3] = net.minecraft.network.chat.Component.literal(
            shop.chestPos() != null ?
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.shop.chest_location", shop.chestPos().getX(), shop.chestPos().getY(), shop.chestPos().getZ()) :
                "ADMIN SHOP"
        );
        // NeoForge 1.21+: Use updateText API to set all lines
        signEntity.updateText((frontText) -> {
            return frontText.setMessage(0, newLines[0])
                            .setMessage(1, newLines[1])
                            .setMessage(2, newLines[2])
                            .setMessage(3, newLines[3]);
        }, true);
        signEntity.setChanged();
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
        if (event.getEntity() instanceof ServerPlayer player) {
            player.sendSystemMessage(MessageUtil.translatable(player, "neoessentials.shop.created", shop.type(), shop.amount(), shop.itemSpec().getHoverName().getString(), shop.price()));
        }
    }
    // --- Remove or comment out the old sign text changed handler ---
    // @SubscribeEvent
    // public void onSignTextChanged(SignTextChangedEvent event) {
    //     // ...old logic, now disabled...
    // }

    // --- Helper: parse sign shop from lines (for multi-line tag support) ---
    private static ParsedSignShop parseSignShopLines(List<String> lines, String defaultSellerName) {
        if (lines.size() < 3) return null;
        String header = lines.get(0).toLowerCase(Locale.ROOT);
        ShopType type = switch (header) {
            case "buy", "[buy]" -> ShopType.BUY;
            case "sell", "[sell]" -> ShopType.SELL;
            case "admin buy", "[admin buy]" -> ShopType.ADMIN_BUY;
            case "admin sell", "[admin sell]" -> ShopType.ADMIN_SELL;
            case "shop", "[shop]" -> ShopType.BUY;
            case "admin shop", "[admin shop]" -> ShopType.ADMIN_BUY;
            default -> null;
        };
        if (type == null) return null;
        // Parse line 2: amount x item/tag
        ShopItemUtil.ParsedAmountItem parsedAmountItem = ShopItemUtil.parseAmountAndItem(lines.get(1));
        if (parsedAmountItem == null) return null;
        int amount = parsedAmountItem.amount;
        String itemOrTag = parsedAmountItem.itemOrTag;
        boolean isTag = parsedAmountItem.isTag;
        // Parse line 3: B: amount | S: amount (either or both, order flexible)
        Double buyPrice = null, sellPrice = null;
        String priceLine = lines.get(2).replace(" ", "");
        for (String part : priceLine.split("\\|")) {
            if (part.toUpperCase().startsWith("B:")) {
                try { buyPrice = Double.parseDouble(part.substring(2)); } catch (Exception ignored) {}
            } else if (part.toUpperCase().startsWith("S:")) {
                try { sellPrice = Double.parseDouble(part.substring(2)); } catch (Exception ignored) {}
            }
        }
        if (buyPrice == null && sellPrice == null) return null;
        // Parse line 4: seller name (or use default)
        String sellerName = (lines.size() > 3 && !lines.get(3).isEmpty()) ? lines.get(3) : defaultSellerName;
        return new ParsedSignShop(type, amount, itemOrTag, isTag, buyPrice, sellPrice, sellerName);
    }

    // Updated ParsedSignShop to support new format
    private static class ParsedSignShop {
        public final ShopType type;
        public final int amount;
        public final String itemOrTag;
        public final boolean isTag;
        public final Double buyPrice;
        public final Double sellPrice;
        public final String sellerName;
        public ParsedSignShop(ShopType type, int amount, String itemOrTag, boolean isTag, Double buyPrice, Double sellPrice, String sellerName) {
            this.type = type;
            this.amount = amount;
            this.itemOrTag = itemOrTag;
            this.isTag = isTag;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
            this.sellerName = sellerName;
        }
    }

    private static BlockPos findLinkedStorage(Level level, BlockPos signPos) {
        // Check adjacent blocks for any storage (Container) block entity
        for (var dir : net.minecraft.core.Direction.values()) {
            BlockPos adj = signPos.relative(dir);
            var be = level.getBlockEntity(adj);
            if (be instanceof net.minecraft.world.Container) {
                return adj;
            }
        }
        return null;
    }

    private static net.minecraft.world.item.ItemStack inferFirstItemFromStorage(Level level, BlockPos storagePos) {
        var be = level.getBlockEntity(storagePos);
        if (be instanceof net.minecraft.world.Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                var stack = container.getItem(i);
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

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();
        var state = level.getBlockState(pos);
        ServerPlayer player = (ServerPlayer) event.getEntity();

        // 1) If chest clicked, store for linking
        if (state.is(Blocks.CHEST)) {
            linkingChest.put(player.getUUID(), pos);
            player.sendSystemMessage(Component.literal("Chest selected. Now punch the sign to link."));
            event.setCanceled(true);
            return;
        }

        // 2) If sign clicked, handle shop creation or transaction
        if (state.getBlock() instanceof SignBlock) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof SignBlockEntity signEntity)) return;
            // Get sign text (front)
            var front = signEntity.getFrontText();
            String line4 = front.getMessage(3, false).getString();

            // If player has a chest waiting, create shop
            BlockPos storagePos = linkingChest.remove(player.getUUID());
            if (storagePos != null) {
                // Parse price from line 4
                double price = parsePrice(line4);
                // Infer item from chest (first non-empty slot)
                ItemStack item = inferFirstItemFromStorage(level, storagePos);
                if (item == null) {
                    player.sendSystemMessage(Component.literal("Chest is empty. Put items in chest first."));
                    return;
                }
                // Create and register shop (fix constructor order)
                Shop shop = new Shop(pos, storagePos, player.getUUID(), ShopType.BUY, item, 1, price);
                ShopRegistry.register(shop);
                player.sendSystemMessage(Component.literal("Shop created."));
                updateSignState(level, pos, "IN STOCK");
                event.setCanceled(true);
                return;
            }

            // Otherwise, process transaction as before
            ShopRegistry.getBySign(pos).ifPresent(shop -> {
                event.setCanceled(true);
                transact(player, level, shop);
            });
        }
    }

    // Helper: parse price from string (e.g. "$10.50")
    private static double parsePrice(String raw) {
        // Place dash at the end to avoid illegal escape character
        String cleaned = raw.replaceAll("[^0-9.-]", "");
        if (cleaned.isEmpty()) return 0.0;
        return Double.parseDouble(cleaned);
    }

    // Helper: update sign text to show state
    private static void updateSignState(Level world, BlockPos signPos, String state) {
        BlockEntity be = world.getBlockEntity(signPos);
        if (!(be instanceof SignBlockEntity sign)) return;
        // NeoForge 1.21+: use updateText API
        sign.updateText((frontText) -> frontText.setMessage(3, Component.literal(state)), true);
        sign.setChanged();
        world.sendBlockUpdated(signPos, world.getBlockState(signPos), world.getBlockState(signPos), 3);
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
        // Storage check (skip for admin shop)
        int found = Integer.MAX_VALUE;
        net.minecraft.world.Container storageEntity = null;
        if (!shop.type().isAdmin()) {
            var storage = level.getBlockEntity(shop.chestPos());
            if (!(storage instanceof net.minecraft.world.Container c)) {
                buyer.sendSystemMessage(MessageUtil.translatable(buyer, "neoessentials.shop.chest_missing"));
                return;
            }
            storageEntity = c;
            found = 0;
            for (int i = 0; i < storageEntity.getContainerSize(); i++) {
                var stack = storageEntity.getItem(i);
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
        // Remove items from storage unless admin shop
        if (!shop.type().isAdmin()) {
            int toRemove = shop.amount();
            for (int i = 0; i < storageEntity.getContainerSize() && toRemove > 0; i++) {
                var stack = storageEntity.getItem(i);
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
            // If not admin, return items to storage
            if (!shop.type().isAdmin() && storageEntity != null) {
                for (int i = 0; i < storageEntity.getContainerSize(); i++) {
                    var stack = storageEntity.getItem(i);
                    if (stack.isEmpty()) {
                        storageEntity.setItem(i, toGive);
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
        // Add items to storage unless admin shop
        if (!shop.type().isAdmin()) {
            var storage = level.getBlockEntity(shop.chestPos());
            if (!(storage instanceof net.minecraft.world.Container storageEntity)) {
                // Refund items to seller
                seller.getInventory().add(shop.itemSpec().copyWithCount(shop.amount()));
                seller.sendSystemMessage(MessageUtil.translatable(seller, "neoessentials.shop.chest_missing"));
                return;
            }
            // Find first empty slot in storage
            ItemStack toAdd = shop.itemSpec().copyWithCount(shop.amount());
            boolean added = false;
            for (int i = 0; i < storageEntity.getContainerSize(); i++) {
                var stack = storageEntity.getItem(i);
                if (stack.isEmpty()) {
                    storageEntity.setItem(i, toAdd);
                    toAdd = null;
                    added = true;
                    break;
                }
            }
            // If no empty slot, try to merge with existing stacks
            if (toAdd != null) {
                for (int i = 0; i < storageEntity.getContainerSize(); i++) {
                    var stack = storageEntity.getItem(i);
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
                // Storage is full, refund items to seller
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

    @SubscribeEvent
    public void onBlockBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();
        // Remove shop if sign is broken
        if (ShopRegistry.getBySign(pos).isPresent()) {
            ShopRegistry.remove(pos);
            if (event.getPlayer() instanceof ServerPlayer player) {
                player.sendSystemMessage(MessageUtil.translatable(player, "neoessentials.shop.removed"));
            }
        } else {
            // Remove shop if chest is broken
            Optional<Shop> shop = ShopRegistry.getByChest(pos);
            if (shop.isPresent()) {
                ShopRegistry.remove(shop.get().signPos());
                if (event.getPlayer() instanceof ServerPlayer player) {
                    player.sendSystemMessage(MessageUtil.translatable(player, "neoessentials.shop.removed"));
                }
            }
        }
    }
}
