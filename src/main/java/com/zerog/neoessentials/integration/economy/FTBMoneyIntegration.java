package com.zerog.neoessentials.integration.economy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.server.level.ServerPlayer;
import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * FTB Money Integration
 * Integrates with FTB Money Forge mod for economy operations
 * Mod: https://www.curseforge.com/minecraft/mc-mods/ftb-money-forge
 * 
 * @author ZeroG
 * @since 2.0.2
 */
public class FTBMoneyIntegration implements EconomyIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(FTBMoneyIntegration.class);
    
    private boolean available = false;
    private boolean initialized = false;
    private DecimalFormat formatter;
    
    // FTB Money API references (will be loaded dynamically)
    private Object moneyAPI;
    
    @Override
    public String getId() {
        return "ftb_money";
    }
    
    @Override
    public String getName() {
        return "FTB Money";
    }
    
    @Override
    public String getModId() {
        return "ftbmoney";
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // Check if FTB Money classes are available
            Class.forName("dev.ftb.mods.ftbmoney.api.MoneyAPI");
            available = true;
            LOGGER.debug("FTB Money classes detected");
            return true;
        } catch (ClassNotFoundException e) {
            LOGGER.debug("FTB Money classes not found: {}", e.getMessage());
            available = false;
            return false;
        }
    }
    
    @Override
    public boolean initialize() {
        if (!isAvailable()) {
            LOGGER.warn("Cannot initialize FTB Money integration - mod not available");
            return false;
        }
        
        try {
            // Initialize FTB Money API
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbmoney.api.MoneyAPI");
            moneyAPI = apiClass.getMethod("getInstance").invoke(null);
            
            this.formatter = new DecimalFormat("#,##0.00");
            this.initialized = true;
            
            LOGGER.info("FTB Money integration initialized successfully");
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize FTB Money integration: {}", e.getMessage());
            initialized = false;
            return false;
        }
    }
    
    @Override
    public void shutdown() {
        if (initialized) {
            LOGGER.info("Shutting down FTB Money integration");
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
            // Use reflection to call FTB Money API
            Object result = moneyAPI.getClass()
                .getMethod("getBalance", java.util.UUID.class)
                .invoke(moneyAPI, player.getUUID());
            
            if (result instanceof Number) {
                return BigDecimal.valueOf(((Number) result).doubleValue());
            }
            
            LOGGER.warn("FTB Money getBalance returned unexpected type: {}", result.getClass());
            return BigDecimal.ZERO;
            
        } catch (Exception e) {
            LOGGER.error("Error getting balance from FTB Money for {}: {}", 
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
            // Use reflection to call FTB Money API
            moneyAPI.getClass()
                .getMethod("setBalance", java.util.UUID.class, double.class)
                .invoke(moneyAPI, player.getUUID(), amount.doubleValue());
            
            LOGGER.debug("Set FTB Money balance for {} to {}", 
                        player.getName().getString(), formatCurrency(amount));
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Error setting balance in FTB Money for {}: {}", 
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
                LOGGER.debug("Deposited {} to {} via FTB Money ({})", 
                            formatCurrency(amount), player.getName().getString(), reason);
            }
            return success;
            
        } catch (Exception e) {
            LOGGER.error("Error depositing to FTB Money for {}: {}", 
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
                LOGGER.debug("Insufficient FTB Money balance for {}: has {}, needs {}", 
                            player.getName().getString(), formatCurrency(currentBalance), formatCurrency(amount));
                return false;
            }
            
            BigDecimal newBalance = currentBalance.subtract(amount);
            boolean success = setBalance(player, newBalance);
            
            if (success) {
                LOGGER.debug("Withdrew {} from {} via FTB Money ({})", 
                            formatCurrency(amount), player.getName().getString(), reason);
            }
            return success;
            
        } catch (Exception e) {
            LOGGER.error("Error withdrawing from FTB Money for {}: {}", 
                        player.getName().getString(), e.getMessage());
            return false;
        }
    }
    
    @Override
    public String formatCurrency(BigDecimal amount) {
        if (formatter != null) {
            return "$" + formatter.format(amount);
        }
        return "$" + amount.toString();
    }
    
    @Override
    public String getCurrencySymbol() {
        return "$";
    }
    
    @Override
    public int getPriority() {
        return 20; // Medium priority
    }
}
