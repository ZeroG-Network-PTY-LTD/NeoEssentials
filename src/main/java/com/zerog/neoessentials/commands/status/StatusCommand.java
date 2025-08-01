package com.zerog.neoessentials.commands.status;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.systems.status.SystemStatusMonitor;
import com.zerog.neoessentials.systems.status.SystemStatusMonitor.ComprehensiveSystemStatus;
import com.zerog.neoessentials.systems.status.SystemStatusMonitor.ComponentStatus;
import com.zerog.neoessentials.systems.status.SystemStatusMonitor.ResourceStatus;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Enhanced Status Commands for NeoEssentials Enterprise
 * Provides comprehensive system status monitoring and reporting
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class StatusCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusCommand.class);
    
    private static final SystemStatusMonitor statusMonitor = SystemStatusMonitor.getInstance();
    
    /**
     * Register status commands
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("neostatus")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("help")
                .executes(StatusCommand::showHelp))
            .then(Commands.literal("overview")
                .executes(StatusCommand::showSystemOverview))
            .then(Commands.literal("resources")
                .executes(StatusCommand::showResourceStatus))
            .then(Commands.literal("components")
                .executes(StatusCommand::showComponentStatus))
            .then(Commands.literal("performance")
                .executes(StatusCommand::showPerformanceMetrics))
            .then(Commands.literal("health")
                .executes(StatusCommand::showHealthStatus))
            .then(Commands.literal("report")
                .executes(StatusCommand::generateStatusReport))
            .then(Commands.literal("history")
                .then(Commands.argument("hours", IntegerArgumentType.integer(1, 24))
                    .executes(ctx -> showStatusHistory(ctx, IntegerArgumentType.getInteger(ctx, "hours")))))
        );
        
        LOGGER.info("Enhanced status commands registered successfully");
    }
    
    /**
     * Show status help menu
     */
    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        sendMessage(ctx.getSource(), "&6&l=== NeoEssentials Status Commands ===");
        sendMessage(ctx.getSource(), "&7Comprehensive system monitoring and status reporting");
        sendMessage(ctx.getSource(), "");
        sendMessage(ctx.getSource(), "&e/neostatus overview &7- Complete system overview");
        sendMessage(ctx.getSource(), "&e/neostatus resources &7- System resource usage");
        sendMessage(ctx.getSource(), "&e/neostatus components &7- Enterprise component status");
        sendMessage(ctx.getSource(), "&e/neostatus performance &7- Performance metrics");
        sendMessage(ctx.getSource(), "&e/neostatus health &7- System health assessment");
        sendMessage(ctx.getSource(), "&e/neostatus report &7- Generate detailed status report");
        sendMessage(ctx.getSource(), "&e/neostatus history <hours> &7- View status history");
        sendMessage(ctx.getSource(), "");
        sendMessage(ctx.getSource(), "&7Real-time monitoring for enterprise environments!");
        
        return 1;
    }
    
    /**
     * Show comprehensive system overview
     */
    private static int showSystemOverview(CommandContext<CommandSourceStack> ctx) {
        try {
            ComprehensiveSystemStatus status = statusMonitor.getSystemStatus();
            
            sendMessage(ctx.getSource(), "&6&l=== NeoEssentials Enterprise System Overview ===");
            sendMessage(ctx.getSource(), "&eSystem Status: &a" + (status.isSystemActive() ? "Active" : "Inactive"));
            sendMessage(ctx.getSource(), "&eHealth Score: &a" + String.format("%.1f%%", status.getHealthScore()) + " &7(" + status.getHealth() + ")");
            sendMessage(ctx.getSource(), "&eUptime: &a" + formatUptime(status.getUptime()));
            sendMessage(ctx.getSource(), "&eLast Update: &a" + status.getLastUpdate().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            sendMessage(ctx.getSource(), "");
            
            // Quick resource summary
            ResourceStatus resources = status.getResourceStatus();
            sendMessage(ctx.getSource(), "&e&lQuick Resource Summary:");
            sendMessage(ctx.getSource(), "&aMemory: &7" + String.format("%.1f%%", resources.getMemoryUsagePercent()) + " usage");
            sendMessage(ctx.getSource(), "&aHeap: &7" + String.format("%.1f%%", resources.getHeapUsagePercent()) + " usage");
            sendMessage(ctx.getSource(), "&aThreads: &7" + resources.getThreadCount() + " active");
            sendMessage(ctx.getSource(), "&aCPU Cores: &7" + resources.getProcessorCount());
            sendMessage(ctx.getSource(), "");
            
            // Component status summary
            Map<String, ComponentStatus> components = status.getComponentStatuses();
            long activeComponents = components.values().stream()
                .mapToLong(comp -> comp.getState() == SystemStatusMonitor.ComponentState.ACTIVE ? 1 : 0)
                .sum();
            
            sendMessage(ctx.getSource(), "&e&lEnterprise Components:");
            sendMessage(ctx.getSource(), "&aActive Components: &7" + activeComponents + " / " + components.size());
            sendMessage(ctx.getSource(), "&aAll Systems: &7" + (activeComponents == components.size() ? "Operational" : "Some Issues"));
            sendMessage(ctx.getSource(), "");
            
            sendMessage(ctx.getSource(), "&7Use &e/neostatus <category> &7for detailed information");
            
        } catch (Exception e) {
            sendMessage(ctx.getSource(), "&cError retrieving system overview: " + e.getMessage());
            LOGGER.error("Error in system overview command", e);
        }
        
        return 1;
    }
    
    /**
     * Show detailed resource status
     */
    private static int showResourceStatus(CommandContext<CommandSourceStack> ctx) {
        try {
            ResourceStatus resources = statusMonitor.getSystemResourceStatus();
            
            sendMessage(ctx.getSource(), "&6&l=== System Resource Status ===");
            sendMessage(ctx.getSource(), "");
            
            // Memory information
            sendMessage(ctx.getSource(), "&e&lMemory Usage:");
            sendMessage(ctx.getSource(), "&aTotal Memory: &7" + formatBytes(resources.getTotalMemory()));
            sendMessage(ctx.getSource(), "&aUsed Memory: &7" + formatBytes(resources.getUsedMemory()));
            sendMessage(ctx.getSource(), "&aMemory Usage: &7" + String.format("%.1f%%", resources.getMemoryUsagePercent()));
            sendMessage(ctx.getSource(), "");
            
            // Heap information
            sendMessage(ctx.getSource(), "&e&lHeap Memory:");
            sendMessage(ctx.getSource(), "&aHeap Max: &7" + formatBytes(resources.getHeapMax()));
            sendMessage(ctx.getSource(), "&aHeap Used: &7" + formatBytes(resources.getHeapUsed()));
            sendMessage(ctx.getSource(), "&aHeap Usage: &7" + String.format("%.1f%%", resources.getHeapUsagePercent()));
            sendMessage(ctx.getSource(), "");
            
            // System information
            sendMessage(ctx.getSource(), "&e&lSystem Information:");
            sendMessage(ctx.getSource(), "&aProcessor Cores: &7" + resources.getProcessorCount());
            sendMessage(ctx.getSource(), "&aActive Threads: &7" + resources.getThreadCount());
            sendMessage(ctx.getSource(), "&aSystem Uptime: &7" + formatUptime(statusMonitor.getUptimeMillis()));
            sendMessage(ctx.getSource(), "");
            
            // Resource health assessment
            String memoryHealth = getResourceHealth(resources.getMemoryUsagePercent());
            String heapHealth = getResourceHealth(resources.getHeapUsagePercent());
            String threadHealth = resources.getThreadCount() > 150 ? "High" : 
                                resources.getThreadCount() > 100 ? "Moderate" : "Good";
            
            sendMessage(ctx.getSource(), "&e&lResource Health:");
            sendMessage(ctx.getSource(), "&aMemory Health: &7" + memoryHealth);
            sendMessage(ctx.getSource(), "&aHeap Health: &7" + heapHealth);
            sendMessage(ctx.getSource(), "&aThread Health: &7" + threadHealth);
            
        } catch (Exception e) {
            sendMessage(ctx.getSource(), "&cError retrieving resource status: " + e.getMessage());
            LOGGER.error("Error in resource status command", e);
        }
        
        return 1;
    }
    
    /**
     * Show enterprise component status
     */
    private static int showComponentStatus(CommandContext<CommandSourceStack> ctx) {
        try {
            Map<String, ComponentStatus> components = statusMonitor.getEnterpriseComponentStatus();
            
            sendMessage(ctx.getSource(), "&6&l=== Enterprise Component Status ===");
            sendMessage(ctx.getSource(), "&7Total Components: " + components.size());
            sendMessage(ctx.getSource(), "");
            
            for (Map.Entry<String, ComponentStatus> entry : components.entrySet()) {
                ComponentStatus status = entry.getValue();
                String statusColor = getStatusColor(status.getState());
                String statusText = status.getState().toString();
                
                sendMessage(ctx.getSource(), "&e" + entry.getKey() + ": " + statusColor + statusText);
                
                if (status.getMessage() != null && !status.getMessage().isEmpty()) {
                    sendMessage(ctx.getSource(), "&7  └─ " + status.getMessage());
                }
                
                sendMessage(ctx.getSource(), "&7  └─ Last Update: " + 
                    status.getLastUpdate().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                sendMessage(ctx.getSource(), "");
            }
            
            // Component summary
            long activeCount = components.values().stream()
                .mapToLong(comp -> comp.getState() == SystemStatusMonitor.ComponentState.ACTIVE ? 1 : 0)
                .sum();
            long errorCount = components.values().stream()
                .mapToLong(comp -> comp.getState() == SystemStatusMonitor.ComponentState.ERROR ? 1 : 0)
                .sum();
            
            sendMessage(ctx.getSource(), "&e&lComponent Summary:");
            sendMessage(ctx.getSource(), "&aActive: &7" + activeCount);
            sendMessage(ctx.getSource(), "&cErrors: &7" + errorCount);
            sendMessage(ctx.getSource(), "&eOther States: &7" + (components.size() - activeCount - errorCount));
            
        } catch (Exception e) {
            sendMessage(ctx.getSource(), "&cError retrieving component status: " + e.getMessage());
            LOGGER.error("Error in component status command", e);
        }
        
        return 1;
    }
    
    /**
     * Show performance metrics
     */
    private static int showPerformanceMetrics(CommandContext<CommandSourceStack> ctx) {
        try {
            Map<String, Double> metrics = statusMonitor.getPerformanceMetrics();
            
            sendMessage(ctx.getSource(), "&6&l=== Performance Metrics ===");
            sendMessage(ctx.getSource(), "&7Real-time system performance data");
            sendMessage(ctx.getSource(), "");
            
            if (metrics.isEmpty()) {
                sendMessage(ctx.getSource(), "&7No performance metrics available yet");
                sendMessage(ctx.getSource(), "&7Metrics will appear after system initialization");
            } else {
                for (Map.Entry<String, Double> entry : metrics.entrySet()) {
                    String metricName = entry.getKey();
                    if (metricName.endsWith("_timestamp")) {
                        continue; // Skip timestamp entries
                    }
                    
                    Double value = entry.getValue();
                    String formattedValue;
                    
                    if (metricName.contains("percent")) {
                        formattedValue = String.format("%.1f%%", value);
                    } else if (metricName.contains("hours")) {
                        formattedValue = String.format("%.2f hours", value);
                    } else {
                        formattedValue = String.format("%.2f", value);
                    }
                    
                    sendMessage(ctx.getSource(), "&e" + formatMetricName(metricName) + ": &a" + formattedValue);
                }
            }
            
            sendMessage(ctx.getSource(), "");
            sendMessage(ctx.getSource(), "&7Metrics updated every 10 seconds");
            
        } catch (Exception e) {
            sendMessage(ctx.getSource(), "&cError retrieving performance metrics: " + e.getMessage());
            LOGGER.error("Error in performance metrics command", e);
        }
        
        return 1;
    }
    
    /**
     * Show health status assessment
     */
    private static int showHealthStatus(CommandContext<CommandSourceStack> ctx) {
        try {
            ComprehensiveSystemStatus status = statusMonitor.getSystemStatus();
            
            sendMessage(ctx.getSource(), "&6&l=== System Health Assessment ===");
            sendMessage(ctx.getSource(), "&eOverall Health Score: &a" + String.format("%.1f%%", status.getHealthScore()));
            sendMessage(ctx.getSource(), "&eHealth Grade: &a" + status.getHealth());
            sendMessage(ctx.getSource(), "&eAssessment Time: &a" + status.getLastUpdate().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            sendMessage(ctx.getSource(), "");
            
            // Health breakdown
            sendMessage(ctx.getSource(), "&e&lHealth Factors:");
            ResourceStatus resources = status.getResourceStatus();
            
            // Memory health
            double memoryScore = calculateMemoryHealthScore(resources.getMemoryUsagePercent());
            sendMessage(ctx.getSource(), "&aMemory Health: &7" + String.format("%.1f%%", memoryScore) + 
                " (" + getResourceHealth(resources.getMemoryUsagePercent()) + ")");
            
            // Heap health
            double heapScore = calculateMemoryHealthScore(resources.getHeapUsagePercent());
            sendMessage(ctx.getSource(), "&aHeap Health: &7" + String.format("%.1f%%", heapScore) + 
                " (" + getResourceHealth(resources.getHeapUsagePercent()) + ")");
            
            // Component health
            Map<String, ComponentStatus> components = status.getComponentStatuses();
            long activeComponents = components.values().stream()
                .mapToLong(comp -> comp.getState() == SystemStatusMonitor.ComponentState.ACTIVE ? 1 : 0)
                .sum();
            double componentScore = (double) activeComponents / components.size() * 100.0;
            sendMessage(ctx.getSource(), "&aComponent Health: &7" + String.format("%.1f%%", componentScore) + 
                " (" + activeComponents + "/" + components.size() + " active)");
            
            sendMessage(ctx.getSource(), "");
            
            // Health recommendations
            sendMessage(ctx.getSource(), "&e&lRecommendations:");
            if (status.getHealthScore() >= 95) {
                sendMessage(ctx.getSource(), "&a✓ System is operating at optimal levels");
                sendMessage(ctx.getSource(), "&7  Continue regular monitoring");
            } else if (status.getHealthScore() >= 80) {
                sendMessage(ctx.getSource(), "&e! System performance is good but could be optimized");
                sendMessage(ctx.getSource(), "&7  Consider running performance optimization");
            } else if (status.getHealthScore() >= 65) {
                sendMessage(ctx.getSource(), "&6⚠ System performance needs attention");
                sendMessage(ctx.getSource(), "&7  Check resource usage and component status");
            } else {
                sendMessage(ctx.getSource(), "&c⚠ Critical system issues detected");
                sendMessage(ctx.getSource(), "&7  Immediate investigation recommended");
            }
            
        } catch (Exception e) {
            sendMessage(ctx.getSource(), "&cError retrieving health status: " + e.getMessage());
            LOGGER.error("Error in health status command", e);
        }
        
        return 1;
    }
    
    /**
     * Generate comprehensive status report
     */
    private static int generateStatusReport(CommandContext<CommandSourceStack> ctx) {
        try {
            sendMessage(ctx.getSource(), "&6&l=== Generating Status Report ===");
            sendMessage(ctx.getSource(), "&eCompiling comprehensive system analysis...");
            
            String report = statusMonitor.generateStatusReport();
            String[] lines = report.split("\n");
            
            sendMessage(ctx.getSource(), "");
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    sendMessage(ctx.getSource(), "");
                } else if (line.startsWith("===")) {
                    sendMessage(ctx.getSource(), "&6&l" + line);
                } else if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        sendMessage(ctx.getSource(), "&e" + parts[0] + ": &a" + parts[1].trim());
                    } else {
                        sendMessage(ctx.getSource(), "&7" + line);
                    }
                } else {
                    sendMessage(ctx.getSource(), "&7" + line);
                }
            }
            
            sendMessage(ctx.getSource(), "");
            sendMessage(ctx.getSource(), "&aStatus report generation completed successfully!");
            
        } catch (Exception e) {
            sendMessage(ctx.getSource(), "&cError generating status report: " + e.getMessage());
            LOGGER.error("Error in status report command", e);
        }
        
        return 1;
    }
    
    /**
     * Show status history
     */
    private static int showStatusHistory(CommandContext<CommandSourceStack> ctx, int hours) {
        try {
            sendMessage(ctx.getSource(), "&6&l=== Status History (Last " + hours + " hours) ===");
            sendMessage(ctx.getSource(), "&7Historical system performance data");
            sendMessage(ctx.getSource(), "");
            
            // This is a placeholder - in a real implementation, we would filter by time
            var history = statusMonitor.getStatusHistory();
            
            if (history.isEmpty()) {
                sendMessage(ctx.getSource(), "&7No historical data available yet");
                sendMessage(ctx.getSource(), "&7History will accumulate as the system runs");
            } else {
                sendMessage(ctx.getSource(), "&eAvailable snapshots: &a" + history.size());
                sendMessage(ctx.getSource(), "&eOldest snapshot: &a" + (history.isEmpty() ? "None" : 
                    history.get(0).getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
                sendMessage(ctx.getSource(), "&eNewest snapshot: &a" + (history.isEmpty() ? "None" : 
                    history.get(history.size() - 1).getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
                
                // Show trend analysis
                if (history.size() >= 2) {
                    double oldestScore = history.get(0).getHealthScore();
                    double newestScore = history.get(history.size() - 1).getHealthScore();
                    double trend = newestScore - oldestScore;
                    
                    sendMessage(ctx.getSource(), "&eHealth Trend: " + 
                        (trend > 0 ? "&a↗ Improving" : trend < 0 ? "&c↘ Declining" : "&7→ Stable") +
                        " &7(" + String.format("%+.1f%%", trend) + ")");
                }
            }
            
            sendMessage(ctx.getSource(), "");
            sendMessage(ctx.getSource(), "&7Detailed history analysis coming in future updates");
            
        } catch (Exception e) {
            sendMessage(ctx.getSource(), "&cError retrieving status history: " + e.getMessage());
            LOGGER.error("Error in status history command", e);
        }
        
        return 1;
    }
    
    // Helper methods
    private static void sendMessage(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(colorize(message)), false);
    }
    
    private static String colorize(String message) {
        return message.replace("&0", "§0").replace("&1", "§1").replace("&2", "§2").replace("&3", "§3")
                     .replace("&4", "§4").replace("&5", "§5").replace("&6", "§6").replace("&7", "§7")
                     .replace("&8", "§8").replace("&9", "§9").replace("&a", "§a").replace("&b", "§b")
                     .replace("&c", "§c").replace("&d", "§d").replace("&e", "§e").replace("&f", "§f")
                     .replace("&l", "§l").replace("&m", "§m").replace("&n", "§n").replace("&o", "§o")
                     .replace("&r", "§r").replace("&k", "§k");
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    private static String formatUptime(long uptimeMs) {
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
    
    private static String getResourceHealth(double usagePercent) {
        if (usagePercent < 50) return "Excellent";
        if (usagePercent < 70) return "Good";
        if (usagePercent < 85) return "Fair";
        if (usagePercent < 95) return "Poor";
        return "Critical";
    }
    
    private static double calculateMemoryHealthScore(double usagePercent) {
        if (usagePercent < 50) return 100.0;
        if (usagePercent < 70) return 90.0;
        if (usagePercent < 85) return 70.0;
        if (usagePercent < 95) return 40.0;
        return 10.0;
    }
    
    private static String getStatusColor(SystemStatusMonitor.ComponentState state) {
        return switch (state) {
            case ACTIVE -> "&a";
            case WARNING -> "&e";
            case ERROR -> "&c";
            case STOPPED -> "&7";
            case STARTING, STOPPING -> "&6";
            default -> "&f";
        };
    }
    
    private static String formatMetricName(String metricName) {
        String formatted = metricName.replace(".", " ").replace("_", " ");
        // Convert to title case
        String[] words = formatted.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        return result.toString().trim();
    }
}
