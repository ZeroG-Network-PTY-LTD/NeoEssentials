package com.zerog.neoessentials.permissions;

import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/**
 * Handler for FTB Ranks permission integration.
 * This class uses ModList to check if FTB Ranks is loaded, then uses reflection
 * to interact with the API to avoid hard dependencies.
 */
public class FTBRanksPermissionHandler implements PermissionHandler {

    private boolean ftbRanksAvailable = false;
    private Object ranksAPI = null;
    private static final String FTB_RANKS_MOD_ID = "ftbranks";
    private static final String FTB_LIBRARY_MOD_ID = "ftblibrary";
    private static final String FTB_RANKS_API_CLASS = "dev.ftb.mods.ftbranks.api.RanksAPI";
    
    public FTBRanksPermissionHandler() {
        try {
            // Check if FTB Ranks is loaded using ModList
            if (ModList.get().isLoaded(FTB_RANKS_MOD_ID)) {
                // Check if FTB Library is also loaded (required dependency)
                if (!ModList.get().isLoaded(FTB_LIBRARY_MOD_ID)) {
                    NeoEssentials.LOGGER.error("FTB Ranks detected but FTB Library is missing! FTB Ranks will not function correctly.");
                    NeoEssentials.LOGGER.error("Please install the FTB Library mod to use FTB Ranks for permissions.");
                    return;
                }
                
                // FTB Ranks and its dependencies are loaded, get the API instance
                Class<?> apiClass = Class.forName(FTB_RANKS_API_CLASS);
                // Get the API instance using the getAPI static method
                ranksAPI = apiClass.getMethod("getAPI").invoke(null);
                
                if (ranksAPI != null) {
                    ftbRanksAvailable = true;
                    NeoEssentials.LOGGER.info("FTB Ranks detected - using for permission checks");
                }
            } else {
                NeoEssentials.LOGGER.debug("FTB Ranks mod not found, skipping permission handler");
            }
        } catch (ClassNotFoundException e) {
            ftbRanksAvailable = false;
            NeoEssentials.LOGGER.debug("FTB Ranks API class not found, skipping permission handler");
        } catch (NoClassDefFoundError e) {
            ftbRanksAvailable = false;
            NeoEssentials.LOGGER.error("FTB Ranks is missing a required dependency: " + e.getMessage());
            NeoEssentials.LOGGER.error("Please ensure FTB Library is installed on your server.");
        } catch (Exception e) {
            ftbRanksAvailable = false;
            NeoEssentials.LOGGER.warn("Error initializing FTB Ranks permission handler", e);
        }
    }
    
    @Override
    public boolean hasPermission(ServerPlayer player, String permission) {
        if (!ftbRanksAvailable || player == null || ranksAPI == null) {
            return false;
        }
        
        try {
            // Get the IRanks instance from RanksAPI and check permission
            Object permValue = ranksAPI.getClass()
                .getMethod("getPermissionValue", ServerPlayer.class, String.class)
                .invoke(ranksAPI, player, permission);
            
            if (permValue != null) {
                // Get the boolean value from the permission result
                return (boolean) permValue.getClass().getMethod("getAsBoolean").invoke(permValue);
            }
            return false;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error checking FTB Ranks permission: {}", permission, e);
            return false;
        }
    }
      @Override
    public boolean isAvailable() {
        return ftbRanksAvailable;
    }
      @Override
    public String getName() {
        return "FTB Ranks";
    }
}
