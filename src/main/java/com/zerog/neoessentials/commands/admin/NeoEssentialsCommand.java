package com.zerog.neoessentials.commands.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.api.NeoEssentialsAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.utils.PerformanceMonitor;
import com.zerog.neoessentials.placeholders.PlaceholderManager;
import com.zerog.neoessentials.utils.TabCompletionUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * NeoEssentials administrative command
 * Provides configuration, monitoring, and management features
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class NeoEssentialsCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("neoessentials")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.CONFIG_ALL))
            .then(Commands.literal("info")
                .executes(NeoEssentialsCommand::showInfo))
            .then(Commands.literal("performance")
                .executes(NeoEssentialsCommand::showPerformance))
            .then(Commands.literal("reload")
                .executes(NeoEssentialsCommand::reloadConfig))
            .then(Commands.literal("placeholders")
                .executes(NeoEssentialsCommand::listPlaceholders))
            .then(Commands.literal("test")
                .then(Commands.argument("placeholder", StringArgumentType.greedyString())
                    .suggests(TabCompletionUtil.CONTEXT_AWARE)
                    .executes(NeoEssentialsCommand::testPlaceholder)))
            .then(Commands.literal("help")
                .executes(NeoEssentialsCommand::showHelp))
        );
    }
    
    private static int showInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        NeoEssentialsAPI api = NeoEssentialsAPI.getInstance();
        ServerPlayer player = source.getPlayer();
        source.sendSuccess(() -> Component.literal(
            player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.info.header") : "§6=== NeoEssentials Information ==="
        ), false);
        source.sendSuccess(() -> Component.literal(
            player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.info.mod_version", NeoEssentialsAPI.getModVersion()) : "§7Mod Version: §e" + NeoEssentialsAPI.getModVersion()
        ), false);
        source.sendSuccess(() -> Component.literal(
            player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.info.api_version", NeoEssentialsAPI.getAPIVersion()) : "§7API Version: §e" + NeoEssentialsAPI.getAPIVersion()
        ), false);
        source.sendSuccess(() -> Component.literal(
            player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.info.features.header") : "§7Available Features:"
        ), false);
        String[] features = {"homes", "economy", "warps", "kits", "messaging", "spawn", "moderation", "placeholders", "performance"};
        for (String feature : features) {
            boolean available = api.isFeatureAvailable(feature);
            String status = available ? "§a✓" : "§c✗";
            source.sendSuccess(() -> Component.literal(
                player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.info.feature", feature, available) : "  " + status + " §7" + feature
            ), false);
        }
        return 1;
    }
    
    private static int showPerformance(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        PerformanceMonitor.SystemMetrics systemMetrics = monitor.getSystemMetrics();
        PerformanceMonitor.PerformanceReport report = monitor.generateReport();
        ServerPlayer player = source.getPlayer();
        source.sendSuccess(() -> Component.literal(
            player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.performance.header") : "§6=== Performance Report ==="
        ), false);
        source.sendSuccess(() -> Component.literal(
            player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.performance.memory", systemMetrics.getFormattedHeapMemory(), systemMetrics.getHeapMemoryUsagePercent()) : "§7Memory Usage: §e" + systemMetrics.getFormattedHeapMemory() + " (§e" + String.format("%.1f%%", systemMetrics.getHeapMemoryUsagePercent()) + "§7)"
        ), false);
        source.sendSuccess(() -> Component.literal(
            player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.performance.load", systemMetrics.getSystemLoadAverage()) : "§7System Load: §e" + String.format("%.2f", systemMetrics.getSystemLoadAverage())
        ), false);
        source.sendSuccess(() -> Component.literal(
            player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.performance.uptime", systemMetrics.getFormattedUptime()) : "§7Uptime: §e" + systemMetrics.getFormattedUptime()
        ), false);
        source.sendSuccess(() -> Component.literal(
            player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.performance.cores", systemMetrics.getAvailableProcessors()) : "§7CPU Cores: §e" + systemMetrics.getAvailableProcessors()
        ), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal(
            player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.performance.command.header") : "§7Command Performance (Top 5):"
        ), false);
        List<PerformanceMonitor.CommandMetrics> commandMetrics = report.getCommandMetrics();
        int count = 0;
        for (PerformanceMonitor.CommandMetrics metrics : commandMetrics) {
            if (count >= 5) break;
            source.sendSuccess(() -> Component.literal(
                player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.performance.command.metric", metrics.getCommandName(), metrics.getTotalExecutions(), metrics.getAverageExecutionTime()) : String.format("  §e%s§7: §a%d §7executions, §e%.1fms §7avg", metrics.getCommandName(), metrics.getTotalExecutions(), metrics.getAverageExecutionTime())
            ), false);
            count++;
        }
        if (commandMetrics.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.performance.command.none") : "  §7No command metrics available"
            ), false);
        }
        return 1;
    }
    
    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        try {
            ConfigManager.getInstance().reloadAll();
            com.zerog.neoessentials.features.TabListManager.getInstance().reloadConfig();
            com.zerog.neoessentials.placeholders.PlaceholderManager.getInstance().reload();
            source.sendSuccess(() -> Component.literal(
                player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.reload.success") : "§aConfiguration reloaded successfully!"
            ), true);
        } catch (Exception e) {
            source.sendFailure(Component.literal(
                player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.reload.error", e.getMessage()) : "§cFailed to reload configuration: " + e.getMessage()
            ));
        }
        return 1;
    }
    
    private static int listPlaceholders(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
    PlaceholderManager placeholderManager = PlaceholderManager.getInstance();
    java.util.Set<String> placeholders = placeholderManager.getRegisteredPlaceholders();
        ServerPlayer player = source.getPlayer();
        source.sendSuccess(() -> Component.literal(
            player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.placeholders.header") : "§d=== Available Placeholders ==="
        ), false);
        source.sendSuccess(() -> Component.literal(
            player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.placeholders.total", placeholders.size()) : "§7Total: §e" + placeholders.size()
        ), false);
        source.sendSuccess(() -> Component.literal(""), false);
        String[] commonPlaceholders = {
            "%player%", "%displayname%", "%balance%", "%homes_count%",
            "%x%", "%y%", "%z%", "%world%", "%ping%", "%gamemode%"
        };
        source.sendSuccess(() -> Component.literal(
            player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.placeholders.common.header") : "§7Common Placeholders:"
        ), false);
        for (String placeholder : commonPlaceholders) {
            if (placeholders.contains(placeholder.replace("%", ""))) {
                source.sendSuccess(() -> Component.literal(
                    player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.placeholders.common.entry", placeholder) : "  §e" + placeholder
                ), false);
            }
        }
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal(
            player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.placeholders.test.help") : "§7Use §e/neoessentials test <placeholder> §7to test a placeholder"
        ), false);
        return 1;
    }
    
    private static int testPlaceholder(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String placeholderText = StringArgumentType.getString(context, "placeholder");
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal(
                "§cThis command can only be used by players"
            ));
            return 0;
        }
    PlaceholderManager placeholderManager = PlaceholderManager.getInstance();
    String result = placeholderManager.processPlaceholders(placeholderText, player);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.testplayer.input", placeholderText)
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.testplayer.output", result)
        ), false);
        return 1;
    }
    
    private static int showHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        source.sendSuccess(() -> Component.literal(
            player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.help.header") : "§6=== NeoEssentials Admin Commands ==="
        ), false);
        source.sendSuccess(() -> Component.literal(
            player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.help.info") : "§e/neoessentials info §7- Show mod information"
        ), false);
        source.sendSuccess(() -> Component.literal(
            player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.help.performance") : "§e/neoessentials performance §7- Show performance metrics"
        ), false);
        source.sendSuccess(() -> Component.literal(
            player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.help.reload") : "§e/neoessentials reload §7- Reload configuration"
        ), false);
        source.sendSuccess(() -> Component.literal(
            player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.help.placeholders") : "§e/neoessentials placeholders §7- List available placeholders"
        ), false);
        source.sendSuccess(() -> Component.literal(
            player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.help.test") : "§e/neoessentials test <text> §7- Test placeholder processing"
        ), false);
        source.sendSuccess(() -> Component.literal(
            player != null ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.help.help") : "§e/neoessentials help §7- Show this help"
        ), false);
        return 1;
    }
}
