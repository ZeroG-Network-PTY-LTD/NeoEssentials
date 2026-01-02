package com.zerog.neoessentials.api.permissions;

import com.zerog.neoessentials.permissions.PermissionGroup;
import com.zerog.neoessentials.permissions.PermissionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Validates and synchronizes permission nodes between registered permissions and group permissions.
 * This helps identify permission mismatches that can cause permission checks to fail.
 */
public class PermissionValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionValidator.class);

    /**
     * Validate all permissions in groups against registered permissions.
     * Logs warnings for any mismatches found.
     */
    public static ValidationResult validate(PermissionManager manager) {
        LOGGER.info("═══════════════════════════════════════════════════════════");
        LOGGER.info("Validating Permission Nodes...");
        LOGGER.info("═══════════════════════════════════════════════════════════");

        PermissionRegistry registry = PermissionRegistry.getInstance();
        Set<String> registeredPermissions = registry.getAllPermissions();

        List<String> warnings = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        int totalChecked = 0;
        int issuesFound = 0;

        // Check each group's permissions
        for (PermissionGroup group : manager.getGroups()) {
            LOGGER.info("Checking group '{}'...", group.getName());

            for (String permission : group.getPermissions()) {
                totalChecked++;

                // Skip wildcard permissions
                if (permission.endsWith(".*") || permission.equals("*")) {
                    continue;
                }

                // Skip negative permissions
                if (permission.startsWith("-")) {
                    String actualPerm = permission.substring(1);
                    if (!registeredPermissions.contains(actualPerm) && !actualPerm.endsWith(".*")) {
                        warnings.add(String.format("  ⚠ Group '%s': Negative permission '%s' not registered",
                            group.getName(), permission));
                        issuesFound++;
                    }
                    continue;
                }

                // Check if permission is registered
                if (!registeredPermissions.contains(permission)) {
                    // Check if there's a similar registered permission
                    String suggestion = findSimilarPermission(permission, registeredPermissions);

                    if (suggestion != null) {
                        warnings.add(String.format("  ✗ Group '%s': Permission '%s' not registered",
                            group.getName(), permission));
                        suggestions.add(String.format("    → Did you mean '%s'?", suggestion));
                        issuesFound++;
                    } else {
                        warnings.add(String.format("  ✗ Group '%s': Unknown permission '%s'",
                            group.getName(), permission));
                        issuesFound++;
                    }
                }
            }
        }

        // Log results
        LOGGER.info("─────────────────────────────────────────────────────────────");
        LOGGER.info("Validation Results:");
        LOGGER.info("  Total permissions checked: {}", totalChecked);
        LOGGER.info("  Registered permissions: {}", registeredPermissions.size());
        LOGGER.info("  Issues found: {}", issuesFound);

        if (!warnings.isEmpty()) {
            LOGGER.warn("─────────────────────────────────────────────────────────────");
            LOGGER.warn("Permission Issues Detected:");
            for (String warning : warnings) {
                LOGGER.warn(warning);
            }
            if (!suggestions.isEmpty()) {
                LOGGER.warn("Suggestions:");
                for (String suggestion : suggestions) {
                    LOGGER.warn(suggestion);
                }
            }
        }

        LOGGER.info("═══════════════════════════════════════════════════════════");

        return new ValidationResult(totalChecked, issuesFound, warnings, suggestions);
    }

    /**
     * Find a similar registered permission (for typo detection).
     * Uses Levenshtein distance to find close matches.
     */
    private static String findSimilarPermission(String permission, Set<String> registeredPermissions) {
        int minDistance = Integer.MAX_VALUE;
        String bestMatch = null;

        for (String registered : registeredPermissions) {
            // Skip if the registered permission is too different in length
            if (Math.abs(registered.length() - permission.length()) > 10) {
                continue;
            }

            int distance = levenshteinDistance(permission, registered);

            // If very similar (distance <= 3), consider it a match
            if (distance < minDistance && distance <= 3) {
                minDistance = distance;
                bestMatch = registered;
            }
        }

        // Also check for simple prefix/suffix mismatches
        if (bestMatch == null) {
            for (String registered : registeredPermissions) {
                // Check for common mistakes like "items" vs "item"
                if (permission.replace("items.", "item.").equals(registered) ||
                    permission.replace("teleportation.", "teleport.").equals(registered) ||
                    permission.replace("utils.", "").equals(registered) ||
                    permission.replace("paytoggle", "pay.toggle").equals(registered)) {
                    return registered;
                }
            }
        }

        return bestMatch;
    }

    /**
     * Calculate Levenshtein distance between two strings.
     */
    private static int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[len1][len2];
    }

    /**
     * Validation result container.
     */
    public static class ValidationResult {
        private final int totalChecked;
        private final int issuesFound;
        private final List<String> warnings;
        private final List<String> suggestions;

        public ValidationResult(int totalChecked, int issuesFound, List<String> warnings, List<String> suggestions) {
            this.totalChecked = totalChecked;
            this.issuesFound = issuesFound;
            this.warnings = new ArrayList<>(warnings);
            this.suggestions = new ArrayList<>(suggestions);
        }

        public int getTotalChecked() { return totalChecked; }
        public int getIssuesFound() { return issuesFound; }
        public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }
        public List<String> getSuggestions() { return Collections.unmodifiableList(suggestions); }
        public boolean hasIssues() { return issuesFound > 0; }
    }
}
