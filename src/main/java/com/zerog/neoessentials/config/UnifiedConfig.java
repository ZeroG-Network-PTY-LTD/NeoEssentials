package com.zerog.neoessentials.config;

/**
 * Unified Configuration for NeoEssentials
 * Represents the main config.json file structure
 */
public class UnifiedConfig {
    
    public General general = new General();
    public Modules modules = new Modules();
    public Discord discord = new Discord();
    public Integrations integrations = new Integrations();
    public Performance performance = new Performance();
    
    public static class General {
        public String serverName = "NeoEssentials Server";
        public String version = "2.0.0";
        public String language = "en";
        public boolean enableDebugMode = false;
        public boolean enableMetrics = true;
        public boolean enableUpdateChecker = true;
        public String configVersion = "2.0.0";
    }
    
    public static class Modules {
        public boolean economy = true;
        public boolean homes = true;
        public boolean kits = true;
        public boolean warps = true;
        public boolean moderation = true;
        public boolean chat = true;
        public boolean tablist = true;
        public boolean scoreboard = true;
        public boolean bossbar = true;
        public boolean teleportation = true;
        public boolean shops = true;
        public boolean discord = true;
    }
    
    public static class Discord {
        public boolean enabled = true;
        public boolean useSimpleDiscordLink = true;
        public EnhancedIntegration enhancedIntegration = new EnhancedIntegration();
        
        public static class EnhancedIntegration {
            public boolean enabled = true;
            public boolean roleSync = true;
            public boolean notifications = true;
            public boolean statusUpdates = true;
            public boolean chatSync = true;
        }
    }
    
    public static class Integrations {
        public boolean ftbTeams = true;
        public boolean ftbRanks = true;
        public boolean worldEdit = true;
        public boolean journeyMap = true;
        public boolean jei = true;
    }
    
    public static class Performance {
        public boolean enableAsyncOperations = true;
        public boolean enableCaching = true;
        public int cacheTimeout = 300;
        public int maxCacheSize = 1000;
    }
}
