package com.zerog.neoessentials.commands;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Commands for managing animations in tablist
 * Scoreboard and bossbar systems have been completely removed
 */
public class AnimationCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("neoanimations")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
            .then(Commands.literal("reload")
                .executes(AnimationCommands::reloadAnimations))
            .then(Commands.literal("stats")
                .executes(AnimationCommands::showStats))
            .then(Commands.literal("help")
                .executes(AnimationCommands::showHelp))
        );
    }
    
    private static int reloadAnimations(CommandContext<CommandSourceStack> context) {
        try {
            // Scoreboard and bossbar systems have been removed
            // Only tablist animations are supported now
            context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Animation systems have been simplified - only tablist animations remain"), false);
            ServerPlayer player = context.getSource().getPlayerOrException();
            MessageUtil.sendMessage(player, "§aAnimation system simplified - scoreboard and bossbar features removed");
        } catch (Exception e) {
            ServerPlayer player = null;
            try { player = context.getSource().getPlayerOrException(); } catch (Exception ignored) {}
            MessageUtil.sendMessage(player, "§cError: " + e.getMessage());
        }
        return 1;
    }
    
    private static int showStats(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            MessageUtil.sendMessage(player, "§6=== Animation System Status ===");
            MessageUtil.sendMessage(player, "§eTablist Animations: §aActive");
            MessageUtil.sendMessage(player, "§eScoreboard Animations: §cRemoved");
            MessageUtil.sendMessage(player, "§eBossbar Animations: §cRemoved");
            MessageUtil.sendMessage(player, "§7Scoreboard and bossbar systems have been completely removed for better performance");
        } catch (Exception e) {
            ServerPlayer player = null;
            try { player = context.getSource().getPlayerOrException(); } catch (Exception ignored) {}
            MessageUtil.sendMessage(player, "§cError showing stats: " + e.getMessage());
        }
        return 1;
    }
    
    private static int showHelp(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            MessageUtil.sendMessage(player, "§6=== NeoEssentials Animation Commands ===");
            MessageUtil.sendMessage(player, "§e/neoanimations reload §7- Reload animation system (tablist only)");
            MessageUtil.sendMessage(player, "§e/neoanimations stats §7- Show animation system status");
            MessageUtil.sendMessage(player, "§e/neoanimations help §7- Show this help");
            MessageUtil.sendMessage(player, "§7Note: Scoreboard and bossbar animations have been removed");
        } catch (Exception e) {
            context.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Error showing help: " + e.getMessage()));
        }
        return 1;
    }
}
