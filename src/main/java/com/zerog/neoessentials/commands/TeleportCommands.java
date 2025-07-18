package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.SpawnManager;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.utils.TeleportUtil;
import com.zerog.neoessentials.data.TeleportHistoryManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Handles all teleport-related commands, including /tpa, /tpahere, /back, and /spawn.
 */
public class TeleportCommands {
    
    /**
     * Registers all teleport-related commands.
     *
     * @param dispatcher The command dispatcher to register commands with
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register /tpa command
        dispatcher.register(
            Commands.literal("tpa")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.tpa"))
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            
                            // Send teleport request
                            boolean success = TeleportUtil.createTeleportRequest(source, target, true);
                            
                            if (success) {
                                LanguageUtil.sendMessage(source, "neoessentials.teleport.request_sent", target.getScoreboardName());
                                LanguageUtil.sendMessage(target, "neoessentials.teleport.request_received", source.getScoreboardName());
                            } else {
                                LanguageUtil.sendErrorMessage(source, "neoessentials.teleport.already_pending");
                            }
                            
                            return 1;
                        })
                )
        );

        // Register /tpahere command
        dispatcher.register(
            Commands.literal("tpahere")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.tpahere"))
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                              // Send teleport here request
                            boolean success = TeleportUtil.createTeleportRequest(source, target, false);
                            
                            if (success) {
                                LanguageUtil.sendMessage(source, "neoessentials.teleport.request_sent", target.getScoreboardName());
                                LanguageUtil.sendMessage(target, "neoessentials.teleport.request_received_here", source.getScoreboardName());
                            } else {
                                LanguageUtil.sendErrorMessage(source, "neoessentials.teleport.already_pending");
                            }
                            
                            return 1;
                        })
                )
        );

        // Register /tpaccept command
        dispatcher.register(
            Commands.literal("tpaccept")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.tpaccept"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Accept teleport request
                    boolean success = TeleportUtil.acceptTeleportRequest(player);
                    
                    if (!success) {
                        LanguageUtil.sendErrorMessage(player, "neoessentials.teleport.no_pending_request");
                    }
                    
                    return success ? 1 : 0;
                })
        );

        // Register /tpdeny command
        dispatcher.register(
            Commands.literal("tpdeny")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.tpdeny"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Deny teleport request
                    boolean hadRequest = TeleportUtil.denyTeleportRequest(player);
                    
                    if (hadRequest) {
                        LanguageUtil.sendMessage(player, "neoessentials.teleport.request_denied");
                    } else {
                        LanguageUtil.sendErrorMessage(player, "neoessentials.teleport.no_pending_request");
                    }
                    
                    return 1;
                })
        );

        // Register /back command
        dispatcher.register(
            Commands.literal("back")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.back"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Try to teleport using the new history manager
                    TeleportHistoryManager historyManager = NeoEssentials.getInstance().getDataManager().getTeleportHistoryManager();
                    boolean success = historyManager.teleportBack(player);
                    
                    // Fall back to the old method if we have no history
                    if (!success) {
                        success = TeleportUtil.teleportToLastLocation(player);
                    }
                    
                    if (success) {
                        LanguageUtil.sendMessage(player, "neoessentials.teleport.back_success");
                    } else {
                        LanguageUtil.sendErrorMessage(player, "neoessentials.teleport.back_no_location");
                    }
                    
                    return success ? 1 : 0;
                })
        );

        // Register /spawn command
        dispatcher.register(
            Commands.literal("spawn")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.spawn"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Get the spawn manager
                    SpawnManager spawnManager = NeoEssentials.getInstance().getDataManager().getSpawnManager();
                    
                    // Get the spawn location
                    ServerLevel level = spawnManager.getSpawnLevel(player.getServer());
                    Vec3 pos = spawnManager.getSpawnPosition();
                    
                    // Teleport to spawn
                    boolean success = TeleportUtil.teleportPlayer(player, level, pos, true);
                    
                    if (success) {
                        LanguageUtil.sendMessage(player, "neoessentials.spawn.teleported");
                    } else {
                        LanguageUtil.sendErrorMessage(player, "neoessentials.spawn.teleport_failed");
                    }
                    
                    return success ? 1 : 0;
                })
        );

        // Register /setspawn command
        dispatcher.register(
            Commands.literal("setspawn")
                .requires(source -> PermissionUtil.hasAdminPermission(source, "neoessentials.command.setspawn"))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Get the spawn manager
                    SpawnManager spawnManager = NeoEssentials.getInstance().getDataManager().getSpawnManager();
                    
                    // Set spawn location
                    boolean success = spawnManager.setSpawn(player);
                    
                    if (success) {
<<<<<<< HEAD
                        MessageUtil.sendSuccessMessage(player, "Spawn location set to your current position.");
                    } else {
                        MessageUtil.sendErrorMessage(player, "Failed to set spawn location.");
                    }
                    
                    return success ? 1 : 0;
                })
        );
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> bac244b (Implement messaging and player state commands)

        // Register /top command
        dispatcher.register(
            Commands.literal("top")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.top"))                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
<<<<<<< HEAD
<<<<<<< HEAD
                      // Get the player's current level
                    ServerLevel level = player.serverLevel();
=======
                    
                    // Get the player's current level
                    ServerLevel level = player.getLevel();
>>>>>>> bac244b (Implement messaging and player state commands)
=======
                      // Get the player's current level
                    ServerLevel level = player.serverLevel();
>>>>>>> 009105b (fix: Improve unban logic to directly remove banned players by name and streamline teleport command level retrieval)
                    
                    // Find the highest block at the player's current x,z position
                    int highestY = findHighestBlock(level, (int)player.getX(), (int)player.getZ());
                    
                    // Teleport the player to the highest block, with a slight offset to avoid suffocation
                    boolean success = TeleportUtil.teleportPlayer(player, level, new Vec3(player.getX(), highestY, player.getZ()), true);
                    
                    if (success) {
                        MessageUtil.sendSuccessMessage(player, "Teleported to the highest block above you.");
                    } else {
                        MessageUtil.sendErrorMessage(player, "Failed to teleport to the highest block.");
=======
                        LanguageUtil.sendMessage(player, "neoessentials.spawn.set_success");
                    } else {
                        LanguageUtil.sendErrorMessage(player, "neoessentials.spawn.set_failed");
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
                    }
                    
                    return success ? 1 : 0;
                })
        );

<<<<<<< HEAD
        // Register /bottom command
        dispatcher.register(
            Commands.literal("bottom")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.bottom"))                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
<<<<<<< HEAD
<<<<<<< HEAD
                      // Get the player's current level
                    ServerLevel level = player.serverLevel();
=======
                    
                    // Get the player's current level
                    ServerLevel level = player.getLevel();
>>>>>>> bac244b (Implement messaging and player state commands)
=======
                      // Get the player's current level
                    ServerLevel level = player.serverLevel();
>>>>>>> 009105b (fix: Improve unban logic to directly remove banned players by name and streamline teleport command level retrieval)
                    
                    // Find the lowest block at the player's current x,z position
                    int lowestY = findLowestBlock(level, (int)player.getX(), (int)player.getZ());
                    
                    // Teleport the player to the lowest block, with a slight offset to avoid suffocation
                    boolean success = TeleportUtil.teleportPlayer(player, level, new Vec3(player.getX(), lowestY, player.getZ()), true);
                    
                    if (success) {
                        MessageUtil.sendSuccessMessage(player, "Teleported to the lowest block below you.");
                    } else {
                        MessageUtil.sendErrorMessage(player, "Failed to teleport to the lowest block.");
                    }
                    
                    return success ? 1 : 0;
                })        );
        
        // Register /top command
        dispatcher.register(
            Commands.literal("top")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.top"))
=======
        // Register /top command
        dispatcher.register(
            Commands.literal("top")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.top"))
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();
                    
                    int x = player.blockPosition().getX();
                    int z = player.blockPosition().getZ();
                    int topY = findHighestBlock(level, x, z);
                    
                    // Record current position for /back
                    TeleportHistoryManager historyManager = NeoEssentials.getInstance().getDataManager().getTeleportHistoryManager();
                    historyManager.recordPosition(player);
                    
                    // Teleport to the top
                    boolean success = TeleportUtil.teleport(player, level, x + 0.5, topY, z + 0.5, player.getYRot(), player.getXRot());
                    
                    if (success) {
<<<<<<< HEAD
                        MessageUtil.sendSuccessMessage(player, "Teleported to the highest point.");
                    } else {
                        MessageUtil.sendErrorMessage(player, "Failed to teleport to the highest point.");
=======
                        LanguageUtil.sendMessage(player, "neoessentials.teleport.top_success");
                    } else {
                        LanguageUtil.sendErrorMessage(player, "neoessentials.teleport.top_failed");
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
                    }
                    
                    return success ? 1 : 0;
                })
        );
        
        // Register /bottom command
        dispatcher.register(
            Commands.literal("bottom")
<<<<<<< HEAD
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.bottom"))
=======
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.bottom"))
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ServerLevel level = player.serverLevel();
                    
                    int x = player.blockPosition().getX();
                    int z = player.blockPosition().getZ();
                    int bottomY = findLowestBlock(level, x, z);
                    
                    // Record current position for /back
                    TeleportHistoryManager historyManager = NeoEssentials.getInstance().getDataManager().getTeleportHistoryManager();
                    historyManager.recordPosition(player);
                    
                    // Teleport to the bottom
                    boolean success = TeleportUtil.teleport(player, level, x + 0.5, bottomY, z + 0.5, player.getYRot(), player.getXRot());
                    
                    if (success) {
<<<<<<< HEAD
                        MessageUtil.sendSuccessMessage(player, "Teleported to the lowest point.");
                    } else {
                        MessageUtil.sendErrorMessage(player, "Failed to teleport to the lowest point.");
=======
                        LanguageUtil.sendMessage(player, "neoessentials.teleport.bottom_success");
                    } else {
                        LanguageUtil.sendErrorMessage(player, "neoessentials.teleport.bottom_failed");
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
                    }
                    
                    return success ? 1 : 0;
                })
        );
        
        NeoEssentials.LOGGER.info("Registered teleport commands");
    }

    /**
     * Finds the highest non-air block at the given x,z coordinates
     */
    private int findHighestBlock(ServerLevel level, int x, int z) {
        int y = level.getMaxBuildHeight();
        while (y > level.getMinBuildHeight()) {
            y--;
            if (!level.getBlockState(new net.minecraft.core.BlockPos(x, y, z)).isAir()) {
                return y + 1; // Return the y-coordinate of the block above the found block
            }
        }
        return level.getMinBuildHeight();
    }

    /**
     * Finds the lowest non-air block at the given x,z coordinates
     */
    private int findLowestBlock(ServerLevel level, int x, int z) {
        int maxY = Math.min(level.getMaxBuildHeight(), 319); // Limit search to a reasonable height
        for (int y = level.getMinBuildHeight(); y < maxY; y++) {
            if (!level.getBlockState(new net.minecraft.core.BlockPos(x, y, z)).isAir() && 
                level.getBlockState(new net.minecraft.core.BlockPos(x, y + 1, z)).isAir()) {
                return y + 1;
            }
        }
        return level.getMinBuildHeight();
    }
<<<<<<< HEAD
<<<<<<< HEAD
=======
        
        NeoEssentials.LOGGER.info("Registered teleport commands");
    }
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> bac244b (Implement messaging and player state commands)
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
}
