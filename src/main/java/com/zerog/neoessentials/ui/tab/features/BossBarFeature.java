package com.zerog.neoessentials.ui.tab.features;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.tab.AnimationManager;
import com.zerog.neoessentials.ui.tab.TabManager;
import com.zerog.neoessentials.ui.tab.TabPlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.level.BossEvent.BossBarCreateEvent;
import net.minecraft.server.level.ServerBossEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles boss bars for the TabManager system
 */
public class BossBarFeature extends AbstractFeature {
    // Patterns for color and style in templates
    private static final Pattern COLOR_PATTERN = Pattern.compile("\\{color:([a-z_]+)\\}");
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("\\{progress:([0-9.]+)\\}");
    private static final Pattern OVERLAY_PATTERN = Pattern.compile("\\{style:([a-z_]+)\\}");
    
    // Boss bar data
    private final Map<String, BossBarConfig> bossBarConfigs = new HashMap<>();
    private final Map<UUID, Map<String, ServerBossEvent>> playerBossBars = new ConcurrentHashMap<>();
    
    // Configuration
    private boolean enabled = false;
    private List<String> globalBossBars = new ArrayList<>();
    private Map<String, List<String>> groupBossBars = new HashMap<>();
    
    /**
     * Creates a new BossBar feature
     * 
     * @param tabManager The tab manager
     */
    public BossBarFeature(TabManager tabManager) {
        super(tabManager);
    }
    
