package com.zerog.neoessentials.resourcepack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Resource Pack Generator - Phase 3 Custom Badge Images
 * Automatically generates a resource pack from badge images in config/neoessentials/badges/
 * Creates proper font definitions and pack structure
 */
public class ResourcePackGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePackGenerator.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final int PACK_FORMAT = 34; // MC 1.21.1-1.21.11
    private static final String PACK_NAME = "NeoEssentials-Badges";

    /**
     * Generate the resource pack from badge images.
     *
     * @return Path to generated pack ZIP file, or null if failed
     */
    public static Path generateResourcePack() {
        try {
            LOGGER.info("Generating NeoEssentials badge resource pack...");

            Path badgesDir = getBadgesDirectory();
            if (!Files.exists(badgesDir)) {
                LOGGER.warn("Badges directory does not exist: {}", badgesDir);
                return null;
            }

            // Find all PNG files
            Map<String, Path> badgeImages = findBadgeImages(badgesDir);
            if (badgeImages.isEmpty()) {
                LOGGER.warn("No badge images found in {}", badgesDir);
                return null;
            }

            LOGGER.info("Found {} badge images to pack", badgeImages.size());

            // Create temp directory for pack structure
            Path tempDir = Files.createTempDirectory("neoessentials-pack-");

            try {
                // Build pack structure
                buildPackStructure(tempDir, badgeImages);

                // Create ZIP file
                Path outputZip = Paths.get("config/neoessentials/", PACK_NAME + ".zip");
                Files.createDirectories(outputZip.getParent());

                createZipFile(tempDir, outputZip);

                LOGGER.info("Resource pack generated successfully: {}", outputZip.toAbsolutePath());

                // Calculate SHA-1 hash
                String sha1 = calculateSHA1(outputZip);
                LOGGER.info("Resource pack SHA-1: {}", sha1);

                // Save hash to file for server.properties
                saveSHA1(sha1);

                return outputZip;

            } finally {
                // Cleanup temp directory
                deleteDirectory(tempDir);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to generate resource pack: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Build the resource pack directory structure.
     */
    private static void buildPackStructure(Path packDir, Map<String, Path> badgeImages) throws IOException {
        // Create pack.mcmeta
        createPackMeta(packDir);

        // Create assets structure
        Path assetsDir = packDir.resolve("assets/neoessentials");
        Path fontDir = assetsDir.resolve("font");
        Path texturesDir = assetsDir.resolve("textures/badges");

        Files.createDirectories(fontDir);
        Files.createDirectories(texturesDir);

        // Copy badge images
        for (Map.Entry<String, Path> entry : badgeImages.entrySet()) {
            String rankName = entry.getKey();
            Path sourceImage = entry.getValue();
            Path targetImage = texturesDir.resolve(rankName + ".png");

            Files.copy(sourceImage, targetImage, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.debug("Copied badge image: {} -> {}", rankName, targetImage);
        }

        // Create font definition
        createFontDefinition(fontDir, badgeImages);
    }

    /**
     * Create pack.mcmeta file.
     */
    private static void createPackMeta(Path packDir) throws IOException {
        JsonObject packMeta = new JsonObject();
        JsonObject pack = new JsonObject();

        pack.addProperty("pack_format", PACK_FORMAT);
        pack.addProperty("description", "NeoEssentials Custom Badge Images");

        packMeta.add("pack", pack);

        Path metaFile = packDir.resolve("pack.mcmeta");
        try (Writer writer = Files.newBufferedWriter(metaFile)) {
            GSON.toJson(packMeta, writer);
        }

        LOGGER.debug("Created pack.mcmeta");
    }

    /**
     * Create font definition JSON for badges.
     */
    private static void createFontDefinition(Path fontDir, Map<String, Path> badgeImages) throws IOException {
        JsonObject fontDef = new JsonObject();
        JsonArray providers = new JsonArray();

        int unicodePoint = 0xE100; // Start at E100
        int imageSize = getConfiguredImageSize();

        for (String rankName : badgeImages.keySet()) {
            JsonObject provider = new JsonObject();
            provider.addProperty("type", "bitmap");
            provider.addProperty("file", "neoessentials:badges/" + rankName + ".png");
            provider.addProperty("ascent", imageSize / 2); // Center vertically
            provider.addProperty("height", imageSize);

            JsonArray chars = new JsonArray();
            chars.add(String.valueOf((char) unicodePoint));
            provider.add("chars", chars);

            providers.add(provider);

            LOGGER.debug("Mapped {} to \\u{}", rankName, Integer.toHexString(unicodePoint).toUpperCase());
            unicodePoint++;
        }

        fontDef.add("providers", providers);

        Path fontFile = fontDir.resolve("badges.json");
        try (Writer writer = Files.newBufferedWriter(fontFile)) {
            GSON.toJson(fontDef, writer);
        }

        LOGGER.debug("Created font definition with {} badge mappings", badgeImages.size());
    }

    /**
     * Create ZIP file from directory.
     */
    private static void createZipFile(Path sourceDir, Path zipFile) throws IOException {
        if (Files.exists(zipFile)) {
            Files.delete(zipFile);
        }

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile));
             var pathStream = Files.walk(sourceDir)) {
            pathStream
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    try {
                        String zipEntryName = sourceDir.relativize(path).toString().replace("\\", "/");
                        zos.putNextEntry(new ZipEntry(zipEntryName));
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        }

        LOGGER.debug("Created ZIP file: {} ({}bytes)", zipFile, Files.size(zipFile));
    }

    /**
     * Calculate SHA-1 hash of file.
     */
    private static String calculateSHA1(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");

        try (InputStream is = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
        }

        byte[] hash = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * Save SHA-1 hash to file.
     */
    private static void saveSHA1(String sha1) throws IOException {
        Path sha1File = Paths.get("config/neoessentials/", PACK_NAME + ".sha1");
        Files.writeString(sha1File, sha1);
        LOGGER.debug("Saved SHA-1 to {}", sha1File);
    }

    /**
     * Find all badge image files.
     */
    private static Map<String, Path> findBadgeImages(Path badgesDir) throws IOException {
        Map<String, Path> images = new java.util.HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(badgesDir, "*.png")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String rankName = fileName.substring(0, fileName.lastIndexOf('.'));
                images.put(rankName, path);
            }
        }

        return images;
    }

    /**
     * Delete directory recursively.
     */
    private static void deleteDirectory(Path dir) {
        try {
            if (Files.exists(dir)) {
                try (var pathStream = Files.walk(dir)) {
                    pathStream
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                NeoLog.debug(LOGGER, LogCategory.GENERAL, "Failed to delete temp path {}", path, e);
                            }
                        });
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to cleanup temp directory: {}", e.getMessage());
        }
    }

    /**
     * Get badges directory path.
     */
    private static Path getBadgesDirectory() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("badges")) {
                var badges = chatConfig.getAsJsonObject("badges");
                if (badges.has("customImagePath")) {
                    return Paths.get(badges.get("customImagePath").getAsString());
                }
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Failed to read badges.customImagePath config — using default", e);
        }
        return Paths.get("config/neoessentials/badges");
    }

    /**
     * Get configured image size.
     */
    private static int getConfiguredImageSize() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("badges")) {
                var badges = chatConfig.getAsJsonObject("badges");
                if (badges.has("customImageSize")) {
                    return badges.get("customImageSize").getAsInt();
                }
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Failed to read badges.customImageSize config — using default", e);
        }
        return 16;
    }
}

