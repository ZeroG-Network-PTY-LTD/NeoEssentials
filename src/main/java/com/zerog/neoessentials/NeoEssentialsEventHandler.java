package com.zerog.neoessentials;

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
        
        ServerPlayer player = (ServerPlayer) event.getEntity();
        BlockPos pos = event.getPos();
        LOGGER.debug("RIGHT CLICK EVENT: Player {} clicked block at {}", player.getName().getString(), pos);
        
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
                MessageUtil.sendMessage(player, "§4[SHOP PROTECTION] §cThis admin shop sign is protected!");
                MessageUtil.sendMessage(player, "§7You need the '§eneoessentials.shop.admin§7' or '§eneoessentials.shop.bypass.protection§7' permission.");
                LOGGER.warn("GRIEFING ATTEMPT: Player {} tried to break admin shop sign at {} - BLOCKED", 
                           player.getName().getString(), signPos);
            } else {
                MessageUtil.sendMessage(player, "§4[SHOP PROTECTION] §cThis shop sign is protected!");
                MessageUtil.sendMessage(player, "§7This sign belongs to another player's shop. Only the owner can break it.");
                MessageUtil.sendMessage(player, "§7Admins can use '§eneoessentials.shop.bypass.protection§7' permission.");
                LOGGER.warn("GRIEFING ATTEMPT: Player {} tried to break shop sign at {} owned by {} - BLOCKED", 
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
            MessageUtil.sendMessage(player, "§4[SHOP PROTECTION] §cThis shop chest is protected!");
            MessageUtil.sendMessage(player, "§7This chest belongs to another player's shop. Only the owner can break it.");
            MessageUtil.sendMessage(player, "§7Admins can use '§eneoessentials.shop.bypass.protection§7' permission.");
            LOGGER.warn("GRIEFING ATTEMPT: Player {} tried to break shop chest at {} owned by {} - BLOCKED", 
                       player.getName().getString(), chestPos, signShop.getOwnerId());
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
        
        // Only proceed if this is actually a chest
        if (!isChest(level, chestPos)) {
            LOGGER.debug("Block at {} is not a chest, skipping protection check", chestPos);
            return;
        }
        
        LOGGER.info("CHEST ACCESS: Player {} clicked chest at {}", player.getName().getString(), chestPos);
        
        ShopManager shopManager = ShopManager.getInstance();
        if (shopManager == null) {
            LOGGER.warn("ShopManager is null - cannot check chest protection");
            return;
        }
        
        // Find if this chest is connected to any shop
        var allShops = shopManager.getSignShops();
        LOGGER.info("Checking {} shops for chest at {}", allShops.size(), chestPos);
        
        var signShop = allShops.stream()
            .filter(shop -> {
                boolean matches = chestPos.equals(shop.getChestPos());
                if (matches) {
                    LOGGER.info("Found matching shop! Chest {} belongs to shop at sign {} owned by {}", 
                               chestPos, shop.getSignPos(), shop.getOwnerId());
                }
                return matches;
            })
            .findFirst().orElse(null);
        
        if (signShop == null) {
            LOGGER.info("Chest at {} is not connected to any shop - allowing access", chestPos);
            return; // Not a shop chest
        }
        
        LOGGER.warn("SHOP CHEST ACCESS: Player {} attempting to access shop chest at {} owned by {}", 
                   player.getName().getString(), chestPos, signShop.getOwnerId());
        
        if (!canAccessShop(player, signShop)) {
            event.setCanceled(true);
            if ("SERVER".equals(signShop.getOwnerId())) {
                MessageUtil.sendMessage(player, "§4[SHOP PROTECTION] §cThis admin shop chest is protected!");
                MessageUtil.sendMessage(player, "§7You need the '§eneoessentials.shop.admin§7' or '§eneoessentials.shop.bypass.protection§7' permission.");
                LOGGER.warn("BLOCKED: Player {} tried to access admin shop chest at {} - PROTECTION ACTIVE", 
                           player.getName().getString(), chestPos);
            } else {
                MessageUtil.sendMessage(player, "§4[SHOP PROTECTION] §cThis shop chest is protected!");
                MessageUtil.sendMessage(player, "§7This chest belongs to another player's shop. Only the owner can access it.");
                MessageUtil.sendMessage(player, "§7Admins can use '§eneoessentials.shop.bypass.protection§7' permission.");
                LOGGER.warn("BLOCKED: Player {} tried to access shop chest at {} owned by {} - PROTECTION ACTIVE", 
                           player.getName().getString(), chestPos, signShop.getOwnerId());
            }
        } else {
            LOGGER.info("ALLOWED: Player {} successfully accessed shop chest at {} - permission granted", 
                       player.getName().getString(), chestPos);
        }
    }
    
    /**
     * Check if player can break/remove a shop
     */
    private static boolean canBreakShop(ServerPlayer player, ShopManager.SignShop signShop) {
        // Check for bypass permission first (for admin players)
        if (PermissionUtil.hasPermission(player, PermissionNodes.SHOP_BYPASS_PROTECTION)) {
            LOGGER.info("Player {} bypassed shop protection with bypass permission", player.getName().getString());
            return true;
        }
        
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
        // Check for bypass permission first (for admin players)
        if (PermissionUtil.hasPermission(player, PermissionNodes.SHOP_BYPASS_PROTECTION)) {
            LOGGER.info("Player {} bypassed shop protection with bypass permission", player.getName().getString());
            return true;
        }
        
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
     * Check if a block is a chest (renamed for clarity)
     */
    private static boolean isChest(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof ChestBlock;
    }
    
    /**
     * Check if a block is a chest that belongs to a shop
     */
    private static boolean isShopChest(Level level, BlockPos pos) {
        if (!isChest(level, pos)) return false;
        
        ShopManager shopManager = ShopManager.getInstance();
        if (shopManager == null) return false;
        
        // Check if this chest is connected to any shop
        return shopManager.getSignShops().stream()
            .anyMatch(shop -> pos.equals(shop.getChestPos()));
    }

    }
