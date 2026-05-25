package com.zerog.neoessentials.shop.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton manager for NPC shop persistence.
 * Stores data in {@code neoessentials/npc_shops.json}.
 */
public class ShopEntityManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShopEntityManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final ShopEntityManager INSTANCE = new ShopEntityManager();
    public static ShopEntityManager getInstance() { return INSTANCE; }
    private ShopEntityManager() {}

    /** shopId → ShopEntityData */
    private final ConcurrentHashMap<UUID, ShopEntityData> byShopId   = new ConcurrentHashMap<>();
    /** entityUUID → ShopEntityData */
    private final ConcurrentHashMap<UUID, ShopEntityData> byEntityId = new ConcurrentHashMap<>();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void initialize() {
        try {
            load();
            LOGGER.info("[NpcShop] Loaded {} NPC shop(s).", byShopId.size());
        } catch (Exception e) {
            LOGGER.error("[NpcShop] Failed to load npc_shops.json", e);
        }
    }

    public void shutdown() {
        try { save(); } catch (Exception e) { LOGGER.error("[NpcShop] Failed to save on shutdown", e); }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public void register(ShopEntityData data) {
        byShopId.put(data.shopId, data);
        if (data.entityUUID != null) byEntityId.put(data.entityUUID, data);
        trySave();
    }

    public boolean remove(UUID shopId) {
        ShopEntityData data = byShopId.remove(shopId);
        if (data == null) return false;
        if (data.entityUUID != null) byEntityId.remove(data.entityUUID);
        trySave();
        return true;
    }

    public ShopEntityData getByShopId(UUID shopId)   { return byShopId.get(shopId);    }
    public ShopEntityData getByShopId(String shopId) {
        try { return byShopId.get(UUID.fromString(shopId)); } catch (Exception e) { return null; }
    }
    public ShopEntityData getByEntityUUID(UUID entityUUID) { return byEntityId.get(entityUUID); }

    public Collection<ShopEntityData> getAll() { return Collections.unmodifiableCollection(byShopId.values()); }

    public List<ShopEntityData> getByOwner(UUID ownerUUID) {
        return byShopId.values().stream()
                .filter(d -> ownerUUID.equals(d.ownerUUID))
                .toList();
    }

    public int getShopCount() { return byShopId.size(); }

    /** Update entity UUID when an entity is spawned/loaded. */
    public void updateEntityUUID(UUID shopId, UUID newEntityUUID) {
        ShopEntityData data = byShopId.get(shopId);
        if (data == null) return;
        if (data.entityUUID != null) byEntityId.remove(data.entityUUID);
        data.entityUUID = newEntityUUID;
        byEntityId.put(newEntityUUID, data);
        trySave();
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private Path getDataFile() { return ResourceUtil.getConfigPath("npc_shops.json"); }

    private void load() throws IOException {
        Path file = getDataFile();
        if (!Files.exists(file)) return;
        try (Reader r = Files.newBufferedReader(file)) {
            Type listType = new TypeToken<List<ShopEntityData>>(){}.getType();
            List<ShopEntityData> shops = GSON.fromJson(r, listType);
            if (shops != null) {
                for (ShopEntityData d : shops) {
                    if (d.shopId == null) d.shopId = UUID.randomUUID();
                    byShopId.put(d.shopId, d);
                    if (d.entityUUID != null) byEntityId.put(d.entityUUID, d);
                }
            }
        }
    }

    private void save() throws IOException {
        Path file = getDataFile();
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer w = Files.newBufferedWriter(tmp)) {
            GSON.toJson(new ArrayList<>(byShopId.values()), w);
        }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private void trySave() {
        try { save(); } catch (Exception e) { LOGGER.error("[NpcShop] Failed to save npc_shops.json", e); }
    }

    public void reload() {
        byShopId.clear(); byEntityId.clear();
        try { load(); LOGGER.info("[NpcShop] Reloaded {} shop(s).", byShopId.size()); }
        catch (Exception e) { LOGGER.error("[NpcShop] Reload failed", e); }
    }
}

