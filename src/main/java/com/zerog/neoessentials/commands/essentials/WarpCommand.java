package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.managers.WarpManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Warp command implementation for NeoEssentials
 * Allows players to teleport to server warps
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class WarpCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /warp <name> - Teleport to warp
        dispatcher.register(Commands.literal("warp")
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(WarpCommand::teleportToWarp)
            )
        );
        
        // /setwarp <name> - Create a warp
        dispatcher.register(Commands.literal("setwarp")
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(WarpCommand::createWarp)
                .then(Commands.argument("category", StringArgumentType.word())
                    .executes(WarpCommand::createWarpWithCategory)
                )
            )
        );
        
        // /delwarp <name> - Delete a warp
        dispatcher.register(Commands.literal("delwarp")
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(WarpCommand::deleteWarp)
            )
        );
        
        // /warps [category] - List warps
        dispatcher.register(Commands.literal("warps")
            .executes(WarpCommand::listAllWarps)
            .then(Commands.argument("category", StringArgumentType.word())
                .executes(WarpCommand::listCategoryWarps)
            )
        );
    }
    
    private static int teleportToWarp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String warpName = StringArgumentType.getString(context, "name");
        
        WarpManager warpManager = WarpManager.getInstance();
        warpManager.teleportToWarp(player, warpName);
        
        return 1;
    }
    
    private static int createWarp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String warpName = StringArgumentType.getString(context, "name");
        
        WarpManager warpManager = WarpManager.getInstance();
        warpManager.createWarp(player, warpName, "general");
        
        return 1;
    }
    
    private static int createWarpWithCategory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String warpName = StringArgumentType.getString(context, "name");
        String category = StringArgumentType.getString(context, "category");
        
        WarpManager warpManager = WarpManager.getInstance();
        warpManager.createWarp(player, warpName, category);
        
        return 1;
    }
    
    private static int deleteWarp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String warpName = StringArgumentType.getString(context, "name");
        
        WarpManager warpManager = WarpManager.getInstance();
        warpManager.deleteWarp(player, warpName);
        
        return 1;
    }
    
    private static int listAllWarps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        WarpManager warpManager = WarpManager.getInstance();
        warpManager.listWarps(player, null);
        
        return 1;
    }
    
    private static int listCategoryWarps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String category = StringArgumentType.getString(context, "category");
        
        WarpManager warpManager = WarpManager.getInstance();
        warpManager.listWarps(player, category);
        
        return 1;
    }
}
