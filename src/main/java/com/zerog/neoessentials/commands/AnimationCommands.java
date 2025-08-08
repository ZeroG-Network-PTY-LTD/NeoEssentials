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
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§a✓ Animation configurations reloaded successfully!"), 
                true);
        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("§c✗ Failed to reload animations: " + e.getMessage()));
        }
        return 1;
    }
    
    private static int showStats(CommandContext<CommandSourceStack> context) {
        try {
            String tablistStats = TablistScoreboardManager.getInstance().getAnimationStats();
            String bossbarStats = CustomBossbarManager.getInstance().getAnimationStats();
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§6=== Animation Statistics ===\n" +
                    "§eTablist/Scoreboard: §f" + tablistStats + "\n" +
                    "§eBossbar: §f" + bossbarStats), 
                false);
        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("§c✗ Failed to get animation stats: " + e.getMessage()));
        }
        return 1;
    }
    
    private static int listAnimations(CommandContext<CommandSourceStack> context) {
        try {
            var tablistAnimations = TablistScoreboardManager.getInstance().getAvailableAnimations();
            var bossbarAnimations = CustomBossbarManager.getInstance().getAvailableAnimations();
            
            StringBuilder message = new StringBuilder("§6=== Available Animations ===\n");
            
            if (!tablistAnimations.isEmpty()) {
                message.append("§eTablist/Scoreboard: §f");
                message.append(String.join(", ", tablistAnimations));
                message.append("\n");
            }
            
            if (!bossbarAnimations.isEmpty()) {
                message.append("§eBossbar: §f");
                message.append(String.join(", ", bossbarAnimations));
            }
            
            if (tablistAnimations.isEmpty() && bossbarAnimations.isEmpty()) {
                message.append("§cNo animations loaded");
            }
            
            context.getSource().sendSuccess(() -> Component.literal(message.toString()), false);
        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("§c✗ Failed to list animations: " + e.getMessage()));
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
        String helpText = """
            §6=== NeoEssentials Animation Commands ===
            §e/neoanimations reload §7- Reload animation configurations
            §e/neoanimations stats §7- Show animation system statistics
            §e/neoanimations list §7- List all available animations
            §e/neoanimations test <animation> §7- Test an animation
            §e/neoanimations help §7- Show this help message
            
            §6=== Animation Features ===
            §7• §bCustom animated placeholders for tablist, scoreboard, and bossbar
            §7• §bMultiple animation types: text cycling, color cycling, conditional, etc.
            §7• §bPlayer-specific animation states
            §7• §bReal-time configuration reloading
            §7• §bHealth bars, weather icons, progress bars, and more!
            
            §6=== Configuration ===
            §7Edit §econfig/neoessentials/animations.json §7to customize animations
            """;
        
        context.getSource().sendSuccess(() -> Component.literal(helpText), false);
        return 1;
    }
}
