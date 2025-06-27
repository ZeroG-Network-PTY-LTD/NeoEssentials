package com.zerog.neoessentials.ui.tablist;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.EconomyManager;
import com.zerog.neoessentials.ui.tablist.placeholders.CustomPlaceholder;
import com.zerog.neoessentials.ui.tablist.placeholders.CustomPlaceholderRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

/**
 * Manages placeholders for tablist headers and footers.
 * Provides an extensible system for adding custom placeholders.
 */
public class TablistPlaceholderManager {
    // Regex pattern for placeholders like %player%, {player}, etc.
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("(%|\\{)(\\w+)(}|%)");
    
    // Regex pattern for animation placeholders like {animation:name}, <anim:name>
    private static final Pattern ANIMATION_PATTERN = Pattern.compile("(\\{animation:|<anim:)([^}]+)(}|>)");
    
    // Cache for expensive placeholder values
    private final Map<String, CachedPlaceholderValue> placeholderCache = new ConcurrentHashMap<>();
      // Placeholder processors mapped by name
    private final Map<String, PlaceholderProcessor> processors = new HashMap<>();
    
    // Custom placeholder registry
    private final CustomPlaceholderRegistry customPlaceholderRegistry = new CustomPlaceholderRegistry();
    
    // Animation manager reference for processing animation placeholders
    private TablistAnimationManager animationManager;
    
    // Minecraft server instance
    private MinecraftServer server;
    
    /**
     * Creates a new placeholder manager
     *
     * @param server The Minecraft server instance
     */
    public TablistPlaceholderManager(MinecraftServer server) {
        this.server = server;
        registerDefaultPlaceholders();
        registerDefaultCustomPlaceholders();
    }
    
    /**
     * Sets the animation manager for processing animation placeholders
     * @param animationManager The animation manager
     */
    public void setAnimationManager(TablistAnimationManager animationManager) {
        this.animationManager = animationManager;
    }
    
    /**
     * Gets the custom placeholder registry
     * @return The custom placeholder registry
     */
    public CustomPlaceholderRegistry getCustomPlaceholderRegistry() {
        return customPlaceholderRegistry;
    }
    
    /**
     * Update the server reference
     *
     * @param server The new server instance
     */
    public void setServer(MinecraftServer server) {
        this.server = server;
        NeoEssentials.LOGGER.debug("TablistPlaceholderManager server reference updated");
    }
    
