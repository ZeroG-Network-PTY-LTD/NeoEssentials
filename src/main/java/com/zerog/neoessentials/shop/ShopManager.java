package com.zerog.neoessentials.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.shop.model.ShopData;
import com.zerog.neoessentials.util.ResourceUtil;
import net.minecraft.core.BlockPos;
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
import java.util.stream.Collectors;

/**
 * Singleton manager that holds all active {@link ShopData} entries in memory
 * and persists them to {@code neoessentials/shops.json}.
 * <p>
 * Key = {@code "<dimension>@<x>,<y>,<z>"} (the sign position).
 */
public class ShopManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShopManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static ShopManager instance;
    public static ShopManager getInstance() {
        if (instance == null) instance = new ShopManager();
        return instance;
    }
    private ShopManager() {}

    // ── State ─────────────────────────────────────────────────────────────────

    /** sign position key → shop */
    private final ConcurrentHashMap<String, ShopData> shopsBySign  = new ConcurrentHashMap<>();
    /** chest position key → shop (for chest-break detection) */
    private final ConcurrentHashMap<String, ShopData> shopsByChest = new ConcurrentHashMap<>();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void initialize() {
        try {
            load();
            LOGGER.info("[ChestShop] Loaded {} shop(s) from shops.json", shopsBySign.size());
        } catch (Exception e) {
            LOGGER.error("[ChestShop] Failed to load shops.json — shops disabled until reload", e);
        }
    }

    public void shutdown() {
        try {
            save();
            LOGGER.info("[ChestShop] shops.json saved on shutdown.");
        } catch (Exception e) {
            LOGGER.error("[ChestShop] Failed to save shops.json on shutdown", e);
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    /** Register (or replace) a shop. Also indexes by chest position. */
    public void registerShop(ShopData shop) {
        shopsBySign.put(shop.toKey(), shop);
        if (shop.hasChest) {
            String chestKey = shop.chestDimension + "@"
                + shop.chestX + "," + shop.chestY + "," + shop.chestZ;
            shopsByChest.put(chestKey, shop);
        }
        trySave();
        // Auto-create / update hologram above this shop sign
        try {
            com.zerog.neoessentials.hologram.integration.ShopHologramManager
                .createShopHologram(shop, shop.signDimension);
        } catch (Exception ignored) {}
    }

    /** Remove by sign position. Returns the removed shop or null. */
    public ShopData removeShop(String dimension, BlockPos signPos) {
        String key = dimension + "@" + signPos.getX() + "," + signPos.getY() + "," + signPos.getZ();
        ShopData removed = shopsBySign.remove(key);
        if (removed != null && removed.hasChest) {
            String ck = removed.chestDimension + "@"
                + removed.chestX + "," + removed.chestY + "," + removed.chestZ;
            shopsByChest.remove(ck);
        }
        if (removed != null) {
            trySave();
            // Remove associated hologram
            try {
                com.zerog.neoessentials.hologram.integration.ShopHologramManager
                    .deleteShopHologram(removed);
            } catch (Exception ignored) {}
        }
        return removed;
    }

    /** Remove all shops owned by the given UUID. */
    public int removeShopsByOwner(UUID ownerUUID) {
        List<ShopData> toRemove = shopsBySign.values().stream()
            .filter(s -> ownerUUID.equals(s.ownerUUID))
            .collect(Collectors.toList());
        toRemove.forEach(s -> removeShop(s.signDimension, s.getSignPos()));
        return toRemove.size();
    }

    /** Look up a shop by the sign block position. */
    public ShopData getShopBySign(String dimension, BlockPos pos) {
        return shopsBySign.get(dimension + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ());
    }

    /** Look up a shop by its linked chest position. */
    public ShopData getShopByChest(String dimension, BlockPos pos) {
        return shopsByChest.get(dimension + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ());
    }

    /**
     * Look up a shop directly by its sign-position key (format: {@code "dim@x,y,z"}).
     * Useful when the key has been stored in NBT and needs to be resolved back to a shop.
     */
    public ShopData getShopByKey(String key) {
        return shopsBySign.get(key);
    }

    /** All shops owned by a given UUID. */
    public List<ShopData> getShopsByOwner(UUID ownerUUID) {
        return shopsBySign.values().stream()
            .filter(s -> ownerUUID.equals(s.ownerUUID))
            .collect(Collectors.toList());
    }

    /** Snapshot of all shops (defensive copy). */
    public Collection<ShopData> getAllShops() {
        return Collections.unmodifiableCollection(shopsBySign.values());
    }

    public int getShopCount() { return shopsBySign.size(); }

    // ── Persistence ───────────────────────────────────────────────────────────

    private Path getDataFile() {
        return ResourceUtil.getDataPath("shops.json");
    }

    private void load() throws IOException {
        Path file = getDataFile();
        if (!Files.exists(file)) return;
        try (Reader r = Files.newBufferedReader(file)) {
            Type listType = new TypeToken<List<ShopData>>(){}.getType();
            List<ShopData> shops = GSON.fromJson(r, listType);
            if (shops != null) {
                for (ShopData s : shops) {
                    shopsBySign.put(s.toKey(), s);
                    if (s.hasChest) {
                        String ck = s.chestDimension + "@"
                            + s.chestX + "," + s.chestY + "," + s.chestZ;
                        shopsByChest.put(ck, s);
                    }
                }
            }
        }
    }

    private void save() throws IOException {
        Path file = getDataFile();
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer w = Files.newBufferedWriter(tmp)) {
            GSON.toJson(new ArrayList<>(shopsBySign.values()), w);
        }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /** Best-effort save (logs on failure, never throws). */
    private void trySave() {
        try { save(); }
        catch (Exception e) { LOGGER.error("[ChestShop] Failed to save shops.json", e); }
    }

    /** Reload from disk — call on /chestshop reload. */
    public void reload() {
        shopsBySign.clear();
        shopsByChest.clear();
        try {
            load();
            LOGGER.info("[ChestShop] Reloaded {} shop(s).", shopsBySign.size());
        } catch (Exception e) {
            LOGGER.error("[ChestShop] Reload failed", e);
        }
    }
}

