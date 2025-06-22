package com.zerog.neoessentials.ui.tablist;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.EconomyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages placeholders for tablist headers and footers.
 * Provides an extensible system for adding custom placeholders.
 */
public class TablistPlaceholderManager {
    // Regex pattern for placeholders like %player%, {player}, etc.
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("(%|\\{)(\\w+)(}|%)");
    
    // Cache for expensive placeholder values
    private final Map<String, CachedPlaceholderValue> placeholderCache = new ConcurrentHashMap<>();
    
    // Placeholder processors mapped by name
    private final Map<String, PlaceholderProcessor> processors = new HashMap<>();
    
    // Minecraft server instance
    private final MinecraftServer server;
    
    /**
     * Creates a new placeholder manager
     *
     * @param server The Minecraft server instance
     */
    public TablistPlaceholderManager(MinecraftServer server) {
        this.server = server;
        registerDefaultPlaceholders();
    }
    
    /**
     * Registers all default placeholders
     */
    private void registerDefaultPlaceholders() {
        // Server information
        registerPlaceholder("server", (player, arg) -> server.getServerName());
        registerPlaceholder("server_name", (player, arg) -> server.getServerName());
        registerPlaceholder("online", (player, arg) -> String.valueOf(server.getPlayerCount()));
        registerPlaceholder("max", (player, arg) -> String.valueOf(server.getMaxPlayers()));
        registerPlaceholder("tps", (player, arg) -> String.format("%.1f", getAverageTPS()));
        registerPlaceholder("server_tps", (player, arg) -> String.format("%.1f", getAverageTPS()));
        
        // Time and date
        registerPlaceholder("time", (player, arg) -> new SimpleDateFormat("HH:mm:ss").format(new Date()));
        registerPlaceholder("date", (player, arg) -> new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        registerPlaceholder("day", (player, arg) -> new SimpleDateFormat("EEEE").format(new Date()));
        
        // Player information
        registerPlaceholder("player", (player, arg) -> player.getGameProfile().getName());
        registerPlaceholder("displayname", (player, arg) -> player.getDisplayName().getString());
        registerPlaceholder("ping", (player, arg) -> String.valueOf(player.latency));
        registerPlaceholder("health", (player, arg) -> String.format("%.1f", player.getHealth()));
        registerPlaceholder("max_health", (player, arg) -> String.format("%.1f", player.getMaxHealth()));
        
        // Economy (if available)
        registerPlaceholder("balance", (player, arg) -> {
            EconomyManager eco = NeoEssentials.getInstance().getEconomyManager();
            if (eco != null) {
                return String.format("%.2f", eco.getBalance(player.getUUID()));
            }
            return "0.00";
        });
        
        // World information
        registerPlaceholder("world", (player, arg) -> player.level().dimension().location().toString());
        registerPlaceholder("biome", (player, arg) -> player.level().getBiome(player.blockPosition()).unwrapKey()
                .map(key -> key.location().toString())
                .orElse("unknown"));
        
        // Server stats
        registerPlaceholder("memory_used", (player, arg) -> {
            long used = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576L;
            return used + " MB";
        });
        registerPlaceholder("memory_max", (player, arg) -> {
            long max = Runtime.getRuntime().maxMemory() / 1048576L;
            return max + " MB";
        });
        registerPlaceholder("memory_percent", (player, arg) -> {
            long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long max = Runtime.getRuntime().maxMemory();
            return String.format("%.1f%%", (used * 100.0) / max);
        });
        
        NeoEssentials.LOGGER.info("Registered {} default placeholders", processors.size());
    }
    
    /**
     * Registers a new placeholder
     *
     * @param name The name of the placeholder (without % or {})
     * @param processor The function to process the placeholder
     */
    public void registerPlaceholder(String name, BiFunction<ServerPlayer, String, String> processor) {
        processors.put(name.toLowerCase(), new PlaceholderProcessor(processor));
        NeoEssentials.LOGGER.debug("Registered placeholder: {}", name);
    }
    
    /**
     * Processes a text string, replacing all placeholders with their values
     *
     * @param text The text to process
     * @param player The player to process placeholders for
     * @return The processed text with placeholders replaced
     */
    public String processPlaceholders(String text, ServerPlayer player) {
        if (text == null || text.isEmpty() || player == null) {
            return text;
        }
        
        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String placeholderName = matcher.group(2).toLowerCase();
            PlaceholderProcessor processor = processors.get(placeholderName);
            
            if (processor != null) {
                String replacement = processor.process(player, "");
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * Processes a text string into a Component, replacing all placeholders
     * 
     * @param text The text to process
     * @param player The player to process placeholders for
     * @return The processed Component
     */
    public Component processPlaceholdersToComponent(String text, ServerPlayer player) {
        String processed = processPlaceholders(text, player);
        return Component.literal(processed);
    }
    
    /**
     * Clears the placeholder cache
     */
    public void clearCache() {
        placeholderCache.clear();
    }
    
    /**
     * Gets the average TPS (ticks per second) of the server
     *
     * @return The average TPS
     */
    private double getAverageTPS() {
        // NeoForge server TPS calculation
        // This is a simplified version - in a real implementation,
        // you would access the server tick times
        return Math.min(20.0, 20.0);
    }
    
    /**
     * Process color codes in text
     *
     * @param text The text to process
     * @return The text with color codes processed
     */
    public static String formatColors(String text) {
        if (text == null) {
            return "";
        }
        
        char colorChar = '&';
        char[] b = text.toCharArray();
        
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == colorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
                b[i] = '§';
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        
        return new String(b);
    }
    
    /**
     * Converts a String with color codes to a Component
     * 
     * @param text The text to convert
     * @return The Component
     */
    public static Component colorize(String text) {
        return Component.literal(formatColors(text));
    }
    
    /**
     * Removes color codes from text
     * 
     * @param text The text to process
     * @return The text without color codes
     */
    public static String stripColor(String text) {
        if (text == null) {
            return "";
        }
        
        return text.replaceAll("(?i)§[0-9A-FK-OR]", "");
    }
    
    /**
     * Gets a substring of a string
     * 
     * @param text The text to get a substring from
     * @param start The start index
     * @param end The end index
     * @return The substring
     */
    public static String substring(String text, int start, int end) {
        if (text == null) {
            return "";
        }
        
        if (start >= text.length()) {
            return "";
        }
        
        if (end > text.length()) {
            end = text.length();
        }
        
        return text.substring(start, end);
    }
    
    /**
     * Transfers color codes from one string to another
     * 
     * @param source The source string with color codes
     * @param target The target string to apply colors to
     * @return The target string with color codes from the source
     */
    public static String transferColors(String source, String target) {
        if (source == null || target == null) {
            return target == null ? "" : target;
        }
        
        // Find the last color code in the source
        StringBuilder lastColors = new StringBuilder();
        char[] sourceChars = source.toCharArray();
        
        for (int i = 0; i < sourceChars.length - 1; i++) {
            if (sourceChars[i] == '§' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(sourceChars[i + 1]) > -1) {
                lastColors.setLength(0); // Reset the color codes when we find a new one
                lastColors.append('§').append(Character.toLowerCase(sourceChars[i + 1]));
            }
        }
        
        return lastColors + target;
    }
    
    /**
     * Class to represent a placeholder processor
     */
    private static class PlaceholderProcessor {
        private final BiFunction<ServerPlayer, String, String> processor;
        
        public PlaceholderProcessor(BiFunction<ServerPlayer, String, String> processor) {
            this.processor = processor;
        }
        
        public String process(ServerPlayer player, String arg) {
            try {
                return processor.apply(player, arg);
            } catch (Exception e) {
                return "Error";
            }
        }
    }
    
    /**
     * Class to represent a cached placeholder value
     */
    private static class CachedPlaceholderValue {
        private final String value;
        private final long timestamp;
        
        public CachedPlaceholderValue(String value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getValue() {
            return value;
        }
        
        public boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - timestamp > ttlMs;
        }
    }
}
