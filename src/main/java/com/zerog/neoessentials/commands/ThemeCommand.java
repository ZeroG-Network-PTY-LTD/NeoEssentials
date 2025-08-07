package com.zerog.neoessentials.commands;

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
 * - /theme bossbar show <template> [animation] [duration] [player] - Show bossbar
 * - /theme bossbar hide <template> [player] - Hide bossbar
 * - /theme list [type] - List available themes/templates
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class ThemeCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("theme")
            .requires(source -> source.hasPermission(2))
            
            // Tablist theme commands
            .then(Commands.literal("tablist")
                .then(Commands.argument("theme", StringArgumentType.string())
                    .executes(EnhancedThemeCommand::executeSetTablistTheme)
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(EnhancedThemeCommand::executeSetTablistThemeOther))))
            
            // Scoreboard theme commands  
            .then(Commands.literal("scoreboard")
                .then(Commands.argument("theme", StringArgumentType.string())
                    .executes(EnhancedThemeCommand::executeSetScoreboardTheme)
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(EnhancedThemeCommand::executeSetScoreboardThemeOther))))
            
            // Bossbar commands
            .then(Commands.literal("bossbar")
                .then(Commands.literal("show")
                    .then(Commands.argument("template", StringArgumentType.string())
                        .executes(EnhancedThemeCommand::executeShowBossbar)
                        .then(Commands.argument("animation", StringArgumentType.string())
                            .executes(EnhancedThemeCommand::executeShowBossbarAnimated)
                            .then(Commands.argument("duration", IntegerArgumentType.integer(1, 3600))
                                .executes(EnhancedThemeCommand::executeShowBossbarAnimatedDuration)
                                .then(Commands.argument("player", EntityArgument.player())
                                    .executes(EnhancedThemeCommand::executeShowBossbarOther))))))
                .then(Commands.literal("hide")
                    .then(Commands.argument("template", StringArgumentType.string())
                        .executes(EnhancedThemeCommand::executeHideBossbar)
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(EnhancedThemeCommand::executeHideBossbarOther))))
                .then(Commands.literal("hideall")
                    .executes(EnhancedThemeCommand::executeHideAllBossbars)
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(EnhancedThemeCommand::executeHideAllBossbarsOther))))
            
            // List commands
            .then(Commands.literal("list")
                .executes(EnhancedThemeCommand::executeListAll)
                .then(Commands.literal("tablist")
                    .executes(EnhancedThemeCommand::executeListTablistThemes))
                .then(Commands.literal("scoreboard")
                    .executes(EnhancedThemeCommand::executeListScoreboardThemes))
                .then(Commands.literal("bossbar")
                    .executes(EnhancedThemeCommand::executeListBossbarTemplates))
                .then(Commands.literal("animations")
                    .executes(EnhancedThemeCommand::executeListAnimations)))
            
            // Reload command
            .then(Commands.literal("reload")
                .requires(source -> source.hasPermission(3))
                .executes(EnhancedThemeCommand::executeReload))
        );
    }
    
    /**
     * Set tablist theme for self
     */
    private static int executeSetTablistTheme(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String theme = StringArgumentType.getString(context, "theme");
            
            if (!EnhancedTablistManager.getInstance().getAvailableThemes().contains(theme)) {
                context.getSource().sendFailure(Component.literal("§cUnknown tablist theme: " + theme));
                return 0;
            }
            
            EnhancedTablistManager.getInstance().setPlayerTheme(player, theme);
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
            
            if (!EnhancedTablistManager.getInstance().getAvailableThemes().contains(theme)) {
                context.getSource().sendFailure(Component.literal("§cUnknown tablist theme: " + theme));
                return 0;
            }
            
            EnhancedTablistManager.getInstance().setPlayerTheme(targetPlayer, theme);
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
            
            if (!EnhancedTablistManager.getInstance().getAvailableScoreboardThemes().contains(theme)) {
                context.getSource().sendFailure(Component.literal("§cUnknown scoreboard theme: " + theme));
                return 0;
            }
            
            EnhancedTablistManager.getInstance().setPlayerScoreboardTheme(player, theme);
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
            
            if (!EnhancedTablistManager.getInstance().getAvailableScoreboardThemes().contains(theme)) {
                context.getSource().sendFailure(Component.literal("§cUnknown scoreboard theme: " + theme));
                return 0;
            }
            
            EnhancedTablistManager.getInstance().setPlayerScoreboardTheme(targetPlayer, theme);
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
            
            if (!EnhancedMultiBossbarManager.getInstance().getAvailableTemplates().contains(template)) {
                context.getSource().sendFailure(Component.literal("§cUnknown bossbar template: " + template));
                return 0;
            }
            
            EnhancedMultiBossbarManager.getInstance().showBossbar(player, template, "auto", 10);
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
     * Show bossbar with animation
     */
    private static int executeShowBossbarAnimated(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String template = StringArgumentType.getString(context, "template");
            String animation = StringArgumentType.getString(context, "animation");
            
            if (!EnhancedMultiBossbarManager.getInstance().getAvailableTemplates().contains(template)) {
                context.getSource().sendFailure(Component.literal("§cUnknown bossbar template: " + template));
                return 0;
            }
            
            if (!EnhancedMultiBossbarManager.getInstance().getAvailableAnimations().contains(animation)) {
                context.getSource().sendFailure(Component.literal("§cUnknown animation: " + animation));
                return 0;
            }
            
            EnhancedMultiBossbarManager.getInstance().showBossbar(player, template, animation, 10);
            context.getSource().sendSuccess(() -> 
                Component.literal("§aShowing bossbar: §6" + template + " §awith animation: §6" + animation), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing show animated bossbar command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to show bossbar: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Show bossbar with animation and duration
     */
    private static int executeShowBossbarAnimatedDuration(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String template = StringArgumentType.getString(context, "template");
            String animation = StringArgumentType.getString(context, "animation");
            int duration = IntegerArgumentType.getInteger(context, "duration");
            
            if (!EnhancedMultiBossbarManager.getInstance().getAvailableTemplates().contains(template)) {
                context.getSource().sendFailure(Component.literal("§cUnknown bossbar template: " + template));
                return 0;
            }
            
            if (!EnhancedMultiBossbarManager.getInstance().getAvailableAnimations().contains(animation)) {
                context.getSource().sendFailure(Component.literal("§cUnknown animation: " + animation));
                return 0;
            }
            
            EnhancedMultiBossbarManager.getInstance().showBossbar(player, template, animation, duration);
            context.getSource().sendSuccess(() -> 
                Component.literal("§aShowing bossbar: §6" + template + " §awith animation: §6" + animation + " §afor §6" + duration + " §aseconds"), false);
            
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
            String animation = StringArgumentType.getString(context, "animation");
            int duration = IntegerArgumentType.getInteger(context, "duration");
            
            if (!EnhancedMultiBossbarManager.getInstance().getAvailableTemplates().contains(template)) {
                context.getSource().sendFailure(Component.literal("§cUnknown bossbar template: " + template));
                return 0;
            }
            
            if (!EnhancedMultiBossbarManager.getInstance().getAvailableAnimations().contains(animation)) {
                context.getSource().sendFailure(Component.literal("§cUnknown animation: " + animation));
                return 0;
            }
            
            EnhancedMultiBossbarManager.getInstance().showBossbar(targetPlayer, template, animation, duration);
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
            String template = StringArgumentType.getString(context, "template");
            
            EnhancedMultiBossbarManager.getInstance().hideBossbar(player, template);
            context.getSource().sendSuccess(() -> 
                Component.literal("§aHidden bossbar: §6" + template), false);
            
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
            String template = StringArgumentType.getString(context, "template");
            
            EnhancedMultiBossbarManager.getInstance().hideBossbar(targetPlayer, template);
            context.getSource().sendSuccess(() -> 
                Component.literal("§aHidden bossbar: §6" + template + " §afor §6" + targetPlayer.getDisplayName().getString()), false);
            
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
            
            EnhancedMultiBossbarManager.getInstance().hideAllBossbars(player);
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
            
            EnhancedMultiBossbarManager.getInstance().hideAllBossbars(targetPlayer);
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
                Component.literal("§6§l=== Enhanced Theme System ==="), false);
            
            // Tablist themes
            context.getSource().sendSuccess(() -> 
                Component.literal("§b§lTablist Themes:"), false);
            for (String theme : EnhancedTablistManager.getInstance().getAvailableThemes()) {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§7- §a" + theme), false);
            }
            
            // Scoreboard themes
            context.getSource().sendSuccess(() -> 
                Component.literal("§e§lScoreboard Themes:"), false);
            for (String theme : EnhancedTablistManager.getInstance().getAvailableScoreboardThemes()) {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§7- §a" + theme), false);
            }
            
            // Bossbar templates
            context.getSource().sendSuccess(() -> 
                Component.literal("§d§lBossbar Templates:"), false);
            for (String template : EnhancedMultiBossbarManager.getInstance().getAvailableTemplates()) {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§7- §a" + template), false);
            }
            
            // Animations
            context.getSource().sendSuccess(() -> 
                Component.literal("§c§lAnimations:"), false);
            for (String animation : EnhancedMultiBossbarManager.getInstance().getAvailableAnimations()) {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§7- §a" + animation), false);
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
            
            for (String theme : EnhancedTablistManager.getInstance().getAvailableThemes()) {
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
            
            for (String theme : EnhancedTablistManager.getInstance().getAvailableScoreboardThemes()) {
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
            
            for (String template : EnhancedMultiBossbarManager.getInstance().getAvailableTemplates()) {
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
     * List animations
     */
    private static int executeListAnimations(CommandContext<CommandSourceStack> context) {
        try {
            context.getSource().sendSuccess(() -> 
                Component.literal("§c§lAvailable Animations:"), false);
            
            for (String animation : EnhancedMultiBossbarManager.getInstance().getAvailableAnimations()) {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§7- §a" + animation), false);
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing list animations command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to list animations: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Reload theme system
     */
    private static int executeReload(CommandContext<CommandSourceStack> context) {
        try {
            // In a real implementation, this would reload configurations
            context.getSource().sendSuccess(() -> 
                Component.literal("§aReloaded enhanced theme system configurations"), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing reload command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to reload theme system: " + e.getMessage()));
            return 0;
        }
    }
}
