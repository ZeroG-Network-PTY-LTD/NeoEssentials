package com.zerog.neoessentials.config.dedicated;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration for Teleportation System customization
 * Provides comprehensive settings for warps, homes, and teleportation
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class TeleportationConfig {
    
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    
    // Home System Settings
    public static final ModConfigSpec.BooleanValue HOMES_ENABLED;
    public static final ModConfigSpec.IntValue MAX_HOMES_PER_PLAYER;
    public static final ModConfigSpec.IntValue HOME_TELEPORT_COOLDOWN;
    public static final ModConfigSpec.DoubleValue HOME_TELEPORT_COST;
    public static final ModConfigSpec.DoubleValue SET_HOME_COST;
    public static final ModConfigSpec.IntValue HOME_TELEPORT_DELAY;
    
    // Warp System Settings
    public static final ModConfigSpec.BooleanValue WARPS_ENABLED;
    public static final ModConfigSpec.IntValue WARP_TELEPORT_COOLDOWN;
    public static final ModConfigSpec.DoubleValue WARP_TELEPORT_COST;
    public static final ModConfigSpec.DoubleValue CREATE_WARP_COST;
    public static final ModConfigSpec.IntValue WARP_TELEPORT_DELAY;
    public static final ModConfigSpec.BooleanValue WARP_CATEGORIES_ENABLED;
    
    // TPA System Settings
    public static final ModConfigSpec.BooleanValue TPA_ENABLED;
    public static final ModConfigSpec.IntValue TPA_REQUEST_TIMEOUT;
    public static final ModConfigSpec.IntValue TPA_COOLDOWN;
    public static final ModConfigSpec.DoubleValue TPA_COST;
    public static final ModConfigSpec.IntValue MAX_CONCURRENT_REQUESTS;
    public static final ModConfigSpec.IntValue TPA_TELEPORT_DELAY;
    
    // Spawn System Settings
    public static final ModConfigSpec.BooleanValue SPAWN_ENABLED;
    public static final ModConfigSpec.IntValue SPAWN_TELEPORT_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SPAWN_TELEPORT_COST;
    public static final ModConfigSpec.IntValue SPAWN_TELEPORT_DELAY;
    public static final ModConfigSpec.BooleanValue SPAWN_ON_FIRST_JOIN;
    public static final ModConfigSpec.BooleanValue SPAWN_ON_DEATH;
    
    // Back System Settings
    public static final ModConfigSpec.BooleanValue BACK_ENABLED;
    public static final ModConfigSpec.IntValue BACK_COOLDOWN;
    public static final ModConfigSpec.DoubleValue BACK_COST;
    public static final ModConfigSpec.IntValue BACK_TELEPORT_DELAY;
    public static final ModConfigSpec.IntValue BACK_LOCATION_HISTORY;
    
    // Cross-Dimension Settings
    public static final ModConfigSpec.BooleanValue ALLOW_CROSS_DIMENSION_HOMES;
    public static final ModConfigSpec.BooleanValue ALLOW_CROSS_DIMENSION_WARPS;
    public static final ModConfigSpec.BooleanValue ALLOW_CROSS_DIMENSION_TPA;
    public static final ModConfigSpec.DoubleValue CROSS_DIMENSION_COST_MULTIPLIER;
    
    // Safety and Validation
    public static final ModConfigSpec.BooleanValue SAFE_TELEPORT_ENABLED;
    public static final ModConfigSpec.IntValue SAFE_TELEPORT_SEARCH_RADIUS;
    public static final ModConfigSpec.BooleanValue PREVENT_TELEPORT_IN_COMBAT;
    public static final ModConfigSpec.IntValue COMBAT_TIMEOUT_SECONDS;
    public static final ModConfigSpec.BooleanValue VALIDATE_TELEPORT_LOCATIONS;
    
    // Movement and Cancellation
    public static final ModConfigSpec.BooleanValue CANCEL_ON_MOVEMENT;
    public static final ModConfigSpec.DoubleValue MOVEMENT_THRESHOLD;
    public static final ModConfigSpec.BooleanValue CANCEL_ON_DAMAGE;
    public static final ModConfigSpec.BooleanValue SHOW_TELEPORT_COUNTDOWN;
    
    // Permission Integration
    public static final ModConfigSpec.BooleanValue USE_PERMISSION_LIMITS;
    public static final ModConfigSpec.ConfigValue<String> HOMES_LIMIT_PERMISSION_PREFIX;
    public static final ModConfigSpec.ConfigValue<String> BYPASS_COOLDOWN_PERMISSION;
    public static final ModConfigSpec.ConfigValue<String> BYPASS_COST_PERMISSION;
    public static final ModConfigSpec.ConfigValue<String> BYPASS_DELAY_PERMISSION;
    
    // Advanced Features
    public static final ModConfigSpec.BooleanValue ENABLE_TELEPORT_WARMUP;
    public static final ModConfigSpec.BooleanValue ENABLE_PARTICLE_EFFECTS;
    public static final ModConfigSpec.BooleanValue ENABLE_SOUND_EFFECTS;
    public static final ModConfigSpec.ConfigValue<String> TELEPORT_PARTICLE_TYPE;
    public static final ModConfigSpec.ConfigValue<String> TELEPORT_SOUND;
    
    static {
        BUILDER.comment("Teleportation Configuration")
               .comment("Comprehensive settings for all teleportation systems");
        
        BUILDER.push("homes");
        HOMES_ENABLED = BUILDER
            .comment("Enable the home system")
            .define("homes_enabled", true);
        MAX_HOMES_PER_PLAYER = BUILDER
            .comment("Maximum number of homes per player (0 = unlimited)")
            .defineInRange("max_homes_per_player", 5, 0, 50);
        HOME_TELEPORT_COOLDOWN = BUILDER
            .comment("Cooldown for home teleportation in seconds")
            .defineInRange("home_teleport_cooldown", 0, 0, 3600);
        HOME_TELEPORT_COST = BUILDER
            .comment("Cost for teleporting to home")
            .defineInRange("home_teleport_cost", 0.0, 0.0, 10000.0);
        SET_HOME_COST = BUILDER
            .comment("Cost for setting a home")
            .defineInRange("set_home_cost", 0.0, 0.0, 10000.0);
        HOME_TELEPORT_DELAY = BUILDER
            .comment("Delay before home teleportation in seconds")
            .defineInRange("home_teleport_delay", 0, 0, 30);
        BUILDER.pop();
        
        BUILDER.push("warps");
        WARPS_ENABLED = BUILDER
            .comment("Enable the warp system")
            .define("warps_enabled", true);
        WARP_TELEPORT_COOLDOWN = BUILDER
            .comment("Cooldown for warp teleportation in seconds")
            .defineInRange("warp_teleport_cooldown", 0, 0, 3600);
        WARP_TELEPORT_COST = BUILDER
            .comment("Cost for teleporting to warps")
            .defineInRange("warp_teleport_cost", 0.0, 0.0, 10000.0);
        CREATE_WARP_COST = BUILDER
            .comment("Cost for creating warps")
            .defineInRange("create_warp_cost", 1000.0, 0.0, 100000.0);
        WARP_TELEPORT_DELAY = BUILDER
            .comment("Delay before warp teleportation in seconds")
            .defineInRange("warp_teleport_delay", 0, 0, 30);
        WARP_CATEGORIES_ENABLED = BUILDER
            .comment("Enable warp categories for organization")
            .define("warp_categories_enabled", true);
        BUILDER.pop();
        
        BUILDER.push("tpa");
        TPA_ENABLED = BUILDER
            .comment("Enable the TPA (teleport request) system")
            .define("tpa_enabled", true);
        TPA_REQUEST_TIMEOUT = BUILDER
            .comment("TPA request timeout in seconds")
            .defineInRange("tpa_request_timeout", 60, 10, 300);
        TPA_COOLDOWN = BUILDER
            .comment("Cooldown between TPA requests in seconds")
            .defineInRange("tpa_cooldown", 30, 0, 3600);
        TPA_COST = BUILDER
            .comment("Cost for sending TPA requests")
            .defineInRange("tpa_cost", 0.0, 0.0, 1000.0);
        MAX_CONCURRENT_REQUESTS = BUILDER
            .comment("Maximum concurrent TPA requests per player")
            .defineInRange("max_concurrent_requests", 3, 1, 10);
        TPA_TELEPORT_DELAY = BUILDER
            .comment("Delay before TPA teleportation in seconds")
            .defineInRange("tpa_teleport_delay", 3, 0, 30);
        BUILDER.pop();
        
        BUILDER.push("spawn");
        SPAWN_ENABLED = BUILDER
            .comment("Enable the spawn system")
            .define("spawn_enabled", true);
        SPAWN_TELEPORT_COOLDOWN = BUILDER
            .comment("Cooldown for spawn teleportation in seconds")
            .defineInRange("spawn_teleport_cooldown", 0, 0, 3600);
        SPAWN_TELEPORT_COST = BUILDER
            .comment("Cost for teleporting to spawn")
            .defineInRange("spawn_teleport_cost", 0.0, 0.0, 1000.0);
        SPAWN_TELEPORT_DELAY = BUILDER
            .comment("Delay before spawn teleportation in seconds")
            .defineInRange("spawn_teleport_delay", 0, 0, 30);
        SPAWN_ON_FIRST_JOIN = BUILDER
            .comment("Teleport players to spawn on first join")
            .define("spawn_on_first_join", true);
        SPAWN_ON_DEATH = BUILDER
            .comment("Teleport players to spawn on death")
            .define("spawn_on_death", false);
        BUILDER.pop();
        
        BUILDER.push("back");
        BACK_ENABLED = BUILDER
            .comment("Enable the back command")
            .define("back_enabled", true);
        BACK_COOLDOWN = BUILDER
            .comment("Cooldown for back command in seconds")
            .defineInRange("back_cooldown", 10, 0, 3600);
        BACK_COST = BUILDER
            .comment("Cost for using back command")
            .defineInRange("back_cost", 25.0, 0.0, 1000.0);
        BACK_TELEPORT_DELAY = BUILDER
            .comment("Delay before back teleportation in seconds")
            .defineInRange("back_teleport_delay", 3, 0, 30);
        BACK_LOCATION_HISTORY = BUILDER
            .comment("Number of previous locations to remember")
            .defineInRange("back_location_history", 5, 1, 20);
        BUILDER.pop();
        
        BUILDER.push("cross_dimension");
        ALLOW_CROSS_DIMENSION_HOMES = BUILDER
            .comment("Allow homes in different dimensions")
            .define("allow_cross_dimension_homes", true);
        ALLOW_CROSS_DIMENSION_WARPS = BUILDER
            .comment("Allow warps in different dimensions")
            .define("allow_cross_dimension_warps", true);
        ALLOW_CROSS_DIMENSION_TPA = BUILDER
            .comment("Allow TPA between different dimensions")
            .define("allow_cross_dimension_tpa", true);
        CROSS_DIMENSION_COST_MULTIPLIER = BUILDER
            .comment("Cost multiplier for cross-dimension teleportation")
            .defineInRange("cross_dimension_cost_multiplier", 2.0, 1.0, 10.0);
        BUILDER.pop();
        
        BUILDER.push("safety");
        SAFE_TELEPORT_ENABLED = BUILDER
            .comment("Enable safe teleportation (find safe landing spots)")
            .define("safe_teleport_enabled", true);
        SAFE_TELEPORT_SEARCH_RADIUS = BUILDER
            .comment("Radius to search for safe teleport locations")
            .defineInRange("safe_teleport_search_radius", 5, 1, 20);
        PREVENT_TELEPORT_IN_COMBAT = BUILDER
            .comment("Prevent teleportation while in combat")
            .define("prevent_teleport_in_combat", false);
        COMBAT_TIMEOUT_SECONDS = BUILDER
            .comment("How long after damage to consider player in combat")
            .defineInRange("combat_timeout_seconds", 10, 5, 60);
        VALIDATE_TELEPORT_LOCATIONS = BUILDER
            .comment("Validate teleport destinations before teleporting")
            .define("validate_teleport_locations", true);
        BUILDER.pop();
        
        BUILDER.push("movement");
        CANCEL_ON_MOVEMENT = BUILDER
            .comment("Cancel teleportation if player moves during delay")
            .define("cancel_on_movement", true);
        MOVEMENT_THRESHOLD = BUILDER
            .comment("Movement distance threshold to cancel teleportation")
            .defineInRange("movement_threshold", 1.0, 0.1, 10.0);
        CANCEL_ON_DAMAGE = BUILDER
            .comment("Cancel teleportation if player takes damage")
            .define("cancel_on_damage", true);
        SHOW_TELEPORT_COUNTDOWN = BUILDER
            .comment("Show countdown timer during teleportation delay")
            .define("show_teleport_countdown", true);
        BUILDER.pop();
        
        BUILDER.push("permissions");
        USE_PERMISSION_LIMITS = BUILDER
            .comment("Use permissions to determine home limits")
            .define("use_permission_limits", false);
        HOMES_LIMIT_PERMISSION_PREFIX = BUILDER
            .comment("Permission prefix for home limits (e.g., 'neoessentials.homes.')")
            .define("homes_limit_permission_prefix", "neoessentials.homes.");
        BYPASS_COOLDOWN_PERMISSION = BUILDER
            .comment("Permission to bypass teleportation cooldowns")
            .define("bypass_cooldown_permission", "neoessentials.teleport.bypass.cooldown");
        BYPASS_COST_PERMISSION = BUILDER
            .comment("Permission to bypass teleportation costs")
            .define("bypass_cost_permission", "neoessentials.teleport.bypass.cost");
        BYPASS_DELAY_PERMISSION = BUILDER
            .comment("Permission to bypass teleportation delays")
            .define("bypass_delay_permission", "neoessentials.teleport.bypass.delay");
        BUILDER.pop();
        
        BUILDER.push("effects");
        ENABLE_TELEPORT_WARMUP = BUILDER
            .comment("Enable visual warmup effects before teleportation")
            .define("enable_teleport_warmup", true);
        ENABLE_PARTICLE_EFFECTS = BUILDER
            .comment("Enable particle effects during teleportation")
            .define("enable_particle_effects", true);
        ENABLE_SOUND_EFFECTS = BUILDER
            .comment("Enable sound effects for teleportation")
            .define("enable_sound_effects", true);
        TELEPORT_PARTICLE_TYPE = BUILDER
            .comment("Particle type for teleportation effects")
            .define("teleport_particle_type", "portal");
        TELEPORT_SOUND = BUILDER
            .comment("Sound effect for teleportation")
            .define("teleport_sound", "entity.enderman.teleport");
        BUILDER.pop();
        
        SPEC = BUILDER.build();
    }
}
