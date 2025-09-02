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
import java.util.concurrent.atomic.LongAdder;
import java.util.LinkedHashMap;

/**
 * Performance monitoring and optimization system for NeoEssentials
 * Provides memory management, command caching, and performance analytics
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class PerformanceManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceManager.class);
    private static volatile PerformanceManager instance;
    
    // Performance monitoring
    private final ScheduledExecutorService scheduler;
    private final MemoryMXBean memoryBean;
    
    // Optimized command tracking - using LongAdder for better performance under contention
    private final Map<String, LongAdder> commandExecutionCounts;
    private final Map<String, LongAdder> commandExecutionTimes;
    
    // Optimized LRU cache with better memory usage
    private final Map<String, CacheEntry> cache;
    private final int cacheMaxSize;
    private final long cacheExpirationTime;
    
    // Performance metrics - using LongAdder for better concurrent performance
    private final LongAdder totalCommandsExecuted;
    private final LongAdder totalExecutionTime;
    private volatile double averageCommandTime;
    private volatile boolean performanceMonitoringEnabled;
    
    // Memory optimization: Reuse objects
    private final ThreadLocal<StringBuilder> stringBuilder = ThreadLocal.withInitial(() -> new StringBuilder(256));
    private volatile long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL = 60000; // 1 minute
    
    private PerformanceManager() {
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        
        // Initialize optimized collections
        this.commandExecutionTimes = new ConcurrentHashMap<>();
        this.commandExecutionCounts = new ConcurrentHashMap<>();
        
        // Initialize LRU cache with memory optimization
        this.cacheMaxSize = 1000; // Limit cache size
        this.cacheExpirationTime = 300000; // 5 minutes
        this.cache = new LinkedHashMap<String, CacheEntry>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > cacheMaxSize || 
                       System.currentTimeMillis() - eldest.getValue().timestamp > cacheExpirationTime;
            }
        };
        
        // Initialize performance counters with better concurrency
        this.totalCommandsExecuted = new LongAdder();
        this.totalExecutionTime = new LongAdder();
        this.performanceMonitoringEnabled = true;
        
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
     * Track command execution time - optimized for memory and concurrency
     */
    public void trackCommandExecution(String commandName, long executionTimeMs) {
        if (!performanceMonitoringEnabled) return;
        
        // Store execution time efficiently using LongAdder
        LongAdder timeAdder = commandExecutionTimes.computeIfAbsent(commandName, k -> new LongAdder());
        timeAdder.add(executionTimeMs);
        
        // Increment command count
        LongAdder countAdder = commandExecutionCounts.computeIfAbsent(commandName, k -> new LongAdder());
        countAdder.increment();
        
        // Update totals efficiently
        totalCommandsExecuted.increment();
        totalExecutionTime.add(executionTimeMs);
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
        com.zerog.neoessentials.util.DebugUtil.debugLog("Performance cache cleared");
    }
    
    /**
     * Get performance statistics
     */
    public PerformanceStats getPerformanceStats() {
        return new PerformanceStats(
            totalCommandsExecuted.sum(),
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
        
        com.zerog.neoessentials.util.DebugUtil.debugLog("Memory cleanup performed");
    }
    
    /**
     * Cleanup expired cache entries with optimized timing
     */
    private void cleanupCache() {
        long currentTime = System.currentTimeMillis();
        
        // Only cleanup if enough time has passed (rate limiting)
        if (currentTime - lastCleanupTime < CLEANUP_INTERVAL) {
            return;
        }
        
        // Use StringBuilder for efficient string operations
        StringBuilder logBuilder = stringBuilder.get();
        logBuilder.setLength(0); // Reset
        
        cache.entrySet().removeIf(entry -> {
            boolean expired = currentTime - entry.getValue().timestamp > cacheExpirationTime;
            if (expired) {
                if (logBuilder.length() == 0) {
                    logBuilder.append("Cleaned expired cache entries: ");
                }
                logBuilder.append(entry.getKey()).append(" ");
            }
            return expired;
        });
        
        lastCleanupTime = currentTime;
        
        if (logBuilder.length() > 0) {
            LOGGER.debug(logBuilder.toString());
        }
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
        long totalCommands = totalCommandsExecuted.sum();
        long totalTime = totalExecutionTime.sum();
        
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
        for (Map.Entry<String, LongAdder> entry : commandExecutionCounts.entrySet()) {
            String command = entry.getKey();
            long count = entry.getValue().sum();
            
            // Get total time for this command
            LongAdder timeAdder = commandExecutionTimes.get(command);
            if (timeAdder != null && count > 0) {
                long totalTimeForCommand = timeAdder.sum();
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
            .sorted(Map.Entry.<String, LongAdder>comparingByValue((a, b) -> 
                Long.compare(b.sum(), a.sum())))
            .limit(limit)
            .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().sum()), HashMap::putAll);
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
        metrics.put("totalCommands", totalCommandsExecuted.sum());
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
