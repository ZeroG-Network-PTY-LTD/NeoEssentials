package com.zerog.neoessentials.permissions.command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.permissions.PermissionManager;
import com.zerog.neoessentials.permissions.PermissionGroup;
import com.zerog.neoessentials.permissions.PermissionUser;
import com.zerog.neoessentials.permissions.PermissionStorage;
import com.zerog.neoessentials.permissions.PermissionAuditLogger;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.economy.EconomyPlayerUtil;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import com.zerog.neoessentials.chat.RichTextFormatter;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.UUID;
import java.util.Optional;


public class PermissionsCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionsCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Check if permissions module is enabled
        if (!com.zerog.neoessentials.config.ConfigManager.isPermissionsEnabled()) {
            LOGGER.debug("Permissions module is disabled, skipping permissions command registration");
            return;
        }
        
        // Check if individual permissions commands are enabled
        boolean pexEnabled = com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("pex");
        boolean permissionsEnabled = com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("permissions");
        
        if (!pexEnabled && !permissionsEnabled) {
            LOGGER.debug("Both pex and permissions commands are disabled, skipping registration");
            return;
        }
        
        // Register under both /pex and /permissions if enabled
        if (pexEnabled) {
            dispatcher.register(createRoot("pex"));
        }
        if (permissionsEnabled) {
            dispatcher.register(createRoot("permissions"));
        }
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
            .then(Commands.literal("info")
                .then(Commands.literal("group")
                    .then(Commands.argument("group", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            try {
                                var groups = PermissionAPI.getManager().getGroups().stream()
                                    .map(PermissionGroup::getName)
                                    .toList();
                                if (!groups.isEmpty()) {
                                    return SharedSuggestionProvider.suggest(groups, builder);
                                }
                            } catch (Exception e) {}
                            return SharedSuggestionProvider.suggest(
                                java.util.List.of("admin", "moderator", "player", "vip", "default"),
                                builder);
                        })
                        .executes(ctx -> showGroupInfo(ctx))))
                .then(Commands.literal("user")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                            ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                                .map(p -> p.getGameProfile().getName()),
                            builder
                        ))
                        .executes(ctx -> showUserInfo(ctx)))))
            .then(Commands.literal("check")
                .then(Commands.literal("user")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                            ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                                .map(p -> p.getGameProfile().getName()),
                            builder
                        ))
                        .then(Commands.argument("permission", StringArgumentType.greedyString())
                            .executes(ctx -> checkUserPermission(ctx)))))
                .then(Commands.literal("group")
                    .then(Commands.argument("group", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            try {
                                var groups = PermissionAPI.getManager().getGroups().stream()
                                    .map(PermissionGroup::getName)
                                    .toList();
                                if (!groups.isEmpty()) {
                                    return SharedSuggestionProvider.suggest(groups, builder);
                                }
                            } catch (Exception e) {}
                            return SharedSuggestionProvider.suggest(
                                java.util.List.of("admin", "moderator", "player", "vip", "default"),
                                builder);
                        })
                        .then(Commands.argument("permission", StringArgumentType.greedyString())
                            .executes(ctx -> checkGroupPermission(ctx))))))
            .then(Commands.literal("search")
                .then(Commands.argument("pattern", StringArgumentType.greedyString())
                    .executes(ctx -> searchPermissions(ctx))))
            .then(Commands.literal("create")
                .then(Commands.literal("group")
                    .then(Commands.argument("group", StringArgumentType.word())
                        .executes(ctx -> createGroup(ctx)))))
            .then(Commands.literal("delete")
                .then(Commands.literal("group")
                    .then(Commands.argument("group", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            try {
                                var groups = PermissionAPI.getManager().getGroups().stream()
                                    .map(PermissionGroup::getName)
                                    .toList();
                                if (!groups.isEmpty()) {
                                    return SharedSuggestionProvider.suggest(groups, builder);
                                }
                            } catch (Exception e) {}
                            return SharedSuggestionProvider.suggest(
                                java.util.List.of("admin", "moderator", "player", "vip", "default"),
                                builder);
                        })
                        .executes(ctx -> deleteGroup(ctx)))))
            .then(Commands.literal("rename")
                .then(Commands.literal("group")
                    .then(Commands.argument("oldName", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            try {
                                var groups = PermissionAPI.getManager().getGroups().stream()
                                    .map(PermissionGroup::getName)
                                    .toList();
                                if (!groups.isEmpty()) {
                                    return SharedSuggestionProvider.suggest(groups, builder);
                                }
                            } catch (Exception e) {}
                            return SharedSuggestionProvider.suggest(
                                java.util.List.of("admin", "moderator", "player", "vip", "default"),
                                builder);
                        })
                        .then(Commands.argument("newName", StringArgumentType.word())
                            .executes(ctx -> renameGroup(ctx))))))
            .then(Commands.literal("clone")
                .then(Commands.literal("group")
                    .then(Commands.argument("source", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            try {
                                var groups = PermissionAPI.getManager().getGroups().stream()
                                    .map(PermissionGroup::getName)
                                    .toList();
                                if (!groups.isEmpty()) {
                                    return SharedSuggestionProvider.suggest(groups, builder);
                                }
                            } catch (Exception e) {}
                            return SharedSuggestionProvider.suggest(
                                java.util.List.of("admin", "moderator", "player", "vip", "default"),
                                builder);
                        })
                        .then(Commands.argument("newGroup", StringArgumentType.word())
                            .executes(ctx -> cloneGroup(ctx))))))
            .then(Commands.literal("group")
                .then(Commands.argument("group", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        try {
                            // First try to get actual groups from PermissionAPI
                            var groups = PermissionAPI.getManager().getGroups().stream()
                                .map(PermissionGroup::getName)
                                .toList();
                            
                            if (!groups.isEmpty()) {
                                return SharedSuggestionProvider.suggest(groups, builder);
                            }
                        } catch (Exception e) {
                            // Fall through to default suggestions
                        }
                        
                        // Fallback to common group names if no groups are loaded
                        return SharedSuggestionProvider.suggest(
                            java.util.List.of("admin", "moderator", "player", "vip", "default"), 
                            builder);
                    })
                    .then(Commands.literal("setprefix")
                        .then(Commands.argument("prefix", StringArgumentType.greedyString())
                            .executes(ctx -> setPrefix(ctx))))
                    .then(Commands.literal("setsuffix")
                        .then(Commands.argument("suffix", StringArgumentType.greedyString())
                            .executes(ctx -> setSuffix(ctx))))
                    .then(Commands.literal("setpriority")
                        .then(Commands.argument("priority", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-999, 999))
                            .executes(ctx -> setGroupPriority(ctx))))
                    .then(Commands.literal("getpriority")
                        .executes(ctx -> getGroupPriority(ctx)))
                    .then(Commands.literal("add")
                        .then(Commands.argument("permission", StringArgumentType.greedyString())
                            .suggests((ctx, builder) -> {
                                // Use dynamic permission provider instead of hardcoded list
                                try {
                                    java.util.List<String> permissions = 
                                        com.zerog.neoessentials.api.permissions.external.ExternalPermissionProvider.getAllNeoEssentialsPermissions();
                                    String input = builder.getRemaining().toLowerCase();
                                    
                                    java.util.List<String> filtered = permissions.stream()
                                        .filter(perm -> perm.toLowerCase().startsWith(input))
                                        .toList();
                                        
                                    return SharedSuggestionProvider.suggest(filtered, builder);
                                } catch (Exception e) {
                                    // Fallback to basic suggestions if dynamic loading fails
                                    return SharedSuggestionProvider.suggest(
                                        java.util.List.of(
                                            "neoessentials.*",
                                            "neoessentials.admin.*",
                                            "neoessentials.economy.*",
                                            "neoessentials.teleport.*"
                                        ),
                                        builder
                                    );
                                }
                            })
                            .executes(ctx -> addGroupPermission(ctx))))
                    .then(Commands.literal("remove")
                        .then(Commands.argument("permission", StringArgumentType.greedyString())
                            .suggests((ctx, builder) -> {
                                // Use dynamic permission provider instead of hardcoded list
                                try {
                                    java.util.List<String> permissions = 
                                        com.zerog.neoessentials.api.permissions.external.ExternalPermissionProvider.getAllNeoEssentialsPermissions();
                                    String input = builder.getRemaining().toLowerCase();
                                    
                                    java.util.List<String> filtered = permissions.stream()
                                        .filter(perm -> perm.toLowerCase().startsWith(input))
                                        .toList();
                                        
                                    return SharedSuggestionProvider.suggest(filtered, builder);
                                } catch (Exception e) {
                                    // Fallback to basic suggestions if dynamic loading fails
                                    return SharedSuggestionProvider.suggest(
                                        java.util.List.of(
                                            "neoessentials.*",
                                            "neoessentials.admin.*",
                                            "neoessentials.economy.*",
                                            "neoessentials.teleport.*"
                                        ),
                                        builder
                                    );
                                }
                            })
                            .executes(ctx -> removeGroupPermission(ctx))))
                    .then(Commands.literal("clear")
                        .executes(ctx -> clearGroupPermissions(ctx)))
                    .then(Commands.literal("inherit")
                        .then(Commands.literal("add")
                            .then(Commands.argument("inheritGroup", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    try {
                                        var groups = PermissionAPI.getManager().getGroups().stream()
                                            .map(PermissionGroup::getName)
                                            .toList();
                                        if (!groups.isEmpty()) {
                                            return SharedSuggestionProvider.suggest(groups, builder);
                                        }
                                    } catch (Exception e) {}
                                    return SharedSuggestionProvider.suggest(
                                        java.util.List.of("admin", "moderator", "player", "vip", "default"),
                                        builder);
                                })
                                .executes(ctx -> addGroupInheritance(ctx))))
                        .then(Commands.literal("remove")
                            .then(Commands.argument("inheritGroup", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    try {
                                        String groupName = StringArgumentType.getString(ctx, "group");
                                        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
                                        if (group != null && !group.getInherits().isEmpty()) {
                                            return SharedSuggestionProvider.suggest(group.getInherits(), builder);
                                        }
                                    } catch (Exception e) {}
                                    return SharedSuggestionProvider.suggest(
                                        java.util.List.of("admin", "moderator", "player", "vip", "default"),
                                        builder);
                                })
                                .executes(ctx -> removeGroupInheritance(ctx)))))
                    .then(Commands.literal("addtemp")
                        .then(Commands.argument("permission", StringArgumentType.string())
                            .suggests((ctx, builder) -> {
                                try {
                                    java.util.List<String> perms =
                                        com.zerog.neoessentials.api.permissions.external.ExternalPermissionProvider.getAllNeoEssentialsPermissions();
                                    String input = builder.getRemaining().toLowerCase();
                                    return SharedSuggestionProvider.suggest(
                                        perms.stream().filter(p -> p.toLowerCase().startsWith(input)).toList(), builder);
                                } catch (Exception e) {
                                    return SharedSuggestionProvider.suggest(
                                        java.util.List.of("neoessentials.*","neoessentials.admin.*"), builder);
                                }
                            })
                            .then(Commands.argument("duration", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                    java.util.List.of("30m","1h","6h","12h","1d","7d","30d"), builder))
                                .executes(ctx -> addGroupTempPermission(ctx)))))
                    .then(Commands.literal("removetemp")
                        .then(Commands.argument("permission", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                try {
                                    String groupName = StringArgumentType.getString(ctx, "group");
                                    PermissionGroup g = PermissionAPI.getManager().getGroup(groupName);
                                    if (g != null) return SharedSuggestionProvider.suggest(g.getTempPermissions().keySet(), builder);
                                } catch (Exception ignored) {}
                                return builder.buildFuture();
                            })
                            .executes(ctx -> removeGroupTempPermission(ctx))))
                    .then(Commands.literal("listtemp")
                        .executes(ctx -> listGroupTempPermissions(ctx)))
                    .then(Commands.literal("context")
                        .then(Commands.literal("add")
                            .then(Commands.argument("contextKey", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                    com.zerog.neoessentials.permissions.PermissionContext.SUGGESTIONS, builder))
                                .then(Commands.argument("permission", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        try {
                                            java.util.List<String> perms =
                                                com.zerog.neoessentials.api.permissions.external.ExternalPermissionProvider.getAllNeoEssentialsPermissions();
                                            return SharedSuggestionProvider.suggest(
                                                perms.stream().filter(p -> p.startsWith(builder.getRemaining().toLowerCase())).toList(), builder);
                                        } catch (Exception e) { return builder.buildFuture(); }
                                    })
                                    .then(Commands.literal("allow")
                                        .executes(ctx -> addGroupContextPermission(ctx, true)))
                                    .then(Commands.literal("deny")
                                        .executes(ctx -> addGroupContextPermission(ctx, false))))))
                        .then(Commands.literal("remove")
                            .then(Commands.argument("contextKey", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    try {
                                        String gn = StringArgumentType.getString(ctx, "group");
                                        PermissionGroup g = PermissionAPI.getManager().getGroup(gn);
                                        if (g != null) return SharedSuggestionProvider.suggest(g.getContextualPermissions().keySet(), builder);
                                    } catch (Exception ignored) {}
                                    return SharedSuggestionProvider.suggest(
                                        com.zerog.neoessentials.permissions.PermissionContext.SUGGESTIONS, builder);
                                })
                                .then(Commands.argument("permission", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        try {
                                            String gn = StringArgumentType.getString(ctx, "group");
                                            String ck = StringArgumentType.getString(ctx, "contextKey");
                                            PermissionGroup g = PermissionAPI.getManager().getGroup(gn);
                                            if (g != null && g.getContextualPermissions().containsKey(ck))
                                                return SharedSuggestionProvider.suggest(g.getContextualPermissions().get(ck).keySet(), builder);
                                        } catch (Exception ignored) {}
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> removeGroupContextPermission(ctx)))))
                        .then(Commands.literal("list")
                            .executes(ctx -> listGroupContextPermissions(ctx))))))
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
                        .then(Commands.argument("permission", StringArgumentType.greedyString())
                            .suggests((ctx, builder) -> {
                                // Use dynamic permission provider instead of hardcoded list
                                try {
                                    java.util.List<String> permissions = 
                                        com.zerog.neoessentials.api.permissions.external.ExternalPermissionProvider.getAllNeoEssentialsPermissions();
                                    String input = builder.getRemaining().toLowerCase();
                                    
                                    java.util.List<String> filtered = permissions.stream()
                                        .filter(perm -> perm.toLowerCase().startsWith(input))
                                        .toList();
                                        
                                    return SharedSuggestionProvider.suggest(filtered, builder);
                                } catch (Exception e) {
                                    // Fallback to basic suggestions if dynamic loading fails
                                    return SharedSuggestionProvider.suggest(
                                        java.util.List.of(
                                            "neoessentials.*",
                                            "neoessentials.admin.*",
                                            "neoessentials.economy.*",
                                            "neoessentials.teleport.*"
                                        ),
                                        builder
                                    );
                                }
                            })
                            .executes(ctx -> addUserPermission(ctx))))
                    .then(Commands.literal("remove")
                        .then(Commands.argument("permission", StringArgumentType.greedyString())
                            .suggests((ctx, builder) -> {
                                // Use dynamic permission provider instead of hardcoded list
                                try {
                                    java.util.List<String> permissions = 
                                        com.zerog.neoessentials.api.permissions.external.ExternalPermissionProvider.getAllNeoEssentialsPermissions();
                                    String input = builder.getRemaining().toLowerCase();
                                    
                                    java.util.List<String> filtered = permissions.stream()
                                        .filter(perm -> perm.toLowerCase().startsWith(input))
                                        .toList();
                                        
                                    return SharedSuggestionProvider.suggest(filtered, builder);
                                } catch (Exception e) {
                                    // Fallback to basic suggestions if dynamic loading fails
                                    return SharedSuggestionProvider.suggest(
                                        java.util.List.of(
                                            "neoessentials.*",
                                            "neoessentials.admin.*",
                                            "neoessentials.economy.*",
                                            "neoessentials.teleport.*"
                                        ),
                                        builder
                                    );
                                }
                            })
                            .executes(ctx -> removeUserPermission(ctx))))
                    .then(Commands.literal("clear")
                        .executes(ctx -> clearUserPermissions(ctx)))
                    .then(Commands.literal("addtemp")
                        .then(Commands.argument("permission", StringArgumentType.string())
                            .suggests((ctx, builder) -> {
                                try {
                                    java.util.List<String> permissions =
                                        com.zerog.neoessentials.api.permissions.external.ExternalPermissionProvider.getAllNeoEssentialsPermissions();
                                    String input = builder.getRemaining().toLowerCase();
                                    java.util.List<String> filtered = permissions.stream()
                                        .filter(p -> p.toLowerCase().startsWith(input)).toList();
                                    return SharedSuggestionProvider.suggest(filtered, builder);
                                } catch (Exception e) {
                                    return SharedSuggestionProvider.suggest(
                                        java.util.List.of("neoessentials.*","neoessentials.admin.*"), builder);
                                }
                            })
                            .then(Commands.argument("duration", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                    java.util.List.of("30m","1h","6h","12h","1d","7d","30d"), builder))
                                .executes(ctx -> addUserTempPermission(ctx)))))
                    .then(Commands.literal("removetemp")
                        .then(Commands.argument("permission", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                try {
                                    String playerName = StringArgumentType.getString(ctx, "player");
                                    Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(ctx.getSource().getServer(), playerName);
                                    if (uuidOpt.isPresent()) {
                                        PermissionUser u = PermissionAPI.getManager().getUser(uuidOpt.get());
                                        if (u != null) return SharedSuggestionProvider.suggest(u.getTempPermissions().keySet(), builder);
                                    }
                                } catch (Exception ignored) {}
                                return builder.buildFuture();
                            })
                            .executes(ctx -> removeUserTempPermission(ctx))))
                    .then(Commands.literal("listtemp")
                        .executes(ctx -> listUserTempPermissions(ctx)))
                    .then(Commands.literal("context")
                        .then(Commands.literal("add")
                            .then(Commands.argument("contextKey", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                    com.zerog.neoessentials.permissions.PermissionContext.SUGGESTIONS, builder))
                                .then(Commands.argument("permission", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        try {
                                            java.util.List<String> perms =
                                                com.zerog.neoessentials.api.permissions.external.ExternalPermissionProvider.getAllNeoEssentialsPermissions();
                                            return SharedSuggestionProvider.suggest(
                                                perms.stream().filter(p -> p.startsWith(builder.getRemaining().toLowerCase())).toList(), builder);
                                        } catch (Exception e) { return builder.buildFuture(); }
                                    })
                                    .then(Commands.literal("allow")
                                        .executes(ctx -> addUserContextPermission(ctx, true)))
                                    .then(Commands.literal("deny")
                                        .executes(ctx -> addUserContextPermission(ctx, false))))))
                        .then(Commands.literal("remove")
                            .then(Commands.argument("contextKey", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    try {
                                        String playerName = StringArgumentType.getString(ctx, "player");
                                        Optional<UUID> uOpt = EconomyPlayerUtil.getUUIDByName(ctx.getSource().getServer(), playerName);
                                        if (uOpt.isPresent()) {
                                            PermissionUser u = PermissionAPI.getManager().getUser(uOpt.get());
                                            if (u != null) return SharedSuggestionProvider.suggest(u.getContextualPermissions().keySet(), builder);
                                        }
                                    } catch (Exception ignored) {}
                                    return SharedSuggestionProvider.suggest(
                                        com.zerog.neoessentials.permissions.PermissionContext.SUGGESTIONS, builder);
                                })
                                .then(Commands.argument("permission", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        try {
                                            String playerName = StringArgumentType.getString(ctx, "player");
                                            String ck = StringArgumentType.getString(ctx, "contextKey");
                                            Optional<UUID> uOpt = EconomyPlayerUtil.getUUIDByName(ctx.getSource().getServer(), playerName);
                                            if (uOpt.isPresent()) {
                                                PermissionUser u = PermissionAPI.getManager().getUser(uOpt.get());
                                                if (u != null && u.getContextualPermissions().containsKey(ck))
                                                    return SharedSuggestionProvider.suggest(u.getContextualPermissions().get(ck).keySet(), builder);
                                            }
                                        } catch (Exception ignored) {}
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> removeUserContextPermission(ctx)))))
                        .then(Commands.literal("list")
                            .executes(ctx -> listUserContextPermissions(ctx))))))
            .then(Commands.literal("debug")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                        ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                            .map(p -> p.getGameProfile().getName()),
                        builder
                    ))
                    .executes(ctx -> debugPlayerPermissions(ctx))));
    }

    /** Returns the player name or "CONSOLE" for non-player sources. */
    private static String getExecutorDisplay(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer p = ctx.getSource().getPlayer();
            return p != null ? p.getGameProfile().getName() : "CONSOLE";
        } catch (Exception e) {
            return "CONSOLE";
        }
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        // Validate admin permission for reloading permissions
        PermissionValidator.PermissionResult permResult = 
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.reload");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }
        
        try {
            com.zerog.neoessentials.permissions.PermissionSystem.reload();
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.PERMISSIONS_RELOADED, "-", "full reload via /permissions reload");
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.reloaded"), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to reload permissions", e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.reload_failed", e.getMessage()));
            return 0;
        }
    }

    private static int setPrefix(CommandContext<CommandSourceStack> ctx) {
        // Validate admin permission for modifying groups
        PermissionValidator.PermissionResult permResult = 
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.group.modify");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }
        
        String groupName = StringArgumentType.getString(ctx, "group");
        String prefix = StringArgumentType.getString(ctx, "prefix");

        // Safety validations
        if (prefix.length() > 64) {
            ctx.getSource().sendFailure(MessageUtil.error("Prefix is too long! Maximum length is 64 characters."));
            return 0;
        }

        // Validate no dangerous characters (but allow color codes &)
        if (prefix.matches(".*[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F].*")) {
            ctx.getSource().sendFailure(MessageUtil.error("Prefix contains invalid control characters!"));
            return 0;
        }

        // Check for group existence
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found"));
            return 0;
        }

        // Set the prefix
        group.setPrefix(prefix);

        // Clear cache to ensure new prefix is used immediately
        PermissionAPI.getManager().clearCache();

        // Save with proper error handling
        try {
            PermissionStorage.save(PermissionAPI.getManager());
            LOGGER.info("Set prefix '{}' for group '{}'", prefix, groupName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_PREFIX_SET, groupName, "prefix=\"" + prefix + "\"");
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.prefix_set", groupName, prefix), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to save permissions after setting prefix", e);
            ctx.getSource().sendFailure(MessageUtil.error("Failed to save prefix: " + e.getMessage()));
            return 0;
        }
    }

    private static int setSuffix(CommandContext<CommandSourceStack> ctx) {
        // Validate admin permission for modifying groups
        PermissionValidator.PermissionResult permResult = 
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.group.modify");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }
        
        String groupName = StringArgumentType.getString(ctx, "group");
        String suffix = StringArgumentType.getString(ctx, "suffix");

        // Safety validations
        if (suffix.length() > 64) {
            ctx.getSource().sendFailure(MessageUtil.error("Suffix is too long! Maximum length is 64 characters."));
            return 0;
        }

        // Validate no dangerous characters (but allow color codes &)
        if (suffix.matches(".*[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F].*")) {
            ctx.getSource().sendFailure(MessageUtil.error("Suffix contains invalid control characters!"));
            return 0;
        }

        // Check for group existence
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found"));
            return 0;
        }

        // Set the suffix
        group.setSuffix(suffix);

        // Clear cache to ensure new suffix is used immediately
        PermissionAPI.getManager().clearCache();

        // Save with proper error handling
        try {
            PermissionStorage.save(PermissionAPI.getManager());
            LOGGER.info("Set suffix '{}' for group '{}'", suffix, groupName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_SUFFIX_SET, groupName, "suffix=\"" + suffix + "\"");
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.suffix_set", groupName, suffix), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to save permissions after setting suffix", e);
            ctx.getSource().sendFailure(MessageUtil.error("Failed to save suffix: " + e.getMessage()));
            return 0;
        }
    }

    private static int setGroupPriority(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.group.modify");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }
        String groupName = StringArgumentType.getString(ctx, "group");
        int priority = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "priority");
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found", groupName));
            return 0;
        }
        group.setPriority(priority);
        PermissionAPI.getManager().clearCache();
        try {
            PermissionStorage.save(PermissionAPI.getManager());
            LOGGER.info("Set priority {} for group '{}'", priority, groupName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_PRIORITY_SET, groupName, "priority=" + priority);
            ctx.getSource().sendSuccess(() -> MessageUtil.success(
                "Priority for group '" + groupName + "' set to " + priority + "."), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to save permissions after setting priority", e);
            ctx.getSource().sendFailure(MessageUtil.error("Failed to save priority: " + e.getMessage()));
            return 0;
        }
    }

    private static int getGroupPriority(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.permissions.info.group");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }
        String groupName = StringArgumentType.getString(ctx, "group");
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found", groupName));
            return 0;
        }
        int p = group.getPriority();
        ctx.getSource().sendSuccess(() -> MessageUtil.info(
            "Group '" + groupName + "' priority: " + p
            + (p != 0 ? " §8(higher = checked first in inheritance)" : "")), false);
        return 1;
    }

    private static int addGroupPermission(CommandContext<CommandSourceStack> ctx) {
        try {
            // Validate admin permission for modifying group permissions
            PermissionValidator.PermissionResult permResult =
                PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.group.permissions");
            if (!permResult.hasPermission()) {
                ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                return 0;
            }

            final String groupName = StringArgumentType.getString(ctx, "group");
            final String perm = StringArgumentType.getString(ctx, "permission").toLowerCase().trim();

            LOGGER.debug("Adding permission '{}' to group '{}'", perm, groupName);
            
            // Validate permission format
            if (!PermissionManager.isValidPermission(perm)) {
                ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.invalid_permission", perm));
                return 0;
            }
            
            PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
            if (group == null) {
                ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found", groupName));
                return 0;
            }
            
            // Check if permission already exists
            if (group.getPermissions().contains(perm)) {
                ctx.getSource().sendFailure(MessageUtil.warning("commands.neoessentials.permissions.permission_already_exists", perm, groupName));
                return 0;
            }
            
            group.addPermission(perm);
            
            // Clear permission cache after modification
            PermissionAPI.getManager().clearCache();
            
            try { 
                PermissionStorage.save(PermissionAPI.getManager()); 
                LOGGER.info("Added permission '{}' to group '{}'", perm, groupName);
                PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_PERM_ADDED, groupName, "node=" + perm);
            } catch (Exception e) { 
                LOGGER.error("Failed to save permissions after adding group permission", e);
                ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed"));
                return 0;
            }
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.permission_added", perm, groupName), false);

            // If the permission is for an external mod, give a helpful note about the handler
            boolean isExternal = !perm.startsWith("neoessentials.");
            if (isExternal) {
                // Check whether the NeoEssentials handler is currently active
                boolean neoessentialsHandlerActive;
                try {
                    var activeHandler = net.neoforged.neoforge.server.permission.PermissionAPI.getActivePermissionHandler();
                    neoessentialsHandlerActive = activeHandler != null &&
                            activeHandler.equals(com.zerog.neoessentials.permissions.NeoEssentialsPermissionHandler.IDENTIFIER);
                } catch (Exception ex) {
                    neoessentialsHandlerActive = false;
                }
                if (neoessentialsHandlerActive) {
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                            "§7[Info] NeoEssentials permission handler is active — §a" + perm +
                            "§7 will apply to all mods that use NeoForge's permission API."), false);
                } else {
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                            "§e[Note] §7Permission §f" + perm + " §7is stored in permissions.json but will only " +
                            "affect NeoEssentials commands right now.  " +
                            "To apply it to external mods (e.g. WorldEdit), NeoEssentials must be the active " +
                            "NeoForge permission handler.  Set §f'permissionHandler = \"neoessentials:handler\"' " +
                            "§7in §fconfig/neoforge-server.toml §7(or install LuckPerms and manage permissions there)."), false);
                }
            }
            return 1;
        } catch (Exception e) {
            LOGGER.error("Unexpected error in addGroupPermission command", e);
            ctx.getSource().sendFailure(MessageUtil.error("§cAn unexpected error occurred: " + e.getMessage()));
            return 0;
        }
    }

    private static int removeGroupPermission(CommandContext<CommandSourceStack> ctx) {
        try {
            // Validate admin permission for modifying group permissions
            PermissionValidator.PermissionResult permResult = 
                PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.group.permissions");
            if (!permResult.hasPermission()) {
                ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                return 0;
            }
            
            String groupName = StringArgumentType.getString(ctx, "group");
            String perm = StringArgumentType.getString(ctx, "permission").toLowerCase().trim();
            
            LOGGER.debug("Removing permission '{}' from group '{}'", perm, groupName);
            
            PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
            if (group == null) {
                ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found", groupName));
                return 0;
            }
            
            // Check if permission exists before removing
            if (!group.getPermissions().contains(perm)) {
                ctx.getSource().sendFailure(MessageUtil.warning("commands.neoessentials.permissions.permission_not_found", perm, groupName));
                return 0;
            }
            
            group.removePermission(perm);
            
            // Clear permission cache after modification
            PermissionAPI.getManager().clearCache();
            
            try { 
                PermissionStorage.save(PermissionAPI.getManager()); 
                LOGGER.info("Removed permission '{}' from group '{}'", perm, groupName);
                PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_PERM_REMOVED, groupName, "node=" + perm);
            } catch (Exception e) { 
                LOGGER.error("Failed to save permissions after removing group permission", e); 
                ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed"));
                return 0;
            }
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.permission_removed", perm, groupName), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Unexpected error in removeGroupPermission command for group '{}', permission '{}'",
                StringArgumentType.getString(ctx, "group"),
                StringArgumentType.getString(ctx, "permission"), e);
            ctx.getSource().sendFailure(MessageUtil.error("§cAn unexpected error occurred: " + e.getMessage()));
            return 0;
        }
    }

    private static int setUserGroup(CommandContext<CommandSourceStack> ctx) {
        try {
            // Validate admin permission for modifying user groups
            PermissionValidator.PermissionResult permResult = 
                PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.user.groups");
            if (!permResult.hasPermission()) {
                ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                return 0;
            }
            
            String playerName = StringArgumentType.getString(ctx, "player");
            String groupName = StringArgumentType.getString(ctx, "group");
            MinecraftServer server = ctx.getSource().getServer();
            
            LOGGER.debug("Setting group '{}' for user '{}'", groupName, playerName);
            
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
                ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found", groupName));
                return 0;
            }
            
            // Check if user is already in this group
            if (groupName.equalsIgnoreCase(user.getGroup())) {
                ctx.getSource().sendFailure(MessageUtil.warning("commands.neoessentials.permissions.user_already_in_group", playerName, groupName));
                return 0;
            }
            
            user.setGroup(groupName);
            
            // Clear permission cache after modification
            PermissionAPI.getManager().clearCache();
            
            try { 
                PermissionStorage.save(PermissionAPI.getManager()); 
                LOGGER.info("Set group '{}' for user '{}'", groupName, playerName);
                PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.USER_GROUP_SET, playerName, "group=" + groupName);
            } catch (Exception e) { 
                LOGGER.error("Failed to save permissions after setting user group", e); 
                ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed"));
                return 0;
            }
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.user_group_set", playerName, groupName), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Unexpected error in setUserGroup command for player '{}', group '{}'",
                StringArgumentType.getString(ctx, "player"),
                StringArgumentType.getString(ctx, "group"), e);
            ctx.getSource().sendFailure(MessageUtil.error("§cAn unexpected error occurred: " + e.getMessage()));
            return 0;
        }
    }

    private static int addUserPermission(CommandContext<CommandSourceStack> ctx) {
        // Validate admin permission for modifying user permissions
        PermissionValidator.PermissionResult permResult = 
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.user.permissions");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }
        
        String playerName = StringArgumentType.getString(ctx, "player");
        String perm = StringArgumentType.getString(ctx, "permission").toLowerCase().trim();
        MinecraftServer server = ctx.getSource().getServer();
        
        // Validate permission format
        if (!PermissionManager.isValidPermission(perm)) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.invalid_permission", perm));
            return 0;
        }
        
        // Try to get UUID by player name
        Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(server, playerName);
        if (uuidOpt.isEmpty()) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.player_not_found"));
            return 0;
        }
        
        UUID uuid = uuidOpt.get();
        PermissionUser user = PermissionAPI.getManager().getUser(uuid);
        if (user == null) {
            // Create user if doesn't exist with default group
            String defaultGroup = PermissionAPI.getManager().getDefaultGroup();
            user = new PermissionUser(uuid, defaultGroup);
            PermissionAPI.getManager().addUser(user);
        }
        
        // Check if permission already exists
        if (user.getPermissions().contains(perm)) {
            ctx.getSource().sendFailure(MessageUtil.warning("commands.neoessentials.permissions.permission_already_exists_for_user", perm, playerName));
            return 0;
        }
        
        user.addPermission(perm);
        
        // Clear permission cache after modification
        PermissionAPI.getManager().clearCache();
        
        try { 
            PermissionStorage.save(PermissionAPI.getManager()); 
            LOGGER.info("Added permission '{}' to user '{}'", perm, playerName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.USER_PERM_ADDED, playerName, "node=" + perm);
        } catch (Exception e) { 
            LOGGER.error("Failed to save permissions after adding user permission", e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.permission_added_to_user", perm, playerName), false);
        return 1;
    }

    private static int removeUserPermission(CommandContext<CommandSourceStack> ctx) {
        // Validate admin permission for modifying user permissions
        PermissionValidator.PermissionResult permResult = 
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.user.permissions");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }
        
        String playerName = StringArgumentType.getString(ctx, "player");
        String perm = StringArgumentType.getString(ctx, "permission").toLowerCase().trim();
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
        
        // Check if permission exists before removing
        if (!user.getPermissions().contains(perm)) {
            ctx.getSource().sendFailure(MessageUtil.warning("commands.neoessentials.permissions.permission_not_found_for_user", perm, playerName));
            return 0;
        }
        
        user.removePermission(perm);
        
        // Clear permission cache after modification
        PermissionAPI.getManager().clearCache();
        
        try { 
            PermissionStorage.save(PermissionAPI.getManager()); 
            LOGGER.info("Removed permission '{}' from user '{}'", perm, playerName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.USER_PERM_REMOVED, playerName, "node=" + perm);
        } catch (Exception e) { 
            LOGGER.error("Failed to save permissions after removing user permission", e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.permission_removed_from_user", perm, playerName), false);
        return 1;
    }
    
    private static int listGroups(CommandContext<CommandSourceStack> ctx) {
        // Validate permission for viewing groups
        PermissionValidator.PermissionResult permResult = 
            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.permissions.list.groups");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }
        
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
            // "group_entry" template is "§7- §f{0} §7(priority: {1})" — only 2 placeholders
            // (name, priority). Prefix/suffix are shown on a separate line below since they're
            // admin-authored &-code/hex/gradient strings that need RichTextFormatter, not a
            // translation-template arg (which would show the raw "&7"/"<gradient:...>" text).
            ctx.getSource().sendSuccess(() -> MessageUtil.component("commands.neoessentials.permissions.group_entry",
                group.getName(), group.getPriority()), false);
            if ((group.getPrefix() != null && !group.getPrefix().isEmpty())
                || (group.getSuffix() != null && !group.getSuffix().isEmpty())) {
                ctx.getSource().sendSuccess(() -> Component.literal("    Prefix: ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(group.getPrefix() != null && !group.getPrefix().isEmpty()
                        ? RichTextFormatter.processTablistText(group.getPrefix()) : Component.literal("none"))
                    .append(Component.literal("  Suffix: ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(group.getSuffix() != null && !group.getSuffix().isEmpty()
                        ? RichTextFormatter.processTablistText(group.getSuffix()) : Component.literal("none")), false);
            }
        }
        return 1;
    }
    
    private static int listUsers(CommandContext<CommandSourceStack> ctx) {
        // Validate permission for viewing users
        PermissionValidator.PermissionResult permResult = 
            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.permissions.list.users");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }
        
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
            
            String group = user.getGroup() != null ? user.getGroup() : MessageUtil.localize("commands.neoessentials.permissions.default");
            ctx.getSource().sendSuccess(() -> MessageUtil.component("commands.neoessentials.permissions.user_entry", userDisplay, group), false);
        }
        return 1;
    }

    // ========== NEW COMMANDS ==========

    private static int showGroupInfo(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.permissions.info.group");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }

        String groupName = StringArgumentType.getString(ctx, "group");
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found", groupName));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> MessageUtil.info("=== Group: " + group.getName() + " ==="), false);
        // Prefix/suffix are admin-authored &-code/hex/gradient strings, not translation keys —
        // route them through RichTextFormatter (always processes rich text, like tablist/holograms)
        // instead of MessageUtil.info(), which would wrap them in Component.literal() unconverted
        // and show the raw "&7"/"<gradient:...>" text instead of the actual colors.
        ctx.getSource().sendSuccess(() -> Component.literal("Prefix: ").withStyle(ChatFormatting.AQUA)
            .append(group.getPrefix() != null ? RichTextFormatter.processTablistText(group.getPrefix()) : Component.literal("None")), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Suffix: ").withStyle(ChatFormatting.AQUA)
            .append(group.getSuffix() != null ? RichTextFormatter.processTablistText(group.getSuffix()) : Component.literal("None")), false);
        ctx.getSource().sendSuccess(() -> MessageUtil.info("Priority: " + group.getPriority()), false);
        ctx.getSource().sendSuccess(() -> MessageUtil.info("Permissions (" + group.getPermissions().size() + "):"), false);

        if (group.getPermissions().isEmpty()) {
            ctx.getSource().sendSuccess(() -> MessageUtil.info("  - No permissions"), false);
        } else {
            group.getPermissions().stream().limit(10).forEach(perm ->
                ctx.getSource().sendSuccess(() -> MessageUtil.info("  - " + perm), false));
            if (group.getPermissions().size() > 10) {
                ctx.getSource().sendSuccess(() -> MessageUtil.info("  ... and " + (group.getPermissions().size() - 10) + " more"), false);
            }
        }

        ctx.getSource().sendSuccess(() -> MessageUtil.info("Inherits (" + group.getInherits().size() + "):"), false);
        if (group.getInherits().isEmpty()) {
            ctx.getSource().sendSuccess(() -> MessageUtil.info("  - No inheritance"), false);
        } else {
            group.getInherits().forEach(inherit ->
                ctx.getSource().sendSuccess(() -> MessageUtil.info("  - " + inherit), false));
        }

        return 1;
    }

    private static int showUserInfo(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.permissions.info.user");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }

        String playerName = StringArgumentType.getString(ctx, "player");
        MinecraftServer server = ctx.getSource().getServer();
        Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(server, playerName);

        if (uuidOpt.isEmpty()) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.player_not_found", playerName));
            return 0;
        }

        UUID playerUUID = uuidOpt.get();
        PermissionUser user = PermissionAPI.getManager().getUser(playerUUID);
        if (user == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.user_not_found", playerName));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> MessageUtil.info("=== User: " + playerName + " ==="), false);
        ctx.getSource().sendSuccess(() -> MessageUtil.info("UUID: " + playerUUID), false);
        ctx.getSource().sendSuccess(() -> MessageUtil.info("Group: " + (user.getGroup() != null ? user.getGroup() : "default")), false);
        ctx.getSource().sendSuccess(() -> MessageUtil.info("Direct Permissions (" + user.getPermissions().size() + "):"), false);

        if (user.getPermissions().isEmpty()) {
            ctx.getSource().sendSuccess(() -> MessageUtil.info("  - No direct permissions"), false);
        } else {
            user.getPermissions().stream().limit(10).forEach(perm ->
                ctx.getSource().sendSuccess(() -> MessageUtil.info("  - " + perm), false));
            if (user.getPermissions().size() > 10) {
                ctx.getSource().sendSuccess(() -> MessageUtil.info("  ... and " + (user.getPermissions().size() - 10) + " more"), false);
            }
        }

        return 1;
    }

    private static int checkUserPermission(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.permissions.check");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }

        String playerName = StringArgumentType.getString(ctx, "player");
        String permission = StringArgumentType.getString(ctx, "permission");
        MinecraftServer server = ctx.getSource().getServer();
        Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(server, playerName);

        if (uuidOpt.isEmpty()) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.player_not_found", playerName));
            return 0;
        }

        UUID playerUUID = uuidOpt.get();
        // Use the full PermissionAPI chain (OP-bypass → external adapter → internal manager
        // → vanillaOpFallback) so the result matches what the player actually experiences.
        boolean hasPermission = PermissionAPI.hasPermission(playerUUID, permission);

        if (hasPermission) {
            ctx.getSource().sendSuccess(() -> MessageUtil.success("✓ " + playerName + " has permission: " + permission), false);
        } else {
            ctx.getSource().sendSuccess(() -> MessageUtil.error("✗ " + playerName + " does NOT have permission: " + permission), false);
        }

        return 1;
    }

    private static int checkGroupPermission(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.permissions.check");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }

        String groupName = StringArgumentType.getString(ctx, "group");
        String permission = StringArgumentType.getString(ctx, "permission");
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);

        if (group == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found", groupName));
            return 0;
        }

        boolean hasPermission = group.getPermissions().contains(permission.toLowerCase());

        if (hasPermission) {
            ctx.getSource().sendSuccess(() -> MessageUtil.success("✓ Group '" + groupName + "' has permission: " + permission), false);
        } else {
            ctx.getSource().sendSuccess(() -> MessageUtil.error("✗ Group '" + groupName + "' does NOT have permission: " + permission), false);
        }

        return 1;
    }

    private static int searchPermissions(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.permissions.search");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }

        String pattern = StringArgumentType.getString(ctx, "pattern").toLowerCase();

        try {
            java.util.List<String> allPermissions =
                com.zerog.neoessentials.api.permissions.external.ExternalPermissionProvider.getAllNeoEssentialsPermissions();

            java.util.List<String> matches = allPermissions.stream()
                .filter(perm -> perm.toLowerCase().contains(pattern))
                .sorted()
                .toList();

            if (matches.isEmpty()) {
                ctx.getSource().sendSuccess(() -> MessageUtil.info("No permissions found matching: " + pattern), false);
                return 1;
            }

            ctx.getSource().sendSuccess(() -> MessageUtil.success("Found " + matches.size() + " permissions matching '" + pattern + "':"), false);
            matches.stream().limit(20).forEach(perm ->
                ctx.getSource().sendSuccess(() -> MessageUtil.info("  - " + perm), false));

            if (matches.size() > 20) {
                ctx.getSource().sendSuccess(() -> MessageUtil.info("  ... and " + (matches.size() - 20) + " more"), false);
            }

            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(MessageUtil.error("Failed to search permissions: " + e.getMessage()));
            return 0;
        }
    }

    private static int createGroup(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.group.create");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }

        String groupName = StringArgumentType.getString(ctx, "group");
        PermissionManager manager = PermissionAPI.getManager();

        if (manager.getGroup(groupName) != null) {
            ctx.getSource().sendFailure(MessageUtil.error("Group '" + groupName + "' already exists!"));
            return 0;
        }

        PermissionGroup newGroup = new PermissionGroup(groupName);
        manager.addGroup(newGroup);
        manager.clearCache();

        try {
            PermissionStorage.save(manager);
            ctx.getSource().sendSuccess(() -> MessageUtil.success("Created group: " + groupName), false);
            LOGGER.info("Created new permission group: {}", groupName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_CREATED, groupName, "new group");
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to save permissions after creating group", e);
            ctx.getSource().sendFailure(MessageUtil.error("Failed to save: " + e.getMessage()));
            return 0;
        }
    }

    private static int deleteGroup(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.group.delete");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }

        String groupName = StringArgumentType.getString(ctx, "group");
        PermissionManager manager = PermissionAPI.getManager();

        if (manager.getGroup(groupName) == null) {
            ctx.getSource().sendFailure(MessageUtil.error("Group '" + groupName + "' does not exist!"));
            return 0;
        }

        // Prevent deleting default group
        if (groupName.equalsIgnoreCase(manager.getDefaultGroup())) {
            ctx.getSource().sendFailure(MessageUtil.error("Cannot delete the default group!"));
            return 0;
        }

        manager.getGroups().removeIf(g -> g.getName().equalsIgnoreCase(groupName));
        manager.clearCache();

        try {
            PermissionStorage.save(manager);
            ctx.getSource().sendSuccess(() -> MessageUtil.success("Deleted group: " + groupName), false);
            LOGGER.info("Deleted permission group: {}", groupName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_DELETED, groupName, "group deleted");
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to save permissions after deleting group", e);
            ctx.getSource().sendFailure(MessageUtil.error("Failed to save: " + e.getMessage()));
            return 0;
        }
    }

    private static int renameGroup(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.group.rename");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }

        String oldName = StringArgumentType.getString(ctx, "oldName");
        String newName = StringArgumentType.getString(ctx, "newName");
        PermissionManager manager = PermissionAPI.getManager();

        PermissionGroup oldGroup = manager.getGroup(oldName);
        if (oldGroup == null) {
            ctx.getSource().sendFailure(MessageUtil.error("Group '" + oldName + "' does not exist!"));
            return 0;
        }

        if (manager.getGroup(newName) != null) {
            ctx.getSource().sendFailure(MessageUtil.error("Group '" + newName + "' already exists!"));
            return 0;
        }

        // Create new group with new name and copy data
        PermissionGroup newGroup = new PermissionGroup(newName);
        newGroup.setPrefix(oldGroup.getPrefix());
        newGroup.setSuffix(oldGroup.getSuffix());
        oldGroup.getPermissions().forEach(newGroup::addPermission);
        oldGroup.getInherits().forEach(newGroup::addInheritance);

        // Remove old group and add new one
        manager.getGroups().removeIf(g -> g.getName().equalsIgnoreCase(oldName));
        manager.addGroup(newGroup);

        // Update users with old group to new group
        manager.getUsers().stream()
            .filter(u -> oldName.equalsIgnoreCase(u.getGroup()))
            .forEach(u -> u.setGroup(newName));

        manager.clearCache();

        try {
            PermissionStorage.save(manager);
            ctx.getSource().sendSuccess(() -> MessageUtil.success("Renamed group '" + oldName + "' to '" + newName + "'"), false);
            LOGGER.info("Renamed permission group '{}' to '{}'", oldName, newName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_RENAMED, oldName, "newName=" + newName);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to save permissions after renaming group", e);
            ctx.getSource().sendFailure(MessageUtil.error("Failed to save: " + e.getMessage()));
            return 0;
        }
    }

    private static int cloneGroup(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.group.clone");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }

        String sourceName = StringArgumentType.getString(ctx, "source");
        String newName = StringArgumentType.getString(ctx, "newGroup");
        PermissionManager manager = PermissionAPI.getManager();

        PermissionGroup sourceGroup = manager.getGroup(sourceName);
        if (sourceGroup == null) {
            ctx.getSource().sendFailure(MessageUtil.error("Group '" + sourceName + "' does not exist!"));
            return 0;
        }

        if (manager.getGroup(newName) != null) {
            ctx.getSource().sendFailure(MessageUtil.error("Group '" + newName + "' already exists!"));
            return 0;
        }

        // Create new group and copy all data
        PermissionGroup newGroup = new PermissionGroup(newName);
        newGroup.setPrefix(sourceGroup.getPrefix());
        newGroup.setSuffix(sourceGroup.getSuffix());
        sourceGroup.getPermissions().forEach(newGroup::addPermission);
        sourceGroup.getInherits().forEach(newGroup::addInheritance);

        manager.addGroup(newGroup);
        manager.clearCache();

        try {
            PermissionStorage.save(manager);
            ctx.getSource().sendSuccess(() -> MessageUtil.success("Cloned group '" + sourceName + "' to '" + newName + "'"), false);
            LOGGER.info("Cloned permission group '{}' to '{}'", sourceName, newName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_CLONED, sourceName, "newGroup=" + newName);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to save permissions after cloning group", e);
            ctx.getSource().sendFailure(MessageUtil.error("Failed to save: " + e.getMessage()));
            return 0;
        }
    }

    private static int clearGroupPermissions(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.group.clear");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }

        String groupName = StringArgumentType.getString(ctx, "group");
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);

        if (group == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found", groupName));
            return 0;
        }

        int count = group.getPermissions().size();
        group.getPermissions().clear();
        PermissionAPI.getManager().clearCache();

        try {
            PermissionStorage.save(PermissionAPI.getManager());
            ctx.getSource().sendSuccess(() -> MessageUtil.success("Cleared " + count + " permissions from group: " + groupName), false);
            LOGGER.info("Cleared all permissions from group '{}'", groupName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_PERMS_CLEARED, groupName, "all nodes removed");
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to save permissions after clearing group", e);
            ctx.getSource().sendFailure(MessageUtil.error("Failed to save: " + e.getMessage()));
            return 0;
        }
    }

    private static int clearUserPermissions(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.user.clear");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }

        String playerName = StringArgumentType.getString(ctx, "player");
        MinecraftServer server = ctx.getSource().getServer();
        Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(server, playerName);

        if (uuidOpt.isEmpty()) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.player_not_found", playerName));
            return 0;
        }

        UUID playerUUID = uuidOpt.get();
        PermissionUser user = PermissionAPI.getManager().getUser(playerUUID);
        if (user == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.user_not_found", playerName));
            return 0;
        }

        int count = user.getPermissions().size();
        user.getPermissions().clear();
        PermissionAPI.getManager().clearCache();

        try {
            PermissionStorage.save(PermissionAPI.getManager());
            ctx.getSource().sendSuccess(() -> MessageUtil.success("Cleared " + count + " permissions from user: " + playerName), false);
            LOGGER.info("Cleared all permissions from user '{}'", playerName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.USER_PERMS_CLEARED, playerName, "all direct nodes removed");
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to save permissions after clearing user", e);
            ctx.getSource().sendFailure(MessageUtil.error("Failed to save: " + e.getMessage()));
            return 0;
        }
    }

    private static int addGroupInheritance(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.group.inherit");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }

        String groupName = StringArgumentType.getString(ctx, "group");
        String inheritGroup = StringArgumentType.getString(ctx, "inheritGroup");
        PermissionManager manager = PermissionAPI.getManager();

        PermissionGroup group = manager.getGroup(groupName);
        if (group == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found", groupName));
            return 0;
        }

        PermissionGroup targetGroup = manager.getGroup(inheritGroup);
        if (targetGroup == null) {
            ctx.getSource().sendFailure(MessageUtil.error("Inherit group '" + inheritGroup + "' does not exist!"));
            return 0;
        }

        if (groupName.equalsIgnoreCase(inheritGroup)) {
            ctx.getSource().sendFailure(MessageUtil.error("A group cannot inherit from itself!"));
            return 0;
        }

        if (group.getInherits().contains(inheritGroup)) {
            ctx.getSource().sendFailure(MessageUtil.error("Group '" + groupName + "' already inherits from '" + inheritGroup + "'!"));
            return 0;
        }

        group.addInheritance(inheritGroup);
        manager.clearCache();

        try {
            PermissionStorage.save(manager);
            ctx.getSource().sendSuccess(() -> MessageUtil.success("Group '" + groupName + "' now inherits from '" + inheritGroup + "'"), false);
            LOGGER.info("Added inheritance from '{}' to group '{}'", inheritGroup, groupName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_INHERIT_ADDED, groupName, "inherits=" + inheritGroup);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to save permissions after adding inheritance", e);
            ctx.getSource().sendFailure(MessageUtil.error("Failed to save: " + e.getMessage()));
            return 0;
        }
    }

    private static int removeGroupInheritance(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.group.inherit");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }

        String groupName = StringArgumentType.getString(ctx, "group");
        String inheritGroup = StringArgumentType.getString(ctx, "inheritGroup");
        PermissionManager manager = PermissionAPI.getManager();

        PermissionGroup group = manager.getGroup(groupName);
        if (group == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found", groupName));
            return 0;
        }

        if (!group.getInherits().contains(inheritGroup)) {
            ctx.getSource().sendFailure(MessageUtil.error("Group '" + groupName + "' does not inherit from '" + inheritGroup + "'!"));
            return 0;
        }

        group.removeInheritance(inheritGroup);
        manager.clearCache();

        try {
            PermissionStorage.save(manager);
            ctx.getSource().sendSuccess(() -> MessageUtil.success("Removed inheritance of '" + inheritGroup + "' from group '" + groupName + "'"), false);
            LOGGER.info("Removed inheritance from '{}' from group '{}'", inheritGroup, groupName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_INHERIT_REMOVED, groupName, "removed=" + inheritGroup);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to save permissions after removing inheritance", e);
            ctx.getSource().sendFailure(MessageUtil.error("Failed to save: " + e.getMessage()));
            return 0;
        }
    }

    // ── Temporary permissions — user ─────────────────────────────────────────

    private static int addUserTempPermission(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.user.temp");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }
        String playerName = StringArgumentType.getString(ctx, "player");
        String perm  = StringArgumentType.getString(ctx, "permission").toLowerCase().trim();
        String durStr = StringArgumentType.getString(ctx, "duration");

        if (!PermissionManager.isValidPermission(perm)) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.invalid_permission", perm));
            return 0;
        }
        long durationMs;
        try { durationMs = PermissionManager.parseDurationMs(durStr); }
        catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(MessageUtil.error("Invalid duration: " + e.getMessage()));
            return 0;
        }
        MinecraftServer server = ctx.getSource().getServer();
        Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(server, playerName);
        if (uuidOpt.isEmpty()) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.player_not_found", playerName));
            return 0;
        }
        PermissionUser user = PermissionAPI.getManager().getUser(uuidOpt.get());
        long expiryMs = System.currentTimeMillis() + durationMs;
        user.addTempPermission(perm, expiryMs);
        PermissionAPI.getManager().clearCache();
        try {
            PermissionStorage.save(PermissionAPI.getManager());
            String remaining = PermissionManager.formatDuration(durationMs);
            LOGGER.info("Granted temp permission '{}' to {} for {}", perm, playerName, remaining);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.USER_TEMP_PERM_ADDED,
                playerName, "node=" + perm + " expires_in=" + remaining);
            final String displayPerm = perm;
            ctx.getSource().sendSuccess(() -> MessageUtil.success(
                "Granted temporary permission §f" + displayPerm + "§a to §f" + playerName
                + "§a for §f" + remaining + "§a."), false);
            // Notify the target if online
            net.minecraft.server.level.ServerPlayer onlineTarget = server.getPlayerList().getPlayer(uuidOpt.get());
            if (onlineTarget != null) {
                onlineTarget.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§eYou have been granted temporary permission §f" + perm + "§e for §f" + remaining + "§e."));
            }
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to save after addtemp", e);
            ctx.getSource().sendFailure(MessageUtil.error("Failed to save: " + e.getMessage()));
            return 0;
        }
    }

    private static int removeUserTempPermission(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.user.temp");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }
        String playerName = StringArgumentType.getString(ctx, "player");
        String perm = StringArgumentType.getString(ctx, "permission").toLowerCase().trim();
        MinecraftServer server = ctx.getSource().getServer();
        Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(server, playerName);
        if (uuidOpt.isEmpty()) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.player_not_found", playerName));
            return 0;
        }
        PermissionUser user = PermissionAPI.getManager().getUser(uuidOpt.get());
        if (!user.getTempPermissions().containsKey(perm)) {
            ctx.getSource().sendFailure(MessageUtil.error(
                "Player §f" + playerName + "§c has no temporary permission §f" + perm + "§c."));
            return 0;
        }
        user.removeTempPermission(perm);
        PermissionAPI.getManager().clearCache();
        try {
            PermissionStorage.save(PermissionAPI.getManager());
            LOGGER.info("Removed temp permission '{}' from {}", perm, playerName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.USER_TEMP_PERM_REMOVED,
                playerName, "node=" + perm);
            final String displayPerm = perm;
            ctx.getSource().sendSuccess(() -> MessageUtil.success(
                "Removed temporary permission §f" + displayPerm + "§a from §f" + playerName + "§a."), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(MessageUtil.error("Failed to save: " + e.getMessage()));
            return 0;
        }
    }

    private static int listUserTempPermissions(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.permissions.info.user");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }
        String playerName = StringArgumentType.getString(ctx, "player");
        Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(ctx.getSource().getServer(), playerName);
        if (uuidOpt.isEmpty()) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.player_not_found", playerName));
            return 0;
        }
        PermissionUser user = PermissionAPI.getManager().getUser(uuidOpt.get());
        java.util.Map<String, Long> temps = user.getTempPermissions();
        if (temps.isEmpty()) {
            send(ctx, "§7Player §f" + playerName + "§7 has no temporary permissions.");
            return 1;
        }
        long now = System.currentTimeMillis();
        send(ctx, "§8━━ §bTemp Permissions: §f" + playerName + " §8━━");
        temps.entrySet().stream()
            .sorted(java.util.Map.Entry.comparingByKey())
            .forEach(e -> {
                long remaining = e.getValue() - now;
                String timeStr = remaining > 0
                    ? "§a" + PermissionManager.formatDuration(remaining)
                    : "§cexpired";
                send(ctx, "§f  " + e.getKey() + " §8— expires in " + timeStr);
            });
        return 1;
    }

    // ── Temporary permissions — group ────────────────────────────────────────

    private static int addGroupTempPermission(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.group.temp");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }
        String groupName = StringArgumentType.getString(ctx, "group");
        String perm   = StringArgumentType.getString(ctx, "permission").toLowerCase().trim();
        String durStr = StringArgumentType.getString(ctx, "duration");

        if (!PermissionManager.isValidPermission(perm)) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.invalid_permission", perm));
            return 0;
        }
        long durationMs;
        try { durationMs = PermissionManager.parseDurationMs(durStr); }
        catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(MessageUtil.error("Invalid duration: " + e.getMessage()));
            return 0;
        }
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found", groupName));
            return 0;
        }
        long expiryMs = System.currentTimeMillis() + durationMs;
        group.addTempPermission(perm, expiryMs);
        PermissionAPI.getManager().clearCache();
        try {
            PermissionStorage.save(PermissionAPI.getManager());
            String remaining = PermissionManager.formatDuration(durationMs);
            LOGGER.info("Granted temp permission '{}' to group '{}' for {}", perm, groupName, remaining);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_TEMP_PERM_ADDED,
                groupName, "node=" + perm + " expires_in=" + remaining);
            final String displayPerm = perm;
            ctx.getSource().sendSuccess(() -> MessageUtil.success(
                "Granted temporary permission §f" + displayPerm + "§a to group §f" + groupName
                + "§a for §f" + remaining + "§a."), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to save after group addtemp", e);
            ctx.getSource().sendFailure(MessageUtil.error("Failed to save: " + e.getMessage()));
            return 0;
        }
    }

    private static int removeGroupTempPermission(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.group.temp");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }
        String groupName = StringArgumentType.getString(ctx, "group");
        String perm = StringArgumentType.getString(ctx, "permission").toLowerCase().trim();
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found", groupName));
            return 0;
        }
        if (!group.getTempPermissions().containsKey(perm)) {
            ctx.getSource().sendFailure(MessageUtil.error(
                "Group §f" + groupName + "§c has no temporary permission §f" + perm + "§c."));
            return 0;
        }
        group.removeTempPermission(perm);
        PermissionAPI.getManager().clearCache();
        try {
            PermissionStorage.save(PermissionAPI.getManager());
            LOGGER.info("Removed temp permission '{}' from group '{}'", perm, groupName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_TEMP_PERM_REMOVED,
                groupName, "node=" + perm);
            final String displayPerm = perm;
            ctx.getSource().sendSuccess(() -> MessageUtil.success(
                "Removed temporary permission §f" + displayPerm + "§a from group §f" + groupName + "§a."), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(MessageUtil.error("Failed to save: " + e.getMessage()));
            return 0;
        }
    }

    private static int listGroupTempPermissions(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.permissions.info.group");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }
        String groupName = StringArgumentType.getString(ctx, "group");
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found", groupName));
            return 0;
        }
        java.util.Map<String, Long> temps = group.getTempPermissions();
        if (temps.isEmpty()) {
            send(ctx, "§7Group §f" + groupName + "§7 has no temporary permissions.");
            return 1;
        }
        long now = System.currentTimeMillis();
        send(ctx, "§8━━ §bTemp Permissions: group §f" + groupName + " §8━━");
        temps.entrySet().stream()
            .sorted(java.util.Map.Entry.comparingByKey())
            .forEach(e -> {
                long remaining = e.getValue() - now;
                String timeStr = remaining > 0
                    ? "§a" + PermissionManager.formatDuration(remaining)
                    : "§cexpired";
                send(ctx, "§f  " + e.getKey() + " §8— expires in " + timeStr);
            });
        return 1;
    }

    // ── Permission Debug ──────────────────────────────────────────────────────

    /**
     * /permissions debug <player>
     *
     * <p>Prints a full permission-resolution trace for the named player so that
     * administrators can understand exactly why a player does or does not have a
     * given permission. Covers:
     * <ul>
     *   <li>Active permission system mode (internal / external / emergency)</li>
     *   <li>Adapter health and version (when external)</li>
     *   <li>Current config flags: opsBypassPermissions, vanillaOpFallback</li>
     *   <li>Player OP status (level 2+)</li>
     *   <li>Assigned group, direct user permissions</li>
     *   <li>Full group inheritance chain with each group's permissions</li>
     *   <li>A summary of which step in the chain would grant/deny for this player</li>
     * </ul>
     */
    private static int debugPlayerPermissions(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult permResult =
            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.permissions.debug");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }

        String playerName = StringArgumentType.getString(ctx, "player");
        MinecraftServer server = ctx.getSource().getServer();
        Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(server, playerName);
        if (uuidOpt.isEmpty()) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.player_not_found", playerName));
            return 0;
        }
        UUID uuid = uuidOpt.get();

        // ── Header ──────────────────────────────────────────────────────────
        send(ctx, "§8━━━━━━━━━ §bPermission Debug: §f" + playerName + " §8━━━━━━━━━");

        // ── System state ─────────────────────────────────────────────────────
        var cfg = com.zerog.neoessentials.config.ConfigManager.getInstance();
        boolean emergencyMode  = PermissionAPI.isEmergencyMode();
        boolean usingExternal  = PermissionAPI.isUsingExternal();
        var externalAdapter    = PermissionAPI.getExternalAdapter();

        String modeLabel = emergencyMode ? "§cEMERGENCY (OP-only fallback)"
                         : usingExternal && externalAdapter != null ? "§a" + externalAdapter.getName()
                         : "§eInternal permissions.json";
        send(ctx, "§7System mode    : " + modeLabel);

        if (usingExternal && externalAdapter != null) {
            String healthLabel = externalAdapter.isHealthy()
                    ? "§a✓ healthy"
                    : "§c✗ UNHEALTHY (" + externalAdapter.getConsecutiveFailures() + " failures)";
            send(ctx, "§7Adapter health : " + healthLabel);
            send(ctx, "§7Adapter version: §f" + externalAdapter.getVersion());
        }

        send(ctx, "§7opsBypassPermissions : §f" + cfg.isOpsBypassPermissionsEnabled());
        send(ctx, "§7vanillaOpFallback    : §f" + cfg.isVanillaOpFallbackEnabled());

        // ── OP status ────────────────────────────────────────────────────────
        boolean isOp = false;
        try {
            ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(uuid);
            if (onlinePlayer != null) {
                isOp = onlinePlayer.hasPermissions(2);
            } else {
                var profileCache = server.getProfileCache();
                if (profileCache != null) {
                    var profile = profileCache.get(uuid).orElse(null);
                    if (profile != null) isOp = server.getPlayerList().isOp(profile);
                }
            }
        } catch (Exception ignored) {}
        send(ctx, "§7OP (level 2+)        : " + (isOp ? "§aYes" : "§cNo"));

        // ── Internal-system details ──────────────────────────────────────────
        PermissionManager manager = PermissionAPI.getManager();
        if (manager != null) {
            PermissionUser user = manager.getUser(uuid);
            String groupName = (user != null && user.getGroup() != null)
                    ? user.getGroup() : manager.getDefaultGroup();
            send(ctx, "§7Assigned group       : §f" + groupName);

            // Direct user permissions
            if (user != null && !user.getPermissions().isEmpty()) {
                send(ctx, "§7Direct user permissions (§f" + user.getPermissions().size() + "§7):");
                int count = 0;
                for (String perm : user.getPermissions()) {
                    if (count++ >= 10) {
                        send(ctx, "§7  §8... " + (user.getPermissions().size() - 10) + " more");
                        break;
                    }
                    send(ctx, "§7  " + (perm.startsWith("-") ? "§c" : "§f") + perm);
                }
            } else {
                send(ctx, "§7Direct user permissions: §8none");
            }

            // Group chain
            send(ctx, "§7Group chain:");
            showGroupChain(ctx, manager, groupName, new java.util.LinkedHashSet<>(), 1);
        } else {
            send(ctx, "§7Internal manager: §cnot loaded");
        }

        // ── Resolution chain summary ─────────────────────────────────────────
        send(ctx, "§8--- §7Resolution chain for this player §8---");
        if (emergencyMode) {
            send(ctx, (isOp ? "§a[1] EMERGENCY MODE + OP → GRANT" : "§c[1] EMERGENCY MODE, not OP → DENY"));
        } else {
            if (cfg.isOpsBypassPermissionsEnabled()) {
                send(ctx, isOp
                    ? "§a[1] opsBypassPermissions: OP → GRANT (node never checked)"
                    : "§8[1] opsBypassPermissions: not OP, continues...");
            } else {
                send(ctx, "§8[1] opsBypassPermissions: disabled");
            }
            if (usingExternal && externalAdapter != null) {
                String adapterStatus = externalAdapter.isHealthy()
                        ? "§e[2] " + externalAdapter.getName() + ": checks specific node"
                        : "§c[2] " + externalAdapter.getName() + " UNHEALTHY → falls through to internal";
                send(ctx, adapterStatus);
            } else {
                send(ctx, "§8[2] External adapter: not configured");
            }
            send(ctx, "§e[3] Internal manager: group / user / wildcard check");
            if (cfg.isVanillaOpFallbackEnabled()) {
                send(ctx, isOp
                    ? "§a[4] vanillaOpFallback: OP → GRANT (if all above returned false)"
                    : "§8[4] vanillaOpFallback: not OP → no effect");
            } else {
                send(ctx, "§8[4] vanillaOpFallback: disabled");
            }
        }
        send(ctx, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        return 1;
    }

    /**
     * Recursively prints a group's permissions and inheritance chain.
     */
    private static void showGroupChain(CommandContext<CommandSourceStack> ctx,
                                       PermissionManager manager,
                                       String groupName,
                                       java.util.Set<String> visited,
                                       int depth) {
        if (groupName == null || visited.contains(groupName.toLowerCase())) return;
        visited.add(groupName.toLowerCase());

        String indent = "  ".repeat(depth);
        PermissionGroup group = manager.getGroup(groupName);
        if (group == null) {
            send(ctx, "§7" + indent + "§cGroup '" + groupName + "' not found");
            return;
        }

        // Legacy &-codes need converting to § before concatenation into this plain-string
        // §-colour tree view (send() wraps the final string in Component.literal() with no
        // further processing) — otherwise "&7" etc. shows up as literal text. Gradient/hex
        // tags aren't expanded here (this is a plain-text tree view, not a rich preview);
        // use `/permissions info <group>` for a fully rendered prefix/suffix.
        String prefixInfo = (group.getPrefix() != null && !group.getPrefix().isEmpty())
                ? " §8(prefix: §r" + group.getPrefix().replaceAll("&([0-9a-fA-Fk-orK-OR])", "§$1") + "§8)" : "";
        send(ctx, "§7" + indent + "§f" + groupName + " §8[" + group.getPermissions().size() + " nodes]" + prefixInfo);

        int count = 0;
        for (String perm : group.getPermissions()) {
            if (count++ >= 8) {
                send(ctx, "§7" + indent + "  §8... " + (group.getPermissions().size() - 8) + " more");
                break;
            }
            send(ctx, "§7" + indent + "  " + (perm.startsWith("-") ? "§c" : "§f") + perm);
        }

        for (String parent : group.getInherits()) {
            send(ctx, "§7" + indent + "  §8↳ inherits §e" + parent + "§8:");
            showGroupChain(ctx, manager, parent, visited, depth + 1);
        }
    }

    /** Sends a plain-text (§-colour) line to the command source. */
    private static void send(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(
                () -> net.minecraft.network.chat.Component.literal(text), false);
    }

    // ── Contextual permission handlers — group ──────────────────────────────

    private static int addGroupContextPermission(CommandContext<CommandSourceStack> ctx, boolean allow) {
        PermissionValidator.PermissionResult pr =
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.group.context");
        if (!pr.hasPermission()) { ctx.getSource().sendFailure(MessageUtil.error(pr.getErrorMessage())); return 0; }
        String groupName = StringArgumentType.getString(ctx, "group");
        String contextKey = StringArgumentType.getString(ctx, "contextKey");
        String permission = StringArgumentType.getString(ctx, "permission");
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) { ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found", groupName)); return 0; }
        group.addContextPermission(contextKey, permission, allow);
        PermissionAPI.getManager().clearCache();
        try { PermissionStorage.save(PermissionAPI.getManager()); } catch (Exception e) {
            ctx.getSource().sendFailure(MessageUtil.error("Failed to save: " + e.getMessage())); return 0; }
        String action = allow ? "§aallow" : "§cdeny";
        PermissionAuditLogger.log(getExecutorDisplay(ctx),
            allow ? PermissionAuditLogger.GROUP_CONTEXT_PERM_ADDED : PermissionAuditLogger.GROUP_CONTEXT_PERM_REMOVED,
            groupName, "context=" + contextKey + " node=" + permission + " value=" + allow);
        ctx.getSource().sendSuccess(() -> MessageUtil.success(
            "Set contextual permission §f" + permission + "§a → " + action + "§a for group §f" + groupName +
            "§a in context §f" + contextKey), false);
        return 1;
    }

    private static int removeGroupContextPermission(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult pr =
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.group.context");
        if (!pr.hasPermission()) { ctx.getSource().sendFailure(MessageUtil.error(pr.getErrorMessage())); return 0; }
        String groupName = StringArgumentType.getString(ctx, "group");
        String contextKey = StringArgumentType.getString(ctx, "contextKey");
        String permission = StringArgumentType.getString(ctx, "permission");
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) { ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found", groupName)); return 0; }
        boolean removed = group.removeContextPermission(contextKey, permission);
        if (!removed) { ctx.getSource().sendFailure(MessageUtil.error("No contextual override found for §f" + permission + "§c in context §f" + contextKey)); return 0; }
        PermissionAPI.getManager().clearCache();
        try { PermissionStorage.save(PermissionAPI.getManager()); } catch (Exception e) {
            ctx.getSource().sendFailure(MessageUtil.error("Failed to save: " + e.getMessage())); return 0; }
        PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_CONTEXT_PERM_REMOVED,
            groupName, "context=" + contextKey + " node=" + permission);
        ctx.getSource().sendSuccess(() -> MessageUtil.success(
            "Removed contextual override for §f" + permission + "§a from group §f" + groupName +
            "§a (context §f" + contextKey + "§a)"), false);
        return 1;
    }

    private static int listGroupContextPermissions(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult pr =
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.group.context");
        if (!pr.hasPermission()) { ctx.getSource().sendFailure(MessageUtil.error(pr.getErrorMessage())); return 0; }
        String groupName = StringArgumentType.getString(ctx, "group");
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) { ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found", groupName)); return 0; }
        java.util.Map<String, java.util.Map<String, Boolean>> ctxPerms = group.getContextualPermissions();
        send(ctx, "§6Contextual permissions for group §f" + groupName + "§6:");
        if (ctxPerms.isEmpty()) { send(ctx, "  §7(none)"); return 1; }
        ctxPerms.forEach((key, nodeMap) -> {
            send(ctx, "  §e" + key + "§7:");
            nodeMap.forEach((node, val) ->
                send(ctx, "    " + (val ? "§a✔ " : "§c✘ ") + "§f" + node));
        });
        return 1;
    }

    // ── Contextual permission handlers — user ───────────────────────────────

    private static int addUserContextPermission(CommandContext<CommandSourceStack> ctx, boolean allow) {
        PermissionValidator.PermissionResult pr =
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.user.context");
        if (!pr.hasPermission()) { ctx.getSource().sendFailure(MessageUtil.error(pr.getErrorMessage())); return 0; }
        String playerName = StringArgumentType.getString(ctx, "player");
        String contextKey = StringArgumentType.getString(ctx, "contextKey");
        String permission = StringArgumentType.getString(ctx, "permission");
        Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(ctx.getSource().getServer(), playerName);
        if (uuidOpt.isEmpty()) { ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.player_not_found", playerName)); return 0; }
        PermissionUser user = PermissionAPI.getManager().getUser(uuidOpt.get());
        user.addContextPermission(contextKey, permission, allow);
        PermissionAPI.getManager().clearCache();
        try { PermissionStorage.save(PermissionAPI.getManager()); } catch (Exception e) {
            ctx.getSource().sendFailure(MessageUtil.error("Failed to save: " + e.getMessage())); return 0; }
        String action = allow ? "§aallow" : "§cdeny";
        PermissionAuditLogger.log(getExecutorDisplay(ctx),
            allow ? PermissionAuditLogger.USER_CONTEXT_PERM_ADDED : PermissionAuditLogger.USER_CONTEXT_PERM_REMOVED,
            playerName, "context=" + contextKey + " node=" + permission + " value=" + allow);
        ctx.getSource().sendSuccess(() -> MessageUtil.success(
            "Set contextual permission §f" + permission + "§a → " + action + "§a for §f" + playerName +
            "§a in context §f" + contextKey), false);
        return 1;
    }

    private static int removeUserContextPermission(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult pr =
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.user.context");
        if (!pr.hasPermission()) { ctx.getSource().sendFailure(MessageUtil.error(pr.getErrorMessage())); return 0; }
        String playerName = StringArgumentType.getString(ctx, "player");
        String contextKey = StringArgumentType.getString(ctx, "contextKey");
        String permission = StringArgumentType.getString(ctx, "permission");
        Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(ctx.getSource().getServer(), playerName);
        if (uuidOpt.isEmpty()) { ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.player_not_found", playerName)); return 0; }
        PermissionUser user = PermissionAPI.getManager().getUser(uuidOpt.get());
        boolean removed = user.removeContextPermission(contextKey, permission);
        if (!removed) { ctx.getSource().sendFailure(MessageUtil.error("No contextual override for §f" + permission + "§c in context §f" + contextKey)); return 0; }
        PermissionAPI.getManager().clearCache();
        try { PermissionStorage.save(PermissionAPI.getManager()); } catch (Exception e) {
            ctx.getSource().sendFailure(MessageUtil.error("Failed to save: " + e.getMessage())); return 0; }
        PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.USER_CONTEXT_PERM_REMOVED,
            playerName, "context=" + contextKey + " node=" + permission);
        ctx.getSource().sendSuccess(() -> MessageUtil.success(
            "Removed contextual override for §f" + permission + "§a from §f" + playerName +
            "§a (context §f" + contextKey + "§a)"), false);
        return 1;
    }

    private static int listUserContextPermissions(CommandContext<CommandSourceStack> ctx) {
        PermissionValidator.PermissionResult pr =
            PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.permissions.user.context");
        if (!pr.hasPermission()) { ctx.getSource().sendFailure(MessageUtil.error(pr.getErrorMessage())); return 0; }
        String playerName = StringArgumentType.getString(ctx, "player");
        Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(ctx.getSource().getServer(), playerName);
        if (uuidOpt.isEmpty()) { ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.player_not_found", playerName)); return 0; }
        PermissionUser user = PermissionAPI.getManager().getUser(uuidOpt.get());
        java.util.Map<String, java.util.Map<String, Boolean>> ctxPerms = user.getContextualPermissions();
        send(ctx, "§6Contextual permissions for §f" + playerName + "§6:");
        if (ctxPerms.isEmpty()) { send(ctx, "  §7(none)"); return 1; }
        ctxPerms.forEach((key, nodeMap) -> {
            send(ctx, "  §e" + key + "§7:");
            nodeMap.forEach((node, val) ->
                send(ctx, "    " + (val ? "§a✔ " : "§c✘ ") + "§f" + node));
        });
        return 1;
    }
}

