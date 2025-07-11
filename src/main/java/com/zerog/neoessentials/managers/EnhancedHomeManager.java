package com.zerog.neoessentials.managers;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.EnhancedHome;
import com.zerog.neoessentials.util.FileUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Enhanced home management system with categories, permissions, and GUI support
 */
public class EnhancedHomeManager {
    
    private final Map<UUID, Map<String, EnhancedHome>> playerHomes = new ConcurrentHashMap<>();
    private final Map<String, EnhancedHome> publicHomes = new ConcurrentHashMap<>();
    private final File dataFile;
    
    // Default home limits by category
    private final Map<EnhancedHome.HomeCategory, Integer> categoryLimits = new HashMap<>();
    
    public EnhancedHomeManager() {
        this.dataFile = new File(com.zerog.neoessentials.util.FileUtils.getDataFolder(), "enhanced_homes.json");
        initializeCategoryLimits();
        loadHomes();
    }
    
    /**
     * Initialize default home limits for each category
     */
    private void initializeCategoryLimits() {
        categoryLimits.put(EnhancedHome.HomeCategory.GENERAL, 5);
        categoryLimits.put(EnhancedHome.HomeCategory.BASE, 3);
        categoryLimits.put(EnhancedHome.HomeCategory.FARM, 5);
        categoryLimits.put(EnhancedHome.HomeCategory.MINE, 10);
        categoryLimits.put(EnhancedHome.HomeCategory.SHOP, 3);
        categoryLimits.put(EnhancedHome.HomeCategory.BUILD, 5);
        categoryLimits.put(EnhancedHome.HomeCategory.ADVENTURE, 8);
        categoryLimits.put(EnhancedHome.HomeCategory.TRANSPORT, 3);
        categoryLimits.put(EnhancedHome.HomeCategory.FRIEND, 10);
        categoryLimits.put(EnhancedHome.HomeCategory.TEMP, 15);
    }
    
    /**
     * Creates a new enhanced home for a player
     */
    public boolean createHome(ServerPlayer player, String name, EnhancedHome.HomeCategory category, String description) {
        UUID playerId = player.getUUID();
        
        // Check if home name already exists
        if (hasHome(playerId, name)) {
            return false;
        }
        
        // Check category limits
        if (!canCreateHomeInCategory(playerId, category)) {
            return false;
        }
        
        // Create the home
        EnhancedHome home = new EnhancedHome(
            name,
            playerId,
            player.level().dimension(),
            player.blockPosition(),
            player.getYRot(),
            player.getXRot()
        );
        
        home.setCategory(category);
        home.setDescription(description != null ? description : "");
        
        // Add to player's homes
        playerHomes.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(name, home);
        
        // Save to file
        saveHomes();
        
        NeoEssentials.LOGGER.info("Created enhanced home '{}' for player {} in category {}", 
            name, player.getName().getString(), category.name());
        
        return true;
    }
    
    /**
     * Deletes a home
     */
    public boolean deleteHome(UUID playerId, String name) {
        Map<String, EnhancedHome> homes = playerHomes.get(playerId);
        if (homes == null) return false;
        
        EnhancedHome removed = homes.remove(name);
        if (removed != null) {
            // Remove from public homes if it was public
            if (removed.isPublic()) {
                publicHomes.remove(name);
            }
            
            saveHomes();
            return true;
        }
        
        return false;
    }
    
    /**
     * Teleports a player to a home
     */
    public boolean teleportToHome(ServerPlayer player, String homeName) {
        UUID playerId = player.getUUID();
        EnhancedHome home = getHome(playerId, homeName);
        
        if (home == null) {
            // Check if it's a public home
            home = publicHomes.get(homeName);
            if (home == null) return false;
        }
        
        // Check permissions
        if (!home.canPlayerUse(playerId)) {
            return false;
        }
        
        // Teleport player
        try {
            player.teleportTo(
                player.server.getLevel(home.getDimension()),
                home.getPosition().getX() + 0.5,
                home.getPosition().getY(),
                home.getPosition().getZ() + 0.5,
                home.getYaw(),
                home.getPitch()
            );
            
            // Record usage
            home.recordUsage();
            saveHomes();
            
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error teleporting player to home", e);
            return false;
        }
    }
    
