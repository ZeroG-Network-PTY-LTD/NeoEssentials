package com.zerog.neoessentials.performance;

import com.zerog.neoessentials.integration.ErrorHandlingIntegration;
import net.minecraft.commands.CommandSourceStack;

/**
 * Performance-aware command wrapper
 * Automatically tracks execution time and provides caching capabilities
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class PerformanceCommandWrapper {
    
    /**
     * Execute command with performance tracking
     */
    public static int executeWithTracking(CommandSourceStack source, String commandName, 
            ErrorHandlingIntegration.ThrowingCommandFunction commandLogic) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            int result = ErrorHandlingIntegration.executeCommand(source, commandName, commandLogic);
            
            long executionTime = System.currentTimeMillis() - startTime;
            PerformanceManager.getInstance().trackCommandExecution(commandName, executionTime);
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            PerformanceManager.getInstance().trackCommandExecution(commandName + "_ERROR", executionTime);
            throw e;
        }
    }
    
    /**
     * Execute command with caching support
     */
    public static <T> T executeWithCaching(CommandSourceStack source, String commandName, String cacheKey,
            java.util.function.Supplier<T> commandLogic, Class<T> returnType, T defaultValue) {
        
        // Check cache first
        T cachedResult = PerformanceManager.getInstance().getCachedData(cacheKey, returnType);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            T result = commandLogic.get();
            
            // Cache the result
            PerformanceManager.getInstance().cacheData(cacheKey, result);
            
            long executionTime = System.currentTimeMillis() - startTime;
            PerformanceManager.getInstance().trackCommandExecution(commandName, executionTime);
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            PerformanceManager.getInstance().trackCommandExecution(commandName + "_ERROR", executionTime);
            return defaultValue;
        }
    }
    
    /**
     * Create cache key for player-specific data
     */
    public static String createPlayerCacheKey(String commandName, String playerName) {
        return String.format("player_%s_%s", commandName, playerName);
    }
    
    /**
     * Create cache key for server-wide data
     */
    public static String createServerCacheKey(String commandName) {
        return String.format("server_%s", commandName);
    }
    
    /**
     * Create cache key with expiration hint
     */
    public static String createCacheKey(String category, String identifier, String hint) {
        return String.format("%s_%s_%s", category, identifier, hint);
    }
}
