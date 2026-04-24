package com.zerog.neoessentials.api;

import com.zerog.neoessentials.NeoEssentialsManager;
import com.zerog.neoessentials.api.economy.EconomyService;
import com.zerog.neoessentials.api.permissions.PermissionsService;
import com.zerog.neoessentials.api.permissions.PermissionsServiceImpl;

/**
 * Main API entrypoint for NeoEssentials mod interoperability.
 *
 * <h3>Changelog</h3>
 * <ul>
 *   <li><b>1.2.0</b> — Exposed {@link PlaceholderManager} via {@link #getPlaceholderManager()};
 *       {@link PlaceholderProvider} and {@link PlaceholderExpansion} are now public top-level types.</li>
 *   <li><b>1.1.0</b> — Added {@link PermissionsService} support.</li>
 *   <li><b>1.0.0</b> — Initial {@link EconomyService} support.</li>
 * </ul>
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
 *
 * // Placeholders — register a custom placeholder
 * NeoEssentialsAPI.getPlaceholderManager().registerPlaceholder("mymod_kills",
 *     (player, params) -> player != null ? String.valueOf(MyStats.getKills(player.getUUID())) : "0"
 * );
 *
 * // Placeholders — register a full expansion
 * PlaceholderAPI.registerExpansion(new MyModExpansion());
 * }</pre>
 */
public class NeoEssentialsAPI {
    public static final String API_VERSION = "1.2.0";

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

    /**
     * Provides access to the global {@link PlaceholderManager} instance.
     *
     * <p>Use this to register custom placeholders and expansions from external mods, or to
     * resolve placeholder text server-side. The manager is thread-safe.
     *
     * <p>Example:
     * <pre>{@code
     * import com.zerog.neoessentials.api.NeoEssentialsAPI;
     * import com.zerog.neoessentials.api.PlaceholderManager;
     *
     * PlaceholderManager ph = NeoEssentialsAPI.getPlaceholderManager();
     *
     * // Register a single placeholder
     * ph.registerPlaceholder("mymod_online", (player, params) ->
     *     String.valueOf(server.getPlayerCount()));
     *
     * // Resolve in text
     * String resolved = ph.setPlaceholders(player, "Players online: {mymod_online}");
     * }</pre>
     *
     * @return the singleton {@link PlaceholderManager} instance
     * @since API 1.2.0
     */
    public static PlaceholderManager getPlaceholderManager() {
        return PlaceholderManager.getInstance();
    }
}
