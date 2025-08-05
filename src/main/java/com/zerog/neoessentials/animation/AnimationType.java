package com.zerog.neoessentials.animation;

/**
 * Types of animations supported by the animation system
 */
public enum AnimationType {
    /**
     * Simple text cycling through predefined frames
     */
    TEXT_CYCLE("text_cycle"),
    
    /**
     * Color cycling through different color codes
     */
    COLOR_CYCLE("color_cycle"),
    
    /**
     * Progress bar animations
     */
    PROGRESS_BAR("progress_bar"),
    
    /**
     * Conditional animations based on server state
     */
    CONDITIONAL("conditional"),
    
    /**
     * Health bar visualization
     */
    HEALTH_BAR("health_bar"),
    
    /**
     * Weather-based dynamic icons
     */
    WEATHER("weather"),
    
    /**
     * Typewriter effect
     */
    TYPEWRITER("typewriter"),
    
    /**
     * Gradient color shifting
     */
    GRADIENT("gradient"),
    
    /**
     * Wave motion effect
     */
    WAVE("wave"),
    
    /**
     * Blinking text effect
     */
    BLINK("blink");
    
    private final String configName;
    
    AnimationType(String configName) {
        this.configName = configName;
    }
    
    public String getConfigName() {
        return configName;
    }
    
    /**
     * Get animation type from config name
     */
    public static AnimationType fromConfigName(String configName) {
        for (AnimationType type : values()) {
            if (type.configName.equals(configName)) {
                return type;
            }
        }
        return TEXT_CYCLE; // Default fallback
    }
    
    /**
     * Check if this animation type requires dynamic processing
     */
    public boolean isDynamic() {
        return this == CONDITIONAL || this == HEALTH_BAR || this == WEATHER || 
               this == TYPEWRITER || this == GRADIENT || this == WAVE;
    }
    
    /**
     * Check if this animation type supports context parameters
     */
    public boolean supportsContext() {
        return isDynamic() || this == COLOR_CYCLE;
    }
}
