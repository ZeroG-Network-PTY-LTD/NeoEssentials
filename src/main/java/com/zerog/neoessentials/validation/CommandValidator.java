package com.zerog.neoessentials.validation;

import com.zerog.neoessentials.error.ErrorHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Command input validation system
 * Provides comprehensive validation for all command parameters
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class CommandValidator {
    
    // Common validation patterns
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^[0-9]+(\\.[0-9]+)?$");
    private static final Pattern TIME_PATTERN = Pattern.compile("^[0-9]+(s|m|h|d)?$");
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("^-?[0-9]+(\\.[0-9]+)?$");
    
    /**
     * Validation result container
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String suggestion;
        
        private ValidationResult(boolean valid, String errorMessage, String suggestion) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.suggestion = suggestion;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }
        
        public static ValidationResult failure(String errorMessage, String suggestion) {
            return new ValidationResult(false, errorMessage, suggestion);
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public String getSuggestion() { return suggestion; }
    }
    
    /**
     * Validate player name format
     */
    public static ValidationResult validatePlayerName(CommandSourceStack source, String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return ValidationResult.failure("Player name cannot be empty", "Provide a valid player name");
        }
        
        if (!PLAYER_NAME_PATTERN.matcher(playerName).matches()) {
            return ValidationResult.failure(
                "Invalid player name format", 
                "Player names must be 1-16 characters, alphanumeric and underscores only"
            );
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate and parse monetary amount
     */
    public static ValidationResult validateAmount(CommandSourceStack source, String amountStr, double min, double max) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return ValidationResult.failure("Amount cannot be empty", "Provide a valid number");
        }
        
        if (!AMOUNT_PATTERN.matcher(amountStr).matches()) {
            return ValidationResult.failure(
                "Invalid amount format", 
                "Amount must be a positive number (e.g., 100, 50.25)"
            );
        }
        
        try {
            double amount = Double.parseDouble(amountStr);
            
            if (amount < min) {
                return ValidationResult.failure(
                    String.format("Amount too small (minimum: %.2f)", min),
                    String.format("Use an amount of %.2f or higher", min)
                );
            }
            
            if (max > 0 && amount > max) {
                return ValidationResult.failure(
                    String.format("Amount too large (maximum: %.2f)", max),
                    String.format("Use an amount of %.2f or lower", max)
                );
            }
            
            return ValidationResult.success();
            
        } catch (NumberFormatException e) {
            return ValidationResult.failure(
                "Invalid number format", 
                "Use a valid decimal number (e.g., 100.50)"
            );
        }
    }
    
    /**
     * Validate time duration format
     */
    public static ValidationResult validateTimeDuration(CommandSourceStack source, String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return ValidationResult.failure("Duration cannot be empty", "Use format like: 30s, 5m, 2h, 1d");
        }
        
        if (!TIME_PATTERN.matcher(timeStr).matches()) {
            return ValidationResult.failure(
                "Invalid time format", 
                "Use format: number + unit (s=seconds, m=minutes, h=hours, d=days). Example: 30s, 5m, 2h, 1d"
            );
        }
        
        try {
            long seconds = parseTimeToSeconds(timeStr);
            
            if (seconds < 1) {
                return ValidationResult.failure(
                    "Duration too short", 
                    "Minimum duration is 1 second"
                );
            }
            
            if (seconds > 31536000) { // 1 year
                return ValidationResult.failure(
                    "Duration too long", 
                    "Maximum duration is 1 year (365d)"
                );
            }
            
            return ValidationResult.success();
            
        } catch (Exception e) {
            return ValidationResult.failure(
                "Invalid time format", 
                "Use format: number + unit (30s, 5m, 2h, 1d)"
            );
        }
    }
    
    /**
     * Validate coordinate values
     */
    public static ValidationResult validateCoordinate(CommandSourceStack source, String coordStr, String axis) {
        if (coordStr == null || coordStr.trim().isEmpty()) {
            return ValidationResult.failure(
                String.format("%s coordinate cannot be empty", axis.toUpperCase()), 
                "Provide a valid coordinate number"
            );
        }
        
        if (!COORDINATE_PATTERN.matcher(coordStr).matches()) {
            return ValidationResult.failure(
                String.format("Invalid %s coordinate format", axis.toUpperCase()), 
                "Coordinates must be numbers (e.g., 100, -50, 0.5)"
            );
        }
        
        try {
            double coord = Double.parseDouble(coordStr);
            
            // World border limits
            if (Math.abs(coord) > 30000000) {
                return ValidationResult.failure(
                    String.format("%s coordinate out of world bounds", axis.toUpperCase()),
                    "Coordinates must be between -30,000,000 and 30,000,000"
                );
            }
            
            return ValidationResult.success();
            
        } catch (NumberFormatException e) {
            return ValidationResult.failure(
                String.format("Invalid %s coordinate", axis.toUpperCase()),
                "Use a valid number for coordinates"
            );
        }
    }
    
    /**
     * Validate item name/ID
     */
    public static ValidationResult validateItem(CommandSourceStack source, String itemStr) {
        if (itemStr == null || itemStr.trim().isEmpty()) {
            return ValidationResult.failure("Item cannot be empty", "Provide a valid item name or ID");
        }
        
        try {
            // Try parsing as ResourceLocation
            ResourceLocation itemLocation;
            if (itemStr.contains(":")) {
                itemLocation = ResourceLocation.parse(itemStr);
            } else {
                itemLocation = ResourceLocation.fromNamespaceAndPath("minecraft", itemStr);
            }
            
            // Check if item exists
            Item item = BuiltInRegistries.ITEM.get(itemLocation);
            if (item == null || item == Items.AIR) {
                return ValidationResult.failure(
                    "Item not found", 
                    "Use a valid item name (e.g., diamond, minecraft:stone, iron_sword)"
                );
            }
            
            return ValidationResult.success();
            
        } catch (Exception e) {
            return ValidationResult.failure(
                "Invalid item format", 
                "Use format: item_name or namespace:item_name (e.g., diamond, minecraft:stone)"
            );
        }
    }
    
    /**
     * Validate speed value
     */
    public static ValidationResult validateSpeed(CommandSourceStack source, String speedStr, double maxSpeed) {
        ValidationResult amountResult = validateAmount(source, speedStr, 0.1, maxSpeed);
        if (!amountResult.isValid()) {
            return ValidationResult.failure(
                amountResult.getErrorMessage(),
                String.format("Speed must be between 0.1 and %.1f", maxSpeed)
            );
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate permission node format
     */
    public static ValidationResult validatePermission(CommandSourceStack source, String permission) {
        if (permission == null || permission.trim().isEmpty()) {
            return ValidationResult.failure("Permission cannot be empty", "Provide a valid permission node");
        }
        
        // Basic permission format validation
        if (!permission.matches("^[a-zA-Z0-9._-]+$")) {
            return ValidationResult.failure(
                "Invalid permission format", 
                "Permissions can only contain letters, numbers, dots, underscores, and hyphens"
            );
        }
        
        if (permission.length() > 100) {
            return ValidationResult.failure(
                "Permission too long", 
                "Permissions must be 100 characters or less"
            );
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Comprehensive validation with error handling
     */
    public static boolean validateAndHandle(CommandSourceStack source, ValidationResult result, String fieldName) {
        if (result.isValid()) {
            return true;
        }
        
        ErrorHandler.handleValidationError(source, fieldName, "user input", result.getSuggestion());
        return false;
    }
    
    /**
     * Parse time string to seconds
     */
    private static long parseTimeToSeconds(String timeStr) {
        if (timeStr.matches("^[0-9]+$")) {
            // No unit specified, assume seconds
            return Long.parseLong(timeStr);
        }
        
        String numberPart = timeStr.replaceAll("[a-zA-Z]", "");
        String unitPart = timeStr.replaceAll("[0-9]", "").toLowerCase();
        
        long number = Long.parseLong(numberPart);
        
        return switch (unitPart) {
            case "s", "" -> number;
            case "m" -> number * 60;
            case "h" -> number * 3600;
            case "d" -> number * 86400;
            default -> throw new IllegalArgumentException("Unknown time unit: " + unitPart);
        };
    }
    
    /**
     * Quick validation helpers
     */
    public static class Quick {
        
        public static boolean isValidPlayerName(String name) {
            return name != null && PLAYER_NAME_PATTERN.matcher(name).matches();
        }
        
        public static boolean isValidAmount(String amount) {
            return amount != null && AMOUNT_PATTERN.matcher(amount).matches();
        }
        
        public static boolean isValidTime(String time) {
            return time != null && TIME_PATTERN.matcher(time).matches();
        }
        
        public static boolean isValidCoordinate(String coord) {
            return coord != null && COORDINATE_PATTERN.matcher(coord).matches();
        }
    }
}
