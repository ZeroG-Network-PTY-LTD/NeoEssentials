package com.zerog.neoessentials.api;

import com.zerog.neoessentials.NeoEssentialsManager;
import com.zerog.neoessentials.api.economy.EconomyService;
import com.zerog.neoessentials.api.permissions.PermissionsService;
import com.zerog.neoessentials.api.permissions.PermissionsServiceImpl;

/**
 * Main API entrypoint for NeoEssentials mod interoperability.
 *
 * <p>Example usage from an external mod:
 * <pre>{@code
 * // Economy
 * EconomyService eco = NeoEssentialsAPI.getEconomyService();
 *
 * // Permissions
 * PermissionsService perms = NeoEssentialsAPI.getPermissionsService();
 * boolean canFly = perms.hasPermission(player, "neoessentials.fly");
 * perms.registerPermission("mymod.cool_feature", "Enables the cool feature");
 * }</pre>
 */
public class NeoEssentialsAPI {
    public static final String API_VERSION = "1.1.0";

    /**
     * Checks if the NeoEssentials API is available for use by other mods.
     * @return true if available
     */
    public static boolean isAvailable() {
        return true;
    }

    /**
     * Provides access to the global EconomyService instance.
     * <p>
     * Usage:
     * <pre>
     * import com.zerog.neoessentials.api.NeoEssentialsAPI;
     * import com.zerog.neoessentials.api.economy.EconomyService;
     * EconomyService eco = NeoEssentialsAPI.getEconomyService();
     * </pre>
     * @return the singleton EconomyService instance
     */
    public static EconomyService getEconomyService() {
        return NeoEssentialsManager.getInstance().getEconomyService();
    }

    /**
     * Provides access to the global {@link PermissionsService} instance.
     *
     * <p>The service exposes the full NeoEssentials permission resolution chain and
     * allows external mods to:
     * <ul>
     *   <li>Check permissions with optional runtime context (world / time / gamemode)</li>
     *   <li>Register their own permission nodes (shows in {@code /permissions search})</li>
     *   <li>Register permission aliases for migration / legacy compatibility</li>
     *   <li>Query group membership, prefix, and suffix</li>
     * </ul>
     *
     * @return the singleton PermissionsService instance
     */
    public static PermissionsService getPermissionsService() {
        return PermissionsServiceImpl.getInstance();
    }
}

