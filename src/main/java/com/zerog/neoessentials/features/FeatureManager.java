package com.zerog.neoessentials.features;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.config.MainConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Feature manager for NeoEssentials
 * 
 * Controls which features are enabled/disabled
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class FeatureManager {
    
    private final MainConfig config;
    private final Map<String, Boolean> features;
    
    public FeatureManager(ConfigManager configManager) {
        this.config = configManager.getMainConfig();
        this.features = new HashMap<>();
        
        loadFeatures();
    }
    
    private void loadFeatures() {
        features.put("economy", config.enableEconomy);
        features.put("homes", config.enableHomes);
        features.put("warps", config.enableWarps);
        features.put("kits", config.enableKits);
        features.put("moderation", config.enableModeration);
        features.put("messaging", config.enableMessaging);
        features.put("teleport", config.enableTeleport);
        features.put("tablist", config.enableTablist);
        features.put("discord", config.enableDiscord);
        
        NeoEssentials.LOGGER.info("Loaded feature settings - {} features enabled", 
            features.values().stream().mapToInt(enabled -> enabled ? 1 : 0).sum());
    }
    
    public boolean isFeatureEnabled(String feature) {
        return features.getOrDefault(feature, false);
    }
    
    public void setFeatureEnabled(String feature, boolean enabled) {
        features.put(feature, enabled);
    }
    
    public Map<String, Boolean> getAllFeatures() {
        return new HashMap<>(features);
    }
}
