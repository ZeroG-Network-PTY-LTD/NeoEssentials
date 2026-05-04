package com.zerog.neoessentials.webdashboard;

import com.zerog.neoessentials.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

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

    /**
     * Ensure dashboard files are up to date
     * Extracts from JAR if missing or if newer version is available
     */
    public static void ensureDashboardFiles() {
        try {
            // Ensure dashboard directory exists
            File dashboardDir = new File(DASHBOARD_DIR);
            if (!dashboardDir.exists()) {
                if (!dashboardDir.mkdirs()) {
                    LOGGER.error("Failed to create dashboard directory: {}", DASHBOARD_DIR);
                    return;
                }
                LOGGER.info("Created dashboard directory: {}", DASHBOARD_DIR);
            }

            // Get current mod version (build number)
            String currentVersion = getCurrentModVersion();
            String installedVersion = getInstalledDashboardVersion();

            LOGGER.debug("Dashboard version check - Current: {}, Installed: {}", currentVersion, installedVersion);

            // Check if we need to update
            boolean needsUpdate = shouldUpdateDashboard(currentVersion, installedVersion);

            if (needsUpdate) {
                LOGGER.info("Dashboard files need update. Extracting from JAR...");
                extractDashboardFiles();
                saveInstalledVersion(currentVersion);
                LOGGER.info("Dashboard files updated to version {}", currentVersion);
            } else {
                // Verify all files exist, extract missing ones
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
     * Get the current mod version (build number)
     */
    private static String getCurrentModVersion() {
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
     * Get the installed dashboard version
     */
    private static String getInstalledDashboardVersion() {
        File versionFile = new File(VERSION_FILE);
        if (!versionFile.exists()) {
            return "none";
        }

        try {
            return Files.readString(versionFile.toPath(), java.nio.charset.StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            LOGGER.debug("Could not read dashboard version file: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * Save the installed dashboard version
     */
    private static void saveInstalledVersion(String version) {
        try {
            File versionFile = new File(VERSION_FILE);
            Files.writeString(versionFile.toPath(), version, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Could not save dashboard version file: {}", e.getMessage());
        }
    }

    /**
     * Determine if dashboard needs update
     */
    private static boolean shouldUpdateDashboard(String currentVersion, String installedVersion) {
        // Always update if no version is installed
        if ("none".equals(installedVersion) || "unknown".equals(installedVersion)) {
            return true;
        }

        // Update if versions differ
        if (!currentVersion.equals(installedVersion)) {
            return true;
        }

        return false;
    }

    /**
     * Verify all dashboard files exist
     */
    private static boolean verifyDashboardFiles() {
        for (String fileName : DASHBOARD_FILES) {
            File file = new File(DASHBOARD_DIR + fileName);
            if (!file.exists()) {
                LOGGER.debug("Dashboard file missing: {}", fileName);
                return false;
            }
        }
        return true;
    }

    /**
     * Extract all dashboard files from JAR to external directory
     */
    private static void extractDashboardFiles() {
        int successCount = 0;
        int failCount = 0;

        for (String fileName : DASHBOARD_FILES) {
            if (extractFile(fileName)) {
                successCount++;
            } else {
                failCount++;
            }
        }

        if (successCount > 0) {
            LOGGER.info("Extracted {} dashboard file(s) successfully", successCount);
        }
        if (failCount > 0) {
            LOGGER.warn("Failed to extract {} dashboard file(s)", failCount);
        }
    }

    /**
     * Extract a single file from JAR to external directory
     */
    private static boolean extractFile(String fileName) {
        String jarPath = "/webdashboard/" + fileName;
        File targetFile = new File(DASHBOARD_DIR + fileName);

        try (InputStream in = DashboardFileManager.class.getResourceAsStream(jarPath)) {
            if (in == null) {
                LOGGER.warn("Dashboard file not found in JAR: {}", jarPath);
                return false;
            }

            // Ensure parent directory exists
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    LOGGER.error("Failed to create parent directory for {}", fileName);
                    return false;
                }
            }

            // Write file
            try (FileOutputStream out = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
            }

            LOGGER.debug("Extracted dashboard file: {}", fileName);
            return true;

        } catch (IOException e) {
            LOGGER.error("Failed to extract dashboard file {}: {}", fileName, e.getMessage());
            return false;
        }
    }

    /**
     * Get path to external dashboard file, or null if it doesn't exist
     */
    public static Path getExternalDashboardFile(String fileName) {
        File file = new File(DASHBOARD_DIR + fileName);
        if (file.exists() && file.isFile()) {
            return file.toPath();
        }
        return null;
    }

    /**
     * Get InputStream for dashboard file (tries external first, then JAR)
     */
    public static InputStream getDashboardFileStream(String fileName) throws IOException {
        // Try external file first
        Path externalFile = getExternalDashboardFile(fileName);
        if (externalFile != null) {
            LOGGER.debug("Serving dashboard file from external directory: {}", fileName);
            return Files.newInputStream(externalFile);
        }

        // Fall back to JAR resource
        String jarPath = "/webdashboard/" + fileName;
        InputStream jarStream = DashboardFileManager.class.getResourceAsStream(jarPath);
        if (jarStream != null) {
            LOGGER.debug("Serving dashboard file from JAR: {}", fileName);
            return jarStream;
        }

        // File not found anywhere
        throw new FileNotFoundException("Dashboard file not found: " + fileName);
    }

    /**
     * Check if external dashboard files are being used
     */
    public static boolean isUsingExternalFiles() {
        return new File(DASHBOARD_DIR).exists() && verifyDashboardFiles();
    }

    /**
     * Force re-extraction of all dashboard files (useful for /dashboard update command)
     */
    public static void forceUpdateDashboardFiles() {
        LOGGER.info("Forcing dashboard files update...");
        extractDashboardFiles();
        String currentVersion = getCurrentModVersion();
        saveInstalledVersion(currentVersion);
        LOGGER.info("Dashboard files force-updated to version {}", currentVersion);
    }
}
