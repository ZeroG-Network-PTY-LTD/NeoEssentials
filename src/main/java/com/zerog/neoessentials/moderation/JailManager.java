package com.zerog.neoessentials.moderation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Manages player jail system with persistent storage
 */
public class JailManager {
    private static boolean jailSystemEnabledCache = true;
    private static final Logger LOGGER = LoggerFactory.getLogger(JailManager.class);
    private static JailManager instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File jailFile;
    private final File jailLocationFile;
    // Track number of times each player has been jailed
    private final Map<UUID, Integer> jailCounts = new ConcurrentHashMap<>();
    
    // In-memory cache for quick lookups
    private final Map<UUID, JailEntry> jailedPlayers = new ConcurrentHashMap<>();
    private final Map<String, JailLocation> jailLocations = new ConcurrentHashMap<>();
    
    public static class JailEntry {
        public String playerName;
        public UUID playerId;
        public String reason;
        public String jailedBy;
        public long jailTime;
        public String jailName;
        public BlockPos originalLocation;
        public String originalDimension;
        
        public JailEntry(String playerName, UUID playerId, String reason, String jailedBy, String jailName) {
            this.playerName = playerName;
            this.playerId = playerId;
            this.reason = reason;
            this.jailedBy = jailedBy;
            this.jailName = jailName;
            this.jailTime = System.currentTimeMillis();
        }
        
        public String getFormattedJailTime() {
            return formatTime(jailTime);
        }
    }
    
    public static class JailLocation {
        public String name;
        public BlockPos position;
        public String dimension;
        public String createdBy;
        public long createdTime;
        
        public JailLocation(String name, BlockPos position, String dimension, String createdBy) {
            this.name = name;
            this.position = position;
            this.dimension = dimension;
            this.createdBy = createdBy;
            this.createdTime = System.currentTimeMillis();
        }
        
        public String getFormattedCreatedTime() {
            return formatTime(createdTime);
        }
    }
    
    private JailManager() {
        // Check config for jail system enabled
        jailSystemEnabledCache = com.zerog.neoessentials.config.ConfigManager.isJailSystemEnabled();
        if (!jailSystemEnabledCache) {
            LOGGER.info("Jail system is disabled via config. All jail features will be inactive.");
        }
        // Create moderation directory if it doesn't exist
        File moderationDir = new File("config/neoessentials/moderation");
        if (!moderationDir.exists()) {
            moderationDir.mkdirs();
        }
        
        this.jailFile = new File(moderationDir, "jailed_players.json");
        this.jailLocationFile = new File(moderationDir, "jail_locations.json");
        loadData();
    }

    public static boolean isJailSystemEnabled() {
        return jailSystemEnabledCache;
    }
    
    public static JailManager getInstance() {
        if (instance == null) {
            instance = new JailManager();
        }
        return instance;
    }
    
