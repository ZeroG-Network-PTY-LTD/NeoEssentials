package com.zerog.neoessentials.listeners;

import com.zerog.neoessentials.managers.EconomyManager;
import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * Event listener for economy system integration
 * Handles initialization of player economy data when they first join
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class EconomyEventListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EconomyEventListener.class);
    
    /**
     * Handle player join - ensure their economy data is initialized
     */
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        try {
            EconomyManager economyManager = EconomyManager.getInstance();
            
            // Skip if economy is disabled
            if (!economyManager.isEnabled()) {
                return;
            }
            
            // This will trigger initialization if the player is new
            // The getBalance method handles checking if the player needs starting balance
            BigDecimal balance = economyManager.getBalance(player.getUUID());
            
            LOGGER.info("Player {} joined with balance: {}", 
                player.getName().getString(), 
                economyManager.formatCurrency(balance));
            
        } catch (Exception e) {
            LOGGER.error("Error initializing economy data for player {}", 
                player.getName().getString(), e);
        }
    }
    
    /**
     * Handle player leave - save their economy data
     */
    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        try {
            EconomyManager economyManager = EconomyManager.getInstance();
            
            // Skip if economy is disabled
            if (!economyManager.isEnabled()) {
                return;
            }
            
            // Economy data is automatically saved through the PlayerDataManager
            // This is just for logging purposes
            BigDecimal balance = economyManager.getBalance(player.getUUID());
            
            LOGGER.info("Player {} left with balance: {}", 
                player.getName().getString(), 
                economyManager.formatCurrency(balance));
            
        } catch (Exception e) {
            LOGGER.error("Error saving economy data for player {}", 
                player.getName().getString(), e);
        }
    }
}
