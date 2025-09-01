package com.zerog.neoessentials.config;

import java.util.List;
import java.util.ArrayList;

/**
 * Animations Configuration for NeoEssentials
 * Represents the animations.json file structure
 */
public class AnimationsConfig {
    
    public Global global = new Global();
    public List<Animation> animations = new ArrayList<>();
    
    public AnimationsConfig() {
        // Initialize with default animation
        Animation rainbowWelcome = new Animation();
        rainbowWelcome.name = "rainbow_welcome";
        rainbowWelcome.type = "rainbow";
        rainbowWelcome.text = "Welcome to NeoEssentials Server!";
        rainbowWelcome.speed = 20;
        rainbowWelcome.colors = List.of("&c", "&6", "&e", "&a", "&b", "&9", "&d");
        rainbowWelcome.loop = true;
        animations.add(rainbowWelcome);
    }
    
    public static class Global {
        public boolean enabled = true;
        public int defaultSpeed = 20;
        public boolean enableRainbowColors = true;
        public boolean enableGradients = true;
    }
    
    public static class Animation {
        public String name;
        public String type;
        public String text;
        public int speed;
        public List<String> colors;
        public boolean loop;
    }
}