package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRootCommand {
    private static final List<String> MOD_COMMANDS = Arrays.asList(
        "enchant", "enchanthand", "eco", "balance", "bal", "pay", "baltop", "paytoggle", "pex", "permissions"
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("neoe")
                .then(Commands.argument("command", StringArgumentType.word())
                    .suggests(ModRootCommand::suggestModCommands)
                    .executes(ModRootCommand::dispatchToModCommand)
                )
        );
        dispatcher.register(
            Commands.literal("neoessentials")
                .then(Commands.argument("command", StringArgumentType.word())
                    .suggests(ModRootCommand::suggestModCommands)
                    .executes(ModRootCommand::dispatchToModCommand)
                )
        );
    }

    private static CompletableFuture<Suggestions> suggestModCommands(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return net.minecraft.commands.SharedSuggestionProvider.suggest(MOD_COMMANDS, builder);
    }

    private static int dispatchToModCommand(CommandContext<CommandSourceStack> ctx) {
        String command = StringArgumentType.getString(ctx, "command");
        CommandSourceStack source = ctx.getSource();
        // Optionally, you could dispatch the command as if the player typed it
        if (source.getEntity() instanceof ServerPlayer player) {
            player.server.getCommands().performPrefixedCommand(source, "/" + command);
            return 1;
        } else {
            source.sendFailure(net.minecraft.network.chat.Component.literal("Only players can use this root command."));
            return 0;
        }
    }
}
