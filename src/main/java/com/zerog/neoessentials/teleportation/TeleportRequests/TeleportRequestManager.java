package com.zerog.neoessentials.teleportation.TeleportRequests;

import com.zerog.neoessentials.teleportation.TeleportLocation;
import com.zerog.neoessentials.teleportation.TeleportUtil;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
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
    // Use daemon threads to prevent blocking JVM shutdown
private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, r -> {
    Thread t = new Thread(r, "TeleportRequest-Scheduler");
    t.setDaemon(true);
    return t;
});

    // Configuration
    private int requestTimeoutSeconds;
    private int teleportDelay = 3; // 3 seconds
    private boolean allowTpaHere = true;
    private boolean allowTpaAll = true;
    private int maxPendingRequests;
    private final int cooldownBetweenRequestsSeconds;
    private final boolean allowMultipleRequests;
    private final boolean enableRequestNotifications;
    private final boolean autoAcceptFromFriends;
    private boolean enableTeleportSafety;
    private final boolean logTeleportRequests;
    private final Map<UUID, Long> lastRequestTimestamps = new ConcurrentHashMap<>();
    
    private TeleportRequestManager() {
    // Load config-driven timeout, max pending requests, cooldown, and allowMultipleRequests
    this.requestTimeoutSeconds = com.zerog.neoessentials.config.ConfigManager.getInstance().getTeleportRequestTimeoutSeconds();
    this.maxPendingRequests = com.zerog.neoessentials.config.ConfigManager.getInstance().getMaxPendingTeleportRequests();
    this.cooldownBetweenRequestsSeconds = com.zerog.neoessentials.config.ConfigManager.getInstance().getCooldownBetweenTeleportRequestsSeconds();
    this.allowMultipleRequests = com.zerog.neoessentials.config.ConfigManager.getInstance().isAllowMultipleTeleportRequestsEnabled();
    this.enableRequestNotifications = com.zerog.neoessentials.config.ConfigManager.getInstance().isTeleportRequestNotificationsEnabled();
    this.autoAcceptFromFriends = com.zerog.neoessentials.config.ConfigManager.getInstance().isAutoAcceptTeleportFromFriendsEnabled();
    this.logTeleportRequests = com.zerog.neoessentials.config.ConfigManager.getInstance().isLogTeleportRequestsEnabled();
    // Enforce teleport safety for teleport requests
    this.enableTeleportSafety = false;
    try {
        com.zerog.neoessentials.config.ConfigManager configManager = com.zerog.neoessentials.config.ConfigManager.getInstance();
        if (configManager != null) {
            com.google.gson.JsonObject config = configManager.getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
            if (config.has("teleportation")) {
                com.google.gson.JsonObject tp = config.getAsJsonObject("teleportation");
                if (tp.has("teleportRequestSettings")) {
                    com.google.gson.JsonObject req = tp.getAsJsonObject("teleportRequestSettings");
                    if (req.has("enableTeleportSafety")) {
                        this.enableTeleportSafety = req.get("enableTeleportSafety").getAsBoolean();
                    }
                }
            }
        }
    } catch (Exception e) {
        LOGGER.warn("Failed to load teleport request safety config, defaulting to disabled: {}", e.getMessage());
    }
        // Start cleanup task
        scheduler.scheduleAtFixedRate(this::cleanupExpiredRequests, 30, 30, TimeUnit.SECONDS);
    }
    
    /**
     * Send a teleportation request
     */
    public boolean sendTeleportRequest(ServerPlayer requester, ServerPlayer target, TeleportRequestType type) {
        UUID requesterId = requester.getUUID();
        UUID targetId = target.getUUID();
        
        // Enforce cooldown between requests - ATOMIC
        if (cooldownBetweenRequestsSeconds > 0) {
            long now = System.currentTimeMillis();
            // Use putIfAbsent to atomically check and set cooldown
            Long last = lastRequestTimestamps.putIfAbsent(requesterId, now);
            if (last != null) {
                if ((now - last) < (cooldownBetweenRequestsSeconds * 1000L)) {
                    long wait = ((cooldownBetweenRequestsSeconds * 1000L) - (now - last)) / 1000L + 1;
                    requester.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.cooldown", wait));
                    return false;
                }
                // Update timestamp atomically
                lastRequestTimestamps.put(requesterId, now);
            }
        }
        
        // Check if requester already has a sent request (ConcurrentHashMap doesn't allow null values)
        TeleportRequest existingSent = sentRequests.get(requesterId);
        if (existingSent != null) {
            // Template needs the EXISTING pending target's name — was missing entirely,
            // leaving a literal unresolved "{0}" in the message.
            requester.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.already_sent",
                existingSent.getTargetName()));
            return false;
        }

        // Enforce allowMultipleRequests: block if requester already has a pending request to this target
        if (!allowMultipleRequests) {
            boolean alreadyRequested = pendingRequests.values().stream()
                .anyMatch(req -> req != null && req.getRequesterId().equals(requesterId) && req.getTargetId().equals(targetId));
            if (alreadyRequested) {
                requester.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.duplicate", target.getName().getString()));
                return false;
            }
        }

        // Essentials tptoggle: check if target is accepting tp requests
        // tpo/tpohere bypass this; only /tpa and /tpahere respect it
        if (!com.zerog.neoessentials.util.commands.ItemCustomisationCommands.isTpToggleAllowed(targetId)
                && !com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(requesterId, "neoessentials.teleport.tpo")) {
            requester.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.tptoggle_off",
                target.getName().getString()));
            return false;
        }

        // Essentials tpauto: if target has auto-accept enabled, skip the request and teleport immediately
        if (com.zerog.neoessentials.teleportation.Misc.MiscTeleportCommands.isTpAutoEnabled(targetId)) {
            executeTeleportRequest(requester, target, type);
            requester.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.request.auto_accepted", target.getName().getString()));
            target.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.request.auto_accepted_target", requester.getName().getString()));
            return true;
        }

        // Check if target has too many pending requests
        long targetPendingCount = pendingRequests.values().stream()
            .filter(req -> req != null && req.getTargetId().equals(targetId))
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
        
        // Store the request atomically - replace the null with actual request
        sentRequests.put(requesterId, request);
        
        // Use putIfAbsent for pending requests to prevent race
        TeleportRequest existingPending = pendingRequests.putIfAbsent(targetId, request);
        if (existingPending != null) {
            // Another request beat us, clean up
            sentRequests.remove(requesterId);
            requester.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.target_busy", target.getName().getString()));
            return false;
        }

        // Auto-accept if enabled and requester is a friend (stub)
        if (autoAcceptFromFriends && isFriend(target, requester)) {
            cleanupRequest(request);
            executeTeleportRequest(requester, target, type);
            requester.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.request.auto_accepted", target.getName().getString()));
            target.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.request.auto_accepted_target", requester.getName().getString()));
            if (logTeleportRequests) {
                LOGGER.info("Teleport request from {} to {} auto-accepted (friends)", requester.getName().getString(), target.getName().getString());
            }
            return true;
        }

        // Schedule timeout
        scheduler.schedule(() -> {
            TeleportRequest currentRequest = pendingRequests.get(targetId);
            if (currentRequest != null && currentRequest.equals(request)) {
                timeoutRequest(request);
            }
        }, requestTimeoutSeconds, TimeUnit.SECONDS);

        // Send messages
        String typeText = type == TeleportRequestType.TPA ? "to teleport to you" : "you to teleport to them";
        requester.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.request.sent",
                                                        target.getName().getString(), requestTimeoutSeconds));

        if (enableRequestNotifications) {
            target.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.request.received",
                                                    requester.getName().getString(), typeText));
            target.sendSystemMessage(MessageUtil.component("commands.neoessentials.teleport.request.instructions"));
            // Send clickable [Accept] and [Deny] buttons
            net.minecraft.network.chat.Component acceptBtn = MessageUtil.clickableCommand(
                "[Accept]", "tpaccept", "Click to accept the teleport request");
            net.minecraft.network.chat.Component denyBtn = MessageUtil.clickableCommand(
                "[Deny]", "tpdeny", "Click to deny the teleport request");
            target.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("")
                    .append(acceptBtn)
                    .append(" ")
                    .append(denyBtn)
            );
        }

        if (logTeleportRequests) {
            LOGGER.info("Player {} sent {} request to {}", 
                   requester.getName().getString(), type, target.getName().getString());
        }

        return true;
    // (isFriend method moved to class body below)
    }
    /**
     * Stub for friends system integration. Returns false until implemented.
     */
    private boolean isFriend(ServerPlayer player, ServerPlayer other) {
        // Simple in-memory friends system (placeholder for real integration)
        // Usage: addFriend(playerUUID, friendUUID) elsewhere in your code
        UUID playerId = player.getUUID();
        UUID otherId = other.getUUID();
        Set<UUID> friends = friendsMap.get(playerId);
        return friends != null && friends.contains(otherId);
    }

    // In-memory friends map: player UUID -> set of friend UUIDs
    private final Map<UUID, Set<UUID>> friendsMap = new ConcurrentHashMap<>();

    /**
     * Add a friend for a player (for demonstration/testing)
     */
    @SuppressWarnings("unused") // Public API method - reserved for future friends system
    public void addFriend(UUID playerId, UUID friendId) {
        friendsMap.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(friendId);
    }

    /**
     * Remove a friend for a player
     */
    @SuppressWarnings("unused") // Public API method - reserved for future friends system
    public void removeFriend(UUID playerId, UUID friendId) {
        Set<UUID> friends = friendsMap.get(playerId);
        if (friends != null) {
            friends.remove(friendId);
        }
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
        
        if (logTeleportRequests) {
            LOGGER.info("Player {} denied {} request from {}", 
                   denier.getName().getString(), request.getType(), request.getRequesterName());
        }
        
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
        
        if (logTeleportRequests) {
            LOGGER.info("Player {} cancelled {} request to {}", 
                   canceller.getName().getString(), request.getType(), request.getTargetName());
        }
        
        return true;
    }
    
    /**
     * Execute the actual teleportation
     */
    private void executeTeleportRequest(ServerPlayer requester, ServerPlayer target, TeleportRequestType type) {
        ServerPlayer teleporter, destination;
        if (type == TeleportRequestType.TPA) {
            teleporter = requester;
            destination = target;
        } else {
            teleporter = target;
            destination = requester;
        }

        // Save current location for /back command
        com.zerog.neoessentials.teleportation.Misc.MiscTeleportManager.getInstance().saveBackLocation(teleporter);

        TeleportLocation targetLocation = new TeleportLocation(destination);

        // Enforce teleport safety if enabled — find a nearby safe spot rather than blocking entirely
        if (enableTeleportSafety && !targetLocation.isSafe()) {
            TeleportLocation safeLocation = targetLocation.findSafeLocation();
            if (safeLocation == null) {
                teleporter.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.unsafe_location", destination.getName().getString()));
                destination.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.unsafe_location_other", teleporter.getName().getString()));
                if (logTeleportRequests) {
                    LOGGER.warn("Teleport request from {} to {} blocked: no safe location found near destination",
                        teleporter.getName().getString(), destination.getName().getString());
                }
                return;
            }
            // Warn and continue with safe location
            teleporter.sendSystemMessage(MessageUtil.warning("commands.neoessentials.teleport.request.moved_to_safety"));
            targetLocation = safeLocation;
        }
        int delayTicks = teleportDelay * 20;
        // findSafe=false: teleport to the destination player's exact position.
        // Using findSafe=true caused /tpa to nether-lava players to land on the nether roof
        // (scanColumnTopDown found Y=128 above bedrock), and /tpa to ocean-boat players to
        // land in underwater caves (top-down scan skipped all water and found a dry cave below).
        // The destination player is alive there → it is an acceptable landing spot.
        TeleportUtil.teleportPlayer(teleporter, targetLocation, delayTicks, false).thenAccept(result -> {
            if (result.isSuccess()) {
                teleporter.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.request.teleported_to", destination.getName().getString()));
                destination.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.request.player_teleported_to_you", teleporter.getName().getString()));
                if (logTeleportRequests) {
                    LOGGER.info("Player {} teleported to {} via {} request", teleporter.getName().getString(), destination.getName().getString(), type);
                }
            } else {
                teleporter.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.failed", result.getMessage()));
                destination.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.request.failed_other", teleporter.getName().getString()));
                if (logTeleportRequests) {
                    LOGGER.warn("Failed teleport request between {} and {}: {}", teleporter.getName().getString(), destination.getName().getString(), result.getMessage());
                }
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
        
        if (logTeleportRequests) {
            LOGGER.info("Teleport request from {} to {} timed out", 
                   request.getRequesterName(), request.getTargetName());
        }
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
    @SuppressWarnings("unused") // Public API method
    public boolean hasPendingRequest(ServerPlayer player) {
        return pendingRequests.containsKey(player.getUUID());
    }
    
    /**
     * Check if player has sent request
     */
    @SuppressWarnings("unused") // Public API method
    public boolean hasSentRequest(ServerPlayer player) {
        return sentRequests.containsKey(player.getUUID());
    }
    
    /**
     * Get pending request info
     */
    @SuppressWarnings("unused") // Public API method
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
    @SuppressWarnings("unused") // Public API method
    public int getRequestTimeoutSeconds() { return requestTimeoutSeconds; }
    @SuppressWarnings("unused") // Public API method
    public void setRequestTimeoutSeconds(int timeout) { this.requestTimeoutSeconds = Math.max(10, timeout); }
    
    @SuppressWarnings("unused") // Public API method
    public int getTeleportDelay() { return teleportDelay; }
    @SuppressWarnings("unused") // Public API method
    public void setTeleportDelay(int delay) { this.teleportDelay = Math.max(0, delay); }
    
    @SuppressWarnings("unused") // Public API method
    public boolean isAllowTpaHere() { return allowTpaHere; }
    @SuppressWarnings("unused") // Public API method
    public void setAllowTpaHere(boolean allow) { this.allowTpaHere = allow; }
    
    @SuppressWarnings("unused") // Public API method
    public boolean isAllowTpaAll() { return allowTpaAll; }
    @SuppressWarnings("unused") // Public API method
    public void setAllowTpaAll(boolean allow) { this.allowTpaAll = allow; }
    
    @SuppressWarnings("unused") // Public API method
    public int getMaxPendingRequests() { return maxPendingRequests; }
    @SuppressWarnings("unused") // Public API method
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

