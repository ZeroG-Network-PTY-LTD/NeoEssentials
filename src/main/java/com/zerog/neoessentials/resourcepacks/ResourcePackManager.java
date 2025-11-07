package com.zerog.neoessentials.resourcepacks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Manages server resource packs including upload, storage, and player assignments.
 */
public class ResourcePackManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePackManager.class);
    private static ResourcePackManager instance;
    
    private final Map<String, ResourcePack> resourcePacks;
    private final Path storageDirectory;
    private final Path packsFile;
    private final Gson gson;
    private MinecraftServer server;
    
    private static final long MAX_PACK_SIZE = 100 * 1024 * 1024; // 100 MB
    private static final String PACKS_DIR = "neoessentials/resourcepacks";
    private static final String PACKS_FILE = "packs.json";
    
    private ResourcePackManager() {
        this.resourcePacks = new ConcurrentHashMap<>();
        this.storageDirectory = Paths.get(PACKS_DIR);
        this.packsFile = storageDirectory.resolve(PACKS_FILE);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        try {
            Files.createDirectories(storageDirectory);
            loadPacks();
        } catch (IOException e) {
            LOGGER.error("Failed to create resource packs directory", e);
        }
    }
    
    public static ResourcePackManager getInstance() {
        if (instance == null) {
            instance = new ResourcePackManager();
        }
        return instance;
    }
    
    public void setServer(MinecraftServer server) {
        this.server = server;
    }
    
    /**
     * Upload a new resource pack from file data
     */
    public ResourcePack uploadPack(String name, byte[] fileData, String uploadedBy) throws Exception {
        if (fileData.length > MAX_PACK_SIZE) {
            throw new IllegalArgumentException("Resource pack exceeds maximum size of " + (MAX_PACK_SIZE / 1024 / 1024) + " MB");
        }
        
        // Calculate SHA-1 hash
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = digest.digest(fileData);
        String hash = bytesToHex(hashBytes);
        
        // Create resource pack
        ResourcePack pack = new ResourcePack();
        pack.setName(name);
        pack.setFileName(name + ".zip");
        pack.setFileHash(hash);
        pack.setFileSize(fileData.length);
        pack.setUploadedBy(uploadedBy);
        pack.setUploadedAt(Instant.now());
        
        // Parse pack.mcmeta and extract icon
        try {
            parsePackMetadata(pack, fileData);
        } catch (Exception e) {
            LOGGER.warn("Failed to parse pack metadata for {}", name, e);
        }
        
        // Save file to disk
        Path packFile = storageDirectory.resolve(pack.getId() + ".zip");
        Files.write(packFile, fileData);
        pack.setUrl(packFile.toString());
        
        // Store pack
        resourcePacks.put(pack.getId(), pack);
        savePacks();
        
        LOGGER.info("Uploaded resource pack: {} ({})", name, pack.getId());
        return pack;
    }
    
    /**
     * Register an external resource pack by URL
     */
    public ResourcePack registerExternalPack(String name, String url, String hash, String uploadedBy) {
        ResourcePack pack = new ResourcePack();
        pack.setName(name);
        pack.setUrl(url);
        pack.setFileHash(hash);
        pack.setExternal(true);
        pack.setUploadedBy(uploadedBy);
        pack.setUploadedAt(Instant.now());
        
        resourcePacks.put(pack.getId(), pack);
        savePacks();
        
        LOGGER.info("Registered external resource pack: {} from {}", name, url);
        return pack;
    }
    
    /**
     * Delete a resource pack
     */
    public boolean deletePack(String packId) {
        ResourcePack pack = resourcePacks.remove(packId);
        if (pack == null) {
            return false;
        }
        
        // Delete file if local
        if (!pack.isExternal()) {
            try {
                Path packFile = Paths.get(pack.getUrl());
                Files.deleteIfExists(packFile);
            } catch (IOException e) {
                LOGGER.error("Failed to delete pack file for {}", packId, e);
            }
        }
        
        savePacks();
        LOGGER.info("Deleted resource pack: {}", pack.getName());
        return true;
    }
    
    /**
     * Get a resource pack by ID
     */
    public ResourcePack getPack(String packId) {
        return resourcePacks.get(packId);
    }
    
    /**
     * Get all resource packs
     */
    public Collection<ResourcePack> getAllPacks() {
        return resourcePacks.values();
    }
    
    /**
     * Set the active resource pack for the server
     */
    public void setActivePack(String packId) {
        // Deactivate all packs
        resourcePacks.values().forEach(pack -> pack.setActive(false));
        
        // Activate specified pack
        ResourcePack pack = resourcePacks.get(packId);
        if (pack != null) {
            pack.setActive(true);
            savePacks();
            LOGGER.info("Set active resource pack: {}", pack.getName());
        }
    }
    
    /**
     * Assign a resource pack to a player
     */
    public void assignToPlayer(String packId, String playerUuid) {
        ResourcePack pack = resourcePacks.get(packId);
        if (pack != null) {
            pack.addAssignedPlayer(playerUuid);
            savePacks();
            
            // Send pack to player if online
            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(playerUuid));
                if (player != null) {
                    sendPackToPlayer(player, pack);
                }
            }
        }
    }
    
    /**
     * Remove a resource pack assignment from a player
     */
    public void unassignFromPlayer(String packId, String playerUuid) {
        ResourcePack pack = resourcePacks.get(packId);
        if (pack != null) {
            pack.removeAssignedPlayer(playerUuid);
            savePacks();
        }
    }
    
    /**
     * Send a resource pack to a player
     */
    public void sendPackToPlayer(ServerPlayer player, ResourcePack pack) {
        if (pack == null || player == null) {
            return;
        }
        
        try {
            UUID packUuid = UUID.randomUUID();
            boolean required = pack.getEnforcementMode() == ResourcePack.EnforcementMode.REQUIRED ||
                             pack.getEnforcementMode() == ResourcePack.EnforcementMode.FORCED;
            
            ClientboundResourcePackPushPacket packet = new ClientboundResourcePackPushPacket(
                packUuid,
                pack.getUrl(),
                pack.getFileHash(),
                required,
                java.util.Optional.of(net.minecraft.network.chat.Component.literal(pack.getDescription() != null ? pack.getDescription() : pack.getName()))
            );
            
            player.connection.send(packet);
            LOGGER.info("Sent resource pack {} to player {}", pack.getName(), player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Failed to send resource pack to player", e);
        }
    }
    
    /**
     * Apply resource pack assignments when a player joins
     */
    public void applyPacksForPlayer(ServerPlayer player) {
        String playerUuid = player.getUUID().toString();
        
        // Apply assigned packs
        for (ResourcePack pack : resourcePacks.values()) {
            if (pack.isAssignedToPlayer(playerUuid)) {
                sendPackToPlayer(player, pack);
            }
        }
        
        // Apply active pack if not specifically assigned
        resourcePacks.values().stream()
            .filter(ResourcePack::isActive)
            .findFirst()
            .ifPresent(pack -> {
                if (!pack.isAssignedToPlayer(playerUuid)) {
                    sendPackToPlayer(player, pack);
                }
            });
    }
    
    /**
     * Parse pack.mcmeta from ZIP file
     */
    private void parsePackMetadata(ResourcePack pack, byte[] zipData) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(zipData);
             ZipInputStream zis = new ZipInputStream(bais)) {
            
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                
                // Parse pack.mcmeta
                if (entryName.equals("pack.mcmeta")) {
                    String json = new String(zis.readAllBytes());
                    Map<String, Object> data = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
                    
                    if (data.containsKey("pack")) {
                        @SuppressWarnings("unchecked") // JSON deserialization guarantees Map<String,Object> structure
                        Map<String, Object> packData = (Map<String, Object>) data.get("pack");
                        ResourcePack.PackMetadata metadata = new ResourcePack.PackMetadata();
                        
                        if (packData.containsKey("pack_format")) {
                            metadata.setPackFormat(((Number) packData.get("pack_format")).intValue());
                        }
                        if (packData.containsKey("description")) {
                            Object desc = packData.get("description");
                            metadata.setDescription(desc.toString());
                        }
                        metadata.setAdditionalData(packData);
                        pack.setMetadata(metadata);
                    }
                }
                
                // Extract icon
                else if (entryName.equals("pack.png")) {
                    byte[] iconData = zis.readAllBytes();
                    pack.setIconData(iconData);
                }
                
                zis.closeEntry();
            }
        }
    }
    
    /**
     * Load resource packs from disk
     */
    private void loadPacks() {
        if (!Files.exists(packsFile)) {
            return;
        }
        
        try {
            String json = Files.readString(packsFile);
            Map<String, ResourcePack> loaded = gson.fromJson(json, new TypeToken<Map<String, ResourcePack>>(){}.getType());
            if (loaded != null) {
                resourcePacks.putAll(loaded);
                LOGGER.info("Loaded {} resource packs", loaded.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load resource packs", e);
        }
    }
    
    /**
     * Save resource packs to disk
     */
    private void savePacks() {
        try {
            // Don't save icon data to JSON (too large)
            Map<String, ResourcePack> toSave = new HashMap<>();
            for (Map.Entry<String, ResourcePack> entry : resourcePacks.entrySet()) {
                ResourcePack pack = entry.getValue();
                ResourcePack copy = new ResourcePack();
                copy.setId(pack.getId());
                copy.setName(pack.getName());
                copy.setDescription(pack.getDescription());
                copy.setFileName(pack.getFileName());
                copy.setFileHash(pack.getFileHash());
                copy.setFileSize(pack.getFileSize());
                copy.setUrl(pack.getUrl());
                copy.setExternal(pack.isExternal());
                copy.setMetadata(pack.getMetadata());
                copy.setUploadedAt(pack.getUploadedAt());
                copy.setUploadedBy(pack.getUploadedBy());
                copy.setActive(pack.isActive());
                copy.setEnforcementMode(pack.getEnforcementMode());
                copy.setAssignedPlayers(new HashSet<>(pack.getAssignedPlayers()));
                copy.setAssignedGroups(new HashSet<>(pack.getAssignedGroups()));
                toSave.put(entry.getKey(), copy);
            }
            
            String json = gson.toJson(toSave);
            Files.writeString(packsFile, json);
        } catch (Exception e) {
            LOGGER.error("Failed to save resource packs", e);
        }
    }
    
    /**
     * Convert byte array to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * Get pack file data
     */
    public byte[] getPackFileData(String packId) throws IOException {
        ResourcePack pack = resourcePacks.get(packId);
        if (pack == null || pack.isExternal()) {
            return null;
        }
        
        Path packFile = Paths.get(pack.getUrl());
        if (Files.exists(packFile)) {
            return Files.readAllBytes(packFile);
        }
        return null;
    }
}
