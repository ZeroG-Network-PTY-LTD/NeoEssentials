package com.zerog.neoessentials.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** * Manages kits for the NeoEssentials mod.
 * Players can claim kits which contain predefined items.
 * Kits can have cooldowns and permissions.
 */
public class KitManager_new {
    private static final String KIT_DATA_FILE = "neoessentials/kits.json";
    
    private final Map<String, Kit> kits = new HashMap<>();
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
                            
                            // Set cooldown
                            if (kitObj.has("cooldown")) {
                                kit.setCooldown(kitObj.get("cooldown").getAsLong());
                            }
                            
                            // Set permission
                            if (kitObj.has("permission")) {
                                kit.setPermission(kitObj.get("permission").getAsString());
                            }
                            
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
                
                // Save cooldown
                kitObj.addProperty("cooldown", kit.getCooldown());
                
                // Save permission
                if (kit.getPermission() != null) {
                    kitObj.addProperty("permission", kit.getPermission());
                }
                
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
    
    /**
     * Creates a new kit
     * 
     * @param name The name of the kit
     * @param cooldown The cooldown in seconds
     * @param permission The permission needed to use the kit
     * @param items The items in the kit
     * @return The created kit
     */
    public Kit createKit(String name, long cooldown, String permission, List<ItemStack> items) {
        Kit kit = new Kit(name);
        kit.setCooldown(cooldown);
        kit.setPermission(permission);
        
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
     */
    public boolean deleteKit(String name) {
        Kit kit = kits.remove(name != null ? name.toLowerCase() : null);
        
        if (kit != null) {
            // Remove cooldowns for this kit
            for (Map<String, Long> playerCooldowns : cooldowns.values()) {
                playerCooldowns.remove(name.toLowerCase());
            }
            
            // Save the kits
            saveKits();
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks if a player can use a kit
     * 
     * @param player The player
     * @param kitName The name of the kit
     * @return True if the player can use the kit, false otherwise
     */
    public boolean canUseKit(ServerPlayer player, String kitName) {
        Kit kit = getKit(kitName);
        
        if (kit == null || player == null) {
            return false;
        }
        
        // Check permission
        String permission = kit.getPermission();
        if (permission != null && !permission.isEmpty()) {
            // TODO: Check permission when we implement LuckPerms and FTB Ranks integration
            // For now, check if player is op
            if (!player.hasPermissions(2)) {
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
    
    /**
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
        
        // Check if the player can use the kit
        if (!canUseKit(player, kitName)) {
            return false;
        }
        
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
     * Represents a kit
     */
    public static class Kit {
        private final String name;
        private long cooldown = 0; // Cooldown in seconds
        private String permission = null;
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
        
        public List<ItemDefinition> getItemDefinitions() {
            return new ArrayList<>(itemDefinitions); // Return a copy to prevent modification
        }
        
        public void addItemDefinition(String itemId, int count) {
            itemDefinitions.add(new ItemDefinition(itemId, count));
        }
        
        public void clearItems() {
            itemDefinitions.clear();
        }
    }
}
