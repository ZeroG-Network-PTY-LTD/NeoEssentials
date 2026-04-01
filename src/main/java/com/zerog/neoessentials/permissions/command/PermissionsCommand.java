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
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.economy.EconomyPlayerUtil;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
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
                                java.util.Arrays.asList("admin", "moderator", "player", "vip", "default"),
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
                                java.util.Arrays.asList("admin", "moderator", "player", "vip", "default"),
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
                                java.util.Arrays.asList("admin", "moderator", "player", "vip", "default"),
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
                                java.util.Arrays.asList("admin", "moderator", "player", "vip", "default"),
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
                                java.util.Arrays.asList("admin", "moderator", "player", "vip", "default"),
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
                            java.util.Arrays.asList("admin", "moderator", "player", "vip", "default"), 
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
                                        java.util.Arrays.asList(
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
                                        java.util.Arrays.asList(
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
                                        java.util.Arrays.asList("admin", "moderator", "player", "vip", "default"),
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
                                        java.util.Arrays.asList("admin", "moderator", "player", "vip", "default"),
                                        builder);
                                })
                                .executes(ctx -> removeGroupInheritance(ctx)))))))
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
                                        java.util.Arrays.asList(
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
                                        java.util.Arrays.asList(
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
                        .executes(ctx -> clearUserPermissions(ctx)))))
            .then(Commands.literal("debug")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                        ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                            .map(p -> p.getGameProfile().getName()),
                        builder
                    ))
                    .executes(ctx -> debugPlayerPermissions(ctx))));
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
            String prefix = group.getPrefix() != null ? group.getPrefix() : MessageUtil.localize("commands.neoessentials.permissions.none");
            String suffix = group.getSuffix() != null ? group.getSuffix() : MessageUtil.localize("commands.neoessentials.permissions.none");
            ctx.getSource().sendSuccess(() -> MessageUtil.component("commands.neoessentials.permissions.group_entry", 
                group.getName(), prefix, suffix), false);
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
        ctx.getSource().sendSuccess(() -> MessageUtil.info("Prefix: " + (group.getPrefix() != null ? group.getPrefix() : "None")), false);
        ctx.getSource().sendSuccess(() -> MessageUtil.info("Suffix: " + (group.getSuffix() != null ? group.getSuffix() : "None")), false);
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
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to save permissions after removing inheritance", e);
            ctx.getSource().sendFailure(MessageUtil.error("Failed to save: " + e.getMessage()));
            return 0;
        }
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

        String prefixInfo = (group.getPrefix() != null && !group.getPrefix().isEmpty())
                ? " §8(prefix: §r" + group.getPrefix() + "§8)" : "";
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
}

