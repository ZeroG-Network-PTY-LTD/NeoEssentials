package com.zerog.neoessentials.integration.economy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.server.level.ServerPlayer;
import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * Created Coins Integration
 * Integrates with Created Coins mod for economy operations
 * Mod: https://www.curseforge.com/minecraft/mc-mods/created-coins
 * 
 * @author ZeroG
 * @since 2.0.2
 */
public class CreatedCoinsIntegration implements EconomyIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreatedCoinsIntegration.class);
    
    private boolean initialized = false;
    private DecimalFormat formatter;
    
    // Created Coins API references
    private Object coinsAPI;
    
    @Override
    public String getId() {
        return "created_coins";
    }
    
    @Override
    public String getName() {
        return "Created Coins";
    }
    
    @Override
    public String getModId() {
        return "created_coins";
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // Check if Created Coins classes are available
            Class.forName("net.rk4z.createdcoins.api.CoinsAPI");
            LOGGER.debug("Created Coins classes detected");
            return true;
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Created Coins classes not found: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean initialize() {
        if (!isAvailable()) {
            LOGGER.warn("Cannot initialize Created Coins integration - mod not available");
            return false;
        }
        
        try {
            // Initialize Created Coins API
            Class<?> apiClass = Class.forName("net.rk4z.createdcoins.api.CoinsAPI");
            coinsAPI = apiClass.getMethod("getInstance").invoke(null);
            
            this.formatter = new DecimalFormat("#,##0.00");
            this.initialized = true;
            
            LOGGER.info("Created Coins integration initialized successfully");
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Created Coins integration: {}", e.getMessage());
            initialized = false;
            return false;
        }
    }
    
    @Override
    public void shutdown() {
        if (initialized) {
            LOGGER.info("Shutting down Created Coins integration");
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
            // Use reflection to call Created Coins API
            Object result = coinsAPI.getClass()
                .getMethod("getBalance", java.util.UUID.class)
                .invoke(coinsAPI, player.getUUID());
            
            if (result instanceof Number) {
                return BigDecimal.valueOf(((Number) result).doubleValue());
            }
            
            LOGGER.warn("Created Coins getBalance returned unexpected type: {}", result.getClass());
            return BigDecimal.ZERO;
            
        } catch (Exception e) {
            LOGGER.error("Error getting balance from Created Coins for {}: {}", 
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
            // Use reflection to call Created Coins API
            coinsAPI.getClass()
                .getMethod("setBalance", java.util.UUID.class, double.class)
                .invoke(coinsAPI, player.getUUID(), amount.doubleValue());
            
            LOGGER.debug("Set Created Coins balance for {} to {}", 
                        player.getName().getString(), formatCurrency(amount));
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Error setting balance in Created Coins for {}: {}", 
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
            // Use direct deposit method if available
            boolean success = (Boolean) coinsAPI.getClass()
                .getMethod("deposit", java.util.UUID.class, double.class)
                .invoke(coinsAPI, player.getUUID(), amount.doubleValue());
            
            if (success) {
                LOGGER.debug("Deposited {} to {} via Created Coins ({})", 
                            formatCurrency(amount), player.getName().getString(), reason);
            }
            return success;
            
        } catch (Exception e) {
            LOGGER.debug("Direct deposit method not available, using balance manipulation");
            
            // Fallback to manual balance manipulation
            try {
                BigDecimal currentBalance = getBalance(player);
                BigDecimal newBalance = currentBalance.add(amount);
                
                boolean success = setBalance(player, newBalance);
                if (success) {
                    LOGGER.debug("Deposited {} to {} via Created Coins balance manipulation ({})", 
                                formatCurrency(amount), player.getName().getString(), reason);
                }
                return success;
                
            } catch (Exception fallbackE) {
                LOGGER.error("Error depositing to Created Coins for {}: {}", 
                            player.getName().getString(), fallbackE.getMessage());
                return false;
            }
        }
    }
    
    @Override
    public boolean withdrawBalance(ServerPlayer player, BigDecimal amount, String reason) {
        if (!initialized || coinsAPI == null) {
            return false;
        }
        
        try {
            // Use direct withdraw method if available
            boolean success = (Boolean) coinsAPI.getClass()
                .getMethod("withdraw", java.util.UUID.class, double.class)
                .invoke(coinsAPI, player.getUUID(), amount.doubleValue());
            
            if (success) {
                LOGGER.debug("Withdrew {} from {} via Created Coins ({})", 
                            formatCurrency(amount), player.getName().getString(), reason);
            } else {
                LOGGER.debug("Insufficient Created Coins balance for withdrawal: {}", 
                            player.getName().getString());
            }
            return success;
            
        } catch (Exception e) {
            LOGGER.debug("Direct withdraw method not available, using balance manipulation");
            
            // Fallback to manual balance manipulation
            try {
                BigDecimal currentBalance = getBalance(player);
                if (currentBalance.compareTo(amount) < 0) {
                    LOGGER.debug("Insufficient Created Coins balance for {}: has {}, needs {}", 
                                player.getName().getString(), formatCurrency(currentBalance), formatCurrency(amount));
                    return false;
                }
                
                BigDecimal newBalance = currentBalance.subtract(amount);
                boolean success = setBalance(player, newBalance);
                
                if (success) {
                    LOGGER.debug("Withdrew {} from {} via Created Coins balance manipulation ({})", 
                                formatCurrency(amount), player.getName().getString(), reason);
                }
                return success;
                
            } catch (Exception fallbackE) {
                LOGGER.error("Error withdrawing from Created Coins for {}: {}", 
                            player.getName().getString(), fallbackE.getMessage());
                return false;
            }
        }
    }
    
    @Override
    public String formatCurrency(BigDecimal amount) {
        if (formatter != null) {
            return "🪙 " + formatter.format(amount); // Coin symbol
        }
        return "🪙 " + amount.toString();
    }
    
    @Override
    public String getCurrencySymbol() {
        return "🪙";
    }
    
    @Override
    public int getPriority() {
        return 35; // Lower priority (newer/less common mod)
    }
}
