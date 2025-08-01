package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.managers.SpawnManager;
import com.zerog.neoessentials.util.LocationUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Spawn command implementation
 * Handles /spawn, /setspawn commands
 */
public class SpawnCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /spawn - Teleport to spawn
        dispatcher.register(Commands.literal("spawn")
            .executes(SpawnCommands::teleportSpawn)
        );
        
        // /setspawn - Set spawn location
        dispatcher.register(Commands.literal("setspawn")
            .requires(source -> source.hasPermission(2))
            .executes(SpawnCommands::setSpawn)
        );
    }
    
    private static int teleportSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        SpawnManager spawnManager = SpawnManager.getInstance();
        
        boolean success = spawnManager.teleportToSpawn(player);
        return success ? 1 : 0;
    }
    
    private static int setSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        SpawnManager spawnManager = SpawnManager.getInstance();
        
        // Get player's current location
        LocationUtil.Location location = new LocationUtil.Location(
            player.serverLevel().dimension().location().toString(),
            player.getX(), player.getY(), player.getZ(),
            player.getYRot(), player.getXRot()
        );
        
        boolean success = spawnManager.setSpawn(player, location);
        return success ? 1 : 0;
    }
}
