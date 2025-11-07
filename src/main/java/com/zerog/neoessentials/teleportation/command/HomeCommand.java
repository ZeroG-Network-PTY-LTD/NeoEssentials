package com.zerog.neoessentials.teleportation.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public class HomeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            net.minecraft.commands.Commands.literal("home")
                .executes(ctx -> {
                    var source = ctx.getSource();
                    if (!(source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
                        source.sendFailure(com.zerog.neoessentials.util.MessageUtil.error("commands.neoessentials.player_only"));
                        return 0;
                    }
                    com.zerog.neoessentials.teleportation.HomeManager.getInstance().teleportToDefaultHome(player);
                    return 1;
                })
        );
    }
}
