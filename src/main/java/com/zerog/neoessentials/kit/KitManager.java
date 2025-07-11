package com.zerog.neoessentials.kit;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.EconomyManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages kits, cooldowns, and kit distribution
 */
public class KitManager {
    
    private final Map<String, Kit> kits = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, LocalDateTime>> playerCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> playerUsedKits = new ConcurrentHashMap<>(); // For one-time use kits
    private final EconomyManager economyManager;
    
    public KitManager(EconomyManager economyManager) {
        this.economyManager = economyManager;
        initializeDefaultKits();
    }
    
    /**
     * Initialize default kits
     */
    private void initializeDefaultKits() {
        // Starter Kit
        Kit starterKit = new Kit.Builder("starter")
            .addItem(new ItemStack(net.minecraft.world.item.Items.WOODEN_SWORD))
            .addItem(new ItemStack(net.minecraft.world.item.Items.WOODEN_PICKAXE))
            .addItem(new ItemStack(net.minecraft.world.item.Items.WOODEN_AXE))
            .addItem(new ItemStack(net.minecraft.world.item.Items.WOODEN_SHOVEL))
            .addItem(new ItemStack(net.minecraft.world.item.Items.BREAD, 10))
            .addItem(new ItemStack(net.minecraft.world.item.Items.TORCH, 32))
            .setDescription("Essential starter items for new players")
            .setCategory(Kit.KitCategory.STARTER)
            .setCooldown(Duration.ofHours(24))
            .setOneTimeUse(true)
            .build();
        
        // Tools Kit
        Kit toolsKit = new Kit.Builder("tools")
            .addItem(new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE))
            .addItem(new ItemStack(net.minecraft.world.item.Items.IRON_AXE))
            .addItem(new ItemStack(net.minecraft.world.item.Items.IRON_SHOVEL))
            .addItem(new ItemStack(net.minecraft.world.item.Items.IRON_HOE))
            .addItem(new ItemStack(net.minecraft.world.item.Items.BUCKET))
            .addItem(new ItemStack(net.minecraft.world.item.Items.SHEARS))
            .setDescription("Basic iron tools for various activities")
            .setCategory(Kit.KitCategory.TOOLS)
            .setCooldown(Duration.ofHours(6))
            .setCost(50)
            .build();
        
