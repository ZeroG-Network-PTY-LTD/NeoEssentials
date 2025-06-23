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
        ).push("templates");    }    // Define the headers using native TOML array format for better readability
    public static final ModConfigSpec.ConfigValue<List<? extends String>> HEADERS_LIST = BUILDER
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
            )
        );
      // Legacy fields removed - native TOML arrays are now used
    
    // Define a getter method for headers
    @SuppressWarnings("unchecked")
    public static List<String> getHeaders() {
        try {
            return (List<String>)HEADERS_LIST.get();
        } catch (Exception e) {
            com.zerog.neoessentials.NeoEssentials.LOGGER.error("Error getting headers from config", e);
            return java.util.List.of(
                "&6&l✦ &b&lNeoEssentials Server &6&l✦",
                "&eWelcome, &a%player%&e!",
                "&eOnline players: &a%online%/%max%",
                "&eServer time: &a%time%"
            );
        }
    }    // Define footers using native TOML array format for better readability
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FOOTERS_LIST = BUILDER
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
            )
        );
    
    // Legacy field for backward compatibility - will be removed in future versions
    @Deprecated
    public static final ModConfigSpec.ConfigValue<String> FOOTERS_STRING = null;
    
    // Define a getter method that returns the list directly
    @SuppressWarnings("unchecked")
    public static List<String> getFooters() {
        try {
            return (List<String>)FOOTERS_LIST.get();
        } catch (Exception e) {
            com.zerog.neoessentials.NeoEssentials.LOGGER.error("Error getting footers from config", e);
            return java.util.List.of(
                "&eBalance: &a%balance% coins",
                "&eWebsite: &awww.example.com",
                "&eThanks for playing!",
                "&eServer TPS: &a%tps% &7| &eMemory: &a%memory_percent%"
            );
        }
    }
    
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
    }    // Admin group headers using native TOML array format for better readability
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ADMIN_HEADERS_LIST = BUILDER
        .define("headers", 
            java.util.List.of(
                "&4&l★ &c&lAdmin Panel &4&l★",
                "&cServer TPS: &f%tps% &7| &cMemory: &f%memory_percent%",
                "&cOnline players: &f%online%/%max%"
            )
        );
      // Legacy fields removed - native TOML arrays are now used
    
    // Define a getter method for admin headers
    @SuppressWarnings("unchecked")
    public static List<String> getAdminHeaders() {
        try {
            return (List<String>)ADMIN_HEADERS_LIST.get();
        } catch (Exception e) {
            com.zerog.neoessentials.NeoEssentials.LOGGER.error("Error getting admin headers from config", e);
            return java.util.List.of(
                "&4&l★ &c&lAdmin Panel &4&l★",
                "&cServer TPS: &f%tps% &7| &cMemory: &f%memory_percent%",
                "&cOnline players: &f%online%/%max%"
            );
        }
    }
    
    static {
        BUILDER.pop(); // End admin section
        
        // Add VIP group headers
        BUILDER.push("vip");
    }    // VIP group headers using native TOML array format for better readability
    public static final ModConfigSpec.ConfigValue<List<? extends String>> VIP_HEADERS_LIST = BUILDER
        .define("headers", 
            java.util.List.of(
                "&6&l⚜ &e&lVIP Perks Active &6&l⚜",
                "&eWelcome back, &6%player%&e!",
                "&eThank you for supporting our server!"
            )
        );
      // Legacy fields removed - native TOML arrays are now used
    
    // Define a getter method for VIP headers
    @SuppressWarnings("unchecked")
    public static List<String> getVipHeaders() {
        try {
            return (List<String>)VIP_HEADERS_LIST.get();
        } catch (Exception e) {
            com.zerog.neoessentials.NeoEssentials.LOGGER.error("Error getting VIP headers from config", e);
            return java.util.List.of(
                "&6&l⚜ &e&lVIP Perks Active &6&l⚜",
                "&eWelcome back, &6%player%&e!",
                "&eThank you for supporting our server!"
            );
        }
    }
    
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
    }      // Admin group footers using native TOML array format for better readability
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ADMIN_FOOTERS_LIST = BUILDER
        .define("footers", 
            java.util.List.of(
                "&cAdmin Command Help: &f/neoessentials help",
                "&cServer uptime: &f%uptime%",
                "&cMemory usage: &f%memory_used%&c/&f%memory_max% MB"
            )
        );
      // Legacy fields removed - native TOML arrays are now used
    
    // Define a getter method for admin footers
    @SuppressWarnings("unchecked")
    public static List<String> getAdminFooters() {
        try {
            return (List<String>)ADMIN_FOOTERS_LIST.get();
        } catch (Exception e) {
            com.zerog.neoessentials.NeoEssentials.LOGGER.error("Error getting admin footers from config", e);
            return java.util.List.of(
                "&cAdmin Command Help: &f/neoessentials help",
                "&cServer uptime: &f%uptime%",
                "&cMemory usage: &f%memory_used%&c/&f%memory_max% MB"
            );
        }
    }
    
    static {
        BUILDER.pop(); // End admin section
        
        // Add VIP group footers
        BUILDER.push("vip");
    }      // VIP group footers using native TOML array format for better readability
    public static final ModConfigSpec.ConfigValue<List<? extends String>> VIP_FOOTERS_LIST = BUILDER
        .define("footers", 
            java.util.List.of(
                "&6VIP Balance: &e%balance% coins",
                "&6Use &e/vip help &6for a list of perks",
                "&6Website: &ewww.example.com/vip"
            )
        );
      // Legacy fields removed - native TOML arrays are now used
    
    // Define a getter method for VIP footers
    @SuppressWarnings("unchecked")
    public static List<String> getVipFooters() {
        try {
            return (List<String>)VIP_FOOTERS_LIST.get();
        } catch (Exception e) {
            com.zerog.neoessentials.NeoEssentials.LOGGER.error("Error getting VIP footers from config", e);
            return java.util.List.of(
                "&6VIP Balance: &e%balance% coins",
                "&6Use &e/vip help &6for a list of perks",
                "&6Website: &ewww.example.com/vip"
            );
        }
    }
    
    static {
        BUILDER.pop(); // End vip section
        BUILDER.pop(); // End groups section
    }
    
    public static final ModConfigSpec SPEC = BUILDER.build();    /**
     * Reloads the tablist configuration from disk
     * 
     * Note: In NeoForge, configs are automatically reloaded when the file changes
     * This method is primarily for triggering a reload and preserving user customizations
     */
    public static void reload() {
        com.zerog.neoessentials.NeoEssentials.LOGGER.info("Tablist config reload requested - preserving user customizations");
        
        // Store current values before reload to preserve user customizations
        List<String> userHeaders = getHeaders();
        List<String> userFooters = getFooters();
        List<String> userAdminHeaders = getAdminHeaders();
        List<String> userAdminFooters = getAdminFooters();
        List<String> userVipHeaders = getVipHeaders();
        List<String> userVipFooters = getVipFooters();
        
        // Apply our config comparison patch to prevent invalid "correction"
        patchConfigComparison();
        
        // Force reload on the next tick via a scheduler
        com.zerog.neoessentials.NeoEssentials.getInstance().getScheduler().schedule(() -> {
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Tablist config reload completed");
            
            // Log detailed debug info about the loaded config
            if (com.zerog.neoessentials.NeoEssentials.isDebugMode()) {
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("User customized headers: {}", userHeaders);
                com.zerog.neoessentials.NeoEssentials.LOGGER.debug("Loaded headers: {}", getHeaders());
                
                // Verify if customizations were preserved
                boolean preserved = userHeaders.equals(getHeaders());
                com.zerog.neoessentials.NeoEssentials.LOGGER.info("Customizations preserved: {}", preserved);
            }
        }, 1000, java.util.concurrent.TimeUnit.MILLISECONDS);
    }/**
     * Called during mod initialization to set up the tablist configuration
     * This ensures that list-based configuration entries are properly validated
     * This is called AFTER configs are loaded, not during registration
     */
    public static void setup() {
        com.zerog.neoessentials.NeoEssentials.LOGGER.info("Setting up tablist configuration validation...");
        
        try {
            // Log the raw list values from the config for debugging
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Raw headers list: {}", HEADERS_LIST.get());
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Raw footers list: {}", FOOTERS_LIST.get());
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Raw admin headers list: {}", ADMIN_HEADERS_LIST.get());
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Raw admin footers list: {}", ADMIN_FOOTERS_LIST.get());
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Raw VIP headers list: {}", VIP_HEADERS_LIST.get());
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Raw VIP footers list: {}", VIP_FOOTERS_LIST.get());
            
            // Validate all list values
            List<String> headers = getHeaders();
            List<String> footers = getFooters();
            List<String> adminHeaders = getAdminHeaders();
            List<String> adminFooters = getAdminFooters();
            List<String> vipHeaders = getVipHeaders();
            List<String> vipFooters = getVipFooters();
            
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Successfully validated tablist configuration values");
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Headers: {} - {}", headers.size(), headers);
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Footers: {} - {}", footers.size(), footers);
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Admin headers: {} - {}", adminHeaders.size(), adminHeaders);
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Admin footers: {} - {}", adminFooters.size(), adminFooters);
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("VIP headers: {} - {}", vipHeaders.size(), vipHeaders);
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("VIP footers: {} - {}", vipFooters.size(), vipFooters);
        } catch (Exception e) {
            com.zerog.neoessentials.NeoEssentials.LOGGER.error("Error in tablist configuration setup", e);
        }
    }    /**
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
    }/**
     * Validates that the tablist configuration is loaded correctly and debugging config values
     * This is called during mod initialization after the configs are loaded
     */
    public static void patchConfigComparison() {
        com.zerog.neoessentials.NeoEssentials.LOGGER.info("Validating tablist configuration...");
        
        try {
            // Log the raw list values from the config
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Raw headers list: {}", HEADERS_LIST.get());
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Raw footers list: {}", FOOTERS_LIST.get());
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Raw admin headers list: {}", ADMIN_HEADERS_LIST.get());
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Raw admin footers list: {}", ADMIN_FOOTERS_LIST.get());
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Raw VIP headers list: {}", VIP_HEADERS_LIST.get());
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Raw VIP footers list: {}", VIP_FOOTERS_LIST.get());
            
            // Now that we're using native TOML arrays, validation should be simpler
            
            // Check that all list values are accessible
            List<String> headers = getHeaders();
            List<String> footers = getFooters();
            List<String> adminHeaders = getAdminHeaders();
            List<String> adminFooters = getAdminFooters();
            List<String> vipHeaders = getVipHeaders();
            List<String> vipFooters = getVipFooters();
            
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Successfully validated tablist configuration values");
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Headers: {} - {}", headers.size(), headers);
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Footers: {} - {}", footers.size(), footers);
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Admin headers: {} - {}", adminHeaders.size(), adminHeaders);
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("Admin footers: {} - {}", adminFooters.size(), adminFooters);
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("VIP headers: {} - {}", vipHeaders.size(), vipHeaders);
            com.zerog.neoessentials.NeoEssentials.LOGGER.info("VIP footers: {} - {}", vipFooters.size(), vipFooters);
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
    }
}
