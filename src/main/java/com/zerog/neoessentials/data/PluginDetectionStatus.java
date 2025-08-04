package com.zerog.neoessentials.data;

/**
 * Plugin detection and integration status information
 */
public class PluginDetectionStatus {
    private final String id;
    private final String name;
    private final String version;
    private final boolean available;

    public PluginDetectionStatus(String id, String name, String version, boolean available) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.available = available;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public boolean isAvailable() {
        return available;
    }
}
