package com.zerog.neoessentials.teleportation.Spawn;

import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles teleporting players to spawn on death if enabled in config.
 */
@EventBusSubscriber(modid = "neoessentials", bus = EventBusSubscriber.Bus.GAME)
public class SpawnOnDeathHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpawnOnDeathHandler.class);

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        try {
            ConfigManager config = ConfigManager.getInstance();
            boolean spawnOnDeath = false;
            if (config != null) {
                com.google.gson.JsonObject mainConfig = config.getConfig(ConfigManager.MAIN_CONFIG);
                if (mainConfig.has("teleportation")) {
                    com.google.gson.JsonObject tp = mainConfig.getAsJsonObject("teleportation");
                    if (tp.has("spawnSettings")) {
                        com.google.gson.JsonObject spawnSettings = tp.getAsJsonObject("spawnSettings");
                        if (spawnSettings.has("spawnOnDeath")) {
                            spawnOnDeath = spawnSettings.get("spawnOnDeath").getAsBoolean();
                        }
                    }
                }
            }
            if (spawnOnDeath) {
                SpawnManager.getInstance().teleportToSpawn(player);
                LOGGER.info("Teleported {} to spawn on death (spawnOnDeath enabled)", player.getName().getString());
            }
        } catch (Exception e) {
            LOGGER.error("Error handling spawnOnDeath for player {}: {}", event.getEntity().getName().getString(), e.getMessage());
        }
    }
}
