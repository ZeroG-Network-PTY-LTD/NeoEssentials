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
import java.util.Set;
import java.util.Stack;
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
    private final Path neoEssentialsDir;
    private Path templatesFile;
    
    // Main reference
    private final TabManager tabManager;
      /**
     * Creates a template manager with references to necessary components
     * 
     * @param tabManager The parent TabManager instance
     */
    public TemplateManager(TabManager tabManager) {
        this.tabManager = tabManager;
        this.neoEssentialsDir = Paths.get("neoessentials");
        this.configDir = Paths.get("config", "neoessentials");
        
        // Check in priority order:
        // 1. neoessentials/templates.json
        // 2. neoessentials/templates.yml
        // 3. config/neoessentials/templates.json
        
        Path neoTemplatesJsonFile = neoEssentialsDir.resolve("templates.json");
        Path neoTemplatesYmlFile = neoEssentialsDir.resolve("templates.yml");
        Path configTemplatesFile = configDir.resolve("templates.json");
        
        if (Files.exists(neoTemplatesJsonFile)) {
            this.templatesFile = neoTemplatesJsonFile;
            NeoEssentials.LOGGER.info("Using templates.json from neoessentials directory");
        } else if (Files.exists(neoTemplatesYmlFile)) {
            this.templatesFile = neoTemplatesYmlFile;
            NeoEssentials.LOGGER.info("Using templates.yml from neoessentials directory");
        } else {
            this.templatesFile = configTemplatesFile;
            NeoEssentials.LOGGER.info("Using templates.json from config/neoessentials directory");
        }
        
        // Ensure necessary directories exist
        try {
            if (!Files.exists(neoEssentialsDir)) {
                Files.createDirectories(neoEssentialsDir);
            }
            
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to create directories", e);
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
    }    /**
     * Creates the default templates file from embedded resources
     * 
     * @return True if the file was created successfully
     */
    public boolean createDefaultTemplatesFile() {
        try {
            // Determine the target directory - prefer neoessentials directory
            Path targetFile = neoEssentialsDir.resolve("templates.json");
            
            // First check if we have the file in resources
            InputStream inputStream = TemplateManager.class.getClassLoader()
                    .getResourceAsStream("default-neoessentials/templates.json");
            
            if (inputStream != null) {
                // Copy from resources to target directory
                Files.copy(inputStream, targetFile);
                
                // Update the templatesFile reference to point to the new file
                templatesFile = targetFile;                
                NeoEssentials.LOGGER.info("Created default templates.json in neoessentials directory");
                
                // Return success
                return true;
                
                // Add README file to explain the new location
                Path readmePath = configDir.resolve("README_TEMPLATES.md");
                String readmeContent = "# NeoEssentials Templates\n\n" +
                        "The templates for the tablist system have been moved to the `neoessentials/templates.json` file.\n" +
                        "This provides better configuration flexibility and avoids TOML serialization issues.\n\n" +
                        "## Location\n\n" +
                        "- Primary location: `neoessentials/templates.json`\n" +
                        "- Legacy location (no longer recommended): `config/neoessentials/templates.json`\n\n" +
                        "## Format\n\n" +
                        "The templates file uses JSON format for maximum compatibility and flexibility.\n\n" +
                        "## Migration\n\n" +
                        "Your existing templates have been automatically migrated to the new location.\n" +
                        "All customizations are preserved.";
                
                if (!Files.exists(readmePath)) {
                    Files.writeString(readmePath, readmeContent);
                }
                
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
                
                // Add groups section
                JsonObject groups = new JsonObject();
                
                // Admin group
                JsonObject adminGroup = new JsonObject();
                JsonArray adminHeaders = new JsonArray();
                adminHeaders.add("&4&l★ &c&lAdmin Panel &4&l★");
                adminHeaders.add("&cServer TPS: &f%tps% &7| &cMemory: &f%memory_percent%");
                adminGroup.add("headers", adminHeaders);
                
                JsonArray adminFooters = new JsonArray();
                adminFooters.add("&cAdmin Command Help: &f/neoessentials help");
                adminFooters.add("&cServer uptime: &f%uptime%");
                adminGroup.add("footers", adminFooters);
                
                groups.add("admin", adminGroup);
                
                // VIP group
                JsonObject vipGroup = new JsonObject();
                JsonArray vipHeaders = new JsonArray();
                vipHeaders.add("&6&l⚜ &e&lVIP Perks Active &6&l⚜");
                vipHeaders.add("&eWelcome back, &6%player%&e!");
                vipGroup.add("headers", vipHeaders);
                
                JsonArray vipFooters = new JsonArray();
                vipFooters.add("&6VIP Balance: &e%balance% coins");
                vipFooters.add("&6Use &e/vip help &6for a list of perks");
                vipGroup.add("footers", vipFooters);
                
                groups.add("vip", vipGroup);
                
                root.add("groups", groups);
                
                // Add bossbars section
                JsonObject bossbars = new JsonObject();
                JsonArray globalBars = new JsonArray();
                globalBars.add("{color:red}{style:progress}{progress:1.0}Server TPS: %tps%");
                globalBars.add("{color:green}{style:notched_6}{progress:0.8}Welcome to the server!");
                bossbars.add("global", globalBars);
                
                // Add bossbar groups
                JsonObject bossbarGroups = new JsonObject();
                
                JsonArray adminBars = new JsonArray();
                adminBars.add("{color:purple}{style:progress}{progress:1.0}Admin Mode Active");
                bossbarGroups.add("admin", adminBars);
                
                JsonArray vipBars = new JsonArray();
                vipBars.add("{color:gold}{style:progress}{progress:1.0}VIP Status Active");
                bossbarGroups.add("vip", vipBars);
                
                bossbars.add("groups", bossbarGroups);
                root.add("bossbars", bossbars);
                
                // Set basic metadata
                JsonObject meta = new JsonObject();
                meta.addProperty("schemaVersion", "1.0");
                meta.addProperty("description", "NeoEssentials Tablist Templates");
                meta.addProperty("generateTime", java.time.LocalDateTime.now().toString());
                root.add("metadata", meta);
                
                // Write to file
                Files.writeString(targetFile, GSON.toJson(root));
                
                // Update the templatesFile reference
                templatesFile = targetFile;
                
                NeoEssentials.LOGGER.info("Created fallback templates.json file in neoessentials directory");
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to create default templates file", e);
        }
    }    /**
     * Loads templates from the templates.json or templates.yml file
     */
    public void loadTemplates() {
        try {
            // Read the templates file
            String content = Files.readString(templatesFile, StandardCharsets.UTF_8);
            JsonObject root;
            
            // Check if we're loading a YML file
            if (templatesFile.toString().endsWith(".yml")) {
                NeoEssentials.LOGGER.info("Loading templates from YAML file");
                try {
                    // Simple YAML parsing - this is basic but handles most common YAML structures
                    // For complex YAML, would need SnakeYAML dependency
                    root = parseSimpleYaml(content);
                    if (root == null) {
                        throw new Exception("Failed to parse YAML");
                    }
                } catch (Exception e) {
                    NeoEssentials.LOGGER.error("Failed to parse YAML file, falling back to default templates", e);
                    createDefaultTemplatesFile();
                    content = Files.readString(templatesFile, StandardCharsets.UTF_8);
                    root = JsonParser.parseString(content).getAsJsonObject();
                }
            } else {
                // Load JSON directly
                root = JsonParser.parseString(content).getAsJsonObject();
            }
            
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
     * Gets all group headers
     * 
     * @return Map of group names to their header templates
     */
    public Map<String, List<String>> getAllGroupHeaders() {
        // Create a deep copy to prevent modifications
        Map<String, List<String>> result = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : groupHeaders.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }
    
    /**
     * Gets all group footers
     * 
     * @return Map of group names to their footer templates
     */
    public Map<String, List<String>> getAllGroupFooters() {
        // Create a deep copy to prevent modifications
        Map<String, List<String>> result = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : groupFooters.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
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
     * Gets the names of all groups that have templates defined
     * 
     * @return Set of group names
     */
    public Set<String> getGroupNames() {
        // Combine keys from both maps
        Set<String> result = new java.util.HashSet<>();
        result.addAll(groupHeaders.keySet());
        result.addAll(groupFooters.keySet());
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
    
    /**
     * Force reloads templates from the templates file
     * This is mainly used for debugging purposes
     * 
     * @return True if templates were successfully reloaded
     */
    public boolean forceReload() {
        try {
            // Check if templates file exists
            if (!Files.exists(templatesFile)) {
                NeoEssentials.LOGGER.error("Cannot reload templates: File does not exist at {}", templatesFile);
                return false;
            }
            
            // Calculate a hash of the current file content to detect changes
            String fileContent = Files.readString(templatesFile, StandardCharsets.UTF_8);
            int contentHash = fileContent.hashCode();
            
            // Log debug info
            NeoEssentials.LOGGER.info("Force reloading templates from {} (content hash: {})", templatesFile, contentHash);
            
            // Clear current templates
            globalHeaders.clear();
            globalFooters.clear();
            groupHeaders.clear();
            groupFooters.clear();
            globalBossBars.clear();
            groupBossBars.clear();
            
            // Load templates from file
            loadTemplates();
            
            NeoEssentials.LOGGER.info("Templates force reloaded: {} headers, {} footers, {} group headers, {} group footers", 
                    globalHeaders.size(), globalFooters.size(), groupHeaders.size(), groupFooters.size());
            
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to force reload templates", e);
            return false;
        }
    }
    
    /**
     * Parse simple YAML content into a JsonObject
     * This is a basic parser that handles simple YAML structures commonly used in templates
     * 
     * @param content YAML content as string
     * @return JsonObject representation of the YAML
     */
    private JsonObject parseSimpleYaml(String content) {
        JsonObject root = new JsonObject();
        try {
            String[] lines = content.split("\\r?\\n");
            String currentSection = null;
            JsonObject currentObject = root;
            int currentIndent = 0;
            Stack<JsonObject> objectStack = new Stack<>();
            Stack<String> sectionStack = new Stack<>();
            Stack<Integer> indentStack = new Stack<>();
            
            for (String line : lines) {
                // Skip comments and empty lines
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }
                
                // Calculate indentation level
                int indent = 0;
                while (indent < line.length() && line.charAt(indent) == ' ') {
                    indent++;
                }
                
                // Remove indentation
                String trimmed = line.trim();
                
                // Handle section
                if (trimmed.endsWith(":")) {
                    String section = trimmed.substring(0, trimmed.length() - 1);
                    
                    // If indentation increases, push current object to stack
                    if (indent > currentIndent) {
                        objectStack.push(currentObject);
                        sectionStack.push(currentSection);
                        indentStack.push(currentIndent);
                    }
                    // If indentation decreases, pop from stack
                    else if (indent < currentIndent) {
                        while (!indentStack.isEmpty() && indent < indentStack.peek()) {
                            currentObject = objectStack.pop();
                            currentSection = sectionStack.pop();
                            currentIndent = indentStack.pop();
                        }
                    }
                    
                    // Create new object for this section
                    JsonObject newObject = new JsonObject();
                    currentObject.add(section, newObject);
                    currentObject = newObject;
                    currentSection = section;
                    currentIndent = indent;
                }
                // Handle list item
                else if (trimmed.startsWith("- ")) {
                    String value = trimmed.substring(2);
                    
                    // Get or create array for current section
                    JsonArray array;
                    if (currentObject.has(currentSection) && currentObject.get(currentSection).isJsonArray()) {
                        array = currentObject.getAsJsonArray(currentSection);
                    } else {
                        array = new JsonArray();
                        currentObject.add(currentSection, array);
                    }
                    
                    // Add value to array - remove quotes if present
                    if ((value.startsWith("'") && value.endsWith("'")) || 
                        (value.startsWith("\"") && value.endsWith("\""))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    array.add(value);
                }
                // Handle key-value pair
                else if (trimmed.contains(": ")) {
                    String[] parts = trimmed.split(": ", 2);
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    
                    // Remove quotes if present
                    if ((value.startsWith("'") && value.endsWith("'")) || 
                        (value.startsWith("\"") && value.endsWith("\""))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    
                    // Try to parse as number or boolean
                    if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                        currentObject.addProperty(key, Boolean.parseBoolean(value));
                    } else if (value.matches("\\d+")) {
                        currentObject.addProperty(key, Integer.parseInt(value));
                    } else if (value.matches("\\d+\\.\\d+")) {
                        currentObject.addProperty(key, Double.parseDouble(value));
                    } else {
                        currentObject.addProperty(key, value);
                    }
                }
            }
            
            NeoEssentials.LOGGER.info("Successfully parsed YAML content");
            return root;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error parsing YAML content", e);
            return null;
        }
    }
}
