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
                NeoEssentials.LOGGER.info("FTB Ranks mod found - attempting to initialize permission handler");
                
                // Check if FTB Library is also loaded (required dependency)
                if (!ModList.get().isLoaded(FTB_LIBRARY_MOD_ID)) {
                    NeoEssentials.LOGGER.error("FTB Ranks detected but FTB Library is missing! FTB Ranks will not function correctly.");
                    NeoEssentials.LOGGER.error("Please install the FTB Library mod to use FTB Ranks for permissions.");
                    return;
                }
                
                NeoEssentials.LOGGER.info("FTB Library found - attempting to load FTB Ranks API");
                
                try {
                    // FTB Ranks and its dependencies are loaded, get the API instance
                    Class<?> apiClass = Class.forName(FTB_RANKS_API_CLASS);
                    NeoEssentials.LOGGER.info("FTB Ranks API class found: {}", apiClass.getName());
                    
                    // Get the API instance using the getAPI static method
                    ranksAPI = apiClass.getMethod("getAPI").invoke(null);
                    
                    if (ranksAPI != null) {
                        ftbRanksAvailable = true;
                        NeoEssentials.LOGGER.info("FTB Ranks API loaded successfully - permission handler ready");
                    } else {
                        NeoEssentials.LOGGER.error("FTB Ranks API returned null - permission handling will not work");
                    }
                } catch (ClassNotFoundException e) {
                    NeoEssentials.LOGGER.error("FTB Ranks API class not found: {}", FTB_RANKS_API_CLASS);
                    NeoEssentials.LOGGER.error("This could be due to a version mismatch - please ensure compatible versions");
                    NeoEssentials.LOGGER.debug("Full exception: ", e);
                } catch (NoClassDefFoundError e) {
                    NeoEssentials.LOGGER.error("FTB Ranks class loading error: {}", e.getMessage());
                    NeoEssentials.LOGGER.error("This is likely due to a dependency issue - check FTB Library version");
                    NeoEssentials.LOGGER.debug("Full exception: ", e);                }
            } else {
                NeoEssentials.LOGGER.debug("FTB Ranks mod not found, skipping permission handler");
            }
        } catch (NoClassDefFoundError e) {
            ftbRanksAvailable = false;
            NeoEssentials.LOGGER.error("FTB Ranks is missing a required dependency: " + e.getMessage());
            NeoEssentials.LOGGER.error("Please ensure FTB Library is installed on your server.");
        } catch (Exception e) {
            ftbRanksAvailable = false;
            NeoEssentials.LOGGER.warn("Error initializing FTB Ranks permission handler", e);
            NeoEssentials.LOGGER.debug("Exception details:", e);
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
                boolean result = (boolean) permValue.getClass().getMethod("getAsBoolean").invoke(permValue);
                if (result) {
                    NeoEssentials.LOGGER.debug("FTB Ranks granted permission '{}' to player {}", 
                            permission, player.getName().getString());
                }
                return result;
            }
            return false;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error checking FTB Ranks permission: {}", permission);
            NeoEssentials.LOGGER.debug("FTB Ranks permission check exception details:", e);
            ftbRanksAvailable = false; // Disable to prevent repeated errors
            NeoEssentials.LOGGER.error("FTB Ranks permission handler has been disabled due to errors");
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
