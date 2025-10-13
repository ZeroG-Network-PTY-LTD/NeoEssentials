package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;

/**
 * Implements the /depth command - Shows current depth/elevation information
 * Displays Y-coordinate, depth below sea level, height above bedrock, etc.
 */
public class DepthCommand {
    private static final int SEA_LEVEL = 63;
    private static final int BEDROCK_LEVEL = -64; // Modern Minecraft bedrock level
    
    /**
     * Register the /depth command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("depth")) return;
        
        dispatcher.register(
            Commands.literal("depth")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                // /depth [player] - Show depth info for another player
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> {
                        PermissionValidator.PermissionResult permResult = 
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.depth.others");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                        showDepthInfo(ctx.getSource(), target, permResult.getPlayer());
                        return 1;
                    })
                )
                // /depth - Show own depth info
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult = 
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.depth");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    
                    ServerPlayer player = permResult.getPlayer();
                    showDepthInfo(ctx.getSource(), player, player);
                    return 1;
                })
        );
    }
    
    /**
     * Display depth information for a player
     */
    private static void showDepthInfo(CommandSourceStack source, ServerPlayer target, ServerPlayer requester) {
        BlockPos pos = target.blockPosition();
        Level level = target.level();
        int currentY = pos.getY();
        
        // Calculate depth/height information
        int depthBelowSeaLevel = SEA_LEVEL - currentY;
        int heightAboveBedrock = currentY - BEDROCK_LEVEL;
        
        // Determine layer description
        String layerDescription = getLayerDescription(currentY, level);
        
        // Send header
        if (target == requester) {
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.depth.header_self"), false);
        } else {
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.depth.header_other", target.getName().getString()), false);
        }
        
        // Send depth information
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.depth.current_y", currentY), false);
        
        if (depthBelowSeaLevel > 0) {
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.depth.below_sea_level", depthBelowSeaLevel), false);
        } else {
            int heightAboveSeaLevel = Math.abs(depthBelowSeaLevel);
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.depth.above_sea_level", heightAboveSeaLevel), false);
        }
        
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.depth.above_bedrock", heightAboveBedrock), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.depth.layer_info", layerDescription), false);
        
        // Additional world-specific information
        if (level.dimension() == Level.OVERWORLD) {
            if (currentY < 0) {
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.depth.deepslate_region"), false);
            }
            if (currentY <= 16) {
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.depth.diamond_level"), false);
            }
            if (currentY >= 90) {
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.depth.cloud_level"), false);
            }
        } else if (level.dimension() == Level.NETHER) {
            if (currentY <= 31) {
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.depth.nether_lava_level"), false);
            }
            if (currentY >= 100) {
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.depth.nether_ceiling"), false);
            }
        }
    }
    
    /**
     * Get a description of the current layer/depth
     */
    private static String getLayerDescription(int y, Level level) {
        if (level.dimension() == Level.OVERWORLD) {
            if (y >= 200) return "Sky Limit";
            if (y >= 150) return "High Altitude";
            if (y >= 90) return "Cloud Level";
            if (y >= SEA_LEVEL) return "Surface Level";
            if (y >= 32) return "Cave Level";
            if (y >= 0) return "Deep Cave Level";
            if (y >= -32) return "Deepslate Level";
            if (y >= -48) return "Deep Deepslate";
            return "Near Bedrock";
        } else if (level.dimension() == Level.NETHER) {
            if (y >= 120) return "Above Nether Ceiling";
            if (y >= 100) return "Nether Ceiling";
            if (y >= 64) return "Upper Nether";
            if (y >= 32) return "Mid Nether";
            if (y >= 16) return "Lower Nether";
            return "Nether Floor";
        } else if (level.dimension() == Level.END) {
            if (y >= 80) return "High End";
            if (y >= 60) return "End Surface";
            if (y >= 40) return "Mid End";
            return "Lower End";
        }
        
        return "Unknown Layer";
    }
}