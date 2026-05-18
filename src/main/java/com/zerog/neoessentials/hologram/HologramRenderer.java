package com.zerog.neoessentials.hologram;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import org.joml.Quaternionf;
import org.joml.Vector3f;
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
 * <h3>Spin / player-tracking</h3>
 * When {@link HologramData#spinEnabled} is {@code true} and
 * {@link HologramData#spinAxis} is {@code "Y"}, the renderer automatically
 * switches the billboard constraint to <b>FIXED</b> and computes the effective
 * Y rotation as:
 * <pre>
 *   total_yaw = yaw_angle_to_nearest_player + current_spin_offset
 * </pre>
 * This means the hologram completes full 360-degree rotations around its
 * vertical axis while the "front face" always tracks the nearest player in
 * the same dimension — so the text remains readable once per revolution and
 * follows whoever walks around it.
 *
 * <p>For X/Z-axis spins the original behaviour is preserved:
 * {@code CENTER} billboard is respected, and the rotation is applied as
 * a simple LEFT_ROTATION quaternion (X = pitch-spin, Z = roll-spin).
 *
 * <p>NeoForge 1.21.1 uses Mojang official mappings at runtime, so reflection
 * by field name is stable between dev and production.
 */
@SuppressWarnings({"unchecked", "UnusedAssignment"})
public final class HologramRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(HologramRenderer.class);
    /** NBT tag placed in persistent data so stale entities can be identified on world reload. */
    private static final String TAG_MARKER = "neoessentials_hologram";
    private static final String TAG_ID     = "neoessentials_hologram_id";
    // -- Reflected accessors for Display.TextDisplay private fields -------------
    private static EntityDataAccessor<Component>   DATA_TEXT;
    private static EntityDataAccessor<Integer>     DATA_BG;
    private static EntityDataAccessor<Byte>        DATA_STYLE;
    private static EntityDataAccessor<Byte>        DATA_TEXT_OPACITY;
    private static EntityDataAccessor<Integer>     DATA_LINE_WIDTH;
    // -- Reflected accessors for Display (base class) ---------------------------
    private static EntityDataAccessor<Byte>        DATA_BILLBOARD;
    private static EntityDataAccessor<Quaternionf> DATA_LEFT_ROTATION;
    private static EntityDataAccessor<Integer>     DATA_INTERP_DURATION;
    private static EntityDataAccessor<Integer>     DATA_INTERP_START_DELTA;
    private static EntityDataAccessor<Vector3f>    DATA_SCALE;
    private static EntityDataAccessor<Float>       DATA_VIEW_RANGE;
    static {
        try {
            DATA_TEXT         = reflectAccessor(Display.TextDisplay.class, "DATA_TEXT_ID");
            DATA_BG           = reflectAccessor(Display.TextDisplay.class, "DATA_BACKGROUND_COLOR_ID");
            DATA_STYLE        = reflectAccessor(Display.TextDisplay.class, "DATA_STYLE_FLAGS_ID");
            DATA_TEXT_OPACITY = reflectAccessor(Display.TextDisplay.class, "DATA_TEXT_OPACITY_ID");
            DATA_LINE_WIDTH   = reflectAccessor(Display.TextDisplay.class, "DATA_LINE_WIDTH_ID");
            LOGGER.debug("[Hologram] Display.TextDisplay data accessors resolved.");
        } catch (Exception e) {
            LOGGER.error("[Hologram] Failed to resolve Display.TextDisplay data accessors.", e);
        }
        try {
            DATA_BILLBOARD          = reflectAccessor(Display.class, "DATA_BILLBOARD_CONSTRAINTS_ID");
            DATA_LEFT_ROTATION      = reflectAccessor(Display.class, "DATA_LEFT_ROTATION_ID");
            DATA_INTERP_DURATION    = reflectAccessor(Display.class, "DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID");
            DATA_INTERP_START_DELTA = reflectAccessor(Display.class, "DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID");
            DATA_SCALE              = reflectAccessor(Display.class, "DATA_SCALE_ID");
            DATA_VIEW_RANGE         = reflectAccessor(Display.class, "DATA_VIEW_RANGE_ID");
            LOGGER.debug("[Hologram] Display base-class data accessors resolved.");
        } catch (Exception e) {
            LOGGER.warn("[Hologram] Failed to resolve Display base-class accessors: {}", e.getMessage());
        }
    }
    private HologramRenderer() {}
    // -- Public API -------------------------------------------------------------
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
        // Seed the player-facing yaw before the first spawn so the text has a
        // sensible orientation immediately (defaults to 0 if no players are present).
        refreshPlayerFacingYaw(data, level);
        for (int i = 0; i < data.lines.size(); i++) {
            HologramLine line = data.lines.get(i);
            double lineY = data.lineYWithHover(i);
            Component text = HologramTextProcessor.processStatic(line.currentText());
            try {
                Display.TextDisplay entity = EntityType.TEXT_DISPLAY.create(level);
                if (entity == null) continue;
                entity.setPos(data.x, lineY, data.z);
                entity.setNoGravity(true);
                entity.setInvulnerable(true);
                entity.setSilent(true);
                entity.getPersistentData().putBoolean(TAG_MARKER, true);
                entity.getPersistentData().putString(TAG_ID, data.id);
                applyText(entity, text, data);
                applyBillboardAndRotation(entity, data);
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
     * Update the rotation (spin) and Y position (hover) for all live entities
     * of the given hologram. Called every animation tick by {@link HologramScheduler}.
     *
     * <p>For Y-axis spin with player tracking, this method re-computes the
     * angle from the hologram to the nearest player before applying the quaternion.
     * This is safe because it is always invoked on the Minecraft server thread.
     */
    public static void updateRotationsAndPositions(HologramData data, ServerLevel level) {
        if (data.entityUUIDs == null || data.entityUUIDs.isEmpty()) return;
        if (data.entityUUIDs.size() != data.lines.size()) {
            spawn(data, level);
            return;
        }
        // For Y-axis spin with player tracking: refresh the heading angle so the
        // front face follows the nearest player through each 360-degree revolution.
        if (isYTrackingSpin(data)) {
            refreshPlayerFacingYaw(data, level);
        }
        for (int i = 0; i < data.lines.size(); i++) {
            try {
                UUID uuid = data.entityUUIDs.get(i);
                net.minecraft.world.entity.Entity raw = level.getEntity(uuid);
                if (!(raw instanceof Display.TextDisplay entity)) {
                    spawn(data, level);
                    return;
                }
                // Hover: update entity Y position
                if (data.hoverEnabled) {
                    entity.setPos(data.x, data.lineYWithHover(i), data.z);
                }
                // Spin: update rotation quaternion
                if (data.spinEnabled && DATA_LEFT_ROTATION != null) {
                    applySpinRotation(entity, data);
                }
            } catch (Exception e) {
                LOGGER.debug("[Hologram] updateRotationsAndPositions error for '{}' line {}: {}",
                        data.id, i, e.getMessage());
            }
        }
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
     * Update a single line's text. Performs a full respawn if the entity is missing.
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
                applyText(td, text, data);
            } else {
                spawn(data, level);
            }
        } catch (Exception e) {
            LOGGER.debug("[Hologram] updateLineText failed for '{}'[{}]: {}", data.id, lineIndex, e.getMessage());
        }
    }
    // -- Internal helpers -------------------------------------------------------
    /**
     * Apply text and visual settings to a TextDisplay via entity data (reflection-backed).
     */
    private static void applyText(Display.TextDisplay entity, Component text, HologramData data) {
        try {
            if (DATA_TEXT  != null) entity.getEntityData().set(DATA_TEXT, text);
            if (DATA_BG    != null) entity.getEntityData().set(DATA_BG,   data.backgroundColorArgb);
            if (DATA_STYLE != null) {
                byte flags = 0x00;
                if (data.textShadow)          flags |= 0x01;
                if (data.seeThrough)          flags |= 0x02;
                if (data.textAlign == 1)      flags |= 0x08;
                else if (data.textAlign == 2) flags |= 0x10;
                entity.getEntityData().set(DATA_STYLE, flags);
            }
            if (DATA_TEXT_OPACITY != null) {
                entity.getEntityData().set(DATA_TEXT_OPACITY, (byte) Math.max(0, Math.min(255, data.textOpacity)));
            }
            if (DATA_LINE_WIDTH != null) {
                entity.getEntityData().set(DATA_LINE_WIDTH, Math.max(1, data.lineWidth));
            }
        } catch (Exception e) {
            LOGGER.debug("[Hologram] applyText error: {}", e.getMessage());
        }
    }
    /**
     * Apply billboard constraint, scale, viewRange, and initial spin rotation.
     * Called once on every (re)spawn.
     *
     * <h4>Billboard override for Y-axis tracking spin</h4>
     * When Y-axis spin with player tracking is active the billboard is forced to
     * <b>FIXED (0)</b>.  CENTER/VERTICAL billboards override the Y component of
     * LEFT_ROTATION automatically, making any Y-axis spin invisible to the client.
     * Using FIXED + LEFT_ROTATION(yaw_to_nearest_player + spin_offset) produces a
     * correct 360-degree rotation whose front face always follows the viewer.
     */
    private static void applyBillboardAndRotation(Display.TextDisplay entity, HologramData data) {
        try {
            if (DATA_BILLBOARD != null) {
                byte mode;
                if (isYTrackingSpin(data)) {
                    // FIXED is required so LEFT_ROTATION is not overridden by the billboard.
                    mode = 0;
                } else {
                    mode = (byte) Math.max(0, Math.min(3, data.billboardMode));
                }
                entity.getEntityData().set(DATA_BILLBOARD, mode);
            }
            if (DATA_SCALE != null) {
                float s = Math.max(0.1f, Math.min(10.0f, data.scale));
                entity.getEntityData().set(DATA_SCALE, new Vector3f(s, s, s));
            }
            if (DATA_VIEW_RANGE != null) {
                entity.getEntityData().set(DATA_VIEW_RANGE, Math.max(0.1f, Math.min(8.0f, data.viewRange)));
            }
            if (data.spinEnabled && DATA_LEFT_ROTATION != null) {
                if (DATA_INTERP_DURATION    != null) entity.getEntityData().set(DATA_INTERP_DURATION, 1);
                if (DATA_INTERP_START_DELTA != null) entity.getEntityData().set(DATA_INTERP_START_DELTA, 0);
                entity.getEntityData().set(DATA_LEFT_ROTATION, buildRotationQuat(data));
            }
        } catch (Exception e) {
            LOGGER.debug("[Hologram] applyBillboardAndRotation error for '{}': {}", data.id, e.getMessage());
        }
    }
    /**
     * Update LEFT_ROTATION for a live entity based on the hologram's current spin state.
     */
    private static void applySpinRotation(Display.TextDisplay entity, HologramData data) {
        try {
            if (DATA_LEFT_ROTATION != null) {
                if (DATA_INTERP_START_DELTA != null) entity.getEntityData().set(DATA_INTERP_START_DELTA, 0);
                entity.getEntityData().set(DATA_LEFT_ROTATION, buildRotationQuat(data));
            }
        } catch (Exception e) {
            LOGGER.debug("[Hologram] applySpinRotation error: {}", e.getMessage());
        }
    }
    /**
     * Build a {@link Quaternionf} for the current spin state.
     *
     * <ul>
     *   <li><b>Y-axis</b> — {@code rotationY(spinPlayerYaw + currentSpinAngle)}.
     *       The spinPlayerYaw component orients the "front face" toward the nearest
     *       player; the currentSpinAngle drives the continuous 360-degree revolution.
     *       Together they produce a spin that follows the viewer.
     *   <li><b>X / Z</b> — simple rotation around the given axis (works with CENTER
     *       billboard for visible pitch or roll effects).
     * </ul>
     */
    private static Quaternionf buildRotationQuat(HologramData data) {
        String axis = data.spinAxis != null ? data.spinAxis.toUpperCase() : "Y";
        if (axis.equals("Y") && data.spinEnabled) {
            // Combined rotation: face-toward-player offset + accumulated spin.
            float playerRad = (float) Math.toRadians(data.spinPlayerYaw);
            float spinRad   = (float) Math.toRadians(data.currentSpinAngle);
            return new Quaternionf().rotationY(playerRad + spinRad);
        }
        float radians = (float) Math.toRadians(data.currentSpinAngle);
        return switch (axis) {
            case "X" -> new Quaternionf().rotationX(radians);
            case "Z" -> new Quaternionf().rotationZ(radians);
            default  -> new Quaternionf().rotationY(radians);
        };
    }
    /**
     * Scan all players in {@code level} and store the yaw angle (degrees) from
     * the hologram's centre to the nearest one in {@link HologramData#spinPlayerYaw}.
     *
     * <p>The angle is computed as {@code atan2(dx, dz)} — this is the rotation
     * around the world Y axis that turns the entity's default south-facing front
     * to point directly at the player.  If no players are present the stored yaw
     * is left unchanged (retaining the last known good value, or 0° on first spawn).
     */
    private static void refreshPlayerFacingYaw(HologramData data, ServerLevel level) {
        double minDist = Double.MAX_VALUE;
        for (ServerPlayer player : level.players()) {
            double dist = data.distanceXZ(player.getX(), player.getZ());
            if (dist < minDist) {
                minDist = dist;
                double dx = player.getX() - data.x;
                double dz = player.getZ() - data.z;
                // atan2(dx, dz): clockwise angle from +Z (the entity's default front/south)
                // to the hologram-to-player direction vector when viewed from above.
                data.spinPlayerYaw = (float) Math.toDegrees(Math.atan2(dx, dz));
            }
        }
    }
    /**
     * Returns {@code true} when Y-axis tracking spin is active:
     * spin is enabled, axis is "Y", and player tracking is enabled.
     */
    private static boolean isYTrackingSpin(HologramData data) {
        return data.spinEnabled
                && "Y".equalsIgnoreCase(data.spinAxis != null ? data.spinAxis : "Y")
                && data.spinTrackPlayer;
    }
    /**
     * Remove orphaned TextDisplay entities (tagged by us) that survived a crash/restart.
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
                LOGGER.debug("[Hologram] Cleaned {} stale entity(ies) from '{}'.",
                        stale.size(), dimensionKey(level));
            }
        } catch (Exception e) {
            LOGGER.debug("[Hologram] cleanStaleEntities error: {}", e.getMessage());
        }
    }
    // -- Reflection -------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private static <T> EntityDataAccessor<T> reflectAccessor(Class<?> cls, String fieldName) throws Exception {
        Field f = cls.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (EntityDataAccessor<T>) f.get(null);
    }
}
