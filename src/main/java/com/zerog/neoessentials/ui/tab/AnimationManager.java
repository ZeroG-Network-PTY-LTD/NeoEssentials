package com.zerog.neoessentials.ui.tab;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages all animations for the TabManager system
 */
public class AnimationManager {
    // Animation type constants
    public enum AnimationType {
        NONE("none"),
        ROTATION("rotation"),
        SCROLL("scroll"),
        FADE("fade"),
        RAINBOW("rainbow"),
        TYPEWRITER("typewriter"),
        BLINK("blink"),
        WAVE("wave"),
        GRADIENT("gradient"),
        PULSE("pulse"),
        HEX_CUSTOM("hex_custom");
        
        private final String configValue;
        
        AnimationType(String configValue) {
            this.configValue = configValue;
        }
        
        public String getConfigValue() {
            return configValue;
        }
        
        public static AnimationType fromString(String value) {
            for (AnimationType type : values()) {
                if (type.configValue.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return NONE;
        }
    }
    
    // Animation processor interface
    public interface AnimationProcessor {
        String processFrame(List<String> templates, ServerPlayer player, int frame);
    }
    
    // Animation frames tracking
    private final AtomicInteger globalFrame = new AtomicInteger(0);
    private final Map<String, Map<ServerPlayer, Integer>> playerFrames = new ConcurrentHashMap<>();
    
    // Animation processors by type
    private final Map<AnimationType, AnimationProcessor> animationProcessors = new HashMap<>();
      // Custom hex animations loaded from config
    private final Map<String, CustomHexAnimation> customHexAnimations = new HashMap<>();/**
     * Initializes animation system
     */
    public void initialize() {
        // Register all animation processors
        registerAnimationProcessors();
        
        // Load custom animations
        loadCustomHexAnimations();
        
        NeoEssentials.LOGGER.info("AnimationManager initialized with {} animation types", animationProcessors.size());
    }
    
    /**
     * Registers all animation processors
     */
    private void registerAnimationProcessors() {
        animationProcessors.put(AnimationType.NONE, new NoAnimationProcessor());
        animationProcessors.put(AnimationType.ROTATION, new RotationAnimationProcessor());
        animationProcessors.put(AnimationType.SCROLL, new ScrollAnimationProcessor());
        animationProcessors.put(AnimationType.FADE, new FadeAnimationProcessor());
        animationProcessors.put(AnimationType.RAINBOW, new RainbowAnimationProcessor());
        animationProcessors.put(AnimationType.TYPEWRITER, new TypewriterAnimationProcessor());
        animationProcessors.put(AnimationType.BLINK, new BlinkAnimationProcessor());
        animationProcessors.put(AnimationType.WAVE, new WaveAnimationProcessor());
        animationProcessors.put(AnimationType.GRADIENT, new GradientAnimationProcessor());
        animationProcessors.put(AnimationType.PULSE, new PulseAnimationProcessor());
        animationProcessors.put(AnimationType.HEX_CUSTOM, new HexCustomAnimationProcessor());
    }
    
    /**
     * Updates animation frames
     */
    public void update() {
        // Increment global frame
        globalFrame.incrementAndGet();
    }
    
    /**
     * Processes an animation frame for a player
     * 
     * @param type Animation type to use
     * @param templates Text templates to use
     * @param player Player to process for
     * @return Processed animation text
     */
    public String processAnimation(AnimationType type, List<String> templates, ServerPlayer player) {
        if (templates == null || templates.isEmpty()) {
            return "";
        }
        
        AnimationProcessor processor = animationProcessors.get(type);
        if (processor == null) {
            return templates.get(0); // Default to first template if no processor
        }
        
        // Get or create player-specific frame counter
        String key = type.name();
        Map<ServerPlayer, Integer> typeFrames = playerFrames.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        int frame = typeFrames.computeIfAbsent(player, p -> globalFrame.get());
        
        // Process the frame
        String result = processor.processFrame(templates, player, frame);
        
        // Increment player-specific frame
        typeFrames.put(player, frame + 1);
        
        return result;
    }
    
    /**
     * Gets frame for animation type and player
     */
    public int getPlayerFrame(AnimationType type, ServerPlayer player) {
        String key = type.name();
        Map<ServerPlayer, Integer> typeFrames = playerFrames.get(key);
        if (typeFrames == null) return 0;
        return typeFrames.getOrDefault(player, 0);
    }
      /**
     * Loads custom hex color animations from config file
     */
    private void loadCustomHexAnimations() {
        // Clear existing cache
        customHexAnimations.clear();
          // Paths to check (in order of preference)
        Path neoEssentialsDir = Paths.get("neoessentials");
        Path configDir = Paths.get("config", "neoessentials");
        
        // First check neoessentials directory for JSON file (preferred)
        Path neoJsonPath = neoEssentialsDir.resolve("animations.json");
        Path neoYmlPath = neoEssentialsDir.resolve("animations.yml");
        Path configJsonPath = configDir.resolve("animations.json");
        Path configTomlPath = configDir.resolve("animations.toml");
        
        // Check in priority order:
        // 1. neoessentials/animations.json
        // 2. neoessentials/animations.yml
        // 3. config/neoessentials/animations.json
        // 4. config/neoessentials/animations.toml (legacy)
        
        if (Files.exists(neoJsonPath)) {
            loadFromJson(neoJsonPath);
            NeoEssentials.LOGGER.info("Loaded animations from neoessentials/animations.json");
        }
        else if (Files.exists(neoYmlPath)) {
            try {
                loadFromYaml(neoYmlPath);
                NeoEssentials.LOGGER.info("Loaded animations from neoessentials/animations.yml");
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to load YAML animations, falling back to defaults", e);
                createDefaultAnimationFile(neoEssentialsDir);
            }
        }
        else if (Files.exists(configJsonPath)) {
            loadFromJson(configJsonPath);
            NeoEssentials.LOGGER.info("Loaded animations from config/neoessentials/animations.json");
        }
        else if (Files.exists(configTomlPath)) {
            loadFromToml(configTomlPath);
            
            // Convert TOML to JSON for future use
            NeoEssentials.LOGGER.info("Found legacy TOML animations, converting to JSON format");
            
            // Create directories if needed
            try {
                if (!Files.exists(neoEssentialsDir)) {
                    Files.createDirectories(neoEssentialsDir);
                }
                
                // Convert existing animations to JSON format
                convertTomlToJson(configTomlPath, neoJsonPath);
            } catch (IOException e) {
                NeoEssentials.LOGGER.error("Failed to create neoessentials directory", e);
            }
        }
        else {
            // No existing file found, create default JSON file
            createDefaultAnimationFile(neoEssentialsDir);
        }
        
        // Ensure we have a default animation
        if (!customHexAnimations.containsKey("default")) {
            List<String> defaultTexts = new ArrayList<>();
            defaultTexts.add("&#54C5EAE&#54DAF4x&#54C5EAa&#54B1DFm&#549CD5p&#5487CBl&#5473C0e");
            defaultTexts.add("&#54B1DFE&#54C5EAx&#54DAF4a&#54C5EAm&#54B1DFp&#549CD5l&#5487CBe");
            customHexAnimations.put("default", new CustomHexAnimation(50, defaultTexts));
        }        
        NeoEssentials.LOGGER.info("Loaded {} custom hex animations", customHexAnimations.size());
        
        // Create a README file in the config directory to explain the new location
        try {
            Path readmePath = configDir.resolve("README_ANIMATIONS.md");
            String readmeContent = "# NeoEssentials Animations\n\n" +
                    "The animations for the tablist system have been moved to the `neoessentials/animations.json` file.\n" +
                    "This provides better configuration flexibility and avoids TOML serialization issues.\n\n" +
                    "## Location\n\n" +
                    "- Primary location: `neoessentials/animations.json`\n" +
                    "- Legacy location (no longer recommended): `config/neoessentials/animations.toml`\n\n" +
                    "## Format\n\n" +
                    "The animations file now uses JSON format for maximum compatibility and flexibility.";
            
            if (!Files.exists(readmePath)) {
                Files.writeString(readmePath, readmeContent);
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.debug("Could not create README_ANIMATIONS.md file", e);
        }
    }
      /**
     * Loads animations from TOML file - simplified parser
     */
    private void loadFromToml(Path path) {
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            
            // Simple TOML parser - just enough to handle our animations format
            Map<String, Map<String, Object>> sections = new HashMap<>();
            String currentSection = null;
            Map<String, Object> currentValues = null;
            
            // Parse line by line
            for (String line : content.split("\\r?\\n")) {
                line = line.trim();
                
                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Handle section headers [section_name]
                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line.substring(1, line.length() - 1);
                    currentValues = new HashMap<>();
                    sections.put(currentSection, currentValues);
                    continue;
                }
                
                // Handle key-value pairs
                if (line.contains("=") && currentValues != null) {
                    String[] parts = line.split("=", 2);
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    
                    // Handle change-interval number
                    if (key.equals("change-interval")) {
                        try {
                            int intVal = Integer.parseInt(value);
                            currentValues.put(key, intVal);
                        } catch (NumberFormatException e) {
                            currentValues.put(key, 50); // Default
                        }
                    }
                    // Handle texts array
                    else if (key.equals("texts") && value.startsWith("[") && value.endsWith("]")) {
                        String arrayContent = value.substring(1, value.length() - 1);
                        List<String> texts = new ArrayList<>();
                        
                        // Very basic CSV parsing - assumes properly quoted strings
                        boolean inQuotes = false;
                        StringBuilder currentText = new StringBuilder();
                        
                        for (int i = 0; i < arrayContent.length(); i++) {
                            char c = arrayContent.charAt(i);
                            
                            if (c == '"') {
                                inQuotes = !inQuotes;
                                if (!inQuotes) {
                                    // End of quoted string
                                    texts.add(currentText.toString());
                                    currentText = new StringBuilder();
                                }
                            } else if (c == ',' && !inQuotes) {
                                // Skip comma separators
                                continue;
                            } else if (inQuotes) {
                                currentText.append(c);
                            }
                        }
                        
                        currentValues.put(key, texts);
                    }
                }
            }
            
            // Create animations from parsed sections
            for (Map.Entry<String, Map<String, Object>> entry : sections.entrySet()) {
                String name = entry.getKey();
                Map<String, Object> data = entry.getValue();
                
                // Get change interval
                int changeInterval = 50; // Default
                if (data.containsKey("change-interval")) {
                    Object interval = data.get("change-interval");
                    if (interval instanceof Integer) {
                        changeInterval = (Integer) interval;
                    }
                }
                
                // Get texts
                List<String> texts = new ArrayList<>();
                if (data.containsKey("texts") && data.get("texts") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> textList = (List<String>) data.get("texts");
                    texts.addAll(textList);
                }
                
                if (!texts.isEmpty()) {
                    customHexAnimations.put(name, new CustomHexAnimation(changeInterval, texts));
                }
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to load animations.toml", e);
        }
    }
      /**
     * Loads animations from JSON file
     */
    private void loadFromJson(Path path) {
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                String name = entry.getKey();
                if (entry.getValue().isJsonObject()) {
                    JsonObject animData = entry.getValue().getAsJsonObject();
                    
                    // Get change interval
                    int changeInterval = 50;
                    if (animData.has("change-interval")) {
                        changeInterval = animData.get("change-interval").getAsInt();
                    }
                    
                    // Get animation texts
                    if (animData.has("texts") && animData.get("texts").isJsonArray()) {
                        JsonArray textsArray = animData.get("texts").getAsJsonArray();
                        List<String> texts = new ArrayList<>();
                        
                        for (JsonElement textElem : textsArray) {
                            if (textElem.isJsonPrimitive()) {
                                texts.add(textElem.getAsString());
                            }
                        }
                        
                        if (!texts.isEmpty()) {
                            customHexAnimations.put(name, new CustomHexAnimation(changeInterval, texts));
                        }
                    }
                }
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to load animations.json", e);
        }
    }
    
