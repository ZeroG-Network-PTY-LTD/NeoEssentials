package com.zerog.neoessentials.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Manages spawn location data for the NeoEssentials mod.
 */
public class SpawnManager {
    private static final String SPAWN_DATA_FILE = "neoessentials/spawn.json";
    
    private SpawnLocation spawnLocation;
    
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    
    /**
     * Initialize the spawn manager
     */
    public void initialize() {
        NeoEssentials.LOGGER.info("Initializing NeoEssentials Spawn Manager");
        
        // Create spawn data directory if it doesn't exist
        File spawnDataDir = new File("neoessentials");
        if (!spawnDataDir.exists() && spawnDataDir.mkdirs()) {
            NeoEssentials.LOGGER.info("Created spawn data directory: {}", spawnDataDir);
        }
        
        // Load spawn data
        loadSpawnData();
    }
    
    /**
     * Load spawn data from disk
     */
    private void loadSpawnData() {
        try {
            File spawnFile = new File(SPAWN_DATA_FILE);
            
            if (spawnFile.exists()) {
                try (FileReader reader = new FileReader(spawnFile)) {
                    spawnLocation = gson.fromJson(reader, SpawnLocation.class);
                }
            }
            
            // If spawn is null or file doesn't exist, set spawn to default
            if (spawnLocation == null) {
                spawnLocation = new SpawnLocation(
                        "minecraft:overworld",
                        0.0,
                        70.0,
                        0.0,
                        0.0f,
                        0.0f
                );
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Error loading spawn data", e);
            
            // Set default spawn location
            spawnLocation = new SpawnLocation(
                    "minecraft:overworld",
                    0.0,
                    70.0,
                    0.0,
                    0.0f,
                    0.0f
            );
        }
    }
    
    /**
     * Save spawn data to disk
     */
    public void saveSpawnData() {
        try {
            File spawnFile = new File(SPAWN_DATA_FILE);
            
            // Create parent directories if they don't exist
            File parentDir = spawnFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                NeoEssentials.LOGGER.error("Failed to create directory for spawn data: {}", parentDir);
                return;
            }
            
            if (spawnLocation != null) {
                try (FileWriter writer = new FileWriter(spawnFile)) {
                    gson.toJson(spawnLocation, writer);
                }
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Error saving spawn data", e);
        }
    }
    
    /**
     * Sets the spawn location
     * 
     * @param player The player setting the spawn (to get their position)
     * @return True if successful
     */
    public boolean setSpawn(ServerPlayer player) {
        spawnLocation = new SpawnLocation(
                player.level().dimension().location().toString(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
        );
        
        saveSpawnData();
        return true;
    }
    
    /**
     * Gets the spawn location
     * 
     * @return The spawn location
     */
    public SpawnLocation getSpawnLocation() {
        return spawnLocation;
    }
    
    /**
     * Gets the server level for the spawn
     * 
     * @param server The Minecraft server
     * @return The server level for the spawn
     */
    public ServerLevel getSpawnLevel(MinecraftServer server) {
        if (spawnLocation == null) {
            return server.overworld();
        }
        
        try {
            ResourceLocation dimLocation = ResourceLocation.tryParse(spawnLocation.dimension);
            if (dimLocation == null) {
                return server.overworld(); // Default to overworld
            }
            
            ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, dimLocation);
            return server.getLevel(levelKey);
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error getting spawn dimension", e);
            return server.overworld();
        }
    }
    
    /**
     * Gets the Vec3 position for the spawn
     * 
     * @return The Vec3 position
     */
    public Vec3 getSpawnPosition() {
        if (spawnLocation == null) {
            return new Vec3(0, 70, 0);
        }
        
        return new Vec3(spawnLocation.x, spawnLocation.y, spawnLocation.z);
    }
    
    /**
     * Class to store spawn location data
     */
    public static class SpawnLocation {
        private String dimension;
        private double x;
        private double y;
        private double z;
        private float yaw;
        private float pitch;
        
        public SpawnLocation() {
            // Default constructor for GSON
        }
        
        public SpawnLocation(String dimension, double x, double y, double z, float yaw, float pitch) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
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
        
        public float getYaw() {
            return yaw;
        }
        
        public float getPitch() {
            return pitch;
        }
        
        public BlockPos getBlockPos() {
            return BlockPos.containing(x, y, z);
        }
    }
}
