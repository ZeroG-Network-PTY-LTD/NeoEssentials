package com.zerog.neoessentials.commands;

/**
 * BossbarCommand - REMOVED
 * This feature has been completely removed from NeoEssentials
 * 
 * @deprecated Bossbar system has been removed
 */
@Deprecated
public class BossbarCommand {
    // Feature removed - no longer supported
}

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
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerPlayer;
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
            
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.bossbar.show.self", template));
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar show command", e);
            MessageUtil.sendMessage(context.getSource().getPlayer(), com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "neoessentials.bossbar.error.show", e.getMessage()));
            return 0;
        }
    }
    
    private static int executeShowPlayer(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            String template = StringArgumentType.getString(context, "template");
            
            CustomBossbarManager.getInstance().showBossbar(target, template, 10);
            
            MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.bossbar.show.player", template, target.getDisplayName().getString()));
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar show player command", e);
            MessageUtil.sendMessage(context.getSource().getPlayer(), com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "neoessentials.bossbar.error.show", e.getMessage()));
            return 0;
        }
    }
    
    private static int executeShowPlayerDuration(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            String template = StringArgumentType.getString(context, "template");
            int duration = IntegerArgumentType.getInteger(context, "duration");
            
            CustomBossbarManager.getInstance().showBossbar(target, template, duration);
            
            MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.bossbar.show.player.duration", template, target.getDisplayName().getString(), duration));
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar show player duration command", e);
            MessageUtil.sendMessage(context.getSource().getPlayer(), com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "neoessentials.bossbar.error.show", e.getMessage()));
            return 0;
        }
    }
    
    private static int executeHideSelf(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            CustomBossbarManager.getInstance().removeBossbar(player);
            
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.bossbar.hide.self"));
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar hide command", e);
            MessageUtil.sendMessage(context.getSource().getPlayer(), com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "neoessentials.bossbar.error.hide", e.getMessage()));
            return 0;
        }
    }
    
    private static int executeHidePlayer(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            
            CustomBossbarManager.getInstance().removeBossbar(target);
            
            MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.bossbar.hide.player", target.getDisplayName().getString()));
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar hide player command", e);
            MessageUtil.sendMessage(context.getSource().getPlayer(), com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "neoessentials.bossbar.error.hide", e.getMessage()));
            return 0;
        }
    }
    
    private static int executeUpdateSelf(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String text = StringArgumentType.getString(context, "text");
            
            CustomBossbarManager.getInstance().updateBossbar(player, text, 1.0f);
            
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.bossbar.update.self"));
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar update command", e);
            MessageUtil.sendMessage(context.getSource().getPlayer(), com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "neoessentials.bossbar.error.update", e.getMessage()));
            return 0;
        }
    }
    
    private static int executeUpdateSelfProgress(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String text = StringArgumentType.getString(context, "text");
            float progress = FloatArgumentType.getFloat(context, "progress");
            
            CustomBossbarManager.getInstance().updateBossbar(player, text, progress);
            
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.bossbar.update.self.progress"));
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar update progress command", e);
            MessageUtil.sendMessage(context.getSource().getPlayer(), com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "neoessentials.bossbar.error.update", e.getMessage()));
            return 0;
        }
    }
    
    private static int executeUpdatePlayer(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            String text = StringArgumentType.getString(context, "text");
            float progress = FloatArgumentType.getFloat(context, "progress");
            
            CustomBossbarManager.getInstance().updateBossbar(target, text, progress);
            
            MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.bossbar.update.player", target.getDisplayName().getString()));
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar update player command", e);
            MessageUtil.sendMessage(context.getSource().getPlayer(), com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "neoessentials.bossbar.error.update", e.getMessage()));
            return 0;
        }
    }
    
    private static int executeBroadcast(CommandContext<CommandSourceStack> context) {
        try {
            String template = StringArgumentType.getString(context, "template");
            int duration = IntegerArgumentType.getInteger(context, "duration");
            
            CustomBossbarManager.getInstance().broadcastBossbar(template, duration);
            
            ServerPlayer player = context.getSource().getPlayer();
            if (player != null) {
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.bossbar.broadcast", template, duration));
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar broadcast command", e);
            ServerPlayer player = context.getSource().getPlayer();
            if (player != null) {
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.bossbar.error.broadcast", e.getMessage()));
            }
            return 0;
        }
    }
    
    private static int executeListTemplates(CommandContext<CommandSourceStack> context) {
        try {
            var templates = CustomBossbarManager.getInstance().getTemplateNames();
            ServerPlayer player = context.getSource().getPlayer();
            if (player != null) {
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.bossbar.list.header"));
                if (templates.isEmpty()) {
                    MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.bossbar.list.empty"));
                } else {
                    for (String template : templates) {
                        MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.bossbar.list.item", template));
                    }
                }
            }
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar templates command", e);
            ServerPlayer player = context.getSource().getPlayer();
            if (player != null) {
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.bossbar.error.list", e.getMessage()));
            }
            return 0;
        }
    }
    
    private static int executeCreateCustom(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String text = StringArgumentType.getString(context, "text");
            // Create a simple custom bossbar template using available constructor
            CustomBossbarManager.BossbarTemplate template = new CustomBossbarManager.BossbarTemplate(
                "Custom",
                text,
                net.minecraft.world.BossEvent.BossBarColor.GREEN, // Use BossBarColor enum
                net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS  // Use BossBarOverlay enum
            );
            // Show bossbar using template name
            CustomBossbarManager.getInstance().showBossbar(player, template.name, 10);
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.bossbar.create.custom"));
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing bossbar create command", e);
            MessageUtil.sendMessage(context.getSource().getPlayer(), com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "neoessentials.bossbar.error.create", e.getMessage()));
            return 0;
        }
    }
}
