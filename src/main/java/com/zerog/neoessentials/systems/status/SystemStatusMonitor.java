package com.zerog.neoessentials.systems.status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive System Status Monitor for NeoEssentials
 * Provides real-time monitoring and status reporting for all enterprise features
 * 
 * Features:
 * - Real-time system resource monitoring
 * - Enterprise component status tracking
 * - Performance metrics collection
 * - Health score calculation
 * - Status history and trending
 * - Automated status reporting
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class SystemStatusMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemStatusMonitor.class);
    
    // Singleton instance
    private static SystemStatusMonitor instance;
    
    // Monitoring components
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    
    // Status tracking
    private final Map<String, ComponentStatus> componentStatuses = new HashMap<>();
    private final List<SystemSnapshot> statusHistory = new ArrayList<>();
    private final Map<String, Double> performanceMetrics = new HashMap<>();
    
    // Background monitoring
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private boolean isRunning = false;
    private long startTime;
    
    // Current system state
    private double currentHealthScore = 100.0;
    private SystemHealth currentHealth = SystemHealth.EXCELLENT;
    private LocalDateTime lastUpdate = LocalDateTime.now();
    
    public SystemStatusMonitor() {
        initializeComponentStatuses();
    }
    
    public static SystemStatusMonitor getInstance() {
        if (instance == null) {
            instance = new SystemStatusMonitor();
        }
        return instance;
    }
    
    /**
     * Initialize the status monitor
     */
    public void initialize() {
        if (isRunning) {
            LOGGER.warn("System Status Monitor is already running");
            return;
        }
        
        LOGGER.info("Initializing System Status Monitor...");
        
        startTime = System.currentTimeMillis();
        
        // Start background monitoring
        startMonitoringServices();
        
        isRunning = true;
        
        // Update component status
        updateComponentStatus("SystemStatusMonitor", ComponentState.ACTIVE, "Monitoring system initialized");
        
        LOGGER.info("System Status Monitor initialized successfully");
    }
    
    /**
     * Shutdown the status monitor
     */
    public void shutdown() {
        if (!isRunning) {
            return;
        }
        
        LOGGER.info("Shutting down System Status Monitor...");
        
        // Update component status
        updateComponentStatus("SystemStatusMonitor", ComponentState.STOPPING, "Shutting down monitoring system");
        
        // Shutdown background services
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        isRunning = false;
        LOGGER.info("System Status Monitor shutdown complete");
    }
    
    /**
     * Get current comprehensive system status
     */
    public ComprehensiveSystemStatus getSystemStatus() {
        updateCurrentMetrics();
        
        return new ComprehensiveSystemStatus(
            isRunning,
            currentHealthScore,
            currentHealth,
            getSystemResourceStatus(),
            getEnterpriseComponentStatus(),
            getPerformanceMetrics(),
            getUptimeMillis(),
            lastUpdate
        );
    }
    
    /**
     * Get system resource status
     */
    public ResourceStatus getSystemResourceStatus() {
        Runtime runtime = Runtime.getRuntime();
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsagePercent = (double) usedMemory / totalMemory * 100.0;
        
        double heapUsagePercent = heapMemory.getMax() > 0 ? 
            (double) heapMemory.getUsed() / heapMemory.getMax() * 100.0 : 0.0;
        
        int threadCount = threadBean.getThreadCount();
        int processorCount = runtime.availableProcessors();
        
        return new ResourceStatus(
            memoryUsagePercent,
            heapUsagePercent,
            threadCount,
            processorCount,
            usedMemory,
            totalMemory,
            heapMemory.getUsed(),
            heapMemory.getMax()
        );
    }
    
    /**
     * Get enterprise component status summary
     */
    public Map<String, ComponentStatus> getEnterpriseComponentStatus() {
        return new HashMap<>(componentStatuses);
    }
    
    /**
     * Update component status
     */
    public void updateComponentStatus(String componentName, ComponentState state, String message) {
        ComponentStatus status = new ComponentStatus(componentName, state, message, LocalDateTime.now());
        componentStatuses.put(componentName, status);
        lastUpdate = LocalDateTime.now();
        
        LOGGER.debug("Component status updated: {} = {} ({})", componentName, state, message);
    }
    
    /**
     * Get performance metrics
     */
    public Map<String, Double> getPerformanceMetrics() {
        return new HashMap<>(performanceMetrics);
    }
    
    /**
     * Update performance metric
     */
    public void updatePerformanceMetric(String metricName, double value) {
        performanceMetrics.put(metricName, value);
        performanceMetrics.put(metricName + "_timestamp", (double) System.currentTimeMillis());
    }
    
    /**
     * Generate detailed status report
     */
    public String generateStatusReport() {
        StringBuilder report = new StringBuilder();
        ComprehensiveSystemStatus status = getSystemStatus();
        
        report.append("=== NeoEssentials Enterprise System Status Report ===\n");
        report.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        report.append("System Uptime: ").append(formatUptime(getUptimeMillis())).append("\n");
        report.append("Overall Health: ").append(status.getHealthScore()).append("% (").append(status.getHealth()).append(")\n");
        report.append("\n");
        
        // System Resources
        ResourceStatus resources = status.getResourceStatus();
        report.append("=== System Resources ===\n");
        report.append("Memory Usage: ").append(String.format("%.1f%%", resources.getMemoryUsagePercent())).append("\n");
        report.append("Heap Usage: ").append(String.format("%.1f%%", resources.getHeapUsagePercent())).append("\n");
        report.append("Thread Count: ").append(resources.getThreadCount()).append("\n");
        report.append("Processor Count: ").append(resources.getProcessorCount()).append("\n");
        report.append("\n");
        
        // Enterprise Components
        report.append("=== Enterprise Components ===\n");
        for (Map.Entry<String, ComponentStatus> entry : status.getComponentStatuses().entrySet()) {
            ComponentStatus componentStatus = entry.getValue();
            report.append(entry.getKey()).append(": ").append(componentStatus.getState());
            if (componentStatus.getMessage() != null && !componentStatus.getMessage().isEmpty()) {
                report.append(" - ").append(componentStatus.getMessage());
            }
            report.append("\n");
        }
        report.append("\n");
        
        // Performance Metrics
        if (!status.getPerformanceMetrics().isEmpty()) {
            report.append("=== Performance Metrics ===\n");
            for (Map.Entry<String, Double> entry : status.getPerformanceMetrics().entrySet()) {
                if (!entry.getKey().endsWith("_timestamp")) {
                    report.append(entry.getKey()).append(": ").append(String.format("%.2f", entry.getValue())).append("\n");
                }
            }
        }
        
        return report.toString();
    }
    
    /**
     * Get system uptime in milliseconds
     */
    public long getUptimeMillis() {
        return isRunning ? System.currentTimeMillis() - startTime : 0;
    }
    
    /**
     * Initialize component statuses
     */
    private void initializeComponentStatuses() {
        // Core components
        updateComponentStatus("DataAnalyticsSystem", ComponentState.UNKNOWN, "Not initialized");
        updateComponentStatus("CommandScheduler", ComponentState.UNKNOWN, "Not initialized");
        updateComponentStatus("PluginCompatibilityManager", ComponentState.UNKNOWN, "Not initialized");
        updateComponentStatus("WebDashboard", ComponentState.UNKNOWN, "Not initialized");
        updateComponentStatus("SecurityManager", ComponentState.UNKNOWN, "Not initialized");
        updateComponentStatus("RealTimeServerMonitor", ComponentState.UNKNOWN, "Not initialized");
        updateComponentStatus("NotificationManager", ComponentState.UNKNOWN, "Not initialized");
    }
    
    /**
     * Start background monitoring services
     */
    private void startMonitoringServices() {
        // System metrics update task (every 10 seconds)
        scheduler.scheduleAtFixedRate(this::updateCurrentMetrics, 0, 10, TimeUnit.SECONDS);
        
        // Status history capture task (every minute)
        scheduler.scheduleAtFixedRate(this::captureStatusSnapshot, 1, 1, TimeUnit.MINUTES);
        
        // Health score calculation task (every 30 seconds)
        scheduler.scheduleAtFixedRate(this::calculateHealthScore, 30, 30, TimeUnit.SECONDS);
    }
    
    /**
     * Update current system metrics
     */
    private void updateCurrentMetrics() {
        try {
            ResourceStatus resources = getSystemResourceStatus();
            
            // Update performance metrics
            updatePerformanceMetric("memory.usage.percent", resources.getMemoryUsagePercent());
            updatePerformanceMetric("heap.usage.percent", resources.getHeapUsagePercent());
            updatePerformanceMetric("thread.count", resources.getThreadCount());
            updatePerformanceMetric("uptime.hours", getUptimeMillis() / (1000.0 * 60.0 * 60.0));
            
            lastUpdate = LocalDateTime.now();
            
        } catch (Exception e) {
            LOGGER.error("Error updating current metrics", e);
        }
    }
    
    /**
     * Capture status snapshot for history
     */
    private void captureStatusSnapshot() {
        try {
            SystemSnapshot snapshot = new SystemSnapshot(
                LocalDateTime.now(),
                currentHealthScore,
                currentHealth,
                getSystemResourceStatus(),
                new HashMap<>(componentStatuses),
                new HashMap<>(performanceMetrics)
            );
            
            statusHistory.add(snapshot);
            
            // Keep only last 100 snapshots (about 1.7 hours at 1-minute intervals)
            if (statusHistory.size() > 100) {
                statusHistory.remove(0);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error capturing status snapshot", e);
        }
    }
    
    /**
     * Calculate overall system health score
     */
    private void calculateHealthScore() {
        try {
            double score = 100.0;
            ResourceStatus resources = getSystemResourceStatus();
            
            // Memory usage impact
            if (resources.getMemoryUsagePercent() > 90) score -= 25;
            else if (resources.getMemoryUsagePercent() > 75) score -= 15;
            else if (resources.getMemoryUsagePercent() > 60) score -= 5;
            
            // Heap usage impact
            if (resources.getHeapUsagePercent() > 90) score -= 25;
            else if (resources.getHeapUsagePercent() > 75) score -= 15;
            else if (resources.getHeapUsagePercent() > 60) score -= 5;
            
            // Thread count impact
            if (resources.getThreadCount() > 200) score -= 20;
            else if (resources.getThreadCount() > 100) score -= 10;
            
            // Component health impact
            long inactiveComponents = componentStatuses.values().stream()
                .mapToLong(status -> status.getState() == ComponentState.ERROR || 
                          status.getState() == ComponentState.STOPPED ? 1 : 0)
                .sum();
            
            score -= inactiveComponents * 10;
            
            // Update health score and grade
            currentHealthScore = Math.max(0, score);
            currentHealth = calculateHealthGrade(currentHealthScore);
            
        } catch (Exception e) {
            LOGGER.error("Error calculating health score", e);
        }
    }
    
    /**
     * Calculate health grade from score
     */
    private SystemHealth calculateHealthGrade(double score) {
        if (score >= 95) return SystemHealth.EXCELLENT;
        if (score >= 80) return SystemHealth.GOOD;
        if (score >= 65) return SystemHealth.FAIR;
        if (score >= 50) return SystemHealth.POOR;
        return SystemHealth.CRITICAL;
    }
    
    /**
     * Format uptime duration
     */
    private String formatUptime(long uptimeMs) {
        if (uptimeMs <= 0) return "0s";
        
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        if (hours > 0) return hours + "h " + (minutes % 60) + "m " + (seconds % 60) + "s";
        if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }
    
    // Getters
    public boolean isRunning() { return isRunning; }
    public double getCurrentHealthScore() { return currentHealthScore; }
    public SystemHealth getCurrentHealth() { return currentHealth; }
    public LocalDateTime getLastUpdate() { return lastUpdate; }
    public List<SystemSnapshot> getStatusHistory() { return new ArrayList<>(statusHistory); }
    
    // Enums and inner classes
    public enum ComponentState {
        UNKNOWN, STARTING, ACTIVE, WARNING, ERROR, STOPPED, STOPPING
    }
    
    public enum SystemHealth {
        EXCELLENT, GOOD, FAIR, POOR, CRITICAL
    }
    
    // Data classes
    public static class ComponentStatus {
        private final String name;
        private final ComponentState state;
        private final String message;
        private final LocalDateTime lastUpdate;
        
        public ComponentStatus(String name, ComponentState state, String message, LocalDateTime lastUpdate) {
            this.name = name;
            this.state = state;
            this.message = message;
            this.lastUpdate = lastUpdate;
        }
        
        public String getName() { return name; }
        public ComponentState getState() { return state; }
        public String getMessage() { return message; }
        public LocalDateTime getLastUpdate() { return lastUpdate; }
    }
    
    public static class ResourceStatus {
        private final double memoryUsagePercent;
        private final double heapUsagePercent;
        private final int threadCount;
        private final int processorCount;
        private final long usedMemory;
        private final long totalMemory;
        private final long heapUsed;
        private final long heapMax;
        
        public ResourceStatus(double memoryUsagePercent, double heapUsagePercent, int threadCount,
                            int processorCount, long usedMemory, long totalMemory, long heapUsed, long heapMax) {
            this.memoryUsagePercent = memoryUsagePercent;
            this.heapUsagePercent = heapUsagePercent;
            this.threadCount = threadCount;
            this.processorCount = processorCount;
            this.usedMemory = usedMemory;
            this.totalMemory = totalMemory;
            this.heapUsed = heapUsed;
            this.heapMax = heapMax;
        }
        
        public double getMemoryUsagePercent() { return memoryUsagePercent; }
        public double getHeapUsagePercent() { return heapUsagePercent; }
        public int getThreadCount() { return threadCount; }
        public int getProcessorCount() { return processorCount; }
        public long getUsedMemory() { return usedMemory; }
        public long getTotalMemory() { return totalMemory; }
        public long getHeapUsed() { return heapUsed; }
        public long getHeapMax() { return heapMax; }
    }
    
    public static class ComprehensiveSystemStatus {
        private final boolean systemActive;
        private final double healthScore;
        private final SystemHealth health;
        private final ResourceStatus resourceStatus;
        private final Map<String, ComponentStatus> componentStatuses;
        private final Map<String, Double> performanceMetrics;
        private final long uptime;
        private final LocalDateTime lastUpdate;
        
        public ComprehensiveSystemStatus(boolean systemActive, double healthScore, SystemHealth health,
                                       ResourceStatus resourceStatus, Map<String, ComponentStatus> componentStatuses,
                                       Map<String, Double> performanceMetrics, long uptime, LocalDateTime lastUpdate) {
            this.systemActive = systemActive;
            this.healthScore = healthScore;
            this.health = health;
            this.resourceStatus = resourceStatus;
            this.componentStatuses = componentStatuses;
            this.performanceMetrics = performanceMetrics;
            this.uptime = uptime;
            this.lastUpdate = lastUpdate;
        }
        
        public boolean isSystemActive() { return systemActive; }
        public double getHealthScore() { return healthScore; }
        public SystemHealth getHealth() { return health; }
        public ResourceStatus getResourceStatus() { return resourceStatus; }
        public Map<String, ComponentStatus> getComponentStatuses() { return componentStatuses; }
        public Map<String, Double> getPerformanceMetrics() { return performanceMetrics; }
        public long getUptime() { return uptime; }
        public LocalDateTime getLastUpdate() { return lastUpdate; }
    }
    
    public static class SystemSnapshot {
        private final LocalDateTime timestamp;
        private final double healthScore;
        private final SystemHealth health;
        private final ResourceStatus resourceStatus;
        private final Map<String, ComponentStatus> componentStatuses;
        private final Map<String, Double> performanceMetrics;
        
        public SystemSnapshot(LocalDateTime timestamp, double healthScore, SystemHealth health,
                            ResourceStatus resourceStatus, Map<String, ComponentStatus> componentStatuses,
                            Map<String, Double> performanceMetrics) {
            this.timestamp = timestamp;
            this.healthScore = healthScore;
            this.health = health;
            this.resourceStatus = resourceStatus;
            this.componentStatuses = componentStatuses;
            this.performanceMetrics = performanceMetrics;
        }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public double getHealthScore() { return healthScore; }
        public SystemHealth getHealth() { return health; }
        public ResourceStatus getResourceStatus() { return resourceStatus; }
        public Map<String, ComponentStatus> getComponentStatuses() { return componentStatuses; }
        public Map<String, Double> getPerformanceMetrics() { return performanceMetrics; }
    }
}
