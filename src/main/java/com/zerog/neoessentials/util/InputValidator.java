package com.zerog.neoessentials.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * Centralized input validation utility for NeoEssentials.
 * Provides secure validation for user inputs, file paths, and command parameters.
 */
public class InputValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(InputValidator.class);
    
    // Security patterns
    private static final Pattern VALID_PLAYER_NAME = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");
    // Deny-list rather than allow-list: real Minecraft commands legitimately use target
    // selectors (@a, @e[type=cow,distance=..5]), NBT/JSON data ({...}, "..."), coordinates
    // (~ ~6 ~, ^ ^ ^1), negation (!), and many other punctuation characters that an
    // enumerated allow-list keeps missing (see containsDangerousCommand's history — the old
    // allow-list didn't even permit '@', so any selector-based command was rejected). Only
    // reject C0 control characters and the backtick, neither of which has any legitimate use
    // in Minecraft command syntax.
    private static final Pattern SAFE_COMMAND = Pattern.compile("^[^\\x00-\\x1F`]+$");
    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9_\\-\\.]+$");
    
    // Config-based limits - loaded from ConfigManager
    private static int getMaxCommandLength() {
        return com.zerog.neoessentials.config.ConfigManager.getInstance().getMaxCommandLength();
    }
    
    private static int getMaxReasonLength() {
        return com.zerog.neoessentials.config.ConfigManager.getInstance().getMaxReasonLength();
    }
    
    private static BigDecimal getMaxEconomyAmount() {
        return com.zerog.neoessentials.config.ConfigManager.getInstance().getMaxEconomyAmount();
    }
    
    private static BigDecimal getMinEconomyAmount() {
        return com.zerog.neoessentials.config.ConfigManager.getInstance().getMinEconomyAmount();
    }
    
    /**
     * Validates a player name for security and format compliance.
     */
    public static ValidationResult validatePlayerName(String playerName) {
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isInputValidationEnabled()) {
            return ValidationResult.success(playerName);
        }
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
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isInputValidationEnabled()) {
            return ValidationResult.success(amount);
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            return ValidationResult.failure("Invalid amount: not a valid number");
        }
        if (amount <= 0) {
            return ValidationResult.failure("Amount must be positive");
        }
        BigDecimal bd = BigDecimal.valueOf(amount);
        BigDecimal maxAmount = getMaxEconomyAmount();
        BigDecimal minAmount = getMinEconomyAmount();
        if (bd.compareTo(maxAmount) > 0) {
            return ValidationResult.failure("Amount too large (max " + maxAmount + ")");
        }
        if (bd.compareTo(minAmount) < 0) {
            return ValidationResult.failure("Amount too small (min " + minAmount + ")");
        }
        return ValidationResult.success(bd);
    }
    
    /**
     * Validates a command string for powertool or similar functionality.
     */
    public static ValidationResult validateCommand(String command) {
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isInputValidationEnabled()) {
            return ValidationResult.success(command);
        }
        if (command == null || command.trim().isEmpty()) {
            return ValidationResult.failure("Command cannot be empty");
        }
        String trimmed = command.trim();
        int maxLength = getMaxCommandLength();
        if (trimmed.length() > maxLength) {
            return ValidationResult.failure("Command too long (max " + maxLength + " characters)");
        }
        // Remove leading slash if present
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        // Check if unsafe commands are allowed via configuration
        boolean allowUnsafeCommands = com.zerog.neoessentials.config.ConfigManager.getInstance().isUnsafeCommandsAllowed();
        // Only perform dangerous command checks if unsafe commands are not allowed
        if (!allowUnsafeCommands) {
            String lowerCommand = trimmed.toLowerCase();
            if (containsDangerousCommand(lowerCommand)) {
                return ValidationResult.failure("Command contains potentially dangerous operations. Enable 'allowUnsafeCommands' in config to use this command.");
            }
            // Check for basic command structure (only when safety is enforced)
            if (!SAFE_COMMAND.matcher(trimmed).matches()) {
                return ValidationResult.failure("Command contains unsafe characters. Enable 'allowUnsafeCommands' in config to use special characters.");
            }
        }
        return ValidationResult.success(trimmed);
    }
    
    /**
     * Validates a file path to prevent directory traversal attacks.
     */
    public static ValidationResult validateFilePath(String filePath, String allowedBasePath) {
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isInputValidationEnabled()) {
            return ValidationResult.success(filePath);
        }
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
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isInputValidationEnabled()) {
            return ValidationResult.success(reason);
        }
        if (reason == null) {
            return ValidationResult.success(""); // Allow null reasons as empty
        }
        String trimmed = reason.trim();
        int maxReasonLength = getMaxReasonLength();
        if (trimmed.length() > maxReasonLength) {
            return ValidationResult.failure("Reason too long (max " + maxReasonLength + " characters)");
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
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isInputValidationEnabled()) {
            // If validation is disabled, just try to get the player (may be null)
            if (playerName == null) return ValidationResult.success(null);
            ServerPlayer player = server.getPlayerList().getPlayers().stream()
                .filter(p -> p.getGameProfile().getName().equalsIgnoreCase(playerName))
                .findFirst().orElse(null);
            return ValidationResult.success(player);
        }
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
    public static ValidationResult validateEnchantmentLevel(int level, boolean allowUnsafeEnchants) {
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isInputValidationEnabled()) {
            return ValidationResult.success(level);
        }
        if (level < 1) {
            return ValidationResult.failure("Enchantment level must be at least 1");
        }
        // Normal enchantment level limits (when unsafe enchants are disabled)
        if (!allowUnsafeEnchants && level > 255) {
            return ValidationResult.failure("Enchantment level too high (max 255). Enable unsafe enchantments to use higher levels.");
        }
        // Unsafe enchantment level limits (configurable maximum when enabled)
        if (allowUnsafeEnchants) {
            int maxUnsafeLevel = com.zerog.neoessentials.config.ConfigManager.getInstance().getMaxUnsafeEnchantmentLevel();
            if (level > maxUnsafeLevel) {
                return ValidationResult.failure("Enchantment level exceeds configured maximum (" + maxUnsafeLevel + ")");
            }
        }
        return ValidationResult.success(level);
    }
    
    /**
     * Check for dangerous command patterns.
     *
     * <p>Deliberately narrow: this scans real Minecraft command strings (typed by
     * players, or bound to powertool items), not shell/OS input, so it should only
     * flag patterns with a genuine injection/traversal risk in that context. The
     * previous list included things like {@code ~} (relative coordinates), {@code ;},
     * {@code $}, {@code &&}/{@code ||}, and {@code exec} (which matches the vanilla
     * {@code /execute} command) — all extremely common in ordinary, safe Minecraft
     * commands — and blocked them by default for every player on every command via
     * {@link com.zerog.neoessentials.security.CommandLengthEnforcer}.</p>
     */
    private static boolean containsDangerousCommand(String command) {
        String[] dangerousPatterns = {
            "../", "..\\",                              // path traversal
            "class.forname", "reflection",              // Java reflection abuse
            "file:", "http:", "https:", "ftp:", "jar:"   // URL/file-scheme injection
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

    /**
     * Escapes HTML special characters to prevent XSS.
     */
    public static String escapeHtml(String input) {
        if (input == null) return null;
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                case '&': sb.append("&amp;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}