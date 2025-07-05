package com.zerog.neoessentials.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.HomeData;
import com.zerog.neoessentials.data.KitManager;
import com.zerog.neoessentials.data.WarpData;

import net.minecraft.core.BlockPos;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Storage handler that saves data to JSON files
 */
public class JsonStorageHandler implements StorageHandler {
    private static final String BASE_DIR = "neoessentials";
    private static final String HOMES_DIR = BASE_DIR + "/homes";
    private static final String ECONOMY_DIR = BASE_DIR + "/economy";
    private static final String WARPS_FILE = BASE_DIR + "/warps.json";
    private static final String KITS_FILE = BASE_DIR + "/kits.json";
    private static final String SPAWN_FILE = BASE_DIR + "/spawn.json";
    
    private final Gson gson;
    
    public JsonStorageHandler() {
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }
    
    @Override
    public void initialize() {
        // Create directories if they don't exist
        createDirectory(BASE_DIR);
        createDirectory(HOMES_DIR);
        createDirectory(ECONOMY_DIR);
        
        NeoEssentials.LOGGER.info("Initialized JSON storage handler");
    }
    
    @Override
    public void shutdown() {
        // Nothing to do here for JSON storage
        NeoEssentials.LOGGER.info("JSON storage handler shut down");
    }
    
