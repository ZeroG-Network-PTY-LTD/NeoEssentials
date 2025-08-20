package com.zerog.neoessentials.commands.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.integration.ErrorHandlingIntegration;
import com.zerog.neoessentials.performance.PerformanceManager;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
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
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.PERFORMANCE_ADMIN))
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
                ServerPlayer player = source.getPlayerOrException();
                
                    source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.stats.header")), false);
                source.sendSuccess(() -> Component.literal(""), false);
                
                // Command statistics
                    source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.stats.command.header")), false);
                    source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.stats.command.total", stats.getTotalCommands())), false);
                    source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.stats.command.avg_time", stats.getAverageCommandTime())), false);
                
                // Memory statistics
                source.sendSuccess(() -> Component.literal(""), false);
                    source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.stats.memory.header")), false);
                    source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.stats.memory.usage", stats.getMemoryUsage())), false);
                
                Map<String, Object> systemMetrics = stats.getSystemMetrics();
                    source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.stats.memory.heap", systemMetrics.get("heapUsed"), systemMetrics.get("heapMax"))), false);
                
                // Cache statistics
                source.sendSuccess(() -> Component.literal(""), false);
                    source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.stats.cache.header")), false);
                    source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.stats.cache.size", stats.getCacheSize(), systemMetrics.get("cacheMaxSize"))), false);
                
                // Top slow commands
                source.sendSuccess(() -> Component.literal(""), false);
                    source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.stats.slow.header")), false);
                    stats.getSlowestCommands().forEach((command, avgTime) -> 
                        source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.stats.slow.entry", command, avgTime)), false));
                
                // Most used commands
                source.sendSuccess(() -> Component.literal(""), false);
                    source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.stats.most.header")), false);
                    stats.getMostUsedCommands().forEach((command, count) -> 
                        source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.stats.most.entry", command, count)), false));
                
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
                
                    ServerPlayer player = source.getPlayerOrException();
                    source.sendSuccess(() -> {
                        return Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.memory.header"));
                    }, false);
                    source.sendSuccess(() -> Component.literal(""), false);
                    source.sendSuccess(() -> {
                        return Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.memory.heap.header"));
                    }, false);
                    long heapUsed = (Long) systemMetrics.get("heapUsed");
                    long heapMax = (Long) systemMetrics.get("heapMax");
                    double heapPercent = (Double) systemMetrics.get("heapUsagePercent");
                    source.sendSuccess(() -> {
                        return Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.memory.heap.used", heapUsed));
                    }, false);
                    source.sendSuccess(() -> {
                        return Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.memory.heap.max", heapMax));
                    }, false);
                    source.sendSuccess(() -> {
                        return Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.memory.heap.usage", heapPercent));
                    }, false);
                    source.sendSuccess(() -> {
                        return Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.memory.heap.free", heapMax - heapUsed));
                    }, false);
                    source.sendSuccess(() -> Component.literal(""), false);
                    source.sendSuccess(() -> {
                        return Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.memory.gc.header"));
                    }, false);
                    long totalGcTime = (Long) systemMetrics.get("totalGcTime");
                    source.sendSuccess(() -> {
                        return Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.memory.gc.total", totalGcTime));
                    }, false);
                    source.sendSuccess(() -> Component.literal(""), false);
                    source.sendSuccess(() -> {
                        return Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.memory.status.header"));
                    }, false);
                    if (heapPercent < 70) {
                        source.sendSuccess(() -> {
                            return Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.memory.status.healthy"));
                        }, false);
                    } else if (heapPercent < 85) {
                        source.sendSuccess(() -> {
                            return Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.memory.status.moderate"));
                        }, false);
                    } else {
                        source.sendSuccess(() -> {
                            return Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.memory.status.high"));
                        }, false);
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
                ServerPlayer player = source.getPlayerOrException();
                source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.cache.cleared")), false);
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
                ServerPlayer player = source.getPlayerOrException();
                source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.monitoring.status", enabled)), false);
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
                ServerPlayer player = source.getPlayerOrException();
                source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.gc.running")), false);
                
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
                
                source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "performance.gc.completed", freedMemory)), false);
                
                return 1;
            }
        );
    }
}
