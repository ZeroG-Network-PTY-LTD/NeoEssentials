package com.zerog.neoessentials.commands.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.integration.ErrorHandlingIntegration;
import com.zerog.neoessentials.performance.PerformanceManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Map;

/**
 * Performance monitoring and management commands
 * Provides admin tools for performance analysis and optimization
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class PerformanceCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("performance")
            .requires(source -> source.hasPermission(3)) // Admin only
            .then(Commands.literal("stats")
                .executes(PerformanceCommand::showPerformanceStats))
            .then(Commands.literal("memory")
                .executes(PerformanceCommand::showMemoryInfo))
            .then(Commands.literal("cache")
                .then(Commands.literal("clear")
                    .executes(PerformanceCommand::clearCache))
                .then(Commands.literal("stats")
                    .executes(PerformanceCommand::showCacheStats)))
            .then(Commands.literal("monitoring")
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(PerformanceCommand::setMonitoring)))
            .then(Commands.literal("gc")
                .executes(PerformanceCommand::runGarbageCollection))
        );
    }
    
    /**
     * Show comprehensive performance statistics
     */
    private static int showPerformanceStats(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeWithPermission(
            context.getSource(),
            "performance stats",
            "neoessentials.admin.performance",
            (source) -> {
                PerformanceManager.PerformanceStats stats = PerformanceManager.getInstance().getPerformanceStats();
                
                source.sendSuccess(() -> Component.literal("§b=== NeoEssentials Performance Statistics ==="), false);
                source.sendSuccess(() -> Component.literal(""), false);
                
                // Command statistics
                source.sendSuccess(() -> Component.literal("§6📊 Command Performance:"), false);
                source.sendSuccess(() -> Component.literal(
                    String.format("§7Total Commands Executed: §e%,d", stats.getTotalCommands())), false);
                source.sendSuccess(() -> Component.literal(
                    String.format("§7Average Execution Time: §e%.2fms", stats.getAverageCommandTime())), false);
                
                // Memory statistics
                source.sendSuccess(() -> Component.literal(""), false);
                source.sendSuccess(() -> Component.literal("§6🧠 Memory Usage:"), false);
                source.sendSuccess(() -> Component.literal(
                    String.format("§7Memory Usage: §e%.1f%%", stats.getMemoryUsage())), false);
                
                Map<String, Object> systemMetrics = stats.getSystemMetrics();
                source.sendSuccess(() -> Component.literal(
                    String.format("§7Heap Used: §e%sMB / %sMB", 
                        systemMetrics.get("heapUsed"), systemMetrics.get("heapMax"))), false);
                
                // Cache statistics
                source.sendSuccess(() -> Component.literal(""), false);
                source.sendSuccess(() -> Component.literal("§6💾 Cache Performance:"), false);
                source.sendSuccess(() -> Component.literal(
                    String.format("§7Cache Size: §e%d / %s entries", 
                        stats.getCacheSize(), systemMetrics.get("cacheMaxSize"))), false);
                
                // Top slow commands
                source.sendSuccess(() -> Component.literal(""), false);
                source.sendSuccess(() -> Component.literal("§6🐌 Slowest Commands:"), false);
                stats.getSlowestCommands().forEach((command, avgTime) -> 
                    source.sendSuccess(() -> Component.literal(
                        String.format("§7  %s: §c%.2fms", command, avgTime)), false));
                
                // Most used commands
                source.sendSuccess(() -> Component.literal(""), false);
                source.sendSuccess(() -> Component.literal("§6🔥 Most Used Commands:"), false);
                stats.getMostUsedCommands().forEach((command, count) -> 
                    source.sendSuccess(() -> Component.literal(
                        String.format("§7  %s: §a%,d times", command, count)), false));
                
                return 1;
            }
        );
    }
    
    /**
     * Show detailed memory information
     */
    private static int showMemoryInfo(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeWithPermission(
            context.getSource(),
            "performance memory",
            "neoessentials.admin.performance",
            (source) -> {
                PerformanceManager.PerformanceStats stats = PerformanceManager.getInstance().getPerformanceStats();
                Map<String, Object> systemMetrics = stats.getSystemMetrics();
                
                source.sendSuccess(() -> Component.literal("§b=== Memory Information ==="), false);
                source.sendSuccess(() -> Component.literal(""), false);
                
                // Heap memory
                source.sendSuccess(() -> Component.literal("§6🧠 Heap Memory:"), false);
                long heapUsed = (Long) systemMetrics.get("heapUsed");
                long heapMax = (Long) systemMetrics.get("heapMax");
                double heapPercent = (Double) systemMetrics.get("heapUsagePercent");
                
                source.sendSuccess(() -> Component.literal(
                    String.format("§7Used: §e%,dMB", heapUsed)), false);
                source.sendSuccess(() -> Component.literal(
                    String.format("§7Max: §e%,dMB", heapMax)), false);
                source.sendSuccess(() -> Component.literal(
                    String.format("§7Usage: §e%.1f%%", heapPercent)), false);
                source.sendSuccess(() -> Component.literal(
                    String.format("§7Free: §a%,dMB", heapMax - heapUsed)), false);
                
                // Garbage collection
                source.sendSuccess(() -> Component.literal(""), false);
                source.sendSuccess(() -> Component.literal("§6🗑️ Garbage Collection:"), false);
                long totalGcTime = (Long) systemMetrics.get("totalGcTime");
                source.sendSuccess(() -> Component.literal(
                    String.format("§7Total GC Time: §e%,dms", totalGcTime)), false);
                
                // Memory status
                source.sendSuccess(() -> Component.literal(""), false);
                source.sendSuccess(() -> Component.literal("§6📊 Memory Status:"), false);
                if (heapPercent < 70) {
                    source.sendSuccess(() -> Component.literal("§a✅ Memory usage is healthy"), false);
                } else if (heapPercent < 85) {
                    source.sendSuccess(() -> Component.literal("§e⚠️ Memory usage is moderate"), false);
                } else {
                    source.sendSuccess(() -> Component.literal("§c❌ Memory usage is high - consider optimization"), false);
                }
                
                return 1;
            }
        );
    }
    
    /**
     * Clear performance cache
     */
    private static int clearCache(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeWithPermission(
            context.getSource(),
            "performance cache clear",
            "neoessentials.admin.performance",
            (source) -> {
                PerformanceManager.getInstance().clearCache();
                source.sendSuccess(() -> Component.literal("§a✅ Performance cache cleared successfully!"), false);
                return 1;
            }
        );
    }
    
    /**
     * Show cache statistics
     */
    private static int showCacheStats(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeWithPermission(
            context.getSource(),
            "performance cache stats",
            "neoessentials.admin.performance",
            (source) -> {
                PerformanceManager.PerformanceStats stats = PerformanceManager.getInstance().getPerformanceStats();
                Map<String, Object> systemMetrics = stats.getSystemMetrics();
                
                source.sendSuccess(() -> Component.literal("§b=== Cache Statistics ==="), false);
                source.sendSuccess(() -> Component.literal(""), false);
                
                int cacheSize = (Integer) systemMetrics.get("cacheSize");
                int cacheMaxSize = (Integer) systemMetrics.get("cacheMaxSize");
                double cacheUsage = (double) cacheSize / cacheMaxSize * 100;
                
                source.sendSuccess(() -> Component.literal(
                    String.format("§7Cache Entries: §e%d / %d", cacheSize, cacheMaxSize)), false);
                source.sendSuccess(() -> Component.literal(
                    String.format("§7Cache Usage: §e%.1f%%", cacheUsage)), false);
                
                if (cacheUsage < 70) {
                    source.sendSuccess(() -> Component.literal("§a✅ Cache usage is optimal"), false);
                } else if (cacheUsage < 90) {
                    source.sendSuccess(() -> Component.literal("§e⚠️ Cache usage is moderate"), false);
                } else {
                    source.sendSuccess(() -> Component.literal("§c❌ Cache is nearly full - automatic cleanup will occur"), false);
                }
                
                return 1;
            }
        );
    }
    
    /**
     * Enable or disable performance monitoring
     */
    private static int setMonitoring(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeWithPermission(
            context.getSource(),
            "performance monitoring",
            "neoessentials.admin.performance",
            (source) -> {
                boolean enabled = BoolArgumentType.getBool(context, "enabled");
                PerformanceManager.getInstance().setPerformanceMonitoring(enabled);
                
                source.sendSuccess(() -> Component.literal(
                    String.format("§a✅ Performance monitoring %s!", enabled ? "enabled" : "disabled")), false);
                
                return 1;
            }
        );
    }
    
    /**
     * Manually trigger garbage collection
     */
    private static int runGarbageCollection(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeWithPermission(
            context.getSource(),
            "performance gc",
            "neoessentials.admin.performance",
            (source) -> {
                source.sendSuccess(() -> Component.literal("§e⏳ Running garbage collection..."), false);
                
                // Get memory before GC
                Runtime runtime = Runtime.getRuntime();
                long usedBefore = runtime.totalMemory() - runtime.freeMemory();
                
                // Run GC
                System.gc();
                
                // Wait a moment for GC to complete
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Get memory after GC
                long usedAfter = runtime.totalMemory() - runtime.freeMemory();
                long freedMemory = usedBefore - usedAfter;
                
                source.sendSuccess(() -> Component.literal(
                    String.format("§a✅ Garbage collection completed! Freed: %,d bytes", freedMemory)), false);
                
                return 1;
            }
        );
    }
}
