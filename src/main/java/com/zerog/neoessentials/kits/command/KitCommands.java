package com.zerog.neoessentials.kits.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers all kit-related commands.
 */
public class KitCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(KitCommands.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LOGGER.info("Registering kit commands...");
        
        try {
            KitCommand.register(dispatcher);
            LOGGER.info("Kit command registered");
        } catch (Exception e) {
            LOGGER.error("Failed to register kit command", e);
        }
        
        try {
            CreateKitCommand.register(dispatcher);
            LOGGER.info("CreateKit command registered");
        } catch (Exception e) {
            LOGGER.error("Failed to register createkit command", e);
        }
        
        try {
            DelKitCommand.register(dispatcher);
            LOGGER.info("DelKit command registered");
        } catch (Exception e) {
            LOGGER.error("Failed to register delkit command", e);
        }
        
        try {
            ListKitsCommand.register(dispatcher);
            LOGGER.info("ListKits command registered");
        } catch (Exception e) {
            LOGGER.error("Failed to register listkits command", e);
        }
        
        LOGGER.info("All kit commands registration completed");
    }
}