package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.zerog.neoessentials.config.ConfigSplitter;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
        NeoLog.info(LOGGER, LogCategory.COMMANDS, "Registering /neoe and /neoessentials root commands");
        com.zerog.neoessentials.config.ConfigManager cfg = com.zerog.neoessentials.config.ConfigManager.getInstance();
        if (cfg.isCommandEnabled("neoe")) {
        dispatcher.register(
            Commands.literal("neoe")
                .requires(source -> {
                    boolean result = hasBaseCommandPermission(source);
                    NeoLog.debug(LOGGER, LogCategory.COMMANDS, "/neoe permission check for {}: {}", source.getTextName(), result);
                    return result;
                })
                .then(Commands.literal("reload")
                    .requires(source -> {
                        boolean result = hasAdminPermission(source);
                        NeoLog.debug(LOGGER, LogCategory.COMMANDS, "/neoe reload admin permission for {}: {}", source.getTextName(), result);
                        return result;
                    })
                    .executes(ModRootCommand::reloadConfiguration)
                )
                .then(Commands.literal("config")
                    .requires(source -> {
                        boolean result = hasAdminPermission(source);
                        NeoLog.debug(LOGGER, LogCategory.COMMANDS, "/neoe config admin permission for {}: {}", source.getTextName(), result);
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
        if (cfg.isCommandEnabled("neoessentials")) {
        dispatcher.register(
            Commands.literal("neoessentials")
                .requires(source -> {
                    boolean result = hasBaseCommandPermission(source);
                    NeoLog.debug(LOGGER, LogCategory.COMMANDS, "/neoessentials permission check for {}: {}", source.getTextName(), result);
                    return result;
                })
                .then(Commands.literal("reload")
                    .requires(source -> {
                        boolean result = hasAdminPermission(source);
                        NeoLog.debug(LOGGER, LogCategory.COMMANDS, "/neoessentials reload admin permission for {}: {}", source.getTextName(), result);
                        return result;
                    })
                    .executes(ModRootCommand::reloadConfiguration)
                )
                .then(Commands.literal("config")
                    .requires(source -> {
                        boolean result = hasAdminPermission(source);
                        NeoLog.debug(LOGGER, LogCategory.COMMANDS, "/neoessentials config admin permission for {}: {}", source.getTextName(), result);
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
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.reload_start"), false);
            int successCount = 0;
            int totalCount = 0;

            // Reload all configuration files
            totalCount++;
            try {
                com.zerog.neoessentials.config.ConfigManager.loadAll();
                NeoLog.info(LOGGER, LogCategory.COMMANDS, "✓ Configuration files reloaded");
                successCount++;
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.COMMANDS, "✗ Failed to reload configuration files: {}", e.getMessage(), e);
                final String fMsg = e.getMessage();
                source.sendFailure(MessageUtil.error("commands.neoessentials.root.reload_error_config", fMsg));
            }

            // Reload translations
            totalCount++;
            try {
                com.zerog.neoessentials.util.MessageUtil.reloadTranslations();
                NeoLog.info(LOGGER, LogCategory.COMMANDS, "✓ Translations reloaded");
                successCount++;
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.COMMANDS, "✗ Failed to reload translations: {}", e.getMessage(), e);
                final String fMsg = e.getMessage();
                source.sendFailure(MessageUtil.warning("commands.neoessentials.root.reload_error_translations", fMsg));
            }
            
            // Reload permissions if enabled
            totalCount++;
            try {
                // PermissionAPI.reload() only re-reads data into an ALREADY-initialized manager
                // (or external adapter) — it throws if neither exists. That's exactly the state
                // a boot-time initialization failure (e.g. a config that couldn't be parsed yet)
                // leaves things in, and it's permanent: every prefix/suffix/permission check
                // keeps silently failing for the rest of the session with no way to recover
                // short of a full restart. If reload() finds nothing to work with, fall back to
                // a full re-initialization instead — it re-runs the same external/internal
                // detection initialize() does at boot, so a config fixed after the fact (or a
                // permission plugin that finished loading late) actually gets picked up.
                if (com.zerog.neoessentials.api.permissions.PermissionAPI.getManager() == null
                        && !com.zerog.neoessentials.api.permissions.PermissionAPI.isUsingExternal()) {
                    NeoLog.warn(LOGGER, LogCategory.COMMANDS, "Permission system was never fully initialized — re-initializing from scratch instead of a plain reload");
                    com.zerog.neoessentials.permissions.PermissionSystem.reinitialize();
                } else {
                    com.zerog.neoessentials.api.permissions.PermissionAPI.reload();
                }
                NeoLog.info(LOGGER, LogCategory.COMMANDS, "✓ Permission system reloaded");
                successCount++;
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.COMMANDS, "✗ Failed to reload permissions: {}", e.getMessage(), e);
                final String fMsg = e.getMessage();
                source.sendFailure(MessageUtil.warning("commands.neoessentials.root.reload_error_permissions", fMsg));
            }
            
            // Reload KitManager
            totalCount++;
            try {
                com.zerog.neoessentials.kits.KitManager.getInstance().reload();
                NeoLog.info(LOGGER, LogCategory.COMMANDS, "✓ Kit system reloaded");
                successCount++;
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.COMMANDS, "✗ Failed to reload kit system: {}", e.getMessage(), e);
                final String fMsg = e.getMessage();
                source.sendFailure(MessageUtil.warning("commands.neoessentials.root.reload_error_kits", fMsg));
            }

            // Reload HomeManager
            totalCount++;
            try {
                com.zerog.neoessentials.teleportation.HomeManager.getInstance().reload();
                NeoLog.info(LOGGER, LogCategory.COMMANDS, "✓ Home system reloaded");
                successCount++;
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.COMMANDS, "✗ Failed to reload home system: {}", e.getMessage(), e);
                final String fMsg = e.getMessage();
                source.sendFailure(MessageUtil.warning("commands.neoessentials.root.reload_error_homes", fMsg));
            }

            // Reload WarpManager
            totalCount++;
            try {
                com.zerog.neoessentials.teleportation.Warp.WarpManager.getInstance().reload();
                NeoLog.info(LOGGER, LogCategory.COMMANDS, "✓ Warp system reloaded");
                successCount++;
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.COMMANDS, "✗ Failed to reload warp system: {}", e.getMessage(), e);
                final String fMsg = e.getMessage();
                source.sendFailure(MessageUtil.warning("commands.neoessentials.root.reload_error_warps", fMsg));
            }

            // Reload SpawnManager
            totalCount++;
            try {
                com.zerog.neoessentials.teleportation.Spawn.SpawnManager.getInstance().reload();
                NeoLog.info(LOGGER, LogCategory.COMMANDS, "✓ Spawn system reloaded");
                successCount++;
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.COMMANDS, "✗ Failed to reload spawn system: {}", e.getMessage(), e);
                final String fMsg = e.getMessage();
                source.sendFailure(MessageUtil.warning("commands.neoessentials.root.reload_error_spawn", fMsg));
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
                
                NeoLog.info(LOGGER, LogCategory.COMMANDS, "✓ Chat system reloaded");
                successCount++;
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.COMMANDS, "✗ Failed to reload chat system: {}", e.getMessage(), e);
                final String fMsg = e.getMessage();
                source.sendFailure(MessageUtil.warning("commands.neoessentials.root.reload_error_chat", fMsg));
            }
            
            // Reload AfkManager
            totalCount++;
            try {
                com.zerog.neoessentials.chat.AfkManager.getInstance().reload();
                NeoLog.info(LOGGER, LogCategory.COMMANDS, "✓ AFK system reloaded");
                successCount++;
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.COMMANDS, "✗ Failed to reload AFK system: {}", e.getMessage(), e);
                final String fMsg = e.getMessage();
                source.sendFailure(MessageUtil.warning("commands.neoessentials.root.reload_error_afk", fMsg));
            }

            // Reload JailManager
            totalCount++;
            try {
                com.zerog.neoessentials.moderation.JailManager.getInstance().reload();
                NeoLog.info(LOGGER, LogCategory.COMMANDS, "✓ Jail system reloaded");
                successCount++;
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.COMMANDS, "✗ Failed to reload jail system: {}", e.getMessage(), e);
                final String fMsg = e.getMessage();
                source.sendFailure(MessageUtil.warning("commands.neoessentials.root.reload_error_jail", fMsg));
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
                NeoLog.info(LOGGER, LogCategory.COMMANDS, "✓ Tablist system reloaded");
                successCount++;
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.COMMANDS, "✗ Failed to reload tablist system: {}", e.getMessage(), e);
                final String fMsg = e.getMessage();
                source.sendFailure(MessageUtil.warning("commands.neoessentials.root.reload_error_tablist", fMsg));
            }

            // Reload ScoreboardManager (had the identical "config changed, /neoe reload does
            // nothing" gap tablist just got fixed for above — ConfigManager.loadAll() only
            // clears the JSON cache, it never told ScoreboardManager's own in-memory board
            // list to re-parse scoreboard.json, so edited boards/titles/lines stayed stale
            // until an explicit /scoreboard reload or a full restart).
            totalCount++;
            try {
                com.zerog.neoessentials.sidebar.ScoreboardManager scoreboardMgr =
                    com.zerog.neoessentials.sidebar.ScoreboardManager.getInstance();
                scoreboardMgr.loadConfig();
                // Push the updated boards to all online players immediately
                net.minecraft.server.MinecraftServer scoreboardReloadServer =
                    net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                if (scoreboardReloadServer != null) {
                    scoreboardMgr.updateAll(scoreboardReloadServer);
                }
                NeoLog.info(LOGGER, LogCategory.COMMANDS, "✓ Scoreboard system reloaded");
                successCount++;
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.COMMANDS, "✗ Failed to reload scoreboard system: {}", e.getMessage(), e);
                final String fMsg = e.getMessage();
                source.sendFailure(MessageUtil.warning("commands.neoessentials.root.reload_error_scoreboard", fMsg));
            }

            // Recover EconomyManager if it never finished initializing at boot (e.g. a config
            // that couldn't be parsed yet at the moment the singleton was first touched) — see
            // EconomyManager.initializeIfEnabled()'s javadoc. Unlike permissions this has no
            // loud failure mode: balances keep working perfectly in memory, they just silently
            // never persist, so there's nothing to visibly "fail to reload" — only something to
            // quietly recover if it's actually broken.
            totalCount++;
            try {
                if (com.zerog.neoessentials.economy.managers.EconomyManager.getInstance().reinitialize()) {
                    NeoLog.warn(LOGGER, LogCategory.COMMANDS, "Economy system was never fully initialized — re-initialized from scratch");
                }
                NeoLog.info(LOGGER, LogCategory.COMMANDS, "✓ Economy system reloaded");
                successCount++;
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.COMMANDS, "✗ Failed to reload economy system: {}", e.getMessage(), e);
                final String fMsg = e.getMessage();
                source.sendFailure(MessageUtil.warning("commands.neoessentials.root.reload_error_economy", fMsg));
            }

            // Reload HologramScheduler's tick rates (hologram.refreshInterval/animationInterval)
            totalCount++;
            try {
                com.zerog.neoessentials.hologram.HologramScheduler.restart();
                NeoLog.info(LOGGER, LogCategory.COMMANDS, "✓ Hologram scheduler reloaded");
                successCount++;
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.COMMANDS, "✗ Failed to reload hologram scheduler: {}", e.getMessage(), e);
                final String fMsg = e.getMessage();
                source.sendFailure(MessageUtil.warning("commands.neoessentials.root.reload_error_hologram", fMsg));
            }

            // Reload WorthManager (item sell prices)
            totalCount++;
            try {
                com.zerog.neoessentials.economy.worth.WorthManager.getInstance().reload();
                NeoLog.info(LOGGER, LogCategory.COMMANDS, "✓ Worth system reloaded");
                successCount++;
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.COMMANDS, "✗ Failed to reload worth system: {}", e.getMessage(), e);
                final String fMsg = e.getMessage();
                source.sendFailure(MessageUtil.warning("commands.neoessentials.root.reload_error_worth", fMsg));
            }

            // Reload RulesCommand (server rules from rules_data.json)
            totalCount++;
            try {
                com.zerog.neoessentials.util.commands.RulesCommand.reload();
                NeoLog.info(LOGGER, LogCategory.COMMANDS, "✓ Rules system reloaded");
                successCount++;
            } catch (Exception e) {
                NeoLog.error(LOGGER, LogCategory.COMMANDS, "✗ Failed to reload rules system: {}", e.getMessage(), e);
                final String fMsg = e.getMessage();
                source.sendFailure(MessageUtil.warning("commands.neoessentials.root.reload_error_rules", fMsg));
            }

            // Build success message
            final int fSuccessCount = successCount, fTotalCount = totalCount;

            if (successCount == totalCount) {
                source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.root.reload_complete_success", fSuccessCount, fTotalCount), true);
            } else {
                source.sendSuccess(() -> MessageUtil.warning("commands.neoessentials.root.reload_complete_errors", fSuccessCount, fTotalCount), true);
            }

            // Command/module toggles are decided once at server start (whether a command is
            // even registered with the dispatcher) — reload only refreshes data, it can't add
            // or remove already-registered commands. Make that limitation visible instead of
            // letting an admin assume a config toggle silently "didn't work".
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.root.reload_modules_notice"), false);

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
                    NeoLog.info(LOGGER, LogCategory.COMMANDS, "✓ Command trees re-sent to {} online player(s)",
                        cmdServer.getPlayerList().getPlayerCount());
                }
            } catch (Exception e) {
                NeoLog.warn(LOGGER, LogCategory.COMMANDS, "Could not re-sync command trees to players after reload: {}", e.getMessage());
            }

            NeoLog.info(LOGGER, LogCategory.COMMANDS, "Configuration reload completed: {}/{} systems reloaded successfully by {}",
                successCount, totalCount, source.getTextName());
            return 1;
            
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.COMMANDS, "CRITICAL: Failed to reload configuration: {}", e.getMessage(), e);
            source.sendFailure(MessageUtil.error("commands.neoessentials.root.reload_error_config", e.getMessage()));
            return 0;
        }
    }

    private static int validateConfiguration(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<String> problems = ConfigSplitter.validateSplitConfigs();
        if (problems.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.root.config_valid"), false);
        } else {
            final int fCount = problems.size();
            source.sendSuccess(() -> MessageUtil.warning("commands.neoessentials.root.config_validation_problems", fCount), false);
            for (String problem : problems) {
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.root.config_problem_line", problem), false);
            }
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.config_repair_hint"), false);
        }
        return problems.isEmpty() ? 1 : 0;
    }

    private static int repairConfiguration(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!ConfigSplitter.isSplittingEnabled()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.root.config_repair_not_enabled"));
            return 0;
        }
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.config_repairing"), false);
        int repaired = ConfigSplitter.repairSplitConfigs();
        if (repaired == 0) {
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.root.config_repair_none_needed"), false);
        } else {
            final int r = repaired;
            source.sendSuccess(() -> MessageUtil.success(
                "commands.neoessentials.root.config_repaired_count", r), false);
        }
        List<String> remaining = ConfigSplitter.validateSplitConfigs();
        if (!remaining.isEmpty()) {
            final int fRemaining = remaining.size();
            source.sendSuccess(() -> MessageUtil.warning(
                "commands.neoessentials.root.config_repair_remaining", fRemaining), false);
            for (String p : remaining) {
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.root.config_problem_line", p), false);
            }
        }
        return 1;
    }

    private static int configStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        boolean splitEnabled = ConfigSplitter.isSplittingEnabled();
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.root.config_status_title"), false);
        final String fMode = splitEnabled ? "commands.neoessentials.root.config_status_mode_split" : "commands.neoessentials.root.config_status_mode_mono";
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.root.config_status_mode", MessageUtil.localize(fMode)), false);
        if (splitEnabled) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.root.config_status_files_header"), false);
            for (java.util.Map.Entry<String, java.util.List<String>> e :
                    ConfigSplitter.FILE_SECTIONS_MAP.entrySet()) {
                String file = e.getKey();
                java.io.File f = com.zerog.neoessentials.util.ResourceUtil.getConfigFile(file);
                boolean exists = f.exists();
                final String fPrefix = exists ? "  §a✔ " : "  §c✘ ";
                final String fJoined = String.join(", ", e.getValue());
                source.sendSuccess(() -> MessageUtil.component(
                    "commands.neoessentials.root.config_status_file_line", fPrefix, file, fJoined), false);
            }
            List<String> problems = ConfigSplitter.validateSplitConfigs();
            if (problems.isEmpty()) {
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.root.config_status_all_valid"), false);
            } else {
                final int n = problems.size();
                source.sendSuccess(() -> MessageUtil.component(
                    "commands.neoessentials.root.config_status_problems", n), false);
            }
        } else {
            java.io.File mainCfg = com.zerog.neoessentials.util.ResourceUtil.getConfigFile("config.json");
            final String fPresence = mainCfg.exists() ? "commands.neoessentials.root.config_status_mono_present" : "commands.neoessentials.root.config_status_mono_missing";
            source.sendSuccess(() -> MessageUtil.component(
                "commands.neoessentials.root.config_status_mono_file", MessageUtil.localize(fPresence)), false);
            source.sendSuccess(() -> MessageUtil.component(
                "commands.neoessentials.root.config_status_split_tip"), false);
        }
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.root.config_status_footer"), false);
        return 1;
    }

    private static int splitConfiguration(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        
        try {
            // Check if already using split configs
            if (ConfigSplitter.isSplittingEnabled()) {
                source.sendSuccess(() -> MessageUtil.warning("commands.neoessentials.root.config_split_already"), false);
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.config_split_already_info"), false);
                return 0;
            }

            final String fSeparator = "─".repeat(40);
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.config_split_separator", fSeparator), false);
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.config_split_migrating"), false);
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.config_split_separator", fSeparator), false);

            // Perform the migration
            boolean success = ConfigSplitter.migrateToSplitConfigs();

            if (success) {
                source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.root.config_split_success"), false);
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.config_split_info_header"), false);
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.config_split_file_main"), false);
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.config_split_file_commands"), false);
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.config_split_file_chat"), false);
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.config_split_file_teleportation"), false);
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.config_split_file_moderation"), false);
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.config_split_file_items"), false);
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.config_split_file_afk"), false);
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.config_split_file_security"), false);
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.config_split_file_tablist"), false);
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.config_split_file_discord_embed"), false);
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.config_split_backup_note"), false);
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.config_split_reload_hint"), false);

                NeoLog.info(LOGGER, LogCategory.COMMANDS, "Configuration split completed successfully by {}", source.getTextName());
                return 1;
            } else {
                source.sendFailure(MessageUtil.error("commands.neoessentials.root.config_split_failed"));
                return 0;
            }

        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.COMMANDS, "Failed to split configuration: {}", e.getMessage(), e);
            source.sendFailure(MessageUtil.error("commands.neoessentials.root.config_split_error", e.getMessage()));
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
            NeoLog.warn(LOGGER, LogCategory.COMMANDS, "Command '{}' is in registry but not in dispatcher - possible registration issue", commandName);
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
                NeoLog.warn(LOGGER, LogCategory.COMMANDS, "Command '{}' has unconsumed arguments: '{}'", commandString, parseResults.getReader().getRemaining());
            }
            
            // Execute the parsed command
            int result = dispatcher.execute(parseResults);
            NeoLog.debug(LOGGER, LogCategory.COMMANDS, "Successfully executed command '{}' with result: {}", commandString, result);
            return result;
            
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            // Handle command syntax errors gracefully
            NeoLog.warn(LOGGER, LogCategory.COMMANDS, "Command syntax error for '{}': {}", commandString, e.getMessage());
            source.sendFailure(MessageUtil.error("commands.neoessentials.root.syntax_error", commandString, e.getMessage()));
            return 0;
        } catch (Exception e) {
            // Handle any other execution errors
            NeoLog.error(LOGGER, LogCategory.COMMANDS, "Failed to execute command '{}': {}", commandString, e.getMessage(), e);
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
            String localizedDesc = getLocalizedDescription(info);
            if (info.hasAliases()) {
                String aliases = String.join(", /", info.getAliases());
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.root.command_with_aliases", 
                    info.getName(), aliases, localizedDesc), false);
            } else {
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.root.command_simple", 
                    info.getName(), localizedDesc), false);
            }
        }
        
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.root.help_footer"), false);
        
        return 1;
    }
    
    /**
     * Get a localized description for a command.
     * Checks for a translation key "commands.neoessentials.cmd.NAME.description" first;
     * falls back to the registered English description if not found.
     */
    private static String getLocalizedDescription(CommandRegistry.CommandInfo cmd) {
        String descKey = "commands.neoessentials.cmd." + cmd.getName().toLowerCase() + ".description";
        if (com.zerog.neoessentials.util.MessageUtil.hasTranslation(descKey)) {
            return com.zerog.neoessentials.util.MessageUtil.localize(descKey);
        }
        String fallback = cmd.getDescription();
        return (fallback != null && !fallback.isEmpty()) ? fallback : "NeoEssentials command";
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
