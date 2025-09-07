package com.zerog.neoessentials.listeners;

import com.zerog.neoessentials.permissions.CustomPermissionsManager;
import com.zerog.neoessentials.storage.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.List;

/**
 * Event listener for permission system integration with player data
 * Handles loading/saving permission data when players join/leave
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class PermissionEventListener {
    
    /**
     * Handle player join - load their permission data from storage
     */
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        try {
            UUID playerUUID = player.getUUID();
            com.zerog.neoessentials.util.DebugUtil.infoLog("Loading player data for player " + player.getName().getString());
            
            // Load player data from PlayerDataManager
            PlayerDataManager playerDataManager = PlayerDataManager.getInstance();
            
            // Load permission data into the permission system
            CustomPermissionsManager permManager = CustomPermissionsManager.getInstance();
            
                        // IMPORTANT: Check if player already has a group in the permission manager
            // Only set group if it's different from current
            String existingGroup = permManager.getPlayerGroup(playerUUID);
            
            // Get permission data from PlayerDataManager settings
            String savedGroup = playerDataManager.getSettingString(playerUUID, "permissionGroup", "default");
            String permissionsString = playerDataManager.getSettingString(playerUUID, "playerPermissions", "{}");
            
            // Parse permissions from JSON-like string format
            java.util.Map<String, Boolean> savedPermissions = new java.util.HashMap<>();
            if (permissionsString != null && !permissionsString.equals("{}") && !permissionsString.isEmpty()) {
                try {
                    // Simple parsing for key=value,key2=value2 format
                    if (permissionsString.startsWith("{") && permissionsString.endsWith("}")) {
                        permissionsString = permissionsString.substring(1, permissionsString.length() - 1);
                        if (!permissionsString.trim().isEmpty()) {
                            String[] pairs = permissionsString.split(",");
                            for (String pair : pairs) {
                                String[] kv = pair.split("=", 2);
                                if (kv.length == 2) {
                                    String key = kv[0].trim();
                                    boolean value = Boolean.parseBoolean(kv[1].trim());
                                    savedPermissions.put(key, value);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    com.zerog.neoessentials.util.DebugUtil.warnLog("Failed to parse player permissions for " + player.getName().getString() + ": " + e.getMessage());
                }
            }
            
            String groupToUse = null;
            if (existingGroup != null && !existingGroup.equals("default") && !existingGroup.isEmpty()) {
                groupToUse = existingGroup;
                com.zerog.neoessentials.util.DebugUtil.infoLog("Player " + player.getName().getString() + " already has group '" + existingGroup + "' in permission manager");
            } else if (savedGroup != null && !savedGroup.isEmpty()) {
                groupToUse = savedGroup;
                com.zerog.neoessentials.util.DebugUtil.infoLog("Loading saved group '" + savedGroup + "' for player " + player.getName().getString());
            } else {
                groupToUse = "default";
                com.zerog.neoessentials.util.DebugUtil.infoLog("Setting default group for new player " + player.getName().getString());
            }
            if (!groupToUse.equals(existingGroup)) {
                permManager.setPlayerGroup(playerUUID, groupToUse);
                // Save back to PlayerDataManager settings
                playerDataManager.setSetting(playerUUID, "permissionGroup", groupToUse);
                com.zerog.neoessentials.util.DebugUtil.infoLog("Set group '" + groupToUse + "' for player " + player.getName().getString());
            }
            
            if (!savedPermissions.isEmpty()) {
                permManager.setPlayerPermissionsFromMap(playerUUID, savedPermissions);
                com.zerog.neoessentials.util.DebugUtil.infoLog("Loaded " + savedPermissions.size() + " individual permissions for player " + player.getName().getString());
            }
            
            // Update tablist for all online players to ensure correct layout and debug output
            try {
                net.minecraft.server.MinecraftServer server = player.getServer();
                if (server != null) {
                    List<ServerPlayer> onlinePlayers = server.getPlayerList().getPlayers();
                    com.zerog.neoessentials.util.DebugUtil.debugLog("Calling updateTabList for " + onlinePlayers.size() + " online players");
                    for (ServerPlayer p : onlinePlayers) {
                        com.zerog.neoessentials.util.DebugUtil.debugLog("Online player: " + p.getGameProfile().getName() + " UUID: " + p.getUUID());
                    }
                    com.zerog.neoessentials.features.TabListManager.getInstance().updateTabList(onlinePlayers);
                    com.zerog.neoessentials.util.DebugUtil.debugLog("updateTabList call completed");
                }
            } catch (Exception e) {
                com.zerog.neoessentials.util.DebugUtil.warnLog("Failed to update tablist for all players: " + e.getMessage());
            }
        } catch (Exception e) {
            com.zerog.neoessentials.util.DebugUtil.errorLog("Failed to load permission data for player " + event.getEntity().getName().getString() + ": " + e.getMessage());
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
            com.zerog.neoessentials.util.DebugUtil.infoLog("Saving permission data for player " + player.getName().getString());
            
            // Get current permission data
            CustomPermissionsManager permManager = CustomPermissionsManager.getInstance();
            PlayerDataManager playerDataManager = PlayerDataManager.getInstance();
            
            // Save the player's current group
            String currentGroup = permManager.getPlayerGroup(playerUUID);
            if (currentGroup != null) {
                playerDataManager.setSetting(playerUUID, "permissionGroup", currentGroup);
                com.zerog.neoessentials.util.DebugUtil.debugLog("Saved group '" + currentGroup + "' for player " + player.getName().getString());
            }
            
            // Save individual permissions as a formatted string
            Map<String, Boolean> currentPermissions = permManager.getPlayerPermissionsMap(playerUUID);
            if (currentPermissions != null && !currentPermissions.isEmpty()) {
                // Convert to simple string format: {key1=value1,key2=value2}
                StringBuilder sb = new StringBuilder("{");
                boolean first = true;
                for (Map.Entry<String, Boolean> entry : currentPermissions.entrySet()) {
                    if (!first) sb.append(",");
                    sb.append(entry.getKey()).append("=").append(entry.getValue());
                    first = false;
                }
                sb.append("}");
                playerDataManager.setSetting(playerUUID, "playerPermissions", sb.toString());
                com.zerog.neoessentials.util.DebugUtil.debugLog("Saved " + currentPermissions.size() + " individual permissions for player " + player.getName().getString());
            } else {
                // Clear permissions if none exist
                playerDataManager.setSetting(playerUUID, "playerPermissions", "{}");
            }
            
            // Trigger save to persistent storage
            playerDataManager.savePlayerData(player.getUUID());
            
            com.zerog.neoessentials.util.DebugUtil.infoLog("Successfully saved permission data for player " + player.getName().getString());
            // Bossbar system removed - no cleanup needed on player leave
        } catch (Exception e) {
            com.zerog.neoessentials.util.DebugUtil.errorLog("Failed to save permission data for player " + event.getEntity().getName().getString() + ": " + e.getMessage());
        }
    }
}
