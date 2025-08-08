package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.managers.SpawnManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Spawn command implementation for NeoEssentials
 * Handles spawn teleportation and spawn setting using SpawnManager
 * 
 * Commands:
 * - /spawn - Teleport to spawn location
 * - /setspawn - Set spawn location at current position
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class SpawnCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /spawn - Teleport to spawn
        dispatcher.register(Commands.literal("spawn")
            .requires(source -> source.getEntity() instanceof ServerPlayer)
            .executes(SpawnCommand::teleportToSpawn)
        );
        
        // /setspawn - Set spawn location
        dispatcher.register(Commands.literal("setspawn")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC)) // Op level 3
            .executes(SpawnCommand::setSpawn)
        );
    }
    
    /**
     * Execute /spawn command to teleport to spawn
     */
    private static int teleportToSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        SpawnManager spawnManager = SpawnManager.getInstance();
        
        boolean success = spawnManager.teleportToSpawn(player);
        return success ? 1 : 0;
    }
    
    /**
     * Execute /setspawn command to set spawn at current location
     */
    private static int setSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        SpawnManager spawnManager = SpawnManager.getInstance();
        
        boolean success = spawnManager.setSpawn(player);
        return success ? 1 : 0;
    }
}
