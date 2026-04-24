package com.zerog.neoessentials.inventory;

import com.zerog.neoessentials.util.ResourceUtil;
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
 * Writes a persistent, append-only audit log for every inventory view/edit action
 * performed through NeoEssentials inventory commands.
 *
 * <p>Log file: {@code neoessentials/inventory_audit.log}
 *
 * <p>Each line is a single record:
 * <pre>
 * [ISO-8601 timestamp]  ACTION                 | executor=NAME | target=NAME | detail
 * </pre>
 *
 * <p>Audit logging is controlled by the config key
 * {@code items.inventoryAuditLog} (default {@code true}).
 */
public class InventoryAuditLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryAuditLogger.class);
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
    private static final Path LOG_FILE =
            ResourceUtil.getDataPath("inventory_audit.log");

    // ── Action constants ────────────────────────────────────────────────────

    public static final String INV_VIEWED      = "INV_VIEWED";
    public static final String INV_EDIT_OPENED = "INV_EDIT_OPENED";
    public static final String INV_EDIT_CLOSED = "INV_EDIT_CLOSED";
    public static final String EC_VIEWED       = "EC_VIEWED";
    public static final String EC_EDIT_OPENED  = "EC_EDIT_OPENED";
    public static final String EC_EDIT_CLOSED  = "EC_EDIT_CLOSED";
    public static final String EDIT_BLOCKED    = "EDIT_BLOCKED";

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Log an inventory audit event.
     *
     * @param executor display name of the staff member performing the action
     * @param action   one of the ACTION constants above
     * @param target   the player whose inventory is being accessed
     * @param detail   human-readable description of the event
     */
    public static void log(String executor, String action, String target, String detail) {
        if (!isEnabled()) return;
        String ts = FMT.format(Instant.now());
        String line = "[" + ts + "] " + pad(action, 18)
                + " | executor=" + executor
                + " | target=" + target
                + " | " + detail;
        try {
            Files.createDirectories(LOG_FILE.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(
                    LOG_FILE, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                w.write(line);
                w.newLine();
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to write to inventory audit log: {}", e.getMessage());
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static boolean isEnabled() {
        try {
            com.zerog.neoessentials.config.ConfigManager cm =
                    com.zerog.neoessentials.config.ConfigManager.getInstance();
            if (cm == null) return true;
            com.google.gson.JsonObject config = cm.getConfig(
                    com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
            if (config.has("items")) {
                com.google.gson.JsonObject items = config.getAsJsonObject("items");
                if (items.has("inventoryAuditLog")) {
                    return items.get("inventoryAuditLog").getAsBoolean();
                }
            }
        } catch (Exception e) {
            // ConfigManager not yet ready — default on
        }
        return true;
    }

    private static String pad(String s, int width) {
        if (s.length() >= width) return s;
        return s + " ".repeat(width - s.length());
    }
}

