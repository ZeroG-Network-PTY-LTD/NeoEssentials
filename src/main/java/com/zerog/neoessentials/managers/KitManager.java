package com.zerog.neoessentials.managers;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.config.MainConfig;
import com.zerog.neoessentials.data.Kit;
import com.zerog.neoessentials.data.KitItem;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.storage.KitStorageManager;
import com.zerog.neoessentials.storage.PlayerDataManager;
import com.zerog.neoessentials.localization.LanguageManager;
import com.zerog.neoessentials.util.DebugUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced Kit management system for NeoEssentials
 * Handles kit creation, distribution, and cooldown management with JSON storage
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class KitManager {
    
    private static KitManager instance;
    
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final KitStorageManager kitStorageManager;
    private final Map<UUID, Map<String, Long>> kitCooldowns;
    
    private KitManager() {
        this.configManager = ConfigManager.getInstance();
        this.playerDataManager = PlayerDataManager.getInstance();
        this.kitStorageManager = KitStorageManager.getInstance();
        this.kitCooldowns = new ConcurrentHashMap<>();
        
        DebugUtil.debugLog("KitManager initialized with JSON storage support");
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
        boolean kitModuleEnabled = configManager.getMainConfig().modules.kits;
        if (!kitModuleEnabled) {
            String message = LanguageManager.getInstance().getMessage(player, "neoessentials.kit.disabled");
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
            return false;
        }
        
        // Get the kit from storage
        Kit kit = kitStorageManager.getKit(kitName);
        if (kit == null) {
            String message = LanguageManager.getInstance().getMessage(player, "neoessentials.kit.not_found", kitName);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
            return false;
        }
        
        // Check if kit is enabled
        if (!kit.isEnabled()) {
            String message = LanguageManager.getInstance().getMessage(player, "neoessentials.kit.disabled_kit", kitName);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
            return false;
        }
        
        // Check permission
        if (kit.requiresPermission()) {
            if (!PermissionUtil.hasPermission(player, kit.getPermission())) {
                String message = LanguageManager.getInstance().getMessage(player, "neoessentials.kit.no_permission", kitName);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
                return false;
            }
        }
        
        // Check one-time only restriction
        if (kit.isOneTimeOnly()) {
            String settingKey = "kit_claimed_" + kitName.toLowerCase();
            Object claimed = playerDataManager.getSetting(player.getUUID(), settingKey);
            if (Boolean.TRUE.equals(claimed) || "true".equals(String.valueOf(claimed))) {
                String message = LanguageManager.getInstance().getMessage(player, "neoessentials.kit.already_claimed", kitName);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
                return false;
            }
        }
        
        // Check cooldown
        if (isOnCooldown(player, kitName)) {
            long remainingTime = getRemainingCooldown(player, kitName);
            String message = LanguageManager.getInstance().getMessage(player, "neoessentials.kit.cooldown", 
                kitName, formatTime(remainingTime));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
            return false;
        }
        
        // Check cost
        if (kit.hasCost()) {
            EconomyManager economyManager = EconomyManager.getInstance();
            if (!economyManager.hasBalance(player.getUUID(), BigDecimal.valueOf(kit.getCost()))) {
                String message = LanguageManager.getInstance().getMessage(player, "neoessentials.kit.insufficient_funds", 
                    kitName, kit.getCost());
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
                return false;
            }
        }
        
        // Give the kit
        boolean success = giveKitItems(player, kit);
        if (success) {
            // Deduct cost if applicable
            if (kit.hasCost()) {
                EconomyManager economyManager = EconomyManager.getInstance();
                economyManager.withdrawBalance(player.getUUID(), BigDecimal.valueOf(kit.getCost()), "Kit purchase: " + kitName);
            }
            
            // Execute kit commands if any
            executeKitCommands(player, kit);
            
            // Set cooldown
            setCooldown(player, kitName, kit.getCooldownMillis());
            
            // Mark as claimed if one-time only
            if (kit.isOneTimeOnly()) {
                String settingKey = "kit_claimed_" + kitName.toLowerCase();
                playerDataManager.setSetting(player.getUUID(), settingKey, true);
            }
            
            String message = LanguageManager.getInstance().getMessage(player, "neoessentials.kit.received", kit.getDisplayName());
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
            
            DebugUtil.debugLog("Player " + player.getName().getString() + " received kit: " + kitName);
            return true;
        }
        
        return false;
    }
    
    /**
     * Get available kits for a player (considering permissions)
     */
    public List<String> getAvailableKits(ServerPlayer player) {
        List<String> availableKits = new ArrayList<>();
        
        for (Kit kit : kitStorageManager.getAllKits()) {
            if (kit.isEnabled()) {
                // Check permission
                if (kit.requiresPermission()) {
                    if (PermissionUtil.hasPermission(player, kit.getPermission())) {
                        availableKits.add(kit.getName());
                    }
                } else {
                    availableKits.add(kit.getName());
                }
            }
        }
        
        return availableKits;
    }
    
    /**
     * Get all kits (for admin commands)
     */
    public List<Kit> getAllKits() {
        return new ArrayList<>(kitStorageManager.getAllKits());
    }
    
    /**
     * Check if player is on cooldown for kit
     */
    public boolean isOnCooldown(ServerPlayer player, String kitName) {
        Map<String, Long> playerCooldowns = kitCooldowns.get(player.getUUID());
        if (playerCooldowns == null) {
            return false;
        }
        
        Long cooldownEnd = playerCooldowns.get(kitName.toLowerCase());
        if (cooldownEnd == null) {
            return false;
        }
        
        return System.currentTimeMillis() < cooldownEnd;
    }
    
    /**
     * Get remaining cooldown time in milliseconds
     */
    public long getRemainingCooldown(ServerPlayer player, String kitName) {
        Map<String, Long> playerCooldowns = kitCooldowns.get(player.getUUID());
        if (playerCooldowns == null) {
            return 0;
        }
        
        Long cooldownEnd = playerCooldowns.get(kitName.toLowerCase());
        if (cooldownEnd == null) {
            return 0;
        }
        
        long remaining = cooldownEnd - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    
    /**
     * Set cooldown for player and kit
     */
    public void setCooldown(ServerPlayer player, String kitName, long cooldownMillis) {
        if (cooldownMillis <= 0) {
            return;
        }
        
        kitCooldowns.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>())
                   .put(kitName.toLowerCase(), System.currentTimeMillis() + cooldownMillis);
    }
    
    /**
     * Clear cooldown for a player and kit
     */
    public void clearCooldown(ServerPlayer player, String kitName) {
        Map<String, Long> playerCooldowns = kitCooldowns.get(player.getUUID());
        if (playerCooldowns != null) {
            playerCooldowns.remove(kitName.toLowerCase());
        }
    }
    
    /**
     * Give kit items to player
     */
    private boolean giveKitItems(ServerPlayer player, Kit kit) {
        if (!kit.hasItems()) {
            return true; // Kit with no items is still successful
        }
        
        try {
            List<ItemStack> itemsToGive = new ArrayList<>();
            
            // Convert kit items to ItemStacks
            for (KitItem kitItem : kit.getItems()) {
                try {
                    ItemStack itemStack = kitItem.toItemStack();
                    if (!itemStack.isEmpty()) {
                        itemsToGive.add(itemStack);
                    }
                } catch (Exception e) {
                    DebugUtil.warnLog("Failed to create item from kit " + kit.getName() + ": " + e.getMessage());
                }
            }
            
            // Give items to player
            for (ItemStack itemStack : itemsToGive) {
                if (!player.getInventory().add(itemStack)) {
                    // Drop items that don't fit
                    player.drop(itemStack, false);
                }
            }
            
            return true;
        } catch (Exception e) {
            DebugUtil.warnLog("Failed to give kit items to player " + player.getName().getString() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Execute kit commands
     */
    private void executeKitCommands(ServerPlayer player, Kit kit) {
        if (!kit.hasCommands()) {
            return;
        }
        
        try {
            for (String command : kit.getCommands()) {
                // Replace placeholders
                String processedCommand = command
                    .replace("%player%", player.getName().getString())
                    .replace("%uuid%", player.getUUID().toString());
                
                // Execute command as server
                player.getServer().getCommands().performPrefixedCommand(
                    player.getServer().createCommandSourceStack(), processedCommand);
                
                DebugUtil.debugLog("Executed kit command: " + processedCommand);
            }
        } catch (Exception e) {
            DebugUtil.warnLog("Failed to execute kit commands for kit " + kit.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Give first join kit if enabled
     */
    public void giveFirstJoinKit(ServerPlayer player) {
        MainConfig.KitSettings kitSettings = configManager.getMainConfig().kitSettings;
        if (!kitSettings.giveKitOnFirstJoin || kitSettings.firstJoinKit.isEmpty()) {
            return;
        }
        
        Object received = playerDataManager.getSetting(player.getUUID(), "received_first_join_kit");
        if (Boolean.TRUE.equals(received) || "true".equals(String.valueOf(received))) {
            return;
        }
        
        if (giveKit(player, kitSettings.firstJoinKit)) {
            playerDataManager.setSetting(player.getUUID(), "received_first_join_kit", true);
            String message = LanguageManager.getInstance().getMessage(player, "neoessentials.kit.first_join", kitSettings.firstJoinKit);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
        }
    }
    
    /**
     * Clean up expired cooldowns
     */
    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        
        kitCooldowns.entrySet().removeIf(playerEntry -> {
            Map<String, Long> playerCooldowns = playerEntry.getValue();
            playerCooldowns.entrySet().removeIf(kitEntry -> 
                currentTime > kitEntry.getValue()
            );
            return playerCooldowns.isEmpty();
        });
    }
    
    /**
     * Create a new kit
     */
    public boolean createKit(String name, String displayName, String description, String createdBy) {
        if (kitStorageManager.kitExists(name)) {
            return false;
        }
        
        Kit kit = new Kit(name, displayName, description);
        kit.setCreatedBy(createdBy);
        
        return kitStorageManager.saveKit(kit);
    }
    
    /**
     * Update an existing kit
     */
    public boolean updateKit(Kit kit) {
        return kitStorageManager.saveKit(kit);
    }
    
    /**
     * Delete a kit
     */
    public boolean deleteKit(String name) {
        return kitStorageManager.deleteKit(name);
    }
    
    /**
     * Get a specific kit
     */
    public Kit getKit(String name) {
        return kitStorageManager.getKit(name);
    }
    
    /**
     * Reload all kits from storage
     */
    public void reloadKits() {
        kitStorageManager.reloadKits();
        DebugUtil.debugLog("Reloaded all kits from storage");
    }
    
    /**
     * Save all kits to storage
     */
    public boolean saveAllKits() {
        return kitStorageManager.forceSave();
    }
    
    /**
     * Get kit storage manager for direct access
     */
    public KitStorageManager getKitStorageManager() {
        return kitStorageManager;
    }
    
    /**
     * Format time for display
     */
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }
        
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m " + (seconds % 60) + "s";
        }
        
        long hours = minutes / 60;
        return hours + "h " + (minutes % 60) + "m";
    }
}
