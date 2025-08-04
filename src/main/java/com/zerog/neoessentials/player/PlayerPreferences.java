package com.zerog.neoessentials.player;

import java.util.HashMap;
import java.util.Map;

/**
 * Player preferences and settings storage
 * Handles user-configurable options and UI preferences
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class PlayerPreferences {
    
    // General preferences
    private String language;
    private String timezone;
    private boolean receiveNotifications;
    private boolean receiveAnnouncements;
    
    // GUI preferences
    private String guiTheme;
    private boolean useAnimations;
    private boolean playClickSounds;
    private int inventorySize;
    
    // Chat preferences
    private boolean enablePrivateMessages;
    private boolean enableChatColors;
    private boolean enableChatTimestamps;
    private String chatFormat;
    
    // Teleportation preferences
    private boolean enableTeleportEffects;
    private boolean enableTeleportSounds;
    private boolean autoAcceptTPA;
    private int teleportCooldownBypass;
    
    // Economy preferences
    private boolean enableEconomyNotifications;
    private String currencyFormat;
    private boolean enablePaymentConfirmation;
    
    // Privacy preferences
    private boolean allowPlayerLookup;
    private boolean showOnlineStatus;
    private boolean allowStatsViewing;
    
    // Advanced preferences
    private Map<String, Object> customPreferences;
    
    public PlayerPreferences() {
        // Set default values
        this.language = "en_US";
        this.timezone = "UTC";
        this.receiveNotifications = true;
        this.receiveAnnouncements = true;
        
        this.guiTheme = "default";
        this.useAnimations = true;
        this.playClickSounds = true;
        this.inventorySize = 54;
        
        this.enablePrivateMessages = true;
        this.enableChatColors = true;
        this.enableChatTimestamps = false;
        this.chatFormat = "default";
        
        this.enableTeleportEffects = true;
        this.enableTeleportSounds = true;
        this.autoAcceptTPA = false;
        this.teleportCooldownBypass = 0;
        
        this.enableEconomyNotifications = true;
        this.currencyFormat = "$#,##0.00";
        this.enablePaymentConfirmation = true;
        
        this.allowPlayerLookup = true;
        this.showOnlineStatus = true;
        this.allowStatsViewing = true;
        
        this.customPreferences = new HashMap<>();
    }
    
    // General preferences getters/setters
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    
    public boolean isReceiveNotifications() {
        return receiveNotifications;
    }
    
    public void setReceiveNotifications(boolean receiveNotifications) {
        this.receiveNotifications = receiveNotifications;
    }
    
    public boolean isReceiveAnnouncements() {
        return receiveAnnouncements;
    }
    
    public void setReceiveAnnouncements(boolean receiveAnnouncements) {
        this.receiveAnnouncements = receiveAnnouncements;
    }
    
    // GUI preferences getters/setters
    public String getGuiTheme() {
        return guiTheme;
    }
    
    public void setGuiTheme(String guiTheme) {
        this.guiTheme = guiTheme;
    }
    
    public boolean isUseAnimations() {
        return useAnimations;
    }
    
    public void setUseAnimations(boolean useAnimations) {
        this.useAnimations = useAnimations;
    }
    
    public boolean isPlayClickSounds() {
        return playClickSounds;
    }
    
    public void setPlayClickSounds(boolean playClickSounds) {
        this.playClickSounds = playClickSounds;
    }
    
    public int getInventorySize() {
        return inventorySize;
    }
    
    public void setInventorySize(int inventorySize) {
        this.inventorySize = inventorySize;
    }
    
    // Chat preferences getters/setters
    public boolean isEnablePrivateMessages() {
        return enablePrivateMessages;
    }
    
    public void setEnablePrivateMessages(boolean enablePrivateMessages) {
        this.enablePrivateMessages = enablePrivateMessages;
    }
    
    public boolean isEnableChatColors() {
        return enableChatColors;
    }
    
    public void setEnableChatColors(boolean enableChatColors) {
        this.enableChatColors = enableChatColors;
    }
    
    public boolean isEnableChatTimestamps() {
        return enableChatTimestamps;
    }
    
    public void setEnableChatTimestamps(boolean enableChatTimestamps) {
        this.enableChatTimestamps = enableChatTimestamps;
    }
    
    public String getChatFormat() {
        return chatFormat;
    }
    
    public void setChatFormat(String chatFormat) {
        this.chatFormat = chatFormat;
    }
    
    // Teleportation preferences getters/setters
    public boolean isEnableTeleportEffects() {
        return enableTeleportEffects;
    }
    
    public void setEnableTeleportEffects(boolean enableTeleportEffects) {
        this.enableTeleportEffects = enableTeleportEffects;
    }
    
    public boolean isEnableTeleportSounds() {
        return enableTeleportSounds;
    }
    
    public void setEnableTeleportSounds(boolean enableTeleportSounds) {
        this.enableTeleportSounds = enableTeleportSounds;
    }
    
    public boolean isAutoAcceptTPA() {
        return autoAcceptTPA;
    }
    
    public void setAutoAcceptTPA(boolean autoAcceptTPA) {
        this.autoAcceptTPA = autoAcceptTPA;
    }
    
    public int getTeleportCooldownBypass() {
        return teleportCooldownBypass;
    }
    
    public void setTeleportCooldownBypass(int teleportCooldownBypass) {
        this.teleportCooldownBypass = teleportCooldownBypass;
    }
    
    // Economy preferences getters/setters
    public boolean isEnableEconomyNotifications() {
        return enableEconomyNotifications;
    }
    
    public void setEnableEconomyNotifications(boolean enableEconomyNotifications) {
        this.enableEconomyNotifications = enableEconomyNotifications;
    }
    
    public String getCurrencyFormat() {
        return currencyFormat;
    }
    
    public void setCurrencyFormat(String currencyFormat) {
        this.currencyFormat = currencyFormat;
    }
    
    public boolean isEnablePaymentConfirmation() {
        return enablePaymentConfirmation;
    }
    
    public void setEnablePaymentConfirmation(boolean enablePaymentConfirmation) {
        this.enablePaymentConfirmation = enablePaymentConfirmation;
    }
    
    // Privacy preferences getters/setters
    public boolean isAllowPlayerLookup() {
        return allowPlayerLookup;
    }
    
    public void setAllowPlayerLookup(boolean allowPlayerLookup) {
        this.allowPlayerLookup = allowPlayerLookup;
    }
    
    public boolean isShowOnlineStatus() {
        return showOnlineStatus;
    }
    
    public void setShowOnlineStatus(boolean showOnlineStatus) {
        this.showOnlineStatus = showOnlineStatus;
    }
    
    public boolean isAllowStatsViewing() {
        return allowStatsViewing;
    }
    
    public void setAllowStatsViewing(boolean allowStatsViewing) {
        this.allowStatsViewing = allowStatsViewing;
    }
    
    // Custom preferences
    public Map<String, Object> getCustomPreferences() {
        return customPreferences;
    }
    
    public void setCustomPreferences(Map<String, Object> customPreferences) {
        this.customPreferences = customPreferences;
    }
    
    public Object getCustomPreference(String key) {
        return customPreferences.get(key);
    }
    
    public void setCustomPreference(String key, Object value) {
        customPreferences.put(key, value);
    }
    
    public boolean hasCustomPreference(String key) {
        return customPreferences.containsKey(key);
    }
    
    public void removeCustomPreference(String key) {
        customPreferences.remove(key);
    }
    
    // Utility methods
    public boolean getBooleanPreference(String key, boolean defaultValue) {
        Object value = getCustomPreference(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    public String getStringPreference(String key, String defaultValue) {
        Object value = getCustomPreference(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }
    
    public int getIntPreference(String key, int defaultValue) {
        Object value = getCustomPreference(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    @Override
    public String toString() {
        return String.format("PlayerPreferences{language=%s, theme=%s, notifications=%s}", 
            language, guiTheme, receiveNotifications);
    }
}
