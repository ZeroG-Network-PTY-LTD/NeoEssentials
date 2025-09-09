package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.commands.admin.AdminCommandManager;
import com.zerog.neoessentials.commands.admin.NeoEssentialsCommand;
import com.zerog.neoessentials.commands.admin.StatusCommand;
import com.zerog.neoessentials.commands.permissions.PermissionsCommand;
import com.zerog.neoessentials.util.CommandConfigUtil;
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
            // Essential utility commands - check configuration before registering
            if (CommandConfigUtil.isCommandEnabled("heal")) {
                HealCommand.register(dispatcher);
                LOGGER.info("Registered heal command");
            } else {
                LOGGER.info("Heal command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("feed")) {
                FeedCommand.register(dispatcher);
                LOGGER.info("Registered feed command");
            } else {
                LOGGER.info("Feed command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("god")) {
                GodCommand.register(dispatcher);
                LOGGER.info("Registered god command");
            } else {
                LOGGER.info("God command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("vanish")) {
                VanishCommand.register(dispatcher);
                LOGGER.info("Registered vanish command");
            } else {
                LOGGER.info("Vanish command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("fly")) {
                FlyCommand.register(dispatcher);
                LOGGER.info("Registered fly command");
            } else {
                LOGGER.info("Fly command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("speed")) {
                SpeedCommand.register(dispatcher);
                LOGGER.info("Registered speed command");
            } else {
                LOGGER.info("Speed command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("gamemode")) {
                GameModeCommand.register(dispatcher);
                LOGGER.info("Registered gamemode commands (/gamemode, /gm, /gmc, /gms, /gma, /gmsp)");
            } else {
                LOGGER.info("Gamemode commands disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("repair")) {
                RepairCommand.register(dispatcher, context);
                LOGGER.info("Registered repair command");
            } else {
                LOGGER.info("Repair command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("time")) {
                TimeCommand.register(dispatcher);
                LOGGER.info("Registered time command");
            } else {
                LOGGER.info("Time command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("weather")) {
                WeatherCommand.register(dispatcher);
                LOGGER.info("Registered weather command");
            } else {
                LOGGER.info("Weather command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("give")) {
                GiveCommand.register(dispatcher, context);
                LOGGER.info("Registered give command");
            } else {
                LOGGER.info("Give command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("workbench")) {
                WorkbenchCommand.register(dispatcher);
                LOGGER.info("Registered workbench command");
            } else {
                LOGGER.info("Workbench command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("anvil")) {
                AnvilCommand.register(dispatcher);
                LOGGER.info("Registered anvil command");
            } else {
                LOGGER.info("Anvil command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("smithing")) {
                SmithingCommand.register(dispatcher);
                LOGGER.info("Registered smithing command");
            } else {
                LOGGER.info("Smithing command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("stonecutter")) {
                StonecutterCommand.register(dispatcher);
                LOGGER.info("Registered stonecutter command");
            } else {
                LOGGER.info("Stonecutter command disabled in configuration");
            }
            
            // Moderation commands - check both command and module status
            if (CommandConfigUtil.isFeatureEnabled("ban", "moderation")) {
                BanCommand.register(dispatcher);
                LOGGER.info("Registered ban command");
            } else {
                LOGGER.info("Ban command disabled in configuration");
            }
            
            if (CommandConfigUtil.isFeatureEnabled("kick", "moderation")) {
                KickCommand.register(dispatcher);
                LOGGER.info("Registered kick command");
            } else {
                LOGGER.info("Kick command disabled in configuration");
            }
            
            if (CommandConfigUtil.isFeatureEnabled("mute", "moderation")) {
                MuteCommand.register(dispatcher);
                LOGGER.info("Registered mute command");
            } else {
                LOGGER.info("Mute command disabled in configuration");
            }
            
            // Player utility commands
            if (CommandConfigUtil.isCommandEnabled("list")) {
                ListCommand.register(dispatcher);
                LOGGER.info("Registered list command");
            } else {
                LOGGER.info("List command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("whois")) {
                WhoisCommand.register(dispatcher);
                LOGGER.info("Registered whois command");
            } else {
                LOGGER.info("Whois command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("seen")) {
                SeenCommand.register(dispatcher);
                LOGGER.info("Registered seen command");
            } else {
                LOGGER.info("Seen command disabled in configuration");
            }
            
            // Help command
            if (CommandConfigUtil.isCommandEnabled("help")) {
                HelpCommand.register(dispatcher);
                LOGGER.info("Registered help command");
            } else {
                LOGGER.info("Help command disabled in configuration");
            }
            
            // Info command
            if (CommandConfigUtil.isCommandEnabled("info")) {
                InfoCommand.register(dispatcher);
                LOGGER.info("Registered info command");
            } else {
                LOGGER.info("Info command disabled in configuration");
            }
            
            // Message commands - check both command and chat module
            if (CommandConfigUtil.isFeatureEnabled("message", "chat")) {
                MessageCommand.register(dispatcher);
                LOGGER.info("Registered message command");
            } else {
                LOGGER.info("Message command disabled in configuration");
            }
            
            if (CommandConfigUtil.isFeatureEnabled("reply", "chat")) {
                ReplyCommand.register(dispatcher);
                LOGGER.info("Registered reply command");
            } else {
                LOGGER.info("Reply command disabled in configuration");
            }
            
            // MOTD command
            if (CommandConfigUtil.isCommandEnabled("motd")) {
                MotdCommand.register(dispatcher);
                LOGGER.info("Registered motd command");
            } else {
                LOGGER.info("MOTD command disabled in configuration");
            }
            
            // Nickname command - check chat module
            if (CommandConfigUtil.isFeatureEnabled("nick", "chat")) {
                NickCommand.register(dispatcher);
                LOGGER.info("Registered nick command");
            } else {
                LOGGER.info("Nick command disabled in configuration");
            }
            
            // Debug command for tablist testing
            com.zerog.neoessentials.commands.debug.TablistTestCommand.register(dispatcher);
            LOGGER.info("Registered tablisttest command");
            
            // Scoreboard test command removed - scoreboard system no longer used
            
            // Professional Scoreboard command - removed as part of cleanup
            LOGGER.info("Scoreboard-related commands removed");
            
            // AFK command
            if (CommandConfigUtil.isCommandEnabled("afk")) {
                com.zerog.neoessentials.commands.essentials.AFKCommand.register(dispatcher);
                LOGGER.info("Registered AFK command");
            } else {
                LOGGER.info("AFK command disabled in configuration");
            }
            
            // Permission test command (always enabled for debugging)
            com.zerog.neoessentials.commands.permissions.PermissionTestCommand.register(dispatcher);
            LOGGER.info("Registered permission test command");
            
            // Economy admin commands - check economy module
            if (CommandConfigUtil.isFeatureEnabled("economy", "economy")) {
                EconomyCommand.register(dispatcher);
                LOGGER.info("Registered economy admin commands");
            } else {
                LOGGER.info("Economy admin commands disabled in configuration");
            }
            
            // Shop system commands - check economy module
            if (CommandConfigUtil.isFeatureEnabled("shop", "economy")) {
                com.zerog.neoessentials.economy.SignShopCommand.register(dispatcher, context);
                com.zerog.neoessentials.commands.economy.SaveShopsCommand.register(dispatcher);
                com.zerog.neoessentials.commands.economy.CheckShopsCommand.register(dispatcher);
                LOGGER.info("Registered shop system commands");
            } else {
                LOGGER.info("Shop system commands disabled in configuration");
            }
            
            // Mail system
            if (CommandConfigUtil.isCommandEnabled("mail")) {
                MailCommand.register(dispatcher);
                LOGGER.info("Registered mail system");
            } else {
                LOGGER.info("Mail system disabled in configuration");
            }
            
            // Teleport commands
            if (CommandConfigUtil.isCommandEnabled("teleport")) {
                TeleportCommand.register(dispatcher);
                LOGGER.info("Registered teleport commands");
            } else {
                LOGGER.info("Teleport commands disabled in configuration");
            }
            
            // TPA (Teleport Request) commands
            if (CommandConfigUtil.isCommandEnabled("tpa")) {
                com.zerog.neoessentials.commands.essentials.TpaCommand.register(dispatcher);
                LOGGER.info("Registered TPA teleport request commands");
            } else {
                LOGGER.info("TPA commands disabled in configuration");
            }
            
            // Server information commands
            if (CommandConfigUtil.isCommandEnabled("rules")) {
                RulesCommand.register(dispatcher);
                LOGGER.info("Registered rules command");
            } else {
                LOGGER.info("Rules command disabled in configuration");
            }
            
            // Back command (teleportation utility)
            if (CommandConfigUtil.isCommandEnabled("back")) {
                BackCommand.register(dispatcher);
                LOGGER.info("Registered back command");
            } else {
                LOGGER.info("Back command disabled in configuration");
            }
            
            // Home commands - check homes module
            if (CommandConfigUtil.isFeatureEnabled("home", "homes")) {
                HomeCommands.register(dispatcher);
                LOGGER.info("Registered home commands");
            } else {
                LOGGER.info("Home commands disabled in configuration");
            }
            
            // Warp commands - check warps module
            if (CommandConfigUtil.isFeatureEnabled("warp", "warps")) {
                WarpCommands.register(dispatcher);
                LOGGER.info("Registered warp commands");
            } else {
                LOGGER.info("Warp commands disabled in configuration");
            }
            
            // Spawn commands - check spawn module
            if (CommandConfigUtil.isFeatureEnabled("spawn", "spawn")) {
                SpawnCommands.register(dispatcher);
                LOGGER.info("Registered spawn commands");
            } else {
                LOGGER.info("Spawn commands disabled in configuration");
            }
            
            // Permission debug command (always enabled for debugging)
            PermissionDebugCommand.register(dispatcher);
            LOGGER.info("Registered permission debug command");
            
            // Economy commands - check economy module
            if (CommandConfigUtil.isFeatureEnabled("balance", "economy")) {
                EconomyCommands.register(dispatcher);
                LOGGER.info("Registered economy commands");
            } else {
                LOGGER.info("Economy commands disabled in configuration");
            }
            
            // Kit commands - check kits module
            if (CommandConfigUtil.isFeatureEnabled("kit", "kits")) {
                KitCommand.register(dispatcher);
                LOGGER.info("Registered kit commands");
            } else {
                LOGGER.info("Kit commands disabled in configuration");
            }
            
            // Messaging commands - check chat module
            if (CommandConfigUtil.isFeatureEnabled("messaging", "chat")) {
                MessagingCommands.register(dispatcher);
                LOGGER.info("Registered messaging commands");
            } else {
                LOGGER.info("Messaging commands disabled in configuration");
            }
            
            // Moderation commands - check moderation module
            if (CommandConfigUtil.isFeatureEnabled("moderation", "moderation")) {
                ModerationCommands.register(dispatcher);
                LOGGER.info("Registered moderation commands");
            } else {
                LOGGER.info("Moderation commands disabled in configuration");
            }
            
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
