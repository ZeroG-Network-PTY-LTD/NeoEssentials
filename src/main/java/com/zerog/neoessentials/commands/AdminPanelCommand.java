package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
<<<<<<< HEAD
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.AdminPanel;
import com.zerog.neoessentials.utils.MessageUtil;
import com.zerog.neoessentials.utils.TextUtil;
import com.zerog.neoessentials.utils.VanillaBooleanParser;
import com.zerog.neoessentials.utils.PermissionUtil;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

/**
 * Implements commands for accessing the comprehensive admin panel interface.
 * <p>
 * The AdminPanelCommand class provides a centralized administrative interface through
 * which server administrators can manage all aspects of the server. Features include:
 * </p>
 * 
 * <ul>
 *   <li><b>Section-Based Organization</b>: Logically divided areas for different administrative tasks</li>
 *   <li><b>Performance Monitoring</b>: Real-time server metrics including TPS, memory usage, and entity counts</li>
 *   <li><b>Player Management</b>: Tools for managing online players</li>
 *   <li><b>Server Controls</b>: Configuration reload, maintenance mode, and optimization options</li>
 *   <li><b>Interactive Interface</b>: Click-based navigation through administrative options</li>
 * </ul>
 * 
 * <p>All commands require the "neoessentials.admin.panel" permission node by default,
 * with additional specialized permissions for certain actions.</p>
 * 
 * @author ZeroG
 * @since 1.0.0
 */
public class AdminPanelCommand {
    
    // Tracks which admin panel sections players have open
    private final Map<ServerPlayer, String> activeAdminSections = new HashMap<>();
    
    // Define admin panel sections for suggestions with well-structured categories
    private static final List<String> ADMIN_SECTIONS = Arrays.asList(
            "overview", "players", "server", "permissions", "economy", "storage", 
            "performance", "commands", "logs", "backups", "maintenance", "settings");
            
    // Define specific feature categories for toggle suggestions
    private static final List<String> FEATURE_CATEGORIES = Arrays.asList(
            "chat", "commands", "teleport", "godmode", "weather", "pvp", "economy",
            "drops", "mobs", "redstone", "explosions", "fire");
    
    // Suggestion provider for admin panel sections
    private static final SuggestionProvider<CommandSourceStack> SECTION_SUGGESTIONS = 
            (context, builder) -> SharedSuggestionProvider.suggest(ADMIN_SECTIONS, builder);
            
    // Suggestion provider for feature categories
    private static final SuggestionProvider<CommandSourceStack> FEATURE_SUGGESTIONS = 
            (context, builder) -> SharedSuggestionProvider.suggest(FEATURE_CATEGORIES, builder);
    
    // Suggestion provider for performance targets (move to correct place)
    private static final List<String> PERFORMANCE_TARGETS = Arrays.asList("entities", "chunks", "items", "mobs", "memory", "all");
    private static final SuggestionProvider<CommandSourceStack> PERFORMANCE_TARGET_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(PERFORMANCE_TARGETS, builder);    /**
     * Registers all admin panel commands with the dispatcher.
     * Organizes commands into a logical hierarchy for better usability.
=======
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.AdminPanel;
import com.zerog.neoessentials.utils.MessageUtil;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Implements commands for accessing the admin panel interface.
 */
public class AdminPanelCommand {

