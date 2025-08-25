package com.zerog.neoessentials.managers;

import com.zerog.neoessentials.config.ConfigurationUnifier;
import com.zerog.neoessentials.config.MainConfig;
// import removed: KitConfig is now centralized in MainConfig
import com.zerog.neoessentials.storage.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kit management system for NeoEssentials
 * Handles kit creation, distribution, and cooldown management
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class KitManager {
    
    // Logger removed, not used after refactor
    private static KitManager instance;
    
    private final ConfigurationUnifier configUnifier;
    private final PlayerDataManager playerDataManager;
    private final Map<UUID, Map<String, Long>> kitCooldowns;
    
    private KitManager() {
        this.configUnifier = ConfigurationUnifier.getInstance();
        this.playerDataManager = PlayerDataManager.getInstance();
        this.kitCooldowns = new ConcurrentHashMap<>();
    }
    
    public static KitManager getInstance() {
        if (instance == null) {
            instance = new KitManager();
        }
        return instance;
    }
    
    /**
     * Give a kit to a player
     */
    public boolean giveKit(ServerPlayer player, String kitName) {
        boolean kitModuleEnabled = configUnifier.getConfigManager().getMainConfig().modules.kits;
        if (!kitModuleEnabled) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("neoessentials.kit.disabled"));
            return false;
        }
        // Since kits are no longer defined in config, this is a stub for kit logic.
        // You must implement kit lookup and definition elsewhere (e.g., KitRegistry, external file, etc.)
        // For now, just send a not found message.
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("neoessentials.kit.not_found", kitName));
        return false;
    }
    
    /**
     * Get available kits for a player
     */
    public List<String> getAvailableKits(ServerPlayer player) {
    // Kits are no longer defined in config. Implement kit registry elsewhere.
    return java.util.Collections.emptyList();
    }
    
    // hasKitPermission removed, not used after refactor
    
    /**
     * Check if player is on cooldown for kit
     */
    public boolean isOnCooldown(ServerPlayer player, String kitName) {
    // Kits are no longer defined in config. Implement cooldown logic elsewhere.
    return false;
    }
    
    /**
     * Get remaining cooldown time in milliseconds
     */
    public long getRemainingCooldown(ServerPlayer player, String kitName) {
    // Kits are no longer defined in config. Implement cooldown logic elsewhere.
    return 0;
    }
    
    /**
     * Set cooldown for player and kit
     */
    
    // giveKitItems removed, not used after refactor
    
    // parseItemString removed, not used after refactor
    
    // isArmorItem removed, not used after refactor
    
    // equipArmor removed, not used after refactor
    
    // executeKitCommands removed, not used after refactor
    
    /**
     * Give first join kit if enabled
     */
    public void giveFirstJoinKit(ServerPlayer player) {
        MainConfig.KitSettings kitSettings = configUnifier.getConfigManager().getMainConfig().kitSettings;
        if (!kitSettings.giveKitOnFirstJoin || kitSettings.firstJoinKit.isEmpty()) {
            return;
        }
        Object received = playerDataManager.getSetting(player.getUUID(), "received_first_join_kit");
        if (Boolean.TRUE.equals(received) || "true".equals(String.valueOf(received))) {
            return;
        }
        if (giveKit(player, kitSettings.firstJoinKit)) {
            playerDataManager.setSetting(player.getUUID(), "received_first_join_kit", true);
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("neoessentials.kit.first_join", kitSettings.firstJoinKit));
        }
    }
    
    /**
     * Clean up expired cooldowns
     */
    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        
        kitCooldowns.entrySet().removeIf(playerEntry -> {
            Map<String, Long> playerCooldowns = playerEntry.getValue();
            playerCooldowns.entrySet().removeIf(kitEntry -> {
                // This would need kit lookup to get proper cooldown time
                // For now, clean up cooldowns older than 24 hours
                return currentTime - kitEntry.getValue() > 24 * 60 * 60 * 1000L;
            });
            
            return playerCooldowns.isEmpty();
        });
    }
}
