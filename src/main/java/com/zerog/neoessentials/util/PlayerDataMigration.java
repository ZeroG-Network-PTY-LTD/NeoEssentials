package com.zerog.neoessentials.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Migrates old monolithic data files to new per-player storage structure.
 *
 * <p>Migration process:</p>
 * <ol>
 *   <li>Check if old file exists (e.g., homes.json)</li>
 *   <li>Read all player data from old file</li>
 *   <li>Split into individual player files</li>
 *   <li>Backup old file as homes.json.backup</li>
 *   <li>Delete old file (optional)</li>
 * </ol>
 *
 * <p>This migration happens automatically on first startup and is idempotent (safe to run multiple times).</p>
 *
 * @author NeoEssentials
 * @version 1.0.0
 */
public class PlayerDataMigration {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerDataMigration.class);

    /**
     * Migrate a monolithic data file to per-player structure.
     *
     * @param oldFileName Old file name (e.g., "homes.json")
     * @param dataType Data type for PlayerDataStore (e.g., "homes")
     * @return Number of players migrated
     */
    public static int migrateToPlayerData(String oldFileName, String dataType) {
        File oldFile = ResourceUtil.getConfigFile(oldFileName);

        // Check if old file exists
        if (!oldFile.exists()) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "No old {} file to migrate", oldFileName);
            return 0;
        }

        NeoLog.info(LOGGER, LogCategory.GENERAL, "════════════════════════════════════════════════════════");
        NeoLog.info(LOGGER, LogCategory.GENERAL, "Migrating {} to per-player storage...", dataType);
        NeoLog.info(LOGGER, LogCategory.GENERAL, "════════════════════════════════════════════════════════");

        try {
            // Load old data
            JsonObject oldData;
            try (FileReader reader = new FileReader(oldFile)) {
                oldData = JsonParser.parseReader(reader).getAsJsonObject();
            }

            if (oldData.keySet().isEmpty()) {
                NeoLog.info(LOGGER, LogCategory.GENERAL, "Old {} file is empty, skipping migration", oldFileName);
                return 0;
            }

            // Create player data store
            PlayerDataStore store = new PlayerDataStore(dataType);

            int migratedCount = 0;
            int failedCount = 0;

            // Migrate each player's data
            for (String playerIdStr : oldData.keySet()) {
                try {
                    UUID playerId = UUID.fromString(playerIdStr);
                    JsonObject playerData = oldData.getAsJsonObject(playerIdStr);

                    // Save to new structure
                    store.save(playerId, playerData);
                    migratedCount++;

                    NeoLog.debug(LOGGER, LogCategory.GENERAL, "Migrated {} data for player {}", dataType, playerId);

                } catch (Exception e) {
                    LOGGER.error("Failed to migrate {} for player {}: {}",
                        dataType, playerIdStr, e.getMessage());
                    failedCount++;
                }
            }

            // Flush all data to disk
            store.flushAll();

            // Backup old file
            File backupFile = new File(oldFile.getAbsolutePath() + ".backup");
            Files.copy(oldFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            NeoLog.info(LOGGER, LogCategory.GENERAL, "✓ Backed up old {} file to {}", oldFileName, backupFile.getName());

            // Delete old file (commented out for safety - admins can delete manually)
            // oldFile.delete();
            // NeoLog.info(LOGGER, LogCategory.GENERAL, "✓ Deleted old {} file", oldFileName);

            // Rename old file to .migrated to prevent re-migration
            File migratedFile = new File(oldFile.getAbsolutePath() + ".migrated");
            Files.move(oldFile.toPath(), migratedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            NeoLog.info(LOGGER, LogCategory.GENERAL, "✓ Renamed old {} file to {} (safe to delete manually)",
                oldFileName, migratedFile.getName());

            NeoLog.info(LOGGER, LogCategory.GENERAL, "════════════════════════════════════════════════════════");
            NeoLog.info(LOGGER, LogCategory.GENERAL, "✓ Migration complete!");
            NeoLog.info(LOGGER, LogCategory.GENERAL, "  - {} migrated: {} players", dataType, migratedCount);
            if (failedCount > 0) {
                LOGGER.warn("  - Failed: {} players", failedCount);
            }
            NeoLog.info(LOGGER, LogCategory.GENERAL, "  - Old file backed up: {}", backupFile.getName());
            NeoLog.info(LOGGER, LogCategory.GENERAL, "  - New location: config/neoessentials/playerdata/{}/", dataType);
            NeoLog.info(LOGGER, LogCategory.GENERAL, "════════════════════════════════════════════════════════");

            return migratedCount;

        } catch (Exception e) {
            LOGGER.error("Failed to migrate {}: {}", oldFileName, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Migrate all data types to per-player structure.
     */
    public static void migrateAll() {
        NeoLog.info(LOGGER, LogCategory.GENERAL, "Checking for data migrations...");

        int totalMigrated = 0;

        // Migrate homes
        totalMigrated += migrateToPlayerData("homes.json", "homes");

        // Add more migrations as needed:
        // totalMigrated += migrateToPlayerData("economy.json", "economy");
        // totalMigrated += migrateToPlayerData("mail.json", "mail");

        if (totalMigrated > 0) {
            NeoLog.info(LOGGER, LogCategory.GENERAL, "Total players migrated across all systems: {}", totalMigrated);
        } else {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "No migrations needed");
        }
    }

    /**
     * Check if migration is needed for a specific data type.
     *
     * @param oldFileName Old file name
     * @return true if old file exists and migration is needed
     */
    public static boolean needsMigration(String oldFileName) {
        File oldFile = ResourceUtil.getConfigFile(oldFileName);
        File migratedFile = new File(oldFile.getAbsolutePath() + ".migrated");

        // Need migration if old file exists and hasn't been migrated yet
        return oldFile.exists() && !migratedFile.exists();
    }
}