    /**
     * Jail a player
     */
    public boolean jailPlayer(String playerName, UUID playerId, String reason, String jailedBy, String jailName) {
        if (isPlayerJailed(playerId)) {
            return false; // Already jailed
        }

        JailLocation jailLoc = jailLocations.get(jailName);
        if (jailLoc == null) {
            return false; // Jail doesn't exist
        }

        // Track jail count
        int jailCount = jailCounts.getOrDefault(playerId, 0) + 1;
        jailCounts.put(playerId, jailCount);

        // Check thresholds
        int tempBanThreshold = com.zerog.neoessentials.config.ConfigManager.getMaxJailsBeforeTempBan();
        int permBanThreshold = com.zerog.neoessentials.config.ConfigManager.getMaxJailsBeforePermBan();
        int tempBanDuration = com.zerog.neoessentials.config.ConfigManager.getTempBanDurationMinutes();

        if (jailCount >= permBanThreshold) {
            // Issue permanent ban
            BanManager banManager = BanManager.getInstance();
            banManager.banPlayer(playerName, playerId, "Exceeded maximum jailings (permanent ban)", "System");
            jailCounts.put(playerId, 0); // Reset count
            if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogJailActionsEnabled()) {
                LOGGER.info("Player {} ({}) permanently banned after {} jailings.", playerName, playerId, jailCount);
            }
            return false;
        } else if (jailCount >= tempBanThreshold) {
            // Issue temp ban
            BanManager banManager = BanManager.getInstance();
            banManager.tempBanPlayer(playerName, playerId, "Exceeded maximum jailings (temporary ban)", "System", tempBanDuration * 60 * 1000L);
            if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogJailActionsEnabled()) {
                LOGGER.info("Player {} ({}) temp-banned for {} minutes after {} jailings.", playerName, playerId, tempBanDuration, jailCount);
            }
            return false;
        }

        // Store original location
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                JailEntry jail = new JailEntry(playerName, playerId, reason, jailedBy, jailName);
                jail.originalLocation = player.blockPosition();
                jail.originalDimension = player.level().dimension().location().toString();

                jailedPlayers.put(playerId, jail);
                saveJailedPlayers();

                // Teleport to jail
                teleportToJail(player, jailLoc);

                String message = MessageUtil.localize("neoessentials.moderation.jailed_message", reason, jailedBy);
                player.sendSystemMessage(MessageUtil.warning(message));

        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogJailActionsEnabled()) {
            LOGGER.info("Player {} ({}) jailed by {} in {} for: {}", 
                playerName, playerId, jailedBy, jailName, reason);
        }
                return true;
            }
        }

        // Player offline - still record the jail
        JailEntry jail = new JailEntry(playerName, playerId, reason, jailedBy, jailName);
        jailedPlayers.put(playerId, jail);
        saveJailedPlayers();

    if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogJailActionsEnabled()) {
        LOGGER.info("Player {} ({}) jailed while offline by {} in {} for: {}", 
            playerName, playerId, jailedBy, jailName, reason);
    }
        return true;
    }
    
    /**
     * Unjail a player
     */
    public boolean unjailPlayer(UUID playerId) {
        JailEntry jail = jailedPlayers.remove(playerId);
        if (jail != null) {
            saveJailedPlayers();
            
            // Teleport back to original location if online
            MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player != null) {
                    if (jail.originalLocation != null && jail.originalDimension != null) {
                        teleportToOriginalLocation(player, jail);
                    }
                    
                    String message = MessageUtil.localize("neoessentials.moderation.unjailed_message");
                    player.sendSystemMessage(MessageUtil.success(message));
                }
            }
            
            if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogJailActionsEnabled()) {
                LOGGER.info("Player {} ({}) unjailed", jail.playerName, playerId);
            }
            return true;
        }
        return false;
    }
    
    /**
     * Set a jail location
     */
    public boolean setJailLocation(String jailName, BlockPos position, String dimension, String createdBy) {
        JailLocation jail = new JailLocation(jailName, position, dimension, createdBy);
        jailLocations.put(jailName, jail);
        saveJailLocations();
        
        LOGGER.info("Jail location '{}' set at {} in {} by {}", jailName, position, dimension, createdBy);
        return true;
    }
    
    /**
     * Remove a jail location
     */
    public boolean removeJailLocation(String jailName) {
        JailLocation removed = jailLocations.remove(jailName);
        if (removed != null) {
            saveJailLocations();
            LOGGER.info("Jail location '{}' removed", jailName);
            return true;
        }
        return false;
    }
    
    /**
     * Check if a player is jailed
     */
    public boolean isPlayerJailed(UUID playerId) {
        return jailedPlayers.containsKey(playerId);
    }
    
    /**
     * Get jail entry for a player
     */
    public JailEntry getJailEntry(UUID playerId) {
        return jailedPlayers.get(playerId);
    }
    
    /**
     * Get jail location by name
     */
    public JailLocation getJailLocation(String jailName) {
        return jailLocations.get(jailName);
    }
    
    /**
     * Get all jailed players
     */
    public List<JailEntry> getAllJailedPlayers() {
        return new ArrayList<>(jailedPlayers.values());
    }
    
    /**
     * Get all jail locations
     */
    public List<JailLocation> getAllJailLocations() {
        return new ArrayList<>(jailLocations.values());
    }
    
    /**
     * Check if player can move (not jailed or within jail bounds)
     */
    public boolean canPlayerMove(ServerPlayer player, BlockPos newPos) {
        UUID playerId = player.getUUID();
        if (!isPlayerJailed(playerId)) {
            return true; // Not jailed, can move freely
        }
        
        JailEntry jail = getJailEntry(playerId);
        if (jail == null) {
            return true;
        }
        
        JailLocation jailLoc = getJailLocation(jail.jailName);
        if (jailLoc == null) {
            return true; // Jail doesn't exist anymore
        }
        
        // Check if within jail bounds (simple distance check)
        double distance = newPos.distSqr(jailLoc.position);
        return distance <= 100; // 10 block radius squared
    }
    
    /**
     * Handle player join - teleport to jail if jailed
     */
    public void onPlayerJoin(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (!isPlayerJailed(playerId)) {
            return;
        }

        JailEntry jail = getJailEntry(playerId);
        if (jail == null) {
            return;
        }

        JailLocation jailLoc = getJailLocation(jail.jailName);
        if (jailLoc == null) {
            // Jail doesn't exist anymore, unjail player
            unjailPlayer(playerId);
            return;
        }

        boolean teleportOnLogin = com.zerog.neoessentials.config.ConfigManager.getInstance().isJailTeleportOnLoginEnabled();
        if (teleportOnLogin) {
            teleportToJail(player, jailLoc);
            String message = MessageUtil.localize("neoessentials.moderation.jail_reminder", jail.reason);
            player.sendSystemMessage(MessageUtil.warning(message));
        }
    }
    
    /**
     * Teleport player to jail
     */
    private void teleportToJail(ServerPlayer player, JailLocation jailLoc) {
        try {
            MinecraftServer server = player.getServer();
            if (server == null) return;
            
            // Get the dimension from jail location or default to overworld
            ResourceKey<Level> dimensionKey = Level.OVERWORLD; // Default
            if (jailLoc.dimension != null && !jailLoc.dimension.isEmpty()) {
                try {
                    ResourceLocation dimensionId = ResourceLocation.tryParse(jailLoc.dimension);
                    if (dimensionId != null) {
                        dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse jail dimension '{}', defaulting to overworld", jailLoc.dimension);
                }
            }
            ServerLevel dimension = server.getLevel(dimensionKey);
            
            if (dimension != null) {
                player.teleportTo(dimension, 
                        jailLoc.position.getX() + 0.5, 
                        jailLoc.position.getY() + 1, 
                        jailLoc.position.getZ() + 0.5, 
                        player.getYRot(), 
                        player.getXRot());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to teleport player {} to jail {}", player.getName().getString(), jailLoc.name, e);
        }
    }
    
    /**
     * Teleport player back to original location
     */
    private void teleportToOriginalLocation(ServerPlayer player, JailEntry jail) {
        try {
            MinecraftServer server = player.getServer();
            if (server == null || jail.originalLocation == null) return;
            
            // Get the dimension from original location or default to overworld
            ResourceKey<Level> dimensionKey = Level.OVERWORLD; // Default
            if (jail.originalDimension != null && !jail.originalDimension.isEmpty()) {
                try {
                    ResourceLocation dimensionId = ResourceLocation.tryParse(jail.originalDimension);
                    if (dimensionId != null) {
                        dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse original dimension '{}', defaulting to overworld", jail.originalDimension);
                }
            }
            ServerLevel dimension = server.getLevel(dimensionKey);
            
            if (dimension != null) {
                player.teleportTo(dimension, 
                        jail.originalLocation.getX() + 0.5, 
                        jail.originalLocation.getY() + 1, 
                        jail.originalLocation.getZ() + 0.5, 
                        player.getYRot(), 
                        player.getXRot());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to teleport player {} back to original location", player.getName().getString(), e);
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
     * Load data from files
     */
    private void loadData() {
        loadJailedPlayers();
        loadJailLocations();
    }
    
    private void loadJailedPlayers() {
        if (!jailFile.exists()) return;
        
        try (FileReader reader = new FileReader(jailFile)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root != null && root.has("jailed")) {
                JsonArray jailedArray = root.getAsJsonArray("jailed");
                for (JsonElement element : jailedArray) {
                    JsonObject jailObj = element.getAsJsonObject();
                    JailEntry jail = new JailEntry(
                        jailObj.get("playerName").getAsString(),
                        UUID.fromString(jailObj.get("playerId").getAsString()),
                        jailObj.get("reason").getAsString(),
                        jailObj.get("jailedBy").getAsString(),
                        jailObj.get("jailName").getAsString()
                    );
                    jail.jailTime = jailObj.get("jailTime").getAsLong();
                    
                    if (jailObj.has("originalLocation")) {
                        JsonObject locObj = jailObj.getAsJsonObject("originalLocation");
                        jail.originalLocation = new BlockPos(
                            locObj.get("x").getAsInt(),
                            locObj.get("y").getAsInt(),
                            locObj.get("z").getAsInt()
                        );
                    }
                    
                    if (jailObj.has("originalDimension")) {
                        jail.originalDimension = jailObj.get("originalDimension").getAsString();
                    }
                    
                    jailedPlayers.put(jail.playerId, jail);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load jailed players", e);
        }
    }
    
    private void loadJailLocations() {
        if (!jailLocationFile.exists()) return;
        
        try (FileReader reader = new FileReader(jailLocationFile)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root != null && root.has("jails")) {
                JsonArray jailsArray = root.getAsJsonArray("jails");
                for (JsonElement element : jailsArray) {
                    JsonObject jailObj = element.getAsJsonObject();
                    
                    JsonObject posObj = jailObj.getAsJsonObject("position");
                    BlockPos position = new BlockPos(
                        posObj.get("x").getAsInt(),
                        posObj.get("y").getAsInt(),
                        posObj.get("z").getAsInt()
                    );
                    
                    JailLocation jail = new JailLocation(
                        jailObj.get("name").getAsString(),
                        position,
                        jailObj.get("dimension").getAsString(),
                        jailObj.get("createdBy").getAsString()
                    );
                    jail.createdTime = jailObj.get("createdTime").getAsLong();
                    
                    jailLocations.put(jail.name, jail);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load jail locations", e);
        }
    }
    
    /**
     * Save jailed players to file
     */
    private void saveJailedPlayers() {
        try (FileWriter writer = new FileWriter(jailFile)) {
            JsonObject root = new JsonObject();
            JsonArray jailedArray = new JsonArray();
            
            for (JailEntry jail : jailedPlayers.values()) {
                JsonObject jailObj = new JsonObject();
                jailObj.addProperty("playerName", jail.playerName);
                jailObj.addProperty("playerId", jail.playerId.toString());
                jailObj.addProperty("reason", jail.reason);
                jailObj.addProperty("jailedBy", jail.jailedBy);
                jailObj.addProperty("jailName", jail.jailName);
                jailObj.addProperty("jailTime", jail.jailTime);
                
                if (jail.originalLocation != null) {
                    JsonObject locObj = new JsonObject();
                    locObj.addProperty("x", jail.originalLocation.getX());
                    locObj.addProperty("y", jail.originalLocation.getY());
                    locObj.addProperty("z", jail.originalLocation.getZ());
                    jailObj.add("originalLocation", locObj);
                }
                
                if (jail.originalDimension != null) {
                    jailObj.addProperty("originalDimension", jail.originalDimension);
                }
                
                jailedArray.add(jailObj);
            }
            
            root.add("jailed", jailedArray);
            gson.toJson(root, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save jailed players", e);
        }
    }
    
    /**
     * Save jail locations to file
     */
    private void saveJailLocations() {
        try (FileWriter writer = new FileWriter(jailLocationFile)) {
            JsonObject root = new JsonObject();
            JsonArray jailsArray = new JsonArray();
            
            for (JailLocation jail : jailLocations.values()) {
                JsonObject jailObj = new JsonObject();
                jailObj.addProperty("name", jail.name);
                jailObj.addProperty("dimension", jail.dimension);
                jailObj.addProperty("createdBy", jail.createdBy);
                jailObj.addProperty("createdTime", jail.createdTime);
                
                JsonObject posObj = new JsonObject();
                posObj.addProperty("x", jail.position.getX());
                posObj.addProperty("y", jail.position.getY());
                posObj.addProperty("z", jail.position.getZ());
                jailObj.add("position", posObj);
                
                jailsArray.add(jailObj);
            }
            
            root.add("jails", jailsArray);
            gson.toJson(root, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save jail locations", e);
        }
    }
}