package com.zerog.neoessentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.integration.FTBIntegrationHelper;

public class FTBIntegrationCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("teaminfo")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    FTBIntegrationHelper.safeTeamInfo(player);
                    return Command.SINGLE_SUCCESS;
                })
        );

        dispatcher.register(
            Commands.literal("checkrank")
                .then(Commands.argument("permission", StringArgumentType.word())
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        FTBIntegrationHelper.safeRankInfo(player);
                        return Command.SINGLE_SUCCESS;
                    })
                )
        );
    }
}
