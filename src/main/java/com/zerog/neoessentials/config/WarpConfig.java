package com.zerog.neoessentials.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Warp system configuration file for NeoEssentials.
 * This config contains all warp-related settings.
 */
public class WarpConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    
    // General warp settings section
    static {
        BUILDER.comment("Warp System Settings").push("warps");
    }
    
    public static final ModConfigSpec.IntValue COOLDOWN_SECONDS = BUILDER
        .comment("Cooldown between warp teleportations (in seconds)")
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
        
    public static final ModConfigSpec.BooleanValue LIST_SHOWS_PRIVATE = BUILDER
        .comment("Whether /warps list shows private warps to regular users")
        .define("listShowsPrivate", false);
        
    public static final ModConfigSpec.BooleanValue ALLOW_PLAYER_WARPS = BUILDER
        .comment("Allow players to create their own warps")
        .define("allowPlayerWarps", true);
        
    public static final ModConfigSpec.IntValue DEFAULT_MAX_PLAYER_WARPS = BUILDER
        .comment("Default maximum number of warps per player")
        .defineInRange("defaultMaxPlayerWarps", 2, 0, 1000);
    
    static {
        BUILDER.pop(); // End warps section
        
        // Permission settings
        BUILDER.comment("Permission Settings").push("permissions");
    }
    
    public static final ModConfigSpec.ConfigValue<String> PERMISSION_PREFIX = BUILDER
        .comment("Permission prefix for warp commands")
        .define("permissionPrefix", "neoessentials.warp");
        
    public static final ModConfigSpec.BooleanValue USE_MAX_WARPS_PERMISSION = BUILDER
        .comment("Use permission-based warp limits (neoessentials.warp.max.#)")
        .define("useMaxWarpsPermission", true);
    
    static {
        BUILDER.pop(); // End permissions section
    }
    
    public static final ModConfigSpec SPEC = BUILDER.build();
}
