package com.zerog.neoessentials.ui.tablist.enhanced;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.tablist.TablistPlaceholderManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages boss bars for enhanced tablist system
 * Handles creation, updating, and display of boss bars similar to TAB plugin
 */
public class BossBarManager {
    
    private MinecraftServer server;
    private TABConfig config;
    private TablistPlaceholderManager placeholderManager;
    
    // Active boss bars
    private final Map<String, BossBarInstance> bossBars = new ConcurrentHashMap<>();
    
    // Player visibility settings (player UUID -> boss bar name -> visible)
    private final Map<UUID, Map<String, Boolean>> playerVisibility = new ConcurrentHashMap<>();
    
    // Last update time for rate limiting
    private long lastUpdate = 0;
    private static final long UPDATE_INTERVAL = 1000; // 1 second
    
    /**
     * Set the server reference
     * @param server The Minecraft server
     */
    public void setServer(MinecraftServer server) {
        this.server = server;
    }
    
    /**
     * Set the placeholder manager
     * @param placeholderManager The placeholder manager
     */
    public void setPlaceholderManager(TablistPlaceholderManager placeholderManager) {
        this.placeholderManager = placeholderManager;
    }
    
    /**
     * Initialize boss bars based on configuration
     * @param config The TAB configuration
     */
    public void initialize(TABConfig config) {
        this.config = config;
        
        if (server == null) {
            NeoEssentials.LOGGER.warn("Cannot initialize BossBarManager without server");
            return;
        }
        
        if (config.isBossBarEnabled()) {
            createBossBars();
        }
        
        NeoEssentials.LOGGER.info("BossBarManager initialized");
    }
    
    /**
     * Create boss bars based on configuration
     */
    private void createBossBars() {
        if (server == null || config == null || !config.isBossBarEnabled()) return;
        
        for (Map.Entry<String, TABConfig.BossBarConfig> entry : config.getBossBars().entrySet()) {
            String name = entry.getKey();
            TABConfig.BossBarConfig bossBarConfig = entry.getValue();
            
            BossBarInstance bossBar = new BossBarInstance(name, bossBarConfig);
            bossBars.put(name, bossBar);
            
            NeoEssentials.LOGGER.debug("Created boss bar: {}", name);
        }
    }
    
