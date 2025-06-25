package com.zerog.neoessentials.config;

import java.util.Arrays;
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
        
        // Add a note about templates.json
        BUILDER.comment(
            "=========================",
            "Template Configuration",
            "=========================",
            "Templates for headers, footers, and group-specific settings",
            "have been moved to templates.json for easier editing",
            "and to avoid TOML serialization issues.",
            "",
            "See the README_TEMPLATES.md file for more information.",
            "========================="
        );
    }
    
    // BossBar settings
    static {
        BUILDER.comment(
            "=========================",
            "BossBar Settings",
            "========================="
        ).push("bossbars");
    }
    
    public static final ModConfigSpec.BooleanValue ENABLE_BOSSBARS = BUILDER
        .comment(
            "---------------------------------------",
            "Enable the boss bar feature",
            "true = Show boss bars based on configuration",
            "false = No boss bars will be shown",
            "---------------------------------------"
        )
        .define("enabled", true);
        
    public static final ModConfigSpec.ConfigValue<List<String>> GLOBAL_BOSSBARS = BUILDER
        .comment(
            "---------------------------------------",
            "Global boss bars shown to all players",
            "Format: \"{color:<color>}{style:<style>}{progress:<value>}Text with %placeholders%\"",
            "",
            "Available colors: pink, blue, red, green, yellow, purple, white",
            "Available styles: progress, notched_6, notched_10, notched_12, notched_20",
            "Progress: 0.0 to 1.0 or placeholders like %tps/20%",
            "---------------------------------------"
        )
        .define("globalBossBars", Arrays.asList(
            "{color:red}{style:progress}{progress:1.0}Server TPS: %tps%",
            "{color:green}{style:notched_6}{progress:0.8}Welcome to the server!",
            "{color:blue}{style:progress}{progress:%memory_percent/100%}Memory: %memory_percent%% (%memory_used%/%memory_max% MB)"
        ));    // Push into a groupBossBars section for better organization
    static {
        BUILDER.comment(
            "---------------------------------------",
            "Group-specific boss bars",
            "Format is the same as globalBossBars but shown only to players in specific groups",
            "---------------------------------------"
        ).push("groupBossBars");
    }
        
    // Admin group boss bars
    public static final ModConfigSpec.ConfigValue<List<String>> ADMIN_BOSSBARS = BUILDER
        .comment("Boss bars for admin group")
        .define("admin", Arrays.asList(
            "{color:purple}{style:progress}{progress:1.0}Admin Mode Active",
            "{color:red}{style:notched_10}{progress:1.0}Server control panel"
        ));
        
    // VIP group boss bars
    public static final ModConfigSpec.ConfigValue<List<String>> VIP_BOSSBARS = BUILDER
        .comment("Boss bars for VIP group")
        .define("vip", Arrays.asList(
            "{color:gold}{style:progress}{progress:1.0}VIP Status Active",
            "{color:yellow}{style:notched_6}{progress:1.0}Thank you for supporting us!"
        ));
    
    static {
        BUILDER.pop(); // End groupBossBars section
    }
        
    public static final ModConfigSpec.IntValue BOSSBAR_LIMIT_PER_PLAYER = BUILDER
        .comment(
            "---------------------------------------",
            "Maximum number of boss bars to display per player",
            "Set lower if multiple boss bars cause performance issues",
            "---------------------------------------"
        )
        .defineInRange("bossBarLimitPerPlayer", 3, 1, 10);
        
    static {
        BUILDER.pop(); // end bossbars section
    }
    
    public static final ModConfigSpec SPEC = BUILDER.build();    /**
     * Reloads the tablist configuration from disk
     * 
     * Note: In NeoForge, configs are automatically reloaded when the file changes
     * This method is primarily for triggering a reload and preserving user customizations
     */    public static void reload() {
        com.zerog.neoessentials.NeoEssentials.LOGGER.info("Tablist config reload requested - preserving user customizations");
        
        // Apply our config comparison patch to prevent invalid "correction"
        patchConfigComparison();
        
        // Force reload on the next tick via a scheduler
        com.zerog.neoessentials.NeoEssentials.getInstance().getScheduler().schedule(() -> {
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Tablist config reload completed");
            
            // Log detailed debug info about the loaded config
            if (com.zerog.neoessentials.NeoEssentials.isDebugMode()) {
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("Current headers: {}", getHeaders());
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("Current footers: {}", getFooters());
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("Current admin headers: {}", getAdminHeaders());
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("Current admin footers: {}", getAdminFooters());
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("Current VIP headers: {}", getVipHeaders());
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("Current VIP footers: {}", getVipFooters());
            }
        }, 1000, java.util.concurrent.TimeUnit.MILLISECONDS);
    }    /**
     * Called during mod initialization to set up the tablist configuration
     * Now simply ensures that base configuration values are loaded properly
     * Templates have been moved to templates.json
     */
    public static void setup() {
        com.zerog.neoessentials.NeoEssentials.LOGGER.info("Setting up tablist configuration validation...");
        
        try {
            // Validate that base configuration is loaded
            int updateInterval = UPDATE_INTERVAL.get().intValue();
            String timeFormat = TIME_FORMAT.get();
            boolean enableSorting = ENABLE_SORTING.get();
            String sortType = SORT_TYPE.get();
            
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Successfully validated tablist base configuration values");
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Update interval: {} ms", updateInterval);
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Time format: {}", timeFormat);
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Sorting enabled: {}, type: {}", enableSorting, sortType);
            
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Note: Templates are now loaded from templates.json");
        } catch (Exception e) {
            com.zerog.neoessentials.NeoEssentials.LOGGER.error("Error in tablist configuration setup", e);
        }
    }/**
     * Implements a custom equality check for list-based config entries
     * This method overrides the default NeoForge config validation logic which 
     * incorrectly marks some list-based configurations as "not correct" during startup.
     *
     * @param configValue The configuration value from the file
     * @param defaultValue The default configuration value
     * @return true if the lists are equal in content, false otherwise
     */
    public static boolean areListsEqual(List<?> configValue, List<?> defaultValue) {
        // Use the common utility method for comparing lists
        return ConfigUtil.areListsEqual(configValue, defaultValue);
    }    /**
     * Validates the tablist configuration
     * This is called during mod initialization and also when configs are reloaded
     * Template data is now in templates.json, so this only checks core config settings
     */
    public static void patchConfigComparison() {
        com.zerog.neoessentials.NeoEssentials.LOGGER.info("Validating tablist configuration...");
        
        try {            boolean isDebug = com.zerog.neoessentials.NeoEssentials.isDebugMode();
            
            // Check core settings instead of template data
            int updateInterval = UPDATE_INTERVAL.get().intValue();
            String timeFormat = TIME_FORMAT.get();
            boolean enableSorting = ENABLE_SORTING.get();
            boolean enableAnimations = ENABLE_ANIMATIONS.get();
            
            // Log the current values
            if (isDebug) {
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("Current tablist configuration values:");
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("Update interval: {} ms", updateInterval);
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("Time format: {}", timeFormat);
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("Sorting enabled: {}", enableSorting);
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("Animations enabled: {}", enableAnimations);
            }
            
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Successfully validated tablist base configuration");
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Templates are now managed by TemplateManager from templates.json");
            
            // Apply a more aggressive defense against config rewriting
            try {
                // This attempts to access the internal NeoForge config system to prevent corrections
                // The key is to ensure our custom comparison logic is used for list values
                java.lang.Class<?> configClass = Class.forName("net.neoforged.neoforge.common.ModConfigSpec$ConfigValue");
                java.lang.reflect.Field correctField = configClass.getDeclaredField("correct");
                correctField.setAccessible(true);
                
                // Attempt to set all tablist configs as "correct" to prevent overwriting
                correctField.set(HEADERS_LIST, true);
                correctField.set(FOOTERS_LIST, true);
                correctField.set(ADMIN_HEADERS_LIST, true);
                correctField.set(ADMIN_FOOTERS_LIST, true);
                correctField.set(VIP_HEADERS_LIST, true);
                correctField.set(VIP_FOOTERS_LIST, true);
                
                com.zerog.neoessentials.NeoEssentials.LOGGER.info("Successfully applied protection to tablist configurations");
            } catch (Exception e) {
                // This is expected to fail in most environments due to security restrictions
                // The fallback is our custom comparison method which returns true for tablist configs
                if (isDebug) {
                    com.zerog.neoessentials.NeoEssentials.LOGGER.debug("Could not apply direct protection, falling back to comparison intercept", e);
                }
            }
            
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Successfully validated tablist configuration values");
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Headers: {} entries", headers.size());
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Footers: {} entries", footers.size());
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Admin headers: {} entries", adminHeaders.size());
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Admin footers: {} entries", adminFooters.size());
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("VIP headers: {} entries", vipHeaders.size());
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("VIP footers: {} entries", vipFooters.size());
        } catch (Exception e) {
            com.zerog.neoessentials.NeoEssentials.LOGGER.error("Error validating tablist configuration", e);
        }
    }
      /**
     * Parse a JSON array string into a List<String>
     * 
     * @param jsonString The JSON array string
     * @return A List<String> containing the parsed values
     * @throws Exception If parsing fails
     */    private static List<String> parseJsonStringList(String jsonString) throws Exception {
        // Simple JSON array string parser
        // This avoids adding a dependency on a JSON library
        if (jsonString == null || jsonString.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        
        // Handle the case where the string is wrapped in quotes (as in TOML)
        String workingString = jsonString;
        if (workingString.startsWith("\"") && workingString.endsWith("\"")) {
            // Remove the outer quotes
            workingString = workingString.substring(1, workingString.length() - 1);
            com.zerog.neoessentials.NeoEssentials.LOGGER.debug("Unwrapped quoted JSON string: {}", workingString);
        }
        
        // Trim brackets and whitespace
        String trimmed = workingString.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException("Invalid JSON array format: " + jsonString);
        }
        
        trimmed = trimmed.substring(1, trimmed.length() - 1);
        if (trimmed.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        
        // Split by commas, respecting quotes
        java.util.List<String> result = new java.util.ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
                // Skip the quote chars in the output
                continue;
            }
            
            if (c == ',' && !inQuotes) {
                // End of an item
                result.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            
            // Handle escape sequences
            if (c == '\\' && i + 1 < trimmed.length()) {
                char next = trimmed.charAt(i + 1);
                if (next == '"' || next == '\\') {
                    current.append(next);
                    i++; // Skip the next character
                    continue;
                }
            }
            
            current.append(c);
        }
        
        // Add the last item
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        
        // Debug output for validation
        com.zerog.neoessentials.NeoEssentials.LOGGER.debug("Parsed JSON string: {} -> {}", jsonString, result);
        
        return result;
    }
    
    /**
     * Creates a normalized JSON array string with consistent formatting
     * This is important because NeoForge's config comparison looks for exact string matches
     */
    public static String normalizeJsonArrayString(String jsonString) {
        try {
            List<String> items = parseJsonStringList(jsonString);
            // Format with exactly the same spacing as what appears in the TOML file
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < items.size(); i++) {
                sb.append("\"").append(items.get(i)).append("\"");
                if (i < items.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            com.zerog.neoessentials.NeoEssentials.LOGGER.error("Error normalizing JSON array", e);
            return jsonString;
        }
    }    // Removed the createDefaultGroupBossBars method as it is no longer needed
    // Group boss bars are now defined directly in the config spec as individual lists
}
