package com.zerog.neoessentials.data;

/**
 * Plugin Integration Data
 */
public class PluginIntegration {
    private final String name;
    private final String version;
    private final PluginType type;
    private final PluginPriority priority;
    private final PluginStatusEnum status;
    
    public PluginIntegration(String name, String version, PluginType type, PluginPriority priority, PluginStatusEnum status) {
        this.name = name;
        this.version = version;
        this.type = type;
        this.priority = priority;
        this.status = status;
    }
    
    public String getName() { return name; }
    public String getVersion() { return version; }
    public PluginType getType() { return type; }
    public PluginPriority getPriority() { return priority; }
    public PluginStatusEnum getStatus() { return status; }
}
