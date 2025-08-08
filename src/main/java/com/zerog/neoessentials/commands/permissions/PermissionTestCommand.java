package com.zerog.neoessentials.commands.permissions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.permissions.CustomPermissionsManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Test command for permission persistence
 */
public class PermissionTestCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("permtest")
            .requires(source -> source.hasPermission(2)) // Require OP level 2
            
            // /permtest assign <player> <group>
            .then(Commands.literal("assign")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("group", StringArgumentType.string())
                        .executes(context -> assignGroup(context)))))
            
            // /permtest check <player>
            .then(Commands.literal("check")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> checkGroup(context))))
            
            // /permtest reload
            .then(Commands.literal("reload")
                .executes(context -> reloadPermissions(context)))
        );
    }
    
    private static int assignGroup(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String groupName = StringArgumentType.getString(context, "group");
            
            CustomPermissionsManager permManager = CustomPermissionsManager.getInstance();
            permManager.setPlayerGroup(targetPlayer.getUUID(), groupName);
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§aAssigned player " + targetPlayer.getName().getString() + " to group '" + groupName + "'"), 
                true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int checkGroup(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            
            CustomPermissionsManager permManager = CustomPermissionsManager.getInstance();
            String currentGroup = permManager.getPlayerGroup(targetPlayer.getUUID());
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§bPlayer " + targetPlayer.getName().getString() + " is in group: §e" + currentGroup), 
                false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int reloadPermissions(CommandContext<CommandSourceStack> context) {
        try {
            CustomPermissionsManager permManager = CustomPermissionsManager.getInstance();
            permManager.initialize();
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§aPermissions reloaded successfully!"), 
                true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("§cError reloading permissions: " + e.getMessage()));
            return 0;
        }
    }
}
