package com.zerog.neoessentials.api.examples;

import com.zerog.neoessentials.api.NeoEssentialsAPI;
import com.zerog.neoessentials.api.NeoEssentialsAPIFactory;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example integration showing how to use the NeoEssentials API
 * This demonstrates best practices for API usage
 */
public class APIExampleIntegration {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(APIExampleIntegration.class);
    
    /**
     * Example: Basic API usage
     */
    public static void handleBasicAPIOperations(ServerPlayer player) {
        try {
            // Check if NeoEssentials is ready
            if (!NeoEssentialsAPIFactory.isNeoEssentialsReady()) {
                LOGGER.warn("NeoEssentials not ready yet");
                return;
            }
            
            // Get the main API instance
            NeoEssentialsAPI api = NeoEssentialsAPI.getInstance();
            
            // Check API availability
            if (!NeoEssentialsAPI.isAvailable()) {
                LOGGER.warn("NeoEssentials API not available");
                return;
            }
            
            // Get API version information
            String apiVersion = NeoEssentialsAPI.getAPIVersion();
            String modVersion = NeoEssentialsAPI.getModVersion();
            
            LOGGER.info("Using NeoEssentials API v{} (Mod v{})", apiVersion, modVersion);
            
            // Check if API version meets requirements
            if (NeoEssentialsAPI.isAPIVersionAtLeast("2.1.0")) {
                LOGGER.info("API version meets requirements for enhanced features");
            }
            
            // Get API status
            var status = NeoEssentialsAPIFactory.getAPIStatus();
            LOGGER.info("API Status: {}", status);
            
        } catch (Exception e) {
            LOGGER.error("Error using NeoEssentials API", e);
        }
    }
    
    /**
     * Example: Provider management
     */
    public static void handleProviderOperations() {
        try {
            // Check what providers are available
            boolean economyAvailable = NeoEssentialsAPIFactory.isProviderAvailable(
                com.zerog.neoessentials.api.interfaces.IEconomyProvider.class);
            boolean playerDataAvailable = NeoEssentialsAPIFactory.isProviderAvailable(
                com.zerog.neoessentials.api.interfaces.IPlayerDataProvider.class);
            boolean placeholderAvailable = NeoEssentialsAPIFactory.isProviderAvailable(
                com.zerog.neoessentials.api.interfaces.IPlaceholderProvider.class);
            
            LOGGER.info("Provider availability - Economy: {}, PlayerData: {}, Placeholder: {}", 
                economyAvailable, playerDataAvailable, placeholderAvailable);
            
            // Get registered provider types
            Class<?>[] providerTypes = NeoEssentialsAPIFactory.getRegisteredProviderTypes();
            LOGGER.info("Registered provider types: {}", (Object) providerTypes);
            
        } catch (Exception e) {
            LOGGER.error("Error checking providers", e);
        }
    }
    
    /**
     * Example: Event system initialization
     */
    public static void initializeEventSystem() {
        try {
            // Get the main API instance
            NeoEssentialsAPI api = NeoEssentialsAPI.getInstance();
            
            // Initialize the event system
            api.initializeEventSystem();
            
            // Get event handler for custom event firing
            var eventHandler = api.getEventHandler();
            
            LOGGER.info("Event system initialized with handler: {}", 
                eventHandler.getClass().getSimpleName());
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize event system", e);
        }
    }
    
    /**
     * Example: API factory management
     */
    public static void handleFactoryOperations() {
        try {
            // Check if API factory is initialized
            boolean initialized = NeoEssentialsAPIFactory.isInitialized();
            LOGGER.info("API Factory initialized: {}", initialized);
            
            // Get comprehensive status information
            var status = NeoEssentialsAPIFactory.getAPIStatus();
            LOGGER.info("Complete API Status:");
            status.forEach((key, value) -> LOGGER.info("  {}: {}", key, value));
            
        } catch (Exception e) {
            LOGGER.error("Error with factory operations", e);
        }
    }
    
    /**
     * Example: Version checking and compatibility
     */
    public static void checkCompatibility() {
        try {
            String currentVersion = NeoEssentialsAPI.getAPIVersion();
            LOGGER.info("Current API version: {}", currentVersion);
            
            // Check if we have at least version 2.0.0
            if (NeoEssentialsAPI.isAPIVersionAtLeast("2.0.0")) {
                LOGGER.info("API supports basic features");
            }
            
            // Check if we have the latest version with enhanced features
            if (NeoEssentialsAPI.isAPIVersionAtLeast("2.1.0")) {
                LOGGER.info("API supports enhanced features (interfaces, events, factory)");
            }
            
            // Check specific version requirements
            if (NeoEssentialsAPI.isAPIVersionAtLeast("3.0.0")) {
                LOGGER.info("API supports future features");
            } else {
                LOGGER.info("API does not yet support future features");
            }
            
        } catch (Exception e) {
            LOGGER.error("Error checking compatibility", e);
        }
    }
    
    /**
     * Initialize the API integration
     * Call this from your mod's initialization
     */
    public static void initialize() {
        try {
            LOGGER.info("Initializing NeoEssentials API integration...");
            
            // Initialize the API factory first
            NeoEssentialsAPIFactory.initialize();
            
            // Basic API operations
            handleFactoryOperations();
            
            // Event system
            initializeEventSystem();
            
            // Provider checking
            handleProviderOperations();
            
            // Compatibility checking
            checkCompatibility();
            
            LOGGER.info("NeoEssentials API integration initialized successfully");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize NeoEssentials API integration", e);
        }
    }
    
    /**
     * Shutdown the API integration
     * Call this from your mod's shutdown
     */
    public static void shutdown() {
        try {
            LOGGER.info("Shutting down NeoEssentials API integration...");
            
            // Shutdown the API factory
            NeoEssentialsAPIFactory.shutdown();
            
            LOGGER.info("NeoEssentials API integration shutdown complete");
            
        } catch (Exception e) {
            LOGGER.error("Failed to shutdown NeoEssentials API integration", e);
        }
    }
}
