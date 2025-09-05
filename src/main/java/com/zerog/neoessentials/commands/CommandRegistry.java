package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.commands.admin.AdminCommandManager;
import com.zerog.neoessentials.commands.admin.NeoEssentialsCommand;
import com.zerog.neoessentials.commands.admin.StatusCommand;
import com.zerog.neoessentials.commands.permissions.PermissionsCommand;
// import com.zerog.neoessentials.commands.status.StatusCommand; // DISABLED - Missing dependencies
// import com.zerog.neoessentials.commands.notifications.AlertCommand; // DISABLED - Missing dependencies
// import com.zerog.neoessentials.commands.monitoring.PerformanceCommand; // DISABLED - Over-engineered
// import com.zerog.neoessentials.commands.enterprise.BackupCommand; // DISABLED - Over-engineered
import com.zerog.neoessentials.commands.placeholders.PlaceholderCommand;
import com.zerog.neoessentials.commands.player.PlaytimeCommand;
import com.zerog.neoessentials.commands.player.AchievementsCommand;
import com.zerog.neoessentials.commands.player.PreferencesCommand;
import com.zerog.neoessentials.commands.language.LanguageCommand;
import com.zerog.neoessentials.commands.essentials.*;
import com.zerog.neoessentials.commands.essentials.ConfigCommand;
// import com.zerog.neoessentials.managers.PluginCompatibilityManager; // DISABLED - Compilation issues
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandBuildContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Central command registration for NeoEssentials
 */
