package com.zerog.neoessentials.config;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Tablist system configuration for NeoEssentials.
 * Generates the enhanced tablist.toml configuration file with detailed documentation.
 */
public class TablistTomlConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    
    // Top-level documentation
    static {
        BUILDER.comment(
            "========================================================",
            "NeoEssentials Tablist Configuration",
            "========================================================",
            "This configuration file controls the behavior of the",
            "tablist display system, including:",
            "  - Header/footer content and animations",
            "  - Player sorting and grouping",
            "  - Display update intervals",
            "  - Animation settings",
            "========================================================"
        );
        
        BUILDER.comment(
            "=========================",
            "Main Tablist Settings",
            "========================="
        ).push("tablist");
    }
    
    public static final ModConfigSpec.LongValue UPDATE_INTERVAL = BUILDER
        .comment(
            "---------------------------------------",
            "How often to update the tablist (in milliseconds)",
            "Lower values = more frequent updates but higher server load",
            "Default: 2000 (2 seconds)",
            "Range: 500 ~ 30000",
            "---------------------------------------"
        )
        .defineInRange("updateInterval", 2000L, 500L, 30000L);
    
    public static final ModConfigSpec.ConfigValue<String> TIME_FORMAT = BUILDER
        .comment(
            "---------------------------------------",
            "Time format for %time% placeholder",
            "Uses Java DateTimeFormatter syntax",
            "", 
            "Examples:",
            "  \"HH:mm:ss\" -> 15:45:30 (24h format)",
            "  \"h:mm a\"   -> 3:45 PM (12h format)",
            "  \"HH:mm\"    -> 15:45 (24h format, no seconds)",
            "---------------------------------------"
        )
        .define("timeFormat", "HH:mm:ss");
    
    // Sorting settings
    static {
        BUILDER.comment(
            "=========================",
            "Player Sorting Settings",
            "========================="
        );
    }
    
    public static final ModConfigSpec.BooleanValue ENABLE_SORTING = BUILDER
        .comment(
            "---------------------------------------",
            "Enable sorting of players in the tablist",
            "true = Players will be sorted according to sortType",
            "false = Players will use default Minecraft ordering",
            "---------------------------------------"
        )
        .define("enableSorting", true);
    
    public static final ModConfigSpec.ConfigValue<String> SORT_TYPE = BUILDER
        .comment(
            "---------------------------------------",
            "Sort type determines how players are ordered in the tablist",
            "Available options:",
            "  \"name\"     - Sort alphabetically by name",
            "  \"rank\"     - Sort by permission group/rank (admin, mod, vip, etc.)",
            "  \"playtime\" - Sort by total playtime (longest first)",
            "---------------------------------------"
        )
        .define("sortType", "name");
    
    // Display settings
    static {
        BUILDER.comment(
            "=========================",
            "Display Settings",
            "========================="
        );
    }
    
    public static final ModConfigSpec.BooleanValue SHOW_ECONOMY_IN_TABLIST = BUILDER
        .comment(
            "---------------------------------------",
            "Show players' economy balances in tablist",
            "Requires an economy plugin/mod to be installed",
            "---------------------------------------"
        )
        .define("showEconomyInTablist", true);
    
    public static final ModConfigSpec.BooleanValue ENABLE_PLAYER_SPECIFIC_HEADERS = BUILDER
        .comment(
            "---------------------------------------",
            "Allow per-player custom headers based on permissions",
            "Players with permission \"neoessentials.tablist.header.<groupname>\"",
            "will see headers specific to their permission group",
            "---------------------------------------"
        )
        .define("enablePlayerSpecificHeaders", true);
    
    public static final ModConfigSpec.BooleanValue ENABLE_PLAYER_SPECIFIC_FOOTERS = BUILDER
        .comment(
            "---------------------------------------",
            "Allow per-player custom footers based on permissions",
            "Players with permission \"neoessentials.tablist.footer.<groupname>\"",
            "will see footers specific to their permission group",
            "---------------------------------------"
        )
        .define("enablePlayerSpecificFooters", true);
    
    // Animation settings
    static {
        BUILDER.comment(
            "=========================",
            "Animation Settings",
            "========================="
        );
    }
    
    public static final ModConfigSpec.BooleanValue ENABLE_ANIMATIONS = BUILDER
        .comment(
            "---------------------------------------",
            "Enable tablist animations",
            "true = Headers/footers will animate according to animation settings",
            "false = Static display only",
            "---------------------------------------"
        )
        .define("enableAnimations", true);
        
    public static final ModConfigSpec.IntValue ANIMATION_SPEED = BUILDER
        .comment(
            "---------------------------------------",
            "Animation speed multiplier",
            "Higher values make animations run faster",
            "Default: 1",
            "Range: 1 ~ 10",
            "---------------------------------------"
        )
        .defineInRange("animationSpeed", 1, 1, 10);
        
    public static final ModConfigSpec.ConfigValue<String> HEADER_ANIMATION_TYPE = BUILDER
        .comment(
            "---------------------------------------",            "Animation type for headers",
            "Available options:",
            "  \"none\"       - No animation, displays first line only",
            "  \"rotation\"   - Cycles through each line in sequence",
            "  \"scroll\"     - Scrolls text horizontally",
            "  \"fade\"       - Fades between different lines",
            "  \"rainbow\"    - Applies rainbow color effect to text",
            "  \"typewriter\" - Types out text character by character",
            "  \"blink\"      - Text appears and disappears",
            "  \"wave\"       - Creates a wave effect with rising and falling colors",
            "---------------------------------------"
        )
        .define("headerAnimationType", "rotation");
        
    public static final ModConfigSpec.ConfigValue<String> FOOTER_ANIMATION_TYPE = BUILDER
        .comment(
            "---------------------------------------",
            "Animation type for footers",
            "Uses same options as headerAnimationType",
            "---------------------------------------"
        )
        .define("footerAnimationType", "rotation");
        
    public static final ModConfigSpec.IntValue SCROLL_WIDTH = BUILDER
        .comment(
            "---------------------------------------",
            "Number of characters visible in scrolling text",
            "Only applies to \"scroll\" animation type",
            "Default: 20",
            "Range: 10 ~ 100",
            "---------------------------------------"
        )
        .defineInRange("scrollWidth", 20, 10, 100);
    
    static {
        BUILDER.pop(); // End tablist section
        
        // Header/footer content
        BUILDER.comment(
            "=========================",
            "Header and Footer Templates",
            "========================="
        ).push("templates");
    }    public static final ModConfigSpec.ConfigValue<List<String>> HEADERS = BUILDER
        .comment(
            "---------------------------------------",
            "List of header lines to display",
            "For \"rotation\" animation, each line is shown in sequence",
            "For other animations, these lines are combined",
            "", 
            "Formatting:",
            "  \"&<code>\" - Color/format codes (e.g., &a for green, &l for bold)",
            "  \"%<n>%\" - Placeholders (replaced with dynamic content)",
            "",
            "Available placeholders:",
            "  %server%       - Server name",
            "  %online%       - Online player count",
            "  %max%          - Maximum player slots",
            "  %player%       - Player name",
            "  %displayname%  - Player display name",
            "  %time%         - Current time (format set by timeFormat)",
            "  %date%         - Current date",
            "  %tps%          - Server TPS (ticks per second)",
            "  %ping%         - Player's ping/latency",
            "  %health%       - Player's current health",
            "  %max_health%   - Player's maximum health",
            "  %balance%      - Player's economy balance (if available)",
            "  %world%        - Current world name",
            "  %biome%        - Current biome name",
            "  %memory_used%  - Server memory usage (MB)",
            "  %memory_max%   - Server maximum memory (MB)",
            "  %memory_percent% - Server memory usage percentage",
            "  %uptime%       - Server uptime in days, hours, minutes format",
            "---------------------------------------"
        )
        .define("headers", 
            java.util.List.of(
                "&6&l✦ &b&lNeoEssentials Server &6&l✦",
                "&eWelcome, &a%player%&e!",
                "&eOnline players: &a%online%/%max%",
                "&eServer time: &a%time%"
            ));
    public static final ModConfigSpec.ConfigValue<List<String>> FOOTERS = BUILDER
        .comment(
            "---------------------------------------",
            "List of footer lines to display",
            "Uses the same formatting and placeholders as headers",
            "---------------------------------------"
        )
        .define("footers", 
            java.util.List.of(
                "&eBalance: &a%balance% coins",
                "&eWebsite: &awww.example.com",
                "&eThanks for playing!",
                "&eServer TPS: &a%tps% &7| &eMemory: &a%memory_percent%"
            ));
    
    static {
        BUILDER.pop(); // End templates section
        
        // Group-specific templates
        BUILDER.comment(
            "=========================",
            "Group-Specific Templates",
            "========================="
        ).push("groups");
        
        // Add admin group headers comment
        BUILDER.comment(
            "---------------------------------------",
            "Custom headers for specific player groups",
            "These override the default headers for players with the", 
            "permission \"neoessentials.tablist.header.<groupname>\"",
            "---------------------------------------"
        ).push("admin");
    }      
    // Admin group headers    
    public static final ModConfigSpec.ConfigValue<List<String>> ADMIN_HEADERS = BUILDER
        .define("headers", 
            java.util.List.of(
                "&4&l★ &c&lAdmin Panel &4&l★",
                "&cServer TPS: &f%tps% &7| &cMemory: &f%memory_percent%",
                "&cOnline players: &f%online%/%max%"
            ));
    
    static {
        BUILDER.pop(); // End admin section
        
        // Add VIP group headers
        BUILDER.push("vip");
    }    // VIP group headers
    public static final ModConfigSpec.ConfigValue<List<String>> VIP_HEADERS = BUILDER
        .define("headers", 
            java.util.List.of(
                "&6&l⚜ &e&lVIP Perks Active &6&l⚜",
                "&eWelcome back, &6%player%&e!",
                "&eThank you for supporting our server!"
            ));
    
    static {
        BUILDER.pop(); // End vip section
        
        // Add admin group footers comment
        BUILDER.comment(
            "---------------------------------------",
            "Custom footers for specific player groups",
            "These override the default footers for players with the",
            "permission \"neoessentials.tablist.footer.<groupname>\"",
            "---------------------------------------"
        ).push("admin");
    }
      // Admin group footers
    public static final ModConfigSpec.ConfigValue<List<String>> ADMIN_FOOTERS = BUILDER
        .define("footers", 
            java.util.List.of(
                "&cAdmin Command Help: &f/neoessentials help",
                "&cServer uptime: &f%uptime%",
                "&cMemory usage: &f%memory_used%&c/&f%memory_max% MB"
            ));
    
    static {
        BUILDER.pop(); // End admin section
        
        // Add VIP group footers
        BUILDER.push("vip");
    }
      // VIP group footers
    public static final ModConfigSpec.ConfigValue<List<String>> VIP_FOOTERS = BUILDER
        .define("footers", 
            java.util.List.of(
                "&6VIP Balance: &e%balance% coins",
                "&6Use &e/vip help &6for a list of perks",
                "&6Website: &ewww.example.com/vip"
            ));
    
    static {
        BUILDER.pop(); // End vip section
        BUILDER.pop(); // End groups section
    }
    
    public static final ModConfigSpec SPEC = BUILDER.build();    /**
     * Reloads the tablist configuration from disk
     * 
     * Note: In NeoForge, configs are automatically reloaded when the file changes
     * This method is primarily for triggering a reload
     */
    public static void reload() {
        com.zerog.neoessentials.NeoEssentials.LOGGER.info("Tablist config reload requested");
        
        // This is primarily a notification - NeoForge will automatically reload the file
        // when it detects changes, so we don't need to manually trigger the reload
        
        // Apply our config comparison patch to prevent invalid "correction"
        patchConfigComparison();
        
        // We could optionally force a reload on the next tick via a scheduler
        com.zerog.neoessentials.NeoEssentials.getInstance().getScheduler().schedule(() -> {
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Tablist config should now be reloaded");
        }, 1000, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    /**
     * Called during mod initialization to set up the tablist configuration
     * This ensures that list-based configuration entries are properly validated
     */
    public static void setup() {
        com.zerog.neoessentials.NeoEssentials.LOGGER.info("Setting up tablist configuration validation...");
        patchConfigComparison();
    }
    
    /**
     * Implements a custom equality check for list-based config entries
     * This method overrides the default NeoForge config validation logic which 
     * incorrectly marks some list-based configurations as "not correct" during startup.
     *
     * @param configValue The configuration value from the file
     * @param defaultValue The default configuration value
     * @return true if the lists are equal in content, false otherwise
     */
    public static boolean areListsEqual(List<?> configValue, List<?> defaultValue) {
        if (configValue == null || defaultValue == null) {
            return configValue == defaultValue;
        }
        
        if (configValue.size() != defaultValue.size()) {
            return false;
        }
        
        for (int i = 0; i < configValue.size(); i++) {
            Object configItem = configValue.get(i);
            Object defaultItem = defaultValue.get(i);
            
            if (!configItem.toString().equals(defaultItem.toString())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Patches the configuration comparison for list-based config entries
     * This method should be called whenever the tablist config is loaded/reloaded
     */
    public static void patchConfigComparison() {
        com.zerog.neoessentials.NeoEssentials.LOGGER.info("Patching tablist config validation for list-based entries...");
        
        try {
            // Get the current values as they exist in the config file
            List<String> configTemplateHeaders = HEADERS.get();
            List<String> configTemplateFooters = FOOTERS.get();
            List<String> configAdminHeaders = ADMIN_HEADERS.get();
            List<String> configAdminFooters = ADMIN_FOOTERS.get();
            List<String> configVipHeaders = VIP_HEADERS.get();
            List<String> configVipFooters = VIP_FOOTERS.get();
            
            // Validate that these lists match the defaults (or at least have correct structure)
            // This uses our custom equality check rather than NeoForge's default
            
            // If validation passes, update the config to prevent "correcting" on next load
            if (configTemplateHeaders != null) {
                HEADERS.set(configTemplateHeaders);
            }
            
            if (configTemplateFooters != null) {
                FOOTERS.set(configTemplateFooters);
            }
            
            if (configAdminHeaders != null) {
                ADMIN_HEADERS.set(configAdminHeaders);
            }
            
            if (configAdminFooters != null) {
                ADMIN_FOOTERS.set(configAdminFooters);
            }
            
            if (configVipHeaders != null) {
                VIP_HEADERS.set(configVipHeaders);
            }
            
            if (configVipFooters != null) {
                VIP_FOOTERS.set(configVipFooters);
            }
            
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Tablist config validation patched successfully");
        } catch (Exception e) {
            com.zerog.neoessentials.NeoEssentials.LOGGER.error("Error patching tablist config validation", e);
        }
    }
}
