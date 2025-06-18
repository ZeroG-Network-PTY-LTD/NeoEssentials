package com.zerog.neoessentials.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.utils.TimeUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages jail locations and jailed players.
 */
public class JailManager {
    private final Map<String, JailLocation> jails = new ConcurrentHashMap<>();
    private final Map<UUID, JailData> jailedPlayers = new ConcurrentHashMap<>();
    private final File jailsFile;
    private final File jailedPlayersFile;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Creates a new JailManager.
     *
     * @param dataFolder The folder to store jail data in
     */
    public JailManager(File dataFolder) {
        this.jailsFile = new File(dataFolder, "jails.json");
        this.jailedPlayersFile = new File(dataFolder, "jailed_players.json");
        
        loadJails();
        loadJailedPlayers();
    }
    
    /**
     * Loads the jails from the jails.json file.
     */
    private void loadJails() {
        try {
            if (!jailsFile.exists()) {
                saveJails();
                return;
            }
            
            try (Reader reader = new FileReader(jailsFile)) {
                Type type = new TypeToken<Map<String, JailLocation>>(){}.getType();
                Map<String, JailLocation> loadedJails = GSON.fromJson(reader, type);
                
                if (loadedJails != null) {
                    jails.clear();
                    jails.putAll(loadedJails);
                    NeoEssentials.LOGGER.info("Loaded {} jails", jails.size());
                }
            }
        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            NeoEssentials.LOGGER.error("Failed to load jails", e);
        }
    }
    
    /**
     * Loads the jailed players from the jailed_players.json file.
     */
    private void loadJailedPlayers() {
        try {
            if (!jailedPlayersFile.exists()) {
                saveJailedPlayers();
                return;
            }
            
            try (Reader reader = new FileReader(jailedPlayersFile)) {
                Type type = new TypeToken<Map<UUID, JailData>>(){}.getType();
                Map<UUID, JailData> loadedPlayers = GSON.fromJson(reader, type);
                
                if (loadedPlayers != null) {
                    jailedPlayers.clear();
                    
                    // Filter out expired jail sentences
                    loadedPlayers.entrySet().removeIf(entry -> {
                        JailData data = entry.getValue();
                        if (data.getReleaseTime() > 0 && data.getReleaseTime() < Instant.now().getEpochSecond()) {
                            NeoEssentials.LOGGER.info("Player {} jail sentence has expired", entry.getKey());
                            return true;
                        }
                        return false;
                    });
                    
                    jailedPlayers.putAll(loadedPlayers);
                    NeoEssentials.LOGGER.info("Loaded {} jailed players", jailedPlayers.size());
                }
            }
        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            NeoEssentials.LOGGER.error("Failed to load jailed players", e);
        }
    }
    
    /**
     * Saves the jails to the jails.json file.
     */
    public void saveJails() {
        try {
            if (!jailsFile.getParentFile().exists() && !jailsFile.getParentFile().mkdirs()) {
                NeoEssentials.LOGGER.error("Failed to create jails directory");
                return;
            }
            
            try (Writer writer = new FileWriter(jailsFile)) {
                GSON.toJson(jails, writer);
            }
        } catch (JsonIOException | IOException e) {
            NeoEssentials.LOGGER.error("Failed to save jails", e);
        }
    }
    
    /**
     * Saves the jailed players to the jailed_players.json file.
     */
    public void saveJailedPlayers() {
        try {
            if (!jailedPlayersFile.getParentFile().exists() && !jailedPlayersFile.getParentFile().mkdirs()) {
                NeoEssentials.LOGGER.error("Failed to create jailed players directory");
                return;
            }
            
            try (Writer writer = new FileWriter(jailedPlayersFile)) {
                GSON.toJson(jailedPlayers, writer);
            }
        } catch (JsonIOException | IOException e) {
            NeoEssentials.LOGGER.error("Failed to save jailed players", e);
        }
    }
    
    /**
     * Adds a jail location.
     *
     * @param name The name of the jail
     * @param level The level the jail is in
     * @param pos The position of the jail
     */
    public void addJail(String name, ServerLevel level, BlockPos pos) {
        JailLocation jail = new JailLocation(
                level.dimension().location().toString(),
                pos.getX(),
                pos.getY(),
                pos.getZ()
        );
        
        jails.put(name.toLowerCase(), jail);
        saveJails();
    }
    
    /**
     * Removes a jail location.
     *
     * @param name The name of the jail
     * @return True if the jail was removed, false otherwise
     */
    public boolean removeJail(String name) {
        boolean removed = jails.remove(name.toLowerCase()) != null;
        
        if (removed) {
            saveJails();
        }
        
        return removed;
    }
    
