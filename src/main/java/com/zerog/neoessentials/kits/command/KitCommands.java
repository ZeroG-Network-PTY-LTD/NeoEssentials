package com.zerog.neoessentials.kits.command;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

/**
 * Registers all kit-related commands.
 */
public class KitCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(KitCommands.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ConfigManager config = ConfigManager.getInstance();
        
        if (!ConfigManager.isKitModuleEnabled()) {
            NeoLog.info(LOGGER, LogCategory.KITS, "Kits module is disabled, skipping kit command registration");
            return;
        }
        
        // Initialize KitManager before registering commands
        try {
            com.zerog.neoessentials.kits.KitManager.getInstance().initialize();
            NeoLog.info(LOGGER, LogCategory.KITS, "KitManager initialized successfully");
        } catch (Throwable e) {
            LOGGER.error("Failed to initialize KitManager: {}", e.getMessage(), e);
        }

        NeoLog.info(LOGGER, LogCategory.KITS, "Registering kit commands...");
        
        // Register kit command
        if (config.isCommandEnabled("kit")) {
            try {
                KitCommand.register(dispatcher);
                NeoLog.info(LOGGER, LogCategory.KITS, "Kit command registered");
            } catch (Exception e) {
                LOGGER.error("Failed to register kit command", e);
            }
        }
        
        // Register createkit command
        if (config.isCommandEnabled("createkit")) {
            try {
                CreateKitCommand.register(dispatcher);
                NeoLog.info(LOGGER, LogCategory.KITS, "CreateKit command registered");
            } catch (Exception e) {
                LOGGER.error("Failed to register createkit command", e);
            }
        }
        
        // Register delkit command
        if (config.isCommandEnabled("delkit")) {
            try {
                DelKitCommand.register(dispatcher);
                NeoLog.info(LOGGER, LogCategory.KITS, "DelKit command registered");
            } catch (Exception e) {
                LOGGER.error("Failed to register delkit command", e);
            }
        }
        
        // Register listkits command  
        if (config.isCommandEnabled("listkits")) {
            try {
                ListKitsCommand.register(dispatcher);
                NeoLog.info(LOGGER, LogCategory.KITS, "ListKits command registered");
            } catch (Exception e) {
                LOGGER.error("Failed to register listkits command", e);
            }
        }

        // Register kitreset command (Essentials: /kitreset)
        if (config.isCommandEnabled("kitreset")) {
            try {
                KitResetCommand.register(dispatcher);
                NeoLog.info(LOGGER, LogCategory.KITS, "KitReset command registered");
            } catch (Exception e) {
                LOGGER.error("Failed to register kitreset command", e);
            }
        }

        NeoLog.info(LOGGER, LogCategory.KITS, "All kit commands registration completed");
    }
}