package com.zerog.neoessentials.animation;

import com.google.gson.JsonObject;
import java.util.List;

/**
 * Represents an animated placeholder that can cycle through frames
 */
public class Animation {
    private final String name;
    private final AnimationType type;
    private final List<String> frames;
    private final int speed; // milliseconds between frames
    private final String description;
    private final JsonObject properties; // Additional properties for complex animations
    
    private int currentFrame = 0;
    private long lastUpdate = 0;
    
    public Animation(String name, AnimationType type, List<String> frames, int speed, String description, JsonObject properties) {
        this.name = name;
        this.type = type;
        this.frames = frames;
        this.speed = speed;
        this.description = description;
        this.properties = properties != null ? properties : new JsonObject();
    }
    
    /**
     * Get the current frame of the animation
     */
    public String getCurrentFrame() {
        if (frames.isEmpty()) return "";
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdate >= speed) {
            currentFrame = (currentFrame + 1) % frames.size();
            lastUpdate = currentTime;
        }
        
        return frames.get(currentFrame);
    }
    
    /**
     * Get a specific frame by index
     */
    public String getFrame(int index) {
        if (frames.isEmpty() || index < 0 || index >= frames.size()) return "";
        return frames.get(index);
    }
    
    /**
     * Reset animation to first frame
     */
    public void reset() {
        currentFrame = 0;
        lastUpdate = System.currentTimeMillis();
    }
    
    /**
     * Process the animation with context (for dynamic animations)
     */
    public String processWithContext(String context, Object... args) {
        String frame = getCurrentFrame();
        
        // Replace {text} placeholder if present
        if (frame.contains("{text}") && context != null) {
            frame = frame.replace("{text}", context);
        }
        
        // Process additional dynamic content based on animation type
        switch (type) {
            case CONDITIONAL:
                return processConditionalFrame(args);
            case HEALTH_BAR:
                return processHealthBar(args);
            case WEATHER:
                return processWeatherFrame(args);
            case TYPEWRITER:
                return processTypewriterEffect(context);
            case GRADIENT:
                return processGradientEffect(context);
            case WAVE:
                return processWaveEffect(context);
            default:
                return frame;
        }
    }
    
    private String processConditionalFrame(Object... args) {
        // Implementation for conditional animations based on server state
        if (properties.has("conditions")) {
            // Process conditions and return appropriate frame
            // This would check placeholder values against conditions
        }
        return getCurrentFrame();
    }
    
    private String processHealthBar(Object... args) {
        if (args.length > 0 && args[0] instanceof Number) {
            double health = ((Number) args[0]).doubleValue();
            double maxHealth = args.length > 1 ? ((Number) args[1]).doubleValue() : 20.0;
            
            String fullChar = properties.has("full_char") ? properties.get("full_char").getAsString() : "❤";
            String halfChar = properties.has("half_char") ? properties.get("half_char").getAsString() : "♡";
            String emptyChar = properties.has("empty_char") ? properties.get("empty_char").getAsString() : "♢";
            String fullColor = properties.has("full_color") ? properties.get("full_color").getAsString() : "&c";
            String halfColor = properties.has("half_color") ? properties.get("half_color").getAsString() : "&6";
            String emptyColor = properties.has("empty_color") ? properties.get("empty_color").getAsString() : "&7";
            int maxHearts = properties.has("max_hearts") ? properties.get("max_hearts").getAsInt() : 10;
            
            StringBuilder healthBar = new StringBuilder();
            double heartsToShow = Math.min(maxHealth / 2, maxHearts);
            double currentHearts = health / 2;
            
            for (int i = 0; i < heartsToShow; i++) {
                if (currentHearts >= i + 1) {
                    healthBar.append(fullColor).append(fullChar);
                } else if (currentHearts >= i + 0.5) {
                    healthBar.append(halfColor).append(halfChar);
                } else {
                    healthBar.append(emptyColor).append(emptyChar);
                }
            }
            
            return healthBar.toString();
        }
        return getCurrentFrame();
    }
    
    private String processWeatherFrame(Object... args) {
        if (args.length > 0 && args[0] instanceof String) {
            String weather = (String) args[0];
            String clear = properties.has("clear") ? properties.get("clear").getAsString() : "☀";
            String rain = properties.has("rain") ? properties.get("rain").getAsString() : "☔";
            String storm = properties.has("storm") ? properties.get("storm").getAsString() : "⚡";
            String snow = properties.has("snow") ? properties.get("snow").getAsString() : "❄";
            
            switch (weather.toLowerCase()) {
                case "clear":
                case "sunny":
                    return clear;
                case "rain":
                case "drizzle":
                    return rain;
                case "thunder":
                case "storm":
                    return storm;
                case "snow":
                case "blizzard":
                    return snow;
                default:
                    return clear;
            }
        }
        return getCurrentFrame();
    }
    
    private String processTypewriterEffect(String text) {
        if (text == null) return "";
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdate >= speed) {
            currentFrame = Math.min(currentFrame + 1, text.length());
            lastUpdate = currentTime;
        }
        
        String partial = text.substring(0, Math.min(currentFrame, text.length()));
        boolean showCursor = properties.has("show_cursor") && properties.get("show_cursor").getAsBoolean();
        String cursor = properties.has("cursor") ? properties.get("cursor").getAsString() : "_";
        
        if (showCursor && currentFrame < text.length()) {
            partial += cursor;
        }
        
        return partial;
    }
    
    private String processGradientEffect(String text) {
        // Implementation for gradient text animation
        return getCurrentFrame().replace("{text}", text != null ? text : "");
    }
    
    private String processWaveEffect(String context) {
        // Implementation for wave animation
        return getCurrentFrame();
    }
    
    // Getters
    public String getName() { return name; }
    public AnimationType getType() { return type; }
    public List<String> getFrames() { return frames; }
    public int getSpeed() { return speed; }
    public String getDescription() { return description; }
    public JsonObject getProperties() { return properties; }
    public int getCurrentFrameIndex() { return currentFrame; }
    public int getFrameCount() { return frames.size(); }
}
