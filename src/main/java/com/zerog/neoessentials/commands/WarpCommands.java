package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.WarpManager;
import com.zerog.neoessentials.utils.MessageUtil;
import com.zerog.neoessentials.utils.PermissionUtil;
import com.zerog.neoessentials.utils.TeleportUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * Handles all warp-related commands
 */
public class WarpCommands {
    
    /**
     * Register all warp-related commands
     * 
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        NeoEssentials.LOGGER.info("Registering warp commands");
        
        // /warp <n> - Teleport to a warp
        dispatcher.register(
            Commands.literal("warp")
                .requires(source -> {
                    boolean hasPermission = PermissionUtil.hasPermission(source, "neoessentials.command.warp");
                    NeoEssentials.LOGGER.debug("Permission check for 'neoessentials.command.warp': {}", hasPermission);
                    return hasPermission;
                })
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(this::executeWarp)
                )
                .executes(this::executeWarpList)
        );
        
        // /warps - List all available warps
        dispatcher.register(
            Commands.literal("warps")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.warp.list"))
                .executes(this::executeWarpList)
        );
        
        // /setwarp <n> - Set a warp at the player's location
        dispatcher.register(
            Commands.literal("setwarp")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.warp.set"))
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(this::executeSetWarp)
                )
        );
        
        // /delwarp <n> - Delete a warp
        dispatcher.register(
            Commands.literal("delwarp")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.warp.delete"))
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(this::executeDeleteWarp)
                )
        );
        
        // /warpplayer <player> <warp> - Teleport another player to a warp
        dispatcher.register(
            Commands.literal("warpplayer")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.warp.player"))
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("warp", StringArgumentType.word())
                        .executes(this::executeWarpPlayer)
                    )
                )
        );
        
        NeoEssentials.LOGGER.info("Warp commands registered successfully");
    }
    
    /**
     * Execute the /warp command
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeWarp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String warpName = StringArgumentType.getString(context, "name");
        
        NeoEssentials.LOGGER.debug("Player {} is attempting to teleport to warp '{}'", player.getScoreboardName(), warpName);
        
        WarpManager warpManager = NeoEssentials.getInstance().getDataManager().getWarpManager();
        if (warpManager == null) {
            NeoEssentials.LOGGER.error("WarpManager is null when executing /warp command");
            context.getSource().sendFailure(Component.literal("Warp system is not available"));
            return 0;
        }
        
        WarpManager.WarpLocation warpLocation = warpManager.getWarp(warpName);
        
        if (warpLocation == null) {
            NeoEssentials.LOGGER.debug("Warp '{}' not found for player {}", warpName, player.getScoreboardName());
            context.getSource().sendFailure(Component.literal("Warp '" + warpName + "' not found"));
            return 0;
        }
        
        NeoEssentials.LOGGER.debug("Attempting to teleport player {} to warp '{}' at dimension {}, [{}, {}, {}]",
            player.getScoreboardName(), warpName, warpLocation.getDimension(), 
            warpLocation.getX(), warpLocation.getY(), warpLocation.getZ());
            
        // Teleport the player to the warp
        boolean success = teleportPlayerToWarp(player, warpLocation);
        
        if (success) {
            NeoEssentials.LOGGER.debug("Successfully teleported player {} to warp '{}'", player.getScoreboardName(), warpName);
            MutableComponent message = Component.literal("Teleported to warp '" + warpName + "'");
            MessageUtil.sendSuccess(player, message);
            return 1;
        } else {
            NeoEssentials.LOGGER.error("Failed to teleport player {} to warp '{}'", player.getScoreboardName(), warpName);
            context.getSource().sendFailure(Component.literal("Failed to teleport to warp '" + warpName + "'"));
            return 0;
        }
    }
    
    /**
     * Execute the /warps command to list all available warps
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeWarpList(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        NeoEssentials.LOGGER.debug("Player {} is requesting the warp list", player.getScoreboardName());
        
        WarpManager warpManager = NeoEssentials.getInstance().getDataManager().getWarpManager();
        if (warpManager == null) {
            NeoEssentials.LOGGER.error("WarpManager is null when executing /warps command");
            context.getSource().sendFailure(Component.literal("Warp system is not available"));
            return 0;
        }
        
        Map<String, WarpManager.WarpLocation> warps = warpManager.getWarps();
        
        if (warps.isEmpty()) {
            NeoEssentials.LOGGER.debug("No warps found for player {}", player.getScoreboardName());
            context.getSource().sendFailure(Component.literal("No warps have been set"));
            return 0;
        }
        
        MutableComponent message = Component.literal("Available warps: ");
        
        boolean first = true;
        for (String warpName : warps.keySet()) {
            if (!first) {
                message.append(Component.literal(", "));
            }
            message.append(Component.literal(warpName));
            first = false;
        }
        
        NeoEssentials.LOGGER.debug("Sending warp list ({} warps) to player {}", warps.size(), player.getScoreboardName());
        MessageUtil.sendInfo(player, message);
        return 1;
    }
    
    /**
     * Execute the /setwarp command
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeSetWarp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String warpName = StringArgumentType.getString(context, "name");
        
        NeoEssentials.LOGGER.debug("Player {} is attempting to set warp '{}' at [{}, {}, {}] in dimension {}",
            player.getScoreboardName(), warpName, 
            player.getX(), player.getY(), player.getZ(),
            player.level().dimension().location());
        
        WarpManager warpManager = NeoEssentials.getInstance().getDataManager().getWarpManager();
        if (warpManager == null) {
            NeoEssentials.LOGGER.error("WarpManager is null when executing /setwarp command");
            context.getSource().sendFailure(Component.literal("Warp system is not available"));
            return 0;
        }
        
        boolean success = warpManager.setWarp(player, warpName);
        
        if (success) {
            NeoEssentials.LOGGER.info("Player {} set warp '{}' at [{}, {}, {}] in dimension {}",
                player.getScoreboardName(), warpName, 
                player.getX(), player.getY(), player.getZ(),
                player.level().dimension().location());
            MutableComponent message = Component.literal("Set warp '" + warpName + "' at your current location");
            MessageUtil.sendSuccess(player, message);
            return 1;
        } else {
            NeoEssentials.LOGGER.error("Failed to set warp '{}' for player {}", warpName, player.getScoreboardName());
            context.getSource().sendFailure(Component.literal("Failed to set warp '" + warpName + "'"));
            return 0;
        }
    }
    
    /**
     * Execute the /delwarp command
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeDeleteWarp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String warpName = StringArgumentType.getString(context, "name");
        
        NeoEssentials.LOGGER.debug("Player {} is attempting to delete warp '{}'", player.getScoreboardName(), warpName);
        
        WarpManager warpManager = NeoEssentials.getInstance().getDataManager().getWarpManager();
        if (warpManager == null) {
            NeoEssentials.LOGGER.error("WarpManager is null when executing /delwarp command");
            context.getSource().sendFailure(Component.literal("Warp system is not available"));
            return 0;
        }
        
        boolean success = warpManager.deleteWarp(warpName);
        
        if (success) {
            NeoEssentials.LOGGER.info("Player {} deleted warp '{}'", player.getScoreboardName(), warpName);
            MutableComponent message = Component.literal("Deleted warp '" + warpName + "'");
            MessageUtil.sendSuccess(player, message);
            return 1;
        } else {
            NeoEssentials.LOGGER.debug("Warp '{}' not found for deletion by player {}", warpName, player.getScoreboardName());
            context.getSource().sendFailure(Component.literal("Warp '" + warpName + "' not found"));
            return 0;
        }
    }
    
    /**
     * Execute the /warpplayer command
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeWarpPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer source = context.getSource().getPlayerOrException();
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        String warpName = StringArgumentType.getString(context, "warp");
        
        NeoEssentials.LOGGER.debug("Player {} is attempting to warp player {} to warp '{}'", 
            source.getScoreboardName(), targetPlayer.getScoreboardName(), warpName);
        
        WarpManager warpManager = NeoEssentials.getInstance().getDataManager().getWarpManager();
        if (warpManager == null) {
            NeoEssentials.LOGGER.error("WarpManager is null when executing /warpplayer command");
            context.getSource().sendFailure(Component.literal("Warp system is not available"));
            return 0;
        }
        
        WarpManager.WarpLocation warpLocation = warpManager.getWarp(warpName);
        
        if (warpLocation == null) {
            NeoEssentials.LOGGER.debug("Warp '{}' not found for warpplayer command by {}", warpName, source.getScoreboardName());
            context.getSource().sendFailure(Component.literal("Warp '" + warpName + "' not found"));
            return 0;
        }
        
        NeoEssentials.LOGGER.debug("Attempting to teleport player {} to warp '{}' at dimension {}, [{}, {}, {}]",
            targetPlayer.getScoreboardName(), warpName, warpLocation.getDimension(), 
            warpLocation.getX(), warpLocation.getY(), warpLocation.getZ());
        
        // Teleport the target player to the warp
        boolean success = teleportPlayerToWarp(targetPlayer, warpLocation);
        
        if (success) {
            NeoEssentials.LOGGER.info("Player {} teleported {} to warp '{}'", 
                source.getScoreboardName(), targetPlayer.getScoreboardName(), warpName);
            MutableComponent messageToAdmin = Component.literal("Teleported " + targetPlayer.getScoreboardName() + " to warp '" + warpName + "'");
            MessageUtil.sendSuccess(source, messageToAdmin);
            
            MutableComponent messageToTarget = Component.literal("You have been teleported to warp '" + warpName + "'");
            MessageUtil.sendInfo(targetPlayer, messageToTarget);
            return 1;
        } else {
            NeoEssentials.LOGGER.error("Failed to teleport player {} to warp '{}'", targetPlayer.getScoreboardName(), warpName);
            context.getSource().sendFailure(Component.literal("Failed to teleport " + targetPlayer.getScoreboardName() + " to warp '" + warpName + "'"));
            return 0;
        }
    }
    
    /**
     * Teleports a player to a warp location
     * 
     * @param player The player to teleport
     * @param warpLocation The warp location
     * @return True if teleportation was successful, false otherwise
     */
    private boolean teleportPlayerToWarp(ServerPlayer player, WarpManager.WarpLocation warpLocation) {
        if (player == null || warpLocation == null) {
            NeoEssentials.LOGGER.error("Cannot teleport with null player or warp location");
            return false;
        }
        
        String dimensionKey = warpLocation.getDimension();
        double x = warpLocation.getX();
        double y = warpLocation.getY();
        double z = warpLocation.getZ();
        float yaw = warpLocation.getYaw();
        float pitch = warpLocation.getPitch();
        
        // Check if player's server is available
        if (player.getServer() == null) {
            NeoEssentials.LOGGER.error("Cannot teleport player, server instance is null");
            return false;
        }
        
        // Get the server from the player
        ServerLevel targetLevel = null;
        for (ServerLevel level : player.getServer().getAllLevels()) {
            if (level.dimension().location().toString().equals(dimensionKey)) {
                targetLevel = level;
                break;
            }
        }
        
        if (targetLevel == null) {
            NeoEssentials.LOGGER.error("Could not find dimension for warp: {}", dimensionKey);
            
            // Try again with a simpler matching approach
            for (ServerLevel level : player.getServer().getAllLevels()) {
                String simpleName = level.dimension().location().getPath();
                if (dimensionKey.contains(simpleName)) {
                    NeoEssentials.LOGGER.info("Found dimension {} for warp using simplified matching", level.dimension().location());
                    targetLevel = level;
                    break;
                }
            }
            
            if (targetLevel == null) {
                // Try an even simpler matching approach as a last resort
                for (ServerLevel level : player.getServer().getAllLevels()) {
                    String dimPath = level.dimension().location().getPath();
                    String dimKey = dimensionKey.toLowerCase();
                    
                    // Check if dimension key contains basic names like "overworld", "nether", "end"
                    if ((dimPath.contains("overworld") && dimKey.contains("overworld")) ||
                        (dimPath.contains("nether") && dimKey.contains("nether")) ||
                        (dimPath.contains("end") && dimKey.contains("end"))) {
                        NeoEssentials.LOGGER.info("Found dimension {} for warp using basic name matching", level.dimension().location());
                        targetLevel = level;
                        break;
                    }
                }
                
                if (targetLevel == null) {
                    NeoEssentials.LOGGER.error("All dimension matching attempts failed for: {}", dimensionKey);
                    return false;
                }
            }
        }
        
        // Log successful dimension resolution
        NeoEssentials.LOGGER.info("Teleporting player {} to warp at [{}, {}, {}] in dimension {}", 
            player.getScoreboardName(), x, y, z, targetLevel.dimension().location());
        
        // Teleport the player
        return TeleportUtil.teleport(player, targetLevel, x, y, z, yaw, pitch);
    }
}
