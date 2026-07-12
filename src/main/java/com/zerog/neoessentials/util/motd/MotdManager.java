package com.zerog.neoessentials.util.motd;

import com.google.gson.*;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;
import com.zerog.neoessentials.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Manages MOTD profiles, rotation, persistence, and in-game feedback.
 *
 * <p>Persisted via the active {@link DataStore}:
 * <ul>
 *   <li>{@code motd_profiles} collection — one record per profile, keyed by profile
 *       name, holding {@code motd}/{@code author}/{@code timestamp}.</li>
 *   <li>{@code motd_meta} collection — a single record (id {@code "settings"}) holding
 *       {@code activeProfile} and the rotation settings ({@code rotationEnabled},
 *       {@code rotationIntervalMinutes}, {@code rotationCurrentIndex}).</li>
 * </ul>
 *
 * <p>Legacy installs are migrated in from {@code config/neoessentials/motd_data.json}
 * (both the multi-profile layout and the older single-motd-at-root layout) the first
 * time the profile collection is empty — see {@link #migrateLegacyFileIfNeeded()}.
 */
public class MotdManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MotdManager.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

    private static final String PROFILE_COLLECTION = "motd_profiles";
    private static final String META_COLLECTION = "motd_meta";
    private static final String META_ID = "settings";

    private final DataStore store;

    // ── Singleton ──────────────────────────────────────────────────────────────
    private static MotdManager INSTANCE;

    public static MotdManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MotdManager();
        }
        return INSTANCE;
    }

    // ── State ──────────────────────────────────────────────────────────────────
    /** Profile name → MotdProfile */
    private final Map<String, MotdProfile> profiles = new LinkedHashMap<>();
    private String activeProfile = "default";

    // Rotation
    private boolean rotationEnabled = false;
    private int rotationIntervalMinutes = 60;
    private int rotationCurrentIndex = 0;
    private ScheduledExecutorService rotationScheduler;

    private MotdManager() {
        this.store = StorageManager.getInstance().getStore();
        migrateLegacyFileIfNeeded();
        load();
    }

    // ── Profile data class ─────────────────────────────────────────────────────
    public static class MotdProfile {
        public String name;
        public String motd;
        public String author;
        public String timestamp;

        public MotdProfile(String name, String motd, String author, String timestamp) {
            this.name = name;
            this.motd = motd;
            this.author = author;
            this.timestamp = timestamp;
        }
    }

    // ── Load / Save ────────────────────────────────────────────────────────────

    /**
     * Load profiles from the active {@link DataStore}.  Returns a human-readable error
     * string on failure, or {@code null} on success.
     */
    public String load() {
        try {
            Map<String, JsonObject> raw = store.getAll(PROFILE_COLLECTION);

            profiles.clear();
            if (raw.isEmpty()) {
                // Nothing persisted yet — create default profile so the store gets written.
                profiles.put("default", new MotdProfile("default", "", "Server",
                        LocalDateTime.now().format(TIME_FMT)));
                activeProfile = "default";
                save();
                return null;
            }

            // DataStore doesn't guarantee insertion order across every backend (e.g. SQL
            // row order isn't the JSON-file field order the old format relied on for
            // rotation), so iterate profile names in a deterministic (alphabetical) order
            // instead of whatever order the backend happens to return.
            for (String name : new TreeSet<>(raw.keySet())) {
                JsonObject p = raw.get(name);
                profiles.put(name, new MotdProfile(
                        name,
                        p.has("motd")      ? p.get("motd").getAsString()      : "",
                        p.has("author")    ? p.get("author").getAsString()    : "Server",
                        p.has("timestamp") ? p.get("timestamp").getAsString() : ""
                ));
            }

            JsonObject meta = store.get(META_COLLECTION, META_ID);
            if (meta != null) {
                activeProfile           = meta.has("activeProfile")           ? meta.get("activeProfile").getAsString()           : "default";
                rotationEnabled         = meta.has("rotationEnabled")         && meta.get("rotationEnabled").getAsBoolean();
                rotationIntervalMinutes = meta.has("rotationIntervalMinutes") ? meta.get("rotationIntervalMinutes").getAsInt()    : 60;
                rotationCurrentIndex    = meta.has("rotationCurrentIndex")    ? meta.get("rotationCurrentIndex").getAsInt()       : 0;
            } else {
                activeProfile = "default";
            }

            // Always ensure a default profile exists
            if (profiles.isEmpty()) {
                profiles.put("default", new MotdProfile("default", "", "Server",
                        LocalDateTime.now().format(TIME_FMT)));
            }

            // Validate active profile reference
            if (!profiles.containsKey(activeProfile)) {
                String fallback = profiles.keySet().iterator().next();
                LOGGER.warn("Active MOTD profile '{}' not found; falling back to '{}'",
                        activeProfile, fallback);
                activeProfile = fallback;
            }

            // (Re)start rotation scheduler if needed
            applyRotationSchedule();

            LOGGER.debug("MOTD data loaded from storage backend '{}'", StorageManager.getInstance().getActiveType());
            return null;

        } catch (Exception e) {
            String msg = "Failed to load MOTD data: " + e.getMessage();
            LOGGER.error(msg, e);
            // Reset to safe defaults
            profiles.clear();
            profiles.put("default", new MotdProfile("default", "", "Server", ""));
            activeProfile = "default";
            return msg;
        }
    }

    /**
     * Save profiles to the active {@link DataStore}.  Returns a human-readable error
     * string on failure, or {@code null} on success.
     */
    public String save() {
        try {
            JsonObject meta = new JsonObject();
            meta.addProperty("activeProfile", activeProfile);
            meta.addProperty("rotationEnabled", rotationEnabled);
            meta.addProperty("rotationIntervalMinutes", rotationIntervalMinutes);
            meta.addProperty("rotationCurrentIndex", rotationCurrentIndex);
            store.put(META_COLLECTION, META_ID, meta);

            for (Map.Entry<String, MotdProfile> e : profiles.entrySet()) {
                store.put(PROFILE_COLLECTION, e.getKey(), profileToJson(e.getValue()));
            }

            LOGGER.debug("MOTD data saved to storage backend '{}'", StorageManager.getInstance().getActiveType());
            return null;
        } catch (Exception e) {
            String msg = "Failed to save MOTD data: " + e.getMessage();
            LOGGER.error(msg, e);
            return msg;
        }
    }

    private JsonObject profileToJson(MotdProfile profile) {
        JsonObject p = new JsonObject();
        p.addProperty("motd",      profile.motd);
        p.addProperty("author",    profile.author);
        p.addProperty("timestamp", profile.timestamp);
        return p;
    }

    // ── Active profile helpers ─────────────────────────────────────────────────

    public MotdProfile getActiveProfile() {
        return profiles.getOrDefault(activeProfile,
                profiles.isEmpty() ? null : profiles.values().iterator().next());
    }

    public String getActiveMotd() {
        MotdProfile p = getActiveProfile();
        return p != null ? p.motd : "";
    }

    public boolean hasMotd() {
        return !getActiveMotd().isEmpty();
    }

    // ── Profile management ─────────────────────────────────────────────────────

    /** Returns an unmodifiable view of all profiles. */
    public Map<String, MotdProfile> getProfiles() {
        return Collections.unmodifiableMap(profiles);
    }

    public String getActiveProfileName() {
        return activeProfile;
    }

    /**
     * Create or update a profile. Returns error string or null.
     */
    public String setProfile(String name, String motd, String author) {
        if (name == null || name.trim().isEmpty())
            return "Profile name cannot be empty";
        if (motd.length() > 2000)
            return "MOTD message too long (max 2000 characters)";
        name = name.toLowerCase(Locale.ROOT);
        String ts = LocalDateTime.now().format(TIME_FMT);
        profiles.put(name, new MotdProfile(name, motd, author, ts));
        return save();
    }

    /**
     * Delete a profile. Returns error string or null.
     */
    public String deleteProfile(String name) {
        name = name.toLowerCase(Locale.ROOT);
        if (!profiles.containsKey(name))
            return "Profile '" + name + "' does not exist";
        if (profiles.size() == 1)
            return "Cannot delete the last MOTD profile";
        profiles.remove(name);
        store.delete(PROFILE_COLLECTION, name);
        if (activeProfile.equals(name)) {
            activeProfile = profiles.keySet().iterator().next();
        }
        return save();
    }

    /**
     * Switch the active profile. Returns error string or null.
     */
    public String switchProfile(String name) {
        name = name.toLowerCase(Locale.ROOT);
        if (!profiles.containsKey(name))
            return "Profile '" + name + "' does not exist";
        activeProfile = name;
        return save();
    }

    // ── Rotation ───────────────────────────────────────────────────────────────

    public boolean isRotationEnabled()          { return rotationEnabled; }
    public int getRotationIntervalMinutes()     { return rotationIntervalMinutes; }

    /**
     * Enable or disable rotation.
     * @param enabled              Whether rotation should run.
     * @param intervalMinutes      How often to switch (minutes).
     * Returns error string or null.
     */
    public String setRotation(boolean enabled, int intervalMinutes) {
        if (intervalMinutes < 1) return "Interval must be at least 1 minute";
        this.rotationEnabled = enabled;
        this.rotationIntervalMinutes = intervalMinutes;
        applyRotationSchedule();
        return save();
    }

    /** Rotate to the next profile immediately (and save the updated index). */
    public void rotateNext() {
        if (profiles.isEmpty()) return;
        List<String> keys = new ArrayList<>(profiles.keySet());
        rotationCurrentIndex = (rotationCurrentIndex + 1) % keys.size();
        activeProfile = keys.get(rotationCurrentIndex);
        save();
        LOGGER.debug("MOTD rotated to profile '{}'", activeProfile);
    }

    private void applyRotationSchedule() {
        // Cancel existing scheduler
        if (rotationScheduler != null && !rotationScheduler.isShutdown()) {
            rotationScheduler.shutdownNow();
            rotationScheduler = null;
        }
        if (rotationEnabled && profiles.size() > 1) {
            rotationScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "neoessentials-motd-rotation");
                t.setDaemon(true);
                return t;
            });
            rotationScheduler.scheduleAtFixedRate(this::rotateNext,
                    rotationIntervalMinutes, rotationIntervalMinutes, TimeUnit.MINUTES);
            LOGGER.info("MOTD rotation enabled: switching every {} minutes", rotationIntervalMinutes);
        }
    }

    /** Shutdown the rotation scheduler (called on server stop). */
    public void shutdown() {
        if (rotationScheduler != null && !rotationScheduler.isShutdown()) {
            rotationScheduler.shutdownNow();
        }
    }

    // ── Legacy migration ─────────────────────────────────────────────────────────

    /**
     * One-time import of the legacy {@code motd_data.json} config file into the active
     * DataStore, if it's still empty and storage.autoMigrate is enabled. Handles both the
     * multi-profile layout ({@code profiles: {...}}) and the older single-motd-at-root
     * layout ({@code motd}/{@code author}/{@code timestamp} directly on the root object).
     */
    private void migrateLegacyFileIfNeeded() {
        if (store.hasAnyData(PROFILE_COLLECTION)) return;
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;

        File legacyFile = ResourceUtil.getConfigFile("motd_data.json");
        if (!legacyFile.exists()) return;

        int migrated = 0;
        try {
            byte[] bytes = Files.readAllBytes(legacyFile.toPath());
            JsonObject root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();

            String legacyActive = root.has("activeProfile") ? root.get("activeProfile").getAsString() : "default";
            boolean legacyRotEnabled = false;
            int legacyRotInterval = 60;
            int legacyRotIndex = 0;
            if (root.has("rotation")) {
                JsonObject rot = root.getAsJsonObject("rotation");
                legacyRotEnabled = rot.has("enabled") && rot.get("enabled").getAsBoolean();
                legacyRotInterval = rot.has("intervalMinutes") ? rot.get("intervalMinutes").getAsInt() : 60;
                legacyRotIndex = rot.has("currentIndex") ? rot.get("currentIndex").getAsInt() : 0;
            }

            if (root.has("profiles")) {
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("profiles").entrySet()) {
                    JsonObject p = entry.getValue().getAsJsonObject();
                    JsonObject rec = new JsonObject();
                    rec.addProperty("motd",      p.has("motd")      ? p.get("motd").getAsString()      : "");
                    rec.addProperty("author",    p.has("author")    ? p.get("author").getAsString()    : "Server");
                    rec.addProperty("timestamp", p.has("timestamp") ? p.get("timestamp").getAsString() : "");
                    store.put(PROFILE_COLLECTION, entry.getKey(), rec);
                    migrated++;
                }
            } else if (root.has("motd")) {
                // Legacy single-motd format (motd / author / timestamp at root)
                JsonObject rec = new JsonObject();
                rec.addProperty("motd", root.get("motd").getAsString());
                rec.addProperty("author", root.has("author") ? root.get("author").getAsString() : "Server");
                rec.addProperty("timestamp", root.has("timestamp") ? root.get("timestamp").getAsString() : "");
                store.put(PROFILE_COLLECTION, "default", rec);
                legacyActive = "default";
                migrated++;
                LOGGER.info("Migrated legacy single-profile motd_data.json into multi-profile storage");
            }

            if (migrated > 0) {
                JsonObject meta = new JsonObject();
                meta.addProperty("activeProfile", legacyActive);
                meta.addProperty("rotationEnabled", legacyRotEnabled);
                meta.addProperty("rotationIntervalMinutes", legacyRotInterval);
                meta.addProperty("rotationCurrentIndex", legacyRotIndex);
                store.put(META_COLLECTION, META_ID, meta);
                LOGGER.info("MotdManager: migrated {} MOTD profile(s) from legacy motd_data.json into the '{}' storage backend.",
                    migrated, StorageManager.getInstance().getActiveType());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to migrate legacy motd_data.json: {}", e.getMessage(), e);
        }
    }
}
