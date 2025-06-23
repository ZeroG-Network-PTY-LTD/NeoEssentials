package com.zerog.neoessentials.ui.tablist.placeholders;

import net.minecraft.server.level.ServerPlayer;
import java.util.function.BiFunction;

/**
 * Represents a custom placeholder that can be used in tablist text
 */
public class CustomPlaceholder {
    private final String name;
    private final String description;
    private final BiFunction<ServerPlayer, String[], String> valueProvider;
    
    /**
     * Creates a new custom placeholder
     * 
     * @param name The name of the placeholder, without % symbols (e.g., "custom_rank")
     * @param description A user-friendly description
     * @param valueProvider A function that provides the placeholder's value
     */
    public CustomPlaceholder(String name, String description, 
                             BiFunction<ServerPlayer, String[], String> valueProvider) {
        this.name = name;
        this.description = description;
        this.valueProvider = valueProvider;
    }
    
    /**
     * Gets the name of the placeholder
     * @return The placeholder's name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the description of the placeholder
     * @return The placeholder's description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the placeholder's value for a specific player
     * 
     * @param player The player to evaluate the placeholder for
     * @param args Any arguments passed to the placeholder
     * @return The placeholder's value
     */
    public String getValue(ServerPlayer player, String[] args) {
        try {
            return valueProvider.apply(player, args);
        } catch (Exception e) {
            return "[Error: " + e.getMessage() + "]";
        }
    }
}
