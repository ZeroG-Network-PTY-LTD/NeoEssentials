package com.zerog.neoessentials.placeholders;

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
            com.zerog.neoessentials.features.ScoreboardManager.getInstance().updateAllScoreboards();
        }
        LOGGER.info("Custom placeholders reloaded and tablist/scoreboard refreshed for all players.");
    }
    // TPS tracking fields
    private double currentTPS = 20.0;
    private long lastTickTime = System.nanoTime();
    private int tickCount = 0;

    /**
     * Call this from a server tick event to update TPS
     */
    public void onServerTick() {
        tickCount++;
        long now = System.nanoTime();
        if (tickCount >= 20) {
            double seconds = (now - lastTickTime) / 1_000_000_000.0;
            currentTPS = 20.0 / seconds * tickCount;
            lastTickTime = now;
            tickCount = 0;
        }
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
        com.zerog.neoessentials.config.CustomPlaceholderConfig configLoader = com.zerog.neoessentials.config.CustomPlaceholderConfig.getInstance();
        java.nio.file.Path chosenPath = configLoader.getConfigPath();
        boolean fileExists = chosenPath != null && java.nio.file.Files.exists(chosenPath);
        com.zerog.neoessentials.util.DebugUtil.debugLog("[NeoEssentials] PlaceholderManager diagnostics:");
        com.zerog.neoessentials.util.DebugUtil.debugLog("  Chosen config path: " + (chosenPath != null ? chosenPath.toAbsolutePath() : "null") + " exists=" + fileExists);
        if (!fileExists) {
            // Only write default template if neither file exists
            LOGGER.warn("No customPlaceholders.json found. Generating default template at {}.", chosenPath);
            com.zerog.neoessentials.util.DebugUtil.debugLog("  No customPlaceholders.json found. Generating default template at " + chosenPath);
            if (chosenPath != null) {
                try {
                    java.nio.file.Files.writeString(chosenPath, com.zerog.neoessentials.config.CustomPlaceholderConfig.DEFAULT_TEMPLATE);
                    LOGGER.info("Generated default customPlaceholders.json at {}", chosenPath);
                    System.out.println("  Generated default customPlaceholders.json at " + chosenPath);
                } catch (Exception e) {
                    com.zerog.neoessentials.util.ErrorHandler.handleError(
                        com.zerog.neoessentials.util.ErrorHandler.ErrorCategory.FILE_IO,
                        com.zerog.neoessentials.util.ErrorHandler.ErrorSeverity.MEDIUM,
                        "Placeholder Config File Write", e);
                }
            }
        } else if (chosenPath != null) {
            try {
                if (java.nio.file.Files.size(chosenPath) == 0) {
                    LOGGER.warn("customPlaceholders.json exists but is empty at {}. Please manually populate it; no overwrite will occur.", chosenPath);
                    System.out.println("  customPlaceholders.json exists but is empty at " + chosenPath);
                }
            } catch (java.io.IOException e) {
                LOGGER.error("Error checking customPlaceholders.json file size: {}", e.getMessage());
                System.out.println("  ERROR checking customPlaceholders.json file size: " + e.getMessage());
            }
        }
        CustomPlaceholderConfig config = CustomPlaceholderConfig.getInstance();
        com.google.gson.JsonObject root = config.getConfigData();
        if (root == null) {
            LOGGER.error("CustomPlaceholderConfig.getConfigData() returned null. No placeholders loaded.");
            System.out.println("  ERROR: CustomPlaceholderConfig.getConfigData() returned null. No placeholders loaded.");
            return;
        }
        if (!root.has("customPlaceholders")) {
            LOGGER.warn("customPlaceholders.json does not contain 'customPlaceholders' object. No placeholders loaded.");
            System.out.println("  customPlaceholders.json does not contain 'customPlaceholders' object. No placeholders loaded.");
            return;
        }
        com.google.gson.JsonObject placeholdersObj = root.getAsJsonObject("customPlaceholders");
        LOGGER.info("Loading custom placeholders from config. Found {} entries.", placeholdersObj.size());
        System.out.println("  Loading custom placeholders from config. Found " + placeholdersObj.size() + " entries.");
        for (String key : placeholdersObj.keySet()) {
            com.google.gson.JsonObject def = placeholdersObj.getAsJsonObject(key);
            String type = def.has("type") ? def.get("type").getAsString() : "static";
            LOGGER.info("Registering custom placeholder: {} (type: {})", key, type);
            System.out.println("    Registering custom placeholder: " + key + " (type: " + type + ")");
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
                java.util.List<String> frames = new java.util.ArrayList<>();
                if (def.has("frames") && def.get("frames").isJsonArray()) {
                    for (var el : def.getAsJsonArray("frames")) {
                        frames.add(el.getAsString());
                    }
                }
                double interval = def.has("interval") ? def.get("interval").getAsDouble() : 1.0;
                registerPlaceholder(key, new AnimatedPlaceholder(frames, interval));
            } else {
                String value = def.has("value") ? def.get("value").getAsString() : "";
                registerPlaceholder(key, ctx -> processPlaceholders(value, ctx));
            }
        }
        LOGGER.info("Custom placeholder loading complete.");
        System.out.println("  Custom placeholder loading complete.");
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
        private long lastUpdateTs = 0;
        public AnimatedPlaceholder(java.util.List<String> frames, double interval) {
            this.frames = frames;
            this.interval = interval;
            // Removed unused assignment to startTime
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
            if (frames.isEmpty()) return "";
            return frames.get(frameIdx);
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
        
        // Performance placeholders
    registerPlaceholder("server_tps", ctx -> String.format("%.2f", currentTPS));
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
        java.util.Set<String> customPlaceholderNames = com.zerog.neoessentials.config.CustomPlaceholderConfig.getInstance().getCustomPlaceholderNames();
        for (String name : customPlaceholderNames) {
            placeholders.remove(name.toLowerCase());
        }
        
        // Reload custom placeholder config
        com.zerog.neoessentials.config.CustomPlaceholderConfig.getInstance().reloadConfig();
        
        // Load custom placeholders again
        loadCustomPlaceholders();
        
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
