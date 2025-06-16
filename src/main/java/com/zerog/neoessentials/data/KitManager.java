package com.zerog.neoessentials.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
<<<<<<< HEAD
=======
import com.google.gson.reflect.TypeToken;
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
import com.zerog.neoessentials.NeoEssentials;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
<<<<<<< HEAD
=======
import java.lang.reflect.Type;
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages kits for the NeoEssentials mod.
 * Players can claim kits which contain predefined items.
 * Kits can have cooldowns and permissions.
 */
public class KitManager {
    private static final String KIT_DATA_FILE = "neoessentials/kits.json";
    
    private final Map<String, Kit> kits = new HashMap<>();
<<<<<<< HEAD
    
    // Kit usage statistics
    private final Map<String, Integer> kitUsage = new HashMap<>();
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    
    /**
     * Creates a new KitManager
     */
    public KitManager() {
    }
    
    /**
     * Initializes the kit manager
     */
    public void initialize() {
        NeoEssentials.LOGGER.info("Initializing NeoEssentials Kit Manager");
        loadKits();
    }
    
    /**
     * Loads kits from the data file
     */
    private void loadKits() {
        try {
            File kitFile = new File(KIT_DATA_FILE);
            
            if (kitFile.exists()) {
                JsonObject kitsData = null;
                
                try (FileReader reader = new FileReader(kitFile)) {
                    kitsData = gson.fromJson(reader, JsonObject.class);
                }
                
                if (kitsData != null) {
                    // Load kits
                    if (kitsData.has("kits")) {
                        JsonObject kitsObj = kitsData.getAsJsonObject("kits");
                        for (Map.Entry<String, JsonElement> entry : kitsObj.entrySet()) {
                            String kitName = entry.getKey();
                            JsonObject kitObj = entry.getValue().getAsJsonObject();
                            
                            // Create the kit
                            Kit kit = new Kit(kitName);
<<<<<<< HEAD
                              // Set cooldown
=======
                            
                            // Set cooldown
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
                            if (kitObj.has("cooldown")) {
                                kit.setCooldown(kitObj.get("cooldown").getAsLong());
                            }
                            
                            // Set permission
                            if (kitObj.has("permission")) {
                                kit.setPermission(kitObj.get("permission").getAsString());
                            }
<<<<<<< HEAD
                              // Set price
                            if (kitObj.has("price")) {
                                kit.setPrice(kitObj.get("price").getAsDouble());
                            }
=======
                            
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
                            // Load items (store item IDs and counts)
                            if (kitObj.has("items")) {
                                JsonArray itemsArray = kitObj.getAsJsonArray("items");
                                for (JsonElement itemElement : itemsArray) {
                                    JsonObject itemObj = itemElement.getAsJsonObject();
                                    
                                    String itemId = itemObj.get("id").getAsString();
                                    int count = itemObj.get("count").getAsInt();
                                    
                                    // Add a placeholder item definition
                                    kit.addItemDefinition(itemId, count);
                                }
                            }
                            
                            // Add the kit
                            kits.put(kitName.toLowerCase(), kit);
                        }
                    }
                    
                    // Load cooldowns
                    if (kitsData.has("cooldowns")) {
                        JsonObject cooldownsObj = kitsData.getAsJsonObject("cooldowns");
                        
                        for (Map.Entry<String, JsonElement> entry : cooldownsObj.entrySet()) {
                            String uuidString = entry.getKey();
                            UUID uuid = UUID.fromString(uuidString);
                            JsonObject playerCooldowns = entry.getValue().getAsJsonObject();
                            
                            // Create cooldown map for player
                            Map<String, Long> playerCooldownMap = new HashMap<>();
                            
                            for (Map.Entry<String, JsonElement> cooldownEntry : playerCooldowns.entrySet()) {
                                String kitName = cooldownEntry.getKey();
                                long lastUse = cooldownEntry.getValue().getAsLong();
                                playerCooldownMap.put(kitName.toLowerCase(), lastUse);
                            }
                            
                            cooldowns.put(uuid, playerCooldownMap);
                        }
                    }
                    
                    NeoEssentials.LOGGER.info("Loaded {} kits from data file", kits.size());
                }
            } else {
                NeoEssentials.LOGGER.info("No existing kits data found, starting fresh");
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Error loading kit data", e);
        }
    }
    
    /**
     * Saves all kits to the data file
     */
    public void saveKits() {
        try {
            File kitFile = new File(KIT_DATA_FILE);
            
            // Create parent directories if they don't exist
            File parentDir = kitFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                NeoEssentials.LOGGER.error("Failed to create directory for kit data: {}", parentDir);
                return;
            }
            
            JsonObject kitsData = new JsonObject();
            
            // Save kits
            JsonObject kitsObj = new JsonObject();
            for (Map.Entry<String, Kit> entry : kits.entrySet()) {
                Kit kit = entry.getValue();
                JsonObject kitObj = new JsonObject();
<<<<<<< HEAD
                  // Save cooldown
=======
                
                // Save cooldown
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
                kitObj.addProperty("cooldown", kit.getCooldown());
                
                // Save permission
                if (kit.getPermission() != null) {
                    kitObj.addProperty("permission", kit.getPermission());
                }
<<<<<<< HEAD
                  // Save price
                kitObj.addProperty("price", kit.getPrice());
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
                
                // Save items
                JsonArray itemsArray = new JsonArray();
                for (ItemDefinition itemDef : kit.getItemDefinitions()) {
                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("id", itemDef.getItemId());
                    itemObj.addProperty("count", itemDef.getCount());
                    
                    itemsArray.add(itemObj);
                }
                kitObj.add("items", itemsArray);
                
                kitsObj.add(entry.getKey(), kitObj);
            }
            kitsData.add("kits", kitsObj);
            
            // Save cooldowns
            JsonObject cooldownsObj = new JsonObject();
            for (Map.Entry<UUID, Map<String, Long>> entry : cooldowns.entrySet()) {
                UUID uuid = entry.getKey();
                Map<String, Long> playerCooldowns = entry.getValue();
                
                JsonObject playerCooldownsObj = new JsonObject();
                for (Map.Entry<String, Long> cooldownEntry : playerCooldowns.entrySet()) {
                    playerCooldownsObj.addProperty(cooldownEntry.getKey(), cooldownEntry.getValue());
                }
                
                cooldownsObj.add(uuid.toString(), playerCooldownsObj);
            }
            kitsData.add("cooldowns", cooldownsObj);
            
            // Save to file
            try (FileWriter writer = new FileWriter(kitFile)) {
                gson.toJson(kitsData, writer);
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Error saving kit data", e);
        }
    }
    
    /**
     * Gets a kit by name
     * 
     * @param name The name of the kit
     * @return The kit, or null if not found
     */
    public Kit getKit(String name) {
        return kits.get(name != null ? name.toLowerCase() : null);
    }
    
    /**
     * Gets all kits
     * 
     * @return A map of kit names to kits
     */
    public Map<String, Kit> getAllKits() {
        return new HashMap<>(kits);
    }
<<<<<<< HEAD
      /**
=======
    
    /**
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
     * Creates a new kit
     * 
     * @param name The name of the kit
     * @param cooldown The cooldown in seconds
     * @param permission The permission needed to use the kit
     * @param items The items in the kit
     * @return The created kit
     */
    public Kit createKit(String name, long cooldown, String permission, List<ItemStack> items) {
<<<<<<< HEAD
        return createKit(name, cooldown, permission, 0, items);
    }
    
    /**
     * Creates a new kit with a price
     * 
     * @param name The name of the kit
     * @param cooldown The cooldown in seconds
     * @param permission The permission needed to use the kit
     * @param price The price to purchase the kit
     * @param items The items in the kit
     * @return The created kit
     */
    public Kit createKit(String name, long cooldown, String permission, double price, List<ItemStack> items) {
        Kit kit = new Kit(name);
        kit.setCooldown(cooldown);
        kit.setPermission(permission);
        kit.setPrice(price);
=======
        Kit kit = new Kit(name);
        kit.setCooldown(cooldown);
        kit.setPermission(permission);
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
        
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                String itemId = BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
                kit.addItemDefinition(itemId, item.getCount());
            }
        }
        
