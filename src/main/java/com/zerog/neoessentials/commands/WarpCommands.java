package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.managers.WarpManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Warp command implementation with proper permission checking
 * Handles /warp, /setwarp, /delwarp commands
 */
public class WarpCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /warp <name> - Teleport to warp
        dispatcher.register(Commands.literal("warp")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.WARP))
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> teleportWarp(context, StringArgumentType.getString(context, "name")))
            )
        );
        
        // /setwarp <name> [category] - Set a warp (admin only)
        dispatcher.register(Commands.literal("setwarp")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.WARP_SET))
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> setWarp(context, StringArgumentType.getString(context, "name"), null))
                .then(Commands.argument("category", StringArgumentType.word())
                    .executes(context -> setWarp(context, 
                        StringArgumentType.getString(context, "name"),
                        StringArgumentType.getString(context, "category")))
                )
            )
        );
        
        // /delwarp <name> - Delete a warp (admin only)
        dispatcher.register(Commands.literal("delwarp")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.WARP_DELETE))
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(context -> deleteWarp(context, StringArgumentType.getString(context, "name")))
            )
        );
        
        // /warps [category] - List all warps
        dispatcher.register(Commands.literal("warps")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.WARP_LIST))
            .executes(context -> listWarps(context, null))
            .then(Commands.argument("category", StringArgumentType.word())
                .executes(context -> listWarps(context, StringArgumentType.getString(context, "category")))
            )
        );
    }
    
    private static int teleportWarp(CommandContext<CommandSourceStack> context, String warpName) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Double-check permission (defense in depth)
        if (!PermissionUtil.hasPermission(player, PermissionNodes.WARP)) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.error.no_permission_use"));
            return 0;
        }
        
        WarpManager warpManager = WarpManager.getInstance();
        boolean success = warpManager.teleportToWarp(player, warpName);
        return success ? 1 : 0;
    }
    
    private static int setWarp(CommandContext<CommandSourceStack> context, String warpName, String category) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Double-check permission (defense in depth)
        if (!PermissionUtil.hasPermission(player, PermissionNodes.WARP_SET)) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.error.no_permission_create"));
            return 0;
        }
        
        WarpManager warpManager = WarpManager.getInstance();
    boolean success = warpManager.createWarp(player, warpName, category);
        return success ? 1 : 0;
    }
    
    private static int deleteWarp(CommandContext<CommandSourceStack> context, String warpName) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Double-check permission (defense in depth)
        if (!PermissionUtil.hasPermission(player, PermissionNodes.WARP_DELETE)) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.error.no_permission_delete"));
            return 0;
        }
        
        WarpManager warpManager = WarpManager.getInstance();
        boolean success = warpManager.deleteWarp(player, warpName);
        return success ? 1 : 0;
    }
    
    private static int listWarps(CommandContext<CommandSourceStack> context, String category) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Double-check permission (defense in depth)
        if (!PermissionUtil.hasPermission(player, PermissionNodes.WARP_LIST)) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "warp.error.no_permission_list"));
            return 0;
        }
        
        WarpManager warpManager = WarpManager.getInstance();
        boolean success = warpManager.listWarps(player, category);
        return success ? 1 : 0;
    }
}
