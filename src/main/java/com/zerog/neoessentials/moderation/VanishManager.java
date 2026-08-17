package com.zerog.neoessentials.moderation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player vanish system for staff invisibility
 */
public class VanishManager {
    // Recognized vanish-priority tiers, checked highest-first — mirrors the tiered-permission
    // pattern used for sell-multiplier/max-balance/pay-limit nodes in PermissionBasedModifiers.
    // Node: neoessentials.vanish.priority.<tier>. Higher tier = more senior (opposite of the
    // old hardcoded group-name scheme this replaced, which used LOWER numbers for higher rank).
    private static final int[] VANISH_PRIORITY_TIERS = {1000, 500, 250, 100, 50, 25, 10, 5, 1};

    /**
     * A player's vanish-visibility priority, purely from permission nodes
     * ({@code neoessentials.vanish.priority.<tier>}) — no hardcoded group-name mapping anymore.
     * Defaults to {@code 0} (no special vanish visibility) when no tier is granted. Equal or
     * higher beats lower: see {@link #canViewerSeeVanishedPlayer} / {@link #hidePlayerFromOthers}.
     *
     * <p>Uses {@link com.zerog.neoessentials.api.permissions.PermissionAPI#hasPermissionStrict}
     * rather than the normal OP-bypassing check: this is a graded, opt-in tier (same reasoning
     * as the economy-modifier tiers in {@code PermissionBasedModifiers}) meant to be granted
     * deliberately, not handed to every OP automatically. It's also excluded from ordinary
     * wildcard matching in {@code PermissionManager} — a bare {@code neoessentials.*} on a
     * group does not grant a vanish-priority tier, only an exact node or a wildcard scoped to
     * {@code neoessentials.vanish.priority.*} does.
     */
    public int getPlayerPriority(UUID playerId) {
        for (int tier : VANISH_PRIORITY_TIERS) {
            if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermissionStrict(playerId,
                    "neoessentials.vanish.priority." + tier)) {
                return tier;
            }
        }
        return 0;
    }
    private static final Logger LOGGER = LoggerFactory.getLogger(VanishManager.class);
    private static VanishManager instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String COLLECTION = "vanishes";
    private final com.zerog.neoessentials.storage.DataStore store;

    // In-memory cache for quick lookups
    // Vanished players and their priority (persisted to DataStore — see COLLECTION)
    private final Map<UUID, Integer> vanishedPlayers = new ConcurrentHashMap<>();
    // Players who can see vanished and their priority. Purely in-memory/session-scoped —
    // this is a runtime toggle (like vanilla /vanish's "see vanished" mode), not a durable
    // record, so it does not persist across restarts.
    private final Map<UUID, Integer> viewerPriorities = new ConcurrentHashMap<>();
    
    public static class VanishEntry {
        public String playerName;
        public UUID playerId;
        public String vanishedBy;
        public long vanishTime;
        public boolean selfVanish;
        
        public VanishEntry(String playerName, UUID playerId, String vanishedBy, boolean selfVanish) {
            this.playerName = playerName;
            this.playerId = playerId;
            this.vanishedBy = vanishedBy;
            this.selfVanish = selfVanish;
            this.vanishTime = System.currentTimeMillis();
        }
        
        public String getFormattedVanishTime() {
            return formatTime(vanishTime);
        }
    }
    
    private VanishManager() {
        this.store = com.zerog.neoessentials.storage.StorageManager.getInstance().getStore();
        migrateLegacyFilesIfNeeded();
        loadData();
    }
    
    public static VanishManager getInstance() {
        if (instance == null) {
            instance = new VanishManager();
        }
        return instance;
    }
    
    /**
     * Vanish a player
     */
    public boolean vanishPlayer(UUID playerId, String playerName, String vanishedBy, boolean selfVanish) {
        if (isPlayerVanished(playerId)) {
            return false; // Already vanished
        }
        // Default priority for vanished player (can be customized)
        int vanishPriority = getPlayerPriority(playerId);
        vanishedPlayers.put(playerId, vanishPriority);
        store.put(COLLECTION, playerId.toString(), toJson(playerId, vanishPriority));
        NeoLog.debug(LOGGER, LogCategory.MODERATION, "Applying vanish: player={} ({}) by={} self={} priority={}",
            playerName, playerId, vanishedBy, selfVanish, vanishPriority);

        // Hide player from others
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer vanishedPlayer = server.getPlayerList().getPlayer(playerId);
            if (vanishedPlayer != null) {
                hidePlayerFromOthers(vanishedPlayer);
                // Suppresses the vanished player's own footstep/splash/etc. sounds — these are
                // broadcast by vanilla based on position to every nearby player regardless of
                // entity visibility (Entity.playSound() isn't gated by the remove-entity packets
                // above at all), so without this a vanished player could still be heard running
                // around even though they were invisible.
                vanishedPlayer.setSilent(true);
                clearExistingMobTargets(vanishedPlayer);

                // Don't send message here - let the command handle it
                // to avoid duplicate messages
            }
        }

        if (com.zerog.neoessentials.config.ConfigManager.isLogVanishActionsEnabled()) {
            NeoLog.info(LOGGER, LogCategory.MODERATION, "Player {} ({}) vanished by {}", playerName, playerId, vanishedBy);
        }
        return true;
    }

    /**
     * Clears any hostile mob's existing target if it's this newly-vanished player — the
     * {@code LivingChangeTargetEvent} cancellation in {@code ModerationEventHandler} only stops
     * mobs from newly ACQUIRING a vanished player as a target; a mob that was already chasing
     * this player before they vanished would otherwise keep right on attacking.
     */
    private void clearExistingMobTargets(ServerPlayer vanishedPlayer) {
        if (!(vanishedPlayer.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        for (net.minecraft.world.entity.Mob mob : level.getEntitiesOfClass(
                net.minecraft.world.entity.Mob.class, vanishedPlayer.getBoundingBox().inflate(64.0))) {
            if (mob.getTarget() == vanishedPlayer) {
                mob.setTarget(null);
            }
        }
    }
    
    /**
     * Unvanish a player
     */
    public boolean unvanishPlayer(UUID playerId) {
        if (vanishedPlayers.remove(playerId) == null) {
            return false; // Not vanished
        }
        store.delete(COLLECTION, playerId.toString());
        NeoLog.debug(LOGGER, LogCategory.MODERATION, "Removing vanish for player {}", playerId);

        // Show player to others
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer unvanishedPlayer = server.getPlayerList().getPlayer(playerId);
            if (unvanishedPlayer != null) {
                showPlayerToOthers(unvanishedPlayer);
                unvanishedPlayer.setSilent(false);

                // Don't send message here - let the command handle it
                // to avoid duplicate messages
            }
        }

        if (com.zerog.neoessentials.config.ConfigManager.isLogVanishActionsEnabled()) {
            NeoLog.info(LOGGER, LogCategory.MODERATION, "Player ({}) unvanished", playerId);
        }
        return true;
    }
    
    /**
     * Toggle vanish for a player
     */
    public boolean toggleVanish(UUID playerId, String playerName, String toggledBy) {
        if (isPlayerVanished(playerId)) {
            return unvanishPlayer(playerId);
        } else {
            return vanishPlayer(playerId, playerName, toggledBy, toggledBy.equals(playerName));
        }
    }
    
    /**
     * Enable see vanished for a player
     */
    public void enableSeeVanished(UUID playerId) {
    // Default priority for viewer (can be customized)
    int viewerPriority = getPlayerPriority(playerId);
    viewerPriorities.put(playerId, viewerPriority);
        
        // Show all vanished players to this player
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer observer = server.getPlayerList().getPlayer(playerId);
            if (observer != null) {
                for (UUID vanishedId : vanishedPlayers.keySet()) {
                    ServerPlayer vanishedPlayer = server.getPlayerList().getPlayer(vanishedId);
                    if (vanishedPlayer != null && !vanishedId.equals(playerId)) {
                        showPlayerToSpecific(vanishedPlayer, observer);
                    }
                }
            }
        }
        
        if (com.zerog.neoessentials.config.ConfigManager.isLogVanishActionsEnabled()) {
            NeoLog.info(LOGGER, LogCategory.MODERATION, "Player ({}) enabled see vanished", playerId);
        }
    }
    
    /**
     * Disable see vanished for a player
     */
    public void disableSeeVanished(UUID playerId) {
    viewerPriorities.remove(playerId);
        
        // Hide all vanished players from this player
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer observer = server.getPlayerList().getPlayer(playerId);
            if (observer != null) {
                for (UUID vanishedId : vanishedPlayers.keySet()) {
                    ServerPlayer vanishedPlayer = server.getPlayerList().getPlayer(vanishedId);
                    if (vanishedPlayer != null && !vanishedId.equals(playerId)) {
                        hidePlayerFromSpecific(vanishedPlayer, observer);
                    }
                }
            }
        }
        
        if (com.zerog.neoessentials.config.ConfigManager.isLogVanishActionsEnabled()) {
            NeoLog.info(LOGGER, LogCategory.MODERATION, "Player ({}) disabled see vanished", playerId);
        }
    }
    
    /**
     * Toggle see vanished for a player
     */
    public boolean toggleSeeVanished(UUID playerId) {
        if (viewerPriorities.containsKey(playerId)) {
            disableSeeVanished(playerId);
            return false;
        } else {
            enableSeeVanished(playerId);
            return true;
        }
    }
    
    /**
     * Check if a player is vanished
     */
    public boolean isPlayerVanished(UUID playerId) {
    return vanishedPlayers.containsKey(playerId);
    }
    
    /**
     * Check if a player can see vanished players (in general — ignores rank).
     * Prefer {@link #canViewerSeeVanishedPlayer(UUID, UUID)} when checking visibility of a
     * SPECIFIC vanished player, since that also enforces the priority-rank rule this method
     * ignores (see {@link #hidePlayerFromOthers} — a viewer must be both registered as a
     * see-vanished viewer AND have sufficient rank relative to the vanished player).
     */
    public boolean canPlayerSeeVanished(UUID playerId) {
    return viewerPriorities.containsKey(playerId);
    }

    /**
     * Whether {@code viewerId} can actually see {@code vanishedId} specifically, applying the
     * same priority-rank rule {@link #hidePlayerFromOthers} uses for the real entity
     * visibility. This is the single source of truth for "can this viewer see this vanished
     * player" — commands like {@code /list}/{@code /near} previously each had their own,
     * mutually inconsistent notion of this (one checked only the permission node, ignoring
     * whether the viewer had actually toggled see-vanished on; another checked map membership
     * but ignored the rank comparison), which could disagree with what the viewer's client is
     * actually rendering.
     */
    public boolean canViewerSeeVanishedPlayer(UUID viewerId, UUID vanishedId) {
        if (!isPlayerVanished(vanishedId)) return true;
        if (viewerId.equals(vanishedId)) return true;
        int vanishedPriority = vanishedPlayers.getOrDefault(vanishedId, 0);
        // Equal priority sees each other too (same group/tier counts as peers), not just
        // strictly higher — priority 0 (no tier granted at all) is excluded either way, so two
        // players with no vanish-priority permission at all still can't see one another.
        Integer toggledPriority = viewerPriorities.get(viewerId);
        if (toggledPriority != null && toggledPriority > 0 && toggledPriority >= vanishedPriority) return true;
        // Automatic, toggle-independent visibility: a high-enough vanish-priority permission
        // node (neoessentials.vanish.priority.<tier>) sees equal-or-lower-priority vanished
        // players without needing to run /vanish see at all.
        int autoPriority = getPlayerPriority(viewerId);
        return autoPriority > 0 && autoPriority >= vanishedPriority;
    }
    
    /**
     * Get all vanished players
     */
    public Set<UUID> getVanishedPlayers() {
    return new HashSet<>(vanishedPlayers.keySet());
    }
    
    /**
     * Get all players who can see vanished
     */
    public Set<UUID> getCanSeeVanished() {
    return new HashSet<>(viewerPriorities.keySet());
    }
    
    /**
     * Handle player join - set up vanish state.
     *
     * Packets are deferred by 1 tick so that vanilla entity-spawn packets sent
     * during the login sequence (ChunkMap entity tracking) arrive on the client
     * BEFORE our remove/add overrides.  Without the delay our packets can be
     * overwritten by the subsequent vanilla spawn packet, causing vanished
     * players to remain visible to the joining observer.
     */
    public void onPlayerJoin(ServerPlayer player) {
        UUID playerId = player.getUUID();
        MinecraftServer server = player.level().getServer();

        // If this player is vanished, hide them from every other online player
        // (use a 1-tick delay so our hide packet arrives after the vanilla spawn
        // packets that the server sends while placing the new player in the world).
        if (isPlayerVanished(playerId)) {
            player.sendSystemMessage(MessageUtil.info(
                MessageUtil.localize("neoessentials.moderation.vanish_reminder")));
            player.setSilent(true);
            if (server != null) {
                com.zerog.neoessentials.scheduler.DelayedTaskScheduler.schedule(1,
                    () -> hidePlayerFromOthers(player));
            }
        }

        // After 1 tick, handle what the joining player should (or should not) see.
        if (server != null) {
            com.zerog.neoessentials.scheduler.DelayedTaskScheduler.schedule(1, () -> {
                for (UUID vanishedId : new HashSet<>(vanishedPlayers.keySet())) {
                    if (vanishedId.equals(playerId)) continue;
                    ServerPlayer vanishedPlayer = server.getPlayerList().getPlayer(vanishedId);
                    if (vanishedPlayer == null) continue;
                    if (canViewerSeeVanishedPlayer(playerId, vanishedId)) {
                        showPlayerToSpecific(vanishedPlayer, player);
                    } else {
                        hidePlayerFromSpecific(vanishedPlayer, player);
                    }
                }
            });
        }
    }

    /**
     * Handle player leave - cleanup vanish state
     */
    public void onPlayerLeave(ServerPlayer player) {
        // No special handling needed on leave for vanish system
        // Vanish state persists across sessions
    }
    
    /**
     * Hide a player from all other players (except those who can see vanished).
     *
     * Previously this method returned early when isHideFromTabListEnabled() was
     * false, meaning the player's entity was NEVER removed from the world — only
     * the tab-list removal was ever attempted.  Now the entity is always removed
     * from every observer who does not have see-vanished permission, and the
     * tab-list removal is performed conditionally on the config flag.
     *
     * Priority rule (see {@link #canViewerSeeVanishedPlayer}, the single source of truth this
     * delegates to): an observer sees the vanished player only if they've toggled see-vanished
     * on with sufficient rank, or automatically via a high-enough vanish-priority permission
     * node — either way, equal-or-higher priority than the vanished player's (never priority 0).
     */
    private void hidePlayerFromOthers(ServerPlayer vanishedPlayer) {
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        UUID vanishedId = vanishedPlayer.getUUID();

        for (ServerPlayer otherPlayer : server.getPlayerList().getPlayers()) {
            if (otherPlayer == vanishedPlayer) continue;
            if (!canViewerSeeVanishedPlayer(otherPlayer.getUUID(), vanishedId)) {
                hidePlayerFromSpecific(vanishedPlayer, otherPlayer);
            }
        }
    }
    
    /**
     * Show a player to all other players
     */
    private void showPlayerToOthers(ServerPlayer unvanishedPlayer) {
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        
        for (ServerPlayer otherPlayer : server.getPlayerList().getPlayers()) {
            if (otherPlayer != unvanishedPlayer) {
                showPlayerToSpecific(unvanishedPlayer, otherPlayer);
            }
        }
    }
    
    /**
     * Hide a specific player from a specific observer.
     *
     * Previously this ONLY sent ClientboundPlayerInfoRemovePacket (tab-list removal).
     * The player's entity was never actually removed from the observer's world —
     * meaning players could still see the vanished player walking around even
     * though they were off the tab list.
     *
     * Fix: also send ClientboundRemoveEntitiesPacket so the entity disappears from
     * the world.  Tab-list removal is still config-gated; entity removal is not.
     */
    private void hidePlayerFromSpecific(ServerPlayer vanishedPlayer, ServerPlayer observer) {
        try {
            // Remove from tab list if configured to do so
            if (com.zerog.neoessentials.config.ConfigManager.isHideFromTabListEnabled()) {
                observer.connection.send(new ClientboundPlayerInfoRemovePacket(
                    List.of(vanishedPlayer.getUUID())
                ));
            }
            // Always remove the entity from the observer's world view — this is
            // the actual "invisible" part that was previously missing entirely.
            observer.connection.send(new ClientboundRemoveEntitiesPacket(
                vanishedPlayer.getId()
            ));
        } catch (Exception e) {
            LOGGER.error("Failed to hide player {} from {}", vanishedPlayer.getName().getString(), observer.getName().getString(), e);
        }
    }
    
    /**
     * Show a specific player to a specific observer.
     *
     * Previously this method contained only a comment and never sent any packets,
     * making it completely non-functional.  Unvanishing therefore never worked for
     * observers who were already online, and see-vanished staff joining the server
     * could never actually see vanished players.
     *
     * Fix: send the full set of packets needed to restore the player in the
     * observer's world: tab-list update → entity spawn → entity data → equipment
     * → head rotation.
     */
    private void showPlayerToSpecific(ServerPlayer unvanishedPlayer, ServerPlayer observer) {
        try {
            // 1. Re-add player to the tab list (safe to send even if never removed)
            observer.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(
                List.of(unvanishedPlayer)
            ));

            // 2. Re-spawn the player entity in the observer's world
            observer.connection.send(new ClientboundAddEntityPacket(
                unvanishedPlayer.getId(),
                unvanishedPlayer.getUUID(),
                unvanishedPlayer.getX(),
                unvanishedPlayer.getY(),
                unvanishedPlayer.getZ(),
                unvanishedPlayer.getXRot(),
                unvanishedPlayer.getYRot(),
                EntityType.PLAYER,
                0,
                unvanishedPlayer.getDeltaMovement(),
                unvanishedPlayer.getYHeadRot()
            ));

            // 3. Send entity metadata (skin flags, display name visibility, etc.)
            List<net.minecraft.network.syncher.SynchedEntityData.DataValue<?>> entityData =
                unvanishedPlayer.getEntityData().getNonDefaultValues();
            if (entityData != null && !entityData.isEmpty()) {
                observer.connection.send(new ClientboundSetEntityDataPacket(
                    unvanishedPlayer.getId(), entityData
                ));
            }

            // 4. Send equipment (held items, armour)
            List<Pair<EquipmentSlot, ItemStack>> equipment = new ArrayList<>();
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = unvanishedPlayer.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    equipment.add(Pair.of(slot, stack.copy()));
                }
            }
            if (!equipment.isEmpty()) {
                observer.connection.send(new ClientboundSetEquipmentPacket(
                    unvanishedPlayer.getId(), equipment
                ));
            }

            // 5. Sync head yaw so the model faces the right direction
            observer.connection.send(new ClientboundRotateHeadPacket(
                unvanishedPlayer,
                (byte) Math.floor(unvanishedPlayer.getYHeadRot() * 256.0F / 360.0F)
            ));
        } catch (Exception e) {
            LOGGER.error("Failed to show player {} to {}", unvanishedPlayer.getName().getString(), observer.getName().getString(), e);
        }
    }
    
    /**
     * Format timestamp to readable string
     */
    private static String formatTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    /**
     * Load vanished-player state from the active {@link com.zerog.neoessentials.storage.DataStore}.
     * viewerPriorities (who currently has see-vanished toggled on) is intentionally NOT
     * persisted — it is a purely in-memory/session-scoped runtime toggle.
     */
    private void loadData() {
        for (JsonObject obj : store.getAll(COLLECTION).values()) {
            UUID uuid = UUID.fromString(obj.get("uuid").getAsString());
            int priority = obj.has("priority") ? obj.get("priority").getAsInt() : 10;
            vanishedPlayers.put(uuid, priority);
        }
    }

    private JsonObject toJson(UUID uuid, int priority) {
        JsonObject obj = new JsonObject();
        obj.addProperty("uuid", uuid.toString());
        obj.addProperty("priority", priority);
        return obj;
    }

    /**
     * One-time import of the legacy vanished_players.json file into the active DataStore, if
     * it's still empty and storage.autoMigrate is enabled. Only the "vanished" array is
     * migrated — the legacy file's "viewerPriorities" array is intentionally dropped, since
     * that state is now purely in-memory/session-scoped (see viewerPriorities field comment).
     */
    private void migrateLegacyFilesIfNeeded() {
        if (store.hasAnyData(COLLECTION)) return;
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;

        File file = new File(com.zerog.neoessentials.util.ResourceUtil.DATA_DIR + "moderation", "vanished_players.json");
        if (!file.exists()) return;

        int migrated = 0;
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root != null && root.has("vanished")) {
                for (JsonElement element : root.getAsJsonArray("vanished")) {
                    JsonObject obj = element.getAsJsonObject();
                    String id = obj.get("uuid").getAsString();
                    store.put(COLLECTION, id, obj.deepCopy());
                    migrated++;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to migrate legacy vanished_players.json: {}", e.getMessage());
        }

        if (migrated > 0) {
            NeoLog.info(LOGGER, LogCategory.MODERATION, "VanishManager: migrated {} vanish record(s) from legacy file into the '{}' storage backend.",
                migrated, com.zerog.neoessentials.storage.StorageManager.getInstance().getActiveType());
        }
    }
}