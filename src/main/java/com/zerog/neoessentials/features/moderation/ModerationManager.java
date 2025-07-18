package com.zerog.neoessentials.features.moderation;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.ModerationConfig;
import com.zerog.neoessentials.storage.StorageManager;

/**
 * Moderation manager for NeoEssentials
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class ModerationManager {
    
    private final ModerationConfig config;
    private final StorageManager storageManager;
    
    public ModerationManager(ModerationConfig config, StorageManager storageManager) {
        this.config = config;
        this.storageManager = storageManager;
        
        NeoEssentials.LOGGER.info("Moderation manager initialized");
    }
}
