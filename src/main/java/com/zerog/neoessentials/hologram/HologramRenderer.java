package com.zerog.neoessentials.hologram;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles spawning, despawning, and updating {@link Display.TextDisplay} entities
 * for holographic displays.
 *
 * <p>NeoForge 1.21.1 uses Mojang official mappings at runtime, so reflection by
 * field name is stable between dev and production.
 */
@SuppressWarnings({"unchecked", "UnusedAssignment"})
public final class HologramRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(HologramRenderer.class);

    /** NBT tag placed in persistent data so stale entities can be identified on world reload. */
    private static final String TAG_MARKER = "neoessentials_hologram";
    private static final String TAG_ID     = "neoessentials_hologram_id";

    // ── Reflected accessors for Display.TextDisplay private fields ─────────────
    // NeoForge 1.21.1 runs with Mojang official names at runtime → field names are stable.

    private static EntityDataAccessor<Component> DATA_TEXT;
    private static EntityDataAccessor<Integer>   DATA_BG;
    private static EntityDataAccessor<Byte>      DATA_STYLE;

    static {
        try {
            DATA_TEXT  = reflectAccessor(Display.TextDisplay.class, "DATA_TEXT_ID");
            DATA_BG    = reflectAccessor(Display.TextDisplay.class, "DATA_BACKGROUND_COLOR_ID");
            DATA_STYLE = reflectAccessor(Display.TextDisplay.class, "DATA_STYLE_FLAGS_ID");
            LOGGER.debug("[Hologram] Display.TextDisplay data accessors resolved.");
        } catch (Exception e) {
            LOGGER.error("[Hologram] Failed to resolve Display.TextDisplay data accessors — holograms may not render text.", e);
        }
    }

    private HologramRenderer() {}

    // ── Public API ─────────────────────────────────────────────────────────────

    /** Returns the dimension key string (e.g. {@code "minecraft:overworld"}). */
    public static String dimensionKey(ServerLevel level) {
        return level.dimension().location().toString();
    }

    /**
     * Spawn all {@link Display.TextDisplay} entities for the hologram.
     * Removes any previously tracked entities first to avoid duplicates.
     */
    public static void spawn(HologramData data, ServerLevel level) {
        despawn(data, level);
        if (!data.visible || data.lines.isEmpty()) return;

        if (data.entityUUIDs == null) data.entityUUIDs = new ArrayList<>();

        for (int i = 0; i < data.lines.size(); i++) {
            HologramLine line = data.lines.get(i);
            double lineY = data.lineY(i);
            Component text = HologramTextProcessor.processStatic(line.currentText());

            try {
                Display.TextDisplay entity = EntityType.TEXT_DISPLAY.create(level);
                if (entity == null) continue;

                entity.setPos(data.x, lineY, data.z);
                entity.setNoGravity(true);
                entity.setInvulnerable(true);
                entity.setSilent(true);

                // Tag so we can identify and trim stale copies on world reload
                entity.getPersistentData().putBoolean(TAG_MARKER, true);
                entity.getPersistentData().putString(TAG_ID, data.id);

                applyText(entity, text);

                level.addFreshEntity(entity);
                data.entityUUIDs.add(entity.getUUID());
            } catch (Exception e) {
                LOGGER.error("[Hologram] Failed to spawn line {} of '{}': {}", i, data.id, e.getMessage(), e);
            }
        }

        data.lastRefreshMs = System.currentTimeMillis();
        LOGGER.debug("[Hologram] Spawned {} line entity(ies) for '{}'.", data.entityUUIDs.size(), data.id);
    }

    /**
     * Discard all tracked TextDisplay entities and reset the UUID list.
     */
    public static void despawn(HologramData data, ServerLevel level) {
        if (data.entityUUIDs == null) { data.entityUUIDs = new ArrayList<>(); return; }
        for (UUID uuid : new ArrayList<>(data.entityUUIDs)) {
            try {
                net.minecraft.world.entity.Entity e = level.getEntity(uuid);
                if (e != null) e.discard();
            } catch (Exception ignored) {}
        }
        data.entityUUIDs.clear();
    }

    /**
     * Spawn all holograms in the given dimension.
     * Orphaned entities saved to world NBT are cleaned up first.
     */
    public static void spawnAllForWorld(ServerLevel level, String dimKey) {
        cleanStaleEntities(level);
        for (HologramData d : HologramManager.getInstance().getHologramsForWorld(dimKey)) {
            spawn(d, level);
        }
    }

    /**
     * Despawn all holograms in the given dimension.
     */
    public static void despawnAllForWorld(ServerLevel level, String dimKey) {
        for (HologramData d : HologramManager.getInstance().getHologramsForWorld(dimKey)) {
            despawn(d, level);
        }
    }

    /**
     * Refresh all line texts (called by the scheduler for placeholder updates).
     *
     * @param player optional player for per-player placeholder context; may be {@code null}
     */
    public static void refreshAllLines(HologramData data, ServerLevel level, @Nullable ServerPlayer player) {
        if (data.entityUUIDs == null || data.entityUUIDs.size() != data.lines.size()) {
            spawn(data, level);
            return;
        }
        for (int i = 0; i < data.lines.size(); i++) {
            Component text = HologramTextProcessor.process(data.lines.get(i).currentText(), player);
            updateLineText(data, i, text, level);
        }
        data.lastRefreshMs = System.currentTimeMillis();
    }

    /**
     * Update a single line's text.  Performs a full respawn if the entity is missing.
     */
    public static void updateLineText(HologramData data, int lineIndex, Component text, ServerLevel level) {
        if (data.entityUUIDs == null || lineIndex >= data.entityUUIDs.size()) {
            spawn(data, level);
            return;
        }
        try {
            UUID uuid = data.entityUUIDs.get(lineIndex);
            net.minecraft.world.entity.Entity entity = level.getEntity(uuid);
            if (entity instanceof Display.TextDisplay td) {
                applyText(td, text);
            } else {
                spawn(data, level); // entity gone — full respawn
            }
        } catch (Exception e) {
            LOGGER.debug("[Hologram] updateLineText failed for '{}'[{}]: {}", data.id, lineIndex, e.getMessage());
        }
    }

    // ── Internal helpers ───────────────────────────────────────────────────────

    /**
     * Apply text and visual settings to a TextDisplay via entity data (reflection-backed).
     */
    private static void applyText(Display.TextDisplay entity, Component text) {
        try {
            if (DATA_TEXT  != null) entity.getEntityData().set(DATA_TEXT,  text);
            if (DATA_BG    != null) entity.getEntityData().set(DATA_BG,    0x00000000); // fully transparent
            if (DATA_STYLE != null) entity.getEntityData().set(DATA_STYLE, (byte) 0x08); // centre-aligned
        } catch (Exception e) {
            LOGGER.debug("[Hologram] applyText error: {}", e.getMessage());
        }
    }

    /**
     * Remove orphaned TextDisplay entities (tagged by us) that survived a crash / server restart.
     * Called once per level load before spawning fresh entities.
     */
    private static void cleanStaleEntities(ServerLevel level) {
        try {
            List<net.minecraft.world.entity.Entity> stale = new ArrayList<>();
            level.getAllEntities().forEach(entity -> {
                if (entity instanceof Display.TextDisplay
                        && entity.getPersistentData().contains(TAG_MARKER)) {
                    stale.add(entity);
                }
            });
            for (var e : stale) e.discard();
            if (!stale.isEmpty()) {
                LOGGER.debug("[Hologram] Cleaned {} stale entity(ies) from '{}'.", stale.size(), dimensionKey(level));
            }
        } catch (Exception e) {
            LOGGER.debug("[Hologram] cleanStaleEntities error: {}", e.getMessage());
        }
    }

    // ── Reflection ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static <T> EntityDataAccessor<T> reflectAccessor(Class<?> cls, String fieldName) throws Exception {
        Field f = cls.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (EntityDataAccessor<T>) f.get(null);
    }
}
