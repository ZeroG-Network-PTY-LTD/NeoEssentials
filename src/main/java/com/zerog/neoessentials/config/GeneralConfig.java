package com.zerog.neoessentials.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * General configuration file for NeoEssentials.
 * This config contains general settings and feature toggles.
 */
public class GeneralConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    
    // General settings section
    static {
        BUILDER.comment("General NeoEssentials Settings").push("general");
    }
    
    public static final ModConfigSpec.ConfigValue<String> SERVER_NAME = BUILDER
        .comment("The server name used in various messages and placeholders")
        .define("serverName", "NeoEssentials Server");
    
    public static final ModConfigSpec.IntValue SAVE_INTERVAL = BUILDER
        .comment("How often data is automatically saved (in minutes)")
        .defineInRange("autoSaveInterval", 5, 1, 60);
    
    public static final ModConfigSpec.BooleanValue DEBUG_MODE = BUILDER
        .comment("Enable debug logging")
        .define("debugMode", false);
    
    static {
        BUILDER.pop(); // End general section
        
        // Feature toggles section
        BUILDER.comment("Feature Toggles - Enable or disable major features").push("features");
    }
    
    // Economy
    public static final ModConfigSpec.BooleanValue ENABLE_ECONOMY = BUILDER
        .comment("Enable the economy system")
        .define("enableEconomy", true);
    
    // Teleportation
    public static final ModConfigSpec.BooleanValue ENABLE_TELEPORTATION = BUILDER
        .comment("Enable all teleportation commands (home, warp, tpa, etc.)")
        .define("enableTeleportation", true);
    
    // Homes
    public static final ModConfigSpec.BooleanValue ENABLE_HOMES = BUILDER
        .comment("Enable the home system")
        .define("enableHomes", true);
    
    // Warps
    public static final ModConfigSpec.BooleanValue ENABLE_WARPS = BUILDER
        .comment("Enable the warp system")
        .define("enableWarps", true);
    
    // Kits
    public static final ModConfigSpec.BooleanValue ENABLE_KITS = BUILDER
        .comment("Enable the kit system")
        .define("enableKits", true);
    
    // Tablist
    public static final ModConfigSpec.BooleanValue ENABLE_TABLIST = BUILDER
        .comment("Enable the enhanced tablist system")
        .define("enableTablist", true);
    
    // Mail
    public static final ModConfigSpec.BooleanValue ENABLE_MAIL = BUILDER
        .comment("Enable the mail system")
        .define("enableMail", true);
    
    // Admin panel
    public static final ModConfigSpec.BooleanValue ENABLE_ADMIN_PANEL = BUILDER
        .comment("Enable the admin panel")
        .define("enableAdminPanel", true);
    
    // Moderation
    public static final ModConfigSpec.BooleanValue ENABLE_MODERATION = BUILDER
        .comment("Enable moderation commands (ban, mute, etc.)")
        .define("enableModeration", true);
    
    // Chat
    public static final ModConfigSpec.BooleanValue ENABLE_CHAT_FEATURES = BUILDER
        .comment("Enable enhanced chat features")
        .define("enableChatFeatures", true);
    
    static {
        BUILDER.pop(); // End features section
    }
    
    public static final ModConfigSpec SPEC = BUILDER.build();
}
