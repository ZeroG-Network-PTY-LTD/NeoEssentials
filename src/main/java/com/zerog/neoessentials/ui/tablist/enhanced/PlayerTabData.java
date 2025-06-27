package com.zerog.neoessentials.ui.tablist.enhanced;

import com.zerog.neoessentials.ui.tablist.TablistPlaceholderManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Stores and manages data for individual players in the tablist system
 */
public class PlayerTabData {
    
    private final UUID uuid;
    private String playerName;
    private String group;
    private String world;
    private String server;
    
    // Display formatting
    private String prefix = "";
    private String suffix = "";
    private String tabFormat = "";
    private String nametagFormat = "";
    
    // Objective values
    private String playerlistValue = "";
    private String belownameValue = "";
    
    // State tracking
    private boolean vanished = false;
    private boolean afk = false;
    private long lastUpdate = 0;
    
    // Cached placeholder values
    private final Map<String, String> placeholderCache = new ConcurrentHashMap<>();
    private final Map<String, Long> placeholderCacheTime = new ConcurrentHashMap<>();
    
    public PlayerTabData(ServerPlayer player) {
        this.uuid = player.getUUID();
        this.playerName = player.getScoreboardName();
        this.world = player.level().dimension().location().toString();
        this.server = "main"; // Could be configurable
        this.lastUpdate = System.currentTimeMillis();
    }
    
    public void update(ServerPlayer player, TABConfig config, TablistPlaceholderManager placeholderManager) {
        this.playerName = player.getScoreboardName();
        this.world = player.level().dimension().location().toString();
        this.lastUpdate = System.currentTimeMillis();
        
        // Update cached values
        updateGroup(player);
        updatePrefixSuffix(player, placeholderManager);
        updateObjectiveValues(player, config, placeholderManager);
        
        // Clear expired placeholder cache
        clearExpiredCache();
    }
    
    private void updateGroup(ServerPlayer player) {
        // This should match the logic in TABLikeTablistManager.getPlayerGroup()
        if (com.zerog.neoessentials.utils.PermissionUtil.hasPermission(player, "neoessentials.group.owner")) {
            this.group = "owner";
        } else if (com.zerog.neoessentials.utils.PermissionUtil.hasPermission(player, "neoessentials.group.admin")) {
            this.group = "admin";
        } else if (com.zerog.neoessentials.utils.PermissionUtil.hasPermission(player, "neoessentials.group.mod")) {
            this.group = "mod";
        } else if (com.zerog.neoessentials.utils.PermissionUtil.hasPermission(player, "neoessentials.group.helper")) {
            this.group = "helper";
        } else if (com.zerog.neoessentials.utils.PermissionUtil.hasPermission(player, "neoessentials.group.builder")) {
            this.group = "builder";
        } else if (com.zerog.neoessentials.utils.PermissionUtil.hasPermission(player, "neoessentials.group.vip")) {
            this.group = "vip";
        } else {
            this.group = "default";
        }
    }
    
    private void updatePrefixSuffix(ServerPlayer player, TablistPlaceholderManager placeholderManager) {
        // Get prefix and suffix based on group
        switch (group) {
            case "owner":
                this.prefix = "&4[OWNER] ";
                this.suffix = "";
                break;
            case "admin":
                this.prefix = "&c[ADMIN] ";
                this.suffix = "";
                break;
            case "mod":
                this.prefix = "&6[MOD] ";
                this.suffix = "";
                break;
            case "helper":
                this.prefix = "&e[HELPER] ";
                this.suffix = "";
                break;
            case "builder":
                this.prefix = "&a[BUILDER] ";
                this.suffix = "";
                break;
            case "vip":
                this.prefix = "&b[VIP] ";
                this.suffix = "";
                break;
            default:
                this.prefix = "";
                this.suffix = "";
                break;
        }
        
        // Apply placeholders to prefix and suffix
        this.prefix = placeholderManager.processPlaceholders(this.prefix, player);
        this.suffix = placeholderManager.processPlaceholders(this.suffix, player);
        
        // Create tab format
        this.tabFormat = this.prefix + "&f" + playerName + this.suffix;
        this.nametagFormat = this.prefix + "&f" + playerName + this.suffix;
    }
    
    private void updateObjectiveValues(ServerPlayer player, TABConfig config, TablistPlaceholderManager placeholderManager) {
        // Update playerlist objective value
        if (config.isPlayerlistObjectiveEnabled()) {
            String rawValue = config.getPlayerlistObjectiveValue();
            this.playerlistValue = placeholderManager.processPlaceholders(rawValue, player);
        }
        
        // Update belowname objective value
        if (config.isBelownameObjectiveEnabled()) {
            String rawValue = config.getBelownameObjectiveValue();
            this.belownameValue = placeholderManager.processPlaceholders(rawValue, player);
        }
    }
    
    private void clearExpiredCache() {
        long currentTime = System.currentTimeMillis();
        long expireTime = 5000; // 5 seconds
        
        placeholderCacheTime.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > expireTime) {
                placeholderCache.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    public String getCachedPlaceholder(String placeholder) {
        return placeholderCache.get(placeholder);
    }
    
    public void cachePlaceholder(String placeholder, String value) {
        placeholderCache.put(placeholder, value);
        placeholderCacheTime.put(placeholder, System.currentTimeMillis());
    }
    
    public boolean isPlaceholderCached(String placeholder) {
        return placeholderCache.containsKey(placeholder) && 
               placeholderCacheTime.containsKey(placeholder) &&
               (System.currentTimeMillis() - placeholderCacheTime.get(placeholder)) < 5000;
    }
    
    // Getters and setters
    
    public UUID getUuid() { return uuid; }
    
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    
    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }
    
    public String getWorld() { return world; }
    public void setWorld(String world) { this.world = world; }
    
    public String getServer() { return server; }
    public void setServer(String server) { this.server = server; }
    
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    
    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }
    
    public String getTabFormat() { return tabFormat; }
    public void setTabFormat(String tabFormat) { this.tabFormat = tabFormat; }
    
    public String getNametagFormat() { return nametagFormat; }
    public void setNametagFormat(String nametagFormat) { this.nametagFormat = nametagFormat; }
    
    public String getPlayerlistValue() { return playerlistValue; }
    public void setPlayerlistValue(String playerlistValue) { this.playerlistValue = playerlistValue; }
    
    public String getBelownameValue() { return belownameValue; }
    public void setBelownameValue(String belownameValue) { this.belownameValue = belownameValue; }
    
    public boolean isVanished() { return vanished; }
    public void setVanished(boolean vanished) { this.vanished = vanished; }
    
    public boolean isAfk() { return afk; }
    public void setAfk(boolean afk) { this.afk = afk; }
    
    public long getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(long lastUpdate) { this.lastUpdate = lastUpdate; }
    
    @Override
    public String toString() {
        return "PlayerTabData{" +
                "uuid=" + uuid +
                ", playerName='" + playerName + '\'' +
                ", group='" + group + '\'' +
                ", world='" + world + '\'' +
                ", prefix='" + prefix + '\'' +
                ", suffix='" + suffix + '\'' +
                '}';
    }
}
