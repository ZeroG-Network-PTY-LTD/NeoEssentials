
package com.zerog.neoessentials.permissions.command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.zerog.neoessentials.permissions.*;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import java.util.UUID;

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
            .then(Commands.literal("group")
                .then(Commands.argument("group", StringArgumentType.word())
                    .then(Commands.literal("setprefix")
                        .then(Commands.argument("prefix", StringArgumentType.greedyString())
                            .executes(ctx -> setPrefix(ctx))))
                    .then(Commands.literal("setsuffix")
                        .then(Commands.argument("suffix", StringArgumentType.greedyString())
                            .executes(ctx -> setSuffix(ctx))))
                    .then(Commands.literal("add")
                        .then(Commands.argument("permission", StringArgumentType.word())
                            .executes(ctx -> addGroupPermission(ctx))))
                    .then(Commands.literal("remove")
                        .then(Commands.argument("permission", StringArgumentType.word())
                            .executes(ctx -> removeGroupPermission(ctx))))))
            .then(Commands.literal("user")
                .then(Commands.argument("uuid", StringArgumentType.word())
                    .then(Commands.literal("setgroup")
                        .then(Commands.argument("group", StringArgumentType.word())
                            .executes(ctx -> setUserGroup(ctx))))
                    .then(Commands.literal("add")
                        .then(Commands.argument("permission", StringArgumentType.word())
                            .executes(ctx -> addUserPermission(ctx))))
                    .then(Commands.literal("remove")
                        .then(Commands.argument("permission", StringArgumentType.word())
                            .executes(ctx -> removeUserPermission(ctx))))));
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        try {
            PermissionManager manager = new PermissionManager();
            PermissionStorage.load(manager);
            PermissionAPI.setManager(manager);
            ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("commands.neoessentials.permissions.reloaded"), false);
            return 1;
        } catch (Exception e) {
            DebugUtil.debugStackTrace(e);
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.permissions.reload_failed", e.getMessage()));
            return 0;
        }
    }

    private static int setPrefix(CommandContext<CommandSourceStack> ctx) {
        String groupName = StringArgumentType.getString(ctx, "group");
        String prefix = StringArgumentType.getString(ctx, "prefix");
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) {
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.permissions.group_not_found"));
            return 0;
        }
    group.setPrefix(prefix);
    try { PermissionStorage.save(PermissionAPI.getManager()); } catch (Exception e) { DebugUtil.debugStackTrace(e); }
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("commands.neoessentials.permissions.prefix_set"), false);
        return 1;
    }

    private static int setSuffix(CommandContext<CommandSourceStack> ctx) {
        String groupName = StringArgumentType.getString(ctx, "group");
        String suffix = StringArgumentType.getString(ctx, "suffix");
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) {
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.permissions.group_not_found"));
            return 0;
        }
    group.setSuffix(suffix);
    try { PermissionStorage.save(PermissionAPI.getManager()); } catch (Exception e) { DebugUtil.debugStackTrace(e); }
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("commands.neoessentials.permissions.suffix_set"), false);
        return 1;
    }

    private static int addGroupPermission(CommandContext<CommandSourceStack> ctx) {
        String groupName = StringArgumentType.getString(ctx, "group");
        String perm = StringArgumentType.getString(ctx, "permission");
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) {
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.permissions.group_not_found"));
            return 0;
        }
    group.addPermission(perm);
    try { PermissionStorage.save(PermissionAPI.getManager()); } catch (Exception e) { DebugUtil.debugStackTrace(e); }
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("commands.neoessentials.permissions.permission_added"), false);
        return 1;
    }

    private static int removeGroupPermission(CommandContext<CommandSourceStack> ctx) {
        String groupName = StringArgumentType.getString(ctx, "group");
        String perm = StringArgumentType.getString(ctx, "permission");
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) {
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.permissions.group_not_found"));
            return 0;
        }
    group.removePermission(perm);
    try { PermissionStorage.save(PermissionAPI.getManager()); } catch (Exception e) { DebugUtil.debugStackTrace(e); }
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("commands.neoessentials.permissions.permission_removed"), false);
        return 1;
    }

    private static int setUserGroup(CommandContext<CommandSourceStack> ctx) {
        String uuidStr = StringArgumentType.getString(ctx, "uuid");
        String groupName = StringArgumentType.getString(ctx, "group");
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.permissions.invalid_uuid"));
            return 0;
        }
        PermissionUser user = PermissionAPI.getManager().getUser(uuid);
        if (user == null) {
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.permissions.user_not_found"));
            return 0;
        }
    user.setGroup(groupName);
    try { PermissionStorage.save(PermissionAPI.getManager()); } catch (Exception e) { DebugUtil.debugStackTrace(e); }
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("commands.neoessentials.permissions.user_group_set"), false);
        return 1;
    }

    private static int addUserPermission(CommandContext<CommandSourceStack> ctx) {
        String uuidStr = StringArgumentType.getString(ctx, "uuid");
        String perm = StringArgumentType.getString(ctx, "permission");
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.permissions.invalid_uuid"));
            return 0;
        }
        PermissionUser user = PermissionAPI.getManager().getUser(uuid);
        if (user == null) {
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.permissions.user_not_found"));
            return 0;
        }
    user.addPermission(perm);
    try { PermissionStorage.save(PermissionAPI.getManager()); } catch (Exception e) { DebugUtil.debugStackTrace(e); }
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("commands.neoessentials.permissions.permission_added_to_user"), false);
        return 1;
    }

    private static int removeUserPermission(CommandContext<CommandSourceStack> ctx) {
        String uuidStr = StringArgumentType.getString(ctx, "uuid");
        String perm = StringArgumentType.getString(ctx, "permission");
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.permissions.invalid_uuid"));
            return 0;
        }
        PermissionUser user = PermissionAPI.getManager().getUser(uuid);
        if (user == null) {
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.permissions.user_not_found"));
            return 0;
        }
    user.removePermission(perm);
    try { PermissionStorage.save(PermissionAPI.getManager()); } catch (Exception e) { DebugUtil.debugStackTrace(e); }
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("commands.neoessentials.permissions.permission_removed_from_user"), false);
        return 1;
    }
}