package com.zerog.neoessentials.gui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles GUI click events for NeoEssentials custom GUIs
 * 
 * @author ZeroG
 * @since 2.0.0
 */
@EventBusSubscriber(modid = "neoessentials")
public class GuiClickHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuiClickHandler.class);
    
    // Track active GUI sessions
    private static final Map<UUID, GuiSession> activeSessions = new ConcurrentHashMap<>();
    
    /**
     * Register a GUI session for click handling
     */
    public static void registerSession(ServerPlayer player, CustomGuiManager.GuiType type, 
                                     Map<Integer, CustomGuiManager.GuiClickAction> clickActions) {
        activeSessions.put(player.getUUID(), new GuiSession(type, clickActions));
    }
    
    /**
     * Remove a GUI session
     */
    public static void removeSession(ServerPlayer player) {
        activeSessions.remove(player.getUUID());
    }
    
    /**
     * Handle container close events
     */
    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Clean up GUI session when container is closed
            activeSessions.remove(player.getUUID());
        }
    }
    
    /**
     * Handle GUI clicks - this would need to be called from a menu click handler
     */
    public static boolean handleClick(ServerPlayer player, int slot, ClickType clickType, ItemStack clickedItem) {
        GuiSession session = activeSessions.get(player.getUUID());
        if (session == null) {
            return false; // Not a custom GUI
        }
        
        try {
            // Get the click action for this slot
            CustomGuiManager.GuiClickAction action = session.clickActions.get(slot);
            if (action != null) {
                action.onClick(player);
                return true; // Cancel the click event
            }
            
            return true; // Cancel all clicks in custom GUIs by default
            
        } catch (Exception e) {
            LOGGER.error("Error handling GUI click for player {} in slot {}", 
                player.getName().getString(), slot, e);
            return true; // Cancel the click to prevent issues
        }
    }
    
    /**
     * GUI session data
     */
    private static class GuiSession {
        final Map<Integer, CustomGuiManager.GuiClickAction> clickActions;
        
        GuiSession(CustomGuiManager.GuiType type, Map<Integer, CustomGuiManager.GuiClickAction> clickActions) {
            this.clickActions = clickActions;
        }
    }
}
