package com.zerog.neoessentials.webdashboard.backup;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;

/**
 * Manages named full snapshots of server configs and player data.
 *
 * <p>Snapshots are stored as ZIP archives in {@code neoessentials/backups/}.
 * Each archive contains a {@code backup-manifest.json} with metadata and
 * all files from the selected target directories.
 *
 * <p><b>Backup targets:</b>
 * <ul>
 *   <li>{@code configs}   — {@code config/neoessentials/} (mod configuration)</li>
 *   <li>{@code neodata}   — {@code neoessentials/} mod data (economy, homes, etc.)</li>
 *   <li>{@code playerdata}— {@code world/playerdata/} (Minecraft player NBT)</li>
 * </ul>
 */
public class BackupManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupManager.class);
    private static BackupManager INSTANCE;

    /** Where snapshots are stored on disk. */
    public static final Path BACKUP_DIR = Path.of("neoessentials", "backups");

    /** Available backup targets → relative source path. */
    public static final Map<String, String> TARGET_PATHS;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("configs",    "config/neoessentials");
        m.put("neodata",    "neoessentials");           // excluding "backups" sub-dir
        m.put("playerdata", "world/playerdata");
        TARGET_PATHS = Collections.unmodifiableMap(m);
    }

    /** Maximum snapshots to retain (oldest removed when exceeded). */
    private static final int MAX_SNAPSHOTS = 20;

    private static final DateTimeFormatter TS_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private BackupManager() {}

    public static BackupManager getInstance() {
        if (INSTANCE == null) INSTANCE = new BackupManager();
        return INSTANCE;
    }

    // ── Create snapshot ───────────────────────────────────────────────────────

    /**
     * Creates a ZIP snapshot named {@code <name>.zip} in {@link #BACKUP_DIR}.
     * If a snapshot with that name already exists it is overwritten.
     *
     * @param name    human-friendly snapshot name (alphanumeric + dash/underscore)
     * @param targets list of target keys from {@link #TARGET_PATHS}
     * @return the resulting manifest as a {@link JsonObject}
     */
    public JsonObject createSnapshot(String name, List<String> targets) throws IOException {
        if (name == null || !name.matches("[a-zA-Z0-9_\\-]{1,64}")) {
            throw new IllegalArgumentException("Invalid snapshot name. Use letters, numbers, - or _  (max 64 chars).");
        }
        if (targets == null || targets.isEmpty()) targets = new ArrayList<>(TARGET_PATHS.keySet());

        Files.createDirectories(BACKUP_DIR);
        Path zipFile = BACKUP_DIR.resolve(name + ".zip");

        long startMs = System.currentTimeMillis();
        long fileCount = 0;

        // Build manifest
        JsonObject manifest = new JsonObject();
        manifest.addProperty("name",      name);
        manifest.addProperty("timestamp", Instant.now().toEpochMilli());
        manifest.addProperty("created",   TS_FMT.format(Instant.now()));
        JsonArray targetsArr = new JsonArray();
        targets.forEach(targetsArr::add);
        manifest.add("targets", targetsArr);

        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(Files.newOutputStream(zipFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)))) {
            zos.setLevel(Deflater.DEFAULT_COMPRESSION);

            // Write manifest entry first
            String manifestJson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(manifest);
            zos.putNextEntry(new ZipEntry("backup-manifest.json"));
            zos.write(manifestJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zos.closeEntry();

            // Write target files
            for (String target : targets) {
                String srcPathStr = TARGET_PATHS.get(target);
                if (srcPathStr == null) {
                    LOGGER.warn("[Backup] Unknown target '{}', skipping.", target);
                    continue;
                }
                Path srcDir = Path.of(srcPathStr);
                if (!Files.exists(srcDir)) {
                    LOGGER.warn("[Backup] Target path '{}' does not exist, skipping.", srcDir);
                    continue;
                }
                fileCount += addDirToZip(zos, srcDir, srcPathStr, target);
            }
        }

        long elapsed = System.currentTimeMillis() - startMs;
        long sizeBytes = Files.size(zipFile);
        manifest.addProperty("fileCount", fileCount);
        manifest.addProperty("sizeBytes", sizeBytes);
        manifest.addProperty("elapsedMs", elapsed);

        LOGGER.info("[Backup] Snapshot '{}' created: {} files, {} KB in {}ms",
            name, fileCount, sizeBytes / 1024, elapsed);

        // Prune old snapshots
        pruneOldSnapshots();
        return manifest;
    }

    /**
     * Recursively adds all files under {@code dir} to the ZIP, prefixed with {@code entryPrefix/}.
     * Skips the {@code neoessentials/backups} sub-directory to avoid infinite recursion.
     */
    private long addDirToZip(ZipOutputStream zos, Path dir, String dirStr, String entryPrefix) throws IOException {
        long count = 0;
        Path absDir = dir.toAbsolutePath().normalize();
        Path skipDir = BACKUP_DIR.toAbsolutePath().normalize();

        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes a) {
                // Skip the backups directory to prevent recursion
                if (d.toAbsolutePath().normalize().startsWith(skipDir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes a) throws IOException {
                String relative = absDir.relativize(file.toAbsolutePath().normalize()).toString()
                    .replace('\\', '/');
                String entryName = entryPrefix + "/" + relative;
                zos.putNextEntry(new ZipEntry(entryName));
                Files.copy(file, zos);
                zos.closeEntry();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                LOGGER.warn("[Backup] Could not read '{}': {}", file, exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });
        // count is tracked separately since lambdas can't modify outer `count`
        // re-walk to count; acceptable for small config dirs
        try (var stream = Files.walk(dir)) {
            return stream.filter(p -> !Files.isDirectory(p) &&
                !p.toAbsolutePath().normalize().startsWith(skipDir)).count();
        }
    }

    // ── Restore snapshot ──────────────────────────────────────────────────────

    /**
     * Restores a snapshot:
     * <ol>
     *   <li>Creates an automatic pre-restore backup ({@code pre-restore-<timestamp>}).</li>
     *   <li>Extracts the snapshot ZIP back to its original paths.</li>
     * </ol>
     *
     * @param name snapshot name (without .zip)
     * @return result JSON with pre-restore backup name and file count
     */
    public JsonObject restoreSnapshot(String name) throws IOException {
        Path zipFile = BACKUP_DIR.resolve(name + ".zip");
        if (!Files.exists(zipFile)) throw new IOException("Snapshot '" + name + "' not found.");

        // Auto-backup current state before overwriting
        String preRestoreName = "pre-restore-" + System.currentTimeMillis();
        List<String> targets = getSnapshotTargets(zipFile);
        JsonObject preBackup = createSnapshot(preRestoreName, targets);
        LOGGER.info("[Backup] Pre-restore backup created: {}", preRestoreName);

        long extracted = 0;
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (entryName.equals("backup-manifest.json") || entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }
                Path target = Path.of(entryName);
                // Safety: only allow entries that start with a known target prefix
                boolean safe = TARGET_PATHS.keySet().stream()
                    .anyMatch(k -> entryName.startsWith(k + "/") || entryName.startsWith(TARGET_PATHS.get(k)));
                if (!safe) {
                    LOGGER.warn("[Backup] Skipping unsafe entry '{}'", entryName);
                    zis.closeEntry();
                    continue;
                }
                Files.createDirectories(target.getParent() != null ? target.getParent() : Path.of("."));
                try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(target,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                    zis.transferTo(out);
                }
                extracted++;
                zis.closeEntry();
            }
        }

        LOGGER.info("[Backup] Restored snapshot '{}': {} files extracted.", name, extracted);
        JsonObject result = new JsonObject();
        result.addProperty("success",        true);
        result.addProperty("restored",        name);
        result.addProperty("filesExtracted",  extracted);
        result.addProperty("preRestoreBackup", preRestoreName);
        return result;
    }

    // ── List snapshots ────────────────────────────────────────────────────────

    /** Returns a JSON array of snapshot metadata objects, newest first. */
    public JsonArray listSnapshots() {
        JsonArray arr = new JsonArray();
        if (!Files.exists(BACKUP_DIR)) return arr;

        List<Path> zips = new ArrayList<>();
        try (var ds = Files.newDirectoryStream(BACKUP_DIR, "*.zip")) {
            ds.forEach(zips::add);
        } catch (IOException e) {
            LOGGER.warn("[Backup] Could not list snapshots: {}", e.getMessage());
            return arr;
        }
        // Sort newest first (by filename or file time)
        zips.sort((a, b) -> {
            try { return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a)); }
            catch (IOException ex) { return 0; }
        });

        for (Path zip : zips) {
            try {
                JsonObject meta = readManifest(zip);
                long sizeBytes = Files.size(zip);
                meta.addProperty("sizeBytes", sizeBytes);
                meta.addProperty("sizeMb",    String.format(Locale.ROOT, "%.2f", sizeBytes / 1_048_576.0));
                String fn = zip.getFileName().toString();
                meta.addProperty("filename",  fn);
                meta.addProperty("name",      fn.replaceAll("\\.zip$", ""));
                arr.add(meta);
            } catch (Exception e) {
                LOGGER.warn("[Backup] Could not read manifest from {}: {}", zip, e.getMessage());
                // Add minimal entry so it still shows in the list
                JsonObject minimal = new JsonObject();
                minimal.addProperty("filename", zip.getFileName().toString());
                minimal.addProperty("name",     zip.getFileName().toString().replaceAll("\\.zip$", ""));
                minimal.addProperty("created",  "unknown");
                try { minimal.addProperty("sizeBytes", Files.size(zip)); } catch (IOException ignored) {}
                arr.add(minimal);
            }
        }
        return arr;
    }

    /** Returns overall backup storage statistics as JSON. */
    public JsonObject getStatus() throws IOException {
        JsonObject status = new JsonObject();

        long totalSize = 0;
        long latestMs = 0;
        int count = 0;
        if (Files.exists(BACKUP_DIR)) {
            List<Path> zips = new ArrayList<>();
            try (var ds = Files.newDirectoryStream(BACKUP_DIR, "*.zip")) {
                ds.forEach(zips::add);
            }
            count = zips.size();
            for (Path z : zips) {
                totalSize += Files.size(z);
                long mt = Files.getLastModifiedTime(z).toMillis();
                if (mt > latestMs) latestMs = mt;
            }
        }

        // Always return the full shape — even before BACKUP_DIR exists (e.g. no backup has
        // ever been taken yet) — so dashboard clients don't have to special-case a truncated
        // response missing availableTargets/maxSnapshots/backupDir.
        status.addProperty("count",       count);
        status.addProperty("totalSizeMb", String.format(Locale.ROOT, "%.2f", totalSize / 1_048_576.0));
        status.addProperty("totalSizeBytes", totalSize);
        status.addProperty("lastBackup",  latestMs > 0 ? TS_FMT.format(Instant.ofEpochMilli(latestMs)) : null);
        status.addProperty("maxSnapshots", MAX_SNAPSHOTS);
        status.addProperty("backupDir",   BACKUP_DIR.toString());

        // Available targets
        JsonArray availableTargets = new JsonArray();
        for (Map.Entry<String, String> e : TARGET_PATHS.entrySet()) {
            JsonObject t = new JsonObject();
            t.addProperty("key",    e.getKey());
            t.addProperty("path",   e.getValue());
            t.addProperty("exists", Files.exists(Path.of(e.getValue())));
            availableTargets.add(t);
        }
        status.add("availableTargets", availableTargets);
        return status;
    }

    // ── Delete snapshot ───────────────────────────────────────────────────────

    public boolean deleteSnapshot(String name) throws IOException {
        Path zipFile = BACKUP_DIR.resolve(name + ".zip");
        if (!Files.exists(zipFile)) return false;
        Files.delete(zipFile);
        LOGGER.info("[Backup] Deleted snapshot '{}'", name);
        return true;
    }

    /** Returns the raw ZIP {@link Path} for a snapshot (for streaming download). */
    public Path getSnapshotPath(String name) {
        return BACKUP_DIR.resolve(name + ".zip");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Reads the {@code backup-manifest.json} from inside the ZIP. */
    private JsonObject readManifest(Path zipFile) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipFile)))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if ("backup-manifest.json".equals(e.getName())) {
                    String json = new String(zis.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    return com.google.gson.JsonParser.parseString(json).getAsJsonObject();
                }
                zis.closeEntry();
            }
        }
        return new JsonObject();
    }

    /** Reads the targets list from a snapshot's manifest. */
    private List<String> getSnapshotTargets(Path zipFile) {
        try {
            JsonObject manifest = readManifest(zipFile);
            if (manifest.has("targets")) {
                List<String> targets = new ArrayList<>();
                manifest.getAsJsonArray("targets").forEach(e -> targets.add(e.getAsString()));
                return targets;
            }
        } catch (Exception ignored) {}
        return new ArrayList<>(TARGET_PATHS.keySet());
    }

    /** Removes oldest snapshots when count exceeds {@link #MAX_SNAPSHOTS}. */
    private void pruneOldSnapshots() {
        if (!Files.exists(BACKUP_DIR)) return;
        List<Path> zips = new ArrayList<>();
        try (var ds = Files.newDirectoryStream(BACKUP_DIR, "*.zip")) {
            ds.forEach(zips::add);
        } catch (IOException e) { return; }

        if (zips.size() <= MAX_SNAPSHOTS) return;
        zips.sort((a, b) -> {
            try { return Files.getLastModifiedTime(a).compareTo(Files.getLastModifiedTime(b)); }
            catch (IOException ex) { return 0; }
        });
        int toDelete = zips.size() - MAX_SNAPSHOTS;
        for (int i = 0; i < toDelete; i++) {
            try { Files.delete(zips.get(i)); LOGGER.info("[Backup] Pruned old snapshot: {}", zips.get(i).getFileName()); }
            catch (IOException e) { LOGGER.warn("[Backup] Could not prune snapshot {}: {}", zips.get(i), e.getMessage()); }
        }
    }
}

