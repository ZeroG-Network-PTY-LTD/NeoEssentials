package com.zerog.neoessentials.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Centralized input validation utility for NeoEssentials.
 * Provides secure validation for user inputs, file paths, and command parameters.
 */
public class InputValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(InputValidator.class);
    
    // Security patterns
    private static final Pattern VALID_PLAYER_NAME = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");
    private static final Pattern SAFE_COMMAND = Pattern.compile("^[a-zA-Z0-9_\\-/\\s]+$");
    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9_\\-\\.]+$");
    
    // Limits
    private static final int MAX_COMMAND_LENGTH = 256;
    private static final int MAX_REASON_LENGTH = 500;
    private static final BigDecimal MAX_ECONOMY_AMOUNT = new BigDecimal("999999999.99");
    private static final BigDecimal MIN_ECONOMY_AMOUNT = new BigDecimal("0.01");
    
    /**
     * Validates a player name for security and format compliance.
     */
    public static ValidationResult validatePlayerName(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return ValidationResult.failure("Player name cannot be empty");
        }
        
        String trimmed = playerName.trim();
        if (trimmed.length() > 16) {
            return ValidationResult.failure("Player name too long (max 16 characters)");
        }
        
        if (!VALID_PLAYER_NAME.matcher(trimmed).matches()) {
            return ValidationResult.failure("Player name contains invalid characters");
        }
        
        return ValidationResult.success(trimmed);
    }
    
    /**
     * Validates an economic amount for transactions.
     */
    public static ValidationResult validateEconomyAmount(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            return ValidationResult.failure("Invalid amount: not a valid number");
        }
        
        if (amount <= 0) {
            return ValidationResult.failure("Amount must be positive");
        }
        
        BigDecimal bd = BigDecimal.valueOf(amount);
        if (bd.compareTo(MAX_ECONOMY_AMOUNT) > 0) {
            return ValidationResult.failure("Amount too large (max " + MAX_ECONOMY_AMOUNT + ")");
        }
        
        if (bd.compareTo(MIN_ECONOMY_AMOUNT) < 0) {
            return ValidationResult.failure("Amount too small (min " + MIN_ECONOMY_AMOUNT + ")");
        }
        
        return ValidationResult.success(bd);
    }
    
    /**
     * Validates a command string for powertool or similar functionality.
     */
    public static ValidationResult validateCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return ValidationResult.failure("Command cannot be empty");
        }
        
        String trimmed = command.trim();
        if (trimmed.length() > MAX_COMMAND_LENGTH) {
            return ValidationResult.failure("Command too long (max " + MAX_COMMAND_LENGTH + " characters)");
        }
        
        // Remove leading slash if present
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        
        // Basic safety check - prevent dangerous commands
        String lowerCommand = trimmed.toLowerCase();
        if (containsDangerousCommand(lowerCommand)) {
            return ValidationResult.failure("Command contains potentially dangerous operations");
        }
        
        // Check for basic command structure
        if (!SAFE_COMMAND.matcher(trimmed).matches()) {
            return ValidationResult.failure("Command contains unsafe characters");
        }
        
        return ValidationResult.success(trimmed);
    }
    
    /**
     * Validates a file path to prevent directory traversal attacks.
     */
    public static ValidationResult validateFilePath(String filePath, String allowedBasePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return ValidationResult.failure("File path cannot be empty");
        }
        
        try {
            Path normalizedPath = Paths.get(filePath).normalize();
            Path basePath = Paths.get(allowedBasePath).normalize();
            
            // Ensure the path doesn't escape the base directory
            if (!normalizedPath.startsWith(basePath)) {
                return ValidationResult.failure("File path outside allowed directory");
            }
            
            // Check for dangerous path components
            for (Path component : normalizedPath) {
                String name = component.getFileName().toString();
                if (name.contains("..") || name.contains("~") || !SAFE_FILENAME.matcher(name).matches()) {
                    return ValidationResult.failure("File path contains unsafe components");
                }
            }
            
            return ValidationResult.success(normalizedPath.toString());
        } catch (Exception e) {
            LOGGER.debug("Path validation error: {}", e.getMessage());
            return ValidationResult.failure("Invalid file path format");
        }
    }
    
    /**
     * Validates a reason/message string.
     */
    public static ValidationResult validateReason(String reason) {
        if (reason == null) {
            return ValidationResult.success(""); // Allow null reasons as empty
        }
        
        String trimmed = reason.trim();
        if (trimmed.length() > MAX_REASON_LENGTH) {
            return ValidationResult.failure("Reason too long (max " + MAX_REASON_LENGTH + " characters)");
        }
        
        // Basic safety - prevent potential injection attempts
        if (containsUnsafeContent(trimmed)) {
            return ValidationResult.failure("Reason contains unsafe content");
        }
        
        return ValidationResult.success(trimmed);
    }
    
    /**
     * Validates that a player exists and is online.
     */
    public static ValidationResult validateOnlinePlayer(String playerName, MinecraftServer server) {
        ValidationResult nameValidation = validatePlayerName(playerName);
        if (!nameValidation.isValid()) {
            return nameValidation;
        }
        
        String validName = (String) nameValidation.getValue();
        ServerPlayer player = server.getPlayerList().getPlayers().stream()
            .filter(p -> p.getGameProfile().getName().equalsIgnoreCase(validName))
            .findFirst().orElse(null);
            
        if (player == null) {
            return ValidationResult.failure("Player '" + validName + "' not found or not online");
        }
        
        return ValidationResult.success(player);
    }
    
    /**
     * Validates an enchantment level.
     */
    public static ValidationResult validateEnchantmentLevel(int level, boolean allowOverMax) {
        if (level < 1) {
            return ValidationResult.failure("Enchantment level must be at least 1");
        }
        
        if (!allowOverMax && level > 255) {
            return ValidationResult.failure("Enchantment level too high (max 255)");
        }
        
        // Even with override, set absolute maximum for safety
        if (level > 32767) {
            return ValidationResult.failure("Enchantment level exceeds absolute maximum");
        }
        
        return ValidationResult.success(level);
    }
    
    /**
     * Check for dangerous command patterns.
     */
    private static boolean containsDangerousCommand(String command) {
        String[] dangerousPatterns = {
            "rm ", "del ", "delete ", "format", "shutdown", "reboot",
            "eval", "exec", "system", "runtime", "process",
            "../", "..\\", "~", "$", "`", "&&", "||", ";",
            "file:", "http:", "https:", "ftp:", "jar:",
            "class.forname", "reflection", "unsafe"
        };
        
        for (String pattern : dangerousPatterns) {
            if (command.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check for unsafe content in text inputs.
     */
    private static boolean containsUnsafeContent(String text) {
        String lower = text.toLowerCase();
        String[] unsafePatterns = {
            "<script", "javascript:", "vbscript:", "data:",
            "\\x", "\\u", "%", "&lt;", "&gt;",
            "eval(", "alert(", "confirm(", "prompt("
        };
        
        for (String pattern : unsafePatterns) {
            if (lower.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Result class for validation operations.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final Object value;
        
        private ValidationResult(boolean valid, String errorMessage, Object value) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.value = value;
        }
        
        public static ValidationResult success(Object value) {
            return new ValidationResult(true, null, value);
        }
        
        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage, null);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public Object getValue() {
            return value;
        }
        
        public <T> T getValue(Class<T> type) {
            return type.cast(value);
        }
    }
}