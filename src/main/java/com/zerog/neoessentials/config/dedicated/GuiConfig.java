package com.zerog.neoessentials.config.dedicated;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration for GUI System customization
 * Provides comprehensive settings for all graphical user interfaces
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class GuiConfig {
    
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    
    // Menu Layout Settings
    public static final ModConfigSpec.IntValue INVENTORY_ROWS;
    public static final ModConfigSpec.ConfigValue<String> MENU_TITLE_FORMAT;
    public static final ModConfigSpec.BooleanValue ENABLE_MENU_SOUNDS;
    public static final ModConfigSpec.ConfigValue<String> CLICK_SOUND;
    public static final ModConfigSpec.ConfigValue<String> NAVIGATION_SOUND;
    
    // Color Schemes and Styling
    public static final ModConfigSpec.ConfigValue<String> PRIMARY_COLOR;
    public static final ModConfigSpec.ConfigValue<String> SECONDARY_COLOR;
    public static final ModConfigSpec.ConfigValue<String> ACCENT_COLOR;
    public static final ModConfigSpec.ConfigValue<String> ERROR_COLOR;
    public static final ModConfigSpec.ConfigValue<String> SUCCESS_COLOR;
    
    // Shop GUI Settings
    public static final ModConfigSpec.BooleanValue SHOP_ENABLE_CATEGORIES;
    public static final ModConfigSpec.IntValue SHOP_ITEMS_PER_PAGE;
    public static final ModConfigSpec.BooleanValue SHOP_SHOW_PRICES;
    public static final ModConfigSpec.BooleanValue SHOP_SHOW_DESCRIPTIONS;
    public static final ModConfigSpec.ConfigValue<String> SHOP_CURRENCY_SYMBOL;
    
    // Kit GUI Settings
    public static final ModConfigSpec.BooleanValue KIT_SHOW_PREVIEWS;
    public static final ModConfigSpec.BooleanValue KIT_SHOW_COOLDOWNS;
    public static final ModConfigSpec.BooleanValue KIT_SHOW_COSTS;
    public static final ModConfigSpec.ConfigValue<String> KIT_AVAILABLE_COLOR;
    public static final ModConfigSpec.ConfigValue<String> KIT_COOLDOWN_COLOR;
    public static final ModConfigSpec.ConfigValue<String> KIT_LOCKED_COLOR;
    
    // Warp GUI Settings
    public static final ModConfigSpec.BooleanValue WARP_SHOW_CATEGORIES;
    public static final ModConfigSpec.BooleanValue WARP_SHOW_DESCRIPTIONS;
    public static final ModConfigSpec.BooleanValue WARP_SHOW_COSTS;
    public static final ModConfigSpec.IntValue WARP_ITEMS_PER_PAGE;
    
    // Player Stats GUI Settings
    public static final ModConfigSpec.BooleanValue STATS_SHOW_PLAYTIME;
    public static final ModConfigSpec.BooleanValue STATS_SHOW_BALANCE;
    public static final ModConfigSpec.BooleanValue STATS_SHOW_RANK;
    public static final ModConfigSpec.BooleanValue STATS_SHOW_LOCATION;
    public static final ModConfigSpec.BooleanValue STATS_SHOW_HEALTH;
    public static final ModConfigSpec.BooleanValue STATS_SHOW_EXPERIENCE;
    
    // Navigation Settings
    public static final ModConfigSpec.BooleanValue ENABLE_BACK_BUTTONS;
    public static final ModConfigSpec.BooleanValue ENABLE_PAGE_NAVIGATION;
    public static final ModConfigSpec.ConfigValue<String> BACK_BUTTON_NAME;
    public static final ModConfigSpec.ConfigValue<String> NEXT_PAGE_NAME;
    public static final ModConfigSpec.ConfigValue<String> PREVIOUS_PAGE_NAME;
    public static final ModConfigSpec.ConfigValue<String> CLOSE_BUTTON_NAME;
    
    // Animation and Effects
    public static final ModConfigSpec.BooleanValue ENABLE_ANIMATIONS;
    public static final ModConfigSpec.IntValue ANIMATION_SPEED;
    public static final ModConfigSpec.BooleanValue ENABLE_PARTICLES;
    public static final ModConfigSpec.BooleanValue ENABLE_HOVER_EFFECTS;
    
    // Advanced Settings
    public static final ModConfigSpec.IntValue AUTO_CLOSE_DELAY;
    public static final ModConfigSpec.BooleanValue PREVENT_ITEM_PICKUP;
    public static final ModConfigSpec.BooleanValue RESTORE_INVENTORY_ON_CLOSE;
    public static final ModConfigSpec.IntValue MAX_ITEMS_PER_GUI;
    
    static {
        BUILDER.comment("GUI Configuration")
               .comment("Customize the appearance and behavior of all graphical interfaces");
        
        BUILDER.push("layout");
        INVENTORY_ROWS = BUILDER
            .comment("Number of rows for GUI inventories (3-6)")
            .defineInRange("inventory_rows", 6, 3, 6);
        MENU_TITLE_FORMAT = BUILDER
            .comment("Format for GUI menu titles. Placeholders: {menu_name}, {page}, {max_page}")
            .define("menu_title_format", "&6&l{menu_name} &7(Page {page}/{max_page})");
        ENABLE_MENU_SOUNDS = BUILDER
            .comment("Enable sound effects for GUI interactions")
            .define("enable_menu_sounds", true);
        CLICK_SOUND = BUILDER
            .comment("Sound played when clicking GUI items")
            .define("click_sound", "ui.button.click");
        NAVIGATION_SOUND = BUILDER
            .comment("Sound played when navigating between pages")
            .define("navigation_sound", "item.book.page_turn");
        BUILDER.pop();
        
        BUILDER.push("colors");
        PRIMARY_COLOR = BUILDER
            .comment("Primary color for GUI elements")
            .define("primary_color", "&6");
        SECONDARY_COLOR = BUILDER
            .comment("Secondary color for GUI elements")
            .define("secondary_color", "&e");
        ACCENT_COLOR = BUILDER
            .comment("Accent color for highlighted elements")
            .define("accent_color", "&a");
        ERROR_COLOR = BUILDER
            .comment("Color for error messages and unavailable items")
            .define("error_color", "&c");
        SUCCESS_COLOR = BUILDER
            .comment("Color for success messages and confirmations")
            .define("success_color", "&a");
        BUILDER.pop();
        
        BUILDER.push("shop");
        SHOP_ENABLE_CATEGORIES = BUILDER
            .comment("Enable category-based shop organization")
            .define("shop_enable_categories", true);
        SHOP_ITEMS_PER_PAGE = BUILDER
            .comment("Number of items to display per shop page")
            .defineInRange("shop_items_per_page", 45, 9, 54);
        SHOP_SHOW_PRICES = BUILDER
            .comment("Show item prices in shop GUI")
            .define("shop_show_prices", true);
        SHOP_SHOW_DESCRIPTIONS = BUILDER
            .comment("Show item descriptions in shop GUI")
            .define("shop_show_descriptions", true);
        SHOP_CURRENCY_SYMBOL = BUILDER
            .comment("Symbol to display for currency in shop")
            .define("shop_currency_symbol", "$");
        BUILDER.pop();
        
        BUILDER.push("kits");
        KIT_SHOW_PREVIEWS = BUILDER
            .comment("Show kit item previews in GUI")
            .define("kit_show_previews", true);
        KIT_SHOW_COOLDOWNS = BUILDER
            .comment("Show kit cooldown information")
            .define("kit_show_cooldowns", true);
        KIT_SHOW_COSTS = BUILDER
            .comment("Show kit costs in GUI")
            .define("kit_show_costs", true);
        KIT_AVAILABLE_COLOR = BUILDER
            .comment("Color for available kits")
            .define("kit_available_color", "&a");
        KIT_COOLDOWN_COLOR = BUILDER
            .comment("Color for kits on cooldown")
            .define("kit_cooldown_color", "&c");
        KIT_LOCKED_COLOR = BUILDER
            .comment("Color for locked/unavailable kits")
            .define("kit_locked_color", "&8");
        BUILDER.pop();
        
        BUILDER.push("warps");
        WARP_SHOW_CATEGORIES = BUILDER
            .comment("Enable category-based warp organization")
            .define("warp_show_categories", true);
        WARP_SHOW_DESCRIPTIONS = BUILDER
            .comment("Show warp descriptions in GUI")
            .define("warp_show_descriptions", true);
        WARP_SHOW_COSTS = BUILDER
            .comment("Show warp teleportation costs")
            .define("warp_show_costs", true);
        WARP_ITEMS_PER_PAGE = BUILDER
            .comment("Number of warps to display per page")
            .defineInRange("warp_items_per_page", 45, 9, 54);
        BUILDER.pop();
        
        BUILDER.push("stats");
        STATS_SHOW_PLAYTIME = BUILDER
            .comment("Show playtime in player stats GUI")
            .define("stats_show_playtime", true);
        STATS_SHOW_BALANCE = BUILDER
            .comment("Show balance in player stats GUI")
            .define("stats_show_balance", true);
        STATS_SHOW_RANK = BUILDER
            .comment("Show rank/permissions in player stats GUI")
            .define("stats_show_rank", true);
        STATS_SHOW_LOCATION = BUILDER
            .comment("Show current location in player stats GUI")
            .define("stats_show_location", true);
        STATS_SHOW_HEALTH = BUILDER
            .comment("Show health information in player stats GUI")
            .define("stats_show_health", true);
        STATS_SHOW_EXPERIENCE = BUILDER
            .comment("Show experience information in player stats GUI")
            .define("stats_show_experience", true);
        BUILDER.pop();
        
        BUILDER.push("navigation");
        ENABLE_BACK_BUTTONS = BUILDER
            .comment("Enable back buttons in all GUIs")
            .define("enable_back_buttons", true);
        ENABLE_PAGE_NAVIGATION = BUILDER
            .comment("Enable next/previous page buttons")
            .define("enable_page_navigation", true);
        BACK_BUTTON_NAME = BUILDER
            .comment("Display name for back buttons")
            .define("back_button_name", "&c&l← Back");
        NEXT_PAGE_NAME = BUILDER
            .comment("Display name for next page buttons")
            .define("next_page_name", "&a&lNext Page →");
        PREVIOUS_PAGE_NAME = BUILDER
            .comment("Display name for previous page buttons")
            .define("previous_page_name", "&a&l← Previous Page");
        CLOSE_BUTTON_NAME = BUILDER
            .comment("Display name for close buttons")
            .define("close_button_name", "&c&lClose");
        BUILDER.pop();
        
        BUILDER.push("effects");
        ENABLE_ANIMATIONS = BUILDER
            .comment("Enable GUI animations and transitions")
            .define("enable_animations", false);
        ANIMATION_SPEED = BUILDER
            .comment("Speed of GUI animations (1-10)")
            .defineInRange("animation_speed", 3, 1, 10);
        ENABLE_PARTICLES = BUILDER
            .comment("Enable particle effects in GUIs")
            .define("enable_particles", false);
        ENABLE_HOVER_EFFECTS = BUILDER
            .comment("Enable hover effects for GUI items")
            .define("enable_hover_effects", true);
        BUILDER.pop();
        
        BUILDER.push("advanced");
        AUTO_CLOSE_DELAY = BUILDER
            .comment("Automatically close GUI after inactivity (seconds, 0 = disabled)")
            .defineInRange("auto_close_delay", 0, 0, 300);
        PREVENT_ITEM_PICKUP = BUILDER
            .comment("Prevent players from picking up items while GUI is open")
            .define("prevent_item_pickup", true);
        RESTORE_INVENTORY_ON_CLOSE = BUILDER
            .comment("Restore player inventory when GUI is closed")
            .define("restore_inventory_on_close", true);
        MAX_ITEMS_PER_GUI = BUILDER
            .comment("Maximum number of items that can be displayed in a single GUI")
            .defineInRange("max_items_per_gui", 54, 9, 54);
        BUILDER.pop();
        
        SPEC = BUILDER.build();
    }
}
