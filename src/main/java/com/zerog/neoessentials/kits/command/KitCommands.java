package com.zerog.neoessentials.kits.command;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers all kit-related commands.
 */
public class KitCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(KitCommands.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ConfigManager config = ConfigManager.getInstance();
        
        if (!ConfigManager.isKitModuleEnabled()) {
            LOGGER.info("Kits module is disabled, skipping kit command registration");
            return;
        }
        
        // Initialize KitManager before registering commands
        try {
            com.zerog.neoessentials.kits.KitManager.getInstance().initialize();
            LOGGER.info("KitManager initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize KitManager: {}", e.getMessage(), e);
        }

        LOGGER.info("Registering kit commands...");
        
        // Register kit command
        if (config.isCommandEnabled("kit")) {
            try {
                KitCommand.register(dispatcher);
                LOGGER.info("Kit command registered");
            } catch (Exception e) {
                LOGGER.error("Failed to register kit command", e);
            }
        }
        
        // Register createkit command
        if (config.isCommandEnabled("createkit")) {
            try {
                CreateKitCommand.register(dispatcher);
                LOGGER.info("CreateKit command registered");
            } catch (Exception e) {
                LOGGER.error("Failed to register createkit command", e);
            }
        }
        
        // Register delkit command
        if (config.isCommandEnabled("delkit")) {
            try {
                DelKitCommand.register(dispatcher);
                LOGGER.info("DelKit command registered");
            } catch (Exception e) {
                LOGGER.error("Failed to register delkit command", e);
            }
        }
        
        // Register listkits command  
        if (config.isCommandEnabled("listkits")) {
            try {
                ListKitsCommand.register(dispatcher);
                LOGGER.info("ListKits command registered");
            } catch (Exception e) {
                LOGGER.error("Failed to register listkits command", e);
            }
        }

        // Register kitreset command (Essentials: /kitreset)
        if (config.isCommandEnabled("kitreset")) {
            try {
                KitResetCommand.register(dispatcher);
                LOGGER.info("KitReset command registered");
            } catch (Exception e) {
                LOGGER.error("Failed to register kitreset command", e);
            }
        }

        LOGGER.info("All kit commands registration completed");
    }
}