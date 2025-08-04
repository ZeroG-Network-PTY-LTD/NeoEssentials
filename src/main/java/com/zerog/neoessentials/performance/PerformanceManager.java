package com.zerog.neoessentials.performance;

import com.zerog.neoessentials.error.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance monitoring and optimization system for NeoEssentials
 * Provides memory management, command caching, and performance analytics
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class PerformanceManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceManager.class);
    private static PerformanceManager instance;
    
    // Performance monitoring
    private final ScheduledExecutorService scheduler;
    private final MemoryMXBean memoryBean;
    private final Map<String, Long> commandExecutionTimes;
    private final Map<String, AtomicLong> commandExecutionCounts;
    
    // Caching system
    private final Map<String, CacheEntry> cache;
    private final long cacheMaxSize;
    private final long cacheExpirationTime;
    
    // Performance metrics
    private final AtomicLong totalCommandsExecuted;
    private final AtomicLong totalExecutionTime;
    private volatile double averageCommandTime;
    private volatile boolean performanceMonitoringEnabled;
    private volatile long lastMemoryCheck;
    
    private PerformanceManager() {
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.commandExecutionTimes = new ConcurrentHashMap<>();
        this.commandExecutionCounts = new ConcurrentHashMap<>();
        this.cache = new ConcurrentHashMap<>();
        this.cacheMaxSize = 1000; // Maximum cache entries
        this.cacheExpirationTime = 300000; // 5 minutes in milliseconds
        this.totalCommandsExecuted = new AtomicLong(0);
        this.totalExecutionTime = new AtomicLong(0);
        this.performanceMonitoringEnabled = true;
        this.lastMemoryCheck = System.currentTimeMillis();
        
        startPerformanceMonitoring();
    }
    
    public static PerformanceManager getInstance() {
        if (instance == null) {
            instance = new PerformanceManager();
        }
        return instance;
    }
    
    /**
     * Start performance monitoring tasks
     */
    private void startPerformanceMonitoring() {
        // Memory monitoring every 30 seconds
        scheduler.scheduleAtFixedRate(this::checkMemoryUsage, 30, 30, TimeUnit.SECONDS);
        
        // Cache cleanup every 60 seconds
        scheduler.scheduleAtFixedRate(this::cleanupCache, 60, 60, TimeUnit.SECONDS);
        
        // Performance metrics calculation every 60 seconds
        scheduler.scheduleAtFixedRate(this::calculateMetrics, 60, 60, TimeUnit.SECONDS);
    }
    
    /**
     * Track command execution time
     */
    public void trackCommandExecution(String commandName, long executionTimeMs) {
        if (!performanceMonitoringEnabled) return;
        
        commandExecutionTimes.put(commandName + "_" + System.currentTimeMillis(), executionTimeMs);
        commandExecutionCounts.computeIfAbsent(commandName, k -> new AtomicLong(0)).incrementAndGet();
        totalCommandsExecuted.incrementAndGet();
        totalExecutionTime.addAndGet(executionTimeMs);
    }
    
    /**
     * Cache frequently accessed data
     */
    public void cacheData(String key, Object data) {
        if (cache.size() >= cacheMaxSize) {
            cleanupOldestCacheEntries();
        }
        
        cache.put(key, new CacheEntry(data, System.currentTimeMillis()));
    }
    
    /**
     * Retrieve cached data
     */
    public <T> T getCachedData(String key, Class<T> type) {
        CacheEntry entry = cache.get(key);
        if (entry == null) return null;
        
        // Check if cache entry is expired
        if (System.currentTimeMillis() - entry.timestamp > cacheExpirationTime) {
            cache.remove(key);
            return null;
        }
        
        try {
            return type.cast(entry.data);
        } catch (ClassCastException e) {
            LOGGER.warn("Cache type mismatch for key: {}", key);
            cache.remove(key);
            return null;
        }
    }
    
    /**
     * Check if data exists in cache
     */
    public boolean isCached(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) return false;
        
        if (System.currentTimeMillis() - entry.timestamp > cacheExpirationTime) {
            cache.remove(key);
            return false;
        }
        
        return true;
    }
    
    /**
     * Clear all cached data
     */
    public void clearCache() {
        cache.clear();
        LOGGER.info("Performance cache cleared");
    }
    
    /**
     * Get performance statistics
     */
    public PerformanceStats getPerformanceStats() {
        return new PerformanceStats(
            totalCommandsExecuted.get(),
            averageCommandTime,
            getMemoryUsagePercentage(),
            cache.size(),
            getTopSlowCommands(5),
            getMostUsedCommands(5),
            getSystemMetrics()
        );
    }
    
    /**
     * Monitor memory usage and trigger cleanup if needed
     */
    private void checkMemoryUsage() {
        try {
            MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
            long used = heapMemory.getUsed();
            long max = heapMemory.getMax();
            double usagePercentage = (double) used / max * 100;
            
            this.lastMemoryCheck = System.currentTimeMillis();
            
            // If memory usage is above 80%, trigger cleanup
            if (usagePercentage > 80.0) {
                LOGGER.warn("High memory usage detected: {:.2f}%. Triggering cleanup.", usagePercentage);
                performMemoryCleanup();
                
                // Notify admins if usage is critical (>90%)
                if (usagePercentage > 90.0) {
                    ErrorHandler.handleSystemError("Performance Manager", "critical memory usage", 
                        new RuntimeException("Memory usage: " + String.format("%.2f%%", usagePercentage)));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error checking memory usage", e);
        }
    }
    
    /**
     * Perform memory cleanup operations
     */
    private void performMemoryCleanup() {
        // Clear old cache entries
        cleanupCache();
        
        // Clear old command execution data (keep only last 1000 entries)
        if (commandExecutionTimes.size() > 1000) {
            commandExecutionTimes.entrySet().removeIf(entry -> 
                System.currentTimeMillis() - Long.parseLong(entry.getKey().split("_")[1]) > 300000);
        }
        
        // Suggest garbage collection
        System.gc();
        
        LOGGER.info("Memory cleanup performed");
    }
    
    /**
     * Cleanup expired cache entries
     */
    private void cleanupCache() {
        long currentTime = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().timestamp > cacheExpirationTime);
    }
    
    /**
     * Remove oldest cache entries when cache is full
     */
    private void cleanupOldestCacheEntries() {
        cache.entrySet().stream()
            .sorted(Map.Entry.<String, CacheEntry>comparingByValue((e1, e2) -> 
                Long.compare(e1.timestamp, e2.timestamp)))
            .limit(cacheMaxSize / 4) // Remove 25% of entries
            .map(Map.Entry::getKey)
            .forEach(cache::remove);
    }
    
    /**
     * Calculate performance metrics
     */
    private void calculateMetrics() {
        long totalCommands = totalCommandsExecuted.get();
        long totalTime = totalExecutionTime.get();
        
        if (totalCommands > 0) {
            averageCommandTime = (double) totalTime / totalCommands;
        }
    }
    
    /**
     * Get memory usage percentage
     */
    private double getMemoryUsagePercentage() {
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        return (double) heapMemory.getUsed() / heapMemory.getMax() * 100;
    }
    
    /**
     * Get top slowest commands
     */
    private Map<String, Double> getTopSlowCommands(int limit) {
        Map<String, Double> averageTimes = new HashMap<>();
        
        // Calculate average execution time per command
        for (Map.Entry<String, AtomicLong> entry : commandExecutionCounts.entrySet()) {
            String command = entry.getKey();
            long count = entry.getValue().get();
            
            long totalTimeForCommand = commandExecutionTimes.entrySet().stream()
                .filter(e -> e.getKey().startsWith(command + "_"))
                .mapToLong(Map.Entry::getValue)
                .sum();
            
            if (count > 0) {
                averageTimes.put(command, (double) totalTimeForCommand / count);
            }
        }
        
        return averageTimes.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(limit)
            .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
    }
    
    /**
     * Get most frequently used commands
     */
    private Map<String, Long> getMostUsedCommands(int limit) {
        return commandExecutionCounts.entrySet().stream()
            .sorted(Map.Entry.<String, AtomicLong>comparingByValue((a, b) -> 
                Long.compare(b.get(), a.get())))
            .limit(limit)
            .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().get()), HashMap::putAll);
    }
    
    /**
     * Get system metrics
     */
    private Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Memory metrics
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        metrics.put("heapUsed", heapMemory.getUsed() / 1024 / 1024); // MB
        metrics.put("heapMax", heapMemory.getMax() / 1024 / 1024); // MB
        metrics.put("heapUsagePercent", getMemoryUsagePercentage());
        
        // GC metrics
        long totalGcTime = ManagementFactory.getGarbageCollectorMXBeans().stream()
            .mapToLong(GarbageCollectorMXBean::getCollectionTime)
            .sum();
        metrics.put("totalGcTime", totalGcTime);
        
        // Cache metrics
        metrics.put("cacheSize", cache.size());
        metrics.put("cacheMaxSize", cacheMaxSize);
        
        // Performance metrics
        metrics.put("totalCommands", totalCommandsExecuted.get());
        metrics.put("averageCommandTime", averageCommandTime);
        
        return metrics;
    }
    
    /**
     * Enable or disable performance monitoring
     */
    public void setPerformanceMonitoring(boolean enabled) {
        this.performanceMonitoringEnabled = enabled;
        LOGGER.info("Performance monitoring {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Shutdown performance manager
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        LOGGER.info("Performance Manager shutdown complete");
    }
    
    /**
     * Cache entry wrapper
     */
    private static class CacheEntry {
        final Object data;
        final long timestamp;
        
        CacheEntry(Object data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * Performance statistics container
     */
    public static class PerformanceStats {
        private final long totalCommands;
        private final double averageCommandTime;
        private final double memoryUsage;
        private final int cacheSize;
        private final Map<String, Double> slowestCommands;
        private final Map<String, Long> mostUsedCommands;
        private final Map<String, Object> systemMetrics;
        
        public PerformanceStats(long totalCommands, double averageCommandTime, double memoryUsage,
                int cacheSize, Map<String, Double> slowestCommands, Map<String, Long> mostUsedCommands,
                Map<String, Object> systemMetrics) {
            this.totalCommands = totalCommands;
            this.averageCommandTime = averageCommandTime;
            this.memoryUsage = memoryUsage;
            this.cacheSize = cacheSize;
            this.slowestCommands = slowestCommands;
            this.mostUsedCommands = mostUsedCommands;
            this.systemMetrics = systemMetrics;
        }
        
        // Getters
        public long getTotalCommands() { return totalCommands; }
        public double getAverageCommandTime() { return averageCommandTime; }
        public double getMemoryUsage() { return memoryUsage; }
        public int getCacheSize() { return cacheSize; }
        public Map<String, Double> getSlowestCommands() { return slowestCommands; }
        public Map<String, Long> getMostUsedCommands() { return mostUsedCommands; }
        public Map<String, Object> getSystemMetrics() { return systemMetrics; }
    }
}
