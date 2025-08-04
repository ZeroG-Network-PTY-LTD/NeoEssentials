package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.analytics.AnalyticsManager;
import com.zerog.neoessentials.analytics.AnalyticsReport;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Commands for viewing and managing analytics data
 */
public class AnalyticsCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsCommands.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("analytics")
            .requires(source -> source.hasPermission(3)) // OP level 3 required
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
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer player = source.getPlayerOrException();
            AnalyticsManager analytics = AnalyticsManager.getInstance();
            
            Map<String, Object> stats = analytics.getRealtimeStats();
            
            MessageUtil.sendMessage(player, "&6&l=== Real-time Analytics Stats ===");
            MessageUtil.sendMessage(player, "&e📊 Total Commands: &f" + stats.get("totalCommands"));
            MessageUtil.sendMessage(player, "&e👥 Active Players: &f" + stats.get("activePlayers"));
            MessageUtil.sendMessage(player, "&e📋 Total Events: &f" + stats.get("totalEvents"));
            MessageUtil.sendMessage(player, "&e🔧 Features Tracked: &f" + stats.get("featuresTracked"));
            MessageUtil.sendMessage(player, "&e⚡ Analytics Enabled: &f" + stats.get("analyticsEnabled"));
            MessageUtil.sendMessage(player, "&e🕒 Last Update: &f" + stats.get("lastUpdate"));
            
            @SuppressWarnings("unchecked")
            Map<String, Long> topCommands = (Map<String, Long>) stats.get("topCommands");
            if (topCommands != null && !topCommands.isEmpty()) {
                MessageUtil.sendMessage(player, "&e🏆 Top Commands:");
                topCommands.forEach((cmd, count) -> 
                    MessageUtil.sendMessage(player, "&f  • &a" + cmd + "&f: &e" + count + " uses"));
            }
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error showing realtime stats: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error retrieving analytics stats: " + e.getMessage()));
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
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer player = source.getPlayerOrException();
            AnalyticsManager analytics = AnalyticsManager.getInstance();
            
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(hours);
            
            MessageUtil.sendMessage(player, "&6Generating " + reportType + " analytics report...");
            
            AnalyticsReport report = analytics.generateReport(reportType, startTime, endTime);
            String formattedReport = report.generateFormattedReport();
            
            // Split report into chunks for chat (Minecraft chat has line limits)
            String[] lines = formattedReport.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    MessageUtil.sendMessage(player, "&f" + line);
                }
            }
            
            MessageUtil.sendMessage(player, "&aReport generated successfully!");
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error generating analytics report: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error generating report: " + e.getMessage()));
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
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer player = source.getPlayerOrException();
            AnalyticsManager analytics = AnalyticsManager.getInstance();
            
            var commandStats = analytics.getCommandUsageStats();
            
            MessageUtil.sendMessage(player, "&6&l=== Command Usage Statistics ===");
            MessageUtil.sendMessage(player, "&eShowing top " + limit + " commands:");
            
            commandStats.entrySet().stream()
                .sorted(Map.Entry.<String, java.util.concurrent.atomic.AtomicLong>comparingByValue(
                    (a, b) -> Long.compare(b.get(), a.get())))
                .limit(limit)
                .forEach(entry -> 
                    MessageUtil.sendMessage(player, String.format("&f  %d. &a%s &f- &e%,d uses", 
                        1, entry.getKey(), entry.getValue().get())));
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error showing command stats: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error retrieving command statistics: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int showPlayerStats(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer player = source.getPlayerOrException();
            AnalyticsManager analytics = AnalyticsManager.getInstance();
            
            var playerSessions = analytics.getPlayerSessions();
            
            MessageUtil.sendMessage(player, "&6&l=== Player Session Statistics ===");
            MessageUtil.sendMessage(player, "&eActive Sessions: &f" + playerSessions.size());
            
            if (!playerSessions.isEmpty()) {
                MessageUtil.sendMessage(player, "&eActive Players:");
                playerSessions.values().forEach(session -> 
                    MessageUtil.sendMessage(player, String.format("&f  • &a%s &f- &e%d min session, %d commands", 
                        session.getPlayerName(), session.getSessionDurationMinutes(), session.getCommandsExecuted())));
            }
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error showing player stats: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error retrieving player statistics: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int showPerformanceStats(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer player = source.getPlayerOrException();
            AnalyticsManager analytics = AnalyticsManager.getInstance();
            
            var performanceMetrics = analytics.getPerformanceMetrics();
            
            MessageUtil.sendMessage(player, "&6&l=== Performance Statistics ===");
            MessageUtil.sendMessage(player, "&eTracked Operations: &f" + performanceMetrics.size());
            
            if (!performanceMetrics.isEmpty()) {
                MessageUtil.sendMessage(player, "&eTop Performance Metrics:");
                performanceMetrics.values().stream()
                    .sorted((a, b) -> Long.compare(b.getExecutionCount(), a.getExecutionCount()))
                    .limit(10)
                    .forEach(metric -> 
                        MessageUtil.sendMessage(player, String.format("&f  • &a%s &f- &e%,d calls, avg: %.2fms", 
                            metric.getName(), metric.getExecutionCount(), metric.getAverageExecutionTime())));
            }
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error showing performance stats: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error retrieving performance statistics: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int showFeatureStats(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer player = source.getPlayerOrException();
            AnalyticsManager analytics = AnalyticsManager.getInstance();
            
            var featureStats = analytics.getFeatureStats();
            
            MessageUtil.sendMessage(player, "&6&l=== Feature Usage Statistics ===");
            MessageUtil.sendMessage(player, "&eTracked Features: &f" + featureStats.size());
            
            if (!featureStats.isEmpty()) {
                MessageUtil.sendMessage(player, "&eMost Used Features:");
                featureStats.values().stream()
                    .sorted((a, b) -> Long.compare(b.getTotalUsage(), a.getTotalUsage()))
                    .limit(10)
                    .forEach(feature -> 
                        MessageUtil.sendMessage(player, String.format("&f  • &a%s &f- &e%,d uses (last: %s)", 
                            feature.getFeatureName(), feature.getTotalUsage(), 
                            feature.getLastUsed().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")))));
            }
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error showing feature stats: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error retrieving feature statistics: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int clearAnalyticsData(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer player = source.getPlayerOrException();
            AnalyticsManager analytics = AnalyticsManager.getInstance();
            
            // Clear all analytics data
            analytics.getCommandUsageStats().clear();
            analytics.getPlayerSessions().clear();
            analytics.getEventHistory().clear();
            analytics.getPerformanceMetrics().clear();
            analytics.getFeatureStats().clear();
            
            MessageUtil.sendMessage(player, "&aAll analytics data has been cleared successfully!");
            LOGGER.info("Analytics data cleared by {}", source.getTextName());
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error clearing analytics data: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error clearing analytics data: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int toggleAnalytics(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer player = source.getPlayerOrException();
            AnalyticsManager analytics = AnalyticsManager.getInstance();
            
            boolean currentState = analytics.isAnalyticsEnabled();
            analytics.setAnalyticsEnabled(!currentState);
            
            String status = analytics.isAnalyticsEnabled() ? "&aENABLED" : "&cDISABLED";
            MessageUtil.sendMessage(player, "&6Analytics tracking is now " + status);
            
            LOGGER.info("Analytics tracking {} by {}", 
                analytics.isAnalyticsEnabled() ? "enabled" : "disabled", source.getTextName());
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error toggling analytics: " + e.getMessage(), e);
            context.getSource().sendFailure(Component.literal("Error toggling analytics: " + e.getMessage()));
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
