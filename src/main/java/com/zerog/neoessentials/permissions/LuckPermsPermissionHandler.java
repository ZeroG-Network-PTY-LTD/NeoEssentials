package com.zerog.neoessentials.permissions;

import com.zerog.neoessentials.NeoEssentials;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handler for LuckPerms permission integration.
 * This class uses direct API calls when LuckPerms is available.
 */
public class LuckPermsPermissionHandler implements PermissionHandler {

    private boolean luckPermsAvailable = false;
    private LuckPerms api = null;
    
    public LuckPermsPermissionHandler() {
        try {
            // Check if LuckPerms API is available
            Class.forName("net.luckperms.api.LuckPermsProvider");
            api = LuckPermsProvider.get();
            luckPermsAvailable = true;
            NeoEssentials.LOGGER.info("LuckPerms detected - using for permission checks");
        } catch (ClassNotFoundException e) {
            luckPermsAvailable = false;
            NeoEssentials.LOGGER.debug("LuckPerms not found, skipping permission handler");
        } catch (Exception e) {
            luckPermsAvailable = false;
            NeoEssentials.LOGGER.warn("Error initializing LuckPerms permission handler", e);
        }
    }
    
    @Override
    public boolean hasPermission(ServerPlayer player, String permission) {
        if (!luckPermsAvailable || player == null || api == null) {
            return false;
        }
        
        try {
            // Get the LuckPerms user
            User user = api.getUserManager().getUser(player.getUUID());
            if (user == null) {
                return false;
            }
            
            // Check permission
            return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error checking LuckPerms permission: {}", permission, e);
            return false;
        }
    }
    
    @Override
    public boolean isAvailable() {
        return luckPermsAvailable;
    }
    
    @Override
    public String getName() {
        return "LuckPerms";
    }
}
