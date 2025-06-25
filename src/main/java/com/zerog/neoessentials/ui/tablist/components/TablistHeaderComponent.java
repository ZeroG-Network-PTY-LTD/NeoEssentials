package com.zerog.neoessentials.ui.tablist.components;

import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * The header component for the tablist
 */
public class TablistHeaderComponent implements TablistComponent {
    private final String id = "header";
    private final String displayName = "Tab Header";
    
    private List<String> lines = new ArrayList<>();
    private Map<String, List<String>> groupLines = new HashMap<>();
    private String animationType = "rotation";
    
    /**
     * Gets the unique ID of this component
     */
    @Override
    public String getId() {
        return id;
    }
    
    /**
     * Gets the display name of this component
     */
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Called when the tablist is being updated
     */
    @Override
    public void update(ServerPlayer player) {
        // No special update logic needed for header
    }
    
    /**
     * Gets the lines of text to display for a player
     * @param player The player
     * @param group The player's group
     * @return The list of lines to display
     */
    public List<String> getLinesForPlayer(ServerPlayer player, String group) {        // Check if player-specific headers are enabled
        boolean enablePlayerSpecific = com.zerog.neoessentials.config.TablistYamlConfig.isEnablePlayerSpecificHeaders();
        if (!enablePlayerSpecific) {
            return new ArrayList<>(lines);
        }
        
        // Check for group-specific lines
        if (group != null && !group.isEmpty() && groupLines.containsKey(group)) {
            List<String> specificLines = groupLines.get(group);
            if (specificLines != null && !specificLines.isEmpty()) {
                return new ArrayList<>(specificLines);
            }
        }
        
        // Fall back to default lines
        return new ArrayList<>(lines);
    }
    
    /**
     * Gets the animation type for this header
     */
    public String getAnimationType() {
        return animationType;
    }
    
    /**
     * Sets the lines of text to display
     */
    public void setLines(List<String> lines) {
        this.lines = new ArrayList<>(lines);
    }
    
    /**
     * Sets the group-specific lines of text
     */
    public void setGroupLines(Map<String, List<String>> groupLines) {
        this.groupLines = new HashMap<>(groupLines);
    }
    
    /**
     * Sets the animation type
     */
    public void setAnimationType(String animationType) {
        this.animationType = animationType;
    }
}
