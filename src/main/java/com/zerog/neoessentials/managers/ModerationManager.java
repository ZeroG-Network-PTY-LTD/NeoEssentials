package com.zerog.neoessentials.managers;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.config.ModerationConfig;
import com.zerog.neoessentials.storage.PlayerDataManager;
import com.zerog.neoessentials.util.LocationUtil;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Moderation management system for NeoEssentials
 * Handles bans, mutes, kicks, jails, and other moderation actions
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class ModerationManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ModerationManager.class);
    private static ModerationManager instance;
    
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final Map<UUID, MuteData> activeMutes;
    private final Map<UUID, JailData> activeJails;
    
    private ModerationManager() {
        this.configManager = ConfigManager.getInstance();
        this.playerDataManager = PlayerDataManager.getInstance();
        this.activeMutes = new ConcurrentHashMap<>();
        this.activeJails = new ConcurrentHashMap<>();
    }
    
    public static ModerationManager getInstance() {
        if (instance == null) {
            instance = new ModerationManager();
        }
        return instance;
    }
    
    /**
     * Kick a player from the server
     */
    public boolean kickPlayer(ServerPlayer target, ServerPlayer moderator, String reason) {
        ModerationConfig config = configManager.getModerationConfig();
        
        if (!config.enabled) {
            MessageUtil.sendMessage(moderator, "&cModeration system is disabled.");
            return false;
        }
        
        // Check permission
        if (!PermissionUtil.hasPermission(moderator, "essentials.kick")) {
            MessageUtil.sendMessage(moderator, config.messages.noPermission);
            return false;
        }
        
        // Check if target is exempt
        if (PermissionUtil.hasPermission(target, "essentials.kick.exempt")) {
            MessageUtil.sendMessage(moderator, config.messages.playerExempt);
            return false;
        }
        
        String finalReason = reason != null && !reason.isEmpty() ? reason : config.kick.defaultKickReason;
        
        // Kick the player
        target.connection.disconnect(Component.literal(config.messages.youAreKicked
            .replace("{REASON}", finalReason)
            .replace("{ADMIN}", moderator.getName().getString())));
        
        // Log action
        logModerationAction("KICK", moderator, target, finalReason, 0);
        
        // Broadcast if enabled
        if (config.broadcastActions) {
            broadcastAction(config.messages.playerKicked
                .replace("{PLAYER}", target.getName().getString())
                .replace("{ADMIN}", moderator.getName().getString())
                .replace("{REASON}", finalReason));
        }
        
        MessageUtil.sendMessage(moderator, "&aSuccessfully kicked " + target.getName().getString() + " for: " + finalReason);
        
        LOGGER.info("Player {} kicked by {} for: {}", 
            target.getName().getString(), moderator.getName().getString(), finalReason);
        
        return true;
    }
    
    /**
     * Mute a player
     */
    public boolean mutePlayer(UUID targetUuid, String targetName, ServerPlayer moderator, String reason, long duration) {
        ModerationConfig config = configManager.getModerationConfig();
        
        if (!config.enabled) {
            MessageUtil.sendMessage(moderator, "&cModeration system is disabled.");
            return false;
        }
        
        // Check permission
        if (!PermissionUtil.hasPermission(moderator, "essentials.mute")) {
            MessageUtil.sendMessage(moderator, config.messages.noPermission);
            return false;
        }
        
        // Check if already muted
        if (isPlayerMuted(targetUuid)) {
            MessageUtil.sendMessage(moderator, "&c" + targetName + " is already muted!");
            return false;
        }
        
        String finalReason = reason != null && !reason.isEmpty() ? reason : config.mute.defaultMuteReason;
        long expiry = duration > 0 ? System.currentTimeMillis() + (duration * 1000) : 0;
        
        MuteData muteData = new MuteData(targetUuid, targetName, moderator.getName().getString(), 
            finalReason, System.currentTimeMillis(), expiry);
        
        activeMutes.put(targetUuid, muteData);
        
        // Save to player data
        Map<String, Object> muteInfo = Map.of(
            "reason", finalReason,
            "moderator", moderator.getName().getString(),
            "timestamp", System.currentTimeMillis(),
            "expiry", expiry
        );
        playerDataManager.setSetting(targetUuid, "mute_data", muteInfo);
        
        // Notify target if online
        ServerPlayer target = getPlayerByUuid(targetUuid);
        if (target != null) {
            String muteMessage = duration > 0 ? 
                config.messages.playerTempMuted
                    .replace("{DURATION}", MessageUtil.formatTime(duration * 1000))
                    .replace("{ADMIN}", moderator.getName().getString())
                    .replace("{REASON}", finalReason) :
                config.messages.youAreMuted
                    .replace("{REASON}", finalReason);
            
            MessageUtil.sendMessage(target, muteMessage);
        }
        
        // Log action
        logModerationAction("MUTE", moderator, target, finalReason, duration);
        
        // Broadcast if enabled
        if (config.broadcastActions) {
            String broadcastMessage = duration > 0 ?
                config.messages.playerTempMuted
                    .replace("{PLAYER}", targetName)
                    .replace("{ADMIN}", moderator.getName().getString())
                    .replace("{DURATION}", MessageUtil.formatTime(duration * 1000))
                    .replace("{REASON}", finalReason) :
                config.messages.playerMuted
                    .replace("{PLAYER}", targetName)
                    .replace("{ADMIN}", moderator.getName().getString())
                    .replace("{REASON}", finalReason);
            
            broadcastAction(broadcastMessage);
        }
        
        String successMessage = duration > 0 ?
            "&aSuccessfully muted " + targetName + " for " + MessageUtil.formatTime(duration * 1000) + ": " + finalReason :
            "&aSuccessfully muted " + targetName + ": " + finalReason;
        
        MessageUtil.sendMessage(moderator, successMessage);
        
        LOGGER.info("Player {} muted by {} for {} seconds: {}", 
            targetName, moderator.getName().getString(), duration, finalReason);
        
        return true;
    }
    
    /**
     * Unmute a player
     */
    public boolean unmutePlayer(UUID targetUuid, String targetName, ServerPlayer moderator) {
        ModerationConfig config = configManager.getModerationConfig();
        
        if (!PermissionUtil.hasPermission(moderator, "essentials.unmute")) {
            MessageUtil.sendMessage(moderator, config.messages.noPermission);
            return false;
        }
        
        if (!isPlayerMuted(targetUuid)) {
            MessageUtil.sendMessage(moderator, "&c" + targetName + " is not muted!");
            return false;
        }
        
        activeMutes.remove(targetUuid);
        playerDataManager.setSetting(targetUuid, "mute_data", null);
        
        // Notify target if online
        ServerPlayer target = getPlayerByUuid(targetUuid);
        if (target != null) {
            MessageUtil.sendMessage(target, config.messages.muteExpired);
        }
        
        MessageUtil.sendMessage(moderator, "&aSuccessfully unmuted " + targetName);
        
        LOGGER.info("Player {} unmuted by {}", targetName, moderator.getName().getString());
        
        return true;
    }
    
    /**
     * Jail a player
     */
    public boolean jailPlayer(UUID targetUuid, String targetName, ServerPlayer moderator, String jailName, String reason, long duration) {
        ModerationConfig config = configManager.getModerationConfig();
        
        if (!config.enabled || !config.jail.enabled) {
            MessageUtil.sendMessage(moderator, "&cJail system is disabled.");
            return false;
        }
        
        // Check permission
        if (!PermissionUtil.hasPermission(moderator, "essentials.jail")) {
            MessageUtil.sendMessage(moderator, config.messages.noPermission);
            return false;
        }
        
        // Check if already jailed
        if (isPlayerJailed(targetUuid)) {
            MessageUtil.sendMessage(moderator, "&c" + targetName + " is already jailed!");
            return false;
        }
        
        // Get jail location
        LocationUtil.Location jailLocation = getJailLocation(jailName);
        if (jailLocation == null) {
            MessageUtil.sendMessage(moderator, "&cJail '" + jailName + "' not found!");
            return false;
        }
        
        ServerPlayer target = getPlayerByUuid(targetUuid);
        if (target == null) {
            MessageUtil.sendMessage(moderator, config.messages.playerNotFound);
            return false;
        }
        
        String finalReason = reason != null && !reason.isEmpty() ? reason : "Jailed by administrator";
        long expiry = duration > 0 ? System.currentTimeMillis() + (duration * 1000) : 0;
        
        // Store previous location
        LocationUtil.Location previousLocation = new LocationUtil.Location(
            target.serverLevel().dimension().location().toString(),
            target.getX(), target.getY(), target.getZ(),
            target.getYRot(), target.getXRot()
        );
        
        JailData jailData = new JailData(targetUuid, targetName, moderator.getName().getString(),
            finalReason, System.currentTimeMillis(), expiry, jailName, jailLocation, previousLocation);
        
        activeJails.put(targetUuid, jailData);
        
        // Save to player data
        Map<String, Object> jailInfo = Map.of(
            "reason", finalReason,
            "moderator", moderator.getName().getString(),
            "timestamp", System.currentTimeMillis(),
            "expiry", expiry,
            "jailName", jailName,
            "previousLocation", Map.of(
                "world", previousLocation.world,
                "x", previousLocation.x,
                "y", previousLocation.y,
                "z", previousLocation.z,
                "yaw", previousLocation.yaw,
                "pitch", previousLocation.pitch
            )
        );
        playerDataManager.setSetting(targetUuid, "jail_data", jailInfo);
        
        // Teleport player to jail
        teleportToLocation(target, jailLocation);
        
        // Notify target
        String jailMessage = duration > 0 ?
            config.messages.playerJailed
                .replace("{REASON}", finalReason)
                .replace("{DURATION}", MessageUtil.formatTime(duration * 1000))
                .replace("{ADMIN}", moderator.getName().getString()) :
            config.messages.youAreJailed
                .replace("{TIME}", "indefinite");
        
        MessageUtil.sendMessage(target, jailMessage);
        
        // Log action
        logModerationAction("JAIL", moderator, target, finalReason + " (Jail: " + jailName + ")", duration);
        
        String successMessage = duration > 0 ?
            "&aSuccessfully jailed " + targetName + " for " + MessageUtil.formatTime(duration * 1000) + ": " + finalReason :
            "&aSuccessfully jailed " + targetName + ": " + finalReason;
        
        MessageUtil.sendMessage(moderator, successMessage);
        
        LOGGER.info("Player {} jailed by {} in {} for {} seconds: {}", 
            targetName, moderator.getName().getString(), jailName, duration, finalReason);
        
        return true;
    }
    
    /**
     * Unjail a player
     */
    public boolean unjailPlayer(UUID targetUuid, String targetName, ServerPlayer moderator) {
        ModerationConfig config = configManager.getModerationConfig();
        
        if (!PermissionUtil.hasPermission(moderator, "essentials.unjail")) {
            MessageUtil.sendMessage(moderator, config.messages.noPermission);
            return false;
        }
        
        JailData jailData = activeJails.get(targetUuid);
        if (jailData == null) {
            MessageUtil.sendMessage(moderator, "&c" + targetName + " is not jailed!");
            return false;
        }
        
        ServerPlayer target = getPlayerByUuid(targetUuid);
        if (target != null && jailData.previousLocation != null) {
            // Teleport back to previous location
            teleportToLocation(target, jailData.previousLocation);
            
            MessageUtil.sendMessage(target, config.messages.jailReleased);
        }
        
        activeJails.remove(targetUuid);
        playerDataManager.setSetting(targetUuid, "jail_data", null);
        
        MessageUtil.sendMessage(moderator, "&aSuccessfully unjailed " + targetName);
        
        LOGGER.info("Player {} unjailed by {}", targetName, moderator.getName().getString());
        
        return true;
    }
    
    /**
     * Check if a player is muted
     */
    public boolean isPlayerMuted(UUID playerUuid) {
        MuteData muteData = activeMutes.get(playerUuid);
        if (muteData == null) {
            return false;
        }
        
        // Check if mute expired
        if (muteData.expiry > 0 && System.currentTimeMillis() > muteData.expiry) {
            activeMutes.remove(playerUuid);
            playerDataManager.setSetting(playerUuid, "mute_data", null);
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if a player is jailed
     */
    public boolean isPlayerJailed(UUID playerUuid) {
        JailData jailData = activeJails.get(playerUuid);
        if (jailData == null) {
            return false;
        }
        
        // Check if jail expired
        if (jailData.expiry > 0 && System.currentTimeMillis() > jailData.expiry) {
            // Auto-unjail
            unjailPlayerSilent(playerUuid);
            return false;
        }
        
        return true;
    }
    
    /**
     * Prevent jailed player from moving outside jail area
     */
    public boolean canPlayerMove(ServerPlayer player, LocationUtil.Location newLocation) {
        if (!isPlayerJailed(player.getUUID())) {
            return true;
        }
        
        JailData jailData = activeJails.get(player.getUUID());
        if (jailData == null || jailData.jailLocation == null) {
            return true;
        }
        
        double distance = jailData.jailLocation.distance(newLocation);
        
        if (distance > 10) { // 10 block jail radius
            MessageUtil.sendMessage(player, "&cYou cannot escape from jail!");
            // Teleport back to jail
            teleportToLocation(player, jailData.jailLocation);
            return false;
        }
        
        return true;
    }
    
    /**
     * Get jail location
     */
    private LocationUtil.Location getJailLocation(String jailName) {
        ModerationConfig config = configManager.getModerationConfig();
        // Return the default jail location
        return new LocationUtil.Location(
            config.jail.jailWorld, 
            config.jail.jailX, 
            config.jail.jailY, 
            config.jail.jailZ, 
            0, 0
        );
    }
    
    /**
     * Teleport player to location
     */
    private void teleportToLocation(ServerPlayer player, LocationUtil.Location location) {
        // Basic teleportation - would need proper implementation
        player.teleportTo(location.x, location.y, location.z);
    }
    
    /**
     * Get player by UUID
     */
    private ServerPlayer getPlayerByUuid(UUID uuid) {
        // This would need access to the server instance
        return null; // Placeholder
    }
    
    /**
     * Unjail player silently (for auto-unjail)
     */
    private void unjailPlayerSilent(UUID playerUuid) {
        JailData jailData = activeJails.remove(playerUuid);
        if (jailData != null) {
            playerDataManager.setSetting(playerUuid, "jail_data", null);
            
            ServerPlayer target = getPlayerByUuid(playerUuid);
            if (target != null && jailData.previousLocation != null) {
                teleportToLocation(target, jailData.previousLocation);
                MessageUtil.sendMessage(target, "&aYour jail time has expired!");
            }
        }
    }
    
    /**
     * Log moderation action
     */
    private void logModerationAction(String action, ServerPlayer moderator, ServerPlayer target, String reason, long duration) {
        String targetName = target != null ? target.getName().getString() : "Unknown";
        LOGGER.info("MODERATION: {} - {} -> {} for {} seconds: {}", 
            action, moderator.getName().getString(), targetName, duration, reason);
    }
    
    /**
     * Broadcast action to players with permission
     */
    private void broadcastAction(String message) {
        // This would need proper server access to broadcast
        LOGGER.info("BROADCAST: {}", message);
    }
    
    /**
     * Clean up expired mutes and jails
     */
    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        
        // Clean up expired mutes
        activeMutes.entrySet().removeIf(entry -> {
            MuteData muteData = entry.getValue();
            if (muteData.expiry > 0 && currentTime > muteData.expiry) {
                playerDataManager.setSetting(entry.getKey(), "mute_data", null);
                return true;
            }
            return false;
        });
        
        // Clean up expired jails
        activeJails.entrySet().removeIf(entry -> {
            JailData jailData = entry.getValue();
            if (jailData.expiry > 0 && currentTime > jailData.expiry) {
                unjailPlayerSilent(entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * Load persistent data on startup
     */
    public void loadData() {
        // Load mutes and jails from player data
        // This would iterate through all player data files
        LOGGER.info("Loading moderation data...");
    }
    
    // Data classes
    public static class MuteData {
        public final UUID playerUuid;
        public final String playerName;
        public final String moderator;
        public final String reason;
        public final long timestamp;
        public final long expiry;
        
        public MuteData(UUID playerUuid, String playerName, String moderator, String reason, long timestamp, long expiry) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.moderator = moderator;
            this.reason = reason;
            this.timestamp = timestamp;
            this.expiry = expiry;
        }
    }
    
    public static class JailData {
        public final UUID playerUuid;
        public final String playerName;
        public final String moderator;
        public final String reason;
        public final long timestamp;
        public final long expiry;
        public final String jailName;
        public final LocationUtil.Location jailLocation;
        public final LocationUtil.Location previousLocation;
        
        public JailData(UUID playerUuid, String playerName, String moderator, String reason, 
                       long timestamp, long expiry, String jailName, 
                       LocationUtil.Location jailLocation, LocationUtil.Location previousLocation) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.moderator = moderator;
            this.reason = reason;
            this.timestamp = timestamp;
            this.expiry = expiry;
            this.jailName = jailName;
            this.jailLocation = jailLocation;
            this.previousLocation = previousLocation;
        }
    }
}
