package com.zerog.neoessentials.config;

import com.zerog.neoessentials.NeoEssentials;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

/**
 * Tablist system YAML configuration for NeoEssentials.
 * Replaces the old TOML configuration with a more flexible YAML format.
 */
public class TablistYamlConfig {
    
    // Configuration file paths
    private static final Path CONFIG_DIR = Paths.get("config", "neoessentials");
    private static final Path YAML_CONFIG_PATH = CONFIG_DIR.resolve("tablist.yml");
    private static final Path OLD_TOML_CONFIG_PATH = CONFIG_DIR.resolve("tablist.toml");
    
    // Default values
    private static final long DEFAULT_UPDATE_INTERVAL = 2000L;
    private static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";
    private static final boolean DEFAULT_ENABLE_SORTING = true;
    private static final String DEFAULT_SORT_TYPE = "name";
    private static final boolean DEFAULT_SHOW_ECONOMY = true;
    private static final boolean DEFAULT_ENABLE_PLAYER_HEADERS = true;
    private static final boolean DEFAULT_ENABLE_PLAYER_FOOTERS = true;
    private static final boolean DEFAULT_ENABLE_ANIMATIONS = true;
    private static final int DEFAULT_ANIMATION_SPEED = 1;
    private static final String DEFAULT_HEADER_ANIMATION = "rotation";
    private static final String DEFAULT_FOOTER_ANIMATION = "rotation";
    private static final int DEFAULT_SCROLL_WIDTH = 20;
    private static final boolean DEFAULT_ENABLE_BOSSBARS = true;
    private static final int DEFAULT_BOSSBAR_LIMIT = 3;
    
    // Configuration values
    private static long updateInterval = DEFAULT_UPDATE_INTERVAL;
    private static String timeFormat = DEFAULT_TIME_FORMAT;
    private static boolean enableSorting = DEFAULT_ENABLE_SORTING;
    private static String sortType = DEFAULT_SORT_TYPE;
    private static boolean showEconomyInTablist = DEFAULT_SHOW_ECONOMY;
    private static boolean enablePlayerSpecificHeaders = DEFAULT_ENABLE_PLAYER_HEADERS;
    private static boolean enablePlayerSpecificFooters = DEFAULT_ENABLE_PLAYER_FOOTERS;
    private static boolean enableAnimations = DEFAULT_ENABLE_ANIMATIONS;
    private static int animationSpeed = DEFAULT_ANIMATION_SPEED;
    private static String headerAnimationType = DEFAULT_HEADER_ANIMATION;
    private static String footerAnimationType = DEFAULT_FOOTER_ANIMATION;
    private static int scrollWidth = DEFAULT_SCROLL_WIDTH;
    private static boolean enableBossbars = DEFAULT_ENABLE_BOSSBARS;
    private static List<String> globalBossbars = new ArrayList<>();
    private static Map<String, List<String>> groupBossbars = new HashMap<>();
    private static int bossBarLimitPerPlayer = DEFAULT_BOSSBAR_LIMIT;
    
    // YAML instance for serialization/deserialization
    private static final Yaml yaml;
    
    static {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        yaml = new Yaml(options);
    }
    
