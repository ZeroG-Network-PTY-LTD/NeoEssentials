package com.zerog.neoessentials.config;

/**
 * Home configuration for NeoEssentials
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class HomeConfig {
    
    public boolean enabled = true;
    public int maxHomesDefault = 3;
    public int maxHomesVIP = 5;
    public int maxHomesAdmin = 10;
    
    // Teleport settings
    public int teleportDelay = 3; // seconds
    public boolean enableTeleportDelay = true;
    public boolean cancelOnMove = true;
    public boolean cancelOnDamage = true;
    
    // Safety settings
    public boolean enableSafetyChecks = true;
    public boolean allowUnsafeHomes = false;
    public int safetySearchRadius = 3;
    
    // Bed homes
    public boolean enableBedHomes = true;
    public boolean setBedAsHome = true;
    public String bedHomeName = "bed";
    
    // Cross-world homes
    public boolean enableCrossWorldHomes = true;
    public boolean requirePermissionForCrossWorld = true;
    
    public static HomeConfig createDefault() {
        return new HomeConfig();
    }
}
