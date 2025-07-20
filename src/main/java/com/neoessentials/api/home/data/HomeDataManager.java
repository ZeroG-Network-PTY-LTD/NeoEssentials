package com.neoessentials.api.home.data;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for home data management
 * Similar to EssentialsX home storage system
 */
public interface HomeDataManager {
    
    /**
     * Save a home for a player
     * @param playerUUID Player's UUID
     * @param homeName Name of the home
     * @param home Home data
     * @return CompletableFuture that completes when save is done
     */
    CompletableFuture<Void> saveHome(UUID playerUUID, String homeName, HomeData home);
    
    /**
     * Load a specific home for a player
     * @param playerUUID Player's UUID
     * @param homeName Name of the home
     * @return CompletableFuture with HomeData or null if not found
     */
    CompletableFuture<HomeData> loadHome(UUID playerUUID, String homeName);
    
    /**
     * Load all homes for a player
     * @param playerUUID Player's UUID
     * @return CompletableFuture with list of homes
     */
    CompletableFuture<List<HomeData>> loadPlayerHomes(UUID playerUUID);
    
    /**
     * Delete a home for a player
     * @param playerUUID Player's UUID
     * @param homeName Name of the home to delete
     * @return CompletableFuture that completes when deletion is done
     */
    CompletableFuture<Boolean> deleteHome(UUID playerUUID, String homeName);
    
    /**
     * Get the count of homes for a player
     * @param playerUUID Player's UUID
     * @return CompletableFuture with home count
     */
    CompletableFuture<Integer> getHomeCount(UUID playerUUID);
    
    /**
     * Check if a player has a specific home
     * @param playerUUID Player's UUID
     * @param homeName Name of the home
     * @return CompletableFuture with boolean result
     */
    CompletableFuture<Boolean> hasHome(UUID playerUUID, String homeName);
    
    /**
     * Get all home names for a player
     * @param playerUUID Player's UUID
     * @return CompletableFuture with list of home names
     */
    CompletableFuture<List<String>> getHomeNames(UUID playerUUID);
}
