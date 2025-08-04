package com.zerog.neoessentials.placeholders;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom Placeholder System for NeoEssentials
 * Provides dynamic content replacement for messages, GUIs, and commands
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class PlaceholderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceholderManager.class);
    private static PlaceholderManager instance;
    
    // Pattern to match placeholders: %placeholder% or {placeholder}
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("(%[^%]+%|\\{[^}]+\\})");
    
    // Built-in placeholder functions
    private final Map<String, Function<PlaceholderContext, String>> placeholders;
    
    private PlaceholderManager() {
        this.placeholders = new HashMap<>();
        registerBuiltInPlaceholders();
        LOGGER.info("PlaceholderManager initialized with {} built-in placeholders", placeholders.size());
    }
    
    public static PlaceholderManager getInstance() {
        if (instance == null) {
            instance = new PlaceholderManager();
        }
        return instance;
    }
    
    /**
     * Register built-in placeholders
     */
    private void registerBuiltInPlaceholders() {
        // Player placeholders
        registerPlaceholder("player_name", ctx -> ctx.getPlayer() != null ? ctx.getPlayer().getName().getString() : "Unknown");
        registerPlaceholder("player_displayname", ctx -> ctx.getPlayer() != null ? ctx.getPlayer().getDisplayName().getString() : "Unknown");
        registerPlaceholder("player_level", ctx -> ctx.getPlayer() != null ? String.valueOf(ctx.getPlayer().experienceLevel) : "0");
        registerPlaceholder("player_health", ctx -> ctx.getPlayer() != null ? String.format("%.1f", ctx.getPlayer().getHealth()) : "0");
        registerPlaceholder("player_max_health", ctx -> ctx.getPlayer() != null ? String.format("%.1f", ctx.getPlayer().getMaxHealth()) : "20");
        registerPlaceholder("player_food", ctx -> ctx.getPlayer() != null ? String.valueOf(ctx.getPlayer().getFoodData().getFoodLevel()) : "20");
        registerPlaceholder("player_x", ctx -> ctx.getPlayer() != null ? String.valueOf((int) ctx.getPlayer().getX()) : "0");
        registerPlaceholder("player_y", ctx -> ctx.getPlayer() != null ? String.valueOf((int) ctx.getPlayer().getY()) : "0");
        registerPlaceholder("player_z", ctx -> ctx.getPlayer() != null ? String.valueOf((int) ctx.getPlayer().getZ()) : "0");
        registerPlaceholder("player_world", ctx -> ctx.getPlayer() != null ? ctx.getPlayer().level().dimension().location().getPath() : "unknown");
        
        // Time placeholders
        registerPlaceholder("time", ctx -> LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        registerPlaceholder("date", ctx -> LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        registerPlaceholder("datetime", ctx -> LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // Server placeholders
        registerPlaceholder("server_name", ctx -> "NeoEssentials Server");
        registerPlaceholder("server_version", ctx -> "1.21.1");
        registerPlaceholder("server_players", ctx -> {
            if (ctx.getPlayer() != null && ctx.getPlayer().getServer() != null) {
                return String.valueOf(ctx.getPlayer().getServer().getPlayerCount());
            }
            return "0";
        });
        registerPlaceholder("server_max_players", ctx -> {
            if (ctx.getPlayer() != null && ctx.getPlayer().getServer() != null) {
                return String.valueOf(ctx.getPlayer().getServer().getMaxPlayers());
            }
            return "20";
        });
        
        // World placeholders
        registerPlaceholder("world_time", ctx -> {
            if (ctx.getPlayer() != null) {
                long time = ctx.getPlayer().level().getDayTime() % 24000;
                return String.valueOf(time);
            }
            return "0";
        });
        registerPlaceholder("world_day", ctx -> {
            if (ctx.getPlayer() != null) {
                long day = ctx.getPlayer().level().getDayTime() / 24000;
                return String.valueOf(day);
            }
            return "0";
        });
        registerPlaceholder("world_weather", ctx -> {
            if (ctx.getPlayer() != null) {
                Level level = ctx.getPlayer().level();
                if (level.isThundering()) return "thunder";
                if (level.isRaining()) return "rain";
                return "clear";
            }
            return "clear";
        });
        
        // Performance placeholders
        registerPlaceholder("server_tps", ctx -> {
            // Simplified TPS calculation - in production would use proper server metrics
            return "20.0";
        });
        registerPlaceholder("server_memory_used", ctx -> {
            Runtime runtime = Runtime.getRuntime();
            long used = runtime.totalMemory() - runtime.freeMemory();
            return String.valueOf(used / 1024 / 1024); // MB
        });
        registerPlaceholder("server_memory_total", ctx -> {
            Runtime runtime = Runtime.getRuntime();
            return String.valueOf(runtime.totalMemory() / 1024 / 1024); // MB
        });
        registerPlaceholder("server_memory_percent", ctx -> {
            Runtime runtime = Runtime.getRuntime();
            long used = runtime.totalMemory() - runtime.freeMemory();
            long total = runtime.totalMemory();
            return String.format("%.1f", (double) used / total * 100);
        });
        
        // Color placeholders
        registerPlaceholder("color_red", ctx -> "§c");
        registerPlaceholder("color_green", ctx -> "§a");
        registerPlaceholder("color_blue", ctx -> "§9");
        registerPlaceholder("color_yellow", ctx -> "§e");
        registerPlaceholder("color_gold", ctx -> "§6");
        registerPlaceholder("color_purple", ctx -> "§5");
        registerPlaceholder("color_gray", ctx -> "§7");
        registerPlaceholder("color_white", ctx -> "§f");
        registerPlaceholder("color_black", ctx -> "§0");
        registerPlaceholder("reset", ctx -> "§r");
        registerPlaceholder("bold", ctx -> "§l");
        registerPlaceholder("italic", ctx -> "§o");
        registerPlaceholder("underline", ctx -> "§n");
        
        // Math placeholders
        registerPlaceholder("random_1_10", ctx -> String.valueOf((int) (Math.random() * 10) + 1));
        registerPlaceholder("random_1_100", ctx -> String.valueOf((int) (Math.random() * 100) + 1));
        
        // NeoEssentials specific placeholders
        registerPlaceholder("neoessentials_version", ctx -> "2.1.0");
        registerPlaceholder("neoessentials_features", ctx -> "12");
        registerPlaceholder("neoessentials_commands", ctx -> "50+");
    }
    
    /**
     * Register a custom placeholder
     */
    public void registerPlaceholder(String identifier, Function<PlaceholderContext, String> function) {
        placeholders.put(identifier.toLowerCase(), function);
        LOGGER.debug("Registered placeholder: {}", identifier);
    }
    
    /**
     * Unregister a placeholder
     */
    public void unregisterPlaceholder(String identifier) {
        placeholders.remove(identifier.toLowerCase());
        LOGGER.debug("Unregistered placeholder: {}", identifier);
    }
    
    /**
     * Process placeholders in text
     */
    public String processPlaceholders(String text, ServerPlayer player) {
        return processPlaceholders(text, new PlaceholderContext(player));
    }
    
    /**
     * Process placeholders in text with context
     */
    public String processPlaceholders(String text, PlaceholderContext context) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = resolvePlaceholder(placeholder, context);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Resolve a single placeholder
     */
    private String resolvePlaceholder(String placeholder, PlaceholderContext context) {
        // Remove placeholder delimiters
        String identifier = placeholder;
        if (identifier.startsWith("%") && identifier.endsWith("%")) {
            identifier = identifier.substring(1, identifier.length() - 1);
        } else if (identifier.startsWith("{") && identifier.endsWith("}")) {
            identifier = identifier.substring(1, identifier.length() - 1);
        }
        
        // Handle parameters (e.g., %player_health_formatted:1%)
        String[] parts = identifier.split(":");
        String baseIdentifier = parts[0].toLowerCase();
        String parameter = parts.length > 1 ? parts[1] : null;
        
        Function<PlaceholderContext, String> function = placeholders.get(baseIdentifier);
        if (function != null) {
            try {
                String value = function.apply(context);
                return formatValue(value, parameter);
            } catch (Exception e) {
                LOGGER.warn("Error processing placeholder '{}': {}", identifier, e.getMessage());
                return placeholder; // Return original placeholder on error
            }
        }
        
        // Placeholder not found
        LOGGER.debug("Unknown placeholder: {}", identifier);
        return placeholder; // Return original placeholder
    }
    
    /**
     * Format placeholder value with parameter
     */
    private String formatValue(String value, String parameter) {
        if (parameter == null) {
            return value;
        }
        
        try {
            // Handle decimal places for numbers
            if (parameter.matches("\\d+")) {
                int decimals = Integer.parseInt(parameter);
                double numValue = Double.parseDouble(value);
                return String.format("%." + decimals + "f", numValue);
            }
        } catch (NumberFormatException ignored) {
            // Not a number format parameter
        }
        
        return value;
    }
    
    /**
     * Get all registered placeholder identifiers
     */
    public java.util.Set<String> getRegisteredPlaceholders() {
        return placeholders.keySet();
    }
    
    /**
     * Check if a placeholder is registered
     */
    public boolean isPlaceholderRegistered(String identifier) {
        return placeholders.containsKey(identifier.toLowerCase());
    }
    
    /**
     * Get placeholder count
     */
    public int getPlaceholderCount() {
        return placeholders.size();
    }
    
    /**
     * Context class for placeholder resolution
     */
    public static class PlaceholderContext {
        private final ServerPlayer player;
        private final Map<String, Object> customData;
        
        public PlaceholderContext(ServerPlayer player) {
            this.player = player;
            this.customData = new HashMap<>();
        }
        
        public ServerPlayer getPlayer() {
            return player;
        }
        
        public void setCustomData(String key, Object value) {
            customData.put(key, value);
        }
        
        public Object getCustomData(String key) {
            return customData.get(key);
        }
        
        public Map<String, Object> getAllCustomData() {
            return new HashMap<>(customData);
        }
    }
}
