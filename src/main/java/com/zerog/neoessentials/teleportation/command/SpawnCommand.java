package com.zerog.neoessentials.teleportation.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public class SpawnCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            net.minecraft.commands.Commands.literal("spawn")
                .executes(ctx -> {
                    var source = ctx.getSource();
                    if (!(source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
                        source.sendFailure(com.zerog.neoessentials.util.MessageUtil.error("commands.neoessentials.player_only"));
                        return 0;
                    }
                    var level = player.serverLevel();
                    var spawnPos = level.getSharedSpawnPos();
                    var spawnLoc = new com.zerog.neoessentials.teleportation.TeleportLocation(
                        level,
                        spawnPos,
                        0f,
                        0f,
                        player.getName().getString()
                    );
                    com.zerog.neoessentials.teleportation.TeleportUtil.teleportPlayer(player, spawnLoc);
                    player.sendSystemMessage(com.zerog.neoessentials.util.MessageUtil.success("commands.neoessentials.teleport.spawn.success"));
                    return 1;
                })
        );
    }
}
