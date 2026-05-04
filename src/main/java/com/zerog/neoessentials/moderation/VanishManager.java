package com.zerog.neoessentials.moderation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
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
import java.io.FileWriter;
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
    /**
     * Get priority for a player (default 10, override for custom logic)
     */
    public int getPlayerPriority(UUID playerId) {
        // Integrate with permission/group system
        // Use PermissionSystem.getManager().getUser(playerId).getGroup()
        String group = null;
        try {
            group = com.zerog.neoessentials.permissions.PermissionSystem.getManager().getUser(playerId).getGroup();
        } catch (Exception e) {
            // fallback
        }
        if (group == null) return 10;
        switch (group.toLowerCase()) {
            case "owner":
                return 0;
            case "admin":
                return 1;
            case "mod":
            case "moderator":
                return 2;
            case "helper":
                return 3;
            case "vip":
                return 5;
            case "default":
            case "member":
            default:
                return 10;
        }
    }
    private static final Logger LOGGER = LoggerFactory.getLogger(VanishManager.class);
    private static VanishManager instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final File vanishFile;
    
    // In-memory cache for quick lookups
    // Vanished players and their priority
    private final Map<UUID, Integer> vanishedPlayers = new ConcurrentHashMap<>();
    // Players who can see vanished and their priority
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
        // Create moderation directory if it doesn't exist
        File moderationDir = new File(com.zerog.neoessentials.util.ResourceUtil.DATA_DIR + "moderation");
        if (!moderationDir.exists()) {
            if (!moderationDir.mkdirs()) {
                LOGGER.error("Failed to create moderation directory: {}", moderationDir.getAbsolutePath());
            }
        }
        
        this.vanishFile = new File(moderationDir, "vanished_players.json");
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
        saveData();
        
        // Hide player from others
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer vanishedPlayer = server.getPlayerList().getPlayer(playerId);
            if (vanishedPlayer != null) {
                hidePlayerFromOthers(vanishedPlayer);
                
                // Don't send message here - let the command handle it
                // to avoid duplicate messages
            }
        }
        
        if (com.zerog.neoessentials.config.ConfigManager.isLogVanishActionsEnabled()) {
            LOGGER.info("Player {} ({}) vanished by {}", playerName, playerId, vanishedBy);
        }
        return true;
    }
    
    /**
     * Unvanish a player
     */
    public boolean unvanishPlayer(UUID playerId) {
        if (vanishedPlayers.remove(playerId) == null) {
            return false; // Not vanished
        }
        saveData();
        
        // Show player to others
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer unvanishedPlayer = server.getPlayerList().getPlayer(playerId);
            if (unvanishedPlayer != null) {
                showPlayerToOthers(unvanishedPlayer);
                
                // Don't send message here - let the command handle it
                // to avoid duplicate messages
            }
        }
        
        if (com.zerog.neoessentials.config.ConfigManager.isLogVanishActionsEnabled()) {
            LOGGER.info("Player ({}) unvanished", playerId);
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
            LOGGER.info("Player ({}) enabled see vanished", playerId);
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
            LOGGER.info("Player ({}) disabled see vanished", playerId);
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
     * Check if a player can see vanished players
     */
    public boolean canPlayerSeeVanished(UUID playerId) {
    return viewerPriorities.containsKey(playerId);
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
        MinecraftServer server = player.getServer();

        // If this player is vanished, hide them from every other online player
        // (use a 1-tick delay so our hide packet arrives after the vanilla spawn
        // packets that the server sends while placing the new player in the world).
        if (isPlayerVanished(playerId)) {
            player.sendSystemMessage(MessageUtil.info(
                MessageUtil.localize("neoessentials.moderation.vanish_reminder")));
            if (server != null) {
                server.tell(new TickTask(server.getTickCount() + 1,
                    () -> hidePlayerFromOthers(player)));
            }
        }

        // After 1 tick, handle what the joining player should (or should not) see.
        if (server != null) {
            server.tell(new TickTask(server.getTickCount() + 1, () -> {
                if (canPlayerSeeVanished(playerId)) {
                    // Show all vanished players whose priority is >= this observer's priority
                    int viewerPriority = viewerPriorities.getOrDefault(playerId, 10);
                    for (UUID vanishedId : new HashSet<>(vanishedPlayers.keySet())) {
                        if (vanishedId.equals(playerId)) continue;
                        int vanishedPriority = vanishedPlayers.getOrDefault(vanishedId, 10);
                        if (viewerPriority <= vanishedPriority) {
                            ServerPlayer vanishedPlayer = server.getPlayerList().getPlayer(vanishedId);
                            if (vanishedPlayer != null) {
                                showPlayerToSpecific(vanishedPlayer, player);
                            }
                        }
                    }
                } else {
                    // Hide every vanished player from this newly joined observer
                    for (UUID vanishedId : new HashSet<>(vanishedPlayers.keySet())) {
                        if (vanishedId.equals(playerId)) continue;
                        ServerPlayer vanishedPlayer = server.getPlayerList().getPlayer(vanishedId);
                        if (vanishedPlayer != null) {
                            hidePlayerFromSpecific(vanishedPlayer, player);
                        }
                    }
                }
            }));
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
     * Priority rule: an observer can see a vanished player only when the observer
     * is explicitly in viewerPriorities AND their priority number is <= the vanished
     * player's priority (i.e. equal or higher staff rank).
     */
    private void hidePlayerFromOthers(ServerPlayer vanishedPlayer) {
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        UUID vanishedId = vanishedPlayer.getUUID();
        int vanishedPriority = vanishedPlayers.getOrDefault(vanishedId, 10);

        for (ServerPlayer otherPlayer : server.getPlayerList().getPlayers()) {
            if (otherPlayer == vanishedPlayer) continue;
            UUID otherId = otherPlayer.getUUID();
            // The observer may only keep seeing the vanished player if they are
            // registered as a see-vanished viewer with sufficient rank.
            boolean canSee = viewerPriorities.containsKey(otherId)
                    && viewerPriorities.get(otherId) <= vanishedPriority;
            if (!canSee) {
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
     * Load data from file
     */
    private void loadData() {
        if (!vanishFile.exists()) return;
        
        try (FileReader reader = new FileReader(vanishFile)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root != null) {
                if (root.has("vanished")) {
                    JsonArray vanishedArray = root.getAsJsonArray("vanished");
                    for (JsonElement element : vanishedArray) {
                        JsonObject obj = element.getAsJsonObject();
                        UUID uuid = UUID.fromString(obj.get("uuid").getAsString());
                        int priority = obj.has("priority") ? obj.get("priority").getAsInt() : 10;
                        vanishedPlayers.put(uuid, priority);
                    }
                }
                if (root.has("viewerPriorities")) {
                    JsonArray viewerArray = root.getAsJsonArray("viewerPriorities");
                    for (JsonElement element : viewerArray) {
                        JsonObject obj = element.getAsJsonObject();
                        UUID uuid = UUID.fromString(obj.get("uuid").getAsString());
                        int priority = obj.has("priority") ? obj.get("priority").getAsInt() : 10;
                        viewerPriorities.put(uuid, priority);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load vanish data", e);
        }
    }
    
    /**
     * Save data to file
     */
    private void saveData() {
        try (FileWriter writer = new FileWriter(vanishFile)) {
            JsonObject root = new JsonObject();
            JsonArray vanishedArray = new JsonArray();
            for (Map.Entry<UUID, Integer> entry : vanishedPlayers.entrySet()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("uuid", entry.getKey().toString());
                obj.addProperty("priority", entry.getValue());
                vanishedArray.add(obj);
            }
            root.add("vanished", vanishedArray);
            JsonArray viewerArray = new JsonArray();
            for (Map.Entry<UUID, Integer> entry : viewerPriorities.entrySet()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("uuid", entry.getKey().toString());
                obj.addProperty("priority", entry.getValue());
                viewerArray.add(obj);
            }
            root.add("viewerPriorities", viewerArray);
            gson.toJson(root, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save vanish data", e);
        }
    }
}