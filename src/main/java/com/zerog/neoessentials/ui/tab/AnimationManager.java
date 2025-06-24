package com.zerog.neoessentials.ui.tab;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.moandjiezana.toml.Toml;
import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final Map<String, CustomHexAnimation> customHexAnimations = new HashMap<>();
    private boolean customHexAnimationsLoaded = false;
    
    /**
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
        
        // Try TOML first
        Path tomlPath = Path.of("config/neoessentials/animations.toml");
        if (Files.exists(tomlPath)) {
            loadFromToml(tomlPath);
        } else {
            // If TOML doesn't exist, try JSON
            Path jsonPath = Path.of("config/neoessentials/animations.json");
            if (Files.exists(jsonPath)) {
                loadFromJson(jsonPath);
            } else {
                // Create default file if none exists
                createDefaultAnimationFile();
            }
        }
        
        // Ensure we have a default animation
        if (!customHexAnimations.containsKey("default")) {
            List<String> defaultTexts = new ArrayList<>();
            defaultTexts.add("&#54C5EAE&#54DAF4x&#54C5EAa&#54B1DFm&#549CD5p&#5487CBl&#5473C0e");
            defaultTexts.add("&#54B1DFE&#54C5EAx&#54DAF4a&#54C5EAm&#54B1DFp&#549CD5l&#5487CBe");
            customHexAnimations.put("default", new CustomHexAnimation(50, defaultTexts));
        }
        
        customHexAnimationsLoaded = true;
        NeoEssentials.LOGGER.info("Loaded {} custom hex animations", customHexAnimations.size());
    }
    
    /**
     * Loads animations from TOML file
     */
    private void loadFromToml(Path path) {
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            Toml toml = new Toml().read(content);
            
            for (Map.Entry<String, Object> entry : toml.entrySet()) {
                String name = entry.getKey();
                if (entry.getValue() instanceof Toml) {
                    Toml animData = (Toml) entry.getValue();
                    
                    // Get change interval
                    Long interval = animData.getLong("change-interval");
                    int changeInterval = interval != null ? interval.intValue() : 50;
                    
                    // Get animation texts
                    List<String> texts = animData.getList("texts");
                    if (texts != null && !texts.isEmpty()) {
                        customHexAnimations.put(name, new CustomHexAnimation(changeInterval, texts));
                    }
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
     * Creates default animation file
     */
    private void createDefaultAnimationFile() {
        Path path = Path.of("config/neoessentials/animations.toml");
        
        try {
            // Create directory if needed
            Files.createDirectories(path.getParent());
            
            // Create default content
            StringBuilder sb = new StringBuilder();
            sb.append("# NeoEssentials Custom Hex Color Animations\n");
            sb.append("# Format: &#RRGGBB for hex colors\n\n");
            
            sb.append("[default]\n");
            sb.append("change-interval = 50\n");
            sb.append("texts = [\n");
            sb.append("  \"&#54C5EAE&#54DAF4x&#54C5EAa&#54B1DFm&#549CD5p&#5487CBl&#5473C0e\",\n");
            sb.append("  \"&#54B1DFE&#54C5EAx&#54DAF4a&#54C5EAm&#54B1DFp&#549CD5l&#5487CBe\",\n");
            sb.append("  \"&#549CD5E&#54B1DFx&#54C5EAa&#54DAF4m&#54C5EAp&#54B1DFl&#549CD5e\",\n");
            sb.append("  \"&#5487CBE&#549CD5x&#54B1DFa&#54C5EAm&#54DAF4p&#54C5EAl&#54B1DFe\",\n");
            sb.append("  \"&#5473C0E&#5487CBx&#549CD5a&#54B1DFm&#54C5EAp&#54DAF4l&#54C5EAe\"\n");
            sb.append("]\n\n");
            
            sb.append("[rainbow]\n");
            sb.append("change-interval = 30\n");
            sb.append("texts = [\n");
            sb.append("  \"&#FF0000R&#FF7F00a&#FFFF00i&#00FF00n&#0000FFb&#4B0082o&#9400D3w\",\n");
            sb.append("  \"&#FF7F00R&#FFFF00a&#00FF00i&#0000FFn&#4B0082b&#9400D3o&#FF0000w\",\n");
            sb.append("  \"&#FFFF00R&#00FF00a&#0000FFi&#4B0082n&#9400D3b&#FF0000o&#FF7F00w\"\n");
            sb.append("]\n");
            
            Files.writeString(path, sb.toString());
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to create default animations.toml", e);
        }
    }
    
    /**
     * Gets a custom hex animation by name
     * 
     * @param name Animation name
     * @return The animation, or default if not found
     */
    public CustomHexAnimation getCustomHexAnimation(String name) {
        if (!customHexAnimationsLoaded) {
            loadCustomHexAnimations();
        }
        
        return customHexAnimations.getOrDefault(name, 
            customHexAnimations.getOrDefault("default", null));
    }
    
    /**
     * Storage class for custom hex animations
     */
    public static class CustomHexAnimation {
        private final int changeInterval;
        private final List<String> texts;
        
        public CustomHexAnimation(int changeInterval, List<String> texts) {
            this.changeInterval = changeInterval;
            this.texts = new ArrayList<>(texts);
        }
        
        public int getChangeInterval() {
            return changeInterval;
        }
        
        public List<String> getTexts() {
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
    private static class HexCustomAnimationProcessor implements AnimationProcessor {
        private static final Pattern HEX_PATTERN = Pattern.compile("&#[0-9A-Fa-f]{6}");
        
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
            
            // Get animation
            AnimationManager manager = NeoEssentials.getInstance().getTabManager().getAnimationManager();
            CustomHexAnimation animation = manager.getCustomHexAnimation(animationName);
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
