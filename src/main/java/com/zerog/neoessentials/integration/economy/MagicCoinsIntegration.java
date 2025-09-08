package com.zerog.neoessentials.integration.economy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.server.level.ServerPlayer;
import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * Magic Coins Integration
 * Integrates with Magic Coins mod for economy operations
 * Mod: https://www.curseforge.com/minecraft/mc-mods/magic-coins
 * 
 * @author ZeroG
 * @since 2.0.2
 */
public class MagicCoinsIntegration implements EconomyIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(MagicCoinsIntegration.class);
    
    private boolean initialized = false;
    private DecimalFormat formatter;
    
    // Magic Coins API references
    private Object coinsAPI;
    
    @Override
    public String getId() {
        return "magic_coins";
    }
    
    @Override
    public String getName() {
        return "Magic Coins";
    }
    
    @Override
    public String getModId() {
        return "magiccoins";
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // Check if Magic Coins classes are available
            Class.forName("com.magiccoins.api.CoinsAPI");
            LOGGER.debug("Magic Coins classes detected");
            return true;
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Magic Coins classes not found: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean initialize() {
        if (!isAvailable()) {
            LOGGER.warn("Cannot initialize Magic Coins integration - mod not available");
            return false;
        }
        
        try {
            // Initialize Magic Coins API
            Class<?> apiClass = Class.forName("com.magiccoins.api.CoinsAPI");
            coinsAPI = apiClass.getMethod("getInstance").invoke(null);
            
            this.formatter = new DecimalFormat("#,##0.00");
            this.initialized = true;
            
            LOGGER.info("Magic Coins integration initialized successfully");
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Magic Coins integration: {}", e.getMessage());
            initialized = false;
            return false;
        }
    }
    
    @Override
    public void shutdown() {
        if (initialized) {
            LOGGER.info("Shutting down Magic Coins integration");
            coinsAPI = null;
            initialized = false;
        }
    }
    
    @Override
    public BigDecimal getBalance(ServerPlayer player) {
        if (!initialized || coinsAPI == null) {
            return BigDecimal.ZERO;
        }
        
        try {
            // Use reflection to call Magic Coins API
            Object result = coinsAPI.getClass()
                .getMethod("getCoins", java.util.UUID.class)
                .invoke(coinsAPI, player.getUUID());
            
            if (result instanceof Number) {
                return BigDecimal.valueOf(((Number) result).doubleValue());
            }
            
            LOGGER.warn("Magic Coins getCoins returned unexpected type: {}", result.getClass());
            return BigDecimal.ZERO;
            
        } catch (Exception e) {
            LOGGER.error("Error getting balance from Magic Coins for {}: {}", 
                        player.getName().getString(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    @Override
    public boolean setBalance(ServerPlayer player, BigDecimal amount) {
        if (!initialized || coinsAPI == null) {
            return false;
        }
        
        try {
            // Use reflection to call Magic Coins API
            coinsAPI.getClass()
                .getMethod("setCoins", java.util.UUID.class, int.class)
                .invoke(coinsAPI, player.getUUID(), amount.intValue());
            
            LOGGER.debug("Set Magic Coins balance for {} to {}", 
                        player.getName().getString(), formatCurrency(amount));
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Error setting balance in Magic Coins for {}: {}", 
                        player.getName().getString(), e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean depositBalance(ServerPlayer player, BigDecimal amount, String reason) {
        if (!initialized || coinsAPI == null) {
            return false;
        }
        
        try {
            BigDecimal currentBalance = getBalance(player);
            BigDecimal newBalance = currentBalance.add(amount);
            
            boolean success = setBalance(player, newBalance);
            if (success) {
                LOGGER.debug("Deposited {} to {} via Magic Coins ({})", 
                            formatCurrency(amount), player.getName().getString(), reason);
            }
            return success;
            
        } catch (Exception e) {
            LOGGER.error("Error depositing to Magic Coins for {}: {}", 
                        player.getName().getString(), e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean withdrawBalance(ServerPlayer player, BigDecimal amount, String reason) {
        if (!initialized || coinsAPI == null) {
            return false;
        }
        
        try {
            BigDecimal currentBalance = getBalance(player);
            if (currentBalance.compareTo(amount) < 0) {
                LOGGER.debug("Insufficient Magic Coins balance for {}: has {}, needs {}", 
                            player.getName().getString(), formatCurrency(currentBalance), formatCurrency(amount));
                return false;
            }
            
            BigDecimal newBalance = currentBalance.subtract(amount);
            boolean success = setBalance(player, newBalance);
            
            if (success) {
                LOGGER.debug("Withdrew {} from {} via Magic Coins ({})", 
                            formatCurrency(amount), player.getName().getString(), reason);
            }
            return success;
            
        } catch (Exception e) {
            LOGGER.error("Error withdrawing from Magic Coins for {}: {}", 
                        player.getName().getString(), e.getMessage());
            return false;
        }
    }
    
    @Override
    public String formatCurrency(BigDecimal amount) {
        if (formatter != null) {
            return "⚪ " + formatter.format(amount); // Magic coin symbol
        }
        return "⚪ " + amount.toString();
    }
    
    @Override
    public String getCurrencySymbol() {
        return "⚪";
    }
    
    @Override
    public int getPriority() {
        return 30; // Lower priority
    }
}
