package com.zerog.neoessentials.ui.tab.features;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.tab.TabManager;
import com.zerog.neoessentials.ui.tab.TabPlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles text displayed below player names in the game world
 */
public class BelowNameFeature extends AbstractFeature {
    // Cache to track last sent values
    private final Map<UUID, String> lastSentText = new ConcurrentHashMap<>();
    
    // Configuration
    private boolean enabled = false;
    private List<String> templates = new ArrayList<>();
    private Map<String, List<String>> groupTemplates = new HashMap<>();
    private int updateInterval = 40; // ticks
    private boolean useCustomNameVisible = true;
    
    // EntityDataAccessor for custom name visible
    private static EntityDataAccessor<Boolean> DATA_CUSTOM_NAME_VISIBLE;
    
    // EntityDataAccessor for custom name
    private static EntityDataAccessor<Optional<Component>> DATA_CUSTOM_NAME;
    
    // Flag to track if reflection initialization succeeded
    private boolean reflectionInitialized = false;
    
    /**
     * Creates a new BelowName feature
     * 
     * @param tabManager The tab manager
     */
    public BelowNameFeature(TabManager tabManager) {
        super(tabManager);
        
        // Try to initialize reflection access
        initReflection();
    }
    
    /**
     * Initialize reflection to access entity data fields
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void initReflection() {
        try {
            // Get entity data fields via reflection
            Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");
            
            // Get DATA_CUSTOM_NAME field
            Field customNameField = entityClass.getDeclaredField("DATA_CUSTOM_NAME");
            customNameField.setAccessible(true);
            DATA_CUSTOM_NAME = (EntityDataAccessor<Optional<Component>>) customNameField.get(null);
            
            // Get DATA_CUSTOM_NAME_VISIBLE field
            Field customNameVisibleField = entityClass.getDeclaredField("DATA_CUSTOM_NAME_VISIBLE");
            customNameVisibleField.setAccessible(true);
            DATA_CUSTOM_NAME_VISIBLE = (EntityDataAccessor<Boolean>) customNameVisibleField.get(null);
            
            reflectionInitialized = true;
            NeoEssentials.LOGGER.debug("BelowNameFeature reflection initialization successful");
        } catch (Exception e) {
            // If reflection fails, use default indices (these may need updating per Minecraft version)
            NeoEssentials.LOGGER.error("BelowNameFeature reflection initialization failed", e);
            
            try {
                // Try to use known indices as fallback
                DATA_CUSTOM_NAME = new EntityDataAccessor<>(2, EntityDataSerializers.OPTIONAL_COMPONENT);
                DATA_CUSTOM_NAME_VISIBLE = new EntityDataAccessor<>(3, EntityDataSerializers.BOOLEAN);
                reflectionInitialized = true;
                NeoEssentials.LOGGER.warn("Using hardcoded entity data indices - this may break in future versions");
            } catch (Exception ex) {
                NeoEssentials.LOGGER.error("BelowNameFeature fallback initialization failed", ex);
                reflectionInitialized = false;
            }
        }
    }
    
    @Override
    public void initialize() {
        NeoEssentials.LOGGER.info("Initializing below name feature");
        
        // If reflection initialization failed, disable the feature
        if (!reflectionInitialized) {
            NeoEssentials.LOGGER.warn("BelowNameFeature disabled due to reflection initialization failure");
            setEnabled(false);
        }
    }
    
    @Override
    public void loadConfig() {
        // TODO: Load from config
        // In a real implementation, you'd load these from TablistTomlConfig
        enabled = true;
        templates = Arrays.asList(
            "&e%health%/%max_health% &c❤",
            "&b%ping%ms",
            "&a%world%"
        );
        
        // Example group templates
        groupTemplates.put("admin", Arrays.asList(
            "&c&lADMIN",
            "&4%health%/%max_health% &c❤"
        ));
        
        groupTemplates.put("vip", Arrays.asList(
            "&e&lVIP",
            "&6%health%/%max_health% &c❤"
        ));
        
        updateInterval = 20;
    }
    
    @Override
    public void update() {
        if (!isEnabled() || !reflectionInitialized || server == null) return;
        
        // Only update on certain ticks to reduce packet spam
        if (server.getTickCount() % updateInterval != 0) return;
        
        // Update below name for each player
        for (ServerPlayer player : tabManager.getOnlinePlayers()) {
            updatePlayerBelowName(player);
        }
    }
    
    /**
     * Updates the below name text for a specific player
     * 
     * @param player The player to update
     */
    private void updatePlayerBelowName(ServerPlayer player) {
        executeWithErrorLogging(() -> {
            TabPlayerData playerData = tabManager.getPlayerData(player);
            if (playerData == null) return;
            
            // Get player's group
            String group = playerData.getGroup();
            
            // Get template for group or use default
            List<String> playerTemplates = groupTemplates.getOrDefault(group, templates);
            if (playerTemplates.isEmpty()) return;
            
            // Get text to display (rotate through templates)
            int frame = (server.getTickCount() / updateInterval) % playerTemplates.size();
            String template = playerTemplates.get(frame);
            
            // Process placeholders
            String processedText = tabManager.getPlaceholderManager().replacePlaceholders(template, player);
            
            // Check if text changed from last update
            if (!processedText.equals(lastSentText.getOrDefault(player.getUUID(), ""))) {
                // Update player's below name text
                setBelowNameText(player, processedText);
                
                // Update cache
                lastSentText.put(player.getUUID(), processedText);
                playerData.setBelowNameText(processedText);
            }
        }, "Error updating below name for player " + player.getScoreboardName());
    }
    
