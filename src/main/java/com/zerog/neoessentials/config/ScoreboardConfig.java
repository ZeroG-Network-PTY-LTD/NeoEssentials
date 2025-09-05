package com.zerog.neoessentials.config;

import java.util.List;
import java.util.ArrayList;

/**
 * Clean Scoreboard Configuration for NeoEssentials
 * Represents the scoreboard.json file structure
 */
public class ScoreboardConfig {
    
    public Scoreboard scoreboard = new Scoreboard();
    
    public static class Scoreboard {
        public boolean enabled = true;
        public int updateInterval = 20;
        public int maxLines = 15;
        public String title = "&6&lNeoEssentials Server";
        public List<Layout> layouts = new ArrayList<>();
        
        public Scoreboard() {
            // Initialize with basic admin-friendly layouts
            Layout defaultLayout = new Layout();
            defaultLayout.priority = 1;
            defaultLayout.conditionType = "default";
            defaultLayout.title = "&7&lPLAYER INFO";
            defaultLayout.lines = List.of(
                "&7Player: &f{player_name}",
                "&7Health: &c{player_health}",
                "&7Level: &a{player_level}",
                "&7Online: &e{server_players}"
            );
            layouts.add(defaultLayout);
        }
        
        public static class Layout {
            public int priority;
            public String conditionType;
            public String condition;
            public String title;
            public List<String> lines;
        }
    }
}