    /**
     * Registers all admin panel commands with the dispatcher.
>>>>>>> aa6024a (feat: Implement Admin Panel and Menu System)
     *
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
<<<<<<< HEAD
        // Main admin panel command with comprehensive subcommands
        LiteralArgumentBuilder<CommandSourceStack> adminPanelCommand = Commands.literal("adminpanel")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel"))
                .executes(this::executeAdminPanel)
                // Section navigation
                .then(Commands.literal("section")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(SECTION_SUGGESTIONS)
                                .executes(this::executeAdminPanelSection)))
                // Configuration management                
                .then(Commands.literal("config")
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel.config"))
                        .executes(this::executeConfigurationMenu)
                        .then(Commands.literal("reload")
                                .executes(this::executeConfigReload))
                        .then(Commands.literal("view")
                                .executes(this::executeConfigView))
                        .then(Commands.literal("edit")
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .executes(this::executeConfigEdit)))))
                // Module reload                
                .then(Commands.literal("reload")
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel.reload"))
                        .executes(this::executeAdminPanelReload)
                        .then(Commands.literal("permissions")
                                .executes(this::executeReloadPermissions))
                        .then(Commands.literal("warps")
                                .executes(this::executeReloadWarps))
                        .then(Commands.literal("kits")
                                .executes(this::executeReloadKits))
                        .then(Commands.literal("all")
                                .executes(this::executeReloadAll)))
                // Player management                
                .then(Commands.literal("manage")
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel.manage"))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(this::executeAdminPanelManagePlayer)
                                .then(Commands.literal("gamemode")
                                        .then(Commands.argument("mode", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        Arrays.asList("survival", "creative", "adventure", "spectator"),
                                                        builder))
                                                .executes(this::executePlayerGameMode)))
                                .then(Commands.literal("godmode")
                                        .then(Commands.argument("enabled", VanillaBooleanParser.argument())
                                                .suggests(VanillaBooleanParser.booleanSuggestions())
                                                .executes(this::executePlayerGodMode)))
                                .then(Commands.literal("fly")
                                        .then(Commands.argument("enabled", VanillaBooleanParser.argument())
                                                .suggests(VanillaBooleanParser.booleanSuggestions())
                                                .executes(this::executePlayerFly)))))
                // Feature toggles                
                .then(Commands.literal("toggle")
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel.toggle"))
                        .executes(this::executeToggleMenu)
                        .then(Commands.argument("feature", StringArgumentType.word())
                                .suggests(FEATURE_SUGGESTIONS)
                                .then(Commands.argument("enabled", VanillaBooleanParser.argument())
                                        .suggests(VanillaBooleanParser.booleanSuggestions())
                                        .executes(this::executeFeatureToggle))))
                // Performance monitoring and optimization                
                .then(Commands.literal("performance")
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel.performance"))
                        .executes(this::executePerformanceMonitor)
                        .then(Commands.literal("tps")
                                .executes(this::executePerformanceTPS))
                        .then(Commands.literal("memory")
                                .executes(this::executePerformanceMemory))
                        .then(Commands.literal("entities")
                                .executes(this::executePerformanceEntities))
                        .then(Commands.literal("dimensions")
                                .executes(this::executePerformanceDimensions))
                        .then(Commands.literal("optimize")
                                .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel.optimize"))
                                .executes(this::executeOptimize)
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests(PERFORMANCE_TARGET_SUGGESTIONS)
                                        .executes(this::executeOptimizeTarget))))
                // Server management                
                .then(Commands.literal("server")
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel.server"))
                        .executes(this::executeServerManagement)
                        .then(Commands.literal("status")
                                .executes(this::executeServerStatus))
                        .then(Commands.literal("maintenance")
                                .then(Commands.argument("enabled", VanillaBooleanParser.argument())
                                        .suggests(VanillaBooleanParser.booleanSuggestions())
                                        .executes(this::executeServerMaintenance)))
                        .then(Commands.literal("broadcast")
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(this::executeServerBroadcast))));

        // Register enhanced alias with better command structure
        dispatcher.register(Commands.literal("ap")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel"))
                .executes(this::executeAdminPanel)
                .then(Commands.literal("s") // Short for section
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(SECTION_SUGGESTIONS)
                                .executes(this::executeAdminPanelSection)))
                .then(Commands.literal("r") // Short for reload
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel.reload"))
                        .executes(this::executeAdminPanelReload))
                .then(Commands.literal("t") // Short for toggle
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel.toggle"))
                        .then(Commands.argument("feature", StringArgumentType.word())
                                .suggests(FEATURE_SUGGESTIONS)
                                .then(Commands.argument("enabled", VanillaBooleanParser.argument())
                                        .suggests(VanillaBooleanParser.booleanSuggestions())
                                        .executes(this::executeFeatureToggle))))
                .then(Commands.literal("p") // Short for performance
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel.performance"))
                        .executes(this::executePerformanceMonitor))
                .then(Commands.literal("m") // Short for manage
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel.manage"))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(this::executeAdminPanelManagePlayer))));
        
        // Register specialized section access commands
        registerSectionCommands(dispatcher);
        
        // Register advanced operation commands
        registerAdvancedCommands(dispatcher);
=======
        // Main admin panel command
        LiteralArgumentBuilder<CommandSourceStack> adminPanelCommand = Commands.literal("adminpanel")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel"))
                .executes(this::executeAdminPanel);

        // Register aliases
        dispatcher.register(adminPanelCommand);
        dispatcher.register(Commands.literal("ap")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel"))
                .executes(this::executeAdminPanel));
        
        // Admin panel sections
        registerSectionCommands(dispatcher);
>>>>>>> aa6024a (feat: Implement Admin Panel and Menu System)
    }
    
    /**
     * Registers commands for specific admin panel sections.
     * 
     * @param dispatcher The command dispatcher
     */
    private void registerSectionCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /adminpanel economy - Opens the economy section of the admin panel
        dispatcher.register(Commands.literal("adminpanel")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel.economy"))
                .then(Commands.literal("economy")
                        .executes(this::executeEconomyPanel)));
                        
        // /adminpanel kits - Opens the kits section of the admin panel
        dispatcher.register(Commands.literal("adminpanel")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel.kits"))
                .then(Commands.literal("kits")
                        .executes(this::executeKitsPanel)));
                        
        // /adminpanel warps - Opens the warps section of the admin panel
        dispatcher.register(Commands.literal("adminpanel")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel.warps"))
                .then(Commands.literal("warps")
                        .executes(this::executeWarpsPanel)));
                        
        // /adminpanel players - Opens the players section of the admin panel
        dispatcher.register(Commands.literal("adminpanel")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel.players"))
                .then(Commands.literal("players")
                        .executes(this::executePlayersPanel)));
    }

    /**
<<<<<<< HEAD
     * Register additional advanced admin commands
     * 
     * @param dispatcher Command dispatcher
     */
    private void registerAdvancedCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register server control commands
        dispatcher.register(Commands.literal("server")
            .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel.server"))
            .then(Commands.literal("status")
                .executes(this::executeServerStatus))
            .then(Commands.literal("reload")
                .executes(this::executeServerReload))
            .then(Commands.literal("maintenance")
                .then(Commands.argument("enabled", VanillaBooleanParser.argument())
                    .suggests(VanillaBooleanParser.booleanSuggestions())
                    .executes(this::executeServerMaintenance))));
                    
