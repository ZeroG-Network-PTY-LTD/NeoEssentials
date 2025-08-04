package com.zerog.neoessentials.analytics;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks usage statistics for specific features
 */
public class FeatureUsageStats {
    private final String featureName;
    private final AtomicLong totalUsage = new AtomicLong(0);
    private final ConcurrentHashMap<String, AtomicLong> actionCounts = new ConcurrentHashMap<>();
    private final LocalDateTime createdTime = LocalDateTime.now();
    private volatile LocalDateTime lastUsed = LocalDateTime.now();
    
    public FeatureUsageStats(String featureName) {
        this.featureName = featureName;
    }
    
    public void recordUsage(String action) {
        totalUsage.incrementAndGet();
        actionCounts.computeIfAbsent(action, k -> new AtomicLong(0)).incrementAndGet();
        lastUsed = LocalDateTime.now();
    }
    
    public long getUsageCount(String action) {
        AtomicLong count = actionCounts.get(action);
        return count != null ? count.get() : 0;
    }
    
    // Getters
    public String getFeatureName() { return featureName; }
    public long getTotalUsage() { return totalUsage.get(); }
    public ConcurrentHashMap<String, AtomicLong> getActionCounts() { return actionCounts; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public LocalDateTime getLastUsed() { return lastUsed; }
    
    @Override
    public String toString() {
        return String.format("FeatureUsageStats{feature='%s', totalUsage=%d, actions=%d, lastUsed=%s}", 
            featureName, getTotalUsage(), actionCounts.size(), lastUsed);
    }
}
