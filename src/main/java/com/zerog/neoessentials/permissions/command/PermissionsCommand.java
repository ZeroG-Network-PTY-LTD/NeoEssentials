
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
                            .executes(ctx -> removeUserPermission(ctx))))));
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

    private static int addGroupPermission(CommandContext<CommandSourceStack> ctx) {
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
            return 1;
        } catch (Exception e) {
            LOGGER.error("Unexpected error in addGroupPermission command", e);
            ctx.getSource().sendFailure(MessageUtil.error("§cAn unexpected error occurred: " + e.getMessage()));
            e.printStackTrace();
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
            LOGGER.error("Unexpected error in removeGroupPermission command", e);
            ctx.getSource().sendFailure(MessageUtil.error("§cAn unexpected error occurred: " + e.getMessage()));
            e.printStackTrace();
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
            LOGGER.error("Unexpected error in setUserGroup command", e);
            ctx.getSource().sendFailure(MessageUtil.error("§cAn unexpected error occurred: " + e.getMessage()));
            e.printStackTrace();
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
}