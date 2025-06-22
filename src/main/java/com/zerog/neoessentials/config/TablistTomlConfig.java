package com.zerog.neoessentials.config;

import java.util.Arrays;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Tablist system configuration for NeoEssentials.
 */
public class TablistTomlConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    
    // General tablist settings
    static {
        BUILDER.comment("Tablist System Settings").push("tablist");
    }
    
    public static final ModConfigSpec.LongValue UPDATE_INTERVAL = BUILDER
        .comment("How often to update the tablist (in milliseconds)")
        .defineInRange("updateInterval", 2000L, 500L, 30000L);
    
    public static final ModConfigSpec.ConfigValue<String> TIME_FORMAT = BUILDER
        .comment("Time format for %time% placeholder (Java DateTimeFormatter syntax)")
        .define("timeFormat", "HH:mm:ss");
    
    // Sorting settings
    public static final ModConfigSpec.BooleanValue ENABLE_SORTING = BUILDER
        .comment("Enable sorting of players in the tablist")
        .define("enableSorting", true);
    
    public static final ModConfigSpec.ConfigValue<String> SORT_TYPE = BUILDER
        .comment("Sort type (name, rank, playtime)")
        .define("sortType", "name");
    
    // Display settings
    public static final ModConfigSpec.BooleanValue SHOW_ECONOMY_IN_TABLIST = BUILDER
        .comment("Show players' economy balances in tablist")
        .define("showEconomyInTablist", true);
    
    public static final ModConfigSpec.BooleanValue ENABLE_PLAYER_SPECIFIC_HEADERS = BUILDER
        .comment("Allow per-player custom headers based on permissions")
        .define("enablePlayerSpecificHeaders", true);
    
    public static final ModConfigSpec.BooleanValue ENABLE_PLAYER_SPECIFIC_FOOTERS = BUILDER
        .comment("Allow per-player custom footers based on permissions")
        .define("enablePlayerSpecificFooters", true);
    
    // Animation settings
    public static final ModConfigSpec.BooleanValue ENABLE_ANIMATIONS = BUILDER
        .comment("Enable tablist animations")
        .define("enableAnimations", true);
        
    public static final ModConfigSpec.IntValue ANIMATION_SPEED = BUILDER
        .comment("Animation speed multiplier (higher = faster)")
        .defineInRange("animationSpeed", 1, 1, 10);
        
    public static final ModConfigSpec.ConfigValue<String> HEADER_ANIMATION_TYPE = BUILDER
        .comment("Animation type for headers (none, rotation, scroll, fade, rainbow, typewriter, blink)")
        .define("headerAnimationType", "rotation");
        
    public static final ModConfigSpec.ConfigValue<String> FOOTER_ANIMATION_TYPE = BUILDER
        .comment("Animation type for footers (none, rotation, scroll, fade, rainbow, typewriter, blink)")
        .define("footerAnimationType", "rotation");
        
    public static final ModConfigSpec.IntValue SCROLL_WIDTH = BUILDER
        .comment("Number of characters visible in scrolling text")
        .defineInRange("scrollWidth", 20, 10, 100);
    
    static {
        BUILDER.pop(); // End tablist section
        
        // Header/footer content
        BUILDER.comment("Header and Footer Templates").push("templates");
    }    public static final ModConfigSpec.ConfigValue<List<? extends String>> HEADERS = BUILDER
        .comment("List of header lines to display in rotation (supports placeholders like %server%, %online%, %time%)")
        .define("headers", 
            Arrays.asList(
                "&6&l✦ &b&lNeoEssentials Server &6&l✦",
                "&eWelcome, &a%player%&e!",
                "&eOnline players: &a%online%/%max%",
                "&eServer time: &a%time%"
            ));
      public static final ModConfigSpec.ConfigValue<List<? extends String>> FOOTERS = BUILDER
        .comment("List of footer lines to display in rotation (supports placeholders like %server%, %online%, %time%)")
        .define("footers", 
            Arrays.asList(
                "&eBalance: &a%balance% coins",
                "&eWebsite: &awww.example.com",
                "&eThanks for playing!",
                "&eRunning &aNeoForge %neoforge%"
            ));
    
    static {
        BUILDER.pop(); // End templates section
    }
    
    public static final ModConfigSpec SPEC = BUILDER.build();
}
