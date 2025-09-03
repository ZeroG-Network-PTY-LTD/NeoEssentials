package com.zerog.neoessentials.commands.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.features.TabListManager;
import com.zerog.neoessentials.permissions.CustomPermissionsManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Debug command for testing tablist functionality
 */
public class TablistTestCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tablisttest")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("assign")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("permission", StringArgumentType.string())
                        .executes(TablistTestCommand::assignPermission))))
            .then(Commands.literal("remove")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("permission", StringArgumentType.string())
                        .executes(TablistTestCommand::removePermission))))
            .then(Commands.literal("check")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("permission", StringArgumentType.string())
                        .executes(TablistTestCommand::checkPermission))))
            .then(Commands.literal("reload")
                .executes(TablistTestCommand::reloadTablist))
            .then(Commands.literal("permissions")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(TablistTestCommand::listPermissions)))
            .then(Commands.literal("group")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("groupName", StringArgumentType.string())
                        .executes(TablistTestCommand::setGroup))))
            .then(Commands.literal("groups")
                .executes(TablistTestCommand::listGroups))
        );
    }
    
    private static int assignPermission(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String permission = StringArgumentType.getString(context, "permission");
            
            CustomPermissionsManager.getInstance().addPlayerPermission(targetPlayer.getUUID(), permission);
            TabListManager.getInstance().updatePlayer(targetPlayer);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§aAssigned permission '" + permission + "' to " + targetPlayer.getName().getString()), false);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int removePermission(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String permission = StringArgumentType.getString(context, "permission");
            
            CustomPermissionsManager.getInstance().removePlayerPermission(targetPlayer.getUUID(), permission);
            TabListManager.getInstance().updatePlayer(targetPlayer);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§cRemoved permission '" + permission + "' from " + targetPlayer.getName().getString()), false);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int checkPermission(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String permission = StringArgumentType.getString(context, "permission");
            
            boolean hasPermission = CustomPermissionsManager.getInstance().hasPermission(targetPlayer, permission);
            String status = hasPermission ? "§aHAS" : "§cDOES NOT HAVE";
            
            context.getSource().sendSuccess(() -> Component.literal(
                targetPlayer.getName().getString() + " " + status + " permission '" + permission + "'"), false);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int reloadTablist(CommandContext<CommandSourceStack> context) {
        try {
            TabListManager.getInstance().reloadConfig();
            
            context.getSource().sendSuccess(() -> Component.literal("§aTablist configuration reloaded!"), false);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int listPermissions(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            
            var permissions = CustomPermissionsManager.getInstance().getPlayerPermissions(targetPlayer.getUUID());
            String group = CustomPermissionsManager.getInstance().getPlayerGroup(targetPlayer.getUUID());
            int groupPriority = CustomPermissionsManager.getInstance().getPlayerPriority(targetPlayer.getUUID());
            String prefix = CustomPermissionsManager.getInstance().getPlayerPrefix(targetPlayer.getUUID());
            String suffix = CustomPermissionsManager.getInstance().getPlayerSuffix(targetPlayer.getUUID());
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§6=== " + targetPlayer.getName().getString() + " ===\n" +
                "§bGroup: §f" + group + " §7(priority: " + groupPriority + ")\n" +
                "§bPrefix: §f'" + prefix + "'\n" +
                "§bSuffix: §f'" + suffix + "'\n" +
                "§bPermissions: §f" + permissions.size() + " total\n" +
                "§7" + String.join(", ", permissions)), false);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int setGroup(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String groupName = StringArgumentType.getString(context, "groupName");
            
            CustomPermissionsManager.getInstance().setPlayerGroup(targetPlayer.getUUID(), groupName);
            TabListManager.getInstance().updatePlayer(targetPlayer);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§aSet " + targetPlayer.getName().getString() + " to group '" + groupName + "'"), false);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int listGroups(CommandContext<CommandSourceStack> context) {
        try {
            var groups = CustomPermissionsManager.getInstance().getAllGroups();
            
            StringBuilder groupList = new StringBuilder("§6=== Available Groups ===\n");
            for (var entry : groups.entrySet()) {
                String groupName = entry.getKey();
                var group = entry.getValue();
                groupList.append("§b").append(groupName)
                    .append(" §7(priority: ").append(group.getPriority())
                    .append(", prefix: '").append(group.getPrefix())
                    .append("', suffix: '").append(group.getSuffix())
                    .append("')\n");
            }
            
            context.getSource().sendSuccess(() -> Component.literal(groupList.toString()), false);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }
}
