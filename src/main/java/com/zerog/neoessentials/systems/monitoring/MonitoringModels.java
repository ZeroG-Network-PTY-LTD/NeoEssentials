package com.zerog.neoessentials.systems.monitoring;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * System snapshot for performance monitoring
 */
public class SystemSnapshot {
    public LocalDateTime timestamp;
    
    // Memory metrics
    public long heapUsed;
    public long heapMax;
    public long heapCommitted;
    public long nonHeapUsed;
    public long nonHeapMax;
    public long totalMemory;
    public long freeMemory;
    public long maxMemory;
    public double memoryUsagePercent;
    public double heapUsagePercent;
    
    // Thread metrics
    public int threadCount;
    public int peakThreadCount;
    public int daemonThreadCount;
    
    // System metrics
    public int availableProcessors;
    public double systemLoadAverage;
    
    // GC metrics
    public long gcCollections;
    public long gcTime;
}

/**
 * Time series data for metrics
 */
public class MetricTimeSeries {
    private final String name;
    private final List<DataPoint> dataPoints = new CopyOnWriteArrayList<>();
    
    public MetricTimeSeries(String name) {
        this.name = name;
    }
    
    public void addDataPoint(double value, LocalDateTime timestamp) {
        dataPoints.add(new DataPoint(value, timestamp));
        
        // Keep only last 1000 points
        if (dataPoints.size() > 1000) {
            dataPoints.subList(0, dataPoints.size() - 1000).clear();
        }
    }
    
    public void cleanup(LocalDateTime cutoff) {
        dataPoints.removeIf(point -> point.timestamp.isBefore(cutoff));
    }
    
    public List<DataPoint> getDataPoints() {
        return new ArrayList<>(dataPoints);
    }
    
    public String getName() { return name; }
    
    public static class DataPoint {
        public final double value;
        public final LocalDateTime timestamp;
        
        public DataPoint(double value, LocalDateTime timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}

/**
 * Performance alert types
 */
public enum AlertType {
    HIGH_MEMORY_USAGE,
    HIGH_CPU_USAGE,
    HIGH_THREAD_COUNT,
    DISK_SPACE_LOW,
    NETWORK_LATENCY_HIGH,
    GARBAGE_COLLECTION_EXCESSIVE,
    SYSTEM_OVERLOAD,
    MEMORY_LEAK_DETECTED,
    DEADLOCK_DETECTED
}

/**
 * Alert severity levels
 */
public enum AlertSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

/**
 * Performance alert
 */
public class PerformanceAlert {
    private final String id = UUID.randomUUID().toString();
    private final AlertType type;
    private final AlertSeverity severity;
    private final String message;
    private final double metricValue;
    private final double threshold;
    private final LocalDateTime timestamp;
    private boolean processed = false;
    
    public PerformanceAlert(AlertType type, AlertSeverity severity, String message,
                           double metricValue, double threshold, LocalDateTime timestamp) {
        this.type = type;
        this.severity = severity;
        this.message = message;
        this.metricValue = metricValue;
        this.threshold = threshold;
        this.timestamp = timestamp;
    }
    
    public boolean isResolved(SystemSnapshot snapshot) {
        // Simple resolution check based on alert type
        switch (type) {
            case HIGH_MEMORY_USAGE:
                return snapshot.heapUsagePercent < threshold - 5; // 5% buffer
            case HIGH_THREAD_COUNT:
                return snapshot.threadCount < threshold - 10; // 10 thread buffer
            default:
                return false;
        }
    }
    
    public void markProcessed() {
        this.processed = true;
    }
    
    // Getters
    public String getId() { return id; }
    public AlertType getType() { return type; }
    public AlertSeverity getSeverity() { return severity; }
    public String getMessage() { return message; }
    public double getMetricValue() { return metricValue; }
    public double getThreshold() { return threshold; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isProcessed() { return processed; }
}

/**
 * System health status
 */
public class SystemHealthStatus {
    private final boolean systemActive;
    private final double healthScore;
    private final SystemSnapshot currentSnapshot;
    private final int activeAlerts;
    private final long uptime;
    private final LocalDateTime lastUpdate;
    
