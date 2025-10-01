
package com.zerog.neoessentials.permissions;

import java.util.UUID;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stub implementation for FTB Ranks integration.
 * Replace with real FTB Ranks API calls if available.
 */
/**
 * Adapter for FTB Ranks integration using reflection to avoid hard dependency.
 */
public class FtbRanksAdapter implements ExternalPermissionAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FtbRanksAdapter.class);
    private final boolean ftbRanksLoaded;
    private Object ftbRanksApi;
    private Class<?> ftbRanksAPIClass;

    public FtbRanksAdapter() {
        this.ftbRanksLoaded = ModList.get().isLoaded("ftbranks");
        if (ftbRanksLoaded) {
            try {
                // Load FTB Ranks API using reflection
                ftbRanksAPIClass = Class.forName("dev.ftb.mods.ftbranks.api.FTBRanksAPI");
                // Get the INSTANCE field
                ftbRanksApi = ftbRanksAPIClass.getField("INSTANCE").get(null);
            } catch (Exception e) {
                LOGGER.error("Failed to load FTB Ranks API: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        if (ftbRanksLoaded && ftbRanksApi != null) {
            try {
                var method = ftbRanksAPIClass.getMethod("hasPermission", UUID.class, String.class);
                return (boolean) method.invoke(ftbRanksApi, uuid, permission);
            } catch (Exception e) {
                LOGGER.error("Failed to check FTB Ranks permission", e);
            }
        }
        return ConfigPermissionUtil.hasPermission(uuid, permission);
    }

    @Override
    public String getPrefix(UUID uuid) {
        if (ftbRanksLoaded && ftbRanksApi != null) {
            try {
                var method = ftbRanksAPIClass.getMethod("getPrefix", UUID.class);
                return (String) method.invoke(ftbRanksApi, uuid);
            } catch (Exception e) {
                LOGGER.error("Failed to get FTB Ranks prefix", e);
            }
        }
        return ConfigPermissionUtil.getPrefix(uuid);
    }

    @Override
    public String getSuffix(UUID uuid) {
        if (ftbRanksLoaded && ftbRanksApi != null) {
            try {
                var method = ftbRanksAPIClass.getMethod("getSuffix", UUID.class);
                return (String) method.invoke(ftbRanksApi, uuid);
            } catch (Exception e) {
                LOGGER.error("Failed to get FTB Ranks suffix", e);
            }
        }
        return ConfigPermissionUtil.getSuffix(uuid);
    }

    @Override
    public void reload() {
        if (!ftbRanksLoaded) {
            ConfigPermissionUtil.reload();
        }
    }

    @Override
    public String getName() {
        return "FTB Ranks";
    }
}
