package com.zerog.neoessentials.shops;

import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerPlayer;

import com.zerog.neoessentials.permissions.PermissionNodes;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.shops.ShopManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Handles sign shop interactions and creation - UPDATED to use separated handlers
 */
public class SignShopHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SignShopHandler.class);
    
    private final ShopManager shopManager;
    
    public SignShopHandler(ShopManager shopManager) {
        this.shopManager = shopManager;
    }
    
    /**
     * Handle player interaction with a sign
     */
    public InteractionResult handleSignInteraction(Player player, Level level, BlockPos pos, InteractionHand hand) {
        if (level.isClientSide) return InteractionResult.PASS;
        
        LOGGER.debug("Player {} interacting with sign at {}", player.getName().getString(), pos);
        
        BlockState blockState = level.getBlockState(pos);
        if (!(blockState.getBlock() instanceof SignBlock)) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity signEntity)) {
            return InteractionResult.PASS;
        }
        Component[] lines = signEntity.getFrontText().getMessages(false);
        
        // Check if this is a shop sign
        if (lines.length < 1 || !lines[0].getString().contains("[SHOP]") && !lines[0].getString().contains("[ADMIN SHOP]")) {
            return InteractionResult.PASS;
        }
        
        // Find shop by sign position
        Optional<ShopManager.SignShop> shopOptional = null;
        for (ShopManager.SignShop shop : shopManager.getSignShops()) {
            if (shop.getSignPos().equals(pos)) {
                shopOptional = Optional.of(shop);
                break;
            }
        }
        
        if (shopOptional == null || shopOptional.isEmpty()) {
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.invalid", new Object[]{}));
            return InteractionResult.FAIL;
        }
        
        ShopManager.SignShop signShop = shopOptional.get();
        
        // Validate shop integrity
        if (!validateShopIntegrity(level, signShop)) {
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.integrity.failed", new Object[]{}));
            LOGGER.error("Shop at {} failed integrity check - transaction blocked", signShop.getSignPos());
            return InteractionResult.FAIL;
        }
        
        // Check permissions
        if (!PermissionUtil.hasPermission((net.minecraft.server.level.ServerPlayer) player, PermissionNodes.SHOP_USE)) {
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.no.permission", new Object[]{}));
            return InteractionResult.FAIL;
        }
        
        // For player shops, show stock information before any transaction
        if (!signShop.isAdminShop()) {
            // Check actual chest stock vs recorded stock for integrity
            if (signShop.getChestPos() != null) {
                if (level.getBlockEntity(signShop.getChestPos()) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chestEntity) {
                    int actualStock = PlayerSignShopHandler.countItemsInChest(chestEntity, signShop.getItem());
                    
                    // Display stock status with color coding
                    if (actualStock == 0) {
                MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.out.of.stock", new Object[]{}));
                    } else if (actualStock < signShop.getQuantity()) {
                MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.low.stock", new Object[]{actualStock, signShop.getQuantity()}));
                    } else {
                MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.in.stock", new Object[]{actualStock}));
                    }
                }
            }
        }
        
        // Determine if player is buying or selling
        ItemStack heldItem = player.getItemInHand(hand);
        boolean isSelling = !heldItem.isEmpty();
        
        if (isSelling) {
            // Player wants to sell to the shop
            if (signShop.getSellPrice() <= 0) {
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.not.buying", new Object[]{}));
                return InteractionResult.FAIL;
            }
            
            // Route to appropriate handler based on shop type
            if (signShop.isAdminShop()) {
                boolean success = AdminSignShopHandler.handleSellTransaction(player, signShop, level, signShop.getQuantity());
                return success ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            } else {
                boolean success = PlayerSignShopHandler.handleSellTransaction(player, signShop, level, signShop.getQuantity());
                return success ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            }
        } else {
            // Player wants to buy from the shop
            if (signShop.getBuyPrice() <= 0) {
            MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.not.selling", new Object[]{}));
                return InteractionResult.FAIL;
            }
            
            // For player shops, prevent buying if no stock available
            if (!signShop.isAdminShop() && signShop.getChestPos() != null) {
                if (level.getBlockEntity(signShop.getChestPos()) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chestEntity) {
                    int actualStock = PlayerSignShopHandler.countItemsInChest(chestEntity, signShop.getItem());
                    if (actualStock == 0) {
                MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.transaction.blocked.empty", new Object[]{}));
                        return InteractionResult.FAIL;
                    }
                    if (actualStock < signShop.getQuantity()) {
                MessageUtil.sendMessage((ServerPlayer) player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage((ServerPlayer) player, "neoessentials.shop.transaction.blocked.partial", new Object[]{actualStock, signShop.getQuantity()}));
                        return InteractionResult.FAIL;
                    }
                }
            }
            
            // Route to appropriate handler based on shop type
            if (signShop.isAdminShop()) {
                boolean success = AdminSignShopHandler.handleBuyTransaction(player, signShop, level, signShop.getQuantity());
                return success ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            } else {
                boolean success = PlayerSignShopHandler.handleBuyTransaction(player, signShop, level, signShop.getQuantity());
                return success ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            }
        }
    }

    /**
     * Validate shop integrity - ensures shop is properly configured and accessible
     */
    private boolean validateShopIntegrity(Level level, ShopManager.SignShop signShop) {
        // Check if shop has valid sign position
        if (signShop.getSignPos() == null) {
            LOGGER.error("Shop has null sign position");
            return false;
        }
        
        // Check if sign still exists
        BlockState signState = level.getBlockState(signShop.getSignPos());
        if (!(signState.getBlock() instanceof SignBlock)) {
            LOGGER.error("Shop at {} no longer has a sign", signShop.getSignPos());
            return false;
        }
        
        // For player shops, verify chest exists and is accessible
        if (!signShop.isAdminShop() && signShop.getChestPos() != null) {
            if (!(level.getBlockEntity(signShop.getChestPos()) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity)) {
                LOGGER.error("Player shop at {} has invalid chest at {}", signShop.getSignPos(), signShop.getChestPos());
                return false;
            }
        }
        
        // Verify shop item is valid
        if (signShop.getItem() == null || signShop.getItem().isEmpty()) {
            LOGGER.error("Player shop at {} has invalid item", signShop.getSignPos());
            return false;
        }
        
        // Verify quantity is valid
        if (signShop.getQuantity() <= 0 || signShop.getQuantity() > 64) {
            LOGGER.error("Player shop at {} has invalid quantity: {}", signShop.getSignPos(), signShop.getQuantity());
            return false;
        }
        
        return true;
    }
    
    /**
     * Refresh all shop signs in the world to update their display based on current stock and shop type
     */
    public static int refreshAllShopSigns(net.minecraft.server.level.ServerLevel level) {
        try {
            com.zerog.neoessentials.economy.shops.ShopManager shopManager = com.zerog.neoessentials.economy.shops.ShopManager.getInstance();
            if (shopManager == null) {
                LOGGER.error("ShopManager not available for refreshing signs");
                return 0;
            }
            
            int refreshedCount = 0;
            var allShops = shopManager.getSignShops();
            
            LOGGER.info("Starting to refresh {} shop signs...", allShops.size());
            
            for (var shop : allShops) {
                if (shop != null && shop.getSignPos() != null) {
                    try {
                        // Determine shop type and update accordingly
                        boolean isAdminShop = "SERVER".equals(shop.getOwnerId());
                        
                        if (updateShopSignDisplay(level, shop, isAdminShop)) {
                            refreshedCount++;
                            LOGGER.debug("Successfully refreshed {} shop sign at {}", 
                                        isAdminShop ? "ADMIN" : "PLAYER", shop.getSignPos());
                        }
                    } catch (Exception e) {
                        com.zerog.neoessentials.util.ErrorHandler.handleError(
                            com.zerog.neoessentials.util.ErrorHandler.ErrorCategory.ECONOMY,
                            com.zerog.neoessentials.util.ErrorHandler.ErrorSeverity.MEDIUM,
                            "Shop Sign Refresh", e);
                    }
                }
            }
            
            LOGGER.info("Successfully refreshed {} out of {} shop signs", refreshedCount, allShops.size());
            return refreshedCount;
            
        } catch (Exception e) {
            com.zerog.neoessentials.util.ErrorHandler.handleError(
                com.zerog.neoessentials.util.ErrorHandler.ErrorCategory.ECONOMY,
                com.zerog.neoessentials.util.ErrorHandler.ErrorSeverity.HIGH,
                "Shop Signs Refresh", e);
            return 0;
        }
    }
    
    /**
     * Update a single shop sign display with proper formatting and stock indicators
     */
    private static boolean updateShopSignDisplay(net.minecraft.server.level.ServerLevel level, 
                                                com.zerog.neoessentials.economy.shops.ShopManager.SignShop shop, 
                                                boolean isAdminShop) {
        BlockPos signPos = shop.getSignPos();
        
        if (!(level.getBlockEntity(signPos) instanceof SignBlockEntity signEntity)) {
            LOGGER.warn("Sign entity not found at {}", signPos);
            return false;
        }
        
        // Determine stock status for color coding
        String stockIndicator = "";
        ChatFormatting headerColor = ChatFormatting.DARK_BLUE;
        
        if (!isAdminShop && shop.getChestPos() != null) {
            // Check actual chest stock for player shops
            if (level.getBlockEntity(shop.getChestPos()) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chestEntity) {
                int actualStock = PlayerSignShopHandler.countItemsInChest(chestEntity, shop.getItem());
                if (actualStock == 0) {
                    stockIndicator = " §4[EMPTY]";
                    headerColor = ChatFormatting.DARK_RED;
                } else if (actualStock < shop.getQuantity()) {
                    stockIndicator = " §6[LOW]";
                    headerColor = ChatFormatting.GOLD;
                } else {
                    stockIndicator = " §2[STOCK]";
                    headerColor = ChatFormatting.DARK_GREEN;
                }
            }
        } else if (isAdminShop) {
            stockIndicator = " §b[∞]";
            headerColor = ChatFormatting.AQUA;
        }
        
        // Create sign lines
        Component[] newLines = new Component[4];
        
        // Line 1: Shop type header with stock indicator
        String headerText = isAdminShop ? "[ADMIN SHOP]" : "[SHOP]";
        newLines[0] = Component.literal(headerText + stockIndicator)
                               .setStyle(Style.EMPTY.withColor(headerColor));
        
        // Line 2: Item and quantity
        String itemName = getShortItemName(shop.getItem());
        newLines[1] = Component.literal(shop.getQuantity() + "x " + itemName)
                               .setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));
        
        // Lines 3-4: Prices
        if (shop.getBuyPrice() > 0 && shop.getSellPrice() > 0) {
            newLines[2] = Component.literal("Buy: $" + String.format("%.2f", shop.getBuyPrice()))
                                   .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
            newLines[3] = Component.literal("Sell: $" + String.format("%.2f", shop.getSellPrice()))
                                   .setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
        } else if (shop.getBuyPrice() > 0) {
            newLines[2] = Component.literal("Buy: $" + String.format("%.2f", shop.getBuyPrice()))
                                   .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
            newLines[3] = Component.literal("(Buy Only)")
                                   .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        } else if (shop.getSellPrice() > 0) {
            newLines[2] = Component.literal("Sell: $" + String.format("%.2f", shop.getSellPrice()))
                                   .setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
            newLines[3] = Component.literal("(Sell Only)")
                                   .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
        } else {
            newLines[2] = Component.literal("§cInvalid Prices")
                                   .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED));
            newLines[3] = Component.literal("§cContact Admin")
                                   .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED));
        }
        
        // Update the sign
        signEntity.updateText((frontText) -> {
            return frontText.setMessage(0, newLines[0])
                           .setMessage(1, newLines[1])
                           .setMessage(2, newLines[2])
                           .setMessage(3, newLines[3]);
        }, true);
        
        signEntity.setChanged();
        level.sendBlockUpdated(signPos, level.getBlockState(signPos), level.getBlockState(signPos), 3);
        
        return true;
    }
    
    /**
     * Get a shortened item name that fits on a sign
     */
    private static String getShortItemName(ItemStack item) {
        String fullName = item.getHoverName().getString();
        if (fullName.length() <= 12) {
            return fullName;
        }
        
        // Try to abbreviate common words
        String shortened = fullName
            .replace("Diamond", "Dia")
            .replace("Golden", "Gold")
            .replace("Iron", "Fe")
            .replace("Stone", "St")
            .replace("Wood", "Wd")
            .replace("Block", "Blk")
            .replace("Ingot", "");
        
        if (shortened.length() <= 12) {
            return shortened.trim();
        }
        
        // If still too long, truncate and add...
        return shortened.substring(0, 9) + "...";
    }
}