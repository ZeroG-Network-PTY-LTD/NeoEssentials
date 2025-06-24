package com.zerog.neoessentials.ui.tab.placeholders;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.TablistTomlConfig;
import com.zerog.neoessentials.ui.tab.TabManager;
import com.zerog.neoessentials.ui.tab.TabPlayerData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages placeholder replacement for the TabManager system
 */
public class PlaceholderManager {
    // Interface for placeholder providers
    public interface PlaceholderProvider {
        String getValue(ServerPlayer player, TabPlayerData playerData);
    }
      // Interface for conditional placeholders
    public interface ConditionalPlaceholder {
        boolean matches(ServerPlayer player, TabPlayerData playerData, String condition);
        
        // Default implementation
        default String getResult(ServerPlayer player, TabPlayerData playerData, String condition, String trueResult, String falseResult) {
            return matches(player, playerData, condition) ? trueResult : falseResult;
        }
    }
    
    // Simple implementation of ConditionalPlaceholder with lambda support
    private static class SimpleConditionMatcher implements ConditionalPlaceholder {
        private final ConditionMatcher matcher;
        
        public SimpleConditionMatcher(ConditionMatcher matcher) {
            this.matcher = matcher;
        }
        
        @Override
        public boolean matches(ServerPlayer player, TabPlayerData playerData, String condition) {
            return matcher.matches(player, playerData, condition);
        }
    }
    
    // Functional interface for condition matching
    @FunctionalInterface
    private interface ConditionMatcher {
        boolean matches(ServerPlayer player, TabPlayerData playerData, String condition);
    }
    
