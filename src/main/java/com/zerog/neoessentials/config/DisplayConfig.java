package com.zerog.neoessentials.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Display Configuration - display-config.json
 * Contains all display-related configurations (tablist, scoreboard, bossbar, animations)
 */
public class DisplayConfig {
    
    public Tablist tablist = new Tablist();
    public Scoreboard scoreboard = new Scoreboard();
    public Bossbar bossbar = new Bossbar();
    public Animations animations = new Animations();
    public Placeholders placeholders = new Placeholders();
    
    public static class Tablist {
        public boolean enabled = true;
        public int updateInterval = 20;
        public String defaultHeader = "&6&lNeoEssentials Server";
        public String defaultFooter = "&7Online: {server_players}/{server_max_players}";
        public boolean enableAnimations = true;
        public boolean enablePlayerList = true;
        public List<Layout> layouts = new ArrayList<>();
        
        public static class Layout {
            public String name = "default";
            public int priority = 0;
            public String conditionType = "default";
            public String condition = "";
            public List<String> header = new ArrayList<>();
            public List<String> footer = new ArrayList<>();
            public List<PlayerEntry> playerEntries = new ArrayList<>();
            
            public static class PlayerEntry {
                public String permission = "";
                public String prefix = "";
                public String suffix = "";
                public String format = "{prefix} {player_name} {suffix}";
            }
        }
    }
    
    public static class Scoreboard {
        public boolean enabled = true;
        public int updateInterval = 20;
        public int maxLines = 15;
        public String title = "&6&lNeoEssentials Server";
        public List<Layout> layouts = new ArrayList<>();
        
        public static class Layout {
            public String name = "default";
            public int priority = 1;
            public String conditionType = "default";
            public String condition = "";
            public String title = "&7&lPLAYER INFO";
            public List<String> lines = new ArrayList<>();
        }
    }
    
    public static class Bossbar {
        public boolean enabled = true;
        public int updateInterval = 20;
        public String defaultText = "&6Welcome to NeoEssentials Server!";
        public String defaultColor = "YELLOW";
        public String defaultStyle = "SOLID";
        public boolean enableAnimations = true;
        public List<BossbarEntry> bossbars = new ArrayList<>();
        
        public static class BossbarEntry {
            public String name = "welcome";
            public String permission = "";
            public String text = "&6Welcome {player_name}!";
            public String color = "YELLOW";
            public String style = "SOLID";
            public float progress = 1.0f;
            public int displayTime = 60;
            public boolean isAnimated = false;
        }
    }
    
    public static class Animations {
        public boolean enabled = true;
        public int defaultSpeed = 20;
        public boolean enableRainbowColors = true;
        public boolean enableGradients = true;
        public List<Animation> animationList = new ArrayList<>();
        
        public static class Animation {
            public String name = "rainbow_welcome";
            public String type = "rainbow";
            public String text = "Welcome to NeoEssentials Server!";
            public int speed = 20;
            public List<String> colors = new ArrayList<>();
            public boolean loop = true;
        }
    }
    
    public static class Placeholders {
        public boolean enabled = true;
        public int refreshInterval = 20;
        public boolean enableCaching = true;
        public boolean enableDiscordPlaceholders = true;
        public boolean enableFTBPlaceholders = true;
        public Map<String, String> customPlaceholders = new HashMap<>();
    }
}
