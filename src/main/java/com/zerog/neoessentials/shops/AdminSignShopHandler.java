package com.zerog.neoessentials.shops;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.shops.ShopManager.SignShop;
import com.zerog.neoessentials.util.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * Handles admin sign shop operations
 * Admin shops have infinite inventory and money
 */
public class AdminSignShopHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminSignShopHandler.class);
    
    public static boolean handleBuyTransaction(Player player, SignShop signShop, Level level, int quantity) {
        // Admin shops always have stock (infinite)
        double totalPrice = signShop.getBuyPrice() * quantity;
        
        // Get economy manager instance
        com.zerog.neoessentials.managers.EconomyManager economyManager = 
            com.zerog.neoessentials.managers.EconomyManager.getInstance();
        
        if (economyManager == null) {
            player.sendSystemMessage(MessageUtil.translatable("neoessentials.shop.economy.unavailable"));
            return false;
        }
        
        // Check if player has sufficient balance
        if (!economyManager.hasBalance(player.getUUID(), totalPrice)) {
            double currentBalance = economyManager.getBalance(player.getUUID()).doubleValue();
            player.sendSystemMessage(MessageUtil.translatable("neoessentials.shop.not.enough.money", 
                String.valueOf(quantity), 
                signShop.getItem().getDisplayName().getString(), 
                economyManager.formatCurrency(totalPrice), 
                economyManager.formatCurrency(currentBalance)));
            return false;
        }
        
        // Create item stack to give to player
        ItemStack itemToGive = signShop.getItem().copy();
        itemToGive.setCount(quantity);
        
        // Give item to player
        if (!player.getInventory().add(itemToGive)) {
            player.sendSystemMessage(MessageUtil.translatable("neoessentials.shop.inventory.full"));
            return false;
        }
        
        LOGGER.info("Items given to player, now processing payment for admin shop transaction");
        
        // Charge player (payment-first security model already implemented above by checking inventory space first)
        boolean withdrawSuccess = economyManager.withdrawBalance(player.getUUID(), totalPrice, 
            "Bought " + quantity + "x " + signShop.getItem().getDisplayName().getString() + " from admin shop");
        
        if (!withdrawSuccess) {
            // Remove the item we just gave since payment failed
            removeItemFromInventory(player, signShop.getItem(), quantity, null);
            player.sendSystemMessage(MessageUtil.translatable("neoessentials.shop.payment.failed.cancelled"));
            return false;
        }
        
        // No need to update stock for admin shops - infinite
        
        player.sendSystemMessage(MessageUtil.translatable("neoessentials.shop.bought", 
            String.valueOf(quantity), 
            signShop.getItem().getDisplayName().getString(), 
            economyManager.formatCurrency(totalPrice)));
        
        // Record transaction
        com.zerog.neoessentials.shops.ShopManager.getInstance().recordShopTransaction(signShop, "BUY", totalPrice, quantity);
        
        LOGGER.info("Admin shop BUY transaction completed for player {}: {}x {} for {}", 
                   player.getName().getString(), quantity, signShop.getItem().getDisplayName().getString(), totalPrice);
        
        return true;
    }
    
    // Overload: support tag-based matching
    private static boolean hasItemInInventory(Player player, ItemStack targetItem, int requiredQuantity, @javax.annotation.Nullable ResourceLocation tagId) {
        int foundQuantity = 0;
        if (tagId != null) {
            TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), tagId);
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty() && stack.is(tag)) {
                    foundQuantity += stack.getCount();
                    if (foundQuantity >= requiredQuantity) {
                        return true;
                    }
                }
            }
        } else {
            for (ItemStack stack : player.getInventory().items) {
                if (ItemStack.isSameItem(stack, targetItem)) {
                    foundQuantity += stack.getCount();
                    if (foundQuantity >= requiredQuantity) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Overload: support tag-based matching
    private static boolean removeItemFromInventory(Player player, ItemStack targetItem, int quantityToRemove, @javax.annotation.Nullable ResourceLocation tagId) {
        int remaining = quantityToRemove;
        if (tagId != null) {
            TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), tagId);
            for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
                ItemStack stack = player.getInventory().items.get(i);
                if (!stack.isEmpty() && stack.is(tag)) {
                    int toRemove = Math.min(remaining, stack.getCount());
                    stack.shrink(toRemove);
                    remaining -= toRemove;
                }
            }
        } else {
            for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
                ItemStack stack = player.getInventory().items.get(i);
                if (ItemStack.isSameItem(stack, targetItem)) {
                    int toRemove = Math.min(remaining, stack.getCount());
                    stack.shrink(toRemove);
                    remaining -= toRemove;
                }
            }
        }
        return remaining == 0;
    }

    // Update usages in handleSellTransaction to use tag-aware methods
    public static boolean handleSellTransaction(Player player, SignShop signShop, Level level, int quantity) {
        // Tag-based matching not used; tagId always null
        ResourceLocation tagId = null;
        if (!hasItemInInventory(player, signShop.getItem(), quantity, tagId)) {
            player.sendSystemMessage(MessageUtil.translatable("neoessentials.shop.not.enough.to.sell", 
                signShop.getItem().getDisplayName().getString()));
            return false;
        }
        double totalEarnings = signShop.getSellPrice() * quantity;
        com.zerog.neoessentials.managers.EconomyManager economyManager = 
            com.zerog.neoessentials.managers.EconomyManager.getInstance();
        if (economyManager == null || !economyManager.isEnabled()) {
            player.sendSystemMessage(MessageUtil.translatable("neoessentials.shop.economy.unavailable"));
            return false;
        }
        LOGGER.info("Processing admin shop SELL transaction for player {}", player.getName().getString());
        if (!removeItemFromInventory(player, signShop.getItem(), quantity, tagId)) {
            player.sendSystemMessage(MessageUtil.translatable("neoessentials.shop.remove.items.failed"));
            return false;
        }
        boolean depositSuccess = economyManager.depositBalance(player.getUUID(), totalEarnings, 
            "Sold " + quantity + "x " + signShop.getItem().getDisplayName().getString() + " to admin shop");
        
        if (!depositSuccess) {
            // Return items since payment failed
            ItemStack returnItem = signShop.getItem().copy();
            returnItem.setCount(quantity);
            player.getInventory().add(returnItem);
            player.sendSystemMessage(MessageUtil.translatable("neoessentials.shop.payment.failed.items.returned"));
            return false;
        }
        
        player.sendSystemMessage(MessageUtil.translatable("neoessentials.shop.sold", 
            String.valueOf(quantity), 
            signShop.getItem().getDisplayName().getString(), 
            economyManager.formatCurrency(totalEarnings)));
        
        // Record transaction (singleton ShopManager)
        com.zerog.neoessentials.shops.ShopManager.getInstance().recordShopTransaction(signShop, "SELL", totalEarnings, quantity);
        
        LOGGER.info("Admin shop SELL transaction completed for player {}: {}x {} for {}", 
                   player.getName().getString(), quantity, signShop.getItem().getDisplayName().getString(), totalEarnings);
        
        return true;
    }
}