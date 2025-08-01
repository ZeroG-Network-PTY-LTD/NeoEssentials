package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.commands.admin.NeoEssentialsCommand;
import com.zerog.neoessentials.commands.admin.EnhancedAdminCommand;
import com.zerog.neoessentials.commands.status.StatusCommand;
import com.zerog.neoessentials.commands.notifications.AlertCommand;
import com.zerog.neoessentials.commands.security.SecurityCommand;
import com.zerog.neoessentials.commands.monitoring.PerformanceCommand;
import com.zerog.neoessentials.commands.enterprise.BackupCommand;
import net.minecraft.commands.CommandSourceStack;
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
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        LOGGER.info("Registering NeoEssentials commands...");
        
        try {
            // Home commands
            HomeCommands.register(dispatcher);
            LOGGER.info("Registered home commands");
            
            // Warp commands
            WarpCommands.register(dispatcher);
            LOGGER.info("Registered warp commands");
            
            // Economy commands
            EconomyCommands.register(dispatcher);
            LOGGER.info("Registered economy commands");
            
            // Kit commands
            KitCommands.register(dispatcher);
            LOGGER.info("Registered kit commands");
            
            // Spawn commands
            SpawnCommands.register(dispatcher);
            LOGGER.info("Registered spawn commands");
            
            // Messaging commands
            MessagingCommands.register(dispatcher);
            LOGGER.info("Registered messaging commands");
            
            // Moderation commands
            ModerationCommands.register(dispatcher);
            LOGGER.info("Registered moderation commands");
            
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
            
            // Enterprise security commands
            SecurityCommand.register();
            LOGGER.info("Registered enterprise security commands");
            
            // Enterprise performance monitoring commands
            PerformanceCommand.register(dispatcher);
            LOGGER.info("Registered enterprise performance monitoring commands");
            
            // Enterprise backup and disaster recovery commands
            BackupCommand.register(dispatcher);
            LOGGER.info("Registered enterprise backup and disaster recovery commands");
            
            LOGGER.info("All NeoEssentials commands registered successfully!");
            
        } catch (Exception e) {
            LOGGER.error("Failed to register commands", e);
        }
    }
}