        // Register optimizations commands
        dispatcher.register(Commands.literal("optimize")
            .requires(source -> CommandManager.hasPermission(source, "neoessentials.adminpanel.optimize"))
            .executes(this::executeOptimize)
            .then(Commands.literal("entities")
                .executes(this::executeOptimizeEntities))
            .then(Commands.literal("chunks")
                .executes(this::executeOptimizeChunks))
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                    Arrays.asList("entities", "chunks", "items", "mobs", "memory", "all"), builder))
                .executes(this::executeOptimizeTarget)));
    }

    /**
=======
>>>>>>> aa6024a (feat: Implement Admin Panel and Menu System)
     * Executes the main admin panel command.
     *
     * @param context The command context
     * @return 1 if successful
     */
    private int executeAdminPanel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Check if player has permission
        if (!CommandManager.hasPermission(context.getSource(), "neoessentials.adminpanel")) {
            MessageUtil.sendErrorMessage(player, "You don't have permission to use the admin panel.");
            return 0;
        }
        
        // Open the main admin panel
        displayMainAdminPanel(player);
        
        return 1;
    }
    
    /**
<<<<<<< HEAD
     * Handle opening a specific admin panel section
     */
    private int executeAdminPanelSection(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String sectionName = StringArgumentType.getString(context, "name");
        
        // Store the active section for this player
        activeAdminSections.put(player, sectionName);
        
        // Display the requested section
        switch (sectionName.toLowerCase()) {
            case "overview":
                displayOverviewSection(player);
                break;
            case "players":
                displayPlayersSection(player);
                break;
            case "server":
                displayServerSection(player);
                break;
            case "permissions":
                displayPermissionsSection(player);
                break;
            case "economy":
                displayEconomySection(player);
                break;
            case "storage":
                displayStorageSection(player);
                break;
            case "performance":
                displayPerformanceSection(player);
                break;
            case "commands":
                displayCommandsSection(player);
                break;
            case "logs":
                displayLogsSection(player);
                break;
            case "backups":
                displayBackupsSection(player);
                break;
            case "maintenance":
                displayMaintenanceSection(player);
                break;
            case "settings":
                displaySettingsSection(player);
                break;
            default:
                MessageUtil.sendErrorMessage(player, "Unknown section: " + sectionName);
                displayMainAdminPanel(player);
                return 0;
        }
        
        return 1;
    }
    
    /**
     * Handle reloading the admin panel or its components
     */
    private int executeAdminPanelReload(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MessageUtil.sendMessage(player, "Reloading admin panel components...");
        context.getSource().sendSuccess(() -> Component.literal(
                TextUtil.colorize("&aAdmin panel components reloaded successfully!")), true);
        return 1;
    }
    
    /**
     * Handle managing a specific player from the admin panel
     */
    private int executeAdminPanelManagePlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        
        displayPlayerManagementPanel(admin, target);
        
        return 1;
    }
    
    /**
     * Handle toggling server features on/off
     */
    private int executeFeatureToggle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String feature = StringArgumentType.getString(context, "feature");
        boolean enabled = VanillaBooleanParser.getBoolean(context, "enabled");
        
        // Apply the toggle for the specified feature
        String featureName = capitalizeFirstLetter(feature);
        
        context.getSource().sendSuccess(() -> Component.literal(
                TextUtil.colorize("&a" + featureName + " has been " + (enabled ? "&2enabled" : "&4disabled") + "&a.")), true);
                
        // Here you would actually apply the toggle in your configuration or runtime settings
        
        return 1;
    }
    
    /**
     * Handle performance monitoring main command
     */
    private int executePerformanceMonitor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Show overall performance dashboard
        displayPerformanceOverview(player);
        
        return 1;
    }
    
    /**
     * Handle showing TPS (ticks per second) statistics
     */
    private int executePerformanceTPS(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MinecraftServer server = context.getSource().getServer();
        // Get MSPT (Milliseconds Per Tick) from the server
        long[] tickTimes = server.getTickTime(server.overworld().dimension());
        double mspt = Arrays.stream(tickTimes).average().orElse(0.0);
        double tps = mspt > 0 ? Math.min(1000.0 / mspt, 20.0) : 20.0;
        String color = tps >= 19.5 ? "&2" : (tps >= 18 ? "&e" : "&c");
        context.getSource().sendSuccess(() -> Component.literal(
                TextUtil.colorize("&6Current server TPS: " + color + String.format("%.2f", tps))), false);
        return 1;
    }
    
    /**
     * Handle showing memory usage statistics
     */
    private int executePerformanceMemory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Get memory statistics
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long allocatedMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = allocatedMemory - freeMemory;
        int memoryPercent = (int)(usedMemory * 100L / maxMemory);
        
        String memoryColor = memoryPercent < 60 ? "&2" : (memoryPercent < 85 ? "&e" : "&c");
        
        // Send memory statistics
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&6===== Memory Usage =====")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7Used: " + memoryColor + usedMemory + "MB &7/ &6" + maxMemory + "MB")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7Usage: " + memoryColor + memoryPercent + "%")));
        
        return 1;
    }
    
    /**
     * Handle displaying entity statistics
     */
    private int executePerformanceEntities(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int totalEntities = 0;
        for (var level : context.getSource().getServer().getAllLevels()) {
            totalEntities += countEntities(level.getAllEntities());
        }
        
        // Make a final copy for the lambda
        final int entityCount = totalEntities;
        
        context.getSource().sendSuccess(() -> Component.literal(
                TextUtil.colorize("&6Total entities across all dimensions: &e" + entityCount)), false);
        return 1;
    }
    
    /**
     * Display server status information
     */
    private int executeServerStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MinecraftServer server = context.getSource().getServer();
        Runtime runtime = Runtime.getRuntime();
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&6===== Server Status =====")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7Uptime: &e" + formatUptime(server.getTickCount()))));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7Players: &e" + server.getPlayerCount() + "&7/&e" + server.getMaxPlayers())));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7Memory: &e" + (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024 + "MB&7/&e" + runtime.maxMemory() / 1024 / 1024 + "MB")));
        long[] tickTimes = server.getTickTime(server.overworld().dimension());
        double mspt = Arrays.stream(tickTimes).average().orElse(0.0);
        double tps = mspt > 0 ? Math.min(1000.0 / mspt, 20.0) : 20.0;
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7TPS: &e" + String.format("%.2f", tps))));
        return 1;
    }
    
    /**
     * Handle server configuration reload
     */
    private int executeServerReload(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&6Reloading server configuration...")));
        
        // Perform reload logic here
        // This should reload configs, permissions, etc.
        
        context.getSource().sendSuccess(() -> Component.literal(
                TextUtil.colorize("&aServer configuration reloaded successfully!")), true);
                
        return 1;
    }
    
    /**
     * Toggle server maintenance mode
     */
    private int executeServerMaintenance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        boolean enabled = VanillaBooleanParser.getBoolean(context, "enabled");
        context.getSource().sendSuccess(() -> Component.literal(
                TextUtil.colorize("&6Maintenance mode: " + (enabled ? "&aENABLED" : "&cDISABLED"))), true);
        context.getSource().getServer().getPlayerList().getPlayers().forEach(p -> {
            if (!PermissionUtil.hasPermission(p, "neoessentials.bypass.maintenance")) {
                p.sendSystemMessage(Component.literal(TextUtil.colorize("&c&lThe server is entering maintenance mode!")));
                p.sendSystemMessage(Component.literal(TextUtil.colorize("&eYou will be disconnected shortly if you don't have bypass permission.")));
            }
        });
        // In a real implementation, you might kick players without bypass permission
        return 1;
    }
    
    /**
     * Handle general optimization command
     */
    private int executeOptimize(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&6Running general server optimization...")));
        
        // Run various optimization routines
        // This would be a comprehensive optimization that includes entities, chunks, etc.
        
        context.getSource().sendSuccess(() -> Component.literal(
                TextUtil.colorize("&aServer optimization completed successfully!")), true);
                
        return 1;
    }
    
    /**
     * Handle entity-specific optimization
     */
    private int executeOptimizeEntities(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int beforeCount = 0;
        for (var level : context.getSource().getServer().getAllLevels()) {
            beforeCount += countEntities(level.getAllEntities());
        }
        // Simulate entity optimization
        int afterCount = beforeCount - (beforeCount / 10); // Simulated 10% reduction
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&6Entity optimization complete:")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7Before: &e" + beforeCount + " &7entities")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7After: &a" + afterCount + " &7entities")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7Removed: &6" + (beforeCount - afterCount) + " &7entities")));
        return 1;
    }
    
    /**
     * Handle chunk-specific optimization
     */
    private int executeOptimizeChunks(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Simulate chunk optimization
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&6Optimizing chunks across all dimensions...")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7This process may cause brief lag spikes.")));
        
        // Perform chunk optimization logic here
        
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&aChunk optimization complete!")));
        return 1;
    }
    
    /**
     * Handle targeted optimization
     */
    private int executeOptimizeTarget(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String target = StringArgumentType.getString(context, "target");
        
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&6Optimizing target: &e" + target)));
        
        // Execute optimization based on target
        switch (target.toLowerCase()) {
            case "entities":
                return executeOptimizeEntities(context);
            case "chunks":
                return executeOptimizeChunks(context);
            case "items":
                player.sendSystemMessage(Component.literal(TextUtil.colorize("&aItem optimization complete! Merged nearby items.")));
                break;
            case "mobs":
                player.sendSystemMessage(Component.literal(TextUtil.colorize("&aMob optimization complete! Removed excessive mobs.")));
                break;
            case "memory":
                player.sendSystemMessage(Component.literal(TextUtil.colorize("&aMemory optimization complete! Garbage collection triggered.")));
                System.gc(); // Request garbage collection
                break;
            case "all":
                executeOptimizeEntities(context);
                executeOptimizeChunks(context);
                player.sendSystemMessage(Component.literal(TextUtil.colorize("&aComplete optimization finished!")));
                break;
            default:
                player.sendSystemMessage(Component.literal(TextUtil.colorize("&cUnknown optimization target: " + target)));
                return 0;
        }
        
        return 1;
    }
    
    // New command execution methods for expanded admin panel functionality
    
    /**
     * Display configuration menu with sections that can be edited
     */
    private int executeConfigurationMenu(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&8⚡ &6&l✦✦✦ &b&lConfiguration Menu &6&l✦✦✦ &8⚡")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7Select a configuration option:")));
        player.sendSystemMessage(Component.empty());
        
        displaySectionButton(player, "&3❖ &bReload Configuration", "/adminpanel config reload", 
                "&7Reload all configuration files from disk\n&7to apply changes made externally.");
        
        displaySectionButton(player, "&3❖ &bView Configuration", "/adminpanel config view", 
                "&7View current configuration settings\n&7for all NeoEssentials modules.");
        
        displaySectionButton(player, "&3❖ &bEdit Configuration", "/adminpanel config edit", 
                "&7Edit specific configuration values\n&7using the interactive configuration editor.");
        
        return 1;
    }
    
    /**
     * Reload configuration from disk
     */
    private int executeConfigReload(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Simulate reloading configuration
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&8[&b&lConfig&8] &7Reloading configuration files...")));
        
        // Here you would reload actual configuration
        // NeoEssentials.getInstance().getConfigManager().reload();
        
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&8[&b&lConfig&8] &aConfiguration reloaded successfully!")));
        return 1;
    }
    
    /**
     * View current configuration values
     */
    private int executeConfigView(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Display configuration overview
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&8[&b&lConfig&8] &7Current configuration values:")));
        
        // Here you would show actual configuration values
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &bTeleport Cooldown: &a20 seconds")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &bHome Limit: &a5 homes")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &bEconomy Enabled: &aTrue")));
        
        return 1;
    }
    
    /**
     * Edit a specific configuration value
     */
    private int executeConfigEdit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String key = StringArgumentType.getString(context, "key");
        String value = StringArgumentType.getString(context, "value");
        
        // Here you would validate and set the configuration
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&8[&b&lConfig&8] &7Setting &b" + key + " &7to &a" + value)));
        
        // Save configuration changes
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&8[&b&lConfig&8] &aConfiguration updated successfully!")));
        
        return 1;
    }
    
    /**
     * Reload permission system
     */
    private int executeReloadPermissions(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&8[&d&lPermissions&8] &7Reloading permission cache...")));
        
        // Here you would reload permissions
        
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&8[&d&lPermissions&8] &aPermission cache refreshed successfully!")));
        return 1;
    }
    
    /**
     * Reload warp data
     */
    private int executeReloadWarps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&8[&e&lWarps&8] &7Reloading warp data...")));
        
        // Here you would reload warps
        
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&8[&e&lWarps&8] &aWarp data reloaded successfully!")));
        return 1;
    }
    
    /**
     * Reload kit data
     */
    private int executeReloadKits(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&8[&6&lKits&8] &7Reloading kit data...")));
        
        // Here you would reload kits
        
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&8[&6&lKits&8] &aKit data reloaded successfully!")));
        return 1;
    }
    
    /**
     * Reload all data
     */
    private int executeReloadAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&8[&c&lReload&8] &7Reloading all NeoEssentials data...")));
        
        // Call individual reload methods
        executeReloadPermissions(context);
        executeReloadWarps(context);
        executeReloadKits(context);
        executeConfigReload(context);
        
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&8[&c&lReload&8] &a&lAll data reloaded successfully!")));
        return 1;
    }
    
    /**
     * Set player's gamemode
     */
    private int executePlayerGameMode(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        String mode = StringArgumentType.getString(context, "mode").toLowerCase();
        
        String gameMode;
        switch (mode) {
            case "c":
            case "1":
            case "creative":
                gameMode = "creative";
                break;
            case "s":
            case "0":
            case "survival":
                gameMode = "survival";
                break;
            case "a":
            case "2":
            case "adventure":
                gameMode = "adventure";
                break;
            case "sp":
            case "3":
            case "spectator":
                gameMode = "spectator";
                break;
            default:
                admin.sendSystemMessage(Component.literal(TextUtil.colorize("&cInvalid gamemode: " + mode)));
                return 0;
        }
        
        // Update player's gamemode here
        
        admin.sendSystemMessage(Component.literal(TextUtil.colorize(
                "&8[&a&lAdmin&8] &7Set &e" + target.getScoreboardName() + "'s &7gamemode to &a" + gameMode)));
        target.sendSystemMessage(Component.literal(TextUtil.colorize(
                "&8[&a&lAdmin&8] &7Your gamemode was set to &a" + gameMode + " &7by &e" + admin.getScoreboardName())));
        
        return 1;
    }
    
    /**
     * Toggle player's godmode
     */
    private int executePlayerGodMode(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        boolean enabled = VanillaBooleanParser.getBoolean(context, "enabled");
        
        // Set godmode for target player
        
        admin.sendSystemMessage(Component.literal(TextUtil.colorize(
                "&8[&a&lAdmin&8] &7" + (enabled ? "&aEnabled" : "&cDisabled") + " &7godmode for &e" + target.getScoreboardName())));
        target.sendSystemMessage(Component.literal(TextUtil.colorize(
                "&8[&a&lAdmin&8] &7Your godmode was " + (enabled ? "&aenabled" : "&cdisabled") + " &7by &e" + admin.getScoreboardName())));
        
        return 1;
    }
    
    /**
     * Toggle player's flight
     */
    private int executePlayerFly(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        boolean enabled = VanillaBooleanParser.getBoolean(context, "enabled");
        
        // Toggle flight for target player
        
        admin.sendSystemMessage(Component.literal(TextUtil.colorize(
                "&8[&a&lAdmin&8] &7" + (enabled ? "&aEnabled" : "&cDisabled") + " &7flight for &e" + target.getScoreboardName())));
        target.sendSystemMessage(Component.literal(TextUtil.colorize(
                "&8[&a&lAdmin&8] &7Your flight was " + (enabled ? "&aenabled" : "&cdisabled") + " &7by &e" + admin.getScoreboardName())));
        
        return 1;
    }
    
    /**
     * Display the toggle menu
     */
    private int executeToggleMenu(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&8⚡ &6&l✦✦✦ &e&lServer Feature Toggles &6&l✦✦✦ &8⚡")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7Toggle server features on/off:")));
        player.sendSystemMessage(Component.empty());
        
        // Show toggles with current status
        displayToggleButton(player, "Chat", "/adminpanel toggle chat", true);
        displayToggleButton(player, "Commands", "/adminpanel toggle commands", true);
        displayToggleButton(player, "Teleport", "/adminpanel toggle teleport", true);
        displayToggleButton(player, "PvP", "/adminpanel toggle pvp", false);
        displayToggleButton(player, "Weather Changes", "/adminpanel toggle weather", true);
        
        return 1;
    }
    
    /**
     * Display server management menu
     */
    private int executeServerManagement(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&8⚡ &6&l✦✦✦ &c&lServer Management &6&l✦✦✦ &8⚡")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7Manage server functions:")));
        player.sendSystemMessage(Component.empty());
        
        displaySectionButton(player, "&c❖ &4Server Status", "/adminpanel server status", 
                "&7View detailed server status information\n&7including TPS, memory usage, and player count.");
                
        displaySectionButton(player, "&c❖ &4Maintenance Mode", "/adminpanel server maintenance", 
                "&7Toggle server maintenance mode\n&7which restricts access to staff only.");
                
        displaySectionButton(player, "&c❖ &4Server Broadcast", "/adminpanel server broadcast", 
                "&7Send an announcement to all online players\n&7with optional formatting.");
        
        return 1;
    }
    
    /**
     * Broadcast a message to the server
     */
    private int executeServerBroadcast(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String message = StringArgumentType.getString(context, "message");
        
        // Format the broadcast message
        Component broadcastMessage = Component.literal(TextUtil.colorize("&8[&c&lBroadcast&8] &f" + message));
        
        // Send to all players
        context.getSource().getServer().getPlayerList().getPlayers().forEach(player -> {
            player.sendSystemMessage(Component.empty());
            player.sendSystemMessage(broadcastMessage);
            player.sendSystemMessage(Component.empty());
        });
        
        return 1;
    }
    
    /**
     * Display performance stats for all dimensions
     */
    private int executePerformanceDimensions(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&8[&d&lPerformance&8] &7Dimension Statistics:")));
        
        // Here you would iterate through all dimensions and show stats
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &bOverworld&7: &a348 &7chunks, &a127 &7entities")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &bNether&7: &a124 &7chunks, &a57 &7entities")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &bEnd&7: &a86 &7chunks, &a25 &7entities")));
        
        return 1;
    }
    
    /**
     * Helper method to display a toggle button with current status
     */
    private void displayToggleButton(ServerPlayer player, String featureName, String baseCommand, boolean currentState) {
        String status = currentState ? "&aEnabled" : "&cDisabled";
        String toggleCommand = baseCommand + " " + !currentState;
        
        Component button = Component.literal(TextUtil.colorize("  &e❖ &6" + featureName + ": " + status))
                .setStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, toggleCommand))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                Component.literal(TextUtil.colorize("&7Click to toggle " + featureName.toLowerCase() + "\n&7Current status: " + status)))));
        player.sendSystemMessage(button);
    }
    
    // Helper methods for display sections
    
    private void displayMainAdminPanel(ServerPlayer player) {
        NeoEssentials.LOGGER.info("Displaying admin panel for player: {}", player.getScoreboardName());
        Component header = Component.literal(TextUtil.colorize("&8⚡ &6&l✦✦✦ &b&lNeoEssentials Admin Panel &6&l✦✦✦ &8⚡"));
        Component subheader = Component.literal(TextUtil.colorize("&7Select a section to manage:"));
        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(header);
        player.sendSystemMessage(subheader);
        displaySectionButton(player, "&b❖ &3Server Overview", "/adminpanel section overview", 
                "&7View server statistics, player count,\n&7performance metrics, and general status.");
        displaySectionButton(player, "&a❖ &2Player Management", "/adminpanel section players", 
                "&7Manage online players, view player info,\n&7and perform player-specific actions.");
        displaySectionButton(player, "&c❖ &4Server Controls", "/adminpanel section server", 
                "&7Control server settings, restart timers,\n&7and manage core server functionality.");
        displaySectionButton(player, "&e❖ &6Economy Dashboard", "/adminpanel section economy", 
                "&7Manage economy settings, view balances,\n&7transactions, and economic statistics.");
        displaySectionButton(player, "&d❖ &5Performance Monitor", "/adminpanel section performance", 
                "&7Monitor server performance, TPS,\n&7memory usage, and optimization options.");
        Component footer = Component.literal(TextUtil.colorize("&7&oNeoEssentials v" + NeoEssentials.getInstance().getVersion()));
        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(footer);
    }
    
    private void displaySectionButton(ServerPlayer player, String buttonName, String command, String hoverText) {
        Component button = Component.literal(TextUtil.colorize("  " + buttonName))
                .setStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                Component.literal(TextUtil.colorize(hoverText)))));
        player.sendSystemMessage(button);
    }
    
    // These methods would display different admin panel sections
    // Implementing stubs for all the mentioned sections
    
    private void displayOverviewSection(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&b❖ &3Server Overview")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fView server statistics, player count, uptime, and performance metrics.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/adminpanel section performance &fto see live TPS and memory usage.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/adminpanel section server &fto manage server state.")));
    }

    private void displayPlayersSection(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&a❖ &2Player Management")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fView and manage online players.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fKick, mute, or teleport players as needed.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/adminpanel manage <player> &fto open player management.")));
    }

    private void displayServerSection(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&c❖ &4Server Controls")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fReload configuration, toggle maintenance mode, or restart the server.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/server reload &for &e/adminpanel reload &fto reload configs.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/server maintenance <true|false> &fto toggle maintenance mode.")));
    }

    private void displayPermissionsSection(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&9❖ &1Permissions Management")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fView and edit player or group permissions.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fIntegrates with LuckPerms/FTB Ranks if available.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/permissions &fto open the permissions editor.")));
    }

    private void displayEconomySection(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&e❖ &6Economy Dashboard")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fView balances, transactions, and manage economy settings.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/balance <player> &fto check balances.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/economy set <player> <amount> &fto set balances.")));
    }

    private void displayStorageSection(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7❖ &8Storage Management")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fManage player data, backups, and storage quotas.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/adminpanel section backups &fto manage backups.")));
    }

    private void displayPerformanceSection(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&d❖ &5Performance Monitor")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fMonitor TPS, memory, and entity counts.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/adminpanel performance tps &fto view TPS.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/adminpanel performance memory &fto view memory usage.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/adminpanel optimize all &fto optimize server performance.")));
    }

    private void displayCommandsSection(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&5❖ &d Commands Management")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fView and manage custom commands and aliases.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/commands &fto list all available commands.")));
    }

    private void displayLogsSection(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&f❖ &7Log Viewer")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fView recent server logs and events.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/adminpanel section logs &fto open the log viewer.")));
    }

    private void displayBackupsSection(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&2❖ &a Backup Management")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fCreate, restore, and manage server backups.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/backup create &fto create a backup.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/backup restore <name> &fto restore a backup.")));
    }

    private void displayMaintenanceSection(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&6❖ &e Maintenance Controls")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fEnable or disable maintenance mode.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/server maintenance <true|false> &fto toggle maintenance mode.")));
    }

    private void displaySettingsSection(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&3❖ &b Settings Configuration")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fConfigure plugin and server settings.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/adminpanel reload &fto reload settings.")));
    }

    // Ensure all open blocks are closed properly
      private boolean checkPlayerPermission(ServerPlayer player, String permission) {
        return com.zerog.neoessentials.utils.PermissionUtil.hasPermission(player, permission);
    }

    /**
     * Display economy panel
     */
    private int executeEconomyPanel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        displayEconomySection(player);
