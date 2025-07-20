package com.neoessentials.api.home.data.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neoessentials.api.home.data.HomeData;
import com.neoessentials.api.home.data.HomeDataManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON-based home data manager implementation
 * Similar to EssentialsX user data files
 */
public class JsonHomeDataManager implements HomeDataManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    private final Path homeDataDir;
    private final Map<UUID, Map<String, HomeData>> cache = new ConcurrentHashMap<>();
    
    public JsonHomeDataManager(Path dataDirectory) {
        this.homeDataDir = dataDirectory.resolve("homes");
        try {
            Files.createDirectories(homeDataDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create home data directory", e);
        }
    }
    
    @Override
    public CompletableFuture<Void> saveHome(UUID playerUUID, String homeName, HomeData home) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Update cache
                cache.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>())
                     .put(homeName, home);
                
                // Save to file
                Path playerFile = getPlayerFile(playerUUID);
                Map<String, HomeData> playerHomes = cache.get(playerUUID);
                
                JsonObject root = new JsonObject();
                JsonObject homesJson = new JsonObject();
                
                for (Map.Entry<String, HomeData> entry : playerHomes.entrySet()) {
                    JsonObject homeJson = serializeHome(entry.getValue());
                    homesJson.add(entry.getKey(), homeJson);
                }
                
                root.add("homes", homesJson);
                root.addProperty("lastModified", System.currentTimeMillis());
                
                Files.writeString(playerFile, GSON.toJson(root));
            } catch (IOException e) {
                throw new RuntimeException("Failed to save home data", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<HomeData> loadHome(UUID playerUUID, String homeName) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, HomeData> playerHomes = cache.get(playerUUID);
            if (playerHomes != null && playerHomes.containsKey(homeName)) {
                return playerHomes.get(homeName);
            }
            
            // Load from file if not in cache
            loadPlayerHomesFromFile(playerUUID);
            playerHomes = cache.get(playerUUID);
            
            return playerHomes != null ? playerHomes.get(homeName) : null;
        });
    }
    
    @Override
    public CompletableFuture<List<HomeData>> loadPlayerHomes(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, HomeData> playerHomes = cache.get(playerUUID);
            if (playerHomes == null) {
                loadPlayerHomesFromFile(playerUUID);
                playerHomes = cache.get(playerUUID);
            }
            
            return playerHomes != null ? new ArrayList<>(playerHomes.values()) : new ArrayList<>();
        });
    }
    
    @Override
    public CompletableFuture<Boolean> deleteHome(UUID playerUUID, String homeName) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, HomeData> playerHomes = cache.get(playerUUID);
            if (playerHomes == null) {
                loadPlayerHomesFromFile(playerUUID);
                playerHomes = cache.get(playerUUID);
            }
            
            if (playerHomes != null && playerHomes.containsKey(homeName)) {
                playerHomes.remove(homeName);
                
                // Save updated data
                try {
                    savePlayerHomesToFile(playerUUID, playerHomes);
                    return true;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to delete home", e);
                }
            }
            
            return false;
        });
    }
    
    @Override
    public CompletableFuture<Integer> getHomeCount(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, HomeData> playerHomes = cache.get(playerUUID);
            if (playerHomes == null) {
                loadPlayerHomesFromFile(playerUUID);
                playerHomes = cache.get(playerUUID);
            }
            
            return playerHomes != null ? playerHomes.size() : 0;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> hasHome(UUID playerUUID, String homeName) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, HomeData> playerHomes = cache.get(playerUUID);
            if (playerHomes == null) {
                loadPlayerHomesFromFile(playerUUID);
                playerHomes = cache.get(playerUUID);
            }
            
            return playerHomes != null && playerHomes.containsKey(homeName);
        });
    }
    
    @Override
    public CompletableFuture<List<String>> getHomeNames(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, HomeData> playerHomes = cache.get(playerUUID);
            if (playerHomes == null) {
                loadPlayerHomesFromFile(playerUUID);
                playerHomes = cache.get(playerUUID);
            }
            
            return playerHomes != null ? new ArrayList<>(playerHomes.keySet()) : new ArrayList<>();
        });
    }
    
    private void loadPlayerHomesFromFile(UUID playerUUID) {
        try {
            Path playerFile = getPlayerFile(playerUUID);
            if (!Files.exists(playerFile)) {
                cache.put(playerUUID, new ConcurrentHashMap<>());
                return;
            }
            
            String content = Files.readString(playerFile);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            
            Map<String, HomeData> playerHomes = new ConcurrentHashMap<>();
            
            if (root.has("homes")) {
                JsonObject homesJson = root.getAsJsonObject("homes");
                for (Map.Entry<String, com.google.gson.JsonElement> entry : homesJson.entrySet()) {
                    String homeName = entry.getKey();
                    JsonObject homeJson = entry.getValue().getAsJsonObject();
                    HomeData home = deserializeHome(homeName, homeJson);
                    playerHomes.put(homeName, home);
                }
            }
            
            cache.put(playerUUID, playerHomes);
        } catch (IOException e) {
            cache.put(playerUUID, new ConcurrentHashMap<>());
        }
    }
    
    private void savePlayerHomesToFile(UUID playerUUID, Map<String, HomeData> playerHomes) throws IOException {
        Path playerFile = getPlayerFile(playerUUID);
        
        JsonObject root = new JsonObject();
        JsonObject homesJson = new JsonObject();
        
        for (Map.Entry<String, HomeData> entry : playerHomes.entrySet()) {
            JsonObject homeJson = serializeHome(entry.getValue());
            homesJson.add(entry.getKey(), homeJson);
        }
        
        root.add("homes", homesJson);
        root.addProperty("lastModified", System.currentTimeMillis());
        
        Files.writeString(playerFile, GSON.toJson(root));
    }
    
    private JsonObject serializeHome(HomeData home) {
        JsonObject json = new JsonObject();
        json.addProperty("dimension", home.getDimension().location().toString());
        json.addProperty("x", home.getX());
        json.addProperty("y", home.getY());
        json.addProperty("z", home.getZ());
        json.addProperty("yaw", home.getYaw());
        json.addProperty("pitch", home.getPitch());
        json.addProperty("createdAt", home.getCreatedAt());
        return json;
    }
    
    private HomeData deserializeHome(String name, JsonObject json) {
        String dimensionStr = json.get("dimension").getAsString();
        ResourceLocation dimensionId = ResourceLocation.parse(dimensionStr);
        ResourceKey<Level> dimension = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION, 
            dimensionId
        );
        
        double x = json.get("x").getAsDouble();
        double y = json.get("y").getAsDouble();
        double z = json.get("z").getAsDouble();
        float yaw = json.get("yaw").getAsFloat();
        float pitch = json.get("pitch").getAsFloat();
        long createdAt = json.get("createdAt").getAsLong();
        
        return new HomeData(name, dimension, x, y, z, yaw, pitch, createdAt);
    }
    
    private Path getPlayerFile(UUID playerUUID) {
        return homeDataDir.resolve(playerUUID.toString() + ".json");
    }
    
    /**
     * Clear cache for a player (useful for logout)
     */
    public void clearPlayerCache(UUID playerUUID) {
        cache.remove(playerUUID);
    }
    
    /**
     * Save all cached data to files
     */
    public CompletableFuture<Void> saveAll() {
        return CompletableFuture.runAsync(() -> {
            for (Map.Entry<UUID, Map<String, HomeData>> entry : cache.entrySet()) {
                try {
                    savePlayerHomesToFile(entry.getKey(), entry.getValue());
                } catch (IOException e) {
                    // Log error but continue with other players
                    System.err.println("Failed to save homes for player " + entry.getKey() + ": " + e.getMessage());
                }
            }
        });
    }
}
