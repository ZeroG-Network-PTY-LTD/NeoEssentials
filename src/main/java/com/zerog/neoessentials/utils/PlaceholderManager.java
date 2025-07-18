package com.zerog.neoessentials.utils;

import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Placeholder manager for NeoEssentials
 * 
 * Handles placeholder parsing and replacement
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class PlaceholderManager {
    
    private final Map<String, PlaceholderProvider> placeholders;
    private final Pattern placeholderPattern;
    
    public PlaceholderManager() {
        this.placeholders = new HashMap<>();
        this.placeholderPattern = Pattern.compile("\\{([^}]+)\\}");
        
        registerDefaultPlaceholders();
    }
    
    private void registerDefaultPlaceholders() {
        // Server placeholders
        registerPlaceholder("SERVER_NAME", (player) -> "NeoEssentials Server");
        registerPlaceholder("ONLINE_PLAYERS", (player) -> String.valueOf(
            NeoEssentials.getServer() != null ? NeoEssentials.getServer().getPlayerCount() : 0));
        registerPlaceholder("MAX_PLAYERS", (player) -> String.valueOf(
            NeoEssentials.getServer() != null ? NeoEssentials.getServer().getMaxPlayers() : 0));
        
        // Player placeholders
        registerPlaceholder("PLAYER", (player) -> player != null ? player.getScoreboardName() : "Console");
        registerPlaceholder("HEALTH", (player) -> player != null ? String.valueOf((int) player.getHealth()) : "0");
        registerPlaceholder("PING", (player) -> player != null ? String.valueOf(player.connection.latency()) : "0");
        
        NeoEssentials.LOGGER.info("Registered {} default placeholders", placeholders.size());
    }
    
    public void registerPlaceholder(String name, PlaceholderProvider provider) {
        placeholders.put(name.toLowerCase(), provider);
    }
    
    public String parsePlaceholders(String text, ServerPlayer player) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String result = text;
        for (Map.Entry<String, PlaceholderProvider> entry : placeholders.entrySet()) {
            String placeholder = "{" + entry.getKey().toUpperCase() + "}";
            if (result.contains(placeholder)) {
                try {
                    String value = entry.getValue().getValue(player);
                    result = result.replace(placeholder, value != null ? value : "");
                } catch (Exception e) {
                    NeoEssentials.LOGGER.warn("Error parsing placeholder {}: {}", placeholder, e.getMessage());
                }
            }
        }
        
        return result;
    }
    
    @FunctionalInterface
    public interface PlaceholderProvider {
        String getValue(ServerPlayer player);
    }
}
