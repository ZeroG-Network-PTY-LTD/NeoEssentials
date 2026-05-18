package com.zerog.neoessentials.hologram;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Listens to world load/unload events to spawn/despawn hologram entities
 * automatically, and starts/stops the HologramScheduler with the server lifecycle.
 */
@EventBusSubscriber(modid = "neoessentials")
public class HologramEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(HologramEventHandler.class);
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        try {
            // Start the animation / placeholder-refresh scheduler.
            // NOTE: hologram entities are spawned in onLevelLoad (which fires before this event
            // for all initial dimensions), so we must NOT spawn again here – that would cause a
            // brief flicker as entities are despawned and immediately re-spawned.
            HologramScheduler.start();
            LOGGER.info("[Hologram] Scheduler started on server start ({} hologram(s) active).",
                com.zerog.neoessentials.hologram.HologramManager.getInstance().getAllHolograms().size());
        } catch (Exception e) {
            LOGGER.error("[Hologram] Failed to start hologram scheduler.", e);
        }
    }
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        try {
            String dimKey = HologramRenderer.dimensionKey(level);
            HologramRenderer.spawnAllForWorld(level, dimKey);
            LOGGER.debug("[Hologram] Spawned holograms for dimension '{}'", dimKey);
        } catch (Exception e) {
            LOGGER.error("[Hologram] Failed to spawn holograms on level load.", e);
        }
    }
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        try {
            String dimKey = HologramRenderer.dimensionKey(level);
            HologramRenderer.despawnAllForWorld(level, dimKey);
            LOGGER.debug("[Hologram] Despawned holograms for dimension '{}'", dimKey);
        } catch (Exception e) {
            LOGGER.debug("[Hologram] Error despawning holograms on level unload: {}", e.getMessage());
        }
    }
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        try {
            HologramScheduler.stop();
        } catch (Exception ignored) {}
    }
}
