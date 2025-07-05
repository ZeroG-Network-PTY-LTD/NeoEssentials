package com.zerog.neoessentials.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced performance monitoring utility for NeoEssentials.
 * Provides real-time server performance metrics, memory usage tracking,
 * and player activity monitoring for administrative purposes.
 */
public class PerformanceMonitor {
    
    private static final int TPS_SAMPLE_SIZE = 20;
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final long MILLIS_PER_TICK = 50L; // 20 TPS = 50ms per tick
    
    // TPS tracking
    private static final List<Long> tickTimeHistory = Collections.synchronizedList(new ArrayList<>());
    private static final AtomicLong lastTickTime = new AtomicLong(System.nanoTime());
    private static long serverStartTime = System.currentTimeMillis();
    
    // Memory monitoring
    // MemoryMXBean can be used for advanced memory monitoring if needed
    
    /**
     * Records a server tick for TPS calculation.
     * Should be called at the end of each server tick.
     */
    public static void recordTick() {
        long currentTime = System.nanoTime();
        long previousTime = lastTickTime.getAndSet(currentTime);
        long tickDuration = currentTime - previousTime;
        
        synchronized (tickTimeHistory) {
            tickTimeHistory.add(tickDuration);
            
            // Keep only the last TPS_SAMPLE_SIZE entries
            if (tickTimeHistory.size() > TPS_SAMPLE_SIZE) {
                tickTimeHistory.remove(0);
            }
        }
    }
    
    /**
     * Calculates the current TPS (Ticks Per Second).
     * 
     * @return Current TPS as a double value
     */
    public static double getCurrentTPS() {
        synchronized (tickTimeHistory) {
            if (tickTimeHistory.isEmpty()) {
                return 20.0; // Default assumption
            }
            
            long totalNanos = tickTimeHistory.stream()
                .mapToLong(Long::longValue)
                .sum();
            
            double averageNanos = (double) totalNanos / tickTimeHistory.size();
            double tps = NANOS_PER_SECOND / averageNanos;
            
            // Cap at 20 TPS
            return Math.min(20.0, tps);
        }
    }
    
    /**
     * Gets the average tick time in milliseconds.
     * 
     * @return Average tick time in milliseconds
     */
    public static double getAverageTickTime() {
        synchronized (tickTimeHistory) {
            if (tickTimeHistory.isEmpty()) {
                return MILLIS_PER_TICK; // Default 50ms
            }
            
            long totalNanos = tickTimeHistory.stream()
                .mapToLong(Long::longValue)
                .sum();
            
            return (double) totalNanos / tickTimeHistory.size() / 1_000_000.0;
        }
    }
    
    /**
     * Gets memory usage information.
     * 
     * @return MemoryInfo object containing current memory statistics
     */
    public static MemoryInfo getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return new MemoryInfo(maxMemory, totalMemory, usedMemory, freeMemory);
    }
    
    /**
     * Gets detailed server performance information.
     * 
     * @param server The Minecraft server instance
     * @return ServerPerformanceInfo object containing comprehensive metrics
     */
    public static ServerPerformanceInfo getServerPerformance(MinecraftServer server) {
        MemoryInfo memory = getMemoryInfo();
        double tps = getCurrentTPS();
        double avgTickTime = getAverageTickTime();
        long uptime = System.currentTimeMillis() - serverStartTime;
        
        // Count loaded chunks across all dimensions
        int totalChunks = 0;
        int totalEntities = 0;
        int totalPlayers = server.getPlayerCount();
        
        for (ServerLevel level : server.getAllLevels()) {
            totalChunks += level.getChunkSource().getLoadedChunksCount();
            // Approximate entity count - this is less accurate but works with the API
            totalEntities += level.players().size() * 10; // Rough estimation
        }
        
        return new ServerPerformanceInfo(
            tps, avgTickTime, memory, uptime, 
            totalChunks, totalEntities, totalPlayers
        );
    }
    
    /**
     * Gets player-specific performance information.
     * 
     * @param player The player to analyze
     * @return PlayerPerformanceInfo object containing player-specific metrics
     */
    public static PlayerPerformanceInfo getPlayerPerformance(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        int nearbyEntities = level.getEntitiesOfClass(
            net.minecraft.world.entity.Entity.class,
            player.getBoundingBox().inflate(32.0)
        ).size();
        
        // Get chunk coordinates
        int chunkX = player.chunkPosition().x;
        int chunkZ = player.chunkPosition().z;
        
        return new PlayerPerformanceInfo(
            player.getName().getString(),
            player.getUUID(),
            level.dimension().location().toString(),
            chunkX, chunkZ,
            nearbyEntities,
            (float) player.connection.latency()
        );
    }
    
    /**
     * Resets the server start time (useful for tracking uptime from specific points).
     */
    public static void resetServerStartTime() {
        serverStartTime = System.currentTimeMillis();
    }
    
    /**
     * Data class for memory information.
     */
    public static class MemoryInfo {
        public final long maxMemory;
        public final long totalMemory;
        public final long usedMemory;
        public final long freeMemory;
        public final double usagePercentage;
        
        public MemoryInfo(long maxMemory, long totalMemory, long usedMemory, long freeMemory) {
            this.maxMemory = maxMemory;
            this.totalMemory = totalMemory;
            this.usedMemory = usedMemory;
            this.freeMemory = freeMemory;
            this.usagePercentage = (double) usedMemory / maxMemory * 100.0;
        }
        
        public String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            int exp = (int) (Math.log(bytes) / Math.log(1024));
            String pre = "KMGTPE".charAt(exp - 1) + "";
            return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
        }
    }
    
    /**
     * Data class for comprehensive server performance information.
     */
    public static class ServerPerformanceInfo {
        public final double tps;
        public final double averageTickTime;
        public final MemoryInfo memory;
        public final long uptimeMillis;
        public final int loadedChunks;
        public final int totalEntities;
        public final int onlinePlayers;
        
        public ServerPerformanceInfo(double tps, double averageTickTime, MemoryInfo memory, 
                                   long uptimeMillis, int loadedChunks, int totalEntities, int onlinePlayers) {
            this.tps = tps;
            this.averageTickTime = averageTickTime;
            this.memory = memory;
            this.uptimeMillis = uptimeMillis;
            this.loadedChunks = loadedChunks;
            this.totalEntities = totalEntities;
            this.onlinePlayers = onlinePlayers;
        }
        
        public String getFormattedUptime() {
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
    }
    
    /**
     * Data class for player-specific performance information.
     */
    public static class PlayerPerformanceInfo {
        public final String playerName;
        public final java.util.UUID playerId;
        public final String dimension;
        public final int chunkX;
        public final int chunkZ;
        public final int nearbyEntities;
        public final float ping;
        
        public PlayerPerformanceInfo(String playerName, java.util.UUID playerId, String dimension,
                                   int chunkX, int chunkZ, int nearbyEntities, float ping) {
            this.playerName = playerName;
            this.playerId = playerId;
            this.dimension = dimension;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.nearbyEntities = nearbyEntities;
            this.ping = ping;
        }
    }
}
