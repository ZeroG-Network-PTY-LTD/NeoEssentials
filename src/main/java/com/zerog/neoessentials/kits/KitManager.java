package com.zerog.neoessentials.kits;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages all kit operations including creation, deletion, usage tracking, and cooldowns.
 * Thread-safe for concurrent access from multiple players.
 */
public class KitManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(KitManager.class);
    private static final KitManager INSTANCE = new KitManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final Map<String, Kit> kits = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> playerCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Integer>> playerUsages = new ConcurrentHashMap<>();
    private final File playerDataFile = com.zerog.neoessentials.util.ResourceUtil.getDataFile("kit_player_data.json");
    private volatile boolean initialized = false;
    
    private KitManager() {}
    
    public static KitManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Initializes the kit manager by loading all kits from configuration.
     */
    public synchronized void initialize() {
        if (initialized) return;
        
        try {
            LOGGER.info("Initializing Kit Manager...");
            loadKits();
            loadPlayerData();
            initialized = true;
            LOGGER.info("Kit Manager initialized with {} kits", kits.size());
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Kit Manager: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Loads all kits from the configuration.
     */
    private void loadKits() {
        try {
            File kitsFile = com.zerog.neoessentials.util.ResourceUtil.getConfigFile("kits.json");
            
            if (kitsFile.exists()) {
                try (Reader reader = new FileReader(kitsFile)) {
                    JsonObject config = GSON.fromJson(reader, JsonObject.class);
                    
                    if (config != null && config.has("kits")) {
                        JsonArray kitsArray = config.getAsJsonArray("kits");
                        int loadedCount = 0;
                        
                        for (JsonElement element : kitsArray) {
                            try {
                                Kit kit = Kit.fromJson(element.getAsJsonObject());
                                kits.put(kit.getName(), kit);
                                
                                // Register kit permission with the permission registry for tab completion
                                try {
                                    com.zerog.neoessentials.api.permissions.PermissionRegistry.getInstance()
                                        .registerKitPermission(kit.getName());
                                } catch (Exception e) {
                                    LOGGER.warn("Failed to register kit permission for '{}': {}", kit.getName(), e.getMessage());
                                }
                                
                                loadedCount++;
                            } catch (Exception e) {
                                LOGGER.warn("Failed to load kit from config: {}", e.getMessage());
                            }
                        }
                        
                        LOGGER.info("Loaded {} kits from configuration", loadedCount);
                    }
                }
            } else {
                LOGGER.info("No kits configuration found, starting with empty kit list");
                // Create default config
                saveKits();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load kits from configuration: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Saves all kits to the configuration.
     */
    private void saveKits() {
        try {
            File kitsFile = com.zerog.neoessentials.util.ResourceUtil.getConfigFile("kits.json");
            
            // Ensure directory exists
            File parentDir = kitsFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            JsonObject config = new JsonObject();
            config.addProperty("_configVersion", 1);
            config.addProperty("_configVersion_comment", 
                "DO NOT MODIFY: This field is used by NeoEssentials for automatic config updates.");
            
            JsonArray kitsArray = new JsonArray();
            for (Kit kit : kits.values()) {
                kitsArray.add(kit.toJson());
            }
            config.add("kits", kitsArray);
            
            try (Writer writer = new FileWriter(kitsFile)) {
                GSON.toJson(config, writer);
            }
            LOGGER.debug("Saved {} kits to configuration", kits.size());
        } catch (Exception e) {
            LOGGER.error("Failed to save kits to configuration: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Loads player cooldown and usage data.
     */
    private void loadPlayerData() {
        try {
            if (!playerDataFile.exists()) {
                LOGGER.debug("No kit player data file found, starting fresh");
                return;
            }
            
            try (Reader reader = new FileReader(playerDataFile)) {
                JsonObject data = GSON.fromJson(reader, JsonObject.class);
                
                if (data != null) {
                // Load cooldowns
                if (data.has("cooldowns")) {
                    JsonObject cooldownsJson = data.getAsJsonObject("cooldowns");
                    for (Map.Entry<String, JsonElement> playerEntry : cooldownsJson.entrySet()) {
                        try {
                            UUID playerId = UUID.fromString(playerEntry.getKey());
                            JsonObject playerCooldowns = playerEntry.getValue().getAsJsonObject();
                            
                            Map<String, Long> cooldowns = new HashMap<>();
                            for (Map.Entry<String, JsonElement> kitEntry : playerCooldowns.entrySet()) {
                                cooldowns.put(kitEntry.getKey(), kitEntry.getValue().getAsLong());
                            }
                            this.playerCooldowns.put(playerId, cooldowns);
                        } catch (Exception e) {
                            LOGGER.warn("Failed to load cooldown data for player: {}", e.getMessage());
                        }
                    }
                }
                
                // Load usage counts
                if (data.has("usages")) {
                    JsonObject usagesJson = data.getAsJsonObject("usages");
                    for (Map.Entry<String, JsonElement> playerEntry : usagesJson.entrySet()) {
                        try {
                            UUID playerId = UUID.fromString(playerEntry.getKey());
                            JsonObject playerUsages = playerEntry.getValue().getAsJsonObject();
                            
                            Map<String, Integer> usages = new HashMap<>();
                            for (Map.Entry<String, JsonElement> kitEntry : playerUsages.entrySet()) {
                                usages.put(kitEntry.getKey(), kitEntry.getValue().getAsInt());
                            }
                            this.playerUsages.put(playerId, usages);
                        } catch (Exception e) {
                            LOGGER.warn("Failed to load usage data for player: {}", e.getMessage());
                        }
                    }
                }
                
                    LOGGER.debug("Loaded player data for {} players", 
                               Math.max(playerCooldowns.size(), playerUsages.size()));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load player kit data: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Saves player cooldown and usage data.
     */
    private void savePlayerData() {
        try {
            // Ensure directory exists
            File parentDir = playerDataFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            JsonObject data = new JsonObject();
            
            // Save cooldowns
            JsonObject cooldownsJson = new JsonObject();
            for (Map.Entry<UUID, Map<String, Long>> playerEntry : playerCooldowns.entrySet()) {
                JsonObject playerCooldowns = new JsonObject();
                for (Map.Entry<String, Long> kitEntry : playerEntry.getValue().entrySet()) {
                    playerCooldowns.addProperty(kitEntry.getKey(), kitEntry.getValue());
                }
                cooldownsJson.add(playerEntry.getKey().toString(), playerCooldowns);
            }
            data.add("cooldowns", cooldownsJson);
            
            // Save usage counts
            JsonObject usagesJson = new JsonObject();
            for (Map.Entry<UUID, Map<String, Integer>> playerEntry : playerUsages.entrySet()) {
                JsonObject playerUsages = new JsonObject();
                for (Map.Entry<String, Integer> kitEntry : playerEntry.getValue().entrySet()) {
                    playerUsages.addProperty(kitEntry.getKey(), kitEntry.getValue());
                }
                usagesJson.add(playerEntry.getKey().toString(), playerUsages);
            }
            data.add("usages", usagesJson);
            
            try (Writer writer = new FileWriter(playerDataFile)) {
                GSON.toJson(data, writer);
            }
            LOGGER.debug("Saved player kit data");
        } catch (Exception e) {
            LOGGER.error("Failed to save player kit data: {}", e.getMessage(), e);
        }
    }
    
    // Kit Management Methods
    
    /**
     * Creates a new kit or updates an existing one.
     */
    public boolean createKit(String name, String displayName, String description, 
                           List<ItemStack> items, long cooldownMillis, String permission) {
        try {
            Kit kit = new Kit(name, displayName, description, items, cooldownMillis, permission, -1, true);
            kits.put(kit.getName(), kit);
            saveKits();
            
            // Register kit permission with the permission registry for tab completion
            try {
                com.zerog.neoessentials.api.permissions.PermissionRegistry.getInstance()
                    .registerKitPermission(kit.getName());
            } catch (Exception e) {
                LOGGER.warn("Failed to register kit permission for '{}': {}", kit.getName(), e.getMessage());
            }
            
            LOGGER.info("Created/updated kit: {}", kit.getName());
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to create kit '{}': {}", name, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Deletes a kit.
     */
    public boolean deleteKit(String name) {
        String normalizedName = name.toLowerCase();
        if (kits.remove(normalizedName) != null) {
            saveKits();
            
            // Unregister kit permission from the permission registry
            try {
                com.zerog.neoessentials.api.permissions.PermissionRegistry.getInstance()
                    .unregisterKitPermission(normalizedName);
            } catch (Exception e) {
                LOGGER.warn("Failed to unregister kit permission for '{}': {}", normalizedName, e.getMessage());
            }
            
            LOGGER.info("Deleted kit: {}", normalizedName);
            return true;
        }
        return false;
    }
    
    /**
     * Gets a kit by name.
     */
    public Kit getKit(String name) {
        return kits.get(name.toLowerCase());
    }
    
    /**
     * Gets all registered kit names.
     */
    public Set<String> getKitNames() {
        return new HashSet<>(kits.keySet());
    }
    
    /**
     * Gets all registered kit names (alias for getKitNames).
     */
    public Set<String> getAllKitNames() {
        return getKitNames();
    }
    
    /**
     * Gets all available kits.
     */
    public Collection<Kit> getAllKits() {
        return new ArrayList<>(kits.values());
    }
    
    /**
     * Gets kits available to a specific player (considering permissions).
     */
    public List<Kit> getAvailableKits(ServerPlayer player) {
        return kits.values().stream()
                .filter(kit -> kit.isEnabled())
                .filter(kit -> kit.getPermission() == null || 
                              PermissionAPI.hasPermission(player.getUUID(), kit.getPermission()))
                .collect(Collectors.toList());
    }
    
    /**
     * Checks if a player can use a kit right now.
     */
    public KitUsageResult canUseKit(ServerPlayer player, String kitName) {
        // If allowKitOverride is enabled and player has override permission, skip all restrictions
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isAllowKitOverrideEnabled() &&
            com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.kits.override")) {
            return new KitUsageResult(true, "Kit can be used (override)");
        }
        
        Kit kit = getKit(kitName);
        if (kit == null) {
            return new KitUsageResult(false, "Kit not found");
        }
        
        if (!kit.isEnabled()) {
            return new KitUsageResult(false, "Kit is currently disabled");
        }
        
        // Check permission
        if (kit.getPermission() != null) {
            if (!PermissionAPI.hasPermission(player.getUUID(), kit.getPermission())) {
                return new KitUsageResult(false, "You don't have permission to use this kit");
            }
        }
        
        // Check cooldown (unless player has exemption)
        if (!hasCooldownExemption(player, kitName)) {
            long remainingCooldown = getRemainingCooldown(player.getUUID(), kitName);
            if (remainingCooldown > 0) {
                return new KitUsageResult(false, "Kit is still on cooldown for " + formatTime(remainingCooldown));
            }
        }

        // Check usage limit
        if (kit.getMaxUses() > 0) {
            int usageCount = getUsageCount(player.getUUID(), kitName);
            if (usageCount >= kit.getMaxUses()) {
                return new KitUsageResult(false, "You have reached the maximum uses for this kit");
            }
        }

        // Enforce maxKitsPerPlayer (active cooldowns)
        int maxKits = com.zerog.neoessentials.config.ConfigManager.getInstance().getMaxKitsPerPlayer();
        if (maxKits > 0 && !hasCooldownExemption(player, kitName)) {
            // Count number of kits with active cooldowns for this player
            int activeCooldowns = 0;
            Map<String, Long> cooldownMap = playerCooldowns.get(player.getUUID());
            if (cooldownMap != null) {
                long now = System.currentTimeMillis();
                for (Long cooldownEnd : cooldownMap.values()) {
                    if (cooldownEnd != null && cooldownEnd > now) {
                        activeCooldowns++;
                    }
                }
            }
            // If this kit is not already on cooldown, using it would add a new cooldown
            boolean alreadyOnCooldown = false;
            if (cooldownMap != null && cooldownMap.containsKey(kitName.toLowerCase())) {
                long cooldownEnd = cooldownMap.get(kitName.toLowerCase());
                if (cooldownEnd > System.currentTimeMillis()) {
                    alreadyOnCooldown = true;
                }
            }
            if (!alreadyOnCooldown && activeCooldowns >= maxKits) {
                return new KitUsageResult(false, "You have reached the maximum number of kits on cooldown (" + maxKits + ")");
            }
        }

        return new KitUsageResult(true, "Kit can be used");
    }
    
    /**
     * Gives a kit to a player.
     */
    public KitUsageResult giveKit(ServerPlayer player, String kitName) {
        KitUsageResult canUse = canUseKit(player, kitName);
        if (!canUse.isAllowed()) {
            return canUse;
        }
        // Double-check maxKitsPerPlayer after giving kit (in case of race conditions), unless override
        if (!(com.zerog.neoessentials.config.ConfigManager.getInstance().isAllowKitOverrideEnabled() &&
              com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.kits.override"))) {
            int maxKits = com.zerog.neoessentials.config.ConfigManager.getInstance().getMaxKitsPerPlayer();
            if (maxKits > 0 && !hasCooldownExemption(player, kitName)) {
                Map<String, Long> cooldownMap = playerCooldowns.get(player.getUUID());
                int activeCooldowns = 0;
                long now = System.currentTimeMillis();
                if (cooldownMap != null) {
                    for (Long cooldownEnd : cooldownMap.values()) {
                        if (cooldownEnd != null && cooldownEnd > now) {
                            activeCooldowns++;
                        }
                    }
                }
                // If this kit is not already on cooldown, using it would add a new cooldown
                boolean alreadyOnCooldown = false;
                if (cooldownMap != null && cooldownMap.containsKey(kitName.toLowerCase())) {
                    long cooldownEnd = cooldownMap.get(kitName.toLowerCase());
                    if (cooldownEnd > now) {
                        alreadyOnCooldown = true;
                    }
                }
                if (!alreadyOnCooldown && activeCooldowns >= maxKits) {
                    return new KitUsageResult(false, "You have reached the maximum number of kits on cooldown (" + maxKits + ")");
                }
            }
        }

        Kit kit = getKit(kitName);
        if (kit == null) {
            return new KitUsageResult(false, "Kit not found");
        }

        try {
            Inventory inventory = player.getInventory();
            List<ItemStack> itemsGiven = new ArrayList<>();
            List<ItemStack> itemsDropped = new ArrayList<>();
            List<String> deniedItems = new ArrayList<>();

            boolean autoEquip = com.zerog.neoessentials.config.ConfigManager.isKitAutoEquipEnabled();
            // Check if all armor slots are empty
            boolean armorSlotsEmpty = inventory.armor.stream().allMatch(ItemStack::isEmpty);

            // Separate armor and non-armor items
            List<ItemStack> armorItems = new ArrayList<>();
            List<ItemStack> otherItems = new ArrayList<>();
            for (ItemStack item : kit.getItems()) {
                if (item.isEmpty()) continue;
                if (item.getItem().getDescriptionId().contains("helmet") ||
                    item.getItem().getDescriptionId().contains("chestplate") ||
                    item.getItem().getDescriptionId().contains("leggings") ||
                    item.getItem().getDescriptionId().contains("boots")) {
                    armorItems.add(item);
                } else {
                    otherItems.add(item);
                }
            }

            // Give armor items (auto-equip if enabled and slots are empty)
            if (autoEquip && armorSlotsEmpty && !armorItems.isEmpty()) {
                for (ItemStack armor : armorItems) {
                    // Find first empty armor slot
                    boolean equipped = false;
                    for (int i = 0; i < inventory.armor.size(); i++) {
                        if (inventory.armor.get(i).isEmpty()) {
                            inventory.armor.set(i, armor.copy());
                            itemsGiven.add(armor.copy());
                            equipped = true;
                            break;
                        }
                    }
                    if (!equipped) {
                        // If no empty slot, add to inventory as fallback
                        if (inventory.add(armor.copy())) {
                            itemsGiven.add(armor.copy());
                        } else {
                            player.drop(armor.copy(), false);
                            itemsDropped.add(armor.copy());
                        }
                    }
                }
            } else {
                // Add armor items to inventory as normal
                for (ItemStack armor : armorItems) {
                    if (inventory.add(armor.copy())) {
                        itemsGiven.add(armor.copy());
                    } else {
                        player.drop(armor.copy(), false);
                        itemsDropped.add(armor.copy());
                    }
                }
            }

            // Give non-armor items
            for (ItemStack item : otherItems) {
                // Enforce permission-based item spawn for each item
                com.zerog.neoessentials.items.ItemSpawnHelper.SpawnResult spawnResult =
                    com.zerog.neoessentials.items.ItemSpawnHelper.canSpawnItem(player, item);
                if (!spawnResult.isSuccess()) {
                    deniedItems.add(item.getDisplayName().getString() + ": " + spawnResult.getErrorMessage());
                    LOGGER.warn("Denied item '{}' from kit '{}' for player '{}': {}", item.getDisplayName().getString(), kitName, player.getName().getString(), spawnResult.getErrorMessage());
                    continue;
                }
                if (inventory.add(item.copy())) {
                    itemsGiven.add(item.copy());
                } else {
                    player.drop(item.copy(), false);
                    itemsDropped.add(item.copy());
                }
            }

            // Update cooldown and usage tracking
            // Only set cooldown if player doesn't have exemption
            if (!hasCooldownExemption(player, kitName)) {
                setCooldown(player.getUUID(), kitName, System.currentTimeMillis() + kit.getCooldownMillis());
            }
            incrementUsage(player.getUUID(), kitName);

            savePlayerData();

            String result = String.format("Given kit '%s' (%d items)", kit.getDisplayName(), itemsGiven.size());
            if (!itemsDropped.isEmpty()) {
                result += String.format(" (%d items dropped)", itemsDropped.size());
            }
            if (!deniedItems.isEmpty()) {
                result += String.format(" (%d items denied: %s)", deniedItems.size(), String.join(", ", deniedItems));
            }

            if (com.zerog.neoessentials.config.ConfigManager.isLogKitUsageEnabled()) {
                LOGGER.info("Player {} used kit {}", player.getName().getString(), kitName);
            }
            return new KitUsageResult(true, result);

        } catch (Exception e) {
            LOGGER.error("Failed to give kit '{}' to player {}: {}", 
                        kitName, player.getName().getString(), e.getMessage(), e);
            return new KitUsageResult(false, "An error occurred while giving the kit");
        }
    }
    
    // Cooldown and Usage Tracking
    
    private long getRemainingCooldown(UUID playerId, String kitName) {
        Map<String, Long> playerCooldownMap = playerCooldowns.get(playerId);
        if (playerCooldownMap == null) return 0;
        
        Long cooldownEnd = playerCooldownMap.get(kitName.toLowerCase());
        if (cooldownEnd == null) return 0;
        
        long remaining = cooldownEnd - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    
    private void setCooldown(UUID playerId, String kitName, long cooldownEnd) {
        playerCooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                      .put(kitName.toLowerCase(), cooldownEnd);
    }
    
    public int getUsageCount(UUID playerId, String kitName) {
        Map<String, Integer> playerUsageMap = playerUsages.get(playerId);
        if (playerUsageMap == null) return 0;
        return playerUsageMap.getOrDefault(kitName.toLowerCase(), 0);
    }
    
    private void incrementUsage(UUID playerId, String kitName) {
        playerUsages.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                   .merge(kitName.toLowerCase(), 1, Integer::sum);
    }
    
    /**
     * Checks if a player has cooldown exemption for a kit.
     * Checks both global cooldown exemption and per-kit exemption.
     */
    private boolean hasCooldownExemption(ServerPlayer player, String kitName) {
        UUID playerId = player.getUUID();
        // Check override permission if allowKitOverride is enabled
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isAllowKitOverrideEnabled()) {
            if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(playerId, "neoessentials.kits.override")) {
                return true;
            }
        }
        // Check global cooldown exemption
        if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(playerId, "neoessentials.kits.nocooldown")) {
            return true;
        }
        
        // Check per-kit cooldown exemption
        String kitNocooldownPermission = "neoessentials.kits." + kitName.toLowerCase() + ".nocooldown";
        if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(playerId, kitNocooldownPermission)) {
            return true;
        }
        
        return false;
    }
    
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /**
     * Reloads all kit data from configuration.
     */
    public void reload() {
        kits.clear();
        playerCooldowns.clear();
        playerUsages.clear();
        initialized = false;
        initialize();
    }
    
    /**
     * Result of a kit usage attempt.
     */
    public static class KitUsageResult {
        private final boolean allowed;
        private final String message;
        
        public KitUsageResult(boolean allowed, String message) {
            this.allowed = allowed;
            this.message = message;
        }
        
        public boolean isAllowed() { return allowed; }
        public String getMessage() { return message; }
    }
}