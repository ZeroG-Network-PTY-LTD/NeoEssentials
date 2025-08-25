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
    /**
     * Example customPlaceholders.json config:
     *
     * {
     *   "customPlaceholders": {
     *     "afk_tag": {
     *       "type": "conditional",
     *       "condition": "${player essentials_afk}",
     *       "true": "&7|&oaway",
     *       "false": ""
     *     },
     *     "viewer_colored_ping": {
     *       "type": "conditional",
     *       "condition": "${viewer ping} < 150",
     *       "true": "${viewer_colored_ping0}",
     *       "false": "&c${viewer ping}"
     *     },
     *     "viewer_colored_ping0": {
     *       "type": "conditional",
     *       "condition": "${viewer ping} < 50",
     *       "true": "&a${viewer ping}",
     *       "false": "&e${viewer ping}"
     *     },
     *     "welcome_animation": {
     *       "type": "animated",
     *       "frames": [
     *         "&cWelcome &f${viewer name}",
     *         "&eW&celcome &f${viewer name}",
     *         "... more frames ..."
     *       ],
     *       "interval": 0.2
     *     },
     *     "static_example": {
     *       "type": "static",
     *       "value": "This is a static placeholder value."
     *     }
     *   }
     * }
     *
     * Place this file in config/neoessentials/customPlaceholders.json.
     * All placeholders are available in tablist, chat, GUIs, etc. as ${placeholder_name}.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceholderManager.class);
    private static PlaceholderManager instance;
    
    // Pattern to match placeholders: %placeholder% or {placeholder}
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("(%[^%]+%|\\{[^}]+\\})");
    
    // Built-in placeholder functions
    private final Map<String, Function<PlaceholderContext, String>> placeholders;
    
    private PlaceholderManager() {
        this.placeholders = new HashMap<>();
        registerBuiltInPlaceholders();
        loadCustomPlaceholders();
        LOGGER.info("PlaceholderManager initialized with {} built-in placeholders", placeholders.size());
    }
    /**
     * Load custom placeholders from config/neoessentials/customPlaceholders.json
     */
    private void loadCustomPlaceholders() {
        java.nio.file.Path configDir = java.nio.file.Paths.get("config", "neoessentials");
        java.nio.file.Path configPath = configDir.resolve("customPlaceholders.json");
        if (!java.nio.file.Files.exists(configPath)) {
            try {
                LOGGER.info("customPlaceholders.json not found at {}. Attempting to create directory {}...", configPath, configDir);
                java.nio.file.Files.createDirectories(configDir);
                LOGGER.info("Directory created or already exists: {}", configDir);
                // Write default file
                String defaultJson = "{\n  \"info\": \"Edit or add custom placeholders here. Use type: static, conditional, or animated. Example usage: ${afk_tag}, ${player_prefix}, etc. Conditional placeholders use 'condition', animated use 'frames'. See documentation for details.\",\n  \"customPlaceholders\": {\n    \"afk_tag\": {\n      \"type\": \"conditional\",\n      \"condition\": \"${player essentials_afk}\",\n      \"true\": \"&7|&oaway\",\n      \"false\": \"\"\n    },\n    \"welcome_animation\": {\n      \"type\": \"animated\",\n      \"frames\": [\n        \"&cWelcome &f${viewer name}\",\n        \"&eW&celcome &f${viewer name}\"\n      ],\n      \"interval\": 0.2\n    },\n    \"static_example\": {\n      \"type\": \"static\",\n      \"value\": \"This is a static placeholder value.\"\n    },\n    \"player_prefix\": {\n      \"type\": \"static\",\n      \"value\": \"&7[Player]\"\n    },\n    \"player_suffix\": {\n      \"type\": \"static\",\n      \"value\": \"&f\"\n    },\n    \"player_status\": {\n      \"type\": \"conditional\",\n      \"condition\": \"${player online}\",\n      \"true\": \"&aOnline\",\n      \"false\": \"&cOffline\"\n    },\n    \"player_rank\": {\n      \"type\": \"static\",\n      \"value\": \"Member\"\n    },\n    \"player_points\": {\n      \"type\": \"static\",\n      \"value\": \"0\"\n    },\n    \"player_joined\": {\n      \"type\": \"static\",\n      \"value\": \"${player name} joined the server!\"\n    }\n  }\n}";
                java.nio.file.Files.writeString(configPath, defaultJson);
                LOGGER.info("Successfully generated default customPlaceholders.json at {}", configPath);
            } catch (Exception e) {
                LOGGER.error("Failed to generate default customPlaceholders.json at {}: {}", configPath, e);
                return;
            }
        } else {
            LOGGER.info("customPlaceholders.json already exists at {}. Skipping generation.", configPath);
        }
        try {
            String json = new String(java.nio.file.Files.readAllBytes(configPath));
            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("customPlaceholders")) return;
            com.google.gson.JsonObject placeholdersObj = root.getAsJsonObject("customPlaceholders");
            for (String key : placeholdersObj.keySet()) {
                com.google.gson.JsonObject def = placeholdersObj.getAsJsonObject(key);
                String type = def.has("type") ? def.get("type").getAsString() : "static";
                if ("conditional".equals(type)) {
                    String condition = def.has("condition") ? def.get("condition").getAsString() : "";
                    String trueValue = def.has("true") ? def.get("true").getAsString() : "";
                    String falseValue = def.has("false") ? def.get("false").getAsString() : "";
                    registerPlaceholder(key, ctx -> {
                        boolean cond = evaluateCondition(condition, ctx);
                        String value = cond ? trueValue : falseValue;
                        return processPlaceholders(value, ctx);
                    });
                } else if ("animated".equals(type)) {
                    // Animated placeholder: cycles through frames
                    java.util.List<String> frames = new java.util.ArrayList<>();
                    if (def.has("frames") && def.get("frames").isJsonArray()) {
                        for (var el : def.getAsJsonArray("frames")) {
                            frames.add(el.getAsString());
                        }
                    }
                    double interval = def.has("interval") ? def.get("interval").getAsDouble() : 1.0;
                    registerPlaceholder(key, new AnimatedPlaceholder(frames, interval));
                } else {
                    // Static placeholder
                    String value = def.has("value") ? def.get("value").getAsString() : "";
                    registerPlaceholder(key, ctx -> processPlaceholders(value, ctx));
                }
                LOGGER.info("Registered custom placeholder: {} (type: {})", key, type);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load custom placeholders: {}", e.getMessage());
        }
    }

    /**
     * Evaluate a simple condition string, e.g. "${player ping} < 150"
     */
    private boolean evaluateCondition(String condition, PlaceholderContext ctx) {
        if (condition == null || condition.isEmpty()) return false;
        // Replace placeholders in condition
        String expr = processPlaceholders(condition, ctx);
    expr = expr.replaceAll("[^0-9<>=!.]+", "").trim();
        try {
            if (expr.contains("<")) {
                String[] parts = expr.split("<");
                double left = Double.parseDouble(parts[0].trim());
                double right = Double.parseDouble(parts[1].trim());
                return left < right;
            } else if (expr.contains(">")) {
                String[] parts = expr.split(">");
                double left = Double.parseDouble(parts[0].trim());
                double right = Double.parseDouble(parts[1].trim());
                return left > right;
            } else if (expr.contains("==")) {
                String[] parts = expr.split("==");
                return parts[0].trim().equals(parts[1].trim());
            } else if (expr.contains("!=")) {
                String[] parts = expr.split("!=");
                return !parts[0].trim().equals(parts[1].trim());
            } else if (expr.equals("true")) {
                return true;
            } else if (expr.equals("false")) {
                return false;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to evaluate condition: {}", condition);
        }
        return false;
    }

    /**
     * Animated placeholder implementation
     */
    private static class AnimatedPlaceholder implements java.util.function.Function<PlaceholderContext, String> {
        private final java.util.List<String> frames;
        private final double interval;
        private long startTime;
        public AnimatedPlaceholder(java.util.List<String> frames, double interval) {
            this.frames = frames;
            this.interval = interval;
            this.startTime = System.currentTimeMillis();
        }
        @Override
        public String apply(PlaceholderContext ctx) {
            if (frames.isEmpty()) return "";
            long now = System.currentTimeMillis();
            int idx = (int)(((now - startTime) / 1000.0 / interval) % frames.size());
            String frame = frames.get(idx);
            return frame;
        }
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
        // Aliases for chat formatting compatibility
        registerPlaceholder("player", ctx -> ctx.getPlayer() != null ? ctx.getPlayer().getName().getString() : "Unknown");
        registerPlaceholder("prefix", ctx -> {
            // Replace with your prefix logic, e.g. from permissions manager
            return com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance().getPlayerPrefix(ctx.getPlayer().getUUID());
        });
        registerPlaceholder("suffix", ctx -> {
            // Replace with your suffix logic, e.g. from permissions manager
            return com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance().getPlayerSuffix(ctx.getPlayer().getUUID());
        });
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
            if (ctx.getPlayer() != null) {
                var server = ctx.getPlayer().getServer();
                if (server != null) {
                    return String.valueOf(server.getPlayerCount());
                }
            }
            return "0";
        });
        registerPlaceholder("server_max_players", ctx -> {
            if (ctx.getPlayer() != null) {
                var server = ctx.getPlayer().getServer();
                if (server != null) {
                    return String.valueOf(server.getMaxPlayers());
                }
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
