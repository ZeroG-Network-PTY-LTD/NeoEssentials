package com.zerog.neoessentials.permissions;

import java.util.UUID;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for Bukkit/Sponge/Mohist permission systems using reflection.
 * Supports hybrid servers without hard dependencies.
 */
public class BukkitSpongeAdapter implements ExternalPermissionAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BukkitSpongeAdapter.class);
    private final boolean available;
    private Object pluginManager;
    private Class<?> pluginManagerClass;
    private boolean isBukkit;
    private boolean isSponge;

    public BukkitSpongeAdapter() {
        boolean found = false;
        try {
            // Try Bukkit
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            pluginManager = bukkitClass.getMethod("getPluginManager").invoke(null);
            pluginManagerClass = Class.forName("org.bukkit.plugin.PluginManager");
            isBukkit = true;
            found = true;
            NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "Bukkit API detected for permissions.");
        } catch (Exception e) {
            // Not Bukkit, try Sponge
            NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Bukkit API not present, probing for Sponge", e);
            try {
                Class<?> spongeClass = Class.forName("org.spongepowered.api.Sponge");
                pluginManager = spongeClass.getMethod("server").invoke(null);
                pluginManagerClass = Class.forName("org.spongepowered.api.Server");
                isSponge = true;
                found = true;
                NeoLog.info(LOGGER, LogCategory.PERMISSIONS, "Sponge API detected for permissions.");
            } catch (Exception ignored) {
                // Not Sponge
                NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Sponge API not present either — Bukkit/Sponge adapter unavailable", ignored);
            }
        }
        available = found;
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        if (!available) return false;
        try {
            if (isBukkit) {
                // Bukkit: get player by UUID, then hasPermission
                Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
                Object player = bukkitClass.getMethod("getPlayer", UUID.class).invoke(null, uuid);
                if (player != null) {
                    boolean result = (boolean) player.getClass().getMethod("hasPermission", String.class).invoke(player, permission);
                    NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Bukkit permission check: player={}, permission={}, result={}", uuid, permission, result);
                    return result;
                }
            } else if (isSponge) {
                // Sponge: get player by UUID, then hasPermission
                Class<?> spongeClass = Class.forName("org.spongepowered.api.Sponge");
                Object server = spongeClass.getMethod("server").invoke(null);
                Object player = server.getClass().getMethod("player", UUID.class).invoke(server, uuid);
                if (player != null) {
                    Object subject = player.getClass().getMethod("subject").invoke(player);
                    boolean result = (boolean) subject.getClass().getMethod("hasPermission", String.class).invoke(subject, permission);
                    NeoLog.debug(LOGGER, LogCategory.PERMISSIONS, "Sponge permission check: player={}, permission={}, result={}", uuid, permission, result);
                    return result;
                }
            }
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.PERMISSIONS, "Failed to check Bukkit/Sponge permission for player {} / {}", uuid, permission, e);
        }
        return false;
    }

    @Override
    public String getPrefix(UUID uuid) {
        // Not implemented for Bukkit/Sponge by default
        return null;
    }

    @Override
    public String getSuffix(UUID uuid) {
        // Not implemented for Bukkit/Sponge by default
        return null;
    }

    @Override
    public void reload() {
        // No-op
    }

    @Override
    public String getName() {
        return isBukkit ? "Bukkit" : (isSponge ? "Sponge" : "Unknown");
    }

    @Override
    public boolean isAvailable() {
        return available;
    }
}
