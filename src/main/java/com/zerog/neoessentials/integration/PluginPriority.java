package com.zerog.neoessentials.integration;

/**
 * Plugin priority levels for loading and compatibility
 */
public enum PluginPriority {
    HIGH("High Priority", 1),
    MEDIUM("Medium Priority", 2), 
    LOW("Low Priority", 3);
    
    private final String displayName;
    private final int priority;
    
    PluginPriority(String displayName, int priority) {
        this.displayName = displayName;
        this.priority = priority;
    }
    
    public String getDisplayName() { return displayName; }
    public int getPriority() { return priority; }
}
