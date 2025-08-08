package com.zerog.neoessentials.events;

import com.zerog.neoessentials.managers.*;
import com.zerog.neoessentials.economy.shops.ShopManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = "neoessentials")
public class NeoEssentialsEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(NeoEssentialsEventHandler.class);
    
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        try {
            ModerationManager moderationManager = ModerationManager.getInstance();
            if (moderationManager.isPlayerJailed(player.getUUID())) {
                event.setCanceled(true);
                MessageUtil.sendMessage(player, "§cYou cannot break blocks while jailed!");
                return;
            }
            
            // Check for shop sign protection
            Level level = (Level) event.getLevel();
            BlockPos pos = event.getPos();
            
            if (isShopSign(level, pos)) {
                handleSignBreak(player, pos, event);
                return;
            }
            
            if (isShopChest(level, pos)) {
                handleChestBreak(player, pos, event);
                return;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error handling block break event", e);
        }
    }
    
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        try {
            ModerationManager moderationManager = ModerationManager.getInstance();
            if (moderationManager.isPlayerJailed(player.getUUID())) {
                event.setCanceled(true);
                MessageUtil.sendMessage(player, "§cYou cannot place blocks while jailed!");
                return;
            }
        } catch (Exception e) {
            LOGGER.error("Error handling block place event", e);
        }
    }
    
    @SubscribeEvent
    public static void onRightClickBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        if (event.getLevel().isClientSide()) return;
        
        try {
            // Check for shop chest access protection
            handleChestAccess(event);
        } catch (Exception e) {
            LOGGER.error("Error handling chest access event", e);
        }
    }
    
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        try {
            Level level = (Level) event.getLevel();
            if (level.isClientSide()) return;
            
            ShopManager shopManager = ShopManager.getInstance();
            if (shopManager == null) return;
            
            // Check all affected blocks for shop signs and chests
            var blocksToRemove = event.getAffectedBlocks().stream()
                .filter(pos -> {
                    if (isShopSign(level, pos)) {
                        LOGGER.warn("Explosion attempted to destroy shop sign at {}", pos);
                        return true; // Remove from explosion damage
                    }
                    if (isShopChest(level, pos)) {
                        // Check if this chest belongs to a shop
                        var signShop = shopManager.getSignShops().stream()
                            .filter(shop -> pos.equals(shop.getChestPos()))
                            .findFirst().orElse(null);
                        if (signShop != null) {
                            LOGGER.warn("Explosion attempted to destroy shop chest at {} owned by {}", 
                                       pos, signShop.getOwnerId());
                            return true; // Remove from explosion damage
                        }
                    }
                    return false;
                })
                .toList();
            
            // Remove protected blocks from explosion
            event.getAffectedBlocks().removeAll(blocksToRemove);
            
            if (!blocksToRemove.isEmpty()) {
                LOGGER.info("Protected {} shop-related blocks from explosion damage", blocksToRemove.size());
            }
            
        } catch (Exception e) {
            LOGGER.error("Error handling explosion event", e);
        }
    }
    
    /**
     * Handle sign break protection for shop signs
     */
    private static void handleSignBreak(ServerPlayer player, BlockPos signPos, BlockEvent.BreakEvent event) {
        ShopManager shopManager = ShopManager.getInstance();
        if (shopManager == null) return;
        
        var signShop = shopManager.getSignShops().stream()
            .filter(shop -> shop.getSignPos().equals(signPos))
            .findFirst().orElse(null);
        
        if (signShop == null) return; // Not a shop sign
        
        LOGGER.info("Player {} attempted to break shop sign at {} owned by {}", 
                   player.getName().getString(), signPos, signShop.getOwnerId());
        
        if (!canBreakShop(player, signShop)) {
            event.setCanceled(true);
            if ("SERVER".equals(signShop.getOwnerId())) {
                MessageUtil.sendMessage(player, "§cYou cannot break this admin shop sign! You need admin permissions.");
                LOGGER.warn("Player {} tried to break admin shop sign at {} - BLOCKED", 
                           player.getName().getString(), signPos);
            } else {
                MessageUtil.sendMessage(player, "§cYou cannot break this shop sign! It belongs to another player.");
                LOGGER.warn("Player {} tried to break shop sign at {} owned by {} - BLOCKED", 
                           player.getName().getString(), signPos, signShop.getOwnerId());
            }
            return;
        }
        
        // Player can break the shop - remove it from the system
        shopManager.removeSignShop(signPos);
        String shopType = "SERVER".equals(signShop.getOwnerId()) ? "Admin shop" : "Shop";
        MessageUtil.sendMessage(player, "§a" + shopType + " removed successfully!");
        LOGGER.info("Player {} successfully removed {} at {}", 
                   player.getName().getString(), shopType, signPos);
    }
    
    /**
     * Handle chest break protection for shop chests
     */
    private static void handleChestBreak(ServerPlayer player, BlockPos chestPos, BlockEvent.BreakEvent event) {
        ShopManager shopManager = ShopManager.getInstance();
        if (shopManager == null) return;
        
        // Find if this chest is connected to any shop
        var signShop = shopManager.getSignShops().stream()
            .filter(shop -> chestPos.equals(shop.getChestPos()))
            .findFirst().orElse(null);
        
        if (signShop == null) return; // Not a shop chest
        
        // Admin shops don't need chest protection since they don't rely on chests
        if ("SERVER".equals(signShop.getOwnerId())) {
            return; // Allow breaking chests connected to admin shops
        }
        
        if (!canBreakShop(player, signShop)) {
            event.setCanceled(true);
            MessageUtil.sendMessage(player, "§cYou cannot break this chest! It belongs to a shop owned by another player.");
            return;
        }
        
        MessageUtil.sendMessage(player, "§eWarning: Breaking this chest will affect the connected shop!");
    }
    
    /**
     * Handle chest access protection for shop chests
     */
    private static void handleChestAccess(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        BlockPos chestPos = event.getPos();
        Level level = (Level) event.getLevel();
        
        if (!isShopChest(level, chestPos)) return;
        
        ShopManager shopManager = ShopManager.getInstance();
        if (shopManager == null) return;
        
        // Find if this chest is connected to any shop
        var signShop = shopManager.getSignShops().stream()
            .filter(shop -> chestPos.equals(shop.getChestPos()))
            .findFirst().orElse(null);
        
        if (signShop == null) return; // Not a shop chest
        
        if (!canAccessShop(player, signShop)) {
            event.setCanceled(true);
            if ("SERVER".equals(signShop.getOwnerId())) {
                MessageUtil.sendMessage(player, "§cYou cannot access this admin shop chest! You need admin permissions.");
            } else {
                MessageUtil.sendMessage(player, "§cYou cannot access this shop chest! It belongs to another player.");
            }
        }
    }
    
    /**
     * Check if player can break/remove a shop
     */
    private static boolean canBreakShop(ServerPlayer player, ShopManager.SignShop signShop) {
        // Admin shops can only be broken by players with admin permissions
        if ("SERVER".equals(signShop.getOwnerId())) {
            return PermissionUtil.hasPermission(player, PermissionNodes.SHOP_ADMIN);
        }
        // Player shops can be broken by owner or admins
        return signShop.getOwnerId().equals(player.getStringUUID()) || 
               PermissionUtil.hasPermission(player, PermissionNodes.SHOP_ADMIN);
    }
    
    /**
     * Check if player can access shop chest (stricter than breaking)
     */
    private static boolean canAccessShop(ServerPlayer player, ShopManager.SignShop signShop) {
        // Admin shops can only be accessed by players with admin permissions
        if ("SERVER".equals(signShop.getOwnerId())) {
            return PermissionUtil.hasPermission(player, PermissionNodes.SHOP_ADMIN);
        }
        // Player shops can only be accessed by the owner or admins
        return signShop.getOwnerId().equals(player.getStringUUID()) || 
               PermissionUtil.hasPermission(player, PermissionNodes.SHOP_ADMIN);
    }
    
    /**
     * Check if a block is a shop sign
     */
    private static boolean isShopSign(Level level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof SignBlock)) return false;
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity signEntity)) return false;
        Component[] lines = signEntity.getFrontText().getMessages(false);
        String firstLine = lines.length > 0 ? lines[0].getString() : "";
        return "[SHOP]".equals(firstLine) || "[Admin Shop]".equals(firstLine);
    }
    
    /**
     * Check if a block is a chest
     */
    private static boolean isShopChest(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof ChestBlock;
    }

    }