    /**
     * Loads animations from YAML file
     * 
     * @param path Path to the YAML file
     */
    private void loadFromYaml(Path path) {
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            Map<String, Object> yamlRoot = parseYaml(content);
            
            // Check if animations are in root or under "animations" key
            Map<String, Object> animations = yamlRoot;
            if (yamlRoot.containsKey("animations") && yamlRoot.get("animations") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> animationsMap = (Map<String, Object>) yamlRoot.get("animations");
                animations = animationsMap;
            }
            
            // Process each animation
            for (Map.Entry<String, Object> entry : animations.entrySet()) {
                String name = entry.getKey();
                
                // Skip metadata section if present
                if (name.equals("metadata")) continue;
                
                if (entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> animData = (Map<String, Object>) entry.getValue();
                    
                    // Get change interval
                    int changeInterval = 50; // Default
                    if (animData.containsKey("change-interval")) {
                        Object intervalValue = animData.get("change-interval");
                        if (intervalValue instanceof Integer) {
                            changeInterval = (Integer) intervalValue;
                        } else if (intervalValue instanceof String) {
                            try {
                                changeInterval = Integer.parseInt((String) intervalValue);
                            } catch (NumberFormatException e) {
                                // Keep default
                            }
                        }
                    }
                    
                    // Get animation texts
                    if (animData.containsKey("texts")) {
                        Object textsObj = animData.get("texts");
                        List<String> texts = new ArrayList<>();
                        
                        if (textsObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Object> textList = (List<Object>) textsObj;
                            
                            for (Object text : textList) {
                                if (text != null) {
                                    texts.add(text.toString());
                                }
                            }
                            
                            if (!texts.isEmpty()) {
                                customHexAnimations.put(name, new CustomHexAnimation(changeInterval, texts));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to load animations.yml", e);
        }
    }
    
    /**
     * Simple YAML parser for animations file
     * Supports basic YAML structure needed for animations
     * 
     * @param content YAML content as string
     * @return Map representing the parsed YAML
     */
    private Map<String, Object> parseYaml(String content) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> currentMap = result;
        List<Object> currentList = null;
        int currentIndent = 0;
        Map<Integer, Object> indentMap = new HashMap<>();
        indentMap.put(0, result);
        
        Pattern listItemPattern = Pattern.compile("^(\\s*)-\\s+(.*)$");
        Pattern keyValuePattern = Pattern.compile("^(\\s*)([^:]+):\\s*(.*)$");
        
        String[] lines = content.split("\\r?\\n");
        for (String rawLine : lines) {
            String line = rawLine;
            
            // Skip comments and empty lines
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }
            
            // Handle list items (- item)
            Matcher listMatcher = listItemPattern.matcher(line);
            if (listMatcher.matches()) {
                int indent = listMatcher.group(1).length();
                String value = listMatcher.group(2).trim();
                
                // Get or create list at this indent level
                if (indent > currentIndent) {
                    // New nested list
                    currentList = new ArrayList<>();
                    Object parent = indentMap.get(currentIndent);
                    if (parent instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parentMap = (Map<String, Object>) parent;
                        parentMap.put(getLastKey(parentMap), currentList);
                    }
                    indentMap.put(indent, currentList);
                } else if (indentMap.get(indent) instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) indentMap.get(indent);
                    currentList = list;
                }
                
                // Add the value to the list
                if (currentList != null) {
                    // Handle quoted strings
                    if ((value.startsWith("\"") && value.endsWith("\"")) || 
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    currentList.add(value);
                }
                
                currentIndent = indent;
                continue;
            }
            
            // Handle key-value pairs (key: value)
            Matcher keyValMatcher = keyValuePattern.matcher(line);
            if (keyValMatcher.matches()) {
                int indent = keyValMatcher.group(1).length();
                String key = keyValMatcher.group(2).trim();
                String value = keyValMatcher.group(3).trim();
                
                // Get the map at this indent level
                if (indent < currentIndent) {
                    if (indentMap.containsKey(indent) && indentMap.get(indent) instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> indentedMap = (Map<String, Object>) indentMap.get(indent);
                        currentMap = indentedMap;
                    }
                } else if (indent > currentIndent) {
                    // New nested map
                    Map<String, Object> nestedMap = new HashMap<>();
                    if (indentMap.get(currentIndent) instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parentMap = (Map<String, Object>) indentMap.get(currentIndent);
                        parentMap.put(getLastKey(parentMap), nestedMap);
                    }
                    currentMap = nestedMap;
                    indentMap.put(indent, nestedMap);
                }
                
                // Process the value
                if (value.isEmpty()) {
                    // Empty value could indicate a nested structure coming up
                    Map<String, Object> nestedMap = new HashMap<>();
                    currentMap.put(key, nestedMap);
                    indentMap.put(indent + 2, nestedMap); // Assume 2-space indentation
                } else {
                    // Handle quoted strings
                    if ((value.startsWith("\"") && value.endsWith("\"")) || 
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                        currentMap.put(key, value);
                    }
                    // Handle numbers
                    else if (value.matches("^-?\\d+$")) {
                        currentMap.put(key, Integer.parseInt(value));
                    }
                    // Handle booleans
                    else if (value.equalsIgnoreCase("true")) {
                        currentMap.put(key, Boolean.TRUE);
                    }
                    else if (value.equalsIgnoreCase("false")) {
                        currentMap.put(key, Boolean.FALSE);
                    }
                    // Handle lists [...] - simple arrays
                    else if (value.startsWith("[") && value.endsWith("]")) {
                        List<String> items = new ArrayList<>();
                        String listContent = value.substring(1, value.length() - 1);
                        
                        // Split by comma but respect quotes
                        boolean inQuotes = false;
                        char quoteChar = '"';
                        StringBuilder currentItem = new StringBuilder();
                        
                        for (int i = 0; i < listContent.length(); i++) {
                            char c = listContent.charAt(i);
                            
                            if ((c == '"' || c == '\'') && (i == 0 || listContent.charAt(i - 1) != '\\')) {
                                if (!inQuotes) {
                                    inQuotes = true;
                                    quoteChar = c;
                                } else if (c == quoteChar) {
                                    inQuotes = false;
                                    items.add(currentItem.toString());
                                    currentItem = new StringBuilder();
                                } else {
                                    currentItem.append(c);
                                }
                            } else if (c == ',' && !inQuotes) {
                                String item = currentItem.toString().trim();
                                if (!item.isEmpty()) {
                                    items.add(item);
                                }
                                currentItem = new StringBuilder();
                            } else {
                                currentItem.append(c);
                            }
                        }
                        
                        // Add the last item if any
                        String item = currentItem.toString().trim();
                        if (!item.isEmpty()) {
                            items.add(item);
                        }
                        
                        currentMap.put(key, items);
                    }
                    // Default to string
                    else {
                        currentMap.put(key, value);
                    }
                }
                
                currentIndent = indent;
                currentList = null;
            }
        }
        
        return result;
    }
    
    /**
     * Helper method to get the last used key in a map
     */
    private String getLastKey(Map<String, Object> map) {
        String lastKey = null;
        for (String key : map.keySet()) {
            lastKey = key;
        }
        return lastKey;
    }
      /**
     * Creates default animation file using JSON format
     * 
     * @param baseDir The directory to create the file in
     */    private void createDefaultAnimationFile(Path baseDir) {
        Path path = baseDir.resolve("animations.json");
        
        try {
            // Create directory if needed
            Files.createDirectories(baseDir);
            
            // Try to load the default animations from resources
            InputStream inputStream = AnimationManager.class.getClassLoader()
                    .getResourceAsStream("default-neoessentials/animations.json");
            
            if (inputStream != null) {
                // Copy the default animations file from resources
                Files.copy(inputStream, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                inputStream.close();
                
                NeoEssentials.LOGGER.info("Created default animations.json in {} from embedded resources", baseDir);
            } else {
                // Fallback - create a basic JSON structure manually
                NeoEssentials.LOGGER.warn("Could not find default animations.json in resources, creating basic version");
                
                JsonObject root = new JsonObject();
                
                // Add metadata
                JsonObject metadata = new JsonObject();
                metadata.addProperty("version", "1.0.0");
                metadata.addProperty("description", "NeoEssentials Tablist Animations");
                metadata.addProperty("created", java.time.LocalDateTime.now().toString());
                root.add("metadata", metadata);
                
                // Add animations
                JsonObject animations = new JsonObject();
                
                // Default animation
                JsonObject defaultAnim = new JsonObject();
                defaultAnim.addProperty("change-interval", 50);
                JsonArray defaultTexts = new JsonArray();
                defaultTexts.add("&#54C5EAE&#54DAF4x&#54C5EAa&#54B1DFm&#549CD5p&#5487CBl&#5473C0e");
                defaultTexts.add("&#54B1DFE&#54C5EAx&#54DAF4a&#54C5EAm&#54B1DFp&#549CD5l&#5487CBe");
                defaultAnim.add("texts", defaultTexts);
                animations.add("default", defaultAnim);
                
                // Add animations to root
                root.add("animations", animations);
                
                // Write the JSON to file with pretty printing
                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                Files.writeString(path, gson.toJson(root));
                
                NeoEssentials.LOGGER.info("Created basic default animations.json in {}", baseDir);
            }
            
            // Create a README file in the config directory explaining the location change
            Path configDir = Paths.get("config", "neoessentials");
            if (Files.exists(configDir)) {
                Path readmePath = configDir.resolve("README_ANIMATIONS.md");
                String readmeContent = "# NeoEssentials Animations\n\n" +
                        "The animations for the tablist system have been moved to the `neoessentials/animations.json` file.\n" +
                        "This provides better configuration flexibility and avoids TOML serialization issues.\n\n" +
                        "## Location\n\n" +
                        "- Primary location: `neoessentials/animations.json`\n" +
                        "- Legacy location (no longer recommended): `config/neoessentials/animations.json`\n\n" +
                        "## Format\n\n" +
                        "The animations file now uses JSON format for maximum compatibility and flexibility.\n\n" +
                        "## Migration\n\n" +
                        "Your existing animations have been automatically migrated to JSON format.\n"+
                        "A backup of your old TOML file has been created with the .bak extension.";
                
                if (!Files.exists(readmePath)) {
                    Files.writeString(readmePath, readmeContent);
                }
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to create default animations.json", e);
        }
    }
      /**
     * Converts a TOML animations file to JSON format
     * 
     * @param tomlPath Path to the source TOML file
     * @param jsonPath Path to the target JSON file
     */
    private void convertTomlToJson(Path tomlPath, Path jsonPath) {
        try {
            // Load the TOML file using our simple parser
            String tomlContent = new String(Files.readAllBytes(tomlPath), StandardCharsets.UTF_8);
            
            // Parse TOML content with our custom parser
            Map<String, Map<String, Object>> parsedData = new HashMap<>();
            String currentSection = null;
            Map<String, Object> currentValues = null;
            
            // Parse line by line
            for (String line : tomlContent.split("\\r?\\n")) {
                line = line.trim();
                
                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Handle section headers [section_name]
                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line.substring(1, line.length() - 1);
                    currentValues = new HashMap<>();
                    parsedData.put(currentSection, currentValues);
                    continue;
                }
                
                // Handle key-value pairs
                if (line.contains("=") && currentValues != null) {
                    String[] parts = line.split("=", 2);
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    
                    // Handle change-interval number
                    if (key.equals("change-interval")) {
                        try {
                            int intVal = Integer.parseInt(value);
                            currentValues.put(key, intVal);
                        } catch (NumberFormatException e) {
                            currentValues.put(key, 50); // Default
                        }
                    }
                    // Handle texts array
                    else if (key.equals("texts") && value.startsWith("[") && value.endsWith("]")) {
                        String arrayContent = value.substring(1, value.length() - 1);
                        List<String> texts = new ArrayList<>();
                        
                        // Very basic CSV parsing - assumes properly quoted strings
                        boolean inQuotes = false;
                        StringBuilder currentText = new StringBuilder();
                        
                        for (int i = 0; i < arrayContent.length(); i++) {
                            char c = arrayContent.charAt(i);
                            
                            if (c == '"') {
                                inQuotes = !inQuotes;
                                if (!inQuotes) {
                                    // End of quoted string
                                    texts.add(currentText.toString());
                                    currentText = new StringBuilder();
                                }
                            } else if (c == ',' && !inQuotes) {
                                // Skip comma separators
                                continue;
                            } else if (inQuotes) {
                                currentText.append(c);
                            }
                        }
                        
                        currentValues.put(key, texts);
                    }
                }
            }
            
            // Create JSON structure
            JsonObject root = new JsonObject();
            
            // Add metadata
            JsonObject metadata = new JsonObject();
            metadata.addProperty("version", "1.0.0");
            metadata.addProperty("description", "Converted from TOML to JSON format");
            metadata.addProperty("converted", java.time.LocalDateTime.now().toString());
            root.add("metadata", metadata);
            
            // Add animations
            JsonObject animations = new JsonObject();
            
            // Convert each animation section to JSON
            for (Map.Entry<String, Map<String, Object>> entry : parsedData.entrySet()) {
                String name = entry.getKey();
                Map<String, Object> animData = entry.getValue();
                JsonObject jsonAnim = new JsonObject();
                
                // Convert interval
                Object intervalObj = animData.get("change-interval");
                if (intervalObj instanceof Integer) {
                    jsonAnim.addProperty("change-interval", (Integer) intervalObj);
                } else {
                    jsonAnim.addProperty("change-interval", 50); // Default value
                }
                
                // Convert text list
                Object textsObj = animData.get("texts");
                if (textsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> texts = (List<String>) textsObj;
                    if (!texts.isEmpty()) {
                        JsonArray jsonTexts = new JsonArray();
                        for (String text : texts) {
                            jsonTexts.add(text);
                        }
                        jsonAnim.add("texts", jsonTexts);
                    }
                }
                
                animations.add(name, jsonAnim);
            }
            
            root.add("animations", animations);
            
            // Write the JSON to file with pretty printing
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            Files.writeString(jsonPath, gson.toJson(root));
            
            NeoEssentials.LOGGER.info("Successfully converted animations from TOML to JSON: {}", jsonPath);
            
            // Create a backup of the original TOML file
            Path backupPath = tomlPath.resolveSibling(tomlPath.getFileName().toString() + ".bak");
            Files.copy(tomlPath, backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            NeoEssentials.LOGGER.info("Created backup of original TOML file: {}", backupPath);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to convert TOML animations to JSON", e);
        }
    }
    
    /**
     * Gets a custom hex animation by name
     * 
     * @param name The animation name
     * @return The animation or null if not found
     */
    public CustomHexAnimation getCustomHexAnimation(String name) {
        return customHexAnimations.get(name);
    }
    
    /**
     * Process any animation tags in a text string
     * 
     * @param text The text to process for animations
     * @return The processed text with animations applied
     */
    public String processAnimations(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Process animation tags: {animation:name}text to animate{/animation}
        Pattern pattern = Pattern.compile("\\{animation:([a-z_]+)\\}(.*?)\\{/animation\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String animationName = matcher.group(1);
            String content = matcher.group(2);
            
            // Process the animation based on type
            AnimationType type = AnimationType.fromString(animationName);
            if (type == AnimationType.NONE) {
                // Not a standard animation, check for custom hex animation
                if (customHexAnimations.containsKey(animationName)) {
                    CustomHexAnimation animation = customHexAnimations.get(animationName);
                    List<String> frames = animation.getFrames();
                    
                    // Use the global frame for animation without player context
                    int frame = globalFrame.get() % frames.size();
                    content = frames.get(frame);
                } 
                // No replacement if not found
                matcher.appendReplacement(result, Matcher.quoteReplacement(content));
            } else {
                // Handle standard animations that require a player
                // For these global animations without player context, we use a simpler approach
                List<String> templates = new ArrayList<>();
                templates.add(content);
                
                // Use the animation processor with null player
                AnimationProcessor processor = animationProcessors.get(type);
                if (processor != null) {
                    String processed = processor.processFrame(templates, null, globalFrame.get());
                    matcher.appendReplacement(result, Matcher.quoteReplacement(processed));
                } else {
                    matcher.appendReplacement(result, Matcher.quoteReplacement(content));
                }
            }
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * Storage class for custom hex animations
     */    public static class CustomHexAnimation {
        private final int changeInterval;
        private final List<String> texts;
        
        public CustomHexAnimation(int changeInterval, List<String> texts) {
            this.changeInterval = changeInterval;
            this.texts = new ArrayList<>(texts);
        }
        
        public int getChangeInterval() {
            return changeInterval;
        }
        
        /**
         * Get the animation texts/frames
         * @return The list of animation frames
         */
        public List<String> getTexts() {
            return texts;
        }
        
        /**
         * Get the animation frames (alias for getTexts)
         * @return The list of animation frames
         */
        public List<String> getFrames() {
            return texts;
        }
    }
    
    /**
     * No animation - just returns the first template
     */
    private static class NoAnimationProcessor implements AnimationProcessor {
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            return templates.get(0);
        }
    }
    
    /**
     * Rotation animation - cycles through templates
     */
    private static class RotationAnimationProcessor implements AnimationProcessor {
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            return templates.get(frame % templates.size());
        }
    }
    
    /**
     * Scroll animation - scrolls text horizontally
     */
    private static class ScrollAnimationProcessor implements AnimationProcessor {
        private static final int DEFAULT_SCROLL_WIDTH = 20;
        private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]|&#[0-9A-F]{6}");
        
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) return "";
            
            String template = templates.get(frame % templates.size());
            
            // Strip color codes for calculation
            String plainText = stripColor(template);
            
            // If text is too short, don't scroll
            if (plainText.length() <= DEFAULT_SCROLL_WIDTH) {
                return template;
            }
            
            // Calculate scroll position
            int scrollPos = frame % (plainText.length() + DEFAULT_SCROLL_WIDTH);
            
            // Create scrolled text
            String scrollText = plainText + " " + plainText;
            String visiblePortion = scrollText.substring(
                Math.min(scrollPos, scrollText.length() - 1),
                Math.min(scrollPos + DEFAULT_SCROLL_WIDTH, scrollText.length())
            );
            
            // Preserve first color code from original
            Matcher matcher = COLOR_PATTERN.matcher(template);
            String firstColor = "";
            if (matcher.find()) {
                firstColor = matcher.group();
            }
            
            return firstColor + visiblePortion;
        }
        
        private String stripColor(String text) {
            return COLOR_PATTERN.matcher(text).replaceAll("");
        }
    }
    
    /**
     * Fade animation - fades between colors
     */
    private static class FadeAnimationProcessor implements AnimationProcessor {
        private static final ChatFormatting[] FADE_COLORS = {
            ChatFormatting.WHITE,
            ChatFormatting.YELLOW,
            ChatFormatting.GOLD,
            ChatFormatting.RED,
            ChatFormatting.DARK_RED,
            ChatFormatting.DARK_PURPLE,
            ChatFormatting.LIGHT_PURPLE,
            ChatFormatting.AQUA,
            ChatFormatting.GREEN,
            ChatFormatting.BLUE
        };
        
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) return "";
            
            String template = templates.get(frame % templates.size());
            String plainText = template.replaceAll("(?i)§[0-9A-FK-OR]|&#[0-9A-F]{6}", "");
            
            ChatFormatting color = FADE_COLORS[frame % FADE_COLORS.length];
            return color + plainText;
        }
    }
    
