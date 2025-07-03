package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.utils.TeleportUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

/**
 * Direct teleportation commands for administrative use.
 * Handles /tp, /tphere, /tpall, /tppos commands for instant teleportation.
 */
public class DirectTeleportCommands {

    /**
     * Registers all direct teleportation commands.
     *
     * @param dispatcher The command dispatcher to register commands with
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /tp <player> - Teleport to a player
        dispatcher.register(
            Commands.literal("tp")
                .requires(source -> PermissionUtil.hasAdminPermission(source, "neoessentials.command.tp"))
                .then(
                    Commands.argument("target", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                            return teleportToPlayer(context, source, target);
                        })
                )
                // /tp <player1> <player2> - Teleport player1 to player2
                .then(
                    Commands.argument("player1", EntityArgument.player())
                        .then(
                            Commands.argument("player2", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer player1 = EntityArgument.getPlayer(context, "player1");
                                    ServerPlayer player2 = EntityArgument.getPlayer(context, "player2");
                                    return teleportPlayerToPlayer(context, player1, player2);
                                })
                        )
                )
                // /tp <x> <y> <z> - Teleport to coordinates
                .then(
                    Commands.argument("location", Vec3Argument.vec3())
                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            Coordinates coords = Vec3Argument.getCoordinates(context, "location");
                            Vec3 pos = coords.getPosition(context.getSource());
                            return teleportToCoordinates(context, source, pos);
                        })
                        // /tp <x> <y> <z> <yaw> <pitch> - Teleport with rotation
                        .then(
                            Commands.argument("yaw", FloatArgumentType.floatArg())
                                .then(
                                    Commands.argument("pitch", FloatArgumentType.floatArg())
                                        .executes(context -> {
                                            ServerPlayer source = context.getSource().getPlayerOrException();
                                            Coordinates coords = Vec3Argument.getCoordinates(context, "location");
                                            Vec3 pos = coords.getPosition(context.getSource());
                                            float yaw = FloatArgumentType.getFloat(context, "yaw");
                                            float pitch = FloatArgumentType.getFloat(context, "pitch");
                                            return teleportToCoordinatesWithRotation(context, source, pos, yaw, pitch);
                                        })
                                )
                        )
                )
                // /tp <player> <x> <y> <z> - Teleport player to coordinates
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .then(
                            Commands.argument("location", Vec3Argument.vec3())
                                .executes(context -> {
                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                    Coordinates coords = Vec3Argument.getCoordinates(context, "location");
                                    Vec3 pos = coords.getPosition(context.getSource());
                                    return teleportPlayerToCoordinates(context, player, pos);
                                })
                        )
                )
        );

        // /tphere <player> - Teleport player to you
        dispatcher.register(
            Commands.literal("tphere")
                .requires(source -> PermissionUtil.hasAdminPermission(source, "neoessentials.command.tphere"))
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            return teleportPlayerToSelf(context, source, target);
                        })
                )
        );

        // /tpall [world] - Teleport all players to you
        dispatcher.register(
            Commands.literal("tpall")
                .requires(source -> PermissionUtil.hasAdminPermission(source, "neoessentials.command.tpall"))
                .executes(context -> {
                    ServerPlayer source = context.getSource().getPlayerOrException();
                    return teleportAllPlayersToSelf(context, source, null);
                })
                .then(
                    Commands.argument("dimension", DimensionArgument.dimension())
                        .executes(context -> {
                            ServerPlayer source = context.getSource().getPlayerOrException();
                            ServerLevel dimension = DimensionArgument.getDimension(context, "dimension");
                            return teleportAllPlayersToSelf(context, source, dimension);
                        })
                )
        );

        // /tppos <x> <y> <z> [world] - Teleport to exact coordinates
        dispatcher.register(
            Commands.literal("tppos")
                .requires(source -> PermissionUtil.hasAdminPermission(source, "neoessentials.command.tppos"))
                .then(
                    Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(
                            Commands.argument("y", DoubleArgumentType.doubleArg())
                                .then(
                                    Commands.argument("z", DoubleArgumentType.doubleArg())
                                        .executes(context -> {
                                            ServerPlayer source = context.getSource().getPlayerOrException();
                                            double x = DoubleArgumentType.getDouble(context, "x");
                                            double y = DoubleArgumentType.getDouble(context, "y");
                                            double z = DoubleArgumentType.getDouble(context, "z");
                                            return teleportToExactCoordinates(context, source, x, y, z, null);
                                        })
                                        .then(
                                            Commands.argument("dimension", DimensionArgument.dimension())
                                                .executes(context -> {
                                                    ServerPlayer source = context.getSource().getPlayerOrException();
                                                    double x = DoubleArgumentType.getDouble(context, "x");
                                                    double y = DoubleArgumentType.getDouble(context, "y");
                                                    double z = DoubleArgumentType.getDouble(context, "z");
                                                    ServerLevel dimension = DimensionArgument.getDimension(context, "dimension");
                                                    return teleportToExactCoordinates(context, source, x, y, z, dimension);
                                                })
                                        )
                                )
                        )
                )
        );
    }

    /**
     * Teleport the command source to a specific player
     */
    private int teleportToPlayer(CommandContext<CommandSourceStack> context, ServerPlayer source, ServerPlayer target) {
        try {
            Vec3 targetPos = target.position();
            boolean success = TeleportUtil.teleport(source, target.serverLevel(), 
                targetPos.x, targetPos.y, targetPos.z, target.getYRot(), target.getXRot());
            
            if (success) {
                LanguageUtil.sendMessage(source, "neoessentials.teleport.success_to_player", target.getScoreboardName());
                return 1;
            } else {
                LanguageUtil.sendErrorMessage(source, "neoessentials.teleport.failed");
                return 0;
            }
        } catch (Exception e) {
            LanguageUtil.sendErrorMessage(source, "neoessentials.teleport.error", e.getMessage());
            return 0;
        }
    }

