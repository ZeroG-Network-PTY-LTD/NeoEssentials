package com.zerog.neoessentials.ui.tab;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.server.level.ServerPlayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages all tablist templates for the TabManager system
 * Templates include headers, footers, and boss bars for players and groups
 */
public class TemplateManager {
    private static final Gson GSON = new Gson();
    
    // Templates storage
    private List<String> globalHeaders = new ArrayList<>();
    private List<String> globalFooters = new ArrayList<>();
    private Map<String, List<String>> groupHeaders = new HashMap<>();
    private Map<String, List<String>> groupFooters = new HashMap<>();
    private List<String> globalBossBars = new ArrayList<>();
    private Map<String, List<String>> groupBossBars = new HashMap<>();
    
    // Paths
    private final Path configDir;
    private final Path templatesFile;
    
    // Main reference
    private final TabManager tabManager;
    
    /**
     * Creates a template manager with references to necessary components
     * 
     * @param tabManager The parent TabManager instance
     */
    public TemplateManager(TabManager tabManager) {
        this.tabManager = tabManager;
        this.configDir = Paths.get("config", "neoessentials");
        this.templatesFile = configDir.resolve("templates.json");
        
        // Ensure config directory exists
        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                NeoEssentials.LOGGER.error("Failed to create config directory", e);
            }
        }
    }
    
    /**
     * Initialize the template manager and load templates from file
     */
    public void initialize() {
        // Create default templates file if it doesn't exist
        if (!Files.exists(templatesFile)) {
            createDefaultTemplatesFile();
        }
        
        // Load templates from file
        loadTemplates();
    }
    
    /**
     * Creates the default templates file from embedded resources
     */
    private void createDefaultTemplatesFile() {
        try {
            // First check if we have the file in resources
            InputStream inputStream = TemplateManager.class.getClassLoader()
                    .getResourceAsStream("default-config/templates.json");
            
            if (inputStream != null) {
                // Copy from resources to config dir
                Files.copy(inputStream, templatesFile);
                NeoEssentials.LOGGER.info("Created default templates.json file");
            } else {
                // Fallback to creating a basic templates file
                JsonObject root = new JsonObject();
                
                // Basic headers and footers
                JsonObject templates = new JsonObject();
                JsonArray headers = new JsonArray();
                headers.add("&6&l✦ &b&lNeoEssentials Server &6&l✦");
                headers.add("&eWelcome, &a%player%&e!");
                headers.add("&eOnline players: &a%online%/%max%");
                headers.add("&eServer time: &a%time%");
                templates.add("headers", headers);
                
                JsonArray footers = new JsonArray();
                footers.add("&eBalance: &a%balance% coins");
                footers.add("&eWebsite: &awww.example.com");
                footers.add("&eThanks for playing!");
                footers.add("&eServer TPS: &a%tps% &7| &eMemory: &a%memory_percent%");
                templates.add("footers", footers);
                
                root.add("templates", templates);
                
                // Set basic metadata
                JsonObject meta = new JsonObject();
                meta.addProperty("schemaVersion", "1.0");
                meta.addProperty("description", "NeoEssentials Tablist Templates");
                meta.addProperty("generateTime", java.time.LocalDateTime.now().toString());
                root.add("metadata", meta);
                
                // Write to file
                Files.writeString(templatesFile, GSON.toJson(root));
                NeoEssentials.LOGGER.info("Created fallback templates.json file");
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to create default templates file", e);
        }
    }
    
    /**
     * Loads templates from the templates.json file
     */
    public void loadTemplates() {
        try {
            // Read the templates file
            String content = Files.readString(templatesFile, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            
            // Load global headers and footers
            if (root.has("templates")) {
                JsonObject templates = root.getAsJsonObject("templates");
                
                if (templates.has("headers")) {
                    globalHeaders = jsonArrayToStringList(templates.getAsJsonArray("headers"));
                    NeoEssentials.LOGGER.info("Loaded {} global header templates", globalHeaders.size());
                }
                
                if (templates.has("footers")) {
                    globalFooters = jsonArrayToStringList(templates.getAsJsonArray("footers"));
                    NeoEssentials.LOGGER.info("Loaded {} global footer templates", globalFooters.size());
                }
            }
            
            // Load group-specific templates
            if (root.has("groups")) {
                JsonObject groups = root.getAsJsonObject("groups");
                
                for (Map.Entry<String, JsonElement> entry : groups.entrySet()) {
                    String groupName = entry.getKey();
                    JsonObject groupData = entry.getValue().getAsJsonObject();
                    
                    if (groupData.has("headers")) {
                        List<String> headers = jsonArrayToStringList(groupData.getAsJsonArray("headers"));
                        groupHeaders.put(groupName, headers);
                    }
                    
                    if (groupData.has("footers")) {
                        List<String> footers = jsonArrayToStringList(groupData.getAsJsonArray("footers"));
                        groupFooters.put(groupName, footers);
                    }
                }
                
                NeoEssentials.LOGGER.info("Loaded templates for {} groups", groups.size());
            }
            
            // Load boss bars
            if (root.has("bossbars")) {
                JsonObject bossbars = root.getAsJsonObject("bossbars");
                
                if (bossbars.has("global")) {
                    globalBossBars = jsonArrayToStringList(bossbars.getAsJsonArray("global"));
                    NeoEssentials.LOGGER.info("Loaded {} global boss bars", globalBossBars.size());
                }
                
                if (bossbars.has("groups")) {
                    JsonObject groups = bossbars.getAsJsonObject("groups");
                    
                    for (Map.Entry<String, JsonElement> entry : groups.entrySet()) {
                        String groupName = entry.getKey();
                        JsonArray bossBarArray = entry.getValue().getAsJsonArray();
                        List<String> bossBarList = jsonArrayToStringList(bossBarArray);
                        groupBossBars.put(groupName, bossBarList);
                    }
                    
                    NeoEssentials.LOGGER.info("Loaded boss bars for {} groups", groupBossBars.size());
                }
            }
            
            NeoEssentials.LOGGER.info("Templates loaded successfully from {}", templatesFile);
            
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to load templates from file", e);
            // Fall back to empty lists for safety
            globalHeaders = new ArrayList<>();
            globalFooters = new ArrayList<>();
            groupHeaders = new HashMap<>();
            groupFooters = new HashMap<>();
            globalBossBars = new ArrayList<>();
            groupBossBars = new HashMap<>();
        }
    }
    
    /**
     * Converts a JsonArray to a List of strings
     * 
     * @param array The JsonArray to convert
     * @return A list of strings from the array
     */
    private List<String> jsonArrayToStringList(JsonArray array) {
        List<String> result = new ArrayList<>(array.size());
        array.forEach(element -> result.add(element.getAsString()));
        return result;
    }
    
    /**
     * Gets the global header templates
     * 
     * @return List of global header templates
     */
    public List<String> getGlobalHeaders() {
        return new ArrayList<>(globalHeaders); // Return a copy to prevent modification
    }
    
    /**
     * Gets the global footer templates
     * 
     * @return List of global footer templates
     */
    public List<String> getGlobalFooters() {
        return new ArrayList<>(globalFooters); // Return a copy to prevent modification
    }
    
    /**
     * Gets the headers for a specific group
     * 
     * @param groupName The name of the group
     * @return List of header templates for the group, or null if not defined
     */
    public List<String> getGroupHeaders(String groupName) {
        List<String> headers = groupHeaders.get(groupName);
        return headers != null ? new ArrayList<>(headers) : null;
    }
    
    /**
     * Gets the footers for a specific group
     * 
     * @param groupName The name of the group
     * @return List of footer templates for the group, or null if not defined
     */
    public List<String> getGroupFooters(String groupName) {
        List<String> footers = groupFooters.get(groupName);
        return footers != null ? new ArrayList<>(footers) : null;
    }
    
    /**
     * Gets the global boss bars
     * 
     * @return List of global boss bars
     */
    public List<String> getGlobalBossBars() {
        return new ArrayList<>(globalBossBars); // Return a copy to prevent modification
    }
    
    /**
     * Gets the boss bars for a specific group
     * 
     * @param groupName The name of the group
     * @return List of boss bar templates for the group, or empty list if not defined
     */
    public Map<String, List<String>> getGroupBossBars() {
        // Create a deep copy to prevent modifications
        Map<String, List<String>> result = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : groupBossBars.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }
    
    /**
     * Reloads templates from disk
     * This can be called when the templates file is modified
     * 
     * @return true if reload was successful, false otherwise
     */
    public boolean reload() {
        try {
            loadTemplates();
            NeoEssentials.LOGGER.info("Templates reloaded successfully");
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to reload templates", e);
            return false;
        }
    }
}
