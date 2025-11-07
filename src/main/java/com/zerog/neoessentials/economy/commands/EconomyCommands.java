
package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// NOTE: Use LangUtil.translate for all user-facing messages to ensure proper localization.

public class EconomyCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(EconomyCommands.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ConfigManager config = ConfigManager.getInstance();
        
        if (!ConfigManager.isEconomyEnabled()) {
            LOGGER.info("Economy module is disabled, skipping economy command registration");
            return;
        }
        
        LOGGER.info("Registering economy commands...");
        
        // Register balance command
        if (config.isCommandEnabled("balance")) {
            try {
                BalanceCommand.register(dispatcher);
                LOGGER.info("Balance command registered");
            } catch (Exception e) {
                LOGGER.error("Failed to register balance command", e);
            }
        }
        
        // Register pay command
        if (config.isCommandEnabled("pay")) {
            try {
                PayCommand.register(dispatcher);
                LOGGER.info("Pay command registered");
            } catch (Exception e) {
                LOGGER.error("Failed to register pay command", e);
            }
        }
        
        // Register paytoggle command
        if (config.isCommandEnabled("paytoggle")) {
            try {
                PayToggleCommand.register(dispatcher);
                LOGGER.info("PayToggle command registered");
            } catch (Exception e) {
                LOGGER.error("Failed to register paytoggle command", e);
            }
        }
        
        // Register eco command
        if (config.isCommandEnabled("eco")) {
            try {
                EcoCommand.register(dispatcher);
                LOGGER.info("Eco command registered");
            } catch (Exception e) {
                LOGGER.error("Failed to register eco command", e);
            }
        }
        
        // Register baltop command
        if (config.isCommandEnabled("baltop")) {
            try {
                BaltopCommand.register(dispatcher);
                LOGGER.info("Baltop command registered");
            } catch (Exception e) {
                LOGGER.error("Failed to register baltop command", e);
            }
        }
        
        LOGGER.info("All economy commands registration completed");
    }
}

//
