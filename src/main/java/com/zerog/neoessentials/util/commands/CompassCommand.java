package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.CommandSourceHelper;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;

/**
 * Implements the /compass command - Shows direction and navigation info
 * Displays cardinal directions, coordinates, biome, and nearby points of interest
 */
public class CompassCommand {
    
    /**
     * Register the /compass command with aliases
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("compass")) return;
        
        // Register main command and aliases
        registerCompassCommand(dispatcher, "compass");
        registerCompassCommand(dispatcher, "direction");
        registerCompassCommand(dispatcher, "bearing");
    }
    
    private static void registerCompassCommand(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(
            Commands.literal(commandName)
                // /compass [player] - Show compass info for another player (console can use)
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> {
                        PermissionValidator.PermissionResult permResult = 
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.compass.others");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                        ServerPlayer requester = CommandSourceHelper.getPlayer(ctx.getSource());
                        showCompassInfo(ctx.getSource(), target, requester);
                        return 1;
                    })
                )
                // /compass - Show own compass info (requires player)
                .executes(ctx -> {
                    ServerPlayer player = CommandSourceHelper.requirePlayer(ctx.getSource(), "commands.neoessentials.compass.player_only");
                    if (player == null) return 0;

                    PermissionValidator.PermissionResult permResult =
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.compass");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    
                    showCompassInfo(ctx.getSource(), player, player);
                    showCompassInfo(ctx.getSource(), player, player);
                    return 1;
                })
        );
    }
    
    /**
     * Display compass and navigation information for a player
     */
    private static void showCompassInfo(CommandSourceStack source, ServerPlayer target, ServerPlayer requester) {
        BlockPos pos = target.blockPosition();
        Level level = target.level();
        
        // Get facing direction
        float yaw = target.getYRot();
        String cardinalDirection = CommandUtil.getCardinalDirection(yaw);
        String exactDirection = getExactDirection(yaw);
        
        // Get coordinates
        double x = target.getX();
        double y = target.getY();
        double z = target.getZ();
        
        // Get world information
        String worldName = CommandUtil.getWorldName(level.dimension().identifier().toString());
        
        // Get biome information
        Biome biome = level.getBiome(pos).value();
        String biomeName = CommandUtil.getBiomeName(biome, level, pos);
        
        // Calculate distance to spawn
        BlockPos spawnPos = level.getSharedSpawnPos();
        double distanceToSpawn = Math.sqrt(Math.pow(pos.getX() - spawnPos.getX(), 2) + 
                                         Math.pow(pos.getZ() - spawnPos.getZ(), 2));
        
        // Send header
        if (target == requester) {
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.compass.header_self"), false);
        } else {
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.compass.header_other", target.getName().getString()), false);
        }
        
        // Send compass information
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.compass.facing", cardinalDirection, exactDirection), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.compass.coordinates", 
            String.format("%.1f", x), String.format("%.1f", y), String.format("%.1f", z)), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.compass.world", worldName), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.compass.biome", biomeName), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.compass.distance_to_spawn", 
            String.format("%.1f", distanceToSpawn)), false);
        
        // Show direction to spawn
        String directionToSpawn = CommandUtil.getDirectionFromOffset(
            spawnPos.getX() - pos.getX(), 
            spawnPos.getZ() - pos.getZ()
        );
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.compass.direction_to_spawn", directionToSpawn), false);
        
        // Dimension-specific information
        if (CommandUtil.isOverworld(level)) {
            // Show distance to world border
            double borderDistance = getDistanceToWorldBorder(level, pos);
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.compass.world_border_distance", 
                String.format("%.0f", borderDistance)), false);
            
        } else if (CommandUtil.isNether(level)) {
            // Show overworld equivalent coordinates
            int overworldX = CommandUtil.netherToOverworld(pos.getX());
            int overworldZ = CommandUtil.netherToOverworld(pos.getZ());
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.compass.overworld_equivalent", 
                overworldX, overworldZ), false);
                
        } else if (CommandUtil.isEnd(level)) {
            // Show distance to main island center (0, 0)
            double distanceToCenter = Math.sqrt(pos.getX() * pos.getX() + pos.getZ() * pos.getZ());
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.compass.distance_to_center", 
                String.format("%.1f", distanceToCenter)), false);
        }
    }
    
    // Removed getCardinalDirection - now using CommandUtil
    
    /**
     * Get exact direction with degrees
     */
    private static String getExactDirection(float yaw) {
        // Normalize yaw to 0-360 range
        yaw = ((yaw % 360) + 360) % 360;
        return String.format("%.1f°", yaw);
    }
    
    // Removed getDirectionToPoint - now using CommandUtil.getDirectionFromOffset
    
    /**
     * Get distance to world border
     */
    private static double getDistanceToWorldBorder(Level level, BlockPos pos) {
        double borderSize = level.getWorldBorder().getSize() / 2.0;
        double centerX = level.getWorldBorder().getCenterX();
        double centerZ = level.getWorldBorder().getCenterZ();
        
        double distanceX = Math.abs(pos.getX() - centerX);
        double distanceZ = Math.abs(pos.getZ() - centerZ);
        
        return Math.min(borderSize - distanceX, borderSize - distanceZ);
    }
    
    // Removed getWorldName and getBiomeName - now using CommandUtil
}