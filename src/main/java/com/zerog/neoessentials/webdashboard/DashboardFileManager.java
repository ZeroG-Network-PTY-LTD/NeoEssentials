package com.zerog.neoessentials.webdashboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * Manages dashboard static files (HTML, CSS, JS)
 * Handles extraction from JAR and version tracking
 * Automatically updates files when newer versions are available
 */
public class DashboardFileManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardFileManager.class);

    // Dashboard files directory (external to JAR for easy customization)
    private static final String DASHBOARD_DIR = "neoessentials/webdashboard/";
    private static final String VERSION_FILE = "neoessentials/webdashboard/.version";

    // Files to manage (auto-update when mod version changes)
    // Updated to include new multi-page structure and new authentication system
    private static final List<String> DASHBOARD_FILES = Arrays.asList(
        "index.html",
        "login.html",
        "permissions.html",
        "admin.html",
        "teleport.html",
        "backup.html",
        "stats.html",
        "discord.html",
        "cloud.html",
        "users.html",
        "moderation.html",
        "kits.html",
        "holograms.html",
        "dashboard.js",
        "permissions.js",
        "teleport.js",
        "backup.js",
        "stats.js",
        "discord.js",
        "cloud.js",
        "users.js",
        "moderation.js",
        "kits.js",
        "holograms.js",
        "styles.css"
    );

    // ── Update result ──────────────────────────────────────────────────────────

    /**
     * Summary returned by {@link #smartUpdateDashboardFiles(boolean)}.
     */
    public static class UpdateSummary {
        /** Files that existed and had different content — overwritten (or would-be in dry-run). */
        public final List<String> updated  = new ArrayList<>();
        /** Files that did not exist externally — newly written (or would-be in dry-run). */
        public final List<String> added    = new ArrayList<>();
        /** Files whose external copy already matched the JAR — left untouched. */
        public final List<String> unchanged = new ArrayList<>();
        /** Files that could not be processed due to errors. */
        public final List<String> failed   = new ArrayList<>();

        public boolean hasChanges() {
            return !updated.isEmpty() || !added.isEmpty();
        }

        public int total() {
            return updated.size() + added.size() + unchanged.size() + failed.size();
        }
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Ensure dashboard files are up to date.
     * Extracts from JAR if missing or if mod version changed since last extraction.
     */
    public static void ensureDashboardFiles() {
        try {
            File dashboardDir = new File(DASHBOARD_DIR);
            if (!dashboardDir.exists()) {
                if (!dashboardDir.mkdirs()) {
                    LOGGER.error("Failed to create dashboard directory: {}", DASHBOARD_DIR);
                    return;
                }
                LOGGER.info("Created dashboard directory: {}", DASHBOARD_DIR);
            }

            String currentVersion  = getCurrentModVersion();
            String installedVersion = getInstalledDashboardVersion();
            LOGGER.debug("Dashboard version check — Current: {}, Installed: {}", currentVersion, installedVersion);

            boolean needsUpdate = shouldUpdateDashboard(currentVersion, installedVersion);
            if (needsUpdate) {
                LOGGER.info("Dashboard files need update. Extracting from JAR...");
                extractDashboardFiles();
                saveInstalledVersion(currentVersion);
                LOGGER.info("Dashboard files updated to version {}", currentVersion);
            } else {
                boolean allFilesExist = verifyDashboardFiles();
                if (!allFilesExist) {
                    LOGGER.info("Some dashboard files are missing. Re-extracting...");
                    extractDashboardFiles();
                    saveInstalledVersion(currentVersion);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error ensuring dashboard files are up to date", e);
        }
    }

    /**
     * Smart per-file update: compares MD5 checksums of JAR vs external copy,
     * and only overwrites files whose content differs.
     *
     * @param dryRun  when {@code true} no files are written — only the summary is returned
     * @return an {@link UpdateSummary} describing what was (or would be) changed
     */
    public static UpdateSummary smartUpdateDashboardFiles(boolean dryRun) {
        UpdateSummary summary = new UpdateSummary();

        // Ensure directory
        File dashboardDir = new File(DASHBOARD_DIR);
        if (!dryRun && !dashboardDir.exists()) {
            dashboardDir.mkdirs();
        }

        for (String fileName : DASHBOARD_FILES) {
            try {
                byte[] jarBytes = readJarFileBytes(fileName);
                if (jarBytes == null) {
                    LOGGER.warn("[DashboardUpdate] {} not found in JAR — skipping", fileName);
                    summary.failed.add(fileName);
                    continue;
                }

                File externalFile = new File(DASHBOARD_DIR + fileName);
                if (!externalFile.exists()) {
                    // New file — doesn't exist externally yet
                    if (!dryRun) writeFile(externalFile, jarBytes);
                    summary.added.add(fileName);
                    LOGGER.debug("[DashboardUpdate] Added: {}", fileName);
                } else {
                    // Compare checksums
                    String jarMd5      = md5Hex(jarBytes);
                    String externalMd5 = md5HexOfFile(externalFile);
                    if (!jarMd5.equals(externalMd5)) {
                        if (!dryRun) writeFile(externalFile, jarBytes);
                        summary.updated.add(fileName);
                        LOGGER.debug("[DashboardUpdate] Updated: {} (JAR={}, disk={})",
                            fileName, jarMd5.substring(0, 8), externalMd5.substring(0, 8));
                    } else {
                        summary.unchanged.add(fileName);
                        LOGGER.debug("[DashboardUpdate] Unchanged: {}", fileName);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[DashboardUpdate] Error processing {}: {}", fileName, e.getMessage());
                summary.failed.add(fileName);
            }
        }

        if (!dryRun && summary.hasChanges()) {
            saveInstalledVersion(getCurrentModVersion());
        }

        return summary;
    }

    /**
     * Force re-extraction of ALL dashboard files regardless of checksum
     * (used by the legacy force path; prefer {@link #smartUpdateDashboardFiles} for the command).
     */
    public static void forceUpdateDashboardFiles() {
        LOGGER.info("Forcing full dashboard files update...");
        extractDashboardFiles();
        String currentVersion = getCurrentModVersion();
        saveInstalledVersion(currentVersion);
        LOGGER.info("Dashboard files force-updated to version {}", currentVersion);
    }

    /**
     * Returns the current mod build version string (from {@code build_number.txt} in the JAR).
     */
    public static String getCurrentModVersion() {
        try (InputStream in = DashboardFileManager.class.getResourceAsStream("/build_number.txt")) {
            if (in != null) {
                return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
            }
        } catch (Exception e) {
            LOGGER.debug("Could not read build number: {}", e.getMessage());
        }
        return "unknown";
    }

    /**
     * Returns the version string last written by {@link #saveInstalledVersion}.
     */
    public static String getInstalledDashboardVersion() {
        File versionFile = new File(VERSION_FILE);
        if (!versionFile.exists()) return "none";
        try {
            return Files.readString(versionFile.toPath(), java.nio.charset.StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            LOGGER.debug("Could not read dashboard version file: {}", e.getMessage());
            return "unknown";
        }
    }

    // ── File access helpers ────────────────────────────────────────────────────

    /**
     * Get path to external dashboard file, or null if it doesn't exist.
     */
    public static Path getExternalDashboardFile(String fileName) {
        File file = new File(DASHBOARD_DIR + fileName);
        if (file.exists() && file.isFile()) return file.toPath();
        return null;
    }

    /**
     * Get InputStream for dashboard file (tries external first, then JAR).
     */
    public static InputStream getDashboardFileStream(String fileName) throws IOException {
        Path externalFile = getExternalDashboardFile(fileName);
        if (externalFile != null) {
            LOGGER.debug("Serving dashboard file from external directory: {}", fileName);
            return Files.newInputStream(externalFile);
        }
        String jarPath = "/webdashboard/" + fileName;
        InputStream jarStream = DashboardFileManager.class.getResourceAsStream(jarPath);
        if (jarStream != null) {
            LOGGER.debug("Serving dashboard file from JAR: {}", fileName);
            return jarStream;
        }
        throw new FileNotFoundException("Dashboard file not found: " + fileName);
    }

    /**
     * Check if all external dashboard files exist (external directory is populated).
     */
    public static boolean isUsingExternalFiles() {
        return new File(DASHBOARD_DIR).exists() && verifyDashboardFiles();
    }

    // ── Internal helpers ───────────────────────────────────────────────────────

    private static void saveInstalledVersion(String version) {
        try {
            File versionFile = new File(VERSION_FILE);
            Files.writeString(versionFile.toPath(), version, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Could not save dashboard version file: {}", e.getMessage());
        }
    }

    private static boolean shouldUpdateDashboard(String currentVersion, String installedVersion) {
        if ("none".equals(installedVersion) || "unknown".equals(installedVersion)) return true;
        return !currentVersion.equals(installedVersion);
    }

    private static boolean verifyDashboardFiles() {
        for (String fileName : DASHBOARD_FILES) {
            if (!new File(DASHBOARD_DIR + fileName).exists()) {
                LOGGER.debug("Dashboard file missing: {}", fileName);
                return false;
            }
        }
        return true;
    }

    private static void extractDashboardFiles() {
        int ok = 0, fail = 0;
        for (String fileName : DASHBOARD_FILES) {
            if (extractFile(fileName)) ok++; else fail++;
        }
        if (ok   > 0) LOGGER.info("Extracted {} dashboard file(s) successfully", ok);
        if (fail > 0) LOGGER.warn("Failed to extract {} dashboard file(s)", fail);
    }

    private static boolean extractFile(String fileName) {
        String jarPath   = "/webdashboard/" + fileName;
        File targetFile  = new File(DASHBOARD_DIR + fileName);
        try (InputStream in = DashboardFileManager.class.getResourceAsStream(jarPath)) {
            if (in == null) { LOGGER.warn("Dashboard file not found in JAR: {}", jarPath); return false; }
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) { LOGGER.error("Failed to create parent directory for {}", fileName); return false; }
            }
            writeFile(targetFile, in.readAllBytes());
            LOGGER.debug("Extracted dashboard file: {}", fileName);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to extract dashboard file {}: {}", fileName, e.getMessage());
            return false;
        }
    }

    private static void writeFile(File target, byte[] bytes) throws IOException {
        try (FileOutputStream out = new FileOutputStream(target)) {
            out.write(bytes);
        }
    }

    /** Read entire file bytes from the JAR resource, or null if not present. */
    private static byte[] readJarFileBytes(String fileName) {
        String jarPath = "/webdashboard/" + fileName;
        try (InputStream in = DashboardFileManager.class.getResourceAsStream(jarPath)) {
            if (in == null) return null;
            return in.readAllBytes();
        } catch (IOException e) {
            LOGGER.debug("Could not read JAR resource {}: {}", jarPath, e.getMessage());
            return null;
        }
    }

    /** Compute lowercase hex MD5 of a byte array. */
    private static String md5Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(Arrays.hashCode(data)); // fallback
        }
    }

    /** Compute lowercase hex MD5 of a File on disk. */
    private static String md5HexOfFile(File file) {
        try {
            return md5Hex(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            return "error";
        }
    }
}
