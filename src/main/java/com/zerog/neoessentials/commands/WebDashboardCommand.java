package com.zerog.neoessentials.commands;

import com.zerog.neoessentials.managers.WebDashboardManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

/**
 * Web Dashboard Management Commands
 */
public class WebDashboardCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("dashboard")
            .requires(source -> source.hasPermission(4)) // Operator level permission
            .then(Commands.literal("status")
                .executes(context -> {
                    showStatus(context.getSource());
                    return 1;
                }))
            .then(Commands.literal("start")
                .executes(context -> {
                    startDashboard(context.getSource());
                    return 1;
                }))
            .then(Commands.literal("stop")
                .executes(context -> {
                    stopDashboard(context.getSource());
                    return 1;
                }))
            .then(Commands.literal("restart")
                .executes(context -> {
                    restartDashboard(context.getSource());
                    return 1;
                }));
        
        dispatcher.register(command);
    }
    
    private static void showStatus(CommandSourceStack source) {
        WebDashboardManager manager = WebDashboardManager.getInstance();
        
        if (manager.isRunning()) {
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&a✓ Web Dashboard is RUNNING")
            ), false);
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&7  URL: &ehttp://localhost:" + manager.getWebDashboard().getPort() + "/")
            ), false);
        } else {
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&c✗ Web Dashboard is STOPPED")
            ), false);
        }
    }
    
    private static void startDashboard(CommandSourceStack source) {
        WebDashboardManager manager = WebDashboardManager.getInstance();
        
        if (manager.isRunning()) {
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&6Web Dashboard is already running!")
            ), false);
            return;
        }
        
        source.sendSuccess(() -> Component.literal(
            MessageUtil.translateColorCodes("&6Starting Web Dashboard...")
        ), false);
        
        manager.initialize();
        
        if (manager.isRunning()) {
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&a✓ Web Dashboard started successfully!")
            ), false);
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&7  URL: &ehttp://localhost:" + manager.getWebDashboard().getPort() + "/")
            ), false);
        } else {
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&c✗ Failed to start Web Dashboard!")
            ), false);
        }
    }
    
    private static void stopDashboard(CommandSourceStack source) {
        WebDashboardManager manager = WebDashboardManager.getInstance();
        
        if (!manager.isRunning()) {
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&6Web Dashboard is already stopped!")
            ), false);
            return;
        }
        
        source.sendSuccess(() -> Component.literal(
            MessageUtil.translateColorCodes("&6Stopping Web Dashboard...")
        ), false);
        
        manager.shutdown();
        
        source.sendSuccess(() -> Component.literal(
            MessageUtil.translateColorCodes("&a✓ Web Dashboard stopped successfully!")
        ), false);
    }
    
    private static void restartDashboard(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(
            MessageUtil.translateColorCodes("&6Restarting Web Dashboard...")
        ), false);
        
        WebDashboardManager manager = WebDashboardManager.getInstance();
        
        if (manager.isRunning()) {
            manager.shutdown();
        }
        
        manager.initialize();
        
        if (manager.isRunning()) {
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&a✓ Web Dashboard restarted successfully!")
            ), false);
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&7  URL: &ehttp://localhost:" + manager.getWebDashboard().getPort() + "/")
            ), false);
        } else {
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&c✗ Failed to restart Web Dashboard!")
            ), false);
        }
    }
}
