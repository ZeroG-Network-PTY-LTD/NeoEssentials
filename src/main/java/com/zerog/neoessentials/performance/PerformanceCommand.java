package com.zerog.neoessentials.performance;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.integration.ErrorHandlingIntegration;
import com.zerog.neoessentials.performance.AsyncOperationManager.AsyncStats;
import com.zerog.neoessentials.performance.AsyncOperationManager.ExecutorStats;
import com.zerog.neoessentials.util.MessageUtil;
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
            
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.performance.stats.header"), false);
            
            // Basic performance metrics
            PerformanceManager.PerformanceStats stats = manager.getPerformanceStats();
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.performance.command_time", 
                String.format("%.2f", stats.getAverageCommandTime())), false);
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.performance.total_commands", 
                String.valueOf(stats.getTotalCommands())), false);
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.performance.memory_usage", 
                String.format("%.1f", stats.getMemoryUsage())), false);
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.performance.cache_size", 
                String.valueOf(stats.getCacheSize())), false);
            
            return 1;
        });
    }
    
    private static int showMemoryInfo(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeCommand(context.getSource(), "Memory Info", (source) -> {
            Runtime runtime = Runtime.getRuntime();
            
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.performance.memory.header"), false);
            
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.performance.memory.used", 
                String.format("%.1f", usedMemory / 1024.0 / 1024.0)), false);
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.performance.memory.free", 
                String.format("%.1f", freeMemory / 1024.0 / 1024.0)), false);
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.performance.memory.max", 
                String.format("%.1f", maxMemory / 1024.0 / 1024.0)), false);
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.performance.memory.usage", 
                String.format("%.1f", (usedMemory * 100.0) / maxMemory)), false);
            
            return 1;
        });
    }
    
    private static int clearCache(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeCommand(context.getSource(), "Clear Cache", (source) -> {
            PerformanceManager manager = PerformanceManager.getInstance();
            
            manager.clearCache();
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.performance.cache.cleared"), false);
            
            return 1;
        });
    }
    
    private static int showCacheInfo(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeCommand(context.getSource(), "Cache Info", (source) -> {
            PerformanceManager manager = PerformanceManager.getInstance();
            
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.performance.cache.header"), false);
            
            PerformanceManager.PerformanceStats stats = manager.getPerformanceStats();
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.performance.cache.entries", 
                String.valueOf(stats.getCacheSize())), false);
            
            return 1;
        });
    }
    
    private static int showAsyncStats(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeCommand(context.getSource(), "Async Stats", (source) -> {
            AsyncOperationManager asyncManager = AsyncOperationManager.getInstance();
            
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.performance.async.header"), false);
            
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
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.performance.async.executor", 
            stats.getName()), false);
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.performance.async.threads", 
            String.valueOf(stats.getActiveThreads()), 
            String.valueOf(stats.getCorePoolSize()), 
            String.valueOf(stats.getMaxPoolSize())), false);
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.performance.async.tasks", 
            String.valueOf(stats.getCompletedTasks()), 
            String.valueOf(stats.getTotalTasks()), 
            String.valueOf(stats.getQueueSize())), false);
    }
    
    private static int forceGarbageCollection(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeCommand(context.getSource(), "Force GC", (source) -> {
            Runtime runtime = Runtime.getRuntime();
            
            long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
            System.gc();
            Thread.yield(); // Give GC a chance to run
            long afterMemory = runtime.totalMemory() - runtime.freeMemory();
            
            long freedMemory = beforeMemory - afterMemory;
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.performance.gc.completed", 
                String.format("%.1f", freedMemory / 1024.0 / 1024.0)), false);
            
            return 1;
        });
    }
}
