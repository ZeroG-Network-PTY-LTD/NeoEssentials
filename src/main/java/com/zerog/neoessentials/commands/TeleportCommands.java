package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.SpawnManager;
import com.zerog.neoessentials.utils.MessageUtil;
import com.zerog.neoessentials.utils.TeleportUtil;
import com.zerog.neoessentials.utils.TeleportHistory;
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
        dispatcher.register(            Commands.literal("tpa")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.tpa"))
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            
                            // Send teleport request
                            boolean success = TeleportUtil.createTeleportRequest(source, target, true);
                            
                            if (success) {
                                MessageUtil.sendMessage(source, "Teleport request sent to " + target.getScoreboardName());
                                MessageUtil.sendMessage(target, source.getScoreboardName() + " has requested to teleport to you. Type /tpaccept to accept or /tpdeny to deny.");
                            } else {
                                MessageUtil.sendErrorMessage(source, "You already have a pending teleport request with this player.");
                            }
                            
                            return 1;
                        })
                )
        );

        // Register /tpahere command
        dispatcher.register(
            Commands.literal("tpahere")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.tpahere"))
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                              // Send teleport here request
                            boolean success = TeleportUtil.createTeleportRequest(source, target, false);
                            
                            if (success) {
                                MessageUtil.sendMessage(source, "Teleport request sent to " + target.getScoreboardName());
                                MessageUtil.sendMessage(target, source.getScoreboardName() + " has requested you to teleport to them. Type /tpaccept to accept or /tpdeny to deny.");
                            } else {
                                MessageUtil.sendErrorMessage(source, "You already have a pending teleport request with this player.");
                            }
                            
                            return 1;
                        })
                )
        );

        // Register /tpaccept command
        dispatcher.register(
            Commands.literal("tpaccept")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.tpaccept"))                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Accept teleport request
                    boolean success = TeleportUtil.acceptTeleportRequest(player);
                    
                    if (!success) {
                        MessageUtil.sendErrorMessage(player, "You have no pending teleport requests.");
                    }
                    
                    return success ? 1 : 0;
                })
        );

        // Register /tpdeny command
        dispatcher.register(
            Commands.literal("tpdeny")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.tpdeny"))                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Deny teleport request
                    boolean hadRequest = TeleportUtil.denyTeleportRequest(player);
                    
                    if (hadRequest) {
                        MessageUtil.sendMessage(player, "Teleport request denied.");
                    } else {
                        MessageUtil.sendErrorMessage(player, "You have no pending teleport requests.");
                    }
                    
                    return 1;
                })
        );        // Register /back command
        dispatcher.register(
            Commands.literal("back")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.back"))                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Try to teleport using the history stack first
                    boolean success = TeleportHistory.teleportBack(player);
                    
                    // Fall back to the old method if we have no history
                    if (!success) {
                        success = TeleportUtil.teleportToLastLocation(player);
                    }
                    
                    if (success) {
                        MessageUtil.sendSuccessMessage(player, "Teleported back to your previous location.");
                    } else {
                        MessageUtil.sendErrorMessage(player, "You have no previous location to return to.");
                    }
                    
                    return success ? 1 : 0;
                })
        );

        // Register /spawn command
        dispatcher.register(
            Commands.literal("spawn")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.spawn"))                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Get the spawn manager
                    SpawnManager spawnManager = NeoEssentials.getInstance().getDataManager().getSpawnManager();
                    
                    // Get the spawn location
                    ServerLevel level = spawnManager.getSpawnLevel(player.getServer());
                    Vec3 pos = spawnManager.getSpawnPosition();
                    
                    // Teleport to spawn
                    boolean success = TeleportUtil.teleportPlayer(player, level, pos, true);
                    
                    if (success) {
                        MessageUtil.sendSuccessMessage(player, "Teleported to spawn.");
                    } else {
                        MessageUtil.sendErrorMessage(player, "Failed to teleport to spawn.");
                    }
                    
                    return success ? 1 : 0;
                })
        );

        // Register /setspawn command
        dispatcher.register(
            Commands.literal("setspawn")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.setspawn"))                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    
                    // Get the spawn manager
                    SpawnManager spawnManager = NeoEssentials.getInstance().getDataManager().getSpawnManager();
                    
                    // Set spawn location
                    boolean success = spawnManager.setSpawn(player);
                    
                    if (success) {
                        MessageUtil.sendSuccessMessage(player, "Spawn location set to your current position.");
                    } else {
                        MessageUtil.sendErrorMessage(player, "Failed to set spawn location.");
                    }
                    
                    return success ? 1 : 0;
                })
        );
        
        NeoEssentials.LOGGER.info("Registered teleport commands");
    }
}
