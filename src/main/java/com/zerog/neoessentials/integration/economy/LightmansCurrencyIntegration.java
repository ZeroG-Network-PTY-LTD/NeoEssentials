package com.zerog.neoessentials.integration.economy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.server.level.ServerPlayer;
import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * Lightman's Currency Integration
 * Integrates with Lightman's Currency mod for economy operations
 * Mod: https://www.curseforge.com/minecraft/mc-mods/lightmans-currency
 * 
 * @author ZeroG
 * @since 2.0.2
 */
public class LightmansCurrencyIntegration implements EconomyIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(LightmansCurrencyIntegration.class);
    
    private boolean initialized = false;
    private DecimalFormat formatter;
    
    // Lightman's Currency API references
    private Object moneyAPI;
    
    @Override
    public String getId() {
        return "lightmans_currency";
    }
    
    @Override
    public String getName() {
        return "Lightman's Currency";
    }
    
    @Override
    public String getModId() {
        return "lightmanscurrency";
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // Check if Lightman's Currency classes are available
            Class.forName("io.github.lightman314.lightmanscurrency.api.money.MoneyAPI");
            LOGGER.debug("Lightman's Currency classes detected");
            return true;
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Lightman's Currency classes not found: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean initialize() {
        if (!isAvailable()) {
            LOGGER.warn("Cannot initialize Lightman's Currency integration - mod not available");
            return false;
        }
        
        try {
            // Initialize Lightman's Currency API
            Class<?> apiClass = Class.forName("io.github.lightman314.lightmanscurrency.api.money.MoneyAPI");
            moneyAPI = apiClass.getMethod("API").invoke(null);
            
            this.formatter = new DecimalFormat("#,##0.00");
            this.initialized = true;
            
            LOGGER.info("Lightman's Currency integration initialized successfully");
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Lightman's Currency integration: {}", e.getMessage());
            initialized = false;
            return false;
        }
    }
    
    @Override
    public void shutdown() {
        if (initialized) {
            LOGGER.info("Shutting down Lightman's Currency integration");
            moneyAPI = null;
            initialized = false;
        }
    }
    
    @Override
    public BigDecimal getBalance(ServerPlayer player) {
        if (!initialized || moneyAPI == null) {
            return BigDecimal.ZERO;
        }
        
        try {
            // Use reflection to call Lightman's Currency API
            Object moneyValue = moneyAPI.getClass()
                .getMethod("GetPlayersBalance", net.minecraft.server.level.ServerPlayer.class)
                .invoke(moneyAPI, player);
            
            // MoneyValue has getCoreValue() method that returns long
            long coreValue = (Long) moneyValue.getClass()
                .getMethod("getCoreValue")
                .invoke(moneyValue);
            
            // Convert to decimal (Lightman's uses 100 base units per coin)
            return BigDecimal.valueOf(coreValue).divide(BigDecimal.valueOf(100));
            
        } catch (Exception e) {
            LOGGER.error("Error getting balance from Lightman's Currency for {}: {}", 
                        player.getName().getString(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    @Override
    public boolean setBalance(ServerPlayer player, BigDecimal amount) {
        if (!initialized || moneyAPI == null) {
            return false;
        }
        
        try {
            // Convert amount to core value (multiply by 100)
            long coreValue = amount.multiply(BigDecimal.valueOf(100)).longValue();
            
            // Create MoneyValue object
            Class<?> moneyValueClass = Class.forName("io.github.lightman314.lightmanscurrency.api.money.value.MoneyValue");
            Object moneyValue = moneyValueClass.getMethod("fromNumber", long.class).invoke(null, coreValue);
            
            // Set player balance
            moneyAPI.getClass()
                .getMethod("SetPlayersBalance", net.minecraft.server.level.ServerPlayer.class, moneyValueClass)
                .invoke(moneyAPI, player, moneyValue);
            
            LOGGER.debug("Set Lightman's Currency balance for {} to {}", 
                        player.getName().getString(), formatCurrency(amount));
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Error setting balance in Lightman's Currency for {}: {}", 
                        player.getName().getString(), e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean depositBalance(ServerPlayer player, BigDecimal amount, String reason) {
        if (!initialized || moneyAPI == null) {
            return false;
        }
        
        try {
            BigDecimal currentBalance = getBalance(player);
            BigDecimal newBalance = currentBalance.add(amount);
            
            boolean success = setBalance(player, newBalance);
            if (success) {
                LOGGER.debug("Deposited {} to {} via Lightman's Currency ({})", 
                            formatCurrency(amount), player.getName().getString(), reason);
            }
            return success;
            
        } catch (Exception e) {
            LOGGER.error("Error depositing to Lightman's Currency for {}: {}", 
                        player.getName().getString(), e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean withdrawBalance(ServerPlayer player, BigDecimal amount, String reason) {
        if (!initialized || moneyAPI == null) {
            return false;
        }
        
        try {
            BigDecimal currentBalance = getBalance(player);
            if (currentBalance.compareTo(amount) < 0) {
                LOGGER.debug("Insufficient Lightman's Currency balance for {}: has {}, needs {}", 
                            player.getName().getString(), formatCurrency(currentBalance), formatCurrency(amount));
                return false;
            }
            
            BigDecimal newBalance = currentBalance.subtract(amount);
            boolean success = setBalance(player, newBalance);
            
            if (success) {
                LOGGER.debug("Withdrew {} from {} via Lightman's Currency ({})", 
                            formatCurrency(amount), player.getName().getString(), reason);
            }
            return success;
            
        } catch (Exception e) {
            LOGGER.error("Error withdrawing from Lightman's Currency for {}: {}", 
                        player.getName().getString(), e.getMessage());
            return false;
        }
    }
    
    @Override
    public String formatCurrency(BigDecimal amount) {
        if (formatter != null) {
            return "💰 " + formatter.format(amount); // Gold coin symbol
        }
        return "💰 " + amount.toString();
    }
    
    @Override
    public String getCurrencySymbol() {
        return "💰";
    }
    
    @Override
    public int getPriority() {
        return 25; // Medium-high priority (popular mod)
    }
}
