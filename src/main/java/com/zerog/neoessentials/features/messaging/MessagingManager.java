package com.zerog.neoessentials.features.messaging;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.MessagingConfig;
import com.zerog.neoessentials.storage.StorageManager;

/**
 * Messaging manager for NeoEssentials
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class MessagingManager {
    
    private final MessagingConfig config;
    private final StorageManager storageManager;
    
    public MessagingManager(MessagingConfig config, StorageManager storageManager) {
        this.config = config;
        this.storageManager = storageManager;
        
        NeoEssentials.LOGGER.info("Messaging manager initialized");
    }
}
