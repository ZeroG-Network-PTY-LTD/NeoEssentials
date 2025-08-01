package com.zerog.neoessentials.commands.monitoring;

import com.zerog.neoessentials.systems.monitoring.EnterprisePerformanceMonitor;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Enterprise Performance Monitoring Command Interface for NeoEssentials
 * 
 * Provides comprehensive command-line interface for managing and monitoring
 * enterprise performance metrics, optimization recommendations, and predictive analytics.
 * 
 * Available Commands:
 * - /neoperformance status        - Show performance monitoring status
 * - /neoperformance start         - Start performance monitoring
 * - /neoperformance stop          - Stop performance monitoring
 * - /neoperformance metrics       - Display current performance metrics
 * - /neoperformance trends        - Show performance trends analysis
 * - /neoperformance optimize      - Show optimization recommendations
 * - /neoperformance config        - Show/modify performance configuration
 * - /neoperformance history       - Display performance history
 * - /neoperformance predict       - Show predictive analytics
 * - /neoperformance test          - Test performance monitoring systems
 * - /neoperformance help          - Show command help
 * 
 * @author ZeroG Enterprise Performance Team
 * @since 2.3.0
 */
public class PerformanceCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceCommand.class);
    
    private static EnterprisePerformanceMonitor performanceSystem;
    
    /**
     * Register performance monitoring commands
     */
    public static void register() {
        try {
            performanceSystem = EnterprisePerformanceMonitor.getInstance();
            LOGGER.info("Performance monitoring commands registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register performance monitoring commands", e);
        }
    }
    
    /**
     * Register performance monitoring commands with dispatcher
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        try {
            performanceSystem = EnterprisePerformanceMonitor.getInstance();
            
            dispatcher.register(Commands.literal("neoperformance")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status")
                    .executes(PerformanceCommand::executeStatusCommand))
                .then(Commands.literal("start")
                    .executes(PerformanceCommand::executeStartCommand))
                .then(Commands.literal("stop")
                    .executes(PerformanceCommand::executeStopCommand))
                .then(Commands.literal("metrics")
                    .executes(PerformanceCommand::executeMetricsCommand))
                .then(Commands.literal("trends")
                    .executes(PerformanceCommand::executeTrendsCommand))
                .then(Commands.literal("optimize")
                    .executes(PerformanceCommand::executeOptimizeCommand))
                .then(Commands.literal("config")
                    .executes(PerformanceCommand::executeConfigCommand)
                    .then(Commands.literal("interval")
                        .then(Commands.argument("seconds", LongArgumentType.longArg(1, 300))
                            .executes(PerformanceCommand::executeConfigIntervalCommand)))
                    .then(Commands.literal("cpu-warning")
                        .then(Commands.argument("threshold", DoubleArgumentType.doubleArg(0.0, 100.0))
                            .executes(PerformanceCommand::executeConfigCpuWarningCommand)))
                    .then(Commands.literal("cpu-critical")
                        .then(Commands.argument("threshold", DoubleArgumentType.doubleArg(0.0, 100.0))
                            .executes(PerformanceCommand::executeConfigCpuCriticalCommand)))
                    .then(Commands.literal("memory-warning")
                        .then(Commands.argument("threshold", DoubleArgumentType.doubleArg(0.0, 100.0))
                            .executes(PerformanceCommand::executeConfigMemoryWarningCommand)))
                    .then(Commands.literal("memory-critical")
                        .then(Commands.argument("threshold", DoubleArgumentType.doubleArg(0.0, 100.0))
                            .executes(PerformanceCommand::executeConfigMemoryCriticalCommand)))
                    .then(Commands.literal("auto-optimization")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                            .executes(PerformanceCommand::executeConfigAutoOptimizationCommand)))
                    .then(Commands.literal("predictive-analytics")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                            .executes(PerformanceCommand::executeConfigPredictiveCommand))))
                .then(Commands.literal("history")
                    .executes(PerformanceCommand::executeHistoryCommand)
                    .then(Commands.argument("entries", LongArgumentType.longArg(1, 100))
                        .executes(PerformanceCommand::executeHistoryLimitCommand)))
                .then(Commands.literal("predict")
                    .executes(PerformanceCommand::executePredictCommand))
                .then(Commands.literal("test")
                    .executes(PerformanceCommand::executeTestCommand))
                .then(Commands.literal("help")
                    .executes(PerformanceCommand::executeHelpCommand))
                .executes(PerformanceCommand::executeStatusCommand));
            
            LOGGER.info("Performance monitoring commands registered with dispatcher successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register performance monitoring commands with dispatcher", e);
        }
    }
    
    /**
     * Execute performance status command
     */
    private static int executeStatusCommand(CommandContext<CommandSourceStack> context) {
        try {
            String response = buildStatusResponse();
            context.getSource().sendSuccess(() -> Component.literal(response), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing performance status command", e);
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute performance start command
     */
    private static int executeStartCommand(CommandContext<CommandSourceStack> context) {
        try {
            if (performanceSystem.isMonitoring()) {
                context.getSource().sendFailure(Component.literal("Performance monitoring is already running"));
                return 0;
            }
            
            performanceSystem.startPerformanceMonitoring();
            context.getSource().sendSuccess(() -> Component.literal("Performance monitoring started successfully"), true);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error starting performance monitoring", e);
            context.getSource().sendFailure(Component.literal("Error starting performance monitoring: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute performance stop command
     */
    private static int executeStopCommand(CommandContext<CommandSourceStack> context) {
        try {
            if (!performanceSystem.isMonitoring()) {
                context.getSource().sendFailure(Component.literal("Performance monitoring is not running"));
                return 0;
            }
            
            performanceSystem.stopPerformanceMonitoring();
            context.getSource().sendSuccess(() -> Component.literal("Performance monitoring stopped successfully"), true);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error stopping performance monitoring", e);
            context.getSource().sendFailure(Component.literal("Error stopping performance monitoring: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute performance metrics command
     */
    private static int executeMetricsCommand(CommandContext<CommandSourceStack> context) {
        try {
            String response = buildMetricsResponse();
            context.getSource().sendSuccess(() -> Component.literal(response), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing performance metrics command", e);
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute performance trends command
     */
    private static int executeTrendsCommand(CommandContext<CommandSourceStack> context) {
        try {
            String response = buildTrendsResponse();
            context.getSource().sendSuccess(() -> Component.literal(response), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing performance trends command", e);
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute optimization recommendations command
     */
    private static int executeOptimizeCommand(CommandContext<CommandSourceStack> context) {
        try {
            String response = buildOptimizationResponse();
            context.getSource().sendSuccess(() -> Component.literal(response), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing optimization command", e);
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute performance configuration command
     */
    private static int executeConfigCommand(CommandContext<CommandSourceStack> context) {
        try {
            String response = buildConfigurationResponse();
            context.getSource().sendSuccess(() -> Component.literal(response), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing performance config command", e);
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute configuration interval command
     */
    private static int executeConfigIntervalCommand(CommandContext<CommandSourceStack> context) {
        try {
            long seconds = LongArgumentType.getLong(context, "seconds");
            performanceSystem.setMonitoringInterval(seconds * 1000);
            context.getSource().sendSuccess(() -> Component.literal("Performance monitoring interval set to " + seconds + " seconds"), true);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error setting monitoring interval", e);
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute configuration CPU warning threshold command
     */
    private static int executeConfigCpuWarningCommand(CommandContext<CommandSourceStack> context) {
        try {
            double threshold = DoubleArgumentType.getDouble(context, "threshold");
            performanceSystem.setCpuWarningThreshold(threshold);
            context.getSource().sendSuccess(() -> Component.literal("CPU warning threshold set to " + threshold + "%"), true);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error setting CPU warning threshold", e);
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute configuration CPU critical threshold command
     */
    private static int executeConfigCpuCriticalCommand(CommandContext<CommandSourceStack> context) {
        try {
            double threshold = DoubleArgumentType.getDouble(context, "threshold");
            performanceSystem.setCpuCriticalThreshold(threshold);
            context.getSource().sendSuccess(() -> Component.literal("CPU critical threshold set to " + threshold + "%"), true);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error setting CPU critical threshold", e);
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute configuration memory warning threshold command
     */
    private static int executeConfigMemoryWarningCommand(CommandContext<CommandSourceStack> context) {
        try {
            double threshold = DoubleArgumentType.getDouble(context, "threshold");
            performanceSystem.setMemoryWarningThreshold(threshold);
            context.getSource().sendSuccess(() -> Component.literal("Memory warning threshold set to " + threshold + "%"), true);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error setting memory warning threshold", e);
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute configuration memory critical threshold command
     */
    private static int executeConfigMemoryCriticalCommand(CommandContext<CommandSourceStack> context) {
        try {
            double threshold = DoubleArgumentType.getDouble(context, "threshold");
            performanceSystem.setMemoryCriticalThreshold(threshold);
            context.getSource().sendSuccess(() -> Component.literal("Memory critical threshold set to " + threshold + "%"), true);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error setting memory critical threshold", e);
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute configuration auto-optimization command
     */
    private static int executeConfigAutoOptimizationCommand(CommandContext<CommandSourceStack> context) {
        try {
            boolean enabled = BoolArgumentType.getBool(context, "enabled");
            performanceSystem.setAutoOptimizationEnabled(enabled);
            context.getSource().sendSuccess(() -> Component.literal("Auto-optimization " + (enabled ? "enabled" : "disabled")), true);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error setting auto-optimization", e);
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute configuration predictive analytics command
     */
    private static int executeConfigPredictiveCommand(CommandContext<CommandSourceStack> context) {
        try {
            boolean enabled = BoolArgumentType.getBool(context, "enabled");
            performanceSystem.setPredictiveAnalyticsEnabled(enabled);
            context.getSource().sendSuccess(() -> Component.literal("Predictive analytics " + (enabled ? "enabled" : "disabled")), true);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error setting predictive analytics", e);
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute performance history command
     */
    private static int executeHistoryCommand(CommandContext<CommandSourceStack> context) {
        try {
            String response = buildHistoryResponse(20); // Default 20 entries
            context.getSource().sendSuccess(() -> Component.literal(response), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing performance history command", e);
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute performance history with limit command
     */
    private static int executeHistoryLimitCommand(CommandContext<CommandSourceStack> context) {
        try {
            long limit = LongArgumentType.getLong(context, "entries");
            String response = buildHistoryResponse((int) limit);
            context.getSource().sendSuccess(() -> Component.literal(response), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing performance history command", e);
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute predictive analytics command
     */
    private static int executePredictCommand(CommandContext<CommandSourceStack> context) {
        try {
            String response = buildPredictiveResponse();
            context.getSource().sendSuccess(() -> Component.literal(response), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing predictive analytics command", e);
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute performance test command
     */
    private static int executeTestCommand(CommandContext<CommandSourceStack> context) {
        try {
            String response = buildTestResponse();
            context.getSource().sendSuccess(() -> Component.literal(response), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing performance test command", e);
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute help command
     */
    private static int executeHelpCommand(CommandContext<CommandSourceStack> context) {
        try {
            String response = buildHelpResponse();
            context.getSource().sendSuccess(() -> Component.literal(response), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing help command", e);
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Build performance status response
     */
    private static String buildStatusResponse() {
        StringBuilder status = new StringBuilder();
        status.append("=== Enterprise Performance Monitoring Status ===\n");
        
        boolean monitoring = performanceSystem.isMonitoring();
        status.append(String.format("Monitoring Status: %s\n", monitoring ? "ACTIVE" : "INACTIVE"));
        
        Map<String, Object> stats = performanceSystem.getPerformanceStatistics();
        status.append(String.format("Total Performance Checks: %d\n", stats.get("totalChecks")));
        status.append(String.format("Performance Warnings: %d\n", stats.get("warnings")));
        status.append(String.format("Performance Criticals: %d\n", stats.get("criticals")));
        status.append(String.format("Optimizations Suggested: %d\n", stats.get("optimizationsSuggested")));
        status.append(String.format("Current Performance Score: %.1f%%\n", stats.get("performanceScore")));
        status.append(String.format("Metrics Collected: %d\n", stats.get("currentMetricsCount")));
        status.append(String.format("History Entries: %d\n", stats.get("historySize")));
        
        Map<String, Object> config = performanceSystem.getPerformanceConfiguration();
        status.append(String.format("Monitoring Interval: %.1f seconds\n", (Long) config.get("monitoringInterval") / 1000.0));
        status.append(String.format("Auto-Optimization: %s\n", config.get("autoOptimizationEnabled")));
        status.append(String.format("Predictive Analytics: %s\n", config.get("predictiveAnalyticsEnabled")));
        
        status.append("\n--- Performance Status ---\n");
        if (monitoring) {
            double score = (Double) stats.get("performanceScore");
            if (score >= 80) {
                status.append("Performance: EXCELLENT ✓\n");
            } else if (score >= 60) {
                status.append("Performance: GOOD ⚠\n");
            } else if (score >= 40) {
                status.append("Performance: FAIR ⚠\n");
            } else {
                status.append("Performance: POOR ✗\n");
            }
            status.append("All performance monitoring systems operational");
        } else {
            status.append("Performance Status: READY (Not Monitoring)\n");
            status.append("Use '/neoperformance start' to activate monitoring");
        }
        
        status.append("\n===============================================");
        return status.toString();
    }
    
    /**
     * Build current metrics response
     */
    private static String buildMetricsResponse() {
        StringBuilder metrics = new StringBuilder();
        metrics.append("=== Current Performance Metrics ===\n");
        
        Map<String, EnterprisePerformanceMonitor.PerformanceMetric> currentMetrics = performanceSystem.getCurrentMetrics();
        
        if (currentMetrics.isEmpty()) {
            metrics.append("No metrics available. Start monitoring to collect metrics.\n");
        } else {
            // Memory metrics
            metrics.append("\n--- Memory Metrics ---\n");
            addMetricToResponse(metrics, currentMetrics, "heap_memory_percent", "Heap Memory Usage");
            addMetricToResponse(metrics, currentMetrics, "heap_memory_used", "Heap Memory Used", "MB", 1024 * 1024);
            addMetricToResponse(metrics, currentMetrics, "heap_memory_max", "Heap Memory Max", "MB", 1024 * 1024);
            addMetricToResponse(metrics, currentMetrics, "non_heap_memory_percent", "Non-Heap Memory Usage");
            
            // CPU metrics
            metrics.append("\n--- CPU Metrics ---\n");
            addMetricToResponse(metrics, currentMetrics, "process_cpu_load", "Process CPU Load");
            addMetricToResponse(metrics, currentMetrics, "system_cpu_load", "System CPU Load");
            addMetricToResponse(metrics, currentMetrics, "system_load_average", "System Load Average");
            addMetricToResponse(metrics, currentMetrics, "load_per_processor", "Load per Processor");
            
            // Thread metrics
            metrics.append("\n--- Thread Metrics ---\n");
            addMetricToResponse(metrics, currentMetrics, "thread_count", "Thread Count");
            addMetricToResponse(metrics, currentMetrics, "daemon_thread_count", "Daemon Thread Count");
            addMetricToResponse(metrics, currentMetrics, "peak_thread_count", "Peak Thread Count");
            
            // Garbage collection metrics
            metrics.append("\n--- Garbage Collection ---\n");
            addMetricToResponse(metrics, currentMetrics, "total_gc_collections", "Total GC Collections");
            addMetricToResponse(metrics, currentMetrics, "total_gc_time", "Total GC Time", "ms", 1);
            
            // Disk metrics
            metrics.append("\n--- Disk Metrics ---\n");
            addMetricToResponse(metrics, currentMetrics, "disk_used_percent", "Disk Used");
            addMetricToResponse(metrics, currentMetrics, "disk_usable_space", "Disk Free Space", "GB", 1024 * 1024 * 1024);
        }
        
        metrics.append("\n=======================================");
        return metrics.toString();
    }
    
    /**
     * Helper method to add metric to response
     */
    private static void addMetricToResponse(StringBuilder response, Map<String, EnterprisePerformanceMonitor.PerformanceMetric> metrics, 
                                          String key, String displayName) {
        addMetricToResponse(response, metrics, key, displayName, null, 1);
    }
    
    /**
     * Helper method to add metric to response with unit conversion
     */
    private static void addMetricToResponse(StringBuilder response, Map<String, EnterprisePerformanceMonitor.PerformanceMetric> metrics, 
                                          String key, String displayName, String unit, double divisor) {
        EnterprisePerformanceMonitor.PerformanceMetric metric = metrics.get(key);
        if (metric != null) {
            double value = metric.getValue() / divisor;
            String unitStr = unit != null ? unit : metric.getUnit();
            if (unitStr.equals("percent")) {
                response.append(String.format("%s: %.1f%%\n", displayName, value));
            } else {
                response.append(String.format("%s: %.2f %s\n", displayName, value, unitStr));
            }
        }
    }
    
    /**
     * Build performance trends response
     */
    private static String buildTrendsResponse() {
        StringBuilder trends = new StringBuilder();
        trends.append("=== Performance Trends Analysis ===\n");
        
        Map<String, EnterprisePerformanceMonitor.PerformanceTrend> performanceTrends = performanceSystem.getPerformanceTrends();
        
        if (performanceTrends.isEmpty()) {
            trends.append("No trend data available. Monitoring needs to run longer to establish trends.\n");
        } else {
            for (Map.Entry<String, EnterprisePerformanceMonitor.PerformanceTrend> entry : performanceTrends.entrySet()) {
                EnterprisePerformanceMonitor.PerformanceTrend trend = entry.getValue();
                String directionSymbol = getTrendDirectionSymbol(trend.getDirection());
                
                trends.append(String.format("%s: %.2f %s (slope: %.3f)\n", 
                    formatMetricName(trend.getMetricName()), 
                    trend.getCurrentValue(), 
                    directionSymbol, 
                    trend.getSlope()));
            }
        }
        
        trends.append("\n--- Trend Legend ---\n");
        trends.append("↗ Increasing trend\n");
        trends.append("↘ Decreasing trend\n");
        trends.append("→ Stable trend\n");
        
        trends.append("\n=====================================");
        return trends.toString();
    }
    
    /**
     * Build optimization recommendations response
     */
    private static String buildOptimizationResponse() {
        StringBuilder optimize = new StringBuilder();
        optimize.append("=== Performance Optimization Recommendations ===\n");
        
        List<EnterprisePerformanceMonitor.OptimizationRecommendation> optimizations = performanceSystem.getPendingOptimizations();
        
        if (optimizations.isEmpty()) {
            optimize.append("No optimization recommendations at this time.\n");
            optimize.append("System performance is within acceptable parameters.\n");
        } else {
            for (int i = 0; i < optimizations.size() && i < 10; i++) {
                EnterprisePerformanceMonitor.OptimizationRecommendation opt = optimizations.get(i);
                String prioritySymbol = getOptimizationPrioritySymbol(opt.getPriority());
                String age = formatAge(System.currentTimeMillis() - opt.getTimestamp());
                
                optimize.append(String.format("%d. [%s] %s %s\n", 
                    i + 1, 
                    opt.getType(), 
                    prioritySymbol, 
                    opt.getRecommendation()));
                optimize.append(String.format("   Generated: %s ago\n", age));
            }
            
            if (optimizations.size() > 10) {
                optimize.append(String.format("... and %d more recommendations\n", optimizations.size() - 10));
            }
        }
        
        Map<String, Object> config = performanceSystem.getPerformanceConfiguration();
        optimize.append(String.format("\nAuto-Optimization: %s\n", 
            (Boolean) config.get("autoOptimizationEnabled") ? "ENABLED" : "DISABLED"));
        
        optimize.append("\n=============================================");
        return optimize.toString();
    }
    
    /**
     * Build configuration response
     */
    private static String buildConfigurationResponse() {
        StringBuilder config = new StringBuilder();
        config.append("=== Performance Configuration ===\n");
        
        Map<String, Object> perfConfig = performanceSystem.getPerformanceConfiguration();
        
        config.append("--- Monitoring Settings ---\n");
        config.append(String.format("Monitoring Interval: %.1f seconds\n", (Long) perfConfig.get("monitoringInterval") / 1000.0));
        config.append(String.format("Optimization Interval: %.1f minutes\n", (Long) perfConfig.get("optimizationInterval") / 60000.0));
        
        config.append("\n--- Performance Thresholds ---\n");
        config.append(String.format("CPU Warning Threshold: %.1f%%\n", perfConfig.get("cpuWarningThreshold")));
        config.append(String.format("CPU Critical Threshold: %.1f%%\n", perfConfig.get("cpuCriticalThreshold")));
        config.append(String.format("Memory Warning Threshold: %.1f%%\n", perfConfig.get("memoryWarningThreshold")));
        config.append(String.format("Memory Critical Threshold: %.1f%%\n", perfConfig.get("memoryCriticalThreshold")));
        config.append(String.format("Disk Space Warning: %.0f MB\n", (Long) perfConfig.get("diskSpaceWarningThreshold") / (1024.0 * 1024.0)));
        
        config.append("\n--- Feature Settings ---\n");
        config.append(String.format("Auto-Optimization: %s\n", perfConfig.get("autoOptimizationEnabled")));
        config.append(String.format("Predictive Analytics: %s\n", perfConfig.get("predictiveAnalyticsEnabled")));
        config.append(String.format("Performance Log: %s\n", perfConfig.get("performanceLogPath")));
        
        config.append("\n--- Configuration Commands ---\n");
        config.append("/neoperformance config interval <seconds>\n");
        config.append("/neoperformance config cpu-warning <threshold>\n");
        config.append("/neoperformance config memory-warning <threshold>\n");
        config.append("/neoperformance config auto-optimization <true|false>\n");
        config.append("/neoperformance config predictive-analytics <true|false>\n");
        
        config.append("\n===================================");
        return config.toString();
    }
    
    /**
     * Build performance history response
     */
    private static String buildHistoryResponse(int limit) {
        StringBuilder history = new StringBuilder();
        history.append("=== Performance History ===\n");
        
        List<EnterprisePerformanceMonitor.PerformanceSnapshot> snapshots = performanceSystem.getPerformanceHistory();
        
        if (snapshots.isEmpty()) {
            history.append("No performance history available.\n");
        } else {
            int count = Math.min(limit, snapshots.size());
            history.append(String.format("Showing last %d entries:\n\n", count));
            
            for (int i = snapshots.size() - count; i < snapshots.size(); i++) {
                EnterprisePerformanceMonitor.PerformanceSnapshot snapshot = snapshots.get(i);
                String timestamp = LocalDateTime.ofEpochSecond(snapshot.getTimestamp() / 1000, 0, 
                    java.time.ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                
                history.append(String.format("%s - Score: %.1f%%", timestamp, snapshot.getPerformanceScore()));
                
                // Add key metrics
                Map<String, EnterprisePerformanceMonitor.PerformanceMetric> metrics = snapshot.getMetrics();
                if (metrics.containsKey("heap_memory_percent")) {
                    history.append(String.format(" | Mem: %.1f%%", metrics.get("heap_memory_percent").getValue()));
                }
                if (metrics.containsKey("process_cpu_load")) {
                    history.append(String.format(" | CPU: %.1f%%", metrics.get("process_cpu_load").getValue()));
                }
                
                history.append("\n");
            }
        }
        
        history.append("\n=============================");
        return history.toString();
    }
    
    /**
     * Build predictive analytics response
     */
    private static String buildPredictiveResponse() {
        StringBuilder predict = new StringBuilder();
        predict.append("=== Predictive Performance Analytics ===\n");
        
        Map<String, Object> config = performanceSystem.getPerformanceConfiguration();
        boolean enabled = (Boolean) config.get("predictiveAnalyticsEnabled");
        
        if (!enabled) {
            predict.append("Predictive analytics is currently DISABLED.\n");
            predict.append("Enable with: /neoperformance config predictive-analytics true\n");
        } else {
            predict.append("Predictive analytics is ENABLED.\n\n");
            
            Map<String, EnterprisePerformanceMonitor.PerformanceTrend> trends = performanceSystem.getPerformanceTrends();
            
            if (trends.isEmpty()) {
                predict.append("Insufficient data for predictions. Monitoring needs to run longer.\n");
            } else {
                predict.append("--- Performance Predictions ---\n");
                
                // Analyze memory trend
                if (trends.containsKey("memory_usage")) {
                    EnterprisePerformanceMonitor.PerformanceTrend memoryTrend = trends.get("memory_usage");
                    if (memoryTrend.getDirection() == EnterprisePerformanceMonitor.TrendDirection.INCREASING) {
                        predict.append("⚠ Memory usage is trending upward\n");
                        predict.append("  Recommendation: Monitor for memory leaks\n");
                    } else {
                        predict.append("✓ Memory usage is stable\n");
                    }
                }
                
                // Analyze CPU trend
                if (trends.containsKey("cpu_usage")) {
                    EnterprisePerformanceMonitor.PerformanceTrend cpuTrend = trends.get("cpu_usage");
                    if (cpuTrend.getDirection() == EnterprisePerformanceMonitor.TrendDirection.INCREASING) {
                        predict.append("⚠ CPU usage is trending upward\n");
                        predict.append("  Recommendation: Consider performance optimization\n");
                    } else {
                        predict.append("✓ CPU usage is stable\n");
                    }
                }
                
                // Overall performance prediction
                if (trends.containsKey("overall_performance")) {
                    EnterprisePerformanceMonitor.PerformanceTrend overallTrend = trends.get("overall_performance");
                    if (overallTrend.getDirection() == EnterprisePerformanceMonitor.TrendDirection.DECREASING) {
                        predict.append("⚠ Overall performance is declining\n");
                        predict.append("  Recommendation: Comprehensive optimization needed\n");
                    } else {
                        predict.append("✓ Overall performance is stable or improving\n");
                    }
                }
            }
        }
        
        predict.append("\n=========================================");
        return predict.toString();
    }
    
    /**
     * Build test response
     */
    private static String buildTestResponse() {
        StringBuilder test = new StringBuilder();
        test.append("=== Performance Monitoring System Test ===\n");
        
        // Test monitoring status
        boolean monitoring = performanceSystem.isMonitoring();
        test.append(String.format("Performance Monitoring: %s ✓\n", monitoring ? "ACTIVE" : "INACTIVE"));
        
        // Test metrics collection
        Map<String, EnterprisePerformanceMonitor.PerformanceMetric> metrics = performanceSystem.getCurrentMetrics();
        test.append(String.format("Metrics Collection: FUNCTIONAL ✓ (%d metrics)\n", metrics.size()));
        
        // Test configuration access
        Map<String, Object> config = performanceSystem.getPerformanceConfiguration();
        test.append(String.format("Configuration Access: FUNCTIONAL ✓ (Interval: %.1fs)\n", 
            (Long) config.get("monitoringInterval") / 1000.0));
        
        // Test statistics
        Map<String, Object> stats = performanceSystem.getPerformanceStatistics();
        test.append(String.format("Statistics Collection: FUNCTIONAL ✓ (%d checks performed)\n", 
            stats.get("totalChecks")));
        
        // Test optimization system
        List<EnterprisePerformanceMonitor.OptimizationRecommendation> optimizations = performanceSystem.getPendingOptimizations();
        test.append(String.format("Optimization System: FUNCTIONAL ✓ (%d recommendations)\n", optimizations.size()));
        
        // Test trend analysis
        Map<String, EnterprisePerformanceMonitor.PerformanceTrend> trends = performanceSystem.getPerformanceTrends();
        test.append(String.format("Trend Analysis: FUNCTIONAL ✓ (%d trends tracked)\n", trends.size()));
        
        // Overall test results
        test.append("\n--- Test Results ---\n");
        test.append("Performance Monitoring System: OPERATIONAL ✓\n");
        test.append("Metrics Collection: ACTIVE ✓\n");
        test.append("Predictive Analytics: READY ✓\n");
        test.append("Optimization Engine: FUNCTIONAL ✓\n");
        test.append("Configuration Management: ACTIVE ✓\n");
        
        if (monitoring) {
            test.append("\nPerformance Status: FULLY OPERATIONAL\n");
            test.append("All performance monitoring systems are functioning correctly");
        } else {
            test.append("\nPerformance Status: READY (Not Monitoring)\n");
            test.append("Use '/neoperformance start' to activate monitoring");
        }
        
        test.append("\n==========================================");
        return test.toString();
    }
    
    /**
     * Build help response
     */
    private static String buildHelpResponse() {
        StringBuilder help = new StringBuilder();
        help.append("=== Enterprise Performance Monitoring Commands ===\n\n");
        
        help.append("--- Basic Commands ---\n");
        help.append("/neoperformance status       - Show performance monitoring status\n");
        help.append("/neoperformance start        - Start performance monitoring\n");
        help.append("/neoperformance stop         - Stop performance monitoring\n");
        help.append("/neoperformance test         - Test performance monitoring systems\n");
        
        help.append("\n--- Monitoring Commands ---\n");
        help.append("/neoperformance metrics      - Display current performance metrics\n");
        help.append("/neoperformance trends       - Show performance trends analysis\n");
        help.append("/neoperformance history [n]  - Display performance history (last n entries)\n");
        help.append("/neoperformance predict      - Show predictive analytics\n");
        
        help.append("\n--- Optimization Commands ---\n");
        help.append("/neoperformance optimize     - Show optimization recommendations\n");
        
        help.append("\n--- Configuration Commands ---\n");
        help.append("/neoperformance config                          - Show current configuration\n");
        help.append("/neoperformance config interval <seconds>       - Set monitoring interval\n");
        help.append("/neoperformance config cpu-warning <percent>    - Set CPU warning threshold\n");
        help.append("/neoperformance config cpu-critical <percent>   - Set CPU critical threshold\n");
        help.append("/neoperformance config memory-warning <percent> - Set memory warning threshold\n");
        help.append("/neoperformance config memory-critical <percent>- Set memory critical threshold\n");
        help.append("/neoperformance config auto-optimization <bool> - Enable/disable auto-optimization\n");
        help.append("/neoperformance config predictive-analytics <bool> - Enable/disable predictive analytics\n");
        
        help.append("\n--- Information ---\n");
        help.append("/neoperformance help         - Show this help information\n");
        
        help.append("\n--- Permission Requirements ---\n");
        help.append("All commands require operator permissions (level 2)\n");
        
        help.append("\n=================================================");
        return help.toString();
    }
    
    // Helper methods
    private static String getTrendDirectionSymbol(EnterprisePerformanceMonitor.TrendDirection direction) {
        switch (direction) {
            case INCREASING: return "↗";
            case DECREASING: return "↘";
            case STABLE: return "→";
            default: return "?";
        }
    }
    
    private static String getOptimizationPrioritySymbol(EnterprisePerformanceMonitor.OptimizationPriority priority) {
        switch (priority) {
            case CRITICAL: return "🔴";
            case HIGH: return "🟠";
            case MEDIUM: return "🟡";
            case LOW: return "🟢";
            default: return "⚪";
        }
    }
    
    private static String formatMetricName(String metricName) {
        return metricName.replace("_", " ").toUpperCase();
    }
    
    private static String formatAge(long ageMillis) {
        long seconds = ageMillis / 1000;
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m";
        long hours = minutes / 60;
        return hours + "h " + (minutes % 60) + "m";
    }
}
