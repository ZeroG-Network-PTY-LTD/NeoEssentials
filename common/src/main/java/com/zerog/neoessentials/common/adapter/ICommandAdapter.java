package com.zerog.neoessentials.common.adapter;

/**
 * Interface for command adapters that handle command registration and execution
 * across different Minecraft versions
 */
public interface ICommandAdapter {
    /**
     * Register a command with the server
     * @param commandName The name of the command
     * @param handlerClass The class that implements the command handler
     * @param permissionNode The permission node required to use this command
     */
    void registerCommand(String commandName, Object handlerClass, String permissionNode);
    
    /**
     * Register all mod commands
     */
    void registerAllCommands();
    
    /**
     * Initialize command system
     * This will be called during mod initialization
     */
    void initialize();
}
