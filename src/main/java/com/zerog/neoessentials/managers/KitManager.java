package com.zerog.neoessentials.managers;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.config.KitConfig;
import com.zerog.neoessentials.storage.PlayerDataManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    private static final Logger LOGGER = LoggerFactory.getLogger(KitManager.class);
    private static KitManager instance;
    
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final Map<UUID, Map<String, Long>> kitCooldowns;
    
    private KitManager() {
        this.configManager = ConfigManager.getInstance();
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
        KitConfig config = configManager.getKitConfig();
        
        if (!config.enabled) {
            MessageUtil.sendMessage(player, "&cKit system is disabled.");
            return false;
        }
        
        // Get kit definition
        KitConfig.KitDefinition kit = config.getKit(kitName);
        if (kit == null) {
            MessageUtil.sendMessage(player, MessageUtil.replacePlaceholders(config.messages.kitNotFound, kitName));
            return false;
        }
        
        // Check permission
        if (!hasKitPermission(player, kit)) {
            MessageUtil.sendMessage(player, config.messages.kitNoPermission);
            return false;
        }
        
        // Check cooldown
        if (isOnCooldown(player, kitName)) {
            long remainingTime = getRemainingCooldown(player, kitName);
            MessageUtil.sendMessage(player, MessageUtil.replacePlaceholders(config.messages.kitCooldown,
                MessageUtil.formatTime(remainingTime)));
            return false;
        }
        
        // Check cost
        if (kit.hasCost()) {
            EconomyManager economyManager = EconomyManager.getInstance();
            if (!economyManager.hasBalance(player.getUUID(), kit.cost)) {
                MessageUtil.sendMessage(player, MessageUtil.replacePlaceholders(config.messages.kitCost,
                    economyManager.formatCurrency(kit.cost),
                    economyManager.formatCurrency(kit.cost.subtract(economyManager.getBalance(player.getUUID())))));
                return false;
            }
        }
        
        // Clear inventory if required
        if (kit.clearInventory) {
            player.getInventory().clearContent();
        }
        
        // Give kit items
        boolean success = giveKitItems(player, kit);
        if (!success) {
            MessageUtil.sendMessage(player, config.messages.inventoryFull);
            // Still continue and charge, as some items may have been given
        }
        
        // Charge cost
        if (kit.hasCost()) {
            EconomyManager economyManager = EconomyManager.getInstance();
            economyManager.withdrawBalance(player.getUUID(), kit.cost, "Kit: " + kitName);
        }
        
        // Set cooldown
        if (kit.hasDelay()) {
            setCooldown(player, kitName);
        }
        
        // Execute commands
        executeKitCommands(player, kit);
        
        MessageUtil.sendMessage(player, MessageUtil.replacePlaceholders(config.messages.kitGiven, kit.displayName));
        
        LOGGER.info("Player {} received kit '{}' with {} items", 
            player.getName().getString(), kitName, kit.items.size());
        
        return true;
    }
    
    /**
     * Get available kits for a player
     */
    public List<String> getAvailableKits(ServerPlayer player) {
        KitConfig config = configManager.getKitConfig();
        List<String> availableKits = new ArrayList<>();
        
        for (String kitName : config.getKitNames()) {
            KitConfig.KitDefinition kit = config.getKit(kitName);
            if (kit != null && hasKitPermission(player, kit)) {
                availableKits.add(kitName);
            }
        }
        
        return availableKits;
    }
    
    /**
     * Check if player has permission for kit
     */
    private boolean hasKitPermission(ServerPlayer player, KitConfig.KitDefinition kit) {
        if (kit.permission == null || kit.permission.isEmpty()) {
            return true;
        }
        return PermissionUtil.hasPermission(player, kit.permission);
    }
    
    /**
     * Check if player is on cooldown for kit
     */
    public boolean isOnCooldown(ServerPlayer player, String kitName) {
        KitConfig config = configManager.getKitConfig();
        if (!config.enableCooldowns) {
            return false;
        }
        
        // Check if player has cooldown bypass
        if (PermissionUtil.hasPermission(player, "essentials.kit.cooldown.bypass")) {
            return false;
        }
        
        Map<String, Long> playerCooldowns = kitCooldowns.get(player.getUUID());
        if (playerCooldowns == null) {
            return false;
        }
        
        Long lastUsed = playerCooldowns.get(kitName.toLowerCase());
        if (lastUsed == null) {
            return false;
        }
        
        KitConfig.KitDefinition kit = config.getKit(kitName);
        if (kit == null || !kit.hasDelay()) {
            return false;
        }
        
        long cooldownTime = kit.delay * 1000L;
        return System.currentTimeMillis() - lastUsed < cooldownTime;
    }
    
    /**
     * Get remaining cooldown time in milliseconds
     */
    public long getRemainingCooldown(ServerPlayer player, String kitName) {
        KitConfig config = configManager.getKitConfig();
        Map<String, Long> playerCooldowns = kitCooldowns.get(player.getUUID());
        if (playerCooldowns == null) {
            return 0;
        }
        
        Long lastUsed = playerCooldowns.get(kitName.toLowerCase());
        if (lastUsed == null) {
            return 0;
        }
        
        KitConfig.KitDefinition kit = config.getKit(kitName);
        if (kit == null || !kit.hasDelay()) {
            return 0;
        }
        
        long cooldownTime = kit.delay * 1000L;
        long elapsed = System.currentTimeMillis() - lastUsed;
        return Math.max(0, cooldownTime - elapsed);
    }
    
    /**
     * Set cooldown for player and kit
     */
    private void setCooldown(ServerPlayer player, String kitName) {
        kitCooldowns.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>())
            .put(kitName.toLowerCase(), System.currentTimeMillis());
    }
    
    /**
     * Give kit items to player
     */
    private boolean giveKitItems(ServerPlayer player, KitConfig.KitDefinition kit) {
        Inventory inventory = player.getInventory();
        boolean allItemsGiven = true;
        
        for (String itemString : kit.items) {
            try {
                ItemStack itemStack = parseItemString(itemString);
                if (itemStack != null) {
                    // Try to add to inventory
                    if (!inventory.add(itemStack)) {
                        // Inventory full, drop the item
                        player.drop(itemStack, false);
                        allItemsGiven = false;
                    }
                    
                    // Auto-equip armor if enabled
                    if (kit.autoEquip && isArmorItem(itemStack)) {
                        equipArmor(player, itemStack);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to parse kit item '{}' for player {}", itemString, player.getName().getString(), e);
                allItemsGiven = false;
            }
        }
        
        return allItemsGiven;
    }
    
    /**
     * Parse item string in format "minecraft:item:amount"
     */
    private ItemStack parseItemString(String itemString) {
        String[] parts = itemString.split(":");
        if (parts.length < 2) {
            return null;
        }
        
        // Basic item parsing - would need enhancement for full item support
        String itemName = parts[0] + ":" + parts[1];
        int amount = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
        
        // Simple item mapping - would need full registry lookup
        ItemStack itemStack = switch (itemName) {
            case "minecraft:wooden_sword" -> new ItemStack(Items.WOODEN_SWORD, amount);
            case "minecraft:wooden_pickaxe" -> new ItemStack(Items.WOODEN_PICKAXE, amount);
            case "minecraft:wooden_axe" -> new ItemStack(Items.WOODEN_AXE, amount);
            case "minecraft:wooden_shovel" -> new ItemStack(Items.WOODEN_SHOVEL, amount);
            case "minecraft:bread" -> new ItemStack(Items.BREAD, amount);
            case "minecraft:apple" -> new ItemStack(Items.APPLE, amount);
            case "minecraft:iron_sword" -> new ItemStack(Items.IRON_SWORD, amount);
            case "minecraft:iron_pickaxe" -> new ItemStack(Items.IRON_PICKAXE, amount);
            case "minecraft:iron_axe" -> new ItemStack(Items.IRON_AXE, amount);
            case "minecraft:iron_shovel" -> new ItemStack(Items.IRON_SHOVEL, amount);
            case "minecraft:iron_hoe" -> new ItemStack(Items.IRON_HOE, amount);
            case "minecraft:cooked_beef" -> new ItemStack(Items.COOKED_BEEF, amount);
            case "minecraft:golden_apple" -> new ItemStack(Items.GOLDEN_APPLE, amount);
            case "minecraft:cake" -> new ItemStack(Items.CAKE, amount);
            case "minecraft:diamond_sword" -> new ItemStack(Items.DIAMOND_SWORD, amount);
            case "minecraft:diamond_pickaxe" -> new ItemStack(Items.DIAMOND_PICKAXE, amount);
            case "minecraft:diamond_helmet" -> new ItemStack(Items.DIAMOND_HELMET, amount);
            case "minecraft:diamond_chestplate" -> new ItemStack(Items.DIAMOND_CHESTPLATE, amount);
            case "minecraft:diamond_leggings" -> new ItemStack(Items.DIAMOND_LEGGINGS, amount);
            case "minecraft:diamond_boots" -> new ItemStack(Items.DIAMOND_BOOTS, amount);
            default -> null;
        };
        
        return itemStack;
    }
    
    /**
     * Check if item is armor
     */
    private boolean isArmorItem(ItemStack itemStack) {
        return itemStack.getItem() == Items.DIAMOND_HELMET ||
               itemStack.getItem() == Items.DIAMOND_CHESTPLATE ||
               itemStack.getItem() == Items.DIAMOND_LEGGINGS ||
               itemStack.getItem() == Items.DIAMOND_BOOTS ||
               itemStack.getItem() == Items.IRON_HELMET ||
               itemStack.getItem() == Items.IRON_CHESTPLATE ||
               itemStack.getItem() == Items.IRON_LEGGINGS ||
               itemStack.getItem() == Items.IRON_BOOTS ||
               itemStack.getItem() == Items.LEATHER_HELMET ||
               itemStack.getItem() == Items.LEATHER_CHESTPLATE ||
               itemStack.getItem() == Items.LEATHER_LEGGINGS ||
               itemStack.getItem() == Items.LEATHER_BOOTS;
    }
    
    /**
     * Auto-equip armor piece
     */
    private void equipArmor(ServerPlayer player, ItemStack armorPiece) {
        Inventory inventory = player.getInventory();
        
        // Simple armor equipping logic
        if (armorPiece.getItem() == Items.DIAMOND_HELMET || armorPiece.getItem() == Items.IRON_HELMET || armorPiece.getItem() == Items.LEATHER_HELMET) {
            if (inventory.armor.get(3).isEmpty()) {
                inventory.armor.set(3, armorPiece.copy());
                armorPiece.setCount(0);
            }
        } else if (armorPiece.getItem() == Items.DIAMOND_CHESTPLATE || armorPiece.getItem() == Items.IRON_CHESTPLATE || armorPiece.getItem() == Items.LEATHER_CHESTPLATE) {
            if (inventory.armor.get(2).isEmpty()) {
                inventory.armor.set(2, armorPiece.copy());
                armorPiece.setCount(0);
            }
        } else if (armorPiece.getItem() == Items.DIAMOND_LEGGINGS || armorPiece.getItem() == Items.IRON_LEGGINGS || armorPiece.getItem() == Items.LEATHER_LEGGINGS) {
            if (inventory.armor.get(1).isEmpty()) {
                inventory.armor.set(1, armorPiece.copy());
                armorPiece.setCount(0);
            }
        } else if (armorPiece.getItem() == Items.DIAMOND_BOOTS || armorPiece.getItem() == Items.IRON_BOOTS || armorPiece.getItem() == Items.LEATHER_BOOTS) {
            if (inventory.armor.get(0).isEmpty()) {
                inventory.armor.set(0, armorPiece.copy());
                armorPiece.setCount(0);
            }
        }
    }
    
    /**
     * Execute kit commands
     */
    private void executeKitCommands(ServerPlayer player, KitConfig.KitDefinition kit) {
        for (String command : kit.commands) {
            try {
                // Replace placeholders
                String processedCommand = MessageUtil.replacePlaceholders(command, player.getName().getString());
                
                // Execute command (this would need proper command execution)
                LOGGER.debug("Would execute command: {}", processedCommand);
            } catch (Exception e) {
                LOGGER.warn("Failed to execute kit command '{}' for player {}", command, player.getName().getString(), e);
            }
        }
    }
    
    /**
     * Give first join kit if enabled
     */
    public void giveFirstJoinKit(ServerPlayer player) {
        KitConfig config = configManager.getKitConfig();
        
        if (!config.giveKitOnFirstJoin || config.firstJoinKit.isEmpty()) {
            return;
        }
        
        // Check if player has received first join kit before
        Object receivedSetting = playerDataManager.getSetting(player.getUUID(), "received_first_join_kit");
        if (Boolean.TRUE.equals(receivedSetting) || "true".equals(String.valueOf(receivedSetting))) {
            return;
        }
        
        // Give the kit
        if (giveKit(player, config.firstJoinKit)) {
            playerDataManager.setSetting(player.getUUID(), "received_first_join_kit", true);
            MessageUtil.sendMessage(player, config.messages.firstJoinKit);
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
