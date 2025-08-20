package com.zerog.neoessentials.commands;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.features.CustomBossbarManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bossbar Command - Manages custom bossbars
 * 
 * Commands:
 * - /bossbar show <template> [player] [duration] - Show bossbar
 * - /bossbar hide [player] - Hide bossbar
 * - /bossbar update <text> <progress> [player] - Update bossbar
 * - /bossbar broadcast <template> <duration> - Broadcast to all
 * - /bossbar templates - List available templates
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class BossbarCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(BossbarCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bossbar")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            
            // Show bossbar
            .then(Commands.literal("show")
                .then(Commands.argument("template", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        CustomBossbarManager.getInstance().getTemplateNames().forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .executes(BossbarCommand::executeShowSelf)
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(BossbarCommand::executeShowPlayer)
                        .then(Commands.argument("duration", IntegerArgumentType.integer(1, 300))
                            .executes(BossbarCommand::executeShowPlayerDuration)))))
            
            // Hide bossbar
            .then(Commands.literal("hide")
                .executes(BossbarCommand::executeHideSelf)
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(BossbarCommand::executeHidePlayer)))
            
            // Update bossbar
            .then(Commands.literal("update")
                .then(Commands.argument("text", StringArgumentType.greedyString())
                    .executes(BossbarCommand::executeUpdateSelf)
                    .then(Commands.argument("progress", FloatArgumentType.floatArg(0.0f, 1.0f))
                        .executes(BossbarCommand::executeUpdateSelfProgress)
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(BossbarCommand::executeUpdatePlayer)))))
            
            // Broadcast bossbar
            .then(Commands.literal("broadcast")
                .then(Commands.argument("template", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        CustomBossbarManager.getInstance().getTemplateNames().forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .then(Commands.argument("duration", IntegerArgumentType.integer(1, 300))
                        .executes(BossbarCommand::executeBroadcast))))
            
            // Announce (alias for broadcast)
            .then(Commands.literal("announce")
                .then(Commands.argument("template", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        CustomBossbarManager.getInstance().getTemplateNames().forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .then(Commands.argument("duration", IntegerArgumentType.integer(1, 300))
                        .executes(BossbarCommand::executeBroadcast))))
            
            // List templates
            .then(Commands.literal("templates")
                .executes(BossbarCommand::executeListTemplates))
            
            // Create custom bossbar
            .then(Commands.literal("create")
                .then(Commands.argument("text", StringArgumentType.greedyString())
                    .executes(BossbarCommand::executeCreateCustom)))
        );
    }
    
    private static int executeShowSelf(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String template = StringArgumentType.getString(context, "template");
            
            CustomBossbarManager.getInstance().showBossbar(player, template, 10);
            
            context.getSource().sendSuccess(() -> 
                Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "bossbar.show.self", template)), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar show command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to show bossbar: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeShowPlayer(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            String template = StringArgumentType.getString(context, "template");
            
            CustomBossbarManager.getInstance().showBossbar(target, template, 10);
            
            context.getSource().sendSuccess(() -> 
                Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "bossbar.show.player", template, target.getDisplayName().getString())), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar show player command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to show bossbar: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeShowPlayerDuration(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            String template = StringArgumentType.getString(context, "template");
            int duration = IntegerArgumentType.getInteger(context, "duration");
            
            CustomBossbarManager.getInstance().showBossbar(target, template, duration);
            
            context.getSource().sendSuccess(() -> 
                Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "bossbar.show.player.duration", template, target.getDisplayName().getString(), duration)), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar show player duration command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to show bossbar: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeHideSelf(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            CustomBossbarManager.getInstance().removeBossbar(player);
            
            context.getSource().sendSuccess(() -> 
                Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "bossbar.hide.self")), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar hide command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to hide bossbar: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeHidePlayer(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            
            CustomBossbarManager.getInstance().removeBossbar(target);
            
            context.getSource().sendSuccess(() -> 
                Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "bossbar.hide.player", target.getDisplayName().getString())), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar hide player command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to hide bossbar: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeUpdateSelf(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String text = StringArgumentType.getString(context, "text");
            
            CustomBossbarManager.getInstance().updateBossbar(player, text, 1.0f);
            
            context.getSource().sendSuccess(() -> 
                Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "bossbar.update.self")), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar update command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to update bossbar: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeUpdateSelfProgress(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String text = StringArgumentType.getString(context, "text");
            float progress = FloatArgumentType.getFloat(context, "progress");
            
            CustomBossbarManager.getInstance().updateBossbar(player, text, progress);
            
            context.getSource().sendSuccess(() -> 
                Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "bossbar.update.self.progress")), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar update progress command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to update bossbar: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeUpdatePlayer(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            String text = StringArgumentType.getString(context, "text");
            float progress = FloatArgumentType.getFloat(context, "progress");
            
            CustomBossbarManager.getInstance().updateBossbar(target, text, progress);
            
            context.getSource().sendSuccess(() -> 
                Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "bossbar.update.player", target.getDisplayName().getString())), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar update player command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to update bossbar: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeBroadcast(CommandContext<CommandSourceStack> context) {
        try {
            String template = StringArgumentType.getString(context, "template");
            int duration = IntegerArgumentType.getInteger(context, "duration");
            
            CustomBossbarManager.getInstance().broadcastBossbar(template, duration);
            
            context.getSource().sendSuccess(() -> 
                Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "bossbar.broadcast", template, duration)), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar broadcast command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to broadcast bossbar: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeListTemplates(CommandContext<CommandSourceStack> context) {
        try {
            var templates = CustomBossbarManager.getInstance().getTemplateNames();
            
            context.getSource().sendSuccess(() -> 
                Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "bossbar.list.header")), false);
            
            if (templates.isEmpty()) {
                context.getSource().sendSuccess(() -> 
                    Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "bossbar.list.empty")), false);
            } else {
                for (String template : templates) {
                    context.getSource().sendSuccess(() -> 
                        Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "bossbar.list.item", template)), false);
                }
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar templates command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to list templates: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeCreateCustom(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String text = StringArgumentType.getString(context, "text");
            
            // Create a simple custom bossbar template
            CustomBossbarManager.BossbarTemplate template = new CustomBossbarManager.BossbarTemplate(
                "Custom",
                text,
                BossEvent.BossBarColor.GREEN,
                BossEvent.BossBarOverlay.PROGRESS,
                1.0f,
                false,
                false
            );
            
            CustomBossbarManager.getInstance().showBossbar(player, template, 10);
            
            context.getSource().sendSuccess(() -> 
                Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "bossbar.create.custom")), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar create command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to create bossbar: " + e.getMessage()));
            return 0;
        }
    }
}