    /**
     * Teleport one player to another player
     */
    private int teleportPlayerToPlayer(CommandContext<CommandSourceStack> context, ServerPlayer player1, ServerPlayer player2) {
        try {
            Vec3 targetPos = player2.position();
            boolean success = TeleportUtil.teleport(player1, player2.serverLevel(), 
                targetPos.x, targetPos.y, targetPos.z, player2.getYRot(), player2.getXRot());
            
            if (success) {
                LanguageUtil.sendMessage(context.getSource(), "neoessentials.teleport.success_player_to_player", 
                    player1.getScoreboardName(), player2.getScoreboardName());
                LanguageUtil.sendMessage(player1, "neoessentials.teleport.teleported_to_player", player2.getScoreboardName());
                return 1;
            } else {
                LanguageUtil.sendErrorMessage(context.getSource(), "neoessentials.teleport.failed");
                return 0;
            }
        } catch (Exception e) {
            LanguageUtil.sendErrorMessage(context.getSource(), "neoessentials.teleport.error", e.getMessage());
            return 0;
        }
    }

    /**
     * Teleport to coordinates
     */
    private int teleportToCoordinates(CommandContext<CommandSourceStack> context, ServerPlayer source, Vec3 pos) {
        try {
            boolean success = TeleportUtil.teleport(source, source.serverLevel(), 
                pos.x, pos.y, pos.z, source.getYRot(), source.getXRot());
            
            if (success) {
                LanguageUtil.sendMessage(source, "neoessentials.teleport.success_to_coordinates", 
                    String.format("%.1f", pos.x), String.format("%.1f", pos.y), String.format("%.1f", pos.z));
                return 1;
            } else {
                LanguageUtil.sendErrorMessage(source, "neoessentials.teleport.failed");
                return 0;
            }
        } catch (Exception e) {
            LanguageUtil.sendErrorMessage(source, "neoessentials.teleport.error", e.getMessage());
            return 0;
        }
    }

    /**
     * Teleport to coordinates with specific rotation
     */
    private int teleportToCoordinatesWithRotation(CommandContext<CommandSourceStack> context, ServerPlayer source, Vec3 pos, float yaw, float pitch) {
        try {
            boolean success = TeleportUtil.teleport(source, source.serverLevel(), pos.x, pos.y, pos.z, yaw, pitch);
            
            if (success) {
                LanguageUtil.sendMessage(source, "neoessentials.teleport.success_to_coordinates_with_rotation", 
                    String.format("%.1f", pos.x), String.format("%.1f", pos.y), String.format("%.1f", pos.z), 
                    String.format("%.1f", yaw), String.format("%.1f", pitch));
                return 1;
            } else {
                LanguageUtil.sendErrorMessage(source, "neoessentials.teleport.failed");
                return 0;
            }
        } catch (Exception e) {
            LanguageUtil.sendErrorMessage(source, "neoessentials.teleport.error", e.getMessage());
            return 0;
        }
    }

