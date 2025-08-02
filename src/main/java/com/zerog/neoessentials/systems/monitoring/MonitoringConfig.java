package com.zerog.neoessentials.systems.monitoring;

/**
 * Configuration class for monitoring system
 */
public class MonitoringConfig {
    public boolean enabled = true;
    public int alertThreshold = 80;
    public long metricsInterval = 30000; // 30 seconds
    public boolean detailedLogging = false;
    public int historyRetentionDays = 7;
    public int collectionInterval = 5000; // 5 seconds
    public int metricRetentionHours = 24;
    public boolean autoOptimizationEnabled = false;
    public double memoryThreshold = 85.0;
    public double cpuThreshold = 80.0;
    public double diskThreshold = 90.0;
    
    public MonitoringConfig() {
        // Default configuration
    }
    
    // Getter methods
    public int getCollectionInterval() {
        return collectionInterval;
    }
    
    public int getMetricRetentionHours() {
        return metricRetentionHours;
    }
    
    public boolean isAutoOptimizationEnabled() {
        return autoOptimizationEnabled;
    }
    
    public double getMemoryThreshold() {
        return memoryThreshold;
    }
    
    public double getCpuThreshold() {
        return cpuThreshold;
    }
    
    public double getDiskThreshold() {
        return diskThreshold;
    }
    
    // Setter methods
    public void setCollectionInterval(int collectionInterval) {
        this.collectionInterval = collectionInterval;
    }
    
    public void setMetricRetentionHours(int metricRetentionHours) {
        this.metricRetentionHours = metricRetentionHours;
    }
    
    public void setAutoOptimizationEnabled(boolean autoOptimizationEnabled) {
        this.autoOptimizationEnabled = autoOptimizationEnabled;
    }
    
    public void setMemoryThreshold(double memoryThreshold) {
        this.memoryThreshold = memoryThreshold;
    }
    
    public void setCpuThreshold(double cpuThreshold) {
        this.cpuThreshold = cpuThreshold;
    }
    
    public void setDiskThreshold(double diskThreshold) {
        this.diskThreshold = diskThreshold;
    }
}
