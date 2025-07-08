package com.zerog.neoessentials.data;

import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced home data structure with categories, descriptions, and metadata
 */
public class EnhancedHome {
    
    private final String name;
    private final UUID owner;
    private final ResourceKey<Level> dimension;
    private final BlockPos position;
    private final float yaw;
    private final float pitch;
    
    private String description;
    private HomeCategory category;
    private final LocalDateTime created;
    private LocalDateTime lastUsed;
    private int usageCount;
    private boolean isPublic;
    private Set<UUID> allowedPlayers;
    private Map<String, String> metadata;
    
    public EnhancedHome(String name, UUID owner, ResourceKey<Level> dimension, BlockPos position, 
                       float yaw, float pitch) {
        this.name = name;
        this.owner = owner;
        this.dimension = dimension;
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
        this.created = LocalDateTime.now();
        this.lastUsed = LocalDateTime.now();
        this.usageCount = 0;
        this.isPublic = false;
        this.allowedPlayers = new HashSet<>();
        this.metadata = new ConcurrentHashMap<>();
        this.category = HomeCategory.GENERAL;
        this.description = "";
    }
    
    /**
     * Records that this home was used (teleported to)
     */
    public void recordUsage() {
        this.lastUsed = LocalDateTime.now();
        this.usageCount++;
    }
    
    /**
     * Checks if a player has permission to use this home
     */
    public boolean canPlayerUse(UUID playerId) {
        if (owner.equals(playerId)) return true;
        if (isPublic) return true;
        return allowedPlayers.contains(playerId);
    }
    
    /**
     * Adds a player to the allowed list
     */
    public void allowPlayer(UUID playerId) {
        allowedPlayers.add(playerId);
    }
    
    /**
     * Removes a player from the allowed list
     */
    public void disallowPlayer(UUID playerId) {
        allowedPlayers.remove(playerId);
    }
    
    /**
     * Sets custom metadata for this home
     */
    public void setMetadata(String key, String value) {
        metadata.put(key, value);
    }
    
    /**
     * Gets custom metadata for this home
     */
    public String getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * Gets the display name with category prefix
     */
    public String getDisplayName() {
        return category.getPrefix() + name;
    }
    
    /**
     * Gets a formatted description for display
     */
    public String getFormattedDescription() {
        if (description == null || description.isEmpty()) {
            return "§7No description set";
        }
        return "§f" + description;
    }
    
    /**
     * Gets usage statistics as a formatted string
     */
    public String getUsageStats() {
        return String.format("§7Used §e%d times§7, last used §b%s", 
            usageCount, 
            lastUsed.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm")));
    }
    
    // Getters and Setters
    public String getName() { return name; }
    public UUID getOwner() { return owner; }
    public ResourceKey<Level> getDimension() { return dimension; }
    public BlockPos getPosition() { return position; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public HomeCategory getCategory() { return category; }
    public void setCategory(HomeCategory category) { this.category = category; }
    public LocalDateTime getCreated() { return created; }
    public LocalDateTime getLastUsed() { return lastUsed; }
    public int getUsageCount() { return usageCount; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    public Set<UUID> getAllowedPlayers() { return new HashSet<>(allowedPlayers); }
    public Map<String, String> getMetadata() { return new HashMap<>(metadata); }
    
    /**
     * Enum for home categories with visual prefixes and descriptions
     */
    public enum HomeCategory {
        GENERAL("§7", "General", "🏠", "General purpose homes"),
        BASE("§a", "Base", "🏰", "Main bases and settlements"),
        FARM("§2", "Farm", "🌾", "Farming and agricultural areas"),
        MINE("§8", "Mine", "⛏️", "Mining locations and quarries"),
        SHOP("§6", "Shop", "🏪", "Trading posts and shops"),
        BUILD("§b", "Build", "🏗️", "Construction and building projects"),
        ADVENTURE("§5", "Adventure", "⚔️", "Adventure and exploration spots"),
        TRANSPORT("§e", "Transport", "🚂", "Transportation hubs and stations"),
        FRIEND("§d", "Friend", "👥", "Friends' locations and meeting spots"),
        TEMP("§f", "Temp", "📍", "Temporary or short-term locations");
        
        private final String colorCode;
        private final String displayName;
        private final String emoji;
        private final String description;
        
        HomeCategory(String colorCode, String displayName, String emoji, String description) {
            this.colorCode = colorCode;
            this.displayName = displayName;
            this.emoji = emoji;
            this.description = description;
        }
        
        public String getPrefix() {
            return colorCode + emoji + " ";
        }
        
        public String getDisplayName() {
            return colorCode + displayName;
        }
        
        public String getDescription() {
            return "§7" + description;
        }
        
        public String getColorCode() {
            return colorCode;
        }
        
        public String getEmoji() {
            return emoji;
        }
        
        /**
         * Gets a category by name (case-insensitive)
         */
        public static HomeCategory fromString(String name) {
            for (HomeCategory category : values()) {
                if (category.displayName.equalsIgnoreCase(name) || category.name().equalsIgnoreCase(name)) {
                    return category;
                }
            }
            return GENERAL;
        }
        
        /**
         * Gets all category names for tab completion
         */
        public static List<String> getCategoryNames() {
            List<String> names = new ArrayList<>();
            for (HomeCategory category : values()) {
                names.add(category.displayName.toLowerCase());
            }
            return names;
        }
    }
    
    /**
     * Builder pattern for creating EnhancedHome instances
     */
    public static class Builder {
        private String name;
        private UUID owner;
        private ResourceKey<Level> dimension;
        private BlockPos position;
        private float yaw = 0.0f;
        private float pitch = 0.0f;
        private String description = "";
        private HomeCategory category = HomeCategory.GENERAL;
        private boolean isPublic = false;
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder owner(UUID owner) {
            this.owner = owner;
            return this;
        }
        
        public Builder dimension(ResourceKey<Level> dimension) {
            this.dimension = dimension;
            return this;
        }
        
        public Builder position(BlockPos position) {
            this.position = position;
            return this;
        }
        
        public Builder rotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder category(HomeCategory category) {
            this.category = category;
            return this;
        }
        
        public Builder isPublic(boolean isPublic) {
            this.isPublic = isPublic;
            return this;
        }
        
        public EnhancedHome build() {
            if (name == null || owner == null || dimension == null || position == null) {
                throw new IllegalStateException("Name, owner, dimension, and position are required");
            }
            
            EnhancedHome home = new EnhancedHome(name, owner, dimension, position, yaw, pitch);
            home.setDescription(description);
            home.setCategory(category);
            home.setPublic(isPublic);
            return home;
        }
    }
}
