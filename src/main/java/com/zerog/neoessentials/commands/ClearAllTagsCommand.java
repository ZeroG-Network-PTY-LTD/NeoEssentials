package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public class ClearAllTagsCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            net.minecraft.commands.Commands.literal("neessentials")
                .then(net.minecraft.commands.Commands.literal("clearalltags")
                    .requires(source -> source.hasPermission(2))
                    .executes(ClearAllTagsCommand::clearAllTags)
                )
        );
    }

    private static int clearAllTags(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Reset display name to vanilla
            player.setCustomName(null);
            // Optionally, reset nickname if you use a nickname system
            // NickCommand.clearNickname(player.getUUID());
        }
        context.getSource().sendSuccess(() -> Component.literal("All player tags and display names have been reset to vanilla."), true);
        return 1;
    }
}
