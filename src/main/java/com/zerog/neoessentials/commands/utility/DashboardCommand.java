package com.zerog.neoessentials.commands.utility;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import com.zerog.neoessentials.webdashboard.DashboardAPI;
import com.zerog.neoessentials.webdashboard.DashboardLifecycleManager;
import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Command to manage the Dashboard API server
 * Usage:
 * - /dashboard - Show dashboard status
 * - /dashboard start - Start the dashboard
 * - /dashboard stop - Stop the dashboard
 * - /dashboard restart - Restart the dashboard
 * - /dashboard url - Show dashboard URL
 */
public class DashboardCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dashboard")
            .requires(source -> PermissionValidator.validateAdminPermission(source, "neoessentials.admin.dashboard").hasPermission())
            .executes(DashboardCommand::showStatus)
            .then(Commands.literal("start")
                .executes(DashboardCommand::startDashboard))
            .then(Commands.literal("stop")
                .executes(DashboardCommand::stopDashboard))
            .then(Commands.literal("restart")
                .executes(DashboardCommand::restartDashboard))
            .then(Commands.literal("status")
                .executes(DashboardCommand::showStatus))
            .then(Commands.literal("url")
                .executes(DashboardCommand::showUrl))
        );
    }
    
    private static int showStatus(CommandContext<CommandSourceStack> context) {
        DashboardLifecycleManager.DashboardStatus status = DashboardLifecycleManager.getStatus();
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.separator"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.title"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.separator"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        // Running status
        String runningStatus = status.running ? "§a§lONLINE" : "§c§lOFFLINE";
        source.sendSuccess(() -> Component.literal("§7Status: " + runningStatus), false);
        
        // Config status
        String configStatus = status.configEnabled ? "§aEnabled" : "§cDisabled";
        source.sendSuccess(() -> Component.literal("§7Config: " + configStatus), false);
        
        // Manual override
        if (status.manuallyDisabled) {
            source.sendSuccess(() -> Component.literal("§7Override: §eManually disabled"), false);
        }
        
        // URL
        if (status.running) {
            source.sendSuccess(() -> Component.literal("§7URL: §b§n" + status.url), false);
            source.sendSuccess(() -> Component.literal("§7API: §b§n" + status.url + "/api/"), false);
        }
        
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);
        
        // Show available commands
        if (!status.running) {
            source.sendSuccess(() -> Component.literal("§7Use §e/dashboard start §7to start the server"), false);
        } else {
            source.sendSuccess(() -> Component.literal("§7Use §e/dashboard stop §7to stop the server"), false);
        }
        
        return 1;
    }
    
    private static int startDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!ConfigManager.isWebDashboardEnabled()) {
            source.sendSuccess(() -> Component.literal("§c§lERROR: §cDashboard is disabled in configuration!"), false);
            source.sendSuccess(() -> Component.literal("§7Enable it in §econfig/neoessentials.toml"), false);
            return 0;
        }
        
        if (DashboardAPI.getInstance().isRunning()) {
            source.sendSuccess(() -> Component.literal("§e§lWARNING: §eDashboard is already running!"), false);
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("§6Starting dashboard server..."), false);
        
        boolean success = DashboardLifecycleManager.startDashboard(source.getServer());
        
        if (success) {
            DashboardLifecycleManager.DashboardStatus status = DashboardLifecycleManager.getStatus();
            source.sendSuccess(() -> Component.literal("§a§l✓ §aDashboard started successfully!"), false);
            source.sendSuccess(() -> Component.literal("§7URL: §b§n" + status.url), false);
            source.sendSuccess(() -> Component.literal("§7API: §b§n" + status.url + "/api/"), false);
            return 1;
        } else {
            source.sendSuccess(() -> Component.literal("§c§l✗ §cFailed to start dashboard!"), false);
            source.sendSuccess(() -> Component.literal("§7Check server logs for details"), false);
            return 0;
        }
    }
    
    private static int stopDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!DashboardAPI.getInstance().isRunning()) {
            source.sendSuccess(() -> Component.literal("§e§lWARNING: §eDashboard is not running!"), false);
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("§6Stopping dashboard server..."), false);
        
        boolean success = DashboardLifecycleManager.stopDashboard();
        
        if (success) {
            source.sendSuccess(() -> Component.literal("§a§l✓ §aDashboard stopped successfully!"), false);
            source.sendSuccess(() -> Component.literal("§7Use §e/dashboard start §7to restart it"), false);
            return 1;
        } else {
            source.sendSuccess(() -> Component.literal("§c§l✗ §cFailed to stop dashboard!"), false);
            source.sendSuccess(() -> Component.literal("§7Check server logs for details"), false);
            return 0;
        }
    }
    
    private static int restartDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!DashboardAPI.getInstance().isRunning()) {
            source.sendSuccess(() -> Component.literal("§e§lWARNING: §eDashboard is not running!"), false);
            source.sendSuccess(() -> Component.literal("§7Use §e/dashboard start §7instead"), false);
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("§6Restarting dashboard server..."), false);
        
        // Stop
        boolean stopSuccess = DashboardLifecycleManager.stopDashboard();
        if (!stopSuccess) {
            source.sendSuccess(() -> Component.literal("§c§l✗ §cFailed to stop dashboard!"), false);
            return 0;
        }
        
        // Wait a moment
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Start
        boolean startSuccess = DashboardLifecycleManager.startDashboard(source.getServer());
        
        if (startSuccess) {
            DashboardLifecycleManager.DashboardStatus status = DashboardLifecycleManager.getStatus();
            source.sendSuccess(() -> Component.literal("§a§l✓ §aDashboard restarted successfully!"), false);
            source.sendSuccess(() -> Component.literal("§7URL: §b§n" + status.url), false);
            return 1;
        } else {
            source.sendSuccess(() -> Component.literal("§c§l✗ §cFailed to restart dashboard!"), false);
            source.sendSuccess(() -> Component.literal("§7Check server logs for details"), false);
            return 0;
        }
    }
    
    private static int showUrl(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!DashboardAPI.getInstance().isRunning()) {
            source.sendSuccess(() -> Component.literal("§c§lERROR: §cDashboard is not running!"), false);
            source.sendSuccess(() -> Component.literal("§7Use §e/dashboard start §7to start it"), false);
            return 0;
        }
        
        DashboardLifecycleManager.DashboardStatus status = DashboardLifecycleManager.getStatus();
        
        source.sendSuccess(() -> Component.literal("§6§lDashboard URLs:"), false);
        source.sendSuccess(() -> Component.literal("§7Frontend: §b§n" + status.url), false);
        source.sendSuccess(() -> Component.literal("§7API: §b§n" + status.url + "/api/"), false);
        
        return 1;
    }
}
