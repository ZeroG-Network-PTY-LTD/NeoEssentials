package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.permissions.PermissionHandler;
import com.zerog.neoessentials.permissions.PermissionHandlerManager;
import com.zerog.neoessentials.permissions.VanillaPermissionHandler;
import com.zerog.neoessentials.utils.ChatUtil;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Commands for managing the NeoEssentials permission system
 */
public class PermissionCommands {

    /**
     * Register permission commands
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("neoperm")
            .requires(source -> source.hasPermission(4)) // Only for server operators
            .then(Commands.literal("player")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.literal("set")
                        .then(Commands.argument("permission", StringArgumentType.string())
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> setPlayerPermission(ctx, 
                                EntityArgument.getPlayer(ctx, "player"),
                                StringArgumentType.getString(ctx, "permission"),
                                BoolArgumentType.getBool(ctx, "value"))))))))
            .then(Commands.literal("group")
                .then(Commands.literal("create")
                    .then(Commands.argument("group", StringArgumentType.word())
                        .executes(ctx -> createGroup(ctx, StringArgumentType.getString(ctx, "group")))))
                .then(Commands.literal("set")
                    .then(Commands.argument("group", StringArgumentType.word())
                    .then(Commands.argument("permission", StringArgumentType.string())
                    .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> setGroupPermission(ctx, 
                            StringArgumentType.getString(ctx, "group"),
                            StringArgumentType.getString(ctx, "permission"),
                            BoolArgumentType.getBool(ctx, "value"))))))))
            .then(Commands.literal("addgroup")
                .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("group", StringArgumentType.word())
                    .executes(ctx -> addPlayerToGroup(ctx, 
                        EntityArgument.getPlayer(ctx, "player"),
                        StringArgumentType.getString(ctx, "group"))))))
            .then(Commands.literal("removegroup")
                .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("group", StringArgumentType.word())
                    .executes(ctx -> removePlayerFromGroup(ctx, 
                        EntityArgument.getPlayer(ctx, "player"),
                        StringArgumentType.getString(ctx, "group"))))))
            .then(Commands.literal("reload")
                .executes(PermissionCommands::reloadPermissions))
            .then(Commands.literal("check")
                .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("permission", StringArgumentType.string())
                    .executes(ctx -> checkPermission(ctx, 
                        EntityArgument.getPlayer(ctx, "player"),
                        StringArgumentType.getString(ctx, "permission"))))))
        );
    }
    
    /**
     * Set a permission for a player
     */
    private static int setPlayerPermission(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String permission, boolean value) {
        VanillaPermissionHandler handler = getVanillaHandler();
        if (handler == null) {
            ctx.getSource().sendFailure(Component.literal("NeoEssentials permission system is not active. Another permission mod is in use."));
            return 0;
        }
        
        handler.setPlayerPermission(player.getUUID(), permission, value);
        
        // Success message
        ChatUtil.sendSuccess(ctx.getSource(), String.format(
                "Set permission %s = %s for player %s", 
                permission, value, player.getName().getString()));
        
        return 1;
    }
    
    /**
     * Create a permission group
     */
    private static int createGroup(CommandContext<CommandSourceStack> ctx, String group) {
        VanillaPermissionHandler handler = getVanillaHandler();
        if (handler == null) {
            ctx.getSource().sendFailure(Component.literal("NeoEssentials permission system is not active. Another permission mod is in use."));
            return 0;
        }
        
        boolean success = handler.createGroup(group);
        
        if (success) {
            ChatUtil.sendSuccess(ctx.getSource(), "Created permission group: " + group);
        } else {
            ctx.getSource().sendFailure(Component.literal("Group already exists: " + group));
            return 0;
        }
        
        return 1;
    }
    
    /**
     * Set a permission for a group
     */
    private static int setGroupPermission(CommandContext<CommandSourceStack> ctx, String group, String permission, boolean value) {
        VanillaPermissionHandler handler = getVanillaHandler();
        if (handler == null) {
            ctx.getSource().sendFailure(Component.literal("NeoEssentials permission system is not active. Another permission mod is in use."));
            return 0;
        }
        
        boolean success = handler.setGroupPermission(group, permission, value);
        
        if (success) {
            ChatUtil.sendSuccess(ctx.getSource(), String.format(
                    "Set permission %s = %s for group %s", 
                    permission, value, group));
        } else {
            ctx.getSource().sendFailure(Component.literal("Group does not exist: " + group));
            return 0;
        }
        
        return 1;
    }
    
    /**
     * Add a player to a permission group
     */
    private static int addPlayerToGroup(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String group) {
        VanillaPermissionHandler handler = getVanillaHandler();
        if (handler == null) {
            ctx.getSource().sendFailure(Component.literal("NeoEssentials permission system is not active. Another permission mod is in use."));
            return 0;
        }
        
        boolean success = handler.addPlayerToGroup(player.getUUID(), group);
        
        if (success) {
            ChatUtil.sendSuccess(ctx.getSource(), String.format(
                    "Added player %s to group %s", 
                    player.getName().getString(), group));
        } else {
            ctx.getSource().sendFailure(Component.literal("Group does not exist: " + group));
            return 0;
        }
        
        return 1;
    }
    
    /**
     * Remove a player from a permission group
     */
    private static int removePlayerFromGroup(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String group) {
        VanillaPermissionHandler handler = getVanillaHandler();
        if (handler == null) {
            ctx.getSource().sendFailure(Component.literal("NeoEssentials permission system is not active. Another permission mod is in use."));
            return 0;
        }
        
        boolean success = handler.removePlayerFromGroup(player.getUUID(), group);
        
        if (success) {
            ChatUtil.sendSuccess(ctx.getSource(), String.format(
                    "Removed player %s from group %s", 
                    player.getName().getString(), group));
        } else {
            ctx.getSource().sendFailure(Component.literal("Cannot remove player from default group or group does not exist: " + group));
            return 0;
        }
        
        return 1;
    }
    
    /**
     * Reload permissions
     */
    private static int reloadPermissions(CommandContext<CommandSourceStack> ctx) {
        // Reinitialize permission handler manager
        PermissionHandlerManager.getInstance();
        
        ChatUtil.sendSuccess(ctx.getSource(), "Reloaded all permissions");
        return 1;
    }
    
    /**
     * Check if a player has a permission
     */
    private static int checkPermission(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String permission) {
        boolean hasPermission = PermissionHandlerManager.getInstance().hasPermission(player, permission);
          if (hasPermission) {
            ChatUtil.sendSuccess(ctx.getSource(), String.format(
                    "Player %s has permission: %s", 
                    player.getName().getString(), permission));
        } else {
            ChatUtil.sendError(ctx.getSource(), String.format(
                    "Player %s does not have permission: %s", 
                    player.getName().getString(), permission));
        }
        
        return 1;
    }
    
    /**
     * Get the VanillaPermissionHandler if it's being used
     */
    private static VanillaPermissionHandler getVanillaHandler() {
        for (PermissionHandler handler : PermissionHandlerManager.getInstance().getAvailableHandlers()) {
            if (handler instanceof VanillaPermissionHandler) {
                return (VanillaPermissionHandler) handler;
            }
        }
        return null;
    }
}
