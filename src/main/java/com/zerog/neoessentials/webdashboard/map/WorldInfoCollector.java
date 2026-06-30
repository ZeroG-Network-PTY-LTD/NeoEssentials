package com.zerog.neoessentials.webdashboard.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.WorldBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * World information collector for map viewer
 * Collects world data, spawn points, world borders, biomes, and dimension info
 */
public class WorldInfoCollector {
    @SuppressWarnings("unused") // Reserved for future logging features
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldInfoCollector.class);
    private static WorldInfoCollector INSTANCE;
    
    private MinecraftServer server;
    
    private WorldInfoCollector() {
    }
    
    public static WorldInfoCollector getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new WorldInfoCollector();
        }
        return INSTANCE;
    }
    
    /**
     * Set the Minecraft server instance
     */
    public void setServer(MinecraftServer server) {
        this.server = server;
    }
    
    /**
     * Get comprehensive world information as JSON
     */
    public JsonObject getWorldInfoJson() {
        if (server == null) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Server not initialized");
            return error;
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("timestamp", System.currentTimeMillis());
        
        // Get all dimensions
        JsonArray dimensionsArray = new JsonArray();
        for (ServerLevel level : server.getAllLevels()) {
            JsonObject dimObj = getDimensionInfo(level);
            dimensionsArray.add(dimObj);
        }
        response.add("dimensions", dimensionsArray);
        
        // Server info
        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("motd", server.getMotd());
        serverInfo.addProperty("maxPlayers", server.getMaxPlayers());
        serverInfo.addProperty("onlinePlayers", server.getPlayerCount());
        serverInfo.addProperty("difficulty", server.getWorldData().getDifficulty().name());
        serverInfo.addProperty("hardcore", server.getWorldData().isHardcore());
        response.add("serverInfo", serverInfo);
        
        return response;
    }
    
    /**
     * Get dimension-specific information
     */
    public JsonObject getDimensionInfo(ServerLevel level) {
        JsonObject dimObj = new JsonObject();
        
        String dimensionKey = level.dimension().location().toString();
        dimObj.addProperty("dimension", dimensionKey);
        dimObj.addProperty("name", getDimensionName(dimensionKey));
        
        // Spawn point
        BlockPos spawnPos = level.getSharedSpawnPos();
        JsonObject spawnObj = new JsonObject();
        spawnObj.addProperty("x", spawnPos.getX());
        spawnObj.addProperty("y", spawnPos.getY());
        spawnObj.addProperty("z", spawnPos.getZ());
        dimObj.add("spawn", spawnObj);
        
        // World border
        WorldBorder border = level.getWorldBorder();
        JsonObject borderObj = new JsonObject();
        borderObj.addProperty("centerX", border.getCenterX());
        borderObj.addProperty("centerZ", border.getCenterZ());
        borderObj.addProperty("size", border.getSize());
        borderObj.addProperty("damagePerBlock", border.getDamagePerBlock());
        borderObj.addProperty("warningDistance", border.getWarningBlocks());
        dimObj.add("worldBorder", borderObj);
        
        // World time
        dimObj.addProperty("dayTime", level.getDayTime());
        dimObj.addProperty("gameTime", level.getGameTime());
        
        // Weather
        dimObj.addProperty("isRaining", level.isRaining());
        dimObj.addProperty("isThundering", level.isThundering());
        dimObj.addProperty("rainLevel", level.getRainLevel(1.0f));
        dimObj.addProperty("thunderLevel", level.getThunderLevel(1.0f));
        
        // Loaded chunks
        dimObj.addProperty("loadedChunks", level.getChunkSource().getLoadedChunksCount());
        
        // Dimension type info
        dimObj.addProperty("hasSkyLight", level.dimensionType().hasSkyLight());
        dimObj.addProperty("hasCeiling", level.dimensionType().hasCeiling());
        dimObj.addProperty("ultraWarm", level.dimensionType().ultraWarm());
        dimObj.addProperty("natural", level.dimensionType().natural());
        dimObj.addProperty("minY", level.dimensionType().minY());
        dimObj.addProperty("maxY", level.dimensionType().minY() + level.dimensionType().height());
        dimObj.addProperty("height", level.dimensionType().height());
        
        return dimObj;
    }
    
    /**
     * Get dimension info by dimension key
     */
    public JsonObject getDimensionInfoJson(String dimensionKey) {
        if (server == null) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Server not initialized");
            return error;
        }
        
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().toString().equals(dimensionKey)) {
                JsonObject response = new JsonObject();
                response.addProperty("timestamp", System.currentTimeMillis());
                response.add("dimension", getDimensionInfo(level));
                return response;
            }
        }
        
        JsonObject error = new JsonObject();
        error.addProperty("error", "Dimension not found: " + dimensionKey);
        return error;
    }
    
    /**
     * Get list of all dimensions
     */
    public JsonObject getDimensionsListJson() {
        if (server == null) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Server not initialized");
            return error;
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("timestamp", System.currentTimeMillis());
        
        JsonArray dimensionsArray = new JsonArray();
        for (ServerLevel level : server.getAllLevels()) {
            JsonObject dimObj = new JsonObject();
            String dimensionKey = level.dimension().location().toString();
            dimObj.addProperty("key", dimensionKey);
            dimObj.addProperty("name", getDimensionName(dimensionKey));
            dimObj.addProperty("playerCount", level.players().size());
            dimObj.addProperty("loadedChunks", level.getChunkSource().getLoadedChunksCount());
            dimensionsArray.add(dimObj);
        }
        
        response.add("dimensions", dimensionsArray);
        response.addProperty("dimensionCount", dimensionsArray.size());
        
        return response;
    }
    
    /**
     * Get spawn points for all dimensions
     */
    public JsonObject getSpawnPointsJson() {
        if (server == null) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Server not initialized");
            return error;
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("timestamp", System.currentTimeMillis());
        
        JsonArray spawnsArray = new JsonArray();
        for (ServerLevel level : server.getAllLevels()) {
            JsonObject spawnObj = new JsonObject();
            String dimensionKey = level.dimension().location().toString();
            spawnObj.addProperty("dimension", dimensionKey);
            spawnObj.addProperty("name", getDimensionName(dimensionKey));
            
            BlockPos spawnPos = level.getSharedSpawnPos();
            spawnObj.addProperty("x", spawnPos.getX());
            spawnObj.addProperty("y", spawnPos.getY());
            spawnObj.addProperty("z", spawnPos.getZ());
            
            spawnsArray.add(spawnObj);
        }
        
        response.add("spawns", spawnsArray);
        return response;
    }
    
    /**
     * Get user-friendly dimension name
     */
    private String getDimensionName(String dimensionKey) {
        if (dimensionKey.contains("overworld")) return "Overworld";
        if (dimensionKey.contains("nether")) return "The Nether";
        if (dimensionKey.contains("end")) return "The End";
        
        // Extract last part after colon for custom dimensions
        int colonIndex = dimensionKey.lastIndexOf(':');
        if (colonIndex >= 0 && colonIndex < dimensionKey.length() - 1) {
            String name = dimensionKey.substring(colonIndex + 1);
            // Capitalize first letter and replace underscores with spaces
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            return name.replace('_', ' ');
        }
        
        return dimensionKey;
    }
}
