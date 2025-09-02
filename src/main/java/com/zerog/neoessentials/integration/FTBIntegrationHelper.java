package com.zerog.neoessentials.integration;

import net.neoforged.fml.ModList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive FTB integration helper for Teams, Chunks, Library, and Ranks
 * Provides safe API access with fallback support and caching
 */
public class FTBIntegrationHelper {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FTBIntegrationHelper.class);
    
    // Cache for team data to avoid frequent API calls
    private static final Map<UUID, TeamInfo> teamCache = new ConcurrentHashMap<>();
    private static final Map<UUID, RankInfo> rankCache = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> teamCacheTime = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> rankCacheTime = new ConcurrentHashMap<>();
    
    private static final long CACHE_DURATION = 30000; // 30 seconds cache
    private static boolean integrationsChecked = false;
    private static boolean ftbTeamsAvailable = false;
    private static boolean ftbRanksAvailable = false;
    private static boolean ftbLibraryAvailable = false;
    private static boolean ftbChunksAvailable = false;
    
    // Team and rank info data classes
    public static class TeamInfo {
        public final String teamName;
        public final String teamDisplayName;
        public final String teamPrefix;
        public final String teamSuffix;
        public final List<String> teamMembers;
        public final boolean isTeamOwner;
        public final boolean isTeamModerator;
        public final String teamColor;
        
        public TeamInfo(String name, String displayName, String prefix, String suffix, 
                       List<String> members, boolean owner, boolean moderator, String color) {
            this.teamName = name != null ? name : "";
            this.teamDisplayName = displayName != null ? displayName : "";
            this.teamPrefix = prefix != null ? prefix : "";
            this.teamSuffix = suffix != null ? suffix : "";
            this.teamMembers = members != null ? members : new ArrayList<>();
            this.isTeamOwner = owner;
            this.isTeamModerator = moderator;
            this.teamColor = color != null ? color : "";
        }
    }
    
    public static class RankInfo {
        public final String rankName;
        public final String rankDisplayName;
        public final String rankPrefix;
        public final String rankSuffix;
        public final String rankColor;
        public final int rankWeight;
        public final List<String> rankPermissions;
        
        public RankInfo(String name, String displayName, String prefix, String suffix, 
                       String color, int weight, List<String> permissions) {
            this.rankName = name != null ? name : "";
            this.rankDisplayName = displayName != null ? displayName : "";
            this.rankPrefix = prefix != null ? prefix : "";
            this.rankSuffix = suffix != null ? suffix : "";
            this.rankColor = color != null ? color : "";
            this.rankWeight = weight;
            this.rankPermissions = permissions != null ? permissions : new ArrayList<>();
        }
    }
    
    /**
     * Initialize FTB integrations and check availability
     */
    public static void initializeIntegrations() {
        if (integrationsChecked) return;
        
        try {
            ftbTeamsAvailable = ModList.get().isLoaded("ftbteams");
            ftbRanksAvailable = ModList.get().isLoaded("ftbranks");
            ftbLibraryAvailable = ModList.get().isLoaded("ftblibrary");
            ftbChunksAvailable = ModList.get().isLoaded("ftbchunks");
            
            LOGGER.info("FTB Integration Status:");
            LOGGER.info("  - FTB Teams: {}", ftbTeamsAvailable ? "Available" : "Not available");
            LOGGER.info("  - FTB Ranks: {}", ftbRanksAvailable ? "Available" : "Not available");
            LOGGER.info("  - FTB Library: {}", ftbLibraryAvailable ? "Available" : "Not available");
            LOGGER.info("  - FTB Chunks: {}", ftbChunksAvailable ? "Available" : "Not available");
            
            integrationsChecked = true;
        } catch (Exception e) {
            LOGGER.error("Failed to initialize FTB integrations", e);
        }
    }
    
    public static boolean isFTBTeamsLoaded() {
        if (!integrationsChecked) initializeIntegrations();
        return ftbTeamsAvailable;
    }

    public static boolean isFTBRanksLoaded() {
        if (!integrationsChecked) initializeIntegrations();
        return ftbRanksAvailable;
    }

    public static boolean isFTBLibraryLoaded() {
        if (!integrationsChecked) initializeIntegrations();
        return ftbLibraryAvailable;
    }
    
    public static boolean isFTBChunksLoaded() {
        if (!integrationsChecked) initializeIntegrations();
        return ftbChunksAvailable;
    }
    
    /**
     * Get team information for a player with caching
     */
    public static TeamInfo getTeamInfo(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        // Check cache first
        Long cacheTime = teamCacheTime.get(playerId);
        if (cacheTime != null && System.currentTimeMillis() - cacheTime < CACHE_DURATION) {
            TeamInfo cached = teamCache.get(playerId);
            if (cached != null) return cached;
        }
        
        TeamInfo teamInfo = fetchTeamInfo(player);
        
        // Cache the result
        teamCache.put(playerId, teamInfo);
        teamCacheTime.put(playerId, System.currentTimeMillis());
        
        return teamInfo;
    }
    
    /**
     * Get rank information for a player with caching
     */
    public static RankInfo getRankInfo(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        // Check cache first
        Long cacheTime = rankCacheTime.get(playerId);
        if (cacheTime != null && System.currentTimeMillis() - cacheTime < CACHE_DURATION) {
            RankInfo cached = rankCache.get(playerId);
            if (cached != null) return cached;
        }
        
        RankInfo rankInfo = fetchRankInfo(player);
        
        // Cache the result
        rankCache.put(playerId, rankInfo);
        rankCacheTime.put(playerId, System.currentTimeMillis());
        
        return rankInfo;
    }
    
    /**
     * Fetch team information from FTB Teams API
     */
    private static TeamInfo fetchTeamInfo(ServerPlayer player) {
        if (!isFTBTeamsLoaded()) {
            return new TeamInfo("", "", "", "", new ArrayList<>(), false, false, "");
        }
        
        try {
            // Use reflection to safely access FTB Teams API
            Class<?> teamManagerClass = Class.forName("dev.ftb.mods.ftbteams.data.TeamManager");
            Class<?> teamClass = Class.forName("dev.ftb.mods.ftbteams.data.Team");
            
            Object teamManager = teamManagerClass.getMethod("instance").invoke(null);
            Object team = teamManagerClass.getMethod("getTeamForPlayer", UUID.class)
                    .invoke(teamManager, player.getUUID());
            
            if (team == null) {
                return new TeamInfo("", "", "", "", new ArrayList<>(), false, false, "");
            }
            
            // Extract team information
            String teamName = (String) teamClass.getMethod("getStringID").invoke(team);
            String displayName = (String) teamClass.getMethod("getDisplayName").invoke(team);
            
            // Get team members
            Object members = teamClass.getMethod("getMembers").invoke(team);
            List<String> memberNames = new ArrayList<>();
            
            if (members instanceof Collection<?>) {
                for (Object member : (Collection<?>) members) {
                    try {
                        String memberName = member.toString();
                        memberNames.add(memberName);
                    } catch (Exception e) {
                        // Skip invalid member
                    }
                }
            }
            
            // Check if player is owner or moderator
            boolean isOwner = false;
            boolean isModerator = false;
            
            try {
                Object owner = teamClass.getMethod("getOwner").invoke(team);
                if (owner != null && owner.equals(player.getUUID())) {
                    isOwner = true;
                }
                
                // Check moderator status if available
                try {
                    Object moderators = teamClass.getMethod("getModerators").invoke(team);
                    if (moderators instanceof Collection<?> && ((Collection<?>) moderators).contains(player.getUUID())) {
                        isModerator = true;
                    }
                } catch (Exception e) {
                    // Moderator check not available
                }
            } catch (Exception e) {
                // Owner check failed
            }
            
            // Get team color if available
            String teamColor = "";
            try {
                Object color = teamClass.getMethod("getColor").invoke(team);
                if (color != null) {
                    teamColor = color.toString();
                }
            } catch (Exception e) {
                // Color not available
            }
            
            return new TeamInfo(teamName, displayName, "", "", memberNames, isOwner, isModerator, teamColor);
            
        } catch (Exception e) {
            LOGGER.debug("Failed to fetch team info for player {}: {}", player.getName().getString(), e.getMessage());
            return new TeamInfo("", "", "", "", new ArrayList<>(), false, false, "");
        }
    }
    
    /**
     * Fetch rank information from FTB Ranks API
     */
    private static RankInfo fetchRankInfo(ServerPlayer player) {
        if (!isFTBRanksLoaded()) {
            return new RankInfo("", "", "", "", "", 0, new ArrayList<>());
        }
        
        try {
            // Use reflection to safely access FTB Ranks API
            Class<?> rankManagerClass = Class.forName("dev.ftb.mods.ftbranks.api.RankManager");
            Class<?> rankClass = Class.forName("dev.ftb.mods.ftbranks.api.Rank");
            
            Object rankManager = rankManagerClass.getMethod("get").invoke(null);
            Object rank = rankManagerClass.getMethod("getRank", UUID.class)
                    .invoke(rankManager, player.getUUID());
            
            if (rank == null) {
                return new RankInfo("", "", "", "", "", 0, new ArrayList<>());
            }
            
            // Extract rank information
            String rankName = (String) rankClass.getMethod("getId").invoke(rank);
            String displayName = (String) rankClass.getMethod("getDisplayName").invoke(rank);
            
            // Get prefix and suffix
            String prefix = "";
            String suffix = "";
            try {
                prefix = (String) rankClass.getMethod("getPrefix").invoke(rank);
                suffix = (String) rankClass.getMethod("getSuffix").invoke(rank);
            } catch (Exception e) {
                // Prefix/suffix not available
            }
            
            // Get rank color
            String color = "";
            try {
                Object colorObj = rankClass.getMethod("getColor").invoke(rank);
                if (colorObj != null) {
                    color = colorObj.toString();
                }
            } catch (Exception e) {
                // Color not available
            }
            
            // Get rank weight
            int weight = 0;
            try {
                weight = (Integer) rankClass.getMethod("getWeight").invoke(rank);
            } catch (Exception e) {
                // Weight not available
            }
            
            // Get permissions
            List<String> permissions = new ArrayList<>();
            try {
                Object permissionSet = rankClass.getMethod("getPermissions").invoke(rank);
                if (permissionSet instanceof Collection<?>) {
                    for (Object perm : (Collection<?>) permissionSet) {
                        permissions.add(perm.toString());
                    }
                }
            } catch (Exception e) {
                // Permissions not available
            }
            
            return new RankInfo(rankName, displayName, prefix, suffix, color, weight, permissions);
            
        } catch (Exception e) {
            LOGGER.debug("Failed to fetch rank info for player {}: {}", player.getName().getString(), e.getMessage());
            return new RankInfo("", "", "", "", "", 0, new ArrayList<>());
        }
    }
    
    /**
     * Clear cache for a specific player
     */
    public static void clearPlayerCache(UUID playerId) {
        teamCache.remove(playerId);
        rankCache.remove(playerId);
        teamCacheTime.remove(playerId);
        rankCacheTime.remove(playerId);
    }
    
    /**
     * Clear all caches
     */
    public static void clearAllCaches() {
        teamCache.clear();
        rankCache.clear();
        teamCacheTime.clear();
        rankCacheTime.clear();
    }
    
    /**
     * Check if player has FTB chunks in an area
     */
    public static boolean hasChunksAtLocation(ServerPlayer player, int chunkX, int chunkZ) {
        if (!isFTBChunksLoaded()) {
            return false;
        }
        
        try {
            // Use reflection to safely access FTB Chunks API
            Class<?> chunkManagerClass = Class.forName("dev.ftb.mods.ftbchunks.data.ClaimedChunkManager");
            Object chunkManager = chunkManagerClass.getMethod("getInstance").invoke(null);
            
            Object claimedChunk = chunkManagerClass.getMethod("getChunk", int.class, int.class, UUID.class)
                    .invoke(chunkManager, chunkX, chunkZ, player.level().dimension().location());
            
            if (claimedChunk == null) return false;
            
            // Check if the chunk belongs to the player's team
            TeamInfo teamInfo = getTeamInfo(player);
            if (teamInfo.teamName.isEmpty()) {
                // Check if claimed by player directly
                Object owner = claimedChunk.getClass().getMethod("getTeamId").invoke(claimedChunk);
                return player.getUUID().equals(owner);
            } else {
                // Check if claimed by player's team
                Object teamId = claimedChunk.getClass().getMethod("getTeamId").invoke(claimedChunk);
                return teamInfo.teamName.equals(teamId.toString());
            }
            
        } catch (Exception e) {
            LOGGER.debug("Failed to check chunks for player {}: {}", player.getName().getString(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the effective prefix for a player (combines FTB rank prefix with team prefix)
     */
    public static String getEffectivePrefix(ServerPlayer player) {
        StringBuilder prefix = new StringBuilder();
        
        // Add FTB rank prefix if available
        if (isFTBRanksLoaded()) {
            RankInfo rankInfo = getRankInfo(player);
            if (!rankInfo.rankPrefix.isEmpty()) {
                prefix.append(rankInfo.rankPrefix);
            }
        }
        
        // Add team prefix if available
        if (isFTBTeamsLoaded()) {
            TeamInfo teamInfo = getTeamInfo(player);
            if (!teamInfo.teamPrefix.isEmpty()) {
                if (prefix.length() > 0) prefix.append(" ");
                prefix.append(teamInfo.teamPrefix);
            }
        }
        
        return prefix.toString();
    }
    
    /**
     * Get the effective suffix for a player (combines FTB rank suffix with team suffix)
     */
    public static String getEffectiveSuffix(ServerPlayer player) {
        StringBuilder suffix = new StringBuilder();
        
        // Add team suffix if available
        if (isFTBTeamsLoaded()) {
            TeamInfo teamInfo = getTeamInfo(player);
            if (!teamInfo.teamSuffix.isEmpty()) {
                suffix.append(teamInfo.teamSuffix);
            }
        }
        
        // Add FTB rank suffix if available
        if (isFTBRanksLoaded()) {
            RankInfo rankInfo = getRankInfo(player);
            if (!rankInfo.rankSuffix.isEmpty()) {
                if (suffix.length() > 0) suffix.append(" ");
                suffix.append(rankInfo.rankSuffix);
            }
        }
        
        return suffix.toString();
    }
    
    // Safe wrapper methods for commands
    public static void safeTeamInfo(ServerPlayer player) {
        try {
            if (isFTBTeamsLoaded()) {
                TeamInfo teamInfo = getTeamInfo(player);
                if (!teamInfo.teamName.isEmpty()) {
                    player.sendSystemMessage(Component.literal("§6Team Information:"));
                    player.sendSystemMessage(Component.literal("§7Name: §f" + teamInfo.teamDisplayName));
                    player.sendSystemMessage(Component.literal("§7Members: §f" + String.join(", ", teamInfo.teamMembers)));
                    player.sendSystemMessage(Component.literal("§7Role: §f" + 
                        (teamInfo.isTeamOwner ? "Owner" : teamInfo.isTeamModerator ? "Moderator" : "Member")));
                    if (!teamInfo.teamColor.isEmpty()) {
                        player.sendSystemMessage(Component.literal("§7Color: §f" + teamInfo.teamColor));
                    }
                } else {
                    player.sendSystemMessage(Component.literal("§cYou are not in a team."));
                }
            } else {
                player.sendSystemMessage(Component.literal("§cFTB Teams is not installed."));
            }
        } catch (Exception e) {
            LOGGER.error("Error displaying team info", e);
            player.sendSystemMessage(Component.literal("§cError retrieving team information."));
        }
    }

    public static void safeRankInfo(ServerPlayer player) {
        try {
            if (isFTBRanksLoaded()) {
                RankInfo rankInfo = getRankInfo(player);
                if (!rankInfo.rankName.isEmpty()) {
                    player.sendSystemMessage(Component.literal("§6Rank Information:"));
                    player.sendSystemMessage(Component.literal("§7Rank: §f" + rankInfo.rankDisplayName));
                    if (!rankInfo.rankPrefix.isEmpty()) {
                        player.sendSystemMessage(Component.literal("§7Prefix: §f" + rankInfo.rankPrefix));
                    }
                    if (!rankInfo.rankSuffix.isEmpty()) {
                        player.sendSystemMessage(Component.literal("§7Suffix: §f" + rankInfo.rankSuffix));
                    }
                    player.sendSystemMessage(Component.literal("§7Weight: §f" + rankInfo.rankWeight));
                    player.sendSystemMessage(Component.literal("§7Permissions: §f" + rankInfo.rankPermissions.size()));
                } else {
                    player.sendSystemMessage(Component.literal("§cNo rank information available."));
                }
            } else {
                player.sendSystemMessage(Component.literal("§cFTB Ranks is not installed."));
            }
        } catch (Exception e) {
            LOGGER.error("Error displaying rank info", e);
            player.sendSystemMessage(Component.literal("§cError retrieving rank information."));
        }
    }
    
    /**
     * Get the number of claimed chunks for a player
     */
    public static int getClaimedChunksCount(ServerPlayer player) {
        if (!isFTBChunksLoaded()) {
            return 0;
        }
        
        try {
            // Use reflection to safely access FTB Chunks API
            Class<?> chunkManagerClass = Class.forName("dev.ftb.mods.ftbchunks.data.ClaimedChunkManager");
            Object chunkManager = chunkManagerClass.getMethod("getInstance").invoke(null);
            
            // Get player's team info to determine ownership
            TeamInfo teamInfo = getTeamInfo(player);
            Object teamId;
            
            if (teamInfo.teamName.isEmpty()) {
                // Player doesn't have a team, use player UUID
                teamId = player.getUUID();
            } else {
                // Player has a team, get team ID
                Class<?> teamManagerClass = Class.forName("dev.ftb.mods.ftbteams.data.TeamManager");
                Object teamManager = teamManagerClass.getMethod("getInstance").invoke(null);
                Object team = teamManagerClass.getMethod("getTeamByName", String.class).invoke(teamManager, teamInfo.teamName);
                teamId = team != null ? team.getClass().getMethod("getId").invoke(team) : player.getUUID();
            }
            
            // Get all claimed chunks for the team/player
            Object claimedChunks = chunkManagerClass.getMethod("getClaimedChunks", Object.class).invoke(chunkManager, teamId);
            
            if (claimedChunks instanceof Collection) {
                return ((Collection<?>) claimedChunks).size();
            }
            
            return 0;
            
        } catch (Exception e) {
            LOGGER.debug("Failed to get claimed chunks count for player {}: {}", player.getName().getString(), e.getMessage());
            return 0;
        }
    }
    
    /**
     * Get the number of loaded chunks for a player
     */
    public static int getLoadedChunksCount(ServerPlayer player) {
        if (!isFTBChunksLoaded()) {
            return 0;
        }
        
        try {
            // Use reflection to safely access FTB Chunks API
            Class<?> chunkManagerClass = Class.forName("dev.ftb.mods.ftbchunks.data.ClaimedChunkManager");
            Object chunkManager = chunkManagerClass.getMethod("getInstance").invoke(null);
            
            // Get player's team info to determine ownership
            TeamInfo teamInfo = getTeamInfo(player);
            Object teamId;
            
            if (teamInfo.teamName.isEmpty()) {
                // Player doesn't have a team, use player UUID
                teamId = player.getUUID();
            } else {
                // Player has a team, get team ID
                Class<?> teamManagerClass = Class.forName("dev.ftb.mods.ftbteams.data.TeamManager");
                Object teamManager = teamManagerClass.getMethod("getInstance").invoke(null);
                Object team = teamManagerClass.getMethod("getTeamByName", String.class).invoke(teamManager, teamInfo.teamName);
                teamId = team != null ? team.getClass().getMethod("getId").invoke(team) : player.getUUID();
            }
            
            // Get all loaded chunks for the team/player
            Object loadedChunks = chunkManagerClass.getMethod("getLoadedChunks", Object.class).invoke(chunkManager, teamId);
            
            if (loadedChunks instanceof Collection) {
                return ((Collection<?>) loadedChunks).size();
            }
            
            return 0;
            
        } catch (Exception e) {
            LOGGER.debug("Failed to get loaded chunks count for player {}: {}", player.getName().getString(), e.getMessage());
            return 0;
        }
    }
}
