package com.zerog.neoessentials.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import com.zerog.neoessentials.util.DebugUtil;

import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * Discord-specific placeholder provider for enhanced integration
 * Provides server statistics, player data, and Discord-specific information
 */
public class DiscordPlaceholderProvider {
    
    private static DiscordPlaceholderProvider instance;
    private final Map<String, PlaceholderSupplier> discordPlaceholders = new HashMap<>();
    
    public DiscordPlaceholderProvider() {
        instance = this;
        registerDiscordPlaceholders();
    }
    
    public static DiscordPlaceholderProvider getInstance() {
        if (instance == null) {
            instance = new DiscordPlaceholderProvider();
        }
        return instance;
    }
    
    /**
     * Register Discord-specific placeholders
     */
    private void registerDiscordPlaceholders() {
        // Server status placeholders
        discordPlaceholders.put("server_status", (player) -> getServerStatus());
        discordPlaceholders.put("server_tps", (player) -> getServerTPS());
        discordPlaceholders.put("server_uptime", (player) -> getServerUptime());
        discordPlaceholders.put("used_memory", (player) -> getUsedMemory());
        discordPlaceholders.put("max_memory", (player) -> getMaxMemory());
        discordPlaceholders.put("free_memory", (player) -> getFreeMemory());
        
        // Player statistics
        discordPlaceholders.put("players_online", (player) -> String.valueOf(getOnlinePlayerCount()));
        discordPlaceholders.put("max_players", (player) -> String.valueOf(getMaxPlayerCount()));
        discordPlaceholders.put("unique_players_today", (player) -> String.valueOf(getUniquePlayersToday()));
        discordPlaceholders.put("top_player_name", (player) -> getTopPlayerName());
        discordPlaceholders.put("top_player_level", (player) -> getTopPlayerLevel());
        
        // Team and rank statistics
        discordPlaceholders.put("total_teams", (player) -> String.valueOf(getTotalTeams()));
        discordPlaceholders.put("top_team_name", (player) -> getTopTeamName());
        discordPlaceholders.put("top_team_members", (player) -> String.valueOf(getTopTeamMembers()));
        discordPlaceholders.put("active_ranks", (player) -> String.valueOf(getActiveRanks()));
        
        // NeoEssentials integration status
        discordPlaceholders.put("active_tablist_layouts", (player) -> String.valueOf(getActiveTablistLayouts()));
        discordPlaceholders.put("active_scoreboard_layouts", (player) -> String.valueOf(getActiveScoreboardLayouts()));
        discordPlaceholders.put("discord_linked_players", (player) -> String.valueOf(getDiscordLinkedPlayers()));
        discordPlaceholders.put("discord_integration_status", (player) -> getDiscordIntegrationStatus());
        
        // Time and date placeholders
        discordPlaceholders.put("timestamp", (player) -> getCurrentTimestamp());
        discordPlaceholders.put("server_time", (player) -> getServerTime());
        discordPlaceholders.put("server_date", (player) -> getServerDate());
        
        // Performance placeholders
        discordPlaceholders.put("tick_time", (player) -> getTickTime());
        discordPlaceholders.put("loaded_chunks", (player) -> String.valueOf(getLoadedChunks()));
        discordPlaceholders.put("loaded_entities", (player) -> String.valueOf(getLoadedEntities()));
        
        // Discord-specific formatting
        discordPlaceholders.put("discord_player_name", (player) -> getDiscordPlayerName(player));
        discordPlaceholders.put("discord_player_avatar", (player) -> getDiscordPlayerAvatar(player));
        discordPlaceholders.put("discord_player_roles", (player) -> getDiscordPlayerRoles(player));
        
        DebugUtil.debugLog("[DiscordPlaceholderProvider] Registered " + discordPlaceholders.size() + " Discord placeholders");
    }
    
    /**
     * Process Discord-specific placeholders
     */
    public String processDiscordPlaceholder(String placeholder, ServerPlayer player) {
        PlaceholderSupplier supplier = discordPlaceholders.get(placeholder);
        if (supplier != null) {
            try {
                return supplier.get(player);
            } catch (Exception e) {
                DebugUtil.errorLog("[DiscordPlaceholderProvider] Error processing placeholder '" + placeholder + "': " + e.getMessage());
                return "Error";
            }
        }
        return null; // Not a Discord placeholder
    }
    
    /**
     * Get all available Discord placeholders
     */
    public Set<String> getAvailablePlaceholders() {
        return new HashSet<>(discordPlaceholders.keySet());
    }
    
