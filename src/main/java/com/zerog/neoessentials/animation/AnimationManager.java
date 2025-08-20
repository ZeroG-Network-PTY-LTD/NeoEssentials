package com.zerog.neoessentials.animation;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages custom animated placeholders for tablist, scoreboard, and bossbar
 */
public class AnimationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnimationManager.class);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    
    private final Map<String, Animation> animations = new ConcurrentHashMap<>();
    private final Map<String, String> placeholderMappings = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Animation>> playerAnimations = new ConcurrentHashMap<>();
    
    private boolean enableAnimations = true;
    private int maxFps = 20;
    private boolean cacheAnimations = true;
    private boolean debugMode = false;
    
    private final File configFile;
    private long lastModified = 0;
    
    public AnimationManager(File configDir) {
        this.configFile = new File(configDir, "animations.json");
        loadAnimations();
        
        // Start animation update task
        if (enableAnimations) {
            startAnimationTask();
        }
    }
    
    /**
     * Load animations from the configuration file
     */
    public void loadAnimations() {
        try {
            if (!configFile.exists()) {
                LOGGER.warn("animations.json not found, creating default configuration");
                createDefaultConfig();
                return;
            }
            
            // Check if file was modified
            if (configFile.lastModified() == lastModified) {
                return; // No changes
            }
            
            lastModified = configFile.lastModified();
            
            JsonObject root = JsonParser.parseReader(new FileReader(configFile)).getAsJsonObject();
            
            // Clear existing animations
            animations.clear();
            placeholderMappings.clear();
            
            // Load global settings
            if (root.has("global_settings")) {
                loadGlobalSettings(root.getAsJsonObject("global_settings"));
            }
            
            // Load animations
            if (root.has("animations")) {
                loadAnimationDefinitions(root.getAsJsonObject("animations"));
            }
            
            // Load placeholder mappings
            if (root.has("placeholder_mappings")) {
                loadPlaceholderMappings(root.getAsJsonObject("placeholder_mappings"));
            }
            
            LOGGER.info("Loaded {} animations with {} placeholder mappings", 
                animations.size(), placeholderMappings.size());
            
            if (debugMode) {
                LOGGER.debug("Available animations: {}", animations.keySet());
                LOGGER.debug("Placeholder mappings: {}", placeholderMappings);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to load animations configuration", e);
        }
    }
    
    private void loadGlobalSettings(JsonObject settings) {
        if (settings.has("enable_animations")) {
            enableAnimations = settings.get("enable_animations").getAsBoolean();
        }
        if (settings.has("max_fps")) {
            maxFps = Math.max(1, Math.min(60, settings.get("max_fps").getAsInt()));
        }
        if (settings.has("cache_animations")) {
            cacheAnimations = settings.get("cache_animations").getAsBoolean();
        }
        if (settings.has("debug_mode")) {
            debugMode = settings.get("debug_mode").getAsBoolean();
        }
    }
    
    private void loadAnimationDefinitions(JsonObject animationsObj) {
        for (Map.Entry<String, JsonElement> entry : animationsObj.entrySet()) {
            String name = entry.getKey();
            JsonObject animData = entry.getValue().getAsJsonObject();
            
            try {
                Animation animation = parseAnimation(name, animData);
                animations.put(name, animation);
                // Automatically register placeholder mappings for new animations
                if (!placeholderMappings.containsKey("{" + name + "}")) {
                    placeholderMappings.put("{" + name + "}", name);
                }
                if (!placeholderMappings.containsKey("%" + name + "%")) {
                    placeholderMappings.put("%" + name + "%", name);
                }
                if (debugMode) {
                    LOGGER.debug("Loaded animation '{}' with {} frames", name, animation.getFrameCount());
                }
            } catch (Exception e) {
                LOGGER.error("Failed to parse animation '{}'", name, e);
            }
        }
    }
    
    private Animation parseAnimation(String name, JsonObject data) {
        AnimationType type = AnimationType.fromConfigName(data.get("type").getAsString());
        
        List<String> frames = new ArrayList<>();
        if (data.has("frames") && data.get("frames").isJsonArray()) {
            for (JsonElement frame : data.getAsJsonArray("frames")) {
                frames.add(frame.getAsString());
            }
        }
        
        int speed = data.has("speed") ? data.get("speed").getAsInt() : 500;
        String description = data.has("description") ? data.get("description").getAsString() : "";
        
        // Extract additional properties for complex animations
        JsonObject properties = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("type") && !key.equals("frames") && !key.equals("speed") && !key.equals("description")) {
                properties.add(key, entry.getValue());
            }
        }
        
        return new Animation(name, type, frames, speed, description, properties);
    }
    
    private void loadPlaceholderMappings(JsonObject mappings) {
        for (Map.Entry<String, JsonElement> entry : mappings.entrySet()) {
            String placeholder = entry.getKey();
            String animationName = entry.getValue().getAsString();
            
            if (animations.containsKey(animationName)) {
                placeholderMappings.put(placeholder, animationName);
            } else {
                LOGGER.warn("Placeholder '{}' references unknown animation '{}'", placeholder, animationName);
            }
        }
    }
    
    private void createDefaultConfig() {
        try {
            // Ensure parent directories exist
            configFile.getParentFile().mkdirs();
            
            // Create comprehensive default animations.json
            JsonObject root = new JsonObject();
            
            // Global settings
            JsonObject globalSettings = new JsonObject();
            globalSettings.addProperty("enable_animations", true);
            globalSettings.addProperty("max_fps", 20);
            globalSettings.addProperty("cache_animations", true);
            globalSettings.addProperty("debug_mode", false);
            globalSettings.addProperty("auto_reload", true);
            root.add("global_settings", globalSettings);
            
            // Animation definitions
            JsonObject animations = new JsonObject();
            
            // Server name animation
            JsonObject serverNameAnimation = new JsonObject();
            serverNameAnimation.addProperty("type", "sequence");
            serverNameAnimation.addProperty("interval", 100);
            serverNameAnimation.addProperty("loop", true);
            JsonArray serverFrames = new JsonArray();
            serverFrames.add("§6§lNeo§e§lEssentials");
            serverFrames.add("§e§lNeo§6§lEssentials");
            serverFrames.add("§6§lN§e§le§6§lo§e§lE§6§ls§e§ls§6§le§e§ln§6§lt§e§li§6§la§e§ll§6§ls");
            serverFrames.add("§e§lNeo§6§lEssentials");
            serverNameAnimation.add("frames", serverFrames);
            animations.add("server_name", serverNameAnimation);
            
            // Player count animation
            JsonObject playerCountAnimation = new JsonObject();
            playerCountAnimation.addProperty("type", "counter");
            playerCountAnimation.addProperty("interval", 50);
            playerCountAnimation.addProperty("format", "§a{value}§7/§c{max}");
            animations.add("player_count", playerCountAnimation);
            
            // Time animation
            JsonObject timeAnimation = new JsonObject();
            timeAnimation.addProperty("type", "time");
            timeAnimation.addProperty("format", "§7{hour}:§f{minute}:§8{second}");
            timeAnimation.addProperty("interval", 20);
            animations.add("server_time", timeAnimation);
            
            // TPS animation with color coding
            JsonObject tpsAnimation = new JsonObject();
            tpsAnimation.addProperty("type", "tps");
            tpsAnimation.addProperty("interval", 40);
            JsonObject tpsColors = new JsonObject();
            tpsColors.addProperty("excellent", "§a"); // >19.5 TPS
            tpsColors.addProperty("good", "§e");      // >15 TPS  
            tpsColors.addProperty("poor", "§6");      // >10 TPS
            tpsColors.addProperty("bad", "§c");       // <=10 TPS
            tpsAnimation.add("colors", tpsColors);
            animations.add("server_tps", tpsAnimation);
            
            // Loading bar animation
            JsonObject loadingAnimation = new JsonObject();
            loadingAnimation.addProperty("type", "sequence");
            loadingAnimation.addProperty("interval", 150);
            loadingAnimation.addProperty("loop", true);
            JsonArray loadingFrames = new JsonArray();
            loadingFrames.add("§7[§c■§8■■■■■■■§7]");
            loadingFrames.add("§7[§6■■§8■■■■■■§7]");
            loadingFrames.add("§7[§e■■■§8■■■■■§7]");
            loadingFrames.add("§7[§a■■■■§8■■■■§7]");
            loadingFrames.add("§7[§b■■■■■§8■■■§7]");
            loadingFrames.add("§7[§d■■■■■■§8■■§7]");
            loadingFrames.add("§7[§5■■■■■■■§8■§7]");
            loadingFrames.add("§7[§a■■■■■■■■§8■§7]");
            loadingFrames.add("§7[§2■■■■■■■■■§7]");
            loadingAnimation.add("frames", loadingFrames);
            animations.add("loading_bar", loadingAnimation);
            
            // Rainbow text animation
            JsonObject rainbowAnimation = new JsonObject();
            rainbowAnimation.addProperty("type", "rainbow");
            rainbowAnimation.addProperty("interval", 80);
            rainbowAnimation.addProperty("text", "NeoEssentials");
            animations.add("rainbow_text", rainbowAnimation);
            
            root.add("animations", animations);
            
            // Placeholder mappings
            JsonObject placeholderMappings = new JsonObject();
            placeholderMappings.addProperty("{server_name}", "server_name");
            placeholderMappings.addProperty("{animated_server}", "server_name");
            placeholderMappings.addProperty("{players_online}", "player_count");
            placeholderMappings.addProperty("{server_time}", "server_time");
            placeholderMappings.addProperty("{server_tps}", "server_tps");
            placeholderMappings.addProperty("{loading}", "loading_bar");
            placeholderMappings.addProperty("{rainbow_server}", "rainbow_text");
            root.add("placeholder_mappings", placeholderMappings);
            
            // Write to file
            try (java.io.FileWriter writer = new java.io.FileWriter(configFile)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(root, writer);
            }
            
            LOGGER.info("Created default animations.json configuration with {} animations", animations.size());
            
        } catch (Exception e) {
            LOGGER.error("Failed to create default animations.json configuration", e);
        }
    }
    
    /**
     * Process text with animated placeholders
     */
    public String processAnimatedText(String text, ServerPlayer player) {
        if (!enableAnimations || text == null || text.isEmpty()) {
            return text;
        }
        
        String processed = text;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String fullPlaceholder = matcher.group(0); // {placeholder}
            String placeholderName = matcher.group(1); // placeholder
            
            // Check if this placeholder has an animation mapping
            String animationName = placeholderMappings.get(fullPlaceholder);
            if (animationName != null) {
                Animation animation = getPlayerAnimation(player, animationName);
                if (animation != null) {
                    String animatedValue = getAnimatedValue(animation, placeholderName, player);
                    processed = processed.replace(fullPlaceholder, animatedValue);
                }
            }
        }
        
        return processed;
    }
    
    /**
     * Get player-specific animation instance
     */
    private Animation getPlayerAnimation(ServerPlayer player, String animationName) {
        if (!cacheAnimations) {
            return animations.get(animationName);
        }
        
        UUID playerId = player.getUUID();
        Map<String, Animation> playerAnims = playerAnimations.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        
        // Clone animation for player-specific state
        Animation baseAnimation = animations.get(animationName);
        if (baseAnimation == null) return null;
        
        return playerAnims.computeIfAbsent(animationName, k -> cloneAnimation(baseAnimation));
    }
    
    private Animation cloneAnimation(Animation original) {
        return new Animation(
            original.getName(),
            original.getType(),
            new ArrayList<>(original.getFrames()),
            original.getSpeed(),
            original.getDescription(),
            original.getProperties().deepCopy()
        );
    }
    
    /**
     * Get the current animated value for a placeholder
     */
    private String getAnimatedValue(Animation animation, String context, ServerPlayer player) {
        try {
            switch (animation.getType()) {
                case HEALTH_BAR:
                    return animation.processWithContext(context, (double) player.getHealth(), (double) player.getMaxHealth());
                case WEATHER:
                    Level level = player.serverLevel();
                    boolean raining = level.isRaining();
                    boolean thundering = level.isThundering();
                    String weather = thundering ? "storm" : (raining ? "rain" : "clear");
                    return animation.processWithContext(context, weather);
                case CONDITIONAL:
                    return animation.processWithContext(context, getServerStats());
                default:
                    return animation.processWithContext(context);
            }
        } catch (Exception e) {
            LOGGER.error("Error processing animation '{}' for player '{}'", 
                animation.getName(), player.getDisplayName().getString(), e);
            return context; // Fallback to original placeholder
        }
    }
    
    private Object[] getServerStats() {
        // Return server statistics for conditional animations
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                int onlinePlayers = server.getPlayerCount();
                return new Object[]{onlinePlayers};
            }
            return new Object[]{0};
        } catch (Exception e) {
            return new Object[]{0};
        }
    }
    
    /**
     * Start the animation update task
     */
    private void startAnimationTask() {
        // Note: In a real implementation, you'd use Minecraft's scheduler
        // For now, this is a placeholder for the concept
        LOGGER.info("Animation system started with max FPS: {}", maxFps);
    }
    
    /**
     * Clean up player animations when they disconnect
     */
    public void cleanupPlayer(UUID playerId) {
        playerAnimations.remove(playerId);
    }
    
    /**
     * Reset all animations
     */
    public void resetAllAnimations() {
        for (Map<String, Animation> playerAnims : playerAnimations.values()) {
            for (Animation animation : playerAnims.values()) {
                animation.reset();
            }
        }
    }
    
    /**
     * Get available animation names
     */
    public Set<String> getAnimationNames() {
        return new HashSet<>(animations.keySet());
    }
    
    /**
     * Get animation by name
     */
    public Animation getAnimation(String name) {
        return animations.get(name);
    }
    
    /**
     * Check if animations are enabled
     */
    public boolean isEnabled() {
        return enableAnimations;
    }
    
    /**
     * Toggle debug mode
     */
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
        LOGGER.info("Animation debug mode: {}", debug ? "enabled" : "disabled");
    }
    
    /**
     * Reload animations from file
     */
    public void reload() {
        lastModified = 0; // Force reload
        loadAnimations();
        LOGGER.info("Animation configuration reloaded");
    }
    
    /**
     * Get animation statistics
     */
    public String getStats() {
        return String.format("Animations: %d loaded, %d players cached, %s",
            animations.size(), playerAnimations.size(), enableAnimations ? "enabled" : "disabled");
    }
}
