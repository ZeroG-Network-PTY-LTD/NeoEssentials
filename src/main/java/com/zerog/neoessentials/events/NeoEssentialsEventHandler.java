package com.zerog.neoessentials.events;

import com.zerog.neoessentials.managers.*;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
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
            if (handleShopProtection(event)) return;
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
    
    private static boolean handleShopProtection(BlockEvent.BreakEvent event) {
        ServerPlayer player = (ServerPlayer) event.getPlayer();
        BlockPos pos = event.getPos();
        Level level = (Level) event.getLevel();
        if (isShopSign(level, pos)) return handleSignBreak(player, pos, event);
        if (isShopChest(level, pos)) return handleChestBreak(player, pos, event);
        return false;
    }
    
    private static boolean handleSignBreak(ServerPlayer player, BlockPos signPos, BlockEvent.BreakEvent event) {
        com.zerog.neoessentials.economy.shops.ShopManager shopManager = 
            com.zerog.neoessentials.economy.shops.ShopManager.getInstance();
        if (shopManager == null) return false;
        var signShop = shopManager.getSignShops().stream()
            .filter(shop -> shop.getSignPos().equals(signPos))
            .findFirst().orElse(null);
        if (signShop == null) return false;
        if (!canBreakShop(player, signShop)) {
            event.setCanceled(true);
            if ("SERVER".equals(signShop.getOwnerId())) {
                MessageUtil.sendMessage(player, "§cYou cannot break this admin shop sign! You need admin permissions.");
            } else {
                MessageUtil.sendMessage(player, "§cYou cannot break this shop sign! It belongs to another player.");
            }
            return true;
        }
        shopManager.removeSignShop(signPos);
        String shopType = "SERVER".equals(signShop.getOwnerId()) ? "Admin shop" : "Shop";
        MessageUtil.sendMessage(player, "§a" + shopType + " removed successfully!");
        return false;
    }
    
    private static boolean handleChestBreak(ServerPlayer player, BlockPos chestPos, BlockEvent.BreakEvent event) {
        com.zerog.neoessentials.economy.shops.ShopManager shopManager = 
            com.zerog.neoessentials.economy.shops.ShopManager.getInstance();
        if (shopManager == null) return false;
        
        // Find if this chest is connected to any shop
        var signShop = shopManager.getSignShops().stream()
            .filter(shop -> chestPos.equals(shop.getChestPos()))
            .findFirst().orElse(null);
        if (signShop == null) return false; // Not a shop chest
        
        // Admin shops don't need chest protection since they don't rely on chests
        if ("SERVER".equals(signShop.getOwnerId())) {
            return false; // Allow breaking chests connected to admin shops
        }
        
        if (!canBreakShop(player, signShop)) {
            event.setCanceled(true);
            MessageUtil.sendMessage(player, "§cYou cannot break this chest! It belongs to a shop owned by another player.");
            return true;
        }
        MessageUtil.sendMessage(player, "§eWarning: Breaking this chest will affect the connected shop!");
        return false;
    }
    
    private static boolean canBreakShop(ServerPlayer player, com.zerog.neoessentials.economy.shops.ShopManager.SignShop signShop) {
        // Admin shops can only be broken by players with admin permissions
        if ("SERVER".equals(signShop.getOwnerId())) {
            return player.hasPermissions(4); // Require OP level for admin shops
        }
        // Player shops can be broken by owner or admins
        return signShop.getOwnerId().equals(player.getStringUUID()) || player.hasPermissions(4);
    }
    
    private static boolean isShopSign(Level level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.SignBlock)) return false;
        if (!(level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.SignBlockEntity signEntity)) return false;
        net.minecraft.network.chat.Component[] lines = signEntity.getFrontText().getMessages(false);
        String firstLine = lines.length > 0 ? lines[0].getString() : "";
        return "[SHOP]".equals(firstLine) || "[Admin Shop]".equals(firstLine);
    }
    
    private static boolean isShopChest(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.ChestBlock;
    }
}
