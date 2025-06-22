package com.zerog.neoessentials.ui.tablist;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.TablistTomlConfig;
import com.zerog.neoessentials.utils.PermissionUtil;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Manages player grouping and sorting in the tablist
 */
public class TablistGroupManager {

    /**
     * Enum for different sort types
     */
    public enum SortType {
        NAME,
        RANK,
        PLAYTIME,
        CUSTOM
    }
    
    // Cache for sorted player lists
    private final Map<SortType, List<ServerPlayer>> sortedPlayerCache = new HashMap<>();
    private long lastSortTime = 0;
    
    // Sort comparators
    private final Map<SortType, Comparator<ServerPlayer>> sortComparators = new HashMap<>();
    
    /**
     * Creates a new TablistGroupManager
     */
    public TablistGroupManager() {
        initComparators();
    }
    
    /**
     * Initialize sort comparators
     */
    private void initComparators() {
        // Sort by name (alphabetical)
        sortComparators.put(SortType.NAME, Comparator.comparing(player -> player.getGameProfile().getName()));
        
        // Sort by rank (using permissions)
        sortComparators.put(SortType.RANK, (player1, player2) -> {
            int rank1 = getPlayerRankWeight(player1);
            int rank2 = getPlayerRankWeight(player2);
            
            if (rank1 != rank2) {
                return Integer.compare(rank1, rank2);
            }
            
            // If ranks are equal, sort by name
            return player1.getGameProfile().getName().compareTo(player2.getGameProfile().getName());
        });
        
        // Sort by playtime (if available)
        sortComparators.put(SortType.PLAYTIME, (player1, player2) -> {
            long playtime1 = getPlayerPlaytime(player1);
            long playtime2 = getPlayerPlaytime(player2);
            
            if (playtime1 != playtime2) {
                return Long.compare(playtime2, playtime1); // Descending order
            }
            
            // If playtimes are equal, sort by name
            return player1.getGameProfile().getName().compareTo(player2.getGameProfile().getName());
        });
        
        // Custom sorting (can be implemented later)
        sortComparators.put(SortType.CUSTOM, sortComparators.get(SortType.NAME));
    }
    
    /**
     * Gets the player's rank weight (higher = more important)
     * This is a simplified implementation - in a real scenario, you would
     * integrate with your permission system
     *
     * @param player The player
     * @return The rank weight
     */
    private int getPlayerRankWeight(ServerPlayer player) {
        // Check for specific permissions that indicate rank
        if (PermissionUtil.hasPermission(player, "neoessentials.group.admin")) {
            return 100;
        } else if (PermissionUtil.hasPermission(player, "neoessentials.group.mod")) {
            return 80;
        } else if (PermissionUtil.hasPermission(player, "neoessentials.group.vip")) {
            return 60;
        } else if (PermissionUtil.hasPermission(player, "neoessentials.group.donor")) {
            return 40;
        } else if (PermissionUtil.hasPermission(player, "neoessentials.group.member")) {
            return 20;
        } else {
            return 0;
        }
    }
    
    /**
     * Gets the player's playtime in minutes
     * This is a placeholder implementation - in a real scenario, you would
     * track player statistics
     *
     * @param player The player
     * @return The playtime in minutes
     */
    private long getPlayerPlaytime(ServerPlayer player) {
        // This is a placeholder. In a real implementation, you would track
        // player statistics or use a stats provider.
        return 0;
    }
    
    /**
     * Gets the player's group name based on permissions
     *
     * @param player The player
     * @return The group name
     */
    public String getPlayerGroup(ServerPlayer player) {
        // Check permissions to determine group
        if (PermissionUtil.hasPermission(player, "neoessentials.group.admin")) {
            return "Admin";
        } else if (PermissionUtil.hasPermission(player, "neoessentials.group.mod")) {
            return "Moderator";
        } else if (PermissionUtil.hasPermission(player, "neoessentials.group.vip")) {
            return "VIP";
        } else if (PermissionUtil.hasPermission(player, "neoessentials.group.donor")) {
            return "Donor";
        } else if (PermissionUtil.hasPermission(player, "neoessentials.group.member")) {
            return "Member";
        } else {
            return "Player";
        }
    }
    
    /**
     * Gets a sorted list of all online players
     *
     * @param players The list of online players
     * @return A sorted list of players
     */
    public List<ServerPlayer> getSortedPlayers(Collection<ServerPlayer> players) {
        if (!TablistTomlConfig.ENABLE_SORTING.get()) {
            return new ArrayList<>(players);
        }
        
        // Determine sort type from config
        SortType sortType = getSortTypeFromConfig();
        
        // Check if we need to resort (every 10 seconds)
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSortTime > TimeUnit.SECONDS.toMillis(10) || !sortedPlayerCache.containsKey(sortType)) {
            // Get the comparator for this sort type
            Comparator<ServerPlayer> comparator = sortComparators.getOrDefault(sortType, sortComparators.get(SortType.NAME));
            
            // Sort the players
            List<ServerPlayer> sortedPlayers = new ArrayList<>(players);
            sortedPlayers.sort(comparator);
            
            // Cache the sorted list
            sortedPlayerCache.put(sortType, sortedPlayers);
            lastSortTime = currentTime;
            
            NeoEssentials.LOGGER.debug("Re-sorted player list using {} sort", sortType);
            
            return sortedPlayers;
        }
        
        // Return cached sorted list
        List<ServerPlayer> cachedList = sortedPlayerCache.get(sortType);
        
        // Filter to only include currently online players
        Set<UUID> onlinePlayerIds = players.stream()
            .map(ServerPlayer::getUUID)
            .collect(Collectors.toSet());
            
        return cachedList.stream()
            .filter(player -> onlinePlayerIds.contains(player.getUUID()))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets a map of players grouped by their group
     *
     * @param players The list of online players
     * @return A map of group name to list of players in that group
     */
    public Map<String, List<ServerPlayer>> getPlayerGroups(Collection<ServerPlayer> players) {
        // Group players by their group name
        Map<String, List<ServerPlayer>> groups = players.stream()
            .collect(Collectors.groupingBy(this::getPlayerGroup));
        
        // Ensure all groups have a list (even if empty)
        Arrays.asList("Admin", "Moderator", "VIP", "Donor", "Member", "Player").forEach(group -> 
            groups.putIfAbsent(group, new ArrayList<>())
        );
        
        // Sort the players within each group
        groups.forEach((group, groupPlayers) -> {
            Collections.sort(groupPlayers, sortComparators.get(SortType.NAME));
        });
        
        return groups;
    }
    
    /**
     * Gets the sort type from config
     *
     * @return The sort type
     */
    private SortType getSortTypeFromConfig() {
        String configValue = TablistTomlConfig.SORT_TYPE.get().toLowerCase();
        
        switch (configValue) {
            case "rank":
                return SortType.RANK;
            case "playtime":
                return SortType.PLAYTIME;
            case "custom":
                return SortType.CUSTOM;
            case "name":
            default:
                return SortType.NAME;
        }
    }
    
    /**
     * Clears the cache
     */
    public void clearCache() {
        sortedPlayerCache.clear();
        lastSortTime = 0;
    }
}
