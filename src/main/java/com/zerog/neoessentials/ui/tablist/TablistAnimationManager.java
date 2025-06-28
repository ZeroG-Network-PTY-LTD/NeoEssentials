package com.zerog.neoessentials.ui.tablist;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
// TOML import removed in YAML migration
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.TablistYamlConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles animations for tablist headers and footers.
 * This class provides various animation types and manages animation state for each player.
 */
public class TablistAnimationManager {    /**
     * Enum defining the different types of animations available
     */    public enum AnimationType {
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
        
        public static AnimationType fromConfigValue(String value) {
            for (AnimationType type : values()) {
                if (type.configValue.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return ROTATION; // Default to rotation
        }
    }
    
    // Animation state tracking per player
    private final Map<UUID, PlayerAnimationState> playerStates = new ConcurrentHashMap<>();
    
    // Animation processors for each animation type
    private final Map<AnimationType, AnimationProcessor> animationProcessors = new HashMap<>();
    
    // YAML animation data loaded from TABConfig
    private Map<String, Object> yamlAnimations = new HashMap<>();
    
    // Animation cache for loaded animations
    private final Map<String, AnimationData> animationCache = new HashMap<>();
    
    // Default change interval if not specified
    private static final int DEFAULT_CHANGE_INTERVAL = 50;
    
    /**
     * Represents animation data loaded from YAML
     */
    private static class AnimationData {
        final int changeInterval;
        final List<String> texts;
        
        AnimationData(int changeInterval, List<String> texts) {
            this.changeInterval = changeInterval;
            this.texts = new ArrayList<>(texts);
        }
    }
    
    /**
     * Initializes the animation manager with all animation processors
     */
    public TablistAnimationManager() {        // Register animation processors
        animationProcessors.put(AnimationType.NONE, new NoAnimationProcessor());
        animationProcessors.put(AnimationType.ROTATION, new RotationAnimationProcessor());
        animationProcessors.put(AnimationType.SCROLL, new ScrollAnimationProcessor());
        animationProcessors.put(AnimationType.FADE, new FadeAnimationProcessor());
        animationProcessors.put(AnimationType.RAINBOW, new RainbowAnimationProcessor());
        animationProcessors.put(AnimationType.TYPEWRITER, new TypewriterAnimationProcessor());
        animationProcessors.put(AnimationType.BLINK, new BlinkAnimationProcessor());        animationProcessors.put(AnimationType.WAVE, new WaveAnimationProcessor());
        animationProcessors.put(AnimationType.GRADIENT, new GradientAnimationProcessor());
        animationProcessors.put(AnimationType.PULSE, new PulseAnimationProcessor());
        animationProcessors.put(AnimationType.HEX_CUSTOM, new HexCustomAnimationProcessor());
        
        NeoEssentials.LOGGER.info("TablistAnimationManager initialized with {} animation types", animationProcessors.size());
    }
    
    /**
     * Gets the animation state for a player, creating it if it doesn't exist
     * 
     * @param player The player
     * @return The player's animation state
     */
    public PlayerAnimationState getPlayerState(ServerPlayer player) {
        return playerStates.computeIfAbsent(player.getUUID(), uuid -> new PlayerAnimationState());
    }
    
    /**
     * Removes a player's animation state when they disconnect
     * 
     * @param playerId The UUID of the disconnected player
     */
    public void removePlayer(UUID playerId) {
        playerStates.remove(playerId);
    }
      /**
     * Updates the animation frame for all players
     */
    public void updateAnimationFrames() {
        playerStates.forEach((uuid, state) -> {
            state.headerFrame++;
            state.footerFrame++;
        });
    }
    
    /**
     * Creates an animated component from a list of text lines
     * 
     * @param player The player to create the component for
     * @param lines The lines of text to animate
     * @param animationTypeStr The animation type name
     * @param placeholderManager The placeholder manager
     * @return The animated component
     */
    public Component createAnimatedComponent(
            ServerPlayer player,
            List<String> lines,
            String animationTypeStr,
            TablistPlaceholderManager placeholderManager) {
        
        // Convert type string to enum
        AnimationType animationType = AnimationType.fromConfigValue(animationTypeStr);
        
        // Get the processor
        AnimationProcessor processor = animationProcessors.get(animationType);
        if (processor == null) {
            // Default to no animation
            processor = animationProcessors.get(AnimationType.NONE);
        }
        
        // Process placeholders
        List<String> processedLines = new ArrayList<>();
        for (String line : lines) {
            processedLines.add(placeholderManager.processPlaceholders(line, player));
        }
        
        // Get player state
        PlayerAnimationState state = getPlayerState(player);
        
        // Process the animation frame
        String text = processor.processFrame(
            processedLines,
            player,
            state.headerFrame
        );
        
        // Process color codes
        text = TablistPlaceholderManager.formatColors(text);
        
        // Return as component
        return net.minecraft.network.chat.Component.literal(text);
    }
    
    /**
     * Gets the animated header for a player
     * 
     * @param player The player
     * @param headerTemplates The list of header templates
     * @param placeholderManager The placeholder manager for processing placeholders
     * @return The animated header component
     */
    public Component getAnimatedHeader(ServerPlayer player, List<String> headerTemplates, TablistPlaceholderManager placeholderManager) {
        PlayerAnimationState state = getPlayerState(player);
        AnimationType animationType = AnimationType.fromConfigValue(TablistYamlConfig.getHeaderAnimationType());
        AnimationProcessor processor = animationProcessors.getOrDefault(animationType, animationProcessors.get(AnimationType.ROTATION));
        
        // Process the animation
        String animatedText = processor.processFrame(headerTemplates, player, state.headerFrame);
        
        // Process placeholders
        animatedText = placeholderManager.processPlaceholders(animatedText, player);
        
        // Convert to Component
        return TablistPlaceholderManager.colorize(animatedText);
    }
    
    /**
     * Gets the animated footer for a player
     * 
     * @param player The player
     * @param footerTemplates The list of footer templates
     * @param placeholderManager The placeholder manager for processing placeholders
     * @return The animated footer component
     */
    public Component getAnimatedFooter(ServerPlayer player, List<String> footerTemplates, TablistPlaceholderManager placeholderManager) {
        PlayerAnimationState state = getPlayerState(player);
        AnimationType animationType = AnimationType.fromConfigValue(TablistYamlConfig.getFooterAnimationType());
        AnimationProcessor processor = animationProcessors.getOrDefault(animationType, animationProcessors.get(AnimationType.ROTATION));
        
        // Process the animation
        String animatedText = processor.processFrame(footerTemplates, player, state.footerFrame);
        
        // Process placeholders
        animatedText = placeholderManager.processPlaceholders(animatedText, player);
        
        // Convert to Component
        return TablistPlaceholderManager.colorize(animatedText);
    }
    
    /**
     * Class to track animation state for a player
     */
    public static class PlayerAnimationState {
        public int headerFrame = 0;
        public int footerFrame = 0;
    }
    
    /**
     * Interface for animation processors
     */
    public interface AnimationProcessor {
        /**
         * Process a frame of animation
         * 
         * @param templates The list of templates to animate
         * @param player The player to animate for
         * @param frame The current frame number
         * @return The processed text for this frame
         */
        String processFrame(List<String> templates, ServerPlayer player, int frame);
    }
    
    /**
     * No animation - just returns the first template
     */
    private static class NoAnimationProcessor implements AnimationProcessor {
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) {
                return "";
            }
            return templates.get(0);
        }
    }
    
