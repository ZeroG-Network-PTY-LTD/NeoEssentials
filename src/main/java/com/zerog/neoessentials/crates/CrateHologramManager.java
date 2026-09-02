package com.zerog.neoessentials.crates;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.hologram.HologramData;
import com.zerog.neoessentials.hologram.HologramLine;
import com.zerog.neoessentials.hologram.HologramManager;
import com.zerog.neoessentials.hologram.HologramRenderer;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.storage.DataStore;
import com.zerog.neoessentials.storage.StorageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Auto-creates a hologram above a physical crate block when it's registered via
 * {@code /crate admin setblock}, and removes it again on {@code /crate admin removeblock}.
 *
 * <p>The hologram it creates is a completely ordinary {@link HologramData} entry — same
 * registry, same file, same rendering — it's just given an id derived from the <em>crate's
 * own name</em> (e.g. {@code crate_common}, disambiguated to {@code crate_common_2} etc. if
 * that crate has more than one physical block) rather than the block's coordinates, so it's
 * actually findable in {@code /hologram} tab-completion instead of being an opaque
 * {@code crate_minecraft_overworld_142_64_-38}-style string. This class only owns the
 * "auto-create/auto-remove alongside the block" lifecycle, not the hologram's actual
 * appearance beyond a sensible starting default — any existing {@code /hologram} subcommand
 * (setline, scale, spin, background, etc.) customizes it further exactly like a hand-made one.
 *
 * <p>Because the id is no longer derived from the block's position, the block → hologram
 * link has to be tracked explicitly (in the {@value #BLOCK_LINK_COLLECTION} collection) rather
 * than recomputed on demand — this also means the link survives the hologram being moved away
 * from its spawn position via {@code /hologram moveto}/{@code movehere}, unlike a purely
 * position-derived id would.
 */
public final class CrateHologramManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrateHologramManager.class);
    /** Prefix for every crate-auto-hologram, so they're identifiable and independently listable. */
    public static final String CRATE_HOLOGRAM_PREFIX = "crate_";
    /** Default vertical offset (blocks) above the block's base — centered in the block, +text height. */
    private static final double DEFAULT_Y_OFFSET = 1.5;
    /** {@code "<dim>@<x>,<y>,<z>" -> {"hologramId": "..."}} — same key shape as
     *  {@code CrateManager}'s own {@code crate_blocks} collection, kept separate since it's a
     *  different concern (which hologram belongs to a block, not which crate does). */
    private static final String BLOCK_LINK_COLLECTION = "crate_block_holograms";

    private CrateHologramManager() {}

    /** Creates (or updates, if this exact block already has one linked) a hologram above the
     *  crate block. Safe to call repeatedly — re-running {@code /crate admin setblock} on the
     *  same spot for a different crate just retargets the existing hologram's text; it never
     *  mints a second hologram for the same block. */
    public static void createOrUpdateCrateHologram(CrateDefinition crate, ServerLevel level, BlockPos pos) {
        if (!com.zerog.neoessentials.config.ConfigManager.isHologramModuleEnabled()) return;
        try {
            String blockKey = blockKey(level, pos);
            String id = linkedHologramId(blockKey);

            HologramData existing = id != null ? HologramManager.getInstance().getHologram(id) : null;
            if (existing == null) {
                // No hologram linked to this block yet (first setblock here, or the linked one
                // was deleted out from under us) — mint a fresh, crate-name-based id and link it.
                id = mintHologramId(crate.id);
                linkBlockToHologram(blockKey, id);
            }

            HologramData data = existing != null ? existing : new HologramData();
            data.id = id;
            data.world = HologramRenderer.dimensionKey(level);
            data.x = pos.getX() + 0.5;
            data.y = pos.getY() + DEFAULT_Y_OFFSET;
            data.z = pos.getZ() + 0.5;
            if (existing == null) {
                data.entityUUIDs = new ArrayList<>();
            }
            data.lines = defaultLines(crate);
            HologramManager.getInstance().registerHologram(data);
            HologramRenderer.spawn(data, level);
            NeoLog.debug(LOGGER, LogCategory.CRATES, "{} crate hologram '{}' for crate '{}' at {}",
                existing == null ? "Created" : "Updated", id, crate.id, pos);
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.CRATES, "Failed to create/update crate hologram at {}", pos, e);
        }
    }

    /** Removes the hologram linked to a crate block, if one exists. Safe to call even if there
     *  never was one (e.g. it was manually deleted via {@code /hologram delete}). */
    public static void deleteCrateHologram(ServerLevel level, BlockPos pos) {
        try {
            String blockKey = blockKey(level, pos);
            String id = linkedHologramId(blockKey);
            if (id != null) {
                HologramData data = HologramManager.getInstance().getHologram(id);
                if (data != null) {
                    HologramRenderer.despawn(data, level);
                    HologramManager.getInstance().removeHologram(id);
                }
                NeoLog.debug(LOGGER, LogCategory.CRATES, "Removed crate hologram '{}' at {}", id, pos);
            }
            StorageManager.getInstance().getStore().delete(BLOCK_LINK_COLLECTION, blockKey);
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.CRATES, "Failed to remove crate hologram at {}", pos, e);
        }
    }

    /** Removes every crate hologram (and its block link) whose linked block no longer has a
     *  valid crate assigned — call after {@code /crate admin reload} or a crate deletion,
     *  mirroring {@code ShopHologramManager#cleanOrphanedShopHolograms}. Walks the block-link
     *  collection rather than the holograms themselves, so a hologram that's been moved via
     *  {@code /hologram moveto} away from its block still resolves correctly. */
    public static void cleanOrphanedCrateHolograms() {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            DataStore store = StorageManager.getInstance().getStore();

            List<String> orphanKeys = new ArrayList<>();
            List<String> orphanHologramIds = new ArrayList<>();
            for (Map.Entry<String, JsonObject> entry : store.getAll(BLOCK_LINK_COLLECTION).entrySet()) {
                String blockKey = entry.getKey();
                JsonObject record = entry.getValue();
                String hologramId = record != null && record.has("hologramId") ? record.get("hologramId").getAsString() : null;

                BlockRef ref = parseBlockKey(server, blockKey);
                if (ref == null || ref.level == null || CrateManager.getInstance().getCrateAt(ref.level, ref.pos) == null) {
                    orphanKeys.add(blockKey);
                    if (hologramId != null) orphanHologramIds.add(hologramId);
                }
            }

            for (String hologramId : orphanHologramIds) {
                HologramData data = HologramManager.getInstance().getHologram(hologramId);
                if (data == null) continue;
                ServerLevel level = findLevel(server, data.world);
                if (level != null) HologramRenderer.despawn(data, level);
                HologramManager.getInstance().removeHologram(hologramId);
            }
            for (String blockKey : orphanKeys) {
                store.delete(BLOCK_LINK_COLLECTION, blockKey);
            }
            if (!orphanKeys.isEmpty()) {
                NeoLog.info(LOGGER, LogCategory.CRATES, "Cleaned {} orphaned crate hologram(s).", orphanKeys.size());
            }
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.CRATES, "Failed to clean orphaned crate holograms", e);
        }
    }

    private static List<HologramLine> defaultLines(CrateDefinition crate) {
        List<HologramLine> lines = new ArrayList<>();
        lines.add(new HologramLine(crate.displayName));
        lines.add(new HologramLine("&7Right-click to open!"));
        return lines;
    }

    // ── Block <-> hologram-id link ──────────────────────────────────────────────

    private static String blockKey(ServerLevel level, BlockPos pos) {
        return HologramRenderer.dimensionKey(level) + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static String linkedHologramId(String blockKey) {
        JsonObject record = StorageManager.getInstance().getStore().get(BLOCK_LINK_COLLECTION, blockKey);
        return record != null && record.has("hologramId") ? record.get("hologramId").getAsString() : null;
    }

    private static void linkBlockToHologram(String blockKey, String hologramId) {
        JsonObject record = new JsonObject();
        record.addProperty("hologramId", hologramId);
        StorageManager.getInstance().getStore().put(BLOCK_LINK_COLLECTION, blockKey, record);
    }

    /** {@code crate_<crateId>}, disambiguated with a numeric suffix if that crate already has
     *  another physical block's hologram using the base id (e.g. {@code crate_common_2}). */
    private static String mintHologramId(String crateId) {
        String base = (CRATE_HOLOGRAM_PREFIX + crateId).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        String id = base;
        int suffix = 2;
        while (HologramManager.getInstance().exists(id)) {
            id = base + "_" + suffix++;
        }
        return id;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private record BlockRef(ServerLevel level, BlockPos pos) {}

    /** Parses a {@code "<dim>@<x>,<y>,<z>"} key back into a level/position, or {@code null} if
     *  the dimension is no longer loaded or the key is malformed. */
    private static BlockRef parseBlockKey(MinecraftServer server, String blockKey) {
        try {
            int at = blockKey.indexOf('@');
            if (at < 0) return null;
            String dimKey = blockKey.substring(0, at);
            String[] coords = blockKey.substring(at + 1).split(",");
            if (coords.length != 3) return null;
            BlockPos pos = new BlockPos(
                Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]));
            return new BlockRef(findLevel(server, dimKey), pos);
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.CRATES, "Failed to parse crate hologram block key '{}'", blockKey, e);
            return null;
        }
    }

    private static ServerLevel findLevel(MinecraftServer server, String dimensionKey) {
        for (ServerLevel level : server.getAllLevels()) {
            if (HologramRenderer.dimensionKey(level).equals(dimensionKey)) return level;
        }
        return null;
    }
}
