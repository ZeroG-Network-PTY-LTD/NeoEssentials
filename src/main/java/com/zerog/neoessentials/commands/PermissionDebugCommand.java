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
import com.zerog.neoessentials.util.MessageUtil;
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
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
            
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
        
    MessageUtil.sendMessage(player, "&ePermission Test: &b{0}", permission);
    MessageUtil.sendMessage(player, "&eResult: {0}", (hasPermission ? "&aHAS PERMISSION" : "&cDOES NOT HAVE PERMISSION"));
        
        // Also test with CustomPermissionsManager directly
        boolean directTest = CustomPermissionsManager.getInstance().hasPermission(player, permission);
    MessageUtil.sendMessage(player, "&eDirect Test: {0}", (directTest ? "&aHAS PERMISSION" : "&cDOES NOT HAVE PERMISSION"));
        
        return 1;
    }
    
    private static int testPermissionPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        String permission = StringArgumentType.getString(context, "permission");
        
    boolean hasPermission = PermissionUtil.hasPermission(target, permission);
        
    MessageUtil.sendMessage(target, "&ePermission Test for {0}: &b{1}", target.getDisplayName().getString(), permission);
    MessageUtil.sendMessage(target, "&eResult: {0}", (hasPermission ? "&aHAS PERMISSION" : "&cDOES NOT HAVE PERMISSION"));
        
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
        
    MessageUtil.sendMessage(player, "&6&l=== Permissions for {0} ===", player.getDisplayName().getString());
    MessageUtil.sendMessage(player, "&eTotal Permissions: &b{0}", permissions.size());
        
        int count = 0;
        for (String perm : permissions) {
            if (count >= 20) {
        MessageUtil.sendMessage(player, "&7... and {0} more (use /permissions info for full list)", (permissions.size() - 20));
                break;
            }
        MessageUtil.sendMessage(player, "&7- &a{0}", perm);
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
        
        MessageUtil.sendMessage(player, "&6&l=== Group Info for {0} ===", player.getDisplayName().getString());
        MessageUtil.sendMessage(player, "&eGroup: &b{0}", group);
        MessageUtil.sendMessage(player, "&ePrefix: &r{0}", prefix);
        MessageUtil.sendMessage(player, "&eSuffix: &r{0}", suffix);
        MessageUtil.sendMessage(player, "&ePriority: &b{0}", priority);
        
    // Test some common permissions
    MessageUtil.sendMessage(player, "&e&l--- Permission Tests ---");
        
        String[] testPerms = {
            PermissionNodes.HOME,
            PermissionNodes.HOME_SET,
            PermissionNodes.SPAWN,
            PermissionNodes.WARP,
            PermissionNodes.WARP_SET
        };
        
        for (String perm : testPerms) {
            boolean hasIt = manager.hasPermission(player, perm);
            MessageUtil.sendMessage(player, "&7- {0}: {1}", perm, (hasIt ? "§aYES" : "§cNO"));
        }
        
        return 1;
    }
    
    private static int setPlayerGroup(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        String group = StringArgumentType.getString(context, "group");
        
        CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
        
        if (manager.getGroup(group) == null) {
            MessageUtil.sendMessage(target, "&cGroup '{0}' does not exist!", group);
            return 0;
        }
        
        manager.setPlayerGroup(target.getUUID(), group);
        
        MessageUtil.sendMessage(target, "&aSet {0}'s group to: &b{1}", target.getDisplayName().getString(), group);
        
        LOGGER.info("Admin {} set player {}'s group to {}", 
            context.getSource().getDisplayName().getString(), 
            target.getDisplayName().getString(), 
            group);
        
        return 1;
    }
}
