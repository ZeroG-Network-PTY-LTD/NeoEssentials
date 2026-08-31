package com.zerog.neoessentials.votifier;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Starts/stops {@link VotifierServer} with the Minecraft server — same lifecycle shape as
 *  {@code DashboardLifecycleManager} (a bad port/config never takes down the MC server itself). */
@EventBusSubscriber(modid = "neoessentials")
public class VotifierLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(VotifierLifecycleManager.class);

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!ConfigManager.isVotifierModuleEnabled()) {
            NeoLog.info(LOGGER, LogCategory.VOTIFIER, "Votifier module is disabled via config, skipping.");
            return;
        }
        if (!isEnabledInVotifierConfig()) {
            NeoLog.info(LOGGER, LogCategory.VOTIFIER, "Votifier is disabled in votifier.json, skipping.");
            return;
        }
        try {
            VotifierServer.getInstance().start();
        } catch (Throwable e) {
            NeoLog.error(LOGGER, LogCategory.VOTIFIER, "Failed to start Votifier listener — vote-site notifications will not be received", e);
        }
        VotePartyManager.getInstance().resetIfConfigured();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        try {
            if (VotifierServer.getInstance().isRunning()) {
                VotifierServer.getInstance().stop();
            }
        } catch (Throwable e) {
            NeoLog.error(LOGGER, LogCategory.VOTIFIER, "Error stopping Votifier listener", e);
        }
    }

    private static boolean isEnabledInVotifierConfig() {
        try {
            JsonObject root = ConfigManager.getInstance().getConfig(ConfigManager.VOTIFIER_CONFIG);
            if (!root.has("votifier")) return true;
            JsonObject votifier = root.getAsJsonObject("votifier");
            return !votifier.has("enabled") || votifier.get("enabled").getAsBoolean();
        } catch (Exception e) {
            return true;
        }
    }
}
