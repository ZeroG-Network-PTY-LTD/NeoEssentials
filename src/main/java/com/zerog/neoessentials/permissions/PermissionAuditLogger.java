package com.zerog.neoessentials.permissions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Writes a persistent, append-only audit log for every permission modification
 * made through NeoEssentials.
 *
 * <p>Log file: {@code neoessentials/permissions_audit.log}
 *
 * <p>Each line is a single TSV record:
 * <pre>
 * [ISO-8601 timestamp]  ACTION  executor(name/uuid)  target  detail
 * </pre>
 *
 * <p>Audit logging is controlled by the config key
 * {@code permissions.auditLogging} (default {@code true}).
 * When disabled, all log calls are no-ops.
 */
public class PermissionAuditLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionAuditLogger.class);
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
    private static final Path LOG_FILE =
            com.zerog.neoessentials.util.ResourceUtil.getConfigPath("permissions_audit.log");

    // ── Action constants ────────────────────────────────────────────────────

    public static final String USER_GROUP_SET             = "USER_GROUP_SET";
    public static final String USER_PERM_ADDED            = "USER_PERM_ADDED";
    public static final String USER_PERM_REMOVED          = "USER_PERM_REMOVED";
    public static final String USER_PERMS_CLEARED         = "USER_PERMS_CLEARED";
    public static final String GROUP_CREATED              = "GROUP_CREATED";
    public static final String GROUP_DELETED              = "GROUP_DELETED";
    public static final String GROUP_RENAMED              = "GROUP_RENAMED";
    public static final String GROUP_CLONED               = "GROUP_CLONED";
    public static final String GROUP_PERM_ADDED           = "GROUP_PERM_ADDED";
    public static final String GROUP_PERM_REMOVED         = "GROUP_PERM_REMOVED";
    public static final String GROUP_PERMS_CLEARED        = "GROUP_PERMS_CLEARED";
    public static final String GROUP_INHERIT_ADDED        = "GROUP_INHERIT_ADDED";
    public static final String GROUP_INHERIT_REMOVED      = "GROUP_INHERIT_REMOVED";
    public static final String GROUP_PREFIX_SET           = "GROUP_PREFIX_SET";
    public static final String GROUP_SUFFIX_SET           = "GROUP_SUFFIX_SET";
    public static final String GROUP_PRIORITY_SET         = "GROUP_PRIORITY_SET";
    public static final String PERMISSIONS_RELOADED       = "PERMISSIONS_RELOADED";

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Log a permission audit event.
     *
     * @param executorDisplay  display name of the executor (e.g. "Steve" or "CONSOLE")
     * @param action           one of the {@code ACTION_*} constants above
     * @param target           the affected entity (group name or player name/UUID)
     * @param detail           human-readable description of the change
     */
    public static void log(String executorDisplay, String action, String target, String detail) {
        if (!isEnabled()) return;
        String ts = FMT.format(Instant.now());
        String line = "[" + ts + "] " + pad(action, 24) + " | executor=" + executorDisplay
                + " | target=" + target + " | " + detail;
        try {
            Files.createDirectories(LOG_FILE.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(
                    LOG_FILE, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                w.write(line);
                w.newLine();
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to write to permissions audit log: {}", e.getMessage());
        }
    }

    /** Convenience overload — uses "CONSOLE" as executor. */
    public static void log(String action, String target, String detail) {
        log("CONSOLE", action, target, detail);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static boolean isEnabled() {
        try {
            return com.zerog.neoessentials.config.ConfigManager.getInstance()
                    .isPermissionAuditEnabled();
        } catch (Exception e) {
            return true; // default on if ConfigManager not yet ready
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static String pad(String s, int width) {
        if (s.length() >= width) return s;
        return s + " ".repeat(width - s.length());
    }
}

