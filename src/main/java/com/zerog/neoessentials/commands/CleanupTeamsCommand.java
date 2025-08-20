package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class CleanupTeamsCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            net.minecraft.commands.Commands.literal("neessentials")
                .then(net.minecraft.commands.Commands.literal("cleanupteams")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        com.zerog.neoessentials.features.TablistScoreboardManager.getInstance().cleanupAllNeoEssentialsTeamsAndScoreboards();
                        ctx.getSource().sendSuccess(() -> Component.literal("NeoEssentials teams and scoreboards cleaned up."), true);
                        return 1;
                    })
                )
        );
    }
}
