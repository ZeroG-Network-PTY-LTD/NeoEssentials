package com.zerog.neoessentials.pvp;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-wide and per-player PvP control (PvPManager-plugin-style port).
 *
 * <p>Three independent gates all have to pass for {@code attacker} to be allowed to hit
 * {@code target} — see {@link #canFight(ServerPlayer, ServerPlayer)}:
 * <ol>
 *   <li>The global switch ({@link #isGlobalPvpEnabled()}, toggled via {@code /pvp on|off|toggle}, persisted)</li>
 *   <li>Neither player has opted themselves out via {@code /pvp toggle} (if
 *       {@code pvp.allowPerPlayerToggle} is enabled)</li>
 *   <li>Neither player is within their newbie-protection window, and neither is standing in a
 *       configured safe zone</li>
 * </ol>
 *
 * <p>Newbie protection and the opt-out toggle are BOTH mutual: a protected/opted-out player can
 * neither be hit by nor deal damage to another player, so neither can be used to attack
 * risk-free.
 */
public class PvpManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PvpManager.class);
    private static final String PLAYER_COLLECTION = "pvp_players";
    private static final String GLOBAL_COLLECTION = "pvp_global";
    private static final String GLOBAL_ID = "state";

    /** Minimum time between "PvP is disabled" messages to the same attacker, to avoid chat spam. */
    private static final long NOTIFY_COOLDOWN_MS = 2_000L;

    private static class SingletonHolder {
        private static final PvpManager INSTANCE = new PvpManager();
    }

    public static PvpManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private final DataStore store = StorageManager.getInstance().getStore();

    private volatile boolean globalPvpEnabled;
    private final ConcurrentHashMap<UUID, Boolean> optedOutCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> firstSeenCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastNotifyCache = new ConcurrentHashMap<>();

    private PvpManager() {
        loadGlobalState();
        loadPlayerStates();
    }

    // ── Global toggle ────────────────────────────────────────────────────────

    public boolean isGlobalPvpEnabled() {
        return globalPvpEnabled;
    }

    public void setGlobalPvpEnabled(boolean enabled) {
        this.globalPvpEnabled = enabled;
        saveGlobalState();
        NeoLog.info(LOGGER, LogCategory.GENERAL, "[PvP] Global PvP {}", enabled ? "enabled" : "disabled");
    }

    private void loadGlobalState() {
        JsonObject root = store.get(GLOBAL_COLLECTION, GLOBAL_ID);
        globalPvpEnabled = (root != null && root.has("enabled"))
            ? root.get("enabled").getAsBoolean()
            : ConfigManager.isPvpEnabledByDefault();
    }

    private void saveGlobalState() {
        JsonObject obj = new JsonObject();
        obj.addProperty("enabled", globalPvpEnabled);
        store.put(GLOBAL_COLLECTION, GLOBAL_ID, obj);
    }

    // ── Per-player opt-out ───────────────────────────────────────────────────

    public boolean isOptedOut(UUID uuid) {
        return optedOutCache.getOrDefault(uuid, false);
    }

    /**
     * Sets a player's own PvP opt-out flag. Returns {@code false} without changing anything if
     * {@code pvp.allowPerPlayerToggle} is disabled — callers should surface that as a permission
     * error to the player.
     */
    public boolean setOptedOut(UUID uuid, boolean optedOut) {
        if (!ConfigManager.isPvpPerPlayerToggleAllowed()) return false;
        optedOutCache.put(uuid, optedOut);
        savePlayerState(uuid);
        return true;
    }

    // ── Newbie protection ────────────────────────────────────────────────────

    /** Records the first time this system has ever seen {@code uuid} — call on player join. */
    public void recordFirstSeenIfAbsent(UUID uuid) {
        if (!firstSeenCache.containsKey(uuid)) {
            firstSeenCache.put(uuid, System.currentTimeMillis());
            savePlayerState(uuid);
        }
    }

    /** Seconds of newbie-protection immunity remaining for {@code uuid}, or 0 if none/expired. */
    public int getNewbieProtectionRemainingSeconds(UUID uuid) {
        if (!ConfigManager.isPvpNewbieProtectionEnabled()) return 0;
        Long firstSeen = firstSeenCache.get(uuid);
        if (firstSeen == null) return 0;
        long durationMs = ConfigManager.getPvpNewbieProtectionDurationSeconds() * 1000L;
        long remaining = (firstSeen + durationMs) - System.currentTimeMillis();
        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }

    public boolean isNewbieProtected(UUID uuid) {
        return getNewbieProtectionRemainingSeconds(uuid) > 0;
    }

    // ── Safe zones (reuses the YAWP protectedAreas list also used for teleport blocking) ───

    /**
     * Returns {@code true} if {@code player} is standing inside one of the regions listed in
     * {@code teleportation.generalSettings.protectedAreas} — mirrors the exact YAWP reflection
     * lookup {@code TeleportUtil} already uses, so PvP and teleport protection stay in sync off
     * a single shared region list. Silently returns {@code false} if YAWP isn't installed.
     */
    public boolean isInSafeZone(ServerPlayer player) {
        if (!ConfigManager.isPvpSafeZoneIntegrationEnabled()) return false;
        List<String> protectedAreas = ConfigManager.getProtectedAreas();
        if (protectedAreas == null || protectedAreas.isEmpty()) return false;

        try {
            Class<?> yawpApiClass = Class.forName("net.yawp.api.YawpAPI");
            Object yawpApi = yawpApiClass.getMethod("getInstance").invoke(null);
            net.minecraft.server.level.ServerLevel level = com.zerog.neoessentials.util.LevelCompat.of(player);
            List<?> regions = (List<?>) yawpApiClass.getMethod("getRegionsAt",
                    net.minecraft.server.level.ServerLevel.class, double.class, double.class, double.class)
                .invoke(yawpApi, level, player.getX(), player.getY(), player.getZ());
            if (regions != null) {
                for (Object region : regions) {
                    String regionName = (String) region.getClass().getMethod("getName").invoke(region);
                    if (protectedAreas.contains(regionName)) return true;
                }
            }
        } catch (ClassNotFoundException e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "[PvP] YAWP not installed, skipping safe-zone check", e);
        } catch (Exception e) {
            LOGGER.error("[PvP] Error checking YAWP safe zones: {}", e.getMessage(), e);
        }
        return false;
    }

    // ── Combined check ───────────────────────────────────────────────────────

    /** Returns {@code true} if {@code attacker} is currently allowed to deal PvP damage to {@code target}. */
    public boolean canFight(ServerPlayer attacker, ServerPlayer target) {
        if (!ConfigManager.isPvpModuleEnabled()) return true;
        if (!globalPvpEnabled) return false;

        if (ConfigManager.isPvpPerPlayerToggleAllowed()
                && (isOptedOut(attacker.getUUID()) || isOptedOut(target.getUUID()))) {
            return false;
        }

        if (isNewbieProtected(attacker.getUUID()) || isNewbieProtected(target.getUUID())) {
            return false;
        }

        return !isInSafeZone(attacker) && !isInSafeZone(target);
    }

    /**
     * Rate-limits the "PvP is disabled" notification per attacker, so holding down attack
     * against a protected target can't spam their chat.
     */
    public boolean shouldNotify(UUID attackerUuid) {
        long now = System.currentTimeMillis();
        Long last = lastNotifyCache.get(attackerUuid);
        if (last != null && now - last < NOTIFY_COOLDOWN_MS) return false;
        lastNotifyCache.put(attackerUuid, now);
        return true;
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    private void loadPlayerStates() {
        for (Map.Entry<String, JsonObject> e : store.getAll(PLAYER_COLLECTION).entrySet()) {
            try {
                UUID uuid = UUID.fromString(e.getKey());
                JsonObject o = e.getValue();
                if (o.has("optedOut")) optedOutCache.put(uuid, o.get("optedOut").getAsBoolean());
                if (o.has("firstSeenMs")) firstSeenCache.put(uuid, o.get("firstSeenMs").getAsLong());
            } catch (Exception ex) {
                LOGGER.error("[PvP] Failed to load player state entry {}: {}", e.getKey(), ex.getMessage());
            }
        }
    }

    private void savePlayerState(UUID uuid) {
        JsonObject o = new JsonObject();
        o.addProperty("optedOut", optedOutCache.getOrDefault(uuid, false));
        Long firstSeen = firstSeenCache.get(uuid);
        if (firstSeen != null) o.addProperty("firstSeenMs", firstSeen);
        store.put(PLAYER_COLLECTION, uuid.toString(), o);
    }

    /** Re-reads global/per-player state from storage (config-derived values are read live elsewhere). */
    public void reload() {
        NeoLog.info(LOGGER, LogCategory.GENERAL, "[PvP] Reloading PvP system...");
        loadGlobalState();
        optedOutCache.clear();
        firstSeenCache.clear();
        loadPlayerStates();
        NeoLog.info(LOGGER, LogCategory.GENERAL, "[PvP] PvP system reloaded: global={}", globalPvpEnabled);
    }
}
