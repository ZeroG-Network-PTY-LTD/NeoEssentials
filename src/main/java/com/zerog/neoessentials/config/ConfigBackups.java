package com.zerog.neoessentials.config;

import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The copies taken of a config file right before an automatic version upgrade merges new keys
 * into it ({@code <name>_v<oldVersion>_backup_<timestamp>.json}). These used to be written next
 * to the live configs and never cleaned up, so every config version bump left one more file in
 * config/neoessentials/ — a long-running server accumulated dozens. They now live in
 * config/neoessentials/backups/, and only the newest {@link #KEEP_PER_FILE} per config are kept.
 */
public final class ConfigBackups {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigBackups.class);

    /** Newest backups kept per config file; older ones are deleted. */
    static final int KEEP_PER_FILE = 5;

    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd_HH-mm-ss";
    // group(1) = config name without ".json", group(2) = timestamp (sorts chronologically as text)
    private static final Pattern BACKUP_NAME =
        Pattern.compile("^(.+)_v\\d+_backup_(\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2})\\.json$");

    private ConfigBackups() {}

    public static File backupDir() {
        return new File(ResourceUtil.CONFIG_DIR, "backups");
    }

    /**
     * Copies {@code configFile} into the backup folder as
     * {@code <name>_v<oldVersion>_backup_<timestamp>.json}, then prunes old backups.
     */
    public static File backup(File configFile, int oldVersion) throws IOException {
        File dir = backupDir();
        Files.createDirectories(dir.toPath());
        String timestamp = new java.text.SimpleDateFormat(TIMESTAMP_FORMAT).format(new java.util.Date());
        File backupFile = new File(dir, configFile.getName().replace(".json",
            String.format("_v%d_backup_%s.json", oldVersion, timestamp)));
        Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        prune(dir);
        return backupFile;
    }

    /**
     * Moves backups that older versions left next to the configs (config/neoessentials/ and its
     * templates/ folder) into the backup folder, then prunes. Safe to call on every startup.
     */
    public static void tidyUp() {
        File dir = backupDir();
        File configDir = new File(ResourceUtil.CONFIG_DIR);
        for (File folder : new File[] { configDir, new File(configDir, "templates") }) {
            File[] stray = folder.listFiles(f -> f.isFile() && BACKUP_NAME.matcher(f.getName()).matches());
            if (stray == null || stray.length == 0) continue;
            try {
                Files.createDirectories(dir.toPath());
            } catch (IOException e) {
                LOGGER.warn("Could not create config backup folder {}: {}", dir.getPath(), e.getMessage());
                return;
            }
            int moved = 0;
            for (File f : stray) {
                try {
                    Files.move(f.toPath(), new File(dir, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    moved++;
                } catch (IOException e) {
                    LOGGER.warn("Could not move old config backup {}: {}", f.getName(), e.getMessage());
                }
            }
            NeoLog.info(LOGGER, LogCategory.CONFIG, "Moved {} old config backup(s) from {} into {}", moved, folder.getPath(), dir.getPath());
        }
        prune(dir);
    }

    /** Deletes all but the newest {@link #KEEP_PER_FILE} backups of each config file. */
    private static void prune(File dir) {
        File[] all = dir.listFiles(f -> f.isFile() && BACKUP_NAME.matcher(f.getName()).matches());
        if (all == null) return;

        Map<String, List<File>> byConfig = new HashMap<>();
        for (File f : all) {
            Matcher m = BACKUP_NAME.matcher(f.getName());
            if (m.matches()) {
                byConfig.computeIfAbsent(m.group(1), k -> new ArrayList<>()).add(f);
            }
        }

        int deleted = 0;
        for (List<File> backups : byConfig.values()) {
            if (backups.size() <= KEEP_PER_FILE) continue;
            backups.sort(Comparator.comparing(ConfigBackups::timestampOf).reversed());
            for (File old : backups.subList(KEEP_PER_FILE, backups.size())) {
                if (old.delete()) {
                    deleted++;
                } else {
                    LOGGER.warn("Could not delete old config backup {}", old.getName());
                }
            }
        }
        if (deleted > 0) {
            NeoLog.info(LOGGER, LogCategory.CONFIG, "Deleted {} old config backup(s), keeping the newest {} per config file", deleted, KEEP_PER_FILE);
        }
    }

    private static String timestampOf(File backup) {
        Matcher m = BACKUP_NAME.matcher(backup.getName());
        return m.matches() ? m.group(2) : "";
    }
}
