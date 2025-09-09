package com.zerog.neoessentials.shops;

import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import com.zerog.neoessentials.economy.shops.ShopManager.SignShop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

/**
 * Handles player sign shop operations
 * Player shops are chest-based with limited inventory and player balance checks
 */
public class PlayerSignShopHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerSignShopHandler.class);
    
    public static boolean handleBuyTransaction(Player player, SignShop signShop, Level level, int quantity) {
        // Check if shop has enough stock - but do REAL-TIME check, not just recorded stock
        BlockPos chestPos = signShop.getChestPos();
        if (chestPos == null) {
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.chest.not.connected", new Object[]{}));
            return false;
        }
        
        if (!(level.getBlockEntity(chestPos) instanceof ChestBlockEntity chestEntity)) {
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.chest.not.found", new Object[]{}));
            return false;
        }
        
        // CRITICAL: Check ACTUAL chest inventory FIRST - this prevents duplication bugs
        int actualItemsInChest = countItemsInChest(chestEntity, signShop.getItem());
        LOGGER.info("ANTI-DUPE CHECK: Player {} attempting to buy {}x {} from shop. Chest actually contains: {} items", 
                   player.getName().getString(), quantity, signShop.getItem().getDisplayName().getString(), actualItemsInChest);
        
        if (actualItemsInChest == 0) {
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.chest.empty.dupe.protection", new Object[]{}));
            LOGGER.warn("BLOCKED POTENTIAL DUPE: Player {} tried to buy from empty chest at {}", 
                       player.getName().getString(), chestPos);
            return false;
        }
        
        if (actualItemsInChest < quantity) {
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.chest.insufficient.stock", new Object[]{actualItemsInChest, signShop.getItem().getDisplayName().getString(), quantity}));
            LOGGER.warn("BLOCKED INSUFFICIENT STOCK: Player {} tried to buy {}x but chest only has {}x at {}", 
                       player.getName().getString(), quantity, actualItemsInChest, chestPos);
            return false;
        }
        
        // Legacy recorded stock check (keeping for compatibility)
        if (!signShop.hasStock() || signShop.getStock() < quantity) {
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.legacy.not.enough.stock", new Object[]{}));
            return false;
        }
        
        double totalPrice = signShop.getBuyPrice() * quantity;
        
        // Get economy manager instance
        com.zerog.neoessentials.managers.EconomyManager economyManager = 
            com.zerog.neoessentials.managers.EconomyManager.getInstance();
        
        if (economyManager == null) {
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.economy.unavailable", new Object[]{}));
            return false;
        }
        
        // Check if player has sufficient balance
        if (!economyManager.hasBalance(player.getUUID(), totalPrice)) {
            double currentBalance = economyManager.getBalance(player.getUUID()).doubleValue();
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.not.enough.money", new Object[]{quantity, signShop.getItem().getDisplayName().getString(), economyManager.formatCurrency(totalPrice), economyManager.formatCurrency(currentBalance)}));
            return false;
        }
        
        // Check chest inventory for items (redundant check for extra safety)
        int availableItems = countItemsInChest(chestEntity, signShop.getItem());
        if (availableItems < quantity) {
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.chest.only.has", new Object[]{availableItems, signShop.getItem().getDisplayName().getString()}));
            return false;
        }
        
        LOGGER.info("Processing player shop BUY transaction for player {}: buying {}x {} for {} from shop owner {}", 
                   player.getName().getString(), quantity, signShop.getItem().getDisplayName().getString(), 
                   totalPrice, signShop.getOwnerId());
        
        // STEP 1: Withdraw payment FIRST (critical for preventing duplication)
        boolean withdrawSuccess = economyManager.withdrawBalance(player.getUUID(), totalPrice, 
            "Bought " + quantity + "x " + signShop.getItem().getDisplayName().getString() + " from player shop");
        
        if (!withdrawSuccess) {
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.payment.failed", new Object[]{}));
            return false;
        }
        
        // STEP 2: Remove items from chest
        if (!removeItemsFromChest(chestEntity, signShop.getItem(), quantity)) {
            // Failed to remove items - refund player
            economyManager.depositBalance(player.getUUID(), totalPrice, "Refund: Shop out of stock");
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.remove.items.failed.refunded", new Object[]{}));
            return false;
        }
        
        // STEP 3: Give items to player
        ItemStack itemToGive = signShop.getItem().copy();
        itemToGive.setCount(quantity);
        
        if (!player.getInventory().add(itemToGive)) {
            // Inventory full - try to return items to chest and refund
            ItemStack returnItem = signShop.getItem().copy();
            returnItem.setCount(quantity);
            addItemsToChest(chestEntity, returnItem);
            economyManager.depositBalance(player.getUUID(), totalPrice, "Refund: Inventory full");
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.inventory.full.refunded", new Object[]{}));
            return false;
        }
        
        // STEP 4: Pay shop owner
        UUID shopOwnerUUID = UUID.fromString(signShop.getOwnerId());
        boolean depositSuccess = economyManager.depositBalance(shopOwnerUUID, totalPrice, 
            "Shop sale: " + quantity + "x " + signShop.getItem().getDisplayName().getString());
        
        if (!depositSuccess) {
            LOGGER.warn("Failed to pay shop owner {} for sale", signShop.getOwnerId());
            // Transaction still successful from buyer's perspective
        }
        
        // Update shop stock
        com.zerog.neoessentials.economy.shops.ShopManager.getInstance().updateSignShopStock(signShop.getSignPos(), signShop.getStock() - quantity);
        
    MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.bought", new Object[]{quantity, signShop.getItem().getDisplayName().getString(), economyManager.formatCurrency(totalPrice)}));
        
        // Record transaction
        com.zerog.neoessentials.economy.shops.ShopManager.getInstance().recordShopTransaction(signShop, "BUY", totalPrice, quantity);
        
        LOGGER.info("Player shop BUY transaction completed for player {}: {}x {} for {}", 
                   player.getName().getString(), quantity, signShop.getItem().getDisplayName().getString(), totalPrice);
        
        return true;
    }
    
    public static boolean handleSellTransaction(Player player, SignShop signShop, Level level, int quantity) {
        // Check if player has items to sell
        if (!hasItemInInventory(player, signShop.getItem(), quantity)) {
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.not.enough.to.sell", new Object[]{signShop.getItem().getDisplayName().getString()}));
            return false;
        }
        
        double totalEarnings = signShop.getSellPrice() * quantity;
        
        // Get economy manager instance
        com.zerog.neoessentials.managers.EconomyManager economyManager = 
            com.zerog.neoessentials.managers.EconomyManager.getInstance();
        
        if (economyManager == null || !economyManager.isEnabled()) {
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.economy.unavailable", new Object[]{}));
            return false;
        }
        
        // Check if shop owner has enough money
        UUID shopOwnerUUID = UUID.fromString(signShop.getOwnerId());
        if (!economyManager.hasBalance(shopOwnerUUID, totalEarnings)) {
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.owner.not.enough.money", new Object[]{}));
            return false;
        }
        
        // Check if chest has space for items
        BlockPos chestPos = signShop.getChestPos();
        if (chestPos == null) {
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.chest.not.connected", new Object[]{}));
            return false;
        }
        
        if (!(level.getBlockEntity(chestPos) instanceof ChestBlockEntity chestEntity)) {
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.chest.not.found", new Object[]{}));
            return false;
        }
        
        // Check if chest has space
        ItemStack testItem = signShop.getItem().copy();
        testItem.setCount(quantity);
        if (!canChestFitItem(chestEntity, testItem)) {
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.chest.full", new Object[]{}));
            return false;
        }
        
        LOGGER.info("Processing player shop SELL transaction for player {}: selling {}x {} for {} to shop owner {}", 
                   player.getName().getString(), quantity, signShop.getItem().getDisplayName().getString(), 
                   totalEarnings, signShop.getOwnerId());
        
        // STEP 1: Remove items from player inventory first (payment-first security model)
        if (!removeItemFromInventory(player, signShop.getItem(), quantity)) {
            player.sendSystemMessage(MessageUtil.translatable("neoessentials.shop.remove.items.failed.inventory"));
            return false;
        }
        
        // STEP 2: Charge shop owner
        boolean withdrawSuccess = economyManager.withdrawBalance(shopOwnerUUID, totalEarnings, 
            "Shop purchase: " + quantity + "x " + signShop.getItem().getDisplayName().getString());
        
        if (!withdrawSuccess) {
            // Return items to player since shop owner payment failed
            ItemStack returnItem = signShop.getItem().copy();
            returnItem.setCount(quantity);
            player.getInventory().add(returnItem);
            player.sendSystemMessage(MessageUtil.translatable("neoessentials.shop.owner.payment.failed.items.returned"));
            return false;
        }
        
        // STEP 3: Pay player
        boolean depositSuccess = economyManager.depositBalance(player.getUUID(), totalEarnings, 
            "Sold " + quantity + "x " + signShop.getItem().getDisplayName().getString() + " to player shop");
        
        if (!depositSuccess) {
            // Refund shop owner and return items to player
            economyManager.depositBalance(shopOwnerUUID, totalEarnings, "Refund: Player payment failed");
            ItemStack returnItem = signShop.getItem().copy();
            returnItem.setCount(quantity);
            player.getInventory().add(returnItem);
            player.sendSystemMessage(MessageUtil.translatable("neoessentials.shop.payment.failed.items.returned"));
            return false;
        }
        
        // STEP 4: Add items to chest
        ItemStack itemsToAdd = signShop.getItem().copy();
        itemsToAdd.setCount(quantity);
        addItemsToChest(chestEntity, itemsToAdd);
        
        player.sendSystemMessage(MessageUtil.translatable("neoessentials.shop.sold", 
            String.valueOf(quantity), 
            signShop.getItem().getDisplayName().getString(), 
            economyManager.formatCurrency(totalEarnings)));
        
        // Record transaction
        com.zerog.neoessentials.economy.shops.ShopManager.getInstance().recordShopTransaction(signShop, "SELL", totalEarnings, quantity);
        
        LOGGER.info("Player shop SELL transaction completed for player {}: {}x {} for {}", 
                   player.getName().getString(), quantity, signShop.getItem().getDisplayName().getString(), totalEarnings);
        
        return true;
    }
    
    private static boolean hasItemInInventory(Player player, ItemStack targetItem, int requiredQuantity) {
        int foundQuantity = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItem(stack, targetItem)) {
                foundQuantity += stack.getCount();
                if (foundQuantity >= requiredQuantity) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private static boolean removeItemFromInventory(Player player, ItemStack targetItem, int quantityToRemove) {
        int remaining = quantityToRemove;
        for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (ItemStack.isSameItem(stack, targetItem)) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }
        return remaining == 0;
    }
    
    public static int countItemsInChest(ChestBlockEntity chest, ItemStack targetItem) {
        int count = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack stack = chest.getItem(i);
            if (ItemStack.isSameItem(stack, targetItem)) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    private static boolean removeItemsFromChest(ChestBlockEntity chest, ItemStack targetItem, int quantityToRemove) {
        int remaining = quantityToRemove;
        for (int i = 0; i < chest.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = chest.getItem(i);
            if (ItemStack.isSameItem(stack, targetItem)) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
                if (stack.isEmpty()) {
                    chest.setItem(i, ItemStack.EMPTY);
                }
            }
        }
        chest.setChanged();
        return remaining == 0;
    }
    
    private static boolean canChestFitItem(ChestBlockEntity chest, ItemStack itemToAdd) {
        // Try to add items to existing stacks first
        int remaining = itemToAdd.getCount();
        for (int i = 0; i < chest.getContainerSize() && remaining > 0; i++) {
            ItemStack existing = chest.getItem(i);
            if (existing.isEmpty()) {
                remaining -= Math.min(remaining, itemToAdd.getMaxStackSize());
            } else if (ItemStack.isSameItem(existing, itemToAdd)) {
                int canAdd = Math.min(remaining, itemToAdd.getMaxStackSize() - existing.getCount());
                remaining -= canAdd;
            }
        }
        return remaining <= 0;
    }
    
    private static void addItemsToChest(ChestBlockEntity chest, ItemStack itemToAdd) {
        int remaining = itemToAdd.getCount();
        
        // First, try to add to existing stacks
        for (int i = 0; i < chest.getContainerSize() && remaining > 0; i++) {
            ItemStack existing = chest.getItem(i);
            if (!existing.isEmpty() && ItemStack.isSameItem(existing, itemToAdd)) {
                int canAdd = Math.min(remaining, itemToAdd.getMaxStackSize() - existing.getCount());
                existing.grow(canAdd);
                remaining -= canAdd;
            }
        }
        
        // Then, add to empty slots
        for (int i = 0; i < chest.getContainerSize() && remaining > 0; i++) {
            ItemStack existing = chest.getItem(i);
            if (existing.isEmpty()) {
                int toAdd = Math.min(remaining, itemToAdd.getMaxStackSize());
                ItemStack newStack = itemToAdd.copy();
                newStack.setCount(toAdd);
                chest.setItem(i, newStack);
                remaining -= toAdd;
            }
        }
        
        chest.setChanged();
    }
}
