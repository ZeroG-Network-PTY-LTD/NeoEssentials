package com.zerog.neoessentials.config.dedicated;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration for TabList customization
 * Provides comprehensive player list appearance and behavior settings
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class TabListConfig {
    
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    
    // Header and Footer Settings
    public static final ModConfigSpec.ConfigValue<String> HEADER_MESSAGE;
    public static final ModConfigSpec.ConfigValue<String> FOOTER_MESSAGE;
    public static final ModConfigSpec.BooleanValue HEADER_ENABLED;
    public static final ModConfigSpec.BooleanValue FOOTER_ENABLED;
    
    // Player Display Settings
    public static final ModConfigSpec.ConfigValue<String> PLAYER_FORMAT;
    public static final ModConfigSpec.BooleanValue SHOW_PING;
    public static final ModConfigSpec.BooleanValue SHOW_HEALTH;
    public static final ModConfigSpec.BooleanValue SHOW_BALANCE;
    public static final ModConfigSpec.BooleanValue SHOW_RANK;
    
    // Color and Styling
    public static final ModConfigSpec.ConfigValue<String> HEADER_COLOR;
    public static final ModConfigSpec.ConfigValue<String> FOOTER_COLOR;
    public static final ModConfigSpec.ConfigValue<String> PLAYER_NAME_COLOR;
    public static final ModConfigSpec.ConfigValue<String> ADMIN_NAME_COLOR;
    public static final ModConfigSpec.ConfigValue<String> VIP_NAME_COLOR;
    
    // Update and Animation Settings
    public static final ModConfigSpec.IntValue UPDATE_INTERVAL;
    public static final ModConfigSpec.BooleanValue ENABLE_ANIMATIONS;
    public static final ModConfigSpec.ConfigValue<String> ANIMATION_TYPE;
    public static final ModConfigSpec.IntValue ANIMATION_SPEED;
    
    // Advanced Settings
    public static final ModConfigSpec.IntValue MAX_PLAYERS_DISPLAYED;
    public static final ModConfigSpec.BooleanValue SORT_BY_RANK;
    public static final ModConfigSpec.BooleanValue HIDE_VANISHED_PLAYERS;
    public static final ModConfigSpec.BooleanValue SHOW_WORLD_NAME;
    
    static {
        BUILDER.comment("TabList Configuration")
               .comment("Customize the appearance and behavior of the player list");
        
        BUILDER.push("header");
        HEADER_ENABLED = BUILDER
            .comment("Enable custom header message")
            .define("enabled", true);
        HEADER_MESSAGE = BUILDER
            .comment("Header message displayed at the top of the player list")
            .comment("Supports color codes (&a, &b, etc.) and placeholders")
            .comment("Placeholders: {server_name}, {online_players}, {max_players}, {motd}")
            .define("message", "&6&l» &e{server_name} &6&l«\n&7Players: &a{online_players}&7/&a{max_players}");
        HEADER_COLOR = BUILDER
            .comment("Default color for header text")
            .define("color", "&e");
        BUILDER.pop();
        
        BUILDER.push("footer");
        FOOTER_ENABLED = BUILDER
            .comment("Enable custom footer message")
            .define("enabled", true);
        FOOTER_MESSAGE = BUILDER
            .comment("Footer message displayed at the bottom of the player list")
            .comment("Supports color codes (&a, &b, etc.) and placeholders")
            .comment("Placeholders: {website}, {discord}, {version}")
            .define("message", "&7Website: &b{website}\n&7Discord: &d{discord}");
        FOOTER_COLOR = BUILDER
            .comment("Default color for footer text")
            .define("color", "&7");
        BUILDER.pop();
        
        BUILDER.push("player_display");
        PLAYER_FORMAT = BUILDER
            .comment("Format for displaying player names in the list")
            .comment("Placeholders: {player_name}, {ping}, {health}, {balance}, {rank}")
            .define("format", "{rank} {player_name} &7({ping}ms)");
        SHOW_PING = BUILDER
            .comment("Show player ping in the player list")
            .define("show_ping", true);
        SHOW_HEALTH = BUILDER
            .comment("Show player health in the player list")
            .define("show_health", false);
        SHOW_BALANCE = BUILDER
            .comment("Show player balance in the player list")
            .define("show_balance", false);
        SHOW_RANK = BUILDER
            .comment("Show player rank/prefix in the player list")
            .define("show_rank", true);
        BUILDER.pop();
        
        BUILDER.push("colors");
        PLAYER_NAME_COLOR = BUILDER
            .comment("Default color for regular player names")
            .define("player_name_color", "&f");
        ADMIN_NAME_COLOR = BUILDER
            .comment("Color for admin player names")
            .define("admin_name_color", "&c");
        VIP_NAME_COLOR = BUILDER
            .comment("Color for VIP player names")
            .define("vip_name_color", "&6");
        BUILDER.pop();
        
        BUILDER.push("updates");
        UPDATE_INTERVAL = BUILDER
            .comment("How often to update the tab list (in seconds)")
            .defineInRange("update_interval", 5, 1, 60);
        ENABLE_ANIMATIONS = BUILDER
            .comment("Enable animated text in the tab list")
            .define("enable_animations", false);
        ANIMATION_TYPE = BUILDER
            .comment("Type of animation: 'scroll', 'fade', 'pulse'")
            .define("animation_type", "scroll");
        ANIMATION_SPEED = BUILDER
            .comment("Animation speed (1-10, higher = faster)")
            .defineInRange("animation_speed", 3, 1, 10);
        BUILDER.pop();
        
        BUILDER.push("advanced");
        MAX_PLAYERS_DISPLAYED = BUILDER
            .comment("Maximum number of players to display in the list (0 = unlimited)")
            .defineInRange("max_players_displayed", 0, 0, 100);
        SORT_BY_RANK = BUILDER
            .comment("Sort players by rank/permissions in the list")
            .define("sort_by_rank", true);
        HIDE_VANISHED_PLAYERS = BUILDER
            .comment("Hide vanished players from non-admin players")
            .define("hide_vanished_players", true);
        SHOW_WORLD_NAME = BUILDER
            .comment("Show the world name players are currently in")
            .define("show_world_name", false);
        BUILDER.pop();
        
        SPEC = BUILDER.build();
    }
}
