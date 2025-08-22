package com.zerog.neoessentials.commands;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.analytics.AnalyticsManager;
import com.zerog.neoessentials.analytics.AnalyticsReport;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.localization.LanguageManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Commands for viewing and managing analytics data
 */
public class AnalyticsCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsCommands.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("analytics")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC)) // OP level 3 required
            .then(Commands.literal("stats")
                .executes(AnalyticsCommands::showRealtimeStats))
            .then(Commands.literal("report")
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        builder.suggest("daily");
                        builder.suggest("weekly");
                        builder.suggest("monthly");
                        builder.suggest("hourly");
                        return builder.buildFuture();
                    })
                    .executes(AnalyticsCommands::generateReport)
                    .then(Commands.argument("hours", IntegerArgumentType.integer(1, 720))
                        .executes(AnalyticsCommands::generateCustomReport))))
            .then(Commands.literal("commands")
                .executes(AnalyticsCommands::showCommandStats)
                .then(Commands.argument("limit", IntegerArgumentType.integer(1, 50))
                    .executes(AnalyticsCommands::showCommandStatsWithLimit)))
            .then(Commands.literal("players")
                .executes(AnalyticsCommands::showPlayerStats))
            .then(Commands.literal("performance")
                .executes(AnalyticsCommands::showPerformanceStats))
            .then(Commands.literal("features")
                .executes(AnalyticsCommands::showFeatureStats))
            .then(Commands.literal("clear")
                .executes(AnalyticsCommands::clearAnalyticsData))
            .then(Commands.literal("toggle")
                .executes(AnalyticsCommands::toggleAnalytics))
        );
    }
    
    private static int showRealtimeStats(CommandContext<CommandSourceStack> context) {
        ServerPlayer playerForError = null;
        try {
            CommandSourceStack source = context.getSource();
            final ServerPlayer player = source.getPlayerOrException();
            playerForError = player;
            AnalyticsManager analytics = AnalyticsManager.getInstance();
            Map<String, Object> stats = analytics.getRealtimeStats();
            LanguageManager lang = LanguageManager.getInstance();
            lang.getMessage(player, "analytics.stats.header");
            MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.stats.header"));
            MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.stats.total_commands", stats.get("totalCommands")));
            MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.stats.active_players", stats.get("activePlayers")));
            MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.stats.total_events", stats.get("totalEvents")));
            MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.stats.features_tracked", stats.get("featuresTracked")));
            MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.stats.enabled", stats.get("analyticsEnabled")));
            MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.stats.last_update", stats.get("lastUpdate")));
            @SuppressWarnings("unchecked")
            Map<String, Long> topCommands = (Map<String, Long>) stats.get("topCommands");
            if (topCommands != null && !topCommands.isEmpty()) {
                MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.stats.top_commands.header"));
                topCommands.forEach((cmd, count) -> 
                    MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.stats.top_commands.entry", cmd, count)));
            }
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error showing realtime stats: " + e.getMessage(), e);
            if (playerForError != null) {
                context.getSource().sendFailure(Component.literal(LanguageManager.getInstance().getMessage(playerForError, "analytics.error.stats", e.getMessage())));
            } else {
                context.getSource().sendFailure(Component.literal("Error showing realtime stats: " + e.getMessage()));
            }
            return 0;
        }
    }
    
    private static int generateReport(CommandContext<CommandSourceStack> context) {
        String reportType = StringArgumentType.getString(context, "type");
        return generateReportForPeriod(context, reportType, getHoursForReportType(reportType));
    }
    
    private static int generateCustomReport(CommandContext<CommandSourceStack> context) {
        String reportType = StringArgumentType.getString(context, "type");
        int hours = IntegerArgumentType.getInteger(context, "hours");
        return generateReportForPeriod(context, reportType, hours);
    }
    
    private static int generateReportForPeriod(CommandContext<CommandSourceStack> context, String reportType, int hours) {
        ServerPlayer playerForError = null;
        try {
            CommandSourceStack source = context.getSource();
            final ServerPlayer player = source.getPlayerOrException();
            playerForError = player;
            AnalyticsManager analytics = AnalyticsManager.getInstance();
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(hours);
            LanguageManager lang = LanguageManager.getInstance();
            MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.report.generating", reportType));
            AnalyticsReport report = analytics.generateReport(reportType, startTime, endTime);
            String formattedReport = report.generateFormattedReport();
            String[] lines = formattedReport.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.report.line", line));
                }
            }
            MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.report.success"));
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error generating analytics report: " + e.getMessage(), e);
            if (playerForError != null) {
                context.getSource().sendFailure(Component.literal(LanguageManager.getInstance().getMessage(playerForError, "analytics.error.report", e.getMessage())));
            } else {
                context.getSource().sendFailure(Component.literal("Error generating analytics report: " + e.getMessage()));
            }
            return 0;
        }
    }
    
    private static int showCommandStats(CommandContext<CommandSourceStack> context) {
        return showCommandStatsWithLimit(context, 10);
    }
    
    private static int showCommandStatsWithLimit(CommandContext<CommandSourceStack> context) {
        int limit = IntegerArgumentType.getInteger(context, "limit");
        return showCommandStatsWithLimit(context, limit);
    }
    
    private static int showCommandStatsWithLimit(CommandContext<CommandSourceStack> context, int limit) {
        ServerPlayer playerForError = null;
        try {
            CommandSourceStack source = context.getSource();
            final ServerPlayer player = source.getPlayerOrException();
            playerForError = player;
            AnalyticsManager analytics = AnalyticsManager.getInstance();
            var commandStats = analytics.getCommandUsageStats();
            LanguageManager lang = LanguageManager.getInstance();
            MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.commands.header"));
            MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.commands.limit", limit));
            commandStats.entrySet().stream()
                .sorted(Map.Entry.<String, java.util.concurrent.atomic.AtomicLong>comparingByValue((a, b) -> Long.compare(b.get(), a.get())))
                .limit(limit)
                .forEach(entry -> 
                    MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.commands.entry", entry.getKey(), entry.getValue().get())));
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error showing command stats: " + e.getMessage(), e);
            if (playerForError != null) {
                context.getSource().sendFailure(Component.literal(LanguageManager.getInstance().getMessage(playerForError, "analytics.error.command_stats", e.getMessage())));
            } else {
                context.getSource().sendFailure(Component.literal("Error showing command stats: " + e.getMessage()));
            }
            return 0;
        }
    }
    
    private static int showPlayerStats(CommandContext<CommandSourceStack> context) {
        ServerPlayer playerForError = null;
        try {
            CommandSourceStack source = context.getSource();
            final ServerPlayer player = source.getPlayerOrException();
            playerForError = player;
            AnalyticsManager analytics = AnalyticsManager.getInstance();
            var playerSessions = analytics.getPlayerSessions();
            LanguageManager lang = LanguageManager.getInstance();
            MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.players.header"));
            MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.players.active_sessions", playerSessions.size()));
            if (!playerSessions.isEmpty()) {
                MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.players.active_players.header"));
                playerSessions.values().forEach(session -> 
                    MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.players.active_players.entry", session.getPlayerName(), session.getSessionDurationMinutes(), session.getCommandsExecuted())));
            }
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error showing player stats: " + e.getMessage(), e);
            if (playerForError != null) {
                context.getSource().sendFailure(Component.literal(LanguageManager.getInstance().getMessage(playerForError, "analytics.error.player_stats", e.getMessage())));
            } else {
                context.getSource().sendFailure(Component.literal("Error showing player stats: " + e.getMessage()));
            }
            return 0;
        }
    }
    
    private static int showPerformanceStats(CommandContext<CommandSourceStack> context) {
        ServerPlayer playerForError = null;
        try {
            CommandSourceStack source = context.getSource();
            final ServerPlayer player = source.getPlayerOrException();
            playerForError = player;
            AnalyticsManager analytics = AnalyticsManager.getInstance();
            var performanceMetrics = analytics.getPerformanceMetrics();
            LanguageManager lang = LanguageManager.getInstance();
            MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.performance.header"));
            MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.performance.tracked_operations", performanceMetrics.size()));
            if (!performanceMetrics.isEmpty()) {
                MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.performance.top_metrics.header"));
                performanceMetrics.values().stream()
                    .sorted((a, b) -> Long.compare(b.getExecutionCount(), a.getExecutionCount()))
                    .limit(10)
                    .forEach(metric -> 
                        MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.performance.top_metrics.entry", metric.getName(), metric.getExecutionCount(), metric.getAverageExecutionTime())));
            }
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error showing performance stats: " + e.getMessage(), e);
            if (playerForError != null) {
                context.getSource().sendFailure(Component.literal(LanguageManager.getInstance().getMessage(playerForError, "analytics.error.performance_stats", e.getMessage())));
            } else {
                context.getSource().sendFailure(Component.literal("Error showing performance stats: " + e.getMessage()));
            }
            return 0;
        }
    }
    
    private static int showFeatureStats(CommandContext<CommandSourceStack> context) {
        ServerPlayer playerForError = null;
        try {
            CommandSourceStack source = context.getSource();
            final ServerPlayer player = source.getPlayerOrException();
            playerForError = player;
            AnalyticsManager analytics = AnalyticsManager.getInstance();
            Map<String, com.zerog.neoessentials.analytics.FeatureUsageStats> featureStats = analytics.getFeatureStats();
            LanguageManager lang = LanguageManager.getInstance();
            MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.features.header"));
            MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.features.tracked_features", featureStats.size()));
            if (!featureStats.isEmpty()) {
                MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.features.top_features.header"));
                featureStats.values().stream()
                    .sorted((a, b) -> Long.compare(b.getTotalUsage(), a.getTotalUsage()))
                    .limit(10)
                    .forEach(stat -> 
                        MessageUtil.sendMessage(player, lang.getMessage(player, "analytics.features.top_features.entry", stat.getFeatureName(), stat.getTotalUsage())));
            }
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error showing feature stats: " + e.getMessage(), e);
            if (playerForError != null) {
                context.getSource().sendFailure(Component.literal(LanguageManager.getInstance().getMessage(playerForError, "analytics.error.feature_stats", e.getMessage())));
            } else {
                context.getSource().sendFailure(Component.literal("Error showing feature stats: " + e.getMessage()));
            }
            return 0;
        }
    }
    
    private static int clearAnalyticsData(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = null;
        try {
            CommandSourceStack source = context.getSource();
            player = source.getPlayerOrException();
            AnalyticsManager analytics = AnalyticsManager.getInstance();
            analytics.getCommandUsageStats().clear();
            analytics.getPlayerSessions().clear();
            analytics.getEventHistory().clear();
            analytics.getPerformanceMetrics().clear();
            analytics.getFeatureStats().clear();
            MessageUtil.sendMessage(player, LanguageManager.getInstance().getMessage(player, "analytics.clear.success"));
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error clearing analytics data: " + e.getMessage(), e);
            if (player != null) {
                context.getSource().sendFailure(Component.literal(LanguageManager.getInstance().getMessage(player, "analytics.error.clear_data", e.getMessage())));
            } else {
                context.getSource().sendFailure(Component.literal("Error clearing analytics data: " + e.getMessage()));
            }
            return 0;
        }
    }
    
    private static int toggleAnalytics(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = null;
        try {
            CommandSourceStack source = context.getSource();
            player = source.getPlayerOrException();
            AnalyticsManager analytics = AnalyticsManager.getInstance();
            boolean newEnabled = !analytics.isAnalyticsEnabled();
            analytics.setAnalyticsEnabled(newEnabled);
            String key = newEnabled ? "analytics.toggle.enabled" : "analytics.toggle.disabled";
            MessageUtil.sendMessage(player, LanguageManager.getInstance().getMessage(player, key));
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error toggling analytics: " + e.getMessage(), e);
            if (player != null) {
                context.getSource().sendFailure(Component.literal(LanguageManager.getInstance().getMessage(player, "analytics.error.toggle", e.getMessage())));
            } else {
                context.getSource().sendFailure(Component.literal("Error toggling analytics: " + e.getMessage()));
            }
            return 0;
        }
    }
    
    private static int getHoursForReportType(String reportType) {
        return switch (reportType.toLowerCase()) {
            case "hourly" -> 1;
            case "daily" -> 24;
            case "weekly" -> 168; // 24 * 7
            case "monthly" -> 720; // 24 * 30
            default -> 24; // Default to daily
        };
    }
}