    /**
     * Update all boss bars for all players
     */
    public void updateBossBars() {
        if (server == null || config == null || !config.isBossBarEnabled()) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdate < UPDATE_INTERVAL) return;
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePlayerBossBars(player);
        }
        
        lastUpdate = currentTime;
    }
    
    /**
     * Update boss bars for a specific player
     * @param player The player to update
     */
    public void updatePlayerBossBars(ServerPlayer player) {
        if (placeholderManager == null) return;
        
        for (BossBarInstance bossBar : bossBars.values()) {
            updateBossBarForPlayer(player, bossBar);
        }
    }
    
    /**
     * Update a specific boss bar for a player
     * @param player The player
     * @param bossBar The boss bar instance
     */
    private void updateBossBarForPlayer(ServerPlayer player, BossBarInstance bossBar) {
        if (!shouldShowBossBar(player, bossBar)) {
            hideBossBar(player, bossBar);
            return;
        }
        
        // Process placeholders
        String processedText = placeholderManager.processPlaceholders(bossBar.config.getText(), player);
        String processedProgress = placeholderManager.processPlaceholders(bossBar.config.getProgress(), player);
        
        // Update boss bar data
        Component title = Component.literal(formatColors(processedText));
        float progress = parseProgress(processedProgress);
        
        // Send boss bar packet to player
        showBossBar(player, bossBar, title, progress);
    }
    
    /**
     * Check if a boss bar should be shown to a player
     * @param player The player
     * @param bossBar The boss bar instance
     * @return True if the boss bar should be shown
     */
    private boolean shouldShowBossBar(ServerPlayer player, BossBarInstance bossBar) {
        // Check if player has toggled off boss bars
        Map<String, Boolean> playerSettings = playerVisibility.get(player.getUUID());
        if (playerSettings != null && playerSettings.containsKey(bossBar.name)) {
            if (!playerSettings.get(bossBar.name)) {
                return false;
            }
        } else if (config.isBossBarHiddenByDefault()) {
            return false;
        }
        
        // Check display condition if set
        String condition = bossBar.config.getDisplayCondition();
        if (!condition.isEmpty()) {
            String result = placeholderManager.processPlaceholders(condition, player);
            return "true".equalsIgnoreCase(result) || "yes".equalsIgnoreCase(result) || "1".equals(result);
        }
        
        return true;
    }
    
    /**
     * Show a boss bar to a player
     * @param player The player
     * @param bossBar The boss bar instance
     * @param title The boss bar title
     * @param progress The boss bar progress (0.0-1.0)
     */
    private void showBossBar(ServerPlayer player, BossBarInstance bossBar, Component title, float progress) {
        // Create or update boss event
        if (bossBar.bossEvent == null) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("neoessentials", "bossbar_" + bossBar.name.toLowerCase());
            bossBar.bossEvent = server.getCustomBossEvents().create(id, title);
            bossBar.bossEvent.setColor(parseBossBarColor(bossBar.config.getColor()));
            bossBar.bossEvent.setOverlay(parseBossBarStyle(bossBar.config.getStyle()));
        } else {
            bossBar.bossEvent.setName(title);
        }
        
        bossBar.bossEvent.setProgress(Math.max(0.0f, Math.min(1.0f, progress)));
        
        // Send add/update packet
        ((CustomBossEvent) bossBar.bossEvent).addPlayer(player);
        
        // Track that this player is seeing this boss bar
        bossBar.visibleTo.put(player.getUUID(), true);
    }
    
    /**
     * Hide a boss bar from a player
     * @param player The player
     * @param bossBar The boss bar instance
     */
    private void hideBossBar(ServerPlayer player, BossBarInstance bossBar) {
        if (bossBar.visibleTo.containsKey(player.getUUID())) {
            ((CustomBossEvent) bossBar.bossEvent).removePlayer(player);
            bossBar.visibleTo.remove(player.getUUID());
        }
    }
    
    /**
     * Parse progress value from string
     * @param progressText The progress text (e.g., "50%", "0.5", "10/20")
     * @return Progress value between 0.0 and 1.0
     */
    private float parseProgress(String progressText) {
        if (progressText == null || progressText.isEmpty()) {
            return 0.0f;
        }
        
        try {
            // Handle percentage (50% -> 0.5)
            if (progressText.endsWith("%")) {
                float percent = Float.parseFloat(progressText.substring(0, progressText.length() - 1));
                return Math.max(0.0f, Math.min(1.0f, percent / 100.0f));
            }
            
            // Handle fraction (10/20 -> 0.5)
            if (progressText.contains("/")) {
                String[] parts = progressText.split("/");
                if (parts.length == 2) {
                    float current = Float.parseFloat(parts[0]);
                    float max = Float.parseFloat(parts[1]);
                    return max > 0 ? Math.max(0.0f, Math.min(1.0f, current / max)) : 0.0f;
                }
            }
            
            // Handle decimal (0.5 -> 0.5)
            float value = Float.parseFloat(progressText);
            
            // If value is > 1, assume it's a percentage without % sign
            if (value > 1.0f && value <= 100.0f) {
                return value / 100.0f;
            }
            
            return Math.max(0.0f, Math.min(1.0f, value));
        } catch (NumberFormatException e) {
            return 0.0f;
        }
    }
    
    /**
     * Parse boss bar color from string
     * @param colorText The color name
     * @return The BossEvent.BossBarColor
     */
    private BossEvent.BossBarColor parseBossBarColor(String colorText) {
        try {
            return BossEvent.BossBarColor.valueOf(colorText.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossEvent.BossBarColor.WHITE; // Default
        }
    }
    
    /**
     * Parse boss bar style from string
     * @param styleText The style name
     * @return The BossEvent.BossBarOverlay
     */
    private BossEvent.BossBarOverlay parseBossBarStyle(String styleText) {
        try {
            return BossEvent.BossBarOverlay.valueOf(styleText.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossEvent.BossBarOverlay.PROGRESS; // Default
        }
    }
    
    /**
     * Format color codes in text
     * @param text The text to format
     * @return Formatted text
     */
    private String formatColors(String text) {
        if (text == null) return "";
        
        // Convert & color codes to § codes
        return text.replaceAll("&([0-9a-fA-Fk-oK-OrR])", "§$1");
    }
    
    /**
     * Toggle boss bar visibility for a player
     * @param player The player
     * @param bossBarName The boss bar name (null = all boss bars)
     * @return True if boss bars are now visible, false if hidden
     */
    public boolean toggleBossBar(ServerPlayer player, String bossBarName) {
        UUID playerUUID = player.getUUID();
        Map<String, Boolean> playerSettings = playerVisibility.computeIfAbsent(playerUUID, k -> new HashMap<>());
        
        if (bossBarName == null || bossBarName.isEmpty()) {
            // Toggle all boss bars
            boolean newState = !playerSettings.values().stream().findFirst().orElse(!config.isBossBarHiddenByDefault());
            for (String name : bossBars.keySet()) {
                playerSettings.put(name, newState);
            }
            
            // Update all boss bars immediately
            updatePlayerBossBars(player);
            return newState;
        } else {
            // Toggle specific boss bar
            boolean currentState = playerSettings.getOrDefault(bossBarName, !config.isBossBarHiddenByDefault());
            boolean newState = !currentState;
            playerSettings.put(bossBarName, newState);
            
            // Update this boss bar immediately
            BossBarInstance bossBar = bossBars.get(bossBarName);
            if (bossBar != null) {
                updateBossBarForPlayer(player, bossBar);
            }
            
            return newState;
        }
    }
    
    /**
     * Handle player joining - show appropriate boss bars
     * @param player The joining player
     */
    public void onPlayerJoin(ServerPlayer player) {
        if (config == null || !config.isBossBarEnabled()) return;
        
        // Initialize player settings if remember toggle is enabled
        if (config.isBossBarRememberToggle()) {
            // Settings are persistent, will be loaded from storage
            // For now, use defaults
            updatePlayerBossBars(player);
        } else {
            // Reset to default state
            playerVisibility.remove(player.getUUID());
            updatePlayerBossBars(player);
        }
    }
    
    /**
     * Handle player leaving - clean up boss bars
     * @param player The leaving player
     */
    public void onPlayerLeave(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        
        // Remove from all boss bar visibility lists
        for (BossBarInstance bossBar : bossBars.values()) {
            bossBar.visibleTo.remove(playerUUID);
        }
        
        // Clean up player settings if not remembering toggle state
        if (!config.isBossBarRememberToggle()) {
            playerVisibility.remove(playerUUID);
        }
    }
    
    /**
     * Reload boss bar configuration
     * @param config The new configuration
     */
    public void reload(TABConfig config) {
        shutdown();
        initialize(config);
    }
    
    /**
     * Clean up all boss bars
     */
    public void shutdown() {
        // Remove all boss bars from all players
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                for (BossBarInstance bossBar : bossBars.values()) {
                    hideBossBar(player, bossBar);
                }
            }
        }
        
        bossBars.clear();
        playerVisibility.clear();
        
        NeoEssentials.LOGGER.info("BossBarManager shutdown");
    }
    
    /**
     * Boss bar instance wrapper
     */
    private static class BossBarInstance {
        final String name;
        final TABConfig.BossBarConfig config;
        final Map<UUID, Boolean> visibleTo = new ConcurrentHashMap<>();
        BossEvent bossEvent;
        
        BossBarInstance(String name, TABConfig.BossBarConfig config) {
            this.name = name;
            this.config = config;
        }
    }
}
