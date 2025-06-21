package com.zerog.neoessentials.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Kit system configuration file for NeoEssentials.
 * This config contains all kit-related settings.
 */
public class KitConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    
    // General kit settings section
    static {
        BUILDER.comment("Kit System Settings").push("kits");
    }
    
    public static final ModConfigSpec.BooleanValue USE_SEPARATE_COOLDOWNS = BUILDER
        .comment("Use separate cooldowns for each kit (if false, claiming any kit puts all on cooldown)")
        .define("useSeparateCooldowns", true);
    
    public static final ModConfigSpec.BooleanValue NOTIFY_ON_AVAILABLE = BUILDER
        .comment("Notify players when a kit becomes available again")
        .define("notifyOnAvailable", false);
    
    public static final ModConfigSpec.BooleanValue SHOW_UNAVAILABLE_KITS = BUILDER
        .comment("Show unavailable kits in the kit list command")
        .define("showUnavailableKits", true);
    
    public static final ModConfigSpec.ConfigValue<String> DEFAULT_KIT = BUILDER
        .comment("Default kit to give to new players (leave empty for none)")
        .define("defaultKit", "starter");
        
    public static final ModConfigSpec.BooleanValue OVERRIDE_FULL_INVENTORY = BUILDER
        .comment("Whether to drop kit items on the ground if inventory is full")
        .define("overrideFullInventory", true);
    
    static {
        BUILDER.pop(); // End kits section
        
        // Permission settings
        BUILDER.comment("Permission Settings").push("permissions");
    }
    
    public static final ModConfigSpec.ConfigValue<String> PERMISSION_PREFIX = BUILDER
        .comment("Permission prefix for kit commands")
        .define("permissionPrefix", "neoessentials.kit");
        
    static {
        BUILDER.pop(); // End permissions section
    }
    
    public static final ModConfigSpec SPEC = BUILDER.build();
}
