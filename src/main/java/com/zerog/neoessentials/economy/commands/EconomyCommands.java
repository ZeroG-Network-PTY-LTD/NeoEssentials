
package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

// NOTE: Use LangUtil.translate for all user-facing messages to ensure proper localization.

public class EconomyCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(EconomyCommands.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ConfigManager config = ConfigManager.getInstance();
        
        if (!ConfigManager.isEconomyEnabled()) {
            NeoLog.info(LOGGER, LogCategory.ECONOMY, "Economy module is disabled, skipping economy command registration");
            return;
        }
        
        NeoLog.info(LOGGER, LogCategory.ECONOMY, "Registering economy commands...");
        
        // Register balance command
        if (config.isCommandEnabled("balance")) {
            try {
                BalanceCommand.register(dispatcher);
                NeoLog.info(LOGGER, LogCategory.ECONOMY, "Balance command registered");
            } catch (Exception e) {
                LOGGER.error("Failed to register balance command", e);
            }
        }
        
        // Register pay command
        if (config.isCommandEnabled("pay")) {
            try {
                PayCommand.register(dispatcher);
                NeoLog.info(LOGGER, LogCategory.ECONOMY, "Pay command registered");
            } catch (Exception e) {
                LOGGER.error("Failed to register pay command", e);
            }
        }
        
        // Register paytoggle command
        if (config.isCommandEnabled("paytoggle")) {
            try {
                PayToggleCommand.register(dispatcher);
                NeoLog.info(LOGGER, LogCategory.ECONOMY, "PayToggle command registered");
            } catch (Exception e) {
                LOGGER.error("Failed to register paytoggle command", e);
            }
        }
        
        // Register eco command
        if (config.isCommandEnabled("eco")) {
            try {
                EcoCommand.register(dispatcher);
                NeoLog.info(LOGGER, LogCategory.ECONOMY, "Eco command registered");
            } catch (Exception e) {
                LOGGER.error("Failed to register eco command", e);
            }
        }
        
        // Register baltop command
        if (config.isCommandEnabled("baltop")) {
            try {
                BaltopCommand.register(dispatcher);
                NeoLog.info(LOGGER, LogCategory.ECONOMY, "Baltop command registered");
            } catch (Exception e) {
                LOGGER.error("Failed to register baltop command", e);
            }
        }
        
        NeoLog.info(LOGGER, LogCategory.ECONOMY, "All economy commands registration completed");
    }
}

//
