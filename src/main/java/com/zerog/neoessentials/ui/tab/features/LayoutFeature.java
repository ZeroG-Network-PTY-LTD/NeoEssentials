package com.zerog.neoessentials.ui.tab.features;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.tab.TabManager;
import com.zerog.neoessentials.ui.tab.TabPlayerData;

import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles custom layout for the tablist
 * Allows for fixed slots, grouping, and special display arrangements
 */
public class LayoutFeature extends AbstractFeature {
    // Layout types
    public enum LayoutType {
        DEFAULT,   // Standard Minecraft layout
        FIXED,     // Players in fixed positions
        GROUP      // Players grouped by permission groups
    }
    
    // Configuration
    private boolean enabled = false;
    private LayoutType layoutType = LayoutType.DEFAULT;
    private Map<Integer, String> fixedSlots = new HashMap<>();
    private Map<String, List<Integer>> groupSlots = new HashMap<>();
    private boolean fillEmptySlots = true;
    
    // Cache of player assignments
    private final Map<UUID, Integer> slotAssignments = new ConcurrentHashMap<>();
    
    /**
     * Creates a new layout feature
     * 
     * @param tabManager The tab manager
     */
    public LayoutFeature(TabManager tabManager) {
        super(tabManager);
    }
    
    @Override
    public void initialize() {
        NeoEssentials.LOGGER.info("Initializing layout feature");
    }
    
