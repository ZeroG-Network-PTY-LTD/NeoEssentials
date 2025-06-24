package com.zerog.neoessentials.ui.tab;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Comprehensive player data storage for the TabManager system
 */
public class TabPlayerData {
    // Basic player info
    private final String name;
    private final UUID uuid;
    private String displayName;
    private String world = "";
    private int ping = 0;
    private long playtime = 0;
    private boolean online = true;

    // Custom attributes
    private String group = "default";
    private boolean vanished = false;
    private String nickname = null;

    // Per-feature data
    private String nameTagPrefix = "";
    private String nameTagSuffix = "";
    private String tablistPrefix = "";
    private String tablistSuffix = "";
    private String belowNameText = "";
    private int sortPriority = 0;
    
    // Custom storage for feature-specific data
    private final Map<String, Object> customData = new HashMap<>();

    /**
     * Creates new player data from a ServerPlayer
     * @param player The server player
     */
    public TabPlayerData(ServerPlayer player) {
        this.name = player.getScoreboardName();
        this.uuid = player.getUUID();
        this.displayName = player.getDisplayName().getString();
        update(player);
    }    /**
     * Updates player data from a ServerPlayer
     * @param player The server player
     */
    public void update(ServerPlayer player) {
        // Update basic information
        this.displayName = player.getDisplayName().getString();
        this.world = player.level().dimension().location().toString();
          // Get ping safely from the connection
        try {
            // Try to get the ping using reflection
            java.lang.reflect.Field pingField = player.connection.getClass().getDeclaredField("latency");
            pingField.setAccessible(true);
            this.ping = pingField.getInt(player.connection);
        } catch (Exception e1) {
            try {
                // Alternative approach - try another field name
                java.lang.reflect.Field pingField = player.connection.getClass().getDeclaredField("e"); // Obfuscated field name
                pingField.setAccessible(true);
                this.ping = pingField.getInt(player.connection);
            } catch (Exception e2) {                // Fallback to a reasonable default
                this.ping = 50;
            }
        }
    }

    /**
     * Gets the player's username
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the player's UUID
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Gets the player's display name 
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the player's display name
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the player's current world
     */
    public String getWorld() {
        return world;
    }

    /**
     * Sets the player's current world
     */
    public void setWorld(String world) {
        this.world = world;
    }

    /**
     * Gets the player's ping/latency in ms
     */
    public int getPing() {
        return ping;
    }

    /**
     * Sets the player's ping/latency
     */
    public void setPing(int ping) {
        this.ping = ping;
    }

    /**
     * Gets the player's playtime in seconds
     */
    public long getPlaytime() {
        return playtime;
    }

    /**
     * Sets the player's playtime
     */
    public void setPlaytime(long playtime) {
        this.playtime = playtime;
    }

    /**
     * Checks if player is online
     */
    public boolean isOnline() {
        return online;
    }

    /**
     * Sets the player's online status
     */
    public void setOnline(boolean online) {
        this.online = online;
    }

    /**
     * Gets the player's permission group
     */
    public String getGroup() {
        return group;
    }

    /**
     * Sets the player's permission group
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * Checks if the player is vanished
     */
    public boolean isVanished() {
        return vanished;
    }

    /**
     * Sets the player's vanish status
     */
    public void setVanished(boolean vanished) {
        this.vanished = vanished;
    }

    /**
     * Gets the player's nickname
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Sets the player's nickname
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * Gets the player's nametag prefix
     */
    public String getNameTagPrefix() {
        return nameTagPrefix;
    }

    /**
     * Sets the player's nametag prefix
     */
    public void setNameTagPrefix(String nameTagPrefix) {
        this.nameTagPrefix = nameTagPrefix;
    }

    /**
     * Gets the player's nametag suffix
     */
    public String getNameTagSuffix() {
        return nameTagSuffix;
    }

    /**
     * Sets the player's nametag suffix
     */
    public void setNameTagSuffix(String nameTagSuffix) {
        this.nameTagSuffix = nameTagSuffix;
    }

    /**
     * Gets the player's tablist prefix
     */
    public String getTablistPrefix() {
        return tablistPrefix;
    }

    /**
     * Sets the player's tablist prefix
     */
    public void setTablistPrefix(String tablistPrefix) {
        this.tablistPrefix = tablistPrefix;
    }

    /**
     * Gets the player's tablist suffix
     */
    public String getTablistSuffix() {
        return tablistSuffix;
    }

    /**
     * Sets the player's tablist suffix
     */
    public void setTablistSuffix(String tablistSuffix) {
        this.tablistSuffix = tablistSuffix;
    }

    /**
     * Gets the player's below name text
     */
    public String getBelowNameText() {
        return belowNameText;
    }

    /**
     * Sets the player's below name text
     */
    public void setBelowNameText(String belowNameText) {
        this.belowNameText = belowNameText;
    }

    /**
     * Gets the player's sort priority
     */
    public int getSortPriority() {
        return sortPriority;
    }

    /**
     * Sets the player's sort priority
     */
    public void setSortPriority(int sortPriority) {
        this.sortPriority = sortPriority;
    }
    
    /**
     * Sets custom data for feature-specific storage
     */
    @SuppressWarnings("unchecked")
    public <T> T getCustomData(String key, Class<T> type) {
        Object data = customData.get(key);
        if (data != null && type.isAssignableFrom(data.getClass())) {
            return (T) data;
        }
        return null;
    }
    
    /**
     * Gets custom data for feature-specific storage
     */
    public void setCustomData(String key, Object value) {
        customData.put(key, value);
    }
}
