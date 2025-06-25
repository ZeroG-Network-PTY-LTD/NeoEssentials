package com.zerog.neoessentials.permissions;

import com.zerog.neoessentials.NeoEssentials;
import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handler for FTB Ranks permission integration.
 * This class uses direct API calls when FTB Ranks is available.
 */
public class FTBRanksPermissionHandler implements PermissionHandler {

    private boolean ftbRanksAvailable = false;
    
    public FTBRanksPermissionHandler() {
        try {
            // Check if FTB Ranks API is available
            Class.forName("dev.ftb.mods.ftbranks.api.FTBRanksAPI");
            ftbRanksAvailable = true;
            NeoEssentials.LOGGER.info("FTB Ranks detected - using for permission checks");
        } catch (ClassNotFoundException e) {
            ftbRanksAvailable = false;
            NeoEssentials.LOGGER.debug("FTB Ranks not found, skipping permission handler");
        } catch (Exception e) {
            ftbRanksAvailable = false;
            NeoEssentials.LOGGER.warn("Error initializing FTB Ranks permission handler", e);
        }
    }
    
    @Override
    public boolean hasPermission(ServerPlayer player, String permission) {
        if (!ftbRanksAvailable || player == null) {
            return false;
        }
        
        try {
            return FTBRanksAPI.getInstance().getPermissionValue(player, permission).getAsBoolean();
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
