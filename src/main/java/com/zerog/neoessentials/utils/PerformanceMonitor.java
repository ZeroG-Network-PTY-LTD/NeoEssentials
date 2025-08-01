package com.zerog.neoessentials.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Performance monitoring system for NeoEssentials
 * Tracks command execution times, memory usage, and system metrics
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class PerformanceMonitor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceMonitor.class);
    private static PerformanceMonitor instance;
    
    // Command performance tracking
    private final Map<String, CommandMetrics> commandMetrics;
    private final Map<String, Long> commandStartTimes;
    
    // System monitoring
    private final MemoryMXBean memoryBean;
    private final OperatingSystemMXBean osBean;
    private final RuntimeMXBean runtimeBean;
    
    // Performance settings
    private final boolean monitoringEnabled;
    private final long performanceWarningThreshold; // milliseconds
    
    private PerformanceMonitor() {
        this.commandMetrics = new ConcurrentHashMap<>();
        this.commandStartTimes = new ConcurrentHashMap<>();
        
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.runtimeBean = ManagementFactory.getRuntimeMXBean();
        
        this.monitoringEnabled = true; // Could be configurable
        this.performanceWarningThreshold = 100; // 100ms warning threshold
    }
    
    public static PerformanceMonitor getInstance() {
        if (instance == null) {
            instance = new PerformanceMonitor();
        }
        return instance;
    }
    
    /**
     * Start tracking command execution
     */
    public void startCommand(String commandName, String playerName) {
        if (!monitoringEnabled) return;
        
        String key = commandName + ":" + playerName + ":" + Thread.currentThread().threadId();
        commandStartTimes.put(key, System.nanoTime());
        
        LOGGER.debug("Started tracking command: {} for player: {}", commandName, playerName);
    }
    
    /**
     * End tracking command execution
     */
    public void endCommand(String commandName, String playerName, boolean success) {
        if (!monitoringEnabled) return;
        
        String key = commandName + ":" + playerName + ":" + Thread.currentThread().threadId();
        Long startTime = commandStartTimes.remove(key);
        
        if (startTime != null) {
            long executionTime = (System.nanoTime() - startTime) / 1_000_000; // Convert to milliseconds
            
            recordCommandExecution(commandName, executionTime, success);
            
            if (executionTime > performanceWarningThreshold) {
                LOGGER.warn("Command {} took {}ms to execute (player: {}, success: {})", 
                    commandName, executionTime, playerName, success);
            }
            
            LOGGER.debug("Command {} completed in {}ms (player: {}, success: {})", 
                commandName, executionTime, playerName, success);
        }
    }
    
    /**
     * Record command execution metrics
     */
    private void recordCommandExecution(String commandName, long executionTime, boolean success) {
        commandMetrics.computeIfAbsent(commandName, k -> new CommandMetrics(commandName))
            .recordExecution(executionTime, success);
    }
    
    /**
     * Get performance metrics for a command
     */
    public CommandMetrics getCommandMetrics(String commandName) {
        return commandMetrics.get(commandName);
    }
    
    /**
     * Get all command metrics
     */
    public Map<String, CommandMetrics> getAllCommandMetrics() {
        return Collections.unmodifiableMap(commandMetrics);
    }
    
    /**
     * Get system performance information
     */
    public SystemMetrics getSystemMetrics() {
        return new SystemMetrics(
            memoryBean.getHeapMemoryUsage().getUsed(),
            memoryBean.getHeapMemoryUsage().getMax(),
            memoryBean.getNonHeapMemoryUsage().getUsed(),
            osBean.getSystemLoadAverage(),
            runtimeBean.getUptime(),
            Runtime.getRuntime().availableProcessors()
        );
    }
    
    /**
     * Clear all metrics (useful for reset/cleanup)
     */
    public void clearMetrics() {
        commandMetrics.clear();
        commandStartTimes.clear();
        LOGGER.info("Performance metrics cleared");
    }
    
    /**
     * Generate performance report
     */
    public PerformanceReport generateReport() {
        List<CommandMetrics> sortedMetrics = new ArrayList<>(commandMetrics.values());
        sortedMetrics.sort((a, b) -> Long.compare(b.getTotalExecutions(), a.getTotalExecutions()));
        
        SystemMetrics systemMetrics = getSystemMetrics();
        
        return new PerformanceReport(sortedMetrics, systemMetrics);
    }
    
    /**
     * Command performance metrics
     */
    public static class CommandMetrics {
        private final String commandName;
        private final AtomicLong totalExecutions;
        private final AtomicLong successfulExecutions;
        private final AtomicLong totalExecutionTime;
        private final AtomicLong minExecutionTime;
        private final AtomicLong maxExecutionTime;
        private final List<Long> recentExecutionTimes;
        
        public CommandMetrics(String commandName) {
            this.commandName = commandName;
            this.totalExecutions = new AtomicLong(0);
            this.successfulExecutions = new AtomicLong(0);
            this.totalExecutionTime = new AtomicLong(0);
            this.minExecutionTime = new AtomicLong(Long.MAX_VALUE);
            this.maxExecutionTime = new AtomicLong(0);
            this.recentExecutionTimes = Collections.synchronizedList(new ArrayList<>());
        }
        
        public void recordExecution(long executionTime, boolean success) {
            totalExecutions.incrementAndGet();
            if (success) {
                successfulExecutions.incrementAndGet();
            }
            
            totalExecutionTime.addAndGet(executionTime);
            
            // Update min/max
            minExecutionTime.updateAndGet(current -> Math.min(current, executionTime));
            maxExecutionTime.updateAndGet(current -> Math.max(current, executionTime));
            
            // Track recent execution times (for calculating moving averages)
            synchronized (recentExecutionTimes) {
                recentExecutionTimes.add(executionTime);
                if (recentExecutionTimes.size() > 100) { // Keep last 100 executions
                    recentExecutionTimes.remove(0);
                }
            }
        }
        
        public String getCommandName() { return commandName; }
        public long getTotalExecutions() { return totalExecutions.get(); }
        public long getSuccessfulExecutions() { return successfulExecutions.get(); }
        public long getFailedExecutions() { return totalExecutions.get() - successfulExecutions.get(); }
        public double getSuccessRate() { 
            long total = totalExecutions.get();
            return total > 0 ? (double) successfulExecutions.get() / total * 100 : 0;
        }
        
        public double getAverageExecutionTime() {
            long total = totalExecutions.get();
            return total > 0 ? (double) totalExecutionTime.get() / total : 0;
        }
        
        public long getMinExecutionTime() { 
            long min = minExecutionTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }
        
        public long getMaxExecutionTime() { return maxExecutionTime.get(); }
        
        public double getRecentAverageExecutionTime() {
            synchronized (recentExecutionTimes) {
                if (recentExecutionTimes.isEmpty()) return 0;
                return recentExecutionTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            }
        }
    }
    
    /**
     * System performance metrics
     */
    public static class SystemMetrics {
        private final long heapMemoryUsed;
        private final long heapMemoryMax;
        private final long nonHeapMemoryUsed;
        private final double systemLoadAverage;
        private final long uptime;
        private final int availableProcessors;
        
        public SystemMetrics(long heapMemoryUsed, long heapMemoryMax, long nonHeapMemoryUsed,
                           double systemLoadAverage, long uptime, int availableProcessors) {
            this.heapMemoryUsed = heapMemoryUsed;
            this.heapMemoryMax = heapMemoryMax;
            this.nonHeapMemoryUsed = nonHeapMemoryUsed;
            this.systemLoadAverage = systemLoadAverage;
            this.uptime = uptime;
            this.availableProcessors = availableProcessors;
        }
        
        public long getHeapMemoryUsed() { return heapMemoryUsed; }
        public long getHeapMemoryMax() { return heapMemoryMax; }
        public long getNonHeapMemoryUsed() { return nonHeapMemoryUsed; }
        public double getSystemLoadAverage() { return systemLoadAverage; }
        public long getUptime() { return uptime; }
        public int getAvailableProcessors() { return availableProcessors; }
        
        public double getHeapMemoryUsagePercent() {
            return heapMemoryMax > 0 ? (double) heapMemoryUsed / heapMemoryMax * 100 : 0;
        }
        
        public String getFormattedHeapMemory() {
            return formatBytes(heapMemoryUsed) + " / " + formatBytes(heapMemoryMax);
        }
        
        public String getFormattedUptime() {
            long seconds = uptime / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;
            
            if (days > 0) {
                return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
            } else if (hours > 0) {
                return String.format("%dh %dm", hours, minutes % 60);
            } else {
                return String.format("%dm %ds", minutes, seconds % 60);
            }
        }
        
        private String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * Performance report containing all metrics
     */
    public static class PerformanceReport {
        private final List<CommandMetrics> commandMetrics;
        private final SystemMetrics systemMetrics;
        private final long reportTimestamp;
        
        public PerformanceReport(List<CommandMetrics> commandMetrics, SystemMetrics systemMetrics) {
            this.commandMetrics = new ArrayList<>(commandMetrics);
            this.systemMetrics = systemMetrics;
            this.reportTimestamp = System.currentTimeMillis();
        }
        
        public List<CommandMetrics> getCommandMetrics() { return commandMetrics; }
        public SystemMetrics getSystemMetrics() { return systemMetrics; }
        public long getReportTimestamp() { return reportTimestamp; }
        
        /**
         * Generate formatted report string
         */
        public String generateFormattedReport() {
            StringBuilder report = new StringBuilder();
            report.append("=== NeoEssentials Performance Report ===\n");
            report.append("Generated: ").append(new java.util.Date(reportTimestamp)).append("\n\n");
            
            // System metrics
            report.append("--- System Metrics ---\n");
            report.append("Memory Usage: ").append(systemMetrics.getFormattedHeapMemory())
                  .append(" (").append(String.format("%.1f%%", systemMetrics.getHeapMemoryUsagePercent())).append(")\n");
            report.append("System Load: ").append(String.format("%.2f", systemMetrics.getSystemLoadAverage())).append("\n");
            report.append("Uptime: ").append(systemMetrics.getFormattedUptime()).append("\n");
            report.append("CPU Cores: ").append(systemMetrics.getAvailableProcessors()).append("\n\n");
            
            // Command metrics
            report.append("--- Command Performance ---\n");
            if (commandMetrics.isEmpty()) {
                report.append("No command metrics available.\n");
            } else {
                report.append(String.format("%-20s %-10s %-10s %-10s %-10s %-10s\n", 
                    "Command", "Executions", "Success%", "Avg(ms)", "Min(ms)", "Max(ms)"));
                report.append("-".repeat(80)).append("\n");
                
                for (CommandMetrics metrics : commandMetrics) {
                    report.append(String.format("%-20s %-10d %-10.1f %-10.1f %-10d %-10d\n",
                        metrics.getCommandName(),
                        metrics.getTotalExecutions(),
                        metrics.getSuccessRate(),
                        metrics.getAverageExecutionTime(),
                        metrics.getMinExecutionTime(),
                        metrics.getMaxExecutionTime()
                    ));
                }
            }
            
            return report.toString();
        }
    }
}
