package com.zerog.neoessentials.moderation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zerog.neoessentials.util.InputValidator;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player freeze system to immobilize players
 */
public class FreezeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FreezeManager.class);
    private static FreezeManager instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final File freezeFile;
    
    // In-memory cache for quick lookups
    private final Map<UUID, FreezeEntry> frozenPlayers = new ConcurrentHashMap<>();
    
    public static class FreezeEntry {
        public String playerName;
        public UUID playerId;
        public String reason;
        public String frozenBy;
        public long freezeTime;
        public BlockPos frozenPosition;
        
        public FreezeEntry(String playerName, UUID playerId, String reason, String frozenBy) {
            this.playerName = playerName;
            this.playerId = playerId;
            this.reason = reason;
            this.frozenBy = frozenBy;
            this.freezeTime = System.currentTimeMillis();
        }
        
        public String getFormattedFreezeTime() {
            return formatTime(freezeTime);
        }
    }
    
    private FreezeManager() {
        // Create moderation directory if it doesn't exist
        File moderationDir = new File(com.zerog.neoessentials.util.ResourceUtil.DATA_DIR + "moderation");
        if (!moderationDir.exists()) {
            if (!moderationDir.mkdirs()) {
                LOGGER.error("Failed to create moderation directory: {}", moderationDir.getAbsolutePath());
            }
        }
        
        this.freezeFile = new File(moderationDir, "frozen_players.json");
        loadData();
    }
    
    public static FreezeManager getInstance() {
        if (instance == null) {
            instance = new FreezeManager();
        }
        return instance;
    }
    
    /**
     * Freeze a player
     */
    public boolean freezePlayer(String playerName, UUID playerId, String reason, String frozenBy) {
        if (isPlayerFrozen(playerId)) {
            return false; // Already frozen
        }

        // Validate reason length and content
    InputValidator.ValidationResult reasonResult = InputValidator.validateReason(reason);
        if (!reasonResult.isValid()) {
            LOGGER.warn("Freeze failed for {}: invalid reason: {}", playerName, reasonResult.getErrorMessage());
            return false;
        }
        reason = (String) reasonResult.getValue();

        FreezeEntry freeze = new FreezeEntry(playerName, playerId, reason, frozenBy);

        // Store current position if online — notification is the command's responsibility
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                freeze.frozenPosition = player.blockPosition();
                // NOTE: do NOT send the frozen message here.
                // FreezeCommand.executeFreeze() and executeFreezeAll() handle player
                // notification to avoid the player receiving duplicate messages.
            }
        }

        frozenPlayers.put(playerId, freeze);
        saveData();

        LOGGER.info("Player {} ({}) frozen by {} for: {}", playerName, playerId, frozenBy, reason);
        return true;
    }
    
    /**
     * Unfreeze a player
     */
    public boolean unfreezePlayer(UUID playerId) {
        FreezeEntry removed = frozenPlayers.remove(playerId);
        if (removed != null) {
            saveData();
            
            // Notify player if online
            MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player != null) {
                    // Get unfreeze message from config, fallback to localization
                    String template = com.zerog.neoessentials.config.ConfigManager.getUnfreezeMessage();
                    String message;
                    if (template.equals("neoessentials.moderation.unfrozen_message")) {
                        message = MessageUtil.localize(template);
                    } else {
                        // Try to get the unfreezer's name if possible (not always available here)
                        message = template.replace("{unfreezer}", "Staff");
                    }
                    player.sendSystemMessage(MessageUtil.success(message));
                }
            }
            
            LOGGER.info("Player {} ({}) unfrozen", removed.playerName, playerId);
            return true;
        }
        return false;
    }
    
    /**
     * Freeze all players on the server
     */
    public int freezeAllPlayers(String reason, String frozenBy) {
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return 0;
        
        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (freezePlayer(player.getName().getString(), player.getUUID(), reason, frozenBy)) {
                count++;
            }
        }
        
        LOGGER.info("Froze {} players by {}", count, frozenBy);
        return count;
    }
    
    /**
     * Unfreeze all players
     */
    public int unfreezeAllPlayers() {
        int count = frozenPlayers.size();
        
        // Notify all online frozen players
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (UUID playerId : frozenPlayers.keySet()) {
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player != null) {
                    String message = MessageUtil.localize("neoessentials.moderation.unfrozen_message");
                    player.sendSystemMessage(MessageUtil.success(message));
                }
            }
        }
        
        frozenPlayers.clear();
        saveData();
        
        LOGGER.info("Unfroze {} players", count);
        return count;
    }
    
    /**
     * Check if a player is frozen
     */
    public boolean isPlayerFrozen(UUID playerId) {
        return frozenPlayers.containsKey(playerId);
    }
    
    /**
     * Get freeze entry for a player
     */
    public FreezeEntry getFreezeEntry(UUID playerId) {
        return frozenPlayers.get(playerId);
    }
    
    /**
     * Get all frozen players
     */
    public List<FreezeEntry> getAllFrozenPlayers() {
        return new ArrayList<>(frozenPlayers.values());
    }
    
    /**
     * Check if player can move
     */
    public boolean canPlayerMove(ServerPlayer player) {
        return !isPlayerFrozen(player.getUUID());
    }
    
    /**
     * Check if player can interact
     */
    public boolean canPlayerInteract(ServerPlayer player) {
        return !isPlayerFrozen(player.getUUID());
    }
    
    /**
     * Check if player can attack
     */
    public boolean canPlayerAttack(ServerPlayer player) {
        return !isPlayerFrozen(player.getUUID());
    }
    
    /**
     * Check if player can break blocks
     */
    public boolean canPlayerBreakBlocks(ServerPlayer player) {
        return !isPlayerFrozen(player.getUUID());
    }
    
    /**
     * Check if player can place blocks
     */
    public boolean canPlayerPlaceBlocks(ServerPlayer player) {
        return !isPlayerFrozen(player.getUUID());
    }
    
    /**
     * Check if player can pickup items
     */
    public boolean canPlayerPickupItems(ServerPlayer player) {
        return !isPlayerFrozen(player.getUUID());
    }
    
    /**
     * Check if player can drop items
     */
    public boolean canPlayerDropItems(ServerPlayer player) {
        return !isPlayerFrozen(player.getUUID());
    }
    
    /**
     * Enforce freeze position - teleport back if player moved too far
     */
    public void enforceFreezePosition(ServerPlayer player) {
        UUID playerId = player.getUUID();
        FreezeEntry freeze = getFreezeEntry(playerId);
        
        if (freeze == null || freeze.frozenPosition == null) {
            return;
        }
        
        BlockPos currentPos = player.blockPosition();
        
        // Allow small movements (1 block) to prevent getting stuck
        if (currentPos.distSqr(freeze.frozenPosition) > 2) {
            // Teleport back to frozen position
            player.teleportTo(
                freeze.frozenPosition.getX() + 0.5, 
                freeze.frozenPosition.getY(), 
                freeze.frozenPosition.getZ() + 0.5
            );
            
            String message = MessageUtil.localize("neoessentials.moderation.freeze_movement_blocked");
            player.sendSystemMessage(MessageUtil.warning(message));
        }
    }
    
    /**
     * Handle player join - set up freeze state
     */
    public void onPlayerJoin(ServerPlayer player) {
        UUID playerId = player.getUUID();
        FreezeEntry freeze = getFreezeEntry(playerId);
        
        if (freeze != null) {
            // Update frozen position to current position if not set
            if (freeze.frozenPosition == null) {
                freeze.frozenPosition = player.blockPosition();
                saveData();
            }
            // Get freeze reminder from config, fallback to localization
            String template = com.zerog.neoessentials.config.ConfigManager.getFreezeReminder();
            String message;
            if (template.equals("neoessentials.moderation.freeze_reminder")) {
                message = MessageUtil.localize(template, freeze.reason);
            } else {
                message = template.replace("{reason}", freeze.reason != null ? freeze.reason : "");
            }
            player.sendSystemMessage(MessageUtil.warning(message));
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
        if (!freezeFile.exists()) return;
        
        try (FileReader reader = new FileReader(freezeFile)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root != null && root.has("frozen")) {
                JsonArray frozenArray = root.getAsJsonArray("frozen");
                for (JsonElement element : frozenArray) {
                    JsonObject freezeObj = element.getAsJsonObject();
                    FreezeEntry freeze = new FreezeEntry(
                        freezeObj.get("playerName").getAsString(),
                        UUID.fromString(freezeObj.get("playerId").getAsString()),
                        freezeObj.get("reason").getAsString(),
                        freezeObj.get("frozenBy").getAsString()
                    );
                    freeze.freezeTime = freezeObj.get("freezeTime").getAsLong();
                    
                    if (freezeObj.has("frozenPosition")) {
                        JsonObject posObj = freezeObj.getAsJsonObject("frozenPosition");
                        freeze.frozenPosition = new BlockPos(
                            posObj.get("x").getAsInt(),
                            posObj.get("y").getAsInt(),
                            posObj.get("z").getAsInt()
                        );
                    }
                    
                    frozenPlayers.put(freeze.playerId, freeze);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load freeze data", e);
        }
    }
    
    /**
     * Save data to file
     */
    private void saveData() {
        try (FileWriter writer = new FileWriter(freezeFile)) {
            JsonObject root = new JsonObject();
            JsonArray frozenArray = new JsonArray();
            
            for (FreezeEntry freeze : frozenPlayers.values()) {
                JsonObject freezeObj = new JsonObject();
                freezeObj.addProperty("playerName", freeze.playerName);
                freezeObj.addProperty("playerId", freeze.playerId.toString());
                freezeObj.addProperty("reason", freeze.reason);
                freezeObj.addProperty("frozenBy", freeze.frozenBy);
                freezeObj.addProperty("freezeTime", freeze.freezeTime);
                
                if (freeze.frozenPosition != null) {
                    JsonObject posObj = new JsonObject();
                    posObj.addProperty("x", freeze.frozenPosition.getX());
                    posObj.addProperty("y", freeze.frozenPosition.getY());
                    posObj.addProperty("z", freeze.frozenPosition.getZ());
                    freezeObj.add("frozenPosition", posObj);
                }
                
                frozenArray.add(freezeObj);
            }
            
            root.add("frozen", frozenArray);
            gson.toJson(root, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save freeze data", e);
        }
    }
}