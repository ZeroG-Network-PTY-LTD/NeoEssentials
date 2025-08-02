package com.zerog.neoessentials.systems.monitoring;

import java.time.LocalDateTime;

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
    
    public SystemSnapshot() {
        this.timestamp = LocalDateTime.now();
    }
    
    public SystemSnapshot(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public double getMemoryUsagePercent() {
        return totalMemory > 0 ? ((double) (totalMemory - freeMemory) / totalMemory) * 100.0 : 0.0;
    }
    
    public double getHeapUsagePercent() {
        return heapMax > 0 ? ((double) heapUsed / heapMax) * 100.0 : 0.0;
    }
    
    @Override
    public String toString() {
        return String.format("SystemSnapshot{timestamp=%s, memoryUsage=%.2f%%, heapUsage=%.2f%%, threads=%d}",
                timestamp, getMemoryUsagePercent(), getHeapUsagePercent(), threadCount);
    }
}
