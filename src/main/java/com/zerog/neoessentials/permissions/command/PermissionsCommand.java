package com.zerog.neoessentials.permissions.command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
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
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Permissions module is disabled, skipping permissions command registration");
            return;
        }
        
        // Check if individual permissions commands are enabled
        boolean pexEnabled = com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("pex");
        boolean permissionsEnabled = com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("permissions");
        
        if (!pexEnabled && !permissionsEnabled) {
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Both pex and permissions commands are disabled, skipping registration");
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
                            } catch (Exception e) {
                                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Group-name suggestion lookup failed, falling back to static list: {}", e.getMessage());
                            }
                            return SharedSuggestionProvider.suggest(
                                java.util.List.of("admin", "moderator", "player", "vip", "default"),
                                builder);
                        })
                        .executes(ctx -> showGroupInfo(ctx))))
                .then(Commands.literal("user")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                            ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                                .map(p -> p.getGameProfile().name()),
                            builder
                        ))
                        .executes(ctx -> showUserInfo(ctx)))))
            .then(Commands.literal("check")
                .then(Commands.literal("user")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                            ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                                .map(p -> p.getGameProfile().name()),
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
                            } catch (Exception e) {
                                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Group-name suggestion lookup failed, falling back to static list: {}", e.getMessage());
                            }
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
                            } catch (Exception e) {
                                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Group-name suggestion lookup failed, falling back to static list: {}", e.getMessage());
                            }
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
                            } catch (Exception e) {
                                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Group-name suggestion lookup failed, falling back to static list: {}", e.getMessage());
                            }
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
                            } catch (Exception e) {
                                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Group-name suggestion lookup failed, falling back to static list: {}", e.getMessage());
                            }
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
                                    } catch (Exception e) {
                                        NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Group-name suggestion lookup failed, falling back to static list: {}", e.getMessage());
                                    }
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
                                    } catch (Exception e) {
                                        NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Group-name suggestion lookup failed, falling back to static list: {}", e.getMessage());
                                    }
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
                                } catch (Exception e) {
                                    NeoLog.debug(LOGGER, com.zerog.neoessentials.logging.LogCategory.PERMISSIONS,
                                        "Failed to build removetemp group-permission suggestions", e);
                                }
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
                                    } catch (Exception e) {
                                        NeoLog.debug(LOGGER, com.zerog.neoessentials.logging.LogCategory.PERMISSIONS,
                                            "Failed to build context-remove group contextKey suggestions", e);
                                    }
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
                                        } catch (Exception e) {
                                            NeoLog.debug(LOGGER, com.zerog.neoessentials.logging.LogCategory.PERMISSIONS,
                                                "Failed to build context-remove group permission suggestions", e);
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> removeGroupContextPermission(ctx)))))
                        .then(Commands.literal("list")
                            .executes(ctx -> listGroupContextPermissions(ctx))))))
            .then(Commands.literal("user")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                        ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                            .map(p -> p.getGameProfile().name()),
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
                                } catch (Exception e) {
                                    NeoLog.debug(LOGGER, com.zerog.neoessentials.logging.LogCategory.PERMISSIONS,
                                        "Failed to build removetemp user-permission suggestions", e);
                                }
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
                                    } catch (Exception e) {
                                        NeoLog.debug(LOGGER, com.zerog.neoessentials.logging.LogCategory.PERMISSIONS,
                                            "Failed to build context-remove user contextKey suggestions", e);
                                    }
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
                                        } catch (Exception e) {
                                            NeoLog.debug(LOGGER, com.zerog.neoessentials.logging.LogCategory.PERMISSIONS,
                                                "Failed to build context-remove user permission suggestions", e);
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> removeUserContextPermission(ctx)))))
                        .then(Commands.literal("list")
                            .executes(ctx -> listUserContextPermissions(ctx))))))
            .then(Commands.literal("debug")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                        ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                            .map(p -> p.getGameProfile().name()),
                        builder
                    ))
                    .executes(ctx -> debugPlayerPermissions(ctx))));
    }

    /** Returns the player name or "CONSOLE" for non-player sources. */
    private static String getExecutorDisplay(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer p = ctx.getSource().getPlayer();
            return p != null ? p.getGameProfile().name() : "CONSOLE";
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Could not resolve executor player for audit log, defaulting to CONSOLE", e);
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
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to reload permissions", e);
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
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.prefix_too_long"));
            return 0;
        }

        // Validate no dangerous characters (but allow color codes &)
        if (prefix.matches(".*[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F].*")) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.prefix_invalid_chars"));
            return 0;
        }

        // Check for group existence
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found", groupName));
            return 0;
        }

        // Set the prefix
        group.setPrefix(prefix);

        // Clear cache to ensure new prefix is used immediately
        PermissionAPI.getManager().clearCache();

        // Save with proper error handling
        try {
            PermissionStorage.save(PermissionAPI.getManager());
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Set prefix '{}' for group '{}'", prefix, groupName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_PREFIX_SET, groupName, "prefix=\"" + prefix + "\"");
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.prefix_set", groupName, prefix), false);
            return 1;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to save permissions after setting prefix", e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_prefix_failed", e.getMessage()));
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
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.suffix_too_long"));
            return 0;
        }

        // Validate no dangerous characters (but allow color codes &)
        if (suffix.matches(".*[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F].*")) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.suffix_invalid_chars"));
            return 0;
        }

        // Check for group existence
        PermissionGroup group = PermissionAPI.getManager().getGroup(groupName);
        if (group == null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_not_found", groupName));
            return 0;
        }

        // Set the suffix
        group.setSuffix(suffix);

        // Clear cache to ensure new suffix is used immediately
        PermissionAPI.getManager().clearCache();

        // Save with proper error handling
        try {
            PermissionStorage.save(PermissionAPI.getManager());
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Set suffix '{}' for group '{}'", suffix, groupName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_SUFFIX_SET, groupName, "suffix=\"" + suffix + "\"");
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.suffix_set", groupName, suffix), false);
            return 1;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to save permissions after setting suffix", e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_suffix_failed", e.getMessage()));
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
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Set priority {} for group '{}'", priority, groupName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_PRIORITY_SET, groupName, "priority=" + priority);
            ctx.getSource().sendSuccess(() -> MessageUtil.success(
                "commands.neoessentials.permissions.priority_set", groupName, priority), false);
            return 1;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to save permissions after setting priority", e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_priority_failed", e.getMessage()));
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
        final String prioritySuffix = p != 0 ? " §8(higher = checked first in inheritance)" : "";
        ctx.getSource().sendSuccess(() -> MessageUtil.info(
            "commands.neoessentials.permissions.group_priority_info", groupName, p, prioritySuffix), false);
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

            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Adding permission '{}' to group '{}'", perm, groupName);
            
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
                NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Added permission '{}' to group '{}'", perm, groupName);
                PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_PERM_ADDED, groupName, "node=" + perm);
            } catch (Exception e) { 
                NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to save permissions after adding group permission", e);
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
                    NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Could not determine active permission handler while adding external permission '{}'", perm, ex);
                    neoessentialsHandlerActive = false;
                }
                if (neoessentialsHandlerActive) {
                    ctx.getSource().sendSuccess(() -> MessageUtil.component(
                            "commands.neoessentials.permissions.external_perm_note_active", perm), false);
                } else {
                    ctx.getSource().sendSuccess(() -> MessageUtil.component(
                            "commands.neoessentials.permissions.external_perm_note_inactive", perm), false);
                }
            }
            return 1;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Unexpected error in addGroupPermission command", e);
            ctx.getSource().sendFailure(MessageUtil.component("commands.neoessentials.permissions.unexpected_error", e.getMessage()));
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
            
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Removing permission '{}' from group '{}'", perm, groupName);
            
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
                NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Removed permission '{}' from group '{}'", perm, groupName);
                PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_PERM_REMOVED, groupName, "node=" + perm);
            } catch (Exception e) { 
                NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to save permissions after removing group permission", e); 
                ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed"));
                return 0;
            }
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.permission_removed", perm, groupName), false);
            return 1;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Unexpected error in removeGroupPermission command for group '{}', permission '{}'",
                StringArgumentType.getString(ctx, "group"),
                StringArgumentType.getString(ctx, "permission"), e);
            ctx.getSource().sendFailure(MessageUtil.component("commands.neoessentials.permissions.unexpected_error", e.getMessage()));
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
            
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS,"Setting group '{}' for user '{}'", groupName, playerName);
            
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
                NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Set group '{}' for user '{}'", groupName, playerName);
                PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.USER_GROUP_SET, playerName, "group=" + groupName);
            } catch (Exception e) { 
                NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to save permissions after setting user group", e); 
                ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed"));
                return 0;
            }
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.user_group_set", playerName, groupName), false);
            return 1;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Unexpected error in setUserGroup command for player '{}', group '{}'",
                StringArgumentType.getString(ctx, "player"),
                StringArgumentType.getString(ctx, "group"), e);
            ctx.getSource().sendFailure(MessageUtil.component("commands.neoessentials.permissions.unexpected_error", e.getMessage()));
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
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Added permission '{}' to user '{}'", perm, playerName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.USER_PERM_ADDED, playerName, "node=" + perm);
        } catch (Exception e) { 
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to save permissions after adding user permission", e);
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
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Removed permission '{}' from user '{}'", perm, playerName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.USER_PERM_REMOVED, playerName, "node=" + perm);
        } catch (Exception e) { 
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to save permissions after removing user permission", e);
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
                ctx.getSource().sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.permissions.info.prefix_label_indented")).withStyle(ChatFormatting.DARK_GRAY)
                    .append(group.getPrefix() != null && !group.getPrefix().isEmpty()
                        ? RichTextFormatter.processTablistText(group.getPrefix()) : Component.literal(MessageUtil.localize("commands.neoessentials.permissions.none")))
                    .append(Component.literal(MessageUtil.localize("commands.neoessentials.permissions.info.suffix_label_indented")).withStyle(ChatFormatting.DARK_GRAY))
                    .append(group.getSuffix() != null && !group.getSuffix().isEmpty()
                        ? RichTextFormatter.processTablistText(group.getSuffix()) : Component.literal(MessageUtil.localize("commands.neoessentials.permissions.none"))), false);
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
                displayName = onlinePlayer.get().getGameProfile().name();
            } else {
                // Try to get from profile cache
                var profile = server.services().nameToIdCache().get(uuid);
                if (profile.isPresent()) {
                    displayName = profile.get().name();
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

        ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.info.group_header", group.getName()), false);
        // Prefix/suffix are admin-authored &-code/hex/gradient strings, not translation keys —
        // route them through RichTextFormatter (always processes rich text, like tablist/holograms)
        // instead of MessageUtil.info(), which would wrap them in Component.literal() unconverted
        // and show the raw "&7"/"<gradient:...>" text instead of the actual colors.
        ctx.getSource().sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.permissions.info.prefix_label")).withStyle(ChatFormatting.AQUA)
            .append(group.getPrefix() != null ? RichTextFormatter.processTablistText(group.getPrefix()) : Component.literal(MessageUtil.localize("commands.neoessentials.permissions.info.none_label"))), false);
        ctx.getSource().sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.permissions.info.suffix_label")).withStyle(ChatFormatting.AQUA)
            .append(group.getSuffix() != null ? RichTextFormatter.processTablistText(group.getSuffix()) : Component.literal(MessageUtil.localize("commands.neoessentials.permissions.info.none_label"))), false);
        ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.info.priority_label", group.getPriority()), false);
        ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.info.permissions_count_label", group.getPermissions().size()), false);

        if (group.getPermissions().isEmpty()) {
            ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.info.no_permissions"), false);
        } else {
            group.getPermissions().stream().limit(10).forEach(perm ->
                ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.info.permission_entry", perm), false));
            if (group.getPermissions().size() > 10) {
                ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.info.more_count", (group.getPermissions().size() - 10)), false);
            }
        }

        ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.info.inherits_count_label", group.getInherits().size()), false);
        if (group.getInherits().isEmpty()) {
            ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.info.no_inheritance"), false);
        } else {
            group.getInherits().forEach(inherit ->
                ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.info.inherit_entry", inherit), false));
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

        ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.info.user_header", playerName), false);
        ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.info.uuid_label", playerUUID), false);
        ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.info.group_label",
            user.getGroup() != null ? user.getGroup() : MessageUtil.localize("commands.neoessentials.permissions.default")), false);
        ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.info.direct_permissions_count_label", user.getPermissions().size()), false);

        if (user.getPermissions().isEmpty()) {
            ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.info.no_direct_permissions"), false);
        } else {
            user.getPermissions().stream().limit(10).forEach(perm ->
                ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.info.permission_entry", perm), false));
            if (user.getPermissions().size() > 10) {
                ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.info.more_count", (user.getPermissions().size() - 10)), false);
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
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.check_user_has", playerName, permission), false);
        } else {
            ctx.getSource().sendSuccess(() -> MessageUtil.error("commands.neoessentials.permissions.check_user_missing", playerName, permission), false);
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
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.check_group_has", groupName, permission), false);
        } else {
            ctx.getSource().sendSuccess(() -> MessageUtil.error("commands.neoessentials.permissions.check_group_missing", groupName, permission), false);
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
                ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.search_none_found", pattern), false);
                return 1;
            }

            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.search_found", matches.size(), pattern), false);
            matches.stream().limit(20).forEach(perm ->
                ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.search_list_entry", perm), false));

            if (matches.size() > 20) {
                ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.permissions.search_more", (matches.size() - 20)), false);
            }

            return 1;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS, "Unexpected error searching permissions for pattern '{}'", pattern, e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.search_failed", e.getMessage()));
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
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_already_exists", groupName));
            return 0;
        }

        PermissionGroup newGroup = new PermissionGroup(groupName);
        manager.addGroup(newGroup);
        manager.clearCache();

        try {
            PermissionStorage.save(manager);
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.group_created", groupName), false);
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Created new permission group: {}", groupName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_CREATED, groupName, "new group");
            return 1;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to save permissions after creating group", e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed_generic", e.getMessage()));
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
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_does_not_exist", groupName));
            return 0;
        }

        // Prevent deleting default group
        if (groupName.equalsIgnoreCase(manager.getDefaultGroup())) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.cannot_delete_default_group"));
            return 0;
        }

        manager.getGroups().removeIf(g -> g.getName().equalsIgnoreCase(groupName));
        manager.clearCache();

        try {
            PermissionStorage.save(manager);
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.group_deleted", groupName), false);
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Deleted permission group: {}", groupName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_DELETED, groupName, "group deleted");
            return 1;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to save permissions after deleting group", e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed_generic", e.getMessage()));
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
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_does_not_exist", oldName));
            return 0;
        }

        if (manager.getGroup(newName) != null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_already_exists", newName));
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
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.group_renamed", oldName, newName), false);
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Renamed permission group '{}' to '{}'", oldName, newName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_RENAMED, oldName, "newName=" + newName);
            return 1;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to save permissions after renaming group", e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed_generic", e.getMessage()));
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
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_does_not_exist", sourceName));
            return 0;
        }

        if (manager.getGroup(newName) != null) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.group_already_exists", newName));
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
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.group_cloned", sourceName, newName), false);
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Cloned permission group '{}' to '{}'", sourceName, newName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_CLONED, sourceName, "newGroup=" + newName);
            return 1;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to save permissions after cloning group", e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed_generic", e.getMessage()));
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
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.group_perms_cleared", count, groupName), false);
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Cleared all permissions from group '{}'", groupName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_PERMS_CLEARED, groupName, "all nodes removed");
            return 1;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to save permissions after clearing group", e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed_generic", e.getMessage()));
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
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.user_perms_cleared", count, playerName), false);
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Cleared all permissions from user '{}'", playerName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.USER_PERMS_CLEARED, playerName, "all direct nodes removed");
            return 1;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to save permissions after clearing user", e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed_generic", e.getMessage()));
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
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.inherit_group_not_found", inheritGroup));
            return 0;
        }

        if (groupName.equalsIgnoreCase(inheritGroup)) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.cannot_inherit_self"));
            return 0;
        }

        if (group.getInherits().contains(inheritGroup)) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.already_inherits", groupName, inheritGroup));
            return 0;
        }

        group.addInheritance(inheritGroup);
        manager.clearCache();

        try {
            PermissionStorage.save(manager);
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.inherit_added", groupName, inheritGroup), false);
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Added inheritance from '{}' to group '{}'", inheritGroup, groupName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_INHERIT_ADDED, groupName, "inherits=" + inheritGroup);
            return 1;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to save permissions after adding inheritance", e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed_generic", e.getMessage()));
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
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.does_not_inherit", groupName, inheritGroup));
            return 0;
        }

        group.removeInheritance(inheritGroup);
        manager.clearCache();

        try {
            PermissionStorage.save(manager);
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.permissions.inherit_removed", inheritGroup, groupName), false);
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Removed inheritance from '{}' from group '{}'", inheritGroup, groupName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_INHERIT_REMOVED, groupName, "removed=" + inheritGroup);
            return 1;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to save permissions after removing inheritance", e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed_generic", e.getMessage()));
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
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Rejected invalid temp-permission duration '{}' from user command", durStr, e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.invalid_duration", e.getMessage()));
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
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Granted temp permission '{}' to {} for {}", perm, playerName, remaining);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.USER_TEMP_PERM_ADDED,
                playerName, "node=" + perm + " expires_in=" + remaining);
            final String displayPerm = perm;
            ctx.getSource().sendSuccess(() -> MessageUtil.success(
                "commands.neoessentials.permissions.temp_granted_user", displayPerm, playerName, remaining), false);
            // Notify the target if online
            net.minecraft.server.level.ServerPlayer onlineTarget = server.getPlayerList().getPlayer(uuidOpt.get());
            if (onlineTarget != null) {
                onlineTarget.sendSystemMessage(MessageUtil.component(
                    "commands.neoessentials.permissions.temp_notify_target", perm, remaining));
            }
            return 1;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to save after addtemp", e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed_generic", e.getMessage()));
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
                "commands.neoessentials.permissions.user_no_temp_permission", playerName, perm));
            return 0;
        }
        user.removeTempPermission(perm);
        PermissionAPI.getManager().clearCache();
        try {
            PermissionStorage.save(PermissionAPI.getManager());
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Removed temp permission '{}' from {}", perm, playerName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.USER_TEMP_PERM_REMOVED,
                playerName, "node=" + perm);
            final String displayPerm = perm;
            ctx.getSource().sendSuccess(() -> MessageUtil.success(
                "commands.neoessentials.permissions.temp_removed_user", displayPerm, playerName), false);
            return 1;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS, "Failed to save permissions after removing temp permission '{}' from user '{}'", perm, playerName, e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed_generic", e.getMessage()));
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
            send(ctx, MessageUtil.localize("commands.neoessentials.permissions.no_temp_permissions_user", playerName));
            return 1;
        }
        long now = System.currentTimeMillis();
        send(ctx, MessageUtil.localize("commands.neoessentials.permissions.temp_permissions_header_user", playerName));
        temps.entrySet().stream()
            .sorted(java.util.Map.Entry.comparingByKey())
            .forEach(e -> {
                long remaining = e.getValue() - now;
                String timeStr = remaining > 0
                    ? "§a" + PermissionManager.formatDuration(remaining)
                    : "§cexpired";
                send(ctx, MessageUtil.localize("commands.neoessentials.permissions.temp_permission_line", e.getKey(), timeStr));
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
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Rejected invalid temp-permission duration '{}' from group command", durStr, e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.invalid_duration", e.getMessage()));
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
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Granted temp permission '{}' to group '{}' for {}", perm, groupName, remaining);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_TEMP_PERM_ADDED,
                groupName, "node=" + perm + " expires_in=" + remaining);
            final String displayPerm = perm;
            ctx.getSource().sendSuccess(() -> MessageUtil.success(
                "commands.neoessentials.permissions.temp_granted_group", displayPerm, groupName, remaining), false);
            return 1;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS,"Failed to save after group addtemp", e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed_generic", e.getMessage()));
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
                "commands.neoessentials.permissions.group_no_temp_permission", groupName, perm));
            return 0;
        }
        group.removeTempPermission(perm);
        PermissionAPI.getManager().clearCache();
        try {
            PermissionStorage.save(PermissionAPI.getManager());
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS,"Removed temp permission '{}' from group '{}'", perm, groupName);
            PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_TEMP_PERM_REMOVED,
                groupName, "node=" + perm);
            final String displayPerm = perm;
            ctx.getSource().sendSuccess(() -> MessageUtil.success(
                "commands.neoessentials.permissions.temp_removed_group", displayPerm, groupName), false);
            return 1;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS, "Failed to save permissions after removing temp permission '{}' from group '{}'", perm, groupName, e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed_generic", e.getMessage()));
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
            send(ctx, MessageUtil.localize("commands.neoessentials.permissions.no_temp_permissions_group", groupName));
            return 1;
        }
        long now = System.currentTimeMillis();
        send(ctx, MessageUtil.localize("commands.neoessentials.permissions.temp_permissions_header_group", groupName));
        temps.entrySet().stream()
            .sorted(java.util.Map.Entry.comparingByKey())
            .forEach(e -> {
                long remaining = e.getValue() - now;
                String timeStr = remaining > 0
                    ? "§a" + PermissionManager.formatDuration(remaining)
                    : "§cexpired";
                send(ctx, MessageUtil.localize("commands.neoessentials.permissions.temp_permission_line", e.getKey(), timeStr));
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
        NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Running permission debug trace for player '{}' ({})", playerName, uuid);

        // ── Header ──────────────────────────────────────────────────────────
        send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.header", playerName));

        // ── System state ─────────────────────────────────────────────────────
        var cfg = com.zerog.neoessentials.config.ConfigManager.getInstance();
        boolean emergencyMode  = PermissionAPI.isEmergencyMode();
        boolean usingExternal  = PermissionAPI.isUsingExternal();
        var externalAdapter    = PermissionAPI.getExternalAdapter();

        String modeLabel = emergencyMode ? MessageUtil.localize("commands.neoessentials.permissions.debug.mode_emergency")
                         : usingExternal && externalAdapter != null ? MessageUtil.localize("commands.neoessentials.permissions.debug.mode_external", externalAdapter.getName())
                         : MessageUtil.localize("commands.neoessentials.permissions.debug.mode_internal");
        send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.system_mode", modeLabel));

        if (usingExternal && externalAdapter != null) {
            String healthLabel = externalAdapter.isHealthy()
                    ? MessageUtil.localize("commands.neoessentials.permissions.debug.adapter_healthy")
                    : MessageUtil.localize("commands.neoessentials.permissions.debug.adapter_unhealthy_count", externalAdapter.getConsecutiveFailures());
            send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.adapter_health", healthLabel));
            send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.adapter_version", externalAdapter.getVersion()));
        }

        send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.ops_bypass", cfg.isOpsBypassPermissionsEnabled()));
        send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.vanilla_fallback", cfg.isVanillaOpFallbackEnabled()));

        // ── OP status ────────────────────────────────────────────────────────
        boolean isOp = false;
        try {
            ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(uuid);
            if (onlinePlayer != null) {
                isOp = com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(onlinePlayer, 2);
            } else {
                var profileCache = server.services().nameToIdCache();
                if (profileCache != null) {
                    var profile = profileCache.get(uuid).orElse(null);
                    if (profile != null) isOp = server.getPlayerList().isOp(profile);
                }
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Could not resolve OP status for '{}' during permission debug trace", playerName, e);
        }
        send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.op_status",
            isOp ? MessageUtil.localize("commands.neoessentials.permissions.debug.op_yes") : MessageUtil.localize("commands.neoessentials.permissions.debug.op_no")));

        // ── Internal-system details ──────────────────────────────────────────
        PermissionManager manager = PermissionAPI.getManager();
        if (manager != null) {
            PermissionUser user = manager.getUser(uuid);
            String groupName = (user != null && user.getGroup() != null)
                    ? user.getGroup() : manager.getDefaultGroup();
            send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.assigned_group", groupName));

            // Direct user permissions
            if (user != null && !user.getPermissions().isEmpty()) {
                send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.direct_permissions_count", user.getPermissions().size()));
                int count = 0;
                for (String perm : user.getPermissions()) {
                    if (count++ >= 10) {
                        send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.more_count", (user.getPermissions().size() - 10)));
                        break;
                    }
                    send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.permission_line", perm.startsWith("-") ? "§c" : "§f", perm));
                }
            } else {
                send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.direct_permissions_none"));
            }

            // Group chain
            send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.group_chain_label"));
            showGroupChain(ctx, manager, groupName, new java.util.LinkedHashSet<>(), 1);
        } else {
            send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.internal_manager_not_loaded"));
        }

        // ── Resolution chain summary ─────────────────────────────────────────
        send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.resolution_chain_header"));
        if (emergencyMode) {
            send(ctx, isOp ? MessageUtil.localize("commands.neoessentials.permissions.debug.emergency_grant")
                           : MessageUtil.localize("commands.neoessentials.permissions.debug.emergency_deny"));
        } else {
            if (cfg.isOpsBypassPermissionsEnabled()) {
                send(ctx, isOp
                    ? MessageUtil.localize("commands.neoessentials.permissions.debug.ops_bypass_grant")
                    : MessageUtil.localize("commands.neoessentials.permissions.debug.ops_bypass_continue"));
            } else {
                send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.ops_bypass_disabled"));
            }
            if (usingExternal && externalAdapter != null) {
                String adapterStatus = externalAdapter.isHealthy()
                        ? MessageUtil.localize("commands.neoessentials.permissions.debug.adapter_checks_node", externalAdapter.getName())
                        : MessageUtil.localize("commands.neoessentials.permissions.debug.adapter_unhealthy_fallthrough", externalAdapter.getName());
                send(ctx, adapterStatus);
            } else {
                send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.adapter_not_configured"));
            }
            send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.internal_manager_check"));
            if (cfg.isVanillaOpFallbackEnabled()) {
                send(ctx, isOp
                    ? MessageUtil.localize("commands.neoessentials.permissions.debug.vanilla_fallback_grant")
                    : MessageUtil.localize("commands.neoessentials.permissions.debug.vanilla_fallback_no_effect"));
            } else {
                send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.vanilla_fallback_disabled"));
            }
        }
        send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.footer"));
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
            send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.group_not_found_line", indent, groupName));
            return;
        }

        // Legacy &-codes need converting to § before concatenation into this plain-string
        // §-colour tree view (send() wraps the final string in Component.literal() with no
        // further processing) — otherwise "&7" etc. shows up as literal text. Gradient/hex
        // tags aren't expanded here (this is a plain-text tree view, not a rich preview);
        // use `/permissions info <group>` for a fully rendered prefix/suffix.
        String prefixInfo = (group.getPrefix() != null && !group.getPrefix().isEmpty())
                ? MessageUtil.localize("commands.neoessentials.permissions.debug.group_chain_prefix_info",
                    group.getPrefix().replaceAll("&([0-9a-fA-Fk-orK-OR])", "§$1")) : "";
        send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.group_chain_line", indent, groupName, group.getPermissions().size(), prefixInfo));

        int count = 0;
        for (String perm : group.getPermissions()) {
            if (count++ >= 8) {
                send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.group_chain_more", indent, (group.getPermissions().size() - 8)));
                break;
            }
            send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.group_chain_perm_line", indent, perm.startsWith("-") ? "§c" : "§f", perm));
        }

        for (String parent : group.getInherits()) {
            send(ctx, MessageUtil.localize("commands.neoessentials.permissions.debug.group_chain_inherits", indent, parent));
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
        NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Setting contextual permission '{}' (context={}) to {} for group '{}'", permission, contextKey, allow, groupName);
        group.addContextPermission(contextKey, permission, allow);
        PermissionAPI.getManager().clearCache();
        try { PermissionStorage.save(PermissionAPI.getManager()); } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS, "Failed to save permissions after setting contextual permission for group '{}'", groupName, e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed_generic", e.getMessage())); return 0; }
        String action = allow ? "§aallow" : "§cdeny";
        PermissionAuditLogger.log(getExecutorDisplay(ctx),
            allow ? PermissionAuditLogger.GROUP_CONTEXT_PERM_ADDED : PermissionAuditLogger.GROUP_CONTEXT_PERM_REMOVED,
            groupName, "context=" + contextKey + " node=" + permission + " value=" + allow);
        ctx.getSource().sendSuccess(() -> MessageUtil.success(
            "commands.neoessentials.permissions.context_permission_set", permission, action, groupName, contextKey), false);
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
        if (!removed) { ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.context_permission_not_found_remove", permission, contextKey)); return 0; }
        PermissionAPI.getManager().clearCache();
        try { PermissionStorage.save(PermissionAPI.getManager()); } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS, "Failed to save permissions after removing contextual permission for group '{}'", groupName, e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed_generic", e.getMessage())); return 0; }
        PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.GROUP_CONTEXT_PERM_REMOVED,
            groupName, "context=" + contextKey + " node=" + permission);
        ctx.getSource().sendSuccess(() -> MessageUtil.success(
            "commands.neoessentials.permissions.context_permission_removed_group", permission, groupName, contextKey), false);
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
        send(ctx, MessageUtil.localize("commands.neoessentials.permissions.context.group_header", groupName));
        if (ctxPerms.isEmpty()) { send(ctx, MessageUtil.localize("commands.neoessentials.permissions.context.none")); return 1; }
        ctxPerms.forEach((key, nodeMap) -> {
            send(ctx, MessageUtil.localize("commands.neoessentials.permissions.context.key_line", key));
            nodeMap.forEach((node, val) ->
                send(ctx, MessageUtil.localize("commands.neoessentials.permissions.context.node_line", val ? "§a✔ " : "§c✘ ", node)));
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
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS, "Failed to save permissions after setting contextual permission for user '{}'", playerName, e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed_generic", e.getMessage())); return 0; }
        String action = allow ? "§aallow" : "§cdeny";
        PermissionAuditLogger.log(getExecutorDisplay(ctx),
            allow ? PermissionAuditLogger.USER_CONTEXT_PERM_ADDED : PermissionAuditLogger.USER_CONTEXT_PERM_REMOVED,
            playerName, "context=" + contextKey + " node=" + permission + " value=" + allow);
        ctx.getSource().sendSuccess(() -> MessageUtil.success(
            "commands.neoessentials.permissions.context_permission_set_user", permission, action, playerName, contextKey), false);
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
        if (!removed) { ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.context_permission_not_found_remove_user", permission, contextKey)); return 0; }
        PermissionAPI.getManager().clearCache();
        try { PermissionStorage.save(PermissionAPI.getManager()); } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS, "Failed to save permissions after removing contextual permission for user '{}'", playerName, e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.permissions.save_failed_generic", e.getMessage())); return 0; }
        PermissionAuditLogger.log(getExecutorDisplay(ctx), PermissionAuditLogger.USER_CONTEXT_PERM_REMOVED,
            playerName, "context=" + contextKey + " node=" + permission);
        ctx.getSource().sendSuccess(() -> MessageUtil.success(
            "commands.neoessentials.permissions.context_permission_removed_user", permission, playerName, contextKey), false);
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
        send(ctx, MessageUtil.localize("commands.neoessentials.permissions.context.user_header", playerName));
        if (ctxPerms.isEmpty()) { send(ctx, MessageUtil.localize("commands.neoessentials.permissions.context.none")); return 1; }
        ctxPerms.forEach((key, nodeMap) -> {
            send(ctx, MessageUtil.localize("commands.neoessentials.permissions.context.key_line", key));
            nodeMap.forEach((node, val) ->
                send(ctx, MessageUtil.localize("commands.neoessentials.permissions.context.node_line", val ? "§a✔ " : "§c✘ ", node)));
        });
        return 1;
    }
}