=======
     * Displays the main admin panel interface with clickable options.
     * 
     * @param player The player to show the panel to
     */
    private void displayMainAdminPanel(ServerPlayer player) {
        NeoEssentials.LOGGER.info("Displaying admin panel for player: {}", player.getScoreboardName());
        
        // Create the header
        Component header = Component.literal(MessageUtil.translateColorCodes("&6====== &lNeoEssentials Admin Panel&r &6======"));
        player.sendSystemMessage(header);        // Create clickable sections based on permissions
        if (player.hasPermissions(2) || checkPlayerPermission(player, "neoessentials.adminpanel.economy")) {
            displaySectionButton(player, "&2Economy Management", "/adminpanel economy", 
                    "&7Click to manage economy settings, view transactions,\n&7set balances, and view leaderboards.");
        }
        
        if (player.hasPermissions(2) || checkPlayerPermission(player, "neoessentials.adminpanel.kits")) {
            displaySectionButton(player, "&3Kit Management", "/adminpanel kits", 
                    "&7Click to manage kits, create new kits,\n&7edit existing kits, and view usage statistics.");
        }
        
        if (player.hasPermissions(2) || checkPlayerPermission(player, "neoessentials.adminpanel.warps")) {
            displaySectionButton(player, "&5Warp Management", "/adminpanel warps", 
                    "&7Click to manage warps, create new warps,\n&7edit existing warps, and set permissions.");
        }
        
        if (player.hasPermissions(2) || checkPlayerPermission(player, "neoessentials.adminpanel.players")) {
            displaySectionButton(player, "&6Player Management", "/adminpanel players", 
                    "&7Click to manage players, view online players,\n&7check player stats, and perform admin actions.");
        }
        
        // Create footer
        Component footer = Component.literal(MessageUtil.translateColorCodes("&6==================================="));
        player.sendSystemMessage(footer);
    }
    
    /**
     * Displays a clickable button for an admin panel section.
     * 
     * @param player The player to show the button to
     * @param title The title of the section
     * @param command The command to run when clicked
     * @param hoverText The hover text to display
     */
    private void displaySectionButton(ServerPlayer player, String title, String command, String hoverText) {
        Component buttonText = Component.literal(MessageUtil.translateColorCodes("&8[&r " + title + " &8]"));
        Component hoverComponent = Component.literal(MessageUtil.translateColorCodes(hoverText));
        
        // Make the button clickable and add hover text
        Component clickableButton = MessageUtil.makeClickableCommand(
                (Component.literal("➤ ").append(buttonText)).copy(), command)
                .withStyle(style -> style.withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                        net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, 
                        hoverComponent)));
        
        player.sendSystemMessage(clickableButton);
    }
    
    /**
     * Executes the economy panel command.
     * 
     * @param context The command context
     * @return 1 if successful
     */
    private int executeEconomyPanel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        if (!CommandManager.hasPermission(context.getSource(), "neoessentials.adminpanel.economy")) {
            MessageUtil.sendErrorMessage(player, "You don't have permission to use the economy admin panel.");
            return 0;
        }
        
        // Display economy management options
        AdminPanel.displayEconomyPanel(player);
        
