package com.zerog.neoessentials.commands.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Enhanced command registry manager for better command organization and management
 */
public class CommandRegistryManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRegistryManager.class);
    
    private final CommandDispatcher<CommandSourceStack> dispatcher;
    private final CommandBuildContext context;
    
    // Command categories for better organization
    private final Map<String, List<RegisteredCommand>> commandCategories = new LinkedHashMap<>();
    private final Map<String, RegisteredCommand> allCommands = new HashMap<>();
    
    public CommandRegistryManager(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        this.dispatcher = dispatcher;
        this.context = context;
        initializeCategories();
    }
    
    private void initializeCategories() {
        commandCategories.put("Essential Utilities", new ArrayList<>());
        commandCategories.put("Player Management", new ArrayList<>());
        commandCategories.put("Moderation", new ArrayList<>());
        commandCategories.put("Communication", new ArrayList<>());
        commandCategories.put("Teleportation", new ArrayList<>());
        commandCategories.put("Economy", new ArrayList<>());
        commandCategories.put("Administration", new ArrayList<>());
        commandCategories.put("Permissions", new ArrayList<>());
        commandCategories.put("Configuration", new ArrayList<>());
        commandCategories.put("Advanced Features", new ArrayList<>());
    }
    
    /**
     * Register a command with proper categorization and error handling
     */
    public void registerCommand(String category, String commandName, String description, 
                               Consumer<CommandDispatcher<CommandSourceStack>> registrationMethod) {
        registerCommand(category, commandName, description, (dispatcher, context) -> registrationMethod.accept(dispatcher));
    }
    
    /**
     * Register a command with proper categorization and error handling (with context)
     */
    public void registerCommand(String category, String commandName, String description, 
                               BiConsumer<CommandDispatcher<CommandSourceStack>, CommandBuildContext> registrationMethod) {
        try {
            LOGGER.debug("Registering command: {} in category: {}", commandName, category);
            
            registrationMethod.accept(dispatcher, context);
            
            RegisteredCommand command = new RegisteredCommand(commandName, description, category);
            commandCategories.computeIfAbsent(category, k -> new ArrayList<>()).add(command);
            allCommands.put(commandName.toLowerCase(), command);
            
            LOGGER.debug("Successfully registered command: {}", commandName);
            
        } catch (Exception e) {
            LOGGER.error("Failed to register command: {} in category: {}", commandName, category, e);
        }
    }
    
    /**
     * Register a command builder directly
     */
    public void registerCommandBuilder(String category, String commandName, String description,
                                     LiteralArgumentBuilder<CommandSourceStack> commandBuilder) {
        try {
            LOGGER.debug("Registering command builder: {} in category: {}", commandName, category);
            
            dispatcher.register(commandBuilder);
            
            RegisteredCommand command = new RegisteredCommand(commandName, description, category);
            commandCategories.computeIfAbsent(category, k -> new ArrayList<>()).add(command);
            allCommands.put(commandName.toLowerCase(), command);
            
            LOGGER.debug("Successfully registered command builder: {}", commandName);
            
        } catch (Exception e) {
            LOGGER.error("Failed to register command builder: {} in category: {}", commandName, category, e);
        }
    }
    
    /**
     * Get all registered commands by category
     */
    public Map<String, List<RegisteredCommand>> getCommandsByCategory() {
        return Collections.unmodifiableMap(commandCategories);
    }
    
    /**
     * Get a specific command by name
     */
    public Optional<RegisteredCommand> getCommand(String name) {
        return Optional.ofNullable(allCommands.get(name.toLowerCase()));
    }
    
    /**
     * Get all commands in a specific category
     */
    public List<RegisteredCommand> getCommandsInCategory(String category) {
        return Collections.unmodifiableList(commandCategories.getOrDefault(category, new ArrayList<>()));
    }
    
    /**
     * Get total command count
     */
    public int getTotalCommandCount() {
        return allCommands.size();
    }
    
    /**
     * Get category count
     */
    public int getCategoryCount() {
        return commandCategories.size();
    }
    
    /**
     * Print registration summary
     */
    public void printRegistrationSummary() {
        LOGGER.info("Command Registration Summary:");
        LOGGER.info("Total Commands: {}", getTotalCommandCount());
        LOGGER.info("Categories: {}", getCategoryCount());
        
        for (Map.Entry<String, List<RegisteredCommand>> entry : commandCategories.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                LOGGER.info("  {}: {} commands", entry.getKey(), entry.getValue().size());
                for (RegisteredCommand command : entry.getValue()) {
                    LOGGER.debug("    - {} ({})", command.getName(), command.getDescription());
                }
            }
        }
    }
    
    /**
     * Represents a registered command with metadata
     */
    public static class RegisteredCommand {
        private final String name;
        private final String description;
        private final String category;
        private final long registrationTime;
        
        public RegisteredCommand(String name, String description, String category) {
            this.name = name;
            this.description = description;
            this.category = category;
            this.registrationTime = System.currentTimeMillis();
        }
        
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getCategory() {
            return category;
        }
        
        public long getRegistrationTime() {
            return registrationTime;
        }
        
        @Override
        public String toString() {
            return String.format("Command{name='%s', description='%s', category='%s'}", 
                               name, description, category);
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RegisteredCommand that = (RegisteredCommand) o;
            return Objects.equals(name, that.name);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
