package com.zerog.neoessentials.managers;

import com.zerog.neoessentials.config.ConfigurationUnifier;
import com.zerog.neoessentials.permissions.PermissionNodes;
import com.zerog.neoessentials.storage.PlayerDataManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.util.LocationUtil;
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
    
    private final ConfigurationUnifier configUnifier;
    private final PlayerDataManager playerDataManager;
    private final Map<UUID, MuteData> activeMutes;
    private final Map<UUID, JailData> activeJails;
    
    private ModerationManager() {
        this.configUnifier = ConfigurationUnifier.getInstance();
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
        boolean moderationModuleEnabled = configUnifier.getConfigManager().getMainConfig().modules.moderation;
        if (!moderationModuleEnabled) {
            MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.disabled");
            return false;
        }

        // Check permission
        if (!PermissionUtil.hasPermission(moderator, PermissionNodes.KICK)) {
            MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.kick.no_permission");
            return false;
        }

        // Check if target is exempt
        if (PermissionUtil.hasPermission(target, PermissionNodes.KICK_EXEMPT)) {
            MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.kick.exempt");
            return false;
        }

        String finalReason = reason != null && !reason.isEmpty() ? reason : "Kicked by administrator";

        // Kick the player
        target.connection.disconnect(Component.translatable("neoessentials.moderation.kick.player", finalReason));

        // Log action
        logModerationAction("KICK", moderator, target, finalReason, 0);

        // Broadcast (always enabled for now)
        broadcastAction(Component.translatable("neoessentials.moderation.kick.broadcast", target.getName().getString(), moderator.getName().getString(), finalReason).getString());

        MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.kick.success", target.getName().getString(), finalReason);

        LOGGER.info("Player {} kicked by {} for: {}", 
            target.getName().getString(), moderator.getName().getString(), finalReason);

        return true;
    }
    
    /**
     * Mute a player
     */
    public boolean mutePlayer(UUID targetUuid, String targetName, ServerPlayer moderator, String reason, long duration) {
        boolean moderationModuleEnabled = configUnifier.getConfigManager().getMainConfig().modules.moderation;
        if (!moderationModuleEnabled) {
            MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.disabled");
            return false;
        }

        // Check permission
        if (!PermissionUtil.hasPermission(moderator, PermissionNodes.MUTE)) {
            MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.mute.no_permission");
            return false;
        }

        // Check if already muted
        if (isPlayerMuted(targetUuid)) {
            MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.mute.already_muted", targetName);
            return false;
        }

        String finalReason = reason != null && !reason.isEmpty() ? reason : "Muted by administrator";
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
            if (duration > 0) {
                MessageUtil.sendTranslatedMessage(target, "neoessentials.moderation.mute.player.temp", MessageUtil.formatTime(duration * 1000), finalReason);
            } else {
                MessageUtil.sendTranslatedMessage(target, "neoessentials.moderation.mute.player", finalReason);
            }
        }

        // Log action
        logModerationAction("MUTE", moderator, target, finalReason, duration);

        // Broadcast (always enabled for now)
        if (duration > 0) {
            broadcastAction(Component.translatable("neoessentials.moderation.mute.broadcast.temp", targetName, moderator.getName().getString(), MessageUtil.formatTime(duration * 1000), finalReason).getString());
        } else {
            broadcastAction(Component.translatable("neoessentials.moderation.mute.broadcast", targetName, moderator.getName().getString(), finalReason).getString());
        }

        if (duration > 0) {
            MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.mute.success.temp", targetName, MessageUtil.formatTime(duration * 1000), finalReason);
        } else {
            MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.mute.success", targetName, finalReason);
        }

        LOGGER.info("Player {} muted by {} for {} seconds: {}", 
            targetName, moderator.getName().getString(), duration, finalReason);

        return true;
    }
    
    /**
     * Unmute a player
     */
    public boolean unmutePlayer(UUID targetUuid, String targetName, ServerPlayer moderator) {
        
        if (!PermissionUtil.hasPermission(moderator, PermissionNodes.UNMUTE)) {
            MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.unmute.no_permission");
            return false;
        }

        if (!isPlayerMuted(targetUuid)) {
            MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.unmute.not_muted", targetName);
            return false;
        }

        activeMutes.remove(targetUuid);
        playerDataManager.setSetting(targetUuid, "mute_data", null);

        // Notify target if online
        ServerPlayer target = getPlayerByUuid(targetUuid);
        if (target != null) {
            MessageUtil.sendTranslatedMessage(target, "neoessentials.moderation.unmute.player");
        }

        MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.unmute.success", targetName);

        LOGGER.info("Player {} unmuted by {}", targetName, moderator.getName().getString());

        return true;
    }
    
    /**
     * Jail a player
     */
    public boolean jailPlayer(UUID targetUuid, String targetName, ServerPlayer moderator, String jailName, String reason, long duration) {
        boolean moderationModuleEnabled = configUnifier.getConfigManager().getMainConfig().modules.moderation;
        if (!moderationModuleEnabled) {
            MessageUtil.sendMessage(moderator, "&cJail system is disabled.");
            return false;
        }
        
        // Check permission
        if (!PermissionUtil.hasPermission(moderator, PermissionNodes.JAIL)) {
            MessageUtil.sendMessage(moderator, "&cYou do not have permission to jail players.");
            return false;
        }
        
        // Check if already jailed
        if (isPlayerJailed(targetUuid)) {
            MessageUtil.sendMessage(moderator, "&c" + targetName + " is already jailed!");
            return false;
        }
        
    // Jail location logic should be handled via main config or not supported
    LocationUtil.Location jailLocation = null; // Replace with main config lookup if needed
        
        ServerPlayer target = getPlayerByUuid(targetUuid);
        if (target == null) {
            MessageUtil.sendMessage(moderator, "&cPlayer not found.");
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
            "&cYou have been jailed for " + MessageUtil.formatTime(duration * 1000) + ": " + finalReason :
            "&cYou have been jailed indefinitely.";
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
        
        if (!PermissionUtil.hasPermission(moderator, PermissionNodes.UNJAIL)) {
            MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.unjail.no_permission");
            return false;
        }

        JailData jailData = activeJails.get(targetUuid);
        if (jailData == null) {
            MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.unjail.not_jailed", targetName);
            return false;
        }

        ServerPlayer target = getPlayerByUuid(targetUuid);
        if (target != null && jailData.previousLocation != null) {
            // Teleport back to previous location
            teleportToLocation(target, jailData.previousLocation);
            MessageUtil.sendTranslatedMessage(target, "neoessentials.moderation.unjail.player");
        }

        activeJails.remove(targetUuid);
        playerDataManager.setSetting(targetUuid, "jail_data", null);

        MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.unjail.success", targetName);

        LOGGER.info("Player {} unjailed by {}", targetName, moderator.getName().getString());

        return true;
    }
    
    /**
     * Temporarily ban a player for a specified duration
     */
    public boolean tempBanPlayer(ServerPlayer target, ServerPlayer moderator, String reason, long durationMinutes) {
        boolean moderationModuleEnabled = configUnifier.getConfigManager().getMainConfig().modules.moderation;
        if (!moderationModuleEnabled) {
            MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.tempban.disabled");
            return false;
        }

        // Check permission
        if (!PermissionUtil.hasPermission(moderator, PermissionNodes.BAN_TEMP)) {
            MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.tempban.no_permission");
            return false;
        }

        // Check if target is exempt
        if (PermissionUtil.hasPermission(target, PermissionNodes.BAN_EXEMPT)) {
            MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.tempban.exempt");
            return false;
        }

        // Get server instance safely
        net.minecraft.server.MinecraftServer server = target.getServer();
        if (server == null) {
            MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.tempban.server_error");
            return false;
        }

        // Check if player is already banned
        if (server.getPlayerList().getBans().isBanned(target.getGameProfile())) {
            MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.tempban.already_banned", target.getName().getString());
            return false;
        }

        String finalReason = reason != null && !reason.isEmpty() ? reason : "Banned by administrator";
        long durationMs = durationMinutes * 60 * 1000; // Convert to milliseconds
        long expiryTime = System.currentTimeMillis() + durationMs;

        try {
            // Create temporary ban entry
            java.util.Date expiryDate = new java.util.Date(expiryTime);
            net.minecraft.server.players.UserBanListEntry banEntry = new net.minecraft.server.players.UserBanListEntry(
                target.getGameProfile(),
                new java.util.Date(),
                moderator.getName().getString(),
                expiryDate,
                finalReason
            );

            // Add to ban list
            server.getPlayerList().getBans().add(banEntry);

            // Disconnect the player
            target.connection.disconnect(Component.translatable("neoessentials.moderation.tempban.player", MessageUtil.formatTime(durationMs), finalReason));

            // Log action
            logModerationAction("TEMPBAN", moderator, target, finalReason, durationMinutes * 60);

            // Broadcast if enabled
            broadcastAction(Component.translatable("neoessentials.moderation.tempban.broadcast", target.getName().getString(), moderator.getName().getString(), MessageUtil.formatTime(durationMs), finalReason).getString());

            MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.tempban.success", target.getName().getString(), MessageUtil.formatTime(durationMs), finalReason);

            LOGGER.info("Player {} temp-banned by {} for {} minutes: {}", 
                target.getName().getString(), moderator.getName().getString(), durationMinutes, finalReason);

            return true;

        } catch (Exception e) {
            MessageUtil.sendTranslatedMessage(moderator, "neoessentials.moderation.tempban.failed", e.getMessage());
            LOGGER.error("Failed to temp-ban player {}", target.getName().getString(), e);
            return false;
        }
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
    // getJailLocation removed: now handled via main config or not supported
    
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
                MessageUtil.sendTranslatedMessage(target, "neoessentials.moderation.jail.expired");
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
