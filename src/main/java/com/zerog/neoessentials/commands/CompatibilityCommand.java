package com.zerog.neoessentials.commands;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.zerog.neoessentials.managers.PluginCompatibilityManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

/**
 * Plugin Compatibility Commands
 */
public class CompatibilityCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, PluginCompatibilityManager compatibilityManager) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("compatibility")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .then(Commands.literal("status")
                .executes(context -> {
                    showStatus(context.getSource(), compatibilityManager);
                    return 1;
                }))
            .then(Commands.literal("refresh")
                .executes(context -> {
                    refreshIntegrations(context.getSource(), compatibilityManager);
                    return 1;
                }))
            .then(Commands.literal("report")
                .executes(context -> {
                    generateReport(context.getSource(), compatibilityManager);
                    return 1;
                }));
        
        dispatcher.register(command);
    }
    
    private static void showStatus(CommandSourceStack source, PluginCompatibilityManager manager) {
        source.sendSuccess(() -> Component.literal(
            MessageUtil.translateColorCodes("&6=== Plugin Compatibility Status ===")
        ), false);
        
        manager.getDetectedPlugins().forEach(plugin -> {
            String status = plugin.getStatus() == com.zerog.neoessentials.data.PluginStatusEnum.INTEGRATED ? 
                "&a✓ INTEGRATED" : "&c✗ AVAILABLE";
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&7" + plugin.getName() + ": " + status)
            ), false);
        });
    }
    
    private static void refreshIntegrations(CommandSourceStack source, PluginCompatibilityManager manager) {
        source.sendSuccess(() -> Component.literal(
            MessageUtil.translateColorCodes("&6Refreshing plugin integrations...")
        ), false);
        
        manager.refreshIntegrations();
        
        source.sendSuccess(() -> Component.literal(
            MessageUtil.translateColorCodes("&aPlugin integrations refreshed!")
        ), false);
    }
    
    private static void generateReport(CommandSourceStack source, PluginCompatibilityManager manager) {
        var report = manager.generateCompatibilityReport();
        
        source.sendSuccess(() -> Component.literal(
            MessageUtil.translateColorCodes("&6=== Compatibility Report ===")
        ), false);
        
        source.sendSuccess(() -> Component.literal(
            MessageUtil.translateColorCodes("&7Total Plugins Detected: &e" + report.getTotalPlugins())
        ), false);
        
        source.sendSuccess(() -> Component.literal(
            MessageUtil.translateColorCodes("&7Successfully Integrated: &a" + report.getIntegratedPlugins())
        ), false);
        
        source.sendSuccess(() -> Component.literal(
            MessageUtil.translateColorCodes("&7Failed Integrations: &c" + report.getFailedPlugins())
        ), false);
        
        if (!report.getIssues().isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                MessageUtil.translateColorCodes("&6Issues Found:")
            ), false);
            
            report.getIssues().forEach(issue -> {
                source.sendSuccess(() -> Component.literal(
                    MessageUtil.translateColorCodes("&c- " + issue)
                ), false);
            });
        }
    }
}
