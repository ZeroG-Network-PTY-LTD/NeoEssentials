package com.zerog.neoessentials.integration;

/**
 * Plugin type categories
 */
public enum PluginType {
    ECONOMY("Economy"),
    PERMISSIONS("Permissions"),
    PROTECTION("Protection"),
    COMMUNICATION("Communication"),
    UTILITY("Utility"),
    COMMANDS("Commands"),
    RPG("RPG"),
    MODERATION("Moderation"),
    CHAT("Chat"),
    WORLD("World Management"),
    ESSENTIALS("Essentials"),
    INTEGRATION("Integration"),
    ENHANCEMENT("Enhancement"),
    OTHER("Other");
    
    private final String displayName;
    
    PluginType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() { return displayName; }
}
