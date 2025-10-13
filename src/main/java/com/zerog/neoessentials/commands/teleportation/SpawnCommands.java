package com.zerog.neoessentials.commands.teleportation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.teleportation.Spawn.SpawnManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Commands for the spawn teleportation system:
 * - /spawn - Teleport to spawn
 * - /setspawn [coordinates] - Set spawn location (admin)
 * - /spawninfo - Display spawn information
 */
public class SpawnCommands {
    
    // Permission nodes for spawn commands (matching PermissionRegistry)
    private static final String PERMISSION_SPAWN = "neoessentials.teleport.spawn";
    private static final String PERMISSION_SETSPAWN = "neoessentials.teleport.spawn.set";
    private static final String PERMISSION_SPAWNINFO = "neoessentials.teleport.spawn.info";
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ConfigManager config = ConfigManager.getInstance();
        
        // Only register if teleportation module is enabled
        if (config.isTeleportationEnabled()) {
            // Register individual commands based on their command settings
            if (config.isCommandEnabled("spawn")) {
                registerSpawnCommand(dispatcher);
                registerSpawnInfoCommand(dispatcher); // Include info with spawn
            }
            if (config.isCommandEnabled("setspawn")) {
                registerSetSpawnCommand(dispatcher);
            }
        }
    }
    
    /**
     * Register /spawn command with aliases
     */
    private static void registerSpawnCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerSpawnCommandWithName(dispatcher, "spawn");
        registerSpawnCommandWithName(dispatcher, "hub");
    }
    
    private static void registerSpawnCommandWithName(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(Commands.literal(commandName)
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_SPAWN);
                }
                return source.hasPermission(2); // Console fallback
            })
            .executes(SpawnCommands::executeSpawn)
        );
    }
    
    /**
     * Register /setspawn command
     */
    private static void registerSetSpawnCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("setspawn")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_SETSPAWN);
                }
                return source.hasPermission(3); // Console fallback
            })
            .executes(SpawnCommands::executeSetSpawnHere)
            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(SpawnCommands::executeSetSpawnAt)
            )
        );
    }
    
    /**
     * Register /spawninfo command with aliases
     */
    private static void registerSpawnInfoCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerSpawnInfoCommandWithName(dispatcher, "spawninfo");
        registerSpawnInfoCommandWithName(dispatcher, "spawni");
    }
    
    private static void registerSpawnInfoCommandWithName(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(Commands.literal(commandName)
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_SPAWNINFO);
                }
                return source.hasPermission(2); // Console fallback
            })
            .executes(SpawnCommands::executeSpawnInfo)
        );
    }
    
    /**
     * Execute /spawn
     */
    private static int executeSpawn(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        SpawnManager spawnManager = SpawnManager.getInstance();
        
        spawnManager.teleportToSpawn(player);
        return 1;
    }
    
    /**
     * Execute /setspawn (at current location)
     */
    private static int executeSetSpawnHere(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        SpawnManager spawnManager = SpawnManager.getInstance();
        
        // Check permission
        if (!hasSetSpawnPermission(player)) {
            context.getSource().sendFailure(MessageUtil.error("teleport.spawn.no_permission"));
            return 0;
        }
        
        if (spawnManager.setSpawn(player)) {
            return 1;
        }
        return 0;
    }
    
    /**
     * Execute /setspawn <coordinates>
     */
    private static int executeSetSpawnAt(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        SpawnManager spawnManager = SpawnManager.getInstance();
        
        // Check permission
        if (!hasSetSpawnPermission(player)) {
            context.getSource().sendFailure(MessageUtil.error("teleport.spawn.no_permission"));
            return 0;
        }
        
        try {
            BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
            ServerLevel level = player.serverLevel();
            
            if (spawnManager.setSpawn(player, level, pos)) {
                return 1;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(MessageUtil.error("teleport.spawn.invalid_coordinates"));
        }
        return 0;
    }
    
    /**
     * Execute /spawninfo
     */
    private static int executeSpawnInfo(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        SpawnManager spawnManager = SpawnManager.getInstance();
        
        String info = spawnManager.getSpawnInfo();
        player.sendSystemMessage(MessageUtil.info(info));
        
        // Show statistics if player has admin permission
        if (hasSetSpawnPermission(player)) {
            String stats = spawnManager.getStatistics();
            player.sendSystemMessage(MessageUtil.component(stats));
        }
        
        return 1;
    }
    
    /**
     * Check if player has permission to set spawn
     */
    private static boolean hasSetSpawnPermission(ServerPlayer player) {
        return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_SETSPAWN);
    }
}