    /**
     * Teleport a player to coordinates
     */
    private int teleportPlayerToCoordinates(CommandContext<CommandSourceStack> context, ServerPlayer player, Vec3 pos) {
        try {
            boolean success = TeleportUtil.teleport(player, player.serverLevel(), 
                pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
            
            if (success) {
                LanguageUtil.sendMessage(context.getSource(), "neoessentials.teleport.success_player_to_coordinates", 
                    player.getScoreboardName(), 
                    String.format("%.1f", pos.x), String.format("%.1f", pos.y), String.format("%.1f", pos.z));
                LanguageUtil.sendMessage(player, "neoessentials.teleport.teleported_to_coordinates", 
                    String.format("%.1f", pos.x), String.format("%.1f", pos.y), String.format("%.1f", pos.z));
                return 1;
            } else {
                LanguageUtil.sendErrorMessage(context.getSource(), "neoessentials.teleport.failed");
                return 0;
            }
        } catch (Exception e) {
            LanguageUtil.sendErrorMessage(context.getSource(), "neoessentials.teleport.error", e.getMessage());
            return 0;
        }
    }

    /**
     * Teleport a player to yourself
     */
    private int teleportPlayerToSelf(CommandContext<CommandSourceStack> context, ServerPlayer source, ServerPlayer target) {
        try {
            Vec3 sourcePos = source.position();
            boolean success = TeleportUtil.teleport(target, source.serverLevel(), 
                sourcePos.x, sourcePos.y, sourcePos.z, source.getYRot(), source.getXRot());
            
            if (success) {
                LanguageUtil.sendMessage(source, "neoessentials.teleport.success_player_to_self", target.getScoreboardName());
                LanguageUtil.sendMessage(target, "neoessentials.teleport.teleported_to_admin", source.getScoreboardName());
                return 1;
            } else {
                LanguageUtil.sendErrorMessage(source, "neoessentials.teleport.failed");
                return 0;
            }
        } catch (Exception e) {
            LanguageUtil.sendErrorMessage(source, "neoessentials.teleport.error", e.getMessage());
            return 0;
        }
    }

    /**
     * Teleport all players to yourself
     */
    private int teleportAllPlayersToSelf(CommandContext<CommandSourceStack> context, ServerPlayer source, ServerLevel filterDimension) {
        try {
            Vec3 sourcePos = source.position();
            Collection<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
            int count = 0;

            for (ServerPlayer player : players) {
                // Skip the source player
                if (player.equals(source)) {
                    continue;
                }

                // Filter by dimension if specified
                if (filterDimension != null && !player.level().equals(filterDimension)) {
                    continue;
                }

                boolean success = TeleportUtil.teleport(player, source.serverLevel(), 
                    sourcePos.x, sourcePos.y, sourcePos.z, source.getYRot(), source.getXRot());
                
                if (success) {
                    LanguageUtil.sendMessage(player, "neoessentials.teleport.teleported_to_admin", source.getScoreboardName());
                    count++;
                }
            }

            if (count > 0) {
                String dimensionName = filterDimension != null ? filterDimension.dimension().location().toString() : "all dimensions";
                LanguageUtil.sendMessage(source, "neoessentials.teleport.success_all_players", String.valueOf(count), dimensionName);
                return count;
            } else {
                LanguageUtil.sendErrorMessage(source, "neoessentials.teleport.no_players_found");
                return 0;
            }
        } catch (Exception e) {
            LanguageUtil.sendErrorMessage(source, "neoessentials.teleport.error", e.getMessage());
            return 0;
        }
    }

    /**
     * Teleport to exact coordinates with optional dimension
     */
    private int teleportToExactCoordinates(CommandContext<CommandSourceStack> context, ServerPlayer source, 
                                         double x, double y, double z, ServerLevel dimension) {
        try {
            ServerLevel targetLevel = dimension != null ? dimension : source.serverLevel();
            boolean success = TeleportUtil.teleport(source, targetLevel, x, y, z, source.getYRot(), source.getXRot());
            
            if (success) {
                String dimensionName = dimension != null ? dimension.dimension().location().toString() : "current dimension";
                LanguageUtil.sendMessage(source, "neoessentials.teleport.success_to_exact_coordinates", 
                    String.format("%.1f", x), String.format("%.1f", y), String.format("%.1f", z), dimensionName);
                return 1;
            } else {
                LanguageUtil.sendErrorMessage(source, "neoessentials.teleport.failed");
                return 0;
            }
        } catch (Exception e) {
            LanguageUtil.sendErrorMessage(source, "neoessentials.teleport.error", e.getMessage());
            return 0;
        }
    }
}
