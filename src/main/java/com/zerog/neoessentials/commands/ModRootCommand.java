package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.zerog.neoessentials.config.ConfigSplitter;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.List;

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
        LOGGER.info("Registering /neoe and /neoessentials root commands");
        dispatcher.register(
            Commands.literal("neoe")
                .requires(source -> {
                    boolean result = hasBaseCommandPermission(source);
                    LOGGER.debug("/neoe permission check for {}: {}", source.getTextName(), result);
                    return result;
                })
                .then(Commands.literal("reload")
                    .requires(source -> {
                        boolean result = hasAdminPermission(source);
                        LOGGER.debug("/neoe reload admin permission for {}: {}", source.getTextName(), result);
                        return result;
                    })
                    .executes(ModRootCommand::reloadConfiguration)
                )
                .then(Commands.literal("config")
                    .requires(source -> {
                        boolean result = hasAdminPermission(source);
                        LOGGER.debug("/neoe config admin permission for {}: {}", source.getTextName(), result);
                        return result;
                    })
                    .then(Commands.literal("split")
                        .executes(ModRootCommand::splitConfiguration)
                    )
                    .then(Commands.literal("validate")
                        .executes(ModRootCommand::validateConfiguration)
                    )
                    .then(Commands.literal("repair")
                        .executes(ModRootCommand::repairConfiguration)
                    )
                    .then(Commands.literal("status")
                        .executes(ModRootCommand::configStatus)
                    )
                )
                .then(Commands.argument("command", StringArgumentType.greedyString())
                    .suggests(ModRootCommand::suggestModCommands)
                    .executes(ModRootCommand::dispatchToModCommand)
                )
                .executes(ModRootCommand::showAvailableCommands) // Show help when no args
        );
        dispatcher.register(
            Commands.literal("neoessentials")
                .requires(source -> {
                    boolean result = hasBaseCommandPermission(source);
                    LOGGER.debug("/neoessentials permission check for {}: {}", source.getTextName(), result);
                    return result;
                })
                .then(Commands.literal("reload")
                    .requires(source -> {
                        boolean result = hasAdminPermission(source);
                        LOGGER.debug("/neoessentials reload admin permission for {}: {}", source.getTextName(), result);
                        return result;
                    })
                    .executes(ModRootCommand::reloadConfiguration)
                )
                .then(Commands.literal("config")
                    .requires(source -> {
                        boolean result = hasAdminPermission(source);
                        LOGGER.debug("/neoessentials config admin permission for {}: {}", source.getTextName(), result);
                        return result;
                    })
                    .then(Commands.literal("split")
                        .executes(ModRootCommand::splitConfiguration)
                    )
                    .then(Commands.literal("validate")
                        .executes(ModRootCommand::validateConfiguration)
                    )
                    .then(Commands.literal("repair")
                        .executes(ModRootCommand::repairConfiguration)
                    )
                    .then(Commands.literal("status")
                        .executes(ModRootCommand::configStatus)
                    )
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
            .toList();
        
        return net.minecraft.commands.SharedSuggestionProvider.suggest(commandNames, builder);
    }
    
    private static int reloadConfiguration(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        
        try {
            source.sendSuccess(() -> MessageUtil.info("Reloading NeoEssentials configuration..."), false);
            int successCount = 0;
            int totalCount = 0;

            // Reload all configuration files
            totalCount++;
            try {
                com.zerog.neoessentials.config.ConfigManager.loadAll();
                LOGGER.info("✓ Configuration files reloaded");
                successCount++;
            } catch (Exception e) {
                LOGGER.error("✗ Failed to reload configuration files: {}", e.getMessage(), e);
                source.sendFailure(MessageUtil.error("Failed to reload configuration: " + e.getMessage()));
            }

            // Reload translations
            totalCount++;
            try {
                com.zerog.neoessentials.util.MessageUtil.reloadTranslations();
                LOGGER.info("✓ Translations reloaded");
                successCount++;
            } catch (Exception e) {
                LOGGER.error("✗ Failed to reload translations: {}", e.getMessage(), e);
                source.sendFailure(MessageUtil.warning("Failed to reload translations: " + e.getMessage()));
            }
            
            // Reload permissions if enabled
            totalCount++;
            try {
                com.zerog.neoessentials.api.permissions.PermissionAPI.reload();
                LOGGER.info("✓ Permission system reloaded");
                successCount++;
            } catch (Exception e) {
                LOGGER.error("✗ Failed to reload permissions: {}", e.getMessage(), e);
                source.sendFailure(MessageUtil.warning("Failed to reload permissions: " + e.getMessage()));
            }
            
            // Reload KitManager
            totalCount++;
            try {
                com.zerog.neoessentials.kits.KitManager.getInstance().reload();
                LOGGER.info("✓ Kit system reloaded");
                successCount++;
            } catch (Exception e) {
                LOGGER.error("✗ Failed to reload kit system: {}", e.getMessage(), e);
                source.sendFailure(MessageUtil.warning("Failed to reload kits: " + e.getMessage()));
            }

            // Reload HomeManager
            totalCount++;
            try {
                com.zerog.neoessentials.teleportation.HomeManager.getInstance().reload();
                LOGGER.info("✓ Home system reloaded");
                successCount++;
            } catch (Exception e) {
                LOGGER.error("✗ Failed to reload home system: {}", e.getMessage(), e);
                source.sendFailure(MessageUtil.warning("Failed to reload homes: " + e.getMessage()));
            }

            // Reload WarpManager
            totalCount++;
            try {
                com.zerog.neoessentials.teleportation.Warp.WarpManager.getInstance().reload();
                LOGGER.info("✓ Warp system reloaded");
                successCount++;
            } catch (Exception e) {
                LOGGER.error("✗ Failed to reload warp system: {}", e.getMessage(), e);
                source.sendFailure(MessageUtil.warning("Failed to reload warps: " + e.getMessage()));
            }

            // Reload SpawnManager
            totalCount++;
            try {
                com.zerog.neoessentials.teleportation.Spawn.SpawnManager.getInstance().reload();
                LOGGER.info("✓ Spawn system reloaded");
                successCount++;
            } catch (Exception e) {
                LOGGER.error("✗ Failed to reload spawn system: {}", e.getMessage(), e);
                source.sendFailure(MessageUtil.warning("Failed to reload spawn: " + e.getMessage()));
            }

            // Reload ChatManager configuration
            totalCount++;
            try {
                com.zerog.neoessentials.config.ConfigManager configManager = com.zerog.neoessentials.config.ConfigManager.getInstance();
                com.google.gson.JsonObject config = configManager.getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
                com.google.gson.JsonObject chatObj = config.has("chat") ? config.getAsJsonObject("chat") : new com.google.gson.JsonObject();
                com.google.gson.JsonObject commandsObj = config.has("commands") ? config.getAsJsonObject("commands") : new com.google.gson.JsonObject();
                
                // Create new ChatManager instance with updated configuration
                com.zerog.neoessentials.chat.ChatManager chatManager = new com.zerog.neoessentials.chat.ChatManager(chatObj, commandsObj);
                com.zerog.neoessentials.api.ChatAPI.setChatManager(chatManager);
                
                LOGGER.info("✓ Chat system reloaded");
                successCount++;
            } catch (Exception e) {
                LOGGER.error("✗ Failed to reload chat system: {}", e.getMessage(), e);
                source.sendFailure(MessageUtil.warning("Failed to reload chat configuration: " + e.getMessage()));
            }
            
            // Reload AfkManager
            totalCount++;
            try {
                com.zerog.neoessentials.chat.AfkManager.getInstance().reload();
                LOGGER.info("✓ AFK system reloaded");
                successCount++;
            } catch (Exception e) {
                LOGGER.error("✗ Failed to reload AFK system: {}", e.getMessage(), e);
                source.sendFailure(MessageUtil.warning("Failed to reload AFK system: " + e.getMessage()));
            }

            // Reload JailManager
            totalCount++;
            try {
                com.zerog.neoessentials.moderation.JailManager.getInstance().reload();
                LOGGER.info("✓ Jail system reloaded");
                successCount++;
            } catch (Exception e) {
                LOGGER.error("✗ Failed to reload jail system: {}", e.getMessage(), e);
                source.sendFailure(MessageUtil.warning("Failed to reload jail system: " + e.getMessage()));
            }

            // Reload TablistManager (was missing – fixes "disable tablist in config, reload, no effect")
            totalCount++;
            try {
                com.zerog.neoessentials.tablist.TablistManager tablistMgr =
                    com.zerog.neoessentials.tablist.TablistManager.getInstance();
                tablistMgr.loadConfig();
                // Push the updated header/footer to all online players immediately
                net.minecraft.server.MinecraftServer reloadServer =
                    net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                if (reloadServer != null) {
                    tablistMgr.updateAll(reloadServer);
                }
                LOGGER.info("✓ Tablist system reloaded");
                successCount++;
            } catch (Exception e) {
                LOGGER.error("✗ Failed to reload tablist system: {}", e.getMessage(), e);
                source.sendFailure(MessageUtil.warning("Failed to reload tablist system: " + e.getMessage()));
            }

            // Reload WorthManager (item sell prices)
            totalCount++;
            try {
                com.zerog.neoessentials.economy.worth.WorthManager.getInstance().reload();
                LOGGER.info("✓ Worth system reloaded");
                successCount++;
            } catch (Exception e) {
                LOGGER.error("✗ Failed to reload worth system: {}", e.getMessage(), e);
                source.sendFailure(MessageUtil.warning("Failed to reload worth system: " + e.getMessage()));
            }

            // Reload RulesCommand (server rules from rules_data.json)
            totalCount++;
            try {
                com.zerog.neoessentials.util.commands.RulesCommand.reload();
                LOGGER.info("✓ Rules system reloaded");
                successCount++;
            } catch (Exception e) {
                LOGGER.error("✗ Failed to reload rules system: {}", e.getMessage(), e);
                source.sendFailure(MessageUtil.warning("Failed to reload rules: " + e.getMessage()));
            }

            // Build success message
            String resultMessage = String.format("NeoEssentials reload complete: %d/%d systems reloaded successfully",
                successCount, totalCount);

            if (successCount == totalCount) {
                source.sendSuccess(() -> MessageUtil.success(resultMessage), true);
            } else {
                source.sendSuccess(() -> MessageUtil.warning(resultMessage + " (check console for errors)"), true);
            }

            // Re-send the Brigadier command tree to all online players so that any
            // permission-gated commands appear/disappear correctly in tab-completion
            // without requiring a relog.
            try {
                net.minecraft.server.MinecraftServer cmdServer =
                    net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                if (cmdServer != null) {
                    for (net.minecraft.server.level.ServerPlayer onlinePlayer :
                            cmdServer.getPlayerList().getPlayers()) {
                        cmdServer.getCommands().sendCommands(onlinePlayer);
                    }
                    LOGGER.info("✓ Command trees re-sent to {} online player(s)",
                        cmdServer.getPlayerList().getPlayerCount());
                }
            } catch (Exception e) {
                LOGGER.warn("Could not re-sync command trees to players after reload: {}", e.getMessage());
            }

            LOGGER.info("Configuration reload completed: {}/{} systems reloaded successfully by {}",
                successCount, totalCount, source.getTextName());
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("CRITICAL: Failed to reload configuration: {}", e.getMessage(), e);
            source.sendFailure(MessageUtil.error("Failed to reload configuration: " + e.getMessage()));
            return 0;
        }
    }

    private static int validateConfiguration(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<String> problems = ConfigSplitter.validateSplitConfigs();
        if (problems.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.success("§a✔ All split config files are valid and complete."), false);
        } else {
            source.sendSuccess(() -> MessageUtil.warning("§e⚠ Split config validation found " + problems.size() + " problem(s):"), false);
            for (String problem : problems) {
                source.sendSuccess(() -> Component.literal("  §c• " + problem), false);
            }
            source.sendSuccess(() -> MessageUtil.info("Run §e/neoe config repair§7 to fix automatically."), false);
        }
        return problems.isEmpty() ? 1 : 0;
    }

    private static int repairConfiguration(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!ConfigSplitter.isSplittingEnabled()) {
            source.sendFailure(MessageUtil.error("Split configs are not enabled. Run /neoe config split first."));
            return 0;
        }
        source.sendSuccess(() -> MessageUtil.info("§eRepairing split config files…"), false);
        int repaired = ConfigSplitter.repairSplitConfigs();
        if (repaired == 0) {
            source.sendSuccess(() -> MessageUtil.success("§a✔ No repairs needed — all split config files are complete."), false);
        } else {
            final int r = repaired;
            source.sendSuccess(() -> MessageUtil.success(
                "§a✔ Repaired " + r + " split config file(s). Run /neoe reload to apply."), false);
        }
        List<String> remaining = ConfigSplitter.validateSplitConfigs();
        if (!remaining.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.warning(
                "§e⚠ " + remaining.size() + " problem(s) could not be auto-repaired:"), false);
            for (String p : remaining) {
                source.sendSuccess(() -> Component.literal("  §c• " + p), false);
            }
        }
        return 1;
    }

    private static int configStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        boolean splitEnabled = ConfigSplitter.isSplittingEnabled();
        source.sendSuccess(() -> Component.literal("§6§l━━━━━━━━━ Config Status ━━━━━━━━━"), false);
        source.sendSuccess(() -> Component.literal("§7Mode: " +
            (splitEnabled ? "§aSplit configs (recommended)" : "§eMonolithic config.json")), false);
        if (splitEnabled) {
            source.sendSuccess(() -> Component.literal("§7Files:"), false);
            for (java.util.Map.Entry<String, java.util.List<String>> e :
                    ConfigSplitter.FILE_SECTIONS_MAP.entrySet()) {
                String file = e.getKey();
                java.io.File f = com.zerog.neoessentials.util.ResourceUtil.getConfigFile(file);
                boolean exists = f.exists();
                source.sendSuccess(() -> Component.literal(
                    (exists ? "  §a✔ " : "  §c✘ ") + file + " §8— " +
                    String.join(", ", e.getValue())), false);
            }
            List<String> problems = ConfigSplitter.validateSplitConfigs();
            if (problems.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§a✔ All files present and valid."), false);
            } else {
                final int n = problems.size();
                source.sendSuccess(() -> Component.literal(
                    "§c⚠ " + n + " problem(s). Run §e/neoe config repair§c."), false);
            }
        } else {
            java.io.File mainCfg = com.zerog.neoessentials.util.ResourceUtil.getConfigFile("config.json");
            source.sendSuccess(() -> Component.literal(
                "§7config.json: " + (mainCfg.exists() ? "§a✔ present" : "§c✘ missing")), false);
            source.sendSuccess(() -> Component.literal(
                "§7Tip: Run §e/neoe config split§7 to use split configs."), false);
        }
        source.sendSuccess(() -> Component.literal("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
        return 1;
    }

    private static int splitConfiguration(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        
        try {
            // Check if already using split configs
            if (ConfigSplitter.isSplittingEnabled()) {
                source.sendSuccess(() -> MessageUtil.warning("Split configs are already enabled!"), false);
                source.sendSuccess(() -> MessageUtil.info("Config files are already split into smaller files."), false);
                return 0;
            }
            
            source.sendSuccess(() -> MessageUtil.info("§6" + "─".repeat(40)), false);
            source.sendSuccess(() -> MessageUtil.info("§eMigrating to split configuration files..."), false);
            source.sendSuccess(() -> MessageUtil.info("§6" + "─".repeat(40)), false);

            // Perform the migration
            boolean success = ConfigSplitter.migrateToSplitConfigs();

            if (success) {
                source.sendSuccess(() -> MessageUtil.success("✓ Successfully migrated to split configs!"), false);
                source.sendSuccess(() -> MessageUtil.info("§aYour config.json has been split into smaller files:"), false);
                source.sendSuccess(() -> MessageUtil.info("  - main.json (modules, logging, permissions)"), false);
                source.sendSuccess(() -> MessageUtil.info("  - commands.json (command enable/disable)"), false);
                source.sendSuccess(() -> MessageUtil.info("  - chat.json (chat system settings)"), false);
                source.sendSuccess(() -> MessageUtil.info("  - teleportation.json (teleport settings)"), false);
                source.sendSuccess(() -> MessageUtil.info("  - moderation.json (ban, jail, freeze, etc.)"), false);
                source.sendSuccess(() -> MessageUtil.info("  - items.json (item spawn settings)"), false);
                source.sendSuccess(() -> MessageUtil.info("  - afk.json (AFK system settings)"), false);
                source.sendSuccess(() -> MessageUtil.info("  - security.json (security settings)"), false);
                source.sendSuccess(() -> MessageUtil.info("§eOriginal config backed up to: config.json.backup"), false);
                source.sendSuccess(() -> MessageUtil.info("§aReload configs with: /neoessentials reload"), false);
                
                LOGGER.info("Configuration split completed successfully by {}", source.getTextName());
                return 1;
            } else {
                source.sendFailure(MessageUtil.error("Failed to split configuration. Check console for details."));
                return 0;
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to split configuration: {}", e.getMessage(), e);
            source.sendFailure(MessageUtil.error("An error occurred while splitting configs: " + e.getMessage()));
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
    
    @SuppressWarnings("SameReturnValue") // Command success - always returns 1
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
    @SuppressWarnings("IfCanBeSwitch") // Current if-else structure is clearer for grouped permissions
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