    /**
     * Initialize the tablist YAML configuration
     */
    public static void initialize() {
        // Create config directory if it doesn't exist
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
            
            // Check for migration from TOML
            if (!Files.exists(YAML_CONFIG_PATH) && Files.exists(OLD_TOML_CONFIG_PATH)) {
                NeoEssentials.LOGGER.info("Migrating tablist.toml to tablist.yml format");
                migrateFromToml();
            }
              // Create default config if it doesn't exist
            if (!Files.exists(YAML_CONFIG_PATH)) {
                // Try to copy from default resources
                try {
                    InputStream defaultConfig = TablistYamlConfig.class.getClassLoader()
                        .getResourceAsStream("default_templates/tablist.yml");
                    if (defaultConfig != null) {
                        NeoEssentials.LOGGER.info("Creating default tablist.yml from templates");
                        Files.copy(defaultConfig, YAML_CONFIG_PATH);
                        return;
                    }
                } catch (Exception e) {
                    NeoEssentials.LOGGER.warn("Could not copy default tablist.yml template", e);
                }
                NeoEssentials.LOGGER.info("Creating default tablist.yml configuration");
                createDefaultConfig();
            }
            
            // Load config
            loadConfig();
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to initialize tablist YAML configuration", e);
        }
    }
    
    /**
     * Load the YAML configuration file
     */
    @SuppressWarnings("unchecked")
    public static void loadConfig() {
        try {
            NeoEssentials.LOGGER.info("Loading tablist configuration from {}", YAML_CONFIG_PATH);
            
            String content = Files.readString(YAML_CONFIG_PATH);
            Map<String, Object> config = yaml.load(content);
            
            // Load tablist section
            Map<String, Object> tablist = (Map<String, Object>) config.getOrDefault("tablist", new HashMap<>());
            
            // Main settings
            updateInterval = getLong(tablist, "updateInterval", DEFAULT_UPDATE_INTERVAL);
            timeFormat = getString(tablist, "timeFormat", DEFAULT_TIME_FORMAT);
            
            // Sorting settings
            enableSorting = getBoolean(tablist, "enableSorting", DEFAULT_ENABLE_SORTING);
            sortType = getString(tablist, "sortType", DEFAULT_SORT_TYPE);
            
            // Display settings
            showEconomyInTablist = getBoolean(tablist, "showEconomyInTablist", DEFAULT_SHOW_ECONOMY);
            enablePlayerSpecificHeaders = getBoolean(tablist, "enablePlayerSpecificHeaders", DEFAULT_ENABLE_PLAYER_HEADERS);
            enablePlayerSpecificFooters = getBoolean(tablist, "enablePlayerSpecificFooters", DEFAULT_ENABLE_PLAYER_FOOTERS);
            
            // Animation settings
            enableAnimations = getBoolean(tablist, "enableAnimations", DEFAULT_ENABLE_ANIMATIONS);
            animationSpeed = getInt(tablist, "animationSpeed", DEFAULT_ANIMATION_SPEED);
            headerAnimationType = getString(tablist, "headerAnimationType", DEFAULT_HEADER_ANIMATION);
            footerAnimationType = getString(tablist, "footerAnimationType", DEFAULT_FOOTER_ANIMATION);
            scrollWidth = getInt(tablist, "scrollWidth", DEFAULT_SCROLL_WIDTH);
            
            // Bossbar settings
            Map<String, Object> bossbars = (Map<String, Object>) config.getOrDefault("bossbars", new HashMap<>());
            enableBossbars = getBoolean(bossbars, "enabled", DEFAULT_ENABLE_BOSSBARS);
            bossBarLimitPerPlayer = getInt(bossbars, "bossBarLimitPerPlayer", DEFAULT_BOSSBAR_LIMIT);
            
            // Load global bossbars (deprecated, kept for migration)
            globalBossbars = getStringList(bossbars, "globalBossBars", Collections.emptyList());
            
            // Load group bossbars (deprecated, kept for migration)
            Map<String, Object> groupBossbarMap = (Map<String, Object>) bossbars.getOrDefault("groupBossBars", new HashMap<>());
            groupBossbars.clear();
            for (Map.Entry<String, Object> entry : groupBossbarMap.entrySet()) {
                String groupName = entry.getKey();
                if (entry.getValue() instanceof List) {
                    List<String> bars = new ArrayList<>();
                    for (Object bar : (List<?>) entry.getValue()) {
                        if (bar instanceof String) {
                            bars.add((String) bar);
                        }
                    }
                    groupBossbars.put(groupName, bars);
                }
            }
            
            NeoEssentials.LOGGER.info("Tablist YAML configuration loaded successfully");
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to load tablist YAML configuration, using defaults", e);
        }
    }
    
    /**
     * Create the default YAML configuration file
     */
    private static void createDefaultConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            
            // Header comment
            String header = "# ========================================================\n" +
                           "# NeoEssentials Tablist Configuration\n" +
                           "# ========================================================\n" +
                           "# This configuration file controls the behavior of the\n" +
                           "# tablist display system, including:\n" +
                           "#  - Header/footer content and animations\n" +
                           "#  - Player sorting and grouping\n" +
                           "#  - Display update intervals\n" +
                           "#  - Animation settings\n" +
                           "# ========================================================\n\n";
            
            // Main tablist settings
            Map<String, Object> tablist = new HashMap<>();
            tablist.put("updateInterval", DEFAULT_UPDATE_INTERVAL);
            tablist.put("timeFormat", DEFAULT_TIME_FORMAT);
            tablist.put("enableSorting", DEFAULT_ENABLE_SORTING);
            tablist.put("sortType", DEFAULT_SORT_TYPE);
            tablist.put("showEconomyInTablist", DEFAULT_SHOW_ECONOMY);
            tablist.put("enablePlayerSpecificHeaders", DEFAULT_ENABLE_PLAYER_HEADERS);
            tablist.put("enablePlayerSpecificFooters", DEFAULT_ENABLE_PLAYER_FOOTERS);
            tablist.put("enableAnimations", DEFAULT_ENABLE_ANIMATIONS);
            tablist.put("animationSpeed", DEFAULT_ANIMATION_SPEED);
            tablist.put("headerAnimationType", DEFAULT_HEADER_ANIMATION);
            tablist.put("footerAnimationType", DEFAULT_FOOTER_ANIMATION);
            tablist.put("scrollWidth", DEFAULT_SCROLL_WIDTH);
            
            config.put("tablist", tablist);
            
            // Bossbar settings
            Map<String, Object> bossbars = new HashMap<>();
            bossbars.put("enabled", DEFAULT_ENABLE_BOSSBARS);
            bossbars.put("bossBarLimitPerPlayer", DEFAULT_BOSSBAR_LIMIT);
            
            // Add deprecated notice for settings moved to templates.json/yml
            List<String> deprecatedNote = Collections.singletonList(
                "DEPRECATED - Configure in neoessentials/templates.json or templates.yml instead"
            );
            bossbars.put("globalBossBars", deprecatedNote);
            
            Map<String, Object> groupBossBars = new HashMap<>();
            groupBossBars.put("admin", deprecatedNote);
            groupBossBars.put("vip", deprecatedNote);
            bossbars.put("groupBossBars", groupBossBars);
            
            config.put("bossbars", bossbars);
            
            // Template note
            Map<String, Object> templateNote = new HashMap<>();
            templateNote.put("note", "Templates for headers, footers, and bossbars are now stored in neoessentials/templates.yml or templates.json");
            config.put("templateNote", templateNote);
            
            // Write to file with header
            String yamlContent = header + yaml.dump(config);
            Files.writeString(YAML_CONFIG_PATH, yamlContent);
            
            NeoEssentials.LOGGER.info("Created default tablist.yml configuration");
            
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to create default tablist.yml configuration", e);
        }
    }
    
    /**
     * Migrate from TOML to YAML configuration
     */
    private static void migrateFromToml() {
        try {
            // Create backup of TOML file
            Path backupPath = OLD_TOML_CONFIG_PATH.resolveSibling("tablist.toml.bak");
            Files.copy(OLD_TOML_CONFIG_PATH, backupPath, StandardCopyOption.REPLACE_EXISTING);
            NeoEssentials.LOGGER.info("Created backup of tablist.toml at {}", backupPath);
            
            // We'll create a default YAML and then let the mod reload it from the TOML values
            createDefaultConfig();
            
            // Add migration notice
            Path migrationNoticePath = CONFIG_DIR.resolve("MIGRATION_NOTICE.md");
            String migrationContent = "# NeoEssentials Configuration Migration\n\n" +
                    "The tablist configuration has been migrated from TOML to YAML format.\n\n" +
                    "- Old configuration: `config/neoessentials/tablist.toml` (backup created)\n" +
                    "- New configuration: `config/neoessentials/tablist.yml`\n\n" +
                    "## Why YAML?\n\n" +
                    "YAML provides better support for complex data structures and is more human-readable.\n" +
                    "This change allows for more flexible configuration options and better supports\n" +
                    "multi-line text in headers, footers, and messages.\n\n" +
                    "## Templates\n\n" +
                    "Templates are now configured in `neoessentials/templates.yml` (preferred) or\n" +
                    "`neoessentials/templates.json` (also supported).\n\n" +
                    "Your existing settings have been automatically migrated.";
            
            Files.writeString(migrationNoticePath, migrationContent);
            NeoEssentials.LOGGER.info("Created migration notice at {}", migrationNoticePath);
            
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to migrate from TOML to YAML configuration", e);
        }
    }
    
    // Helper methods for type-safe config access
    
    private static String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }
    
    private static boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }
    
    private static int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        return defaultValue;
    }
    
    private static long getLong(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        return defaultValue;
    }
    
    @SuppressWarnings("unchecked")
    private static List<String> getStringList(Map<String, Object> map, String key, List<String> defaultValue) {
        Object value = map.get(key);
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item instanceof String) {
                    result.add((String) item);
                }
            }
            return result;
        }
        return defaultValue;
    }
    
    // Getters for configuration values
    
    public static long getUpdateInterval() {
        return updateInterval;
    }
    
    public static String getTimeFormat() {
        return timeFormat;
    }
    
    public static boolean isEnableSorting() {
        return enableSorting;
    }
    
    public static String getSortType() {
        return sortType;
    }
    
    public static boolean isShowEconomyInTablist() {
        return showEconomyInTablist;
    }
    
    public static boolean isEnablePlayerSpecificHeaders() {
        return enablePlayerSpecificHeaders;
    }
    
    public static boolean isEnablePlayerSpecificFooters() {
        return enablePlayerSpecificFooters;
    }
    
    public static boolean isEnableAnimations() {
        return enableAnimations;
    }
    
    public static int getAnimationSpeed() {
        return animationSpeed;
    }
    
    public static String getHeaderAnimationType() {
        return headerAnimationType;
    }
    
    public static String getFooterAnimationType() {
        return footerAnimationType;
    }
    
    public static int getScrollWidth() {
        return scrollWidth;
    }
    
    public static boolean isEnableBossbars() {
        return enableBossbars;
    }
    
    public static List<String> getGlobalBossbars() {
        return new ArrayList<>(globalBossbars);
    }
    
    public static Map<String, List<String>> getGroupBossbars() {
        return new HashMap<>(groupBossbars);
    }
    
    public static int getBossBarLimitPerPlayer() {
        return bossBarLimitPerPlayer;
    }
    
    /**
     * Force a reload of the configuration
     */
    public static void reload() {
        loadConfig();
        
        // Reload the TABLikeTablistManager if available
        var dataManager = NeoEssentials.getInstance().getDataManager();
        var tablistManager = dataManager != null ? dataManager.getTablistManager() : null;
        if (tablistManager != null) {
            tablistManager.reloadConfig();
            NeoEssentials.LOGGER.info("TABLikeTablistManager configuration reloaded");
        }
    }
}
