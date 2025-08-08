package com.zerog.neoessentials.commands;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.features.TablistScoreboardManager;
import com.zerog.neoessentials.features.CustomBossbarManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Theme Command for NeoEssentials
 * Provides commands for managing tablist themes, scoreboard themes, and bossbar customization
 * 
 * Commands:
 * - /theme tablist <theme> [player] - Change tablist theme
 * - /theme scoreboard <theme> [player] - Change scoreboard theme  
 * - /theme bossbar show <template> [duration] [player] - Show bossbar
 * - /theme bossbar hide [player] - Hide bossbar
 * - /theme list [type] - List available themes/templates
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class ThemeCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("theme")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            
            // Tablist theme commands
            .then(Commands.literal("tablist")
                .then(Commands.argument("theme", StringArgumentType.string())
                    .executes(ThemeCommand::executeSetTablistTheme)
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ThemeCommand::executeSetTablistThemeOther))))
            
            // Scoreboard theme commands  
            .then(Commands.literal("scoreboard")
                .then(Commands.argument("theme", StringArgumentType.string())
                    .executes(ThemeCommand::executeSetScoreboardTheme)
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ThemeCommand::executeSetScoreboardThemeOther))))
            
            // Bossbar commands
            .then(Commands.literal("bossbar")
                .then(Commands.literal("show")
                    .then(Commands.argument("template", StringArgumentType.string())
                        .executes(ThemeCommand::executeShowBossbar)
                        .then(Commands.argument("duration", IntegerArgumentType.integer(1, 3600))
                            .executes(ThemeCommand::executeShowBossbarWithDuration)
                            .then(Commands.argument("player", EntityArgument.player())
                                .executes(ThemeCommand::executeShowBossbarOther)))))
                .then(Commands.literal("hide")
                    .executes(ThemeCommand::executeHideBossbar)
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ThemeCommand::executeHideBossbarOther)))
                .then(Commands.literal("hideall")
                    .executes(ThemeCommand::executeHideAllBossbars)
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ThemeCommand::executeHideAllBossbarsOther))))
            
            // List commands
            .then(Commands.literal("list")
                .executes(ThemeCommand::executeListAll)
                .then(Commands.literal("tablist")
                    .executes(ThemeCommand::executeListTablistThemes))
                .then(Commands.literal("scoreboard")
                    .executes(ThemeCommand::executeListScoreboardThemes))
                .then(Commands.literal("bossbar")
                    .executes(ThemeCommand::executeListBossbarTemplates)))
            
            // Reload command
            .then(Commands.literal("reload")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
                .executes(ThemeCommand::executeReload))
        );
    }
    
    /**
     * Set tablist theme for self
     */
    private static int executeSetTablistTheme(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String theme = StringArgumentType.getString(context, "theme");
            
            if (!TablistScoreboardManager.getInstance().getAvailableTablistThemes().contains(theme)) {
                context.getSource().sendFailure(Component.literal("§cUnknown tablist theme: " + theme));
                return 0;
            }
            
            TablistScoreboardManager.getInstance().setPlayerTablistTheme(player, theme);
            context.getSource().sendSuccess(() -> 
                Component.literal("§aSet your tablist theme to: §6" + theme), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing tablist theme command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to set tablist theme: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Set tablist theme for other player
     */
    private static int executeSetTablistThemeOther(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String theme = StringArgumentType.getString(context, "theme");
            
            if (!TablistScoreboardManager.getInstance().getAvailableTablistThemes().contains(theme)) {
                context.getSource().sendFailure(Component.literal("§cUnknown tablist theme: " + theme));
                return 0;
            }
            
            TablistScoreboardManager.getInstance().setPlayerTablistTheme(targetPlayer, theme);
            context.getSource().sendSuccess(() -> 
                Component.literal("§aSet tablist theme for §6" + targetPlayer.getDisplayName().getString() + " §ato: §6" + theme), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing tablist theme command for other player", e);
            context.getSource().sendFailure(Component.literal("§cFailed to set tablist theme: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Set scoreboard theme for self
     */
    private static int executeSetScoreboardTheme(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String theme = StringArgumentType.getString(context, "theme");
            
            if (!TablistScoreboardManager.getInstance().getAvailableScoreboardThemes().contains(theme)) {
                context.getSource().sendFailure(Component.literal("§cUnknown scoreboard theme: " + theme));
                return 0;
            }
            
            TablistScoreboardManager.getInstance().setPlayerScoreboardTheme(player, theme);
            context.getSource().sendSuccess(() -> 
                Component.literal("§aSet your scoreboard theme to: §6" + theme), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing scoreboard theme command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to set scoreboard theme: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Set scoreboard theme for other player
     */
    private static int executeSetScoreboardThemeOther(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String theme = StringArgumentType.getString(context, "theme");
            
            if (!TablistScoreboardManager.getInstance().getAvailableScoreboardThemes().contains(theme)) {
                context.getSource().sendFailure(Component.literal("§cUnknown scoreboard theme: " + theme));
                return 0;
            }
            
            TablistScoreboardManager.getInstance().setPlayerScoreboardTheme(targetPlayer, theme);
            context.getSource().sendSuccess(() -> 
                Component.literal("§aSet scoreboard theme for §6" + targetPlayer.getDisplayName().getString() + " §ato: §6" + theme), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing scoreboard theme command for other player", e);
            context.getSource().sendFailure(Component.literal("§cFailed to set scoreboard theme: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Show bossbar with default settings
     */
    private static int executeShowBossbar(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String template = StringArgumentType.getString(context, "template");
            
            if (!CustomBossbarManager.getInstance().getTemplateNames().contains(template)) {
                context.getSource().sendFailure(Component.literal("§cUnknown bossbar template: " + template));
                return 0;
            }
            
            CustomBossbarManager.getInstance().showBossbar(player, template, 10);
            context.getSource().sendSuccess(() -> 
                Component.literal("§aShowing bossbar: §6" + template), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing show bossbar command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to show bossbar: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Show bossbar with duration
     */
    private static int executeShowBossbarWithDuration(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String template = StringArgumentType.getString(context, "template");
            int duration = IntegerArgumentType.getInteger(context, "duration");
            
            if (!CustomBossbarManager.getInstance().getTemplateNames().contains(template)) {
                context.getSource().sendFailure(Component.literal("§cUnknown bossbar template: " + template));
                return 0;
            }
            
            CustomBossbarManager.getInstance().showBossbar(player, template, duration);
            context.getSource().sendSuccess(() -> 
                Component.literal("§aShowing bossbar: §6" + template + " §afor §6" + duration + " §aseconds"), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing show bossbar command with duration", e);
            context.getSource().sendFailure(Component.literal("§cFailed to show bossbar: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Show bossbar for other player
     */
    private static int executeShowBossbarOther(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String template = StringArgumentType.getString(context, "template");
            int duration = IntegerArgumentType.getInteger(context, "duration");
            
            if (!CustomBossbarManager.getInstance().getTemplateNames().contains(template)) {
                context.getSource().sendFailure(Component.literal("§cUnknown bossbar template: " + template));
                return 0;
            }
            
            CustomBossbarManager.getInstance().showBossbar(targetPlayer, template, duration);
            context.getSource().sendSuccess(() -> 
                Component.literal("§aShowing bossbar: §6" + template + " §ato §6" + targetPlayer.getDisplayName().getString() + " §afor §6" + duration + " §aseconds"), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing show bossbar command for other player", e);
            context.getSource().sendFailure(Component.literal("§cFailed to show bossbar: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Hide bossbar for self
     */
    private static int executeHideBossbar(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            CustomBossbarManager.getInstance().removeBossbar(player);
            context.getSource().sendSuccess(() -> 
                Component.literal("§aHidden your bossbar"), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing hide bossbar command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to hide bossbar: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Hide bossbar for other player
     */
    private static int executeHideBossbarOther(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            
            CustomBossbarManager.getInstance().removeBossbar(targetPlayer);
            context.getSource().sendSuccess(() -> 
                Component.literal("§aHidden bossbar for §6" + targetPlayer.getDisplayName().getString()), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing hide bossbar command for other player", e);
            context.getSource().sendFailure(Component.literal("§cFailed to hide bossbar: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Hide all bossbars for self
     */
    private static int executeHideAllBossbars(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            CustomBossbarManager.getInstance().removeBossbar(player);
            context.getSource().sendSuccess(() -> 
                Component.literal("§aHidden all your bossbars"), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing hide all bossbars command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to hide bossbars: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Hide all bossbars for other player
     */
    private static int executeHideAllBossbarsOther(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            
            CustomBossbarManager.getInstance().removeBossbar(targetPlayer);
            context.getSource().sendSuccess(() -> 
                Component.literal("§aHidden all bossbars for §6" + targetPlayer.getDisplayName().getString()), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing hide all bossbars command for other player", e);
            context.getSource().sendFailure(Component.literal("§cFailed to hide bossbars: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * List all available themes and templates
     */
    private static int executeListAll(CommandContext<CommandSourceStack> context) {
        try {
            context.getSource().sendSuccess(() -> 
                Component.literal("§6§l=== NeoEssentials Theme System ==="), false);
            
            // Tablist themes
            context.getSource().sendSuccess(() -> 
                Component.literal("§b§lTablist Themes:"), false);
            for (String theme : TablistScoreboardManager.getInstance().getAvailableTablistThemes()) {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§7- §a" + theme), false);
            }
            
            // Scoreboard themes
            context.getSource().sendSuccess(() -> 
                Component.literal("§e§lScoreboard Themes:"), false);
            for (String theme : TablistScoreboardManager.getInstance().getAvailableScoreboardThemes()) {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§7- §a" + theme), false);
            }
            
            // Bossbar templates
            context.getSource().sendSuccess(() -> 
                Component.literal("§d§lBossbar Templates:"), false);
            for (String template : CustomBossbarManager.getInstance().getTemplateNames()) {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§7- §a" + template), false);
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing list all command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to list themes: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * List tablist themes
     */
    private static int executeListTablistThemes(CommandContext<CommandSourceStack> context) {
        try {
            context.getSource().sendSuccess(() -> 
                Component.literal("§b§lAvailable Tablist Themes:"), false);
            
            for (String theme : TablistScoreboardManager.getInstance().getAvailableTablistThemes()) {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§7- §a" + theme), false);
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing list tablist themes command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to list tablist themes: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * List scoreboard themes
     */
    private static int executeListScoreboardThemes(CommandContext<CommandSourceStack> context) {
        try {
            context.getSource().sendSuccess(() -> 
                Component.literal("§e§lAvailable Scoreboard Themes:"), false);
            
            for (String theme : TablistScoreboardManager.getInstance().getAvailableScoreboardThemes()) {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§7- §a" + theme), false);
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing list scoreboard themes command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to list scoreboard themes: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * List bossbar templates
     */
    private static int executeListBossbarTemplates(CommandContext<CommandSourceStack> context) {
        try {
            context.getSource().sendSuccess(() -> 
                Component.literal("§d§lAvailable Bossbar Templates:"), false);
            
            for (String template : CustomBossbarManager.getInstance().getTemplateNames()) {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§7- §a" + template), false);
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing list bossbar templates command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to list bossbar templates: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Reload theme system
     */
    private static int executeReload(CommandContext<CommandSourceStack> context) {
        try {
            context.getSource().sendSuccess(() -> 
                Component.literal("§aReloaded theme system configurations"), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing reload command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to reload theme system: " + e.getMessage()));
            return 0;
        }
    }
}
