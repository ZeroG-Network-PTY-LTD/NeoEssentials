package com.zerog.neoessentials.listeners;

import com.zerog.neoessentials.permissions.CustomPermissionsManager;
import com.zerog.neoessentials.player.PlayerData;
import com.zerog.neoessentials.player.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

/**
 * Event listener for permission system integration with player data
 * Handles loading/saving permission data when players join/leave
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class PermissionEventListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionEventListener.class);
    
    /**
     * Handle player join - load their permission data from storage
     */
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        try {
            UUID playerUUID = player.getUUID();
            LOGGER.info("Loading permission data for player {}", player.getName().getString());
            
            // Load player data
            PlayerDataManager playerDataManager = PlayerDataManager.getInstance();
            PlayerData playerData = playerDataManager.loadPlayerData(playerUUID);
            
            // Load permission data into the permission system
            CustomPermissionsManager permManager = CustomPermissionsManager.getInstance();
            
            // IMPORTANT: Check if player already has a group in the permission manager
            String existingGroup = permManager.getPlayerGroup(playerUUID);
            String savedGroup = playerData.getPermissionGroup();
            
            // Use existing group if available, otherwise use saved group
            String groupToUse = null;
            if (existingGroup != null && !existingGroup.equals("default") && !existingGroup.isEmpty()) {
                groupToUse = existingGroup;
                LOGGER.info("Player {} already has group '{}' in permission manager", player.getName().getString(), existingGroup);
            } else if (savedGroup != null && !savedGroup.isEmpty()) {
                groupToUse = savedGroup;
                LOGGER.info("Loading saved group '{}' for player {}", savedGroup, player.getName().getString());
            } else {
                groupToUse = "default";
                LOGGER.info("Setting default group for new player {}", player.getName().getString());
            }
            
            // Only set group if it's different from current
            if (!groupToUse.equals(existingGroup)) {
                permManager.setPlayerGroup(playerUUID, groupToUse);
                playerData.setPermissionGroup(groupToUse);
                LOGGER.info("Set group '{}' for player {}", groupToUse, player.getName().getString());
            }
            
            // Load individual permissions
            Map<String, Boolean> savedPermissions = playerData.getPlayerPermissions();
            if (savedPermissions != null && !savedPermissions.isEmpty()) {
                permManager.setPlayerPermissionsFromMap(playerUUID, savedPermissions);
                LOGGER.info("Loaded {} individual permissions for player {}", 
                    savedPermissions.size(), player.getName().getString());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to load permission data for player {}", 
                event.getEntity().getName().getString(), e);
        }
    }
    
    /**
     * Handle player leave - save their permission data to storage
     */
    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        try {
            UUID playerUUID = player.getUUID();
            LOGGER.info("Saving permission data for player {}", player.getName().getString());
            
            // Get current permission data
            CustomPermissionsManager permManager = CustomPermissionsManager.getInstance();
            PlayerDataManager playerDataManager = PlayerDataManager.getInstance();
            PlayerData playerData = playerDataManager.getPlayerData(playerUUID);
            
            // Save the player's current group
            String currentGroup = permManager.getPlayerGroup(playerUUID);
            if (currentGroup != null) {
                playerData.setPermissionGroup(currentGroup);
                LOGGER.debug("Saved group '{}' for player {}", currentGroup, player.getName().getString());
            }
            
            // Save individual permissions
            Map<String, Boolean> currentPermissions = permManager.getPlayerPermissionsMap(playerUUID);
            if (currentPermissions != null && !currentPermissions.isEmpty()) {
                playerData.setPlayerPermissions(currentPermissions);
                LOGGER.debug("Saved {} individual permissions for player {}", 
                    currentPermissions.size(), player.getName().getString());
            }
            
            // Trigger save to persistent storage
            playerDataManager.savePlayerData(playerData);
            
            LOGGER.info("Successfully saved permission data for player {}", player.getName().getString());
            
        } catch (Exception e) {
            LOGGER.error("Failed to save permission data for player {}", 
                event.getEntity().getName().getString(), e);
        }
    }
}
