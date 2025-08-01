package com.zerog.neoessentials.utils;

import com.zerog.neoessentials.managers.EconomyManager;
import com.zerog.neoessentials.managers.HomeManager;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Advanced placeholder manager for NeoEssentials
 * Supports dynamic placeholders with real data integration
 * Compatible with PlaceholderAPI-style placeholders
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class PlaceholderManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceholderManager.class);
    private static PlaceholderManager instance;
    
    private final Map<String, BiFunction<ServerPlayer, String, String>> placeholders;
    private final DecimalFormat currencyFormat;
    private final DateTimeFormatter timeFormat;
    
    private PlaceholderManager() {
        this.placeholders = new HashMap<>();
        this.currencyFormat = new DecimalFormat("#,##0.00");
        this.timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        registerDefaultPlaceholders();
    }
    
    public static PlaceholderManager getInstance() {
        if (instance == null) {
            instance = new PlaceholderManager();
        }
        return instance;
    }
    
    /**
     * Register all default placeholders
     */
    private void registerDefaultPlaceholders() {
        // Player info placeholders
        registerPlaceholder("player", (player, args) -> player.getName().getString());
        registerPlaceholder("displayname", (player, args) -> player.getDisplayName().getString());
        registerPlaceholder("uuid", (player, args) -> player.getUUID().toString());
        registerPlaceholder("ping", (player, args) -> String.valueOf(player.connection.latency()));
        
        // Location placeholders
        registerPlaceholder("world", (player, args) -> player.level().dimension().location().getPath());
        registerPlaceholder("x", (player, args) -> String.valueOf((int) player.getX()));
        registerPlaceholder("y", (player, args) -> String.valueOf((int) player.getY()));
        registerPlaceholder("z", (player, args) -> String.valueOf((int) player.getZ()));
        registerPlaceholder("yaw", (player, args) -> String.valueOf((int) player.getYRot()));
        registerPlaceholder("pitch", (player, args) -> String.valueOf((int) player.getXRot()));
        
        // Detailed location placeholders
        registerPlaceholder("x_exact", (player, args) -> String.format("%.2f", player.getX()));
        registerPlaceholder("y_exact", (player, args) -> String.format("%.2f", player.getY()));
        registerPlaceholder("z_exact", (player, args) -> String.format("%.2f", player.getZ()));
        
        // Time placeholders
        registerPlaceholder("time", (player, args) -> timeFormat.format(LocalDateTime.now()));
        registerPlaceholder("time_12", (player, args) -> 
            DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a").format(LocalDateTime.now()));
        registerPlaceholder("date", (player, args) -> 
            DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now()));
        
        // Server info placeholders
        registerPlaceholder("server_name", (player, args) -> "NeoEssentials Server");
        registerPlaceholder("server_players", (player, args) -> {
            if (player.getServer() != null) {
                return String.valueOf(player.getServer().getPlayerCount());
            }
            return "0";
        });
        registerPlaceholder("server_max_players", (player, args) -> {
            if (player.getServer() != null) {
                return String.valueOf(player.getServer().getMaxPlayers());
            }
            return "20";
        });
        
        // Economy placeholders
        registerPlaceholder("balance", (player, args) -> {
            EconomyManager economy = EconomyManager.getInstance();
            BigDecimal balance = economy.getBalance(player.getUUID());
            return currencyFormat.format(balance);
        });
        
        registerPlaceholder("balance_formatted", (player, args) -> {
            EconomyManager economy = EconomyManager.getInstance();
            BigDecimal balance = economy.getBalance(player.getUUID());
            return economy.formatCurrency(balance);
        });
        
        // Home placeholders
        registerPlaceholder("homes_count", (player, args) -> {
            HomeManager homeManager = HomeManager.getInstance();
            int homeCount = homeManager.getHomeCount(player.getUUID());
            return String.valueOf(homeCount);
        });
        
        registerPlaceholder("homes_list", (player, args) -> {
            HomeManager homeManager = HomeManager.getInstance();
            List<String> homes = homeManager.getPlayerHomes(player.getUUID());
            return String.join(", ", homes);
        });
        
        // Kit placeholders
        registerPlaceholder("kits_available", (player, args) -> {
            // This would need a method to get available kits for player
            return "starter,tools,food"; // Placeholder
        });
        
        // Warp placeholders
        registerPlaceholder("warps_count", (player, args) -> {
            // WarpManager doesn't expose warp count directly, so return placeholder
            return "3"; // Could be enhanced to get actual warp count
        });
        
        // Player stats placeholders
        registerPlaceholder("health", (player, args) -> String.valueOf((int) player.getHealth()));
        registerPlaceholder("max_health", (player, args) -> String.valueOf((int) player.getMaxHealth()));
        registerPlaceholder("food", (player, args) -> String.valueOf(player.getFoodData().getFoodLevel()));
        registerPlaceholder("exp", (player, args) -> String.valueOf(player.experienceLevel));
        registerPlaceholder("gamemode", (player, args) -> player.gameMode.getGameModeForPlayer().getName());
        
        // Permission placeholders
        registerPlaceholder("has_permission", (player, args) -> {
            if (args == null || args.isEmpty()) return "false";
            // This would integrate with permission system
            return String.valueOf(player.hasPermissions(2)); // Basic check
        });
        
        // Conditional placeholders
        registerPlaceholder("if_op", (player, args) -> {
            if (args == null) return "";
            String[] parts = args.split(",", 2);
            String trueValue = parts.length > 0 ? parts[0] : "";
            String falseValue = parts.length > 1 ? parts[1] : "";
            return player.hasPermissions(4) ? trueValue : falseValue;
        });
        
        registerPlaceholder("if_creative", (player, args) -> {
            if (args == null) return "";
            String[] parts = args.split(",", 2);
            String trueValue = parts.length > 0 ? parts[0] : "";
            String falseValue = parts.length > 1 ? parts[1] : "";
            return player.isCreative() ? trueValue : falseValue;
        });
        
        // Mathematical placeholders
        registerPlaceholder("math", (player, args) -> {
            if (args == null) return "0";
            try {
                // Simple math evaluation - could be enhanced
                return evaluateSimpleMath(args);
            } catch (Exception e) {
                return "Error";
            }
        });
    }
    
    /**
     * Register a custom placeholder
     */
    public void registerPlaceholder(String identifier, BiFunction<ServerPlayer, String, String> function) {
        placeholders.put(identifier.toLowerCase(), function);
        LOGGER.debug("Registered placeholder: {}", identifier);
    }
    
    /**
     * Process placeholders in a message
     */
    public String processPlaceholders(ServerPlayer player, String message) {
        if (player == null || message == null || message.isEmpty()) {
            return message;
        }
        
        String result = message;
        
        // Process %identifier% format
        result = processPlaceholderFormat(player, result, "%", "%");
        
        // Process {identifier} format
        result = processPlaceholderFormat(player, result, "{", "}");
        
        // Process %neoessentials_identifier% format (PlaceholderAPI style)
        result = processPlaceholderFormat(player, result, "%neoessentials_", "%");
        
        return result;
    }
    
    /**
     * Process placeholders with specific format
     */
    private String processPlaceholderFormat(ServerPlayer player, String message, String prefix, String suffix) {
        String result = message;
        
        int startIndex = 0;
        while ((startIndex = result.indexOf(prefix, startIndex)) != -1) {
            int endIndex = result.indexOf(suffix, startIndex + prefix.length());
            if (endIndex == -1) break;
            
            String placeholder = result.substring(startIndex + prefix.length(), endIndex);
            String replacement = processPlaceholder(player, placeholder);
            
            if (replacement != null) {
                result = result.substring(0, startIndex) + replacement + result.substring(endIndex + suffix.length());
                startIndex += replacement.length();
            } else {
                startIndex = endIndex + suffix.length();
            }
        }
        
        return result;
    }
    
    /**
     * Process a single placeholder
     */
    private String processPlaceholder(ServerPlayer player, String placeholder) {
        if (placeholder == null || placeholder.isEmpty()) {
            return null;
        }
        
        // Handle placeholders with arguments (e.g., "has_permission:essentials.home")
        String identifier = placeholder;
        String args = null;
        
        if (placeholder.contains(":")) {
            String[] parts = placeholder.split(":", 2);
            identifier = parts[0];
            args = parts[1];
        }
        
        identifier = identifier.toLowerCase();
        
        BiFunction<ServerPlayer, String, String> function = placeholders.get(identifier);
        if (function != null) {
            try {
                return function.apply(player, args);
            } catch (Exception e) {
                LOGGER.warn("Error processing placeholder '{}': {}", placeholder, e.getMessage());
                return placeholder; // Return original if error
            }
        }
        
        return null; // Placeholder not found
    }
    
    /**
     * Simple math evaluation for basic operations
     */
    private String evaluateSimpleMath(String expression) {
        try {
            // Remove spaces
            expression = expression.replaceAll("\\s", "");
            
            // Very basic math - could be enhanced with a proper expression parser
            if (expression.contains("+")) {
                String[] parts = expression.split("\\+");
                double result = 0;
                for (String part : parts) {
                    result += Double.parseDouble(part);
                }
                return String.valueOf((int) result);
            } else if (expression.contains("-")) {
                String[] parts = expression.split("-");
                double result = Double.parseDouble(parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    result -= Double.parseDouble(parts[i]);
                }
                return String.valueOf((int) result);
            } else if (expression.contains("*")) {
                String[] parts = expression.split("\\*");
                double result = 1;
                for (String part : parts) {
                    result *= Double.parseDouble(part);
                }
                return String.valueOf((int) result);
            } else if (expression.contains("/")) {
                String[] parts = expression.split("/");
                double result = Double.parseDouble(parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    result /= Double.parseDouble(parts[i]);
                }
                return String.valueOf((int) result);
            }
            
            // If no operation, just return the number
            return String.valueOf((int) Double.parseDouble(expression));
            
        } catch (Exception e) {
            return "Error";
        }
    }
    
    /**
     * Get all registered placeholders
     */
    public Map<String, BiFunction<ServerPlayer, String, String>> getRegisteredPlaceholders() {
        return new HashMap<>(placeholders);
    }
    
    /**
     * Check if a placeholder exists
     */
    public boolean hasPlaceholder(String identifier) {
        return placeholders.containsKey(identifier.toLowerCase());
    }
    
    /**
     * Unregister a placeholder
     */
    public boolean unregisterPlaceholder(String identifier) {
        return placeholders.remove(identifier.toLowerCase()) != null;
    }
    
    /**
     * Clear all placeholders and re-register defaults
     */
    public void reload() {
        placeholders.clear();
        registerDefaultPlaceholders();
        LOGGER.info("Placeholder manager reloaded with {} placeholders", placeholders.size());
    }
}
