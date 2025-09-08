package com.zerog.neoessentials.economy.shops;

import com.zerog.neoessentials.permissions.PermissionNodes;
import com.zerog.neoessentials.util.PermissionUtil;
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
     * Handle player interaction with a sign with enhanced error handling
     */
    public InteractionResult handleSignInteraction(Player player, Level level, BlockPos pos, InteractionHand hand) {
        if (level.isClientSide) return InteractionResult.PASS;
        
        try {
            BlockState blockState = level.getBlockState(pos);
            if (!(blockState.getBlock() instanceof SignBlock)) {
                return InteractionResult.PASS;
            }
            
            if (!(level.getBlockEntity(pos) instanceof SignBlockEntity signEntity)) {
                player.sendSystemMessage(Component.literal("§cError: Sign data could not be loaded!"));
                return InteractionResult.FAIL;
            }
            
            Component[] lines = signEntity.getFrontText().getMessages(false);
            if (lines.length == 0 || (!lines[0].getString().equals(SHOP_HEADER) && !lines[0].getString().equals("[ADMIN SHOP]"))) {
                return InteractionResult.PASS;
            }
            
            // NOTE: Removed permission check for shop usage - all players should be able to use shops
            // Only shop creation requires permissions, not shop usage
            
            // Enhanced shop retrieval with error recovery
            ShopManager.SignShop signShop = shopManager.getSignShop(pos);
            if (signShop == null) {
                player.sendSystemMessage(Component.literal("§c✗ Shop Error!")
                    .append(Component.literal("\n§7This shop sign is not properly configured."))
                    .append(Component.literal("\n§7Please contact an administrator.")));
                LOGGER.warn("Sign shop at {} not found in registry for player {}", pos, player.getName().getString());
                return InteractionResult.FAIL;
            }
            
            return handleShopTransaction(player, signShop, player.isCrouching());
            
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§c✗ Unexpected Error!")
                .append(Component.literal("\n§7An error occurred while accessing the shop."))
                .append(Component.literal("\n§7Please try again or contact an administrator.")));
            LOGGER.error("Unexpected error in sign shop interaction for player {} at {}", 
                        player.getName().getString(), pos, e);
            return InteractionResult.FAIL;
        }
    }
    
    /**
     * Create a new sign shop with enhanced validation and user guidance
     */
    public boolean createSignShop(Player player, BlockPos signPos, ItemStack item, double buyPrice, double sellPrice, int quantity) {
        try {
            if (!PermissionUtil.hasPermission((net.minecraft.server.level.ServerPlayer) player, PermissionNodes.SHOP_SIGN_CREATE)) {
                player.sendSystemMessage(Component.literal("§c✗ Permission Denied!")
                    .append(Component.literal("\n§7You don't have permission to create sign shops."))
                    .append(Component.literal("\n§7Contact an administrator for access.")));
                return false;
            }
            
            // Enhanced price validation with specific guidance
            if (buyPrice <= 0 && sellPrice <= 0) {
                player.sendSystemMessage(Component.literal("§c✗ Invalid Pricing!")
                    .append(Component.literal("\n§7At least one price (buy or sell) must be greater than 0."))
                    .append(Component.literal("\n§7Example: /signshop create diamond 1 10.0 5.0")));
                return false;
            }
            
            if (buyPrice > 0 && sellPrice > 0 && buyPrice <= sellPrice) {
                player.sendSystemMessage(Component.literal("§c✗ Invalid Price Logic!")
                    .append(Component.literal("\n§7Buy price must be higher than sell price."))
                    .append(Component.literal("\n§7Current: Buy $" + formatPrice(buyPrice) + ", Sell $" + formatPrice(sellPrice)))
                    .append(Component.literal("\n§7Suggestion: Increase buy price or decrease sell price.")));
                return false;
            }
            
            // Enhanced quantity validation
            if (quantity <= 0 || quantity > 64) {
                player.sendSystemMessage(Component.literal("§c✗ Invalid Quantity!")
                    .append(Component.literal("\n§7Quantity must be between 1 and 64."))
                    .append(Component.literal("\n§7You specified: " + quantity)));
                return false;
            }
            
            LOGGER.info("Creating sign shop for player {} at {} with item {}, buyPrice: {}, sellPrice: {}, quantity: {}", 
                       player.getName().getString(), signPos, item.getDisplayName().getString(), buyPrice, sellPrice, quantity);
            
            // Create the shop
            boolean success = shopManager.createSignShop(player, signPos, item, buyPrice, sellPrice, quantity);
            
            if (success) {
                try {
                    updateSignText(player.level(), signPos, item, buyPrice, sellPrice, quantity);
                    player.sendSystemMessage(Component.literal("§a✓ Shop Created Successfully!")
                        .append(Component.literal("\n§7├ Item: §f" + quantity + "x " + item.getDisplayName().getString()))
                        .append(Component.literal("\n§7├ Buy Price: §a$" + (buyPrice > 0 ? formatPrice(buyPrice) : "N/A")))
                        .append(Component.literal("\n§7├ Sell Price: §c$" + (sellPrice > 0 ? formatPrice(sellPrice) : "N/A")))
                        .append(Component.literal("\n§7└ Location: §f" + signPos.toShortString())));
                    LOGGER.info("Player {} successfully created a sign shop at {} for item {}", 
                               player.getName().getString(), signPos, item.getDisplayName().getString());
                } catch (Exception e) {
                    LOGGER.error("Error updating sign text after creating shop", e);
                    player.sendSystemMessage(Component.literal("§a✓ Shop Created!")
                        .append(Component.literal("\n§7Shop created successfully, but sign display may need refresh.")));
                }
            } else {
                player.sendSystemMessage(Component.literal("§c✗ Shop Creation Failed!")
                    .append(Component.literal("\n§7Common causes:"))
                    .append(Component.literal("\n§7• No chest found within 3 blocks"))
                    .append(Component.literal("\n§7• Shop already exists at this location"))
                    .append(Component.literal("\n§7• Insufficient permissions")));
                LOGGER.warn("Failed to create sign shop for player {} at {}", player.getName().getString(), signPos);
            }
            
            try {
                shopManager.saveShopsToStorage();
            } catch (Exception e) {
                LOGGER.error("Error saving shops to storage", e);
                if (success) {
                    player.sendSystemMessage(Component.literal("§6⚠ Warning: Shop created but save failed. Contact an administrator."));
                }
            }
            
            return success;
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§c✗ Unexpected Error!")
                .append(Component.literal("\n§7Failed to create shop due to an internal error."))
                .append(Component.literal("\n§7Please try again or contact an administrator.")));
            LOGGER.error("Unexpected error creating sign shop for player {} at {}", 
                        player.getName().getString(), signPos, e);
            return false;
        }
    }
    
    /**
     * Create a new sign shop with admin shop option
     */
    public boolean createSignShop(Player player, BlockPos signPos, ItemStack item, double buyPrice, double sellPrice, int quantity, boolean isAdminShop) {
        // Check permissions
        String requiredPermission = isAdminShop ? PermissionNodes.SHOP_ADMIN : PermissionNodes.SHOP_SIGN_CREATE;
        if (!PermissionUtil.hasPermission((net.minecraft.server.level.ServerPlayer) player, requiredPermission)) {
            String shopType = isAdminShop ? "admin shops" : "sign shops";
            player.sendSystemMessage(Component.literal("§cYou don't have permission to create " + shopType + "!"));
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
        
        // Validate quantity
        if (quantity <= 0 || quantity > 64) {
            player.sendSystemMessage(Component.literal("§cQuantity must be between 1 and 64!"));
            return false;
        }
        
        String shopType = isAdminShop ? "admin shop" : "sign shop";
        LOGGER.info("Creating {} for player {} at {} with item {}, buyPrice: {}, sellPrice: {}, quantity: {}", 
                   shopType, player.getName().getString(), signPos, item.getDisplayName().getString(), buyPrice, sellPrice, quantity);
        
        // Create the shop with admin status
        boolean success;
        if (isAdminShop) {
            success = shopManager.createSignShop(player, signPos, item, buyPrice, sellPrice, quantity, true);
        } else {
            success = shopManager.createSignShop(player, signPos, item, buyPrice, sellPrice, quantity);
        }
        
        if (success) {
            try {
                updateSignText(player.level(), signPos, item, buyPrice, sellPrice, quantity, isAdminShop);
                player.sendSystemMessage(Component.literal("§a" + Character.toUpperCase(shopType.charAt(0)) + shopType.substring(1) + " created successfully!"));
                LOGGER.info("Player {} successfully created a {} at {} for item {}", 
                           player.getName().getString(), shopType, signPos, item.getDisplayName().getString());
            } catch (Exception e) {
                LOGGER.error("Error updating sign text after creating " + shopType, e);
                player.sendSystemMessage(Component.literal("§a" + Character.toUpperCase(shopType.charAt(0)) + shopType.substring(1) + " created, but there was an error updating the sign text."));
            }
        } else {
            player.sendSystemMessage(Component.literal("§cFailed to create " + shopType + "! Check that there's a chest nearby and no existing shop at this location."));
            LOGGER.warn("Failed to create {} for player {} at {}", shopType, player.getName().getString(), signPos);
        }
        
        try {
            shopManager.saveShopsToStorage();
        } catch (Exception e) {
            LOGGER.error("Error saving shops to storage", e);
        }
        
        return success;
    }
    
    /**
     * Handle a transaction with a sign shop with enhanced validation
     */
    private InteractionResult handleShopTransaction(Player player, ShopManager.SignShop signShop, boolean isSelling) {
        try {
            if (isSelling) {
                // Player wants to sell to the shop
                if (signShop.getSellPrice() <= 0) {
                    player.sendSystemMessage(Component.literal("§c✗ Transaction Unavailable!")
                        .append(Component.literal("\n§7This shop doesn't purchase items."))
                        .append(Component.literal("\n§7Try left-clicking to buy instead.")));
                    return InteractionResult.FAIL;
                }
                
                return handleSellTransaction(player, signShop);
            } else {
                // Player wants to buy from the shop
                if (signShop.getBuyPrice() <= 0) {
                    player.sendSystemMessage(Component.literal("§c✗ Transaction Unavailable!")
                        .append(Component.literal("\n§7This shop doesn't sell items."))
                        .append(Component.literal("\n§7Try shift+left-clicking to sell instead.")));
                    return InteractionResult.FAIL;
                }
                
                return handleBuyTransaction(player, signShop);
            }
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§c✗ Transaction Failed!")
                .append(Component.literal("\n§7An error occurred during the transaction."))
                .append(Component.literal("\n§7Your items and money are safe.")));
            LOGGER.error("Error in shop transaction for player {} with shop at {}", 
                        player.getName().getString(), signShop.getSignPos(), e);
            return InteractionResult.FAIL;
        }
    }
    
    /**
     * Handle buying items from a sign shop
     */
    private InteractionResult handleBuyTransaction(Player player, ShopManager.SignShop signShop) {
        // For player shops, check actual chest contents instead of just stock number
        if (!hasChestStock(player.level(), signShop)) {
            player.sendSystemMessage(Component.literal("§cShop is out of stock!"));
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
        
        LOGGER.info("Processing buy transaction: Player {} buying from shop owner {} for {}", 
            player.getName().getString(), signShop.getOwnerId(), economyManager.formatCurrency(totalPrice));
        
        // Check buyer's balance before transaction
        BigDecimal buyerBalanceBefore = economyManager.getBalance(player.getUUID());
        LOGGER.info("Buyer balance before: {}", economyManager.formatCurrency(buyerBalanceBefore));

        // CRITICAL: Remove items from chest FIRST before any money transactions
        if (!removeItemsFromChest(player.level(), signShop)) {
            // If we can't remove items from chest, don't proceed with transaction
            player.sendSystemMessage(Component.literal("§cShop is out of stock!"));
            return InteractionResult.FAIL;
        }

        // Now that we have the items, proceed with money transactions
        boolean withdrawSuccess = economyManager.withdrawBalance(
                player.getUUID(), 
                totalPrice,
                "Shop purchase: " + signShop.getQuantity() + "x " + signShop.getItem().getDisplayName().getString()
        );
        
        LOGGER.info("Withdraw success: {}", withdrawSuccess);
        
        if (!withdrawSuccess) {
            // Money transaction failed - put items back in chest
            addItemsToChest(player.level(), signShop);
            player.sendSystemMessage(Component.literal("§cFailed to process payment!"));
            return InteractionResult.FAIL;
        }
        
        // Check buyer's balance after withdrawal
        BigDecimal buyerBalanceAfter = economyManager.getBalance(player.getUUID());
        LOGGER.info("Buyer balance after withdrawal: {}", economyManager.formatCurrency(buyerBalanceAfter));

        // Give money to shop owner (skip for admin shops as they have unlimited funds)
        if (!"SERVER".equals(signShop.getOwnerId())) {
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
                    // Refund the buyer and put items back if we can't pay the shop owner
                    economyManager.depositBalance(player.getUUID(), totalPrice, "Refund: Shop payment failed");
                    addItemsToChest(player.level(), signShop);
                    player.sendSystemMessage(Component.literal("§cShop payment failed! Money refunded."));
                    return InteractionResult.FAIL;
                }
            } catch (IllegalArgumentException e) {
                // Invalid UUID - refund buyer and put items back
                economyManager.depositBalance(player.getUUID(), totalPrice, "Refund: Invalid shop owner");
                addItemsToChest(player.level(), signShop);
                player.sendSystemMessage(Component.literal("§cInvalid shop owner! Money refunded."));
                return InteractionResult.FAIL;
            }
        } else {
            // Admin shop - no money transfer needed, admin shops have unlimited funds
            LOGGER.info("Admin shop transaction - no money transfer to server account");
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
        
        // Enhanced success message with detailed feedback
        player.sendSystemMessage(Component.literal("§a✓ Purchase Successful!")
            .append(Component.literal("\n§7├ Item: §f" + signShop.getQuantity() + "x " + signShop.getItem().getDisplayName().getString()))
            .append(Component.literal("\n§7├ Cost: §e$" + String.format("%.2f", totalPrice)))
            .append(Component.literal("\n§7├ Shop Owner: §b" + shopOwnerName))
            .append(Component.literal("\n§7└ New Balance: §a$" + economyManager.formatCurrency(economyManager.getBalance(player.getUUID()))))
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
            player.sendSystemMessage(Component.literal("§cChest is full! Cannot sell items to this shop."));
            return InteractionResult.FAIL;
        }

        // Player-to-player transaction: Shop owner pays seller for items
        double earnings = signShop.getSellPrice() * quantityToSell;
        
        // Get the economy manager
        com.zerog.neoessentials.managers.EconomyManager economyManager = 
            com.zerog.neoessentials.managers.EconomyManager.getInstance();
        
        if (economyManager != null && economyManager.isEnabled()) {
            // Admin shops have unlimited money, skip balance checks for them
            if ("SERVER".equals(signShop.getOwnerId())) {
                LOGGER.info("Admin shop sell transaction - unlimited funds available");
                
                // ALL CHECKS PASSED - Now execute the transaction atomically
                
                // 1. Remove items from player inventory
                int toRemove = quantityToSell;
                for (ItemStack stack : player.getInventory().items) {
                    if (ItemStack.isSameItem(stack, shopItem) && toRemove > 0) {
                        int removeFromStack = Math.min(stack.getCount(), toRemove);
                        stack.shrink(removeFromStack);
                        toRemove -= removeFromStack;
                    }
                }

                // 2. Add items to the shop's connected chest (skip for admin shops if no chest)
                if (signShop.getChestPos() != null) {
                    if (!addItemsToChest(player.level(), signShop)) {
                        // If we can't add items to chest, give items back to player
                        ItemStack itemToReturn = shopItem.copy();
                        itemToReturn.setCount(quantityToSell);
                        if (!player.getInventory().add(itemToReturn)) {
                            player.spawnAtLocation(itemToReturn);
                        }
                        player.sendSystemMessage(Component.literal("§cChest is full! Items returned."));
                        return InteractionResult.FAIL;
                    }
                }

                // 3. Give money to seller (admin shops pay from unlimited funds)
                boolean depositSuccess = economyManager.depositBalance(
                    player.getUUID(), 
                    earnings,
                    "Admin shop sale: " + quantityToSell + "x " + shopItem.getDisplayName().getString()
                );
                
                if (!depositSuccess) {
                    // Return items to player since payment failed
                    ItemStack itemToReturn = shopItem.copy();
                    itemToReturn.setCount(quantityToSell);
                    if (!player.getInventory().add(itemToReturn)) {
                        player.spawnAtLocation(itemToReturn);
                    }
                    player.sendSystemMessage(Component.literal("§cPayment failed! Items returned."));
                    return InteractionResult.FAIL;
                }
                
                // Enhanced success message for admin shop sales
                player.sendSystemMessage(Component.literal("§a✓ Sale to Admin Shop Successful!")
                    .append(Component.literal("\n§7├ Sold: §f" + quantityToSell + "x " + shopItem.getDisplayName().getString()))
                    .append(Component.literal("\n§7├ Earned: §a$" + economyManager.formatCurrency(earnings)))
                    .append(Component.literal("\n§7├ Shop Type: §6Admin Shop"))
                    .append(Component.literal("\n§7└ New Balance: §a$" + economyManager.formatCurrency(economyManager.getBalance(player.getUUID()))))
                );
                
                return InteractionResult.SUCCESS;
            } else {
                // Regular player shop - check shop owner balance
                try {
                    java.util.UUID shopOwnerUUID = java.util.UUID.fromString(signShop.getOwnerId());
                    
                    // Check if shop owner has enough money to buy the items BEFORE taking items
                    if (!economyManager.hasBalance(shopOwnerUUID, earnings)) {
                        player.sendSystemMessage(Component.literal("§cShop owner doesn't have enough money to buy your items!"));
                        return InteractionResult.FAIL;
                    }
                
                // ALL CHECKS PASSED - Now execute the transaction atomically
                
                // 1. Remove items from player inventory
                int toRemove = quantityToSell;
                for (ItemStack stack : player.getInventory().items) {
                    if (ItemStack.isSameItem(stack, shopItem) && toRemove > 0) {
                        int removeFromStack = Math.min(stack.getCount(), toRemove);
                        stack.shrink(removeFromStack);
                        toRemove -= removeFromStack;
                    }
                }

                // 2. Add items to the shop's connected chest
                if (!addItemsToChest(player.level(), signShop)) {
                    // If we can't add items to chest, give items back to player
                    ItemStack itemToReturn = shopItem.copy();
                    itemToReturn.setCount(quantityToSell);
                    if (!player.getInventory().add(itemToReturn)) {
                        player.spawnAtLocation(itemToReturn);
                    }
                    player.sendSystemMessage(Component.literal("§cChest is full! Items returned."));
                    return InteractionResult.FAIL;
                }

                // 3. Remove money from shop owner
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
                // Enhanced success message for player shop sales
                player.sendSystemMessage(Component.literal("§a✓ Sale to Player Shop Successful!")
                    .append(Component.literal("\n§7├ Sold: §f" + quantityToSell + "x " + shopItem.getDisplayName().getString()))
                    .append(Component.literal("\n§7├ Earned: §a$" + String.format("%.2f", earnings)))
                    .append(Component.literal("\n§7├ Shop Owner: §b" + shopOwnerName))
                    .append(Component.literal("\n§7└ New Balance: §a$" + economyManager.formatCurrency(economyManager.getBalance(player.getUUID()))))
                );
                
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
            }                } catch (Exception e) {
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
            } // End of player shop handling
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
     * Update the text on a sign shop with enhanced visual feedback
     */
    private void updateSignText(Level level, BlockPos pos, ItemStack item, double buyPrice, double sellPrice, int quantity) {
        if (level.getBlockEntity(pos) instanceof SignBlockEntity signEntity) {
            Component[] newLines = new Component[4];
            
            // Enhanced header with better styling
            newLines[0] = Component.literal("[SHOP]").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_BLUE).withBold(true));
            
            // Enhanced item display with quantity and smart abbreviation
            String itemDisplay = quantity + "x " + getShortItemName(item);
            newLines[1] = Component.literal(itemDisplay).setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));
            
            // Enhanced pricing display with better formatting
            if (buyPrice > 0 && sellPrice > 0) {
                newLines[2] = Component.literal("Buy: $" + formatPrice(buyPrice)).setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
                newLines[3] = Component.literal("Sell: $" + formatPrice(sellPrice)).setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
            } else if (buyPrice > 0) {
                newLines[2] = Component.literal("Buy: $" + formatPrice(buyPrice)).setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
                newLines[3] = Component.literal("(Buy Only)").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true));
            } else {
                newLines[2] = Component.literal("Sell: $" + formatPrice(sellPrice)).setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
                newLines[3] = Component.literal("(Sell Only)").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true));
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
     * Format price for display with smart decimal handling
     */
    private String formatPrice(double price) {
        if (price == (long) price) {
            return String.format("%.0f", price);
        } else {
            return String.format("%.2f", price);
        }
    }
    
    /**
     * Get a shortened item name for display on signs with smart abbreviation
     */
    private String getShortItemName(ItemStack item) {
        String fullName = item.getDisplayName().getString();
        
        // Enhanced name shortening with intelligent abbreviations
        if (fullName.length() <= 12) {
            return fullName;
        }
        
        // Smart abbreviations for common items
        String shortName = fullName
            .replace("Minecraft:", "")
            .replace("Block of ", "")
            .replace("Cooked ", "C.")
            .replace("Diamond ", "Dia.")
            .replace("Iron ", "Fe.")
            .replace("Gold ", "Au.")
            .replace("Emerald ", "Em.")
            .replace("Netherite ", "Neth.")
            .replace("Enchanted ", "Ench.")
            .replace("Potion of ", "Pot.")
            .replace("Sword", "Swd")
            .replace("Pickaxe", "Pick")
            .replace("Shovel", "Shvl")
            .replace("Helmet", "Helm")
            .replace("Chestplate", "Chest")
            .replace("Leggings", "Legs")
            .replace("Boots", "Boot");
        
        // If still too long, use first 12 characters with ellipsis indicator
        if (shortName.length() > 12) {
            shortName = shortName.substring(0, 9) + "...";
        }
        
        return shortName;
    }
    
    /**
     * Update the text on a sign shop with admin shop support and enhanced visuals
     */
    private void updateSignText(Level level, BlockPos pos, ItemStack item, double buyPrice, double sellPrice, int quantity, boolean isAdminShop) {
        if (level.getBlockEntity(pos) instanceof SignBlockEntity signEntity) {
            Component[] newLines = new Component[4];
            
            // Enhanced header based on shop type with better styling
            if (isAdminShop) {
                newLines[0] = Component.literal("[ADMIN SHOP]").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
            } else {
                newLines[0] = Component.literal(SHOP_HEADER).setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_BLUE).withBold(true));
            }
            
            // Enhanced item display with quantity and smart abbreviation
            String itemDisplay = quantity + "x " + getShortItemName(item);
            newLines[1] = Component.literal(itemDisplay).setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));
            
            // Enhanced pricing display with better formatting
            if (buyPrice > 0 && sellPrice > 0) {
                newLines[2] = Component.literal("Buy: $" + formatPrice(buyPrice)).setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
                newLines[3] = Component.literal("Sell: $" + formatPrice(sellPrice)).setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
            } else if (buyPrice > 0) {
                newLines[2] = Component.literal("Buy: $" + formatPrice(buyPrice)).setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
                if (isAdminShop) {
                    newLines[3] = Component.literal("(∞ Stock)").setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withItalic(true));
                } else {
                    newLines[3] = Component.literal("(Buy Only)").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true));
                }
            } else {
                newLines[2] = Component.literal("Sell: $" + formatPrice(sellPrice)).setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
                if (isAdminShop) {
                    newLines[3] = Component.literal("(∞ Funds)").setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withItalic(true));
                } else {
                    newLines[3] = Component.literal("(Sell Only)").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true));
                }
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
     * Check if the shop's chest has enough items for a purchase with enhanced efficiency
     */
    private boolean hasChestStock(Level level, ShopManager.SignShop signShop) {
        // Admin shops have unlimited stock
        if ("SERVER".equals(signShop.getOwnerId())) {
            return true;
        }
        
        BlockPos chestPos = signShop.getChestPos();
        if (chestPos == null) {
            LOGGER.warn("STOCK CHECK: Shop at {} has no chest connected!", signShop.getSignPos());
            return false; // No chest connected
        }
        
        LOGGER.debug("STOCK CHECK: Checking chest at {} for shop at {} owned by {}", 
                   chestPos, signShop.getSignPos(), signShop.getOwnerId());
        
        if (level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chestEntity) {
            ItemStack shopItem = signShop.getItem();
            int requiredQuantity = signShop.getQuantity();
            int availableCount = 0;
            
            // Enhanced counting with early exit optimization
            for (int i = 0; i < chestEntity.getContainerSize(); i++) {
                ItemStack stack = chestEntity.getItem(i);
                if (ItemStack.isSameItem(stack, shopItem)) {
                    availableCount += stack.getCount();
                    // Early exit if we already have enough
                    if (availableCount >= requiredQuantity) {
                        LOGGER.debug("STOCK RESULT: Early exit - found sufficient stock ({} >= {})", 
                                   availableCount, requiredQuantity);
                        return true;
                    }
                }
            }
            
            boolean hasStock = availableCount >= requiredQuantity;
            LOGGER.debug("STOCK RESULT: Shop needs {} {}, chest has {} - Stock available: {}", 
                       requiredQuantity, shopItem.getDisplayName().getString(), availableCount, hasStock);
            
            return hasStock;
        }
        
        LOGGER.warn("STOCK CHECK: No chest entity found at {}", chestPos);
        return false; // Chest not found or not a chest
    }
    
    /**
     * Remove items from the shop's chest for a purchase
     */
    private boolean removeItemsFromChest(Level level, ShopManager.SignShop signShop) {
        // Admin shops don't need to remove items - they have unlimited stock
        if ("SERVER".equals(signShop.getOwnerId())) {
            LOGGER.info("ITEM REMOVAL: Admin shop - unlimited stock, no removal needed");
            return true;
        }
        
        BlockPos chestPos = signShop.getChestPos();
        if (chestPos == null) {
            LOGGER.warn("ITEM REMOVAL: Shop at {} has no chest connected!", signShop.getSignPos());
            return false; // No chest connected
        }
        
        LOGGER.info("ITEM REMOVAL: Removing {} {} from chest at {} for shop at {} owned by {}", 
                   signShop.getQuantity(), signShop.getItem().getDisplayName().getString(),
                   chestPos, signShop.getSignPos(), signShop.getOwnerId());
        
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
                    
                    LOGGER.debug("ITEM REMOVAL: Removed {} from slot {}, {} remaining to remove", 
                               removeFromStack, i, toRemove);
                }
            }
            
            // Mark chest as changed
            chestEntity.setChanged();
            boolean success = toRemove == 0;
            
            if (success) {
                LOGGER.info("ITEM REMOVAL: Successfully removed all {} items from chest", requiredQuantity);
            } else {
                LOGGER.warn("ITEM REMOVAL: Could not remove all items! {} items still needed", toRemove);
            }
            
            return success; // Success if we removed all required items
        }

        LOGGER.warn("ITEM REMOVAL: No chest entity found at {}", chestPos);
        return false; // Chest not found or not a chest
    }
    
    /**
     * Check if the shop's chest has space for items being sold
     */
    private boolean chestHasSpace(Level level, ShopManager.SignShop signShop) {
        // Admin shops have unlimited storage space (items are void when sold to them)
        if ("SERVER".equals(signShop.getOwnerId())) {
            LOGGER.info("SPACE CHECK: Admin shop - unlimited storage space");
            return true;
        }
        
        BlockPos chestPos = signShop.getChestPos();
        if (chestPos == null) {
            LOGGER.warn("SPACE CHECK: Shop at {} has no chest connected!", signShop.getSignPos());
            return false; // No chest connected
        }
        
        LOGGER.info("SPACE CHECK: Checking space in chest at {} for {} {} for shop at {} owned by {}", 
                   chestPos, signShop.getQuantity(), signShop.getItem().getDisplayName().getString(),
                   signShop.getSignPos(), signShop.getOwnerId());
        
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
            
            boolean hasSpace = remainingToAdd <= 0;
            LOGGER.info("SPACE RESULT: Chest has space for all {} items: {}", quantityToAdd, hasSpace);
            
            return hasSpace; // Success if all items can fit
        }
        
        LOGGER.warn("SPACE CHECK: No chest entity found at {}", chestPos);
        return false; // Chest not found or not a chest
    }
    
    /**
     * Add items to the shop's chest when a player sells
     */
    private boolean addItemsToChest(Level level, ShopManager.SignShop signShop) {
        // Admin shops void all items sold to them (unlimited storage)
        if ("SERVER".equals(signShop.getOwnerId())) {
            LOGGER.info("ITEM ADDITION: Admin shop - items voided, no addition needed");
            return true;
        }
        
        BlockPos chestPos = signShop.getChestPos();
        if (chestPos == null) {
            LOGGER.warn("ITEM ADDITION: Shop at {} has no chest connected!", signShop.getSignPos());
            return false; // No chest connected
        }
        
        LOGGER.info("ITEM ADDITION: Adding {} {} to chest at {} for shop at {} owned by {}", 
                   signShop.getQuantity(), signShop.getItem().getDisplayName().getString(),
                   chestPos, signShop.getSignPos(), signShop.getOwnerId());
        
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
                    
                    LOGGER.debug("ITEM ADDITION: Added {} to empty slot {}, {} remaining to add", 
                               toAdd, i, remainingToAdd);
                } else if (ItemStack.isSameItem(stack, shopItem)) {
                    // Same item - add to existing stack
                    int spaceInStack = shopItem.getMaxStackSize() - stack.getCount();
                    int toAdd = Math.min(remainingToAdd, spaceInStack);
                    stack.grow(toAdd);
                    remainingToAdd -= toAdd;
                    
                    LOGGER.debug("ITEM ADDITION: Added {} to existing stack in slot {}, {} remaining to add", 
                               toAdd, i, remainingToAdd);
                }
            }
            
            // Mark chest as changed
            chestEntity.setChanged();
            boolean success = remainingToAdd == 0;
            
            if (success) {
                LOGGER.info("ITEM ADDITION: Successfully added all {} items to chest", quantityToAdd);
            } else {
                LOGGER.warn("ITEM ADDITION: Could not add all items! {} items still need to be added", remainingToAdd);
            }
            
            return success; // Success if we added all items
        }

        LOGGER.warn("ITEM ADDITION: No chest entity found at {}", chestPos);
        return false; // Chest not found or not a chest
    }
    
    /**
     * Refresh all shop signs in the world to update their colors based on current stock
     */
    public static int refreshAllShopSigns(net.minecraft.server.level.ServerLevel level) {
        try {
            ShopManager shopManager = ShopManager.getInstance();
            if (shopManager == null) {
                LOGGER.error("ShopManager not available for refreshing signs");
                return 0;
            }
            
            int refreshedCount = 0;
            var allShops = shopManager.getSignShops();
            
            for (var shop : allShops) {
                if (shop != null && shop.getSignPos() != null) {
                    try {
                        // Create a temporary handler instance to call non-static methods
                        SignShopHandler handler = new SignShopHandler(shopManager);
                        
                        // CRITICAL FIX: Properly detect admin shops and call correct method
                        boolean isAdminShop = "SERVER".equals(shop.getOwnerId());
                        if (isAdminShop) {
                            // Use the admin shop updateSignText method
                            handler.updateSignText(level, shop.getSignPos(), shop.getItem(), 
                                                 shop.getBuyPrice(), shop.getSellPrice(), shop.getQuantity(), true);
                            LOGGER.debug("Refreshed ADMIN shop sign at {}", shop.getSignPos());
                        } else {
                            // Use the player shop updateSignText method
                            handler.updateSignText(level, shop.getSignPos(), shop.getItem(), 
                                                 shop.getBuyPrice(), shop.getSellPrice(), shop.getQuantity(), false);
                            LOGGER.debug("Refreshed PLAYER shop sign at {}", shop.getSignPos());
                        }
                        refreshedCount++;
                    } catch (Exception e) {
                        LOGGER.warn("Failed to refresh sign at {}: {}", shop.getSignPos(), e.getMessage());
                    }
                }
            }
            
            LOGGER.info("Refreshed {} shop signs", refreshedCount);
            return refreshedCount;
        } catch (Exception e) {
            LOGGER.error("Error refreshing all shop signs: {}", e.getMessage());
            return 0;
        }
    }
}