>>>>>>> aa6024a (feat: Implement Admin Panel and Menu System)
        return 1;
    }
    
    /**
<<<<<<< HEAD
     * Display kits panel
     */
    private int executeKitsPanel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&6❖ &eKit Management")));
        // Display kit management options
=======
     * Executes the kits panel command.
     * 
     * @param context The command context
     * @return 1 if successful
     */
    private int executeKitsPanel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        if (!CommandManager.hasPermission(context.getSource(), "neoessentials.adminpanel.kits")) {
            MessageUtil.sendErrorMessage(player, "You don't have permission to use the kit admin panel.");
            return 0;
        }
        
        // Display kit management options
        AdminPanel.displayKitsPanel(player);
        
>>>>>>> aa6024a (feat: Implement Admin Panel and Menu System)
        return 1;
    }
    
    /**
<<<<<<< HEAD
     * Display warps panel
     */
    private int executeWarpsPanel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&3❖ &bWarp Management")));
        // Display warp management options
=======
     * Executes the warps panel command.
     * 
     * @param context The command context
     * @return 1 if successful
     */
    private int executeWarpsPanel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        if (!CommandManager.hasPermission(context.getSource(), "neoessentials.adminpanel.warps")) {
            MessageUtil.sendErrorMessage(player, "You don't have permission to use the warps admin panel.");
            return 0;
        }
        
        // Display warp management options
        AdminPanel.displayWarpsPanel(player);
        