        kits.put(name.toLowerCase(), kit);
        
        // Save the kits
        saveKits();
        
        return kit;
    }
    
    /**
     * Deletes a kit
     * 
     * @param name The name of the kit
     * @return True if the kit was deleted, false if not found
<<<<<<< HEAD
     */    public boolean deleteKit(String name) {
        if (name == null) {
            return false;
        }
        
        String kitName = name.toLowerCase();
        Kit kit = kits.remove(kitName);
=======
     */
    public boolean deleteKit(String name) {
        Kit kit = kits.remove(name != null ? name.toLowerCase() : null);
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
        
        if (kit != null) {
            // Remove cooldowns for this kit
            for (Map<String, Long> playerCooldowns : cooldowns.values()) {
<<<<<<< HEAD
                playerCooldowns.remove(kitName);
=======
                playerCooldowns.remove(name.toLowerCase());
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
            }
            
            // Save the kits
            saveKits();
            
            return true;
        }
        
        return false;
    }
<<<<<<< HEAD
      /**
     * Checks if a player can use a kit (without considering price)
     * 
     * @param player The player
     * @param kitName The name of the kit
     * @return True if the player can use the kit, false otherwise
     */
    public boolean canUseKit(ServerPlayer player, String kitName) {
        return canUseKit(player, kitName, false);
    }
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
    
    /**
     * Checks if a player can use a kit
     * 
     * @param player The player
     * @param kitName The name of the kit
<<<<<<< HEAD
     * @param checkPrice Whether to check if the player has enough money for the kit
     * @return True if the player can use the kit, false otherwise
     */
    public boolean canUseKit(ServerPlayer player, String kitName, boolean checkPrice) {
=======
     * @return True if the player can use the kit, false otherwise
     */
    public boolean canUseKit(ServerPlayer player, String kitName) {
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
        Kit kit = getKit(kitName);
        
        if (kit == null || player == null) {
            return false;
        }
        
        // Check permission
        String permission = kit.getPermission();
        if (permission != null && !permission.isEmpty()) {
<<<<<<< HEAD
            // If specific kit permission defined, check if player has it
            if (!com.zerog.neoessentials.utils.PermissionUtil.hasPermission((ServerPlayer)player, permission)) {
                // Check if player has bypass permission (e.g., admin level permission)
                if (!com.zerog.neoessentials.utils.PermissionUtil.hasPermission((ServerPlayer)player, "neoessentials.kit.admin")) {
                    return false;
                }
            }
        } else {
            // Default permission check if no specific permission is set for the kit
            if (!com.zerog.neoessentials.utils.PermissionUtil.hasPermission((ServerPlayer)player, "neoessentials.command.kit")) {
=======
            // TODO: Check permission when we implement LuckPerms and FTB Ranks integration
            // For now, check if player is op
            if (!player.hasPermissions(2)) {
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
                return false;
            }
        }
        
        // Check cooldown
        UUID playerUuid = player.getUUID();
        Map<String, Long> playerCooldowns = cooldowns.getOrDefault(playerUuid, new HashMap<>());
        
        if (playerCooldowns.containsKey(kitName.toLowerCase())) {
            long lastUse = playerCooldowns.get(kitName.toLowerCase());
            long cooldownMs = kit.getCooldown() * 1000; // Convert to milliseconds
            
            // Check if cooldown has expired
            if (System.currentTimeMillis() - lastUse < cooldownMs) {
                return false;
            }
        }
        
<<<<<<< HEAD
        // Check if player has enough money if kit has a price and checking price
        if (checkPrice && kit.getPrice() > 0) {
            var economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
            if (economyManager != null && economyManager.getBalance(player.getUUID()) < kit.getPrice()) {
                return false;
            }
        }
        
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
        return true;
    }
    
    /**
     * Gets the remaining cooldown time for a kit
     * 
     * @param player The player
     * @param kitName The name of the kit
     * @return The remaining cooldown time in seconds, or 0 if no cooldown
     */
    public long getRemainingCooldown(ServerPlayer player, String kitName) {
        Kit kit = getKit(kitName);
        
        if (kit == null || player == null) {
            return 0;
        }
        
        UUID playerUuid = player.getUUID();
        Map<String, Long> playerCooldowns = cooldowns.getOrDefault(playerUuid, new HashMap<>());
        
        if (playerCooldowns.containsKey(kitName.toLowerCase())) {
            long lastUse = playerCooldowns.get(kitName.toLowerCase());
            long cooldownMs = kit.getCooldown() * 1000; // Convert to milliseconds
            
            long elapsed = System.currentTimeMillis() - lastUse;
            if (elapsed < cooldownMs) {
                return (cooldownMs - elapsed) / 1000; // Convert back to seconds
            }
        }
        
        return 0;
    }
<<<<<<< HEAD
      /**
=======
    
    /**
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
     * Gives a kit to a player
     * 
     * @param player The player
     * @param kitName The name of the kit
     * @return True if the kit was given, false otherwise
     */
    public boolean giveKit(ServerPlayer player, String kitName) {
        Kit kit = getKit(kitName);
        
        if (kit == null || player == null) {
            return false;
        }
        
<<<<<<< HEAD
        // Check if the player can use the kit (including price check)
        if (!canUseKit(player, kitName, true)) {
            return false;
        }
        
        // Handle payment if kit has a price
        if (kit.getPrice() > 0) {
            var economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
            if (economyManager != null) {
                // Check one more time if player has enough money
                if (economyManager.getBalance(player.getUUID()) < kit.getPrice()) {
                    return false;
                }
                
                // Charge the player for the kit
                boolean success = economyManager.removeBalance(player.getUUID(), kit.getPrice());
                if (!success) {
                    return false;
                }
                
                // Record the transaction with a specific description
                economyManager.recordTransaction(
                    player.getUUID(), 
                    EconomyTransaction.TYPE_WITHDRAW, 
                    kit.getPrice(), 
                    "Purchased kit: " + kit.getName()
                );
            }
        }
        
=======
        // Check if the player can use the kit
        if (!canUseKit(player, kitName)) {
            return false;
        }
        
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
        // Create and give items to the player
        for (ItemDefinition itemDef : kit.getItemDefinitions()) {
            try {
                // Try to get the item from its ID
                ResourceLocation resourceLocation = ResourceLocation.tryParse(itemDef.getItemId());
                Item item = BuiltInRegistries.ITEM.get(resourceLocation);
                
                if (item != null && item != Items.AIR) {
                    ItemStack itemStack = new ItemStack(item, itemDef.getCount());
                    
                    // Give item to player
                    if (!player.getInventory().add(itemStack)) {
                        // If inventory is full, drop the item
                        player.drop(itemStack, false);
                    }
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error giving item from kit: {}", e.getMessage());
            }
        }
        
        // Update cooldown
        UUID playerUuid = player.getUUID();
        Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(playerUuid, k -> new HashMap<>());
        playerCooldowns.put(kitName.toLowerCase(), System.currentTimeMillis());
        
        // Save cooldowns
        saveKits();
        
        return true;
    }
    
    /**
     * Represents a definition of an item in a kit
     */
    public static class ItemDefinition {
        private final String itemId;
        private final int count;
        
        public ItemDefinition(String itemId, int count) {
            this.itemId = itemId;
            this.count = Math.max(1, count);
        }
        
        public String getItemId() {
            return itemId;
        }
        
        public int getCount() {
            return count;
        }
    }
    
    /**
<<<<<<< HEAD
     * Kit class representing a set of items that players can claim
     */
    public static class Kit {
        private final String name;
        private long cooldown;
        private String permission;
        private double price = 0.0; // Add price field with default value of 0
=======
     * Represents a kit
     */
    public static class Kit {
        private final String name;
        private long cooldown = 0; // Cooldown in seconds
        private String permission = null;
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
        private final List<ItemDefinition> itemDefinitions = new ArrayList<>();
        
        public Kit(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        public long getCooldown() {
            return cooldown;
        }
        
        public void setCooldown(long cooldown) {
            this.cooldown = cooldown;
        }
        
        public String getPermission() {
            return permission;
        }
        
        public void setPermission(String permission) {
            this.permission = permission;
        }
        
<<<<<<< HEAD
        public double getPrice() {
            return price;
        }
        
        public void setPrice(double price) {
            this.price = Math.max(0, price);
=======
        public List<ItemDefinition> getItemDefinitions() {
            return new ArrayList<>(itemDefinitions); // Return a copy to prevent modification
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
        }
        
        public void addItemDefinition(String itemId, int count) {
            itemDefinitions.add(new ItemDefinition(itemId, count));
        }
<<<<<<< HEAD
          public List<ItemDefinition> getItemDefinitions() {
            return new ArrayList<>(itemDefinitions);
        }
        
        // Method to match the approach in AdminPanel
        public long cooldown() {
            return cooldown;
        }
        
        // Method to match the approach in AdminPanel
        public double price() {
            return price;        }
    }
    
    /**
     * Gets all kits
     * 
     * @return The map of kits
     */
    public Map<String, Kit> getKits() {
        return new HashMap<>(kits);
    }
    
    /**
     * Gets the usage count for a kit
     * 
     * @param kitName The name of the kit
     * @return The number of times the kit has been used
     */
    public int getKitUsageCount(String kitName) {
        return kitUsage.getOrDefault(kitName, 0);
    }
    
    /**
     * Gets all kit usage statistics
     * 
     * @return The map of kit usage
     */
    public Map<String, Integer> getKitUsageStats() {
        return new HashMap<>(kitUsage);
    }
    
    /**
     * Increments the usage count for a kit
     * 
     * @param kitName The name of the kit
     */
    public void incrementKitUsage(String kitName) {
        kitUsage.put(kitName, kitUsage.getOrDefault(kitName, 0) + 1);
=======
        
        public void clearItems() {
            itemDefinitions.clear();
        }
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
    }
}
