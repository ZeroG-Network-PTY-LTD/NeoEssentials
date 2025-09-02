
package com.zerog.neoessentials.commands;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.ErrorHandler;

/**
 * NeoEssentials Main Command - Shows all available commands with tab completion
 * 
 * Commands:
 * - /neoessentials - Shows main information and feature list
 * - /neoessentials <command> - Tab completion for all available commands
 * - /ne - Alias for /neoessentials
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class NeoEssentialsCommand {
    
    // All available NeoEssentials commands for tab completion
    private static final List<String> ALL_COMMANDS = Arrays.asList(
        // Essential utility commands
        "heal", "feed", "god", "vanish", "fly", "speed", "gamemode", "gm", "gmc", "gms", "gma", "gmsp",
        "repair", "time", "weather", "give", "workbench", "anvil",
        
        // Moderation commands  
        "ban", "kick", "mute", "list", "whois", "seen",
        
        // Help and info commands
        "help", "info", "serverinfo", "motd", "rules",
        
        // Communication commands
        "msg", "tell", "reply", "r", "mail", "broadcast", "ignore", "unignore",
        
        // Teleportation commands
        "teleport", "tp", "tpa", "tpaccept", "tpdeny", "back", "spawn", "setspawn",
        
        // Home and warp commands
        "home", "sethome", "delhome", "homes", "warp", "setwarp", "delwarp", "warps",
        
        // Economy commands
        "balance", "bal", "pay", "economy", "eco", "kit", "kits",
        
        // Player features
        "nick", "afk", "playtime", "achievements", "preferences",
        
        // Admin commands
        "config", "language", "lang", "permissions", "bossbar", "neoanimations",
        "placeholder", "webdashboard", "status",
        
        // Shop commands
        "signshop", "shop",
        
        // NeoEssentials specific commands
        "neoessentials", "ne", "version", "features", "commands"
    );
    
    // Tab completion suggestion provider for all commands
    private static final SuggestionProvider<CommandSourceStack> COMMAND_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(ALL_COMMANDS, builder);
    };
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Main command with tab completion for all available commands
        dispatcher.register(Commands.literal("neoessentials")
            .executes(NeoEssentialsCommand::executeMainCommand)
            .then(Commands.argument("command", StringArgumentType.word())
                .suggests(COMMAND_SUGGESTIONS)
                .executes(NeoEssentialsCommand::executeCommandHelp))
            .then(Commands.literal("version")
                .executes(NeoEssentialsCommand::executeVersionCommand))
            .then(Commands.literal("features")
                .executes(NeoEssentialsCommand::executeFeaturesCommand))
            .then(Commands.literal("commands")
                .executes(NeoEssentialsCommand::executeCommandsCommand))
        );
        
        // Alias with same functionality
        dispatcher.register(Commands.literal("ne")
            .executes(NeoEssentialsCommand::executeMainCommand)
            .then(Commands.argument("command", StringArgumentType.word())
                .suggests(COMMAND_SUGGESTIONS)
                .executes(NeoEssentialsCommand::executeCommandHelp))
            .then(Commands.literal("version")
                .executes(NeoEssentialsCommand::executeVersionCommand))
            .then(Commands.literal("features")
                .executes(NeoEssentialsCommand::executeFeaturesCommand))
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
                MessageUtil.sendMessage(player, "§6Available Commands (use tab completion):§r");
                MessageUtil.sendMessage(player, "§7Type §e/neoessentials <command>§7 and press TAB to see all available commands§r");
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
            ErrorHandler.handleError(
                ErrorHandler.ErrorCategory.COMMAND_EXECUTION,
                ErrorHandler.ErrorSeverity.MEDIUM,
                "NeoEssentials Main Command", e, context.getSource().getPlayer());
            return 0;
        }
    }
    
    private static int executeCommandHelp(CommandContext<CommandSourceStack> context) {
        try {
            var player = context.getSource().getPlayer();
            String commandName = StringArgumentType.getString(context, "command");
            
            if (player != null) {
                MessageUtil.sendMessage(player, "§6=== NeoEssentials Command Help ===§r");
                MessageUtil.sendMessage(player, "§7Command: §e/" + commandName + "§r");
                MessageUtil.sendMessage(player, "");
                
                if (ALL_COMMANDS.contains(commandName.toLowerCase())) {
                    MessageUtil.sendMessage(player, "§aThis command is available in NeoEssentials!§r");
                    MessageUtil.sendMessage(player, "§7Try running: §e/" + commandName + "§r");
                    MessageUtil.sendMessage(player, "");
                    MessageUtil.sendMessage(player, "§7For detailed help on this command, try:§r");
                    MessageUtil.sendMessage(player, "§e/" + commandName + " help§r (if available)");
                    MessageUtil.sendMessage(player, "§e/help " + commandName + "§r");
                } else {
                    MessageUtil.sendMessage(player, "§cCommand not found in NeoEssentials.§r");
                    MessageUtil.sendMessage(player, "§7Available commands:§r");
                    
                    // Show similar commands
                    List<String> similar = ALL_COMMANDS.stream()
                        .filter(cmd -> cmd.contains(commandName.toLowerCase()) || commandName.toLowerCase().contains(cmd))
                        .collect(Collectors.toList());
                    
                    if (!similar.isEmpty()) {
                        MessageUtil.sendMessage(player, "§7Did you mean: §e" + String.join("§7, §e", similar) + "§r");
                    }
                }
                
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, "§7Use §e/neoessentials commands§7 to see all available commands§r");
            }
            
            return 1;
        } catch (Exception e) {
            ErrorHandler.handleError(
                ErrorHandler.ErrorCategory.COMMAND_EXECUTION,
                ErrorHandler.ErrorSeverity.MEDIUM,
                "Command Help", e, context.getSource().getPlayer());
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
            ErrorHandler.handleError(
                ErrorHandler.ErrorCategory.COMMAND_EXECUTION,
                ErrorHandler.ErrorSeverity.MEDIUM,
                "Version Command", e, context.getSource().getPlayer());
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
            ErrorHandler.handleError(
                ErrorHandler.ErrorCategory.COMMAND_EXECUTION,
                ErrorHandler.ErrorSeverity.MEDIUM,
                "Features Command", e, context.getSource().getPlayer());
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
                
                // Show all available commands in organized categories
                MessageUtil.sendMessage(player, "§6=== Essential Utility Commands ===§r");
                MessageUtil.sendMessage(player, "§e/heal, /feed, /god, /vanish, /fly, /speed§r");
                MessageUtil.sendMessage(player, "§e/gamemode (/gm, /gmc, /gms, /gma, /gmsp)§r");
                MessageUtil.sendMessage(player, "§e/repair, /give, /workbench, /anvil§r");
                MessageUtil.sendMessage(player, "");
                
                MessageUtil.sendMessage(player, "§6=== World Management ===§r");
                MessageUtil.sendMessage(player, "§e/time, /weather§r");
                MessageUtil.sendMessage(player, "");
                
                MessageUtil.sendMessage(player, "§6=== Moderation Commands ===§r");
                MessageUtil.sendMessage(player, "§e/ban, /kick, /mute§r");
                MessageUtil.sendMessage(player, "§e/list, /whois, /seen§r");
                MessageUtil.sendMessage(player, "");
                
                MessageUtil.sendMessage(player, "§6=== Communication ===§r");
                MessageUtil.sendMessage(player, "§e/msg (/tell), /reply (/r), /mail§r");
                MessageUtil.sendMessage(player, "§e/broadcast, /ignore, /unignore§r");
                MessageUtil.sendMessage(player, "");
                
                MessageUtil.sendMessage(player, "§6=== Teleportation ===§r");
                MessageUtil.sendMessage(player, "§e/teleport (/tp), /tpa, /tpaccept, /tpdeny§r");
                MessageUtil.sendMessage(player, "§e/back, /spawn, /setspawn§r");
                MessageUtil.sendMessage(player, "");
                
                MessageUtil.sendMessage(player, "§6=== Homes & Warps ===§r");
                MessageUtil.sendMessage(player, "§e/home, /sethome, /delhome, /homes§r");
                MessageUtil.sendMessage(player, "§e/warp, /setwarp, /delwarp, /warps§r");
                MessageUtil.sendMessage(player, "");
                
                MessageUtil.sendMessage(player, "§6=== Economy & Items ===§r");
                MessageUtil.sendMessage(player, "§e/balance (/bal), /pay, /economy (/eco)§r");
                MessageUtil.sendMessage(player, "§e/kit, /kits, /shop, /signshop§r");
                MessageUtil.sendMessage(player, "");
                
                MessageUtil.sendMessage(player, "§6=== Player Features ===§r");
                MessageUtil.sendMessage(player, "§e/nick, /afk, /playtime§r");
                MessageUtil.sendMessage(player, "§e/achievements, /preferences§r");
                MessageUtil.sendMessage(player, "");
                
                MessageUtil.sendMessage(player, "§6=== Information & Help ===§r");
                MessageUtil.sendMessage(player, "§e/help, /info (/serverinfo), /motd, /rules§r");
                MessageUtil.sendMessage(player, "§e/neoessentials (/ne), /version, /features§r");
                MessageUtil.sendMessage(player, "");
                
                MessageUtil.sendMessage(player, "§6=== Admin Commands ===§r");
                MessageUtil.sendMessage(player, "§e/config, /language (/lang), /permissions§r");
                MessageUtil.sendMessage(player, "§e/bossbar, /neoanimations, /placeholder§r");
                MessageUtil.sendMessage(player, "§e/webdashboard, /status§r");
                MessageUtil.sendMessage(player, "");
                
                MessageUtil.sendMessage(player, "§7Use §e/neoessentials <command>§7 with tab completion to explore!§r");
                MessageUtil.sendMessage(player, "§7Total Commands Available: §e" + ALL_COMMANDS.size() + "§r");
            }
            return 1;
        } catch (Exception e) {
            ErrorHandler.handleError(
                ErrorHandler.ErrorCategory.COMMAND_EXECUTION,
                ErrorHandler.ErrorSeverity.MEDIUM,
                "Commands List", e, context.getSource().getPlayer());
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
