package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.stream.Collectors;

public class ModRootCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModRootCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("neoe")
                .then(Commands.argument("command", StringArgumentType.word())
                    .suggests(ModRootCommand::suggestModCommands)
                    .executes(ModRootCommand::dispatchToModCommand)
                )
                .executes(ModRootCommand::showAvailableCommands) // Show help when no args
        );
        dispatcher.register(
            Commands.literal("neoessentials")
                .then(Commands.argument("command", StringArgumentType.word())
                    .suggests(ModRootCommand::suggestModCommands)
                    .executes(ModRootCommand::dispatchToModCommand)
                )
                .executes(ModRootCommand::showAvailableCommands) // Show help when no args
        );
    }

    private static CompletableFuture<Suggestions> suggestModCommands(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        // Get all available commands from the dynamic registry
        CommandRegistry registry = CommandRegistry.getInstance();
        List<String> commandNames = registry.getAllCommandNames().stream()
            .sorted()
            .collect(Collectors.toList());
        
        return net.minecraft.commands.SharedSuggestionProvider.suggest(commandNames, builder);
    }

    private static int dispatchToModCommand(CommandContext<CommandSourceStack> ctx) {
        String command = StringArgumentType.getString(ctx, "command");
        CommandSourceStack source = ctx.getSource();
        
        // Check if the command is registered in our registry
        CommandRegistry registry = CommandRegistry.getInstance();
        if (!registry.isCommandRegistered(command)) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.root.unknown_command", command));
            source.sendFailure(MessageUtil.info("commands.neoessentials.root.help_hint"));
            return 0;
        }
        
        // Dispatch the command as if the player typed it
        if (source.getEntity() instanceof ServerPlayer player) {
            try {
                player.server.getCommands().performPrefixedCommand(source, "/" + command);
                return 1;
            } catch (Exception e) {
                LOGGER.error("Failed to execute command '{}': {}", command, e.getMessage());
                source.sendFailure(MessageUtil.error("commands.neoessentials.root.execution_failed", command));
                return 0;
            }
        } else {
            source.sendFailure(MessageUtil.error("commands.neoessentials.root.players_only"));
            return 0;
        }
    }
    
    private static int showAvailableCommands(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        CommandRegistry registry = CommandRegistry.getInstance();
        
        List<CommandRegistry.CommandInfo> commands = registry.getAllCommandsSorted();
        
        if (commands.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.warning("commands.neoessentials.root.no_commands"), false);
            return 1;
        }
        
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.help_header"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.root.help_count", commands.size()), false);
        
        for (CommandRegistry.CommandInfo info : commands) {
            if (info.hasAliases()) {
                String aliases = String.join(", /", info.getAliases());
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.root.command_with_aliases", 
                    info.getName(), aliases, info.getDescription()), false);
            } else {
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.root.command_simple", 
                    info.getName(), info.getDescription()), false);
            }
        }
        
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.help_footer"), false);
        
        return 1;
    }
}
