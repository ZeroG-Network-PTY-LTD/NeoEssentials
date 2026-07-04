package com.zerog.neoessentials.teleportation.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import com.zerog.neoessentials.util.PermissionValidator;
import com.zerog.neoessentials.util.MessageUtil;

public class SpawnCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            net.minecraft.commands.Commands.literal("spawn")
                .executes(ctx -> {
                    var source = ctx.getSource();
                    if (!(source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.player_only"));
                        return 0;
                    }
                    
                    // Check permission
                    PermissionValidator.PermissionResult permResult = 
                        PermissionValidator.validatePermission(source, "neoessentials.teleport.spawn");
                    if (!permResult.hasPermission()) {
                        source.sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    
                    var level = com.zerog.neoessentials.util.LevelCompat.of(player);
                    var spawnPos = level.getRespawnData().pos();
                    var spawnLoc = new com.zerog.neoessentials.teleportation.TeleportLocation(
                        level,
                        spawnPos,
                        0f,
                        0f,
                        player.getName().getString()
                    );
                    com.zerog.neoessentials.teleportation.TeleportUtil.teleportPlayer(player, spawnLoc);
                    player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.spawn.success"));
                    return 1;
                })
        );
    }
}
