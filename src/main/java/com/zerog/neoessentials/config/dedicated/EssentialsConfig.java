package com.zerog.neoessentials.config.dedicated;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

/**
 * Configuration for Essential Commands customization
 * Provides comprehensive settings for core NeoEssentials commands
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class EssentialsConfig {
    
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    
    // Command Aliases and Shortcuts
    public static final ModConfigSpec.ConfigValue<List<? extends String>> HEAL_ALIASES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FEED_ALIASES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FLY_ALIASES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> GOD_ALIASES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> VANISH_ALIASES;
    
    // Default Values and Limits
    public static final ModConfigSpec.DoubleValue DEFAULT_FLY_SPEED;
    public static final ModConfigSpec.DoubleValue DEFAULT_WALK_SPEED;
    public static final ModConfigSpec.DoubleValue MAX_SPEED_LIMIT;
    public static final ModConfigSpec.IntValue MAX_ITEMS_PER_GIVE;
    public static final ModConfigSpec.IntValue BACK_LOCATION_HISTORY;
    
    // Feature Enable/Disable Toggles
    public static final ModConfigSpec.BooleanValue ENABLE_HEAL_COMMAND;
    public static final ModConfigSpec.BooleanValue ENABLE_FEED_COMMAND;
    public static final ModConfigSpec.BooleanValue ENABLE_FLY_COMMAND;
    public static final ModConfigSpec.BooleanValue ENABLE_GOD_COMMAND;
    public static final ModConfigSpec.BooleanValue ENABLE_VANISH_COMMAND;
    public static final ModConfigSpec.BooleanValue ENABLE_SPEED_COMMAND;
    public static final ModConfigSpec.BooleanValue ENABLE_TIME_COMMAND;
    public static final ModConfigSpec.BooleanValue ENABLE_WEATHER_COMMAND;
    public static final ModConfigSpec.BooleanValue ENABLE_GIVE_COMMAND;
    public static final ModConfigSpec.BooleanValue ENABLE_REPAIR_COMMAND;
    public static final ModConfigSpec.BooleanValue ENABLE_WORKBENCH_COMMAND;
    public static final ModConfigSpec.BooleanValue ENABLE_ANVIL_COMMAND;
    public static final ModConfigSpec.BooleanValue ENABLE_BACK_COMMAND;
    
    // Cooldown Configurations (in seconds)
    public static final ModConfigSpec.IntValue HEAL_COOLDOWN;
    public static final ModConfigSpec.IntValue FEED_COOLDOWN;
    public static final ModConfigSpec.IntValue FLY_COOLDOWN;
    public static final ModConfigSpec.IntValue REPAIR_COOLDOWN;
    public static final ModConfigSpec.IntValue WORKBENCH_COOLDOWN;
    public static final ModConfigSpec.IntValue ANVIL_COOLDOWN;
    public static final ModConfigSpec.IntValue BACK_COOLDOWN;
    
    // Cost Configurations (economy integration)
    public static final ModConfigSpec.DoubleValue HEAL_COST;
    public static final ModConfigSpec.DoubleValue FEED_COST;
    public static final ModConfigSpec.DoubleValue REPAIR_COST;
    public static final ModConfigSpec.DoubleValue WORKBENCH_COST;
    public static final ModConfigSpec.DoubleValue ANVIL_COST;
    public static final ModConfigSpec.DoubleValue BACK_COST;
    
    // Advanced Behavior Settings
    public static final ModConfigSpec.BooleanValue HEAL_REMOVES_EFFECTS;
    public static final ModConfigSpec.BooleanValue FEED_GIVES_SATURATION;
    public static final ModConfigSpec.BooleanValue VANISH_HIDES_FROM_LIST;
    public static final ModConfigSpec.BooleanValue GOD_PREVENTS_DAMAGE;
    public static final ModConfigSpec.BooleanValue REPAIR_ALL_ITEMS;
    public static final ModConfigSpec.BooleanValue GIVE_TO_INVENTORY_FIRST;
    
    // Permission Exemptions
    public static final ModConfigSpec.BooleanValue BYPASS_COOLDOWNS_WITH_PERMISSION;
    public static final ModConfigSpec.BooleanValue BYPASS_COSTS_WITH_PERMISSION;
    public static final ModConfigSpec.ConfigValue<String> COOLDOWN_BYPASS_PERMISSION;
    public static final ModConfigSpec.ConfigValue<String> COST_BYPASS_PERMISSION;
    
    static {
        BUILDER.comment("Essentials Configuration")
               .comment("Customize core NeoEssentials commands behavior and settings");
        
        BUILDER.push("aliases");
        HEAL_ALIASES = BUILDER
            .comment("Alternative command names for /heal")
            .defineList("heal_aliases", List.of("health", "restore"), obj -> obj instanceof String);
        FEED_ALIASES = BUILDER
            .comment("Alternative command names for /feed")
            .defineList("feed_aliases", List.of("food", "hunger", "eat"), obj -> obj instanceof String);
        FLY_ALIASES = BUILDER
            .comment("Alternative command names for /fly")
            .defineList("fly_aliases", List.of("flight", "floating"), obj -> obj instanceof String);
        GOD_ALIASES = BUILDER
            .comment("Alternative command names for /god")
            .defineList("god_aliases", List.of("godmode", "invincible", "invulnerable"), obj -> obj instanceof String);
        VANISH_ALIASES = BUILDER
            .comment("Alternative command names for /vanish")
            .defineList("vanish_aliases", List.of("invis", "invisible", "hide"), obj -> obj instanceof String);
        BUILDER.pop();
        
        BUILDER.push("defaults");
        DEFAULT_FLY_SPEED = BUILDER
            .comment("Default fly speed when enabling flight (0.1 = normal)")
            .defineInRange("default_fly_speed", 0.1, 0.01, 1.0);
        DEFAULT_WALK_SPEED = BUILDER
            .comment("Default walk speed when resetting speed (0.2 = normal)")
            .defineInRange("default_walk_speed", 0.2, 0.01, 1.0);
        MAX_SPEED_LIMIT = BUILDER
            .comment("Maximum speed players can set (prevents server lag)")
            .defineInRange("max_speed_limit", 5.0, 0.1, 10.0);
        MAX_ITEMS_PER_GIVE = BUILDER
            .comment("Maximum number of items that can be given with /give command")
            .defineInRange("max_items_per_give", 64, 1, 2304);
        BACK_LOCATION_HISTORY = BUILDER
            .comment("Number of previous locations to remember for /back command")
            .defineInRange("back_location_history", 5, 1, 20);
        BUILDER.pop();
        
        BUILDER.push("features");
        ENABLE_HEAL_COMMAND = BUILDER
            .comment("Enable the /heal command")
            .define("enable_heal", true);
        ENABLE_FEED_COMMAND = BUILDER
            .comment("Enable the /feed command")
            .define("enable_feed", true);
        ENABLE_FLY_COMMAND = BUILDER
            .comment("Enable the /fly command")
            .define("enable_fly", true);
        ENABLE_GOD_COMMAND = BUILDER
            .comment("Enable the /god command")
            .define("enable_god", true);
        ENABLE_VANISH_COMMAND = BUILDER
            .comment("Enable the /vanish command")
            .define("enable_vanish", true);
        ENABLE_SPEED_COMMAND = BUILDER
            .comment("Enable the /speed command")
            .define("enable_speed", true);
        ENABLE_TIME_COMMAND = BUILDER
            .comment("Enable the /time command")
            .define("enable_time", true);
        ENABLE_WEATHER_COMMAND = BUILDER
            .comment("Enable the /weather command")
            .define("enable_weather", true);
        ENABLE_GIVE_COMMAND = BUILDER
            .comment("Enable the /give command")
            .define("enable_give", true);
        ENABLE_REPAIR_COMMAND = BUILDER
            .comment("Enable the /repair command")
            .define("enable_repair", true);
        ENABLE_WORKBENCH_COMMAND = BUILDER
            .comment("Enable the /workbench command")
            .define("enable_workbench", true);
        ENABLE_ANVIL_COMMAND = BUILDER
            .comment("Enable the /anvil command")
            .define("enable_anvil", true);
        ENABLE_BACK_COMMAND = BUILDER
            .comment("Enable the /back command")
            .define("enable_back", true);
        BUILDER.pop();
        
        BUILDER.push("cooldowns");
        HEAL_COOLDOWN = BUILDER
            .comment("Cooldown for /heal command in seconds (0 = no cooldown)")
            .defineInRange("heal_cooldown", 30, 0, 3600);
        FEED_COOLDOWN = BUILDER
            .comment("Cooldown for /feed command in seconds (0 = no cooldown)")
            .defineInRange("feed_cooldown", 30, 0, 3600);
        FLY_COOLDOWN = BUILDER
            .comment("Cooldown for /fly command in seconds (0 = no cooldown)")
            .defineInRange("fly_cooldown", 0, 0, 3600);
        REPAIR_COOLDOWN = BUILDER
            .comment("Cooldown for /repair command in seconds (0 = no cooldown)")
            .defineInRange("repair_cooldown", 60, 0, 3600);
        WORKBENCH_COOLDOWN = BUILDER
            .comment("Cooldown for /workbench command in seconds (0 = no cooldown)")
            .defineInRange("workbench_cooldown", 5, 0, 3600);
        ANVIL_COOLDOWN = BUILDER
            .comment("Cooldown for /anvil command in seconds (0 = no cooldown)")
            .defineInRange("anvil_cooldown", 5, 0, 3600);
        BACK_COOLDOWN = BUILDER
            .comment("Cooldown for /back command in seconds (0 = no cooldown)")
            .defineInRange("back_cooldown", 10, 0, 3600);
        BUILDER.pop();
        
        BUILDER.push("costs");
        HEAL_COST = BUILDER
            .comment("Cost for /heal command (0 = free)")
            .defineInRange("heal_cost", 0.0, 0.0, 10000.0);
        FEED_COST = BUILDER
            .comment("Cost for /feed command (0 = free)")
            .defineInRange("feed_cost", 0.0, 0.0, 10000.0);
        REPAIR_COST = BUILDER
            .comment("Cost for /repair command (0 = free)")
            .defineInRange("repair_cost", 100.0, 0.0, 10000.0);
        WORKBENCH_COST = BUILDER
            .comment("Cost for /workbench command (0 = free)")
            .defineInRange("workbench_cost", 10.0, 0.0, 10000.0);
        ANVIL_COST = BUILDER
            .comment("Cost for /anvil command (0 = free)")
            .defineInRange("anvil_cost", 50.0, 0.0, 10000.0);
        BACK_COST = BUILDER
            .comment("Cost for /back command (0 = free)")
            .defineInRange("back_cost", 25.0, 0.0, 10000.0);
        BUILDER.pop();
        
        BUILDER.push("behavior");
        HEAL_REMOVES_EFFECTS = BUILDER
            .comment("Whether /heal should remove negative effects")
            .define("heal_removes_effects", true);
        FEED_GIVES_SATURATION = BUILDER
            .comment("Whether /feed should also give saturation")
            .define("feed_gives_saturation", true);
        VANISH_HIDES_FROM_LIST = BUILDER
            .comment("Whether vanished players are hidden from player lists")
            .define("vanish_hides_from_list", true);
        GOD_PREVENTS_DAMAGE = BUILDER
            .comment("Whether god mode prevents all damage types")
            .define("god_prevents_damage", true);
        REPAIR_ALL_ITEMS = BUILDER
            .comment("Whether /repair all should repair all items in inventory")
            .define("repair_all_items", true);
        GIVE_TO_INVENTORY_FIRST = BUILDER
            .comment("Whether /give should try inventory first before dropping items")
            .define("give_to_inventory_first", true);
        BUILDER.pop();
        
        BUILDER.push("permissions");
        BYPASS_COOLDOWNS_WITH_PERMISSION = BUILDER
            .comment("Allow players with permission to bypass cooldowns")
            .define("bypass_cooldowns_with_permission", true);
        BYPASS_COSTS_WITH_PERMISSION = BUILDER
            .comment("Allow players with permission to bypass costs")
            .define("bypass_costs_with_permission", true);
        COOLDOWN_BYPASS_PERMISSION = BUILDER
            .comment("Permission node to bypass cooldowns")
            .define("cooldown_bypass_permission", "neoessentials.bypass.cooldown");
        COST_BYPASS_PERMISSION = BUILDER
            .comment("Permission node to bypass costs")
            .define("cost_bypass_permission", "neoessentials.bypass.cost");
        BUILDER.pop();
        
        SPEC = BUILDER.build();
    }
}
