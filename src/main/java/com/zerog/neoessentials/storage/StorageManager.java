package com.zerog.neoessentials.storage;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Selects and owns the active {@link DataStore} backend, per {@code storage.type} in
 * {@code config.json}. Managers that have been migrated onto this system (see
 * {@code MigrationHelper} for bringing their old bespoke JSON files in) call
 * {@link #getInstance()}{@code .getStore()} instead of doing their own file I/O.
 *
 * <p>If MySQL is configured but unreachable at boot, falls back to the JSON backend
 * rather than leaving every dependent manager unable to persist anything — a
 * misconfigured/offline database shouldn't be able to take down moderation, economy,
 * etc. entirely.
 */
public class StorageManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageManager.class);
    private static StorageManager instance;

    private final DataStore store;
    private final String activeType;

    private StorageManager() {
        ConfigManager config = ConfigManager.getInstance();
        String requestedType = config.getStorageType();
        File storeDir = new File(ResourceUtil.DATA_DIR + "store");

        DataStore resolved;
        String resolvedType;

        switch (requestedType) {
            case "yaml" -> {
                resolved = new YamlFileDataStore(storeDir);
                resolvedType = "yaml";
            }
            case "sqlite" -> {
                resolved = new SqliteDataStore(new File(storeDir, config.getSqliteFile()));
                resolvedType = "sqlite";
            }
            case "mysql" -> {
                MySqlDataStore mysql = new MySqlDataStore(
                    config.getMysqlHost(), config.getMysqlPort(), config.getMysqlDatabase(),
                    config.getMysqlUsername(), config.getMysqlPassword(),
                    config.isMysqlUseSSL(), config.getMysqlPoolSize());
                if (mysql.isConnected()) {
                    resolved = mysql;
                    resolvedType = "mysql";
                } else {
                    LOGGER.error("storage.type is 'mysql' but the connection failed — falling back to JSON "
                        + "storage so the server can still start. Fix storage.mysql.* in config.json and restart.");
                    resolved = new JsonFileDataStore(storeDir);
                    resolvedType = "json";
                }
            }
            default -> {
                resolved = new JsonFileDataStore(storeDir);
                resolvedType = "json";
            }
        }

        this.store = resolved;
        this.activeType = resolvedType;
        LOGGER.info("StorageManager: active backend is '{}'", resolvedType);
    }

    public static StorageManager getInstance() {
        if (instance == null) {
            instance = new StorageManager();
        }
        return instance;
    }

    public DataStore getStore() {
        return store;
    }

    /** The backend actually in use — may differ from config if MySQL fell back to JSON. */
    public String getActiveType() {
        return activeType;
    }

    public void shutdown() {
        store.close();
    }
}
