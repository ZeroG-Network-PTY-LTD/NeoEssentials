package com.zerog.neoessentials.economy.shops;

import com.zerog.neoessentials.permissions.PermissionNodes;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.ChatFormatting;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Handles sign shop interactions and creation
 */
public class SignShopHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SignShopHandler.class);
    private static final String SHOP_HEADER = "[SHOP]";
    
    private final ShopManager shopManager;
    
    public SignShopHandler(ShopManager shopManager) {
        this.shopManager = shopManager;
    }
    
    /**
     * Handle player interaction with a sign
     */
    public InteractionResult handleSignInteraction(Player player, Level level, BlockPos pos, InteractionHand hand) {
        if (level.isClientSide) return InteractionResult.PASS;
        
        BlockState blockState = level.getBlockState(pos);
        if (!(blockState.getBlock() instanceof SignBlock)) {
            return InteractionResult.PASS;
        }
        
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity signEntity)) {
            return InteractionResult.PASS;
        }
        
        // Check if this is a shop sign
        Component[] lines = signEntity.getFrontText().getMessages(false);
        if (lines.length == 0 || !lines[0].getString().equals(SHOP_HEADER)) {
            return InteractionResult.PASS;
        }
        
        // Find the corresponding shop
        Optional<ShopManager.SignShop> signShop = shopManager.getSignShops().stream()
                .filter(shop -> shop.getSignPos().equals(pos))
                .findFirst();
        
        if (signShop.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cThis shop sign is not properly configured!"));
            return InteractionResult.FAIL;
        }
        
        return handleShopTransaction(player, signShop.get(), player.isCrouching());
    }
    
    /**
     * Create a new sign shop
     */
    public boolean createSignShop(Player player, BlockPos signPos, ItemStack item, double buyPrice, double sellPrice, int quantity) {
        // Check permissions
        if (!hasPermission(player, PermissionNodes.SHOP_SIGN_CREATE)) {
            player.sendSystemMessage(Component.literal("§cYou don't have permission to create sign shops!"));
            return false;
        }
        
        // Validate prices
        if (buyPrice <= 0 && sellPrice <= 0) {
            player.sendSystemMessage(Component.literal("§cAt least one price (buy or sell) must be greater than 0!"));
            return false;
        }
        
        if (buyPrice > 0 && sellPrice > 0 && buyPrice <= sellPrice) {
            player.sendSystemMessage(Component.literal("§cBuy price must be higher than sell price!"));
            return false;
        }
        
        // Create the shop
        boolean success = shopManager.createSignShop(player, signPos, item, buyPrice, sellPrice, quantity);
        
        if (success) {
            updateSignText(player.level(), signPos, item, buyPrice, sellPrice, quantity);
            player.sendSystemMessage(Component.literal("§aSign shop created successfully!"));
            LOGGER.info("Player {} created a sign shop at {} for item {}", 
                       player.getName().getString(), signPos, item.getDisplayName().getString());
        } else {
            player.sendSystemMessage(Component.literal("§cFailed to create sign shop!"));
        }
        
        return success;
    }
    
    /**
     * Handle a transaction with a sign shop
     */
    private InteractionResult handleShopTransaction(Player player, ShopManager.SignShop signShop, boolean isSelling) {
        if (isSelling) {
            // Player wants to sell to the shop
            if (signShop.getSellPrice() <= 0) {
                player.sendSystemMessage(Component.literal("§cThis shop doesn't buy items!"));
                return InteractionResult.FAIL;
            }
            
            return handleSellTransaction(player, signShop);
        } else {
            // Player wants to buy from the shop
            if (signShop.getBuyPrice() <= 0) {
                player.sendSystemMessage(Component.literal("§cThis shop doesn't sell items!"));
                return InteractionResult.FAIL;
            }
            
            return handleBuyTransaction(player, signShop);
        }
    }
    
    /**
     * Handle buying items from a sign shop
     */
    private InteractionResult handleBuyTransaction(Player player, ShopManager.SignShop signShop) {
        // For player shops, check actual chest contents instead of just stock number
        if (!hasChestStock(player.level(), signShop)) {
            MessageUtil.sendTranslatedMessage((net.minecraft.server.level.ServerPlayer) player, 
                "neoessentials.shop.buy.shop_empty");
            return InteractionResult.FAIL;
        }
        
        double totalPrice = signShop.getBuyPrice() * signShop.getQuantity();
        
        // Get the economy manager and check if economy is enabled
        com.zerog.neoessentials.managers.EconomyManager economyManager = 
            com.zerog.neoessentials.managers.EconomyManager.getInstance();
        
        if (economyManager == null) {
            player.sendSystemMessage(Component.literal("§cEconomy system is not available!"));
            return InteractionResult.FAIL;
        }
        
        // Check if player has enough money
        if (!economyManager.hasBalance(player.getUUID(), totalPrice)) {
            double currentBalance = economyManager.getBalance(player.getUUID()).doubleValue();
            LOGGER.warn("Player {} has insufficient funds. Required: {}, Has: {}", 
                       player.getName().getString(), totalPrice, currentBalance);
            
            player.sendSystemMessage(Component.literal(String.format(
                    "§cInsufficient funds! You need %s but have %s",
                    economyManager.formatCurrency(totalPrice),
                    economyManager.formatCurrency(currentBalance)
            )));
            return InteractionResult.FAIL;
        }
        
        LOGGER.info("Player {} attempting to buy {}x {} for {} from shop owned by {}", 
                   player.getName().getString(), signShop.getQuantity(), 
                   signShop.getItem().getDisplayName().getString(),
                   economyManager.formatCurrency(totalPrice),
                   signShop.getOwnerId());
        
        // Player-to-player transaction: Remove money from buyer, give to shop owner
        LOGGER.info("Processing buy transaction: Player {} buying from shop owner {} for {}", 
            player.getName().getString(), signShop.getOwnerId(), economyManager.formatCurrency(totalPrice));
        
        // Check buyer's balance before transaction
        BigDecimal buyerBalanceBefore = economyManager.getBalance(player.getUUID());
        LOGGER.info("Buyer balance before: {}", economyManager.formatCurrency(buyerBalanceBefore));
        
        boolean withdrawSuccess = economyManager.withdrawBalance(
                player.getUUID(), 
                totalPrice,
                "Shop purchase: " + signShop.getQuantity() + "x " + signShop.getItem().getDisplayName().getString()
        );
        
        LOGGER.info("Withdraw success: {}", withdrawSuccess);
        
        if (!withdrawSuccess) {
            player.sendSystemMessage(Component.literal("§cFailed to process payment!"));
            return InteractionResult.FAIL;
        }
        
        // Check buyer's balance after withdrawal
        BigDecimal buyerBalanceAfter = economyManager.getBalance(player.getUUID());
        LOGGER.info("Buyer balance after withdrawal: {}", economyManager.formatCurrency(buyerBalanceAfter));

        // Give money to shop owner
        try {
            java.util.UUID shopOwnerUUID = java.util.UUID.fromString(signShop.getOwnerId());
            
            // Check shop owner's balance before transaction
            BigDecimal ownerBalanceBefore = economyManager.getBalance(shopOwnerUUID);
            LOGGER.info("Shop owner balance before: {}", economyManager.formatCurrency(ownerBalanceBefore));
            
            boolean depositSuccess = economyManager.depositBalance(
                    shopOwnerUUID,
                    totalPrice,
                    "Shop sale: " + signShop.getQuantity() + "x " + signShop.getItem().getDisplayName().getString() + " to " + player.getName().getString()
            );
            
            LOGGER.info("Deposit success: {}", depositSuccess);
            
            if (!depositSuccess) {
                // Refund the buyer if we can't pay the shop owner
                economyManager.depositBalance(player.getUUID(), totalPrice, "Refund: Shop payment failed");
                player.sendSystemMessage(Component.literal("§cShop payment failed! Money refunded."));
                return InteractionResult.FAIL;
            }
        } catch (IllegalArgumentException e) {
            // Invalid UUID - refund buyer
            economyManager.depositBalance(player.getUUID(), totalPrice, "Refund: Invalid shop owner");
            player.sendSystemMessage(Component.literal("§cInvalid shop owner! Money refunded."));
            return InteractionResult.FAIL;
        }
        
        // Transaction successful - take items from chest and give to player
        if (!removeItemsFromChest(player.level(), signShop)) {
            // If we can't remove items from chest, refund the money
            economyManager.depositBalance(player.getUUID(), totalPrice, "Refund for shop stock issue");
            MessageUtil.sendTranslatedMessage((net.minecraft.server.level.ServerPlayer) player,
                "neoessentials.shop.buy.shop_empty");
            return InteractionResult.FAIL;
        }
        
        // Give items to player
        ItemStack itemToGive = signShop.getItem().copy();
        itemToGive.setCount(signShop.getQuantity());
        
        if (!player.getInventory().add(itemToGive)) {
            player.spawnAtLocation(itemToGive);
        }
        
        // Reduce shop stock and save to storage
        int newStock = signShop.getStock() - signShop.getQuantity();
        shopManager.updateSignShopStock(signShop.getSignPos(), newStock);
        
        // Get shop owner name for better display
        String shopOwnerName = signShop.getOwnerId();
        try {
            net.minecraft.server.MinecraftServer server = player.getServer();
            if (server != null) {
                java.util.UUID ownerUUID = java.util.UUID.fromString(signShop.getOwnerId());
                net.minecraft.server.level.ServerPlayer ownerPlayer = server.getPlayerList().getPlayer(ownerUUID);
                if (ownerPlayer != null) {
                    shopOwnerName = ownerPlayer.getName().getString();
                }
            }
        } catch (Exception e) {
            // Keep UUID if name lookup fails
        }
        
        MessageUtil.sendTranslatedMessage((net.minecraft.server.level.ServerPlayer) player, "neoessentials.shop.buy.success",
                signShop.getQuantity(),
                signShop.getItem().getDisplayName().getString(),
                totalPrice,
                shopOwnerName
        );
        
        LOGGER.info("Player {} purchased {}x {} for {} from shop owned by {}", 
                   player.getName().getString(), signShop.getQuantity(), 
                   signShop.getItem().getDisplayName().getString(), 
                   economyManager.formatCurrency(totalPrice),
                   signShop.getOwnerId());
        
        return InteractionResult.SUCCESS;
    }
    
    /**
     * Handle selling items to a sign shop
     */
    private InteractionResult handleSellTransaction(Player player, ShopManager.SignShop signShop) {
        ItemStack shopItem = signShop.getItem();
        int quantityToSell = signShop.getQuantity();
        
        // Check if player has the required items
        int playerQuantity = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItem(stack, shopItem)) {
                playerQuantity += stack.getCount();
            }
        }
        
        if (playerQuantity < quantityToSell) {
            player.sendSystemMessage(Component.literal(String.format(
                    "§cYou need %dx %s to sell to this shop!",
                    quantityToSell,
                    shopItem.getDisplayName().getString()
            )));
            return InteractionResult.FAIL;
        }
        
        // Check if chest has space for the items
        if (!chestHasSpace(player.level(), signShop)) {
            MessageUtil.sendTranslatedMessage((net.minecraft.server.level.ServerPlayer) player,
                "neoessentials.shop.sell.chest_full");
            return InteractionResult.FAIL;
        }

        // Remove items from player inventory
        int toRemove = quantityToSell;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItem(stack, shopItem) && toRemove > 0) {
                int removeFromStack = Math.min(stack.getCount(), toRemove);
                stack.shrink(removeFromStack);
                toRemove -= removeFromStack;
            }
        }

        // Add items to the shop's connected chest
        if (!addItemsToChest(player.level(), signShop)) {
            // If we can't add items to chest, give items back to player
            ItemStack itemToReturn = shopItem.copy();
            itemToReturn.setCount(quantityToSell);
            if (!player.getInventory().add(itemToReturn)) {
                player.spawnAtLocation(itemToReturn);
            }
            MessageUtil.sendTranslatedMessage((net.minecraft.server.level.ServerPlayer) player,
                "neoessentials.shop.sell.chest_full");
            return InteractionResult.FAIL;
        }        // Player-to-player transaction: Shop owner pays seller for items
        double earnings = signShop.getSellPrice() * quantityToSell;
        
        // Get the economy manager
        com.zerog.neoessentials.managers.EconomyManager economyManager = 
            com.zerog.neoessentials.managers.EconomyManager.getInstance();
        
        if (economyManager != null && economyManager.isEnabled()) {
            try {
                java.util.UUID shopOwnerUUID = java.util.UUID.fromString(signShop.getOwnerId());
                
                // Check if shop owner has enough money to buy the items
                if (!economyManager.hasBalance(shopOwnerUUID, earnings)) {
                    // Return items to player since shop owner can't afford them
                    ItemStack itemToReturn = shopItem.copy();
                    itemToReturn.setCount(quantityToSell);
                    if (!player.getInventory().add(itemToReturn)) {
                        player.spawnAtLocation(itemToReturn);
                    }
                    player.sendSystemMessage(Component.literal("§cShop owner doesn't have enough money! Items returned."));
                    return InteractionResult.FAIL;
                }
                
                // Remove money from shop owner
                boolean withdrawSuccess = economyManager.withdrawBalance(
                    shopOwnerUUID,
                    earnings,
                    "Shop purchase: " + quantityToSell + "x " + shopItem.getDisplayName().getString() + " from " + player.getName().getString()
                );
                
                if (!withdrawSuccess) {
                    // Return items to player since payment failed
                    ItemStack itemToReturn = shopItem.copy();
                    itemToReturn.setCount(quantityToSell);
                    if (!player.getInventory().add(itemToReturn)) {
                        player.spawnAtLocation(itemToReturn);
                    }
                    player.sendSystemMessage(Component.literal("§cPayment failed! Items returned."));
                    return InteractionResult.FAIL;
                }
                
                // Give money to seller
                boolean depositSuccess = economyManager.depositBalance(
                    player.getUUID(), 
                    earnings,
                    "Shop sale: " + quantityToSell + "x " + shopItem.getDisplayName().getString() + " to " + signShop.getOwnerId() + "'s shop"
                );
                
                if (!depositSuccess) {
                    // Refund the shop owner since we couldn't pay the seller
                    economyManager.depositBalance(shopOwnerUUID, earnings, "Refund for failed shop sale");
                    // Return items to player
                    ItemStack itemToReturn = shopItem.copy();
                    itemToReturn.setCount(quantityToSell);
                    if (!player.getInventory().add(itemToReturn)) {
                        player.spawnAtLocation(itemToReturn);
                    }
                    player.sendSystemMessage(Component.literal("§cPayment failed! Items and money returned."));
                    return InteractionResult.FAIL;
                }
                
                // Transaction successful
                String shopOwnerName = "Unknown";
                try {
                    net.minecraft.server.MinecraftServer server = player.getServer();
                    if (server != null) {
                        net.minecraft.server.level.ServerPlayer shopOwnerPlayer = server.getPlayerList().getPlayer(shopOwnerUUID);
                        if (shopOwnerPlayer != null) {
                            shopOwnerName = shopOwnerPlayer.getName().getString();
                        }
                    }
                } catch (Exception e) {
                    // Keep default name if we can't get the actual name
                }
            
            if (depositSuccess) {
                player.sendSystemMessage(Component.literal("§aSold " + quantityToSell + "x " + shopItem.getDisplayName().getString() + 
                    " to " + shopOwnerName + "'s shop for $" + String.format("%.2f", earnings)));
                
                LOGGER.info("Player {} sold {}x {} for {} to {} shop at {}", 
                           player.getName().getString(), quantityToSell, 
                           shopItem.getDisplayName().getString(), 
                           String.format("%.2f", earnings),
                           shopOwnerName,
                           signShop.getSignPos());
                
                return InteractionResult.SUCCESS;
            } else {
                // This should not happen since we already checked depositSuccess above
                // But included for safety
                player.sendSystemMessage(Component.literal("§cUnexpected transaction error!"));
                return InteractionResult.FAIL;
            }
            
            } catch (Exception e) {
                // Return items to player on any error
                ItemStack itemToReturn = shopItem.copy();
                itemToReturn.setCount(quantityToSell);
                if (!player.getInventory().add(itemToReturn)) {
                    player.spawnAtLocation(itemToReturn);
                }
                player.sendSystemMessage(Component.literal("§cTransaction failed! Items returned."));
                LOGGER.error("Error in sell transaction", e);
                return InteractionResult.FAIL;
            }
        } else {
            player.sendSystemMessage(Component.literal("§cEconomy system is not available!"));
            
            // Give items back since economy is disabled
            ItemStack itemToReturn = shopItem.copy();
            itemToReturn.setCount(quantityToSell);
            if (!player.getInventory().add(itemToReturn)) {
                player.spawnAtLocation(itemToReturn);
            }
            
            return InteractionResult.FAIL;
        }
    }
    
    /**
     * Update the text on a sign shop
     */
    private void updateSignText(Level level, BlockPos pos, ItemStack item, double buyPrice, double sellPrice, int quantity) {
        if (level.getBlockEntity(pos) instanceof SignBlockEntity signEntity) {
            Component[] newLines = new Component[4];
            
            newLines[0] = Component.literal(SHOP_HEADER).setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_BLUE));
            newLines[1] = Component.literal(quantity + "x " + getShortItemName(item)).setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));
            
            if (buyPrice > 0 && sellPrice > 0) {
                newLines[2] = Component.literal("Buy: $" + String.format("%.2f", buyPrice)).setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
                newLines[3] = Component.literal("Sell: $" + String.format("%.2f", sellPrice)).setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
            } else if (buyPrice > 0) {
                newLines[2] = Component.literal("Buy: $" + String.format("%.2f", buyPrice)).setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
                newLines[3] = Component.literal("(Buy Only)").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            } else {
                newLines[2] = Component.literal("Sell: $" + String.format("%.2f", sellPrice)).setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
                newLines[3] = Component.literal("(Sell Only)").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
            }
            
            // Update sign text using the correct method  
            signEntity.updateText((frontText) -> {
                return frontText.setMessage(0, newLines[0])
                               .setMessage(1, newLines[1])
                               .setMessage(2, newLines[2])
                               .setMessage(3, newLines[3]);
            }, true);
            signEntity.setChanged();
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
        }
    }
    
    /**
     * Get a shortened item name for display on signs
     */
    private String getShortItemName(ItemStack item) {
        String fullName = item.getDisplayName().getString();
        return fullName.length() > 12 ? fullName.substring(0, 12) : fullName;
    }
    
    /**
     * Check if the shop's chest has enough items for a purchase
     */
    private boolean hasChestStock(Level level, ShopManager.SignShop signShop) {
        BlockPos chestPos = signShop.getChestPos();
        if (chestPos == null) {
            return false; // No chest connected
        }
        
        if (level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chestEntity) {
            ItemStack shopItem = signShop.getItem();
            int requiredQuantity = signShop.getQuantity();
            int availableCount = 0;
            
            // Count matching items in chest
            for (int i = 0; i < chestEntity.getContainerSize(); i++) {
                ItemStack stack = chestEntity.getItem(i);
                if (ItemStack.isSameItem(stack, shopItem)) {
                    availableCount += stack.getCount();
                }
            }
            
            return availableCount >= requiredQuantity;
        }
        
        return false; // Chest not found or not a chest
    }
    
    /**
     * Remove items from the shop's chest for a purchase
     */
    private boolean removeItemsFromChest(Level level, ShopManager.SignShop signShop) {
        BlockPos chestPos = signShop.getChestPos();
        if (chestPos == null) {
            return false; // No chest connected
        }
        
        if (level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chestEntity) {
            ItemStack shopItem = signShop.getItem();
            int requiredQuantity = signShop.getQuantity();
            int toRemove = requiredQuantity;
            
            // Remove items from chest
            for (int i = 0; i < chestEntity.getContainerSize() && toRemove > 0; i++) {
                ItemStack stack = chestEntity.getItem(i);
                if (ItemStack.isSameItem(stack, shopItem)) {
                    int removeFromStack = Math.min(stack.getCount(), toRemove);
                    stack.shrink(removeFromStack);
                    toRemove -= removeFromStack;
                }
            }
            
            // Mark chest as changed
            chestEntity.setChanged();
            return toRemove == 0; // Success if we removed all required items
        }

        return false; // Chest not found or not a chest
    }
    
    /**
     * Check if the shop's chest has space for items being sold
     */
    private boolean chestHasSpace(Level level, ShopManager.SignShop signShop) {
        BlockPos chestPos = signShop.getChestPos();
        if (chestPos == null) {
            return false; // No chest connected
        }
        
        if (level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chestEntity) {
            ItemStack shopItem = signShop.getItem();
            int quantityToAdd = signShop.getQuantity();
            int remainingToAdd = quantityToAdd;
            
            // Check if chest has space for the items
            for (int i = 0; i < chestEntity.getContainerSize() && remainingToAdd > 0; i++) {
                ItemStack stack = chestEntity.getItem(i);
                if (stack.isEmpty()) {
                    // Empty slot - can fit full stack
                    remainingToAdd -= Math.min(remainingToAdd, shopItem.getMaxStackSize());
                } else if (ItemStack.isSameItem(stack, shopItem)) {
                    // Same item - check how many more can fit
                    int spaceInStack = shopItem.getMaxStackSize() - stack.getCount();
                    remainingToAdd -= Math.min(remainingToAdd, spaceInStack);
                }
            }
            
            return remainingToAdd <= 0; // Success if all items can fit
        }
        
        return false; // Chest not found or not a chest
    }
    
    /**
     * Add items to the shop's chest when a player sells
     */
    private boolean addItemsToChest(Level level, ShopManager.SignShop signShop) {
        BlockPos chestPos = signShop.getChestPos();
        if (chestPos == null) {
            return false; // No chest connected
        }
        
        if (level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chestEntity) {
            ItemStack shopItem = signShop.getItem();
            int quantityToAdd = signShop.getQuantity();
            int remainingToAdd = quantityToAdd;
            
            // Add items to chest
            for (int i = 0; i < chestEntity.getContainerSize() && remainingToAdd > 0; i++) {
                ItemStack stack = chestEntity.getItem(i);
                if (stack.isEmpty()) {
                    // Empty slot - add items
                    int toAdd = Math.min(remainingToAdd, shopItem.getMaxStackSize());
                    ItemStack newStack = shopItem.copy();
                    newStack.setCount(toAdd);
                    chestEntity.setItem(i, newStack);
                    remainingToAdd -= toAdd;
                } else if (ItemStack.isSameItem(stack, shopItem)) {
                    // Same item - add to existing stack
                    int spaceInStack = shopItem.getMaxStackSize() - stack.getCount();
                    int toAdd = Math.min(remainingToAdd, spaceInStack);
                    stack.grow(toAdd);
                    remainingToAdd -= toAdd;
                }
            }
            
            // Mark chest as changed
            chestEntity.setChanged();
            return remainingToAdd == 0; // Success if we added all items
        }

        return false; // Chest not found or not a chest
    }    /**
     * Check if player has permission
     */
    private boolean hasPermission(Player player, String permission) {
        // TODO: Implement proper permission checking
        return true; // For now, allow all players
    }
}
