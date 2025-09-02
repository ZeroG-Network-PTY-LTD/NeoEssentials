package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

/**
 * Base interface for all essential commands
 * Provides standard structure and documentation
 * 
 * @author NeoEssentials Team
 * @since 1.0.0
 */
public interface IEssentialCommand {
    
    /**
     * Register the command with the dispatcher
     * 
     * @param dispatcher The command dispatcher
     */
    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        throw new UnsupportedOperationException("Each command must implement its own register method");
    }
    
    /**
     * Get the command name
     * 
     * @return The primary command name
     */
    default String getCommandName() {
        return this.getClass().getSimpleName().replace("Command", "").toLowerCase();
    }
    
    /**
     * Get command aliases
     * 
     * @return Array of command aliases, can be empty
     */
    default String[] getAliases() {
        return new String[0];
    }
    
    /**
     * Get command description
     * 
     * @return Command description for help systems
     */
    default String getDescription() {
        return "No description available";
    }
    
    /**
     * Get command usage
     * 
     * @return Command usage string
     */
    default String getUsage() {
        return "/" + getCommandName();
    }
    
    /**
     * Get required permission
     * 
     * @return Base permission node for this command
     */
    default String getPermission() {
        return "neoessentials." + getCommandName();
    }
}
