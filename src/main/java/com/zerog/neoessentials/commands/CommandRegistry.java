package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.commands.admin.NeoEssentialsCommand;
import com.zerog.neoessentials.commands.admin.EnhancedAdminCommand;
import com.zerog.neoessentials.commands.permissions.PermissionsCommand;
import com.zerog.neoessentials.commands.status.StatusCommand;
import com.zerog.neoessentials.commands.notifications.AlertCommand;
// import com.zerog.neoessentials.commands.monitoring.PerformanceCommand; // DISABLED - Over-engineered
// import com.zerog.neoessentials.commands.enterprise.BackupCommand; // DISABLED - Over-engineered
import com.zerog.neoessentials.commands.placeholders.PlaceholderCommand;
import com.zerog.neoessentials.commands.BossbarCommand;
import com.zerog.neoessentials.commands.essentials.*;
// import com.zerog.neoessentials.managers.PluginCompatibilityManager; // DISABLED - Compilation issues
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandBuildContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central command registration for NeoEssentials
 */
public class CommandRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRegistry.class);
    
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
            
            // Admin teleportation commands
            TeleportCommand.register(dispatcher);
            LOGGER.info("Registered teleport commands");
            
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
            
            // Economy commands
            EconomyCommands.register(dispatcher);
            LOGGER.info("Registered economy commands");
            
            // Analytics commands
            AnalyticsCommands.register(dispatcher);
            LOGGER.info("Registered analytics commands");
            
            // Kit commands
            KitCommand.register(dispatcher);
            LOGGER.info("Registered kit commands");
            
            // Spawn commands
            SpawnCommand.register(dispatcher);
            LOGGER.info("Registered spawn commands");
            
            // Messaging commands
            MessagingCommands.register(dispatcher);
            LOGGER.info("Registered messaging commands");
            
            // Moderation commands
            ModerationCommands.register(dispatcher);
            LOGGER.info("Registered moderation commands");
            
            // Plugin compatibility commands
            // CompatibilityCommand.register(dispatcher, PluginCompatibilityManager.getInstance()); // DISABLED - Compilation issues
            // LOGGER.info("Registered plugin compatibility commands");
            
            // Admin commands
            NeoEssentialsCommand.register(dispatcher);
            LOGGER.info("Registered admin commands");
            
            // Enhanced admin commands
            EnhancedAdminCommand.register(dispatcher);
            LOGGER.info("Registered enhanced admin commands");
            
            // Status monitoring commands
            StatusCommand.register(dispatcher);
            LOGGER.info("Registered status monitoring commands");
            
            // Alert and notification commands
            AlertCommand.register(dispatcher);
            LOGGER.info("Registered alert and notification commands");
            
            // Language management commands
            LanguageCommand.register(dispatcher);
            LOGGER.info("Registered language management commands");
            
            // Permission management commands
            PermissionsCommand.register(dispatcher);
            LOGGER.info("Registered permission management commands");
            
            // Placeholder system commands
            PlaceholderCommand.register(dispatcher);
            LOGGER.info("Registered placeholder system commands");
            
            // GUI system commands
            GuiCommand.register(dispatcher);
            LOGGER.info("Registered GUI system commands");
            
            // Bossbar management commands
            BossbarCommand.register(dispatcher);
            LOGGER.info("Registered bossbar management commands");
            
            // Enhanced security commands - DISABLED (Missing dependencies)
            // Enterprise performance monitoring commands - DISABLED (Over-engineered for Minecraft mod)
            // PerformanceCommand.register(dispatcher);
            // LOGGER.info("Registered enterprise performance monitoring commands");
            
            // Enterprise backup and disaster recovery commands - DISABLED (Over-engineered for Minecraft mod)
            // BackupCommand.register(dispatcher);
            // LOGGER.info("Registered enterprise backup and disaster recovery commands");
            
            LOGGER.info("All NeoEssentials commands registered successfully!");
            
        } catch (Exception e) {
            LOGGER.error("Failed to register commands", e);
        }
    }
}
