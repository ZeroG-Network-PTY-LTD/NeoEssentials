
package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// NOTE: Use LangUtil.translate for all user-facing messages to ensure proper localization.

public class EconomyCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(EconomyCommands.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LOGGER.info("Registering ALL economy commands directly (bypassing config for debugging)...");
        
        try {
            BalanceCommand.register(dispatcher);
            LOGGER.info("Balance command registered");
        } catch (Exception e) {
            LOGGER.error("Failed to register balance command", e);
        }
        
        // Register other commands directly too for now
        try {
            PayCommand.register(dispatcher);
            LOGGER.info("Pay command registered");
        } catch (Exception e) {
            LOGGER.error("Failed to register pay command", e);
        }
        
        try {
            PayToggleCommand.register(dispatcher);
            LOGGER.info("PayToggle command registered");
        } catch (Exception e) {
            LOGGER.error("Failed to register paytoggle command", e);
        }
        
        try {
            EcoCommand.register(dispatcher);
            LOGGER.info("Eco command registered");
        } catch (Exception e) {
            LOGGER.error("Failed to register eco command", e);
        }
        
        try {
            BaltopCommand.register(dispatcher);
            LOGGER.info("Baltop command registered");
        } catch (Exception e) {
            LOGGER.error("Failed to register baltop command", e);
        }
        
        LOGGER.info("All economy commands registration completed");
    }
}

//
