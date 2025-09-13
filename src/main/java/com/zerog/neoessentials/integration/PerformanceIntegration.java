package com.zerog.neoessentials.integration;

import com.zerog.neoessentials.performance.PerformanceManager;
import com.zerog.neoessentials.performance.AsyncOperationManager;
// PerformanceCommand removed
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration class for performance monitoring systems
 * Handles initialization and coordination of performance-related components
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class PerformanceIntegration {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceIntegration.class);
    private static boolean initialized = false;
    
    /**
     * Initialize performance monitoring systems
     */
    public static void initialize() {
        if (initialized) {
            LOGGER.warn("Performance integration already initialized");
            return;
        }
        
        try {
            LOGGER.info("Initializing performance monitoring systems...");
            
            // Initialize performance manager
            PerformanceManager.getInstance();
            LOGGER.info("Performance manager initialized");
            
            // Initialize async operation manager
            AsyncOperationManager.getInstance();
            LOGGER.info("Async operation manager initialized");
            
            // Performance commands removed
            
            initialized = true;
            LOGGER.info("Performance integration initialization complete");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize performance integration", e);
            throw new RuntimeException("Performance integration initialization failed", e);
        }
    }
    
    /**
     * Shutdown performance monitoring systems
     */
    public static void shutdown() {
        if (!initialized) {
            return;
        }
        
        try {
            LOGGER.info("Shutting down performance monitoring systems...");
            
            // Shutdown async operations manager
            AsyncOperationManager.getInstance().shutdown();
            LOGGER.info("Async operation manager shutdown complete");
            
            // Shutdown performance manager
            PerformanceManager.getInstance().shutdown();
            LOGGER.info("Performance manager shutdown complete");
            
            initialized = false;
            LOGGER.info("Performance integration shutdown complete");
            
        } catch (Exception e) {
            LOGGER.error("Error during performance integration shutdown", e);
        }
    }
    
    /**
     * Check if performance integration is initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Get performance manager instance (convenience method)
     */
    public static PerformanceManager getPerformanceManager() {
        if (!initialized) {
            throw new IllegalStateException("Performance integration not initialized");
        }
        return PerformanceManager.getInstance();
    }
    
    /**
     * Get async operation manager instance (convenience method)
     */
    public static AsyncOperationManager getAsyncOperationManager() {
        if (!initialized) {
            throw new IllegalStateException("Performance integration not initialized");
        }
        return AsyncOperationManager.getInstance();
    }
    
    /**
     * Enable performance monitoring
     */
    public static void enableMonitoring() {
        if (initialized) {
            PerformanceManager.getInstance().setPerformanceMonitoring(true);
            LOGGER.info("Performance monitoring enabled");
        }
    }
    
    /**
     * Disable performance monitoring
     */
    public static void disableMonitoring() {
        if (initialized) {
            PerformanceManager.getInstance().setPerformanceMonitoring(false);
            LOGGER.info("Performance monitoring disabled");
        }
    }
    
    /**
     * Clear all performance caches
     */
    public static void clearCaches() {
        if (initialized) {
            PerformanceManager.getInstance().clearCache();
            LOGGER.info("Performance caches cleared");
        }
    }
    
    /**
     * Force garbage collection and log memory usage
     */
    public static void performMaintenanceGC() {
        if (initialized) {
            Runtime runtime = Runtime.getRuntime();
            long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
            
            System.gc();
            Thread.yield();
            
            long afterMemory = runtime.totalMemory() - runtime.freeMemory();
            long freedMemory = beforeMemory - afterMemory;
            
            LOGGER.info("Maintenance GC completed. Freed: {:.1f} MB", 
                freedMemory / 1024.0 / 1024.0);
        }
    }
    
    /**
     * Get current performance statistics summary
     */
    public static String getPerformanceSummary() {
        if (!initialized) {
            return "Performance monitoring not initialized";
        }
        
        try {
            PerformanceManager.PerformanceStats stats = PerformanceManager.getInstance().getPerformanceStats();
            Runtime runtime = Runtime.getRuntime();
            
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            double memoryUsageMB = usedMemory / 1024.0 / 1024.0;
            
            return String.format(
                "Performance Summary - Commands: %d, Avg Time: %.2fms, Memory: %.1fMB, Cache: %d entries",
                stats.getTotalCommands(),
                stats.getAverageCommandTime(),
                memoryUsageMB,
                stats.getCacheSize()
            );
            
        } catch (Exception e) {
            LOGGER.error("Error generating performance summary", e);
            return "Error generating performance summary: " + e.getMessage();
        }
    }
}
