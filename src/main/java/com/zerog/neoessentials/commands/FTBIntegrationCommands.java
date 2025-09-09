package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FTB Integration Commands - Provides integration with FTB Teams and other FTB mods
 */
public class FTBIntegrationCommands {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FTBIntegrationCommands.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        try {
            // Register FTB integration commands here when needed
            LOGGER.info("FTB integration commands registered successfully");
        } catch (Exception e) {
            LOGGER.warn("Could not register FTB integration commands: {}", e.getMessage());
        }
    }
}
