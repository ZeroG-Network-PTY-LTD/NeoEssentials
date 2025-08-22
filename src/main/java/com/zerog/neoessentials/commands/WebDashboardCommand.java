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

import java.util.Map;

/**
 * Enhanced Web Dashboard Management Commands
 * Provides comprehensive web dashboard administration
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
            .then(Commands.literal("port")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_ADMIN))
                .then(Commands.argument("port", IntegerArgumentType.integer(1024, 65535))
                    .executes(context -> {
                        setPort(context.getSource(), IntegerArgumentType.getInteger(context, "port"));
                        return 1;
                    })))
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
                }));
        
        dispatcher.register(command);
    }
    
    private static void showStatus(CommandSourceStack source) {
        WebDashboardManager manager = WebDashboardManager.getInstance();
        
        if (manager.isDashboardEnabled()) {
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.status.running")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.status.url", "http://localhost:" + manager.getPort() + "/")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.status.sessions", manager.getActiveSessionsCount())
            ), false);
        } else {
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.status.stopped")
            ), false);
        }
    }
    
    private static void startDashboard(CommandSourceStack source) {
        WebDashboardManager manager = WebDashboardManager.getInstance();
        if (manager.isDashboardEnabled()) {
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.start.already_running")
            ), false);
            return;
        }
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.start.starting")
        ), false);
        if (manager.start()) {
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.start.success")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.status.url", "http://localhost:" + manager.getPort() + "/")
            ), false);
            manager.addRealTimeEvent("SYSTEM", com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.event.started", source.getDisplayName().getString()), "INFO");
        } else {
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.start.failed")
            ), false);
        }
    }
    
    private static void stopDashboard(CommandSourceStack source) {
        WebDashboardManager manager = WebDashboardManager.getInstance();
        manager.stop();
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.stop.success")
        ), false);
        // Log the stop event
        manager.addRealTimeEvent("SYSTEM", com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.event.stopped", source.getDisplayName().getString()), "INFO");
    }

    private static void restartDashboard(CommandSourceStack source) {
        WebDashboardManager manager = WebDashboardManager.getInstance();
        if (manager.isDashboardEnabled()) {
            manager.stop();
        }
        if (manager.start()) {
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.restart.success")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.status.url", "http://localhost:" + manager.getPort() + "/")
            ), false);
            // Log the restart event
            manager.addRealTimeEvent("SYSTEM", com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.event.restarted", source.getDisplayName().getString()), "INFO");
        } else {
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.restart.failed")
            ), false);
        }
    }
    
    private static void setPort(CommandSourceStack source, int port) {
        WebDashboardManager manager = WebDashboardManager.getInstance();
        
        if (manager.isDashboardEnabled()) {
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.port.cannot_change_while_running")
            ), false);
            return;
        }
        
        manager.setPort(port);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.port.set_success", port)
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.port.start_hint")
        ), false);
    }
    
    private static void showAnalytics(CommandSourceStack source) {
        WebDashboardManager manager = WebDashboardManager.getInstance();
        Map<String, Object> analytics = manager.getShopAnalytics();
        Map<String, Object> economy = manager.getEconomyHealth();
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.analytics.header")
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.analytics.total_shops", analytics.get("total_shops"))
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.analytics.active_shops", analytics.get("active_shops"))
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.analytics.daily_transactions", analytics.get("daily_transactions"))
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.analytics.daily_revenue", String.format("%.2f", analytics.get("daily_revenue")))
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.analytics.economy_status", economy.get("economy_status"))
        ), false);
    }

    private static void showPerformance(CommandSourceStack source) {
        WebDashboardManager manager = WebDashboardManager.getInstance();
        Map<String, Object> performance = manager.getServerPerformance();
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.performance.header")
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.performance.tps", String.format("%.1f", performance.get("tps")))
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.performance.memory_usage", performance.get("memory_usage"))
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.performance.cpu_usage", performance.get("cpu_usage"))
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.performance.disk_usage", performance.get("disk_usage"))
        ), false);
    }
    
    @SuppressWarnings("unchecked")
    private static void showRecentEvents(CommandSourceStack source) {
        WebDashboardManager manager = WebDashboardManager.getInstance();
        Map<String, Object> data = manager.getDashboardData();
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.events.header")
        ), false);
        Object eventsObj = data.get("recent_events");
        if (eventsObj instanceof java.util.List) {
            java.util.List<Map<String, Object>> events = (java.util.List<Map<String, Object>>) eventsObj;
            if (events.isEmpty()) {
                source.sendSuccess(() -> Component.literal(
                    com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.events.none")
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
                    com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.events.entry", color, event.get("type"), event.get("message"))
                ), false);
                count++;
            }
        } else {
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "webdashboard.events.not_available")
            ), false);
        }
    }
}
