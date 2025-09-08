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
 * - Automatic detection and fallback support
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
    
    private ExternalEconomyManager() {
        this.configManager = ConfigManager.getInstance();
        this.integrations = new ConcurrentHashMap<>();
        this.enabledIntegrations = new HashSet<>();
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
    
    private void initializeIntegrations() {
        try {
            LOGGER.info("Initializing external economy integrations...");
            
            // Register all integrations
            registerIntegration(new FTBMoneyIntegration());
            registerIntegration(new MagicCoinsIntegration());
            registerIntegration(new LightmansCurrencyIntegration());
            registerIntegration(new CreatedCoinsIntegration());
            
            // Load configuration
            loadConfiguration();
            
            // Detect and activate the best available integration
            detectAndActivateIntegration();
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize external economy integrations: {}", e.getMessage());
            useExternalEconomy = false;
        }
    }
    
    private void registerIntegration(EconomyIntegration integration) {
        integrations.put(integration.getId(), integration);
        LOGGER.debug("Registered economy integration: {}", integration.getName());
    }
    
    private void loadConfiguration() {
        try {
            com.zerog.neoessentials.config.MainConfig mainConfig = configManager.getMainConfig();
            com.zerog.neoessentials.config.MainConfig.ExternalEconomySettings settings = 
                mainConfig.externalEconomySettings;
            
            useExternalEconomy = settings.enabled;
            
            // Load individual integration settings
            if (settings.ftbMoney) enabledIntegrations.add("ftb_money");
            if (settings.magicCoins) enabledIntegrations.add("magic_coins");
            if (settings.lightmansCurrency) enabledIntegrations.add("lightmans_currency");
            if (settings.createdCoins) enabledIntegrations.add("created_coins");
            
            LOGGER.info("Loaded external economy configuration: {} integrations enabled", 
                       enabledIntegrations.size());
            
        } catch (Exception e) {
            LOGGER.error("Failed to load external economy configuration: {}", e.getMessage());
            // Enable all integrations by default
            enabledIntegrations.addAll(integrations.keySet());
        }
    }
    
    private void detectAndActivateIntegration() {
        if (!useExternalEconomy) {
            LOGGER.info("External economy disabled in configuration");
            return;
        }
        
        List<EconomyIntegration> availableIntegrations = new ArrayList<>();
        
        for (EconomyIntegration integration : integrations.values()) {
            if (enabledIntegrations.contains(integration.getId()) && 
                integration.isAvailable() && 
                integration.initialize()) {
                availableIntegrations.add(integration);
                LOGGER.info("Available economy integration: {} (Priority: {})", 
                           integration.getName(), integration.getPriority());
            }
        }
        
        if (availableIntegrations.isEmpty()) {
            LOGGER.info("No external economy integrations available, using built-in economy");
            return;
        }
        
        // Sort by priority (lower number = higher priority)
        availableIntegrations.sort(Comparator.comparingInt(EconomyIntegration::getPriority));
        
        // Activate the highest priority integration
        activeIntegration = availableIntegrations.get(0);
        LOGGER.info("Activated external economy integration: {}", activeIntegration.getName());
    }
    
    // Public API Methods
    
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
                return false;
            }
        }
        return false;
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
                return false;
            }
        }
        return false;
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
                return false;
            }
        }
        return false;
    }
    
    /**
     * Check if player has sufficient balance
     */
    public boolean hasBalance(ServerPlayer player, BigDecimal amount) {
        BigDecimal balance = getBalance(player);
        return balance.compareTo(amount) >= 0;
    }
    
    /**
     * Format currency amount for display
     */
    public String formatCurrency(BigDecimal amount) {
        if (activeIntegration != null) {
            try {
                return activeIntegration.formatCurrency(amount);
            } catch (Exception e) {
                LOGGER.error("Error formatting currency with external economy: {}", e.getMessage());
            }
        }
        // Fallback to simple formatting
        return "$" + amount.toString();
    }
    
    /**
     * Get the name of the currently active economy
     */
    public String getActiveEconomyName() {
        if (activeIntegration != null) {
            return activeIntegration.getName();
        }
        return "Built-in Economy";
    }
    
    /**
     * Check if external economy is currently active
     */
    public boolean isExternalEconomyActive() {
        return activeIntegration != null && useExternalEconomy;
    }
    
    /**
     * Get status of all integrations for admin commands
     */
    public Map<String, String> getIntegrationStatus() {
        Map<String, String> status = new LinkedHashMap<>();
        
        for (EconomyIntegration integration : integrations.values()) {
            String integrationStatus;
            if (!enabledIntegrations.contains(integration.getId())) {
                integrationStatus = "Disabled in config";
            } else if (!integration.isAvailable()) {
                integrationStatus = "Mod not available";
            } else if (integration.equals(activeIntegration)) {
                integrationStatus = "Active";
            } else {
                integrationStatus = "Available but not active";
            }
            
            status.put(integration.getName(), integrationStatus);
        }
        
        return status;
    }
    
    /**
     * Reload configuration and redetect integrations
     */
    public void reload() {
        try {
            LOGGER.info("Reloading external economy manager...");
            
            // Shutdown current integration
            if (activeIntegration != null) {
                activeIntegration.shutdown();
                activeIntegration = null;
            }
            
            // Clear enabled integrations
            enabledIntegrations.clear();
            
            // Reload configuration
            loadConfiguration();
            
            // Redetect and activate integration
            detectAndActivateIntegration();
            
            LOGGER.info("External economy manager reloaded successfully");
            
        } catch (Exception e) {
            LOGGER.error("Failed to reload external economy manager: {}", e.getMessage());
        }
    }
    
    /**
     * Shutdown all integrations
     */
    public void shutdown() {
        try {
            LOGGER.info("Shutting down external economy manager...");
            
            if (activeIntegration != null) {
                activeIntegration.shutdown();
                activeIntegration = null;
            }
            
            for (EconomyIntegration integration : integrations.values()) {
                try {
                    integration.shutdown();
                } catch (Exception e) {
                    LOGGER.warn("Error shutting down integration {}: {}", 
                               integration.getName(), e.getMessage());
                }
            }
            
            integrations.clear();
            enabledIntegrations.clear();
            
            LOGGER.info("External economy manager shutdown completed");
            
        } catch (Exception e) {
            LOGGER.error("Error during external economy manager shutdown: {}", e.getMessage());
        }
    }
}
