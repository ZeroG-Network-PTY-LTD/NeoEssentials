package com.zerog.neoessentials.data;

/**
 * Plugin integration status enumeration
 */
public enum PluginStatusEnum {
    DETECTED("Detected"),
    INTEGRATED("Integrated"), 
    FAILED("Failed"),
    ERROR("Error"),
    DISABLED("Disabled");
    
    private final String displayName;
    
    PluginStatusEnum(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() { return displayName; }
}
