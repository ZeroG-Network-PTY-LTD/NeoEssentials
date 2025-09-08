package com.zerog.neoessentials.managers;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.localization.LanguageManager;
import com.zerog.neoessentials.util.DebugUtil;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Unified Feature Manager - Coordinates all NeoEssentials features and managers
 * 
 * This manager ensures proper initialization order, cross-feature integration,
 * and provides a centralized interface for feature management.
 * 
 * @author ZeroG
 * @version 2.0.0
 */
public class FeatureManager {
    private static FeatureManager instance;
    private final Map<String, Object> managers = new ConcurrentHashMap<>();
    private boolean initialized = false;
    
    private FeatureManager() {}
    
    public static FeatureManager getInstance() {
        if (instance == null) {
            synchronized (FeatureManager.class) {
                if (instance == null) {
                    instance = new FeatureManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize all features in the proper order with dependency management
     */
    public void initializeFeatures() {
        if (initialized) {
            DebugUtil.debugLog("FeatureManager already initialized");
            return;
        }
        
        DebugUtil.debugLog("Starting feature initialization sequence...");
        
        try {
            // Phase 1: Core Configuration and Storage
            initializeCoreServices();
            
            // Phase 2: Data Management
            initializeDataManagers();
            
            // Phase 3: Player Features
            initializePlayerFeatures();
            
            // Phase 4: Server Features
            initializeServerFeatures();
            
            // Phase 5: Integration Features
            initializeIntegrationFeatures();
            
            initialized = true;
            DebugUtil.debugLog("All features initialized successfully");
            
        } catch (Exception e) {
            DebugUtil.warnLog("Error during feature initialization: " + e.getMessage());
            throw new RuntimeException("Failed to initialize NeoEssentials features", e);
        }
    }
    
    /**
     * Phase 1: Initialize core services that other features depend on
     */
    private void initializeCoreServices() {
        DebugUtil.debugLog("Phase 1: Initializing core services...");
        
        // Configuration system (should already be initialized)
        ConfigManager configManager = ConfigManager.getInstance();
        managers.put("config", configManager);
        
        // Language system - initialize with config path
        LanguageManager languageManager = LanguageManager.getInstance(configManager.getConfigPath());
        managers.put("language", languageManager);
        
        // Player data management
        com.zerog.neoessentials.player.PlayerDataManager playerDataManager = 
            com.zerog.neoessentials.player.PlayerDataManager.getInstance();
        managers.put("playerData", playerDataManager);
        
        // Storage system
        com.zerog.neoessentials.storage.StorageManager storageManager = 
            com.zerog.neoessentials.storage.StorageManager.getInstance();
        managers.put("storage", storageManager);
        
        DebugUtil.debugLog("Core services initialized");
    }
    
    /**
     * Phase 2: Initialize data management features
     */
    private void initializeDataManagers() {
        DebugUtil.debugLog("Phase 2: Initializing data managers...");
        
        // Permission system
        com.zerog.neoessentials.permissions.CustomPermissionsManager permissionManager = 
            com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance();
        managers.put("permissions", permissionManager);
        
        // Placeholder system
        com.zerog.neoessentials.placeholders.PlaceholderManager placeholderManager = 
            com.zerog.neoessentials.placeholders.PlaceholderManager.getInstance();
        managers.put("placeholders", placeholderManager);
        
        // Kit storage system
        com.zerog.neoessentials.storage.KitStorageManager kitStorageManager = 
            com.zerog.neoessentials.storage.KitStorageManager.getInstance();
        managers.put("kitStorage", kitStorageManager);
        
        // Performance monitoring
        com.zerog.neoessentials.performance.PerformanceManager performanceManager = 
            com.zerog.neoessentials.performance.PerformanceManager.getInstance();
        managers.put("performance", performanceManager);
        
        DebugUtil.debugLog("Data managers initialized");
    }
    
    /**
     * Phase 3: Initialize player-focused features
     */
    private void initializePlayerFeatures() {
        DebugUtil.debugLog("Phase 3: Initializing player features...");
        
        // Economy system
        EconomyManager economyManager = EconomyManager.getInstance();
        managers.put("economy", economyManager);
        
        // Home system
        HomeManager homeManager = HomeManager.getInstance();
        managers.put("homes", homeManager);
        
        // Warp system
        WarpManager warpManager = WarpManager.getInstance();
        managers.put("warps", warpManager);
        
        // Kit system
        KitManager kitManager = KitManager.getInstance();
        managers.put("kits", kitManager);
        
        // Spawn management
        SpawnManager spawnManager = SpawnManager.getInstance();
        managers.put("spawn", spawnManager);
        
        // Teleport requests
        TeleportRequestManager teleportManager = TeleportRequestManager.getInstance();
        managers.put("teleports", teleportManager);
        
        // Player social features
        MessagingManager messagingManager = MessagingManager.getInstance();
        managers.put("messaging", messagingManager);
        
        NickManager nickManager = NickManager.get();
        managers.put("nicknames", nickManager);
        
        AFKManager afkManager = AFKManager.getInstance();
        managers.put("afk", afkManager);
        
        IgnoreManager ignoreManager = IgnoreManager.getInstance();
        managers.put("ignore", ignoreManager);
        
        // Note: SocialSpyManager is a utility class, not a singleton
        managers.put("socialSpy", "Utility class");
        
        LastSeenManager lastSeenManager = LastSeenManager.getInstance();
        managers.put("lastSeen", lastSeenManager);
        
        DebugUtil.debugLog("Player features initialized");
    }
    
    /**
     * Phase 4: Initialize server administration features
     */
    private void initializeServerFeatures() {
        DebugUtil.debugLog("Phase 4: Initializing server features...");
        
        // Moderation tools
        ModerationManager moderationManager = ModerationManager.getInstance();
        managers.put("moderation", moderationManager);
        
        // Shop system
        com.zerog.neoessentials.economy.shops.ShopManager shopManager = 
            com.zerog.neoessentials.economy.shops.ShopManager.getInstance();
        managers.put("shops", shopManager);
        
        // Tablist and UI features (using HeaderFooterManager for tablist functionality)
        com.zerog.neoessentials.features.TabListManager headerFooterManager = 
            new com.zerog.neoessentials.features.TabListManager();
        managers.put("headerFooter", headerFooterManager);
        
        // Bossbar system removed - keeping only tablist functionality
        
        // Web dashboard
        WebDashboardManager webDashboardManager = WebDashboardManager.getInstance();
        managers.put("webDashboard", webDashboardManager);
        
        DebugUtil.debugLog("Server features initialized");
    }
    
    /**
     * Phase 5: Initialize integration and compatibility features
     */
    private void initializeIntegrationFeatures() {
        DebugUtil.debugLog("Phase 5: Initializing integration features...");
        
        // Plugin compatibility
        PluginCompatibilityManager compatibilityManager = PluginCompatibilityManager.getInstance();
        compatibilityManager.initialize();
        managers.put("compatibility", compatibilityManager);
        
        // Start animated placeholders
        com.zerog.neoessentials.placeholders.PlaceholderManager placeholderManager = 
            (com.zerog.neoessentials.placeholders.PlaceholderManager) managers.get("placeholders");
        if (placeholderManager != null) {
            placeholderManager.startAnimatedPlaceholderRefreshForAll();
            DebugUtil.debugLog("Animated placeholders started");
        }
        
        DebugUtil.debugLog("Integration features initialized");
    }
    
    /**
     * Get a specific manager by name
     */
    @SuppressWarnings("unchecked")
    public <T> T getManager(String name, Class<T> type) {
        Object manager = managers.get(name);
        if (manager != null && type.isInstance(manager)) {
            return (T) manager;
        }
        return null;
    }
    
    /**
     * Check if all features are initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Get feature status for debugging
     */
    public Map<String, String> getFeatureStatus() {
        Map<String, String> status = new ConcurrentHashMap<>();
        managers.forEach((name, manager) -> {
            if (manager != null) {
                status.put(name, "Initialized (" + manager.getClass().getSimpleName() + ")");
            } else {
                status.put(name, "Failed to initialize");
            }
        });
        return status;
    }
    
    /**
     * Gracefully shutdown all features
     */
    public CompletableFuture<Void> shutdown() {
        DebugUtil.debugLog("Shutting down all features...");
        
        return CompletableFuture.runAsync(() -> {
            try {
                // Save player data
                com.zerog.neoessentials.player.PlayerDataManager playerDataManager = 
                    getManager("playerData", com.zerog.neoessentials.player.PlayerDataManager.class);
                if (playerDataManager != null) {
                    playerDataManager.saveAllPlayerData();
                }
                
                // Stop performance monitoring
                com.zerog.neoessentials.performance.PerformanceManager performanceManager = 
                    getManager("performance", com.zerog.neoessentials.performance.PerformanceManager.class);
                if (performanceManager != null) {
                    performanceManager.shutdown();
                }
                
                // Clear managers
                managers.clear();
                initialized = false;
                
                DebugUtil.debugLog("All features shut down successfully");
                
            } catch (Exception e) {
                DebugUtil.warnLog("Error during feature shutdown: " + e.getMessage());
            }
        });
    }
}
