package com.zerog.neoessentials.config;

import java.math.BigDecimal;
import java.util.*;

/**
 * Kit system configuration for NeoEssentials
 * Compatible with EssentialsX kit system
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class KitConfig {
    
    // Basic kit settings
    public boolean enabled = true;
    public boolean giveKitOnFirstJoin = true;
    public String firstJoinKit = "starter";
    
    // Kit delay and cooldown settings
    public boolean enableCooldowns = true;
    public boolean persistCooldowns = true; // Persist across server restarts
    
    // Kit cost settings
    public boolean enableKitCosts = true;
    
    // Available kits
    public Map<String, KitDefinition> kits = new HashMap<>();
    
    // Messages
    public MessagesConfig messages = new MessagesConfig();
    
    public KitConfig() {
        initializeDefaultKits();
    }
    
    private void initializeDefaultKits() {
        // Starter kit
        KitDefinition starter = new KitDefinition();
        starter.displayName = "&aStarter Kit";
        starter.description = Arrays.asList("&7Basic tools and food for new players");
        starter.delay = 0;
        starter.cost = BigDecimal.ZERO;
        starter.items = Arrays.asList(
            "minecraft:wooden_sword:1",
            "minecraft:wooden_pickaxe:1", 
            "minecraft:wooden_axe:1",
            "minecraft:wooden_shovel:1",
            "minecraft:bread:10",
            "minecraft:apple:5"
        );
        starter.permission = "essentials.kit.starter";
        kits.put("starter", starter);
        
        // Tools kit
        KitDefinition tools = new KitDefinition();
        tools.displayName = "&bTools Kit";
        tools.description = Arrays.asList("&7Basic iron tools for everyday use");
        tools.delay = 3600; // 1 hour
        tools.cost = new BigDecimal("100.00");
        tools.items = Arrays.asList(
            "minecraft:iron_sword:1",
            "minecraft:iron_pickaxe:1",
            "minecraft:iron_axe:1", 
            "minecraft:iron_shovel:1",
            "minecraft:iron_hoe:1"
        );
        tools.permission = "essentials.kit.tools";
        kits.put("tools", tools);
        
        // Food kit
        KitDefinition food = new KitDefinition();
        food.displayName = "&6Food Kit";
        food.description = Arrays.asList("&7Variety of food items");
        food.delay = 1800; // 30 minutes
        food.cost = new BigDecimal("25.00");
        food.items = Arrays.asList(
            "minecraft:cooked_beef:16",
            "minecraft:bread:10",
            "minecraft:golden_apple:2",
            "minecraft:cake:1"
        );
        food.permission = "essentials.kit.food";
        kits.put("food", food);
        
        // VIP kit
        KitDefinition vip = new KitDefinition();
        vip.displayName = "&dVIP Kit";
        vip.description = Arrays.asList("&7Special kit for VIP players", "&7Contains diamond tools and armor");
        vip.delay = 7200; // 2 hours
        vip.cost = BigDecimal.ZERO; // Free for VIPs
        vip.items = Arrays.asList(
            "minecraft:diamond_sword:1",
            "minecraft:diamond_pickaxe:1",
            "minecraft:diamond_helmet:1",
            "minecraft:diamond_chestplate:1",
            "minecraft:diamond_leggings:1",
            "minecraft:diamond_boots:1",
            "minecraft:golden_apple:5"
        );
        vip.permission = "essentials.kit.vip";
        kits.put("vip", vip);
    }
    
    public static class KitDefinition {
        public String displayName;
        public List<String> description = new ArrayList<>();
        public List<String> items = new ArrayList<>();
        public String permission;
        public int delay = 0; // Cooldown in seconds
        public BigDecimal cost = BigDecimal.ZERO;
        public boolean autoEquip = false; // Auto-equip armor pieces
        public boolean clearInventory = false; // Clear inventory before giving kit
        public List<String> commands = new ArrayList<>(); // Commands to run when kit is given
        
        public boolean hasPermission(String playerPermission) {
            return permission == null || permission.isEmpty() || playerPermission.equals(permission);
        }
        
        public boolean hasCost() {
            return cost.compareTo(BigDecimal.ZERO) > 0;
        }
        
        public boolean hasDelay() {
            return delay > 0;
        }
    }
    
    public static class MessagesConfig {
        public String kitGiven = "&aYou received the {KIT} kit!";
        public String kitNotFound = "&cKit '{KIT}' not found!";
        public String kitNoPermission = "&cYou don't have permission to use this kit!";
        public String kitCooldown = "&cYou must wait {TIME} before using this kit again!";
        public String kitCost = "&cThis kit costs {COST}! You need {NEEDED} more.";
        public String kitListHeader = "&6Available kits:";
        public String kitListEntry = "&7- &a{KIT} &7({DESCRIPTION}) &8[Delay: {DELAY}] [Cost: {COST}]";
        public String kitListEmpty = "&cNo kits available!";
        public String inventoryFull = "&cYour inventory is full! Some items were dropped.";
        public String firstJoinKit = "&aWelcome! You received the starter kit!";
    }
    
    /**
     * Get kit definition by name
     */
    public KitDefinition getKit(String name) {
        return kits.get(name.toLowerCase());
    }
    
    /**
     * Check if kit exists
     */
    public boolean hasKit(String name) {
        return kits.containsKey(name.toLowerCase());
    }
    
    /**
     * Get all available kit names
     */
    public Set<String> getKitNames() {
        return kits.keySet();
    }
    
    /**
     * Get kits available to a player based on permissions
     */
    public Map<String, KitDefinition> getAvailableKits(List<String> playerPermissions) {
        Map<String, KitDefinition> available = new HashMap<>();
        
        for (Map.Entry<String, KitDefinition> entry : kits.entrySet()) {
            KitDefinition kit = entry.getValue();
            if (kit.permission == null || kit.permission.isEmpty() || 
                playerPermissions.contains(kit.permission)) {
                available.put(entry.getKey(), kit);
            }
        }
        
        return available;
    }
}
