package com.zerog.neoessentials.commands;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.features.CustomBossbarManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Commands for managing animations in tablist, scoreboard, and bossbar
 */
public class AnimationCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("neoanimations")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
            .then(Commands.literal("reload")
                .executes(AnimationCommands::reloadAnimations))
            .then(Commands.literal("stats")
                .executes(AnimationCommands::showStats))
            .then(Commands.literal("list")
                .executes(AnimationCommands::listAnimations))
            .then(Commands.literal("test")
                .then(Commands.argument("animation", StringArgumentType.string())
                    .executes(AnimationCommands::testAnimation)))
            .then(Commands.literal("help")
                .executes(AnimationCommands::showHelp))
        );
    }
    
    private static int reloadAnimations(CommandContext<CommandSourceStack> context) {
        try {
            CustomBossbarManager.getInstance().reloadAnimations();
            ServerPlayer player = context.getSource().getPlayerOrException();
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.reload.success"));
        } catch (Exception e) {
            ServerPlayer player = null;
            try { player = context.getSource().getPlayerOrException(); } catch (Exception ignored) {}
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.reload.failure", e.getMessage()));
        }
        return 1;
    }
    
    private static int showStats(CommandContext<CommandSourceStack> context) {
        try {
            String bossbarStats = CustomBossbarManager.getInstance().getAnimationStats();
            ServerPlayer player = context.getSource().getPlayerOrException();
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.stats.header"));
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.stats.bossbar", bossbarStats));
        } catch (Exception e) {
            ServerPlayer player = null;
            try { player = context.getSource().getPlayerOrException(); } catch (Exception ignored) {}
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.stats.failure", e.getMessage()));
        }
        return 1;
    }
    
    private static int listAnimations(CommandContext<CommandSourceStack> context) {
        try {
            List<String> bossbarAnimations = CustomBossbarManager.getInstance().getAvailableAnimations();
            ServerPlayer player = context.getSource().getPlayerOrException();
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.list.header"));
            if (bossbarAnimations != null && !bossbarAnimations.isEmpty()) {
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.list.bossbar", String.join(", ", bossbarAnimations)));
            }
        } catch (Exception e) {
            ServerPlayer player = null;
            try { player = context.getSource().getPlayerOrException(); } catch (Exception ignored) {}
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.list.failure", e.getMessage()));
        }
        return 1;
    }
    
    private static int testAnimation(CommandContext<CommandSourceStack> context) {
        String animationName = StringArgumentType.getString(context, "animation");
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            // Test animation by showing a bossbar with the animation
            CustomBossbarManager.getInstance().showBossbar(player, "test", 5);
            MessageUtil.sendMessage(player, "&a✓ Testing animation: " + animationName);
        } else {
            try {
                MessageUtil.sendMessage(context.getSource().getPlayerOrException(), "&c✗ This command can only be used by players");
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) {
                // Optionally log or handle the error
            }
        }
        return 1;
    }
    
    private static int showHelp(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.help"));
        } catch (Exception e) {
            ServerPlayer player = null;
            try { player = context.getSource().getPlayerOrException(); } catch (Exception ignored) {}
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.help.failure", e.getMessage()));
        }
        return 1;
    }
}
