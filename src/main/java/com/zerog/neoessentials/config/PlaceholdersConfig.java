package com.zerog.neoessentials.config;

import java.util.Map;
import java.util.HashMap;

/**
 * Placeholders Configuration for NeoEssentials
 * Represents the placeholders.json file structure
 */
public class PlaceholdersConfig {
    
    public Settings settings = new Settings();
    public Map<String, String> placeholders = new HashMap<>();
    
    public PlaceholdersConfig() {
        // Initialize with default placeholders
        placeholders.put("server_name", "NeoEssentials Server");
        placeholders.put("server_players", "Online Players Count");
        placeholders.put("server_max_players", "Maximum Players");
        placeholders.put("server_tps", "Server TPS");
        placeholders.put("player_name", "Player Name");
        placeholders.put("player_health", "Player Health");
        placeholders.put("player_ping", "Player Ping");
        placeholders.put("discord_members", "Discord Member Count");
        placeholders.put("discord_online", "Discord Online Count");
        placeholders.put("ftb_team_name", "FTB Team Name");
        placeholders.put("ftb_rank_name", "FTB Rank Name");
    }
    
    public static class Settings {
        public boolean enabled = true;
        public int refreshInterval = 20;
        public boolean enableCaching = true;
        public boolean enableDiscordPlaceholders = true;
        public boolean enableFTBPlaceholders = true;
    }
}
