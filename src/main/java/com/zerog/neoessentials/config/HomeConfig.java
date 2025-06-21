package com.zerog.neoessentials.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Home system configuration file for NeoEssentials.
 * This config contains all home-related settings.
 */
public class HomeConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    
    // General home settings section
    static {
        BUILDER.comment("Home System Settings").push("homes");
    }
    
    public static final ModConfigSpec.IntValue DEFAULT_MAX_HOMES = BUILDER
        .comment("Default maximum number of homes per player")
        .defineInRange("defaultMaxHomes", 3, 1, 1000);
    
    public static final ModConfigSpec.IntValue COOLDOWN_SECONDS = BUILDER
        .comment("Cooldown between home teleportations (in seconds)")
        .defineInRange("cooldownSeconds", 30, 0, Integer.MAX_VALUE);
    
    public static final ModConfigSpec.IntValue WARMUP_SECONDS = BUILDER
        .comment("Warmup delay before teleportation (in seconds)")
        .defineInRange("warmupSeconds", 3, 0, Integer.MAX_VALUE);
    
    public static final ModConfigSpec.BooleanValue CANCEL_ON_MOVE = BUILDER
        .comment("Cancel teleportation if player moves during warmup")
        .define("cancelOnMove", true);
    
    public static final ModConfigSpec.BooleanValue CANCEL_ON_DAMAGE = BUILDER
        .comment("Cancel teleportation if player takes damage during warmup")
        .define("cancelOnDamage", true);
        
    public static final ModConfigSpec.BooleanValue ALLOW_CROSS_DIMENSION = BUILDER
        .comment("Allow teleporting across dimensions")
        .define("allowCrossDimension", true);
        
    static {
        BUILDER.pop(); // End homes section
        
        // Permission settings
        BUILDER.comment("Permission Settings").push("permissions");
    }
    
    public static final ModConfigSpec.ConfigValue<String> PERMISSION_PREFIX = BUILDER
        .comment("Permission prefix for home commands")
        .define("permissionPrefix", "neoessentials.home");
        
    public static final ModConfigSpec.BooleanValue USE_MAX_HOMES_PERMISSION = BUILDER
        .comment("Use permission-based home limits (neoessentials.home.max.#)")
        .define("useMaxHomesPermission", true);
    
    static {
        BUILDER.pop(); // End permissions section
    }
    
    public static final ModConfigSpec SPEC = BUILDER.build();
}