    /**
     * Gets a jail location.
     *
     * @param name The name of the jail
     * @return The jail location, or null if it doesn't exist
     */
    public JailLocation getJail(String name) {
        return jails.get(name.toLowerCase());
    }
    
    /**
     * Gets all jail locations.
     *
     * @return A collection of all jail locations
     */
    public Collection<Map.Entry<String, JailLocation>> getJails() {
        return jails.entrySet();
    }
    
    /**
     * Checks if a jail exists.
     *
     * @param name The name of the jail
     * @return True if the jail exists, false otherwise
     */
    public boolean jailExists(String name) {
        return jails.containsKey(name.toLowerCase());
    }
    
    /**
     * Jails a player.
     *
     * @param player The player to jail
     * @param jailName The name of the jail to put them in
     * @param duration The duration of the jail sentence in seconds, or -1 for infinite
     * @param reason The reason for jailing the player
     * @return True if the player was jailed successfully, false otherwise
     */
    public boolean jailPlayer(ServerPlayer player, String jailName, long duration, String reason) {
        jailName = jailName.toLowerCase();
        
        if (!jailExists(jailName)) {
            return false;
        }
        
        JailLocation jail = getJail(jailName);
        ServerLevel level = jail.getLevel(player.server);
        
        if (level == null) {
            return false;
        }
        
        // Store their previous location before teleporting
        JailData jailData = new JailData(
                player.serverLevel().dimension().location().toString(),
                player.getX(),
                player.getY(),
                player.getZ(),
                jailName,
                reason,
                duration > 0 ? Instant.now().getEpochSecond() + duration : -1
        );
        
        jailedPlayers.put(player.getUUID(), jailData);
        saveJailedPlayers();
        
        // Teleport to jail
        player.teleportTo(level, 
                jail.getX() + 0.5, 
                jail.getY(), 
                jail.getZ() + 0.5, 
                player.getYRot(), 
                player.getXRot());
        
        return true;
    }
    
    /**
     * Unjails a player.
     *
     * @param player The player to unjail
     * @param teleportBack Whether to teleport them back to their previous location
     * @return True if the player was unjailed successfully, false otherwise
     */
    public boolean unjailPlayer(ServerPlayer player, boolean teleportBack) {
        JailData jailData = jailedPlayers.remove(player.getUUID());
        
        if (jailData == null) {
            return false;
        }
        
        saveJailedPlayers();
        
        if (teleportBack) {
            // Try to teleport them back to their previous location
            try {                ResourceLocation dimensionKey = ResourceLocation.parse(jailData.getDimension());
                ServerLevel level = player.server.getLevel(net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION, dimensionKey));
                
                if (level != null) {
                    player.teleportTo(level, 
                            jailData.getX(), 
                            jailData.getY(), 
                            jailData.getZ(), 
                            player.getYRot(), 
                            player.getXRot());
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to teleport player back from jail", e);
            }
        }
        
        return true;
    }
    
    /**
     * Unjails a player by UUID.
     *
     * @param uuid The UUID of the player to unjail
     * @return True if the player was unjailed successfully, false otherwise
     */
    public boolean unjailPlayer(UUID uuid) {
        boolean removed = jailedPlayers.remove(uuid) != null;
        
        if (removed) {
            saveJailedPlayers();
        }
        
        return removed;
    }
    
    /**
     * Checks if a player is jailed.
     *
     * @param player The player to check
     * @return True if the player is jailed, false otherwise
     */
    public boolean isJailed(ServerPlayer player) {
        return isJailed(player.getUUID());
    }
    
