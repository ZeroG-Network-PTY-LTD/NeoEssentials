package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Teleport command implementation for NeoEssentials
 * Provides basic teleportation functionality for server operators
 * 
 * Commands:
 * - /tp <player> - Teleport to a player
 * - /tp <player1> <player2> - Teleport player1 to player2
 * - /tp <x> <y> <z> - Teleport to coordinates
 * - /tp <player> <x> <y> <z> - Teleport player to coordinates
 * - /tphere <player> - Teleport a player to you
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class TeleportCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /tp <player> - Teleport to a player
        dispatcher.register(Commands.literal("tp")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> teleportToPlayer(ctx, EntityArgument.getPlayer(ctx, "target")))
                // /tp <player1> <player2> - Teleport player1 to player2
                .then(Commands.argument("destination", EntityArgument.player())
                    .executes(ctx -> teleportPlayerToPlayer(ctx, 
                        EntityArgument.getPlayer(ctx, "target"), 
                        EntityArgument.getPlayer(ctx, "destination")))
                )
                // /tp <player> <x> <y> <z> - Teleport player to coordinates
                .then(Commands.argument("location", Vec3Argument.vec3())
                    .executes(ctx -> teleportPlayerToLocation(ctx, 
                        EntityArgument.getPlayer(ctx, "target"), 
                        Vec3Argument.getCoordinates(ctx, "location")))
                )
            )
            // /tp <x> <y> <z> - Teleport to coordinates
            .then(Commands.argument("location", Vec3Argument.vec3())
                .executes(ctx -> teleportToLocation(ctx, Vec3Argument.getCoordinates(ctx, "location")))
            )
        );
        
        // /teleport - Alias for /tp
        dispatcher.register(Commands.literal("teleport")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> teleportToPlayer(ctx, EntityArgument.getPlayer(ctx, "target")))
                .then(Commands.argument("destination", EntityArgument.player())
                    .executes(ctx -> teleportPlayerToPlayer(ctx, 
                        EntityArgument.getPlayer(ctx, "target"), 
                        EntityArgument.getPlayer(ctx, "destination")))
                )
                .then(Commands.argument("location", Vec3Argument.vec3())
                    .executes(ctx -> teleportPlayerToLocation(ctx, 
                        EntityArgument.getPlayer(ctx, "target"), 
                        Vec3Argument.getCoordinates(ctx, "location")))
                )
            )
            .then(Commands.argument("location", Vec3Argument.vec3())
                .executes(ctx -> teleportToLocation(ctx, Vec3Argument.getCoordinates(ctx, "location")))
            )
        );
        
        // /tphere <player> - Teleport a player to you
        dispatcher.register(Commands.literal("tphere")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> teleportPlayerHere(ctx, EntityArgument.getPlayer(ctx, "player")))
            )
        );
    }
    
    /**
     * Teleport executor to a target player
     */
    private static int teleportToPlayer(CommandContext<CommandSourceStack> context, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        if (player.getUUID().equals(target.getUUID())) {
            context.getSource().sendFailure(Component.literal("§cYou cannot teleport to yourself!"));
            return 0;
        }
        
        // Perform teleportation
        boolean success = performTeleport(player, target.getX(), target.getY(), target.getZ(), 
            target.getYRot(), target.getXRot(), target.serverLevel());
        
        if (success) {
            context.getSource().sendSuccess(() -> Component.literal("§aTeleported to " + target.getName().getString()), true);
            MessageUtil.sendMessage(target, "§7" + player.getName().getString() + " teleported to you");
        } else {
            context.getSource().sendFailure(Component.literal("§cTeleportation failed!"));
        }
        
        return success ? 1 : 0;
    }
    
    /**
     * Teleport one player to another player
     */
    private static int teleportPlayerToPlayer(CommandContext<CommandSourceStack> context, ServerPlayer player, ServerPlayer target) throws CommandSyntaxException {
        if (player.getUUID().equals(target.getUUID())) {
            context.getSource().sendFailure(Component.literal("§cCannot teleport a player to themselves!"));
            return 0;
        }
        
        // Perform teleportation
        boolean success = performTeleport(player, target.getX(), target.getY(), target.getZ(), 
            target.getYRot(), target.getXRot(), target.serverLevel());
        
        if (success) {
            context.getSource().sendSuccess(() -> Component.literal("§aTeleported " + player.getName().getString() + " to " + target.getName().getString()), true);
            MessageUtil.sendMessage(player, "§7You were teleported to " + target.getName().getString());
            MessageUtil.sendMessage(target, "§7" + player.getName().getString() + " was teleported to you");
        } else {
            context.getSource().sendFailure(Component.literal("§cTeleportation failed!"));
        }
        
        return success ? 1 : 0;
    }
    
    /**
     * Teleport executor to coordinates
     */
    private static int teleportToLocation(CommandContext<CommandSourceStack> context, Coordinates coordinates) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Vec3 position = coordinates.getPosition(context.getSource());
        
        // Perform teleportation
        boolean success = performTeleport(player, position.x, position.y, position.z, 
            player.getYRot(), player.getXRot(), player.serverLevel());
        
        if (success) {
            context.getSource().sendSuccess(() -> Component.literal(String.format("§aTeleported to %.1f, %.1f, %.1f", 
                position.x, position.y, position.z)), true);
        } else {
            context.getSource().sendFailure(Component.literal("§cTeleportation failed!"));
        }
        
        return success ? 1 : 0;
    }
    
    /**
     * Teleport a player to coordinates
     */
    private static int teleportPlayerToLocation(CommandContext<CommandSourceStack> context, ServerPlayer player, Coordinates coordinates) throws CommandSyntaxException {
        Vec3 position = coordinates.getPosition(context.getSource());
        
        // Perform teleportation
        boolean success = performTeleport(player, position.x, position.y, position.z, 
            player.getYRot(), player.getXRot(), player.serverLevel());
        
        if (success) {
            context.getSource().sendSuccess(() -> Component.literal(String.format("§aTeleported %s to %.1f, %.1f, %.1f", 
                player.getName().getString(), position.x, position.y, position.z)), true);
            MessageUtil.sendMessage(player, String.format("§7You were teleported to %.1f, %.1f, %.1f", 
                position.x, position.y, position.z));
        } else {
            context.getSource().sendFailure(Component.literal("§cTeleportation failed!"));
        }
        
        return success ? 1 : 0;
    }
    
    /**
     * Teleport a player to the executor
     */
    private static int teleportPlayerHere(CommandContext<CommandSourceStack> context, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        if (player.getUUID().equals(target.getUUID())) {
            context.getSource().sendFailure(Component.literal("§cYou cannot teleport yourself to yourself!"));
            return 0;
        }
        
        // Perform teleportation
        boolean success = performTeleport(target, player.getX(), player.getY(), player.getZ(), 
            player.getYRot(), player.getXRot(), player.serverLevel());
        
        if (success) {
            context.getSource().sendSuccess(() -> Component.literal("§aTeleported " + target.getName().getString() + " to you"), true);
            MessageUtil.sendMessage(target, "§7You were teleported to " + player.getName().getString());
        } else {
            context.getSource().sendFailure(Component.literal("§cTeleportation failed!"));
        }
        
        return success ? 1 : 0;
    }
    
    /**
     * Perform the actual teleportation with safety checks
     */
    private static boolean performTeleport(ServerPlayer player, double x, double y, double z, float yaw, float pitch, ServerLevel level) {
        try {
            // Basic safety check - ensure Y coordinate is within world bounds
            if (y < level.getMinBuildHeight() || y > level.getMaxBuildHeight()) {
                return false;
            }
            
            // Ensure the target location is loaded
            BlockPos blockPos = BlockPos.containing(x, y, z);
            if (!level.isLoaded(blockPos)) {
                return false;
            }
            
            // Perform the teleportation
            player.teleportTo(level, x, y, z, yaw, pitch);
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
