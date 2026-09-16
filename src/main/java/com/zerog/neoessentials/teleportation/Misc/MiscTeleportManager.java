package com.zerog.neoessentials.teleportation.Misc;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.teleportation.TeleportLocation;
import com.zerog.neoessentials.teleportation.TeleportUtil;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for miscellaneous teleportation utilities (/back, death locations, etc.)
 */
@EventBusSubscriber(modid = "neoessentials", bus = EventBusSubscriber.Bus.GAME)
public class MiscTeleportManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiscTeleportManager.class);
    
    // Singleton pattern
    private static class SingletonHolder {
        private static final MiscTeleportManager INSTANCE = new MiscTeleportManager();
    }
    
    public static MiscTeleportManager getInstance() {
        return SingletonHolder.INSTANCE;
    }
    
    // Per-player /back undo-stack — most recent push is next to pop. Deliberately in-memory
    // only (no disk persistence): a stale history from a previous session shouldn't resurface
    // after a disconnect/reconnect or a server restart. Bounded to maxBackHistory, oldest
    // entry dropped first once a push would exceed that size.
    private final Map<UUID, Deque<TeleportLocation>> backHistory = new ConcurrentHashMap<>();

    // Configuration (loaded lazily from ConfigManager)
    private int maxBackHistory = 10;
    private int teleportDelay = 3;
    private boolean enableDeathBack = true;
    private boolean enableTeleportBack = true;
    private int backCooldownSeconds = 0; // 0 = no cooldown
    private boolean enableBackSafety = true; // teleportation.backSettings.enableBackSafety

    // Cooldown tracking for /back
    private final Map<UUID, Long> lastBackTimestamps = new ConcurrentHashMap<>();

    // Players whose most recent history push was a death location — consulted once on
    // respawn to decide whether to show the "use /back to return" hint, then cleared.
    private final java.util.Set<UUID> pendingDeathHints = ConcurrentHashMap.newKeySet();

    private MiscTeleportManager() {
        loadConfig();
    }

    /**
     * (Re)load configuration values from ConfigManager.
     */
    private void loadConfig() {
        try {
            ConfigManager cfg = ConfigManager.getInstance();
            teleportDelay = cfg.getBackTeleportDelay();
            enableDeathBack = cfg.isDeathBackEnabled();
            enableTeleportBack = cfg.isTeleportBackEnabled();
            enableBackSafety = cfg.isBackTeleportSafetyEnabled();
            maxBackHistory = cfg.getMaxBackHistory();
            // Read back cooldown from config (check backSettings first, then legacy miscSettings)
            try {
                com.google.gson.JsonObject config = cfg.getConfig(ConfigManager.MAIN_CONFIG);
                if (config.has("teleportation")) {
                    com.google.gson.JsonObject tp = config.getAsJsonObject("teleportation");
                    // Prefer backSettings (new canonical location)
                    if (tp.has("backSettings")) {
                        com.google.gson.JsonObject bs = tp.getAsJsonObject("backSettings");
                        if (bs.has("backCooldown")) {
                            backCooldownSeconds = bs.get("backCooldown").getAsInt();
                        }
                    } else if (tp.has("miscSettings")) {
                        // Legacy fallback
                        com.google.gson.JsonObject misc = tp.getAsJsonObject("miscSettings");
                        if (misc.has("backCooldown")) {
                            backCooldownSeconds = misc.get("backCooldown").getAsInt();
                        }
                    }
                }
            } catch (Exception e) {
                NeoLog.debug(LOGGER, LogCategory.TELEPORTATION,
                    "Failed to read back cooldown from config, using default", e);
            }
        NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "[MiscTeleportManager] Config loaded — warmup={}s, cooldown={}s, deathBack={}, teleportBack={}, backSafety={}",
            teleportDelay, backCooldownSeconds, enableDeathBack, enableTeleportBack, enableBackSafety);
        } catch (Exception e) {
            LOGGER.warn("Failed to load MiscTeleportManager config, using defaults: {}", e.getMessage());
        }
    }

    // ── History stack helpers ────────────────────────────────────────────────

    /** Pushes {@code location} onto {@code playerId}'s undo-stack, evicting the oldest entry if over the cap. */
    private void pushHistory(UUID playerId, TeleportLocation location) {
        Deque<TeleportLocation> stack = backHistory.computeIfAbsent(playerId, id -> new ArrayDeque<>());
        synchronized (stack) {
            stack.addLast(location);
            while (stack.size() > maxBackHistory) {
                stack.removeFirst();
            }
        }
    }

    /** Pops (removes and returns) the most recent entry from {@code playerId}'s undo-stack, or {@code null} if empty. */
    private TeleportLocation popHistory(UUID playerId) {
        Deque<TeleportLocation> stack = backHistory.get(playerId);
        if (stack == null) return null;
        synchronized (stack) {
            return stack.pollLast();
        }
    }

    /** Peeks the most recent entry from {@code playerId}'s undo-stack without removing it, or {@code null} if empty. */
    private TeleportLocation peekHistory(UUID playerId) {
        Deque<TeleportLocation> stack = backHistory.get(playerId);
        if (stack == null) return null;
        synchronized (stack) {
            return stack.peekLast();
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Push a player's current location onto their /back undo-stack.
     */
    public void saveBackLocation(ServerPlayer player) {
        if (!enableTeleportBack) {
            return;
        }

        UUID playerId = player.getUUID();
        TeleportLocation backLocation = new TeleportLocation(player);
        pushHistory(playerId, backLocation);

        NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "Saved back location for {}: {}",
                    player.getName().getString(), backLocation);
    }

    /**
     * Push a player's death location onto their /back undo-stack.
     */
    public void saveDeathLocation(ServerPlayer player) {
        if (!enableDeathBack) {
            return;
        }

        UUID playerId = player.getUUID();
        TeleportLocation deathLocation = new TeleportLocation(player);
        pushHistory(playerId, deathLocation);
        pendingDeathHints.add(playerId);

        // Do NOT send a chat message here — LivingDeathEvent fires while the player is
        // transitioning to the death screen, so they cannot read it.  The "use /back to
        // return to death location" hint is sent on respawn instead (see onPlayerRespawn).
        NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Saved death location for {}: {}",
                   player.getName().getString(), deathLocation);
    }

    /**
     * Teleport player back through their undo-stack, one hop per call.
     *
     * <p>Each teleport (home/warp/spawn/tpa/death/etc.) pushes the player's prior
     * location onto a per-player LIFO stack; /back simply pops the most recent
     * entry. The stack is purely in-memory — it does not persist across a
     * disconnect/reconnect or a server restart.</p>
     */
    public boolean teleportBack(ServerPlayer player) {
        UUID playerId = player.getUUID();
        NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "teleportBack request: player={}", player.getName().getString());

        TeleportLocation targetLocation = popHistory(playerId);
        if (targetLocation == null) {
            NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "teleportBack: {} has no back location", player.getName().getString());
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.no_back_location"));
            return false;
        }

        // Enforce /back cooldown (skip if player has bypass permission)
        boolean bypassCooldown = com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(playerId, "neoessentials.teleport.bypass.cooldown")
            || com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(playerId, "neoessentials.teleport.back.bypass.cooldown");
        if (backCooldownSeconds > 0 && !bypassCooldown) {
            long now = System.currentTimeMillis();
            Long lastBack = lastBackTimestamps.putIfAbsent(playerId, now);
            if (lastBack != null) {
                long elapsed = (now - lastBack) / 1000L;
                if (elapsed < backCooldownSeconds) {
                    long wait = backCooldownSeconds - elapsed;
                    player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.back_cooldown", wait));
                    // Put the popped entry back — the /back attempt didn't actually happen.
                    pushHistory(playerId, targetLocation);
                    return false;
                }
                lastBackTimestamps.put(playerId, now);
            }
        }

        final TeleportLocation finalTargetLocation = targetLocation;

        // Bypass warmup for players with the permission
        boolean bypassWarmup = com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(playerId, "neoessentials.teleport.bypass.warmup")
            || com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(playerId, "neoessentials.teleport.back.bypass.warmup");
        int delayTicks = bypassWarmup ? 0 : teleportDelay * 20;
        if (delayTicks > 0) {
            player.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.misc.back_warmup", teleportDelay));
        }
        TeleportUtil.teleportPlayer(player, finalTargetLocation, delayTicks, enableBackSafety).thenAccept(result -> {
            if (result.isSuccess()) {
                player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.misc.back_success"));
                NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Player {} teleported back to {}", player.getName().getString(), finalTargetLocation);
            } else {
                // Teleport failed (e.g. blocked destination) — restore the popped entry
                // so the player doesn't lose a step of history for a no-op /back.
                pushHistory(playerId, finalTargetLocation);
                player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.back_failed", result.getMessage()));
                LOGGER.warn("Failed back teleport for {}: {}", player.getName().getString(), result.getMessage());
            }
        });
        return true;
    }

    /**
     * Helper to check if player is already at a given location (within a small threshold)
     */
    private boolean isPlayerAtLocation(ServerPlayer player, TeleportLocation location) {
        double dx = player.getX() - location.getX();
        double dy = player.getY() - location.getY();
        double dz = player.getZ() - location.getZ();
        return Math.abs(dx) < 0.5 && Math.abs(dy) < 1.0 && Math.abs(dz) < 0.5 &&
                player.level().dimension().location().toString().equals(location.getWorldName());
    }

    /**
     * Clear a player's /back undo-stack.
     */
    public void clearBackLocation(ServerPlayer player) {
        UUID playerId = player.getUUID();
        backHistory.remove(playerId);
        pendingDeathHints.remove(playerId);

        NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "Cleared back locations for {}", player.getName().getString());
    }

    /**
     * Check if player has any /back history.
     */
    public boolean hasBackLocation(ServerPlayer player) {
        Deque<TeleportLocation> stack = backHistory.get(player.getUUID());
        return stack != null && !stack.isEmpty();
    }

    /**
     * Get info about the top of a player's /back undo-stack.
     */
    public String getBackLocationInfo(ServerPlayer player) {
        TeleportLocation location = peekHistory(player.getUUID());
        if (location != null) {
            return MessageUtil.localize("commands.neoessentials.teleport.misc.back_info",
                                       location.getWorldName(),
                                       String.format("%.1f %.1f %.1f", location.getX(), location.getY(), location.getZ()));
        }

        return MessageUtil.localize("commands.neoessentials.teleport.misc.no_back_location");
    }

    /**
     * Handle player disconnect — clears the in-memory /back history since it is
     * deliberately not persisted (a stale history shouldn't resurface on rejoin).
     */
    public void onPlayerDisconnect(ServerPlayer player) {
        UUID playerId = player.getUUID();
        backHistory.remove(playerId);
        pendingDeathHints.remove(playerId);
        NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "Player {} disconnected; cleared in-memory /back history.", player.getName().getString());
    }

    /**
     * Event handler: clear a player's /back history on disconnect.
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MiscTeleportManager.getInstance().onPlayerDisconnect(player);
    }
    
    /**
     * Event handler: Save death location when player dies.
     * The hint message is NOT sent here (player is in-death-screen); it is sent
     * on respawn via {@link #onPlayerRespawn} instead.
     *
     * <p>Uses {@code receiveCanceled = true} so the death position is captured even
     * when another mod cancels the event (keep-inventory, god-mode plugins, etc.).
     * We still save only when the entity is an actual {@link ServerPlayer}.</p>
     */
    @SubscribeEvent(receiveCanceled = true)
    public static void onPlayerDeathEvent(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "[MiscTeleportManager] Death event fired for {} at ({}, {}, {}) in {} — cancelled={}",
            player.getName().getString(),
            String.format("%.2f", player.getX()),
            String.format("%.2f", player.getY()),
            String.format("%.2f", player.getZ()),
            player.level().dimension().location(),
            event.isCanceled());
        // Always save the death location, regardless of event cancellation status.
        // This ensures /back works even with keep-inventory or protection plugins.
        MiscTeleportManager.getInstance().saveDeathLocation(player);
    }

    /**
     * Event handler: Send the "death location saved – use /back" hint after
     * the player has respawned and can actually read the message.
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MiscTeleportManager mgr = MiscTeleportManager.getInstance();
        if (!mgr.enableDeathBack) return;
        // Only show the hint if this respawn was due to death (not /kill or end-portal return)
        UUID playerId = player.getUUID();
        if (mgr.pendingDeathHints.remove(playerId)) {
            net.minecraft.server.MinecraftServer server = player.getServer();
            if (server == null) return;
            // Delay one tick so the hint arrives after vanilla respawn messages
            com.zerog.neoessentials.scheduler.DelayedTaskScheduler.schedule(1,
                () -> player.sendSystemMessage(
                    MessageUtil.info("commands.neoessentials.teleport.misc.death_location_saved")));
        }
    }
    
    /**
     * Configuration getters/setters
     */
    public int getMaxBackHistory() {
        return maxBackHistory;
    }
    
    public void setMaxBackHistory(int max) {
        this.maxBackHistory = Math.max(1, max);
    }
    
    public int getTeleportDelay() {
        return teleportDelay;
    }
    
    public void setTeleportDelay(int delay) {
        this.teleportDelay = Math.max(0, delay);
    }
    
    public boolean isEnableDeathBack() {
        return enableDeathBack;
    }
    
    public void setEnableDeathBack(boolean enable) {
        this.enableDeathBack = enable;
    }
    
    public boolean isEnableTeleportBack() {
        return enableTeleportBack;
    }
    
    public void setEnableTeleportBack(boolean enable) {
        this.enableTeleportBack = enable;
    }
    
    /**
     * Get statistics
     */
    public String getStatistics() {
        return String.format("MiscTeleport Statistics: %d players with /back history (in-memory), delay=%ds",
                           backHistory.size(), teleportDelay);
    }
    
    /**
     * Teleport player to the highest solid block at their current position
     */
    public boolean teleportToTop(ServerPlayer player) {
        NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "teleportToTop request: player={}", player.getName().getString());
        // Save current location as back location
        saveBackLocation(player);
        
        int currentX = (int) player.getX();
        int currentZ = (int) player.getZ();
        int maxY = com.zerog.neoessentials.util.LevelHeightCompat.maxBuildHeight(player.level()) - 1;
        
        // Find the highest solid block
        for (int y = maxY; y >= com.zerog.neoessentials.util.LevelHeightCompat.minBuildHeight(player.level()); y--) {
            if (!player.level().getBlockState(new net.minecraft.core.BlockPos(currentX, y, currentZ)).isAir()) {
                final int targetY = y + 1;
                TeleportLocation topLocation = new TeleportLocation(
                    player.level().dimension().location().toString(),
                    currentX + 0.5, targetY, currentZ + 0.5,
                    player.getYRot(), player.getXRot(), "system"
                );
                
                int delayTicks = teleportDelay * 20;
                // /top already found a solid block — safety check is redundant and would
                // re-run findSafeLocation on an already-safe spot; skip it.
                TeleportUtil.teleportPlayer(player, topLocation, delayTicks, false).thenAccept(result -> {
                    if (result.isSuccess()) {
                        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.misc.top_success"));
                        NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Player {} teleported to top at Y={}", player.getName().getString(), targetY);
                    } else {
                        player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.top_failed", result.getMessage()));
                    }
                });
                return true;
            }
        }
        
        player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.no_solid_block"));
        return false;
    }
    
    /**
     * Teleport player through walls to the next open space
     */
    public boolean teleportJump(ServerPlayer player) {
        // Save current location as back location
        saveBackLocation(player);
        
        net.minecraft.world.phys.Vec3 lookDirection = player.getLookAngle();
        net.minecraft.world.phys.Vec3 currentPos = player.position();
        
        // Search for next open space in look direction
        for (int distance = 1; distance <= 20; distance++) {
            double newX = currentPos.x + lookDirection.x * distance;
            double newY = currentPos.y + lookDirection.y * distance;
            double newZ = currentPos.z + lookDirection.z * distance;
            
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos((int) newX, (int) newY, (int) newZ);
            net.minecraft.core.BlockPos posAbove = pos.above();
            
            // Check if there's enough space for player (2 blocks high)
            if (player.level().getBlockState(pos).isAir() && 
                player.level().getBlockState(posAbove).isAir()) {
                
                final int finalDistance = distance; // Make final for lambda
                TeleportLocation jumpLocation = new TeleportLocation(
                    player.level().dimension().location().toString(),
                    newX, newY, newZ,
                    player.getYRot(), player.getXRot(), "system"
                );
                
                int delayTicks = teleportDelay * 20;
                // /jump scanned for open air space — safety check not needed.
                TeleportUtil.teleportPlayer(player, jumpLocation, delayTicks, false).thenAccept(result -> {
                    if (result.isSuccess()) {
                        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.misc.jump_success"));
                        NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Player {} jumped through walls to distance {}", player.getName().getString(), finalDistance);
                    } else {
                        player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.jump_failed", result.getMessage()));
                    }
                });
                return true;
            }
        }
        
        player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.no_open_space"));
        return false;
    }
    
    /**
     * Teleport player to the block they are looking at
     */
    public boolean teleportToLookingAt(ServerPlayer player) {
        // Save current location as back location
        saveBackLocation(player);
        
        // Perform raycast to find what player is looking at
        net.minecraft.world.phys.HitResult hitResult = player.pick(100.0D, 1.0F, false);
        
        if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            net.minecraft.world.phys.BlockHitResult blockHitResult = (net.minecraft.world.phys.BlockHitResult) hitResult;
            net.minecraft.core.BlockPos targetPos = blockHitResult.getBlockPos();
            
            // Teleport to one block above the target block
            TeleportLocation jumpToLocation = new TeleportLocation(
                player.level().dimension().location().toString(),
                targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
                player.getYRot(), player.getXRot(), "system"
            );
            
            int delayTicks = teleportDelay * 20;
            // /jumpto targets a block the player explicitly looked at — skip safety
            // re-scan so players can intentionally teleport to wall-adjacent spots.
            TeleportUtil.teleportPlayer(player, jumpToLocation, delayTicks, false).thenAccept(result -> {
                if (result.isSuccess()) {
                    player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.misc.jumpto_success"));
                    NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Player {} teleported to looking at: {}", player.getName().getString(), targetPos);
                } else {
                    player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.jumpto_failed", result.getMessage()));
                }
            });
            return true;
        }
        
        player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.no_block_in_sight"));
        return false;
    }
    
    /**
     * Random teleportation within configured bounds
     */
    public boolean randomTeleport(ServerPlayer player) {
        // Save current location as back location
        saveBackLocation(player);
        
        // Configuration for random teleport bounds
        int maxDistance = 1000; // Maximum distance from spawn/current location
        int attempts = 10; // Number of attempts to find safe location
        
        java.util.Random random = new java.util.Random();
        
        for (int attempt = 0; attempt < attempts; attempt++) {
            // Generate random coordinates
            int randomX = (int) player.getX() + random.nextInt(maxDistance * 2) - maxDistance;
            int randomZ = (int) player.getZ() + random.nextInt(maxDistance * 2) - maxDistance;
            
            // Find safe Y coordinate (highest solid block + 1)
            int maxY = com.zerog.neoessentials.util.LevelHeightCompat.maxBuildHeight(player.level()) - 1;
            for (int y = maxY; y >= com.zerog.neoessentials.util.LevelHeightCompat.minBuildHeight(player.level()); y--) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(randomX, y, randomZ);
                net.minecraft.core.BlockPos posAbove = pos.above();
                net.minecraft.core.BlockPos posAbove2 = posAbove.above();
                
                // Check if location is safe (solid ground, air above)
                if (!player.level().getBlockState(pos).isAir() && 
                    player.level().getBlockState(posAbove).isAir() && 
                    player.level().getBlockState(posAbove2).isAir()) {
                    
                    final int safeY = y + 1; // Make final for lambda
                    TeleportLocation randomLocation = new TeleportLocation(
                        player.level().dimension().location().toString(),
                        randomX + 0.5, safeY, randomZ + 0.5,
                        player.getYRot(), player.getXRot(), "system"
                    );
                    
                    int delayTicks = teleportDelay * 20;
                    TeleportUtil.teleportPlayer(player, randomLocation, delayTicks, true).thenAccept(result -> {
                        if (result.isSuccess()) {
                            player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.misc.tpr_success", randomX, safeY, randomZ));
                            NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Player {} randomly teleported to: {} {} {}", player.getName().getString(), randomX, safeY, randomZ);
                        } else {
                            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.tpr_failed", result.getMessage()));
                        }
                    });
                    return true;
                }
            }
        }
        
        player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.tpr_no_safe_location"));
        return false;
    }
    
    /**
     * Clear all data (for server shutdown)
     */
    public void clearAllData() {
        backHistory.clear();
        pendingDeathHints.clear();
        NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Cleared all misc teleport data");
    }

    /**
     * Reload configuration (called by dashboard teleport settings endpoint or /reload command).
     */
    public void reload() {
        NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Reloading MiscTeleportManager config...");
        loadConfig();
    }
}