    /**
     * Rotation animation - cycles through templates
     */
    private static class RotationAnimationProcessor implements AnimationProcessor {
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) {
                return "";
            }
            int index = frame % templates.size();
            return templates.get(index);
        }
    }
    
    /**
     * Scroll animation - scrolls text horizontally
     */
    private static class ScrollAnimationProcessor implements AnimationProcessor {
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) {
                return "";
            }
            
            // Get the template to scroll
            String template = templates.get(frame % templates.size());
            
            // Remove color codes for scrolling calculation
            String plainText = TablistPlaceholderManager.stripColor(template);
            
            // If text is shorter than scroll width, no need to scroll
            int scrollWidth = TablistYamlConfig.getScrollWidth();
            if (plainText.length() <= scrollWidth) {
                return template;
            }
            
            // Calculate scroll position
            int effectiveFrame = frame % (plainText.length() + scrollWidth);
            
            // Create scrolled text with padding
            String scrolledText = plainText + " " + plainText;
            String visiblePortion = TablistPlaceholderManager.substring(scrolledText, effectiveFrame, effectiveFrame + scrollWidth);
            
            // Preserve color codes from the original template
            return TablistPlaceholderManager.transferColors(template, visiblePortion);
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
            ChatFormatting.GREEN
        };
        
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) {
                return "";
            }
            
            // Get template to use
            String template = templates.get(frame % templates.size());
            
            // Get the color for this frame
            ChatFormatting color = FADE_COLORS[frame % FADE_COLORS.length];
            
            // Apply the color to the template
            return color + TablistPlaceholderManager.stripColor(template);
        }
    }
    
    /**
     * Rainbow animation - cycles through colors for each character
     */
    private static class RainbowAnimationProcessor implements AnimationProcessor {
        private static final ChatFormatting[] RAINBOW_COLORS = {
            ChatFormatting.RED,
            ChatFormatting.GOLD,
            ChatFormatting.YELLOW,
            ChatFormatting.GREEN,
            ChatFormatting.AQUA,
            ChatFormatting.LIGHT_PURPLE
        };
        
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) {
                return "";
            }
            
            // Get template to use
            String template = templates.get(frame % templates.size());
            String plainText = TablistPlaceholderManager.stripColor(template);
            
            // Build rainbow text
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < plainText.length(); i++) {
                int colorIndex = (i + frame) % RAINBOW_COLORS.length;
                result.append(RAINBOW_COLORS[colorIndex]).append(plainText.charAt(i));
            }
            
            return result.toString();
        }
    }
    
    /**
     * Typewriter animation - types out the text
     */
    private static class TypewriterAnimationProcessor implements AnimationProcessor {
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) {
                return "";
            }
            
            // Get template to use
            String template = templates.get(frame / 20 % templates.size()); // Slower rotation
            String plainText = TablistPlaceholderManager.stripColor(template);
            
            // Calculate how many characters to show
            int charactersToShow = frame % (plainText.length() + 15);
            if (charactersToShow > plainText.length()) {
                // Hold on complete text for a while
                return template;
            }
            
            // Show only the first N characters
            String visiblePortion = plainText.substring(0, charactersToShow);
            
            // Preserve color codes from the original template
            return TablistPlaceholderManager.transferColors(template, visiblePortion) + "§r§e_";
        }
    }
      /**
     * Blink animation - makes text blink
     */
    private static class BlinkAnimationProcessor implements AnimationProcessor {
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) {
                return "";
            }
            
            // Get template to use
            String template = templates.get(frame / 10 % templates.size());
            
            // Blink every 10 frames
            if ((frame / 5) % 2 == 0) {
                return template;
            } else {
                // This effectively hides the text using a color that matches the background
                return "§8" + TablistPlaceholderManager.stripColor(template);
            }
        }
    }
    
    /**
     * Wave animation - creates a wave effect where characters rise and fall
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
            if (templates.isEmpty()) {
                return "";
            }
            
            // Get template to use
            String template = templates.get(frame / 15 % templates.size());
            String plainText = TablistPlaceholderManager.stripColor(template);
            
            // Build wave text
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < plainText.length(); i++) {
                // Calculate wave position (shifted by character position for wave effect)
                int wavePos = (frame + i * 2) % WAVE_COLORS.length;
                result.append(WAVE_COLORS[wavePos]).append(plainText.charAt(i));
            }
              return result.toString();
        }
    }
    
    /**
     * Gradient animation - applies a color gradient across the text
     */
    private static class GradientAnimationProcessor implements AnimationProcessor {
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) {
                return "";
            }
            
            // Get the template
            String template = templates.get(frame % templates.size());
            
            // Remove color codes for processing
            String plainText = TablistPlaceholderManager.stripColor(template);
            
            // If text is too short, just return it
            if (plainText.length() < 3) {
                return template;
            }
            
            // Define gradient colors (can be customized later from config)
            ChatFormatting[] gradientColors = {
                ChatFormatting.RED,
                ChatFormatting.GOLD,
                ChatFormatting.YELLOW,
                ChatFormatting.GREEN,
                ChatFormatting.AQUA,
                ChatFormatting.BLUE,
                ChatFormatting.LIGHT_PURPLE
            };
            
            // Calculate gradient starting position that shifts over time
            int startColorIndex = frame % gradientColors.length;
            
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < plainText.length(); i++) {
                // Calculate color index for this character
                int colorIndex = (startColorIndex + i) % gradientColors.length;
                
                // Apply gradient color to character
                result.append(gradientColors[colorIndex]).append(plainText.charAt(i));
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
            if (templates.isEmpty()) {
                return "";
            }
            
            // Get the template
            String template = templates.get(frame % templates.size());
            
            // Remove color codes for processing
            String plainText = TablistPlaceholderManager.stripColor(template);
            
            // Define pulse colors (primary and secondary)
            ChatFormatting primaryColor = ChatFormatting.WHITE;
            ChatFormatting secondaryColor = ChatFormatting.YELLOW;
            
            // Determine current pulse state (8 frames per pulse cycle)
            int pulseState = (frame / 4) % 2;
            ChatFormatting currentColor = (pulseState == 0) ? primaryColor : secondaryColor;
            
            // Apply current pulse color to entire text
            return currentColor + plainText;
        }
    }
    
    /**
     * Custom Hex Color Animation - uses pre-defined hex colored text from animations.toml/json
     */
    private static class HexCustomAnimationProcessor implements AnimationProcessor {
        // Cache animations by name for quick lookup
        private static final Map<String, CustomHexAnimation> animationCache = new HashMap<>();
        // Default change interval if not specified
        private static final int DEFAULT_CHANGE_INTERVAL = 50; 
        // Flag to track if initialization has happened
        private static boolean initialized = false;
        
        /**
         * Process a frame using custom hex-color-coded text
         */
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) {
                return "";
            }
            
            // Ensure animations are loaded
            if (!initialized) {
                loadCustomAnimations();
                initialized = true;
            }
            
            // Get animation name from template (if specified)
            String template = templates.get(0);
            String animationName = "default";
            
            // Check if template specifies an animation
            if (template.startsWith("animation:")) {
                String[] parts = template.split(":", 2);
                if (parts.length == 2) {
                    animationName = parts[1].trim();
                }
                
                // If more templates exist, use the second one as the base text
                if (templates.size() > 1) {
                    template = templates.get(1);
                } else {
                    template = "";
                }
            }
            
            // For simplified animation system, just return a basic animation text
            // Get the current frame based on system time
            long currentTime = System.currentTimeMillis();
            int animFrame = (int) (currentTime / 50); // Convert to ticks (50ms per tick)
            
            // Use a simple rotating animation if no specific animation found
            String[] defaultTexts = {
                "&#54C5EAE&#54DAF4x&#54C5EAa&#54B1DFm&#549CD5p&#5487CBl&#5473C0e",
                "&#54B1DFE&#54C5EAx&#54DAF4a&#54C5EAm&#54B1DFp&#549CD5l&#5487CBe",
                "&#549CD5E&#54B1DFx&#54C5EAa&#54DAF4m&#54C5EAp&#54B1DFl&#549CD5e"
            };
            
            int textIndex = (animFrame / 50) % defaultTexts.length;
            return defaultTexts[textIndex];
        }
        
        /**
         * Load custom animations from config files
         */
        private void loadCustomAnimations() {
            // Clear existing cache
            animationCache.clear();
            
            // First try to load from TOML as it's the preferred format
            Path tomlPath = FMLPaths.CONFIGDIR.get().resolve("neoessentials/animations.toml");
            if (Files.exists(tomlPath)) {
                loadFromToml(tomlPath);
            } else {
                // If TOML doesn't exist, try JSON
                Path jsonPath = FMLPaths.CONFIGDIR.get().resolve("neoessentials/animations.json");
                if (Files.exists(jsonPath)) {
                    loadFromJson(jsonPath);
                } else {
                    // If no files exist, create default files
                    createDefaultAnimationFiles();
                }
            }
            
            // Always ensure we have at least the default animation
            if (!animationCache.containsKey("default")) {
                // TODO: Fix this to use proper animation data
                // animationCache.put("default", getDefaultAnimation());
            }
            
            NeoEssentials.LOGGER.info("Loaded {} custom hex animations", animationCache.size());
        }
          /**
         * Load animations from a YAML file
         */
        private void loadFromToml(Path path) {
            try {
                String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                Map<String, Object> yamlData = new Yaml().load(content);
                
                // Process each animation section
                for (Map.Entry<String, Object> entry : yamlData.entrySet()) {
                    String animationName = entry.getKey();
                    if (entry.getValue() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> animData = (Map<String, Object>) entry.getValue();
                        
                        // Get change interval
                        Object intervalObj = animData.get("change-interval");
                        int interval = (intervalObj instanceof Number) 
                            ? ((Number) intervalObj).intValue() 
                            : DEFAULT_CHANGE_INTERVAL;
                        
                        // Get texts
                        Object textsObj = animData.get("texts");
                        if (textsObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> texts = (List<String>) textsObj;
                            if (!texts.isEmpty()) {
                                animationCache.put(animationName, new CustomHexAnimation(interval, texts));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error loading custom hex animations from YAML", e);
            }
        }
        
        /**
         * Load animations from a JSON file
         */
        private void loadFromJson(Path path) {
            try {
                String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                
                // Process each animation entry
                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    String animationName = entry.getKey();
                    if (entry.getValue().isJsonObject()) {
                        JsonObject animData = entry.getValue().getAsJsonObject();
                        
                        // Get change interval
                        int interval = DEFAULT_CHANGE_INTERVAL;
                        if (animData.has("change-interval")) {
                            interval = animData.get("change-interval").getAsInt();
                        }
                        
                        // Get texts
                        if (animData.has("texts") && animData.get("texts").isJsonArray()) {
                            JsonArray textsArray = animData.get("texts").getAsJsonArray();
                            List<String> texts = new ArrayList<>();
                            
                            for (JsonElement textElement : textsArray) {
                                texts.add(textElement.getAsString());
                            }
                            
                            if (!texts.isEmpty()) {
                                animationCache.put(animationName, new CustomHexAnimation(interval, texts));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error loading custom hex animations from JSON", e);
            }
        }
          /**
         * Create default animation files if none exist
         */
        private void createDefaultAnimationFiles() {
            // Create YAML file with example
            Path yamlPath = FMLPaths.CONFIGDIR.get().resolve("neoessentials/animations.yml");
            StringBuilder yamlBuilder = new StringBuilder();
            yamlBuilder.append("# NeoEssentials Custom Hex Color Animations\n");
            yamlBuilder.append("# Format: &#RRGGBB for hex colors\n\n");
            yamlBuilder.append("default:\n");
            yamlBuilder.append("  change-interval: 50\n");
            yamlBuilder.append("  texts:\n");
            
            // Add example texts with hex colors
            // TODO: Fix this to use proper animation data structure
            List<String> exampleTexts = Arrays.asList("Example", "Animation", "Text");
            // List<String> exampleTexts = getDefaultAnimation().texts;
            for (int i = 0; i < exampleTexts.size(); i++) {
                yamlBuilder.append("    - \"").append(exampleTexts.get(i)).append("\"\n");
            }
            yamlBuilder.append("\n");
              // Add another example
            yamlBuilder.append("rainbow_wave:\n");
            yamlBuilder.append("  change-interval: 30\n");
            yamlBuilder.append("  texts:\n");
            yamlBuilder.append("    - \"&#FF0000R&#FF7F00a&#FFFF00i&#00FF00n&#0000FFb&#4B0082o&#9400D3w\"\n");
            yamlBuilder.append("    - \"&#FF7F00R&#FFFF00a&#00FF00i&#0000FFn&#4B0082b&#9400D3o&#FF0000w\"\n");
            yamlBuilder.append("    - \"&#FFFF00R&#00FF00a&#0000FFi&#4B0082n&#9400D3b&#FF0000o&#FF7F00w\"\n");
            yamlBuilder.append("    - \"&#00FF00R&#0000FFa&#4B0082i&#9400D3n&#FF0000b&#FF7F00o&#FFFF00w\"\n");
            yamlBuilder.append("    - \"&#0000FFR&#4B0082a&#9400D3i&#FF0000n&#FF7F00b&#FFFF00o&#00FF00w\"\n");            yamlBuilder.append("    - \"&#4B0082R&#9400D3a&#FF0000i&#FF7F00n&#FFFF00b&#00FF00o&#0000FFw\"\n");
            
            try {
                Files.createDirectories(yamlPath.getParent());
                Files.write(yamlPath, yamlBuilder.toString().getBytes(StandardCharsets.UTF_8));
                NeoEssentials.LOGGER.info("Created default animations.yml file");
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error creating default animations.yml file", e);
            }
        }
        
        /**
         * Custom Hex Animation data class
         */
        private static class CustomHexAnimation {
            final int changeInterval;
            final List<String> texts;
            
            CustomHexAnimation(int changeInterval, List<String> texts) {
                this.changeInterval = changeInterval;
                this.texts = texts;
            }
        }
    }
    
    /**
     * Get the current animation frame for a specific animation
     * @param animationName The name of the animation
     * @param player The player (for context if needed)
     * @return The current animation frame text, or null if animation not found
     */
    public String getAnimationFrame(String animationName, ServerPlayer player) {
        AnimationData animation = animationCache.get(animationName);
        if (animation == null) {
            NeoEssentials.LOGGER.debug("Animation '{}' not found in cache", animationName);
            return null;
        }
        
        // Get current frame based on system time and animation interval
        // changeInterval is in ticks (1 tick = 50ms)
        long currentTime = System.currentTimeMillis();
        long tickTime = currentTime / 50; // Convert to ticks
        
        // Calculate frame index based on the animation's change interval
        int frameIndex = (int) (tickTime / animation.changeInterval) % animation.texts.size();
        
        String animationText = animation.texts.get(frameIndex);
        
        // Return raw text - color processing will be done later by colorize() method
        NeoEssentials.LOGGER.debug("Animation '{}' interval:{} tick:{} frame:{} text:'{}'", 
            animationName, animation.changeInterval, tickTime, frameIndex, animationText);
        
        return animationText;
    }
    
    /**
     * Load animations from TABConfig YAML data
     * @param animationsData The animations data from TABConfig
     */
    public void loadAnimationsFromConfig(Map<String, Object> animationsData) {
        this.yamlAnimations = new HashMap<>(animationsData);
        
        // Clear existing cache and reload from YAML data
        animationCache.clear();
        
        NeoEssentials.LOGGER.info("Loading animations from YAML configuration...");
        
        for (Map.Entry<String, Object> entry : animationsData.entrySet()) {
            String animationName = entry.getKey();
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> animData = (Map<String, Object>) entry.getValue();
                
                // Get change interval
                Object intervalObj = animData.get("change_interval");
                int interval = (intervalObj instanceof Number) 
                    ? ((Number) intervalObj).intValue() 
                    : DEFAULT_CHANGE_INTERVAL;
                
                // Get texts
                Object textsObj = animData.get("texts");
                if (textsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> texts = (List<String>) textsObj;
                    if (!texts.isEmpty()) {
                        animationCache.put(animationName, new AnimationData(interval, texts));
                        NeoEssentials.LOGGER.info("Loaded animation '{}' with {} frames and {}ms interval", 
                            animationName, texts.size(), interval);
                    }
                }
            }
        }
        
        // Always ensure we have at least the default animation
        if (!animationCache.containsKey("default")) {
            animationCache.put("default", getDefaultAnimation());
            NeoEssentials.LOGGER.info("Added fallback default animation");
        }
        
        NeoEssentials.LOGGER.info("Loaded {} animations from YAML configuration", animationCache.size());
    }
    
    /**
     * Gets the default animation data
     * @return Default animation data
     */
    private AnimationData getDefaultAnimation() {
        List<String> defaultTexts = Arrays.asList(
            "&#54C5EAE&#54DAF4x&#54C5EAa&#54B1DFm&#549CD5p&#5487CBl&#5473C0e",
            "&#54B1DFE&#54C5EAx&#54DAF4a&#54C5EAm&#54B1DFp&#549CD5l&#5487CBe",
            "&#549CD5E&#54B1DFx&#54C5EAa&#54DAF4m&#54C5EAp&#54B1DFl&#549CD5e"
        );
        return new AnimationData(DEFAULT_CHANGE_INTERVAL, defaultTexts);
    }
}
