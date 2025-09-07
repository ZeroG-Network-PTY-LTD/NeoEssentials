package com.zerog.neoessentials.config;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

/**
 * Custom Placeholders Configuration for NeoEssentials
 * Represents the customPlaceholders.json file structure
 */
public class CustomPlaceholderConfig {
    
    public Map<String, CustomPlaceholder> customPlaceholders = new HashMap<>();
    
    public CustomPlaceholderConfig() {
        // Initialize with some default placeholders
        customPlaceholders.put("afk_tag", new CustomPlaceholder(
            "conditional", "${essentials_afk} == true", "&7[&cAFK&7]", ""
        ));
        customPlaceholders.put("welcome_message", new CustomPlaceholder(
            "static", "&6Welcome to &bNeoEssentials &6Server!"
        ));
        customPlaceholders.put("server_status_animation", new CustomPlaceholder(
            "animated", new String[]{"&a◉ &fOnline", "&e◉ &fOnline", "&6◉ &fOnline"}, 0.8
        ));
    }
    
    public static final String DEFAULT_TEMPLATE = "{\n" +
            "  \"customPlaceholders\": {\n" +
            "    \"afk_tag\": {\n" +
            "      \"type\": \"conditional\",\n" +
            "      \"condition\": \"${essentials_afk} == true\",\n" +
            "      \"true\": \"&7[&cAFK&7]\",\n" +
            "      \"false\": \"\"\n" +
            "    },\n" +
            "    \"welcome_message\": {\n" +
            "      \"type\": \"static\",\n" +
            "      \"value\": \"&6Welcome to &bNeoEssentials &6Server!\"\n" +
            "    }\n" +
            "  }\n" +
            "}";
    
    public Set<String> getCustomPlaceholderNames() {
        return new HashSet<>(customPlaceholders.keySet());
    }
    
    public CustomPlaceholder getPlaceholder(String name) {
        return customPlaceholders.get(name);
    }
    
    public void addPlaceholder(String name, CustomPlaceholder placeholder) {
        customPlaceholders.put(name, placeholder);
    }
    
    // For backward compatibility - static singleton methods
    private static CustomPlaceholderConfig instance;
    
    public static CustomPlaceholderConfig getInstance() {
        if (instance == null) {
            instance = new CustomPlaceholderConfig();
        }
        return instance;
    }
    
    public void reloadConfig() {
        // This method exists for compatibility but does nothing since
        // configuration is now managed by ConfigManager
    }
    
    // For backward compatibility
    public boolean hasConfig() {
        return !customPlaceholders.isEmpty();
    }
    
    public com.google.gson.JsonObject getConfigData() {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String json = gson.toJson(this);
        return com.google.gson.JsonParser.parseString(json).getAsJsonObject();
    }
    
    public static class CustomPlaceholder {
        public String type;
        public String condition;
        public String trueValue;
        public String falseValue;
        public String value;
        public String[] frames;
        public double interval;
        
        // Default constructor for JSON deserialization
        public CustomPlaceholder() {}
        
        // Constructor for conditional placeholders
        public CustomPlaceholder(String type, String condition, String trueValue, String falseValue) {
            this.type = type;
            this.condition = condition;
            this.trueValue = trueValue;
            this.falseValue = falseValue;
        }
        
        // Constructor for static placeholders
        public CustomPlaceholder(String type, String value) {
            this.type = type;
            this.value = value;
        }
        
        // Constructor for animated placeholders
        public CustomPlaceholder(String type, String[] frames, double interval) {
            this.type = type;
            this.frames = frames;
            this.interval = interval;
        }
    }
}
