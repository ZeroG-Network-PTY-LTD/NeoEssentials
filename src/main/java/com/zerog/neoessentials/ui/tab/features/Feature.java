package com.zerog.neoessentials.ui.tab.features;

import com.zerog.neoessentials.ui.tab.TabManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Base interface for all TabManager feature modules
 */
public interface Feature {
    /**
     * Initialize the feature
     */
    void initialize();
    
    /**
     * Load configuration settings
     */
    void loadConfig();
    
    /**
     * Update the feature state
     */
    void update();
    
    /**
     * Called when a player joins the server
     * 
     * @param player The player who joined
     */
    void onPlayerJoin(ServerPlayer player);
    
    /**
     * Called when a player leaves the server
     * 
     * @param player The player who left
     */
    void onPlayerLeave(ServerPlayer player);
    
    /**
     * Called when a player changes worlds
     * 
     * @param player The player who changed worlds
     * @param worldName The name of the new world
     */
    void onPlayerChangeWorld(ServerPlayer player, String worldName);
    
    /**
     * Called when the server reference changes
     * 
     * @param server The new server instance
     */
    void onServerChanged(MinecraftServer server);
    
    /**
     * Get the TabManager instance
     * 
     * @return The TabManager
     */
    TabManager getTabManager();
}
