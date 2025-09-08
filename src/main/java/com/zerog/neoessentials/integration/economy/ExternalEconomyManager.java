package com.zerog.neoessentials.integration.economy;

import com.zerog.neoessentials.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * External Economy Integration Manager
 * Provides comprehensive integration with popular economy mods:
 * - FTB Money Forge
 * - Magic Coins  
 * - Lightman's Currency
 * - Created Coins
 * - Automatic detection and fallback to built-in economy
 * - Configurable enable/disable per integration
 * 
 * @author ZeroG
 * @since 2.0.2
 */
public class ExternalEconomyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalEconomyManager.class);
    private static volatile ExternalEconomyManager instance;
    
    // Available integrations
    private final Map<String, EconomyIntegration> integrations;
    private final Set<String> enabledIntegrations;
    private EconomyIntegration activeIntegration;
    
    // Configuration
    private final ConfigManager configManager;
    private boolean useExternalEconomy;
    
    // Fallback handling (removed circular dependency)
    private boolean fallbackToBuiltinEnabled;
    
    private ExternalEconomyManager() {
        this.configManager = ConfigManager.getInstance();
        this.integrations = new ConcurrentHashMap<>();
        this.enabledIntegrations = new HashSet<>();
        this.fallbackToBuiltinEnabled = true;
        this.useExternalEconomy = true;
        
        initializeIntegrations();
    }
    
    public static ExternalEconomyManager getInstance() {
        if (instance == null) {
            synchronized (ExternalEconomyManager.class) {
                if (instance == null) {
                    instance = new ExternalEconomyManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize all available economy integrations
     */
    private void initializeIntegrations() {
        LOGGER.info("Initializing external economy integrations...");
        
        // Register integrations
        registerIntegration(new FTBMoneyIntegration());
        registerIntegration(new MagicCoinsIntegration());
        registerIntegration(new LightmansCurrencyIntegration());
        registerIntegration(new CreatedCoinsIntegration());
        
        // Load configuration
        loadConfiguration();
        
        // Detect and activate best available integration
        detectAndActivateIntegration();
        
        LOGGER.info("External economy manager initialized - Active: {}", 
                   activeIntegration != null ? activeIntegration.getName() : "Built-in");
    }
    
    /**
     * Register an economy integration
     */
    private void registerIntegration(EconomyIntegration integration) {
        try {
            integrations.put(integration.getId(), integration);
            LOGGER.debug("Registered economy integration: {}", integration.getName());
        } catch (Exception e) {
            LOGGER.error("Failed to register integration {}: {}", integration.getName(), e.getMessage());
        }
    }
    
    /**
     * Load configuration settings
     */
    private void loadConfiguration() {
        var economyConfig = configManager.getMainConfig().externalEconomySettings;
        
        this.useExternalEconomy = economyConfig.enabled;
        this.enabledIntegrations.clear();
        
        if (economyConfig.ftbMoney) enabledIntegrations.add("ftb_money");
        if (economyConfig.magicCoins) enabledIntegrations.add("magic_coins");
        if (economyConfig.lightmansCurrency) enabledIntegrations.add("lightmans_currency");
        if (economyConfig.createdCoins) enabledIntegrations.add("created_coins");
        
        LOGGER.info("External economy enabled: {}, Active integrations: {}", 
                   useExternalEconomy, enabledIntegrations);
    }
    
    /**
     * Detect and activate the best available integration
     */
    private void detectAndActivateIntegration() {
        if (!useExternalEconomy) {
            LOGGER.info("External economy disabled - using built-in economy system");
            return;
        }
        
        // Priority order for integration selection
        String[] priorityOrder = {"lightmans_currency", "ftb_money", "magic_coins", "created_coins"};
        
        for (String integrationId : priorityOrder) {
            if (enabledIntegrations.contains(integrationId)) {
                EconomyIntegration integration = integrations.get(integrationId);
                if (integration != null && integration.isAvailable()) {
                    try {
                        if (integration.initialize()) {
                            activeIntegration = integration;
                            LOGGER.info("Successfully activated economy integration: {}", integration.getName());
                            return;
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to initialize integration {}: {}", integration.getName(), e.getMessage());
                    }
                }
            }
        }
        
        LOGGER.warn("No external economy integrations available - using built-in economy system");
    }
    
    /**
     * Get player's balance
     */
    public BigDecimal getBalance(ServerPlayer player) {
        if (activeIntegration != null) {
            try {
                return activeIntegration.getBalance(player);
            } catch (Exception e) {
                LOGGER.error("Error getting balance from external economy for {}: {}", 
                            player.getName().getString(), e.getMessage());
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Set player's balance
     */
    public boolean setBalance(ServerPlayer player, BigDecimal amount) {
        if (activeIntegration != null) {
            try {
                return activeIntegration.setBalance(player, amount);
            } catch (Exception e) {
                LOGGER.error("Error setting balance in external economy for {}: {}", 
                            player.getName().getString(), e.getMessage());
                fallbackToBuiltin().setBalance(player.getUUID(), amount);
                return false;
            }
        }
        fallbackToBuiltin().setBalance(player.getUUID(), amount);
        return true;
    }
    
    /**
     * Add to player's balance
     */
    public boolean depositBalance(ServerPlayer player, BigDecimal amount, String reason) {
        if (activeIntegration != null) {
            try {
                return activeIntegration.depositBalance(player, amount, reason);
            } catch (Exception e) {
                LOGGER.error("Error depositing to external economy for {}: {}", 
                            player.getName().getString(), e.getMessage());
                return fallbackToBuiltin().depositBalance(player.getUUID(), amount, reason);
            }
        }
        return fallbackToBuiltin().depositBalance(player.getUUID(), amount, reason);
    }
    
    /**
     * Remove from player's balance
     */
    public boolean withdrawBalance(ServerPlayer player, BigDecimal amount, String reason) {
        if (activeIntegration != null) {
            try {
                return activeIntegration.withdrawBalance(player, amount, reason);
            } catch (Exception e) {
                LOGGER.error("Error withdrawing from external economy for {}: {}", 
                            player.getName().getString(), e.getMessage());
                return fallbackToBuiltin().withdrawBalance(player.getUUID(), amount, reason);
            }
        }
        return fallbackToBuiltin().withdrawBalance(player.getUUID(), amount, reason);
    }
    
    /**
     * Check if player has sufficient balance
     */
    public boolean hasBalance(ServerPlayer player, BigDecimal amount) {
        BigDecimal balance = getBalance(player);
        return balance.compareTo(amount) >= 0;
    }
    
    /**
     * Format currency for display
     */
    public String formatCurrency(BigDecimal amount) {
        if (activeIntegration != null) {
            try {
                return activeIntegration.formatCurrency(amount);
            } catch (Exception e) {
                LOGGER.error("Error formatting currency from external economy: {}", e.getMessage());
            }
        }
        return fallbackToBuiltin().formatCurrency(amount);
    }
    
    /**
     * Get the name of the active economy system
     */
    public String getActiveEconomyName() {
        if (activeIntegration != null) {
            return activeIntegration.getName();
        }
        return "NeoEssentials Built-in Economy";
    }
    
    /**
     * Check if external economy is active
     */
    public boolean isExternalEconomyActive() {
        return activeIntegration != null;
    }
    
    /**
     * Get available integrations for status display
     */
    public Map<String, String> getIntegrationStatus() {
        Map<String, String> status = new LinkedHashMap<>();
        for (Map.Entry<String, EconomyIntegration> entry : integrations.entrySet()) {
            EconomyIntegration integration = entry.getValue();
            String integrationStatus = "Disabled";
            
            if (enabledIntegrations.contains(entry.getKey())) {
                if (integration.isAvailable()) {
                    integrationStatus = integration == activeIntegration ? "Active" : "Available";
                } else {
                    integrationStatus = "Mod Not Found";
                }
            }
            
            status.put(integration.getName(), integrationStatus);
        }
        return status;
    }
    
    /**
     * Fallback to built-in economy system
     */
    private EconomyManager fallbackToBuiltin() {
        return builtinEconomy;
    }
    
    /**
     * Reload configuration and reinitialize
     */
    public void reload() {
        LOGGER.info("Reloading external economy configuration...");
        loadConfiguration();
        
        // Shutdown current integration
        if (activeIntegration != null) {
            try {
                activeIntegration.shutdown();
            } catch (Exception e) {
                LOGGER.error("Error shutting down current integration: {}", e.getMessage());
            }
            activeIntegration = null;
        }
        
        // Redetect and activate
        detectAndActivateIntegration();
        LOGGER.info("External economy reloaded - Active: {}", 
                   activeIntegration != null ? activeIntegration.getName() : "Built-in");
    }
    
    /**
     * Shutdown all integrations
     */
    public void shutdown() {
        LOGGER.info("Shutting down external economy manager...");
        
        if (activeIntegration != null) {
            try {
                activeIntegration.shutdown();
            } catch (Exception e) {
                LOGGER.error("Error shutting down integration {}: {}", 
                            activeIntegration.getName(), e.getMessage());
            }
        }
        
        integrations.clear();
        enabledIntegrations.clear();
        activeIntegration = null;
        
        LOGGER.info("External economy manager shutdown completed");
    }
}
