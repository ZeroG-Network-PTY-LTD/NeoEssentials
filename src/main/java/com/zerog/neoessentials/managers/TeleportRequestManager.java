package com.zerog.neoessentials.managers;

import com.zerog.neoessentials.localization.LanguageManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Teleport request management system for NeoEssentials
 * Handles /tpa, /tpaccept, /tpdeny teleport request functionality
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class TeleportRequestManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeleportRequestManager.class);
    private static TeleportRequestManager instance;
    
    // Map of target player UUID -> list of pending requests
    private final Map<UUID, List<TeleportRequest>> pendingRequests;
    // Map of requester UUID -> last request time (for cooldown)
    private final Map<UUID, Long> requestCooldowns;
    
    private final ScheduledExecutorService scheduler;
    private static final long REQUEST_TIMEOUT_SECONDS = 60;
    private static final long REQUEST_COOLDOWN_SECONDS = 30;
    private static final int MAX_PENDING_REQUESTS = 5;

    public enum RequestType {
        TPA,      // Requester wants to teleport TO target
        TPAHERE   // Requester wants target to teleport TO requester
    }

    public static class TeleportRequest {
        public final UUID requesterId;
        public final String requesterName;
        public final UUID targetId;
        public final String targetName;
        public final RequestType type;
        public final long timestamp;
        
        public TeleportRequest(UUID requesterId, String requesterName, UUID targetId, String targetName, RequestType type) {
            this.requesterId = requesterId;
            this.requesterName = requesterName;
            this.targetId = targetId;
            this.targetName = targetName;
            this.type = type;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > (REQUEST_TIMEOUT_SECONDS * 1000);
        }
    }

    private TeleportRequestManager() {
        this.pendingRequests = new ConcurrentHashMap<>();
        this.requestCooldowns = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        
        // Schedule cleanup task to remove expired requests
        scheduler.scheduleAtFixedRate(this::cleanupExpiredRequests, 30, 30, TimeUnit.SECONDS);
        
        LOGGER.info("TeleportRequestManager initialized");
    }

    public static TeleportRequestManager getInstance() {
        if (instance == null) {
            instance = new TeleportRequestManager();
        }
        return instance;
    }

    /**
     * Send a teleport request
     */
    public boolean sendRequest(ServerPlayer requester, ServerPlayer target, RequestType type) {
        UUID requesterId = requester.getUUID();
        UUID targetId = target.getUUID();
        
        // Check if requester is on cooldown
        if (isOnCooldown(requesterId)) {
            long remaining = getRemainingCooldown(requesterId);
            requester.sendSystemMessage(MessageUtil.translatable(requester, "neoessentials.teleport.cooldown_active", String.valueOf(remaining)));
            return false;
        }
        // Check if requester is trying to request to themselves
        if (requesterId.equals(targetId)) {
            requester.sendSystemMessage(MessageUtil.translatable(requester, "neoessentials.teleport.cannot_request_self"));
            return false;
        }
        // Check if target has too many pending requests
        List<TeleportRequest> targetRequests = pendingRequests.computeIfAbsent(targetId, k -> new ArrayList<>());
        if (targetRequests.size() >= MAX_PENDING_REQUESTS) {
            requester.sendSystemMessage(MessageUtil.translatable(requester, "neoessentials.teleport.target_too_many_requests", target.getName().getString()));
            return false;
        }
        // Check if there's already a pending request from this requester to this target
        boolean alreadyExists = targetRequests.stream()
            .anyMatch(req -> req.requesterId.equals(requesterId) && !req.isExpired());
        if (alreadyExists) {
            requester.sendSystemMessage(MessageUtil.translatable(requester, "neoessentials.teleport.already_pending_request", target.getName().getString()));
            return false;
        }
        
        // Create and add the request
        TeleportRequest request = new TeleportRequest(
            requesterId, 
            requester.getName().getString(),
            targetId, 
            target.getName().getString(),
            type
        );
        
        targetRequests.add(request);
        
        // Set cooldown for requester
        requestCooldowns.put(requesterId, System.currentTimeMillis());
        
        // Send localized messages
    String typeText = type == RequestType.TPA ? com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.teleport.type_tpa") : com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.teleport.type_tpahere");
    MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.teleport.requested", requester.getName().getString(), typeText));
    MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.teleport.accept_deny"));
    MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.teleport.request_expires", String.valueOf(REQUEST_TIMEOUT_SECONDS)));
    MessageUtil.sendMessage(requester, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(requester, "neoessentials.teleport.request_sent", target.getName().getString()));
        
        // Schedule auto-removal
        scheduler.schedule(() -> {
            removeRequest(targetId, requesterId);
        }, REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        LOGGER.info("TPA request sent from {} to {} (type: {})", 
            requester.getName().getString(), target.getName().getString(), type);
        
        return true;
    }

    /**
     * Accept a teleport request
     */
    public boolean acceptRequest(ServerPlayer target, String requesterName) {
        UUID targetId = target.getUUID();
        List<TeleportRequest> requests = pendingRequests.get(targetId);
        
        if (requests == null || requests.isEmpty()) {
            MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.teleport.no_pending_requests"));
            return false;
        }
        
        // Remove expired requests
        requests.removeIf(TeleportRequest::isExpired);
        
        if (requests.isEmpty()) {
            MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.teleport.no_pending_requests"));
            return false;
        }
        
        TeleportRequest request = null;
        
        // Find specific request if requester name provided
        if (requesterName != null && !requesterName.isEmpty()) {
            request = requests.stream()
                .filter(req -> req.requesterName.equalsIgnoreCase(requesterName) && !req.isExpired())
                .findFirst()
                .orElse(null);
                
            if (request == null) {
                MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.teleport.no_pending_from", requesterName));
                return false;
            }
        } else {
            // Accept the most recent request
            request = requests.get(requests.size() - 1);
        }
        
        // Find the requester player
        var server = target.getServer();
        if (server == null) {
            MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.teleport.server_error"));
            requests.remove(request);
            return false;
        }
        
        ServerPlayer requester = server.getPlayerList().getPlayer(request.requesterId);
        if (requester == null) {
            MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.teleport.requester_offline", request.requesterName));
            requests.remove(request);
            return false;
        }
        
        // Perform the teleportation
        boolean success = performTeleport(request, requester, target);
        
        // Remove the request
        requests.remove(request);
        
        return success;
    }

    /**
     * Deny a teleport request
     */
    public boolean denyRequest(ServerPlayer target, String requesterName) {
        UUID targetId = target.getUUID();
        List<TeleportRequest> requests = pendingRequests.get(targetId);
        
        if (requests == null || requests.isEmpty()) {
            MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.teleport.no_pending_requests"));
            return false;
        }
        
        // Remove expired requests
        requests.removeIf(TeleportRequest::isExpired);
        
        if (requests.isEmpty()) {
            MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.teleport.no_pending_requests"));
            return false;
        }
        
        TeleportRequest request = null;
        
        // Find specific request if requester name provided
        if (requesterName != null && !requesterName.isEmpty()) {
            request = requests.stream()
                .filter(req -> req.requesterName.equalsIgnoreCase(requesterName) && !req.isExpired())
                .findFirst()
                .orElse(null);
                
            if (request == null) {
                MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.teleport.no_pending_from", requesterName));
                return false;
            }
        } else {
            // Deny the most recent request
            request = requests.get(requests.size() - 1);
        }
        
        // Find the requester player to notify them
        var server = target.getServer();
        if (server != null) {
            ServerPlayer requester = server.getPlayerList().getPlayer(request.requesterId);
            if (requester != null) {
                MessageUtil.sendMessage(requester, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(requester, "neoessentials.teleport.request_denied_by_target", target.getName().getString()));
            }
        }
        
    MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.teleport.denied", request.requesterName));
        
        // Remove the request
        requests.remove(request);
        
        LOGGER.info("TPA request from {} to {} denied", request.requesterName, target.getName().getString());
        
        return true;
    }

    /**
     * Get pending requests for a player
     */
    public List<TeleportRequest> getPendingRequests(UUID playerId) {
        List<TeleportRequest> requests = pendingRequests.get(playerId);
        if (requests == null) {
            return new ArrayList<>();
        }
        
        // Remove expired requests
        requests.removeIf(TeleportRequest::isExpired);
        return new ArrayList<>(requests);
    }

    /**
     * Check if player is on request cooldown
     */
    private boolean isOnCooldown(UUID playerId) {
        Long lastRequest = requestCooldowns.get(playerId);
        if (lastRequest == null) {
            return false;
        }
        
        return (System.currentTimeMillis() - lastRequest) < (REQUEST_COOLDOWN_SECONDS * 1000);
    }

    /**
     * Get remaining cooldown in seconds
     */
    private long getRemainingCooldown(UUID playerId) {
        Long lastRequest = requestCooldowns.get(playerId);
        if (lastRequest == null) {
            return 0;
        }
        
        long elapsed = (System.currentTimeMillis() - lastRequest) / 1000;
        return Math.max(0, REQUEST_COOLDOWN_SECONDS - elapsed);
    }

    /**
     * Remove a specific request
     */
    private void removeRequest(UUID targetId, UUID requesterId) {
        List<TeleportRequest> requests = pendingRequests.get(targetId);
        if (requests != null) {
            requests.removeIf(req -> req.requesterId.equals(requesterId));
            if (requests.isEmpty()) {
                pendingRequests.remove(targetId);
            }
        }
    }

    /**
     * Perform the actual teleportation
     */
    private boolean performTeleport(TeleportRequest request, ServerPlayer requester, ServerPlayer target) {
        try {
            if (request.type == RequestType.TPA) {
                requester.teleportTo(target.serverLevel(), 
                    target.getX(), target.getY(), target.getZ(), 
                    target.getYRot(), target.getXRot());
                MessageUtil.sendMessage(requester, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(requester, "neoessentials.teleport.success", target.getName().getString()));
                MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.teleport.success_to_you", requester.getName().getString()));
            } else { // TPAHERE
                target.teleportTo(requester.serverLevel(), 
                    requester.getX(), requester.getY(), requester.getZ(), 
                    requester.getYRot(), requester.getXRot());
                MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.teleport.success", requester.getName().getString()));
                MessageUtil.sendMessage(requester, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(requester, "neoessentials.teleport.success_to_you", target.getName().getString()));
            }
            
            LOGGER.info("TPA teleport completed: {} (type: {})", request.requesterName, request.type);
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to perform TPA teleport", e);
            MessageUtil.sendMessage(requester, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(requester, "neoessentials.teleport.failed"));
            MessageUtil.sendMessage(target, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(target, "neoessentials.teleport.failed"));
            return false;
        }
    }

    /**
     * Clean up expired requests
     */
    private void cleanupExpiredRequests() {
        pendingRequests.values().forEach(requests -> requests.removeIf(TeleportRequest::isExpired));
        pendingRequests.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        // Clean up old cooldowns (older than 24 hours)
        long dayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        requestCooldowns.entrySet().removeIf(entry -> entry.getValue() < dayAgo);
    }

    /**
     * Shutdown the manager
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
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
        LOGGER.info("TeleportRequestManager shutdown");
    }
}
