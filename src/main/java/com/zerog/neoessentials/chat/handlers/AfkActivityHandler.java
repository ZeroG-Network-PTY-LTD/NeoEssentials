package com.zerog.neoessentials.chat.handlers;

import com.zerog.neoessentials.chat.AfkManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
// Note: Some events not available in this NeoForge version
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Comprehensive activity tracking for AFK detection.
 * Tracks all player interactions that indicate the player is active.
 */
public class AfkActivityHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AfkActivityHandler.class);
    
    /**
     * Track chat activity
     */
    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            AfkManager.getInstance().updateActivity(player.getUUID());
            LOGGER.debug("Activity tracked for {}: chat", player.getName().getString());
        }
    }
    
    /**
     * Track right-click interactions (blocks, items, entities)
     */
    @SubscribeEvent
    public static void onPlayerRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AfkManager.getInstance().updateActivity(player.getUUID());
            LOGGER.debug("Activity tracked for {}: right-click block", player.getName().getString());
        }
    }
    
    @SubscribeEvent
    public static void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AfkManager.getInstance().updateActivity(player.getUUID());
            LOGGER.debug("Activity tracked for {}: right-click item", player.getName().getString());
        }
    }
    
    @SubscribeEvent
    public static void onPlayerRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AfkManager.getInstance().updateActivity(player.getUUID());
            LOGGER.debug("Activity tracked for {}: right-click entity", player.getName().getString());
        }
    }
    
    /**
     * Track left-click interactions
     */
    @SubscribeEvent
    public static void onPlayerLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AfkManager.getInstance().updateActivity(player.getUUID());
            LOGGER.debug("Activity tracked for {}: left-click block", player.getName().getString());
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AfkManager.getInstance().updateActivity(player.getUUID());
            LOGGER.debug("Activity tracked for {}: left-click empty", player.getName().getString());
        }
    }
    
    /**
     * Track block breaking/placing
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            AfkManager.getInstance().updateActivity(player.getUUID());
            LOGGER.debug("Activity tracked for {}: block break", player.getName().getString());
        }
    }
    
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AfkManager.getInstance().updateActivity(player.getUUID());
            LOGGER.debug("Activity tracked for {}: block place", player.getName().getString());
        }
    }
    
    /**
     * Track item dropping
     */
    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            AfkManager.getInstance().updateActivity(player.getUUID());
            LOGGER.debug("Activity tracked for {}: item toss", player.getName().getString());
        }
    }
    
    /**
     * Track damage taken (indicates player interaction with environment)
     * Note: LivingHurtEvent not available in this NeoForge version
     */
    /*
    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AfkManager.getInstance().updateActivity(player.getUUID());
            LOGGER.debug("Activity tracked for {}: damage taken", player.getName().getString());
        }
    }
    */
    
    /**
     * Handle player logout - notify AFK manager
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AfkManager.getInstance().onPlayerLogout(player.getUUID());
            LOGGER.debug("Player logout handled for AFK system: {}", player.getName().getString());
        }
    }
    
    /**
     * Handle player login - ensure they start as active
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AfkManager.getInstance().updateActivity(player.getUUID());
            LOGGER.debug("Player login handled for AFK system: {}", player.getName().getString());
        }
    }
}