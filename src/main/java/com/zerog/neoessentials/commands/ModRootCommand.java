package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main NeoEssentials mod command providing system management and command routing functionality.
 * 
 * <p>Commands:</p>
 * <ul>
 *   <li>/neoessentials - Display help and list available commands</li>
 *   <li>/neoessentials reload - Reload all configurations (admin only)</li>
 *   <li>/neoessentials &lt;command&gt; [args] - Execute NeoEssentials command through router</li>
 *   <li>/neoe - Short alias for /neoessentials</li>
 * </ul>
 * 
 * <p>Permissions:</p>
 * <ul>
 *   <li>neoessentials.use - Base command access and help display</li>
 *   <li>neoessentials.admin.reload - Configuration reload capability</li>
 * </ul>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Comprehensive configuration reload (config, translations, permissions, chat)</li>
 *   <li>Command routing through centralized dispatcher</li>
 *   <li>Permission-based command filtering in help display</li>
 *   <li>Console support with full access</li>
 *   <li>Command validation through CommandRegistry</li>
 *   <li>Detailed error handling and user feedback</li>
 *   <li>Audit logging for administrative actions</li>
 * </ul>
 * 
 * <p>Reload Functionality:</p>
 * The reload subcommand refreshes:
 * <ul>
 *   <li>All configuration files from disk</li>
 *   <li>Translation/language files</li>
 *   <li>Permission system data</li>
 *   <li>ChatManager configuration</li>
 * </ul>
 */
