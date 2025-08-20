package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeoEssentials Summary Command - Shows all available features
 * 
 * Commands:
 * - /neoessentials - Shows main information and feature list
 * - /ne - Alias for /neoessentials
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class NeoEssentialsCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(NeoEssentialsCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Main command
        dispatcher.register(Commands.literal("neoessentials")
            .executes(NeoEssentialsCommand::executeMainCommand)
        );
        
        // Alias
        dispatcher.register(Commands.literal("ne")
            .executes(NeoEssentialsCommand::executeMainCommand)
        );
        
        // Version command
        dispatcher.register(Commands.literal("neoessentials")
            .then(Commands.literal("version")
                .executes(NeoEssentialsCommand::executeVersionCommand))
        );
        
        // Features command
        dispatcher.register(Commands.literal("neoessentials")
            .then(Commands.literal("features")
                .executes(NeoEssentialsCommand::executeFeaturesCommand))
        );
        
        // Commands list
        dispatcher.register(Commands.literal("neoessentials")
            .then(Commands.literal("commands")
                .executes(NeoEssentialsCommand::executeCommandsCommand))
        );
    }
    
    private static int executeMainCommand(CommandContext<CommandSourceStack> context) {
        try {
            var source = context.getSource();
            
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.main.header")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.main.title")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.main.subtitle")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.main.header")), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.main.available.commands")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.main.features")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.main.commands")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.main.version")), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.main.quick.access")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.main.shop")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.main.menu")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.main.stats")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.main.warps")), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.main.author.version")), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing NeoEssentials main command", e);
            return 0;
        }
    }
    
    private static int executeVersionCommand(CommandContext<CommandSourceStack> context) {
        try {
            var source = context.getSource();
            
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.version.header")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.version.title")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.version.header")), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.version.modinfo")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.version.name")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.version.version")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.version.author")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.version.target")), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.version.featurestatus")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.version.essential")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.version.discord")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.version.guis")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.version.tablist")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.version.economy")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.version.home")), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.version.warp")), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(context.getSource().getPlayer(), "ne.version.compatibility")), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing version command", e);
            return 0;
        }
    }
    
    private static int executeFeaturesCommand(CommandContext<CommandSourceStack> context) {
        try {
            var source = context.getSource();
            var player = source.getPlayer();
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.header")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.title")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.separator")
            ), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.essential")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.essential.player_management")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.essential.movement")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.essential.items")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.essential.world")
            ), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.discord")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.discord.webhook")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.discord.account_linking")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.discord.broadcasts")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.discord.embeds")
            ), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.gui")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.gui.shop")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.gui.stats")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.gui.info")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.gui.kit_warp")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.gui.economy")
            ), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.tablist")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.tablist.header_footer")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.tablist.realtime_info")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.tablist.stats")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.tablist.session")
            ), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.additional")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.additional.home")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.additional.language")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.additional.config")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.features.additional.persistence")
            ), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing features command", e);
            return 0;
        }
    }

    private static int executeCommandsCommand(CommandContext<CommandSourceStack> context) {
        try {
            var source = context.getSource();
            var player = source.getPlayer();
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.header")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.title")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.separator")
            ), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.utility.header")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.utility.heal")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.utility.feed")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.utility.god")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.utility.vanish")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.utility.fly")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.utility.speed")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.utility.give")
            ), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.world.header")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.world.time")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.world.weather")
            ), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.interface.header")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.interface.workbench")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.interface.anvil")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.interface.enderchest")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.interface.invsee")
            ), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.gui.header")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.gui.shop")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.gui.menu")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.gui.stats")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.gui.kits")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.gui.warps")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.gui.economy")
            ), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.discord.header")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.discord.menu")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.discord.link")
            ), false);
            source.sendSuccess(() -> Component.literal(
                com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "ne.commands.discord.broadcast")
            ), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing commands command", e);
            return 0;
        }
    }
}