    /**
     * Checks if a player is jailed by UUID.
     *
     * @param uuid The UUID of the player to check
     * @return True if the player is jailed, false otherwise
     */
    public boolean isJailed(UUID uuid) {
        JailData jailData = jailedPlayers.get(uuid);
        
        if (jailData == null) {
            return false;
        }
        
        // Check if their sentence has expired
        if (jailData.getReleaseTime() > 0 && jailData.getReleaseTime() < Instant.now().getEpochSecond()) {
            jailedPlayers.remove(uuid);
            saveJailedPlayers();
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets the jail data for a player.
     *
     * @param player The player to get the jail data for
     * @return The jail data, or null if the player is not jailed
     */
    public JailData getJailData(ServerPlayer player) {
        return getJailData(player.getUUID());
    }
    
    /**
     * Gets the jail data for a player by UUID.
     *
     * @param uuid The UUID of the player to get the jail data for
     * @return The jail data, or null if the player is not jailed
     */
    public JailData getJailData(UUID uuid) {
        JailData jailData = jailedPlayers.get(uuid);
        
        if (jailData != null && jailData.getReleaseTime() > 0 && jailData.getReleaseTime() < Instant.now().getEpochSecond()) {
            jailedPlayers.remove(uuid);
            saveJailedPlayers();
            return null;
        }
        
        return jailData;
    }
    
    /**
     * Gets all jailed players.
     *
     * @return A collection of all jailed player UUIDs and their jail data
     */
    public Collection<Map.Entry<UUID, JailData>> getJailedPlayers() {
        // Remove expired jail sentences
        jailedPlayers.entrySet().removeIf(entry -> {
            JailData data = entry.getValue();
            return data.getReleaseTime() > 0 && data.getReleaseTime() < Instant.now().getEpochSecond();
        });
        
        return jailedPlayers.entrySet();
    }
    
    /**
     * Gets the remaining jail time for a player in seconds.
     *
     * @param player The player to check
     * @return The remaining jail time in seconds, or -1 if the player is jailed indefinitely, or 0 if the player is not jailed
     */
    public long getRemainingJailTime(ServerPlayer player) {
        return getRemainingJailTime(player.getUUID());
    }
    
    /**
     * Gets the remaining jail time for a player in seconds by UUID.
     *
     * @param uuid The UUID of the player to check
     * @return The remaining jail time in seconds, or -1 if the player is jailed indefinitely, or 0 if the player is not jailed
     */
    public long getRemainingJailTime(UUID uuid) {
        JailData jailData = jailedPlayers.get(uuid);
        
        if (jailData == null) {
            return 0;
        }
        
        if (jailData.getReleaseTime() < 0) {
            return -1;
        }
        
        long remaining = jailData.getReleaseTime() - Instant.now().getEpochSecond();
        
        if (remaining <= 0) {
            jailedPlayers.remove(uuid);
            saveJailedPlayers();
            return 0;
        }
        
        return remaining;
    }
    
    /**
     * Gets the formatted remaining jail time for a player.
     *
     * @param player The player to check
     * @return The formatted remaining jail time, or "forever" if the player is jailed indefinitely, or null if the player is not jailed
     */
    public String getFormattedRemainingJailTime(ServerPlayer player) {
        return getFormattedRemainingJailTime(player.getUUID());
    }
    
    /**
     * Gets the formatted remaining jail time for a player by UUID.
     *
     * @param uuid The UUID of the player to check
     * @return The formatted remaining jail time, or "forever" if the player is jailed indefinitely, or null if the player is not jailed
     */
    public String getFormattedRemainingJailTime(UUID uuid) {
        long remaining = getRemainingJailTime(uuid);
        
        if (remaining == 0) {
            return null;
        }
        
        if (remaining == -1) {
            return "forever";
        }
        
        return TimeUtil.formatTimeDuration(remaining);
    }
    
    /**
     * Represents a jail location.
     */
    public static class JailLocation {
        private final String dimension;
        private final double x;
        private final double y;
        private final double z;
        
        public JailLocation(String dimension, double x, double y, double z) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public String getDimension() {
            return dimension;
        }
        
        public double getX() {
            return x;
        }
        
        public double getY() {
            return y;
        }
        
        public double getZ() {
            return z;
        }
        
        /**
         * Gets the level for this jail location.
         *
         * @param server The server to get the level from
         * @return The level, or null if it doesn't exist
         */
        public ServerLevel getLevel(MinecraftServer server) {            try {
                ResourceLocation dimensionKey = ResourceLocation.parse(dimension);
                return server.getLevel(net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION, dimensionKey));
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to get level for dimension {}", dimension, e);
                return null;
            }
        }
    }
    
    /**
     * Represents data for a jailed player.
     */
    public static class JailData {
        private final String dimension;
        private final double x;
        private final double y;
        private final double z;
        private final String jailName;
        private final String reason;
        private final long releaseTime; // -1 for indefinite, otherwise Unix timestamp when they'll be released
        
        public JailData(String dimension, double x, double y, double z, String jailName, String reason, long releaseTime) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.jailName = jailName;
            this.reason = reason;
            this.releaseTime = releaseTime;
        }
        
        public String getDimension() {
            return dimension;
        }
        
        public double getX() {
            return x;
        }
        
        public double getY() {
            return y;
        }
        
        public double getZ() {
            return z;
        }
        
        public String getJailName() {
            return jailName;
        }
        
        public String getReason() {
            return reason;
        }
        
        public long getReleaseTime() {
            return releaseTime;
        }
    }
}