public class ModRootCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModRootCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("neoe")
                .requires(source -> hasBaseCommandPermission(source))
                .then(Commands.literal("reload")
                    .requires(source -> hasAdminPermission(source))
                    .executes(ModRootCommand::reloadConfiguration)
                )
                .then(Commands.argument("command", StringArgumentType.greedyString())
                    .suggests(ModRootCommand::suggestModCommands)
                    .executes(ModRootCommand::dispatchToModCommand)
                )
                .executes(ModRootCommand::showAvailableCommands) // Show help when no args
        );
        dispatcher.register(
            Commands.literal("neoessentials")
                .requires(source -> hasBaseCommandPermission(source))
                .then(Commands.literal("reload")
                    .requires(source -> hasAdminPermission(source))
                    .executes(ModRootCommand::reloadConfiguration)
                )
                .then(Commands.argument("command", StringArgumentType.greedyString())
                    .suggests(ModRootCommand::suggestModCommands)
                    .executes(ModRootCommand::dispatchToModCommand)
                )
                .executes(ModRootCommand::showAvailableCommands) // Show help when no args
        );
    }
    
    /**
     * Check if the command source has permission to use the base NeoEssentials commands.
     * @param source Command source to check
     * @return true if has permission or is console
     */
    private static boolean hasBaseCommandPermission(CommandSourceStack source) {
        // Console always has access
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return true;
        }
        
        // Check for base command permission
        return com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
            player.getUUID(), "neoessentials.use");
    }
    
    /**
     * Check if the command source has admin permission for configuration changes.
     * @param source Command source to check
     * @return true if has admin permission or is console
     */
    private static boolean hasAdminPermission(CommandSourceStack source) {
        // Console always has access
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return true;
        }
        
        // Check for admin permission
        return com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
            player.getUUID(), "neoessentials.admin.reload");
    }

    private static CompletableFuture<Suggestions> suggestModCommands(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        // Get all available commands from the dynamic registry
        CommandRegistry registry = CommandRegistry.getInstance();
        List<String> commandNames = registry.getAllCommandNames().stream()
            .sorted()
            .collect(Collectors.toList());
        
        return net.minecraft.commands.SharedSuggestionProvider.suggest(commandNames, builder);
    }
    
    private static int reloadConfiguration(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        
        try {
            // Reload all configuration files
            com.zerog.neoessentials.config.ConfigManager.loadAll();
            
            // Reload translations
            try {
                com.zerog.neoessentials.util.MessageUtil.reloadTranslations();
            } catch (Exception e) {
                LOGGER.warn("Failed to reload translations: {}", e.getMessage());
                source.sendFailure(MessageUtil.warning("Failed to reload translations: " + e.getMessage()));
            }
            
            // Reload permissions if enabled
            try {
                com.zerog.neoessentials.api.permissions.PermissionAPI.reload();
            } catch (Exception e) {
                LOGGER.warn("Failed to reload permissions: {}", e.getMessage());
                source.sendFailure(MessageUtil.warning("Failed to reload permissions: " + e.getMessage()));
            }
            
            // Reload ChatManager configuration
            try {
                com.zerog.neoessentials.config.ConfigManager configManager = com.zerog.neoessentials.config.ConfigManager.getInstance();
                com.google.gson.JsonObject config = configManager.getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
                com.google.gson.JsonObject chatObj = config.has("chat") ? config.getAsJsonObject("chat") : new com.google.gson.JsonObject();
                com.google.gson.JsonObject commandsObj = config.has("commands") ? config.getAsJsonObject("commands") : new com.google.gson.JsonObject();
                
                // Create new ChatManager instance with updated configuration
                com.zerog.neoessentials.chat.ChatManager chatManager = new com.zerog.neoessentials.chat.ChatManager(chatObj, commandsObj);
                com.zerog.neoessentials.api.ChatAPI.setChatManager(chatManager);
                
                LOGGER.info("ChatManager configuration reloaded");
            } catch (Exception e) {
                LOGGER.warn("Failed to reload ChatManager: {}", e.getMessage());
                source.sendFailure(MessageUtil.warning("Failed to reload chat configuration: " + e.getMessage()));
            }
            
            source.sendSuccess(() -> MessageUtil.success("NeoEssentials configuration reloaded successfully!"), true);
            LOGGER.info("Configuration reloaded by {}", source.getTextName());
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Failed to reload configuration: {}", e.getMessage(), e);
            source.sendFailure(MessageUtil.error("Failed to reload configuration: " + e.getMessage()));
            return 0;
        }
    }

    private static int dispatchToModCommand(CommandContext<CommandSourceStack> ctx) {
        String commandString = StringArgumentType.getString(ctx, "command");
        CommandSourceStack source = ctx.getSource();
        
        // Extract just the command name (first word) for validation
        String commandName = commandString.split("\\s+")[0];
        
        // Check if the command is registered in our registry and actually exists
        CommandRegistry registry = CommandRegistry.getInstance();
        CommandDispatcher<CommandSourceStack> dispatcher = source.getServer().getCommands().getDispatcher();
        
        if (!registry.isCommandRegistered(commandName)) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.root.unknown_command", commandName));
            source.sendFailure(MessageUtil.info("commands.neoessentials.root.help_hint"));
            return 0;
        }
        
        // Double-check that the command actually exists in the dispatcher
        if (!registry.isCommandActuallyRegistered(commandName, dispatcher)) {
            LOGGER.warn("Command '{}' is in registry but not in dispatcher - possible registration issue", commandName);
            source.sendFailure(MessageUtil.error("commands.neoessentials.root.unknown_command", commandName));
            source.sendFailure(MessageUtil.info("commands.neoessentials.root.help_hint"));
            return 0;
        }
        
        // Execute the command properly through the dispatcher
        try {
            
            // Parse and execute the full command string directly through the dispatcher
            // This avoids recursive calls and properly handles permissions
            // Note: parse() expects command WITHOUT leading slash
            var parseResults = dispatcher.parse(commandString, source);
            
            if (parseResults.getReader().canRead()) {
                // Command has additional arguments that weren't consumed
                LOGGER.warn("Command '{}' has unconsumed arguments: '{}'", commandString, parseResults.getReader().getRemaining());
            }
            
            // Execute the parsed command
            int result = dispatcher.execute(parseResults);
            LOGGER.debug("Successfully executed command '{}' with result: {}", commandString, result);
            return result;
            
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            // Handle command syntax errors gracefully
            LOGGER.warn("Command syntax error for '{}': {}", commandString, e.getMessage());
            source.sendFailure(MessageUtil.error("commands.neoessentials.root.syntax_error", commandString, e.getMessage()));
            return 0;
        } catch (Exception e) {
            // Handle any other execution errors
            LOGGER.error("Failed to execute command '{}': {}", commandString, e.getMessage(), e);
            source.sendFailure(MessageUtil.error("commands.neoessentials.root.execution_failed", commandString));
            return 0;
        }
    }
    
    private static int showAvailableCommands(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        CommandRegistry registry = CommandRegistry.getInstance();
        
        List<CommandRegistry.CommandInfo> commands = registry.getAllCommandsSorted();
        
        if (commands.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.warning("commands.neoessentials.root.no_commands"), false);
            return 1;
        }
        
        // Show different header based on whether this is a player or console
        boolean isConsole = !(source.getEntity() instanceof ServerPlayer);
        String headerKey = isConsole ? "commands.neoessentials.root.help_header_console" : "commands.neoessentials.root.help_header";
        
        source.sendSuccess(() -> MessageUtil.info(headerKey), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.root.help_count", commands.size()), false);
        
        // Filter commands based on permissions for players
        List<CommandRegistry.CommandInfo> availableCommands = commands;
        if (!isConsole) {
            ServerPlayer player = (ServerPlayer) source.getEntity();
            availableCommands = commands.stream()
                .filter(info -> hasCommandPermission(player, info.getName()))
                .toList();
        }
        
        if (availableCommands.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.warning("commands.neoessentials.root.no_permission_commands"), false);
            return 1;
        }
        
        for (CommandRegistry.CommandInfo info : availableCommands) {
            if (info.hasAliases()) {
                String aliases = String.join(", /", info.getAliases());
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.root.command_with_aliases", 
                    info.getName(), aliases, info.getDescription()), false);
            } else {
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.root.command_simple", 
                    info.getName(), info.getDescription()), false);
            }
        }
        
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.help_footer"), false);
        
        return 1;
    }
    
    /**
     * Check if a player has permission to use a specific command.
     * @param player Player to check
     * @param commandName Command name to check
     * @return true if player has permission
     */
    private static boolean hasCommandPermission(ServerPlayer player, String commandName) {
        // For economy commands
        if (commandName.equals("balance") || commandName.equals("pay") || commandName.equals("paytoggle") || 
            commandName.equals("eco") || commandName.equals("baltop")) {
            return com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                player.getUUID(), "neoessentials.economy." + commandName);
        }
        
        // For chat commands
        if (commandName.equals("msg") || commandName.equals("reply") || commandName.equals("socialspy") ||
            commandName.equals("ignore") || commandName.equals("unignore") || commandName.equals("mute") ||
            commandName.equals("unmute") || commandName.equals("mutelist")) {
            return com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                player.getUUID(), "neoessentials.chat." + commandName);
        }
        
        // For item commands
        if (commandName.equals("repair") || commandName.equals("dispose") || commandName.equals("powertool") ||
            commandName.equals("enchant") || commandName.equals("clearinventory")) {
            return com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                player.getUUID(), "neoessentials.item." + commandName);
        }
        
        // For permission commands
        if (commandName.equals("pex") || commandName.equals("permissions")) {
            return com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                player.getUUID(), "neoessentials.admin.permissions");
        }
        
        // For utility commands
        if (commandName.equals("afk")) {
            return com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                player.getUUID(), "neoessentials.afk");
        }
        
        // Default: check generic command permission
        return com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
            player.getUUID(), "neoessentials.use");
    }
}
