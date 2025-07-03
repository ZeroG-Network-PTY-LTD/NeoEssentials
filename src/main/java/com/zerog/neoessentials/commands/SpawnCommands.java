package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.SpawnManager;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Commands for spawn point management and teleportation.
 * 
 * <p>This class provides commands for managing server spawn points and teleporting players:
 * <ul>
 *   <li>{@code /spawn} - Teleport to the server spawn point</li>
 *   <li>{@code /setspawn} - Set the server spawn point at current location</li>
 *   <li>{@code /spawn <player>} - Teleport another player to spawn (admin)</li>
 * </ul>
 * 
 * <p>The spawn system supports per-world spawn points and includes safety checks
 * for spawn location validity and player teleportation permissions.
 * 
 * @author ZeroG
 * @since 1.0.2.97
 */
public class SpawnCommands {
    
    /**
     * Registers all spawn-related commands with the command dispatcher.
     * 
     * @param dispatcher The command dispatcher to register commands with
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerSpawnCommand(dispatcher);
        registerSetSpawnCommand(dispatcher);
    }
    
    /**
     * Registers the /spawn command and its variants.
     * 
     * @param dispatcher The command dispatcher
     */
    private static void registerSpawnCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawn")
            .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.spawn"))
            .executes(context -> executeSpawn(context))
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.spawn.others"))
                .suggests(TabCompletionUtil.ONLINE_PLAYER_SUGGESTIONS)
                .executes(context -> executeSpawnForPlayer(context, EntityArgument.getPlayer(context, "player")))
            )
        );
    }
    
    /**
     * Registers the /setspawn command.
     * 
     * @param dispatcher The command dispatcher
     */
    private static void registerSetSpawnCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("setspawn")
            .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.setspawn"))
            .executes(context -> executeSetSpawn(context))
        );
    }
    
    /**
     * Executes the /spawn command for the command sender.
     * 
     * @param context The command context
     * @return Command result
     */
    private static int executeSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return executeSpawnForPlayer(context, player);
    }
    
    /**
     * Executes the /spawn command for a specific player.
     * 
     * @param context The command context
     * @param target The target player to teleport to spawn
     * @return Command result
     */
    private static int executeSpawnForPlayer(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        try {
            // Get the spawn manager instance
            SpawnManager spawnManager = null;
            if (NeoEssentials.getInstance() != null && NeoEssentials.getInstance().getDataManager() != null) {
                spawnManager = NeoEssentials.getInstance().getDataManager().getSpawnManager();
            }
            
            if (spawnManager == null) {
                LanguageUtil.sendErrorMessage(context.getSource(), "spawn.manager.unavailable");
                return 0;
            }
            
            SpawnManager.SpawnLocation spawnLocation = spawnManager.getSpawnLocation();
            if (spawnLocation == null) {
                // Fallback to world spawn if no custom spawn is set
                ServerLevel world = target.serverLevel();
                BlockPos worldSpawn = world.getSharedSpawnPos();
                target.teleportTo(world, worldSpawn.getX() + 0.5, worldSpawn.getY(), worldSpawn.getZ() + 0.5, 
                                target.getYRot(), target.getXRot());
                
                LanguageUtil.sendMessage(target, "spawn.teleported_world");
                if (context.getSource().getEntity() instanceof ServerPlayer executor && !executor.equals(target)) {
                    LanguageUtil.sendMessage(executor, "spawn.teleported_other_world", target.getDisplayName().getString());
                }
                return 1;
            }
            
            // Get the spawn world
            ServerLevel spawnWorld = spawnManager.getSpawnLevel(target.getServer());
            if (spawnWorld == null) {
                LanguageUtil.sendErrorMessage(context.getSource(), "spawn.world.unavailable");
                return 0;
            }
            
            // Get spawn position
            BlockPos spawnPos = spawnLocation.getBlockPos();
            
            // Find safe spawn location
            BlockPos safeSpawn = findSafeSpawnLocation(spawnWorld, spawnPos);
            if (safeSpawn == null) {
                LanguageUtil.sendErrorMessage(context.getSource(), "spawn.location.unsafe");
                return 0;
            }
            
            // Teleport player
            target.teleportTo(spawnWorld, safeSpawn.getX() + 0.5, safeSpawn.getY(), safeSpawn.getZ() + 0.5, 
                            spawnLocation.getYaw(), spawnLocation.getPitch());
            
            // Send success messages
            if (context.getSource().getEntity() instanceof ServerPlayer executor && !executor.equals(target)) {
                LanguageUtil.sendMessage(executor, "spawn.teleported_other", target.getDisplayName().getString());
            }
            
            LanguageUtil.sendMessage(target, "spawn.teleported");
            
            // Log the teleport
            NeoEssentials.LOGGER.info("Player {} teleported {} to spawn at {}", 
                context.getSource().getTextName(), target.getDisplayName().getString(), safeSpawn);
            
            return 1;
            
        } catch (Exception e) {
            LanguageUtil.sendErrorMessage(context.getSource(), "spawn.teleport.error", e.getMessage());
            NeoEssentials.LOGGER.error("Error in /spawn command", e);
            return 0;
        }
    }
    
    /**
     * Executes the /setspawn command.
     * 
     * @param context The command context
     * @return Command result
     */
    private static int executeSetSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        try {
            // Get the spawn manager instance
            SpawnManager spawnManager = null;
            if (NeoEssentials.getInstance() != null && NeoEssentials.getInstance().getDataManager() != null) {
                spawnManager = NeoEssentials.getInstance().getDataManager().getSpawnManager();
            }
            
            if (spawnManager == null) {
                LanguageUtil.sendErrorMessage(context.getSource(), "spawn.manager.unavailable");
                return 0;
            }
            
            BlockPos playerPos = player.blockPosition();
            ServerLevel world = player.serverLevel();
            
            // Validate spawn location
            if (!isValidSpawnLocation(world, playerPos)) {
                LanguageUtil.sendErrorMessage(context.getSource(), "spawn.location.invalid");
                return 0;
            }
            
            // Set the spawn point using the existing SpawnManager
            boolean success = spawnManager.setSpawn(player);
            
            if (success) {
                // Update world spawn as well
                world.setDefaultSpawnPos(playerPos, player.getYRot());
                
                // Send success message
                LanguageUtil.sendMessage(player, "spawn.set.success", 
                    String.valueOf(playerPos.getX()), 
                    String.valueOf(playerPos.getY()), 
                    String.valueOf(playerPos.getZ()));
                
                // Log the change
                NeoEssentials.LOGGER.info("Player {} set spawn point at {} in world {}", 
                    context.getSource().getTextName(), playerPos, world.dimension().location());
                
                return 1;
            } else {
                LanguageUtil.sendErrorMessage(context.getSource(), "spawn.set.failed");
                return 0;
            }
            
        } catch (Exception e) {
            LanguageUtil.sendErrorMessage(context.getSource(), "spawn.set.error", e.getMessage());
            NeoEssentials.LOGGER.error("Error in /setspawn command", e);
            return 0;
        }
    }
    
    /**
     * Finds a safe spawn location near the specified position.
     * 
     * @param world The world to search in
     * @param spawnPos The desired spawn position
     * @return A safe spawn position, or null if none found
     */
    private static BlockPos findSafeSpawnLocation(ServerLevel world, BlockPos spawnPos) {
        // Check if the original position is safe
        if (isValidSpawnLocation(world, spawnPos)) {
            return spawnPos;
        }
        
        // Search in a small radius for a safe location
        for (int y = -2; y <= 5; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos checkPos = spawnPos.offset(x, y, z);
                    if (isValidSpawnLocation(world, checkPos)) {
                        return checkPos;
                    }
                }
            }
        }
        
        return null; // No safe location found
    }
    
    /**
     * Checks if a location is valid for spawning players.
     * 
     * @param world The world to check in
     * @param pos The position to check
     * @return true if the location is safe for spawning
     */
    private static boolean isValidSpawnLocation(ServerLevel world, BlockPos pos) {
        try {
            // Check if position is within world bounds
            if (!world.isInWorldBounds(pos)) {
                return false;
            }
            
            // Check if the spawn location has solid ground
            BlockPos groundPos = pos.below();
            if (!world.getBlockState(groundPos).isSolid()) {
                return false;
            }
            
            // Check if there's enough space for a player (2 blocks high)
            if (!world.getBlockState(pos).isAir() || !world.getBlockState(pos.above()).isAir()) {
                return false;
            }
            
            // Check for dangerous blocks (lava, fire, etc.)
            if (world.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.LAVA) ||
                world.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.FIRE) ||
                world.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.CACTUS)) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error checking spawn location validity", e);
            return false;
        }
    }
}
