package com.zerog.neoessentials.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration organization system
 * Groups configurations into logical categories for better management
 */
public class ConfigCategories {
    
    public enum Category {
        CORE("Core", "Essential server functionality"),
        FEATURES("Features", "Optional server features"),  
        INTEGRATION("Integration", "External service integrations"),
        APPEARANCE("Appearance", "Visual and display settings");
        
        private final String displayName;
        private final String description;
        
        Category(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // Configuration categorization mapping
    private static final Map<String, Category> CONFIG_CATEGORIES = new HashMap<>();
    
    static {
        // Core configurations
        CONFIG_CATEGORIES.put("main", Category.CORE);
        CONFIG_CATEGORIES.put("spawn", Category.CORE);
        CONFIG_CATEGORIES.put("moderation", Category.CORE);
        
        // Feature configurations  
        CONFIG_CATEGORIES.put("economy", Category.FEATURES);
        CONFIG_CATEGORIES.put("homes", Category.FEATURES);
        CONFIG_CATEGORIES.put("kits", Category.FEATURES);
        CONFIG_CATEGORIES.put("warps", Category.FEATURES);
        CONFIG_CATEGORIES.put("messaging", Category.FEATURES);
        
        // Integration configurations
        CONFIG_CATEGORIES.put("discord", Category.INTEGRATION);
        
        // Appearance configurations
        CONFIG_CATEGORIES.put("tablist", Category.APPEARANCE);
    }
    
    /**
     * Get category for a configuration
     */
    public static Category getCategory(String configName) {
        return CONFIG_CATEGORIES.getOrDefault(configName.toLowerCase(), Category.FEATURES);
    }
    
    /**
     * Get all configurations in a category
     */
    public static List<String> getConfigsInCategory(Category category) {
        return CONFIG_CATEGORIES.entrySet().stream()
            .filter(entry -> entry.getValue() == category)
            .map(Map.Entry::getKey)
            .sorted()
            .toList();
    }
    
    /**
     * Get configurations grouped by category
     */
    public static Map<Category, List<String>> getConfigsByCategory() {
        Map<Category, List<String>> result = new HashMap<>();
        
        for (Category category : Category.values()) {
            result.put(category, getConfigsInCategory(category));
        }
        
        return result;
    }
    
    /**
     * Get configuration priority (core configs should be loaded first)
     */
    public static int getLoadPriority(String configName) {
        Category category = getCategory(configName);
        return switch (category) {
            case CORE -> 1;         // Load first
            case FEATURES -> 2;     // Load second
            case APPEARANCE -> 3;   // Load third
            case INTEGRATION -> 4;  // Load last
        };
    }
    
    /**
     * Get configuration display info
     */
    public static String getConfigDisplayInfo(String configName) {
        Category category = getCategory(configName);
        return String.format("%s (%s)", configName, category.getDisplayName());
    }
    
    /**
     * Get configuration descriptions
     */
    public static Map<String, String> getConfigDescriptions() {
        Map<String, String> descriptions = new HashMap<>();
        
        descriptions.put("main", "Core server configuration and module toggles");
        descriptions.put("economy", "Virtual economy system settings");
        descriptions.put("homes", "Player home system configuration"); 
        descriptions.put("kits", "Predefined item kit configurations");
        descriptions.put("warps", "Server warp point configurations");
        descriptions.put("moderation", "Moderation tools and settings");
        descriptions.put("messaging", "Private messaging system settings");
        descriptions.put("discord", "Discord webhook integration settings");
        descriptions.put("tablist", "Player list appearance and formatting");
        descriptions.put("spawn", "Server spawn point configuration");
        
        return descriptions;
    }
    
    /**
     * Check if configuration is critical (required for basic operation)
     */
    public static boolean isCritical(String configName) {
        return Arrays.asList("main", "spawn").contains(configName.toLowerCase());
    }
}
