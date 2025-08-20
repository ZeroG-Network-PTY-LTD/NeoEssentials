package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.managers.SpawnManager;
import com.zerog.neoessentials.util.LocationUtil;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Spawn command implementation with proper permission checking
 * Handles /spawn, /setspawn commands
 */
public class SpawnCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /spawn - Teleport to spawn
        dispatcher.register(Commands.literal("spawn")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.SPAWN))
            .executes(SpawnCommands::teleportSpawn)
        );
        
        // /setspawn - Set spawn location (admin only)
        dispatcher.register(Commands.literal("setspawn")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.SPAWN_SET))
            .executes(SpawnCommands::setSpawn)
        );
    }
    
    private static int teleportSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Double-check permission (defense in depth)
        if (!PermissionUtil.hasPermission(player, PermissionNodes.SPAWN)) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "spawn.error.no_permission_use"));
            return 0;
        }
        
        SpawnManager spawnManager = SpawnManager.getInstance();
        boolean success = spawnManager.teleportToSpawn(player);
        return success ? 1 : 0;
    }
    
    private static int setSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Double-check permission (defense in depth)
        if (!PermissionUtil.hasPermission(player, PermissionNodes.SPAWN_SET)) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "spawn.error.no_permission_set"));
            return 0;
        }
        
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