    /**
     * Gets all homes for a player
     */
    public List<EnhancedHome> getPlayerHomes(UUID playerId) {
        Map<String, EnhancedHome> homes = playerHomes.get(playerId);
        if (homes == null) return new ArrayList<>();
        
        return new ArrayList<>(homes.values());
    }
    
    /**
     * Gets homes for a player filtered by category
     */
    public List<EnhancedHome> getPlayerHomesByCategory(UUID playerId, EnhancedHome.HomeCategory category) {
        return getPlayerHomes(playerId).stream()
            .filter(home -> home.getCategory() == category)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets a specific home
     */
    public EnhancedHome getHome(UUID playerId, String name) {
        Map<String, EnhancedHome> homes = playerHomes.get(playerId);
        if (homes == null) return null;
        return homes.get(name);
    }
    
    /**
     * Checks if a player has a home with the given name
     */
    public boolean hasHome(UUID playerId, String name) {
        return getHome(playerId, name) != null;
    }
    
    /**
     * Updates a home's properties
     */
    public boolean updateHome(UUID playerId, String name, String description, 
                           EnhancedHome.HomeCategory category, boolean isPublic) {
        EnhancedHome home = getHome(playerId, name);
        if (home == null) return false;
        
        home.setDescription(description);
        home.setCategory(category);
        home.setPublic(isPublic);
        
        // Update public homes registry
        if (isPublic) {
            publicHomes.put(name, home);
        } else {
            publicHomes.remove(name);
        }
        
        saveHomes();
        return true;
    }
    
    /**
     * Checks if a player can create a home in the given category
     */
    public boolean canCreateHomeInCategory(UUID playerId, EnhancedHome.HomeCategory category) {
        int currentCount = getPlayerHomesByCategory(playerId, category).size();
        int limit = categoryLimits.getOrDefault(category, 5);
        return currentCount < limit;
    }
    
    /**
     * Gets the home limit for a category
     */
    public int getCategoryLimit(EnhancedHome.HomeCategory category) {
        return categoryLimits.getOrDefault(category, 5);
    }
    
    /**
     * Gets all public homes
     */
    public List<EnhancedHome> getPublicHomes() {
        return new ArrayList<>(publicHomes.values());
    }
    
    /**
     * Gets home statistics for a player
     */
    public Map<EnhancedHome.HomeCategory, Integer> getPlayerHomeStats(UUID playerId) {
        Map<EnhancedHome.HomeCategory, Integer> stats = new HashMap<>();
        
        for (EnhancedHome.HomeCategory category : EnhancedHome.HomeCategory.values()) {
            int count = getPlayerHomesByCategory(playerId, category).size();
            stats.put(category, count);
        }
        
        return stats;
    }
    
    /**
     * Loads homes from file
     */
    private void loadHomes() {
        // Implementation for loading from JSON file
        // For now, we'll initialize empty
        NeoEssentials.LOGGER.info("Enhanced home manager initialized");
    }
    
    /**
     * Saves homes to file
     */
    private void saveHomes() {
        // Implementation for saving to JSON file
        // For now, we'll just log
        NeoEssentials.LOGGER.debug("Enhanced homes saved");
    }
    
    /**
     * Gets the total number of homes a player has
     */
    public int getTotalPlayerHomes(UUID playerId) {
        return getPlayerHomes(playerId).size();
    }
    
    /**
     * Gets the maximum total homes a player can have
     */
    public int getMaxPlayerHomes() {
        return categoryLimits.values().stream().mapToInt(Integer::intValue).sum();
    }
}
