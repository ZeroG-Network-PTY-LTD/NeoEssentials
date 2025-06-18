package com.zerog.neoessentials.adapter;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.common.adapter.ICommandAdapter;
import com.zerog.neoessentials.commands.CommandManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge 1.20.1 implementation of the command adapter
 */
public class CommandAdapter implements ICommandAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandAdapter.class);
    private CommandManager commandManager;
    
    @Override
    public void registerCommand(String commandName, Object handlerClass, String permissionNode) {
        if (commandManager == null) {
            LOGGER.error("Cannot register command {}: CommandManager not initialized", commandName);
            return;
        }
        
        try {
            commandManager.registerCommand(commandName, handlerClass, permissionNode);
        } catch (Exception e) {
            LOGGER.error("Failed to register command: {}", commandName, e);
        }
    }
    
    @Override
    public void registerAllCommands() {
        if (commandManager == null) {
            LOGGER.error("Cannot register commands: CommandManager not initialized");
            return;
        }
        
        commandManager.registerAllCommands();
    }
    
    @Override
    public void initialize() {
        // Create the command manager for this version
        commandManager = new CommandManager();
        
        // Register command system with event handlers
        NeoEssentials.LOGGER.info("Initializing CommandAdapter for Forge 1.20.1");
    }
    
    /**
     * Get the command manager instance
     * @return The command manager
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }
}
