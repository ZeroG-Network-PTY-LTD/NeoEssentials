package com.zerog.neoessentials.webdashboard.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.text.DecimalFormat;

/**
 * Server Data Collector
 * Collects all server-related data for the Dashboard API
 * 
 * Endpoints served:
 * - Server Profiles (version, mods, config)
 * - Server Statistics (TPS, memory, CPU)
 * - Server Status (online/offline, uptime)
 * - Server Health (performance metrics)
 * - Server World Information (worlds, dimensions)
 */
public class ServerDataCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerDataCollector.class);
    private final MinecraftServer server;
    private final DecimalFormat df = new DecimalFormat("#.##");
    
    public ServerDataCollector(MinecraftServer server) {
        this.server = server;
        LOGGER.debug("ServerDataCollector initialized");
    }
    
    /**
     * Get complete server profile
     * Endpoint: GET /api/server/profile
     */
    public JsonObject getServerProfile() {
        JsonObject profile = new JsonObject();
        
        profile.addProperty("serverName", server.getServerModName());
        profile.addProperty("motd", server.getMotd());
        profile.addProperty("minecraftVersion", server.getServerVersion());
        
        // Get NeoForge version dynamically from mod list
        String neoforgeVersion = "Unknown";
        try {
            var neoforgeModOpt = net.neoforged.fml.ModList.get().getModContainerById("neoforge");
            if (neoforgeModOpt.isPresent()) {
                neoforgeVersion = "NeoForge " + neoforgeModOpt.get().getModInfo().getVersion().toString();
            }
        } catch (Exception e) {
            neoforgeVersion = "NeoForge (version unavailable)";
        }
        profile.addProperty("modVersion", neoforgeVersion);
        profile.addProperty("neoforgeVersion", neoforgeVersion);
        
        profile.addProperty("gameVersion", "1.21.1");
        profile.addProperty("difficulty", server.getWorldData().getDifficulty().getKey());
        profile.addProperty("hardcore", server.getWorldData().isHardcore());
        profile.addProperty("maxPlayers", server.getMaxPlayers());
        profile.addProperty("pvpEnabled", server.isPvpAllowed());
        profile.addProperty("onlineMode", server.usesAuthentication());
        profile.addProperty("commandBlocksEnabled", server.isCommandBlockEnabled());
        
        // Installed mods
        JsonArray mods = new JsonArray();
        net.neoforged.fml.ModList.get().getMods().forEach(modInfo -> {
            JsonObject mod = new JsonObject();
            mod.addProperty("id", modInfo.getModId());
            mod.addProperty("name", modInfo.getDisplayName());
            mod.addProperty("version", modInfo.getVersion().toString());
            mods.add(mod);
        });
        profile.add("mods", mods);
        profile.addProperty("modCount", mods.size());
        profile.addProperty("modsLoaded", mods.size()); // For frontend compatibility
        
        return profile;
    }
    
    /**
     * Get server statistics
     * Endpoint: GET /api/server/statistics
     */
    public JsonObject getServerStatistics() {
        JsonObject stats = new JsonObject();
        
        // TPS (Ticks Per Second)
        double avgTickTime = server.getAverageTickTimeNanos() / 1_000_000.0; // Convert to ms
        double tps = Math.min(20.0, 1000.0 / Math.max(50.0, avgTickTime));
        stats.addProperty("tps", df.format(tps));
        stats.addProperty("averageTickTime", df.format(avgTickTime));
        stats.addProperty("tpsPercent", df.format((tps / 20.0) * 100));
        
        // Memory statistics
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        JsonObject memory = new JsonObject();
        memory.addProperty("used", formatBytes(usedMemory));
        memory.addProperty("free", formatBytes(freeMemory));
        memory.addProperty("allocated", formatBytes(totalMemory));
        memory.addProperty("max", formatBytes(maxMemory));
        memory.addProperty("usedMB", usedMemory / (1024 * 1024));
        memory.addProperty("maxMB", maxMemory / (1024 * 1024));
        memory.addProperty("usedPercent", df.format((double) usedMemory / maxMemory * 100));
        stats.add("memory", memory);
        
        // CPU statistics
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        JsonObject cpu = new JsonObject();
        cpu.addProperty("processors", osBean.getAvailableProcessors());
        cpu.addProperty("loadAverage", df.format(osBean.getSystemLoadAverage()));
        stats.add("cpu", cpu);
        
        // Player statistics
        stats.addProperty("playersOnline", server.getPlayerCount());
        stats.addProperty("playersMax", server.getMaxPlayers());
        
        // World statistics
        int worldCount = 0;
        for (@SuppressWarnings("unused") var level : server.getAllLevels()) {
            worldCount++;
        }
        stats.addProperty("worldsLoaded", worldCount);
        
        // Chunk statistics
        JsonArray worldChunks = new JsonArray();
        final int[] totalLoadedChunks = {0}; // Use array to allow modification in lambda
        server.getAllLevels().forEach(level -> {
            JsonObject worldChunk = new JsonObject();
            worldChunk.addProperty("dimension", level.dimension().location().toString());
            
            // Count ONLY fully loaded chunks by checking visible chunk count
            int loadedChunks = 0;
            try {
                var chunkSource = level.getChunkSource();
                // Use the visible chunk count which represents actually loaded chunks
                loadedChunks = chunkSource.chunkMap.size();
                
                // If that's 0 and we have players, something is wrong - use a more accurate count
                if (loadedChunks == 0 && server.getPlayerCount() > 0) {
                    // Try counting by player view distance
                    int viewDistance = server.getPlayerList().getViewDistance();
                    int playersInDim = 0;
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        if (player.level().dimension().location().toString().equals(level.dimension().location().toString())) {
                            playersInDim++;
                        }
                    }
                    // Approximate: each player loads roughly (viewDistance * 2)^2 chunks
                    if (playersInDim > 0) {
                        loadedChunks = playersInDim * (viewDistance * 2) * (viewDistance * 2);
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("Failed to count chunks for statistics: {}", e.getMessage());
                loadedChunks = 0;
            }
            
            worldChunk.addProperty("loadedChunks", loadedChunks);
            worldChunks.add(worldChunk);
            totalLoadedChunks[0] += loadedChunks;
        });
        stats.add("chunks", worldChunks);
        stats.addProperty("totalLoadedChunks", totalLoadedChunks[0]);
        
        return stats;
    }
    
    /**
     * Get server status
     * Endpoint: GET /api/server/status
     */
    public JsonObject getServerStatus() {
        JsonObject status = new JsonObject();
        
        status.addProperty("online", !server.isStopped());
        status.addProperty("playersOnline", server.getPlayerCount());
        status.addProperty("playersMax", server.getMaxPlayers());
        
        // Uptime
        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        status.addProperty("uptimeMillis", uptimeMillis);
        status.addProperty("uptimeFormatted", formatUptime(uptimeMillis));
        
        // TPS
        double avgTickTime = server.getAverageTickTimeNanos() / 1_000_000.0;
        double tps = Math.min(20.0, 1000.0 / Math.max(50.0, avgTickTime));
        status.addProperty("tps", df.format(tps));
        
        // Health indicator
        String health = "healthy";
        if (tps < 15) health = "struggling";
        if (tps < 10) health = "critical";
        status.addProperty("health", health);
        
        return status;
    }
    
    /**
     * Get server health metrics
     * Endpoint: GET /api/server/health
     */
    public JsonObject getServerHealth() {
        JsonObject health = new JsonObject();
        
        // TPS Health
        double avgTickTime = server.getAverageTickTimeNanos() / 1_000_000.0;
        double tps = Math.min(20.0, 1000.0 / Math.max(50.0, avgTickTime));
        JsonObject tpsHealth = new JsonObject();
        tpsHealth.addProperty("value", df.format(tps));
        tpsHealth.addProperty("status", tps >= 18 ? "good" : tps >= 15 ? "warning" : "critical");
        tpsHealth.addProperty("percentage", df.format((tps / 20.0) * 100));
        health.add("tps", tpsHealth);
        
        // Memory Health
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double memoryPercent = (double) usedMemory / maxMemory * 100;
        
        JsonObject memoryHealth = new JsonObject();
        memoryHealth.addProperty("used", formatBytes(usedMemory));
        memoryHealth.addProperty("max", formatBytes(maxMemory));
        memoryHealth.addProperty("percentage", df.format(memoryPercent));
        memoryHealth.addProperty("status", memoryPercent < 70 ? "good" : memoryPercent < 85 ? "warning" : "critical");
        health.add("memory", memoryHealth);
        
        // Overall health
        String overallStatus = "healthy";
        if (tps < 15 || memoryPercent > 85) overallStatus = "warning";
        if (tps < 10 || memoryPercent > 95) overallStatus = "critical";
        health.addProperty("overall", overallStatus);
        
        return health;
    }
    
    /**
     * Get server world information
     * Endpoint: GET /api/server/worlds
     */
    public JsonObject getServerWorlds() {
        LOGGER.info("=== Starting getServerWorlds data collection ===");
        JsonObject worlds = new JsonObject();
        JsonArray worldsList = new JsonArray();
        
        // Log total players first
        LOGGER.info("Total players online: {}", server.getPlayerList().getPlayers().size());
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            LOGGER.info("  - Player: {}, Dimension: {}", p.getName().getString(), p.level().dimension().location().toString());
        }
        
        server.getAllLevels().forEach(level -> {
            JsonObject world = new JsonObject();
            String dimensionKey = level.dimension().location().toString();
            LOGGER.info("Processing dimension: {}", dimensionKey);
            
            world.addProperty("dimension", dimensionKey);
            world.addProperty("name", getDimensionDisplayName(dimensionKey));
            world.addProperty("difficulty", level.getDifficulty().getKey());
            
            // Count players IN this specific dimension
            int playersInDimension = 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                String playerDim = player.level().dimension().location().toString();
                boolean matches = playerDim.equals(dimensionKey);
                LOGGER.info("  Checking player {}: dimension={}, matches={}", 
                    player.getName().getString(), playerDim, matches);
                if (matches) {
                    playersInDimension++;
                }
            }
            world.addProperty("playersInWorld", playersInDimension);
            LOGGER.info("  Final player count for {}: {}", dimensionKey, playersInDimension);
            
            // Count only chunks within simulation distance of players
            int loadedChunks = 0;
            if (playersInDimension > 0) {
                try {
                    // Count chunks based on player positions and simulation distance
                    int simulationDistance = server.getPlayerList().getSimulationDistance();
                    int chunksPerPlayer = (simulationDistance * 2 + 1) * (simulationDistance * 2 + 1);
                    
                    // Estimate: each player loads approximately chunks in simulation distance
                    loadedChunks = playersInDimension * chunksPerPlayer;
                    
                    LOGGER.info("  Estimated loaded chunks for {} ({} players × {} sim distance): {}", 
                        dimensionKey, playersInDimension, simulationDistance, loadedChunks);
                } catch (Exception e) {
                    LOGGER.warn("  Failed to count chunks for {}: {}", dimensionKey, e.getMessage());
                    loadedChunks = 0;
                }
            } else {
                LOGGER.info("  No players in {}, setting chunks to 0", dimensionKey);
            }
            world.addProperty("loadedChunks", loadedChunks);
            
            // Count ALL entities (simpler approach for debugging)
            int entityCount = 0;
            try {
                // Using Iterable size counting without explicitly using the entity variable
                var entities = level.getAllEntities();
                for (@SuppressWarnings("unused") var entity : entities) {
                    entityCount++;
                }
                LOGGER.info("  Total entities in {}: {}", dimensionKey, entityCount);
            } catch (Exception e) {
                LOGGER.warn("  Failed to count entities for {}: {}", dimensionKey, e.getMessage());
                entityCount = 0;
            }
            world.addProperty("entities", entityCount);
            
            world.addProperty("time", level.getDayTime());
            world.addProperty("raining", level.isRaining());
            world.addProperty("thundering", level.isThundering());
            
            // World spawn
            JsonObject spawn = new JsonObject();
            spawn.addProperty("x", level.getSharedSpawnPos().getX());
            spawn.addProperty("y", level.getSharedSpawnPos().getY());
            spawn.addProperty("z", level.getSharedSpawnPos().getZ());
            world.add("spawn", spawn);
            
            worldsList.add(world);
            LOGGER.info("Completed processing dimension: {}", dimensionKey);
        });
        
        worlds.add("worlds", worldsList);
        worlds.addProperty("count", worldsList.size());
        
        LOGGER.info("=== Completed getServerWorlds data collection ===");
        return worlds;
    }
    
    /**
     * Get server configuration
     * Endpoint: GET /api/server/config
     */
    public JsonObject getServerConfig() {
        JsonObject config = new JsonObject();
        
        // View distance
        config.addProperty("viewDistance", server.getPlayerList().getViewDistance());
        config.addProperty("simulationDistance", server.getPlayerList().getSimulationDistance());
        
        // Game rules (from overworld)
        var overworld = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (overworld != null) {
            JsonObject gameRules = new JsonObject();
            net.minecraft.world.level.GameRules.visitGameRuleTypes(new net.minecraft.world.level.GameRules.GameRuleTypeVisitor() {
                @Override
                public <T extends net.minecraft.world.level.GameRules.Value<T>> void visit(
                    net.minecraft.world.level.GameRules.Key<T> key, 
                    net.minecraft.world.level.GameRules.Type<T> type
                ) {
                    gameRules.addProperty(key.getId(), overworld.getGameRules().getRule(key).toString());
                }
            });
            config.add("gameRules", gameRules);
        }
        
        return config;
    }
    
    /**
     * Get server performance history
     * Endpoint: GET /api/server/performance
     */
    public JsonObject getServerPerformance() {
        JsonObject performance = new JsonObject();
        
        // Current metrics
        double avgTickTime = server.getAverageTickTimeNanos() / 1_000_000.0;
        double tps = Math.min(20.0, 1000.0 / Math.max(50.0, avgTickTime));
        performance.addProperty("currentTPS", df.format(tps));
        performance.addProperty("averageTickTime", df.format(avgTickTime));
        
        // FUTURE: Implement historical performance tracking with time-series data
        // JsonArray tpsHistory = new JsonArray();
        // performance.add("tpsHistory", tpsHistory);
        
        // JsonArray memoryHistory = new JsonArray();
        // performance.add("memoryHistory", memoryHistory);
        
        return performance;
    }
    
    // Helper methods
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    private String formatUptime(long uptimeMillis) {
        long seconds = uptimeMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    private String getDimensionDisplayName(String dimensionKey) {
        return switch (dimensionKey) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "End";
            default -> dimensionKey;
        };
    }
}
