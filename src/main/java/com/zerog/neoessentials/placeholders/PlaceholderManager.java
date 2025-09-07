package com.zerog.neoessentials.placeholders;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.config.CustomPlaceholderConfig;
import com.zerog.neoessentials.integration.FTBIntegrationHelper;
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
     * Tick all animated placeholders to update their frames.
     * Call this from AnimationScheduler.
     */
    // ...existing code...
    /**
     * Tick all animated placeholders to update their frames.
     * Call this from AnimationScheduler.
     */
    public void tickAnimatedPlaceholders(long now) {
        for (Function<PlaceholderContext, String> fn : placeholders.values()) {
            if (fn instanceof AnimatedPlaceholder anim) {
                anim.tick(now);
            }
        }
    }
    /**
     * Reload custom placeholders from config and refresh tablist for all players
     */
    public void reload() {
        loadCustomPlaceholders();
        // After reload, update tablist and header/footer for all online players
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            java.util.List<net.minecraft.server.level.ServerPlayer> players = server.getPlayerList().getPlayers();
            com.zerog.neoessentials.features.TabListManager.getInstance().refreshTablistForAll(players);
            // Scoreboard system removed - keeping only tablist functionality
        }
        LOGGER.info("Custom placeholders reloaded and tablist refreshed for all players.");
    }
    // TPS tracking fields - now using NeoForge's built-in TPS calculation
    private double currentTPS = 20.0;
    private long lastTickTime = System.nanoTime();
    private int tickCount = 0;

    /**
     * Call this from a server tick event to update TPS
     */
    public void onServerTick() {
        // Update our basic counter for fallback
        tickCount++;
        long now = System.nanoTime();
        if (tickCount >= 20) {
            double seconds = (now - lastTickTime) / 1_000_000_000.0;
            currentTPS = tickCount / seconds;
            lastTickTime = now;
            tickCount = 0;
        }
    }

    /**
     * Get accurate TPS using NeoForge's MinecraftServer tick data
     */
    private double getAccurateTPS() {
        try {
            net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) return Math.min(20.0, Math.max(0.0, currentTPS));
            
            // Try to get the server's recent tick times
            long[] recentTps = server.getTickTimesNanos();
            if (recentTps != null && recentTps.length > 0) {
                // Calculate average tick time over recent ticks (last 20-100 ticks)
                long totalTickTime = 0;
                int validTicks = 0;
                
                // Use recent tick data, but not too few samples
                int sampleSize = Math.min(Math.max(20, recentTps.length / 4), 100);
                int start = Math.max(0, recentTps.length - sampleSize);
                
                for (int i = start; i < recentTps.length; i++) {
                    if (recentTps[i] > 0) {
                        totalTickTime += recentTps[i];
                        validTicks++;
                    }
                }
                
                if (validTicks >= 10) { // Need at least 10 valid samples
                    double avgTickTimeNs = (double) totalTickTime / validTicks;
                    double avgTickTimeMs = avgTickTimeNs / 1_000_000.0;
                    
                    // Calculate TPS: ideal tick time is 50ms for 20 TPS
                    // TPS = 1000 / avgTickTimeMs, capped at 20.0
                    double calculatedTPS = Math.min(20.0, 1000.0 / Math.max(1.0, avgTickTimeMs));
                    
                    // Sanity check: TPS should be between 0 and 20
                    if (calculatedTPS >= 0.0 && calculatedTPS <= 20.0) {
                        // Smooth the TPS value to reduce jitter
                        this.currentTPS = (this.currentTPS * 0.8) + (calculatedTPS * 0.2);
                        return Math.round(this.currentTPS * 10.0) / 10.0; // Round to 1 decimal
                    }
                }
            }
            
            // Alternative approach: try to get tick time directly if available
            try {
                // For NeoForge 1.21.1, try alternative methods
                double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
                if (mspt > 0) {
                    double calculatedTPS = Math.min(20.0, 1000.0 / mspt);
                    if (calculatedTPS >= 0.0 && calculatedTPS <= 20.0) {
                        return Math.round(calculatedTPS * 10.0) / 10.0;
                    }
                }
            } catch (Exception e) {
                // Method might not exist in this version
            }
            
        } catch (Exception e) {
            // Fall back to basic calculation if NeoForge method fails
            com.zerog.neoessentials.util.DebugUtil.debugLog("Failed to get accurate TPS, using fallback: " + e.getMessage());
        }
        
        // Return our basic calculation, clamped to reasonable values
        return Math.round(Math.min(20.0, Math.max(0.0, currentTPS)) * 10.0) / 10.0;
    }
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
    
    // Pattern to match conditional statements: {condition: ..., value: '...', else: '...'}
    private static final Pattern CONDITIONAL_PATTERN = Pattern.compile("\\{condition:\\s*([^,]+),\\s*value:\\s*'([^']*)',\\s*else:\\s*'([^']*)'\\}");
    
    // Built-in placeholder functions
    private final Map<String, Function<PlaceholderContext, String>> placeholders;
    

    private PlaceholderManager() {
        this.placeholders = new HashMap<>();
        com.zerog.neoessentials.util.DebugUtil.debugLog("[PlaceholderManager] Initializing...");
        registerBuiltInPlaceholders();
        com.zerog.neoessentials.util.DebugUtil.debugLog("[PlaceholderManager] Built-in placeholders registered: " + placeholders.keySet());
        loadCustomPlaceholders();
        com.zerog.neoessentials.util.DebugUtil.debugLog("[PlaceholderManager] Custom placeholders loaded: " + placeholders.keySet());
        LOGGER.info("PlaceholderManager initialized with {} total placeholders", placeholders.size());

    }

        // Demo: Broadcast animated placeholder to all online players every 0.2s
    /**
     * Call this after all managers are initialized to start animated placeholder refresh.
     */
    public void startAnimatedPlaceholderRefreshForAll() {
        for (String key : placeholders.keySet()) {
            Function<PlaceholderContext, String> fn = placeholders.get(key);
            if (fn instanceof AnimatedPlaceholder) {
                double interval = ((AnimatedPlaceholder) fn).interval;
                try {
                    com.zerog.neoessentials.features.TabListManager tlm = com.zerog.neoessentials.features.TabListManager.getInstance();
                    if (tlm != null) {
                        tlm.startAnimatedPlaceholderRefresh(key, interval);
                    }
                } catch (Exception e) {
                    com.zerog.neoessentials.util.ErrorHandler.handleError(
                        com.zerog.neoessentials.util.ErrorHandler.ErrorCategory.PLACEHOLDER,
                        com.zerog.neoessentials.util.ErrorHandler.ErrorSeverity.MEDIUM,
                        "Animated Placeholder Refresh", e);
                }
            }
        }
    }

    /**
     * Load custom placeholders from CustomPlaceholderConfig
     */
    public void loadCustomPlaceholders() {
        try {
            LOGGER.info("Loading custom placeholders from ConfigManager...");
            
            ConfigManager configManager = ConfigManager.getInstance();
            CustomPlaceholderConfig config = configManager.getCustomPlaceholderConfig();
            
            if (config != null && config.customPlaceholders != null) {
                for (Map.Entry<String, CustomPlaceholderConfig.CustomPlaceholder> entry : config.customPlaceholders.entrySet()) {
                    String placeholderName = entry.getKey();
                    CustomPlaceholderConfig.CustomPlaceholder placeholder = entry.getValue();
                    
                    if (placeholder == null || placeholder.type == null) {
                        LOGGER.warn("Skipping invalid placeholder: {}", placeholderName);
                        continue;
                    }
                    
                    switch (placeholder.type.toLowerCase()) {
                        case "static":
                            if (placeholder.value != null) {
                                registerPlaceholder(placeholderName, ctx -> processPlaceholders(placeholder.value, ctx));
                                LOGGER.debug("Registered static placeholder: {} = {}", placeholderName, placeholder.value);
                            }
                            break;
                            
                        case "animated":
                            if (placeholder.frames != null && placeholder.frames.length > 0) {
                                registerPlaceholder(placeholderName, new CustomAnimatedPlaceholder(
                                    java.util.List.of(placeholder.frames), 
                                    placeholder.interval > 0 ? placeholder.interval : 1.0
                                ));
                                LOGGER.debug("Registered animated placeholder: {} with {} frames", placeholderName, placeholder.frames.length);
                            }
                            break;
                            
                        case "conditional":
                            if (placeholder.condition != null && placeholder.trueValue != null && placeholder.falseValue != null) {
                                registerPlaceholder(placeholderName, ctx -> {
                                    try {
                                        boolean conditionResult = evaluateCondition(placeholder.condition, ctx);
                                        String result = conditionResult ? placeholder.trueValue : placeholder.falseValue;
                                        // Process nested placeholders in the result
                                        return processPlaceholders(result, ctx);
                                    } catch (Exception e) {
                                        LOGGER.error("Error evaluating condition for placeholder {}: {}", placeholderName, e.getMessage());
                                        String fallback = placeholder.falseValue != null ? placeholder.falseValue : "";
                                        return processPlaceholders(fallback, ctx);
                                    }
                                });
                                LOGGER.debug("Registered conditional placeholder: {} with condition: {}", placeholderName, placeholder.condition);
                            }
                            break;
                            
                        default:
                            LOGGER.warn("Unknown placeholder type '{}' for placeholder: {}", placeholder.type, placeholderName);
                            break;
                    }
                }
                LOGGER.info("Successfully loaded {} custom placeholders", config.customPlaceholders.size());
            } else {
                LOGGER.info("No custom placeholders configuration found");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to load custom placeholders: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Evaluate a condition string with support for various operators and boolean logic
     */
    private boolean evaluateCondition(String condition, PlaceholderContext ctx) {
        if (condition == null || condition.isEmpty()) return false;
        
        try {
            // Replace placeholders in condition first
            String expr = processPlaceholders(condition, ctx).trim();
            // Only log in debug mode
            if (Boolean.getBoolean("neoessentials.debug.placeholders")) {
                com.zerog.neoessentials.util.DebugUtil.debugLog("Evaluating condition: '" + condition + "' -> '" + expr + "'");
            }
            
            // Handle boolean values directly
            if ("true".equalsIgnoreCase(expr)) return true;
            if ("false".equalsIgnoreCase(expr)) return false;
            
            // Handle 'is' conditions (e.g., "is FTB_Active")
            if (condition.toLowerCase().startsWith("is ")) {
                String placeholder = condition.substring(3).trim(); // Remove "is " prefix
                String value = resolvePlaceholder("{" + placeholder + "}", ctx);
                return !"".equals(value) && !"false".equalsIgnoreCase(value) && !"0".equals(value);
            }
            
            // Handle numeric comparisons
            if (expr.contains(">=")) {
                String[] parts = expr.split(">=", 2);
                if (parts.length == 2) {
                    double left = parseNumber(parts[0].trim());
                    double right = parseNumber(parts[1].trim());
                    return left >= right;
                }
            } else if (expr.contains("<=")) {
                String[] parts = expr.split("<=", 2);
                if (parts.length == 2) {
                    double left = parseNumber(parts[0].trim());
                    double right = parseNumber(parts[1].trim());
                    return left <= right;
                }
            } else if (expr.contains("!=")) {
                String[] parts = expr.split("!=", 2);
                if (parts.length == 2) {
                    String left = parts[0].trim();
                    String right = parts[1].trim();
                    // Try numeric comparison first
                    try {
                        double leftNum = parseNumber(left);
                        double rightNum = parseNumber(right);
                        return leftNum != rightNum;
                    } catch (NumberFormatException e) {
                        // Fallback to string comparison
                        return !left.equals(right);
                    }
                }
            } else if (expr.contains("==")) {
                String[] parts = expr.split("==", 2);
                if (parts.length == 2) {
                    String left = parts[0].trim();
                    String right = parts[1].trim();
                    // Try numeric comparison first
                    try {
                        double leftNum = parseNumber(left);
                        double rightNum = parseNumber(right);
                        return leftNum == rightNum;
                    } catch (NumberFormatException e) {
                        // Fallback to string comparison
                        return left.equals(right);
                    }
                }
            } else if (expr.contains("<")) {
                String[] parts = expr.split("<", 2);
                if (parts.length == 2) {
                    double left = parseNumber(parts[0].trim());
                    double right = parseNumber(parts[1].trim());
                    return left < right;
                }
            } else if (expr.contains(">")) {
                String[] parts = expr.split(">", 2);
                if (parts.length == 2) {
                    double left = parseNumber(parts[0].trim());
                    double right = parseNumber(parts[1].trim());
                    return left > right;
                }
            }
            
            // If no operators found, try to parse as boolean or number
            try {
                double num = parseNumber(expr);
                return num != 0; // Non-zero is true
            } catch (NumberFormatException e) {
                // Last resort: non-empty string is true
                return !expr.isEmpty();
            }
            
        } catch (Exception e) {
            // Only log condition errors if debug mode is enabled
            if (Boolean.getBoolean("neoessentials.debug.placeholders")) {
                LOGGER.warn("Failed to evaluate condition '{}': {}", condition, e.getMessage());
                com.zerog.neoessentials.util.DebugUtil.debugLog("Condition evaluation error: " + e.getMessage());
            }
        }
        return false;
    }
    
    /**
     * Parse a string as a number, handling various formats
     */
    private double parseNumber(String str) throws NumberFormatException {
        if (str == null || str.isEmpty()) {
            throw new NumberFormatException("Empty string");
        }
        
        // Remove common non-numeric suffixes
        String cleaned = str.replaceAll("(?i)(ms|px|%|°)$", "").trim();
        
        // Handle boolean strings
        if ("true".equalsIgnoreCase(cleaned)) return 1.0;
        if ("false".equalsIgnoreCase(cleaned)) return 0.0;
        
        return Double.parseDouble(cleaned);
    }

    /**
     * Animated placeholder implementation
     */
    public static class AnimatedPlaceholder implements java.util.function.Function<PlaceholderContext, String> {
        private final java.util.List<String> frames;
        public final double interval;
    // ...existing code...
        private int frameIdx = 0;
        private long lastUpdateTs = System.currentTimeMillis();
        
        public AnimatedPlaceholder(java.util.List<String> frames, double interval) {
            this.frames = frames;
            this.interval = interval;
            this.lastUpdateTs = System.currentTimeMillis(); // Initialize with current time
        }
        
        public void tick(long now) {
            long intervalMs = (long)(interval * 1000);
            if (frames.size() > 1 && now - lastUpdateTs >= intervalMs) {
                frameIdx = (frameIdx + 1) % frames.size();
                lastUpdateTs = now;
            }
        }
        
        @Override
        public String apply(PlaceholderContext ctx) {
            // Also tick during apply to ensure animation works even if server tick is missed
            tick(System.currentTimeMillis());
            if (frames.isEmpty()) return "";
            return frames.get(frameIdx);
        }
    }
    
    /**
     * Custom animated placeholder for JSON config that supports nested placeholders
     */
    public static class CustomAnimatedPlaceholder extends AnimatedPlaceholder {
        public CustomAnimatedPlaceholder(java.util.List<String> frames, double interval) {
            super(frames, interval);
        }
        
        @Override
        public String apply(PlaceholderContext ctx) {
            String frame = super.apply(ctx);
            // Process nested placeholders in the current frame  
            // Need to get the PlaceholderManager instance to avoid recursion
            PlaceholderManager pm = PlaceholderManager.getInstance();
            if (pm != null && frame != null && !frame.isEmpty()) {
                return pm.processPlaceholders(frame, ctx);
            }
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
        registerPlaceholder("player_ping", ctx -> {
            if (ctx.getPlayer() != null) {
                return String.valueOf(ctx.getPlayer().connection.latency());
            }
            return "0";
        });
        
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
        
        // Performance placeholders - Enhanced with accurate calculations
        registerPlaceholder("server_tps", ctx -> String.format("%.1f", getAccurateTPS()));
        registerPlaceholder("server_tps_colored", ctx -> {
            double tps = getAccurateTPS();
            String color;
            if (tps >= 18.0) {
                color = "§a"; // Green for good TPS
            } else if (tps >= 15.0) {
                color = "§e"; // Yellow for moderate TPS
            } else if (tps >= 10.0) {
                color = "§6"; // Orange for poor TPS
            } else {
                color = "§c"; // Red for critical TPS
            }
            return color + String.format("%.1f", tps);
        });
        registerPlaceholder("server_mspt", ctx -> {
            double tps = getAccurateTPS();
            double mspt = tps > 0 ? 1000.0 / tps : 50.0;
            return String.format("%.1f", mspt);
        });
        registerPlaceholder("server_performance", ctx -> {
            double tps = getAccurateTPS();
            if (tps >= 19.5) return "§a§lEXCELLENT";
            if (tps >= 18.0) return "§a§lGOOD";
            if (tps >= 15.0) return "§e§lFAIR";
            if (tps >= 10.0) return "§6§lPOOR";
            return "§c§lCRITICAL";
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
        
        // Animated placeholders for testing
        registerPlaceholder("server_status", new AnimatedPlaceholder(
            java.util.List.of("§a●", "§e●", "§6●", "§c●"), 0.5
        ));
        registerPlaceholder("loading_dots", new AnimatedPlaceholder(
            java.util.List.of("§7.", "§7..", "§7...", "§7"), 0.3
        ));
        registerPlaceholder("rainbow_star", new AnimatedPlaceholder(
            java.util.List.of("§c★", "§6★", "§e★", "§a★", "§b★", "§9★", "§5★"), 0.2
        ));
        
        // FTB Integration placeholders
        registerFTBPlaceholders();
        
        // Essentials AFK placeholder (returns true/false or custom value)
        registerPlaceholder("essentials_afk", ctx -> {
            // Replace with your AFK detection logic
            // Example: return ctx.getPlayer().getData("afk") ? "true" : "false";
            // For now, always return "false" (not AFK)
            return "false";
        });
    }
    
    /**
     * Register FTB-specific placeholders for Teams, Ranks, and Chunks integration
     */
    private void registerFTBPlaceholders() {
        if (!FTBIntegrationHelper.isFTBTeamsLoaded() && !FTBIntegrationHelper.isFTBRanksLoaded()) {
            return; // Skip FTB placeholders if neither FTB Teams nor Ranks are available
        }
        
        // FTB Teams placeholders
        registerPlaceholder("ftb_team_name", ctx -> {
            if (ctx.getPlayer() != null) {
                FTBIntegrationHelper.TeamInfo teamInfo = FTBIntegrationHelper.getTeamInfo(ctx.getPlayer());
                return teamInfo != null ? teamInfo.teamName : "";
            }
            return "";
        });
        
        registerPlaceholder("ftb_team_display_name", ctx -> {
            if (ctx.getPlayer() != null) {
                FTBIntegrationHelper.TeamInfo teamInfo = FTBIntegrationHelper.getTeamInfo(ctx.getPlayer());
                return teamInfo != null ? teamInfo.teamDisplayName : "";
            }
            return "";
        });
        
        registerPlaceholder("ftb_team_role", ctx -> {
            if (ctx.getPlayer() != null) {
                FTBIntegrationHelper.TeamInfo teamInfo = FTBIntegrationHelper.getTeamInfo(ctx.getPlayer());
                if (teamInfo != null) {
                    if (teamInfo.isTeamOwner) return "Owner";
                    if (teamInfo.isTeamModerator) return "Moderator";
                    return "Member";
                }
            }
            return "";
        });
        
        registerPlaceholder("ftb_team_members", ctx -> {
            if (ctx.getPlayer() != null) {
                FTBIntegrationHelper.TeamInfo teamInfo = FTBIntegrationHelper.getTeamInfo(ctx.getPlayer());
                return teamInfo != null ? String.valueOf(teamInfo.teamMembers.size()) : "0";
            }
            return "0";
        });
        
        registerPlaceholder("ftb_team_prefix", ctx -> {
            if (ctx.getPlayer() != null) {
                FTBIntegrationHelper.TeamInfo teamInfo = FTBIntegrationHelper.getTeamInfo(ctx.getPlayer());
                return teamInfo != null ? teamInfo.teamPrefix : "";
            }
            return "";
        });
        
        registerPlaceholder("ftb_team_suffix", ctx -> {
            if (ctx.getPlayer() != null) {
                FTBIntegrationHelper.TeamInfo teamInfo = FTBIntegrationHelper.getTeamInfo(ctx.getPlayer());
                return teamInfo != null ? teamInfo.teamSuffix : "";
            }
            return "";
        });
        
        registerPlaceholder("ftb_team_color", ctx -> {
            if (ctx.getPlayer() != null) {
                FTBIntegrationHelper.TeamInfo teamInfo = FTBIntegrationHelper.getTeamInfo(ctx.getPlayer());
                return teamInfo != null ? teamInfo.teamColor : "";
            }
            return "";
        });
        
        // FTB Ranks placeholders
        registerPlaceholder("ftb_rank_name", ctx -> {
            if (ctx.getPlayer() != null) {
                FTBIntegrationHelper.RankInfo rankInfo = FTBIntegrationHelper.getRankInfo(ctx.getPlayer());
                return rankInfo != null ? rankInfo.rankName : "";
            }
            return "";
        });
        
        registerPlaceholder("ftb_rank_display_name", ctx -> {
            if (ctx.getPlayer() != null) {
                FTBIntegrationHelper.RankInfo rankInfo = FTBIntegrationHelper.getRankInfo(ctx.getPlayer());
                return rankInfo != null ? rankInfo.rankDisplayName : "";
            }
            return "";
        });
        
        registerPlaceholder("ftb_rank_prefix", ctx -> {
            if (ctx.getPlayer() != null) {
                FTBIntegrationHelper.RankInfo rankInfo = FTBIntegrationHelper.getRankInfo(ctx.getPlayer());
                return rankInfo != null ? rankInfo.rankPrefix : "";
            }
            return "";
        });
        
        registerPlaceholder("ftb_rank_suffix", ctx -> {
            if (ctx.getPlayer() != null) {
                FTBIntegrationHelper.RankInfo rankInfo = FTBIntegrationHelper.getRankInfo(ctx.getPlayer());
                return rankInfo != null ? rankInfo.rankSuffix : "";
            }
            return "";
        });
        
        registerPlaceholder("ftb_rank_color", ctx -> {
            if (ctx.getPlayer() != null) {
                FTBIntegrationHelper.RankInfo rankInfo = FTBIntegrationHelper.getRankInfo(ctx.getPlayer());
                return rankInfo != null ? rankInfo.rankColor : "";
            }
            return "";
        });
        
        registerPlaceholder("ftb_rank_weight", ctx -> {
            if (ctx.getPlayer() != null) {
                FTBIntegrationHelper.RankInfo rankInfo = FTBIntegrationHelper.getRankInfo(ctx.getPlayer());
                return rankInfo != null ? String.valueOf(rankInfo.rankWeight) : "0";
            }
            return "0";
        });
        
        registerPlaceholder("ftb_rank_permissions", ctx -> {
            if (ctx.getPlayer() != null) {
                FTBIntegrationHelper.RankInfo rankInfo = FTBIntegrationHelper.getRankInfo(ctx.getPlayer());
                return rankInfo != null ? String.valueOf(rankInfo.rankPermissions.size()) : "0";
            }
            return "0";
        });
        
        // Combined FTB placeholders (for integration with existing systems)
        registerPlaceholder("ftb_combined_prefix", ctx -> {
            if (ctx.getPlayer() != null) {
                return FTBIntegrationHelper.getEffectivePrefix(ctx.getPlayer());
            }
            return "";
        });
        
        registerPlaceholder("ftb_combined_suffix", ctx -> {
            if (ctx.getPlayer() != null) {
                return FTBIntegrationHelper.getEffectiveSuffix(ctx.getPlayer());
            }
            return "";
        });
        
        // FTB Chunks placeholders (if available)
        registerPlaceholder("ftb_chunks_claimed", ctx -> {
            if (ctx.getPlayer() != null) {
                return String.valueOf(FTBIntegrationHelper.getClaimedChunksCount(ctx.getPlayer()));
            }
            return "0";
        });
        
        registerPlaceholder("ftb_chunks_loaded", ctx -> {
            if (ctx.getPlayer() != null) {
                return String.valueOf(FTBIntegrationHelper.getLoadedChunksCount(ctx.getPlayer()));
            }
            return "0";
        });
        
        // Status placeholders
        registerPlaceholder("ftb_has_team", ctx -> {
            if (ctx.getPlayer() != null) {
                FTBIntegrationHelper.TeamInfo teamInfo = FTBIntegrationHelper.getTeamInfo(ctx.getPlayer());
                return teamInfo != null ? "true" : "false";
            }
            return "false";
        });
        
        registerPlaceholder("ftb_has_rank", ctx -> {
            if (ctx.getPlayer() != null) {
                FTBIntegrationHelper.RankInfo rankInfo = FTBIntegrationHelper.getRankInfo(ctx.getPlayer());
                return rankInfo != null ? "true" : "false";
            }
            return "false";
        });
        
                // Legacy aliases for compatibility
        registerPlaceholder("team_name", ctx -> {
            if (ctx.getPlayer() != null) {
                FTBIntegrationHelper.TeamInfo teamInfo = FTBIntegrationHelper.getTeamInfo(ctx.getPlayer());
                return teamInfo != null ? teamInfo.teamName : "";
            }
            return "";
        });
        
        // Status placeholder for conditional usage
        registerPlaceholder("FTB_Active", ctx -> {
            boolean teamsActive = FTBIntegrationHelper.isFTBTeamsLoaded();
            boolean ranksActive = FTBIntegrationHelper.isFTBRanksLoaded();
            return (teamsActive || ranksActive) ? "true" : "false";
        });
        
        registerPlaceholder("rank_name", ctx -> {
            if (ctx.getPlayer() != null) {
                FTBIntegrationHelper.RankInfo rankInfo = FTBIntegrationHelper.getRankInfo(ctx.getPlayer());
                return rankInfo != null ? rankInfo.rankName : "";
            }
            return "";
        });
        
        registerPlaceholder("team_role", ctx -> {
            if (ctx.getPlayer() != null) {
                FTBIntegrationHelper.TeamInfo teamInfo = FTBIntegrationHelper.getTeamInfo(ctx.getPlayer());
                if (teamInfo != null) {
                    if (teamInfo.isTeamOwner) return "Owner";
                    if (teamInfo.isTeamModerator) return "Moderator";
                    return "Member";
                }
            }
            return "";
        });
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
        // Only log if debug placeholders is specifically enabled
        if (Boolean.getBoolean("neoessentials.debug.placeholders")) {
            com.zerog.neoessentials.util.DebugUtil.debugLog("[PlaceholderManager] processPlaceholders called: input='" + text + "'");
        }
        String result = processPlaceholders(text, new PlaceholderContext(player));
        if (Boolean.getBoolean("neoessentials.debug.placeholders")) {
            com.zerog.neoessentials.util.DebugUtil.debugLog("[PlaceholderManager] processPlaceholders output: '" + result + "'");
        }
        return result;
    }
    
    /**
     * Process placeholders in text with context
     */
    public String processPlaceholders(String text, PlaceholderContext context) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // First process conditional statements
        text = processConditionals(text, context);
        
        // Then process regular placeholders
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = resolvePlaceholder(placeholder, context);
            // Only log placeholder processing if debug mode is enabled
            if (Boolean.getBoolean("neoessentials.debug.placeholders")) {
                LOGGER.debug("Processing placeholder: '{}' -> '{}'", placeholder, replacement);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * Process conditional statements in text
     * Handles syntax: {condition: CONDITION, value: 'TRUE_VALUE', else: 'FALSE_VALUE'}
     */
    private String processConditionals(String text, PlaceholderContext context) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        Matcher matcher = CONDITIONAL_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String condition = matcher.group(1).trim();
            String trueValue = matcher.group(2);
            String falseValue = matcher.group(3);
            
            // Evaluate the condition
            boolean conditionResult = evaluateCondition(condition, context);
            String replacement = conditionResult ? trueValue : falseValue;
            
            if (Boolean.getBoolean("neoessentials.debug.placeholders")) {
                LOGGER.debug("Processing conditional: condition='{}' -> {} -> '{}'", condition, conditionResult, replacement);
            }
            
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * Resolve a single placeholder
     */
    private String resolvePlaceholder(String placeholder, PlaceholderContext context) {
        // Only log if debug placeholders is specifically enabled
        if (Boolean.getBoolean("neoessentials.debug.placeholders")) {
            com.zerog.neoessentials.util.DebugUtil.debugLog("[PlaceholderManager] resolvePlaceholder called: '" + placeholder + "'");
        }
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
                // Only log placeholder processing errors if debug logging is enabled
                if (Boolean.getBoolean("neoessentials.debug.placeholders")) {
                    LOGGER.warn("Error processing placeholder '{}': {}", identifier, e.getMessage());
                }
                return placeholder; // Return original placeholder on error
            }
        }
        
        // Placeholder not found - only log if debug mode enabled to reduce spam
        if (Boolean.getBoolean("neoessentials.debug.placeholders")) {
            LOGGER.warn("[PlaceholderManager] Placeholder not registered: '{}'", baseIdentifier);
        }
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
     * Returns a set of animated placeholder IDs found in the given text.
     */
    public java.util.Set<String> getAnimatedPlaceholderIdsInText(String text) {
        java.util.Set<String> result = new java.util.HashSet<>();
        if (text == null || text.isEmpty()) return result;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String identifier = placeholder;
            if (identifier.startsWith("%") && identifier.endsWith("%")) {
                identifier = identifier.substring(1, identifier.length() - 1);
            } else if (identifier.startsWith("{") && identifier.endsWith("}")) {
                identifier = identifier.substring(1, identifier.length() - 1);
            }
            Function<PlaceholderContext, String> fn = placeholders.get(identifier.toLowerCase());
            if (fn instanceof AnimatedPlaceholder) {
                result.add(identifier.toLowerCase());
            }
        }
        return result;
    }

    /**
     * Returns the animation interval for a given animated placeholder ID, or 1.0s if not found.
     */
    public double getAnimationInterval(String placeholderId) {
        Function<PlaceholderContext, String> fn = placeholders.get(placeholderId.toLowerCase());
        if (fn instanceof AnimatedPlaceholder) {
            return ((AnimatedPlaceholder) fn).interval;
        }
        return 1.0;
    }
    
    /**
     * Reload custom placeholders from configuration
     */
    public void reloadCustomPlaceholders() {
        LOGGER.info("Reloading custom placeholders...");
        
        // Clear existing custom placeholders (keep built-in ones)
        try {
            ConfigManager.getInstance().reloadAll();
            loadCustomPlaceholders(); // Re-register placeholders
        } catch (Exception e) {
            LOGGER.error("Failed to reload custom placeholders: {}", e.getMessage());
        }
        
        LOGGER.info("Custom placeholders reloaded. Total placeholders: {}", placeholders.size());
    }
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

    /**
     * Returns the path to the customPlaceholders.json config file (workspace path)
     */
}
