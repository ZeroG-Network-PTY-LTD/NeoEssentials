package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.managers.WarpManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Warp command implementation
 * Handles /warp, /setwarp, /delwarp commands
 */
public class WarpCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /warp <name> - Teleport to warp
        dispatcher.register(Commands.literal("warp")
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> teleportWarp(context, StringArgumentType.getString(context, "name")))
            )
        );
        
        // /setwarp <name> [category] - Set a warp
        dispatcher.register(Commands.literal("setwarp")
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> setWarp(context, StringArgumentType.getString(context, "name"), null))
                .then(Commands.argument("category", StringArgumentType.word())
                    .executes(context -> setWarp(context, 
                        StringArgumentType.getString(context, "name"),
                        StringArgumentType.getString(context, "category")))
                )
            )
        );
        
        // /delwarp <name> - Delete a warp
        dispatcher.register(Commands.literal("delwarp")
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> deleteWarp(context, StringArgumentType.getString(context, "name")))
            )
        );
        
        // /warps [category] - List all warps
        dispatcher.register(Commands.literal("warps")
            .executes(context -> listWarps(context, null))
            .then(Commands.argument("category", StringArgumentType.word())
                .executes(context -> listWarps(context, StringArgumentType.getString(context, "category")))
            )
        );
    }
    
    private static int teleportWarp(CommandContext<CommandSourceStack> context, String warpName) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        WarpManager warpManager = WarpManager.getInstance();
        
        boolean success = warpManager.teleportToWarp(player, warpName);
        return success ? 1 : 0;
    }
    
    private static int setWarp(CommandContext<CommandSourceStack> context, String warpName, String category) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        WarpManager warpManager = WarpManager.getInstance();
        
        boolean success = warpManager.createWarp(player, warpName, category);
        return success ? 1 : 0;
    }
    
    private static int deleteWarp(CommandContext<CommandSourceStack> context, String warpName) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        WarpManager warpManager = WarpManager.getInstance();
        
        boolean success = warpManager.deleteWarp(player, warpName);
        return success ? 1 : 0;
    }
    
    private static int listWarps(CommandContext<CommandSourceStack> context, String category) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        WarpManager warpManager = WarpManager.getInstance();
        
        boolean success = warpManager.listWarps(player, category);
        return success ? 1 : 0;
    }
}
