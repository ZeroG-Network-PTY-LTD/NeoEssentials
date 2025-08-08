package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.permissions.CustomPermissionsManager;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Debug command for testing permissions
 */
public class PermissionDebugCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionDebugCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("permdebug")
            .requires(source -> source.hasPermission(3))
            
            // Test permission for self
            .then(Commands.literal("test")
                .then(Commands.argument("permission", StringArgumentType.string())
                    .executes(PermissionDebugCommand::testPermissionSelf)))
            
            // Test permission for another player
            .then(Commands.literal("testplayer")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("permission", StringArgumentType.string())
                        .executes(PermissionDebugCommand::testPermissionPlayer))))
            
            // Show all permissions for a player
            .then(Commands.literal("showperms")
                .executes(PermissionDebugCommand::showPermissionsSelf)
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(PermissionDebugCommand::showPermissionsPlayer)))
            
            // Show player's group info
            .then(Commands.literal("groupinfo")
                .executes(PermissionDebugCommand::showGroupInfoSelf)
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(PermissionDebugCommand::showGroupInfoPlayer)))
            
            // Set player group
            .then(Commands.literal("setgroup")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("group", StringArgumentType.string())
                        .executes(PermissionDebugCommand::setPlayerGroup))))
        );
    }
    
    private static int testPermissionSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String permission = StringArgumentType.getString(context, "permission");
        
        boolean hasPermission = PermissionUtil.hasPermission(player, permission);
        
        context.getSource().sendSuccess(() -> 
            Component.literal("§ePermission Test: §b" + permission), false);
        context.getSource().sendSuccess(() -> 
            Component.literal("§eResult: " + (hasPermission ? "§aHAS PERMISSION" : "§cDOES NOT HAVE PERMISSION")), false);
        
        // Also test with CustomPermissionsManager directly
        boolean directTest = CustomPermissionsManager.getInstance().hasPermission(player, permission);
        context.getSource().sendSuccess(() -> 
            Component.literal("§eDirect Test: " + (directTest ? "§aHAS PERMISSION" : "§cDOES NOT HAVE PERMISSION")), false);
        
        return 1;
    }
    
    private static int testPermissionPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        String permission = StringArgumentType.getString(context, "permission");
        
        boolean hasPermission = PermissionUtil.hasPermission(target, permission);
        
        context.getSource().sendSuccess(() -> 
            Component.literal("§ePermission Test for " + target.getDisplayName().getString() + ": §b" + permission), false);
        context.getSource().sendSuccess(() -> 
            Component.literal("§eResult: " + (hasPermission ? "§aHAS PERMISSION" : "§cDOES NOT HAVE PERMISSION")), false);
        
        return 1;
    }
    
    private static int showPermissionsSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return showPermissionsForPlayer(context, player);
    }
    
    private static int showPermissionsPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        return showPermissionsForPlayer(context, target);
    }
    
    private static int showPermissionsForPlayer(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
        Set<String> permissions = manager.getPlayerPermissions(player.getUUID());
        
        context.getSource().sendSuccess(() -> 
            Component.literal("§6§l=== Permissions for " + player.getDisplayName().getString() + " ==="), false);
        context.getSource().sendSuccess(() -> 
            Component.literal("§eTotal Permissions: §b" + permissions.size()), false);
        
        int count = 0;
        for (String perm : permissions) {
            if (count >= 20) {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§7... and " + (permissions.size() - 20) + " more (use /permissions info for full list)"), false);
                break;
            }
            context.getSource().sendSuccess(() -> 
                Component.literal("§7- §a" + perm), false);
            count++;
        }
        
        return 1;
    }
    
    private static int showGroupInfoSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return showGroupInfoForPlayer(context, player);
    }
    
    private static int showGroupInfoPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        return showGroupInfoForPlayer(context, target);
    }
    
    private static int showGroupInfoForPlayer(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
        
        String group = manager.getPlayerGroup(player.getUUID());
        String prefix = manager.getPlayerPrefix(player.getUUID());
        String suffix = manager.getPlayerSuffix(player.getUUID());
        int priority = manager.getPlayerPriority(player.getUUID());
        
        context.getSource().sendSuccess(() -> 
            Component.literal("§6§l=== Group Info for " + player.getDisplayName().getString() + " ==="), false);
        context.getSource().sendSuccess(() -> 
            Component.literal("§eGroup: §b" + group), false);
        context.getSource().sendSuccess(() -> 
            Component.literal("§ePrefix: §r" + prefix), false);
        context.getSource().sendSuccess(() -> 
            Component.literal("§eSuffix: §r" + suffix), false);
        context.getSource().sendSuccess(() -> 
            Component.literal("§ePriority: §b" + priority), false);
        
        // Test some common permissions
        context.getSource().sendSuccess(() -> 
            Component.literal("§e§l--- Permission Tests ---"), false);
        
        String[] testPerms = {
            PermissionNodes.HOME,
            PermissionNodes.HOME_SET,
            PermissionNodes.SPAWN,
            PermissionNodes.WARP,
            PermissionNodes.WARP_SET
        };
        
        for (String perm : testPerms) {
            boolean hasIt = manager.hasPermission(player, perm);
            context.getSource().sendSuccess(() -> 
                Component.literal("§7- " + perm + ": " + (hasIt ? "§aYES" : "§cNO")), false);
        }
        
        return 1;
    }
    
    private static int setPlayerGroup(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        String group = StringArgumentType.getString(context, "group");
        
        CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
        
        if (manager.getGroup(group) == null) {
            context.getSource().sendFailure(Component.literal("§cGroup '" + group + "' does not exist!"));
            return 0;
        }
        
        manager.setPlayerGroup(target.getUUID(), group);
        
        context.getSource().sendSuccess(() -> 
            Component.literal("§aSet " + target.getDisplayName().getString() + "'s group to: §b" + group), false);
        
        LOGGER.info("Admin {} set player {}'s group to {}", 
            context.getSource().getDisplayName().getString(), 
            target.getDisplayName().getString(), 
            group);
        
        return 1;
    }
}
