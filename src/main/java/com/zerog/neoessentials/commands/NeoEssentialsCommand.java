
package com.zerog.neoessentials.commands;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.zerog.neoessentials.util.MessageUtil;
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
            var player = context.getSource().getPlayer();
            if (player != null) {
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.main.header"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.main.title"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.main.subtitle"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.main.header"));
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.main.available.commands"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.main.features"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.main.commands"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.main.version"));
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.main.quick.access"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.main.shop"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.main.menu"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.main.stats"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.main.warps"));
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.main.author.version"));
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing NeoEssentials main command", e);
            return 0;
        }
    }
    
    private static int executeVersionCommand(CommandContext<CommandSourceStack> context) {
        try {
            var player = context.getSource().getPlayer();
            if (player != null) {
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.version.header"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.version.title"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.version.header"));
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.version.modinfo"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.version.name"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.version.version"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.version.author"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.version.target"));
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.version.featurestatus"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.version.essential"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.version.discord"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.version.guis"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.version.tablist"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.version.economy"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.version.home"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.version.warp"));
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.version.compatibility"));
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing version command", e);
            return 0;
        }
    }
    
    private static int executeFeaturesCommand(CommandContext<CommandSourceStack> context) {
        try {
            var player = context.getSource().getPlayer();
            if (player != null) {
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.header"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.title"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.separator"));
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.essential"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.essential.player_management"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.essential.movement"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.essential.items"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.essential.world"));
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.discord"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.discord.webhook"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.discord.account_linking"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.discord.broadcasts"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.discord.embeds"));
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.gui"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.gui.shop"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.gui.stats"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.gui.info"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.gui.kit_warp"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.gui.economy"));
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.tablist"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.tablist.header_footer"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.tablist.realtime_info"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.tablist.stats"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.tablist.session"));
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.additional"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.additional.home"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.additional.language"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.additional.config"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.features.additional.persistence"));
            }
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing features command", e);
            return 0;
        }
    }

    private static int executeCommandsCommand(CommandContext<CommandSourceStack> context) {
        try {
            var player = context.getSource().getPlayer();
            if (player != null) {
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.header"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.title"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.separator"));
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.utility.header"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.utility.heal"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.utility.feed"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.utility.god"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.utility.vanish"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.utility.fly"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.utility.speed"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.utility.give"));
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.world.header"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.world.time"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.world.weather"));
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.interface.header"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.interface.workbench"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.interface.anvil"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.interface.enderchest"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.interface.invsee"));
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.gui.header"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.gui.shop"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.gui.menu"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.gui.stats"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.gui.kits"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.gui.warps"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.gui.economy"));
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.discord.header"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.discord.menu"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.discord.link"));
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.commands.discord.broadcast"));
            }
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing commands command", e);
            return 0;
        }
    }

    // Dynamic ICommand implementation for /neoessentials parent command
    public static class DynamicNeoEssentialsCommand implements ICommand {
        @Override
        public void execute(net.minecraft.server.level.ServerPlayer player, String[] args) {
            if (args.length == 0) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "Available commands: " + String.join(", ", com.zerog.neoessentials.commands.CommandRegistry.getDynamicCommandNames())
                ));
                return;
            }
            ICommand subCommand = com.zerog.neoessentials.commands.CommandRegistry.getDynamicCommand(args[0]);
            if (subCommand == null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "Unknown command: " + args[0]
                ));
                return;
            }
            subCommand.execute(player, Arrays.copyOfRange(args, 1, args.length));
        }

        @Override
        public List<String> tabComplete(net.minecraft.server.level.ServerPlayer player, String[] args) {
            if (args.length == 1) {
                return com.zerog.neoessentials.commands.CommandRegistry.getDynamicCommandNames().stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
            } else if (args.length > 1) {
                ICommand subCommand = com.zerog.neoessentials.commands.CommandRegistry.getDynamicCommand(args[0]);
                if (subCommand != null) {
                    return subCommand.tabComplete(player, Arrays.copyOfRange(args, 1, args.length));
                }
            }
            return Collections.emptyList();
        }

        @Override
        public List<String> getAliases() {
            return Arrays.asList("ne", "essentials");
        }
    }
}
