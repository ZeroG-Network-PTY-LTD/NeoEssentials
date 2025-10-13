package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;

/**
 * Implements the /getpos command - Shows detailed position information
 * Displays coordinates, world info, biome, light level, and more
 */
public class GetPosCommand {
    
    /**
     * Register the /getpos command with aliases
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("getpos")) return;
        
        // Register main command and aliases
        registerGetPosCommand(dispatcher, "getpos");
        registerGetPosCommand(dispatcher, "coords");
        registerGetPosCommand(dispatcher, "pos");
        registerGetPosCommand(dispatcher, "whereami");
    }
    
    private static void registerGetPosCommand(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(
            Commands.literal(commandName)
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                // /getpos [player] - Show position info for self or another player
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> {
                        PermissionValidator.PermissionResult permResult = 
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.getpos.others");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                        showPositionInfo(ctx.getSource(), target, permResult.getPlayer());
                        return 1;
                    })
                )
                // /getpos - Show own position info
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult = 
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.getpos");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    
                    ServerPlayer player = permResult.getPlayer();
                    showPositionInfo(ctx.getSource(), player, player);
                    return 1;
                })
        );
    }
    
    /**
     * Display detailed position information for a player
     */
    private static void showPositionInfo(CommandSourceStack source, ServerPlayer target, ServerPlayer requester) {
        BlockPos pos = target.blockPosition();
        Level level = target.level();
        
        // Get basic position info
        double x = target.getX();
        double y = target.getY();
        double z = target.getZ();
        
        // Get world information
        String worldName = getWorldName(level);
        String dimensionName = getDimensionName(level);
        
        // Get biome information
        Biome biome = level.getBiome(pos).value();
        String biomeName = getBiomeName(biome, level, pos);
        
        // Get light levels
        int blockLight = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos);
        int skyLight = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos);
        int totalLight = Math.max(blockLight, skyLight);
        
        // Get block at feet
        String blockAtFeet = level.getBlockState(pos.below()).getBlock().getName().getString();
        
        // Get time information
        long worldTime = level.getDayTime();
        long gameTime = level.getGameTime();
        
        // Format coordinates
        String exactCoords = String.format("%.3f, %.3f, %.3f", x, y, z);
        String blockCoords = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        
        // Send header
        if (target == requester) {
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.getpos.header_self"), false);
        } else {
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.getpos.header_other", target.getName().getString()), false);
        }
        
        // Send position information
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.getpos.exact_coords", exactCoords), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.getpos.block_coords", blockCoords), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.getpos.world", worldName, dimensionName), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.getpos.biome", biomeName), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.getpos.light_levels", totalLight, blockLight, skyLight), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.getpos.block_at_feet", blockAtFeet), false);
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.getpos.world_time", worldTime, gameTime), false);
        
        // Additional info for different dimensions
        if (level.dimension() == Level.NETHER) {
            // Show overworld coordinates
            int overworldX = pos.getX() * 8;
            int overworldZ = pos.getZ() * 8;
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.getpos.overworld_equiv", overworldX, overworldZ), false);
        } else if (level.dimension() == Level.OVERWORLD) {
            // Show nether coordinates
            int netherX = pos.getX() / 8;
            int netherZ = pos.getZ() / 8;
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.getpos.nether_equiv", netherX, netherZ), false);
        }
    }
    
    /**
     * Get user-friendly world name
     */
    private static String getWorldName(Level level) {
        String dimensionKey = level.dimension().location().toString();
        return switch (dimensionKey) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "The Nether";
            case "minecraft:the_end" -> "The End";
            default -> dimensionKey;
        };
    }
    
    /**
     * Get dimension name from level
     */
    private static String getDimensionName(Level level) {
        return level.dimension().location().toString();
    }
    
    /**
     * Get biome name from biome
     */
    private static String getBiomeName(Biome biome, Level level, BlockPos pos) {
        ResourceLocation biomeKey = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getKey(biome);
        if (biomeKey != null) {
            return biomeKey.toString();
        }
        return "Unknown";
    }
}