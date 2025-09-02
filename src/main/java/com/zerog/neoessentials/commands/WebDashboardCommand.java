package com.zerog.neoessentials.commands;

import com.zerog.neoessentials.web.WebDashboardManager;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.util.Map;

/**
 * Enhanced Web Dashboard Management Commands
 * Provides comprehensive web dashboard administration with advanced features
 */
public class WebDashboardCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("dashboard")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_USE))
            .then(Commands.literal("status")
                .executes(context -> {
                    showStatus(context.getSource());
                    return 1;
                }))
            .then(Commands.literal("start")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_MANAGE))
                .executes(context -> {
                    startDashboard(context.getSource());
                    return 1;
                }))
            .then(Commands.literal("stop")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_MANAGE))
                .executes(context -> {
                    stopDashboard(context.getSource());
                    return 1;
                }))
            .then(Commands.literal("restart")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_MANAGE))
                .executes(context -> {
                    restartDashboard(context.getSource());
                    return 1;
                }))
            .then(Commands.literal("config")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_ADMIN))
                .then(Commands.literal("show")
                    .executes(context -> {
                        showConfiguration(context.getSource());
                        return 1;
                    }))
                .then(Commands.literal("port")
                    .then(Commands.argument("port", IntegerArgumentType.integer(1024, 65535))
                        .executes(context -> {
                            setPort(context.getSource(), IntegerArgumentType.getInteger(context, "port"));
                            return 1;
                        })))
                .then(Commands.literal("theme")
                    .then(Commands.argument("theme", StringArgumentType.string())
                        .executes(context -> {
                            setTheme(context.getSource(), StringArgumentType.getString(context, "theme"));
                            return 1;
                        })))
                .then(Commands.literal("maxsessions")
                    .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                        .executes(context -> {
                            setMaxSessions(context.getSource(), IntegerArgumentType.getInteger(context, "count"));
                            return 1;
                        })))
                .then(Commands.literal("realtime")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            setRealTimeUpdates(context.getSource(), BoolArgumentType.getBool(context, "enabled"));
                            return 1;
                        }))))
            .then(Commands.literal("analytics")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_ANALYTICS))
                .executes(context -> {
                    showAnalytics(context.getSource());
                    return 1;
                }))
            .then(Commands.literal("performance")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_ANALYTICS))
                .executes(context -> {
                    showPerformance(context.getSource());
                    return 1;
                }))
            .then(Commands.literal("events")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_ANALYTICS))
                .executes(context -> {
                    showRecentEvents(context.getSource());
                    return 1;
                }))
            .then(Commands.literal("sessions")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_ADMIN))
                .executes(context -> {
                    showActiveSessions(context.getSource());
                    return 1;
                }))
            .then(Commands.literal("alerts")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_ANALYTICS))
                .executes(context -> {
                    showAlerts(context.getSource());
                    return 1;
                }))
            .then(Commands.literal("security")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_ADMIN))
                .executes(context -> {
                    showSecurityEvents(context.getSource());
                    return 1;
                }))
            .then(Commands.literal("widgets")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_ANALYTICS))
                .executes(context -> {
                    showWidgets(context.getSource());
                    return 1;
                }));
        
        dispatcher.register(command);
    }
    
    private static void showStatus(CommandSourceStack source) {
        WebDashboardManager manager = WebDashboardManager.getInstance();
        
        if (manager.isDashboardEnabled()) {
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.status.running")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.status.url", "http://localhost:" + manager.getPort() + "/")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.status.sessions", manager.getActiveSessionsCount())
            ), false);
        } else {
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.status.stopped")
            ), false);
        }
    }
    
    private static void startDashboard(CommandSourceStack source) {
        WebDashboardManager manager = WebDashboardManager.getInstance();
        if (manager.isDashboardEnabled()) {
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.start.already_running")
            ), false);
            return;
        }
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.start.starting")
        ), false);
        if (manager.start()) {
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.start.success")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.status.url", "http://localhost:" + manager.getPort() + "/")
            ), false);
            manager.addRealTimeEvent("SYSTEM", com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.event.started", source.getDisplayName().getString()), "INFO");
        } else {
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.start.failed")
            ), false);
        }
    }
    
    private static void stopDashboard(CommandSourceStack source) {
        WebDashboardManager manager = WebDashboardManager.getInstance();
        manager.stop();
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.stop.success")
        ), false);
        // Log the stop event
        manager.addRealTimeEvent("SYSTEM", com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.event.stopped", source.getDisplayName().getString()), "INFO");
    }

    private static void restartDashboard(CommandSourceStack source) {
        WebDashboardManager manager = WebDashboardManager.getInstance();
        if (manager.isDashboardEnabled()) {
            manager.stop();
        }
        if (manager.start()) {
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.restart.success")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.status.url", "http://localhost:" + manager.getPort() + "/")
            ), false);
            // Log the restart event
            manager.addRealTimeEvent("SYSTEM", com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.event.restarted", source.getDisplayName().getString()), "INFO");
        } else {
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.restart.failed")
            ), false);
        }
    }
    
    private static void setPort(CommandSourceStack source, int port) {
        WebDashboardManager manager = WebDashboardManager.getInstance();
        
        if (manager.isDashboardEnabled()) {
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.port.cannot_change_while_running")
            ), false);
            return;
        }
        
        manager.setPort(port);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.port.set_success", port)
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.port.start_hint")
        ), false);
    }
    
    private static void showAnalytics(CommandSourceStack source) {
        WebDashboardManager manager = WebDashboardManager.getInstance();
        Map<String, Object> analytics = manager.getShopAnalytics();
        Map<String, Object> economy = manager.getEconomyHealth();
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.analytics.header")
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.analytics.total_shops", analytics.get("total_shops"))
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.analytics.active_shops", analytics.get("active_shops"))
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.analytics.daily_transactions", analytics.get("daily_transactions"))
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.analytics.daily_revenue", String.format("%.2f", analytics.get("daily_revenue")))
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.analytics.economy_status", economy.get("economy_status"))
        ), false);
    }

    private static void showPerformance(CommandSourceStack source) {
        WebDashboardManager manager = WebDashboardManager.getInstance();
        Map<String, Object> performance = manager.getServerPerformance();
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.performance.header")
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.performance.tps", String.format("%.1f", performance.get("tps")))
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.performance.memory_usage", performance.get("memory_usage"))
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.performance.cpu_usage", performance.get("cpu_usage"))
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.performance.disk_usage", performance.get("disk_usage"))
        ), false);
    }
    
    @SuppressWarnings("unchecked")
    private static void showRecentEvents(CommandSourceStack source) {
        WebDashboardManager manager = WebDashboardManager.getInstance();
        Map<String, Object> data = manager.getDashboardData();
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.events.header")
        ), false);
        Object eventsObj = data.get("recent_events");
        if (eventsObj instanceof java.util.List) {
            java.util.List<Map<String, Object>> events = (java.util.List<Map<String, Object>>) eventsObj;
            if (events.isEmpty()) {
                source.sendSuccess(() -> Component.literal(
                    com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.events.none")
                ), false);
                return;
            }
            int count = 0;
            for (Map<String, Object> event : events) {
                if (count >= 5) break;
                String severity = (String) event.get("severity");
                String color = switch (severity) {
                    case "ERROR" -> "&c";
                    case "WARN" -> "&6";
                    case "INFO" -> "&a";
                    default -> "&7";
                };
                source.sendSuccess(() -> Component.literal(
                    com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.events.entry", color, event.get("type"), event.get("message"))
                ), false);
                count++;
            }
        } else {
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "neoessentials.webdashboard.events.not_available")
            ), false);
        }
    }
    
    private static void showConfiguration(CommandSourceStack source) {
        WebDashboardManager manager = WebDashboardManager.getInstance();
        
        source.sendSuccess(() -> Component.literal("§6Web Dashboard Configuration:"), false);
        source.sendSuccess(() -> Component.literal("§7Port: §b" + manager.getPort()), false);
        source.sendSuccess(() -> Component.literal("§7Running: " + 
            (manager.isDashboardEnabled() ? "§aYes" : "§cNo")), false);
        source.sendSuccess(() -> Component.literal("§7Session Count: §b" + manager.getActiveSessionsCount()), false);
    }
    
    private static void setTheme(CommandSourceStack source, String theme) {
        source.sendSuccess(() -> Component.literal("§eTheme configuration not yet implemented."), false);
        source.sendSuccess(() -> Component.literal("§7Available themes: dark, light, neo, classic"), false);
    }
    
    private static void setMaxSessions(CommandSourceStack source, int count) {
        source.sendSuccess(() -> Component.literal("§eMax sessions configuration not yet implemented."), false);
        source.sendSuccess(() -> Component.literal("§7Requested count: " + count), false);
    }
    
    private static void setRealTimeUpdates(CommandSourceStack source, boolean enabled) {
        source.sendSuccess(() -> Component.literal("§eReal-time updates configuration not yet implemented."), false);
        source.sendSuccess(() -> Component.literal("§7Requested state: " + (enabled ? "enabled" : "disabled")), false);
    }
    
    private static void showActiveSessions(CommandSourceStack source) {
        WebDashboardManager manager = WebDashboardManager.getInstance();
        
        source.sendSuccess(() -> Component.literal("§6Active Dashboard Sessions:"), false);
        source.sendSuccess(() -> Component.literal("§7Session Count: §b" + manager.getActiveSessionsCount()), false);
        source.sendSuccess(() -> Component.literal("§7Detailed session info not yet implemented."), false);
    }
    
    private static void showAlerts(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6Dashboard Alerts:"), false);
        source.sendSuccess(() -> Component.literal("§7Alert system not yet implemented."), false);
    }
    
    private static void showSecurityEvents(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6Security Events:"), false);
        source.sendSuccess(() -> Component.literal("§7Security event logging not yet implemented."), false);
    }
    
    private static void showWidgets(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6Dashboard Widgets:"), false);
        source.sendSuccess(() -> Component.literal("§7Widget system not yet implemented."), false);
    }
}
