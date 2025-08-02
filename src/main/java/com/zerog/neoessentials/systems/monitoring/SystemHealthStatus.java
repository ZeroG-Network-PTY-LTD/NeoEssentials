package com.zerog.neoessentials.systems.monitoring;

import java.time.LocalDateTime;

/**
 * System health status information
 */
public class SystemHealthStatus {
    private final boolean healthy;
    private final String status;
    private final double cpuUsage;
    private final double memoryUsage;
    private final double diskUsage;
    private final int threadCount;
    private final long uptime;
    private final double healthScore;
    private final SystemSnapshot snapshot;
    private final int alertCount;
    private final LocalDateTime timestamp;
    
    public SystemHealthStatus(boolean healthy, String status, double cpuUsage, 
                            double memoryUsage, double diskUsage, int threadCount, long uptime) {
        this.healthy = healthy;
        this.status = status;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.diskUsage = diskUsage;
        this.threadCount = threadCount;
        this.uptime = uptime;
        this.healthScore = 0.0;
        this.snapshot = null;
        this.alertCount = 0;
        this.timestamp = LocalDateTime.now();
    }
    
    // Additional constructor for RealTimeServerMonitor
    public SystemHealthStatus(boolean isRunning, double healthScore, SystemSnapshot snapshot,
                            int alertCount, long uptime, LocalDateTime timestamp) {
        this.healthy = isRunning && healthScore > 50.0;
        this.status = healthy ? "Healthy" : "Unhealthy";
        this.cpuUsage = snapshot != null ? snapshot.systemLoadAverage : 0.0;
        this.memoryUsage = snapshot != null ? snapshot.memoryUsagePercent : 0.0;
        this.diskUsage = 0.0; // Not available in snapshot
        this.threadCount = snapshot != null ? snapshot.threadCount : 0;
        this.uptime = uptime;
        this.healthScore = healthScore;
        this.snapshot = snapshot;
        this.alertCount = alertCount;
        this.timestamp = timestamp;
    }
    
    public boolean isHealthy() { return healthy; }
    public String getStatus() { return status; }
    public double getCpuUsage() { return cpuUsage; }
    public double getMemoryUsage() { return memoryUsage; }
    public double getDiskUsage() { return diskUsage; }
    public int getThreadCount() { return threadCount; }
    public long getUptime() { return uptime; }
    public double getHealthScore() { return healthScore; }
    public SystemSnapshot getSnapshot() { return snapshot; }
    public int getAlertCount() { return alertCount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