    @Override
    public void loadConfig() {
        // TODO: Load from config
        // In a real implementation, you'd load these from TablistTomlConfig
        enabled = true;
        layoutType = LayoutType.GROUP;
        fillEmptySlots = true;
        
        // Example fixed slots
        Map<Integer, String> fixed = new HashMap<>();
        fixed.put(0, "&c&lADMINS"); // Header at position 0
        fixed.put(10, "&a&lMODS");  // Header at position 10
        fixed.put(20, "&e&lVIPS");  // Header at position 20
        fixed.put(30, "&f&lPLAYERS"); // Header at position 30
        this.fixedSlots = fixed;
        
        // Example group slots
        Map<String, List<Integer>> groups = new HashMap<>();
        groups.put("admin", Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9)); // Admin slots
        groups.put("mod", Arrays.asList(11, 12, 13, 14, 15, 16, 17, 18, 19)); // Mod slots
        groups.put("vip", Arrays.asList(21, 22, 23, 24, 25, 26, 27, 28, 29)); // VIP slots
        groups.put("default", Arrays.asList(31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45)); // Regular player slots
        this.groupSlots = groups;
    }
    
    @Override
    public void update() {
        if (!isEnabled() || server == null) return;
        
        // Nothing to do here - layout is applied when other features update the tablist
        // Slot assignments are calculated on demand
    }
    
    /**
     * Calculates slot assignments for all online players
     * 
     * @return Map of players to their assigned slots
     */
    public Map<UUID, Integer> calculateSlotAssignments() {
        // Clear existing assignments
        slotAssignments.clear();
        
        // Skip if feature is disabled
        if (!enabled) {
            return slotAssignments;
        }
        
        // Get all online players
        List<ServerPlayer> players = tabManager.getOnlinePlayers();
        if (players.isEmpty()) {
            return slotAssignments;
        }
        
        // Apply the selected layout type
        switch (layoutType) {
            case FIXED:
                applyFixedLayout(players);
                break;
            case GROUP:
                applyGroupLayout(players);
                break;
            case DEFAULT:
            default:
                // Default layout doesn't need slot assignments
                break;
        }
        
        return slotAssignments;
    }
    
    /**
     * Applies fixed layout positioning
     * 
     * @param players The list of online players
     */
    private void applyFixedLayout(List<ServerPlayer> players) {
        // Set fixed slots (strings/headers)
        // In a real implementation, you'd communicate these to the client
        
        // Assign remaining players to available slots
        int nextSlot = 0;
        for (ServerPlayer player : players) {
            // Skip if player already has a fixed slot assignment
            if (slotAssignments.containsKey(player.getUUID())) {
                continue;
            }
            
            // Find next available slot
            while (fixedSlots.containsKey(nextSlot)) {
                nextSlot++;
            }
            
            // Assign player to this slot
            slotAssignments.put(player.getUUID(), nextSlot);
            nextSlot++;
        }
    }
    
    /**
     * Applies group-based layout positioning
     * 
     * @param players The list of online players
     */
    private void applyGroupLayout(List<ServerPlayer> players) {
        // First, organize players by group
        Map<String, List<ServerPlayer>> playersByGroup = new HashMap<>();
        for (ServerPlayer player : players) {
            TabPlayerData playerData = tabManager.getPlayerData(player);
            if (playerData == null) continue;
            
            String group = playerData.getGroup();
            if (group == null || group.isEmpty()) {
                group = "default";
            }
            
            playersByGroup.computeIfAbsent(group, k -> new ArrayList<>()).add(player);
        }
        
        // Keep track of how many slots we've used in each group
        Map<String, Integer> groupUsage = new HashMap<>();
        
        // Assign players to their group slots
        for (Map.Entry<String, List<ServerPlayer>> entry : playersByGroup.entrySet()) {
            String group = entry.getKey();
            List<ServerPlayer> groupPlayers = entry.getValue();
            
            // Get slots for this group
            List<Integer> slots = groupSlots.getOrDefault(group, Collections.emptyList());
            if (slots.isEmpty() && fillEmptySlots) {
                // If no slots configured for this group and fillEmptySlots is enabled,
                // find unused slots
                slots = findUnusedSlots(groupPlayers.size());
            }
            
            // Initialize group usage counter
            groupUsage.put(group, 0);
            
            // Assign players to slots
            for (ServerPlayer player : groupPlayers) {
                int usageIndex = groupUsage.get(group);
                if (usageIndex < slots.size()) {
                    int slot = slots.get(usageIndex);
                    slotAssignments.put(player.getUUID(), slot);
                    groupUsage.put(group, usageIndex + 1);
                }
            }
        }
    }
    
    /**
     * Finds unused slots for overflow groups
     * 
     * @param count How many slots needed
     * @return List of available slots
     */
    private List<Integer> findUnusedSlots(int count) {
        List<Integer> availableSlots = new ArrayList<>();
        
        // Start from slot 0 and find the first 'count' unused slots
        int slot = 0;
        while (availableSlots.size() < count) {
            // Check if this slot is already used
            boolean slotUsed = fixedSlots.containsKey(slot);
            
            if (!slotUsed) {
                for (List<Integer> groupSlotList : groupSlots.values()) {
                    if (groupSlotList.contains(slot)) {
                        slotUsed = true;
                        break;
                    }
                }
            }
            
            if (!slotUsed) {
                availableSlots.add(slot);
            }
            
            slot++;
        }
        
        return availableSlots;
    }
    
    /**
     * Gets the slot assignment for a player
     * 
     * @param player The player
     * @return The assigned slot, or -1 if not assigned
     */
    public int getPlayerSlot(ServerPlayer player) {
        // Calculate assignments if needed
        if (slotAssignments.isEmpty()) {
            calculateSlotAssignments();
        }
        
        return slotAssignments.getOrDefault(player.getUUID(), -1);
    }
    
    @Override
    public void onPlayerJoin(ServerPlayer player) {
        if (!isEnabled()) return;
        
        // Recalculate slot assignments when a player joins
        calculateSlotAssignments();
    }
    
    @Override
    public void onPlayerLeave(ServerPlayer player) {
        if (!isEnabled()) return;
        
        // Remove player from assignments
        slotAssignments.remove(player.getUUID());
        
        // Recalculate for remaining players
        calculateSlotAssignments();
    }
    
    @Override
    public void onPlayerChangeWorld(ServerPlayer player, String worldName) {
        // World changes don't affect layout unless we're doing per-world layouts
        // For now, this is handled by the PlayerListFeature
    }
    
    /**
     * Get whether the feature is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Sets whether the feature is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Gets the current layout type
     */
    public LayoutType getLayoutType() {
        return layoutType;
    }
    
    /**
     * Sets the layout type
     */
    public void setLayoutType(LayoutType layoutType) {
        this.layoutType = layoutType;
        
        // Recalculate assignments when layout type changes
        if (isEnabled()) {
            calculateSlotAssignments();
        }
    }
}
