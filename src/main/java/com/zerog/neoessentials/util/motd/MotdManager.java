package com.zerog.neoessentials.util.motd;

import com.google.gson.*;
import com.zerog.neoessentials.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Manages MOTD profiles, rotation, persistence, and in-game feedback.
 *
 * <p>Data file layout (config/neoessentials/motd_data.json):
 * <pre>
 * {
 *   "activeProfile": "default",
 *   "rotation": { "enabled": false, "intervalMinutes": 60, "currentIndex": 0 },
 *   "profiles": {
 *     "default": { "motd": "...", "author": "Server", "timestamp": "..." },
 *     "event":   { "motd": "...", "author": "Admin",  "timestamp": "..." }
 *   }
 * }
 * </pre>
 */
public class MotdManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MotdManager.class);
    private static final File DATA_FILE = ResourceUtil.getConfigFile("motd_data.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

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
     * Load profiles from disk.  Returns a human-readable error string on failure,
     * or {@code null} on success.
     */
    public String load() {
        try {
            if (!DATA_FILE.exists()) {
                ensureDir();
                // Create default profile so the file gets written
                if (profiles.isEmpty()) {
                    profiles.put("default", new MotdProfile("default", "", "Server",
                            LocalDateTime.now().format(TIME_FMT)));
                }
                save();
                return null;
            }

            byte[] bytes = Files.readAllBytes(DATA_FILE.toPath());
            JsonObject root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8))
                    .getAsJsonObject();

            activeProfile = root.has("activeProfile") ? root.get("activeProfile").getAsString() : "default";

            // Rotation settings
            if (root.has("rotation")) {
                JsonObject rot = root.getAsJsonObject("rotation");
                rotationEnabled         = rot.has("enabled")         && rot.get("enabled").getAsBoolean();
                rotationIntervalMinutes = rot.has("intervalMinutes") ? rot.get("intervalMinutes").getAsInt() : 60;
                rotationCurrentIndex    = rot.has("currentIndex")    ? rot.get("currentIndex").getAsInt()    : 0;
            }

            // Profiles
            profiles.clear();
            if (root.has("profiles")) {
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("profiles").entrySet()) {
                    JsonObject p = entry.getValue().getAsJsonObject();
                    profiles.put(entry.getKey(), new MotdProfile(
                            entry.getKey(),
                            p.has("motd")       ? p.get("motd").getAsString()       : "",
                            p.has("author")     ? p.get("author").getAsString()     : "Server",
                            p.has("timestamp")  ? p.get("timestamp").getAsString()  : ""
                    ));
                }
            }

            // Legacy migration: single-motd format (motd / author / timestamp at root)
            if (profiles.isEmpty() && root.has("motd")) {
                String legacyMotd = root.get("motd").getAsString();
                String legacyAuthor = root.has("author") ? root.get("author").getAsString() : "Server";
                String legacyTs    = root.has("timestamp") ? root.get("timestamp").getAsString() : "";
                profiles.put("default", new MotdProfile("default", legacyMotd, legacyAuthor, legacyTs));
                activeProfile = "default";
                save(); // rewrite in new format
                LOGGER.info("Migrated legacy motd_data.json to multi-profile format");
            }

            // Always ensure a default profile exists
            if (profiles.isEmpty()) {
                profiles.put("default", new MotdProfile("default", "", "Server",
                        LocalDateTime.now().format(TIME_FMT)));
            }

            // Validate active profile reference
            if (!profiles.containsKey(activeProfile)) {
                activeProfile = profiles.keySet().iterator().next();
                LOGGER.warn("Active MOTD profile '{}' not found; falling back to '{}'",
                        activeProfile, activeProfile);
            }

            // (Re)start rotation scheduler if needed
            applyRotationSchedule();

            LOGGER.debug("MOTD data loaded from {}", DATA_FILE.getAbsolutePath());
            return null;

        } catch (Exception e) {
            String msg = "Failed to load MOTD data from " + DATA_FILE.getAbsolutePath() + ": " + e.getMessage();
            LOGGER.error(msg, e);
            // Reset to safe defaults
            profiles.clear();
            profiles.put("default", new MotdProfile("default", "", "Server", ""));
            activeProfile = "default";
            return msg;
        }
    }

    /**
     * Save profiles to disk.  Returns a human-readable error string on failure,
     * or {@code null} on success.
     */
    public String save() {
        try {
            ensureDir();
            JsonObject root = new JsonObject();
            root.addProperty("activeProfile", activeProfile);

            JsonObject rot = new JsonObject();
            rot.addProperty("enabled", rotationEnabled);
            rot.addProperty("intervalMinutes", rotationIntervalMinutes);
            rot.addProperty("currentIndex", rotationCurrentIndex);
            root.add("rotation", rot);

            JsonObject profilesObj = new JsonObject();
            for (Map.Entry<String, MotdProfile> e : profiles.entrySet()) {
                JsonObject p = new JsonObject();
                p.addProperty("motd",      e.getValue().motd);
                p.addProperty("author",    e.getValue().author);
                p.addProperty("timestamp", e.getValue().timestamp);
                profilesObj.add(e.getKey(), p);
            }
            root.add("profiles", profilesObj);

            try (FileWriter w = new FileWriter(DATA_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
            LOGGER.debug("MOTD data saved to {}", DATA_FILE.getAbsolutePath());
            return null;
        } catch (Exception e) {
            String msg = "Failed to save MOTD data to " + DATA_FILE.getAbsolutePath() + ": " + e.getMessage();
            LOGGER.error(msg, e);
            return msg;
        }
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
        if (motd.length() > 500)
            return "MOTD message too long (max 500 characters)";
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

    // ── Utils ──────────────────────────────────────────────────────────────────

    private void ensureDir() {
        File dir = DATA_FILE.getParentFile();
        if (dir != null && !dir.exists()) dir.mkdirs();
    }
}

