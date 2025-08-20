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
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "compat.status.header")
        ), false);
        manager.getDetectedPlugins().forEach(plugin -> {
            String statusKey = plugin.getStatus() == com.zerog.neoessentials.data.PluginStatusEnum.INTEGRATED ?
                "compat.status.integrated" : "compat.status.available";
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", statusKey, plugin.getName())
            ), false);
        });
    }
    
    private static void refreshIntegrations(CommandSourceStack source, PluginCompatibilityManager manager) {
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "compat.refresh.start")
        ), false);
        manager.refreshIntegrations();
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "compat.refresh.success")
        ), false);
    }
    
    private static void generateReport(CommandSourceStack source, PluginCompatibilityManager manager) {
        var report = manager.generateCompatibilityReport();
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "compat.report.header")
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "compat.report.total", report.getTotalPlugins())
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "compat.report.integrated", report.getIntegratedPlugins())
        ), false);
        source.sendSuccess(() -> Component.literal(
            com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "compat.report.failed", report.getFailedPlugins())
        ), false);
        if (!report.getIssues().isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "compat.report.issues.header")
            ), false);
            report.getIssues().forEach(issue -> {
                source.sendSuccess(() -> Component.literal(
                    com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_US", "compat.report.issue", issue)
                ), false);
            });
        }
    }
}
