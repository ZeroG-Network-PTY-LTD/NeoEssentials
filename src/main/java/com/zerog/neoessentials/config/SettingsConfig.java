package com.zerog.neoessentials.config;

/**
 * Settings Configuration for NeoEssentials
 * Represents the settings.json file structure
 */
public class SettingsConfig {
    
    public General general = new General();
    public Features features = new Features();
    
    public static class General {
        public String pluginName = "NeoEssentials";
        public String version = "2.0.0";
        public String language = "en";
        public boolean enableDebugMode = false;
        public boolean enableMetrics = true;
        public boolean enableUpdateChecker = true;
    }
    
    public static class Features {
        public Tablist tablist = new Tablist();
        public Discord discord = new Discord();
        
        public static class Tablist {
            public boolean enabled = true;
            public boolean useUnifiedConfig = true;
            public boolean enableAnimations = true;
            public int updateInterval = 20;
        }
        
        public static class Discord {
            public boolean enabled = true;
            public boolean useSimpleDiscordLink = true;
            public boolean enableEnhancedIntegration = true;
            public boolean enableRoleSync = true;
        }
    }
}
