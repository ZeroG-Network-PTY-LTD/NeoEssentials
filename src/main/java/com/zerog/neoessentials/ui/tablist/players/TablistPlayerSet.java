package com.zerog.neoessentials.ui.tablist.players;

import com.zerog.neoessentials.ui.tablist.TablistPlayerData;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Represents a set of players that can be used in tablist display
 * Supports filtering, sorting, and other operations
 */
public class TablistPlayerSet {
    private final String id;
    private final String displayName;
    private boolean showVanishedPlayers = false;
    private Predicate<ServerPlayer> filterPredicate;
    private PlayerSortType sortType = PlayerSortType.NAME;
    private List<PlayerDisplayStrategy> displayStrategies = new ArrayList<>();
    
    /**
     * Creates a new player set with the specified ID and display name
     * @param id The unique ID of the player set
     * @param displayName The display name of the player set
     */
    public TablistPlayerSet(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    /**
     * Gets the unique ID of this player set
     * @return The player set ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the display name of this player set
     * @return The player set display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Sets whether vanished players should be shown in this set
     * @param showVanishedPlayers True to show vanished players, false to hide them
     * @return This player set for chaining
     */
    public TablistPlayerSet setShowVanishedPlayers(boolean showVanishedPlayers) {
        this.showVanishedPlayers = showVanishedPlayers;
        return this;
    }
    
    /**
     * Sets a filter predicate for this player set
     * @param filterPredicate The filter predicate
     * @return This player set for chaining
     */
    public TablistPlayerSet setFilterPredicate(Predicate<ServerPlayer> filterPredicate) {
        this.filterPredicate = filterPredicate;
        return this;
    }
    
    /**
     * Sets the sort type for this player set
     * @param sortType The sort type
     * @return This player set for chaining
     */
    public TablistPlayerSet setSortType(PlayerSortType sortType) {
        this.sortType = sortType;
        return this;
    }
    
    /**
     * Adds a display strategy to this player set
     * @param strategy The display strategy
     * @return This player set for chaining
     */
    public TablistPlayerSet addDisplayStrategy(PlayerDisplayStrategy strategy) {
        this.displayStrategies.add(strategy);
        return this;
    }
    
    /**
     * Gets the filtered and sorted list of players in this set
     * @param allPlayers All players to filter from
     * @param playerData Player data for sorting
     * @return The filtered and sorted list of players
     */
    public List<ServerPlayer> getPlayers(List<ServerPlayer> allPlayers, 
                                          java.util.Map<UUID, TablistPlayerData> playerData) {
        // Start with all players
        List<ServerPlayer> filteredPlayers = new ArrayList<>(allPlayers);
        
        // Apply filter predicate if set
        if (filterPredicate != null) {
            filteredPlayers = filteredPlayers.stream()
                .filter(filterPredicate)
                .collect(Collectors.toList());
        }
        
        // Filter vanished players if needed
        if (!showVanishedPlayers) {
            filteredPlayers = filteredPlayers.stream()
                .filter(player -> !isVanished(player))
                .collect(Collectors.toList());
        }
        
        // Sort the players
        sortPlayers(filteredPlayers, playerData);
        
        return filteredPlayers;
    }
    
    /**
     * Sorts the players according to the sort type
     * @param players The players to sort
     * @param playerData The player data for sorting
     */
    private void sortPlayers(List<ServerPlayer> players, 
                            java.util.Map<UUID, TablistPlayerData> playerData) {
        switch (sortType) {
            case NAME:
                players.sort((p1, p2) -> 
                    p1.getScoreboardName().compareToIgnoreCase(p2.getScoreboardName()));
                break;
                
            case RANK:
                // Sort by player rank - higher ranks first
                players.sort((p1, p2) -> {
                    TablistPlayerData data1 = playerData.get(p1.getUUID());
                    TablistPlayerData data2 = playerData.get(p2.getUUID());
                    
                    if (data1 == null || data2 == null) {
                        return 0;
                    }
                    
                    // Compare group ranks - assume groups are ordered by rank weight
                    int rankCompare = Integer.compare(getRankWeight(data2.getGroup()), 
                                                     getRankWeight(data1.getGroup()));
                    
                    // If ranks are equal, sort by name
                    if (rankCompare == 0) {
                        return p1.getScoreboardName().compareToIgnoreCase(p2.getScoreboardName());
                    }
                    
                    return rankCompare;
                });
                break;
                
            case PLAYTIME:
                // Sort by playtime - most playtime first
                players.sort((p1, p2) -> {
                    TablistPlayerData data1 = playerData.get(p1.getUUID());
                    TablistPlayerData data2 = playerData.get(p2.getUUID());
                    
                    if (data1 == null || data2 == null) {
                        return 0;
                    }
                    
                    int playtimeCompare = Long.compare(data2.getPlaytime(), data1.getPlaytime());
                    
                    // If playtimes are equal, sort by name
                    if (playtimeCompare == 0) {
                        return p1.getScoreboardName().compareToIgnoreCase(p2.getScoreboardName());
                    }
                    
                    return playtimeCompare;
                });
                break;
        }
    }
    
    /**
     * Get the rank weight of a player group
     * @param group The player group
     * @return The rank weight (higher = more important)
     */
    private int getRankWeight(String group) {
        if (group == null) {
            return 0;
        }
        
        switch (group.toLowerCase()) {
            case "admin": return 100;
            case "mod": return 80;
            case "vip": return 60;
            case "donor": return 40;
            case "default": 
            default: return 0;
        }
    }
    
    /**
     * Checks if a player is vanished
     * @param player The player to check
     * @return True if the player is vanished, false otherwise
     */
    private boolean isVanished(ServerPlayer player) {
        // Implementation depends on how vanish is implemented
        // This is a placeholder that should be replaced with actual vanish detection logic
        
        // Check for vanish tag
        if (player.getTags().contains("vanished")) {
            return true;
        }
        
        // Check for vanish capability or status effect
        // Implement integration with vanish plugins/mods here
        
        return false;
    }
}
