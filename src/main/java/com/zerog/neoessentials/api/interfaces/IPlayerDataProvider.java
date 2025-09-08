package com.zerog.neoessentials.api.interfaces;

// Temporarily disabled Minecraft imports due to classpath issues  
// import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

/**
 * Player data provider interface for NeoEssentials API
 * Allows third-party plugins to access and modify player data
 * TEMPORARILY SIMPLIFIED due to import issues - will restore when dependencies work
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public interface IPlayerDataProvider {
    
    /**
     * Get player data by UUID
     * @param playerUuid Player's UUIDN
     * @return Optional containing player data, or empty if not found
     */
    Optional<PlayerData> getPlayerData(UUID playerUuid);
    
    /**
     * Get player data by name
     * @param playerName Player's name
     * @return Optional containing player data, or empty if not found
     */
    Optional<PlayerData> getPlayerData(String playerName);
    
    /**
     * Save player data
     * @param playerData Player data to save
     * @return true if save was successful
     */
    boolean savePlayerData(PlayerData playerData);
    
    /**
     * Check if player data exists
     * @param playerUuid Player's UUID
     * @return true if player data exists
     */
    boolean hasPlayerData(UUID playerUuid);
    
    /**
     * Create new player data - using Object instead of ServerPlayer temporarily
     * @param player Player instance (Object type due to import issues)
     * @return Created player data
     */
    PlayerData createPlayerData(Object player);
    
    /**
     * Delete player data
     * @param playerUuid Player's UUID
     * @return true if deletion was successful
     */
    boolean deletePlayerData(UUID playerUuid);
    
    /**
     * Get all player UUIDs
     * @return List of all player UUIDs
     */
    List<UUID> getAllPlayerUUIDs();
    
    /**
     * Get online players count
     * @return Number of online players
     */
    int getOnlinePlayersCount();
    
    /**
     * Get offline players count
     * @return Number of offline players
     */
    int getOfflinePlayersCount();
    
    /**
     * Player data container class
     */
    interface PlayerData {
        
        /**
         * Get player's UUID
         * @return Player UUID
         */
        UUID getUUID();
        
        /**
         * Get player's name
         * @return Player name
         */
        String getName();
        
        /**
         * Get last known display name
         * @return Display name
         */
        String getDisplayName();
        
        /**
         * Set display name
         * @param displayName New display name
         */
        void setDisplayName(String displayName);
        
        /**
         * Get first login timestamp
         * @return First login time in milliseconds
         */
        long getFirstLogin();
        
        /**
         * Get last login timestamp
         * @return Last login time in milliseconds
         */
        long getLastLogin();
        
        /**
         * Set last login timestamp
         * @param timestamp Login time in milliseconds
         */
        void setLastLogin(long timestamp);
        
        /**
         * Get last logout timestamp
         * @return Last logout time in milliseconds
         */
        long getLastLogout();
        
        /**
         * Set last logout timestamp
         * @param timestamp Logout time in milliseconds
         */
        void setLastLogout(long timestamp);
        
        /**
         * Get total playtime in milliseconds
         * @return Total playtime
         */
        long getTotalPlaytime();
        
        /**
         * Add playtime
         * @param playtime Playtime to add in milliseconds
         */
        void addPlaytime(long playtime);
        
        /**
         * Check if player is currently online
         * @return true if player is online
         */
        boolean isOnline();
        
        /**
         * Set online status
         * @param online Online status
         */
        void setOnline(boolean online);
        
        /**
         * Get custom data value
         * @param key Data key
         * @return Data value, or null if not found
         */
        Object getCustomData(String key);
        
        /**
         * Set custom data value
         * @param key Data key
         * @param value Data value
         */
        void setCustomData(String key, Object value);
        
        /**
         * Remove custom data
         * @param key Data key
         * @return Previous value, or null if not found
         */
        Object removeCustomData(String key);
        
        /**
         * Check if custom data exists
         * @param key Data key
         * @return true if data exists
         */
        boolean hasCustomData(String key);
        
        /**
         * Get all custom data keys
         * @return List of all custom data keys
         */
        List<String> getCustomDataKeys();
        
        /**
         * Get player's IP address
         * @return Last known IP address
         */
        String getIpAddress();
        
        /**
         * Set player's IP address
         * @param ipAddress IP address
         */
        void setIpAddress(String ipAddress);
        
        /**
         * Get player's current world
         * @return World name
         */
        String getCurrentWorld();
        
        /**
         * Set player's current world
         * @param world World name
         */
        void setCurrentWorld(String world);
        
        /**
         * Check if player is AFK
         * @return true if player is AFK
         */
        boolean isAFK();
        
        /**
         * Set AFK status
         * @param afk AFK status
         */
        void setAFK(boolean afk);
        
        /**
         * Get AFK timestamp
         * @return Time when player went AFK, or 0 if not AFK
         */
        long getAFKTime();
        
        /**
         * Set AFK timestamp
         * @param timestamp AFK time in milliseconds
         */
        void setAFKTime(long timestamp);
        
        /**
         * Check if player is muted
         * @return true if player is muted
         */
        boolean isMuted();
        
        /**
         * Set mute status
         * @param muted Mute status
         */
        void setMuted(boolean muted);
        
        /**
         * Get mute expiration time
         * @return Mute expiration time, or 0 if permanent/not muted
         */
        long getMuteExpiration();
        
        /**
         * Set mute expiration time
         * @param expiration Expiration time in milliseconds, 0 for permanent
         */
        void setMuteExpiration(long expiration);
        
        /**
         * Get mute reason
         * @return Mute reason, or null if not muted
         */
        String getMuteReason();
        
        /**
         * Set mute reason
         * @param reason Mute reason
         */
        void setMuteReason(String reason);
    }
}
