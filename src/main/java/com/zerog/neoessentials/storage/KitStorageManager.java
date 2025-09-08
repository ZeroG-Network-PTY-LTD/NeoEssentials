package com.zerog.neoessentials.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.Kit;
import com.zerog.neoessentials.util.DebugUtil;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Kit storage manager for NeoEssentials
 * Handles loading, saving, and managing kit data in JSON format
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class KitStorageManager {
    
    private static KitStorageManager instance;
    
    private final Gson gson;
    private final Path kitsDirectory;
    private final Path kitsFile;
    private final Path backupDirectory;
    
    private final Map<String, Kit> kitCache;
    private final ReadWriteLock kitLock;
    
    private long lastSaveTime = 0;
    private boolean autoSave = true;
    
    // File watching for external changes
    private long lastFileModified = 0;
    
    private KitStorageManager() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();
        
        // Initialize paths
        this.kitsDirectory = getServerDataPath().resolve("neoessentials").resolve("kits");
        this.kitsFile = kitsDirectory.resolve("kits.json");
        this.backupDirectory = kitsDirectory.resolve("backups");
        
        this.kitCache = new ConcurrentHashMap<>();
        this.kitLock = new ReentrantReadWriteLock();
        
        // Create directories
        createDirectories();
        
        // Load existing kits
        loadKits();
        
        // Create default kits if none exist
        createDefaultKits();
    }
    
    public static synchronized KitStorageManager getInstance() {
        if (instance == null) {
            instance = new KitStorageManager();
        }
        return instance;
    }
    
    /**
     * Get the server data path
     */
    private Path getServerDataPath() {
        // Try to get the server directory, fallback to current directory
        String serverPath = System.getProperty("user.dir");
        return Paths.get(serverPath);
    }
    
    /**
     * Create necessary directories
     */
    private void createDirectories() {
        try {
            Files.createDirectories(kitsDirectory);
            Files.createDirectories(backupDirectory);
            DebugUtil.debugLog("Created kit storage directories");
        } catch (IOException e) {
            DebugUtil.warnLog("Failed to create kit directories: " + e.getMessage());
        }
    }
    
    /**
     * Load kits from JSON file
     */
    private void loadKits() {
        kitLock.writeLock().lock();
        try {
            if (!Files.exists(kitsFile)) {
                DebugUtil.debugLog("No existing kits file found, starting with empty kit list");
                return;
            }
            
            String jsonContent = Files.readString(kitsFile);
            Type kitMapType = new TypeToken<Map<String, Kit>>(){}.getType();
            Map<String, Kit> loadedKits = gson.fromJson(jsonContent, kitMapType);
            
            if (loadedKits != null) {
                kitCache.clear();
                kitCache.putAll(loadedKits);
                lastFileModified = Files.getLastModifiedTime(kitsFile).toMillis();
                DebugUtil.debugLog("Loaded " + kitCache.size() + " kits from storage");
            }
            
        } catch (Exception e) {
            DebugUtil.warnLog("Failed to load kits from file: " + e.getMessage());
            
            // Try to load from backup
            tryLoadFromBackup();
        } finally {
            kitLock.writeLock().unlock();
        }
    }
    
    /**
     * Try to load kits from the most recent backup
     */
    private void tryLoadFromBackup() {
        try {
            File[] backupFiles = backupDirectory.toFile().listFiles(
                (dir, name) -> name.startsWith("kits_backup_") && name.endsWith(".json")
            );
            
            if (backupFiles != null && backupFiles.length > 0) {
                // Sort by modification time, newest first
                Arrays.sort(backupFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                
                File newestBackup = backupFiles[0];
                String jsonContent = Files.readString(newestBackup.toPath());
                Type kitMapType = new TypeToken<Map<String, Kit>>(){}.getType();
                Map<String, Kit> loadedKits = gson.fromJson(jsonContent, kitMapType);
                
                if (loadedKits != null) {
                    kitCache.putAll(loadedKits);
                    DebugUtil.debugLog("Loaded " + loadedKits.size() + " kits from backup: " + newestBackup.getName());
                }
            }
        } catch (Exception e) {
            DebugUtil.warnLog("Failed to load kits from backup: " + e.getMessage());
        }
    }
    
    /**
     * Save kits to JSON file
     */
    public boolean saveKits() {
        return saveKits(false);
    }
    
    /**
     * Save kits to JSON file with optional backup
     */
    public boolean saveKits(boolean createBackup) {
        kitLock.readLock().lock();
        try {
            if (createBackup && Files.exists(kitsFile)) {
                createBackup();
            }
            
            String jsonContent = gson.toJson(kitCache);
            
            // Write to temporary file first, then rename (atomic operation)
            Path tempFile = kitsFile.resolveSibling(kitsFile.getFileName() + ".tmp");
            Files.writeString(tempFile, jsonContent);
            Files.move(tempFile, kitsFile, StandardCopyOption.REPLACE_EXISTING);
            
            lastSaveTime = System.currentTimeMillis();
            lastFileModified = Files.getLastModifiedTime(kitsFile).toMillis();
            
            DebugUtil.debugLog("Saved " + kitCache.size() + " kits to storage");
            return true;
            
        } catch (IOException e) {
            DebugUtil.warnLog("Failed to save kits to file: " + e.getMessage());
            return false;
        } finally {
            kitLock.readLock().unlock();
        }
    }
    
    /**
     * Create a backup of the current kits file
     */
    private void createBackup() {
        try {
            if (Files.exists(kitsFile)) {
                String timestamp = String.valueOf(System.currentTimeMillis());
                Path backupFile = backupDirectory.resolve("kits_backup_" + timestamp + ".json");
                Files.copy(kitsFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                
                // Clean up old backups (keep only 5 most recent)
                cleanupOldBackups();
            }
        } catch (IOException e) {
            DebugUtil.warnLog("Failed to create backup: " + e.getMessage());
        }
    }
    
    /**
     * Clean up old backup files, keeping only the 5 most recent
     */
    private void cleanupOldBackups() {
        try {
            File[] backupFiles = backupDirectory.toFile().listFiles(
                (dir, name) -> name.startsWith("kits_backup_") && name.endsWith(".json")
            );
            
            if (backupFiles != null && backupFiles.length > 5) {
                // Sort by modification time, oldest first
                Arrays.sort(backupFiles, Comparator.comparingLong(File::lastModified));
                
                // Delete oldest files, keeping only 5
                for (int i = 0; i < backupFiles.length - 5; i++) {
                    Files.deleteIfExists(backupFiles[i].toPath());
                }
            }
        } catch (Exception e) {
            DebugUtil.warnLog("Failed to cleanup old backups: " + e.getMessage());
        }
    }
    
    /**
     * Check if the file was modified externally and reload if needed
     */
    public void checkForExternalChanges() {
        try {
            if (Files.exists(kitsFile)) {
                long currentFileModified = Files.getLastModifiedTime(kitsFile).toMillis();
                if (currentFileModified > lastFileModified) {
                    DebugUtil.debugLog("Detected external changes to kits file, reloading...");
                    loadKits();
                }
            }
        } catch (IOException e) {
            DebugUtil.warnLog("Failed to check for external changes: " + e.getMessage());
        }
    }
    
    // Kit management methods
    
    /**
     * Get a kit by name
     */
    public Kit getKit(String name) {
        kitLock.readLock().lock();
        try {
            checkForExternalChanges(); // Check for external changes
            return kitCache.get(name.toLowerCase());
        } finally {
            kitLock.readLock().unlock();
        }
    }
    
    /**
     * Get all kits
     */
    public Collection<Kit> getAllKits() {
        kitLock.readLock().lock();
        try {
            checkForExternalChanges(); // Check for external changes
            return new ArrayList<>(kitCache.values());
        } finally {
            kitLock.readLock().unlock();
        }
    }
    
    /**
     * Get kit names
     */
    public Set<String> getKitNames() {
        kitLock.readLock().lock();
        try {
            checkForExternalChanges(); // Check for external changes
            return new HashSet<>(kitCache.keySet());
        } finally {
            kitLock.readLock().unlock();
        }
    }
    
    /**
     * Add or update a kit
     */
    public boolean saveKit(Kit kit) {
        if (kit == null || kit.getName() == null) {
            return false;
        }
        
        kitLock.writeLock().lock();
        try {
            String kitName = kit.getName().toLowerCase();
            kitCache.put(kitName, kit);
            
            // Auto-save if enabled
            if (autoSave) {
                return saveKits(true); // Create backup when adding/updating kits
            }
            return true;
            
        } finally {
            kitLock.writeLock().unlock();
        }
    }
    
    /**
     * Delete a kit
     */
    public boolean deleteKit(String name) {
        if (name == null) {
            return false;
        }
        
        kitLock.writeLock().lock();
        try {
            String kitName = name.toLowerCase();
            Kit removedKit = kitCache.remove(kitName);
            
            if (removedKit != null) {
                // Auto-save if enabled
                if (autoSave) {
                    return saveKits(true); // Create backup when deleting kits
                }
                return true;
            }
            return false;
            
        } finally {
            kitLock.writeLock().unlock();
        }
    }
    
    /**
     * Check if a kit exists
     */
    public boolean kitExists(String name) {
        if (name == null) {
            return false;
        }
        
        kitLock.readLock().lock();
        try {
            checkForExternalChanges(); // Check for external changes
            return kitCache.containsKey(name.toLowerCase());
        } finally {
            kitLock.readLock().unlock();
        }
    }
    
    /**
     * Get kits count
     */
    public int getKitCount() {
        kitLock.readLock().lock();
        try {
            return kitCache.size();
        } finally {
            kitLock.readLock().unlock();
        }
    }
    
    /**
     * Create default kits if none exist
     */
    private void createDefaultKits() {
        if (kitCache.isEmpty()) {
            DebugUtil.debugLog("Creating default kits...");
            
            // Starter kit
            Kit starterKit = new Kit("starter", "Starter Kit", "Basic items to get you started");
            starterKit.setCooldown(3600); // 1 hour cooldown
            starterKit.setPermission("neoessentials.kit.starter");
            starterKit.setCreatedBy("System");
            
            List<com.zerog.neoessentials.data.KitItem> starterItems = new ArrayList<>();
            starterItems.add(new com.zerog.neoessentials.data.KitItem("minecraft:wooden_sword", 1));
            starterItems.add(new com.zerog.neoessentials.data.KitItem("minecraft:wooden_pickaxe", 1));
            starterItems.add(new com.zerog.neoessentials.data.KitItem("minecraft:wooden_axe", 1));
            starterItems.add(new com.zerog.neoessentials.data.KitItem("minecraft:wooden_shovel", 1));
            starterItems.add(new com.zerog.neoessentials.data.KitItem("minecraft:bread", 16));
            starterKit.setItems(starterItems);
            
            // Tools kit
            Kit toolsKit = new Kit("tools", "Tools Kit", "Basic iron tools");
            toolsKit.setCooldown(7200); // 2 hours cooldown
            toolsKit.setCost(500.0);
            toolsKit.setPermission("neoessentials.kit.tools");
            toolsKit.setCreatedBy("System");
            
            List<com.zerog.neoessentials.data.KitItem> toolsItems = new ArrayList<>();
            toolsItems.add(new com.zerog.neoessentials.data.KitItem("minecraft:iron_sword", 1));
            toolsItems.add(new com.zerog.neoessentials.data.KitItem("minecraft:iron_pickaxe", 1));
            toolsItems.add(new com.zerog.neoessentials.data.KitItem("minecraft:iron_axe", 1));
            toolsItems.add(new com.zerog.neoessentials.data.KitItem("minecraft:iron_shovel", 1));
            toolsKit.setItems(toolsItems);
            
            // Food kit
            Kit foodKit = new Kit("food", "Food Kit", "Variety of food items");
            foodKit.setCooldown(1800); // 30 minutes cooldown
            foodKit.setPermission("neoessentials.kit.food");
            foodKit.setCreatedBy("System");
            
            List<com.zerog.neoessentials.data.KitItem> foodItems = new ArrayList<>();
            foodItems.add(new com.zerog.neoessentials.data.KitItem("minecraft:cooked_beef", 16));
            foodItems.add(new com.zerog.neoessentials.data.KitItem("minecraft:golden_apple", 2));
            foodItems.add(new com.zerog.neoessentials.data.KitItem("minecraft:bread", 8));
            foodKit.setItems(foodItems);
            
            // Save default kits
            saveKit(starterKit);
            saveKit(toolsKit);
            saveKit(foodKit);
            
            DebugUtil.debugLog("Created 3 default kits");
        }
    }
    
    /**
     * Reload all kits from disk
     */
    public void reloadKits() {
        DebugUtil.debugLog("Reloading all kits from disk...");
        loadKits();
    }
    
    /**
     * Enable or disable auto-save
     */
    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
    }
    
    /**
     * Check if auto-save is enabled
     */
    public boolean isAutoSave() {
        return autoSave;
    }
    
    /**
     * Get the kits file path (for external editing)
     */
    public Path getKitsFilePath() {
        return kitsFile;
    }
    
    /**
     * Force save with backup
     */
    public boolean forceSave() {
        return saveKits(true);
    }
    
    /**
     * Get storage statistics
     */
    public Map<String, Object> getStorageStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalKits", getKitCount());
        stats.put("kitsFilePath", kitsFile.toString());
        stats.put("lastSaveTime", lastSaveTime);
        stats.put("autoSave", autoSave);
        stats.put("fileExists", Files.exists(kitsFile));
        
        try {
            if (Files.exists(kitsFile)) {
                stats.put("fileSize", Files.size(kitsFile));
                stats.put("lastModified", Files.getLastModifiedTime(kitsFile).toMillis());
            }
        } catch (IOException e) {
            stats.put("fileError", e.getMessage());
        }
        
        return stats;
    }
}
