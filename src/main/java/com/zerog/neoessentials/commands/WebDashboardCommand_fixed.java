package com.zerog.neoessentials.commands;

import com.zerog.neoessentials.web.WebDashboardManager;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

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
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_ADMIN))
                .executes(context -> showConfig(context)))
            .then(Commands.literal("analytics")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_ANALYTICS))
                .executes(context -> showAnalytics(context)))
            .then(Commands.literal("sessions")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_ANALYTICS))
                .executes(context -> showSessions(context)));
        
        // Register both "dashboard" and "webdashboard" aliases
        dispatcher.register(command);
        dispatcher.register(Commands.literal("webdashboard")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WEBDASH_USE))
                .redirect(command.build()));
    }
    
    private static int showStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        WebDashboardManager manager = WebDashboardManager.getInstance();
        
        if (isRunning(manager)) {
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
        WebDashboardManager manager = WebDashboardManager.getInstance();
        
        if (isRunning(manager)) {
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
        WebDashboardManager manager = WebDashboardManager.getInstance();
        
        manager.stop();
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.stop.success"), false);
        
        manager.addRealTimeEvent("SYSTEM", MessageUtil.getRawMessage("neoessentials.webdashboard.event.stopped", source.getDisplayName().getString()), "INFO");
        return Command.SINGLE_SUCCESS;
    }
    
    private static int restartDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        WebDashboardManager manager = WebDashboardManager.getInstance();
        
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
        WebDashboardManager manager = WebDashboardManager.getInstance();
        
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.config.header"), false);
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.config.port", manager.getPort()), false);
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.config.enabled", manager.isDashboardEnabled()), false);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int showAnalytics(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        WebDashboardManager manager = WebDashboardManager.getInstance();
        
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.analytics.header"), false);
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.analytics.requests", "N/A"), false);
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.analytics.unique_visitors", "N/A"), false);
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int showSessions(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        WebDashboardManager manager = WebDashboardManager.getInstance();
        
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.sessions.header"), false);
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.sessions.active", manager.getActiveSessionsCount()), false);
        source.sendSuccess(() -> MessageUtil.translatable("neoessentials.webdashboard.sessions.total", "N/A"), false);
        
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Check if the dashboard is running by inspecting the HTTP server state
     */
    private static boolean isRunning(WebDashboardManager manager) {
        try {
            // Use reflection to check if httpServer field is not null
            java.lang.reflect.Field httpServerField = WebDashboardManager.class.getDeclaredField("httpServer");
            httpServerField.setAccessible(true);
            Object httpServer = httpServerField.get(manager);
            return httpServer != null;
        } catch (Exception e) {
            return false;
        }
    }
}
