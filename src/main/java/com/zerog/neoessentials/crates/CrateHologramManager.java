package com.zerog.neoessentials.crates;

import com.zerog.neoessentials.hologram.HologramData;
import com.zerog.neoessentials.hologram.HologramLine;
import com.zerog.neoessentials.hologram.HologramManager;
import com.zerog.neoessentials.hologram.HologramRenderer;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Auto-creates a hologram above a physical crate block when it's registered via
 * {@code /crate admin setblock}, and removes it again on {@code /crate admin removeblock}.
 *
 * <p>The hologram it creates is a completely ordinary {@link HologramData} entry — same
 * registry, same file, same rendering — it's just given a predictable id
 * ({@link #crateHologramId}) so an admin can immediately customize it further with any
 * existing {@code /hologram} subcommand (setline, scale, spin, background, etc.) exactly like
 * a hand-made one. This class only owns the "auto-create/auto-remove alongside the block"
 * lifecycle, not the hologram's actual appearance beyond a sensible starting default.
 */
public final class CrateHologramManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrateHologramManager.class);
    /** Prefix for every crate-auto-hologram, so they're identifiable and independently listable. */
    public static final String CRATE_HOLOGRAM_PREFIX = "crate_";
    /** Default vertical offset (blocks) above the block's base — centered in the block, +text height. */
    private static final double DEFAULT_Y_OFFSET = 1.5;

    private CrateHologramManager() {}

    /** Creates (or replaces, if one already exists at this position) a hologram above the crate
     *  block. Safe to call repeatedly — re-running {@code /crate admin setblock} on the same spot
     *  for a different crate just retargets the existing hologram's text. */
    public static void createOrUpdateCrateHologram(CrateDefinition crate, ServerLevel level, BlockPos pos) {
        if (!com.zerog.neoessentials.config.ConfigManager.isHologramModuleEnabled()) return;
        try {
            String id = crateHologramId(level, pos);
            String dimKey = HologramRenderer.dimensionKey(level);

            HologramData existing = HologramManager.getInstance().getHologram(id);
            HologramData data = existing != null ? existing : new HologramData();
            data.id = id;
            data.world = dimKey;
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

    /** Removes the hologram above a crate block, if one exists. Safe to call even if there
     *  never was one (e.g. it was manually deleted via {@code /hologram delete}). */
    public static void deleteCrateHologram(ServerLevel level, BlockPos pos) {
        try {
            String id = crateHologramId(level, pos);
            HologramData data = HologramManager.getInstance().getHologram(id);
            if (data == null) return;
            HologramRenderer.despawn(data, level);
            HologramManager.getInstance().removeHologram(id);
            NeoLog.debug(LOGGER, LogCategory.CRATES, "Removed crate hologram '{}' at {}", id, pos);
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.CRATES, "Failed to remove crate hologram at {}", pos, e);
        }
    }

    /** Removes every crate-auto-hologram that no longer has a matching physical crate block —
     *  call after {@code /crate admin reload} or a crate/block deletion outside the normal
     *  removeblock path, mirroring {@code ShopHologramManager#cleanOrphanedShopHolograms}. */
    public static void cleanOrphanedCrateHolograms() {
        try {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;

            List<HologramData> orphans = new ArrayList<>();
            for (HologramData holo : HologramManager.getInstance().getAllHolograms()) {
                if (!holo.id.startsWith(CRATE_HOLOGRAM_PREFIX)) continue;
                ServerLevel level = findLevel(server, holo.world);
                if (level == null) continue;
                if (CrateManager.getInstance().getCrateAt(level, holo.blockPos()) == null) {
                    orphans.add(holo);
                }
            }
            for (HologramData orphan : orphans) {
                ServerLevel level = findLevel(server, orphan.world);
                if (level != null) HologramRenderer.despawn(orphan, level);
                HologramManager.getInstance().removeHologram(orphan.id);
            }
            if (!orphans.isEmpty()) {
                NeoLog.info(LOGGER, LogCategory.CRATES, "Cleaned {} orphaned crate hologram(s).", orphans.size());
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

    private static String crateHologramId(ServerLevel level, BlockPos pos) {
        String raw = CRATE_HOLOGRAM_PREFIX + HologramRenderer.dimensionKey(level) + "_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
        return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }

    private static ServerLevel findLevel(net.minecraft.server.MinecraftServer server, String dimensionKey) {
        for (ServerLevel level : server.getAllLevels()) {
            if (HologramRenderer.dimensionKey(level).equals(dimensionKey)) return level;
        }
        return null;
    }
}
