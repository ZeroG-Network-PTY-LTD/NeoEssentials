package com.zerog.neoessentials.common.data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * User data class that is version-independent.
 * This contains the core user data that's shared across all versions.
 */
public class UserData {
    private UUID uuid;
    private String username;
    private String displayName;
    private String nickname;
    private Map<String, HomeData> homes;
    private Location lastLocation;
    private Date lastSeen;
    private boolean muted;
    private Date muteExpiry;
    private String muteReason;
    private String ipAddress;
    private double balance;
    private boolean godMode;
    private boolean afk;
    private long totalPlayTime; // in seconds
    private long sessionPlayTime; // in seconds
    private Date firstJoin;
    
    /**
     * Default constructor required for deserialization
     */
    public UserData() {
        this.homes = new HashMap<>();
    }
    
    /**
     * Create a new user data object
     * 
     * @param uuid The UUID of the player
     * @param username The username of the player
     */
    public UserData(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
        this.displayName = username;
        this.nickname = null;
        this.homes = new HashMap<>();
        this.lastLocation = null;
        this.lastSeen = new Date();
        this.muted = false;
        this.muteExpiry = null;
        this.muteReason = null;
        this.ipAddress = "";
        this.balance = 0.0;
        this.godMode = false;
        this.afk = false;
        this.totalPlayTime = 0;
        this.sessionPlayTime = 0;
        this.firstJoin = new Date();
    }
    
    /**
     * Get the UUID of the player
     * 
     * @return The UUID of the player
     */
    public UUID getUuid() {
        return uuid;
    }
    
    /**
     * Set the UUID of the player
     * 
     * @param uuid The UUID of the player
     */
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    
    /**
     * Get the username of the player
     * 
     * @return The username of the player
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Set the username of the player
     * 
     * @param username The username of the player
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
    /**
     * Get the display name of the player
     * 
     * @return The display name of the player
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Set the display name of the player
     * 
     * @param displayName The display name of the player
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Get the nickname of the player
     * 
     * @return The nickname of the player
     */
    public String getNickname() {
        return nickname;
    }
    
    /**
     * Set the nickname of the player
     * 
     * @param nickname The nickname of the player
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    /**
     * Get the homes of the player
     * 
     * @return The homes of the player
     */
    public Map<String, HomeData> getHomes() {
        return homes;
    }
    
    /**
     * Set the homes of the player
     * 
     * @param homes The homes of the player
     */
    public void setHomes(Map<String, HomeData> homes) {
        this.homes = homes;
    }
    
    /**
     * Get a home by name
     * 
     * @param homeName The name of the home
     * @return The home, or null if not found
     */
    public HomeData getHome(String homeName) {
        return homes.get(homeName);
    }
    
    /**
     * Set a home
     * 
     * @param homeName The name of the home
     * @param home The home data
     */
    public void setHome(String homeName, HomeData home) {
        homes.put(homeName, home);
    }
    
    /**
     * Delete a home
     * 
     * @param homeName The name of the home
     * @return The deleted home, or null if not found
     */
    public HomeData deleteHome(String homeName) {
        return homes.remove(homeName);
    }
    
    /**
     * Get the last location of the player
     * 
     * @return The last location of the player
     */
    public Location getLastLocation() {
        return lastLocation;
    }
    
    /**
     * Set the last location of the player
     * 
     * @param lastLocation The last location of the player
     */
    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
    }
    
    /**
     * Get when the player was last seen
     * 
     * @return When the player was last seen
     */
    public Date getLastSeen() {
        return lastSeen;
    }
    
    /**
     * Set when the player was last seen
     * 
     * @param lastSeen When the player was last seen
     */
    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    /**
     * Get whether the player is muted
     * 
     * @return Whether the player is muted
     */
    public boolean isMuted() {
        if (!muted) {
            return false;
        }
        
        if (muteExpiry != null && muteExpiry.before(new Date())) {
            muted = false;
            muteExpiry = null;
            muteReason = null;
            return false;
        }
        
        return true;
    }
    
    /**
     * Set whether the player is muted
     * 
     * @param muted Whether the player is muted
     */
    public void setMuted(boolean muted) {
        this.muted = muted;
    }
    
    /**
     * Get when the player's mute expires
     * 
     * @return When the player's mute expires
     */
    public Date getMuteExpiry() {
        return muteExpiry;
    }
    
    /**
     * Set when the player's mute expires
     * 
     * @param muteExpiry When the player's mute expires
     */
    public void setMuteExpiry(Date muteExpiry) {
        this.muteExpiry = muteExpiry;
    }
    
    /**
     * Get the reason the player is muted
     * 
     * @return The reason the player is muted
     */
    public String getMuteReason() {
        return muteReason;
    }
    
    /**
     * Set the reason the player is muted
     * 
     * @param muteReason The reason the player is muted
     */
    public void setMuteReason(String muteReason) {
        this.muteReason = muteReason;
    }
    
    /**
     * Get the IP address of the player
     * 
     * @return The IP address of the player
     */
    public String getIpAddress() {
        return ipAddress;
    }
    
    /**
     * Set the IP address of the player
     * 
     * @param ipAddress The IP address of the player
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    /**
     * Get the balance of the player
     * 
     * @return The balance of the player
     */
    public double getBalance() {
        return balance;
    }
    
    /**
     * Set the balance of the player
     * 
     * @param balance The balance of the player
     */
    public void setBalance(double balance) {
        this.balance = balance;
    }
    
    /**
     * Get whether the player is in god mode
     * 
     * @return Whether the player is in god mode
     */
    public boolean isGodMode() {
        return godMode;
    }
    
    /**
     * Set whether the player is in god mode
     * 
     * @param godMode Whether the player is in god mode
     */
    public void setGodMode(boolean godMode) {
        this.godMode = godMode;
    }
    
    /**
     * Get whether the player is AFK
     * 
     * @return Whether the player is AFK
     */
    public boolean isAfk() {
        return afk;
    }
    
    /**
     * Set whether the player is AFK
     * 
     * @param afk Whether the player is AFK
     */
    public void setAfk(boolean afk) {
        this.afk = afk;
    }
    
    /**
     * Get the total play time of the player in seconds
     * 
     * @return The total play time of the player in seconds
     */
    public long getTotalPlayTime() {
        return totalPlayTime;
    }
    
    /**
     * Set the total play time of the player in seconds
     * 
     * @param totalPlayTime The total play time of the player in seconds
     */
    public void setTotalPlayTime(long totalPlayTime) {
        this.totalPlayTime = totalPlayTime;
    }
    
    /**
     * Get the session play time of the player in seconds
     * 
     * @return The session play time of the player in seconds
     */
    public long getSessionPlayTime() {
        return sessionPlayTime;
    }
    
    /**
     * Set the session play time of the player in seconds
     * 
     * @param sessionPlayTime The session play time of the player in seconds
     */
    public void setSessionPlayTime(long sessionPlayTime) {
        this.sessionPlayTime = sessionPlayTime;
    }
    
    /**
     * Get when the player first joined
     * 
     * @return When the player first joined
     */
    public Date getFirstJoin() {
        return firstJoin;
    }
    
    /**
     * Set when the player first joined
     * 
     * @param firstJoin When the player first joined
     */
    public void setFirstJoin(Date firstJoin) {
        this.firstJoin = firstJoin;
    }
}