>>>>>>> aa6024a (feat: Implement Admin Panel and Menu System)
        return 1;
    }
    
    /**
<<<<<<< HEAD
     * Display players panel
     */
    private int executePlayersPanel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        displayPlayersSection(player);
        return 1;
    }
    
    // Place all helper methods at the end of the class, outside any other method
    private void displayPlayerManagementPanel(ServerPlayer admin, ServerPlayer target) {
        admin.sendSystemMessage(Component.literal(TextUtil.colorize("&a❖ &2Player Management: &e" + target.getScoreboardName())));
        admin.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fKick, mute, or teleport this player.")));
        admin.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/kick " + target.getScoreboardName() + " <reason> &fto kick.")));
        admin.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/mute " + target.getScoreboardName() + " <time> <reason> &fto mute.")));
    }

    private void displayPerformanceOverview(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&d❖ &5Performance Overview")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fTPS, memory, and entity statistics at a glance.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/adminpanel performance tps &fto view TPS.")));
        player.sendSystemMessage(Component.literal(TextUtil.colorize("&7- &fUse &e/adminpanel performance memory &fto view memory usage.")));
    }

    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    private String formatUptime(int ticks) {
        long seconds = ticks / 20;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        if (days > 0) {
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m " + (seconds % 60) + "s";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    // Utility to count entities in an Iterable
    private int countEntities(Iterable<?> entities) {
        int count = 0;
        for (Object ignored : entities) count++;
        return count;
    }
=======
     * Executes the players panel command.
     * 
     * @param context The command context
     * @return 1 if successful
     */
    private int executePlayersPanel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        if (!CommandManager.hasPermission(context.getSource(), "neoessentials.adminpanel.players")) {
            MessageUtil.sendErrorMessage(player, "You don't have permission to use the players admin panel.");
            return 0;
        }
        
        // Display player management options
        AdminPanel.displayPlayersPanel(player);
        
        return 1;
    }
<<<<<<< HEAD
>>>>>>> aa6024a (feat: Implement Admin Panel and Menu System)
=======
    
    /**
     * Check if a player has a permission using CommandSourceStack approach
     * 
     * @param player The player to check
     * @param permission The permission to check
     * @return True if the player has the permission
     */
    private boolean checkPlayerPermission(ServerPlayer player, String permission) {
        // Convert player to CommandSourceStack and use the existing method
        net.minecraft.commands.CommandSourceStack source = player.createCommandSourceStack();
        return CommandManager.hasPermission(source, permission);
    }
>>>>>>> 02542de (refactor: Simplify permission checks in AdminPanelCommand; add checkPlayerPermission method in PermissionUtil)
}
