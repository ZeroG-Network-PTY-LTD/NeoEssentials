package com.zerog.neoessentials.economy.shops;

import com.zerog.neoessentials.permissions.PermissionNodes;
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
        if (!signShop.hasStock()) {
            player.sendSystemMessage(Component.literal("§cThis shop is out of stock!"));
            return InteractionResult.FAIL;
        }
        
        double totalPrice = signShop.getBuyPrice() * signShop.getQuantity();
        
        // Get the economy manager and check if economy is enabled
        com.zerog.neoessentials.economy.EconomyManager economyManager = 
            com.zerog.neoessentials.economy.EconomyManager.getInstance();
        
        if (economyManager == null || !economyManager.isEnabled()) {
            player.sendSystemMessage(Component.literal("§cEconomy system is not available!"));
            return InteractionResult.FAIL;
        }
        
        // Check if player has enough money
        if (!economyManager.hasBalance(player.getUUID(), totalPrice)) {
            player.sendSystemMessage(Component.literal(String.format(
                    "§cInsufficient funds! You need %s but have %s",
                    economyManager.formatCurrency(totalPrice),
                    economyManager.formatCurrency(economyManager.getBalance(player.getUUID()).doubleValue())
            )));
            return InteractionResult.FAIL;
        }
        
        // Process the transaction through the shop manager (which handles economy deduction)
        boolean success = shopManager.processTransaction(
                player.getStringUUID(),
                "sign_shop_" + signShop.getSignPos().toShortString(),
                signShop.getItem(),
                signShop.getQuantity(),
                totalPrice
        );
        
        if (success) {
            // Give items to player
            ItemStack itemToGive = signShop.getItem().copy();
            itemToGive.setCount(signShop.getQuantity());
            
            if (!player.getInventory().add(itemToGive)) {
                player.spawnAtLocation(itemToGive);
            }
            
            // Reduce shop stock
            signShop.setStock(signShop.getStock() - signShop.getQuantity());
            
            player.sendSystemMessage(Component.literal(String.format(
                    "§aPurchased %dx %s for %s",
                    signShop.getQuantity(),
                    signShop.getItem().getDisplayName().getString(),
                    economyManager.formatCurrency(totalPrice)
            )));
            
            LOGGER.info("Player {} purchased {}x {} for {} from sign shop at {}", 
                       player.getName().getString(), signShop.getQuantity(), 
                       signShop.getItem().getDisplayName().getString(), 
                       economyManager.formatCurrency(totalPrice),
                       signShop.getSignPos());
            
            return InteractionResult.SUCCESS;
        } else {
            player.sendSystemMessage(Component.literal("§cTransaction failed! Please try again."));
            return InteractionResult.FAIL;
        }
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
        
        // Remove items from player inventory
        int toRemove = quantityToSell;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItem(stack, shopItem) && toRemove > 0) {
                int removeFromStack = Math.min(stack.getCount(), toRemove);
                stack.shrink(removeFromStack);
                toRemove -= removeFromStack;
            }
        }
        
        // Add money to player through economy manager
        double earnings = signShop.getSellPrice() * quantityToSell;
        
        // Get the economy manager and add money to player
        com.zerog.neoessentials.economy.EconomyManager economyManager = 
            com.zerog.neoessentials.economy.EconomyManager.getInstance();
        
        if (economyManager != null && economyManager.isEnabled()) {
            boolean success = economyManager.addBalance(
                player.getUUID(), 
                economyManager.getCurrencyManager().getPrimaryCurrency(), 
                java.math.BigDecimal.valueOf(earnings)
            );
            
            if (success) {
                player.sendSystemMessage(Component.literal(String.format(
                        "§aSold %dx %s for %s",
                        quantityToSell,
                        shopItem.getDisplayName().getString(),
                        economyManager.formatCurrency(earnings)
                )));
                
                LOGGER.info("Player {} sold {}x {} for {} to sign shop at {}", 
                           player.getName().getString(), quantityToSell, 
                           shopItem.getDisplayName().getString(), 
                           economyManager.formatCurrency(earnings),
                           signShop.getSignPos());
                
                return InteractionResult.SUCCESS;
            } else {
                // If adding money failed, give items back
                ItemStack itemToReturn = shopItem.copy();
                itemToReturn.setCount(quantityToSell);
                if (!player.getInventory().add(itemToReturn)) {
                    player.spawnAtLocation(itemToReturn);
                }
                
                player.sendSystemMessage(Component.literal("§cFailed to process payment! Items returned."));
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
     * Check if player has permission
     */
    private boolean hasPermission(Player player, String permission) {
        // TODO: Implement proper permission checking
        return true; // For now, allow all players
    }
}