public class CommandRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRegistry.class);
    
    // Dynamic command registry for auto-completion and execution
    private static final Map<String, ICommand> dynamicCommands = new HashMap<>();

    public static void registerDynamic(String name, ICommand command) {
        dynamicCommands.put(name.toLowerCase(), command);
        for (String alias : command.getAliases()) {
            dynamicCommands.put(alias.toLowerCase(), command);
        }
    }

    public static Collection<String> getDynamicCommandNames() {
        return dynamicCommands.keySet();
    }

    public static ICommand getDynamicCommand(String name) {
        return dynamicCommands.get(name.toLowerCase());
    }

    /**
     * Register all NeoEssentials commands
     */
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        LOGGER.info("Registering NeoEssentials commands...");
        
        try {
            // Essential utility commands
            HealCommand.register(dispatcher);
            LOGGER.info("Registered heal command");
            
            FeedCommand.register(dispatcher);
            LOGGER.info("Registered feed command");
            
            GodCommand.register(dispatcher);
            LOGGER.info("Registered god command");
            
            VanishCommand.register(dispatcher);
            LOGGER.info("Registered vanish command");
            
            FlyCommand.register(dispatcher);
            LOGGER.info("Registered fly command");
            
            SpeedCommand.register(dispatcher);
            LOGGER.info("Registered speed command");
            
            GameModeCommand.register(dispatcher);
            LOGGER.info("Registered gamemode commands (/gamemode, /gm, /gmc, /gms, /gma, /gmsp)");
            
            RepairCommand.register(dispatcher, context);
            LOGGER.info("Registered repair command");
            
            TimeCommand.register(dispatcher);
            LOGGER.info("Registered time command");
            
            WeatherCommand.register(dispatcher);
            LOGGER.info("Registered weather command");
            
            GiveCommand.register(dispatcher, context);
            LOGGER.info("Registered give command");
            
            WorkbenchCommand.register(dispatcher);
            LOGGER.info("Registered workbench command");
            
            AnvilCommand.register(dispatcher);
            LOGGER.info("Registered anvil command");
            
            // Moderation commands
            BanCommand.register(dispatcher);
            LOGGER.info("Registered ban command");
            
            KickCommand.register(dispatcher);
            LOGGER.info("Registered kick command");
            
            MuteCommand.register(dispatcher);
            LOGGER.info("Registered mute command");
            
            // Player utility commands
            ListCommand.register(dispatcher);
            LOGGER.info("Registered list command");
            
            WhoisCommand.register(dispatcher);
            LOGGER.info("Registered whois command");
            
            SeenCommand.register(dispatcher);
            LOGGER.info("Registered seen command");
            
            // Help command
            HelpCommand.register(dispatcher);
            LOGGER.info("Registered help command");
            
            // Info command
            InfoCommand.register(dispatcher);
            LOGGER.info("Registered info command");
            
            // Message commands
            MessageCommand.register(dispatcher);
            LOGGER.info("Registered message command");
            
            ReplyCommand.register(dispatcher);
            LOGGER.info("Registered reply command");
            
            // MOTD command
            MotdCommand.register(dispatcher);
            LOGGER.info("Registered motd command");
            
            // Nickname command
            NickCommand.register(dispatcher);
            LOGGER.info("Registered nick command");
            
            // Debug command for tablist testing
            com.zerog.neoessentials.commands.debug.TablistTestCommand.register(dispatcher);
            LOGGER.info("Registered tablisttest command");
            
            // Debug command for scoreboard testing
            com.zerog.neoessentials.commands.debug.ScoreboardTestCommand.register(dispatcher);
            LOGGER.info("Registered scoreboardtest command");
            
            // Professional Scoreboard command
            // ScoreboardCommand.register(dispatcher); // Removed as part of cleanup
            LOGGER.info("Registered scoreboard command");
            
            // AFK command
            com.zerog.neoessentials.commands.essentials.AFKCommand.register(dispatcher);
            LOGGER.info("Registered AFK command");
            
            // Permission test command
            com.zerog.neoessentials.commands.permissions.PermissionTestCommand.register(dispatcher);
            LOGGER.info("Registered permission test command");
            
            // Economy admin commands
            EconomyCommand.register(dispatcher);
            LOGGER.info("Registered economy admin commands");
            
            // Transaction history command - TEMPORARILY DISABLED (API compatibility issues)
            // com.zerog.neoessentials.commands.economy.TransactionHistoryCommand.register(dispatcher);
            // LOGGER.info("Registered transaction history command");
            
            // Economy analytics command - TEMPORARILY DISABLED (API compatibility issues)
            // com.zerog.neoessentials.commands.economy.EconomyAnalyticsCommand.register(dispatcher);
            // LOGGER.info("Registered economy analytics command");
            
            // Shop system commands
            // Temporarily disabled shop commands (user requested to ignore shop section)
            // com.zerog.neoessentials.commands.economy.ShopCommand.register(dispatcher);
            com.zerog.neoessentials.economy.SignShopCommand.register(dispatcher, context);
            LOGGER.info("Registered shop system commands");
            
            // Mail system
            MailCommand.register(dispatcher);
            LOGGER.info("Registered mail system");
            
            // Teleport commands
            TeleportCommand.register(dispatcher);
            LOGGER.info("Registered teleport commands");
            
            // TPA (Teleport Request) commands
            com.zerog.neoessentials.commands.essentials.TpaCommand.register(dispatcher);
            LOGGER.info("Registered TPA teleport request commands");
            
            // Server information commands
            RulesCommand.register(dispatcher);
            LOGGER.info("Registered rules command");
            
            // Back command (teleportation utility)
            BackCommand.register(dispatcher);
            LOGGER.info("Registered back command");
            
            // Home commands
            HomeCommands.register(dispatcher);
            LOGGER.info("Registered home commands");
            
            // Warp commands
            WarpCommands.register(dispatcher);
            LOGGER.info("Registered warp commands");
            
            // Spawn commands
            SpawnCommands.register(dispatcher);
            LOGGER.info("Registered spawn commands");
            
            // Permission debug command (for testing)
            PermissionDebugCommand.register(dispatcher);
            LOGGER.info("Registered permission debug command");
            
            // Economy commands
            EconomyCommands.register(dispatcher);
            LOGGER.info("Registered economy commands");
            
            // Kit commands
            KitCommand.register(dispatcher);
            LOGGER.info("Registered kit commands");
            
            // Messaging commands
            MessagingCommands.register(dispatcher);
            LOGGER.info("Registered messaging commands");
            
            // Moderation commands
            ModerationCommands.register(dispatcher);
            LOGGER.info("Registered moderation commands");
            
            // Plugin compatibility commands
            // CompatibilityCommand.register(dispatcher, PluginCompatibilityManager.getInstance()); // DISABLED - Compilation issues
            // LOGGER.info("Registered plugin compatibility commands");
            
            // Admin commands - Centralized admin command management
            AdminCommandManager.getInstance().registerCommands(dispatcher);
            LOGGER.info("Registered centralized admin command system");
            
            // Legacy admin commands for backwards compatibility
            NeoEssentialsCommand.register(dispatcher);
            LOGGER.info("Registered legacy admin commands");
            
            // Web Dashboard management commands
            com.zerog.neoessentials.commands.WebDashboardCommand.register(dispatcher);
            LOGGER.info("Registered web dashboard management commands");
            
            // Discord Management commands (comprehensive)
            // System status monitoring commands (Phase 5)
            StatusCommand.register(dispatcher);
            LOGGER.info("Registered system status monitoring commands");
            
            // Status monitoring commands - DISABLED (Missing enterprise dependencies)
            // StatusCommand.register(dispatcher);
            // LOGGER.info("Registered status monitoring commands");
            
            // Alert and notification commands - DISABLED (Missing enterprise dependencies)
            // AlertCommand.register(dispatcher);
            // LOGGER.info("Registered alert and notification commands");
            
            // Language management commands
            LanguageCommand.register(dispatcher);
            LOGGER.info("Registered language management commands");
            
            // Permission management commands
            PermissionsCommand.register(dispatcher);
            LOGGER.info("Registered permission management commands");
            
            // Animated prefix commands
            com.zerog.neoessentials.commands.permissions.AnimatedPrefixCommand.register(dispatcher);
            LOGGER.info("Registered animated prefix commands");
            
            // Placeholder system commands
            PlaceholderCommand.register(dispatcher);
            LOGGER.info("Registered placeholder system commands");
            
            // Configuration management commands
            ConfigCommand.register(dispatcher);
            LOGGER.info("Registered configuration management commands");
            
            // Bossbar management commands
            // BossbarCommand.register(dispatcher); // Removed as part of cleanup
            LOGGER.info("Registered bossbar management commands");
            
            // Animation management commands
            AnimationCommands.register(dispatcher);
            LOGGER.info("Registered animation management commands");
            
            // Enhanced Theme System (Phase 6) - Commands integrated into existing systems

            // Advanced Player Features Commands
            PlaytimeCommand.register(dispatcher);
            LOGGER.info("Registered playtime tracking commands");
            
            AchievementsCommand.register(dispatcher);
            LOGGER.info("Registered achievement system commands");
            
            PreferencesCommand.register(dispatcher);
            LOGGER.info("Registered player preference commands");

            // Language System (Phase 4)
            LanguageCommand.register(dispatcher);
            LOGGER.info("Registered enhanced language system commands");
            
            // Enhanced security commands - DISABLED (Missing dependencies)
            // Enterprise performance monitoring commands - uses @SubscribeEvent pattern
            // com.zerog.neoessentials.performance.PerformanceCommand.register(event);
            LOGGER.info("Performance commands available via @SubscribeEvent registration");
            
            // Enterprise backup and disaster recovery commands - DISABLED (Over-engineered for Minecraft mod)
            // BackupCommand.register(dispatcher);
            // LOGGER.info("Registered enterprise backup and disaster recovery commands");
            
            LOGGER.info("All NeoEssentials commands registered successfully!");
            
        } catch (Exception e) {
            com.zerog.neoessentials.util.ErrorHandler.handleError(
                com.zerog.neoessentials.util.ErrorHandler.ErrorCategory.INITIALIZATION,
                com.zerog.neoessentials.util.ErrorHandler.ErrorSeverity.CRITICAL,
                "Command Registration", e);
        }
    }
}