    private void createDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists() && !dir.mkdirs()) {
            NeoEssentials.LOGGER.error("Failed to create directory: {}", path);
        }
    }
    
    @Override
    public boolean saveHomeData(UUID uuid, Map<String, HomeData> homes) {
        try {
            File file = new File(HOMES_DIR + "/" + uuid.toString() + ".json");
            JsonObject rootObj = new JsonObject();
            JsonObject homesObj = new JsonObject();
            
            for (Map.Entry<String, HomeData> entry : homes.entrySet()) {
                String homeName = entry.getKey();
                HomeData home = entry.getValue();
                
                JsonObject homeObj = new JsonObject();
                homeObj.addProperty("dimension", home.getDimension());
                
                // Save position
                JsonObject posObj = new JsonObject();
                posObj.addProperty("x", home.getPosition().getX());
                posObj.addProperty("y", home.getPosition().getY());
                posObj.addProperty("z", home.getPosition().getZ());
                homeObj.add("position", posObj);
                
                // Save rotation
                homeObj.addProperty("pitch", home.getPitch());
                homeObj.addProperty("yaw", home.getYaw());
                
                homesObj.add(homeName, homeObj);
            }
            
            rootObj.add("homes", homesObj);
            
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(rootObj, writer);
            }
            
            return true;
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to save home data for {}: {}", uuid, e.getMessage());
            return false;
        }
    }
    
    @Override
    public Map<String, HomeData> loadHomeData(UUID uuid) {
        Map<String, HomeData> homes = new HashMap<>();
        
        try {
            File file = new File(HOMES_DIR + "/" + uuid.toString() + ".json");
            
            if (!file.exists()) {
                return homes;
            }
            
            JsonObject rootObj;
            try (FileReader reader = new FileReader(file)) {
                rootObj = JsonParser.parseReader(reader).getAsJsonObject();
            }
            
            if (rootObj.has("homes")) {
                JsonObject homesObj = rootObj.getAsJsonObject("homes");
                
                for (Map.Entry<String, JsonElement> entry : homesObj.entrySet()) {
                    String homeName = entry.getKey();
                    JsonObject homeObj = entry.getValue().getAsJsonObject();
                    
                    String dimension = homeObj.get("dimension").getAsString();
                    
                    // Load position
                    JsonObject posObj = homeObj.getAsJsonObject("position");
                    int x = posObj.get("x").getAsInt();
                    int y = posObj.get("y").getAsInt();
                    int z = posObj.get("z").getAsInt();
                    BlockPos pos = new BlockPos(x, y, z);
                    
                    // Load rotation
                    float pitch = homeObj.get("pitch").getAsFloat();
                    float yaw = homeObj.get("yaw").getAsFloat();
                    
                    homes.put(homeName, new HomeData(dimension, pos, pitch, yaw));
                }
            }
            
            return homes;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to load home data for {}: {}", uuid, e.getMessage());
            return homes;
        }
    }
      @Override
    public boolean saveWarps(Map<String, WarpData> warps) {
        NeoEssentials.LOGGER.info("JsonStorageHandler: Saving {} warps to {}", warps.size(), WARPS_FILE);
        
        try {
            File file = new File(WARPS_FILE);
            
            // Create parent directory if it doesn't exist
            File parentDir = file.getParentFile();
            if (!parentDir.exists() && !parentDir.mkdirs()) {
                NeoEssentials.LOGGER.error("Failed to create directory: {}", parentDir.getAbsolutePath());
                return false;
            }
            
            JsonObject rootObj = new JsonObject();
            JsonObject warpsObj = new JsonObject();
            
            for (Map.Entry<String, WarpData> entry : warps.entrySet()) {
                String warpName = entry.getKey();
                WarpData warp = entry.getValue();
                
                try {
                    JsonObject warpObj = new JsonObject();
                    warpObj.addProperty("name", warp.getName());
                    warpObj.addProperty("dimension", warp.getDimension());
                    
                    BlockPos position = warp.getPosition();
                    // Save position
                    JsonObject posObj = new JsonObject();
                    posObj.addProperty("x", position.getX());
                    posObj.addProperty("y", position.getY());
                    posObj.addProperty("z", position.getZ());
                    warpObj.add("position", posObj);
                    
                    // Save rotation
                    warpObj.addProperty("pitch", warp.getPitch());
                    warpObj.addProperty("yaw", warp.getYaw());
                    
                    // Save permission
                    if (warp.getPermission() != null) {
                        warpObj.addProperty("permission", warp.getPermission());
                    }
                    
                    warpsObj.add(warpName, warpObj);
                    NeoEssentials.LOGGER.debug("Prepared warp '{}' at [{}, {}, {}] in dimension '{}' for saving", 
                        warpName, position.getX(), position.getY(), position.getZ(), warp.getDimension());
                } catch (Exception e) {
                    NeoEssentials.LOGGER.error("Error preparing warp '{}' for saving: {}", warpName, e.getMessage());
                }
            }
            
            rootObj.add("warps", warpsObj);
            
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(rootObj, writer);
            }
            
            NeoEssentials.LOGGER.info("Successfully saved {} warps to {}", warps.size(), file.getAbsolutePath());
            return true;
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to save warps: {}", e.getMessage(), e);
            return false;
        }
    }
      @Override
    public Map<String, WarpData> loadWarps() {
        Map<String, WarpData> warps = new HashMap<>();
        
        NeoEssentials.LOGGER.info("JsonStorageHandler: Loading warps from {}", WARPS_FILE);
        
        try {
            File file = new File(WARPS_FILE);
            
            if (!file.exists()) {
                NeoEssentials.LOGGER.info("Warps file doesn't exist yet at {}", file.getAbsolutePath());
                return warps;
            }
            
            NeoEssentials.LOGGER.debug("Reading warps file: {}", file.getAbsolutePath());
            
            JsonObject rootObj;
            try (FileReader reader = new FileReader(file)) {
                rootObj = JsonParser.parseReader(reader).getAsJsonObject();
            }
            
            if (rootObj.has("warps")) {
                JsonObject warpsObj = rootObj.getAsJsonObject("warps");
                NeoEssentials.LOGGER.debug("Found {} warps in JSON file", warpsObj.size());
                
                for (Map.Entry<String, JsonElement> entry : warpsObj.entrySet()) {
                    String warpName = entry.getKey();
                    
                    try {
                        JsonObject warpObj = entry.getValue().getAsJsonObject();
                        
                        String name = warpObj.get("name").getAsString();
                        String dimension = warpObj.get("dimension").getAsString();
                        
                        // Load position
                        JsonObject posObj = warpObj.getAsJsonObject("position");
                        int x = posObj.get("x").getAsInt();
                        int y = posObj.get("y").getAsInt();
                        int z = posObj.get("z").getAsInt();
                        BlockPos pos = new BlockPos(x, y, z);
                        
                        // Load rotation
                        float pitch = warpObj.get("pitch").getAsFloat();
                        float yaw = warpObj.get("yaw").getAsFloat();
                        
                        // Load permission
                        String permission = null;
                        if (warpObj.has("permission")) {
                            permission = warpObj.get("permission").getAsString();
                        }
                        
                        warps.put(warpName, new WarpData(name, dimension, pos, pitch, yaw, permission));
                        NeoEssentials.LOGGER.debug("Successfully loaded warp '{}' at [{}, {}, {}] in dimension '{}'", 
                            name, x, y, z, dimension);
                    } catch (Exception e) {
                        NeoEssentials.LOGGER.error("Error loading warp '{}': {}", warpName, e.getMessage());
                    }
                }
                
                NeoEssentials.LOGGER.info("Successfully loaded {} warps", warps.size());
            } else {
                NeoEssentials.LOGGER.warn("No 'warps' object found in JSON file");
            }
            
            return warps;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to load warps: {}", e.getMessage(), e);
            return warps;
        }
    }
    
    @Override
    public boolean saveKits(Map<String, KitManager.Kit> kits, Map<UUID, Map<String, Long>> cooldowns) {
        try {
            File kitFile = new File(KITS_FILE);
            
            // Create parent directories if they don't exist
            File parentDir = kitFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                NeoEssentials.LOGGER.error("Failed to create directory for kit data: {}", parentDir);
                return false;
            }
            
            JsonObject kitsData = new JsonObject();
            
            // Save kits
            JsonObject kitsObj = new JsonObject();
            for (Map.Entry<String, KitManager.Kit> entry : kits.entrySet()) {
                KitManager.Kit kit = entry.getValue();
                JsonObject kitObj = new JsonObject();
                
                // Save cooldown
                kitObj.addProperty("cooldown", kit.getCooldown());
                
                // Save permission
                if (kit.getPermission() != null) {
                    kitObj.addProperty("permission", kit.getPermission());
                }
                
                // Save items
                JsonArray itemsArray = new JsonArray();
                for (KitManager.ItemDefinition itemDef : kit.getItemDefinitions()) {
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
            
            return true;
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Error saving kit data", e);
            return false;
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<Object> loadKits() {
        try {
            File kitFile = new File(KITS_FILE);
            Map<String, KitManager.Kit> kits = new HashMap<>();
            Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
            
            if (kitFile.exists()) {
                JsonObject kitsData = null;
                
                try (FileReader reader = new FileReader(kitFile)) {
                    kitsData = JsonParser.parseReader(reader).getAsJsonObject();
                }
                
                if (kitsData != null) {
                    // Load kits
                    if (kitsData.has("kits")) {
                        JsonObject kitsObj = kitsData.getAsJsonObject("kits");
                        for (Map.Entry<String, JsonElement> entry : kitsObj.entrySet()) {
                            String kitName = entry.getKey();
                            JsonObject kitObj = entry.getValue().getAsJsonObject();
                            
                            // Create the kit
                            KitManager.Kit kit = new KitManager.Kit(kitName);
                            
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
                }
            }
            
            List<Object> result = new ArrayList<>();
            result.add(kits);
            result.add(cooldowns);
            return result;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error loading kit data", e);
            return null;
        }
    }
    
    @Override
    public boolean saveSpawnData(Map<String, Object> spawn) {
        try {
            File file = new File(SPAWN_FILE);
            JsonObject rootObj = new JsonObject();
            
            // Add spawn data
            if (spawn.containsKey("dimension")) {
                rootObj.addProperty("dimension", (String) spawn.get("dimension"));
            }
            
            if (spawn.containsKey("position")) {
                BlockPos pos = (BlockPos) spawn.get("position");
                JsonObject posObj = new JsonObject();
                posObj.addProperty("x", pos.getX());
                posObj.addProperty("y", pos.getY());
                posObj.addProperty("z", pos.getZ());
                rootObj.add("position", posObj);
            }
            
            if (spawn.containsKey("pitch")) {
                rootObj.addProperty("pitch", (Float) spawn.get("pitch"));
            }
            
            if (spawn.containsKey("yaw")) {
                rootObj.addProperty("yaw", (Float) spawn.get("yaw"));
            }
            
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(rootObj, writer);
            }
            
            return true;
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to save spawn data: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public Map<String, Object> loadSpawnData() {
        Map<String, Object> spawn = new HashMap<>();
        
        try {
            File file = new File(SPAWN_FILE);
            
            if (!file.exists()) {
                return spawn;
            }
            
            JsonObject rootObj;
            try (FileReader reader = new FileReader(file)) {
                rootObj = JsonParser.parseReader(reader).getAsJsonObject();
            }
            
            // Load dimension
            if (rootObj.has("dimension")) {
                spawn.put("dimension", rootObj.get("dimension").getAsString());
            }
            
            // Load position
            if (rootObj.has("position")) {
                JsonObject posObj = rootObj.getAsJsonObject("position");
                int x = posObj.get("x").getAsInt();
                int y = posObj.get("y").getAsInt();
                int z = posObj.get("z").getAsInt();
                spawn.put("position", new BlockPos(x, y, z));
            }
            
            // Load rotation
            if (rootObj.has("pitch")) {
                spawn.put("pitch", rootObj.get("pitch").getAsFloat());
            }
            
            if (rootObj.has("yaw")) {
                spawn.put("yaw", rootObj.get("yaw").getAsFloat());
            }
            
            return spawn;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to load spawn data: {}", e.getMessage());
            return spawn;
        }
    }
}
