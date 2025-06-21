package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.WarpManager;
import com.zerog.neoessentials.utils.MessageUtil;
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
        // /warp <name> - Teleport to a warp
        dispatcher.register(
            Commands.literal("warp")
                .requires(source -> source.hasPermission(2)) // Requires permission level 2 (op)
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(this::executeWarp)
                )
                .executes(this::executeWarpList)
        );
        
        // /warps - List all available warps
        dispatcher.register(
            Commands.literal("warps")
                .requires(source -> source.hasPermission(0)) // Available to all players
                .executes(this::executeWarpList)
        );
        
        // /setwarp <name> - Set a warp at the player's location
        dispatcher.register(
            Commands.literal("setwarp")
                .requires(source -> source.hasPermission(2)) // Requires permission level 2 (op)
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(this::executeSetWarp)
                )
        );
        
        // /delwarp <name> - Delete a warp
        dispatcher.register(
            Commands.literal("delwarp")
                .requires(source -> source.hasPermission(2)) // Requires permission level 2 (op)
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(this::executeDeleteWarp)
                )
        );
        
        // /warpplayer <player> <warp> - Teleport another player to a warp
        dispatcher.register(
            Commands.literal("warpplayer")
                .requires(source -> source.hasPermission(2)) // Requires permission level 2 (op)
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("warp", StringArgumentType.word())
                        .executes(this::executeWarpPlayer)
                    )
                )
        );
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
        
        WarpManager warpManager = NeoEssentials.getInstance().getDataManager().getWarpManager();
        WarpManager.WarpLocation warpLocation = warpManager.getWarp(warpName);
        
        if (warpLocation == null) {
            context.getSource().sendFailure(Component.literal("Warp '" + warpName + "' not found"));
            return 0;
        }
        
        // Teleport the player to the warp
        boolean success = teleportPlayerToWarp(player, warpLocation);
        
        if (success) {
            MutableComponent message = Component.literal("Teleported to warp '" + warpName + "'");
            MessageUtil.sendSuccess(player, message);
            return 1;
        } else {
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
        
        WarpManager warpManager = NeoEssentials.getInstance().getDataManager().getWarpManager();
        Map<String, WarpManager.WarpLocation> warps = warpManager.getWarps();
        
        if (warps.isEmpty()) {
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
        
        WarpManager warpManager = NeoEssentials.getInstance().getDataManager().getWarpManager();
        boolean success = warpManager.setWarp(player, warpName);
        
        if (success) {
            MutableComponent message = Component.literal("Set warp '" + warpName + "' at your current location");
            MessageUtil.sendSuccess(player, message);
            return 1;
        } else {
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
        
        WarpManager warpManager = NeoEssentials.getInstance().getDataManager().getWarpManager();
        boolean success = warpManager.deleteWarp(warpName);
        
        if (success) {
            MutableComponent message = Component.literal("Deleted warp '" + warpName + "'");
            MessageUtil.sendSuccess(player, message);
            return 1;
        } else {
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
        
        WarpManager warpManager = NeoEssentials.getInstance().getDataManager().getWarpManager();
        WarpManager.WarpLocation warpLocation = warpManager.getWarp(warpName);
        
        if (warpLocation == null) {
            context.getSource().sendFailure(Component.literal("Warp '" + warpName + "' not found"));
            return 0;
        }
        
        // Teleport the target player to the warp
        boolean success = teleportPlayerToWarp(targetPlayer, warpLocation);
        
        if (success) {
            MutableComponent messageToAdmin = Component.literal("Teleported " + targetPlayer.getScoreboardName() + " to warp '" + warpName + "'");
            MessageUtil.sendSuccess(source, messageToAdmin);
            
            MutableComponent messageToTarget = Component.literal("You have been teleported to warp '" + warpName + "'");
            MessageUtil.sendInfo(targetPlayer, messageToTarget);
            return 1;
        } else {
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
        String dimensionKey = warpLocation.getDimension();
        double x = warpLocation.getX();
        double y = warpLocation.getY();
        double z = warpLocation.getZ();
        float yaw = warpLocation.getYaw();
        float pitch = warpLocation.getPitch();
        
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
            return false;
        }
        
        // Teleport the player
        return TeleportUtil.teleport(player, targetLevel, x, y, z, yaw, pitch);
    }
}