    /**
     * Rainbow animation - applies rainbow colors
     */
    private static class RainbowAnimationProcessor implements AnimationProcessor {
        private static final ChatFormatting[] RAINBOW_COLORS = {
            ChatFormatting.RED,
            ChatFormatting.GOLD,
            ChatFormatting.YELLOW,
            ChatFormatting.GREEN,
            ChatFormatting.AQUA,
            ChatFormatting.BLUE,
            ChatFormatting.LIGHT_PURPLE
        };
        
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) return "";
            
            StringBuilder result = new StringBuilder();
            String baseText = templates.get(frame % templates.size())
                .replaceAll("(?i)§[0-9A-FK-OR]|&#[0-9A-F]{6}", "");
            
            // Apply rainbow effect, shifting colors by frame
            for (int i = 0; i < baseText.length(); i++) {
                int colorIndex = (i + frame) % RAINBOW_COLORS.length;
                result.append(RAINBOW_COLORS[colorIndex]).append(baseText.charAt(i));
            }
            
            return result.toString();
        }
    }
    
    /**
     * Typewriter animation - types text character by character
     */
    private static class TypewriterAnimationProcessor implements AnimationProcessor {
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) return "";
            
            String template = templates.get(frame / 30 % templates.size());
            String plainText = template.replaceAll("(?i)§[0-9A-FK-OR]|&#[0-9A-F]{6}", "");
            
            // Calculate how many characters to show
            int visibleChars = frame % (plainText.length() + 15);
            if (visibleChars > plainText.length()) {
                visibleChars = plainText.length();
            }
            
            // Get visible portion
            String visibleText = plainText.substring(0, visibleChars);
            
            // Preserve first color code from original
            Matcher matcher = Pattern.compile("(?i)§[0-9A-FK-OR]|&#[0-9A-F]{6}").matcher(template);
            String firstColor = "";
            if (matcher.find()) {
                firstColor = matcher.group();
            }
            
            return firstColor + visibleText;
        }
    }
    
    /**
     * Blink animation - text blinks on and off
     */
    private static class BlinkAnimationProcessor implements AnimationProcessor {
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) return "";
            
            // Show text for 15 frames, hide for 10
            boolean visible = (frame % 25) < 15;
            if (!visible) return "";
            
            return templates.get(frame / 15 % templates.size());
        }
    }
    
    /**
     * Wave animation - applies wave effect to text
     */
    private static class WaveAnimationProcessor implements AnimationProcessor {
        private static final ChatFormatting[] WAVE_COLORS = {
            ChatFormatting.AQUA,
            ChatFormatting.BLUE,
            ChatFormatting.DARK_AQUA,
            ChatFormatting.BLUE,
            ChatFormatting.AQUA,
            ChatFormatting.WHITE
        };
        
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) return "";
            
            String template = templates.get(frame / 15 % templates.size());
            String plainText = template.replaceAll("(?i)§[0-9A-FK-OR]|&#[0-9A-F]{6}", "");
            
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < plainText.length(); i++) {
                int wavePos = (frame + i * 2) % WAVE_COLORS.length;
                result.append(WAVE_COLORS[wavePos]).append(plainText.charAt(i));
            }
            
            return result.toString();
        }
    }
    
    /**
     * Gradient animation - shifts colors like a gradient
     */
    private static class GradientAnimationProcessor implements AnimationProcessor {
        private static final ChatFormatting[] GRADIENT_COLORS = {
            ChatFormatting.RED,
            ChatFormatting.GOLD,
            ChatFormatting.YELLOW,
            ChatFormatting.GREEN,
            ChatFormatting.AQUA,
            ChatFormatting.BLUE,
            ChatFormatting.LIGHT_PURPLE
        };
        
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) return "";
            
            String template = templates.get(frame % templates.size());
            String plainText = template.replaceAll("(?i)§[0-9A-FK-OR]|&#[0-9A-F]{6}", "");
            
            // Calculate starting color based on frame
            int startColorIndex = frame % GRADIENT_COLORS.length;
            
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < plainText.length(); i++) {
                int colorIndex = (startColorIndex + i) % GRADIENT_COLORS.length;
                result.append(GRADIENT_COLORS[colorIndex]).append(plainText.charAt(i));
            }
            
            return result.toString();
        }
    }
    
    /**
     * Pulse animation - text pulses between two colors
     */
    private static class PulseAnimationProcessor implements AnimationProcessor {
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) return "";
            
            String template = templates.get(frame / 10 % templates.size());
            String plainText = template.replaceAll("(?i)§[0-9A-FK-OR]|&#[0-9A-F]{6}", "");
            
            // Determine pulse state (primary or secondary color)
            boolean isPrimary = (frame % 10) < 5;
            ChatFormatting color = isPrimary ? ChatFormatting.GOLD : ChatFormatting.YELLOW;
            
            return color + plainText;
        }
    }
      /**
     * Custom hex color animation - uses hex color animations from config file
     */
    private class HexCustomAnimationProcessor implements AnimationProcessor {
        private final Pattern HEX_PATTERN = Pattern.compile("&#[0-9A-Fa-f]{6}");
        
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) return "";
            
            // Check if template specifies animation name
            String template = templates.get(0);
            String animationName = "default";
            
            if (template.startsWith("animation:")) {
                String[] parts = template.split(":", 2);
                if (parts.length == 2) {
                    animationName = parts[1].trim();
                }
                
                // Use next template as content if available
                if (templates.size() > 1) {
                    template = templates.get(1);
                } else {
                    template = "";
                }
            }
            
            // Get animation directly from the outer class instance
            CustomHexAnimation animation = getCustomHexAnimation(animationName);
            if (animation == null) return template;
            
            // If we have a template and it's not an animation command, apply animation to it
            if (!template.isEmpty() && !template.startsWith("animation:")) {
                return applyAnimationToText(animation, template, frame);
            }
            
            // Otherwise, get the current animation frame
            int frameIndex = (frame / animation.getChangeInterval()) % animation.getTexts().size();
            return animation.getTexts().get(frameIndex);
        }
        
        private String applyAnimationToText(CustomHexAnimation animation, String template, int frame) {
            // Get current frame text for colors
            int frameIndex = (frame / animation.getChangeInterval()) % animation.getTexts().size();
            String colorSource = animation.getTexts().get(frameIndex);
            
            // Extract hex colors
            List<String> hexColors = new ArrayList<>();
            Matcher matcher = HEX_PATTERN.matcher(colorSource);
            while (matcher.find()) {
                hexColors.add(matcher.group());
            }
            
            if (hexColors.isEmpty()) return template;
            
            // Strip colors from template
            String plainText = template.replaceAll("(?i)§[0-9A-FK-OR]|&#[0-9A-F]{6}", "");
            
            // Apply colors to content
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < plainText.length(); i++) {
                int colorIndex = i % hexColors.size();
                result.append(hexColors.get(colorIndex)).append(plainText.charAt(i));
            }
            
            return result.toString();
        }
    }
}
