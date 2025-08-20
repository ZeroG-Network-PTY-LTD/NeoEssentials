package com.zerog.neoessentials.commands;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.features.CustomBossbarManager;
import com.zerog.neoessentials.features.TablistScoreboardManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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
            TablistScoreboardManager.getInstance().reloadAnimations();
            CustomBossbarManager.getInstance().reloadAnimations();
            ServerPlayer player = context.getSource().getPlayerOrException();
            context.getSource().sendSuccess(() ->
                Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.reload.success")),
                true);
        } catch (Exception e) {
            ServerPlayer player = null;
            try { player = context.getSource().getPlayerOrException(); } catch (Exception ignored) {}
            context.getSource().sendFailure(
                Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.reload.failure", e.getMessage())));
        }
        return 1;
    }
    
    private static int showStats(CommandContext<CommandSourceStack> context) {
        try {
            String tablistStats = TablistScoreboardManager.getInstance().getAnimationStats();
            String bossbarStats = CustomBossbarManager.getInstance().getAnimationStats();
            ServerPlayer player = context.getSource().getPlayerOrException();
            context.getSource().sendSuccess(() ->
                Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.stats.header") + "\n"
                    + com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.stats.tablist", tablistStats) + "\n"
                    + com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.stats.bossbar", bossbarStats)),
                false);
        } catch (Exception e) {
            ServerPlayer player = null;
            try { player = context.getSource().getPlayerOrException(); } catch (Exception ignored) {}
            context.getSource().sendFailure(
                Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.stats.failure", e.getMessage())));
        }
        return 1;
    }
    
    private static int listAnimations(CommandContext<CommandSourceStack> context) {
        try {
            var tablistAnimations = TablistScoreboardManager.getInstance().getAvailableAnimations();
            var bossbarAnimations = CustomBossbarManager.getInstance().getAvailableAnimations();
            ServerPlayer player = context.getSource().getPlayerOrException();
            StringBuilder message = new StringBuilder(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.list.header") + "\n");
            if (!tablistAnimations.isEmpty()) {
                message.append(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.list.tablist", String.join(", ", tablistAnimations))).append("\n");
            }
            if (!bossbarAnimations.isEmpty()) {
                message.append(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.list.bossbar", String.join(", ", bossbarAnimations))).append("\n");
            }
            context.getSource().sendSuccess(() -> Component.literal(message.toString()), false);
        } catch (Exception e) {
            ServerPlayer player = null;
            try { player = context.getSource().getPlayerOrException(); } catch (Exception ignored) {}
            context.getSource().sendFailure(
                Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.list.failure", e.getMessage())));
        }
        return 1;
    }
    
    private static int testAnimation(CommandContext<CommandSourceStack> context) {
        try {
            String animationName = StringArgumentType.getString(context, "animation");
            
            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                // Test animation by showing a bossbar with the animation
                CustomBossbarManager.getInstance().showBossbar(player, "test", 5);
                
                context.getSource().sendSuccess(() -> 
                    Component.literal("§a✓ Testing animation: " + animationName), 
                    false);
            } else {
                context.getSource().sendFailure(
                    Component.literal("§c✗ This command can only be used by players"));
            }
        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("§c✗ Failed to test animation: " + e.getMessage()));
        }
        return 1;
    }
    
    private static int showHelp(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            context.getSource().sendSuccess(() ->
                Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.help")),
                false);
        } catch (Exception e) {
            ServerPlayer player = null;
            try { player = context.getSource().getPlayerOrException(); } catch (Exception ignored) {}
            context.getSource().sendFailure(
                Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "animation.help.failure", e.getMessage())));
        }
        return 1;
    }
}
