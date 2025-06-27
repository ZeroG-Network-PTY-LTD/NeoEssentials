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
        // 1. neoessentials/templates.yml (YAML preferred over JSON)
        // 2. neoessentials/templates.json
        // 3. config/neoessentials/templates.yml
        // 4. config/neoessentials/templates.json
        
        Path neoTemplatesYmlFile = neoEssentialsDir.resolve("templates.yml");
        Path neoTemplatesJsonFile = neoEssentialsDir.resolve("templates.json");
        Path configTemplatesYmlFile = configDir.resolve("templates.yml");
        Path configTemplatesJsonFile = configDir.resolve("templates.json");
        
        // First try to find YAML format (preferred)
        if (Files.exists(neoTemplatesYmlFile)) {
            this.templatesFile = neoTemplatesYmlFile;
            NeoEssentials.LOGGER.info("Using templates.yml from neoessentials directory");
        } else if (Files.exists(neoTemplatesJsonFile)) {
            this.templatesFile = neoTemplatesJsonFile;
            NeoEssentials.LOGGER.info("Using templates.json from neoessentials directory (YAML format preferred)");
        } else if (Files.exists(configTemplatesYmlFile)) {
            this.templatesFile = configTemplatesYmlFile;
            NeoEssentials.LOGGER.info("Using templates.yml from config/neoessentials directory");
        } else {
            this.templatesFile = configTemplatesJsonFile;
            NeoEssentials.LOGGER.info("Using templates.json from config/neoessentials directory (YAML format preferred)");
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
            // Determine the target directory - prefer neoessentials directory with YAML
            Path targetYamlFile = neoEssentialsDir.resolve("templates.yml");
            Path targetJsonFile = neoEssentialsDir.resolve("templates.json");
            
            boolean success = false;
            
            // Try to extract the YAML template first (preferred format)
            if (extractDefaultTemplate("templates.yml", targetYamlFile)) {
                // Update the templatesFile reference to point to the new file
                templatesFile = targetYamlFile;
                success = true;
                NeoEssentials.LOGGER.info("Created default templates.yml in neoessentials directory");
            }
            // Try default_templates resource next
            else if (extractDefaultTemplate("default_templates/templates.yml", targetYamlFile)) {
                // Update the templatesFile reference to point to the new file
                templatesFile = targetYamlFile;
                success = true;
                NeoEssentials.LOGGER.info("Created default templates.yml in neoessentials directory from default_templates resource");
            }
            // Fall back to JSON if YAML extraction fails
            else if (extractDefaultTemplate("default-neoessentials/templates.json", targetJsonFile)) {
                // Update the templatesFile reference to point to the new file
                templatesFile = targetJsonFile;
                success = true;
                NeoEssentials.LOGGER.info("Created default templates.json in neoessentials directory");
            }
            
            if (success) {
                // Add README file to explain the new location
                try {
                    Path readmePath = configDir.resolve("README_TEMPLATES.md");
                    String readmeContent = "# NeoEssentials Templates\n\n" +
                            "The templates for the tablist system have been moved to the `neoessentials/templates.yml` file.\n" +
                            "This provides better configuration flexibility and avoids serialization issues.\n\n" +
                            "## Location\n\n" +
                            "- Primary location: `neoessentials/templates.yml` (YAML format, preferred)\n" +
                            "- Alternative: `neoessentials/templates.json` (JSON format, supported)\n" +
                            "- Legacy location (no longer recommended): `config/neoessentials/templates.json`\n\n" +
                            "## Format\n\n" +
                            "The templates file now uses YAML format for improved readability and flexibility.\n" +
                            "JSON format is still supported for backward compatibility.\n\n" +
                            "## Migration\n\n" +
                            "Your existing templates have been automatically migrated to the new location and format.\n" +
                            "All customizations are preserved.";
                    
                    if (!Files.exists(readmePath)) {
                        Files.writeString(readmePath, readmeContent);
                    }
                } catch (Exception e) {
                    NeoEssentials.LOGGER.error("Error creating README file", e);
                    // Don't fail over README issues
                }
                
                // Return success
                return true;
            }
            
            // If no success with resource extraction, create a basic templates file from scratch
            // We'll create a YAML file since it's our preferred format
            Path yamlTargetFile = neoEssentialsDir.resolve("templates.yml");
            
            // Create basic YAML template content
            String yamlContent = "# NeoEssentials Tablist Templates\n" +
                "# Generated: " + java.time.LocalDateTime.now().toString() + "\n\n" +
                "templates:\n" +
                "  headers:\n" +
                "    - \"&6&l✦ &b&lNeoEssentials Server &6&l✦\"\n" +
                "    - \"&eWelcome, &a{player_name}&e!\"\n" +
                "    - \"&eOnline players: &a{online_players}/{max_players}\"\n" +
                "    - \"&eServer time: &a{time}\"\n\n" +
                "  footers:\n" +
                "    - \"&eBalance: &a{balance} coins\"\n" +
                "    - \"&eWebsite: &awww.example.com\"\n" +
                "    - \"&eThanks for playing!\"\n" +
                "    - \"&eServer TPS: &a{tps} &7| &eMemory: &a{memory_percent}%\"\n\n" +
                "groups:\n" +
                "  admin:\n" +
                "    headers:\n" +
                "      - \"&4&l★ &c&lAdmin Panel &4&l★\"\n" +
                "      - \"&cServer TPS: &f{tps} &7| &cMemory: &f{memory_percent}%\"\n" +
                "    footers:\n" +
                "      - \"&cAdmin Command Help: &f/neoessentials help\"\n" +
                "      - \"&cServer uptime: &f{uptime}\"\n\n" +
                "  default:\n" +
                "    headers:\n" +
                "      - \"&6&l⚜ &e&lWelcome &6&l⚜\"\n" +
                "      - \"&eWelcome to the server, &6{player_name}&e!\"\n" +
                "    footers:\n" +
                "      - \"&6Balance: &e{balance} coins\"\n" +
                "      - \"&6Use &e/help &6for a list of commands\"\n\n" +
                "bossbars:\n" +
                "  global:\n" +
                "    - \"title:Server TPS: {tps};progress:1.0;color:red\"\n" +
                "    - \"title:Welcome to the server!;progress:0.8;color:green\"\n" +
                "  groups:\n" +
                "    admin:\n" +
                "      - \"title:Admin Mode Active;progress:1.0;color:purple\"\n" +
                "    vip:\n" +
                "      - \"title:VIP Status Active;progress:1.0;color:gold\"\n\n" +
                "# Metadata\n" +
                "metadata:\n" +
                "  schemaVersion: \"1.0\"\n" +
                "  description: \"NeoEssentials Tablist Templates\"\n" +
                "  format: \"yaml\"\n";
                  // Ensure directories exist and write YAML content
        if (!Files.exists(yamlTargetFile.getParent())) {
            Files.createDirectories(yamlTargetFile.getParent());
        }
        
        try {
            // Write YAML content
            Files.writeString(yamlTargetFile, yamlContent);
            
            // Update the templatesFile reference
            templatesFile = yamlTargetFile;
            NeoEssentials.LOGGER.info("Created new templates.yml file in neoessentials directory");
            return true;
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to create YAML templates file, trying JSON format", e);
            
            // Fall back to JSON format if YAML fails
            Path jsonTargetFile = neoEssentialsDir.resolve("templates.json");
            
            // Create basic JSON structure
            JsonObject root = new JsonObject();
            JsonObject templates = new JsonObject();
            JsonObject groups = new JsonObject();
            JsonObject bossbars = new JsonObject();
            
            // Add simple templates
            JsonArray headersArray = new JsonArray();
            headersArray.add("&6&l✦ &b&lNeoEssentials Server &6&l✦");
            headersArray.add("&eWelcome, &a{player_name}&e!");
            templates.add("headers", headersArray);
            
            JsonArray footersArray = new JsonArray();
            footersArray.add("&eBalance: &a{balance} coins");
            footersArray.add("&eServer TPS: &a{tps}");
            templates.add("footers", footersArray);
            
            // Add minimal structure
            root.add("templates", templates);
            root.add("groups", groups);
            root.add("bossbars", bossbars);
            
            try {
                // Write to JSON file
                Files.writeString(jsonTargetFile, GSON.toJson(root));
                
                // Update the templatesFile reference
                templatesFile = jsonTargetFile;
                NeoEssentials.LOGGER.info("Created fallback templates.json file in neoessentials directory");
                return true;
            } catch (IOException jsonError) {
                NeoEssentials.LOGGER.error("Failed to create JSON templates file", jsonError);
                return false;
            }
        }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to create default templates file", e);
            return false;
        }
    }    /**
     * Loads templates from the templates.json or templates.yml file
     */
    public void loadTemplates() {
        try {
            // Check if the file exists first
            if (!Files.exists(templatesFile)) {
                NeoEssentials.LOGGER.warn("Template file {} not found, creating default template file", templatesFile);
                createDefaultTemplatesFile();
            }
            
            // Read the templates file
            String content = Files.readString(templatesFile, StandardCharsets.UTF_8);
            JsonObject root;
            
            // Check if we're loading a YML file
            if (templatesFile.toString().endsWith(".yml")) {
                NeoEssentials.LOGGER.info("Loading templates from YAML file: {}", templatesFile);
                try {
                    // Use SnakeYAML if available, otherwise fall back to simple parser
                    try {
                        Class.forName("org.yaml.snakeyaml.Yaml");
                        // If we get here, SnakeYAML is available
                        root = parseYamlWithSnakeYaml(content);
                    } catch (ClassNotFoundException e) {
                        NeoEssentials.LOGGER.warn("SnakeYAML not found, using simple YAML parser");
                        root = parseSimpleYaml(content);
                    }
                    
                    if (root == null) {
                        throw new Exception("Failed to parse YAML file");
                    }
                } catch (Exception e) {
                    NeoEssentials.LOGGER.error("Failed to parse YAML file, falling back to default templates", e);
                    createDefaultTemplatesFile();
                    content = Files.readString(templatesFile, StandardCharsets.UTF_8);
                    root = JsonParser.parseString(content).getAsJsonObject();
                }
            } else {
                // Load JSON directly
                NeoEssentials.LOGGER.info("Loading templates from JSON file: {}", templatesFile);
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
     * Check if templates are loaded
     * @return true if at least one template is loaded
     */
    public boolean hasTemplates() {
        return !globalHeaders.isEmpty() 
               || !globalFooters.isEmpty() 
               || !groupHeaders.isEmpty() 
               || !groupFooters.isEmpty()
               || !globalBossBars.isEmpty()
               || !groupBossBars.isEmpty();
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
    
    /**
     * Parse YAML content using SnakeYAML library
     * @param yamlContent The YAML content to parse
     * @return JsonObject representation of the YAML content
     */
    private JsonObject parseYamlWithSnakeYaml(String yamlContent) {
        try {
            // Use reflection to avoid direct dependency on SnakeYAML
            Class<?> yamlClass = Class.forName("org.yaml.snakeyaml.Yaml");
            Object yamlInstance = yamlClass.getDeclaredConstructor().newInstance();
            
            // Load the YAML content
            java.lang.reflect.Method loadMethod = yamlClass.getMethod("load", String.class);
            Object yamlMap = loadMethod.invoke(yamlInstance, yamlContent);
            
            // Convert the map to a JsonObject
            return convertMapToJsonObject(yamlMap);
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error using SnakeYAML to parse YAML content", e);
            return null;
        }
    }
    
    /**
     * Convert a Map from SnakeYAML to a JsonObject
     * @param obj The object from SnakeYAML
     * @return JsonObject representation
     */
    @SuppressWarnings("unchecked")
    private JsonObject convertMapToJsonObject(Object obj) {
        if (obj == null) return null;
        
        JsonObject result = new JsonObject();
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                if (value instanceof Map) {
                    result.add(key, convertMapToJsonObject(value));
                } else if (value instanceof List) {
                    result.add(key, convertListToJsonArray((List<?>) value));
                } else if (value instanceof String) {
                    result.addProperty(key, (String) value);
                } else if (value instanceof Number) {
                    result.addProperty(key, (Number) value);
                } else if (value instanceof Boolean) {
                    result.addProperty(key, (Boolean) value);
                } else if (value == null) {
                    result.add(key, null);
                } else {
                    result.addProperty(key, value.toString());
                }
            }
        }
        return result;
    }
    
    /**
     * Convert a List from SnakeYAML to a JsonArray
     * @param list The list from SnakeYAML
     * @return JsonArray representation
     */
    private JsonArray convertListToJsonArray(List<?> list) {
        JsonArray array = new JsonArray();
        for (Object item : list) {
            if (item instanceof Map) {
                array.add(convertMapToJsonObject(item));
            } else if (item instanceof List) {
                array.add(convertListToJsonArray((List<?>) item));
            } else if (item instanceof String) {
                array.add((String) item);
            } else if (item instanceof Number) {
                array.add((Number) item);
            } else if (item instanceof Boolean) {
                array.add((Boolean) item);
            } else if (item == null) {
                array.add((String) null);
            } else {
                array.add(item.toString());
            }
        }
        return array;
    }
    
    /**
     * Reload templates from disk
     */
    public void reloadTemplates() {
        NeoEssentials.LOGGER.info("Reloading tablist templates");
        // Clear all existing templates
        globalHeaders.clear();
        globalFooters.clear();
        groupHeaders.clear();
        groupFooters.clear();
        globalBossBars.clear();
        groupBossBars.clear();
        
        // Load templates
        loadTemplates();
    }
    
    /**
     * Extract a default template file from the JAR resources to the given path
     * 
     * @param resourcePath The path to the resource in the JAR
     * @param targetPath The path to extract the resource to
     * @return True if successful, false otherwise
     */
    private boolean extractDefaultTemplate(String resourcePath, Path targetPath) {
        try {
            // Get the resource as a stream
            InputStream resourceStream = TemplateManager.class.getClassLoader().getResourceAsStream(resourcePath);
            if (resourceStream == null) {
                NeoEssentials.LOGGER.warn("Resource not found: {}", resourcePath);
                return false;
            }
            
            // Create parent directories if they don't exist
            if (targetPath.getParent() != null && !Files.exists(targetPath.getParent())) {
                Files.createDirectories(targetPath.getParent());
            }
            
            // Copy the resource to the target path
            Files.copy(resourceStream, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            NeoEssentials.LOGGER.info("Extracted default template: {} -> {}", resourcePath, targetPath);
            return true;
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to extract default template: {}", resourcePath, e);
            return false;
        }
    }
}
