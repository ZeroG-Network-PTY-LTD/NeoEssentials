<<<<<<< HEAD
public class PermissionAdapter {
    
=======
package com.zerog.neoessentials.adapter;

import com.zerog.neoessentials.common.adapter.IPermissionAdapter;
import com.zerog.neoessentials.common.utils.CommonPermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NeoForge 1.21.1 implementation of the permission adapter
 * Handles permission checks using LuckPerms if available, or falls back to operator status
 */
public class PermissionAdapter implements IPermissionAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionAdapter.class);
    private static final ConcurrentHashMap<String, Boolean> PERMISSION_CACHE = new ConcurrentHashMap<>();
    
    private boolean luckPermsAvailable = false;
    private Object luckPermsApi = null;
    
    @Override
    public boolean hasPermission(Object playerRef, String permission) {
        if (playerRef == null || permission == null) return false;
        
        ServerPlayer player;
        if (playerRef instanceof ServerPlayer) {
            player = (ServerPlayer)playerRef;
        } else if (playerRef instanceof CommandSourceStack) {
            CommandSourceStack source = (CommandSourceStack)playerRef;
            if (source.getEntity() instanceof ServerPlayer) {
                player = (ServerPlayer)source.getEntity();
            } else {
                // Command block or console
                return true;
            }
        } else {
            return false;
        }
        
        String cacheKey = player.getUUID().toString() + ":" + permission;
        
        // Check cache first to avoid frequent API calls
        if (PERMISSION_CACHE.containsKey(cacheKey)) {
            return PERMISSION_CACHE.get(cacheKey);
        }
        
        boolean hasPermission = checkPermission(player, permission);
        PERMISSION_CACHE.put(cacheKey, hasPermission);
        
        return hasPermission;
    }
    
    @Override
    public boolean hasPermissionOrOp(Object playerRef, String permission) {
        if (isOp(playerRef)) return true;
        return hasPermission(playerRef, permission);
    }
    
    @Override
    public boolean isOp(Object playerRef) {
        if (playerRef == null) return false;
        
        if (playerRef instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)playerRef;
            return player.hasPermissions(4); // Op level 4 is highest
        } else if (playerRef instanceof CommandSourceStack) {
            CommandSourceStack source = (CommandSourceStack)playerRef;
            return source.hasPermission(4);
        }
        
        return false;
    }
    
    @Override
    public void initialize() {
        // Check if LuckPerms is installed
        luckPermsAvailable = ModList.get().isLoaded("luckperms");
        
        if (luckPermsAvailable) {
            try {
                // Try to get LuckPerms API using reflection
                Class<?> apiClass = Class.forName("net.luckperms.api.LuckPermsProvider");
                java.lang.reflect.Method getApi = apiClass.getMethod("get");
                luckPermsApi = getApi.invoke(null);
                
                LOGGER.info("LuckPerms detected, using LuckPerms for permission checks");
            } catch (Exception e) {
                LOGGER.error("Failed to initialize LuckPerms integration", e);
                luckPermsAvailable = false;
            }
        } else {
            LOGGER.info("LuckPerms not found, using operator status for permission checks");
        }
    }
    
    /**
     * Checks a permission using LuckPerms if available, or falls back to operator status
     */
    private boolean checkPermission(ServerPlayer player, String permission) {
        if (player == null) return false;
        
        // Always allow operators all permissions
        if (player.hasPermissions(4)) {
            return true;
        }
        
        // Use LuckPerms if available
        if (luckPermsAvailable && luckPermsApi != null) {
            try {
                // Get user from API
                java.lang.reflect.Method getUserMethod = luckPermsApi.getClass().getMethod("getPlayerAdapter", Class.class);
                Object playerAdapter = getUserMethod.invoke(luckPermsApi, Class.forName("net.minecraft.server.level.ServerPlayer"));
                
                java.lang.reflect.Method getUser = playerAdapter.getClass().getMethod("getUser", ServerPlayer.class);
                Object user = getUser.invoke(playerAdapter, player);
                
                // Check permission
                java.lang.reflect.Method getCachedData = user.getClass().getMethod("getCachedData");
                Object cachedData = getCachedData.invoke(user);
                
                java.lang.reflect.Method getPermissionData = cachedData.getClass().getMethod("getPermissionData");
                Object permissionData = getPermissionData.invoke(cachedData);
                
                java.lang.reflect.Method checkPermission = permissionData.getClass().getMethod("checkPermission", String.class);
                Object result = checkPermission.invoke(permissionData, permission);
                
                // Get the result value
                java.lang.reflect.Method asBoolean = result.getClass().getMethod("asBoolean");
                return (boolean)asBoolean.invoke(result);
            } catch (Exception e) {
                LOGGER.error("Error checking LuckPerms permission: {}", permission, e);
                return false;
            }
        }
        
        // Fall back to checking wildcard nodes for operators only
        // For custom permission support without LuckPerms, use the permission node format:
        // neoessentials.commands.<command>
        return permission.startsWith(CommonPermissionUtil.PERMISSION_PREFIX) && player.hasPermissions(2);
    }
>>>>>>> 81f44ad (feat: Enhance README with multi-version support details)
}