    // Interface for placeholder replacements
    public interface PlaceholderReplacement {
        String apply(String input);
    }
    
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^%]+)%");
    private static final Pattern CONDITIONAL_PATTERN = Pattern.compile("%if:([^:]+):([^:]+):([^%]+)%");
    private static final Pattern COLOR_HEX_PATTERN = Pattern.compile("&#[0-9a-fA-F]{6}");
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
    
    // Time-based formatters
    private DateTimeFormatter timeFormatter;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    
    // TabManager reference
    private final TabManager tabManager;
    
    // Registered placeholders
    private final Map<String, PlaceholderProvider> placeholders = new ConcurrentHashMap<>();
    
    // Registered conditional placeholders
    private final Map<String, ConditionalPlaceholder> conditionalPlaceholders = new ConcurrentHashMap<>();
    
    // Registered placeholder output replacements
    private final Map<String, PlaceholderReplacement> outputReplacements = new ConcurrentHashMap<>();
    
    // Server start time for uptime calculation
    private final LocalDateTime serverStartTime = LocalDateTime.now();
    
    /**
     * Creates a new placeholder manager
     * 
     * @param tabManager The TabManager instance
     */
    public PlaceholderManager(TabManager tabManager) {
        this.tabManager = tabManager;
        
        // Set default time formatter
        updateTimeFormatter(TablistTomlConfig.TIME_FORMAT.get());
    }
    
    /**
     * Initialize the placeholder system
     */
    public void initialize() {
        // Register standard placeholders
        registerStandardPlaceholders();
        
        // Register conditional placeholders
        registerConditionalPlaceholders();
        
        // Register output replacements
        registerOutputReplacements();
        
        NeoEssentials.LOGGER.info("PlaceholderManager initialized with {} standard placeholders",
            placeholders.size());
    }
    
    /**
     * Register the standard placeholder providers
     */
    private void registerStandardPlaceholders() {
        // Server information
        register("server", (player, data) -> com.zerog.neoessentials.config.GeneralConfig.SERVER_NAME.get());
        register("online", (player, data) -> String.valueOf(tabManager.getServer().getPlayerCount()));
        register("max", (player, data) -> String.valueOf(tabManager.getServer().getMaxPlayers()));
        register("tps", (player, data) -> {
            MinecraftServer server = tabManager.getServer();
            return server != null ? "20.0" : "0.0"; // Simplified implementation
        });
        register("time", (player, data) -> timeFormatter.format(LocalDateTime.now()));
        register("date", (player, data) -> dateFormat.format(new Date()));
        register("uptime", (player, data) -> formatUptime(serverStartTime));
        
        // Memory stats
        register("memory_used", (player, data) -> String.valueOf(getUsedMemoryMB()));
        register("memory_max", (player, data) -> String.valueOf(getMaxMemoryMB()));
        register("memory_percent", (player, data) -> String.valueOf(getUsedMemoryPercent()) + "%");
        
        // Player information
        register("player", (player, data) -> player.getScoreboardName());
        register("displayname", (player, data) -> player.getDisplayName().getString());
        register("world", (player, data) -> data.getWorld());
        register("ping", (player, data) -> String.valueOf(data.getPing()));
        register("health", (player, data) -> String.format("%.1f", player.getHealth()));
        register("max_health", (player, data) -> String.format("%.1f", player.getMaxHealth()));
        register("food", (player, data) -> String.valueOf(player.getFoodData().getFoodLevel()));
        register("xp", (player, data) -> String.valueOf(player.experienceLevel));
        register("x", (player, data) -> String.format("%.1f", player.getX()));
        register("y", (player, data) -> String.format("%.1f", player.getY()));
        register("z", (player, data) -> String.format("%.1f", player.getZ()));
        register("biome", (player, data) -> player.level().getBiome(player.blockPosition()).unwrapKey().orElse(null).location().getPath());
        
        // Group and vanish info
        register("group", (player, data) -> data.getGroup());
        register("vanished", (player, data) -> data.isVanished() ? "true" : "false");
        
        // Custom data - will be populated by other features
        register("balance", (player, data) -> data.getCustomData("balance", String.class));
        register("playtime", (player, data) -> formatPlaytime(data.getPlaytime()));
        register("nickname", (player, data) -> data.getNickname() != null ? data.getNickname() : player.getScoreboardName());
    }
    
    /**
     * Register conditional placeholder handlers
     */    private void registerConditionalPlaceholders() {
        // Group check - %if:group:admin:ADMIN:Player%
        registerConditional("group", new SimpleConditionMatcher(
            (player, data, condition) -> data.getGroup().equalsIgnoreCase(condition)
        ));
        
        // World check - %if:world:nether:NETHER:OVERWORLD%
        registerConditional("world", new SimpleConditionMatcher(
            (player, data, condition) -> data.getWorld().contains(condition)
        ));
        
        // Permission check - %if:perm:some.permission:Has Perm:No Perm%
        registerConditional("perm", new SimpleConditionMatcher(
            (player, data, condition) -> checkPermission(player, condition)
        ));
        
        // Vanish check - %if:vanished:Hidden:Visible%
        registerConditional("vanished", new SimpleConditionMatcher(
            (player, data, condition) -> data.isVanished()
        ));
    }
    
    /**
     * Register output replacement handlers
     */
    private void registerOutputReplacements() {
        // Color codes (&a, &b, etc.)
        registerReplacement("color_codes", input -> {
            Matcher matcher = COLOR_CODE_PATTERN.matcher(input);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(sb, "§" + matcher.group(1));
            }
            matcher.appendTail(sb);
            return sb.toString();
        });
        
        // Hex colors (&#RRGGBB)
        registerReplacement("hex_colors", input -> {
            Matcher matcher = COLOR_HEX_PATTERN.matcher(input);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String hexColor = matcher.group().substring(1); // Remove & from &#RRGGBB
                matcher.appendReplacement(sb, "§x§" + String.join("§", hexColor.substring(1).split("")));
            }
            matcher.appendTail(sb);
            return sb.toString();
        });
    }
    
    /**
     * Register a new placeholder
     * 
     * @param name The placeholder name
     * @param provider The provider function
     */
    public void register(String name, PlaceholderProvider provider) {
        placeholders.put(name.toLowerCase(), provider);
    }
    
    /**
     * Register a conditional placeholder
     * 
     * @param name The conditional name (e.g., "group", "perm")
     * @param conditional The conditional implementation
     */
    public void registerConditional(String name, ConditionalPlaceholder conditional) {
        conditionalPlaceholders.put(name.toLowerCase(), conditional);
    }
    
    /**
     * Register a placeholder output replacement
     * 
     * @param name The replacement name
     * @param replacement The replacement function
     */
    public void registerReplacement(String name, PlaceholderReplacement replacement) {
        outputReplacements.put(name.toLowerCase(), replacement);
    }
    
    /**
     * Replace all placeholders in a string
     * 
     * @param text The text to process
     * @param player The player context
     * @return The processed text
     */
    public String replacePlaceholders(String text, ServerPlayer player) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        TabPlayerData playerData = tabManager.getPlayerData(player);
        if (playerData == null) {
            return text;
        }
        
        String result = text;
        
        // Process conditional placeholders first
        result = replaceConditionalPlaceholders(result, player, playerData);
        
        // Process standard placeholders
        result = replaceStandardPlaceholders(result, player, playerData);
        
        // Apply output replacements
        result = applyOutputReplacements(result);
        
        return result;
    }
    
    /**
     * Replace conditional placeholders in text
     */
    private String replaceConditionalPlaceholders(String text, ServerPlayer player, TabPlayerData playerData) {
        Matcher matcher = CONDITIONAL_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String type = matcher.group(1).toLowerCase();
            String condition = matcher.group(2);
            String[] results = matcher.group(3).split(":", 2);
            
            String trueResult = results[0];
            String falseResult = results.length > 1 ? results[1] : "";
            
            ConditionalPlaceholder conditional = conditionalPlaceholders.get(type);
            if (conditional != null) {
                String replacement = conditional.getResult(player, playerData, condition, trueResult, falseResult);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(falseResult));
            }
        }
        
        matcher.appendTail(sb);
        return sb.toString();
    }
    
    /**
     * Replace standard placeholders in text
     */
    private String replaceStandardPlaceholders(String text, ServerPlayer player, TabPlayerData playerData) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1).toLowerCase();
            PlaceholderProvider provider = placeholders.get(placeholder);
            
            if (provider != null) {
                try {
                    String replacement = provider.getValue(player, playerData);
                    if (replacement != null) {
                        matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                    } else {
                        matcher.appendReplacement(sb, "");
                    }
                } catch (Exception e) {
                    matcher.appendReplacement(sb, "Error");
                    tabManager.getErrorLogger().logError(
                        "Error processing placeholder %" + placeholder + "%", e);
                }
            } else {
                // Leave placeholder intact if not found
                matcher.appendReplacement(sb, Matcher.quoteReplacement("%" + placeholder + "%"));
            }
        }
        
        matcher.appendTail(sb);
        return sb.toString();
    }
    
    /**
     * Apply all registered output replacements
     */
    private String applyOutputReplacements(String text) {
        String result = text;
        for (PlaceholderReplacement replacement : outputReplacements.values()) {
            try {
                result = replacement.apply(result);
            } catch (Exception e) {
                tabManager.getErrorLogger().logError(
                    "Error applying output replacement", e);
            }
        }
        return result;
    }
    
    /**
     * Updates the time formatter pattern
     * 
     * @param pattern The new pattern
     */
    public void updateTimeFormatter(String pattern) {
        try {
            this.timeFormatter = DateTimeFormatter.ofPattern(pattern);
        } catch (Exception e) {
            this.timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            tabManager.getErrorLogger().logError("Invalid time format pattern: " + pattern, e);
        }
    }
    
    /**
     * Format TPS value nicely
     */
    private String formatTps(double tickTime) {
        // Convert milliseconds per tick to TPS (ticks per second)
        double tps = Math.min(20.0, 1000.0 / Math.max(50.0, tickTime));
        return String.format("%.1f", tps);
    }
    
    /**
     * Format server uptime nicely
     */
    private String formatUptime(LocalDateTime startTime) {
        Duration uptime = Duration.between(startTime, LocalDateTime.now());
        long days = uptime.toDays();
        long hours = uptime.toHours() % 24;
        long minutes = uptime.toMinutes() % 60;
        
        return String.format("%dd %dh %dm", days, hours, minutes);
    }
    
    /**
     * Format playtime in hours and minutes
     */
    private String formatPlaytime(long seconds) {
        if (seconds <= 0) return "0m";
        
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
    
    /**
     * Get used memory in MB
     */
    private int getUsedMemoryMB() {
        long usedBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        return (int) (usedBytes / (1024 * 1024));
    }
    
    /**
     * Get max memory in MB
     */
    private int getMaxMemoryMB() {
        return (int) (Runtime.getRuntime().maxMemory() / (1024 * 1024));
    }
    
    /**
     * Get used memory as percentage
     */
    private int getUsedMemoryPercent() {
        long used = getUsedMemoryMB();
        long max = getMaxMemoryMB();
        return (int) ((used * 100) / max);
    }
    
    /**
     * Check if player has a permission
     * Note: This is a stub that should be replaced with actual permission checking
     */
    private boolean checkPermission(ServerPlayer player, String permission) {
        // TODO: Integrate with permission system
        // For now, we assume ops have all permissions
        return player.hasPermissions(4);
    }
    
    /**
     * Standard conditional placeholder implementation
     */
    private class StandardConditional implements ConditionalPlaceholder {
        private final BiFunction<ServerPlayer, TabPlayerData, Boolean> condition;
        
        public StandardConditional(BiFunction<ServerPlayer, TabPlayerData, Boolean> condition) {
            this.condition = condition;
        }
        
        @Override
        public boolean matches(ServerPlayer player, TabPlayerData playerData, String condition) {
            return this.condition.apply(player, playerData);
        }
        
        @Override
        public String getResult(ServerPlayer player, TabPlayerData playerData, String condition, String trueResult, String falseResult) {
            if (matches(player, playerData, condition)) {
                return trueResult;
            } else {
                return falseResult;
            }
        }
    }
}
