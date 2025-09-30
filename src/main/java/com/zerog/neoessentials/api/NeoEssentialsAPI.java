package com.zerog.neoessentials.api;

import com.zerog.neoessentials.NeoEssentialsManager;
import com.zerog.neoessentials.api.economy.EconomyService;

/**
 * Main API entrypoint for NeoEssentials mod interoperability.
 */
public class NeoEssentialsAPI {
    public static final String API_VERSION = "1.0.0";

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

    // Future API methods for mod interoperability will be added here.
}
