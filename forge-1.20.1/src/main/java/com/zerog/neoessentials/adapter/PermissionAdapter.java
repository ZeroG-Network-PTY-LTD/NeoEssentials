package com.zerog.neoessentials.adapter;

import com.zerog.neoessentials.common.adapter.IPermissionAdapter;
import com.zerog.neoessentials.common.utils.CommonPermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge 1.20.1 implementation of the permission adapter
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
     * 
     * @param player The player to check
     * @param permission The permission node
     * @return True if the player has the permission
     */
    private boolean checkPermission(ServerPlayer player, String permission) {
        if (luckPermsAvailable && luckPermsApi != null) {
            try {
                // Use reflection to call LuckPerms API
                return CommonPermissionUtil.checkLuckPerms(luckPermsApi, player.getUUID(), permission);
            } catch (Exception e) {
                LOGGER.error("Failed to check permission using LuckPerms", e);
            }
        }
        
        // Fallback to OP check if LuckPerms is not available or fails
        return player.hasPermissions(2); // Op level 2 is enough for most commands
    }
}
