package com.zerog.neoessentials.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic registry for tracking all NeoEssentials commands.
 * This allows the main command system to automatically discover and list
 * all available commands without hardcoded lists.
 */
public class CommandRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRegistry.class);
    private static final CommandRegistry INSTANCE = new CommandRegistry();
    
    // Thread-safe storage for command information
    private final Map<String, CommandInfo> commands = new ConcurrentHashMap<>();
    private final Map<String, String> aliases = new ConcurrentHashMap<>(); // alias -> main command name
    
    private CommandRegistry() {}
    
    public static CommandRegistry getInstance() {
        return INSTANCE;
    }
    
    /**
     * Register a command with the registry.
     * @param name Primary command name (without /)
     * @param description Brief description of what the command does
     * @param aliases Additional aliases for this command
     */
    public void registerCommand(String name, String description, String... aliases) {
        CommandInfo info = new CommandInfo(name, description, Arrays.asList(aliases));
        commands.put(name.toLowerCase(), info);
        
        // Register aliases pointing to main command
        for (String alias : aliases) {
            this.aliases.put(alias.toLowerCase(), name.toLowerCase());
        }
        
        LOGGER.debug("Registered command: {} with {} aliases", name, aliases.length);
    }
    
    /**
     * Register a command with just the name (no description or aliases).
     * @param name Primary command name (without /)
     */
    public void registerCommand(String name) {
        registerCommand(name, "NeoEssentials command", new String[0]);
    }
    
    /**
     * Get all registered command names (including aliases).
     * @return Set of all available command names
     */
    public Set<String> getAllCommandNames() {
        Set<String> allNames = new HashSet<>(commands.keySet());
        allNames.addAll(aliases.keySet());
        return allNames;
    }
    
    /**
     * Get all primary command names (no aliases).
     * @return Set of primary command names
     */
    public Set<String> getPrimaryCommandNames() {
        return new HashSet<>(commands.keySet());
    }
    
    /**
     * Get command information by name or alias.
     * @param name Command name or alias
     * @return CommandInfo or null if not found
     */
    public CommandInfo getCommandInfo(String name) {
        String key = name.toLowerCase();
        
        // Check if it's a primary command
        CommandInfo info = commands.get(key);
        if (info != null) {
            return info;
        }
        
        // Check if it's an alias
        String primaryName = aliases.get(key);
        if (primaryName != null) {
            return commands.get(primaryName);
        }
        
        return null;
    }
    
    /**
     * Check if a command is registered.
     * @param name Command name or alias
     * @return true if the command exists
     */
    public boolean isCommandRegistered(String name) {
        String key = name.toLowerCase();
        return commands.containsKey(key) || aliases.containsKey(key);
    }
    
    /**
     * Get a sorted list of all commands for display purposes.
     * @return List of CommandInfo objects sorted by name
     */
    public List<CommandInfo> getAllCommandsSorted() {
        List<CommandInfo> result = new ArrayList<>(commands.values());
        result.sort(Comparator.comparing(CommandInfo::getName));
        return result;
    }
    
    /**
     * Clear the registry (useful for testing or reloading).
     */
    public void clear() {
        commands.clear();
        aliases.clear();
        LOGGER.debug("Command registry cleared");
    }
    
    /**
     * Get registry statistics.
     * @return Map containing registry stats
     */
    public Map<String, Integer> getStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("commands", commands.size());
        stats.put("aliases", aliases.size());
        stats.put("total", commands.size() + aliases.size());
        return stats;
    }
    
    /**
     * Information about a registered command.
     */
    public static class CommandInfo {
        private final String name;
        private final String description;
        private final List<String> aliases;
        
        public CommandInfo(String name, String description, List<String> aliases) {
            this.name = name;
            this.description = description != null ? description : "NeoEssentials command";
            this.aliases = new ArrayList<>(aliases);
        }
        
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public List<String> getAliases() {
            return new ArrayList<>(aliases);
        }
        
        public boolean hasAliases() {
            return !aliases.isEmpty();
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(name);
            if (!aliases.isEmpty()) {
                sb.append(" (").append(String.join(", ", aliases)).append(")");
            }
            sb.append(" - ").append(description);
            return sb.toString();
        }
    }
}