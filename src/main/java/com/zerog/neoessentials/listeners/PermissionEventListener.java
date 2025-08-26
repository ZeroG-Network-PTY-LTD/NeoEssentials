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
import java.util.List;

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
            com.zerog.neoessentials.util.DebugUtil.infoLog("Loading permission data for player " + player.getName().getString());
            
            // Load player data
            PlayerDataManager playerDataManager = PlayerDataManager.getInstance();
            PlayerData playerData = playerDataManager.loadPlayerData(playerUUID);
            
            // Load permission data into the permission system
            CustomPermissionsManager permManager = CustomPermissionsManager.getInstance();
            
            // IMPORTANT: Check if player already has a group in the permission manager
            // Only set group if it's different from current
            String existingGroup = permManager.getPlayerGroup(playerUUID);
            String savedGroup = playerData.getPermissionGroup();
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
                playerData.setPermissionGroup(groupToUse);
                com.zerog.neoessentials.util.DebugUtil.infoLog("Set group '" + groupToUse + "' for player " + player.getName().getString());
            }
            Map<String, Boolean> savedPermissions = playerData.getPlayerPermissions();
            if (savedPermissions != null && !savedPermissions.isEmpty()) {
                permManager.setPlayerPermissionsFromMap(playerUUID, savedPermissions);
                com.zerog.neoessentials.util.DebugUtil.infoLog("Loaded " + savedPermissions.size() + " individual permissions for player " + player.getName().getString());
            }
            // --- New manager integration ---
            com.zerog.neoessentials.features.PlaceholderManager placeholderManager = new com.zerog.neoessentials.features.PlaceholderManager();
            com.zerog.neoessentials.features.TabListManager tabListManager = new com.zerog.neoessentials.features.TabListManager();
            com.zerog.neoessentials.features.ScoreboardManager scoreboardManager = new com.zerog.neoessentials.features.ScoreboardManager();
            com.zerog.neoessentials.features.BossBarManager bossBarManager = new com.zerog.neoessentials.features.BossBarManager();
            String rawDisplayName = com.zerog.neoessentials.features.NameFormatManager.getInstance().getDisplayName(player);
            String displayName = placeholderManager.parse(player, rawDisplayName);
            // Use parsed displayName in tablist and scoreboard updates
            tabListManager.updateHeaderFooter(player, displayName);
            tabListManager.updatePlayerEntry(player);
            if (player.getServer() != null) {
                scoreboardManager.updateScoreboard(player);
            }
            bossBarManager.showBossBar(player, displayName, 1.0f, 0x00FF00);

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
            PlayerData playerData = playerDataManager.getPlayerData(playerUUID);
            
            // Save the player's current group
            String currentGroup = permManager.getPlayerGroup(playerUUID);
            if (currentGroup != null) {
                playerData.setPermissionGroup(currentGroup);
                com.zerog.neoessentials.util.DebugUtil.debugLog("Saved group '" + currentGroup + "' for player " + player.getName().getString());
            }
            // Save individual permissions
            Map<String, Boolean> currentPermissions = permManager.getPlayerPermissionsMap(playerUUID);
            if (currentPermissions != null && !currentPermissions.isEmpty()) {
                playerData.setPlayerPermissions(currentPermissions);
                com.zerog.neoessentials.util.DebugUtil.debugLog("Saved " + currentPermissions.size() + " individual permissions for player " + player.getName().getString());
            }
            // Trigger save to persistent storage
            playerDataManager.savePlayerData(playerData);
            
            com.zerog.neoessentials.util.DebugUtil.infoLog("Successfully saved permission data for player " + player.getName().getString());
            // --- Remove bossbar on leave ---
            com.zerog.neoessentials.features.BossBarManager bossBarManager = new com.zerog.neoessentials.features.BossBarManager();
            bossBarManager.removeBossBar(player);
        } catch (Exception e) {
            com.zerog.neoessentials.util.DebugUtil.errorLog("Failed to save permission data for player " + event.getEntity().getName().getString() + ": " + e.getMessage());
        }
    }
}
