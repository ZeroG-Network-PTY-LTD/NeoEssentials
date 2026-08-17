package com.zerog.neoessentials.teleportation.Misc;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.teleportation.TeleportLocation;
import com.zerog.neoessentials.teleportation.TeleportUtil;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PlayerDataStore;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    // Storage for back locations (in-memory cache; backed by PlayerDataStore on disk)
    private final Map<UUID, TeleportLocation> backLocations = new ConcurrentHashMap<>();
    private final Map<UUID, TeleportLocation> deathLocations = new ConcurrentHashMap<>();

    // Timestamps (ms) for when each location type was last saved — used to pick the most recent one
    private final Map<UUID, Long> backLocationTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, Long> deathLocationTimestamps = new ConcurrentHashMap<>();

    // Persistent storage — survives server restarts
    private final PlayerDataStore dataStore = new PlayerDataStore("back_locations");
    
    // Configuration (loaded lazily from ConfigManager)
    private int maxBackHistory = 5;
    private int teleportDelay = 3;
    private boolean enableDeathBack = true;
    private boolean enableTeleportBack = true;
    private int backCooldownSeconds = 0; // 0 = no cooldown
    private boolean enableBackSafety = true; // teleportation.backSettings.enableBackSafety

    // Cooldown tracking for /back
    private final Map<UUID, Long> lastBackTimestamps = new ConcurrentHashMap<>();
    
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
            } catch (Exception ignored) {}
        NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "[MiscTeleportManager] Config loaded — warmup={}s, cooldown={}s, deathBack={}, teleportBack={}, backSafety={}",
            teleportDelay, backCooldownSeconds, enableDeathBack, enableTeleportBack, enableBackSafety);
        } catch (Exception e) {
            LOGGER.warn("Failed to load MiscTeleportManager config, using defaults: {}", e.getMessage());
        }
    }

    // ── Persistence helpers ───────────────────────────────────────────────────

    /**
     * Return the death location for {@code playerId}, loading from disk if not in memory.
     */
    private TeleportLocation loadDeathLocation(UUID playerId) {
        TeleportLocation inMemory = deathLocations.get(playerId);
        if (inMemory != null) return inMemory;

        JsonObject data = dataStore.load(playerId);
        if (data.has("deathLocation") && data.get("deathLocation").isJsonObject()) {
            TeleportLocation loc = TeleportLocation.fromJson(data.getAsJsonObject("deathLocation"));
            if (loc != null) {
                deathLocations.put(playerId, loc);
                // Restore persisted timestamp if available
                if (data.has("deathLocationTs")) {
                    deathLocationTimestamps.put(playerId, data.get("deathLocationTs").getAsLong());
                }
            }
            return loc;
        }
        return null;
    }

    /**
     * Return the back location for {@code playerId}, loading from disk if not in memory.
     */
    private TeleportLocation loadBackLocation(UUID playerId) {
        TeleportLocation inMemory = backLocations.get(playerId);
        if (inMemory != null) return inMemory;

        JsonObject data = dataStore.load(playerId);
        if (data.has("backLocation") && data.get("backLocation").isJsonObject()) {
            TeleportLocation loc = TeleportLocation.fromJson(data.getAsJsonObject("backLocation"));
            if (loc != null) {
                backLocations.put(playerId, loc);
                // Restore persisted timestamp if available
                if (data.has("backLocationTs")) {
                    backLocationTimestamps.put(playerId, data.get("backLocationTs").getAsLong());
                }
            }
            return loc;
        }
        return null;
    }

    /**
     * Persist the current in-memory back + death locations for {@code playerId} to disk.
     */
    private void persistLocations(UUID playerId) {
        try {
            JsonObject data = dataStore.load(playerId);
            TeleportLocation backLoc  = backLocations.get(playerId);
            TeleportLocation deathLoc = deathLocations.get(playerId);

            if (backLoc != null) {
                data.add("backLocation", backLoc.toJson());
                Long ts = backLocationTimestamps.get(playerId);
                if (ts != null) data.addProperty("backLocationTs", ts);
            } else {
                data.remove("backLocation");
                data.remove("backLocationTs");
            }
            if (deathLoc != null) {
                data.add("deathLocation", deathLoc.toJson());
                Long ts = deathLocationTimestamps.get(playerId);
                if (ts != null) data.addProperty("deathLocationTs", ts);
            } else {
                data.remove("deathLocation");
                data.remove("deathLocationTs");
            }
            dataStore.save(playerId, data);
        } catch (Exception e) {
            LOGGER.error("Failed to persist back/death locations for {}: {}", playerId, e.getMessage());
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Save a player's current location as their back location.
     * Also records a timestamp so that /back always uses whichever location
     * (death or teleport) was saved most recently — matching Essentials' behaviour.
     */
    public void saveBackLocation(ServerPlayer player) {
        if (!enableTeleportBack) {
            return;
        }
        
        UUID playerId = player.getUUID();
        TeleportLocation backLocation = new TeleportLocation(player);
        
        backLocations.put(playerId, backLocation);
        backLocationTimestamps.put(playerId, System.currentTimeMillis());
        persistLocations(playerId);

        NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "Saved back location for {}: {}",
                    player.getName().getString(), backLocation);
    }
    
    /**
     * Save a player's death location.
     * Also records a timestamp so /back picks whichever location was saved most recently.
     */
    public void saveDeathLocation(ServerPlayer player) {
        if (!enableDeathBack) {
            return;
        }
        
        UUID playerId = player.getUUID();
        TeleportLocation deathLocation = new TeleportLocation(player);
        
        deathLocations.put(playerId, deathLocation);
        deathLocationTimestamps.put(playerId, System.currentTimeMillis());
        persistLocations(playerId);

        // Do NOT send a chat message here — LivingDeathEvent fires while the player is
        // transitioning to the death screen, so they cannot read it.  The "use /back to
        // return to death location" hint is sent on respawn instead (see onPlayerRespawn).
        NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Saved death location for {}: {}", 
                   player.getName().getString(), deathLocation);
    }
    
    /**
     * Teleport player back to their previous location or death location.
     *
     * <p>Uses the <em>most recently saved</em> location — either a death point
     * (saved on death) or a teleport origin (saved by /tp, /tpa, /home, etc.).
     * This mirrors Essentials' behaviour where {@code setLastLocation()} is called
     * by both the death listener and the teleport listener, and whichever happened
     * last is the one used by {@code /back}.</p>
     *
     * <p>Disk-backed: loads persisted locations if not in memory so /back
     * continues to work after a server restart.</p>
     */
    public boolean teleportBack(ServerPlayer player) {
        UUID playerId = player.getUUID();
        NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "teleportBack request: player={}", player.getName().getString());
        // Load from disk if not in memory (survives server restarts)
        TeleportLocation deathLocation = loadDeathLocation(playerId);
        TeleportLocation backLocation  = loadBackLocation(playerId);
        TeleportLocation targetLocation;
        final boolean usedDeath;

        // Pick the most recently saved location. If timestamps are missing (e.g.
        // loaded from a pre-fix save) fall back to death-first for safety.
        long deathTs = deathLocationTimestamps.getOrDefault(playerId, 0L);
        long backTs  = backLocationTimestamps.getOrDefault(playerId, 0L);

        if (deathLocation != null && (backLocation == null || deathTs >= backTs)) {
            targetLocation = deathLocation;
            usedDeath = true;
        } else if (backLocation != null) {
            targetLocation = backLocation;
            usedDeath = false;
        } else {
            NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "teleportBack: {} has no back or death location", player.getName().getString());
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
                    return false;
                }
                lastBackTimestamps.put(playerId, now);
            }
        }

        // Capture current location BEFORE teleporting — this becomes the new back
        // location so the player can /back again to undo the /back.
        TeleportLocation currentLocation = new TeleportLocation(player);
        final TeleportLocation finalTargetLocation = targetLocation;

        // Snapshot timestamp of the back location we consumed — used later to detect
        // whether another teleport overwrote the back slot while we were warming up.
        // If another teleport DID save a newer location during warmup, we still store
        // the /back-undo position but we do NOT discard the newer back entry that was
        // saved by the intervening teleport (both are important for the chain).
        final long backTsAtDispatch = backLocationTimestamps.getOrDefault(playerId, 0L);
        final long deathTsAtDispatch = deathLocationTimestamps.getOrDefault(playerId, 0L);

        // Bypass warmup for players with the permission
        boolean bypassWarmup = com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(playerId, "neoessentials.teleport.bypass.warmup")
            || com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(playerId, "neoessentials.teleport.back.bypass.warmup");
        int delayTicks = bypassWarmup ? 0 : teleportDelay * 20;
        if (delayTicks > 0) {
            player.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.misc.back_warmup", teleportDelay));
        }
        TeleportUtil.teleportPlayer(player, finalTargetLocation, delayTicks, enableBackSafety).thenAccept(result -> {
            if (result.isSuccess()) {
                // Check if another teleport updated the back slot during our warmup.
                boolean interveningTeleport =
                    backLocationTimestamps.getOrDefault(playerId, 0L) != backTsAtDispatch
                    || deathLocationTimestamps.getOrDefault(playerId, 0L) != deathTsAtDispatch;

                if (!interveningTeleport) {
                    // Normal case: store where player was (undo-/back position)
                    backLocations.put(playerId, currentLocation);
                    backLocationTimestamps.put(playerId, System.currentTimeMillis());
                } else {
                    // Another teleport ran during warmup (e.g. player accepted a TPA
                    // from someone else).  That teleport already saved the correct back
                    // location.  We still want the player to be able to undo THIS /back,
                    // so we save currentLocation at a timestamp just ONE millisecond
                    // older than the intervening save so it won't overshadow it.
                    long interveningTs = Math.max(
                        backLocationTimestamps.getOrDefault(playerId, 0L),
                        deathLocationTimestamps.getOrDefault(playerId, 0L));
                    backLocations.put(playerId, currentLocation);
                    backLocationTimestamps.put(playerId, interveningTs - 1);
                    NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "Player {} had an intervening teleport during /back warmup; undo-back stored with prior-timestamp.",
                        player.getName().getString());
                }

                if (usedDeath) {
                    // Clear the death location after a successful /back to it
                    deathLocations.remove(playerId);
                    deathLocationTimestamps.remove(playerId);
                    player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.misc.death_teleport_success"));
                } else {
                    player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.misc.back_success"));
                }
                persistLocations(playerId);
                NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Player {} teleported back to {}", player.getName().getString(), finalTargetLocation);
            } else {
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
     * Teleport player to their death location
     */
    public boolean teleportToDeathLocation(ServerPlayer player) {
        UUID playerId = player.getUUID();
        NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "teleportToDeathLocation request: player={}", player.getName().getString());

        // Load from disk if not in memory (survives server restarts)
        TeleportLocation deathLocation = loadDeathLocation(playerId);
        if (deathLocation == null) {
            NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "teleportToDeathLocation: {} has no death location", player.getName().getString());
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.no_death_location"));
            return false;
        }
        
        // Save current location as back location (with timestamp)
        backLocations.put(playerId, new TeleportLocation(player));
        backLocationTimestamps.put(playerId, System.currentTimeMillis());
        
        // Perform the teleport
        int delayTicks = teleportDelay * 20;
        TeleportUtil.teleportPlayer(player, deathLocation, delayTicks, enableBackSafety).thenAccept(result -> {
            if (result.isSuccess()) {
                player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.misc.death_teleport_success"));
                
                // Clear death location after successful teleport
                deathLocations.remove(playerId);
                deathLocationTimestamps.remove(playerId);
                persistLocations(playerId);
                
                NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Player {} teleported to death location: {}", 
                           player.getName().getString(), deathLocation);
            } else {
                player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.death_teleport_failed", result.getMessage()));
                
                LOGGER.warn("Failed death teleport for {}: {}", 
                           player.getName().getString(), result.getMessage());
            }
        });
        
        return true;
    }
    
    /**
     * Clear a player's back location (memory + disk)
     */
    public void clearBackLocation(ServerPlayer player) {
        UUID playerId = player.getUUID();
        backLocations.remove(playerId);
        deathLocations.remove(playerId);
        backLocationTimestamps.remove(playerId);
        deathLocationTimestamps.remove(playerId);
        dataStore.delete(playerId);
        
        NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "Cleared back locations for {}", player.getName().getString());
    }
    
    /**
     * Check if player has a back location (checks disk if not in memory)
     */
    public boolean hasBackLocation(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (backLocations.containsKey(playerId) || deathLocations.containsKey(playerId)) {
            return true;
        }
        // Check disk
        JsonObject data = dataStore.load(playerId);
        return data.has("backLocation") || data.has("deathLocation");
    }
    
    /**
     * Get back location info for a player
     */
    public String getBackLocationInfo(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        TeleportLocation backLocation = loadBackLocation(playerId);
        if (backLocation != null) {
            return MessageUtil.localize("commands.neoessentials.teleport.misc.back_info", 
                                       backLocation.getWorldName(), 
                                       String.format("%.1f %.1f %.1f", backLocation.getX(), backLocation.getY(), backLocation.getZ()));
        }
        
        TeleportLocation deathLocation = loadDeathLocation(playerId);
        if (deathLocation != null) {
            return MessageUtil.localize("commands.neoessentials.teleport.misc.death_info", 
                                       deathLocation.getWorldName(), 
                                       String.format("%.1f %.1f %.1f", deathLocation.getX(), deathLocation.getY(), deathLocation.getZ()));
        }
        
        return MessageUtil.localize("commands.neoessentials.teleport.misc.no_back_location");
    }
    
    /**
     * Handle player disconnect - no-op since data is persisted to disk
     */
    public void onPlayerDisconnect(ServerPlayer player) {
        NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "Player {} disconnected; back/death locations are persisted to disk.", player.getName().getString());
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
        if (mgr.deathLocations.containsKey(playerId) || mgr.deathLocationTimestamps.containsKey(playerId)) {
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
        return String.format("MiscTeleport Statistics: %d back locations (in-memory), %d death locations (in-memory), delay=%ds", 
                           backLocations.size(), deathLocations.size(), teleportDelay);
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
        dataStore.flushAll();
        backLocations.clear();
        deathLocations.clear();
        backLocationTimestamps.clear();
        deathLocationTimestamps.clear();
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