    public SystemHealthStatus(boolean systemActive, double healthScore,
                             SystemSnapshot currentSnapshot, int activeAlerts,
                             long uptime, LocalDateTime lastUpdate) {
        this.systemActive = systemActive;
        this.healthScore = healthScore;
        this.currentSnapshot = currentSnapshot;
        this.activeAlerts = activeAlerts;
        this.uptime = uptime;
        this.lastUpdate = lastUpdate;
    }
    
    // Getters
    public boolean isSystemActive() { return systemActive; }
    public double getHealthScore() { return healthScore; }
    public SystemSnapshot getCurrentSnapshot() { return currentSnapshot; }
    public int getActiveAlerts() { return activeAlerts; }
    public long getUptime() { return uptime; }
    public LocalDateTime getLastUpdate() { return lastUpdate; }
    
    public String getHealthGrade() {
        if (healthScore >= 95) return "Excellent";
        if (healthScore >= 80) return "Good";
        if (healthScore >= 65) return "Fair";
        if (healthScore >= 50) return "Poor";
        return "Critical";
    }
}

/**
 * Optimization result
 */
public class OptimizationResult {
    private final int optimizationsApplied;
    private final String summary;
    private final double performanceImprovement;
    private final long memoryFreed;
    
    public OptimizationResult(int optimizationsApplied, String summary,
                             double performanceImprovement, long memoryFreed) {
        this.optimizationsApplied = optimizationsApplied;
        this.summary = summary;
        this.performanceImprovement = performanceImprovement;
        this.memoryFreed = memoryFreed;
    }
    
    public boolean hasOptimizations() {
        return optimizationsApplied > 0;
    }
    
    // Getters
    public int getOptimizationsApplied() { return optimizationsApplied; }
    public String getSummary() { return summary; }
    public double getPerformanceImprovement() { return performanceImprovement; }
    public long getMemoryFreed() { return memoryFreed; }
}

/**
 * Monitoring configuration
 */
public class MonitoringConfig {
    private int collectionInterval = 5000; // 5 seconds
    private int metricRetentionHours = 24;
    private boolean autoOptimizationEnabled = true;
    private double memoryThreshold = 85.0;
    private double cpuThreshold = 80.0;
    private double diskThreshold = 90.0;
    private boolean realTimeAlertsEnabled = true;
    private boolean performanceReportsEnabled = true;
    
    // Getters and setters
    public int getCollectionInterval() { return collectionInterval; }
    public void setCollectionInterval(int collectionInterval) { this.collectionInterval = collectionInterval; }
    
    public int getMetricRetentionHours() { return metricRetentionHours; }
    public void setMetricRetentionHours(int metricRetentionHours) { this.metricRetentionHours = metricRetentionHours; }
    
    public boolean isAutoOptimizationEnabled() { return autoOptimizationEnabled; }
    public void setAutoOptimizationEnabled(boolean autoOptimizationEnabled) { this.autoOptimizationEnabled = autoOptimizationEnabled; }
    
    public double getMemoryThreshold() { return memoryThreshold; }
    public void setMemoryThreshold(double memoryThreshold) { this.memoryThreshold = memoryThreshold; }
    
    public double getCpuThreshold() { return cpuThreshold; }
    public void setCpuThreshold(double cpuThreshold) { this.cpuThreshold = cpuThreshold; }
    
    public double getDiskThreshold() { return diskThreshold; }
    public void setDiskThreshold(double diskThreshold) { this.diskThreshold = diskThreshold; }
    
    public boolean isRealTimeAlertsEnabled() { return realTimeAlertsEnabled; }
    public void setRealTimeAlertsEnabled(boolean realTimeAlertsEnabled) { this.realTimeAlertsEnabled = realTimeAlertsEnabled; }
    
    public boolean isPerformanceReportsEnabled() { return performanceReportsEnabled; }
    public void setPerformanceReportsEnabled(boolean performanceReportsEnabled) { this.performanceReportsEnabled = performanceReportsEnabled; }
}
