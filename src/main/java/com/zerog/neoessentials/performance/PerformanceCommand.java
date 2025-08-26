package com.zerog.neoessentials.performance;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.integration.ErrorHandlingIntegration;
import com.zerog.neoessentials.performance.AsyncOperationManager.AsyncStats;
import com.zerog.neoessentials.performance.AsyncOperationManager.ExecutorStats;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Admin commands for performance monitoring and management
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class PerformanceCommand {
    
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("performance")
            .requires(source -> source.hasPermission(3))
            .then(Commands.literal("stats")
                .executes(PerformanceCommand::showStats))
            .then(Commands.literal("memory")
                .executes(PerformanceCommand::showMemoryInfo))
            .then(Commands.literal("cache")
                .then(Commands.literal("clear")
                    .executes(PerformanceCommand::clearCache))
                .then(Commands.literal("info")
                    .executes(PerformanceCommand::showCacheInfo)))
            .then(Commands.literal("async")
                .executes(PerformanceCommand::showAsyncStats))
            .then(Commands.literal("gc")
                .executes(PerformanceCommand::forceGarbageCollection))
        );
    }
    
    private static int showStats(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeCommand(context.getSource(), "Performance Stats", (source) -> {
            PerformanceManager manager = PerformanceManager.getInstance();
            
            source.sendSuccess(() -> Component.literal("§6=== Performance Statistics ==="), false);
            
            // Basic performance metrics
            PerformanceManager.PerformanceStats stats = manager.getPerformanceStats();
            source.sendSuccess(() -> Component.literal(String.format(
                "§7Average Command Time: §e%.2fms", 
                stats.getAverageCommandTime())), false);
            source.sendSuccess(() -> Component.literal(String.format(
                "§7Total Commands: §e%d", 
                stats.getTotalCommands())), false);
            source.sendSuccess(() -> Component.literal(String.format(
                "§7Memory Usage: §e%.1f%%", 
                stats.getMemoryUsage())), false);
            source.sendSuccess(() -> Component.literal(String.format(
                "§7Cache Size: §e%d", 
                stats.getCacheSize())), false);
            
            return 1;
        });
    }
    
    private static int showMemoryInfo(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeCommand(context.getSource(), "Memory Info", (source) -> {
            Runtime runtime = Runtime.getRuntime();
            
            source.sendSuccess(() -> Component.literal("§6=== Memory Information ==="), false);
            
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            source.sendSuccess(() -> Component.literal(String.format(
                "§7Used Memory: §e%.1f MB", 
                usedMemory / 1024.0 / 1024.0)), false);
            source.sendSuccess(() -> Component.literal(String.format(
                "§7Free Memory: §e%.1f MB", 
                freeMemory / 1024.0 / 1024.0)), false);
            source.sendSuccess(() -> Component.literal(String.format(
                "§7Max Memory: §e%.1f MB", 
                maxMemory / 1024.0 / 1024.0)), false);
            source.sendSuccess(() -> Component.literal(String.format(
                "§7Memory Usage: §e%.1f%%", 
                (usedMemory * 100.0) / maxMemory)), false);
            
            return 1;
        });
    }
    
    private static int clearCache(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeCommand(context.getSource(), "Clear Cache", (source) -> {
            PerformanceManager manager = PerformanceManager.getInstance();
            
            manager.clearCache();
            source.sendSuccess(() -> Component.literal("§aCached data cleared"), false);
            
            return 1;
        });
    }
    
    private static int showCacheInfo(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeCommand(context.getSource(), "Cache Info", (source) -> {
            PerformanceManager manager = PerformanceManager.getInstance();
            
            source.sendSuccess(() -> Component.literal("§6=== Cache Information ==="), false);
            
            PerformanceManager.PerformanceStats stats = manager.getPerformanceStats();
            source.sendSuccess(() -> Component.literal(String.format(
                "§7Cache Size: §e%d entries", 
                stats.getCacheSize())), false);
            
            return 1;
        });
    }
    
    private static int showAsyncStats(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeCommand(context.getSource(), "Async Stats", (source) -> {
            AsyncOperationManager asyncManager = AsyncOperationManager.getInstance();
            
            source.sendSuccess(() -> Component.literal("§6=== Async Operation Statistics ==="), false);
            
            AsyncStats stats = asyncManager.getAsyncStats();
            
            displayExecutorStats(source, stats.getFileIOStats());
            displayExecutorStats(source, stats.getNetworkStats());
            displayExecutorStats(source, stats.getScheduledStats());
            displayExecutorStats(source, stats.getFileIOStats());
            displayExecutorStats(source, stats.getNetworkStats());
            displayExecutorStats(source, stats.getScheduledStats());
            
            return 1;
        });
    }
    
    private static void displayExecutorStats(CommandSourceStack source, ExecutorStats stats) {
        source.sendSuccess(() -> Component.literal(String.format(
            "§7%s Executor:", stats.getName())), false);
        source.sendSuccess(() -> Component.literal(String.format(
            "  §7Active: §e%d§7/§e%d §7(Max: §e%d§7)", 
            stats.getActiveThreads(), stats.getCorePoolSize(), stats.getMaxPoolSize())), false);
        source.sendSuccess(() -> Component.literal(String.format(
            "  §7Completed: §e%d§7/§e%d §7Queue: §e%d", 
            stats.getCompletedTasks(), stats.getTotalTasks(), stats.getQueueSize())), false);
    }
    
    private static int forceGarbageCollection(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeCommand(context.getSource(), "Force GC", (source) -> {
            Runtime runtime = Runtime.getRuntime();
            
            long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
            System.gc();
            Thread.yield(); // Give GC a chance to run
            long afterMemory = runtime.totalMemory() - runtime.freeMemory();
            
            long freedMemory = beforeMemory - afterMemory;
            source.sendSuccess(() -> Component.literal(String.format(
                "§aGarbage collection completed. Freed: §e%.1f MB", 
                freedMemory / 1024.0 / 1024.0)), false);
            
            return 1;
        });
    }
}
