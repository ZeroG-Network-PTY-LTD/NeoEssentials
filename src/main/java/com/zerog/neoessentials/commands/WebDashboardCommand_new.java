package com.zerog.neoessentials.commands;

import com.zerog.neoessentials.web.WebDashboardManager;
import com.zerog.neoessentials.managers.FeatureManager;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.Command;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import java.util.Map;

/**
 * Enhanced Web Dashboard Management Commands
 * Provides comprehensive web dashboard administration with advanced features
 */
public class WebDashboardCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("dashboard")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_USE))
            .executes(context -> showStatus(context))
            .then(Commands.literal("status")
                .executes(context -> showStatus(context)))
            .then(Commands.literal("start")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_MANAGE))
                .executes(context -> startDashboard(context)))
            .then(Commands.literal("stop")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_MANAGE))
                .executes(context -> stopDashboard(context)))
            .then(Commands.literal("restart")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_MANAGE))
                .executes(context -> restartDashboard(context)))
            .then(Commands.literal("config")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_CONFIG))
                .executes(context -> showConfig(context)))
            .then(Commands.literal("analytics")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_VIEW))
                .executes(context -> showAnalytics(context)))
            .then(Commands.literal("sessions")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_VIEW))
                .executes(context -> showSessions(context)))
            .then(Commands.literal("security")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_SECURITY))
                .executes(context -> showSecurityEvents(context)))
            .then(Commands.literal("widgets")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_CONFIG))
                .executes(context -> showWidgets(context)));
        
        // Register both "dashboard" and "webdashboard" aliases
        dispatcher.register(command);
        dispatcher.register(Commands.literal("webdashboard")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_USE))
                .redirect(command.build()));
    }
    
    private static int showStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        WebDashboardManager manager = FeatureManager.getInstance().getWebDashboard();
        
        if (manager.isRunning()) {
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.status.running"), false);
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.status.url", "http://localhost:" + manager.getPort() + "/"), false);
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.status.sessions", manager.getActiveSessionsCount()), false);
        } else {
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.status.stopped"), false);
        }
        return Command.SINGLE_SUCCESS;
    }
    
    private static int startDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        WebDashboardManager manager = FeatureManager.getInstance().getWebDashboard();
        
        if (manager.isRunning()) {
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.start.already_running"), false);
            return Command.SINGLE_SUCCESS;
        }
        
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.start.starting"), false);
        if (manager.start()) {
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.start.success"), false);
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.status.url", "http://localhost:" + manager.getPort() + "/"), false);
            manager.addRealTimeEvent("SYSTEM", MessageUtil.getRawMessage("neoessentials.webdashboard.event.started", source.getDisplayName().getString()), "INFO");
        } else {
            source.sendFailure(MessageUtil.translatable("neoessentials.webdashboard.start.failed"));
        }
        return Command.SINGLE_SUCCESS;
    }
    
    private static int stopDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        WebDashboardManager manager = FeatureManager.getInstance().getWebDashboard();
        
        manager.stop();
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.stop.success"), false);
        
        manager.addRealTimeEvent("SYSTEM", MessageUtil.getRawMessage("neoessentials.webdashboard.event.stopped", source.getDisplayName().getString()), "INFO");
        return Command.SINGLE_SUCCESS;
    }
    
    private static int restartDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        WebDashboardManager manager = FeatureManager.getInstance().getWebDashboard();
        
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.restart.starting"), false);
        manager.stop();
        if (manager.start()) {
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.restart.success"), false);
            source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.status.url", "http://localhost:" + manager.getPort() + "/"), false);
            manager.addRealTimeEvent("SYSTEM", MessageUtil.getRawMessage("neoessentials.webdashboard.event.restarted", source.getDisplayName().getString()), "INFO");
        } else {
            source.sendFailure(MessageUtil.translatable("neoessentials.webdashboard.restart.failed"));
        }
        return Command.SINGLE_SUCCESS;
    }
    
    private static int showConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        WebDashboardManager manager = FeatureManager.getInstance().getWebDashboard();
        
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.config.header"), false);
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.config.port", manager.getPort()), false);
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.config.enabled", manager.isEnabled()), false);
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.config.auto_start", manager.isAutoStart()), false);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int showAnalytics(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        WebDashboardManager manager = FeatureManager.getInstance().getWebDashboard();
        
        Map<String, Object> analytics = manager.getAnalytics();
        
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.analytics.header"), false);
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.analytics.uptime", analytics.get("uptime")), false);
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.analytics.requests", analytics.get("requests")), false);
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.analytics.unique_visitors", analytics.get("unique_visitors")), false);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int showSessions(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        WebDashboardManager manager = FeatureManager.getInstance().getWebDashboard();
        
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.sessions.header"), false);
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.sessions.active", manager.getActiveSessionsCount()), false);
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.sessions.total", manager.getTotalSessionsCount()), false);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int showSecurityEvents(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("§6Security Events:"), false);
        source.sendSuccess(() -> Component.literal("§7Security event logging not yet implemented."), false);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int showWidgets(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("§6Dashboard Widgets:"), false);
        source.sendSuccess(() -> Component.literal("§7Widget system not yet implemented."), false);
        
        return Command.SINGLE_SUCCESS;
    }
}
