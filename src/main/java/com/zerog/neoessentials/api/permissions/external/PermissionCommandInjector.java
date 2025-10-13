package com.zerog.neoessentials.api.permissions.external;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Permission Command Injector - Creates fake permission commands that PermissionsEX can discover.
 * This is a workaround to make PermissionsEX aware of our permissions by registering them as actual commands.
 */
public class PermissionCommandInjector {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCommandInjector.class);
    
    /**
     * Inject permission commands into the command dispatcher.
     * This makes PermissionsEX able to discover our permissions for tab completion.
     */
    public static void injectPermissionCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        LOGGER.info("Injecting NeoEssentials permissions as discoverable commands for PermissionsEX...");
        
        try {
            // Get all permissions
            List<String> permissions = ExternalPermissionProvider.getAllNeoEssentialsPermissions();
            
            // Create a suggestion provider for all our permissions
            SuggestionProvider<CommandSourceStack> permissionSuggestions = (context, builder) -> {
                return SharedSuggestionProvider.suggest(permissions, builder);
            };
            
            // Register a fake "pex" command that includes our permissions in suggestions
            // This makes PermissionsEX tab completion work by piggy-backing on our command
            dispatcher.register(Commands.literal("neoessentials-pex-bridge")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("group")
                    .then(Commands.argument("groupname", StringArgumentType.word())
                        .then(Commands.literal("add")
                            .then(Commands.argument("permission", StringArgumentType.greedyString())
                                .suggests(permissionSuggestions)
                                .executes(ctx -> {
                                    String permission = StringArgumentType.getString(ctx, "permission");
                                    String group = StringArgumentType.getString(ctx, "groupname");
                                    ctx.getSource().sendSuccess(() -> 
                                        net.minecraft.network.chat.Component.literal(
                                            "§6NeoEssentials Permission Bridge: §7Use §f/pex group " + group + " add " + permission + 
                                            " §7in your actual PermissionsEX plugin"
                                        ), false);
                                    return 1;
                                })
                            )
                        )
                        .then(Commands.literal("remove")
                            .then(Commands.argument("permission", StringArgumentType.greedyString())
                                .suggests(permissionSuggestions)
                                .executes(ctx -> {
                                    String permission = StringArgumentType.getString(ctx, "permission");
                                    String group = StringArgumentType.getString(ctx, "groupname");
                                    ctx.getSource().sendSuccess(() -> 
                                        net.minecraft.network.chat.Component.literal(
                                            "§6NeoEssentials Permission Bridge: §7Use §f/pex group " + group + " remove " + permission + 
                                            " §7in your actual PermissionsEX plugin"
                                        ), false);
                                    return 1;
                                })
                            )
                        )
                    )
                )
                .then(Commands.literal("user")
                    .then(Commands.argument("username", StringArgumentType.word())
                        .then(Commands.literal("add")
                            .then(Commands.argument("permission", StringArgumentType.greedyString())
                                .suggests(permissionSuggestions)
                                .executes(ctx -> {
                                    String permission = StringArgumentType.getString(ctx, "permission");
                                    String user = StringArgumentType.getString(ctx, "username");
                                    ctx.getSource().sendSuccess(() -> 
                                        net.minecraft.network.chat.Component.literal(
                                            "§6NeoEssentials Permission Bridge: §7Use §f/pex user " + user + " add " + permission + 
                                            " §7in your actual PermissionsEX plugin"
                                        ), false);
                                    return 1;
                                })
                            )
                        )
                        .then(Commands.literal("remove")
                            .then(Commands.argument("permission", StringArgumentType.greedyString())
                                .suggests(permissionSuggestions)
                                .executes(ctx -> {
                                    String permission = StringArgumentType.getString(ctx, "permission");
                                    String user = StringArgumentType.getString(ctx, "username");
                                    ctx.getSource().sendSuccess(() -> 
                                        net.minecraft.network.chat.Component.literal(
                                            "§6NeoEssentials Permission Bridge: §7Use §f/pex user " + user + " remove " + permission + 
                                            " §7in your actual PermissionsEX plugin"
                                        ), false);
                                    return 1;
                                })
                            )
                        )
                    )
                )
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> 
                        net.minecraft.network.chat.Component.literal(
                            "§6=== NeoEssentials PermissionsEX Bridge ===\n" +
                            "§7This command provides tab completion for NeoEssentials permissions.\n" +
                            "§7Use: §f/neoessentials-pex-bridge group <name> add <permission>\n" +
                            "§7Use: §f/neoessentials-pex-bridge user <name> add <permission>\n" +
                            "§7Total permissions available: §f" + permissions.size() + "\n" +
                            "§7For actual permission management, use your PermissionsEX plugin."
                        ), false);
                    return 1;
                })
            );
            
            // Also try to hook into existing permission-related commands if they exist
            injectIntoExistingCommands(dispatcher, permissions);
            
            LOGGER.info("Permission command injection completed - {} permissions now discoverable", permissions.size());
            
        } catch (Exception e) {
            LOGGER.error("Failed to inject permission commands", e);
        }
    }
    
    /**
     * Try to hook into existing permission commands to provide tab completion
     */
    private static void injectIntoExistingCommands(CommandDispatcher<CommandSourceStack> dispatcher, List<String> permissions) {
        try {
            // Check if there are any existing permission-related commands we can enhance
            var rootCommands = dispatcher.getRoot().getChildren();
            
            for (var command : rootCommands) {
                String commandName = command.getName();
                
                // Look for permission-related commands
                if (commandName.contains("pex") || commandName.contains("permission") || 
                    commandName.contains("perm") || commandName.contains("group") || commandName.contains("user")) {
                    
                    LOGGER.debug("Found potential permission command to enhance: {}", commandName);
                    // We could potentially modify these commands, but it's risky
                    // Instead, we'll just log that we found them
                }
            }
            
        } catch (Exception e) {
            LOGGER.debug("Could not analyze existing commands for permission injection", e);
        }
    }
    
    /**
     * Create a simple permission lookup command for testing
     */
    public static void registerTestCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("test-pex-integration")
            .requires(source -> source.hasPermission(4))
            .executes(ctx -> {
                List<String> permissions = ExternalPermissionProvider.getAllNeoEssentialsPermissions();
                
                ctx.getSource().sendSuccess(() -> 
                    net.minecraft.network.chat.Component.literal(
                        "§6=== PermissionsEX Integration Test ===\n" +
                        "§7Available NeoEssentials permissions: §f" + permissions.size() + "\n" +
                        "§7Sample permissions:\n" +
                        "§f" + String.join("\n", permissions.subList(0, Math.min(10, permissions.size()))) +
                        (permissions.size() > 10 ? "\n§7... and " + (permissions.size() - 10) + " more" : "")
                    ), false);
                return 1;
            })
        );
    }
}