        // Combat Kit
        Kit combatKit = new Kit.Builder("combat")
            .addItem(new ItemStack(net.minecraft.world.item.Items.IRON_SWORD))
            .addItem(new ItemStack(net.minecraft.world.item.Items.IRON_HELMET))
            .addItem(new ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE))
            .addItem(new ItemStack(net.minecraft.world.item.Items.IRON_LEGGINGS))
            .addItem(new ItemStack(net.minecraft.world.item.Items.IRON_BOOTS))
            .addItem(new ItemStack(net.minecraft.world.item.Items.SHIELD))
            .addItem(new ItemStack(net.minecraft.world.item.Items.BOW))
            .addItem(new ItemStack(net.minecraft.world.item.Items.ARROW, 64))
            .setDescription("Combat equipment and armor")
            .setCategory(Kit.KitCategory.COMBAT)
            .setCooldown(Duration.ofHours(12))
            .setCost(100)
            .build();
        
        // Building Kit
        Kit buildingKit = new Kit.Builder("building")
            .addItem(new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS, 64))
            .addItem(new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 64))
            .addItem(new ItemStack(net.minecraft.world.item.Items.GLASS, 32))
            .addItem(new ItemStack(net.minecraft.world.item.Items.OAK_DOOR, 4))
            .addItem(new ItemStack(net.minecraft.world.item.Items.LADDER, 16))
            .addItem(new ItemStack(net.minecraft.world.item.Items.CHEST, 4))
            .setDescription("Building materials and blocks")
            .setCategory(Kit.KitCategory.BUILDING)
            .setCooldown(Duration.ofHours(4))
            .setCost(30)
            .build();
        
        // Food Kit
        Kit foodKit = new Kit.Builder("food")
            .addItem(new ItemStack(net.minecraft.world.item.Items.BREAD, 16))
            .addItem(new ItemStack(net.minecraft.world.item.Items.COOKED_BEEF, 8))
            .addItem(new ItemStack(net.minecraft.world.item.Items.APPLE, 8))
            .addItem(new ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE, 2))
            .addItem(new ItemStack(net.minecraft.world.item.Items.CAKE))
            .setDescription("Food and sustenance items")
            .setCategory(Kit.KitCategory.FOOD)
            .setCooldown(Duration.ofHours(2))
            .setCost(25)
            .build();
        
        // Farming Kit
        Kit farmingKit = new Kit.Builder("farming")
            .addItem(new ItemStack(net.minecraft.world.item.Items.DIAMOND_HOE))
            .addItem(new ItemStack(net.minecraft.world.item.Items.WHEAT_SEEDS, 64))
            .addItem(new ItemStack(net.minecraft.world.item.Items.CARROT, 32))
            .addItem(new ItemStack(net.minecraft.world.item.Items.POTATO, 32))
            .addItem(new ItemStack(net.minecraft.world.item.Items.BONE_MEAL, 64))
            .addItem(new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET))
            .setDescription("Farming supplies and seeds")
            .setCategory(Kit.KitCategory.FARMING)
            .setCooldown(Duration.ofHours(8))
            .setCost(40)
            .build();
        
        // Mining Kit
        Kit miningKit = new Kit.Builder("mining")
            .addItem(new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE))
            .addItem(new ItemStack(net.minecraft.world.item.Items.TORCH, 64))
            .addItem(new ItemStack(net.minecraft.world.item.Items.LADDER, 32))
            .addItem(new ItemStack(net.minecraft.world.item.Items.BREAD, 16))
            .addItem(new ItemStack(net.minecraft.world.item.Items.CHEST, 2))
            .setDescription("Mining equipment and materials")
            .setCategory(Kit.KitCategory.MINING)
            .setCooldown(Duration.ofHours(6))
            .setCost(75)
            .build();
        
        // Add all kits to the manager
        addKit(starterKit);
        addKit(toolsKit);
        addKit(combatKit);
        addKit(buildingKit);
        addKit(foodKit);
        addKit(farmingKit);
        addKit(miningKit);
        
        NeoEssentials.LOGGER.info("Initialized {} default kits", kits.size());
    }
    
    /**
     * Adds a new kit to the manager
     */
    public boolean addKit(Kit kit) {
        if (kit == null || kit.getName() == null) return false;
        
        kits.put(kit.getName().toLowerCase(), kit);
        NeoEssentials.LOGGER.info("Added kit: {}", kit.getName());
        return true;
    }
    
    /**
     * Removes a kit from the manager
     */
    public boolean removeKit(String name) {
        Kit removed = kits.remove(name.toLowerCase());
        if (removed != null) {
            NeoEssentials.LOGGER.info("Removed kit: {}", name);
            return true;
        }
        return false;
    }
    
    /**
     * Gets a kit by name
     */
    public Optional<Kit> getKit(String name) {
        return Optional.ofNullable(kits.get(name.toLowerCase()));
    }
    
    /**
     * Gets all kits
     */
    public Collection<Kit> getAllKits() {
        return new ArrayList<>(kits.values());
    }
    
    /**
     * Gets kits by category
     */
    public List<Kit> getKitsByCategory(Kit.KitCategory category) {
        return kits.values().stream()
            .filter(kit -> kit.getCategory() == category)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets kits that a player can use (has permission and not on cooldown)
     */
    public List<Kit> getAvailableKits(ServerPlayer player) {
        return kits.values().stream()
            .filter(kit -> canUseKit(player, kit.getName()))
            .collect(Collectors.toList());
    }
    
    /**
     * Gives a kit to a player
     */
    public KitResult giveKit(ServerPlayer player, String kitName) {
        Optional<Kit> kitOpt = getKit(kitName);
        if (!kitOpt.isPresent()) {
            return new KitResult(false, "Kit not found: " + kitName);
        }
        
        Kit kit = kitOpt.get();
        
        // Check if kit is enabled
        if (!kit.isEnabled()) {
            return new KitResult(false, "Kit is currently disabled");
        }
        
        // Check permissions
        if (!kit.hasPermission(player)) {
            return new KitResult(false, "You don't have permission to use this kit");
        }
        
        // Check if one-time use and already used
        if (kit.isOneTimeUse() && hasUsedKit(player.getUUID(), kitName)) {
            return new KitResult(false, "You have already used this one-time kit");
        }
        
        // Check cooldown
        if (isOnCooldown(player.getUUID(), kitName)) {
            Duration remaining = getRemainingCooldown(player.getUUID(), kitName);
            return new KitResult(false, "Kit is on cooldown. Time remaining: " + formatDuration(remaining));
        }
        
        // Check cost
        if (kit.getCost() > 0 && economyManager.isEnabled()) {
            var account = economyManager.getOrCreateAccount(player.getUUID(), player.getName().getString());
            var currency = economyManager.getDefaultCurrency();
            if (account == null || !account.hasBalance(currency, java.math.BigDecimal.valueOf(kit.getCost()))) {
                return new KitResult(false, "Insufficient funds. Required: $" + kit.getCost());
            }
        }
        
        // Check inventory space
        if (!hasInventorySpace(player, kit.getItems())) {
            return new KitResult(false, "Insufficient inventory space");
        }
        
        // Deduct cost
        if (kit.getCost() > 0 && economyManager.isEnabled()) {
            var currency = economyManager.getDefaultCurrency();
            if (!economyManager.subtractMoney(player.getUUID(), java.math.BigDecimal.valueOf(kit.getCost()), currency, "Kit: " + kitName)) {
                return new KitResult(false, "Failed to process payment");
            }
        }
        
        // Give items
        for (ItemStack item : kit.getItems()) {
            player.getInventory().add(item.copy());
        }
        
        // Set cooldown
        setCooldown(player.getUUID(), kitName, kit.getCooldown());
        
        // Mark as used if one-time use
        if (kit.isOneTimeUse()) {
            markKitAsUsed(player.getUUID(), kitName);
        }
        
        return new KitResult(true, "Kit '" + kitName + "' given successfully!");
    }
    
    /**
     * Checks if a player can use a kit
     */
    public boolean canUseKit(ServerPlayer player, String kitName) {
        Optional<Kit> kitOpt = getKit(kitName);
        if (!kitOpt.isPresent()) return false;
        
        Kit kit = kitOpt.get();
        
        return kit.isEnabled() && 
               kit.hasPermission(player) && 
               !isOnCooldown(player.getUUID(), kitName) && 
               (!kit.isOneTimeUse() || !hasUsedKit(player.getUUID(), kitName));
    }
    
    /**
     * Checks if a player has inventory space for kit items
     */
    private boolean hasInventorySpace(ServerPlayer player, List<ItemStack> items) {
        int requiredSlots = 0;
        
        for (ItemStack item : items) {
            // Simplified check - in reality, you'd want to check for stackable items
            requiredSlots += Math.ceil((double) item.getCount() / item.getMaxStackSize());
        }
        
        int availableSlots = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).isEmpty()) {
                availableSlots++;
            }
        }
        
        return availableSlots >= requiredSlots;
    }
    
    /**
     * Sets cooldown for a player and kit
     */
    private void setCooldown(UUID playerId, String kitName, Duration cooldown) {
        if (cooldown.equals(Duration.ZERO)) return;
        
        playerCooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .put(kitName.toLowerCase(), LocalDateTime.now().plus(cooldown));
    }
    
    /**
     * Checks if a player is on cooldown for a kit
     */
    public boolean isOnCooldown(UUID playerId, String kitName) {
        Map<String, LocalDateTime> cooldowns = playerCooldowns.get(playerId);
        if (cooldowns == null) return false;
        
        LocalDateTime cooldownEnd = cooldowns.get(kitName.toLowerCase());
        if (cooldownEnd == null) return false;
        
        return LocalDateTime.now().isBefore(cooldownEnd);
    }
    
    /**
     * Gets remaining cooldown time
     */
    public Duration getRemainingCooldown(UUID playerId, String kitName) {
        Map<String, LocalDateTime> cooldowns = playerCooldowns.get(playerId);
        if (cooldowns == null) return Duration.ZERO;
        
        LocalDateTime cooldownEnd = cooldowns.get(kitName.toLowerCase());
        if (cooldownEnd == null) return Duration.ZERO;
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(cooldownEnd)) {
            cooldowns.remove(kitName.toLowerCase());
            return Duration.ZERO;
        }
        
        return Duration.between(now, cooldownEnd);
    }
    
    /**
     * Resets cooldown for a player and kit
     */
    public void resetCooldown(UUID playerId, String kitName) {
        Map<String, LocalDateTime> cooldowns = playerCooldowns.get(playerId);
        if (cooldowns != null) {
            cooldowns.remove(kitName.toLowerCase());
        }
    }
    
    /**
     * Marks a kit as used (for one-time use kits)
     */
    private void markKitAsUsed(UUID playerId, String kitName) {
        playerUsedKits.computeIfAbsent(playerId, k -> new HashSet<>())
            .add(kitName.toLowerCase());
    }
    
    /**
     * Checks if a player has used a one-time kit
     */
    public boolean hasUsedKit(UUID playerId, String kitName) {
        Set<String> usedKits = playerUsedKits.get(playerId);
        if (usedKits == null) return false;
        
        return usedKits.contains(kitName.toLowerCase());
    }
    
    /**
     * Resets one-time use status for a player and kit
     */
    public void resetKitUsage(UUID playerId, String kitName) {
        Set<String> usedKits = playerUsedKits.get(playerId);
        if (usedKits != null) {
            usedKits.remove(kitName.toLowerCase());
        }
    }
    
    /**
     * Formats duration for display
     */
    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        
        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
    
    /**
     * Gets kit statistics for a player
     */
    public Map<String, Object> getPlayerKitStats(UUID playerId) {
        Map<String, Object> stats = new HashMap<>();
        
        int totalKits = kits.size();
        int availableKits = 0;
        int onCooldown = 0;
        int used = 0;
        
        for (Kit kit : kits.values()) {
            if (isOnCooldown(playerId, kit.getName())) {
                onCooldown++;
            } else if (kit.isOneTimeUse() && hasUsedKit(playerId, kit.getName())) {
                used++;
            } else {
                availableKits++;
            }
        }
        
        stats.put("total", totalKits);
        stats.put("available", availableKits);
        stats.put("cooldown", onCooldown);
        stats.put("used", used);
        
        return stats;
    }
    
    /**
     * Result class for kit operations
     */
    public static class KitResult {
        private final boolean success;
        private final String message;
        
        public KitResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
