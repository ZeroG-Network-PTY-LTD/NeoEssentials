package com.zerog.neoessentials.resourcepacks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
 *
 * <p>Pack metadata (name, hash, assignments, etc.) is persisted through the pluggable
 * {@link com.zerog.neoessentials.storage.DataStore} (collection {@link #PACKS_COLLECTION},
 * id = pack id). The actual {@code .zip} binary files always stay on disk under
 * {@link #storageDirectory} — only their metadata record moves between backends.
 */
public class ResourcePackManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePackManager.class);
    private static ResourcePackManager instance;

    private final Map<String, ResourcePack> resourcePacks;
    private final Path storageDirectory;
    private final Path packsFile;
    private final Gson gson;
    private final com.zerog.neoessentials.storage.DataStore store;
    private MinecraftServer server;

    private static final long MAX_PACK_SIZE = 100 * 1024 * 1024; // 100 MB
    private static final String PACKS_DIR = "neoessentials/resourcepacks";
    private static final String PACKS_FILE = "packs.json";
    private static final String PACKS_COLLECTION = "resource_packs";

    private ResourcePackManager() {
        this.resourcePacks = new ConcurrentHashMap<>();
        this.storageDirectory = Paths.get(PACKS_DIR);
        this.packsFile = storageDirectory.resolve(PACKS_FILE);
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        this.store = com.zerog.neoessentials.storage.StorageManager.getInstance().getStore();

        try {
            Files.createDirectories(storageDirectory);
            migrateLegacyFilesIfNeeded();
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

        store.delete(PACKS_COLLECTION, packId);
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
     * Load resource pack metadata from the active {@link com.zerog.neoessentials.storage.DataStore}.
     * The pack {@code .zip} binaries themselves are never touched here — only the metadata
     * record (name, hash, url pointing at the on-disk file, assignments, etc.).
     */
    private void loadPacks() {
        Map<String, JsonObject> all = store.getAll(PACKS_COLLECTION);
        for (Map.Entry<String, JsonObject> entry : all.entrySet()) {
            try {
                resourcePacks.put(entry.getKey(), fromJson(entry.getValue()));
            } catch (Exception e) {
                LOGGER.error("Failed to parse resource pack record {}", entry.getKey(), e);
            }
        }
        if (!all.isEmpty()) {
            LOGGER.info("Loaded {} resource packs", all.size());
        }
    }

    /**
     * Persist all current resource pack metadata records to the active DataStore. Icon
     * data is never included (too large — same as the legacy packs.json behavior).
     */
    private void savePacks() {
        try {
            for (Map.Entry<String, ResourcePack> entry : resourcePacks.entrySet()) {
                store.put(PACKS_COLLECTION, entry.getKey(), toJson(entry.getValue()));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save resource packs", e);
        }
    }

    /** Manually build a metadata JsonObject for one pack (icon data is deliberately excluded). */
    private JsonObject toJson(ResourcePack pack) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", pack.getId());
        obj.addProperty("name", pack.getName());
        obj.addProperty("description", pack.getDescription());
        obj.addProperty("fileName", pack.getFileName());
        obj.addProperty("fileHash", pack.getFileHash());
        obj.addProperty("fileSize", pack.getFileSize());
        obj.addProperty("url", pack.getUrl());
        obj.addProperty("external", pack.isExternal());
        obj.addProperty("uploadedAt", pack.getUploadedAt() != null ? pack.getUploadedAt().toString() : null);
        obj.addProperty("uploadedBy", pack.getUploadedBy());
        obj.addProperty("active", pack.isActive());
        obj.addProperty("enforcementMode", pack.getEnforcementMode() != null ? pack.getEnforcementMode().name() : null);

        JsonArray players = new JsonArray();
        for (String p : pack.getAssignedPlayers()) players.add(p);
        obj.add("assignedPlayers", players);

        JsonArray groups = new JsonArray();
        for (String g : pack.getAssignedGroups()) groups.add(g);
        obj.add("assignedGroups", groups);

        ResourcePack.PackMetadata metadata = pack.getMetadata();
        if (metadata != null) {
            JsonObject metaObj = new JsonObject();
            metaObj.addProperty("packFormat", metadata.getPackFormat());
            metaObj.addProperty("description", metadata.getDescription());
            metaObj.add("additionalData", gson.toJsonTree(metadata.getAdditionalData()));
            obj.add("metadata", metaObj);
        }
        return obj;
    }

    /** Reconstruct a ResourcePack from a metadata JsonObject (see {@link #toJson}). */
    private ResourcePack fromJson(JsonObject obj) {
        ResourcePack pack = new ResourcePack();
        if (obj.has("id") && !obj.get("id").isJsonNull()) pack.setId(obj.get("id").getAsString());
        pack.setName(getOrNull(obj, "name"));
        pack.setDescription(getOrNull(obj, "description"));
        pack.setFileName(getOrNull(obj, "fileName"));
        pack.setFileHash(getOrNull(obj, "fileHash"));
        pack.setFileSize(obj.has("fileSize") && !obj.get("fileSize").isJsonNull() ? obj.get("fileSize").getAsLong() : 0L);
        pack.setUrl(getOrNull(obj, "url"));
        pack.setExternal(obj.has("external") && obj.get("external").getAsBoolean());
        String uploadedAt = getOrNull(obj, "uploadedAt");
        pack.setUploadedAt(uploadedAt != null ? Instant.parse(uploadedAt) : Instant.now());
        pack.setUploadedBy(getOrNull(obj, "uploadedBy"));
        pack.setActive(obj.has("active") && obj.get("active").getAsBoolean());
        String enforcement = getOrNull(obj, "enforcementMode");
        pack.setEnforcementMode(enforcement != null
            ? ResourcePack.EnforcementMode.valueOf(enforcement)
            : ResourcePack.EnforcementMode.OPTIONAL);

        Set<String> players = new HashSet<>();
        if (obj.has("assignedPlayers") && obj.get("assignedPlayers").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("assignedPlayers")) players.add(e.getAsString());
        }
        pack.setAssignedPlayers(players);

        Set<String> groups = new HashSet<>();
        if (obj.has("assignedGroups") && obj.get("assignedGroups").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("assignedGroups")) groups.add(e.getAsString());
        }
        pack.setAssignedGroups(groups);

        if (obj.has("metadata") && obj.get("metadata").isJsonObject()) {
            JsonObject metaObj = obj.getAsJsonObject("metadata");
            ResourcePack.PackMetadata metadata = new ResourcePack.PackMetadata();
            if (metaObj.has("packFormat") && !metaObj.get("packFormat").isJsonNull()) {
                metadata.setPackFormat(metaObj.get("packFormat").getAsInt());
            }
            if (metaObj.has("description") && !metaObj.get("description").isJsonNull()) {
                metadata.setDescription(metaObj.get("description").getAsString());
            }
            if (metaObj.has("additionalData") && !metaObj.get("additionalData").isJsonNull()) {
                Map<String, Object> additionalData = gson.fromJson(metaObj.get("additionalData"),
                    new TypeToken<Map<String, Object>>(){}.getType());
                metadata.setAdditionalData(additionalData);
            }
            pack.setMetadata(metadata);
        }
        return pack;
    }

    private static String getOrNull(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    /**
     * One-time import of the legacy packs.json file into the active DataStore, if it's
     * still empty and storage.autoMigrate is enabled. The .zip binaries on disk are left
     * exactly where they are — only the metadata records move.
     */
    private void migrateLegacyFilesIfNeeded() {
        if (store.hasAnyData(PACKS_COLLECTION)) return;
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;
        if (!Files.exists(packsFile)) return;

        try {
            String json = Files.readString(packsFile);
            Map<String, ResourcePack> loaded = gson.fromJson(json, new TypeToken<Map<String, ResourcePack>>(){}.getType());
            if (loaded == null || loaded.isEmpty()) return;

            int count = 0;
            for (Map.Entry<String, ResourcePack> entry : loaded.entrySet()) {
                store.put(PACKS_COLLECTION, entry.getKey(), toJson(entry.getValue()));
                count++;
            }
            LOGGER.info("ResourcePackManager: migrated {} resource pack record(s) from legacy packs.json into the '{}' storage backend.",
                count, com.zerog.neoessentials.storage.StorageManager.getInstance().getActiveType());
        } catch (Exception e) {
            LOGGER.error("Failed to migrate legacy packs.json: {}", e.getMessage(), e);
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
