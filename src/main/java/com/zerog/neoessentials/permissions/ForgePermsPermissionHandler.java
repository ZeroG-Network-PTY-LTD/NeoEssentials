package com.zerog.neoessentials.permissions;

import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;

/**
 * Permission handler that integrates with ForgePerms using reflection.
 * This handler will detect and use ForgePerms if available without requiring a hard dependency.
 * 
 * Based on the ForgePerms API documentation.
 */
public class ForgePermsPermissionHandler implements PermissionHandler {

    private boolean forgePermsAvailable = false;
    private Class<?> forgePermsClass = null;
    private Class<?> permissionsBaseClass = null;
    private Method getPermissionHandlerMethod = null;
    private Method canAccessMethod = null;
    private Object permissionHandler = null;

    public ForgePermsPermissionHandler() {
        try {
            // Try to find ForgePerms classes using reflection
            forgePermsClass = Class.forName("com.sperion.forgeperms.ForgePerms");
            permissionsBaseClass = Class.forName("com.sperion.forgeperms.PermissionsBase");
            
            // Get the methods needed
            getPermissionHandlerMethod = forgePermsClass.getMethod("getPermissionHandler");
            canAccessMethod = permissionsBaseClass.getMethod("canAccess", String.class, String.class, String.class);
            
            // Get the actual permission handler instance
            permissionHandler = getPermissionHandlerMethod.invoke(null);
            
            if (permissionHandler != null) {
                forgePermsAvailable = true;
                NeoEssentials.LOGGER.info("ForgePerms detected - using for permission checks");
            } else {
                NeoEssentials.LOGGER.debug("ForgePerms detected but handler is null, skipping");
            }
        } catch (ClassNotFoundException e) {
            forgePermsAvailable = false;
            NeoEssentials.LOGGER.debug("ForgePerms not found, skipping permission handler");
        } catch (Exception e) {
            forgePermsAvailable = false;
            NeoEssentials.LOGGER.warn("Error initializing ForgePerms permission handler", e);
        }
    }
    
    @Override
    public boolean hasPermission(ServerPlayer player, String permission) {
        if (!forgePermsAvailable || player == null || permissionHandler == null) {
            return false;
        }
        
        try {
            // Get player name and world name
            String username = player.getName().getString();
            String world = player.level().dimension().location().toString();
            
            // Use reflection to call the canAccess method
            Object result = canAccessMethod.invoke(permissionHandler, username, world, permission);
            
            // Check if the result is a Boolean and return its value
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            
            return false;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error checking ForgePerms permission: {}", permission, e);
            return false;
        }
    }
    
    @Override
    public boolean isAvailable() {
        return forgePermsAvailable;
    }
    
    @Override
    public String getName() {
        return "ForgePerms";
    }
}
