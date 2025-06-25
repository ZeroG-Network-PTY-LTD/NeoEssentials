package com.zerog.neoessentials.ui.tab.features;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.TablistTomlConfig;
import com.zerog.neoessentials.ui.tab.AnimationManager;
import com.zerog.neoessentials.ui.tab.TabManager;
import com.zerog.neoessentials.ui.tab.TabPlayerData;
import com.zerog.neoessentials.ui.tab.placeholders.PlaceholderManager;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles header and footer content for the tablist
 */
public class HeaderFooterFeature extends AbstractFeature {
    private List<String> headerTemplates;
    private List<String> footerTemplates;
    private AnimationManager.AnimationType headerAnimationType;
    private AnimationManager.AnimationType footerAnimationType;
    private boolean enablePlayerSpecificHeaders;
    private boolean enablePlayerSpecificFooters;
    
    // Group-specific headers and footers
    private final Map<String, List<String>> groupHeaders = new HashMap<>();
    private final Map<String, List<String>> groupFooters = new HashMap<>();
    
    // Cache of last sent headers/footers to avoid unnecessary updates
    private final Map<UUID, String> lastSentHeaders = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastSentFooters = new ConcurrentHashMap<>();
    
    /**
     * Creates a new header/footer feature
     * 
     * @param tabManager The tab manager
     */
    public HeaderFooterFeature(TabManager tabManager) {
        super(tabManager);
    }
    
    @Override
    public void initialize() {
        NeoEssentials.LOGGER.info("Initializing header/footer feature");
    }    @Override
    public void loadConfig() {
        // Load templates from the template manager
        this.headerTemplates = getTabManager().getTemplateManager().getGlobalHeaders();
        this.footerTemplates = getTabManager().getTemplateManager().getGlobalFooters();
        
        // Load animation types
        String headerAnimType = TablistTomlConfig.HEADER_ANIMATION_TYPE.get();
        String footerAnimType = TablistTomlConfig.FOOTER_ANIMATION_TYPE.get();
        this.headerAnimationType = AnimationManager.AnimationType.fromString(headerAnimType);
        this.footerAnimationType = AnimationManager.AnimationType.fromString(footerAnimType);
        
        // Load group-specific settings
        this.enablePlayerSpecificHeaders = TablistTomlConfig.ENABLE_PLAYER_SPECIFIC_HEADERS.get();
        this.enablePlayerSpecificFooters = TablistTomlConfig.ENABLE_PLAYER_SPECIFIC_FOOTERS.get();
        
        // Load group-specific headers/footers from the template manager
        loadGroupSpecificTemplatesFromManager();
        
        NeoEssentials.LOGGER.info("Header/footer feature config loaded");
    }
      /**
     * Loads group-specific header and footer templates from template manager
     */
    private void loadGroupSpecificTemplatesFromManager() {
        // Clear existing data
        groupHeaders.clear();
        groupFooters.clear();
          // Get available group names from the template manager's headers and footers
        Map<String, List<String>> templateGroupHeaders = getTabManager().getTemplateManager().getAllGroupHeaders();
        Map<String, List<String>> templateGroupFooters = getTabManager().getTemplateManager().getAllGroupFooters();
        
        // Process headers for all groups
        for (Map.Entry<String, List<String>> entry : templateGroupHeaders.entrySet()) {
            String groupName = entry.getKey();
            List<String> headers = entry.getValue();
            if (headers != null && !headers.isEmpty()) {
                groupHeaders.put(groupName, new ArrayList<>(headers));
            }
        }
        
        // Process footers for all groups
        for (Map.Entry<String, List<String>> entry : templateGroupFooters.entrySet()) {
            String groupName = entry.getKey();
            List<String> footers = entry.getValue();
            if (footers != null && !footers.isEmpty()) {
                groupFooters.put(groupName, new ArrayList<>(footers));
            }
        }
        
        NeoEssentials.LOGGER.info("Loaded header/footer templates for {} groups", groupHeaders.size());
    }
    
    @Override
    public void update() {
        if (!isEnabled() || server == null) return;
        
        // Update header/footer for all online players
        for (ServerPlayer player : tabManager.getOnlinePlayers()) {
            updatePlayerHeaderFooter(player);
        }
    }
    
    /**
     * Updates header and footer for a specific player
     * 
     * @param player The player to update
     */
    public void updatePlayerHeaderFooter(ServerPlayer player) {
        executeWithErrorLogging(() -> {
            TabPlayerData playerData = tabManager.getPlayerData(player);
            if (playerData == null) return;
            
            // Get player's group for group-specific content
            String group = playerData.getGroup();
            
            // Get and process header
            String header = getProcessedHeader(player, group);
            
            // Get and process footer
            String footer = getProcessedFooter(player, group);
            
            // Only send update if content changed
            if (!header.equals(lastSentHeaders.getOrDefault(player.getUUID(), "")) || 
                !footer.equals(lastSentFooters.getOrDefault(player.getUUID(), ""))) {
                
                // Send packet to update header/footer
                Component headerComponent = Component.literal(header);
                Component footerComponent = Component.literal(footer);
                
                ClientboundTabListPacket packet = new ClientboundTabListPacket(headerComponent, footerComponent);
                player.connection.send(packet);
                
                // Cache the sent content
                lastSentHeaders.put(player.getUUID(), header);
                lastSentFooters.put(player.getUUID(), footer);
            }
        }, "Error updating header/footer for player " + player.getScoreboardName());
    }
    
    /**
     * Gets the processed header for a player
     * 
     * @param player The player
     * @param group The player's permission group
     * @return The processed header
     */
    private String getProcessedHeader(ServerPlayer player, String group) {
        List<String> templates = headerTemplates;
        
        // Use group-specific header if enabled and available
        if (enablePlayerSpecificHeaders && groupHeaders.containsKey(group)) {
            templates = groupHeaders.get(group);
        }
        
        // Process animation
        String header = tabManager.getAnimationManager().processAnimation(
            headerAnimationType, templates, player);
        
        // Process placeholders
        return tabManager.getPlaceholderManager().replacePlaceholders(header, player);
    }
    
    /**
     * Gets the processed footer for a player
     * 
     * @param player The player
     * @param group The player's permission group
     * @return The processed footer
     */
    private String getProcessedFooter(ServerPlayer player, String group) {
        List<String> templates = footerTemplates;
        
        // Use group-specific footer if enabled and available
        if (enablePlayerSpecificFooters && groupFooters.containsKey(group)) {
            templates = groupFooters.get(group);
        }
        
        // Process animation
        String footer = tabManager.getAnimationManager().processAnimation(
            footerAnimationType, templates, player);
        
        // Process placeholders
        return tabManager.getPlaceholderManager().replacePlaceholders(footer, player);
    }
    
    @Override
    public void onPlayerJoin(ServerPlayer player) {
        // Update header/footer when player joins
        updatePlayerHeaderFooter(player);
    }
    
    @Override
    public void onPlayerChangeWorld(ServerPlayer player, String worldName) {
        // Re-send header/footer when player changes world (may need different world-specific content)
        updatePlayerHeaderFooter(player);
    }
}
