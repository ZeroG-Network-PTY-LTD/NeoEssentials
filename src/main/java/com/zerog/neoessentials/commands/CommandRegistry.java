package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.commands.admin.AdminCommandManager;
import com.zerog.neoessentials.commands.admin.NeoEssentialsCommand;
import com.zerog.neoessentials.commands.permissions.PermissionsCommand;
import com.zerog.neoessentials.util.CommandConfigUtil;
// Backup, Status, Performance commands removed
// import com.zerog.neoessentials.commands.notifications.AlertCommand; // DISABLED - Missing dependencies
import com.zerog.neoessentials.commands.placeholders.PlaceholderCommand;
import com.zerog.neoessentials.commands.player.PlaytimeCommand;
import com.zerog.neoessentials.commands.player.PreferencesCommand;
import com.zerog.neoessentials.commands.language.LanguageCommand;
import com.zerog.neoessentials.commands.WebDashboardCommand;
import com.zerog.neoessentials.commands.essentials.*;
import com.zerog.neoessentials.commands.essentials.ConfigCommand;
// Additional essential command imports for missing commands
import com.zerog.neoessentials.commands.essentials.AnvilCommand;
import com.zerog.neoessentials.commands.essentials.BanCommand;
import com.zerog.neoessentials.commands.essentials.CreateShopCommand;
import com.zerog.neoessentials.commands.essentials.KickCommand;
import com.zerog.neoessentials.commands.essentials.MuteCommand;
import com.zerog.neoessentials.commands.essentials.PayCommand;
import com.zerog.neoessentials.commands.essentials.SmithingCommand;
import com.zerog.neoessentials.commands.essentials.StonecutterCommand;
import com.zerog.neoessentials.commands.essentials.WorkbenchCommand;
// Performance and backup command imports removed
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
        LOGGER.info("Starting NeoEssentials command registration...");
        
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
            
            // Additional essential commands that were missing
            if (CommandConfigUtil.isCommandEnabled("invsee")) {
                com.zerog.neoessentials.commands.essentials.InvSeeCommand.register(dispatcher);
                LOGGER.info("Registered inventory inspection command");
            } else {
                LOGGER.info("Inventory inspection command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("enderchest")) {
                com.zerog.neoessentials.commands.essentials.EnderChestCommand.register(dispatcher);
                LOGGER.info("Registered ender chest command");
            } else {
                LOGGER.info("Ender chest command disabled in configuration");
            }
            
            if (CommandConfigUtil.isFeatureEnabled("tempban", "moderation")) {
                com.zerog.neoessentials.commands.essentials.TempBanCommand.register(dispatcher);
                LOGGER.info("Registered temporary ban command");
            } else {
                LOGGER.info("Temporary ban command disabled in configuration");
            }
            
            if (CommandConfigUtil.isFeatureEnabled("socialspy", "chat")) {
                com.zerog.neoessentials.commands.essentials.SocialSpyCommand.register(dispatcher);
                LOGGER.info("Registered social spy command");
            } else {
                LOGGER.info("Social spy command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("spawner")) {
                com.zerog.neoessentials.commands.essentials.SpawnerCommand.register(dispatcher);
                LOGGER.info("Registered spawner management command");
            } else {
                LOGGER.info("Spawner management command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("item")) {
                com.zerog.neoessentials.commands.essentials.ItemCommand.register(dispatcher, context);
                LOGGER.info("Registered item management command");
            } else {
                LOGGER.info("Item management command disabled in configuration");
            }

            // MISSING ESSENTIAL COMMANDS - Adding all commands that exist but weren't registered
            if (CommandConfigUtil.isCommandEnabled("anvil")) {
                com.zerog.neoessentials.commands.essentials.AnvilCommand.register(dispatcher);
                LOGGER.info("Registered anvil command");
            } else {
                LOGGER.info("Anvil command disabled in configuration");
            }
            
            if (CommandConfigUtil.isFeatureEnabled("ban", "moderation")) {
                com.zerog.neoessentials.commands.essentials.BanCommand.register(dispatcher);
                LOGGER.info("Registered ban command");
            } else {
                LOGGER.info("Ban command disabled in configuration");
            }
            
            if (CommandConfigUtil.isFeatureEnabled("createshop", "economy")) {
                com.zerog.neoessentials.commands.essentials.CreateShopCommand.register(dispatcher);
                LOGGER.info("Registered create shop command");
            } else {
                LOGGER.info("Create shop command disabled in configuration");
            }
            
            if (CommandConfigUtil.isFeatureEnabled("kick", "moderation")) {
                com.zerog.neoessentials.commands.essentials.KickCommand.register(dispatcher);
                LOGGER.info("Registered kick command");
            } else {
                LOGGER.info("Kick command disabled in configuration");
            }
            
            if (CommandConfigUtil.isFeatureEnabled("mute", "moderation")) {
                com.zerog.neoessentials.commands.essentials.MuteCommand.register(dispatcher);
                LOGGER.info("Registered mute command");
            } else {
                LOGGER.info("Mute command disabled in configuration");
            }
            
            if (CommandConfigUtil.isFeatureEnabled("pay", "economy")) {
                com.zerog.neoessentials.commands.essentials.PayCommand.register(dispatcher);
                LOGGER.info("Registered pay command");
            } else {
                LOGGER.info("Pay command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("smithing")) {
                com.zerog.neoessentials.commands.essentials.SmithingCommand.register(dispatcher);
                LOGGER.info("Registered smithing table command");
            } else {
                LOGGER.info("Smithing table command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("stonecutter")) {
                com.zerog.neoessentials.commands.essentials.StonecutterCommand.register(dispatcher);
                LOGGER.info("Registered stonecutter command");
            } else {
                LOGGER.info("Stonecutter command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("workbench")) {
                com.zerog.neoessentials.commands.essentials.WorkbenchCommand.register(dispatcher);
                LOGGER.info("Registered workbench command");
            } else {
                LOGGER.info("Workbench command disabled in configuration");
            }
            
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
            
            // Admin commands - Centralized admin command management
            AdminCommandManager.getInstance().registerCommands(dispatcher);
            LOGGER.info("Registered centralized admin command system");
            
            // Additional admin commands
            if (CommandConfigUtil.isCommandEnabled("kitadmin")) {
                com.zerog.neoessentials.commands.admin.KitAdminCommand.register(dispatcher);
                LOGGER.info("Registered kit administration commands");
            } else {
                LOGGER.info("Kit administration commands disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("tablistdebug")) {
                com.zerog.neoessentials.commands.admin.TabListDebugCommand.register(dispatcher);
                LOGGER.info("Registered tablist debug commands");
            } else {
                LOGGER.info("Tablist debug commands disabled in configuration");
            }

            // MISSING ADMIN COMMANDS - Adding all admin commands that exist but weren't registered            
            if (CommandConfigUtil.isCommandEnabled("error")) {
                com.zerog.neoessentials.commands.admin.ErrorCommand.register(dispatcher);
                LOGGER.info("Registered error command");
            } else {
                LOGGER.info("Error command disabled in configuration");
            }
            
            // Performance monitoring command removed
            
            // Utility commands
            if (CommandConfigUtil.isCommandEnabled("cleanupteams")) {
                CleanupTeamsCommand.register(dispatcher);
                LOGGER.info("Registered team cleanup command");
            } else {
                LOGGER.info("Team cleanup command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("cleartags")) {
                ClearAllTagsCommand.register(dispatcher);
                LOGGER.info("Registered clear tags command");
            } else {
                LOGGER.info("Clear tags command disabled in configuration");
            }
            
            if (CommandConfigUtil.isCommandEnabled("role")) {
                // RoleCommand uses ICommand interface, register it dynamically
                registerDynamic("role", new RoleCommand());
                LOGGER.info("Registered role management command (dynamic)");
            } else {
                LOGGER.info("Role management command disabled in configuration");
            }
            
            // FTB Integration commands
            if (CommandConfigUtil.isCommandEnabled("ftb")) {
                FTBIntegrationCommands.register(dispatcher);
                LOGGER.info("Registered FTB integration commands");
            } else {
                LOGGER.info("FTB integration commands disabled in configuration");
            }
            
            // Legacy admin commands for backwards compatibility
            NeoEssentialsCommand.register(dispatcher);
            LOGGER.info("Registered legacy admin commands");
            
            // Web Dashboard management commands
            WebDashboardCommand.register(dispatcher);
            LOGGER.info("Registered web dashboard management commands");
            
            // Discord Management commands (comprehensive)
            // Status command removed
            
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
