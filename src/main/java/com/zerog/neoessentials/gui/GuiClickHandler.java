package com.zerog.neoessentials.gui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
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
    
    // Track menu instances for click handling
    private static final Map<AbstractContainerMenu, GuiSession> menuSessions = new ConcurrentHashMap<>();
    
    /**
     * Register a GUI session for click handling
     */
    public static void registerSession(ServerPlayer player, CustomGuiManager.GuiType type, 
                                     Map<Integer, CustomGuiManager.GuiClickAction> clickActions) {
        GuiSession session = new GuiSession(type, clickActions);
        activeSessions.put(player.getUUID(), session);
        
        // Also register by menu if available
        if (player.containerMenu != null) {
            menuSessions.put(player.containerMenu, session);
        }
        
        LOGGER.debug("Registered GUI session for player {}: {}", player.getName().getString(), type);
    }
    
    /**
     * Remove a GUI session
     */
    public static void removeSession(ServerPlayer player) {
        GuiSession removed = activeSessions.remove(player.getUUID());
        
        // Also remove from menu sessions
        if (player.containerMenu != null) {
            menuSessions.remove(player.containerMenu);
        }
        
        if (removed != null) {
            LOGGER.debug("Removed GUI session for player {}", player.getName().getString());
        }
    }
    
    /**
     * Handle container open events
     */
    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Re-register menu session if player has an active GUI session
            GuiSession session = activeSessions.get(player.getUUID());
            if (session != null) {
                menuSessions.put(event.getContainer(), session);
                LOGGER.debug("Re-registered menu session for player {}", player.getName().getString());
            }
        }
    }
    
    /**
     * Handle container close events
     */
    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Clean up GUI session when container is closed
            activeSessions.remove(player.getUUID());
            menuSessions.remove(event.getContainer());
            LOGGER.debug("Cleaned up GUI session for player {}", player.getName().getString());
        }
    }
    
    /**
     * Handle GUI clicks - this would need to be called from a menu click handler
     * This method can be called by custom menu implementations
     */
    public static boolean handleClick(ServerPlayer player, int slot, ClickType clickType, ItemStack clickedItem) {
        GuiSession session = activeSessions.get(player.getUUID());
        if (session == null) {
            // Try to get session from current menu
            if (player.containerMenu != null) {
                session = menuSessions.get(player.containerMenu);
            }
            
            if (session == null) {
                return false; // Not a custom GUI
            }
        }
        
        try {
            // Get the click action for this slot
            CustomGuiManager.GuiClickAction action = session.clickActions.get(slot);
            if (action != null) {
                LOGGER.debug("Executing click action for player {} in slot {}", player.getName().getString(), slot);
                action.onClick(player);
                return true; // Cancel the click event
            }
            
            // Cancel all clicks in custom GUIs by default to prevent item movement
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Error handling GUI click for player {} in slot {}", 
                player.getName().getString(), slot, e);
            return true; // Cancel the click to prevent issues
        }
    }
    
    /**
     * Handle menu click for specific container menu
     */
    public static boolean handleMenuClick(AbstractContainerMenu menu, ServerPlayer player, int slot, 
                                        ClickType clickType, ItemStack clickedItem) {
        GuiSession session = menuSessions.get(menu);
        if (session == null) {
            return false; // Not a custom GUI menu
        }
        
        try {
            // Get the click action for this slot
            CustomGuiManager.GuiClickAction action = session.clickActions.get(slot);
            if (action != null) {
                LOGGER.debug("Executing menu click action for player {} in slot {}", player.getName().getString(), slot);
                action.onClick(player);
                return true; // Cancel the click event
            }
            
            // Cancel all clicks in custom GUIs by default
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Error handling menu click for player {} in slot {}", 
                player.getName().getString(), slot, e);
            return true; // Cancel the click to prevent issues
        }
    }
    
    /**
     * Check if a player has an active GUI session
     */
    public static boolean hasActiveSession(ServerPlayer player) {
        return activeSessions.containsKey(player.getUUID());
    }
    
    /**
     * Check if a menu has an active GUI session
     */
    public static boolean hasActiveMenuSession(AbstractContainerMenu menu) {
        return menuSessions.containsKey(menu);
    }
    
    /**
     * Get the GUI type for a player's active session
     */
    public static CustomGuiManager.GuiType getActiveGuiType(ServerPlayer player) {
        GuiSession session = activeSessions.get(player.getUUID());
        return session != null ? session.guiType : null;
    }
    
    /**
     * Clean up expired sessions (called periodically)
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Clean up sessions for offline players every 20 ticks (1 second)
        if (event.getServer().getTickCount() % 20 == 0) {
            activeSessions.entrySet().removeIf(entry -> {
                UUID playerId = entry.getKey();
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerId);
                return player == null || !player.isAlive();
            });
            
            // Clean up menu sessions for closed menus
            menuSessions.entrySet().removeIf(entry -> {
                AbstractContainerMenu menu = entry.getKey();
                // Check if menu is still valid (this is a simplified check)
                return menu == null;
            });
        }
    }
    
    /**
     * GUI session data
     */
    private static class GuiSession {
        final CustomGuiManager.GuiType guiType;
        final Map<Integer, CustomGuiManager.GuiClickAction> clickActions;
        
        GuiSession(CustomGuiManager.GuiType guiType, Map<Integer, CustomGuiManager.GuiClickAction> clickActions) {
            this.guiType = guiType;
            this.clickActions = clickActions;
        }
    }
}
