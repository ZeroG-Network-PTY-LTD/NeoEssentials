package com.zerog.neoessentials.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages persistent storage for player bookmarks.
 * Handles saving and loading bookmark data to/from JSON files.
 */
public class BookmarkManager {
    
    private static final String BOOKMARKS_DIR = "neoessentials/bookmarks";
    private static final String BOOKMARKS_FILE = "player_bookmarks.json";
    
    // In-memory storage for quick access
    private final Map<UUID, Map<String, BookmarkData>> playerBookmarks = new ConcurrentHashMap<>();
    
    // Maximum bookmarks per player
    private static final int MAX_BOOKMARKS = 25;
    
    private final Gson gson;
    private final Path bookmarksFile;
    
    public BookmarkManager() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        
        // Initialize storage directory
        Path bookmarksDir = Paths.get(BOOKMARKS_DIR);
        try {
            Files.createDirectories(bookmarksDir);
            this.bookmarksFile = bookmarksDir.resolve(BOOKMARKS_FILE);
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to create bookmarks directory: {}", e.getMessage());
            throw new RuntimeException("Could not initialize bookmark storage", e);
        }
        
        loadBookmarks();
    }
    
    /**
     * Adds a bookmark for a player.
     * 
     * @param player The player adding the bookmark
     * @param name The bookmark name
     * @param bookmark The bookmark data
     * @return True if added successfully, false if limit exceeded or name exists
     */
    public boolean addBookmark(ServerPlayer player, String name, BookmarkData bookmark) {
        UUID playerId = player.getUUID();
        Map<String, BookmarkData> bookmarks = playerBookmarks.computeIfAbsent(playerId, k -> new HashMap<>());
        
        // Check if player has too many bookmarks
        if (bookmarks.size() >= MAX_BOOKMARKS && !bookmarks.containsKey(name)) {
            return false;
        }
        
        bookmarks.put(name, bookmark);
        saveBookmarks();
        return true;
    }
    
    /**
     * Removes a bookmark for a player.
     * 
     * @param playerId The player's UUID
     * @param name The bookmark name to remove
     * @return True if removed, false if not found
     */
    public boolean removeBookmark(UUID playerId, String name) {
        Map<String, BookmarkData> bookmarks = playerBookmarks.get(playerId);
        if (bookmarks == null) {
            return false;
        }
        
        boolean removed = bookmarks.remove(name) != null;
        if (removed) {
            saveBookmarks();
        }
        return removed;
    }
    
    /**
     * Gets all bookmarks for a player.
     * 
     * @param playerId The player's UUID
     * @return Map of bookmark names to bookmark data
     */
    public Map<String, BookmarkData> getPlayerBookmarks(UUID playerId) {
        return playerBookmarks.getOrDefault(playerId, new HashMap<>());
    }
    
    /**
     * Gets a specific bookmark for a player.
     * 
     * @param playerId The player's UUID
     * @param name The bookmark name
     * @return The bookmark data, or null if not found
     */
    public BookmarkData getBookmark(UUID playerId, String name) {
        Map<String, BookmarkData> bookmarks = playerBookmarks.get(playerId);
        return bookmarks != null ? bookmarks.get(name) : null;
    }
    
    /**
     * Gets the maximum number of bookmarks allowed per player.
     */
    public int getMaxBookmarks() {
        return MAX_BOOKMARKS;
    }
    
    /**
     * Loads bookmarks from persistent storage.
     */
    private void loadBookmarks() {
        if (!Files.exists(bookmarksFile)) {
            NeoEssentials.LOGGER.info("No existing bookmarks file found, starting with empty data");
            return;
        }
        
        try (Reader reader = Files.newBufferedReader(bookmarksFile)) {
            Type type = new TypeToken<Map<UUID, Map<String, BookmarkData>>>(){}.getType();
            Map<UUID, Map<String, BookmarkData>> loadedData = gson.fromJson(reader, type);
            
            if (loadedData != null) {
                playerBookmarks.clear();
                playerBookmarks.putAll(loadedData);
                NeoEssentials.LOGGER.info("Loaded bookmarks for {} players", playerBookmarks.size());
            }
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to load bookmarks: {}", e.getMessage());
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to parse bookmarks file: {}", e.getMessage());
        }
    }
    
    /**
     * Saves bookmarks to persistent storage.
     */
    private void saveBookmarks() {
        try (Writer writer = Files.newBufferedWriter(bookmarksFile)) {
            gson.toJson(playerBookmarks, writer);
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to save bookmarks: {}", e.getMessage());
        }
    }
    
    /**
     * Forces a save of all bookmark data.
     */
    public void forceSave() {
        saveBookmarks();
    }
    
    /**
     * Data class for storing bookmark information.
     */
    public static class BookmarkData {
        public final String dimension;
        public final double x, y, z;
        public final float yaw, pitch;
        public final long timestamp;
        public final String description;
        
        public BookmarkData(String dimension, double x, double y, double z, float yaw, float pitch, long timestamp, String description) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.timestamp = timestamp;
            this.description = description != null ? description : "";
        }
        
        public BookmarkData(String dimension, double x, double y, double z, float yaw, float pitch, long timestamp) {
            this(dimension, x, y, z, yaw, pitch, timestamp, "");
        }
    }
}
