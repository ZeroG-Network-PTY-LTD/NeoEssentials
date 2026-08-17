package com.zerog.neoessentials.api.permissions.external;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import java.util.List;

/**
 * Fake PermissionsEX Command - Provides actual /pex command with NeoEssentials tab completion.
 * This command mimics PermissionsEX behavior to provide tab completion until the real plugin loads.
 */
public class FakePermissionsEXCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(FakePermissionsEXCommand.class);
    
    /**
     * Register a fake /pex command that provides tab completion for NeoEssentials permissions.
     * This will be overridden if the real PermissionsEX loads, but provides fallback functionality.
     */
    public static void registerFakePexCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("pex")) {
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Fake /pex command is disabled in config, skipping registration");
            return;
        }
        NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "Registering fake /pex command for NeoEssentials permission tab completion...");

        try {
            // Get all permissions for suggestions
            SuggestionProvider<CommandSourceStack> permissionSuggestions = (context, builder) -> {
                List<String> permissions = ExternalPermissionProvider.getAllNeoEssentialsPermissions();
                String input = builder.getRemaining().toLowerCase();
                
                List<String> filtered = permissions.stream()
                    .filter(perm -> perm.toLowerCase().startsWith(input))
                    .toList();
                    
                return SharedSuggestionProvider.suggest(filtered, builder);
            };
            
            // Register the fake /pex command
            dispatcher.register(Commands.literal("pex")
                .requires(source -> com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(source, 2)) // Require op level 2
                
                // /pex group <name> add <permission>
                .then(Commands.literal("group")
                    .then(Commands.argument("groupname", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                            List.of("admin", "moderator", "player", "vip", "default"), builder))
                        
                        .then(Commands.literal("add")
                            .then(Commands.argument("permission", StringArgumentType.greedyString())
                                .suggests(permissionSuggestions)
                                .executes(ctx -> {
                                    String group = StringArgumentType.getString(ctx, "groupname");
                                    String permission = StringArgumentType.getString(ctx, "permission");
                                    
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                        "§c[FAKE PEX] §6Would add permission §f" + permission + 
                                        " §6to group §f" + group + "§6.\n" +
                                        "§7This is a NeoEssentials simulation. Install actual PermissionsEX for real functionality.\n" +
                                        "§7Permission is valid: §a" + ExternalPermissionProvider.hasPermission(permission)
                                    ), false);
                                    
                                    NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "Fake PEX: Would add {} to group {}", permission, group);
                                    return 1;
                                })
                            )
                        )
                        
                        .then(Commands.literal("remove")
                            .then(Commands.argument("permission", StringArgumentType.greedyString())
                                .suggests(permissionSuggestions)
                                .executes(ctx -> {
                                    String group = StringArgumentType.getString(ctx, "groupname");
                                    String permission = StringArgumentType.getString(ctx, "permission");
                                    
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                        "§c[FAKE PEX] §6Would remove permission §f" + permission + 
                                        " §6from group §f" + group + "§6.\n" +
                                        "§7This is a NeoEssentials simulation. Install actual PermissionsEX for real functionality."
                                    ), false);
                                    
                                    return 1;
                                })
                            )
                        )
                        
                        .then(Commands.literal("list")
                            .executes(ctx -> {
                                String group = StringArgumentType.getString(ctx, "groupname");
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                    "§c[FAKE PEX] §6Group §f" + group + " §6permissions:\n" +
                                    "§7This is a simulation. Install actual PermissionsEX to manage real permissions.\n" +
                                    "§7Available NeoEssentials permissions: §f" + ExternalPermissionProvider.getAllNeoEssentialsPermissions().size()
                                ), false);
                                return 1;
                            })
                        )
                    )
                )
                
                // /pex user <name> add <permission>
                .then(Commands.literal("user")
                    .then(Commands.argument("username", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            // Suggest online players
                            return SharedSuggestionProvider.suggest(
                                context.getSource().getServer().getPlayerNames(), builder);
                        })
                        
                        .then(Commands.literal("add")
                            .then(Commands.argument("permission", StringArgumentType.greedyString())
                                .suggests(permissionSuggestions)
                                .executes(ctx -> {
                                    String user = StringArgumentType.getString(ctx, "username");
                                    String permission = StringArgumentType.getString(ctx, "permission");
                                    
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                        "§c[FAKE PEX] §6Would add permission §f" + permission + 
                                        " §6to user §f" + user + "§6.\n" +
                                        "§7This is a NeoEssentials simulation. Install actual PermissionsEX for real functionality.\n" +
                                        "§7Permission is valid: §a" + ExternalPermissionProvider.hasPermission(permission)
                                    ), false);
                                    
                                    NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "Fake PEX: Would add {} to user {}", permission, user);
                                    return 1;
                                })
                            )
                        )
                        
                        .then(Commands.literal("remove")
                            .then(Commands.argument("permission", StringArgumentType.greedyString())
                                .suggests(permissionSuggestions)
                                .executes(ctx -> {
                                    String user = StringArgumentType.getString(ctx, "username");
                                    String permission = StringArgumentType.getString(ctx, "permission");
                                    
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                        "§c[FAKE PEX] §6Would remove permission §f" + permission + 
                                        " §6from user §f" + user + "§6.\n" +
                                        "§7This is a NeoEssentials simulation. Install actual PermissionsEX for real functionality."
                                    ), false);
                                    
                                    return 1;
                                })
                            )
                        )
                    )
                )
                
                // /pex (main command)
                .executes(ctx -> {
                    List<String> permissions = ExternalPermissionProvider.getAllNeoEssentialsPermissions();
                    
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "§c[FAKE PermissionsEX] §6NeoEssentials Permission Bridge\n" +
                        "§7This is a simulation providing tab completion for NeoEssentials permissions.\n" +
                        "§7Install actual PermissionsEX for real permission management.\n\n" +
                        "§eAvailable commands:\n" +
                        "§f/pex group <name> add <permission> §7- Add permission to group\n" +
                        "§f/pex user <name> add <permission> §7- Add permission to user\n" +
                        "§f/pex group <name> remove <permission> §7- Remove permission from group\n" +
                        "§f/pex user <name> remove <permission> §7- Remove permission from user\n\n" +
                        "§7NeoEssentials permissions available: §f" + permissions.size() + "\n" +
                        "§7Try typing: §f/pex group admin add neoessentials.§7 and press TAB"
                    ), false);
                    
                    return 1;
                })
            );
            
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "Fake /pex command registered - {} permissions available for tab completion", 
                ExternalPermissionProvider.getAllNeoEssentialsPermissions().size());
            
        } catch (Exception e) {
            LOGGER.error("Failed to register fake /pex command", e);
        }
    }
    
    /**
     * Check if the real PermissionsEX is loaded and unregister our fake command if needed.
     */
    public static void checkForRealPermissionsEX(CommandDispatcher<CommandSourceStack> dispatcher) {
        try {
            // This would check if real PermissionsEX commands exist
            // If they do, we could remove our fake command
            var existing = dispatcher.getRoot().getChild("pex");
            if (existing != null) {
                NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "Real /pex command detected - our fake command may be overridden");
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Could not check for real PermissionsEX", e);
        }
    }
}