package com.zerog.neoessentials.ui.tab.features;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.tab.TabManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Base implementation for all feature modules
 * Provides common functionality and default implementations
 */
public abstract class AbstractFeature implements Feature {
    protected final TabManager tabManager;
    protected MinecraftServer server;
    protected boolean enabled = true;
    
    /**
     * Creates a new feature
     * 
     * @param tabManager The tab manager
     */
    public AbstractFeature(TabManager tabManager) {
        this.tabManager = tabManager;
    }
    
    @Override
    public TabManager getTabManager() {
        return tabManager;
    }
    
    @Override
    public void initialize() {
        // Default implementation does nothing
    }
    
    @Override
    public void loadConfig() {
        // Default implementation does nothing
    }
    
    @Override
    public void update() {
        // Default implementation does nothing
    }
    
    @Override
    public void onPlayerJoin(ServerPlayer player) {
        // Default implementation does nothing
    }
    
    @Override
    public void onPlayerLeave(ServerPlayer player) {
        // Default implementation does nothing
    }
    
    @Override
    public void onPlayerChangeWorld(ServerPlayer player, String worldName) {
        // Default implementation does nothing
    }
    
    @Override
    public void onServerChanged(MinecraftServer server) {
        this.server = server;
    }
    
    /**
     * Set the enabled state
     * 
     * @param enabled true if enabled, false otherwise
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        
        if (enabled) {
            NeoEssentials.LOGGER.debug("{} feature enabled", getClass().getSimpleName());
        } else {
            NeoEssentials.LOGGER.debug("{} feature disabled", getClass().getSimpleName());
        }
    }
    
    /**
     * Check if the feature is enabled
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Execute a protected operation with error logging
     * 
     * @param operation The operation to execute
     * @param errorMessage The error message to log
     */
    protected void executeWithErrorLogging(Runnable operation, String errorMessage) {
        try {
            operation.run();
        } catch (Exception e) {
            tabManager.getErrorLogger().logError(errorMessage, e);
        }
    }
}
