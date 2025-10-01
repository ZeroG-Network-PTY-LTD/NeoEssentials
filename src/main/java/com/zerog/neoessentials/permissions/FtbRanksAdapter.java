
package com.zerog.neoessentials.permissions;

import java.util.UUID;
import net.neoforged.fml.ModList;
import com.zerog.neoessentials.util.DebugUtil;

/**
 * Stub implementation for FTB Ranks integration.
 * Replace with real FTB Ranks API calls if available.
 */
/**
 * Adapter for FTB Ranks integration using reflection to avoid hard dependency.
 */
public class FtbRanksAdapter implements ExternalPermissionAdapter {
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
                DebugUtil.debugErr("Failed to load FTB Ranks API: " + e.getMessage());
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
                DebugUtil.debugStackTrace(e);
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
                DebugUtil.debugStackTrace(e);
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
                DebugUtil.debugStackTrace(e);
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