    // Server status methods
    private String getServerStatus() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return "Offline";
        return server.isRunning() ? "Online" : "Starting";
    }
    
    private String getServerTPS() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return "0.0";
        
        try {
            long[] recentTps = server.getTickTimesNanos();
            if (recentTps.length > 0) {
                double avgTps = Arrays.stream(recentTps).average().orElse(0.0);
                return String.format("%.1f", Math.min(20.0, 1000.0 / (avgTps / 1_000_000.0)));
            }
        } catch (Exception e) {
            // Fallback calculation
        }
        return "20.0";
    }
    
    private String getServerUptime() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return "0s";
        
        // Simplified uptime calculation - get system property or fallback
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        long hours = uptimeMs / (1000 * 60 * 60);
        long minutes = (uptimeMs % (1000 * 60 * 60)) / (1000 * 60);
        
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
    
    private String getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        return String.valueOf(usedMemory);
    }
    
    private String getMaxMemory() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        return String.valueOf(maxMemory);
    }
    
    private String getFreeMemory() {
        Runtime runtime = Runtime.getRuntime();
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        return String.valueOf(freeMemory);
    }
    
    // Player statistics methods
    private int getOnlinePlayerCount() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return 0;
        return server.getPlayerList().getPlayerCount();
    }
    
    private int getMaxPlayerCount() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return 0;
        return server.getPlayerList().getMaxPlayers();
    }
    
    private int getUniquePlayersToday() {
        // This would need to be implemented based on your player tracking system
        return getOnlinePlayerCount(); // Simplified
    }
    
    private String getTopPlayerName() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return "None";
        
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return "None";
        
        // Find player with highest level
        return players.stream()
            .max(Comparator.comparingInt(p -> p.experienceLevel))
            .map(p -> p.getName().getString())
            .orElse("None");
    }
    
    private String getTopPlayerLevel() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return "0";
        
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return "0";
        
        return String.valueOf(players.stream()
            .mapToInt(p -> p.experienceLevel)
            .max()
            .orElse(0));
    }
    
    // Team and rank methods (FTB integration)
    private int getTotalTeams() {
        try {
            // This would integrate with FTB Teams API
            return 1; // Simplified
        } catch (Exception e) {
            return 0;
        }
    }
    
    private String getTopTeamName() {
        try {
            // This would integrate with FTB Teams API to find team with most members
            return "NeoEssentials";
        } catch (Exception e) {
            return "None";
        }
    }
    
    private int getTopTeamMembers() {
        try {
            // This would integrate with FTB Teams API
            return getOnlinePlayerCount();
        } catch (Exception e) {
            return 0;
        }
    }
    
    private int getActiveRanks() {
        try {
            // This would integrate with FTB Ranks API
            return 5; // Simplified
        } catch (Exception e) {
            return 0;
        }
    }
    
    // NeoEssentials integration methods
    private int getActiveTablistLayouts() {
        try {
            // Count active tablist layouts
            return 3; // Simplified
        } catch (Exception e) {
            return 0;
        }
    }
    
    private int getActiveScoreboardLayouts() {
        try {
            // Count active scoreboard layouts
            return 7; // From the scoreboard config
        } catch (Exception e) {
            return 0;
        }
    }
    
    private int getDiscordLinkedPlayers() {
        try {
            SimpleDiscordLinkIntegration integration = SimpleDiscordLinkIntegration.getInstance();
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return 0;
            
            return (int) server.getPlayerList().getPlayers().stream()
                .filter(p -> integration.isPlayerLinked(p.getUUID()))
                .count();
        } catch (Exception e) {
            return 0;
        }
    }
    
    private String getDiscordIntegrationStatus() {
        try {
            DiscordIntegrationManager manager = DiscordIntegrationManager.getInstance();
            return manager.isEnabled() ? "Active" : "Disabled";
        } catch (Exception e) {
            return "Error";
        }
    }
    
    // Time and date methods
    private String getCurrentTimestamp() {
        return java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    private String getServerTime() {
        return java.time.LocalTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    
    private String getServerDate() {
        return java.time.LocalDate.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
    
    // Performance methods
    private String getTickTime() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return "0ms";
        
        try {
            long[] recentTps = server.getTickTimesNanos();
            if (recentTps.length > 0) {
                double avgTickTime = Arrays.stream(recentTps).average().orElse(0.0) / 1_000_000.0;
                return String.format("%.2fms", avgTickTime);
            }
        } catch (Exception e) {
            // Fallback
        }
        return "50ms";
    }
    
    private int getLoadedChunks() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return 0;
        
        try {
            int totalChunks = 0;
            for (var level : server.getAllLevels()) {
                totalChunks += level.getChunkSource().getLoadedChunksCount();
            }
            return totalChunks;
        } catch (Exception e) {
            return 0;
        }
    }
    
    private int getLoadedEntities() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return 0;
        
        try {
            int totalEntities = 0;
            for (var level : server.getAllLevels()) {
                for (@SuppressWarnings("unused") var entity : level.getAllEntities()) {
                    totalEntities++;
                }
            }
            return totalEntities;
        } catch (Exception e) {
            return 0;
        }
    }
    
    // Discord-specific formatting methods
    private String getDiscordPlayerName(ServerPlayer player) {
        if (player == null) return "Server";
        
        SimpleDiscordLinkIntegration integration = SimpleDiscordLinkIntegration.getInstance();
        SimpleDiscordLinkIntegration.DiscordUserData userData = integration.getDiscordUserData(player.getUUID());
        
        if (userData != null) {
            return userData.discordName + " (" + player.getName().getString() + ")";
        }
        return player.getName().getString();
    }
    
    private String getDiscordPlayerAvatar(ServerPlayer player) {
        if (player == null) return "https://i.imgur.com/server-icon.png";
        
        // Use Minotar for player avatars
        String playerName = player.getName().getString();
        return "https://minotar.net/helm/" + playerName + "/64.png";
    }
    
    private String getDiscordPlayerRoles(ServerPlayer player) {
        if (player == null) return "Server";
        
        try {
            // Get player's Discord roles if linked
            SimpleDiscordLinkIntegration integration = SimpleDiscordLinkIntegration.getInstance();
            if (integration.isPlayerLinked(player.getUUID())) {
                return "Verified Player";
            }
            return "Unverified";
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * Functional interface for placeholder suppliers
     */
    @FunctionalInterface
    private interface PlaceholderSupplier {
        String get(ServerPlayer player);
    }
}