    /**
     * Registers all default placeholders
     */
    private void registerDefaultPlaceholders() {
        // Server information
        registerPlaceholder("server", (player, arg) -> server != null ? server.name() : "Server");
        registerPlaceholder("server_name", (player, arg) -> server != null ? server.name() : "Server");
        registerPlaceholder("online", (player, arg) -> server != null ? String.valueOf(server.getPlayerCount()) : "0");
        registerPlaceholder("max", (player, arg) -> server != null ? String.valueOf(server.getMaxPlayers()) : "0");
        registerPlaceholder("tps", (player, arg) -> String.format("%.1f", getAverageTPS()));
        registerPlaceholder("server_tps", (player, arg) -> String.format("%.1f", getAverageTPS()));
        
        // Time and date
        registerPlaceholder("time", (player, arg) -> new SimpleDateFormat("HH:mm:ss").format(new Date()));
        registerPlaceholder("date", (player, arg) -> new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        registerPlaceholder("day", (player, arg) -> new SimpleDateFormat("EEEE").format(new Date()));
          // Player information
        registerPlaceholder("player", (player, arg) -> player.getGameProfile().getName());
        registerPlaceholder("displayname", (player, arg) -> player.getDisplayName().getString());        // Use a safer approach to get ping without direct access
        registerPlaceholder("ping", (player, arg) -> "~ms"); // Simplified placeholder since we can't access ping safely
        registerPlaceholder("health", (player, arg) -> String.format("%.1f", player.getHealth()));
        registerPlaceholder("max_health", (player, arg) -> String.format("%.1f", player.getMaxHealth()));
        
        // Economy (if available)
        registerPlaceholder("balance", (player, arg) -> {
            // Check if the economy manager is available
            if (NeoEssentials.getInstance() != null && 
                NeoEssentials.getInstance().getDataManager() != null && 
                NeoEssentials.getInstance().getDataManager().getEconomyManager() != null) {
                return String.format("%.2f", NeoEssentials.getInstance().getDataManager().getEconomyManager().getBalance(player.getUUID()));
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
        
        // Server uptime
        registerPlaceholder("uptime", (player, arg) -> {
            if (server == null) return "Server offline";
            
            long ticks = server.getTickCount();
            long seconds = ticks / 20;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;
            
            if (days > 0) {
                return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
            } else if (hours > 0) {
                return hours + "h " + (minutes % 60) + "m " + (seconds % 60) + "s";
            } else if (minutes > 0) {
                return minutes + "m " + (seconds % 60) + "s";
            } else {
                return seconds + "s";
            }
        });
        
        // NeoForge version
        registerPlaceholder("neoforge", (player, arg) -> "21.1.179+");
        
        NeoEssentials.LOGGER.info("Registered {} default placeholders", processors.size());    }
      /**
     * Registers default custom placeholders with parameter support
     */
    private void registerDefaultCustomPlaceholders() {
        // Register custom placeholder for ranks with optional formatting
        customPlaceholderRegistry.register(new CustomPlaceholder(
            "rank",
            "Shows player's rank with optional color/formatting",
            (player, args) -> {
                String group = determinePlayerGroup(player);
                if (args.length > 0) {
                    // If format specifier provided, apply it
                    return args[0] + group;
                }
                return group;
            }
        ));
        
        // Register custom placeholder for conditional text
        customPlaceholderRegistry.register(new CustomPlaceholder(
            "if",
            "Conditional text based on comparison: %if:value1,op,value2,trueText,falseText%",
            (player, args) -> {
                if (args.length < 5) return "[Invalid if format]";
                
                String val1 = args[0];
                String op = args[1];
                String val2 = args[2];
                String trueText = args[3];
                String falseText = args[4];
                
                boolean result = false;
                try {
                    switch (op) {
                        case "=":
                        case "==":
                            result = val1.equals(val2);
                            break;
                        case "!=":
                            result = !val1.equals(val2);
                            break;
                        case ">":
                            result = Double.parseDouble(val1) > Double.parseDouble(val2);
                            break;
                        case "<":
                            result = Double.parseDouble(val1) < Double.parseDouble(val2);
                            break;
                        case ">=":
                            result = Double.parseDouble(val1) >= Double.parseDouble(val2);
                            break;
                        case "<=":
                            result = Double.parseDouble(val1) <= Double.parseDouble(val2);
                            break;
                        default:
                            return "[Invalid operator: " + op + "]";
                    }
                } catch (NumberFormatException e) {
                    // For non-numeric comparisons, default to string comparison
                    result = val1.equals(val2);
                }
                
                return result ? trueText : falseText;
            }
        ));
        
        // Progress bar placeholder
        customPlaceholderRegistry.register(new CustomPlaceholder(
            "progress",
            "Shows a progress bar: %progress:value,max,length,filledChar,emptyChar%",
            (player, args) -> {
                if (args.length < 5) return "[Invalid progress format]";
                
                try {
                    double value = Double.parseDouble(args[0]);
                    double max = Double.parseDouble(args[1]);
                    int length = Integer.parseInt(args[2]);
                    String filledChar = args[3];
                    String emptyChar = args[4];
                    
                    double percentage = Math.max(0, Math.min(1, value / max));
                    int filledCount = (int) Math.round(length * percentage);
                    int emptyCount = length - filledCount;
                    
                    StringBuilder bar = new StringBuilder();
                    for (int i = 0; i < filledCount; i++) {
                        bar.append(filledChar);
                    }
                    for (int i = 0; i < emptyCount; i++) {
                        bar.append(emptyChar);
                    }
                    
                    return bar.toString();
                } catch (NumberFormatException e) {
                    return "[Invalid number format]";
                }
            }
        ));
        
        // Expression evaluator - for dynamic calculations
        customPlaceholderRegistry.register(new CustomPlaceholder(
            "expr",
            "Evaluates a mathematical expression: %expr:2+3*4%",
            (player, args) -> {
                if (args.length < 1) return "[Invalid expr format]";
                
                try {
                    // Very simple expression evaluator that supports basic operations
                    String expr = args[0];
                    return String.valueOf(evaluateExpression(expr));
                } catch (Exception e) {
                    return "[Expr error: " + e.getMessage() + "]";
                }
            }
        ));
        
        // Format placeholder - for number formatting
        customPlaceholderRegistry.register(new CustomPlaceholder(
            "format",
            "Formats a number with decimal places: %format:number,decimals%",
            (player, args) -> {
                if (args.length < 2) return "[Invalid format]";
                
                try {
                    double number = Double.parseDouble(args[0]);
                    int decimals = Integer.parseInt(args[1]);
                    return String.format("%." + decimals + "f", number);
                } catch (Exception e) {
                    return "[Format error]";
                }
            }
        ));
        
        // Date formatter
        customPlaceholderRegistry.register(new CustomPlaceholder(
            "date_format",
            "Formats current date/time with pattern: %date_format:yyyy-MM-dd HH:mm:ss%",
            (player, args) -> {
                if (args.length < 1) return "[Invalid date format]";
                
                try {
                    String pattern = args[0];
                    return new SimpleDateFormat(pattern).format(new Date());
                } catch (Exception e) {
                    return "[Date format error]";
                }
            }
        ));
        
        // Player presence check (for vanish integration)
        customPlaceholderRegistry.register(new CustomPlaceholder(
            "is_visible",
            "Checks if a player is visible: %is_visible:playerName,visibleText,hiddenText%",
            (player, args) -> {
                if (args.length < 3) return "[Invalid format]";
                
                String playerName = args[0];
                String visibleText = args[1];
                String hiddenText = args[2];
                
                boolean isVisible = true;
                
                // Check if player is online and visible
                if (server != null) {
                    ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(playerName);
                    if (targetPlayer == null) {
                        isVisible = false;
                    } else {                        // Check for vanish status - safely extract player data
                        try {
                            if (playerStatesContainsVanishedPlayer(targetPlayer.getUUID())) {
                                isVisible = false;
                            }
                        } catch (Exception e) {
                            // If we can't check vanish status, assume player is visible
                            isVisible = true;
                        }
                    }
                }
                
                return isVisible ? visibleText : hiddenText;
            }
        ));
        
        // Table cell text formatter
        customPlaceholderRegistry.register(new CustomPlaceholder(
            "cell",
            "Formats text for a fixed-width table cell: %cell:text,width,alignment%",
            (player, args) -> {
                if (args.length < 3) return "[Invalid cell format]";
                
                String text = args[0];
                int width = Integer.parseInt(args[1]);
                String align = args[2].toLowerCase();
                
                // Truncate if too long
                if (text.length() > width) {
                    return text.substring(0, width);
                }
                
                // Pad to fill width
                StringBuilder result = new StringBuilder();
                int padding = width - text.length();
                
                switch (align) {
                    case "left":
                        result.append(text);
                        for (int i = 0; i < padding; i++) {
                            result.append(" ");
                        }
                        break;
                    case "right":
                        for (int i = 0; i < padding; i++) {
                            result.append(" ");
                        }
                        result.append(text);
                        break;
                    case "center":
                        int leftPad = padding / 2;
                        int rightPad = padding - leftPad;
                        for (int i = 0; i < leftPad; i++) {
                            result.append(" ");
                        }
                        result.append(text);
                        for (int i = 0; i < rightPad; i++) {
                            result.append(" ");
                        }
                        break;
                    default:
                        // Default to left align
                        result.append(text);
                        for (int i = 0; i < padding; i++) {
                            result.append(" ");
                        }
                }
                
                return result.toString();
            }
        ));
        
        NeoEssentials.LOGGER.info("Registered default custom placeholders");
    }
    
    /**
     * Determines the player's group based on permissions
     * @param player The player
     * @return The player's group name
     */
    private String determinePlayerGroup(ServerPlayer player) {
        // Use the centralized permission system to determine player's group
        return com.zerog.neoessentials.utils.PermissionUtil.getPlayerGroup(player);
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
     */    public String processPlaceholders(String text, ServerPlayer player) {
        if (text == null || text.isEmpty() || player == null) {
            return text;
        }
        
        // First process animation placeholders
        text = processAnimationPlaceholders(text, player);
        
        // Check for missing placeholders first (only in debug mode)
        if (NeoEssentials.LOGGER.isDebugEnabled()) {
            logMissingPlaceholders(text);
        }
        
        // First process standard placeholders
        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String placeholderName = matcher.group(2).toLowerCase();
            PlaceholderProcessor processor = processors.get(placeholderName);
            
            if (processor != null) {
                try {
                    String replacement = processor.process(player, "");
                    matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                } catch (Exception e) {
                    // Handle processing errors gracefully
                    NeoEssentials.LOGGER.error("Error processing placeholder %{}%: {}", 
                        placeholderName, e.getMessage());
                    matcher.appendReplacement(result, Matcher.quoteReplacement("[Error]"));
                }
            } else {
                // Leave unknown placeholders as is - they might be custom placeholders
                // that will be processed in the next step
                matcher.appendReplacement(result, Matcher.quoteReplacement(
                    "%" + placeholderName + "%"));
            }
        }
          matcher.appendTail(result);
        
        // Now process custom placeholders with arguments
        String processed = customPlaceholderRegistry.processPlaceholders(result.toString(), player);
        
        return processed;
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
     */    private double getAverageTPS() {
        // NeoForge server TPS calculation
        // This is a simplified version - in a real implementation,
        // you would access the server tick times
        return Math.min(20.0, 20.0);
    }
    
    /**
     * Simple method to check if a player is vanished
     * 
     * @param playerUUID The player's UUID
     * @return True if the player is vanished
     */
    private boolean playerStatesContainsVanishedPlayer(UUID playerUUID) {
        // Implement vanish check here
        // This is a placeholder implementation - replace with actual vanish detection
        return false;
    }
    
    /**
     * Evaluates a simple mathematical expression
     * 
     * @param expression The expression to evaluate
     * @return The result
     */
    private double evaluateExpression(String expression) {
        // This is a simple expression evaluator
        // In a real implementation, you'd use a proper expression parser library
        
        // Remove all whitespace
        expression = expression.replaceAll("\\s+", "");
        
        // First handle parentheses
        while (expression.contains("(")) {
            int openIndex = expression.lastIndexOf('(');
            int closeIndex = expression.indexOf(')', openIndex);
            
            if (closeIndex == -1) {
                throw new IllegalArgumentException("Mismatched parentheses");
            }
            
            String subExpr = expression.substring(openIndex + 1, closeIndex);
            double subResult = evaluateExpression(subExpr);
            
            expression = expression.substring(0, openIndex) + subResult + 
                expression.substring(closeIndex + 1);
        }
        
        // Handle addition and subtraction
        List<Double> numbers = new ArrayList<>();
        List<Character> operations = new ArrayList<>();
        
        StringBuilder currentNumber = new StringBuilder();
        boolean negative = false;
        
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            
            if (c == '+' || c == '-' || c == '*' || c == '/') {
                // Handle negative numbers
                if (c == '-' && (i == 0 || operations.size() == numbers.size())) {
                    negative = true;
                    continue;
                }
                
                // Add current number to list
                if (currentNumber.length() > 0) {
                    double num = Double.parseDouble(currentNumber.toString());
                    numbers.add(negative ? -num : num);
                    negative = false;
                    currentNumber = new StringBuilder();
                }
                
                operations.add(c);
            } else if (Character.isDigit(c) || c == '.') {
                currentNumber.append(c);
            } else {
                throw new IllegalArgumentException("Invalid character: " + c);
            }
        }
        
        // Add the last number
        if (currentNumber.length() > 0) {
            double num = Double.parseDouble(currentNumber.toString());
            numbers.add(negative ? -num : num);
        }
        
        // First pass: multiplication and division
        for (int i = 0; i < operations.size(); i++) {
            if (operations.get(i) == '*' || operations.get(i) == '/') {
                double result;
                if (operations.get(i) == '*') {
                    result = numbers.get(i) * numbers.get(i + 1);
                } else {
                    if (numbers.get(i + 1) == 0) {
                        throw new ArithmeticException("Division by zero");
                    }
                    result = numbers.get(i) / numbers.get(i + 1);
                }
                
                numbers.set(i, result);
                numbers.remove(i + 1);
                operations.remove(i);
                i--;
            }
        }
        
        // Second pass: addition and subtraction
        double result = numbers.get(0);
        for (int i = 0; i < operations.size(); i++) {
            if (operations.get(i) == '+') {
                result += numbers.get(i + 1);
            } else if (operations.get(i) == '-') {
                result -= numbers.get(i + 1);
            }
        }
        
        return result;
    }
    
    /**
     * Gets a player's ping in milliseconds
     * 
     * @param player The player
     * @return The ping in milliseconds
     */    // Placeholder methods are now directly implemented above
    
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
     * Strips color codes from text
     *
     * @param text The text to process
     * @return The text with color codes removed
     */
    public static String stripColor(String text) {
        if (text == null) {
            return "";
        }
        
        return text.replaceAll("(?i)§[0-9A-FK-OR]", "").replaceAll("(?i)&[0-9A-FK-OR]", "");
    }
      /**
     * Applies color codes to a text string and returns it as a Component
     *
     * @param text The text to colorize
     * @return The colorized Component
     */
    public static Component colorize(String text) {
        if (text == null) {
            return Component.literal("");
        }
        String formattedText = formatColors(text);
        return Component.literal(formattedText);
    }
    
    /**
     * Gets a substring of text, accounting for color codes
     * 
     * @param text The original text
     * @param start Start index (inclusive)
     * @param end End index (exclusive)
     * @return The substring with color codes preserved
     */
    public static String substring(String text, int start, int end) {
        if (text == null) {
            return "";
        }
        
        // Handle out of bounds indices
        if (start < 0) start = 0;
        if (end > text.length()) end = text.length();
        if (start >= end) return "";
        
        // Simple case: direct substring
        return text.substring(start, end);
    }
    
    /**
     * Transfer color codes from source to target
     * 
     * @param source Text with color codes to transfer
     * @param target Text to apply color codes to
     * @return The target text with color codes from source
     */
    public static String transferColors(String source, String target) {
        if (source == null || target == null || source.isEmpty() || target.isEmpty()) {
            return target;
        }
        
        // Find the last color code in source
        char colorChar = '§';
        String lastColorCode = "";
        for (int i = 0; i < source.length() - 1; i++) {
            if (source.charAt(i) == colorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(source.charAt(i + 1)) > -1) {
                lastColorCode = source.substring(i, i + 2);
            }
        }
        
        // Apply the color code to target
        return lastColorCode + target;
    }
    
    /**
     * Inner class for representing a placeholder processor
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
                NeoEssentials.LOGGER.error("Error processing placeholder", e);
                return "Error";
            }
        }
    }
    
    /**
     * Inner class for representing a cached placeholder value
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
        
        public boolean isExpired(long maxAgeMs) {
            return System.currentTimeMillis() - timestamp > maxAgeMs;
        }
    }
    
    /**
     * Debugging method to help identify missing placeholders
     *
     * @param text Text with placeholders
     * @return List of unhandled placeholders
     */
    private List<String> findMissingPlaceholders(String text) {
        List<String> missing = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return missing;
        }
        
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            String placeholder = matcher.group(2).toLowerCase();
            if (!processors.containsKey(placeholder)) {
                missing.add(placeholder);
            }
        }
        
        return missing;
    }
    
    /**
     * Logs any missing placeholders found in the text
     * 
     * @param text Text to check for missing placeholders
     */
    public void logMissingPlaceholders(String text) {
        List<String> missing = findMissingPlaceholders(text);
        if (!missing.isEmpty()) {
            NeoEssentials.LOGGER.warn("Missing placeholders in text: {}", String.join(", ", missing));
        }
    }
    
    /**
     * Process animation placeholders in text
     * @param text The text to process
     * @param player The player for context
     * @return Text with animation placeholders replaced
     */
    private String processAnimationPlaceholders(String text, ServerPlayer player) {
        if (animationManager == null) {
            return text;
        }
        
        StringBuffer result = new StringBuffer();
        Matcher matcher = ANIMATION_PATTERN.matcher(text);
        
        boolean foundAnimations = false;
        while (matcher.find()) {
            foundAnimations = true;
            String animationName = matcher.group(2);
            
            try {
                // Get the current animation frame for this animation
                String animatedText = animationManager.getAnimationFrame(animationName, player);
                if (animatedText != null) {
                    matcher.appendReplacement(result, Matcher.quoteReplacement(animatedText));
                    NeoEssentials.LOGGER.debug("Replaced animation placeholder '{}' with '{}'", 
                        matcher.group(0), animatedText);
                } else {
                    // Animation not found, leave placeholder as is
                    matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
                    NeoEssentials.LOGGER.warn("Animation '{}' not found, leaving placeholder as-is", animationName);
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error processing animation placeholder {}: {}", 
                    animationName, e.getMessage());
                matcher.appendReplacement(result, Matcher.quoteReplacement("[Anim Error]"));
            }
        }
        
        if (foundAnimations) {
            NeoEssentials.LOGGER.debug("Processed animation placeholders in text: '{}' -> '{}'", 
                text, result.toString());
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
}