    @Override
    public void initialize() {
        NeoEssentials.LOGGER.info("Initializing boss bar feature");
    }
      @Override
    public void loadConfig() {
        // Load configuration from TablistTomlConfig
        enabled = com.zerog.neoessentials.config.TablistTomlConfig.ENABLE_BOSSBARS.get();
        
        if (!enabled) {
            NeoEssentials.LOGGER.info("BossBar feature is disabled in config");
            return;
        }
        
        // Clear existing configuration
        bossBarConfigs.clear();
        
        // Load global boss bars
        try {
            List<String> configGlobalBars = com.zerog.neoessentials.config.TablistTomlConfig.GLOBAL_BOSSBARS.get();
            if (configGlobalBars != null && !configGlobalBars.isEmpty()) {
                globalBossBars = new ArrayList<>(configGlobalBars);
            } else {
                // Fallback to default examples if none are configured
                globalBossBars = Arrays.asList(
                    "{color:red}{style:progress}{progress:1.0}Server TPS: %tps%",
                    "{color:green}{style:notched_6}{progress:0.8}Welcome to the server!",
                    "{color:blue}{style:progress}{progress:%memory_percent/100%}Memory: %memory_percent%% (%memory_used%/%memory_max% MB)"
                );
                NeoEssentials.LOGGER.info("Using default global boss bars as none were configured");
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error loading global boss bars", e);
            globalBossBars = new ArrayList<>();
        }
        
        // Load group-specific boss bars
        try {
            Map<String, List<String>> configGroupBars = com.zerog.neoessentials.config.TablistTomlConfig.GROUP_BOSSBARS.get();
            if (configGroupBars != null && !configGroupBars.isEmpty()) {
                // Create a deep copy to avoid reference issues
                Map<String, List<String>> groupBars = new HashMap<>();
                for (Map.Entry<String, List<String>> entry : configGroupBars.entrySet()) {
                    groupBars.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                }
                this.groupBossBars = groupBars;
            } else {
                // Fallback to example groups if none are configured
                Map<String, List<String>> groupSpecific = new HashMap<>();
                groupSpecific.put("admin", Arrays.asList(
                    "{color:purple}{style:progress}{progress:1.0}Admin Mode Active",
                    "{color:red}{style:notched_10}{progress:1.0}Server control panel"
                ));
                
                groupSpecific.put("vip", Arrays.asList(
                    "{color:gold}{style:progress}{progress:1.0}VIP Status Active",
                    "{color:yellow}{style:notched_6}{progress:1.0}Thank you for supporting us!"
                ));
                
                this.groupBossBars = groupSpecific;
                NeoEssentials.LOGGER.info("Using default group-specific boss bars as none were configured");
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error loading group-specific boss bars", e);
            this.groupBossBars = new HashMap<>();
        }
        
        // Parse all boss bar templates to prepare configurations
        parseBossBarTemplates();
        
        NeoEssentials.LOGGER.info("BossBar configuration loaded - {} global bars, {} group-specific configurations", 
            globalBossBars.size(), groupBossBars.size());
    }
    
    /**
     * Parses all boss bar templates and prepares the configuration objects
     */
    private void parseBossBarTemplates() {
        // Parse global boss bars
        for (int i = 0; i < globalBossBars.size(); i++) {
            String template = globalBossBars.get(i);
            String barId = "global_" + i;
            bossBarConfigs.put(barId, parseBossBarTemplate(template));
        }
        
        // Parse group-specific boss bars
        for (Map.Entry<String, List<String>> entry : groupBossBars.entrySet()) {
            String group = entry.getKey();
            List<String> templates = entry.getValue();
            
            for (int i = 0; i < templates.size(); i++) {
                String template = templates.get(i);
                String barId = "group_" + group + "_" + i;
                bossBarConfigs.put(barId, parseBossBarTemplate(template));
            }
        }
    }
    
    @Override
    public void update() {
        if (!isEnabled() || server == null) return;
        
        // Update all boss bars for all players
        for (ServerPlayer player : tabManager.getOnlinePlayers()) {
            updatePlayerBossBars(player);
        }
    }
    
    /**
     * Updates boss bars for a specific player
     * 
     * @param player The player to update
     */
    private void updatePlayerBossBars(ServerPlayer player) {
        executeWithErrorLogging(() -> {
            TabPlayerData playerData = tabManager.getPlayerData(player);
            if (playerData == null) return;
            
            // Get player's group
            String group = playerData.getGroup();
            
            // Get list of boss bars to show
            List<String> barsToShow = new ArrayList<>(globalBossBars);
            if (groupBossBars.containsKey(group)) {
                barsToShow.addAll(groupBossBars.get(group));
            }
            
            if (barsToShow.isEmpty()) {
                // If no boss bars to show, remove any existing ones
                removeAllBossBars(player);
                return;
            }
            
            // Get or create boss bar map for this player
            Map<String, ServerBossEvent> bossBars = playerBossBars.computeIfAbsent(
                player.getUUID(), k -> new ConcurrentHashMap<>());
            
            // Track which boss bars to keep
            Set<String> barsToKeep = new HashSet<>();
            
            // Process each boss bar
            for (int i = 0; i < barsToShow.size(); i++) {
                String barId = "bar_" + i;
                barsToKeep.add(barId);
                
                String template = barsToShow.get(i);
                
                // Parse the template and extract properties
                BossBarConfig config = parseBossBarTemplate(template);
                
                // Replace placeholders in title
                String processedTitle = tabManager.getPlaceholderManager().replacePlaceholders(config.title, player);
                
                // Process placeholders in progress
                if (config.progress < 0) {
                    // Negative progress means it should be dynamically calculated from a placeholder
                    try {
                        String progressValue = config.progressVariable;
                        String processed = tabManager.getPlaceholderManager().replacePlaceholders(progressValue, player);
                        config.progress = Float.parseFloat(processed);
                    } catch (Exception e) {
                        config.progress = 1.0f;
                        tabManager.getErrorLogger().logError(
                            "Error processing boss bar progress value: " + config.progressVariable, e);
                    }
                }
                
                // Clamp progress value
                config.progress = Math.max(0, Math.min(1.0f, config.progress));
                
                // Get or create the boss bar
                ServerBossEvent bossBar = bossBars.computeIfAbsent(barId, k -> {
                    ServerBossEvent newBar = new ServerBossEvent(
                        Component.literal(processedTitle),
                        config.color,
                        config.overlay
                    );
                    newBar.addPlayer(player);
                    return newBar;
                });
                
                // Update boss bar
                bossBar.setName(Component.literal(processedTitle));
                bossBar.setColor(config.color);
                bossBar.setOverlay(config.overlay);
                bossBar.setProgress(config.progress);
            }
            
            // Remove any boss bars that are no longer needed
            Iterator<Map.Entry<String, ServerBossEvent>> iterator = bossBars.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, ServerBossEvent> entry = iterator.next();
                if (!barsToKeep.contains(entry.getKey())) {
                    entry.getValue().removePlayer(player);
                    iterator.remove();
                }
            }
        }, "Error updating boss bars for player " + player.getScoreboardName());
    }
    
    /**
     * Removes all boss bars for a player
     * 
     * @param player The player
     */
    private void removeAllBossBars(ServerPlayer player) {
        Map<String, ServerBossEvent> bossBars = playerBossBars.remove(player.getUUID());
        if (bossBars != null) {
            for (ServerBossEvent bossBar : bossBars.values()) {
                bossBar.removePlayer(player);
            }
        }
    }
    
    /**
     * Parses a boss bar template string to extract properties
     * 
     * @param template The template string
     * @return The parsed boss bar configuration
     */
    private BossBarConfig parseBossBarTemplate(String template) {
        BossBarConfig config = new BossBarConfig();
        
        // Extract color if present
        Matcher colorMatcher = COLOR_PATTERN.matcher(template);
        if (colorMatcher.find()) {
            String colorName = colorMatcher.group(1).toUpperCase();
            try {
                config.color = BossBarColor.valueOf(colorName);
            } catch (IllegalArgumentException e) {
                NeoEssentials.LOGGER.warn("Invalid boss bar color: {}", colorName);
            }
            template = colorMatcher.replaceAll("");
        }
        
        // Extract overlay/style if present
        Matcher overlayMatcher = OVERLAY_PATTERN.matcher(template);
        if (overlayMatcher.find()) {
            String overlayName = overlayMatcher.group(1).toUpperCase();
            try {
                config.overlay = BossBarOverlay.valueOf(overlayName);
            } catch (IllegalArgumentException e) {
                NeoEssentials.LOGGER.warn("Invalid boss bar overlay: {}", overlayName);
            }
            template = overlayMatcher.replaceAll("");
        }
        
        // Extract progress if present
        Matcher progressMatcher = PROGRESS_PATTERN.matcher(template);
        if (progressMatcher.find()) {
            String progressStr = progressMatcher.group(1);
            
            // Check if this is a placeholder variable
            if (progressStr.contains("%")) {
                config.progress = -1.0f; // Mark for dynamic processing
                config.progressVariable = progressStr;
            } else {
                try {
                    config.progress = Float.parseFloat(progressStr);
                } catch (NumberFormatException e) {
                    NeoEssentials.LOGGER.warn("Invalid boss bar progress: {}", progressStr);
                }
            }
            template = progressMatcher.replaceAll("");
        }
        
        // The remaining text is the title
        config.title = template.trim();
        
        return config;
    }
    
    @Override
    public void onPlayerJoin(ServerPlayer player) {
        if (!isEnabled()) return;
        
        // Initialize boss bars for the joining player
        updatePlayerBossBars(player);
    }
    
    @Override
    public void onPlayerLeave(ServerPlayer player) {
        if (!isEnabled()) return;
        
        // Remove all boss bars for the leaving player
        removeAllBossBars(player);
    }
    
    @Override
    public void onPlayerChangeWorld(ServerPlayer player, String worldName) {
        if (!isEnabled()) return;
        
        // Update boss bars when player changes world
        updatePlayerBossBars(player);
    }
    
    /**
     * Class to store boss bar configuration
     */
    private static class BossBarConfig {
        String title = "";
        BossBarColor color = BossBarColor.WHITE;
        BossBarOverlay overlay = BossBarOverlay.PROGRESS;
        float progress = 1.0f;
        String progressVariable = null;
    }
}
