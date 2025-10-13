package com.zerog.neoessentials.teleportation.TeleportRequests;

import com.zerog.neoessentials.teleportation.TeleportLocation;
import com.zerog.neoessentials.teleportation.TeleportUtil;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages teleportation requests between players (/tpa, /tpaccept, /tpdeny)
 */
public class TeleportRequestManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeleportRequestManager.class);
    
    // Singleton pattern
    private static class SingletonHolder {
        private static final TeleportRequestManager INSTANCE = new TeleportRequestManager();
    }
    
    public static TeleportRequestManager getInstance() {
        return SingletonHolder.INSTANCE;
    }
    
    private final Map<UUID, TeleportRequest> pendingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, TeleportRequest> sentRequests = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // Configuration
    private int requestTimeoutSeconds = 60; // 1 minute
    private int teleportDelay = 3; // 3 seconds
    private boolean allowTpaHere = true;
    private boolean allowTpaAll = true;
    private int maxPendingRequests = 5;
    
    private TeleportRequestManager() {
        // Start cleanup task
        scheduler.scheduleAtFixedRate(this::cleanupExpiredRequests, 30, 30, TimeUnit.SECONDS);
    }
    
    /**
     * Send a teleportation request
     */
    public boolean sendTeleportRequest(ServerPlayer requester, ServerPlayer target, TeleportRequestType type) {
        UUID requesterId = requester.getUUID();
        UUID targetId = target.getUUID();
        
        // Check if requester can send requests
        if (sentRequests.containsKey(requesterId)) {
            requester.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.already_sent"));
            return false;
        }
        
        // Check if target has too many pending requests
        long targetPendingCount = pendingRequests.values().stream()
            .filter(req -> req.getTargetId().equals(targetId))
            .count();
        
        if (targetPendingCount >= maxPendingRequests) {
            requester.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.target_busy", target.getName().getString()));
            return false;
        }
        
        // Create the request
        TeleportRequest request = new TeleportRequest(
            requesterId,
            requester.getName().getString(),
            targetId,
            target.getName().getString(),
            type,
            System.currentTimeMillis() + (requestTimeoutSeconds * 1000L)
        );
        
        // Store the request
        pendingRequests.put(targetId, request);
        sentRequests.put(requesterId, request);
        
        // Schedule timeout
        scheduler.schedule(() -> {
            if (pendingRequests.containsKey(targetId) && 
                pendingRequests.get(targetId).equals(request)) {
                timeoutRequest(request);
            }
        }, requestTimeoutSeconds, TimeUnit.SECONDS);
        
        // Send messages
        String typeText = type == TeleportRequestType.TPA ? "to you" : "you to them";
        requester.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.request.sent", 
                                                        target.getName().getString(), typeText));
        
        target.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.request.received", 
                                                  requester.getName().getString(), typeText));
        target.sendSystemMessage(MessageUtil.component("commands.neoessentials.teleport.request.instructions"));
        
        LOGGER.info("Player {} sent {} request to {}", 
                   requester.getName().getString(), type, target.getName().getString());
        
        return true;
    }
    
    /**
     * Accept a teleportation request
     */
    public boolean acceptTeleportRequest(ServerPlayer accepter) {
        UUID accepterId = accepter.getUUID();
        TeleportRequest request = pendingRequests.get(accepterId);
        
        if (request == null) {
            accepter.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.none_pending"));
            return false;
        }
        
        // Check if request has expired
        if (System.currentTimeMillis() > request.getExpiryTime()) {
            cleanupRequest(request);
            accepter.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.expired"));
            return false;
        }
        
        // Get the requester
        ServerPlayer requester = getPlayerById(request.getRequesterId());
        if (requester == null) {
            cleanupRequest(request);
            accepter.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.requester_offline"));
            return false;
        }
        
        // Clean up the request
        cleanupRequest(request);
        
        // Execute the teleportation
        executeTeleportRequest(requester, accepter, request.getType());
        
        return true;
    }
    
    /**
     * Deny a teleportation request
     */
    public boolean denyTeleportRequest(ServerPlayer denier) {
        UUID denierId = denier.getUUID();
        TeleportRequest request = pendingRequests.get(denierId);
        
        if (request == null) {
            denier.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.none_pending"));
            return false;
        }
        
        // Get the requester
        ServerPlayer requester = getPlayerById(request.getRequesterId());
        
        // Clean up the request
        cleanupRequest(request);
        
        // Send messages
        denier.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.request.denied_by_you", 
                                                     request.getRequesterName()));
        
        if (requester != null) {
            requester.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.denied_by_target", 
                                                          denier.getName().getString()));
        }
        
        LOGGER.info("Player {} denied {} request from {}", 
                   denier.getName().getString(), request.getType(), request.getRequesterName());
        
        return true;
    }
    
    /**
     * Cancel a sent teleportation request
     */
    public boolean cancelTeleportRequest(ServerPlayer canceller) {
        UUID cancellerId = canceller.getUUID();
        TeleportRequest request = sentRequests.get(cancellerId);
        
        if (request == null) {
            canceller.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.none_sent"));
            return false;
        }
        
        // Get the target
        ServerPlayer target = getPlayerById(request.getTargetId());
        
        // Clean up the request
        cleanupRequest(request);
        
        // Send messages
        canceller.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.request.cancelled", 
                                                        request.getTargetName()));
        
        if (target != null) {
            target.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.request.cancelled_by_requester", 
                                                      canceller.getName().getString()));
        }
        
        LOGGER.info("Player {} cancelled {} request to {}", 
                   canceller.getName().getString(), request.getType(), request.getTargetName());
        
        return true;
    }
    
    /**
     * Execute the actual teleportation
     */
    private void executeTeleportRequest(ServerPlayer requester, ServerPlayer target, TeleportRequestType type) {
        ServerPlayer teleporter, destination;
        
        if (type == TeleportRequestType.TPA) {
            // Requester teleports to target
            teleporter = requester;
            destination = target;
        } else {
            // Target teleports to requester
            teleporter = target;
            destination = requester;
        }
        
        TeleportLocation targetLocation = new TeleportLocation(destination);
        
        // Perform teleportation with delay
        int delayTicks = teleportDelay * 20; // Convert seconds to ticks
        TeleportUtil.teleportPlayer(teleporter, targetLocation, delayTicks, true).thenAccept(result -> {
            if (result.isSuccess()) {
                teleporter.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.request.teleported_to", 
                                                                 destination.getName().getString()));
                destination.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.request.player_teleported_to_you", 
                                                                  teleporter.getName().getString()));
                
                LOGGER.info("Player {} teleported to {} via {} request", 
                           teleporter.getName().getString(), destination.getName().getString(), type);
            } else {
                teleporter.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.failed", result.getMessage()));
                destination.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.failed_other", 
                                                               teleporter.getName().getString()));
                
                LOGGER.warn("Failed teleport request between {} and {}: {}", 
                           teleporter.getName().getString(), destination.getName().getString(), result.getMessage());
            }
        });
    }
    
    /**
     * Handle request timeout
     */
    private void timeoutRequest(TeleportRequest request) {
        cleanupRequest(request);
        
        ServerPlayer requester = getPlayerById(request.getRequesterId());
        ServerPlayer target = getPlayerById(request.getTargetId());
        
        if (requester != null) {
            requester.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.timed_out", 
                                                          request.getTargetName()));
        }
        
        if (target != null) {
            target.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.expired_received", 
                                                       request.getRequesterName()));
        }
        
        LOGGER.info("Teleport request from {} to {} timed out", 
                   request.getRequesterName(), request.getTargetName());
    }
    
    /**
     * Clean up a request from all maps
     */
    private void cleanupRequest(TeleportRequest request) {
        pendingRequests.remove(request.getTargetId());
        sentRequests.remove(request.getRequesterId());
    }
    
    /**
     * Clean up expired requests
     */
    private void cleanupExpiredRequests() {
        long currentTime = System.currentTimeMillis();
        
        pendingRequests.values().removeIf(request -> {
            if (currentTime > request.getExpiryTime()) {
                sentRequests.remove(request.getRequesterId());
                return true;
            }
            return false;
        });
    }
    
    /**
     * Get player by UUID
     */
    private ServerPlayer getPlayerById(UUID playerId) {
        // Get the current server instance
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        
        // Search through online players for matching UUID
        return server.getPlayerList().getPlayers().stream()
                .filter(player -> player.getUUID().equals(playerId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Check if player has pending request
     */
    public boolean hasPendingRequest(ServerPlayer player) {
        return pendingRequests.containsKey(player.getUUID());
    }
    
    /**
     * Check if player has sent request
     */
    public boolean hasSentRequest(ServerPlayer player) {
        return sentRequests.containsKey(player.getUUID());
    }
    
    /**
     * Get pending request info
     */
    public String getPendingRequestInfo(ServerPlayer player) {
        TeleportRequest request = pendingRequests.get(player.getUUID());
        if (request == null) {
            return MessageUtil.localize("commands.neoessentials.teleport.request.no_pending");
        }
        
        long timeLeft = (request.getExpiryTime() - System.currentTimeMillis()) / 1000;
        String typeText = request.getType() == TeleportRequestType.TPA ? "to teleport to you" : "you to teleport to them";
        
        return MessageUtil.localize("teleport.request.pending_info", 
                                   request.getRequesterName(), typeText, timeLeft);
    }
    
    // Configuration getters/setters
    public int getRequestTimeoutSeconds() { return requestTimeoutSeconds; }
    public void setRequestTimeoutSeconds(int timeout) { this.requestTimeoutSeconds = Math.max(10, timeout); }
    
    public int getTeleportDelay() { return teleportDelay; }
    public void setTeleportDelay(int delay) { this.teleportDelay = Math.max(0, delay); }
    
    public boolean isAllowTpaHere() { return allowTpaHere; }
    public void setAllowTpaHere(boolean allow) { this.allowTpaHere = allow; }
    
    public boolean isAllowTpaAll() { return allowTpaAll; }
    public void setAllowTpaAll(boolean allow) { this.allowTpaAll = allow; }
    
    public int getMaxPendingRequests() { return maxPendingRequests; }
    public void setMaxPendingRequests(int max) { this.maxPendingRequests = Math.max(1, max); }
    
    /**
     * Shutdown the manager
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Get statistics
     */
    public String getStatistics() {
        return String.format("TeleportRequest Statistics: %d pending, %d sent, timeout: %ds", 
                           pendingRequests.size(), sentRequests.size(), requestTimeoutSeconds);
    }
}