package com.zerog.neoessentials.localization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Language file validation system for NeoEssentials
 * Ensures language files are complete and properly formatted
 * 
 * @author ZeroG
 * @since 2.0.0 (Phase 5 Final Polish)
 */
public class LanguageValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageValidator.class);
    
    // Required message keys that every language file must have
    private static final Set<String> REQUIRED_KEYS = Set.of(
        // Basic commands
        "command.success", "command.failure", "command.error", "command.no_permission",
        "command.player_not_found", "command.invalid_usage", "command.cooldown",
        
        // Economy
        "economy.balance", "economy.insufficient_funds", "economy.transaction_success",
        "economy.invalid_amount", "economy.pay_success", "economy.pay_received",
        
        // Homes
        "home.set_success", "home.teleport_success", "home.not_found", "home.max_reached",
        "home.delete_success", "home.list_header", "home.cost_required",
        
        // Warps
        "warp.teleport_success", "warp.not_found", "warp.list_header", "warp.create_success",
        "warp.delete_success", "warp.permission_required",
        
        // Kits
        "kit.received", "kit.not_found", "kit.cooldown", "kit.no_permission",
        "kit.cost_required", "kit.list_header",
        
        // Teleportation
        "teleport.success", "teleport.unsafe", "teleport.request_sent", "teleport.request_received",
        "teleport.request_expired", "teleport.already_pending", "teleport.cancelled",
        
        // Moderation
        "moderation.mute_success", "moderation.unmute_success", "moderation.ban_success",
        "moderation.unban_success", "moderation.kick_success", "moderation.already_muted",
        
        // General
        "general.reload_success", "general.config_saved", "general.invalid_world",
        "general.feature_disabled", "general.maintenance_mode", "general.server_restart"
    );
    
    // Pattern for valid message keys
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_\\.]*[a-z0-9]$");
    
    // Pattern for valid placeholders
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{[A-Z_]+\\}");
    
    /**
     * Validation result for a language file
     */
    public static class ValidationResult {
        private final String languageCode;
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final Set<String> missingKeys = new HashSet<>();
        private final Set<String> extraKeys = new HashSet<>();
        private final Map<String, String> invalidKeys = new HashMap<>();
        
        public ValidationResult(String languageCode) {
            this.languageCode = languageCode;
        }
        
        public void addError(String error) { errors.add(error); }
        public void addWarning(String warning) { warnings.add(warning); }
        public void addMissingKey(String key) { missingKeys.add(key); }
        public void addExtraKey(String key) { extraKeys.add(key); }
        public void addInvalidKey(String key, String reason) { invalidKeys.put(key, reason); }
        
        public boolean isValid() { return errors.isEmpty() && missingKeys.isEmpty(); }
        public boolean hasWarnings() { return !warnings.isEmpty() || !extraKeys.isEmpty(); }
        
        public String getLanguageCode() { return languageCode; }
        public List<String> getErrors() { return new ArrayList<>(errors); }
        public List<String> getWarnings() { return new ArrayList<>(warnings); }
        public Set<String> getMissingKeys() { return new HashSet<>(missingKeys); }
        public Set<String> getExtraKeys() { return new HashSet<>(extraKeys); }
        public Map<String, String> getInvalidKeys() { return new HashMap<>(invalidKeys); }
        
        public int getCompletenessPercentage() {
            int total = REQUIRED_KEYS.size();
            int missing = missingKeys.size();
            return (int) (((double) (total - missing) / total) * 100);
        }
    }
    
    /**
     * Validate a language file
     */
    public static ValidationResult validateLanguageFile(Path languageFile) {
        String fileName = languageFile.getFileName().toString();
        String languageCode = fileName.replace(".properties", "");
        ValidationResult result = new ValidationResult(languageCode);
        
        if (!Files.exists(languageFile)) {
            result.addError("Language file does not exist: " + languageFile);
            return result;
        }
        
        if (!Files.isReadable(languageFile)) {
            result.addError("Language file is not readable: " + languageFile);
            return result;
        }
        
        try {
            Properties properties = new Properties();
            try (FileInputStream fis = new FileInputStream(languageFile.toFile())) {
                properties.load(fis);
            }
            
            // Check file size
            long fileSize = Files.size(languageFile);
            if (fileSize == 0) {
                result.addError("Language file is empty");
                return result;
            }
            
            if (fileSize > 1024 * 1024) { // 1MB
                result.addWarning("Language file is unusually large (" + fileSize + " bytes)");
            }
            
            // Get all keys from the file
            Set<String> fileKeys = properties.stringPropertyNames();
            
            if (fileKeys.isEmpty()) {
                result.addError("Language file contains no properties");
                return result;
            }
            
            // Check for missing required keys
            for (String requiredKey : REQUIRED_KEYS) {
                if (!fileKeys.contains(requiredKey)) {
                    result.addMissingKey(requiredKey);
                }
            }
            
            // Check for extra keys (not necessarily bad, but worth noting)
            for (String fileKey : fileKeys) {
                if (!REQUIRED_KEYS.contains(fileKey)) {
                    result.addExtraKey(fileKey);
                }
            }
            
            // Validate key formats and values
            for (String key : fileKeys) {
                String value = properties.getProperty(key);
                
                // Check key format
                if (!KEY_PATTERN.matcher(key).matches()) {
                    result.addInvalidKey(key, "Invalid key format (should be lowercase with dots/underscores)");
                }
                
                // Check value
                if (value == null || value.trim().isEmpty()) {
                    result.addInvalidKey(key, "Empty or null value");
                } else {
                    // Check for common issues
                    if (value.length() > 500) {
                        result.addWarning("Very long message value for key: " + key);
                    }
                    
                    // Check for unmatched placeholders
                    if (value.contains("{") && !PLACEHOLDER_PATTERN.matcher(value).find()) {
                        result.addWarning("Possible malformed placeholder in key: " + key);
                    }
                    
                    // Check for color codes without proper format
                    if (value.contains("&") && !value.matches(".*&[0-9a-fklmnor].*")) {
                        result.addWarning("Possible invalid color code in key: " + key);
                    }
                }
            }
            
            // Log completion percentage
            int completeness = result.getCompletenessPercentage();
            if (completeness < 100) {
                result.addWarning("Language file is " + completeness + "% complete");
            }
            
        } catch (IOException e) {
            result.addError("Failed to read language file: " + e.getMessage());
        } catch (Exception e) {
            result.addError("Error validating language file: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validate all language files in a directory
     */
    public static Map<String, ValidationResult> validateAllLanguageFiles(Path languageDir) {
        Map<String, ValidationResult> results = new HashMap<>();
        
        if (!Files.exists(languageDir) || !Files.isDirectory(languageDir)) {
            LOGGER.warn("Language directory does not exist or is not a directory: {}", languageDir);
            return results;
        }
        
        try {
            Files.list(languageDir)
                .filter(path -> path.toString().endsWith(".properties"))
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    String languageCode = fileName.replace(".properties", "");
                    ValidationResult result = validateLanguageFile(path);
                    results.put(languageCode, result);
                });
        } catch (IOException e) {
            LOGGER.error("Error listing language directory: {}", languageDir, e);
        }
        
        return results;
    }
    
    /**
     * Generate a report for validation results
     */
    public static String generateValidationReport(Map<String, ValidationResult> results) {
        StringBuilder report = new StringBuilder();
        report.append("=== LANGUAGE VALIDATION REPORT ===\n\n");
        
        int totalLanguages = results.size();
        int validLanguages = 0;
        int languagesWithWarnings = 0;
        
        for (Map.Entry<String, ValidationResult> entry : results.entrySet()) {
            String lang = entry.getKey();
            ValidationResult result = entry.getValue();
            
            report.append("Language: ").append(lang).append(" (").append(result.getCompletenessPercentage()).append("% complete)\n");
            
            if (result.isValid()) {
                validLanguages++;
                report.append("  ✓ Valid\n");
            } else {
                report.append("  ✗ Invalid\n");
                for (String error : result.getErrors()) {
                    report.append("    ERROR: ").append(error).append("\n");
                }
                if (!result.getMissingKeys().isEmpty()) {
                    report.append("    MISSING KEYS: ").append(String.join(", ", result.getMissingKeys())).append("\n");
                }
            }
            
            if (result.hasWarnings()) {
                languagesWithWarnings++;
                for (String warning : result.getWarnings()) {
                    report.append("    WARNING: ").append(warning).append("\n");
                }
                if (!result.getExtraKeys().isEmpty() && result.getExtraKeys().size() > 10) {
                    report.append("    INFO: ").append(result.getExtraKeys().size()).append(" extra keys found\n");
                }
            }
            
            report.append("\n");
        }
        
        report.append("=== SUMMARY ===\n");
        report.append("Total Languages: ").append(totalLanguages).append("\n");
        report.append("Valid Languages: ").append(validLanguages).append("\n");
        report.append("Languages with Warnings: ").append(languagesWithWarnings).append("\n");
        report.append("Success Rate: ").append(totalLanguages > 0 ? (validLanguages * 100 / totalLanguages) : 0).append("%\n");
        
        return report.toString();
    }
    
    /**
     * Get required message keys
     */
    public static Set<String> getRequiredKeys() {
        return new HashSet<>(REQUIRED_KEYS);
    }
    
    /**
     * Generate a template properties file with all required keys
     */
    public static Properties generateTemplate() {
        Properties template = new Properties();
        
        // Add all required keys with placeholder values
        template.setProperty("command.success", "&aCommand executed successfully!");
        template.setProperty("command.failure", "&cCommand failed to execute.");
        template.setProperty("command.error", "&cAn error occurred: {ERROR}");
        template.setProperty("command.no_permission", "&cYou don't have permission to use this command.");
        template.setProperty("command.player_not_found", "&cPlayer '{PLAYER}' not found.");
        template.setProperty("command.invalid_usage", "&cInvalid usage. Try: {USAGE}");
        template.setProperty("command.cooldown", "&cYou must wait {TIME} before using this command again.");
        
        template.setProperty("economy.balance", "&aYour balance: &6{BALANCE}");
        template.setProperty("economy.insufficient_funds", "&cInsufficient funds. You need {AMOUNT}.");
        template.setProperty("economy.transaction_success", "&aTransaction completed successfully!");
        template.setProperty("economy.invalid_amount", "&cInvalid amount specified.");
        template.setProperty("economy.pay_success", "&aYou paid &6{AMOUNT} &ato &f{PLAYER}");
        template.setProperty("economy.pay_received", "&aYou received &6{AMOUNT} &afrom &f{PLAYER}");
        
        template.setProperty("home.set_success", "&aHome '{NAME}' set successfully!");
        template.setProperty("home.teleport_success", "&aTeleported to home '{NAME}'");
        template.setProperty("home.not_found", "&cHome '{NAME}' not found.");
        template.setProperty("home.max_reached", "&cYou have reached the maximum number of homes ({MAX}).");
        template.setProperty("home.delete_success", "&aHome '{NAME}' deleted successfully!");
        template.setProperty("home.list_header", "&aYour homes:");
        template.setProperty("home.cost_required", "&cSetting a home costs &6{COST}");
        
        template.setProperty("warp.teleport_success", "&aTeleported to warp '{NAME}'");
        template.setProperty("warp.not_found", "&cWarp '{NAME}' not found.");
        template.setProperty("warp.list_header", "&aAvailable warps:");
        template.setProperty("warp.create_success", "&aWarp '{NAME}' created successfully!");
        template.setProperty("warp.delete_success", "&aWarp '{NAME}' deleted successfully!");
        template.setProperty("warp.permission_required", "&cYou don't have permission to use this warp.");
        
        template.setProperty("kit.received", "&aYou received kit '{NAME}'");
        template.setProperty("kit.not_found", "&cKit '{NAME}' not found.");
        template.setProperty("kit.cooldown", "&cYou must wait {TIME} before claiming this kit again.");
        template.setProperty("kit.no_permission", "&cYou don't have permission to claim this kit.");
        template.setProperty("kit.cost_required", "&cThis kit costs &6{COST}");
        template.setProperty("kit.list_header", "&aAvailable kits:");
        
        template.setProperty("teleport.success", "&aTeleported successfully!");
        template.setProperty("teleport.unsafe", "&cTeleportation cancelled - unsafe destination.");
        template.setProperty("teleport.request_sent", "&aTeleport request sent to {PLAYER}");
        template.setProperty("teleport.request_received", "&a{PLAYER} wants to teleport to you. &e/tpaccept &aor &e/tpdeny");
        template.setProperty("teleport.request_expired", "&cTeleport request expired.");
        template.setProperty("teleport.already_pending", "&cYou already have a pending teleport request.");
        template.setProperty("teleport.cancelled", "&cTeleport request cancelled.");
        
        template.setProperty("moderation.mute_success", "&aPlayer {PLAYER} has been muted for {DURATION}");
        template.setProperty("moderation.unmute_success", "&aPlayer {PLAYER} has been unmuted.");
        template.setProperty("moderation.ban_success", "&aPlayer {PLAYER} has been banned.");
        template.setProperty("moderation.unban_success", "&aPlayer {PLAYER} has been unbanned.");
        template.setProperty("moderation.kick_success", "&aPlayer {PLAYER} has been kicked.");
        template.setProperty("moderation.already_muted", "&cPlayer {PLAYER} is already muted.");
        
        template.setProperty("general.reload_success", "&aConfiguration reloaded successfully!");
        template.setProperty("general.config_saved", "&aConfiguration saved successfully!");
        template.setProperty("general.invalid_world", "&cInvalid world: {WORLD}");
        template.setProperty("general.feature_disabled", "&cThis feature is currently disabled.");
        template.setProperty("general.maintenance_mode", "&cServer is in maintenance mode.");
        template.setProperty("general.server_restart", "&cServer is restarting in {TIME}");
        
        return template;
    }
}
