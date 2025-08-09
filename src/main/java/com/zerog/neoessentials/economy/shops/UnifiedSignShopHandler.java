package com.zerog.neoessentials.economy.shops;

import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.utils.MessageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.container.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UNIFIED Shop Handler - ALL shop logic in ONE place
 * This replaces ALL other shop handlers to prevent conflicts and duplication bugs
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class UnifiedSignShopHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(UnifiedSignShopHandler.class);
    private final ShopManager shopManager;
    private final EconomyManager economyManager;
    
    public UnifiedSignShopHandler(ShopManager shopManager) {
        this.shopManager = shopManager;
        this.economyManager = EconomyManager.getInstance();
    }
    
    /**
     * Handle ALL shop transactions - both player and admin shops
     */
    public InteractionResult handleShopTransaction(ServerPlayer player, ShopManager.SignShop signShop, 
                                                  InteractionHand hand, String action) {
        
        LOGGER.info("UNIFIED SHOP: Player {} attempting {} transaction on shop at {}", 
                   player.getName().getString(), action, signShop.getSignPos());
        
        try {
            // Real-time stock validation BEFORE any transaction
            int realStock = validateAndGetRealStock(signShop);
            
            if (signShop.isAdminShop()) {
                return handleAdminShopTransaction(player, signShop, action);
            } else {
                return handlePlayerShopTransaction(player, signShop, action, realStock);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error handling shop transaction for player {} at shop {}", 
                        player.getName().getString(), signShop.getSignPos(), e);
            MessageUtil.sendMessage(player, "§cShop transaction failed. Please try again.");
            return InteractionResult.FAIL;
        }
    }
    
    /**
     * Validate stock by checking actual chest contents
     * This prevents the "items from thin air" bug
     */
    private int validateAndGetRealStock(ShopManager.SignShop signShop) {
        if (signShop.isAdminShop()) {
            return Integer.MAX_VALUE; // Admin shops have infinite stock
        }
        
        BlockPos chestPos = signShop.getChestPos();
        if (chestPos == null) {
            LOGGER.warn("Shop at {} has no chest position - marking as empty", signShop.getSignPos());
            return 0;
        }
        
        Level level = shopManager.getServerLevel();
        if (level == null) {
            LOGGER.warn("Cannot get server level - marking shop as empty");
            return 0;
        }
        
        BlockEntity blockEntity = level.getBlockEntity(chestPos);
        if (!(blockEntity instanceof ChestBlockEntity chestEntity)) {
            LOGGER.warn("Chest missing at {} for shop {} - marking as empty", chestPos, signShop.getSignPos());
            return 0;
        }
        
        // Count actual items in chest matching the shop item
        ItemStack shopItem = signShop.getItemStack();
        int actualStock = 0;
        
        for (int i = 0; i < chestEntity.getContainerSize(); i++) {
            ItemStack slotItem = chestEntity.getItem(i);
            if (ItemStack.isSameItemSameTags(slotItem, shopItem)) {
                actualStock += slotItem.getCount();
            }
        }
        
        // Update shop data with real stock to prevent desync
        if (actualStock != signShop.getStock()) {
            LOGGER.info("Stock mismatch detected! Shop claimed {} but chest has {} - fixing", 
                       signShop.getStock(), actualStock);
            shopManager.updateSignShopStock(signShop.getSignPos(), actualStock);
            signShop.setStock(actualStock);
        }
        
        return actualStock;
    }
    
    /**
     * Handle admin shop transactions (infinite stock/money)
     */
    private InteractionResult handleAdminShopTransaction(ServerPlayer player, ShopManager.SignShop signShop, String action) {
        ItemStack shopItem = signShop.getItemStack();
        
        if ("BUY".equals(action)) {
            // Player buying from admin shop
            double totalPrice = signShop.getBuyPrice();
            
            if (!economyManager.hasBalance(player.getUUID(), totalPrice)) {
                MessageUtil.sendMessage(player, "§cInsufficient funds! You need $" + totalPrice);
                return InteractionResult.FAIL;
            }
            
            // Take money and give item
            economyManager.withdrawMoney(player.getUUID(), totalPrice);
            player.getInventory().add(shopItem.copy());
            
            // Record transaction
            shopManager.recordShopTransaction(signShop, "BUY", totalPrice, 1);
            
            MessageUtil.sendMessage(player, "§aPurchased " + shopItem.getDisplayName().getString() + 
                                  " for $" + totalPrice + " from admin shop");
            
            // Update sign display
            updateSignDisplay(signShop);
            
            return InteractionResult.SUCCESS;
            
        } else if ("SELL".equals(action)) {
            // Player selling to admin shop
            if (signShop.getSellPrice() <= 0) {
                MessageUtil.sendMessage(player, "§cThis admin shop doesn't buy items");
                return InteractionResult.FAIL;
            }
            
            // Check if player has the item
            ItemStack playerItem = findMatchingItemInInventory(player, shopItem);
            if (playerItem.isEmpty()) {
                MessageUtil.sendMessage(player, "§cYou don't have " + shopItem.getDisplayName().getString() + " to sell");
                return InteractionResult.FAIL;
            }
            
            // Take item and give money
            playerItem.shrink(1);
            double totalEarnings = signShop.getSellPrice();
            economyManager.depositMoney(player.getUUID(), totalEarnings);
            
            // Record transaction
            shopManager.recordShopTransaction(signShop, "SELL", totalEarnings, 1);
            
            MessageUtil.sendMessage(player, "§aSold " + shopItem.getDisplayName().getString() + 
                                  " for $" + totalEarnings + " to admin shop");
            
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.FAIL;
    }
    
    /**
     * Handle player shop transactions (limited by chest contents)
     */
    private InteractionResult handlePlayerShopTransaction(ServerPlayer player, ShopManager.SignShop signShop, 
                                                         String action, int realStock) {
        
        String shopOwner = signShop.getOwnerId();
        ItemStack shopItem = signShop.getItemStack();
        
        if ("BUY".equals(action)) {
            // Player buying from player shop
            if (realStock <= 0) {
                MessageUtil.sendMessage(player, "§cThis shop is out of stock!");
                return InteractionResult.FAIL;
            }
            
            double totalPrice = signShop.getBuyPrice();
            
            if (!economyManager.hasBalance(player.getUUID(), totalPrice)) {
                MessageUtil.sendMessage(player, "§cInsufficient funds! You need $" + totalPrice);
                return InteractionResult.FAIL;
            }
            
            // Remove item from chest FIRST to prevent duplication
            if (!removeItemFromChest(signShop, shopItem, 1)) {
                MessageUtil.sendMessage(player, "§cCould not remove item from shop chest!");
                return InteractionResult.FAIL;
            }
            
            // Process payment
            economyManager.withdrawMoney(player.getUUID(), totalPrice);
            economyManager.depositMoney(shopOwner, totalPrice);
            
            // Give item to player
            player.getInventory().add(shopItem.copy());
            
            // Update stock
            int newStock = realStock - 1;
            shopManager.updateSignShopStock(signShop.getSignPos(), newStock);
            signShop.setStock(newStock);
            
            // Record transaction
            shopManager.recordShopTransaction(signShop, "BUY", totalPrice, 1);
            
            MessageUtil.sendMessage(player, "§aPurchased " + shopItem.getDisplayName().getString() + 
                                  " for $" + totalPrice + " from " + shopOwner);
            
            // Update sign display
            updateSignDisplay(signShop);
            
            return InteractionResult.SUCCESS;
            
        } else if ("SELL".equals(action)) {
            // Player selling to player shop
            if (signShop.getSellPrice() <= 0) {
                MessageUtil.sendMessage(player, "§cThis shop doesn't buy items");
                return InteractionResult.FAIL;
            }
            
            // Check if shop owner has money
            double totalEarnings = signShop.getSellPrice();
            if (!economyManager.hasBalance(shopOwner, totalEarnings)) {
                MessageUtil.sendMessage(player, "§cShop owner has insufficient funds!");
                return InteractionResult.FAIL;
            }
            
            // Check if player has the item
            ItemStack playerItem = findMatchingItemInInventory(player, shopItem);
            if (playerItem.isEmpty()) {
                MessageUtil.sendMessage(player, "§cYou don't have " + shopItem.getDisplayName().getString() + " to sell");
                return InteractionResult.FAIL;
            }
            
            // Add item to chest FIRST
            if (!addItemToChest(signShop, shopItem, 1)) {
                MessageUtil.sendMessage(player, "§cShop chest is full!");
                return InteractionResult.FAIL;
            }
            
            // Take item from player
            playerItem.shrink(1);
            
            // Process payment
            economyManager.withdrawMoney(shopOwner, totalEarnings);
            economyManager.depositMoney(player.getUUID(), totalEarnings);
            
            // Update stock
            int newStock = realStock + 1;
            shopManager.updateSignShopStock(signShop.getSignPos(), newStock);
            signShop.setStock(newStock);
            
            // Record transaction
            shopManager.recordShopTransaction(signShop, "SELL", totalEarnings, 1);
            
            MessageUtil.sendMessage(player, "§aSold " + shopItem.getDisplayName().getString() + 
                                  " for $" + totalEarnings + " to " + shopOwner);
            
            // Update sign display
            updateSignDisplay(signShop);
            
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.FAIL;
    }
    
    /**
     * Remove item from shop chest
     */
    private boolean removeItemFromChest(ShopManager.SignShop signShop, ItemStack itemToRemove, int quantity) {
        BlockPos chestPos = signShop.getChestPos();
        Level level = shopManager.getServerLevel();
        
        if (level == null || chestPos == null) return false;
        
        BlockEntity blockEntity = level.getBlockEntity(chestPos);
        if (!(blockEntity instanceof ChestBlockEntity chestEntity)) {
            return false;
        }
        
        int remaining = quantity;
        for (int i = 0; i < chestEntity.getContainerSize() && remaining > 0; i++) {
            ItemStack slotItem = chestEntity.getItem(i);
            if (ItemStack.isSameItemSameTags(slotItem, itemToRemove)) {
                int toRemove = Math.min(remaining, slotItem.getCount());
                slotItem.shrink(toRemove);
                remaining -= toRemove;
                chestEntity.setChanged();
            }
        }
        
        return remaining == 0;
    }
    
    /**
     * Add item to shop chest
     */
    private boolean addItemToChest(ShopManager.SignShop signShop, ItemStack itemToAdd, int quantity) {
        BlockPos chestPos = signShop.getChestPos();
        Level level = shopManager.getServerLevel();
        
        if (level == null || chestPos == null) return false;
        
        BlockEntity blockEntity = level.getBlockEntity(chestPos);
        if (!(blockEntity instanceof ChestBlockEntity chestEntity)) {
            return false;
        }
        
        ItemStack stackToAdd = itemToAdd.copy();
        stackToAdd.setCount(quantity);
        
        // Try to add to existing stacks first
        for (int i = 0; i < chestEntity.getContainerSize(); i++) {
            ItemStack slotItem = chestEntity.getItem(i);
            if (slotItem.isEmpty()) {
                chestEntity.setItem(i, stackToAdd);
                chestEntity.setChanged();
                return true;
            } else if (ItemStack.isSameItemSameTags(slotItem, stackToAdd)) {
                int maxStack = slotItem.getMaxStackSize();
                int canAdd = maxStack - slotItem.getCount();
                if (canAdd >= quantity) {
                    slotItem.grow(quantity);
                    chestEntity.setChanged();
                    return true;
                }
            }
        }
        
        return false; // Chest is full
    }
    
    /**
     * Find matching item in player inventory
     */
    private ItemStack findMatchingItemInInventory(ServerPlayer player, ItemStack targetItem) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(stack, targetItem)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * Update the visual sign display with current stock
     */
    private void updateSignDisplay(ShopManager.SignShop signShop) {
        Level level = shopManager.getServerLevel();
        if (level == null) return;
        
        String stockDisplay;
        if (signShop.isAdminShop()) {
            stockDisplay = "§6[∞]"; // Infinite symbol for admin shops
        } else {
            int stock = signShop.getStock();
            if (stock <= 0) {
                stockDisplay = "§c[EMPTY]";
            } else if (stock < 5) {
                stockDisplay = "§e[LOW]";
            } else {
                stockDisplay = "§a[STOCK]";
            }
        }
        
        // Update the sign with stock info
        shopManager.updateShopSign(signShop.getSignPos(), stockDisplay);
    }
    
    /**
     * Check if player can access this shop (for chest protection)
     */
    public boolean canAccessShop(ServerPlayer player, ShopManager.SignShop signShop) {
        String playerId = player.getStringUUID();
        String ownerId = signShop.getOwnerId();
        
        // Owner can always access
        if (playerId.equals(ownerId)) {
            return true;
        }
        
        // Check bypass permission
        if (player.hasPermissions(2) || // Op level 2+
            player.hasPermissions(4)) {  // Op level 4 (admin)
            return true;
        }
        
        // Check specific permissions
        // Note: This would need proper permission system integration
        // For now, using op level as fallback
        
        return false;
    }
    
    /**
     * Check if player can break this shop
     */
    public boolean canBreakShop(ServerPlayer player, ShopManager.SignShop signShop) {
        // Same logic as access for now
        return canAccessShop(player, signShop);
    }
}
