package com.zerog.neoessentials.ui.tablist;

import net.minecraft.server.level.ServerPlayer;

/**
 * Stores per-player data for tablist management
 */
public class TablistPlayerData {
    private final String name;
    private final String uuid;
    private String group = "default";
    private int ping = 0;
    private String world = "";
    private long playtime = 0;
    private boolean vanished = false;
    private String nickname = null;
    
    /**
     * Creates a new TablistPlayerData for a player
     * @param player The player
     */
    public TablistPlayerData(ServerPlayer player) {
        this.name = player.getScoreboardName();
        this.uuid = player.getUUID().toString();
        update(player);
    }    /**
     * Updates the data from the player
     * @param player The player
     */    public void update(ServerPlayer player) {
        // Get ping safely (implementation for NeoForge 1.21.1)
        try {
            // Try to get ping via specific methods for 1.21.1
            this.ping = player.connection.latency();
        } catch (Exception e) {
            // Fallback to reflection if method not available
            try {
                Object result = player.connection.getClass().getMethod("latency").invoke(player.connection);
                if (result instanceof Integer) {
                    this.ping = (Integer) result;
                } else {
                    this.ping = 0;
                }
            } catch (Exception ex) {
                // Default to 0 if all methods fail
                this.ping = 0;
            }
        }
        
        this.world = player.level().dimension().location().toString();
    }
    
    /**
     * Gets the player's name
     * @return The player's name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the player's UUID
     * @return The player's UUID as a string
     */
    public String getUuid() {
        return uuid;
    }
    
    /**
     * Gets the player's group
     * @return The player's group
     */
    public String getGroup() {
        return group;
    }
    
    /**
     * Sets the player's group
     * @param group The group to set
     */
    public void setGroup(String group) {
        this.group = group;
    }
      /**
     * Gets the player's ping
     * @return The player's ping in milliseconds
     */
    public int getPing() {
        return ping;
    }
    
    /**
     * Gets the player's current world
     * @return The world name
     */    public String getWorld() {
        return world;
    }
    
    /**
     * Gets the player's playtime
     * @return The player's playtime in seconds
     */
    public long getPlaytime() {
        return playtime;
    }
    
    /**
     * Sets the player's playtime
     * @param playtime The player's playtime in seconds
     */
    public void setPlaytime(long playtime) {
        this.playtime = playtime;
    }
    
    /**
     * Gets whether the player is vanished
     * @return True if the player is vanished, false otherwise
     */
    public boolean isVanished() {
        return vanished;
    }
    
    /**
     * Sets whether the player is vanished
     * @param vanished True if the player is vanished, false otherwise
     */
    public void setVanished(boolean vanished) {
        this.vanished = vanished;
    }
    
    /**
     * Gets the player's nickname
     * @return The player's nickname, or null if none
     */
    public String getNickname() {
        return nickname;
    }
    
    /**
     * Sets the player's nickname
     * @param nickname The player's nickname
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
