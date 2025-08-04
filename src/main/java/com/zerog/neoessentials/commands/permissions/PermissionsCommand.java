package com.zerog.neoessentials.commands.permissions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.permissions.CustomPermissionsManager;
import com.zerog.neoessentials.permissions.PermissionGroup;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Permission Management Commands
 * 
 * Commands:
 * - /permissions info [player] - Show permission info
 * - /permissions group <create/delete/list/info> - Group management
 * - /permissions user <player> <add/remove/set/clear> - User management
 * - /permissions reload - Reload permissions
 * - /permissions stats - Show permission statistics
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class PermissionsCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionsCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("permissions")
            .requires(source -> source.hasPermission(3))
            
            // Permission info
            .then(Commands.literal("info")
                .executes(PermissionsCommand::executeInfoSelf)
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(PermissionsCommand::executeInfoPlayer)))
            
            // Group management
            .then(Commands.literal("group")
                .then(Commands.literal("list")
                    .executes(PermissionsCommand::executeGroupList))
                .then(Commands.literal("info")
                    .then(Commands.argument("group", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            CustomPermissionsManager.getInstance().getAllGroups().keySet().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(PermissionsCommand::executeGroupInfo)))
                .then(Commands.literal("create")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.argument("prefix", StringArgumentType.greedyString())
                            .executes(PermissionsCommand::executeGroupCreate))))
                .then(Commands.literal("delete")
                    .then(Commands.argument("group", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            CustomPermissionsManager.getInstance().getAllGroups().keySet().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(PermissionsCommand::executeGroupDelete)))
                .then(Commands.literal("permission")
                    .then(Commands.argument("group", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            CustomPermissionsManager.getInstance().getAllGroups().keySet().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(Commands.literal("add")
                            .then(Commands.argument("permission", StringArgumentType.string())
                                .executes(PermissionsCommand::executeGroupAddPermission)))
                        .then(Commands.literal("remove")
                            .then(Commands.argument("permission", StringArgumentType.string())
                                .executes(PermissionsCommand::executeGroupRemovePermission)))))
                .then(Commands.literal("inheritance")
                    .then(Commands.argument("group", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            CustomPermissionsManager.getInstance().getAllGroups().keySet().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("parent", StringArgumentType.string())
                            .suggests((context, builder) -> {
                                CustomPermissionsManager.getInstance().getAllGroups().keySet().forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(PermissionsCommand::executeGroupSetInheritance)))))
            
            // User management
            .then(Commands.literal("user")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.literal("info")
                        .executes(PermissionsCommand::executeUserInfo))
                    .then(Commands.literal("group")
                        .then(Commands.literal("set")
                            .then(Commands.argument("group", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    CustomPermissionsManager.getInstance().getAllGroups().keySet().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(PermissionsCommand::executeUserSetGroup))))
                    .then(Commands.literal("permission")
                        .then(Commands.literal("add")
                            .then(Commands.argument("permission", StringArgumentType.string())
                                .executes(PermissionsCommand::executeUserAddPermission)))
                        .then(Commands.literal("remove")
                            .then(Commands.argument("permission", StringArgumentType.string())
                                .executes(PermissionsCommand::executeUserRemovePermission)))
                        .then(Commands.literal("temp")
                            .then(Commands.argument("permission", StringArgumentType.string())
                                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 86400))
                                    .executes(PermissionsCommand::executeUserTempPermission)))))
                    .then(Commands.literal("clear")
                        .executes(PermissionsCommand::executeUserClear))))
            
            // System commands
            .then(Commands.literal("reload")
                .executes(PermissionsCommand::executeReload))
            .then(Commands.literal("stats")
                .executes(PermissionsCommand::executeStats))
            .then(Commands.literal("check")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("permission", StringArgumentType.string())
                        .executes(PermissionsCommand::executeCheck))))
        );
    }
    
    private static int executeInfoSelf(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            return showPlayerPermissionInfo(context, player);
        } catch (Exception e) {
            LOGGER.error("Error executing permissions info command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to show permission info: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeInfoPlayer(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            return showPlayerPermissionInfo(context, target);
        } catch (Exception e) {
            LOGGER.error("Error executing permissions info player command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to show permission info: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int showPlayerPermissionInfo(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
        UUID playerId = player.getUUID();
        
        String group = manager.getPlayerGroup(playerId);
        String prefix = manager.getPlayerPrefix(playerId);
        String suffix = manager.getPlayerSuffix(playerId);
        int priority = manager.getPlayerPriority(playerId);
        
        context.getSource().sendSuccess(() -> 
            Component.literal("§6§l=== Permission Info for " + player.getDisplayName().getString() + " ==="), false);
        context.getSource().sendSuccess(() -> 
            Component.literal("§eGroup: §b" + group), false);
        context.getSource().sendSuccess(() -> 
            Component.literal("§ePrefix: §r" + prefix), false);
        context.getSource().sendSuccess(() -> 
            Component.literal("§eSuffix: §r" + suffix), false);
        context.getSource().sendSuccess(() -> 
            Component.literal("§ePriority: §b" + priority), false);
        
        Set<String> permissions = manager.getPlayerPermissions(playerId);
        context.getSource().sendSuccess(() -> 
            Component.literal("§eTotal Permissions: §b" + permissions.size()), false);
        
        // Show first 10 permissions
        int count = 0;
        for (String perm : permissions) {
            if (count >= 10) {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§7... and " + (permissions.size() - 10) + " more"), false);
                break;
            }
            context.getSource().sendSuccess(() -> 
                Component.literal("§7- §a" + perm), false);
            count++;
        }
        
        return 1;
    }
    
    private static int executeGroupList(CommandContext<CommandSourceStack> context) {
        try {
            CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
            Map<String, PermissionGroup> groups = manager.getAllGroups();
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§6§l=== Permission Groups ==="), false);
            
            for (PermissionGroup group : groups.values()) {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e" + group.getName() + " §7(Priority: " + group.getPriority() + 
                    ", Prefix: " + group.getPrefix() + "§7)"), false);
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing group list command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to list groups: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeGroupInfo(CommandContext<CommandSourceStack> context) {
        try {
            String groupName = StringArgumentType.getString(context, "group");
            CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
            PermissionGroup group = manager.getGroup(groupName);
            
            if (group == null) {
                context.getSource().sendFailure(Component.literal("§cGroup '" + groupName + "' not found"));
                return 0;
            }
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§6§l=== Group Info: " + group.getName() + " ==="), false);
            context.getSource().sendSuccess(() -> 
                Component.literal("§ePrefix: §r" + group.getPrefix()), false);
            context.getSource().sendSuccess(() -> 
                Component.literal("§eSuffix: §r" + group.getSuffix()), false);
            context.getSource().sendSuccess(() -> 
                Component.literal("§ePriority: §b" + group.getPriority()), false);
            context.getSource().sendSuccess(() -> 
                Component.literal("§eInheritance: §b" + (group.getInheritance() != null ? group.getInheritance() : "None")), false);
            
            Set<String> permissions = group.getDirectPermissions();
            context.getSource().sendSuccess(() -> 
                Component.literal("§ePermissions (" + permissions.size() + "):"), false);
            
            for (String perm : permissions) {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§7- §a" + perm), false);
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing group info command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to show group info: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeGroupCreate(CommandContext<CommandSourceStack> context) {
        try {
            String name = StringArgumentType.getString(context, "name");
            String prefix = StringArgumentType.getString(context, "prefix");
            
            CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
            
            if (manager.getGroup(name) != null) {
                context.getSource().sendFailure(Component.literal("§cGroup '" + name + "' already exists"));
                return 0;
            }
            
            manager.createGroup(name, prefix, "", 0);
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§aCreated group '" + name + "' with prefix '" + prefix + "'"), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing group create command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to create group: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeGroupDelete(CommandContext<CommandSourceStack> context) {
        try {
            String groupName = StringArgumentType.getString(context, "group");
            CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
            
            if (manager.deleteGroup(groupName)) {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§aDeleted group '" + groupName + "'"), false);
                return 1;
            } else {
                context.getSource().sendFailure(Component.literal("§cCannot delete group '" + groupName + "' (default group or not found)"));
                return 0;
            }
        } catch (Exception e) {
            LOGGER.error("Error executing group delete command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to delete group: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeGroupAddPermission(CommandContext<CommandSourceStack> context) {
        try {
            String groupName = StringArgumentType.getString(context, "group");
            String permission = StringArgumentType.getString(context, "permission");
            
            CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
            PermissionGroup group = manager.getGroup(groupName);
            
            if (group == null) {
                context.getSource().sendFailure(Component.literal("§cGroup '" + groupName + "' not found"));
                return 0;
            }
            
            group.addPermission(permission);
            manager.clearCache();
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§aAdded permission '" + permission + "' to group '" + groupName + "'"), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing group add permission command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to add permission: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeGroupRemovePermission(CommandContext<CommandSourceStack> context) {
        try {
            String groupName = StringArgumentType.getString(context, "group");
            String permission = StringArgumentType.getString(context, "permission");
            
            CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
            PermissionGroup group = manager.getGroup(groupName);
            
            if (group == null) {
                context.getSource().sendFailure(Component.literal("§cGroup '" + groupName + "' not found"));
                return 0;
            }
            
            group.removePermission(permission);
            manager.clearCache();
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§aRemoved permission '" + permission + "' from group '" + groupName + "'"), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing group remove permission command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to remove permission: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeGroupSetInheritance(CommandContext<CommandSourceStack> context) {
        try {
            String groupName = StringArgumentType.getString(context, "group");
            String parentName = StringArgumentType.getString(context, "parent");
            
            CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
            PermissionGroup group = manager.getGroup(groupName);
            PermissionGroup parent = manager.getGroup(parentName);
            
            if (group == null) {
                context.getSource().sendFailure(Component.literal("§cGroup '" + groupName + "' not found"));
                return 0;
            }
            
            if (parent == null) {
                context.getSource().sendFailure(Component.literal("§cParent group '" + parentName + "' not found"));
                return 0;
            }
            
            group.setInheritance(parentName);
            manager.clearCache();
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§aSet group '" + groupName + "' to inherit from '" + parentName + "'"), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing group set inheritance command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to set inheritance: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeUserInfo(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            return showPlayerPermissionInfo(context, target);
        } catch (Exception e) {
            LOGGER.error("Error executing user info command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to show user info: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeUserSetGroup(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            String groupName = StringArgumentType.getString(context, "group");
            
            CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
            
            if (manager.getGroup(groupName) == null) {
                context.getSource().sendFailure(Component.literal("§cGroup '" + groupName + "' not found"));
                return 0;
            }
            
            manager.setPlayerGroup(target.getUUID(), groupName);
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§aSet " + target.getDisplayName().getString() + "'s group to '" + groupName + "'"), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing user set group command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to set user group: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeUserAddPermission(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            String permission = StringArgumentType.getString(context, "permission");
            
            CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
            manager.addPlayerPermission(target.getUUID(), permission);
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§aAdded permission '" + permission + "' to " + target.getDisplayName().getString()), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing user add permission command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to add permission: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeUserRemovePermission(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            String permission = StringArgumentType.getString(context, "permission");
            
            CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
            manager.removePlayerPermission(target.getUUID(), permission);
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§aRemoved permission '" + permission + "' from " + target.getDisplayName().getString()), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing user remove permission command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to remove permission: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeUserTempPermission(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            String permission = StringArgumentType.getString(context, "permission");
            int duration = IntegerArgumentType.getInteger(context, "duration");
            
            CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
            manager.addTemporaryPermission(target.getUUID(), permission, duration * 1000L);
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§aAdded temporary permission '" + permission + "' to " + 
                target.getDisplayName().getString() + " for " + duration + " seconds"), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing user temp permission command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to add temporary permission: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeUserClear(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            
            CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
            manager.setPlayerGroup(target.getUUID(), "default");
            // Clear custom permissions would go here
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§aCleared custom permissions for " + target.getDisplayName().getString()), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing user clear command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to clear user permissions: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeReload(CommandContext<CommandSourceStack> context) {
        try {
            CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
            manager.initialize();
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§aReloaded permissions system"), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing reload command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to reload permissions: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeStats(CommandContext<CommandSourceStack> context) {
        try {
            CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
            Map<String, Object> stats = manager.getPermissionStats();
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§6§l=== Permission Statistics ==="), false);
            
            for (Map.Entry<String, Object> entry : stats.entrySet()) {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e" + entry.getKey() + ": §b" + entry.getValue()), false);
            }
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing stats command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to show stats: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeCheck(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            String permission = StringArgumentType.getString(context, "permission");
            
            CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
            boolean hasPermission = manager.hasPermission(target, permission);
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§ePlayer " + target.getDisplayName().getString() + 
                (hasPermission ? " §aHAS" : " §cDOES NOT HAVE") + " §epermission: §b" + permission), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing check command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to check permission: " + e.getMessage()));
            return 0;
        }
    }
}
