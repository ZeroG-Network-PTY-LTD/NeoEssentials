package com.zerog.neoessentials.integration.economy;

import net.minecraft.server.level.ServerPlayer;
import java.math.BigDecimal;

/**
 * Interface for external economy integrations
 * Provides a standardized way to integrate with various economy mods
 * 
 * @author ZeroG
 * @since 2.0.2
 */
public interface EconomyIntegration {
    
    /**
     * Get the unique identifier for this integration
     */
    String getId();
    
    /**
     * Get the display name for this integration
     */
    String getName();
    
    /**
     * Get the mod ID this integration supports
     */
    String getModId();
    
    /**
     * Check if the required mod is available
     */
    boolean isAvailable();
    
    /**
     * Initialize the integration
     * @return true if initialization was successful
     */
    boolean initialize();
    
    /**
     * Shutdown the integration
     */
    void shutdown();
    
    /**
     * Get player's balance
     */
    BigDecimal getBalance(ServerPlayer player);
    
    /**
     * Set player's balance
     */
    boolean setBalance(ServerPlayer player, BigDecimal amount);
    
    /**
     * Add money to player's balance
     */
    boolean depositBalance(ServerPlayer player, BigDecimal amount, String reason);
    
    /**
     * Remove money from player's balance
     */
    boolean withdrawBalance(ServerPlayer player, BigDecimal amount, String reason);
    
    /**
     * Check if player has sufficient balance
     */
    default boolean hasBalance(ServerPlayer player, BigDecimal amount) {
        BigDecimal balance = getBalance(player);
        return balance != null && balance.compareTo(amount) >= 0;
    }
    
    /**
     * Format currency amount for display
     */
    String formatCurrency(BigDecimal amount);
    
    /**
     * Get the currency symbol used by this economy
     */
    default String getCurrencySymbol() {
        return "$";
    }
    
    /**
     * Get integration priority (lower = higher priority)
     */
    default int getPriority() {
        return 100;
    }
}
