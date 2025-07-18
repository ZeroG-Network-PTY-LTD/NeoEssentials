package com.zerog.neoessentials.config;

/**
 * Warp configuration for NeoEssentials
 */
public class WarpConfig {
    public boolean enabled = true;
    public int teleportDelay = 3;
    public boolean enableTeleportDelay = true;
    public boolean cancelOnMove = true;
    public boolean cancelOnDamage = true;
    public boolean enableSafetyChecks = true;
    public boolean allowUnsafeWarps = false;
    public int safetySearchRadius = 3;
    public boolean enableCrossWorldWarps = true;
    public boolean requirePermissionForCrossWorld = true;
    
    public static WarpConfig createDefault() {
        return new WarpConfig();
    }
}
