
package com.zerog.neoessentials.permissions.command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zerog.neoessentials.util.DebugUtil;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.permissions.*;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.economy.EconomyPlayerUtil;
import com.zerog.neoessentials.util.MessageUtil;
import java.util.UUID;
import java.util.Optional;
import java.util.stream.Collectors;

public class PermissionsCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionsCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register under both /pex and /permissions
        dispatcher.register(createRoot("pex"));
        dispatcher.register(createRoot("permissions"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createRoot(String root) {
        return Commands.literal(root)
            .then(Commands.literal("reload")
                .executes(ctx -> reload(ctx)))
            .then(Commands.literal("list")
                .then(Commands.literal("groups")
                    .executes(ctx -> listGroups(ctx)))
                .then(Commands.literal("users")
                    .executes(ctx -> listUsers(ctx))))
            .then(Commands.literal("group")
                .then(Commands.argument("group", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                        PermissionAPI.getManager().getGroups().stream()
                            .map(PermissionGroup::getName),
                        builder
                    ))
                    .then(Commands.literal("setprefix")
                        .then(Commands.argument("prefix", StringArgumentType.greedyString())
                            .executes(ctx -> setPrefix(ctx))))
                    .then(Commands.literal("setsuffix")
                        .then(Commands.argument("suffix", StringArgumentType.greedyString())
                            .executes(ctx -> setSuffix(ctx))))
                    .then(Commands.literal("add")
                        .then(Commands.argument("permission", StringArgumentType.word())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                java.util.Arrays.asList(
                                    "neoessentials.*",
                                    "neoessentials.item.*",
                                    "neoessentials.economy.*",
                                    "neoessentials.chat.*",
                                    "neoessentials.admin.*",
                                    "neoessentials.teleport.*"
                                ),
                                builder
                            ))
                            .executes(ctx -> addGroupPermission(ctx))))
                    .then(Commands.literal("remove")
                        .then(Commands.argument("permission", StringArgumentType.word())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                java.util.Arrays.asList(
                                    "neoessentials.*",
                                    "neoessentials.item.*",
                                    "neoessentials.economy.*",
                                    "neoessentials.chat.*",
                                    "neoessentials.admin.*",
                                    "neoessentials.teleport.*"
                                ),
                                builder
                            ))
                            .executes(ctx -> removeGroupPermission(ctx))))))
            .then(Commands.literal("user")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                        ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                            .map(p -> p.getGameProfile().getName()),
                        builder
                    ))
                    .then(Commands.literal("setgroup")
                        .then(Commands.argument("group", StringArgumentType.word())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                PermissionAPI.getManager().getGroups().stream()
                                    .map(PermissionGroup::getName),
                                builder
                            ))
                            .executes(ctx -> setUserGroup(ctx))))
                    .then(Commands.literal("add")
                        .then(Commands.argument("permission", StringArgumentType.word())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                java.util.Arrays.asList(
                                    "neoessentials.*",
                                    "neoessentials.item.*",
                                    "neoessentials.economy.*",
                                    "neoessentials.chat.*",
                                    "neoessentials.admin.*",
                                    "neoessentials.teleport.*"
                                ),
                                builder
                            ))
                            .executes(ctx -> addUserPermission(ctx))))
                    .then(Commands.literal("remove")
                        .then(Commands.argument("permission", StringArgumentType.word())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                java.util.Arrays.asList(
                                    "neoessentials.*",
                                    "neoessentials.item.*",
                                    "neoessentials.economy.*",
                                    "neoessentials.chat.*",
                                    "neoessentials.admin.*",
                                    "neoessentials.teleport.*"
                                ),
                                builder
                            ))
                            .executes(ctx -> removeUserPermission(ctx))))));
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        try {
            PermissionManager manager = new PermissionManager();
            PermissionStorage.load(manager);
            PermissionAPI.setManager(manager);
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.reloaded"), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to reload permissions", e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.reload_failed", e.getMessage()));
            return 0;
        }
    }

    private static int setPrefix(CommandContext<CommandSourceStack> ctx) {
        String groupName = StringArgumentType.getString(ctx, "group");
        String prefix = StringArgumentType.getString(ctx, "prefix");
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found"));
            return 0;
        }
    group.setPrefix(prefix);
    try { PermissionStorage.save(PermissionAPI.getManager()); } catch (Exception e) { LOGGER.error("Failed to save permissions after setting prefix", e); }
        ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.prefix_set", groupName, prefix), false);
        return 1;
    }

    private static int setSuffix(CommandContext<CommandSourceStack> ctx) {
        String groupName = StringArgumentType.getString(ctx, "group");
        String suffix = StringArgumentType.getString(ctx, "suffix");
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found"));
            return 0;
        }
    group.setSuffix(suffix);
    try { PermissionStorage.save(PermissionAPI.getManager()); } catch (Exception e) { LOGGER.error("Failed to save permissions after setting suffix", e); }
        ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.suffix_set", groupName, suffix), false);
        return 1;
    }

    private static int addGroupPermission(CommandContext<CommandSourceStack> ctx) {
        String groupName = StringArgumentType.getString(ctx, "group");
        String perm = StringArgumentType.getString(ctx, "permission");
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found"));
            return 0;
        }
    group.addPermission(perm);
    try { PermissionStorage.save(PermissionAPI.getManager()); } catch (Exception e) { LOGGER.error("Failed to save permissions after adding group permission", e); }
        ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.permission_added", perm, groupName), false);
        return 1;
    }

    private static int removeGroupPermission(CommandContext<CommandSourceStack> ctx) {
        String groupName = StringArgumentType.getString(ctx, "group");
        String perm = StringArgumentType.getString(ctx, "permission");
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found"));
            return 0;
        }
    group.removePermission(perm);
    try { PermissionStorage.save(PermissionAPI.getManager()); } catch (Exception e) { LOGGER.error("Failed to save permissions after removing group permission", e); }
        ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.permission_removed", perm, groupName), false);
        return 1;
    }

    private static int setUserGroup(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        String groupName = StringArgumentType.getString(ctx, "group");
        MinecraftServer server = ctx.getSource().getServer();
        
        // Try to get UUID by player name
        Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(server, playerName);
        if (uuidOpt.isEmpty()) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.player_not_found"));
            return 0;
        }
        
        UUID uuid = uuidOpt.get();
        PermissionUser user = PermissionAPI.getManager().getUser(uuid);
        if (user == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.user_not_found"));
            return 0;
        }
        
        // Check if group exists
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found"));
            return 0;
        }
        
        user.setGroup(groupName);
        try { PermissionStorage.save(PermissionAPI.getManager()); } catch (Exception e) { LOGGER.error("Failed to save permissions after setting user group", e); }
        ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.user_group_set", playerName, groupName), false);
        return 1;
    }

    private static int addUserPermission(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        String perm = StringArgumentType.getString(ctx, "permission");
        MinecraftServer server = ctx.getSource().getServer();
        
        // Try to get UUID by player name
        Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(server, playerName);
        if (uuidOpt.isEmpty()) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.player_not_found"));
            return 0;
        }
        
        UUID uuid = uuidOpt.get();
        PermissionUser user = PermissionAPI.getManager().getUser(uuid);
        if (user == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.user_not_found"));
            return 0;
        }
        
        user.addPermission(perm);
        try { PermissionStorage.save(PermissionAPI.getManager()); } catch (Exception e) { LOGGER.error("Failed to save permissions after adding user permission", e); }
        ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.permission_added_to_user", perm, playerName), false);
        return 1;
    }

    private static int removeUserPermission(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        String perm = StringArgumentType.getString(ctx, "permission");
        MinecraftServer server = ctx.getSource().getServer();
        
        // Try to get UUID by player name
        Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(server, playerName);
        if (uuidOpt.isEmpty()) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.player_not_found"));
            return 0;
        }
        
        UUID uuid = uuidOpt.get();
        PermissionUser user = PermissionAPI.getManager().getUser(uuid);
        if (user == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.user_not_found"));
            return 0;
        }
        
        user.removePermission(perm);
        try { PermissionStorage.save(PermissionAPI.getManager()); } catch (Exception e) { LOGGER.error("Failed to save permissions after removing user permission", e); }
        ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.permission_removed_from_user", perm, playerName), false);
        return 1;
    }
    
    private static int listGroups(CommandContext<CommandSourceStack> ctx) {
        PermissionManager manager = PermissionAPI.getManager();
        if (manager == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.manager_not_available"));
            return 0;
        }
        
        var groups = manager.getGroups();
        if (groups.isEmpty()) {
            ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.no_groups"), false);
            return 1;
        }
        
        ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.groups_header"), false);
        for (PermissionGroup group : groups) {
            String prefix = group.getPrefix() != null ? group.getPrefix() : "none";
            String suffix = group.getSuffix() != null ? group.getSuffix() : "none";
            ctx.getSource().sendSuccess(() -> MessageUtil.component("commands.neoessentials.permissions.group_entry", 
                group.getName(), prefix, suffix), false);
        }
        return 1;
    }
    
    private static int listUsers(CommandContext<CommandSourceStack> ctx) {
        PermissionManager manager = PermissionAPI.getManager();
        if (manager == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.manager_not_available"));
            return 0;
        }
        
        var users = manager.getUsers();
        if (users.isEmpty()) {
            ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.no_users"), false);
            return 1;
        }
        
        MinecraftServer server = ctx.getSource().getServer();
        ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.users_header"), false);
        
        for (PermissionUser user : users) {
            UUID uuid = user.getUuid();
            String displayName = uuid.toString();
            
            // Try to get player name from online players first
            Optional<ServerPlayer> onlinePlayer = server.getPlayerList().getPlayers().stream()
                .filter(p -> p.getUUID().equals(uuid))
                .findFirst();
                
            if (onlinePlayer.isPresent()) {
                displayName = onlinePlayer.get().getGameProfile().getName();
            } else {
                // Try to get from profile cache
                var profile = server.getProfileCache().get(uuid);
                if (profile.isPresent()) {
                    displayName = profile.get().getName();
                }
            }
            
            // Show both name and UUID if we found a name, otherwise just UUID
            String userDisplay = displayName.equals(uuid.toString()) ? 
                displayName : displayName + " (" + uuid.toString().substring(0, 8) + "...)";
            
            String group = user.getGroup() != null ? user.getGroup() : "default";
            ctx.getSource().sendSuccess(() -> MessageUtil.component("commands.neoessentials.permissions.user_entry", userDisplay, group), false);
        }
        return 1;
    }
}