    /**
     * Sets the below-name text for a player
     * 
     * @param player The target player
     * @param text The text to display
     */
    public void setBelowNameText(ServerPlayer player, String text) {
        if (!reflectionInitialized) return;
        
        // Create synched data list for the packet
        List<SynchedEntityData.DataValue<?>> dataValues = new ArrayList<>();
        
        // Add custom name value
        dataValues.add(SynchedEntityData.DataValue.create(
            DATA_CUSTOM_NAME, Optional.of(Component.literal(text))));
        
        // Add custom name visible value if needed
        if (useCustomNameVisible) {
            dataValues.add(SynchedEntityData.DataValue.create(
                DATA_CUSTOM_NAME_VISIBLE, true));
        }
        
        // Create and broadcast the entity data packet
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(
            player.getId(), dataValues);
        
        // Send to all players who can see this player
        for (ServerPlayer viewer : tabManager.getOnlinePlayers()) {
            // Skip sending the packet to vanished players or the player themselves
            // (to avoid showing belowname text to themselves)
            TabPlayerData viewerData = tabManager.getPlayerData(viewer);
            if (viewerData != null && (viewerData.isVanished() || viewer.getUUID().equals(player.getUUID()))) {
                continue;
            }
            
            viewer.connection.send(packet);
        }
    }
    
    /**
     * Removes the below name text for a player
     * 
     * @param player The target player
     */
    public void removeBelowNameText(ServerPlayer player) {
        if (!reflectionInitialized) return;
        
        // Create synched data list for the packet
        List<SynchedEntityData.DataValue<?>> dataValues = new ArrayList<>();
        
        // Reset custom name value
        dataValues.add(SynchedEntityData.DataValue.create(
            DATA_CUSTOM_NAME, Optional.empty()));
        
        // Reset custom name visible value if needed
        if (useCustomNameVisible) {
            dataValues.add(SynchedEntityData.DataValue.create(
                DATA_CUSTOM_NAME_VISIBLE, false));
        }
        
        // Create and broadcast the entity data packet
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(
            player.getId(), dataValues);
        
        // Send to all players
        for (ServerPlayer viewer : tabManager.getOnlinePlayers()) {
            viewer.connection.send(packet);
        }
        
        // Update cache
        lastSentText.remove(player.getUUID());
    }
    
    @Override
    public void onPlayerJoin(ServerPlayer player) {
        if (!isEnabled() || !reflectionInitialized) return;
        
        // Update the joining player's below name text
        updatePlayerBelowName(player);
        
        // Send all other players' below name data to the joining player
        for (ServerPlayer otherPlayer : tabManager.getOnlinePlayers()) {
            if (!otherPlayer.getUUID().equals(player.getUUID())) {
                String text = lastSentText.getOrDefault(otherPlayer.getUUID(), "");
                if (!text.isEmpty()) {
                    // Create packets just for this joining player to see other players' below name text
                    List<SynchedEntityData.DataValue<?>> dataValues = new ArrayList<>();
                    dataValues.add(SynchedEntityData.DataValue.create(
                        DATA_CUSTOM_NAME, Optional.of(Component.literal(text))));
                    
                    if (useCustomNameVisible) {
                        dataValues.add(SynchedEntityData.DataValue.create(
                            DATA_CUSTOM_NAME_VISIBLE, true));
                    }
                    
                    ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(
                        otherPlayer.getId(), dataValues);
                    
                    player.connection.send(packet);
                }
            }
        }
    }
    
    @Override
    public void onPlayerLeave(ServerPlayer player) {
        if (!isEnabled()) return;
        
        // Remove from cache
        lastSentText.remove(player.getUUID());
    }
    
    @Override
    public void onPlayerChangeWorld(ServerPlayer player, String worldName) {
        if (!isEnabled() || !reflectionInitialized) return;
        
        // Update below name text when player changes world
        updatePlayerBelowName(player);
    }
}
