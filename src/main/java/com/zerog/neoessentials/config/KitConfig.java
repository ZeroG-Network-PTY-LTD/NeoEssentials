package com.zerog.neoessentials.config;

/**
 * Kit configuration for NeoEssentials
 */
public class KitConfig {
    public boolean enabled = true;
    public int defaultCooldown = 300; // 5 minutes
    public boolean enableCooldowns = true;
    public boolean enablePermissions = true;
    public boolean enableOneTimeKits = true;
    public boolean enableKitPreview = true;
    public boolean enableKitSigns = true;
    public boolean enableKitCosts = true;
    
    public static KitConfig createDefault() {
        return new KitConfig();
    }
}
