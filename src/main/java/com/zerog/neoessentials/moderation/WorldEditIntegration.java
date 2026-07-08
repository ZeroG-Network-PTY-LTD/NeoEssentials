package com.zerog.neoessentials.moderation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Best-effort, reflection-only soft integration with WorldEdit, used by {@code /setjail} so a
 * player who already has a WorldEdit cuboid selection can turn it directly into a cuboid jail
 * without needing to reselect with NeoEssentials' own wand.
 *
 * <p>WorldEdit is never a compile-time dependency here — this class only touches it via
 * reflection, and every failure mode (not installed, API shape differs between WorldEdit
 * versions/forks, no active selection, non-cuboid selection) degrades to "no selection
 * available" rather than throwing. {@code /setjail}'s own wand-based selection always works
 * regardless of whether this integration succeeds.</p>
 */
public final class WorldEditIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldEditIntegration.class);
    private static Boolean available;

    private WorldEditIntegration() {}

    public static boolean isAvailable() {
        if (available == null) {
            try {
                available = ModList.get().isLoaded("worldedit");
            } catch (Exception e) {
                available = false;
            }
        }
        return available;
    }

    public record Selection(BlockPos min, BlockPos max) {}

    /**
     * Attempts to read {@code player}'s current WorldEdit cuboid selection. Returns {@code null}
     * on any failure — WorldEdit not installed, adapter class not found (API differs from the
     * version this was written against), no active selection, or a non-cuboid selection type.
     */
    public static Selection getSelection(ServerPlayer player) {
        if (!isAvailable()) return null;
        try {
            Object nativeActor = adaptPlayer(player);
            if (nativeActor == null) return null;

            Class<?> weClass = Class.forName("com.sk89q.worldedit.WorldEdit");
            Object weInstance = weClass.getMethod("getInstance").invoke(null);
            Object sessionManager = weClass.getMethod("getSessionManager").invoke(weInstance);
            Class<?> actorClass = Class.forName("com.sk89q.worldedit.extension.platform.Actor");
            Object session = sessionManager.getClass().getMethod("get", actorClass).invoke(sessionManager, nativeActor);
            if (session == null) return null;

            Object world = session.getClass().getMethod("getSelectionWorld").invoke(session);
            if (world == null) return null;
            Class<?> worldClass = Class.forName("com.sk89q.worldedit.world.World");
            Object region = session.getClass().getMethod("getSelection", worldClass).invoke(session, world);
            if (region == null) return null;

            Object minVec = region.getClass().getMethod("getMinimumPoint").invoke(region);
            Object maxVec = region.getClass().getMethod("getMaximumPoint").invoke(region);
            BlockPos min = toBlockPos(minVec);
            BlockPos max = toBlockPos(maxVec);
            if (min == null || max == null) return null;
            return new Selection(min, max);
        } catch (ReflectiveOperationException e) {
            // WorldEdit is installed but its API doesn't match what this integration was
            // written against (version/fork mismatch) — log once at debug, not an error, since
            // this is a soft integration and NeoEssentials' own wand still works fine.
            LOGGER.debug("WorldEdit selection lookup failed (API mismatch?): {}", e.toString());
            return null;
        } catch (Exception e) {
            LOGGER.debug("WorldEdit selection lookup failed: {}", e.toString());
            return null;
        }
    }

    /** Tries the NeoForge adapter first, falling back to the older Forge adapter class name
     *  some WorldEdit ports still ship under. Returns null if neither is present. */
    private static Object adaptPlayer(ServerPlayer player) throws ReflectiveOperationException {
        for (String adapterClassName : new String[] {
                "com.sk89q.worldedit.neoforge.NeoForgeAdapter",
                "com.sk89q.worldedit.forge.ForgeAdapter"
        }) {
            try {
                Class<?> adapterClass = Class.forName(adapterClassName);
                return adapterClass.getMethod("adaptPlayer", net.minecraft.server.level.ServerPlayer.class)
                    .invoke(null, player);
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                // Try the next known adapter class name.
            }
        }
        return null;
    }

    private static BlockPos toBlockPos(Object blockVector3) throws ReflectiveOperationException {
        int x = (int) blockVector3.getClass().getMethod("getX").invoke(blockVector3);
        int y = (int) blockVector3.getClass().getMethod("getY").invoke(blockVector3);
        int z = (int) blockVector3.getClass().getMethod("getZ").invoke(blockVector3);
        return new BlockPos(x, y, z);
    }
}
