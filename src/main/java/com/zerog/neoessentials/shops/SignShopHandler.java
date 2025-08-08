package com.zerog.neoessentials.shops;

import com.zerog.neoessentials.permissions.PermissionNodes;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
            player.sendSystemMessage(Component.literal("§cThis shop is no longer valid!"));
            return InteractionResult.FAIL;
        }
        
        ShopManager.SignShop signShop = shopOptional.get();
        
        // Validate shop integrity
        if (!validateShopIntegrity(level, signShop)) {
            player.sendSystemMessage(Component.literal("§cThis shop has integrity issues and cannot be used!"));
            LOGGER.error("Shop at {} failed integrity check - transaction blocked", signShop.getSignPos());
            return InteractionResult.FAIL;
        }
        
        // Check permissions
        if (!PermissionUtil.hasPermission((net.minecraft.server.level.ServerPlayer) player, PermissionNodes.SHOP_USE)) {
            player.sendSystemMessage(Component.literal("§cYou don't have permission to use shops!"));
            return InteractionResult.FAIL;
        }
        
        // Determine if player is buying or selling
        ItemStack heldItem = player.getItemInHand(hand);
        boolean isSelling = !heldItem.isEmpty();
        
        if (isSelling) {
            // Player wants to sell to the shop
            if (signShop.getSellPrice() <= 0) {
                player.sendSystemMessage(Component.literal("§cThis shop doesn't buy items!"));
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
                player.sendSystemMessage(Component.literal("§cThis shop doesn't sell items!"));
                return InteractionResult.FAIL;
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
}